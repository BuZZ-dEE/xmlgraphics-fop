package org.apache.fop.fo.properties;

import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.properties.Property;

public class LeaderLength extends Property  {
    public static final int dataTypes =
                                COMPOUND | LENGTH | PERCENTAGE | INHERIT;
    public static final int traitMapping = FORMATTING;
    public static final int initialValueType = NOTYPE_IT;
    public static final int inherited = NO;
}

