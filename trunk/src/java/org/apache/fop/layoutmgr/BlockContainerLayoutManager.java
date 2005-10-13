/*
 * Copyright 1999-2005 The Apache Software Foundation.
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

package org.apache.fop.layoutmgr;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.awt.Point;
import java.awt.geom.Rectangle2D;

import org.apache.fop.area.Area;
import org.apache.fop.area.BlockViewport;
import org.apache.fop.area.Block;
import org.apache.fop.area.Trait;
import org.apache.fop.fo.flow.BlockContainer;
import org.apache.fop.fo.properties.CommonAbsolutePosition;
import org.apache.fop.area.CTM;
import org.apache.fop.datatypes.FODimension;
import org.apache.fop.datatypes.Length;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.traits.SpaceVal;

/**
 * LayoutManager for a block-container FO.
 */
public class BlockContainerLayoutManager extends BlockStackingLayoutManager {
    
    private BlockViewport viewportBlockArea;
    private Block referenceArea;

    private CommonAbsolutePosition abProps;
    private FODimension relDims;
    private CTM absoluteCTM;
    private boolean clip = false;
    private Length width;
    private Length height;
    private int vpContentIPD;
    private int vpContentBPD;
    
    // When viewport should grow with the content.
    private boolean autoHeight = true; 
    
    /* holds the (one-time use) fo:block space-before
    and -after properties.  Large fo:blocks are split
    into multiple Area.Blocks to accomodate the subsequent
    regions (pages) they are placed on.  space-before
    is applied at the beginning of the first
    Block and space-after at the end of the last Block
    used in rendering the fo:block.
    */
    //TODO space-before|after: handle space-resolution rules
    private MinOptMax foBlockSpaceBefore;
    private MinOptMax foBlockSpaceAfter;
    
    /**
     * Create a new block container layout manager.
     * @param node block-container node to create the layout manager for.
     */
    public BlockContainerLayoutManager(BlockContainer node) {
        super(node);
    }
    
    public void initialize() {
        abProps = getBlockContainerFO().getCommonAbsolutePosition();
        foBlockSpaceBefore = new SpaceVal(getBlockContainerFO().getCommonMarginBlock()
                    .spaceBefore, this).getSpace();
        foBlockSpaceAfter = new SpaceVal(getBlockContainerFO().getCommonMarginBlock()
                    .spaceAfter, this).getSpace();

        boolean rotated = (getBlockContainerFO().getReferenceOrientation() % 180 != 0);
        if (rotated) {
            height = getBlockContainerFO().getInlineProgressionDimension()
                            .getOptimum(this).getLength();
            width = getBlockContainerFO().getBlockProgressionDimension()
                            .getOptimum(this).getLength();
        } else {
            height = getBlockContainerFO().getBlockProgressionDimension()
                            .getOptimum(this).getLength();
            width = getBlockContainerFO().getInlineProgressionDimension()
                            .getOptimum(this).getLength();
        }
        
        bpUnit = 0; //layoutProps.blockProgressionUnit;
        if (bpUnit == 0) {
            // use optimum space values
            adjustedSpaceBefore = getBlockContainerFO().getCommonMarginBlock()
                .spaceBefore.getSpace().getOptimum(this).getLength().getValue(this);
            adjustedSpaceAfter = getBlockContainerFO().getCommonMarginBlock()
                .spaceAfter.getSpace().getOptimum(this).getLength().getValue(this);
        } else {
            // use minimum space values
            adjustedSpaceBefore = getBlockContainerFO().getCommonMarginBlock()
                .spaceBefore.getSpace().getMinimum(this).getLength().getValue(this);
            adjustedSpaceAfter = getBlockContainerFO().getCommonMarginBlock()
                .spaceAfter.getSpace().getMinimum(this).getLength().getValue(this);
        }
    }

    /** @return the content IPD */
    protected int getRotatedIPD() {
        return getBlockContainerFO().getInlineProgressionDimension()
                .getOptimum(this).getLength().getValue(this);
    }

    private int getSpaceBefore() {
        return foBlockSpaceBefore.opt;
    }
    
    private int getBPIndents() {
        int indents = 0;
        indents += getBlockContainerFO().getCommonMarginBlock()
                    .spaceBefore.getOptimum(this).getLength().getValue(this);
        indents += getBlockContainerFO().getCommonMarginBlock()
                    .spaceAfter.getOptimum(this).getLength().getValue(this);
        indents += getBlockContainerFO().getCommonBorderPaddingBackground()
                    .getBPPaddingAndBorder(false, this);
        return indents;
    }
    
