/*
 * $Id$
 * 
 * ============================================================================
 *                   The Apache Software License, Version 1.1
 * ============================================================================
 * 
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
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
 *
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 */

package org.apache.fop.fo.pagination;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

// FOP
import org.apache.fop.fo.properties.Property;
import org.apache.fop.fo.properties.*;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObjectNames;
import org.apache.fop.fo.PropNames;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.datatypes.NCName;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.datatypes.EnumType;
import org.apache.fop.datatypes.IntegerType;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.pagination.FoPageSequenceMaster;
import org.apache.fop.fo.pagination.FoPageSequenceMaster
                                                .FoSinglePageMasterReference;
import org.apache.fop.fo.pagination.FoPageSequenceMaster
                                            .FoRepeatablePageMasterReference;
import org.apache.fop.fo.pagination.FoPageSequenceMaster
                                        .FoRepeatablePageMasterAlternatives;
import org.apache.fop.fo.pagination.FoPageSequenceMaster
        .FoRepeatablePageMasterAlternatives.FoConditionalPageMasterReference;

/**
 * Encodes an <b>fo:page-sequence-master</b> and associated
 * conditions.  All <i>page-sequence-master</i> specifications are converted
 * into <i>repeatable-page-master-alternative</i>s.
 * For a <i>simple-page-master</i>, a <i>PageSequenceMaster</i> object is
 * created which shares a name with its target <i>simple-page-master</i>.
 * This procedure results in a single common structure and manner of access
 * for all page masters.
 */
public class PageSequenceMaster {

    private static final String tag = "$Name$";
    private static final String revision = "$Revision$";

    /** Constant representing the <i>no-limit</i> value of the
            maximum-repeats property. */
    public static final int NO_LIMIT = -1;

    private String masterName;

    private ArrayList masters = new ArrayList(1);

    /**
     * Create a <i>PageSequenceMaster</i> from an
     * <tt>FoSimplePageMaster</tt>.
     * @param simplePM - the <tt>FoSimplePageMaster</tt> from which this
     * <i>PageSequenceMaster</i> is derived.
     */
    public PageSequenceMaster(FoSimplePageMaster simplePM)
        throws PropertyException, FOPException
    {
        PageMasterAlternatives masterAlt;
        masterName = simplePM.getMasterName();
        masterAlt = new PageMasterAlternatives(1, NO_LIMIT);
        // Create and add a single default PageCondition
        masterAlt.addCondition
                    (masterAlt.new PageCondition(simplePM,
                                   BlankOrNotBlank.ANY,
                                   OddOrEven.ANY,
                                   PagePosition.ANY));
        masters.add(masterAlt);
    }

