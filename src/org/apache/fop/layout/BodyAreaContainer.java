/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.layout;

// FOP
import org.apache.fop.render.Renderer;
import org.apache.fop.fo.properties.*;
import org.apache.fop.datatypes.IDReferences;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.flow.Block;
import org.apache.fop.fo.flow.BlockContainer;

// Java
import java.util.ArrayList;

public class BodyAreaContainer extends Area {

    // dimensions for the 'region-reference-area'
    private int xPosition;    // should be able to take value 'left' and 'right' too
    private int yPosition;    // should be able to take value 'top' and 'bottom' too
    private int position;

    // the column-count and column-gap
    private int columnCount;
    private int columnGap;

    // the 3 primary reference areas
    private AreaContainer mainReferenceArea;
    private AreaContainer beforeFloatReferenceArea;
    private AreaContainer footnoteReferenceArea;

    // current heights
    private int mainRefAreaHeight;
    private int beforeFloatRefAreaHeight;
    private int footnoteRefAreaHeight;

    // reference area yPositions
    private int mainYPosition;
    private int beforeFloatYPosition;
    private int footnoteYPosition;

    // the start FO in case of rollback
    private FObj startFO;
    private boolean isNewSpanArea;

    // keeps track of footnote state for multiple layouts
    private int footnoteState = 0;

    public BodyAreaContainer(FontState fontState, int xPosition,
                             int yPosition, int allocationWidth,
                             int maxHeight, int position, int columnCount,
                             int columnGap) {
        super(fontState, allocationWidth, maxHeight);
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.position = position;
        this.columnCount = columnCount;
        this.columnGap = columnGap;

        // create the primary reference areas
        beforeFloatRefAreaHeight = 0;
        footnoteRefAreaHeight = 0;
        mainRefAreaHeight = maxHeight - beforeFloatRefAreaHeight
                            - footnoteRefAreaHeight;
        beforeFloatReferenceArea = new AreaContainer(fontState, xPosition,
                yPosition, allocationWidth, beforeFloatRefAreaHeight,
                Position.ABSOLUTE);
        beforeFloatReferenceArea.setAreaName("before-float-reference-area");
        this.addChild(beforeFloatReferenceArea);
        mainReferenceArea = new AreaContainer(fontState, xPosition,
                                              yPosition, allocationWidth,
                                              mainRefAreaHeight,
                                              Position.ABSOLUTE);
        mainReferenceArea.setAreaName("main-reference-area");
        this.addChild(mainReferenceArea);
        int footnoteRefAreaYPosition = yPosition - mainRefAreaHeight;
        footnoteReferenceArea = new AreaContainer(fontState, xPosition,
                                                  footnoteRefAreaYPosition,
                                                  allocationWidth,
                                                  footnoteRefAreaHeight,
                                                  Position.ABSOLUTE);
        footnoteReferenceArea.setAreaName("footnote-reference-area");
        this.addChild(footnoteReferenceArea);

        // all padding and border-width must be 0
        // setBorderAndPadding(new BorderAndPadding());
        // setPadding(0, 0, 0, 0);
        // setBorderWidth(0, 0, 0, 0);
    }

    public void render(Renderer renderer) {
        renderer.renderBodyAreaContainer(this);
    }

    public int getPosition() {
        return position;
    }

    public int getXPosition() {
        return xPosition + getPaddingLeft() + getBorderLeftWidth();
    }

    public void setXPosition(int value) {
        xPosition = value;
    }

    public int getYPosition() {
        return yPosition + getPaddingTop() + getBorderTopWidth();
    }

    public void setYPosition(int value) {
        yPosition = value;
    }

    public AreaContainer getMainReferenceArea() {
        return mainReferenceArea;
    }

    public AreaContainer getBeforeFloatReferenceArea() {
        return beforeFloatReferenceArea;
    }

    public AreaContainer getFootnoteReferenceArea() {
        return footnoteReferenceArea;
    }

    public void setIDReferences(IDReferences idReferences) {
        mainReferenceArea.setIDReferences(idReferences);
    }

    public IDReferences getIDReferences() {
        return mainReferenceArea.getIDReferences();
    }

