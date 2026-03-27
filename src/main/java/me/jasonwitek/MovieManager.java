package me.jasonwitek;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Message.Attachment;

public class MovieManager {
    
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String movieRatingsPath = "movierating.json";
    private static final String allMovieListPath = "movielist.json";
    private static final String thisWeeksOptionsPath = "movieweeklyoptions.json";
    private static final String configFilePath = "config.properties";

    private List<Movie> allMovies = new ArrayList<>();          // Holds all the movies
    private List<Movie> unwatchedMovies = new ArrayList<>();    // Holds only Unwatched Movies
    private List<Movie> watchedMovies = new ArrayList<>();      // Holds only Watched Movies

    private Properties properties;
    private List<Movie> thisWeeksMovieOptions = new ArrayList<>();  // Holds the potential options              || movieoptions.json
    private Map<String, Integer> movieRating = new HashMap<>();     // Holds the ratings                        || movierating.json
    private Movie previousMovie;                                    // most recently watched movie              || moviemanager.previousmovie.movietitle.releaseyear
    private Movie currentMovie;                                     // the movie we will be watching            || moviemanager.currentmovie.movietitle.releaseyear
    private boolean isRatingOpen;                                   // if people are allowed to use /ratemovie  || moviemanager.isratingopen.boolean

    // Will load our properties after allMovies has been read from movistlistcurrent.json
    public void loadProperties(Properties p) {

        properties = Objects.requireNonNull(p, "Properties passed to MovieManager is null! Unable to initialize properties");

        // Tries to load the previous movie
        try {
            setPreviousMovie(loadMovieFromProperty("moviemanager.previousmovie.movietitle.releaseyear"));
            System.out.println("Set previousMovie to: " + getPreviousMovie().getMovieTitle());

        } catch (Exception e) {
            System.out.println("Unable to set previousMovie");
           e.printStackTrace();
        }

        //Tries to load the current movie
        try {
            setCurrentMovie(loadMovieFromProperty("moviemanager.currentmovie.movietitle.releaseyear"));
            System.out.println("Set currentMovie to: " + getCurrentMovie().getMovieTitle());
        } catch (Exception e) {
            System.out.println("Unable to set currentMovie");
            e.printStackTrace();
        }

        // Tries to load our isratingopen value
        try {
            String isRatingOpenAsString = require(properties, "moviemanager.isratingopen.boolean");
            if (isRatingOpenAsString.isEmpty()) {
                isRatingOpenAsString = "false";
            }
            setIsRatingOpen(Boolean.parseBoolean(isRatingOpenAsString));
            System.out.println("Set isRatingOpen boolean to: " + isRatingOpenAsString);
        } catch (Exception e) {
            System.out.println("Unable to set isRatingOpen");
            e.printStackTrace();
        }

        // Tries to load our movie raitings
        try {
            File file = new File(movieRatingsPath);
            movieRating = mapper.readValue(file, new TypeReference<Map<String, Integer>>() {});
            System.out.println("Successfully able to initialize movieRating!");
        } catch (Exception e) {
            System.out.println("Could not initialize movieRating from " + movieRatingsPath + "Is it null?");
            e.printStackTrace();
        }

        // Tries to load our weekly options
        try {
            File file = new File(thisWeeksOptionsPath);
            thisWeeksMovieOptions = mapper.readValue(file, new TypeReference<List<Movie>>() {});
        } catch (Exception e) {
            System.out.println("Could not initialize thisWeeksMovieOptions from " + thisWeeksOptionsPath + "; Is it null?");
            e.printStackTrace();
        }
    }

    private Movie loadMovieFromProperty(String propertyKey) {
        String movieAsString = require(properties, propertyKey);
        String[] parts = movieAsString.split("\\|");
        String title = parts[0].trim();
        int year = Integer.parseInt(parts[1].trim());
        return findMovieByTitleAndYear(title, year);
    }

