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

    // Tworzenie klienta Google Vision z pliku JSON (bez ADC)
    private ImageAnnotatorClient createVisionClient() throws Exception {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream("src/main/resources/google-vision-key.json"))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        return ImageAnnotatorClient.create(settings);
    }

    // Główna metoda importu z MultipartFile
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

    // OCR Google Vision dla File
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

    // Parser tygodniowego jadłospisu z Vision OCR
    private List<FoodEntry> parseWeeklyPlan(String text) {
        List<FoodEntry> entries = new ArrayList<>();
        String currentDay = null;

        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            // Dni tygodnia
            if (isDay(line)) {
                currentDay = normalizeDay(line);
                continue;
            }

            if (currentDay == null) continue;

            // Wykrycie posiłku
            String mealName = detectMealName(line);
            if (mealName != null) {
                String description = extractAfterSeparator(line);
                String mealType = resolveMealType(mealName);

                entries.add(buildEntry(currentDay, mealName, mealType, description));
            }
        }

        return entries;
    }

    // Wykrywanie nazwy posiłku na podstawie linii
    private String detectMealName(String line) {
        if (line.startsWith("Śniadanie")) return "Śniadanie";
        if (line.startsWith("II śniadanie")) return "II śniadanie";
        if (line.startsWith("Zupa")) return "Zupa";
        if (line.startsWith("II danie")) return "II danie";
        if (line.startsWith("Podwieczorek")) return "Podwieczorek";
        return null;
    }

    // Mapowanie nazwy posiłku na typ (śniadanie / obiad / przekąska)
    private String resolveMealType(String mealName) {
        return switch (mealName) {
            case "Śniadanie", "II śniadanie" -> "śniadanie";
            case "Zupa", "II danie" -> "obiad";
            case "Podwieczorek" -> "przekąska";
            default -> "inne";
        };
    }

    // Normalizacja dnia tygodnia (na razie tylko nazwa, później można mapować na datę)
    private String normalizeDay(String line) {
        if (line.startsWith("Poniedziałek")) return "Poniedziałek";
        if (line.startsWith("Wtorek")) return "Wtorek";
        if (line.startsWith("Środa")) return "Środa";
        if (line.startsWith("Czwartek")) return "Czwartek";
        if (line.startsWith("Piątek")) return "Piątek";
        return line;
    }

    // Wyciąganie opisu po separatorze "-"
    private String extractAfterSeparator(String line) {
        int idx = line.indexOf("-");
        if (idx >= 0 && idx + 1 < line.length()) {
            return line.substring(idx + 1).trim();
        }
        return "";
    }

    // Sprawdzanie, czy linia jest dniem tygodnia
    private boolean isDay(String line) {
        return line.startsWith("Poniedziałek")
                || line.startsWith("Wtorek")
                || line.startsWith("Środa")
                || line.startsWith("Czwartek")
                || line.startsWith("Piątek");
    }

    // Budowanie wpisu z pełnym zestawem danych
    private FoodEntry buildEntry(String dayName, String mealName, String mealType, String description) {
        FoodEntry entry = new FoodEntry();
        entry.setMealName(mealName);
        entry.setMealType(mealType);
        entry.setDescription(description);
        entry.setCalories(0);
        entry.setDate(LocalDate.now()); // później można podmienić na datę z nagłówka jadłospisu
        return entry;
    }
}