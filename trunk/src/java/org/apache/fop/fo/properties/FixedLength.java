/*
 * Copyright 1999-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.fo.properties;

import org.apache.fop.datatypes.PercentBaseContext;

/**
 * An absolute length quantity in XSL
 */
public class FixedLength extends LengthProperty {
    private int millipoints;

    /**
     * Set the length given
     * @param numRelUnits the number of relative units
     * @param iCurFontSize the current font size in base units.
     */
    public FixedLength(double numRelUnits, int iCurFontSize) {
        millipoints = (int) (numRelUnits * (double)iCurFontSize);
    }

    /**
     * Set the length given a number of units and a unit name.
     * @param numUnits quantity of input units
     * @param units input unit specifier (in, cm, etc.)
     */
    public FixedLength(double numUnits, String units) {
        convert(numUnits, units);
    }

    /**
     * @param baseUnits the length as a number of base units (millipoints)
     */
    public FixedLength(int baseUnits) {
        millipoints = baseUnits;
    }

    /**
     * Convert the given length to a dimensionless integer representing
     * a whole number of base units (milli-points).
     * @param dvalue quantity of input units
     * @param unit input unit specifier (in, cm, etc.)
     */
    protected void convert(double dvalue, String unit) {
        // TODO: the whole routine smells fishy.

        int assumedResolution = 1;    // points/pixel

        if (unit.equals("in")) {
            dvalue = dvalue * 72;
        } else if (unit.equals("cm")) {
            dvalue = dvalue * 28.3464567;
        } else if (unit.equals("mm")) {
            dvalue = dvalue * 2.83464567;
        } else if (unit.equals("pt")) {
            // Do nothing.
            // dvalue = dvalue;
        } else if (unit.equals("mpt")) { //mpt is non-standard!!! mpt=millipoints
            // TODO: this seems to be wrong.
            // Do nothing.
            // dvalue = dvalue;
        } else if (unit.equals("pc")) {
            dvalue = dvalue * 12;
            /*
             * } else if (unit.equals("em")) {
             * dvalue = dvalue * fontsize;
             */
        } else if (unit.equals("px")) {
            // TODO: get resolution from user agent?
            dvalue = dvalue * assumedResolution;
        } else {
            dvalue = 0;
            log.error("Unknown length unit '" + unit + "'");
        }
        if (unit.equals("mpt")) {
            millipoints = (int)dvalue;
        } else {
            millipoints = (int)(dvalue * 1000);
        }
    }

    /**
     * @see org.apache.fop.datatypes.Numeric#getValue()
     */
    public int getValue() {
        return millipoints;
    }

    /**
     * @see org.apache.fop.datatypes.Numeric#getValue(PercentBaseContext)
     */
    public int getValue(PercentBaseContext context) {
        return millipoints;
    }

    /**
     * @see org.apache.fop.datatypes.Numeric#getNumericValue()
     */
    public double getNumericValue() {
        return millipoints;
    }

    /**
     * @see org.apache.fop.datatypes.Numeric#getNumericValue(PercentBaseContext)
     */
    public double getNumericValue(PercentBaseContext context) {
        return millipoints;
    }

    /**
     * Return true since FixedLength are always absolute.
     * @see org.apache.fop.datatypes.Numeric#isAbsolute()
     */
    public boolean isAbsolute() {
        return true;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return millipoints + "mpt";
    }

}

