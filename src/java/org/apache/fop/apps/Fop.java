/*
 *
 * Copyright 1999-2003 The Apache Software Foundation.
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
 * 
 * $Id$
 */

package org.apache.fop.apps;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.fop.configuration.Configuration;

public class Fop {

    public static Runtime runtime;
    public static long startTotal;
    public static long startFree;
    public static long startTime;
    public static long startPCi;
    public static long endPCi;
    
    /**
     * The top-level package for FOP
     */
    public static final String fopPackage = "org.apache.fop";
    
    public static final Logger logger = Logger.getLogger(fopPackage);
    static {
        logger.setLevel(Level.INFO);
    }
    
    public Configuration configuration = new Configuration();

    public static void main(String[] args) {

        long endtotal, endfree, gctotal, gcfree;
        Driver driver;
        Configuration configuration;
        FOPOptions options = null;
        Boolean bool = null;

        runtime = Runtime.getRuntime();
        startTotal = runtime.totalMemory();
        startFree = runtime.freeMemory();
        startTime = System.currentTimeMillis();

        try {
            configuration = new Configuration();
            options = new FOPOptions(configuration, args);
            driver = new Driver(args, configuration, options);
            driver.run();
            System.out.println("Back from driver.run()");
            System.out.println("Elapsed time: " +
                                (System.currentTimeMillis() - startTime));
            endtotal = runtime.totalMemory();
            endfree = runtime.freeMemory();
            System.gc();
            gctotal = runtime.totalMemory();
            gcfree = runtime.freeMemory();
            System.out.println("Total memory before run : " + startTotal);
            System.out.println("Total memory after run  : " + endtotal);
            System.out.println("Total memory after GC   : " + gctotal);
            System.out.println("Diff before/after total : "
                                                   + (endtotal - startTotal));
            System.out.println("Diff before/GC total    : "
                                                   + (gctotal - startTotal));
            System.out.println("Diff after/GC total     : "
                                                   + (gctotal - endtotal));
            System.out.println("Free memory before run  : " + startFree);
            System.out.println("Free memory after run   : " + endfree);
            System.out.println("Free memory after GC    : " + gcfree);
            System.out.println("Diff before/after free  : "
                                                   + (endfree - startFree));
            System.out.println("Diff before/GC free     : "
                                                   + (gcfree - startFree));
            System.out.println("Diff after/GC free      : "
                                                   + (gcfree - endfree));
            System.out.println("cg() freed              : "
                                                    + (gcfree - endfree));
            //System.out.println("PC time     : " + (endPCi - startPCi));
            
        } catch (FOPException e) {
            logger.warning(e.getMessage());
            if (options.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets the parser Class name.
     * 
     * @return a String with the value of the property
     * <code>org.xml.sax.parser</code> or the default value
     * <code>org.apache.xerces.parsers.SAXParser</code>.
     */
    public static final String getParserClassName() {
        String parserClassName = null;
        try {
            parserClassName = System.getProperty("org.xml.sax.parser");
        } catch (SecurityException se) {}

        if (parserClassName == null) {
            parserClassName = "org.apache.xerces.parsers.SAXParser";
        }
        return parserClassName;
    }

    private Fop() {
    }

}

