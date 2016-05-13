package com.sensorberg.sdk.scanner;

import android.test.AndroidTestCase;

import com.sensorberg.sdk.settings.SettingsDefaults;
import com.sensorberg.sdk.settings.Settings;
import com.sensorberg.sdk.testUtils.TestPlatform;

import org.mockito.Mockito;

import static com.sensorberg.sdk.testUtils.SensorbergMatcher.isEntryEvent;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Created by Burak on 22.09.2014.
 */
public class TheForegroundScannerShould extends AndroidTestCase {
    private TestPlatform platform;
    private UIScanner tested;

    @Override
    public void setUp() throws Exception {
        platform = new TestPlatform();
        setUpScanner();

        tested.start();
    }

    private void setUpScanner() {
        tested = new UIScanner(new Settings(platform, null), platform);
        tested.waitTime = SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME;
        tested.scanTime = SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME;
    }


    public void test_be_in_foreground_mode(){
        assertThat(tested.waitTime).isEqualTo(SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME);
        assertThat(tested.scanTime).isEqualTo(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME);
    }

    public void test_detect_no_beacon_because_it_is_sleeping(){
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + 1);

        ScannerListener mockListener = Mockito.mock(ScannerListener.class);
        tested.addScannerListener(mockListener);

        platform.fakeIBeaconSighting();

        verifyZeroInteractions(mockListener);
    }

    public void test_detect_beacon_because_sleep_has_ended(){
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME - 1);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + 1);

        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME + 1);

        ScannerListener mockListener = Mockito.mock(ScannerListener.class);
        tested.addScannerListener(mockListener);

        platform.fakeIBeaconSighting();

        verify(mockListener).onScanEventDetected(isEntryEvent());
    }

    public void test_foreground_times_should_be_switched_to_background_times() {
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME - 1);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + 1);

        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME / 2);

        tested.hostApplicationInBackground();
        assertThat(tested.waitTime).isEqualTo(SettingsDefaults.DEFAULT_BACKGROUND_WAIT_TIME);
        assertThat(tested.waitTime).isNotEqualTo(SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME);
        assertThat(tested.scanTime).isEqualTo(SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME);
        assertThat(tested.scanTime).isNotEqualTo(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME);
    }

    public void test_do_not_detect_beacon_because_sleep_has_not_ended_due_to_background(){
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME - 1);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + 1);

        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME / 2);

        tested.hostApplicationInBackground();

        ScannerListener mockListener = Mockito.mock(ScannerListener.class);
        tested.addScannerListener(mockListener);

        platform.fakeIBeaconSighting();

        verifyZeroInteractions(mockListener);
    }

    public void test_background_scan_times_are_applied(){

        ScannerListener mockListener = Mockito.mock(ScannerListener.class);
        tested.addScannerListener(mockListener);

        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME - 1);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + 1);

        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME / 2);

        tested.hostApplicationInBackground();

        //finish the foreground wait time
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME - 1);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME + 1);


        //set time just before the end of the Background scan time
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME + SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME -1);
        //mock a beacon, since the scanner is active, this one should be recognized
        platform.fakeIBeaconSighting();

        verify(mockListener).onScanEventDetected(isEntryEvent());
    }

    public void test_background_wait_starts(){

        ScannerListener mockListener = Mockito.mock(ScannerListener.class);
        tested.addScannerListener(mockListener);

        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME - 1);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + 1);

        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME / 2);

        tested.hostApplicationInBackground();

        //finish the foreground wait time
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME - 1);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME);
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME + 1);


        //set time just before the end of the Background scan time
        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME + SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME + 1);
        //mock a beacon, since the scanner is should be inactive, this should not be recognized
        platform.fakeIBeaconSighting();

        //since it is one millis after, there should not be interactions
        verifyZeroInteractions(mockListener);

        platform.clock.setNowInMillis(SettingsDefaults.DEFAULT_FOREGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_FOREGROUND_WAIT_TIME + SettingsDefaults.DEFAULT_BACKGROUND_SCAN_TIME + SettingsDefaults.DEFAULT_BACKGROUND_WAIT_TIME -1);
        platform.fakeIBeaconSighting();

        //is is one milli before the end...
        verifyZeroInteractions(mockListener);
    }
}
