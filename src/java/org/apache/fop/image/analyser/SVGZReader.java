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

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.image.FopImage;

/**
 * Implements a reader for gzipped XMLFiles.
 * 
 * <p>
 * The current implementation is limited to SVG files only.
 */
public class SVGZReader extends XMLReader {
    /**
     * Default constructor.
     */
    public SVGZReader() {
    }

    /** {@inheritDoc} */
    protected FopImage.ImageInfo loadImage(final String uri,
            final InputStream bis, final FOUserAgent ua) {
        try {
            return new SVGReader().verifySignature(uri,
                    new GZIPInputStream(bis), ua);
        } catch (final IOException e) {
            // ignore
        }
        return null;
    }
}
