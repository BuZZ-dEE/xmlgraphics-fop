package org.apache.fop.fo.properties;

import org.apache.fop.datatypes.PropertyValueList;
import org.apache.fop.datatypes.NCName;
import org.apache.fop.datatypes.EnumType;
import org.apache.fop.datastructs.ROStringArray;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.datatypes.Auto;
import org.apache.fop.datatypes.indirect.Inherit;
import org.apache.fop.datatypes.indirect.FromParent;
import org.apache.fop.datatypes.indirect.FromNearestSpecified;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.ShorthandPropSets;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.properties.Property;

public class PageBreakInside extends Property  {
    public static final int dataTypes = SHORTHAND | AUTO | ENUM | INHERIT;
    public static final int traitMapping = SHORTHAND_MAP;
    public static final int initialValueType = AUTO_IT;
    public static final int AVOID = 1;
    public static final int inherited = NO;

    private static final String[] rwEnums = {
        null
        ,"avoid"
    };

    /*
     * @param propindex - the <tt>int</tt> property index.
     * @param foNode - the <tt>FONode</tt> being built
     * @param value <tt>PropertyValue</tt> returned by the parser
     * @return <tt>PropertyValue</tt> the verified value
     */
    public PropertyValue refineParsing
                        (int propindex, FONode foNode, PropertyValue value)
                    throws PropertyException
    {
        if (value instanceof Inherit |
                value instanceof FromParent |
                    value instanceof FromNearestSpecified |
                        value instanceof Auto)
        {
            return refineExpansionList(PropNames.PAGE_BREAK_INSIDE, foNode,
                                ShorthandPropSets.expandAndCopySHand(value));
        }
        if (value instanceof NCName) {
            EnumType enum = null;
            String ncname = ((NCName)value).getNCName();
            //PropertyValueList list =
                    //new PropertyValueList(PropNames.PAGE_BREAK_INSIDE);
            if (ncname.equals("avoid")) {
                //list.add
                    //(new EnumType(PropNames.KEEP_TOGETHER, "always"));
                //return list;
                return new EnumType(PropNames.KEEP_TOGETHER, "always");
            } else
                throw new PropertyException
                ("Unrecognized NCName in page-break-inside: " + ncname);
        }

        throw new PropertyException
            ("Invalid value for 'page-break-inside': "
                + value.getClass().getName());
    }

    public int getEnumIndex(String enum) throws PropertyException {
        return enumValueToIndex(enum, rwEnums);
    }
    public String getEnumText(int index) {
        return rwEnums[index];
    }
}

