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

import org.apache.fop.apps.FOPException;

import org.apache.fop.area.AreaTreeHandler;
import org.apache.fop.area.AreaTreeModel;
import org.apache.fop.area.Area;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.Resolvable;
import org.apache.fop.area.Trait;

import org.apache.fop.datatypes.PercentBase;

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.flow.Marker;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.Region;
import org.apache.fop.fo.pagination.SideRegion;
import org.apache.fop.fo.pagination.SimplePageMaster;
import org.apache.fop.fo.pagination.StaticContent;

import java.util.LinkedList;
import java.util.List;

/**
 * LayoutManager for a PageSequence.  This class is instantiated by
 * area.AreaTreeHandler for each fo:page-sequence found in the
 * input document.
 */
public class PageSequenceLayoutManager extends AbstractLayoutManager {

    /** 
     * AreaTreeHandler which activates the PSLM and controls
     * the rendering of its pages.
     */
    private AreaTreeHandler areaTreeHandler;

    /** 
     * fo:page-sequence formatting object being
     * processed by this class
     */
    private PageSequence pageSeq;

    /** 
     * Current page-viewport-area being filled by
     * the PSLM.
     */
    private PageViewport curPV = null;

    /**
     * Zero-based index of column (Normal Flow) in span (of the PV) 
     * being filled.  See XSL Rec description of fo:region-body 
     * and fop.Area package classes for more information. 
     */
    private int curFlowIdx = -1;

    /*
    private static class BlockBreakPosition extends LeafPosition {
        protected BreakPoss breakps;

        protected BlockBreakPosition(LayoutManager lm, BreakPoss bp) {
            super(lm, 0);
            breakps = bp;
        }
    }*/

    private int startPageNum = 0;
    private int currentPageNum = 0;

    /**
     * The single FlowLayoutManager object, which processes
     * the single fo:flow of the fo:page-sequence
     */
    private FlowLayoutManager childFLM = null;
    
    /**
     * The collection of StaticContentLayoutManager objects that
     * are associated with this Page Sequence, keyed by flow-name.
     */
    //private HashMap staticContentLMs = new HashMap(4);

    /**
     * Constructor
     *
     * @param ath the area tree handler object
     * @param pseq fo:page-sequence to process
     */
    public PageSequenceLayoutManager(AreaTreeHandler ath, PageSequence pseq) {
        super(pseq);
        this.areaTreeHandler = ath;
        this.pageSeq = pseq;
    }

    /**
     * @see org.apache.fop.layoutmgr.LayoutManager
     * @return the LayoutManagerMaker object
     */
    public LayoutManagerMaker getLayoutManagerMaker() {
        return areaTreeHandler.getLayoutManagerMaker();
    }

    /**
     * Start the layout of this page sequence.
     * This completes the layout of the page sequence
     * which creates and adds all the pages to the area tree.
     */
    public void activateLayout() {
        startPageNum = pageSeq.getStartingPageNumber();
        currentPageNum = startPageNum - 1;

        LineArea title = null;

        if (pageSeq.getTitleFO() != null) {
            ContentLayoutManager clm = new ContentLayoutManager(pageSeq
                    .getTitleFO(), this);
            title = (LineArea) clm.getParentArea(null); // can improve
        }

        areaTreeHandler.getAreaTreeModel().startPageSequence(title);
        log.debug("Starting layout");

        curPV = makeNewPage(false, true, false);

        PageBreaker breaker = new PageBreaker(this);
        int flowBPD = (int) curPV.getBodyRegion().getBPD();
        breaker.doLayout(flowBPD);
        
        finishPage();
        pageSeq.getRoot().notifyPageSequenceFinished(currentPageNum,
                (currentPageNum - startPageNum) + 1);
        log.debug("Ending layout");
    }

    private class PageBreaker extends AbstractBreaker {
        
        private PageSequenceLayoutManager pslm;
        private boolean firstPart = true;
        