    private int getIPIndents() {
        int iIndents = 0;
        iIndents += getBlockContainerFO().getCommonMarginBlock().startIndent.getValue(this);
        iIndents += getBlockContainerFO().getCommonMarginBlock().endIndent.getValue(this);
        return iIndents;
    }
    
    private boolean isAbsoluteOrFixed() {
        return (abProps.absolutePosition == EN_ABSOLUTE) 
                || (abProps.absolutePosition == EN_FIXED);
    }

    private boolean isFixed() {
        return (abProps.absolutePosition == EN_FIXED);
    }
    
    /** @see org.apache.fop.layoutmgr.LayoutManager */
    public LinkedList getNextKnuthElements(LayoutContext context, int alignment) {
        if (isAbsoluteOrFixed()) {
            return getNextKnuthElementsAbsolute(context, alignment);
        }
        
        autoHeight = false;
        //boolean rotated = (getBlockContainerFO().getReferenceOrientation() % 180 != 0);
        referenceIPD = context.getRefIPD();
        int maxbpd = context.getStackLimit().opt;
        int allocBPD, allocIPD;
        if (height.getEnum() != EN_AUTO) {
            allocBPD = height.getValue(this); //this is the content-height
            allocBPD += getBPIndents();
        } else {
            allocBPD = maxbpd;
            autoHeight = true;
        }
        if (width.getEnum() != EN_AUTO) {
            allocIPD = width.getValue(this); //this is the content-width
            allocIPD += getIPIndents();
        } else {
            allocIPD = referenceIPD;
        }

        vpContentBPD = allocBPD - getBPIndents();
        vpContentIPD = allocIPD - getIPIndents();
        setContentAreaIPD(vpContentIPD);
        double contentRectOffsetX = 0;
        contentRectOffsetX += getBlockContainerFO()
                .getCommonMarginBlock().startIndent.getValue(this);
        double contentRectOffsetY = 0;
        contentRectOffsetY += getBlockContainerFO()
                .getCommonBorderPaddingBackground().getBorderBeforeWidth(false);
        contentRectOffsetY += getBlockContainerFO()
                .getCommonBorderPaddingBackground().getPaddingBefore(false, this);
        
        Rectangle2D rect = new Rectangle2D.Double(
                contentRectOffsetX, contentRectOffsetY, 
                vpContentIPD, vpContentBPD);
        relDims = new FODimension(0, 0);
        absoluteCTM = CTM.getCTMandRelDims(getBlockContainerFO().getReferenceOrientation(),
                getBlockContainerFO().getWritingMode(), rect, relDims);

        MinOptMax stackLimit = new MinOptMax(relDims.bpd);

        LinkedList returnedList = null;
        LinkedList contentList = new LinkedList();
        LinkedList returnList = new LinkedList();
        
        if (!bBreakBeforeServed) {
            try {
                if (addKnuthElementsForBreakBefore(returnList)) {
                    return returnList;
                }
            } finally {
                bBreakBeforeServed = true;
            }
        }

        if (!bSpaceBeforeServed) {
            addKnuthElementsForSpaceBefore(returnList, alignment);
            bSpaceBeforeServed = true;
        }
        
        addKnuthElementsForBorderPaddingBefore(returnList);

        if (autoHeight) {
            BlockLevelLayoutManager curLM; // currently active LM
            BlockLevelLayoutManager prevLM = null; // previously active LM
            while ((curLM = (BlockLevelLayoutManager) getChildLM()) != null) {
                LayoutContext childLC = new LayoutContext(0);
                // curLM is a ?
                childLC.setStackLimit(MinOptMax.subtract(context
                        .getStackLimit(), stackLimit));
                childLC.setRefIPD(relDims.ipd);
                childLC.setWritingMode(getBlockContainerFO().getWritingMode());

                // get elements from curLM
                returnedList = curLM.getNextKnuthElements(childLC, alignment);
                if (returnedList.size() == 1
                        && ((KnuthElement)returnedList.getFirst()).isPenalty()
                        && ((KnuthPenalty)returnedList.getFirst()).getP() 
                                == -KnuthElement.INFINITE) {
                    // a descendant of this block has break-before
                    if (returnList.size() == 0) {
                        // the first child (or its first child ...) has
                        // break-before;
                        // all this block, including space before, will be put in
                        // the
                        // following page
                        bSpaceBeforeServed = false;
                    }
                    contentList.addAll(returnedList);

                    // "wrap" the Position inside each element
                    // moving the elements from contentList to returnList
                    returnedList = new LinkedList();
                    wrapPositionElements(contentList, returnList);

                    return returnList;
                } else {
                    if (prevLM != null) {
                        // there is a block handled by prevLM
                        // before the one handled by curLM
                        if (mustKeepTogether() 
                                || prevLM.mustKeepWithNext()
                                || curLM.mustKeepWithPrevious()) {
                            // add an infinite penalty to forbid a break between
                            // blocks
                            contentList.add(new KnuthPenalty(0,
                                    KnuthElement.INFINITE, false,
                                    new Position(this), false));
                        } else if (!((KnuthElement) contentList.getLast()).isGlue()) {
                            // add a null penalty to allow a break between blocks
                            contentList.add(new KnuthPenalty(0, 0, false,
                                    new Position(this), false));
                        } else {
                            // the last element in contentList is a glue;
                            // it is a feasible breakpoint, there is no need to add
                            // a penalty
                        }
                    }
                    contentList.addAll(returnedList);
                    if (returnedList.size() == 0) {
                        //Avoid NoSuchElementException below (happens with empty blocks)
                        continue;
                    }
                    if (((KnuthElement) returnedList.getLast()).isPenalty()
                            && ((KnuthPenalty) returnedList.getLast()).getP() 
                                    == -KnuthElement.INFINITE) {
                        // a descendant of this block has break-after
                        if (curLM.isFinished()) {
                            // there is no other content in this block;
                            // it's useless to add space after before a page break
                            setFinished(true);
                        }

                        returnedList = new LinkedList();
                        wrapPositionElements(contentList, returnList);

                        return returnList;
                    }
                }
                prevLM = curLM;
            }

            returnedList = new LinkedList();
            wrapPositionElements(contentList, returnList);

        } else {
            MinOptMax range = new MinOptMax(relDims.ipd);
            BlockContainerBreaker breaker = new BlockContainerBreaker(this, range);
            breaker.doLayout(relDims.bpd);
            boolean contentOverflows = false;
            if (!breaker.isEmpty()) {
                contentOverflows = (breaker.deferredAlg.getPageBreaks().size() > 1);
            }

            Position bcPosition = new BlockContainerPosition(this, breaker);
            returnList.add(new KnuthBox(vpContentBPD, notifyPos(bcPosition), false));
            //TODO Handle min/opt/max for block-progression-dimension
            /* These two elements will be used to add stretchability to the above box
            returnList.add(new KnuthPenalty(0, KnuthElement.INFINITE, 
                                   false, returnPosition, false));
            returnList.add(new KnuthGlue(0, 1 * constantLineHeight, 0,
                                   LINE_NUMBER_ADJUSTMENT, returnPosition, false));
            */

            if (contentOverflows) {
                log.warn("Contents overflow block-container viewport: clipping");
                if (getBlockContainerFO().getOverflow() == EN_HIDDEN) {
                    clip = true;
                } else if (getBlockContainerFO().getOverflow() == EN_ERROR_IF_OVERFLOW) {
                    //TODO Throw layout exception
                    clip = true;
                }
            }
        }
        addKnuthElementsForBorderPaddingAfter(returnList);
        addKnuthElementsForSpaceAfter(returnList, alignment);
        addKnuthElementsForBreakAfter(returnList);

        setFinished(true);
        return returnList;
    }
    
