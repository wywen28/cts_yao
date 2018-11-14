package unittest.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import unittest.gui.helper.FileTreeNode;
import unittest.gui.helper.FunctionTreeNode;
import unittest.gui.helper.Resource;
import unittest.gui.helper.TreeNode;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.module.seperate.TestModule;
import unittest.util.AnalysisFile;
import unittest.util.Config;
import unittest.util.Project;
import unittest.util.RecordToLogger;
import unittest.util.SerializableAnalysisFileInfo;

/*
 * @author Cai Min
 * 批处理，支持对多个文件进行模块划分、对多个模块进行测试
 */
public class UATBatchProcess {
	private UATGUI demo;
	private static Shell shell;
	private Display display;
	private Group groupTop;
	private Group groupLeft;
	private Group groupRight;
	private Composite compMid;
	private Composite compBottom;
	private Tree projectTree;

	private Button moduleSeperateButton;
	private Button autoTestButton;
	private Combo testCombo;

	private Button addButton;
	private Button delButton;
	private Button addAllButton;
	private Button delAllButton;
	
	private Button okButton;
	private Button cancelButton;

	private List selectedItem;

//	private Set<AnalysisFile> afSet;
//	private Set<AnalysisFile> newafSet;
	private ArrayList<String> afSet;
	private ArrayList<String> newafSet;
	private Set<TestModule> tmSet;
	private Set<TestModule> newtmSet;

	public UATBatchProcess(UATGUI gui)
	{
		this.demo = gui;
		gui.getShell().setEnabled(false);
//		afSet = new HashSet();
		afSet = new ArrayList<String>();
		tmSet = new HashSet();
	}

