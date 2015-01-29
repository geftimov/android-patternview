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
import com.eftimoff.patternview.utils.PatternUtils;

import java.util.ArrayList;
import java.util.List;

public class PatternView extends View {

    private int mMatrixWidth = 3;
    private int matrixSize;

    private CellManager cellManager;

    private static final boolean PROFILE_DRAWING = false;
    private boolean mDrawingProfilingStarted = false;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // TODO: make this common with PhoneWindow
    // static final int STATUS_BAR_HEIGHT = 25;

    /**
     * How many milliseconds we spend animating each circle of a lock pattern if
     * the animating mode is set. The entire animation should take this constant
     * * the length of the pattern to complete.
     */
    private static final int MILLIS_PER_CIRCLE_ANIMATING = 700;

    private OnPatternListener mOnPatternListener;
    private ArrayList<Cell> mPattern;

    void init() {
        cellManager = new CellManager(mMatrixWidth, mMatrixWidth);
        matrixSize = cellManager.getSize();
        mPattern = new ArrayList<>(matrixSize);
    }


    /**
     * the in progress point: - during interaction: where the user's finger is -
     * during animation: the current tip of the animating line
     */
    private float mInProgressX = -1;
    private float mInProgressY = -1;

    private long mAnimatingPeriodStart;

    private DisplayMode mPatternDisplayMode = DisplayMode.Correct;
    private boolean mInputEnabled = true;
    private boolean mInStealthMode = false;
    private boolean mInErrorStealthMode = false;
    private boolean mEnableHapticFeedback = true;
    private boolean mPatternInProgress = false;

    private final float mDiameterFactor = 0.10f; // TODO: move to attrs
    private final float mHitFactor = 0.6f;

    private float mSquareWidth;
    private float mSquareHeight;

    private Bitmap mBitmapBtnDefault;
    private Bitmap mBitmapBtnTouched;
    private Bitmap mBitmapCircleDefault;
    private int mBitmapCircleSelectedResourceId = R.drawable.pattern_btn_touched;
    private Bitmap mBitmapCircleSelected;
    private Bitmap mBitmapCircleRed;

    // private Bitmap mBitmapArrowGreenUp;
    // private Bitmap mBitmapArrowRedUp;

    private final Path mCurrentPath = new Path();
    private final Rect mInvalidate = new Rect();

    private int mBitmapWidth;
    private int mBitmapHeight;

    // private final Matrix mArrowMatrix = new Matrix();
    private final Matrix mCircleMatrix = new Matrix();

    private final int mPadding = 0;
    private final int mPaddingLeft = mPadding;
    private final int mPaddingTop = mPadding;

    private int mMaxSize = 0;

    /**
     * @return The distance in inches that the finger has swiped over the
     * pattern<br>
     * This is calculated as the distance between the pattern circles,
     * not the real distance of the finger
     */
    public float getFingerDistance() {
        // TODO Pixel to inch
        float xppi = getResources().getDisplayMetrics().xdpi;
        float yppi = getResources().getDisplayMetrics().ydpi;
        float ppi = (xppi + yppi) / 2;
        float inchesPerDot = (mBitmapWidth + mBitmapHeight) / 2 / ppi;
        return inchesPerDot * mPattern.size();
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
    public static interface OnPatternListener {

        /**
         * A new pattern has begun.
         */
        void onPatternStart();

        /**
         * The pattern was cleared.
         */
        void onPatternCleared();

        /**
         * The user extended the pattern currently being drawn by one cell.
         */
        void onPatternCellAdded();

        /**
         * A pattern was detected from the user.
         */
        void onPatternDetected();
    }

    public PatternView(Context context) {
        this(context, null);
    }

    public void setSize(int newSize) {
        mMatrixWidth = newSize;
        init();
    }

    public PatternView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setClickable(true);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.PatternView);
        try {

            mMaxSize = a.getDimensionPixelSize(R.styleable.PatternView_maxSize,
                    0);
            mBitmapCircleSelectedResourceId = a.getResourceId(
                    R.styleable.PatternView_defaultCircleColor,
                    R.drawable.pattern_circle_white);
        } finally {
            a.recycle();
        }

