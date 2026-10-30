package tv.afnon.home;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

import java.io.IOException;

/** Foreground media service: the adhan continues after Home/another app takes the foreground. */
public final class AdhanService extends Service {
    static final String ACTION_PLAY = "tv.afnon.home.PLAY_ADHAN";
    static final String EXTRA_PRAYER = "prayer";
    static final String EXTRA_TIME = "time";
    private static final int NOTIFICATION_ID = 2407;
    private MediaPlayer player;
    private AudioFocusRequest focus;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        int prayer = intent == null ? PrayerDay.DHUHR : intent.getIntExtra(EXTRA_PRAYER, PrayerDay.DHUHR);
        startForeground(NOTIFICATION_ID, notification());
        play(prayer);
        return START_NOT_STICKY;
    }

    private Notification notification() {
        String channelId = "adhan_playback";
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26 && nm != null) {
            NotificationChannel ch = new NotificationChannel(channelId, "الأذان", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }
        if (Build.VERSION.SDK_INT >= 26) {
            return new Notification.Builder(this, channelId)
                .setContentTitle("الأذان")
                .setContentText("يُشغّل الأذان")
                .setSmallIcon(R.drawable.ic_arch)
                .setOngoing(true)
                .build();
        }
        return new Notification.Builder(this)
            .setContentTitle("الأذان")
            .setContentText("يُشغّل الأذان")
            .setSmallIcon(R.drawable.ic_arch)
            .setOngoing(true)
            .build();
    }

    private void play(int prayer) {
        stopPlayback();
        String id = Prefs.voice(this, prayer == PrayerDay.FAJR);
        MediaPlayer mp = new MediaPlayer();
        AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();
        try (android.content.res.AssetFileDescriptor fd = getAssets().openFd("adhan/" + id + ".mp3")) {
            mp.setAudioAttributes(attrs);
            mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            mp.prepare();
        } catch (IOException | RuntimeException e) {
            mp.release();
            stopSelf();
            return;
        }
        AudioManager am = getSystemService(AudioManager.class);
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs)
            .build();
        if (am != null) am.requestAudioFocus(focus);
        player = mp;
        mp.setOnCompletionListener(p -> stopSelf());
        mp.start();
    }

    private void stopPlayback() {
        if (player != null) {
            try { player.stop(); } catch (IllegalStateException ignored) {}
            player.release();
            player = null;
        }
        AudioManager am = getSystemService(AudioManager.class);
        if (am != null && focus != null) am.abandonAudioFocusRequest(focus);
        focus = null;
    }

    @Override public void onDestroy() {
        stopPlayback();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
