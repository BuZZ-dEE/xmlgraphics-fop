/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.render.ps;

// FOP
import org.apache.fop.svg.SVGArea;
import org.apache.fop.render.AbstractRenderer;
import org.apache.fop.render.Renderer;
import org.apache.fop.image.ImageArea;
import org.apache.fop.image.FopImage;
import org.apache.fop.image.JpegImage;
import org.apache.fop.image.FopImageException;
import org.apache.fop.layout.*;
import org.apache.fop.layout.inline.*;
import org.apache.fop.datatypes.*;
import org.apache.fop.fo.properties.*;
import org.apache.fop.render.pdf.Font;
import org.apache.fop.image.*;

import org.apache.batik.bridge.*;
import org.apache.batik.swing.svg.*;
import org.apache.batik.swing.gvt.*;
import org.apache.batik.gvt.*;
import org.apache.batik.gvt.renderer.*;
import org.apache.batik.gvt.filter.*;
import org.apache.batik.gvt.event.*;

// SVG
import org.w3c.dom.svg.SVGSVGElement;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.*;
import org.w3c.dom.svg.*;

// Java
import java.io.*;
import java.util.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Dimension;

/**
 * Renderer that renders to PostScript.
 * <br>
 * This class currently generates PostScript Level 2 code. The only exception
 * is the FlateEncode filter which is a Level 3 feature. The filters in use
 * are hardcoded at the moment.
 * <br>
 * This class follows the Document Structuring Conventions (DSC) version 3.0
 * (If I did everything right). If anyone modifies this renderer please make
 * sure to also follow the DSC to make it simpler to programmatically modify
 * the generated Postscript files (ex. extract pages etc.).
 * <br>
 * TODO: Character size/spacing, SVG Transcoder for Batik, configuration, move
 * to PrintRenderer, maybe improve filters (I'm not very proud of them), add a
 * RunLengthEncode filter (useful for Level 2 Postscript), Improve
 * DocumentProcessColors stuff (probably needs to be configurable, then maybe
 * add a color to grayscale conversion for bitmaps to make output smaller (See
 * PCLRenderer), font embedding, support different character encodings, try to
 * implement image transparency, positioning of images is wrong etc. <P>
 *
 * Modified by Mark Lillywhite mark-fop@inomial.com, to use the new
 * Renderer interface. This PostScript renderer appears to be the
 * most efficient at producing output.
 *
 * @author Jeremias M�rki
 */
public class PSRenderer extends AbstractRenderer {

    /**
     * the application producing the PostScript
     */
    protected String producer;

    int imagecount = 0;    // DEBUG

    private boolean enableComments = true;

    /**
     * the stream used to output the PostScript
     */
    protected PSStream out;
    private boolean ioTrouble = false;

    private String currentFontName;
    private int currentFontSize;
    private int pageHeight;
    private int pageWidth;
    private float currRed;
    private float currGreen;
    private float currBlue;

    private FontInfo fontInfo;

    protected IDReferences idReferences;

    protected Hashtable options;


    /**
     * set the document's producer
     *
     * @param producer string indicating application producing the PostScript
     */
    public void setProducer(String producer) {
        this.producer = producer;
    }


    /**
     * set up renderer options
     */
    public void setOptions(Hashtable options) {
        this.options = options;
    }

    /**
     * write out a command
     */
    protected void write(String cmd) {
        try {
            out.write(cmd);
        } catch (IOException e) {
            if (!ioTrouble)
                e.printStackTrace();
            ioTrouble = true;
        }
    }

    /**
     * write out a comment
     */
    protected void comment(String comment) {
        if (this.enableComments)
            write(comment);
    }

