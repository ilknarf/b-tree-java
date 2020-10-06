# b-tree-java

This is a B-tree implementation in Java. (It uses memory, but a real-world implementation
would use disk storage to take full advantage of the benefits of a B-tree).

This implementation uses no libraries (standard or otherwise) and
can be run by just cloning to your desktop.

This data structure ensures linear space and logarithmic search, insert, and deletes. The main
advantage of this data structure is that each node can be loaded as a block, and the multiple children
resulting in fewer block loads as compared to a binary tree. This makes it very well-suited
for disk storage, where large amounts of data are stored and i/o times are massive. This implementation
just sorts integers though lol.

Requires Java 10 or above to run, because I used `var` declarations, because why not?
