/*
 * Copyright 2005 The Apache Software Foundation.
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

package org.apache.fop.layoutmgr.table;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.fop.fo.flow.Table;
import org.apache.fop.fo.flow.TableCell;
import org.apache.fop.fo.flow.TableColumn;

/**
 * Class holding a number of columns making up the column setup of a row.
 */
public class ColumnSetup {

    /** Logger **/
    private static Log log = LogFactory.getLog(ColumnSetup.class);

    private Table table;
    private List columns = new java.util.ArrayList();
    private int maxColIndexReferenced = 0;
    
    public ColumnSetup(Table table) {
        this.table = table;
        prepareExplicitColumns();
        if (getColumnCount() == 0) {
            createColumnsFromFirstRow();
        }
    }
    
    private void prepareExplicitColumns() {
        List rawCols = table.getColumns();
        if (rawCols != null) {
            int colnum = 1;
            ListIterator iter = rawCols.listIterator();
            while (iter.hasNext()) {
                TableColumn col = (TableColumn)iter.next();
                if (col.hasColumnNumber()) {
                    colnum = col.getColumnNumber();
                }
                for (int i = 0; i < col.getNumberColumnsRepeated(); i++) {
                    while (colnum > columns.size()) {
                        columns.add(null);
                    }
                    columns.set(colnum - 1, col);
                    colnum++;
                }
            }
            //Post-processing the list (looking for gaps)
            int pos = 1;
            ListIterator ppIter = columns.listIterator();
            while (ppIter.hasNext()) {
                TableColumn col = (TableColumn)ppIter.next();
                if (col == null) {
                    log.error("Found a gap in the table-columns at position " + pos);
                }
                pos++;
            }
        }
    }

    public TableColumn getColumn(int index) {
        int size = columns.size();
        if (index > size - 1) {
            maxColIndexReferenced = index;
            return (TableColumn)columns.get(size - 1);
        } else {
            return (TableColumn)columns.get(index - 1);
        }
    }
 
    /** @see java.lang.Object#toString() */
    public String toString() {
        return columns.toString();
    }

    /** @return the number of columns in the setup. */
    public int getColumnCount() {
        if (maxColIndexReferenced > columns.size()) {
            return maxColIndexReferenced;
        } else {
            return columns.size();
        }
   }
    
    /** @return an Iterator over all columns */
    public Iterator iterator() {
        return this.columns.iterator();
    }
    
    private void createColumnsFromFirstRow() {
        //TODO Create oldColumns from first row here 
        //--> rule 2 in "fixed table layout", see CSS2, 17.5.2
        //Alternative: extend oldColumns on-the-fly, but in this case we need the
        //new property evaluation context so proportional-column-width() works
        //correctly.
        if (columns.size() == 0) {
            this.columns.add(table.getDefaultColumn());
        }
    }

    /**
     * @param col column index (1 is first column)
     * @return the X offset of the requested column
     */
    public int getXOffset(int col) {
        int xoffset = 0;
        for (int i = 1; i < col; i++) {
            xoffset += getColumn(i).getColumnWidth().getValue();
        }
        return xoffset;
    }

}
