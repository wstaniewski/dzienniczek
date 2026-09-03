package com.mobilernd.dzienniczek.repository;

import com.mobilernd.dzienniczek.model.FoodEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FoodEntryRepository extends JpaRepository<FoodEntry, Long> {

    boolean existsByMealNameAndDate(String mealName, LocalDate date);

    // sortowanie po dacie
    List<FoodEntry> findAllByOrderByDateDesc(); // najnowsze pierwsze

    List<FoodEntry> findByMealNameOrderByDateDesc(String mealName);

    List<FoodEntry> findByMealType(String mealType);

    Optional<FoodEntry> findByDateAndMealName(LocalDate date, String mealName);
}

