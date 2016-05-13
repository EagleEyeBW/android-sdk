package com.sensorberg.sdk.scanner;

import android.test.AndroidTestCase;

import com.sensorberg.sdk.settings.Settings;
import com.sensorberg.sdk.settings.SettingsDefaults;
import com.sensorberg.sdk.testUtils.TestPlatform;

import static org.fest.assertions.api.Assertions.assertThat;

public class TheDefaultScanner extends AndroidTestCase {
    private TestPlatform platform;
    private Scanner tested;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        platform = new TestPlatform();
        tested = new Scanner(new Settings(platform, null), platform, false);

        tested.start();
    }

    public void test_should_be_initially_setup_to_scan_in_with_the_background_configuration() throws Exception {
        assertThat(tested.waitTime).isEqualTo(SettingsDefaults.DEFAULT_BACKGROUND_WAIT_TIME);
        assertThat(tested.scanTime).isEqualTo(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME);
    }
}



