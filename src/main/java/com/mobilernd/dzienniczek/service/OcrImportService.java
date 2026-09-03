package com.mobilernd.dzienniczek.service;

import com.mobilernd.dzienniczek.model.FoodEntry;
import com.mobilernd.dzienniczek.repository.FoodEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class OcrImportService {

    private final OcrReaderService reader;
    private final OcrParserService parser;
    private final FoodEntryRepository repo;
    private final KidsviewMenuFetcher kidsviewMenuFetcher;

    public OcrImportService(OcrReaderService reader,
                            OcrParserService parser,
                            FoodEntryRepository repo,
                            KidsviewMenuFetcher kidsviewMenuFetcher) {
        this.reader = reader;
        this.parser = parser;
        this.repo = repo;
        this.kidsviewMenuFetcher = kidsviewMenuFetcher;
    }

    public void process(MultipartFile file) {
        String text = reader.read(file);
        log.info("=== PARSER INPUT TEXT ===\n{}", text);
        saveEntries(text);
    }


    public void processFromWebsite() {
        try {
            saveEntries(kidsviewMenuFetcher.fetchMenuText());
        } catch (Exception e) {
            throw new RuntimeException("Nie udało się pobrać jadłospisu", e);
        }
    }

    private void saveEntries(String text) {
        List<FoodEntry> newEntries = parser.parse(text);

        for (FoodEntry e : newEntries) {
            // sprawdź czy taki wpis już istnieje
            Optional<FoodEntry> existing = repo.findByDateAndMealName(e.getDate(), e.getMealName());

            if (existing.isPresent()) {
                // aktualizuj opis zamiast nadpisywać cały dzień
                FoodEntry old = existing.get();
                old.setDescription(old.getDescription() + "\n" + e.getDescription());
                repo.save(old);
            } else {
                // dodaj nowy wpis
                repo.save(e);
            }
        }
    }

}