/*
 * * Copyright (C) 2018 Matt Baxter http://kitteh.org
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
import net.dean.jraw.models.Submission;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.GuildEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageHistory;
import sx.blah.discord.util.RequestBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class Admincraft {
    private static IDiscordClient client;
    public static Config config; //TODO stop sharing the damn modifiable config!
    private static Database database;
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
        try {
            database = new Database(config);
        } catch (SQLException ohNo) {
            System.out.println("Oh no, db");
            ohNo.printStackTrace();
            System.exit(66);
        }

        Redditor reddit = new Redditor(config);

        client = new ClientBuilder().withToken(config.getApiToken()).build();
        client.getDispatcher().registerListener(new DiscordListener());
        client.login();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    for (Submission post : database.processNew(reddit.getNew())) {
                        sendMessage(client.getChannelByID(config.getPostChannelId()), "**" + post.getAuthor() + "** writes: **" + post.getTitle() + "**\n" +
                                "https://www.reddit.com/r/admincraft/comments/" + post.getId());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    IChannel channel = client.getChannelByID(config.getRoleChannelId());
                    MessageHistory history = channel.getFullMessageHistory();
                    Iterator<IMessage> it = history.iterator();
                    while (it.hasNext()) {
                        IMessage message = it.next();
                        if (message.getContent().contains("Are you a ")) {
                            String part = message.getContent().substring(message.getContent().indexOf("Are you a ") + "Are you a ".length());
                            String roleName = part.substring(0, part.indexOf('?'));
                            List<IRole> roles = channel.getGuild().getRolesByName(roleName);
                            if (roles.size() == 1) {
                                IRole role = roles.get(0);
                                List<IUser> usersInRole = new LinkedList<>(role.getGuild().getUsersByRole(role));
                                Set<IUser> usersWantingRole = new HashSet<>();
                                message.getReactions().forEach(reaction -> usersWantingRole.addAll(reaction.getUsers()));

                                for (IUser user : usersWantingRole) {
                                    if (user != null && !usersInRole.contains(user)) {
                                        queue(() -> user.addRole(role));
                                    }
                                }
                                for (IUser user : usersInRole) {
                                    if (user != null && !usersWantingRole.contains(user)) {
                                        queue(() -> user.removeRole(role));
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 5000, 60000);
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

    public static void queue(ThingDoer thingToDo) {
        RequestBuffer.request(() -> {
            try {
                thingToDo.doTheThing();
            } catch (DiscordException e) {
                // TODO Handle cleaner
                e.printStackTrace();
            }
        });
    }
}
