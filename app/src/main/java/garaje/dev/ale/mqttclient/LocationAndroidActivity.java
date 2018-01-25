package garaje.dev.ale.mqttclient;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;

import garaje.dev.ale.services.LastLocationService;

public class LocationAndroidActivity extends AppCompatActivity implements LastLocationService.MyCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    // UI Widgets.

    private TextView mLastUpdateTimeTextView;
    private TextView mLatitudeTextView;
    private TextView mLongitudeTextView;

    // Labels.
    private String mLatitudeLabel;
    private String mLongitudeLabel;
    private String mLastUpdateTimeLabel;

    //map
    GoogleMap mGoogleMap;
    Marker mCurrLocationMarker;

    private Location mCurrentLocation;
    private String mLastUpdateTime;

    //service lastLocation
    LastLocationService mLastLocationService;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LastLocationService.LocalBinder binder = (LastLocationService.LocalBinder) iBinder;
            mLastLocationService  = binder.getService();

            mLastLocationService.registerClient(LocationAndroidActivity.this); //Activity register in the service as client for callabcks!

            Toast.makeText(LocationAndroidActivity.this,"OOOKKK!!",Toast.LENGTH_SHORT);
            Log.d(TAG,"//////Servicio conectado");

            mLastLocationService.startLocationUpdates(LocationAndroidActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
       //mLastLocationService.stopLocationUpdates(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }

    /////////////////////
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_android);

        Intent intent = new Intent(this, LastLocationService.class);
        //startService(intent); //Starting the service
        ////////////// bind the service
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        //////////////////////MAP
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mGoogleMap=googleMap;
                mGoogleMap.setBuildingsEnabled(true);
                //mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

            }
        });
        ////////////////////////
        // Locate the UI widgets.
        mLatitudeTextView =  findViewById(R.id.latitude_text);
        mLongitudeTextView =  findViewById(R.id.longitude_text);
        mLastUpdateTimeTextView =  findViewById(R.id.last_update_time_text);

        // Set labels.
        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mLastUpdateTimeLabel = getResources().getString(R.string.last_update_time_label);

        updateLocationUI();



    }

    @Override
    public void updateClient(Location location, String mLastUpdateTime) {

        this.mCurrentLocation = location;
        this.mLastUpdateTime = mLastUpdateTime;

        /////////////////////// change mark in map
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);

        //move map camera
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,18));

        /////////////////////////////////////

        updateLocationUI();
    }

    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    public void startUpdatesButtonHandler(View view) {
        mLastLocationService.startLocationUpdates(LocationAndroidActivity.this);
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates.
     */
    public void stopUpdatesButtonHandler(View view) {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mLastLocationService.stopLocationUpdates(this);

    }

    /**
     * Sets the value of the UI fields for the location latitude, longitude and last update time.
     */
    private void updateLocationUI() {
        if (mCurrentLocation != null) {
            mLatitudeTextView.setText(String.format(Locale.ENGLISH, "%s: %f", mLatitudeLabel, mCurrentLocation.getLatitude()));
            mLongitudeTextView.setText(String.format(Locale.ENGLISH, "%s: %f", mLongitudeLabel, mCurrentLocation.getLongitude()));
            mLastUpdateTimeTextView.setText(String.format(Locale.ENGLISH, "%s: %s", mLastUpdateTimeLabel, mLastUpdateTime));
        }
    }



}
