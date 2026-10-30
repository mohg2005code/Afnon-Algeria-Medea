package tv.afnon.home;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Date;

/** Schedules the next prayer independently of the AccessibilityService. */
final class PrayerAlarm {
    static final String ACTION_PRAYER = "tv.afnon.home.PRAYER_ALARM";
    static final String ACTION_TEST = "tv.afnon.home.TEST_ADHAN_ALARM";
    private static final int REQUEST = 2407;
    private static final int TEST_REQUEST = 2408;
    private static final String TEST_PENDING = "background_test_pending";

    private PrayerAlarm() {}

    static void schedule(Context c) {
        Context app = c.getApplicationContext();
        PrayerDay day = new PrayerDay(app, new Date());
        long when = day.nextTime.getTime();
        if (when <= System.currentTimeMillis() + 1000L) return;
        AlarmManager am = app.getSystemService(AlarmManager.class);
        if (am == null) return;
        PendingIntent pi = pending(app, ACTION_PRAYER, REQUEST);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, pi), pi);
    }

    static void scheduleTest(Context c) {
        Context app = c.getApplicationContext();
        android.content.SharedPreferences p = Prefs.of(app);
        if (p.getBoolean(TEST_PENDING, false)) return;
        AlarmManager am = app.getSystemService(AlarmManager.class);
        if (am == null) return;
        long when = System.currentTimeMillis() + 15_000L;
        PendingIntent pi = pending(app, ACTION_TEST, TEST_REQUEST);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, pi), pi);
        p.edit().putBoolean(TEST_PENDING, true).apply();
    }

    static void clearTest(Context c) {
        Prefs.of(c.getApplicationContext()).edit().remove(TEST_PENDING).apply();
    }

    private static PendingIntent pending(Context c, String action, int request) {
        Intent i = new Intent(c, PrayerAlarmReceiver.class).setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, request, i, flags);
    }
}