    private LinkedList getNextKnuthElementsAbsolute(LayoutContext context, int alignment) {
        autoHeight = false;

        Point offset = getAbsOffset();
        int allocBPD, allocIPD;
        if (height.getEnum() != EN_AUTO) {
            allocBPD = height.getValue(this); //this is the content-height
            allocBPD += getBPIndents();
        } else {
            allocBPD = 0;
            if (abProps.bottom.getEnum() != EN_AUTO) {
                int availHeight;
                if (isFixed()) {
                    availHeight = (int)getCurrentPV().getViewArea().getHeight();
                } else {
                    availHeight = context.getStackLimit().opt; 
                }
                allocBPD = availHeight;
                allocBPD -= offset.y;
                if (abProps.bottom.getEnum() != EN_AUTO) {
                    allocBPD -= abProps.bottom.getValue(this);
                    if (allocBPD < 0) {
                        log.error("The current combination of top and bottom properties results"
                                + " in a negative extent for the block-container. 'bottom' may be"
                                + " at most " + (allocBPD + abProps.bottom.getValue(this)) + " mpt,"
                                + " but was actually " + abProps.bottom.getValue(this) + " mpt."
                                + " The nominal available height is " + availHeight + " mpt.");
                        allocBPD = 0;
                    }
                } else {
                    if (allocBPD < 0) {
                        log.error("The current combination of top and bottom properties results"
                                + " in a negative extent for the block-container. 'top' may be"
                                + " at most " + availHeight + " mpt,"
                                + " but was actually " + offset.y + " mpt."
                                + " The nominal available height is " + availHeight + " mpt.");
                        allocBPD = 0;
                    }
                }
            } else {
                autoHeight = true;
            }
        }
        if (width.getEnum() != EN_AUTO) {
            allocIPD = width.getValue(this); //this is the content-width
            allocIPD += getIPIndents();
        } else {
            int availWidth;
            if (isFixed()) {
                availWidth = (int)getCurrentPV().getViewArea().getWidth(); 
            } else {
                availWidth = context.getRefIPD();
            }
            allocIPD = availWidth;
            if (abProps.left.getEnum() != EN_AUTO) {
                allocIPD -= abProps.left.getValue(this);
            }
            if (abProps.right.getEnum() != EN_AUTO) {
                allocIPD -= abProps.right.getValue(this);
                if (allocIPD < 0) {
                    log.error("The current combination of left and right properties results"
                            + " in a negative extent for the block-container. 'right' may be"
                            + " at most " + (allocIPD + abProps.right.getValue(this)) + " mpt,"
                            + " but was actually " + abProps.right.getValue(this) + " mpt."
                            + " The nominal available width is " + availWidth + " mpt.");
                    allocIPD = 0;
                }
            } else {
                if (allocIPD < 0) {
                    log.error("The current combination of left and right properties results"
                            + " in a negative extent for the block-container. 'left' may be"
                            + " at most " + allocIPD + " mpt,"
                            + " but was actually " + abProps.left.getValue(this) + " mpt."
                            + " The nominal available width is " + availWidth + " mpt.");
                    allocIPD = 0;
                }
            }
        }

        vpContentBPD = allocBPD - getBPIndents();
        vpContentIPD = allocIPD - getIPIndents();
        
        double contentRectOffsetX = offset.getX();
        contentRectOffsetX += getBlockContainerFO()
                .getCommonMarginBlock().startIndent.getValue(this);
        double contentRectOffsetY = offset.getY();
        contentRectOffsetY += getSpaceBefore();
        contentRectOffsetY += getBlockContainerFO()
                .getCommonBorderPaddingBackground().getBorderBeforeWidth(false);
        contentRectOffsetY += getBlockContainerFO()
                .getCommonBorderPaddingBackground().getPaddingBefore(false, this);
        
        Rectangle2D rect = new Rectangle2D.Double(
                contentRectOffsetX, contentRectOffsetY, 
                vpContentIPD, vpContentBPD);
        relDims = new FODimension(0, 0);
        absoluteCTM = CTM.getCTMandRelDims(
                getBlockContainerFO().getReferenceOrientation(),
                getBlockContainerFO().getWritingMode(), 
                rect, relDims);

        MinOptMax range = new MinOptMax(relDims.ipd);
        BlockContainerBreaker breaker = new BlockContainerBreaker(this, range);
        breaker.doLayout(relDims.bpd);
        boolean contentOverflows = breaker.isOverflow();
        LinkedList returnList = new LinkedList();
        if (!breaker.isEmpty()) {
            Position bcPosition = new BlockContainerPosition(this, breaker);
            returnList.add(new KnuthBox(0, notifyPos(bcPosition), false));
    
            //TODO Maybe check for page overflow when autoHeight=true
            if (!autoHeight & (contentOverflows/*usedBPD > relDims.bpd*/)) {
                log.warn("Contents overflow block-container viewport: clipping");
                if (getBlockContainerFO().getOverflow() == EN_HIDDEN) {
                    clip = true;
                } else if (getBlockContainerFO().getOverflow() == EN_ERROR_IF_OVERFLOW) {
                    //TODO Throw layout exception
                    clip = true;
                }
            }
        }

        setFinished(true);
        return returnList;
    }
    
