package unittest.gui;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;

public class UATCompilerSettingGUI_New {
	private static Logger logger = Logger.getLogger(UATSystemSettingGUI.class);
	private Shell shell = null;
	private Display  display = null;
	public UATGUI uatGui = null;

	private Group group = null;
	private Group gccGroup = null;	
	private Group szccGroup = null;
	private Composite bottomComposite = null;
	
	private Label compilerLabel = null;

	private Combo compilerCombo = null;
	int selection = 0;
	
	private Button gccCompilerButton = null;
	private Button szccCompilerButton = null;
	
	private Label memoryModeLabel = null;
	private Combo memoryModeCombo = null;
	
	private Label includePathLabel = null;
	private Text includePathText = null;
	private Button includePathButton = null;
	
	private Label libPathLabel = null;
	private Text libPathText = null;
	private Button libPathButton = null;
	
	private Label scriptPathLabel = null;
	private Text scriptPathText = null;
	private Button scriptPathButton = null;
	
	private Button showWarnMesg = null;
	
	private Button okButton = null;
	private Button cancelButton = null;
	
	public UATCompilerSettingGUI_New(UATGUI uatgui) 
	{
		this.uatGui = uatgui;
	}

	public void go() {
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

	private void createShell()
	{
		shell = new Shell(display, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL);
		shell.setText( GUILanguageResource.getProperty("Compiler") );
		shell.setLayout(new FormLayout());
		
		shell.setSize(500, 400);
		
		//使shell在屏幕中央显示
		LayoutUtil.centerShell(display, shell);
		
		group = WidgetFactory.createGroup(shell, SWT.NULL);
		group.setText(GUILanguageResource.getProperty("CompilerSelection"));
		WidgetFactory.configureFormData(group, new FormAttachment(0,10), new FormAttachment(0,10), new FormAttachment(100,-10), new FormAttachment(0,80));
		group.setLayout(new FormLayout());
		
		/*compilerCombo = WidgetFactory.createCombo(group, new String[]{"GCC","SZCC"});
		WidgetFactory.configureFormData(compilerCombo, new FormAttachment(0,5), new FormAttachment(0,5), new FormAttachment(0,120), new FormAttachment(100,-20));
	*/
		gccCompilerButton = WidgetFactory.createButton(group, SWT.RADIO);
		WidgetFactory.configureFormData(gccCompilerButton, new FormAttachment(0,50), new FormAttachment(0,10), null, null);
		gccCompilerButton.setText("GCC");
		szccCompilerButton = WidgetFactory.createButton(group, SWT.RADIO);
		WidgetFactory.configureFormData(szccCompilerButton, new FormAttachment(0,50), new FormAttachment(0,35), null, null);
		szccCompilerButton.setText("SZCC");
		
		gccGroup = WidgetFactory.createGroup(shell, SWT.NONE);
		WidgetFactory.configureFormData(gccGroup,  new FormAttachment(0,10), new FormAttachment(group),  new FormAttachment(100,-10),  new FormAttachment(100,-50) );
		gccGroup.setText("GCC设置");
		//gccGroup.setVisible(true);
		gccGroup.setEnabled(false);
		gccGroup.setEnabled(false);
		gccGroup.setLayout(new FormLayout());
		Text text = new Text(gccGroup,SWT.BORDER);
		text.setText("TODO");
		
		szccGroup = WidgetFactory.createGroup(shell, SWT.NONE);
		WidgetFactory.configureFormData(szccGroup,  new FormAttachment(0,10), new FormAttachment(group),  new FormAttachment(100,-10),  new FormAttachment(100,-50) );
		szccGroup.setText("SZCC设置");
		szccGroup.setVisible(false);
		szccGroup.setEnabled(false);
		szccGroup.setLayout(new FormLayout());
		
		{
			memoryModeLabel = WidgetFactory.createLabel(szccGroup, SWT.NONE, "内存模式");
			//memoryModeLabel.setFont( Resource.timesNewRoman_10_Font );
			WidgetFactory.configureFormData(memoryModeLabel, new FormAttachment(0,10),new FormAttachment(0,10) , new FormAttachment(0,95), new FormAttachment(0,35));
			memoryModeCombo = WidgetFactory.createCombo(szccGroup, new String[]{"i386-real-tiny"});
			WidgetFactory.configureFormData(memoryModeCombo, new FormAttachment(0,100),new FormAttachment(0,5), new FormAttachment(100,-50), new FormAttachment(0,35));
			
			includePathLabel = WidgetFactory.createLabel(szccGroup, SWT.NONE, "头文件地址");
			//includePathLabel.setFont( Resource.timesNewRoman_10_Font );
			WidgetFactory.configureFormData(includePathLabel, new FormAttachment(0,10),new FormAttachment(0,45) , new FormAttachment(0,95), new FormAttachment(0,70));
			includePathText = WidgetFactory.createText(szccGroup, SWT.BORDER);
			WidgetFactory.configureFormData(includePathText,new FormAttachment(0,100), new FormAttachment(0,45), new FormAttachment(100,-110), new FormAttachment(0,65));
			includePathButton = WidgetFactory.createButton(szccGroup, SWT.PUSH, GUILanguageResource.getProperty("Browse"));
			WidgetFactory.configureFormData(includePathButton, new FormAttachment(100,-100), new FormAttachment(0,45), new FormAttachment(100,-10), new FormAttachment(0,65));
			includePathButton.addSelectionListener( new SelectionAdapter() 
			{
				public void widgetSelected( SelectionEvent e ) 
				{
					DirectoryDialog dirDialog = WidgetFactory.createDirectoryDialog( shell );
					dirDialog.setText("path" );
				
					String path = dirDialog.open();
					
					if( path != null ) 
					{
						includePathText.setText(path); 
					}
				}
			});
			
			libPathLabel = WidgetFactory.createLabel(szccGroup, SWT.NONE, "库文件地址");
			//includePathLabel.setFont( Resource.timesNewRoman_10_Font );
			WidgetFactory.configureFormData(libPathLabel, new FormAttachment(0,10),new FormAttachment(0,80) , new FormAttachment(0,95), new FormAttachment(0,105));
			libPathText = WidgetFactory.createText(szccGroup, SWT.BORDER);
			WidgetFactory.configureFormData(libPathText,new FormAttachment(0,100), new FormAttachment(0,80), new FormAttachment(100,-110), new FormAttachment(0,100));
			libPathButton = WidgetFactory.createButton(szccGroup, SWT.PUSH, GUILanguageResource.getProperty("Browse"));
			WidgetFactory.configureFormData(libPathButton, new FormAttachment(100,-100), new FormAttachment(0,80), new FormAttachment(100,-10), new FormAttachment(0,100));
			libPathButton.addSelectionListener( new SelectionAdapter() 
			{
				public void widgetSelected( SelectionEvent e ) 
				{
					DirectoryDialog dirDialog = WidgetFactory.createDirectoryDialog( shell );
					dirDialog.setText("path" );
				
					String path = dirDialog.open();
					
					if( path != null ) 
					{
						libPathText.setText(path); 
					}
				}
			});
			
			scriptPathLabel = WidgetFactory.createLabel(szccGroup, SWT.NONE, "自定义连接脚本");
			//includePathLabel.setFont( Resource.timesNewRoman_10_Font );
			WidgetFactory.configureFormData(scriptPathLabel, new FormAttachment(0,10),new FormAttachment(0,115) , new FormAttachment(0,95), new FormAttachment(0,140));
			scriptPathText = WidgetFactory.createText(szccGroup, SWT.BORDER);
			WidgetFactory.configureFormData(scriptPathText,new FormAttachment(0,100), new FormAttachment(0,115), new FormAttachment(100,-110), new FormAttachment(0,135));
			scriptPathButton = WidgetFactory.createButton(szccGroup, SWT.PUSH, GUILanguageResource.getProperty("Browse"));
			WidgetFactory.configureFormData(scriptPathButton, new FormAttachment(100,-100), new FormAttachment(0,115), new FormAttachment(100,-10), new FormAttachment(0,135));
			scriptPathButton.addSelectionListener( new SelectionAdapter() 
			{
				public void widgetSelected( SelectionEvent e ) 
				{
					DirectoryDialog dirDialog = WidgetFactory.createDirectoryDialog( shell );
					dirDialog.setText("path" );
				
					String path = dirDialog.open();
					
					if( path != null ) 
					{
						scriptPathText.setText(path); 
					}
				}
			});
			
			showWarnMesg = WidgetFactory.createButton(szccGroup, SWT.CHECK, "显示警告信息");
			WidgetFactory.configureFormData(showWarnMesg, new FormAttachment(0,10), new FormAttachment(0,150), null, null);
			
		}
		gccCompilerButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e){
				/*szccGroup.setVisible(false);
				gccGroup.setVisible(true);
				gccGroup.setEnabled(true);*/
				szccGroup.setVisible(true);
				gccGroup.setVisible(false);
				componentSetEnabled(szccGroup,false);
				/*Color color = null;
				szccGroup.setBackground();*/
			}
		});
		szccCompilerButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e){
				gccGroup.setVisible(false);
				szccGroup.setVisible(true);
				componentSetEnabled(szccGroup,true);
			}
		});
		
		bottomComposite = WidgetFactory.createComposite(shell, SWT.NONE);
		bottomComposite.setLayout(new FormLayout());
		
		WidgetFactory.configureFormData(bottomComposite, new FormAttachment(0,10), new FormAttachment(szccGroup),  new FormAttachment(100,-10),  new FormAttachment(100,0));
		okButton = WidgetFactory.createButton(bottomComposite, SWT.PUSH);
		okButton.setText(GUILanguageResource.getProperty("OK"));
		WidgetFactory.configureFormData(okButton, new FormAttachment(0,50), new FormAttachment(0,10), new FormAttachment(0,100), new FormAttachment(100,-15));
		cancelButton = WidgetFactory.createButton(bottomComposite, SWT.PUSH);
		cancelButton.setText(GUILanguageResource.getProperty("Cancel"));
		WidgetFactory.configureFormData(cancelButton, new FormAttachment(100,-100), new FormAttachment(0,10), new FormAttachment(100,-50), new FormAttachment(100,-15));
	
	}
	
	//设置composite上的所有组件是否可用
	private void componentSetEnabled(Composite composite,boolean enabled)
	{
		szccGroup.setEnabled(enabled);
		for(Control c: composite.getChildren())
			c.setEnabled(enabled);
	}
	
	private void dealEvent()
	{
		shell.addShellListener( new ShellCloseListener( this ) );
		okButton.addSelectionListener( new OKButtonListener( this )) ;		
		cancelButton.addSelectionListener( new CancelButtonListener( this ) );
	}
	
	public class ShellCloseListener extends ShellAdapter 
	{
		private UATCompilerSettingGUI_New demo;
		public ShellCloseListener( UATCompilerSettingGUI_New uatCompilerSettingGUI_New ) 
		{
			this.demo = uatCompilerSettingGUI_New;
		} 

		public void shellClosed( ShellEvent e ) 
		{
			demo.uatGui.getShell().setEnabled( true );
			demo.shell.dispose();
		}

	}

	private class OKButtonListener extends SelectionAdapter 
	{
		private UATCompilerSettingGUI_New demo;
		public OKButtonListener( UATCompilerSettingGUI_New demo ) 
		{
			this.demo = demo;
		}
		public void widgetSelected( SelectionEvent e ) 
		{
			demo.uatGui.getShell().setEnabled( true );
			demo.shell.dispose();
		}
	}
	
	private class CancelButtonListener extends SelectionAdapter 
	{
		private UATCompilerSettingGUI_New demo;
		public CancelButtonListener( UATCompilerSettingGUI_New demo ) 
		{
			this.demo = demo;
		}
		public void widgetSelected( SelectionEvent e ) 
		{
			demo.uatGui.getShell().setEnabled( true );
			demo.shell.dispose();
		}
	}
}
