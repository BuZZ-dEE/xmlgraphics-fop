/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo.flow;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.fo.properties.*;
import org.apache.fop.layout.*;
import org.apache.fop.datatypes.*;
import org.apache.fop.apps.FOPException;

/*
  Modified by Mark Lillywhite mark-fop@inomial.com. The changes
  here are based on memory profiling and do not change functionality.
  Essentially, the Block object had a pointer to a BlockArea object
  that it created. The BlockArea was not referenced after the Block
  was finished except to determine the size of the BlockArea, however
  a reference to the BlockArea was maintained and this caused a lot of
  GC problems, and was a major reason for FOP memory leaks. So,
  the reference to BlockArea was made local, the required information
  is now stored (instead of a reference to the complex BlockArea object)
  and it appears that there are a lot of changes in this file, in fact
  there are only a few sematic changes; mostly I just got rid of 
  "this." from blockArea since BlockArea is now local.
  */

public class Block extends FObjMixed {

    public static class Maker extends FObj.Maker {
        public FObj make(FObj parent,
                         PropertyList propertyList) throws FOPException {
            return new Block(parent, propertyList);
        }

    }

    public static FObj.Maker maker() {
        return new Block.Maker();
    }

    int align;
    int alignLast;
    int breakAfter;
    int lineHeight;
    int startIndent;
    int endIndent;
    int spaceBefore;
    int spaceAfter;
    int textIndent;
    int keepWithNext;
    ColorType backgroundColor;
    int blockWidows;
    int blockOrphans;

    int areaHeight = 0;
    int contentWidth = 0;

    String id;
    int span;

    // this may be helpful on other FOs too
    boolean anythingLaidOut = false;

    public Block(FObj parent, PropertyList propertyList)
        throws FOPException {

        super(parent, propertyList);
        this.name = "fo:block";
        this.span = this.properties.get("span").getEnum();
        ts = propMgr.getTextDecoration(parent);
    }

