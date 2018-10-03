package com.way.cloudnine.wherearyou.repositories;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.way.cloudnine.wherearyou.models.Waypoint;

import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference locationsReference;
    private List<Waypoint> waypoints = new ArrayList<>();

    public DatabaseManager() {
        setupDatabase();
    }

    public void setupDatabase() {
        firebaseDatabase = FirebaseDatabase.getInstance();
        locationsReference = firebaseDatabase.getReference("locations");

        locationsReference.addValueEventListener(new ValueEventListener() {
            @Override
            // Called whenever data changes
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    waypoints.add(child.getValue(Waypoint.class));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Database Error");
            }
        });
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public Waypoint getWaypointById(String id) {
        for (Waypoint waypoint : waypoints) {
            if (waypoint.getId().equals(id)) {
                return waypoint;
            }
        }
        return null;
    }
}
