/*
 * $Id$
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
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 * @version $Revision$ $Name$
 */
package org.apache.fop.xml;

import org.xml.sax.helpers.AttributesImpl;

/**
 * This is a data class to encapsulate the data of an individual XML
 * parse event. The current version, while defining accessor methods,
 * leaves the component data of the event as protected.  The
 * <tt>XMLSerialHandler</tt> methods set the values directly.
 */

public class XMLEvent {

    private static final String tag = "$Name$";
    private static final String revision = "$Revision$";

    public static final int NOEVENT = 0;
    public static final int STARTDOCUMENT = 1;
    public static final int ENDDOCUMENT = 2;
    public static final int STARTELEMENT = 3;
    public static final int ENDELEMENT = 4;
    public static final int CHARACTERS = 5;

    private static final int MIN_XML_EV_TYPE = NOEVENT;
    private static final int MAX_XML_EV_TYPE = CHARACTERS;

    public static final boolean DISCARD_W_SPACE = true;
    public static final boolean RETAIN_W_SPACE = false;

    public static String eventTypeName(int type) {
        switch (type) {
        case NOEVENT:
            return "NOEVENT";
        case STARTDOCUMENT:
            return "STARTDOCUMENT";
        case ENDDOCUMENT:
            return "ENDDOCUMENT";
        case STARTELEMENT:
            return "STARTELEMENT";
        case ENDELEMENT:
            return "ENDELEMENT";
        case CHARACTERS:
            return "CHARACTERS";
        default:
            return "Unknown type " + type;
        }
    }

    // These are made public specifically so the the values of individual
    // XMLEvent instances can be changed directly, on the assumption that
    // the basic XML events are unlikely to change.
    protected int type = NOEVENT;
    protected String chars;
    protected int uriIndex;
    protected String localName;
    protected String qName;
    protected AttributesImpl attributes;
    protected XMLNamespaces namespaces;

    /** Sequence id for this <i>XMLEvent</i>. */
    public final int id;

    /**
     * The one-argument constructor uses the default initialization values:
     * NOEVENT for the event <i>type</i>, and null references for all others
     * except <i>namespaces</i>.
     */
    public XMLEvent (XMLNamespaces namespaces) {
        this.namespaces = namespaces;
        id = namespaces.getSequence();
    }

    /**
     * The fully defined constructor takes values for each of the data
     * elements.
     */
    public XMLEvent(int type, String chars, int uriIndex,
                    String localName, String qName,
                    AttributesImpl attributes, XMLNamespaces namespaces)
    {
        this.namespaces = namespaces;
        id = namespaces.getSequence();
        this.type = type;
        this.chars = chars;
        this.uriIndex = uriIndex;
        this.localName = localName;
        this.qName = qName;
        this.attributes = attributes;
    }

    /**
     * The cloning constructor takes a reference to an existing
     * <tt>XMLEvent</tt> object.
     */
    public XMLEvent(XMLEvent ev) {
        namespaces = ev.namespaces;
        id = namespaces.getSequence();
        type = ev.type;
        chars = ev.chars;
        uriIndex = ev.uriIndex;
        localName = ev.localName;
        qName = ev.qName;
        attributes = ev.attributes;
    }

    public XMLEvent(int type, String chars, XMLNamespaces namespaces) {
        this.namespaces = namespaces;
        id = namespaces.getSequence();
        this.type = type;
        this.chars = chars;
    }

    /**
     * Clear the fields of this event.  Provided for pool operations.
     * Neither the <i>namespaces</i> nor the <i>id</i> field is cleared.
     * @return the cleared event.
     */
    public XMLEvent clear() {
        type = NOEVENT;
        chars = null;
        uriIndex = 0;
        localName = null;
        qName = null;
        attributes = null;
        return this;
    }

