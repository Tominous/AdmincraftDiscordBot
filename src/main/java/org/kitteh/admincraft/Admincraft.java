/*
 * * Copyright (C) 2018-2019 Matt Baxter http://kitteh.org
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.kitteh.admincraft;

import com.google.gson.Gson;
import net.dean.jraw.ApiException;
import net.dean.jraw.models.Submission;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.GuildEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RateLimitException;
import sx.blah.discord.util.RequestBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.SQLException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Admincraft {
    private static IDiscordClient client;
    public static Config config; //TODO stop sharing the damn modifiable config!
    private static Database database;
    private static Redditor reddit;
    private static Timer timer;

    public static void main(String[] args) {
        restartDiscord();
    }

    private static void restartDiscord() {
        if (client != null) {
            client.logout();
        }
        if (database != null) {
            database.shutdown();
        }
        if (timer != null) {
            timer.cancel();
        }

        // TODO better config strategy
        try {
            config = new Gson().fromJson(new FileReader(new File("config.json")), Config.class);
        } catch (FileNotFoundException e) {
            System.out.println("Oh no, no config file");
            System.exit(66);
        }

        boolean redditEnabled = config.shouldListenForReddit();
        if (redditEnabled) {
            try {
                database = new Database(config);
            } catch (SQLException ohNo) {
                System.out.println("Oh no, db");
                ohNo.printStackTrace();
                System.exit(66);
            }
            reddit = new Redditor(config);
        }

        client = new ClientBuilder().withToken(config.getApiToken()).build();
        client.getDispatcher().registerListener(new DiscordListener());
        client.login();

        if (redditEnabled) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        List<Submission> newPosts;
                        try {
                            newPosts = reddit.getNew();
                        } catch (ApiException ohNo) {
                            reddit = new Redditor(config);
                            newPosts = reddit.getNew(); // Try again, could still fail.
                            // https://github.com/mattbdean/JRAW/issues/264
                        }
                        for (Submission post : database.processNew(newPosts)) {
                            String escapeAllTheTitles = post.getTitle()
                                    .replaceAll("\\*", "\\\\*")
                                    .replaceAll("_", "\\\\_")
                                    .replaceAll("~", "\\\\~")
                                    .replaceAll("`", "\\\\`");
                            EmbedObject embed = new EmbedBuilder()
                                    .withColor(233, 38, 79)
                                    .withAuthorName(post.getAuthor() + " writes:")
                                    .withAuthorUrl("http://www.reddit.com/user/" + post.getAuthor())
                                    .withTitle(escapeAllTheTitles + "\n")
                                    .withUrl("https://www.reddit.com/r/admincraft/comments/" + post.getId())
                                    .withTimestamp(post.getCreated().toInstant())
                                    .build();

                            sendMessage(client.getChannelByID(config.getPostChannelId()), embed);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 5000, 240000);
        }
    }

    public static void log(GuildEvent event, EmbedObject embed) {
        sendMessage(event.getGuild().getChannelByID(config.getLogChannelId()), embed);
    }

    public static void sendMessage(IChannel channel, String message) {
        queue(() -> channel.sendMessage(message));
    }

    public static void sendMessage(IChannel channel, EmbedObject embed) {
        queue(() -> channel.sendMessage(embed));
    }

    public static void queue(Runnable thingToDo) {
        RequestBuffer.request(() -> {
            try {
                thingToDo.run();
            } catch (RateLimitException e) {
                throw e;
            } catch (DiscordException e) {
                // TODO Handle cleaner
                e.printStackTrace();
            }
        });
    }
}
