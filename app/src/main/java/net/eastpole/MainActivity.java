/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.eastpole;

import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;

/**
 * Getting Location Updates.
 *
 * Demonstrates how to use the Fused Location Provider API to get updates about a device's
 * location. The Fused Location Provider is part of the Google Play services location APIs.
 *
 * For a simpler example that shows the use of Google Play services to fetch the last known location
 * of a device, see
 * https://github.com/googlesamples/android-play-location/tree/master/BasicLocation.
 *
 * This sample uses Google Play services, but it does not require authentication. For a sample that
 * uses Google Play services for authentication, see
 * https://github.com/googlesamples/android-google-accounts/tree/master/QuickStart.
 */
public class MainActivity extends AppCompatActivity implements
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    protected static final String TAG = "location-updates-sample";

     // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    // The fastest rate for active location updates. Exact. Updates will never be more frequent than this value.
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 2000;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    // This will become a real user in later revisions
    protected String mUserId = "GM";

    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;

    // Special alias to the host loopback interface (i.e., 127.0.0.1 on the development machine):
    protected static final String UDP_HOST_NAME = "10.0.2.2";
    protected static final int mPort = 11000;   // Arbitrary port

    // UI Widgets: Button-s.
    protected Button mStartUpdatesButton;
    protected Button mStopUpdatesButton;

    // UI Widgets: TextView-s
    protected enum WigetK { LONGITUDE, LATITUDE, ACCURACY, ALTITUDE, SPEED, BEARING, TIME, DELTA0, DELTA1, DELTA2, DELTA3, DELTA4 };
    //
    private interface GetLocationProp { public String get(MainActivity me); }   // Until we can use real lambdas!
    private static class GetDeltaTimeProp implements GetLocationProp {
        GetDeltaTimeProp(int mIdx) { this.mIdx = mIdx; }
        public String get(MainActivity me) {
            return me.mUpdateTimeDeltas[mIdx] >= 0 ? String.format("%.3f s", me.mUpdateTimeDeltas[mIdx]/1000.0) : "";
        }
        private int mIdx;
    }
    private static class ResIdSpec {
        ResIdSpec(WigetK mWigetK, int mLabelId, int mViewId, GetLocationProp mGetLocationProp)
        {
            this.mWigetK            = mWigetK;
            this.mLabelId           = mLabelId;
            this.mViewId            = mViewId;
            this.mGetLocationProp   = mGetLocationProp;
        }
        public final WigetK mWigetK;
        public final int mViewId;
        public final int mLabelId;
        public final GetLocationProp mGetLocationProp;
    }
    private static final ResIdSpec mResIdSpecs[] = {
            new ResIdSpec(WigetK.LATITUDE,   R.string.latitude_label,    R.id.latitude_text
                    , new GetLocationProp() { public String get(MainActivity me) { return String.valueOf(me.mCurrentLocation.getLatitude()); } } ),
            new ResIdSpec(WigetK.LONGITUDE,  R.string.longitude_label,   R.id.longitude_text
                    , new GetLocationProp() { public String get(MainActivity me) { return String.valueOf(me.mCurrentLocation.getLongitude()); } } ),
            new ResIdSpec(WigetK.ACCURACY,   R.string.accuracy_label,    R.id.accuracy_text
                    , new GetLocationProp() { public String get(MainActivity me) { return me.mCurrentLocation.hasAccuracy() ? String.valueOf(me.mCurrentLocation.getAccuracy()) : ""; } } ),
            new ResIdSpec(WigetK.ALTITUDE,   R.string.altitude_label,    R.id.altitude_text
                    , new GetLocationProp() { public String get(MainActivity me) { return me.mCurrentLocation.hasAltitude() ? String.valueOf(me.mCurrentLocation.getAltitude()) : ""; } } ),
            new ResIdSpec(WigetK.SPEED,      R.string.speed_label,       R.id.speed_text
                    , new GetLocationProp() { public String get(MainActivity me) { return me.mCurrentLocation.hasSpeed() ? String.valueOf(me.mCurrentLocation.getSpeed()) : ""; } } ),
            new ResIdSpec(WigetK.BEARING,    R.string.bearing_label,     R.id.bearing_text
                    , new GetLocationProp() { public String get(MainActivity me) { return me.mCurrentLocation.hasBearing() ? String.valueOf(me.mCurrentLocation.getBearing()) : ""; } } ),

            new ResIdSpec(WigetK.TIME,       R.string.last_update_time_label,    R.id.last_update_time_text
                    , new GetLocationProp() { public String get(MainActivity me) { return String.valueOf(DateFormat.getTimeInstance().format(new Date(me.mLastUpdateTime))); } } ),
            new ResIdSpec(WigetK.DELTA0,     R.string.prev_update_time_label,    R.id.last_delta_time0_text, new GetDeltaTimeProp(0) ),
            new ResIdSpec(WigetK.DELTA1,     R.string.prev_update_time_label,    R.id.last_delta_time1_text, new GetDeltaTimeProp(1) ),
            new ResIdSpec(WigetK.DELTA2,     R.string.prev_update_time_label,    R.id.last_delta_time2_text, new GetDeltaTimeProp(2) ),
            new ResIdSpec(WigetK.DELTA3,     R.string.prev_update_time_label,    R.id.last_delta_time3_text, new GetDeltaTimeProp(3) ),
            new ResIdSpec(WigetK.DELTA4,     R.string.prev_update_time_label,    R.id.last_delta_time4_text, new GetDeltaTimeProp(4) ),
    };
    protected static class WidgetD {
        WidgetD(String mLabelText, TextView mTextView, GetLocationProp mGetLocationProp) {
            this.mLabelText         = mLabelText;
            this.mTextView          = mTextView;
            this.mGetLocationProp   = mGetLocationProp;
        }
        public final String mLabelText;
        public final TextView mTextView;
        public final GetLocationProp mGetLocationProp;
    }
    protected EnumMap<WigetK, WidgetD> mWidgetMap;

    protected Boolean mRequestingLocationUpdates;
    protected Long mLastUpdateTime;
    protected Long mUpdateTimeDeltas[] = new Long[5];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Without this, any attempt to use sockets in the Main Thread will throw!
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Locate the UI widgets.
        mStartUpdatesButton = (Button) findViewById(R.id.start_updates_button);
        mStopUpdatesButton = (Button) findViewById(R.id.stop_updates_button);

        mWidgetMap = new EnumMap<WigetK, WidgetD>(WigetK.class);
        for (ResIdSpec resIdSpec : mResIdSpecs) {
            mWidgetMap.put(resIdSpec.mWigetK, new WidgetD(
                    getResources().getString(resIdSpec.mLabelId),
                    (TextView) findViewById(resIdSpec.mViewId),
                    resIdSpec.mGetLocationProp));
        }

        mRequestingLocationUpdates = false;
        mLastUpdateTime = 0L;
        for (int ii = 0; ii < mUpdateTimeDeltas.length; ++ii) {
            mUpdateTimeDeltas[ii] = -1L;
        }

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        // Kick off the process of building a GoogleApiClient and requesting the LocationServices API.
        buildGoogleApiClient();
    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getLong(LAST_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }

    private void addNextDelta(Long now, Long before) {
        if (now <= 0 || before <= 0) {
            for (int ii = 0; ii < mUpdateTimeDeltas.length; ++ii)
                mUpdateTimeDeltas[ii] = -1L;
            return;
        }
        for (int ii = mUpdateTimeDeltas.length - 2; ii >= 0; --ii) {
            mUpdateTimeDeltas[ii + 1] = mUpdateTimeDeltas[ii];
        }
        mUpdateTimeDeltas[0] = now - before;
    }
    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    public void startUpdatesButtonHandler(View view) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            setButtonsEnabledState();
            startLocationUpdates();
        }
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates. Does nothing if
     * updates were not previously requested.
     */
    public void stopUpdatesButtonHandler(View view) {
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            setButtonsEnabledState();
            stopLocationUpdates();
        }
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Ensures that only one button is enabled at any time. The Start Updates button is enabled
     * if the user is not requesting location updates. The Stop Updates button is enabled if the
     * user is requesting location updates.
     */
    private void setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStartUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setEnabled(false);
        }
    }

    /**
     * Updates the latitude, the longitude, and the last location time in the UI.
     */
    private void updateUI() {
        for (EnumMap.Entry<WigetK, WidgetD> entry : mWidgetMap.entrySet()) {
            WidgetD wd = entry.getValue();
            if (wd.mGetLocationProp == null)
                continue;

            wd.mTextView.setText(String.format("%s: %s", wd.mLabelText, wd.mGetLocationProp.get(this)));
        }
    }

    private void sendUpdate2Server() {
        try {
            InetAddress addr = InetAddress.getByName(UDP_HOST_NAME);
            DatagramSocket sock = new DatagramSocket();
            String msg = String.format("[\"%s\",%d,%s,%s]", mUserId, mLastUpdateTime/1000,
                    mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(), addr, mPort);
            sock.send(packet);
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.bad_host, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();

        super.onStop();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mCurrentLocation != null) {
                Long prevUpdateTime = mLastUpdateTime;
                mLastUpdateTime = System.currentTimeMillis();
                addNextDelta(mLastUpdateTime, prevUpdateTime);
                updateUI();
                sendUpdate2Server();
            }
        }

        // If the user presses the Start Updates button before GoogleApiClient connects, we set
        // mRequestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
        // the value of mRequestingLocationUpdates and if it is true, we start location updates.
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        Long prevUpdateTime = mLastUpdateTime;

        mCurrentLocation = location;
        mLastUpdateTime = System.currentTimeMillis();
        addNextDelta(mLastUpdateTime, prevUpdateTime);
        updateUI();
        Toast.makeText(this, getResources().getString(R.string.location_updated_message),
                Toast.LENGTH_SHORT).show();
        sendUpdate2Server();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putLong(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }
}
