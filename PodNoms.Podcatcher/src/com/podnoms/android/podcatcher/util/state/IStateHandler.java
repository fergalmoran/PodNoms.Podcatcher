package com.podnoms.android.podcatcher.util.state;

public interface IStateHandler {
    //TODO: Add default overrides

    void putBoolean(String key, Boolean value);
    void putFloat(String key, float value);
    void putInt(String key, int value);
    void putLong(String key, long value);
    void putString(String key, String value);

    boolean getBoolean(String key, Boolean defaultValue);
    boolean getBoolean(String key);

    float getFloat(String key);

    int getInt(String key, int defaultValue);
    int getInt(String key);

    long getLong(String key);

    String getString(String key, String defaultValue);
    String getString(String key);

    boolean has(String value);
    void remove(String value);
}
