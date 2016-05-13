package com.sensorberg.sdk.settings;

import android.content.SharedPreferences;

import com.android.sensorbergVolley.VolleyError;
import com.sensorberg.sdk.Constants;
import com.sensorberg.sdk.Logger;
import com.sensorberg.sdk.internal.Platform;
import com.sensorberg.sdk.internal.transport.SettingsCallback;

import org.json.JSONObject;

public class Settings implements SettingsCallback {

    private String advertisingIdentifier;

    public void setCallback(SettingsCallback callback) {
        this.callback = callback;
    }

    public interface SettingsCallback {
        SettingsCallback NONE = new SettingsCallback() {
            @Override
            public void onSettingsUpdateIntervalChange(Long updateIntervalMillies) {

            }

            @Override
            public void onSettingsBeaconLayoutUpdateIntervalChange(long newLayoutUpdateInterval) {

            }

            @Override
            public void onHistoryUploadIntervalChange(long newHistoryUploadInterval) {

            }
        };

        void onSettingsUpdateIntervalChange(Long updateIntervalMillies);

        void onSettingsBeaconLayoutUpdateIntervalChange(long newLayoutUpdateInterval);

        void onHistoryUploadIntervalChange(long newHistoryUploadInterval);
    }

    private long cacheTtl = SettingsDefaults.DEFAULT_CACHE_TTL;

    private final Platform platform;
    private final SharedPreferences preferences;

    private long layoutUpdateInterval = SettingsDefaults.DEFAULT_LAYOUT_UPDATE_INTERVAL;

    private long messageDelayWindowLength = SettingsDefaults.DEFAULT_MESSAGE_DELAY_WINDOW_LENGTH;

    private long exitTimeoutMillis = SettingsDefaults.DEFAULT_EXIT_TIMEOUT_MILLIS;

    private long foreGroundScanTime = SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME;
    private long foreGroundWaitTime = SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME;

    private long backgroundScanTime = SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME;
    private long backgroundWaitTime = SettingsDefaults.DEFAULT_BACKGROUND_WAIT_TIME;

    private long millisBetweenRetries =  SettingsDefaults.DEFAULT_MILLIS_BEETWEEN_RETRIES;
    private int maxRetries = SettingsDefaults.DEFAULT_MAX_RETRIES;

    private long historyUploadInterval = SettingsDefaults.DEFAULT_HISTORY_UPLOAD_INTERVAL;

    private long cleanBeaconMapRestartTimeout = SettingsDefaults.DEFAULT_CLEAN_BEACONMAP_ON_RESTART_TIMEOUT;
    private Long revision = null;
    private long settingsUpdateInterval = SettingsDefaults.DEFAULT_SETTINGS_UPDATE_INTERVAL;

    private boolean shouldRestoreBeaconStates = SettingsDefaults.DEFAULT_SHOULD_RESTORE_BEACON_STATE;

    private SettingsCallback callback = SettingsCallback.NONE;

    public Settings(Platform platform, SharedPreferences preferences){
        this.platform = platform;
        this.preferences = preferences;
    }

