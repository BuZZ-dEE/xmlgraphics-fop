/*
 * ============================================================================
 *                   The Apache Software License, Version 1.1
 * ============================================================================
 * 
 * Copyright (C) 1999-2004 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of  source code must  retain the above copyright  notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include  the following  acknowledgment:  "This product includes  software
 *    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
 *    Alternately, this  acknowledgment may  appear in the software itself,  if
 *    and wherever such third-party acknowledgments normally appear.
 * 
 * 4. The names "FOP" and  "Apache Software Foundation"  must not be used to
 *    endorse  or promote  products derived  from this  software without  prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 * 
 * 5. Products  derived from this software may not  be called "Apache", nor may
 *    "Apache" appear  in their name,  without prior written permission  of the
 *    Apache Software Foundation.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 * APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 * ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 * (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * This software  consists of voluntary contributions made  by many individuals
 * on  behalf of the Apache Software  Foundation and was  originally created by
 * James Tauber <jtauber@jtauber.com>. For more  information on the Apache 
 * Software Foundation, please see <http://www.apache.org/>.
 *  
 * FONode.java
 * Created: Sat Nov 10 01:39:37 2001
 * $Id: FONode.java,v 1.19.2.33 2003/06/12 18:19:33 pbwest Exp $
 * 
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 * @version $Revision: 1.19.2.33 $ $Name:  $
 */
package org.apache.fop.fo;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datastructs.Node;
import org.apache.fop.datastructs.ROBitSet;
import org.apache.fop.datastructs.TreeException;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.datatypes.PropertyValueList;
import org.apache.fop.datatypes.TextDecorations;
import org.apache.fop.datatypes.indirect.IndirectValue;
import org.apache.fop.fo.expr.FunctionNotImplementedException;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.expr.PropertyParser;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.messaging.MessageHandler;
import org.apache.fop.xml.XmlEvent;
import org.apache.fop.xml.SyncedXmlEventsBuffer;
import org.apache.fop.xml.Namespaces;

/**
 * Class for nodes in the FO tree.
 */

public class FONode extends Node{

    private static final String tag = "$Name:  $";
    private static final String revision = "$Revision: 1.19.2.33 $";

    /**
     * State flags: a bit set of states applicable during FO tree build.
     * N.B. States must be powers of 2.
     * <p><b>BEWARE</b> At what point are these ancestry flags supposed to
     * apply?  If they are son-of (MC), they should only be expected to
     * be applied on the children of a particular node type.  I don't think
     * the higher level FO nodes are making this assumption.
     * <p>Just changed most of the higher-level flags by removing the MC_
     * prefix, which remains on some of the lower levels.  Use this convention
     * for now: unprefixed name (e.g. Root), is a self-or-descendent
     * indicator, while MC_ prefixed names are descendents only.
     * <p>TODO: check for consistency of application.
     */
    public static final int
                     NOSTATE = 0
                // These are used to select the attribute set for the node
                       ,ROOT = 1
               ,DECLARATIONS = 2
                     ,LAYOUT = 4
                 ,SEQ_MASTER = 8
                    ,PAGESEQ = 16
                       ,FLOW = 32
                     ,STATIC = 64
                      ,TITLE = 128
                  ,MC_MARKER = 256
                   ,MC_FLOAT = 512
                ,MC_FOOTNOTE = 1024
              ,MC_MULTI_CASE = 2048
   ,MC_ABSOLUTELY_POSITIONED = 4096
                        ;

    public static final int
                ROOT_SET = ROOT
       ,DECLARATIONS_SET = ROOT | DECLARATIONS
             ,LAYOUT_SET = ROOT | LAYOUT
         ,SEQ_MASTER_SET = LAYOUT_SET | SEQ_MASTER
            ,PAGESEQ_SET = ROOT | PAGESEQ
               ,FLOW_SET = PAGESEQ_SET | FLOW
             ,STATIC_SET = PAGESEQ_SET | STATIC
             ,TITLE_SET = PAGESEQ_SET | TITLE
            ,MARKER_SET = FLOW_SET | MC_MARKER
                        ;

    public static final int MC_OUT_OF_LINE =
        MC_FLOAT | MC_FOOTNOTE | MC_ABSOLUTELY_POSITIONED;

    /** The subset of <i>stateFlags</i> that select the relevant
        atttribute set or the node. */
    public static final int ATTRIBUTESETS = 
        ROOT | DECLARATIONS | LAYOUT | SEQ_MASTER |
        PAGESEQ | FLOW | STATIC | TITLE | MC_MARKER;

