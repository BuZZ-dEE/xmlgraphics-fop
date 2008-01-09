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

/* $Id: $ */

package org.apache.fop.render.afp.modca.goca;

import org.apache.fop.render.afp.tools.BinaryUtils;

/**
 * Sets the current character set (font) to be used for following graphics strings 
 */
public class GraphicsSetCharacterSet extends AbstractPreparedAFPObject {
    /** font character set reference */
    private int fontReference;

    /**
     * @param fontReference character set font reference
     */
    public GraphicsSetCharacterSet(int fontReference) {
        this.fontReference = fontReference;
        prepareData();
    }

    /**
     * {@inheritDoc}
     */
    protected void prepareData() {
        super.data = new byte[] {
            0x38, // GSCS order code
            BinaryUtils.convert(fontReference)[0]
        };
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "GraphicsSetCharacterSet(" + fontReference + ")";
    }
}