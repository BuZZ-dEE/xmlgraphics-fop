/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.layout;

import java.util.Hashtable;
import org.apache.fop.messaging.MessageHandler;
import java.util.Enumeration;

import org.apache.fop.apps.FOPException;

public class FontInfo {
    Hashtable usedFonts;
    Hashtable triplets;    // look up a font-triplet to find a font-name
    Hashtable fonts;    // look up a font-name to get a font (that implements FontMetric at least)

    public FontInfo() {
        this.triplets = new Hashtable();
        this.fonts = new Hashtable();
        this.usedFonts = new Hashtable();
    }

    public void addFontProperties(String name, String family, String style,
                                  String weight) {
        /*
         * add the given family, style and weight as a lookup for the font
         * with the given name
         */

        String key = createFontKey(family, style, weight);
        this.triplets.put(key, name);
    }

    public void addMetrics(String name, FontMetric metrics) {
        // add the given metrics as a font with the given name

        this.fonts.put(name, metrics);
    }

    public String fontLookup(String family, String style,
                             String weight) throws FOPException {
        return fontLookup(createFontKey(family, style, weight));
    }

    public String fontLookup(String key) throws FOPException {

        String f = (String)this.triplets.get(key);
        if (f == null) {
            int i = key.indexOf(',');
            String s = "any" + key.substring(i);
            f = (String)this.triplets.get(s);
            if (f == null) {
                f = (String)this.triplets.get("any,normal,normal");
                if (f == null) {
                    throw new FOPException("no default font defined by OutputConverter");
                }
                MessageHandler.errorln("defaulted font to any,normal,normal");
            }
            MessageHandler.errorln("unknown font " + key
                                   + " so defaulted font to any");
        }

        usedFonts.put(f, fonts.get(f));
        return f;
    }

    public boolean hasFont(String family, String style, String weight) {
        String key = createFontKey(family, style, weight);
        return this.triplets.get(key) != null;
    }

    public boolean hasFont(String key) {
        return this.triplets.get(key) != null;
    }

    /**
     * Creates a key from the given strings
     */
    public static String createFontKey(String family, String style,
                                       String weight) {
        int i;

        try {
            i = Integer.parseInt(weight);
        } catch (NumberFormatException e) {
            i = 0;
        }

        if (i > 600)
            weight = "bold";
        else if (i > 0)
            weight = "normal";

        return family + "," + style + "," + weight;
    }

    public Hashtable getFonts() {
        return this.fonts;
    }

    public Hashtable getUsedFonts() {
        return this.usedFonts;
    }

    public FontMetric getMetricsFor(String fontName) throws FOPException {
        usedFonts.put(fontName, fonts.get(fontName));
        return (FontMetric)fonts.get(fontName);
    }

}
