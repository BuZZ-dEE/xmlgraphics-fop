/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.viewer;

import java.util.*;
import org.apache.fop.messaging.MessageHandler;
import java.io.*;


/**
 * Die Klasse <code>SecureResourceBundle</code> ist ein Resourceundle, das im Falle eines fehlenden
 * Eintrages keinen Absturz verursacht, sondern die Meldung
 * <strong>Key <i>key</i> not found</strong> zur�ckgibt.
 *
 * @author Stanislav.Gorkhover@jCatalog.com
 * @version 1.0 18.03.1999
 */
public class SecureResourceBundle extends ResourceBundle
    implements Translator {

    // Fehlende keys mit einer Meldung zur�ckgeben.
    private boolean isMissingEmphasized = false;

    // private Properties lookup = new Properties();
    private LoadableProperties lookup = new LoadableProperties();

    private boolean isSourceFound = true;

    public void setMissingEmphasized(boolean flag) {
        isMissingEmphasized = flag;
    }

    /**
     * Kreiert ein ResourceBundle mit der Quelle in <strong>in</strong>.
     */

    public SecureResourceBundle(InputStream in) {
        try {
            lookup.load(in);
        } catch (Exception ex) {
            MessageHandler.logln("Exception catched: " + ex.getMessage());
            isSourceFound = false;
        }
    }



    public Enumeration getKeys() {
        return lookup.keys();
    }



    /**
     * H�ndelt den abgefragten Key, liefert entweder den zugeh�rigen Wert oder eine Meldung.
     * Die <strong>null</strong> wird nie zur�ckgegeben.
     * Schreibt die fehlenden Suchschl�ssel in die Protokoll-Datei.
     * @return <code>Object</code><UL>
     * <LI>den zu dem Suchschl�ssel <strong>key</strong> gefundenen Wert, falls vorhanden, <br>
     * <LI>Meldung <strong>Key <i>key</i> not found</strong>, falls der Suchschl�ssel fehlt
     * und die Eigenschaft "jCatalog.DevelopmentStartModus" in der ini-Datei aus true gesetzt ist.
     * <LI>Meldung <strong>Key is null</strong>, falls der Suchschl�ssel <code>null</code> ist.
     * </UL>
     *
     */
    public Object handleGetObject(String key) {

        if (key == null)
            return "Key is null";

        Object obj = lookup.get(key);
        if (obj != null)
            return obj;
        else {
            if (isMissingEmphasized) {
                MessageHandler.logln(getClass().getName() + ": missing key: "
                                     + key);
                return getMissedRepresentation(key.toString());
            } else
                return key.toString();
        }
    }

    /**
     * Stellt fest, ob es den Key gibt.
     */
    public boolean contains(String key) {
        return (key == null || lookup.get(key) == null) ? false : true;
    }


    private String getMissedRepresentation(String str) {
        return "<!" + str + "!>";
    }

    public boolean isSourceFound() {
        return isSourceFound;
    }

}
