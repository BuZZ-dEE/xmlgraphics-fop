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

package org.apache.fop.fo.flow;

// Java
import java.util.BitSet;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;

/**
 * Class modelling the fo:table-body object.
 */
public class TableBody extends TableFObj {
    // The value of properties relevant for fo:table-body.
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    // Unused but valid items, commented out for performance:
    //     private CommonAccessibility commonAccessibility;
    //     private CommonAural commonAural;
    //     private CommonRelativePosition commonRelativePosition;
    //    private int visibility;
    // End of property values
    
    /**
     * used for validation
     */
    protected boolean tableRowsFound = false;
    protected boolean tableCellsFound = false;
    
    /**
     * used for initial values of column-number property
     */
    protected List pendingSpans;
    protected BitSet usedColumnIndices;
    private int columnIndex = 1;
    protected boolean firstRow = true;
    
    /**
     * @param parent FONode that is the parent of the object
     */
    public TableBody(FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
    public void bind(PropertyList pList) throws FOPException {
        commonBorderPaddingBackground = pList.getBorderPaddingBackgroundProps();
        super.bind(pList);
    }
    
    /**
     * {@inheritDoc} 
     */
    public void processNode(String elementName, Locator locator, 
                            Attributes attlist, PropertyList pList) 
                    throws FOPException {
        if (!inMarker()) {
            if (getTable().columns != null) {
                int cap = getTable().columns.size();
                pendingSpans = new java.util.ArrayList(cap);
                usedColumnIndices = new java.util.BitSet(cap);
            } else {
                pendingSpans = new java.util.ArrayList();
                usedColumnIndices = new java.util.BitSet();
            }
            setNextColumnIndex();
        }
        super.processNode(elementName, locator, attlist, pList);
    }

    /**
     * {@inheritDoc}
     */
    protected void startOfNode() throws FOPException {
        getFOEventHandler().startBody(this);
    }

    /**
     * {@inheritDoc}
     */
    protected void endOfNode() throws FOPException {
        
        if (!inMarker()) {
            pendingSpans = null;
            usedColumnIndices = null;
        }
        
        getFOEventHandler().endBody(this);
        
        if (!(tableRowsFound || tableCellsFound)) {
            if (getUserAgent().validateStrictly()) {
                missingChildElementError("marker* (table-row+|table-cell+)");
            } else {
                log.error("fo:table-body must not be empty. "
                        + "Expected: marker* (table-row+|table-cell+)");
                getParent().removeChild(this);
            }
        }        
    }

    /**
     * {@inheritDoc} String, String)
     * XSL Content Model: marker* (table-row+|table-cell+)
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (localName.equals("marker")) {
                if (tableRowsFound || tableCellsFound) {
                   nodesOutOfOrderError(loc, "fo:marker", "(table-row+|table-cell+)");
                }
            } else if (localName.equals("table-row")) {
                tableRowsFound = true;
                if (tableCellsFound) {
                    invalidChildError(loc, nsURI, localName, "Either fo:table-rows" +
                      " or fo:table-cells may be children of an " + getName() +
                      " but not both");
                }
            } else if (localName.equals("table-cell")) {
                tableCellsFound = true;
                if (tableRowsFound) {
                    invalidChildError(loc, nsURI, localName, 
                            "Either fo:table-rows or fo:table-cells "
                            + "may be children of an " 
                            + getName() + " but not both");
                }  
            } else {
                invalidChildError(loc, nsURI, localName);
            }
        } else {
            invalidChildError(loc, nsURI, localName);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected void addChildNode(FONode child) throws FOPException {
        if (!inMarker()) {
            if (firstRow) {
                Table t = getTable();
                
                if (t.columns == null) {
                    t.columns = new java.util.ArrayList();
                }
                
                switch (child.getNameId()) {
                case FO_TABLE_ROW:
                    firstRow = false;
                    break;
                case FO_TABLE_CELL:
                    TableCell cell = (TableCell) child;
                    int colNr = cell.getColumnNumber();
                    int colSpan = cell.getNumberColumnsSpanned();
                    Length colWidth = null;

                    if (cell.getWidth().getEnum() != EN_AUTO
                            && colSpan == 1) {
                        colWidth = cell.getWidth();
                    }
                    
                    for (int i = colNr; i < colNr + colSpan; ++i) {
                        if (t.columns.size() < i
                                || t.columns.get(i - 1) == null) {
                            t.addDefaultColumn(colWidth, 
                                    i == colNr 
                                        ? cell.getColumnNumber()
                                        : 0);
                        } else {
                            TableColumn col = 
                                (TableColumn) t.columns.get(i - 1);
                            if (!col.isDefaultColumn()
                                    && colWidth != null) {
                                col.setColumnWidth(colWidth);
                            }
                        }
                    }
                    break;
                default:
                    //nop
                }
            }
        }
        super.addChildNode(child);
    }
    
    /**
     * @return the Common Border, Padding, and Background Properties.
     */
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return commonBorderPaddingBackground;
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "table-body";
    }

