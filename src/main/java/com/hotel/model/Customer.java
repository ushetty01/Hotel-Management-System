// File: src/main/java/com/hotel/model/Customer.java
package com.hotel.model;

public class Customer {

    private String name;
    private String phone;

    public Customer(String name, String phone) {
        this.name  = name;
        this.phone = phone;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────────

    public String getName()         { return name; }
    public void setName(String n)   { this.name = n; }

    public String getPhone()        { return phone; }
    public void setPhone(String p)  { this.phone = p; }

    @Override
    public String toString() {
        return name + " (" + phone + ")";
    }
}
