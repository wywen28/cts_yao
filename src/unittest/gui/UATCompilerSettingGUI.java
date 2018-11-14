package unittest.gui;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.xml.sax.SAXException;

import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.util.Config;


/**
 * This GUI will be shown when setting the c++ compiler .
 * @author joaquin(孙华衿)
 */
public class UATCompilerSettingGUI 
{
	private Shell shell = null;
	private Display  display = null;
	public UATGUI uatGui = null;
	
	private String compilerPath;
	private String linkExe;
	private String compileExe;
	private String editorExe;
	
	private Text compilerPathText;
	private Text linkExeText;
	private Text compileExeText;
	private Text editorExeText;
	
	private Color backgroundColor = null;
	
	private CLabel infoCLabel = null;
	private Button okButton = null;
	private Button cancelButton = null;
	
	private Button compilerPathButton = null;
	private Button linkExePathButton = null;
	private Button compileExePathButton = null;
	private Button editorExePathButton = null;
	
	private Composite topComposite;
	private Composite bottomComposite;
	private Composite controlComposite;
	
	private CLabel compileExePathCLabel;
	private CLabel linkExePathCLabel;
	private CLabel editorExePathCLabel;
	private CLabel compilerPathCLabel;
	
	public UATCompilerSettingGUI(UATGUI uatgui)
	{
		this.uatGui = uatgui;
	}


	/**
	 * The UATGui call this function to show the NewProject GUI. 
	 */
	public void go() 
	{
		display = Display.getDefault();
		this.createResource();
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
		compilerPathButton.addSelectionListener( new SelectionAdapter() 
		{
			public void widgetSelected( SelectionEvent e ) 
			{
				FileDialog fd = WidgetFactory.createFileDialog( shell );
				fd.setText( "选择 VSVAR32.bat " );
				fd.setFilterExtensions( new String[]{ "*.bat" } );
				String path = "";
				path = fd.open();
				
				if( path != null ) 
				{
					compilerPathText.setText( path ); 
				}
			}
		});
		
		linkExePathButton.addSelectionListener( new SelectionAdapter() 
		{
			public void widgetSelected( SelectionEvent e ) 
			{
				FileDialog fd = WidgetFactory.createFileDialog( shell );
				fd.setText( "选择 Link.exe " );
				fd.setFilterExtensions( new String[]{ "link.exe" } );

				String path = fd.open();
				if( path != null ) 
				{
					linkExeText.setText( path ); 
				}
			}
		});
		
		editorExePathButton.addSelectionListener( new SelectionAdapter() 
		{
			public void widgetSelected( SelectionEvent e ) 
			{
				FileDialog fd = WidgetFactory.createFileDialog( shell );
				fd.setText( "选择 MSDEV.exe " );
				fd.setFilterExtensions( new String[]{ "MSDEV.exe" ,"devenv.exe"} );

				String path = fd.open();
				if( path != null ) 
				{
					editorExeText.setText( path ); 
				}
			}
		});
		
		compileExePathButton.addSelectionListener( new SelectionAdapter() 
		{
			public void widgetSelected( SelectionEvent e ) 
			{
				FileDialog fd = WidgetFactory.createFileDialog( shell );
				fd.setText( "选择 cl.exe " );
				fd.setFilterExtensions( new String[]{ "cl.exe"} );

				String path = fd.open();
				if( path != null ) 
				{
					compileExeText.setText( path ); 
				}
			}
		});
		
		okButton.addSelectionListener( new OkButtonListener( this )) ;
		
		cancelButton.addSelectionListener( new CancelButtonListener( this ) );
	}


