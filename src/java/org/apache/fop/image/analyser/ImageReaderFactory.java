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
 
package org.apache.fop.image.analyser;

// Java
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.image.FopImage;
import org.apache.xmlgraphics.util.Service;

/**
 * Factory for ImageReader objects.
 *
 * @author    Pankaj Narula
 * @version   $Id$
 */
public class ImageReaderFactory {

    private static List formats = new java.util.ArrayList();

    /** logger */
    protected static Log log = LogFactory.getLog(ImageReaderFactory.class);

    static {
        registerFormat(new JPEGReader());
        registerFormat(new BMPReader());
        registerFormat(new GIFReader());
        registerFormat(new PNGReader());
        registerFormat(new TIFFReader());
        registerFormat(new EPSReader());
        registerFormat(new EMFReader());

        //Dynamic registration of ImageReaders
        Iterator iter = Service.providers(ImageReader.class, true);
        while (iter.hasNext()) {
            registerFormat((ImageReader)iter.next());
        }
        
        // the xml parser through batik closes the stream when finished
        // so there is a workaround in the SVGReader
        registerFormat(new SVGReader());
        registerFormat(new SVGZReader());
        registerFormat(new XMLReader());
    }

    /**
     * Registers a new ImageReader.
     *
     * @param reader  An ImageReader instance
     */
    public static void registerFormat(ImageReader reader) {
        formats.add(reader);
    }

    /**
     * ImageReader maker.
     *
     * @param uri  URI to the image
     * @param in   image input stream
     * @param ua   user agent
     * @return     An ImageInfo object describing the image
     */
    public static FopImage.ImageInfo make(String uri, InputStream in,
            FOUserAgent ua) {

        ImageReader reader;
        try {
            for (int count = 0; count < formats.size(); count++) {
                reader = (ImageReader) formats.get(count);
                FopImage.ImageInfo info = reader.verifySignature(uri, in, ua);
                if (info != null) {
                    return info;
                }
            }
            log.warn("No ImageReader found for " + uri);
            in.close();
        } catch (IOException ex) {
            log.error("Error while recovering Image Informations ("
                + uri + ")", ex);
        }
        return null;
    }

}

