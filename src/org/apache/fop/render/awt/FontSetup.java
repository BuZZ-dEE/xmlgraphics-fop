/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.render.awt;

// FOP
import org.apache.fop.messaging.MessageHandler;
import org.apache.fop.layout.FontInfo;
import org.apache.fop.layout.FontDescriptor;
import org.apache.fop.configuration.Configuration;
import org.apache.fop.configuration.FontTriplet;
import org.apache.fop.apps.FOPException;


// Java
import java.util.Enumeration;
import java.util.Hashtable;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Vector;

/**
 * sets up the AWT fonts. It is similar to
 * org.apache.fop.render.pdf.FontSetup.
 * Assigns the font (with metrics) to internal names like "F1" and
 * assigns family-style-weight triplets to the fonts
 */
public class FontSetup {

    /** Regular */
    private static int normal = java.awt.Font.PLAIN;
    /** Bold */
    private static int bold = java.awt.Font.BOLD;
    /** Italic */
    private static int italic = java.awt.Font.ITALIC;
    /** BoldItalic */
    private static int bolditalic = java.awt.Font.BOLD + java.awt.Font.ITALIC;

    /**
     * sets up the font info object.
     *
     * adds metrics for basic fonts and useful family-style-weight
     * triplets for lookup
     *
     * @param fontInfo the font info object to set up
     * @param parent needed, since a live AWT component is needed
     * to get a valid java.awt.FontMetrics object
     */
    public static void setup(FontInfo fontInfo, Graphics2D graphics)
        throws FOPException {

        FontMetricsMapper metric;

        MessageHandler.logln("setting up fonts");

        /*
         * available java fonts are:
         * Serif - bold, normal, italic, bold-italic
         * SansSerif - bold, normal, italic, bold-italic
         * MonoSpaced - bold, normal, italic, bold-italic
         */

        metric = new FontMetricsMapper("SansSerif", normal, graphics);
        // --> goes to  F1
        fontInfo.addMetrics("F1", metric);
        metric = new FontMetricsMapper("SansSerif", italic, graphics);
        // --> goes to  F2
        fontInfo.addMetrics("F2", metric);
        metric = new FontMetricsMapper("SansSerif", bold, graphics);
        // --> goes to  F3
        fontInfo.addMetrics("F3", metric);
        metric = new FontMetricsMapper("SansSerif", bolditalic, graphics);
        // --> goes to  F4
        fontInfo.addMetrics("F4", metric);


        metric = new FontMetricsMapper("Serif", normal, graphics);
        // --> goes to  F5
        fontInfo.addMetrics("F5", metric);
        metric = new FontMetricsMapper("Serif", italic, graphics);
        // --> goes to  F6
        fontInfo.addMetrics("F6", metric);
        metric = new FontMetricsMapper("Serif", bold, graphics);
        // --> goes to  F7
        fontInfo.addMetrics("F7", metric);
        metric = new FontMetricsMapper("Serif", bolditalic, graphics);
        // --> goes to  F8
        fontInfo.addMetrics("F8", metric);

        metric = new FontMetricsMapper("MonoSpaced", normal, graphics);
        // --> goes to  F9
        fontInfo.addMetrics("F9", metric);
        metric = new FontMetricsMapper("MonoSpaced", italic, graphics);
        // --> goes to  F10
        fontInfo.addMetrics("F10", metric);
        metric = new FontMetricsMapper("MonoSpaced", bold, graphics);
        // --> goes to  F11
        fontInfo.addMetrics("F11", metric);
        metric = new FontMetricsMapper("MonoSpaced", bolditalic, graphics);
        // --> goes to  F12
        fontInfo.addMetrics("F12", metric);

        metric = new FontMetricsMapper("Symbol", bolditalic, graphics);
        // --> goes to  F13 and F14
        fontInfo.addMetrics("F13", metric);
        fontInfo.addMetrics("F14", metric);

        // Custom type 1 fonts step 1/2
        // fontInfo.addMetrics("F15", new OMEP());
        // fontInfo.addMetrics("F16", new GaramondLightCondensed());
        // fontInfo.addMetrics("F17", new BauerBodoniBoldItalic());

        /* any is treated as serif */
        fontInfo.addFontProperties("F5", "any", "normal", "normal");
        fontInfo.addFontProperties("F6", "any", "italic", "normal");
        fontInfo.addFontProperties("F6", "any", "oblique", "normal");
        fontInfo.addFontProperties("F7", "any", "normal", "bold");
        fontInfo.addFontProperties("F8", "any", "italic", "bold");
        fontInfo.addFontProperties("F8", "any", "oblique", "bold");

        fontInfo.addFontProperties("F1", "sans-serif", "normal", "normal");
        fontInfo.addFontProperties("F2", "sans-serif", "oblique", "normal");
        fontInfo.addFontProperties("F2", "sans-serif", "italic", "normal");
        fontInfo.addFontProperties("F3", "sans-serif", "normal", "bold");
        fontInfo.addFontProperties("F4", "sans-serif", "oblique", "bold");
        fontInfo.addFontProperties("F4", "sans-serif", "italic", "bold");
        fontInfo.addFontProperties("F5", "serif", "normal", "normal");
        fontInfo.addFontProperties("F6", "serif", "oblique", "normal");
        fontInfo.addFontProperties("F6", "serif", "italic", "normal");
        fontInfo.addFontProperties("F7", "serif", "normal", "bold");
        fontInfo.addFontProperties("F8", "serif", "oblique", "bold");
        fontInfo.addFontProperties("F8", "serif", "italic", "bold");
        fontInfo.addFontProperties("F9", "monospace", "normal", "normal");
        fontInfo.addFontProperties("F10", "monospace", "oblique", "normal");
        fontInfo.addFontProperties("F10", "monospace", "italic", "normal");
        fontInfo.addFontProperties("F11", "monospace", "normal", "bold");
        fontInfo.addFontProperties("F12", "monospace", "oblique", "bold");
        fontInfo.addFontProperties("F12", "monospace", "italic", "bold");

        fontInfo.addFontProperties("F1", "Helvetica", "normal", "normal");
        fontInfo.addFontProperties("F2", "Helvetica", "oblique", "normal");
        fontInfo.addFontProperties("F2", "Helvetica", "italic", "normal");
        fontInfo.addFontProperties("F3", "Helvetica", "normal", "bold");
        fontInfo.addFontProperties("F4", "Helvetica", "oblique", "bold");
        fontInfo.addFontProperties("F4", "Helvetica", "italic", "bold");
        fontInfo.addFontProperties("F5", "Times", "normal", "normal");
        fontInfo.addFontProperties("F6", "Times", "oblique", "normal");
        fontInfo.addFontProperties("F6", "Times", "italic", "normal");
        fontInfo.addFontProperties("F7", "Times", "normal", "bold");
        fontInfo.addFontProperties("F8", "Times", "oblique", "bold");
        fontInfo.addFontProperties("F8", "Times", "italic", "bold");
        fontInfo.addFontProperties("F9", "Courier", "normal", "normal");
        fontInfo.addFontProperties("F10", "Courier", "oblique", "normal");
        fontInfo.addFontProperties("F10", "Courier", "italic", "normal");
        fontInfo.addFontProperties("F11", "Courier", "normal", "bold");
        fontInfo.addFontProperties("F12", "Courier", "oblique", "bold");
        fontInfo.addFontProperties("F12", "Courier", "italic", "bold");
        fontInfo.addFontProperties("F13", "Symbol", "normal", "normal");
        fontInfo.addFontProperties("F14", "ZapfDingbats", "normal", "normal");

        // Custom type 1 fonts step 2/2
        // fontInfo.addFontProperties("F15", "OMEP", "normal", "normal");
        // fontInfo.addFontProperties("F16", "Garamond-LightCondensed", "normal", "normal");
        // fontInfo.addFontProperties("F17", "BauerBodoni", "italic", "bold");

        /* for compatibility with PassiveTex */
        fontInfo.addFontProperties("F5", "Times-Roman", "normal", "normal");
        fontInfo.addFontProperties("F6", "Times-Roman", "oblique", "normal");
        fontInfo.addFontProperties("F6", "Times-Roman", "italic", "normal");
        fontInfo.addFontProperties("F7", "Times-Roman", "normal", "bold");
        fontInfo.addFontProperties("F8", "Times-Roman", "oblique", "bold");
        fontInfo.addFontProperties("F8", "Times-Roman", "italic", "bold");
        fontInfo.addFontProperties("F5", "Times Roman", "normal", "normal");
        fontInfo.addFontProperties("F6", "Times Roman", "oblique", "normal");
        fontInfo.addFontProperties("F6", "Times Roman", "italic", "normal");
        fontInfo.addFontProperties("F7", "Times Roman", "normal", "bold");
        fontInfo.addFontProperties("F8", "Times Roman", "oblique", "bold");
        fontInfo.addFontProperties("F8", "Times Roman", "italic", "bold");
        fontInfo.addFontProperties("F9", "Computer-Modern-Typewriter",
                                   "normal", "normal");

        /* Add configured fonts */
        addConfiguredFonts(fontInfo, 15, graphics);
    }

