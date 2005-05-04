/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

package org.apache.fop.layoutmgr.table;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.area.Block;
import org.apache.fop.area.Trait;
import org.apache.fop.fo.flow.Table;
import org.apache.fop.fo.flow.TableRow;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.LengthRangeProperty;
import org.apache.fop.layoutmgr.KnuthBox;
import org.apache.fop.layoutmgr.KnuthElement;
import org.apache.fop.layoutmgr.KnuthPenalty;
import org.apache.fop.layoutmgr.KnuthPossPosIter;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.LayoutManager;
import org.apache.fop.layoutmgr.MinOptMaxUtil;
import org.apache.fop.layoutmgr.Position;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.TraitSetter;
import org.apache.fop.traits.MinOptMax;

/**
 * Layout manager for table contents, particularly managing the creation of combined element lists.
 */
public class TableContentLayoutManager {

    /** Logger **/
    private static Log log = LogFactory.getLog(TableContentLayoutManager.class);

    private TableLayoutManager tableLM;
    private TableRowIterator trIter;
    private TableRowIterator headerIter;
    private TableRowIterator footerIter;
    private LinkedList headerList;
    private LinkedList footerList;
    private int headerNetHeight = 0;
    private int footerNetHeight = 0;

    private int startXOffset;
    private int usedBPD;
    
    /**
     * Main constructor
     * @param parent Parent layout manager
     */
    public TableContentLayoutManager(TableLayoutManager parent) {
        this.tableLM = parent;
        Table table = getTableLM().getTable();
        this.trIter = new TableRowIterator(table, getTableLM().getColumns(), TableRowIterator.BODY);
        if (table.getTableHeader() != null) {
            headerIter = new TableRowIterator(table, 
                    getTableLM().getColumns(), TableRowIterator.HEADER);
        }
        if (table.getTableFooter() != null) {
            footerIter = new TableRowIterator(table, 
                    getTableLM().getColumns(), TableRowIterator.FOOTER);
        }
    }
    
    /**
     * @return the table layout manager
     */
    public TableLayoutManager getTableLM() {
        return this.tableLM;
    }
    
    /**
     * @return the column setup of this table
     */
    public ColumnSetup getColumns() {
        return getTableLM().getColumns();
    }
    
    /**
     * @see org.apache.fop.layoutmgr.LayoutManager#getNextKnuthElements(org.apache.fop.layoutmgr.LayoutContext, int)
     */
    public LinkedList getNextKnuthElements(LayoutContext context, int alignment) {
        log.debug("Columns: " + getTableLM().getColumns());
        KnuthBox headerAsFirst = null;
        KnuthBox headerAsSecondToLast = null;
        KnuthBox footerAsLast = null;
        if (headerIter != null) {
            this.headerList = getKnuthElementsForRowIterator(
                    headerIter, context, alignment, true);
            removeLegalBreaks(this.headerList);
            this.headerNetHeight = calcCellHeightFromContents(this.headerList);
            if (log.isDebugEnabled()) {
                log.debug("Header: " + headerNetHeight + " - " + this.headerList);
            }
            TableHeaderFooterPosition pos = new TableHeaderFooterPosition(
                    getTableLM(), true, this.headerList);
            KnuthBox box = new KnuthBox(headerNetHeight, pos, false);
            if (getTableLM().getTable().omitHeaderAtBreak()) {
                //We can simply add the table header at the beginning of the whole list
                headerAsFirst = box;
                //returnList.add(0, box);
            } else {
                headerAsSecondToLast = box;
                //returnList.add(box);
            }
        }
        if (footerIter != null) {
            this.footerList = getKnuthElementsForRowIterator(
                    footerIter, context, alignment, true);
            removeLegalBreaks(this.footerList);
            this.footerNetHeight = calcCellHeightFromContents(this.footerList);
            if (log.isDebugEnabled()) {
                log.debug("Footer: " + footerNetHeight + " - " + this.footerList);
            }
            if (true /*getTableLM().getTable().omitFooterAtBreak()*/) {
                //We can simply add the table header at the end of the whole list
                TableHeaderFooterPosition pos = new TableHeaderFooterPosition(
                        getTableLM(), false, this.footerList);
                KnuthBox box = new KnuthBox(footerNetHeight, pos, false);
                footerAsLast = box;
                //returnList.add(box);
            }
        }
        LinkedList returnList = getKnuthElementsForRowIterator(
                trIter, context, alignment, false);
        if (headerAsFirst != null) {
            returnList.add(0, headerAsFirst);
        } else if (headerAsSecondToLast != null) {
            returnList.add(headerAsSecondToLast);
        }
        if (footerAsLast != null) {
            returnList.add(footerAsLast);
        }
        return returnList;
    }
    
