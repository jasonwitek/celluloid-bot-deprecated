package me.jasonwitek;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WeeklyScheduler {

    // File paths
    private static final String configFilePath = "config.properties";

    // Scheduler Timezone
    private static final String timeZoneID = "America/Chicago";

    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    private EventManager eveManager;
    private Properties properties;

    // boolean values to ensure that the events are being run in order
    private boolean isPollPosted;
    private boolean isMovieAnnouncementPosted;
    private boolean isHourNoticePosted;
    private boolean isStartingNoticePosted;

    public WeeklyScheduler(EventManager inpuEventManager, Properties inpuProperties) {

        eveManager = Objects.requireNonNull(inpuEventManager, "EventManager passed to WeeklyScheduler is null! Unable to initialize eveManager");
        properties = Objects.requireNonNull(inpuProperties, "Properties passed to WeeklyScheduler is null! Unable to initialize properties");

        // Initialize boolean states from the config.properties file
        try {
            String isPollPostedAsString = properties.getProperty("orderchecker.ispollposted.boolean", "false");
        
            isPollPosted = Boolean.parseBoolean(isPollPostedAsString.trim());
            
            System.out.println("Set isPollPosted to: " + isPollPosted);
        } catch (Exception e) {
            System.out.println("Unable to read orderchecker.ispollposted.boolean; Does config.properties exist?");
            e.printStackTrace();
        }

        try {
            String isAnnouncementPostedAsString = properties.getProperty("orderchecker.isannouncementposted.boolean", "true");
            
            isMovieAnnouncementPosted = Boolean.parseBoolean(isAnnouncementPostedAsString.trim());
            
            System.out.println("Set isMovieAnnouncementPosted to: " + isMovieAnnouncementPosted);
        } catch (Exception e) {
            System.out.println("Unable to read orderchecker.isannouncementposted.boolean; Does config.properties exist?");
            e.printStackTrace();
        }

        try {
            String isHourNoticePostedAsString = properties.getProperty("orderchecker.ishournoticeposted.boolean", "true");
            
            isHourNoticePosted = Boolean.parseBoolean(isHourNoticePostedAsString.trim());
            
            System.out.println("Set isHourNoticePosted to: " + isHourNoticePosted);
        } catch (Exception e) {
            System.out.println("Unable to read orderchecker.ishournoticeposted.boolean; Does config.properties exist?");
            e.printStackTrace();
        }

        try {
            String isStartingNoticePostedAsString = properties.getProperty("orderchecker.isstartingnoticeposted.boolean", "true");
            
            isStartingNoticePosted = Boolean.parseBoolean(isStartingNoticePostedAsString.trim());
            
            System.out.println("Set isStartingNoticePosted to: " + isStartingNoticePosted);
        } catch (Exception e) {
            System.out.println("Unable to read orderchecker.isstartingnoticeposted.boolean; Does config.properties exist?");
            e.printStackTrace();
        }
        System.out.println("Scheduler Created");
    }
    


    // END OF INITIALIZATION
    // END OF INITIALIZATION
    // END OF INITIALIZATION



    public void start() {
        // TODO: MAKE THESE TIMES TOGGLEABLE
        scheduleNext(this::pollSend, DayOfWeek.MONDAY, 12, 0);
        scheduleNext(this::movieEventCreation, DayOfWeek.WEDNESDAY, 12, 0);
        scheduleNext(this::movieHourNotice, DayOfWeek.SUNDAY, 18, 0);
        scheduleNext(this::movieStartingNotice, DayOfWeek.SUNDAY, 19, 0);
    }

    private void scheduleNext(Runnable task, DayOfWeek day, int hour, int minute) {
        ZonedDateTime nextRun = nextOccurrence(day, hour, minute);

        executor.schedule(() -> {
            try {
                task.run();
            } finally {
                scheduleNext(task, day, hour, minute);
            }
        }, Duration.between(ZonedDateTime.now(ZoneId.of(timeZoneID)), nextRun).toSeconds(), TimeUnit.SECONDS);

        System.out.println(day + " Task Successfully Scheduled for " + nextRun.toInstant());
    }

    private ZonedDateTime nextOccurrence(DayOfWeek day, int hour, int minute) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timeZoneID));

        ZonedDateTime next = now
            .with(TemporalAdjusters.nextOrSame(day))
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0);

        if (next.isBefore(now)) {
            next = next.plusWeeks(1);
        }

        return next;
    }

    // Responsible just for formatting and sending the poll message on Monday at 8AM
    public void pollSend() {
        // Checks for exact correct state (everything else posted but monday)
        if (isMovieAnnouncementPosted && isHourNoticePosted && isStartingNoticePosted && !isPollPosted) {
            // Calculate poll close time here and then we pass it to the method
            ZonedDateTime thisWednesday = nextOccurrence(DayOfWeek.WEDNESDAY, 12, 0);
            if (eveManager.sendPollMessage(thisWednesday)) {
                System.out.println("Sucessfully sent Monday Poll Message at :" + Instant.now().toString());

                // Updates booleans to be in the reset state.
                if (setBooleanState("orderchecker.isannouncementposted.boolean", false)) {
                    isMovieAnnouncementPosted = false;
                }
                if (setBooleanState("orderchecker.ishournoticeposted.boolean", false)) {
                    isHourNoticePosted = false;
                }
                if (setBooleanState("orderchecker.isstartingnoticeposted.boolean", false)) {
                    isStartingNoticePosted = false;
                }
                if (setBooleanState("orderchecker.ispollposted.boolean", true)) {
                    isPollPosted = true;
                }
            }
        } else {
            System.out.println("Unable to post Monday poll because bot state is incorrect:");
            printOutBooleanStates();
        }
    }

    // Responsible for calculating the movie that won (in result of tie pick random)
    // and then creating the appropriate event and sending the announcement message
    public void movieEventCreation() {
        // Checks for exact correct state (only monday poll has been posted)
        if (isPollPosted && !isMovieAnnouncementPosted && !isHourNoticePosted && !isStartingNoticePosted) {
            ZonedDateTime thisSunday = nextOccurrence(DayOfWeek.SUNDAY, 19, 0);
        
            if (eveManager.createWednesdayEvent(thisSunday.toOffsetDateTime())) {
                System.out.println("Sucessfully create Event and sent Messages at:" + Instant.now().toString());

                if (setBooleanState("orderchecker.isannouncementposted.boolean", true)) {
                    isMovieAnnouncementPosted = true;
                }
            } // Event Created
        } else {
            System.out.println("Unable to post Wednesday announcements because bot state is incorrect:");
            printOutBooleanStates();
        }
    
    }

    // Responsible for just sending out a ping that says the movie will begin in an hour
    public void movieHourNotice() {
        if (isPollPosted && isMovieAnnouncementPosted && !isHourNoticePosted && !isStartingNoticePosted) {
            if (eveManager.sendSundayOneHourNotice()) {
                System.out.println("Sucessfully posted One Hour Notice Message at: " + Instant.now().toString());
            }
            if (setBooleanState("orderchecker.ishournoticeposted.boolean", true)) {
                isHourNoticePosted = true;
            }
        } else {
            System.out.println("Unable to post Sunday hour notice because bot state is incorrect:");
            printOutBooleanStates();
        }
    }

    // Responsible for sending out a ping that says the movie is beginning now
    // Opens rating for the movie
    public void movieStartingNotice() {
        if (isPollPosted && isMovieAnnouncementPosted && isHourNoticePosted && !isStartingNoticePosted) {
            if (eveManager.sendSundayStartingNowNotice()) {
                System.out.println("Sucessfully posted Movie Starting Notice Message at: " + Instant.now().toString());
            }
            if (setBooleanState("orderchecker.isstartingnoticeposted.boolean", true)) {
                isStartingNoticePosted = true;
            }
            if (setBooleanState("orderchecker.ispollposted.boolean", false)) {
                isPollPosted = false;
            }

            // By this point of the week the boolean states should be as follows:
            // isPollPosted = false;
            // isMovieAnnouncementPosted = true;
            // isHourNoticePosted = true;
            // isStartingNoticePosted = true;

        } else {
            System.out.println("Unable to post Sunday starting notice because bot state is incorrect:");
            printOutBooleanStates();
        }
    }

    private void printOutBooleanStates() {
        System.out.println("isPollPosted = " + isPollPosted);
        System.out.println("isMovieAnnouncementPosted = " + isMovieAnnouncementPosted);
        System.out.println("isHourNoticePosted = " + isHourNoticePosted);
        System.out.println("isStartingNoticePosted = " + isStartingNoticePosted);
    }

    public boolean setBooleanState(String configString, boolean boolValue) {
        try {
            FileInputStream fis = new FileInputStream(configFilePath);
            properties.load(fis);
            properties.setProperty(configString, Boolean.toString(boolValue));
            FileOutputStream fos = new FileOutputStream(configFilePath);
            properties.store(fos, null);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
