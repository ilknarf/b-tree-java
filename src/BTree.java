/**
 * The BTree program implements a basic non-concurrent B-Tree using nodes.
 * Sample inserts and deletes are given in the main method. Should be the
 * main point of interface for users, as the insertion and deletion logic
 * is handled behind-the-scenes.
 *
 * @author Frank Li
 * @version 1.0
 * @since 2020-04-10
 */
public class BTree {
    BTreeNode root;

    /**
     * Initializes a new BTree object of a provided order;
     *
     * @param order the order of the BTree object. Must be
     *              greater than 2.
     */
    BTree(int order) {
        assert(order > 2);
        root = new BTreeNode(order);
    }

    /**
     * Inserts a value `val` into the BTree.
     *
     * @param val the value to be inserted
     */
    public void insert(int val) {
        var curr = root;

        while (!curr.isLeaf()) {
            curr = curr.getChild(val);
        }

        // insert and set new root if added
        var newRoot = curr.insert(val);

        if (newRoot != null) {
            root = newRoot;
        }
    }

    /**
     * Checks if a value is contained within the B-Tree.
     *
     * @param val the value to be checked
     * @return returns true if the value is in the B-Tree, false otherwise.
     */
    public boolean contains(int val) {
        var loc = root.find(val);

        return loc != null;
    }

    /**
     * Deletes an occurrence of a value if if exists in the B-Tree
     *
     * @param val the value to be deleted
     */
    public void delete(int val) {
        var loc = root.find(val);

        if (loc == null) {
            return;
        }

        var node = loc.node;
        var index = loc.index;
        if (node != null) {
            node.delete(index);

            // drop current root if empty and non-leaf
            if (root.size == 0) {
                if (!root.isLeaf()) {
                    root = root.keys[0];
                }
            }
        }
    }

    @Override
    public String toString() {
        return root.toString();
    }

    public static void main(String[] args) {
        var b = new BTree(3);
        final int length = 10000;

        var vals = new int[length];

        System.out.println("begin insertion");
        for (int i = 0; i < length; i++) {
            int val = (int) (Math.random() * 1000);
            b.insert(val);
            System.out.println("inserting " + val);
            vals[i] = val;
        }
        System.out.println("finished insertion");

        int counter = 0;
        System.out.println("begin missing check:");
        for (int i = 0; i < length; i++) {
            if (!b.contains(vals[i])) {
                System.out.println(vals[i] + " missing");
                counter++;
            }
        }
        System.out.println("finished missing check: missing " + counter + " items");

        System.out.println("begin deleting");
        for (int i = 0; i < length; i++) {
            b.delete(vals[i]);
        }
        System.out.println("finished deleting");
        System.out.println(b);

        System.out.println("done");
    }
}

/**
 * Class used to represent a block in a B-Tree. Holds many of the insertion and deletion logic,
 * and should never be directly manipulated by a user.
 */
class BTreeNode {
    protected final int minSize;
    protected boolean isLeaf;

    protected BTreeNode parent;
    protected int[] vals;
    protected int size;
    protected BTreeNode[] keys;
    protected int parentIndex;

    /**
     * Initializes a BTreeNode of a given order.
     *
     * @param order the order of the B-Tree
     */
    BTreeNode(int order) {
        vals = new int[order]; // cap + 1 to allow for insertion before split

        size = 0;
        keys = new BTreeNode[order + 1];

        isLeaf = true;
        parentIndex = 0;

        minSize = (order - 1) / 2;
    }

    /**
     * Initializes a BTreeNode given a previous array of values.
     *
     * @param order the order of the B-Tree
     * @param vals an existing array of values
     * @param size the size (current capacity) of the array
     */
    BTreeNode(int order, int[] vals, int size) {
        assert (vals.length == order);

        this.vals = vals;
        this.size = size;

        keys = new BTreeNode[order + 1];
        isLeaf = true;
        parentIndex = 0;

        minSize = (order - 1) / 2;
    }

    /**
     * Initializes a BTreeNode given a previous array of values and a previous array of keys.
     *
     * @param order the order of the B-Tree
     * @param vals an existing array of values
     * @param children an existing array of keys
     * @param size the size (current capacity) of both arrays
     */
    BTreeNode(int order, int[] vals, BTreeNode[] children, int size) {
        this(order, vals, size);
        this.keys = children;

        for (int i = 0; i < size + 1; i++) {
            var child = this.keys[i];
            if (child != null) {
                child.setParent(this);
                child.parentIndex = i;
                isLeaf = false;
            }
        }
    }

    /**
     * Sets the parent node to a new BTreeNode.
     *
     * @param parent the new parent
     */
    void setParent(BTreeNode parent) {
        this.parent = parent;
    }

    /**
     * Finds the hypothetical insertion point of a new value.
     * @param val the value to insert
     * @return returns a valid insertion point for a new value.
     */
    int bisect(int val) {
        int lo = 0;
        int hi = size;

        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (vals[mid] < val) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }

