/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.pdf;

// Java
import java.io.UnsupportedEncodingException;

/**
 * class representing a font descriptor.
 *
 * Font descriptors are specified on page 222 and onwards of the PDF 1.3 spec.
 */
public class PDFFontDescriptor extends PDFObject {

    // Required fields
    protected int ascent;
    protected int capHeight;
    protected int descent;
    protected int flags;
    protected PDFRectangle fontBBox;
    protected String basefont;    // PDF-spec: FontName
    protected int italicAngle;
    protected int stemV;
    // Optional fields
    protected int stemH = 0;
    protected int xHeight = 0;
    protected int leading = 0;
    protected int avgWidth = 0;
    protected int maxWidth = 0;
    protected int missingWidth = 0;
    protected PDFStream fontfile;
    // protected String charSet = null;

    protected byte subtype;

    /**
     * create the /FontDescriptor object
     *
     * @param number the object's number
     * @param ascent the maximum height above the baseline
     * @param descent the maximum depth below the baseline
     * @param capHeight height of the capital letters
     * @param flags various characteristics of the font
     * @param fontBBox the bounding box for the described font
     * @param basefont the base font name
     * @param italicAngle the angle of the vertical dominant strokes
     * @param stemV the width of the dominant vertical stems of glyphs
     */
    public PDFFontDescriptor(int number, String basefont, int ascent,
                             int descent, int capHeight, int flags,
                             PDFRectangle fontBBox, int italicAngle,
                             int stemV) {

        /* generic creation of PDF object */
        super(number);

        /* set fields using paramaters */
        this.basefont = basefont;
        this.ascent = ascent;
        this.descent = descent;
        this.capHeight = capHeight;
        this.flags = flags;
        this.fontBBox = fontBBox;
        this.italicAngle = italicAngle;
        this.stemV = stemV;
    }

    /**
     * set the optional metrics
     */
    public void setMetrics(int avgWidth, int maxWidth, int missingWidth,
                           int leading, int stemH, int xHeight) {
        this.avgWidth = avgWidth;
        this.maxWidth = maxWidth;
        this.missingWidth = missingWidth;
        this.leading = leading;
        this.stemH = stemH;
        this.xHeight = xHeight;
    }

    /**
     * set the optional font file stream
     *
     * @param subtype the font type defined in the font stream
     * @param fontfile the stream containing an embedded font
     */
    public void setFontFile(byte subtype, PDFStream fontfile) {
        this.subtype = subtype;
        this.fontfile = fontfile;
    }

    // public void setCharSet(){}//for subset fonts

    /**
     * produce the PDF representation for the object
     *
     * @return the PDF
     */
    public byte[] toPDF() {
        StringBuffer p = new StringBuffer(this.number + " " + this.generation
                                          + " obj\n<< /Type /FontDescriptor"
                                          + "\n/FontName /" + this.basefont);

        p.append("\n/FontBBox ");
        p.append(fontBBox.toPDFString());
        p.append("\n/Flags ");
        p.append(flags);
        p.append("\n/CapHeight ");
        p.append(capHeight);
        p.append("\n/Ascent ");
        p.append(ascent);
        p.append("\n/Descent ");
        p.append(descent);
        p.append("\n/ItalicAngle ");
        p.append(italicAngle);
        p.append("\n/StemV ");
        p.append(stemV);
        // optional fields
        if (stemH != 0) {
            p.append("\n/StemH ");
            p.append(stemH);
        }
        if (xHeight != 0) {
            p.append("\n/XHeight ");
            p.append(xHeight);
        }
        if (avgWidth != 0) {
            p.append("\n/AvgWidth ");
            p.append(avgWidth);
        }
        if (maxWidth != 0) {
            p.append("\n/MaxWidth ");
            p.append(maxWidth);
        }
        if (missingWidth != 0) {
            p.append("\n/MissingWidth ");
            p.append(missingWidth);
        }
        if (leading != 0) {
            p.append("\n/Leading ");
            p.append(leading);
        }
        if (fontfile != null) {
            switch (subtype) {
            case PDFFont.TYPE1:
                p.append("\n/FontFile ");
                break;
            case PDFFont.TRUETYPE:
                p.append("\n/FontFile2 ");
                break;
            case PDFFont.TYPE0:
                p.append("\n/FontFile2 ");
                break;
            default:
                p.append("\n/FontFile2 ");
            }
            p.append(fontfile.referencePDF());
        }
        // charSet for subset fonts // not yet implemented
        // CID optional field
        fillInPDF(p);
        p.append("\n >>\nendobj\n");

        try {
            return p.toString().getBytes(PDFDocument.ENCODING);
        } catch (UnsupportedEncodingException ue) {
            return p.toString().getBytes();
        }       
    }

    /**
     * fill in the specifics for the font's descriptor.
     *
     * the given buffer already contains the fields common to all descriptors.
     *
     * @param begin the buffer to be completed with the specific fields
     */
    protected void fillInPDF(StringBuffer begin) {}

}
