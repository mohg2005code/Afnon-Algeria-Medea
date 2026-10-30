#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
APP="$ROOT/app/src/main"
PATCH="$ROOT/patches/reliable-background-adhan"

python3 - <<'PY'
from pathlib import Path

p = Path('app/src/main/java/tv/afnon/home/Prefs.java')
s = p.read_text()
old = 'of(c).getString("city", "cairo")'
if old not in s:
    raise SystemExit('city default not found')
p.write_text(s.replace(old, 'of(c).getString("city", "medea")', 1))

p = Path('app/src/main/java/tv/afnon/home/NowActivity.java')
s = p.read_text()
old = 'AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)'
if old not in s:
    raise SystemExit('NowActivity audio focus not found')
p.write_text(s.replace(old, 'AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)', 1))

p = Path('app/src/main/java/tv/afnon/home/MainActivity.java')
s = p.read_text()
old = 'super.onCreate(state);\n        runTest(getIntent());'
if old not in s:
    raise SystemExit('MainActivity onCreate marker not found')
s = s.replace(old, 'super.onCreate(state);\n        PrayerAlarm.schedule(this);\n        PrayerAlarm.scheduleTest(this);\n        runTest(getIntent());', 1)
old = 'RemoteKeys.ensureOn(this);\n        if (!builtApps.equals(appsKey())) build();'
if old not in s:
    raise SystemExit('MainActivity onResume marker not found')
s = s.replace(old, 'RemoteKeys.ensureOn(this);\n        PrayerAlarm.schedule(this);\n        if (!builtApps.equals(appsKey())) build();', 1)
p.write_text(s)
PY

mkdir -p app/src/main/java/tv/afnon/home
cp "$PATCH/PrayerAlarm.java" app/src/main/java/tv/afnon/home/PrayerAlarm.java
cp "$PATCH/PrayerAlarmReceiver.java" app/src/main/java/tv/afnon/home/PrayerAlarmReceiver.java
cp "$PATCH/AdhanService.java" app/src/main/java/tv/afnon/home/AdhanService.java

python3 - <<'PY'
from pathlib import Path
p = Path('app/src/main/AndroidManifest.xml')
s = p.read_text()
needle = '<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />'
if needle not in s:
    raise SystemExit('manifest permission marker not found')
add = needle + '''\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />\n    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />\n    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />'''
s = s.replace(needle, add, 1)
marker = '        <!-- Remote buttons. Turned on with:'
if marker not in s:
    raise SystemExit('manifest receiver marker not found')
insert = '''        <receiver\n            android:name=".PrayerAlarmReceiver"\n            android:exported="false">\n            <intent-filter>\n                <action android:name="tv.afnon.home.PRAYER_ALARM" />\n                <action android:name="tv.afnon.home.TEST_ADHAN_ALARM" />\n                <action android:name="android.intent.action.BOOT_COMPLETED" />\n                <action android:name="android.intent.action.TIME_SET" />\n                <action android:name="android.intent.action.TIMEZONE_CHANGED" />\n            </intent-filter>\n        </receiver>\n\n        <service\n            android:name=".AdhanService"\n            android:exported="false"\n            android:foregroundServiceType="mediaPlayback" />\n\n'''
s = s.replace(marker, insert + marker, 1)
p.write_text(s)
PY

# The source intentionally excludes the recordings. Use the project's own release helper.
bash tools/fetch-adhans.sh

# Avoid crashing MainActivity on Android builds where exact-alarm access is not granted.
python3 - <<'PY'
from pathlib import Path
p = Path('app/src/main/java/tv/afnon/home/PrayerAlarm.java')
s = p.read_text()
s = s.replace('        am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, pi), pi);', '''        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, pi), pi);
        } catch (SecurityException denied) {
            // Some Android TV firmware does not grant exact-alarm access by default.
            // Keep the app usable and fall back to a small inexact window instead of crashing.
            am.setWindow(AlarmManager.RTC_WAKEUP, when, 60_000L, pi);
        }''', 1)
s = s.replace('        am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, pi), pi);', '''        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, pi), pi);
        } catch (SecurityException denied) {
            am.setWindow(AlarmManager.RTC_WAKEUP, when, 30_000L, pi);
        }''', 1)
p.write_text(s)
PY