    /**
     * Depending on the column-count of the next FO, determine whether
     * a new span area needs to be constructed or not, and return the
     * appropriate ColumnArea.
     * The next cut of this method should also inspect the FO to see
     * whether the area to be returned ought not to be the footnote
     * or before-float reference area.
     * @param fo The next formatting object
     * @returns the next column area (possibly the current one)
     */
    public AreaContainer getNextArea(FObj fo) throws FOPException {
        isNewSpanArea = false;

        int span = Span.NONE;
        if (fo instanceof Block)
            span = ((Block)fo).getSpan();
        else if (fo instanceof BlockContainer)
            span = ((BlockContainer)fo).getSpan();

        if (this.mainReferenceArea.getChildren().isEmpty()) {
            if (span == Span.ALL)
                return addSpanArea(1);
            else
                return addSpanArea(columnCount);
        }

        ArrayList spanAreas = this.mainReferenceArea.getChildren();
        SpanArea spanArea = (SpanArea)spanAreas.get(spanAreas.size()
                - 1);

        if ((span == Span.ALL) && (spanArea.getColumnCount() == 1)) {
            // return the single column area in the same span area
            return spanArea.getCurrentColumnArea();
        } else if ((span == Span.NONE)
                   && (spanArea.getColumnCount() == columnCount)) {
            // return the current column area in the same span area
            return spanArea.getCurrentColumnArea();
        } else if (span == Span.ALL) {
            // create new span area with one column; return column area
            return addSpanArea(1);
        } else if (span == Span.NONE) {
            // create new span area with multiple columns; return first column area
            return addSpanArea(columnCount);
        } else {
            throw new FOPException("BodyAreaContainer::getNextArea(): Span attribute messed up");
        }
    }

    /**
     * Add a new span area with specified number of column areas.
     * @param numColumns The number of column areas
     * @returns AreaContainer The next column area
     */
    private AreaContainer addSpanArea(int numColumns) {
        resetHeights();
        // create span area and child column-areas, using whatever
        // height remains after existing span areas (in the main
        // reference area).
        int spanAreaYPosition = getYPosition()
                                - this.mainReferenceArea.getContentHeight();

        SpanArea spanArea = new SpanArea(fontState, getXPosition(),
                                         spanAreaYPosition, allocationWidth,
                                         getRemainingHeight(), numColumns,
                                         columnGap);
        this.mainReferenceArea.addChild(spanArea);
        spanArea.setPage(this.getPage());
        this.isNewSpanArea = true;
        return spanArea.getCurrentColumnArea();
    }

    /**
     * This almost does what getNewArea() does, without actually
     * returning an area. These 2 methods can be reworked.
     * @param fo The next formatting object
     * @returns boolean True if we need to balance.
     */
    public boolean isBalancingRequired(FObj fo) {
        if (this.mainReferenceArea.getChildren().isEmpty())
            return false;

        ArrayList spanAreas = this.mainReferenceArea.getChildren();
        SpanArea spanArea = (SpanArea)spanAreas.get(spanAreas.size()
                - 1);

        if (spanArea.isBalanced())
            return false;

        int span = Span.NONE;
        if (fo instanceof Block)
            span = ((Block)fo).getSpan();
        else if (fo instanceof BlockContainer)
            span = ((BlockContainer)fo).getSpan();

        if ((span == Span.ALL) && (spanArea.getColumnCount() == 1))
            return false;
        else if ((span == Span.NONE)
                 && (spanArea.getColumnCount() == columnCount))
            return false;
        else if (span == Span.ALL)
            return true;
        else if (span == Span.NONE)
            return false;
        else
            return false;
    }