    /** The buffer from which parser events are drawn. */
    protected final SyncedXmlEventsBuffer xmlevents;

    /** The namespaces object associated with <i>xmlevents</i>. */
    protected Namespaces namespaces;

    /** The FO type. */
    public final int type;

    /** The attributes defined on this node. When the FO subtree of this
     * node has been constructed, it will be deleted. */
    public FOAttributes foAttributes;

    /** The map of properties specified on this node. N.B. This
      * <tt>HashMap</tt> starts life in FOAttributes.  It is modifiable, and
      * will be modified when is contains shorthands or compounds.
      * When the FO subtree of this node has been constructed, and the
      * <i>propertySet</i> is complete, it will be deleted. */
    public HashMap foProperties = null;

    /** The sorted keys of <i>foProperties</i>. */
    protected Integer[] foKeys = null;

    /** The size of <i>foKeys</i>. */
    private int numAttrs = 0;

    /** BitSet of properties which have been specified on this node. */
    private BitSet specifiedProps =
                                new BitSet(PropNames.LAST_PROPERTY_INDEX + 1);

    /** The property set for this node.  This reference has two lives.
        During FO subtree building, it holds all values which may potentially
        be defined on the node.  It must, therefore, be able to accommodate
        every property.  When FO subtree construction is completed, the
        <i>sparsePropsSet</i> array is constructed for use during Area
        tree building, and <i>propertySet</i> is nullified.
        While <i>sparsePropsSet</i> is null,
        this variable will be a reference to the complete property set. */
    private PropertyValue[] propertySet;

    /** The set of properties directly applicable to this node.  Its size is
        determined by the <i>numProps</i> value passed in to the constructor.
        */
    private PropertyValue[] sparsePropsSet;

    /** Map of <tt>Integer</tt> indices of <i>sparsePropsSet</i> array.
        It is indexed by the FO index of the FO associated with a given
        position in the <i>propertySet</i> array. */
    private final int[] sparsePropsMap;

    /** An array of of the applicable property indices, in property index
        order. */
    private final int[] sparseIndices;

    /** The number of applicable properties. Size of <i>sparsePropsSet</i>. */
    private final int numProps;

    /** The property expression parser in the FOTree. */
    protected PropertyParser exprParser;

    /** The <tt>ROBitSet</tt> from the <i>stateFlags</i> argument. */
    protected ROBitSet attrBitSet;

    /** The state flags passed to this node. */
    protected int stateFlags;

    /** Ancestor reference area of this FONode. */
    protected FONode ancestorRefArea = null;

    /**
     * @param foTree an <tt>FOTree</tt> to which this node belongs
     * @param type the fo type of this FONode.
     * @param parent an <tt>FONode</tt>, the parent node of this node in
     * <i>foTree</i>
     * @param event the <tt>XmlEvent</tt> that triggered the creation of this
     * node.
     * @param stateFlags - the set of states relevant at this point in the
     * tree.  Includes the state information necessaryto select an attribute
     * set for this node.
     * @param sparsePropsMap  maps the property indices
     * to their offsets in the set of properties applicable to this node.
     * @param sparseIndices  holds the set of property
     * indices applicable to this node, in ascending order.
     * <i>sparsePropsMap</i> maps property indices to a position in this array.
     * Together they provide a sparse array facility for this node's
     * properties.
     */
    public FONode
        (FOTree foTree, int type, FONode parent, XmlEvent event,
             int stateFlags, int[] sparsePropsMap, int[] sparseIndices)
        throws TreeException, FOPException, PropertyException
    {
        super(foTree, parent);
        this.type = type;
        this.stateFlags = stateFlags;
        this.sparsePropsMap = sparsePropsMap;
        this.sparseIndices = sparseIndices;
        this.numProps = sparseIndices.length;
        attrBitSet = FOPropertySets.getAttrROBitSet(stateFlags);
        xmlevents = foTree.xmlevents;
        namespaces = xmlevents.getNamespaces();
        exprParser = foTree.exprParser;
        propertySet = new PropertyValue[PropNames.LAST_PROPERTY_INDEX + 1];
        foAttributes = new FOAttributes(event, this);
        if ((stateFlags & MC_MARKER) == 0) {
            processAttributes();
        }
        // Do not set up the remaining properties now.
        // These will be developed by inheritance or from the initial values
        // as the property values are referenced.
    }

