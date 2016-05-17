package com.sensorberg.sdk.scanner;

import android.test.AndroidTestCase;

import com.sensorberg.sdk.settings.Settings;
import com.sensorberg.sdk.settings.SettingsDefaults;
import com.sensorberg.sdk.testUtils.TestPlatform;

import static com.sensorberg.sdk.testUtils.SensorbergMatcher.isEntryEvent;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;

import static org.fest.assertions.api.Assertions.assertThat;


public class TheBackgroundScannerShould extends AndroidTestCase{
    private TestPlatform platform;
    private UIScanner tested;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        platform = new TestPlatform();
        setUpScanner();

        tested.hostApplicationInBackground();

        tested.start();
    }

    private void setUpScanner() {
        tested = new UIScanner(new Settings(platform, null), platform);
    }

    public void test_be_in_background_mode(){
        assertThat(tested.waitTime).isEqualTo(SettingsDefaults.DEFAULT_BACKGROUND_WAIT_TIME);
        assertThat(tested.scanTime).isEqualTo(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME);
    }


   public void test_detect_no_beacon_because_it_is_sleeping(){
       platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME + 1);

       ScannerListener mockListener = Mockito.mock(ScannerListener.class);
       tested.addScannerListener(mockListener);

       platform.fakeIBeaconSighting();

       verifyZeroInteractions(mockListener);

   }

    public void test_detect_beacon_because_sleep_has_ended(){
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME - 1);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME + 1);

        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_BACKGROUND_WAIT_TIME);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_BACKGROUND_WAIT_TIME + 1);

        ScannerListener mockListener = Mockito.mock(ScannerListener.class);
        tested.addScannerListener(mockListener);

        platform.fakeIBeaconSighting();

        verify(mockListener).onScanEventDetected(isEntryEvent());
    }

    public void test_background_times_should_be_switched_to_foreground_times() {
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME - 1);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME + 1);

        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_BACKGROUND_WAIT_TIME / 2);

        tested.hostApplicationInForeground();
        assertThat(tested.waitTime).isNotEqualTo(SettingsDefaults.DEFAULT_BACKGROUND_WAIT_TIME);
        assertThat(tested.waitTime).isEqualTo(SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME);
        assertThat(tested.scanTime).isNotEqualTo(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME);
        assertThat(tested.scanTime).isEqualTo(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME);
    }

    public void test_detect_beacon_because_sleep_has_ended_due_to_foreground(){
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME - 1);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME + 1);

        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_BACKGROUND_WAIT_TIME / 2);

        tested.hostApplicationInForeground();

        platform.clock.increaseTimeInMillis(1);

        ScannerListener mockListener = Mockito.mock(ScannerListener.class);
        tested.addScannerListener(mockListener);

        platform.fakeIBeaconSighting();

        verify(mockListener).onScanEventDetected(isEntryEvent());
    }
}
