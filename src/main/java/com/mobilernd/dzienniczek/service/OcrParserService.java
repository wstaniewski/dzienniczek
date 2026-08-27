package com.mobilernd.dzienniczek.service;

import com.mobilernd.dzienniczek.model.FoodEntry;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OcrParserService {

    private static final String[] MEAL_KEYWORDS = {
            "śniadanie", "sniadanie",
            "ii śniadanie", "ii sniadanie", "il śniadanie", "il sniadanie",
            "zupa",
            "ii danie", "ii  danie", "il danie", "il  danie",
            "podwieczorek", "podw", "podwieczore"
    };

    private String normalizeLine(String line) {
        return line
                .toLowerCase()
                .replace("—", "-")
                .replace("–", "-")
                .replace("−", "-")
                .replace(":", "-")
                .replace("  ", " ")
                .trim();
    }

    private String detectMeal(String line) {
        String normalized = normalizeLine(line);

        for (String keyword : MEAL_KEYWORDS) {
            if (normalized.startsWith(keyword)) {

                if (keyword.contains("śniadanie") && normalized.startsWith("ii"))
                    return "II śniadanie";
                if (keyword.contains("śniadanie") && normalized.startsWith("il"))
                    return "II śniadanie";
                if (keyword.equals("śniadanie") || keyword.equals("sniadanie"))
                    return "Śniadanie";

                if (keyword.contains("zupa"))
                    return "Zupa";

                if (keyword.contains("ii danie") || keyword.contains("il danie"))
                    return "II danie";

                if (keyword.contains("podw") || keyword.contains("podwiecz"))
                    return "Podwieczorek";
            }
        }
        return null;
    }

    private String extractDescription(String line) {
        if (line.contains("-")) {
            return line.split("-", 2)[1].trim();
        }

        String normalized = normalizeLine(line);
        for (String keyword : MEAL_KEYWORDS) {
            if (normalized.startsWith(keyword)) {
                return normalized.replace(keyword, "").trim();
            }
        }

        return line.trim();
    }

    public List<FoodEntry> parse(String text) {

        List<FoodEntry> entries = new ArrayList<>();
        String currentDay = null;
        String currentMeal = null;
        StringBuilder currentDesc = new StringBuilder();

        LocalDate mondayDate = detectMondayDate(text);

        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            if (isDay(line)) {

                if (currentDay != null && currentMeal != null) {
                    entries.add(buildEntry(currentDay, currentMeal, currentDesc.toString().trim(),
                            mapDayToDate(currentDay, mondayDate)));
                }

                currentDay = normalizeDay(line);
                currentMeal = null;
                currentDesc = new StringBuilder();
                continue;
            }

            if (currentDay == null) continue;

            String meal = detectMeal(line);

            if (meal != null) {

                if (currentMeal != null) {
                    entries.add(buildEntry(currentDay, currentMeal, currentDesc.toString().trim(),
                            mapDayToDate(currentDay, mondayDate)));
                }

                currentMeal = meal;
                currentDesc = new StringBuilder();

                String desc = extractDescription(line);
                if (!desc.isEmpty()) currentDesc.append(desc).append(" ");

                continue;
            }

            if (currentMeal != null) {
                currentDesc.append(line).append(" ");
            }
        }

        if (currentDay != null && currentMeal != null) {
            entries.add(buildEntry(currentDay, currentMeal, currentDesc.toString().trim(),
                    mapDayToDate(currentDay, mondayDate)));
        }

        return entries;
    }

    private FoodEntry buildEntry(String day, String meal, String desc, LocalDate date) {
        FoodEntry entry = new FoodEntry();
        entry.setDayName(day);
        entry.setMealName(meal);
        entry.setMealType(resolveMealType(meal));
        entry.setDescription(desc);
        entry.setCalories(0);
        entry.setDate(date);
        return entry;
    }

    private String resolveMealType(String mealName) {
        return switch (mealName) {
            case "Śniadanie", "II śniadanie" -> "śniadanie";
            case "Zupa", "II danie" -> "obiad";
            case "Podwieczorek" -> "przekąska";
            default -> "inne";
        };
    }

    private boolean isDay(String line) {
        return line.startsWith("Poniedziałek")
                || line.startsWith("Wtorek")
                || line.startsWith("Środa")
                || line.startsWith("Czwartek")
                || line.startsWith("Piątek")
                || line.startsWith("Piatek");
    }

    private String normalizeDay(String line) {
        if (line.startsWith("Poniedziałek")) return "Poniedziałek";
        if (line.startsWith("Wtorek")) return "Wtorek";
        if (line.startsWith("Środa")) return "Środa";
        if (line.startsWith("Czwartek")) return "Czwartek";
        if (line.startsWith("Piatek") || line.startsWith("Piątek")) return "Piątek";
        return line;
    }

    private LocalDate detectMondayDate(String text) {
        for (String line : text.split("\n")) {
            line = line.trim().replace("r.", "").replace("r", "").trim();

            if (line.matches("\\d{1,2}-\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}")) {
                String[] parts = line.split("-");
                String startDay = parts[0].trim();
                String[] rightSplit = parts[1].trim().split("\\.");

                return LocalDate.of(
                        Integer.parseInt(rightSplit[2]),
                        Integer.parseInt(rightSplit[1]),
                        Integer.parseInt(startDay)
                );
            }
        }

        return LocalDate.now();
    }

    private LocalDate mapDayToDate(String dayName, LocalDate mondayDate) {
        return switch (dayName) {
            case "Poniedziałek" -> mondayDate;
            case "Wtorek" -> mondayDate.plusDays(1);
            case "Środa" -> mondayDate.plusDays(2);
            case "Czwartek" -> mondayDate.plusDays(3);
            case "Piątek" -> mondayDate.plusDays(4);
            default -> mondayDate;
        };
    }
}
