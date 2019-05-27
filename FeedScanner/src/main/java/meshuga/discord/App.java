package meshuga.discord;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.overzealous.remark.Remark;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class App implements RequestHandler<Object, Object> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final LambdaLogger LOG = LambdaRuntime.getLogger();
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    @SuppressWarnings("unchecked")
    public Object handleRequest(final Object input, final Context context) {
        Map<String, String> feeds = (Map) input;
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
        int scheduleHours = Integer.parseInt(System.getenv("SCHEDULE_HOURS"));
        feed.getEntries().forEach(syndEntry -> {
            Date postDate = Optional.ofNullable(syndEntry.getPublishedDate())
                    .orElse(syndEntry.getUpdatedDate());
            if (postDate != null) {
                Instant instant = postDate.toInstant();
                if (instant.isAfter(Instant.now()
                        .minus(scheduleHours, ChronoUnit.HOURS))) {
                    try {
                        String contentHtml = Optional.ofNullable(syndEntry.getDescription())
                                .orElseGet(() -> syndEntry.getContents().isEmpty() ?
                                        new SyndContentImpl() : syndEntry.getContents().get(0))
                                .getValue();
                        Embed embed = new Embed()
                                .setTitle(StringUtils.left(syndEntry.getTitle(), 255))
                                .setDescription(StringUtils.left(remark.convert(contentHtml), 1000))
                                .setUrl(syndEntry.getLink());
                        DiscordRequest discordRequest = new DiscordRequest()
                                .setUsername(feedName);
                        if (feedName.contains("Reddit")) {
                            discordRequest.getEmbeds().add(embed);
                        } else {
                            discordRequest.setContent(syndEntry.getLink());
                        }
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
}
