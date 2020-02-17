package io.intonation.harmonia;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SoundCloudPlaybackService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String notificationId = intent.getStringExtra("notificationId");
        Notification notification = intent.getExtras().getParcelable("notification");
        startForeground(Integer.parseInt(notificationId), notification);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(false);
    }
}