package com.mediabox.tv.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class M3UParserTest {

    @Test
    public void parsesChannelWithAllAttributes() {
        String playlist = "#EXTM3U\n"
                + "#EXTINF:-1 tvg-id=\"ch1\" tvg-name=\"BBC One\" tvg-logo=\"http://x/1.png\" group-title=\"News\",BBC One HD\n"
                + "http://example.com/bbc.m3u8\n";

        List<Channel> channels = M3UParser.parse(playlist);

        assertEquals(1, channels.size());
        Channel c = channels.get(0);
        assertEquals("BBC One", c.getName());
        assertEquals("News", c.getGroup());
        assertEquals("http://x/1.png", c.getLogo());
        assertEquals("http://example.com/bbc.m3u8", c.getUrl());
        assertTrue(c.isPlayable());
    }

    @Test
    public void usesDisplayNameWhenTvgNameMissing() {
        String playlist = "#EXTM3U\n#EXTINF:-1 group-title=\"Sports\",beIN Sports 1\nhttps://cdn/s1.m3u8\n";

        List<Channel> channels = M3UParser.parse(playlist);

        assertEquals(1, channels.size());
        assertEquals("beIN Sports 1", channels.get(0).getName());
    }

    @Test
    public void tvgNameWinsOverDisplayName() {
        String playlist = "#EXTM3U\n#EXTINF:-1 tvg-name=\"Channel A\",Channel A Backup\nhttps://cdn/a.m3u8\n";

        assertEquals("Channel A", M3UParser.parse(playlist).get(0).getName());
    }

    @Test
    public void fallsBackToUrlHostWhenNoNameAtAll() {
        String playlist = "#EXTM3U\n#EXTINF:-1 ,\nhttps://media.example.org/live/index.m3u8\n";

        assertEquals("media.example.org", M3UParser.parse(playlist).get(0).getName());
    }

    @Test
    public void parsesMultipleChannelsInOrder() {
        String playlist = "#EXTM3U\n"
                + "#EXTINF:-1,One\nhttps://a/1.m3u8\n"
                + "#EXTINF:-1,Two\nhttps://a/2.m3u8\n"
                + "#EXTINF:-1,Three\nhttps://a/3.m3u8\n";

        List<Channel> channels = M3UParser.parse(playlist);

        assertEquals(3, channels.size());
        assertEquals("One", channels.get(0).getName());
        assertEquals("Two", channels.get(1).getName());
        assertEquals("Three", channels.get(2).getName());
        assertEquals("https://a/3.m3u8", channels.get(2).getUrl());
    }

    @Test
    public void handlesCrlfLineEndings() {
        String playlist = "#EXTM3U\r\n#EXTINF:-1,CRLF Ch\r\nhttps://a/crlf.m3u8\r\n";

        List<Channel> channels = M3UParser.parse(playlist);

        assertEquals(1, channels.size());
        assertEquals("CRLF Ch", channels.get(0).getName());
        assertEquals("https://a/crlf.m3u8", channels.get(0).getUrl());
    }

    @Test
    public void ignoresCommentsBlankLinesAndOrphanUrls() {
        String playlist = "\n"
                + "#EXTM3U\n"
                + "\n"
                + "# some comment\n"
                + "#EXTGRP:News\n"
                + "#EXTINF:-1,Only Real\n"
                + "https://a/real.m3u8\n"
                + "\n";

        List<Channel> channels = M3UParser.parse(playlist);

        assertEquals(1, channels.size());
        assertEquals("Only Real", channels.get(0).getName());
    }

    @Test
    public void dropsExtinfWithoutFollowingUrl() {
        String playlist = "#EXTM3U\n#EXTINF:-1,Has No Url\n#EXTINF:-1,Has Url\nhttps://a/ok.m3u8\n";

        List<Channel> channels = M3UParser.parse(playlist);

        assertEquals(1, channels.size());
        assertEquals("Has Url", channels.get(0).getName());
    }

    @Test
    public void displayNameMayContainComma() {
        String playlist = "#EXTM3U\n#EXTINF:-1 tvg-id=\"x\",News, Weather 24\nhttps://a/w.m3u8\n";

        assertEquals("News, Weather 24", M3UParser.parse(playlist).get(0).getName());
    }

    @Test
    public void urlWithQueryStringIsKept() {
        String playlist = "#EXTM3U\n"
                + "#EXTINF:-1,Q\nhttps://host/live.m3u8?token=abc&exp=123\n";

        assertEquals("https://host/live.m3u8?token=abc&exp=123",
                M3UParser.parse(playlist).get(0).getUrl());
    }

    @Test
    public void emptyOrNullInputReturnsEmptyList() {
        assertTrue(M3UParser.parse(null).isEmpty());
        assertTrue(M3UParser.parse("").isEmpty());
        assertTrue(M3UParser.parse("   \n\n  ").isEmpty());
    }

    @Test
    public void extinfWithoutCommaStillYieldsChannelWithFallbackName() {
        String playlist = "#EXTM3U\n#EXTINF:-1 tvg-name=\"Attr Only\"\nhttps://a/x.m3u8\n";

        List<Channel> channels = M3UParser.parse(playlist);

        assertEquals(1, channels.size());
        assertEquals("Attr Only", channels.get(0).getName());
    }

    @Test
    public void bundledPlaylistParsesIntoFourPlayableChannels() throws Exception {
        java.io.InputStream in = getClass().getClassLoader().getResourceAsStream("channels.m3u");
        assertTrue("channels.m3u ต้องถูก copy เข้า test resources", in != null);
        String content = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        List<Channel> channels = M3UParser.parse(content);

        assertEquals(4, channels.size());
        for (Channel c : channels) {
            assertTrue("ทุกช่องต้องเล่นได้: " + c, c.isPlayable());
        }
        assertEquals("Big Buck Bunny", channels.get(0).getName());
        assertEquals("test-streams.mux.dev", java.net.URI.create(channels.get(0).getUrl()).getHost());
    }
}
