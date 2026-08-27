package com.mobilernd.dzienniczek.service;

import com.mobilernd.dzienniczek.model.FoodEntry;
import com.mobilernd.dzienniczek.repository.FoodEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class OcrImportService {

    private final OcrReaderService reader;
    private final OcrParserService parser;
    private final FoodEntryRepository repo;

    public OcrImportService(OcrReaderService reader, OcrParserService parser,
                            FoodEntryRepository repo) {
        this.reader = reader;
        this.parser = parser;
        this.repo = repo;
    }

    public void process(MultipartFile file) {

        String text = reader.read(file);
        List<FoodEntry> entries = parser.parse(text);

        for (FoodEntry e : entries) {
            repo.save(e);
        }
    }
}