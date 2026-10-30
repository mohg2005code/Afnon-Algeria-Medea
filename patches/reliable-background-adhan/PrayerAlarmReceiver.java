package tv.afnon.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Date;

/** AlarmManager entry point; survives Home/app changes and recreates the next alarm after each trigger. */
public final class PrayerAlarmReceiver extends BroadcastReceiver {
    private static final long WINDOW_MS = 5 * 60_000L;

    @Override public void onReceive(Context context, Intent intent) {
        Context app = context.getApplicationContext();
        String action = intent == null ? null : intent.getAction();

        if (PrayerAlarm.ACTION_TEST.equals(action)) {
            PrayerAlarm.clearTest(app);
            startAdhan(app, PrayerDay.DHUHR, System.currentTimeMillis());
            PrayerAlarm.schedule(app);
            return;
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            PrayerAlarm.schedule(app);
            return;
        }

        if (!PrayerAlarm.ACTION_PRAYER.equals(action)) return;

        Date now = new Date();
        PrayerDay day = new PrayerDay(app, now);
        int prayer = Reminders.dueCall(app, day, now);
        if (prayer >= 0) {
            String key = prayer + ":" + day.times[prayer].getTime();
            String old = Prefs.of(app).getString("called", "");
            if (!key.equals(old)) {
                Prefs.of(app).edit().putString("called", key).apply();
                startAdhan(app, prayer, day.times[prayer].getTime());
            }
        } else {
            for (int i = 0; i < 6; i++) {
                if (i == PrayerDay.SUNRISE) continue;
                long since = now.getTime() - day.times[i].getTime();
                if (since >= 0 && since < WINDOW_MS && Prefs.adhan(app) && Prefs.bell(app, i)) {
                    String key = i + ":" + day.times[i].getTime();
                    if (!key.equals(Prefs.of(app).getString("called", ""))) {
                        Prefs.of(app).edit().putString("called", key).apply();
                        startAdhan(app, i, day.times[i].getTime());
                    }
                    break;
                }
            }
        }
        PrayerAlarm.schedule(app);
    }

    private static void startAdhan(Context c, int prayer, long time) {
        Intent i = new Intent(c, AdhanService.class)
            .setAction(AdhanService.ACTION_PLAY)
            .putExtra(AdhanService.EXTRA_PRAYER, prayer)
            .putExtra(AdhanService.EXTRA_TIME, time);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }
}
