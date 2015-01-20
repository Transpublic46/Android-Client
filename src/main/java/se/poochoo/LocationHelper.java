package se.poochoo;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

import se.poochoo.proto.Messages.SelectionDeviceData.Position;
import se.poochoo.proto.Messages.SelectionDeviceData;

/**
 * Created by Erik on 2013-12-13.
 */
public class LocationHelper implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {
    public static interface LocationCallBack {
        public void locationInitialized();
    }
    private static final int MIN_TIME_BETWEEN_STORE_POSITION = 100000;
    private static Position currentPosition = null;
    private static Position previousPosition = null;
    private static long lastSuccessFulTime = 0;

    public static LocationHelper create(Context context, LocationCallBack callBack) {
        return createAndConnectClient(false, context, callBack);
    }

    public static LocationHelper createForSingleUpdate(Context context) {
        return createAndConnectClient(true, context, null);
    }

    public void addLocationsToRequest(SelectionDeviceData.Builder requestBuilder) {
        if (locationClient.isConnected()) {
            getPositionFromLocationClient();
        }
        addLocationToRequestStatic(requestBuilder);
    }

    public static void addLocationToRequestStatic(SelectionDeviceData.Builder requestBuilder) {
        if (hasFreshLocation()) {
            requestBuilder.setPosition(currentPosition);
            if (previousPosition != null) {
                requestBuilder.addPreviousPosition(previousPosition);
            }
        }
    }

    public static boolean hasFreshLocation() {
        return currentPosition != null
                && (System.currentTimeMillis() - lastSuccessFulTime) < MIN_TIME_BETWEEN_STORE_POSITION;
    }

    public boolean isConnected() {
        return locationClient != null && locationClient.isConnected();
    }

    private static LocationHelper createAndConnectClient(
            boolean requestSingleUpdate, Context context, LocationCallBack callBack) {
        LocationHelper helper = new LocationHelper(requestSingleUpdate);
        helper.setLocationCallBack(callBack);
        LocationClient client = new LocationClient(context, helper, helper);
        helper.locationClient = client;
        client.connect();
        return helper;
    }

    private static Position toPosition(Location location) {
        return Position.newBuilder()
                .setLat(location.getLatitude())
                .setLng(location.getLongitude())
                .setAccuracy((int) location.getAccuracy())
                .setTime(location.getTime())
                .build();
    }

    private void getPositionFromLocationClient() {
        Location location = locationClient.isConnected() ?  locationClient.getLastLocation() : null;
        feedLocation(location);
    }

    static void feedLocation(Location location) {
        if (location != null) {
            Position newPosition = toPosition(location);
            if (currentPosition != null
                    && currentPosition.getLat() != newPosition.getLat()
                    && currentPosition.getLng() != newPosition.getLng()) {
                previousPosition = currentPosition;
            }
            currentPosition = newPosition;
            lastSuccessFulTime = System.currentTimeMillis();
        }
    }

    private final boolean requestSingleUpdate;
    private LocationCallBack locationCallBack;
    private LocationClient locationClient;

    private LocationHelper(boolean requestSingleUpdate) {
        this.requestSingleUpdate = requestSingleUpdate;
    }

    public void setLocationCallBack(LocationCallBack locationCallBack) {
        this.locationCallBack = locationCallBack;
    }
    @Override
    public void onConnected(Bundle bundle) {
        feedLocation(locationClient.getLastLocation());
        if (requestSingleUpdate) {
            locationClient.disconnect();
        }
        if (locationCallBack != null) {
            locationCallBack.locationInitialized();
        }
    }

    public void disconnect() {
        if (locationClient != null && locationClient.isConnected()) {
            locationClient.disconnect();
        }
    }

    public void connect() {
        if (locationClient != null && !locationClient.isConnected()) {
            locationClient.connect();
        }
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (locationCallBack != null) {
            locationCallBack.locationInitialized();
        }
    }
}