    protected void writeFontDict(FontInfo fontInfo) {
        write("%%BeginResource: procset FOPFonts");
        write("%%Title: Font setup (shortcuts) for this file");
        write("/FOPFonts 100 dict dup begin");
        write("/bd{bind def}bind def");
        write("/ld{load def}bd");
        write("/M/moveto ld");
        write("/RM/rmoveto ld");
        write("/t/show ld");
        write("/A/ashow ld");

        write("/ux 0.0 def");
        write("/uy 0.0 def");
        // write("/cf /Helvetica def");
        // write("/cs 12000 def");

        // <font> <size> F
        write("/F {");
        write("  /Tp exch def");
        // write("  currentdict exch get");
        write("  /Tf exch def");
        write("  Tf findfont Tp scalefont setfont");
        write("  /cf Tf def  /cs Tp def  /cw ( ) stringwidth pop def");
        write("} bd");

        write("/ULS {currentpoint /uy exch def /ux exch def} bd");
        write("/ULE {");
        write("  /Tcx currentpoint pop def");
        write("  gsave");
        write("  newpath");
        write("  cf findfont cs scalefont dup");
        write("  /FontMatrix get 0 get /Ts exch def /FontInfo get dup");
        write("  /UnderlinePosition get Ts mul /To exch def");
        write("  /UnderlineThickness get Ts mul /Tt exch def");
        write("  ux uy To add moveto  Tcx uy To add lineto");
        write("  Tt setlinewidth stroke");
        write("  grestore");
        write("} bd");

        write("/OLE {");
        write("  /Tcx currentpoint pop def");
        write("  gsave");
        write("  newpath");
        write("  cf findfont cs scalefont dup");
        write("  /FontMatrix get 0 get /Ts exch def /FontInfo get dup");
        write("  /UnderlinePosition get Ts mul /To exch def");
        write("  /UnderlineThickness get Ts mul /Tt exch def");
        write("  ux uy To add cs add moveto Tcx uy To add cs add lineto");
        write("  Tt setlinewidth stroke");
        write("  grestore");
        write("} bd");

        write("/SOE {");
        write("  /Tcx currentpoint pop def");
        write("  gsave");
        write("  newpath");
        write("  cf findfont cs scalefont dup");
        write("  /FontMatrix get 0 get /Ts exch def /FontInfo get dup");
        write("  /UnderlinePosition get Ts mul /To exch def");
        write("  /UnderlineThickness get Ts mul /Tt exch def");
        write("  ux uy To add cs 10 mul 26 idiv add moveto Tcx uy To add cs 10 mul 26 idiv add lineto");
        write("  Tt setlinewidth stroke");
        write("  grestore");
        write("} bd");



        // write("/gfF1{/Helvetica findfont} bd");
        // write("/gfF3{/Helvetica-Bold findfont} bd");
        Hashtable fonts = fontInfo.getFonts();
        Enumeration enum = fonts.keys();
        while (enum.hasMoreElements()) {
            String key = (String)enum.nextElement();
            Font fm = (Font)fonts.get(key);
            write("/" + key + " /" + fm.fontName() + " def");
        }
        write("end def");
        write("%%EndResource");
        enum = fonts.keys();
        while (enum.hasMoreElements()) {
            String key = (String)enum.nextElement();
            Font fm = (Font)fonts.get(key);
            write("/" + fm.fontName() + " findfont");
            write("dup length dict begin");
            write("  {1 index /FID ne {def} {pop pop} ifelse} forall");
            write("  /Encoding ISOLatin1Encoding def");
            write("  currentdict");
            write("end");
            write("/" + fm.fontName() + " exch definefont pop");
        }
    }

    protected void movetoCurrPosition() {
        write(this.currentXPosition + " " + this.currentYPosition + " M");
    }

    /**
     * set up the font info
     *
     * @param fontInfo the font info object to set up
     */
    public void setupFontInfo(FontInfo fontInfo) {
        /* use PDF's font setup to get PDF metrics */
        org.apache.fop.render.pdf.FontSetup.setup(fontInfo);
        this.fontInfo = fontInfo;
    }

    protected void addFilledRect(int x, int y, int w, int h,
                                 ColorType col) {
            write("newpath");
            write(x + " " + y + " M");
            write(w + " 0 rlineto");
            write("0 " + (-h) + " rlineto");
            write((-w) + " 0 rlineto");
            write("0 " + h + " rlineto");
            write("closepath");
            useColor(col);
            write("fill");
    }

    /**
     * render a display space to PostScript
     *
     * @param space the space to render
     */
    public void renderDisplaySpace(DisplaySpace space) {
        // write("% --- DisplaySpace size="+space.getSize());
        this.currentYPosition -= space.getSize();
        movetoCurrPosition();
    }

    /**
     * render a foreign object area
     */
    public void renderForeignObjectArea(ForeignObjectArea area) {
        // if necessary need to scale and align the content
        area.getObject().render(this);
    }

