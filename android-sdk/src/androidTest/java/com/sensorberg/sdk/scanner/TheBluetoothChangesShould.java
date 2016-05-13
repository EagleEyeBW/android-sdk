package com.sensorberg.sdk.scanner;

import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;

import com.sensorberg.sdk.settings.Settings;
import com.sensorberg.sdk.settings.SettingsDefaults;
import com.sensorberg.sdk.testUtils.TestPlatform;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import util.Utils;

import static com.sensorberg.sdk.testUtils.SensorbergMatcher.isEntryEvent;
import static com.sensorberg.sdk.testUtils.SensorbergMatcher.isExitEvent;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
public class TheBluetoothChangesShould {

    Scanner tested;
    private TestPlatform platform;
    private long RANDOM_VALUE_THAT_IS_SHORTER_THAN_CLEAN_BEACONMAP_ON_RESTART_TIMEOUT_BUT_LONGER_THAN_EXIT_EVENT_DELAY = Utils.THIRTY_SECONDS;
    private Settings settings;

    @Before
    public void setUp() throws Exception {

        platform = new TestPlatform();

        settings = new Settings(platform, null);
        tested = new Scanner(settings, platform, false);
        tested.scanTime = Long.MAX_VALUE;
        tested.waitTime = 0L;
        tested.start();
        platform.clock.setNowInMillis(0);
    }

    @Test
    public void assert_random_values_are_within_range() {
        assertThat(RANDOM_VALUE_THAT_IS_SHORTER_THAN_CLEAN_BEACONMAP_ON_RESTART_TIMEOUT_BUT_LONGER_THAN_EXIT_EVENT_DELAY).isLessThan(SettingsDefaults.DEFAULT_CLEAN_BEACONMAP_ON_RESTART_TIMEOUT);
        assertThat(RANDOM_VALUE_THAT_IS_SHORTER_THAN_CLEAN_BEACONMAP_ON_RESTART_TIMEOUT_BUT_LONGER_THAN_EXIT_EVENT_DELAY).isGreaterThan(settings.getExitTimeout());
    }

    @Test
    public void still_sees_exit_events_when_bluetooth_is_restarted_in_a_short_interval() {
        ScannerListener mockScannerListener = mock(ScannerListener.class);
        tested.addScannerListener(mockScannerListener);
        platform.fakeIBeaconSighting();

        verify(mockScannerListener).onScanEventDetected(isEntryEvent());

        tested.stop();
        reset(mockScannerListener);
        platform.clock.increaseTimeInMillis(RANDOM_VALUE_THAT_IS_SHORTER_THAN_CLEAN_BEACONMAP_ON_RESTART_TIMEOUT_BUT_LONGER_THAN_EXIT_EVENT_DELAY);
        tested.start();

        verify(mockScannerListener, never()).onScanEventDetected(isEntryEvent());
        verify(mockScannerListener, never()).onScanEventDetected(isExitEvent());

        long start = platform.clock.now();
        while (platform.clock.now() < start + Utils.EXIT_TIME) {
            platform.clock.increaseTimeInMillis(Utils.ONE_ADVERTISEMENT_INTERVAL);
        }
        verify(mockScannerListener, never()).onScanEventDetected(isExitEvent());

        platform.clock.increaseTimeInMillis(1);
        verify(mockScannerListener).onScanEventDetected(isExitEvent());

        verify(mockScannerListener, never()).onScanEventDetected(isEntryEvent());
    }


    public void beacon_events_are_removed_when_bluetooth_is_restarted_after_a_long_break_interval() {
        ScannerListener mockScannerListener = mock(ScannerListener.class);
        tested.addScannerListener(mockScannerListener);
        platform.fakeIBeaconSighting();

        verify(mockScannerListener).onScanEventDetected(isEntryEvent());

        tested.stop();
        reset(mockScannerListener);
        platform.clock.increaseTimeInMillis(Utils.VERY_LONG_TIME);
        tested.start();

        verify(mockScannerListener, never()).onScanEventDetected(isEntryEvent());
        verify(mockScannerListener, never()).onScanEventDetected(isExitEvent());

        long start = platform.clock.now();
        while (platform.clock.now() < start + Utils.EXIT_TIME * 2) {
            platform.clock.increaseTimeInMillis(Utils.ONE_ADVERTISEMENT_INTERVAL);
        }
        verify(mockScannerListener, never()).onScanEventDetected(isEntryEvent());
        verify(mockScannerListener, never()).onScanEventDetected(isExitEvent());

        while (platform.clock.now() < start + Utils.ONE_HOUR) {
            platform.clock.increaseTimeInMillis(Utils.ONE_ADVERTISEMENT_INTERVAL);
        }
        verify(mockScannerListener, never()).onScanEventDetected(isEntryEvent());
        verify(mockScannerListener, never()).onScanEventDetected(isExitEvent());

    }
}

