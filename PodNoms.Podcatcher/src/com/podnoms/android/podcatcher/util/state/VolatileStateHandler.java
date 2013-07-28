package com.podnoms.android.podcatcher.util.state;

import java.util.Hashtable;

public class VolatileStateHandler implements IStateHandler {
    private static Hashtable<String, Object> _bundles;
    private static VolatileStateHandler _instance;

    public static synchronized VolatileStateHandler I() {
        if (_instance == null) {
            _instance = new VolatileStateHandler();
            _bundles = new Hashtable<String, Object>();
        }
        return _instance;
    }

    @Override
    public void putBoolean(String key, Boolean value) {
        _bundles.put(key, value);
    }

    @Override
    public void putFloat(String key, float value) {
        _bundles.put(key, value);
    }

    @Override
    public void putInt(String key, int value) {
        _bundles.put(key, value);
    }

    @Override
    public void putLong(String key, long value) {
        _bundles.put(key, value);
    }

    @Override
    public void putString(String key, String value) {
        _bundles.put(key, value);
    }


    public void putObject(String key, Object value) {
        _bundles.put(key, value);
    }

    @Override
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    @Override
    public boolean getBoolean(String key, Boolean defaultValue) {
        if (_bundles.containsKey(key))
            return (Boolean) _bundles.get(key);

        return defaultValue;
    }

    @Override
    public float getFloat(String key) {
        if (_bundles.containsKey(key))
            return (Float) _bundles.get(key);
        return 0;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        if (_bundles.containsKey(key))
            return (Integer) _bundles.get(key);
        return defaultValue;
    }

    @Override
    public int getInt(String key) {
        return getInt(key, 0);
    }

    @Override
    public long getLong(String key) {
        if (_bundles.containsKey(key))
            return (Long) _bundles.get(key);
        return 0;
    }

    @Override
    public String getString(String key, String defaultValue) {
        if (_bundles.containsKey(key))
            return _bundles.get(key).toString();
        return defaultValue;
    }

    @Override
    public String getString(String key) {
        return getString(key, "");
    }

    public Object getObject(String key) {
        if (_bundles.containsKey(key))
            return _bundles.get(key);
        return null;
    }

    @Override
    public void remove(String key) {
        _bundles.remove(key);
    }

    @Override
    public boolean has(String key) {
        return _bundles.containsKey(key);
    }

}
