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

package org.apache.fop.render.afp.modca;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.render.afp.modca.goca.GraphicsBox;
import org.apache.fop.render.afp.modca.goca.GraphicsData;
import org.apache.fop.render.afp.modca.goca.GraphicsFillet;
import org.apache.fop.render.afp.modca.goca.GraphicsFullArc;
import org.apache.fop.render.afp.modca.goca.GraphicsImageBegin;
import org.apache.fop.render.afp.modca.goca.GraphicsImageData;
import org.apache.fop.render.afp.modca.goca.GraphicsImageEnd;
import org.apache.fop.render.afp.modca.goca.GraphicsLine;
import org.apache.fop.render.afp.modca.goca.GraphicsSetArcParameters;
import org.apache.fop.render.afp.modca.goca.GraphicsSetCharacterSet;
import org.apache.fop.render.afp.modca.goca.GraphicsSetCurrentPosition;
import org.apache.fop.render.afp.modca.goca.GraphicsSetLineType;
import org.apache.fop.render.afp.modca.goca.GraphicsSetLineWidth;
import org.apache.fop.render.afp.modca.goca.GraphicsSetPatternSymbol;
import org.apache.fop.render.afp.modca.goca.GraphicsSetProcessColor;
import org.apache.fop.render.afp.modca.goca.GraphicsString;

/**
 * Top-level GOCA graphics object.
 * 
 * Acts as container and factory of all other graphic objects
 */
public class GraphicsObject extends AbstractDataObject {
        
    /**
     * The graphics data
     */
    private GraphicsData graphicsData = null;

    /**
     * Default constructor
     * 
     * @param name the name of graphics object
     */
    public GraphicsObject(String name) {
        super(name);
    }

    /**
     * {@inheritDoc}
     */
    public void setViewport(int x, int y, int width, int height,
            int widthRes, int heightRes, int rotation) {
        super.setViewport(x, y, width, height, widthRes, heightRes, rotation);
        objectEnvironmentGroup.setGraphicsData(
                widthRes, heightRes, 0, x + width, 0, y + height);        
    }

    /**
     * {@inheritDoc}
     */
    protected void writeStart(OutputStream os) throws IOException {
        super.writeStart(os);
        byte[] data = new byte[] {
            0x5A, // Structured field identifier
            0x00, //sfLen[0], // Length byte 1
            0x10, //sfLen[1], // Length byte 2
            (byte) 0xD3, // Structured field id byte 1
            (byte) 0xA8, // Structured field id byte 2
            (byte) 0xBB, // Structured field id byte 3
            0x00, // Flags
            0x00, // Reserved
            0x00, // Reserved
            super.nameBytes[0], // gdoName
            super.nameBytes[1],
            super.nameBytes[2],
            super.nameBytes[3],
            super.nameBytes[4],
            super.nameBytes[5],
            super.nameBytes[6],
            super.nameBytes[7]
        };
        os.write(data);
    }

    /**
     * {@inheritDoc}
     */
    protected void writeEnd(OutputStream os) throws IOException {
        byte[] data = new byte[] {
            0x5A, // Structured field identifier
            0x00, // sfLen[0], // Length byte 1
            0x10, // sfLen[1], // Length byte 2
            (byte) 0xD3, // Structured field id byte 1
            (byte) 0xA9, // Structured field id byte 2
            (byte) 0xBB, // Structured field id byte 3
            0x00, // Flags
            0x00, // Reserved
            0x00, // Reserved
            super.nameBytes[0], // gdoName
            super.nameBytes[1],
            super.nameBytes[2],
            super.nameBytes[3],
            super.nameBytes[4],
            super.nameBytes[5],
            super.nameBytes[6],
            super.nameBytes[7]
        };
        os.write(data);
    }

    /**
     * {@inheritDoc}
     */
    protected PreparedAFPObject addDrawingOrder(PreparedAFPObject drawingOrder) {
        if (graphicsData == null
                || (graphicsData.getDataLength() + drawingOrder.getDataLength())
                >= GraphicsData.MAX_DATA_LEN) {
            newData();
        }
        graphicsData.addDrawingOrder(drawingOrder);
        return drawingOrder;
    }
    
    /**
     * Gets the current graphics data, creating a new one if necessary
     * @return the current graphics data
     */
    private GraphicsData getData() {
        if (this.graphicsData == null) {
            return newData();
        }
        return this.graphicsData;
    }
    
