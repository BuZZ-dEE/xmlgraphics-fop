package org.apache.fop.fo;

import org.apache.fop.datastructs.Tree;
import org.apache.fop.datatypes.Ints;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.datatypes.CountryLanguageScript;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.xml.FoXMLEvent;
import org.apache.fop.xml.XMLNamespaces;
import org.apache.fop.xml.SyncedFoXmlEventsBuffer;
import org.apache.fop.apps.Driver;
import org.apache.fop.apps.FOPException;
import org.apache.fop.configuration.Configuration;
import org.apache.fop.fo.PropertyConsts;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.expr.PropertyParser;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.util.HashMap;
import java.util.ArrayList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 *
 * Created: Thu Aug  2 20:29:57 2001
 *
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 * @version $Id$
 */
/**
 * <tt>FOTree</tt> is the class that generates and maintains the FO Tree.
 * It runs as a thread, so it implements the <tt>run()</tt> method.
 */

public class FOTree extends Tree implements Runnable {

    private static final String tag = "$Name$";
    private static final String revision = "$Revision$";

    /**
     * The buffer from which the <tt>XMLEvent</tt>s from the parser will
     * be read.  <tt>protected</tt> so that FONode can access it.
     */
    protected SyncedFoXmlEventsBuffer xmlevents;
    private Thread parserThread;
    private boolean errorDump;

    /**
     * The <tt>PropertyParser</tt> which will be used by the FO tree
     * builder.
     */
    protected PropertyParser exprParser;

    /**
     * @param xmlevents the buffer from which <tt>XMLEvent</tt>s from the
     * parser are read.
     */
    public FOTree(SyncedFoXmlEventsBuffer xmlevents)
        throws PropertyException
    {
        super();
        errorDump = Configuration.getBooleanValue("debugMode").booleanValue();
        this.xmlevents = xmlevents;
        exprParser = new PropertyParser(this);

        // Initialize the FontSize first.  Any lengths defined in ems must
        // be resolved relative to the current font size.  This may happen
        // during setup of initial values.
        // Set the initial value
        PropertyValue prop =
                PropertyConsts.pconsts.getInitialValue(PropNames.FONT_SIZE);
        if ( ! (prop instanceof Numeric) || ! ((Numeric)prop).isLength())
            throw new PropertyException("Initial font-size is not a Length");


        /*
        for (int i = 1; i <= PropNames.LAST_PROPERTY_INDEX; i++) {
            if (i == PropNames.FONT_SIZE) continue;
            // Set up the initial values for each property
            PropertyConsts.pconsts.getInitialValue(i);
            //System.out.println("....Setting initial value for " + i);
        }
        */

    }

    /**
     * Parser thread notifies itself to FO tree builder by this call.  The
     * purpose of this notification is to allow the FO tree builder thread
     * to attempt to interrupt the parser thread when the builder
     * terminates.
     * @param parserThread - the <tt>Thread</tt> object of the parser thread.
     */
    public void setParserThread(Thread parserThread) {
        this.parserThread = parserThread;
    }

    /**
     * Get the <i>xmlevents</i> buffer through which descendents can access
     * parser events.
     * @return <i>xmlevents</i>.
     */
    public SyncedFoXmlEventsBuffer getXmlevents() {
        return xmlevents;
    }

    /**
     * The <tt>run</tt> method() invoked by the call of <tt>start</tt>
     * on the thread in which runs off FOTree.
     */
    public void run() {
        FoRoot foRoot;
        FoXMLEvent event;
        try {
            // Let the parser look after STARTDOCUMENT and the correct
            // positioning of the root element
            event = xmlevents.getStartElement(FObjectNames.ROOT);
            foRoot = new FoRoot(this, event);
            foRoot.buildFoTree();
            System.out.println("Back from buildFoTree");
            // Clean up the fo:root event
            xmlevents.getEndElement(event);
            // Get the end of document
            xmlevents.getEndDocument();
        } catch (Exception e) {
            if (errorDump) Driver.dumpError(e);
            if (parserThread != null) {
                try {
                    parserThread.interrupt();
                } catch (Exception ex) {} // Ignore
            }
            // Now propagate a Runtime exception
            throw new RuntimeException(e);
        }
    }

}// FOTree
