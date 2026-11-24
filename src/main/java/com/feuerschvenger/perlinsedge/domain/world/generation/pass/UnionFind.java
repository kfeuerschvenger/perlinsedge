package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Objects;

/**
 * Disjoint sets of the integers from 0 up, in two {@code int} arrays, with path halving and union by size. Kruskal's
 * algorithm uses it to tell whether an edge would close a cycle.
 */
final class UnionFind {

    private final int[] parent;
    private final int[] size;
    private int sets;

    /**
     * Creates the sets, each element alone in its own.
     *
     * @param elements how many elements there are
     * @throws IllegalArgumentException when the count is negative
     */
    UnionFind(int elements) {
        if (elements < 0) {
            throw new IllegalArgumentException("the number of elements cannot be negative: " + elements);
        }
        parent = new int[elements];
        size = new int[elements];
        sets = elements;
        for (var element = 0; element < elements; element++) {
            parent[element] = element;
            size[element] = 1;
        }
    }

    /**
     * Returns the representative of the set an element is in.
     *
     * @param element an element from 0
     * @return the representative, which is the same for every element of the set until it is merged
     */
    int find(int element) {
        Objects.checkIndex(element, parent.length);
        var current = element;
        while (parent[current] != current) {
            parent[current] = parent[parent[current]];
            current = parent[current];
        }
        return current;
    }

    /**
     * Merges the sets of two elements.
     *
     * @param first  an element
     * @param second an element
     * @return true when they were in different sets, false when they already were in the same one
     */
    boolean union(int first, int second) {
        var firstRoot = find(first);
        var secondRoot = find(second);
        if (firstRoot == secondRoot) {
            return false;
        }
        if (size[firstRoot] < size[secondRoot]) {
            final var held = firstRoot;
            firstRoot = secondRoot;
            secondRoot = held;
        }
        parent[secondRoot] = firstRoot;
        size[firstRoot] += size[secondRoot];
        sets--;
        return true;
    }

    /**
     * Returns the number of sets left.
     *
     * @return the number of sets, which starts at the number of elements
     */
    int sets() {
        return sets;
    }
}