    /**
     * This is where the balancing algorithm lives, or gets called.
     * Right now it's primitive: get the total content height in all
     * columns, divide by the column count, and add a heuristic
     * safety factor.
     * Then the previous (unbalanced) span area is removed, and a new
     * one added with the computed max height.
     */
    public void resetSpanArea() {
        ArrayList spanAreas = this.mainReferenceArea.getChildren();
        SpanArea spanArea = (SpanArea)spanAreas.get(spanAreas.size() - 1);

        if (!spanArea.isBalanced()) {
            // span area maintains a record of the total height of
            // laid-out content in the previous (first) attempt
            int newHeight = spanArea.getTotalContentHeight()
                            / spanArea.getColumnCount();
            newHeight += 2 * 15600;    // ???

            this.mainReferenceArea.removeChild(spanArea);
            resetHeights();
            SpanArea newSpanArea = new SpanArea(fontState, getXPosition(),
                                                spanArea.getYPosition(),
                                                allocationWidth, newHeight,
                                                spanArea.getColumnCount(),
                                                columnGap);
            this.mainReferenceArea.addChild(newSpanArea);
            newSpanArea.setPage(this.getPage());
            newSpanArea.setIsBalanced();
            this.isNewSpanArea = true;
        } else {
            throw new IllegalStateException("Trying to balance balanced area");
        }
    }

    /**
     * Determine remaining height for new span area. Needs to be
     * modified for footnote and before-float reference areas when
     * those are supported.
     * @returns int The remaining available height in millipoints.
     */
    public int getRemainingHeight() {
        return this.mainReferenceArea.getMaxHeight()
               - this.mainReferenceArea.getContentHeight();
    }

    /**
     * Used by resetSpanArea() and addSpanArea() to adjust the main
     * reference area height before creating a new span.
     */
    private void resetHeights() {
        int totalHeight = 0;
        ArrayList children = this.mainReferenceArea.getChildren();
        for (int i = 0; i < children.size(); i++) {
            SpanArea spanArea = (SpanArea)children.get(i);
            int spanContentHeight = spanArea.getMaxContentHeight();
            int spanMaxHeight = spanArea.getMaxHeight();

            totalHeight += (spanContentHeight < spanMaxHeight)
                           ? spanContentHeight : spanMaxHeight;
        }
        this.mainReferenceArea.setHeight(totalHeight);
    }

    /**
     * Used in Flow when layout returns incomplete.
     * @returns boolean Is this the last column in this span?
     */
    public boolean isLastColumn() {
        ArrayList spanAreas = this.mainReferenceArea.getChildren();
        SpanArea spanArea = (SpanArea)spanAreas.get(spanAreas.size() - 1);
        return spanArea.isLastColumn();
    }

    /**
     * This variable is unset by getNextArea(), is set by addSpanArea(),
     * and <i>may</i> be set by resetSpanArea().
     * @returns boolean Is the span area new or not?
     */
    public boolean isNewSpanArea() {
        return isNewSpanArea;
    }

    public AreaContainer getCurrentColumnArea() {
        ArrayList spanAreas = this.mainReferenceArea.getChildren();
        SpanArea spanArea = (SpanArea)spanAreas.get(spanAreas.size() - 1);
        return spanArea.getCurrentColumnArea();
    }

    public int getFootnoteState() {
        return footnoteState;
    }

    public boolean needsFootnoteAdjusting() {
        footnoteYPosition = footnoteReferenceArea.getYPosition();
        switch (footnoteState) {
        case 0:
            resetHeights();
            if (footnoteReferenceArea.getHeight() > 0
                    && mainYPosition + mainReferenceArea.getHeight()
                       > footnoteYPosition) {
                return true;
            }
        case 1:
            break;
        }
        return false;
    }

    public void adjustFootnoteArea() {
        footnoteState++;
        if (footnoteState == 1) {
            mainReferenceArea.setMaxHeight(footnoteReferenceArea.getYPosition()
                                           - mainYPosition);
            footnoteYPosition = footnoteReferenceArea.getYPosition();
            footnoteReferenceArea.setMaxHeight(footnoteReferenceArea.getHeight());

            ArrayList children = footnoteReferenceArea.getChildren();
            for (int i = 0; i < children.size(); i++) {
                Object obj = children.get(i);
                if (obj instanceof Area) {
                    Area childArea = (Area)obj;
                    footnoteReferenceArea.removeChild(childArea);
                }
            }

            getPage().setPendingFootnotes(null);
        }
    }

    protected static void resetMaxHeight(Area ar, int change) {
        ar.setMaxHeight(change);
        ArrayList childs = ar.getChildren();
        for (int i = 0; i < childs.size(); i++) {
            Object obj = childs.get(i);
            if (obj instanceof Area) {
                Area childArea = (Area)obj;
                resetMaxHeight(childArea, change);
            }
        }
    }

}
