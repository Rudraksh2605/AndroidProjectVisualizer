
package com.projectvisualizer.services;

import com.projectvisualizer.model.CodeComponent;
import java.util.*;

public class EnhancedComponentCategorizer {

    public static CategorizationResult categorizeWithConfidence(CodeComponent component) {
        String category = ComponentCategorizer.detectCategory(component);
        int confidence = calculateConfidence(component, category);
        return new CategorizationResult(category, confidence);
    }

    public static Map<String, List<CodeComponent>> categorizeBatch(List<CodeComponent> components) {
        Map<String, List<CodeComponent>> result = new HashMap<>();
        for (CodeComponent component : components) {
            String category = ComponentCategorizer.detectCategory(component);
            result.computeIfAbsent(category, k -> new ArrayList<>()).add(component);
        }
        return result;
    }

    private static int calculateConfidence(CodeComponent component, String category) {
        int score = 0;
        return Math.min(100, score);
    }

    public static class CategorizationResult {
        private final String category;
        private final int confidence;

        public CategorizationResult(String category, int confidence) {
            this.category = category;
            this.confidence = confidence;
        }

        public int getConfidence() {
            return confidence;
        }

        public String getCategory() {
            return category;
        }
    }
}
