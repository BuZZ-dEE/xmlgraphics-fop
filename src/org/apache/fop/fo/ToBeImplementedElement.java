/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo;

// FOP
import org.apache.fop.fo.flow.*;
import org.apache.fop.fo.properties.*;
import org.apache.fop.layout.Area;
import org.apache.fop.apps.FOPException;

/**
 */
public abstract class ToBeImplementedElement extends FObj {

    protected ToBeImplementedElement(FObj parent, PropertyList propertyList)
            throws FOPException {
        super(parent, propertyList);
    }

    public int layout(Area area) throws FOPException {
      log.debug("The element '" + this.getName()
                + "' is not yet implemented.");
        return Status.OK;
    }

}
