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

package org.apache.fop.fo.properties;

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;

/**
 * Store all common hyphenation properties.
 * See Sec. 7.9 of the XSL-FO Standard.
 * Public "structure" allows direct member access.
 */
public final class CommonHyphenation {
    
    private static final PropertyCache cache = new PropertyCache();
    
    private int hash = 0;
    
    /** The "language" property */
    public final StringProperty language;

    /** The "country" property */
    public final StringProperty country;

    /** The "script" property */
    public final StringProperty script;

    /** The "hyphenate" property */
    public final EnumProperty hyphenate;

    /** The "hyphenation-character" property */
    public final CharacterProperty hyphenationCharacter;

    /** The "hyphenation-push-character-count" property */
    public final NumberProperty hyphenationPushCharacterCount;

    /** The "hyphenation-remain-character-count" property*/
    public final NumberProperty hyphenationRemainCharacterCount;

    /**
     * Construct a CommonHyphenation object holding the given properties
     * 
     */
    private CommonHyphenation(StringProperty language,
                              StringProperty country,
                              StringProperty script,
                              EnumProperty hyphenate,
                              CharacterProperty hyphenationCharacter,
                              NumberProperty hyphenationPushCharacterCount,
                              NumberProperty hyphenationRemainCharacterCount) {
        this.language = language;
        this.country = country;
        this.script = script;
        this.hyphenate = hyphenate;
        this.hyphenationCharacter = hyphenationCharacter;
        this.hyphenationPushCharacterCount = hyphenationPushCharacterCount;
        this.hyphenationRemainCharacterCount = hyphenationRemainCharacterCount;
    }
    
    /**
     * Gets the canonical <code>CommonHyphenation</code> instance corresponding
     * to the values of the related properties present on the given 
     * <code>PropertyList</code>
     * 
     * @param propertyList  the <code>PropertyList</code>
     */
    public static CommonHyphenation getInstance(PropertyList propertyList) throws PropertyException {
        StringProperty language = 
            (StringProperty) propertyList.get(Constants.PR_LANGUAGE);
        StringProperty country = 
            (StringProperty) propertyList.get(Constants.PR_COUNTRY);
        StringProperty script = 
            (StringProperty) propertyList.get(Constants.PR_SCRIPT);
        EnumProperty hyphenate = 
            (EnumProperty) propertyList.get(Constants.PR_HYPHENATE);
        CharacterProperty hyphenationCharacter = 
            (CharacterProperty) propertyList.get(Constants.PR_HYPHENATION_CHARACTER);
        NumberProperty hyphenationPushCharacterCount = 
            (NumberProperty) propertyList.get(Constants.PR_HYPHENATION_PUSH_CHARACTER_COUNT);
        NumberProperty hyphenationRemainCharacterCount = 
            (NumberProperty) propertyList.get(Constants.PR_HYPHENATION_REMAIN_CHARACTER_COUNT);
        
        CommonHyphenation instance = new CommonHyphenation(
                                language, 
                                country, 
                                script, 
                                hyphenate, 
                                hyphenationCharacter, 
                                hyphenationPushCharacterCount, 
                                hyphenationRemainCharacterCount);
        
        return cache.fetch(instance);
        
    }
    
    /** {@inheritDoc */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CommonHyphenation) {
            CommonHyphenation ch = (CommonHyphenation) obj;
            return (ch.language == this.language
                    && ch.country == this.country
                    && ch.script == this.script
                    && ch.hyphenate == this.hyphenate
                    && ch.hyphenationCharacter == this.hyphenationCharacter
                    && ch.hyphenationPushCharacterCount == this.hyphenationPushCharacterCount
                    && ch.hyphenationRemainCharacterCount == this.hyphenationRemainCharacterCount);
        }
        return false;
    }
    
    /** {@inheritDoc} */
    public int hashCode() {
        if (hash == 0) {
            int hash = 17;
            hash = 37 * hash + (language == null ? 0 : language.hashCode());
            hash = 37 * hash + (script == null ? 0 : script.hashCode());
            hash = 37 * hash + (country == null ? 0 : country.hashCode());
            hash = 37 * hash + (hyphenate == null ? 0 : hyphenate.hashCode());
            hash = 37 * hash + 
                (hyphenationCharacter == null ? 0 : hyphenationCharacter.hashCode());
            hash = 37 * hash + 
                (hyphenationPushCharacterCount == null ? 0 : hyphenationPushCharacterCount.hashCode());
            hash = 37 * hash + 
                (hyphenationRemainCharacterCount == null ? 0 : hyphenationRemainCharacterCount.hashCode());
        }
        return hash;
    }
    
}
