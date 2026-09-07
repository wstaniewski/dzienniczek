package com.mobilernd.dzienniczek.service;

import com.mobilernd.dzienniczek.model.FoodEntry;

import java.util.List;

public interface OcrParserStrategy {
    List<FoodEntry> parse(String text);
}

