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

import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.datatypes.PropertyValueList;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.PropertyConsts;
import org.apache.fop.fo.ShorthandPropSets;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.messaging.MessageHandler;

public class Border extends Property  {
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


    public PropertyValue refineParsing
                        (int propindex, FONode foNode, PropertyValue value)
        throws PropertyException
    {
        int type = value.getType();
        if (type == PropertyValue.INHERIT ||
                type == PropertyValue.FROM_PARENT ||
                    type == PropertyValue.FROM_NEAREST_SPECIFIED)
            // Copy the value to each member of the shorthand expansion
            return refineExpansionList(PropNames.BORDER, foNode,
                                ShorthandPropSets.expandAndCopySHand(value));

        PropertyValueList ssList = null;
        // Must be a space-separated list or a single value from the
        // set of choices
        if (type != PropertyValue.LIST) {
            // If it's a single value, form a list from that value
            ssList = new PropertyValueList(PropNames.BORDER);
            ssList.add(value);
        } else {
            // Must be a space-separated list
            try {
                ssList = spaceSeparatedList((PropertyValueList)value);
            } catch (PropertyException e) {
                throw new PropertyException
                    ("Space-separated list required for 'border'");
            }
        }
        // Look for appropriate values in ssList
        PropertyValue width = null;
        PropertyValue style = null;
        PropertyValue color = null;
        Iterator values = ssList.iterator();
        while (values.hasNext()) {
            PropertyValue val = (PropertyValue)(values.next());
            PropertyValue pv = null;
            try {
                pv = PropertyConsts.pconsts.refineParsing
                        (PropNames.BORDER_WIDTH, foNode, val, IS_NESTED);
                if (width != null)
                    MessageHandler.log("border: duplicate" +
                    "width overrides previous width");
                width = pv;
                continue;
            } catch (PropertyException e) {}
            try {
                pv = PropertyConsts.pconsts.refineParsing
                            (PropNames.BORDER_STYLE, foNode, val, IS_NESTED);
                if (style != null)
                    MessageHandler.log("border: duplicate" +
                    "style overrides previous style");
                style = pv;
                continue;
            } catch (PropertyException e) {}
            try {
                pv = PropertyConsts.pconsts.refineParsing
                            (PropNames.BORDER_COLOR, foNode, val, IS_NESTED);
                if (color != null)
                    MessageHandler.log("border: duplicate" +
                    "color overrides previous color");
                color = pv;
                continue;
            } catch (PropertyException e) {}

            throw new PropertyException
                ("Unrecognized value; looking for style, "
                + "width or color in border: "
                + val.getClass().getName());
        }

        // Construct the shorthand expansion list
        // Only those elements which are actually specified fint their
        // way into this list.  Other elements will take their normally
        // inherited or initial values.
        PropertyValueList borderexp =
                                new PropertyValueList(PropNames.BORDER);
        if (style != null)
            borderexp.addAll((PropertyValueList)style);
        if (color != null)
            borderexp.addAll((PropertyValueList)color);
        if (width != null)
            borderexp.addAll((PropertyValueList)width);
        return borderexp;
    }
}

