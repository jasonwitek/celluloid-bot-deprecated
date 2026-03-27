package me.jasonwitek;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Movie {
    
    private String movieTitle;
    private int releaseYear;
    private int runtime;
    private String emoji;
    private String bannerURL;
    private boolean watched;
    private double totalRating;  // Holds total number of **stars**
    private int numberOfRatings; // Holds number of **votes**

    public Movie(String inputMovieTitle,
                 int inputReleaseYear,
                 int inputRunTime,
                 String inputEmoji,
                 String inputBannerURL,
                 Boolean inputWatchedStatus,
                 double inputTotalRating, 
                 int inputNumberOfRatings) {

        movieTitle = Objects.requireNonNull(inputMovieTitle, "Movie Initializer Error: Movie Title is null");
        emoji = Objects.requireNonNull(inputEmoji, "Movie Initializer Error: Emoji is null");
        bannerURL = Objects.requireNonNull(inputBannerURL, "Movie Initializer Error: BannerURL is null");

        if (inputReleaseYear <= 1888 || inputRunTime <= 0 || inputTotalRating < 0 || inputNumberOfRatings < 0) {
            throw new IllegalArgumentException("Movie Initilizer Error: One or more primitives are out of expected range");
        }

        releaseYear = inputReleaseYear;
        runtime = inputRunTime;
        totalRating = inputTotalRating;
        numberOfRatings = inputNumberOfRatings;

        if (inputWatchedStatus == null) {
            watched = false;
        } else {
            watched = inputWatchedStatus;
        }
    }

    // Required for jackson object mapper
    public Movie() {}

    public String getMovieTitle() {return movieTitle;}
    public int getReleaseYear() {return releaseYear;}
    public int getRuntime() {return runtime;}
    public String getEmoji() {return emoji;}
    public String getBannerURL() {return bannerURL;}
    public boolean getWatched() {return watched;}
    public double getTotalRating() {return totalRating;}
    public int getNumberOfRatings() {return numberOfRatings;}

    public void setWatched(boolean b) {watched = b;}
    public void setMovieTitle(String s) {movieTitle = s;}
    public void setReleaseYear(int i) {releaseYear = i;}
    public void setRuntime(int i) {runtime = i;}
    public void setEmoji(String s) {emoji = s;}
    public void setBannerURL(String s) {bannerURL = s;}
    public void setRating(double r) {totalRating = r;}
    public void setNumberOfRatings(int nR) {numberOfRatings = nR;}

    @JsonIgnore
    public double calculateAverageRating() {
        if (getNumberOfRatings() == 0) {return 0.0;}
        return Math.round((double) (getTotalRating() / getNumberOfRatings()) * 100) / 100.0;
    }
}
