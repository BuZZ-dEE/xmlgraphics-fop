/*
 * $Id$
 * 
 * 
 * ============================================================================
 *                   The Apache Software License, Version 1.1
 * ============================================================================
 * 
 * Copyright (C) 1999-2004 The Apache Software Foundation. All rights reserved.
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
 *
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 * @version $Revision$ $Name$
 */
package org.apache.fop.xml;

import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datastructs.SyncedCircularBuffer;
import org.apache.fop.fo.FObjectNames;
import org.apache.fop.fo.FObjectSets;

/**
 * A synchronized circular buffer for XMLEvents.
 * @see org.apache.fop.datastructs.SyncedCircularBuffer
 */
public class SyncedXmlEventsBuffer extends SyncedCircularBuffer {

    /**
     * Constant for <i>discardEvent</i> field of
     * <i>getEndElement(boolean discardEvent, XMLEvent(, boolean)).
     */
    public static final boolean DISCARD_EV = true,
                                 RETAIN_EV = false;

    /**
     * Maintains an index of namespace URIs.  These can then be referred to
     * by an <tt>int</tt> index.
     */
    private Namespaces namespaces;

    /**
     * No-argument constructor sets up a buffer with the default number of
     * elements.
     * The producer and consumer <tt>Thread</tt>s default to the current
     * thread at the time of instantiation.
     */
    public SyncedXmlEventsBuffer()
        throws IllegalArgumentException
    {
        super();
        namespaces = new Namespaces();
    }

    /**
     * Constructor taking one argument; the size of the buffer.
     * @param size the size of the buffer.  Must be > 1.
     */
    public SyncedXmlEventsBuffer(int size)
        throws IllegalArgumentException
    {
        super(size);
        namespaces = new Namespaces();
    }

    /**
     * Get the <tt>Namespaces</tt> from this buffer.
     * @return - the namespaces object.
     */
    public Namespaces getNamespaces() { return namespaces; }

    /**
     * @return next event from the SyncedCircularBuffer
     * @exception FOPException  exception into which
     * any InterruptedException exceptions thrown by the
     * <tt>SyncedCircularBuffer</tt> are transformed
     */
    public XMLEvent getEvent() throws FOPException {
        XMLEvent ev;
        try {
            ev = (XMLEvent)get();
            //System.out.println("getEvent: " + ev);
            return ev;
        } catch (InterruptedException e) {
            throw new FOPException(e);
        }
    }

    /**
     * Get the next event of the given type from the buffer.  Discard
     * intervening events.
     * @param eventType - the <tt>int</tt> event type.
     * @return an event of the given type.
     * @exception FOPException if buffer errors or interrupts occur.
     * @exception NoSuchElementException if the event is not found.
     */
    public XMLEvent getSaxEvent(int eventType) throws FOPException {
        XMLEvent ev = getEvent();
        while (ev != null && ev.type != eventType) {
            ev = getEvent();
        }
        if (ev == null) {
            throw new NoSuchElementException
                        (XMLEvent.eventTypeName(eventType) + " not found.");
        }
        return ev;
    }

    /**
     * Get the next event of the given type and with the given <tt>QName</tt>
     * from the buffer.  Discard intervening events.
     * @param eventType - the <tt>int</tt> event type.
     * @param qName a <tt>String</tt> with the <tt>QName</tt> of the
     * required element.
     * @return an event of the given type.
     * @exception FOPException if buffer errors or interrupts occur.
     * @exception NoSuchElementException if the event is not found.
     */
    public XMLEvent getSaxQNameEvent(int eventType, String qName)
                throws FOPException
    {
        XMLEvent ev = getEvent();
        while (ev != null &&
               ! (ev.type == eventType && ev.qName.equals(qName))) {
            ev = getEvent();
        }
        if (ev == null) {
            throw new NoSuchElementException
            (XMLEvent.eventTypeName(eventType) + " " + qName + " not found.");
        }
        return ev;
    }

    /**
     * Get the next event of the given SAX type, from the given namespace
     * (<code>uriIndex</code>) with the given local name, from the buffer.
     * Discard intervening events.
     * @param eventType the SAX event type.
     * @param uriIndex the URI index maintained in the
     * <tt>Namespaces</tt> object.
     * @param localName of the required element.
     * @return an event of the given type.
     * @exception FOPException if buffer errors or interrupts occur.
     * @exception NoSuchElementException if the event is not found.
     */
    public XMLEvent getSaxUriLocalEvent
                            (int eventType, int uriIndex, String localName)
                throws FOPException
    {
        XMLEvent ev = getEvent();
        while (ev != null &&
               ! (ev.type == eventType
                  && ev.uriIndex == uriIndex
                  && ev.localName.equals(localName))) {
            namespaces.surrenderEvent(ev);
            ev = getEvent();
        }
        if (ev == null)
            throw new NoSuchElementException
                    (XMLEvent.eventTypeName(eventType)
                             + namespaces.getIndexURI(uriIndex)
                                       + ":" + localName + " not found.");
        return ev;
    }
    
