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

package org.apache.fop.fo.pagination;

// Java
import java.awt.Rectangle;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;

/**
 * Abstract base class for fo:region-start and fo:region-end.
 */
public abstract class RegionSE extends RegionOuter {
    // The value of properties relevant for fo:region-[start|end].
    // End of property values

    /**
     * @see org.apache.fop.fo.FONode#FONode(FONode)
     */
    protected RegionSE(FONode parent) {
        super(parent);
    }

    /**
     * @see org.apache.fop.fo.FObj#bind(PropertyList)
     */
    public void bind(PropertyList pList) throws FOPException {
        super.bind(pList);
    }

    /**
     * Adjust the viewport reference rectangle for a region as a function
     * of precedence.
     * If  before and after have precedence = true, the start and end
     * regions only go to the limits of their extents, otherwise
     * they extend in the BPD to the page reference rectangle
     * diminish by extend of start and end if present.
     * @param vpRefRect viewport reference rectangle
     * @param wm writing mode
     */
    protected void adjustIPD(Rectangle vpRefRect, int wm) {
        int offset = 0;
        RegionBefore before = (RegionBefore) getSiblingRegion(FO_REGION_BEFORE);
        if (before != null && before.getPrecedence() == EN_TRUE) {
            offset = before.getExtent().getValue();
            vpRefRect.translate(0, offset);
        }
        RegionAfter after = (RegionAfter) getSiblingRegion(FO_REGION_AFTER);
        if (after != null && after.getPrecedence() == EN_TRUE) {
            offset += after.getExtent().getValue();
        }
        if (offset > 0) {
            if (wm == EN_LR_TB || wm == EN_RL_TB) {
                vpRefRect.height -= offset;
            } else {
                vpRefRect.width -= offset;
            }
        }
    }
}

