/*
 * Copyright 1999-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

package org.apache.fop.area;

import java.awt.geom.Rectangle2D;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.pagination.SimplePageMaster;

/**
 * Page viewport that specifies the viewport area and holds the page contents.
 * This is the top level object for a page and remains valid for the life
 * of the document and the area tree.
 * This object may be used as a key to reference a page.
 * This is the level that creates the page.
 * The page (reference area) is then rendered inside the page object
 */
public class PageViewport implements Resolvable, Cloneable {

    private Page page;
    private Rectangle2D viewArea;
    private boolean clip = false;
    private String pageNumberString = null;
    private SimplePageMaster spm = null;

    // list of id references and the rectangle on the page
    private Map idReferences = null;

    // this keeps a list of currently unresolved areas or extensions
    // once an idref is resolved it is removed
    // when this is empty the page can be rendered
    private HashMap unresolvedIDRefs = new HashMap();

    private Map pendingResolved = null;

    // hashmap of markers for this page
    // start and end are added by the fo that contains the markers
    private Map markerFirstStart = null;
    private Map markerLastStart = null;
    private Map markerFirstAny = null;
    private Map markerLastEnd = null;
    private Map markerLastAny = null;

    /**
     * logging instance
     */
    protected static Log log = LogFactory.getLog(PageViewport.class);

    /**
     * Create a page viewport.
     * @param p the page reference area that holds the contents
     * @param bounds the bounds of this viewport
     */
    public PageViewport(SimplePageMaster spm, Page p, Rectangle2D bounds) {
        page = p;
        this.spm = spm;
        viewArea = bounds;
    }

    /**
     * Convenience method to get BodyRegion of this PageViewport
     * @return BodyRegion object
     */
    public BodyRegion getBodyRegion() {
        return (BodyRegion) getPage().getRegionViewport(
                Constants.FO_REGION_BODY).getRegionReference();
    }    

    /**
     * Convenience method to create a new Span for this
     * this PageViewport.
     * 
     * @param spanAll whether this is a single-column span
     * @return Span object created
     */
    public Span createSpan(boolean spanAll) {
        return getBodyRegion().getMainReference().createSpan(spanAll);
    }    

    /**
     * Convenience method to get the span-reference-area currently
     * being processed
     * 
     * @return span currently being processed.
     */
    public Span getCurrentSpan() {
        return getBodyRegion().getMainReference().getCurrentSpan();
    }    

    /**
     * Set if this viewport should clip.
     * @param c true if this viewport should clip
     */
    public void setClip(boolean c) {
        clip = c;
    }

    /**
     * Get the view area rectangle of this viewport.
     * @return the rectangle for this viewport
     */
    public Rectangle2D getViewArea() {
        return viewArea;
    }

    /**
     * Get the page reference area with the contents.
     * @return the page reference area
     */
    public Page getPage() {
        return page;
    }

    /**
     * Set the page number for this page.
     * @param num the string representing the page number
     */
    public void setPageNumberString(String num) {
        pageNumberString = num;
    }

    /**
     * Get the page number of this page.
     * @return the string that represents this page
     */
    public String getPageNumberString() {
        return pageNumberString;
    }

    /**
     * Get the key for this page viewport.
     * This is used so that a serializable key can be used to
     * lookup the page or some other reference.
     *
     * @return a unique page viewport key for this area tree
     */
    public String getKey() {
        return toString();
    }

    /**
     * Add an idref to this page.
     * All idrefs found for child areas of this PageViewport are added
     * to unresolvedIDRefs, for subsequent resolution by AreaTreeHandler
     * calls to this object's resolveIDRef().
     *
     * @param id the idref
     * @param res the child element of this page that needs this
     *      idref resolved
     */
    public void addUnresolvedIDRef(String idref, Resolvable res) {
        if (unresolvedIDRefs == null) {
            unresolvedIDRefs = new HashMap();
        }
        List list = (List)unresolvedIDRefs.get(idref);
        if (list == null) {
            list = new ArrayList();
            unresolvedIDRefs.put(idref, list);
        }
        list.add(res);
    }