    /**
     * Get the next event with of the given SAX type, whose URI is matched
     * by the namespaces URI indexed by <code>uriIndex</code>, and whose
     * namespace-specific type matches <code>nsType</code>.
     * Discard any intervening events.
     * @param eventType the SAX event type
     * @param uriIndex of the URI in namespaces
     * @param nsType the namespace-specific type
     * @return the matching event
     * @throws FOPException
     */
    public XMLEvent getSaxUriTypedEvent(
            int eventType, int uriIndex, int nsType) throws FOPException {
        XMLEvent ev = getEvent();
        while (ev != null) {
            if (ev.type == eventType && ev.uriIndex == uriIndex) {
                switch (uriIndex) {
                case Namespaces.DefAttrNSIndex:
                    throw new NoSuchElementException
                    ("No special types for default attribute namespace");
                case Namespaces.XSLNSpaceIndex:
                    // The FO namespace
                    if (ev.getFoType() == nsType) {
                        return ev;
                    }
                    break;
                case Namespaces.FOXNSpaceIndex:
                    // The FOX namespace
                    if (ev.getFoxType() == nsType) {
                        return ev;
                    }
                    break;
                case Namespaces.SVGNSpaceIndex:
                    // The SVG namespace
                    if (ev.getSvgType() == nsType) {
                        return ev;
                    }
                    break;
                }
            }
            namespaces.surrenderEvent(ev);
            ev = getEvent();
        }
        throw new NoSuchElementException
            (XMLEvent.eventTypeName(eventType) + " "
                    + namespaces.getIndexURI(uriIndex)
                    + " type " + nsType + " not found.");
    }

    /**
     * Get the next event of the given type, from the fo: namespace, with
     * the given FO type.  Discard intervening events.
     * @param eventType - the <tt>int</tt> event type.
     * @param foType - the <tt>int</tt> FO type.
     * @return an event of the given type.
     * @exception FOPException if buffer errors or interrupts occur.
     * @exception NoSuchElementException if the event is not found.
     */
    public XMLEvent getSaxFoEvent(int eventType, int foType)
                throws FOPException
    {
        return getSaxUriTypedEvent(
                eventType, Namespaces.XSLNSpaceIndex, foType);
    }

    /**
     * Return the next element if it is of the required type.  If the next
     * element is not of the required type, push it back onto the buffer.
     * @param eventType - the <tt>int</tt> event type.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return an event of the required type.  If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur.
     * @exception NoSuchElementException if the buffer is empty.
     */
    public XMLEvent expectSaxEvent
                                    (int eventType, boolean discardWhiteSpace)
                throws FOPException
    {
        XMLEvent ev = getEvent();
        if (discardWhiteSpace) {
            while (ev != null && ev.type == XMLEvent.CHARACTERS
                   && ev.chars.trim().equals("")) {
                namespaces.surrenderEvent(ev);
                ev = getEvent();
            }
        }
        if (ev != null && ev.type == eventType) {
            return ev;
        }
        if (ev == null)
            throw new NoSuchElementException
                        (XMLEvent.eventTypeName(eventType)
                                           + " not found: end of buffer.");
        pushBack(ev);
        return null;
    }

    /**
     * Return the next element if it is of the required type and has the
     * required <tt>QName</tt>.  If the next
     * element is not of the required type, push it back onto the buffer.
     * @param eventType - the <tt>int</tt> event type.
     * @param qName a <tt>String</tt> with the <tt>QName</tt> of the
     * required element.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return an event of the required type.  If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur.
     * @exception NoSuchElementException if the event is not found.
     */
    /*
    public XMLEvent expectSaxQNameEvent
                    (int eventType, String qName, boolean discardWhiteSpace)
                throws FOPException
    {
        XMLEvent ev = getEvent();
        if (discardWhiteSpace) {
            while (ev != null && ev.type == XMLEvent.CHARACTERS
                   && ev.chars.trim().equals("")) {
                namespaces.surrenderEvent(ev);
                ev = getEvent();
            }
        }
        if (ev != null && ev.type == eventType && ev.qName.equals(qName)) {
            return ev;
        }
        if (ev == null)
            throw new NoSuchElementException
                        (XMLEvent.eventTypeName(eventType)
                                           + " not found: end of buffer.");
        pushBack(ev);
        return null;
    }
    */

