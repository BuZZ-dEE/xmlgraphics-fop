/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

package org.apache.fop.fo.flow;

import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObjMixed;
import org.apache.fop.fo.ValidationException;
import org.xml.sax.Locator;

/**
 * Class modelling the fo:wrapper object.
 * The wrapper object serves as a property holder for 
 * its child node objects.
 */
public class Wrapper extends FObjMixed {
    // The value of properties relevant for fo:wrapper.
    // End of property values
    
    // used for FO validation
    private boolean blockOrInlineItemFound = false;

    /**
     * @param parent FONode that is the parent of this object
     */
    public Wrapper(FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     * XSL Content Model: marker* (#PCDATA|%inline;|%block;)*
     * Additionally (unimplemented): "An fo:wrapper that is a child of an 
     * fo:multi-properties is only permitted to have children that would 
     * be permitted in place of the fo:multi-properties."
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws ValidationException {
        if (FO_URI.equals(nsURI) && localName.equals("marker")) {
            if (blockOrInlineItemFound) {
               nodesOutOfOrderError(loc, "fo:marker", 
                    "(#PCDATA|%inline;|%block;)");
            }
        } else if (isBlockOrInlineItem(nsURI, localName)) {
            blockOrInlineItemFound = true;
        } else {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "wrapper";
    }

    /** {@inheritDoc} */
    public int getNameId() {
        return FO_WRAPPER;
    }
}

