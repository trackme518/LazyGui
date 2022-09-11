package lazy.windows.nodes.saves;

import processing.core.PGraphics;
import lazy.global.State;
import lazy.windows.nodes.AbstractNode;
import lazy.windows.nodes.NodeFolder;
import lazy.windows.nodes.NodeType;
import processing.core.PImage;

class SaveNode extends AbstractNode {
    String fileName, fullPath;

    public SaveNode(String path, NodeFolder parent, String fileName, String fullPath) {
        super(NodeType.TRANSIENT, path, parent);
        this.fileName = fileName;
        this.fullPath = fullPath;
    }

    protected void updateDrawInlineNodeInner(PGraphics pg) {
        fillForegroundBasedOnMouseOver(pg);
        drawRightText(pg, "load");
    }

    public void mousePressedOverNode(float x, float y) {
        State.loadStateFromFile(fileName);
    }
}
