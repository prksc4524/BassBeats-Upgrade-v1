package com.bassbeats.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.BassBoost;
import android.os.Handler;
import android.os.Looper;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.view.KeyEvent;

public class PlaybackService extends Service {
    public static final String PLAY = "com.bassbeats.player.PLAY";
    public static final String TOGGLE = "com.bassbeats.player.TOGGLE";
    public static final String NEXT = "com.bassbeats.player.NEXT";
    public static final String PREV = "com.bassbeats.player.PREV";
    public static final String STATE = "com.bassbeats.player.STATE";
    public static final String SKIP_NEXT = "com.bassbeats.player.SKIP_NEXT";
    public static final String SKIP_PREV = "com.bassbeats.player.SKIP_PREV";
    public static final String SEEK = "com.bassbeats.player.SEEK";
    public static final String EQ_BAND = "com.bassbeats.player.EQ_BAND";
    public static final String BASS = "com.bassbeats.player.BASS";

    private static final String CHANNEL_ID = "bass_playback";
    private static final int NOTIFICATION_ID = 42;
    private MediaPlayer player;
    private MediaSession session;
    private String title = "BASS BEATS";
    private String artist = "Offline Player";
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private final Handler ticker = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() { @Override public void run() { if (player != null) { boolean active=false; try { active=player.isPlaying(); } catch(Exception ignored) {} broadcastState(active); if(active) updateSession(true); } ticker.postDelayed(this, 500); } };

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Music playback", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("BASS BEATS playback controls");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        session = new MediaSession(this, "BASS BEATS");
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSession.Callback() {
            @Override public void onSeekTo(long pos) { if (player != null) try { player.seekTo((int)pos); broadcastState(player.isPlaying()); } catch (Exception ignored) {} }
            @Override public void onPlay() { resumePlayback(); }
            @Override public void onPause() { pausePlayback(); }
            @Override public void onSkipToNext() { sendBroadcast(new Intent(SKIP_NEXT)); }
            @Override public void onSkipToPrevious() { sendBroadcast(new Intent(SKIP_PREV)); }
            @Override public boolean onMediaButtonEvent(Intent intent) {
                KeyEvent event = intent == null ? null : intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                    int key = event.getKeyCode();
                    if (key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) { togglePlayback(); return true; }
                    if (key == KeyEvent.KEYCODE_MEDIA_PLAY) { resumePlayback(); return true; }
                    if (key == KeyEvent.KEYCODE_MEDIA_PAUSE) { pausePlayback(); return true; }
                    if (key == KeyEvent.KEYCODE_MEDIA_NEXT) { sendBroadcast(new Intent(SKIP_NEXT)); return true; }
                    if (key == KeyEvent.KEYCODE_MEDIA_PREVIOUS) { sendBroadcast(new Intent(SKIP_PREV)); return true; }
                }
                return super.onMediaButtonEvent(intent);
            }
        });
        session.setActive(true);
        ticker.post(tick);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (PLAY.equals(action)) startTrack(intent);
        else if (TOGGLE.equals(action)) togglePlayback();
        else if (NEXT.equals(action)) sendBroadcast(new Intent(SKIP_NEXT));
        else if (PREV.equals(action)) sendBroadcast(new Intent(SKIP_PREV));
        else if (SEEK.equals(action) && player != null) { try { player.seekTo(intent.getIntExtra("position", 0)); broadcastState(player.isPlaying()); } catch (Exception ignored) {} }
        else if (EQ_BAND.equals(action)) applyBand(intent.getIntExtra("band", 0), intent.getIntExtra("gain", 0));
        else if (BASS.equals(action)) applyBass(intent.getBooleanExtra("enabled", false), intent.getIntExtra("strength", 700));
        return START_NOT_STICKY;
    }

    private void startTrack(Intent intent) {
        String uriString = intent.getStringExtra("uri");
        if (uriString == null) { broadcastState(false); return; }
        title = intent.getStringExtra("title");
        artist = intent.getStringExtra("artist");
        if (title == null) title = "Unknown title";
        if (artist == null) artist = "Unknown artist";
        // Promote to foreground immediately, before asynchronous media preparation.
        startForeground(NOTIFICATION_ID, buildNotification(false));
        releasePlayer();
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setDataSource(this, Uri.parse(uriString));
            player.setOnPreparedListener(mp -> {
                mp.start();
                attachEffects(mp.getAudioSessionId());
                updateSession(true);
                updateNotification(true);
                broadcastState(true);
            });
            player.setOnCompletionListener(mp -> {
                updateSession(false);
                updateNotification(false);
                broadcastState(false);
                sendBroadcast(new Intent(SKIP_NEXT));
            });
            player.setOnErrorListener((mp, what, extra) -> {
                broadcastState(false);
                updateNotification(false);
                return true;
            });
            player.prepareAsync();
            updateSession(false);
        } catch (Exception e) {
            releasePlayer();
            broadcastState(false);
            updateNotification(false);
        }
    }

    private void togglePlayback() {
        if (player == null) return;
        if (player.isPlaying()) pausePlayback(); else resumePlayback();
    }
    private void resumePlayback() {
        if (player == null) return;
        try { player.start(); updateSession(true); updateNotification(true); broadcastState(true); } catch (IllegalStateException ignored) { }
    }
    private void pausePlayback() {
        if (player != null) {
            try { if (player.isPlaying()) player.pause(); } catch (IllegalStateException ignored) { }
        }
        updateSession(false); updateNotification(false); broadcastState(false);
    }
    private void updateSession(boolean isPlaying) {
        if (session == null) return;
        long duration = 0, position = 0;
        if (player != null) try { duration = player.getDuration(); position = player.getCurrentPosition(); } catch (IllegalStateException ignored) { }
        session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, title).putString(MediaMetadata.METADATA_KEY_ARTIST, artist).putLong(MediaMetadata.METADATA_KEY_DURATION, duration).build());
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SEEK_TO;
        session.setPlaybackState(new PlaybackState.Builder().setActions(actions).setState(isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, position, 1f).build());
    }
    private PendingIntent serviceAction(String action, int requestCode) {
        Intent i = new Intent(this, PlaybackService.class).setAction(action);
        return PendingIntent.getService(this, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private Notification buildNotification(boolean isPlaying) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        builder.setContentTitle(title).setContentText(artist).setSmallIcon(android.R.drawable.ic_media_play).setVisibility(Notification.VISIBILITY_PUBLIC).setOngoing(isPlaying)
            .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_previous, "Previous", serviceAction(PREV, 1)).build())
            .addAction(new Notification.Action.Builder(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play, isPlaying ? "Pause" : "Play", serviceAction(TOGGLE, 2)).build())
            .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_next, "Next", serviceAction(NEXT, 3)).build());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && session != null) {
            builder.setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0, 1, 2));
        }
        return builder.build();
    }
    private void updateNotification(boolean isPlaying) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification(isPlaying));
    }
    private void broadcastState(boolean isPlaying) {
        Intent i = new Intent(STATE).putExtra("playing", isPlaying).putExtra("title", title).putExtra("artist", artist);
        if (player != null) try { i.putExtra("duration", player.getDuration()).putExtra("position", player.getCurrentPosition()); } catch (IllegalStateException ignored) { }
        sendBroadcast(i);
    }
    private void attachEffects(int sessionId) {
        releaseEffects();
        try { equalizer = new Equalizer(0, sessionId); equalizer.setEnabled(true); } catch (Exception ignored) { equalizer = null; }
        try { bassBoost = new BassBoost(0, sessionId); bassBoost.setEnabled(false); } catch (Exception ignored) { bassBoost = null; }
    }
    private void applyBand(int band, int gain) {
        if (equalizer == null) return;
        try {
            short count = equalizer.getNumberOfBands();
            if (count <= 0) return;
            short actual = (short)Math.max(0, Math.min(count - 1, band));
            short[] range = equalizer.getBandLevelRange();
            int millibels = gain * 100;
            millibels = Math.max(range[0], Math.min(range[1], millibels));
            equalizer.setBandLevel(actual, (short)millibels);
        } catch (Exception ignored) { }
    }
    private void applyBass(boolean enabled, int strength) {
        if (bassBoost == null) return;
        try { bassBoost.setStrength((short)Math.max(0, Math.min(1000, strength))); bassBoost.setEnabled(enabled); } catch (Exception ignored) { }
    }
    private void releaseEffects() {
        try { if (equalizer != null) { equalizer.setEnabled(false); equalizer.release(); } } catch (Exception ignored) { }
        try { if (bassBoost != null) { bassBoost.setEnabled(false); bassBoost.release(); } } catch (Exception ignored) { }
        equalizer = null; bassBoost = null;
    }
    private void releasePlayer() { releaseEffects(); if (player != null) { try { player.setOnPreparedListener(null); player.setOnCompletionListener(null); player.release(); } catch (Exception ignored) { } player = null; } }
    @Override public void onDestroy() {
        releasePlayer();
        ticker.removeCallbacks(tick);
        if (session != null) { session.setActive(false); session.release(); session = null; }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
