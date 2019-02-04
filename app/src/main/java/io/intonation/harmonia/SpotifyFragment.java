package io.intonation.harmonia;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.ErrorCallback;
import com.spotify.protocol.types.Track;

import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.SavedTrack;
import retrofit.client.Response;


public class SpotifyFragment extends Fragment implements SpotifyTrackAdapter.OnTrackClickListener {
    private static final String SPOTIFY_CLIENT_ID = "825a6e2138bf4cb6934b6d859b3def5c";
    private static final String SPOTIFY_REDIRECT_URI = "https://intonation.io";
    private static SpotifyTrackAdapter spotifyTrackAdapter;
    private static List<SavedTrack> spotifyFavoriteTracks;
    //Spotify credentials
    private HarmoniaUserCredentials harmoniaUserCredentials;
    private SpotifyApi spotifyApi;
    private SpotifyService spotifyService;
    private SpotifyAppRemote mSpotifyAppRemote;
    private RecyclerView spotifyTracksRecyclerView;
    private ErrorCallback mErrorCallback = throwable -> Log.d("Spotify Error", "");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        harmoniaUserCredentials = getArguments().getParcelable("harmoniaUserCredentials");
        return inflater.inflate(R.layout.fragment_spotify, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
        spotifyApi = new SpotifyApi();
        spotifyApi.setAccessToken(harmoniaUserCredentials.getSpotifyAccessToken());
        spotifyService = spotifyApi.getService();
        initSpotifyAppRemote();
    }

    private void getSpotifyUserSavedTracks() {
        spotifyService.getMySavedTracks(new SpotifyCallback<Pager<SavedTrack>>() {
            @Override
            public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                // handle successful response
                spotifyFavoriteTracks = savedTrackPager.items;
                populateTracklist();

                // Subscribe to PlayerState
                mSpotifyAppRemote.getPlayerApi()
                        .subscribeToPlayerState()
                        .setEventCallback(playerState -> {
                            final Track track = playerState.track;
                            if (track != null) {
                                Log.d("MainActivity", track.name + " by " + track.artist.name);
                            }
                        });
            }

            @Override
            public void failure(SpotifyError error) {
                //handle error
                Log.e("MainActivity", error.toString());
            }
        });
    }

    private void initSpotifyAppRemote() {
        // Set the connection parameters
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(SPOTIFY_CLIENT_ID)
                        .setRedirectUri(SPOTIFY_REDIRECT_URI)
                        .build();

        SpotifyAppRemote.connect(getContext(), connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("MainActivity", "Connected! Yay!");

                        spotifyAppRemote.getUserApi()
                                .getCapabilities()
                                .setResultCallback(capabilities -> Log.d("MainActivity", String.format("Can play on demand: %s", capabilities.canPlayOnDemand)))
                                .setErrorCallback(mErrorCallback);

                        getSpotifyUserSavedTracks();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("MainActivity", throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });

    }

    private void populateTracklist() {
        spotifyTracksRecyclerView = getView().findViewById(R.id.spotifyTrackRecyclerView);
        spotifyTrackAdapter = new SpotifyTrackAdapter(spotifyFavoriteTracks, this);

        spotifyTracksRecyclerView.setAdapter(spotifyTrackAdapter);
        spotifyTracksRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    }

    @Override
    public void onTrackClick(int position) {
        mSpotifyAppRemote.getPlayerApi().play(spotifyFavoriteTracks.get(0).track.uri);
    }

    @Override
    public void onStop() {
        super.onStop();
        mSpotifyAppRemote.getPlayerApi().pause();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }
}
