package org.apache.fop.fo.properties;

import org.apache.fop.datatypes.PropertyValueList;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.ShorthandPropSets;
import org.apache.fop.fo.properties.Property;

public class Cue extends Property  {
    public static final int dataTypes = SHORTHAND;
    public static final int traitMapping = SHORTHAND_MAP;
    public static final int initialValueType = NOTYPE_IT;
    public static final int inherited = NO;

    /**
     * 'value' is a PropertyValueList or an individual PropertyValue.
     *
     * <p>If 'value' is an individual PropertyValue, it must contain
     * either
     *   a parsed UriType value,
     *   a FromParent value,
     *   a FromNearestSpecified value,
     *   or an Inherit value.
     *
     * <p>If 'value' is a PropertyValueList, it contains an inner
     * PropertyValueList of 2 parsed UriType values.
     *
     * <p>The value(s) provided, if valid, are converted into a list
     * containing the expansion of the shorthand.
     * The first element is a value for cue-before,
     * the second element is a value for cue-after.
     *
     * @param propindex - the <tt>int</tt> property index.
     * @param foNode - the <tt>FONode</tt> being built
     * @param value <tt>PropertyValue</tt> returned by the parser
     * @return <tt>PropertyValue</tt> the verified value
     */
    public /*static*/ PropertyValue refineParsing
                        (int propindex, FONode foNode, PropertyValue value)
                throws PropertyException
    {
        int type = value.getType();
        if (type == PropertyValue.LIST) {
            if (type == PropertyValue.INHERIT ||
                    type == PropertyValue.FROM_PARENT ||
                        type == PropertyValue.FROM_NEAREST_SPECIFIED ||
                            type == PropertyValue.URI_TYPE)
                return refineExpansionList(PropNames.CUE, foNode,
                                ShorthandPropSets.expandAndCopySHand(value));
            throw new PropertyException
                ("Invalid " + value.getClass().getName() +
                    " object for cue");
        } else {
            // List may contain only 2 uri specifiers
            PropertyValueList list =
                            spaceSeparatedList((PropertyValueList)value);
            if (list.size() != 2)
                throw new PropertyException
                    ("List of " + list.size() + " for cue");
            PropertyValue cue1 = (PropertyValue)(list.getFirst());
            int cue1type = cue1.getType();
            PropertyValue cue2 = (PropertyValue)(list.getLast());
            int cue2type = cue2.getType();

            if ( ! ((cue1type == PropertyValue.URI_TYPE) &&
                    (cue2type == PropertyValue.URI_TYPE)))
                throw new PropertyException
                    ("Values to cue are not both URIs");
            // Set the individual expanded properties of the
            // cue compound property
            // Should I clone these values?
            cue1.setProperty(PropNames.CUE_BEFORE);
            cue2.setProperty(PropNames.CUE_AFTER);
            return value;
        }
    }
}

