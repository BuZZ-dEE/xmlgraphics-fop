/*
 * Copyright 1999-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: PDFRenderer.java,v 1.38 2004/04/07 14:24:17 cbowditch Exp $ */

package org.apache.fop.render.pdf;

// Java
import java.io.IOException;
import java.io.OutputStream;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

// XML
import org.w3c.dom.Document;

// Avalon
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

// FOP
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.area.BlockViewport;
import org.apache.fop.area.CTM;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.Page;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.Trait;
import org.apache.fop.area.OffDocumentItem;
import org.apache.fop.area.BookmarkData;
import org.apache.fop.area.inline.Character;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.InlineBlockParent;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.area.inline.Viewport;
import org.apache.fop.area.inline.ForeignObject;
import org.apache.fop.area.inline.Image;
import org.apache.fop.area.inline.Leader;
import org.apache.fop.area.inline.InlineParent;
import org.apache.fop.datatypes.ColorType;
import org.apache.fop.fonts.Typeface;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontSetup;
import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.image.FopImage;
import org.apache.fop.image.ImageFactory;
import org.apache.fop.image.XMLImage;
import org.apache.fop.pdf.PDFAnnotList;
import org.apache.fop.pdf.PDFColor;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFEncryptionManager;
import org.apache.fop.pdf.PDFFilterList;
import org.apache.fop.pdf.PDFInfo;
import org.apache.fop.pdf.PDFLink;
import org.apache.fop.pdf.PDFOutline;
import org.apache.fop.pdf.PDFPage;
import org.apache.fop.pdf.PDFResourceContext;
import org.apache.fop.pdf.PDFResources;
import org.apache.fop.pdf.PDFState;
import org.apache.fop.pdf.PDFStream;
import org.apache.fop.pdf.PDFText;
import org.apache.fop.pdf.PDFXObject;
import org.apache.fop.render.PrintRenderer;
import org.apache.fop.render.RendererContext;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.fo.Constants;


/*
todo:

word rendering and optimistion
pdf state optimisation
line and border
background pattern
writing mode
text decoration

*/

/**
 * Renderer that renders areas to PDF
 *
 */
public class PDFRenderer extends PrintRenderer {
    
    /**
     * The mime type for pdf
     */
    public static final String MIME_TYPE = "application/pdf";

    /** Controls whether comments are written to the PDF stream. */
    protected static final boolean WRITE_COMMENTS = true;
    
    /**
     * the PDF Document being created
     */
    protected PDFDocument pdfDoc;

    /**
     * Map of pages using the PageViewport as the key
     * this is used for prepared pages that cannot be immediately
     * rendered
     */
    protected Map pages = null;

    /**
     * Page references are stored using the PageViewport as the key
     * when a reference is made the PageViewport is used
     * for pdf this means we need the pdf page reference
     */
    protected Map pageReferences = new java.util.HashMap();

    /** Page viewport references */
    protected Map pvReferences = new java.util.HashMap();

    /**
     * The output stream to write the document to
     */
    protected OutputStream ostream;

    /**
     * the /Resources object of the PDF document being created
     */
    protected PDFResources pdfResources;

    /**
     * the current stream to add PDF commands to
     */
    protected PDFStream currentStream;

    /**
     * the current annotation list to add annotations to
     */
    protected PDFResourceContext currentContext = null;

    /**
     * the current page to add annotations to
     */
    protected PDFPage currentPage;
    
    protected AffineTransform currentBasicTransform;

    /** drawing state */
    protected PDFState currentState = null;

    /** Name of currently selected font */
    protected String currentFontName = "";
    /** Size of currently selected font */
    protected int currentFontSize = 0;
    /** page height */
    protected int pageHeight;

    /** Registry of PDF filters */
    protected Map filterMap;

    /**
     * true if a TJ command is left to be written
     */
    protected boolean textOpen = false;

    /**
     * true if a BT command has been written. 
     */
    protected boolean inTextMode = false;

    /**
     * the previous Y coordinate of the last word written.
     * Used to decide if we can draw the next word on the same line.
     */
    protected int prevWordY = 0;

    /**
     * the previous X coordinate of the last word written.
     * used to calculate how much space between two words
     */
    protected int prevWordX = 0;

    /**
     * The width of the previous word. Used to calculate space between
     */
    protected int prevWordWidth = 0;

    /**
     * reusable word area string buffer to reduce memory usage
     */
    //private StringBuffer wordAreaPDF = new StringBuffer();

    /**
     * create the PDF renderer
     */
    public PDFRenderer() {
    }

    /**
     * Configure the PDF renderer.
     * Get the configuration to be used for pdf stream filters,
     * fonts etc.
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration cfg) throws ConfigurationException {
        //PDF filters
        this.filterMap = PDFFilterList.buildFilterMapFromConfiguration(cfg);

        //Font configuration
        List cfgFonts = FontSetup.buildFontListFromConfiguration(cfg);
        if (this.fontList == null) {
            this.fontList = cfgFonts;
        } else {
            this.fontList.addAll(cfgFonts);
        }
    }

    /**
     * @see org.apache.fop.render.Renderer#setUserAgent(FOUserAgent)
     */
    public void setUserAgent(FOUserAgent agent) {
        super.setUserAgent(agent);
        PDFXMLHandler xmlHandler = new PDFXMLHandler();
        userAgent.getXMLHandlerRegistry().addXMLHandler(xmlHandler);
    }

    /**
     * @see org.apache.fop.render.Renderer#startRenderer(OutputStream)
     */
    public void startRenderer(OutputStream stream) throws IOException {
        if (userAgent == null) {
            throw new IllegalStateException("UserAgent must be set before starting the renderer");
        }
        ostream = stream;
        this.pdfDoc = new PDFDocument(
                userAgent.getProducer() != null ? userAgent.getProducer() : "");
        this.pdfDoc.setCreator(userAgent.getCreator());
        this.pdfDoc.setCreationDate(userAgent.getCreationDate());
        this.pdfDoc.getInfo().setAuthor(userAgent.getAuthor());
        this.pdfDoc.getInfo().setTitle(userAgent.getTitle());
        this.pdfDoc.getInfo().setKeywords(userAgent.getKeywords());
        this.pdfDoc.setFilterMap(filterMap);
        this.pdfDoc.outputHeader(stream);

        //Setup encryption if necessary
        PDFEncryptionManager.setupPDFEncryption(
                userAgent.getPDFEncryptionParams(), this.pdfDoc);
    }

    /**
     * @see org.apache.fop.render.Renderer#stopRenderer()
     */
    public void stopRenderer() throws IOException {
        pdfDoc.getResources().addFonts(pdfDoc, fontInfo);
        pdfDoc.outputTrailer(ostream);

        this.pdfDoc = null;
        ostream = null;

        pages = null;

        pageReferences.clear();
        pvReferences.clear();
        pdfResources = null;
        currentStream = null;
        currentContext = null;
        currentPage = null;
        currentState = null;
        currentFontName = "";
    }

    /**
     * @see org.apache.fop.render.Renderer#supportsOutOfOrder()
     */
    public boolean supportsOutOfOrder() {
        return false;
    }

    /**
     * @see org.apache.fop.render.Renderer#processOffDocumentItem(OffDocumentItem)
     */
    public void processOffDocumentItem(OffDocumentItem odi) {
        // render Bookmark-Tree
        if (odi instanceof BookmarkData) {
            renderBookmarkTree((BookmarkData) odi);
        }
    }

    /**
     * Renders a Bookmark-Tree object
     * @param bookmarks the BookmarkData object containing all the Bookmark-Items
     */
    protected void renderBookmarkTree(BookmarkData bookmarks) {
        for (int i = 0; i < bookmarks.getCount(); i++) {
            BookmarkData ext = bookmarks.getSubData(i);
            renderBookmarkItem(ext, null);
        }
    }

