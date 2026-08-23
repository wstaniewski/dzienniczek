package com.mobilernd.dzienniczek.model;

import jakarta.persistence.*;
import java.time.LocalDate;

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

    @Column(length = 1000)
    private String description;

    public FoodEntry() {}

    public FoodEntry(String mealName, String mealType, String dayName,
                     Integer calories, LocalDate date, String description) {
        this.mealName = mealName;
        this.mealType = mealType;
        this.dayName = dayName;
        this.calories = calories;
        this.date = date;
        this.description = description;
    }

    public Long getId() { return id; }

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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}