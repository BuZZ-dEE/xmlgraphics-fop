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

/* $Id$ */

package org.apache.fop.layoutmgr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.area.Area;
import org.apache.fop.area.Trait;
import org.apache.fop.datatypes.LengthBase;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.datatypes.SimplePercentBaseContext;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.properties.CommonMarginBlock;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.CommonTextDecoration;

/**
 * This is a helper class used for setting common traits on areas.
 */
public class TraitSetter {

    /** logger */
    protected static Log log = LogFactory.getLog(TraitSetter.class);
    
    /**
     * Sets border and padding traits on areas.
     * @param area area to set the traits on
     * @param bpProps border and padding properties
     * @param bNotFirst True if the area is not the first area
     * @param bNotLast True if the area is not the last area
     * @param context Property evaluation context
     */
    public static void setBorderPaddingTraits(Area area,
            CommonBorderPaddingBackground bpProps, boolean bNotFirst, boolean bNotLast,
            PercentBaseContext context) {
        int iBP;
        iBP = bpProps.getPadding(CommonBorderPaddingBackground.START, bNotFirst, context);
        if (iBP > 0) {
            area.addTrait(Trait.PADDING_START, new Integer(iBP));
        }
        iBP = bpProps.getPadding(CommonBorderPaddingBackground.END, bNotLast, context);
        if (iBP > 0) {
            area.addTrait(Trait.PADDING_END, new Integer(iBP));
        }
        iBP = bpProps.getPadding(CommonBorderPaddingBackground.BEFORE, false, context);
        if (iBP > 0) {
            area.addTrait(Trait.PADDING_BEFORE, new Integer(iBP));
        }
        iBP = bpProps.getPadding(CommonBorderPaddingBackground.AFTER, false, context);
        if (iBP > 0) {
            area.addTrait(Trait.PADDING_AFTER, new Integer(iBP));
        }

        addBorderTrait(area, bpProps, bNotFirst,
                       CommonBorderPaddingBackground.START, 
                       BorderProps.SEPARATE, Trait.BORDER_START);

        addBorderTrait(area, bpProps, bNotLast, 
                       CommonBorderPaddingBackground.END,
                       BorderProps.SEPARATE, Trait.BORDER_END);

        addBorderTrait(area, bpProps, false, 
                       CommonBorderPaddingBackground.BEFORE,
                       BorderProps.SEPARATE, Trait.BORDER_BEFORE);

        addBorderTrait(area, bpProps, false, 
                       CommonBorderPaddingBackground.AFTER,
                       BorderProps.SEPARATE, Trait.BORDER_AFTER);
    }

    /**
     * Sets border traits on an area.
     * @param area area to set the traits on
     * @param bpProps border and padding properties
     * @param mode the border paint mode (see BorderProps)
     */
    private static void addBorderTrait(Area area,
                                       CommonBorderPaddingBackground bpProps,
                                       boolean bDiscard, int iSide, int mode,
                                       Object oTrait) {
        int iBP = bpProps.getBorderWidth(iSide, bDiscard);
        if (iBP > 0) {
            area.addTrait(oTrait,
                          new BorderProps(bpProps.getBorderStyle(iSide),
                                          iBP, bpProps.getBorderColor(iSide),
                                          mode));
        }
    }

    /**
     * Add borders to an area.
     * Layout managers that create areas with borders can use this to
     * add the borders to the area.
     * @param area the area to set the traits on.
     * @param bordProps border properties
     * @param context Property evaluation context
     */
    public static void addBorders(Area area, CommonBorderPaddingBackground bordProps,
                                PercentBaseContext context) {
        BorderProps bps = getBorderProps(bordProps, CommonBorderPaddingBackground.BEFORE);
        if (bps != null) {
            area.addTrait(Trait.BORDER_BEFORE, bps);
        }
        bps = getBorderProps(bordProps, CommonBorderPaddingBackground.AFTER);
        if (bps != null) {
            area.addTrait(Trait.BORDER_AFTER, bps);
        }
        bps = getBorderProps(bordProps, CommonBorderPaddingBackground.START);
        if (bps != null) {
            area.addTrait(Trait.BORDER_START, bps);
        }
        bps = getBorderProps(bordProps, CommonBorderPaddingBackground.END);
        if (bps != null) {
            area.addTrait(Trait.BORDER_END, bps);
        }

        addPadding(area, bordProps, context);
    }

