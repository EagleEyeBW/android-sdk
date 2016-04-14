package com.sensorberg.sdk.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.test.AndroidTestCase;

import com.sensorberg.sdk.SensorbergApplicationTest;

import com.sensorberg.sdk.action.Action;
import com.sensorberg.sdk.presenter.LocalBroadcastManager;
import com.sensorberg.sdk.presenter.ManifestParser;
import com.sensorberg.sdk.testUtils.TestPlatform;

import org.fest.assertions.api.Assertions;

import java.util.List;

import static org.mockito.Matchers.anyByte;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TheAndroidPlatformShould extends SensorbergApplicationTest {

    PermissionChecker tested;

    public void test_should_cache_the_permissions(){
        Context mockContext = mock(Context.class);

        when(mockContext.checkCallingOrSelfPermission(anyString())).thenReturn(PackageManager.PERMISSION_GRANTED);
        PackageManager mockPackageManager = mock(PackageManager.class);
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);

        tested = new PermissionChecker(mockContext);

        tested.hasVibratePermission();
        tested.hasVibratePermission();

        verify(mockContext, times(1)).checkCallingOrSelfPermission(anyString());

    }

    @Override
    public void tearDown() throws Exception {
        TestGenericBroadcastReceiver2.reset();
        super.tearDown();
    }

    public void test_should_return_the_sync_setting(){
        AndroidPlatform platform = new AndroidPlatform(getContext());
        Assertions.assertThat(platform.isSyncEnabled()).isTrue();
    }

    public void test_should_not_registerBroadcastReceiver_twice(){
        TestGenericBroadcastReceiver2.reset(1);
        AndroidPlatform androidPlatform = new AndroidPlatform(getContext());

        androidPlatform.registerBroadcastReceiver();
        androidPlatform.registerBroadcastReceiver();
        androidPlatform.registerBroadcastReceiver();

        Intent broadcastIntent = new Intent(ManifestParser.actionString);
        broadcastIntent.putExtra(Action.INTENT_KEY, "foo");
        LocalBroadcastManager.getInstance(getContext()).sendBroadcastSync(broadcastIntent);

        Assertions.assertThat(TestGenericBroadcastReceiver2.intentList).hasSize(1);
    }

    public void test_should_find_the_TestGenericBroadcastReceiver(){
        List<BroadcastReceiver> list = ManifestParser.findBroadcastReceiver(getContext());

        Assertions.assertThat(list).hasSize(2);
    }
}
