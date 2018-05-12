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

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.models.TimePeriod;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.references.SubredditReference;

import java.util.List;

/**
 * Every account on reddit is a bot except you.
 */
public class Redditor {
    private final SubredditReference subreddit;

    /**
     * Constructs the redditor.
     *
     * @param config config for info
     */
    public Redditor(Config config) {
        Credentials credentials = Credentials.userless(config.getRedditClientId(), config.getRedditClientSecret(), config.getDeviceId());
        UserAgent userAgent = new UserAgent("server", "org.kitteh.admincraft", "1.0.0", "mbaxj2");
        RedditClient client = OAuthHelper.automatic(new OkHttpNetworkAdapter(userAgent), credentials);
        client.setLogHttp(false);
        this.subreddit = client.subreddit("admincraft");
    }

    /**
     * Gets the latest five posts.
     *
     * @return five posts
     */
    public List<Submission> getNew() {
        return this.subreddit.posts()
                .limit(5)
                .sorting(SubredditSort.NEW)
                .timePeriod(TimePeriod.DAY)
                .build()
                .accumulateMerged(1);
    }
}
