/*
 * Copyright 2006 The Apache Software Foundation.
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

package org.apache.fop.apps;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.ElementMappingRegistry;
import org.apache.fop.image.ImageFactory;
import org.apache.fop.layoutmgr.LayoutManagerMaker;
import org.apache.fop.render.RendererFactory;
import org.apache.fop.render.XMLHandlerRegistry;
import org.apache.fop.util.ContentHandlerFactoryRegistry;
import org.xml.sax.SAXException;

/**
 * Factory class which instantiates new Fop and FOUserAgent instances. This class also holds
 * environmental information and configuration used by FOP. Information that may potentially be
 * different for each rendering run can be found and managed in the FOUserAgent.
 */
public class FopFactory {

    /** Defines the default source resolution (72dpi) for FOP */
    private static final float DEFAULT_SOURCE_RESOLUTION = 72.0f; //dpi
    /** Defines the default page-height */
    private static final String DEFAULT_PAGE_HEIGHT = "11in";
    /** Defines the default page-width */
    private static final String DEFAULT_PAGE_WIDTH = "8.26in";

    /** logger instance */
    private static Log log = LogFactory.getLog(FopFactory.class);
    
    /** Factory for Renderers and FOEventHandlers */
    private RendererFactory rendererFactory = new RendererFactory();
    
    /** Registry for XML handlers */
    private XMLHandlerRegistry xmlHandlers = new XMLHandlerRegistry();
    
    /** The registry for ElementMapping instances */
    private ElementMappingRegistry elementMappingRegistry;

    /** The registry for ContentHandlerFactory instance */ 
    private ContentHandlerFactoryRegistry contentHandlerFactoryRegistry 
                = new ContentHandlerFactoryRegistry();
    
    private ImageFactory imageFactory = new ImageFactory();

    /** user configuration */
    private Configuration userConfig = null;

    /** The base URL for all font URL resolutions */
    private String fontBaseURL;
    
    /**
     * FOP has the ability, for some FO's, to continue processing even if the
     * input XSL violates that FO's content model.  This is the default  
     * behavior for FOP.  However, this flag, if set, provides the user the
     * ability for FOP to halt on all content model violations if desired.   
     */ 
    private boolean strictValidation = true;

    /** Source resolution in dpi */
    private float sourceResolution = DEFAULT_SOURCE_RESOLUTION;
    private String pageHeight = DEFAULT_PAGE_HEIGHT;
    private String pageWidth = DEFAULT_PAGE_WIDTH;

    /** @see #setBreakIndentInheritanceOnReferenceAreaBoundary(boolean) */
    private boolean breakIndentInheritanceOnReferenceAreaBoundary = false;

    /** Additional fo.ElementMapping subclasses set by user */
    private List additionalElementMappings = null;

    /** Optional overriding LayoutManagerMaker */
    private LayoutManagerMaker lmMakerOverride = null;
    
    /**
     * Main constructor.
     */
    protected FopFactory() {
        this.elementMappingRegistry = new ElementMappingRegistry(this);
    }
    
    /**
     * Returns a new FopFactory instance.
     * @return the requested FopFactory instance.
     */
    public static FopFactory newInstance() {
        return new FopFactory();
    }
    
    /**
     * Returns a new FOUserAgent instance. Use the FOUserAgent to configure special values that
     * are particular to a rendering run. Don't reuse instances over multiple rendering runs but
     * instead create a new one each time and reuse the FopFactory.
     * @return the newly created FOUserAgent instance initialized with default values
     */
    public FOUserAgent newFOUserAgent() {
        FOUserAgent userAgent = new FOUserAgent(this);
        return userAgent;
    }

    /**
     * Returns a new {@link Fop} instance. FOP will be configured with a default user agent 
     * instance.
     * <p>
     * MIME types are used to select the output format (ex. "application/pdf" for PDF). You can
     * use the constants defined in {@link MimeConstants}.
     * @param outputFormat the MIME type of the output format to use (ex. "application/pdf").     
     * @return the new Fop instance
     */
    public Fop newFop(String outputFormat) {
        return new Fop(outputFormat, newFOUserAgent());
    }

    /**
     * Returns a new {@link Fop} instance. Use this factory method if you want to configure this 
     * very rendering run, i.e. if you want to set some metadata like the title and author of the
     * document you want to render. In that case, create a new {@link FOUserAgent} 
     * instance using {@link #newFOUserAgent()}.
     * <p>
     * MIME types are used to select the output format (ex. "application/pdf" for PDF). You can
     * use the constants defined in {@link MimeConstants}.
     * @param outputFormat the MIME type of the output format to use (ex. "application/pdf").
     * @param userAgent the user agent that will be used to control the rendering run     
     * @return the new Fop instance
     */
    public Fop newFop(String outputFormat, FOUserAgent userAgent) {
        if (userAgent == null) {
            throw new NullPointerException("The userAgent parameter must not be null!");
        }
        return new Fop(outputFormat, userAgent);
    }
    
