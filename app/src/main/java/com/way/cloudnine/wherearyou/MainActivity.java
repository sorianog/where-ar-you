package com.way.cloudnine.wherearyou;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.way.cloudnine.wherearyou.utils.DatabaseManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    LocationManager locationManager;
    Context mContext;
    GeomagneticField geoField;
    private ArFragment arrowFragment;
    private ModelRenderable arrowRenderable;
    private boolean installRequested;
    private boolean finishedLoading = false;
    private Snackbar loadingMessageSnackbar = null;
    private ArSceneView arSceneView;
    private ModelRenderable andyRenderable;
    private ViewRenderable firstPointRenderable;
    private ViewRenderable secondPointRenderable;
    private LocationScene locationScene;
    private DatabaseManager databaseManager;
    private int currentWaypoint = 1;

    @Override
    // Called when the activity is created
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the view to the following activity
        setContentView(R.layout.activity_sceneform);

        databaseManager = new DatabaseManager();
        databaseManager.callDatabase();

        arSceneView = findViewById(R.id.ar_scene_view);

        // Build a renderable from a 2D View.
        CompletableFuture<ViewRenderable> firstPoint =
                ViewRenderable.builder()
                        .setView(this, R.layout.activity_main)
                        .build();

        CompletableFuture<ViewRenderable> secondPoint =
                ViewRenderable.builder()
                        .setView(this, R.layout.second_point)
                        .build();

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        CompletableFuture<ModelRenderable> andy = ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build();

        CompletableFuture.allOf(
                firstPoint,
                andy)
                .handle(
                        (notUsed, throwable) -> {
                            // When you build a Renderable, Sceneform loads its resources in the background while
                            // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                            // before calling get().

                            if (throwable != null) {
                                DemoUtils.displayError(this, "Unable to load renderables", throwable);
                                return null;
                            }

                            try {
                                firstPointRenderable = firstPoint.get();
                                secondPointRenderable = secondPoint.get();
                                andyRenderable = andy.get();
                                finishedLoading = true;

                            } catch (InterruptedException | ExecutionException ex) {
                                DemoUtils.displayError(this, "Unable to load renderables", ex);
                            }

                            return null;
                        });

        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        arSceneView
                .getScene().addOnUpdateListener(
                frameTime -> {
                    if (!finishedLoading) {
                        return;
                    }

                    if (locationScene == null) {
                        // If our locationScene object hasn't been setup yet, this is a good time to do it
                        // We know that here, the AR components have been initiated.
                        locationScene = new LocationScene(this, this, arSceneView);

                        // Now lets create our location markers.
                        // First, a layout
                        LocationMarker layoutLocationMarkerFirstPoint = new LocationMarker(
                                databaseManager.waypoints.get(0).getLongitude(),
                                databaseManager.waypoints.get(0).getLatitude(),
                                firstPointView()
                        );

                        LocationMarker layoutLocationMarkerSecondPoint = new LocationMarker(
                                -81.442835,
                                41.54800,
                                secondPointView()
                        );

                        // An example "onRender" event, called every frame
                        // Updates the layout with the markers distance
                        layoutLocationMarkerFirstPoint.setRenderEvent(waypoint -> {

                            View eView = firstPointRenderable.getView();
                            TextView distanceTextView = eView.findViewById(R.id.textView2);
                            distanceTextView.setText(waypoint.getDistance() + "M");
                        });
                        layoutLocationMarkerSecondPoint.setRenderEvent(waypoint -> {

                            View eView = secondPointRenderable.getView();
                            TextView distanceTextView = eView.findViewById(R.id.secondPointLocation);
                            distanceTextView.setText(waypoint.getDistance() + "M");
                        });
                        // Adding the marker
                        locationScene.mLocationMarkers.add(layoutLocationMarkerFirstPoint);
                    }

                    Frame frame = arSceneView.getArFrame();
                    if (frame == null) {
                        return;
                    }

                    if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                        return;
                    }

                    if (locationScene != null) {
                        locationScene.processFrame(frame);
                    }

                    if (loadingMessageSnackbar != null) {
                        for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
                            if (plane.getTrackingState() == TrackingState.TRACKING) {
                                hideLoadingMessage();
                            }
                        }
                    }
                });

        // Lastly request CAMERA & fine location permission which is required by ARCore-Location.
        ARLocationPermissionHelper.requestPermission(this);
    }

    private Node firstPointView() {
        Node base = new Node();
        base.setRenderable(firstPointRenderable);
        Context c = this;
        // Add  listeners etc here
        View eView = firstPointRenderable.getView();
        eView.setOnTouchListener((v, event) -> {
            if (currentWaypoint < databaseManager.waypoints.size() - 1) {
                double latitude = databaseManager.waypoints.get(currentWaypoint).getLatitude();
                double longitude = databaseManager.waypoints.get(currentWaypoint).getLongitude();
                locationScene.mLocationMarkers.get(0).latitude = latitude;
                locationScene.mLocationMarkers.get(0).longitude = longitude;
                Toast.makeText(
                        c, "Latitude: " + locationScene.mLocationMarkers.get(0).latitude + " Longitude: " + locationScene.mLocationMarkers.get(0).longitude + " Location: " + databaseManager.waypoints.get(currentWaypoint).getName(), Toast.LENGTH_LONG)
                        .show();
            }
            currentWaypoint++;
            return false;
        });
        return base;
    }

    private Node secondPointView() {
        Node base = new Node();
        base.setRenderable(secondPointRenderable);
        Context c = this;
        // Add  listeners etc here
        View eView = secondPointRenderable.getView();
        eView.setOnTouchListener((v, event) -> {
            Toast.makeText(
                    c, "Location marker touched.", Toast.LENGTH_LONG)
                    .show();
            return false;
        });
        return base;
    }

    private Node getAndy() {
        Node base = new Node();
        base.setRenderable(andyRenderable);
        Context c = this;
        base.setOnTapListener((v, event) -> {
            Toast.makeText(
                    c, "Andy touched.", Toast.LENGTH_LONG)
                    .show();
        });
        return base;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (locationScene != null) {
            locationScene.resume();
        }

        if (arSceneView.getSession() == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                Session session = DemoUtils.createArSession(this, installRequested);
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission(this);
                    return;
                } else {
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                DemoUtils.handleSessionException(this, e);
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            DemoUtils.displayError(this, "Unable to get camera", ex);
            finish();
            return;
        }

        if (arSceneView.getSession() != null) {
            showLoadingMessage();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (locationScene != null) {
            locationScene.pause();
        }

        arSceneView.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arSceneView.destroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (!ARLocationPermissionHelper.hasPermission(this)) {
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                ARLocationPermissionHelper.launchPermissionSettings(this);
            } else {
                Toast.makeText(
                        this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                        .show();
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void showLoadingMessage() {
        if (loadingMessageSnackbar != null && loadingMessageSnackbar.isShownOrQueued()) {
            return;
        }

        loadingMessageSnackbar =
                Snackbar.make(
                        MainActivity.this.findViewById(android.R.id.content),
                        R.string.plane_finding,
                        Snackbar.LENGTH_INDEFINITE);
        loadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
        loadingMessageSnackbar.show();
    }

    private void hideLoadingMessage() {
        if (loadingMessageSnackbar == null) {
            return;
        }

        loadingMessageSnackbar.dismiss();
        loadingMessageSnackbar = null;
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            String msg = "New Latitude: " + latitude + " New Longitude: " + longitude;
            //Toast.makeText(mContext,msg,Toast.LENGTH_LONG).show();
            System.out.println("New Latitude: " + latitude + " New Longitude: " + longitude);
            geoField = new GeomagneticField(
                    Double.valueOf(location.getLatitude()).floatValue(),
                    Double.valueOf(location.getLongitude()).floatValue(),
                    Double.valueOf(location.getAltitude()).floatValue(),
                    System.currentTimeMillis()
            );

            //new DatabaseManager().trash(locationManager,geoField);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    private void isLocationEnabled() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
            alertDialog.setTitle("Enable Location");
            alertDialog.setMessage("Your locations setting is not enabled. Please enabled it in settings menu.");
            alertDialog.setPositiveButton("Location Settings", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            });
            alertDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            AlertDialog alert = alertDialog.create();
            alert.show();
        } else {
//            AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
//            alertDialog.setTitle("Confirm Location");
//            alertDialog.setMessage("Your Location is enabled, please enjoy");
//            alertDialog.setNegativeButton("Back to interface", (dialog, which) -> dialog.cancel());
//            AlertDialog alert = alertDialog.create();
//            alert.show();
        }
    }
}