    private void renderBookmarkItem(BookmarkData bookmarkItem, 
            PDFOutline parentBookmarkItem) {
        PDFOutline pdfOutline = null;
        PageViewport pv = bookmarkItem.getPageViewport();
        if (pv != null) {
            Rectangle2D bounds = pv.getViewArea();
            double h = bounds.getHeight();
            float yoffset = (float)h / 1000f;
            String intDest = (String)pageReferences.get(pv.getKey());
            if (parentBookmarkItem == null) {
                PDFOutline outlineRoot = pdfDoc.getOutlineRoot();
                pdfOutline = pdfDoc.getFactory().makeOutline(outlineRoot,
                                        bookmarkItem.getBookmarkTitle(), 
                                        intDest, yoffset,
                                        bookmarkItem.showChildItems());
            } else {
                pdfOutline = pdfDoc.getFactory().makeOutline(parentBookmarkItem,
                                        bookmarkItem.getBookmarkTitle(), 
                                        intDest, yoffset, 
                                        bookmarkItem.showChildItems());
            }
        }

        for (int i = 0; i < bookmarkItem.getCount(); i++) {
            renderBookmarkItem(bookmarkItem.getSubData(i), pdfOutline);
        }
    }
    
    /** 
     * writes out a comment.
     * @param text text for the comment
     */
    protected void comment(String text) {
        if (WRITE_COMMENTS) {
            currentStream.add("% " + text + "\n");
        }
    }

    /** Saves the graphics state of the rendering engine. */
    protected void saveGraphicsState() {
        endTextObject();
        currentStream.add("q\n");
    }

    /** Restores the last graphics state of the rendering engine. */
    protected void restoreGraphicsState() {
        endTextObject();
        currentStream.add("Q\n");
    }

    /** Indicates the beginning of a text object. */
    protected void beginTextObject() {
        if (!inTextMode) {
            currentStream.add("BT\n");
            inTextMode = true;
        }
    }

    /** Indicates the end of a text object. */
    protected void endTextObject() {
        closeText();
        if (inTextMode) {
            currentStream.add("ET\n");
            inTextMode = false;
        }
    }

    /**
     * Start the next page sequence.
     * For the pdf renderer there is no concept of page sequences
     * but it uses the first available page sequence title to set
     * as the title of the pdf document.
     *
     * @param seqTitle the title of the page sequence
     */
    public void startPageSequence(LineArea seqTitle) {
        if (seqTitle != null) {
            String str = convertTitleToString(seqTitle);
            PDFInfo info = this.pdfDoc.getInfo();
            if (info.getTitle() == null) {
                info.setTitle(str);
            }
        }
    }

    /**
     * The pdf page is prepared by making the page.
     * The page is made in the pdf document without any contents
     * and then stored to add the contents later.
     * The page objects is stored using the area tree PageViewport
     * as a key.
     *
     * @param page the page to prepare
     */
    public void preparePage(PageViewport page) {
        this.pdfResources = this.pdfDoc.getResources();

        Rectangle2D bounds = page.getViewArea();
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        currentPage = this.pdfDoc.getFactory().makePage(
            this.pdfResources,
            (int) Math.round(w / 1000), (int) Math.round(h / 1000));
        if (pages == null) {
            pages = new java.util.HashMap();
        }
        pages.put(page, currentPage);
        pageReferences.put(page.getKey(), currentPage.referencePDF());
        pvReferences.put(page.getKey(), page);
    }

