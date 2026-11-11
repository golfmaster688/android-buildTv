package com.mediabox.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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

/** หน้าแรก: แสดงรายการช่องจาก playlist ที่เลือกไว้ */
public class MainActivity extends Activity {

    private static final String PREFS = "mediabox";
    private static final String KEY_PLAYLIST_URL = "playlist_url";

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
        Button playlistButton = findViewById(R.id.btn_playlist);

        adapter = new ChannelAdapter(this::openChannel);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(adapter);

        playlistButton.setOnClickListener(v -> showPlaylistDialog());

        loadPlaylist(prefs.getString(KEY_PLAYLIST_URL, null));
    }

    private void openChannel(Channel channel) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_URL, channel.getUrl());
        intent.putExtra(PlayerActivity.EXTRA_TITLE, channel.getName());
        startActivity(intent);
    }

    private void loadPlaylist(String url) {
        progress.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        sourceView.setText(R.string.loading_playlist);

        repository.load(url, new PlaylistRepository.Callback() {
            @Override
            public void onLoaded(List<Channel> channels, String source) {
                progress.setVisibility(View.GONE);
                adapter.submit(channels);
                sourceView.setText(getString(R.string.playlist_source, source)
                        + " · " + getString(R.string.channel_count, channels.size()));
                emptyView.setVisibility(channels.isEmpty() ? View.VISIBLE : View.GONE);

                // ให้ D-pad โฟกัสแถวแรกทันที (ไม่ใช่ปุ่มตั้งค่า playlist)
                listView.post(() -> {
                    listView.requestFocus();
                    RecyclerView.ViewHolder first = listView.findViewHolderForAdapterPosition(0);
                    if (first != null) {
                        first.itemView.requestFocus();
                    }
                });
            }

            @Override
            public void onError(String source, Exception error) {
                progress.setVisibility(View.GONE);
                adapter.submit(Collections.emptyList());
                emptyView.setVisibility(View.VISIBLE);
                sourceView.setText(getString(R.string.playlist_source, source));
                String detail = error.getMessage() == null ? error.toString() : error.getMessage();
                Toast.makeText(MainActivity.this,
                        getString(R.string.error_loading, detail), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showPlaylistDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.playlist_dialog_hint);
        input.setSingleLine(true);
        input.setText(prefs.getString(KEY_PLAYLIST_URL, ""));
        input.setSelection(input.getText().length());

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
                    prefs.edit().putString(KEY_PLAYLIST_URL, url).apply();
                    loadPlaylist(TextUtils.isEmpty(url) ? null : url);
                })
                .setNeutralButton(R.string.action_use_bundled, (dialog, which) -> {
                    prefs.edit().putString(KEY_PLAYLIST_URL, "").apply();
                    loadPlaylist(null);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
