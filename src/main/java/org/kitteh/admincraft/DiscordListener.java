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

import org.apache.commons.lang3.exception.ExceptionUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageSendEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.impl.events.guild.member.NicknameChangedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.impl.events.shard.ShardReadyEvent;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageHistory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discord event listening.
 */
public class DiscordListener {
    private static final ReactionEmoji TOOT_TOOT = ReactionEmoji.of("\uD83C\uDFBA"); // Trumpet emoji
    private static final String UPBOAT = "upvote";
    private static final String DOWNBOAT = "downvote";
    private static final String BAN = "banhammer";
    private static final String DONTBAN = "true_love";

    private static final Pattern USERID_PATTERN = Pattern.compile("\\[([0-9]+)]");

    private Map<Long, UserMonitor> monitor = new ConcurrentHashMap<>();
    private List<String> admincraftRoles = new ArrayList<>();
    private Instant lastResponse = Instant.now();

    @EventSubscriber
    public void login(ShardReadyEvent event) {
        Admincraft.queue(() -> event.getClient().changeUsername(Admincraft.config.getName()));
        Admincraft.queue(() -> event.getClient().changePresence(StatusType.ONLINE, ActivityType.PLAYING, "Minecraft!"));
        try {
            IChannel channel = event.getClient().getChannelByID(Admincraft.config.getRoleChannelId());
            IGuild guild = channel.getGuild();
            MessageHistory history = channel.getFullMessageHistory();
            Iterator<IMessage> it = history.iterator();
            IUser self = event.getClient().getOurUser();
            while (it.hasNext()) {
                IMessage message = it.next();
                IRole role = this.getAreYouARole(message);
                if (role != null) {
                    this.admincraftRoles.add(role.getName());
                    if (message.getReactions().isEmpty()) {
                        Admincraft.queue(() -> message.addReaction(TOOT_TOOT));
                        continue;
                    }
                    for (IUser user : message.getReactionByEmoji(TOOT_TOOT).getUsers()) {
                        if (user != null && !self.equals(user)) {
                            if (channel.getGuild().getUsersByRole(role).contains(user)) {
                                Admincraft.queue(() -> user.removeRole(role));
                            } else {
                                Admincraft.queue(() -> user.addRole(role));
                            }
                            Admincraft.queue(() -> message.removeReaction(user, TOOT_TOOT));
                        }
                    }
                }
            }
            IRole active = guild.getRoleByID(Admincraft.config.getAdmincraftRole());
            List<IUser> activeUsers = guild.getUsersByRole(active);
            guild.getRoles().stream()
                    .flatMap(role -> guild.getUsersByRole(role).stream())
                    .distinct()
                    .filter(user -> !activeUsers.contains(user) && user.getRolesForGuild(guild).size() > 1)
                    .forEach(user -> Admincraft.queue(() -> user.addRole(active)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventSubscriber
    public void message(MessageSendEvent event) {
        if (event.getChannel().getLongID() == Admincraft.config.getPostChannelId()) {
            this.updoot(event.getMessage());
        }
        if (event.getChannel().getLongID() == Admincraft.config.getThonkChannel() && event.getMessage().getContent().startsWith("I'm")) {
            Admincraft.queue(() -> event.getMessage().addReaction(ReactionEmoji.of(event.getMessage().getGuild().getEmojiByName(BAN))));
        }
        if (event.getChannel().getLongID() == Admincraft.config.getWelcomeChannelId() && event.getMessage().getContent().startsWith("Welcome") && !event.getMessage().getMentions().isEmpty()) {
            IUser target = event.getMessage().getMentions().get(0);
            if (this.monitor.containsKey(target.getLongID())) {
                this.monitor.get(target.getLongID()).setWelcome(event.getMessageID());
            }
        }
    }

    @EventSubscriber
    public void messageReceived(MessageReceivedEvent event) {
        if (event.getChannel().getLongID() == Admincraft.config.getRoleChannelId()) {
            Admincraft.queue(() -> event.getMessage().addReaction(TOOT_TOOT));
        }
        // HALP
        if (event.getChannel().getLongID() != Admincraft.config.getHalpChannelId() &&
                event.getMessage().getRoleMentions().contains(event.getGuild().getRoleByID(Admincraft.config.getHalpRole()))) {
            if (!event.getAuthor().isBot()) {
                Admincraft.queue(() -> event.getAuthor().addRole(event.getGuild().getRoleByID(Admincraft.config.getHalpRole())));
            }
            Admincraft.queue(() -> event.getMessage().delete());
            String content = event.getAuthor().mention() + " says:\n" + event.getMessage().getContent();
            if (content.length() > 2000) {
                content = content.substring(0, 2000);
            }
            Admincraft.sendMessage(event.getGuild().getChannelByID(Admincraft.config.getHalpChannelId()), content);
        }
        // Reaction
        if (event.getMessage().getContent().startsWith("!")) {
            String response = Admincraft.getResponse(event.getMessage().getContent().substring(1));
            if (response != null && Duration.between(lastResponse, Instant.now()).toMillis() > 20000) {
                Admincraft.sendMessage(event.getChannel(), response);
                lastResponse = Instant.now();
            }
        }
        out:
        if (event.getChannel().getLongID() == Admincraft.config.getDebugChannelId()) {
            // debug stuffs?!?
            String message = event.getMessage().getContent();
            if (message.toLowerCase().startsWith(";reaction")) {
                String[] split = message.toLowerCase().split(" ");
                if (split.length < 3) {
                    break out;
                }
                switch (split[1].toLowerCase()) {
                    case "add":
                        if (split.length < 4) {
                            break out;
                        }
                        try {
                            Admincraft.addResponse(split[2], String.join(" ", Arrays.copyOfRange(message.split(" "), 3, split.length)));
                            this.updoot(event.getMessage());
                        } catch (IOException e) {
                            this.downdoot(event.getMessage());
                            Admincraft.sendMessage(event.getChannel(), ExceptionUtils.getStackTrace(e));
                        }
                        break;
                    case "del":
                    case "remove":
                    case "delete":
                        try {
                            if (Admincraft.removeResponse(split[2])) {
                                this.updoot(event.getMessage());
                                Admincraft.sendMessage(event.getChannel(), "Deleted `" + split[2] + "`");
                            } else {
                                this.downdoot(event.getMessage());
                                Admincraft.sendMessage(event.getChannel(), "But it doesn't exist...");
                            }
                        } catch (IOException e) {
                            this.downdoot(event.getMessage());
                            Admincraft.sendMessage(event.getChannel(), ExceptionUtils.getStackTrace(e));
                        }
                        break;

                }
            }
        }
        if (event.getChannel().getLongID() == Admincraft.config.getThonkChannel()) {
            IMessage message = event.getMessage();
            if (message.getContent().startsWith("!addmember ")) {
                if (!message.getMentions().isEmpty()) {
                    IUser target = message.getMentions().get(0);
                    Admincraft.sendMessage(event.getChannel(), "Added " + target.getDisplayName(event.getGuild()) + "(" + target.mention() + ") at request of " + event.getAuthor().getDisplayName(event.getGuild()) +
                            "\nWelcome to the :thonk: channel, for handling spammers. You have been invited to help manage any incoming spammers." +
                            "\nReact appropriately to the post by the bot." +
                            "\nIf you have no interest in helping, go ahead and mute the channel." +
                            "\nThis channel is just for handling spammers, and is not a cool kids club. The cool kids club is #off-topic and this channel should be only for handling spammers." +
                            "\nAdd only folks who are active and trustworthy.");
                    target.addRole(event.getGuild().getRoleByID(Admincraft.config.getThonkRole()));
                }
            }
        }
        UserMonitor monitor = this.getMonitor(event.getAuthor());
        if (monitor != null) {
            monitor.addMessage();
            monitor.addMention(event.getMessage().getMentions().size());
            if (monitor.needsFlag()) {
                Admincraft.sendMessage(event.getGuild().getChannelByID(Admincraft.config.getThonkChannel()),
                        "I'm looking at brand new user " + event.getAuthor().getDisplayName(event.getGuild()) + " (" + event.getAuthor().mention() + "). Are they a spammer? Hammer to ban, heart to mark as safe. They haven't necessarily done anything wrong yet, so play VERY safe. [" + event.getAuthor().getLongID() + "]");
                //        "I'm worried about " + event.getAuthor().getDisplayName(event.getGuild()) + " (" + event.getAuthor().mention() + ") - " + monitor.getMentions() + " mentions in " + monitor.getMessages() + " messages within " + monitor.getMinutes() + " minutes. Are they a spammer? Upvote to destroy, downvote if I'm wrong.");
            }
        }
    }

    private IRole getAreYouARole(IMessage message) {
        if (message.getContent().contains("Are you a ")) {
            String part = message.getContent().substring(message.getContent().indexOf("Are you a ") + "Are you a ".length());
            String roleName = part.substring(0, part.indexOf('?'));
            List<IRole> roles = message.getGuild().getRolesByName(roleName);
            if (roles.size() == 1) {
                return roles.get(0);
            }
        }
        return null;
    }

    @EventSubscriber
    public void react(ReactionAddEvent event) {
        if (event.getChannel().getLongID() == Admincraft.config.getPostChannelId() &&
                event.getUser().equals(event.getClient().getOurUser()) &&
                event.getReaction().getEmoji().equals(ReactionEmoji.of(event.getGuild().getEmojiByName(UPBOAT)))) {
            this.downdoot(event.getMessage());
        }
        if (event.getChannel().getLongID() == Admincraft.config.getThonkChannel() &&
                event.getMessage().getContent().startsWith("I'm") &&
                event.getReaction().getEmoji().equals(ReactionEmoji.of(event.getGuild().getEmojiByName(BAN)))) {
            Admincraft.queue(() -> event.getMessage().addReaction(ReactionEmoji.of(event.getGuild().getEmojiByName(DONTBAN))));
        }
        if (event.getUser().equals(event.getClient().getOurUser())) {
            return; // Hey it's me
        }
        if (event.getChannel().getLongID() == Admincraft.config.getRoleChannelId()) {
            IMessage message = event.getMessage();
            IUser user = event.getUser();
            IRole role = this.getAreYouARole(message);
            if (event.getGuild().getUsersByRole(role).contains(user)) {
                if (user.getRolesForGuild(event.getGuild()).stream().map(IRole::getName).filter(this.admincraftRoles::contains).count() == 1) {
                    Admincraft.queue(() -> user.removeRole(event.getGuild().getRoleByID(Admincraft.config.getAdmincraftRole())));
                }
                Admincraft.queue(() -> user.removeRole(role));
            } else {
                Admincraft.queue(() -> user.addRole(role));
                IRole admincraftRole = event.getGuild().getRoleByID(Admincraft.config.getAdmincraftRole());
                if (!user.getRolesForGuild(event.getGuild()).contains(admincraftRole)) {
                    Admincraft.queue(() -> user.addRole(admincraftRole));
                }
            }
            Admincraft.queue(() -> event.getMessage().removeReaction(user, TOOT_TOOT));
        }
        dance:
        if (event.getChannel().getLongID() == Admincraft.config.getThonkChannel()) {
            if (!event.getMessage().getContent().startsWith("HANDLED") &&
                    event.getMessage().getAuthor().equals(event.getClient().getOurUser()) &&
                    !event.getMessage().getMentions().isEmpty()) {
                IUser target = event.getMessage().getMentions().get(0);
                boolean update = true;
                long l;
                if (target == null) {
                    Matcher matcher = USERID_PATTERN.matcher(event.getMessage().getContent());
                    if (matcher.find()) {
                        try {
                            l = Long.parseLong(matcher.group(1));
                        } catch (NumberFormatException ignored) {
                            break dance;
                        }
                    } else {
                        break dance;
                    }
                } else {
                    l = target.getLongID();
                }
                if (event.getReaction().getEmoji().equals(ReactionEmoji.of(event.getGuild().getEmojiByName(BAN)))) {
                    if (target == null) {
                        Admincraft.sendMessage(event.getChannel(), "User ID " + l + " banned. They seem to have left. Done at request of " + event.getUser().getDisplayName(event.getGuild()));
                    } else {
                        Admincraft.sendMessage(event.getChannel(), "Banned user " + target.getDisplayName(event.getGuild()) + " at request of " + event.getUser().getDisplayName(event.getGuild()));
                    }
                    Admincraft.queue(() -> event.getGuild().banUser(l, 1));
                    this.remove(l, event.getGuild());
                } else if (event.getReaction().getEmoji().equals(ReactionEmoji.of(event.getGuild().getEmojiByName(DONTBAN)))) {
                    if (target == null) {
                        Admincraft.sendMessage(event.getChannel(), "No longer considering user ID " + l + " (seem to have left) at request of " + event.getUser().getDisplayName(event.getGuild()));
                    } else {
                        Admincraft.sendMessage(event.getChannel(), "No longer considering user " + target.getDisplayName(event.getGuild()) + " at request of " + event.getUser().getDisplayName(event.getGuild()));
                    }
                } else {
                    update = false;
                }
                if (update) {
                    Admincraft.queue(() -> event.getMessage().edit("HANDLED: " + event.getMessage().getContent()));
                    Admincraft.queue(() -> event.getMessage().removeAllReactions());
                }
            }
        }
    }

    private void remove(long id, IGuild guild) {
        UserMonitor monitor = this.monitor.remove(id);
        if (monitor != null) {
            long welcomeId = monitor.getWelcome();
            if (welcomeId != 0) {
                IMessage message = guild.getMessageByID(welcomeId);
                if (message != null) {
                    Admincraft.queue(message::delete);
                }
            }
        }
    }

    @EventSubscriber
    public void join(UserJoinEvent event) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.withColor(50, 150, 75);
        builder.withFooterText(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(event.getJoinTime()));
        builder.appendField("Joined:", event.getUser().getName(), true);
        Admincraft.log(event, builder.build());
        this.monitor.put(event.getUser().getLongID(), new UserMonitor());
        Admincraft.sendMessage(event.getGuild().getChannelByID(Admincraft.config.getWelcomeChannelId()), "Welcome, " + event.getUser().mention() + " :) Tell us about yourself!");
    }

    @EventSubscriber
    public void leave(UserLeaveEvent event) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.withColor(150, 50, 75);
        builder.withFooterText(LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM)));
        builder.appendField("Left:", event.getUser().getName(), true);
        Admincraft.log(event, builder.build());
        this.monitor.remove(event.getUser().getLongID());
    }

    @EventSubscriber
    public void nick(NicknameChangedEvent event) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.withAuthorName("Nickname change!");
        builder.withColor(66, 134, 244);
        builder.withFooterText(LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM)));
        builder.appendField("Old nick:", event.getOldNickname().orElse(event.getUser().getName()), true);
        builder.appendField("New nick:", event.getNewNickname().orElse(event.getUser().getName()), true);
        Admincraft.log(event, builder.build());
    }

    private UserMonitor getMonitor(IUser user) {
        UserMonitor monitor = this.monitor.get(user.getLongID());
        if (monitor != null && monitor.canRemove()) {
            this.monitor.remove(user.getLongID());
            monitor = null;
        }
        return monitor;
    }

    private void updoot(IMessage message) {
        Admincraft.queue(() -> message.addReaction(ReactionEmoji.of(message.getGuild().getEmojiByName(UPBOAT))));
    }

    private void downdoot(IMessage message) {
        Admincraft.queue(() -> message.addReaction(ReactionEmoji.of(message.getGuild().getEmojiByName(DOWNBOAT))));
    }
}
