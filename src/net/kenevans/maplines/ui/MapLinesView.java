package net.kenevans.maplines.ui;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

import net.kenevans.maplines.lines.GPSLUtils;
import net.kenevans.maplines.lines.GPXUtils;
import net.kenevans.maplines.lines.Line;
import net.kenevans.maplines.lines.Lines;
import net.kenevans.maplines.lines.MapCalibration;
import net.kenevans.maplines.lines.MapCalibration.MapData;
import net.kenevans.maplines.plugin.Activator;
import net.kenevans.maplines.plugin.IPreferenceConstants;
import net.kenevans.maplines.utils.SWTUtils;

public class MapLinesView extends ViewPart implements IPreferenceConstants {
	public static final String ID = "net.kenevans.maplines.view";

	/** The name of the current image file */
	protected String imageFileName;
	/** The name of the current calibration file */
	protected String calibFileName;

	private Display display;
	private Shell shell;

	/**
	 * Path used for initializing the open dialogs.
	 */
	protected String initialImagePath;
	/**
	 * Image path used for initializing the open calibration dialog.
	 */

	/** The viewer that controls most of the drawing. */
	protected SWTImageViewerControl viewer;

	/** The Lines class that holds the lines. May be empty. */
	protected Lines lines;

	/** The MapCalibration class that holds the calibration if any. */
	protected MapCalibration mapCalibration;

	/**
	 * Counter used for naming lines. Is incremented when each line is created. Does
	 * not account for deleted or renamed lines but continues incrementing.
	 */
	protected static int nextLineNumber = 1;

