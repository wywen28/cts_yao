package unittest.gui;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.util.Config;

//@author:Cai Min
public class UATSystemSettingGUI {
	private static Logger logger = Logger.getLogger(UATSystemSettingGUI.class);
	private Shell shell = null;
	private Composite leftComposite = null;
	private Composite rightComposite = null,rightComposite2 = null,rightComposite3 = null;
	private Composite controlComposite = null;
	private Display  display = null;
	public UATGUI uatGui = null;
	
	private Button okButton = null;
	private Button cancelButton = null;
	
	private CLabel cmdToolPathLabel = null;
	private String cmdToolPath = null;
	private Text cmdToolPathText = null;
	
	private CLabel VCincludePathLabel = null;
	private String VCincludePath = null;
	private Text VCincludePathText = null;
	
	private CLabel includePathLabel = null;
	private String includePath = null;
	private Text includePathText = null;
	
	private Button logPathButton = null;
	private CLabel logPathLabel = null;
	private String logPath = Config.LOG_FILE;
	private Text logPathText = null;
	
	private Button delTmpFileButton = null;
	private boolean delTmpFile = false;
	
	private CLabel bakPostfixLabel = null;
	private String bakPostfix = null;
	private Text bakPostfixText = null;
	
	private Button fileOrderButton;
	private boolean fileOrder = Config.fileOrder;
	
	private Button normalCoverageButton;
	private boolean normalCoverage = Config.NormalCoverage;
	
	private Button rechoosePathButton;
	private boolean rechoosePath = Config.IsRechoosepath;
	
	private Button changeTargetButton;
	private boolean changeTarget = Config.ChangeTarget;
	
	private Button calculatePathConstraintButton;
	private boolean calculatePathConstraint = Config.calculatePathConstraint;
	
	private CLabel stubGenarateModeLabel = null;
	private String stubGenarateMode = null;
	private Combo stubGenarateModeCombo = null;
	
	public UATSystemSettingGUI(UATGUI uatgui) 
	{
		this.uatGui = uatgui;
	}

