package me.jasonwitek;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.function.Function;

import kotlin.Pair;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.requests.restaction.ScheduledEventAction;

public class EventManager {
    
    // File paths
    private static final String configFilePath = "config.properties";

    private JDA jda;
    private Guild server;
    private MovieManager movManager;
    private Properties properties;

    private MessageChannel announcementChannel; // stored in eventmanager.announcementchannel.id
    private MessageChannel eventChannel;        // stored in eventmanager.eventchannel.id
    private VoiceChannel viewingChannel;        // stored in eventmanager.viewingchannel.id
    private Role moviePingRole;                 // stored in eventmanager.moviepingrole.id
    private long pollMessageID;                 // stored in eventmanager.pollmessage.id

    public EventManager(MovieManager inputMovieManager, Properties inputProperties) {

        movManager = Objects.requireNonNull(inputMovieManager, "Movie Manager is null, unable to set Eventmanager");
        properties = Objects.requireNonNull(inputProperties, "Movie Manager is null, unable to set Eventmanager");
    
    }

    public void setJDA(JDA inputBot, Guild inputGuild) {
        jda = inputBot;
        server = inputGuild;

        setBotActivityStatus();
        
        announcementChannel = getPropertyFor(
            "eventmanager.announcementchannel.id", 
            properties, 
            id -> jda.getChannelById(MessageChannel.class, id),
            "Unable to initilaze announcement channel"
        );

        eventChannel = getPropertyFor(
            "eventmanager.eventchannel.id",
            properties,
            id -> jda.getChannelById(MessageChannel.class, id),
            "Unable to initilaze event channel"
        );

        moviePingRole = getPropertyFor(
            "eventmanager.moviepingrole.id",
            properties,
            id -> server.getRoleById(id),
            "Unable to initilaze movie ping role"
        );
        
        pollMessageID = getPropertyFor(
            "eventmanager.pollmessage.id",
            properties,
            id -> Long.valueOf(id),
            "Unable to initilaze monday poll message id"
        );
        
        viewingChannel = getPropertyFor(
            "eventmanager.viewingchannel.id",
            properties,
            id -> server.getVoiceChannelById(id),
            "Unable to initilaze viewing channel"
        );
    }

    private void setBotActivityStatus() {
        if (movManager.getCurrentMovie() == null) {
            jda.getPresence().setActivity(Activity.customStatus("Awaiting for Movie"));
            return;
        }

        if (movManager.getCurrentMovie().getMovieTitle().length() > 120) {
            jda.getPresence().setActivity(Activity.playing("pepper"));
        } else {
            jda.getPresence().setActivity(Activity.playing(movManager.getCurrentMovie().getEmoji()
            + " " + movManager.getCurrentMovie().getMovieTitle() 
            + " (" + movManager.getCurrentMovie().getReleaseYear() + ")" ));
        }
    }

