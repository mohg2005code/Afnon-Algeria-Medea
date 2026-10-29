package tv.afnon.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;

/** Build override: Algeria/Médéa defaults. This file is copied over the extracted Afnon source by CI. */
final class Prefs {
    private Prefs() {}

    static SharedPreferences of(Context c) {
        return c.getSharedPreferences("afnon", Context.MODE_PRIVATE);
    }

    // The original Prefs.java is large; CI cannot patch binary ZIP contents directly.
    // This override is intentionally generated from the current Afnon source at build time.
    // DO NOT USE: placeholder.
}
