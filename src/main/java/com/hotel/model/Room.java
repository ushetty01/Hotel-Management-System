// File: src/main/java/com/hotel/model/Room.java
package com.hotel.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Room entity.
 * status field replaces the old boolean available.
 * Possible values: "Available" | "Booked" | "Cleaning"
 */
public class Room {

    // Status constants – use these everywhere instead of raw strings
    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_BOOKED    = "Booked";
    public static final String STATUS_CLEANING  = "Cleaning";

    private int           roomNumber;
    private String        type;
    private double        price;
    private String        status;           // replaces boolean available
    private Customer      assignedCustomer;
    private LocalDateTime checkInTime;      // recorded when a booking is made

    public Room(int roomNumber, String type, double price) {
        this.roomNumber = roomNumber;
        this.type       = type;
        this.price      = price;
        this.status     = STATUS_AVAILABLE;
    }

    // ── Convenience helpers ───────────────────────────────────────────────────────

    /** True only when status is "Available". */
    public boolean isAvailable() {
        return STATUS_AVAILABLE.equals(status);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public int    getRoomNumber()             { return roomNumber; }
    public void   setRoomNumber(int n)        { this.roomNumber = n; }

    public String getType()                   { return type; }
    public void   setType(String t)           { this.type = t; }

    public double getPrice()                  { return price; }
    public void   setPrice(double p)          { this.price = p; }

    public String getStatus()                 { return status; }
    public void   setStatus(String s)         { this.status = s; }

    public Customer getAssignedCustomer()     { return assignedCustomer; }
    public void     setAssignedCustomer(Customer c) { this.assignedCustomer = c; }

    public LocalDateTime getCheckInTime()     { return checkInTime; }
    public void          setCheckInTime(LocalDateTime t) { this.checkInTime = t; }

    // ── File serialisation ────────────────────────────────────────────────────────
    // Format: roomNumber,type,price,status,checkInTime,customerName,customerPhone

    @Override
    public String toString() {
        String ci    = (checkInTime != null)
                       ? checkInTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                       : "";
        String cName = (assignedCustomer != null) ? assignedCustomer.getName()  : "";
        String cPh   = (assignedCustomer != null) ? assignedCustomer.getPhone() : "";
        return roomNumber + "," + type + "," + price + "," + status
               + "," + ci + "," + cName + "," + cPh;
    }
}
