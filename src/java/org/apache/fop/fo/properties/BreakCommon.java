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

public class BreakCommon extends Property  {
    public static final int COLUMN = 1;
    public static final int PAGE = 2;
    public static final int EVEN_PAGE = 3;
    public static final int ODD_PAGE = 4;

    private static final String[] rwEnums = {
        null
        ,"column"
        ,"page"
        ,"even-page"
        ,"odd-page"
    };

    private static final HashMap rwEnumHash;
    static {
        rwEnumHash = new HashMap((int)(rwEnums.length / 0.75) + 1);
        for (int i = 1; i < rwEnums.length; i++ ) {
            rwEnumHash.put(rwEnums[i],
                                Ints.consts.get(i));
        }
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

