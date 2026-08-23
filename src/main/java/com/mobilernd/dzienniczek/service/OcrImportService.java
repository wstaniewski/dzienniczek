package com.mobilernd.dzienniczek.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.mobilernd.dzienniczek.model.FoodEntry;
import com.mobilernd.dzienniczek.repository.FoodEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class OcrImportService {

    private final FoodEntryRepository foodEntryRepository;

    public OcrImportService(FoodEntryRepository foodEntryRepository) {
        this.foodEntryRepository = foodEntryRepository;
    }

    private ImageAnnotatorClient createVisionClient() throws Exception {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream("src/main/resources/google-vision-key.json"))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        return ImageAnnotatorClient.create(settings);
    }

    public void process(MultipartFile file) {
        try {
            File tempFile = File.createTempFile("plan-", ".tmp");
            Files.write(tempFile.toPath(), file.getBytes());

            String text = doOcrGoogleVision(tempFile);
            System.out.println("=== OCR TEXT ===");
            System.out.println(text);
            System.out.println("=== END ===");

            List<FoodEntry> entries = parseWeeklyPlan(text);

            for (FoodEntry entry : entries) {
                foodEntryRepository.save(entry);
            }

            tempFile.delete();

        } catch (Exception e) {
            throw new RuntimeException("Błąd pliku podczas importu", e);
        }
    }

    private String doOcrGoogleVision(File file) throws Exception {
        byte[] data = Files.readAllBytes(file.toPath());
        ByteString imgBytes = ByteString.copyFrom(data);

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();

        try (ImageAnnotatorClient client = createVisionClient()) {
            BatchAnnotateImagesResponse response =
                    client.batchAnnotateImages(Collections.singletonList(request));

            AnnotateImageResponse res = response.getResponses(0);

            if (res.hasError()) {
                throw new RuntimeException("Vision API error: " + res.getError().getMessage());
            }

            return res.getFullTextAnnotation().getText();
        }
    }

    // ⭐ FINALNY PARSER — poprawne daty + dayName
    private List<FoodEntry> parseWeeklyPlan(String text) {
        List<FoodEntry> entries = new ArrayList<>();
        String currentDay = null;
        String currentMealName = null;
        StringBuilder currentDescription = new StringBuilder();

        LocalDate mondayDate = detectMondayDate(text);

        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            // Dzień tygodnia
            if (isDay(line)) {

                // ZAPISZ POPRZEDNI POSIŁEK Z POPRZEDNIEGO DNIA
                if (currentMealName != null) {
                    String mealType = resolveMealType(currentMealName);
                    LocalDate date = mapDayToDate(currentDay, mondayDate);

                    FoodEntry entry = buildEntry(currentDay, currentMealName, mealType, date, currentDescription.toString().trim());
                    entries.add(entry);
                }

                currentDay = normalizeDay(line);
                currentMealName = null;
                currentDescription = new StringBuilder();
                continue;
            }

            if (currentDay == null) continue;

            // Nazwa posiłku
            String mealName = detectMealName(line);
            if (mealName != null) {

                // ZAPISZ POPRZEDNI POSIŁEK
                if (currentMealName != null) {
                    String mealType = resolveMealType(currentMealName);
                    LocalDate date = mapDayToDate(currentDay, mondayDate);

                    FoodEntry entry = buildEntry(currentDay, currentMealName, mealType, date, currentDescription.toString().trim());
                    entries.add(entry);
                }

                currentMealName = mealName;
                currentDescription = new StringBuilder();

                // opis po myślniku
                if (line.contains("-")) {
                    String[] parts = line.split("-", 2);
                    if (parts.length > 1) {
                        currentDescription.append(parts[1].trim()).append(" ");
                    }
                }

                continue;
            }

            // ignorujemy śmieci
            if (line.equals("Usuń")) continue;
            if (line.equals("Edytuj")) continue;

            // opis
            if (currentMealName != null) {
                currentDescription.append(line).append(" ");
            }
        }

        // ZAPISZ OSTATNI POSIŁEK
        if (currentMealName != null) {
            String mealType = resolveMealType(currentMealName);
            LocalDate date = mapDayToDate(currentDay, mondayDate);

            FoodEntry entry = buildEntry(currentDay, currentMealName, mealType, date, currentDescription.toString().trim());
            entries.add(entry);
        }

        // sortowanie
        entries.sort((a, b) -> mealOrder(a.getMealName()) - mealOrder(b.getMealName()));

        return entries;
    }

    private int mealOrder(String mealName) {
        return switch (mealName) {
            case "Śniadanie" -> 1;
            case "II śniadanie" -> 2;
            case "Zupa" -> 3;
            case "II danie" -> 4;
            case "Podwieczorek" -> 5;
            case "Kolacja" -> 6;
            default -> 99;
        };
    }

    // ⭐ mapowanie dni tygodnia → daty
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

    // ⭐ wykrywanie zakresu dat "10-14.08.2026"
    private LocalDate detectMondayDate(String text) {
        for (String line : text.split("\n")) {
            line = line.trim();

            // usuwamy "r.", kropki i zbędne spacje na końcu
            line = line.replace("r.", "").replace("r", "").trim();

            // teraz obsługujemy format typu: 10-14.08.2026
            if (line.matches("\\d{1,2}-\\d{1,2}\\.\\d{2}\\.\\d{4}")
                    || line.matches("\\d{1,2}-\\d{1,2}\\.\\d{2}\\.\\d{2}")
                    || line.matches("\\d{1,2}-\\d{1,2}\\.\\d{2}\\.\\d{4}")) {

                // przykład: 10-14.08.2026
                // bierzemy pierwszy dzień (10) i miesiąc/rok z końcówki (08.2026)
                String[] parts = line.split("-");
                String startDay = parts[0].trim();          // "10"
                String rightPart = parts[1].trim();         // "14.08.2026"

                // wyciągamy "08.2026" z prawej części
                String[] rightSplit = rightPart.split("\\.");
                // rightSplit: [ "14", "08", "2026" ]
                int day = Integer.parseInt(startDay);
                int month = Integer.parseInt(rightSplit[1]);
                int year = Integer.parseInt(rightSplit[2]);

                return LocalDate.of(year, month, day);
            }

            // prostszy wariant: 10-14.08.2026 (bez "r.")
            if (line.matches("\\d{1,2}-\\d{1,2}\\.\\d{2}\\.\\d{4}")) {
                String[] parts = line.split("-");
                String startDay = parts[0].trim();
                String[] monthYear = parts[1].trim().split("\\.");

                int day = Integer.parseInt(startDay);
                int month = Integer.parseInt(monthYear[1]);
                int year = Integer.parseInt(monthYear[2]);

                return LocalDate.of(year, month, day);
            }
        }

        // fallback — jeśli nic nie pasuje, ale to już awaryjnie
        return LocalDate.now();
    }

    private String detectMealName(String line) {
        line = line.replace("—", "-");

        if (line.startsWith("Śniadanie")) return "Śniadanie";
        if (line.startsWith("II śniadanie")) return "II śniadanie";
        if (line.startsWith("Zupa")) return "Zupa";
        if (line.startsWith("II danie")) return "II danie";
        if (line.startsWith("Podwieczorek")) return "Podwieczorek";

        return null;
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
                || line.startsWith("Piatek")
                || line.startsWith("Piątek");
    }

    private String normalizeDay(String line) {
        if (line.startsWith("Poniedziałek")) return "Poniedziałek";
        if (line.startsWith("Wtorek")) return "Wtorek";
        if (line.startsWith("Środa")) return "Środa";
        if (line.startsWith("Czwartek")) return "Czwartek";
        if (line.startsWith("Piatek") || line.startsWith("Piątek")) return "Piątek";
        return line;
    }

    // ⭐ buildEntry — poprawne ustawianie dayName + date
    private FoodEntry buildEntry(String dayName, String mealName, String mealType, LocalDate date, String description) {
        FoodEntry entry = new FoodEntry();
        entry.setMealName(mealName);
        entry.setMealType(mealType);
        entry.setDayName(dayName);     // <<< DODANE
        entry.setDescription(description);
        entry.setCalories(0);
        entry.setDate(date);           // <<< POPRAWNE USTAWIENIE DATY
        return entry;
    }
}