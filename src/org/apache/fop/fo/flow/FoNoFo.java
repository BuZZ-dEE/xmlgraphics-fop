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

import java.util.Arrays;
import java.util.HashMap;
import java.util.BitSet;

/**
 * Implements the fo:no-fo flow object.
 */
public class FoNoFo {

    private static final String tag = "$Name$";
    private static final String revision = "$Revision$";

    /** Map of <tt>Integer</tt> indices of <i>sparsePropsSet</i> array.
        It is indexed by the FO index of the FO associated with a given
        position in the <i>sparsePropsSet</i> array. See
        {@link org.apache.fop.fo.FONode#sparsePropsSet FONode.sparsePropsSet}.
     */
    //private static final int[] sparsePropsMap;

    /** An <tt>int</tt> array of of the applicable property indices, in
        property index order. */
    //private static final int[] sparseIndices;

    /** The number of applicable properties.  This is the size of the
        <i>sparsePropsSet</i> array. */
    //private static final int numProps;

    /*
    static {
        // Collect the sets of properties that apply
        BitSet propsets = new BitSet();

        // Map these properties into sparsePropsSet
        // sparsePropsSet is a HashMap containing the indicies of the
        // sparsePropsSet array, indexed by the FO index of the FO slot
        // in sparsePropsSet.
        sparsePropsMap = new HashMap(1);
        Arrays.fill(sparsePropsMap, -1);
        numProps = 1;
        sparseIndices = new int[] { PropNames.NO_PROPERTY };
        sparsePropsMap[PropNames.NO_PROPERTY] = 0;
    }
    */

    /**
     * The non-existent flow object.  If it comes to this, something
     * has gone wrong.  A use may be found for this later.
     * @param foTree the FO tree being built
     * @param parent the parent FONode of this node
     * @param event the <tt>FoXMLEvent</tt> that triggered the creation of
     * this node
     * @param stateFlags - passed down from the parent.  Includes the
     * attribute set information.
     * @throws FOPException, without exception.
     */
    public FoNoFo
            (FOTree foTree, FONode parent, FoXMLEvent event, int stateFlags)
        throws FOPException
    {
        throw new FOPException("No such flow object as fo:no-fo.");
    }

}
