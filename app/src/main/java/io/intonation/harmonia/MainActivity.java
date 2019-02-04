package io.intonation.harmonia;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private HarmoniaUserCredentials harmoniaUserCredentials;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        harmoniaUserCredentials = getIntent().getParcelableExtra("harmoniaUserCredentials");

        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = findViewById(R.id.navigation);

        mOnNavigationItemSelectedListener = item -> {
            Bundle bundle = new Bundle();
            bundle.putParcelable("harmoniaUserCredentials", harmoniaUserCredentials);
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    SoundCloudFragment soundCloudFragment = new SoundCloudFragment();
                    soundCloudFragment.setArguments(bundle);
                    fragmentTransaction.replace(R.id.fragmentPlaceholder, soundCloudFragment);
                    fragmentTransaction.commit();
                    return true;
                case R.id.navigation_dashboard:
                    SpotifyFragment spotifyFragment = new SpotifyFragment();
                    spotifyFragment.setArguments(bundle);
                    fragmentTransaction.replace(R.id.fragmentPlaceholder, spotifyFragment);
                    fragmentTransaction.commit();
                    return true;
                case R.id.navigation_notifications:
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setAction(Intent.ACTION_DELETE);
                    startActivity(intent);
                    return true;
            }
            return false;
        };

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(R.id.navigation_home);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
