package com.mobilernd.dzienniczek.service;

public class OcrParserFactory {

    public static OcrParserStrategy choose(String text) {

        String lower = text.toLowerCase();

        if (lower.contains("oxfordzik") || lower.contains("żłobek") || lower.contains("zupa") && lower.contains("ii danie")) {
            return new ZlobekParserService();
        }

        if (lower.contains("przedszkole miejskie") || lower.contains("mali odkrywcy") || lower.contains("alergen")) {
            return new PrzedszkoleParserService();
        }

        // fallback
        return new PrzedszkoleParserService();
    }
}

