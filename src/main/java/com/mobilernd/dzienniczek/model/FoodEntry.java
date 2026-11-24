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

    private String mealType;   // typ posiłku (śniadanie / obiad / przekąska / podwieczorek / inne)

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

    public String getIconPath() {

        if (mealType == null) {
            throw new IllegalStateException("mealType jest null dla wpisu id=" + id + ", mealName=" + mealName);
        }

        return switch (mealType) {
            case "pierwsze śniadanie" -> "/icons/firstbreakfast.svg";
            case "śniadanie" -> "/icons/breakfast.svg";
            case "zupa" -> "/icons/soup.svg";
            case "drugie danie" -> "/icons/secondcourse.svg";
            case "podwieczorek" -> "/icons/afternoonsnack.svg";
            case "kolacja" -> "/icons/dinner.svg";
            case "przekąska" -> "/icons/snack.svg";

            default -> throw new IllegalStateException("Nieobsługiwany mealType: " + mealType);
        };
    }

    public String getCssClass() {

        if (mealType == null) {
            throw new IllegalStateException("mealType jest null dla wpisu id=" + id + ", mealName=" + mealName);
        }

        return switch (mealType) {
            case "pierwsze śniadanie" -> "meal-first-breakfast";
            case "śniadanie" -> "meal-breakfast";
            case "zupa" -> "meal-soup";
            case "drugie danie" -> "meal-second-course";
            case "podwieczorek" -> "meal-afternoon-snack";
            case "kolacja" -> "meal-dinner";
            case "przekąska" -> "meal-snack";
            default -> throw new IllegalStateException("Nieobsługiwany mealType: " + mealType);
        };
    }
}