    /**
     * render an SVG area to PostScript
     *
     * @param area the area to render
     */
    public void renderSVGArea(SVGArea area) {
        int x = this.currentXPosition;
        int y = this.currentYPosition;
        Document doc = area.getSVGDocument();

        org.apache.fop.svg.SVGUserAgent userAgent
            = new org.apache.fop.svg.SVGUserAgent(new AffineTransform());
        userAgent.setLogger(log);

        GVTBuilder builder = new GVTBuilder();
        BridgeContext ctx = new BridgeContext(userAgent);

        GraphicsNode root;
        try {
            root = builder.build(ctx, doc);
        } catch (Exception e) {
            log.error("svg graphic could not be built: "
                                   + e.getMessage(), e);
            return;
        }
        // get the 'width' and 'height' attributes of the SVG document
        float w = (float)ctx.getDocumentSize().getWidth() * 1000f;
        float h = (float)ctx.getDocumentSize().getHeight() * 1000f;
        ctx = null;
        builder = null;

        float sx = 1, sy = -1;
        int xOffset = x, yOffset = y;

        comment("% --- SVG Area");
        write("gsave");
        if (w != 0 && h != 0) {
            write("newpath");
            write(x + " " + y + " M");
            write((x + w) + " " + y + " rlineto");
            write((x + w) + " " + (y - h) + " rlineto");
            write(x + " " + (y - h) + " rlineto");
            write("closepath");
            write("clippath");
        }
        // transform so that the coordinates (0,0) is from the top left
        // and positive is down and to the right. (0,0) is where the
        // viewBox puts it.
        write(xOffset + " " + yOffset + " translate");
        write(sx + " " + sy + " scale");

        PSGraphics2D graphics = new PSGraphics2D(false, area.getFontState(),
                                this, currentFontName,
                                currentFontSize,
                                currentXPosition,
                                currentYPosition);
        graphics.setGraphicContext(new org.apache.batik.ext.awt.g2d.GraphicContext());
        try {
            root.paint(graphics);
        } catch (Exception e) {
            log.error("svg graphic could not be rendered: "
                                   + e.getMessage(), e);
        }

        write("grestore");

        comment("% --- SVG Area end");
        movetoCurrPosition();
    }

    public void renderEPS(FopImage img, int x, int y, int w, int h) {
        try {
            EPSImage eimg = (EPSImage)img;
            int[] bbox = eimg.getBBox();
            int bboxw = bbox[2] - bbox[0];
            int bboxh = bbox[3] - bbox[1];


            write("%%BeginDocument: " + eimg.getDocName());
            write("BeginEPSF");

            write(x + " " + (y - h) + " translate");
            write("0.0 rotate");
            write((long)(w/bboxw) + " " + (long)(h/bboxh) + " scale");
            write(-bbox[0] + " " + (-bbox[1]) + " translate");
            write(bbox[0] + " " + bbox[1] + " " + bboxw + " " + bboxh + " rectclip");
            write("newpath");
            out.writeByteArr(img.getBitmaps());
            write("%%EndDocument");
            write("EndEPSF");
            write("");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("PSRenderer.renderImageArea(): Error rendering bitmap ("
                                   + e.getMessage() + ")", e);
        }
    }