    /**
     * Add borders to an area for the collapsing border model in tables.
     * Layout managers that create areas with borders can use this to
     * add the borders to the area.
     * @param area the area to set the traits on.
     * @param bordProps border properties
     * @param outer 4 boolean values indicating if the side represents the 
     *     table's outer border. Order: before, after, start, end
     * @param context Property evaluation context
     */
    public static void addCollapsingBorders(Area area, 
            CommonBorderPaddingBackground bordProps,
            boolean[] outer,
            PercentBaseContext context) {
        BorderProps bps = getCollapsingBorderProps(bordProps, 
                CommonBorderPaddingBackground.BEFORE, outer[0]);
        if (bps != null) {
            area.addTrait(Trait.BORDER_BEFORE, bps);
        }
        bps = getCollapsingBorderProps(bordProps, 
                CommonBorderPaddingBackground.AFTER, outer[1]);
        if (bps != null) {
            area.addTrait(Trait.BORDER_AFTER, bps);
        }
        bps = getCollapsingBorderProps(bordProps, 
                CommonBorderPaddingBackground.START, outer[2]);
        if (bps != null) {
            area.addTrait(Trait.BORDER_START, bps);
        }
        bps = getCollapsingBorderProps(bordProps, 
                CommonBorderPaddingBackground.END, outer[3]);
        if (bps != null) {
            area.addTrait(Trait.BORDER_END, bps);
        }

        addPadding(area, bordProps, context);
    }

    private static void addPadding(Area area, CommonBorderPaddingBackground bordProps, 
                                PercentBaseContext context) {
        int padding = bordProps.getPadding(CommonBorderPaddingBackground.START, false, context);
        if (padding != 0) {
            area.addTrait(Trait.PADDING_START, new java.lang.Integer(padding));
        }

        padding = bordProps.getPadding(CommonBorderPaddingBackground.END, false, context);
        if (padding != 0) {
            area.addTrait(Trait.PADDING_END, new java.lang.Integer(padding));
        }

        padding = bordProps.getPadding(CommonBorderPaddingBackground.BEFORE, false, context);
        if (padding != 0) {
            area.addTrait(Trait.PADDING_BEFORE, new java.lang.Integer(padding));
        }

        padding = bordProps.getPadding(CommonBorderPaddingBackground.AFTER, false, context);
        if (padding != 0) {
            area.addTrait(Trait.PADDING_AFTER, new java.lang.Integer(padding));
        }
    }
    
    private static BorderProps getBorderProps(CommonBorderPaddingBackground bordProps, int side) {
        int width = bordProps.getBorderWidth(side, false);
        if (width != 0) {
            BorderProps bps;
            bps = new BorderProps(bordProps.getBorderStyle(side),
                                  width,
                                  bordProps.getBorderColor(side),
                                  BorderProps.SEPARATE);
            return bps;
        } else {
            return null;
        }
    }

    private static BorderProps getCollapsingBorderProps(
            CommonBorderPaddingBackground bordProps, int side, boolean outer) {
        int width = bordProps.getBorderWidth(side, false);
        if (width != 0) {
            BorderProps bps;
            bps = new BorderProps(bordProps.getBorderStyle(side),
                    width, bordProps.getBorderColor(side),
                    (outer ? BorderProps.COLLAPSE_OUTER : BorderProps.COLLAPSE_INNER));
            return bps;
        } else {
            return null;
        }
    }

    /**
     * Add background to an area.
     * Layout managers that create areas with a background can use this to
     * add the background to the area.
     * Note: The area's IPD and BPD must be set before calling this method.
     * @param area the area to set the traits on
     * @param backProps the background properties
     * @param context Property evaluation context
     */
    public static void addBackground(Area area, 
                                     CommonBorderPaddingBackground backProps,
                                     PercentBaseContext context) {
        if (!backProps.hasBackground()) {
            return;
        }
        Trait.Background back = new Trait.Background();
        back.setColor(backProps.backgroundColor);

        if (backProps.getFopImage() != null) {
            back.setURL(backProps.backgroundImage);
            back.setFopImage(backProps.getFopImage());
            back.setRepeat(backProps.backgroundRepeat);
            if (backProps.backgroundPositionHorizontal != null) {
                if (back.getRepeat() == Constants.EN_NOREPEAT 
                        || back.getRepeat() == Constants.EN_REPEATY) {
                    if (area.getIPD() > 0) {
                        int width = area.getIPD();
                        width += backProps.getPaddingStart(false, context);
                        width += backProps.getPaddingEnd(false, context);
                        back.setHoriz(backProps.backgroundPositionHorizontal.getValue(
                                new SimplePercentBaseContext(context, 
                                                             LengthBase.IMAGE_BACKGROUND_POSITION_HORIZONTAL,
                                                             (width - back.getFopImage().getIntrinsicWidth())
                                                            )
                            ));
                    } else {
                        //TODO Area IPD has to be set for this to work
                        log.warn("Horizontal background image positioning ignored"
                                + " because the IPD was not set on the area."
                                + " (Yes, it's a bug in FOP)");
                    }
                }
            }
            if (backProps.backgroundPositionVertical != null) {
                if (back.getRepeat() == Constants.EN_NOREPEAT 
                        || back.getRepeat() == Constants.EN_REPEATX) {
                    if (area.getBPD() > 0) {
                        int height = area.getBPD();
                        height += backProps.getPaddingBefore(false, context);
                        height += backProps.getPaddingAfter(false, context);
                        back.setVertical(backProps.backgroundPositionVertical.getValue(
                                new SimplePercentBaseContext(context, 
                                                             LengthBase.IMAGE_BACKGROUND_POSITION_VERTICAL,
                                                             (height - back.getFopImage().getIntrinsicHeight())
                                                            )
                            ));
                    } else {
                        //TODO Area BPD has to be set for this to work
                        log.warn("Vertical background image positioning ignored"
                                + " because the BPD was not set on the area."
                                + " (Yes, it's a bug in FOP)");
                    }
                }
            }
        }

        area.addTrait(Trait.BACKGROUND, back);
    }

