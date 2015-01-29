package com.eftimoff.patternview.cells;

public interface Manager<T> {

    int getRowCount();

    int getColumnCount();

    T get(final int row, final int column);

    int getSize();

    void clear();

    void draw(T t);

    void draw(final int row, final int column);

    boolean isDrawn(final int row, final int column);

    boolean isDrawn(T t);
}
