/*
 * $Id$
 *
 *
 * Copyright 1999-2003 The Apache Software Foundation.
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
 *  
 */
package org.apache.fop.fo.properties;

import java.util.HashMap;

import org.apache.fop.datatypes.Ints;
import org.apache.fop.fo.expr.PropertyException;

/**
 * Pseudo-property class for common color values, and the special value
 * "transparent", occurring in a number of classes.
 */
public class ColorTransparent extends ColorCommon  {

    /**
     * include "transparent"
     */
    private static final HashMap rwEnumHash;
    static {
	rwEnumHash = new HashMap((int)((rwEnums.length + 1) / 0.75) + 1);
	for (int i = 1; i < rwEnums.length; i++ ) {
	    rwEnumHash.put(rwEnums[i], Ints.consts.get(i));
	}
        rwEnumHash.put("grey", Ints.consts.get(ColorCommon.GRAY));
    }

    public int getEnumIndex(String enum)
        throws PropertyException
    {
        Integer ii = (Integer)(rwEnumHash.get(enum));
        if (ii == null)
            throw new PropertyException("Unknown enum value: " + enum);
        return ii.intValue();
    }

    public String getEnumText(int index)
        throws PropertyException
    {
        if (index < 1 || index >= rwEnums.length)
            throw new PropertyException("index out of range: " + index);
        return rwEnums[index];
    }

}

