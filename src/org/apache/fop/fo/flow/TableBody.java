/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo.flow;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.fo.properties.*;
import org.apache.fop.datatypes.*;
import org.apache.fop.layout.*;
import org.apache.fop.apps.FOPException;

// Java
import java.util.Vector;
import java.util.Enumeration;

public class TableBody extends AbstractTableBody {

    public static class Maker extends FObj.Maker {
        public FObj make(FObj parent,
                         PropertyList propertyList) throws FOPException {
            return new TableBody(parent, propertyList);
        }

    }

    public static FObj.Maker maker() {
        return new TableBody.Maker();
    }

    public TableBody(FObj parent, PropertyList propertyList) {
        super(parent, propertyList);
        this.name = "fo:table-body";
    }

}
