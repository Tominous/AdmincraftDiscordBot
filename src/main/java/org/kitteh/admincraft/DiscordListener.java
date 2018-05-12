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

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.member.NicknameChangedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.impl.events.shard.LoginEvent;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.EmbedBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Discord event listening.
 */
public class DiscordListener {
    @EventSubscriber
    public void login(LoginEvent event) {
        Admincraft.queue(() -> event.getClient().changeUsername(Admincraft.config.getName()));
        Admincraft.queue(() -> event.getClient().changePresence(StatusType.ONLINE, ActivityType.WATCHING, "you!"));
    }

    @EventSubscriber
    public void join(UserJoinEvent event) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.withColor(50, 150, 75);
        builder.withFooterText(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(event.getJoinTime()));
        builder.appendField("Joined:", event.getUser().getName(), true);
        Admincraft.log(event, builder.build());
    }

    @EventSubscriber
    public void leave(UserLeaveEvent event) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.withColor(150, 50, 75);
        builder.withFooterText(LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM)));
        builder.appendField("Left:", event.getUser().getName(), true);
        Admincraft.log(event, builder.build());
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
}
