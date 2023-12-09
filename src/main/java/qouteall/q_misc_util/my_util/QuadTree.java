package qouteall.q_misc_util.my_util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public class QuadTree<T> {
    public static final int MAX_LEVEL = 8;
    
    // the node 0 is root node
    // for node n, its --, -+, +-, ++ children are respectively at index 4n, 4n+1, 4n+2, 4n+3
    // -1 means no child
    public final IntArrayList children = new IntArrayList();
    
    // the data of node n is at index n
    public final ObjectArrayList<T> elements = new ObjectArrayList<>();
    
    public final Supplier<T> elementFactory;
    
    public QuadTree(Supplier<T> elementFactory) {
        this.elementFactory = elementFactory;
        allocateNode(); // allocate the root node
    }
    
    private int allocateNode() {
        Validate.isTrue(elements.size() * 4 == children.size());
        int index = elements.size();
        elements.add(elementFactory.get());
        children.add(-1);
        children.add(-1);
        children.add(-1);
        children.add(-1);
        return index;
    }
    
    public int acquireNodeForBoundingBox(
        double nodeRelativeMinX, double nodeRelativeMinY,
        double nodeRelativeMaxX, double nodeRelativeMaxY
    ) {
        return acquireNodeForBoundingBoxInternal(
            0, 0,
            nodeRelativeMinX, nodeRelativeMinY, nodeRelativeMaxX, nodeRelativeMaxY
        );
    }
    
    public T acquireElementForBoundingBox(
        double nodeRelativeMinX, double nodeRelativeMinY,
        double nodeRelativeMaxX, double nodeRelativeMaxY
    ) {
        int node = acquireNodeForBoundingBox(
            nodeRelativeMinX, nodeRelativeMinY, nodeRelativeMaxX, nodeRelativeMaxY
        );
        return elements.get(node);
    }
    
    private int acquireNodeForBoundingBoxInternal(
        int level, int startingNode,
        double nodeRelativeMinX, double nodeRelativeMinY,
        double nodeRelativeMaxX, double nodeRelativeMaxY
    ) {
        if (level >= MAX_LEVEL) {
            return startingNode;
        }
        
        boolean minXSig = nodeRelativeMinX >= 0;
        boolean minYSig = nodeRelativeMinY >= 0;
        boolean maxXSig = nodeRelativeMaxX >= 0;
        boolean maxYSig = nodeRelativeMaxY >= 0;
        
        if (minXSig == maxXSig && minYSig == maxYSig) {
            int childNodeIndex = startingNode * 4 + (minXSig ? 2 : 0) + (minYSig ? 1 : 0);
            int childNode = children.getInt(childNodeIndex);
            if (childNode == -1) {
                childNode = allocateNode();
                children.set(childNodeIndex, childNode);
            }
            double childNodeCenterX = minXSig ? 0.5 : -0.5;
            double childNodeCenterY = minYSig ? 0.5 : -0.5;
            return acquireNodeForBoundingBoxInternal(
                level + 1, childNode,
                (nodeRelativeMinX - childNodeCenterX) * 2,
                (nodeRelativeMinY - childNodeCenterY) * 2,
                (nodeRelativeMaxX - childNodeCenterX) * 2,
                (nodeRelativeMaxY - childNodeCenterY) * 2
            );
        }
        
        return startingNode;
    }
    
    /**
     * @param bbMinX        the minimum x of the bounding box
     * @param bbMinY        the minimum y of the bounding box
     * @param bbMaxX        the maximum x of the bounding box
     * @param bbMaxY        the maximum y of the bounding box
     * @param searchingFunc the function that takes the element and returns the result.
     *                      if the result is not null, the searching will stop and return the result.
     *                      if the result is null, the searching will continue.
     * @param <U>           the type of the result
     * @return the searching result, null if not found
     */
    public <U> @Nullable U traverse(
        double bbMinX, double bbMinY, double bbMaxX, double bbMaxY,
        Function<T, U> searchingFunc
    ) {
        return traverseInternal(
            0, 0,
            bbMinX, bbMinY, bbMaxX, bbMaxY,
            searchingFunc
        );
    }
    
    private <U> @Nullable U traverseInternal(
        int level, int startingNode,
        double bbMinX, double bbMinY, double bbMaxX, double bbMaxY,
        Function<T, U> func
    ) {
        U r1 = func.apply(elements.get(startingNode));
        
        if (r1 != null) {
            return r1;
        }
        
        int bxMin = bbMinX > 0 ? 1 : 0;
        int byMin = bbMinY > 0 ? 1 : 0;
        int bxMax = bbMaxX > 0 ? 1 : 0;
        int byMax = bbMaxY > 0 ? 1 : 0;
        
        for (int bx = bxMin; bx <= bxMax; bx++) {
            for (int by = byMin; by <= byMax; by++) {
                int childNodeIndex = startingNode * 4 + bx * 2 + by;
                int childNode = children.getInt(childNodeIndex);
                if (childNode != -1) {
                    double childNodeCenterX = bx == 1 ? 0.5 : -0.5;
                    double childNodeCenterY = by == 1 ? 0.5 : -0.5;
                    U r2 = traverseInternal(
                        level + 1, childNode,
                        (bbMinX - childNodeCenterX) * 2,
                        (bbMinY - childNodeCenterY) * 2,
                        (bbMaxX - childNodeCenterX) * 2,
                        (bbMaxY - childNodeCenterY) * 2,
                        func
                    );
                    if (r2 != null) {
                        return r2;
                    }
                }
            }
        }
        
        return null;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        appendInfoToStringBuilder(0, 0, sb, "root");
        
        return sb.toString();
    }
    
    private void appendInfoToStringBuilder(
        int nodeIndex, int level, StringBuilder sb, String prefix
    ) {
        sb.append(" ".repeat(level * 2));
        sb.append(prefix);
        sb.append(" ");
        sb.append(nodeIndex);
        sb.append(" ");
        sb.append(elements.get(nodeIndex));
        sb.append("\n");
        
        int childIndex = nodeIndex * 4;
        for (int i = 0; i < 4; i++) {
            int child = children.getInt(childIndex + i);
            if (child != -1) {
                String childPrefix = switch (i) {
                    case 0 -> "--";
                    case 1 -> "-+";
                    case 2 -> "+-";
                    case 3 -> "++";
                    default -> throw new IllegalStateException("Unexpected value: " + i);
                };
                appendInfoToStringBuilder(child, level + 1, sb, childPrefix);
            }
        }
    }
}