    /**
     * Return the next element if it is of the required type and has the
     * required URI index and local name.  If the next
     * element is not of the required type, push it back onto the buffer.
     * @param eventType - the <tt>int</tt> event type.
     * @param uriIndex - the <tt>int</tt> URI index.
     * @param localName a <tt>String</tt> with the local name of the
     * required element.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return an event of the required type.  If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur.
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectSaxUriLocalEvent
                            (int eventType, int uriIndex,
                                 String localName, boolean discardWhiteSpace)
                throws FOPException
    {
        XMLEvent ev = getEvent();
        if (discardWhiteSpace) {
            while (ev != null && ev.type == XMLEvent.CHARACTERS
                   && ev.chars.trim().equals("")) {
                namespaces.surrenderEvent(ev);
                ev = getEvent();
            }
        }
        if (ev != null
                && ev.type == eventType
                   && ev.uriIndex == uriIndex
                       && ev.localName.equals(localName)) {
            return ev;
        }
        if (ev == null)
            throw new NoSuchElementException
                        (XMLEvent.eventTypeName(eventType)
                                           + " not found: end of buffer.");
        pushBack(ev);
        return null;
    }
    
    /**
     * Return the next event if it is of the given SAX type, whose URI is
     * matched by the namespaces URI indexed by <code>uriIndex</code>, and
     * whose namespace-specific type matches <code>nsType</code>.
     * If the next element is not of the required type,
     * push it back onto the buffer.
     * @param eventType the SAX event type
     * @param uriIndex of the URI in namespaces
     * @param nsType the namespace-specific type
     * @param discardWhiteSpace - if true, discard any intervening
     * <tt>characters</tt> events which contain only whitespace.
     * @return an event of the required type.  If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @throws FOPException
     */
    public XMLEvent expectSaxUriTypedEvent(
            int eventType, int uriIndex, int nsType,
            boolean discardWhiteSpace)
    throws FOPException {
        XMLEvent ev = getEvent();
        if (discardWhiteSpace) {
            while (ev != null && ev.type == XMLEvent.CHARACTERS
                    && ev.chars.trim().equals("")) {
                namespaces.surrenderEvent(ev);
                ev = getEvent();
            }
        }
        if (ev != null && ev.type == eventType) {
            switch (uriIndex) {
            case Namespaces.DefAttrNSIndex:
                throw new NoSuchElementException
                ("No special types for default attribute namespace");
            case Namespaces.XSLNSpaceIndex:
                // The FO namespace
                if (ev.getFoType() == nsType) {
                    return ev;
                }
                break;
            case Namespaces.FOXNSpaceIndex:
                // The FOX namespace
                if (ev.getFoxType() == nsType) {
                    return ev;
                }
                break;
            case Namespaces.SVGNSpaceIndex:
                // The SVG namespace
                if (ev.getSvgType() == nsType) {
                    return ev;
                }
                break;
            }
        }
        if (ev == null)
            throw new NoSuchElementException
            (XMLEvent.eventTypeName(eventType) + " "
                    + namespaces.getIndexURI(uriIndex)
                    + " type " + nsType + " not found.");
        pushBack(ev);
        return null;
    }
    
    /**
     * Return the next element if it is of the required type and has the
     * required FO type.  If the next
     * element is not of the required type, push it back onto the buffer.
     * @param eventType - the <tt>int</tt> event type.
     * @param foType - the <tt>int</tt> FO type.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return an event of the required type.  If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur.
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectSaxFoEvent
                    (int eventType, int foType, boolean discardWhiteSpace)
                throws FOPException
    {
        return expectSaxUriTypedEvent(
                eventType, Namespaces.XSLNSpaceIndex,
                foType, discardWhiteSpace);
    }

    /**
     * Get the next ENDDOCUMENT event from the buffer.  Discard any other
     * events preceding the ENDDOCUMENT event.
     * @return an ENDDOCUMENT event
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getEndDocument() throws FOPException {
        return getSaxEvent(XMLEvent.ENDDOCUMENT);
    }

    /**
     * Return the next element if it is an ENDDOCUMENT.  If the next
     * element is not of the required type, push it back onto the buffer.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return an ENDDOCUMENT event. If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectEndDocument(boolean discardWhiteSpace)
                throws FOPException
    {
        return expectSaxEvent(XMLEvent.ENDDOCUMENT, discardWhiteSpace);
    }

    /**
     * Get the next STARTELEMENT event from the buffer.  Discard any other
     * events preceding the STARTELEMENT event.
     * @return a STARTELEMENT event
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getStartElement() throws FOPException {
        return getSaxEvent(XMLEvent.STARTELEMENT);
    }

    /**
     * Return the next element if it is a STARTELEMENT.  If the next
     * element is not of the required type, push it back onto the buffer.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return a STARTELEMENT event.  If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectStartElement(boolean discardWhiteSpace)
                throws FOPException
    {
        return expectSaxEvent(XMLEvent.STARTELEMENT, discardWhiteSpace);
    }

    /**
     * Get the next STARTELEMENT event with the given <tt>QName</tt>
     * from the buffer.  Discard any other events preceding the
     * STARTELEMENT event.
     * @param qName a <tt>String</tt> with the <tt>QName</tt> of the
     * required STARTELEMENT
     * @return a STARTELEMENT event
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    /*
    public XMLEvent getStartElement(String qName) throws FOPException
    {
        return getSaxQNameEvent(XMLEvent.STARTELEMENT, qName);
    }
    */

    /**
     * Return the next element if it is a STARTELEMENT with the given
     * <tt>QName</tt>.  If the next
     * element is not of the required type, push it back onto the buffer.
     * @param qName a <tt>String</tt> with the <tt>QName</tt> of the
     * required STARTELEMENT
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return a STARTELEMENT event.  If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    /*
    public XMLEvent expectStartElement
                                (String qName, boolean discardWhiteSpace)
        throws FOPException
    {
        return expectSaxQNameEvent
                        (XMLEvent.STARTELEMENT, qName, discardWhiteSpace);
    }
    */

