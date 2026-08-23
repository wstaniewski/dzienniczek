package com.mobilernd.dzienniczek.controller;

import com.mobilernd.dzienniczek.model.FoodEntry;
import com.mobilernd.dzienniczek.service.FoodEntryService;
import com.mobilernd.dzienniczek.service.OcrImportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Comparator;

@Controller
public class FoodEntryController {

    private final FoodEntryService service;
    private final OcrImportService ocrImportService;

    public FoodEntryController(FoodEntryService service, OcrImportService ocrImportService) {
        this.service = service;
        this.ocrImportService = ocrImportService;
    }

    @GetMapping("/")
    public String index(Model model) {

        // ⭐ Pobieramy posortowane wpisy
        List<FoodEntry> entries = service.findAllSorted();

        // ⭐ Grupowanie po dacie + sortowanie dat malejąco
        Map<LocalDate, List<FoodEntry>> entriesByDate = entries.stream()
                .collect(Collectors.groupingBy(FoodEntry::getDate))
                .entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<FoodEntry>>comparingByKey().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new
                ));

        model.addAttribute("foodEntry", new FoodEntry());
        model.addAttribute("entriesByDate", entriesByDate);
        model.addAttribute("selectedFilter", "Wszystkie");

        return "index";
    }

    @PostMapping("/save")
    public String save(FoodEntry entry, Model model) {

        if (!service.isMealUniqueForDay(entry)) {

            List<FoodEntry> entries = service.findAllSorted();

            Map<LocalDate, List<FoodEntry>> entriesByDate = entries.stream()
                    .collect(Collectors.groupingBy(FoodEntry::getDate))
                    .entrySet().stream()
                    .sorted(Map.Entry.<LocalDate, List<FoodEntry>>comparingByKey().reversed())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a,
                            java.util.LinkedHashMap::new
                    ));

            model.addAttribute("foodEntry", entry);
            model.addAttribute("entriesByDate", entriesByDate);
            model.addAttribute("error", "Na ten dzień taki posiłek już istnieje.");

            return "index";
        }

        service.save(entry);
        return "redirect:/";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {

        FoodEntry entry = service.findById(id);

        // dane do sekcji edycji
        model.addAttribute("editEntry", entry);
        model.addAttribute("openEdit", true);

        // dane potrzebne do index.html
        model.addAttribute("foodEntry", new FoodEntry());
        model.addAttribute("entriesByDate", service.findAllGroupedByDate());
        model.addAttribute("selectedFilter", "Wszystkie");

        return "index";
    }


    @PostMapping("/update")
    public String update(FoodEntry entry, Model model) {

        if (!service.isMealUniqueForDay(entry)) {
            model.addAttribute("foodEntry", entry);
            model.addAttribute("error", "Na ten dzień taki posiłek już istnieje.");
            return "index";
        }

        service.save(entry);
        return "redirect:/";
    }

    @GetMapping("/filter")
    public String filter(@RequestParam String meal, Model model) {

        List<FoodEntry> entries = service.filterByMeal(meal);

        // ⭐ Grupowanie + sortowanie dat malejąco również w filtrze
        Map<LocalDate, List<FoodEntry>> entriesByDate = entries.stream()
                .collect(Collectors.groupingBy(FoodEntry::getDate))
                .entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<FoodEntry>>comparingByKey().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new
                ));

        model.addAttribute("foodEntry", new FoodEntry());
        model.addAttribute("entriesByDate", entriesByDate);
        model.addAttribute("selectedFilter", meal);

        return "index";
    }

    @PostMapping("/import/photo")
    public String importFromPhoto(@RequestParam("file") MultipartFile file) {
        ocrImportService.process(file);
        return "redirect:/";
    }
}