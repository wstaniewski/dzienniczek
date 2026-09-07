package com.mobilernd.dzienniczek.service;

import com.mobilernd.dzienniczek.model.FoodEntry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OcrParserService {

    public List<FoodEntry> parse(String text) {
        OcrParserStrategy strategy = OcrParserFactory.choose(text);
        return strategy.parse(text);
    }
}