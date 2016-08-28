package com.eftimoff.patternview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.Debug;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.eftimoff.mylibrary.R;
import com.eftimoff.patternview.cells.Cell;
import com.eftimoff.patternview.cells.CellManager;
import com.eftimoff.patternview.utils.CellUtils;

import java.util.ArrayList;
import java.util.List;

public class PatternView extends View {

    /**
     * The width of the matrix.
     */
    private int gridColumns;
    /**
     * The width of the matrix.
     */
    private int gridRows;
    /**
     * The maximum size when it is used wrap content.
     */
    private int circleSize;
    /**
     * Manager for the cells.
     */
    private CellManager cellManager;
    /**
     * The paint the will draw the path.
     */
    private final Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /**
     * The color of the path.
     */
    private int pathColor;
    /**
     * The paint of the circle.
     */
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /**
     * The color of the circle.
     */
    private int circleColor;
    /**
     * The paint of the dot.
     */
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /**
     * The color of the dot.
     */
    private int dotColor;

    private static final boolean PROFILE_DRAWING = false;
    private boolean drawingProfilingStarted = false;

    /**
     * How many milliseconds we spend animating each circle of a lock pattern if
     * the animating mode is set. The entire animation should take this constant
     * * the length of the pattern to complete.
     */
    private static final int MILLIS_PER_CIRCLE_ANIMATING = 700;

    private OnPatternStartListener onPatternStartListener;
    private OnPatternClearedListener onPatternClearedListener;
    private OnPatternCellAddedListener onPatternCellAddedListener;
    private OnPatternDetectedListener onPatternDetectedListener;


    private ArrayList<Cell> mPattern;


    /**
     * the in progress point: - during interaction: where the user's finger is -
     * during animation: the current tip of the animating line
     */
    private float inProgressX = -1;
    private float inProgressY = -1;

    private long animatingPeriodStart;

    private DisplayMode patternDisplayMode = DisplayMode.Correct;
    private boolean inputEnabled = true;
    private boolean inStealthMode = false;
    private boolean inErrorStealthMode = false;
    private boolean enableHapticFeedback = true;
    private boolean patternInProgress = false;

    private final float diameterFactor = 0.10f;
    private final float hitFactor = 0.6f;

    private float squareWidth;
    private float squareHeight;

    private Bitmap bitmapBtnDefault;
    private Bitmap bitmapBtnTouched;
    private Bitmap bitmapCircleDefault;
    private Bitmap bitmapCircleSelected;
    private Bitmap bitmapCircleRed;

    private final Path currentPath = new Path();
    private final Rect invalidate = new Rect();

    private int bitmapWidth;
    private int bitmapHeight;

    private final Matrix circleMatrix = new Matrix();

    private final int padding = 0;
    private final int paddingLeft = padding;
    private final int paddingTop = padding;

    public PatternView(Context context) {
        this(context, null);
    }

    public PatternView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PatternView(Context context, AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        getFromAttributes(context, attrs);
        init();
        pathPaint.setDither(true);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        loadBitmaps();
    }