    private void removeLegalBreaks(LinkedList elements) {
        ListIterator i = elements.listIterator();
        while (i.hasNext()) {
            KnuthElement el = (KnuthElement)i.next();
            if (el.isPenalty()) {
                KnuthPenalty penalty = (KnuthPenalty)el;
                //Convert all penalties no break inhibitors
                if (penalty.getP() < KnuthPenalty.INFINITE) {
                    i.set(new KnuthPenalty(penalty.getW(), KnuthPenalty.INFINITE, 
                            penalty.isFlagged(), penalty.getPosition(), penalty.isAuxiliary()));
                }
            } else if (el.isGlue()) {
                i.previous();
                if (el.isBox()) {
                    i.next();
                    i.add(new KnuthPenalty(0, KnuthPenalty.INFINITE, false, 
                            new Position(getTableLM()), false));
                }
            }
        }
    }
    
    /**
     * Creates Knuth elements by iterating over a TableRowIterator.
     * @param iter TableRowIterator instance to fetch rows from
     * @param context Active LayoutContext
     * @param alignment alignment indicator
     * @param isHeaderFooter true if currently handling headers/footers
     * @return An element list
     */
    private LinkedList getKnuthElementsForRowIterator(TableRowIterator iter, 
            LayoutContext context, int alignment, boolean isHeaderFooter) {
        LinkedList returnList = new LinkedList();
        TableRowIterator.EffRow[] rowGroup = null;
        while ((rowGroup = iter.getNextRowGroup()) != null) {
            resolveNormalBeforeAfterBordersForRowGroup(rowGroup, iter);
            createElementsForRowGroup(context, alignment, isHeaderFooter, 
                        returnList, rowGroup);
        }
        
        //Remove last penalty
        KnuthElement last = (KnuthElement)returnList.getLast();
        if (last.isPenalty() && last.getW() == 0 && last.getP() == 0) {
            returnList.removeLast();
        }
        return returnList;
    }

    /**
     * Resolves normal borders for a row group.
     * @param iter Table row iterator to operate on
     */
    private void resolveNormalBeforeAfterBordersForRowGroup(TableRowIterator.EffRow[] rowGroup, 
            TableRowIterator iter) {
        for (int rgi = 0; rgi < rowGroup.length; rgi++) {
            TableRowIterator.EffRow row = rowGroup[rgi];
            TableRowIterator.EffRow prev = iter.getCachedRow(row.getIndex() - 1);
            TableRowIterator.EffRow next = iter.getCachedRow(row.getIndex() + 1);
            if (next == null) {
                //It wasn't read, yet, or we are at the last row
                next = iter.getNextRow();
                iter.backToPreviewRow();
            }
            if ((prev == null) && (iter == this.trIter) && (this.headerIter != null)) {
                prev = this.headerIter.getLastRow();
            }
            if ((prev == null) && (iter == this.headerIter)) {
                prev = this.trIter.getFirstRow();
            }
            if ((next == null) && (iter == this.trIter) && (this.footerIter != null)) {
                next = this.footerIter.getFirstRow();
            }
            if ((next == null) && (iter == this.footerIter)) {
                //TODO This could be bad for memory consumption because it already causes the
                //whole body iterator to be prefetched!
                prev = this.trIter.getLastRow();
            }
            log.debug(prev + " - " + row + " - " + next);
            
            //Determine the grid units necessary for getting all the borders right
            int guCount = row.getGridUnits().size();
            if (prev != null) {
                guCount = Math.max(guCount, prev.getGridUnits().size());
            }
            if (next != null) {
                guCount = Math.max(guCount, next.getGridUnits().size());
            }
            GridUnit gu = (GridUnit)row.getGridUnits().get(0);
            //Create empty grid units to hold resolved borders of neighbouring cells
            //TODO maybe this needs to be done differently (and sooner)
            for (int i = 0; i < guCount - row.getGridUnits().size(); i++) {
                //TODO This block in untested!
                int pos = row.getGridUnits().size() + i;
                row.getGridUnits().add(new EmptyGridUnit(gu.getRow(), 
                        this.tableLM.getColumns().getColumn(pos + 1), gu.getBody(), 
                        pos));
            }
            
            //Now resolve normal borders
            if (getTableLM().getTable().isSeparateBorderModel()) {
                //nop, borders are already assigned at this point
            } else {
                for (int i = 0; i < row.getGridUnits().size(); i++) {
                    gu = (GridUnit)row.getGridUnits().get(i);
                    GridUnit other;
                    int flags = 0;
                    if (prev != null && i < prev.getGridUnits().size()) {
                        other = (GridUnit)prev.getGridUnits().get(i);
                    } else {
                        other = null;
                    }
                    if ((iter == this.trIter)
                            && gu.getFlag(GridUnit.FIRST_IN_TABLE)
                            && (this.headerIter == null)) {
                        flags |= CollapsingBorderModel.VERTICAL_START_END_OF_TABLE;
                    }
                    if ((iter == this.headerIter)
                            && gu.getFlag(GridUnit.FIRST_IN_TABLE)) {
                        flags |= CollapsingBorderModel.VERTICAL_START_END_OF_TABLE;
                    }
                    gu.resolveBorder(other, 
                            CommonBorderPaddingBackground.BEFORE, flags);
                    
                    flags = 0;
                    if (next != null && i < next.getGridUnits().size()) {
                        other = (GridUnit)next.getGridUnits().get(i);
                    } else {
                        other = null;
                    }
                    if ((iter == this.trIter)
                            && gu.getFlag(GridUnit.LAST_IN_TABLE)
                            && (this.footerIter == null)) {
                        flags |= CollapsingBorderModel.VERTICAL_START_END_OF_TABLE;
                    }
                    if ((iter == this.footerIter)
                            && gu.getFlag(GridUnit.LAST_IN_TABLE)) {
                        flags |= CollapsingBorderModel.VERTICAL_START_END_OF_TABLE;
                    }
                    gu.resolveBorder(other, 
                            CommonBorderPaddingBackground.AFTER, flags);
                }

            }
        }
    }

