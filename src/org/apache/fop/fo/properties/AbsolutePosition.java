package org.apache.fop.fo.properties;

import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.datastructs.ROStringArray;

public class AbsolutePosition extends Property  {
    public static final int dataTypes = AUTO | ENUM | INHERIT;
    public static final int traitMapping = NEW_TRAIT;
    public static final int initialValueType = AUTO_IT;
    public static final int ABSOLUTE = 1;
    public static final int FIXED = 2;

    public static final int inherited = NO;

    private static final String[] rwEnums = {
	null
	,"absolute"
	,"fixed"
    };
    public /**/static/**/ int getEnumIndex(String enum) throws PropertyException {
        return enumValueToIndex(enum, rwEnums);
    }
    public /**/static/**/ String getEnumText(int index) {
        return rwEnums[index];
    }
}

