package org.apache.fop.fo.properties;

import org.apache.fop.datatypes.PropertyValueList;
import org.apache.fop.datatypes.EnumType;
import org.apache.fop.datatypes.ColorType;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.properties.Property;

public class BorderLeft extends Property  {
    public static final int dataTypes = SHORTHAND;
    public static final int traitMapping = SHORTHAND_MAP;
    public static final int initialValueType = NOTYPE_IT;
    public static final int inherited = NO;

    /**
     * 'value' is a PropertyValueList or an individual PropertyValue.
     *
     * <p>The value(s) provided, if valid, are converted into a list
     * containing the expansion of the shorthand.  The elements may
     * be in any order.  A minimum of one value will be present.
     *
     *   a border-EDGE-color ColorType or inheritance value
     *   a border-EDGE-style EnumType or inheritance value
     *   a border-EDGE-width MappedNumeric or inheritance value
     *
     *  N.B. this is the order of elements defined in
     *       ShorthandPropSets.borderRightExpansion
     *
     * @param foNode - the <tt>FONode</tt> being built
     * @param value <tt>PropertyValue</tt> returned by the parser
     * @return <tt>PropertyValue</tt> the verified value
     */
    public /**/static/**/ PropertyValue refineParsing
                                    (FONode foNode, PropertyValue value)
                throws PropertyException
    {
        return borderEdge(foNode, value,
                                PropNames.BORDER_LEFT_STYLE,
                                PropNames.BORDER_LEFT_COLOR,
                                PropNames.BORDER_LEFT_WIDTH
                                );
    }
}

