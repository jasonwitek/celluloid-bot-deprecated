# Celluloid
A Movie Night Scheduler for Discord

# Setup

1. Download the source code and open the DiscordBot.java file

2. Edit the String token on line 24 to be your personal bot key. **DO NOT SHOW ANYONE THIS KEY**

3. In the config.properties file, replace the botcommands.adminid field with your discord user id. This will ensure that you are the only person who can run specific commands that change the state of the bot.

4. Save the file, build, and run, your bot should now be online.

# How to use

There are several commands that only the owner of the bot can use, for example: 
- Setting the announcement channel
- Setting the event channel
- Setting the viewing channel
- Setting the movie ping role
- Dumping a list of movie objects in the pool
- Setting the current movie
- Setting the previous movie
- Modifying data of movies
- Enabling / Disabling movie rating

There are also commands available to all users in the server:
- Viewing the watched movies
- Rating a movie (provided rating is enabled)
- Viewing the top ten rated movies
- /help which displays the available commands
- Searching for a specific movie in the pool

This build currently uses the JDA (Java Discord API) and was in the middle of integrating TMDB (The Movie Database) API to automate the movie pool. This build is deprecated, for an updated version, please see: https://github.com/jasonwitek/celluloid-bot
