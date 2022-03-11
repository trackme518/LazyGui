package toolbox;

import com.jogamp.newt.event.KeyEvent;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import toolbox.global.palettes.PaletteStore;
import toolbox.global.State;
import toolbox.global.NodeTree;
import toolbox.windows.nodes.*;
import toolbox.windows.nodes.colorPicker.Color;
import toolbox.windows.nodes.colorPicker.ColorPickerFolderNode;
import toolbox.userInput.UserInputPublisher;
import toolbox.userInput.UserInputSubscriber;
import toolbox.windows.FolderWindow;
import toolbox.windows.WindowManager;
import toolbox.windows.nodes.gradient.GradientFolderNode;
import toolbox.windows.nodes.imagePicker.ImagePickerFolderNode;

import static processing.core.PApplet.*;

@SuppressWarnings("unused")
public class Gui implements UserInputSubscriber {
    public static boolean isGuiHidden = false;
    public PGraphics pg;
    PApplet app;
    boolean lastRecordNow = false;
    String recordingFolderName = "";
    boolean isRecording = false;
    PVector recordingPosInput = new PVector(0, 0);
    PVector recordingSizeInput = new PVector(1000, 1000);
    private int recordingFrame = 1;

    public Gui(PApplet p, boolean isGuiVisibleByDefault) {
        isGuiHidden = !isGuiVisibleByDefault;
        new Gui(p);
    }

    public Gui(PApplet p) {
        this.app = p;
        if(!app.sketchRenderer().equals(P2D) && !app.sketchRenderer().equals(P3D)){
            println("The Toolbox library requires the P2D or P3D renderer");
        }
        State.init(this, app);
        State.loadMostRecentSave();
        PaletteStore.initSingleton();
        UserInputPublisher.createSingleton();
        UserInputPublisher.subscribe(this);
        WindowManager.createSingleton();
        float cell = State.cell;
        FolderWindow rootFolder = new FolderWindow(
                new PVector(cell, cell),
                NodeTree.getRoot(),
                false
        );
        rootFolder.createStateListFolderNode();
        WindowManager.addWindow(rootFolder);
        lazyFollowSketchResolution();
    }

    void lazyFollowSketchResolution() {
        if (pg == null || pg.width != app.width || pg.height != app.height) {
            pg = app.createGraphics(app.width, app.height, P2D);
            pg.noSmooth();
        }
    }

    public void draw() {
        draw(State.app.g);
    }

    public void draw(PGraphics canvas) {
        lazyFollowSketchResolution();
        pg.beginDraw();
        pg.colorMode(HSB, 1, 1, 1, 1);
        pg.clear();
        if (!isGuiHidden) {
            WindowManager.updateAndDrawWindows(pg);
        }
        pg.endDraw();
        resetMatrixInAnyRenderer();
        canvas.pushStyle();
        canvas.imageMode(CORNER);
        canvas.image(pg, 0, 0);
        canvas.popStyle();
    }

