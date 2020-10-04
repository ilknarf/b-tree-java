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
    }
}

class BTreeNode {
    boolean isLeaf;

    BTreeNode parent;
    int[] vals;
    int size;
    BTreeNode[] keys;

    BTreeNode(int order) {
        vals = new int[order]; // cap + 1 to allow for insertion before split
        size = 0;
        keys = new BTreeNode[order + 1];
        isLeaf = true;
    }

    BTreeNode(int order, boolean isNewRoot) {
        this(order);

        if (isNewRoot) {
            keys = new BTreeNode[order + 1];
        }
        isLeaf = !isNewRoot;
    }

    BTreeNode(int order, int[] vals, int size) {
        assert (vals.length == order);
        this.vals = vals;
        this.size = size;
        keys = null;
        isLeaf = true;
    }

    BTreeNode(int order, int[] vals, BTreeNode[] children, int size, boolean isLeaf) {
        this(order, vals, size);
        this.keys = children;
        for (var child : this.keys) {
            if (child != null) {
                child.setParent(this);
            }
        }
        this.isLeaf = isLeaf;
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
        return insert(val, null, null);
    }

    BTreeNode insert(int val, BTreeNode left, BTreeNode right) {
        int index = bisect(val);
        // make space for new value and key
        int lengthToCopy = size - index;
        if (size - index > 0) {
            System.arraycopy(vals, index, vals, index + 1, lengthToCopy);
            if (keys != null) {
                System.arraycopy(keys, index, keys, index + 1, lengthToCopy + 1);
            }
        }

        vals[index] = val;
        size++;
        keys[index] = left;
        keys[index + 1] = right;

        if (left != null) {
            left.setParent(this);
        }
        if (right != null) {
            right.setParent(this);
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

            var newLeft = new BTreeNode(size, leftVals, leftChildren, leftLength, isLeaf);
            var newRight = new BTreeNode(size, rightVals, rightChildren, rightLength, isLeaf);

            if (parent == null) {
                parent = new BTreeNode(size, true);
                parent.insert(vals[median], newLeft, newRight);
                return parent;
            }

            return parent.insert(vals[median], newLeft, newRight);
        }

        return null;
    }

    boolean isLeaf() {
        return isLeaf;
    }

    BTreeNode getChild(int val) {
        int index = bisect(val);
        return keys[index];
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