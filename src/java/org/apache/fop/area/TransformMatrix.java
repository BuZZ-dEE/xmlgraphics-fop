/*
 * $Id$
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 *
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 *
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */
package org.apache.fop.area;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import org.apache.fop.fo.properties.WritingMode;

/**
 * Describe a PDF or PostScript style coordinate transformation matrix (TransformMatrix).
 * The matrix encodes translations, scaling and rotations of the coordinate
 * system used to render pages.
 */
public class TransformMatrix implements Serializable {

    private double a, b, c, d, e, f;

    private static final TransformMatrix CTM_LRTB =
        new TransformMatrix(1, 0, 0, 1, 0, 0);
    private static final TransformMatrix CTM_RLTB =
        new TransformMatrix(-1, 0, 0, 1, 0, 0);
    private static final TransformMatrix CTM_TBRL =
        new TransformMatrix(0, 1, -1, 0, 0, 0);

    /**
     * Create the identity matrix
     */
    public TransformMatrix() {
        a = 1;
        b = 0;
        c = 0;
        d = 1;
        e = 0;
        f = 0;
    }

    /**
     * Initialize a TransformMatrix from the passed arguments.
     *
     * @param a the x scale
     * @param b the x shear
     * @param c the y shear
     * @param d the y scale
     * @param e the x shift
     * @param f the y shift
     */
    public TransformMatrix(
            double a, double b, double c, double d, double e, double f) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
        this.f = f;
    }

    /**
     * Initialize a TransformMatrix to the identity matrix with a translation
     * specified by x and y
     *
     * @param x the x shift
     * @param y the y shift.
     */
    public TransformMatrix(double x, double y) {
        this.a = 1;
        this.b = 0;
        this.c = 0;
        this.d = 1;
        this.e = x;
        this.f = y;
    }

    /**
     * Initialize a TransformMatrix with the values of another TransformMatrix.
     *
     * @param ctm another TransformMatrix
     */
    protected TransformMatrix(TransformMatrix ctm) {
        this.a = ctm.a;
        this.b = ctm.b;
        this.c = ctm.c;
        this.d = ctm.d;
        this.e = ctm.e;
        this.f = ctm.f;
    }

    /**
     * Return a TransformMatrix which will transform coordinates for a
     * particular writing-mode into normalized first quandrant coordinates.
     * @param wm A writing mode constant from fo.properties.WritingMode, ie.
     * one of LR_TB, RL_TB, TB_RL.
     * @param ipd The inline-progression dimension of the reference area whose
     * TransformMatrix is being set..
     * @param bpd The block-progression dimension of the reference area whose
     * TransformMatrix is being set.
     * @return a new TransformMatrix with the required transform
     */
    public static TransformMatrix getWMctm(int wm, int ipd, int bpd) {
        TransformMatrix wmctm;
        switch (wm) {
            case WritingMode.LR_TB:
                return new TransformMatrix(CTM_LRTB);
            case WritingMode.RL_TB: {
                    wmctm = new TransformMatrix(CTM_RLTB);
                    wmctm.e = ipd;
                    return wmctm;
                }
                //return  CTM_RLTB.translate(ipd, 0);
            case WritingMode.TB_RL: { // CJK
                    wmctm = new TransformMatrix(CTM_TBRL);
                    wmctm.e = bpd;
                    return wmctm;
                }
                //return CTM_TBRL.translate(0, ipd);
            default:
                return null;
        }
    }

    /**
     * Multiply new passed TransformMatrix with this one and generate a new
     * result TransformMatrix.
     * @param premult The TransformMatrix to multiply with this one.
     *  The new one will be the first multiplicand.
     * @return TransformMatrix The result of multiplying premult * this.
     */
    public TransformMatrix multiply(TransformMatrix premult) {
        TransformMatrix rslt = 
            new TransformMatrix ((premult.a * a) + (premult.b * c),
                            (premult.a * b) + (premult.b * d),
                            (premult.c * a) + (premult.d * c),
                            (premult.c * b) + (premult.d * d),
                            (premult.e * a) + (premult.f * c) + e,
                            (premult.e * b) + (premult.f * d) + f);
        return rslt;
    }

    /**
     * Rotate this TransformMatrix by "angle" radians and return a new result
     * TransformMatrix.  This is used to account for reference-orientation.
     * @param angle The angle in radians.
     * Positive angles are measured counter-clockwise.
     * @return TransformMatrix The result of rotating this TransformMatrix.
     */
    public TransformMatrix rotate(double angle) {
        double cos, sin;
        if (angle == 90.0) {
            cos = 0.0;
            sin = 1.0;
        } else if (angle == 270.0) {
            cos = 0.0;
            sin = -1.0;
        } else if (angle == 180.0) {
            cos = -1.0;
            sin = 0.0;
        } else {
            double rad = Math.toRadians(angle);
            cos = Math.cos(rad);
            sin = Math.sin(rad);
        }
        TransformMatrix rotate = new TransformMatrix(cos, -sin, sin, cos, 0, 0);
        return multiply(rotate);
    }

    /**
     * Translate this TransformMatrix by the passed x and y values and return
     * a new result TransformMatrix.
     * @param x The amount to translate along the x axis.
     * @param y The amount to translate along the y axis.
     * @return TransformMatrix The result of translating this TransformMatrix.
     */
    public TransformMatrix translate(double x, double y) {
        TransformMatrix translate = new TransformMatrix(1, 0, 0, 1, x, y);
        return multiply(translate);
    }

    /**
     * Scale this TransformMatrix by the passed x and y values and return
     * a new result TransformMatrix.
     * @param x The amount to scale along the x axis.
     * @param y The amount to scale along the y axis.
     * @return TransformMatrix The result of scaling this TransformMatrix.
     */
    public TransformMatrix scale(double x, double y) {
        TransformMatrix scale = new TransformMatrix(x, 0, 0, y, 0, 0);
        return multiply(scale);
    }

    /**
     * Transform a rectangle by the TransformMatrix to produce a rectangle in
     * the transformed coordinate system.
     * @param inRect The rectangle in the original coordinate system
     * @return Rectangle2D The rectangle in the transformed coordinate system.
     */
    public Rectangle2D transform(Rectangle2D inRect) {
        // Store as 2 sets of 2 points and transform those, then
        // recalculate the width and height
        int x1t = (int)(inRect.getX() * a + inRect.getY() * c + e);
        int y1t = (int)(inRect.getX() * b + inRect.getY() * d + f);
        int x2t = (int)((inRect.getX() + inRect.getWidth()) * a
                        + (inRect.getY() + inRect.getHeight()) * c + e);
        int y2t = (int)((inRect.getX() + inRect.getWidth()) * b
                        + (inRect.getY() + inRect.getHeight()) * d + f);
        // Normalize with x1 < x2
        if (x1t > x2t) {
            int tmp = x2t;
            x2t = x1t;
            x1t = tmp;
        }
        if (y1t > y2t) {
            int tmp = y2t;
            y2t = y1t;
            y1t = tmp;
        }
        return new Rectangle(x1t, y1t, x2t - x1t, y2t - y1t);
    }

    /**
     * Get string for this transform.
     *
     * @return a string with the transform values
     */
    public String toString() {
        return "[" + a + " " + b + " " + c + " " + d + " " + e + " "
               + f + "]";
    }

    /**
     * Get an array containing the values of this transform.
     * This creates and returns a new transform with the values in it.
     *
     * @return an array containing the transform values
     */
    public double[] toArray() {
        return new double[]{a, b, c, d, e, f};
    }

    /**
     * @param absRefOrient
     * @param writingMode
     * @param absVPrect
     * @return
     */
    /**
     * Construct a coordinate transformation matrix.
     * @param absVPrect absolute viewpoint rectangle
     * @param relBPDim the relative block progression dimension
     * @param relIPDim the relative inline progression dimension
     * @return TransformMatrix the coordinate transformation matrix
     */
    public static TransformMatrix getMatrixandRelDims(int absRefOrient,
                                       int writingMode,
                                       Rectangle2D absVPrect,
                                       int relBPDim, int relIPDim) {
        int width, height;
        // We will use the absolute reference-orientation to set up the
        // TransformMatrix.
        // The value here is relative to its ancestor reference area.
        if (absRefOrient % 180 == 0) {
            width = (int) absVPrect.getWidth();
            height = (int) absVPrect.getHeight();
        } else {
            // invert width and height since top left are rotated by 90
            // (cl or ccl)
            height = (int) absVPrect.getWidth();
            width = (int) absVPrect.getHeight();
        }
        /* Set up the TransformMatrix for the content of this reference area.
         * This will transform region content coordinates in
         * writing-mode relative into absolute page-relative
         * which will then be translated based on the position of
         * the region viewport.
         * (Note: scrolling between region vp and ref area when
         * doing online content!)
         */
        TransformMatrix ctm = 
            new TransformMatrix(absVPrect.getX(), absVPrect.getY());

        // First transform for rotation
        if (absRefOrient != 0) {
            // Rotation implies translation to keep the drawing area in the
            // first quadrant. Note: rotation is counter-clockwise
            switch (absRefOrient) {
                case 90:
                    ctm = ctm.translate(0, width); // width = absVPrect.height
                    break;
                case 180:
                    ctm = ctm.translate(width, height);
                    break;
                case 270:
                    ctm = ctm.translate(height, 0); // height = absVPrect.width
                    break;
            }
            ctm = ctm.rotate(absRefOrient);
        }
        /* Since we've already put adjusted width and height values for the
         * top and left positions implied by the reference-orientation, we
         * can set ipd and bpd appropriately based on the writing mode.
         */

        if (writingMode == WritingMode.LR_TB
                || writingMode == WritingMode.RL_TB) {
            relIPDim = width;
            relBPDim = height;
        } else {
            relIPDim = height;
            relBPDim = width;
        }
        // Set a rectangle to be the writing-mode relative version???
        // Now transform for writing mode
        return ctm.multiply(
                TransformMatrix.getWMctm(writingMode, relIPDim, relBPDim));
    }

}
