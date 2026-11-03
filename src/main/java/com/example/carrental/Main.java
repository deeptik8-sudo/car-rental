package com.example.carrental;

import com.example.carrental.exception.NoCarAvailableException;
import com.example.carrental.fleet.Fleet;
import com.example.carrental.model.CarType;
import com.example.carrental.model.Reservation;
import com.example.carrental.repository.InMemoryReservationRepository;
import com.example.carrental.service.CarRentalService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Scanner;

/**
 * Small console demo of the reservation flow. Run with: mvn -q compile exec:java
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
    	
    	Scanner scanner = new Scanner(System.in);
        System.out.println("***********************************");
        System.out.println("Administrator of the rental car system" + "\n" + "Enter the inventory");
        System.out.println("***********************************");

        int sedans = getValidIntInput(scanner, "Enter number of Sedans: ");
        int suvs = getValidIntInput(scanner, "Enter number of SUVs: ");
        int vans = getValidIntInput(scanner, "Enter number of Vans: ");

        // Create the fleet dynamically using user input
        Fleet fleet = Fleet.withCapacity(Map.of(
                CarType.SEDAN, sedans,
                CarType.SUV, suvs,
                CarType.VAN, vans
        ));

        // Initialize your services/repositories and continue with your app logic here...
        System.out.println("Administrator initialized fleet inventory successfully with capacity!");
        System.out.println("Available SEDAN: " + fleet.capacity(CarType.SEDAN));
        System.out.println("Available SUV  : " + fleet.capacity(CarType.SUV));
        System.out.println("Available VAN  : " + fleet.capacity(CarType.VAN));
        System.out.println("***********************************");

        CarRentalService service = new CarRentalService(
                fleet, new InMemoryReservationRepository(), Clock.systemDefaultZone());

        LocalDateTime pickUp = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

	     // 1. First Reservation
	     Reservation first = null;
         try {
            first = service.reserve(CarType.SUV, pickUp, 3);
            System.out.println("Booked:   " + first);
         } catch (NoCarAvailableException e) {
            System.out.println("Rejected (First reservation): " + e.getMessage());
         }

         // 2. Second Reservation
         try {
        	 Reservation second = service.reserve(CarType.SUV, pickUp.plusDays(1), 2);
             System.out.println("Booked:   " + second);

            // System.out.println("Booked:   SUV for 2 days starting " + pickUp.plusDays(1));
         } catch (NoCarAvailableException e) {
             System.out.println("Rejected (Second reservation): " + e.getMessage());
         }

         // 3. Back-to-Back Reservation (Only runs if 'first' was successful)
         if (first != null) {
             try {
                 Reservation backToBack = service.reserve(CarType.SUV, first.getEnd(), 2);
                 System.out.println("Booked:   " + backToBack + "  (starts exactly when the first ends)");
             } catch (NoCarAvailableException e) {
                 System.out.println("Rejected (Back-to-back reservation): " + e.getMessage());
             }

             // 4. Cancel First
             try {
                 service.cancel(first.getId());
                 System.out.println("Cancelled " + first.getId());
             } catch (Exception e) {
                 System.out.println("Could not cancel: " + e.getMessage());
             }
	     }

         // 5. Rebooked Reservation
         try {
             Reservation rebooked = service.reserve(CarType.SUV, pickUp.plusDays(1), 2);
             System.out.println("Booked:   " + rebooked);
         } catch (NoCarAvailableException e) {
            System.out.println("Rejected (Rebooked reservation): " + e.getMessage());
         }
         
         //6. Book Another SUV
         try {
             Reservation rebooked = service.reserve(CarType.SUV, pickUp.plusDays(5), 5);
             System.out.println("Booked:   " + rebooked);
         } catch (NoCarAvailableException e) {
            System.out.println("Rejected (Rebooked reservation): " + e.getMessage());
         }
   }
    
    // Helper method to enforce integer input
    private static int getValidIntInput(Scanner scanner, String prompt) {
    	int value;
        while (true) {
            System.out.print(prompt);
            if (scanner.hasNextInt()) {
                value = scanner.nextInt();
                if (value >= 0) {
                    return value; // Valid input: integer and >= 0
                } else {
                    System.out.println("Invalid input! Please enter 0 or a positive number.");
                }
            } else {
                System.out.println("Invalid input! Please enter a valid whole number.");
                scanner.next(); // Discard the invalid non-integer token
            }
    }
    }
}
