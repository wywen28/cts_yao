package unittest.gui;

import java.io.File;
import java.io.IOException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
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
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.util.Config;
import unittest.util.DirDeletor;
import unittest.util.Project;




/**
 * This GUI will be shown when new a project.
 * @author joaquin(孙华衿)
 */
public class UATNewProjectGUI{
	private Shell shell = null;
	private Composite topComposite = null;
	private Composite bottomComposite = null;
	private Composite controlComposite = null;
	
	private CLabel infoCLabel = null;
	private Button okButton = null;
	private Button cancelButton = null;
	
	//add by chenruolin  金凯峰师兄演示使用按钮
	private Button manualInputCompilerOptionButton = null;
	
	private CLabel projectNameCLabel = null;
	private CLabel projectPathCLabel = null;
	private CLabel sourceCodePathCLabel = null;
	private CLabel foreignLibPathCLabel = null;
	private CLabel compilerOptionCLabel = null;
	
	private Text projectNameText = null;
	private Text projectPathText = null;
	private Text sourceCodePathText = null;
	private Text foreignLibPathText = null;
	private Text compilerOptionText = null;
	
	private Button projectPathButton = null;
	private Button sourceCodePathButton = null;
	private Button foreignLibPathButton = null;
	private Button compilerOptionButton = null;

	private Color backgroundColor = null;
	private String projectName = null;
	private String projectPath = null;
	private String sourceCodePath = null;
	private String foreignLibPath = null;
	private String rememberChoice = null;
	private String compilerOption = null;
	private String manualInput = "false";

	private Display  display = Display.getDefault();
	public UATGUI uatGui = null;
	
	public String getProjectName() {
		return this.projectName;
	}
	
	public String getProjectPath() {
		return this.projectPath;
	}
	
	
	public String getSourceCodePath() {
		return this.sourceCodePath;
	}
	public UATNewProjectGUI( UATGUI uatGui ) {
		this.uatGui = uatGui;
	}
	
	/**
	 * The UATGui call this function to show the NewProject GUI. 
	 */
	public void go() {
		display = Display.getDefault();
		this.createResource();
		this.createShell();
		this.dealEvent();
		this.shell.open();
		
		while( !display.isDisposed() ) {
			if( !display.readAndDispatch() ) {
				display.sleep();
			}
		}
		display.dispose();
	}
	
