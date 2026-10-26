
package com.bassbeats.player;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.session.MediaSession;
import android.view.KeyEvent;
import android.os.Build;
import android.os.IBinder;

public class PlaybackService extends Service {

    private MediaSession session;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "bass_playback",
                    "Music playback",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription("BASS BEATS playback controls");

            NotificationManager manager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        session = new MediaSession(this, "BASS BEATS");

        session.setCallback(new MediaSession.Callback() {

            @Override
            public void onPlay() {
                resume();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                sendBroadcast(new Intent(
                        "com.bassbeats.player.SKIP_NEXT"
                ));
            }

            @Override
            public void onSkipToPrevious() {
                sendBroadcast(new Intent(
                        "com.bassbeats.player.SKIP_PREV"
                ));
            }

            @Override
            public boolean onMediaButtonEvent(Intent intent) {
                KeyEvent event = intent.getParcelableExtra(
                        Intent.EXTRA_KEY_EVENT
                );

                if (event != null &&
                        event.getAction() == KeyEvent.ACTION_DOWN) {

                    int keyCode = event.getKeyCode();

                    if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                        toggle();
                        return true;
                    }

                    if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                        resume();
                        return true;
                    }

                    if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                        pause();
                        return true;
                    }
                }

                return super.onMediaButtonEvent(intent);
            }
        });

        session.setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
        );

        session.setActive(true);
    }

    private void resume() {
        sendBroadcast(new Intent(
                "com.bassbeats.player.ACTION_PLAY"
        ));
    }

    private void pause() {
        sendBroadcast(new Intent(
                "com.bassbeats.player.ACTION_PAUSE"
        ));
    }

    private void toggle() {
        sendBroadcast(new Intent(
                "com.bassbeats.player.ACTION_TOGGLE"
        ));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (session != null) {
            session.setActive(false);
            session.release();
            session = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
