package com.projectvisualizer.visualization;

import com.projectvisualizer.model.CodeComponent;
import java.util.*;

public class DependencyGraph {
    // Primary storage for all components (Symbol Table)
    private Map<String, CodeComponent> nodeRegistry = new HashMap<>();

    // Adjacency Lists
    // Key: Source ID -> Value: Set of Target IDs (Outgoing dependencies)
    private Map<String, Set<String>> outgoingEdges = new HashMap<>();

    // Key: Target ID -> Value: Set of Source IDs (Incoming usages - Reverse Graph)
    private Map<String, Set<String>> incomingEdges = new HashMap<>();

    public void addComponent(CodeComponent component) {
        nodeRegistry.put(component.getId(), component);
        // Also map by simple name for fuzzy lookups if ID is unique
        if (component.getName() != null) {
            // This is a helper, collisions might happen but it's useful for fuzzy matching
            // We don't overwrite if ID matches to preserve canonical entry
        }
    }

    public CodeComponent getComponent(String id) {
        return nodeRegistry.get(id);
    }

    public Collection<CodeComponent> getAllComponents() {
        return nodeRegistry.values();
    }

    public void addEdge(String sourceId, String targetId) {
        if (!nodeRegistry.containsKey(sourceId) || !nodeRegistry.containsKey(targetId)) return;

        outgoingEdges.computeIfAbsent(sourceId, k -> new HashSet<>()).add(targetId);
        incomingEdges.computeIfAbsent(targetId, k -> new HashSet<>()).add(sourceId);
    }

    public Set<CodeComponent> getNeighbors(String sourceId) {
        Set<String> targetIds = outgoingEdges.getOrDefault(sourceId, Collections.emptySet());
        Set<CodeComponent> neighbors = new HashSet<>();
        for (String id : targetIds) {
            CodeComponent c = nodeRegistry.get(id);
            if (c != null) neighbors.add(c);
        }
        return neighbors;
    }

    public Set<CodeComponent> getIncomingNeighbors(String targetId) {
        Set<String> sourceIds = incomingEdges.getOrDefault(targetId, Collections.emptySet());
        Set<CodeComponent> sources = new HashSet<>();
        for (String id : sourceIds) {
            CodeComponent c = nodeRegistry.get(id);
            if (c != null) sources.add(c);
        }
        return sources;
    }

    public void clear() {
        nodeRegistry.clear();
        outgoingEdges.clear();
        incomingEdges.clear();
    }
}
