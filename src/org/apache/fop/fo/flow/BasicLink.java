/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo.flow;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.properties.*;
import org.apache.fop.layout.*;
import org.apache.fop.datatypes.ColorType;

// Java
import java.awt.Rectangle;

public class BasicLink extends Inline {

    public static class Maker extends FObj.Maker {
        public FObj make(FObj parent,
                         PropertyList propertyList) throws FOPException {
            return new BasicLink(parent, propertyList);
        }
    }

    public static FObj.Maker maker() {
        return new BasicLink.Maker();
    }

    public BasicLink(FObj parent,
                     PropertyList propertyList) throws FOPException {
        super(parent, propertyList);
    }

    public String getName() {
        return "fo:basic-link";
    }

    public int layout(Area area) throws FOPException {
        String destination;
        int linkType;

        // Common Accessibility Properties
        AccessibilityProps mAccProps = propMgr.getAccessibilityProps();

        // Common Aural Properties
        AuralProps mAurProps = propMgr.getAuralProps();

        // Common Border, Padding, and Background Properties
        BorderAndPadding bap = propMgr.getBorderAndPadding();
        BackgroundProps bProps = propMgr.getBackgroundProps();

        // Common Margin Properties-Inline
        MarginInlineProps mProps = propMgr.getMarginInlineProps();

        // Common Relative Position Properties
        RelativePositionProps mRelProps = propMgr.getRelativePositionProps();

        // this.properties.get("alignment-adjust");
        // this.properties.get("alignment-baseline");
        // this.properties.get("baseline-shift");
        // this.properties.get("destination-place-offset");
        // this.properties.get("dominant-baseline");
        // this.properties.get("external-destination");        
        // this.properties.get("id");
        // this.properties.get("indicate-destination");  
        // this.properties.get("internal-destination");  
        // this.properties.get("keep-together");
        // this.properties.get("keep-with-next");
        // this.properties.get("keep-with-previous");
        // this.properties.get("line-height");
        // this.properties.get("line-height-shift-adjustment");
        // this.properties.get("show-destination");  
        // this.properties.get("target-processing-context");  
        // this.properties.get("target-presentation-context");  
        // this.properties.get("target-stylesheet");  

        if (!(destination =
                this.properties.get("internal-destination").getString()).equals("")) {
            linkType = LinkSet.INTERNAL;
        } else if (!(destination =
        this.properties.get("external-destination").getString()).equals("")) {
            linkType = LinkSet.EXTERNAL;
        } else {
            throw new FOPException("internal-destination or external-destination must be specified in basic-link");
        }

        if (this.marker == START) {
            // initialize id
            String id = this.properties.get("id").getString();
            area.getIDReferences().initializeID(id, area);
            this.marker = 0;
        }

        // new LinkedArea to gather up inlines
        LinkSet ls = new LinkSet(destination, area, linkType);

        Page p = area.getPage();

        //AreaContainer ac = p.getBody().getCurrentColumnArea();
        AreaContainer ac = area.getNearestAncestorAreaContainer();
	while (ac!=null && ac.getPosition()!=Position.ABSOLUTE) {
	    ac = ac.getNearestAncestorAreaContainer();
	}
	if (ac == null) {
	    ac = p.getBody().getCurrentColumnArea();
	    //System.err.println("Using currentColumnArea as AC for link");
	}
        if (ac == null) {
            throw new FOPException("Couldn't get ancestor AreaContainer when processing basic-link");
        }

        int numChildren = this.children.size();
        for (int i = this.marker; i < numChildren; i++) {
            FONode fo = (FONode)children.get(i);
            fo.setLinkSet(ls);

            int status;
            if (Status.isIncomplete((status = fo.layout(area)))) {
                this.marker = i;
                return status;
            }
        }

        ls.applyAreaContainerOffsets(ac, area);

        // pass on command line
        String mergeLinks = System.getProperty("links.merge");
        if ((null == mergeLinks) || mergeLinks.equalsIgnoreCase("yes")) {
            ls.mergeLinks();
        }

        p.addLinkSet(ls);

        return Status.OK;
    }

}
