package com.sensorberg.sdk;

import android.content.IntentFilter;

import com.sensorberg.sdk.action.ActionFactory;
import com.sensorberg.sdk.internal.OkHttpClientTransport;
import com.sensorberg.sdk.internal.TestGenericBroadcastReceiver;
import com.sensorberg.sdk.internal.transport.HeadersJsonObjectRequest;
import com.sensorberg.sdk.model.server.ResolveAction;
import com.sensorberg.sdk.model.server.ResolveResponse;
import com.sensorberg.sdk.presenter.LocalBroadcastManager;
import com.sensorberg.sdk.presenter.ManifestParser;
import com.sensorberg.sdk.scanner.ScanEvent;
import com.sensorberg.sdk.scanner.ScanEventType;
import com.sensorberg.sdk.testUtils.TestPlatform;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.fest.assertions.api.Assertions;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import util.TestConstants;

public class TheInternalBootstrapperIntegration extends SensorbergApplicationTest {

    InternalApplicationBootstrapper tested;

    private static final JSONObject ANY_IN_APP_JSON = new JSONObject();
    static {
        try {
            ANY_IN_APP_JSON.put("url", "sensorberg://");
        } catch (JSONException e) {
        }
    }

    private static final String ANY_UUID = UUID.randomUUID().toString();
    private static final String ANOTHER_UUID = UUID.randomUUID().toString();

    private static final ResolveResponse PUBLISH_HISTORY_RESPONSE = new ResolveResponse.Builder()
            .withInstantActions(Arrays.asList(
                    new ResolveAction.Builder()
                            .withBeacons(Arrays.asList(TestConstants.ANY_BEACON_ID.getBid()))
                            .withType(ActionFactory.SERVER_TYPE_IN_APP)
                            .withUuid(ANY_UUID)
                            .withContent(ANY_IN_APP_JSON)
                            .build()
            ))
            .build();

    private ResolveResponse RESOLVE_RESPONSE_WITH_REPORT_IMMEDIATELY = new ResolveResponse.Builder()
            .withActions(Arrays.asList(
                    new ResolveAction.Builder()
                            .withBeacons(Arrays.asList(TestConstants.ANY_BEACON_ID.getBid()))
                            .withTrigger(ScanEventType.ENTRY.getMask())
                            .withUuid(ANOTHER_UUID)
                            .withReportImmediately(true)
                            .build()
            ))
            .build();

    private ResolveResponse RESOLVE_RESPONSE_WITH_ACTION = new ResolveResponse.Builder()
            .withActions(Arrays.asList(
                    new ResolveAction.Builder()
                            .withBeacons(Arrays.asList(TestConstants.ANY_BEACON_ID.getBid()))
                            .withType(ActionFactory.SERVER_TYPE_IN_APP)
                            .withTrigger(ScanEventType.ENTRY.getMask())
                            .withUuid(ANY_UUID)
                            .withContent(ANY_IN_APP_JSON)
                            .build()
            ))
            .build();
    private TestGenericBroadcastReceiver broadcastReceiver;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestPlatform platform = new TestPlatform().setContext(getContext());
        platform.setTransport(new OkHttpClientTransport(platform, null));
        tested = new InternalApplicationBootstrapper(platform);

        broadcastReceiver = new TestGenericBroadcastReceiver();

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(broadcastReceiver, new IntentFilter(ManifestParser.actionString));

        startWebserver();

