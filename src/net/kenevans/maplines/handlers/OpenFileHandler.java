package net.kenevans.maplines.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import net.kenevans.maplines.ui.MapLinesView;
import net.kenevans.maplines.utils.SWTUtils;

/*
 * Created on Aug 17, 2013
 * By Kenneth Evans, Jr.
 */

public class OpenFileHandler extends AbstractHandler
{
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.
     * commands .ExecutionEvent)
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        if(window == null) {
            SWTUtils.errMsg("Cannot determine the workbench window");
            return null;
        }

        // Find the MapLinesView
        MapLinesView view = null;
        try {
            view = (MapLinesView)window.getActivePage()
                .findView(MapLinesView.ID);
            if(view == null) {
                SWTUtils.errMsgAsync("Cannot find MapLinesView");
                return null;
            }
        } catch(Exception ex) {
            SWTUtils.excMsgAsync("Error finding MapLinesView", ex);
        }
        if(view == null) {
            SWTUtils.errMsgAsync("MapLinesView is null");
            return null;
        }

        // Run the method
        view.openImage();

        // Must currently be null
        return null;
    }

}
