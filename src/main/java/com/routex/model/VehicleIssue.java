package com.routex.model;
import java.time.LocalDateTime;

/**
 * Captures a defect or breakdown reported by a Field Driver (UC10).
 * On creation, the associated Vehicle is transitioned to MAINTENANCE_HOLD.
 */
public class VehicleIssue {

    private String        issueId;
    private String        vehicleId;
    private String        vehicleLicensePlate;  // Denormalised
    private String        driverId;
    private String        driverName;            // Denormalised
    private String        description;
    private String        category;
    private String        gpsLocation;
    private LocalDateTime timestamp;
    private boolean       isResolved;

    public VehicleIssue() {}

    public VehicleIssue(String issueId, String vehicleId, String driverId,
                        String description, String category, String gpsLocation) {
        this.issueId     = issueId;
        this.vehicleId   = vehicleId;
        this.driverId    = driverId;
        this.description = description;
        this.category    = category;
        this.gpsLocation = gpsLocation;
        this.timestamp   = LocalDateTime.now();
        this.isResolved  = false;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getIssueId()               { return issueId; }
    public void   setIssueId(String issueId) { this.issueId = issueId; }

    public String getVehicleId()                  { return vehicleId; }
    public void   setVehicleId(String vehicleId)  { this.vehicleId = vehicleId; }

    public String getVehicleLicensePlate()                           { return vehicleLicensePlate; }
    public void   setVehicleLicensePlate(String vehicleLicensePlate){ this.vehicleLicensePlate = vehicleLicensePlate; }

    public String getDriverId()                  { return driverId; }
    public void   setDriverId(String driverId)   { this.driverId = driverId; }

    public String getDriverName()                    { return driverName; }
    public void   setDriverName(String driverName)   { this.driverName = driverName; }

    public String getDescription()                       { return description; }
    public void   setDescription(String description)     { this.description = description; }

    public String getCategory()                  { return category; }
    public void   setCategory(String category)   { this.category = category; }

    public String getGpsLocation()                   { return gpsLocation; }
    public void   setGpsLocation(String gpsLocation) { this.gpsLocation = gpsLocation; }

    public LocalDateTime getTimestamp()                { return timestamp; }
    public void          setTimestamp(LocalDateTime t) { this.timestamp = t; }

    public boolean isResolved()                { return isResolved; }
    public void    setResolved(boolean resolved){ this.isResolved = resolved; }
}