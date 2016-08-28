package com.eftimoff.patternview.cells;

import android.os.Parcel;
import android.os.Parcelable;

import com.eftimoff.patternview.utils.CellUtils;

/**
 * Class representing an object in specific position.
 */
public class Cell implements Parcelable {

    private int row;
    private int column;

    /**
     * @param row    The row of the cell.
     * @param column The column of the cell.
     */
    public Cell(int row, int column) {
        CellUtils.checkRange(row, column);
        this.row = row;
        this.column = column;
    }

    /**
     * Gets the row index.
     *
     * @return the row index.
     */
    public int getRow() {
        return row;
    }

    /**
     * Gets the column index.
     *
     * @return the column index.
     */
    public int getColumn() {
        return column;
    }

    /**
     * Gets the ID.It is counted from left to right, top to bottom of the
     * matrix, starting by zero.
     *
     * @return the ID.
     */
    public String getId() {
        final String formatRow = String.format("%03d", row);
        final String formatColumn = String.format("%03d", column);
        return formatRow + "-" + formatColumn;
    }


    @Override
    public String toString() {
        return "(r=" + getRow() + ",c=" + getColumn() + ")";
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Cell) {
            return getColumn() == ((Cell) object).getColumn() && getRow() == ((Cell) object).getRow();
        }
        return super.equals(object);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(getColumn());
        parcel.writeInt(getRow());
    }


    public void readFromParcel(Parcel in) {
        column = in.readInt();
        row = in.readInt();
    }

    public static final Parcelable.Creator<Cell> CREATOR = new Parcelable.Creator<Cell>() {

        public Cell createFromParcel(Parcel in) {
            return new Cell(in);
        }

        public Cell[] newArray(int size) {
            return new Cell[size];
        }
    };

    private Cell(Parcel in) {
        readFromParcel(in);
    }
}
