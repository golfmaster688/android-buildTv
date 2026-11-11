package com.mediabox.tv;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.ui.PlayerView;

import com.mediabox.tv.data.StreamKind;

/**
 * เล่น stream ด้วย Media3/ExoPlayer
 *
 * <p>สองเรื่องที่ IPTV portal ต้องการจริง ๆ (ทดสอบกับ portal จริงแล้ว):
 * <ol>
 *   <li><b>User-Agent</b> — portal จำนวนมากตอบ HTTP 461 ถ้า UA ว่างหรือเป็น player
 *       แปลกหน้า จึงส่ง UA แบบ browser</li>
 *   <li><b>ชนิดสื่อ</b> — URL แบบ {@code live.php?...&extension=m3u8} ไม่ลงท้ายด้วย
 *       .m3u8 ทำให้ ExoPlayer เดาชนิดผิดเป็นไฟล์เดี่ยว ต้องบอก
 *       {@link MimeTypes#APPLICATION_M3U8} เอง</li>
 * </ol>
 * ถ้าเดาชนิดผิด จะลองอีกครั้งด้วยชนิดตรงข้ามก่อนรายงาน error
 */
public class PlayerActivity extends Activity {

    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_TITLE = "extra_title";

    /** portal หลายเจ้าเช็ค UA — ใช้ UA ที่ browser/player ทั่วไปใช้ */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Android TV) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private ExoPlayer player;
    private TextView statusView;
    private PlayerView playerView;

    private String mediaUrl;
    private String mediaTitle;
    private boolean hlsHint;
    private boolean triedFallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        statusView = findViewById(R.id.txt_status);
        playerView = findViewById(R.id.player_view);

        mediaUrl = getIntent().getStringExtra(EXTRA_URL);
        mediaTitle = getIntent().getStringExtra(EXTRA_TITLE);

        if (TextUtils.isEmpty(mediaUrl)) {
            Toast.makeText(this, R.string.empty_playlist, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        hlsHint = looksLikeHls(mediaUrl);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    showStatus(getString(R.string.player_buffering));
                } else if (state == Player.STATE_READY) {
                    hideStatus();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                handlePlaybackError(error);
            }
        });

        setTitle(mediaTitle);
        showStatus(mediaTitle == null ? mediaUrl : mediaTitle);
        startPlayback(hlsHint);
    }

    /** true ถ้า URL น่าจะเป็น HLS แม้ไม่มี .m3u8 ที่ path (เช่น ?extension=m3u8) */
    static boolean looksLikeHls(String url) {
        return StreamKind.looksLikeHls(url);
    }

    private void startPlayback(boolean asHls) {
        player.setMediaSource(buildMediaSource(mediaUrl, asHls));
        player.setPlayWhenReady(true);
        player.prepare();
    }

    private MediaSource buildMediaSource(String url, boolean asHls) {
        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000);

        MediaItem.Builder item = new MediaItem.Builder().setUri(url);
        if (asHls) {
            item.setMimeType(MimeTypes.APPLICATION_M3U8);
        }

        return new DefaultMediaSourceFactory(httpFactory).createMediaSource(item.build());
    }

    private void handlePlaybackError(PlaybackException error) {
        // ลองสลับสมมติฐานเรื่องชนิดสื่อหนึ่งครั้งก่อนยอมแพ้
        if (!triedFallback) {
            triedFallback = true;
            hlsHint = !hlsHint;
            showStatus(getString(R.string.retrying, error.getErrorCodeName()));
            startPlayback(hlsHint);
            return;
        }
        showStatus(getString(R.string.player_error, error.getErrorCodeName()));
    }

    private void showStatus(String text) {
        statusView.setText(text);
        statusView.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        statusView.setVisibility(View.GONE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
