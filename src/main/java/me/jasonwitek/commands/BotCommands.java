package me.jasonwitek.commands;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import me.jasonwitek.EventManager;
import me.jasonwitek.Movie;
import me.jasonwitek.MovieManager;
import me.jasonwitek.WeeklyScheduler;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;

public class BotCommands extends ListenerAdapter {

    // All initialized on startup, no need to worry
    private MovieManager movieManager;
    private EventManager eventManager;
    private WeeklyScheduler scheduler;
    private String adminID = "715017058080915536"; // User ID of who is allow to configure the Bo

    public BotCommands(MovieManager inputMovieManager, EventManager inputEventManager, WeeklyScheduler inputWeeklyScheduler) {

        movieManager = Objects.requireNonNull(inputMovieManager,"MovieManager value is null; Make sure to pass a to the BotCommands");
        eventManager = Objects.requireNonNull(inputEventManager, "EventManager value is null; Make sure to pass a to the BotCommands");
        scheduler = Objects.requireNonNull(inputWeeklyScheduler, "WeeklyScheduler value is null; make sure to pass a to the BotCommands");

    }






    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        String eventName = event.getName();

        event.deferReply().queue();
        User user = event.getUser();
        
        switch (eventName) {
            // Config Commands
            case "setannouncementchannel" -> setAnnouncementChannel(event, user);
            case "seteventchannel" -> setEventChannel(event, user);
            case "setviewingchannel" -> setViewingChannel(event, user);
            case "setmoviepingrole" -> setMoviePingRole(event, user);

            // Override Commands
            case "setcurrentmovie" -> setCurrentMovie(event, user);
            case "setpreviousmovie" -> setPreviousMovie(event, user);
            case "enablerating" -> enableRating(event, user);
            case "disablerating" -> disableRating(event, user);
            
            // Edit Movie States
            case "addmovie" -> addMovie(event, user);
            case "deletemovie" -> deleteMovie(event, user);
            case "setwatched" -> setWatched(event, user);
            case "settotalrating" -> setTotalRating(event, user);
            case "setnumofrating" -> setNumberOfRating(event, user);

            // File Commands
            case "uploadmovielist" -> uploadMovieList(event, user);
            case "dump" -> dump(event,user);

            // Default User Commands
            case "help" -> help(event);
            case "suggestmovie" -> suggestMovie(event);
            case "watchedmovies" -> watchedMovies(event, user);
            case "ratemovie" -> rateMovie(event, user);
            case "topten" -> topTen(event);

            // Test Scheduler Methods
            case "testpollmessage" -> testPollMessage(event, user);
            case "testeventcreation" -> testEventCreation(event, user);
            case "testhournotice" -> testHourNotice(event, user);
            case "teststartingnotice" -> testStartingNotice(event, user);
        }
    }

    // HELPER METHODS TO MAKE THE COMMANDS LISTED ABOVE MORE READABLE
    
    private boolean isAdmin(@NotNull SlashCommandInteractionEvent event, User u) {
        if (u.getId().equals(adminID)) {
            return true;
        }
        event.getHook().sendMessage("Sorry, you do not have permissions to use this command")
                    .setEphemeral(true)
                    .queue();
        return false;
    }

    private void sendSuccessMessage(@NotNull SlashCommandInteractionEvent event, boolean b, String successMessage, String failMessage) {
        if (b) {
            event.getHook().sendMessage(successMessage)
                    .setEphemeral(true) 
                    .queue();
        } else {
            event.getHook().sendMessage(failMessage)
                    .setEphemeral(true) 
                    .queue();
        }
    }




    private void setAnnouncementChannel(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            sendSuccessMessage(event, // Event
                eventManager.setAnnouncementChannel(event.getChannel()), // Our boolean of success
                "Successfully set #" + event.getChannel().getName() + " as the movie announcement channel", // Success message
                "Unable to set #" + event.getChannel().getName() + " as the movie announcement channel"); // Failiure message
        }
    }

    private void setEventChannel(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            sendSuccessMessage(event, 
                eventManager.setEventChannel(event.getChannel()), 
                "Successfully set #" + event.getChannel().getName() + " as the movie event channel", 
                "Unable to set #" + event.getChannel().getName() + " as the movie event channel");
        }
    }

    private void setViewingChannel(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            OptionMapping voiceChannel = event.getOption("voicechannel");
            sendSuccessMessage(event, 
                eventManager.setViewingChannel(voiceChannel.getAsChannel().asVoiceChannel()), 
                "Successfully set " + voiceChannel.getAsString() + " as the viewing channel", 
                "Unable to set the viewing channel; are you such its a voice channel?");
        }
    }

    private void setMoviePingRole(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            OptionMapping optionRole = event.getOption("role");
            sendSuccessMessage(event, 
                eventManager.setMoviePingRole(optionRole.getAsRole()), 
                "Successfully set " + event.getOption("role").getAsRole() + " as the movie ping role", 
                "Unable to set " + event.getOption("role").getAsRole() + " as the movie ping role");
        }
    }




    private void setCurrentMovie(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            String movieTitleInput = Objects.requireNonNull(event.getOption("movietitle").getAsString());
            int releaseYearInput = event.getOption("releaseyear").getAsInt();
            sendSuccessMessage(event, 
                movieManager.setCurrentMovie(movieManager.findMovieByTitleAndYear(movieTitleInput, releaseYearInput)), 
                "Successfully set " + movieTitleInput + " as the current movie", 
                "Unable to set " + movieTitleInput + " as the current movie; Are you sure you entered it correctly?");
        }
    }

    private void setPreviousMovie(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            String movieTitleInput = Objects.requireNonNull(event.getOption("movietitle").getAsString());
            int releaseYearInput = event.getOption("releaseyear").getAsInt();
            sendSuccessMessage(event, 
                movieManager.setPreviousMovie(movieManager.findMovieByTitleAndYear(movieTitleInput, releaseYearInput)), 
                "Successfully set " + movieTitleInput + " as the previous movie", 
                "Unable to set " + movieTitleInput + " as the previous movie; Are you sure you entered it correctly?");
        }
    }

    private void enableRating(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            sendSuccessMessage(event, 
                movieManager.setIsRatingOpen(true), 
                "Successfully enabled movie rating", 
                "Unable to enable movie rating; something went wrong");
        }
    }

    private void disableRating(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            sendSuccessMessage(event, 
                movieManager.setIsRatingOpen(false), 
                "Successfully disabled movie rating", 
                "Unable to disable movie rating; something went wrong");
        }
    }




    private void addMovie(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            String movieTitle = event.getOption("movietitle").getAsString();
            int releaseYear = event.getOption("releaseyear").getAsInt();
            int runTime = event.getOption("runtime").getAsInt();
            String emoji = event.getOption("emoji").getAsString();
            String bannerURL = event.getOption("bannerurl").getAsString();
            boolean isWatched = event.getOption("iswatched").getAsBoolean();
            double rating = event.getOption("totalrating").getAsDouble();
            int numOfRating = event.getOption("numofrating").getAsInt();

            sendSuccessMessage(event, 
                movieManager.addMovie(movieTitle, releaseYear, runTime, emoji, bannerURL, isWatched, rating, numOfRating), 
                "Successfully added: " + movieTitle + " (" + releaseYear + ") to the movie list", 
                "Unable to add: " + movieTitle + " (" + releaseYear + ") to the movie list; is it already added?");
        }
    }

    private void deleteMovie(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            String movieTitle = event.getOption("movietitle").getAsString();
            int releaseYear = event.getOption("releaseyear").getAsInt();

            sendSuccessMessage(event, 
                movieManager.deleteMovie(movieTitle, releaseYear), 
                "Successfully deleted: " + movieTitle + " (" + releaseYear + ") from the movie list", 
                "Unable to delete: " + movieTitle + " (" + releaseYear + ") from the movie list; check for formatting issues");
        }
    }




    private void uploadMovieList(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {

            OptionMapping eventFile = event.getOption("list");

            if (eventFile == null || !eventFile.getAsAttachment().getFileName().endsWith(".json")) {
                event.getHook().sendMessage("Missing a .json file; Please try again")
                    .setEphemeral(true)
                    .queue();
                return;
            }

            Attachment attachment = eventFile.getAsAttachment();
            sendSuccessMessage(event, 
                movieManager.downloadMovieListToServer(attachment), 
                "Successfully uploaded the movielist", 
                "Movie list could not be uploaded; Check for typos in the .json file");
        }
    }

    private void dump(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            if (movieManager.getMovieListFile() == null) {
                event.getHook().sendMessage("Unable to retrieve the movie list; something went wrong.")
                    .setEphemeral(true)
                    .queue();
                return;
            }

            
            FileUpload movieList = FileUpload.fromData(movieManager.getMovieListFile());
            FileUpload configProperties = FileUpload.fromData(movieManager.getConfigPropertiesFile());
            FileUpload movieRating = FileUpload.fromData(movieManager.getMovieRatingFile());
            FileUpload weeklyOptions = FileUpload.fromData(movieManager.getMovieWeeklyOptionsFile());

            event.getHook()
                .sendFiles(movieList, configProperties, movieRating, weeklyOptions)
                .setEphemeral(true).queue();
        }
    }

    private void setWatched(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            String movieTitle = event.getOption("movietitle").getAsString();
            int releaseYear = event.getOption("releaseyear").getAsInt();
            boolean watched = event.getOption("iswatched").getAsBoolean();

            Movie ourMovie = movieManager.findMovieByTitleAndYear(movieTitle, releaseYear);
            ourMovie.setWatched(watched);
            movieManager.updateAllMovies();
            sendSuccessMessage(event, 
            ourMovie.getWatched() == watched, 
            "Successfully set the watch status of: " + movieTitle + " (" + releaseYear + ") to: " + watched, 
            "Unable to set the watch status of: " + movieTitle + " (" + releaseYear + ")");
            movieManager.sortWatchedUnWatched();
        }
    }

    private void setTotalRating(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            String movieTitle = event.getOption("movietitle").getAsString();
            int releaseYear = event.getOption("releaseyear").getAsInt();
            int totalRating = event.getOption("totalrating").getAsInt();
            Movie ourMovie = movieManager.findMovieByTitleAndYear(movieTitle, releaseYear);
            ourMovie.setRating(totalRating);
            movieManager.updateAllMovies();
            sendSuccessMessage(event, 
            ourMovie.getTotalRating() == totalRating, 
            "Successfully set the total rating of: " + movieTitle + " (" + releaseYear + ") to: " + totalRating, 
            "Unable to set the total rating of: " + movieTitle + " (" + releaseYear + ")");
        }
    }

    private void setNumberOfRating(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            String movieTitle = event.getOption("movietitle").getAsString();
            int releaseYear = event.getOption("releaseyear").getAsInt();
            int numOfRating = event.getOption("numofrating").getAsInt();
            Movie ourMovie = movieManager.findMovieByTitleAndYear(movieTitle, releaseYear);
            ourMovie.setNumberOfRatings(numOfRating);
            movieManager.updateAllMovies();

            sendSuccessMessage(event, 
            ourMovie.getNumberOfRatings() == numOfRating, 
            "Successfully set the number of ratings of: " + movieTitle + " (" + releaseYear + ") to: " + numOfRating, 
            "Unable to set the number of ratings of: " + movieTitle + " (" + releaseYear + ")");
        }
    }




    private void watchedMovies(SlashCommandInteractionEvent event, User user) {
        List<Movie> watchedMovies = movieManager.getWatchedMovies();

        StringBuilder outputMessage = new StringBuilder();

        outputMessage.append("**Here are the currently watched movies:** \n\n");

        if (watchedMovies == null || watchedMovies.size() == 0) {
            event.getHook().sendMessage(outputMessage.toString()).queue();
            return;
        }

        watchedMovies.sort(Comparator.comparing(Movie::getMovieTitle, String.CASE_INSENSITIVE_ORDER));
           
        for (Movie m : watchedMovies) {
            double averageRating = m.calculateAverageRating();

            outputMessage.append(String.format("%s %s (%d) ⭐%.2f\n",
                 m.getEmoji(),
                 m.getMovieTitle(),
                 m.getReleaseYear(), 
                 averageRating));
        }

        event.getHook().sendMessage(outputMessage.toString()).queue();
    }

    private void rateMovie(SlashCommandInteractionEvent event, User user) {
        if (!movieManager.getIsRatingOpen()) {
            event.getHook().sendMessage("The movie is unable to be rated at this time")
                .setEphemeral(true)
                .queue();
            return;
        }

        int rating = event.getOption("rating").getAsInt();

        if (rating < 0 || rating > 5) {
            event.getHook().sendMessage("Invalid rating, must be an integer between 0-5")
                .setEphemeral(true)
                .queue();
            return;
        }

        sendSuccessMessage(event, 
            movieManager.processRatingCommand(event.getUser(), rating), 
            "Successfully set ⭐" + rating + " as your rating for: " + movieManager.getCurrentMovie().getMovieTitle(), 
            "Unable to set to process your rating; are you sure you entered it correctly?");
    }

    private void help(SlashCommandInteractionEvent event) {
        StringBuilder response = new StringBuilder();
        response.append("# **Here are the current commands:** \n");
        response.append("``suggestmovie`` - suggest movies to add rotation \n\n");
        response.append("``watchedmovies`` - display all the watched movies alphabetically \n\n");
        response.append("``ratemovie`` - rate the current movie (only options are: 1, 2, 3, 4, 5) \n\n");
        response.append("``topten`` - display the top ten movies by rating \n\n");
        response.append("*Other features are currently in the works*");
        event.getHook().sendMessage(response.toString()).setEphemeral(true).queue();
    }

    private void suggestMovie(SlashCommandInteractionEvent event) {
        StringBuilder response = new StringBuilder();

        response.append("Want to suggest a movie to add to the list?\n");
        response.append("Fill out the form **here**: \n");
        response.append("https://forms.gle/WsxwhiU7oQ21wPrQ6");

        event.getHook().sendMessage(response.toString()).setEphemeral(true).queue();
    }

    private void topTen(SlashCommandInteractionEvent event) {
        StringBuilder outputMessage = new StringBuilder();

        outputMessage.append("**Here are the top ten rated movies:** \n\n");

        List<Movie> topTenList = movieManager.getWatchedMovies();

        topTenList.sort(Comparator.comparingDouble(Movie::calculateAverageRating).reversed()
                                .thenComparing(Movie::getTotalRating)
                                .thenComparing(Movie::getMovieTitle));

        for (int i = 0; i < 10 && i < topTenList.size(); ++i) {
            Movie m = topTenList.get(i);
            double averageRating = m.calculateAverageRating();
            outputMessage.append("**" + (i+1) + ".** ");
            outputMessage.append(String.format("%s %s (%d) ⭐%.2f\n",
                 m.getEmoji(),
                 m.getMovieTitle(),
                 m.getReleaseYear(), 
                 averageRating));
        }

        event.getHook().sendMessage(outputMessage.toString()).queue();
    }




    private void testPollMessage(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            scheduler.pollSend();

            event.getHook().sendMessage("Sent Monday Poll Message")
                .setEphemeral(true) // Makes it so only the user who ran the command sees the message
                .queue();
        }
    }

    private void testEventCreation(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            scheduler.movieEventCreation();

            event.getHook().sendMessage("Sent Wednesday Schedule Message")
                .setEphemeral(true) // Makes it so only the user who ran the command sees the message
                .queue();
        }
    }

    private void testHourNotice(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            scheduler.movieHourNotice();

            event.getHook().sendMessage("Sent Sunday One Hour Message")
                .setEphemeral(true) // Makes it so only the user who ran the command sees the message
                .queue();
        }
    }

    private void testStartingNotice(SlashCommandInteractionEvent event, User user) {
        if (isAdmin(event, user)) {
            scheduler.movieStartingNotice();

            event.getHook().sendMessage("Sent Sunday Movie Starting Message")
                .setEphemeral(true) // Makes it so only the user who ran the command sees the message
                .queue();
        }
    }

}