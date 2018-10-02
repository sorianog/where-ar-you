package com.way.cloudnine.wherearyou.joe;

import java.util.List;

public class Node {
    private String building;
    private int floor;
    private double latitude;
    private double longitude;
    private String name;
    private List<String> connections;
    private String id;


    public Node() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public Node(String building, int floor, double latitude, double longitude, String name, List<String> connections, String id){
        this.building = building;
        this.floor = floor;
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.connections = connections;
        this.id = id;
    }

    public String getBuilding() {
        return building;
    }

    public String getId(){
        return id;
    }

    public int getFloor() {
        return floor;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String setName() {
        return name;
    }

    public List<String> getConnections() {
        return connections;
    }

}