    private class BlockContainerPosition extends NonLeafPosition {

        private BlockContainerBreaker breaker;

        public BlockContainerPosition(LayoutManager lm, BlockContainerBreaker breaker) {
            super(lm, null);
            this.breaker = breaker;
        }
        
        public BlockContainerBreaker getBreaker() {
            return this.breaker;
        }
        
    }
    
    private class BlockContainerBreaker extends AbstractBreaker {
        
        private BlockContainerLayoutManager bclm;
        private MinOptMax ipd;
        
        //Info for deferred adding of areas
        private PageBreakingAlgorithm deferredAlg;
        private BlockSequence deferredOriginalList;
        private BlockSequence deferredEffectiveList;
        
        public BlockContainerBreaker(BlockContainerLayoutManager bclm, MinOptMax ipd) {
            this.bclm = bclm;
            this.ipd = ipd;
        }

        /** @see org.apache.fop.layoutmgr.AbstractBreaker#observeElementList(java.util.List) */
        protected void observeElementList(List elementList) {
            ElementListObserver.observe(elementList, "block-container", 
                    bclm.getBlockContainerFO().getId());
        }
        
        /** @see org.apache.fop.layoutmgr.AbstractBreaker#isPartOverflowRecoveryActivated() */
        protected boolean isPartOverflowRecoveryActivated() {
            //For block-containers, this must be disabled because of wanted overflow.
            return false;
        }

