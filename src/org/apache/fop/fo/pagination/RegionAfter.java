/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo.pagination;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.fo.properties.*;
import org.apache.fop.apps.FOPException;
import org.apache.fop.layout.RegionArea;
import org.apache.fop.layout.BorderAndPadding;
import org.apache.fop.layout.BackgroundProps;

public class RegionAfter extends Region {

    public static class Maker extends FObj.Maker {
        public FObj make(FObj parent,
                         PropertyList propertyList) throws FOPException {
            return new RegionAfter(parent, propertyList);
        }

    }

    public static FObj.Maker maker() {
        return new RegionAfter.Maker();
    }

    public static final String REGION_CLASS = "after";

    private int precedence;

    protected RegionAfter(FObj parent,
                          PropertyList propertyList) throws FOPException {
        super(parent, propertyList);
        precedence = this.properties.get("precedence").getEnum();
    }

    public String getName() {
        return "fo:region-after";
    }

    RegionArea makeRegionArea(int allocationRectangleXPosition,
                              int allocationRectangleYPosition,
                              int allocationRectangleWidth,
                              int allocationRectangleHeight) {

        // Common Border, Padding, and Background Properties
        BorderAndPadding bap = propMgr.getBorderAndPadding();
        BackgroundProps bProps = propMgr.getBackgroundProps();

        // this.properties.get("clip");
        // this.properties.get("display-align");
        int extent = this.properties.get("extent").getLength().mvalue();
        // this.properties.get("overflow");
        // this.properties.get("precedence");
        // this.properties.get("region-name");
        // this.properties.get("reference-orientation");
        // this.properties.get("writing-mode");

        RegionArea area = new RegionArea(allocationRectangleXPosition,
					 allocationRectangleYPosition
					 - allocationRectangleHeight + extent,
					 allocationRectangleWidth, extent);
	area.setBackground(bProps);
	return area;
    }

    RegionArea makeRegionArea(int allocationRectangleXPosition,
                              int allocationRectangleYPosition,
                              int allocationRectangleWidth,
                              int allocationRectangleHeight,
                              int startExtent, int endExtent) {
        if (getPrecedence() == false) {
            allocationRectangleXPosition += startExtent;
            allocationRectangleWidth -= startExtent + endExtent;
        }
        return makeRegionArea(allocationRectangleXPosition,
                              allocationRectangleYPosition,
                              allocationRectangleWidth,
                              allocationRectangleHeight);
    }

    protected String getDefaultRegionName() {
        return "xsl-region-after";
    }

    public String getRegionClass() {
        return REGION_CLASS;
    }

    public boolean getPrecedence() {
        return (precedence == Precedence.TRUE ? true : false);
    }

    public int getExtent() {
        return properties.get("extent").getLength().mvalue();
    }
}
