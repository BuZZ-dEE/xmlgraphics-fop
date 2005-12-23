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

package org.apache.fop.layoutmgr.inline;

import java.util.ListIterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.fop.area.Area;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.InlineBlockParent;
import org.apache.fop.area.inline.InlineParent;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.flow.Inline;
import org.apache.fop.fo.flow.InlineLevel;
import org.apache.fop.fo.flow.Leader;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.CommonMarginInline;
import org.apache.fop.fo.properties.SpaceProperty;
import org.apache.fop.fonts.Font;
import org.apache.fop.layoutmgr.KnuthBox;
import org.apache.fop.layoutmgr.KnuthElement;
import org.apache.fop.layoutmgr.KnuthSequence;
import org.apache.fop.layoutmgr.KnuthPenalty;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.NonLeafPosition;
import org.apache.fop.layoutmgr.SpaceSpecifier;
import org.apache.fop.layoutmgr.TraitSetter;
import org.apache.fop.layoutmgr.LayoutManager;
import org.apache.fop.layoutmgr.Position;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.inline.InlineStackingLayoutManager.StackingIter;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.traits.SpaceVal;

/**
 * LayoutManager for objects which stack children in the inline direction,
 * such as Inline or Line
 */
public class InlineLayoutManager extends InlineStackingLayoutManager {
    private InlineLevel fobj;

    private CommonMarginInline inlineProps = null;
    private CommonBorderPaddingBackground borderProps = null;

    private boolean areaCreated = false;
    private LayoutManager lastChildLM = null; // Set when return last breakposs;

    private Position auxiliaryPosition;

    private Font font;

    /** The alignment adjust property */
    protected Length alignmentAdjust;
    /** The alignment baseline property */
    protected int alignmentBaseline = EN_BASELINE;
    /** The baseline shift property */
    protected Length baselineShift;
    /** The dominant baseline property */
    protected int dominantBaseline;
    /** The line height property */
    protected SpaceProperty lineHeight;
    
    private AlignmentContext alignmentContext = null;

    /**
     * Create an inline layout manager.
     * This is used for fo's that create areas that
     * contain inline areas.
     *
     * @param node the formatting object that creates the area
     */
    // The node should be FObjMixed
    public InlineLayoutManager(InlineLevel node) {
        super(node);
        fobj = node;
    }
    
    private Inline getInlineFO() {
        return (Inline)fobj;
    }
    
    /** @see LayoutManager#initialize */
    public void initialize() {
        int padding = 0;
        font = fobj.getCommonFont().getFontState(fobj.getFOEventHandler().getFontInfo(), this);
        lineHeight = fobj.getLineHeight();

        if (fobj instanceof Inline) {
            inlineProps = fobj.getCommonMarginInline();
            borderProps = fobj.getCommonBorderPaddingBackground();
            alignmentAdjust = ((Inline)fobj).getAlignmentAdjust();
            alignmentBaseline = ((Inline)fobj).getAlignmentBaseline();
            baselineShift = ((Inline)fobj).getBaselineShift();
            dominantBaseline = ((Inline)fobj).getDominantBaseline();
        } else if (fobj instanceof Leader) {
            alignmentAdjust = ((Leader)fobj).getAlignmentAdjust();
            alignmentBaseline = ((Leader)fobj).getAlignmentBaseline();
            baselineShift = ((Leader)fobj).getBaselineShift();
            dominantBaseline = ((Leader)fobj).getDominantBaseline();
        }
        if (borderProps != null) {
            padding = borderProps.getPadding(CommonBorderPaddingBackground.BEFORE, false, this);
            padding += borderProps.getBorderWidth(CommonBorderPaddingBackground.BEFORE,
                                                 false);
            padding += borderProps.getPadding(CommonBorderPaddingBackground.AFTER, false, this);
            padding += borderProps.getBorderWidth(CommonBorderPaddingBackground.AFTER, false);
        }
        extraBPD = new MinOptMax(padding);

    }

