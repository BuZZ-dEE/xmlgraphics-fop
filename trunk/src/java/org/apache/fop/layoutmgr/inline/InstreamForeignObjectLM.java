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

package org.apache.fop.layoutmgr.inline;

import org.apache.fop.area.Area;
import org.apache.fop.area.inline.ForeignObject;
import org.apache.fop.fo.XMLObj;
import org.apache.fop.fo.flow.InstreamForeignObject;

/**
 * LayoutManager for the fo:instream-foreign-object formatting object
 */
public class InstreamForeignObjectLM extends AbstractGraphicsLayoutManager {
    
    private InstreamForeignObject fobj;
    
    /**
     * Constructor
     * @param node the formatting object that creates this area
     */
    public InstreamForeignObjectLM(InstreamForeignObject node) {
        super(node);
        fobj = node;
    }

    /**
     * Get the inline area created by this element.
     *
     * @return the inline area
     */
    protected Area getChildArea() {
        XMLObj child = (XMLObj) fobj.getChildXMLObj();

        org.w3c.dom.Document doc = child.getDOMDocument();
        String ns = child.getDocumentNamespace();

        return new ForeignObject(doc, ns);
    }
    
}

