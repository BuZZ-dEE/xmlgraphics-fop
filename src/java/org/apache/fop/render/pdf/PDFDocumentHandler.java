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

package org.apache.fop.render.pdf;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.xmlgraphics.xmp.Metadata;

import org.apache.fop.apps.MimeConstants;
import org.apache.fop.fo.extensions.xmp.XMPMetadata;
import org.apache.fop.pdf.PDFAnnotList;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFPage;
import org.apache.fop.pdf.PDFReference;
import org.apache.fop.pdf.PDFResourceContext;
import org.apache.fop.pdf.PDFResources;
import org.apache.fop.render.extensions.prepress.PageBoundariesAttributes;
import org.apache.fop.render.extensions.prepress.PageScaleAttributes;
import org.apache.fop.render.intermediate.AbstractBinaryWritingIFDocumentHandler;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandlerConfigurator;
import org.apache.fop.render.intermediate.IFDocumentNavigationHandler;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFPainter;

/**
 * {@code IFDocumentHandler} implementation that produces PDF.
 */
public class PDFDocumentHandler extends AbstractBinaryWritingIFDocumentHandler {

    /** logging instance */
    private static Log log = LogFactory.getLog(PDFDocumentHandler.class);

    /** the PDF Document being created */
    protected PDFDocument pdfDoc;

    /**
     * Utility class which enables all sorts of features that are not directly connected to the
     * normal rendering process.
     */
    protected PDFRenderingUtil pdfUtil;

    /** the /Resources object of the PDF document being created */
    protected PDFResources pdfResources;

    /** The current content generator */
    protected PDFContentGenerator generator;

    /** the current annotation list to add annotations to */
    protected PDFResourceContext currentContext;

    /** the current page to add annotations to */
    protected PDFPage currentPage;

    /** the current page's PDF reference */
    protected PageReference currentPageRef;

    /** Used for bookmarks/outlines. */
    protected Map pageReferences = new java.util.HashMap();

    private PDFDocumentNavigationHandler documentNavigationHandler
            = new PDFDocumentNavigationHandler(this);

    /**
     * Default constructor.
     */
    public PDFDocumentHandler() {
    }

    /** {@inheritDoc} */
    public boolean supportsPagesOutOfOrder() {
        return true;
    }

    /** {@inheritDoc} */
    public String getMimeType() {
        return MimeConstants.MIME_PDF;
    }

    /** {@inheritDoc} */
    public void setContext(IFContext context) {
        super.setContext(context);
        this.pdfUtil = new PDFRenderingUtil(context.getUserAgent());
    }

    /** {@inheritDoc} */
    public IFDocumentHandlerConfigurator getConfigurator() {
        return new PDFRendererConfigurator(getUserAgent());
    }

    /** {@inheritDoc} */
    public IFDocumentNavigationHandler getDocumentNavigationHandler() {
        return this.documentNavigationHandler;
    }

    PDFRenderingUtil getPDFUtil() {
        return this.pdfUtil;
    }

    /** {@inheritDoc} */
    public void startDocument() throws IFException {
        super.startDocument();
        try {
            this.pdfDoc = pdfUtil.setupPDFDocument(this.outputStream);
        } catch (IOException e) {
            throw new IFException("I/O error in startDocument()", e);
        }
    }

    /** {@inheritDoc} */
    public void endDocumentHeader() throws IFException {
        pdfUtil.generateDefaultXMPMetadata();
    }

    /** {@inheritDoc} */
    public void endDocument() throws IFException {
        try {
            pdfDoc.getResources().addFonts(pdfDoc, fontInfo);
            pdfDoc.outputTrailer(this.outputStream);

            this.pdfDoc = null;

            pdfResources = null;
            this.generator = null;
            currentContext = null;
            currentPage = null;
        } catch (IOException ioe) {
            throw new IFException("I/O error in endDocument()", ioe);
        }
        super.endDocument();
    }

    /** {@inheritDoc} */
    public void startPageSequence(String id) throws IFException {
        //TODO page sequence title, country and language
    }

    /** {@inheritDoc} */
    public void endPageSequence() throws IFException {
        //nop
    }

