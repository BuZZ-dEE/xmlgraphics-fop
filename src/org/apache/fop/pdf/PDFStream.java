/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.pdf;

// Java
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Vector;
import java.util.Enumeration;
// Fop
import org.apache.fop.configuration.Configuration;
import org.apache.fop.messaging.MessageHandler;

/**
 * class representing a PDF stream.
 *
 * A derivative of the PDF Object, a PDF Stream has not only a dictionary
 * but a stream of PDF commands. The stream of commands is where the real
 * work is done, the dictionary just provides information like the stream
 * length.
 */
public class PDFStream extends PDFObject {

    /**
     * the stream of PDF commands
     */
    protected ByteArrayOutputStream _data;

    /**
     * the filters that should be applied
     */
    private Vector _filters;

    /**
     * create an empty stream object
     *
     * @param number the object's number
     */
    public PDFStream(int number) {
        super(number);
        _data = new ByteArrayOutputStream();
        _filters = new Vector();
    }

    /**
     * append data to the stream
     *
     * @param s the string of PDF to add
     */
    public void add(String s) {
        try {
            try {
                _data.write(s.getBytes(PDFDocument.ENCODING));
            } catch (UnsupportedEncodingException ue) {
                _data.write(s.getBytes());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Add a filter for compression of the stream. Filters are
     * applied in the order they are added. This should always be a
     * new instance of the particular filter of choice. The applied
     * flag in the filter is marked true after it has been applied to the
     * data.
     */
    public void addFilter(PDFFilter filter) {
        if (filter != null) {
            _filters.addElement(filter);
        }

    }

    public void addFilter(String filterType) {
        if (filterType == null) {
            return;
        }
        if (filterType.equals("flate")) {
            addFilter(new FlateFilter());
        } else if (filterType.equals("ascii-85")) {
            addFilter(new ASCII85Filter());
        } else if (filterType.equals("ascii-hex")) {
            addFilter(new ASCIIHexFilter());
        } else if (filterType.equals("")) {
            return;
        } else {
            MessageHandler.errorln("Unsupported filter type in stream-filter-list: "
                                   + filterType);
        }
    }


    protected void addDefaultFilters() {
        Vector filters = Configuration.getListValue("stream-filter-list",
                                                    Configuration.PDF);
        if (filters == null) {
            // try getting it as a String
            String filter = Configuration.getStringValue("stream-filter-list",
                    Configuration.PDF);
            if (filter == null) {
                // built-in default to flate
                addFilter(new FlateFilter());
            } else {
                addFilter(filter);
            }
        } else {
            for (int i = 0; i < filters.size(); i++) {
                String v = (String)filters.elementAt(i);
                addFilter(v);
            }
        }
    }


    /**
     * append an array of xRGB pixels, ASCII Hex Encoding it first
     *
     * @param pixels the area of pixels
     * @param width the width of the image in pixels
     * @param height the height of the image in pixels
     */
    public void addImageArray(int[] pixels, int width, int height) {
        try {
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int p = pixels[i * width + j];
                    int r = (p >> 16) & 0xFF;
                    int g = (p >> 8) & 0xFF;
                    int b = (p) & 0xFF;
                    if (r < 16) {
                        _data.write('0');
                    }
                    try {
                        _data.write(Integer.toHexString(r).getBytes(PDFDocument.ENCODING));
                    } catch (UnsupportedEncodingException ue) {
                        _data.write(Integer.toHexString(r).getBytes());
                    }
                    if (g < 16) {
                        _data.write('0');
                    }
                    try {
                        _data.write(Integer.toHexString(g).getBytes(PDFDocument.ENCODING));
                    } catch (UnsupportedEncodingException ue) {
                        _data.write(Integer.toHexString(g).getBytes());
                    }
                    if (b < 16) {
                        _data.write('0');
                    }
                    try {
                        _data.write(Integer.toHexString(b).getBytes(PDFDocument.ENCODING));
                    } catch (UnsupportedEncodingException ue) {
                        _data.write(Integer.toHexString(b).getBytes());
                    }
                    _data.write(' ');
                }
            }
            try {
                _data.write(">\n".getBytes(PDFDocument.ENCODING));
            } catch (UnsupportedEncodingException ue) {
                _data.write(">\n".getBytes());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public void setData(byte[] data) throws IOException {
        _data.reset();
        _data.write(data);
    }

    public byte[] getData() {
        return _data.toByteArray();
    }

    public int getDataLength() {
        return _data.size();
    }



    /**
     * represent as PDF.
     *
     * @return the PDF string.
     */
    /*
     * public byte[] toPDF() {
     * byte[] d = _data.toByteArray();
     * ByteArrayOutputStream s = new ByteArrayOutputStream();
     * String p = this.number + " " + this.generation
     * + " obj\n<< /Length " + (d.length+1)
     * + " >>\nstream\n";
     * s.write(p.getBytes());
     * s.write(d);
     * s.write("\nendstream\nendobj\n".getBytes());
     * return s.toByteArray();
     * }
     */
    public byte[] toPDF() {
        throw new RuntimeException();
    }


    // overload the base object method so we don't have to copy
    // byte arrays around so much
    protected int output(OutputStream stream) throws IOException {
        int length = 0;
        String filterEntry = applyFilters();
        String s = this.number + " " + this.generation + " obj\n<< /Length "
                    + (_data.size() + 1) + " " + filterEntry
                    + " >>\n";
        byte[] p;
        try {
            p = s.getBytes(PDFDocument.ENCODING);
        } catch (UnsupportedEncodingException ue) {
            p = s.getBytes();
        }
        stream.write(p);
        length += p.length;
        length += outputStreamData(stream);
        try {
            p = "endobj\n".getBytes(PDFDocument.ENCODING);
        } catch (UnsupportedEncodingException ue) {
            p = "endobj\n".getBytes();
        }
        stream.write(p);
        length += p.length;
        return length;

    }

    /**
     * Output just the stream data enclosed by stream/endstream markers
     */
    protected int outputStreamData(OutputStream stream) throws IOException {
        int length = 0;
        byte[] p;
        try {
            p = "stream\n".getBytes(PDFDocument.ENCODING);
        } catch (UnsupportedEncodingException ue) {
            p = "stream\n".getBytes();
        }
        stream.write(p);
        length += p.length;
        _data.writeTo(stream);
        length += _data.size();
        try {
            p = "\nendstream\n".getBytes(PDFDocument.ENCODING);
        } catch (UnsupportedEncodingException ue) {
            p = "\nendstream\n".getBytes();
        }
        stream.write(p);
        length += p.length;
        return length;

    }


    /**
     * Apply the filters to the data
     * in the order given and return the /Filter and /DecodeParms
     * entries for the stream dictionary. If the filters have already
     * been applied to the data (either externally, or internally)
     * then the dictionary entries are built and returned.
     */
    protected String applyFilters() throws IOException {
        if (_filters.size() > 0) {
            Vector names = new Vector();
            Vector parms = new Vector();

            // run the filters
            Enumeration e = _filters.elements();
            while (e.hasMoreElements()) {
                PDFFilter filter = (PDFFilter)e.nextElement();
                // apply the filter encoding if neccessary
                if (!filter.isApplied()) {
                    byte[] tmp = filter.encode(_data.toByteArray());
                    _data.reset();
                    _data.write(tmp);
                    filter.setApplied(true);
                }
                // place the names in our local vector in reverse order
                names.insertElementAt(filter.getName(), 0);
                parms.insertElementAt(filter.getDecodeParms(), 0);
            }

            // now build up the filter entries for the dictionary
            return buildFilterEntries(names) + buildDecodeParms(parms);
        }
        return "";

    }

    private String buildFilterEntries(Vector names) {
        StringBuffer sb = new StringBuffer();
        sb.append("/Filter ");
        if (names.size() > 1) {
            sb.append("[ ");
        }
        Enumeration e = names.elements();
        while (e.hasMoreElements()) {
            sb.append((String)e.nextElement());
            sb.append(" ");
        }
        if (names.size() > 1) {
            sb.append("]");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String buildDecodeParms(Vector parms) {
        StringBuffer sb = new StringBuffer();
        boolean needParmsEntry = false;
        sb.append("/DecodeParms ");

        if (parms.size() > 1) {
            sb.append("[ ");
        }
        Enumeration e = parms.elements();
        while (e.hasMoreElements()) {
            String s = (String)e.nextElement();
            if (s != null) {
                sb.append(s);
                needParmsEntry = true;
            } else {
                sb.append("null");
            }
            sb.append(" ");
        }
        if (parms.size() > 1) {
            sb.append("]");
        }
        sb.append("\n");
        if (needParmsEntry) {
            return sb.toString();
        } else {
            return "";
        }
    }


}
