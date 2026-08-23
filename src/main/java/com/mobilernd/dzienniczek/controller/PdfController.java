package com.mobilernd.dzienniczek.controller;

import com.mobilernd.dzienniczek.model.FoodEntry;
import com.mobilernd.dzienniczek.repository.FoodEntryRepository;
import com.mobilernd.dzienniczek.service.PdfService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pdf")
public class PdfController {

    private final PdfService pdfService;
    private final FoodEntryRepository foodEntryRepository;

    public PdfController(PdfService pdfService, FoodEntryRepository foodEntryRepository) {
        this.pdfService = pdfService;
        this.foodEntryRepository = foodEntryRepository;
    }

    @GetMapping("/food-entries")
    public ResponseEntity<byte[]> getFoodEntriesPdf() {
        List<FoodEntry> entries = foodEntryRepository.findAll();
        byte[] pdfBytes = pdfService.generateFoodEntriesPdf(entries);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=dzienniczek.pdf")
                .body(pdfBytes);
    }
}