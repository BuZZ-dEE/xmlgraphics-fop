
package org.apache.fop.datatypes;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.PropertyConsts;
import org.apache.fop.fo.properties.*;
import org.apache.fop.datatypes.AbstractPropertyValue;
import org.apache.fop.datatypes.PropertyValue;

/*
 * FontFamilySet.java
 * $Id$
 *
 * Created: Mon Nov 26 22:46:05 2001
 * 
 *  ============================================================================
 *                    The Apache Software License, Version 1.1
 *  ============================================================================
 *  
 *  Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without modifica-
 *  tion, are permitted provided that the following conditions are met:
 *  
 *  1. Redistributions of  source code must  retain the above copyright  notice,
 *     this list of conditions and the following disclaimer.
 *  
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  
 *  3. The end-user documentation included with the redistribution, if any, must
 *     include  the following  acknowledgment:  "This product includes  software
 *     developed  by the  Apache Software Foundation  (http://www.apache.org/)."
 *     Alternately, this  acknowledgment may  appear in the software itself,  if
 *     and wherever such third-party acknowledgments normally appear.
 *  
 *  4. The names "FOP" and  "Apache Software Foundation"  must not be used to
 *     endorse  or promote  products derived  from this  software without  prior
 *     written permission. For written permission, please contact
 *     apache@apache.org.
 *  
 *  5. Products  derived from this software may not  be called "Apache", nor may
 *     "Apache" appear  in their name,  without prior written permission  of the
 *     Apache Software Foundation.
 *  
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 *  APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 *  INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 *  DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 *  OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 *  ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 *  (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 *  This software  consists of voluntary contributions made  by many individuals
 *  on  behalf of the Apache Software  Foundation and was  originally created by
 *  James Tauber <jtauber@jtauber.com>. For more  information on the Apache 
 *  Software Foundation, please see <http://www.apache.org/>.
 *  
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 * @version $Revision$ $Name$
 */
/**
 * A base class for representing a set of font family names.
 */

public class FontFamilySet extends AbstractPropertyValue {

    private static final String tag = "$Name$";
    private static final String revision = "$Revision$";

    /**
     * An array of <tt>String</tt>s containing a prioritized list of
     * font family or generic font family names.
     */
    private String[] fontFamilyNames;

    /**
     * @param property <tt>int</tt> index of the property.
     * @param fontNames an array of <tt>String</tt>s containing a
     * prioritized list of font names, as literals or <tt>NCName</tt>s,
     * being either the full name of a font, or an enumeration token
     * representing a font family.
     * @exception PropertyException.
     */
    public FontFamilySet(int property, String[] fontFamilyNames)
        throws PropertyException
    {
        super(property, PropertyValue.FONT_FAMILY);
        this.fontFamilyNames = fontFamilyNames;
    }

    /**
     * @param propertyName <tt>String</tt> name of the property.
     * @param fontNames an array of <tt>String</tt>s containing a
     * prioritized list of font names, as literals or <tt>NCName</tt>s,
     * being either the full name of a font, or an enumeration token
     * representing a font family.
     * @exception PropertyException.
     */
    public FontFamilySet(String propertyName, String[] fontFamilyNames)
        throws PropertyException
    {
        this(PropNames.getPropertyIndex(propertyName), fontFamilyNames);
    }

    /**
     * Validate the <i>FontFamilySet</i> against the associated property.
     */
    public void validate() throws PropertyException {
        super.validate(Property.FONTSET);
    }

    /**
     * An <tt>Iterator</tt> implementing member class of FontFamilySet.
     */
    class Traverser implements Iterator {

        /**
         * The index for the iteration across the fontFamilyNames array.
         */
        private int index = 0;

        public Traverser() {}

        public boolean hasNext() {
            return index < fontFamilyNames.length;
        }

        public Object next() {
            if (hasNext()) return (Object)fontFamilyNames[index++];
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
