package net.kenevans.maplines.ui;

import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;

import net.kenevans.maplines.lines.Line;
import net.kenevans.maplines.lines.Lines;
import net.kenevans.maplines.lines.MapCalibration;
import net.kenevans.maplines.lines.MapCalibration.MapData;
import net.kenevans.maplines.utils.SWTUtils;

public class SWTImageViewerControl extends Composite
{
    // private static final boolean usePrintSettingDialog = true;
    private static final boolean useStartImage = false;
    private static final String startImageName = "C:/users/evans/Pictures/DAZ.Dogfight.15017.jpg";
    // private static final String startImageName =
    // "C:/Documents and Settings/evans/My Documents/My
    // Pictures/ChromaticityDiagram.png";

    private static final int LINE_WIDTH = 3;
    private static final int SELECTED_LINE_WIDTH = 5;

    private Display display;
    private Shell shell;
    private Canvas canvas;
    private ScrollBar hBar;
    private ScrollBar vBar;
    private Point origin;
    private Image image;

    private Lines lines;
    private Line curLine;
    private MapLinesView view;

    public SWTImageViewerControl(Composite parent, int style,
        MapLinesView view) {
        super(parent, style);
        shell = parent.getShell();
        display = shell.getDisplay();
        this.view = view;

        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        this.setLayout(layout);

        origin = new Point(0, 0);

        canvas = new Canvas(this,
            SWT.NO_REDRAW_RESIZE | SWT.V_SCROLL | SWT.H_SCROLL);
        // canvas = new Canvas(shell, SWT.NO_BACKGROUND
        // | SWT.NO_REDRAW_RESIZE | SWT.V_SCROLL | SWT.H_SCROLL);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(canvas);
        canvas.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
        canvas.setLayoutData(new GridData(GridData.FILL_BOTH));

        hBar = canvas.getHorizontalBar();
        hBar.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event ev) {
                // // DEBUG
                // System.out.println("handleEvent for SWT.Selection H");
                int hSelection = hBar.getSelection();
                int destX = -hSelection - origin.x;
                Rectangle rect = image.getBounds();
                canvas.scroll(destX, 0, 0, 0, rect.width, rect.height, false);
                origin.x = -hSelection;
            }
        });

        vBar = canvas.getVerticalBar();
        vBar.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event ev) {
                // // DEBUG
                // System.out.println("handleEvent for SWT.Selection V");
                int vSelection = vBar.getSelection();
                int destY = -vSelection - origin.y;
                Rectangle rect = image.getBounds();
                canvas.scroll(0, destY, 0, 0, rect.width, rect.height, false);
                origin.y = -vSelection;
            }
        });

        canvas.addListener(SWT.Resize, new Listener() {
            public void handleEvent(Event ev) {
                // // DEBUG
                // System.out.println("handleEvent for SWT.Resize");
                resetCanvas();
            }
        });

        canvas.addListener(SWT.Paint, new Listener() {
            public void handleEvent(Event ev) {
                if(image == null || image.isDisposed()) {
                    return;
                }
                // // DEBUG
                // System.out.println("handleEvent for SWT.Paint");
                // Rectangle clip = ev.gc.getClipping();
                // System.out.println("clipping: x=" + clip.x + " y=" + clip.x
                // + " width=" + clip.width + " height=" + clip.height);
                // Rectangle bounds = canvas.getBounds();
                // System.out.println("bounds: x=" + bounds.x + " y=" + bounds.x
                // + " width=" + bounds.width + " height=" + bounds.height);
                // Rectangle client1 = canvas.getClientArea();
                // System.out.println("client: x=" + client1.x + " y=" +
                // client1.x
                // + " width=" + client1.width + " height=" + client1.height);
                // System.out.println("origin: x=" + origin.x + " y=" +
                // origin.y);
                GC gc = ev.gc;
                gc.drawImage(image, origin.x, origin.y);
                Rectangle rect = image.getBounds();
                Rectangle client = canvas.getClientArea();
                int marginWidth = client.width - rect.width;
                if(marginWidth > 0) {
                    gc.fillRectangle(rect.width, 0, marginWidth, client.height);
                }
                int marginHeight = client.height - rect.height;
                if(marginHeight > 0) {
                    gc.fillRectangle(0, rect.height, client.width,
                        marginHeight);
                }

                // Draw the lines
                if(lines != null || lines.getNLines() > 0) {
                    boolean first;
                    Point prev = null;
                    for(Line line : lines.getLines()) {
                        if(line.getNPoints() < 1) {
                            continue;
                        }
                        Color color = new Color(Display.getCurrent(),
                            line.getRgb());
                        gc.setForeground(color);
                        if(line.isSelected()) {
                            gc.setLineWidth(SELECTED_LINE_WIDTH);
                        } else {
                            gc.setLineWidth(LINE_WIDTH);
                        }
                        first = true;
                        for(Point point : line.getPoints()) {
                            if(first) {
                                first = false;
                                prev = point;
                            } else {
                                // Only the image scrolls. Have to add origin to
                                // get the right coordinates.
                                gc.drawLine(origin.x + prev.x,
                                    origin.y + prev.y, origin.x + point.x,
                                    origin.y + point.y);
                                prev = point;
                            }
                        }
                        color.dispose();
                    }
                }

                // Draw the calibration points
                MapCalibration mapCalibration = null;
                List<MapData> dataList = null;
                if(SWTImageViewerControl.this.view != null) {
                    mapCalibration = SWTImageViewerControl.this.view
                        .getMapCalibration();
                    if(mapCalibration != null) {
                        dataList = mapCalibration.getDataList();
                    }
                }
                if(dataList != null && dataList.size() > 0) {
                    gc.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
                    gc.setLineWidth(1);
                    int x, y;
                    int len = 10;
                    for(MapData data : dataList) {
                        x = origin.x + data.getX();
                        y = origin.y + data.getY();
                        gc.drawLine(x + len, y, x - len, y);
                        gc.drawLine(x, y + len, x, y - len);
                    }
                }
            }
        });

        canvas.addMouseListener(new MouseListener() {
            @Override
            public void mouseDoubleClick(MouseEvent ev) {
                // TODO Auto-generated method stub
            }

            @Override
            public void mouseDown(MouseEvent ev) {
                // TODO Auto-generated method stub
            }

            @Override
            public void mouseUp(MouseEvent ev) {
                if(ev.button == 1) {
                    if(curLine != null) {
                        // // DEBUG
                        // System.out.println("mouseUp: ev: x=" + ev.x + " y=" +
                        // ev.y);
                        // System.out.println("nLines=" + lines.getNLines());
                        // for(Line line : lines.getLines()) {
                        // System.out.println(" nPoints=" + line.getNPoints()
                        // + (line == curLine ? " current" : ""));
                        // }
                        curLine.addPoint(
                            new Point(ev.x - origin.x, ev.y - origin.y));
                        // // DEBUG
                        // System.out.println("mouseUp: x=" + (origin.x + ev.x)
                        // + ", " + (origin.y + ev.y) + " origin: x="
                        // + origin.x + ", " + origin.y + " ev: x=" + ev.x
                        // + ", " + ev.y);
                        canvas.redraw();
                    }
                }
            }
        });

        // Menu menu = new Menu(canvas);
        // MenuItem item = new MenuItem(menu, SWT.PUSH);
        // item.setText("Test");
        // canvas.setMenu(menu);

        // MenuManager mgr = new MenuManager();
        // Menu menu = mgr.createContextMenu(parent);
        // canvas.setMenu(menu);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        if(this.image != null && !this.image.isDisposed()) {
            this.image.dispose();
            this.image = null;
        }
    }

    /**
     * @return The value of image.
     */
    public Image getImage() {
        return image;
    }

    /**
     * @param image The new value for image.
     */
    public void setImage(Image image) {
        if(this.image != image) {
            if(this.image != null && !this.image.isDisposed()) {
                this.image.dispose();
                this.image = null;
            }
            this.image = image;
            resetCanvas();
        }
    }

    /**
     * Resets the canvas and scroll bars and does a redraw.
     */
    public void resetCanvas() {
        resetCanvas(0, 0);
    }

    /**
     * Resets the canvas and sets the scroll bars to select the given width and
     * height then does a redraw.
     * 
     * @param x The x coordinate of the selection
     * @param y The coordinate of the selection
     */
    public void resetCanvas(int x, int y) {
        if(canvas == null || canvas.isDisposed()) return;
        if(image == null || image.isDisposed()) {
            canvas.redraw();
            return;
        }
        if(hBar == null || hBar.isDisposed()) return;
        if(vBar == null || vBar.isDisposed()) return;
        Rectangle rect = image.getBounds();
        Rectangle client = canvas.getClientArea();
        hBar.setMaximum(rect.width);
        vBar.setMaximum(rect.height);
        hBar.setThumb(Math.min(rect.width, client.width));
        vBar.setThumb(Math.min(rect.height, client.height));
        int hPage = rect.width - client.width;
        int vPage = rect.height - client.height;
        hBar.setSelection(x);
        vBar.setSelection(y);
        // The selection values may be different than set
        int hSelection = hBar.getSelection();
        int vSelection = vBar.getSelection();
        if(hSelection >= hPage) {
            if(hPage <= 0) hSelection = 0;
        }
        if(vSelection >= vPage) {
            if(vPage <= 0) vSelection = 0;
        }
        origin.x = -hSelection;
        origin.y = -vSelection;
        canvas.redraw();
    }

    public static void main(String[] args) {
        Display display = new Display();
        Shell shell = new Shell(display);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        shell.setLayout(layout);
        shell.setText("SWT Image Viewer");
        shell.setLayout(new GridLayout(1, true));

        // SWT.DEFAULT gives scroll bars in addition to those on the Control
        // SWT.NONE does not
        SWTImageViewerControl control = new SWTImageViewerControl(shell,
            SWT.NONE, null);
        Image image = null;
        if(useStartImage) {
            try {
                image = new Image(display, startImageName);
                control.setImage(image);
            } catch(RuntimeException ex) {
                SWTUtils.excMsgAsync(shell,
                    "Cannot load image from:\n" + startImageName, ex);
            }
        }

        GridDataFactory.fillDefaults().grab(true, true).applyTo(control);

        shell.setSize(800, 600);
        shell.open();

        // Set up the event loop.
        while(!shell.isDisposed()) {
            if(!display.readAndDispatch()) {
                // If no more entries in event queue
                display.sleep();
            }
        }
        display.dispose();
    }

    /**
     * @return The value of lines.
     */
    public Lines getLines() {
        return lines;
    }

    /**
     * @param lines The new value for lines.
     */
    public void setLines(Lines lines) {
        this.lines = lines;
    }

    /**
     * @return The value of curLine.
     */
    public Line getCurLine() {
        return curLine;
    }

    /**
     * @param curLine The new value for curLine.
     */
    public void setCurLine(Line curLine) {
        this.curLine = curLine;
    }

    /**
     * @return The value of canvas.
     */
    public Canvas getCanvas() {
        return canvas;
    }

}