package com.example.android.cricketscoreboard.scoring;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure-Java deterministic key-value serializer with escaping so scoring engine models
 * serialize identically in both host JVM unit tests and Android SQLite / Firestore payloads.
 */
public final class SimpleKvSerializer {

    private SimpleKvSerializer() {
    }

    public static String encode(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) {
                sb.append('\n');
            }
            first = false;
            sb.append(escape(entry.getKey()))
                    .append('=')
                    .append(escape(entry.getValue() == null ? "" : entry.getValue()));
        }
        return sb.toString();
    }

    public static Map<String, String> decode(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return map;
        }
        String[] lines = raw.split("\n");
        for (String line : lines) {
            int eq = line.indexOf('=');
            if (eq > 0) {
                String k = unescape(line.substring(0, eq));
                String v = unescape(line.substring(eq + 1));
                map.put(k, v);
            }
        }
        return map;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("=", "\\e");
    }

    private static String unescape(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 'e':
                        out.append('=');
                        break;
                    case '\\':
                    default:
                        out.append(next);
                        break;
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