    /** {@inheritDoc} */
    public void startPage(int index, String name, String pageMasterName, Dimension size)
                throws IFException {
        this.pdfResources = this.pdfDoc.getResources();

        String bleedWidth = (String) getContext().getForeignAttribute(
                PageBoundariesAttributes.EXT_BLEED);
        String cropOffset = (String) getContext().getForeignAttribute(
                PageBoundariesAttributes.EXT_CROP_OFFSET);
        String cropBoxValue = (String) getContext().getForeignAttribute(
                PageBoundariesAttributes.EXT_CROP_BOX);

        Rectangle trimBox = new Rectangle(0, 0,
                (int) size.getWidth(), (int) size.getHeight());
        Rectangle bleedBox
                = PageBoundariesAttributes.getBleedBoxRectangle(trimBox, bleedWidth);
        Rectangle mediaBox
                = PageBoundariesAttributes.getMediaBoxRectangle(trimBox, cropOffset);

        Rectangle cropBox = PageBoundariesAttributes.getCropBoxRectangle(
                trimBox, bleedBox, mediaBox, cropBoxValue);

        // set scale attributes
        double scaleX = 1;
        double scaleY = 1;
        String scale = (String) getContext().getForeignAttribute(
                PageScaleAttributes.EXT_PAGE_SCALE);
        Point2D scales = PageScaleAttributes.getScaleAttributes(scale);
        if (scales != null) {
            scaleX = scales.getX();
            scaleY = scales.getY();
        }

        this.currentPage = this.pdfDoc.getFactory().makePage(
                this.pdfResources,
                index,
                new Rectangle2D.Double(mediaBox.getX() * scaleX / 1000,
                        mediaBox.getY() * scaleY / 1000,
                        mediaBox.getWidth() * scaleX / 1000,
                        mediaBox.getHeight() * scaleY / 1000),
                new Rectangle2D.Double(cropBox.getX() * scaleX / 1000,
                        cropBox.getY() * scaleY / 1000,
                        cropBox.getWidth() * scaleX / 1000,
                        cropBox.getHeight() * scaleY / 1000),
                new Rectangle2D.Double(bleedBox.getX() * scaleX / 1000,
                        bleedBox.getY() * scaleY / 1000,
                        bleedBox.getWidth() * scaleX / 1000,
                        bleedBox.getHeight() * scaleY / 1000),
                new Rectangle2D.Double(trimBox.getX() * scaleX / 1000,
                        trimBox.getY() * scaleY / 1000,
                        trimBox.getWidth() * scaleX / 1000,
                        trimBox.getHeight() * scaleY / 1000));

        pdfUtil.generatePageLabel(index, name);

        currentPageRef = new PageReference(currentPage, size);
        this.pageReferences.put(new Integer(index), currentPageRef);

        this.generator = new PDFContentGenerator(this.pdfDoc, this.outputStream, this.currentPage);
        // Transform the PDF's default coordinate system (0,0 at lower left) to the PDFPainter's
        AffineTransform basicPageTransform = new AffineTransform(1, 0, 0, -1, 0,
                (scaleY * size.height) / 1000f);
        basicPageTransform.scale(scaleX, scaleY);
        generator.concatenate(basicPageTransform);
    }

    /** {@inheritDoc} */
    public IFPainter startPageContent() throws IFException {
        return new PDFPainter(this);
    }

    /** {@inheritDoc} */
    public void endPageContent() throws IFException {
        //nop
    }

    /** {@inheritDoc} */
    public void endPage() throws IFException {
        try {
            this.documentNavigationHandler.commit();
            this.pdfDoc.registerObject(generator.getStream());
            currentPage.setContents(generator.getStream());
            PDFAnnotList annots = currentPage.getAnnotations();
            if (annots != null) {
                this.pdfDoc.addObject(annots);
            }
            this.pdfDoc.addObject(currentPage);
            this.generator.flushPDFDoc();
            this.generator = null;
        } catch (IOException ioe) {
            throw new IFException("I/O error in endPage()", ioe);
        }
    }

    /** {@inheritDoc} */
    public void handleExtensionObject(Object extension) throws IFException {
        if (extension instanceof XMPMetadata) {
            pdfUtil.renderXMPMetadata((XMPMetadata)extension);
        } else if (extension instanceof Metadata) {
            XMPMetadata wrapper = new XMPMetadata(((Metadata)extension));
            pdfUtil.renderXMPMetadata(wrapper);
        } else {
            log.debug("Don't know how to handle extension object. Ignoring: "
                    + extension + " (" + extension.getClass().getName() + ")");
        }
    }

    PageReference getPageReference(int pageIndex) {
        return (PageReference)this.pageReferences.get(
                new Integer(pageIndex));
    }

    static final class PageReference {

        private PDFReference pageRef;
        private Dimension pageDimension;

        private PageReference(PDFPage page, Dimension dim) {
            this.pageRef = page.makeReference();
            this.pageDimension = new Dimension(dim);
        }

        public PDFReference getPageRef() {
            return this.pageRef;
        }

        public Dimension getPageDimension() {
            return this.pageDimension;
        }
    }

}
