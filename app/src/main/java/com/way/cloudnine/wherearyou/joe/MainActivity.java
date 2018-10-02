package com.way.cloudnine.wherearyou.joe;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.way.cloudnine.wherearyou.R;

import org.jetbrains.annotations.Nullable;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    LocationManager locationManager;
    Context mContext;
    GeomagneticField geoField;
    private ImageView arrowView;
    public TextView arrowText;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        //new DatabaseManager().Trash();

        mContext = this;
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        boolean permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (permissionGranted) {
            // {Some Code}
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    2000, 10, locationListenerGPS);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
        }




        //isLocationEnabled();


    }
    private void rotateArrow(float angle){

        //arrowView = findViewById(R.id.arrowView);
        Matrix  matrix = new Matrix();
        arrowView.setScaleType(ImageView.ScaleType.MATRIX);
        matrix.postRotate(angle, arrowView.getDrawable().getIntrinsicWidth(),arrowView.getDrawable().getIntrinsicHeight());
        arrowView.setImageMatrix(matrix);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 200: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // {Some Code}
                }
            }
        }
    }



    LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            String msg = "New Latitude: " +latitude + " New Longitude: " + longitude;
            //Toast.makeText(mContext,msg,Toast.LENGTH_LONG).show();
            System.out.println("New Latitude: " +latitude + " New Longitude: " + longitude);
            geoField = new GeomagneticField(
                    Double.valueOf(location.getLatitude()).floatValue(),
                    Double.valueOf(location.getLongitude()).floatValue(),
                    Double.valueOf(location.getAltitude()).floatValue(),
                    System.currentTimeMillis()
            );

            new DatabaseManager().Trash(locationManager,geoField);
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

    protected void onResume(){
        super.onResume();
        isLocationEnabled();

    }

    public void DoStuff(){
        @SuppressLint("MissingPermission") Location startingLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


        Location endingLocation = new Location("ending point");
        endingLocation.setLatitude(41.548053);
        endingLocation.setLongitude(-81.442706);

        float targetBearing = startingLocation.bearingTo(endingLocation);

        System.out.println(targetBearing);

        float heading = geoField.getDeclination();
        heading = targetBearing - (targetBearing + heading);
        rotateArrow(heading);
        int round = Math.round(-heading / 360 + 180);
    }

    private void isLocationEnabled(){
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            AlertDialog.Builder alertDialog=new AlertDialog.Builder(mContext);
            alertDialog.setTitle("Enable Location");
            alertDialog.setMessage("Your locations setting is not enabled. Please enabled it in settings menu.");
            alertDialog.setPositiveButton("Location Settings", (dialog, which) -> {
                Intent intent=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            });
            alertDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            AlertDialog alert=alertDialog.create();
            alert.show();


        }
        else{
            AlertDialog.Builder alertDialog=new AlertDialog.Builder(mContext);
            alertDialog.setTitle("Confirm Location");
            alertDialog.setMessage("Your Location is enabled, please enjoy");
            alertDialog.setNegativeButton("Back to interface", (dialog, which) -> dialog.cancel());
            AlertDialog alert=alertDialog.create();
            //alert.show();
        }
    }
}
