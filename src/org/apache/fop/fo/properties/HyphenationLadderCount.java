package org.apache.fop.fo.properties;

import org.apache.fop.datastructs.ROStringArray;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.datatypes.EnumType;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.properties.Property;

public class HyphenationLadderCount extends Property  {
    public static final int dataTypes = NUMBER | ENUM | INHERIT;
    public static final int traitMapping = FORMATTING;
    public static final int initialValueType = ENUM_IT;
    public static final int NO_LIMIT = 1;
    public /*static*/ PropertyValue getInitialValue(int property)
        throws PropertyException
    {
        return new EnumType (PropNames.HYPHENATION_LADDER_COUNT, NO_LIMIT);
    }
    public static final int inherited = COMPUTED;

    private static final String[] rwEnums = {
        null
        ,"no-limit"
    };
    public /*static*/ int getEnumIndex(String enum) throws PropertyException {
        return enumValueToIndex(enum, rwEnums);
    }
    public /*static*/ String getEnumText(int index) {
        return rwEnums[index];
    }
}

