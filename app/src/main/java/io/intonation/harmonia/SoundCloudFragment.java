package io.intonation.harmonia;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.jlubecki.soundcloud.webapi.android.SoundCloudAPI;
import com.jlubecki.soundcloud.webapi.android.SoundCloudService;
import com.jlubecki.soundcloud.webapi.android.models.Track;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;


public class SoundCloudFragment extends Fragment implements SoundCloudTrackAdapter.OnTrackClickListener, SoundCloudTrackAdapter.OnTrackCheckListener {
    //SoundCloud credentials
    private static final String SOUNDCLOUD_CLIENT_ID = "v4hEbr6QReyb81OAe82kyvhbvzPOES4V";
    private HarmoniaUserCredentials harmoniaUserCredentials;

    //SoundCloud stuff
    private static SoundCloudTrackAdapter soundCloudTrackAdapter;
    private static List<Track> soundCloudFavoriteTracks;
    private List<Track> playlist;
    private RecyclerView soundCloudTrackRecyclerView;

    //ExoPlayer
    private SimpleExoPlayer simpleExoPlayer;
    private PlayerView playerView;

    // The following are used for the shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private Target artworkPlaceholderTarget;

    //Current track stuff
    private MutableLiveData<Track> currentTrack;
    private CurrentTrackViewModel currentTrackViewModel;
    private ImageView currentTrackArtworkImageView;
    private TextView currentTrackTitleTextView;
    private TextView currentTrackPlatformTextView;
    private TextView currentTrackStatusTextView;
    private TextView currentTrackDateAddedTextView;
    private CardView currentTrackCardView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        harmoniaUserCredentials = getArguments().getParcelable("harmoniaUserCredentials");
        return inflater.inflate(R.layout.fragment_soundcloud, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentTrackArtworkImageView = getView().findViewById(R.id.artworkImageView);
        currentTrackTitleTextView = getView().findViewById(R.id.titleTextView);
        currentTrackPlatformTextView = getView().findViewById(R.id.artistTextView);
        currentTrackStatusTextView = getView().findViewById(R.id.durationTextView);
        currentTrackDateAddedTextView = getView().findViewById(R.id.popularityTextView);
        currentTrackCardView = getView().findViewById(R.id.currentTrackCardView);
        currentTrackCardView.getBackground().setAlpha(128);

        getSoundCloudFavorites(harmoniaUserCredentials.getSoundCloudUserId());

        // ShakeDetector initialization
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {
            @Override
            public void onShake(int count) {
                handleShakeEvent(count);
            }
        });

        currentTrackViewModel = ViewModelProviders.of(this).get(CurrentTrackViewModel.class);
        currentTrack = currentTrackViewModel.getCurrentTrack();