    public void renderBitmap(FopImage img, int x, int y, int w, int h) {
        try {
            boolean iscolor = img.getColorSpace().getColorSpace()
                              != ColorSpace.DEVICE_GRAY;
            byte[] imgmap = img.getBitmaps();

            write("gsave");
            if (img.getColorSpace().getColorSpace() == ColorSpace.DEVICE_CMYK)
                write("/DeviceCMYK setcolorspace");
            else
                write("/DeviceRGB setcolorspace");

            write(x + " " + (y - h) + " translate");
            write(w + " " + h + " scale");
            write("<<");
            write("  /ImageType 1");
            write("  /Width " + img.getWidth());
            write("  /Height " + img.getHeight());
            write("  /BitsPerComponent 8");
            if (img.getColorSpace().getColorSpace() == ColorSpace.DEVICE_CMYK) {
                if (img.invertImage())
                    write("  /Decode [1 0 1 0 1 0 1 0]");
                else
                    write("  /Decode [0 1 0 1 0 1 0 1]");
            } else if (iscolor) {
                write("  /Decode [0 1 0 1 0 1]");
            } else {
                write("  /Decode [0 1]");
            }
            // Setup scanning for left-to-right and top-to-bottom
            write("  /ImageMatrix [" + img.getWidth() + " 0 0 -"
                  + img.getHeight() + " 0 " + img.getHeight() + "]");

            if (img instanceof JpegImage)
                write("  /DataSource currentfile /ASCII85Decode filter /DCTDecode filter");
            else
                write("  /DataSource currentfile /ASCII85Decode filter /FlateDecode filter");
            // write("  /DataSource currentfile /ASCIIHexDecode filter /FlateDecode filter");
            // write("  /DataSource currentfile /ASCII85Decode filter /RunLengthDecode filter");
            // write("  /DataSource currentfile /ASCIIHexDecode filter /RunLengthDecode filter");
            // write("  /DataSource currentfile /ASCIIHexDecode filter");
            // write("  /DataSource currentfile /ASCII85Decode filter");
            // write("  /DataSource currentfile /RunLengthDecode filter");
            write(">>");
            write("image");

            /*
             * for (int y=0; y<img.getHeight(); y++) {
             * int indx = y * img.getWidth();
             * if (iscolor) indx*= 3;
             * for (int x=0; x<img.getWidth(); x++) {
             * if (iscolor) {
             * writeASCIIHex(imgmap[indx++] & 0xFF);
             * writeASCIIHex(imgmap[indx++] & 0xFF);
             * writeASCIIHex(imgmap[indx++] & 0xFF);
             * } else {
             * writeASCIIHex(imgmap[indx++] & 0xFF);
             * }
             * }
             * }
             */
            try {
                // imgmap[0] = 1;
                InputStream bain = new ByteArrayInputStream(imgmap);
                InputStream in;
                in = bain;
                if (!(img instanceof JpegImage))
                    in = FlateEncodeFilter.filter(in);
                // in = RunLengthEncodeFilter.filter(in);
                // in = ASCIIHexEncodeFilter.filter(in);
                in = ASCII85EncodeFilter.filter(in);
                copyStream(in, this.out);
            } catch (IOException e) {
                if (!ioTrouble)
                    e.printStackTrace();
                ioTrouble = true;
            }

            write("");
            write("grestore");
        } catch (FopImageException e) {
            log.error("PSRenderer.renderImageArea(): Error rendering bitmap ("
                                   + e.getMessage() + ")", e);
        }
    }

    /**
     * render an image area to PostScript
     *
     * @param area the area to render
     */
    public void renderImageArea(ImageArea area) {
        int x = this.currentAreaContainerXPosition + area.getXOffset();
        int y = this.currentYPosition;
        int w = area.getContentWidth();
        int h = area.getHeight();
        this.currentYPosition -= area.getHeight();

        imagecount++;
        // if (imagecount!=4) return;

                comment("% --- ImageArea");
        if (area.getImage() instanceof SVGImage) {}
        else if (area.getImage() instanceof EPSImage) {
            renderEPS(area.getImage(), x, y, w, h);
        } else {
            renderBitmap(area.getImage(), x, y, w, h);
        }
        comment("% --- ImageArea end");
    }

    private long copyStream(InputStream in, OutputStream out,
                            int bufferSize) throws IOException {
        long bytes_total = 0;
        byte[] buf = new byte[bufferSize];
        int bytes_read;
        while ((bytes_read = in.read(buf)) != -1) {
            bytes_total += bytes_read;
            out.write(buf, 0, bytes_read);
        }
        return bytes_total;
    }


    private long copyStream(InputStream in,
                            OutputStream out) throws IOException {
        return copyStream(in, out, 4096);
    }