	/**
	 * The content provider class is responsible for providing objects to the view.
	 * It can wrap existing objects in adapters or simply return objects as-is.
	 * These objects may be sensitive to the current input of the view, or ignore it
	 * and always show the same content (like Task List, for example).
	 */
	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			if (parent instanceof Object[]) {
				return (Object[]) parent;
			}
			return new Object[0];
		}
	}

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}

		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}

		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		display = parent.getDisplay();
		shell = parent.getShell();
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 2;
		parent.setLayout(layout);

		// Get the stored values
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		imageFileName = prefs.getString(P_IMAGE_FILE_NAME);
		if (imageFileName != null && imageFileName.length() == 0) {
			imageFileName = null;
		}
		calibFileName = prefs.getString(P_CALIB_FILE_NAME);
		if (calibFileName != null && calibFileName.length() == 0) {
			calibFileName = null;
		}
		initialImagePath = prefs.getString(P_INITIAL_IMAGE_PATH);
		if (initialImagePath != null && initialImagePath.length() == 0) {
			initialImagePath = null;
		}

		// SWT.DEFAULT gives scroll bars in addition to those on the Control
		// SWT.NONE does not
		viewer = new SWTImageViewerControl(parent, SWT.NONE, this);
		lines = new Lines();
		viewer.setLines(lines);
		// Line line = new Line();
		// lines.addLine(line);
		// viewer.setCurLine(line);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(viewer);
		// viewer.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
		// parent.setBackground(display.getSystemColor(SWT.COLOR_RED));

		// Load the starting image and calibration file
		if (imageFileName != null) {
			loadImage(imageFileName);
		}
		if (calibFileName != null) {
			loadCalibFile(calibFileName);
		}

		createHandlers();
		hookContextMenu(viewer.getCanvas());

		// // DEBUG
		// // Sleak
		// Sleak sleak = new Sleak();
		// sleak.open();
	}

	/**
	 * Passing the focus request to the viewer's viewer.
	 */
	public void setFocus() {
		viewer.setFocus();
	}

	/**
	 * Utility to set preference String Value to handle its not allowing null
	 * 
	 * @param name
	 * @param value
	 */
	public void setStringPreference(String name, String value) {
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		if (prefs != null && value != null) {
			prefs.setValue(name, value);
		}
	}

	/**
	 * Brings up a FileDialog to choose an image file.
	 */
	public void openImage() {
		// Open a FileDialog
		FileDialog dlg = new FileDialog(Display.getDefault().getActiveShell(), SWT.OPEN);
		dlg.setFilterPath(initialImagePath);

		String selectedPath = dlg.open();
		String fileName = selectedPath;
		// Save the path for next time
		if (selectedPath != null) {
			// initialImagePath will be set in loadImage
			loadImage(fileName);
		}
	}

	/**
	 * Brings up a FileDialog to choose a calibration file.
	 */
	public void openCalibration() {
		// Open a FileDialog
		FileDialog dlg = new FileDialog(Display.getDefault().getActiveShell(), SWT.OPEN);
		String[] extensions = { "*.calib" };
		String[] names = { "Calibration: *.calib" };
		dlg.setFilterExtensions(extensions);
		dlg.setFilterNames(names);
		dlg.setFilterPath(initialImagePath);

		String selectedPath = dlg.open();
		String fileName = selectedPath;
		if (selectedPath != null) {
			// initialCalibPath will be set in loadCalibFile
			loadCalibFile(fileName);
		}
	}

	/**
	 * Creates a MapCalibration and reads the given fileName as the calibration
	 * file.
	 * 
	 * @param fileName
	 */
	public void loadCalibFile(String fileName) {
		try {
			mapCalibration = new MapCalibration();
			mapCalibration.read(new File(fileName));
			calibFileName = fileName;
			// Save these as startup preferences
			setStringPreference(P_CALIB_FILE_NAME, calibFileName);
			viewer.getCanvas().redraw();
		} catch (Exception ex) {
			SWTUtils.excMsg("Failed to read calibration file", ex);
			mapCalibration = null;
			calibFileName = null;
		}
	}

	/**
	 * Brings up a dialog to edit the current lines.
	 */
	public void editLines() {
		EditLinesDialog dialog = null;
		boolean success = false;
		// Without this try/catch, the application hangs on error
		try {
			dialog = new EditLinesDialog(Display.getDefault().getActiveShell(), this);
			success = dialog.open();
		} catch (Exception ex) {
			SWTUtils.excMsgAsync("Error with EditLinesDialog", ex);
			return;
		}
		if (!success) {
			return;
		}
	}

	/**
	 * Brings up a FileDialog to choose a lines file.
	 */
	public void openLines() {
		// Open a FileDialog
		FileDialog dlg = new FileDialog(Display.getDefault().getActiveShell(), SWT.OPEN);
		String[] extensions = { "*.lines" };
		String[] names = { "Lines: *.lines" };
		dlg.setFilterExtensions(extensions);
		dlg.setFilterNames(names);
		dlg.setFilterPath(initialImagePath);

		String selectedPath = dlg.open();
		String fileName = selectedPath;
		// Save the path for next time
		if (selectedPath != null) {
			try {
				lines.readLines(fileName);
			} catch (Exception ex) {
				SWTUtils.excMsg("Failed to read lines file", ex);
			}
			viewer.getCanvas().redraw();
		}
	}

	/**
	 * Brings up a FileDialog to choose a GPX file for lines.
	 */
	public boolean linesFromGpx() {
		if (mapCalibration == null) {
			SWTUtils.errMsg("Calibration for converting lines is not available");
			return false;
		}
		if (mapCalibration.getTransform() == null) {
			SWTUtils.errMsg("Calibration for converting lines is not valid");
			return false;
		}
		// Open a FileDialog with multiple selection
		FileDialog dlg = new FileDialog(Display.getDefault().getActiveShell(), SWT.MULTI);
		String[] extensions = { "*.gpx" };
		String[] names = { "GPX: *.gpx" };
		dlg.setFilterExtensions(extensions);
		dlg.setFilterNames(names);
		dlg.setFilterPath(initialImagePath);

		String selectedPath = dlg.open();
		String filePath = null, fileName = null;
		// Extract the directory part of the selectedPath
		int index = selectedPath.lastIndexOf(File.separator);
		if (index > 0) {
			filePath = selectedPath.substring(0, index);
		}
		// Save the path for next time
		if (filePath == null)
			return false;
		// Loop over the files
		String[] files = dlg.getFileNames();
		File file;
		for (String name : files) {
			// getFileNames() returns only the names, and not the path
			file = new File(filePath, name);
			fileName = file.getPath();
			boolean res = false;
			try {
				res = lines.readGpxLines(fileName, mapCalibration);
				if (!res) {
					SWTUtils.errMsg("Error reading GpxFile: " + fileName + SWTUtils.LS + "Aborting");
					return false;
				}
			} catch (Exception ex) {
				SWTUtils.excMsg("Failed to read GPX file: " + fileName, ex);
				return false;
			}
		}
		viewer.getCanvas().redraw();
		return true;
	}

	/**
	 * Brings up a FileDialog to choose a file to save lines.
	 */
	public void saveLines() {
		if (lines == null || lines.getLines().isEmpty()) {
			SWTUtils.errMsg("There are no lines defined");
			return;
		}
		// Open a FileDialog
		FileDialog dlg = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
		String[] extensions = { "*.lines" };
		String[] names = { "Lines: *.lines" };
		dlg.setFilterExtensions(extensions);
		dlg.setFilterNames(names);
		dlg.setFilterPath(initialImagePath);

		String selectedPath = dlg.open();
		String fileName = selectedPath;
		// Save the path for next time
		if (selectedPath != null) {
			lines.saveLines(fileName);
		}
	}

	/**
	 * Brings up a FileDialog to choose a file to save lines as an image.
	 */
	public void saveLinesImage(boolean withImage) {
		if (lines == null || lines.getLines().isEmpty()) {
			SWTUtils.errMsg("There are no lines defined");
			return;
		}
		if (viewer.getImage() == null) {
			SWTUtils.errMsg("Image for determining size is not valid");
			return;
		}

		// Get the width and height
		int width, height;
		try {
			Image image = viewer.getImage();
			ImageData imageData = image.getImageData();
			width = imageData.width;
			height = imageData.height;
		} catch (Exception ex) {
			SWTUtils.excMsg("Error getting image data", ex);
			return;
		}
		if (width <= 0 || height <= 0) {
			SWTUtils.errMsg("Image width and/or height is 0");
			return;
		}

		// Open a FileDialog
		FileDialog dlg = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
		String[] extensions = { "*.png" };
		String[] names = { "PNG: *.png" };
		dlg.setFilterExtensions(extensions);
		dlg.setFilterNames(names);
		dlg.setFilterPath(initialImagePath);

		String selectedPath = dlg.open();
		String fileName = selectedPath;
		// Save the path for next time
		if (selectedPath != null) {
			// Check the extension
			String ext = SWTUtils.getExtension(new File(selectedPath));
			if (ext == null || !ext.toLowerCase().equals("png")) {
				SWTUtils.errMsg("Only PNG (.png) is supported");
				return;
			}
			lines.saveLinesImage(display, fileName, width, height, withImage ? viewer.getImage() : null);
		}
	}

	/**
	 * Brings up a FileDialog to choose a GPX file to save tracks.
	 */
	public void saveGPX() {
		if (lines == null) {
			SWTUtils.errMsg("No lines available");
			return;
		}
		if (mapCalibration == null) {
			SWTUtils.errMsg("Calibration for converting lines is not available");
			return;
		}
		if (mapCalibration.getTransform() == null) {
			SWTUtils.errMsg("Calibration for converting lines is not valid");
			return;
		}
		// Open a FileDialog
		FileDialog dlg = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
		String[] extensions = { "*.gpx" };
		String[] names = { "GPX: *.gpx" };
		dlg.setFilterExtensions(extensions);
		dlg.setFilterNames(names);
		dlg.setFilterPath(initialImagePath);

		String selectedPath = dlg.open();
		String fileName = selectedPath;
		// Save the path for next time
		if (selectedPath != null) {
			String trackName = "Map Lines";
			if (imageFileName != null) {
				File file = new File(imageFileName);
				trackName = file.getName();
				int i = trackName.lastIndexOf('.');
				if (i > 0) {
					trackName = trackName.substring(0, i);
				}
			}
			// Prompt for the track name
			InputDialog descDlg = new InputDialog(null, "Description", "Enter a track name:", trackName, null);
			descDlg.setBlockOnOpen(true);
			int res = descDlg.open();
			if (res == Dialog.OK) {
				String val = descDlg.getValue();
				if (val != null) {
					trackName = val;
				}
			}

			GPXUtils.writeGPXFile(fileName, trackName, mapCalibration, lines);
		}
	}

	/**
	 * Brings up a FileDialog to choose a GPSL file to save maps and calibration.
	 */
	public void saveGPSLMap() {
		if (lines == null) {
			SWTUtils.errMsg("No lines available");
			return;
		}
		if (mapCalibration == null) {
			SWTUtils.errMsg("Calibration for converting lines is not available");
			return;
		}
		if (mapCalibration.getTransform() == null) {
			SWTUtils.errMsg("Calibration for converting lines is not valid");
			return;
		}
		if (imageFileName == null || imageFileName.length() == 0) {
			SWTUtils.errMsg("Image file is invalid");
			return;
		}
		// Open a FileDialog
		FileDialog dlg = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
		String[] extensions = { "*.gpsl" };
		String[] names = { "GPSL: *.gpsl" };
		dlg.setFilterExtensions(extensions);
		dlg.setFilterNames(names);
		dlg.setFilterPath(initialImagePath);

		String selectedPath = dlg.open();
		String fileName = selectedPath;
		// Save the path for next time
		if (selectedPath != null) {
			try {
				GPSLUtils.saveGPSLMapFile(fileName, imageFileName, mapCalibration);
			} catch (Throwable t) {
				SWTUtils.excMsg("Failed to create GPSL file", t);
			}
		}
	}

	/**
	 * Brings up a FileDialog to choose a CSV file to save lines.
	 */
	public void saveCSV() {
		if (lines == null) {
			SWTUtils.errMsg("No lines available");
			return;
		}
		if (mapCalibration == null) {
			SWTUtils.errMsg("Calibration for converting lines is not available");
			return;
		}
		if (mapCalibration.getTransform() == null) {
			SWTUtils.errMsg("Calibration for converting lines is not valid");
			return;
		}
		// Open a FileDialog
		FileDialog dlg = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
		String[] extensions = { "*.csv" };
		String[] names = { "CSV: *.csv" };
		dlg.setFilterExtensions(extensions);
		dlg.setFilterNames(names);
		dlg.setFilterPath(initialImagePath);

		String selectedPath = dlg.open();
		String fileName = selectedPath;
		if (selectedPath != null) {
			GPXUtils.writeCSVFile(fileName, mapCalibration, lines);
		}
	}

	/**
	 * Loads a new image from the given filename.
	 * 
	 * @param fileName
	 */
	public void loadImage(String fileName) {
//		 // DEBUG
//		 System.out.println("loadImage:");
//		 double free = Runtime.getRuntime().freeMemory();
//		 double total = Runtime.getRuntime().totalMemory();
//		 double max = Runtime.getRuntime().maxMemory();
//		 System.out.println(String.format(
//		 " Before: Free Memory: %.2f / %.2f (Max %.2f) MB",
//		 free / 1024. / 1024., total / 1024. / 1024., max / 1024. / 1024.));
		if (fileName == null) {
			return;
		}
		Image newImage = null;
		try {
			// Do this to conserve memory
			Image oldImage = viewer.getImage();
			if (oldImage != null && !oldImage.isDisposed()) {
				oldImage.dispose();
			}
			viewer.setImage(null);
//			 // DEBUG
//			 free = Runtime.getRuntime().freeMemory();
//			 total = Runtime.getRuntime().totalMemory();
//			 max = Runtime.getRuntime().maxMemory();
//			 System.out.println(String.format(
//			 " After Dispose Old Image: Free Memory: %.2f / %.2f (Max %.2f) MB",
//			 free / 1024. / 1024., total / 1024. / 1024., max / 1024. / 1024.));
			newImage = new Image(display, fileName);
			shell.setText(fileName);
			imageFileName = fileName;
			// Extract the directory part of the selectedPath
			int index = fileName.lastIndexOf(File.separator);
			if (index > 0) {
				initialImagePath = fileName.substring(0, index);
			}
			// Save these as startup preferences
			setStringPreference(P_IMAGE_FILE_NAME, imageFileName);
			setStringPreference(P_INITIAL_IMAGE_PATH, initialImagePath);
			viewer.setImage(newImage);
		} catch (Throwable t) {
			SWTUtils.excMsgAsync(shell, "Cannot load image from:\n" + fileName, t);
			imageFileName = null;
			if (newImage != null && !newImage.isDisposed()) {
				newImage.dispose();
				newImage = null;
			}
		}
//		 // DEBUG
//		 free = Runtime.getRuntime().freeMemory();
//		 total = Runtime.getRuntime().totalMemory();
//		 max = Runtime.getRuntime().maxMemory();
//		 System.out.println(String.format(
//		 " After: Free Memory: %.2f / %.2f (Max %.2f) MB",
//		 free / 1024. / 1024., total / 1024. / 1024., max / 1024. / 1024.));
	}

	/**
	 * Adds a menu listener to hook the context menu when it is invoked.
	 * 
	 * @param control
	 */
	private void hookContextMenu(Control control) {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		Menu menu = menuMgr.createContextMenu(control);
		control.setMenu(menu);
		getSite().registerContextMenu(menuMgr, null);
	}

	/**
	 * Creates handlers.
	 */
	protected void createHandlers() {
		// IHandlerService from the workbench is the global handler service. it
		// provides no special activation scoping or lifecycle.

		// IHandlerService from the workbench window is the window handler
		// service. Any handlers activated through the window handler service
		// will be active when that window is active. Any listeners added to the
		// window handler service will be removed when the window is disposed,
		// and any active handlers will be deactivated (but not disposed).

		// IHandlerService from the workbench part site is the part handler
		// service. Any handlers activated through the part handlers service
		// will only be active when that part is active. Any listeners added to
		// the part handler service will be removed when the part is disposed,
		// and any active handlers will be deactivated (but not disposed).

		// Get the handler service from the view site
		IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);

		AbstractHandler handler;
		String id;

		// Open Image
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				openImage();
				return null;
			}
		};
		id = "net.kenevans.maplines.openimage";
		handlerService.activateHandler(id, handler);

		// Open Calibration
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				openCalibration();
				return null;
			}
		};
		id = "net.kenevans.maplines.opencalibration";
		handlerService.activateHandler(id, handler);

		// Open Lines
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				openLines();
				return null;
			}
		};
		id = "net.kenevans.maplines.openlines";
		handlerService.activateHandler(id, handler);

		// Open GPX lines
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				linesFromGpx();
				return null;
			}
		};
		id = "net.kenevans.maplines.linesfromgpx";
		handlerService.activateHandler(id, handler);

		// Edit Lines
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				editLines();
				return null;
			}
		};
		id = "net.kenevans.maplines.editlines";
		handlerService.activateHandler(id, handler);

		// Save Lines
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				saveLines();
				return null;
			}
		};
		id = "net.kenevans.maplines.savelines";
		handlerService.activateHandler(id, handler);

		// Save Lines Image
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				saveLinesImage(false);
				return null;
			}
		};
		id = "net.kenevans.maplines.savelinesimage";
		handlerService.activateHandler(id, handler);

		// Save Lines With Image
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				saveLinesImage(true);
				return null;
			}
		};
		id = "net.kenevans.maplines.savelineswithimage";
		handlerService.activateHandler(id, handler);

		// Clear Lines
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				clearLines();
				return null;
			}
		};
		id = "net.kenevans.maplines.clearlines";
		handlerService.activateHandler(id, handler);

		// Calibration Lines
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				calibrationLines();
				return null;
			}
		};
		id = "net.kenevans.maplines.calibrationlines";
		handlerService.activateHandler(id, handler);

		// Delete Current Line
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				deleteLastPoint();
				return null;
			}
		};
		id = "net.kenevans.maplines.deletelastpoint";
		handlerService.activateHandler(id, handler);

		// Save GPX
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				saveGPX();
				return null;
			}
		};
		id = "net.kenevans.maplines.savegpx";
		handlerService.activateHandler(id, handler);

		// Save GPSL
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				saveGPSLMap();
				return null;
			}
		};
		id = "net.kenevans.maplines.savegpsl";
		handlerService.activateHandler(id, handler);

		// Save CSV
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				saveCSV();
				return null;
			}
		};
		id = "net.kenevans.maplines.savecsv";
		handlerService.activateHandler(id, handler);

		// Start Line
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				startLine();
				return null;
			}
		};
		id = "net.kenevans.maplines.startline";
		handlerService.activateHandler(id, handler);

		// End Line
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws ExecutionException {
				endLine();
				return null;
			}
		};
		id = "net.kenevans.maplines.endline";
		handlerService.activateHandler(id, handler);
	}

	/**
	 * Starts a new line.
	 */
	public void startLine() {
		if (lines == null || viewer == null) {
			return;
		}
		Line line = new Line();
		lines.addLine(line);
		viewer.setCurLine(line);
		line.setDesc("Line " + nextLineNumber++);
		// // Prompt for the description
		// InputDialog dlg = new InputDialog(null, "Description",
		// "Enter a description:", "Line " + nextLineNumber, null);
		// dlg.setBlockOnOpen(true);
		// int res = dlg.open();
		// if(res == Dialog.OK) {
		// String val = dlg.getValue();
		// if(val != null) {
		// line.setDesc(val);
		// nextLineNumber++;
		// }
		// }

		// // DEBUG
		// System.out.println("startLine");
		// System.out.println("curLine=" + viewer.getCurLine());
		// System.out.println(lines.info());
	}

	/**
	 * Clears the lines.
	 */
	public void clearLines() {
		if (viewer == null) {
			return;
		}
		endLine();
		lines.clear();
		viewer.getCanvas().redraw();
		// // DEBUG
		// System.out.println("endLine");
		// System.out.println("curLine=" + viewer.getCurLine());
		// System.out.println(lines.info());
	}

	/**
	 * Makes lines corresponding to the calibration points.
	 */
	public void calibrationLines() {
		if (lines == null || viewer == null) {
			return;
		}
		if (mapCalibration == null) {
			SWTUtils.errMsg("Calibration is not available");
			return;
		}
		if (mapCalibration.getTransform() == null) {
			SWTUtils.errMsg("Calibration is not valid");
			return;
		}
		Line line = new Line();
		lines.addLine(line);
		viewer.setCurLine(line);
		for (MapData data : mapCalibration.getDataList()) {
			line.addPoint(new Point(data.getX(), data.getY()));
		}
		if (mapCalibration.getDataList().size() > 1) {
			MapData data = mapCalibration.getDataList().get(0);
			line.addPoint(new Point(data.getX(), data.getY()));
		}
		viewer.getCanvas().redraw();

		// Prompt for the description
		InputDialog dlg = new InputDialog(null, "Description", "Enter a description:", "Calibration Lines", null);
		dlg.setBlockOnOpen(true);
		int res = dlg.open();
		if (res == Dialog.OK) {
			String val = dlg.getValue();
			if (val != null) {
				line.setDesc(val);
				nextLineNumber++;
			}
		}
	}

	/**
	 * Deletes the current line.
	 */
	public void deleteLastPoint() {
		if (viewer == null) {
			return;
		}
		if (viewer.getCurLine() == null) {
			SWTUtils.errMsg("There is no current line active");
			return;
		}
		viewer.getCurLine().deleteLastPoint();
		viewer.getCanvas().redraw();
		// // DEBUG
		// System.out.println("endLine");
		// System.out.println("curLine=" + viewer.getCurLine());
		// System.out.println(lines.info());
	}

	/**
	 * Ends a line line.
	 */
	public void endLine() {
		if (viewer == null) {
			return;
		}
		viewer.setCurLine(null);
		// // DEBUG
		// System.out.println("endLine");
		// System.out.println("curLine=" + viewer.getCurLine());
		// System.out.println(lines.info());
	}

	/**
	 * @return The value of mapCalibration.
	 */
	public MapCalibration getMapCalibration() {
		return mapCalibration;
	}

	/**
	 * @return The value of lines.
	 */
	public Lines getLines() {
		return lines;
	}

	/**
	 * @return The value of viewer.
	 */
	public SWTImageViewerControl getViewer() {
		return viewer;
	}

}