    private void processAttributes() throws FOPException, PropertyException {
        // Process the FOAttributes - parse and stack the values
        // Build a HashMap of the properties defined on this node
        foProperties = foAttributes.getFoAttrMap();
        numAttrs = foProperties.size();
        if (numAttrs > 0) {
            foKeys = foAttributes.getFoAttrKeys();
        }
        for (int propx = 0; propx < numAttrs; propx++) {
            PropertyValue props;
            int ptype;
            int property;
            int prop = foKeys[propx].intValue();
            if ( ! attrBitSet.get(prop)) {
                MessageHandler.logln("Ignoring "
                                   + PropNames.getPropertyName(prop)
                                   + " on "
                                   + FObjectNames.getFOName(type)
                                   + " for attribute set "
                                   + FOPropertySets.getAttrSetName(stateFlags)
                                   + ".");
                continue;
            }
            String attrValue = foAttributes.getFoAttrValue(prop);
            try {
                props = handleAttrValue(prop, attrValue);
                ptype = props.getType();
                if (ptype != PropertyValue.LIST) { 
                    property = props.getProperty();
                    // Update the propertySet
                    propertySet[property] = props;
                    specifiedProps.set(property);
                    // Handle corresponding properties here
                } else { // a list
                    PropertyValue value;
                    Iterator propvals = ((PropertyValueList)props).iterator();
                    while (propvals.hasNext()) {
                        value = (PropertyValue)(propvals.next());
                        property = value.getProperty();
                        propertySet[value.getProperty()] = value;
                        specifiedProps.set(property);
                        // Handle corresponding properties here
                    }
                }
            } catch (FunctionNotImplementedException e) {
                MessageHandler.logln
                        ("Function not implemented: " + e.getMessage()
                         + ". Ignoring property '"
                         + PropNames.getPropertyName(prop) + "'.");
            } catch (PropertyException e) {
                MessageHandler.logln
                        ("Problem with '" + PropNames.getPropertyName(prop)
                         + "':\n" + e.getMessage() + "\nIgnoring property.");
            }
        }
    }

    private PropertyValue handleAttrValue(int property, String attrValue)
        throws FunctionNotImplementedException, PropertyException
    {
        // parse the expression
        exprParser.resetParser();
        Property prop = PropertyConsts.pconsts.setupProperty(property);
        PropertyValue pv = exprParser.parse(this, property, attrValue);
        return prop.refineParsing(pv.getProperty(), this, pv);
    }

    /**
     * Reduce the properties currently associated with the node to a
     * sparse propeties set of only those properties relevant to the node.
     * During tree building, a node may have associated with it properties
     * which have no direct relevance to the node, but which may be used
     * by descendant nodes.  Once the tree building process is finished, these
     * properties are no longer required.  The property set for the node can
     * be reduced to the minimum required for this formatting object.
     * This minimal set is maintained in a sparse array.
     * @see #sparsePropsSet
     * @see #sparsePropsMap
     * @see #sparseIndices
     */
    public void makeSparsePropsSet() throws PropertyException {
        sparsePropsSet = new PropertyValue[numProps];
        // Scan the sparseIndices array, and copy the PropertyValue from
        // propertySet[], if it exists.  Else generate the pertinent value
        // for that property.
        for (int i = 0; i < numProps; i++)
            sparsePropsSet[i] = getPropertyValue(sparseIndices[i]);
        // Clean up structures that are no longer needed
        propertySet = null;
        specifiedProps = null;
        attrBitSet = null;
        foKeys = null;
        foProperties = null;
        foAttributes = null;
    }

    /**
     * Get the <i>sparsePropsSet</i> for this node.
     * @return the <tt>PropertyValue[]</tt>.
     */
    /*  DEBUG
    public PropertyValue[] getSparsePropsSet() {
        return sparsePropsSet;
    }
    */

    /**
     * Get the <i>sparsePropsMap</i> for this node.
     * @return the <tt>int[]</tt>.
     */
    /*  DEBUG
    public int[] getSparsePropsMap() {
        return sparsePropsMap;
    }
    */

    /**
     * Get the eclosing <tt>FOTree</tt> instance of this <tt>FONode</tt>.
     * @return the <tt>FOTree</tt>.
     */
    public FOTree getFOTree() {
        return (FOTree)tree;
    }

    /**
     * Get the adjusted <tt>PropertyValue</tt> of the property
     * on the nearest ancestor with a specified value for that property.
     * @see #fromNearestSpecified(int,int)
     * @see #getNearestSpecifiedValue(int)
     * @param property - the index of both target and source properties.
     * to the PropertyTriplet.
     * @return - the adjusted value corresponding to the nearest specified
     * value if it exists, else the adjusted initial value.
     */
    public PropertyValue fromNearestSpecified(int property)
                throws PropertyException
    {
        return fromNearestSpecified(property, property);
    }

