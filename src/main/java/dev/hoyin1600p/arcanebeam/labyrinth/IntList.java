package dev.hoyin1600p.arcanebeam.labyrinth;

import java.util.Arrays;

/** A minimal growable int array, so the graph builders do not box. */
final class IntList {
    private int[] data;
    private int size;

    IntList() {
        this(16);
    }

    IntList(int capacity) {
        data = new int[Math.max(4, capacity)];
    }

    void add(int value) {
        if (size == data.length) {
            data = Arrays.copyOf(data, size * 2);
        }
        data[size++] = value;
    }

    int get(int index) {
        return data[index];
    }

    void set(int index, int value) {
        data[index] = value;
    }

    int size() {
        return size;
    }

    boolean isEmpty() {
        return size == 0;
    }

    void clear() {
        size = 0;
    }

    int last() {
        return data[size - 1];
    }

    int removeLast() {
        return data[--size];
    }

    int[] toArray() {
        return Arrays.copyOf(data, size);
    }

    boolean contains(int value) {
        for (int i = 0; i < size; i++) {
            if (data[i] == value) {
                return true;
            }
        }
        return false;
    }
}
