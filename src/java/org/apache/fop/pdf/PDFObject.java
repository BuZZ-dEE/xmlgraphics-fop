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
 
package org.apache.fop.pdf;

// Java
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * generic PDF object.
 *
 * A PDF Document is essentially a collection of these objects. A PDF
 * Object has a number and a generation (although the generation will always
 * be 0 in new documents).
 */
public abstract class PDFObject implements PDFWritable {

    /** logger for all PDFObjects (and descendants) */
    protected static Log log = LogFactory.getLog(PDFObject.class.getName());

    /**
     * the object's number
     */
    private int objnum;

    /**
     * the object's generation (0 in new documents)
     */
    private int generation = 0;

    /**
     * the parent PDFDocument
     */
    private PDFDocument document;

    /** the parent PDFObject (may be null and may not always be set, needed for encryption) */
    private PDFObject parent;
    
    /**
     * Returns the object's number.
     * @return the PDF Object number
     */
    public int getObjectNumber() {
        if (this.objnum == 0) {
            throw new IllegalStateException("Object has no number assigned: " + this.toString());
        }
        return this.objnum;
    }
    
    /**
     * Default constructor.
     */
    public PDFObject() {
        //nop
    }
    
    /**
     * Constructor for direct objects.
     * @param parent the containing PDFObject instance
     */
    public PDFObject(PDFObject parent) {
        setParent(parent);
    }
    
    /**
     * Indicates whether this PDFObject has already been assigned an 
     * object number.
     * @return True if it has an object number
     */
    public boolean hasObjectNumber() {
        return this.objnum > 0;
    }

    /**
     * Sets the object number
     * @param objnum the object number
     */
    public void setObjectNumber(int objnum) {
        this.objnum = objnum;
        if (log.isTraceEnabled()) {
            log.trace("Assigning " + this + " object number " + objnum);
        }
    }

    /**
     * Returns the object's generation.
     * @return the PDF Object generation
     */
    public int getGeneration() {
        return this.generation;
    }

    /**
     * Returns the parent PDFDocument if assigned.
     * @return the parent PDFDocument (May be null if the parent PDFDocument
     * has not been assigned)
     */
    public final PDFDocument getDocument() {
        if (this.document != null) {
            return this.document;
        } else if (getParent() != null) {
            return getParent().getDocument();
        } else {
            return null;
        }
    }

    /**
     * Returns the parent PDFDocument, but unlike <code>getDocument()</code>
     * it throws an informative Exception if the parent document is unavailable
     * instead of having a NullPointerException somewhere without a message.
     * @return the parent PDFDocument
     */
    public final PDFDocument getDocumentSafely() {
        final PDFDocument doc = getDocument();
        if (doc == null) {
            throw new IllegalStateException("Parent PDFDocument is unavailable");
        }
        return doc;
    }

    /**
     * Sets the parent PDFDocument.
     * @param doc the PDFDocument.
     */
    public void setDocument(PDFDocument doc) {
        this.document = doc;
    }

    /**
     * Returns this objects's parent. The parent is null if it is a "direct object".
     * @return the parent or null if there's no parent (or it hasn't been set)
     */
    public PDFObject getParent() {
        return this.parent;
    }
    
    /**
     * Sets the direct parent object.
     * @param parent the direct parent
     */
    public void setParent(PDFObject parent) {
        this.parent = parent;
    }

    /**
     * Returns the PDF representation of the Object ID.
     * @return the Object ID
     */
    public String getObjectID() {
        return getObjectNumber() + " " + getGeneration() + " obj\n";
    }

    /**
     * Returns the PDF representation of a reference to this object.
     * @return the reference string
     */
    public String referencePDF() {
        if (!hasObjectNumber()) {
            throw new IllegalArgumentException(
                    "Cannot reference this object. It doesn't have an object number");
        }
        String ref = getObjectNumber() + " " + getGeneration() + " R";
        return ref;
    }

    /**
     * Creates and returns a reference to this object.
     * @return the object reference
     */
    public PDFReference makeReference() {
        return new PDFReference(this);
    }
    
    /**
     * Write the PDF represention of this object
     *
     * @param stream the stream to write the PDF to
     * @throws IOException if there is an error writing to the stream
     * @return the number of bytes written
     */
    protected int output(OutputStream stream) throws IOException {
        byte[] pdf = this.toPDF();
        stream.write(pdf);
        return pdf.length;
    }

    /** {@inheritDoc} */
    public void outputInline(OutputStream out, Writer writer) throws IOException {
        if (hasObjectNumber()) {
            writer.write(referencePDF());
        } else {
            writer.flush();
            output(out);
        }
    }
    
