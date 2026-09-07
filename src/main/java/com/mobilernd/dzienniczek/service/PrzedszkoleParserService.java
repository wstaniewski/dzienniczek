package com.mobilernd.dzienniczek.service;

import com.mobilernd.dzienniczek.model.FoodEntry;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrzedszkoleParserService implements OcrParserStrategy {

    private static final Pattern DAY_HEADER_PATTERN =
            Pattern.compile("(?i)(poniedziałek|wtorek|środa|czwartek|piątek).*?(\\d{2})\\.(\\d{2})\\.(\\d{4})");

    private static final Pattern MEAL_PATTERN = Pattern.compile(
            "^(śniadanie|ii\\s*sniad|ii\\s*śniad|il\\s*sniad|il\\s*śniad|" +
                    "zupa|" +
                    "ii\\s*danie|il\\s*danie|" +
                    "obiad|" +
                    "podwieczorek|podw|podwiecz)" +
                    "\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    // 🔥 słowa-klucze zup (działa nawet bez przecinków)
    private static final List<String> SOUP_KEYWORDS = List.of(
            "zupa", "krupnik", "pomidorowa", "jarzynowa", "ogórkowa", "rosół",
            "krem", "barszcz", "żurek", "fasolowa", "grochowa"
    );

    private String normalize(String line) {
        return line.toLowerCase()
                .replace("—", "-")
                .replace("–", "-")
                .replace("−", "-")
                .replace(":", ": ")
                .replace("  ", " ")
                .trim();
    }

    private boolean isAllergenHeader(String line) {
        return normalize(line).startsWith("alergen");
    }

    private boolean isFooter(String line) {
        String n = normalize(line);
        return n.contains("progman")
                || n.contains("stołówka")
                || n.contains("wolters")
                || n.contains("kluwer")
                || n.contains("kolacja");
    }

    private LocalDate parseDateFromHeader(String line) {
        Matcher m = DAY_HEADER_PATTERN.matcher(normalize(line));
        if (!m.find()) return null;

        int day = Integer.parseInt(m.group(2));
        int month = Integer.parseInt(m.group(3));
        int year = Integer.parseInt(m.group(4));

        return LocalDate.of(year, month, day);
    }

    private String detectMeal(String line) {
        Matcher m = MEAL_PATTERN.matcher(normalize(line));
        if (!m.find()) return null;

        String key = m.group(1).toLowerCase();

        if (key.startsWith("ii s") || key.startsWith("il s")) return "II śniadanie";
        if (key.startsWith("śniad") || key.startsWith("sniad")) return "Śniadanie";
        if (key.startsWith("zupa")) return "Zupa";
        if (key.startsWith("ii d") || key.startsWith("il d")) return "II danie";
        if (key.startsWith("obiad")) return "Obiad";
        if (key.startsWith("podw") || key.startsWith("podwiecz")) return "Podwieczorek";

        return null;
    }

    private String extractDescription(String line) {
        String n = normalize(line);

        if (n.contains(":")) return n.split(":", 2)[1].trim();
        if (n.contains("-")) return n.split("-", 2)[1].trim();

        Matcher m = MEAL_PATTERN.matcher(n);
        if (m.find()) {
            String meal = m.group(1);
            return n.replace(meal, "").trim();
        }

        return n.trim();
    }

    // 🔥 automatyczne rozdzielanie zupy i drugiego dania
    private String[] splitSoupAndSecond(String desc) {
        String lower = desc.toLowerCase();

        for (String keyword : SOUP_KEYWORDS) {
            int idx = lower.indexOf(keyword);
            if (idx != -1) {

                // znajdź pierwszy przecinek po słowie zupy
                int comma = desc.indexOf(",", idx);

                if (comma != -1) {
                    String soup = desc.substring(0, comma).trim();
                    String second = desc.substring(comma + 1).trim();
                    return new String[]{soup, second};
                }

                // brak przecinka → heurystyka:
                // pierwsze zdanie = zupa, reszta = drugie danie
                String[] parts = desc.split("\\.");
                if (parts.length > 1) {
                    return new String[]{parts[0].trim(), desc.substring(parts[0].length()).trim()};
                }

                // brak kropki → całość to zupa
                return new String[]{desc.trim(), ""};
            }
        }

        // brak zupy → całość to drugie danie
        return new String[]{"", desc.trim()};
    }

    private String resolveDayName(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "Poniedziałek";
            case TUESDAY -> "Wtorek";
            case WEDNESDAY -> "Środa";
            case THURSDAY -> "Czwartek";
            case FRIDAY -> "Piątek";
            case SATURDAY -> "Sobota";
            case SUNDAY -> "Niedziela";
        };
    }

    private List<String> splitIntoDayBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = null;

        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            Matcher m = DAY_HEADER_PATTERN.matcher(normalize(line));
            if (m.find()) {
                if (current != null) {
                    blocks.add(current.toString());
                }
                current = new StringBuilder();
            }

            if (current != null) {
                current.append(line).append("\n");
            }
        }

        if (current != null) {
            blocks.add(current.toString());
        }

        return blocks;
    }

    @Override
    public List<FoodEntry> parse(String text) {

        List<FoodEntry> allEntries = new ArrayList<>();

        List<String> dayBlocks = splitIntoDayBlocks(text);

        for (String dayText : dayBlocks) {

            LocalDate currentDate = null;
            String currentMeal = null;
            StringBuilder currentDesc = new StringBuilder();
            boolean inAllergenBlock = false;

            for (String rawLine : dayText.split("\n")) {
                String line = rawLine.trim();
                if (line.isEmpty()) continue;

                if (currentDate == null) {
                    LocalDate d = parseDateFromHeader(line);
                    if (d != null) {
                        currentDate = d;
                        continue;
                    }
                }

                if (isFooter(line)) continue;

                if (isAllergenHeader(line)) {
                    inAllergenBlock = true;
                    continue;
                }

                String meal = detectMeal(line);

                if (meal != null && currentDate == null) {
                    continue;
                }

                if (inAllergenBlock) {
                    if (meal != null) {
                        inAllergenBlock = false;

                        if (currentMeal != null) {
                            allEntries.add(buildEntry(
                                    resolveDayName(currentDate),
                                    currentMeal,
                                    currentDesc.toString().trim(),
                                    currentDate
                            ));
                        }

                        currentMeal = meal;
                        currentDesc = new StringBuilder();

                        String desc = extractDescription(line);
                        if (!desc.isEmpty()) currentDesc.append(desc).append(" ");
                    }
                    continue;
                }

                if (meal != null) {

                    if (currentMeal != null) {
                        allEntries.add(buildEntry(
                                resolveDayName(currentDate),
                                currentMeal,
                                currentDesc.toString().trim(),
                                currentDate
                        ));
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

            if (currentMeal != null) {

                // 🔥 rozdzielanie zupy i II dania — niezależnie od nagłówków
                String[] parts = splitSoupAndSecond(currentDesc.toString().trim());
                String soup = parts[0];
                String second = parts[1];

                if (!soup.isEmpty()) {
                    allEntries.add(buildEntry(
                            resolveDayName(currentDate),
                            "Zupa",
                            soup,
                            currentDate
                    ));
                }

                if (!second.isEmpty()) {
                    allEntries.add(buildEntry(
                            resolveDayName(currentDate),
                            "II danie",
                            second,
                            currentDate
                    ));
                }
            }
        }

        return allEntries;
    }

    private FoodEntry buildEntry(String day, String meal, String desc, LocalDate date) {
        FoodEntry entry = new FoodEntry();
        entry.setDayName(day);
        entry.setMealName(meal);
        entry.setDescription(desc);
        entry.setDate(date);
        entry.setMealType(resolveMealType(meal));
        entry.setCalories(0);
        return entry;
    }

    private String resolveMealType(String mealName) {
        return switch (mealName) {
            case "Śniadanie", "II śniadanie" -> "śniadanie";
            case "Zupa" -> "pierwsze_danie";
            case "II danie" -> "drugie_danie";
            case "Obiad" -> "obiad";
            case "Podwieczorek" -> "przekąska";
            default -> "inne";
        };
    }
}