    /**
     * Get the next STARTELEMENT event with the given URI index and local name
     * from the buffer.  Discard any other events preceding the
     * STARTELEMENT event.
     * @param uriIndex an <tt>int</tt> with the index of the URI of the
     * required URI
     * @param localName a <tt>String</tt> with the local name of the
     * required STARTELEMENT
     * @return a STARTELEMENT event
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getStartElement(int uriIndex, String localName)
        throws FOPException
    {
        return getSaxUriLocalEvent(XMLEvent.STARTELEMENT, uriIndex, localName);
    }

    /**
     * Return the next element if it is a STARTELEMENT with the given
     * URI index and local name.  If the next
     * element is not of the required type, push it back onto the buffer.
     * @param uriIndex an <tt>int</tt> URI index.
     * @param localName a <tt>String</tt> with the local name of the
     * required STARTELEMENT
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return a STARTELEMENT event.  If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectStartElement
                (int uriIndex, String localName, boolean discardWhiteSpace)
        throws FOPException
    {
        return expectSaxUriLocalEvent
            (XMLEvent.STARTELEMENT, uriIndex, localName, discardWhiteSpace);
    }

    /**
     * Get the next STARTELEMENT event with the given URI index and local name
     * from the buffer.  Discard any other events preceding the
     * STARTELEMENT event.
     * @param uriIndex an <tt>int</tt> with the index of the URI of the
     * required URI
     * @param nsType the namespace-dependent event type
     * @return a STARTELEMENT event
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getStartElement(int uriIndex, int nsType)
    throws FOPException
    {
        return getSaxUriTypedEvent(XMLEvent.STARTELEMENT, uriIndex, nsType);
    }

    /**
     * Return the next element if it is a STARTELEMENT with the given
     * URI index and local name.  If the next
     * element is not of the required type, push it back onto the buffer.
     * @param uriIndex an <tt>int</tt> URI index.
     * @param nsType the namespace-dependent event type
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return a STARTELEMENT event.  If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectStartElement(
            int uriIndex, int nsType, boolean discardWhiteSpace)
    throws FOPException
    {
        return expectSaxUriTypedEvent(
                XMLEvent.STARTELEMENT, uriIndex, nsType, discardWhiteSpace);
    }
    
    /**
     * From the buffer get the next STARTELEMENT event from the fo: namespace
     * with the given FO object type.
     *  Discard any other events preceding the
     * STARTELEMENT event.
     * @param foType - the <tt>int</tt> FO object type, as defined in
     * <tt>FObjectNames</tt>.
     * @return a matching STARTELEMENT event.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getStartElement(int foType)
    throws FOPException
    {
        return getSaxFoEvent(XMLEvent.STARTELEMENT, foType);
    }

    /**
     * From the buffer return the next STARTELEMENT event from the fo:
     * namespace with the given FO object type.  If the next
     * element is not of the required type, push it back onto the buffer.
     * @param foType - the <tt>int</tt> FO object type, as defined in
     * <tt>FObjectNames</tt>.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return a matching STARTELEMENT event.  If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectStartElement
    (int foType, boolean discardWhiteSpace)
    throws FOPException
    {
        return expectSaxFoEvent(
                XMLEvent.STARTELEMENT, foType, discardWhiteSpace);
    }
    
    /**
     * Get one of a list of possible STARTELEMENT events.
     * @param list a <tt>LinkedList</tt> containing either <tt>String</tt>s
     * with the <tt>QName</tt>, or <tt>UriLocalName</tt>
     * objects with the URI index and local name of one of the required
     * STARTELEMENT events.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return a STARTELEMENT event
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getStartElement
                                (LinkedList list, boolean discardWhiteSpace)
        throws FOPException
    {
        XMLEvent ev;
        do {
            ev = expectStartElement(list, discardWhiteSpace);
            if (ev != null) return ev;
            // The non-matching event has been pushed back.
            // Get it and discard.  Note that if the first attempt to
            // getEvent() returns null, the expectStartElement() calls
            // return null.
            ev = getEvent();
            namespaces.surrenderEvent(ev);
        } while (ev != null);
        // Exit from this while loop is only by discovery of null event
        throw new NoSuchElementException
                    ("StartElement from list not found.");
    }

    /**
     * Get one of a list of possible STARTELEMENT events.
     * @param list a <tt>LinkedList</tt> containing either
     * <tt>UriLocalName</tt> objects with the URI index and local name,
     * <tt>NameSpaceType</tt> objects with the URI index and local name and
     * a namespace-dependent <tt>int</tt> type, or <tt>Integer</tt>s with
     * the FO type of one of the required STARTELEMENT events.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return a STARTELEMENT event.  If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectStartElement
                                (LinkedList list, boolean discardWhiteSpace)
        throws FOPException
    {
        XMLEvent ev;
        Iterator elements = list.iterator();
        while (elements.hasNext()) {
            Object o = elements.next();
            if (o instanceof UriLocalName) {
                if (o instanceof NameSpaceType) {
                    NameSpaceType nameSpType = (NameSpaceType)o;
                    ev = expectStartElement(
                            nameSpType.uriIndex,
                            nameSpType.nsType,
                            discardWhiteSpace);
                    // Found it!
                    if (ev != null) return ev;
                } else {
                    UriLocalName uriLocalName = (UriLocalName)o;
                    ev = expectStartElement
                    (uriLocalName.uriIndex,
                            uriLocalName.localName,
                            discardWhiteSpace);
                    // Found it!
                    if (ev != null) return ev;
                }
            } else if (o instanceof Integer) {
                ev = expectStartElement(((Integer)o).intValue(),
                                        discardWhiteSpace);
                if (ev != null) return ev;
            } else
                throw new FOPException
                        ("Invalid list elements for getStartElement");
        }
        return null;
    }

    /**
     * Get one of a array of possible STARTELEMENT events.  Scan and discard
     * events until a STARTELEMENT event is found whose URI index and
     * local name matches one of those in the argument
     * <tt>UriLocalName[]</tt> array.
     * @param list an array containing <tt>UriLocalName</tt>
     * objects with the URI index and local name of
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * STARTELEMENT events, one of which is required.
     * @return the next matching STARTELEMENT event from the buffer.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getStartElement
                    (UriLocalName[] list, boolean discardWhiteSpace)
        throws FOPException
    {
        XMLEvent ev;
        do {
            ev = expectStartElement(list, discardWhiteSpace);
            if (ev != null) return ev;
            // The non-matching event has been pushed back.
            // Get it and discard.  Note that if the first attempt to
            // getEvent() returns null, the expectStartElement() calls
            // will throw a NoSuchElementException
            ev = getEvent();
            namespaces.surrenderEvent(ev);
        } while (ev != null);
        // Exit from this while loop is only by discovery of null event
        throw new NoSuchElementException
                    ("StartElement from list not found.");
    }

    /**
     * Expect one of an array of possible STARTELEMENT events.  The next
     * STARTELEMENT must have a URI index and local name which match
     * an element of the argument <tt>UriLocalName[]</tt> list.
     * @param list an <tt>UriLocalName[]</tt> array containing the
     * namespace Uri index and LocalName
     * of possible events, one of which must be the next returned.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return the matching STARTELEMENT event. If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectStartElement
                    (UriLocalName[] list, boolean discardWhiteSpace)
        throws FOPException
    {
        XMLEvent ev;
        for (int i = 0; i < list.length; i++) {
            ev = expectStartElement(list[i].uriIndex,
                                    list[i].localName,
                                    discardWhiteSpace);
            // Found it!
            if (ev != null) return ev;
        }
        return null;
    }

    /**
     * Get one of a array of possible STARTELEMENT events.  Scan and discard
     * events until a STARTELEMENT event is found whose <tt>QName</tt>
     * matches one of those in the argument <tt>String[]</tt> array.
     * @param list a <tt>String[]</tt> array containing <tt>QName</tt>s,
     * one of which is required.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return the next matching STARTELEMENT event from the buffer.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    /*
    public XMLEvent getStartElement(String[] list, boolean discardWhiteSpace)
        throws FOPException
    {
        XMLEvent ev;
        do {
            ev = expectStartElement(list, discardWhiteSpace);
            if (ev != null) return ev;
            // The non-matching event has been pushed back.
            // Get it and discard.  Note that if the first attempt to
            // getEvent() returns null, the expectStartElement() calls
            // will throw a NoSuchElementException
            ev = getEvent();
            namespaces.surrenderEvent(ev);
        } while (ev != null);
        // Exit from this while loop is only by discovery of null event
        throw new NoSuchElementException
                    ("StartElement from array not found.");
    }
    */

