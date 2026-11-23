package com.mobilernd.dzienniczek.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
public class FoodEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mealName;

    private String mealType;   // typ posiłku (śniadanie / obiad / przekąska / inne)

    private String dayName;    // NOWE POLE — dzień tygodnia (Poniedziałek, Wtorek...)

    private Integer calories;

    private LocalDate date;

    private LocalTime mealTime;

    @Column(length = 1000)
    private String description;

    public FoodEntry() {}

    public FoodEntry(String mealName, String mealType, String dayName,
                     Integer calories, LocalDate date, LocalTime mealTime, String description) {
        this.mealName = mealName;
        this.mealType = mealType;
        this.dayName = dayName;
        this.calories = calories;
        this.date = date;
        this.mealTime = mealTime;
        this.description = description;
    }

    public Long getId() { return id; }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMealName() { return mealName; }
    public void setMealName(String mealName) { this.mealName = mealName; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }

    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getMealTime() {return mealTime;}

    public void setMealTime(LocalTime mealTime) {this.mealTime = mealTime;}

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getMealOrder() {

        return switch (getMealName()) {

            case "Pierwsze śniadanie" -> 10;
            case "Śniadanie" -> 20;
            case "Obiad" -> 30;
            case "Podwieczorek" -> 40;
            case "Kolacja" -> 50;

            case "Przekąska" -> 100;
            case "Inne" -> 110;

            default -> 999;
        };
    }
}