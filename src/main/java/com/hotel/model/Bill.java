// File: src/main/java/com/hotel/model/Bill.java
package com.hotel.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bill / Invoice entity.
 * invoiceId  – timestamp-based unique identifier.
 * dateTime   – moment the bill was generated.
 */
public class Bill {

    private String        invoiceId;
    private LocalDateTime dateTime;
    private int           roomNumber;
    private String        customerName;
    private int           days;
    private double        pricePerDay;
    private double        totalAmount;

    public Bill(int roomNumber, String customerName, int days, double pricePerDay) {
        this.invoiceId    = "INV" + System.currentTimeMillis();
        this.dateTime     = LocalDateTime.now();
        this.roomNumber   = roomNumber;
        this.customerName = customerName;
        this.days         = days;
        this.pricePerDay  = pricePerDay;
        this.totalAmount  = days * pricePerDay;
    }

    // ── Formatted invoice text ────────────────────────────────────────────────────

    public String getSummary() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy  HH:mm");
        return  "========================================\n"
              + "           GRAND HOTEL\n"
              + "              INVOICE\n"
              + "========================================\n"
              + "Invoice ID   : " + invoiceId               + "\n"
              + "Date & Time  : " + dateTime.format(fmt)    + "\n"
              + "----------------------------------------\n"
              + "Room No      : " + roomNumber              + "\n"
              + "Customer     : " + customerName            + "\n"
              + "Days Stayed  : " + days                    + "\n"
              + "Rate / Day   : Rs. " + String.format("%.2f", pricePerDay) + "\n"
              + "----------------------------------------\n"
              + "TOTAL AMOUNT : Rs. " + String.format("%.2f", totalAmount) + "\n"
              + "========================================\n"
              + "    Thank you for staying with us.\n"
              + "========================================";
    }

    // ── Getters ───────────────────────────────────────────────────────────────────

    public String        getInvoiceId()    { return invoiceId; }
    public LocalDateTime getDateTime()     { return dateTime; }
    public int           getRoomNumber()   { return roomNumber; }
    public String        getCustomerName() { return customerName; }
    public int           getDays()         { return days; }
    public double        getPricePerDay()  { return pricePerDay; }
    public double        getTotalAmount()  { return totalAmount; }

    // ── File serialisation ────────────────────────────────────────────────────────
    // Format: invoiceId,roomNumber,customerName,days,pricePerDay,totalAmount,dateTime

    @Override
    public String toString() {
        return invoiceId + "," + roomNumber + "," + customerName + ","
               + days + "," + pricePerDay + "," + totalAmount + ","
               + dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
