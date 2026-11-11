package com.mediabox.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mediabox.tv.data.Channel;
import com.mediabox.tv.data.PlaylistRepository;

import java.util.Collections;
import java.util.List;

/**
 * หน้าแรก: แสดงรายการช่อง และให้ผู้ใช้เลือกแหล่ง playlist ได้ 2 ทาง
 * <ul>
 *   <li>เพิ่มจาก URL (กรอกเองใน dialog)</li>
 *   <li>เปิดไฟล์ .m3u/.m3u8 ในเครื่องผ่าน file picker (SAF)</li>
 * </ul>
 */
public class MainActivity extends Activity {

    private static final String PREFS = "mediabox";
    private static final String KEY_SOURCE = "playlist_source";
    private static final String KEY_LEGACY_URL = "playlist_url";
    private static final int REQ_PICK_PLAYLIST = 1001;

    private RecyclerView listView;
    private ProgressBar progress;
    private TextView sourceView;
    private TextView emptyView;

    private ChannelAdapter adapter;
    private PlaylistRepository repository;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        repository = new PlaylistRepository(this);

        listView = findViewById(R.id.list_channels);
        progress = findViewById(R.id.progress);
        sourceView = findViewById(R.id.txt_source);
        emptyView = findViewById(R.id.txt_empty);
        Button urlButton = findViewById(R.id.btn_add_url);
        Button fileButton = findViewById(R.id.btn_open_file);

        adapter = new ChannelAdapter(this::openChannel);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(adapter);

        urlButton.setOnClickListener(v -> showUrlDialog());
        fileButton.setOnClickListener(v -> pickPlaylistFile());

        loadPlaylist(currentSource());
    }

    /** แหล่ง playlist ที่บันทึกไว้ (รองรับ key เก่าจากเวอร์ชันก่อน) */
    private String currentSource() {
        String source = prefs.getString(KEY_SOURCE, null);
        if (source == null) {
            source = prefs.getString(KEY_LEGACY_URL, null);
        }
        return source;
    }

    private void saveSource(String source) {
        prefs.edit().putString(KEY_SOURCE, source == null ? "" : source).apply();
    }

    private void openChannel(Channel channel) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_URL, channel.getUrl());
        intent.putExtra(PlayerActivity.EXTRA_TITLE, channel.getName());
        startActivity(intent);
    }

    private void loadPlaylist(String source) {
        progress.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        sourceView.setText(R.string.loading_playlist);

        repository.load(source, new PlaylistRepository.Callback() {
            @Override
            public void onLoaded(List<Channel> channels, String label) {
                progress.setVisibility(View.GONE);
                adapter.submit(channels);
                sourceView.setText(getString(R.string.playlist_source, label)
                        + " · " + getString(R.string.channel_count, channels.size()));
                emptyView.setVisibility(channels.isEmpty() ? View.VISIBLE : View.GONE);

                // ให้ D-pad โฟกัสแถวแรกทันที (ไม่ใช่ปุ่มด้านบน)
                listView.post(() -> {
                    listView.requestFocus();
                    RecyclerView.ViewHolder first = listView.findViewHolderForAdapterPosition(0);
                    if (first != null) {
                        first.itemView.requestFocus();
                    }
                });
            }

            @Override
            public void onError(String label, Exception error) {
                progress.setVisibility(View.GONE);
                adapter.submit(Collections.emptyList());
                emptyView.setVisibility(View.VISIBLE);
                sourceView.setText(getString(R.string.playlist_source, label));
                String detail = error.getMessage() == null ? error.toString() : error.getMessage();
                Toast.makeText(MainActivity.this,
                        getString(R.string.error_loading, detail), Toast.LENGTH_LONG).show();
            }
        });
    }

    /** เปิด file picker ให้เลือกไฟล์ playlist ในเครื่อง */
    private void pickPlaylistFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "audio/x-mpegurl",
                "application/x-mpegurl",
                "application/vnd.apple.mpegurl",
                "audio/mpegurl",
                "text/plain",
                "*/*"
        });
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        try {
            startActivityForResult(intent, REQ_PICK_PLAYLIST);
        } catch (Exception e) {
            Toast.makeText(this, R.string.no_file_picker, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_PLAYLIST || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        try {
            // เก็บสิทธิ์ถาวร เพื่อให้เปิดไฟล์เดิมได้หลังรีบูต
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            // provider บางตัวไม่รองรับ persistable — ใช้ได้เฉพาะรอบนี้
        }
        saveSource(uri.toString());
        loadPlaylist(uri.toString());
    }

    private void showUrlDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.playlist_dialog_hint);
        input.setSingleLine(true);
        String current = currentSource();
        if (PlaylistRepository.isRemote(current)) {
            input.setText(current);
            input.setSelection(input.getText().length());
        }

        FrameLayout container = new FrameLayout(this);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.playlist_dialog_title)
                .setMessage(R.string.playlist_dialog_help)
                .setView(container)
                .setPositiveButton(R.string.action_apply, (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    saveSource(url);
                    loadPlaylist(TextUtils.isEmpty(url) ? null : url);
                })
                .setNeutralButton(R.string.action_open_file, (dialog, which) -> pickPlaylistFile())
                .setNegativeButton(R.string.action_use_bundled, (dialog, which) -> {
                    saveSource("");
                    loadPlaylist(null);
                })
                .show();
    }
}
