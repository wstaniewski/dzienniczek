package com.mobilernd.dzienniczek.service;

import com.mobilernd.dzienniczek.model.FoodEntry;
import com.mobilernd.dzienniczek.repository.FoodEntryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FoodEntryService {

    private final FoodEntryRepository foodEntryRepository;

    public FoodEntryService(FoodEntryRepository foodEntryRepository) {
        this.foodEntryRepository = foodEntryRepository;
    }

    public void save(FoodEntry entry) {

        if (entry.getMealName() != null) {
            entry.setMealType(entry.getMealName().toLowerCase());

            switch (entry.getMealName()) {
                case "Śniadanie" ->
                        entry.setMealTime(LocalTime.of(8,30));
                case "Zupa" ->
                        entry.setMealTime(LocalTime.of(11,0));
                case "Drugie danie" ->
                        entry.setMealTime(LocalTime.of(12,0));
                case "Podwieczorek" ->
                        entry.setMealTime(LocalTime.of(14,30));
            }
        }

        if (entry.getDate() != null) {
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
        }

        foodEntryRepository.save(entry);
    }

    public List<FoodEntry> findAllSorted() {
        return foodEntryRepository.findAllByOrderByDateDescMealTimeAsc(); // lub Asc
    }

    public List<FoodEntry> filterByMeal(String mealName) {
        if (mealName == null || mealName.equals("Wszystkie")) {
            return findAllSorted();
        }
        return foodEntryRepository.findByMealType(mealName);
    }

    public Map<LocalDate, List<FoodEntry>> entriesByDate(List<FoodEntry> entries) {

        return entries.stream()
                .collect(Collectors.groupingBy(FoodEntry::getDate))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<LocalDate, List<FoodEntry>>comparingByKey().reversed())
                .peek(day ->
                        day.getValue().sort(
                                Comparator.comparing(
                                        FoodEntry::getMealTime,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder()
                                        )
                                )
                        )
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public void delete(Long id) {
        foodEntryRepository.deleteById(id);
    }

    // NOWE: walidacja unikalności posiłku
    public boolean isMealUniqueForDay(FoodEntry entry) {

        // przekąski i inne mogą być wielokrotne
        if (entry.getMealName().equals("Przekąska") || entry.getMealName().equals("Inne")) {
            return true;
        }

        boolean exists = foodEntryRepository.existsByMealNameAndDate(entry.getMealName(), entry.getDate());

        // jeśli edytujemy istniejący wpis, nie traktujemy go jako duplikat
        if (entry.getId() != null) {
            FoodEntry original = foodEntryRepository.findById(entry.getId()).orElse(null);
            if (original != null &&
                    original.getMealName().equals(entry.getMealName()) &&
                    original.getDate().equals(entry.getDate())) {
                return true;
            }
        }

        return !exists;
    }

    public FoodEntry findById(Long id) {
        return foodEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono wpisu: " + id));
    }

    public Map<LocalDate, List<FoodEntry>> findAllGroupedByDate() {
        List<FoodEntry> all = foodEntryRepository.findAllByOrderByDateDesc();
        return all.stream()
                .collect(Collectors.groupingBy(FoodEntry::getDate,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    public void deleteAll() {
        foodEntryRepository.deleteAll();
    }

    private int mealOrder(FoodEntry entry) {
        return switch (entry.getMealName()) {
            case "Pierwsze śniadanie" -> 10;
            case "Śniadanie" -> 20;
            case "Zupa" -> 30;
            case "Drugie danie" -> 40;
            case "Podwieczorek" -> 50;
            case "Kolacja" -> 60;
            case "Przekąska" -> 100;
            default -> 999;
        };
    }
}

