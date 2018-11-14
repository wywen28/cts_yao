package unittest.gui;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.eclipse.swt.widgets.Shell;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.util.Config;

public class UATLanguageSettingGUI 
{

	private Shell shell = null;
	private Display  display = null;
	public UATGUI uatGui = null;
	
	
	private CLabel infoCLabel = null;
	private Combo languageCombox = null; 
	private Button okButton = null;
	private Button cancelButton = null;
	
	private Composite topComposite = null;
	private Composite bottomComposite = null;
	private Composite controlComposite = null;

	public UATLanguageSettingGUI(UATGUI uatgui) 
	{
		this.uatGui = uatgui;
	}

	public void go() 
	{
		display = Display.getDefault();
		
		this.createShell();
		this.dealEvent();
		this.shell.open();
		
		while( !display.isDisposed() ) 
		{
			if( !display.readAndDispatch() ) 
			{
				display.sleep();
			}
		}
		display.dispose();
		
	}
	private void dealEvent() 
	{
		
		shell.addShellListener( new ShellCloseListener( this ) );
		
		okButton.addSelectionListener( new OkButtonListener( this )) ;
		
		cancelButton.addSelectionListener( new CancelButtonListener( this ) );
	}


	private void createShell() 
	{
		shell = new Shell( SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL );
		shell.setText( GUILanguageResource.getProperty("Language") );
		shell.setBounds( 200, 200, 250, 175 );
		shell.setLayout( null );
		
		topComposite = WidgetFactory.createComposite( shell, SWT.FLAT );
		topComposite.setBackground( Resource.backgroundColor );
		topComposite.setLayout( new FillLayout() );
		topComposite.setBounds( 5, 5, 240, 50);
		
		infoCLabel = WidgetFactory.createCLabel( topComposite, SWT.BORDER,
				GUILanguageResource.getProperty("LanguageSettingInfo"));
		infoCLabel.setBackground( new Color( null, 230, 250, 175) );
		infoCLabel.setFont( Resource.courierNew_8_Font );
		infoCLabel.setBounds( 0, 0, 240, 40 );
		
		bottomComposite = WidgetFactory.createComposite( shell, SWT.BORDER );
		bottomComposite.setBounds( 5, 56, 240, 50);
		bottomComposite.setLayout( null );
		
		languageCombox = WidgetFactory.createCombo(bottomComposite, new String[]{"简体中文","English"});
		
		languageCombox.setBounds( 50, 15, 150, 30 );
		// set the default selection of the language
		if(Config.Language.equals("zh_CN"))
			languageCombox.select(0);
		else
			languageCombox.select(1);
		
		controlComposite = WidgetFactory.createComposite( shell, SWT.NONE );
		controlComposite.setLayout( null );
		controlComposite.setBounds( 5, 100, 240, 50 );
		
		okButton = WidgetFactory.createButton( controlComposite, SWT.PUSH );
		okButton.setText( GUILanguageResource.getProperty("OK") );
		okButton.setFont( Resource.courierNew_10_Font );
		cancelButton = WidgetFactory.createButton( controlComposite, SWT.PUSH );
		okButton.setBounds( 15, 15, 60, 30 );
		cancelButton.setBounds( 160, 15, 60, 30 );
		cancelButton.setText( GUILanguageResource.getProperty("Cancel") );
		cancelButton.setFont( Resource.courierNew_10_Font );
	}

	/**
	 * This is the SelectionListener of Ok Button.
	 * @author joaquin(孙华衿)
	 *
	 */
	private class OkButtonListener extends SelectionAdapter 
	{
		private UATLanguageSettingGUI demo;
		public OkButtonListener( UATLanguageSettingGUI demo ) 
		{
			this.demo = demo;
		}

		public void widgetSelected( SelectionEvent e ) 
		{
			//to do  when the languge is changed 
			// the config file should be changed and after
			
			if(languageCombox.getSelectionIndex()==0){
				Config.Language = "zh_CN";
			}
			else if(languageCombox.getSelectionIndex()==1){
				Config.Language = "en_US";
			}
			try {
				Config.updateConfigFile("./config/config.xml");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			demo.uatGui.getShell().setEnabled( true );
			demo.shell.dispose();
			
			
		}

		
	}
	/**
	 * This is the SelectionListener for Cancel Button.
	 * @author joaquin()
	 *
	 */
	private class CancelButtonListener extends SelectionAdapter 
	{
		private UATLanguageSettingGUI demo;
		public CancelButtonListener( UATLanguageSettingGUI demo ) 
		{
			this.demo = demo;
		}
		public void widgetSelected( SelectionEvent e ) 
		{
			demo.uatGui.getShell().setEnabled( true );
			demo.shell.dispose();
		}
	}
	
	/**
	 * This is the ShellListener of UATLanguageSettingGUI.
	 * @author joaquin(孙华衿)
	 *
	 */
	public class ShellCloseListener extends ShellAdapter 
	{
		private UATLanguageSettingGUI demo;
		public ShellCloseListener( UATLanguageSettingGUI demo ) 
		{
			this.demo = demo;
		} 
		
		public void shellClosed( ShellEvent e ) 
		{
			demo.uatGui.getShell().setEnabled( true );
			demo.shell.dispose();
		}
		
	}

}