    /**
     * Create a <i>PageSequenceMaster</i> from an
     * <tt>FoPageSequenceMaster</tt>.
     * @param pageSeq - the <tt>FoPageSequenceMaster</tt> from which this
     * <i>PageSequenceMaster</i> is derived.
     * @param simplePageMasters - a <tt>HashMap</tt> of
     * <tt>FoSimplePageMaster</tt>s indexed by master-name.
     */
    public PageSequenceMaster
                (FoPageSequenceMaster pageSeq, HashMap simplePageMasters)
        throws PropertyException, FOPException
    {
        String masterRef;
        FoSimplePageMaster simplePM;
        PageMasterAlternatives masterAlt;
        int maxRepeats = 1;
        int enumValue;
        PropertyValue pv;
        Numeric npv;
        masterName = pageSeq.getMasterName();
        // Process the sequence of masters.
        int numChildren = pageSeq.numChildren();
        for (int child = 0; child < numChildren; child++) {
            FONode masterReference = (FONode)(pageSeq.getChild(child));
            switch (masterReference.type) {
            case FObjectNames.SINGLE_PAGE_MASTER_REFERENCE:
                // Get the master-reference property value
                simplePM = getMasterReference
                                        (masterReference, simplePageMasters);
                // Create and add a PageMasterAlternatives
                masterAlt = new PageMasterAlternatives(1, 1);
                // Create and add a single default PageCondition
                masterAlt.addCondition
                            (masterAlt.new PageCondition(simplePM,
                                           BlankOrNotBlank.ANY,
                                           OddOrEven.ANY,
                                           PagePosition.ANY));
                masters.add(masterAlt);
                break;
            case FObjectNames.REPEATABLE_PAGE_MASTER_REFERENCE:
                // Get the master-reference property value
                simplePM = getMasterReference
                                        (masterReference, simplePageMasters);
                // Get the maximum-repeats
                pv = masterReference.getPropertyValue
                                                (PropNames.MAXIMUM_REPEATS);
                switch (pv.getType()) {
                case PropertyValue.ENUM:
                    enumValue = ((EnumType)pv).getEnumValue();
                    if (enumValue == MaximumRepeats.NO_LIMIT) {
                        maxRepeats = NO_LIMIT;
                        break;
                    }
                    throw new FOPException
                            ("Unrecognized maximum-repeats enum: "
                             + enumValue);
                case PropertyValue.INTEGER:
                    maxRepeats = ((IntegerType)pv).getInt();
                    break;
                case PropertyValue.NUMERIC:
                    npv = (Numeric)pv;
                    if (npv.isInteger()) maxRepeats = npv.asInt();
                    break;
                    // else fall through to exception
                default:
                    throw new FOPException
                                        ("MAXIMUM_REPEATS not an integer.");
                }
                // Create and add a PageMasterAlternatives
                masterAlt = new PageMasterAlternatives(0, maxRepeats);
                // Create and add a single default PageCondition
                masterAlt.addCondition
                            (masterAlt.new PageCondition(simplePM,
                                           BlankOrNotBlank.ANY,
                                           OddOrEven.ANY,
                                           PagePosition.ANY));
                masters.add(masterAlt);
                break;
            case FObjectNames.REPEATABLE_PAGE_MASTER_ALTERNATIVES:
                // Get the maximum-repeats
                pv = masterReference.getPropertyValue
                                                (PropNames.MAXIMUM_REPEATS);
                switch (pv.getType()) {
                case PropertyValue.ENUM:
                    enumValue = ((EnumType)pv).getEnumValue();
                    if (enumValue == MaximumRepeats.NO_LIMIT) {
                        maxRepeats = NO_LIMIT;
                        break;
                    }
                    throw new FOPException
                            ("Unrecognized maximum-repeats enum: "
                             + enumValue);
                case PropertyValue.INTEGER:
                    maxRepeats = ((IntegerType)pv).getInt();
                    break;
                case PropertyValue.NUMERIC:
                    npv = (Numeric)pv;
                    if (npv.isInteger()) maxRepeats = npv.asInt();
                    break;
                    // else fall through to exception
                default:
                    throw new FOPException
                                        ("MAXIMUM_REPEATS not an integer.");
                }
                // Create and add a PageMasterAlternatives
                masterAlt = new PageMasterAlternatives(0, maxRepeats);
                // Process the conditional-page-master-alternatives children
                int numConds = masterReference.numChildren();
                for (int cond = 0; cond < numConds; cond++) {
                    FoConditionalPageMasterReference foCond;
                    int blankOrNot, oddOrEven, pagePosition;
                    EnumType enum;

                    foCond = (FoConditionalPageMasterReference)
                                            (masterReference.getChild(cond));
                    // Get the master-reference property value
                    simplePM = getMasterReference(foCond, simplePageMasters);
                    enum = (EnumType)
                                (foCond.getPropertyValue
                                             (PropNames.BLANK_OR_NOT_BLANK));
                    blankOrNot = enum.getEnumValue();
                    enum = (EnumType)
                                (foCond.getPropertyValue
                                             (PropNames.ODD_OR_EVEN));
                    oddOrEven = enum.getEnumValue();
                    enum = (EnumType)
                                (foCond.getPropertyValue
                                             (PropNames.PAGE_POSITION));
                    pagePosition = enum.getEnumValue();
                    // Create and add a single default PageCondition
                    masterAlt.addCondition
                            (masterAlt.new PageCondition
                                 (simplePM,
                                      blankOrNot, oddOrEven, pagePosition));
                }
                masters.add(masterAlt);
                break;
            default:
                throw new FOPException
                        ("Unknown master reference type: "
                         + masterReference.type);
            }
        }
    }

    /**
     * Get the master-name of this <i>PageSequenceMaster</i>.  This is the
     * name by which <i>fo:page-sequence</i>s will reference the master
     * through their <i>master-reference</i> property.
     * @return the name.
     */
    public String getMasterName() {
        return masterName;
    }

