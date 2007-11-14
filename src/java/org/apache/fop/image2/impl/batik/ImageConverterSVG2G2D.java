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
 
package org.apache.fop.image2.impl.batik;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.fop.image2.Image;
import org.apache.fop.image2.ImageException;
import org.apache.fop.image2.ImageFlavor;
import org.apache.fop.image2.ImageProcessingHints;
import org.apache.fop.image2.impl.AbstractImageConverter;
import org.apache.fop.image2.impl.ImageGraphics2D;
import org.apache.fop.image2.impl.ImageXMLDOM;
import org.apache.fop.render.Graphics2DImagePainter;
import org.apache.fop.svg.SVGUserAgent;
import org.apache.fop.util.UnitConv;

/**
 * This ImageConverter converts SVG images to Java2D.
 * <p>
 * Note: The target flavor is "generic" Java2D. No Batik-specific bridges are hooked into the
 * conversion process. Specialized renderers may want to provide specialized adapters to profit
 * from target-format features (for example with PDF or PS). This converter is mainly for formats
 * which only support bitmap images or rudimentary Java2D support. 
 */
public class ImageConverterSVG2G2D extends AbstractImageConverter {

    /** {@inheritDoc} */
    public Image convert(Image src, Map hints) throws ImageException {
        checkSourceFlavor(src);
        final ImageXMLDOM svg = (ImageXMLDOM)src;
        if (!SVGDOMImplementation.SVG_NAMESPACE_URI.equals(svg.getRootNamespace())) {
            throw new IllegalArgumentException("XML DOM is not in the SVG namespace: "
                    + svg.getRootNamespace());
        }

        //Prepare
        float pxToMillimeter = (float)UnitConv.mm2in(72); //default: 72dpi
        Number ptm = (Number)hints.get(ImageProcessingHints.SOURCE_RESOLUTION);
        if (ptm != null) {
            pxToMillimeter = (float)UnitConv.mm2in(ptm.doubleValue());
        }
        SVGUserAgent ua = new SVGUserAgent(
                pxToMillimeter,
                new AffineTransform());
        GVTBuilder builder = new GVTBuilder();
        final BridgeContext ctx = new BridgeContext(ua);

        //Build the GVT tree
        final GraphicsNode root;
        try {
            root = builder.build(ctx, svg.getDocument());
        } catch (Exception e) {
            throw new ImageException("GVT tree could not be built for SVG graphic", e);
        }

        //Create the painter
        Graphics2DImagePainter painter = new Graphics2DImagePainter() {

            public void paint(Graphics2D g2d, Rectangle2D area) {
                // If no viewbox is defined in the svg file, a viewbox of 100x100 is
                // assumed, as defined in SVGUserAgent.getViewportSize()
                float iw = (float) ctx.getDocumentSize().getWidth();
                float ih = (float) ctx.getDocumentSize().getHeight();
                float w = (float) area.getWidth();
                float h = (float) area.getHeight();
                g2d.scale(w / iw, h / ih);

                root.paint(g2d);
            }

            public Dimension getImageSize() {
                return new Dimension(svg.getSize().getWidthMpt(), svg.getSize().getHeightMpt());
            }

        };

        ImageGraphics2D g2dImage = new ImageGraphics2D(src.getInfo(), painter);
        return g2dImage;
    }

    /** {@inheritDoc} */
    public ImageFlavor getSourceFlavor() {
        return ImageFlavor.XML_DOM;
    }

    /** {@inheritDoc} */
    public ImageFlavor getTargetFlavor() {
        return ImageFlavor.GRAPHICS2D;
    }

}