    private String require(Properties p, String s) {
        String value = p.getProperty(s);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Properties file exists, but unable to initialize " + s);
        }
        return value.trim();
    }

    private <T> T getPropertyFor(String propID, Properties properties, Function<String, T> resolver, String errorMsg) {
        try {
            String valueID = require(properties, propID);
            T result = resolver.apply(valueID);
            if (result == null) {
                throw new IllegalStateException("Resolved value is null");
            }
            return result;
        } catch (Exception e) {
            System.out.println(errorMsg);
            e.printStackTrace();
            return null;
        }
    }


    
    
    

    public boolean sendPollMessage(ZonedDateTime wednesdayPollClose) {

        //Closes rating
        movManager.setIsRatingOpen(false);

        // Tabulate the ratings
        Map<String, Integer> movieRatings = movManager.getRatings();
        movManager.eraseRatings();
        int numOfRatings = movieRatings.size();
        int totalRating = 0;
        for (Map.Entry<String, Integer> entry: movieRatings.entrySet()) {
            totalRating += entry.getValue();
        }

        updateCurrentMovieSetPrevious(numOfRatings, totalRating);

        return buildAndSendPollMessage(wednesdayPollClose);
    }

    private void updateCurrentMovieSetPrevious(int numOfRatings, int totalRating) {
        //prevents null pointer execption if no movies have previously been watched
        // or if its the first time the botis run
        if (movManager.getCurrentMovie() != null) {
            movManager.getCurrentMovie().setRating(totalRating);
            movManager.getCurrentMovie().setNumberOfRatings(numOfRatings);
            movManager.getCurrentMovie().setWatched(true);
            movManager.setPreviousMovie(movManager.getCurrentMovie());
            movManager.sortAllMoviesAlphabetically();
            movManager.sortWatchedUnWatched();
            movManager.updateAllMovies();
        }
    }

    private boolean buildAndSendPollMessage(ZonedDateTime wednesdayPollClose) {

        List<Movie> thisWeeksMovies = generateThisWeeksMovies();

        long unixTime = wednesdayPollClose.toEpochSecond();
        StringBuilder pollMessage = new StringBuilder();

        pollMessage.append("Thank you for coming to watch " 
            + movManager.getPreviousMovie().getMovieTitle() 
            + "\n\n");
        double averageRating = movManager.getPreviousMovie().calculateAverageRating();
        
        pollMessage.append(movManager.getPreviousMovie().getMovieTitle() 
            + " was collectively rated: **⭐" 
            + averageRating
            + "**\n\n");

        pollMessage.append("Please vote for this weeks movie: \n");


        for (Movie m : thisWeeksMovies) {
                pollMessage.append(String.format("%s %s (%d)\n",
                     m.getEmoji(),
                     m.getMovieTitle(),
                     m.getReleaseYear()));
        }

        pollMessage.append("\n*Voting Closes on: \n" + "<t:" + unixTime + ":f>* \n");
        pollMessage.append("||" + moviePingRole.getAsMention() + "||");

        announcementChannel.sendMessage(pollMessage.toString()).queue(message -> { 
            setPollMessageID(message.getIdLong());
            for (Movie m : thisWeeksMovies) {

                Emoji e = Emoji.fromUnicode(m.getEmoji());
                message.addReaction(e).queue();
            }
        });
        
        movManager.setThisWeeksMovieOptions(thisWeeksMovies);
        return true;
    }
  
    private List<Movie> generateThisWeeksMovies() {
        List<Movie> thisWeeksMovies = new ArrayList<>();

        List<Movie> unWatched = movManager.getUnwatchedMovies();

        if (unWatched.size() <= 5) {
            thisWeeksMovies = unWatched;
        } else {
            Random random = new Random();
            while (thisWeeksMovies.size() < 5) {

                // gets our next movie, checks if we already added it
                Movie cur = unWatched.get(random.nextInt(unWatched.size()));
                if (thisWeeksMovies.contains(cur)) {
                    continue;
                } 
                
                // Checks to make sure each emoji is unique
                // Otherwise we wouldnt be able to make a poll
                boolean isEmojiUsed = false;
                for (Movie m : thisWeeksMovies) {
                    if (cur.getEmoji().equals(m.getEmoji())) {
                        isEmojiUsed = true;
                    }
                }
                
                if (isEmojiUsed) {
                    continue;
                } else {
                    thisWeeksMovies.add(cur);
                }
            }
        }
        return thisWeeksMovies;
    }



    public boolean createWednesdayEvent(OffsetDateTime startTime) {

        Movie thisWeeksMovie = getMostVotedMovie();
        movManager.setCurrentMovie(thisWeeksMovie);
        
        setBotActivityStatus();

        Movie currentMovie = movManager.getCurrentMovie();

        Icon eventBanner = convertURLStringToIcon(currentMovie.getBannerURL());

        ScheduledEventAction movieEvent;

        
        movieEvent = server.createScheduledEvent(currentMovie.getMovieTitle() + " (" + currentMovie.getReleaseYear() + ") ", viewingChannel, startTime)
            .setDescription("The movie will start at 7:10PM CST. The runtime is " +
                 convertRuntimeToString(currentMovie.getRuntime()) +
                 ". Expect to be done by " +
                 calculateEndTimeToString(currentMovie.getRuntime()));

        if (eventBanner != null) {
            movieEvent = movieEvent.setImage(eventBanner);
        }

        ScheduledEvent scheduledMovieEvent = movieEvent.complete();

        return buildAndSendEventMessage(startTime, scheduledMovieEvent) 
                && buildAndSendAnnouncementMessage(scheduledMovieEvent);
    }
    
    private boolean buildAndSendAnnouncementMessage(ScheduledEvent scheduledEvent) {
        StringBuilder announcementMessage = new StringBuilder();
        announcementMessage.append(movManager.getCurrentMovie().getMovieTitle() + " will be played this **Sunday** at 7:00PM CST. \n\n");
        announcementMessage.append("Mark as Interested: \n");
        announcementMessage.append(scheduledEvent.getJumpUrl() + "\n");
        announcementMessage.append("||" + getMoviePingRole().getAsMention() + "||");

        announcementChannel.sendMessage(announcementMessage.toString()).queue();
        return true;
    }

    private boolean buildAndSendEventMessage(OffsetDateTime startTime, ScheduledEvent scheduledEvent) {

        StringBuilder eventNoticeMessage = new StringBuilder();

        eventNoticeMessage.append("# **" + movManager.getCurrentMovie().getMovieTitle() + "** \n\n Starts: \n");
        eventNoticeMessage.append("<t:" + startTime.toEpochSecond() + ":f> \n\n Mark As Interested: \n");
        eventNoticeMessage.append(scheduledEvent.getJumpUrl());
        eventChannel.sendMessage(eventNoticeMessage.toString()).queue();
        return true;
    }
    
    private Icon convertURLStringToIcon(String s) {
        try {   
            try (InputStream is = new URL(s).openStream()) {
                return Icon.from(is);
            }
        } catch (Exception e) {
            System.out.println("Unable to upload banner image passing default one");
            e.printStackTrace();
        }
        return null;
    }

    private Movie getMostVotedMovie() {

        List<MessageReaction> reactions = getAnnouncementChannel()
            .retrieveMessageById(pollMessageID).complete().getReactions();
        List<Movie> thisWeekMovies = movManager.getThisWeeksMovieOptions();

        Pair<List<Pair<String, Integer>>, Integer> pair = compileMessageVotes(thisWeekMovies, reactions);
        List<Pair<String, Integer>> voteList = pair.getFirst();
        int highestVote = pair.getSecond();

        // Removes any movies that do not have the most votes
        // Size() > 1 if there is a tie
        voteList.removeIf(element -> element.getSecond() != highestVote);

        Random random = new Random();
        // Should get the emoji of one of the top ranked movies
        String selectedMovieAsEmoji = voteList.get(random.nextInt(voteList.size())).getFirst();

        for (Movie m : thisWeekMovies) {
            if (m.getEmoji().equals(selectedMovieAsEmoji)) {
                return m;
            }
        }

        throw new NoSuchElementException("Unable to find the correct movie; check to make sure emojis are correct");
    }

    private Pair<List<Pair<String, Integer>>, Integer> compileMessageVotes(List<Movie> thisWeekMovies, List<MessageReaction> reactions) {
        int highestVote = 0;
        List<Pair<String, Integer>> voteList = new ArrayList<>();
        for (MessageReaction mR : reactions) {
            boolean isInThisWeekMovies = false;
            for (Movie m : thisWeekMovies) {
                if (mR.getEmoji().getFormatted().equals(m.getEmoji())) {
                    isInThisWeekMovies = true;
                }
            }
            if (!isInThisWeekMovies) {
                continue;
            }
            Pair<String, Integer> emojiVote = new Pair<String,Integer>(mR.getEmoji().getFormatted(), mR.getCount());
            voteList.add(emojiVote);

            if (mR.getCount() > highestVote) {
                highestVote = mR.getCount();
            }
        }
        return new Pair<List<Pair<String, Integer>>, Integer>(voteList, highestVote);
    }
    
    private String convertRuntimeToString(int min) {

        String toReturn = "";
        int minute = min % 60;
        int hour = (min - minute) / 60;

        if (hour == 0) {
            if (minute > 1) {
                toReturn += minute + " minutes";
            } else {
                toReturn += minute + " minute";
            }
            return toReturn;
        } else if (minute == 0) {
            if (hour > 1) {
                toReturn += hour + " hours";
            } else {
                toReturn += hour + " hour";
            }
            return toReturn;
        }
        if (hour > 1) {
            toReturn += hour + " hours";
        } else {
            toReturn += hour + " hour";
        }
        if (minute > 1) {
            toReturn += minute + " minutes";
        } else {
            toReturn += minute + " minute";
        }
        return toReturn;
    }
    
    private String calculateEndTimeToString(int min) {
        String toReturn = "";
                    
        final int MINUTES_FROM_NOON_TO_SHOWTIME = 430;
                    
        int endTime = MINUTES_FROM_NOON_TO_SHOWTIME + min;
        endTime = (int) Math.ceil(endTime / 10.0) * 10; // Rounds up to nearest 10 minutes
        int minutes = endTime % 60;
        int hours = (endTime - minutes) / 60;

        if (hours > 12) {
            hours -= 12;
        }
        if (minutes !=  0) {
            toReturn += hours + ":" + minutes;
        } else {
            toReturn += hours + ":00";
        }

        if (endTime >= 720) {
            toReturn += "AM CST";
        } else {
            toReturn += "PM CST";
        }

        return toReturn;
    }
    



    
    public boolean sendSundayOneHourNotice() {
        StringBuilder noticeMessage = new StringBuilder();

        // BUilds the message
        noticeMessage.append(movManager.getCurrentMovie().getMovieTitle() +
             " (" + movManager.getCurrentMovie().getReleaseYear() + ")" +
             " will be starting in 1 hour \n\n");

        noticeMessage.append("||" + moviePingRole.getAsMention() + "||");

        // Sends the message
        announcementChannel.sendMessage(noticeMessage.toString()).queue();
        return true;
    }

    public boolean sendSundayStartingNowNotice() {
        StringBuilder startingMessage = new StringBuilder();

        // Builds our message
        startingMessage.append(movManager.getCurrentMovie().getMovieTitle() + 
             " (" + movManager.getCurrentMovie().getReleaseYear() + ")" +
              " is starting now in " + getViewingChannel().getAsMention());

        startingMessage.append("\n\n||" + getMoviePingRole().getAsMention() + "||");

        // Sends our message
        announcementChannel.sendMessage(startingMessage.toString()).queue();

        movManager.setIsRatingOpen(true);
        return true;
    }
    
    // getters and setts for our two channels that announcement messages can be sent into

    public MessageChannel getAnnouncementChannel() {
        return announcementChannel;
    }

    public MessageChannel getEventChannel() {
        return eventChannel;
    }

    public VoiceChannel getViewingChannel() {
        return viewingChannel;
    }

    public Role getMoviePingRole() {
        return moviePingRole;
    }

    public long getPollMessageID() {
        return pollMessageID;
    }




    public boolean setAnnouncementChannel(MessageChannelUnion mcu) {
        if (mcu == null) {
            throw new IllegalArgumentException("Channel is null, unable to set announcement channel");
        }
        announcementChannel = mcu;
        return saveProperty("eventmanager.announcementchannel.id", mcu.getId());
    }

    public boolean setViewingChannel(VoiceChannel vc) {
        if (vc == null) {
            throw new IllegalArgumentException("Unable to set viewing channel because passed channel is null");
        }
        viewingChannel = vc;
        return saveProperty("eventmanager.viewingchannel.id", vc.getId());
    }

    public boolean setPollMessageID(long l) {
        pollMessageID = l;
        return saveProperty("eventmanager.pollmessage.id", Long.toString(l));
    }

    public boolean setEventChannel(MessageChannelUnion mcu) {
        if (mcu == null) {
            throw new IllegalArgumentException("Channel is null, unable to set announcement channel");
        }
        eventChannel = mcu;
        return saveProperty("eventmanager.eventchannel.id", mcu.getId());
    }

    public boolean setMoviePingRole(Role r) {
        if (r == null) {
            throw new IllegalArgumentException("Role is null, unable to set movie ping role");
        }
        moviePingRole = r;
        return saveProperty("eventmanager.moviepingrole.id", r.getId());
    }

    private boolean saveProperty(String key, String value) {
        try (FileInputStream fis = new FileInputStream(configFilePath);) {
            properties.load(fis);
            properties.setProperty(key, value);
            try (FileOutputStream fos = new FileOutputStream(configFilePath)) {
                properties.store(fos, null);
            }
            return true;
        } catch (Exception e) {
            System.out.println("Unable to save property: " + key);
            e.printStackTrace();
            return false;
        }
    }
}