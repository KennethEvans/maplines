package net.kenevans.maplines.handlers;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.Bundle;

import net.kenevans.maplines.plugin.Activator;
import net.kenevans.maplines.plugin.IPreferenceConstants;
import net.kenevans.maplines.utils.SWTUtils;

public class HelpContentsHandler extends AbstractHandler {
	public static final String LS = System.getProperty("line.separator");

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.
	 * commands .ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		if (window == null) {
			SWTUtils.errMsg("Cannot determine the workbench window");
			return null;
		}

		boolean success = false;
		WebPageDialog dialog = null;

		// Find the HTML file
		Bundle plugin = Activator.getDefault().getBundle();
		String urlString = null;
		String html = "help/overview.html";
		IPath relativePagePath = new Path(html);
		URL fileInPlugin = FileLocator.find(plugin, relativePagePath, null);
		URL pageUrl;
		try {
			pageUrl = FileLocator.toFileURL(fileInPlugin);
		} catch (IOException ex) {
			SWTUtils.excMsgAsync("Failed to convert " + html, ex);
			return null;
		}
		if (pageUrl == null) {
			SWTUtils.errMsgAsync("Failed to find " + html);
			return null;
		}
		urlString = pageUrl.toString();

		// Without this try/catch, the application hangs on error
		try {
			dialog = new WebPageDialog(Display.getDefault().getActiveShell(), urlString);
			success = dialog.open();
		} catch (Exception ex) {
			SWTUtils.excMsgAsync("Error with WebPageDialog", ex);
			return null;
		}
		if (!success) {
			return null;
		}

		return success;
	}

	public class WebPageDialog extends Dialog implements IPreferenceConstants {
		private String url;
		private boolean success = false;

		/**
		 * Constructor.
		 * 
		 * @param parent
		 */
		public WebPageDialog(Shell parent, String url) {
			// We want this to be modeless
			this(parent, SWT.DIALOG_TRIM | SWT.NONE);
			this.url = url;
		}

		/**
		 * Constructor.
		 * 
		 * @param parent The parent of this dialog.
		 * @param style  Style passed to the parent.
		 */
		public WebPageDialog(Shell parent, int style) {
			super(parent, style);
		}

		/**
		 * Convenience method to open the dialog.
		 * 
		 * @return Whether OK was selected or not.
		 */
		public boolean open() {
			Shell shell = new Shell(getParent(), getStyle() | SWT.RESIZE);
			shell.setText("Overview");
			// It can take a long time to do this so use a wait cursor
			// Probably not, though
			Cursor waitCursor = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
			if (waitCursor != null)
				getParent().setCursor(waitCursor);
			createContents(shell);
			getParent().setCursor(null);
			waitCursor.dispose();
			shell.setSize(800, 800);
			shell.pack();
			shell.open();
			Display display = getParent().getDisplay();
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
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

			// Create a web browser
			Browser browser = new Browser(shell, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(browser);

			// Set the url
			browser.setUrl(url);
		}

	}

}
