package org.apache.xml.fop.apps;

// SAX
import org.xml.sax.Parser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

// Java
import java.io.FileReader;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;

/**
 * mainline class.
 *
 * Gets input and output filenames from the command line.
 * Creates a SAX Parser (defaulting to XP).
 * 
 */
public class CommandLine {

    /**
     * creates a SAX parser, using the value of org.xml.sax.parser
     * defaulting to com.jclark.xml.sax.Driver 
     *
     * @return the created SAX parser
     */
    static Parser createParser() {
	String parserClassName =
	    System.getProperty("org.xml.sax.parser");
	if (parserClassName == null) {
	    parserClassName = "com.jclark.xml.sax.Driver";
	}
	System.err.println("using SAX parser " + parserClassName);

	try {
	    return (Parser)
		Class.forName(parserClassName).newInstance();
	} catch (ClassNotFoundException e) {
	    System.err.println("Could not find " + parserClassName);
	} catch (InstantiationException e) {
	    System.err.println("Could not instantiate "
			       + parserClassName);
	} catch (IllegalAccessException e) {
	    System.err.println("Could not access " + parserClassName);
	} catch (ClassCastException e) {
	    System.err.println(parserClassName + " is not a SAX driver"); 
	}
	return null;
    }

    /**
     * create an InputSource from a file name
     *
     * @param filename the name of the file
     * @return the InputSource created
     */
    protected static InputSource fileInputSource(String filename) {
	
	/* this code adapted from James Clark's in XT */
	File file = new File(filename);
	String path = file.getAbsolutePath();
	String fSep = System.getProperty("file.separator");
	if (fSep != null && fSep.length() == 1)
	    path = path.replace(fSep.charAt(0), '/');
	if (path.length() > 0 && path.charAt(0) != '/')
	    path = '/' + path;
	try {
	    return new InputSource(new URL("file", null,
					   path).toString());
	}
	catch (java.net.MalformedURLException e) {
	    throw new Error("unexpected MalformedURLException");
	}
    }

    /**
     * mainline method
     *
     * first command line argument is input file
     * second command line argument is output file
     *
     * @param command line arguments
     */
    public static void main(String[] args) {
	String version = Version.getVersion();
	System.err.println(version);
		
	if (args.length != 2) {
	    System.err.println("usage: java "
			       + "org.apache.xml.fop.apps.CommandLine "
			       + "formatting-object-file pdf-file");
	    System.exit(1);
	}
		
	Parser parser = createParser();
		
	if (parser == null) {
	    System.err.println("ERROR: Unable to create SAX parser");
	    System.exit(1);
	}
	
	try {
	    Driver driver = new Driver();
	    driver.setRenderer("org.apache.xml.fop.render.pdf.PDFRenderer", version);
	    driver.addElementMapping("org.apache.xml.fop.fo.StandardElementMapping");
	    driver.addElementMapping("org.apache.xml.fop.svg.SVGElementMapping");
	    driver.setWriter(new PrintWriter(new FileWriter(args[1])));
	    driver.buildFOTree(parser, fileInputSource(args[0]));
	    driver.format();
	    driver.render();
	} catch (Exception e) {
	    System.err.println("FATAL ERROR: " + e.getMessage());
	    System.exit(1);
	}
    }
}