	public void go()
	{
		display = Display.getDefault();
		createContents();
		dealEvent();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	private void createContents()
	{
		shell = new Shell(display, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL );
		shell.setText( GUILanguageResource.getProperty("BatchProcess") );
		shell.setImage(Resource.UATImage);
		shell.setLayout(new FormLayout());

		shell.setSize(800, 500);
		//使shell在屏幕中央显示
		LayoutUtil.centerShell(display, shell);

		createGroupTopContent();

		createGroupLeftContent();

		createMidCompContent();

		createGroupRightContent();
				
		createBottomCompContent();
		}

	private void createGroupTopContent()
	{
		groupTop = WidgetFactory.createGroup(shell, SWT.NULL);
		groupTop.setText(GUILanguageResource.getProperty("Action"));
		groupTop.setForeground(Resource.backgroundColor2);
		WidgetFactory.configureFormData(groupTop, new FormAttachment(0,10), new FormAttachment(0,10), new FormAttachment(100,-10), new FormAttachment(0,60));
		groupTop.setLayout(new FormLayout());

		moduleSeperateButton = WidgetFactory.createButton(groupTop, SWT.RADIO);
		WidgetFactory.configureFormData(moduleSeperateButton, null, new FormAttachment(0,6), new FormAttachment(50, -200), null);
		moduleSeperateButton.setText(GUILanguageResource.getProperty("moduleSeperate"));
		moduleSeperateButton.setSelection(true);
		
		//注释掉 2014.7.9 add by vector
//		autoTestButton = WidgetFactory.createButton(groupTop, SWT.RADIO);
//		WidgetFactory.configureFormData(autoTestButton, new FormAttachment(50,100), new FormAttachment(0,6), null, null);
//		autoTestButton.setText(GUILanguageResource.getProperty("autoTest"));
//
//		String[] comboContents = new String[2];
//		createComboContents(comboContents);
//		testCombo = WidgetFactory.createCombo(groupTop, comboContents);		
//		WidgetFactory.configureFormData(testCombo, new FormAttachment(autoTestButton), new FormAttachment(0,3), null, null);
//		testCombo.select(0);
//		testCombo.setEnabled(false);
	}
	
	private void createGroupLeftContent()
	{
		groupLeft = WidgetFactory.createGroup(shell, SWT.NULL);
		groupLeft.setText(GUILanguageResource.getProperty("ProjectTree"));
		WidgetFactory.configureFormData(groupLeft, new FormAttachment(0,10), new FormAttachment(0,70), new FormAttachment(50,-40), new FormAttachment(100,-40));
		groupLeft.setLayout(new FormLayout());

		createProjectTreeContents();
	}
	
	private void createMidCompContent()
	{
		compMid = WidgetFactory.createComposite(shell, SWT.NULL);
//		WidgetFactory.configureFormData(compMid, new FormAttachment(groupLeft), new FormAttachment(0,150),null,new FormAttachment(100,-170));
		WidgetFactory.configureFormData(compMid, new FormAttachment(50,-35), new FormAttachment(0,150),new FormAttachment(50,35),new FormAttachment(100,-170));
		FillLayout fillLayout = new FillLayout();
		compMid.setLayout(fillLayout);
		fillLayout.type = SWT.VERTICAL;

		addButton = WidgetFactory.createButton(compMid, SWT.PUSH);
		addButton.setText(GUILanguageResource.getProperty("add"));
		addButton.setEnabled(false);
		delButton = WidgetFactory.createButton(compMid, SWT.PUSH);
		delButton.setText(GUILanguageResource.getProperty("del"));
		delButton.setEnabled(false);
		addAllButton = WidgetFactory.createButton(compMid, SWT.PUSH);
		addAllButton.setText(GUILanguageResource.getProperty("addAll"));
		addAllButton.setEnabled(true);
		delAllButton = WidgetFactory.createButton(compMid, SWT.PUSH);
		delAllButton.setText(GUILanguageResource.getProperty("delAll"));
		delAllButton.setEnabled(false);
	}
	
	private void createGroupRightContent()
	{
		groupRight = WidgetFactory.createGroup(shell, SWT.H_SCROLL | SWT.V_SCROLL);
		groupRight.setText(GUILanguageResource.getProperty("Selected"));
		WidgetFactory.configureFormData(groupRight, new FormAttachment(50,40), new FormAttachment(0,70), new FormAttachment(100,-10), new FormAttachment(100,-40));
		groupRight.setLayout(new FormLayout());

		selectedItem = new List(groupRight, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		WidgetFactory.configureFormData(selectedItem, new FormAttachment(0,0),new FormAttachment(0,5),new FormAttachment(100,0),new FormAttachment(100,0));
	}
	
	private void createBottomCompContent()
	{
		compBottom = WidgetFactory.createComposite(shell,SWT.NULL);
		WidgetFactory.configureFormData(compBottom, new FormAttachment(0,10),new FormAttachment(groupLeft), new FormAttachment(100,-10), new FormAttachment(100, 0));
		compBottom.setLayout(new FormLayout());
		
		okButton = WidgetFactory.createButton(compBottom, SWT.PUSH);
		okButton.setText(GUILanguageResource.getProperty("OK"));
		WidgetFactory.configureFormData(okButton, new FormAttachment(0,150), new FormAttachment(0,5), new FormAttachment(0,200), new FormAttachment(100,-2));
		cancelButton = WidgetFactory.createButton(compBottom, SWT.PUSH);
		cancelButton.setText(GUILanguageResource.getProperty("Cancel"));
		WidgetFactory.configureFormData(cancelButton, new FormAttachment(100,-200), new FormAttachment(0,5), new FormAttachment(100,-150), new FormAttachment(100,-2));
	}
	
	private void createComboContents(String[] comboContents) {
		comboContents[0] = GUILanguageResource.getProperty("RandomTestcaseGenerate");
		comboContents[1] = GUILanguageResource.getProperty("ConstraintTestcaseGenerate");
	}

	private void createProjectTreeContents()
	{
//		TreeNode projectViewTreeRoot = new TreeNode( GUILanguageResource.getProperty("Project") );
//		projectTree = WidgetFactory.createTree(groupLeft, 
//				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER ,
//				projectViewTreeRoot, 0 );
		//2013/03/26
		//by xujiaoxian
		//优化工程树
		projectTree = new Tree( groupLeft, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL );
	//	projectTree.setBackground( Resource.backgroundColor2 );

		WidgetFactory.configureFormData( projectTree, new FormAttachment( 0, 0 ),
				new FormAttachment( 0, 5 ),
				new FormAttachment( 100, 0 ),
				new FormAttachment(100,0));
		Project currentProject = demo.getCurrentProject();
		if(currentProject == null)
			return ;

//		java.util.List<AnalysisFile> fileList = currentProject.getFileList();
//		java.util.List<String> filenameList = currentProject.getFilenameList();
//		String sourceCodePathString = currentProject.getSourceCodePathString();
//		int currentCharLoc = sourceCodePathString.length();
//		TreeNode root = new TreeNode( currentProject.getName() );
//		
//		try {
//			int i = 0;
//			while (i<filenameList.size()) {
//				i = demo.buildTree(filenameList, root, i, currentCharLoc);
//			}
//		}catch (StackOverflowError e) {
//			// TODO: handle exception
//			RecordToLogger.recordExceptionInfo(e, demo.getLogger());
//			if(Config.printExceptionInfoToConsole)
//				e.printStackTrace();
//			Display.getDefault().asyncExec(new Runnable ()
//			{
//				public void run()
//				{
//					String info = "创建批处理工程树发生异常" + "\n内存不足,堆栈溢出,请调整jvm的虚拟机参数";
//					demo.actionsGUI.addOutputMessage(info);
//					demo.setStatusBarInfo(info);
//					MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "创建工程树出现异常", info);
//					box.open();
//				}
//			});
//			return ;
//		}
		TreeItem root = new TreeItem(projectTree, 0);
		root.setText(currentProject.getName());
		root.setData(new File(currentProject.getSourceCodePathString()));
		root.setImage(Resource.projectImage);
		new TreeItem (root, 0);
		projectTree.addListener (SWT.Expand, new Listener () {
		    public void handleEvent (Event event) {
		      TreeItem root = (TreeItem) event.item;
		      demo.buildTree(root);
		    }
		  });
		//这句很重要，与树的风格有关，要想保留树原来的风格，必须弄懂
//		WidgetFactory.setTreeContents( root,projectTree, WidgetFactory.PROJECT );
		
		for(TreeItem ti : projectTree.getItems() ) {
			demo.buildTree(ti);
			ti.setExpanded(true);
			//for(TreeItem item : ti.getItems())
				//item.setExpanded( true );
		}
		/*
		TreeNode root = new TreeNode( "工程::"  + demo.getCurrentProject().getName());
//		java.util.List<AnalysisFile> fileList = demo.getCurrentProject().getFileList();
		java.util.List<String> filenameList =  demo.getCurrentProject().getFilenameList();
		for( int i = 0; i <filenameList.size(); i++ ) {
//			AnalysisFile f = fileList.get( i );
			String filename = filenameList.get(i);
			boolean isError = demo.getCurrentProject().getIsError()[i];
			boolean isExpand = demo.getCurrentProject().getIsExpand()[i];
			TreeNode tn = new FileTreeNode( filename ,isError,isExpand);
			root.addChild( tn );
//			if(f.isError())
			if(isError)
				continue;
			
			//add by xujiaoxian 2012-11-26
			AnalysisFile f = null;
			if(filenameList.size()<=Config.AnalysisFileInMem){
				f = demo.getCurrentProject().getFileList().get(i);
			}
			else{
				for(int j = 0;j<demo.getCurrentProject().getFileList().size();j++){
					if(filename.equals(demo.getCurrentProject().getFileList().get(j).getFile())){
						f = demo.getCurrentProject().getFileList().get(j);
						break;
					}
				}
				if(f==null)
				{
					try {
//						String analysisFilePath = "";
//						int begin = filename.lastIndexOf(File.separator)+1;
//						String filenameWithPath = filename.substring(begin);
//						File analysisdir = new File(demo.getCurrentProject().getAnaylysisFilePath());
//						String analysisFileNames[] = analysisdir.list();
//						for(String str : analysisFileNames){
//							if(str.endsWith(filenameWithPath)){
//								analysisFilePath = demo.getCurrentProject().getAnaylysisFilePath()+File.separator+str;
//								break;
//							}
//						}
						int begin = filename.lastIndexOf(File.separator)+1;
						FileInputStream fis = new FileInputStream(demo.getCurrentProject().getAnaylysisFilePath()+File.separator+filename.substring(begin)+i);
						ObjectInputStream ois = new ObjectInputStream(fis);
						SerializableAnalysisFileInfo saf = (SerializableAnalysisFileInfo) ois.readObject();
						ois.close();
						f = new AnalysisFile(saf);
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			//end add by xujiaoxian
			
			for( int j =0; j < f.getFunctionList().size(); j++ ) {
				TestModule t = f.getFunctionList().get( j );
				FunctionTreeNode ftn = new FunctionTreeNode( filename, t.isFirstTest() );
				tn.addChild( ftn );
				ftn.setName(t.getFuncName());
			}
			WidgetFactory.setTreeContents( root, projectTree, WidgetFactory.PROJECT );
		}

		for(TreeItem ti : projectTree.getItems() ) {
			ti.setExpanded(true);
//			for(TreeItem item : ti.getItems())
//				item.setExpanded( true );
		}
		*/
	}


	private void dealEvent()
	{
		//注释掉 2014.7.9 add by vector
//		autoTestButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e){
//				testCombo.setEnabled(true);
//				selectedItem.removeAll();
//				afSet.clear();
//				tmSet.clear();
//				addButton.setEnabled(false);
//				delButton.setEnabled(false);
//				delAllButton.setEnabled(false);
//			}
//		});

		moduleSeperateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e){
//				testCombo.setEnabled(false);
				selectedItem.removeAll();
				afSet.clear();
				tmSet.clear();
				addButton.setEnabled(false);
				delButton.setEnabled(false);
				delAllButton.setEnabled(false);
			}
		});

		addButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				if(moduleSeperateButton.getSelection())
//					for(AnalysisFile af : newafSet){
					for(String file : newafSet){
						//add by xujiaoxian
						int loc = demo.getCurrentProject().getfilesLoc(file);
//						AnalysisFile af = demo.getCurrentProject().getAnalysisFileWithoutUpateFileList(file);
						//end add by xujiaoxian
						
						//songhao 
						//解决编译未过文件可以进行批处理模块划分的bug；
						//解决批处理中可以无限添加已选中文件或者函数的bug；
//						if(!af.isError()&&!afSet.contains(af.getFile())){
						if(!demo.getCurrentProject().getIsError().get(loc) && !afSet.contains(file)){
							selectedItem.add(file);
							afSet.add(file);
						}
						//newafSet.add(af);
					}

//				else
//					for(TestModule tm : newtmSet){
//						//songhao
//						if(!tmSet.contains(tm)){
//							selectedItem.add(tm.getFileName() +" :: " + tm.getFuncName());
//							tmSet.add(tm);
//						}
//					}
				delAllButton.setEnabled(true);
			}
		});

		selectedItem.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				if(selectedItem.getSelection().length > 0)
					delButton.setEnabled(true);
				else
					delButton.setEnabled(false);
			}
		});

		projectTree.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TreeItem[] selection = projectTree.getSelection();
				TreeItem[] revisedSelection = new TreeItem[0];
