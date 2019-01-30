package com.example.harmonia;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
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


public class SoundCloudFragment extends Fragment implements SoundCloudTrackAdapter.OnTrackClickListener {
    private static final String SOUNDCLOUD_CLIENT_ID = "v4hEbr6QReyb81OAe82kyvhbvzPOES4V";
    private static SoundCloudTrackAdapter soundCloudTrackAdapter;
    private static List<Track> soundCloudFavoriteTracks;
    //SoundCloud credentials
    private HarmoniaUserCredentials harmoniaUserCredentials;
    private RecyclerView soundCloudTrackRecyclerView;
    private SimpleExoPlayer simpleExoPlayer;
    private PlayerView playerView;
    // The following are used for the shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private Target artworkPlaceholderTarget;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        harmoniaUserCredentials = getArguments().getParcelable("harmoniaUserCredentials");
        return inflater.inflate(R.layout.fragment_soundcloud, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getSoundCloudFavorites(harmoniaUserCredentials.getSoundCloudUserId());

        // ShakeDetector initialization
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

            @Override
            public void onShake(int count) {
                /*
                 * The following method, "handleShakeEvent(count):" is a stub //
                 * method you would use to setup whatever you want done once the
                 * device has been shook.
                 */
                handleShakeEvent(count);
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
        Log.d("randomId", String.valueOf(randomTrackId));
        playSoundCloud(randomTrackId);
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
        if (simpleExoPlayer != null && playerView != null) {
            simpleExoPlayer.stop();
        } else {
            simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(getContext());
            playerView = getView().findViewById(R.id.playerView);
            playerView.setPlayer(simpleExoPlayer);
            playerView.setControllerShowTimeoutMs(0);
            playerView.setUseArtwork(true);
        }

        if (soundCloudTrackRecyclerView.getLayoutManager().findViewByPosition(position) != null) {
            ImageView view = Objects.requireNonNull(Objects.requireNonNull(soundCloudTrackRecyclerView.getLayoutManager()).findViewByPosition(position)).findViewById(R.id.artworkImageView);
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
            Picasso.get().load(soundCloudFavoriteTracks.get(position).artwork_url.replace("large", "t300x300")).into(artworkPlaceholderTarget);
        }

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getContext(), Util.getUserAgent(getContext(), "Harmonia"));
        // This is the MediaSource representing the media to be played.
        MediaSource audioSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(soundCloudFavoriteTracks.get(position).stream_url + "?client_id=" + SOUNDCLOUD_CLIENT_ID));
        // Prepare the player with the source.
        simpleExoPlayer.prepare(audioSource);
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
        Picasso.get().cancelRequest(artworkPlaceholderTarget);
    }
}
