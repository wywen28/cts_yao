package unittest.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import unittest.gui.helper.Resource;


/**
 * This Class is the About Frame of UATGui
 * @author joaquin(孙华衿)
 * @see UATGui
 */
public class UATAboutGUI {

	private Shell sShell = null;  
	private CLabel cLabel = null;
	private CLabel cLabel1 = null;
	private Button button = null;

	public UATGUI uatGui;
	public UATAboutGUI( UATGUI uatGui) {
		this.uatGui = uatGui;
	}

	/**
	 * CodemonGui call this function to show the about Frame.
	 *@author joaquin(孙华衿)
	 */
	public void go() {
		Display display = Display.getDefault();
		this.createSShell();
		this.dealEvent();
		this.sShell.open();

		while (!display.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
	
	/**
	 * This method initializes sShell
	 * @author joaquin(孙华衿)
	 */
	private void createSShell() {
		sShell = new Shell( SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL );
		sShell.setText("关于 CTS");
		sShell.setLocation( 250, 250 );
		sShell.setSize(new org.eclipse.swt.graphics.Point(400,230));
		
		cLabel = new CLabel(sShell, SWT.NONE );
		cLabel.setText("");
		cLabel.setImage( Resource.aboutImage );
		cLabel.setBounds(new org.eclipse.swt.graphics.Rectangle(7,50,45,45));
		cLabel1 = new CLabel(sShell, SWT.NONE);
		cLabel1.setText("CTS Version 1.0 测试版 Copyright@2009 北京邮电大学\n" );
		cLabel1.setBounds(new org.eclipse.swt.graphics.Rectangle(55,60,330,45));
		button = new Button(sShell, SWT.NONE);
		button.setBounds(new org.eclipse.swt.graphics.Rectangle(145,150,101,25));
		button.setText("关闭");
	}
	
	/**
	 * This function deal the event of CodemonAboutGui.
	 */
	public void dealEvent() {
		sShell.addShellListener( new ShellCloseListener( this ) );
		button.addSelectionListener( new OkButtonListener( this ) );
	}
	/**
	 * This is the SelectionListener of the Ok Button
	 * @author joaquin(孙华衿)
	 *
	 */
	public class OkButtonListener extends SelectionAdapter {
		private UATAboutGUI demo;
		public OkButtonListener( UATAboutGUI demo ) {
			this.demo = demo;
		}
		
		public void widgetSelected( SelectionEvent e ) {
			demo.uatGui.getShell().setEnabled( true );
			demo.sShell.dispose();
		}
	}
	
	/**
	 * This is the ShellListener of CodemonNewProjectGui.
	 * @author joaquin(孙华衿)
	 *
	 */
	public class ShellCloseListener extends ShellAdapter {
		private UATAboutGUI demo;
		public ShellCloseListener( UATAboutGUI demo ) {
			this.demo = demo;
		} 
		
		public void shellClosed( ShellEvent e ) {
			demo.uatGui.getShell().setEnabled( true );
			demo.sShell.dispose();
		}
		
	}
}