        currentTrack.observe(this, new Observer<Track>() {
            @Override
            public void onChanged(@Nullable Track track) {
                Picasso.get().load(track.artwork_url.replace("large", "t300x300"))
                        .into(currentTrackArtworkImageView);
                currentTrackTitleTextView.setText(track.title);
                currentTrackDateAddedTextView.setText(track.favoritings_count);
                currentTrackStatusTextView.setText(soundCloudTrackAdapter.secondsToMMSS(track.duration));
                currentTrackPlatformTextView.setText(track.user.username);
                currentTrackCardView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Add the following line to register the Session Manager Listener onResume
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        mSensorManager.unregisterListener(mShakeDetector);
        super.onPause();
    }

    private void handleShakeEvent(int count) {
        int randomTrackId = new Random().nextInt(soundCloudFavoriteTracks.size());
        playSoundCloud(randomTrackId);
        if (!simpleExoPlayer.getShuffleModeEnabled()) {
            simpleExoPlayer.setShuffleModeEnabled(true);
        } else {
            simpleExoPlayer.setShuffleModeEnabled(false);
        }
    }

    private void getSoundCloudFavorites(String soundCloudUserId) {
        //init soundcloud-api
        SoundCloudAPI soundCloudAPI = new SoundCloudAPI(SOUNDCLOUD_CLIENT_ID);
        SoundCloudService soundCloudService = soundCloudAPI.getService();

        soundCloudService.getUserFavorites(soundCloudUserId).enqueue(new Callback<List<com.jlubecki.soundcloud.webapi.android.models.Track>>() {
            @Override
            public void onResponse(Call<List<com.jlubecki.soundcloud.webapi.android.models.Track>> call, retrofit2.Response<List<com.jlubecki.soundcloud.webapi.android.models.Track>> response) {
                List<com.jlubecki.soundcloud.webapi.android.models.Track> userFavorites = response.body();
                if (userFavorites != null) {
                    Log.d("SC", "Track success: " + userFavorites.get(0).stream_url);
                    soundCloudFavoriteTracks = userFavorites;
                    populateTracklist();

                } else {
                    Log.e("SC", "Error in response.");
                }
            }

            @Override
            public void onFailure(Call<List<com.jlubecki.soundcloud.webapi.android.models.Track>> call, Throwable t) {
                Log.e("SC", "Error getting track.", t);
            }
        });
    }

    private void playSoundCloud(int position) {
        if (simpleExoPlayer == null && playerView == null) {
            simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(getContext());
            playerView = getView().findViewById(R.id.playerView);
            playerView.setPlayer(simpleExoPlayer);
            playerView.setControllerShowTimeoutMs(0);
            playerView.setUseArtwork(true);
            simpleExoPlayer.addListener(new ExoPlayer.EventListener() {
                @Override
                public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                    int newPosition = simpleExoPlayer.getCurrentWindowIndex();
                    if (soundCloudTrackRecyclerView.getLayoutManager().findViewByPosition(newPosition) != null) {
                        ImageView view = Objects.requireNonNull(Objects.requireNonNull(soundCloudTrackRecyclerView.getLayoutManager()).findViewByPosition(newPosition)).findViewById(R.id.artworkImageView);
                        playerView.setDefaultArtwork(((BitmapDrawable) view.getDrawable()).getBitmap());
                    } else {
                        artworkPlaceholderTarget = new Target() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                playerView.setDefaultArtwork(bitmap);
                            }

                            @Override
                            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                            }

                            @Override
                            public void onPrepareLoad(Drawable placeHolderDrawable) {
                            }
                        };
                        Picasso.get().load(soundCloudFavoriteTracks.get(newPosition).artwork_url.replace("large", "t300x300")).into(artworkPlaceholderTarget);
                    }
                    currentTrackViewModel.getCurrentTrack().setValue(soundCloudFavoriteTracks.get(newPosition));
                }
            });
            // Produces DataSource instances through which media data is loaded.
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getContext(), Util.getUserAgent(getContext(), "Harmonia"));
            // This is the MediaSource representing the media to be played.
            ConcatenatingMediaSource exoPlayerPlaylist = new ConcatenatingMediaSource();
            for (Track track : soundCloudFavoriteTracks) {
                exoPlayerPlaylist.addMediaSource(new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(track.stream_url + "?client_id=" + SOUNDCLOUD_CLIENT_ID)));
            }
            // Prepare the player with the source.
            simpleExoPlayer.prepare(exoPlayerPlaylist);
        }
        simpleExoPlayer.seekTo(position, 0);
        simpleExoPlayer.setPlayWhenReady(true);
    }

    private void populateTracklist() {
        soundCloudTrackRecyclerView = getView().findViewById(R.id.soundCloudTrackRecyclerView);
        soundCloudTrackAdapter = new SoundCloudTrackAdapter(soundCloudFavoriteTracks, this);

        soundCloudTrackRecyclerView.setAdapter(soundCloudTrackAdapter);
        soundCloudTrackRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    }

    @Override
    public void onTrackClick(int position) {
        playSoundCloud(position);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (simpleExoPlayer != null) {
            simpleExoPlayer.release();
        }
        if (artworkPlaceholderTarget != null) {
            Picasso.get().cancelRequest(artworkPlaceholderTarget);
        }
    }

    @Override
    public void onTrackCheck(int position) {
        playlist.add(soundCloudFavoriteTracks.get(position));
    }
}
