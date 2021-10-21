package com.powsybl.python;

import com.farao_community.farao.data.glsk.api.AbstractGlskRegisteredResource;
import com.farao_community.farao.data.glsk.ucte.UcteGlskDocument;
import com.farao_community.farao.data.glsk.ucte.UcteGlskDocumentImporter;
import com.farao_community.farao.data.glsk.ucte.UcteGlskPoint;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GLSKimportContext {

    private UcteGlskDocument document = null;

    private Map<String, List<Map<String, Double>>> data = new HashMap();

    public void load(String filename) {
        System.out.println("Import GLSK : Try loading " + filename);
        try {
            document = (UcteGlskDocument) new UcteGlskDocumentImporter().importGlsk(new FileInputStream(filename));
        } catch (FileNotFoundException e) {
            System.out.println("Import GLSK : File not found " + filename);
            return;
        }

        for (Map.Entry<String, List<UcteGlskPoint>> dataPerCountry : document.getUcteGlskPointsByCountry().entrySet()) {
            List<Map<String, Double>> points = new ArrayList<>();
            for (UcteGlskPoint point : dataPerCountry.getValue()) {
                Map<String, Double> factors = new HashMap<>();
                if (point.getGlskShiftKeys().size() > 1) {
                    System.out.println("There is several shift keys...");
                    continue;
                }
                for (AbstractGlskRegisteredResource resource : point.getGlskShiftKeys().get(0).getRegisteredResourceArrayList()) {
                    factors.put(resource.getName(), resource.getParticipationFactor());
                }
                points.add(factors);
            }
            data.put(dataPerCountry.getKey(), points);
        }
        /*System.out.println("GLSK loaded");
        System.out.println("Got " + data.keySet().size() + " country");
        for (List<Map<String, Double>> list : data.values()) {
            System.out.println("Got list of " + list.size() + " elements");
            for (Map<String, Double> m : list) {
                System.out.println("Got map of " + m.size() + " elements");
            }
        }*/
    }

    public List<Map<String, Double>> getPointsForCountry(String country) {
        return data.get(country);
    }

    public List<String> getInjectionIdForCountry(String country, Instant instant) {
        try {
            UcteGlskPoint point = document.getGlskPointsForInstant(instant).get(country);
            List<String> generatorIds = new ArrayList<>();
            if (point.getGlskShiftKeys().size() > 1) {
                System.out.println("There is several shift keys for country " + country + " at instant " + instant + " choosing first.");
            }
            for (AbstractGlskRegisteredResource resource : point.getGlskShiftKeys().get(0).getRegisteredResourceArrayList()) {
                generatorIds.add(resource.getName());
            }
            return generatorIds;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Double> getInjectionShiftKeysForCountry(String country) {
        return data.get(country).get(0).values().stream().collect(Collectors.toList());
    }

    public Instant getInjectionFactorStartTS() {
        return document.getGSKTimeInterval().getStart();
    }

    public Instant getInjectionFactorEndTS() {
        return document.getGSKTimeInterval().getEnd();
    }

    public List<Double> getInjectionFactorForCountryTimeinterval(String country, Instant instant) {
        try {
            System.out.println("Get Point for country and instant" + country);
            System.out.println("Time interval start " + document.getGSKTimeInterval().getStart());
            System.out.println("Time interval end at " + document.getGSKTimeInterval().getEnd());
            System.out.println("Instant is " + instant);
            UcteGlskPoint point = document.getGlskPointsForInstant(instant).get(country);
            List<Double> factors = new ArrayList<>();
            if (point.getGlskShiftKeys().size() > 1) {
                System.out.println("There is several shift keys for country " + country + " at instant " + instant + " choosing first.");
            }
            for (AbstractGlskRegisteredResource resource : point.getGlskShiftKeys().get(0).getRegisteredResourceArrayList()) {
                factors.add(resource.getParticipationFactor());
            }
            return factors;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<String> getCountries() {
        return data.keySet().stream().collect(Collectors.toList());
    }
}