    /**
     * Get the adjusted <tt>PropertyValue</tt> of the source property
     * on the nearest ancestor with a specified value for that property.
     * <p>If this node is not the root, call the
     * <i>getNearestSpecifiedValue</i> method in the parent node, adjust
     * that value, and return the adjusted value. Do not set the current
     * value of the property on this node.
     * <p>If this is the root node, return the adjusted initial value for the
     * property.  Do not set the current value of the property on this node.
     * <p>The <b>adjusted value</b> is either the value itself, or, if the
     * value is an unresolved relative length, an <tt>IndirectValue</tt>
     * referring to that unresolved length.
     * Cf. {@link #getNearestSpecifiedValue(int)}.
     * @param property - the index of the target property.
     * @param sourceProperty - the index of the source property.
     * @return - the adjusted value corresponding to the nearest specified
     * value if it exists, else the adjusted initial value.
     */
    public PropertyValue fromNearestSpecified
                                        (int property, int sourceProperty)
                throws PropertyException
    {
        if (parent != null)
            return IndirectValue.adjustedPropertyValue
                (((FONode)parent).getNearestSpecifiedValue(sourceProperty));
        else // root
            return IndirectValue.adjustedPropertyValue
                    (PropertyConsts.pconsts.getInitialValue(sourceProperty));
    }

    /**
     * Get the adjusted <tt>PropertyValue</tt> of the property on the nearest
     * ancestor with a specified value for the given property.
     * <p>If a value has been specified on this node, return the adjusted
     * value.
     * <p>Otherwise, if the this node is not the root, return the adjusted
     * value from a recursive call.
     * <p>If this is the root node, return the adjusted initial value for the
     * property.
     * <p>The <b>adjusted value</b> is either the value itself, or, if the
     * value is an unresolved relative length, an <tt>IndirectValue</tt>
     * referring to that unresolved length.
     * @param property - the property of interest.
     * @return the adjusted value of the nearest specified
     * <tt>PropertyValue</tt>.
     */
    public PropertyValue getNearestSpecifiedValue(int property)
                throws PropertyException
    {
        if (specifiedProps.get(property))
            return IndirectValue.adjustedPropertyValue(propertySet[property]);
        if (parent != null)
            return IndirectValue.adjustedPropertyValue
                        (((FONode)parent).getNearestSpecifiedValue(property));
        else // root
            return IndirectValue.adjustedPropertyValue
                        (PropertyConsts.pconsts.getInitialValue(property));
    }

    /**
     * Get the adjusted value from the parent FO of the source property.
     * @see #fromParent(int,int)
     * @see #getPropertyValue(int)
     * @param property - the index of both target and source properties.
     * @return - the adjusted value from the parent FO node, if it exists.
     * If not, get the adjusted initial value.
     */
    public PropertyValue fromParent(int property)
                throws PropertyException
    {
        return fromParent(property, property);
    }

    /**
     * Get the adjusted <tt>PropertyValue</tt> for the given source property
     * on the parent <tt>FONode</tt>. If this node is not the root,
     * call the <i>getPropertyValue</i> method in the parent node, adjust
     * that value, and return the adjusted value. Do not set the current
     * value of the property on this node.
     * <p>If this is the root node, return the adjusted initial value for the
     * property.  Do not set the current value of the property on this node.
     * <p>The <b>adjusted value</b> is either the value itself, or, if the
     * value is an unresolved relative length, an <tt>IndirectValue</tt>
     * referring to that unresolved length.
     * Cf. {@link #getPropertyValue(int)}.
     * @param property - the index of the target property.
     * @param sourceProperty - the index of the source property.
     * @return - the computed value from the parent FO node, if it exists.
     * If not, get the adjusted initial value.
     */
    public PropertyValue fromParent(int property, int sourceProperty)
                throws PropertyException
    {
        if (parent != null)
            return IndirectValue.adjustedPropertyValue
                        (((FONode)parent).getPropertyValue(sourceProperty));
        else // root
            return IndirectValue.adjustedPropertyValue
                    (PropertyConsts.pconsts.getInitialValue(sourceProperty));
    }


