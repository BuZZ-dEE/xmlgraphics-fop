/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.apps;

// Avalon
import org.apache.avalon.framework.logger.Logger;

// SAX
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;

// Java
import java.io.*;
import java.net.URL;

/**
 * abstract super class
 * Creates a SAX Parser (defaulting to Xerces).
 *
 */
public abstract class Starter {

    Options options;
    InputHandler inputHandler;
    protected Logger log;

    public Starter() throws FOPException {
        options = new Options();
    }

    public void setLogger(Logger handler) {
        log = handler;
    }

    public void setInputHandler(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    abstract public void run() throws FOPException;

}
