/*-- $Id$ --

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

    Copyright (C) 1999 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Fop" and  "Apache Software Foundation"  must not be used to
    endorse  or promote  products derived  from this  software without  prior
    written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 James Tauber <jtauber@jtauber.com>. For more  information on the Apache
 Software Foundation, please see <http://www.apache.org/>.

 */
package org.apache.fop.svg;

import java.util.*;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.apps.FOPException;

/**
 * a class representing all the length properties in SVG
 */
public class SVGStringProperty extends Property {

    /**
     * inner class for making SVG Length objects.
     */
    public static class Maker extends Property.Maker {

        /**
         * whether this property is inherited or not.
         *
         * @return is this inherited?
         */
        public boolean isInherited() {
            return false;
        }

        /**
         * make an SVG Length property with the given value.
         *
         * @param propertyList the property list this is a member of
         * @param value the explicit string value of the property
         */
        public Property make(PropertyList propertyList, String value,
                             FObj fo) throws FOPException {
            return new SVGStringProperty(propertyList, value);
        }

        /**
         * make an SVG Length property with the default value.
         *
         * @param propertyList the property list the property is a member of
         */
        public Property make(PropertyList propertyList)
        throws FOPException {
            return make(propertyList, "", null);
        }
    }

    /**
     * returns the maker for this object.
     *
     * @return the maker for SVG Length objects
     */
    public static Property.Maker maker(String name) {
        return new SVGStringProperty.Maker();
    }

    /** the length as a Length object */
    protected String value;

    /**
     * construct an SVG length (called by the Maker).
     *
     * @param propertyList the property list this is a member of
     * @param explicitValue the explicit value as a Length object
     */
    protected SVGStringProperty(PropertyList propertyList,
                                String explicitValue) {
        this.value = explicitValue;
    }

    /**
     * get the length
     *
     * @return the length as a Length object
     */
    public String getString() {
        return this.value;
    }
}
