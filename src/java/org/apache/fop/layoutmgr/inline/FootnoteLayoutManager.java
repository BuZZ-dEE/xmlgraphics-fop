/*
 * Copyright 1999-2005 The Apache Software Foundation.
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

package org.apache.fop.layoutmgr.inline;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.apache.fop.fo.flow.Footnote;
import org.apache.fop.layoutmgr.AbstractLayoutManager;
import org.apache.fop.layoutmgr.FootnoteBodyLayoutManager;
import org.apache.fop.layoutmgr.KnuthElement;
import org.apache.fop.layoutmgr.KnuthSequence;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.Position;

public class FootnoteLayoutManager extends AbstractLayoutManager 
                                   implements InlineLevelLayoutManager {

    private Footnote footnote;
    private InlineStackingLayoutManager citationLM;
    private FootnoteBodyLayoutManager bodyLM;

    /**
     * Create a new footnote layout manager.
     * @param node footnote to create the layout manager for
     */
    public FootnoteLayoutManager(Footnote node) {
        super(node);
        footnote = node;

        // create an InlineStackingLM handling the fo:inline child of fo:footnote
        citationLM = new InlineLayoutManager(footnote.getFootnoteCitation());

        // create a FootnoteBodyLM handling the fo:footnote-body child of fo:footnote
        bodyLM = new FootnoteBodyLayoutManager(footnote.getFootnoteBody());
    }

    public LinkedList getNextKnuthElements(LayoutContext context,
                                           int alignment) {
        // this is the only method that must be implemented:
        // all other methods will never be called, as the returned elements
        // contain Positions created by the citationLM, so its methods will
        // be called instead

        // set the citationLM parent to be this LM's parent
        citationLM.setParent(getParent());
        bodyLM.setParent(this);

        // get Knuth elements representing the footnote citation
        LinkedList returnedList = new LinkedList();
        while (!citationLM.isFinished()) {
            LinkedList partialList = citationLM.getNextKnuthElements(context, alignment);
            if (partialList != null) {
                returnedList.addAll(partialList);
            }
        }
        if (returnedList.size() == 0) {
            //Inline part of the footnote is empty. Need to send back an auxiliary
            //zero-width, zero-height inline box so the footnote gets painted.
            returnedList.add(new KnuthInlineBox(0, 0, 0, 0, null, true));
        }
        setFinished(true);

        addAnchor(returnedList);

        return returnedList;
    }

    private void addAnchor(LinkedList citationList) {
        // find the last box in the sequence, and add a reference
        // to the FootnoteBodyLM
        KnuthInlineBox lastBox = null;
        ListIterator citationIterator = citationList.listIterator(citationList.size());
        while (citationIterator.hasPrevious() && lastBox == null) {
            Object obj = citationIterator.previous();
            if (obj instanceof KnuthElement) {
                KnuthElement element = (KnuthElement)obj;
                if (element instanceof KnuthInlineBox) {
                    lastBox = (KnuthInlineBox) element;
                }
            } else {
                KnuthSequence seq = (KnuthSequence)obj;
                ListIterator nestedIterator = seq.listIterator(seq.size());
                while (nestedIterator.hasPrevious() && lastBox == null) {
                    KnuthElement element = (KnuthElement)nestedIterator.previous();
                    if (element instanceof KnuthInlineBox) {
                        lastBox = (KnuthInlineBox) element;
                    }
                }
            }
        }
        if (lastBox != null) {
            lastBox.setFootnoteBodyLM(bodyLM);
        }
    }

    public List addALetterSpaceTo(List oldList) {
        log.warn("null implementation of addALetterSpaceTo() called!");
        return oldList;
    }

    public void getWordChars(StringBuffer sbChars, Position pos) {
        log.warn("null implementation of getWordChars() called!");
    }

    public void hyphenate(Position pos, HyphContext hc) {
        log.warn("null implementation of hyphenate called!");
    }

    public boolean applyChanges(List oldList) {
        log.warn("null implementation of applyChanges() called!");
        return false;
    }

    public LinkedList getChangedKnuthElements(List oldList,
                                              int alignment) {
        log.warn("null implementation of getChangeKnuthElement() called!");
        return null;
    }
}
