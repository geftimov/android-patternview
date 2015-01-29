package com.eftimoff.patternview.cells;

public interface Manager<T> {

    int getRowCount();

    int getColumnCount();

    T get(final int row, final int column);

    int getSize();

    void clear();

    void draw(T t, final boolean drawn);

    void draw(final int row, final int column, final boolean drawn);
    
    void clearDrawing();

    boolean isDrawn(final int row, final int column);

    boolean isDrawn(T t);
}