    /**
     * Copy the fields of the argument event to this event.
     * Provided for pool operations.
     * Neither the <i>namespaces</i> nor the <i>id</i> field is copied.
     * @param ev the <tt>XMLEvent</tt> to copy.
     * @return this (copied) event.
     */
    public XMLEvent copyEvent(XMLEvent ev) {
        type = ev.type;
        chars = ev.chars;
        uriIndex = ev.uriIndex;
        localName = ev.localName;
        qName = ev.qName;
        attributes = ev.attributes;
        return this;
    }

    public int getType() { return type; }
    public void setType(int type) {
        if (type < MIN_XML_EV_TYPE || type > MAX_XML_EV_TYPE) {
            throw new IllegalArgumentException(
                    "XML event type out of range.");
        }
        this.type = type;
    }

    public String getChars() { return chars; }

    public void setChars(String chars) {
        this.chars = chars;
    }

    public void setChars(char[] ch, int start, int length) {
        chars = new String(ch, start, length);
    }

    public String getUri() { return namespaces.getIndexURI(uriIndex); }
    public int getUriIndex() { return uriIndex; }
    public void setUriIndex(int uriIndex) {
        this.uriIndex = uriIndex;
    }

    /**
     * Get the local name of this event.
     * @return the local name.
     */
    public String getLocalName() { return localName; }

    /**
     * Set the local name of this event.
     * @param localName - the local name.
     */
    public void setLocalName(String localName) {
        this.localName = localName;
    }

    /**
     * Get the qualified name of this event.
     * @return the qualified name.
     */
    public String getQName() { return qName; }

    /**
     * Get the prefix of the qualified name of this evetn.
     * @return - the prefix.
     */
    public String getQNamePrefix() {
        int i;
        if ((i = qName.indexOf(':')) == -1) {
            return "";
        } else {
            return qName.substring(0, i);
        }
    }

    /**
     * Set the qualified name of this event.
     * @param qName - the qualified name.
     */
    public void setQName(String qName) {
        this.qName = qName;
    }

    /**
     * Get the <tt>AttributesImpl</tt> object associated with this event.
     * @return the <tt>AttributesImpl</tt> object.
     */
    public AttributesImpl getAttributes() { return attributes; }

    /**
     * Set the <tt>AttributesImpl</tt> object associated with this event.
     * @param attributes the attributes
     */
    public void setAttributes(AttributesImpl attributes) {
        this.attributes = attributes;
    }

    /**
     * Set the <tt>XMLNamespaces</tt> object associated with this event.
     * @param namespaces  the XMLNamespaces
     */
    public void setNamespaces(XMLNamespaces namespaces) {
        this.namespaces = namespaces;
    }

    /**
     * Get the <tt>XMLNamespaces</tt> object associated with this event.
     * @return the <tt>XMLNamespaces</tt> object.
     */
    public XMLNamespaces getNamespaces() { return namespaces; }

    public String toString() {
        String tstr;
        tstr = eventTypeName(type);
        tstr = tstr + "\nSeq " + id;
        tstr = tstr + "\nNamespaces " + namespaces.hashCode();
        tstr = tstr + "\nURI "
                + uriIndex + " " + namespaces.getIndexURI(uriIndex);
        tstr = tstr + "\n" + "Local Name " + localName;
        tstr = tstr + "\n" + "QName " + qName;
        tstr = tstr + "\n" + "Chars <<<" + chars + ">>>";
        if (attributes == null) {
            tstr = tstr + "\n" + "Attributes null";
        } else {
            int len = attributes.getLength();
            tstr = tstr + "\n" + "No. of attributes " + len;
            for (int i = 0; i < len; i++) {
                tstr = tstr + "\n" + "  URI: " + attributes.getURI(i);
                tstr = tstr + "\n" + "  QName: " + attributes.getQName(i);
                tstr = tstr + "\n" + "  LocalName: "
                                   + attributes.getLocalName(i);
                tstr = tstr + "\n" + "  type: " + attributes.getType(i);
                tstr = tstr + "\n" + "  value: " + attributes.getValue(i);
            }
        }
        return tstr;
    }

}