        TestGenericBroadcastReceiver.reset();

    }

    public void test_the_gson_serialization() throws Exception {
        String json = HeadersJsonObjectRequest.gson.toJson(PUBLISH_HISTORY_RESPONSE);
        ResolveResponse response = HeadersJsonObjectRequest.gson.fromJson(json, ResolveResponse.class);
        Assertions.assertThat(json).isNotEmpty();
        Assertions.assertThat(response).isNotNull().isEqualsToByComparingFields(PUBLISH_HISTORY_RESPONSE);
    }

    public void test_caching() throws Exception {
        //we´e waiting for 2 requests
        TestGenericBroadcastReceiver.reset(2);

        //enqueue an event that can be cached
        server.enqueue(new MockResponse()
                        .setBody(HeadersJsonObjectRequest.gson.toJson(RESOLVE_RESPONSE_WITH_ACTION))
                        .setHeader("Cache-Control", new CacheControl.Builder()
                                .maxAge(24, TimeUnit.HOURS)
                                .noTransform()
                                .build())
                        .setHeader("Etag", ANY_UUID)
        );

        //simulate the entry
        tested.onScanEventDetected(new ScanEvent.Builder()
                .withBeaconId(TestConstants.ANY_BEACON_ID)
                .withEventMask(ScanEventType.ENTRY.getMask())
                .build());

        //another entry
        tested.onScanEventDetected(new ScanEvent.Builder()
                .withBeaconId(TestConstants.ANY_BEACON_ID)
                .withEventMask(ScanEventType.ENTRY.getMask())
                .build());

        //verify only one request to the actual server
        waitForRequests(1);

        //but two notifications displayed
        Assertions.assertThat(TestGenericBroadcastReceiver.getLatch().await(1, TimeUnit.SECONDS)).isTrue();

    }

    public void test_an_instant_action_workflow() throws Exception {
        //enqueue the layout with a beacon for report immediately
        server.enqueue(new MockResponse()
                        .setBody(HeadersJsonObjectRequest.gson.toJson(RESOLVE_RESPONSE_WITH_REPORT_IMMEDIATELY))
        );

        //enqueue the reporting result
        server.enqueue(new MockResponse()
                .setBody(HeadersJsonObjectRequest.gson.toJson(PUBLISH_HISTORY_RESPONSE)));

        //simulate the entry
        tested.onScanEventDetected(new ScanEvent.Builder()
                .withBeaconId(TestConstants.ANY_BEACON_ID)
                .withEventMask(ScanEventType.ENTRY.getMask())
                .build());

        waitForRequests(2);

        //we should have exactly one notification
        Assertions.assertThat(TestGenericBroadcastReceiver.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    }

    public void test_precaching() throws IOException, JSONException, InterruptedException {
        enqueue(com.sensorberg.sdk.test.R.raw.response_resolve_precaching);

        tested.updateBeaconLayout();

        waitForRequests(1);

        //simulate the entry
        tested.onScanEventDetected(new ScanEvent.Builder()
                        .withBeaconId(TestConstants.ANY_BEACON_ID)
                        .withEventMask(ScanEventType.ENTRY.getMask())
                        .build()
        );

        //we should have exactly one notification
        Assertions.assertThat(TestGenericBroadcastReceiver.getLatch().await(10, TimeUnit.SECONDS)).isTrue();

        Assertions.assertThat(server.getRequestCount()).isEqualTo(1);
    }

    public void test_if_the_precaching_always_fetches_from_network() throws IOException, JSONException, InterruptedException {
        enqueue(com.sensorberg.sdk.test.R.raw.response_resolve_precaching);
        enqueue(com.sensorberg.sdk.test.R.raw.response_resolve_precaching);

        tested.updateBeaconLayout();
        tested.updateBeaconLayout();

        RecordedRequest secondRequest = waitForRequests(2).get(1);
        Assertions.assertThat(secondRequest.getHeader("If-None-Match")).isNotNull().isNotEmpty();
    }

    public void test_precaching_of_account_proximityUUIDS() throws IOException, JSONException, InterruptedException {
        enqueue(com.sensorberg.sdk.test.R.raw.response_resolve_precaching);
        Assertions.assertThat(tested.proximityUUIDs).hasSize(0);

        tested.updateBeaconLayout();

        waitForRequests(1);

        Assertions.assertThat(tested.proximityUUIDs).hasSize(5);
    }
}
