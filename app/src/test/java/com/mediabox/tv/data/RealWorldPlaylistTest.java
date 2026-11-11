package com.mediabox.tv.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

/**
 * เคสจาก playlist จริงของผู้ใช้: attribute ไม่มีเว้นวรรคคั่น และ URL เป็น portal
 * ที่ลงท้ายด้วย query {@code extension=m3u8} แทนที่จะเป็น path .m3u8
 */
public class RealWorldPlaylistTest {

    @Test
    public void parsesExtinfWithNoSpacesBetweenAttributes() {
        String playlist = "#EXTM3U\n"
                + "#EXTINF:-1 tvg-name=\"LFC TV\"tvg-id=\"\"tvg-logo=\"https://example.com/lfc.png\" group-title=\"PREMIER LEAGUE\",LFC TV\n"
                + "http://main.example.net:80/play/live.php?mac=00:00:00:00:00:00&stream=1312823&extension=m3u8\n";

        List<Channel> channels = M3UParser.parse(playlist);

        assertEquals(1, channels.size());
        Channel c = channels.get(0);
        assertEquals("LFC TV", c.getName());
        assertEquals("PREMIER LEAGUE", c.getGroup());
        assertEquals("https://example.com/lfc.png", c.getLogo());
    }

    @Test
    public void emptyTvgIdDoesNotBreakFollowingAttributes() {
        String playlist = "#EXTM3U\n"
                + "#EXTINF:-1 tvg-id=\"\" tvg-name=\"After Empty Id\" group-title=\"G\",After Empty Id\n"
                + "http://h:80/play/live.php?stream=1&extension=m3u8\n";

        Channel c = M3UParser.parse(playlist).get(0);

        assertEquals("After Empty Id", c.getName());
        assertEquals("G", c.getGroup());
    }

    @Test
    public void portalUrlWithMacAndExtensionIsPlayable() {
        String url = "http://main.example.net:80/play/live.php?mac=00:11:22:33:44:55&stream=1312823&extension=m3u8";
        String playlist = "#EXTM3U\n#EXTINF:-1,Portal Ch\n" + url + "\n";

        Channel c = M3UParser.parse(playlist).get(0);

        assertEquals(url, c.getUrl());
        assertEquals(true, c.isPlayable());
    }
}