    public void restoreValuesFromPreferences(){
        if (preferences != null) {
            exitTimeoutMillis = preferences.getLong(Constants.SharedPreferencesKeys.Scanner.TIMEOUT_MILLIES, SettingsDefaults.DEFAULT_EXIT_TIMEOUT_MILLIS);
            foreGroundScanTime = preferences.getLong(Constants.SharedPreferencesKeys.Scanner.FORE_GROUND_SCAN_TIME, SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME);
            foreGroundWaitTime = preferences.getLong(Constants.SharedPreferencesKeys.Scanner.FORE_GROUND_WAIT_TIME, SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME);
            backgroundScanTime = preferences.getLong(Constants.SharedPreferencesKeys.Scanner.BACKGROUND_SCAN_TIME, SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME);
            backgroundWaitTime = preferences.getLong(Constants.SharedPreferencesKeys.Scanner.BACKGROUND_WAIT_TIME, SettingsDefaults.DEFAULT_BACKGROUND_WAIT_TIME);
            cleanBeaconMapRestartTimeout = preferences.getLong(Constants.SharedPreferencesKeys.Scanner.CLEAN_BEACON_MAP_RESTART_TIMEOUT, SettingsDefaults.DEFAULT_CLEAN_BEACONMAP_ON_RESTART_TIMEOUT);
            revision = preferences.getLong(Constants.SharedPreferencesKeys.Settings.REVISION, Long.MIN_VALUE);

            settingsUpdateInterval = preferences.getLong(Constants.SharedPreferencesKeys.Settings.UPDATE_INTERVAL, SettingsDefaults.DEFAULT_SETTINGS_UPDATE_INTERVAL);

            maxRetries = preferences.getInt(Constants.SharedPreferencesKeys.Network.MAX_RESOLVE_RETRIES, SettingsDefaults.DEFAULT_MAX_RETRIES);
            millisBetweenRetries =   preferences.getLong(Constants.SharedPreferencesKeys.Network.TIME_BETWEEN_RESOLVE_RETRIES, SettingsDefaults.DEFAULT_MILLIS_BEETWEEN_RETRIES);

            historyUploadInterval = preferences.getLong(Constants.SharedPreferencesKeys.Network.HISTORY_UPLOAD_INTERVAL, SettingsDefaults.DEFAULT_HISTORY_UPLOAD_INTERVAL);
            layoutUpdateInterval = preferences.getLong(Constants.SharedPreferencesKeys.Network.BEACON_LAYOUT_UPDATE_INTERVAL, SettingsDefaults.DEFAULT_HISTORY_UPLOAD_INTERVAL);
            shouldRestoreBeaconStates = preferences.getBoolean(Constants.SharedPreferencesKeys.Scanner.SHOULD_RESTORE_BEACON_STATES, SettingsDefaults.DEFAULT_SHOULD_RESTORE_BEACON_STATE);
            cacheTtl = preferences.getLong(Constants.SharedPreferencesKeys.Platform.CACHE_OBJECT_TIME_TO_LIVE, SettingsDefaults.DEFAULT_CACHE_TTL);
            advertisingIdentifier = preferences.getString(Constants.SharedPreferencesKeys.Network.ADVERTISING_IDENTIFIER, null);
        }
    }

    public void updateValues() {
        platform.getTransport().getSettings(this);
    }

    @Override
    public void nothingChanged() {
        //all is good nothing to do
        Logger.log.logSettingsUpdateState("nothingChanged");
    }

    @Override
    public void onFailure(VolleyError e) {
        Logger.log.logSettingsUpdateState("onFailure");
        Logger.log.logError("settings update failed", e);
    }

    @Override
    public void onSettingsFound(JSONObject settings) {
        Logger.log.logSettingsUpdateState("onSettingsFound: " + revision);

        if (settings == null) {
            settings = new JSONObject();
            preferences.edit().clear().apply();
        }

        exitTimeoutMillis = settings.optLong("scanner.exitTimeoutMillis", SettingsDefaults.DEFAULT_EXIT_TIMEOUT_MILLIS);
        foreGroundScanTime = settings.optLong("scanner.foreGroundScanTime", SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME);
        foreGroundWaitTime = settings.optLong("scanner.foreGroundWaitTime", SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME);

        backgroundScanTime = settings.optLong("scanner.backgroundScanTime", SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME);
        backgroundWaitTime = settings.optLong("scanner.backgroundWaitTime", SettingsDefaults.DEFAULT_BACKGROUND_WAIT_TIME);

        cleanBeaconMapRestartTimeout = settings.optLong("scanner.cleanBeaconMapRestartTimeout", SettingsDefaults.DEFAULT_CLEAN_BEACONMAP_ON_RESTART_TIMEOUT);

        messageDelayWindowLength = settings.optLong("presenter.messageDelayWindowLength", SettingsDefaults.DEFAULT_CLEAN_BEACONMAP_ON_RESTART_TIMEOUT);

        cacheTtl = settings.optLong("cache.objectTTL", SettingsDefaults.DEFAULT_CACHE_TTL);

        maxRetries = settings.optInt("network.maximumResolveRetries", SettingsDefaults.DEFAULT_MAX_RETRIES);
        millisBetweenRetries = settings.optLong("network.millisBetweenRetries", SettingsDefaults.DEFAULT_MILLIS_BEETWEEN_RETRIES);
        shouldRestoreBeaconStates = settings.optBoolean("scanner.restoreBeaconStates", SettingsDefaults.DEFAULT_SHOULD_RESTORE_BEACON_STATE);

        long newHistoryUploadIntervalMillis = settings.optLong("network.historyUploadInterval", SettingsDefaults.DEFAULT_HISTORY_UPLOAD_INTERVAL);
        if (newHistoryUploadIntervalMillis != historyUploadInterval){
            historyUploadInterval = newHistoryUploadIntervalMillis;
            callback.onHistoryUploadIntervalChange(newHistoryUploadIntervalMillis);
        }

        long newLayoutUpdateInterval = settings.optLong("network.beaconLayoutUpdateInterval", SettingsDefaults.DEFAULT_LAYOUT_UPDATE_INTERVAL);
        if (newLayoutUpdateInterval != layoutUpdateInterval){
            layoutUpdateInterval = newLayoutUpdateInterval;
            callback.onSettingsBeaconLayoutUpdateIntervalChange(newLayoutUpdateInterval);
        }

        final long newSettingsUpdateInterval = settings.optLong("settings.updateTime", SettingsDefaults.DEFAULT_SETTINGS_UPDATE_INTERVAL);
        if (newSettingsUpdateInterval != settingsUpdateInterval){
            settingsUpdateInterval = newSettingsUpdateInterval;
            callback.onSettingsUpdateIntervalChange(newSettingsUpdateInterval);
        }

        persistToPreferences();
    }

