/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.viewer;

import java.io.*;
import org.apache.fop.messaging.MessageHandler;
import java.util.*;

/**
 * Erweitert Hashtable um die Methode load.
 * Die Zeilen der Textdatei, die mit # oder ! anfangen sind Kommentarzeilen.
 * Eine g�ltige Zeile ist entweder eine Kommentarzeile oder eine Zeile mit dem
 * Gleichheitszeichen "in der Mitte".
 * Die Klasse LoadableProperties l�sst im Gegensatz zu der Klasse Properties die
 * Schl�sselwerte mit Leerzeichen zu.
 *
 * @version 02.12.99
 * @author Stanislav.Gorkhover@jCatalog.com
 *
 */
public class LoadableProperties extends Hashtable {

    public LoadableProperties() {
        super();
    }


    public void load(InputStream inStream) throws IOException {

        BufferedReader in = new BufferedReader(new InputStreamReader(inStream,
                "UTF-8"));

        String aKey;
        String aValue;
        int index;
        String line = getNextLine(in);
        while (line != null) {
            line = line.trim();
            if (isValid(line)) {
                index = line.indexOf("=");
                aKey = line.substring(0, index);
                aValue = line.substring(index + 1);
                put(aKey, aValue);
            }
            line = getNextLine(in);
        }
    }


    private boolean isValid(String str) {
        if (str == null)
            return false;
        if (str.length() > 0) {
            if (str.startsWith("#") || str.startsWith("!")) {
                return false;
            }
        } else {
            return false;
        }

        int index = str.indexOf("=");
        if (index > 0 && str.length() > index) {
            return true;
        } else {
            MessageHandler.logln(getClass().getName()
                                 + ": load(): invalid line " + str + "."
                                 + " Character '=' missed.");
            return false;
        }
    }

    private String getNextLine(BufferedReader br) {
        try {
            return br.readLine();
        } catch (Exception e) {
            return null;
        }

    }


}