    /**
     * render an inline area to PostScript
     *
     * @param area the area to render
     */
    public void renderWordArea(WordArea area) {
        FontState fs = area.getFontState();
        String fontWeight = fs.getFontWeight();
        StringBuffer sb = new StringBuffer();
        String s;
        if (area.getPageNumberID()
                != null) {    // this text is a page number, so resolve it
            s = idReferences.getPageNumber(area.getPageNumberID());
            if (s == null) {
                s = "";
            }
        } else {
            s = area.getText();
        }
        int l = s.length();

        for (int i = 0; i < l; i++) {
            char ch = s.charAt(i);
            char mch = fs.mapChar(ch);
            if (mch > 127) {
                sb = sb.append("\\" + Integer.toOctalString(mch));
            } else {
                String escape = "\\()[]{}";
                if (escape.indexOf(mch) >= 0) {
                    sb.append("\\");
                }
                sb = sb.append(mch);
            }
        }

        String psString = null;
        if (area.getFontState().getLetterSpacing() > 0) {
            float f = area.getFontState().getLetterSpacing() * 1000 / this.currentFontSize;
            psString = (new StringBuffer().append(f).append(" 0.0 (").append(sb).
                        append(") A")).toString();
        } else {
            psString = (new StringBuffer("(").append(sb).append(") t")).toString();
        }


        // System.out.println("["+s+"] --> ["+sb.toString()+"]");

        // comment("% --- InlineArea font-weight="+fontWeight+": " + sb.toString());
        useFont(fs.getFontName(), fs.getFontSize());
        useColor(area.getRed(), area.getGreen(), area.getBlue());
        if (area.getUnderlined() || area.getLineThrough()
                || area.getOverlined())
            write("ULS");
        write(psString);
        if (area.getUnderlined())
            write("ULE");
        if (area.getLineThrough())
            write("SOE");
        if (area.getOverlined())
            write("OLE");
        this.currentXPosition += area.getContentWidth();
    }

    public void useFont(String name, int size) {
        if ((currentFontName != name) || (currentFontSize != size)) {
            write(name + " " + size + " F");
            currentFontName = name;
            currentFontSize = size;
        }
    }

    /**
     * render an inline space to PostScript
     *
     * @param space the space to render
     */
    public void renderInlineSpace(InlineSpace space) {
        // write("% --- InlineSpace size="+space.getSize());
        this.currentXPosition += space.getSize();
        if (space.getUnderlined() || space.getLineThrough()
                || space.getOverlined())
            write("ULS");
        write(space.getSize() + " 0 RM");
        if (space.getUnderlined())
            write("ULE");
        if (space.getLineThrough())
            write("SOE");
        if (space.getOverlined())
            write("OLE");
    }

    /**
     * render a line area to PostScript
     *
     * @param area the area to render
     */
    public void renderLineArea(LineArea area) {
        int rx = this.currentAreaContainerXPosition + area.getStartIndent();
        int ry = this.currentYPosition;
        int w = area.getContentWidth();
        int h = area.getHeight();

        this.currentYPosition -= area.getPlacementOffset();
        this.currentXPosition = rx;

        int bl = this.currentYPosition;
        // method is identical to super method except next line
        movetoCurrPosition();

        String fontWeight = area.getFontState().getFontWeight();
        // comment("% --- LineArea begin font-weight="+fontWeight);
        Enumeration e = area.getChildren().elements();
        while (e.hasMoreElements()) {
            Box b = (Box)e.nextElement();
            this.currentYPosition = ry - area.getPlacementOffset();
            b.render(this);
        }
        // comment("% --- LineArea end");

        this.currentYPosition = ry - h;
        this.currentXPosition = rx;
    }

    /**
     * render a page to PostScript
     *
     * @param page the page to render
     */
    public void renderPage(Page page) {
        this.idReferences = page.getIDReferences();

        BodyAreaContainer body;
        AreaContainer before, after;
        write("%%Page: " + page.getNumber() + " " + page.getNumber());
        write("%%BeginPageSetup");
        write("FOPFonts begin");
        write("0.001 0.001 scale");
        write("%%EndPageSetup");
        body = page.getBody();
        before = page.getBefore();
        after = page.getAfter();
        if (before != null) {
            renderAreaContainer(before);
        }
        renderBodyAreaContainer(body);
        if (after != null) {
            renderAreaContainer(after);
        }
        write("showpage");
        write("%%PageTrailer");
        write("%%EndPage");
    }

