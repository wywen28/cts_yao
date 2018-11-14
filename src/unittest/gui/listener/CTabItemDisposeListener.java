package unittest.gui.listener;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

import unittest.gui.UATGUI;
import unittest.gui.helper.FileCTabItem;

/**
 * The Dispose Listener for the CTabItem of codeTabFolder.
 * @author joaquin(Ëï»ªñÆ)
 * @see CodemonGui
 */
public class CTabItemDisposeListener implements DisposeListener 
{
	

	private FileCTabItem fcti;
	private UATGUI demo;
	public CTabItemDisposeListener( UATGUI demo, FileCTabItem fcti ) 
	{
		this.demo = demo;
		this.fcti = fcti;
	}
	public void widgetDisposed( DisposeEvent e ) 
	{
		demo.items.remove( fcti );
	}
}