package com.sensorberg.sdk.internal.transport.interfaces;

import com.sensorberg.sdk.internal.transport.model.HistoryBody;
import com.sensorberg.sdk.internal.transport.model.SettingsResponse;
import com.sensorberg.sdk.model.server.BaseResolveResponse;
import com.sensorberg.sdk.model.server.ResolveResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface RetrofitApiService {

    @GET
    Call<BaseResolveResponse> updateBeaconLayout(@Url String beaconLayoutUrl);

    @GET
    Call<ResolveResponse> getBeacon(@Url String beaconURLString, @Header("X-pid") String beaconId, @Header("X-qos") String networkInfo);

    @POST
    Call<ResolveResponse> publishHistory(@Url String beaconLayoutUrl, @Body HistoryBody body);

    @GET
    Call<SettingsResponse> getSettings(@Url String settingsUrl);
}