        public PageBreaker(PageSequenceLayoutManager pslm) {
            this.pslm = pslm;
        }
        
        protected LayoutContext createLayoutContext() {
            LayoutContext lc = new LayoutContext(0);
            int flowIPD = curPV.getCurrentSpan().getColumnWidth();
            lc.setRefIPD(flowIPD);
            return lc;
        }
        
        protected LayoutManager getTopLevelLM() {
            return pslm;
        }
        
        protected LinkedList getNextKnuthElements(LayoutContext context, int alignment) {
            return pslm.getNextKnuthElements(context, alignment);
        }
        
        protected int getCurrentDisplayAlign() {
            return curPV.getSPM().getRegion(Constants.FO_REGION_BODY).getDisplayAlign();
        }
        
        protected boolean hasMoreContent() {
            return !isFinished();
        }
        
        protected void addAreas(PositionIterator posIter, LayoutContext context) {
            getCurrentChildLM().addAreas(posIter, context);    
        }
        
        protected void doPhase3(PageBreakingAlgorithm alg, int partCount, 
                BlockSequence originalList, BlockSequence effectiveList) {
            //Directly add areas after finding the breaks
            addAreas(alg, partCount, originalList, effectiveList);
        }
        
        protected void startPart(BlockSequence list, boolean bIsFirstPage) {
            if (curPV == null) {
                throw new IllegalStateException("curPV must not be null");
            } else {
                //firstPart is necessary because we need the first page before we start the 
                //algorithm so we have a BPD and IPD. This may subject to change later when we
                //start handling more complex cases.
                if (!firstPart) {
                    if (curFlowIdx < curPV.getCurrentSpan().getColumnCount()-1) {
                        curFlowIdx++;
                    } else  {
                        // if this is the first page that will be created by
                        // the current BlockSequence, it could have a break
                        // condition that must be satisfied;
                        // otherwise, we simply need a new page
                        handleBreak(bIsFirstPage ? list.getStartOn() : Constants.EN_PAGE);
                    }
                }
            }
            // add static areas and resolve any new id areas
            // finish page and add to area tree
            firstPart = false;
        }
        
        protected void finishPart() {
        }
        
        protected LayoutManager getCurrentChildLM() {
            return childFLM;
        }
        
    }
    
    /** @see org.apache.fop.layoutmgr.LayoutManager#isBogus() */
    public boolean isBogus() {
        return false;
    }
    
    /**
     * Get the next break possibility.
     * This finds the next break for a page which is always at the end
     * of the page.
     *
     * @param context the layout context for finding breaks
     * @return the break for the page
     */
    public LinkedList getNextKnuthElements(LayoutContext context, int alignment) {

        LayoutManager curLM; // currently active LM

        while ((curLM = getChildLM()) != null) {
/*LF*/      LinkedList returnedList = null;
/*LF*/      if (childFLM == null && (curLM instanceof FlowLayoutManager)) {
/*LF*/          childFLM = (FlowLayoutManager)curLM;
/*LF*/      } else {
/*LF*/          if (curLM != childFLM) {
/*LF*/              System.out.println("PLM> figlio sconosciuto (invalid child LM)");
/*LF*/          }
/*LF*/      }

            LayoutContext childLC = new LayoutContext(0);
            childLC.setStackLimit(context.getStackLimit());
            childLC.setRefIPD(context.getRefIPD());

            if (!curLM.isFinished()) {
                int flowIPD = curPV.getCurrentSpan().getColumnWidth();
                int flowBPD = (int) curPV.getBodyRegion().getBPD();
                pageSeq.setLayoutDimension(PercentBase.REFERENCE_AREA_IPD, flowIPD);
                pageSeq.setLayoutDimension(PercentBase.REFERENCE_AREA_BPD, flowBPD);
/*LF*/          returnedList = curLM.getNextKnuthElements(childLC, alignment);
            }
            if (returnedList != null) {
                return returnedList;
            }
        }
        setFinished(true);
        return null;
    }