	/**
	 * This function create the shell and all its children. 
	 */
	public void createShell() {
		shell = new Shell( SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL );
		shell.setText(GUILanguageResource.getProperty("NewUnitTestProject"));
		//shell.setText( "新建单元测试工程" );
//		shell.setBounds( 250, 250, 430, 355 );
		shell.setBounds( 250, 250, 430, 405 );
		shell.setLayout( null );
		shell.setImage(Resource.UATImage);
		LayoutUtil.centerShell(display, shell);
		
//		topComposite = WidgetFactory.createComposite( shell, SWT.FLAT );
		topComposite = WidgetFactory.createComposite( shell, SWT.BORDER );
		topComposite.setBackground( Resource.backgroundColor );
		topComposite.setLayout( new FillLayout() );
		topComposite.setBounds( 5, 5, 410, 50);
		
		//infoCLabel = WidgetFactory.createCLabel( topComposite, SWT.BORDER, "创建一个测试工程，实现对被测代码进行单元测试" );
		infoCLabel = WidgetFactory.createCLabel( topComposite, SWT.BORDER, GUILanguageResource.getProperty("NewUnitTestInfo"));
		infoCLabel.setBackground( backgroundColor );
		//infoCLabel.setFont( Resource. );
		infoCLabel.setBounds( 0, 0, 410, 45 );
		
		bottomComposite = WidgetFactory.createComposite( shell, SWT.BORDER );
//		bottomComposite.setBounds( 5, 56, 410, 210);
		bottomComposite.setBounds( 5, 56, 410, 260);
		bottomComposite.setLayout( null );
		
		//sourceCodePathCLabel = WidgetFactory.createCLabel( bottomComposite, SWT.FLAT, "被测文件路径" );
		sourceCodePathCLabel = WidgetFactory.createCLabel( bottomComposite, SWT.FLAT, GUILanguageResource.getProperty("TestedSrcPath") );
		
		projectNameCLabel = WidgetFactory.createCLabel( bottomComposite, SWT.FLAT, GUILanguageResource.getProperty("TestProjectName") );
		//projectNameCLabel = WidgetFactory.createCLabel( bottomComposite, SWT.FLAT, "单元测试工程名" );
		//projectNameCLabel.setFont( Resource.courierNew_10_Font );
		projectPathCLabel = WidgetFactory.createCLabel( bottomComposite, SWT.FLAT, GUILanguageResource.getProperty("TestResultPath") );
		//projectPathCLabel = WidgetFactory.createCLabel( bottomComposite, SWT.FLAT, "测试结果路径" );
		//projectPathCLabel.setFont( Resource.courierNew_10_Font );
		foreignLibPathCLabel = WidgetFactory.createCLabel( bottomComposite, SWT.FLAT, GUILanguageResource.getProperty("foreignLibPath") );
		compilerOptionCLabel = WidgetFactory.createCLabel(bottomComposite, SWT.FLAT, GUILanguageResource.getProperty("compilerOption") );
		//compilerOptionCLabel.setFont( Resource.courierNew_10_Font );
		
		projectNameText = WidgetFactory.createText( bottomComposite, SWT.SINGLE | SWT.BORDER, projectName, true );
		sourceCodePathText = WidgetFactory.createText( bottomComposite, SWT.SINGLE | SWT.BORDER, sourceCodePath, true );
		//projectNameText.setFont( Resource.courierNew_10_Font );
		projectPathText = WidgetFactory.createText( bottomComposite, SWT.SINGLE | SWT.BORDER, projectPath, true );
		//projectPathText.setFont( Resource.courierNew_10_Font );
		foreignLibPathText = WidgetFactory.createText( bottomComposite, SWT.SINGLE | SWT.BORDER, foreignLibPath, true );
		compilerOptionText = WidgetFactory.createText(bottomComposite, SWT.SINGLE | SWT.BORDER, compilerOption, true);
		
		sourceCodePathButton = WidgetFactory.createButton( bottomComposite, SWT.PUSH, GUILanguageResource.getProperty("Browse") );
		//sourceCodePathText.setFont( Resource.courierNew_10_Font );
		
		projectPathButton = WidgetFactory.createButton( bottomComposite, SWT.PUSH, GUILanguageResource.getProperty("Browse") );
		
		foreignLibPathButton = WidgetFactory.createButton( bottomComposite, SWT.PUSH, GUILanguageResource.getProperty("Browse") );
		compilerOptionButton = WidgetFactory.createButton(bottomComposite, SWT.PUSH, GUILanguageResource.getProperty("Browse"));
		manualInputCompilerOptionButton = WidgetFactory.createButton(bottomComposite, SWT.CHECK, null);
		manualInputCompilerOptionButton.setSelection(true);
		//projectPathButton = WidgetFactory.createButton( bottomComposite, SWT.PUSH, "浏览..." );
		//projectPathButton.setFont( Resource.courierNew_8_Font );

		
		//sourceCodePathButton = WidgetFactory.createButton( bottomComposite, SWT.PUSH, "浏览..." );
		//sourceCodePathButton.setFont( Resource.courierNew_8_Font );
		
		projectNameCLabel.setBounds( 5, 5, 150, 30 );
		sourceCodePathCLabel.setBounds( 5, 55, 150, 30 );
		projectPathCLabel.setBounds( 5, 110, 150, 30 );
		foreignLibPathCLabel.setBounds(5, 165, 150, 30);
		compilerOptionCLabel.setBounds(5, 220, 60, 30);
		manualInputCompilerOptionButton.setBounds(65,220,30,30);
		//add by chenruolin  根据配置文件初始化是否自动获取编译选项
		if (manualInput.equals("false")){
			manualInputCompilerOptionButton.setSelection(false);
			compilerOptionText.setEnabled(false);
		}
		
		projectNameText.setBounds( 160, 5, 180, 30 );
		sourceCodePathText.setBounds( 160, 55, 180, 30 );
		projectPathText.setBounds( 160, 110, 180, 30 );
		foreignLibPathText.setBounds(160, 165, 180, 30);
		compilerOptionText.setBounds(160, 220, 180, 30);
		
		
		projectPathButton.setBounds( 345, 110, 60, 30 );
		sourceCodePathButton.setBounds( 345, 55, 60, 30 );
		foreignLibPathButton.setBounds(345, 165, 60, 30);
		compilerOptionButton.setBounds(345, 220, 60, 30);
		
		controlComposite = WidgetFactory.createComposite( shell, SWT.NONE );
		controlComposite.setLayout( null );
//		controlComposite.setBounds( 5, 275, 370, 50 );
		controlComposite.setBounds( 5, 325, 370, 50 );
		okButton = WidgetFactory.createButton( controlComposite, SWT.PUSH );
		okButton.setText(GUILanguageResource.getProperty("OK"));
		//okButton.setText( "确定" );
		okButton.setFont( Resource.courierNew_10_Font );
		cancelButton = WidgetFactory.createButton( controlComposite, SWT.PUSH );
		okButton.setBounds( 15, 5, 60, 30 );
		cancelButton.setBounds( 305, 5, 60, 30 );
		cancelButton.setText(GUILanguageResource.getProperty("Cancel"));
		//cancelButton.setText( "取消" );
		cancelButton.setFont( Resource.courierNew_10_Font );
	}
	