        return lo;
    }

    /**
     * Inserts a new value into the BTreeNode, and restructures if necessary.
     *
     * @param val the value to insert
     * @return BTreeNode instance if new root is created, null otherwise.
     */
    BTreeNode insert(int val) {
        assert(isLeaf());

        int index = bisect(val);
        int lengthToCopy = size - index;
        if(lengthToCopy > 0) {
            System.arraycopy(vals, index, vals, index + 1, lengthToCopy);
        }

        size++;
        vals[index] = val;

        if (size == vals.length) {
            return split();
        }

        return null;
    }

    /**
     * Inserts a new value (with its left and right children) into the BTreeNode,
     * and restructures if necessary.
     *
     * @param val the value to insert
     * @param left the left side children
     * @param right the right side children
     * @return returns BTreeNode instance if new root inserted, otherwise null.
     */
    protected BTreeNode insert(int val, BTreeNode left, BTreeNode right) {
        int index;

        index = left.parentIndex;
        isLeaf = false;

        // make space for new value and key
        int lengthToCopy = size - index;
        size++;

        if (lengthToCopy > 0) {
            System.arraycopy(vals, index, vals, index + 1, lengthToCopy);
            if (keys != null) {
                System.arraycopy(keys, index + 1, keys, index + 2, lengthToCopy);
            }

            for (int i = index + 2; i < size + 1; i++) {
                keys[i].parentIndex++;
            }
        }

        vals[index] = val;
        keys[index] = left;
        keys[index + 1] = right;

        left.setParent(this);
        left.parentIndex = index;

        right.setParent(this);
        right.parentIndex = index + 1;


        if (size == vals.length) {
            return split();
        }

        return null;
    }

    /**
     * Splits a BTreeNode and inserts into parent.
     * @return returns insertion results in the case of a new parent
     */
    protected BTreeNode split() {
        int median = size / 2;

        var leftVals = new int[size];
        int leftLength = median;
        System.arraycopy(vals, 0, leftVals, 0, leftLength);

        var rightVals = new int[size];
        int rightLength = size - median - 1;
        System.arraycopy(vals, median + 1, rightVals, 0, rightLength);

        BTreeNode newLeft;
        BTreeNode newRight;

        if (!isLeaf()) {
            var leftChildren = new BTreeNode[keys.length];
            System.arraycopy(keys, 0, leftChildren, 0, leftLength + 1);

            var rightChildren = new BTreeNode[keys.length];
            System.arraycopy(keys, median + 1, rightChildren, 0, rightLength + 1);

            // build new split child nodes
            newLeft = new BTreeNode(size, leftVals, leftChildren, leftLength);
            newRight = new BTreeNode(size, rightVals, rightChildren, rightLength);
        } else {
            newLeft = new BTreeNode(size, leftVals, leftLength);
            newRight = new BTreeNode(size, rightVals, rightLength);
        }

        newLeft.parentIndex = parentIndex;
        newRight.parentIndex = parentIndex + 1;

        if (parent == null) {
            setParent(new BTreeNode(vals.length));
            parent.insert(vals[median], newLeft, newRight);

            return parent;
        }

        // insert new split index into parent;

        return parent.insert(vals[median], newLeft, newRight);
    }

    /**
     * Checks whether BTreeNode instance is a leaf.
     *
     * @return returns true if a leaf, otherwise false.
     */
    boolean isLeaf() {
        return isLeaf;
    }

    /**
     * Given a value, returns a valid child node value for insertion.
     *
     * @param val the value to insert.
     * @return a valid child for insertion of val.
     */
    BTreeNode getChild(int val) {
        int index = bisect(val);
        return keys[index];
    }

    /**
     * Find the node and index of a given value, if it exists.
     *
     * @param val the value to find.
     * @return returns a Location object upon finding value. returns null upon exhausting search.
     */
    Location find(int val) {
        int index = bisect(val);

        if (index < size && vals[index] == val) {
            return new Location(index, this);
        } else if (keys[index] != null) {
            return keys[index].find(val);
        }

        return null;
    }

    /**
     * Merges node with a separator and another node (presumably right side neighbor).
     *
     * @param sibling the sibling to merge into the node.
     * @param sep the separator to add to the node.
     */
    protected void merge(BTreeNode sibling, int sep) {
        // asserts valid merge scenario
        assert(minSize == size && sibling.size < minSize);

        vals[size] = sep;
        size++;

        var sibVals = sibling.vals;
        var sibKeys = sibling.keys;
        int sibSize = sibling.size;

        for (int i = 0; i < sibSize; i++) {
            var key = sibKeys[i];

            vals[size] = sibVals[i];
            keys[size] = key;
            if (key != null) {
                key.setParent(this);
                key.parentIndex = size;
                isLeaf = false;
            }
            size++;
        }

        var key = sibKeys[sibSize];
        keys[size] = key;

        if (key != null) {
            key.setParent(this);
            key.parentIndex = size;
        }
    }

    /**
     * Deletes the value at an index in the B-Tree, then restructures if needed.
     *
     * @param index the index at which to delete.
     */
    void delete(int index) {
        assert(size > index);

        if (isLeaf()) {
            System.arraycopy(vals, index + 1, vals, index, vals.length - index - 1);
            size--;
        } else {
            var right = findSmallestGtChild(index).node;

            if (right.size > right.minSize) {
                vals[index] = right.vals[0];
                right.delete(0);
            } else {
                var left = findLargestLtChild(index).node;
                vals[index] = left.vals[left.size - 1];
                left.delete(left.size - 1);
            }
        }

        // if undersized and non-root
        if (size < minSize && parent != null) {
            rebalance();
        }
    }

    /**
     * Finds the greatest smaller child of the value at the index.
     * @param index The index to find the greatest smaller child for.
     * @return returns a Location object corresponding with the smallest item on the left child
     *         of the value at the index.
     */
    Location findLargestLtChild(int index) {
        assert(index < size);

        if(isLeaf()) {
            return new Location(size - 1, this);
        }

        var left = keys[index];

        while(!left.isLeaf()) {
            left = left.keys[left.size];
        }

        return new Location(left.size - 1, left);
    }

    /**
     * Finds the smallest greater child of the value at the index.
     * @param index The index to find the smallest greater child for.
     * @return returns a Location object corresponding with the smallest item on the right child
     *         of the value at index.
     */
    Location findSmallestGtChild(int index) {
        assert(index < size);

        if (isLeaf()) {
            return new Location(0, this);
        }

        var right = keys[index + 1];


        while(!right.isLeaf()) {
            right = right.keys[0];
        }

        return new Location (0, right);
    }

    /**
     * Rebalances node when below the minimum size. Follows B-Tree rebalancing procedures to ensure a
     * well-formed tree.
     */
    void rebalance() {
        assert (size < minSize);

        if (size == 0 && !isLeaf()) {
            return;
        }

        if (parentIndex != 0) {
            var leftAdjacent = parent.keys[parentIndex - 1];
            var largestLeft = parent.findLargestLtChild(parentIndex - 1).node;

            if (!leftAdjacent.isLeaf() || leftAdjacent.size > minSize) {
                insert(parent.vals[parentIndex - 1]);

                parent.vals[parentIndex - 1] = largestLeft.vals[largestLeft.size - 1];
                largestLeft.delete(largestLeft.size - 1);
            } else {
                leftAdjacent.mergeAndDelete();
            }
        } else {
            var rightAdjacent = parent.keys[parentIndex + 1];
            var rightMinimum = parent.findSmallestGtChild(parentIndex).node;

            if (!rightAdjacent.isLeaf() || rightAdjacent.size > minSize) {
                insert(parent.vals[parentIndex]);

                parent.vals[parentIndex] = rightMinimum.vals[0];
                rightMinimum.delete(0);
            } else {
                mergeAndDelete();
            }
        }
    }

    /**
     * Merges with right neighbor and separator, then deletes from parent.
     */
    protected void mergeAndDelete() {
        var rightAdjacent = parent.keys[parentIndex + 1];
        merge(rightAdjacent, parent.vals[parentIndex]);

        // delete right adjacent value and keys
        System.arraycopy(parent.vals, parentIndex + 1, parent.vals, parentIndex, parent.vals.length - parentIndex - 1);
        System.arraycopy(parent.keys, parentIndex + 2, parent.keys, parentIndex + 1, parent.keys.length - parentIndex - 2);

        parent.size--;
        parent.keys[parent.size + 1] = null;
        for (int i = parentIndex + 1; i < parent.size + 1; i++) {
            parent.keys[i].parentIndex = i;
        }

        if (parent.size == 0) {
            var oldParent = parent;
            setParent(oldParent.parent);

            if (parent != null) {
                parent.keys[oldParent.parentIndex] = this;
                parentIndex = oldParent.parentIndex;
            }
        }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append('[');

        for (int i = 0; i < size; i++) {
            sb.append(" (");
            if (keys[i] != null) {
                sb.append(keys[i].toString());
            }
            sb.append(") ");

            sb.append(vals[i]);
        }

        sb.append(" (");
        if (size > 0 && keys[size] != null) {
            sb.append(keys[size].toString());
        }
        sb.append(") ");

        sb.append(']');
        return sb.toString();
    }
}

/**
 * Class used to represent the location of a value in a B-Tree
 */
class Location {
    int index;
    BTreeNode node;

    /**
     * Initializes a Location object with a given index and node.
     * @param index the index of the value
     * @param node the node of the value
     */
    Location(int index, BTreeNode node) {
        this.index = index;
        this.node = node;
    }
}