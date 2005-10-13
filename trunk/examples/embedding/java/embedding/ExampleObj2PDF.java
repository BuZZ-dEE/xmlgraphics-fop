/*
 * Copyright 1999-2004 The Apache Software Foundation.
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
 
package embedding;

// Java
import java.io.File;
import java.io.OutputStream;
import java.io.IOException;

// JAXP
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXResult;

// FOP
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FOPException;
import embedding.model.ProjectTeam;

/**
 * This class demonstrates the conversion of an arbitrary object file to a 
 * PDF using JAXP (XSLT) and FOP (XSL:FO).
 */
public class ExampleObj2PDF {

    /**
     * Converts a ProjectTeam object to a PDF file.
     * @param team the ProjectTeam object
     * @param xslt the stylesheet file
     * @param pdf the target PDF file
     * @throws IOException In case of an I/O problem
     * @throws FOPException In case of a FOP problem
     * @throws TransformerException In case of a XSL transformation problem
     */
    public void convertProjectTeam2PDF(ProjectTeam team, File xslt, File pdf) 
                throws IOException, FOPException, TransformerException {
                    
        // Construct fop with desired output format
        Fop fop = new Fop(Fop.RENDER_PDF);

        // Setup output
        OutputStream out = new java.io.FileOutputStream(pdf);
        out = new java.io.BufferedOutputStream(out);
        try {
            fop.setOutputStream(out);

            // Setup XSLT
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(xslt));
        
            // Setup input for XSLT transformation
            Source src = team.getSourceForProjectTeam();
        
            // Resulting SAX events (the generated FO) must be piped through to FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);
        } finally {
            out.close();
        }
    }


    /**
     * Main method.
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            System.out.println("FOP ExampleObj2PDF\n");
            System.out.println("Preparing...");
            
            // Setup directories
            File baseDir = new File(".");
            File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output
            File xsltfile = new File(baseDir, "xml/xslt/projectteam2fo.xsl");
            File pdffile = new File(outDir, "ResultObj2PDF.pdf");

            System.out.println("Input: a ProjectTeam object");
            System.out.println("Stylesheet: " + xsltfile);
            System.out.println("Output: PDF (" + pdffile + ")");
            System.out.println();
            System.out.println("Transforming...");

            ExampleObj2PDF app = new ExampleObj2PDF();
            app.convertProjectTeam2PDF(ExampleObj2XML.createSampleProjectTeam(), xsltfile, pdffile);
            
            System.out.println("Success!");
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}
