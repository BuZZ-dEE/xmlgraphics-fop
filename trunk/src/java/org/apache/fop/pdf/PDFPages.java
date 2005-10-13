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
 
package org.apache.fop.pdf;

// Java
import java.util.List;
import java.util.ArrayList;

/**
 * class representing a /Pages object.
 *
 * A /Pages object is an ordered collection of pages (/Page objects)
 * (Actually, /Pages can contain further /Pages as well but this
 * implementation doesn't allow this)
 */
public class PDFPages extends PDFObject {

    /**
     * the /Page objects
     */
    protected List kids = new ArrayList();

    /**
     * the number of /Page objects
     */
    protected int count = 0;

    // private PDFPages parent;

    /**
     * create a /Pages object. NOTE: The PDFPages
     * object must be created before the PDF document is
     * generated, but it is not written to the stream immediately.
     * It must also be allocated an object ID (so that the kids
     * can refer to the parent) so that the XRef table needs to
     * be updated before this object is written.
     *
     * @param objnum the object's number
     */
    public PDFPages(int objnum) {
        setObjectNumber(objnum);
    }

    /**
     * add a /Page object.
     *
     * @param page the PDFPage to add.
     */
    public void addPage(PDFPage page) {
        page.setParent(this);
        this.incrementCount();
    }
    
    /**
     * Use this method to notify the PDFPages object that a child page
     * @param page the child page
     */
    public void notifyKidRegistered(PDFPage page) {
        this.kids.add(page.referencePDF());
    }

    /**
     * get the count of /Page objects
     *
     * @return the number of pages
     */
    public int getCount() {
        return this.count;
    }

    /**
     * increment the count of /Page objects
     */
    public void incrementCount() {
        this.count++;
        // log.debug("Incrementing count to " + this.getCount());
    }

    /**
     * @see org.apache.fop.pdf.PDFObject#toPDFString()
     */
    public String toPDFString() {
        StringBuffer sb = new StringBuffer(64);
        sb.append(getObjectID()).
            append("<< /Type /Pages\n/Count ").
            append(this.getCount()).
            append("\n/Kids [");
        for (int i = 0; i < kids.size(); i++) {
            sb.append(kids.get(i)).append(" ");
        }
        sb.append("] >>\nendobj\n");
        return sb.toString();
    }

}
