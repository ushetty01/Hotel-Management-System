// File: src/main/java/com/hotel/service/HotelService.java
package com.hotel.service;

import com.hotel.model.Bill;
import com.hotel.model.Customer;
import com.hotel.model.Room;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class HotelService {

    private final List<Room>  rooms       = new ArrayList<>();
    private final FileService fileService = new FileService();

    public HotelService() {
        List<Room> loaded = fileService.loadRooms();
        if (!loaded.isEmpty()) {
            rooms.addAll(loaded);
        } else {
            rooms.add(new Room(101, "Single", 1200));
            rooms.add(new Room(102, "Double", 2000));
            rooms.add(new Room(103, "Suite",  4500));
            rooms.add(new Room(104, "Deluxe", 3200));
            saveRooms();
        }
    }

    // ── Room Management ───────────────────────────────────────────────────────────

    public boolean addRoom(int roomNumber, String type, double price) {
        for (Room r : rooms) {
            if (r.getRoomNumber() == roomNumber) return false;
        }
        rooms.add(new Room(roomNumber, type, price));
        saveRooms();
        return true;
    }

    public boolean editRoom(int roomNumber, String newType, double newPrice) {
        Room room = findRoom(roomNumber);
        if (room == null) return false;
        room.setType(newType);
        room.setPrice(newPrice);
        saveRooms();
        return true;
    }

    public List<Room> getAllRooms()        { return new ArrayList<>(rooms); }

    public List<Room> getAvailableRooms() {
        return rooms.stream()
                    .filter(Room::isAvailable)
                    .collect(Collectors.toList());
    }

    public Room findRoom(int roomNumber) {
        return rooms.stream()
                    .filter(r -> r.getRoomNumber() == roomNumber)
                    .findFirst().orElse(null);
    }

    // ── Booking ───────────────────────────────────────────────────────────────────

    public boolean bookRoom(int roomNumber, Customer customer) {
        Room room = findRoom(roomNumber);
        if (room == null || !room.isAvailable()) return false;
        room.setStatus(Room.STATUS_BOOKED);
        room.setAssignedCustomer(customer);
        room.setCheckInTime(LocalDateTime.now());
        saveRooms();
        fileService.saveBookingRecord(room, customer);
        return true;
    }

    // ── Checkout ──────────────────────────────────────────────────────────────────

    public Bill checkoutRoom(int roomNumber, int days) {
        Room room = findRoom(roomNumber);
        if (room == null || room.isAvailable()) return null;
        Customer customer = room.getAssignedCustomer();
        Bill bill = new Bill(
            roomNumber,
            customer != null ? customer.getName() : "Unknown",
            days,
            room.getPrice()
        );
        room.setStatus(Room.STATUS_CLEANING);
        room.setAssignedCustomer(null);
        room.setCheckInTime(null);
        saveRooms();
        fileService.saveBill(bill);
        return bill;
    }

    public boolean markRoomAvailable(int roomNumber) {
        Room room = findRoom(roomNumber);
        if (room == null || !Room.STATUS_CLEANING.equals(room.getStatus())) return false;
        room.setStatus(Room.STATUS_AVAILABLE);
        saveRooms();
        return true;
    }

    // ── Statistics ────────────────────────────────────────────────────────────────

    public int[] getStatistics() {
        int total     = rooms.size();
        int booked    = (int) rooms.stream().filter(r -> Room.STATUS_BOOKED.equals(r.getStatus())).count();
        int available = (int) rooms.stream().filter(r -> Room.STATUS_AVAILABLE.equals(r.getStatus())).count();
        int occupancy = total == 0 ? 0 : (booked * 100 / total);
        return new int[]{total, booked, available, occupancy};
    }

    public Map<String, String> getEnhancedStats() {
        int total    = rooms.size();
        int booked   = (int) rooms.stream().filter(r -> Room.STATUS_BOOKED.equals(r.getStatus())).count();
        int avail    = (int) rooms.stream().filter(r -> Room.STATUS_AVAILABLE.equals(r.getStatus())).count();
        int cleaning = (int) rooms.stream().filter(r -> Room.STATUS_CLEANING.equals(r.getStatus())).count();
        int occupancy = total == 0 ? 0 : (booked * 100 / total);

        double revenue       = fileService.calculateTotalRevenue();
        int    todayBookings = fileService.getTodayBookingCount();
        String popular       = fileService.getMostBookedRoomType();

        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("Total Rooms",       String.valueOf(total));
        stats.put("Booked",            String.valueOf(booked));
        stats.put("Available",         String.valueOf(avail));
        stats.put("Cleaning",          String.valueOf(cleaning));
        stats.put("Occupancy",         occupancy + "%");
        stats.put("Total Revenue",     "Rs. " + String.format("%.2f", revenue));
        stats.put("Today's Bookings",  String.valueOf(todayBookings));
        stats.put("Most Popular Type", popular);
        return stats;
    }

    // ── Reset Statistics ──────────────────────────────────────────────────────────

    /**
     * Clears bills.txt and bookings.txt.
     * Rooms and their current status are NOT affected.
     */
    public void resetStatistics() {
        fileService.resetStatistics();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private void saveRooms() { fileService.saveRooms(rooms); }

    public FileService getFileService() { return fileService; }
}
