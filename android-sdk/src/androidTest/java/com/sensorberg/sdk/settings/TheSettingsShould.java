package com.sensorberg.sdk.settings;

import com.sensorberg.sdk.Constants;
import com.sensorberg.sdk.internal.OkHttpClientTransport;
import com.sensorberg.sdk.testUtils.TestPlatform;

import org.fest.assertions.api.Assertions;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;

import util.TestConstants;

public class TheSettingsShould extends AndroidTestCase {

    Settings tested;

    Settings untouched;

    private TestPlatform platform;

    private SharedPreferences testedSharedPreferences;

    private SharedPreferences untouchedSharedPreferences;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        platform = new TestPlatform().setContext(getContext());
        platform.setTransport(new OkHttpClientTransport(platform, null));
        platform.getTransport().setApiToken(TestConstants.API_TOKEN);
        testedSharedPreferences = getContext().getSharedPreferences(Long.toString(System.currentTimeMillis()), Context.MODE_PRIVATE);
        tested = new Settings(platform, testedSharedPreferences);

        untouchedSharedPreferences = getContext().getSharedPreferences(Long.toString(System.currentTimeMillis()), Context.MODE_PRIVATE);
        untouched = new Settings(platform, untouchedSharedPreferences);
    }

    public void test_initial_values_should_be_identical() throws Exception {
        Assertions.assertThat(untouched.getBackgroundScanTime()).isEqualTo(tested.getBackgroundScanTime());
        Assertions.assertThat(untouched.getBackgroundWaitTime()).isEqualTo(tested.getBackgroundWaitTime());
        Assertions.assertThat(untouched.getExitTimeout()).isEqualTo(tested.getExitTimeout());
        Assertions.assertThat(untouched.getForeGroundScanTime()).isEqualTo(tested.getForeGroundScanTime());
        Assertions.assertThat(untouched.getForeGroundWaitTime()).isEqualTo(tested.getForeGroundWaitTime());
    }

    public void test_fetch_values_from_the_network() throws Exception {
        tested.updateValues();

        Assertions.assertThat(untouched.getBackgroundScanTime()).isNotEqualTo(tested.getBackgroundScanTime());
        Assertions.assertThat(untouched.getBackgroundWaitTime()).isNotEqualTo(tested.getBackgroundWaitTime());
        Assertions.assertThat(untouched.getExitTimeout()).isNotEqualTo(tested.getExitTimeout());
        Assertions.assertThat(untouched.getForeGroundScanTime()).isNotEqualTo(tested.getForeGroundScanTime());
        Assertions.assertThat(untouched.getForeGroundWaitTime()).isNotEqualTo(tested.getForeGroundWaitTime());
    }

    public void test_update_the_default_values_if_the_constants_change() throws Exception {
        //prepare the shared preferences
        SharedPreferences.Editor editor = testedSharedPreferences.edit();
        editor.putLong(Constants.SharedPreferencesKeys.Scanner.BACKGROUND_WAIT_TIME, Constants.Time.ONE_MINUTE * 6);
        editor.commit();

        //load the last values from the shared preferences, as it happens after a restart
        tested.restoreValuesFromPreferences();
        Assertions.assertThat(tested.getBackgroundWaitTime()).isEqualTo(Constants.Time.ONE_MINUTE * 6);

        //simulating a settings request without content
        tested.onSettingsFound(new JSONObject());

        Assertions.assertThat(tested.getBackgroundWaitTime()).isEqualTo(SettingsDefaults.DEFAULT_BACKGROUND_WAIT_TIME);
    }

    public void test_advertising_id_gets_persisted() throws Exception {
        //prepare the shared preferences

        Assertions.assertThat(untouched.getAdvertisingIdentifier()).isNull();
        Assertions.assertThat(tested.getAdvertisingIdentifier()).isEqualTo(untouched.getAdvertisingIdentifier());
        Assertions.assertThat(testedSharedPreferences.getString(Constants.SharedPreferencesKeys.Network.ADVERTISING_IDENTIFIER, "")).isEmpty();

        tested.setAdvertisingIdentifier("TEST_ID");
        Assertions.assertThat(testedSharedPreferences.getString(Constants.SharedPreferencesKeys.Network.ADVERTISING_IDENTIFIER, "")).isEqualTo(
                "TEST_ID");

        //load the last values from the shared preferences, as it happens after a restart
        tested.restoreValuesFromPreferences();
        Assertions.assertThat(tested.getAdvertisingIdentifier()).isEqualTo("TEST_ID");

        //simulating a settings request without content
        tested.onSettingsFound(new JSONObject());
        Assertions.assertThat(tested.getAdvertisingIdentifier()).isEqualTo("TEST_ID");
    }
}