    /**
     * Expect one of an array of possible STARTELEMENT events.  The next
     * STARTELEMENT must have a <tt>QName</tt> which matches an element
     * of the argument <tt>String[]</tt> list.
     * @param list a <tt>String[]</tt> array containing <tt>QName</tt>s
     * of possible events, one of which must be the next returned.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return the matching STARTELEMENT event.If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    /*
    public XMLEvent expectStartElement
                                    (String[] list, boolean discardWhiteSpace)
        throws FOPException
    {
        XMLEvent ev;
        for (int i = 0; i < list.length; i++) {
            ev = expectStartElement(list[i], discardWhiteSpace);
            // Found it!
            if (ev != null) return ev;
        }
        return null;
    }
    */

    /**
     * Get one of a array of possible STARTELEMENT events.  Scan and discard
     * events until a STARTELEMENT event is found which is in the fo:
     * namespace and whose FO type matches one of those in the argument
     * <tt>int</tt> array.
     * @param list an <tt>int[]</tt> array containing FO types
     * one of which is required.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return the next matching STARTELEMENT event from the buffer.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getStartElement(int[] list, boolean discardWhiteSpace)
        throws FOPException
    {
        XMLEvent ev;
        do {
            ev = expectStartElement(list, discardWhiteSpace);
            if (ev != null) return ev;
            // The non-matching event has been pushed back.
            // Get it and discard.  Note that if the first attempt to
            // getEvent() returns null, the expectStartElement() calls
            // will throw a NoSuchElementException
            ev = getEvent();
            namespaces.surrenderEvent(ev);
        } while (ev != null);
        // Exit from this while loop is only by discovery of null event
        throw new NoSuchElementException
                    ("StartElement from array not found.");
    }

    /**
     * Expect one of an array of possible STARTELEMENT events.  The next
     * STARTELEMENT must be in the fo: namespace, and must have an FO type
     * which matches one of those in the argument <tt>int[]</tt> list.
     * @param list a <tt>int[]</tt> array containing the FO types
     * of possible events, one of which must be the next returned.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return the matching STARTELEMENT event.If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectStartElement
                                    (int[] list, boolean discardWhiteSpace)
        throws FOPException
    {
        XMLEvent ev;
        for (int i = 0; i < list.length; i++) {
            ev = expectStartElement(list[i], discardWhiteSpace);
            // Found it!
            if (ev != null) return ev;
        }
        return null;
    }

    /**
     * Get one of a <tt>BitSet</tt> of possible STARTELEMENT events.  Scan
     * and discard events until a STARTELEMENT event is found which is in
     * the fo: namespace and whose FO type matches one of those in the
     * argument <tt>BitSet</tt>.
     * @param set a <tt>BitSet</tt> containing FO types one of which is
     * required.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return the next matching STARTELEMENT event from the buffer.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getStartElement(BitSet set, boolean discardWhiteSpace)
        throws FOPException
    {
        XMLEvent ev;
        do {
            try {
                ev = expectStartElement(set, discardWhiteSpace);
                if (ev != null) return ev;
                // The non-matching event has been pushed back.
                // Get it and discard.  Note that if the first attempt to
                // getEvent() returns null, the expectStartElement() calls
                // will throw a NoSuchElementException
                ev = getEvent();
                namespaces.surrenderEvent(ev);
            } catch(UnexpectedStartElementException e) {
                ev = getEvent();
                namespaces.surrenderEvent(ev);
            }
        } while (ev != null);
        // Exit from this while loop is only by discovery of null event
        throw new NoSuchElementException
                    ("StartElement from BitSet not found.");
    }

    /**
     * Expect one of an <tt>BitSet</tt> of possible STARTELEMENT events.
     * The next STARTELEMENT must be in the fo: namespace, and must have an
     * FO type which matches one of those in the argument <tt>BitSet</tt>.
     * <p>TODO:<br>
     * This method should be retro-fitted to list and array versions.
     *
     * @param set a <tt>BitSet</tt> containing the FO types
     * of possible events, one of which must be the next returned.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return the matching STARTELEMENT event.If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectStartElement
                                    (BitSet set, boolean discardWhiteSpace)
        throws FOPException, UnexpectedStartElementException
    {
        XMLEvent ev;
        ev = expectSaxEvent(XMLEvent.STARTELEMENT, discardWhiteSpace);
        if (ev == null) return ev;

        for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(++i)) {
            if (ev.getFoType() == i)
                return ev; // Found it!
        }
        // Not found - push the STARTELEMENT event back and throw an
        // UnexpectedStartElementException
        pushBack(ev);
        throw new UnexpectedStartElementException
                ("Unexpected START element: " + ev.getQName());
    }

    /**
     * Expect that the next element will be a STARTELEMENT for one of the
     * flow objects which are members of %block; from
     * <b>6.2 Formatting Object Content</b>, including out-of-line flow
     * objects which may occur except as descendents of out-of-line formatting
     * objects.  White space is discarded.
     * @return the <tt>XMLEvent found. If any other events are encountered
     * return <tt>null</tt>.
     */
    public XMLEvent expectBlock()
        throws FOPException, UnexpectedStartElementException
    {
        return expectStartElement
                (FObjectSets.blockEntity, XMLEvent.DISCARD_W_SPACE);
    }

