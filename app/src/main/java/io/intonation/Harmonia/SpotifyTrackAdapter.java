package io.intonation.harmonia;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import jp.wasabeef.picasso.transformations.BlurTransformation;
import kaaes.spotify.webapi.android.models.SavedTrack;
import kaaes.spotify.webapi.android.models.Track;

public class SpotifyTrackAdapter extends RecyclerView.Adapter<SpotifyTrackAdapter.TrackViewHolder> {

    private List<SavedTrack> trackList;
    private OnTrackClickListener onTrackClickListener;

    SpotifyTrackAdapter(List<SavedTrack> trackList, OnTrackClickListener onTrackClickListener) {
        this.trackList = trackList;
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
        final Track track = trackList.get(position).track;
        Picasso.get().load(track.album.images.get(0).url).transform(new BlurTransformation(holder.view.getContext(), 5)).into(holder.artworkBackground);
        Picasso.get().load(track.album.images.get(0).url).into(holder.artwork);
        holder.title.setText(track.name);
        holder.dateAdded.setText(track.popularity.toString());
        holder.status.setText(secondsToMMSS(track.duration_ms));
        holder.platform.setText(track.artists.get(0).name);
        holder.view.setOnClickListener(v -> onTrackClickListener.onTrackClick(position));
    }

    //TODO: fix crappy timestamp code
    private String secondsToMMSS(Long duration) {
        Long minutes = duration / 60000;
        Long seconds = (duration - minutes * 60000) / 1000;
        return String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
    }

    @Override
    public int getItemCount() {
        return trackList.size();
    }

    void swapList(List<SavedTrack> newList) {
        trackList = newList;
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
