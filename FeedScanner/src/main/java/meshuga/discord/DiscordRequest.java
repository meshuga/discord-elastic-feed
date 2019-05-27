package meshuga.discord;

import java.util.ArrayList;
import java.util.List;

public class DiscordRequest {
    private boolean tts;
    private String username;
    private String content;
    private List<Embed> embeds = new ArrayList<>();

    public boolean isTts() {
        return tts;
    }

    public DiscordRequest setTts(boolean tts) {
        this.tts = tts;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public DiscordRequest setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getContent() {
        return content;
    }

    public DiscordRequest setContent(String content) {
        this.content = content;
        return this;
    }

    public List<Embed> getEmbeds() {
        return embeds;
    }

    public DiscordRequest setEmbeds(List<Embed> embeds) {
        this.embeds = embeds;
        return this;
    }
}
