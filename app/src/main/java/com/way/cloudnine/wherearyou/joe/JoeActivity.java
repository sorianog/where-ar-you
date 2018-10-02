package com.way.cloudnine.wherearyou.joe;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.ArrayMap;
import android.widget.ImageView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.way.cloudnine.wherearyou.R;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JoeActivity {
    public List<Node> list = new ArrayList<>();
    public List<Node> shortestPathList = new ArrayList<>();
    public Node nextNode = new Node();
    LocationListener locationListener;
    private ImageView arrowView;

    public void Trash(LocationManager locationManager, GeomagneticField geoField) {


        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("locations");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot child: dataSnapshot.getChildren()) {
                    list.add(child.getValue(Node.class));
                }
                System.out.println(list.get(0).getBuilding());
                DoStuff(locationManager, geoField);
                //GetSortedShortestPath(list);
                //System.out.println("Feet:" + GetDistance(list,"1","2"));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });



    }

    public void CallDatabase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("locations");
        myRef.child("10000/name").setValue("message");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot child: dataSnapshot.getChildren()) {
                    list.add(child.getValue(Node.class));
                }
                System.out.println(list.get(0).getBuilding());
                //GetSortedShortestPath(list);
                //System.out.println("Feet:" + GetDistance(list,"1","2"));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }



    /*
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            geoField = new GeomagneticField(
              Double.valueOf(location.getLatitude()).floatValue(),
                    Double.valueOf(location.getLongitude()).floatValue(),
                    Double.valueOf(location.getAltitude()).floatValue(),
                    System.currentTimeMillis()
            );
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
    };*/


    public void ChangeDirection(String id){
        if((GetCurrentLocationLatitude() >= shortestPathList.get(Integer.parseInt(id)).getLatitude() - .00100  && GetCurrentLocationLatitude() <=shortestPathList.get(Integer.parseInt(id)).getLatitude() + .00100 ) &&
                (GetCurrentLocationLongitude() >= shortestPathList.get(Integer.parseInt(id)).getLongitude() - .00100  && GetCurrentLocationLongitude() <=shortestPathList.get(Integer.parseInt(id)).getLatitude() + .00100) ) {
            //Turn Arrow
        }
    }

    private double GetCurrentLocationLatitude() {
        return 0.0;
    }

    private double GetCurrentLocationLongitude() {
        return 0.0;
    }


    public Node GetNodeById(int id){
        for (Node node: list
             ) {
            if(node.getId().equals(Integer.toString(id))){
                return node;
            }
        }
        return null;
    }



    public void GetShortesPathOfNodes(){
        for(int i = 0; i < list.size(); i++){
            shortestPathList.add(list.get(i));
        }
    }

    public double GetDistance(List<Node> list, String firstNode, String secondNode){
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

        double a = Math.pow(Math.sin(dLat / 2),2) + Math.pow(Math.sin(dLon / 2),2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double d =  R * c * kmToFeet;
        return d;
    }

    public List<Double> GetSortedShortestPath(List<Node> list){
        List<Double> path = new ArrayList<>();

        for(int i =0; i < list.size() - 1; i++){
            for (int j = i + 1; j < list.size() ;j++){
                path.add(GetDistance(list, Integer.toString(i),Integer.toString(j)));
            }
        }
        Collections.sort(path);
        return  path;
    }


    public List<String> FindPath(List<Node> list){
        List<String> path = new ArrayList<>();

        for (int i = 0; i < list.size(); i++){
            for (int j = 0; j < list.get(i).getConnections().size(); j++){
                if(!path.contains(list.get(i).getConnections().get(j))) {
                    path.add(list.get(i).getConnections().get(j));
                }
            }
        }
        return path;
    }

    public List<String> GetNextConnectingNodes(List<Node> list, String currentNode){
        List<String> connectingNode = new ArrayList<>();
        for (int i = 0; i < list.get(Integer.parseInt(currentNode)).getConnections().size(); i++){
            connectingNode.add(list.get(Integer.parseInt(currentNode)).getConnections().get(i));
        }
        return connectingNode;
    }
    
    public double[] FindCoordinatesOfNextConnectingNode(List<Node> list, String currentNode){
        double coordinates[] = new double[2];

        String nextNode = list.get(Integer.parseInt(currentNode)).getConnections().get(0);
        double latitude = list.get(Integer.parseInt(nextNode)).getLatitude();
        double longitude = list.get(Integer.parseInt(nextNode)).getLongitude();
        coordinates[0] = latitude;
        coordinates[1] = longitude;
        return coordinates;
    }

    private void ReadFromDatabase(DatabaseReference myRef){
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

    public void DoStuff(LocationManager locationManager, GeomagneticField geoField){

        @SuppressLint("MissingPermission") Location startingLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        Location endingLocation = new Location("ending point");

        Node nextNode = GetNodeById(2);
        endingLocation.setLatitude(nextNode.getLatitude());
        endingLocation.setLongitude(nextNode.getLongitude());

        float targetBearing = startingLocation.bearingTo(endingLocation);

        System.out.println(targetBearing);

        float heading = geoField.getDeclination();
        heading = targetBearing - (targetBearing + heading);
        //rotateArrow(heading);
        int round = Math.round(-heading / 360 + 180);
    }

    private void rotateArrow(float angle){

        //arrowView.findViewById(R.id.arrowView);
        Matrix matrix = new Matrix();
        arrowView.setScaleType(ImageView.ScaleType.MATRIX);
        matrix.postRotate(angle, arrowView.getDrawable().getIntrinsicWidth(),arrowView.getDrawable().getIntrinsicHeight());
        arrowView.setImageMatrix(matrix);

    }
}
