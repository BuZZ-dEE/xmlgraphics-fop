/*
 * $Id$
 * Copyright (C) 2001-2002 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.render.xml;

// FOP
import org.apache.fop.svg.*;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.AbstractRenderer;
import org.apache.fop.image.FopImage;
import org.apache.fop.image.ImageArea;
import org.apache.fop.layout.*;
import org.apache.fop.layout.inline.*;
import org.apache.fop.pdf.*;
import org.apache.fop.fo.properties.LeaderPattern;
import org.apache.fop.apps.FOPException;

// Avalon
import org.apache.avalon.framework.logger.Logger;

// Java
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.util.List;

/**
 * Renderer that renders areas to XML for debugging purposes.
 *
 * Modified by Mark Lillywhite mark-fop@inomial.com to use the
 * new renderer interface. Not 100% certain that this is correct.
 */
public class XMLRenderer implements Renderer {

    protected Logger log;

    public void setLogger(Logger logger) {
        log = logger;
    }

    /**
     * indentation to use for pretty-printing the XML
     */
    protected int indent = 0;

    /**
     * the application producing the XML
     */
    protected String producer;

    /**
     * the writer used to output the XML
     */
    protected PrintWriter writer;

    /**
     * options
     */
    protected java.util.Map options;
    private boolean consistentOutput = false;

    public XMLRenderer() {}

    /**
     * set up renderer options
     */
    public void setOptions(java.util.Map options) {
        this.options = options;
        Boolean con = (Boolean)options.get("consistentOutput");
        if(con != null) {
            consistentOutput = con.booleanValue();
        }
    }

    /**
     * set the document's producer
     *
     * @param producer string indicating application producing the XML
     */
    public void setProducer(String producer) {
        this.producer = producer;
    }


    public void render(Page page, OutputStream outputStream)
    throws IOException {
        this.renderPage(page);
    }

