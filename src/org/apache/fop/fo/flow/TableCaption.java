/*
 * -- $Id$ --
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo.flow;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.fo.flow.*;
import org.apache.fop.fo.properties.*;
import org.apache.fop.layout.*;
import org.apache.fop.apps.FOPException;

/**
 */
public class TableCaption extends ToBeImplementedElement {

    public static class Maker extends FObj.Maker {
        public FObj make(FObj parent,
                         PropertyList propertyList) throws FOPException {
            return new TableCaption(parent, propertyList);
        }

    }

    public static FObj.Maker maker() {
        return new TableCaption.Maker();
    }

    protected TableCaption(FObj parent,
                           PropertyList propertyList) throws FOPException {
        super(parent, propertyList);
    }

    public String getName() {
        return "fo:table-caption";
    }

    public Status layout(Area area) throws FOPException {

        // Common Accessibility Properties
        AccessibilityProps mAccProps = propMgr.getAccessibilityProps();

        // Common Aural Properties
        AuralProps mAurProps = propMgr.getAuralProps();

        // Common Border, Padding, and Background Properties
        BorderAndPadding bap = propMgr.getBorderAndPadding();
        BackgroundProps bProps = propMgr.getBackgroundProps();

        // Common Relative Position Properties
        RelativePositionProps mRelProps = propMgr.getRelativePositionProps();

        // this.properties.get("block-progression-dimension");
        // this.properties.get("height");
        // this.properties.get("id");
        // this.properties.get("inline-progression-dimension");
        // this.properties.get("keep-togethe");
        // this.properties.get("width");

        return super.layout(area);
    }
}
