/*
 *
 * Copyright 1999-2004 The Apache Software Foundation.
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
 * $Id$
 */

package org.apache.fop.configuration;

// sax
import org.xml.sax.InputSource;

// java
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
// fop
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.fop.apps.Driver;
import org.apache.fop.apps.FOFileHandler;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.InputHandler;
import org.apache.fop.apps.XSLTInputHandler;

/**
 * FOPOptions handles loading of configuration files and
 * additional setting of commandline options
 */
public class FOPOptions {

    /** input / output not set */
    public static final int NOT_SET = 0;
    /** input: fo file */
    public static final int FO_INPUT = 1;
    /** input: xml+xsl file */
    public static final int XSLT_INPUT = 2;
    /** output: pdf file */
    public static final int PDF_OUTPUT = 1;
    /** output: screen using swing */
    public static final int AWT_OUTPUT = 2;
    /** output: mif file */
    public static final int MIF_OUTPUT = 3;
    /** output: sent swing rendered file to printer */
    public static final int PRINT_OUTPUT = 4;
    /** output: pcl file */
    public static final int PCL_OUTPUT = 5;
    /** output: postscript file */
    public static final int PS_OUTPUT = 6;
    /** output: text file */
    public static final int TXT_OUTPUT = 7;
    /** output: svg file */
    public static final int SVG_OUTPUT = 8;
    /** output: XML area tree */
    public static final int AREA_OUTPUT = 9;
    /** output: RTF file */
    public static final int RTF_OUTPUT = 10;
    
    private static final int LAST_INPUT_MODE = XSLT_INPUT;
    private static final int LAST_OUTPUT_MODE = RTF_OUTPUT;

    private Configuration configuration = null;

    /* show configuration information */
    private boolean dumpConfig = false;
    /* name of user configuration file */
    private File userConfigFile = null;
    /* name of input fo file */
    private File foFile = null;
    /* name of xsltFile (xslt transformation as input) */
    private File xsltFile = null;
    /* name of xml file (xslt transformation as input) */
    private File xmlFile = null;
    /* name of output file */
    private File outputFile = null;
    /* name of buffer file */
    private File bufferFile = null;
    /* input mode */
    private int inputmode = NOT_SET;
    /* output mode */
    private int outputmode = NOT_SET;
    /* buffer mode */
    private int buffermode = NOT_SET;
    /* language for user information */
    // baseDir (set from the config files
    private String baseDir = null;

    private java.util.HashMap rendererOptions;

    private Logger log = Logger.getLogger(Fop.fopPackage);

    private Vector xsltParams = null;
    
    private Options options = new Options();

    private static final String defaultConfigFile = "config.xml";
    private static final String defaultUserConfigFile = "userconfig.xml";
    
    /**
     * An array of String indexed by the integer constants representing
     * the various input modes.  Provided so that integer modes can be
     * mapped to a more descriptive string, and vice versa.
     */
    private String[] inputModes;
    /**
     * An array of String indexed by the integer constants representing
     * the various output modes.  Provided so that integer modes can be
     * mapped to a more descriptive string, and vice versa.
     */
    private String[] outputModes;


