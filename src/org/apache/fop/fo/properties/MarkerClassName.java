package org.apache.fop.fo.properties;

import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.datatypes.NCName;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.properties.Property;

public class MarkerClassName extends Property  {
    public static final int dataTypes = NCNAME;
    public static final int traitMapping = FORMATTING;
    public static final int initialValueType = NCNAME_IT;
    public PropertyValue getInitialValue(int property)
        throws PropertyException
    {
        return new NCName(PropNames.MARKER_CLASS_NAME, "");
    }
    public static final int inherited = NO;
}

