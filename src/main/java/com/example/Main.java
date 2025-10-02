package com.example;

import com.example.api.ElpriserAPI;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void main(String[] args) {
        ElpriserAPI elpriserAPI = new ElpriserAPI();
        Locale.setDefault(new Locale("sv","se"));

        String zone = null;
        String date = null;
        boolean sorted = false;
        String charging = null;

        System.out.println("--Välkommen till Elpriskollen--");

        if (args.length == 0) {
            System.out.println("Argument saknas.");
            printHelp();
            return;
        }

        //Loopar igenom String argumenten som matas in
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

        //Validerar zone
        List<String> validZones = Arrays.asList("SE1", "SE2", "SE3", "SE4");

        if (zone == null || !validZones.contains(zone.toUpperCase())) {
            System.out.println("Ogiltig zone. Välj någon av följande: SE1, SE2, SE3, SE4");
            return;
        } else {
            zone = zone.toUpperCase();
            System.out.println("Vald zon: " + zone);
        }

        //Validerar date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate parsedDate;

        if (date == null) {
            //Om inget datum angetts, använd dagens
            parsedDate = LocalDate.now();
            date = parsedDate.format(formatter);
            System.out.println("Inget datum angavs. Använder dagens datum. " + date);
        } else {
            try {
                parsedDate = LocalDate.parse(date, formatter);
                System.out.println("Valt datum: " + parsedDate);
            } catch (DateTimeParseException e) {
                System.out.println("Ogiltigt datum, använd formatet (yyyy-MM-dd).");
                return;
            }
        }

        LocalDate nextDay = parsedDate.plusDays(1);
        String currentDateStr = parsedDate.format(formatter);
        String nextDateStr = nextDay.format(formatter);

        //Arraylist för dagens/morgondagens priser
        List<ElpriserAPI.Elpris> elpriser = new ArrayList<>();

        var priserDag1 = elpriserAPI.getPriser(currentDateStr,
                ElpriserAPI.Prisklass.valueOf(zone));
        var priserDag2 = elpriserAPI.getPriser(nextDateStr,
                ElpriserAPI.Prisklass.valueOf(zone));

        elpriser.addAll(priserDag1);
        elpriser.addAll(priserDag2);

        //Filtrera ut dagens priser
        List<ElpriserAPI.Elpris> dagensPriser = elpriser.stream()
                .filter(p -> p.timeStart().toLocalDate().equals(parsedDate)).toList();

        if (!sorted) {
            if (dagensPriser.size() == 96){
                listWith96Values(dagensPriser);
            } else {
                try {
                    validateElpriserList(dagensPriser);

                    //Anropar metoder för max/min,medelpris
                    ElpriserAPI.Elpris maxPrice = getMaxPrice(dagensPriser);
                    ElpriserAPI.Elpris minPrice = getMinPrice(dagensPriser);
                    double averagePrice = getAveragePrice(dagensPriser);

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
            }

        } else {
            //Skriver ut dagens/morgondagens priser
            sortPrices(elpriser);
        }

        if (charging != null) {
            if (!charging.equals("2h") && !charging.equals("4h") && !charging.equals("8h")) {
                System.out.println("Fel: Ogiltig laddningstid. Använd 2h, 4h eller 8h.");
                return;
            }
            System.out.println("Vald laddningstid: " + charging);

            int timmar = Integer.parseInt(charging.replace("h", ""));

            try {
                List<ElpriserAPI.Elpris> optimalChargingWindow = findOptimalChargingWindow(elpriser, timmar);

                ElpriserAPI.Elpris forstaTimme = optimalChargingWindow.getFirst();

                LocalDate startDatum = forstaTimme.timeStart().toLocalDate();
                LocalTime startTime = forstaTimme.timeStart().toLocalTime();

                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                String datumStr = startDatum.format(dateFormatter);
                String startTidStr = startTime.format(timeFormatter);

                System.out.printf("Optimalt laddningsfönster: Påbörja laddning %s kl %s\n", datumStr, startTidStr);

                double totalPrice = 0.0;

                for (ElpriserAPI.Elpris elpris : optimalChargingWindow) {
                    String datum = elpris.timeStart().toLocalDate().format(dateFormatter);
                    String timeRange = elpris.timeStart().toLocalTime().format(timeFormatter) + "-" +
                            elpris.timeEnd().toLocalTime().format(timeFormatter);
                    double orepris = elpris.sekPerKWh() * 100;
                    totalPrice += orepris;

                    System.out.printf("%s %s: %.2f öre\n", datum, timeRange, orepris);

                }
                double averagePrice = totalPrice / timmar;
                System.out.printf("Medelpris för fönster: %.2f öre\n", averagePrice);

            } catch (IllegalArgumentException e) {
                System.out.println("Fel vid beräkning: " + e.getMessage());
            }
        }
    }

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
            System.out.printf("%s %s öre\n", timeRange, formateratPris);

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
        return min ;
    }

    public static double getAveragePrice(List<ElpriserAPI.Elpris> elpriser) {
        double sum = 0.0;
        for (ElpriserAPI.Elpris elpris : elpriser) {
            sum += elpris.sekPerKWh();
        }
        return sum / elpriser.size();
    }

    public static List<ElpriserAPI.Elpris> findOptimalChargingWindow(List<ElpriserAPI.Elpris> elpriser, int timmar) {
        if (elpriser.size() < timmar) {
            throw new IllegalArgumentException("Hittade inte tillräckligt många timmar för att skapa laddningsfönstret");
        }
        double mySum = Double.MAX_VALUE;
        int startIndex = 0;

        for (int i = 0; i <= elpriser.size() - timmar ; i++) {
            double sum = 0;
            for (int j = 0; j < timmar; j++){
                sum += elpriser.get(i + j).sekPerKWh();
            }
            if (sum < mySum){
                mySum = sum;
                startIndex = i;
            }
        }
        return elpriser.subList(startIndex, startIndex + timmar);
    }

    public static void listWith96Values(List<ElpriserAPI.Elpris> dagensPriser) {

        List<Elpris> elpriser = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            int startIndex = i * 4;
            int endIndex = startIndex + 4;

            double sumPrice = 0.0;

            ZonedDateTime timeStart = dagensPriser.get(startIndex).timeStart();
            ZonedDateTime timeEnd = dagensPriser.get(endIndex - 1).timeEnd();

            for (int j = startIndex; j < endIndex; j++) {
                sumPrice += dagensPriser.get(j).sekPerKWh();
            }
            double averagePrice = sumPrice / 4;

            Elpris elprisHour = new Elpris(averagePrice, timeStart, timeEnd);
            elpriser.add(elprisHour);
        }

        Elpris minPriceHour = elpriser.get(0);
        Elpris maxPriceHour = elpriser.get(0);

        for (Elpris elpris : elpriser) {
            if (elpris.sekPerKWh() < minPriceHour.sekPerKWh()) {
                minPriceHour = elpris;
            }
            if (elpris.sekPerKWh() > maxPriceHour.sekPerKWh()) {
                maxPriceHour = elpris;
            }
        }

        double totalSum = 0;
        for (ElpriserAPI.Elpris kvartalPris : dagensPriser) {
            totalSum += kvartalPris.sekPerKWh();
        }
        double medelPris = totalSum / dagensPriser.size();

        //Formatera pris till öre (multiplicera med 100 och visa 2 decimaler)
        String formatOreMin = String.format(Locale.forLanguageTag("sv-SE"), "%.2f", minPriceHour.sekPerKWh() * 100);
        String formatOreMax = String.format(Locale.forLanguageTag("sv-SE"), "%.2f", maxPriceHour.sekPerKWh() * 100);
        String formatOreMedel = String.format(Locale.forLanguageTag("sv-SE"), "%.2f", medelPris * 100);

        //Formatera timmar till 00-01
        String formatHourRangeMin = String.format("%02d-%02d", minPriceHour.timeStart().getHour(), minPriceHour.timeEnd().getHour());
        String formatHourRangeMax = String.format("%02d-%02d", maxPriceHour.timeStart().getHour(), maxPriceHour.timeEnd().getHour());

        System.out.printf("Högsta pris: %s, Pris: %s öre/kWh\n", formatHourRangeMax, formatOreMax);
        System.out.printf("Lägsta pris: %s, Pris: %s öre/kWh\n", formatHourRangeMin, formatOreMin);
        System.out.printf("Medelpris: %s öre/kWh\n", formatOreMedel);

    }
    
    public static void printHelp() {
        System.out.println("--Användning/usage av Elpriser API--");
        System.out.println("--zone SE1|SE2|SE3|SE4 (Nödvändig)");
        System.out.println("--date YYYY-MM-DD (Valfritt, dagens datum anges som standard)");
        System.out.println("--sorted (Valfritt, visar en sorterad prislista)");
        System.out.println("--charging 2h|4h|8h (Valfritt, hittar optimala laddningsfönstret)");
        System.out.println("--help Valfritt, visar denna hjälpinformation");
    }

    public record Elpris (double sekPerKWh, ZonedDateTime timeStart, ZonedDateTime timeEnd) {}

}