    /**
     * Expect that the next element will be a STARTELEMENT for one of the
     * flow objects which are members of %block; from
     * <b>6.2 Formatting Object Content</b>, excluding out-of-line flow
     * objects which may not occur as descendents of out-of-line formatting
     * objects.  White space is discarded.
     * @return the <tt>XMLEvent found. If any other events are encountered
     * return <tt>null</tt>.
     */
    public XMLEvent expectOutOfLineBlock()
        throws FOPException, UnexpectedStartElementException
    {
        return expectStartElement
                (FObjectSets.outOfLineBlockSet, XMLEvent.DISCARD_W_SPACE);
    }

    /**
     * Expect that the next element will be a STARTELEMENT for one of the
     * flow objects which are members of (#PCDATA|%inline;) from
     * <b>6.2 Formatting Object Content</b>, including out-of-line flow
     * objects which may occur except as descendents of out-of-line
     * formatting objects.  White space is retained, and
     * will appear as #PCDATA, i.e, as an instance of FoCharacters.
     * @return the <tt>XMLEvent found. If any other events are encountered
     * return <tt>null</tt>.
     */
    public XMLEvent expectPcdataOrInline()
        throws FOPException, UnexpectedStartElementException
    {
        XMLEvent ev = expectStartElement
                (FObjectSets.normalPcdataInlineSet, XMLEvent.RETAIN_W_SPACE);
        if (ev == null)
            ev = expectCharacters();
        return ev;
    }

    /**
     * Expect that the next element will be a STARTELEMENT for one of the
     * flow objects which are members of (#PCDATA|%inline;) from
     * <b>6.2 Formatting Object Content</b>, excluding out-of-line flow
     * objects which may not occur as descendents of out-of-line formatting
     * objects.  White space is retained, and
     * will appear as #PCDATA, i.e, as an instance of FoCharacters.
     * @return the <tt>XMLEvent found. If any other events are encountered
     * return <tt>null</tt>.
     */
    public XMLEvent expectOutOfLinePcdataOrInline()
        throws FOPException, UnexpectedStartElementException
    {
        XMLEvent ev = expectStartElement
                    (FObjectSets.inlineEntity, XMLEvent.RETAIN_W_SPACE);
        if (ev == null)
            ev = expectCharacters();
        return ev;
    }

