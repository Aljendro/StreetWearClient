package alejandro.alvarado.com.streetwear;

import alejandro.alvarado.com.streetwear.Network.RetrofitNetwork;
import alejandro.alvarado.com.streetwear.Network.RowsChanged;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static alejandro.alvarado.com.streetwear.R.string.submitPotholeButton;


/**
 * Taken from the camera application tutorial
 * https://developer.android.com/guide/topics/media/camera.html
 * with additions for uploading the image to the network
 */
public class CameraCapturePotholes extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener
        , LocationListener {

    private static int cameraId = 0;
    private Camera mCamera;
    private CameraPreview mCameraPreview;

    // Requires the Google API for location services
    GoogleApiClient mGoogleApiClient;

    // Location update settings
    private LocationRequest mLocationRequest;
    private final int extreme_accuracy = 5000; // Receive an update every 5 seconds
    private final int background_accuracy = 10000; // Receive updates every 10 seconds


    // Save the location from the last update
    Location mLastLocation;
    // This is only a measure to stop images from submitting when
    // the user is standing still.
    // The actual image capture relies on the timing
    // set in the LocationRequest object.
    double mMinimumDistanceForNextImage = 3.0;

    private static final String TAG = CameraCapturePotholes.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_capture_potholes);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mCameraPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mCameraPreview);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(extreme_accuracy);
        mLocationRequest.setInterval(extreme_accuracy * 2);
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

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();              // release the camera immediately on pause event
        mLocationRequest.setFastestInterval(background_accuracy);
        mLocationRequest.setInterval(background_accuracy * 2);
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }
    /** A safe way to get an instance of the Camera object. */
    public Camera getCameraInstance(){
        Camera c = null;
        try {
            releaseCamera();
            c = Camera.open(cameraId); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            Log.e(TAG, "The Camera is not available or does not exist");
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connected to the GoogleClientApi");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Play services connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Refer to the reference doc for ConnectionResult to see what error codes might
        // be returned in onConnectionFailed.
        Log.d(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
    }

    public void startCapturing (View v) {
        try {
            // Need to hide the capture button
            final Button startCaptureButton =  (Button) findViewById(R.id.startCapture);
            startCaptureButton.setVisibility(View.GONE);

            // Need to show the stop capture button
            final Button stopCaptureButton = (Button) findViewById(R.id.stopCapture);
            stopCaptureButton.setVisibility(View.VISIBLE);

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
            Log.d(TAG, "The location updates have STARTED....");

        } catch (SecurityException e) {
            Log.d(TAG, "The user needs to grant permissions for location");
        }
    }

    public void stopCapturing (View v) {

        // Need to hide the capture button
        final Button startCaptureButton =  (Button) findViewById(R.id.startCapture);
        startCaptureButton.setVisibility(View.VISIBLE);

        // Need to show the stop capture button
        final Button stopCaptureButton = (Button) findViewById(R.id.stopCapture);
        stopCaptureButton.setVisibility(View.GONE);

        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);

        Log.d(TAG, "The location updates have STOPPED....");
    }

    @Override
    public void onLocationChanged(Location location) {
        // This callback will fire off based on the settings to the LocationRequest object
        // handed to the FusedLocation Api

        Log.d(TAG, "The onLocationChanged callback has fired!!!");

        double distanceToLastLocation = mMinimumDistanceForNextImage + 1;
        if (mLastLocation != null) {
            distanceToLastLocation = location.distanceTo(mLastLocation);
        }

        if (distanceToLastLocation > mMinimumDistanceForNextImage) {

            Log.d(TAG, "We are sufficiently far away and we can now take an image ...");

            final double latitude = location.getLatitude();
            final double longitude = location.getLongitude();

            // Change the last image location
            mLastLocation = location;

            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    File tempFile = null;
                    try {
                        tempFile = File.createTempFile("streetwear", ".jpeg", null);
                        FileOutputStream fos = new FileOutputStream(tempFile);
                        fos.write(data);
                    } catch (FileNotFoundException e) {
                        Log.d(TAG, "FILE was not able to be found");
                    } catch (IOException e) {
                        Log.d(TAG, "The temporary file was not able to be created.");
                    }
                    RequestBody requestFile =
                            RequestBody.create(MediaType.parse("image/jpeg"), tempFile);
                    MultipartBody.Part body =
                            MultipartBody.Part.createFormData("file", tempFile.getName(), requestFile);

                    Call<RowsChanged> call =
                            RetrofitNetwork.getInstance().getService().uploadImage(latitude, longitude, body);

                    call.enqueue(new Callback<RowsChanged>() {
                        @Override
                        public void onResponse(Call<RowsChanged> call, Response<RowsChanged> response) {
                            Log.i(TAG, "The response from the upload image request was: " + response.toString());
                        }

                        @Override
                        public void onFailure(Call<RowsChanged> call, Throwable t) {
                            Log.d(TAG, "The server was unable to handle request from upload image");
                        }
                    });
                }
            });
        }
    }


    // TODO: 4/13/17 Need to create a workflow to ask the user for camera permissions
    // TODO: 4/13/17 Need to create a workflow to ask the user for location permissions
    
}
