/*
 *
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created on 30/01/2004
 * $Id$
 */
package org.apache.fop.area;

import java.awt.geom.Rectangle2D;

import org.apache.fop.datastructs.Node;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.flow.FoPageSequence;

/**
 * @author pbw
 * @version $Revision$ $Name$
 */
public class BlockArea extends Area {

    /**
     * @param parent of this node
     */
    public BlockArea(
            FoPageSequence pageSeq,
            FONode generatedBy,
            Node parent,
            Object sync) {
        super(pageSeq, generatedBy, parent, sync);
        // TODO Auto-generated constructor stub
    }

    /** The page space allocation for layout of the block */
    private Rectangle2D pageSpace = new Rectangle2D.Float();

//    /**
//     * Receives an allocation of page space from area parent
//     * @param pageSpace
//     */
//    public void receivePageSpace(Rectangle2D pageSpace) {
//        this.pageSpace = pageSpace;
//    }

    /**
     * An allocation of page space has been requested by the currently active
     * child area.  <i>N.B.</i> <code>reference-area</code>s must override
     * this method to apply an <code>AffineTransform</code> to areas passed
     * up and returned.
     * @return
     */
    public Rectangle2D pageSpaceRequest(AreaRange spaceRange) {
        AreaRange request = adjustedRequest(spaceRange);
        // Is there a sufficient allocation already available?
        if (spaceContains(spaceRange.minima)) {
            if (spaceContains(spaceRange.maxima)) {
                // Reduce available space and OK the request
                // N.B. the space request must take into account the
                // space required for footnotes
                // If all of the text fits into the available space, then the
                // siblings of the line area must be tested to discover whether
                // they contain inline items which will go into the same
                // line-area.  At this point, e.g., footnotes will be found.
            } else {
                // Negotiate available space with the requester

            }
        } else { // Need more space from above.  Page may be full
            // Negotiate with parent for more space
        }
        return null;
    }

    private boolean spaceContains(Rectangle2D rect) {
        return pageSpace.contains(
                rect.getMinX(), rect.getMinY(), rect.getWidth(), rect.getHeight());
    }
    private AreaRange adjustedRequest(AreaRange request) {
        // TODO Adjust the request for padding, borders and margins on this
        // block
        // For now, do nothing.
        return request;
    }
    /**
     * Accepts a laid-out block from an area child
     * @param layout
     */
    public void acceptLayout(Rectangle2D layout) {
        
    }

    protected float getStartSpace() {
        return getStartIndent() + getStartIntrusion();
    }

    protected float getEndSpace() {
        return getEndIndent() + getEndIntrusion();
    }

    protected float getStartIndent() {
        // Dummy start-indent
        return 10.0f;
    }

    protected float getEndIndent() {
        // Dummy end-indent
        return 10.0f;
    }

    protected float getStartIntrusion() {
        // dummy intrusion
        return 0.0f;
    }

    protected float getEndIntrusion() {
        // dummy intrusion
        return 0.0f;
    }

}