    /**
     * render a leader area to PostScript
     *
     * @param area the area to render
     */
    public void renderLeaderArea(LeaderArea area) {
        int rx = this.currentXPosition;
        int ry = this.currentYPosition;
        int w = area.getContentWidth();
        int th = area.getRuleThickness();
        int th2 = th / 2;
        int th3 = th / 3;
        int th4 = th / 4;

        switch (area.getLeaderPattern()) {
        case LeaderPattern.SPACE:
            // NOP

            break;
        case LeaderPattern.RULE:
            if (area.getRuleStyle() == RuleStyle.NONE)
                break;
            useColor(area.getRed(), area.getGreen(), area.getBlue());
            write("gsave");
            write("0 setlinecap");
            switch (area.getRuleStyle()) {
            case RuleStyle.DOTTED:
                write("newpath");
                write("[1000 3000] 0 setdash");
                write(th + " setlinewidth");
                write(rx + " " + ry + " M");
                write(w + " 0 rlineto");
                useColor(area.getRed(), area.getGreen(), area.getBlue());
                write("stroke");
                break;
            case RuleStyle.DASHED:
                write("newpath");
                write("[3000 3000] 0 setdash");
                write(th + " setlinewidth");
                write(rx + " " + ry + " M");
                write(w + " 0 rlineto");
                useColor(area.getRed(), area.getGreen(), area.getBlue());
                write("stroke");
                break;
            case RuleStyle.SOLID:
                write("newpath");
                write(th + " setlinewidth");
                write(rx + " " + ry + " M");
                write(w + " 0 rlineto");
                useColor(area.getRed(), area.getGreen(), area.getBlue());
                write("stroke");
                break;
            case RuleStyle.DOUBLE:
                write("newpath");
                write(th3 + " setlinewidth");
                write(rx + " " + (ry - th3) + " M");
                write(w + " 0 rlineto");
                write(rx + " " + (ry + th3) + " M");
                write(w + " 0 rlineto");
                useColor(area.getRed(), area.getGreen(), area.getBlue());
                write("stroke");
                break;
            case RuleStyle.GROOVE:
                write(th2 + " setlinewidth");
                write("newpath");
                write(rx + " " + (ry - th4) + " M");
                write(w + " 0 rlineto");
                useColor(area.getRed(), area.getGreen(), area.getBlue());
                write("stroke");
                write("newpath");
                write(rx + " " + (ry + th4) + " M");
                write(w + " 0 rlineto");
                useColor(1, 1, 1);    // white
                write("stroke");
                break;
            case RuleStyle.RIDGE:
                write(th2 + " setlinewidth");
                write("newpath");
                write(rx + " " + (ry - th4) + " M");
                write(w + " 0 rlineto");
                useColor(1, 1, 1);    // white
                write("stroke");
                write("newpath");
                write(rx + " " + (ry + th4) + " M");
                write(w + " 0 rlineto");
                useColor(area.getRed(), area.getGreen(), area.getBlue());
                write("stroke");
                break;
            }
            write("grestore");
            break;
        case LeaderPattern.DOTS:
            comment("% --- Leader dots NYI");
            log.error("Leader dots: Not yet implemented");
            break;
        case LeaderPattern.USECONTENT:
            comment("% --- Leader use-content NYI");
            log.error("Leader use-content: Not yet implemented");
            break;
        }
        this.currentXPosition += area.getContentWidth();
        write(area.getContentWidth() + " 0 RM");
    }

