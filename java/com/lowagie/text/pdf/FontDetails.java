/*
 * $Id: FontDetails.java,v 1.44 2005/05/04 14:32:23 blowagie Exp $
 * $Name:  $
 *
 * Copyright 2001, 2002 by Paulo Soares.
 *
 *
 * The Original Code is 'iText, a free JAVA-PDF library'.
 *
 * The Initial Developer of the Original Code is Bruno Lowagie. Portions created by
 * the Initial Developer are Copyright (C) 1999, 2000, 2001, 2002 by Bruno Lowagie.
 * All Rights Reserved.
 * Co-Developer of the code is Paulo Soares. Portions created by the Co-Developer
 * are Copyright (C) 2000, 2001, 2002 by Paulo Soares. All Rights Reserved.
 *
 * Contributor(s): all the names of the contributors are added in the source code
 * where applicable.
 *
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 *
 *
 * If you didn't download this code from the following link, you should check if
 * you aren't using an obsolete version:
 * http://www.lowagie.com/iText/
 */

package com.lowagie.text.pdf;

import com.lowagie.text.ExceptionConverter;
import java.util.HashMap;
import java.io.UnsupportedEncodingException;
/** Each font in the document will have an instance of this class
 * where the characters used will be represented.
 *
 * @author  Paulo Soares (psoares@consiste.pt)
 */
class FontDetails {
    
    /** The indirect reference to this font
     */    
    PdfIndirectReference indirectReference;
    /** The font name that appears in the document body stream
     */    
    PdfName fontName;
    /** The font
     */    
    BaseFont baseFont;
    /** The font if its an instance of <CODE>TrueTypeFontUnicode</CODE>
     */    
    TrueTypeFontUnicode ttu;

    CJKFont cjkFont;
    /** The array used with single byte encodings
     */    
    byte shortTag[];
    /** The map used with double byte encodings. The key is Integer(glyph) and the
     * value is int[]{glyph, width, Unicode code}
     */    
    HashMap longTag;
    
    IntHashtable cjkTag;
    /** The font type
     */    
    int fontType;
    /** <CODE>true</CODE> if the font is symbolic
     */    
    boolean symbolic;
    /** Indicates if all the glyphs and widths for that particular
     * encoding should be included in the document.
     */
    protected boolean subset = true;
    /** Each font used in a document has an instance of this class.
     * This class stores the characters used in the document and other
     * specifics unique to the current working document.
     * @param fontName the font name
     * @param indirectReference the indirect reference to the font
     * @param baseFont the <CODE>BaseFont</CODE>
     */
    FontDetails(PdfName fontName, PdfIndirectReference indirectReference, BaseFont baseFont) {
        this.fontName = fontName;
        this.indirectReference = indirectReference;
        this.baseFont = baseFont;
        fontType = baseFont.getFontType();
        switch (fontType) {
            case BaseFont.FONT_TYPE_T1:
            case BaseFont.FONT_TYPE_TT:
                shortTag = new byte[256];
                break;
            case BaseFont.FONT_TYPE_CJK:
                cjkTag = new IntHashtable();
                cjkFont = (CJKFont)baseFont;
                break;
            case BaseFont.FONT_TYPE_TTUNI:
                longTag = new HashMap();
                ttu = (TrueTypeFontUnicode)baseFont;
                symbolic = baseFont.isFontSpecific();
                break;
        }
    }
    
    /** Gets the indirect reference to this font.
     * @return the indirect reference to this font
     */    
    PdfIndirectReference getIndirectReference() {
        return indirectReference;
    }
    
    /** Gets the font name as it appears in the document body.
     * @return the font name
     */    
    PdfName getFontName() {
        return fontName;
    }
    
    /** Gets the <CODE>BaseFont</CODE> of this font.
     * @return the <CODE>BaseFont</CODE> of this font
     */    
    BaseFont getBaseFont() {
        return baseFont;
    }
    
