/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 *
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 */

package org.apache.fop.fo.pagination;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;

// FOP
import org.apache.fop.fo.FOAttributes;
import org.apache.fop.xml.FoXMLEvent;
import org.apache.fop.xml.XMLEvent;
import org.apache.fop.xml.XMLNamespaces;
import org.apache.fop.xml.UriLocalName;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.FOPropertySets;
import org.apache.fop.fo.FObjectNames;
import org.apache.fop.fo.FOTree;
import org.apache.fop.fo.FONode;
import org.apache.fop.datastructs.Tree;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.datatypes.NCName;

/**
 * Implements the fo:page-sequence-master flow object.  These Fos are
 * children of fo:layout-master-set FOs.  Their contents are specified by
 * (single-page-master-reference|repeatable-page-master-reference
 *                                |repeatable-page-master-alternatives)+
 */
public class FoPageSequenceMaster extends FONode {

    private static final String tag = "$Name$";
    private static final String revision = "$Revision$";

    /**
     * An array with <tt>int</tt>s identifying
     * <tt>single-page-master-reference</tt>,
     * <tt>repeatable-page-master-reference</tt> and
     * <tt>repeatable-page-master-alternatives</tt> XML events.
     */
    private static final int[] singleOrRepeatableMasterRefs = {
        FObjectNames.SINGLE_PAGE_MASTER_REFERENCE,
        FObjectNames.REPEATABLE_PAGE_MASTER_REFERENCE,
        FObjectNames.REPEATABLE_PAGE_MASTER_ALTERNATIVES
    };

    private String masterName;

    //private ArrayList subSequenceList = new ArrayList(1);

    public FoPageSequenceMaster(FOTree foTree, FONode parent, FoXMLEvent event)
        throws Tree.TreeException, FOPException, PropertyException
    {
        super(foTree, FObjectNames.PAGE_SEQUENCE_MASTER, parent, event,
                                              FOPropertySets.SEQ_MASTER_SET);
        // Process sequence members here
        try {
            do {
                FoXMLEvent ev = xmlevents.expectStartElement
                    (singleOrRepeatableMasterRefs, XMLEvent.DISCARD_W_SPACE);
                if (ev == null) break;  // page-sequence-masters exhausted
                int foType = ev.getFoType();
                if (foType == FObjectNames.SINGLE_PAGE_MASTER_REFERENCE) {
                    //System.out.println("Found single-page-master-reference");
		    //subSequenceList.add(new FoSinglePageMasterReference
							//(foTree, this, ev));
		    new FoSinglePageMasterReference(foTree, this, ev);
                } else if (foType ==
                            FObjectNames.REPEATABLE_PAGE_MASTER_REFERENCE) {
                    //System.out.println
                    //        ("Found repeatable-page-master-reference");
		    //subSequenceList.add(new FoRepeatablePageMasterReference
							//(foTree, this, ev));
		    new FoRepeatablePageMasterReference(foTree, this, ev);
                } else if (foType ==
                        FObjectNames.REPEATABLE_PAGE_MASTER_ALTERNATIVES) {
                    //System.out.println
                    //        ("Found repeatable-page-master-alternatives");
		    //subSequenceList.add(new FoRepeatablePageMasterAlternatives
							//(foTree, this, ev));
		    new FoRepeatablePageMasterAlternatives(foTree, this, ev);
                } else
                    throw new FOPException
                            ("Aargh! expectStartElement(events, list)");
            } while (true);
        } catch (NoSuchElementException e) {
            throw new FOPException("Unexpected EOF in page-sequence-master.");
        }
        if (this.numChildren() == 0)
            throw new FOPException("No children of page-sequence-master.");
        XMLEvent ev = xmlevents.getEndElement(event);
    }

    /**
     * @return a <tt>String</tt> with the "master-name" attribute value.
     */
    public String getMasterName() throws PropertyException {
	if (masterName == null) {
	    PropertyValue name = propertySet[PropNames.MASTER_NAME];
	    if (name == null)
		throw new PropertyException("master-name property not set");
	    if (name.getType() != PropertyValue.NCNAME)
		throw new PropertyException
				    ("master-name property not an NCName.");
	    masterName = ((NCName)name).getNCName();
	}
	return masterName;
    }

