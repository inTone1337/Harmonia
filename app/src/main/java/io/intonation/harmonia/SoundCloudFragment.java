package io.intonation.harmonia;

import android.app.Notification;
import android.app.PendingIntent;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
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
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.jlubecki.soundcloud.webapi.android.SoundCloudAPI;
import com.jlubecki.soundcloud.webapi.android.SoundCloudService;
import com.jlubecki.soundcloud.webapi.android.models.Track;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;

import static com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_PERIOD_TRANSITION;
import static com.google.android.exoplayer2.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL;
import static com.google.android.exoplayer2.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE;


public class SoundCloudFragment extends Fragment implements SoundCloudTrackAdapter.OnTrackClickListener {
    //SoundCloud credentials
    private static final String SOUNDCLOUD_CLIENT_ID = "v4hEbr6QReyb81OAe82kyvhbvzPOES4V";
    //SoundCloud stuff
    private static SoundCloudTrackAdapter soundCloudTrackAdapter;
    private static SoundCloudTrackAdapter playListAdapter;
    private static List<Track> soundCloudFavoriteTracks;
    private HarmoniaUserCredentials harmoniaUserCredentials;
    private List<Track> playList;
    private RecyclerView soundCloudTrackRecyclerView;
    private RecyclerView playListRecyclerView;
    private ConstraintLayout soundCloudFragment;
    private int position;
    private boolean playlistOpen = false;

    //ExoPlayer
    private SimpleExoPlayer simpleExoPlayer;
    private PlayerControlView playerControlView;
    private PlayerNotificationManager playerNotificationManager;

    // The following are used for the shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

    //Current track stuff
    private MutableLiveData<Track> currentTrack;
    private Bitmap currentTrackArtwork;
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
        soundCloudFragment = getView().findViewById(R.id.soundCloudFragment);

        currentTrackArtworkImageView = getView().findViewById(R.id.artworkImageView);
        currentTrackTitleTextView = getView().findViewById(R.id.titleTextView);
        currentTrackPlatformTextView = getView().findViewById(R.id.artistTextView);
        currentTrackCardView = getView().findViewById(R.id.currentTrackCardView);
        playerControlView = getView().findViewById(R.id.playerControlView);
        playerControlView.setShowShuffleButton(true);
        playerControlView.setRepeatToggleModes(REPEAT_TOGGLE_MODE_ONE | REPEAT_TOGGLE_MODE_ALL);
        playerControlView.hide();

        getSoundCloudFavorites(harmoniaUserCredentials.getSoundCloudUserId());

        // ShakeDetector initialization
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(count -> handleShakeEvent(count));

        currentTrackViewModel = ViewModelProviders.of(this).get(CurrentTrackViewModel.class);
        currentTrack = currentTrackViewModel.getCurrentTrack();
        currentTrackCardView.setVisibility(View.GONE);

