/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 *
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 */

package org.apache.fop.fo.flow;

// FOP
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.PropertySets;
import org.apache.fop.fo.FObjectNames;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOTree;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.xml.FoXMLEvent;
import org.apache.fop.apps.FOPException;
import org.apache.fop.datastructs.TreeException;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.datatypes.Ints;

import java.util.HashMap;
import java.util.BitSet;

/**
 * Implements the fo:character flow object.
 */
public class FoCharacter extends FONode {

    private static final String tag = "$Name$";
    private static final String revision = "$Revision$";

    /** Map of <tt>Integer</tt> indices of <i>sparsePropsSet</i> array.
        It is indexed by the FO index of the FO associated with a given
        position in the <i>sparsePropsSet</i> array. See
        {@link org.apache.fop.fo.FONode#sparsePropsSet FONode.sparsePropsSet}.
     */
    private static final HashMap sparsePropsMap;

    /** An <tt>int</tt> array of of the applicable property indices, in
        property index order. */
    private static final int[] sparseIndices;

    /** The number of applicable properties.  This is the size of the
        <i>sparsePropsSet</i> array. */
    private static final int numProps;

    static {
        // Collect the sets of properties that apply
        BitSet propsets = new BitSet();
        propsets.or(PropertySets.auralSet);
        propsets.or(PropertySets.backgroundSet);
        propsets.or(PropertySets.borderSet);
        propsets.or(PropertySets.fontSet);
        propsets.or(PropertySets.hyphenationSet);
        propsets.or(PropertySets.marginInlineSet);
        propsets.or(PropertySets.paddingSet);
        propsets.or(PropertySets.relativePositionSet);
        propsets.set(PropNames.ALIGNMENT_ADJUST);
        propsets.set(PropNames.TREAT_AS_WORD_SPACE);
        propsets.set(PropNames.ALIGNMENT_BASELINE);
        propsets.set(PropNames.BASELINE_SHIFT);
        propsets.set(PropNames.CHARACTER);
        propsets.set(PropNames.COLOR);
        propsets.set(PropNames.DOMINANT_BASELINE);
        propsets.set(PropNames.TEXT_DEPTH);
        propsets.set(PropNames.TEXT_ALTITUDE);
        propsets.set(PropNames.GLYPH_ORIENTATION_HORIZONTAL);
        propsets.set(PropNames.GLYPH_ORIENTATION_VERTICAL);
        propsets.set(PropNames.ID);
        propsets.set(PropNames.KEEP_WITH_NEXT);
        propsets.set(PropNames.KEEP_WITH_PREVIOUS);
        propsets.set(PropNames.LETTER_SPACING);
        propsets.set(PropNames.LINE_HEIGHT);
        propsets.set(PropNames.SCORE_SPACES);
        propsets.set(PropNames.SUPPRESS_AT_LINE_BREAK);
        propsets.set(PropNames.TEXT_DECORATION);
        propsets.set(PropNames.TEXT_SHADOW);
        propsets.set(PropNames.TEXT_TRANSFORM);
        propsets.set(PropNames.VISIBILITY);
        propsets.set(PropNames.WORD_SPACING);

        // Map these properties into sparsePropsSet
        // sparsePropsSet is a HashMap containing the indicies of the
        // sparsePropsSet array, indexed by the FO index of the FO slot
        // in sparsePropsSet.
        sparsePropsMap = new HashMap();
        numProps = propsets.cardinality();
        sparseIndices = new int[numProps];
        int propx = 0;
        for (int next = propsets.nextSetBit(0);
                next >= 0;
                next = propsets.nextSetBit(next + 1)) {
            sparseIndices[propx] = next;
            sparsePropsMap.put
                        (Ints.consts.get(next), Ints.consts.get(propx++));
        }
    }

    /**
     * Construct an fo:character node.  This node has no children.
     * @param foTree the FO tree being built
     * @param parent the parent FONode of this node
     * @param event the <tt>FoXMLEvent</tt> that triggered the creation of
     * this node
     * @param stateFlags - passed down from the parent.  Includes the
     * attribute set information.
     */
    public FoCharacter
            (FOTree foTree, FONode parent, FoXMLEvent event, int stateFlags)
        throws TreeException, FOPException
    {
        super(foTree, FObjectNames.CHARACTER, parent, event,
                          stateFlags, sparsePropsMap, sparseIndices);
        makeSparsePropsSet();
    }

}
