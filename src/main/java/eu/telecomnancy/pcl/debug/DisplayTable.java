package eu.telecomnancy.pcl.debug;

import eu.telecomnancy.pcl.Main;
import eu.telecomnancy.pcl.antlr.ANTLRParser;
import eu.telecomnancy.pcl.antlr.ANTLRTreeException;
import eu.telecomnancy.pcl.ast.ASTRoot;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;
import eu.telecomnancy.pcl.symbolTable.TableCreation;
import org.piccolo2d.PCamera;
import org.piccolo2d.PCanvas;
import org.piccolo2d.PNode;
import org.piccolo2d.extras.pswing.PSwing;
import org.piccolo2d.extras.pswing.PSwingCanvas;
import org.piccolo2d.util.PBounds;
import org.piccolo2d.util.PPaintContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


public class DisplayTable extends JFrame implements ActionListener {
    static final int SPEED=50;
    static ArrayList<SingleTable> singleTable = new ArrayList<>();
    JDesktopPane desktop;
    private final Logger logger = new Logger();
    public DisplayTable(String filename,boolean shouldPrompt) {
        super("DisplaySymbolTable");

        int inset = 50;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(inset, inset, screenSize.width - inset * 2, screenSize.height - inset * 2);

        desktop = new JDesktopPane();

        //Special Camera layers
        PSwing swingWrapper = new PSwing(desktop);
        PSwingCanvas canvas = new PSwingCanvas();
        canvas.add(new Background(canvas));
        canvas.removeInputEventListener(canvas.getPanEventHandler());
        canvas.removeInputEventListener(canvas.getZoomEventHandler());
        canvas.getLayer().addChild(swingWrapper);
        canvas.setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        canvas.setDefaultRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        canvas.setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        canvas.setMinimumSize(new Dimension(500, 500));

        //actual JFrame
        setContentPane(canvas);
        setJMenuBar(createMenuBar());

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent evt) {
                Dimension dim = new Dimension(0, 0);
                Component[] com = desktop.getComponents();
                for (Component component : com) {
                    int w = (int) Math.max(component.getX() + component.getWidth() + inset, dim.getWidth());
                    int h = (int) Math.max(component.getY() + component.getHeight() + inset, dim.getHeight());
                    dim.setSize(new Dimension(w, h));
                }
                desktop.setPreferredSize(dim);
                desktop.revalidate();
                revalidate();
                repaint();
            }
        });
        //Make dragging a little faster but perhaps uglier.
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

        this.revalidate();
        this.repaint();
        openNew(shouldPrompt, filename);
        centerOver(swingWrapper,canvas.getCamera());
        canvas.getCamera().setViewScale(0.5);
    }

    public static void createAndShowGUI(String filename,boolean shouldPrompt) throws ANTLRTreeException, IOException {
        //Make sure we have nice window decorations.

        //Create and set up the window.
        DisplayTable frame = new DisplayTable(filename,shouldPrompt);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Display the window.
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                createAndShowGUI("tree.a60",false);
            } catch (ANTLRTreeException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void centerOver(PNode node, PCamera camera) {
        final PBounds b = node.getGlobalFullBounds();
        camera.viewToLocal(b);
        node.centerBoundsOnPoint(b.getCenterX(), b.getCenterY());
    }

    protected JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        //Set up the lone menu.
        JMenu menu = new JMenu("Document");
        menu.setMnemonic(KeyEvent.VK_D);
        menuBar.add(menu);

        JMenuItem menuItem = new JMenuItem("New");
        menuItem.setMnemonic(KeyEvent.VK_N);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK));
        menuItem.setActionCommand("new");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Quit");
        menuItem.setMnemonic(KeyEvent.VK_Q);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_Q, InputEvent.ALT_DOWN_MASK));
        menuItem.setActionCommand("quit");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menu = new JMenu("View");
        menu.setMnemonic(KeyEvent.VK_D);
        menuBar.add(menu);

        menuItem = new JMenuItem("Reset View");
        menuItem.setMnemonic(KeyEvent.VK_R);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK));
        menuItem.setActionCommand("resetView");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Zoom In");
        menuItem.setMnemonic(KeyEvent.VK_P);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK));
        menuItem.setActionCommand("zoomIn");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Move Left");
        menuItem.setMnemonic(KeyEvent.VK_LEFT);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK));
        menuItem.setActionCommand("left");
        menuItem.addActionListener(this);
        menu.add(menuItem);


        menuItem = new JMenuItem("Move Right");
        menuItem.setMnemonic(KeyEvent.VK_RIGHT);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK));
        menuItem.setActionCommand("right");
        menuItem.addActionListener(this);
        menu.add(menuItem);


        menuItem = new JMenuItem("Move Up");
        menuItem.setMnemonic(KeyEvent.VK_UP);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK));
        menuItem.setActionCommand("up");
        menuItem.addActionListener(this);
        menu.add(menuItem);


        menuItem = new JMenuItem("Move Down");
        menuItem.setMnemonic(KeyEvent.VK_DOWN);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK));
        menuItem.setActionCommand("down");
        menuItem.addActionListener(this);
        menu.add(menuItem);


        menuItem = new JMenuItem("Zoom Out");
        menuItem.setMnemonic(KeyEvent.VK_M);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK));
        menuItem.setActionCommand("zoomOut");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        return menuBar;
    }

    //React to menu selections.
    public void actionPerformed(ActionEvent e) {

        switch (e.getActionCommand()) {
            case "new":
                openNew(true, "lego.a60");
                break;
            case "resetView":
                resetView();
                break;
            case "zoomIn":
                zoomIn();
                break;
            case "zoomOut":
                zoomOut();
                break;
            case "left":
                moveScreen(SPEED, 0);
                break;
            case "right":
                moveScreen(-SPEED, 0);
                break;
            case "up":
                moveScreen(0, SPEED);
                break;
            case "down":
                moveScreen(0, -SPEED);
                break;

            default:
                quit();
        }
    }

    public void moveScreen(int directionx, int directiony) {
        PCanvas canvas = (PCanvas) this.getContentPane();
        canvas.getCamera().translateView(directionx, directiony);
    }


    public void openNew(Boolean shouldPrompt, String fallback) {
        desktop.removeAll();
        singleTable.clear();
        SingleTable.openFrameCount = 0;
        revalidate();
        repaint();
        InputStream is = null;
        if (shouldPrompt) {

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
            int result = fileChooser.showOpenDialog(this);
            try {
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    is = new FileInputStream(selectedFile);
                } else {
                    JOptionPane.showMessageDialog(null, "No file Selected", "Error while reading file", JOptionPane.ERROR_MESSAGE);

                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "File opener failed: " + e.getMessage(), "Error while reading file", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            is = Main.class.getClassLoader().getResourceAsStream(fallback);
        }

        try {
            if (is == null) {
                throw new Exception("No stream to read");
            }
            ANTLRParser parser = new ANTLRParser();
            parser.readSource(is);
            if (logger.isCritical()) {
                logger.getLogs();
                throw new Exception(logger.getInternalLogs());
            }
            ASTRoot ast = parser.buildAST();
            SymbolTable symbolTable = (new TableCreation()).create(ast);
            makeTree(symbolTable);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "File parsing failed: " + e.getMessage(), "Error while reading file", JOptionPane.ERROR_MESSAGE);
        }
    }


    public void zoomIn() {
        PCanvas canvas = (PCanvas) this.getContentPane();
        canvas.getCamera().setViewScale(canvas.getCamera().getViewScale() + 1 / 10f);
    }

    public void zoomOut() {
        PCanvas canvas = (PCanvas) this.getContentPane();
        canvas.getCamera().setViewScale(Math.max(canvas.getCamera().getViewScale() - 1 / 10f,0.1));
    }

    public void resetView() {
        PCanvas canvas = (PCanvas) this.getContentPane();
        canvas.getCamera().setViewScale(1.0);
        canvas.getCamera().setViewBounds(canvas.getBounds());
    }

    public void paintAgain() {
        for (Component component : desktop.getComponents()) {
            component.transferFocus();
        }
    }
    private void makeTree(SymbolTable symbolTable){
        SingleTable root = new SingleTable(symbolTable.getRoot().getScopeName(), symbolTable.getRoot());
        root.setVisible(true);
        desktop.add(root);
        singleTable.add(root);
        root.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                super.componentResized(e);
                repaint();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                repaint();
            }
        });
        instantiateFrame(symbolTable.getRoot(),root);
        int maxDepth=0;
        for (SingleTable singleTable:singleTable){
            maxDepth=Math.max(singleTable.getIndex().size(),maxDepth);
        }
        int[] maxChildren=new int[maxDepth];
        for (SingleTable singleTable:singleTable){
            maxChildren[singleTable.getIndex().size()-1]=Math.max(maxChildren[singleTable.getIndex().size()-1],singleTable.scope.getChildren().size());
        }
        for (SingleTable singleTable:singleTable){
            singleTable.relocate(maxChildren);
        }
        paintAgain();
    }
    private void instantiateFrame(SymbolTable.Scope scope,SingleTable parent) {
        for (SymbolTable.Scope children : scope.getChildren()) {
            SingleTable frame = new SingleTable(children.getScopeName(), children);
            frame.setVisible(true);
            desktop.add(frame);
            singleTable.add(frame);
            parent.addChildren(frame);
            frame.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentMoved(ComponentEvent e) {
                    super.componentResized(e);
                    repaint();
                }

                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    repaint();
                }
            });
            instantiateFrame(children,frame);
        }
    }

    //Quit the application.
    protected void quit() {
        System.exit(0);
    }

    public static class Background extends JComponent {
        PSwingCanvas parent;
        Background( PSwingCanvas canvas ) {
            int inset = 0;
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setBounds(inset, inset,
                    screenSize.width - inset * 2,
                    screenSize.height - inset * 2);
            this.parent=canvas;
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            double s=parent.getCamera().getViewScale();
            g.translate((int)(-parent.getCamera().getViewBounds().x*s),(int)(-parent.getCamera().getViewBounds().y*s));
            for (SingleTable frame:singleTable){
                for (SingleTable frame2: frame.getChildren()){
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setStroke(new BasicStroke(5));
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
                    g2.draw(new Line2D.Double((frame.getWidth() / 2f + frame.getX())*s, (frame.getHeight() + frame.getY() )*s, (frame2.getWidth() / 2f + frame2.getX())*s, (frame2.getY())*s));
                    g2.setPaintMode();
                }
            }
        }
    }
}
