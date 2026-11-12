package com.mobilernd.dzienniczek.controller;

import com.mobilernd.dzienniczek.model.FoodEntry;
import com.mobilernd.dzienniczek.model.ImportStatus;
import com.mobilernd.dzienniczek.repository.ImportStatusRepository;
import com.mobilernd.dzienniczek.service.FoodEntryService;
import com.mobilernd.dzienniczek.service.OcrImportService;
import com.mobilernd.dzienniczek.service.ProgmanImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class FoodEntryController {

    private final FoodEntryService service;
    private final OcrImportService ocrImportService;
    private final ProgmanImportService progmanImportService;
    private final ImportStatusRepository importStatusRepository;

    public FoodEntryController(FoodEntryService service, OcrImportService ocrImportService, ProgmanImportService progmanImportService, ImportStatusRepository importStatusRepository) {
        this.service = service;
        this.ocrImportService = ocrImportService;
        this.progmanImportService = progmanImportService;
        this.importStatusRepository = importStatusRepository;
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

        LocalDateTime lastImportDate = importStatusRepository.findById("PROGMAN")
                .map(ImportStatus::getLastImportDate)
                .orElse(null);
        model.addAttribute("lastImportDate", lastImportDate);

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

    @PostMapping("/jadlospis/importWebsite")
    public ResponseEntity<Void> importWebsite() {
        try {
            ocrImportService.processFromWebsite();
            return ResponseEntity.ok().build(); // ✔ 200 OK — brak redirectu
        } catch (Exception e) {
            return ResponseEntity.status(500).build(); // ✔ 500 — JS pokaże Snackbar
        }
    }

    @PostMapping("/progman/import")
    @ResponseBody
    public String importProgman(@RequestParam String fromDate) {

        try {
            int imported = progmanImportService.importFromDate(LocalDate.parse(fromDate));
            LocalDateTime lastImportDate = LocalDateTime.now();
            importStatusRepository.save(new ImportStatus("PROGMAN", lastImportDate));
            return "Zaimportowano: " + imported;

        } catch (Exception e) {
            e.printStackTrace();
            return e.getClass().getName()
                    + " -> "
                    + e.getMessage();
        }
    }

    @PostMapping("/reset")
    @ResponseBody
    public String resetAll() {

        service.deleteAll();

        return "OK";
    }

/*    @GetMapping("/food-diary")
    public String foodDiary(Model model) {
        LocalDateTime lastImportDate = importStatusRepository.findById("PROGMAN")
                .map(ImportStatus::getLastImportDate)
                .orElse(null);
        model.addAttribute("lastImportDate", lastImportDate);
        return "index";
    }*/
}