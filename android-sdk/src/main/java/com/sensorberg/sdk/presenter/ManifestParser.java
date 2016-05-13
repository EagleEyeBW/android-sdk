package com.sensorberg.sdk.presenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.sensorberg.sdk.Logger;

import java.util.ArrayList;
import java.util.List;

public class ManifestParser {

    public static final String actionString = "com.sensorberg.android.PRESENT_ACTION";
    private static final Intent actionIntent = new Intent();
    static {
        actionIntent.setAction(actionString);
    }

    @SuppressWarnings("EmptyCatchBlock")
    public static List<BroadcastReceiver> findBroadcastReceiver(Context context) {
        List<BroadcastReceiver> result = new ArrayList<>();

        List<ResolveInfo> infos = context.getPackageManager().queryBroadcastReceivers(actionIntent, PackageManager.SIGNATURE_MATCH);
        for (ResolveInfo resolveInfo : infos) {

            try {
                if (!resolveInfo.activityInfo.processName.endsWith(".sensorberg")){
                    continue;
                }
                BroadcastReceiver broadcastReceiver = (BroadcastReceiver) Class.forName(resolveInfo.activityInfo.name).newInstance();
                result.add(broadcastReceiver);
            } catch (Exception e) {
                Logger.log.logError("could not find any broadcastreceiver", e);
            }
        }
        return result;
    }
}
