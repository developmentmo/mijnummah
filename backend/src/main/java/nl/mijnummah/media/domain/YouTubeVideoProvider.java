package nl.mijnummah.media.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * YouTube. Accepts the URL forms people copy from YouTube (watch, youtu.be, embed, shorts, live, mobile, music)
 * and bare 11-character video IDs. The embed URL uses youtube-nocookie.com (privacy-enhanced mode), so playing
 * an embedded video does not set YouTube tracking cookies before the user starts it.
 */
public final class YouTubeVideoProvider implements VideoProvider {

    private static final Pattern VIDEO_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");
    private static final Set<String> HOSTS = Set.of("youtube.com", "www.youtube.com", "m.youtube.com",
            "music.youtube.com", "youtube-nocookie.com", "www.youtube-nocookie.com");
    private static final Set<String> PATH_PREFIXES = Set.of("embed", "shorts", "live", "v");

    @Override
    public VideoProviderType type() {
        return VideoProviderType.YOUTUBE;
    }

    @Override
    public Optional<String> parseVideoId(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String value = input.trim();
        if (VIDEO_ID.matcher(value).matches()) {
            return Optional.of(value);
        }
        URI uri;
        try {
            uri = new URI(value.contains("://") ? value : "https://" + value);
        } catch (URISyntaxException ex) {
            return Optional.empty();
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("http")) {
            return Optional.empty();
        }
        String[] segments = uri.getPath() == null ? new String[0] : uri.getPath().replaceFirst("^/", "").split("/");
        String candidate = null;
        if (host.equals("youtu.be") && segments.length >= 1) {
            candidate = segments[0];
        } else if (HOSTS.contains(host)) {
            if (segments.length >= 1 && segments[0].equals("watch")) {
                candidate = queryParameter(uri.getRawQuery(), "v");
            } else if (segments.length >= 2 && PATH_PREFIXES.contains(segments[0])) {
                candidate = segments[1];
            }
        }
        return Optional.ofNullable(candidate).filter(id -> VIDEO_ID.matcher(id).matches());
    }

    @Override
    public String watchUrl(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    @Override
    public String embedUrl(String videoId) {
        return "https://www.youtube-nocookie.com/embed/" + videoId;
    }

    @Override
    public String defaultThumbnailUrl(String videoId) {
        return "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
    }

    private static String queryParameter(String rawQuery, String name) {
        if (rawQuery == null) {
            return null;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }
}