    /** Converts the text into bytes to be placed in the document.
     * The conversion is done according to the font and the encoding and the characters
     * used are stored.
     * @param text the text to convert
     * @return the conversion
     */    
    byte[] convertToBytes(String text) {
        byte b[] = null;
        switch (fontType) {
            case BaseFont.FONT_TYPE_T3:
                return baseFont.convertToBytes(text);
            case BaseFont.FONT_TYPE_T1:
            case BaseFont.FONT_TYPE_TT: {
                b = baseFont.convertToBytes(text);
                int len = b.length;
                for (int k = 0; k < len; ++k)
                    shortTag[((int)b[k]) & 0xff] = 1;
                break;
            }
            case BaseFont.FONT_TYPE_CJK: {
                int len = text.length();
                for (int k = 0; k < len; ++k)
                    cjkTag.put(cjkFont.getCidCode(text.charAt(k)), 0);
                b = baseFont.convertToBytes(text);
                break;
            }
            case BaseFont.FONT_TYPE_DOCUMENT: {
                b = baseFont.convertToBytes(text);
                break;
            }
            case BaseFont.FONT_TYPE_TTUNI: {
                try {
                    int len = text.length();
                    int metrics[] = null;
                    char glyph[] = new char[len];
                    int i = 0;
                    if (symbolic) {
                        b = PdfEncodings.convertToBytes(text, "symboltt");
                        len = b.length;
                        for (int k = 0; k < len; ++k) {
                            metrics = ttu.getMetricsTT(b[k] & 0xff);
                            if (metrics == null)
                                continue;
                            longTag.put(new Integer(metrics[0]), new int[]{metrics[0], metrics[1], ttu.getUnicodeDifferences(b[k] & 0xff)});
                            glyph[i++] = (char)metrics[0];
                        }
                    }
                    else {
                        for (int k = 0; k < len; ++k) {
                            char c = text.charAt(k);
                            metrics = ttu.getMetricsTT(c);
                            if (metrics == null)
                                continue;
                            int m0 = metrics[0];
                            Integer gl = new Integer(m0);
                            if (!longTag.containsKey(gl))
                                longTag.put(gl, new int[]{m0, metrics[1], c});
                            glyph[i++] = (char)m0;
                        }
                    }
                    String s = new String(glyph, 0, i);
                    b = s.getBytes(CJKFont.CJK_ENCODING);
                }
                catch (UnsupportedEncodingException e) {
                    throw new ExceptionConverter(e);
                }
                break;
            }
        }
        return b;
    }
    
    /** Writes the font definition to the document.
     * @param writer the <CODE>PdfWriter</CODE> of this document
     */    
    void writeFont(PdfWriter writer) {
        try {
            switch (fontType) {
                case BaseFont.FONT_TYPE_T3:
                    baseFont.writeFont(writer, indirectReference, null);
                    break;
                case BaseFont.FONT_TYPE_T1:
                case BaseFont.FONT_TYPE_TT: {
                    int firstChar;
                    int lastChar;
                    if (subset) {
                        for (firstChar = 0; firstChar < 256; ++firstChar) {
                            if (shortTag[firstChar] != 0)
                                break;
                        }
                        for (lastChar = 255; lastChar >= firstChar; --lastChar) {
                            if (shortTag[lastChar] != 0)
                                break;
                        }
                        if (firstChar > 255) {
                            firstChar = 255;
                            lastChar = 255;
                        }
                    }
                    else {
                        for (int k = 0; k < shortTag.length; ++k)
                            shortTag[k] = 1;
                        firstChar = 0;
                        lastChar = shortTag.length - 1;
                    }
                    baseFont.writeFont(writer, indirectReference, new Object[]{new Integer(firstChar), new Integer(lastChar), shortTag});
                    break;
                }
                case BaseFont.FONT_TYPE_CJK:
                    baseFont.writeFont(writer, indirectReference, new Object[]{cjkTag});
                    break;
                case BaseFont.FONT_TYPE_TTUNI:
                    baseFont.writeFont(writer, indirectReference, new Object[]{longTag});
                    break;
            }
        }
        catch(Exception e) {
            throw new ExceptionConverter(e);
        }
    }
    
    /** Indicates if all the glyphs and widths for that particular
     * encoding should be included in the document.
     * @return <CODE>false</CODE> to include all the glyphs and widths.
     */
    public boolean isSubset() {
        return subset;
    }
    
    /** Indicates if all the glyphs and widths for that particular
     * encoding should be included in the document. Set to <CODE>false</CODE>
     * to include all.
     * @param subset new value of property subset
     */
    public void setSubset(boolean subset) {
        this.subset = subset;
    }
}
