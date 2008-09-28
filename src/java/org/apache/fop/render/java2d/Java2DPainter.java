/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.render.java2d;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Map;
import java.util.Stack;

import org.w3c.dom.Document;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.render.intermediate.AbstractIFPainter;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFState;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.traits.RuleStyle;

/**
 * {@code IFPainter} implementation that paints on a Graphics2D instance.
 */
public class Java2DPainter extends AbstractIFPainter {

    /** logging instance */
    private static Log log = LogFactory.getLog(Java2DPainter.class);

    /** the FO user agent */
    protected FOUserAgent userAgent;

    /** The font information */
    protected FontInfo fontInfo;

    /** Holds the intermediate format state */
    protected IFState state;

    private Java2DBorderPainter borderPainter;

    /** The current state, holds a Graphics2D and its context */
    protected Java2DGraphicsState g2dState;
    private Stack g2dStateStack = new Stack();

    /**
     * Main constructor.
     * @param g2d the target Graphics2D instance
     * @param userAgent the user agent
     * @param fontInfo the font information
     */
    public Java2DPainter(Graphics2D g2d, FOUserAgent userAgent, FontInfo fontInfo) {
        super();
        this.userAgent = userAgent;
        this.state = IFState.create();
        this.fontInfo = fontInfo;
        this.g2dState = new Java2DGraphicsState(g2d, fontInfo, g2d.getTransform());
        this.borderPainter = new Java2DBorderPainter(this);
    }

    /** {@inheritDoc} */
    public FOUserAgent getUserAgent() {
        return this.userAgent;
    }

    /**
     * Returns the associated {@code FontInfo} object.
     * @return the font info
     */
    protected FontInfo getFontInfo() {
        return this.fontInfo;
    }

    /**
     * Returns the Java2D graphics state.
     * @return the graphics state
     */
    protected Java2DGraphicsState getState() {
        return this.g2dState;
    }

    //----------------------------------------------------------------------------------------------


    /** {@inheritDoc} */
    public void startViewport(AffineTransform transform, Dimension size, Rectangle clipRect)
            throws IFException {
        saveGraphicsState();
        try {
            concatenateTransformationMatrix(transform);
            //TODO CLIP!
        } catch (IOException ioe) {
            throw new IFException("I/O error in startViewport()", ioe);
        }
    }

    /** {@inheritDoc} */
    public void endViewport() throws IFException {
        restoreGraphicsState();
    }

    /** {@inheritDoc} */
    public void startGroup(AffineTransform transform) throws IFException {
        saveGraphicsState();
        try {
            concatenateTransformationMatrix(transform);
        } catch (IOException ioe) {
            throw new IFException("I/O error in startGroup()", ioe);
        }
    }

    /** {@inheritDoc} */
    public void endGroup() throws IFException {
        restoreGraphicsState();
    }

    /** {@inheritDoc} */
    public void drawImage(String uri, Rectangle rect, Map foreignAttributes) throws IFException {
        drawImageUsingURI(uri, rect);
    }

    /** {@inheritDoc} */
    protected RenderingContext createRenderingContext() {
        Java2DRenderingContext java2dContext = new Java2DRenderingContext(
                getUserAgent(), g2dState.getGraph(), getFontInfo());
        return java2dContext;
    }

    /** {@inheritDoc} */
    public void drawImage(Document doc, Rectangle rect, Map foreignAttributes) throws IFException {
        drawImageUsingDocument(doc, rect);
    }

    /** {@inheritDoc} */
    public void clipRect(Rectangle rect) throws IFException {
    }

    /** {@inheritDoc} */
    public void fillRect(Rectangle rect, Paint fill) throws IFException {
        if (fill == null) {
            return;
        }
        if (rect.width != 0 && rect.height != 0) {
            g2dState.updatePaint(fill);
            g2dState.getGraph().fill(rect);
        }
    }

    /** {@inheritDoc} */
    public void drawBorderRect(Rectangle rect, BorderProps before, BorderProps after,
            BorderProps start, BorderProps end) throws IFException {
        if (before != null || after != null || start != null || end != null) {
            this.borderPainter.drawBorders(rect, before, after, start, end);
        }
    }

    /** {@inheritDoc} */
    public void drawLine(Point start, Point end, int width, Color color, RuleStyle style)
            throws IFException {
        this.borderPainter.drawLine(start, end, width, color, style);
    }

    /** {@inheritDoc} */
    public void drawText(int x, int y, int[] dx, int[] dy, String text) throws IFException {
        //Note: dy is currently ignored
        g2dState.updateColor(state.getTextColor());
        FontTriplet triplet = new FontTriplet(
                state.getFontFamily(), state.getFontStyle(), state.getFontWeight());
        //TODO Ignored: state.getFontVariant()
        //TODO Opportunity for font caching if font state is more heavily used
        Font font = getFontInfo().getFontInstance(triplet, state.getFontSize());
        //String fontName = font.getFontName();
        //float fontSize = state.getFontSize() / 1000f;
        g2dState.updateFont(font.getFontName(), state.getFontSize() * 1000);

        Graphics2D g2d = this.g2dState.getGraph();
        GlyphVector gv = g2d.getFont().createGlyphVector(g2d.getFontRenderContext(), text);
        Point2D cursor = new Point2D.Float(0, 0);

        int l = text.length();
        int dxl = (dx != null ? dx.length : 0);

        if (dx != null && dxl > 0 && dx[0] != 0) {
            cursor.setLocation(cursor.getX() - (dx[0] / 10f), cursor.getY());
            gv.setGlyphPosition(0, cursor);
        }
        for (int i = 0; i < l; i++) {
            char orgChar = text.charAt(i);
            float glyphAdjust = 0;
            int cw = font.getCharWidth(orgChar);

            if (dx != null && i < dxl - 1) {
                glyphAdjust += dx[i + 1];
            }

            cursor.setLocation(cursor.getX() + cw - glyphAdjust, cursor.getY());
            gv.setGlyphPosition(i + 1, cursor);
        }
        g2d.drawGlyphVector(gv, x, y);
    }

    /** {@inheritDoc} */
    public void setFont(String family, String style, Integer weight, String variant, Integer size,
            Color color) throws IFException {
        if (family != null) {
            state.setFontFamily(family);
        }
        if (style != null) {
            state.setFontStyle(style);
        }
        if (weight != null) {
            state.setFontWeight(weight.intValue());
        }
        if (variant != null) {
            state.setFontVariant(variant);
        }
        if (size != null) {
            state.setFontSize(size.intValue());
        }
        if (color != null) {
            state.setTextColor(color);
        }
    }

    //----------------------------------------------------------------------------------------------

    /** Saves the current graphics state on the stack. */
    protected void saveGraphicsState() {
        g2dStateStack.push(g2dState);
        g2dState = new Java2DGraphicsState(g2dState);
    }

    /** Restores the last graphics state from the stack. */
    protected void restoreGraphicsState() {
        g2dState.dispose();
        g2dState = (Java2DGraphicsState)g2dStateStack.pop();
    }

    private void concatenateTransformationMatrix(AffineTransform transform) throws IOException {
        g2dState.transform(transform);
    }

}
