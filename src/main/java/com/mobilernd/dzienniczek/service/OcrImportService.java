package com.mobilernd.dzienniczek.service;

import com.mobilernd.dzienniczek.model.FoodEntry;
import com.mobilernd.dzienniczek.repository.FoodEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
        } catch (Exception e){
            throw new RuntimeException("Nie udało się pobrać jadłospisu", e);
        }
    }

    private void saveEntries(String text) {
        List<FoodEntry> entries = parser.parse(text);
        for (FoodEntry e : entries) {
            repo.save(e);
        }
    }
}