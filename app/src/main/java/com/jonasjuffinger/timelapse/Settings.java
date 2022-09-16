package com.jonasjuffinger.timelapse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.concurrent.atomic.AtomicInteger;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by jonas on 2/18/17.
 */

class Settings {
    private static final String EXTRA_MINUTES = "com.jonasjuffinger.timelapse.MINUTES";
    private static final String EXTRA_SECONDS = "com.jonasjuffinger.timelapse.SECONDS";
    private static final String EXTRA_SHOTCOUNT = "com.jonasjuffinger.timelapse.SHOTCOUNT";
    private static final String EXTRA_DELAY = "com.jonasjuffinger.timelapse.DELAY";
    private static final String EXTRA_DISPLAYOFF = "com.jonasjuffinger.timelapse.DISPLAYOFF";
    private static final String EXTRA_SILENTSHUTTER = "com.jonasjuffinger.timelapse.SILENTSHUTTER";
    private static final String EXTRA_AEL = "com.jonasjuffinger.timelapse.AEL";
    private static final String EXTRA_MF = "com.jonasjuffinger.timelapse.MF";
    private static final String EXTRA_ISO = "com.jonasjuffinger.timelapse.ISO";
    private static final String EXTRA_AP = "com.jonasjuffinger.timelapse.AP";
    private static final String EXTRA_EXP_NUM = "com.jonasjuffinger.timelapse.EXP_NUM";
    private static final String EXTRA_EXP_DEN = "com.jonasjuffinger.timelapse.EXP_DEN";

    int minutes, seconds;
    int delay;
    int rawDelay;
    int shotCount, rawShotCount;
    boolean displayOff;
    boolean silentShutter;
    boolean ael;
    boolean mf;

    int iso;
    int aperture;
    int exposure_num; //numerator
    int exposure_den; //denominator


    Settings() {
        minutes = 1;
        seconds = 1;
        delay = 0;
        rawDelay = 0;
        shotCount = 1;
        rawShotCount = 1;
        displayOff = false;
        silentShutter = true;
        ael = true;
        mf = true;
        iso = 100;
        aperture = 710;
        exposure_num = 1;
        exposure_den = 1;
    }

    public Settings(int minutes, int seconds, int shotCount, int delay, boolean displayOff, boolean silentShutter, boolean ael, boolean mf, int iso, int aperture, int exposure_num, int exposure_den) {
        this.minutes = minutes;
        this.seconds = seconds;
        this.delay = delay;
        this.shotCount = shotCount;
        this.displayOff = displayOff;
        this.silentShutter = silentShutter;
        this.ael = ael;
        this.mf = mf;
        this.iso = iso;
        this.aperture = aperture;
        this.exposure_num = exposure_num;
        this.exposure_den = exposure_den;

    }

    void putInIntent(Intent intent) {
        intent.putExtra(EXTRA_MINUTES, minutes);
        intent.putExtra(EXTRA_SECONDS, seconds);
        intent.putExtra(EXTRA_SHOTCOUNT, shotCount);
        intent.putExtra(EXTRA_DELAY, delay);
        intent.putExtra(EXTRA_DISPLAYOFF, displayOff);
        intent.putExtra(EXTRA_SILENTSHUTTER, silentShutter);
        intent.putExtra(EXTRA_AEL, ael);
        intent.putExtra(EXTRA_MF, mf);
        intent.putExtra(EXTRA_MF, mf);
        intent.putExtra(EXTRA_MF, mf);
        intent.putExtra(EXTRA_MF, mf);
        intent.putExtra(EXTRA_MF, mf);
    }

    static Settings getFromIntent(Intent intent) {
        return new Settings(
                intent.getIntExtra(EXTRA_MINUTES, 1),
                intent.getIntExtra(EXTRA_SECONDS, 1),
                intent.getIntExtra(EXTRA_SHOTCOUNT, 1),
                intent.getIntExtra(EXTRA_DELAY, 1),
                intent.getBooleanExtra(EXTRA_DISPLAYOFF, false),
                intent.getBooleanExtra(EXTRA_SILENTSHUTTER, true),
                intent.getBooleanExtra(EXTRA_AEL, false),
                intent.getBooleanExtra(EXTRA_MF, true),
                intent.getIntExtra(EXTRA_ISO, 100),
                intent.getIntExtra(EXTRA_AP, 500),
                intent.getIntExtra(EXTRA_EXP_NUM, 1),
                intent.getIntExtra(EXTRA_EXP_DEN, 320)

        );
    }

    void save(Context context)
    {
        SharedPreferences sharedPref = getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("shotCount", rawShotCount);
        editor.putInt("delay", rawDelay);
        editor.putBoolean("silentShutter", silentShutter);
        editor.putBoolean("ael", ael);
        editor.putBoolean("mf", mf);
        editor.putBoolean("displayOff", displayOff);
        editor.putInt("iso", iso);
        editor.putInt("aperture", aperture);
        editor.putInt("exposure_num", exposure_num);
        editor.putInt("exposure_den", exposure_den);

        editor.apply();
    }

    void load(Context context)
    {
        SharedPreferences sharedPref = getDefaultSharedPreferences(context);
        rawShotCount = sharedPref.getInt("shotCount", rawShotCount);
        rawDelay = sharedPref.getInt("delay", rawDelay);
        silentShutter = sharedPref.getBoolean("silentShutter", silentShutter);
        ael = sharedPref.getBoolean("ael", ael);
        mf = sharedPref.getBoolean("mf", mf);
        displayOff = sharedPref.getBoolean("displayOff", displayOff);
        iso = sharedPref.getInt("iso", iso);
        aperture = sharedPref.getInt("aperture", aperture);
        exposure_num = sharedPref.getInt("exposure_num", exposure_num);
        exposure_den = sharedPref.getInt("exposure_den", exposure_den);

    }
}