    /**
     * Get the adjusted <tt>PropertyValue</tt> for the given property index.
     * <pre>
     * If the property has a value in the node, return that adjusted value.
     * If not, and
     *     if this node is not the root,
     *                     and the property is an inherited property,
     *         call this method in the parent node,
     *         adjust that that value,
     *         set this node's value to the adjusted value,
     *         and return the adjusted value.
     *     else this node is the root, or the property is not inherited
     *         get the adjusted initial value of the property
     *         set the property value in this node to that value,
     *         and return that value.
     * <pre>
     * <p>The <b>adjusted value</b> is either the value itself, or, if the
     * value is an unresolved relative length, an <tt>IndirectValue</tt>
     * referring to that unresolved length
     * @param property  the property index
     * @return a <tt>PropertyValue</tt> containing the adjusted property
     * value for the indexed property
     */
    public PropertyValue getPropertyValue(int property)
                throws PropertyException
    {
        PropertyValue pval;
        if (propertySet == null) {
            return IndirectValue.adjustedPropertyValue
                                (sparsePropsSet[ sparsePropsMap[property] ]);
        }
        if ((pval = propertySet[property]) != null) 
            return IndirectValue.adjustedPropertyValue(pval);
        if (parent != null && PropertyConsts.pconsts.isInherited(property))
            return (propertySet[property] =
                           IndirectValue.adjustedPropertyValue
                            (((FONode)parent).getPropertyValue(property)));
        else // root
            return (propertySet[property] =
                    IndirectValue.adjustedPropertyValue
                        (PropertyConsts.pconsts.getInitialValue(property)));
    }

    /**
     * Get the property value for the given property from the
     * <i>sparsePropsSet</i> array.
     * @param prop - the <tt>int</tt> property index.
     * @return the <tt>PropertyValue</tt> for the specified property.
     */
    public PropertyValue getSparsePropValue(int prop) {
        return sparsePropsSet[ sparsePropsMap[prop] ];
    }


    /**
     * Clone the adjusted <tt>PropertyValue</tt> for the given property index.
     * <p>The <b>adjusted value</b> is either the value itself, or, if the
     * value is an unresolved relative length, an <tt>IndirectValue</tt>
     * referring to that unresolved length.
     * Cf. {@link #getPropertyValue(int)}.
     * @param index - the property index.
     * @return a <tt>PropertyValue</tt> containing a clone of the adjusted
     * property value for the indexed property.
     */
    public PropertyValue clonePropertyValue(int index)
                throws PropertyException
    {
        PropertyValue tmpval = getPropertyValue(index);
        try {
            return (PropertyValue)(tmpval.clone());
        } catch (CloneNotSupportedException e) {
            throw new PropertyException("Clone not supported.");
        }
    }

    /**
     * Get the current font size.  This is a reference to the
     * <tt>PropertyValue</tt> located.
     * @return a <tt>Numeric</tt> containing the current font size
     * @exception PropertyException if current font size is not defined,
     * or is not expressed as a <tt>Numeric</tt>.
     */
    public Numeric currentFontSize() throws PropertyException {
        PropertyValue fontsize = getPropertyValue(PropNames.FONT_SIZE);
        if ( ! (fontsize.getType() == PropertyValue.NUMERIC
                            && ((Numeric)fontsize).isLength()))
            throw new PropertyException
                    ("font-size value is not a length.");
        return (Numeric)fontsize;
    }

    /**
     * Clone the current font size.
     * @return a <tt>Numeric</tt> containing the current font size
     * @exception PropertyException if current font size is not defined,
     * or is not expressed as a <tt>Numeric</tt>, or if cloning is not
     * supported.
     */
    public Numeric cloneCurrentFontSize() throws PropertyException {
        Numeric tmpval = currentFontSize();
        try {
            return (Numeric)(tmpval.clone());
        } catch (CloneNotSupportedException e) {
            throw new PropertyException(e);
        }
    }

    /**
     * Clone the current <i>TextDecorations</i> property.
     * @return a <tt>TextDecorations</tt> object containing the current
     * text decorations
     * @exception PropertyException if current text decorations are not
     * defined, or are not expressed as <tt>TextDecorations</tt>.
     */
    public TextDecorations cloneCurrentTextDecorations()
            throws PropertyException
    {
        PropertyValue textdec = getPropertyValue(PropNames.TEXT_DECORATION);
        if (textdec.getType() != PropertyValue.TEXT_DECORATIONS)
            throw new PropertyException
                ("text-decoration value is not a TextDecorations object.");
        try {
            return (TextDecorations)(textdec.clone());
        } catch (CloneNotSupportedException e) {
            throw new PropertyException("Clone not supported.");
        }
    }

}// FONode
