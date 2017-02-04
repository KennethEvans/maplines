package net.kenevans.maplines.ui;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import net.kenevans.maplines.lines.Line;
import net.kenevans.maplines.lines.Lines;
import net.kenevans.maplines.utils.SWTUtils;

/*
 * Created on Aug 23, 2010
 * By Kenneth Evans, Jr.
 */

public class EditLinesDialog extends Dialog
{
    // private static final int TEXT_COLS_LARGE = 50;
    // private static final int TEXT_COLS_SMALL = 10;
    private boolean success = false;

    private MapLinesView view;
    private Lines lines;
    private List list;

    /**
     * Constructor.
     * 
     * @param parent
     */
    public EditLinesDialog(Shell parent, MapLinesView view) {
        // We want this to be modeless
        this(parent, SWT.DIALOG_TRIM | SWT.NONE, view);
    }

    /**
     * Constructor.
     * 
     * @param parent The parent of this dialog.
     * @param style Style passed to the parent.
     */
    public EditLinesDialog(Shell parent, int style, MapLinesView view) {
        super(parent, style);
        this.view = view;
        this.lines = view.getLines();
    }

    /**
     * Convenience method to open the dialog.
     * 
     * @return Whether OK was selected or not.
     */
    public boolean open() {
        Shell shell = new Shell(getParent(), getStyle() | SWT.RESIZE);
        shell.setText("Edit Lines");
        // It can take a long time to do this so use a wait cursor
        // Probably not, though
        Cursor waitCursor = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
        if(waitCursor != null) getParent().setCursor(waitCursor);
        createContents(shell);
        getParent().setCursor(null);
        waitCursor.dispose();
        shell.pack();
        shell.open();
        Display display = getParent().getDisplay();
        while(!shell.isDisposed()) {
            if(!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return success;
    }

    /**
     * Creates the contents of the dialog.
     * 
     * @param shell
     */
    private void createContents(final Shell shell) {
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        shell.setLayout(gridLayout);

        // Create the groups
        createListGroup(shell);
        createActionsGroup(shell);

        // Create the buttons
        // Make a zero margin composite for the OK and Cancel buttons
        Composite composite = new Composite(shell, SWT.NONE);
        // Change END to FILL to center the buttons
        GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL)
            .grab(true, false).applyTo(composite);
        gridLayout = new GridLayout();
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.numColumns = 3;
        composite.setLayout(gridLayout);

        Button button;

        button = new Button(composite, SWT.PUSH);
        button.setText("OK");
        GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL)
            .grab(true, true).applyTo(button);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                success = true;
                shell.close();
                if(lines == null) {
                    return;
                }
                for(Line line : lines.getLines()) {
                    line.setSelected(false);
                }
                view.getViewer().getCanvas().redraw();
            }
        });
        shell.setDefaultButton(button);
    }

    /**
     * Creates the List group.
     * 
     * @param shell
     */
    private void createListGroup(final Shell shell) {
        Group box = new Group(shell, SWT.BORDER);
        box.setText("Lines");
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        box.setLayout(gridLayout);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(box);

        // Make a zero margin composite
        Composite composite = new Composite(box, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
            .grab(true, false).applyTo(composite);
        gridLayout = new GridLayout();
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.numColumns = 1;
        composite.setLayout(gridLayout);

        list = new List(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        // Limit the vertical height or it will go off screen
        GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 400)
            .applyTo(list);
        list.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                int[] selections = list.getSelectionIndices();
                for(Line line : lines.getLines()) {
                    line.setSelected(false);
                }
                Line line;
                for(int i = 0; i < selections.length; i++) {
                    line = lines.getLines().get(selections[i]);
                    line.setSelected(true);
                }
                view.getViewer().getCanvas().redraw();
            }

            public void widgetDefaultSelected(SelectionEvent event) {
                int[] selections = list.getSelectionIndices();
                for(Line line : lines.getLines()) {
                    line.setSelected(false);
                }
                Line line;
                for(int i = 0; i < selections.length; i++) {
                    line = lines.getLines().get(selections[i]);
                    line.setSelected(true);
                }
                view.getViewer().getCanvas().redraw();
            }
        });

        resetList();
    }

    /**
     * Creates the actions group.
     * 
     * @param shell
     */
    private void createActionsGroup(final Shell shell) {
        Group box = new Group(shell, SWT.BORDER);
        box.setText("Actions");
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        box.setLayout(gridLayout);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(box);

        // Make a zero margin composite
        Composite composite = new Composite(box, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
            .grab(true, false).applyTo(composite);
        gridLayout = new GridLayout();
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.numColumns = 8; // Set equal to number of buttons
        composite.setLayout(gridLayout);

        Button button;
        button = new Button(composite, SWT.PUSH);
        button.setText("Delete");
        button.setToolTipText("Delete selected lines.");
        GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL)
            .grab(true, true).applyTo(button);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                if(list == null || list.getSelectionCount() == 0) {
                    return;
                }
                if(lines == null) {
                    return;
                }
                int[] indices = list.getSelectionIndices();
                // Order is not guaranteed
                Arrays.sort(indices);
                int len = indices.length;
                // Remove them in reverse order to preserve the correct indices
                for(int i = 0; i < len; i++) {
                    lines.removeLine(indices[len - 1 - i]);
                }
                resetList();
                view.getViewer().getCanvas().redraw();
            }
        });

        button = new Button(composite, SWT.PUSH);
        button.setText("Rename");
        button.setToolTipText("Rename selected lines.");
        GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL)
            .grab(true, true).applyTo(button);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                if(list == null || list.getSelectionCount() == 0) {
                    return;
                }
                if(lines == null) {
                    return;
                }
                int[] indices = list.getSelectionIndices();
                String desc;
                Line line;
                for(int i : indices) {
                    line = lines.getLines().get(i);
                    desc = line.getDesc();
                    InputDialog dlg = new InputDialog(null, "Description",
                        "Enter a description:", desc, null);
                    dlg.setBlockOnOpen(true);
                    int res = dlg.open();
                    if(res == org.eclipse.jface.dialogs.Dialog.OK) {
                        String val = dlg.getValue();
                        if(val != null) {
                            line.setDesc(val);
                        }
                    }
                }
                resetList();
                // Reset what was selected
                list.setSelection(indices);
                for(int i : indices) {
                    line = lines.getLines().get(i);
                    desc = line.getDesc();
                    line.setSelected(true);
                }
                view.getViewer().getCanvas().redraw();
            }
        });

        button = new Button(composite, SWT.PUSH);
        button.setText("Color");
        button.setToolTipText("Set the color for the selected lines.");
        GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL)
            .grab(true, true).applyTo(button);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                if(list == null || list.getSelectionCount() == 0) {
                    return;
                }
                if(lines == null) {
                    return;
                }
                int[] indices = list.getSelectionIndices();

                ColorDialog dialog = new ColorDialog(shell);
                dialog.setText("Line Color");
                Line line = lines.getLines().get(indices[0]);
                RGB defaultRgb = line.getRgb();
                dialog.setRGB(defaultRgb);
                RGB newColor = dialog.open();
                if(newColor != null) {
                    for(int i : indices) {
                        line = lines.getLines().get(i);
                        line.setRgb(newColor);
                    }
                }
                view.getViewer().getCanvas().redraw();
            }
        });

        button = new Button(composite, SWT.PUSH);
        button.setText("Up");
        button.setToolTipText("Move selected lines up.");
        GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL)
            .grab(true, true).applyTo(button);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                if(list == null || list.getSelectionCount() == 0) {
                    return;
                }
                if(lines == null) {
                    return;
                }
                int[] indices = list.getSelectionIndices();
                // Order is not guaranteed
                Arrays.sort(indices);
                int len = indices.length;
                int minIndex = indices[0];
                if(minIndex == 0) {
                    SWTUtils.errMsg("Already at the top");
                    return;
                }
                ArrayList<Line> movedLines = new ArrayList<Line>(len);
                // Remove them in reverse order to preserve the correct indices
                int idx;
                for(int i = 0; i < len; i++) {
                    idx = indices[len - 1 - i];
                    movedLines.add(0, lines.getLines().get(idx));
                    lines.removeLine(idx);
                }
                lines.getLines().addAll(minIndex - 1, movedLines);
                movedLines.clear();
                // Find the selected ones
                idx = 0;
                Line line;
                for(int i = 0; i < lines.getNLines(); i++) {
                    line = lines.getLines().get(i);
                    if(line.isSelected() && idx < len) {
                        indices[idx++] = i;
                    }
                }
                resetList();
                // Reselect the moved ones
                for(int i = 0; i < len; i++) {
                    line = lines.getLines().get(indices[i]);
                    line.setSelected(true);
                }
                list.setSelection(indices);
                view.getViewer().getCanvas().redraw();
            }
        });

        button = new Button(composite, SWT.PUSH);
        button.setText("Down");
        button.setToolTipText("Move selected lines down.");
        GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL)
            .grab(true, true).applyTo(button);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                if(list == null || list.getSelectionCount() == 0) {
                    return;
                }
                if(lines == null) {
                    return;
                }
                int[] indices = list.getSelectionIndices();
                // Order is not guaranteed
                Arrays.sort(indices);
                int len = indices.length;
                int maxIndex = indices[len - 1];
                if(maxIndex == lines.getNLines() - 1) {
                    SWTUtils.errMsg("Already at the bottom");
                    return;
                }
                ArrayList<Line> movedLines = new ArrayList<Line>(len);
                // Remove them in reverse order to preserve the correct indices
                int idx;
                for(int i = 0; i < len; i++) {
                    idx = indices[len - 1 - i];
                    movedLines.add(0, lines.getLines().get(idx));
                    lines.removeLine(idx);
                }
                lines.getLines().addAll(maxIndex + 1, movedLines);
                movedLines.clear();
                // Find the selected ones
                idx = 0;
                Line line;
                for(int i = 0; i < lines.getNLines(); i++) {
                    line = lines.getLines().get(i);
                    if(line.isSelected() && idx < len) {
                        indices[idx++] = i;
                    }
                }
                resetList();
                // Reselect the moved ones
                for(int i = 0; i < len; i++) {
                    line = lines.getLines().get(indices[i]);
                    line.setSelected(true);
                }
                list.setSelection(indices);
                view.getViewer().getCanvas().redraw();
            }
        });

        button = new Button(composite, SWT.PUSH);
        button.setText("Number");
        button.setToolTipText("Number all lines consecutively.");
        GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL)
            .grab(true, true).applyTo(button);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                if(lines == null) {
                    return;
                }
                int[] indices = list.getSelectionIndices();
                int n = 1;
                for(Line line : lines.getLines()) {
                    line.setDesc("Line " + n++);
                }
                resetList();
                // Reset what was selected
                list.setSelection(indices);
                Line line;
                for(int i : indices) {
                    line = lines.getLines().get(i);
                    line.setSelected(true);
                }
                view.getViewer().getCanvas().redraw();
            }
        });

        button = new Button(composite, SWT.PUSH);
        button.setText("Refresh");
        button.setToolTipText("Refresh the list.");
        GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL)
            .grab(true, true).applyTo(button);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                resetList();
            }
        });

        button = new Button(composite, SWT.PUSH);
        button.setText("Deselect All");
        button.setToolTipText("Deselect all lines.");
        GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL)
            .grab(true, true).applyTo(button);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                if(list == null || list.getSelectionCount() == 0) {
                    return;
                }
                list.deselectAll();
                if(lines == null) {
                    return;
                }
                for(Line line : lines.getLines()) {
                    line.setSelected(false);
                }
                view.getViewer().getCanvas().redraw();
            }
        });

    }

    private void resetList() {
        if(list == null) {
            return;
        }
        list.deselectAll();
        list.removeAll();
        if(lines == null) {
            return;
        }
        for(Line line : lines.getLines()) {
            list.add(line.getDesc());
            line.setSelected(false);
        }
        view.getViewer().getCanvas().redraw();
    }

}
