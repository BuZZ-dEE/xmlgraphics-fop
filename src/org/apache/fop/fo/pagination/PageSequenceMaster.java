/*-- $Id$ -- 

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================
 
    Copyright (C) 1999 The Apache Software Foundation. All rights reserved.
 
 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:
 
 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.
 
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.
 
 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.
 
 4. The names "Fop" and  "Apache Software Foundation"  must not be used to
    endorse  or promote  products derived  from this  software without  prior
    written permission. For written permission, please contact
    apache@apache.org.
 
 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.
 
 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 James Tauber <jtauber@jtauber.com>. For more  information on the Apache 
 Software Foundation, please see <http://www.apache.org/>.
 
 */
package org.apache.fop.fo.pagination;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.fo.properties.*;
import org.apache.fop.layout.PageMaster;
import org.apache.fop.layout.Region;
import org.apache.fop.apps.FOPException;				   
import org.apache.fop.messaging.MessageHandler;

// Java
import java.util.Vector;
import java.util.Enumeration;

public class PageSequenceMaster extends FObj {
	
    public static class Maker extends FObj.Maker {
	public FObj make(FObj parent, PropertyList propertyList)
	    throws FOPException {
	    return new PageSequenceMaster(parent, propertyList);
	}
    }

    public static FObj.Maker maker() {
	return new PageSequenceMaster.Maker();
    }

    LayoutMasterSet layoutMasterSet;
    Vector subSequenceSpecifiers;
    SubSequenceSpecifier currentPmr;
	private int ssIndex;
	
	// SimplePageMasters are not exposed outside this class. Hence, this
	// variable tracks the current master-name for the last SPM.
	String currentPageMasterName;
	
    // The terminology may be confusing. A 'page-sequence-master' consists
    // of a sequence of what the XSL spec refers to as
    // 'sub-sequence-specifiers'. These are, in fact, simple or complex
    // references to page-masters. So the methods use the former
	// terminology ('sub-sequence-specifiers', or SSS),
    // but the actual FO's are MasterReferences.
    protected PageSequenceMaster(FObj parent, PropertyList propertyList)
	throws FOPException {
	super(parent, propertyList);
	this.name = "fo:page-sequence-master";

	subSequenceSpecifiers = new Vector();
	ssIndex = 0;
	
	if (parent.getName().equals("fo:layout-master-set")) {
	    this.layoutMasterSet = (LayoutMasterSet) parent;
	    String pm = this.properties.get("master-name").getString();
	    if (pm.equals("")) {
		System.err.println("WARNING: page-sequence-master does not have "
				   + "a page-master-name and so is being ignored");
	    } else {
		this.layoutMasterSet.addPageSequenceMaster(pm, this);
	    }
	} else {
	    throw new FOPException("fo:page-sequence-master must be child "
				   + "of fo:layout-master-set, not " 
				   + parent.getName());
	}
    }
    
    protected void addSubsequenceSpecifier( SubSequenceSpecifier pageMasterReference )
    {
		subSequenceSpecifiers.addElement( pageMasterReference );
    }

    protected SubSequenceSpecifier getNextSubsequenceSpecifier()
    {
		if (ssIndex == subSequenceSpecifiers.size())
			return null;
		SubSequenceSpecifier pmr = (SubSequenceSpecifier)subSequenceSpecifiers.elementAt( ssIndex );
		ssIndex++;
		return pmr;
    }

    public PageMaster getNextPageMaster( int currentPageNumber, boolean thisIsFirstPage,
		boolean isEmptyPage )
    {
		if (null == currentPmr)
		{
			currentPmr = getNextSubsequenceSpecifier();
		}
		
	    String nextPageMaster =
			currentPmr.getNextPageMaster( currentPageNumber, thisIsFirstPage, isEmptyPage );
		
		if (null == nextPageMaster)
		{
			currentPmr = getNextSubsequenceSpecifier();
			nextPageMaster =
				currentPmr.getNextPageMaster( currentPageNumber, thisIsFirstPage, isEmptyPage );
		}
		
		SimplePageMaster spm = this.layoutMasterSet.getSimplePageMaster( nextPageMaster );
		currentPageMasterName = spm.getMasterName();	// store for outside access

		return spm.getPageMaster();
    }
	
	/**
	  * Return the 'master-name' for the last SimplePageMaster
	  * processed in this class
	  * @returns String master name for last SPM
	  */
	public String getNextPageMasterName()
	{
		return currentPageMasterName;
	}
	
	public void reset()
	{
		for (Enumeration e = subSequenceSpecifiers.elements(); e.hasMoreElements(); )
		{
			((SubSequenceSpecifier)e.nextElement()).reset();
		}
		ssIndex = 0;
	}
	
	public boolean isFlowForMasterNameDone( String masterName )
	{		
		// parameter is master-name of PMR; we need to locate PM
		// referenced by this, and determine whether flow(s) are OK
		if (this.layoutMasterSet.isFlowForMasterNameDone( masterName ))
			return true;
		else
			return false;
	}
}
