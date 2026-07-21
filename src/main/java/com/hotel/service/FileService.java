// File: src/main/java/com/hotel/service/FileService.java
package com.hotel.service;

import com.hotel.model.Bill;
import com.hotel.model.Customer;
import com.hotel.model.Room;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileService {

    private static final String ROOMS_FILE    = "rooms.txt";
    private static final String BILLS_FILE    = "bills.txt";
    private static final String BOOKINGS_FILE = "bookings.txt";

    // ── Rooms ─────────────────────────────────────────────────────────────────────

    public void saveRooms(List<Room> rooms) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(ROOMS_FILE))) {
            for (Room r : rooms) { w.write(r.toString()); w.newLine(); }
        } catch (IOException e) {
            System.err.println("Error saving rooms: " + e.getMessage());
        }
    }

    public List<Room> loadRooms() {
        List<Room> rooms = new ArrayList<>();
        File file = new File(ROOMS_FILE);
        if (!file.exists()) return rooms;

        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(",", -1);
                if (p.length < 4) continue;

                int    roomNo = Integer.parseInt(p[0].trim());
                String type   = p[1].trim();
                double price  = Double.parseDouble(p[2].trim());

                String statusRaw = p[3].trim();
                String status;
                if      ("true".equalsIgnoreCase(statusRaw))  status = Room.STATUS_AVAILABLE;
                else if ("false".equalsIgnoreCase(statusRaw)) status = Room.STATUS_BOOKED;
                else                                          status = statusRaw;

                Room room = new Room(roomNo, type, price);
                room.setStatus(status);

                if (p.length > 4 && !p[4].trim().isEmpty()) {
                    try {
                        room.setCheckInTime(LocalDateTime.parse(p[4].trim(),
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    } catch (Exception ignored) {}
                }
                if (p.length > 6 && !p[5].trim().isEmpty() && !p[6].trim().isEmpty()) {
                    room.setAssignedCustomer(new Customer(p[5].trim(), p[6].trim()));
                }
                rooms.add(room);
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading rooms: " + e.getMessage());
        }
        return rooms;
    }

    // ── Bills ─────────────────────────────────────────────────────────────────────

    public void saveBill(Bill bill) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(BILLS_FILE, true))) {
            w.write(bill.toString()); w.newLine();
        } catch (IOException e) {
            System.err.println("Error saving bill: " + e.getMessage());
        }
    }

    public double calculateTotalRevenue() {
        double total = 0;
        File file = new File(BILLS_FILE);
        if (!file.exists()) return total;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(",", -1);
                if (p.length >= 6) {
                    try { total += Double.parseDouble(p[5].trim()); }
                    catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading bills: " + e.getMessage());
        }
        return total;
    }

    public int getTodayBookingCount() {
        int count = 0;
        File file = new File(BILLS_FILE);
        if (!file.exists()) return count;
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(",", -1);
                if (p.length >= 7 && p[6].trim().startsWith(today)) count++;
            }
        } catch (IOException e) {
            System.err.println("Error reading bills: " + e.getMessage());
        }
        return count;
    }

    // ── Booking History ───────────────────────────────────────────────────────────

    public void saveBookingRecord(Room room, Customer customer) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(BOOKINGS_FILE, true))) {
            String dt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            w.write(dt + "," + room.getRoomNumber() + "," + room.getType()
                    + "," + customer.getName() + "," + customer.getPhone());
            w.newLine();
        } catch (IOException e) {
            System.err.println("Error saving booking record: " + e.getMessage());
        }
    }

    public List<String> loadRecentBookings(int limit) {
        List<String> lines = new ArrayList<>();
        File file = new File(BOOKINGS_FILE);
        if (!file.exists()) return lines;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error loading bookings: " + e.getMessage());
        }
        int from = Math.max(0, lines.size() - limit);
        return lines.subList(from, lines.size());
    }

    public String getMostBookedRoomType() {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        File file = new File(BOOKINGS_FILE);
        if (!file.exists()) return "N/A";
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length >= 3) counts.merge(p[2].trim(), 1, Integer::sum);
            }
        } catch (IOException e) {
            System.err.println("Error reading bookings: " + e.getMessage());
        }
        return counts.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("N/A");
    }

    // ── Reset Statistics ──────────────────────────────────────────────────────────

    /**
     * Clears bills.txt and bookings.txt completely.
     * Rooms are NOT touched.
     */
    public void resetStatistics() {
        clearFile(BILLS_FILE);
        clearFile(BOOKINGS_FILE);
    }

    /** Overwrite a file with empty content (effectively clears it). */
    private void clearFile(String path) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(path, false))) {
            // Writing nothing – file is now empty
        } catch (IOException e) {
            System.err.println("Error clearing " + path + ": " + e.getMessage());
        }
    }

    // ── PDF Bill ──────────────────────────────────────────────────────────────────

    public String generateBillPDF(Bill bill) {
        String fileName = "bill_" + bill.getRoomNumber() + ".pdf";
        try {
            PdfWriter   writer   = new PdfWriter(fileName);
            PdfDocument pdfDoc   = new PdfDocument(writer);
            Document    document = new Document(pdfDoc, PageSize.A5);
            document.setMargins(36, 36, 36, 36);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

            document.add(new Paragraph("GRAND HOTEL")
                .setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("INVOICE")
                .setFontSize(14).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

            Table table = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                              .useAllAvailableWidth().setMarginTop(10);

            addRow(table, "Invoice ID",  bill.getInvoiceId());
            addRow(table, "Date & Time", bill.getDateTime().format(fmt));
            addRow(table, "Room No",     String.valueOf(bill.getRoomNumber()));
            addRow(table, "Customer",    bill.getCustomerName());
            addRow(table, "Days Stayed", String.valueOf(bill.getDays()));
            addRow(table, "Rate / Day",  "Rs. " + String.format("%.2f", bill.getPricePerDay()));

            Cell lc = new Cell().add(new Paragraph("TOTAL AMOUNT").setBold())
                                .setBackgroundColor(ColorConstants.LIGHT_GRAY);
            Cell vc = new Cell()
                    .add(new Paragraph("Rs. " + String.format("%.2f", bill.getTotalAmount())).setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY);
            table.addCell(lc); table.addCell(vc);

            document.add(table);
            document.add(new Paragraph("\nThank you for staying with us.")
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(20).setFontSize(11));
            document.close();
            return new File(fileName).getAbsolutePath();
        } catch (Exception e) {
            System.err.println("PDF generation error: " + e.getMessage());
            return null;
        }
    }

    private void addRow(Table table, String key, String value) {
        table.addCell(new Cell().add(new Paragraph(key).setBold()));
        table.addCell(new Cell().add(new Paragraph(value)));
    }
}
