/*
 * $Id$
 *
 * ============================================================================
 *                   The Apache Software License, Version 1.1
 * ============================================================================
 * 
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of  source code must  retain the above copyright  notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include  the following  acknowledgment:  "This product includes  software
 *    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
 *    Alternately, this  acknowledgment may  appear in the software itself,  if
 *    and wherever such third-party acknowledgments normally appear.
 * 
 * 4. The names "FOP" and  "Apache Software Foundation"  must not be used to
 *    endorse  or promote  products derived  from this  software without  prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 * 
 * 5. Products  derived from this software may not  be called "Apache", nor may
 *    "Apache" appear  in their name,  without prior written permission  of the
 *    Apache Software Foundation.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 * APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 * ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 * (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * This software  consists of voluntary contributions made  by many individuals
 * on  behalf of the Apache Software  Foundation and was  originally created by
 * James Tauber <jtauber@jtauber.com>. For more  information on the Apache 
 * Software Foundation, please see <http://www.apache.org/>.
 *  
 */
package org.apache.fop.fo.properties;

import java.util.Iterator;

import org.apache.fop.datatypes.ColorType;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.datatypes.PropertyValueList;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.ShorthandPropSets;
import org.apache.fop.fo.expr.PropertyException;

public class BorderColor extends ColorTransparent {
    public static final int dataTypes = SHORTHAND;

    public int getDataTypes() {
        return dataTypes;
    }

    public static final int traitMapping = SHORTHAND_MAP;

    public int getTraitMapping() {
        return traitMapping;
    }

    public static final int initialValueType = NOTYPE_IT;

    public int getInitialValueType() {
        return initialValueType;
    }

    public static final int inherited = NO;

    public int getInherited() {
        return inherited;
    }



    /**
     * 'value' is a PropertyValueList or an individual PropertyValue.
     *
     * <p>If 'value' is an individual PropertyValue, it must contain
     * either
     *   a ColorType value
     *   a NCName containing a standard color name or 'transparent'
     *   a FromParent value,
     *   a FromNearestSpecified value,
     *   or an Inherit value.
     *
     * <p>If 'value' is a PropertyValueList, it contains a list of
     * 2 to 4 ColorType values or NCName enum tokens representing colors.
     *
     * <p>The value(s) provided, if valid, are converted into a list
     * containing the expansion of the shorthand.
     * The first element is a value for border-top-color,
     * the second element is a value for border-right-color,
     * the third element is a value for border-bottom-color,
     * the fourth element is a value for border-left-color.
     *
     * @param propindex - the <tt>int</tt> property index.
     * @param foNode - the <tt>FONode</tt> being built
     * @param value <tt>PropertyValue</tt> returned by the parser
     * @return <tt>PropertyValue</tt> the verified value
     */
    public PropertyValue refineParsing
                        (int propindex, FONode foNode, PropertyValue value)
                throws PropertyException
    {
        return refineParsing(propindex, foNode, value, NOT_NESTED);
    }

    /**
     * Do the work for the three argument refineParsing method.
     * @param propindex - the <tt>int</tt> property index.
     * @param foNode - the <tt>FONode</tt> being built
     * @param value <tt>PropertyValue</tt> returned by the parser
     * @param nested <tt>boolean</tt> indicating whether this method is
     * called normally (false), or as part of another <i>refineParsing</i>
     * method.
     * @return <tt>PropertyValue</tt> the verified value
     * @see #refineParsing(FONode,PropertyValue)
     */
    public PropertyValue refineParsing
        (int propindex, FONode foNode, PropertyValue value, boolean nested)
                throws PropertyException
    {
        int type = value.getType();
        if ( ! (value instanceof PropertyValueList)) {
            if ( ! nested) {
                if (type == PropertyValue.INHERIT ||
                        type == PropertyValue.FROM_PARENT ||
                            type == PropertyValue.FROM_NEAREST_SPECIFIED)
                    return refineExpansionList(PropNames.BORDER_COLOR, foNode,
                            ShorthandPropSets.expandAndCopySHand(value));
            }
            // Form a list and pass to processList
	    PropertyValueList tmpList = new PropertyValueList(propindex);
	    tmpList.add(value);
	    return processList(tmpList);
        } else {
            if (nested) throw new PropertyException
                    ("PropertyValueList invalid for nested border-color "
                        + "refineParsing() method");
            return processList(spaceSeparatedList((PropertyValueList)value));
        }
    }

    private PropertyValueList processList(PropertyValueList list)
        throws PropertyException
    {
        // List may contain only multiple color specifiers
        // i.e. ColorTypes or NCNames specifying a standard color or
        // 'transparent'.
        ColorType top, left, bottom, right;
        int count = list.size();
        if (count < 1 || count > 4)
            throw new PropertyException
                ("border-color list contains " + count + " items");

        Iterator colors = list.iterator();

        // There must be at least one
        top = getColor(PropNames.BORDER_TOP_COLOR,
                                           (PropertyValue)(colors.next()));

        try {
            if (colors.hasNext())
                right = getColor
                        (PropNames.BORDER_RIGHT_COLOR, 
                                             (PropertyValue)(colors.next()));
            else
                right = (ColorType)(top.clone());

            bottom = (ColorType)(top.clone());
            left = (ColorType)(right.clone());
        } catch (CloneNotSupportedException cnse) {
            throw new PropertyException
                            ("clone() not supported on ColorType");
        }

        if (colors.hasNext())
                    bottom = getColor
                            (PropNames.BORDER_BOTTOM_COLOR,
                                             (PropertyValue)(colors.next()));
        if (colors.hasNext())
                    left = getColor
                            (PropNames.BORDER_LEFT_COLOR, 
                                             (PropertyValue)(colors.next()));

        // Set the properties for each
        right.setProperty(PropNames.BORDER_RIGHT_COLOR);
        bottom.setProperty(PropNames.BORDER_BOTTOM_COLOR);
        left.setProperty(PropNames.BORDER_LEFT_COLOR);

        list = new PropertyValueList(PropNames.BORDER_COLOR);
        list.add(top);
        list.add(right);
        list.add(bottom);
        list.add(left);
        // Question: if less than four colors have been specified in
        // the shorthand, what border-?-color properties, if any,
        // have been specified?
        return list;
    }

}

