package com.example.carrental;

import com.example.carrental.exception.NoCarAvailableException;
import com.example.carrental.exception.ReservationNotFoundException;
import com.example.carrental.fleet.Fleet;
import com.example.carrental.model.CarType;
import com.example.carrental.model.Reservation;
import com.example.carrental.repository.InMemoryReservationRepository;
import com.example.carrental.service.CarRentalService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Administrator Configure Your Fleet ===");
        int sedans = getValidIntInput(scanner, "Enter number of Sedans (0 or more): ");
        int suvs = getValidIntInput(scanner, "Enter number of SUVs (0 or more): ");
        int vans = getValidIntInput(scanner, "Enter number of Vans (0 or more): ");

        Fleet fleet = Fleet.withCapacity(Map.of(
                CarType.SEDAN, sedans,
                CarType.SUV, suvs,
                CarType.VAN, vans
        ));

        System.out.println("Fleet initialized successfully by the Administrator !\n");

        //Use SystemDefaultZone for this assignment
        
        CarRentalService service = new CarRentalService(
                fleet, new InMemoryReservationRepository(), Clock.systemDefaultZone());

        // Interactive Loop for Menu
        while (true) {
            System.out.println("=== Car Rental Menu ===");
            System.out.println("1. Make a Reservation");
            System.out.println("2. Cancel a Reservation");
            System.out.println("3. Check Available Inventory");
            System.out.println("4. Exit");
            System.out.print("Choose an option (1-4): ");

            int choice = getValidIntInput(scanner, "");
            if (choice == 4) {
                System.out.println("Exiting program. Goodbye!");
                break;
            } 
            
            switch (choice) {
            	// Make a Reservation
                case 1: {
                    CarType carType = selectCarType(scanner);
                    LocalDateTime pickUp = getValidPickupDateTime(scanner);
                    
                    int rentalDays = getValidIntInput(scanner, "Enter number of days to rent (must be > 0): ");
                    while (rentalDays <= 0) {
                        System.out.println("Rental days must be greater than 0.");
                        rentalDays = getValidIntInput(scanner, "Enter number of days to rent: ");
                    }

                    try {
                        Reservation reservation = service.reserve(carType, pickUp, rentalDays);
                        System.out.println("Success! Booked: " + reservation);
                        System.out.println("Your Reservation ID: " + reservation.getId() + "\n");
                    } catch (NoCarAvailableException e) {
                        System.out.println("Rejected: No " + carType + " available for the selected dates. (" + e.getMessage() + ")\n");
                    }
                    break;
                }
                //Cancel a Reservation
                case 2: {
                    System.out.print("Enter Reservation ID to cancel: ");
                    scanner.nextLine(); // Consume leftover newline
                    String reservationId = scanner.nextLine().trim();

                    try {
                        service.cancel(reservationId);
                        System.out.println("Successfully cancelled reservation ID: " + reservationId + "\n");
                    } catch (ReservationNotFoundException e) {
                        System.out.println("Cancellation failed: Reservation ID not found. (" + e.getMessage() + ")\n");
                    } catch (Exception e) {
                        System.out.println("Cancellation failed: " + e.getMessage() + "\n");
                    }
                    break;
                }
                //Check Available Inventory
                case 3: {                	
                	// Check available inventory and active reservations for a given date range
                    LocalDateTime checkDate = getValidPickupDateTime(scanner);
                    int durationDays = getValidIntInput(scanner, "Enter duration in days to check: ");
                    while (durationDays <= 0) {
                        System.out.println("Duration must be greater than 0.");
                        durationDays = getValidIntInput(scanner, "Enter duration in days to check: ");
                    }

                    System.out.println("\n--- Available Inventory for period starting " + checkDate + " (" + durationDays + " days) ---");
                    System.out.println("----------------------------------------");
                    for (CarType type : CarType.values()) {
                        int availableCount = service.availableCount(type, checkDate, durationDays);
                        System.out.println(type + ": " + availableCount + " available");
                        System.out.println("----------------------------------------");
                        service.printReservedCars(type, checkDate, durationDays);
                        System.out.println("---------------------------------------");
                    }
                    System.out.println();
                    break;
                }
                default:
                    System.out.println("Invalid choice. Please enter 1, 2, 3, or 4.\n");
            }
        }

        scanner.close();
    }

    //Menu to display car type selection
    private static CarType selectCarType(Scanner scanner) {
        while (true) {
            System.out.println("Select Car Type:");
            System.out.println("1. SEDAN");
            System.out.println("2. SUV");
            System.out.println("3. VAN");
            System.out.print("Enter choice (1-3): ");

            int typeChoice = getValidIntInput(scanner, "");
            switch (typeChoice) {
                case 1: return CarType.SEDAN;
                case 2: return CarType.SUV;
                case 3: return CarType.VAN;
                default: System.out.println("Invalid selection. Try again.");
            }
        }
    }

    /**
     * Date Format: yyyy-MM-dd HH:mm
     * Suggest default time starting 10 minutes from now
     * Cannot book a pickup time in the past
     * @param scanner
     * @return LocalDateTime
     */
    private static LocalDateTime getValidPickupDateTime(Scanner scanner) {
        scanner.nextLine(); // Consume leftover newline buffer
        
        // Suggest default time starting 10 minutes from now
        LocalDateTime suggestedTime = LocalDateTime.now().plusMinutes(10);
        String suggestedStr = suggestedTime.format(DATE_TIME_FORMATTER);

        while (true) {
            System.out.print("Enter start date and time [Format: yyyy-MM-dd HH:mm] (Press Enter to use default: " + suggestedStr + "): ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                input = suggestedStr;
            }

            try {
                LocalDateTime dateTime = LocalDateTime.parse(input, DATE_TIME_FORMATTER);
                LocalDateTime now = LocalDateTime.now();

                // Only validate that the pickup time is not in the past
                if (dateTime.isBefore(now)) {
                    System.out.println("Error: Cannot book a pickup time in the past.");
                    continue;
                }

                return dateTime;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid format! Please use yyyy-MM-dd HH:mm (e.g., 2026-06-01 14:30).");
            }
        }
    }

    /*
     * Input validation for the menu
     */
    private static int getValidIntInput(Scanner scanner, String prompt) {
        int value;
        while (true) {
            if (!prompt.isEmpty()) {
                System.out.print(prompt);
            }
            if (scanner.hasNextInt()) {
                value = scanner.nextInt();
                if (value >= 0) {
                    return value;
                } else {
                    System.out.println("Invalid input! Please enter 0 or a positive number.");
                }
            } else {
                System.out.println("Invalid input! Please enter a valid whole number.");
                scanner.next(); 
            }
        }
    }
}