    /**
     * Creates Knuth elements for a row group (see TableRowIterator.getNextRowGroup()).
     * @param context Active LayoutContext
     * @param alignment alignment indicator
     * @param isHeaderFooter true if currently processing headers/footers
     * @param returnList List to received the generated elements
     * @param rowGroup row group to process
     */
    private void createElementsForRowGroup(LayoutContext context, int alignment, 
            boolean isHeaderFooter, LinkedList returnList, 
            TableRowIterator.EffRow[] rowGroup) {
        MinOptMax[] rowHeights = new MinOptMax[rowGroup.length];
        TableRowIterator.EffRow row;
        List pgus = new java.util.ArrayList(); //holds a list of a row's primary grid units
        for (int rgi = 0; rgi < rowGroup.length; rgi++) {
            row = rowGroup[rgi];
            rowHeights[rgi] = new MinOptMax(0, 0, Integer.MAX_VALUE);
            
            pgus.clear();
            TableRow tableRow = null;
            int minContentHeight = 0;
            int maxCellHeight = 0;
            for (int j = 0; j < row.getGridUnits().size(); j++) {
                GridUnit gu = (GridUnit)row.getGridUnits().get(j);
                if (gu.isPrimary() && !gu.isEmpty()) {
                    PrimaryGridUnit primary = (PrimaryGridUnit)gu;
                    primary.getCellLM().setParent(tableLM);

                    //Determine the table-row if any
                    if (tableRow == null) {
                        tableRow = primary.getRow();
                        
                        //Check for bpd on row, see CSS21, 17.5.3 Table height algorithms
                        LengthRangeProperty bpd = tableRow.getBlockProgressionDimension();
                        if (!bpd.getMinimum().isAuto()) {
                            minContentHeight = Math.max(minContentHeight, 
                                    bpd.getMinimum().getLength().getValue());
                        }
                    }

                    //Calculate width of cell
                    int spanWidth = 0;
                    for (int i = primary.getStartCol(); 
                            i < primary.getStartCol() + primary.getCell().getNumberColumnsSpanned();
                            i++) {
                        spanWidth += getTableLM().getColumns().getColumn(i + 1)
                            .getColumnWidth().getValue();
                    }
                    log.info("spanWidth=" + spanWidth);
                    LayoutContext childLC = new LayoutContext(0);
                    childLC.setStackLimit(context.getStackLimit()); //necessary?
                    childLC.setRefIPD(spanWidth);
                    
                    //Get the element list for the cell contents
                    LinkedList elems = primary.getCellLM().getNextKnuthElements(childLC, alignment);
                    primary.setElements(elems);
                    log.debug("Elements: " + elems);
                    
                    //Calculate height of cell contents
                    primary.setContentLength(calcCellHeightFromContents(elems));
                    maxCellHeight = Math.max(maxCellHeight, primary.getContentLength());

                    //Calculate height of row, see CSS21, 17.5.3 Table height algorithms
                    if (gu.isLastGridUnitRowSpan()) {
                        int effCellContentHeight = minContentHeight;
                        LengthRangeProperty bpd = primary.getCell().getBlockProgressionDimension();
                        if (!bpd.getMinimum().isAuto()) {
                            effCellContentHeight = Math.max(effCellContentHeight,
                                    bpd.getMinimum().getLength().getValue());
                        }
                        effCellContentHeight = Math.max(effCellContentHeight, 
                                primary.getContentLength());
                        int halfMaxBorderWidths = primary.getHalfMaxBorderWidth();
                        int padding = 0;
                        CommonBorderPaddingBackground cbpb 
                            = primary.getCell().getCommonBorderPaddingBackground(); 
                        padding += cbpb.getPaddingBefore(false);
                        padding += cbpb.getPaddingAfter(false);
                        int effRowHeight = effCellContentHeight + padding + halfMaxBorderWidths;
                        if (effRowHeight > rowHeights[rgi].min) {
                            //This is the new height of the (grid) row
                            MinOptMaxUtil.extendMinimum(rowHeights[rgi], effRowHeight, false);
                            row.setHeight(rowHeights[rgi]);
                        }
                    }
                    
                    pgus.add(primary);
                }
            }
            
            log.debug(row);
            
            PrimaryGridUnit[] pguArray = new PrimaryGridUnit[pgus.size()];
            pguArray = (PrimaryGridUnit[])pgus.toArray(pguArray);
            LinkedList returnedList = getCombinedKnuthElementsForRow(pguArray, row, 
                    isHeaderFooter);
            if (returnedList != null) {
                returnList.addAll(returnedList);
            }

            /* not necessary anymore
            if (row.getHeight().opt > maxCellHeight) {
                int space = row.getHeight().opt - maxCellHeight;
                KnuthPenalty penalty = (KnuthPenalty)returnList.removeLast();
                //Insert dummy box before penalty
                returnList.add(new KnuthBox(space, new Position(getTableLM()), false));
                returnList.add(penalty);
            }*/
            
            //Calculate row height in row groups with spans
            /*
            if (tableRow != null) {
                LengthRangeProperty bpd = tableRow.getBlockProgressionDimension();
                if (bpd.getOptimum().isAuto()) {
                    rowHeights[rgi] = new MinOptMax(0, 0, Integer.MAX_VALUE);
                } else {
                    rowHeights[rgi] = MinOptMaxUtil.toMinOptMax(bpd);
                }
            } else {
                rowHeights[rgi] = new MinOptMax(0, 0, Integer.MAX_VALUE);
            }*/
            /*
            for (int j = 0; j < row.getGridUnits().size(); j++) {
                GridUnit gu = (GridUnit)row.getGridUnits().get(j);
                if (gu.isLastGridUnitRowSpan() && !gu.isEmpty()) {
                    log.debug(rgi + " - " + gu);
                    MinOptMax effCellHeight; 
                    LengthRangeProperty bpd = gu.getCell().getBlockProgressionDimension();
                    if (bpd.getOptimum().isAuto()) {
                        effCellHeight = new MinOptMax(0, 0, Integer.MAX_VALUE);
                    } else {
                        effCellHeight = MinOptMaxUtil.toMinOptMax(bpd);
                    }
                    int contentLen = gu.getPrimary().getContentLength();
                    if (getTableLM().getTable().isSeparateBorderModel()) {
                        //contentLen += before and after borders of that cell plus half the BPD border-separation 
                    } else {
                        //contentLen += half of before and after borders for that cell
                    }
                    for (int previous = 0; previous < gu.getRowSpanIndex(); previous++) {
                        contentLen -= rowHeights[rgi - previous - 1].opt;
                    }
                    log.debug("->" + contentLen);
                    if (contentLen > effCellHeight.min) {
                        MinOptMaxUtil.extendMinimum(effCellHeight, contentLen, true);
                    }
                    if (effCellHeight.min > rowHeights[rgi].min) {
                        MinOptMaxUtil.extendMinimum(rowHeights[rgi], effCellHeight.min, false);
                    }
                }
            }*/
        }
    }

