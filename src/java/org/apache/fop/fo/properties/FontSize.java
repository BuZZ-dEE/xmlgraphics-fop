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

import java.util.HashMap;

import org.apache.fop.datatypes.Ems;
import org.apache.fop.datatypes.Ints;
import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.expr.PropertyException;

public class FontSize extends Property  {
    public static final int dataTypes =
                        PERCENTAGE | LENGTH | MAPPED_LENGTH | INHERIT;

    public int getDataTypes() {
        return dataTypes;
    }

    public static final int traitMapping = FORMATTING| RENDERING;

    public int getTraitMapping() {
        return traitMapping;
    }

    public static final int initialValueType = LENGTH_IT;

    public int getInitialValueType() {
        return initialValueType;
    }

    public static final int XX_SMALL = 1;
    public static final int X_SMALL = 2;
    public static final int SMALL = 3;
    public static final int MEDIUM = 4;
    public static final int LARGE = 5;
    public static final int X_LARGE = 6;
    public static final int XX_LARGE = 7;
    public static final int LARGER = 8;
    public static final int SMALLER = 9;

    // N.B. This foundational value MUST be an absolute length
    public PropertyValue getInitialValue(int property)
        throws PropertyException
    {
        return getMappedLength(null, MEDIUM);
    }

    public static final int inherited = COMPUTED;

    public int getInherited() {
        return inherited;
    }


    private static final String[] rwEnums = {
        null
        ,"xx-small"
        ,"x-small"
        ,"small"
        ,"medium"
        ,"large"
        ,"x-large"
        ,"xx-large"
        ,"larger"
        ,"smaller"
    };

    // N.B. this is a combination of points and ems
    private static final double[] mappedLengths = {
        0d
        ,7d         // xx-small
        ,8.3d       // x-small
        ,10d        // small
        ,12d        // medium
        ,14.4d      // large
        ,17.3d      // x-large
        ,20.7d      // xx-large
        ,1.2d       // larger
        ,0.83d      // smaller
    };

    public Numeric getMappedLength(FONode node, int enum)
        throws PropertyException
    {
        if (enum == LARGER || enum == SMALLER)
            return Ems.makeEms
                            (node, PropNames.FONT_SIZE, mappedLengths[enum]);
        return
            Length.makeLength
                    (PropNames.FONT_SIZE, mappedLengths[enum], Length.PT);
    }

    private static final HashMap rwEnumHash;
    static {
        rwEnumHash = new HashMap((int)(rwEnums.length / 0.75) + 1);
        for (int i = 1; i < rwEnums.length; i++ ) {
            rwEnumHash.put((Object)rwEnums[i],
                                (Object) Ints.consts.get(i));
        }
    }

}