    /**
     * Expect that the next element will be a STARTELEMENT for one of the
     * flow objects which are members of (#PCDATA|%inline;|%block;) from
     * <b>6.2 Formatting Object Content</b>, including out-of-line flow
     * objects which may occur except as descendents of out-of-line
     * formatting objects.  White space is retained, and
     * will appear as #PCDATA, i.e, as an instance of FoCharacters.
     * @return the <tt>XMLEvent</tt> found. If any other events are
     * encountered return <tt>null</tt>.
     */
    public XMLEvent expectPcdataOrInlineOrBlock()
        throws FOPException, UnexpectedStartElementException
    {
        XMLEvent ev = expectStartElement
            (FObjectSets.normalPcdataBlockInlineSet, XMLEvent.RETAIN_W_SPACE);
        if (ev == null)
            ev = expectCharacters();
        return ev;
    }

    /**
     * Expect that the next element will be a STARTELEMENT for one of the
     * flow objects which are members of (#PCDATA|%inline;|%block;) from
     * <b>6.2 Formatting Object Content</b>, excluding out-of-line flow
     * objects which may not occur as descendents of out-of-line formatting
     * objects.  White space is retained, and
     * will appear as #PCDATA, i.e, as an instance of FoCharacters.
     * @return the <tt>XMLEvent</tt> found. If any other events are
     * encountered return <tt>null</tt>.
     */
    public XMLEvent expectOutOfLinePcdataOrInlineOrBlock()
        throws FOPException, UnexpectedStartElementException
    {
        XMLEvent ev = expectStartElement
            (FObjectSets.outOfLinePcdataBlockInlineSet,
                                                     XMLEvent.RETAIN_W_SPACE);
        if (ev == null)
            ev = expectCharacters();
        return ev;
    }

    /**
     * Get the next ENDELEMENT event from the buffer.  Discard any other
     * events preceding the ENDELEMENT event.
     * @return an ENDELEMENT event
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getEndElement() throws FOPException {
        return getSaxEvent(XMLEvent.ENDELEMENT);
    }

    /**
     * Return the next element if it is an ENDELEMENT.  If the next
     * element is not of the required type, push it back onto the buffer.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return a matching ENDELEMENT event.  If the next
     * event detected is not of the required type, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if ENDELEMENT is not the next
     * event detected.  The erroneous event is pushed back.
     */
    public XMLEvent expectEndElement(boolean discardWhiteSpace)
                throws FOPException
    {
        return expectSaxEvent(XMLEvent.ENDELEMENT, discardWhiteSpace);
    }

    /**
     * Get the next ENDELEMENT event with the given <tt>QName</tt>
     * from the buffer.  Discard any other events preceding the
     * ENDELEMENT event.
     * @param qName a <tt>String</tt> with the <tt>QName</tt> of the
     * required STARTELEMENT
     * @return an ENDELEMENT event
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getEndElement(String qName) throws FOPException
    {
        return getSaxQNameEvent(XMLEvent.ENDELEMENT, qName);
    }

    /**
     * Return the next element if it is an ENDELEMENT with the given
     * <tt>QName</tt>.  If the next
     * element is not of the required type, push it back onto the buffer.
     * @param qName a <tt>String</tt> with the <tt>QName</tt> of the
     * required ENDELEMENT
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return an ENDELEMENT with the given qname.  If the next
     * event detected is not an ENDELEMENT, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    /*
    public XMLEvent expectEndElement(String qName, boolean discardWhiteSpace)
        throws FOPException
    {
        return expectSaxQNameEvent(XMLEvent.ENDELEMENT, qName, discardWhiteSpace);
    }
    */