        public int getDifferenceOfFirstPart() {
            PageBreakPosition pbp = (PageBreakPosition)this.deferredAlg.getPageBreaks().getFirst();
            return pbp.difference;
        }
        
        public boolean isOverflow() {
            if (isEmpty()) {
                return false;
            } else {
                return (deferredAlg.getPageBreaks().size() > 1);
            }
        }
        
        protected LayoutManager getTopLevelLM() {
            return bclm;
        }

        protected LayoutContext createLayoutContext() {
            LayoutContext lc = super.createLayoutContext();
            lc.setRefIPD(ipd.opt);
            lc.setWritingMode(getBlockContainerFO().getWritingMode());
            return lc;
        }
        
        protected LinkedList getNextKnuthElements(LayoutContext context, int alignment) {
            LayoutManager curLM; // currently active LM
            LinkedList returnList = new LinkedList();

            while ((curLM = getChildLM()) != null) {
                LayoutContext childLC = new LayoutContext(0);
                childLC.setStackLimit(context.getStackLimit());
                childLC.setRefIPD(context.getRefIPD());
                childLC.setWritingMode(getBlockContainerFO().getWritingMode());
                
                LinkedList returnedList = null;
                if (!curLM.isFinished()) {
                    returnedList = curLM.getNextKnuthElements(childLC, alignment);
                }
                if (returnedList != null) {
                    bclm.wrapPositionElements(returnedList, returnList);
                    //returnList.addAll(returnedList);
                }
            }
            setFinished(true);
            return returnList;
        }

        protected int getCurrentDisplayAlign() {
            return getBlockContainerFO().getDisplayAlign();
        }
        
        protected boolean hasMoreContent() {
            return !isFinished();
        }
        
        protected void addAreas(PositionIterator posIter, LayoutContext context) {
            AreaAdditionUtil.addAreas(bclm, posIter, context);    
        }
        
        protected void doPhase3(PageBreakingAlgorithm alg, int partCount, 
                BlockSequence originalList, BlockSequence effectiveList) {
            //Defer adding of areas until addAreas is called by the parent LM
            this.deferredAlg = alg;
            this.deferredOriginalList = originalList;
            this.deferredEffectiveList = effectiveList;
        }
        
        protected void finishPart(PageBreakingAlgorithm alg, PageBreakPosition pbp) {
            //nop for bclm
        }
        
        protected LayoutManager getCurrentChildLM() {
            return curChildLM;
        }
        
        public void addContainedAreas() {
            if (isEmpty()) {
                return;
            }
            //Rendering all parts (not just the first) at once for the case where the parts that 
            //overflow should be visible.
            //TODO Check if this has any unwanted side-effects. Feels a bit like a hack.
            addAreas(this.deferredAlg, 
                    /*1*/ this.deferredAlg.getPageBreaks().size(), 
                    this.deferredOriginalList, this.deferredEffectiveList);
        }
        
    }
    
    private Point getAbsOffset() {
        int x = 0;
        int y = 0;
        if (abProps.left.getEnum() != EN_AUTO) {
            x = abProps.left.getValue(this);
        }
        if (abProps.top.getEnum() != EN_AUTO) {
            y = abProps.top.getValue(this);
        }
        return new Point(x, y);
    }
    
