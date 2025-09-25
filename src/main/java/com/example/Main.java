package com.example;

import com.example.api.ElpriserAPI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void main(String[] args) {
        ElpriserAPI elpriserAPI = new ElpriserAPI();

        //Minnesanteckningar för egen skull
        // --zone SE1|SE2|SE3|SE4 (required)
        // --date YYYY-MM-DD (optional, defaults to current date)
        //--sorted (optional, to display prices in descending order)
        //--charging 2h|4h|8h (optional, to find optimal charging windows)
        //--help (optional, to display usage information)

        //java -cp target/classes com.example.Main --zone SE3 --date 2025-09-04
       // java -cp target/classes com.example.Main --zone SE1 --charging 4h
        //java -cp target/classes com.example.Main --zone SE2 --date 2025-09-04 --sorted
        //java -cp target/classes com.example.Main --help

        String zone = null;
        String date = null;
        boolean sorted = false;
        String charging = null;

        //Om inga argument matas in, ge felmeddelande och visa hjälpinfo
        if (args.length == 0) {
            System.out.println("Argument saknas.");
            printHelp();
            return;
        }

        //Loopa genom String argumenten som matas is
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone":
                    if (i + 1 < args.length) {
                        zone = args[++i];
                    }
                    break;
                case "--date":
                    if (i + 1 < args.length) {
                        date = args[++i];
                    }
                    break;
                case "--sorted":
                    sorted = true;
                    break;
                case "--charging":
                    if (i + 1 < args.length) {
                        charging = args[++i];
                    }
                    break;
                case "--help":
                    printHelp();
                    break;
                default:
                    System.out.println("Ogiltigt input: " + args[i]);
            }
        }

        //Validera zone
        List<String> validZones = Arrays.asList("SE1", "SE2", "SE3", "SE4");
        if (zone == null || !validZones.contains(zone)) {
            System.out.println("invalid zone, please enter one of the following: SE1, SE2, SE3, SE4");
            return;
        } else {
            System.out.println("Vald zon: " + zone);
        }

        //Validera date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (date == null) {
            date = LocalDate.now().format(formatter);
            System.out.println("Ogiltigt datum. Använder dagens datum " + date);
        } else {
            try {
                LocalDate parsedDate = LocalDate.parse(date, formatter);
                System.out.println("Valt datum: " + parsedDate);
            } catch (DateTimeParseException e) {
                System.out.println("invalid date, please enter a valid date");
                return;
            }
        }
        if (sorted) {
            //Skriv ut lista av sorterade priser
            System.out.println("Här kommer en sorterad lista skrivas ut");
        }
        if (charging != null) {
            System.out.println("Vald charging: " + charging);
        }
    }


    //Problem, kan inte hämta priser från API:n
    //För att jag aldrig sparar några variabler som kan skickas in för att hämta dem?
    //Önskar tips på omstrukturering...

    public static void printHelp() {
        System.out.println("--Användning av Elpriser API--");
        System.out.println("--zone SE1|SE2|SE3|SE4 (required)");
        System.out.println("--date YYYY-MM-DD (optional, defaults to current date)");
        System.out.println("--sorted (optional, to display prices in descending order)");
        System.out.println("--charging 2h|4h|8h (optional, to find optimal charging windows)");
        System.out.println("--help optional, display this usage information");
    }
}

