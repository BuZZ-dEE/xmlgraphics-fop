/*
 * TNode.java
 *
 * Created: Sat Oct 27 13:44:34 2001
 *
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 *
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 * @version $Revision$ $Name$
 */

package org.apache.fop.datastructs;

//import Tree;

/**
 * A testbed for <tt>Node</tt>.
 */
public class TNode extends Node {

    private Object content = null;

    public TNode (Tree tree) throws TreeException {
        super(tree);
    }

    public TNode(Tree tree, TNode parent, int index)
        throws TreeException {
        super(tree, parent, index);
    }

    public TNode(Tree tree, TNode parent) throws TreeException {
        super(tree, parent);
    }

    /**
     * @param tree    the enclosing <tt>Tree</tt> instance.  Needed to enable
     *                the call to the superclass constructor.
     * @param parent  The parent <tt>TNode</tt> of this TNode.  If null,
     *                this must be the root node.
     * @param content An object which is the actual content of this node;
     *                the contents of the TNode.
     */

    public TNode(Tree tree, TNode parent, Object content)
        throws TreeException {
        super(tree, parent);
        this.content = content;
    }

    /**
     * @param tree    the enclosing <tt>Tree</tt> instance.  Needed to enable
     *                the call to the superclass constructor.
     * @param parent  The parent <tt>TNode</tt> of this TNode.  If null,
     *                this must be the root node.
     * @param index   int index of this child in the parent node.
     * @param content An object which is the actual content of this node;
     *                the contents of the TNode.
     */

    public TNode(Tree tree, TNode parent, int index, Object content)
        throws TreeException, IndexOutOfBoundsException {
        super(tree, parent, index);
        this.content = content;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public void unsetContent() {
        this.content = null;
    }


}// TNode