    /**
     * Returns a new {@link Fop} instance. Use this factory method if you want to supply your
     * own {@link org.apache.fop.render.Renderer Renderer} or 
     * {@link org.apache.fop.fo.FOEventHandler FOEventHandler} 
     * instance instead of the default ones created internally by FOP.
     * @param userAgent the user agent that will be used to control the rendering run     
     * @return the new Fop instance
     */
    public Fop newFop(FOUserAgent userAgent) {
        if (userAgent.getRendererOverride() == null 
                && userAgent.getFOEventHandlerOverride() == null) {
            throw new IllegalStateException("Either the overriding renderer or the overriding"
                    + " FOEventHandler must be set when this factory method is used!");
        }
        return newFop(null, userAgent);
    }

    /** @return the RendererFactory */
    public RendererFactory getRendererFactory() {
        return this.rendererFactory;
    }

    /** @return the XML handler registry */
    public XMLHandlerRegistry getXMLHandlerRegistry() {
        return this.xmlHandlers;
    }
    
    /** @return the element mapping registry */
    public ElementMappingRegistry getElementMappingRegistry() {
        return this.elementMappingRegistry;
    }

    /** @return the content handler factory registry */
    public ContentHandlerFactoryRegistry getContentHandlerFactoryRegistry() {
        return this.contentHandlerFactoryRegistry;
    }

    /** @return the image factory */
    public ImageFactory getImageFactory() {
        return this.imageFactory;
    }

    /**
     * Add the element mapping with the given class name.
     * @param elementMapping the class name representing the element mapping.
     */
    public void addElementMapping(ElementMapping elementMapping) {
        if (additionalElementMappings == null) {
            additionalElementMappings = new java.util.ArrayList();
        }
        additionalElementMappings.add(elementMapping);
    }

    /**
     * Returns the List of user-added ElementMapping class names
     * @return List of Strings holding ElementMapping names.
     */
    public List getAdditionalElementMappings() {
        return additionalElementMappings;
    }

    /**
     * Sets an explicit LayoutManagerMaker instance which overrides the one
     * defined by the AreaTreeHandler.
     * @param lmMaker the LayoutManagerMaker instance
     */
    public void setLayoutManagerMakerOverride(LayoutManagerMaker lmMaker) {
        this.lmMakerOverride = lmMaker;
    }

    /**
     * Returns the overriding LayoutManagerMaker instance, if any.
     * @return the overriding LayoutManagerMaker or null
     */
    public LayoutManagerMaker getLayoutManagerMakerOverride() {
        return this.lmMakerOverride;
    }

    /**
     * Sets the font base URL.
     * @param fontBaseURL font base URL
     */
    public void setFontBaseURL(String fontBaseURL) {
        this.fontBaseURL = fontBaseURL;
    }

    /** @return the font base URL */
    public String getFontBaseURL() {
        return this.fontBaseURL;
    }

    /**
     * Activates strict XSL content model validation for FOP
     * Default is false (FOP will continue processing where it can)
     * @param validateStrictly true to turn on strict validation
     */
    public void setStrictValidation(boolean validateStrictly) {
        this.strictValidation = validateStrictly;
    }

    /**
     * Returns whether FOP is strictly validating input XSL
     * @return true of strict validation turned on, false otherwise
     */
    public boolean validateStrictly() {
        return strictValidation;
    }

    /**
     * @return true if the indent inheritance should be broken when crossing reference area 
     *         boundaries (for more info, see the javadoc for the relative member variable)
     */
    public boolean isBreakIndentInheritanceOnReferenceAreaBoundary() {
        return breakIndentInheritanceOnReferenceAreaBoundary;
    }

    /**
     * Controls whether to enable a feature that breaks indent inheritance when crossing
     * reference area boundaries.
     * <p>
     * This flag controls whether FOP will enable special code that breaks property
     * inheritance for start-indent and end-indent when the evaluation of the inherited
     * value would cross a reference area. This is described under
     * http://wiki.apache.org/xmlgraphics-fop/IndentInheritance as is intended to
     * improve interoperability with commercial FO implementations and to produce
     * results that are more in line with the expectation of unexperienced FO users.
     * Note: Enabling this features violates the XSL specification!
     * @param value true to enable the feature
     */
    public void setBreakIndentInheritanceOnReferenceAreaBoundary(boolean value) {
        this.breakIndentInheritanceOnReferenceAreaBoundary = value;
    }
    
    /** @return the resolution for resolution-dependant input */
    public float getSourceResolution() {
        return this.sourceResolution;
    }

    /**
     * Returns the conversion factor from pixel units to millimeters. This
     * depends on the desired source resolution.
     * @return float conversion factor
     * @see #getSourceResolution()
     */
    public float getSourcePixelUnitToMillimeter() {
        return 25.4f / getSourceResolution(); 
    }
    
    /**
     * Sets the source resolution in dpi. This value is used to interpret the pixel size
     * of source documents like SVG images and bitmap images without resolution information.
     * @param dpi resolution in dpi
     */
    public void setSourceResolution(int dpi) {
        this.sourceResolution = dpi;
    }
    