    /** @see InlineStackingLayoutManager#getExtraIPD(boolean, boolean) */
    protected MinOptMax getExtraIPD(boolean isNotFirst, boolean isNotLast) {
        int borderAndPadding = 0;
        if (borderProps != null) {
            borderAndPadding 
                = borderProps.getPadding(CommonBorderPaddingBackground.START, isNotFirst, this);
            borderAndPadding 
                += borderProps.getBorderWidth(CommonBorderPaddingBackground.START, isNotFirst);
            borderAndPadding 
                += borderProps.getPadding(CommonBorderPaddingBackground.END, isNotLast, this);
            borderAndPadding 
                += borderProps.getBorderWidth(CommonBorderPaddingBackground.END, isNotLast);
        }
        return new MinOptMax(borderAndPadding);
    }


    /** @see InlineStackingLayoutManager#hasLeadingFence(boolean) */
    protected boolean hasLeadingFence(boolean isNotFirst) {
        return borderProps != null
            && (borderProps.getPadding(CommonBorderPaddingBackground.START, isNotFirst, this) > 0
                || borderProps.getBorderWidth(CommonBorderPaddingBackground.START, isNotFirst) > 0
               );
    }

    /** @see InlineStackingLayoutManager#hasTrailingFence(boolean) */
    protected boolean hasTrailingFence(boolean isNotLast) {
        return borderProps != null
            && (borderProps.getPadding(CommonBorderPaddingBackground.END, isNotLast, this) > 0
                || borderProps.getBorderWidth(CommonBorderPaddingBackground.END, isNotLast) > 0
               );
    }

    /** @see InlineStackingLayoutManager#getSpaceStart */
    protected SpaceProperty getSpaceStart() {
        return inlineProps != null ? inlineProps.spaceStart : null;
    }
    /** @see InlineStackingLayoutManager#getSpaceEnd */
    protected SpaceProperty getSpaceEnd() {
        return inlineProps != null ? inlineProps.spaceEnd : null;
    }
    
    /** @see org.apache.fop.layoutmgr.inline.InlineLayoutManager#createArea(boolean) */
    protected InlineArea createArea(boolean hasInlineParent) {
        InlineArea area;
        if (hasInlineParent) {
            area = new InlineParent();
            area.setOffset(0);
        } else {
            area = new InlineBlockParent();
        }
        if (fobj instanceof Inline) {
            TraitSetter.setProducerID(area, getInlineFO().getId());
        }
        return area;
    }
    
    /**
     * @see org.apache.fop.layoutmgr.inline.InlineStackingLayoutManager#setTraits(boolean, boolean)
     */
    protected void setTraits(boolean isNotFirst, boolean isNotLast) {
        if (borderProps != null) {
            // Add border and padding to current area and set flags (FIRST, LAST ...)
            TraitSetter.setBorderPaddingTraits(getCurrentArea(),
                                               borderProps, isNotFirst, isNotLast, this);
            TraitSetter.addBackground(getCurrentArea(), borderProps, this);
        }
    }

