package me.jasonwitek;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import me.jasonwitek.commands.BotCommands;

public class DiscordBot {

    // File Paths
    private static final String configFilePath = "config.properties";
    private static final String allMovieListPath = "movielist.json";

    public static void main(String[] args) throws InterruptedException {

        String token = System.getenv("DISCORD_BOT_CELLULOID_TOKEN");
        
        File configFile = new File(configFilePath);
        Properties properties = new Properties();
        loadConfig(configFile, properties);

        MovieManager movieManager = new MovieManager();
        loadMovieManagerFiles(movieManager, properties);

        EventManager eventManager = new EventManager(movieManager, properties);

        WeeklyScheduler scheduler = new WeeklyScheduler(eventManager, properties);
        scheduler.start();

        BotCommands commands = new BotCommands(movieManager, eventManager, scheduler);

        JDA bot = JDABuilder.createDefault(token)
            .addEventListeners(commands)
            .build()
            .awaitReady();
        System.out.println("Bot Successfully Started");

        // Gets the first server
        // shouldn't be a problem since the bot will only be in 1 server at a time
        Guild server = bot.getGuilds().getFirst(); 

        verifyServerExists(server);

        initializeEventManager(eventManager, bot, server);

        registerSlashCommands(server);
    }

    private static void loadConfig(File configFile, Properties properties) {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)){
                properties.load(reader);
                System.out.println("Config file successfully loaded");
            } catch (Exception e) {
                System.out.println("Config file exists, but was unable to be loaded; check for formatting issues");
                e.printStackTrace();
            }
        }
    }

    private static void loadMovieManagerFiles(MovieManager movieManager, Properties properties) {
        File listFile = new File(allMovieListPath);
        if (listFile.exists()) {
            try {
                // This will also call the loader for the properties
                movieManager.convertMovieListToObject(listFile);
                movieManager.loadProperties(properties);
                movieManager.sortWatchedUnWatched();
                System.out.println("Successfully initialized " + allMovieListPath);
            } catch (Exception e) {
                System.out.println("Unable to initialize " + allMovieListPath);
                e.printStackTrace();
            }
        } else {
            System.out.println("No current movie list uploaded, make sure to use /uploadmovielist with a .json file");
        }
    }

    private static void verifyServerExists(Guild server) {
        if (server == null) {
            throw new IllegalStateException("Server is equal to null; Check your server ID");
        }
    }

    private static void initializeEventManager(EventManager eventManager, JDA bot, Guild server) {
        if (bot == null || server == null || eventManager == null) {
            System.out.println("Unable to parse JDA instance to EventManager; Something went wrong");
            throw new IllegalStateException("Bot, Server, or Eventmanager is null; unable to continue");
        } else {
            eventManager.setJDA(bot, server);
            System.out.println("Successfully parsed JDA instance to EventManager");
        }
    }

    private static void registerSlashCommands(Guild server) {
        server.updateCommands().queue();
        registerPublicSlashCommands(server);
        registerAdminSlashCommands(server);
        registerDebuggingSlashCommands(server);
    }

    private static void registerPublicSlashCommands(Guild server) {
        server.upsertCommand("watchedmovies", "Returns a list of the movies we have already watched")
             .queue();
        server.upsertCommand("ratemovie", "Rate the current movie 1 - 5 stars")
             .addOption(OptionType.INTEGER, "rating", "The whole number of stars you rated", true)
             .queue();
        server.upsertCommand("topten", "Returns a list of the top ten movies we have watched")
             .queue();
        server.upsertCommand("help", "Display the help message")
             .queue();
        server.upsertCommand("search", "Search for a specific Movie")
             .addOption(OptionType.STRING, "query", "The name of the movie you wish to search for", true)
             .queue();
    }

    private static void registerAdminSlashCommands(Guild server) {
        server.upsertCommand("setannouncementchannel", "**ADMIN ONLY** Sets the current channel as movie poll / movie announcement channel")
             .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
             .queue();
        server.upsertCommand("seteventchannel", "**ADMIN ONLY** Sets the current channel as event channel")
             .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)) 
             .queue();
        server.upsertCommand("setviewingchannel", "**ADMIN ONLY** Must be the id of a VOICE CHANNEL")
            .addOption(OptionType.CHANNEL, "voicechannel", "The voice channel that the event will take place", true)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .queue();
        server.upsertCommand("setmoviepingrole", "**ADMIN ONLY** Sets the role to be pinged for movies")
             .addOption(OptionType.ROLE, "role", "A role to be pinged", true)
             .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
             .queue();

        server.upsertCommand("uploadmovielist", "**ADMIN ONLY** Upload .json of Movie Objects")
             .addOption(OptionType.ATTACHMENT, "list", "A .json file containing Movie Objects", true)
             .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
             .queue();
        server.upsertCommand("dump", "**ADMIN ONLY** Returns a .json file of the Movie Objects")
             .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
             .queue();

        server.upsertCommand("setcurrentmovie", "**ADMIN ONLY** Sets the current movie")
            .addOption(OptionType.STRING, "movietitle", "the movie title; exactly as written", true)
            .addOption(OptionType.INTEGER, "releaseyear", "the year the movie was released", true)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .queue();
        server.upsertCommand("setpreviousmovie", "**ADMIN ONLY** Sets the previous movie")
            .addOption(OptionType.STRING, "movietitle", "the movie title; exactly as written", true)
            .addOption(OptionType.INTEGER, "releaseyear", "the year the movie was released", true)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .queue();

        server.upsertCommand("setwatched", "**ADMIN ONLY** Set the watch status of a movie")
            .addOption(OptionType.STRING, "movietitle", "the movie title; exactly as written", true)
            .addOption(OptionType.INTEGER, "releaseyear", "the year the movie was released", true)
            .addOption(OptionType.BOOLEAN, "iswatched", "true if watched, false if unwatched", true)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .queue();
        server.upsertCommand("settotalrating", "**ADMIN ONLY** Set the total rating of a movie")
            .addOption(OptionType.STRING, "movietitle", "the movie title; exactly as written", true)
            .addOption(OptionType.INTEGER, "releaseyear", "the year the movie was released", true)
            .addOption(OptionType.INTEGER, "totalrating", "the new totalrating", true)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .queue();
        server.upsertCommand("setnumofrating", "**ADMIN ONLY** Set the number of ratings of a movie")
            .addOption(OptionType.STRING, "movietitle", "the movie title; exactly as written", true)
            .addOption(OptionType.INTEGER, "releaseyear", "the year the movie was released", true)
            .addOption(OptionType.INTEGER, "numofrating", "the new numofrating", true)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .queue();

        server.upsertCommand("enablerating", "**ADMIN ONLY** enables movie rating")
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .queue();
        server.upsertCommand("disablerating", "**ADMIN ONLY** disables movie rating")
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .queue();

        server.upsertCommand("addmovie", "**ADMIN ONLY** adds a movie to the rotation")
            .addOption(OptionType.STRING, "movietitle", "The EXACT title of the movie", true)
            .addOption(OptionType.INTEGER, "releaseyear", "The year the movie was originally released", true)
            .addOption(OptionType.INTEGER, "runtime", "The movie runtime in MINUTES", true)
            .addOption(OptionType.STRING, "emoji", "An emoji to represent the movie", true)
            .addOption(OptionType.STRING, "bannerurl", "A link to the image used for event banner", true)
            .addOption(OptionType.BOOLEAN, "iswatched", "true if watched, false if unwatched", true)
            .addOption(OptionType.NUMBER, "totalrating", "The collective rating of the movie", true)
            .addOption(OptionType.INTEGER, "numofrating", "The number of votes for the movie", true)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .queue();
        server.upsertCommand("deletemovie", "**ADMIN ONLY** Deletes a movie from rotation")
            .addOption(OptionType.STRING, "movietitle", "the movie title; exactly as written", true)
            .addOption(OptionType.INTEGER, "releaseyear", "the year the movie was released", true)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .queue();
    }

    private static void registerDebuggingSlashCommands(Guild server) {
        server.upsertCommand("testpollmessage", "**DEBUGGING** gives a test message of the monday poll")
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
             .queue();
        server.upsertCommand("testeventcreation", "**DEBUGGING** creates an event and posts wednesday messages")
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .queue();
        server.upsertCommand("testhournotice", "**DEBUGGING** gives a test message of one hour movie notice")
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .queue();
        server.upsertCommand("teststartingnotice", "**DEBUGGING** gives a test message of moving starting notice")
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .queue();
    }
}