        mPathPaint.setAntiAlias(true);
        mPathPaint.setDither(true);
        mPathPaint.setColor(Color.WHITE); // TODO
        int mStrokeAlpha = 128;
        mPathPaint.setAlpha(mStrokeAlpha);
        mPathPaint.setStyle(Paint.Style.STROKE);
        mPathPaint.setStrokeJoin(Paint.Join.ROUND);
        mPathPaint.setStrokeCap(Paint.Cap.ROUND);
        loadBitmaps();
    }

    /**
     * Sets the bitmap for active pattern sections (the circles)
     *
     * @param resId
     */
    public void setSelectedBitmap(int resId) {
        mBitmapCircleSelectedResourceId = resId;
        // Apply changes
        loadBitmaps();
    }

    private void loadBitmaps() {
        mBitmapBtnDefault = getBitmapFor(R.drawable.pattern_btn_touched);
        // mBitmapBtnTouched = getBitmapFor(R.drawable.pattern_btn_touched);
        mBitmapBtnTouched = mBitmapBtnDefault;
        mBitmapCircleDefault = getBitmapFor(R.drawable.pattern_button_untouched);

        mBitmapCircleSelected = getBitmapFor(mBitmapCircleSelectedResourceId);
        mBitmapCircleRed = getBitmapFor(R.drawable.pattern_circle_blue);

        // mBitmapArrowGreenUp =
        // getBitmapFor(R.drawable.indicator_code_lock_drag_direction_green_up);
        // mBitmapArrowRedUp = getBitmapFor(R.drawable.pattern_circle_red);

        // bitmaps have the size of the largest bitmap in this group
        final Bitmap[] bitmaps = {mBitmapBtnDefault,
                // mBitmapBtnTouched, mBitmapCircleDefault,
                mBitmapCircleSelected, mBitmapCircleRed};

        for (Bitmap bitmap : bitmaps) {
            mBitmapWidth = Math.max(mBitmapWidth, bitmap.getWidth());
            mBitmapHeight = Math.max(mBitmapHeight, bitmap.getHeight());
        }
    }

    private Bitmap getBitmapFor(int resId) {
        return BitmapFactory.decodeResource(getContext().getResources(), resId);
    }

    /**
     * @return Whether the view is in stealth mode.
     */
    public boolean isInStealthMode() {
        return mInStealthMode;
    }

    /**
     * Set whether the view is in stealth mode. If true, there will be no
     * visible feedback as the user enters the pattern.
     *
     * @param inStealthMode Whether in stealth mode.
     */
    public void setInStealthMode(boolean inStealthMode) {
        mInStealthMode = inStealthMode;
    }

    /**
     * Set whether the view is in erro stealth mode, If true, there will be no
     * visible feedback when the user enters a wrong pattern
     */
    public void setInErrorStealthMode(boolean inErrorStealthMode) {
        mInErrorStealthMode = inErrorStealthMode;
    }

    /**
     * @return Whether the view has tactile feedback enabled.
     */
    public boolean isTactileFeedbackEnabled() {
        return mEnableHapticFeedback;
    }

    /**
     * Set whether the view will use tactile feedback. If true, there will be
     * tactile feedback as the user enters the pattern.
     *
     * @param tactileFeedbackEnabled Whether tactile feedback is enabled
     */
    public void setTactileFeedbackEnabled(boolean tactileFeedbackEnabled) {
        mEnableHapticFeedback = tactileFeedbackEnabled;
    }

    /**
     * Set the call back for pattern detection.
     *
     * @param onPatternListener The call back.
     */
    public void setOnPatternListener(OnPatternListener onPatternListener) {
        mOnPatternListener = onPatternListener;
    }

    /**
     * Set the pattern explicitly (rather than waiting for the user to input a
     * pattern).
     *
     * @param displayMode How to display the pattern.
     * @param pattern     The pattern.
     */
    void setPattern(DisplayMode displayMode, List<Cell> pattern) {
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
    public void setDisplayMode(DisplayMode displayMode) {
        mPatternDisplayMode = displayMode;
        if (displayMode == DisplayMode.Animate) {
            if (mPattern.size() == 0) {
                throw new IllegalStateException(
                        "you must have a pattern to "
                                + "animate if you want to set the display mode to animate");
            }
            mAnimatingPeriodStart = SystemClock.elapsedRealtime();
            final Cell first = mPattern.get(0);
            mInProgressX = getCenterXForColumn(first.getColumn());
            mInProgressY = getCenterYForRow(first.getRow());
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
        return mPatternDisplayMode;
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
    String patternToString() {
        if (mPattern == null) {
            return "";
        }
        final int patternSize = mPattern.size();
        final StringBuilder res = new StringBuilder(patternSize);
        int padding = String.valueOf(matrixSize).length();
        for (int i = 0; i < patternSize; i++) {
            int cell = mPattern.get(i).getId();
            res.append(String.format("%0" + padding + "d", cell));
        }
        return res.toString();
    }

    private void notifyCellAdded() {
        if (mOnPatternListener != null) {
            mOnPatternListener.onPatternCellAdded();
        }
    }

    private void notifyPatternStarted() {
        if (mOnPatternListener != null) {
            mOnPatternListener.onPatternStart();
        }
    }

    private void notifyPatternDetected() {
        if (mOnPatternListener != null) {
            mOnPatternListener.onPatternDetected();
        }
    }

    private void notifyPatternCleared() {
        if (mOnPatternListener != null) {
            mOnPatternListener.onPatternCleared();
        }
    }

    /**
     * Clear the pattern. Automatically removes any scheduled clears.
     */
    public void clearPattern() {
        cancelClearDelay();
        resetPattern();
        // TODO Change if it breaks
        notifyPatternCleared();
    }

    /**
     * Reset all pattern state.
     */
    private void resetPattern() {
        mPattern.clear();
        clearPatternDrawLookup();
        mPatternDisplayMode = DisplayMode.Correct;
        invalidate();
    }

    /**
     * Clear the pattern lookup table.
     */
    private void clearPatternDrawLookup() {
        for (int i = 0; i < mMatrixWidth; i++) {
            for (int j = 0; j < mMatrixWidth; j++) {
                cellManager.clearDrawing();
            }
        }
    }

    /**
     * Disable input (for instance when displaying a message that will timeout
     * so user doesn't get view into messy state).
     */
    public void disableInput() {
        mInputEnabled = false;
    }

    /**
     * Enable input.
     */
    public void enableInput() {
        mInputEnabled = true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int mPaddingRight = mPadding;
        final int width = w - mPaddingLeft - mPaddingRight;
        mSquareWidth = width / (float) mMatrixWidth;

        int mPaddingBottom = mPadding;
        final int height = h - mPaddingTop - mPaddingBottom;
        mSquareHeight = height / (float) mMatrixWidth;
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        // View should be large enough to contain MATRIX_WIDTH side-by-side
        // target
        // bitmaps
        return mMatrixWidth * mBitmapWidth;
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        // View should be large enough to contain MATRIX_WIDTH side-by-side
        // target
        // bitmaps
        return mMatrixWidth * mBitmapWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int size = Math.min(width, height);
        if (mMaxSize != 0) {
            size = Math.min(size, mMaxSize);
        }
        setMeasuredDimension(size, size);
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
            if (mEnableHapticFeedback) {
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

        final float squareHeight = mSquareHeight;
        float hitSize = squareHeight * mHitFactor;

        float offset = mPaddingTop + (squareHeight - hitSize) / 2f;
        for (int i = 0; i < mMatrixWidth; i++) {

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
        final float squareWidth = mSquareWidth;
        float hitSize = squareWidth * mHitFactor;

        float offset = mPaddingLeft + (squareWidth - hitSize) / 2f;
        for (int i = 0; i < mMatrixWidth; i++) {

            final float hitLeft = offset + squareWidth * i;
            if (x >= hitLeft && x <= hitLeft + hitSize) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mInputEnabled || !isEnabled()) {
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
             * Original source check for mPatternInProgress == true first before
			 * calling next three lines. But if we do that, there will be
			 * nothing happened when the user taps at empty area and releases
			 * the finger. We want the pattern to be reset and the message will
			 * be updated after the user did that.
			 */
                mPatternInProgress = false;
                resetPattern();
                notifyPatternCleared();

                if (PROFILE_DRAWING) {
                    if (mDrawingProfilingStarted) {
                        Debug.stopMethodTracing();
                        mDrawingProfilingStarted = false;
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
                mPatternInProgress = true;
                notifyPatternStarted();
            }
            // note current x and y for rubber banding of in progress patterns
            final float dx = Math.abs(x - mInProgressX);
            final float dy = Math.abs(y - mInProgressY);
            if (dx + dy > mSquareWidth * 0.01f) {
                float oldX = mInProgressX;
                float oldY = mInProgressY;

                mInProgressX = x;
                mInProgressY = y;

                if (mPatternInProgress && patternSize > 0) {
                    final ArrayList<Cell> pattern = mPattern;
                    final float radius = mSquareWidth * mDiameterFactor * 0.5f;

                    final Cell lastCell = pattern.get(patternSize - 1);

                    float startX = getCenterXForColumn(lastCell.getColumn());
                    float startY = getCenterYForRow(lastCell.getRow());

                    float left;
                    float top;
                    float right;
                    float bottom;

                    final Rect invalidateRect = mInvalidate;

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

                        final float widthOffset = mSquareWidth / 2f;
                        final float heightOffset = mSquareHeight / 2f;

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
    }

    private void handleActionUp() {
        // report pattern detected
        if (!mPattern.isEmpty()) {
            mPatternInProgress = false;
            notifyPatternDetected();
            invalidate();
        }
        if (PROFILE_DRAWING) {
            if (mDrawingProfilingStarted) {
                Debug.stopMethodTracing();
                mDrawingProfilingStarted = false;
            }
        }
    }

    private void handleActionDown(MotionEvent event) {
        resetPattern();
        final float x = event.getX();
        final float y = event.getY();
        final Cell hitCell = detectAndAddHit(x, y);
        if (hitCell != null) {
            mPatternInProgress = true;
            mPatternDisplayMode = DisplayMode.Correct;
            notifyPatternStarted();
        } else {
            /*
             * Original source check for mPatternInProgress == true first before
			 * calling this block. But if we do that, there will be nothing
			 * happened when the user taps at empty area and releases the
			 * finger. We want the pattern to be reset and the message will be
			 * updated after the user did that.
			 */
            mPatternInProgress = false;
            notifyPatternCleared();
        }
        if (hitCell != null) {
            final float startX = getCenterXForColumn(hitCell.getColumn());
            final float startY = getCenterYForRow(hitCell.getRow());

            final float widthOffset = mSquareWidth / 2f;
            final float heightOffset = mSquareHeight / 2f;

            invalidate((int) (startX - widthOffset),
                    (int) (startY - heightOffset),
                    (int) (startX + widthOffset), (int) (startY + heightOffset));
        }
        mInProgressX = x;
        mInProgressY = y;
        if (PROFILE_DRAWING) {
            if (!mDrawingProfilingStarted) {
                Debug.startMethodTracing("LockPatternDrawing");
                mDrawingProfilingStarted = true;
            }
        }
    }

    private float getCenterXForColumn(int column) {
        return mPaddingLeft + column * mSquareWidth + mSquareWidth / 2f;
    }

    private float getCenterYForRow(int row) {
        return mPaddingTop + row * mSquareHeight + mSquareHeight / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final ArrayList<Cell> pattern = mPattern;
        final int count = pattern.size();
//        final boolean[][] drawLookup = mPatternDrawLookup;

        if (mPatternDisplayMode == DisplayMode.Animate) {

            // figure out which circles to draw

            // + 1 so we pause on complete pattern
            final int oneCycle = (count + 1) * MILLIS_PER_CIRCLE_ANIMATING;
            final int spotInCycle = (int) (SystemClock.elapsedRealtime() - mAnimatingPeriodStart)
                    % oneCycle;
            final int numCircles = spotInCycle / MILLIS_PER_CIRCLE_ANIMATING;

            clearPatternDrawLookup();
            for (int i = 0; i < numCircles; i++) {
                final Cell cell = pattern.get(i);
//                drawLookup[cell.getRow()][cell.getColumn()] = true;
                cellManager.draw(cell, true);
            }

            // figure out in progress portion of ghosting line

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
                mInProgressX = centerX + dx;
                mInProgressY = centerY + dy;
            }
            // TODO: Infinite loop here...
            invalidate();
        }

        final float squareWidth = mSquareWidth;
        final float squareHeight = mSquareHeight;

        float radius = (squareWidth * mDiameterFactor * 0.5f);
        mPathPaint.setStrokeWidth(radius);

        final Path currentPath = mCurrentPath;
        currentPath.rewind();

        // draw the circles
        final int paddingTop = mPaddingTop;
        final int paddingLeft = mPaddingLeft;

        for (int i = 0; i < mMatrixWidth; i++) {
            float topY = paddingTop + i * squareHeight;
            // float centerY = mPaddingTop + i * mSquareHeight + (mSquareHeight
            // / 2);
            for (int j = 0; j < mMatrixWidth; j++) {
                float leftX = paddingLeft + j * squareWidth;
//                drawCircle(canvas, (int) leftX, (int) topY, drawLookup[i][j]);
                drawCircle(canvas, (int) leftX, (int) topY, cellManager.isDrawn(i, j));
            }
        }

        // TODO: the path should be created and cached every time we hit-detect
        // a cell
        // only the last segment of the path should be computed here
        // draw the path of the pattern (unless the user is in progress, and
        // we are in stealth mode)
        final boolean drawPath = (!mInStealthMode
                && mPatternDisplayMode == DisplayMode.Correct || !mInErrorStealthMode
                && mPatternDisplayMode == DisplayMode.Wrong);

        // draw the arrows associated with the path (unless the user is in
        // progress, and
        // we are in stealth mode)
        boolean oldFlag = (mPaint.getFlags() & Paint.FILTER_BITMAP_FLAG) != 0;
        mPaint.setFilterBitmap(true); // draw with higher quality since we
        // render with transforms

        // Not drawing arrows
        // if (drawPath) {
        // for (int i = 0; i < count - 1; i++) {
        // Cell cell = pattern.get(i);
        // Cell next = pattern.get(i + 1);
        //
        // // only draw the part of the pattern stored in
        // // the lookup table (this is only different in the case
        // // of animation).
        // if (!drawLookup[next.mRow][next.mColumn]) {
        // break;
        // }
        //
        // float leftX = paddingLeft + cell.mColumn * squareWidth;
        // float topY = paddingTop + cell.mRow * squareHeight;
        //
        // drawArrow(canvas, leftX, topY, cell, next);
        // }
        // }

        if (drawPath) {
            boolean anyCircles = false;
            for (int i = 0; i < count; i++) {
                Cell cell = pattern.get(i);

                // only draw the part of the pattern stored in
                // the lookup table (this is only different in the case
                // of animation).
//                if (!drawLookup[cell.getRow()][cell.getColumn()]) {
//                    break;
//                }
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
            if ((mPatternInProgress || mPatternDisplayMode == DisplayMode.Animate)
                    && anyCircles && count > 1) {
                currentPath.lineTo(mInProgressX, mInProgressY);
            }
            canvas.drawPath(currentPath, mPathPaint);
        }

        mPaint.setFilterBitmap(oldFlag); // restore default flag
    }

    // private void drawArrowUnused(Canvas canvas, float leftX, float topY,
    // Cell start, Cell end) {
    // boolean green = mPatternDisplayMode != DisplayMode.Wrong;
    //
    // final int endRow = end.mRow;
    // final int startRow = start.mRow;
    // final int endColumn = end.mColumn;
    // final int startColumn = start.mColumn;
    //
    // // offsets for centering the bitmap in the cell
    // final int offsetX = ((int) mSquareWidth - mBitmapWidth) / 2;
    // final int offsetY = ((int) mSquareHeight - mBitmapHeight) / 2;
    //
    // // compute transform to place arrow bitmaps at correct angle inside
    // // circle.
    // // This assumes that the arrow image is drawn at 12:00 with it's top
    // // edge
    // // coincident with the circle bitmap's top edge.
    // Bitmap arrow = green ? mBitmapArrowGreenUp : mBitmapArrowRedUp;
    // final int cellWidth = mBitmapWidth;
    // final int cellHeight = mBitmapHeight;
    //
    // // the up arrow bitmap is at 12:00, so find the rotation from x axis and
    // // add 90 degrees.
    // final float theta = (float) Math.atan2((double) (endRow - startRow),
    // (double) (endColumn - startColumn));
    // final float angle = (float) Math.toDegrees(theta) + 90.0f;
    //
    // // compose matrix
    // float sx = Math.min(mSquareWidth / mBitmapWidth, 1.0f);
    // float sy = Math.min(mSquareHeight / mBitmapHeight, 1.0f);
    // mArrowMatrix.setTranslate(leftX + offsetX, topY + offsetY); // transform
    // // to cell
    // // position
    // mArrowMatrix.preTranslate(mBitmapWidth / 2, mBitmapHeight / 2);
    // mArrowMatrix.preScale(sx, sy);
    // mArrowMatrix.preTranslate(-mBitmapWidth / 2, -mBitmapHeight / 2);
    // mArrowMatrix.preRotate(angle, cellWidth / 2.0f, cellHeight / 2.0f); //
    // rotate
    // // about
    // // cell
    // // center
    // mArrowMatrix.preTranslate((cellWidth - arrow.getWidth()) / 2.0f, 0.0f);
    // // translate
    // // to
    // // 12:00
    // // pos
    // canvas.drawBitmap(arrow, mArrowMatrix, mPaint);
    // }

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
                || (mInStealthMode && mPatternDisplayMode == DisplayMode.Correct)
                || (mInErrorStealthMode && mPatternDisplayMode == DisplayMode.Wrong)) {
            // unselected circle
            outerCircle = mBitmapCircleDefault;
            innerCircle = mBitmapBtnDefault;
        } else if (mPatternInProgress) {
            // user is in middle of drawing a pattern
            outerCircle = mBitmapCircleSelected;
            innerCircle = mBitmapBtnTouched;
        } else if (mPatternDisplayMode == DisplayMode.Wrong) {
            // the pattern is wrong
            outerCircle = mBitmapCircleRed;
            innerCircle = mBitmapBtnDefault;
        } else if (mPatternDisplayMode == DisplayMode.Correct
                || mPatternDisplayMode == DisplayMode.Animate) {
            // the pattern is correct
            outerCircle = mBitmapCircleSelected;
            innerCircle = mBitmapBtnDefault;
        } else {
            throw new IllegalStateException("unknown display mode "
                    + mPatternDisplayMode);
        }

        final int width = mBitmapWidth;
        final int height = mBitmapHeight;

        final float squareWidth = mSquareWidth;
        final float squareHeight = mSquareHeight;

        int offsetX = (int) ((squareWidth - width) / 2f);
        int offsetY = (int) ((squareHeight - height) / 2f);

        // Allow circles to shrink if the view is too small to hold them.
        float sx = Math.min(mSquareWidth / mBitmapWidth, 1.0f);
        float sy = Math.min(mSquareHeight / mBitmapHeight, 1.0f);

        mCircleMatrix.setTranslate(leftX + offsetX, topY + offsetY);
        mCircleMatrix.preTranslate(mBitmapWidth / 2, mBitmapHeight / 2);
        mCircleMatrix.preScale(sx, sy);
        mCircleMatrix.preTranslate(-mBitmapWidth / 2, -mBitmapHeight / 2);

        canvas.drawBitmap(outerCircle, mCircleMatrix, mPaint);
        canvas.drawBitmap(innerCircle, mCircleMatrix, mPaint);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, patternToString(),
                mPatternDisplayMode.ordinal(), mInputEnabled, mInStealthMode,
                mEnableHapticFeedback);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        final SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setPattern(DisplayMode.Correct, PatternUtils.stringToPatternOld(ss.getSerializedPattern(), cellManager));
        mPatternDisplayMode = DisplayMode.values()[ss.getDisplayMode()];
        mInputEnabled = ss.isInputEnabled();
        mInStealthMode = ss.isInStealthMode();
        mEnableHapticFeedback = ss.isTactileFeedbackEnabled();
    }

    /**
     * The parecelable for saving and restoring a lock pattern view.
     */
    private static class SavedState extends BaseSavedState {

        private final String mSerializedPattern;
        private final int mDisplayMode;
        private final boolean mInputEnabled;
        private final boolean mInStealthMode;
        private final boolean mTactileFeedbackEnabled;

        /**
         * Constructor called from {@link PatternView#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, String serializedPattern,
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
            mSerializedPattern = in.readString();
            mDisplayMode = in.readInt();
            mInputEnabled = (Boolean) in.readValue(null);
            mInStealthMode = (Boolean) in.readValue(null);
            mTactileFeedbackEnabled = (Boolean) in.readValue(null);
        }

        public String getSerializedPattern() {
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
            dest.writeString(mSerializedPattern);
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

}