    /**
     * Get the <tt>FoSimplePageMaster</tt> referred to by the
     * <i>master-reference</i> property in the argument <tt>FONode</tt>.
     * @param node.
     * @return the <tt>FoSimplePageMaster</tt>.
     */
    private FoSimplePageMaster getMasterReference
                                    (FONode node, HashMap simplePageMasters)
        throws FOPException, PropertyException
    {
        FoSimplePageMaster simplePM;
        // Get the master-reference property value
        String masterRef =
            ((NCName)
             (node.getPropertyValue(PropNames.MASTER_REFERENCE))).getNCName();
        // Valid reference?
        if ((simplePM = (FoSimplePageMaster)(simplePageMasters.get(masterRef)))
                    != null)
            return simplePM;
        throw new FOPException
                ("No simple-page-master referent for "
                     + FObjectNames.getFOName(node.type) + ": " + masterRef);
    }

    /**
     * Add a new alternatives master to the sequence.
     * @param master - a <tt>PageMasterAlternatives</tt>.
     */
    public void addMaster(PageMasterAlternatives master) {
        masters.add(master);
    }

    /**
     * Get the length of the <i>masters</i> <tt>ArrayList</tt>.
     * @return - the length.
     */
    public int getMastersLength() {
        return masters.size();
    }

    /**
     * Get the indexed <tt>PageMasterAlternatives</tt> master.
     * @param i - the index of the master to retrieve.
     * @return - the indexed <tt>PageMasterAlternatives</tt> master.
     */
    public PageMasterAlternatives getMaster(int i) {
        return (PageMasterAlternatives)(masters.get(i));
    }

    /**
     * Encodes an fo:repeatable-page-master-alternatives and associated
     * conditions.
     */
    public class PageMasterAlternatives {

        private static final String tag = "$Name$";
        private static final String revision = "$Revision$";

        /** The minumum number of repeats for this set of alternatives. */
        public final int minRepeats;
        /** The maximum-repeats value for this set of alternatives. */
        public final int maxRepeats;

        private ArrayList alternatives = new ArrayList(1);

        /**
         * @param minRepeats - the <tt>int</tt> minimum number of repeats for
         * this alternative.
         * @param maxRepeats - the <tt>int</tt> maximum number of repeats for
         * this alternative.  Set from the <i>maximum-repeats</i> property.
         */
        public PageMasterAlternatives(int minRepeats, int maxRepeats) {
            this.minRepeats = minRepeats;
            this.maxRepeats = maxRepeats;
            //System.out.println("New PMA: " + minRepeats + " " + maxRepeats);
        }

        /**
         * Add a new alternative condition set.
         * @param condition - a <tt>PageCondition</tt>.
         */
        public void addCondition(PageCondition condition) {
            alternatives.add(condition);
        }

        /**
         * Get the length of the <i>alternatives</i> <tt>ArrayList</tt>.
         * @return - the length.
         */
        public int getAlternativesLength() {
            return alternatives.size();
        }

        /**
         * Get the indexed <tt>PageMasterAlternatives</tt> master.
         * @param i - the index of the master to retrieve.
         * @return - the indexed <tt>PageMasterAlternatives</tt> master.
         */
        public PageCondition getAlternative(int i) {
            return (PageCondition)(alternatives.get(i));
        }

        /**
         * Encodes a condition set from an FoConditionalPageReferenceMaster.
         */
        public class PageCondition {

            private static final String tag = "$Name$";
            private static final String revision = "$Revision$";

            /** The simple page master used when these conditions are true. */
            public final FoSimplePageMaster master;
            /** The blank-or-not-blank condition.
                Encoded from BlankOrNotBlank. */
            public final int blankOrNot;
            /** The odd-or-even condition.
                                        Encoded from OddOrEven. */
            public final int oddOrEven;
            /** The page-position condition.
                                       Encoded from pagePosition. */
            public final int pagePosition;

            /**
             * @param master - a reference to an <i>fo:simple-page-master</i>.
             * @param blankOrNot - an <tt>int</tt> encoding a
             * <i>blank-or-not-blank</i> condition.
             * @param oddOrEven - an <tt>int</tt> encoding an
             * <i>odd-or-even</i> condition.
             * @param pagePosition - an <tt>int</tt> encoding a
             * <i>page-position</i>
             * condition.
             */
            public PageCondition(FoSimplePageMaster master,
                            int blankOrNot, int oddOrEven, int pagePosition) {
                this.master       = master;
                this.blankOrNot   = blankOrNot;
                this.oddOrEven    = oddOrEven;
                this.pagePosition = pagePosition;
                //System.out.println("New condition: "
                //                   + blankOrNot
                //                   + " "+ oddOrEven + " " + pagePosition);
            }
        }

    }

}