//				newafSet = new HashSet();
				newafSet = new ArrayList<String>();
				newtmSet = new HashSet();
				for (int i = 0; i < selection.length; i++) 
					if(moduleSeperateButton.getSelection()){
						if(selection[i].getParentItem() != null)
//							if(selection[i].getParentItem().getParentItem() == null){ //file
							if(selection[i].getText().endsWith(".c")||selection[i].getText().endsWith(".C")||selection[i].getText().endsWith(".cc")){ //file
								//String allPathFileName = (String)selection[i].getData();
								String allPathFileName = selection[i].getText();
								TreeItem tempItem = selection[i];
								while (tempItem.getParentItem() != null){
									tempItem = tempItem.getParentItem();
									if (tempItem.getParentItem() == null)	//tree root
										allPathFileName = demo.getCurrentProject().getSourceCodePathString()+File.separator+allPathFileName;
									else
										allPathFileName = tempItem.getText()+File.separator+allPathFileName;
								}
////								for( int j = 0; j <demo.getCurrentProject().getFileList().size(); j++ ) 
//								for( int j = 0; j <demo.getCurrentProject().getFilenameList().size(); j++ )
//								{
////									AnalysisFile f = demo.getCurrentProject().getFileList().get( j );
//									String analysisFileName = demo.getCurrentProject().getFilenameList().get(j);
////									if(f.getFile().equals(allPathFileName)){
//									if(analysisFileName.equals(allPathFileName)){
////										if(!afSet.contains(f)){
//										if(!afSet.contains(analysisFileName)){
////											newafSet.add(f);
//											newafSet.add(analysisFileName);
//											TreeItem[] newSelection = new TreeItem[revisedSelection.length + 1];
//											System.arraycopy(revisedSelection, 0, newSelection, 0,
//													revisedSelection.length);
//											newSelection[revisedSelection.length] = selection[i];
//											revisedSelection = newSelection;
//										}
//										break;
//									}
//								}
								if(!afSet.contains(allPathFileName)){
//									newafSet.add(f);
									newafSet.add(allPathFileName);
									TreeItem[] newSelection = new TreeItem[revisedSelection.length + 1];
									System.arraycopy(revisedSelection, 0, newSelection, 0,
											revisedSelection.length);
									newSelection[revisedSelection.length] = selection[i];
									revisedSelection = newSelection;
								}
							}
					}
					else{
						if(selection[i].getParentItem() != null)
//							if(selection[i].getParentItem().getParentItem() != null)
								//if(selection[i].getParentItem().getParentItem().getParentItem() == null){//module
								if(selection[i].getParentItem().getText().endsWith(".c")||selection[i].getParentItem().getText().endsWith(".C")||selection[i].getParentItem().getText().endsWith(".cc")){//module
									//String allPathFileName = (String)selection[i].getParentItem().getData();
									String allPathFileName = selection[i].getParentItem().getText();
									TreeItem tempItem = selection[i].getParentItem();
									while (tempItem.getParentItem() != null){
										tempItem = tempItem.getParentItem();
										if (tempItem.getParentItem() == null)	//tree root
											allPathFileName = demo.getCurrentProject().getSourceCodePathString()+File.separator+allPathFileName;
										else
											allPathFileName = tempItem.getText()+File.separator+allPathFileName;
									}
									String funcName = selection[i].getText();
//									ArrayList<AnalysisFile> fileList = demo.getCurrentProject().getFileList();
//									for( int k = 0; k < fileList.size(); k++ ) 
//									{
//										AnalysisFile file = fileList.get( k );
//										if(file.getFile().equals(allPathFileName))
//										{
//											for( int j = 0; j < file.getFunctionList().size(); j++ ) 
//											{
//												TestModule tm = file.getFunctionList().get( j );
//												if( tm.getFuncName().startsWith(funcName) ) 
//												{
//													//	demo.setCurrentFunc(tm);
//													if(!tmSet.contains(tm))
//													{
//														newtmSet.add(tm);
//														TreeItem[] newSelection = new TreeItem[revisedSelection.length + 1];
//														System.arraycopy(revisedSelection, 0, newSelection, 0,
//																revisedSelection.length);
//														newSelection[revisedSelection.length] = selection[i];
//														revisedSelection = newSelection;
//													}
//													break;
//												}
//											}
//											break;
//										}
//									}
									AnalysisFile file = demo.getCurrentProject().getAnalysisFileWithoutUpateFileList(allPathFileName);
									for( int j = 0; j < file.getFunctionList().size(); j++ ) 
									{
										TestModule tm = file.getFunctionList().get( j );
										if( tm.getFuncName().startsWith(funcName) ) 
										{
											//	demo.setCurrentFunc(tm);
											if(!tmSet.contains(tm))
											{
												newtmSet.add(tm);
												TreeItem[] newSelection = new TreeItem[revisedSelection.length + 1];
												System.arraycopy(revisedSelection, 0, newSelection, 0,
														revisedSelection.length);
												newSelection[revisedSelection.length] = selection[i];
												revisedSelection = newSelection;
											}
											break;
										}
									}
								}
					}
				projectTree.setSelection(revisedSelection);

				if(revisedSelection.length != 0)
					addButton.setEnabled(true);
				else
					addButton.setEnabled(false);
			}
		});

		delButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				String[] items = selectedItem.getSelection();
				if(moduleSeperateButton.getSelection())
				{
					for(int i = 0; i < items.length; i ++)
//						for( int j = 0; j <demo.getCurrentProject().getFileList().size(); j++ ) 
						for( int j = 0; j <demo.getCurrentProject().getFilenameList().size(); j++ )
						{
//							AnalysisFile f = demo.getCurrentProject().getFileList().get(j);
							String analysisFileName = demo.getCurrentProject().getFilenameList().get(j);
//							if(f.getFile().equals(items[i])){
							if(analysisFileName.equals(items[i])){
//								afSet.remove(f);
								afSet.remove(analysisFileName);
								selectedItem.remove(items[i]);
							}
						}
				}
				else
				{
					for(int i = 0; i < items.length; i ++){
						System.out.println(items[i]);
						String allPathFileName = (items[i].split("::"))[0].trim();
						String funcName = (items[i].split("::"))[1].trim();
						ArrayList<AnalysisFile> fileList = demo.getCurrentProject().getFileList();
						for( int k = 0; k < fileList.size(); k++ ) 
						{
							AnalysisFile file = fileList.get( k );
							if(file.getFile().equals(allPathFileName))
							{
								for( int j = 0; j < file.getFunctionList().size(); j++ ) 
								{
									TestModule tm = file.getFunctionList().get( j );
									if( tm.getFuncName().startsWith(funcName) ) 
									{
										System.out.println("yes");
										tmSet.remove(tm);
										selectedItem.remove(items[i]);
										break;
									}

								}
								break;
							}
						}
					}
				}
				delButton.setEnabled(false);
			}
		});

		addAllButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				selectedItem.removeAll();
				if(moduleSeperateButton.getSelection()){
					afSet.clear();
					
//					for(AnalysisFile af : demo.getCurrentProject().getFileList())
					for(String file : demo.getCurrentProject().getFilenameList())
					{
						//add by xujiaoxian
						int loc = demo.getCurrentProject().getfilesLoc(file);
						//end add by xujiaoxian
//						if(!af.isError()){
						if(!demo.getCurrentProject().getIsError().get(loc)){
//							afSet.add(af);
							afSet.add(file);
							selectedItem.add(file);
						}
					}
				}
				else
				{
					tmSet.clear();
					for(AnalysisFile af : demo.getCurrentProject().getFileList())
						for(TestModule tm : af.getFunctionList()){
							tmSet.add(tm);
							selectedItem.add(tm.getFileName() +" :: " + tm.getFuncName());
						}
				}
				delAllButton.setEnabled(true);
			}
		});
		
		delAllButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				if(moduleSeperateButton.getSelection())
					afSet.clear();
				else
					tmSet.clear();
				selectedItem.removeAll();
				delAllButton.setEnabled(false);
			}
		});
		
		shell.addShellListener(new ShellListener() {
			public void shellIconified(ShellEvent arg0) {}
			public void shellDeiconified(ShellEvent arg0) {}
			public void shellDeactivated(ShellEvent arg0) {}
			public void shellClosed(ShellEvent arg0) {
				demo.getShell().setEnabled(true);
				shell.dispose();
			}
			public void shellActivated(ShellEvent arg0) {}
		});
		
		okButton.addSelectionListener( new OKButtonListener( this )) ;		
		cancelButton.addSelectionListener( new CancelButtonListener( this ) );
	}
	
	private class OKButtonListener extends SelectionAdapter 
	{
		private UATBatchProcess demo2;
		public OKButtonListener( UATBatchProcess demo2 ) 
		{
			this.demo2 = demo2;
		}
		public void widgetSelected( SelectionEvent e ) 
		{
			demo2.demo.getShell().setEnabled( true );
			
			if(moduleSeperateButton.getSelection())
				demo.doModuleSeparateForSelectedFiles(afSet);
//			else{
//				switch(testCombo.getSelectionIndex())
//				{
//				case 0:
//					demo.actionsGUI.doAutoTestForSelectedFiles(1,tmSet);
//					break;
//				case 1:
//					demo.actionsGUI.doAutoTestForSelectedFiles(2,tmSet);
//					break;
//				case 2:
//					//demo.doAutoTestForSelectedFiles(2,tmSet);
//					break;
//				}
//			}
			//songhao 批处理完后可以保存工程
			Config.needSavePro = true;
				
			shell.dispose();
		}
	}
	
	private class CancelButtonListener extends SelectionAdapter 
	{
		private UATBatchProcess demo2;
		public CancelButtonListener( UATBatchProcess demo2 ) 
		{
			this.demo2 = demo2;
		}
		public void widgetSelected( SelectionEvent e ) 
		{
			demo2.demo.getShell().setEnabled(true);
			shell.dispose();
		}
	}

}
