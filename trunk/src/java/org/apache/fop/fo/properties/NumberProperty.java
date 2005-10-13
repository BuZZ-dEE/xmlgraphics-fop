/*
 * Copyright 1999-2004 The Apache Software Foundation.
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
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;

/**
 * Class for handling numeric properties
 */
public class NumberProperty extends Property implements Numeric {

    /**
     * Inner class for making NumberProperty objects
     */
    public static class Maker extends PropertyMaker {

        /**
         * Constructor for NumberProperty.Maker
         * @param propId the id of the property for which a Maker should be created
         */
        public Maker(int propId) {
            super(propId);
        }

        /**
         * @see PropertyMaker#convertProperty
         */
        public Property convertProperty(Property p,
                                        PropertyList propertyList, FObj fo) {
            if (p instanceof NumberProperty) {
                return p;
            }
            if (p instanceof EnumProperty) {
                return new EnumNumber(p);
            }
            Number val = p.getNumber();
            if (val != null) {
                return new NumberProperty(val);
            }
            return convertPropertyDatatype(p, propertyList, fo);
        }

    }

    private Number number;

    /**
     * Constructor for Number input
     * @param num Number object value for property
     */
    public NumberProperty(Number num) {
        this.number = num;
    }

    /**
     * Constructor for double input
     * @param num double numeric value for property
     */
    public NumberProperty(double num) {
        this.number = new Double(num);
    }

    /**
     * Constructor for integer input
     * @param num integer numeric value for property
     */
    public NumberProperty(int num) {
        this.number = new Integer(num);
    }
    
    /**
     * Plain number always has a dimension of 0.
     * @return a dimension of 0.
     * @see Numeric#getDimension()
     */
    public int getDimension() {
        return 0;
    }

    /**
     * Return the value of this Numeric.
     * @return The value as a double.
     * @see Numeric#getNumericValue()
     */
    public double getNumericValue() {
        return number.doubleValue();
    }

    /**
     * Return the value of this Numeric.
     * @param context Evaluation context
     * @return The value as a double.
     * @see Numeric#getNumericValue(PercentBaseContext)
     */
    public double getNumericValue(PercentBaseContext context) {
        return getNumericValue();
    }

    public int getValue() {
        return number.intValue();
    }

    /**
     * Return the value
     * @param context Evaluation context
     * @return The value as an int.
     * @see Numeric#getValue(PercentBaseContext)
     */
    public int getValue(PercentBaseContext context) {
        return getValue();
    }

    /**
     * Return true because all numbers are absolute.
     * @return true.
     * @see Numeric#isAbsolute()
     */
    public boolean isAbsolute() {
        return true;
    }

    /**
     * @return this.number cast as a Number
     */
    public Number getNumber() {
        return this.number;
    }

    /**
     * @return this.number cast as an Object
     */
    public Object getObject() {
        return this.number;
    }

    /**
     * Convert NumberProperty to Numeric object
     * @return Numeric object corresponding to this
     */
    public Numeric getNumeric() {
        return this;
    }

    /**
     * Convert NumberProperty to a ColorType. Not sure why this is needed.
     * @return ColorType that corresponds to black
     */
    public ColorTypeProperty getColorType() {
        // Convert numeric value to color ???
        // Convert to hexadecimal and then try to make it into a color?
        return new ColorTypeProperty((float)0.0, (float)0.0, (float)0.0);
    }

}