    public Status layout(Area area) throws FOPException {
        BlockArea blockArea;

        // log.error(" b:LAY[" + marker + "] ");


        if (this.marker == BREAK_AFTER) {
            return new Status(Status.OK);
        }

        if (this.marker == START) {

            // Common Accessibility Properties
            AccessibilityProps mAccProps = propMgr.getAccessibilityProps();

            // Common Aural Properties
            AuralProps mAurProps = propMgr.getAuralProps();

            // Common Border, Padding, and Background Properties
            BorderAndPadding bap = propMgr.getBorderAndPadding();
            BackgroundProps bProps = propMgr.getBackgroundProps();

            // Common Font Properties
            //this.fontState = propMgr.getFontState(area.getFontInfo());

            // Common Hyphenation Properties
            HyphenationProps mHyphProps = propMgr.getHyphenationProps();

            // Common Margin Properties-Block
            MarginProps mProps = propMgr.getMarginProps();

            // Common Relative Position Properties
            RelativePositionProps mRelProps = propMgr.getRelativePositionProps();

            // this.properties.get("break-after");
            // this.properties.get("break-before");
            // this.properties.get("color");
            // this.properties.get("text-depth");
            // this.properties.get("text-altitude");
            // this.properties.get("hyphenation-keep");
            // this.properties.get("hyphenation-ladder-count");
            // this.properties.get("id");
            // this.properties.get("keep-together");
            // this.properties.get("keep-with-next");
            // this.properties.get("keep-with-previous");
            // this.properties.get("last-line-end-indent");
            // this.properties.get("linefeed-treatment");
            // this.properties.get("line-height");
            // this.properties.get("line-height-shift-adjustment");
            // this.properties.get("line-stacking-strategy");
            // this.properties.get("orphans");
            // this.properties.get("white-space-treatment");
            // this.properties.get("span");
            // this.properties.get("text-align");
            // this.properties.get("text-align-last");
            // this.properties.get("text-indent");
            // this.properties.get("visibility");
            // this.properties.get("white-space-collapse");
            // this.properties.get("widows");
            // this.properties.get("wrap-option");
            // this.properties.get("z-index");

            this.align = this.properties.get("text-align").getEnum();
            this.alignLast = this.properties.get("text-align-last").getEnum();
            this.breakAfter = this.properties.get("break-after").getEnum();
            this.lineHeight =
                this.properties.get("line-height").getLength().mvalue();
            this.startIndent =
                this.properties.get("start-indent").getLength().mvalue();
            this.endIndent =
                this.properties.get("end-indent").getLength().mvalue();
            this.spaceBefore =
                this.properties.get("space-before.optimum").getLength().mvalue();
            this.spaceAfter =
                this.properties.get("space-after.optimum").getLength().mvalue();
            this.textIndent =
                this.properties.get("text-indent").getLength().mvalue();
            this.keepWithNext =
                this.properties.get("keep-with-next").getEnum();
            this.backgroundColor =
                this.properties.get("background-color").getColorType();

            this.blockWidows =
                this.properties.get("widows").getNumber().intValue();
            this.blockOrphans =
                this.properties.get("orphans").getNumber().intValue();



            this.id = this.properties.get("id").getString();

            if (area instanceof BlockArea) {
                area.end();
            }

            if (area.getIDReferences() != null)
                area.getIDReferences().createID(id);

            this.marker = 0;

            // no break if first in area tree, or leading in context
            // area
            int breakBeforeStatus = propMgr.checkBreakBefore(area);
            if (breakBeforeStatus != Status.OK) {
                return new Status(breakBeforeStatus);
            }

            int numChildren = this.children.size();
            for (int i = 0; i < numChildren; i++) {
                FONode fo = (FONode)children.elementAt(i);
                if (fo instanceof FOText) {
                    if (((FOText)fo).willCreateArea()) {
                        fo.setWidows(blockWidows);
                        break;
                    } else {
                        children.removeElementAt(i);
                        numChildren = this.children.size();
                        i--;
                    }
                } else {
                    fo.setWidows(blockWidows);
                    break;
                }
            }

            for (int i = numChildren - 1; i >= 0; i--) {
                FONode fo = (FONode)children.elementAt(i);
                if (fo instanceof FOText) {
                    if (((FOText)fo).willCreateArea()) {
                        fo.setOrphans(blockOrphans);
                        break;
                    }
                } else {
                    fo.setOrphans(blockOrphans);
                    break;
                }
            }
        }

        if ((spaceBefore != 0) && (this.marker == 0)) {
            area.addDisplaySpace(spaceBefore);
        }

        if (anythingLaidOut) {
            this.textIndent = 0;
        }

        if (marker == 0 && area.getIDReferences() != null) {
            area.getIDReferences().configureID(id, area);
        }

        int spaceLeft = area.spaceLeft();
        blockArea =
            new BlockArea(propMgr.getFontState(area.getFontInfo()),
                          area.getAllocationWidth(), area.spaceLeft(),
                          startIndent, endIndent, textIndent, align,
                          alignLast, lineHeight);
        blockArea.setGeneratedBy(this);
        this.areasGenerated++;
        if (this.areasGenerated == 1)
            blockArea.isFirst(true);
        // for normal areas this should be the only pair
        blockArea.addLineagePair(this, this.areasGenerated);

        // markers
        if (this.hasMarkers())
            blockArea.addMarkers(this.getMarkers());

        blockArea.setParent(area);    // BasicLink needs it
        blockArea.setPage(area.getPage());
        blockArea.setBackgroundColor(backgroundColor);
        blockArea.setBorderAndPadding(propMgr.getBorderAndPadding());
        blockArea.setHyphenation(propMgr.getHyphenationProps());
        blockArea.start();

        blockArea.setAbsoluteHeight(area.getAbsoluteHeight());
        blockArea.setIDReferences(area.getIDReferences());

        blockArea.setTableCellXOffset(area.getTableCellXOffset());

        int numChildren = this.children.size();
        for (int i = this.marker; i < numChildren; i++) {
            FONode fo = (FONode)children.elementAt(i);
            Status status;
            if ((status = fo.layout(blockArea)).isIncomplete()) {
                this.marker = i;
                // this block was modified by
                // Hani Elabed 11/27/2000
                // if ((i != 0) && (status.getCode() == Status.AREA_FULL_NONE))
                // {
                // status = new Status(Status.AREA_FULL_SOME);
                // }

                // new block to replace the one above
                // Hani Elabed 11/27/2000
                if (status.getCode() == Status.AREA_FULL_NONE) {
                    // something has already been laid out
                    if ((i != 0)) {
                        status = new Status(Status.AREA_FULL_SOME);
                        area.addChild(blockArea);
                        area.setMaxHeight(area.getMaxHeight() - spaceLeft
                                          + blockArea.getMaxHeight());
                        area.increaseHeight(blockArea.getHeight());
                        area.setAbsoluteHeight(blockArea.getAbsoluteHeight());
                        anythingLaidOut = true;

                        return status;
                    } else    // i == 0 nothing was laid out..
                    {
                        anythingLaidOut = false;
                        return status;
                    }
                }

                // blockArea.end();
                area.addChild(blockArea);
                area.setMaxHeight(area.getMaxHeight() - spaceLeft
                                  + blockArea.getMaxHeight());
                area.increaseHeight(blockArea.getHeight());
                area.setAbsoluteHeight(blockArea.getAbsoluteHeight());
                anythingLaidOut = true;
                return status;
            }
            anythingLaidOut = true;
        }

        blockArea.end();

        area.setMaxHeight(area.getMaxHeight() - spaceLeft
                          + blockArea.getMaxHeight());

        area.addChild(blockArea);

        /* should this be combined into above? */
        area.increaseHeight(blockArea.getHeight());

        area.setAbsoluteHeight(blockArea.getAbsoluteHeight());

        if (spaceAfter != 0) {
            area.addDisplaySpace(spaceAfter);
        }

        if (area instanceof BlockArea) {
            area.start();
        }
        // This is not needed any more and it consumes a LOT
        // of memory. So we release it for the GC.
        areaHeight= blockArea.getHeight();
        contentWidth= blockArea.getContentWidth();

        // no break if last in area tree, or trailing in context
        // area
        int breakAfterStatus = propMgr.checkBreakAfter(area);
        if (breakAfterStatus != Status.OK) {
            this.marker = BREAK_AFTER;
            blockArea = null; //Faster GC - BlockArea is big
            return new Status(breakAfterStatus);
        }

        if (keepWithNext != 0) {
            blockArea = null; // Faster GC - BlockArea is big
            return new Status(Status.KEEP_WITH_NEXT);
        }

        // log.error(" b:OK" + marker + " ");
        blockArea.isLast(true);
        blockArea = null; // Faster GC - BlockArea is big
        return new Status(Status.OK);
    }

    public int getAreaHeight() {
        return areaHeight;
    }


    /**
     * Return the content width of the boxes generated by this FO.
     */
    public int getContentWidth() {
        return contentWidth;    // getAllocationWidth()??
    }


    public int getSpan() {
        return this.span;
    }

}