    /**
     * Add fonts from configuration file starting with
     * internalnames F<num>
     */
    public static void addConfiguredFonts(
                             FontInfo fontInfo, int num, Graphics2D graphics)
                             throws FOPException {
        FontMetricsMapper metric;
        String internalName = null;

        Vector fontInfos = Configuration.getFonts();
        if (fontInfos == null)
            return;

        for (Enumeration e = fontInfos.elements(); e.hasMoreElements(); ) {
            org.apache.fop.configuration.FontInfo configFontInfo =
                (org.apache.fop.configuration.FontInfo)e.nextElement();

            try {
                String metricsFile = configFontInfo.getMetricsFile();
                if (metricsFile != null) {
                    internalName = "F" + num;
                    num++;
                    
                    Vector triplets = configFontInfo.getFontTriplets();
                    for (Enumeration t = triplets.elements();
                            t.hasMoreElements(); ) {
                        FontTriplet triplet = (FontTriplet)t.nextElement();
                        boolean embed = configFontInfo.getEmbedFile() != null;
                        // if embed font is not specified, use system "Dialog"
                        // logical font name for each Locale.
                        String family = embed ? triplet.getName() : "Dialog";
                        metric = new FontMetricsMapper(family,
                                                       getFontMetrics(triplet),
                                                       graphics);
                        if (embed)
                            metric.setEmbedFont(configFontInfo.getEmbedFile());
                        fontInfo.addMetrics(internalName, metric);
                        fontInfo.addFontProperties(internalName,
                                                   triplet.getName(),
                                                   triplet.getStyle(),
                                                   triplet.getWeight());
                    }
                }
            } catch (Exception ex) {
                MessageHandler.error("Failed to read font metrics file "
                                     + configFontInfo.getMetricsFile()
                                     + " : " + ex.getMessage());
            }
        }
    }

    /**
     * Return configured font metrics value.
     */
    private static int getFontMetrics(FontTriplet triplet) {
        boolean isBold = ("bold".equalsIgnoreCase(triplet.getWeight()));
        boolean isItalic = ("italic".equalsIgnoreCase(triplet.getStyle()));
        if (isBold && isItalic) {
            return bolditalic;
        } else if (isBold) {
            return bold;
        } else if (isItalic) {
            return italic;
        }
        return normal;
    }
}










