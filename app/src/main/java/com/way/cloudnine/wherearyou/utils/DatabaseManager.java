package com.way.cloudnine.wherearyou.utils;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.ImageView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatabaseManager {
    public List<Waypoint> waypoints = new ArrayList<>();
    public List<Waypoint> shortestPath = new ArrayList<>();
    public Waypoint nextWaypoint = new Waypoint();
    LocationListener locationListener;
    private ImageView arrowView;

    public void callDatabase() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference locationsReference = firebaseDatabase.getReference("locations");

        locationsReference.addValueEventListener(new ValueEventListener() {
            @Override
            // Called whenever data changes
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    waypoints.add(child.getValue(Waypoint.class));
                }
                System.out.println(waypoints.get(0).getBuilding());
                //getSortedShortestPath(waypoints);
                //System.out.println("Feet:" + getDistance(waypoints,"1","2"));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void trash(LocationManager locationManager, GeomagneticField geoField) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("locations");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    waypoints.add(child.getValue(Waypoint.class));
                }
                System.out.println(waypoints.get(0).getBuilding());
                doStuff(locationManager, geoField);
                //getSortedShortestPath(waypoints);
                //System.out.println("Feet:" + getDistance(waypoints,"1","2"));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void changeDirection(String id) {
        if ((getCurrentLocationLatitude() >= shortestPath.get(Integer.parseInt(id)).getLatitude() - .00100 && getCurrentLocationLatitude() <= shortestPath.get(Integer.parseInt(id)).getLatitude() + .00100) &&
                (getCurrentLocationLongitude() >= shortestPath.get(Integer.parseInt(id)).getLongitude() - .00100 && getCurrentLocationLongitude() <= shortestPath.get(Integer.parseInt(id)).getLatitude() + .00100)) {
            //Turn Arrow
        }
    }

    private double getCurrentLocationLatitude() {
        return 0.0;
    }

    private double getCurrentLocationLongitude() {
        return 0.0;
    }


    public Waypoint getWaypointById(int id) {
        for (Waypoint waypoint : waypoints) {

            if (waypoint.getId().equals(Integer.toString(id))) {
                return waypoint;
            }
        }
        return null;
    }


    public void getShortestPath() {
        for (int i = 0; i < waypoints.size(); i++) {
            shortestPath.add(waypoints.get(i));
        }
    }

    public double getDistance(List<Waypoint> list, String firstNode, String secondNode) {
        double R = 6372.8; // In kilometers
        double kmToFeet = 3280.84;
        double lat1 = list.get(Integer.parseInt(firstNode)).getLatitude();
        double lat2 = list.get(Integer.parseInt(secondNode)).getLatitude();
        double lon1 = list.get(Integer.parseInt(firstNode)).getLongitude();
        double lon2 = list.get(Integer.parseInt(secondNode)).getLongitude();


        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double d = R * c * kmToFeet;
        return d;
    }

    public List<Double> getSortedShortestPath(List<Waypoint> list) {
        List<Double> path = new ArrayList<>();

        for (int i = 0; i < list.size() - 1; i++) {
            for (int j = i + 1; j < list.size(); j++) {
                path.add(getDistance(list, Integer.toString(i), Integer.toString(j)));
            }
        }
        Collections.sort(path);
        return path;
    }

    public List<String> findPath(List<Waypoint> list) {
        List<String> path = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j < list.get(i).getConnections().size(); j++) {
                if (!path.contains(list.get(i).getConnections().get(j))) {
                    path.add(list.get(i).getConnections().get(j));
                }
            }
        }
        return path;
    }

    public List<String> getNextConnectingNodes(List<Waypoint> list, String currentNode) {
        List<String> connectingNode = new ArrayList<>();
        for (int i = 0; i < list.get(Integer.parseInt(currentNode)).getConnections().size(); i++) {
            connectingNode.add(list.get(Integer.parseInt(currentNode)).getConnections().get(i));
        }
        return connectingNode;
    }

    public double[] findCoordinatesOfNextConnectingNode(List<Waypoint> list, String currentNode) {
        double coordinates[] = new double[2];

        String nextNode = list.get(Integer.parseInt(currentNode)).getConnections().get(0);
        double latitude = list.get(Integer.parseInt(nextNode)).getLatitude();
        double longitude = list.get(Integer.parseInt(nextNode)).getLongitude();
        coordinates[0] = latitude;
        coordinates[1] = longitude;
        return coordinates;
    }

    private void readFromDatabase(DatabaseReference myRef) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef2 = database.getReference("message2");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                myRef2.setValue(value);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    public void doStuff(LocationManager locationManager, GeomagneticField geoField) {

        @SuppressLint("MissingPermission") Location startingLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        Location endingLocation = new Location("ending point");

        Waypoint nextWaypoint = getWaypointById(2);
        endingLocation.setLatitude(nextWaypoint.getLatitude());
        endingLocation.setLongitude(nextWaypoint.getLongitude());

        float targetBearing = startingLocation.bearingTo(endingLocation);

        System.out.println(targetBearing);

        float heading = geoField.getDeclination();
        heading = targetBearing - (targetBearing + heading);
        //rotateArrow(heading);
        int round = Math.round(-heading / 360 + 180);
    }

    private void rotateArrow(float angle) {

        //arrowView.findViewById(R.id.arrowView);
        Matrix matrix = new Matrix();
        arrowView.setScaleType(ImageView.ScaleType.MATRIX);
        matrix.postRotate(angle, arrowView.getDrawable().getIntrinsicWidth(), arrowView.getDrawable().getIntrinsicHeight());
        arrowView.setImageMatrix(matrix);

    }
}
