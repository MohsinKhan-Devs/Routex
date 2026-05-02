package com.routex.model;

import com.routex.enums.VehicleStatus;

public class Vehicle {

    private String vehicleId;
    private String licensePlate;
    private int capacity;
    private VehicleStatus status;
    private String currentLocation;
    private int mileage;

    public Vehicle() {
    }

    public Vehicle(String vehicleId, String licensePlate, int capacity,
                   VehicleStatus status, String currentLocation, int mileage) {
        this.vehicleId = vehicleId;
        this.licensePlate = licensePlate;
        this.capacity = capacity;
        this.status = status;
        this.currentLocation = currentLocation;
        this.mileage = mileage;
    }

    public boolean isEligibleForAssignment() {
        return status == VehicleStatus.AVAILABLE;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
    }

    @Override
    public String toString() {
        return licensePlate + " (" + status + ")";
    }
}
