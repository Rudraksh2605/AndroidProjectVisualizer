package com.projectvisualizer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the time and space complexity analysis of a method.
 */
public class ComplexityInfo {
    private String timeComplexity;      // e.g., "O(1)", "O(n)", "O(n²)", "O(n³)", "O(log n)", "O(n log n)"
    private String spaceComplexity;     // e.g., "O(1)", "O(n)"
    private String rationale;           // Explanation of why this complexity was determined
    private int loopDepth;              // Maximum nested loop depth detected
    private boolean hasRecursion;       // Whether recursion was detected
    private List<String> contributors;  // What contributes to complexity (loops, recursion, etc.)

    public ComplexityInfo() {
        this.timeComplexity = "O(1)";
        this.spaceComplexity = "O(1)";
        this.contributors = new ArrayList<>();
    }

    public ComplexityInfo(String timeComplexity, String spaceComplexity) {
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
        this.contributors = new ArrayList<>();
    }

    // Getters and Setters
    public String getTimeComplexity() { return timeComplexity; }
    public void setTimeComplexity(String timeComplexity) { this.timeComplexity = timeComplexity; }

    public String getSpaceComplexity() { return spaceComplexity; }
    public void setSpaceComplexity(String spaceComplexity) { this.spaceComplexity = spaceComplexity; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public int getLoopDepth() { return loopDepth; }
    public void setLoopDepth(int loopDepth) { this.loopDepth = loopDepth; }

    public boolean isHasRecursion() { return hasRecursion; }
    public void setHasRecursion(boolean hasRecursion) { this.hasRecursion = hasRecursion; }

    public List<String> getContributors() { return contributors; }
    public void setContributors(List<String> contributors) { this.contributors = contributors; }

    public void addContributor(String contributor) {
        if (!this.contributors.contains(contributor)) {
            this.contributors.add(contributor);
        }
    }

    /**
     * Returns a severity level for color coding: 1=low, 2=medium, 3=high
     */
    public int getSeverityLevel() {
        if (timeComplexity == null) return 1;
        if (timeComplexity.contains("n³") || timeComplexity.contains("n^3") || 
            timeComplexity.contains("2ⁿ") || timeComplexity.contains("2^n") ||
            timeComplexity.contains("n!")) {
            return 3; // High - red
        } else if (timeComplexity.contains("n²") || timeComplexity.contains("n^2")) {
            return 3; // High - red
        } else if (timeComplexity.contains("n log n") || timeComplexity.contains("n")) {
            return 2; // Medium - yellow
        }
        return 1; // Low - green (O(1), O(log n))
    }

    /**
     * Returns an emoji indicator based on severity
     */
    public String getSeverityEmoji() {
        int level = getSeverityLevel();
        switch (level) {
            case 3: return "🔴";
            case 2: return "🟡";
            default: return "🟢";
        }
    }

    @Override
    public String toString() {
        return String.format("Time: %s, Space: %s", timeComplexity, spaceComplexity);
    }
}
