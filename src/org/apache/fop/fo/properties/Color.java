package org.apache.fop.fo.properties;

import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.datatypes.ColorType;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.properties.ColorNonTransparent;

public class Color extends ColorNonTransparent {
    public static final int dataTypes = COLOR_T | INHERIT;
    public static final int traitMapping = RENDERING;
    public static final int initialValueType = COLOR_IT;
    public static final int inherited = COMPUTED;
    public PropertyValue getInitialValue(int property)
        throws PropertyException
    {
        return new ColorType(PropNames.BACKGROUND_COLOR, BLACK);
    }

}

