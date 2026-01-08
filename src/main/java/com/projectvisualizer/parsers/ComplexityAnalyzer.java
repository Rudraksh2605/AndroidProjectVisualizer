package com.projectvisualizer.parsers;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.projectvisualizer.model.ComplexityInfo;

import java.util.*;

/**
 * Analyzes method complexity by examining AST patterns.
 * Uses heuristics to estimate time and space complexity.
 */
public class ComplexityAnalyzer {

    // Known method patterns and their complexities
    private static final Map<String, String> KNOWN_METHOD_COMPLEXITIES = new HashMap<>();
    static {
        // Sorting methods
        KNOWN_METHOD_COMPLEXITIES.put("sort", "O(n log n)");
        KNOWN_METHOD_COMPLEXITIES.put("parallelSort", "O(n log n)");
        KNOWN_METHOD_COMPLEXITIES.put("Collections.sort", "O(n log n)");
        KNOWN_METHOD_COMPLEXITIES.put("Arrays.sort", "O(n log n)");
        
        // Search methods
        KNOWN_METHOD_COMPLEXITIES.put("binarySearch", "O(log n)");
        KNOWN_METHOD_COMPLEXITIES.put("Arrays.binarySearch", "O(log n)");
        KNOWN_METHOD_COMPLEXITIES.put("Collections.binarySearch", "O(log n)");
        
        // Collection operations
        KNOWN_METHOD_COMPLEXITIES.put("contains", "O(n)");
        KNOWN_METHOD_COMPLEXITIES.put("indexOf", "O(n)");
        KNOWN_METHOD_COMPLEXITIES.put("lastIndexOf", "O(n)");
        
        // HashMap operations (average case)
        KNOWN_METHOD_COMPLEXITIES.put("get", "O(1)");
        KNOWN_METHOD_COMPLEXITIES.put("put", "O(1)");
        KNOWN_METHOD_COMPLEXITIES.put("containsKey", "O(1)");
    }

    /**
     * Analyzes a Java method and returns complexity information.
     */
    public ComplexityInfo analyzeMethod(MethodDeclaration method) {
        ComplexityInfo info = new ComplexityInfo();
        
        if (method == null || !method.getBody().isPresent()) {
            info.setRationale("Empty or abstract method");
            return info;
        }

        // Analyze loop depth
        int maxLoopDepth = analyzeLoopDepth(method);
        info.setLoopDepth(maxLoopDepth);

        // Check for recursion
        boolean hasRecursion = detectRecursion(method);
        info.setHasRecursion(hasRecursion);

        // Analyze space complexity
        String spaceComplexity = analyzeSpaceComplexity(method);
        info.setSpaceComplexity(spaceComplexity);

        // Check for known method calls that affect complexity
        List<String> knownCalls = detectKnownMethodCalls(method);
        
        // Determine time complexity based on all factors
        String timeComplexity = determineTimeComplexity(maxLoopDepth, hasRecursion, knownCalls, info);
        info.setTimeComplexity(timeComplexity);

        // Generate rationale
        info.setRationale(generateRationale(info));

        return info;
    }

    /**
     * Analyzes the maximum loop nesting depth in a method.
     */
    private int analyzeLoopDepth(MethodDeclaration method) {
        LoopDepthVisitor visitor = new LoopDepthVisitor();
        method.accept(visitor, null);
        return visitor.getMaxDepth();
    }

    /**
     * Detects if the method contains recursive calls to itself.
     */
    private boolean detectRecursion(MethodDeclaration method) {
        String methodName = method.getNameAsString();
        RecursionDetector detector = new RecursionDetector(methodName);
        method.accept(detector, null);
        return detector.hasRecursion();
    }

    /**
     * Analyzes space complexity by looking for collection/array allocations.
     */
    private String analyzeSpaceComplexity(MethodDeclaration method) {
        SpaceComplexityVisitor visitor = new SpaceComplexityVisitor();
        method.accept(visitor, null);
        
        if (visitor.hasLoopAllocation()) {
            return "O(n)";
        } else if (visitor.hasCollectionAllocation()) {
            return "O(n)";
        }
        return "O(1)";
    }

    /**
     * Detects calls to known methods with specific complexity.
     */
    private List<String> detectKnownMethodCalls(MethodDeclaration method) {
        List<String> knownCalls = new ArrayList<>();
        
        method.findAll(MethodCallExpr.class).forEach(call -> {
            String callName = call.getNameAsString();
            if (KNOWN_METHOD_COMPLEXITIES.containsKey(callName)) {
                knownCalls.add(callName + ":" + KNOWN_METHOD_COMPLEXITIES.get(callName));
            }
        });
        
        return knownCalls;
    }

    /**
     * Determines overall time complexity based on all analyzed factors.
     */
    private String determineTimeComplexity(int loopDepth, boolean hasRecursion, 
                                           List<String> knownCalls, ComplexityInfo info) {
        String complexity = "O(1)";

        // Check known method calls first
        for (String call : knownCalls) {
            String[] parts = call.split(":");
            if (parts.length == 2) {
                info.addContributor("Method call: " + parts[0] + " → " + parts[1]);
                // Update complexity if this call has worse complexity
                complexity = worseComplexity(complexity, parts[1]);
            }
        }

        // Handle recursion
        if (hasRecursion) {
            info.addContributor("Recursion detected");
            if (loopDepth > 0) {
                info.addContributor("Recursive loops may indicate O(2^n) or worse");
                complexity = worseComplexity(complexity, "O(2ⁿ)");
            } else {
                complexity = worseComplexity(complexity, "O(n)");
            }
        }

        // Handle loop depth
        if (loopDepth > 0) {
            String loopComplexity;
            switch (loopDepth) {
                case 1:
                    loopComplexity = "O(n)";
                    info.addContributor("Single loop");
                    break;
                case 2:
                    loopComplexity = "O(n²)";
                    info.addContributor("Nested loops (depth 2)");
                    break;
                case 3:
                    loopComplexity = "O(n³)";
                    info.addContributor("Nested loops (depth 3)");
                    break;
                default:
                    loopComplexity = "O(n^" + loopDepth + ")";
                    info.addContributor("Nested loops (depth " + loopDepth + ")");
            }
            complexity = worseComplexity(complexity, loopComplexity);
        }

        return complexity;
    }

