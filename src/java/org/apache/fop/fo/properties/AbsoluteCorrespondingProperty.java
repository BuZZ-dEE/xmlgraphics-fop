/*
 *
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created on 20/04/2004
 * $Id$
 */
package org.apache.fop.fo.properties;

import org.apache.fop.fo.FONode;
import org.apache.fop.fo.expr.PropertyException;

/**
 * Interface implemented by those <code>Property</code> classes which have
 * corresponding <b><i>relative</i></b> properties; <i>i.e.</i> which are
 * themselves corresponding <i>absolute</i> properties.
 * 
 * @author pbw
 * @version $Revision$ $Name$
 */
public interface AbsoluteCorrespondingProperty {
    public int getWritingMode (FONode foNode)
    throws PropertyException;
    public int getCorrespondingRelativeProperty(FONode foNode)
    throws PropertyException;
    public boolean overridesCorresponding(FONode foNode);
}
