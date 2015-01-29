package com.eftimoff.patternview.utils;

public class CellUtils {

    private CellUtils() {

    }

    /**
     * Check if the Cell is in the size of the manager.
     * Throw exception if it is not.
     *
     * @param row
     * @param column
     */
    public static void checkRange(int row, int column) {
        if (row < 0) {
            throw new IllegalArgumentException("row must be in range 0-" + (row - 1));
        }
        if (column < 0) {
            throw new IllegalArgumentException("column must be in range 0-" + (row - 1));
        }
    }
}
