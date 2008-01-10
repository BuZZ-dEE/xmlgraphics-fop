/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.fo.flow.table;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.layoutmgr.ElementListUtils;
import org.apache.fop.layoutmgr.table.TableCellLayoutManager;

/**
 * This class represents a primary grid unit of a spanned cell. This is the "before-start"
 * (top-left, usually) grid unit of the span.
 */
public class PrimaryGridUnit extends GridUnit {

    private static Log log = LogFactory.getLog(PrimaryGridUnit.class);

    /** Cell layout manager. */
    private TableCellLayoutManager cellLM;
    /** List of Knuth elements representing the contents of the cell. */
    private LinkedList elements;
    /** Index of row where this cell starts */
    private int startRow;
    /** Links to the spanned grid units. (List of GridUnit arrays, one array represents a row) */
    private List rows;
    /** The calculated size of the cell's content. (cached value) */
    private int contentLength = -1;

    /**
     * Creates a new primary grid unit.
     *
     * @param cell table cell which occupies this grid unit
     * @param row the table-row element this grid unit belongs to (if any)
     * @param column table column this grid unit belongs to
     * @param startCol index of the column this grid unit belongs to, zero-based
     * @param startRow index of the row this grid unit belongs to, zero-based
     */
    PrimaryGridUnit(TableCell cell, TableRow row, TableColumn column, int startCol) {
        super(cell, row, column, startCol, 0, 0);
        log.trace("PrimaryGridUnit created, row " + startRow + " col " + startCol);
    }

    public TableCellLayoutManager getCellLM() {
        assert cellLM != null;
        return cellLM;
    }

    /** {@inheritDoc} */
    public PrimaryGridUnit getPrimary() {
        return this;
    }

    /** {@inheritDoc} */
    public boolean isPrimary() {
        return true;
    }

    /**
     * Sets the Knuth elements for the table cell containing this grid unit.
     *
     * @param elements a list of ListElement (?)
     */
    public void setElements(LinkedList elements) {
        this.elements = elements;
    }

    public LinkedList getElements() {
        return this.elements;
    }

    /**
     * @return half the maximum before border width of this cell.
     */
    public int getHalfMaxBeforeBorderWidth() {
        int value = 0;
        if (getRows() != null) {
            int before = 0;
            //first row for before borders
            GridUnit[] row = (GridUnit[])getRows().get(0);
            for (int i = 0; i < row.length; i++) {
                if (row[i].hasBorders()) {
                    before = Math.max(before,
                            row[i].getBorders().getBorderBeforeWidth(false));
                }
            }
            value += before / 2;
        } else {
            if (hasBorders()) {
                value += getBorders().getBorderBeforeWidth(false) / 2;
            }
        }
        return value;
    }

    /**
     * @return half the maximum after border width of this cell.
     */
    public int getHalfMaxAfterBorderWidth() {
        int value = 0;
        if (getRows() != null) {
            //Last row for after borders
            int after = 0;
            GridUnit[] row = (GridUnit[])getRows().get(getRows().size() - 1);
            for (int i = 0; i < row.length; i++) {
                if (row[i].hasBorders()) {
                    after = Math.max(after, row[i].getBorders().getBorderAfterWidth(false));
                }
            }
            value += after / 2;
        } else {
            if (hasBorders()) {
                value += getBorders().getBorderAfterWidth(false) / 2;
            }
        }
        return value;
    }

    /**
     * @return the sum of half the maximum before and after border
     * widths of this cell.
     */
    public int getHalfMaxBorderWidth() {
        return getHalfMaxBeforeBorderWidth() + getHalfMaxAfterBorderWidth();
    }

    /** @return the length of the cell content. */
    public int getContentLength() {
        if (contentLength < 0) {
            contentLength = ElementListUtils.calcContentLength(elements);
        }
        return contentLength;
    }

    /** @return true if cell/row has an explicit BPD/height */
    public boolean hasBPD() {
        if (!getCell().getBlockProgressionDimension().getOptimum(null).isAuto()) {
            return true;
        }
        if (getRow() != null
                && !getRow().getBlockProgressionDimension().getOptimum(null).isAuto()) {
            return true;
        }
        return false;
    }

    /**
     * Returns the grid units belonging to the same span as this one.
     *
     * @return a list of GridUnit[], each array corresponds to a row
     */
    public List getRows() {
        return this.rows;
    }

    public void addRow(GridUnit[] row) {
        if (rows == null) {
            rows = new java.util.ArrayList();
        }
        rows.add(row);
    }

    void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    /**
     * Returns the index of the row this grid unit belongs to.
     *
     * @return the index of the row this grid unit belongs to.
     */
    public int getStartRow() {
        return this.startRow;
    }

    /**
     * Returns the widths of the start- and end-borders of the span this grid unit belongs
     * to.
     *
     * @return a two-element array containing the widths of the start-border then the
     * end-border
     */
    public int[] getStartEndBorderWidths() {
        int[] widths = new int[2];
        if (rows == null) {
            widths[0] = getBorders().getBorderStartWidth(false);
            widths[1] = getBorders().getBorderEndWidth(false);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                GridUnit[] gridUnits = (GridUnit[])rows.get(i);
                widths[0] = Math.max(widths[0],
                        (gridUnits[0]).
                            getBorders().getBorderStartWidth(false));
                widths[1] = Math.max(widths[1],
                        (gridUnits[gridUnits.length - 1]).
                            getBorders().getBorderEndWidth(false));
            }
        }
        return widths;
    }

    /** {@inheritDoc} */
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        sb.append(" startRow=").append(startRow);
        return sb.toString();
    }

    /** @return true if this cell spans over more than one grid unit. */
    public boolean hasSpanning() {
        return (getCell().getNumberColumnsSpanned() > 1)
            || (getCell().getNumberRowsSpanned() > 1);
    }

    /**
     * Creates a cellLM for the corresponding table-cell. A new one must be created
     * for each new static-content (TODO).
     */
    public void createCellLM() {
        cellLM = new TableCellLayoutManager(cell, this);
    }

}