    /**
     * Encodes the object as a byte array for output to a PDF file.
     *
     * @return PDF string
     */
    protected byte[] toPDF() {
        return encode(toPDFString());
    }
    
    
    /**
     * This method returns a String representation of the PDF object. The result
     * is normally converted/encoded to a byte array by toPDF(). Only use 
     * this method to implement the serialization if the object can be fully 
     * represented as text. If the PDF representation of the object contains
     * binary content use toPDF() or output(OutputStream) instead. This applies
     * to any object potentially containing a string object because string object
     * are encrypted and therefore need to be binary.
     * @return String the String representation
     */
    protected String toPDFString() {
        throw new UnsupportedOperationException("Not implemented. "
                    + "Use output(OutputStream) instead.");
    }
    
    /**
     * Converts text to a byte array for writing to a PDF file.
     * @param text text to convert/encode
     * @return byte[] the resulting byte array
     */
    public static final byte[] encode(String text) {
        return PDFDocument.encode(text);
    }
    
    /**
     * Encodes a Text String (3.8.1 in PDF 1.4 specs)
     * @param text the text to encode
     * @return byte[] the encoded text
     */
    protected byte[] encodeText(String text) {
        if (getDocumentSafely().isEncryptionActive()) {
            final byte[] buf = PDFText.toUTF16(text);
            return PDFText.escapeByteArray(
                getDocument().getEncryption().encrypt(buf, this));
        } else {
            return encode(PDFText.escapeText(text, false));
        }
    }
    
    /**
     * Encodes a String (3.2.3 in PDF 1.4 specs)
     * @param string the string to encode
     * @return byte[] the encoded string
     */
    protected byte[] encodeString(String string) {
        return encodeText(string);
        /*
        final byte[] buf = encode(PDFText.escapeString(string));
        if (getDocumentSafely().isEncryptionActive()) {
            return PDFText.escapeByteArray(
                getDocument().getEncryption().encrypt(buf, this));
        } else {
            return buf;
        }*/
    }
    
    /**
     * Formats an object for serialization to PDF.
     * @param obj the object
     * @param out the OutputStream to write to
     * @param writer a Writer for text content (will always be a wrapper around the above
     *                  OutputStream. Make sure <code>flush</code> is called when mixing calls)
     * @throws IOException If an I/O error occurs
     */
    protected void formatObject(Object obj, OutputStream out, Writer writer) throws IOException {
        if (obj == null) {
            writer.write("null");
        } else if (obj instanceof PDFWritable) {
            ((PDFWritable)obj).outputInline(out, writer);
        } else if (obj instanceof Number) {
            if (obj instanceof Double || obj instanceof Float) {
                writer.write(PDFNumber.doubleOut(((Number)obj).doubleValue()));
            } else {
                writer.write(obj.toString());
            }
        } else if (obj instanceof Boolean) {
            writer.write(obj.toString());
        } else {
            writer.flush();
            out.write(encodeText(obj.toString()));
        }
    }
    
    /** Formatting pattern for PDF date */
    protected static final SimpleDateFormat DATE_FORMAT 
            = new SimpleDateFormat("'D:'yyyyMMddHHmmss");

    /**
     * Formats a date/time according to the PDF specification 
     * (D:YYYYMMDDHHmmSSOHH'mm').
     * @param time date/time value to format
     * @return the requested String representation
     */
    protected String formatDateTime(Date time) {
        StringBuffer sb = new StringBuffer();
        sb.append(DATE_FORMAT.format(time));
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        
        int era = cal.get(Calendar.ERA);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int milliseconds = cal.get(Calendar.HOUR_OF_DAY) * 1000 * 60 * 60;
        milliseconds += cal.get(Calendar.MINUTE) * 1000 * 60;
        milliseconds += cal.get(Calendar.SECOND) * 1000;
        milliseconds += cal.get(Calendar.MILLISECOND);
        
        int offset = tz.getOffset(era, year, month, day, dayOfWeek, milliseconds);
        if (offset == 0) {
            sb.append('Z');
        } else {
            if (offset > 0) {
                sb.append('+');
            } else {
                sb.append('-');
            }
            final int HOUR = (1000 * 60 * 60);
            int offsetHour = Math.abs(offset / HOUR);
            int offsetMinutes = (offset - (offsetHour * HOUR)) / (1000 * 60);
            if (offsetHour < 10) {
                sb.append('0');
            }
            sb.append(Integer.toString(offsetHour));
            sb.append('\'');
            if (offsetMinutes < 10) {
                sb.append('0');
            }
            sb.append(Integer.toString(offsetMinutes));
            sb.append('\'');
        }
        return sb.toString();
    }

}
