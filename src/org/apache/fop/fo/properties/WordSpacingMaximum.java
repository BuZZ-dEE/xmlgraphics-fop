package org.apache.fop.fo.properties;

import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.PropNames;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.properties.WordSpacingCommon;

public class WordSpacingMaximum extends WordSpacingCommon  {
    public static final int dataTypes = LENGTH;
    public static final int traitMapping = DISAPPEARS;
    public static final int initialValueType = LENGTH_IT;
    public /*static*/ PropertyValue getInitialValue(int property)
        throws PropertyException
    {
        return getMappedLength(NORMAL);
    }
    public static final int inherited = COMPUTED;

    public /*static*/ Numeric getMappedLength(int enum)
        throws PropertyException
    {
        if (enum != NORMAL)
            throw new PropertyException("Invalid MAPPED_LENGTH enum: "
                                        + enum);
        return Length.makeLength
                            (PropNames.WORD_SPACING_MAXIMUM, 0d, Length.PT);
    }
}