    public void historyUploadIntervalChanged(Long newHistoryUploadIntervalMillis) {
        if (newHistoryUploadIntervalMillis != historyUploadInterval){
            historyUploadInterval = newHistoryUploadIntervalMillis;
            callback.onHistoryUploadIntervalChange(newHistoryUploadIntervalMillis);
            persistToPreferences();
        }
    }

    private void persistToPreferences(){
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();

            if (revision != null) {
                editor.putLong(Constants.SharedPreferencesKeys.Settings.REVISION, revision);
            } else {
                editor.remove(Constants.SharedPreferencesKeys.Settings.REVISION);
            }

            editor.putLong(Constants.SharedPreferencesKeys.Scanner.TIMEOUT_MILLIES, exitTimeoutMillis);
            editor.putLong(Constants.SharedPreferencesKeys.Scanner.FORE_GROUND_SCAN_TIME, foreGroundScanTime);
            editor.putLong(Constants.SharedPreferencesKeys.Scanner.FORE_GROUND_WAIT_TIME, foreGroundWaitTime);
            editor.putLong(Constants.SharedPreferencesKeys.Scanner.BACKGROUND_SCAN_TIME, backgroundScanTime);
            editor.putLong(Constants.SharedPreferencesKeys.Scanner.BACKGROUND_WAIT_TIME, backgroundWaitTime);
            editor.putBoolean(Constants.SharedPreferencesKeys.Scanner.SHOULD_RESTORE_BEACON_STATES, shouldRestoreBeaconStates);

            editor.putLong(Constants.SharedPreferencesKeys.Settings.MESSAGE_DELAY_WINDOW_LENGTH, messageDelayWindowLength);
            editor.putLong(Constants.SharedPreferencesKeys.Settings.UPDATE_INTERVAL, settingsUpdateInterval);

            editor.putInt(Constants.SharedPreferencesKeys.Network.MAX_RESOLVE_RETRIES, maxRetries);
            editor.putLong(Constants.SharedPreferencesKeys.Network.TIME_BETWEEN_RESOLVE_RETRIES, millisBetweenRetries);
            editor.putLong(Constants.SharedPreferencesKeys.Network.HISTORY_UPLOAD_INTERVAL, historyUploadInterval);
            editor.putLong(Constants.SharedPreferencesKeys.Network.BEACON_LAYOUT_UPDATE_INTERVAL, layoutUpdateInterval);


            editor.apply();
        }
    }

    public long getLayoutUpdateInterval() {
        return layoutUpdateInterval;
    }

    public long getExitTimeout() {
        return exitTimeoutMillis;
    }

    public long getForeGroundScanTime() {
        return foreGroundScanTime;
    }

    public long getForeGroundWaitTime() {
        return foreGroundWaitTime;
    }

    public long getBackgroundScanTime() {
        return backgroundScanTime;
    }

    public long getBackgroundWaitTime() {
        return backgroundWaitTime;
    }

    public long getCleanBeaconMapRestartTimeout() {
        return cleanBeaconMapRestartTimeout;
    }

    public long getSettingsUpdateInterval() {
        return settingsUpdateInterval;
    }

    public long getMessageDelayWindowLength() {
        return messageDelayWindowLength;
    }

    public long getMillisBetweenRetries() {
        return millisBetweenRetries;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getHistoryUploadInterval() {
        return historyUploadInterval;
    }

    public boolean shouldRestoreBeaconStates() {
        return shouldRestoreBeaconStates;
    }

    public long getCacheTtl() {
        return cacheTtl;
    }

    public String getAdvertisingIdentifier() {
        return advertisingIdentifier;
    }

    public void setAdvertisingIdentifier(String advertisingIdentifier) {
        this.advertisingIdentifier = advertisingIdentifier;
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            if (advertisingIdentifier == null) {
                editor.remove(Constants.SharedPreferencesKeys.Network.ADVERTISING_IDENTIFIER);
            } else {
                editor.putString(Constants.SharedPreferencesKeys.Network.ADVERTISING_IDENTIFIER, advertisingIdentifier);
                editor.apply();
            }
        }
    }
}
