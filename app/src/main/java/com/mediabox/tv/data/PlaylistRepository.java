package com.mediabox.tv.data;

import android.content.Context;
import android.net.Uri;
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
 * โหลด playlist ได้ 3 แหล่ง:
 * <ul>
 *   <li>{@code null} หรือว่าง → asset ที่ bundle มากับแอป</li>
 *   <li>{@code content://…} → ไฟล์ที่ผู้ใช้เลือกผ่าน file picker (SAF)</li>
 *   <li>{@code http(s)://…} → playlist บนเครือข่าย</li>
 * </ul>
 * ทำงานบน background thread แล้ว callback กลับมาบน main thread
 */
public final class PlaylistRepository {

    public static final String ASSET_PLAYLIST = "channels.m3u";
    private static final int TIMEOUT_MS = 15000;
    private static final int MAX_BYTES = 8 * 1024 * 1024;

    public static final String SOURCE_ASSET = "asset:" + ASSET_PLAYLIST;

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

    public static boolean isRemote(String source) {
        return source != null && (source.startsWith("http://") || source.startsWith("https://"));
    }

    public static boolean isLocalFile(String source) {
        return source != null && source.startsWith("content://");
    }

    public static String describe(String source) {
        if (source == null || source.trim().isEmpty()) {
            return SOURCE_ASSET;
        }
        if (isLocalFile(source)) {
            return "ไฟล์ในเครื่อง";
        }
        return source;
    }

    /** @param source null/ว่าง = asset, content:// = ไฟล์ในเครื่อง, http(s):// = เครือข่าย */
    public void load(final String source, final Callback callback) {
        final String trimmed = (source == null) ? "" : source.trim();
        final String label = describe(trimmed);

        io.execute(() -> {
            try {
                String content;
                if (trimmed.isEmpty()) {
                    content = readAsset();
                } else if (isLocalFile(trimmed)) {
                    content = readContentUri(Uri.parse(trimmed));
                } else if (isRemote(trimmed)) {
                    content = readUrl(trimmed);
                } else {
                    throw new IOException("ไม่รู้จักแหล่ง playlist: " + trimmed);
                }

                List<Channel> channels = M3UParser.parse(content);
                if (channels.isEmpty()) {
                    throw new IOException("playlist ไม่มีช่องที่เล่นได้ (" + content.length() + " ตัวอักษร)");
                }
                main.post(() -> callback.onLoaded(channels, label));
            } catch (Exception e) {
                main.post(() -> callback.onError(label, e));
            }
        });
    }

    private String readAsset() throws IOException {
        try (InputStream in = appContext.getAssets().open(ASSET_PLAYLIST)) {
            return readAll(in);
        }
    }

    /** ไฟล์ที่ผู้ใช้เลือกผ่าน SAF — ต้องมีสิทธิ์แบบ persistable เก็บไว้แล้ว */
    private String readContentUri(Uri uri) throws IOException {
        try (InputStream in = appContext.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("เปิดไฟล์ไม่ได้ (openInputStream ได้ null): " + uri);
            }
            return readAll(in);
        }
    }

    private String readUrl(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "MediaboxTV/1.1");
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
