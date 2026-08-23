package com.mobilernd.dzienniczek.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class FoodEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mealName;
    private Integer calories;
    private LocalDate date;

    @Column(length = 1000)
    private String description;

    public FoodEntry() {}

    public FoodEntry(String mealName, Integer calories, LocalDate date, String description) {
        this.mealName = mealName;
        this.calories = calories;
        this.date = date;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getMealName() { return mealName; }
    public void setMealName(String mealName) { this.mealName = mealName; }
    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