    /**
     * Gets the default page-height to use as fallback,
     * in case page-height="auto"
     * 
     * @return the page-height, as a String
     */
    public String getPageHeight() {
        return this.pageHeight;
    }
    
    /**
     * Sets the page-height to use as fallback, in case
     * page-height="auto"
     * 
     * @param pageHeight    page-height as a String
     */
    public void setPageHeight(String pageHeight) {
        this.pageHeight = pageHeight;
    }
    
    /**
     * Gets the default page-width to use as fallback,
     * in case page-width="auto"
     * 
     * @return the page-width, as a String
     */
    public String getPageWidth() {
        return this.pageWidth;
    }
    
    /**
     * Sets the page-width to use as fallback, in case
     * page-width="auto"
     * 
     * @param pageWidth    page-width as a String
     */
    public void setPageWidth(String pageWidth) {
        this.pageWidth = pageWidth;
    }
    
    //------------------------------------------- Configuration stuff
    
    /**
     * Set the user configuration.
     * @param userConfigFile the configuration file
     * @throws IOException if an I/O error occurs
     * @throws SAXException if a parsing error occurs
     */
    public void setUserConfig(File userConfigFile)
                throws SAXException, IOException {
        try {
            DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
            setUserConfig(cfgBuilder.buildFromFile(userConfigFile));
        } catch (ConfigurationException cfge) {
            log.error("Error loading configuration: "
                    + cfge.getMessage());
        }
    }
    
    /**
     * Set the user configuration from an URI.
     * @param uri the URI to the configuration file
     * @throws IOException if an I/O error occurs
     * @throws SAXException if a parsing error occurs
     */
    public void setUserConfig(String uri)
                throws SAXException, IOException {
        try {
            DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
            setUserConfig(cfgBuilder.build(uri));
        } catch (ConfigurationException cfge) {
            log.error("Error loading configuration: "
                    + cfge.getMessage());
        }
    }
    
    /**
     * Set the user configuration.
     * @param userConfig configuration
     */
    public void setUserConfig(Configuration userConfig) {
        this.userConfig = userConfig;
        try {
            initUserConfig();
        } catch (ConfigurationException cfge) {
            log.error("Error initializing factory configuration: "
                    + cfge.getMessage());
        }
    }

    /**
     * Get the user configuration.
     * @return the user configuration
     */
    public Configuration getUserConfig() {
        return userConfig;
    }
    
    /**
     * Initializes user agent settings from the user configuration
     * file, if present: baseURL, resolution, default page size,...
     * 
     * @throws ConfigurationException when there is an entry that 
     *          misses the required attribute
     */
    public void initUserConfig() throws ConfigurationException {
        log.debug("Initializing User Agent Configuration");
        setFontBaseURL(getBaseURLfromConfig(userConfig, "font-base"));
        if (userConfig.getChild("source-resolution", false) != null) {
            this.sourceResolution 
                = userConfig.getChild("source-resolution").getValueAsFloat(
                        DEFAULT_SOURCE_RESOLUTION);
            log.info("Source resolution set to: " + sourceResolution 
                    + "dpi (px2mm=" + getSourcePixelUnitToMillimeter() + ")");
        }
        if (userConfig.getChild("strict-validation", false) != null) {
            this.strictValidation = userConfig.getChild("strict-validation").getValueAsBoolean();
        }
        if (userConfig.getChild("break-indent-inheritance", false) != null) {
            this.breakIndentInheritanceOnReferenceAreaBoundary 
                = userConfig.getChild("break-indent-inheritance").getValueAsBoolean();
        }
        Configuration pageConfig = userConfig.getChild("default-page-settings");
        if (pageConfig.getAttribute("height", null) != null) {
            setPageHeight(pageConfig.getAttribute("height"));
            log.info("Default page-height set to: " + pageHeight);
        }
        if (pageConfig.getAttribute("width", null) != null) {
            setPageWidth(pageConfig.getAttribute("width"));
            log.info("Default page-width set to: " + pageWidth);
        }
    }

    /**
     * Retrieves and verifies a base URL.
     * @param cfg The Configuration object to retrieve the base URL from
     * @param name the element name for the base URL
     * @return the requested base URL or null if not available
     */
    public static String getBaseURLfromConfig(Configuration cfg, String name) {
        if (cfg.getChild(name, false) != null) {
            try {
                String cfgBaseDir = cfg.getChild(name).getValue(null);
                if (cfgBaseDir != null) {
                    File dir = new File(cfgBaseDir);
                    if (dir.isDirectory()) {
                        cfgBaseDir = "file://" + dir.getCanonicalPath() 
                            + System.getProperty("file.separator");
                        cfgBaseDir = cfgBaseDir.replace(
                                System.getProperty("file.separator").charAt(0), '/');
                    } else {
                        //The next statement is for validation only
                        new URL(cfgBaseDir);
                    }
                }
                log.info(name + " set to: " + cfgBaseDir);
                return cfgBaseDir;
            } catch (MalformedURLException mue) {
                log.error("Base URL in user config is malformed!");
            } catch (IOException ioe) {
                log.error("Error converting relative base directory to absolute URL.");
            }
        }
        return null;
    }

    
    
}