	public void go() {
		display = Display.getDefault();
		this.createShell();
		this.dealEvent();
		
		this.shell.pack();
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
		shell.setText(GUILanguageResource.getProperty("Advance") );
		
		FormLayout formLayout = new FormLayout();
		shell.setLayout(formLayout);
		formLayout.marginHeight = 10;

		leftComposite = WidgetFactory.createComposite( shell, SWT.FLAT );
		leftComposite.setLayout( new FillLayout() );
		FormData formData1 = new FormData(100, 350);
		formData1.width = 100;
		formData1.height = 350;
		formData1.left = new FormAttachment(0,10);
		formData1.top = new FormAttachment(0,10);
		formData1.bottom = new FormAttachment(100,-50);
		
		leftComposite.setLayoutData(formData1);
		
		//创建具有单选和边框的Tree
		Tree t=new Tree(leftComposite,SWT.SINGLE|SWT.BORDER);
		//创建主干项目1、2和3
		TreeItem child1=new TreeItem(t,SWT.NONE,0);
		child1.setText("工具路径");
		TreeItem child2=new TreeItem(t,SWT.NONE,1);
		child2.setText("文件设置");
		TreeItem child3 = new TreeItem(t,SWT.NONE,2);
		child3.setText("高级");
		t.setBounds( 0, 0, 100, 350 );
		
		FormData formData2 = new FormData();
		formData2.left = new FormAttachment(0,115);
		formData2.top = new FormAttachment(0,10);
		formData2.right = new FormAttachment(100, -10);
		formData2.bottom = new FormAttachment(100,-50);
		formData2.width = 400;
		formData2.height = 350;
		
		//***********************************rightComposite***********************************
		rightComposite = WidgetFactory.createComposite( shell, SWT.FLAT|SWT.BORDER);
		rightComposite.setLayout(new FormLayout());
		rightComposite.setLayoutData(formData2);
		rightComposite.setVisible(true);
		
		//the block is for rightComposite 
		{
			cmdToolPathLabel = WidgetFactory.createCLabel( rightComposite, SWT.FLAT, 
					GUILanguageResource.getProperty("CmdToolPath")  );
			FormData formdata = new FormData();
			formdata.height = 30;
			formdata.width = 390;
			formdata.top = new FormAttachment();
			cmdToolPathLabel.setLayoutData(formdata);

			cmdToolPathText = WidgetFactory.createText( rightComposite, SWT.BORDER |SWT.H_SCROLL, cmdToolPath , true );
			cmdToolPathText.setText(Config.CmdToolPath );
			formdata = new FormData();
			formdata.height = 20;
			formdata.left = new FormAttachment(0,5);
			formdata.right = new FormAttachment(100,-5);
			formdata.top = new FormAttachment(cmdToolPathLabel);
			cmdToolPathText.setLayoutData(formdata);

			VCincludePathLabel = WidgetFactory.createCLabel( rightComposite, SWT.FLAT, 
					GUILanguageResource.getProperty("IncludePath")  );
			formdata = new FormData();
			formdata.height = 30;
			formdata.width = 390;
			formdata.top = new FormAttachment(cmdToolPathText);
			VCincludePathLabel.setLayoutData(formdata);

			VCincludePathText = WidgetFactory.createText( rightComposite, SWT.BORDER |SWT.H_SCROLL, VCincludePath, true );
			VCincludePathText.setText(Config.IncludePath );
			formdata = new FormData();
			formdata.height = 20;
			formdata.left = new FormAttachment(0,5);
			formdata.right = new FormAttachment(100,-5);
			formdata.top = new FormAttachment(VCincludePathLabel);
			VCincludePathText.setLayoutData(formdata);
			
			includePathLabel = WidgetFactory.createCLabel( rightComposite, SWT.FLAT, 
					GUILanguageResource.getProperty("userLibPath")  );
			formdata = new FormData();
			formdata.height = 30;
			formdata.width = 390;
			formdata.top = new FormAttachment(VCincludePathText);
			includePathLabel.setLayoutData(formdata);

			includePathText = WidgetFactory.createText( rightComposite, SWT.BORDER |SWT.H_SCROLL, includePath, true );
			includePathText.setText(Config.userLibPath);
			formdata = new FormData();
			formdata.height = 20;
			formdata.left = new FormAttachment(0,5);
			formdata.right = new FormAttachment(100,-5);
			formdata.top = new FormAttachment(includePathLabel);
			includePathText.setLayoutData(formdata);
		}
		
		//***********************************rightComposite2***********************************
		rightComposite2 = WidgetFactory.createComposite( shell, SWT.FLAT|SWT.BORDER );
		rightComposite2.setLayout(new FormLayout());
		rightComposite2.setLayoutData(formData2);
		rightComposite2.setVisible(false);
		
		//this block is for rightComposite3
		{
			logPathLabel = WidgetFactory.createCLabel( rightComposite2, SWT.FLAT,
					GUILanguageResource.getProperty("LOG_FILE") );
			logPathText = WidgetFactory.createText( rightComposite2, SWT.SINGLE | SWT.BORDER, logPath, true );
			logPathButton = WidgetFactory.createButton( rightComposite2, SWT.PUSH, GUILanguageResource.getProperty("Browse") );
			
			
			FormData formdata = new FormData();
			formdata.height = 30;
			formdata.width = 120;
			formdata.top = new FormAttachment();
			logPathLabel.setLayoutData(formdata);
			
			formdata = new FormData();
			formdata.height = 30;
			formdata.width = 70;
			formdata.top = new FormAttachment();
			formdata.right = new FormAttachment(100,-5);
			logPathButton.setLayoutData(formdata);
			formdata = new FormData();
			formdata.height = 26;
			formdata.left = new FormAttachment(logPathLabel);
			formdata.top = new FormAttachment();
			formdata.right = new FormAttachment(100,-80);
			logPathText.setLayoutData(formdata);
			
			logPathButton.addSelectionListener( new SelectionAdapter() 
			{
				public void widgetSelected( SelectionEvent e ) 
				{
					DirectoryDialog dirDialog = WidgetFactory.createDirectoryDialog( shell );
					dirDialog.setText( GUILanguageResource.getProperty("LOG_FILE") );
					
					String path = dirDialog.open();
					
					if( path != null ) 
					{
						logPathText.setText( path); 
					}
				}
			});
			
			delTmpFileButton = WidgetFactory.createButton( rightComposite2, SWT.CHECK, GUILanguageResource.getProperty("DELETETESTTMPFILE") );
			if(delTmpFile == true)
				delTmpFileButton.setSelection(true);
			
			formdata = new FormData();
			formdata.height = 30;
			formdata.left = new FormAttachment(0,5);
			formdata.top = new FormAttachment(0,35);
			delTmpFileButton.setLayoutData(formdata);
			
			delTmpFileButton.addSelectionListener( new SelectionAdapter() 
			{
				public void widgetSelected( SelectionEvent e ) 
				{
					delTmpFile = true;
				}
			});
			
			bakPostfixLabel = WidgetFactory.createCLabel( rightComposite2, SWT.FLAT, 
					GUILanguageResource.getProperty("BakPostfix")  );
			bakPostfixText = WidgetFactory.createText( rightComposite2, SWT.SINGLE | SWT.BORDER, bakPostfix , true );
			bakPostfixText.setText(Config.BakPostfix );
	
			formdata = new FormData();
			formdata.height = 30;
			formdata.left = new FormAttachment(0,5);
			formdata.top = new FormAttachment(0,65);
			bakPostfixLabel.setLayoutData(formdata);
			formdata = new FormData();
			formdata.height = 30;
			formdata.left = new FormAttachment(bakPostfixLabel);
			formdata.top = new FormAttachment(0,65);
			formdata.right = new FormAttachment(100,-5);
			bakPostfixText.setLayoutData(formdata);
		}
		
		//***********************************rightComposite3***********************************
		rightComposite3 = WidgetFactory.createComposite( shell, SWT.FLAT|SWT.BORDER );
		rightComposite3.setLayout(new FormLayout());
		rightComposite3.setLayoutData(formData2);
		rightComposite3.setVisible(false);
		//this block is for rightComposite3
		{
			fileOrderButton = WidgetFactory.createButton( rightComposite3, SWT.CHECK, 
					GUILanguageResource.getProperty("fileOrder") );
			if(fileOrder == true)
				fileOrderButton.setSelection(true);
			
			FormData formdata = new FormData();
			formdata.height = 30;
			formdata.left = new FormAttachment(0,5);
			fileOrderButton.setLayoutData(formdata);
			fileOrderButton.addSelectionListener( new SelectionAdapter() 
			{
				public void widgetSelected( SelectionEvent e ) 
				{
					fileOrder = true;
				}
			});
			
			normalCoverageButton = WidgetFactory.createButton( rightComposite3, SWT.CHECK , 
					GUILanguageResource.getProperty("NormalCoverage")  );
			if(normalCoverage == true)
				normalCoverageButton.setSelection(true);
			
			formdata = new FormData();
			formdata.height = 30;
			formdata.left = new FormAttachment(0,5);
			formdata.top = new FormAttachment(fileOrderButton);
			normalCoverageButton.setLayoutData(formdata);
			
			normalCoverageButton.addSelectionListener( new SelectionAdapter() 
			{
				public void widgetSelected( SelectionEvent e ) 
				{
					normalCoverage = true;
				}
			});
			
			rechoosePathButton = WidgetFactory.createButton( rightComposite3, SWT.CHECK, 
					GUILanguageResource.getProperty("IsRechoosepath") );
			if(rechoosePath == true)
				rechoosePathButton.setSelection(true);
			
			formdata = new FormData();
			formdata.height = 30;
			formdata.left = new FormAttachment(0,5);
			formdata.top = new FormAttachment(normalCoverageButton);
			rechoosePathButton.setLayoutData(formdata);
			rechoosePathButton.addSelectionListener( new SelectionAdapter() 
			{
				public void widgetSelected( SelectionEvent e ) 
				{
					rechoosePath = true;
				}
			});
			
			changeTargetButton = WidgetFactory.createButton( rightComposite3, SWT.CHECK, 
					GUILanguageResource.getProperty("ChangeTarget")  );
			if(changeTarget == true)
				rechoosePathButton.setSelection(true);
			
			formdata = new FormData();
			formdata.height = 30;
			formdata.left = new FormAttachment(0,5);
			formdata.top = new FormAttachment(rechoosePathButton);
			changeTargetButton.setLayoutData(formdata);
			changeTargetButton.addSelectionListener( new SelectionAdapter() 
			{
				public void widgetSelected( SelectionEvent e ) 
				{
					changeTarget = true;
				}
			});
			
			calculatePathConstraintButton = WidgetFactory.createButton( rightComposite3, SWT.CHECK, 
					GUILanguageResource.getProperty("calculatePathConstraint") );
			if(calculatePathConstraint == true)
				calculatePathConstraintButton.setSelection(true);
			
			formdata = new FormData();
			formdata.height = 30;
			formdata.left = new FormAttachment(0,5);
			formdata.top = new FormAttachment(changeTargetButton);
			calculatePathConstraintButton.setLayoutData(formdata);
			
			calculatePathConstraintButton.addSelectionListener( new SelectionAdapter() 
			{
				public void widgetSelected( SelectionEvent e ) 
				{
					calculatePathConstraint = true;
				}
			});
			
			stubGenarateModeLabel = WidgetFactory.createCLabel( rightComposite3, SWT.FLAT, 
					GUILanguageResource.getProperty("stubGenarateMode")  );
			stubGenarateModeCombo = new Combo( rightComposite3, SWT.NONE);
			stubGenarateModeCombo.add("基于返回值类型最大区间打桩");
			stubGenarateModeCombo.add("基于基于函数摘要打桩");
			stubGenarateModeCombo.add("基于路径约束打桩");
			stubGenarateModeCombo.select(Config.stubGenarateMode);
			formdata = new FormData();
			formdata.height = 30;
			formdata.width = 200;
			formdata.left = new FormAttachment(0,5);
			formdata.top = new FormAttachment(calculatePathConstraintButton);
			stubGenarateModeLabel.setLayoutData(formdata);
			formdata = new FormData();
			formdata.height = 30;
			formdata.width = 120;
			formdata.left = new FormAttachment(stubGenarateModeLabel);
			formdata.top = new FormAttachment(0,155);
			stubGenarateModeCombo.setLayoutData(formdata);
		}
		
		t.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				TreeItem ti=(TreeItem)e.item;
				populateList(ti.getText());
			}
			private void populateList(String text) {
				if(text.equals("工具路径"))
				{					
					rightComposite.setVisible(true);
					rightComposite2.setVisible(false);
					rightComposite3.setVisible(false);
				
				}
				
				if(text.equals("文件设置"))
				{					
					rightComposite.setVisible(false);
					rightComposite2.setVisible(true);
					rightComposite3.setVisible(false);			
				}	
				
				if(text.equals("高级"))
				{
					rightComposite.setVisible(false);
					rightComposite2.setVisible(false);
					rightComposite3.setVisible(true);
				}
			}
		});

		FormData formData3 = new FormData();
		formData3.top = new FormAttachment(leftComposite);
		formData3.bottom = new FormAttachment(100, 0);
		formData3.left = new FormAttachment(0,0);
		formData3.right = new FormAttachment(100,0);
		controlComposite = WidgetFactory.createComposite( shell, SWT.NONE );
		controlComposite.setLayout( new FormLayout() );
		controlComposite.setLayoutData(formData3);
		
		okButton = WidgetFactory.createButton( controlComposite, SWT.PUSH );
		okButton.setText(GUILanguageResource.getProperty("OK"));
		cancelButton = WidgetFactory.createButton( controlComposite, SWT.PUSH );
		cancelButton.setText( GUILanguageResource.getProperty("Cancel") );
		FormData formData4 = new FormData(60,30);
		formData4.left = new FormAttachment(0,50);
		formData4.top = new FormAttachment(0,10);
		formData4.bottom = new FormAttachment(100, -10);
		okButton.setLayoutData(formData4);
		FormData formData5 = new FormData(60,30);
		formData5.top = new FormAttachment(0,10);
		formData5.right = new FormAttachment(100,-50);
		formData5.bottom = new FormAttachment(100, -10);
		cancelButton.setLayoutData(formData5);
		
	}

	private void dealEvent()
	{
		shell.addShellListener( new ShellCloseListener( this ) );		
		okButton.addSelectionListener( new OkButtonListener( this )) ;		
		cancelButton.addSelectionListener( new CancelButtonListener( this ) );
	}
	
	private class OkButtonListener extends SelectionAdapter 
	{
		private UATSystemSettingGUI demo;
		public OkButtonListener(UATSystemSettingGUI demo ) 
		{
			this.demo = demo;
		}
		public void widgetSelected( SelectionEvent e ) 
		{
			cmdToolPath = cmdToolPathText.getText().trim();
			VCincludePath = VCincludePathText.getText().trim();
			includePath = includePathText.getText().trim();
			logPath = logPathText.getText().trim();
		//	stubGenarateMode = stubGenarateModeText.getText().trim();
			int mode = stubGenarateModeCombo.getSelectionIndex();
			stubGenarateMode = Integer.toString(mode);
			bakPostfix = bakPostfixText.getText().trim();
			
			String errorMsg = checkValidity();
			if( errorMsg.equals( "" ) ) 
			{				
				//update the config , however, it will   
				Config.CmdToolPath = cmdToolPath ;
				Config.IncludePath = VCincludePath;
				Config.LOG_FILE = logPath;
				Config.stubGenarateMode = Integer.parseInt(stubGenarateMode);
				Config.BakPostfix = bakPostfix;
				Config.fileOrder = fileOrder;
				Config.NormalCoverage = normalCoverage;
				Config.IsRechoosepath = rechoosePath;
				Config.calculatePathConstraint = calculatePathConstraint;
				Config.ChangeTarget = changeTarget;
				Config.DELETETESTTMPFILE = delTmpFile;
				Config.userLibPath = includePath;
				
				logger.info("cmdToolPath is changed to  " + cmdToolPath );
				logger.info("includePath is changed to  " + includePath );
				logger.info("logPath is changed to  " + logPath );
				logger.info("stubGenarateMode is changed to  " + stubGenarateMode );
				logger.info("BakPostfix is changed to  " + bakPostfix );
				logger.info("fileOrder is changed to  " + fileOrder );
				logger.info("normalCoverage is changed to  " + normalCoverage );
				logger.info("rechoosePath is changed to  " + rechoosePath );
				logger.info("calculatePathConstraint is changed to  " + calculatePathConstraint );
				logger.info("changeTarget is changed to  " + changeTarget );
				logger.info("delTmpFile is changed to  " + delTmpFile );
				logger.info("userLibPath is changed to  " + includePath );
				
				try 
				{
					Config.updateConfigFile("./config/config.xml");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				demo.uatGui.getShell().setEnabled( true );

				demo.shell.dispose();
			} 
			else 
			{
				MessageBox mb = WidgetFactory.createMessageBox( shell, SWT.ICON_ERROR | SWT.OK, "错误信息", errorMsg );
				mb.open();
			}
		}
		
		private String checkValidity() 
		{
			String result = "";
			File file = null;
			
			file = new File(cmdToolPath);
			if (!file.isFile())
				result = cmdToolPath + " not exists.";
			
			/*file = new File(includePath);
			if (!file.isDirectory())
				result = file.getParentFile() + " not exists.";*/
				
			file = new File(logPath);
			if (!file.isFile())
				result = "Log file is invalid.";
			else if (!file.getParentFile().isDirectory())
				result = file.getParentFile() + " not exists.";
			
		    return result;
		}
		
	}
	
	private class CancelButtonListener extends SelectionAdapter 
	{
		private UATSystemSettingGUI demo;
		public CancelButtonListener( UATSystemSettingGUI demo ) 
		{
			this.demo = demo;
		}
		public void widgetSelected( SelectionEvent e ) 
		{
			demo.uatGui.getShell().setEnabled( true );
			demo.shell.dispose();
		}
	}
	
	public class ShellCloseListener extends ShellAdapter 
	{
		private UATSystemSettingGUI demo;
		public ShellCloseListener( UATSystemSettingGUI demo ) 
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
