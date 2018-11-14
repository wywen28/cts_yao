package unittest.gui;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class LayoutUtil {
	//使弹出的窗口在屏幕中央显示
	public static void centerShell(Display display,Shell shell){ 
		Rectangle displayBounds = display.getPrimaryMonitor().getBounds(); 
		Rectangle shellBounds = shell.getBounds(); 
		int x = displayBounds.x + (displayBounds.width - shellBounds.width)>>1; 
		int y = displayBounds.y + (displayBounds.height - shellBounds.height)>>1; 
		shell.setLocation(x, y); 
	} 
}