    /** @see org.apache.fop.layoutmgr.LayoutManager */
    public LinkedList getNextKnuthElements(LayoutContext context, int alignment) {
        InlineLevelLayoutManager curILM;
        LayoutManager curLM, lastLM = null;

        // the list returned by child LM
        LinkedList returnedList;
        KnuthElement returnedElement;

        // the list which will be returned to the parent LM
        LinkedList returnList = new LinkedList();
        KnuthSequence lastSequence = null;

        SpaceSpecifier leadingSpace = context.getLeadingSpace();
        
        alignmentContext = new AlignmentContext(font
                                    , lineHeight.getOptimum(this).getLength().getValue(this)
                                    , alignmentAdjust
                                    , alignmentBaseline
                                    , baselineShift
                                    , dominantBaseline
                                    , context.getAlignmentContext());

        childLC = new LayoutContext(context);
        childLC.setAlignmentContext(alignmentContext);

        if (context.startsNewArea()) {
            // First call to this LM in new parent "area", but this may
            // not be the first area created by this inline
            if (getSpaceStart() != null) {
                context.getLeadingSpace().addSpace(new SpaceVal(getSpaceStart(), this));
            }

            // Check for "fence"
            if (hasLeadingFence(!context.isFirstArea())) {
                // Reset leading space sequence for child areas
                leadingSpace = new SpaceSpecifier(false);
            }
            // Reset state variables
            clearPrevIPD(); // Clear stored prev content dimensions
        }

        StringBuffer trace = new StringBuffer("InlineLM:");

        // We'll add the border to the first inline sequence created.
        // This flag makes sure we do it only once.
        boolean borderAdded = false;

        if (borderProps != null) {
            childLC.setLineStartBorderAndPaddingWidth(context.getLineStartBorderAndPaddingWidth()
                + borderProps.getPaddingStart(true, this)
                + borderProps.getBorderStartWidth(true)
             );
            childLC.setLineEndBorderAndPaddingWidth(context.getLineEndBorderAndPaddingWidth()
                + borderProps.getPaddingEnd(true, this)
                + borderProps.getBorderEndWidth(true)
             );
        }
        
        while ((curLM = (LayoutManager) getChildLM()) != null) {
            if (!(curLM instanceof InlineLevelLayoutManager)) {
                // A block LM
                // Leave room for start/end border and padding
                if (borderProps != null) {
                    childLC.setRefIPD(childLC.getRefIPD()
                            - borderProps.getPaddingStart(lastChildLM != null, this)
                            - borderProps.getBorderStartWidth(lastChildLM != null)
                            - borderProps.getPaddingEnd(hasNextChildLM(), this)
                            - borderProps.getBorderEndWidth(hasNextChildLM()));
                }
            }
            // get KnuthElements from curLM
            returnedList = curLM.getNextKnuthElements(childLC, alignment);
            if (returnedList == null) {
                // curLM returned null because it finished;
                // just iterate once more to see if there is another child
                continue;
            }
            if (curLM instanceof InlineLevelLayoutManager) {
                // close the last block sequence 
                if (lastSequence != null && !lastSequence.isInlineSequence()) {
                    lastSequence = null;
                    if (log.isTraceEnabled()) {
                        trace.append(" ]");
                    }
                }
                // "wrap" the Position stored in each element of returnedList
                ListIterator seqIter = returnedList.listIterator();
                while (seqIter.hasNext()) {
                    KnuthSequence sequence = (KnuthSequence) seqIter.next();
                    ListIterator listIter = sequence.listIterator();
                    while (listIter.hasNext()) {
                        returnedElement = (KnuthElement) listIter.next();
                        returnedElement.setPosition
                        (notifyPos(new NonLeafPosition(this,
                                returnedElement.getPosition())));
                    }
                    if (!sequence.isInlineSequence()) {
                        if (lastSequence != null && lastSequence.isInlineSequence()) {
                            // log.error("Last inline sequence should be closed before"
                            //                + " a block sequence");
                            lastSequence.add(new KnuthPenalty(0, -KnuthElement.INFINITE,
                                                   false, null, false));
                            lastSequence = null;
                            if (log.isTraceEnabled()) {
                                trace.append(" ]");
                            }
                        }
                        returnList.add(sequence);
                        if (log.isTraceEnabled()) {
                            trace.append(" B");
                        }
                    } else {
                        if (lastSequence == null) {
                            lastSequence = new KnuthSequence(true);
                            returnList.add(lastSequence);
                            if (!borderAdded) {
                                addKnuthElementsForBorderPaddingStart(lastSequence);
                                borderAdded = true;
                            }
                            if (log.isTraceEnabled()) {
                                trace.append(" [");
                            }
                        } else {
                            if (log.isTraceEnabled()) {
                                trace.append(" +");
                            }
                        }
                        lastSequence.addAll(sequence);
                        if (log.isTraceEnabled()) {
                            trace.append(" I");
                        }
                       // finish last paragraph if it was closed with a linefeed
                        KnuthElement lastElement = (KnuthElement) sequence.getLast();
                        if (lastElement.isPenalty()
                                && ((KnuthPenalty) lastElement).getP()
                                == -KnuthPenalty.INFINITE) {
                            // a penalty item whose value is -inf
                            // represents a preserved linefeed,
                            // wich forces a line break
                            lastSequence = null;
                            if (log.isTraceEnabled()) {
                                trace.append(" ]");
                            }
                        }
                    }
                }
            } else { // A block LM
                // close the last inline sequence 
                if (lastSequence != null && lastSequence.isInlineSequence()) {
                    lastSequence.add(new KnuthPenalty(0, -KnuthElement.INFINITE,
                                           false, null, false));
                    lastSequence = null;
                    if (log.isTraceEnabled()) {
                        trace.append(" ]");
                    }
                }
                if (curLM != lastLM) {
                    // close the last block sequence
                    if (lastSequence != null && !lastSequence.isInlineSequence()) {
                        lastSequence = null;
                        if (log.isTraceEnabled()) {
                            trace.append(" ]");
                        }
                    }
                    lastLM = curLM;
                }
                if (lastSequence == null) {
                    lastSequence = new KnuthSequence(false);
                    returnList.add(lastSequence);
                    if (log.isTraceEnabled()) {
                        trace.append(" [");
                    }
                    if (!borderAdded) {
                        addKnuthElementsForBorderPaddingStart(lastSequence);
                        borderAdded = true;
                    }
                } else {
                    if (log.isTraceEnabled()) {
                        trace.append(" +");
                    }
                }
                ListIterator iter = returnedList.listIterator();
                while (iter.hasNext()) {
                    KnuthElement element = (KnuthElement) iter.next();
                    element.setPosition
                        (notifyPos(new NonLeafPosition(this,
                                element.getPosition())));
                }
                lastSequence.addAll(returnedList);
                if (log.isTraceEnabled()) {
                    trace.append(" L");
                }
            }
            lastChildLM = curLM;
        }

        if (lastSequence != null) {
            addKnuthElementsForBorderPaddingEnd(lastSequence);
        }

        setFinished(true);
        log.trace(trace);
        return returnList.size() == 0 ? null : returnList;
    }

