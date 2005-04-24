/*
 * Copyright 2004-2005 The Apache Software Foundation.
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
package org.apache.fop.layoutmgr;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOText;
import org.apache.fop.fo.FObjMixed;
import org.apache.fop.fo.flow.BasicLink;
import org.apache.fop.fo.flow.BidiOverride;
import org.apache.fop.fo.flow.Block;
import org.apache.fop.fo.flow.BlockContainer;
import org.apache.fop.fo.flow.Character;
import org.apache.fop.fo.flow.ExternalGraphic;
import org.apache.fop.fo.flow.Footnote;
import org.apache.fop.fo.flow.Inline;
import org.apache.fop.fo.flow.InlineLevel;
import org.apache.fop.fo.flow.InlineContainer;
import org.apache.fop.fo.flow.InstreamForeignObject;
import org.apache.fop.fo.flow.Leader;
import org.apache.fop.fo.flow.ListBlock;
import org.apache.fop.fo.flow.ListItem;
import org.apache.fop.fo.flow.PageNumber;
import org.apache.fop.fo.flow.PageNumberCitation;
import org.apache.fop.fo.flow.RetrieveMarker;
import org.apache.fop.fo.flow.Table;
import org.apache.fop.fo.flow.TableBody;
import org.apache.fop.fo.flow.TableCell;
import org.apache.fop.fo.flow.TableColumn;
import org.apache.fop.fo.flow.TableFooter;
import org.apache.fop.fo.flow.TableHeader;
import org.apache.fop.fo.flow.TableRow;
import org.apache.fop.fo.flow.Wrapper;
import org.apache.fop.fo.pagination.Flow;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.StaticContent;
import org.apache.fop.fo.pagination.Title;

import org.apache.fop.layoutmgr.list.ListBlockLayoutManager;
import org.apache.fop.layoutmgr.list.ListItemLayoutManager;
/*import org.apache.fop.layoutmgr.table.Body;
import org.apache.fop.layoutmgr.table.Cell;
import org.apache.fop.layoutmgr.table.Column;
import org.apache.fop.layoutmgr.table.Row;*/
import org.apache.fop.layoutmgr.table.TableLayoutManager;

/**
 * The default LayoutManager maker class
 */
public class LayoutManagerMapping implements LayoutManagerMaker {

    /** logging instance */
    protected static Log log = LogFactory.getLog(LayoutManagerMapping.class);

    /** The map of LayoutManagerMakers */
    private Map makers = new HashMap();

    public LayoutManagerMapping() {
        initialize();
    }

    /**
     * Initializes the set of maker objects associated with this LayoutManagerMapping
     */
    protected void initialize() {
        makers.put(FOText.class, new FOTextLayoutManagerMaker());
        makers.put(FObjMixed.class, new Maker());
        makers.put(BidiOverride.class, new BidiOverrideLayoutManagerMaker());
        makers.put(Inline.class, new InlineLayoutManagerMaker());
        makers.put(Footnote.class, new FootnodeLayoutManagerMaker());
        makers.put(InlineContainer.class,
                   new InlineContainerLayoutManagerMaker());
        makers.put(BasicLink.class, new BasicLinkLayoutManagerMaker());
        makers.put(Block.class, new BlockLayoutManagerMaker());
        makers.put(Leader.class, new LeaderLayoutManagerMaker());
        makers.put(RetrieveMarker.class, new WrapperLayoutManagerMaker());
        makers.put(Character.class, new CharacterLayoutManagerMaker());
        makers.put(ExternalGraphic.class,
                   new ExternalGraphicLayoutManagerMaker());
        makers.put(BlockContainer.class,
                   new BlockContainerLayoutManagerMaker());
        makers.put(ListItem.class, new ListItemLayoutManagerMaker());
        makers.put(ListBlock.class, new ListBlockLayoutManagerMaker());
        makers.put(InstreamForeignObject.class,
                   new InstreamForeignObjectLayoutManagerMaker());
        makers.put(PageNumber.class, new PageNumberLayoutManagerMaker());
        makers.put(PageNumberCitation.class,
                   new PageNumberCitationLayoutManagerMaker());
        makers.put(PageSequence.class, new PageSequenceLayoutManagerMaker());
        makers.put(Table.class, new TableLayoutManagerMaker());
        makers.put(TableBody.class, new /*TableBodyLayoutManager*/Maker());
        makers.put(TableColumn.class, new /*TableColumnLayoutManager*/Maker());
        makers.put(TableRow.class, new /*TableRowLayoutManager*/Maker());
        makers.put(TableCell.class, new /*TableCellLayoutManager*/Maker());
        makers.put(TableFooter.class, new /*TableBodyLayoutManager*/Maker());
        makers.put(TableHeader.class, new /*TableBodyLayoutManager*/Maker());
        makers.put(Flow.class, new FlowLayoutManagerMaker());
        makers.put(StaticContent.class, new StaticContentLayoutManagerMaker());
        makers.put(Wrapper.class, new WrapperLayoutManagerMaker());
        makers.put(Title.class, new InlineLayoutManagerMaker());
    }

