package com.mobilernd.dzienniczek.service;

import com.mobilernd.dzienniczek.model.FoodEntry;
import com.mobilernd.dzienniczek.repository.FoodEntryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FoodEntryService {

    private final FoodEntryRepository repo;

    public FoodEntryService(FoodEntryRepository repo) {
        this.repo = repo;
    }

    public void save(FoodEntry entry) {
        repo.save(entry);
    }

    public List<FoodEntry> findAllSorted() {
        return repo.findAllByOrderByDateDesc(); // lub Asc
    }

    public List<FoodEntry> filterByMeal(String mealName) {
        if (mealName == null || mealName.equals("Wszystkie")) {
            return repo.findAllByOrderByDateDesc();
        }
        return repo.findByMealNameOrderByDateDesc(mealName);
    }

    public FoodEntry findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    // NOWE: walidacja unikalności posiłku
    public boolean isMealUniqueForDay(FoodEntry entry) {

        // przekąski i inne mogą być wielokrotne
        if (entry.getMealName().equals("Przekąska") || entry.getMealName().equals("Inne")) {
            return true;
        }

        boolean exists = repo.existsByMealNameAndDate(entry.getMealName(), entry.getDate());

        // jeśli edytujemy istniejący wpis, nie traktujemy go jako duplikat
        if (entry.getId() != null) {
            FoodEntry original = repo.findById(entry.getId()).orElse(null);
            if (original != null &&
                    original.getMealName().equals(entry.getMealName()) &&
                    original.getDate().equals(entry.getDate())) {
                return true;
            }
        }

        return !exists;
    }

    public Map<LocalDate, List<FoodEntry>> findAllGroupedByDate() {
        List<FoodEntry> all = repo.findAllByOrderByDateDesc();
        return all.stream()
                .collect(Collectors.groupingBy(FoodEntry::getDate,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }
}

