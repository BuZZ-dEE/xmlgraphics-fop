package org.apache.fop.fo.properties;

import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.datatypes.Bool;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.properties.Property;

public class ScoreSpaces extends Property  {
    public static final int dataTypes = BOOL | INHERIT;
    public static final int traitMapping = FORMATTING;
    public static final int initialValueType = BOOL_IT;
    public /*static*/ PropertyValue getInitialValue(int property)
        throws PropertyException
    {
        return new Bool(PropNames.SCORE_SPACES, true);
    }
    public static final int inherited = COMPUTED;
}