    private void getFromAttributes(Context context, AttributeSet attrs) {
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PatternView);
        try {
            circleSize = typedArray.getDimensionPixelSize(R.styleable.PatternView_circleSize, 200);
            circleColor = typedArray.getColor(R.styleable.PatternView_circleColor, Color.BLACK);
            dotColor = typedArray.getColor(R.styleable.PatternView_dotColor, Color.BLACK);
            pathColor = typedArray.getColor(R.styleable.PatternView_pathColor, Color.BLACK);
            gridColumns = typedArray.getInt(R.styleable.PatternView_gridColumns, 3);
            gridRows = typedArray.getInt(R.styleable.PatternView_gridRows, 3);
        } finally {
            typedArray.recycle();
        }
    }

    private void init() {
        setPathColor(pathColor);
        setCircleColor(circleColor);
        setDotColor(dotColor);
        cellManager = new CellManager(gridRows, gridColumns);
        final int matrixSize = cellManager.getSize();
        mPattern = new ArrayList<>(matrixSize);
    }

    /**
     * Sets the bitmap for active pattern sections (the circle)
     *
     * @param resId
     */
    public void setSelectedBitmap(final int resId) {
        bitmapCircleSelected = getBitmapFor(resId);
        computeBitmapSize();
    }

    /**
     * Sets the bitmap for active pattern sections (the circle)
     *
     * @param resId
     */
    public void setDefaultBitmap(final int resId) {
        bitmapCircleDefault = getBitmapFor(resId);
        computeBitmapSize();
    }

    private void loadBitmaps() {
        bitmapBtnDefault = getBitmapFor(R.drawable.pattern_btn_touched);
        bitmapBtnTouched = bitmapBtnDefault;
        bitmapCircleDefault = getBitmapFor(R.drawable.pattern_button_untouched);

        bitmapCircleSelected = getBitmapFor(R.drawable.pattern_circle_white);
        bitmapCircleRed = getBitmapFor(R.drawable.pattern_circle_blue);
        computeBitmapSize();
    }

    private void computeBitmapSize() {
        // bitmaps have the size of the largest bitmap in this group
        final Bitmap[] bitmaps = {bitmapBtnDefault, bitmapCircleSelected, bitmapCircleRed};
        if (isInEditMode()) {
            bitmapWidth = Math.max(bitmapWidth, 150);
            bitmapHeight = Math.max(bitmapHeight, 150);
            return;
        }

        for (final Bitmap bitmap : bitmaps) {
            bitmapWidth = Math.max(bitmapWidth, bitmap.getWidth());
            bitmapHeight = Math.max(bitmapHeight, bitmap.getHeight());
        }
    }

    private Bitmap getBitmapFor(final int resId) {
        return BitmapFactory.decodeResource(getContext().getResources(), resId);
    }

    /**
     * @return Whether the view is in stealth mode.
     */
    public boolean isInStealthMode() {
        return inStealthMode;
    }

    /**
     * Set whether the view is in stealth mode. If true, there will be no
     * visible feedback as the user enters the pattern.
     *
     * @param inStealthMode Whether in stealth mode.
     */
    public void setInStealthMode(final boolean inStealthMode) {
        this.inStealthMode = inStealthMode;
    }

    /**
     * Set whether the view is in erro stealth mode, If true, there will be no
     * visible feedback when the user enters a wrong pattern
     */
    public void setInErrorStealthMode(final boolean inErrorStealthMode) {
        this.inErrorStealthMode = inErrorStealthMode;
    }

    /**
     * @return Whether the view has tactile feedback enabled.
     */
    public boolean isTactileFeedbackEnabled() {
        return enableHapticFeedback;
    }

    /**
     * Set whether the view will use tactile feedback. If true, there will be
     * tactile feedback as the user enters the pattern.
     *
     * @param tactileFeedbackEnabled Whether tactile feedback is enabled
     */
    public void setTactileFeedbackEnabled(boolean tactileFeedbackEnabled) {
        enableHapticFeedback = tactileFeedbackEnabled;
    }

    /**
     * Set the call back for pattern start.
     *
     * @param onPatternStartListener The call back.
     */
    public void setOnPatternStartListener(OnPatternStartListener onPatternStartListener) {
        this.onPatternStartListener = onPatternStartListener;
    }

    /**
     * Set the call back for pattern clear.
     *
     * @param onPatternClearedListener The call back.
     */
    public void setOnPatternClearedListener(OnPatternClearedListener onPatternClearedListener) {
        this.onPatternClearedListener = onPatternClearedListener;
    }

    /**
     * Set the call back for pattern cell added.
     *
     * @param onPatternCellAddedListener The call back.
     */
    public void setOnPatternCellAddedListener(OnPatternCellAddedListener onPatternCellAddedListener) {
        this.onPatternCellAddedListener = onPatternCellAddedListener;
    }

    /**
     * Set the call back for pattern detection.
     *
     * @param onPatternDetectedListener The call back.
     */
    public void setOnPatternDetectedListener(OnPatternDetectedListener onPatternDetectedListener) {
        this.onPatternDetectedListener = onPatternDetectedListener;
    }

    /**
     * Set the pattern explicitly (rather than waiting for the user to input a
     * pattern).
     *
     * @param displayMode How to display the pattern.
     * @param pattern     The pattern.
     */
    public void setPattern(final DisplayMode displayMode, final List<Cell> pattern) {
        mPattern.clear();
        mPattern.addAll(pattern);
        clearPatternDrawLookup();
        for (Cell cell : pattern) {
            cellManager.draw(cell, true);
        }

        setDisplayMode(displayMode);
    }

    /**
     * Set the display mode of the current pattern. This can be useful, for
     * instance, after detecting a pattern to tell this view whether change the
     * in progress result to correct or wrong.
     *
     * @param displayMode The display mode.
     */
    public void setDisplayMode(final DisplayMode displayMode) {
        patternDisplayMode = displayMode;
        if (displayMode == DisplayMode.Animate) {
            if (mPattern.size() == 0) {
                throw new IllegalStateException(
                        "you must have a pattern to "
                                + "animate if you want to set the display mode to animate");
            }
            animatingPeriodStart = SystemClock.elapsedRealtime();
            final Cell first = mPattern.get(0);
            inProgressX = getCenterXForColumn(first.getColumn());
            inProgressY = getCenterYForRow(first.getRow());
            clearPatternDrawLookup();
        }
        invalidate();
    }

    /**
     * Retrieves last display mode. This method is useful in case of storing
     * states and restoring them after screen orientation changed.
     *
     * @return {@link DisplayMode}
     * @since v1.5.3 beta
     */
    public DisplayMode getDisplayMode() {
        return patternDisplayMode;
    }

    /**
     * Retrieves current displaying pattern. This method is useful in case of
     * storing states and restoring them after screen orientation changed.
     *
     * @return current displaying pattern. <b>Note:</b> This is an independent
     * list with the view's pattern itself.
     * @since v1.5.3 beta
     */
    @SuppressWarnings("unchecked")
    public List<Cell> getPattern() {
        return (List<Cell>) mPattern.clone();
    }

    /**
     * Never null
     *
     * @return
     */
    public String getPatternString() {
        return patternToString();
    }

    /**
     * @return a String that represents the current pattern. Never null
     */
    public String patternToString() {
        if (mPattern == null) {
            return "";
        }
        final int patternSize = mPattern.size();
        final StringBuilder res = new StringBuilder(patternSize);
        for (int i = 0; i < patternSize; i++) {
            final String cellToString = mPattern.get(i).getId();
            res.append(cellToString);
            if (i != patternSize - 1) {
                res.append("&");
            }
        }
        return res.toString();
    }

    public int[] patternToIntArray() {
        if (mPattern == null) {
            return new int[0];
        }
        final int patternSize = mPattern.size();
        final int[] array = new int[patternSize * 2];
        for (int i = 0; i < patternSize; i++) {
            array[i] = mPattern.get(i).getRow();
            array[i + 1] = mPattern.get(i).getColumn();
        }
        return array;
    }

    private void notifyCellAdded() {
        if (onPatternCellAddedListener != null) {
            onPatternCellAddedListener.onPatternCellAdded();
        }
    }

    private void notifyPatternStarted() {
        if (onPatternStartListener != null) {
            onPatternStartListener.onPatternStart();
        }
    }

    private void notifyPatternDetected() {
        if (onPatternDetectedListener != null) {
            onPatternDetectedListener.onPatternDetected();
        }
    }

    private void notifyPatternCleared() {
        if (onPatternClearedListener != null) {
            onPatternClearedListener.onPatternCleared();
        }
    }

    /**
     * Clear the pattern. Automatically removes any scheduled clears.
     */
    public void clearPattern() {
        cancelClearDelay();
        resetPattern();
        notifyPatternCleared();
    }

    /**
     * Reset all pattern state.
     */
    private void resetPattern() {
        mPattern.clear();
        clearPatternDrawLookup();
        patternDisplayMode = DisplayMode.Correct;
        invalidate();
    }

    /**
     * Clear the pattern lookup table.
     */
    private void clearPatternDrawLookup() {
        for (int i = 0; i < gridRows; i++) {
            for (int j = 0; j < gridColumns; j++) {
                cellManager.clearDrawing();
            }
        }
    }

    /**
     * Disable input (for instance when displaying a message that will timeout
     * so user doesn't get view into messy state).
     */
    public void disableInput() {
        inputEnabled = false;
    }

    /**
     * Enable input.
     */
    public void enableInput() {
        inputEnabled = true;
    }

    public int getPathColor() {
        return pathColor;
    }

    public void setPathColor(int pathColor) {
        this.pathColor = pathColor;
        pathPaint.setColor(pathColor);
        invalidate();
    }

    public int getCircleColor() {
        return circleColor;
    }

    public void setCircleColor(int circleColor) {
        this.circleColor = circleColor;
        circlePaint.setColorFilter(new PorterDuffColorFilter(circleColor, PorterDuff.Mode.MULTIPLY));
        invalidate();
    }

    public int getDotColor() {
        return dotColor;
    }

    public void setDotColor(int dotColor) {
        this.dotColor = dotColor;
        dotPaint.setColorFilter(new PorterDuffColorFilter(dotColor, PorterDuff.Mode.MULTIPLY));
        invalidate();
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        // View should be large enough to contain MATRIX_WIDTH side-by-side
        // target
        // bitmaps
        return gridColumns * circleSize;
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        // View should be large enough to contain MATRIX_WIDTH side-by-side
        // target
        // bitmaps
        return gridRows * circleSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode == MeasureSpec.AT_MOST) {
            width = gridColumns * circleSize;
            squareWidth = circleSize;
        } else {
            squareWidth = width / (float) gridColumns;
        }
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.AT_MOST) {
            height = gridRows * circleSize;
            squareHeight = circleSize;
        } else {
            squareHeight = height / (float) gridRows;
        }

        squareWidth = Math.min(squareWidth, squareHeight);
        squareHeight = Math.min(squareWidth, squareHeight);
        setMeasuredDimension(width, height);
    }


    /**
     * Determines whether the point x, y will add a new point to the current
     * pattern (in addition to finding the cell, also makes heuristic choices
     * such as filling in gaps based on current pattern).
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    private Cell detectAndAddHit(float x, float y) {
        final Cell cell = checkForNewHit(x, y);
        if (cell != null) {

            // check for gaps in existing pattern
            // Cell fillInGapCelal = null;
            final ArrayList<Cell> newCells = new ArrayList<>();
            if (!mPattern.isEmpty()) {
                final Cell lastCell = mPattern.get(mPattern.size() - 1);
                int dRow = cell.getRow() - lastCell.getRow();
                int dCol = cell.getColumn() - lastCell.getColumn();
                int rsign = dRow > 0 ? 1 : -1;
                int csign = dCol > 0 ? 1 : -1;

                if (dRow == 0) {
                    for (int i = 1; i < Math.abs(dCol); i++) {
                        newCells.add(new Cell(lastCell.getRow(), lastCell.getColumn()
                                + i * csign));
                    }
                } else if (dCol == 0) {
                    for (int i = 1; i < Math.abs(dRow); i++) {
                        newCells.add(new Cell(lastCell.getRow() + i * rsign,
                                lastCell.getColumn()));
                    }
                } else if (Math.abs(dCol) == Math.abs(dRow)) {
                    for (int i = 1; i < Math.abs(dRow); i++) {
                        newCells.add(new Cell(lastCell.getRow() + i * rsign,
                                lastCell.getColumn() + i * csign));
                    }
                }

            }
            for (Cell fillInGapCell : newCells) {
                if (fillInGapCell != null && !cellManager.isDrawn(fillInGapCell)) {
                    addCellToPattern(fillInGapCell);
                }
            }
            addCellToPattern(cell);
            if (enableHapticFeedback) {
                performHapticFeedback(
                        HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                                | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
            return cell;
        }
        return null;
    }

    private void addCellToPattern(Cell newCell) {
        cellManager.draw(newCell, true);
        mPattern.add(newCell);
        notifyCellAdded();
    }

    // helper method to find which cell a point maps to
    private Cell checkForNewHit(float x, float y) {

        final int rowHit = getRowHit(y);
        if (rowHit < 0) {
            return null;
        }
        final int columnHit = getColumnHit(x);
        if (columnHit < 0) {
            return null;
        }

        if (cellManager.isDrawn(rowHit, columnHit)) {
            return null;
        }
        return cellManager.get(rowHit, columnHit);
    }

    /**
     * Helper method to find the row that y falls into.
     *
     * @param y The y coordinate
     * @return The row that y falls in, or -1 if it falls in no row.
     */
    private int getRowHit(float y) {

        final float squareHeight = this.squareHeight;
        float hitSize = squareHeight * hitFactor;

        float offset = paddingTop + (squareHeight - hitSize) / 2f;
        for (int i = 0; i < gridRows; i++) {

            final float hitTop = offset + squareHeight * i;
            if (y >= hitTop && y <= hitTop + hitSize) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Helper method to find the column x fallis into.
     *
     * @param x The x coordinate.
     * @return The column that x falls in, or -1 if it falls in no column.
     */
    private int getColumnHit(float x) {
        final float squareWidth = this.squareWidth;
        float hitSize = squareWidth * hitFactor;

        float offset = paddingLeft + (squareWidth - hitSize) / 2f;
        for (int i = 0; i < gridColumns; i++) {

            final float hitLeft = offset + squareWidth * i;
            if (x >= hitLeft && x <= hitLeft + hitSize) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (!inputEnabled || !isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleActionDown(event);
                return true;
            case MotionEvent.ACTION_UP:
                handleActionUp();
                return true;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(event);
                return true;
            case MotionEvent.ACTION_CANCEL:
            /*
             * Original source check for patternInProgress == true first before
			 * calling next three lines. But if we do that, there will be
			 * nothing happened when the user taps at empty area and releases
			 * the finger. We want the pattern to be reset and the message will
			 * be updated after the user did that.
			 */
                patternInProgress = false;
                resetPattern();
                notifyPatternCleared();

                if (PROFILE_DRAWING) {
                    if (drawingProfilingStarted) {
                        Debug.stopMethodTracing();
                        drawingProfilingStarted = false;
                    }
                }
                return true;
        }
        return false;
    }

    private void handleActionMove(MotionEvent event) {
        // Handle all recent motion events so we don't skip any cells even when
        // the device
        // is busy...
        final int historySize = event.getHistorySize();
        for (int i = 0; i < historySize + 1; i++) {
            final float x = i < historySize ? event.getHistoricalX(i) : event
                    .getX();
            final float y = i < historySize ? event.getHistoricalY(i) : event
                    .getY();
            final int patternSizePreHitDetect = mPattern.size();
            Cell hitCell = detectAndAddHit(x, y);
            final int patternSize = mPattern.size();
            if (hitCell != null && patternSize == 1) {
                patternInProgress = true;
                notifyPatternStarted();
            }
            // note current x and y for rubber banding of in progress patterns
            final float dx = Math.abs(x - inProgressX);
            final float dy = Math.abs(y - inProgressY);
            if (dx + dy > squareWidth * 0.01f) {
                float oldX = inProgressX;
                float oldY = inProgressY;

                inProgressX = x;
                inProgressY = y;

                if (patternInProgress && patternSize > 0) {
                    final ArrayList<Cell> pattern = mPattern;
                    final float radius = squareWidth * diameterFactor * 0.5f;

                    final Cell lastCell = pattern.get(patternSize - 1);

                    float startX = getCenterXForColumn(lastCell.getColumn());
                    float startY = getCenterYForRow(lastCell.getRow());

                    float left;
                    float top;
                    float right;
                    float bottom;

                    final Rect invalidateRect = invalidate;

                    if (startX < x) {
                        left = startX;
                        right = x;
                    } else {
                        left = x;
                        right = startX;
                    }

                    if (startY < y) {
                        top = startY;
                        bottom = y;
                    } else {
                        top = y;
                        bottom = startY;
                    }

                    // Invalidate between the pattern's last cell and the
                    // current location
                    invalidateRect.set((int) (left - radius),
                            (int) (top - radius), (int) (right + radius),
                            (int) (bottom + radius));

                    if (startX < oldX) {
                        left = startX;
                        right = oldX;
                    } else {
                        left = oldX;
                        right = startX;
                    }

                    if (startY < oldY) {
                        top = startY;
                        bottom = oldY;
                    } else {
                        top = oldY;
                        bottom = startY;
                    }

                    // Invalidate between the pattern's last cell and the
                    // previous location
                    invalidateRect.union((int) (left - radius),
                            (int) (top - radius), (int) (right + radius),
                            (int) (bottom + radius));

                    // Invalidate between the pattern's new cell and the
                    // pattern's previous cell
                    if (hitCell != null) {
                        startX = getCenterXForColumn(hitCell.getColumn());
                        startY = getCenterYForRow(hitCell.getRow());

                        if (patternSize >= 2) {
                            // (re-using hitcell for old cell)
                            hitCell = pattern.get(patternSize - 1
                                    - (patternSize - patternSizePreHitDetect));
                            oldX = getCenterXForColumn(hitCell.getColumn());
                            oldY = getCenterYForRow(hitCell.getRow());

                            if (startX < oldX) {
                                left = startX;
                                right = oldX;
                            } else {
                                left = oldX;
                                right = startX;
                            }

                            if (startY < oldY) {
                                top = startY;
                                bottom = oldY;
                            } else {
                                top = oldY;
                                bottom = startY;
                            }
                        } else {
                            left = right = startX;
                            top = bottom = startY;
                        }

                        final float widthOffset = squareWidth / 2f;
                        final float heightOffset = squareHeight / 2f;

                        invalidateRect.set((int) (left - widthOffset),
                                (int) (top - heightOffset),
                                (int) (right + widthOffset),
                                (int) (bottom + heightOffset));
                    }

                    invalidate(invalidateRect);
                } else {
                    invalidate();
                }
            }
        }
        invalidate();
    }

    private void handleActionUp() {
        // report pattern detected
        if (!mPattern.isEmpty()) {
            patternInProgress = false;
            notifyPatternDetected();
            invalidate();
        }
        if (PROFILE_DRAWING) {
            if (drawingProfilingStarted) {
                Debug.stopMethodTracing();
                drawingProfilingStarted = false;
            }
        }
    }

    private void handleActionDown(MotionEvent event) {
        resetPattern();
        final float x = event.getX();
        final float y = event.getY();
        final Cell hitCell = detectAndAddHit(x, y);
        if (hitCell != null) {
            patternInProgress = true;
            patternDisplayMode = DisplayMode.Correct;
            notifyPatternStarted();
        } else {
            /*
             * Original source check for patternInProgress == true first before
			 * calling this block. But if we do that, there will be nothing
			 * happened when the user taps at empty area and releases the
			 * finger. We want the pattern to be reset and the message will be
			 * updated after the user did that.
			 */
            patternInProgress = false;
            notifyPatternCleared();
        }
        if (hitCell != null) {
            final float startX = getCenterXForColumn(hitCell.getColumn());
            final float startY = getCenterYForRow(hitCell.getRow());

            final float widthOffset = squareWidth / 2f;
            final float heightOffset = squareHeight / 2f;

            invalidate((int) (startX - widthOffset),
                    (int) (startY - heightOffset),
                    (int) (startX + widthOffset), (int) (startY + heightOffset));
        }
        inProgressX = x;
        inProgressY = y;
        if (PROFILE_DRAWING) {
            if (!drawingProfilingStarted) {
                Debug.startMethodTracing("LockPatternDrawing");
                drawingProfilingStarted = true;
            }
        }
    }

    private float getCenterXForColumn(int column) {
        return paddingLeft + column * squareWidth + squareWidth / 2f;
    }

    private float getCenterYForRow(int row) {
        return paddingTop + row * squareHeight + squareHeight / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final ArrayList<Cell> pattern = mPattern;
        final int count = pattern.size();

        if (patternDisplayMode == DisplayMode.Animate) {

            final int oneCycle = (count + 1) * MILLIS_PER_CIRCLE_ANIMATING;
            final int spotInCycle = (int) (SystemClock.elapsedRealtime() - animatingPeriodStart)
                    % oneCycle;
            final int numCircles = spotInCycle / MILLIS_PER_CIRCLE_ANIMATING;

            clearPatternDrawLookup();
            for (int i = 0; i < numCircles; i++) {
                final Cell cell = pattern.get(i);
                cellManager.draw(cell, true);
            }

            final boolean needToUpdateInProgressPoint = numCircles > 0
                    && numCircles < count;

            if (needToUpdateInProgressPoint) {
                final float percentageOfNextCircle = ((float) (spotInCycle % MILLIS_PER_CIRCLE_ANIMATING))
                        / MILLIS_PER_CIRCLE_ANIMATING;

                final Cell currentCell = pattern.get(numCircles - 1);
                final float centerX = getCenterXForColumn(currentCell.getColumn());
                final float centerY = getCenterYForRow(currentCell.getRow());

                final Cell nextCell = pattern.get(numCircles);
                final float dx = percentageOfNextCircle
                        * (getCenterXForColumn(nextCell.getColumn()) - centerX);
                final float dy = percentageOfNextCircle
                        * (getCenterYForRow(nextCell.getRow()) - centerY);
                inProgressX = centerX + dx;
                inProgressY = centerY + dy;
            }
            invalidate();
        }

        final float squareWidth = this.squareWidth;
        final float squareHeight = this.squareHeight;

        float radius = (squareWidth * diameterFactor * 0.5f);
        pathPaint.setStrokeWidth(radius);

        final Path currentPath = this.currentPath;
        currentPath.rewind();

        // draw the circles
        final int paddingTop = this.paddingTop;
        final int paddingLeft = this.paddingLeft;

        for (int i = 0; i < gridRows; i++) {
            float topY = paddingTop + i * squareHeight;
            for (int j = 0; j < gridColumns; j++) {
                float leftX = paddingLeft + j * squareWidth;
                drawCircle(canvas, (int) leftX, (int) topY, cellManager.isDrawn(i, j));
            }
        }

        // only the last segment of the path should be computed here
        // draw the path of the pattern (unless the user is in progress, and
        // we are in stealth mode)
        final boolean drawPath = (!inStealthMode
                && patternDisplayMode == DisplayMode.Correct || !inErrorStealthMode
                && patternDisplayMode == DisplayMode.Wrong);

        // draw the arrows associated with the path (unless the user is in
        // progress, and
        // we are in stealth mode)
        boolean oldFlagCircle = (circlePaint.getFlags() & Paint.FILTER_BITMAP_FLAG) != 0;
        boolean oldFlagDot = (dotPaint.getFlags() & Paint.FILTER_BITMAP_FLAG) != 0;
        circlePaint.setFilterBitmap(true);
        dotPaint.setFilterBitmap(true);

        if (drawPath) {
            boolean anyCircles = false;
            for (int i = 0; i < count; i++) {
                Cell cell = pattern.get(i);

                // only draw the part of the pattern stored in
                // the lookup table (this is only different in the case
                // of animation).
                if (!cellManager.isDrawn(cell)) {
                    break;
                }
                anyCircles = true;

                float centerX = getCenterXForColumn(cell.getColumn());
                float centerY = getCenterYForRow(cell.getRow());
                if (i == 0) {
                    currentPath.moveTo(centerX, centerY);
                } else {
                    currentPath.lineTo(centerX, centerY);
                }
            }

            // add last in progress section
            if ((patternInProgress || patternDisplayMode == DisplayMode.Animate)
                    && anyCircles && count > 1) {
                currentPath.lineTo(inProgressX, inProgressY);
            }
            canvas.drawPath(currentPath, pathPaint);
        }

        circlePaint.setFilterBitmap(oldFlagCircle); // restore default flag
        dotPaint.setFilterBitmap(oldFlagDot); // restore default flag
    }

    /**
     * @param canvas
     * @param leftX
     * @param topY
     * @param partOfPattern Whether this circle is part of the pattern.
     */
    private void drawCircle(Canvas canvas, int leftX, int topY,
                            boolean partOfPattern) {
        Bitmap outerCircle;
        Bitmap innerCircle;

        if (!partOfPattern
                || (inStealthMode && patternDisplayMode == DisplayMode.Correct)
                || (inErrorStealthMode && patternDisplayMode == DisplayMode.Wrong)) {
            // unselected circle
            outerCircle = bitmapCircleDefault;
            innerCircle = bitmapBtnDefault;
        } else if (patternInProgress) {
            // user is in middle of drawing a pattern
            outerCircle = bitmapCircleSelected;
            innerCircle = bitmapBtnTouched;
        } else if (patternDisplayMode == DisplayMode.Wrong) {
            // the pattern is wrong
            outerCircle = bitmapCircleRed;
            innerCircle = bitmapBtnDefault;
        } else if (patternDisplayMode == DisplayMode.Correct
                || patternDisplayMode == DisplayMode.Animate) {
            // the pattern is correct
            outerCircle = bitmapCircleSelected;
            innerCircle = bitmapBtnDefault;
        } else {
            throw new IllegalStateException("unknown display mode "
                    + patternDisplayMode);
        }

        final int width = bitmapWidth;
        final int height = bitmapHeight;

        final float squareWidth = this.squareWidth;
        final float squareHeight = this.squareHeight;

        int offsetX = (int) ((squareWidth - width) / 2f);
        int offsetY = (int) ((squareHeight - height) / 2f);

        // Allow circles to shrink if the view is too small to hold them.
        float sx = Math.min(this.squareWidth / bitmapWidth, 1.0f);
        float sy = Math.min(this.squareHeight / bitmapHeight, 1.0f);

        circleMatrix.setTranslate(leftX + offsetX, topY + offsetY);
        circleMatrix.preTranslate(bitmapWidth / 2, bitmapHeight / 2);
        circleMatrix.preScale(sx, sy);
        circleMatrix.preTranslate(-bitmapWidth / 2, -bitmapHeight / 2);

        canvas.drawBitmap(outerCircle, circleMatrix, circlePaint);
        canvas.drawBitmap(innerCircle, circleMatrix, dotPaint);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, patternToIntArray(),
                patternDisplayMode.ordinal(), inputEnabled, inStealthMode,
                enableHapticFeedback);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        final SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setPattern(DisplayMode.Correct, CellUtils.intArrayToPattern(ss.getSerializedPattern(), cellManager));
        patternDisplayMode = DisplayMode.values()[ss.getDisplayMode()];
        inputEnabled = ss.isInputEnabled();
        inStealthMode = ss.isInStealthMode();
        enableHapticFeedback = ss.isTactileFeedbackEnabled();
    }

    /**
     * The parecelable for saving and restoring a lock pattern view.
     */
    private static class SavedState extends BaseSavedState {

        private final int[] mSerializedPattern;
        private final int mDisplayMode;
        private final boolean mInputEnabled;
        private final boolean mInStealthMode;
        private final boolean mTactileFeedbackEnabled;

        /**
         * Constructor called from {@link PatternView#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, int[] serializedPattern,
                           int displayMode, boolean inputEnabled, boolean inStealthMode,
                           boolean tactileFeedbackEnabled) {
            super(superState);
            mSerializedPattern = serializedPattern;
            mDisplayMode = displayMode;
            mInputEnabled = inputEnabled;
            mInStealthMode = inStealthMode;
            mTactileFeedbackEnabled = tactileFeedbackEnabled;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            mSerializedPattern = in.createIntArray();
            mDisplayMode = in.readInt();
            mInputEnabled = (Boolean) in.readValue(null);
            mInStealthMode = (Boolean) in.readValue(null);
            mTactileFeedbackEnabled = (Boolean) in.readValue(null);
        }

        public int[] getSerializedPattern() {
            return mSerializedPattern;
        }

        public int getDisplayMode() {
            return mDisplayMode;
        }

        public boolean isInputEnabled() {
            return mInputEnabled;
        }

        public boolean isInStealthMode() {
            return mInStealthMode;
        }

        public boolean isTactileFeedbackEnabled() {
            return mTactileFeedbackEnabled;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeIntArray(mSerializedPattern);
            dest.writeInt(mDisplayMode);
            dest.writeValue(mInputEnabled);
            dest.writeValue(mInStealthMode);
            dest.writeValue(mTactileFeedbackEnabled);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

    }

    /**
     * Cancels any previous scheduled clears and clears the pattern after
     * delayMillis
     *
     * @param delayMillis
     */
    public void clearPattern(long delayMillis) {
        cancelClearDelay();
        postDelayed(mPatternClearer, delayMillis);
    }

    public void cancelClearDelay() {
        removeCallbacks(mPatternClearer);
    }

    private final Runnable mPatternClearer = new Runnable() {

        @Override
        public void run() {
            clearPattern();
        }
    };

    public void onShow() {
        clearPattern();
    }

    /**
     * How to display the current pattern.
     */
    public enum DisplayMode {

        /**
         * The pattern drawn is correct (i.e draw it in a friendly color)
         */
        Correct,

        /**
         * Animate the pattern (for demo, and help).
         */
        Animate,

        /**
         * The pattern is wrong (i.e draw a foreboding color)
         */
        Wrong
    }

    /**
     * The call back interface for detecting patterns entered by the user.
     */
    public interface OnPatternStartListener {

        /**
         * A new pattern has begun.
         */
        void onPatternStart();
    }

    /**
     * The call back interface for detecting patterns entered by the user.
     */
    public interface OnPatternClearedListener {

        /**
         * The pattern was cleared.
         */
        void onPatternCleared();
    }

    /**
     * The call back interface for detecting patterns entered by the user.
     */
    public interface OnPatternCellAddedListener {

        /**
         * The user extended the pattern currently being drawn by one cell.
         */
        void onPatternCellAdded();
    }

    /**
     * The call back interface for detecting patterns entered by the user.
     */
    public interface OnPatternDetectedListener {

        /**
         * A pattern was detected from the user.
         */
        void onPatternDetected();
    }

}