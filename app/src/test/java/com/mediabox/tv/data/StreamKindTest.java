package com.mediabox.tv.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StreamKindTest {

    @Test
    public void detectsPlainM3u8Path() {
        assertTrue(StreamKind.looksLikeHls("https://cdn.example.com/live/index.m3u8"));
    }

    @Test
    public void detectsPortalUrlWithExtensionParam() {
        assertTrue(StreamKind.looksLikeHls(
                "http://main.example.net:80/play/live.php?mac=00:11:22:33:44:55&stream=1312823&extension=m3u8"));
    }

    @Test
    public void detectsTypeAndFormatParams() {
        assertTrue(StreamKind.looksLikeHls("http://h/play?type=m3u8"));
        assertTrue(StreamKind.looksLikeHls("http://h/play?format=m3u8"));
    }

    @Test
    public void caseInsensitive() {
        assertTrue(StreamKind.looksLikeHls("HTTP://H/LIVE/index.M3U8"));
        assertTrue(StreamKind.looksLikeHls("http://h/play?Extension=M3U8"));
    }

    @Test
    public void rejectsNonHlsUrls() {
        assertFalse(StreamKind.looksLikeHls("http://h/video.mp4"));
        assertFalse(StreamKind.looksLikeHls("http://h/play/live.php?stream=1"));
        assertFalse(StreamKind.looksLikeHls("https://h/manifest.mpd"));
    }

    @Test
    public void handlesNullAndEmpty() {
        assertFalse(StreamKind.looksLikeHls(null));
        assertFalse(StreamKind.looksLikeHls(""));
    }
}
