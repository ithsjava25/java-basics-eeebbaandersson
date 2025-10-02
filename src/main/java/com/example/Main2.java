package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class Main2 {
    public static void main(String[] args) {
        ElpriserAPI elpriserAPI = new ElpriserAPI();

        //Testning av metoder/raderas senare

        LocalDate idag = LocalDate.now();
        List<ElpriserAPI.Elpris> dagensPriser = elpriserAPI.getPriser(idag, ElpriserAPI.Prisklass.SE3);

        //Skriver ut sekPerKWh sorterat och dess klockslag
        List<ElpriserAPI.Elpris> sorteradePriser = new ArrayList<>(dagensPriser);
        sorteradePriser.sort(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh).reversed());

        for (ElpriserAPI.Elpris elpriser : sorteradePriser) {
            System.out.printf("Tid: %s, Pris: %.4f SEK/kWh\n",
                    elpriser.timeStart().toLocalTime(), elpriser.sekPerKWh());
        }


//        double maxPris = maxPris(dagensPriser);
//        System.out.println("Högsta pris: " + maxPris +" SEK/kWh.");

    }

    public static double maxPris(List<ElpriserAPI.Elpris> dagensPriser) {
        if (dagensPriser.isEmpty()) return 0;
        double max = dagensPriser.get(0).sekPerKWh();
        for (ElpriserAPI.Elpris elpris : dagensPriser) {
            if (elpris.sekPerKWh() > max) {
                max = elpris.sekPerKWh();
            }
        }
        //Ska * med 100 för att få korrekta örespriset.
        return max;
    }
}