    /**
     * write out spaces to make indent
     */
    protected void writeIndent() {
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < this.indent; i++) {
            s = s.append("  ");
        }
        this.writer.write(s.toString());
    }

    /**
     * write out an element
     *
     * @param element the full text of the element including tags
     */
    protected void writeElement(String element) {
        writeIndent();
        this.writer.write(element + "\n");
    }

    /**
     * write out an empty-element-tag
     *
     * @param tag the text of the tag
     */
    protected void writeEmptyElementTag(String tag) {
        writeIndent();
        this.writer.write(tag + "\n");
    }

    /**
     * write out an end tag
     *
     * @param tag the text of the tag
     */
    protected void writeEndTag(String tag) {
        this.indent--;
        writeIndent();
        this.writer.write(tag + "\n");
    }

    /**
     * write out a start tag
     *
     * @param tag the text of the tag
     */
    protected void writeStartTag(String tag) {
        writeIndent();
        this.writer.write(tag + "\n");
        this.indent++;
    }

    /**
     * set up the font info
     *
     * @param fontInfo the font info object to set up
     */
    public void setupFontInfo(FontInfo fontInfo) throws FOPException {

        /* use PDF's font setup to get PDF metrics */
        org.apache.fop.render.pdf.FontSetup.setup(fontInfo);
    }

    /**
     * Renders an image, scaling it to the given width and height.
     * If the scaled width and height is the same intrinsic size
     * of the image, the image is not scaled.
     *
     * @param x the x position of left edge in millipoints
     * @param y the y position of top edge in millipoints
     * @param w the width in millipoints
     * @param h the height in millipoints
     * @param image the image to be rendered
     * @param fs the font state to use when rendering text
     *           in non-bitmapped images.
     */
    protected void drawImageScaled(int x, int y, int w, int h,
                   FopImage image,
                   FontState fs) {
    // XXX: implement this
    }

    /**
     * Renders an image, clipping it as specified.
     *
     * @param x the x position of left edge in millipoints.
     * @param y the y position of top edge in millipoints.
     * @param clipX the left edge of the clip in millipoints
     * @param clipY the top edge of the clip in millipoints
     * @param clipW the clip width in millipoints
     * @param clipH the clip height in millipoints
     * @param fill the image to be rendered
     * @param fs the font state to use when rendering text
     *           in non-bitmapped images.
     */
    protected void drawImageClipped(int x, int y,
                    int clipX, int clipY,
                    int clipW, int clipH,
                    FopImage image,
                    FontState fs) {
    // XXX: implement this
    }

    /**
     * render an area container to XML
     *
     * @param area the area container to render
     */
    public void renderAreaContainer(AreaContainer area) {
        writeStartTag("<AreaContainer name=\"" + area.getAreaName() + "\">");
        List children = area.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Box b = (Box)children.get(i);
            b.render(this);
        }
        writeEndTag("</AreaContainer>");
    }

    /**
     * render a body area container to XML
     *
     * @param area the body area container to render
     */
    public void renderBodyAreaContainer(BodyAreaContainer area) {
        writeStartTag("<BodyAreaContainer>");
        List children = area.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Box b = (Box)children.get(i);
            b.render(this);
        }
        writeEndTag("</BodyAreaContainer>");
    }

    /**
     * render a region area container to XML
     *
     * @param area the region area container to render
     */
    public void renderRegionAreaContainer(AreaContainer area) {
        renderAreaContainer(area);
    }

    /**
     * render a span area to XML
     *
     * @param area the span area to render
     */
    public void renderSpanArea(SpanArea area) {
        writeStartTag("<SpanArea>");
        List children = area.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Box b = (Box)children.get(i);
            b.render(this);
        }
        writeEndTag("</SpanArea>");
    }

    /**
     * render a block area to XML
     *
     * @param area the block area to render
     */
    public void renderBlockArea(BlockArea area) {
        StringBuffer baText = new StringBuffer();
        baText.append("<BlockArea start-indent=\"" + area.getStartIndent()
                      + "\"");
        baText.append(" end-indent=\"" + area.getEndIndent() + "\"");
        baText.append("\nis-first=\"" + area.isFirst() + "\"");
        baText.append(" is-last=\"" + area.isLast() + "\"");
        if (null != area.getGeneratedBy())
            baText.append(" generated-by=\""
                          + area.getGeneratedBy().getName() + "//");
            if(consistentOutput) {
                baText.append(area.getGeneratedBy().getClass() + "\"");
            } else {
                baText.append(area.getGeneratedBy() + "\"");
            }
        baText.append(">");
        writeStartTag(baText.toString());

        // write out marker info
        List markers = area.getMarkers();
        if (!markers.isEmpty()) {
            writeStartTag("<Markers>");
            for (int i = 0; i < markers.size(); i++) {
                org.apache.fop.fo.flow.Marker marker =
                    (org.apache.fop.fo.flow.Marker)markers.get(i);
                StringBuffer maText = new StringBuffer();
                maText.append("<Marker marker-class-name=\""
                              + marker.getMarkerClassName() + "\"");
                maText.append(" RegisteredArea=\"" + marker.getRegistryArea()
                              + "\"");
                maText.append("/>");
                writeEmptyElementTag(maText.toString());
            }
            writeEndTag("</Markers>");
        }

        List children = area.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Box b = (Box)children.get(i);
            b.render(this);
        }
        writeEndTag("</BlockArea>");
    }

    public void renderInlineArea(InlineArea area) {
        StringBuffer iaText = new StringBuffer();
        iaText.append("<InlineArea");
        iaText.append("\nis-first=\"" + area.isFirst() + "\"");
        iaText.append(" is-last=\"" + area.isLast() + "\"");
        if (null != area.getGeneratedBy())
            iaText.append(" generated-by=\""
                          + area.getGeneratedBy().getName() + "//"
                          + area.getGeneratedBy() + "\"");
        iaText.append(">");
        writeStartTag(iaText.toString());

        // write out marker info
        List markers = area.getMarkers();
        if (!markers.isEmpty()) {
            writeStartTag("<Markers>");
            for (int i = 0; i < markers.size(); i++) {
                org.apache.fop.fo.flow.Marker marker =
                    (org.apache.fop.fo.flow.Marker)markers.get(i);
                StringBuffer maText = new StringBuffer();
                maText.append("<Marker marker-class-name=\""
                              + marker.getMarkerClassName() + "\"");
                maText.append(" RegisteredArea=\"" + marker.getRegistryArea()
                              + "\"");
                maText.append("/>");
                writeEmptyElementTag(maText.toString());
            }
            writeEndTag("</Markers>");
        }

        List children = area.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Box b = (Box)children.get(i);
            b.render(this);
        }
        writeEndTag("</InlineArea>");
    }

    /**
     * render a display space to XML
     *
     * @param space the space to render
     */
    public void renderDisplaySpace(DisplaySpace space) {
        if (!isCoarseXml())
            writeEmptyElementTag("<DisplaySpace size=\"" + space.getSize()
                                 + "\"/>");
    }

    /**
     * render a foreign object area
     */
    public void renderForeignObjectArea(ForeignObjectArea area) {
        // if necessary need to scale and align the content
        area.getObject().render(this);
    }

    /**
     * render an SVG area to XML
     *
     * @param area the area to render
     */
    public void renderSVGArea(SVGArea area) {
        writeEmptyElementTag("<SVG/>");
    }

    /**
     * render an image area to XML
     *
     * @param area the area to render
     */
    public void renderImageArea(ImageArea area) {
        writeEmptyElementTag("<ImageArea/>");
    }

    /**
     * render an inline area to XML
     *
     * @param area the area to render
     */
    public void renderWordArea(WordArea area) {
        String fontWeight = area.getFontState().getFontWeight();
        StringBuffer sb = new StringBuffer();
        String s = area.getText();
        int l = s.length();
        for (int i = 0; i < l; i++) {
            char ch = s.charAt(i);
            if (ch > 127)
                sb = sb.append("&#" + (int)ch + ";");
            else
                sb = sb.append(ch);
        }
        if (!isCoarseXml()) {
            writeElement("<WordArea font-weight=\"" + fontWeight
                         + "\" red=\"" + area.getRed() + "\" green=\""
                         + area.getGreen() + "\" blue=\"" + area.getBlue()
                         + "\" width=\"" + area.getContentWidth() + "\">"
                         + sb.toString() + "</WordArea>");
        } else {
            this.writer.write(sb.toString());
        }
    }

    /**
     * render an inline space to XML
     *
     * @param space the space to render
     */
    public void renderInlineSpace(InlineSpace space) {
        if (!isCoarseXml())
            writeEmptyElementTag("<InlineSpace size=\"" + space.getSize()
                                 + "\"/>");
        else
            this.writer.write(" ");
    }

    /**
     * render a line area to XML
     *
     * @param area the area to render
     */
    public void renderLineArea(LineArea area) {
        if (!isCoarseXml()) {
            String fontWeight = area.getFontState().getFontWeight();
            writeStartTag("<LineArea font-weight=\"" + fontWeight + "\">");
        }
        List children = area.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Box b = (Box)children.get(i);
            b.render(this);
        }
        if (!isCoarseXml())
            writeEndTag("</LineArea>");
        else
            this.writer.write("\n");
    }

    /**
     * render a page to XML
     *
     * @param page the page to render
     */
    public void renderPage(Page page) {
        BodyAreaContainer body;
        AreaContainer before, after, start, end;
        writeStartTag("<Page number=\"" + page.getFormattedNumber() + "\">");
        body = page.getBody();
        before = page.getBefore();
        after = page.getAfter();
        start = page.getStart();
        end = page.getEnd();
        if (before != null)
            before.render(this);
        if (after != null)
            after.render(this);
        if (start != null)
            start.render(this);
        if (end != null)
            end.render(this);
        writeEndTag("</Page>");
    }

    /**
     * render a leader area to XML
     *
     * @param area the area to render
     */
    public void renderLeaderArea(LeaderArea area) {
        if (isCoarseXml())
            return;
        String leaderPattern = "";
        switch (area.getLeaderPattern()) {
        case LeaderPattern.SPACE:
            leaderPattern = "space";
            break;
        case LeaderPattern.RULE:
            leaderPattern = "rule";
            break;
        case LeaderPattern.DOTS:
            leaderPattern = "dots";
            break;
        case LeaderPattern.USECONTENT:
            leaderPattern = "use-content";
            break;
        }

        writeEmptyElementTag("<Leader leader-pattern=\"" + leaderPattern
                             + " leader-length=\"" + area.getLeaderLength()
                             + "\" rule-thickness=\""
                             + area.getRuleThickness() + "\" rule-style=\""
                             + area.getRuleStyle() + "\" red=\""
                             + area.getRed() + "\" green=\""
                             + area.getGreen() + "\" blue=\""
                             + area.getBlue() + "\"/>");
    }

    private boolean isCoarseXml() {
        return ((Boolean)options.get("fineDetail")).booleanValue();
    }

    /**
      Default start renderer method. This would
      normally be overridden. (mark-fop@inomial.com).
    */
    public void startRenderer(OutputStream outputStream)
    throws IOException {
        log.debug("rendering areas to XML");
        this.writer = new PrintWriter(outputStream);
        this.writer.write( "<?xml version=\"1.0\"?>\n<!-- produced by " +
                           this.producer + " -->\n");
        writeStartTag("<AreaTree>");
    }

    /**
      Default stop renderer method. This would
      normally be overridden. (mark-fop@inomial.com).
    */
    public void stopRenderer(OutputStream outputStream)
    throws IOException {
        writeEndTag("</AreaTree>");
        this.writer.flush();
        log.debug("written out XML");
    }
}
