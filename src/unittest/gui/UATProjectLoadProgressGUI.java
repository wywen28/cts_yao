package unittest.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;

public class UATProjectLoadProgressGUI {
	public Shell shell = null;
	public Display display = null;
	private String message = "’˝‘⁄÷¥––£¨«Î…‘∫Ú";
	
	private Composite composite = null;
	
	private ProgressBar bar = null;
	private CLabel label = null;
	
	public UATProjectLoadProgressGUI(){
	}
	
	public UATProjectLoadProgressGUI(String message){
		if(message != null)
			this.message = message;
	}
	
	public void go(){
		display = Display.getDefault();
		this.createShell();
		this.dealEvent();
		this.shell.open();
		LayoutUtil.centerShell(display, shell);
		
		while( !display.isDisposed() ) 
		{
			if( !display.readAndDispatch() ) 
			{
				display.sleep();
			}
		}
		display.dispose();
	}
	
	private void createShell(){
		shell = new Shell(SWT.BORDER | SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL );
		shell.setText( message);
		shell.setImage( Resource.UATImage );
		shell.setBounds( 50, 50, 300, 100);
		shell.setLayout( new FormLayout() );
		shell.setMaximized( false );
		createComposite();
	}
	
	public void dealEvent(){
	}
	
	private void createComposite(){
		composite = WidgetFactory.createComposite( shell, SWT.BORDER );
		composite.setLayout( new FormLayout() );		
		WidgetFactory.configureFormData( composite, new FormAttachment( 0, 5 ),
				new FormAttachment( 0, 5 ),
				new FormAttachment( 100, -5 ),
				new FormAttachment( 100, -5 ));
		bar = new ProgressBar(composite,SWT.HORIZONTAL|SWT.INDETERMINATE);
		label = WidgetFactory.createCLabel(composite, 0, message);
		WidgetFactory.configureFormData( label, new FormAttachment(30,100, 5 ),
				new FormAttachment( 20,100, 0 ),
				null,
				null);
		WidgetFactory.configureFormData( bar, new FormAttachment(20,100, 5),
				new FormAttachment( 50,100, 5 ),
				null,
				null);
	}
	
	public void close(){
		if(!shell.isDisposed())
			shell.dispose();
	}

}
