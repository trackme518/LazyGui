package toolbox.windows;

import com.jogamp.newt.event.KeyEvent;
import processing.core.PGraphics;
import processing.core.PVector;
import toolbox.GlobalState;
import toolbox.MathUtils;
import toolbox.tree.Node;
import toolbox.Palette;

import static processing.core.PConstants.*;

public class TreeWindow extends Window {
    PVector contentTranslateOrigin = new PVector(0, cell);
    PVector contentTranslate = contentTranslateOrigin.copy();
    PVector treeDrawingOrigin = new PVector(cell, cell);
    private boolean isDraggedInside = false;
    Node root;

    public TreeWindow(Node node, PVector pos, PVector size, boolean closeable) {
        super(node, pos, size, closeable);
        root = node; // I could just use the node variable but root is clearer, it's a special node

        // TODO make more than 1 recursive level translation work
        // TODO FOLDERS!

//        Node hello = new Node(app, "/hello/", "hello");
//        hello.children.add(new Node(app, "/hello/world/", "world"));
//        root.children.add(hello);

/*
*
        root.children.add(new Node("/button/", "test", BUTTON));
        root.children.add(new Node("/toggle/", "test", TOGGLE));
        root.children.add(new Node("/stroke/", "stroke", COLOR));
        root.children.add(new Node("/count/", "count", SLIDER_INT_X));
        root.children.add(new Node("/speed/", "speed", SLIDER_X));
        root.children.add(new Node("/position/", "position", PLOT_XY));
        root.children.add(new Node("/rotate/", "rotate", PLOT_XY_SLIDER_Z));
        root.children.add(new Node("/background gradient/", "background gradient", GRADIENT));


* */
    }

    public void tryRegisterNode(Node node) {
        if (findNodeByPath(root, node.path) != null) {
            return;
        }
        // TODO add to lower levels when path indicates it
        root.children.add(node);
    }

    protected void drawContent(PGraphics pg) {
        pg.pushMatrix();
        pg.translate(contentTranslate.x, contentTranslate.y);
        drawGrid(pg);
        drawTree(pg);
        pg.popMatrix();
    }

    private void drawTree(PGraphics pg) {
        pg.pushMatrix();
        pg.translate(treeDrawingOrigin.x, treeDrawingOrigin.y);
        drawTreeNodeRecursively(pg, root);
        pg.popMatrix();
    }

    private void drawTreeNodeRecursively(PGraphics pg, Node parent) {
        pg.stroke(Palette.treeWindowContentStroke);
        if (parent.children.size() > 0) {
            pg.strokeWeight(1);
            pg.line(cell / 2f, cell * 1.25f, cell / 2f, cell * 0.75f + parent.children.size() * cell);
        }
        pg.strokeWeight(1);
        updateDrawNodeHitbox(pg, parent, true);

        if (WindowManager.isHidden(parent.path) || parent.path.equals(WindowManager.treeWindow.node.path)) {
            pg.fill(Palette.standardTextFill);
        } else {
            pg.fill(Palette.selectedTextFill);
        }
        pg.textAlign(LEFT, TOP);
        pg.noStroke();
        pg.text(parent.name, 5, GlobalState.textOffsetY);

        if (parent.children.size() > 0) {
            pg.pushMatrix();
            pg.translate(cell, 0);
            for (Node child : parent.children) {
                pg.translate(0, cell);
                drawTreeNodeRecursively(pg, child);
            }
            pg.popMatrix();
        }
    }

    private void updateDrawNodeHitbox(PGraphics pg, Node parent, boolean hidden) {
        float paddingX = cell / 2f;
        float x = -paddingX / 2f;
        float y = 0;
        float w = pg.textWidth(parent.name) + paddingX;
        float h = cell;
        parent.screenPos.x = windowPos.x + pg.screenX(x, y);
        parent.screenPos.y = windowPos.y + pg.screenY(x, y);
        parent.screenSize.x = w;
        parent.screenSize.y = h;
        if (!hidden) {
            pg.fill(Palette.windowContentFill);
            pg.stroke(Palette.windowBorderStroke);
            pg.rect(x, y, w, h);
        }
    }

    public void debugHitboxes(PGraphics pg, Node parent) {
        pg.noFill();
        pg.stroke(0.6f, 1, 1);
        pg.rect(parent.screenPos.x, parent.screenPos.y, parent.screenSize.x, parent.screenSize.y);
        for (Node child : parent.children) {
            debugHitboxes(pg, child);
        }
    }

    private void tryInteractWithTree(float x, float y) {
        Node hitboxMatch = tryFindHitboxUnderPointRecursively(root, x, y);
        if (hitboxMatch != null) {
            //noinspection ConstantConditions
            WindowManager.registerOrUncoverWindow(WindowFactory.createWindowFromNode(hitboxMatch));
        }
    }

    private Node tryFindHitboxUnderPointRecursively(Node parent, float x, float y) {
        if (MathUtils.isPointInRect(x, y, parent.screenPos.x, parent.screenPos.y, parent.screenSize.x, parent.screenSize.y)) {
            return parent;
        }
        for (Node child : parent.children) {
            Node potentialHit = tryFindHitboxUnderPointRecursively(child, x, y);
            if (potentialHit != null) {
                return potentialHit;
            }
        }
        return null;
    }

    public Node findNodeByPathInTree(String pathQuery){
        return findNodeByPath(root, pathQuery);
    }

    private Node findNodeByPath(Node parent, String pathQuery) {
        if (parent.path.equals(pathQuery)) {
            return parent;
        }
        for (Node child : parent.children) {
            Node potentialMatch = findNodeByPath(child, pathQuery);
            if (potentialMatch != null) {
                return child;
            }
        }
        return null;
    }

    @Override
    protected void reactToMouseDraggedInsideWithoutDrawing(float x, float y, float px, float py) {
        super.reactToMouseDraggedInsideWithoutDrawing(x, y, x, y);
        contentTranslate.x += x - px;
        contentTranslate.y += y - py;
    }

    @Override
    protected void reactToMouseReleasedInsideWithoutDrawing(float x, float y) {
        super.reactToMouseReleasedInsideWithoutDrawing(x, y);
        tryInteractWithTree(x, y);
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        super.keyPressed(keyEvent);
        if (hidden) {
            return;
        }
        if (keyEvent.getKeyChar() == 'r') {
            contentTranslate = contentTranslateOrigin.copy();
        }
    }
}