    /**
     * Check if this page has been fully resolved.
     * @return true if the page is resolved and can be rendered
     */
    public boolean isResolved() {
        return unresolvedIDRefs == null;
    }

    /**
     * Get the unresolved idrefs for this page.
     * @return String array of idref's that still have not been resolved
     */
    public String[] getIDRefs() {
        return (unresolvedIDRefs == null) ? null :
            (String[]) unresolvedIDRefs.keySet().toArray(new String[] {});
    }

    /**
     * @see org.apache.fop.area.Resolveable#resolveIDRef(String, List)
     */
    public void resolveIDRef(String id, List pages) {
        if (page == null) {
            if (pendingResolved == null) {
                pendingResolved = new HashMap();
            }
            pendingResolved.put(id, pages);
        } else {
            if (unresolvedIDRefs != null) {
                List todo = (List)unresolvedIDRefs.get(id);
                if (todo != null) {
                    for (int count = 0; count < todo.size(); count++) {
                        Resolvable res = (Resolvable)todo.get(count);
                        res.resolveIDRef(id, pages);
                    }
                }
            }
        }
        if (unresolvedIDRefs != null && pages != null) {
            unresolvedIDRefs.remove(id);
            if (unresolvedIDRefs.isEmpty()) {
                unresolvedIDRefs = null;
            }
        }
    }

    /**
     * Add the markers for this page.
     * Only the required markers are kept.
     * For "first-starting-within-page" it adds the markers
     * that are starting only if the marker class name is not
     * already added.
     * For "first-including-carryover" it adds any starting marker
     * if the marker class name is not already added.
     * For "last-starting-within-page" it adds all marks that
     * are starting, replacing earlier markers.
     * For "last-ending-within-page" it adds all markers that
     * are ending, replacing earlier markers.
     * 
     * Should this logic be placed in the Page layout manager.
     *
     * @param marks the map of markers to add
     * @param starting if the area being added is starting or ending
     * @param isfirst if the area being added has is-first trait
     * @param islast if the area being added has is-last trait
     */
    public void addMarkers(Map marks, boolean starting,
            boolean isfirst, boolean islast) {
        // at the start of the area, register is-first and any areas
        if (starting) {
            if (isfirst) {
                if (markerFirstStart == null) {
                    markerFirstStart = new HashMap();
                }
                if (markerFirstAny == null) {
                    markerFirstAny = new HashMap();
                }
                // first on page: only put in new values, leave current
                for (Iterator iter = marks.keySet().iterator(); iter.hasNext();) {
                    Object key = iter.next();
                    if (!markerFirstStart.containsKey(key)) {
                        markerFirstStart.put(key, marks.get(key));
                        log.trace("page " + pageNumberString + ": " + "Adding marker " + key + " to FirstStart");
                    }
                    if (!markerFirstAny.containsKey(key)) {
                        markerFirstAny.put(key, marks.get(key));
                        log.trace("page " + pageNumberString + ": " + "Adding marker " + key + " to FirstAny");
                    }
                }
                if (markerLastStart == null) {
                    markerLastStart = new HashMap();
                }
                // last on page: replace all
                markerLastStart.putAll(marks);
                log.trace("page " + pageNumberString + ": " + "Adding all markers to LastStart");
            } else {
                if (markerFirstAny == null) {
                    markerFirstAny = new HashMap();
                }
                // first on page: only put in new values, leave current
                for (Iterator iter = marks.keySet().iterator(); iter.hasNext();) {
                    Object key = iter.next();
                    if (!markerFirstAny.containsKey(key)) {
                        markerFirstAny.put(key, marks.get(key));
                        log.trace("page " + pageNumberString + ": " + "Adding marker " + key + " to FirstAny");
                    }
                }
            }
        }
        // at the end of the area, register is-last and any areas
        else {
            if (islast) {
                if (markerLastEnd == null) {
                    markerLastEnd = new HashMap();
                }
                // last on page: replace all
                markerLastEnd.putAll(marks);
                log.trace("page " + pageNumberString + ": " + "Adding all markers to LastEnd");
            }
            if (markerLastAny == null) {
                markerLastAny = new HashMap();
            }
            // last on page: replace all
            markerLastAny.putAll(marks);
            log.trace("page " + pageNumberString + ": " + "Adding all markers to LastAny");
        }
    }

