package com.sensorberg.sdk.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Message;
import android.util.Pair;

import com.sensorberg.sdk.Constants;
import com.sensorberg.sdk.Logger;
import com.sensorberg.sdk.model.BeaconId;
import com.sensorberg.sdk.internal.Platform;
import com.sensorberg.sdk.internal.RunLoop;
import com.sensorberg.sdk.settings.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

public abstract class AbstractScanner implements RunLoop.MessageHandlerCallback, Platform.ForegroundStateListener {

    protected final Platform platform;

    public long waitTime = Settings.DEFAULT_BACKGROUND_WAIT_TIME;
    public long scanTime = Settings.DEFAULT_BACKGROUND_SCAN_TIME;
   
    protected Settings settings;


    final ScanCallback scanCallback = new ScanCallback();
    final Object listenersMonitor = new Object();
    final List<ScannerListener> listeners = new ArrayList<ScannerListener>();

    private final Object enteredBeaconsMonitor = new Object();
    private final BeaconMap enteredBeacons;
    protected final RunLoop runLoop;
    private long lastStopTimestamp = 0L; // this.platform.getClock().now(); // or 0L

    private long started;
    private boolean scanning;
    private long lastExitCheckTimestamp;
    private long lastBreakLength = 0;
    private long lastScanStart;

    private RssiListener rssiListener = RssiListener.NONE;

    public AbstractScanner(Settings settings, Platform platform, boolean shouldRestoreBeaconStates) {
        this.platform = platform;
        this.settings = settings;
        scanning = false;
        runLoop = platform.getScannerRunLoop(this);

        File beaconFile = platform.getFile("enteredBeaconsCache");
        enteredBeacons = new BeaconMap(shouldRestoreBeaconStates ? beaconFile : null);
    }


    /**
     * Adds a {@link ScannerListener} to the {@link List} of {@link ScannerListener}s.
     *
     * @param listener the {@link ScannerListener} to be added
     */
    public void addScannerListener(ScannerListener listener) {
        synchronized (listenersMonitor) {
            listeners.add(listener);
        }
    }

    void checkAndExitEnteredBeacons() {
        final long now = platform.getClock().now();
        lastExitCheckTimestamp = now;
        synchronized (enteredBeaconsMonitor) {
            if (enteredBeacons.size() > 0) {
                enteredBeacons.filter(new BeaconMap.Filter() {
                    public boolean filter(EventEntry beaconEntry, BeaconId beaconId) {
                        //might be negative!!!
                        long timeSinceWeSawTheBeacon = now - lastBreakLength - beaconEntry.lastBeaconTime;
                        if (timeSinceWeSawTheBeacon > settings.getExitTimeout()) {
                            ScanEvent scanEvent = new ScanEvent(beaconId, now, ScanEventType.EXIT.getMask());
                            runLoop.sendMessage(ScannerEvent.EVENT_DETECTED, scanEvent);
                            Logger.log.beaconResolveState(scanEvent, " exited (time since we saw the beacon: " + (int) (timeSinceWeSawTheBeacon / 1000) + " seconds)");
                            return true;
                        }
                        return false;
                    }
                });
            }
        }
    }

    /**
     * Clears the {@link ScanEvent} cache.
     */
    public void clearCache() {
        synchronized (enteredBeaconsMonitor) {
            enteredBeacons.clear();
        }
    }

    /**
     * Returns a flag indicating whether the {@link Scanner} is currently running.
     *
     * @return a flag indicating whether the {@link Scanner} is currently running
     */
    public boolean isScanRunning() {
        return scanning;
    }

