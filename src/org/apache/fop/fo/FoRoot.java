/**
 * $Id$
 * <br/>Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * <br/>For details on use and redistribution please refer to the
 * <br/>LICENSE file included with these sources.
 *
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 * @version $Revision$ $Name$
 */

package org.apache.fop.fo;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FOPropertySets;
import org.apache.fop.fo.FObjectNames;
import org.apache.fop.datastructs.Tree;
import org.apache.fop.fo.FOTree;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.pagination.FoLayoutMasterSet;
import org.apache.fop.xml.FoXMLEvent;
import org.apache.fop.xml.XMLEvent;
import org.apache.fop.xml.XMLNamespaces;
import org.apache.fop.xml.SyncedFoXmlEventsBuffer;

import org.xml.sax.Attributes;

import java.util.NoSuchElementException;

/**
 * <tt>FoRoot</tt> is the class which processes the fo:root start element
 * XML event.
 * <p>
 * The building of all of the fo tree, and the forwarding of FO tree events
 * on to further stages of processing, will all take place within the
 * <tt>buildFoTree()</tt> method of this class instance.
 */

public class FoRoot extends FONode {

    private static final String tag = "$Name$";
    private static final String revision = "$Revision$";

    private FoLayoutMasterSet layoutMasters;

    /**
     * @param foTree the FO tree being built
     * @param event the <tt>FoXMLEvent</tt> that triggered the creation of this
     * node
     */
    public FoRoot
        (FOTree foTree, FoXMLEvent event)
        throws Tree.TreeException, FOPException, PropertyException
    {
        // This is the root node of the tree; hence the null argument
        super(foTree, FObjectNames.ROOT, null, event, FOPropertySets.ROOT_SET);
    }

    /**
     * Process the FO tree, starting with this fo:root element.
     * N.B. the FO tree is a collection of trees.
     * Layout trees only occur with fo:flow and fo:static-content.  These will
     * be built at the appropriate places as part of the FO tree processing.
     * Terminates at the completion of FO tree processing.
     * <p>
     * <tt>fo:root</tt> contents are <br/>
     * (layout-master-set,declarations?,page-sequence+)
     * <p>
     * I.e. the <fo:root> element is the parent of a two-element sequence:
     * (layout-master-set-declarations),(page-sequence-sequence)
     * <p>
     * 'layout-master-set-declarations' is logically an unordered set of
     * (layout-master-set,declarations?), although this definition
     * determines an order of occurrence in the input tree.  It is 
     * unordered in the sense that there is no necessary order in the
     * set.  However, all of the elements of the page-sequence-sequence
     * are ordered.
     * <p>The contents of declarations must be available to all FO tree
     * processing; the contents of the layout-master-set must be available
     * during the page setup phase of the processing of each page-sequence
     * in the page-sequence-sequence.
     */
    public void buildFoTree() throws FOPException{
        FoXMLEvent ev;
        System.out.println("buildFoTree: " + event);
        // Look for layout-master-set
        try {
            ev = xmlevents.expectStartElement
                    (XMLNamespaces.XSLNSpaceIndex, "layout-master-set",
                                                    XMLEvent.DISCARD_W_SPACE);
        } catch (NoSuchElementException e) {
            throw new FOPException("buildFoTree: Unexpected EOF in layout-master-set.");
        }
        // Process the layout-master-set
        try {
            layoutMasters = new FoLayoutMasterSet(foTree, this, ev);
        } catch(Tree.TreeException e) {
            throw new FOPException("TreeException: " + e.getMessage());
        } catch(PropertyException e) {
            throw new FOPException("PropertyException: " + e.getMessage());
        }
        // Look for optional declarations
        try {
            ev = xmlevents.expectStartElement
                        (XMLNamespaces.XSLNSpaceIndex, "declarations",
                                                    XMLEvent.DISCARD_W_SPACE);
            if (ev != null) {
                // process the declarations
                xmlevents.getEndElement
                        (XMLNamespaces.XSLNSpaceIndex, "declarations");
            }
        } catch (NoSuchElementException e) {
            throw new FOPException
                ("Unexpected EOF while processing declarations.");
        }

    }
}
