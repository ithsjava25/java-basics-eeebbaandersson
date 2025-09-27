package com.example;

import com.example.api.ElpriserAPI;

import java.lang.classfile.FieldElement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
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

        String zone = null;
        String date = null;
        boolean sorted = false;
        String charging = null;

        System.out.println("--Välkommen till Elpriskollen--");


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
                    } else {
                        System.out.println("Fel: --zone kräver en zon (SE1-SE4).");
                        return;
                    }
                    break;
                case "--date":
                    if (i + 1 < args.length) {
                        date = args[++i];
                    } else {
                        System.out.println("Fel: --date kräver ett datum (yyyy-MM-dd).");
                        return;
                    }
                    break;
                case "--sorted":
                    sorted = true;
                    break;
                case "--charging":
                    if (i + 1 < args.length) {
                        charging = args[++i];
                    } else {
                        System.out.println("Fel: --charging kräver ett argument (2h, 4h eller 8h).");
                    }
                    break;
                case "--help":
                    printHelp();
                    return;
                default:
                    System.out.println("Ogiltigt input: " + args[i]);
                    return;
            }
        }

        //Validera zone
        List<String> validZones = Arrays.asList("SE1", "SE2", "SE3", "SE4");

        //Todo: se över to.UperCase inmatningen i zonvalideringen?
        if (zone == null || !validZones.contains(zone.toUpperCase())) {
            System.out.println("Ogiltig zone. Välj någon av följande: SE1, SE2, SE3, SE4");
            return;
        } else {
            zone = zone.toUpperCase();
            System.out.println("Vald zon: " + zone);
        }

        //Validera date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        //Variabel för datum som LocalDate
        LocalDate parsedDate;

        if (date == null) {
            //Om inget datum angetts, använd dagens
            parsedDate = LocalDate.now();
            date = parsedDate.format(formatter);
            System.out.println("Inget datum angavs. Använder dagens datum. " + date);
        } else {
            try {
               parsedDate = LocalDate.parse(date,formatter);
                System.out.println("Valt datum: " + parsedDate);
            } catch (DateTimeParseException e) {
                System.out.println("Ogiltigt datum, använd formatet (yyyy-MM-dd).");
                return;
            }
        }

        //Anropar API:n med data/input från användaren
        List<ElpriserAPI.Elpris> elpriser = elpriserAPI.getPriser(date, ElpriserAPI.Prisklass.valueOf(zone));

        if (!sorted) {
            try {
                validateElpriserList(elpriser);

                //Anropar metoder för max/min,medelpris efter att listan validerats i tidigare metod
                ElpriserAPI.Elpris maxPrice = getMaxPrice(elpriser);
                ElpriserAPI.Elpris minPrice = getMinPrice(elpriser);
                double averagePrice = getAveragePrice(elpriser);

                DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("HH");

                String maxTime = maxPrice.timeStart().toLocalTime().format(hourFormatter) + "-"
                        + maxPrice.timeEnd().toLocalTime().format(hourFormatter);

                String minTime = minPrice.timeStart().toLocalTime().format(hourFormatter) + "-"
                        + minPrice.timeEnd().toLocalTime().format(hourFormatter);


                System.out.printf("Högsta pris: %s %05.2f öre\n", maxTime, maxPrice.sekPerKWh() * 100);
                System.out.printf("Lägsta pris: %s %05.2f öre\n", minTime, minPrice.sekPerKWh() * 100);
                System.out.printf("Medelpris: %05.2f öre\n", averagePrice * 100);

            } catch (IllegalArgumentException e) {
                System.out.println("Ingen data hittades vid hämtning eller beräkning av elpriser");
                return;
            }

        } else {
            sortPrices(elpriser);
        }

        if (charging != null) {
            if (!charging.equals("2h") && !charging.equals("4h") && !charging.equals("8h")) {
                System.out.println("Fel: Ogiltig laddningstid. Använd 2h, 4h eller 8h.");
                return;
            }
            //Lägg till metod/logik för att hitta billigaste laddningstimmen
            System.out.println("Vald laddningstid: " + charging);
        }
    }

    //Metoder
    public static void validateElpriserList(List<ElpriserAPI.Elpris> elpriser) {
        if (elpriser == null || elpriser.isEmpty()) {
            throw new IllegalArgumentException("Fel: Ingen tillgänglig data kunde hittas");
        }
    }

    public static void sortPrices(List<ElpriserAPI.Elpris> elpriser) {
        List<ElpriserAPI.Elpris> sorteradePriser = new ArrayList<>(elpriser);
        sorteradePriser.sort(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh).reversed());

        DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("HH");

        for (ElpriserAPI.Elpris elpris : sorteradePriser) {
            String timeRange = elpris.timeStart().toLocalTime().format(hourFormatter) + "-" +
                    elpris.timeEnd().toLocalTime().format(hourFormatter);

            double orepris = elpris.sekPerKWh() * 100.0;
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("sv", "SE"));
            DecimalFormat df = new DecimalFormat("0.00", symbols);

            String formateratPris = df.format(orepris);
            System.out.printf("%s %s öre\n",
                    timeRange, formateratPris);
        }
    }

    public static ElpriserAPI.Elpris getMaxPrice(List<ElpriserAPI.Elpris> elpriser){
        ElpriserAPI.Elpris max = elpriser.getFirst();
        for (ElpriserAPI.Elpris elpris : elpriser) {
            if (elpris.sekPerKWh() > max.sekPerKWh()) {
                max = elpris;
            }
        }
        return max;
    }

    public static ElpriserAPI.Elpris getMinPrice(List<ElpriserAPI.Elpris> elpriser){
        ElpriserAPI.Elpris min = elpriser.getFirst();
        for (ElpriserAPI.Elpris elpris : elpriser){
            if (elpris.sekPerKWh() < min.sekPerKWh()) {
                min = elpris;
            }
        }
        return min;
    }

    public static double getAveragePrice(List<ElpriserAPI.Elpris> elpriser){
        double sum = 0.0;
        for(ElpriserAPI.Elpris elpris : elpriser){
            sum += elpris.sekPerKWh();
        }
        return sum/ elpriser.size();
    }

    public static void printHelp() {
        System.out.println("--Användning/usage av Elpriser API--");
        System.out.println("--zone SE1|SE2|SE3|SE4 (Nödvändig)");
        System.out.println("--date YYYY-MM-DD (Valfritt, dagens datum anges som standard)");
        System.out.println("--sorted (Valfritt, visar en sorterad prislista)");
        System.out.println("--charging 2h|4h|8h (Valfritt, hittar optimala laddningsfönstret)");
        System.out.println("--help Valfritt, visar denna hjälpinformation");
    }
}