	/**
	 * This function deal the event of CodemonNewProjectGui.
	 */
	public void dealEvent() {
		projectNameText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				 if (e.stateMask == SWT.CTRL && e.keyCode == 'a') {
					 projectNameText.selectAll();
			     }
			}
			@Override
			public void keyReleased(KeyEvent arg0) {
				// TODO Auto-generated method stub
			}
		});
		
		projectPathText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				 if (e.stateMask == SWT.CTRL && e.keyCode == 'a') {
					 projectPathText.selectAll();
			     }
			}
			@Override
			public void keyReleased(KeyEvent arg0) {
				// TODO Auto-generated method stub
			}
		});
		
		//temporary use for jinkaifeng
		manualInputCompilerOptionButton.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected( SelectionEvent e ) {
				if(manualInputCompilerOptionButton.getSelection()) {
					manualInput = "true";
					compilerOptionText.setEnabled(true);
				}
				else {
					manualInput = "false";
					compilerOptionText.setEnabled(false);
				}
			}
		});
		
		sourceCodePathText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				 if (e.stateMask == SWT.CTRL && e.keyCode == 'a') {
					 sourceCodePathText.selectAll();
			     }
			}
			@Override
			public void keyReleased(KeyEvent arg0) {
				// TODO Auto-generated method stub
			}
		});
		
		shell.addShellListener( new ShellCloseListener( this ) );
		projectPathButton.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected( SelectionEvent e ) {
				DirectoryDialog dd = WidgetFactory.createDirectoryDialog( shell );
				dd.setText(GUILanguageResource.getProperty("SelectTestResultDirectory"));
				File file = new File(Config.lastProjectPath);
				if(file.exists() && file.isDirectory())
					dd.setFilterPath(Config.lastProjectPath);
				//dd.setText( "选择测试输出目录:" );
				String path = dd.open();
				if( path != null ) {
					projectPathText.setText( path ); 
					Config.lastProjectPath = path;
				}
			}
		});
		
		foreignLibPathButton.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected( SelectionEvent e ) {
				DirectoryDialog dd = WidgetFactory.createDirectoryDialog( shell );
				dd.setText("请选择外部库路径");
				File file = new File(Config.lastForeignLibPath);
				if(file.exists() && file.isDirectory())
					dd.setFilterPath(Config.lastForeignLibPath);
				//dd.setText( "选择测试输出目录:" );
				String path = dd.open();
				if( path != null ) {
					foreignLibPathText.setText( path ); 
					Config.lastForeignLibPath = path;
				}
			}
		});
		
		compilerOptionButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected( SelectionEvent e ) {
				UATCompilerOptionEditGUI compilerOptionEditGUI = new UATCompilerOptionEditGUI(compilerOptionText.getText().trim());
				compilerOptionText.setText(compilerOptionEditGUI.getOptions()); 
				Config.shareLibPath = compilerOptionEditGUI.getOptions();
			}
		});
		
		
		
		sourceCodePathButton.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected( SelectionEvent e ) {
				DirectoryDialog dd = WidgetFactory.createDirectoryDialog( shell );
				dd.setText(GUILanguageResource.getProperty("SelectTestedSourceFileDirectory"));
				//dd.setText( "选择被测文件目录:" );
				File file = new File(Config.lastSourceCodePath);
				if(file.exists() && file.isDirectory())
					dd.setFilterPath(Config.lastSourceCodePath);
				String path = dd.open();
				if( path != null ) {
					sourceCodePathText.setText( path ); 
					Config.lastSourceCodePath = path;
				}
			}
		});
		
		okButton.addSelectionListener( new OkButtonListener( this )) ;
		
		cancelButton.addSelectionListener( new CancelButtonListener( this ) );
	}
	
	/**
	 * This function check the validity of projectName, projectPath, metaInfo file and 
	 * Source Code Path.
	 * @return a wrong message if there are some errors, else return "";
	 */
	public boolean checkValidity() {
		boolean error = false;
		String errorMsg = "";
		if( projectName.equals( "" ) ) {
			String str = GUILanguageResource.getProperty("TestProjectNameEmpty");
			errorMsg += str;
			MessageBox mb = WidgetFactory.createMessageBox( shell, SWT.ICON_ERROR | SWT.OK, "错误信息", errorMsg );
			mb.open();
			return true;
		}
		if( new File( sourceCodePath ).isDirectory() == false ) {
			errorMsg += GUILanguageResource.getProperty("TestedSourceCodeDirectoryNotExist");
			MessageBox mb = WidgetFactory.createMessageBox( shell, SWT.ICON_ERROR | SWT.OK, "错误信息", errorMsg );
			mb.open();
			return true;
		}
		for (int i=0; i<sourceCodePath.length(); i++){		//判断源代码路径中是否含有中文
			if ((sourceCodePath.charAt(i) >= 0x4e00) && (sourceCodePath.charAt(i) <= 0x9fbb)) {
				MessageBox mb = WidgetFactory.createMessageBox( shell, SWT.ICON_ERROR | SWT.OK, "错误信息", "源文件路径不支持中文，请重新选择" );
				mb.open();
                return true;  
            }
		}
//		for( int i = 0; i < codemonGui.getProjects().size(); i++ ) {
//			if( projectName.equals( codemonGui.getProjects().get( i ).getName() ) ) {
//				errorMsg += "A Project With the Same Name Already Exsits;\n";
//				break;
//			}
//		}
		if( new File( projectPath ).isDirectory() == false ) {
			errorMsg += GUILanguageResource.getProperty("TestResultDirectoryNotExist") + ", 请重新指定测试结果路径！";
			MessageBox box = WidgetFactory.createMessageBox(shell, SWT.ICON_ERROR | SWT.YES , GUILanguageResource.getProperty("NewUnitTestProject"), errorMsg );
			box.open();
			return true;  
		}
		
		if(new File(projectPath +File.separator+projectName).exists() == true)
		{
			//errorMsg += GUILanguageResource.getProperty("ExistSameNameProject");
			String msg =  GUILanguageResource.getProperty("ExistSameNameProject") + ", 要替换它吗？";
			MessageBox box = WidgetFactory.createMessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO , GUILanguageResource.getProperty("NewUnitTestProject"), msg );
			int flag = box.open();
			if(flag == SWT.NO)
				return true;
			else if(flag == SWT.YES) {
				File file =  new File( projectPath + File.separator + projectName);
				DirDeletor.delFolder(file.toString());
				file.mkdir();
			}
		}
		return error;
	}
	
	/**
	 * This function create the resources that will be used in CodemonNewProjectGui.
	 */
	public void createResource() 
	{
		backgroundColor = new Color( null, 230, 250, 175);
		projectName = "TestProject";
		projectPath = Config.lastProjectPath;
		sourceCodePath = Config.lastSourceCodePath;
		foreignLibPath = Config.lastForeignLibPath;
		rememberChoice = Config.rememberChoice;
		compilerOption = Config.shareLibPath;
		manualInput = Config.manualInputCompilerOption;
	}
	
	/**
	 * This function will be called if there is no errors about projectName, projectPath,
	 * Source Code Path and metaInfo file, it NEW a project and add it into the PROJECTS
	 * list of CodemonGui.
	 */
	public void doWorkForCodemonGui() {
		if( projectPath.endsWith( "\\" )) {
			projectPath = projectPath.substring( 0, projectPath.length() - 1 );
		}
		projectPath = projectPathText.getText().trim() ;//+ java.io.File.separator + projectName + ".utp";
		 
		new Thread()
		{
			public void run ()
			{
				Display.getDefault().asyncExec(new Runnable ()
				{
					public void run()
					{
						infoCLabel.setText("正在收集文件并分析文件的调用顺序....loading...");
						okButton.setEnabled(false);
						cancelButton.setEnabled(false);
						
						projectNameText.setEnabled(false);
						projectPathText.setEnabled(false);
						sourceCodePathText.setEnabled(false);
						foreignLibPathText.setEnabled(false);
						compilerOptionText.setEnabled(false);
						
						projectPathButton.setEnabled(false);
						sourceCodePathButton.setEnabled(false);
						foreignLibPathButton.setEnabled(false);
						compilerOptionButton.setEnabled(false);
						manualInputCompilerOptionButton.setEnabled(false);
					}
				});
				final Project project = new Project( projectName, projectPath, sourceCodePath);
				Display.getDefault().asyncExec(new Runnable ()
				{
					public void run()
					{
						while(project ==null)
							;
						uatGui.setCurrentProject(project);
						uatGui.setCurrentCoverCriteria(project.getCriteria());
						uatGui.setSoftwareMetricMenuItemEnable();
						uatGui.getShell().setEnabled( true );
						//uatGui.doProjectViewRefresh();
						//demo.uatGui.doFileViewRefresh();
						uatGui.doRefresh();
						shell.dispose();
					}
				});
			}
		}.start();
		
	}
	
	/**
	 * This is the SelectionListener of Ok Button.
	 * @author joaquin(孙华衿)
	 *
	 */
	private class OkButtonListener extends SelectionAdapter {
		private UATNewProjectGUI demo;
		public OkButtonListener( UATNewProjectGUI demo ) {
			this.demo = demo;
		}

		public void widgetSelected( SelectionEvent e ) {
			projectName = projectNameText.getText().trim();
			projectPath = projectPathText.getText().trim();
			//if (!projectPath.endsWith("/") )
			projectPath = new File(projectPath).getAbsolutePath();
			String path = projectPath + File.separator + projectName + File.separator + projectName + ".utp";
			try {
				uatGui.actionsGUI.addPathToConfigFile(path);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			sourceCodePath = sourceCodePathText.getText().trim();
			boolean error = checkValidity();
			if(!error) {
				//关闭当前工程
				Project currentProject = uatGui.getCurrentProject();
				if (currentProject != null) {
					UATExitDialog dialogTest = new UATExitDialog(uatGui);
					dialogTest.setUsage(2);
					dialogTest.go();
					if (!dialogTest.getCancle())	//选择了取消按钮
						return;
				}
				
				doWorkForCodemonGui();
				//demo.uatGui.getShell().setEnabled( true );
				//uatGui.doProjectViewRefresh();
				//demo.uatGui.doFileViewRefresh();
				//demo.uatGui.doRefresh();

				//demo.shell.dispose();
			} 
			
			//demo.codemonGui.doTestCaseMenu_ToolBarRefresh();
			String  foreignLibPath = foreignLibPathText.getText().trim();
			Config.lastProjectPath = new File(projectPath).getAbsolutePath();//modified by caimin, there were too much "\"
			Config.lastSourceCodePath = new File(sourceCodePath).getAbsolutePath();
			if(!foreignLibPath.equals(""))
				Config.lastForeignLibPath = new File(foreignLibPath).getAbsolutePath();
			else
				Config.lastForeignLibPath = "";
			Config.shareLibPath = compilerOptionText.getText().trim();
			Config.manualInputCompilerOption = manualInput;
			uatGui.getSystemConfigManager().storeCofig();
			//String pathName = projectPath
			Config.needSavePro = true;
		}
	}

	/**
	 * This is the SelectionListener for Cancel Button.
	 * @author joaquin()
	 *
	 */
	private class CancelButtonListener extends SelectionAdapter {
		private UATNewProjectGUI demo;
		public CancelButtonListener( UATNewProjectGUI demo ) {
			this.demo = demo;
		}
		public void widgetSelected( SelectionEvent e ) {
			demo.uatGui.getShell().setEnabled( true );
			demo.shell.dispose();
		}
	}
	
	/**
	 * This is the ShellListener of UATNewProjectGui.
	 * @author joaquin(孙华衿)
	 *
	 */
	public class ShellCloseListener extends ShellAdapter {
		private UATNewProjectGUI demo;
		public ShellCloseListener( UATNewProjectGUI demo ) {
			this.demo = demo;
		} 
		
		public void shellClosed( ShellEvent e ) {
			demo.uatGui.getShell().setEnabled( true );
			demo.shell.dispose();
		}
		
	}
}