    void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Pair<BeaconId,Integer> beacon = ScanHelper.getBeaconID(scanRecord);
        if (beacon != null) {
            BeaconId beaconId = beacon.first;
            synchronized (enteredBeaconsMonitor) {
                long now = platform.getClock().now();
                EventEntry entry = enteredBeacons.get(beaconId);
                if (entry == null) {
                    int calRssi = beacon.second;
                    String address = device != null ? device.getAddress() : null;
                    ScanEvent scanEvent = new ScanEvent(beaconId, now, ScanEventType.ENTRY.getMask(), address, rssi, calRssi);
                    runLoop.sendMessage(ScannerEvent.EVENT_DETECTED, scanEvent);
                    entry = new EventEntry(now, ScanEventType.ENTRY.getMask());
                    Logger.log.beaconResolveState(scanEvent, "entered");
                } else {
                    entry = new EventEntry(entry);
                    entry.lastBeaconTime = now;
                    Logger.log.beaconSeenAgain(beaconId);
                    if (this.rssiListener != RssiListener.NONE) {
                        runLoop.sendMessage(ScannerEvent.RSSI_UPDATED, new Pair<>(beaconId, rssi));
                    }
                }
                enteredBeacons.put(beaconId, entry);
            }
        }
    }

    @Override
    public void handleMessage(Message message) {
        ScannerEvent queueEvent = new ScannerEvent(message.what, message.obj);
        switch (queueEvent.type) {
            case ScannerEvent.LOGICAL_SCAN_START_REQUESTED: {
                if (!scanning) {
                    lastExitCheckTimestamp = platform.getClock().now();
                    if (lastExitCheckTimestamp - lastStopTimestamp > settings.getCleanBeaconMapRestartTimeout()) {
                        clearCache();
                    }
                    started = platform.getClock().now();
                    scanning = true;
                    runLoop.sendMessage(ScannerEvent.UN_PAUSE_SCAN);
                }
                break;
            }
            case ScannerEvent.PAUSE_SCAN: {
                platform.stopLeScan(scanCallback);
                Logger.log.scannerStateChange("sleeping for" + waitTime + "millis");
                scheduleExecution(ScannerEvent.UN_PAUSE_SCAN, waitTime);
                runLoop.cancelFixedRateExecution();
                break;
            }
            case ScannerEvent.UN_PAUSE_SCAN: {
                long now = platform.getClock().now();
                lastScanStart = now;
                lastBreakLength = platform.getClock().now() - lastExitCheckTimestamp;
                Logger.log.scannerStateChange("starting to scan again, scan break was " + lastBreakLength + "millis");
                if (scanning) {
                    Logger.log.scannerStateChange("scanning for" + scanTime + "millis");
                    platform.startLeScan(scanCallback);
                    scheduleExecution(ScannerEvent.PAUSE_SCAN, scanTime);

                    runLoop.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            loop();
                        }
                    }, 0, Constants.Time.ONE_SECOND);
                }
                break;
            }
            case ScannerEvent.SCAN_STOP_REQUESTED: {
                started = 0;
                scanning = false;
                clearScheduledExecutions();
                platform.stopLeScan(scanCallback);
                lastStopTimestamp = platform.getClock().now();
                runLoop.cancelFixedRateExecution();
                Logger.log.scannerStateChange("scan stopped");
                break;
            }
            case ScannerEvent.EVENT_DETECTED: {
                ScanEvent scanEvent = (ScanEvent) queueEvent.data;
                synchronized (listenersMonitor) {
                    for (ScannerListener listener : listeners) {
                        listener.onScanEventDetected(scanEvent);
                    }
                }
                break;
            }
            case ScannerEvent.RSSI_UPDATED: {
                Pair<BeaconId, Integer> value = (Pair<BeaconId, Integer>) queueEvent.data;
                this.rssiListener.onRssiUpdated(value.first, value.second);
                break;

            }
            default: {
                throw new IllegalArgumentException("unhandled case "+ queueEvent.type);
            }
        }
    }

    protected abstract void clearScheduledExecutions();

    public void loop() {
        if (platform.getClock().now() > (started + settings.getExitTimeout())) {
            if (platform.isLeScanRunning()) {
                checkAndExitEnteredBeacons();
            }
        }
    }

    /**
     * Removes a {@link ScannerListener} from the {@link List} of {@link ScannerListener}s.
     *
     * @param listener the {@link ScannerListener} to be removed
     */
    public void removeScannerListener(ScannerListener listener) {
        synchronized (listenersMonitor) {
            listeners.remove(listener);
        }
    }

    public RssiListener getRssiListener() {
        return rssiListener;
    }

    public void setRssiListener(RssiListener rssiListener) {
        this.rssiListener = rssiListener;
    }

    /**
     * Starts scanning.
     */
    public void start() {
        runLoop.sendMessage(ScannerEvent.LOGICAL_SCAN_START_REQUESTED);
    }


    /**
     * Stop the scanning.
     */
    public void stop() {
        runLoop.sendMessage(ScannerEvent.SCAN_STOP_REQUESTED);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    class ScanCallback implements BluetoothAdapter.LeScanCallback {
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            AbstractScanner.this.onLeScan(device, rssi, scanRecord);
        }
    }


    @Override
    public void hostApplicationInForeground() {
        if (isNotSetupForForegroundScanning()) {
            waitTime = settings.getForeGroundWaitTime();
            scanTime = settings.getForeGroundScanTime();
            if (scanning) {
                long lastWaitTime = platform.getClock().now() - lastExitCheckTimestamp;
                clearScheduledExecutions();
                if (lastWaitTime > waitTime) {
                    Logger.log.scannerStateChange("We have been waiting longer than the foreground wait time, so we´e going to scan right away");
                    runLoop.sendMessage(ScannerEvent.UN_PAUSE_SCAN);
                } else {
                    long timeRemainingToWait = waitTime - lastWaitTime;
                    Logger.log.scannerStateChange("We have been waiting longer than the foreground wait time, so we´e going to scan in " + timeRemainingToWait + " millis");
                    scheduleExecution(ScannerEvent.UN_PAUSE_SCAN, waitTime - lastWaitTime);
                }
            }
        }
    }

    abstract void scheduleExecution(int type, long delay);

    private boolean isNotSetupForForegroundScanning() {
        return waitTime != settings.getForeGroundWaitTime() || scanTime != settings.getForeGroundScanTime();
    }

    @Override
    public void hostApplicationInBackground() {
        waitTime = settings.getBackgroundWaitTime();
        scanTime = settings.getBackgroundScanTime();
        if ((platform.getClock().now() - lastScanStart) > scanTime){
            Logger.log.scannerStateChange("We have been scanning longer than the background scan, so we´e going to pause right away");
            clearScheduledExecutions();
            runLoop.sendMessage(ScannerEvent.PAUSE_SCAN);
        }
    }

    public interface RssiListener {
        RssiListener NONE = new RssiListener() {
            @Override
            public void onRssiUpdated(BeaconId first, Integer second) {

            }
        };

        void onRssiUpdated(BeaconId first, Integer second);
    }
}
