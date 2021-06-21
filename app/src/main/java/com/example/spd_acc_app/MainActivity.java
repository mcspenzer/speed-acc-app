package com.example.spd_acc_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.spd_acc_app.geofence.GeofenceHelper;
import com.example.spd_acc_app.notification.NotificationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity
 *
 * Type: Activity
 * Superclass: AppCompatActivity
 * Interfaces: View.OnclickListener
 *
 * Properties:
 *  !> TextViews
 *      !> mConstraintLayout                    - Root Layout
 *      !> mTxtSpeed                            - Speed Display
 *      !> mTxtLong                             - Longitude Display
 *      !> mTxtLat                              - Latitude Display
 *      !> mTxtAcceleration                     - Acceleration Display
 *      !> mTxtLastGeofence                     - Last Geofence Display
 *      !> mTxtIsInGeofence                     - Is In Geofence Display
 *      !> mTxtSpdStatus                        - Speed Status Display
 *      !> mTxtTurnStatus                       - Turn Status Display (Currently unused; merged to Notification)
 *      !> mTxtTitleLat                         - Latitude Title Display
 *      !> mTxtTitleLon                         - Longitude Title Display
 *      !> mTxtTitleLastLoc                     - Last Location Title Display
 *      !> mTxtTitleInsideLoc                   - Inside Location Title Display
 *      !> mTxtDirection                        - Direction Display
 *      !> mTxtTitleDirection                   - Direction Title Display
 *
 *  !> mBtnStartMonitoring                      - Start/Stop Button
 *
 *  !> Location Objects/Primitives
 *      !> mLocationRequest                     - Location Request Object
 *      !> mFusedLocationClient                 - Fused Location Object
 *      !> mLocationCallback                    - Location Callback Object
 *      !> mPreviousLocation                    - Previous Location Object for comparison of speed (acceleration) and determination of trajectory (northbound/southbound)
 *
 *  !> Geofencing Objects/Primitives
 *      !> mGeofencingClient                    - Main Geofencing Client
 *      !> mGeofenceHelper                      - GeofenceHelper reference object
 *      !> mTrajectory                          - Current Trajectory
 *      !> mIsGefencePushNeeded                 - Determines if Geofences are to be pushed
 *
 *  !> Activity Objects/Primitives
 *      !> mNumberFormat                        - Number format for displaying double values in two decimal places
 *      !> mNotificationHelper                  - Reference to NotificationHelper Object
 *      !> mCurrentGeofence                     - Current Geofence ID
 *      !> mIsWithinGeofece                     - Flag to check if user is currently on set geofence
 *      !> mMainActivity                        - MainActivity reference for static operations
 *
 *  !> Constants
 *      !> TAG                                  - Debug Purposes
 *      !> ACCESS_FINE_LOCATION_CODE            - Access Fine Location Code
 *      !> ACCESS_BACKGROUND_LOCATION_CODE      - Access Background Location Code
 *      !> ACCESS_COARSE_LOCATION_CODE          - Access Course Location Code
 *      !> BUTTON_TAG_INIT                      - Determination of Initialization state for Button
 *      !> BUTTON_TAG_END                       - Determination of Termination state for Button
 *      !> LOCATION_UPDATE_INTERVAL             - Update Interval in ms
 *      !> DIRECTION_SOUTHBOUND                 - Flag for Southbound
 *      !> DIRECTION_NORTHBOUND                 - Flag for Northbound
 *      !> NOTIFICATION_ID_RIGHT_TURN           - Notification channel id for turns
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // UI Elements
    Button mBtnStartMonitoring;
    ConstraintLayout mConstraintLayout;
    TextView mTxtAcceleration;
    TextView mTxtDirection;
    TextView mTxtIsInGeofence;
    TextView mTxtLastGeofence;
    TextView mTxtLat;
    TextView mTxtLong;
    TextView mTxtSpdStatus;
    TextView mTxtSpeed;
    TextView mTxtTitleDirection;
    TextView mTxtTitleInsideLoc;
    TextView mTxtTitleLastLoc;
    TextView mTxtTitleLat;
    TextView mTxtTitleLon;
    TextView mTxtTurnStatus;

    // Location Objects
    FusedLocationProviderClient mFusedLocationClient;
    Location mPreviousLocation;
    LocationCallback mLocationCallback;
    LocationRequest mLocationRequest;

    // Geofencing Objects
    GeofenceHelper mGeofenceHelper;
    GeofencingClient mGeofencingClient;
    String mTrajectory;
    boolean mIsGefencePushNeeded;

    // Activity variables
    NumberFormat mNumberFormat;
    private static String mCurrentGeofence;
    public static MainActivity mMainActivity;
    static NotificationHelper mNotificationHelper;
    static boolean mIsWithinGeofence;

    private final String BUTTON_TAG_END = "END";
    private final String BUTTON_TAG_INIT = "INIT";
    private final String DIRECTION_NORTHBOUND = "NORTHBOUND";
    private final String DIRECTION_SOUTHBOUND = "SOUTHBOUND";
    private final String TAG = "MAIN-ACT";
    private final int ACCESS_BACKGROUND_LOCATION_CODE = 1002;
    private final int ACCESS_COARSE_LOCATION_CODE = 1003;
    private final int ACCESS_FINE_LOCATION_CODE = 1001;
    private final int LOCATION_UPDATE_INTERVAL = 1000;
    private final int NOTIFICATION_ID_RIGHT_TURN = 2002;


    /**
     * onCreate(): Primary entry point of the Application
     * @param savedInstanceState - SavedInstanceState object for onResume() activities
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIsGefencePushNeeded = true;
        mMainActivity = this;

        // Initialize UI Elements
        initializeUi();

        // Request location permissions
        requestPermissions();
    }

    /**
     * initializeUi(): Initializes all TextView objects
     */
    protected void initializeUi() {
        // Parent Constraint Layout
        mConstraintLayout = findViewById(R.id.layout_constraint_layout);

        // Textviews
        mTxtSpeed = findViewById(R.id.txt_speed);
        mTxtLong = findViewById(R.id.txt_long);
        mTxtLat = findViewById(R.id.txt_lat);
        mTxtAcceleration = findViewById(R.id.txt_acceleration);
        mTxtLastGeofence = findViewById(R.id.txt_last_geofence);
        mTxtIsInGeofence = findViewById(R.id.txt_is_in_geo);
        mTxtSpdStatus = findViewById(R.id.txt_spd_status);
        mTxtTurnStatus = findViewById(R.id.txt_turn_status);
        mTxtTitleLat = findViewById(R.id.txt_title_lat);
        mTxtTitleLon = findViewById(R.id.txt_title_lon);
        mTxtTitleLastLoc = findViewById(R.id.txt_title_last_loc);
        mTxtTitleInsideLoc = findViewById(R.id.txt_title_inside_loc);
        mTxtDirection = findViewById(R.id.txt_direction);
        mTxtTitleDirection = findViewById(R.id.txt_title_direction);

        // TextView visibility
        mTxtSpeed.setVisibility(View.GONE);
        mTxtLong.setVisibility(View.GONE);
        mTxtLat.setVisibility(View.GONE);
        mTxtAcceleration.setVisibility(View.GONE);
        mTxtLastGeofence.setVisibility(View.GONE);
        mTxtIsInGeofence.setVisibility(View.GONE);
        mTxtSpdStatus.setVisibility(View.GONE);
        mTxtTurnStatus.setVisibility(View.GONE);
        mTxtTitleLat.setVisibility(View.GONE);
        mTxtTitleLon.setVisibility(View.GONE);
        mTxtTitleLastLoc.setVisibility(View.GONE);
        mTxtTitleInsideLoc.setVisibility(View.GONE);
        mTxtDirection.setVisibility(View.GONE);
        mTxtTitleDirection.setVisibility(View.GONE);

        // Button
        mBtnStartMonitoring = findViewById(R.id.btn_start_monitoring);
        mBtnStartMonitoring.setTag(BUTTON_TAG_INIT);
        mBtnStartMonitoring.setOnClickListener(this);
    }

    /**
     * requestPermissions(): method that requests necessary android permissions from user
     */
    protected void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Check if Android Version is 29 and up
                if (Build.VERSION.SDK_INT >= 29) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        popSnackbar("Permissions all set");
                    } else {
                        Log.d(TAG, "requestPermissions() :: No Background Location Permission");
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, ACCESS_BACKGROUND_LOCATION_CODE);
                    }
                } else {
                    popSnackbar("Permissions all set");
                }
            } else {
                Log.d(TAG, "requestPermissions() :: No Fine Location Permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, ACCESS_COARSE_LOCATION_CODE);
            }
        } else {
            List<String> arrayList = new ArrayList<>();

            arrayList.add(Manifest.permission.ACCESS_FINE_LOCATION);

            Log.d(TAG, "requestPermissions() :: No Fine Location Permission");
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "requestPermissions() :: No Coarse Location Permission");
                arrayList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "requestPermissions() :: No Background Location Permission");
                arrayList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }

            String[] arr = arrayList.toArray(new String[arrayList.size()]);

            Log.d(TAG, "Permissions array: " + arr.toString());

            ActivityCompat.requestPermissions(this, arr, ACCESS_FINE_LOCATION_CODE);
        }
    }

    /**
     * popSnackbar(): Make and display snackbar
     * @param text - Display text
     */
    protected void popSnackbar(String text) {
        Snackbar.make(mConstraintLayout, text, Snackbar.LENGTH_LONG).show();
    }

    /**
     * onClick(): Implemented method for button onClick() event
     * @param v - the view object for reference to the Application UI
     */
    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick() :: init");

        if (BUTTON_TAG_INIT.equals(v.getTag())) {
            monitorStart(v);
        } else if (BUTTON_TAG_END.equals(v.getTag())) {
            monitorEnd(v);
        }
    }

    /**
     * monitorStart(): Start all location monitoring process and initiates dependent processes
     * @param v - the view object for reference to the Application UI
     */
    protected void monitorStart(View v) {
        v.animate().translationY(500).start();

        // Initialize UI Strings
        setUiElements();

        // Initialize Location services
        initializeLocation();

        // Initialize Geofencing services
        initializeGeofence();
    }

    /**
     * monitorEnd(): End all location monitoring process and removes all registered geofences
     * @param v - the view object for reference to the Application UI
     */
    private void monitorEnd(View v) {
        v.animate().translationY(0).start();

        mTxtSpeed.setVisibility(View.GONE);
        mTxtLong.setVisibility(View.GONE);
        mTxtLat.setVisibility(View.GONE);
        mTxtAcceleration.setVisibility(View.GONE);
        mTxtLastGeofence.setVisibility(View.GONE);
        mTxtIsInGeofence.setVisibility(View.GONE);
        mTxtSpdStatus.setVisibility(View.GONE);
        mTxtTurnStatus.setVisibility(View.GONE);
        mTxtTitleLat.setVisibility(View.GONE);
        mTxtTitleLon.setVisibility(View.GONE);
        mTxtTitleLastLoc.setVisibility(View.GONE);
        mTxtTitleInsideLoc.setVisibility(View.GONE);
        mTxtDirection.setVisibility(View.GONE);
        mTxtTitleDirection.setVisibility(View.GONE);

        mBtnStartMonitoring.setText(R.string.START);
        mBtnStartMonitoring.setBackgroundColor(getResources().getColor(R.color.pastel_green));

        mBtnStartMonitoring.setTag(BUTTON_TAG_INIT);

        if (mFusedLocationClient != null && mLocationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            Log.d(TAG, "Stopping Process");
            removeGeofences();
        }
    }


    /**
     * setUiElements(): Set initial value for TextViews
     */
    protected void setUiElements() {
        mTxtSpeed.setText(R.string.ZERO_KMH);
        mTxtLong.setText("-");
        mTxtLat.setText("-");
        mTxtAcceleration.setText("-");
        mTxtLastGeofence.setText("-");
        mTxtIsInGeofence.setText("-");
        mTxtSpdStatus.setText("-");
        mTxtTurnStatus.setText("-");
        mTxtDirection.setText("-");

        mTxtSpeed.setVisibility(View.VISIBLE);
        mTxtLong.setVisibility(View.VISIBLE);
        mTxtLat.setVisibility(View.VISIBLE);
        mTxtAcceleration.setVisibility(View.VISIBLE);
        mTxtLastGeofence.setVisibility(View.VISIBLE);
        mTxtIsInGeofence.setVisibility(View.VISIBLE);
        mTxtSpdStatus.setVisibility(View.VISIBLE);
        mTxtTurnStatus.setVisibility(View.VISIBLE);
        mTxtTitleLat.setVisibility(View.VISIBLE);
        mTxtTitleLon.setVisibility(View.VISIBLE);
        mTxtTitleLastLoc.setVisibility(View.VISIBLE);
        mTxtTitleInsideLoc.setVisibility(View.VISIBLE);
        mTxtTitleDirection.setVisibility(View.VISIBLE);
        mTxtDirection.setVisibility(View.VISIBLE);

        mBtnStartMonitoring.setText(R.string.STOP);
        mBtnStartMonitoring.setBackgroundColor(getResources().getColor(R.color.pastel_red));
        mBtnStartMonitoring.setTag(BUTTON_TAG_END);
    }

    /**
     * initializeLocation(): method to initialize all location assets and configurations.
     */
    protected void initializeLocation() {
        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(LOCATION_UPDATE_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // FusedLocationClient for interfacing with actual location details
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Triggered when there's updates to the location
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                onLocationChanged(locationResult.getLastLocation());
            }
        };

        // Execute the location request via the client
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    /**
     * initializeGeofence(): Initializes geofencing client and other assets from the GeofenceHelper class
     */
    public void initializeGeofence() {
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        mGeofenceHelper = new GeofenceHelper(this);
    }

    /**
     * onLocationChanged(): Triggered when there is a change in location based on FusedLocationClient
     * @param location - Location object
     */
    protected void onLocationChanged(Location location) {
        double kmPerHour = location.getSpeed() * 18 / 5;
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();

        if (mPreviousLocation == null) {
            mPreviousLocation = location;
            return;
        }

        double distanceDelta = mPreviousLocation.distanceTo(location);

        // User changed position
        if (distanceDelta != 0) {

            // Check if northbound or southbound

            double latDelta = location.getLatitude() - mPreviousLocation.getLatitude();

            Log.d(TAG, "latDelta: " + latDelta);

            if (latDelta < 0) {
                // Southbound

                Log.d(TAG, "SOUTHBOUND");

                if (mIsGefencePushNeeded) {
                    pushGeofencesSouth();
                    mIsGefencePushNeeded = false;
                } else {
                    if (!mTrajectory.equals(DIRECTION_SOUTHBOUND)) {
                        removeGeofences();
                        mIsGefencePushNeeded = true;
                        return;
                    }
                }

                mTrajectory = DIRECTION_SOUTHBOUND;
            } else {
                // Northbound

                Log.d(TAG, "NORTHBOUND");

                if (mIsGefencePushNeeded) {
                    pushGeofencesNorth();
                    mIsGefencePushNeeded = false;
                } else {
                    if (!mTrajectory.equals(DIRECTION_NORTHBOUND)) {
                        removeGeofences();
                        mIsGefencePushNeeded = true;
                        return;
                    }
                }

                mTrajectory = DIRECTION_NORTHBOUND;
            }

            // Acceleration with constant time
            double acceleration = (location.getSpeed() - mPreviousLocation.getSpeed()) / 1;

            // Acceleration with time delta
            double accelerationTd = (location.getSpeed() - mPreviousLocation.getSpeed()) / ((location.getTime() - mPreviousLocation.getTime()) / 1000.0);

            // Set TextView values
            setTextViewValues(kmPerHour, new LatLng(latitude, longitude), acceleration, accelerationTd, mTrajectory);

            // Modify TextViews for warnings when speed & acceleration thresholds are reached
            checkNextTurnLocation(mCurrentGeofence, location);

            mPreviousLocation = location;

            // Refresh TextViews
            invalidateTextViews();
        }
    }

    /**
     * invalidateTextViews(): method to refresh TextViews
     */
    private void invalidateTextViews() {
        mTxtSpeed.invalidate();
        mTxtSpeed.requestLayout();

        mTxtLong.invalidate();
        mTxtLong.requestLayout();

        mTxtLat.invalidate();
        mTxtLat.requestLayout();

        mTxtLastGeofence.invalidate();
        mTxtLastGeofence.requestLayout();

        mTxtIsInGeofence.invalidate();
        mTxtIsInGeofence.requestLayout();

        mTxtSpdStatus.invalidate();
        mTxtSpdStatus.requestLayout();

        mTxtTurnStatus.invalidate();
        mTxtTurnStatus.requestLayout();

        mTxtAcceleration.invalidate();
        mTxtAcceleration.requestLayout();

        mTxtDirection.invalidate();
        mTxtDirection.requestLayout();
    }

    /**
     * checkNextTurnLocation(): Checks if current location is near on of the turns. If yes, function will send notification
     * @param geofenceId
     * @param currrentLocation
     */
    private void checkNextTurnLocation(String geofenceId, Location currrentLocation) {
        Location nextTurn = new Location("");
        String trnStatus = "";

        if (!mIsWithinGeofence) {
            return;
        }

        if (geofenceId.equals(getString(R.string.SB_SAN_SIMON_ID))) {
            nextTurn.setLatitude(Double.parseDouble(getString(R.string.SB_SAN_SIMON_TRN_LAT)));
            nextTurn.setLongitude(Double.parseDouble(getString(R.string.SB_SAN_SIMON_TRN_LON)));
            trnStatus = "Nearing San Simon right turn";
        } else if (geofenceId.equals(getString(R.string.SB_DON_ANTONIO_ID))) {
            nextTurn.setLatitude(Double.parseDouble(getString(R.string.SB_DON_ANTONIO_TRN_LAT)));
            nextTurn.setLongitude(Double.parseDouble(getString(R.string.SB_DON_ANTONIO_TRN_LON)));
            trnStatus = "Nearing Don Antonio right turn";
        } else if (geofenceId.equals(getString(R.string.SB_LUZON_AVE_ID))) {
            nextTurn.setLatitude(Double.parseDouble(getString(R.string.SB_LUZON_AVE_TRN_LAT)));
            nextTurn.setLongitude(Double.parseDouble(getString(R.string.SB_LUZON_AVE_TRN_LON)));
            trnStatus = "Nearing Luzon Ave right turn";
        } else if (geofenceId.equals(getString(R.string.SB_TANDANG_SORA_ID))) {
            nextTurn.setLatitude(Double.parseDouble(getString(R.string.SB_TANDANG_SORA_TRN_LAT)));
            nextTurn.setLongitude(Double.parseDouble(getString(R.string.SB_TANDANG_SORA_TRN_LON)));
            trnStatus = "Nearing Tandang Sora right turn";
        } else if (geofenceId.equals(getString(R.string.SB_CENTRAL_AVE_ID))) {
            nextTurn.setLatitude(Double.parseDouble(getString(R.string.SB_CENTRAL_AVE_TRN_LAT)));
            nextTurn.setLongitude(Double.parseDouble(getString(R.string.SB_CENTRAL_AVE_TRN_LON)));
            trnStatus = "Nearing Central Ave right turn";
        } else if (geofenceId.equals(getString(R.string.NB_UNIVERSITY_AVE_ID))) {
            nextTurn.setLatitude(Double.parseDouble(getString(R.string.NB_UNIVERSITY_AVE_TRN_LAT)));
            nextTurn.setLongitude(Double.parseDouble(getString(R.string.NB_UNIVERSITY_AVE_TRN_LON)));
            trnStatus = "Nearing University Ave right turn";
        } else if (geofenceId.equals(getString(R.string.NB_TANDANG_SORA_ID))) {
            nextTurn.setLatitude(Double.parseDouble(getString(R.string.NB_TANDANG_SORA_TRN_LAT)));
            nextTurn.setLongitude(Double.parseDouble(getString(R.string.NB_TANDANG_SORA_TRN_LON)));
            trnStatus = "Nearing Tandang Sora right turn";
        } else if (geofenceId.equals(getString(R.string.NB_ZUZUARREGUI_ST_ID))) {
            nextTurn.setLatitude(Double.parseDouble(getString(R.string.NB_ZUZUARREGUI_ST_TRN_LAT)));
            nextTurn.setLongitude(Double.parseDouble(getString(R.string.NB_ZUZUARREGUI_ST_TRN_LON)));
            trnStatus = "Nearing Zuzuarregui St right turn";
        } else if (geofenceId.equals(getString(R.string.NB_AMSTERDAM_AVE_ID))) {
            nextTurn.setLatitude(Double.parseDouble(getString(R.string.NB_AMSTERDAM_AVE_TRN_LAT)));
            nextTurn.setLongitude(Double.parseDouble(getString(R.string.NB_AMSTERDAM_AVE_TRN_LON)));
            trnStatus = "Nearing Amsterdam Ave right turn";
        } else if (geofenceId.equals(getString(R.string.NB_BATASAN_ID))) {
            nextTurn.setLatitude(Double.parseDouble(getString(R.string.NB_BATASAN_TRN_LAT)));
            nextTurn.setLongitude(Double.parseDouble(getString(R.string.NB_BATASAN_TRN_LON)));
            trnStatus = "Nearing Batasan right turn";
        } else if (geofenceId.equals(getString(R.string.NB_IBP_RD_ID))) {
            nextTurn.setLatitude(Double.parseDouble(getString(R.string.NB_IBP_RD_TRN_LAT)));
            nextTurn.setLongitude(Double.parseDouble(getString(R.string.NB_IBP_RD_TRN_LON)));
            trnStatus = "Nearing IBP Road right turn";
        }

        if (currrentLocation.distanceTo(nextTurn) <= 50) {
            mNotificationHelper.sendHighPriorityNotification("NEARING TURN", trnStatus, MainActivity.class, NOTIFICATION_ID_RIGHT_TURN);
        }
    }

    /**
     * checkLimits(): Sets the display if speed and/or acceleration reaches set threshold
     * @param speed
     * @param acceleration
     */
    private void checkLimits(double speed, double acceleration) {
        if (mIsWithinGeofence) {
            if (speed > 40) {
                if (acceleration > 1.96) {
                    mTxtSpdStatus.setText(getString(R.string.SPD_ACC_LIMIT));
                    mTxtSpeed.setTextColor(getResources().getColor(R.color.pastel_red));
                    mTxtAcceleration.setTextColor(getResources().getColor(R.color.pastel_red));
                } else {
                    mTxtSpdStatus.setText(getString(R.string.SPD_LIMIT));
                    mTxtSpeed.setTextColor(getResources().getColor(R.color.pastel_red));
                    mTxtAcceleration.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
                }
            } else {
                if (acceleration > 1.96) {
                    mTxtSpdStatus.setText(getString(R.string.ACC_LIMIT));
                    mTxtAcceleration.setTextColor(getResources().getColor(R.color.pastel_red));
                } else {
                    mTxtSpdStatus.setText(getString(R.string.SPD_ACC_NORMAL));
                    mTxtAcceleration.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
                }
                mTxtSpeed.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
            }
        } else {
            if (speed > 60) {
                if (acceleration > 1.47) {
                    mTxtSpdStatus.setText(getString(R.string.SPD_ACC_LIMIT));
                    mTxtSpeed.setTextColor(getResources().getColor(R.color.pastel_red));
                    mTxtAcceleration.setTextColor(getResources().getColor(R.color.pastel_red));
                } else {
                    mTxtSpdStatus.setText(getString(R.string.SPD_LIMIT));
                    mTxtSpeed.setTextColor(getResources().getColor(R.color.pastel_red));
                    mTxtAcceleration.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
                }
            } else {
                if (acceleration > 1.47) {
                    mTxtSpdStatus.setText(getString(R.string.ACC_LIMIT));
                    mTxtAcceleration.setTextColor(getResources().getColor(R.color.pastel_red));
                } else {
                    mTxtSpdStatus.setText(getString(R.string.SPD_ACC_NORMAL));
                    mTxtAcceleration.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
                }
                mTxtSpeed.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
            }
        }
    }

    /**
     * setTextViewValues(): Sets relevant TextViews with corresponding values
     * @param speed - Speed in km/h
     * @param latLng - LatLng object for coordinates
     * @param acceleration - Acceleration in m/s^2 (Constant Time)
     * @param accelerationTd - Acceleration in m/s^2 (Delta Time)
     * @param trajectory - Direction of current traffic (Southbound/Northbound)
     */
    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void setTextViewValues(double speed, LatLng latLng, double acceleration, double accelerationTd, String trajectory) {
        mTxtSpeed.setText(String.format("%.2f", speed) + "km/h");
        mTxtLat.setText(String.valueOf(latLng.latitude));
        mTxtLong.setText(String.valueOf(latLng.longitude));
        mTxtDirection.setText(trajectory);

        if (mNumberFormat == null) {
            mNumberFormat = new DecimalFormat("#0.00");
        }

        mTxtAcceleration.setText(mNumberFormat.format(acceleration) + " m/s\u00B2" + " || " + mNumberFormat.format(accelerationTd) + " m/s\u00B2");
        Log.d(TAG, mNumberFormat.format(acceleration) + " m/s\u00B2");

        if (mCurrentGeofence != null) {
            String[] currentLocationSplit = mCurrentGeofence.split("_");

            if (currentLocationSplit.length == 3) {
                String currentLocation = currentLocationSplit[1].substring(0, 1).toUpperCase() + currentLocationSplit[1].substring(1).toLowerCase() + " " + currentLocationSplit[2].substring(0, 1).toUpperCase() + currentLocationSplit[2].substring(1).toLowerCase();
                mTxtLastGeofence.setText((mCurrentGeofence.equals("")) ? "-" : currentLocation);
            } else {
                mTxtLastGeofence.setText("-");
            }
        } else {
            mTxtLastGeofence.setText("-");
        }

        mTxtIsInGeofence.setText(String.valueOf(mIsWithinGeofence));

        checkLimits(speed, acceleration);
    }

    /**
     * pushGeofencesSouth(): Push Southbound geofences
     *  - San Simon
     *  - Don Antonio
     *  - Luzon Ave
     *  - Tandang Sora
     *  - Central
     */
    private void pushGeofencesSouth() {
        removeGeofences();
        Log.d(TAG, "Pushing South Geofences");
        addGeofence(
                getString(R.string.SB_SAN_SIMON_ID),
                new LatLng(Double.parseDouble(getString(R.string.SB_SAN_SIMON_LAT)), Double.parseDouble(getString(R.string.SB_SAN_SIMON_LON))),
                Float.parseFloat(getString(R.string.SB_SAN_SIMON_RAD))
        );

        addGeofence(
                getString(R.string.SB_DON_ANTONIO_ID),
                new LatLng(Double.parseDouble(getString(R.string.SB_DON_ANTONIO_LAT)), Double.parseDouble(getString(R.string.SB_DON_ANTONIO_LON))),
                Float.parseFloat(getString(R.string.SB_DON_ANTONIO_RAD))
        );

        addGeofence(
                getString(R.string.SB_LUZON_AVE_ID),
                new LatLng(Double.parseDouble(getString(R.string.SB_LUZON_AVE_LAT)), Double.parseDouble(getString(R.string.SB_LUZON_AVE_LON))),
                Float.parseFloat(getString(R.string.SB_LUZON_AVE_RAD))
        );

        addGeofence(
                getString(R.string.SB_TANDANG_SORA_ID),
                new LatLng(Double.parseDouble(getString(R.string.SB_TANDANG_SORA_LAT)), Double.parseDouble(getString(R.string.SB_TANDANG_SORA_LON))),
                Float.parseFloat(getString(R.string.SB_TANDANG_SORA_RAD))
        );

        addGeofence(
                getString(R.string.SB_CENTRAL_AVE_ID),
                new LatLng(Double.parseDouble(getString(R.string.SB_CENTRAL_AVE_LAT)), Double.parseDouble(getString(R.string.SB_CENTRAL_AVE_LON))),
                Float.parseFloat(getString(R.string.SB_CENTRAL_AVE_RAD))
        );
    }

    /**
     * pushgeofencesNorth(): Push Northbound geofences
     *  - University Ave
     *  - Tandang Sora
     *  - Zuzuarregui St
     *  - Amsterdam Ave
     *  - Batasan Rd
     *  - IBP Rd
     */
    private void pushGeofencesNorth() {
        removeGeofences();
        Log.d(TAG, "Pushing North Geofences");
        addGeofence(
                getString(R.string.NB_UNIVERSITY_AVE_ID),
                new LatLng(Double.parseDouble(getString(R.string.NB_UNIVERSITY_AVE_LAT)), Double.parseDouble(getString(R.string.NB_UNIVERSITY_AVE_LON))),
                Float.parseFloat(getString(R.string.NB_UNIVERSITY_AVE_RAD))
        );

        addGeofence(
                getString(R.string.NB_TANDANG_SORA_ID),
                new LatLng(Double.parseDouble(getString(R.string.NB_TANDANG_SORA_LAT)), Double.parseDouble(getString(R.string.NB_TANDANG_SORA_LON))),
                Float.parseFloat(getString(R.string.NB_TANDANG_SORA_RAD))
        );

        addGeofence(
                getString(R.string.NB_ZUZUARREGUI_ST_ID),
                new LatLng(Double.parseDouble(getString(R.string.NB_ZUZUARREGUI_ST_LAT)), Double.parseDouble(getString(R.string.NB_ZUZUARREGUI_ST_LON))),
                Float.parseFloat(getString(R.string.NB_ZUZUARREGUI_ST_RAD))
        );

        addGeofence(
                getString(R.string.NB_AMSTERDAM_AVE_ID),
                new LatLng(Double.parseDouble(getString(R.string.NB_AMSTERDAM_AVE_LAT)), Double.parseDouble(getString(R.string.NB_AMSTERDAM_AVE_LON))),
                Float.parseFloat(getString(R.string.NB_AMSTERDAM_AVE_RAD))
        );

        addGeofence(
                getString(R.string.NB_BATASAN_ID),
                new LatLng(Double.parseDouble(getString(R.string.NB_BATASAN_LAT)), Double.parseDouble(getString(R.string.NB_BATASAN_LON))),
                Float.parseFloat(getString(R.string.NB_BATASAN_RAD))
        );

        addGeofence(
                getString(R.string.NB_IBP_RD_ID),
                new LatLng(Double.parseDouble(getString(R.string.NB_IBP_RD_LAT)), Double.parseDouble(getString(R.string.NB_IBP_RD_LON))),
                Float.parseFloat(getString(R.string.NB_IBP_RD_RAD))
        );
    }

    /**
     * addGeofence(): method to add a geofence
     * @param geofenceId - Geofence Identifier
     * @param latLng - LatLng object for coordinates
     * @param radius - Radius of Geofence circle
     */
    private void addGeofence(String geofenceId, LatLng latLng, float radius) {
        Geofence geofence = mGeofenceHelper.getGeofence(geofenceId, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);

        PendingIntent pendingIntent = mGeofenceHelper.getPendingIntent();

        GeofencingRequest geofencingRequest = mGeofenceHelper.getGeofencingRequest(geofence);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        mGeofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "Geofences added");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMessage = mGeofenceHelper.getErrorString(e);

                        Log.d(TAG, "onFailure " + errorMessage);
                    }
                });
    }

    /**
     * removeGeofences(): method to remove all registered geofences using the GeofenceHelper class
     */
    public void removeGeofences() {
        PendingIntent pendingIntent = mGeofenceHelper.getPendingIntent();

        mGeofencingClient.removeGeofences(pendingIntent)
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences removed
                        Log.d(TAG, "GEOFENCES REMOVED");
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to remove geofences
                        Log.d(TAG, "FAIL GEOFENCES REMOVED");
                    }
                });
    }

    /**
     * GeofenceBroadcastReceiver
     *
     * Type: Static
     * Superclass: BroadcastReceiver
     * Interfaces: N/A
     *
     * Properties:
     *  !> TAG: For debug purposes
     *      - Type: String
     *      - Modifier: Final
     *      - Value: "BROADCAST"
     *  !> NOTIFICATION_ID_GEOFENCE_TRANSITION: Constant for Notification ID, as required by notification channel
     *      - Type: Int
     *      - Modifier: Final
     *      - Value: 2001
     */
    public static class GeofenceBroadcastReceiver extends BroadcastReceiver {

        protected final String TAG = "BROADCAST";
        private final int NOTIFICATION_ID_GEOFENCE_TRANSITION = 2001;

        /**
         * onReceive(): invoked when geofences are triggered (entry, dwell, exit)
         * @param context - Application Context
         * @param intent - Intent object from the application context
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

            if (geofencingEvent.hasError()) {
                Log.d(TAG, "onReceive() :: Error receiving geofence event");
                return;
            }

            if (mNotificationHelper == null) {
                mNotificationHelper = new NotificationHelper(context);
            }

            List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();
            for (Geofence geofence: geofenceList) {
                Log.d(TAG, "onReceive() :: " + geofence.getRequestId());

                int transitionType = geofencingEvent.getGeofenceTransition();

                String[] currentLocationSplit = geofence.getRequestId().split("_");
                String currentLocation = currentLocationSplit[1].substring(0, 1).toUpperCase() + currentLocationSplit[1].substring(1).toLowerCase() + " " + currentLocationSplit[2].substring(0, 1).toUpperCase() + currentLocationSplit[2].substring(1).toLowerCase();

                String title = "";
                String description = "";

                mCurrentGeofence = geofence.getRequestId();

                switch (transitionType) {
                    case Geofence.GEOFENCE_TRANSITION_ENTER:
                        title = "Entering Location";
                        description = "Entering on: " + currentLocation;
                        mIsWithinGeofence = true;
                        break;
                    case Geofence.GEOFENCE_TRANSITION_DWELL:
                        title = "Dwelling Location";
                        description = "Dwelling on: " + currentLocation;
                        mIsWithinGeofence = true;
                        break;
                    case Geofence.GEOFENCE_TRANSITION_EXIT:
                        title = "Exiting Location";
                        description = "Exiting: " + currentLocation;
                        mIsWithinGeofence = false;
                        break;
                }

                mMainActivity.popSnackbar(description);
                mNotificationHelper.sendHighPriorityNotification(title, description, MainActivity.class, NOTIFICATION_ID_GEOFENCE_TRANSITION);
            }
        }
    }
}