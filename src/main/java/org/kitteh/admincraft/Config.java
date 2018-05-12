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

import com.google.gson.GsonBuilder;

import java.util.UUID;

/**
 * Config class! Woo
 */
public class Config {
    // TODO  better config strategy.

    private String apiToken;
    private UUID deviceId = UUID.randomUUID();
    private long logChannelId;
    private long postChannelId;
    private String dbPass;
    private String name;
    private String redditClientId;
    private String redditClientSecret;

    public String getApiToken() {
        return this.apiToken;
    }

    public UUID getDeviceId() {
        return this.deviceId;
    }

    public long getLogChannelId() {
        return this.logChannelId;
    }

    public long getPostChannelId() {
        return this.postChannelId;
    }

    public String getDbPass() {
        return this.dbPass;
    }

    public String getName() {
        return this.name;
    }

    public String getRedditClientId() {
        return this.redditClientId;
    }

    public String getRedditClientSecret() {
        return this.redditClientSecret;
    }

    // This is just a cute little helper to make yourself a fresh config
    public static void main(String[] args) {
        System.out.println(new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(new Config()));
    }
}
