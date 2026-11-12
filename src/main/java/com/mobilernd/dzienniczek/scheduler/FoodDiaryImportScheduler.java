package com.mobilernd.dzienniczek.scheduler;

import com.mobilernd.dzienniczek.model.ImportStatus;
import com.mobilernd.dzienniczek.repository.ImportStatusRepository;
import com.mobilernd.dzienniczek.service.ProgmanImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class FoodDiaryImportScheduler {

    private final ProgmanImportService progmanImportService;
    private final ImportStatusRepository importStatusRepository;

    @Scheduled(cron = "0 0 2 * * *") // codziennie o 02:00
    public void scheduledImport() {

        try {
            LocalDate fromDate = LocalDate.now().minusDays(1);

            int imported = progmanImportService.importFromDate(fromDate);

            importStatusRepository.save(new ImportStatus("PROGMAN", LocalDateTime.now()));

            log.info("Zaimportowano {} rekordów z Progman od {}", imported, fromDate);

        } catch (Exception e) {
            log.error("Błąd podczas automatycznego importu z Progman", e);
        }
    }
}