    /**
     * Get a marker from this page.
     * This will retrieve a marker with the class name
     * and position.
     *
     * @param name The class name of the marker to retrieve 
     * @param pos the position to retrieve
     * @return Object the marker found or null
     */
    public Object getMarker(String name, int pos) {
        Object mark = null;
        String posName = null;
        switch (pos) {
            case Constants.EN_FSWP:
                if (markerFirstStart != null) {
                    mark = markerFirstStart.get(name);
                    posName = "FSWP";
                }
                if (mark == null && markerFirstAny != null) {
                    mark = markerFirstAny.get(name);
                    posName = "FirstAny after " + posName;
                }
            break;
            case Constants.EN_FIC:
                if (markerFirstAny != null) {
                    mark = markerFirstAny.get(name);
                    posName = "FIC";
                }
            break;
            case Constants.EN_LSWP:
                if (markerLastStart != null) {
                    mark = markerLastStart.get(name);
                    posName = "LSWP";
                }
                if (mark == null && markerLastAny != null) {
                    mark = markerLastAny.get(name);
                    posName = "LastAny after " + posName;
                }
            break;
            case Constants.EN_LEWP:
                if (markerLastEnd != null) {
                    mark = markerLastEnd.get(name);
                    posName = "LEWP";
                }
                if (mark == null && markerLastAny != null) {
                    mark = markerLastAny.get(name);
                    posName = "LastAny after " + posName;
                }
            break;
        }
        log.trace("page " + pageNumberString + ": " + "Retrieving marker " + name + "at position " + posName); 
        return mark;    
    }

    /**
     * Save the page contents to an object stream.
     * The map of unresolved references are set on the page so that
     * the resolvers can be properly serialized and reloaded.
     * @param out the object output stream to write the contents
     * @throws Exception if there is a problem saving the page
     */
    public void savePage(ObjectOutputStream out) throws Exception {
        // set the unresolved references so they are serialized
        page.setUnresolvedReferences(unresolvedIDRefs);
        out.writeObject(page);
        page = null;
    }

    /**
     * Load the page contents from an object stream.
     * This loads the page contents from the stream and
     * if there are any unresolved references that were resolved
     * while saved they will be resolved on the page contents.
     * @param in the object input stream to read the page from
     * @throws Exception if there is an error loading the page
     */
    public void loadPage(ObjectInputStream in) throws Exception {
        page = (Page) in.readObject();
        unresolvedIDRefs = page.getUnresolvedReferences();
        if (unresolvedIDRefs != null && pendingResolved != null) {
            for (Iterator iter = pendingResolved.keySet().iterator();
                         iter.hasNext();) {
                String id = (String) iter.next();
                resolveIDRef(id, (List)pendingResolved.get(id));
            }
            pendingResolved = null;
        }
    }

    /**
     * Clone this page.
     * Used by the page master to create a copy of an original page.
     * @return a copy of this page and associated viewports
     */
    public Object clone() {
        Page p = (Page)page.clone();
        PageViewport ret = new PageViewport(spm, p, (Rectangle2D)viewArea.clone());
        return ret;
    }

    /**
     * Clear the page contents to save memory.
     * This object is kept for the life of the area tree since
     * it holds id and marker information and is used as a key.
     */
    public void clear() {
        page = null;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(64);
        sb.append("PageViewport: page=");
        sb.append(getPageNumberString());
        return sb.toString();
    }
    /**
     * @return Returns the spm.
     */
    public SimplePageMaster getSPM() {
        return spm;
    }
}