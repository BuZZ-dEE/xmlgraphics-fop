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
package org.apache.fop.svg;

import org.apache.fop.fo.TreeBuilder;
import org.apache.fop.fo.FOTreeBuilder;
import org.apache.fop.fo.ElementMapping;

public class SVGElementMapping implements ElementMapping {

	public void addToBuilder(TreeBuilder builder) {
		String uri = "http://www.w3.org/2000/svg";
		builder.addMapping(uri, "svg", SVG.maker());
		builder.addMapping(uri, "rect", Rect.maker());
		builder.addMapping(uri, "line", Line.maker());
		builder.addMapping(uri, "text", Text.maker());

		builder.addMapping(uri, "desc", Desc.maker());
		builder.addMapping(uri, "title", Title.maker());
		builder.addMapping(uri, "circle", Circle.maker());
		builder.addMapping(uri, "ellipse", Ellipse.maker());
		builder.addMapping(uri, "g", G.maker());
		builder.addMapping(uri, "polyline", Polyline.maker());
		builder.addMapping(uri, "polygon", Polygon.maker());
		builder.addMapping(uri, "defs", Defs.maker());
		builder.addMapping(uri, "path", Path.maker());
		builder.addMapping(uri, "use", Use.maker());
		builder.addMapping(uri, "tspan", Tspan.maker());
		builder.addMapping(uri, "tref", Tref.maker());
		builder.addMapping(uri, "image", Image.maker());
		builder.addMapping(uri, "style", Style.maker());

// elements in progress
		builder.addMapping(uri, "textPath", TextPath.maker());
		builder.addMapping(uri, "clipPath", ClipPath.maker());
		builder.addMapping(uri, "mask", Mask.maker());
		builder.addMapping(uri, "linearGradient", LinearGradient.maker());
		builder.addMapping(uri, "radialGradient", RadialGradient.maker());
		builder.addMapping(uri, "stop", Stop.maker());
		builder.addMapping(uri, "a", A.maker());
		builder.addMapping(uri, "switch", Switch.maker());
		builder.addMapping(uri, "symbol", Symbol.maker());

// elements below will not work
		builder.addMapping(uri, "pattern", Pattern.maker());

		builder.addMapping(uri, "marker", Marker.maker());
		builder.addMapping(uri, "animate", Animate.maker());
		builder.addMapping(uri, "altGlyph", AltGlyph.maker());
		builder.addMapping(uri, "font", Font.maker());
		builder.addMapping(uri, "glyph", Glyph.maker());
		builder.addMapping(uri, "missing-glyph", MissingGlyph.maker());
		builder.addMapping(uri, "hkern", Hkern.maker());
		builder.addMapping(uri, "vkern", Vkern.maker());
		builder.addMapping(uri, "set", Set.maker());
		builder.addMapping(uri, "animateMotion", AnimateMotion.maker());
		builder.addMapping(uri, "animateColor", AnimateColor.maker());
		builder.addMapping(uri, "animateTransform", AnimateTransform.maker());
		builder.addMapping(uri, "cursor", Cursor.maker());
		builder.addMapping(uri, "filter", Filter.maker());
	}
}