    // Checks to make sure the property isnt null or empty
    private String require(Properties p, String s) throws IllegalStateException {
        String value = p.getProperty(s);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Properties file exists, but unable to initialize " + s);
        }
        return value.trim();
    }

    




    
    public boolean downloadMovieListToServer(Attachment attachment) throws Error {

        // Where the current in-use movie list is located
        File movieListFileTarget = new File(allMovieListPath);

        attachment.getProxy().downloadToFile(movieListFileTarget).thenAccept(
                file -> {
                    // Executes if file download was successful
                    System.out.println(attachment.getFileName() + " sucessfully downloaded");
                }
            )
            .exceptionally(
                error -> {
                    // Execute if file download was not successful
                    throw new Error("Could not download " + allMovieListPath);
                }
            ).join(); // waits until this download is complete
        
        boolean conversionSuccessful = false;
        try {
            // Tries to convert the movie list,
            // if successful sets the boolean to true to let the caller know it was successful
            convertMovieListToObject(movieListFileTarget);
            conversionSuccessful = true;
            System.out.println("Conversion successfull attempting to sort");
            this.sortWatchedUnWatched();

            // otherwise, catches each type of error and prints them out to the console, will return false
        } catch (Exception e) {
            System.out.println("Able to download " + allMovieListPath + ". But cannot convert to Object");
            System.out.println(e);
        }

        return conversionSuccessful;
    }
    // Uses a Jackson ObjectMapper to convert from the parsed Json file to the allMovies list of Movies,
    // returns a boolean showing state of success
    public boolean convertMovieListToObject(File file) throws StreamReadException, DatabindException, IOException {
        allMovies = mapper.readValue(file, new TypeReference<List<Movie>>() {});
        return true;
    }
    
    public boolean updateAllMovies() {
        File file = new File(allMovieListPath);
        try {
            mapper.writeValue(file, allMovies);
            System.out.println("Successfully wrote current state of allMovies to " + allMovieListPath);
            return true;
        } catch (Exception e) {
            System.out.println("An error occurred while trying to write to " + allMovieListPath);
            e.printStackTrace();
            return false;
        }
    }



    public File getMovieListFile() {
        return new File(allMovieListPath);
    }
    public File getConfigPropertiesFile() {
        return new File(configFilePath);
    }
    public File getMovieRatingFile() {
        return new File(movieRatingsPath);
    }
    public File getMovieWeeklyOptionsFile() {
        return new File(thisWeeksOptionsPath);
    }
    public List<Movie> getWatchedMovies() {
        return watchedMovies;
    }
    public List<Movie> getUnwatchedMovies() {
        return unwatchedMovies;
    }
    public List<Movie> getAllMovies() {
        return allMovies;
    }
    public List<Movie> getThisWeeksMovieOptions() {
        return thisWeeksMovieOptions;
    }
    public Movie getPreviousMovie() {
        return previousMovie;
    }
    public Movie getCurrentMovie() {
        return currentMovie;
    }
    public boolean getIsRatingOpen() {
        return isRatingOpen;
    }
    public Map<String, Integer> getRatings() {
        return movieRating;
    }




    public boolean processRatingCommand(User u, int r) {
        movieRating.put(u.toString(), r);
        File file = new File(movieRatingsPath);
        try {
            mapper.writeValue(file, movieRating);
            return true;
        } catch (Exception e) {
            System.out.println("Unable to update " + movieRatingsPath + ", results may be inaccurate");
            e.printStackTrace();
        }
        return false;
    }

    public void setRatings(Map<String, Integer> map) {
        File file = new File(movieRatingsPath);
        try {
            mapper.writeValue(file, map);
            movieRating = map;
        } catch (Exception e) {
            System.out.println("Unable to update " + movieRatingsPath + ", results may be inaccurate");
            e.printStackTrace();
        }
    }
    
    public void eraseRatings() {
        File file = new File(movieRatingsPath);
        try (FileWriter fw = new FileWriter(file, false)) {    
            movieRating = new HashMap<>();
            return;
        } catch (Exception e) {
            System.out.println("Unable to erase all data in " + movieRatingsPath);
           e.printStackTrace();
        }
    }

    public boolean setIsRatingOpen(boolean b) {
        try {
            String booleanString = Boolean.toString(b);
            properties.setProperty("moviemanager.isratingopen.boolean", booleanString);
            try (FileOutputStream fos = new FileOutputStream(configFilePath)) {
                properties.store(fos, null);
                isRatingOpen = b;
                return true;
            }
        } catch (Exception e) {
            System.out.println("Unable to write boolean value to " + configFilePath);
            e.printStackTrace();
        }
        return false;
    }
    



    public boolean setCurrentMovie(Movie m) {
        if (m == null) {
            System.out.println("Movie passed to setCurrentMovie is null; ; not updating value of currentMovie");
            return false;
        }
        if (saveMovieProperty("moviemanager.currentmovie.movietitle.releaseyear", m)) {
            currentMovie = m;
            return true;
        }
        return false;
    }
    
    public boolean setPreviousMovie(Movie m) {
        if (m == null) {
            System.out.println("Movie passed to setPreviousMovie is null; not updating value of previousMovie");
            return false;
        }
        if (saveMovieProperty("moviemanager.previousmovie.movietitle.releaseyear", m)) {
            previousMovie = m;
            return true;
        }
        return false;
    }
    
    private boolean saveMovieProperty(String key, Movie m) {
        String value = m.getMovieTitle() + "|" + m.getReleaseYear();
        properties.setProperty(key, value);
        try (FileOutputStream fos = new FileOutputStream(configFilePath)) {
            properties.store(fos, null);
            return true;
        } catch (Exception e) {
            System.out.println("Unable to write movie value to " + configFilePath);
            e.printStackTrace();
            return false;
        }
    }

    public void setThisWeeksMovieOptions(List<Movie> lMovies) {

        if (lMovies == null || lMovies.size() == 0) {
            System.out.println("This weeks movies list was null; not updating the value");
            return;
        }

        File file = new File(thisWeeksOptionsPath);
        try {
            mapper.writeValue(file, lMovies);
            thisWeeksMovieOptions = lMovies;
        } catch (Exception e) {
            System.out.println("Unable to update " + thisWeeksOptionsPath + ", results may be inaccurate");
            e.printStackTrace();
        }
    }

    public void eraseThisWeeksMovieOptions() {
        File file = new File(thisWeeksOptionsPath);
        try (FileWriter fw = new FileWriter(file, false)) {
            thisWeeksMovieOptions = new ArrayList<>();
            return;
        } catch (Exception e) {
            System.out.println("Unable to erase all data in " + thisWeeksOptionsPath);
           e.printStackTrace();
        }
    }



    public boolean addMovie(String mT, int rY, int rT, String e, String bURL, boolean iW, double r, int nR) {
        // prevents duplicates hopefully
        for (Movie m : allMovies) {
            if (m.getMovieTitle().equals(mT) && m.getReleaseYear() == rY) {
                return false;
            }
        }
        try {
            allMovies.add(new Movie(mT, rY, rT, e, bURL, iW, r, nR));
            sortAllMoviesAlphabetically();
            sortWatchedUnWatched();
            System.out.println("Successfully created new Movie() instance to allMovies");
            try {
                updateAllMovies();
                return true;
            } catch (Exception ex) {
                System.out.println("Added Movie to allMovies list but was unable to update the file.");
                ex.printStackTrace();
            }
            return false;
        } catch (Exception ex) {
            System.out.println("Unable to create a new Movie(). Returning false");
            ex.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteMovie(String movieTitle, int releaseYear) {
        for (Movie m : allMovies) {
            if (m.getMovieTitle().equals(movieTitle) && releaseYear == m.getReleaseYear()) {
                if (allMovies.remove(m)) {
                    System.out.println("Deleted movie: " + m.getMovieTitle() +" With Release year: " + releaseYear);
                    sortAllMoviesAlphabetically();
                    return updateAllMovies();
                }
            }
        }
        System.out.println("Unable to find movie to delete, returning false");
        return false;
    }




    public Movie findMovieByTitleAndYear(String s, int i) throws IllegalArgumentException {
        if (s == null || i < 1888) {
            System.out.println("Couldn't find movie with passed information: " + s + " " + i +
                 "Likely caused by illegal arguments for movieTitle and releaseYear");
            return null;
        }
        List<Movie> allMoviesList = getAllMovies();
        for (Movie m : allMoviesList) {
            if (m.getMovieTitle().equals(s) && m.getReleaseYear() == i) {
                return m;
            }
        }
        System.out.println("Couldn't find movie with passed information: " + s + " " + i);
        System.out.println("Returning null");
        return null;
    }

    // Will need to run this after we change the watch status of a movie to ensure the whole list is correctly sorted
    public void sortWatchedUnWatched() {
        unwatchedMovies.clear();
        watchedMovies.clear();
        for (Movie m : allMovies) {
            if (m.getWatched()) {
                watchedMovies.add(m);
            } else {
                unwatchedMovies.add(m);
            }
        }
    }
    
    public void sortAllMoviesAlphabetically() {
        allMovies.sort(Comparator.comparing(Movie::getMovieTitle, String.CASE_INSENSITIVE_ORDER));
    }
}