        currentTrack.observe(this, track -> {
            Picasso.get().load(track.artwork_url.replace("large", "t300x300"))
                    .into(currentTrackArtworkImageView);
            currentTrackTitleTextView.setText(track.title);
            currentTrackPlatformTextView.setText(track.user.username);
            currentTrackCardView.getBackground().setAlpha(204);
            currentTrackCardView.setVisibility(View.VISIBLE);
            currentTrackCardView.setOnClickListener(v -> {
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(soundCloudFragment);

                AutoTransition transition = new AutoTransition();
                transition.setOrdering(TransitionSet.ORDERING_TOGETHER);
                transition.setDuration(250);
                transition.setInterpolator(new AccelerateInterpolator());
                if (!playlistOpen) {
                    constraintSet.clear(R.id.soundCloudTrackRecyclerView, ConstraintSet.TOP);
                    constraintSet.clear(R.id.currentTrackCardView, ConstraintSet.BOTTOM);

                    TransitionManager.beginDelayedTransition(soundCloudFragment, transition);
                    constraintSet.applyTo(soundCloudFragment);
                    //TODO: get this scrolltoposition to show the current track at top
                    playListRecyclerView.scrollToPosition(position);

                    playlistOpen = true;
                } else {
                    constraintSet.connect(R.id.soundCloudTrackRecyclerView, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                    constraintSet.connect(R.id.currentTrackCardView, ConstraintSet.BOTTOM, R.id.playerControlView, ConstraintSet.TOP);

                    TransitionManager.beginDelayedTransition(soundCloudFragment, transition);
                    constraintSet.applyTo(soundCloudFragment);

                    playlistOpen = false;
                }
            });
        });

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(getContext(), "playback", R.string.notification_channel_name_playback_controls, 1337, new MediaDescriptionAdapter());
        playerNotificationManager.setNotificationListener(new PlayerNotificationManager.NotificationListener() {
            @Override
            public void onNotificationStarted(int notificationId, Notification notification) {
                Intent serviceIntent = new Intent(getContext(), SoundCloudPlaybackService.class);
                serviceIntent.putExtra("notificationId", notificationId);
                serviceIntent.putExtra("notification", notification);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getActivity().startService(serviceIntent);
                }
            }

            @Override
            public void onNotificationCancelled(int notificationId) {
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
                    populateTrackList();

                } else {
                    Log.e("SC", "Error in response.");
                }
            }

            @Override
            public void onFailure(Call<List<com.jlubecki.soundcloud.webapi.android.models.Track>> call, Throwable t) {
                Log.e("SC", "Error getting track.", t);
            }
        });

        playList = new ArrayList<>();
    }

    public void setPosition(int position) {
        this.position = position;
    }

    private void playSoundCloud(int position) {
        if (simpleExoPlayer == null) {
            simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(getContext());
            playerControlView.setPlayer(simpleExoPlayer);
            playerControlView.setShowTimeoutMs(0);
            simpleExoPlayer.addListener(new Player.EventListener() {
                @Override
                public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                    updateCurrentTrack();
                }

                @Override
                public void onPositionDiscontinuity(int reason) {
                    if (reason == DISCONTINUITY_REASON_PERIOD_TRANSITION) {
                        updateCurrentTrack();
                    }
                }

                private void updateCurrentTrack() {
                    int newPosition = simpleExoPlayer.getCurrentWindowIndex();
                    playerControlView.show();
                    currentTrackViewModel.getCurrentTrack().setValue(soundCloudFavoriteTracks.get(newPosition));
                    Target target = new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            currentTrackArtwork = bitmap;
                        }

                        @Override
                        public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {

                        }
                    };
                    Picasso.get().load(currentTrack.getValue().artwork_url.replace("large", "t300x300")).into(target);
                    playerNotificationManager.setPlayer(simpleExoPlayer);
                    setPosition(newPosition);
                    playListRecyclerView.scrollToPosition(position);
                }
            });
            // Produces DataSource instances through which media data is loaded.
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getContext(), Util.getUserAgent(getContext(), "Harmonia"));
            // This is the MediaSource representing the media to be played.
            ConcatenatingMediaSource exoPlayerPlaylist = new ConcatenatingMediaSource();
            for (Track track : soundCloudFavoriteTracks) {
                exoPlayerPlaylist.addMediaSource(new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(track.stream_url + "?client_id=" + SOUNDCLOUD_CLIENT_ID)));
                playList.add(track);
            }
            // Prepare the player with the source.
            simpleExoPlayer.prepare(exoPlayerPlaylist);
            populatePlayList();
        }
        simpleExoPlayer.seekTo(position, 0);
        simpleExoPlayer.setPlayWhenReady(true);
    }

    private void populateTrackList() {
        soundCloudTrackRecyclerView = getView().findViewById(R.id.soundCloudTrackRecyclerView);
        soundCloudTrackAdapter = new SoundCloudTrackAdapter(soundCloudFavoriteTracks, this);

        soundCloudTrackRecyclerView.setAdapter(soundCloudTrackAdapter);
        soundCloudTrackRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    }

    private void populatePlayList() {
        playListRecyclerView = getView().findViewById(R.id.playListRecyclerView);
        playListAdapter = new SoundCloudTrackAdapter(playList, this);

        playListRecyclerView.setAdapter(playListAdapter);
        playListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    }

    @Override
    public void onTrackClick(int position) {
        playSoundCloud(position);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (simpleExoPlayer != null) {
            Intent serviceIntent = new Intent(getContext(), SoundCloudPlaybackService.class);
            getActivity().stopService(serviceIntent);
            playerNotificationManager.setPlayer(null);
            simpleExoPlayer.release();
            simpleExoPlayer = null;
        }
    }

    public class MediaDescriptionAdapter implements PlayerNotificationManager.MediaDescriptionAdapter {

        @Override
        public String getCurrentContentTitle(Player player) {
            return Objects.requireNonNull(currentTrack.getValue()).title;
        }

        @Nullable
        @Override
        public String getCurrentContentText(Player player) {
            return Objects.requireNonNull(currentTrack.getValue()).user.username;
        }

        @Nullable
        @Override
        public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
            return currentTrackArtwork;
        }

        @Nullable
        @Override
        public PendingIntent createCurrentContentIntent(Player player) {
            return PendingIntent.getActivity(getContext(), 1337, new Intent(getContext(), LoginActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }
}
