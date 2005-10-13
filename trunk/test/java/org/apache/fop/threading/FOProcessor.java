/*
 * Copyright 2004 The Apache Software Foundation.
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

package org.apache.fop.threading;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Templates;

public interface FOProcessor {

    String ROLE = FOProcessor.class.getName();

    void process(InputStream in, Templates templates, OutputStream out)
            throws org.apache.fop.apps.FOPException, java.io.IOException;

}