    /**
     * Creates a new graphics data
     * @return a newly created graphics data
     */
    private GraphicsData newData() {
        this.graphicsData = new GraphicsData();            
        super.addDrawingOrder(graphicsData);
        return graphicsData;
    }
    
    /**
     * Sets the current color
     * @param col the active color to use
     */
    public void setColor(Color col) {
        addDrawingOrder(new GraphicsSetProcessColor(col));
    }

    /**
     * Sets the current position
     * @param coords the x and y coordinates of the current position
     */
    public void setCurrentPosition(int[] coords) {
        addDrawingOrder(new GraphicsSetCurrentPosition(coords));
    }

    /**
     * Sets the line width
     * @param multiplier the line width multiplier
     */
    public void setLineWidth(int multiplier) {
        GraphicsSetLineWidth lw = new GraphicsSetLineWidth(multiplier);
        addDrawingOrder(lw);
    }

    /**
     * Sets the line type
     * @param type the line type
     */
    public void setLineType(byte type) {
        GraphicsSetLineType lt = new GraphicsSetLineType(type);
        addDrawingOrder(lt);
    }    

    /**
     * Sets whether to fill the next shape
     * @param fill whether to fill the next shape
     */
    public void setFill(boolean fill) {
        GraphicsSetPatternSymbol pat = new GraphicsSetPatternSymbol(
                fill ? GraphicsSetPatternSymbol.SOLID_FILL
                     : GraphicsSetPatternSymbol.NO_FILL
        );
        addDrawingOrder(pat);
    }
    
    /**
     * Sets the character set to use
     * @param fontReference the character set (font) reference
     */
    public void setCharacterSet(int fontReference) {
        addDrawingOrder(new GraphicsSetCharacterSet(fontReference));
    }

    /**
     * Adds a line at the given x/y coordinates
     * @param coords the x/y coordinates (can be a series)
     */
    public void addLine(int[] coords) {
        addDrawingOrder(new GraphicsLine(coords));
    }

    /**
     * Adds a box at the given coordinates
     * @param coords the x/y coordinates
     */
    public void addBox(int[] coords) {
        addDrawingOrder(new GraphicsBox(coords));
    }

    /**
     * Adds a fillet (curve) at the given coordinates
     * @param coords the x/y coordinates
     */
    public void addFillet(int[] coords) {
        addDrawingOrder(new GraphicsFillet(coords));
    }

    /**
     * Sets the arc parameters 
     * @param xmaj the maximum value of the x coordinate
     * @param ymin the minimum value of the y coordinate
     * @param xmin the minimum value of the x coordinate
     * @param ymaj the maximum value of the y coordinate
     */
    public void setArcParams(int xmaj, int ymin, int xmin, int ymaj) {
        addDrawingOrder(new GraphicsSetArcParameters(xmaj, ymin, xmin, ymaj));
    }

    /**
     * Adds an arc
     * @param x the x coordinate
     * @param y the y coordinate
     * @param mh the integer portion of the multiplier
     * @param mhr the fractional portion of the multiplier
     */
    public void addFullArc(int x, int y, int mh, int mhr) {
        addDrawingOrder(new GraphicsFullArc(x, y, mh, mhr));
    }

    /**
     * Adds an image
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the image width
     * @param height the image height
     * @param rawData the image data
     */
    public void addImage(int x, int y, int width, int height, byte[] rawData) {
        addDrawingOrder(new GraphicsImageBegin(x, y, width, height));
        for (int startIndex = 0;
            startIndex <= rawData.length;
            startIndex += GraphicsImageData.MAX_DATA_LEN) {
            addDrawingOrder(new GraphicsImageData(rawData, startIndex));
        }
        addDrawingOrder(new GraphicsImageEnd());
    }

    /**
     * Adds a string
     * @param str the string
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void addString(String str, int x, int y) {
        addDrawingOrder(new GraphicsString(str, x, y));
    }
    
    /**
     * Begins a graphics area (start of fill)
     */
    public void beginArea() {
        if (graphicsData == null) {
            newData();
        }
        graphicsData.beginArea();
    }

    /**
     * Ends a graphics area (end of fill)
     */
    public void endArea() {
        if (graphicsData != null) {
            graphicsData.endArea();
        }
    }
        
    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "GraphicsObject";
    }

    /**
     * Creates a new graphics segment
     */
    public void newSegment() {
        getData().newSegment();
    }
}