    /**
     * @see org.apache.fop.layoutmgr.LayoutManager#addAreas(org.apache.fop.layoutmgr.PositionIterator, org.apache.fop.layoutmgr.LayoutContext)
     */
    public void addAreas(PositionIterator parentIter,
            LayoutContext layoutContext) {
        getParentArea(null);

        // if this will create the first block area in a page
        // and display-align is bottom or center, add space before
        if (layoutContext.getSpaceBefore() > 0) {
            addBlockSpacing(0.0, new MinOptMax(layoutContext.getSpaceBefore()));
        }

        LayoutManager childLM = null;
        LayoutManager lastLM = null;
        LayoutContext lc = new LayoutContext(0);
        // set space after in the LayoutContext for children
        if (layoutContext.getSpaceAfter() > 0) {
            lc.setSpaceAfter(layoutContext.getSpaceAfter());
        }
        BlockContainerPosition bcpos = null;
        PositionIterator childPosIter;

        // "unwrap" the NonLeafPositions stored in parentIter
        // and put them in a new list;
        LinkedList positionList = new LinkedList();
        Position pos;
        boolean bSpaceBefore = false;
        boolean bSpaceAfter = false;
        Position firstPos = null;
        Position lastPos = null;
        while (parentIter.hasNext()) {
            pos = (Position) parentIter.next();
            //System.out.println("pos = " + pos.getClass().getName());
            if (pos.getIndex() >= 0) {
                if (firstPos == null) {
                    firstPos = pos;
                }
                lastPos = pos;
            }
            Position innerPosition = null;
            if (pos instanceof NonLeafPosition) {
                innerPosition = ((NonLeafPosition)pos).getPosition();
            }
            if (pos instanceof BlockContainerPosition) {
                if (bcpos != null) {
                    throw new IllegalStateException("Only one BlockContainerPosition allowed");
                }
                bcpos = (BlockContainerPosition)pos; 
                //Add child areas inside the reference area
                //bcpos.getBreaker().addContainedAreas();
            } else if (innerPosition == null) {
                if (pos instanceof NonLeafPosition) {
                    // pos was created by this BCLM and was inside an element
                    // representing space before or after
                    // this means the space was not discarded
                    if (positionList.size() == 0 && bcpos == null) {
                        // pos was in the element representing space-before
                        bSpaceBefore = true;
                    } else {
                        // pos was in the element representing space-after
                        bSpaceAfter = true;
                    }
                } else {
                    //ignore (probably a Position for a simple penalty between blocks)
                }
            } else if (innerPosition.getLM() == this
                    && !(innerPosition instanceof MappingPosition)) {
                // pos was created by this BlockLM and was inside a penalty
                // allowing or forbidding a page break
                // nothing to do
                /* LF *///System.out.println(" penalty");
            } else {
                // innerPosition was created by another LM
                positionList.add(innerPosition);
                lastLM = innerPosition.getLM();
                /* LF *///System.out.println(" " +
                      // innerPosition.getClass().getName());
            }
        }

        getPSLM().addIDToPage(getBlockContainerFO().getId());
        if (markers != null) {
            getCurrentPV().addMarkers(markers, true, isFirst(firstPos), isLast(lastPos));
        }

        if (bcpos == null) {
            if (bpUnit == 0) {
                // the Positions in positionList were inside the elements
                // created by the LineLM
                childPosIter = new StackingIter(positionList.listIterator());
            } else {
                // the Positions in positionList were inside the elements
                // created by the BCLM in the createUnitElements() method
                //if (((Position) positionList.getLast()) instanceof
                      // LeafPosition) {
                //    // the last item inside positionList is a LeafPosition
                //    // (a LineBreakPosition, more precisely); this means that
                //    // the whole paragraph is on the same page
                //    System.out.println("paragrafo intero");
                //    childPosIter = new KnuthPossPosIter(storedList, 0,
                      // storedList.size());
                //} else {
                //    // the last item inside positionList is a Position;
                //    // this means that the paragraph has been split
                //    // between consecutive pages
                LinkedList splitList = new LinkedList();
                int splitLength = 0;
                int iFirst = ((MappingPosition) positionList.getFirst()).getFirstIndex();
                int iLast = ((MappingPosition) positionList.getLast()).getLastIndex();
                // copy from storedList to splitList all the elements from
                // iFirst to iLast
                ListIterator storedListIterator = storedList.listIterator(iFirst);
                while (storedListIterator.nextIndex() <= iLast) {
                    KnuthElement element = (KnuthElement) storedListIterator
                            .next();
                    // some elements in storedList (i.e. penalty items) were created
                    // by this BlockLM, and must be ignored
                    if (element.getLayoutManager() != this) {
                        splitList.add(element);
                        splitLength += element.getW();
                        lastLM = element.getLayoutManager();
                    }
                }
                //System.out.println("addAreas riferito a storedList da " +
                      // iFirst + " a " + iLast);
                //System.out.println("splitLength= " + splitLength
                //                   + " (" + neededUnits(splitLength) + " unita') "
                //                   + (neededUnits(splitLength) * bpUnit - splitLength) + " spazi");
                // add space before and / or after the paragraph
                // to reach a multiple of bpUnit
                if (bSpaceBefore && bSpaceAfter) {
                    foBlockSpaceBefore = new SpaceVal(getBlockContainerFO()
                                .getCommonMarginBlock().spaceBefore, this).getSpace();
                    foBlockSpaceAfter = new SpaceVal(getBlockContainerFO()
                                .getCommonMarginBlock().spaceAfter, this).getSpace();
                    adjustedSpaceBefore = (neededUnits(splitLength
                            + foBlockSpaceBefore.min
                            + foBlockSpaceAfter.min)
                            * bpUnit - splitLength) / 2;
                    adjustedSpaceAfter = neededUnits(splitLength
                            + foBlockSpaceBefore.min
                            + foBlockSpaceAfter.min)
                            * bpUnit - splitLength - adjustedSpaceBefore;
                } else if (bSpaceBefore) {
                    adjustedSpaceBefore = neededUnits(splitLength
                            + foBlockSpaceBefore.min)
                            * bpUnit - splitLength;
                } else {
                    adjustedSpaceAfter = neededUnits(splitLength
                            + foBlockSpaceAfter.min)
                            * bpUnit - splitLength;
                }
                //System.out.println("spazio prima = " + adjustedSpaceBefore
                      // + " spazio dopo = " + adjustedSpaceAfter + " totale = " +
                      // (adjustedSpaceBefore + adjustedSpaceAfter + splitLength));
                childPosIter = new KnuthPossPosIter(splitList, 0, splitList
                        .size());
                //}
            }
    
            // if adjusted space before
            if (bSpaceBefore) {
                addBlockSpacing(0, new MinOptMax(adjustedSpaceBefore));
            }
    
            while ((childLM = childPosIter.getNextChildLM()) != null) {
                // set last area flag
                lc.setFlags(LayoutContext.LAST_AREA,
                        (layoutContext.isLastArea() && childLM == lastLM));
                /*LF*/lc.setStackLimit(layoutContext.getStackLimit());
                // Add the line areas to Area
                childLM.addAreas(childPosIter, lc);
            }
        } else {
            // if adjusted space before
            if (bSpaceBefore) {
                addBlockSpacing(0, new MinOptMax(adjustedSpaceBefore));
            }
            //Add child areas inside the reference area
            bcpos.getBreaker().addContainedAreas();
        }

        //int bIndents = getBlockContainerFO().getCommonBorderPaddingBackground()
        //    .getBPPaddingAndBorder(false);

        if (markers != null) {
            getCurrentPV().addMarkers(markers, false, isFirst(firstPos), isLast(lastPos));
        }

        flush();

        // if adjusted space after
        if (bSpaceAfter) {
            addBlockSpacing(0, new MinOptMax(adjustedSpaceAfter));
        }

        viewportBlockArea = null;
        referenceArea = null;
    }
    