    /**
     * Add space to a block area.
     * Layout managers that create block areas can use this to add space
     * outside of the border rectangle to the area.
     * @param area the area to set the traits on.
     * @param bpProps the border, padding and background properties
     * @param marginProps the margin properties.
     * @param context the context for evaluation of percentages
     */
    public static void addMargins(Area area,
                                  CommonBorderPaddingBackground bpProps,
                                  CommonMarginBlock marginProps,
                                  PercentBaseContext context) {
        int startIndent = marginProps.startIndent.getValue(context);
        if (startIndent != 0) {
            area.addTrait(Trait.START_INDENT, new Integer(startIndent));
        }
        
        int spaceStart = marginProps.startIndent.getValue(context)
                            - bpProps.getBorderStartWidth(false)
                            - bpProps.getPaddingStart(false, context);
        if (spaceStart != 0) {
            area.addTrait(Trait.SPACE_START, new Integer(spaceStart));
        }

        int endIndent = marginProps.endIndent.getValue(context);
        if (endIndent != 0) {
            area.addTrait(Trait.END_INDENT, new Integer(endIndent));
        }
        int spaceEnd = marginProps.endIndent.getValue(context)
                            - bpProps.getBorderEndWidth(false)
                            - bpProps.getPaddingEnd(false, context);
        if (spaceEnd != 0) {
            area.addTrait(Trait.SPACE_END, new Integer(spaceEnd));
        }
    }

    /**
     * Sets the traits for breaks on an area.
     * @param area the area to set the traits on.
     * @param breakBefore the value for break-before
     * @param breakAfter the value for break-after
     */
    public static void addBreaks(Area area,  int breakBefore, int breakAfter) {
        area.addTrait(Trait.BREAK_AFTER, new Integer(breakAfter));
        area.addTrait(Trait.BREAK_BEFORE, new Integer(breakBefore));
    }
    
    /**
     * Adds the text-decoration traits to the area.
     * @param area the area to set the traits on
     * @param deco the text decorations
     */
    public static void addTextDecoration(Area area, CommonTextDecoration deco) {
        //TODO Finish text-decoration
        if (deco != null) {
            if (deco.hasUnderline()) {
                area.addTrait(Trait.UNDERLINE, Boolean.TRUE);
                area.addTrait(Trait.UNDERLINE_COLOR, deco.getUnderlineColor());
            }
            if (deco.hasOverline()) {
                area.addTrait(Trait.OVERLINE, Boolean.TRUE);
                area.addTrait(Trait.OVERLINE_COLOR, deco.getOverlineColor());
            }
            if (deco.hasLineThrough()) {
                area.addTrait(Trait.LINETHROUGH, Boolean.TRUE);
                area.addTrait(Trait.LINETHROUGH_COLOR, deco.getLineThroughColor());
            }
            if (deco.isBlinking()) {
                area.addTrait(Trait.BLINK, Boolean.TRUE);
            }
        }
    }
    
    /**
     * Sets the producer's ID as a trait on the area. This can be used to track back the 
     * generating FO node.
     * @param area the area to set the traits on
     * @param id the ID to set
     */
    public static void setProducerID(Area area, String id) {
        if (id != null && id.length() > 0) {
            area.addTrait(Trait.PROD_ID, id);
        }
    }
}
