/*
 *
 * Copyright 1999-2004 The Apache Software Foundation.
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
 * $Id$
 */

package org.apache.fop.fo.flow;

// FOP
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datastructs.TreeException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOPageSeqNode;
import org.apache.fop.fo.FOTree;
import org.apache.fop.fo.FObjectNames;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.PropertySets;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fonts.FontException;
import org.apache.fop.render.FontData;
import org.apache.fop.xml.XmlEvent;

/**
 * Implements #PcdATA within page-sequence flow objects.
 *
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 */
public class FoPcdata extends FOPageSeqNode {

    private static final String tag = "$Name$";
    private static final String revision = "$Revision$";

    /** Map of <tt>Integer</tt> indices of <i>sparsePropsSet</i> array.
        It is indexed by the FO index of the FO associated with a given
        position in the <i>sparsePropsSet</i> array.
     */
    private static final int[] sparsePropsMap;

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
        sparsePropsMap = new int[PropNames.LAST_PROPERTY_INDEX + 1];
        Arrays.fill(sparsePropsMap, -1);
        numProps = propsets.cardinality();
        sparseIndices = new int[numProps];
        int propx = 0;
        for (int next = propsets.nextSetBit(0);
                next >= 0;
                next = propsets.nextSetBit(next + 1)) {
            sparseIndices[propx] = next;
            sparsePropsMap[next] = propx++;
        }
    }

    /** The #PCDATA characters. */
    private String characters;
    private FontData fontData;

    /**
     * Construct an FoPcdata object to contain the characters from a
     * character data node.  There is no corresponding Flow Obect in the
     * specification.
     * @param foTree the FO tree being built
     * @param pageSequence ancestor of this node
     * @param parent the parent FONode of this node
     * @param event the <tt>XmlEvent</tt> that triggered the creation of
     * this node
     * @param stateFlags - passed down from the parent.  Includes the
     * attribute set information.
     */
    public FoPcdata
            (FOTree foTree, FONode pageSequence, FOPageSeqNode parent,
                    XmlEvent event, int stateFlags)
        throws TreeException, FOPException
    {
        super(foTree, FObjectNames.PCDATA, pageSequence, parent, event,
                          stateFlags, sparsePropsMap, sparseIndices);
        characters = event.getChars();

        makeSparsePropsSet();
    }

    /**
     * Get the <tt>String</tt> data of the node.
     * @return the string of characters.
     */
    public String getCharacters() {
        return characters;
    }

    // PCDATA provides sequences of inline-areas to fill line-areas in the
    // parent block area.
    // Generate a text-layout for the PCDATA.
    /**
     * Generates a TextMeasurer from the PCDATA text.  The font and text
     * attributes of the text are applied.
     */
    private void processText() throws PropertyException, FontException {
        // Get the font, size, style and weight attributes
        Map attributes = getFontAttributes();
        Font font = getFopFont(attributes);
        // Add the text decorations
        // TODO separate color support for text decorations
        if (decorations.underlined()) {
            attributes.put(TextAttribute.UNDERLINE,
                    TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
        }
        if (decorations.overlined()) {
            // Not supported yet
        }
        if (decorations.struckthrough()) {
            attributes.put(TextAttribute.STRIKETHROUGH,
                    TextAttribute.STRIKETHROUGH_ON);
        }
        AttributedString attText =
            new AttributedString(characters, attributes);
    }

}