    /**
     * Get the parent area for children of this block container.
     * This returns the current block container area
     * and creates it if required.
     *
     * @see org.apache.fop.layoutmgr.LayoutManager#getParentArea(Area)
     */
    public Area getParentArea(Area childArea) {
        if (referenceArea == null) {
            viewportBlockArea = new BlockViewport();
            viewportBlockArea.addTrait(Trait.IS_VIEWPORT_AREA, Boolean.TRUE);
            viewportBlockArea.setIPD(vpContentIPD);
            if (autoHeight) {
                viewportBlockArea.setBPD(0);
            } else {
                viewportBlockArea.setBPD(vpContentBPD);
            }

            TraitSetter.setProducerID(viewportBlockArea, getBlockContainerFO().getId());
            TraitSetter.addBorders(viewportBlockArea, 
                    getBlockContainerFO().getCommonBorderPaddingBackground(), this);
            // TraitSetter.addBackground(viewportBlockArea, 
            //        getBlockContainerFO().getCommonBorderPaddingBackground(),
            //        this);
            TraitSetter.addMargins(viewportBlockArea, 
                    getBlockContainerFO().getCommonBorderPaddingBackground(),
                    getBlockContainerFO().getCommonMarginBlock(),
                    this);
            
            viewportBlockArea.setCTM(absoluteCTM);
            viewportBlockArea.setClip(clip);
            /*
            if (getSpaceBefore() != 0) {
                viewportBlockArea.addTrait(Trait.SPACE_BEFORE, new Integer(getSpaceBefore()));
            }
            if (foBlockSpaceAfter.opt != 0) {
                viewportBlockArea.addTrait(Trait.SPACE_AFTER, new Integer(foBlockSpaceAfter.opt));
            }*/

            if (abProps.absolutePosition == EN_ABSOLUTE 
                    || abProps.absolutePosition == EN_FIXED) {
                Point offset = getAbsOffset();
                viewportBlockArea.setXOffset(offset.x);
                viewportBlockArea.setYOffset(offset.y);
            } else {
                //nop
            }

            referenceArea = new Block();
            referenceArea.addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
            TraitSetter.setProducerID(referenceArea, getBlockContainerFO().getId());

            if (abProps.absolutePosition == EN_ABSOLUTE) {
                viewportBlockArea.setPositioning(Block.ABSOLUTE);
            } else if (abProps.absolutePosition == EN_FIXED) {
                viewportBlockArea.setPositioning(Block.FIXED);
            }

            // Set up dimensions
            // Must get dimensions from parent area
            /*Area parentArea =*/ parentLM.getParentArea(referenceArea);
            //int referenceIPD = parentArea.getIPD();
            referenceArea.setIPD(relDims.ipd);
            // Get reference IPD from parentArea
            setCurrentArea(viewportBlockArea); // ??? for generic operations
        }
        return referenceArea;
    }

