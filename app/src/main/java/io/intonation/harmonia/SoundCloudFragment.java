package io.intonation.harmonia;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
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

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.jlubecki.soundcloud.webapi.android.SoundCloudAPI;
import com.jlubecki.soundcloud.webapi.android.SoundCloudService;
import com.jlubecki.soundcloud.webapi.android.models.Track;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;


public class SoundCloudFragment extends Fragment implements SoundCloudTrackAdapter.OnTrackClickListener, SoundCloudTrackAdapter.OnTrackCheckListener {
    //SoundCloud credentials
    private static final String SOUNDCLOUD_CLIENT_ID = "v4hEbr6QReyb81OAe82kyvhbvzPOES4V";
    //SoundCloud stuff
    private static SoundCloudTrackAdapter soundCloudTrackAdapter;
    private static List<Track> soundCloudFavoriteTracks;
    private HarmoniaUserCredentials harmoniaUserCredentials;
    private List<Track> playlist;
    private RecyclerView soundCloudTrackRecyclerView;

    //ExoPlayer
    private SimpleExoPlayer simpleExoPlayer;
    private PlayerControlView playerControlView;

    // The following are used for the shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

    //Current track stuff
    private MutableLiveData<Track> currentTrack;
    private CurrentTrackViewModel currentTrackViewModel;
    private ImageView currentTrackArtworkImageView;
    private TextView currentTrackTitleTextView;
    private TextView currentTrackPlatformTextView;
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
        currentTrackCardView = getView().findViewById(R.id.currentTrackCardView);
        playerControlView = getView().findViewById(R.id.playerControlView);
        playerControlView.hide();

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
        currentTrackCardView.setVisibility(View.GONE);

        currentTrack.observe(this, track -> {
            Picasso.get().load(track.artwork_url.replace("large", "t300x300"))
                    .into(currentTrackArtworkImageView);
            currentTrackTitleTextView.setText(track.title);
            currentTrackPlatformTextView.setText(track.user.username);
            currentTrackCardView.getBackground().setAlpha(200);
            currentTrackCardView.setVisibility(View.VISIBLE);
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

        playlist = new ArrayList<>();
    }

    private void playSoundCloud(int position) {
        if (simpleExoPlayer == null) {
            simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(getContext());
            playerControlView.setPlayer(simpleExoPlayer);
            playerControlView.setShowTimeoutMs(0);
            simpleExoPlayer.addListener(new Player.EventListener() {
                @Override
                public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                    int newPosition = simpleExoPlayer.getCurrentWindowIndex();
                    playerControlView.show();
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
        soundCloudTrackAdapter = new SoundCloudTrackAdapter(soundCloudFavoriteTracks, this, this);

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
    }

    @Override
    public void onTrackCheck(int position) {
        playlist.add(soundCloudFavoriteTracks.get(position));
    }
}