    /**
     * Implements the fo:single-page-master-reference flow object.  It is
     * always a child of an fo:page-sequence-master.
     */
    public class FoSinglePageMasterReference extends FONode {

	public FoSinglePageMasterReference
			    (FOTree foTree, FONode parent, FoXMLEvent event)
	    throws Tree.TreeException, FOPException, PropertyException
	{
	    super(foTree, FObjectNames.SINGLE_PAGE_MASTER_REFERENCE, parent,
					event, FOPropertySets.SEQ_MASTER_SET);
	    this.xmlevents.getEndElement(event);
	}

	public PropertyValue getMasterReference() throws PropertyException {
	    return this.getPropertyValue(PropNames.MASTER_REFERENCE);
	}

    }// FoSinglePageMasterReference

    /**
     * Implements the fo:repeatable-page-master-reference flow object.  It is
     * always a child of an fo:page-sequence-master.
     */
    public class FoRepeatablePageMasterReference extends FONode {

	public FoRepeatablePageMasterReference
			    (FOTree foTree, FONode parent, FoXMLEvent event)
	    throws Tree.TreeException, FOPException, PropertyException
	{
	    super(foTree, FObjectNames.REPEATABLE_PAGE_MASTER_REFERENCE,
				parent, event, FOPropertySets.SEQ_MASTER_SET);
	    this.xmlevents.getEndElement(event);
	}

	public PropertyValue getMasterReference() throws PropertyException {
	    return this.getPropertyValue(PropNames.MASTER_REFERENCE);
	}

	public PropertyValue getMaximumRepeats() throws PropertyException {
	    return this.getPropertyValue(PropNames.MAXIMUM_REPEATS);
	}

    }// FoRepeatablePageMasterReference

    /**
     * Implements the fo:repeatable-page-master-alternatives flow object.
     * It is always a child of an fo:page-sequence-master.
     */
    public class FoRepeatablePageMasterAlternatives extends FONode {

	public FoRepeatablePageMasterAlternatives
			    (FOTree foTree, FONode parent, FoXMLEvent event)
	    throws Tree.TreeException, FOPException, PropertyException
	{
	    super(foTree, FObjectNames.REPEATABLE_PAGE_MASTER_ALTERNATIVES,
				parent, event, FOPropertySets.SEQ_MASTER_SET);

	    // Process conditional-page-master-references here
	    try {
		do {
		    FoXMLEvent ev = this.xmlevents.expectStartElement
                            (FObjectNames.CONDITIONAL_PAGE_MASTER_REFERENCE,
                                                    XMLEvent.DISCARD_W_SPACE);
                    if (ev == null) break; // Sub-sequences exhausted
                    //System.out.println
                    //    ("Found conditional-page-master-reference");
                    new FoConditionalPageMasterReference(foTree, this, ev);
                    this.xmlevents.getEndElement(ev);
		} while (true);
	    } catch (NoSuchElementException e) {
		// End of file reached
                throw new FOPException("EOF in repeatable-page-masters.");
	    }
	    XMLEvent ev = this.xmlevents.getEndElement(event);
	}

	public PropertyValue getMaximumRepeats() throws PropertyException {
	    return this.getPropertyValue(PropNames.MAXIMUM_REPEATS);
	}

	public class FoConditionalPageMasterReference extends FONode {

	    public FoConditionalPageMasterReference
			    (FOTree foTree, FONode parent, FoXMLEvent event)
	    throws Tree.TreeException, FOPException, PropertyException
	    {
		super(foTree, FObjectNames.CONDITIONAL_PAGE_MASTER_REFERENCE,
				parent, event, FOPropertySets.SEQ_MASTER_SET);
	    }

	    public PropertyValue getMasterReference() throws PropertyException
	    {
		return this.getPropertyValue(PropNames.MASTER_REFERENCE);
	    }

	    public PropertyValue getPagePosition() throws PropertyException {
		return this.getPropertyValue(PropNames.PAGE_POSITION);
	    }

	    public PropertyValue getOddOrEven() throws PropertyException {
		return this.getPropertyValue(PropNames.ODD_OR_EVEN);
	    }

	    public PropertyValue getBlankOrNotBlank() throws PropertyException
	    {
		return this.getPropertyValue(PropNames.BLANK_OR_NOT_BLANK);
	    }

	} // FoConditionalPageMasterReference

    }// FoRepeatablePageMasterAlternatives

}
