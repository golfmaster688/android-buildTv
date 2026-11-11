package com.mediabox.tv.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser สำหรับ playlist แบบ M3U/M3U8 (IPTV)
 *
 * <p>รูปแบบที่รองรับ:
 * <pre>
 * #EXTM3U
 * #EXTINF:-1 tvg-id="ch1" tvg-name="BBC One" tvg-logo="http://x/1.png" group-title="News",BBC One HD
 * http://example.com/stream.m3u8
 * </pre>
 *
 * <p>คลาสนี้เป็น pure Java (ไม่พึ่ง Android API) จึงเขียน unit test บน JVM ได้
 */
public final class M3UParser {

    private static final Pattern EXTINF = Pattern.compile("^#EXTINF:", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTR = Pattern.compile("([A-Za-z0-9_-]+)=\"([^\"]*)\"");

    private M3UParser() {
        // utility class
    }

    public static List<Channel> parse(String content) {
        List<Channel> channels = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return channels;
        }

        String pendingName = null;
        String pendingGroup = null;
        String pendingLogo = null;
        boolean pending = false;

        for (String rawLine : content.split("\r?\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (EXTINF.matcher(line).find()) {
                PendingInfo info = parseExtInf(line);
                pendingName = info.name;
                pendingGroup = info.group;
                pendingLogo = info.logo;
                pending = true;
                continue;
            }

            if (line.startsWith("#")) {
                // #EXTM3U, #EXTGRP, #EXTVLCOPT ฯลฯ — ข้าม
                continue;
            }

            if (pending) {
                String name = pendingName != null ? pendingName : fallbackName(line);
                channels.add(new Channel(name, line, pendingGroup, pendingLogo));
                pending = false;
                pendingName = null;
                pendingGroup = null;
                pendingLogo = null;
            }
        }
        return channels;
    }

    private static PendingInfo parseExtInf(String line) {
        String tvgName = null;
        String group = null;
        String logo = null;

        int commaIndex = firstCommaOutsideQuotes(line);
        String attrPart = commaIndex >= 0 ? line.substring(0, commaIndex) : line;
        String displayName = commaIndex >= 0 ? line.substring(commaIndex + 1).trim() : "";

        Matcher m = ATTR.matcher(attrPart);
        while (m.find()) {
            String key = m.group(1).toLowerCase(Locale.US);
            String value = m.group(2).trim();
            if (value.isEmpty()) {
                continue;
            }
            switch (key) {
                case "tvg-name":
                    tvgName = value;
                    break;
                case "group-title":
                    group = value;
                    break;
                case "tvg-logo":
                    logo = value;
                    break;
                default:
                    break;
            }
        }

        String name = (tvgName != null && !tvgName.isEmpty()) ? tvgName : displayName;
        return new PendingInfo(name, group, logo);
    }

    /** หา comma ตัวแรกที่ไม่ได้อยู่ในเครื่องหมายคำพูด */
    private static int firstCommaOutsideQuotes(String line) {
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                return i;
            }
        }
        return -1;
    }

    /** ชื่อสำรองเมื่อ playlist ไม่ระบุชื่อเลย: ใช้ host ของ URL */
    public static String fallbackName(String url) {
        if (url == null || url.isEmpty()) {
            return "Unknown";
        }
        try {
            String host = java.net.URI.create(url).getHost();
            if (host != null && !host.isEmpty()) {
                return host;
            }
        } catch (IllegalArgumentException ignored) {
            // URL ผิดรูปแบบ — ตกไปใช้ค่าต่ำสุดด้านล่าง
        }
        return "Unknown";
    }

    private static final class PendingInfo {
        final String name;
        final String group;
        final String logo;

        PendingInfo(String name, String group, String logo) {
            this.name = (name == null || name.isEmpty()) ? null : name;
            this.group = group;
            this.logo = logo;
        }
    }
}
