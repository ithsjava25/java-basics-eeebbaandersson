package com.example;

import com.example.api.ElpriserAPI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
        //Ta bort hjälp utskrift senare?
        List<String> validZones = Arrays.asList("SE1", "SE2", "SE3", "SE4");
        if (zone == null || !validZones.contains(zone)) {
            System.out.println("invalid zone, please enter one of the following: SE1, SE2, SE3, SE4");
            return;
        } else {
            System.out.println("Vald zon: " + zone);
        }

        //Validera date
        //Ta bort denna utskrift senare?
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

        //Anropar API:n med data/input från användaren
        List<ElpriserAPI.Elpris> Elpriser = elpriserAPI.getPriser(date, ElpriserAPI.Prisklass.valueOf(zone));

        if (sorted) {
            //Skriver ut lista av sorterade priser
            sortPrices(Elpriser);
        }
        if (charging != null) {
            //Hantera argumentfel/ge felmeddelande vid fel laddningstid ex. --charging 10h
            System.out.println("Vald charging: " + charging);
        }

        //Todo: Anropa min/max/medelpris korrekt i programmet,kontrollera att utskrifterna matchar testresultatet
        //Todo: Skapa metod för att hitta optimal charginghour 2h, 4h, 8h.
        //Todo: Fixa utskriften av sortPrices!
    }


    //Metod för sortering av priser
    public static void sortPrices(List<ElpriserAPI.Elpris> Elpriser) {
        List<ElpriserAPI.Elpris> sorteradePriser = new ArrayList<>(Elpriser);
        sorteradePriser.sort(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh).reversed());

        DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("HH");

        for (ElpriserAPI.Elpris elpriser : sorteradePriser) {
            String timeRange = elpriser.timeStart().toLocalTime().format(hourFormatter) + "-" +
                    elpriser.timeEnd().toLocalTime().format(hourFormatter);

            double orepris = elpriser.sekPerKWh() * 100;

            System.out.printf("%s %05.2f öre\n",
                    timeRange,orepris);
        }
    }

    public static ElpriserAPI.Elpris getMaxPrice(List<ElpriserAPI.Elpris> priser){
        if (priser == null || priser.isEmpty()){
            return null;
        }
        ElpriserAPI.Elpris max = priser.getFirst();
        for (ElpriserAPI.Elpris elpris : priser) {
            if (elpris.sekPerKWh() > max.sekPerKWh()) {
                max = elpris;
            }
        }
        return max;
    }

    public static ElpriserAPI.Elpris getMinPrice(List<ElpriserAPI.Elpris> priser){
        if (priser == null || priser.isEmpty()){
            return null;
        }
        ElpriserAPI.Elpris min = priser.getFirst();
        for (ElpriserAPI.Elpris elpris : priser){
            if (elpris.sekPerKWh() < min.sekPerKWh()) {
                min = elpris;
            }
        }
        return min;
    }

    public static double getAveragePrice(List<ElpriserAPI.Elpris> priser){
        if (priser == null || priser.isEmpty()){
            return 0;
            //Lägg till throw exception?
        }
        double sum = 0.0;
        for(ElpriserAPI.Elpris elpris : priser){
            sum += elpris.sekPerKWh();
        }
        return sum/priser.size();
    }

    public static void printHelp() {
        System.out.println("--Användning av Elpriser API--");
        System.out.println("--zone SE1|SE2|SE3|SE4 (required)");
        System.out.println("--date YYYY-MM-DD (optional, defaults to current date)");
        System.out.println("--sorted (optional, to display prices in descending order)");
        System.out.println("--charging 2h|4h|8h (optional, to find optimal charging windows)");
        System.out.println("--help optional, display this usage information");
    }
}