    /**
     * 
     */
    public FOPOptions(Configuration configuration) {
        setup();
        this.configuration = configuration;
        try {
            configure();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (FOPException e) {
            throw new RuntimeException(e);
        }
    }
    
    public FOPOptions(Configuration configuration, String[] args) {
        setup();
        this.configuration = configuration;
        try {
            configure(args);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (FOPException e) {
            throw new RuntimeException(e);
        }
    }

    private void setup() {
        inputModes = new String[LAST_INPUT_MODE + 1];
        inputModes[NOT_SET] = "NotSet";
        inputModes[FO_INPUT] = "fo";
        inputModes[XSLT_INPUT] = "xslt";
        
        outputModes = new String[LAST_OUTPUT_MODE + 1];
        outputModes[NOT_SET] = "NotSet";
        outputModes[PDF_OUTPUT] = "pdf";
        outputModes[PS_OUTPUT] = "ps";
        outputModes[PCL_OUTPUT] = "pcl";
        outputModes[PRINT_OUTPUT] = "print";
        outputModes[AWT_OUTPUT] = "awt";
        outputModes[MIF_OUTPUT] = "mif";
        outputModes[RTF_OUTPUT] = "rtf";
        outputModes[SVG_OUTPUT] = "svg";
        outputModes[TXT_OUTPUT] = "txt";
        outputModes[AREA_OUTPUT] = "at";
    }

    /**
     * @param mode the mode whose index in the array inputModes is to be
     * returned.
     * @return the int index of the mode string in the array, or -1 if the
     * mode string is not found in the array
     */
    public int inputModeIndex(String mode)
                throws FOPException {
        for (int i = 0; i <= LAST_INPUT_MODE; i++) {
            if (inputModes[i] != null)
                if (mode.equals(inputModes[i]))
                    return i;
        }
        throw new FOPException("Input mode " + mode + " not known");
    }

    /**
     * @param mode the mode whose index in the array outputModes is to be
     * returned.
     * @return the int index of the mode string in the array, or -1 if the
     * mode string is not found in the array
     */
    public int outputModeIndex(String mode)
                throws FOPException {
        for (int i = 0; i <= LAST_INPUT_MODE; i++) {
            if (outputModes[i] != null)
                if (mode.equals(outputModes[i]))
                    return i;
        }
        throw new FOPException("Output mode " + mode + " not known");
    }

    public void configure()
        throws FOPException, FileNotFoundException {
        configure(new HashMap(0));
    }
    /**
     * Configure the system according to the system configuration file
     * config.xml and the user configuration file if it is specified in the
     * system configuration file.
     */
    public void configure(HashMap cliArgs)
    throws FOPException, FileNotFoundException {
        loadConfigFiles(cliArgs);
        loadArguments(cliArgs);
        initOptions();
        try {
            checkSettings();
        } catch (java.io.FileNotFoundException e) {
            printUsage();
            throw e;
        }
    }
    
    public void configure(String[] args)
    throws FOPException, FileNotFoundException {
        configure(parseOptions(args));
    }
    
    /**
     * Method to map an inputMode name to an inputmode index.
     * @param name a String containing the name of an input mode
     * @return the index of that name in the array of input mode names,
     * or -1 if not found
     */
    public int inputModeNameToIndex(String name) {
        for (int i = 0; i <= LAST_INPUT_MODE; i++) {
            if (name.equals(inputModes[i])) return i;
        }
        return -1;
    }
    
    /**
     * Method to map an outputMode name to an outputmode index.
     * @param name a String containing the name of an output mode
     * @return the index of that name in the array of output mode names,
     * or -1 if not found
     */
    public int outputModeNameToIndex(String name) {
        for (int i = 0; i <= LAST_INPUT_MODE; i++) {
            if (name.equals(outputModes[i])) return i;
        }
        return -1;
    }
    
    /**
     * <code>parseOptions()</code> parses the command line into a
     * <code>HashMap</code> which is
     * passed to this method.  All key-Object pairs are installed in the
     * <code>Configuration</code> maps.
     */
    void loadArguments(HashMap arguments) {
        String key = null;
        if (arguments != null) {
            Set keys = arguments.keySet();
            Iterator iter = keys.iterator();
            while (iter.hasNext()) {
                key = (String)iter.next();
                configuration.put(key, arguments.get(key));
            }
        }
    }
    
    
    /**
     * Finish initialization of options.  The command line options, if
     * present, have been parsed and stored in the HashMap arguments.
     * The ints inputmode and outputmode will have been set as a side-
     * effect of command line parsing.
     *
     * The standard configuration file has been read and its contents
     * stored in the Configuration HashMaps.  If a user configuration file
     * was specified in the command line arguments, or, failing that, in
     * the standard configuration file, it had been read and its contents
     * have overridden the Configuration maps.
     *
     * It remains for any related variables defined in this class to be set.
     *
     * @exception FOPException
     */
    void initOptions() throws FOPException {
        String str = null;
        
        // show configuration settings
        dumpConfig = configuration.isTrue("dumpConfiguration");
        
        if ((str = getFoFileName()) != null)
            foFile = new File(str);
        if ((str = getXmlFileName()) != null)
            xmlFile = new File(str);
        if ((str = getXsltFileName()) != null)
            xsltFile = new File(str);
        if ((str = getOutputFileName()) != null)
            outputFile = new File(str);
        if ((str = getBufferFileName()) != null)
            bufferFile = new File(str);
        // userConfigFile may be set in the process of loading said file
        if (userConfigFile == null && (str = getUserConfigFileName()) != null)
            userConfigFile = new File(str);
        
        if ((str = getInputMode()) != null)
            inputmode = inputModeIndex(str);
        if ((str = getOutputMode()) != null)
            outputmode = outputModeIndex(str);
        
        // set base directory
        // This is not set directly from the command line, but may be set
        // indirectly from the input file setting if not set in the standard
        // or user configuration files
        baseDir = configuration.getStringValue("baseDir");
        if (baseDir == null) {
            try {
                baseDir = new File(getInputFile().getAbsolutePath())
                .getParentFile().toURL().toExternalForm();
                configuration.put("baseDir", baseDir);
            } catch (Exception e) {}
        }
        if (isDebugMode()) {
            log.config("base directory: " + baseDir);
        }
        
        if (dumpConfig) {
            configuration.dumpConfiguration();
            System.exit(0);
        }
        
        // quiet mode - this is the last setting, so there is no way to
        // supress the logging of messages during options processing
        if (configuration.isTrue("quiet")) {
            log.setLevel(Level.OFF);
        }
        
    }
    
    /**
     * Load the standard configuration file and the user-defined configuration
     * file if one has been defined.  The definition can occur in either the
     * standard file or as a command line argument.
     * @exception FOPException
     */
    private void loadConfigFiles(HashMap arguments) throws FOPException {
        String str = null;
        loadConfiguration(defaultConfigFile);
        // load user configuration file,if there is one
        // Has the userConfigFile been set from the command line?
        if (arguments != null) {
            if ((str = (String)arguments.get("userConfigFileName")) != null) {
                configuration.put("userConfigFileName", str);
                log.config("Using user configuration file " + str);
            }
        }
        if ((str = configuration.getStringValue("userConfigFileName"))
        != null) {  // No
            log.config("Loading user configuration file " + str);
            loadUserConfiguration(str);
        }
    }
    
    /**
     * Loads configuration file from a system standard place.
     * The context class loader and the <code>ConfigurationReader</code>
     * class loader are asked in turn to <code>getResourceAsStream</code>
     * on <i>fname</i> from a directory called <i>conf</i>.
     * @param fname the name of the configuration file to load.
     * @exception FOPException if the configuration file
     * cannot be discovered.
     */
    public void loadConfiguration(String fname)
    throws FOPException {
        InputStream configfile = ConfigurationResource.getResourceFile(
                "conf/" + fname, ConfigurationReader.class);
        
        if (isDebugMode()) {
            log.config(
                    "reading configuration file " + fname);
        }
        ConfigurationReader reader = new ConfigurationReader(
                new InputSource(configfile), configuration);
    }
    
    
    /**
     * Load a user-defined configuration file.
     * An initial attempt is made to use a File generated from
     * <code>userConfigFileName</code> as the configuration reader file input
     * source.  If this fails, an attempt is made to load the file using
     * <code>loadConfiguration</code>.
     * @param userConfigFileName the name of the user configuration file.
     */
    public void loadUserConfiguration(String userConfigFileName) {
        // read user configuration file
        boolean readOk = true;
        userConfigFile = new File(userConfigFileName);
        if (userConfigFile == null) {
            return;
        }
        log.config(
                "reading user configuration file " + userConfigFileName);
        try {
            ConfigurationReader reader = new ConfigurationReader(
                    InputHandler.fileInputSource(userConfigFile),
                    configuration);
        } catch (FOPException ex) {
            log.warning("Can't find user configuration file "
                    + userConfigFile + " in user locations");
            if (isDebugMode()) {
                ex.printStackTrace();
            }
            readOk = false;
        }
        if (! readOk) {
            try {
                // Try reading the file using loadConfig()
                loadConfiguration(userConfigFileName);
            } catch (FOPException ex) {
                log.warning("Can't find user configuration file "
                        + userConfigFile + " in system locations");
                if (isDebugMode()) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Get the log.
     * @return the log
     */
    public Logger getLogger() {
        return log;
    }
    
    private static final boolean TAKES_ARG = true;
    private static final boolean NO_ARG = false;
    private Options makeOptions() {
        // Create the Options object that will be returned
        Options options = new Options();
        // The mutually exclusive verbosity group includes the -d and -q flags
        OptionGroup verbosity = new OptionGroup();
        OptionBuilder.withArgName("debug mode");
        OptionBuilder.withLongOpt("full-error-dump");
        OptionBuilder.withDescription("Debug mode: verbose reporting");
        verbosity.addOption(
                OptionBuilder.create("d"));
        OptionBuilder.withArgName("quiet mode");
        OptionBuilder.withLongOpt("quiet");
        OptionBuilder.withDescription("Quiet mode: report errors only");
        verbosity.addOption(
                OptionBuilder.create("q"));
        verbosity.setRequired(false);
        // Add verbosity to options
        options.addOptionGroup(verbosity);
        // Add the dump-config option directly
        OptionBuilder.withArgName("dump config");
        OptionBuilder.withLongOpt("dump-config");
        OptionBuilder.withDescription("Dump configuration settings");
        options.addOption(
                OptionBuilder.create("x"));
        // Add the config-file option directly
        OptionBuilder.withArgName("config file");
        OptionBuilder.withLongOpt("config-file");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Configuration file");
        options.addOption(
                OptionBuilder.create("c"));
        // Add the language option directly
        OptionBuilder.withArgName("language");
        OptionBuilder.withLongOpt("language");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("ISO639 language code");
        options.addOption(
                OptionBuilder.create("l"));
        // Create the mutually exclusive input group
        OptionGroup input = new OptionGroup();
        OptionBuilder.withArgName("fo:file");
        OptionBuilder.withLongOpt("fo");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("XSL-FO input file");
        input.addOption(
                OptionBuilder.create("fo"));
        OptionBuilder.withArgName("xml file");
        OptionBuilder.withLongOpt("xml");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("XML source file for generating XSL-FO input");
        input.addOption(
                OptionBuilder.create("xml"));
        // Add the input group to the options
        options.addOptionGroup(input);
        // The xsl option depends on the xml input option.  There is no
        // simple way to express this relationship
        OptionBuilder.withArgName("xsl stylesheet");
        OptionBuilder.withLongOpt("xsl");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("XSL stylesheet for transforming XML to XSL-FO");
        options.addOption(
                OptionBuilder.create("xsl"));
        // Work-around for the xsl parameters
        // Allow multiple arguments (does this apply to multiple instances
        // of the argument specifier?) of the form <name=value>, using '='
        // as a value separator
        OptionBuilder.withArgName("name=value");
        OptionBuilder.withValueSeparator();
        OptionBuilder.withLongOpt("xsl-param");
        OptionBuilder.hasArgs(Option.UNLIMITED_VALUES);
        OptionBuilder.withDescription("Parameter to XSL stylesheet");
        options.addOption(
                OptionBuilder.create("param"));
        
        // Create the mutually exclusive output group
        OptionGroup output = new OptionGroup();
        OptionBuilder.withArgName("screen renderer");
        OptionBuilder.withLongOpt("awt");
        OptionBuilder.withDescription("Input will be renderered to display");
        output.addOption(
                OptionBuilder.create("awt"));
        OptionBuilder.withArgName("pdf output file");
        OptionBuilder.withLongOpt("pdf");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Input will be rendered as PDF to named file");
        output.addOption(
                OptionBuilder.create("pdf"));
        OptionBuilder.withArgName("postscript output file");
        OptionBuilder.withLongOpt("ps");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Input will be rendered as Postscript to named file");
        output.addOption(
                OptionBuilder.create("ps"));
        OptionBuilder.withArgName("pcl output file");
        OptionBuilder.withLongOpt("pcl");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Input will be rendered as PCL to named file");
        output.addOption(
                OptionBuilder.create("pcl"));
        OptionBuilder.withArgName("rtf output file");
        OptionBuilder.withLongOpt("rtf");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Input will be rendered as RTF to named file");
        output.addOption(
                OptionBuilder.create("rtf"));
        OptionBuilder.withArgName("mif output file");
        OptionBuilder.withLongOpt("mif");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Input will be rendered as MIF to named file");
        output.addOption(
                OptionBuilder.create("mif"));
        OptionBuilder.withArgName("svg output file");
        OptionBuilder.withLongOpt("svg");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Input will be rendered as SVG to named file");
        output.addOption(
                OptionBuilder.create("svg"));
        OptionBuilder.withArgName("text output file");
        OptionBuilder.withLongOpt("plain-text");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Input will be rendered as plain text to named file");
        output.addOption(
                OptionBuilder.create("txt"));
        OptionBuilder.withArgName("area tree output file");
        OptionBuilder.withLongOpt("area-tree");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Area tree will be output as XML to named file");
        output.addOption(
                OptionBuilder.create("at"));
        OptionBuilder.withArgName("help");
        OptionBuilder.withLongOpt("print");
        OptionBuilder.hasOptionalArg();
        OptionBuilder.withDescription("Input will be rendered and sent to the printer. "
                + "Requires extra arguments to the \"java\" command. "
                + "See options with \"-print help\".");
        output.addOption(
                OptionBuilder.create("print"));
        
        // -s option relevant only to -at area tree output.  Again, no way
        // to express this directly
        OptionBuilder.withArgName("supress low-level areas");
        OptionBuilder.withLongOpt("only-block-areas");
        OptionBuilder.withDescription("Suppress non-block areas in XML renderer");
        options.addOption(
                OptionBuilder.create("s"));
        return options;
    }
    
    private static final boolean STOP_AT_NON_OPTION = true;
    
    /**
     * parses the commandline arguments
     * @return true if parse was successful and processing can continue, false
     * if processing should stop
     * @exception FOPException if there was an error in the format of the options
     */
    private HashMap parseOptions(String[] args) throws FOPException {
        HashMap arguments = new HashMap();
        options = makeOptions();
        CommandLineParser parser = new PosixParser();
        CommandLine cli;
        String[] xslParams = null;
        String[] remArgs = null;
        try {
            cli = parser.parse(options, args, STOP_AT_NON_OPTION);
        } catch (ParseException e) {
            throw new FOPException(e);
        }
        // Find out what we have
        // Miscellaneous
        if (cli.hasOption("d")) {
            arguments.put("debugMode", Boolean.TRUE);
            //Fop.setLoggingLevel(Level.FINE);
            log.setLevel(Level.FINE);
        }
        if (cli.hasOption("q")) {
            arguments.put("quiet", Boolean.TRUE);
            //Fop.setLoggingLevel(Level.SEVERE);
            log.setLevel(Level.SEVERE);
        }
        if (cli.hasOption("x")) {
            arguments.put("dumpConfiguration", Boolean.TRUE);
            if (log.getLevel().intValue() > Level.CONFIG.intValue()) {
                //Fop.setLoggingLevel(Level.CONFIG);
                log.setLevel(Level.CONFIG);
            }
        }
        if (cli.hasOption("c")) {
            arguments.put("userConfigFileName", cli.getOptionValue("c"));
        }
        if (cli.hasOption("l")) {
            arguments.put("language", cli.getOptionValue("l"));
            //Locale.setDefault(new Locale(cli.getOptionValue("l")));
        }
        if (cli.hasOption("s")) {
            arguments.put("noLowLevelAreas", Boolean.TRUE);
        }
        if (cli.hasOption("fo")) {
            setInputMode(FO_INPUT, arguments);
            arguments.put("foFileName", cli.getOptionValue("fo"));
        }
        if (cli.hasOption("xml")) {
            if (cli.hasOption("xsl")) {
                setInputMode(XSLT_INPUT, arguments);
                arguments.put("xsltFileName", cli.getOptionValue("xsl"));
            } else {
                throw new FOPException(
                "XSLT file must be specified for the transform mode");
            }
            arguments.put("xmlFileName", cli.getOptionValue("xml"));
        } else {
            if (cli.hasOption("xsl")) {
                throw new FOPException(
                "XML file must be specified for the transform mode");
            }
        }
        // Any parameters?
        if (cli.hasOption("param")) {
            // TODO Don't know how to handle these yet
            xslParams = cli.getOptionValues("param");
        }
        
        // Output arguments
        if (cli.hasOption("awt")) {
            setOutputMode(AWT_OUTPUT, arguments);
        }
        if (cli.hasOption("pdf")) {
            setOutputMode(PDF_OUTPUT, arguments);
            arguments.put("outputFileName", cli.getOptionValue("pdf"));
        }
        if (cli.hasOption("mif")) {
            setOutputMode(MIF_OUTPUT, arguments);
            arguments.put("outputFileName", cli.getOptionValue("mif"));
        }
        if (cli.hasOption("rtf")) {
            setOutputMode(RTF_OUTPUT, arguments);
            arguments.put("outputFileName", cli.getOptionValue("rtf"));
        }
        if (cli.hasOption("pcl")) {
            setOutputMode(PCL_OUTPUT, arguments);
            arguments.put("outputFileName", cli.getOptionValue("pcl"));
        }
        if (cli.hasOption("ps")) {
            setOutputMode(PS_OUTPUT, arguments);
            arguments.put("outputFileName", cli.getOptionValue("ps"));
        }
        if (cli.hasOption("txt")) {
            setOutputMode(TXT_OUTPUT, arguments);
            arguments.put("outputFileName", cli.getOptionValue("txt"));
        }
        if (cli.hasOption("svg")) {
            setOutputMode(SVG_OUTPUT, arguments);
            arguments.put("outputFileName", cli.getOptionValue("svg"));
        }
        if (cli.hasOption("at")) {
            setOutputMode(AREA_OUTPUT, arguments);
            arguments.put("outputFileName", cli.getOptionValue("at"));
        }
        if (cli.hasOption("print")) {
            setOutputMode(PRINT_OUTPUT, arguments);
            if (cli.getOptionValue("print").toLowerCase(Locale.getDefault())
                    == "help") {
                printUsagePrintOutput();
                throw new FOPException("Usage only");
            }
        }
        // Get any remaining non-options
        remArgs = cli.getArgs();
        if (remArgs != null) {
            String filename = null;
            int i = 0;
            if (inputmode == NOT_SET && i < remArgs.length
                    && remArgs[i].charAt(0) != '-') {
                setInputMode(FO_INPUT, arguments);
                filename = remArgs[i++];
                arguments.put("foFileName", filename);
                foFile = new File(filename);
            }
            if (outputmode == NOT_SET && i < remArgs.length
                    && remArgs[i].charAt(0) != '-') {
                setOutputMode(PDF_OUTPUT, arguments);
                arguments.put("outputFileName", remArgs[i++]);
            }
            if (i < remArgs.length) {
                throw new FOPException("Don't know what to do with "
                        + remArgs[i]);
            }
        }
        return arguments;
    }    // end parseOptions
    

    /**
     * If the <code>String</code> value for the key <code>inputMode</code>
     * has not been installed in <code>Configuration</code>, install the
     * value passed in the parameter, and set the field <code>inputmode</code>
     * to the integer value associated with <code>mode</code>.
     * If the key already exists with the same value as <code>mode</code>,
     * do nothing.
     * If the key already exists with a different value to <code>mode</code>,
     * throw an exception.
     * @param mode the input mode code
     * @exception FOPException
     */
    private void setInputMode(int mode, HashMap arguments)
    throws FOPException {
        String tempMode = null;
        if ((tempMode = getInputMode()) == null) {
            arguments.put("inputMode", inputModes[mode]);
            inputmode = mode;
        } else if (tempMode.equals(inputModes[mode])) {
            return;
        } else {
            throw new FOPException("you can only set one input method");
        }
    }

    /**
     * If the <code>String</code> value for the key <code>outputMode</code>
     * has not been installed in <code>Configuration</code>, install the
     * value passed in the parameter, and set the field <code>outputmode</code>
     * to the integer value associated with <code>mode</code>.
     * If the key already exists with the same value as <code>mode</code>,
     * do nothing.
     * If the key already exists with a different value to <code>mode</code>,
     * throw an exception.
     * @param mode the output mode code
     * @exception FOPException
     */
    private void setOutputMode(int mode, HashMap arguments)
    throws FOPException {
        String tempMode = null;
        if ((tempMode = getOutputMode()) == null) {
            arguments.put("outputMode", outputModes[mode]);
            outputmode = mode;
        } else if (tempMode.equals(outputModes[mode])) {
            return;
        } else {
            throw new FOPException("you can only set one output method");
        }
    }
    
    /**
     * checks whether all necessary information has been given in a consistent way
     */
    private void checkSettings() throws FOPException, FileNotFoundException {
        if (inputmode == NOT_SET) {
            throw new FOPException("No input file specified");
        }
        
        if (outputmode == NOT_SET) {
            throw new FOPException("No output file specified");
        }
        
        if (inputmode == XSLT_INPUT) {
            if (!xmlFile.exists()) {
                throw new FileNotFoundException("Error: xml file "
                        + xmlFile.getAbsolutePath()
                        + " not found ");
            }
            if (!xsltFile.exists()) {
                throw new FileNotFoundException("Error: xsl file "
                        + xsltFile.getAbsolutePath()
                        + " not found ");
            }
            
        } else if (inputmode == FO_INPUT) {
            if (!foFile.exists()) {
                throw new FileNotFoundException("Error: fo file "
                        + foFile.getAbsolutePath()
                        + " not found ");
            }
        }
    }    // end checkSettings
    
    /**
     * @return the type chosen renderer
     * @throws FOPException for invalid output modes
     */
    public int getRenderer() throws FOPException {
        switch (outputmode) {
            case NOT_SET:
                throw new FOPException("Renderer has not been set!");
            case PDF_OUTPUT:
                return Driver.RENDER_PDF;
            case AWT_OUTPUT:
                return Driver.RENDER_AWT;
            case MIF_OUTPUT:
                return Driver.RENDER_MIF;
            case PRINT_OUTPUT:
                return Driver.RENDER_PRINT;
            case PCL_OUTPUT:
                return Driver.RENDER_PCL;
            case PS_OUTPUT:
                return Driver.RENDER_PS;
            case TXT_OUTPUT:
                return Driver.RENDER_TXT;
            case SVG_OUTPUT:
                return Driver.RENDER_SVG;
            case AREA_OUTPUT:
                rendererOptions.put("fineDetail", coarseAreaXmlValue());
                return Driver.RENDER_XML;
            case RTF_OUTPUT:
                return Driver.RENDER_RTF;
            default:
                throw new FOPException("Invalid Renderer setting!");
        }
    }
    
    /**
     * Get the input handler.
     * @return the input handler
     * @throws FOPException if creating the InputHandler fails
     */
    public InputHandler getInputHandler() throws FOPException {
        switch (inputmode) {
            case FO_INPUT:
                return new FOFileHandler(foFile);
            case XSLT_INPUT:
                return new XSLTInputHandler(xmlFile, xsltFile, xsltParams);
            default:
                throw new FOPException("Invalid inputmode setting!");
        }
    }
    
    /**
     * Get the renderer specific options.
     * @return hash map with option/value pairs.
     */
    public java.util.HashMap getRendererOptions() {
        return rendererOptions;
    }
    

    public String getInputMode() {
        return configuration.getStringValue("inputMode");
    }

    /**
     * Returns the input mode (type of input data, ex. NOT_SET or FO_INPUT)
     * @return the input mode
     */
    public int getInputModeIndex() throws FOPException {
        String mode;
        if ((mode = getInputMode()) == null) return NOT_SET;
        return inputModeIndex(mode);
    }

    public String getOutputMode() {
        return configuration.getStringValue("outputMode");
    }

    /**
     * Returns the output mode (output format, ex. NOT_SET or PDF_OUTPUT)
     * @return the output mode
     */
    public int getOutputModeIndex() throws FOPException {
        String mode;
        if ((mode = getOutputMode()) == null) return NOT_SET;
        return outputModeIndex(mode);
    }
    

    public String getFoFileName() {
        return configuration.getStringValue("foFileName");
    }

    public File getFoFile() {
        return foFile;
    }

    public String getXmlFileName() {
        return configuration.getStringValue("xmlFileName");
    }

    public File getXmlFile() {
        return xmlFile;
    }

    public String getXsltFileName() {
        return configuration.getStringValue("xsltFileName");
    }

    public File getXsltFile() {
        return xsltFile;
    }

    public String getOutputFileName() {
        return configuration.getStringValue("outputFileName");
    }

    public File getOutputFile() {
        return outputFile;
    }

    public String getUserConfigFileName() {
        return configuration.getStringValue("userConfigFileName");
    }

    public File getUserConfigFile() {
        return userConfigFile;
    }

    public String getBufferFileName() {
        return configuration.getStringValue("bufferFileName");
    }

    public File getBufferFile() {
        return bufferFile;
    }

    public String getLanguage() {
        return configuration.getStringValue("language");
    }

    public boolean isQuiet() {
        return configuration.isTrue("quiet");
    }

    public Boolean doDumpConfiguration() {
        return configuration.getBooleanObject("dumpConfiguration");
    }

    public boolean isDebugMode() {
        return configuration.isTrue("debugMode");
    }

    public Boolean coarseAreaXmlValue() {
        return configuration.getBooleanObject("noLowLevelAreas");
    }

    public boolean isCoarseAreaXml() {
        return configuration.isTrue("noLowLevelAreas");
    }

    /**
     * return either the foFile or the xmlFile
     */
    public File getInputFile() {
        switch (inputmode) {
        case FO_INPUT:
            return foFile;
        case XSLT_INPUT:
            return xmlFile;
        default:
            return foFile;
        }
    }

    /**
     * shows the commandline syntax including a summary of all available options and some examples
     */
    public void printUsage() {
        HelpFormatter help = new HelpFormatter();
        help.printHelp("FOP", options, true);
    }
    
    /**
     * shows the options for print output
     */
    public void printUsagePrintOutput() {
        System.err.println("USAGE: -print [-Dstart=i] [-Dend=i] [-Dcopies=i] [-Deven=true|false] "
                + " org.apache.fop.apps.Fop (..) -print \n"
                + "Example:\n"
                + "java -Dstart=1 -Dend=2 org.apache.Fop.apps.Fop infile.fo -print ");
    }
    
    
    /**
     * debug mode. outputs all commandline settings
     */
    private void debug() {
        StringBuffer fine = new StringBuffer();
        StringBuffer severe = new StringBuffer();
        fine.append("Input mode: ");
        switch (inputmode) {
            case NOT_SET:
                fine.append("not set");
                break;
            case FO_INPUT:
                fine.append("FO ");
                fine.append("fo input file: " + foFile.toString());
                break;
            case XSLT_INPUT:
                fine.append("xslt transformation");
                fine.append("xml input file: " + xmlFile.toString());
                fine.append("xslt stylesheet: " + xsltFile.toString());
                break;
            default:
                fine.append("unknown input type");
        }
        fine.append("\nOutput mode: ");
        switch (outputmode) {
            case NOT_SET:
                fine.append("not set");
                break;
            case PDF_OUTPUT:
                fine.append("pdf");
                fine.append("output file: " + outputFile.toString());
                break;
            case AWT_OUTPUT:
                fine.append("awt on screen");
                if (outputFile != null) {
                    severe.append("awt mode, but outfile is set:\n");
                    fine.append("out file: " + outputFile.toString());
                }
                break;
            case MIF_OUTPUT:
                fine.append("mif");
                fine.append("output file: " + outputFile.toString());
                break;
            case RTF_OUTPUT:
                fine.append("rtf");
                fine.append("output file: " + outputFile.toString());
                break;
            case PRINT_OUTPUT:
                fine.append("print directly");
                if (outputFile != null) {
                    severe.append("print mode, but outfile is set:\n");
                    severe.append("out file: " + outputFile.toString() + "\n");
                }
                break;
            case PCL_OUTPUT:
                fine.append("pcl");
                fine.append("output file: " + outputFile.toString());
                break;
            case PS_OUTPUT:
                fine.append("PostScript");
                fine.append("output file: " + outputFile.toString());
                break;
            case TXT_OUTPUT:
                fine.append("txt");
                fine.append("output file: " + outputFile.toString());
                break;
            case SVG_OUTPUT:
                fine.append("svg");
                fine.append("output file: " + outputFile.toString());
                break;
            default:
                fine.append("unknown input type");
        }
        
        
        fine.append("\nOPTIONS\n");
        if (userConfigFile != null) {
            fine.append("user configuration file: "
                    + userConfigFile.toString());
        } else {
            fine.append("no user configuration file is used [default]");
        }
        fine.append("\n");
        if (dumpConfig == true) {
            fine.append("dump configuration");
        } else {
            fine.append("don't dump configuration [default]");
        }
        fine.append("\n");
        if (configuration.isTrue("quiet")) {
            fine.append("quiet mode on");
        } else {
            fine.append("quiet mode off [default]");
        }
        fine.append("\n");
        log.fine(fine.toString());
        if (severe.toString() != "") {
            log.severe(severe.toString());
        }
        
    }
}