    protected void doFrame(Area area) {
        int w, h;
        int rx = this.currentAreaContainerXPosition;
        w = area.getContentWidth();
        BorderAndPadding bap = area.getBorderAndPadding();

        if (area instanceof BlockArea)
            rx += ((BlockArea)area).getStartIndent();

        h = area.getContentHeight();
        int ry = this.currentYPosition;

        rx = rx - area.getPaddingLeft();
        ry = ry + area.getPaddingTop();
        w = w + area.getPaddingLeft() + area.getPaddingRight();
        h = h + area.getPaddingTop() + area.getPaddingBottom();

        rx = rx - area.getBorderLeftWidth();
        ry = ry + area.getBorderTopWidth();
        w = w + area.getBorderLeftWidth() + area.getBorderRightWidth();
        h = h + area.getBorderTopWidth() + area.getBorderBottomWidth();

        // Create a textrect with these dimensions.
        // The y co-ordinate is measured +ve downwards so subtract page-height

        ColorType bg = area.getBackgroundColor();
        if ((bg != null) && (bg.alpha() == 0)) {
            write("newpath");
            write(rx + " " + ry + " M");
            write(w + " 0 rlineto");
            write("0 " + (-h) + " rlineto");
            write((-w) + " 0 rlineto");
            write("0 " + h + " rlineto");
            write("closepath");
            useColor(bg);
            write("fill");
        }


        if (area.getBorderTopWidth() != 0) {
            write("newpath");
            write(rx + " " + ry + " M");
            write(w + " 0 rlineto");
            write(area.getBorderTopWidth() + " setlinewidth");
            write("0 setlinecap");
            useColor(bap.getBorderColor(BorderAndPadding.TOP));
            write("stroke");
        }
        if (area.getBorderLeftWidth() != 0) {
            write("newpath");
            write(rx + " " + ry + " M");
            write("0 " + (-h) + " rlineto");
            write(area.getBorderLeftWidth() + " setlinewidth");
            write("0 setlinecap");
            useColor(bap.getBorderColor(BorderAndPadding.LEFT));
            write("stroke");
        }
        if (area.getBorderRightWidth() != 0) {
            write("newpath");
            write((rx + w) + " " + ry + " M");
            write("0 " + (-h) + " rlineto");
            write(area.getBorderRightWidth() + " setlinewidth");
            write("0 setlinecap");
            useColor(bap.getBorderColor(BorderAndPadding.RIGHT));
            write("stroke");
        }
        if (area.getBorderBottomWidth() != 0) {
            write("newpath");
            write(rx + " " + (ry - h) + " M");
            write(w + " 0 rlineto");
            write(area.getBorderBottomWidth() + " setlinewidth");
            write("0 setlinecap");
            useColor(bap.getBorderColor(BorderAndPadding.BOTTOM));
            write("stroke");
        }
    }

    private void useColor(ColorType col) {
        useColor(col.red(), col.green(), col.blue());
    }

    private void useColor(float red, float green, float blue) {
        if ((red != currRed) || (green != currGreen) || (blue != currBlue)) {
            write(red + " " + green + " " + blue + " setrgbcolor");
            currRed = red;
            currGreen = green;
            currBlue = blue;
        }
    }

    /**
      Default start renderer method. This would
      normally be overridden. (mark-fop@inomial.com).
    */
    public void startRenderer(OutputStream outputStream)
    throws IOException {
        log.debug("rendering areas to PostScript");

        this.out = new PSStream(outputStream);
        write("%!PS-Adobe-3.0");
        write("%%Creator: "+this.producer);
        write("%%DocumentProcessColors: Black");
        write("%%DocumentSuppliedResources: procset FOPFonts");
        write("%%EndComments");
        write("%%BeginDefaults");
        write("%%EndDefaults");
        write("%%BeginProlog");
        write("%%EndProlog");
        write("%%BeginSetup");
        writeFontDict(fontInfo);

        /* Write proc for including EPS */
        write("%%BeginResource: procset EPSprocs");
        write("%%Title: EPS encapsulation procs");

        write("/BeginEPSF { %def");
        write("/b4_Inc_state save def         % Save state for cleanup");
        write("/dict_count countdictstack def % Count objects on dict stack");
        write("/op_count count 1 sub def      % Count objects on operand stack");
        write("userdict begin                 % Push userdict on dict stack");
        write("/showpage { } def              % Redefine showpage, { } = null proc");
        write("0 setgray 0 setlinecap         % Prepare graphics state");
        write("1 setlinewidth 0 setlinejoin");
        write("10 setmiterlimit [ ] 0 setdash newpath");
        write("/languagelevel where           % If level not equal to 1 then");
        write("{pop languagelevel             % set strokeadjust and");
        write("1 ne                           % overprint to their defaults.");
        write("{false setstrokeadjust false setoverprint");
        write("} if");
        write("} if");
        write("} bind def");

        write("/EndEPSF { %def");
        write("count op_count sub {pop} repeat            % Clean up stacks");
        write("countdictstack dict_count sub {end} repeat");
        write("b4_Inc_state restore");
        write("} bind def");
        write("%%EndResource");

        write("%%EndSetup");
        write("FOPFonts begin");
    }

    /**
      Default stop renderer method. This would
      normally be overridden. (mark-fop@inomial.com).
    */
    public void stopRenderer(OutputStream outputStream)
    throws IOException {
        write("%%Trailer");
        write("%%EOF");
        this.out.flush();
        log.debug("written out PostScript");
    }

    public void render(Page page, OutputStream outputStream) {
        this.renderPage(page);
    }
}
