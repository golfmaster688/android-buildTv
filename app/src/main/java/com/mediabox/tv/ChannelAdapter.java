package com.mediabox.tv;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mediabox.tv.data.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** รายการช่อง — แต่ละแถว focusable เพื่อใช้ D-pad บน Android TV */
public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {

    public interface OnChannelClickListener {
        void onChannelClick(Channel channel);
    }

    private final List<Channel> channels = new ArrayList<>();
    private final OnChannelClickListener listener;

    public ChannelAdapter(OnChannelClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<Channel> newChannels) {
        channels.clear();
        if (newChannels != null) {
            channels.addAll(newChannels);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        Channel channel = channels.get(position);
        holder.index.setText(String.format(Locale.US, "%02d", position + 1));
        holder.name.setText(channel.getName());
        String group = channel.getGroup();
        holder.group.setText(group.isEmpty() ? channel.getUrl() : group);
        holder.itemView.setOnClickListener(v -> listener.onChannelClick(channel));
    }

    @Override
    public int getItemCount() {
        return channels.size();
    }

    static class ChannelViewHolder extends RecyclerView.ViewHolder {
        final TextView index;
        final TextView name;
        final TextView group;

        ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            index = itemView.findViewById(R.id.txt_index);
            name = itemView.findViewById(R.id.txt_name);
            group = itemView.findViewById(R.id.txt_group);
        }
    }
}
