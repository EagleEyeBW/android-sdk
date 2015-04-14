package util;

import com.sensorberg.sdk.model.BeaconId;
import com.sensorberg.sdk.scanner.ScanEvent;
import com.sensorberg.sdk.scanner.ScanEventType;

import java.util.UUID;

public class TestConstants {


//    https://manage.sensorberg.com/#/beacon/edit/7e7c8ae1-e593-404e-a131-b4cbf149ed1d
    public static final BeaconId DELAY_BEACON_ID = new BeaconId(UUID.fromString("73676723-7400-0000-FFFF-0000FFFF0004"), 64202, 20003);
    public static final BeaconId LEET_BEACON_ID_1 = new BeaconId(UUID.fromString("73676723-7400-0000-FFFF-0000FFFF0001"), 1337, 1337);
    public static final BeaconId LEET_BEACON_ID_2 = new BeaconId(UUID.fromString("73676723-7400-0000-FFFF-0000FFFF0002"), 1337, 1337);
    public static final BeaconId LEET_BEACON_ID_3 = new BeaconId(UUID.fromString("73676723-7400-0000-FFFF-0000FFFF0003"), 1337, 1337);
    public static final BeaconId LEET_BEACON_ID_4 = new BeaconId(UUID.fromString("73676723-7400-0000-FFFF-0000FFFF0004"), 1337, 1337);
    public static final BeaconId LEET_BEACON_ID_5 = new BeaconId(UUID.fromString("73676723-7400-0000-FFFF-0000FFFF0005"), 1337, 1337);
    public static final BeaconId LEET_BEACON_ID_6 = new BeaconId(UUID.fromString("73676723-7400-0000-FFFF-0000FFFF0006"), 1337, 1337);
    public static final BeaconId LEET_BEACON_ID_7 = new BeaconId(UUID.fromString("73676723-7400-0000-FFFF-0000FFFF0007"), 1337, 1337);

    public static final BeaconId REGULAR_BEACON_ID = new BeaconId(UUID.fromString("192E463C-9B8E-4590-A23F-D32007299EF5"), 1337, 1337);


    public static final String API_TOKEN = "f257de3b91d141aa93b6a9b39c97b83df257de3b91d141aa93b6a9b39c97b83d";
    public static final String API_TOKEN_NO_SETTINGS = "9fa8eb37c8fe143cfee54def807bd778d05a8fa87c232d21aa9a45d4bd919181";


    public static final BeaconId IN_APP_BEACON_ID = new BeaconId(UUID.fromString("73676723-7400-0000-FFFF-0000FFFF0005"), 59364, 12297);


    public static final UUID BEACON_PROXIMITY_ID = UUID.fromString("192E463C-9B8E-4590-A23F-D32007299EF5");

    public static final String BEACON_ID_STRING = "192e463c9b8e4590a23fd32007299ef5";
    public static final int MAJOR = 1337;
    public static final int MINOR = 1337;

    public static final Long REVISION = 5L;

    public static final BeaconId ANY_BEACON_ID = new BeaconId(BEACON_PROXIMITY_ID, MAJOR, MINOR);
    public static final BeaconId ANY_OTHER_BEACON_ID = new BeaconId(BEACON_PROXIMITY_ID, MAJOR+1, MINOR+2);


    public static BeaconId randomBeaconId(){
        return new BeaconId(UUID.randomUUID(), 1, 1);
    }

    public static ScanEvent REGULAR_BEACON_SCAN_EVENT(long now) {
        return new ScanEvent.Builder()
                .withBeaconId(TestConstants.REGULAR_BEACON_ID)
                .withEventMask(ScanEventType.ENTRY.getMask())
                .withEventTime(now)
                .build();
    }
}
