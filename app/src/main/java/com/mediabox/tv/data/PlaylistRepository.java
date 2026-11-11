package com.mediabox.tv.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * โหลด playlist จาก asset ที่ bundle มากับแอป หรือจาก URL ที่ผู้ใช้กรอก
 * ทำงานบน background thread แล้ว callback กลับมาบน main thread
 */
public final class PlaylistRepository {

    public static final String ASSET_PLAYLIST = "channels.m3u";
    private static final int TIMEOUT_MS = 15000;
    private static final int MAX_BYTES = 8 * 1024 * 1024;

    public interface Callback {
        void onLoaded(List<Channel> channels, String source);

        void onError(String source, Exception error);
    }

    private final Context appContext;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "playlist-io");
        t.setDaemon(true);
        return t;
    });
    private final Handler main = new Handler(Looper.getMainLooper());

    public PlaylistRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /** @param url null หรือว่าง = ใช้ playlist ที่ bundle มากับแอป */
    public void load(final String url, final Callback callback) {
        io.execute(() -> {
            String source = (url == null || url.trim().isEmpty())
                    ? "asset:" + ASSET_PLAYLIST
                    : url.trim();
            try {
                String content = (url == null || url.trim().isEmpty())
                        ? readAsset()
                        : readUrl(url.trim());
                List<Channel> channels = M3UParser.parse(content);
                if (channels.isEmpty()) {
                    throw new IOException("playlist ไม่มีช่องที่เล่นได้ (" + content.length() + " ตัวอักษร)");
                }
                main.post(() -> callback.onLoaded(channels, source));
            } catch (Exception e) {
                main.post(() -> callback.onError(source, e));
            }
        });
    }

    private String readAsset() throws IOException {
        try (InputStream in = appContext.getAssets().open(ASSET_PLAYLIST)) {
            return readAll(in);
        }
    }

    private String readUrl(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "MediaboxTV/1.0");
        try {
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + " จาก " + url);
            }
            try (InputStream in = conn.getInputStream()) {
                return readAll(in);
            }
        } finally {
            conn.disconnect();
        }
    }

    private String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int read;
        int total = 0;
        while ((read = in.read(buf)) != -1) {
            total += read;
            if (total > MAX_BYTES) {
                throw new IOException("playlist ใหญ่เกิน " + (MAX_BYTES / 1024 / 1024) + " MB");
            }
            out.write(buf, 0, read);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }
}
