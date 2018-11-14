package unittest.gui;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;


public class UATTipsGUI 
{
	private static final String Message = null;
	private Shell shell = null;
	private Display  display = null;
	private CLabel infoCLabel = null;
	private Composite topComposite;
	private String tips;
	public UATGUI uatGui;
	
	public UATTipsGUI(UATGUI Gui,String message)
	{
		uatGui = Gui;
		tips = message;
	}
	public UATTipsGUI(String message)
	{
		uatGui = null;
		tips = message;
	}
	public void dispose()
	{
		shell.dispose();
	}
	public void go() 
	{
		display = Display.getDefault();
		
		this.createShell();
		
		this.shell.open();
		
		/*while( !display.isDisposed() ) 
		{
			if( !display.readAndDispatch() ) 
			{
				display.sleep();
			}
		}
		display.dispose();*/
	}
	
	public void dealEvent() 
	{
		shell.addShellListener( new ShellCloseListener( this ) );
	}
	
	/**
	 * This is the ShellListener of UATTipsGUI.
	 * @author sunhuajin
	 *
	 */
	public class ShellCloseListener extends ShellAdapter {
		private UATTipsGUI demo;
		public ShellCloseListener( UATTipsGUI demo ) {
			this.demo = demo;
		} 
		
		public void shellClosed( ShellEvent e ) {
			demo.uatGui.getShell().setEnabled( true );
			demo.shell.dispose();
		}
		
	}

	private void createShell() 
	{
		shell = new Shell(SWT.TITLE | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL);
		shell.setText( "Tips" );
		shell.setBounds( 600, 400, 200, 100 );
		shell.setLayout( new FillLayout() );
		//display.s
		
		topComposite = WidgetFactory.createComposite( shell, SWT.FLAT );
		//topComposite.setBackground( Resource.backgroundColor );
		topComposite.setLayout( new FillLayout() );
		//topComposite.setBounds( 40, 5, 150, 70);
		
		infoCLabel = WidgetFactory.createCLabel( topComposite, SWT.BORDER,tips);
		infoCLabel.setFont( Resource.courierNew_10_Font );
		//infoCLabel.setBounds( 40, 5, 150, 70 );
		infoCLabel.setLayout(new FillLayout());
		
	}
	
	public static void main(String args[])
	{
		UATTipsGUI u = new UATTipsGUI("正在模块划分........");
		u.go();
		
		
		//u.dispose();
	}
	
	

}
