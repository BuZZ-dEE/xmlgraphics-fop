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
import org.apache.fop.layout.Region;
import org.apache.fop.apps.FOPException;				   

public class RegionAfter extends FObj {
	
    public static class Maker extends FObj.Maker {
	public FObj make(FObj parent, PropertyList propertyList) throws FOPException {
	    return new RegionAfter(parent,propertyList);
	}
    }

    public static FObj.Maker maker() {
	return new RegionAfter.Maker();
    }

    SimplePageMaster layoutMaster;
	String regionName;
	
    protected RegionAfter(FObj parent, PropertyList propertyList)
	throws FOPException {
	super(parent, propertyList);
	this.name = "fo:region-after";

	// regions may have name, or default
	if (null == this.properties.get("region-name"))
		regionName = "xsl-region-after";
	else if (this.properties.get("region-name").getString().equals(""))
		regionName = "xsl-region-after";
	else
	{
		regionName = this.properties.get("region-name").getString();
		// check that name is OK. Not very pretty.
		if (regionName.equals( "xsl-region-before" ) || regionName.equals( "xsl-region-start" )
			|| regionName.equals( "xsl-region-end" ) || regionName.equals( "xsl-region-body" )
			|| regionName.equals( "xsl-before-float-separator" )
			|| regionName.equals( "xsl-footnote-separator" ))
			{
	    		throw new FOPException("region-name '" + regionName
				   + "' for fo:region-after not permitted."); 
			}
	}
	
	if (parent.getName().equals("fo:simple-page-master")) {
	    this.layoutMaster = (SimplePageMaster) parent;
	    this.layoutMaster.setRegionAfter(this);
	} else {
	    throw new FOPException("region-after must be child "
				   + "of simple-page-master, not "
				   + parent.getName()); 
	}
    }

    Region makeRegion(int allocationRectangleXPosition,
		      int allocationRectangleYPosition,
		      int allocationRectangleWidth,
		      int allocationRectangleHeight) {
	int marginTop = this.properties.get("margin-top").getLength().mvalue();
	int marginBottom = this.properties.get("margin-bottom").getLength().mvalue();
	int marginLeft = this.properties.get("margin-left").getLength().mvalue();
	int marginRight = this.properties.get("margin-right").getLength().mvalue();
	int extent = this.properties.get("extent").getLength().mvalue();

	return new Region(allocationRectangleXPosition + marginLeft,
			  allocationRectangleYPosition -
			  allocationRectangleHeight + extent,
			  allocationRectangleWidth - marginLeft -
			  marginRight,extent);
    }

	public String getRegionName()
	{
		return regionName;
	}
}