    private LinkedList getCombinedKnuthElementsForRow(PrimaryGridUnit[] pguArray, 
            TableRowIterator.EffRow row, boolean isHeaderFooter) {
        List[] elementLists = new List[pguArray.length];
        for (int i = 0; i < pguArray.length; i++) {
            if (pguArray[i].hasBPD()) {
                List list = new java.util.ArrayList(1);
                list.add(new KnuthBoxCellWithBPD(
                        pguArray[i].getEffectiveContentLength(), pguArray[i]));
                elementLists[i] = list;
            } else {
                //Copy elements (LinkedList) to array lists to improve element access performance
                elementLists[i] = new java.util.ArrayList(pguArray[i].getElements());
            }
        }
        int[] index = new int[pguArray.length];
        int[] start = new int[pguArray.length];
        int[] end = new int[pguArray.length];
        int[] widths = new int[pguArray.length];
        int[] fullWidths = new int[pguArray.length];
        Arrays.fill(end, -1);
        
        int totalHeight = 0;
        for (int i = 0; i < pguArray.length; i++) {
            fullWidths[i] = pguArray[i].getContentLength();
            totalHeight = Math.max(totalHeight, fullWidths[i]);
        }
        int laststep = 0;
        int step;
        int addedBoxLen = 0;
        LinkedList returnList = new LinkedList();
        while ((step = getNextStep(laststep, elementLists, index, start, end, 
                widths, fullWidths)) > 0) {
            int increase = step - laststep;
            int penaltyLen = step + getMaxRemainingHeight(fullWidths, widths) - totalHeight;
            int boxLen = step - addedBoxLen - penaltyLen;
            addedBoxLen += boxLen;
            
            log.debug(step + " " + increase + " box=" + boxLen + " penalty=" + penaltyLen);
            
            //Put all involved grid units into a list
            List gridUnitParts = new java.util.ArrayList(pguArray.length);
            for (int i = 0; i < pguArray.length; i++) {
                if (end[i] >= start[i]) {
                    if (start[i] == 0 && end[i] == 0 
                            && elementLists[i].size() == 1
                            && elementLists[i].get(0) instanceof KnuthBoxCellWithBPD) {
                        gridUnitParts.add(new GridUnitPart(pguArray[i], 
                                0, pguArray[i].getElements().size() - 1));
                    } else {
                        gridUnitParts.add(new GridUnitPart(pguArray[i], start[i], end[i]));
                    }
                }
            }
            
            //Create elements for step
            TableContentPosition tcpos = new TableContentPosition(getTableLM(), 
                    gridUnitParts, row);
            returnList.add(new KnuthBox(boxLen, tcpos, false));
            TableHFPenaltyPosition penaltyPos = new TableHFPenaltyPosition(getTableLM());
            if (!isHeaderFooter) {
                if (!getTableLM().getTable().omitHeaderAtBreak()) {
                    penaltyLen += this.headerNetHeight;
                    penaltyPos.headerElements = this.headerList;
                }
                if (!getTableLM().getTable().omitFooterAtBreak()) {
                    penaltyLen += this.footerNetHeight;
                    penaltyPos.footerElements = this.footerList;
                }
            }
            returnList.add(new KnuthPenalty(penaltyLen, 0, false, penaltyPos, false));
            laststep = step;
        }
        return returnList;
    }

