/*
 * $Id$
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 * 
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 * 
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */ 
package org.apache.fop.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

/**
 * This class represents a list of PDF filters to be applied when serializing 
 * the output of a PDF object.
 */
public class PDFFilterList {

    /** Key for the default filter */
    public static final String DEFAULT_FILTER = "default";
    /** Key for the filter used for normal content*/
    public static final String CONTENT_FILTER = "content";
    /** Key for the filter used for images */
    public static final String IMAGE_FILTER = "image";
    /** Key for the filter used for JPEG images */
    public static final String JPEG_FILTER = "jpeg";
    /** Key for the filter used for fonts */
    public static final String FONT_FILTER = "font";

    private List filters = new java.util.ArrayList();

    private boolean ignoreASCIIFilters = false;
    
    /**
     * Default constructor.
     * <p>
     * The flag for ignoring ASCII filters defaults to false.
     */
    public PDFFilterList() {
        //nop
    }
    
    /**
     * Use this descriptor if you want to have ASCII filters (such as ASCIIHex
     * and ASCII85) ignored, for example, when encryption is active.
     * @param ignoreASCIIFilters true if ASCII filters should be ignored
     */
    public PDFFilterList(boolean ignoreASCIIFilters) {
        this.ignoreASCIIFilters = ignoreASCIIFilters;
    }

    /**
     * Indicates whether the filter list is already initialized.
     * @return true if more there are filters present
     */
    public boolean isInitialized() {
        return this.filters.size() > 0;
    }

    /**
     * Add a filter for compression of the stream. Filters are
     * applied in the order they are added. This should always be a
     * new instance of the particular filter of choice. The applied
     * flag in the filter is marked true after it has been applied to the
     * data.
     * @param filter filter to add
     */
    public void addFilter(PDFFilter filter) {
        if (filter != null) {
            if (this.ignoreASCIIFilters && filter.isASCIIFilter()) {
                return; //ignore ASCII filter
            }
            filters.add(filter);
        }
    }
    
    /**
     * Add a filter for compression of the stream by name.
     * @param filterType name of the filter to add
     */
    public void addFilter(String filterType) {
        if (filterType == null) {
            return;
        }
        if (filterType.equals("flate")) {
            addFilter(new FlateFilter());
        } else if (filterType.equals("ascii-85")) {
            if (this.ignoreASCIIFilters) {
                return; //ignore ASCII filter
            }
            addFilter(new ASCII85Filter());
        } else if (filterType.equals("ascii-hex")) {
            if (this.ignoreASCIIFilters) {
                return; //ignore ASCII filter
            }
            addFilter(new ASCIIHexFilter());
        } else if (filterType.equals("")) {
            return;
        } else {
            throw new IllegalArgumentException(
                "Unsupported filter type in stream-filter-list: " + filterType);
        }
    }

    /**
     * Checks the filter list for the DCT filter and adds it in the correct
     * place if necessary.
     */
    public void ensureDCTFilterInPlace() {
        if (this.filters.size() == 0) {
            addFilter(new DCTFilter());
        } else {
            if (!(this.filters.get(0) instanceof DCTFilter)) {
                this.filters.add(0, new DCTFilter());
            }
        }
    }

    /**
     * Adds the default filters to this stream.
     * @param filters Map of filters
     * @param type which filter list to modify
     */
    public void addDefaultFilters(Map filters, String type) {
        List filterset = null;
        if (filters != null) {
            filterset = (List)filters.get(type);
            if (filterset == null) {
                filterset = (List)filters.get(DEFAULT_FILTER);
            }
        }
        if (filterset == null || filterset.size() == 0) {
            // built-in default to flate
            //addFilter(new FlateFilter());
        } else {
            for (int i = 0; i < filterset.size(); i++) {
                String v = (String)filterset.get(i);
                addFilter(v);
            }
        }
    }

    /**
     * Apply the filters to the data
     * in the order given and return the /Filter and /DecodeParms
     * entries for the stream dictionary. If the filters have already
     * been applied to the data (either externally, or internally)
     * then the dictionary entries are built and returned.
     * @return a String representing the filter list
     */
    protected String buildFilterDictEntries() {
        if (filters != null && filters.size() > 0) {
            List names = new java.util.ArrayList();
            List parms = new java.util.ArrayList();

            // run the filters
            for (int count = 0; count < filters.size(); count++) {
                PDFFilter filter = (PDFFilter)filters.get(count);
                // place the names in our local vector in reverse order
                names.add(0, filter.getName());
                parms.add(0, filter.getDecodeParms());
            }

            // now build up the filter entries for the dictionary
            return buildFilterEntries(names) + buildDecodeParms(parms);
        }
        return "";

    }

    private String buildFilterEntries(List names) {
        boolean needFilterEntry = false;
        StringBuffer sb = new StringBuffer(64);
        sb.append("/Filter [ ");
        for (int i = 0; i < names.size(); i++) {
            final String name = (String)names.get(i);
            if (name.length() > 0) {
                needFilterEntry = true;
                sb.append(name);
                sb.append(" ");
            }
        }
        if (needFilterEntry) {
            sb.append("]");
            return sb.toString();
        } else {
            return "";
        }
    }

    private String buildDecodeParms(List parms) {
        StringBuffer sb = new StringBuffer();
        boolean needParmsEntry = false;
        sb.append("/DecodeParms ");

        if (parms.size() > 1) {
            sb.append("[ ");
        }
        for (int count = 0; count < parms.size(); count++) {
            String s = (String)parms.get(count);
            if (s != null) {
                sb.append(s);
                needParmsEntry = true;
            } else {
                sb.append("null");
            }
            sb.append(" ");
        }
        if (parms.size() > 1) {
            sb.append("]");
        }
        if (needParmsEntry) {
            return sb.toString();
        } else {
            return "";
        }
    }

    
    /**
     * Applies all registered filters as necessary. The method returns an 
     * OutputStream which will receive the filtered contents.
     * @param stream raw data output stream
     * @return OutputStream filtered output stream
     * @throws IOException In case of an I/O problem
     */
    public OutputStream applyFilters(OutputStream stream) throws IOException {
        OutputStream out = stream;
        if (filters != null) {
            for (int count = filters.size() - 1; count >= 0; count--) {
                PDFFilter filter = (PDFFilter)filters.get(count);
                out = filter.applyFilter(out);
            }
        }
        return out;
    }

    /**
     * Builds a filter map from an Avalon Configuration object.
     * @param cfg the Configuration object
     * @return Map the newly built filter map
     * @throws ConfigurationException if a filter list is defined twice
     */
    public static Map buildFilterMapFromConfiguration(Configuration cfg) 
                throws ConfigurationException {
        Map filterMap = new java.util.HashMap();
        Configuration[] filterLists = cfg.getChildren("filterList");
        for (int i = 0; i < filterLists.length; i++) {
            Configuration filters = filterLists[i];
            String type = filters.getAttribute("type", null);
            Configuration[] filt = filters.getChildren("value");
            List filterList = new java.util.ArrayList();
            for (int j = 0; j < filt.length; j++) {
                String name = filt[j].getValue();
                filterList.add(name);
            }
            
            if (type == null) {
                type = PDFFilterList.DEFAULT_FILTER;
            }
            if (filterMap.get(type) != null) {
                throw new ConfigurationException("A filterList of type '" 
                    + type + "' has already been defined");
            }
            filterMap.put(type, filterList);
        }
        return filterMap;                
    }

}
