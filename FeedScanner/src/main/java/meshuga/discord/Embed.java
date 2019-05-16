package meshuga.discord;

public class Embed {
    private String title;
    private String description;
    private String url;

    public String getTitle() {
        return title;
    }

    public Embed setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Embed setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public Embed setUrl(String url) {
        this.url = url;
        return this;
    }
}
