/*
 * Ems.java
 * $Id$
 * Created: Wed Nov 21 15:39:30 2001
 * 
 * ============================================================================
 *                   The Apache Software License, Version 1.1
 * ============================================================================
 * 
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of  source code must  retain the above copyright  notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include  the following  acknowledgment:  "This product includes  software
 *    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
 *    Alternately, this  acknowledgment may  appear in the software itself,  if
 *    and wherever such third-party acknowledgments normally appear.
 * 
 * 4. The names "FOP" and  "Apache Software Foundation"  must not be used to
 *    endorse  or promote  products derived  from this  software without  prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 * 
 * 5. Products  derived from this software may not  be called "Apache", nor may
 *    "Apache" appear  in their name,  without prior written permission  of the
 *    Apache Software Foundation.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 * APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 * ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 * (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * This software  consists of voluntary contributions made  by many individuals
 * on  behalf of the Apache Software  Foundation and was  originally created by
 * James Tauber <jtauber@jtauber.com>. For more  information on the Apache 
 * Software Foundation, please see <http://www.apache.org/>.
 *  
 */

package org.apache.fop.datatypes;

import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.PropertyConsts;
import org.apache.fop.fo.expr.PropertyException;

/**
 * Constructor class for relative lengths measured in <i>ems</i>.  Constructs
 * a <tt>Numeric</tt>.
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 * @version $Revision$ $Name$
 */

public class Ems {

    private static final String tag = "$Name$";
    private static final String revision = "$Revision$";

    /**
     * Private constructor - don't instantiate a <i>Ems</i> object.
     */
    private Ems() {}

    /**
     * Construct a <tt>Numeric</tt> with a given unit and quantity.
     * The unit power is assumed as 1.  The base unit is millipoints.
     * @param node - the <tt>FONode</tt> with reference to which this
     * <i>EM</i> value is being consructed.  A null value imples the
     * construction of an <i>initial value</i>.
     * @param property the index of the property with which this value
     * is associated.
     * @param value the number of units.
     * @return a <tt>Numeric</tt> representing this <i>Ems</i>.
     */
    public static Numeric makeEms(FONode node, int property, double value)
        throws PropertyException
    {
        Numeric numeric = new Numeric(property, value, Numeric.EMS, 0, 0);
        if (node == null)
            numeric.expandEms((Numeric)
            (PropertyConsts.pconsts.getInitialValue(PropNames.FONT_SIZE)));
        else
            numeric.expandEms(node.currentFontSize());
        return numeric;
    }

    /**
     * Construct a <tt>Numeric</tt> with a given unit and quantity.
     * The unit power is assumed as 1.  The base unit is millipoints.
     * @param node - the <tt>FONode</tt> with reference to which this
     * <i>EM</i> value is being consructed.  A null value imples the
     * construction of an <i>initial value</i>.
     * @param propertyName the name of the property with which this value
     * is associated.
     * @param value the number of units.
     * @return a <tt>Numeric</tt> representing this <i>Ems</i>.
     */
    public static Numeric makeEms
                            (FONode node, String propertyName, double value)
        throws PropertyException
    {
        return makeEms(node, PropNames.getPropertyIndex(propertyName), value);
    }

}