    /**
     * Add the child to the block container.
     *
     * @see org.apache.fop.layoutmgr.LayoutManager#addChildArea(Area)
     */
    public void addChildArea(Area childArea) {
        if (referenceArea != null) {
            referenceArea.addBlock((Block) childArea);
        }
    }

    /**
     * @see org.apache.fop.layoutmgr.LayoutManager#resetPosition(org.apache.fop.layoutmgr.Position)
     */
    public void resetPosition(Position resetPos) {
        if (resetPos == null) {
            reset(null);
        }
    }

    /** 
     * Force current area to be added to parent area.
     * @see org.apache.fop.layoutmgr.BlockStackingLayoutManager#flush()
     */
    protected void flush() {
        viewportBlockArea.addBlock(referenceArea, autoHeight);

        TraitSetter.addBackground(viewportBlockArea, 
                getBlockContainerFO().getCommonBorderPaddingBackground(),
                this);
        
        // Fake a 0 height for absolute positioned blocks.
        int saveBPD = viewportBlockArea.getBPD();
        if (viewportBlockArea.getPositioning() == Block.ABSOLUTE) {
            viewportBlockArea.setBPD(0);
        }
        super.flush();
        // Restore the right height.
        if (viewportBlockArea.getPositioning() == Block.ABSOLUTE) {
            viewportBlockArea.setBPD(saveBPD);
        }
    }

    /**
     * @see org.apache.fop.layoutmgr.BlockLevelLayoutManager#negotiateBPDAdjustment(int, org.apache.fop.layoutmgr.KnuthElement)
     */
    public int negotiateBPDAdjustment(int adj, KnuthElement lastElement) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @see org.apache.fop.layoutmgr.BlockLevelLayoutManager#discardSpace(org.apache.fop.layoutmgr.KnuthGlue)
     */
    public void discardSpace(KnuthGlue spaceGlue) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see org.apache.fop.layoutmgr.BlockLevelLayoutManager#mustKeepTogether()
     */
    public boolean mustKeepTogether() {
        //TODO Keeps will have to be more sophisticated sooner or later
        return ((BlockLevelLayoutManager)getParent()).mustKeepTogether() 
                || !getBlockContainerFO().getKeepTogether().getWithinPage().isAuto()
                || !getBlockContainerFO().getKeepTogether().getWithinColumn().isAuto();
    }

    /**
     * @see org.apache.fop.layoutmgr.BlockLevelLayoutManager#mustKeepWithPrevious()
     */
    public boolean mustKeepWithPrevious() {
        return !getBlockContainerFO().getKeepWithPrevious().getWithinPage().isAuto()
                || !getBlockContainerFO().getKeepWithPrevious().getWithinColumn().isAuto();
    }

    /**
     * @see org.apache.fop.layoutmgr.BlockLevelLayoutManager#mustKeepWithNext()
     */
    public boolean mustKeepWithNext() {
        return !getBlockContainerFO().getKeepWithNext().getWithinPage().isAuto()
                || !getBlockContainerFO().getKeepWithNext().getWithinColumn().isAuto();
    }

    /**
     * @return the BlockContainer node
     */
    protected BlockContainer getBlockContainerFO() {
        return (BlockContainer) fobj;
    }    

    // --------- Property Resolution related functions --------- //
    
    /**
     * @see org.apache.fop.layoutmgr.LayoutManager#getGeneratesReferenceArea
     */
    public boolean getGeneratesReferenceArea() {
        return true;
    }
   
    /**
     * @see org.apache.fop.layoutmgr.LayoutManager#getGeneratesBlockArea
     */
    public boolean getGeneratesBlockArea() {
        return true;
    }
   
}

