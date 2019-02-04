package io.intonation.harmonia;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jlubecki.soundcloud.webapi.android.models.Track;
import com.squareup.picasso.Picasso;

import java.util.List;

import jp.wasabeef.picasso.transformations.BlurTransformation;

public class SoundCloudTrackAdapter extends RecyclerView.Adapter<SoundCloudTrackAdapter.TrackViewHolder> {

    private List<Track> mTrackList;
    private OnTrackClickListener onTrackClickListener;

    SoundCloudTrackAdapter(List<Track> trackList, OnTrackClickListener onTrackClickListener) {
        mTrackList = trackList;
        this.onTrackClickListener = onTrackClickListener;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, final int position) {
        final Track track = mTrackList.get(position);
        Picasso.get().load(track.artwork_url.replace("large", "t300x300")).transform(new BlurTransformation(holder.view.getContext(), 5)).into(holder.artworkBackground);
        Picasso.get().load(track.artwork_url.replace("large", "t300x300")).into(holder.artwork);
        holder.title.setText(track.title);
        holder.dateAdded.setText(track.favoritings_count);
        holder.status.setText(secondsToMMSS(track.duration));
        holder.platform.setText(track.user.username);
        holder.view.setOnClickListener(v -> onTrackClickListener.onTrackClick(position));
    }

    //TODO: fix crappy timestamp code
    public String secondsToMMSS(String duration) {
        int durationAsInt = Integer.parseInt(duration);
        int minutes = durationAsInt / 60000;
        int seconds = (durationAsInt - minutes * 60000) / 1000;
        return String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
    }

    @Override
    public int getItemCount() {
        return mTrackList.size();
    }

    void swapList(List<Track> newList) {
        mTrackList = newList;
        if (newList != null) {
            this.notifyDataSetChanged();
        }
    }

    public interface OnTrackClickListener {
        void onTrackClick(int position);
    }

    class TrackViewHolder extends RecyclerView.ViewHolder {

        private View view;
        private ImageView artworkBackground;
        private ImageView artwork;
        private TextView title;
        private TextView platform;
        private TextView status;
        private TextView dateAdded;

        private TrackViewHolder(View trackView) {
            super(trackView);
            view = trackView;
            artworkBackground = trackView.findViewById(R.id.artworkBackgroundImageView);
            artwork = trackView.findViewById(R.id.artworkImageView);
            title = trackView.findViewById(R.id.titleTextView);
            platform = trackView.findViewById(R.id.artistTextView);
            status = trackView.findViewById(R.id.durationTextView);
            dateAdded = trackView.findViewById(R.id.popularityTextView);
        }
    }
}
