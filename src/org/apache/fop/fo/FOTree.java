package org.apache.fop.fo;

import org.apache.fop.datastructs.Tree;
import org.apache.fop.datatypes.Ints;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.xml.XMLEvent;
import org.apache.fop.xml.XMLNamespaces;
import org.apache.fop.xml.SyncedXmlEventsBuffer;
import org.apache.fop.apps.Driver;
import org.apache.fop.apps.FOPException;
import org.apache.fop.configuration.Configuration;
import org.apache.fop.fo.Properties;
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
 * FOTree.java
 *
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
    SyncedXmlEventsBuffer xmlevents;
    private Thread parserThread;
    private boolean errorDump;

    /**
     * The <tt>PropertyParser</tt> which will be used by the FO tree
     * builder.
     */
    protected PropertyParser exprParser;

    /**
     * Args array for refineParsingMethods[].invoke() calls
     */
    Object[] args = new Object[2];

    protected PropertyValue[] initialValues
                    = new PropertyValue[PropNames.LAST_PROPERTY_INDEX + 1];

    /**
     * @param xmlevents the buffer from which <tt>XMLEvent</tt>s from the
     * parser are read.
     */
    public FOTree(SyncedXmlEventsBuffer xmlevents)
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
                    PropertyConsts.getInitialValue(PropNames.FONT_SIZE);
        if ( ! (prop instanceof Numeric) || ! ((Numeric)prop).isLength())
            throw new PropertyException("Initial font-size is not a Length");
        initialValues[PropNames.FONT_SIZE] = prop;


        for (int i = 1; i <= PropNames.LAST_PROPERTY_INDEX; i++) {
            if (i == PropNames.FONT_SIZE) continue;
            // Set up the initial values for each property
            prop = PropertyConsts.getInitialValue(i);
            //System.out.println("....Setting initial value: "
            //                 + i + ((prop == null) ? " NULL" : " notNULL"));
            initialValues[i] = prop;
        }

    }

    /**
     * Get the initial value <tt>PropertyValue</tt> for a given property.
     * Note that this is a <b>raw</b> value; if it is
     * an unresolved percentage that value will be returned.
     * @param index - the property index.
     * @return a <tt>PropertyValue</tt> containing the property
     * value element at the bottom of the stack for the indexed property.
     */
    public PropertyValue getInitialValue(int index)
            throws PropertyException
    {
        return initialValues[index];
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
     * The <tt>run</tt> method() invoked by the call of <tt>start</tt>
     * on the thread in which runs off FOTree.
     */
    public void run() {
        FoRoot foRoot;
        XMLEvent event;
        try {
            // Dummy only - check the language and country setup
            System.out.println((String)Configuration.getHashMapEntry
                               ("countriesMap","AU"));
            System.out.println((String)Configuration.getHashMapEntry
                               ("languagesMap","EN"));
            System.out.println((String)Configuration.getHashMapEntry
                               ("scriptsMap","Pk"));
            // Let the parser look after STARTDOCUMENT and the correct
            // positioning of the root element
            event = xmlevents.getStartElement
                                    (XMLNamespaces.XSLNSpaceIndex, "root");
            if (event != null) {
                System.out.println("FOTree:" + event);
            }
            foRoot = new FoRoot(this, event);
            foRoot.buildFoTree();
            System.out.println("Back from buildFoTree");
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