    /**
     * @see org.apache.fop.layoutmgr.LayoutManagerMaker#makeLayoutManagers(FONode, List)
     */
    public void makeLayoutManagers(FONode node, List lms) {
        Maker maker = (Maker) makers.get(node.getClass());
        if (maker == null) {
            log.error("No LayoutManager maker for class " + node.getClass());
        } else {
            maker.make(node, lms);
        }
    }

    /**
     * @see org.apache.fop.layoutmgr.LayoutManagerMaker#makeLayoutManager(FONode)
     */
    public LayoutManager makeLayoutManager(FONode node) {
        List lms = new ArrayList();
        makeLayoutManagers(node, lms);
        if (lms.size() == 0) {
            throw new IllegalStateException("LayoutManager for class "
                                   + node.getClass()
                                   + " is missing.");
        } else if (lms.size() > 1) {
            throw new IllegalStateException("Duplicate LayoutManagers for class "
                                   + node.getClass()
                                   + " found, only one may be declared."); 
        }
        return (LayoutManager) lms.get(0);
    }

    public static class Maker {
        public void make(FONode node, List lms) {
            // no layout manager
            return;
        }
    }

    public static class FOTextLayoutManagerMaker extends Maker {
        public void make(FONode node, List lms) {
            FOText foText = (FOText) node;
            if (foText.endIndex - foText.startIndex > 0) {
                lms.add(new TextLayoutManager(foText));
            }
        }
    }

    /*
    public static class FObjMixedLayoutManagerMaker extends Maker {
        public void make(FONode node, List lms) {
            if (node.getChildNodes() != null) {
                InlineStackingLayoutManager lm;
                lm = new InlineStackingLayoutManager((FObjMixed) node);
                lms.add(lm);
            }
        }       
    }
    */

    public static class BidiOverrideLayoutManagerMaker extends Maker {
        // public static class BidiOverrideLayoutManagerMaker extends FObjMixedLayoutManagerMaker {
        public void make(BidiOverride node, List lms) {
            if (false) {
                // this is broken; it does nothing
                // it should make something like an InlineStackingLM
                super.make(node, lms);
            } else {
                ArrayList childList = new ArrayList();
                // this is broken; it does nothing
                // it should make something like an InlineStackingLM
                super.make(node, childList);
                for (int count = childList.size() - 1; count >= 0; count--) {
                    LayoutManager lm = (LayoutManager) childList.get(count);
                    if (lm.generatesInlineAreas()) {
                        LayoutManager blm = new BidiLayoutManager
                            (node, (InlineLayoutManager) lm);
                        lms.add(blm);
                    } else {
                        lms.add(lm);
                    }
                }
            }
        }
    }

    public static class InlineLayoutManagerMaker extends Maker {
         public void make(FONode node, List lms) {
             if (node.getChildNodes() != null) {
                 lms.add(new InlineLayoutManager((InlineLevel) node));
             }
         }
    }

    public static class FootnodeLayoutManagerMaker extends Maker {
        public void make(FONode node, List lms) {
            Inline citation = ((Footnote) node).getInlineFO();
            if (citation != null) {
                lms.add(new InlineLayoutManager(citation));
            }
        }
    }

    public static class InlineContainerLayoutManagerMaker extends Maker {
        public void make(FONode node, List lms) {
            ArrayList childList = new ArrayList();
            super.make(node, childList);
            lms.add(new ICLayoutManager((InlineContainer) node, childList));
        }
    }

    public static class BasicLinkLayoutManagerMaker extends Maker {
        public void make(FONode node, List lms) {
            lms.add(new BasicLinkLayoutManager((BasicLink) node));
        }
    }

    public static class BlockLayoutManagerMaker extends Maker {
         public void make(FONode node, List lms) {
             lms.add(new BlockLayoutManager((Block) node));
         }
    }

    public static class LeaderLayoutManagerMaker extends Maker {
        public void make(FONode node, List lms) {
            lms.add(new LeaderLayoutManager((Leader) node));
        }
    }

    public static class CharacterLayoutManagerMaker extends Maker {
        public void make(FONode node, List lms) {
            lms.add(new CharacterLayoutManager((Character) node));
        }
    }

