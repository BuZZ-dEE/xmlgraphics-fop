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

package org.apache.fop.fo.pagination;

// XML
import org.xml.sax.Locator;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.ValidationException;

/**
 * Class modelling the fo:static-content object.
 */
public class StaticContent extends Flow {

    /**
     * @param parent FONode that is the parent of this object
     */
    public StaticContent(FONode parent) {
        super(parent);
    }

    /**
     * @see org.apache.fop.fo.FONode#startOfNode()
     */
    protected void startOfNode() throws FOPException {
        if (getFlowName() == null || getFlowName().equals("")) {
            throw new ValidationException("A 'flow-name' is required for "
                                   + getName() + ".", locator);
        }
        getFOEventHandler().startFlow(this);
    }

    /**
     * Make sure content model satisfied, if so then tell the
     * FOEventHandler that we are at the end of the flow.
     * @see org.apache.fop.fo.FONode#endOfNode
     */
    protected void endOfNode() throws FOPException {
        if (childNodes == null && getUserAgent().validateStrictly()) {
            missingChildElementError("(%block;)+");
        }
        getFOEventHandler().endFlow(this);
    }

    /**
     * @see org.apache.fop.fo.FONode#validateChildNode(Locator, String, String)
     * XSL Content Model: (%block;)+
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws ValidationException {
        if (!isBlockItem(nsURI, localName)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /** @see org.apache.fop.fo.FObj#getLocalName() */
    public String getLocalName() {
        return "static-content";
    }

    /** @see org.apache.fop.fo.FObj#getNameId() */
    public int getNameId() {
        return FO_STATIC_CONTENT;
    }
}
