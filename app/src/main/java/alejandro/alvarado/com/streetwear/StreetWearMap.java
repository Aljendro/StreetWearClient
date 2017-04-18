package alejandro.alvarado.com.streetwear;


import alejandro.alvarado.com.streetwear.Network.RetrofitNetwork;
import alejandro.alvarado.com.streetwear.Network.RowsChanged;
import alejandro.alvarado.com.streetwear.Network.StreetWearServer;
import android.app.ActionBar;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * An activity that displays a map showing the place at the device's current location.
 */
public class StreetWearMap extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = StreetWearMap.class.getSimpleName();
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    // The entry point to Google Play services, used by the Places API and Fused Location Provider.
    private GoogleApiClient mGoogleApiClient;

    // A default location (Norwalk, California) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(33.9022, -118.0817);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Will track the location of the marker the user drags.
    private Marker mCurrentPositionMarker;
    private Double mCurrentPositionLatitude;
    private Double mCurrentPositionLongitude;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_street_wear_map);

        // Build the Play services client for use by the Fused Location Provider and the Places API.
        // Use the addApi() method to request the Google Places API and the Fused Location Provider.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Builds the map when the Google Play services client is successfully connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Handles failure to connect to the Google Play services client.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // Refer to the reference doc for ConnectionResult to see what error codes might
        // be returned in onConnectionFailed.
        Log.d(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    /**
     * Handles suspension of the connection to the Google Play services client.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Play services connection suspended");
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                // We need to get the final position of the dragged marker to submit a pothole
                LatLng pos = marker.getPosition();
                mCurrentPositionLatitude = pos.latitude;
                mCurrentPositionLongitude = pos.longitude;
            }
        });

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        // Populate the potholes in the area on the map
        populatePotholes();
    }

    /**
     * Sends a get request to the server to show the potholes on the map
     */
    private void populatePotholes() {
        if (mLastKnownLocation != null) {

            Call<List<LatLng>> coordinates =
                    RetrofitNetwork
                            .getInstance().getService().getPotholes(
                            mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude(),
                            100);

            coordinates.enqueue(new retrofit2.Callback<List<LatLng>>() {
                @Override
                public void onResponse(Call<List<LatLng>> call, retrofit2.Response<List<LatLng>> response) {
                    List<LatLng> coordinates = response.body();
                    if (coordinates != null) {
                        for (LatLng coordinate : coordinates) {
                            mMap.addMarker(new MarkerOptions()
                                                 .position(coordinate));
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<LatLng>> call, Throwable t) {
                    Log.e(TAG, "The network request was unable to be completed.");
                }
            });
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        if (mLocationPermissionGranted) {
            mLastKnownLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
        }

        // Set the map's camera position to the current location of the device.
        if (mCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
        } else if (mLastKnownLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        } else {
            Log.d(TAG, "Current location is null. Using defaults.");
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }

        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mLastKnownLocation = null;
        }
    }

    /*
     * The rest of the code will focus be the method for the onclick
     * listeners to the pothole declarations
     */

    // Will place a marker for a the user to drag. The submit button
    // will take care of sending the locations of  the marker to the server.
    public void declarePothole(View view) {

        if (mLastKnownLocation != null) {

            // Need to hide the potholes button
            final Button potholeButton =  (Button) findViewById(R.id.declarationButton);
            potholeButton.setVisibility(View.GONE);

            // Need to show the submit button
            final Button submitPotholeButton = (Button) findViewById(R.id.submitionPotholeButton);
            submitPotholeButton.setVisibility(View.VISIBLE);

            LatLng currentLocation = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());

            // Save the marker so that we can manipulate it and submit it coordinates
            // to the server.
            mCurrentPositionMarker = mMap.addMarker(new MarkerOptions()
                    .position(currentLocation)
                    .title("Pothole?")
                    .draggable(true));

            // Set the latitude and longitude variables for later use
            mCurrentPositionLatitude = currentLocation.latitude;
            mCurrentPositionLongitude = currentLocation.longitude;

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 19));

        } else {
            getDeviceLocation();
        }
    }

    public void submitPothole (View view) {

        if (mCurrentPositionMarker != null) {
            // Need to hide the potholes button
            final Button potholeButton =  (Button) findViewById(R.id.declarationButton);
            potholeButton.setVisibility(View.VISIBLE);

            // Need to show the submit button
            final Button submitPotholeButton = (Button) findViewById(R.id.submitionPotholeButton);
            submitPotholeButton.setVisibility(View.GONE);

            // Place the information we want to post in the body
            HashMap<String, Double> parameters = new HashMap<>();
            parameters.put("latitude", mCurrentPositionLatitude);
            parameters.put("longitude", mCurrentPositionLongitude);

            Call<RowsChanged> request =
                    RetrofitNetwork.getInstance().getService()
                            .postPotholes(parameters);

            // Put the request on the queue
            request.enqueue(new Callback<RowsChanged>() {
                @Override
                public void onResponse(Call<RowsChanged> call, retrofit2.Response<RowsChanged> response) {
                    Log.d(TAG, "The response for the post potholes: " + response.toString());
                }

                @Override
                public void onFailure(Call<RowsChanged> call, Throwable t) {
                    Log.d(TAG, "There was an error with post potholes");
                }
            });

            // Need to get rid of the marker
            mCurrentPositionMarker.remove();
            mCurrentPositionMarker = null;
        }
    }
    // Create the intent that will open the street analyzing activity.
    public void analyzeStreet(View view) {
        Intent streetAnalyze = new Intent(this, CameraCapturePotholes.class);
        startActivity(streetAnalyze);
    }
}
