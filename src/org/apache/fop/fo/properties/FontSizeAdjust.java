package org.apache.fop.fo.properties;

import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.properties.Property;

public class FontSizeAdjust extends Property  {
    public static final int dataTypes = NUMBER | NONE | INHERIT;
    public static final int traitMapping = FONT_SELECTION;
    public static final int initialValueType = NONE_IT;
    public static final int inherited = COMPUTED;
}

