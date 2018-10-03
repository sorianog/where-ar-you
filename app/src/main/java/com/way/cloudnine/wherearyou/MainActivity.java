package com.way.cloudnine.wherearyou;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
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
import com.way.cloudnine.wherearyou.models.Waypoint;
import com.way.cloudnine.wherearyou.repositories.WaypointRepository;
import com.way.cloudnine.wherearyou.utils.ArHelper;

import java.util.List;
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
    private WaypointRepository waypointRepository = new WaypointRepository();
    private int currentWaypoint = 1;
    private TextView locationDetails, distanceDetails;

    @Override
    // Called when the activity is created
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the view to the following activity
        setContentView(R.layout.activity_sceneform);
        locationDetails = findViewById(R.id.locationDetails);
        distanceDetails = findViewById(R.id.distanceDetails);
        locationDetails.setText("");
        distanceDetails.setText("");

        arSceneView = findViewById(R.id.ar_scene_view);

        // Build a renderable from a 2D View.
        CompletableFuture<ViewRenderable> firstPoint =
                ViewRenderable.builder()
                        .setView(this, R.layout.activity_main)
                        .build();

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().

        CompletableFuture
                .allOf(firstPoint)
                .handle((notUsed, throwable) -> {
                    // When you build a Renderable, Sceneform loads its resources in the background while
                    // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                    // before calling get().

                    if (throwable != null) {
                        ArHelper.displayError(this, "Unable to load renderables", throwable);
                        return null;
                    }

                    try {
                        firstPointRenderable = firstPoint.get();
                        finishedLoading = true;

                    } catch (InterruptedException | ExecutionException ex) {
                        ArHelper.displayError(this, "Unable to load renderables", ex);
                    }

                    return null;
                });

        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        arSceneView.getScene().addOnUpdateListener(frameTime -> {
            if (!finishedLoading) {
                return;
            }

            if (locationScene == null) {
                locationScene = new LocationScene(this, this, arSceneView);

                Waypoint firstWaypoint = waypointRepository.getWaypointById("1");
                LocationMarker layoutLocationMarkerFirstPoint = new LocationMarker(
                        firstWaypoint.getLongitude(),
                        firstWaypoint.getLatitude(),
                        firstPointView()
                );

                // An example "onRender" event, called every frame
                // Updates the layout with the markers distance
                layoutLocationMarkerFirstPoint.setRenderEvent(waypoint -> {
                    String way = waypointRepository.getWaypoints().get(currentWaypoint).getName()

                    String location = waypointRepository.getWaypoints().get(currentWaypoint).getName();
                    locationDetails.setText(location);

                    String distance = waypoint.getDistance() + "M";
                    distanceDetails.setText(distance);
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

    @SuppressLint("ClickableViewAccessibility")
    private Node firstPointView() {
        Node base = new Node();
        base.setRenderable(firstPointRenderable);
        Context c = this;
        // Add  listeners etc here
        View eView = firstPointRenderable.getView();
        eView.setOnTouchListener((v, event) -> {
            List<Waypoint> waypoints = waypointRepository.getWaypoints();
            if (currentWaypoint < waypoints.size() - 1) {
                double latitude = waypoints.get(currentWaypoint).getLatitude();
                double longitude = waypoints.get(currentWaypoint).getLongitude();
                locationScene.mLocationMarkers.get(0).latitude = latitude;
                locationScene.mLocationMarkers.get(0).longitude = longitude;
                Toast.makeText(
                        c, "Latitude: " + locationScene.mLocationMarkers.get(0).latitude + " Longitude: " + locationScene.mLocationMarkers.get(0).longitude + " Location: " + waypoints.get(currentWaypoint).getName(), Toast.LENGTH_LONG)
                        .show();
            }
            currentWaypoint++;
            return false;
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
                Session session = ArHelper.createArSession(this, installRequested);
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission(this);
                    return;
                } else {
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                ArHelper.handleSessionException(this, e);
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            ArHelper.displayError(this, "Unable to get camera", ex);
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
}