    private int getMaxRemainingHeight(int[] fullWidths, int[] widths) {
        int maxW = 0;
        for (int i = 0; i < fullWidths.length; i++) {
            maxW = Math.max(maxW, fullWidths[i] - widths[i]);
        }
        return maxW;
    }
    
    private int getNextStep(int laststep, List[] elementLists, int[] index, 
            int[] start, int[] end, int[] widths, int[] fullWidths) {
        int[] backupWidths = new int[start.length];
        System.arraycopy(widths, 0, backupWidths, 0, backupWidths.length);
        //set starting points
        for (int i = 0; i < start.length; i++) {
            if (end[i] < elementLists[i].size()) {
                start[i] = end[i] + 1;
            } else {
                start[i] = -1; //end of list reached
                end[i] = -1;
            }
        }
        
        //Get next possible sequence for each cell
        int seqCount = 0;
        for (int i = 0; i < start.length; i++) {
            while (end[i] + 1 < elementLists[i].size()) {
                end[i]++;
                KnuthElement el = (KnuthElement)elementLists[i].get(end[i]);
                if (el.isPenalty()) {
                    if (el.getP() < KnuthElement.INFINITE) {
                        //First legal break point
                        break;
                    }
                } else if (el.isGlue()) {
                    KnuthElement prev = (KnuthElement)elementLists[i].get(end[i] - 1);
                    if (prev.isBox()) {
                        //Second legal break point
                        break;
                    }
                    widths[i] += el.getW();
                } else {
                    widths[i] += el.getW();
                }
            }
            if (end[i] < start[i]) {
                widths[i] = backupWidths[i];
            } else {
                seqCount++;
            }
            //log.debug("part " + start[i] + "-" + end[i] + " " + widths[i]);
        }
        if (seqCount == 0) {
            return 0;
        }
        
        //Determine smallest possible step
        int minStep = Integer.MAX_VALUE;
        for (int i = 0; i < widths.length; i++) {
            if (end[i] >= start[i]) {
                minStep = Math.min(widths[i], minStep);
            }
        }

        //Reset bigger-than-minimum sequences
        for (int i = 0; i < widths.length; i++) {
            if (widths[i] > minStep) {
                widths[i] = backupWidths[i];
                end[i] = start[i] - 1;
            }
        }
        if (log.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < widths.length; i++) {
                if (end[i] >= start[i]) {
                    sb.append(i + ": " + start[i] + "-" + end[i] + "(" + widths[i] + "), ");
                } else {
                    sb.append(i + ": skip, ");
                }
            }
            log.debug(sb.toString());
        }
        return minStep;
    }

    private int calcCellHeightFromContents(List elems, int start, int end) {
        ListIterator iter = elems.listIterator(start);
        int count = end - start + 1;
        int len = 0;
        while (iter.hasNext()) {
            KnuthElement el = (KnuthElement)iter.next();
            if (el.isBox()) {
                len += el.getW();
            } else if (el.isGlue()) {
                len += el.getW();
            } else {
                log.debug("Ignoring penalty: " + el);
                //ignore penalties
            }
            count--;
            if (count == 0) {
                break;
            }
        }
        return len;
    }
    
    private int calcCellHeightFromContents(List elems) {
        return calcCellHeightFromContents(elems, 0, elems.size() - 1);
    }
    
    protected int getXOffsetOfGridUnit(GridUnit gu) {
        int col = gu.getStartCol();
        return startXOffset + getTableLM().getColumns().getXOffset(col + 1);
    }
    
    public void addAreas(PositionIterator parentIter, LayoutContext layoutContext) {
        this.usedBPD = 0;
        RowPainter painter = new RowPainter(layoutContext);

        List positions = new java.util.ArrayList();
        Position lastPos = null;
        while (parentIter.hasNext()) {
            Position pos = (Position)parentIter.next();
            lastPos = pos;
            if (pos instanceof TableHeaderFooterPosition) {
                TableHeaderFooterPosition thfpos = (TableHeaderFooterPosition)pos;
                //these positions need to be unpacked
                if (thfpos.header) {
                    //header positions for the last part are the second-to-last element and need to
                    //be handled first before all other TableContentPositions
                    PositionIterator nestedIter = new KnuthPossPosIter(thfpos.nestedElements);
                    while (nestedIter.hasNext()) {
                        Position containedPos = (Position)nestedIter.next();
                        if (containedPos instanceof TableContentPosition) {
                            TableContentPosition tcpos = (TableContentPosition)containedPos;
                            painter.handleTableContentPosition(tcpos);
                        } else {
                            log.debug("Ignoring position: " + containedPos);
                        }
                    }
                } else {
                    //Positions for footers are simply added at the end
                    PositionIterator iter = new KnuthPossPosIter(thfpos.nestedElements);
                    while (iter.hasNext()) {
                        Position containedPos = (Position)iter.next();
                        positions.add(containedPos);
                    }
                }
            } else if (pos instanceof TableHFPenaltyPosition) {
                //ignore for now, see special handling below if break is at a penalty
                //Only if the last position in this part/page us such a position it will be used 
            } else {
                //leave order as is for the rest
                positions.add(pos);
            }
        }
        if (lastPos instanceof TableHFPenaltyPosition) {
            TableHFPenaltyPosition penaltyPos = (TableHFPenaltyPosition)lastPos;
            log.debug("Break at penalty!");
            if (penaltyPos.headerElements != null) {
                //Header positions for the penalty position are in the last element and need to
                //be handled first before all other TableContentPositions
                PositionIterator nestedIter = new KnuthPossPosIter(penaltyPos.headerElements);
                while (nestedIter.hasNext()) {
                    Position containedPos = (Position)nestedIter.next();
                    if (containedPos instanceof TableContentPosition) {
                        TableContentPosition tcpos = (TableContentPosition)containedPos;
                        painter.handleTableContentPosition(tcpos);
                    } else {
                        log.debug("Ignoring position: " + containedPos);
                    }
                }
            }
            if (penaltyPos.footerElements != null) {
                //Positions for footers are simply added at the end
                PositionIterator iter = new KnuthPossPosIter(penaltyPos.footerElements);
                while (iter.hasNext()) {
                    Position containedPos = (Position)iter.next();
                    positions.add(containedPos);
                }
            }
        }

        
        Iterator posIter = positions.iterator();
        //Iterate over all steps
        while (posIter.hasNext()) {
            Position pos = (Position)posIter.next();
            if (pos instanceof TableContentPosition) {
                TableContentPosition tcpos = (TableContentPosition)pos;
                painter.handleTableContentPosition(tcpos);
            } else {
                log.debug("Ignoring position: " + pos);
            }
        }
        
        //Calculate the height of the row
        int maxLen = painter.addAreasAndFlushRow(true);
        this.usedBPD += painter.getAccumulatedBPD();
    }
   
    private class RowPainter {
        
        private TableRow rowFO = null;
        private int colCount = getColumns().getColumnCount();
        private int yoffset = 0;
        private int accumulatedBPD = 0;
        private TableRowIterator.EffRow lastRow = null;
        private LayoutContext layoutContext;
        private int lastRowHeight = 0;

        //These three variables are our buffer to recombine the individual steps into cells
        private PrimaryGridUnit[] gridUnits = new PrimaryGridUnit[colCount];
        private int[] start = new int[colCount];
        private int[] end = new int[colCount];
        private int[] partLength = new int[colCount];
        
        public RowPainter(LayoutContext layoutContext) {
            this.layoutContext = layoutContext;
        }
        
        public int getAccumulatedBPD() {
            return this.accumulatedBPD;
        }
        
        private void handleTableContentPosition(TableContentPosition tcpos) {
            rowFO = null;
            if (lastRow != tcpos.row && lastRow != null) {
                yoffset += lastRow.getHeight().opt;
                this.accumulatedBPD += lastRow.getHeight().opt;
            }
            lastRow = tcpos.row;
            Iterator partIter = tcpos.gridUnitParts.iterator();
            //Iterate over all grid units in the current step
            while (partIter.hasNext()) {
                GridUnitPart gup = (GridUnitPart)partIter.next();
                log.debug(">" + gup);
                int colIndex = gup.pgu.getStartCol();
                if (gridUnits[colIndex] != gup.pgu) {
                    gridUnits[colIndex] = gup.pgu;
                    start[colIndex] = gup.start;
                    end[colIndex] = gup.end;
                } else {
                    end[colIndex] = gup.end;
                }
                if (rowFO == null) {
                    //Find the row if any
                    rowFO = gridUnits[colIndex].getRow();
                }
            }
            
            //Calculate the height of the row
            int maxLen = addAreasAndFlushRow(false);
            lastRowHeight = tcpos.row.getHeight().opt;
        }
        
        private int addAreasAndFlushRow(boolean finalFlush) {
            int maxLen = 0;
            for (int i = 0; i < gridUnits.length; i++) {
                if ((gridUnits[i] != null) 
                        && (finalFlush || (end[i] == gridUnits[i].getElements().size() - 1))) {
                    log.debug("getting len for " + i + " " 
                            + start[i] + "-" + end[i]);
                    int len = calcCellHeightFromContents(
                            gridUnits[i].getElements(), start[i], end[i]);
                    partLength[i] = len;
                    log.debug("len of part: " + len);
                    maxLen = Math.max(maxLen, len);
                    //maxLen = Math.max(maxLen, getExplicitCellHeight(gridUnits[i]));
                }
            }
            
            //Add areas for row
            //addRowBackgroundArea(rowFO, lastRowHeight, layoutContext.getRefIPD(), yoffset);
            for (int i = 0; i < gridUnits.length; i++) {
                if ((gridUnits[i] != null) 
                        && (finalFlush || (end[i] == gridUnits[i].getElements().size() - 1))) {
                    if (log.isDebugEnabled()) {
                        log.debug((finalFlush ? "final " : "") + "flushing..." + i + " " 
                                + start[i] + "-" + end[i]);
                    }
                    addAreasForCell(gridUnits[i], start[i], end[i], 
                            layoutContext, lastRow, yoffset, 
                            partLength[i], lastRow.getHeight().opt);
                    gridUnits[i] = null;
                    start[i] = 0;
                    end[i] = 0;
                }
            }
            if (finalFlush) {
                this.accumulatedBPD += lastRowHeight; //for last row
            }
            return maxLen;
        }

    }

    /*
    private int getExplicitCellHeight(PrimaryGridUnit pgu) {
        int len = 0;
        if (!pgu.getCell().getBlockProgressionDimension().getOptimum().isAuto()) {
            len = pgu.getCell().getBlockProgressionDimension()
                    .getOptimum().getLength().getValue();
        }
        if (pgu.getRow() != null 
                && !pgu.getRow().getBlockProgressionDimension().getOptimum().isAuto()) {
            len = Math.max(len, pgu.getRow().getBlockProgressionDimension()
                    .getOptimum().getLength().getValue());
        }
        return len;
    }*/
    
    private void addAreasForCell(PrimaryGridUnit gu, int start, int end, 
            LayoutContext layoutContext, TableRowIterator.EffRow row, 
            int yoffset, int contentHeight, int rowHeight) {
        Cell cellLM = gu.getCellLM();
        cellLM.setXOffset(getXOffsetOfGridUnit(gu));
        cellLM.setYOffset(yoffset);
        cellLM.setContentHeight(contentHeight);
        cellLM.setRowHeight(rowHeight);
        //cellLM.setRowHeight(row.getHeight().opt);
        cellLM.addAreas(new KnuthPossPosIter(gu.getElements(), 
                start, end + 1), layoutContext);
    }
    
    /**
     * Get the area for a row for background.
     * @param row the table-row object or null
     * @return the row area or null if there's no background to paint
     */
    public Block getRowArea(TableRow row) {
        if (row == null || !row.getCommonBorderPaddingBackground().hasBackground()) {
            return null;
        } else {
            Block block = new Block();
            block.addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
            block.setPositioning(Block.ABSOLUTE);
            TraitSetter.addBackground(block, row.getCommonBorderPaddingBackground());
            return block;
        }
    }

    public void addRowBackgroundArea(TableRow row, int bpd, int ipd, int yoffset) {
        //Add row background if any
        Block rowBackground = getRowArea(row);
        if (rowBackground != null) {
            rowBackground.setBPD(bpd);
            rowBackground.setIPD(ipd);
            rowBackground.setXOffset(this.startXOffset);
            rowBackground.setYOffset(yoffset);
            getTableLM().addChildArea(rowBackground);
        }
    }
    
    
    /**
     * Sets the overall starting x-offset. Used for proper placement of cells.
     * @param startXOffset starting x-offset (table's start-indent)
     */
    public void setStartXOffset(int startXOffset) {
        this.startXOffset = startXOffset;
    }

    public int getUsedBPD() {
        return this.usedBPD;
    }
    
    private class GridUnitPart {
        
        protected PrimaryGridUnit pgu;
        protected int start;
        protected int end;
        
        protected GridUnitPart(PrimaryGridUnit pgu, int start, int end) {
            this.pgu = pgu;
            this.start = start;
            this.end = end;
        }
        
        /** @see java.lang.Object#toString() */
        public String toString() {
            StringBuffer sb = new StringBuffer("Part: ");
            sb.append(start).append("-").append(end);
            sb.append(" ").append(pgu);
            return sb.toString();
        }
        
    }
    
    public class TableContentPosition extends Position {

        protected List gridUnitParts;
        protected TableRowIterator.EffRow row;
        
        protected TableContentPosition(LayoutManager lm, List gridUnitParts, 
                TableRowIterator.EffRow row) {
            super(lm);
            this.gridUnitParts = gridUnitParts;
            this.row = row;
        }
        
        /** @see java.lang.Object#toString() */
        public String toString() {
            StringBuffer sb = new StringBuffer("TableContentPosition {");
            sb.append(gridUnitParts);
            sb.append("}");
            return sb.toString();
        }
    }
    
    public class TableHeaderFooterPosition extends Position {
        
        protected boolean header;
        protected List nestedElements;
        
        protected TableHeaderFooterPosition(LayoutManager lm, 
                boolean header, List nestedElements) {
            super(lm);
            this.header = header;
            this.nestedElements = nestedElements;
        }
        
        /** @see java.lang.Object#toString() */
        public String toString() {
            StringBuffer sb = new StringBuffer("Table");
            sb.append(header ? "Header" : "Footer");
            sb.append("Position {");
            sb.append(nestedElements);
            sb.append("}");
            return sb.toString();
        }
    }

    public class TableHFPenaltyPosition extends Position {
        
        protected List headerElements;
        protected List footerElements;
        
        protected TableHFPenaltyPosition(LayoutManager lm) {
            super(lm);
        }
        
        /** @see java.lang.Object#toString() */
        public String toString() {
            StringBuffer sb = new StringBuffer("TableHFPenaltyPosition");
            sb.append(" {");
            sb.append("header:");
            sb.append(headerElements);
            sb.append(", footer:");
            sb.append(footerElements);
            sb.append("}");
            return sb.toString();
        }
    }
    
    private class KnuthBoxCellWithBPD extends KnuthBox {
        
        private PrimaryGridUnit pgu;
        
        public KnuthBoxCellWithBPD(int w, PrimaryGridUnit pgu) {
            super(w, null, true);
            this.pgu = pgu;
        }
}

}