    /**
     * {@inheritDoc}
     */
    public int getNameId() {
        return FO_TABLE_BODY;
    }

    /**
     * @param obj table row in question
     * @return true if the given table row is the first row of this body.
     */
    public boolean isFirst(TableRow obj) {
        return (firstChild == null 
                || firstChild == obj);
    }

    /**
     * Initializes list of pending row-spans; used for correctly
     * assigning initial value for column-number for the
     * cells of following rows
     * (note: not literally mentioned in the Rec, but it is assumed
     *  that, if the first cell in a given row spans two rows, then
     *  the first cell of the following row will have an initial
     *  column-number of 2, since the first column is already 
     *  occupied...)
     */
    protected void initPendingSpans(FONode child) {
        if (child.getNameId() == FO_TABLE_ROW) {
            pendingSpans = ((TableRow) child).pendingSpans;
        } else if (pendingSpans == null) {
            if (getTable().columns != null) {
                List tableCols = getTable().columns;
                pendingSpans = new java.util.ArrayList(tableCols.size());
                for (int i = tableCols.size(); --i >= 0;) {
                    pendingSpans.add(null);
                }
            } else {
                pendingSpans = new java.util.ArrayList();
            }
        }
    }
        
    /**
     * Returns the current column index of the TableBody
     * 
     * @return the next column number to use
     */
    protected int getCurrentColumnIndex() {
        return columnIndex;
    }

    /**
     * Sets the current column index to a specific value
     * (used by ColumnNumberPropertyMaker.make() in case the 
     *  column-number was explicitly specified on the cell)
     * 
     * @param newIndex  the new column index
     */
    protected void setCurrentColumnIndex(int newIndex) {
        columnIndex = newIndex;
    }

    /**
     * Resets the current column index for the TableBody
     *
     */
    protected void resetColumnIndex() {
        columnIndex = 1;
        for (int i = usedColumnIndices.length(); --i >= 0;) {
            usedColumnIndices.clear(i);
        }
        
        PendingSpan pSpan;
        for (int i = pendingSpans.size(); --i >= 0;) {
            pSpan = (PendingSpan) pendingSpans.get(i);
            if (pSpan != null) {
                pSpan.rowsLeft--;
                if (pSpan.rowsLeft == 0) {
                    pendingSpans.set(i, null);
                } else {
                    usedColumnIndices.set(i);
                }
            }
        }
        if (!firstRow) {
            setNextColumnIndex();
        }
    }

    /**
     * Increases columnIndex to the next available value
     *
     */
    protected void setNextColumnIndex() {
        while (usedColumnIndices.get(columnIndex - 1)) {
            //increment columnIndex
            columnIndex++;
        }
        //if the table has explicit columns, and
        //the index is not assigned to any
        //column, increment further until the next
        //index occupied by a column...
        if (getTable().columns != null) {
            while (columnIndex <= getTable().columns.size()
                    && !getTable().isColumnNumberUsed(columnIndex) ) {
                columnIndex++;
            }
        }
    }

    /**
     * Checks whether the previous cell had 'ends-row="true"'
     * 
     * @return true if:
     *          a) there is a previous cell, which
     *             had ends-row="true"
     *          b) there is no previous cell (implicit 
     *             start of row)
     */
    protected boolean previousCellEndedRow() {
        if (firstChild != null) {
            FONode prevNode = getChildNodes().lastNode();
            if (prevNode.getNameId() == FO_TABLE_CELL) {
                return ((TableCell) prevNode).endsRow();
            }
        }
        return true;
    }

    /**
     * Checks whether a given column-number is already in use
     * for the current row;
     * 
     * @param   colNr   the column-number to check
     * @return true if column-number is already occupied
     */
    public boolean isColumnNumberUsed(int colNr) {
        return usedColumnIndices.get(colNr - 1);
    }
    
    /**
     * {@inheritDoc} 
     */
    protected void flagColumnIndices(int start, int end) {
        for (int i = start; i < end; i++) {
            usedColumnIndices.set(i);
        }
        setNextColumnIndex();
    }
}
