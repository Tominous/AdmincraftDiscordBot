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

public class Redditor {
    private final SubredditReference subreddit;

    public Redditor(Config config) {
        Credentials credentials = Credentials.userless(config.getRedditClientId(), config.getRedditClientSecret(), config.getDeviceId());
        UserAgent userAgent = new UserAgent("server", "org.kitteh.admincraft", "1.0.0", "mbaxj2");
        RedditClient client = OAuthHelper.automatic(new OkHttpNetworkAdapter(userAgent), credentials);
        this.subreddit = client.subreddit("admincraft");
    }

    public List<Submission> getNew() {
        return this.subreddit.posts()
                .limit(5)
                .sorting(SubredditSort.NEW)
                .timePeriod(TimePeriod.DAY)
                .build()
                .accumulateMerged(1);
    }
}
