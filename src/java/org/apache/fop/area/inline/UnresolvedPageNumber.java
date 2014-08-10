/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.area.inline;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

import org.apache.fop.area.PageViewport;
import org.apache.fop.area.Resolvable;
import org.apache.fop.complexscripts.bidi.InlineRun;
import org.apache.fop.fonts.Font;

/**
 * Unresolvable page number area.
 * This is a word area that resolves itself to a page number
 * from an id reference.
 */
public class UnresolvedPageNumber extends TextArea implements Resolvable {


    private static final long serialVersionUID = -1758090835371647980L;

    private boolean resolved;
    private String pageIDRef;
    private String text;
    private boolean pageType;

    /** Indicates that the reference refers to the first area generated by a formatting object. */
    public static final boolean FIRST = true;
    /** Indicates that the reference refers to the last area generated by a formatting object. */
    public static final boolean LAST = false;

    //Transient fields
    private transient Font font;

    public UnresolvedPageNumber() {
        this(null, null, FIRST);
    }

    /**
     * Create a new unresolved page number.
     *
     * @param id the id reference for resolving this
     * @param f  the font for formatting the page number
     */
    public UnresolvedPageNumber(String id, Font f) {
        this(id, f, FIRST);
    }

    /**
     * Create a new unresolved page number.
     *
     * @param id   the id reference for resolving this
     * @param f    the font for formatting the page number
     * @param type indicates whether the reference refers to the first or last area generated by
     *             a formatting object
     */
    public UnresolvedPageNumber(String id, Font f, boolean type) {
        pageIDRef = id;
        font = f;
        text = "?";
        pageType = type;
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
    }

    /**
     * Get the id references for this area.
     *
     * @return the id reference for this unresolved page number
     */
    public String[] getIDRefs() {
        return new String[] {pageIDRef};
    }

    /**
     * Get the (resolved or unresolved) text.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * Resolve the page number idref
     * This resolves the idref for this object by getting the page number
     * string from the first page in the list of pages that apply
     * for this ID.  The page number text is then set to the String value
     * of the page number.
     *
     * TODO: [GA] May need to run bidi algorithm and script processor
     * on resolved page number.
     *
     * @param id an id whose PageViewports have been determined
     * @param pages the list of PageViewports associated with this ID
     */
    public void resolveIDRef(String id, List<PageViewport> pages) {
        if (!resolved && pageIDRef.equals(id) && pages != null) {
            if (log.isDebugEnabled()) {
                log.debug("Resolving pageNumber: " + id);
            }
            resolved = true;
            int pageIndex = pageType ? 0 : pages.size() - 1;
            PageViewport page = pages.get(pageIndex);
            // replace the text
            removeText();
            text = page.getPageNumberString();
            addWord(text, 0, getBidiLevel());
            // update ipd
            if (font != null) {
                handleIPDVariation(font.getWordWidth(text) - getIPD());
                // set the Font object to null, as we don't need it any more
                font = null;
            } else {
                log.warn("Cannot update the IPD of an unresolved page number."
                        + " No font information available.");
            }
        }
    }

    /**
     * Check if this is resolved.
     *
     * @return true when this has been resolved
     */
    public boolean isResolved() {
       return resolved;
    }

    /**
     * recursively apply the variation factor to all descendant areas
     * @param variationFactor the variation factor that must be applied to adjustment ratios
     * @param lineStretch     the total stretch of the line
     * @param lineShrink      the total shrink of the line
     * @return true if there is an UnresolvedArea descendant
     */
    @Override
    public boolean applyVariationFactor(double variationFactor,
                                        int lineStretch, int lineShrink) {
        return true;
    }

    /**
     * Collection bidi inline runs.
     * Override of @{link InlineParent} implementation.
     *
     * N.B. [GA] without this override, the page-number-citation_writing_mode_rl
     * layout engine test will fail. It may be that the test needs to
     * be updated rather than using this override.
     * @param runs current list of inline runs
     * @return modified list of inline runs, having appended new run
     */
    @Override
    public List collectInlineRuns(List runs) {
        assert runs != null;
        runs.add(new InlineRun(this, new int[] {getBidiLevel()}));
        return runs;
    }
}
