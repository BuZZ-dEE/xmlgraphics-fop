
package org.apache.fop.datatypes;

import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.expr.PropertyValue;
import org.apache.fop.fo.expr.AbstractPropertyValue;
import org.apache.fop.fo.Properties;

/*
 * Bool.java
 * $Id$
 *
 * Created: Fri Nov 23 15:21:37 2001
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */
/**
 * Boolean property value.  May take values of "true" or "false".
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 * @version $Revision$ $Name$
 */

public class Bool extends AbstractPropertyValue {

    private static final String tag = "$Name$";
    private static final String revision = "$Revision$";

    /**
     * The boolean value of the property
     */
    private boolean bool = false;

    /**
     * @param property the <tt>int</tt> index of the property on which
     * this value is being defined.
     * @param bool the <tt>boolean</tt> value.
     * @exception PropertyException
     */
    public Bool (int property, boolean bool)
        throws PropertyException
    {
        super(property, PropertyValue.BOOL);
        this.bool = bool;
    }

    /**
     * @param property the <tt>int</tt> index of the property on which
     * this value is being defined.
     * @param boolStr a <tt>String</tt> containing the boolean value.  It
     * must be either "true" or "false".
     * @exception PropertyException
     */
    public Bool (int property, String boolStr)
        throws PropertyException
    {
        super(property, PropertyValue.BOOL);
        if (boolStr.equals("true")) bool = true;
        else if (boolStr.equals("false")) bool = false;
        else throw new PropertyException
                     ("Attempt to set Bool to " + boolStr);
    }

    /**
     * @param propertyName the <tt>String</tt< name of the property on which
     * this value is being defined.
     * @param boolStr a <tt>String</tt> containing the boolean value.  It
     * must be either "true" or "false".
     * @exception PropertyException
     */
    public Bool (String propertyName, String boolStr)
        throws PropertyException
    {
        super(propertyName, PropertyValue.BOOL);
        if (boolStr.equals("true")) bool = true;
        else if (boolStr.equals("false")) bool = false;
        else throw new PropertyException
                     ("Attempt to set Bool to " + boolStr);
    }

    /**
     * @param propertyName the <tt>String</tt< name of the property on which
     * this value is being defined.
     * @param bool the <tt>boolean</tt> value.
     * @exception PropertyException
     */
    public Bool (String propertyName, boolean bool)
        throws PropertyException
    {
        super(propertyName, PropertyValue.BOOL);
        this.bool = bool;
    }

    /**
     * @return the String.
     */
    public boolean getBoolean() {
        return bool;
    }

    /**
     * validate the <i>Bool</i> against the associated property.
     */
    public void validate() throws PropertyException {
        super.validate(Properties.BOOL);
    }

    public String toString() {
        return bool ? "true" : "false" + "\n" + super.toString();
    }

}
