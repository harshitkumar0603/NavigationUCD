package com.project.android.test;

import java.util.HashSet;
import java.util.Set;

//Graph of the floors containing all the nodes
public class Graph {
    private Set<Node> nodes = new HashSet<>();

    public void addNode(Node nodeA) {
        nodes.add(nodeA);
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public void setNodes(Set<Node> nodes) {
        this.nodes = nodes;
    }

    // getters and setters
}

