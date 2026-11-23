package com.mobilernd.dzienniczek.controller;

import com.mobilernd.dzienniczek.model.FoodEntry;
import com.mobilernd.dzienniczek.model.ImportStatus;
import com.mobilernd.dzienniczek.repository.ImportStatusRepository;
import com.mobilernd.dzienniczek.service.FoodEntryService;
import com.mobilernd.dzienniczek.service.OcrImportService;
import com.mobilernd.dzienniczek.service.ProgmanImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@Slf4j
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
        LocalDateTime lastImportDate = importStatusRepository.findById("PROGMAN")
                .map(ImportStatus::getLastImportDate)
                .orElse(null);
        model.addAttribute("lastImportDate", lastImportDate);

        model.addAttribute("foodEntry", new FoodEntry());
        model.addAttribute("entriesByDate", service.entriesByDate(service.findAllSorted()));
        model.addAttribute("selectedFilter", "Wszystkie");

        return "index";
    }

    @PostMapping("/save")
    public String save(FoodEntry entry, Model model) {

        entry.setMealType(entry.getMealName().toLowerCase());

        if (!service.isMealUniqueForDay(entry)) {
            model.addAttribute("foodEntry", entry);
            model.addAttribute("entriesByDate", service.entriesByDate(service.findAllSorted()));
            model.addAttribute("error", "Na ten dzień taki posiłek już istnieje.");
            return "index";
        }
        entry.setDayName(
                switch (entry.getDate().getDayOfWeek()) {
                    case MONDAY -> "Poniedziałek";
                    case TUESDAY -> "Wtorek";
                    case WEDNESDAY -> "Środa";
                    case THURSDAY -> "Czwartek";
                    case FRIDAY -> "Piątek";
                    case SATURDAY -> "Sobota";
                    case SUNDAY -> "Niedziela";
                }
        );

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
        System.out.println("ID=" + entry.getId());
        System.out.println("mealName=" + entry.getMealName());
        System.out.println("mealType=" + entry.getMealType());
        System.out.println("dayName=" + entry.getDayName());
        System.out.println("date=" + entry.getDate());

        FoodEntry existing = service.findById(entry.getId());

        existing.setMealName(entry.getMealName());
        existing.setCalories(entry.getCalories());
        existing.setDate(entry.getDate());
        existing.setDescription(entry.getDescription());

        if (!service.isMealUniqueForDay(existing)) {
            model.addAttribute("foodEntry", existing);
            model.addAttribute(
                    "error",
                    "Na ten dzień taki posiłek już istnieje."
            );
            return "index";
        }

        service.save(existing);

        return "redirect:/";
    }

    @GetMapping("/filter")
    public String filter(@RequestParam String meal, Model model) {
        model.addAttribute("foodEntry", new FoodEntry());
        model.addAttribute("entriesByDate", service.entriesByDate(service.filterByMeal(meal)));
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

    @PostMapping("/progman/import-auto")
    public ResponseEntity<String> importAuto() {

        Optional<ImportStatus> status = importStatusRepository.findById("PROGMAN");
        if (status.isPresent() && status.get().getLastImportDate().isAfter(LocalDateTime.now().minusHours(23))) {

            log.info("Import już był wykonany");
            return ResponseEntity.ok("SKIPPED");
        }

        LocalDateTime now = LocalDateTime.now();
        int imported = progmanImportService.importFromDate(now.toLocalDate().minusDays(1));
        importStatusRepository.save(new ImportStatus("PROGMAN", now));
        return ResponseEntity.ok("Zaimportowano " + imported);
    }

    @PostMapping("/reset")
    @ResponseBody
    public String resetAll() {
        service.deleteAll();
        return "OK";
    }
}