    private void resetMatrixInAnyRenderer() {
        if (State.app.sketchRenderer().equals(P3D)) {
            State.app.camera();
        } else {
            State.app.resetMatrix();
        }
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.isAutoRepeat()) {
            return;
        }
        if (keyEvent.getKeyChar() == 'h') {
            isGuiHidden = !isGuiHidden;
        }
        if(keyEvent.getKeyChar() == 'p'){
            PaletteStore.setNextPalette();
        }
    }

    public void record() {
        record(State.app.g);
    }

    public void record(PGraphics pg) {
        boolean screenshot = button("recorder/screenshot");
        boolean recordNow = button("recorder/start recording");
        boolean stopRecording = toggle("recorder/stop recording");
        int framesToRecord = sliderInt("recorder/frames", 360, 0, Integer.MAX_VALUE);
        boolean useCropRectangle = toggle("recorder/crop/active");
        recordingPosInput.x = sliderInt("recorder/crop/x");
        recordingPosInput.y = sliderInt("recorder/crop/y");
        int defaultSize = 1000;
        recordingSizeInput.x = sliderInt("recorder/crop/width", defaultSize);
        recordingSizeInput.y = sliderInt("recorder/crop/height", defaultSize);

        int rectX = floor(pg.width / 2f + recordingPosInput.x - recordingSizeInput.x / 2f);
        int rectY = floor(pg.height / 2f + recordingPosInput.y - recordingSizeInput.y / 2f);
        int rectW = floor(recordingSizeInput.x);
        int rectH = floor(recordingSizeInput.y);

        // ffmpeg doesn't like resolutions not divisible by 2
        if (rectW % 2 != 0) {
            rectW += 1;
        }
        if (rectH % 2 != 0) {
            rectH += 1;
        }

        if (!lastRecordNow && recordNow) {
            recordingFolderName = generateRecordingFolderName();
            recordingFrame = 1;
            isRecording = true;
        }
        lastRecordNow = recordNow;
        if (isRecording && (stopRecording || recordingFrame > framesToRecord)) {
            isRecording = false;
        }
        if (isRecording) {
            PImage img = pg;
            if (useCropRectangle) {
                img = pg.get(rectX, rectY, rectW, rectH);
            }
            img.save("out/recorded/" + recordingFolderName + "/" + recordingFrame + ".jpg");
            println("recording " + recordingFrame + " / " + framesToRecord);
            recordingFrame++;
        }
        if (screenshot) {
            PImage img = pg;
            if (useCropRectangle) {
                img = pg.get(rectX, rectY, rectW, rectH);
            }
            String filename = "out/screenshots/" + State.timestamp() + ".png";
            img.save(filename);
            println("saved screenshot: " + filename);
        }

        if (useCropRectangle) {
            State.app.g.pushStyle();
            State.app.g.noFill();
            State.app.g.strokeWeight(1);
            State.app.g.stroke(0xFFFFFFFF);
            State.app.g.rect(rectX, rectY, recordingSizeInput.x, recordingSizeInput.y);
            State.app.g.popStyle();
        }
    }

    private String generateRecordingFolderName() {
        return year() + nf(month(), 2) + nf(day(), 2) + "-" + nf(hour(), 2) + nf(minute(), 2) + nf(second(),
                2);
    }

    public float slider(String path) {
        return slider(path, 0, Float.MAX_VALUE, -Float.MAX_VALUE, false);
    }

    public float slider(String path, float defaultValue) {
        return slider(path, defaultValue, Float.MAX_VALUE, -Float.MAX_VALUE, false);
    }

    public float slider(String path, float defaultValue, float min, float max){
        return slider(path, defaultValue, min, max, true);
    }

    private float slider(String path, float defaultValue, float min, float max, boolean constrained) {
        SliderNode node = (SliderNode) NodeTree.findNodeByPathInTree(path);
        if (node == null) {
            node = createSliderNode(path, defaultValue, 0.1f, min, max, constrained);
            NodeTree.insertNodeAtItsPath(node);
        }
        return node.valueFloat;
    }

    public SliderNode createSliderNode(String path, float defaultValue, float defaultPrecision, float min, float max, boolean constrained) {
        FolderNode folder = (FolderNode) NodeTree.getLazyInitParentFolderByPath(path);
        SliderNode node = new SliderNode(path, folder, defaultValue, min, max, defaultPrecision, constrained);
        node.initSliderBackgroundShader();
        return node;
    }

    public int sliderInt(String path) {
        return sliderInt(path, 0, -Integer.MAX_VALUE, Integer.MAX_VALUE, false);
    }

    public int sliderInt(String path, int defaultValue) {
        return sliderInt(path, defaultValue, -Integer.MAX_VALUE, Integer.MAX_VALUE, false);
    }

    public int sliderInt(String path, int defaultValue, int min, int max) {
        return sliderInt(path, defaultValue, min, max, true);
    }

    private int sliderInt(String path, int defaultValue, int min, int max, boolean constrained) {
        SliderIntNode node = (SliderIntNode) NodeTree.findNodeByPathInTree(path);
        if (node == null) {
            node = createSliderIntNode(path, defaultValue, min, max, constrained);
            NodeTree.insertNodeAtItsPath(node);
        }
        return PApplet.floor(node.valueFloat);
    }

    private SliderIntNode createSliderIntNode(String path, int defaultValue, int min, int max, boolean constrained) {
        FolderNode folder = (FolderNode) NodeTree.getLazyInitParentFolderByPath(path);
        SliderIntNode node = new SliderIntNode(path, folder, defaultValue, min, max, 0.1f, constrained);
        node.initSliderBackgroundShader();
        return node;
    }

    public boolean toggle(String path) {
        return toggle(path, false);
    }

    public boolean toggle(String path, boolean defaultValue) {
        ToggleNode node = (ToggleNode) NodeTree.findNodeByPathInTree(path);
        if (node == null) {
            node = createToggleNode(path, defaultValue);
            NodeTree.insertNodeAtItsPath(node);
        }
        return node.valueBoolean;
    }

    private ToggleNode createToggleNode(String path, boolean defaultValue) {
        FolderNode folder = (FolderNode) NodeTree.getLazyInitParentFolderByPath(path);
        return new ToggleNode(path, folder, defaultValue);
    }

    public boolean button(String path) {
        ButtonNode node = (ButtonNode) NodeTree.findNodeByPathInTree(path);
        if (node == null) {
            node = createButtonNode(path);
            NodeTree.insertNodeAtItsPath(node);
        }
        return node.valueBoolean;
    }

    private ButtonNode createButtonNode(String path) {
        FolderNode folder = (FolderNode) NodeTree.getLazyInitParentFolderByPath(path);
        return new ButtonNode(path, folder);
    }

    public Color colorPicker(String path) {
        return colorPicker(path, 1, 1, 0, 1);
    }

    public Color colorPicker(String path, float grayNorm) {
        return colorPicker(path, grayNorm, grayNorm, grayNorm, 1);
    }

    public Color colorPicker(String path, float hueNorm, float saturationNorm, float brightnessNorm) {
        return colorPicker(path, hueNorm, saturationNorm, brightnessNorm, 1);
    }

    public Color colorPicker(String path, float hueNorm, float saturationNorm, float brightnessNorm, float alphaNorm) {
        ColorPickerFolderNode node = (ColorPickerFolderNode) NodeTree.findNodeByPathInTree(path);
        if (node == null) {
            int hex = State.normalizedColorProvider.color(hueNorm, saturationNorm, brightnessNorm, 1);
            FolderNode folder = (FolderNode) NodeTree.getLazyInitParentFolderByPath(path);
            node = new ColorPickerFolderNode(path, folder, hex);
            NodeTree.insertNodeAtItsPath(node);
        }
        return node.getColor();
    }

    public Color colorPicker(String path, int hex) {
        ColorPickerFolderNode node = (ColorPickerFolderNode) NodeTree.findNodeByPathInTree(path);
        if (node == null) {
            FolderNode folder = (FolderNode) NodeTree.getLazyInitParentFolderByPath(path);
            node = new ColorPickerFolderNode(path, folder, hex);
            NodeTree.insertNodeAtItsPath(node);
        }
        return node.getColor();
    }

    public void colorPickerSet(String path, int hex) {
        ColorPickerFolderNode node = (ColorPickerFolderNode) NodeTree.findNodeByPathInTree(path);
        if (node == null) {
            FolderNode folder = (FolderNode) NodeTree.getLazyInitParentFolderByPath(path);
            node = new ColorPickerFolderNode(path, folder, hex);
            NodeTree.insertNodeAtItsPath(node);
        } else {

            node.setHex(hex);
            node.loadValuesFromHex(false);
        }
    }

    public PGraphics gradient(String path) {
        return gradient(path, 1);
    }

    public PGraphics gradient(String path, float alpha) {
        GradientFolderNode node = (GradientFolderNode) NodeTree.findNodeByPathInTree(path);
        if (node == null) {
            FolderNode parentFolder = (FolderNode) NodeTree.getLazyInitParentFolderByPath(path);
            node = new GradientFolderNode(path, parentFolder, alpha);
            NodeTree.insertNodeAtItsPath(node);
        }
        return node.getOutputGraphics();
    }

    public PImage imagePicker(String path) {
        return imagePicker(path, "");
    }

    public PImage imagePicker(String path, String defaultFilePath) {
        ImagePickerFolderNode node = (ImagePickerFolderNode) NodeTree.findNodeByPathInTree(path);
        if (node == null) {
            FolderNode parentFolder = (FolderNode) NodeTree.getLazyInitParentFolderByPath(path);
            node = new ImagePickerFolderNode(path, parentFolder, defaultFilePath);
            NodeTree.insertNodeAtItsPath(node);
        }
        return node.getOutputImage();
    }
}