    public static class ExternalGraphicLayoutManagerMaker extends Maker {
        public void make(FONode node, List lms) {
            ExternalGraphic eg = (ExternalGraphic) node;
            if (!eg.getSrc().equals("")) {
                lms.add(new ExternalGraphicLayoutManager(eg));
            }
        }
    }

    public static class BlockContainerLayoutManagerMaker extends Maker {
        public void make(FONode node, List lms) {
            lms.add(new BlockContainerLayoutManager((BlockContainer) node));
         }
    }

    public static class ListItemLayoutManagerMaker extends Maker {
         public void make(FONode node, List lms) {
             lms.add(new ListItemLayoutManager((ListItem) node));
         }      
    }

    public static class ListBlockLayoutManagerMaker extends Maker {
        public void make(FONode node, List lms) {
            lms.add(new ListBlockLayoutManager((ListBlock) node));
        }
    }

    public static class InstreamForeignObjectLayoutManagerMaker extends Maker {
        public void make(FONode node, List lms) {
            lms.add(new InstreamForeignObjectLM((InstreamForeignObject) node));
        }
    }

    public static class PageNumberLayoutManagerMaker extends Maker {
         public void make(FONode node, List lms) {
             lms.add(new PageNumberLayoutManager((PageNumber) node));
         }
    }

    public static class PageNumberCitationLayoutManagerMaker extends Maker {
         public void make(FONode node, List lms) {
            lms.add(new PageNumberCitationLayoutManager((PageNumberCitation) node));
         }
    }

    public static class PageSequenceLayoutManagerMaker extends Maker {
         public void make(FONode node, List lms) {
             lms.add(new PageSequenceLayoutManager((PageSequence) node));
         }
    }

    public static class TableLayoutManagerMaker extends Maker {
        
        /*
        private List getColumnLayoutManagerList(Table table, TableLayoutManager tlm) {
            List columnLMs = null;
            List columns = table.getColumns();
            if (columns != null) {
                columnLMs = new java.util.ArrayList();
                int colnum = 1;
                ListIterator iter = columns.listIterator();
                while (iter.hasNext()) {
                    TableColumn col = (TableColumn)iter.next();
                    if (col.hasColumnNumber()) {
                        colnum = col.getColumnNumber();
                    }
                    for (int i = 0; i < col.getNumberColumnsRepeated(); i++) {
                        while (colnum > columnLMs.size()) {
                            columnLMs.add(null);
                        }
                        Column colLM = new Column(col);
                        colLM.setParent(tlm);
                        columnLMs.set(colnum - 1, colLM);
                        colnum++;
                    }
                }
                //Post-processing the list (looking for gaps)
                int pos = 1;
                ListIterator ppIter = columnLMs.listIterator();
                while (ppIter.hasNext()) {
                    Column col = (Column)ppIter.next();
                    if (col == null) {
                        log.error("Found a gap in the table-columns at position " + pos);
                    }
                    pos++;
                }
            }
            return columnLMs;
        }*/
        
        public void make(FONode node, List lms) {
            Table table = (Table) node;
            TableLayoutManager tlm = new TableLayoutManager(table);
            /*
            List columnLMs = getColumnLayoutManagerList(table, tlm);
            if (columnLMs != null) {
                tlm.setColumns(columnLMs);
            }*/
            lms.add(tlm);
        }
    }
     
    /*
    public static class TableBodyLayoutManagerMaker extends Maker {
         public void make(FONode node, List lms) {
             lms.add(new Body((TableBody) node));
         }

    }
    
    public static class TableColumnLayoutManagerMaker extends Maker {
         public void make(FONode node, List lms) {
             lms.add(new Column((TableColumn) node));
        }       
    }

    public static class TableRowLayoutManagerMaker extends Maker {
        public void make(FONode node, List lms) {
            lms.add(new Row((TableRow) node));
        }
    }

    public static class TableCellLayoutManagerMaker extends Maker {
        public void make(FONode node, List lms) {
            lms.add(new Cell((TableCell) node));
        }
    }*/

    public static class FlowLayoutManagerMaker extends Maker {
         public void make(FONode node, List lms) {
             lms.add(new FlowLayoutManager((Flow) node));
         }
    }

    public static class StaticContentLayoutManagerMaker extends Maker {
         public void make(FONode node, List lms) {
             lms.add(new StaticContentLayoutManager((StaticContent) node));
         }
    }

    public class WrapperLayoutManagerMaker extends Maker {
        public void make(FONode node, List lms) {
            Iterator baseIter;
            baseIter = node.getChildNodes();
            if (baseIter == null) {
                return;
            }
            while (baseIter.hasNext()) {
                FONode child = (FONode) baseIter.next();
                makeLayoutManagers(child, lms);
            }
        }       
    }

}
