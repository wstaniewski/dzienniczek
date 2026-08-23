package com.mobilernd.dzienniczek.repository;

import com.mobilernd.dzienniczek.model.FoodEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FoodEntryRepository extends JpaRepository<FoodEntry, Long> {

    boolean existsByMealNameAndDate(String mealName, LocalDate date);

    // sortowanie po dacie
    List<FoodEntry> findAllByOrderByDateDesc(); // najnowsze pierwsze

    List<FoodEntry> findByMealNameOrderByDateDesc(String mealName);
}

