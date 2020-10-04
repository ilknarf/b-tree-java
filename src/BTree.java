public class BTree {
    BTreeNode root;

    BTree(int order) {
        assert(order > 1);
        root = new BTreeNode(order);
    }

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

    public boolean contains(int val) {
        var loc = root.find(val);

        return loc.node != null;
    }

    public void delete(int val) {
        var loc = root.find(val);

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
        return "[" + root.toString() + "]";
    }

    public static void main(String[] args) {
        var b = new BTree(3);

        System.out.println(b);

        b.insert(5);
        System.out.println(b);

        b.insert(3);
        System.out.println(b);

        b.insert(2);
        System.out.println(b);

        b.insert(3);
        System.out.println(b);

        b.delete(3);
        System.out.println(b);

        b.delete(3);
        System.out.println(b);

        System.out.println(b.contains(2));
        System.out.println(b.contains(4));
    }
}

class BTreeNode {
    protected final int minSize;
    protected int childrenCount;

    protected BTreeNode parent;
    protected int[] vals;
    protected int size;
    protected BTreeNode[] keys;
    protected int parentIndex;

    BTreeNode(int order) {
        vals = new int[order]; // cap + 1 to allow for insertion before split

        size = 0;
        keys = new BTreeNode[order + 1];

        childrenCount = 0;
        parentIndex = -1;

        minSize = (order - 1) / 2;
    }

    BTreeNode(int order, boolean isNewRoot) {
        this(order);

        if (isNewRoot) {
            keys = new BTreeNode[order + 1];
        }
    }

    BTreeNode(int order, int[] vals, int size) {
        assert (vals.length == order);

        this.vals = vals;
        this.size = size;

        keys = new BTreeNode[order + 1];
        childrenCount = 0;
        parentIndex = -1;

        minSize = (order - 1) / 2;
    }

    BTreeNode(int order, int[] vals, BTreeNode[] children, int size) {
        this(order, vals, size);
        this.keys = children;

        for (int i = 0; i < size + 1; i++) {
            var child = this.keys[i];
            if (child != null) {
                child.setParent(this);
                child.parentIndex = i;

                childrenCount++;
            }
        }
    }

    void setParent(BTreeNode parent) {
        this.parent = parent;
    }

    // use binary search to find insertion point
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

    BTreeNode insert(int val) {
        assert isLeaf();

        return insert(val, null, null);
    }

    protected BTreeNode insert(int val, BTreeNode left, BTreeNode right) {
        int index;

        if (left != null) {
            index = left.parentIndex;
        } else if (right != null){
            index = right.parentIndex - 1;
        } else {
            index = bisect(val);
        }

        if (keys[index] != null) {
            childrenCount--;
        }

        // make space for new value and key
        int lengthToCopy = size - index;
        if (size - index > 0) {
            System.arraycopy(vals, index, vals, index + 1, lengthToCopy);
            if (keys != null) {
                System.arraycopy(keys, index, keys, index + 1, lengthToCopy + 1);
            }
        }

        size++;
        vals[index] = val;
        keys[index] = left;
        keys[index + 1] = right;

        if (left != null) {
            left.setParent(this);
            left.parentIndex = index;
            childrenCount++;
        }
        if (right != null) {
            right.setParent(this);
            right.parentIndex = index + 1;
            childrenCount++;
        }

        if (size == vals.length) {
            int median = size / 2;

            var leftVals = new int[size];
            var rightVals = new int[size];
            int leftLength = median;
            int rightLength = size - median - 1;

            System.arraycopy(vals, 0, leftVals, 0, leftLength);
            System.arraycopy(vals, median + 1, rightVals, 0, rightLength);

            var leftChildren = new BTreeNode[keys.length];
            var rightChildren = new BTreeNode[keys.length];

            System.arraycopy(keys, 0, leftChildren, 0, leftLength + 1);
            System.arraycopy(keys, median + 1, rightChildren, 0, rightLength + 1);

            // build new split child nodes
            var newLeft = new BTreeNode(size, leftVals, leftChildren, leftLength);
            newLeft.parentIndex = this.parentIndex;
            var newRight = new BTreeNode(size, rightVals, rightChildren, rightLength);
            newLeft.parentIndex = this.parentIndex + 1;

            if (parent == null) {
                parent = new BTreeNode(size, true);
                parent.insert(vals[median], newLeft, newRight);
                return parent;
            }

            // insert new split index into parent;
            return parent.insert(vals[median], newLeft, newRight);
        }

        return null;
    }

    boolean isLeaf() {
        return childrenCount == 0;
    }

    BTreeNode getChild(int val) {
        int index = bisect(val);
        return keys[index];
    }

    Location find(int val) {
        int index = bisect(val);
        if (index < size) {
            if (vals[index] == val) {
                return new Location(index, this);
            }
        }
        if (keys[index] != null) {
            return keys[index].find(val);
        } else {
            return new Location(-1, null);
        }
    }

    // merges with sibling and separator
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
                key.parent = this;
                key.parentIndex = size;
            }
            size++;
        }

        var key = sibKeys[sibSize];
        keys[sibSize] = key;
        if (key != null) {
            key.parent = this;
            key.parentIndex = sibSize;
        }
    }

    void delete(int index) {
        assert(size > index);

        if(isLeaf()) {
            System.arraycopy(vals, index + 1, vals, index, vals.length - index - 1);
            size--;

            // if undersized and non-root
            if (size < minSize && parent != null) {
                rebalance();

                // if parent empty, return child as new root or replace
                if (parent.size == 0) {
                    var oldParent = parent;
                    parent = oldParent.parent;
                    if (parent != null) {
                        parent.keys[oldParent.parentIndex] = this;
                    }
                }
            }
        } else {
            var right = keys[index + 1];

            if (right.size > right.minSize) {
                vals[index] = right.vals[0];
                right.delete(0);
            } else {
                var left = keys[index];
                vals[index] = left.vals[left.size - 1];
                left.delete(left.size - 1);
            }

            if (size < minSize && parent != null) {
                rebalance();
            }
        }
    }

    void rebalance() {
        if (parentIndex != 0) {
            var leftAdjacent = parent.keys[parentIndex - 1];

            if (leftAdjacent.size > minSize) {
                insert(parent.vals[parentIndex]);
                parent.vals[parentIndex] = leftAdjacent.vals[size - 1];
                leftAdjacent.delete(size - 1);
            }
        }

        var rightAdjacent = parent.keys[parentIndex + 1];
        if (rightAdjacent.size > minSize) {
            insert(parent.vals[parentIndex]);
            parent.vals[parentIndex] = rightAdjacent.vals[0];

            rightAdjacent.delete(0);
        } else {
            merge(rightAdjacent, parent.vals[parentIndex]);
            System.arraycopy(parent.vals, parentIndex + 2, parent.vals, parentIndex + 1, parent.vals.length - parentIndex - 2);
            System.arraycopy(parent.keys, parentIndex + 2, parent.keys, parentIndex + 1, parent.keys.length - parentIndex - 2);

            parent.size--;
        }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append('[');

        for (int i = 0; i < size; i++) {
            sb.append(" (");
            if (keys != null && keys[i] != null) {
                sb.append(keys[i].toString());
            }
            sb.append(") ");

            sb.append(vals[i]);
        }

        sb.append(" (");
        if (size > 0 && keys != null && keys[size] != null) {
            sb.append(keys[size].toString());
        }
        sb.append(") ");

        sb.append(']');
        return sb.toString();
    }
}

class Location {
    int index;
    BTreeNode node;

    Location(int index, BTreeNode node) {
        this.index = index;
        this.node = node;
    }
}