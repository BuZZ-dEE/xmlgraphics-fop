/*
 * Copyright 1999-2004 The Apache Software Foundation.
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

import java.util.ArrayList;
import java.util.List;

import org.apache.fop.fo.Constants;

/**
 * This is a region reference area for a page regions.
 * This area is the direct child of a region-viewport-area. It is cloneable
 * so the page master can make copies from the original page and regions.
 */
public class RegionReference extends Area implements Cloneable {
    private int regionClass = Constants.FO_REGION_BEFORE;
    private CTM ctm;
    private int bpd;

    // the list of block areas from the static flow
    private List blocks = new ArrayList();
    
    // the parent RegionViewport for this object
    protected RegionViewport regionViewport;

    /**
     * Create a new region reference area.
     *
     * @param type the region class type
     */
    public RegionReference(int type, RegionViewport parent) {
        regionClass = type;
        addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
        regionViewport = parent;
    }

    /**
     * Set the Coordinate Transformation Matrix which transforms content
     * coordinates in this region reference area which are specified in
     * terms of "start" and "before" into coordinates in a system which
     * is positioned in "absolute" directions (with origin at lower left of
     * the region reference area.
     *
     * @param ctm the current transform to position this region
     */
    public void setCTM(CTM ctm) {
        this.ctm = ctm;
    }
    
    /**
     * @return Returns the parent RegionViewport.
     */
    public RegionViewport getRegionViewport() {
        return regionViewport;
    }

    /**
     * Get the current transform of this region.
     *
     * @return ctm the current transform to position this region
     */
    public CTM getCTM() {
        return this.ctm;
    }

    /**
     * Get the block in this region.
     *
     * @return the list of blocks in this region
     */
    public List getBlocks() {
        return blocks;
    }

    /**
     * Get the region class of this region.
     *
     * @return the region class
     */
    public int getRegionClass() {
        return regionClass;
    }

    /**
     * Add a block area to this region reference area.
     *
     * @param block the block area to add
     */
    public void addBlock(Block block) {
        blocks.add(block);
    }

    /**
     * Set the block-progression-dimension.
     *
     * @return the footnote area
     */
    public void setBPD(int bpd) {
        this.bpd = bpd;
    }

    /**
     * Set the block-progression-dimension.
     *
     * @return the footnote area
     */
    public int getBPD() {
        return bpd;
    }

    /**
     * Clone this region.
     * This is used when cloning the page by the page master.
     * The blocks are not copied since the master will have no blocks.
     *
     * @return a copy of this region reference area
     */
    public Object clone() {
        RegionReference rr = new RegionReference(regionClass, regionViewport);
        rr.ctm = ctm;
        rr.setIPD(getIPD());
        return rr;
    }

}