    /**
     * Provides access to the current page.
     * @return the current PageViewport
     */
    public PageViewport getCurrentPV() {
        return curPV;
    }

    /**
     * Resolve a reference ID.
     * This resolves a reference ID and returns the first PageViewport
     * that contains the reference ID or null if reference not found.
     *
     * @param id the reference ID to lookup
     * @return the first page viewport that contains the reference
     */
    public PageViewport resolveRefID(String id) {
        List list = areaTreeHandler.getPageViewportsContainingID(id);
        if (list != null && list.size() > 0) {
            return (PageViewport) list.get(0);
        }
        return null;
    }

    /**
     * Add an ID reference to the current page.
     * When adding areas the area adds its ID reference.
     * For the page layout manager it adds the id reference
     * with the current page to the area tree.
     *
     * @param id the ID reference to add
     */
    public void addIDToPage(String id) {
        areaTreeHandler.associateIDWithPageViewport(id, curPV);
    }

    /**
     * Add an unresolved area to the layout manager.
     * The Page layout manager handles the unresolved ID
     * reference by adding to the current page and then adding
     * the page as a resolvable to the area tree.
     * This is so that the area tree can resolve the reference
     * and the page can serialize the resolvers if required.
     *
     * @param id the ID reference to add
     * @param res the resolvable object that needs resolving
     */
    public void addUnresolvedArea(String id, Resolvable res) {
        // add to the page viewport so it can serialize
        curPV.addUnresolvedIDRef(id, res);
        // add unresolved to tree
        areaTreeHandler.addUnresolvedIDRef(id, curPV);
    }

    /**
     * Retrieve a marker from this layout manager.
     * If the boundary is page then it will only check the
     * current page. For page-sequence and document it will
     * lookup preceding pages from the area tree and try to find
     * a marker.
     * If we retrieve a marker from a preceding page,
     * then the containing page does not have a qualifying area,
     * and all qualifying areas have ended.
     * Therefore we use last-ending-within-page (Constants.EN_LEWP)
     * as the position. 
     *
     * @param name the marker class name to lookup
     * @param pos the position to locate the marker
     * @param boundary the boundary for locating the marker
     * @return the layout manager for the marker contents
     */
    public Marker retrieveMarker(String name, int pos, int boundary) {
        AreaTreeModel areaTreeModel = areaTreeHandler.getAreaTreeModel();
        
        // get marker from the current markers on area tree
        Marker mark = (Marker)curPV.getMarker(name, pos);
        if (mark == null && boundary != EN_PAGE) {
            // go back over pages until mark found
            // if document boundary then keep going
            boolean doc = boundary == EN_DOCUMENT;
            int seq = areaTreeModel.getPageSequenceCount();
            int page = areaTreeModel.getPageCount(seq) - 1;
            while (page < 0 && doc && seq > 1) {
                seq--;
                page = areaTreeModel.getPageCount(seq) - 1;
            }
            while (page >= 0) {
                PageViewport pv = areaTreeModel.getPage(seq, page);
                mark = (Marker)pv.getMarker(name, Constants.EN_LEWP);
                if (mark != null) {
                    return mark;
                }
                page--;
                if (page < 0 && doc && seq > 1) {
                    seq--;
                    page = areaTreeModel.getPageCount(seq) - 1;
                }
            }
        }

        if (mark == null) {
            log.debug("found no marker with name: " + name);
        }

        return mark;
    }

