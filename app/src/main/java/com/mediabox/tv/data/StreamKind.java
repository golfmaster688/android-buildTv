package com.mediabox.tv.data;

import java.util.Locale;

/** เดาชนิดสตรีมจาก URL — pure Java จึงเขียน unit test ได้ */
public final class StreamKind {

    private StreamKind() {
    }

    /**
     * true ถ้า URL น่าจะเป็น HLS (M3U8) แม้ path จะไม่มีนามสกุล
     * เช่น {@code http://host/play/live.php?mac=…&stream=1&extension=m3u8}
     */
    public static boolean looksLikeHls(String url) {
        if (url == null) {
            return false;
        }
        String lower = url.toLowerCase(Locale.US);
        return lower.contains(".m3u8")
                || lower.contains("extension=m3u8")
                || lower.contains("type=m3u8")
                || lower.contains("format=m3u8");
    }
}