    /**
     * Generate and add areas to parent area.
     * Set size of each area. This should only create and return one
     * inline area for any inline parent area.
     *
     * @param parentIter Iterator over Position information returned
     * by this LayoutManager.
     * @param context layout context.
     */
    public void addAreas(PositionIterator parentIter,
                         LayoutContext context) {
        
        Position lastPos = null;
        
        addId();

        setChildContext(new LayoutContext(context)); // Store current value

        // If this LM has fence, make a new leading space specifier.
        if (hasLeadingFence(areaCreated)) {
            getContext().setLeadingSpace(new SpaceSpecifier(false));
            getContext().setFlags(LayoutContext.RESOLVE_LEADING_SPACE, true);
        } else {
            getContext().setFlags(LayoutContext.RESOLVE_LEADING_SPACE, false);
        }

        if (getSpaceStart() != null) {
            context.getLeadingSpace().addSpace(new SpaceVal(getSpaceStart(), this));
        }

        // "Unwrap" the NonLeafPositions stored in parentIter and put
        // them in a new list.  Set lastLM to be the LayoutManager
        // which created the last Position: if the LAST_AREA flag is
        // set in the layout context, it must be also set in the
        // layout context given to lastLM, but must be cleared in the
        // layout context given to the other LMs.
        LinkedList positionList = new LinkedList();
        NonLeafPosition pos = null;
        LayoutManager lastLM = null; // last child LM in this iterator
        while (parentIter.hasNext()) {
            pos = (NonLeafPosition) parentIter.next();
            if (pos != null && pos.getPosition() != null) {
                positionList.add(pos.getPosition());
                lastLM = pos.getPosition().getLM();
                lastPos = pos;
            }
        }
        /*if (pos != null) {
            lastLM = pos.getPosition().getLM();
        }*/

        InlineArea parent = createArea(lastLM == null 
                                        || lastLM instanceof InlineLevelLayoutManager);
        parent.setBPD(alignmentContext.getHeight());
        if (parent instanceof InlineParent) {
            parent.setOffset(alignmentContext.getOffset());
        } else if (parent instanceof InlineBlockParent) {
            // All inline elements are positioned by the renderers relative to
            // the before edge of their content rectangle
            if (borderProps != null) {
                parent.setOffset(borderProps.getPaddingBefore(false, this)
                                + borderProps.getBorderBeforeWidth(false));
            }
        }
        setCurrentArea(parent);
        
        StackingIter childPosIter
            = new StackingIter(positionList.listIterator());

        LayoutManager prevLM = null;
        LayoutManager childLM;
        while ((childLM = childPosIter.getNextChildLM()) != null) {
            getContext().setFlags(LayoutContext.LAST_AREA,
                                  context.isLastArea() && childLM == lastLM);
            childLM.addAreas(childPosIter, getContext());
            getContext().setLeadingSpace(getContext().getTrailingSpace());
            getContext().setFlags(LayoutContext.RESOLVE_LEADING_SPACE, true);
            prevLM = childLM;
        }

        /* If this LM has a trailing fence, resolve trailing space
         * specs from descendants.  Otherwise, propagate any trailing
         * space specs to the parent LM via the layout context.  If
         * the last child LM called returns LAST_AREA in the layout
         * context and it is the last child LM for this LM, then this
         * must be the last area for the current LM too.
         */
        boolean isLast = (getContext().isLastArea() && prevLM == lastChildLM);
        if (hasTrailingFence(isLast)) {
            addSpace(getCurrentArea(),
                     getContext().getTrailingSpace().resolve(false),
                     getContext().getSpaceAdjust());
            context.setTrailingSpace(new SpaceSpecifier(false));
        } else {
            // Propagate trailing space-spec sequence to parent LM in context.
            context.setTrailingSpace(getContext().getTrailingSpace());
        }
        // Add own trailing space to parent context (or set on area?)
        if (context.getTrailingSpace() != null  && getSpaceEnd() != null) {
            context.getTrailingSpace().addSpace(new SpaceVal(getSpaceEnd(), this));
        }
        
        setTraits(areaCreated, !isLast(lastPos));
        parentLM.addChildArea(getCurrentArea());

        context.setFlags(LayoutContext.LAST_AREA, isLast);
        areaCreated = true;
    }