    private PageViewport makeNewPage(boolean bIsBlank, boolean bIsFirst, boolean bIsLast) {
        if (curPV != null) {
            finishPage();
        }

        currentPageNum++;
        String pageNumberString = pageSeq.makeFormattedPageNumber(currentPageNum);

        try {
            // create a new page
            SimplePageMaster spm = pageSeq.getSimplePageMasterToUse(
                currentPageNum, bIsFirst, bIsBlank);
            
            Region body = spm.getRegion(FO_REGION_BODY);
            if (!pageSeq.getMainFlow().getFlowName().equals(body.getRegionName())) {
              // this is fine by the XSL Rec (fo:flow's flow-name can be mapped to
              // any region), but we don't support it yet.
              throw new FOPException("Flow '" + pageSeq.getMainFlow().getFlowName()
                 + "' does not map to the region-body in page-master '"
                 + spm.getMasterName() + "'.  FOP presently "
                 + "does not support this.");
            }
            curPV = new PageViewport(spm);
        } catch (FOPException fopex) {
            throw new IllegalArgumentException("Cannot create page: " + fopex.getMessage());
        }

        curPV.setPageNumberString(pageNumberString);
        if (log.isDebugEnabled()) {
            log.debug("[" + curPV.getPageNumberString() + (bIsBlank ? "*" : "") + "]");
        }

        curPV.createSpan(false);
        curFlowIdx = 0;
        return curPV;
    }

    /* TODO: See if can initialize the SCLM's just once for
     * the page sequence, instead of after every page.
     */
    private void layoutSideRegion(int regionID) {
        SideRegion reg = (SideRegion)curPV.getSPM().getRegion(regionID);
        if (reg == null) {
            return;
        }
        StaticContent sc = pageSeq.getStaticContent(reg.getRegionName());
        if (sc == null) {
            return;
        }

        RegionViewport rv = curPV.getPage().getRegionViewport(regionID);
        StaticContentLayoutManager lm;
        lm = (StaticContentLayoutManager)
            areaTreeHandler.getLayoutManagerMaker().makeLayoutManager(sc);
        lm.initialize();
        lm.setTargetRegion(rv.getRegionReference());
        lm.setParent(this);
        /*
        LayoutContext childLC = new LayoutContext(0);
        childLC.setStackLimit(new MinOptMax((int)curPV.getViewArea().getHeight()));
        childLC.setRefIPD(rv.getRegion().getIPD());
        */
        
        lm.doLayout(reg);
        
        /*
        while (!lm.isFinished()) {
            BreakPoss bp = lm.getNextBreakPoss(childLC);
            if (bp != null) {
                List vecBreakPoss = new java.util.ArrayList();
                vecBreakPoss.add(bp);
                lm.addAreas(new BreakPossPosIter(vecBreakPoss, 0,
                                                 vecBreakPoss.size()), null);
            } else {
                log.error("bp==null  cls=" + reg.getRegionName());
            }
        }*/
        lm.reset(null);
    }

    private void finishPage() {
        // Layout side regions
        layoutSideRegion(FO_REGION_BEFORE); 
        layoutSideRegion(FO_REGION_AFTER);
        layoutSideRegion(FO_REGION_START);
        layoutSideRegion(FO_REGION_END);
        // Queue for ID resolution and rendering
        areaTreeHandler.getAreaTreeModel().addPage(curPV);
        log.debug("page finished: " + curPV.getPageNumberString() + ", current num: " + currentPageNum);
        curPV = null;
        curFlowIdx = -1;
    }

    private void prepareNormalFlowArea(Area childArea) {
        // Need span, break
        int breakVal = Constants.EN_AUTO;
        Integer breakBefore = (Integer)childArea.getTrait(Trait.BREAK_BEFORE);
        if (breakBefore != null) {
            breakVal = breakBefore.intValue();
        }
        if (breakVal != Constants.EN_AUTO) {
            // We may be forced to make new page
            handleBreak(breakVal);
        }
        /* Determine if a new span is needed.  From the XSL
         * fo:region-body definition, if an fo:block has a span="ALL"
         * (i.e., span all columns defined for the region-body), it
         * must be placed in a span-reference-area whose 
         * column-count = 1.  If its span-value is "NONE", 
         * place in a normal Span whose column-count is what
         * is defined for the region-body. 
         */  // temporarily hardcoded to EN_NONE.
        boolean bNeedNewSpan = false;
        int span = Constants.EN_NONE; // childArea.getSpan()
        int numColsNeeded;
        if (span == Constants.EN_ALL) {
            numColsNeeded = 1;
        } else { // EN_NONE
            numColsNeeded = curPV.getBodyRegion().getColumnCount();
        }
        if (numColsNeeded != curPV.getCurrentSpan().getColumnCount()) {
            // need a new Span, with numColsNeeded columns
            if (curPV.getCurrentSpan().getColumnCount() > 1) {
                // finished with current span, so balance 
                // its columns to make them the same "height"
                // balanceColumns();  // TODO: implement
            }
            bNeedNewSpan = true;
        }
        if (bNeedNewSpan) {
            curPV.createSpan(span == Constants.EN_ALL);
            curFlowIdx = 0;
        }
    }
    
