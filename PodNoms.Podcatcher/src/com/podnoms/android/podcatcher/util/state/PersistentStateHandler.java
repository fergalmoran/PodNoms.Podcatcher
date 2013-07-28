package com.podnoms.android.podcatcher.util.state;

import android.content.SharedPreferences;
import com.podnoms.android.podcatcher.PodNomsApplication;

public class PersistentStateHandler implements IStateHandler {

    private static PersistentStateHandler _instance;
    private static SharedPreferences _prefs;

    public static synchronized PersistentStateHandler I() {
        if (_instance == null) {
            _instance = new PersistentStateHandler();
            _prefs = PodNomsApplication.getContext().getSharedPreferences("__persistance_", 0);
        }

        return _instance;
    }

    @Override
    public void putBoolean(String key, Boolean value) {
        _prefs.edit().putBoolean(key, value).commit();
    }

    @Override
    public void putFloat(String key, float value) {
        _prefs.edit().putFloat(key, value).commit();
    }

    @Override
    public void putInt(String key, int value) {
        _prefs.edit().putInt(key, value).commit();
    }

    @Override
    public void putLong(String key, long value) {
        _prefs.edit().putLong(key, value).commit();
    }

    @Override
    public void putString(String key, String value) {
        _prefs.edit().putString(key, value).commit();
    }

    @Override
    public boolean getBoolean(String key, Boolean defaultValue) {
        return _prefs.getBoolean(key, defaultValue);
    }

    @Override
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    @Override
    public float getFloat(String key) {
        return _prefs.getFloat(key, 0);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return _prefs.getInt(key, defaultValue);
    }

    @Override
    public int getInt(String key) {
        return getInt(key, 0);
    }

    @Override
    public long getLong(String key) {
        return _prefs.getLong(key, 0);
    }

    @Override
    public String getString(String key) {
        return getString(key, "");
    }

    @Override
    public String getString(String key, String defaultValue) {
        return _prefs.getString(key, defaultValue);
    }

    @Override
    public boolean has(String value) {
        return _prefs.contains(value);
    }

    @Override
    public void remove(String value) {
        _prefs.edit().remove(value);
    }
}
