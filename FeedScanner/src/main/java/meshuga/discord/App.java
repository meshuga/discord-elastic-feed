package meshuga.discord;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.overzealous.remark.Remark;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import okhttp3.*;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class App implements RequestHandler<Object, Object> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final LambdaLogger LOG = LambdaRuntime.getLogger();
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final Map<String, String> feeds = new HashMap<String, String>() {{
        put("Blog post", "https://www.elastic.co/blog/feed");
        put("New Release", "https://www.elastic.co/downloads/past-releases/feed");
        put("Press release", "https://www.elastic.co/about/press/feed");
    }};

    public Object handleRequest(final Object input, final Context context) {
        OkHttpClient client = new OkHttpClient();
        feeds.entrySet()
                .forEach(feedEntry -> processFeed(feedEntry, client));
        return "OK";
    }

    private void processFeed(Map.Entry<String, String> feedEntry, OkHttpClient client) {
        try {
            URL feedSource = new URL(feedEntry.getValue());
            SyndFeedInput feedInput = new SyndFeedInput();
            SyndFeed feed = feedInput.build(new XmlReader(feedSource));
            publishNews(feed, feedEntry.getKey(), client);
        } catch (FeedException | IOException e) {
            LOG.log(e.getMessage());
        }
    }

    private void publishNews(SyndFeed feed, String feedName, OkHttpClient client) {
        Remark remark = new Remark();
        String webhookUrl = System.getenv("WEBHOOK_URL");
        feed.getEntries().forEach(syndEntry -> {
            Date publishedDate = syndEntry.getPublishedDate();
            if (publishedDate != null) {
                Instant instant = publishedDate.toInstant();
                if (instant.isAfter(Instant.now()
                        .minus(1, ChronoUnit.HOURS))) {
                    try {
                        Embed embed = new Embed()
                                .setTitle(substring(syndEntry.getTitle(), 255))
                                .setDescription(substring(remark.convert(syndEntry.getDescription().getValue()), 1000))
                                .setUrl(syndEntry.getUri());
                        DiscordRequest discordRequest = new DiscordRequest()
                                .setUsername(feedName);
                        discordRequest.getEmbeds().add(embed);
                        Call call = client.newCall(new Request.Builder()
                                .url(webhookUrl)
                                .post(RequestBody.create(MEDIA_TYPE, MAPPER.writeValueAsString(discordRequest)))
                                .build());
                        call.execute();
                    } catch (Exception e) {
                        LOG.log("Couldn't parse " + syndEntry.getTitle() + ": " + e.getMessage());
                    }
                }
            }
        });
    }

    private String substring(String str, int limit) {
        return str != null && str.length() > limit ? str.substring(0, 255) : str;
    }
}