    /**
     * Compares two complexity strings and returns the worse one.
     */
    private String worseComplexity(String c1, String c2) {
        int rank1 = getComplexityRank(c1);
        int rank2 = getComplexityRank(c2);
        return rank1 >= rank2 ? c1 : c2;
    }

    /**
     * Assigns a numeric rank to complexity for comparison.
     */
    private int getComplexityRank(String complexity) {
        if (complexity == null) return 0;
        if (complexity.contains("n!")) return 100;
        if (complexity.contains("2ⁿ") || complexity.contains("2^n")) return 90;
        if (complexity.contains("n³") || complexity.contains("n^3")) return 60;
        if (complexity.contains("n²") || complexity.contains("n^2")) return 50;
        if (complexity.contains("n log n")) return 35;
        if (complexity.contains("n") && !complexity.contains("log")) return 30;
        if (complexity.contains("log n")) return 20;
        if (complexity.equals("O(1)")) return 10;
        return 0;
    }

    /**
     * Generates a human-readable rationale for the complexity analysis.
     */
    private String generateRationale(ComplexityInfo info) {
        StringBuilder sb = new StringBuilder();
        
        if (info.getLoopDepth() > 0) {
            sb.append("Loop depth: ").append(info.getLoopDepth()).append(". ");
        }
        if (info.isHasRecursion()) {
            sb.append("Contains recursion. ");
        }
        if (!info.getContributors().isEmpty()) {
            sb.append("Contributors: ").append(String.join(", ", info.getContributors()));
        }
        
        if (sb.length() == 0) {
            return "Simple constant-time operations";
        }
        
        return sb.toString().trim();
    }

    // === Inner Visitor Classes ===

    /**
     * Visitor to calculate maximum loop nesting depth.
     */
    private static class LoopDepthVisitor extends VoidVisitorAdapter<Void> {
        private int currentDepth = 0;
        private int maxDepth = 0;

        @Override
        public void visit(ForStmt n, Void arg) {
            currentDepth++;
            maxDepth = Math.max(maxDepth, currentDepth);
            super.visit(n, arg);
            currentDepth--;
        }

        @Override
        public void visit(ForEachStmt n, Void arg) {
            currentDepth++;
            maxDepth = Math.max(maxDepth, currentDepth);
            super.visit(n, arg);
            currentDepth--;
        }

        @Override
        public void visit(WhileStmt n, Void arg) {
            currentDepth++;
            maxDepth = Math.max(maxDepth, currentDepth);
            super.visit(n, arg);
            currentDepth--;
        }

        @Override
        public void visit(DoStmt n, Void arg) {
            currentDepth++;
            maxDepth = Math.max(maxDepth, currentDepth);
            super.visit(n, arg);
            currentDepth--;
        }

        public int getMaxDepth() {
            return maxDepth;
        }
    }

    /**
     * Visitor to detect recursive calls.
     */
    private static class RecursionDetector extends VoidVisitorAdapter<Void> {
        private final String methodName;
        private boolean recursionFound = false;

        public RecursionDetector(String methodName) {
            this.methodName = methodName;
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            if (n.getNameAsString().equals(methodName)) {
                recursionFound = true;
            }
            super.visit(n, arg);
        }

        public boolean hasRecursion() {
            return recursionFound;
        }
    }

    /**
     * Visitor to analyze space complexity.
     */
    private static class SpaceComplexityVisitor extends VoidVisitorAdapter<Void> {
        private boolean collectionAllocation = false;
        private boolean loopAllocation = false;
        private int loopDepth = 0;

        @Override
        public void visit(ForStmt n, Void arg) {
            loopDepth++;
            super.visit(n, arg);
            loopDepth--;
        }

        @Override
        public void visit(ForEachStmt n, Void arg) {
            loopDepth++;
            super.visit(n, arg);
            loopDepth--;
        }

        @Override
        public void visit(WhileStmt n, Void arg) {
            loopDepth++;
            super.visit(n, arg);
            loopDepth--;
        }

        @Override
        public void visit(com.github.javaparser.ast.expr.ObjectCreationExpr n, Void arg) {
            String type = n.getType().asString();
            if (isCollectionType(type)) {
                collectionAllocation = true;
                if (loopDepth > 0) {
                    loopAllocation = true;
                }
            }
            super.visit(n, arg);
        }

        private boolean isCollectionType(String type) {
            return type.contains("List") || type.contains("Set") || type.contains("Map") ||
                   type.contains("Array") || type.contains("Collection") || type.contains("Queue") ||
                   type.contains("Stack") || type.contains("Vector");
        }

        public boolean hasCollectionAllocation() {
            return collectionAllocation;
        }

        public boolean hasLoopAllocation() {
            return loopAllocation;
        }
    }
}