	private void createShell() 
	{
		shell = new Shell( SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL );
		shell.setText( GUILanguageResource.getProperty("Compiler") );
		shell.setBounds( 250, 250, 430, 305 );
		shell.setLayout( null );
		
		topComposite = WidgetFactory.createComposite( shell, SWT.FLAT );
		topComposite.setBackground( Resource.backgroundColor );
		topComposite.setLayout( new FillLayout() );
		topComposite.setBounds( 5, 5, 410, 50);
		
		infoCLabel = WidgetFactory.createCLabel( topComposite, SWT.BORDER,
				GUILanguageResource.getProperty("CompilerSettingInfo"));
		infoCLabel.setBackground( backgroundColor );
		infoCLabel.setFont( Resource.courierNew_10_Font );
		infoCLabel.setBounds( 0, 0, 410, 40 );
		
		bottomComposite = WidgetFactory.createComposite( shell, SWT.BORDER );
		bottomComposite.setBounds( 5, 56, 410, 160);
		bottomComposite.setLayout( null );
		
		compilerPathCLabel = WidgetFactory.createCLabel( bottomComposite, SWT.FLAT,
				GUILanguageResource.getProperty("VSVAR32Dir") );
		compilerPathCLabel.setFont( Resource.courierNew_10_Font );
		
		editorExePathCLabel = WidgetFactory.createCLabel( bottomComposite, SWT.FLAT, 
				GUILanguageResource.getProperty("MSDEVDir") );
		editorExePathCLabel.setFont( Resource.courierNew_10_Font );
		
		linkExePathCLabel = WidgetFactory.createCLabel( bottomComposite, SWT.FLAT, 
				GUILanguageResource.getProperty("LINKDir"));
		linkExePathCLabel.setFont( Resource.courierNew_10_Font );
		
		compileExePathCLabel = WidgetFactory.createCLabel( bottomComposite, SWT.FLAT, 
				GUILanguageResource.getProperty("CLDir") );
		compileExePathCLabel.setFont( Resource.courierNew_10_Font );
		
		compilerPathText = WidgetFactory.createText( bottomComposite, SWT.SINGLE | SWT.BORDER, compilerPath, true );
		compilerPathText.setFont( Resource.courierNew_10_Font );
		
		editorExeText = WidgetFactory.createText( bottomComposite, SWT.SINGLE | SWT.BORDER, editorExe, true );
		editorExeText.setFont( Resource.courierNew_10_Font );
		
		compileExeText = WidgetFactory.createText( bottomComposite, SWT.SINGLE | SWT.BORDER, compileExe, true );
		compileExeText.setFont( Resource.courierNew_10_Font );
		
		linkExeText = WidgetFactory.createText( bottomComposite, SWT.SINGLE | SWT.BORDER, linkExe, true );
		linkExeText.setFont( Resource.courierNew_10_Font );
		
		compilerPathButton = WidgetFactory.createButton( bottomComposite, SWT.PUSH, GUILanguageResource.getProperty("Browse") );
		compilerPathButton.setFont( Resource.courierNew_8_Font );

		linkExePathButton = WidgetFactory.createButton( bottomComposite, SWT.PUSH, GUILanguageResource.getProperty("Browse") );
		linkExePathButton.setFont( Resource.courierNew_8_Font );
		
		compileExePathButton = WidgetFactory.createButton( bottomComposite, SWT.PUSH, GUILanguageResource.getProperty("Browse") );
		compileExePathButton.setFont( Resource.courierNew_8_Font );

		editorExePathButton = WidgetFactory.createButton( bottomComposite, SWT.PUSH, GUILanguageResource.getProperty("Browse") );
		editorExePathButton.setFont( Resource.courierNew_8_Font );
		
		compilerPathCLabel.setBounds( 5, 5, 150, 30 );
		compileExePathCLabel.setBounds( 5, 40, 150, 30 );
		linkExePathCLabel.setBounds( 5, 75, 150, 30 );
		editorExePathCLabel.setBounds( 5, 110, 150, 30 );
		
		compilerPathText.setBounds( 160, 10, 180, 25 );
		compileExeText.setBounds( 160, 45, 180, 25 );
		linkExeText.setBounds( 160, 80, 180, 25 );
		editorExeText.setBounds( 160, 115, 180, 25 );
		
		compilerPathButton.setBounds( 345, 10, 60, 34 );
		compileExePathButton.setBounds( 345, 45, 60, 34 );
		linkExePathButton.setBounds( 345, 75, 60, 34 );
		editorExePathButton.setBounds( 345, 110, 60, 34 );
		
		controlComposite = WidgetFactory.createComposite( shell, SWT.NONE );
		controlComposite.setLayout( null );
		controlComposite.setBounds( 5, 220, 370, 50 );
		okButton = WidgetFactory.createButton( controlComposite, SWT.PUSH );
		okButton.setText( GUILanguageResource.getProperty("OK"));
		okButton.setFont( Resource.courierNew_10_Font );
		cancelButton = WidgetFactory.createButton( controlComposite, SWT.PUSH );
		okButton.setBounds( 15, 5, 60, 30 );
		cancelButton.setBounds( 305, 5, 60, 30 );
		cancelButton.setText( GUILanguageResource.getProperty("Cancel") );
		cancelButton.setFont( Resource.courierNew_10_Font );
	}


