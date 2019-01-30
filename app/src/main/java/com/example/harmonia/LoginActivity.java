package com.example.harmonia;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class LoginActivity extends AppCompatActivity {

    public final static int TASK_SELECT_ALL = 0;
    public final static int TASK_INSERT = 1;
    public final static int TASK_DELETE = 2;
    public final static int TASK_WIPE = 3;
    //Spotify credentials
    private static final String SPOTIFY_CLIENT_ID = "825a6e2138bf4cb6934b6d859b3def5c";
    private static final String SPOTIFY_REDIRECT_URI = "https://intonation.io";
    // Request code will be used to verify if result comes from the login activity. Can be set to any integer.
    private static final int REQUEST_CODE = 1337;
    //SoundCloud credentials
    private static final String SOUNDCLOUD_CLIENT_ID = "v4hEbr6QReyb81OAe82kyvhbvzPOES4V";
    private static AppDatabase appDatabase;
    private String soundCloudUserId;
    private String spotifyAccessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appDatabase = AppDatabase.getInstance(this);

        if (getIntent().getAction() == Intent.ACTION_DELETE) {
            new HarmoniaUserCredentialsAsyncTask(TASK_WIPE).execute();
        } else {
            new HarmoniaUserCredentialsAsyncTask(TASK_SELECT_ALL).execute();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button soundCloudLoginButton = findViewById(R.id.soundCloudLoginButton);
        soundCloudLoginButton.setOnClickListener(v -> authenticateSoundcloud());
    }

    private void authenticateSoundcloud() {
        EditText soundCloudLoginEditText = (EditText) findViewById(R.id.soundCloudLoginEditText);
        String soundCloudUsername = soundCloudLoginEditText.getText().toString();

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.soundcloud.com/resolve?url=https://soundcloud.com/" + soundCloudUsername + "&client_id=" + SOUNDCLOUD_CLIENT_ID;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        soundCloudUserId = jsonObject.getString("id");
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                }, error -> {
        });

        queue.add(stringRequest);
        authenticateSpotify();
    }

    private void authenticateSpotify() {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, SPOTIFY_REDIRECT_URI);

        builder.setScopes(new String[]{"streaming", "app-remote-control", "user-library-read"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                case TOKEN:
                    spotifyAccessToken = response.getAccessToken();
                    saveHarmoniaUserCredentials();
                    break;
                case ERROR:
                    Log.e("Spotify Authentication", response.getError());
                    break;
                default:
            }
        }
    }

    private void saveHarmoniaUserCredentials() {
        HarmoniaUserCredentials harmoniaUserCredentials = new HarmoniaUserCredentials(soundCloudUserId, spotifyAccessToken);
        new HarmoniaUserCredentialsAsyncTask(TASK_INSERT).execute(harmoniaUserCredentials);
    }

    private void openMainActivity(HarmoniaUserCredentials harmoniaUserCredentials) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("harmoniaUserCredentials", harmoniaUserCredentials);
        startActivity(intent);
    }

    public class HarmoniaUserCredentialsAsyncTask extends AsyncTask<HarmoniaUserCredentials, Void, List> {
        private int taskCode;

        public HarmoniaUserCredentialsAsyncTask(int taskCode) {
            this.taskCode = taskCode;
        }

        @Override
        protected List doInBackground(HarmoniaUserCredentials... harmoniaUserCredentials) {
            switch (taskCode) {
                case TASK_INSERT:
                    appDatabase.harmoniaUserCredentialsDao().insertHarmoniaUserCredentials(harmoniaUserCredentials[0]);
                    break;
                case TASK_DELETE:
                    appDatabase.harmoniaUserCredentialsDao().deleteHarmoniaUserCredentials(harmoniaUserCredentials[0]);
                    break;
                case TASK_WIPE:
                    appDatabase.clearAllTables();
                    break;
            }
            //To return a new list with the updated data, we get all the data from the database again.
            return appDatabase.harmoniaUserCredentialsDao().getAllHarmoniaUserCredentials();
        }

        @Override
        protected void onPostExecute(List list) {
            super.onPostExecute(list);
            if (!list.isEmpty()) {
                HarmoniaUserCredentials harmoniaUserCredentials = (HarmoniaUserCredentials) list.get(0);
                openMainActivity(harmoniaUserCredentials);
            }
        }
    }
}
