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
 
package org.apache.fop.area;

import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.fo.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * The line area.
 * This is a line area that contains inline areas.
 */
public class LineArea extends Area {

    /**
     * this class stores information about line width and potential adjustments
     * that can be used in order to re-compute adjustement and / or indents when a
     * page-number or a page-number-citation is resolved
     */
    private class LineAdjustingInfo {
        private int lineAlignment;
        private int difference;
        private int availableStretch;
        private int availableShrink;
        private double variationFactor;
        private boolean bAddedToAreaTree;
        
        private LineAdjustingInfo(int alignment, int diff,
                                  int stretch, int shrink) {
            lineAlignment = alignment;
            difference = diff;
            availableStretch = stretch;
            availableShrink = shrink;
            variationFactor = 1.0;
            bAddedToAreaTree = false;
        }
    }
    
    private LineAdjustingInfo adjustingInfo = null;

    private int stacking = LR;
    // contains inline areas
    // has start indent and length, dominant baseline, height
    private int startIndent;

    // this is the offset for the dominant baseline
    private int baseLine;

    // this class can contain the dominant char styling info
    // this means that many renderers can optimise a bit

    private List inlineAreas = new ArrayList();

    /**
     * default constructor:
     * nothing to do
     */
    public LineArea() {        
    }

    /**
     * constructor with extra parameters:
     * a new LineAdjustingInfo object is created
     * @param alignment alignment of this line
     * @param diff      difference between content width and line width
     */
    public LineArea(int alignment, int diff,
                    int stretch, int shrink) {
        adjustingInfo = new LineAdjustingInfo(alignment, diff, stretch, shrink);
    }

    /**
     * Add a child area to this line area.
     *
     * @param childArea the inline child area to add
     */
    public void addChildArea(Area childArea) {
        if (childArea instanceof InlineArea) {
            addInlineArea((InlineArea)childArea);
            // set the parent area for the child area
            ((InlineArea) childArea).setParentArea(this);
        }
    }

    /**
     * Add an inline child area to this line area.
     *
     * @param area the inline child area to add
     */
    public void addInlineArea(InlineArea area) {
        inlineAreas.add(area);
    }

    /**
     * Get the inline child areas of this line area.
     *
     * @return the list of inline areas
     */
    public List getInlineAreas() {
        return inlineAreas;
    }

    /**
     * Set the start indent of this line area.
     * The start indent is used for offsetting the start of
     * the inline areas for alignment or other indents.
     *
     * @param si the start indent value
     */
    public void setStartIndent(int si) {
        startIndent = si;
    }

    /**
     * Get the start indent of this line area.
     * The start indent is used for offsetting the start of
     * the inline areas for alignment or other indents.
     *
     * @return the start indent value
     */
    public int getStartIndent() {
        return startIndent;
    }

    /**
     * Updates the extents of the line area from its children.
     */
    public void updateExtentsFromChildren() {
        int ipd = 0;
        int bpd = 0;
        for (int i = 0, len = inlineAreas.size(); i < len; i++) {
            ipd = Math.max(ipd, ((InlineArea)inlineAreas.get(i)).getAllocIPD());
            bpd += ((InlineArea)inlineAreas.get(i)).getAllocBPD();
        }
        setIPD(ipd);
        setBPD(bpd);
    }
    
    /**
     * receive notification about the ipd variation of a descendant area
     * and perform the needed adjustment, according to the alignment;
     * in particular:
     * <ul>
     *   <li>left-aligned text needs no adjustement;</li>
     *   <li>right-aligned text and centered text are handled locally,
     *       adjusting the indent of this LineArea;</li>
     *   <li>justified text requires a more complex adjustment, as the 
     *       variation factor computed on the basis of the total
     *       stretch and shrink of the line must be applied in every
     *       descendant leaf areas (text areas and leader areas).</li> 
     * </ul>
     * @param ipdVariation the difference between old and new ipd 
     */
    public void handleIPDVariation(int ipdVariation) {
        switch (adjustingInfo.lineAlignment) {
            case Constants.EN_START:
                // nothing to do in this case
                break;
            case Constants.EN_CENTER:
                // re-compute indent
                startIndent -= ipdVariation / 2;
                break;
            case Constants.EN_END:
                // re-compute indent
                startIndent -= ipdVariation;
                break;
            case Constants.EN_JUSTIFY:
                // compute variation factor
                adjustingInfo.variationFactor *= (float) (adjustingInfo.difference - ipdVariation) / adjustingInfo.difference;
                adjustingInfo.difference -= ipdVariation;
                // if the LineArea has already been added to the area tree,
                // call finalize(); otherwise, wait for the LineLM to call it
                if (adjustingInfo.bAddedToAreaTree) {
                    finalize();
                }
                break;
            default:
                throw new RuntimeException();
        }
    }
    
    /**
     * apply the variation factor to all descendant areas
     * and destroy the AdjustingInfo object if there are
     * no UnresolvedAreas left
     */
    public void finalize() {
        if (adjustingInfo.lineAlignment == Constants.EN_JUSTIFY) {
            // justified line: apply the variation factor
            boolean bUnresolvedAreasPresent = false;
            // recursively apply variation factor to descendant areas
            for (int i = 0, len = inlineAreas.size(); i < len; i++) {
                bUnresolvedAreasPresent |= ((InlineArea) inlineAreas.get(i))
                        .applyVariationFactor(adjustingInfo.variationFactor,
                                adjustingInfo.availableStretch,
                                adjustingInfo.availableShrink);
            }
            if (!bUnresolvedAreasPresent) {
                // there are no more UnresolvedAreas:
                // destroy the AdjustingInfo instance
                adjustingInfo = null;
            } else {
                // this method will be called again later:
                // the first time, it is called by the LineLM,
                // afterwards it must be called by the LineArea itself
                if (!adjustingInfo.bAddedToAreaTree) {
                    adjustingInfo.bAddedToAreaTree = true;
                }
                // reset the variation factor
                adjustingInfo.variationFactor = 1.0;
            }
        } else {
            // the line is not justified: the ipd variation has already
            // been handled, modifying the line indent
        }
    }
}