	private void createResource()
	{
		backgroundColor = new Color( null, 230, 250, 175);
		compilerPath = Config.VSVAR32S;
		linkExe = Config.LinkToolPath;
		compileExe =Config.CompileToolPath;
		editorExe = Config.VCEXEPath;
	}
	
	/**
	 * This function check the validity of compilerPath, linkExe, compileExe and 
	 * editorExe Path.
	 * @return a wrong message if there are some errors, else return "";
	 */
	public String checkValidity() 
	{
		String errorMsg = "";

		if( new File( compilerPath ).exists() != true )
		{
			errorMsg += "Project Path does not exist;\n";
		}
		
		if(new File(linkExe).exists() != true)
		{
			errorMsg += linkExe  + " not Exsits;\n";
		}
		
		if(new File(compileExe).exists() != true)
		{
			errorMsg += compileExe  + " not Exsits;\n";
		}
		
		if(new File(editorExe).exists() != true)
		{
			errorMsg += editorExe  + " not Exsits;\n";
		}
		
		
		
		return errorMsg;
	}
	
	/**
	 * This is the SelectionListener of Ok Button.
	 * @author joaquin(孙华衿)
	 *
	 */
	private class OkButtonListener extends SelectionAdapter 
	{
		private UATCompilerSettingGUI demo;
		public OkButtonListener( UATCompilerSettingGUI demo ) 
		{
			this.demo = demo;
		}

		public void widgetSelected( SelectionEvent e ) 
		{
			compilerPath = compilerPathText.getText().trim();
			linkExe = linkExeText.getText().trim();
			compileExe = compileExeText.getText().trim();
			editorExe = editorExeText.getText().trim();
			String errorMsg = checkValidity();
			if( errorMsg.equals( "" ) ) 
			{
				doCompilerConfig();
				demo.uatGui.getShell().setEnabled( true );

				//demo.uatGui.doRefresh();

				demo.shell.dispose();
			} 
			else 
			{
				MessageBox mb = WidgetFactory.createMessageBox( shell, SWT.ICON_ERROR | SWT.OK, "错误信息", errorMsg );
				mb.open();
			}
			
		}

		
	}
	
	private void doCompilerConfig() 
	{
		Config.CompileToolPath = compileExe.replace("\\", "\\\\");
		Config.LinkToolPath = linkExe.replace("\\", "\\\\");
		Config.VCEXEPath = editorExe.replace("\\", "\\\\");
		Config.VSVAR32S = compilerPath.replace("\\", "\\\\");
		
		try 
		{
			//File file= new File("./config/config.xml");
			
			Config.updateConfigFile("./config/config.xml");
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * This is the SelectionListener for Cancel Button.
	 * @author joaquin()
	 *
	 */
	private class CancelButtonListener extends SelectionAdapter 
	{
		private UATCompilerSettingGUI demo;
		public CancelButtonListener( UATCompilerSettingGUI demo ) 
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
	 * This is the ShellListener of CompilerSettingGUI.
	 * @author joaquin(孙华衿)
	 *
	 */
	public class ShellCloseListener extends ShellAdapter 
	{
		private UATCompilerSettingGUI demo;
		public ShellCloseListener( UATCompilerSettingGUI demo ) 
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