    /**
     * Get the next ENDELEMENT event with the given URI index and local name
     * from the buffer.  Discard any other events preceding the
     * ENDELEMENT event.
     * @param uriIndex an <tt>int</tt> with the index of the URI of the
     * required URI
     * @param localName a <tt>String</tt> with the local name of the
     * required ENDELEMENT
     * @return an ENDELEMENT event
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getEndElement(int uriIndex, String localName)
        throws FOPException
    {
        return getSaxUriLocalEvent(XMLEvent.ENDELEMENT, uriIndex, localName);
    }

    /**
     * Return the next element if it is an ENDELEMENT with the given
     * URI index and local name.  If the next
     * element is not of the required type, push it back onto the buffer.
     * @param uriIndex an <tt>int</tt> with the index of the URI of the
     * required URI
     * @param localName a <tt>String</tt> with the local name of the
     * required ENDELEMENT
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return a matching ENDELEMENT event.
     * If the next
     * event detected is not an ENDELEMENT, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectEndElement
                (int uriIndex, String localName, boolean discardWhiteSpace)
        throws FOPException
    {
        return expectSaxUriLocalEvent
                (XMLEvent.ENDELEMENT, uriIndex, localName, discardWhiteSpace);
    }

    /**
     * Get the next ENDELEMENT event with the given Fo type
     * from the buffer.  Discard any other events preceding the
     * ENDELEMENT event.
     * @param foType - the FO type of the required ENDELEMENT
     * @return a matching ENDELEMENT event.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getEndElement(int foType) throws FOPException
    {
        return getSaxFoEvent(XMLEvent.ENDELEMENT, foType);
    }

    /**
     * Return the next element if it is an ENDELEMENT with the given
     * FO type.  If the next
     * element is not of the required type, push it back onto the buffer.
     * @param foType - the FO type of the required ENDELEMENT
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return a matching ENDELEMENT.  If the next
     * event detected is not an ENDELEMENT, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectEndElement(int foType, boolean discardWhiteSpace)
        throws FOPException
    {
        return expectSaxFoEvent
                            (XMLEvent.ENDELEMENT, foType, discardWhiteSpace);
    }

    /**
     * Get the next ENDELEMENT event, with the same URI index and local name
     * as the <tt>XMLEvent</tt> argument, from the buffer.
     * Discard any other events preceding the ENDELEMENT event.
     * @param event an <tt>XMLEvent</tt>.  Only the uriIndex and the
     * localName from the event are used.  It is intended that the XMLEvent
     * returned to the corresponding get/expectStartElement() call be used.
     * @return an ENDELEMENT event
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getEndElement(XMLEvent event) throws FOPException
    {
        int foType;
        if ((foType = event.getFoType()) != FObjectNames.NO_FO)
            return getSaxFoEvent(XMLEvent.ENDELEMENT, foType);
        return getSaxUriLocalEvent
                    (XMLEvent.ENDELEMENT, event.uriIndex, event.localName);
    }

    /**
     * Return the next element if it is an ENDELEMENT with the same
     * URI index and local name as the <tt>XMLEvent argument</tt>.  If the
     * next element is not of the required type, push it back onto the buffer.
     * @param event an <tt>XMLEvent</tt>.  Only the uriIndex and the
     * localName from the event are used.  It is intended that the XMLEvent
     * returned to the corresponding get/expectStartElement() call be used.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return a matching ENDELEMENT event.  If the next
     * event detected is not an ENDELEMENT, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectEndElement
                                (XMLEvent event, boolean discardWhiteSpace)
        throws FOPException
    {
        int foType;
        if ((foType = event.getFoType()) != FObjectNames.NO_FO)
            return expectSaxFoEvent
                    (XMLEvent.ENDELEMENT, foType, discardWhiteSpace);
        return expectSaxUriLocalEvent
                (XMLEvent.ENDELEMENT, event.uriIndex, event.localName,
                                                         discardWhiteSpace);
    }

    /**
     * Get the next ENDELEMENT event, with the same URI index and local name
     * as the <tt>XMLEvent</tt> argument, from the buffer.
     * Discard any other events preceding the ENDELEMENT event.
     * @param discardEvent the argument event may be discarded.
     * @param event an <tt>XMLEvent</tt>.  Only the uriIndex and the
     * localName from the event are used.  It is intended that the XMLEvent
     * returned to the corresponding get/expectStartElement() call be used.
     * @return an ENDELEMENT event
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getEndElement(boolean discardEvent, XMLEvent event)
        throws FOPException
    {
        XMLEvent ev;
        int foType;
        if ((foType = event.getFoType()) != FObjectNames.NO_FO)
            ev = getSaxFoEvent(XMLEvent.ENDELEMENT, foType);
        else
            ev = getSaxUriLocalEvent
                    (XMLEvent.ENDELEMENT, event.uriIndex, event.localName);
        if (discardEvent) {
            //System.out.println("discardEvent");
            namespaces.surrenderEvent(event);
        }
        return ev;
    }

    /**
     * Return the next element if it is an ENDELEMENT with the same
     * URI index and local name as the <tt>XMLEvent argument</tt>.  If the
     * next element is not of the required type, push it back onto the buffer.
     * @param discardEvent the argument event may be discarded.
     * @param event an <tt>XMLEvent</tt>.  Only the uriIndex and the
     * localName from the event are used.  It is intended that the XMLEvent
     * returned to the corresponding get/expectStartElement() call be used.
     * @param discardWhiteSpace - if true, discard any <tt>characters</tt>
     * events which contain only whitespace.
     * @return a matching ENDELEMENT event.  If the next
     * event detected is not an ENDELEMENT, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectEndElement
        (boolean discardEvent, XMLEvent event, boolean discardWhiteSpace)
        throws FOPException
    {
        XMLEvent ev;
        int foType;
        if ((foType = event.getFoType()) != FObjectNames.NO_FO)
            ev = expectSaxFoEvent
                    (XMLEvent.ENDELEMENT, foType, discardWhiteSpace);
        else
            ev = expectSaxUriLocalEvent
                (XMLEvent.ENDELEMENT, event.uriIndex, event.localName,
                                                         discardWhiteSpace);
        if (discardEvent)
            namespaces.surrenderEvent(event);
        return ev;
    }

    /**
     * @return a CHARACTERS event
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if the event is not found
     */
    public XMLEvent getCharacters() throws FOPException {
        XMLEvent ev = getEvent();
        while (ev != null && ev.type != XMLEvent.CHARACTERS) {
            namespaces.surrenderEvent(ev);
            ev = getEvent();
        }
        if (ev == null) {
            throw new NoSuchElementException("Characters not found.");
        }
        return ev;
    }

    /**
     * @return a CHARACTERS event.  If the next event detected is not
     * a CHARACTERS event, <tt>null</tt> is returned.
     * The erroneous event is pushed back.
     * @exception FOPException if buffer errors or interrupts occur
     * @exception NoSuchElementException if end of buffer detected.
     */
    public XMLEvent expectCharacters() throws FOPException {
        XMLEvent ev = getEvent();
        if (ev != null && ev.type == XMLEvent.CHARACTERS) {
            return ev;
        }
        pushBack(ev);
        return null;
    }

}