    /**
     * This is called from FlowLayoutManager when it needs to start
     * a new flow container (while generating areas).
     *
     * @param childArea The area for which a container is needed. It must be
     * some kind of block-level area. It must have area-class, break-before
     * and span properties set.
     * @return the parent area
     */
    public Area getParentArea(Area childArea) {
        int aclass = childArea.getAreaClass();

        if (aclass == Area.CLASS_NORMAL) {
            //We now do this in PageBreaker
            //prepareNormalFlowArea(childArea);
            return curPV.getCurrentSpan().getNormalFlow(curFlowIdx);
        } else if (aclass == Area.CLASS_BEFORE_FLOAT) {
            return curPV.getBodyRegion().getBeforeFloat();
        } else if (aclass == Area.CLASS_FOOTNOTE) {
            return curPV.getBodyRegion().getFootnote();
        }
        // todo!!! other area classes (side-float, absolute, fixed)
        return null;
    }

    /**
     * Depending on the kind of break condition, make new column
     * or page. May need to make an empty page if next page would
     * not have the desired "handedness".
     *
     * @param breakVal the break value to handle
     */
    private void handleBreak(int breakVal) {
        if (breakVal == Constants.EN_COLUMN) {
            if (curFlowIdx < curPV.getCurrentSpan().getColumnCount()) {
                // Move to next column
                curFlowIdx++;
                return;
            }
            // else need new page
            breakVal = Constants.EN_PAGE;
        }
        log.debug("handling break after page " + currentPageNum + " breakVal=" + breakVal);
        if (needEmptyPage(breakVal)) {
            curPV = makeNewPage(true, false, false);
        }
        if (needNewPage(breakVal)) {
            curPV = makeNewPage(false, false, false);
        }
    }

    /**
     * If we have already started to layout content on a page,
     * and there is a forced break, see if we need to generate
     * an empty page.
     * Note that if not all content is placed, we aren't sure whether
     * it will flow onto another page or not, so we'd probably better
     * block until the queue of layoutable stuff is empty!
     */
    private boolean needEmptyPage(int breakValue) {

        if (breakValue == Constants.EN_PAGE || ((curPV != null) && curPV.getPage().isEmpty())) {
            // any page is OK or we already have an empty page
            return false;
        } else {
            /* IF we are on the kind of page we need, we'll need a new page. */
            if (currentPageNum % 2 != 0) {
                // Current page is odd
                return (breakValue == Constants.EN_ODD_PAGE);
            } else {
                return (breakValue == Constants.EN_EVEN_PAGE);
            }
        }
    }

    /**
     * See if need to generate a new page for a forced break condition.
     */
    private boolean needNewPage(int breakValue) {
        if (curPV != null && curPV.getPage().isEmpty()) {
            if (breakValue == Constants.EN_PAGE) {
                return false;
            }
            else if (currentPageNum % 2 != 0) {
                // Current page is odd
                return (breakValue == Constants.EN_EVEN_PAGE);
            }
            else {
                return (breakValue == Constants.EN_ODD_PAGE);
            }
        } else {
            return true;
        }
    }
}