    /** @see LayoutManager#addChildArea(Area) */
    public void addChildArea(Area childArea) {
        Area parent = getCurrentArea();
        if (getContext().resolveLeadingSpace()) {
            addSpace(parent,
                    getContext().getLeadingSpace().resolve(false),
                    getContext().getSpaceAdjust());
        }
        parent.addChildArea(childArea);
    }

    /** @see LayoutManager#getChangedKnuthElements(List, int) */
    public LinkedList getChangedKnuthElements(List oldList, int alignment) {
        LinkedList returnedList = new LinkedList();
        addKnuthElementsForBorderPaddingStart(returnedList);
        returnedList.addAll(super.getChangedKnuthElements(oldList, alignment));
        addKnuthElementsForBorderPaddingEnd(returnedList);
        return returnedList;
    }
    
    /**
     * Creates Knuth elements for start border padding and adds them to the return list.
     * @param returnList return list to add the additional elements to
     */
    protected void addKnuthElementsForBorderPaddingStart(List returnList) {
        //Border and Padding (start)
        CommonBorderPaddingBackground borderAndPadding = fobj.getCommonBorderPaddingBackground();
        if (borderAndPadding != null) {
            int ipStart = borderAndPadding.getBorderStartWidth(false)
                         + borderAndPadding.getPaddingStart(false, this);
            if (ipStart > 0) {
                returnList.add(new KnuthBox(ipStart, getAuxiliaryPosition(), true));
            }
        }
    }

    /**
     * Creates Knuth elements for end border padding and adds them to the return list.
     * @param returnList return list to add the additional elements to
     */
    protected void addKnuthElementsForBorderPaddingEnd(List returnList) {
        //Border and Padding (after)
        CommonBorderPaddingBackground borderAndPadding = fobj.getCommonBorderPaddingBackground();
        if (borderAndPadding != null) {
            int ipEnd = borderAndPadding.getBorderEndWidth(false)
                        + borderAndPadding.getPaddingEnd(false, this);
            if (ipEnd > 0) {
                returnList.add(new KnuthBox(ipEnd, getAuxiliaryPosition(), true));
            }
        }
    }

    /** @return a cached auxiliary Position instance used for things like spaces. */
    protected Position getAuxiliaryPosition() {
        //if (this.auxiliaryPosition == null) {
            //this.auxiliaryPosition = new NonLeafPosition(this, new LeafPosition(this, -1));
            this.auxiliaryPosition = new NonLeafPosition(this, null);
        //}
        return this.auxiliaryPosition;
    }
    
    /** @see org.apache.fop.layoutmgr.inline.LeafNodeLayoutManager#addId() */
    protected void addId() {
        if (fobj instanceof Inline) {
            getPSLM().addIDToPage(getInlineFO().getId());
        }
    }
    
}