    /**
     * This method creates a pdf stream for the current page
     * uses it as the contents of a new page. The page is written
     * immediately to the output stream.
     * @see org.apache.fop.render.Renderer#renderPage(PageViewport)
     */
    public void renderPage(PageViewport page)
                throws IOException, FOPException {
        if (pages != null
                && (currentPage = (PDFPage) pages.get(page)) != null) {
            pages.remove(page);
            Rectangle2D bounds = page.getViewArea();
            double h = bounds.getHeight();
            pageHeight = (int) h;
        } else {
            this.pdfResources = this.pdfDoc.getResources();
            Rectangle2D bounds = page.getViewArea();
            double w = bounds.getWidth();
            double h = bounds.getHeight();
            pageHeight = (int) h;
            currentPage = this.pdfDoc.getFactory().makePage(
                this.pdfResources,
                (int) Math.round(w / 1000), (int) Math.round(h / 1000));
            pageReferences.put(page.getKey(), currentPage.referencePDF());
            pvReferences.put(page.getKey(), page);
        }
        currentStream = this.pdfDoc.getFactory()
            .makeStream(PDFFilterList.CONTENT_FILTER, false);

        currentState = new PDFState();
        /* This transform shouldn't affect PDFState as it only sets the basic
         * coordinate system for the rendering process.
         *
        currentState.setTransform(new AffineTransform(1, 0, 0, -1, 0,
                                   (int) Math.round(pageHeight / 1000)));
        */
        // Transform origin at top left to origin at bottom left
        currentStream.add("1 0 0 -1 0 "
                           + (int) Math.round(pageHeight / 1000) + " cm\n");
        currentBasicTransform = new AffineTransform(1, 0, 0, -1, 0,
                (int) Math.round(pageHeight / 1000));
        
        
        currentFontName = "";

        Page p = page.getPage();
        renderPageAreas(p);

        this.pdfDoc.registerObject(currentStream);
        currentPage.setContents(currentStream);
        PDFAnnotList annots = currentPage.getAnnotations();
        if (annots != null) {
            this.pdfDoc.addObject(annots);
        }
        this.pdfDoc.addObject(currentPage);
        this.pdfDoc.output(ostream);
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#startVParea(CTM)
     */
    protected void startVParea(CTM ctm) {
        // Set the given CTM in the graphics state
        currentState.push();
        currentState.setTransform(
                new AffineTransform(CTMHelper.toPDFArray(ctm)));

        saveGraphicsState();
        // multiply with current CTM
        currentStream.add(CTMHelper.toPDFString(ctm) + " cm\n");
        // Set clip?
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#endVParea()
     */
    protected void endVParea() {
        restoreGraphicsState();
        currentState.pop();
    }

    /**
     * Handle the traits for a region
     * This is used to draw the traits for the given page region.
     * (See Sect. 6.4.1.2 of XSL-FO spec.)
     * @param region the RegionViewport whose region is to be drawn
     */
    protected void handleRegionTraits(RegionViewport region) {
        currentFontName = "";
        Rectangle2D viewArea = region.getViewArea();
        float startx = (float)(viewArea.getX() / 1000f);
        float starty = (float)(viewArea.getY() / 1000f);
        float width = (float)(viewArea.getWidth() / 1000f);
        float height = (float)(viewArea.getHeight() / 1000f);

        if (region.getRegionReference().getRegionClass() == FO_REGION_BODY) {
            currentBPPosition = region.getBorderAndPaddingWidthBefore();
            currentIPPosition = region.getBorderAndPaddingWidthStart();
        }
        drawBackAndBorders(region, startx, starty, width, height);
    }

    /**
     * Handle block traits.
     * The block could be any sort of block with any positioning
     * so this should render the traits such as border and background
     * in its position.
     *
     * @param block the block to render the traits
     */
    protected void handleBlockTraits(Block block) {
        int borderPaddingStart = block.getBorderAndPaddingWidthStart();
        int borderPaddingBefore = block.getBorderAndPaddingWidthBefore();
        
        float startx = currentIPPosition / 1000f;
        float starty = currentBPPosition / 1000f;
        float width = block.getIPD() / 1000f;
        float height = block.getBPD() / 1000f;

        /* using start-indent now
        Integer spaceStart = (Integer) block.getTrait(Trait.SPACE_START);
        if (spaceStart != null) {
            startx += spaceStart.floatValue() / 1000f;
        }*/
        startx += block.getStartIndent() / 1000f;
        startx -= block.getBorderAndPaddingWidthStart() / 1000f;

        width += borderPaddingStart / 1000f;
        width += block.getBorderAndPaddingWidthEnd() / 1000f;
        height += borderPaddingBefore / 1000f;
        height += block.getBorderAndPaddingWidthAfter() / 1000f;

        drawBackAndBorders(block, startx, starty,
            width, height);
    }

    /**
     * Draw the background and borders.
     * This draws the background and border traits for an area given
     * the position.
     *
     * @param area the area to get the traits from
     * @param startx the start x position
     * @param starty the start y position
     * @param width the width of the area
     * @param height the height of the area
     */
    protected void drawBackAndBorders(Area area,
                    float startx, float starty,
                    float width, float height) {
        // draw background then border

        BorderProps bpsBefore = (BorderProps)area.getTrait(Trait.BORDER_BEFORE);
        BorderProps bpsAfter = (BorderProps)area.getTrait(Trait.BORDER_AFTER);
        BorderProps bpsStart = (BorderProps)area.getTrait(Trait.BORDER_START);
        BorderProps bpsEnd = (BorderProps)area.getTrait(Trait.BORDER_END);

        Trait.Background back;
        back = (Trait.Background)area.getTrait(Trait.BACKGROUND);
        if (back != null) {
            endTextObject();

            //Calculate padding rectangle
            float sx = startx;
            float sy = starty;
            float paddRectWidth = width;
            float paddRectHeight = height;
            if (bpsStart != null) {
                sx += bpsStart.width / 1000f;
                paddRectWidth -= bpsStart.width / 1000f;
            }
            if (bpsBefore != null) {
                sy += bpsBefore.width / 1000f;
                paddRectHeight -= bpsBefore.width / 1000f;
            }
            if (bpsEnd != null) {
                paddRectWidth -= bpsEnd.width / 1000f;
            }
            if (bpsAfter != null) {
                paddRectHeight -= bpsAfter.width / 1000f;
            }

            if (back.getColor() != null) {
                updateColor(back.getColor(), true, null);
                currentStream.add(sx + " " + sy + " "
                                  + paddRectWidth + " " + paddRectHeight + " re\n");
                currentStream.add("f\n");
            }
            if (back.getFopImage() != null) {
                FopImage fopimage = back.getFopImage();
                if (fopimage != null && fopimage.load(FopImage.DIMENSIONS)) {
                    saveGraphicsState();
                    clip(sx, sy, paddRectWidth, paddRectHeight);
                    int horzCount = (int)((paddRectWidth 
                            * 1000 / fopimage.getIntrinsicWidth()) + 1.0f); 
                    int vertCount = (int)((paddRectHeight 
                            * 1000 / fopimage.getIntrinsicHeight()) + 1.0f); 
                    if (back.getRepeat() == EN_NOREPEAT) {
                        horzCount = 1;
                        vertCount = 1;
                    } else if (back.getRepeat() == EN_REPEATX) {
                        vertCount = 1;
                    } else if (back.getRepeat() == EN_REPEATY) {
                        horzCount = 1;
                    }
                    //change from points to millipoints
                    sx *= 1000;
                    sy *= 1000;
                    if (horzCount == 1) {
                        sx += back.getHoriz();
                    }
                    if (vertCount == 1) {
                        sy += back.getVertical();
                    }
                    for (int x = 0; x < horzCount; x++) {
                        for (int y = 0; y < vertCount; y++) {
                            // place once
                            Rectangle2D pos;
                            pos = new Rectangle2D.Float(sx + (x * fopimage.getIntrinsicWidth()),
                                                        sy + (y * fopimage.getIntrinsicHeight()),
                                                        fopimage.getIntrinsicWidth(),
                                                        fopimage.getIntrinsicHeight());
                            putImage(back.getURL(), pos);
                        }
                    }
                    
                    restoreGraphicsState();
                } else {
                    log.warn("Can't find background image: " + back.getURL());
                }
            }
        }

        boolean[] b = new boolean[] {
            (bpsBefore != null), (bpsEnd != null), 
            (bpsAfter != null), (bpsStart != null)};
        if (!b[0] && !b[1] && !b[2] && !b[3]) {
            return;
        }
        float[] bw = new float[] {
            (b[0] ? bpsBefore.width / 1000f : 0.0f),
            (b[1] ? bpsEnd.width / 1000f : 0.0f),
            (b[2] ? bpsAfter.width / 1000f : 0.0f),
            (b[3] ? bpsStart.width / 1000f : 0.0f)};
        float[] clipw = new float[] {
            BorderProps.getClippedWidth(bpsBefore) / 1000f,    
            BorderProps.getClippedWidth(bpsEnd) / 1000f,    
            BorderProps.getClippedWidth(bpsAfter) / 1000f,    
            BorderProps.getClippedWidth(bpsStart) / 1000f};
        starty += clipw[0];
        height -= clipw[0];
        height -= clipw[2];
        startx += clipw[3];
        width -= clipw[3];
        width -= clipw[1];
        
        boolean[] slant = new boolean[] {
            (b[3] && b[0]), (b[0] && b[1]), (b[1] && b[2]), (b[2] && b[3])};
        if (bpsBefore != null) {
            endTextObject();

            float sx1 = startx;
            float sx2 = (slant[0] ? sx1 + bw[3] - clipw[3] : sx1);
            float ex1 = startx + width;
            float ex2 = (slant[1] ? ex1 - bw[1] + clipw[1] : ex1);
            float outery = starty - clipw[0];
            float clipy = outery + clipw[0];
            float innery = outery + bw[0];

            saveGraphicsState();
            moveTo(sx1, clipy);
            float sx1a = sx1;
            float ex1a = ex1;
            if (bpsBefore.mode == BorderProps.COLLAPSE_OUTER) {
                if (bpsStart != null && bpsStart.mode == BorderProps.COLLAPSE_OUTER) {
                    sx1a -= clipw[3];
                }
                if (bpsEnd != null && bpsEnd.mode == BorderProps.COLLAPSE_OUTER) {
                    ex1a += clipw[1];
                }
                lineTo(sx1a, outery);
                lineTo(ex1a, outery);
            }
            lineTo(ex1, clipy);
            lineTo(ex2, innery);
            lineTo(sx2, innery);
            closePath();
            clip();
            drawBorderLine(sx1a, outery, ex1a, innery, true, true, 
                    bpsBefore.style, bpsBefore.color);
            restoreGraphicsState();
        }
        if (bpsEnd != null) {
            endTextObject();

            float sy1 = starty;
            float sy2 = (slant[1] ? sy1 + bw[0] - clipw[0] : sy1);
            float ey1 = starty + height;
            float ey2 = (slant[2] ? ey1 - bw[2] + clipw[2] : ey1);
            float outerx = startx + width + clipw[1];
            float clipx = outerx - clipw[1];
            float innerx = outerx - bw[1];
            
            saveGraphicsState();
            moveTo(clipx, sy1);
            float sy1a = sy1;
            float ey1a = ey1;
            if (bpsEnd.mode == BorderProps.COLLAPSE_OUTER) {
                if (bpsBefore != null && bpsBefore.mode == BorderProps.COLLAPSE_OUTER) {
                    sy1a -= clipw[0];
                }
                if (bpsAfter != null && bpsAfter.mode == BorderProps.COLLAPSE_OUTER) {
                    ey1a += clipw[2];
                }
                lineTo(outerx, sy1a);
                lineTo(outerx, ey1a);
            }
            lineTo(clipx, ey1);
            lineTo(innerx, ey2);
            lineTo(innerx, sy2);
            closePath();
            clip();
            drawBorderLine(innerx, sy1a, outerx, ey1a, false, false, bpsEnd.style, bpsEnd.color);
            restoreGraphicsState();
        }
        if (bpsAfter != null) {
            endTextObject();

            float sx1 = startx;
            float sx2 = (slant[3] ? sx1 + bw[3] - clipw[3] : sx1);
            float ex1 = startx + width;
            float ex2 = (slant[2] ? ex1 - bw[1] + clipw[1] : ex1);
            float outery = starty + height + clipw[2];
            float clipy = outery - clipw[2];
            float innery = outery - bw[2];

            saveGraphicsState();
            moveTo(ex1, clipy);
            float sx1a = sx1;
            float ex1a = ex1;
            if (bpsAfter.mode == BorderProps.COLLAPSE_OUTER) {
                if (bpsStart != null && bpsStart.mode == BorderProps.COLLAPSE_OUTER) {
                    sx1a -= clipw[3];
                }
                if (bpsEnd != null && bpsEnd.mode == BorderProps.COLLAPSE_OUTER) {
                    ex1a += clipw[1];
                }
                lineTo(ex1a, outery);
                lineTo(sx1a, outery);
            }
            lineTo(sx1, clipy);
            lineTo(sx2, innery);
            lineTo(ex2, innery);
            closePath();
            clip();
            drawBorderLine(sx1a, innery, ex1a, outery, true, false, bpsAfter.style, bpsAfter.color);
            restoreGraphicsState();
        }
        if (bpsStart != null) {
            endTextObject();

            float sy1 = starty;
            float sy2 = (slant[0] ? sy1 + bw[0] - clipw[0] : sy1);
            float ey1 = sy1 + height;
            float ey2 = (slant[3] ? ey1 - bw[2] + clipw[2] : ey1);
            float outerx = startx - clipw[3];
            float clipx = outerx + clipw[3];
            float innerx = outerx + bw[3];

            saveGraphicsState();
            moveTo(clipx, ey1);
            float sy1a = sy1;
            float ey1a = ey1;
            if (bpsStart.mode == BorderProps.COLLAPSE_OUTER) {
                if (bpsBefore != null && bpsBefore.mode == BorderProps.COLLAPSE_OUTER) {
                    sy1a -= clipw[0];
                }
                if (bpsAfter != null && bpsAfter.mode == BorderProps.COLLAPSE_OUTER) {
                    ey1a += clipw[2];
                }
                lineTo(outerx, ey1a);
                lineTo(outerx, sy1a);
            }
            lineTo(clipx, sy1);
            lineTo(innerx, sy2);
            lineTo(innerx, ey2);
            closePath();
            clip();
            drawBorderLine(outerx, sy1a, innerx, ey1a, false, true, bpsStart.style, bpsStart.color);
            restoreGraphicsState();
        }
    }
    
    private Color lightenColor(Color col, float factor) {
        float[] cols = new float[3];
        cols = col.getColorComponents(cols);
        if (factor > 0) {
            cols[0] += (1.0 - cols[0]) * factor;
            cols[1] += (1.0 - cols[1]) * factor;
            cols[2] += (1.0 - cols[2]) * factor;
        } else {
            cols[0] -= cols[0] * -factor;
            cols[1] -= cols[1] * -factor;
            cols[2] -= cols[2] * -factor;
        }
        return new Color(cols[0], cols[1], cols[2]);
    }

    private void drawBorderLine(float x1, float y1, float x2, float y2, 
            boolean horz, boolean startOrBefore, int style, ColorType col) {
        float w = x2 - x1;
        float h = y2 - y1;
        if ((w < 0) || (h < 0)) {
            log.error("Negative extent received. Border won't be painted.");
            return;
        }
        switch (style) {
            case Constants.EN_DASHED: 
                setColor(toColor(col), false, null);
                if (horz) {
                    float unit = Math.abs(2 * h);
                    int rep = (int)(w / unit);
                    if (rep % 2 == 0) {
                        rep++;
                    }
                    unit = w / rep;
                    currentStream.add("[" + unit + "] 0 d ");
                    currentStream.add(h + " w\n");
                    float ym = y1 + (h / 2);
                    currentStream.add(x1 + " " + ym + " m " + x2 + " " + ym + " l S\n");
                } else {
                    float unit = Math.abs(2 * w);
                    int rep = (int)(h / unit);
                    if (rep % 2 == 0) {
                        rep++;
                    }
                    unit = h / rep;
                    currentStream.add("[" + unit + "] 0 d ");
                    currentStream.add(w + " w\n");
                    float xm = x1 + (w / 2);
                    currentStream.add(xm + " " + y1 + " m " + xm + " " + y2 + " l S\n");
                }
                break;
            case Constants.EN_DOTTED:
                setColor(toColor(col), false, null);
                currentStream.add("1 J ");
                if (horz) {
                    float unit = Math.abs(2 * h);
                    int rep = (int)(w / unit);
                    if (rep % 2 == 0) {
                        rep++;
                    }
                    unit = w / rep;
                    currentStream.add("[0 " + unit + "] 0 d ");
                    currentStream.add(h + " w\n");
                    float ym = y1 + (h / 2);
                    currentStream.add(x1 + " " + ym + " m " + x2 + " " + ym + " l S\n");
                } else {
                    float unit = Math.abs(2 * w);
                    int rep = (int)(h / unit);
                    if (rep % 2 == 0) {
                        rep++;
                    }
                    unit = h / rep;
                    currentStream.add("[0 " + unit + " ] 0 d ");
                    currentStream.add(w + " w\n");
                    float xm = x1 + (w / 2);
                    currentStream.add(xm + " " + y1 + " m " + xm + " " + y2 + " l S\n");
                }
                break;
            case Constants.EN_DOUBLE:
                setColor(toColor(col), false, null);
                currentStream.add("[] 0 d ");
                if (horz) {
                    float h3 = h / 3;
                    currentStream.add(h3 + " w\n");
                    float ym1 = y1 + (h3 / 2);
                    float ym2 = ym1 + h3 + h3;
                    currentStream.add(x1 + " " + ym1 + " m " + x2 + " " + ym1 + " l S\n");
                    currentStream.add(x1 + " " + ym2 + " m " + x2 + " " + ym2 + " l S\n");
                } else {
                    float w3 = w / 3;
                    currentStream.add(w3 + " w\n");
                    float xm1 = x1 + (w3 / 2);
                    float xm2 = xm1 + w3 + w3;
                    currentStream.add(xm1 + " " + y1 + " m " + xm1 + " " + y2 + " l S\n");
                    currentStream.add(xm2 + " " + y1 + " m " + xm2 + " " + y2 + " l S\n");
                }
                break;
            case Constants.EN_GROOVE:
            case Constants.EN_RIDGE:
            {
                float colFactor = (style == EN_GROOVE ? 0.4f : -0.4f);
                currentStream.add("[] 0 d ");
                Color c = toColor(col);
                if (horz) {
                    Color uppercol = lightenColor(c, -colFactor);
                    Color lowercol = lightenColor(c, colFactor);
                    float h3 = h / 3;
                    currentStream.add(h3 + " w\n");
                    float ym1 = y1 + (h3 / 2);
                    setColor(uppercol, false, null);
                    currentStream.add(x1 + " " + ym1 + " m " + x2 + " " + ym1 + " l S\n");
                    setColor(c, false, null);
                    currentStream.add(x1 + " " + (ym1 + h3) + " m " + x2 + " " + (ym1 + h3) + " l S\n");
                    setColor(lowercol, false, null);
                    currentStream.add(x1 + " " + (ym1 + h3 + h3) + " m " + x2 + " " + (ym1 + h3 + h3) + " l S\n");
                } else {
                    Color leftcol = lightenColor(c, -colFactor);
                    Color rightcol = lightenColor(c, colFactor);
                    float w3 = w / 3;
                    currentStream.add(w3 + " w\n");
                    float xm1 = x1 + (w3 / 2);
                    setColor(leftcol, false, null);
                    currentStream.add(xm1 + " " + y1 + " m " + xm1 + " " + y2 + " l S\n");
                    setColor(c, false, null);
                    currentStream.add((xm1 + w3) + " " + y1 + " m " + (xm1 + w3) + " " + y2 + " l S\n");
                    setColor(rightcol, false, null);
                    currentStream.add((xm1 + w3 + w3) + " " + y1 + " m " + (xm1 + w3 + w3) + " " + y2 + " l S\n");
                }
                break;
            }
            case Constants.EN_INSET:
            case Constants.EN_OUTSET:
            {
                float colFactor = (style == EN_OUTSET ? 0.4f : -0.4f);
                currentStream.add("[] 0 d ");
                Color c = toColor(col);
                if (horz) {
                    c = lightenColor(c, (startOrBefore ? 1 : -1) * colFactor);
                    currentStream.add(h + " w\n");
                    float ym1 = y1 + (h / 2);
                    setColor(c, false, null);
                    currentStream.add(x1 + " " + ym1 + " m " + x2 + " " + ym1 + " l S\n");
                } else {
                    c = lightenColor(c, (startOrBefore ? 1 : -1) * colFactor);
                    currentStream.add(w + " w\n");
                    float xm1 = x1 + (w / 2);
                    setColor(c, false, null);
                    currentStream.add(xm1 + " " + y1 + " m " + xm1 + " " + y2 + " l S\n");
                }
                break;
            }
            case Constants.EN_HIDDEN:
                break;
            default:
                setColor(toColor(col), false, null);
                currentStream.add("[] 0 d ");
                if (horz) {
                    currentStream.add(h + " w\n");
                    float ym = y1 + (h / 2);
                    currentStream.add(x1 + " " + ym + " m " + x2 + " " + ym + " l S\n");
                } else {
                    currentStream.add(w + " w\n");
                    float xm = x1 + (w / 2);
                    currentStream.add(xm + " " + y1 + " m " + xm + " " + y2 + " l S\n");
                }
        }
    }
    
    /**
     * Sets the current line width in points.
     * @param width line width in points
     */
    private void updateLineWidth(float width) {
        if (currentState.setLineWidth(width)) {
            //Only write if value has changed WRT the current line width
            currentStream.add(width + " w\n");
        }
    }
    
    private void updateLineStyle(int style) {
        switch (style) {
            case Constants.EN_DASHED:
                currentStream.add("[3] 0 d\n");
                break;
            case Constants.EN_DOTTED:
                currentStream.add("[1 7] 0 d\n");
                break;
            default:
                // solid
                currentStream.add("[] 0 d\n");
                break;
        }
    }

    /**
     * Moves the current point to (x, y), omitting any connecting line segment. 
     * @param x x coordinate
     * @param y y coordinate
     */
    private void moveTo(float x, float y) {
        currentStream.add(x + " " + y + " m ");
    }
    
    /**
     * Appends a straight line segment from the current point to (x, y). The 
     * new current point is (x, y). 
     * @param x x coordinate
     * @param y y coordinate
     */
    private void lineTo(float x, float y) {
        currentStream.add(x + " " + y + " l ");
    }
    
    /**
     * Closes the current subpath by appending a straight line segment from 
     * the current point to the starting point of the subpath.
     */
    private void closePath() {
        currentStream.add("h ");
    }
    
    /**
     * Draw a line.
     *
     * @param startx the start x position
     * @param starty the start y position
     * @param endx the x end position
     * @param endy the y end position
     */
    private void drawLine(float startx, float starty, float endx, float endy) {
        currentStream.add(startx + " " + starty + " m ");
        currentStream.add(endx + " " + endy + " l S\n");
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderBlockViewport(BlockViewport, List)
     */
    protected void renderBlockViewport(BlockViewport bv, List children) {
        // clip and position viewport if necessary

        // save positions
        int saveIP = currentIPPosition;
        int saveBP = currentBPPosition;
        String saveFontName = currentFontName;

        CTM ctm = bv.getCTM();
        int borderPaddingStart = bv.getBorderAndPaddingWidthStart();
        int borderPaddingBefore = bv.getBorderAndPaddingWidthBefore();
        float x, y;
        x = (float)(bv.getXOffset() + containingIPPosition) / 1000f;
        y = (float)(bv.getYOffset() + containingBPPosition) / 1000f;

        if (bv.getPositioning() == Block.ABSOLUTE
                || bv.getPositioning() == Block.FIXED) {

            //For FIXED, we need to break out of the current viewports to the
            //one established by the page. We save the state stack for restoration
            //after the block-container has been painted. See below.
            List breakOutList = null;
            if (bv.getPositioning() == Block.FIXED) {
                //break out
                breakOutList = new java.util.ArrayList();
                PDFState.Data data;
                while (true) {
                    data = currentState.getData();
                    if (currentState.pop() == null) {
                        break;
                    }
                    if (breakOutList.size() == 0) {
                        comment("------ break out!");
                    }
                    breakOutList.add(0, data); //Insert because of stack-popping
                    //getLogger().debug("Adding to break out list: " + data);
                    restoreGraphicsState();
                }
            }
            
            CTM tempctm = new CTM(containingIPPosition, containingBPPosition);
            ctm = tempctm.multiply(ctm);

            //This is the content-rect
            float width = (float)bv.getIPD() / 1000f;
            float height = (float)bv.getBPD() / 1000f;
            
            //Adjust for spaces (from margin or indirectly by start-indent etc.
            Integer spaceStart = (Integer) bv.getTrait(Trait.SPACE_START);
            if (spaceStart != null) {
                x += spaceStart.floatValue() / 1000;
            }
            Integer spaceBefore = (Integer) bv.getTrait(Trait.SPACE_BEFORE);
            if (spaceBefore != null) {
                y += spaceBefore.floatValue() / 1000;
            }

            float bpwidth = (borderPaddingStart + bv.getBorderAndPaddingWidthEnd()) / 1000f;
            float bpheight = (borderPaddingBefore + bv.getBorderAndPaddingWidthAfter()) / 1000f;

            drawBackAndBorders(bv, x, y, width + bpwidth, height + bpheight);

            //Now adjust for border/padding
            x += borderPaddingStart / 1000f;
            y += borderPaddingBefore / 1000f;
            
            if (bv.getClip()) {
                saveGraphicsState();
                clip(x, y, width, height);
            }

            startVParea(ctm);

            currentIPPosition = 0;
            currentBPPosition = 0;

            renderBlocks(bv, children);
            endVParea();

            if (bv.getClip()) {
                restoreGraphicsState();
            }

            // clip if necessary

            if (breakOutList != null) {
                comment("------ restoring context after break-out...");
                PDFState.Data data;
                Iterator i = breakOutList.iterator();
                while (i.hasNext()) {
                    data = (PDFState.Data)i.next();
                    //getLogger().debug("Restoring: " + data);
                    currentState.push();
                    saveGraphicsState();
                    if (data.concatenations != null) {
                        Iterator tr = data.concatenations.iterator();
                        while (tr.hasNext()) {
                            AffineTransform at = (AffineTransform)tr.next();
                            currentState.setTransform(at);
                            double[] matrix = new double[6];
                            at.getMatrix(matrix);
                            tempctm = new CTM(matrix[0], matrix[1], matrix[2], matrix[3], 
                                    matrix[4] * 1000, matrix[5] * 1000);
                            currentStream.add(CTMHelper.toPDFString(tempctm) + " cm\n");
                        }
                    }
                    //TODO Break-out: Also restore items such as line width and color
                    //Left out for now because all this painting stuff is very
                    //inconsistent. Some values go over PDFState, some don't.
                }
                comment("------ done.");
            }
            
            currentIPPosition = saveIP;
            currentBPPosition = saveBP;
        } else {

            Integer spaceBefore = (Integer)bv.getTrait(Trait.SPACE_BEFORE);
            if (spaceBefore != null) {
                currentBPPosition += spaceBefore.intValue();
            }

            //borders and background in the old coordinate system
            handleBlockTraits(bv);

            CTM tempctm = new CTM(containingIPPosition, currentBPPosition);
            ctm = tempctm.multiply(ctm);
            
            //Now adjust for border/padding
            x += borderPaddingStart / 1000f;
            y += borderPaddingBefore / 1000f;

            // clip if necessary
            if (bv.getClip()) {
                saveGraphicsState();
                float width = (float)bv.getIPD() / 1000f;
                float height = (float)bv.getBPD() / 1000f;
                clip(x, y, width, height);
            }

            if (ctm != null) {
                startVParea(ctm);
                currentIPPosition = 0;
                currentBPPosition = 0;
            }
            renderBlocks(bv, children);
            if (ctm != null) {
                endVParea();
            }

            if (bv.getClip()) {
                restoreGraphicsState();
            }

            currentIPPosition = saveIP;
            currentBPPosition = saveBP;
            
            //Adjust BP position (alloc BPD + spaces)
            if (spaceBefore != null) {
                currentBPPosition += spaceBefore.intValue();
            }
            currentBPPosition += (int)(bv.getAllocBPD());
            Integer spaceAfter = (Integer)bv.getTrait(Trait.SPACE_AFTER);
            if (spaceAfter != null) {
                currentBPPosition += spaceAfter.intValue();
            }
        }
        currentFontName = saveFontName;
    }

    /**
     * Clip an area.
     * write a clipping operation given coordinates in the current
     * transform.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the area
     * @param height the height of the area
     */
    protected void clip(float x, float y, float width, float height) {
        currentStream.add(x + " " + y + " " + width + " " + height + " re ");
        clip();
    }

    /**
     * Clip an area.
     */
    protected void clip() {
        currentStream.add("W\n");
        currentStream.add("n\n");
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderLineArea(LineArea)
     */
    protected void renderLineArea(LineArea line) {
        super.renderLineArea(line);
        closeText();
    }

    /**
     * Render inline parent area.
     * For pdf this handles the inline parent area traits such as
     * links, border, background.
     * @param ip the inline parent area
     */
    public void renderInlineParent(InlineParent ip) {
        float start = currentIPPosition / 1000f;
        float top = (ip.getOffset() + currentBPPosition) / 1000f;
        float width = ip.getIPD() / 1000f;
        float height = ip.getBPD() / 1000f;
        drawBackAndBorders(ip, start, top, width, height);

        // render contents
        super.renderInlineParent(ip);

        // place the link over the top
        Object tr = ip.getTrait(Trait.INTERNAL_LINK);
        boolean internal = false;
        String dest = null;
        float yoffset = 0;
        if (tr == null) {
            dest = (String)ip.getTrait(Trait.EXTERNAL_LINK);
        } else {
            String pvKey = (String)tr;
            dest = (String)pageReferences.get(pvKey);
            if (dest != null) {
                PageViewport pv = (PageViewport)pvReferences.get(pvKey);
                Rectangle2D bounds = pv.getViewArea();
                double h = bounds.getHeight();
                yoffset = (float)h / 1000f;
                internal = true;
            }
        }
        if (dest != null) {
            // add link to pdf document
            Rectangle2D rect = new Rectangle2D.Float(start, top, width, height);
            // transform rect to absolute coords
            AffineTransform transform = currentState.getTransform();
            rect = transform.createTransformedShape(rect).getBounds2D();
            rect = currentBasicTransform.createTransformedShape(rect).getBounds2D();

            int type = internal ? PDFLink.INTERNAL : PDFLink.EXTERNAL;
            PDFLink pdflink = pdfDoc.getFactory().makeLink(
                        rect, dest, type, yoffset);
            currentPage.addAnnotation(pdflink);
        }
    }

    /** @see org.apache.fop.render.AbstractRenderer */
    protected void renderInlineBlockParent(InlineBlockParent ibp) {
        float start = currentIPPosition / 1000f;
        float top = (ibp.getOffset() + currentBPPosition) / 1000f;
        float width = ibp.getIPD() / 1000f;
        float height = ibp.getBPD() / 1000f;
        drawBackAndBorders(ibp, start, top, width, height);
        
        super.renderInlineBlockParent(ibp);
    }
    
    /**
     * @see org.apache.fop.render.Renderer#renderCharacter(Character)
     */
    public void renderCharacter(Character ch) {
        StringBuffer pdf = new StringBuffer();

        String name = (String) ch.getTrait(Trait.FONT_NAME);
        int size = ((Integer) ch.getTrait(Trait.FONT_SIZE)).intValue();

        // This assumes that *all* CIDFonts use a /ToUnicode mapping
        Typeface f = (Typeface) fontInfo.getFonts().get(name);
        boolean useMultiByte = f.isMultiByte();

        // String startText = useMultiByte ? "<FEFF" : "(";
        String startText = useMultiByte ? "<" : "(";
        String endText = useMultiByte ? "> " : ") ";

        updateFont(name, size, pdf);
        ColorType ct = (ColorType) ch.getTrait(Trait.COLOR);
        if (ct != null) {
            updateColor(ct, true, pdf);
        }

        // word.getOffset() = only height of text itself
        // currentBlockIPPosition: 0 for beginning of line; nonzero
        //  where previous line area failed to take up entire allocated space
        int rx = currentIPPosition;
        int bl = currentBPPosition + ch.getOffset();

/*        System.out.println("Text = " + ch.getTextArea() +
            "; text width: " + ch.getWidth() +
            "; BlockIP Position: " + currentBlockIPPosition +
            "; currentBPPosition: " + currentBPPosition +
            "; offset: " + ch.getOffset());
*/
        // Set letterSpacing
        //float ls = fs.getLetterSpacing() / this.currentFontSize;
        //pdf.append(ls).append(" Tc\n");

        if (!textOpen || bl != prevWordY) {
            closeText();

            pdf.append("1 0 0 -1 " + (rx / 1000f) + " " + (bl / 1000f) + " Tm "
                       + (ch.getTextLetterSpaceAdjust() / 1000f) + " Tc "
                       + (ch.getTextWordSpaceAdjust() / 1000f) + " Tw [" + startText);
            prevWordY = bl;
            textOpen = true;
        } else {
            closeText();

            pdf.append("1 0 0 -1 " + (rx / 1000f) + " " + (bl / 1000f) + " Tm "
                           + (ch.getTextLetterSpaceAdjust() / 1000f) + " Tc "
                           + (ch.getTextWordSpaceAdjust() / 1000f) + " Tw [" + startText);
            textOpen = true;
        }
        prevWordWidth = ch.getIPD();
        prevWordX = rx;

        String s = ch.getChar();


        FontMetrics metrics = fontInfo.getMetricsFor(name);
        Font fs = new Font(name, metrics, size);
        escapeText(s, fs, useMultiByte, pdf);
        pdf.append(endText);

        currentStream.add(pdf.toString());

        renderTextDecoration(fs, ch, bl, rx);
        
        super.renderCharacter(ch);
    }

    /**
     * @see org.apache.fop.render.Renderer#renderText(TextArea)
     */
    public void renderText(TextArea text) {
        beginTextObject();
        StringBuffer pdf = new StringBuffer();

        String name = (String) text.getTrait(Trait.FONT_NAME);
        int size = ((Integer) text.getTrait(Trait.FONT_SIZE)).intValue();
        
        // This assumes that *all* CIDFonts use a /ToUnicode mapping
        Typeface f = (Typeface) fontInfo.getFonts().get(name);
        boolean useMultiByte = f.isMultiByte();

        // String startText = useMultiByte ? "<FEFF" : "(";
        String startText = useMultiByte ? "<" : "(";
        String endText = useMultiByte ? "> " : ") ";

        updateFont(name, size, pdf);
        ColorType ct = (ColorType) text.getTrait(Trait.COLOR);
        updateColor(ct, true, pdf);

        // word.getOffset() = only height of text itself
        // currentBlockIPPosition: 0 for beginning of line; nonzero
        //  where previous line area failed to take up entire allocated space
        int rx = currentIPPosition;
        int bl = currentBPPosition + text.getOffset();

/*        System.out.println("Text = " + text.getTextArea() +
            "; text width: " + text.getWidth() +
            "; BlockIP Position: " + currentBlockIPPosition +
            "; currentBPPosition: " + currentBPPosition +
            "; offset: " + text.getOffset());
*/
        // Set letterSpacing
        //float ls = fs.getLetterSpacing() / this.currentFontSize;
        //pdf.append(ls).append(" Tc\n");

        if (!textOpen || bl != prevWordY) {
            closeText();

            pdf.append("1 0 0 -1 " + (rx / 1000f) + " " + (bl / 1000f) + " Tm "
                       + (text.getTextLetterSpaceAdjust() / 1000f) + " Tc "
                       + (text.getTextWordSpaceAdjust() / 1000f) + " Tw [" + startText);
            prevWordY = bl;
            textOpen = true;
        } else {
            closeText();

            pdf.append("1 0 0 -1 " + (rx / 1000f) + " " + (bl / 1000f) + " Tm "
                       + (text.getTextLetterSpaceAdjust() / 1000f) + " Tc "
                       + (text.getTextWordSpaceAdjust() / 1000f) + " Tw [" + startText);
            textOpen = true;
        }
        prevWordWidth = text.getIPD();
        prevWordX = rx;

        String s = text.getTextArea();

        FontMetrics metrics = fontInfo.getMetricsFor(name);
        Font fs = new Font(name, metrics, size);
        escapeText(s, fs, useMultiByte, pdf);
        pdf.append(endText);

        currentStream.add(pdf.toString());

        renderTextDecoration(fs, text, bl, rx);
        
        super.renderText(text);
    }
    
    /**
     * Paints the text decoration marks.
     * @param fs Current font
     * @param inline inline area to paint the marks for
     * @param baseline position of the baseline
     * @param startx start IPD
     */
    protected void renderTextDecoration(Font fs, InlineArea inline, 
                    int baseline, int startx) {
        boolean hasTextDeco = inline.hasUnderline() 
                || inline.hasOverline() 
                || inline.hasLineThrough();
        if (hasTextDeco) {
            endTextObject();
            updateLineStyle(Constants.EN_SOLID);
            updateLineWidth(fs.getDescender() / -8 / 1000f);
            float endx = (startx + inline.getIPD()) / 1000f;
            if (inline.hasUnderline()) {
                ColorType ct = (ColorType) inline.getTrait(Trait.UNDERLINE_COLOR);
                updateColor(ct, false, null);
                float y = baseline - fs.getDescender() / 2;
                drawLine(startx / 1000f, y / 1000f, endx, y / 1000f);
            }
            if (inline.hasOverline()) {
                ColorType ct = (ColorType) inline.getTrait(Trait.OVERLINE_COLOR);
                updateColor(ct, false, null);
                float y = (float)(baseline - (1.1 * fs.getCapHeight()));
                drawLine(startx / 1000f, y / 1000f, endx, y / 1000f);
            }
            if (inline.hasLineThrough()) {
                ColorType ct = (ColorType) inline.getTrait(Trait.LINETHROUGH_COLOR);
                updateColor(ct, false, null);
                float y = (float)(baseline - (0.45 * fs.getCapHeight()));
                drawLine(startx / 1000f, y / 1000f, endx, y / 1000f);
            }
        }
    }

    /**
     * Escapes text according to PDF rules.
     * @param s Text to escape
     * @param fs Font state
     * @param useMultiByte Indicates the use of multi byte convention
     * @param pdf target buffer for the escaped text
     */
    public void escapeText(String s, Font fs,
                           boolean useMultiByte, StringBuffer pdf) {
        String startText = useMultiByte ? "<" : "(";
        String endText = useMultiByte ? "> " : ") ";

        boolean kerningAvailable = false;
        Map kerning = fs.getKerning();
        if (kerning != null && !kerning.isEmpty()) {
            kerningAvailable = true;
        }

        int l = s.length();

        for (int i = 0; i < l; i++) {
            char ch = fs.mapChar(s.charAt(i));

            if (!useMultiByte) {
                if (ch > 127) {
                    pdf.append("\\");
                    pdf.append(Integer.toOctalString((int) ch));
                } else {
                    switch (ch) {
                        case '(':
                        case ')':
                        case '\\':
                            pdf.append("\\");
                            break;
                    }
                    pdf.append(ch);
                }
            } else {
                pdf.append(PDFText.toUnicodeHex(ch));
            }

            if (kerningAvailable && (i + 1) < l) {
                addKerning(pdf, (new Integer((int) ch)),
                           (new Integer((int) fs.mapChar(s.charAt(i + 1)))
                           ), kerning, startText, endText);
            }
        }
    }

    private void addKerning(StringBuffer buf, Integer ch1, Integer ch2,
                            Map kerning, String startText, String endText) {
        Map kernPair = (Map) kerning.get(ch1);

        if (kernPair != null) {
            Integer width = (Integer) kernPair.get(ch2);
            if (width != null) {
                buf.append(endText).append(-width.intValue());
                buf.append(' ').append(startText);
            }
        }
    }

    /**
     * Checks to see if we have some text rendering commands open
     * still and writes out the TJ command to the stream if we do
     */
    protected void closeText() {
        if (textOpen) {
            currentStream.add("] TJ\n");
            textOpen = false;
            prevWordX = 0;
            prevWordY = 0;
            currentFontName = "";
        }
    }

    /**
     * Establishes a new foreground or fill color. In contrast to updateColor
     * this method does not check the PDFState for optimization possibilities.
     * @param col the color to apply 
     * @param fill true to set the fill color, false for the foreground color
     * @param pdf StringBuffer to write the PDF code to, if null, the code is
     *     written to the current stream.
     */
    protected void setColor(Color col, boolean fill, StringBuffer pdf) {
        PDFColor color = new PDFColor(col);

        closeText();
        
        if (pdf != null) {
            pdf.append(color.getColorSpaceOut(fill));
        } else {
            currentStream.add(color.getColorSpaceOut(fill));
        }
    }
    
    /**
     * Converts a ColorType to a java.awt.Color (sRGB).
     * @param col the color
     * @return the converted color
     */
    private Color toColor(ColorType col) {
        return new Color(col.getRed(), col.getGreen(), col.getBlue());
    }
    
    /**
     * Establishes a new foreground or fill color.
     * @param col the color to apply (null skips this operation)
     * @param fill true to set the fill color, false for the foreground color
     * @param pdf StringBuffer to write the PDF code to, if null, the code is
     *     written to the current stream.
     */
    private void updateColor(ColorType col, boolean fill, StringBuffer pdf) {
        if (col == null) {
            return;
        }
        Color newCol = toColor(col);
        boolean update = false;
        if (fill) {
            update = currentState.setBackColor(newCol);
        } else {
            update = currentState.setColor(newCol);
        }

        if (update) {
            setColor(newCol, fill, pdf);
        }
    }

    private void updateFont(String name, int size, StringBuffer pdf) {
        if ((!name.equals(this.currentFontName))
                || (size != this.currentFontSize)) {
            closeText();

            this.currentFontName = name;
            this.currentFontSize = size;
            pdf = pdf.append("/" + name + " " + ((float) size / 1000f)
                              + " Tf\n");
        }
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderImage(Image, Rectangle2D)
     */
    public void renderImage(Image image, Rectangle2D pos) {
        endTextObject();
        String url = image.getURL();
        putImage(url, pos);
    }

    /**
     * Adds a PDF XObject (a bitmap) to the PDF that will later be referenced.
     * @param url URL of the bitmap
     * @param pos Position of the bitmap
     */
    protected void putImage(String url, Rectangle2D pos) {
        PDFXObject xobject = pdfDoc.getImage(url);
        if (xobject != null) {
            float w = (float) pos.getWidth() / 1000f;
            float h = (float) pos.getHeight() / 1000f;
            placeImage((float)pos.getX() / 1000f,
                       (float)pos.getY() / 1000f, w, h, xobject.getXNumber());
            return;
        }

        url = ImageFactory.getURL(url);
        ImageFactory fact = ImageFactory.getInstance();
        FopImage fopimage = fact.getImage(url, userAgent);
        if (fopimage == null) {
            return;
        }
        if (!fopimage.load(FopImage.DIMENSIONS)) {
            return;
        }
        String mime = fopimage.getMimeType();
        if ("text/xml".equals(mime)) {
            if (!fopimage.load(FopImage.ORIGINAL_DATA)) {
                return;
            }
            Document doc = ((XMLImage) fopimage).getDocument();
            String ns = ((XMLImage) fopimage).getNameSpace();

            renderDocument(doc, ns, pos);
        } else if ("image/svg+xml".equals(mime)) {
            if (!fopimage.load(FopImage.ORIGINAL_DATA)) {
                return;
            }
            Document doc = ((XMLImage) fopimage).getDocument();
            String ns = ((XMLImage) fopimage).getNameSpace();

            renderDocument(doc, ns, pos);
        } else if ("image/eps".equals(mime)) {
            if (!fopimage.load(FopImage.ORIGINAL_DATA)) {
                return;
            }
            FopPDFImage pdfimage = new FopPDFImage(fopimage, url);
            int xobj = pdfDoc.addImage(currentContext, pdfimage).getXNumber();
            fact.releaseImage(url, userAgent);
        } else if ("image/jpeg".equals(mime)) {
            if (!fopimage.load(FopImage.ORIGINAL_DATA)) {
                return;
            }
            FopPDFImage pdfimage = new FopPDFImage(fopimage, url);
            int xobj = pdfDoc.addImage(currentContext, pdfimage).getXNumber();
            fact.releaseImage(url, userAgent);

            float w = (float)pos.getWidth() / 1000f;
            float h = (float)pos.getHeight() / 1000f;
            placeImage((float) pos.getX() / 1000,
                       (float) pos.getY() / 1000, w, h, xobj);
        } else {
            if (!fopimage.load(FopImage.BITMAP)) {
                return;
            }
            FopPDFImage pdfimage = new FopPDFImage(fopimage, url);
            int xobj = pdfDoc.addImage(currentContext, pdfimage).getXNumber();
            fact.releaseImage(url, userAgent);

            float w = (float) pos.getWidth() / 1000f;
            float h = (float) pos.getHeight() / 1000f;
            placeImage((float) pos.getX() / 1000f,
                       (float) pos.getY() / 1000f, w, h, xobj);
        }

        // output new data
        try {
            this.pdfDoc.output(ostream);
        } catch (IOException ioe) {
            // ioexception will be caught later
        }
    }

    /**
     * Places a previously registered image at a certain place on the page.
     * @param x X coordinate
     * @param y Y coordinate
     * @param w width for image
     * @param h height for image
     * @param xobj object number of the referenced image
     */
    protected void placeImage(float x, float y, float w, float h, int xobj) {
        saveGraphicsState();
        currentStream.add(w + " 0 0 "
                          + -h + " "
                          + (currentIPPosition / 1000f + x) + " "
                          + (currentBPPosition / 1000f + h + y) 
                          + " cm\n" + "/Im" + xobj + " Do\n");
        restoreGraphicsState();
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderForeignObject(ForeignObject, Rectangle2D)
     */
    public void renderForeignObject(ForeignObject fo, Rectangle2D pos) {
        endTextObject();
        Document doc = fo.getDocument();
        String ns = fo.getNameSpace();
        renderDocument(doc, ns, pos);
    }

    /**
     * Renders an XML document (SVG for example).
     * @param doc DOM document representing the XML document
     * @param ns Namespace for the document
     * @param pos Position on the page
     */
    public void renderDocument(Document doc, String ns, Rectangle2D pos) {
        RendererContext context;
        context = new RendererContext(this, MIME_TYPE);
        context.setUserAgent(userAgent);

        context.setProperty(PDFXMLHandler.PDF_DOCUMENT, pdfDoc);
        context.setProperty(PDFXMLHandler.OUTPUT_STREAM, ostream);
        context.setProperty(PDFXMLHandler.PDF_STATE, currentState);
        context.setProperty(PDFXMLHandler.PDF_PAGE, currentPage);
        context.setProperty(PDFXMLHandler.PDF_CONTEXT,
                    currentContext == null ? currentPage : currentContext);
        context.setProperty(PDFXMLHandler.PDF_CONTEXT, currentContext);
        context.setProperty(PDFXMLHandler.PDF_STREAM, currentStream);
        context.setProperty(PDFXMLHandler.PDF_XPOS,
                            new Integer(currentIPPosition + (int) pos.getX()));
        context.setProperty(PDFXMLHandler.PDF_YPOS,
                            new Integer(currentBPPosition + (int) pos.getY()));
        context.setProperty(PDFXMLHandler.PDF_FONT_INFO, fontInfo);
        context.setProperty(PDFXMLHandler.PDF_FONT_NAME, currentFontName);
        context.setProperty(PDFXMLHandler.PDF_FONT_SIZE,
                            new Integer(currentFontSize));
        context.setProperty(PDFXMLHandler.PDF_WIDTH,
                            new Integer((int) pos.getWidth()));
        context.setProperty(PDFXMLHandler.PDF_HEIGHT,
                            new Integer((int) pos.getHeight()));
        renderXML(context, doc, ns);

    }

    /**
     * Render an inline viewport.
     * This renders an inline viewport by clipping if necessary.
     * @param viewport the viewport to handle
     */
    public void renderViewport(Viewport viewport) {

        float x = currentIPPosition / 1000f;
        float y = (currentBPPosition + viewport.getOffset()) / 1000f;
        float width = viewport.getIPD() / 1000f;
        float height = viewport.getBPD() / 1000f;
        // TODO: Calculate the border rect correctly. 
        drawBackAndBorders(viewport, x, y, width, height);

        if (viewport.getClip()) {
            saveGraphicsState();

            clip(x, y, width, height);
        }
        super.renderViewport(viewport);

        if (viewport.getClip()) {
            restoreGraphicsState();
        }
    }

    /**
     * Render leader area.
     * This renders a leader area which is an area with a rule.
     * @param area the leader area to render
     */
    public void renderLeader(Leader area) {
        saveGraphicsState();
        int style = area.getRuleStyle();
        boolean alt = false;
        switch(style) {
            case EN_SOLID:
                currentStream.add("[] 0 d\n");
            break;
            case EN_DOTTED:
                currentStream.add("[2] 0 d\n");
            break;
            case EN_DASHED:
                currentStream.add("[6 4] 0 d\n");
            break;
            case EN_DOUBLE:
            case EN_GROOVE:
            case EN_RIDGE:
                alt = true;
            break;
        }
        float startx = ((float) currentIPPosition) / 1000f;
        float starty = ((currentBPPosition + area.getOffset()) / 1000f);
        float endx = (currentIPPosition + area.getIPD()) / 1000f;
        if (!alt) {
            updateLineWidth(area.getRuleThickness() / 1000f);
            drawLine(startx, starty, endx, starty);
        } else {
            if (style == EN_DOUBLE) {
                float third = area.getRuleThickness() / 3000f;
                updateLineWidth(third);
                drawLine(startx, starty, endx, starty);

                drawLine(startx, (starty + 2 * third), endx, (starty + 2 * third));
            } else {
                float half = area.getRuleThickness() / 2000f;

                currentStream.add("1 g\n");
                currentStream.add(startx + " " + starty + " m\n");
                currentStream.add(endx + " " + starty + " l\n");
                currentStream.add(endx + " " + (starty + 2 * half) + " l\n");
                currentStream.add(startx + " " + (starty + 2 * half) + " l\n");
                currentStream.add("h\n");
                currentStream.add("f\n");
                if (style == EN_GROOVE) {
                    currentStream.add("0 g\n");
                    currentStream.add(startx + " " + starty + " m\n");
                    currentStream.add(endx + " " + starty + " l\n");
                    currentStream.add(endx + " " + (starty + half) + " l\n");
                    currentStream.add((startx + half) + " " + (starty + half) + " l\n");
                    currentStream.add(startx + " " + (starty + 2 * half) + " l\n");
                } else {
                    currentStream.add("0 g\n");
                    currentStream.add(endx + " " + starty + " m\n");
                    currentStream.add(endx + " " + (starty + 2 * half) + " l\n");
                    currentStream.add(startx + " " + (starty + 2 * half) + " l\n");
                    currentStream.add(startx + " " + (starty + half) + " l\n");
                    currentStream.add((endx - half) + " " + (starty + half) + " l\n");
                }
                currentStream.add("h\n");
                currentStream.add("f\n");
            }

        }

        restoreGraphicsState();
        beginTextObject();
        super.renderLeader(area);
    }

    /** @see org.apache.fop.render.AbstractRenderer */
    public String getMimeType() {
        return MIME_TYPE;
    }
}

