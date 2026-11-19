package com.gathin.tvhome;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extract only the public result data. Page scripts are never evaluated. */
final class YouTubeSearchData {
    private static final Pattern ASSIGNMENT = Pattern.compile(
            "(?:var\\s+ytInitialData|window\\[\"ytInitialData\"\\])\\s*=\\s*");

    static String initialData(String html) throws IOException {
        Matcher match = ASSIGNMENT.matcher(html);
        if (!match.find() || match.end() >= html.length() || html.charAt(match.end()) != '{') {
            throw new IOException("YouTube liefert gerade keine öffentlichen Suchergebnisse. Später erneut suchen oder einen Video-Link eingeben.");
        }
        int start = match.end(), depth = 0;
        boolean string = false, escaped = false;
        for (int i = start; i < html.length(); i++) {
            char c = html.charAt(i);
            if (string) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') string = false;
            } else if (c == '"') string = true;
            else if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return html.substring(start, i + 1);
        }
        throw new IOException("YouTube hat unvollständige Suchergebnisse geliefert. Bitte erneut versuchen.");
    }
}
