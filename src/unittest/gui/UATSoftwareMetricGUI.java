package unittest.gui;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import softtest.interpro.c.InterContext;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.metric.FileMetric;
import unittest.metric.FolderMetric;
import unittest.metric.MethodMetric;
import unittest.metric.ProjectMetric;
import unittest.module.seperate.TestModule;
import unittest.util.AnalysisFile;
import unittest.util.Config;
import unittest.util.Project;
import unittest.util.RecordToLogger;

public class UATSoftwareMetricGUI {
	static Logger logger = Logger.getLogger(UATSoftwareMetricGUI.class);
	
	private Tree tree;
	private Project currentProject = null;
	
	public Shell shell = null;
	private Display  display = null;
	private Composite topComposite = null;
	private Composite bottomComposite = null;
	
	private Button okButton = null;
	private Button exportButton = null;
	
	public UATSoftwareMetricGUI(Project currentProject){
		this.currentProject = currentProject;
		shell = new Shell(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL | SWT.MAX | SWT.MIN);
		display = Display.getDefault();
	}
	
	public void go(){
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
		shell.setText( GUILanguageResource.getProperty("softwareMetric") );
		shell.setImage( Resource.UATImage );
		shell.setBounds( 50, 50, 800, 400);
		shell.setLayout( new FormLayout() );
		shell.setMaximized( false );
		createComposite();
	}
	
	private void createComposite(){
		createTopComposite();
		createBottomComposite();
	}
	
	private void createTopComposite(){
		topComposite = WidgetFactory.createComposite( shell, SWT.FLAT );
		topComposite.setBackground( Resource.backgroundColor );
		topComposite.setLayout( new FormLayout() );
		WidgetFactory.configureFormData( topComposite, new FormAttachment( 0, 5 ),
				new FormAttachment( 0, 5 ),
				new FormAttachment( 100, -5 ),
				new FormAttachment( 85, 100, 0));
		
//		tree = new Tree( topComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER| SWT.SINGLE);
		tree = new Tree( topComposite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL );
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		WidgetFactory.configureFormData( tree, new FormAttachment( 0, 0 ),
				new FormAttachment( 0, 0 ),
				new FormAttachment( 100, 0 ),
				new FormAttachment(100,0));
		
		createTableColumns();
		TreeItem ti = new TreeItem(tree, SWT.NONE);
	    ti.setImage( Resource.projectImage );
	    ti.setText(0,"工程::"  + currentProject.getName());
	    ti.setData(new File(currentProject.getSourceCodePathString()));
	    //ti.setText(0,"工程::"  + currentProject.getSourceCodePathString());
	    setTreeitemText(ti, currentProject.getProjectMetric());
	    new TreeItem (ti, 0);
	    
	    //add by xujiaoxian  优化软件度量工程树导航
	    tree.addListener (SWT.Expand, new Listener () {
		    public void handleEvent (Event event) {
			      TreeItem root = (TreeItem) event.item;
			      buildMetricTree(root);
			    }
			  });
	    //end add by xujiaoxian
	    
////		java.util.List<AnalysisFile> fileList = currentProject.getFileList();
//	    java.util.List<String> filenameList = currentProject.getFilenameList();
////		for( int i = 0; i <fileList.size(); i++ ) {
//	    for(int i=0; i<filenameList.size();i++){
////			AnalysisFile f = fileList.get( i );
////	    	String fileName = f.getFile();
//	    	String fileName = filenameList.get(i);
//	    	AnalysisFile f = currentProject.getAnalysisFileWithoutUpateFileList(fileName);
//	    	int index = currentProject.getSourceCodePathString().length();
////	    	int index = fileName.lastIndexOf("\\");
//			fileName = fileName.substring(index + 1);
//			TreeItem tix = new TreeItem(ti, SWT.NONE);
//            if(f.isError())
//            	tix.setImage( Resource.ErrorFile );
//            else
//            {
//            	tix.setImage(Resource.File);
//            }	            	
//            tix.setData(f.getFile());  
//            tix.setText(0,fileName);
//            setTreeitemText(tix, f.getFileMetric());
//			if(f.isError())
//				continue;
//			for( int j =0; j < f.getFunctionList().size(); j++ ) {
//				TestModule t = f.getFunctionList().get( j );
//				TreeItem tim = new TreeItem(tix, SWT.NONE);
//				tim.setImage( Resource.testCaseImage );
//				tim.setText(0,t.getFuncName());
//				setTreeitemText(tim, t.getMethodMetric());
//			}
//		}
		//WidgetFactory.setTreeContents( root, tree, WidgetFactory.PROJECT );	
//		for(TreeItem t : tree.getItems() ) {
//			buildMetricTree(t);
//			t.setExpanded(true);
//		}

		packColumns();
	}
	
	/**
	 * LAZY 方式创建软件度量工程树导航
	 * @author xujiaoxian
	 * @param root
	 */
	public void buildMetricTree(TreeItem root){
		TreeItem [] items = root.getItems ();
	    for (int i= 0; i<items.length; i++) {
	    	if (items [i].getData () != null) return;
	    	items [i].dispose ();
	    }
	    File file = (File) root.getData ();
	    if(file.isFile()){//对文件的展开要另外处理
	    	AnalysisFile analysisfile = currentProject.getAnalysisFile(file.getAbsolutePath());
	    	List<TestModule> functionlist = analysisfile.getFunctionList();
	    	for(int i=0;i<functionlist.size();i++){
	    		TreeItem item = new TreeItem(root,0);
	    		item.setText(functionlist.get(i).getFuncName());
	    		item.setData(functionlist.get(i).getFuncName());
	    		item.setImage( Resource.testCaseImage );
				setTreeitemText(item, functionlist.get(i).getMethodMetric());
	    	}
	    }
	    else if(file.isDirectory()){
	    	File [] files = file.listFiles ();
	    	if (files == null) return;
	    	for (int i= 0; i<files.length; i++) {
	    		if(files[i].getName().matches("(^\\..*)")){//隐藏文件不显示
	    			continue;
	    		}
	    		else if(files[i].isDirectory()){
	    			if(hasSrcFile(files[i]))
	    			{
	    				//文件夹软件度量 add by zhuqianchao
	    				TreeItem item = new TreeItem(root,0);
	    				item.setText(files[i].getName());
	    				item.setData(files[i]);
	    				item.setImage(Resource.folderImage);
	    				setTreeitemText(item ,FolderMetric.updateMetric(files[i],currentProject));	    				
	    				new TreeItem(item, 0);
	    			}
	    		}
	    		else if(files[i].getName().matches(InterContext.SRCFILE_POSTFIX)){
	    			TreeItem item = new TreeItem (root, 0);
		    		item.setText (files [i].getName ());
		    		item.setData (files [i]);
		    		int fileloc = currentProject.getfilesLoc(files[i].getAbsolutePath());
		    		if(currentProject.getIsError().get(fileloc))
		    			item.setImage(Resource.ErrorFile);
		    		else
		    			item.setImage(Resource.File);
		    		if(!currentProject.getIsError().get(fileloc) && currentProject.getFuncsNumList(fileloc)!=0)
                  	  new TreeItem(item,0);
		    		setTreeitemText(item, currentProject.getAnalysisFile(files[i].getAbsolutePath()).getFileMetric());
		    }
	    		else{
	    			  //do nothing
	    		}
	    	}
	    	
	    	
	   }
	}
	
	 /**
     * 2013/04/01
     * 判断某一目录下是否有源文件
     * @author xujiaoxian
     * @param srcDir
     * @return 如果有返回true，如果没有返回false
     */
    public boolean hasSrcFile(File srcDir)
	{
        boolean has = false;
		if (srcDir.isFile()) 
		{
			//收集源文件
			if(srcDir.getName().matches(InterContext.SRCFILE_POSTFIX))
				has = true;
		} 
		else if (srcDir.isDirectory()) 
		{
			File[] fs = srcDir.listFiles();
			for (int i = 0; i < fs.length; i++) 
			{
				has = has || hasSrcFile(fs[i]);
			}
		}
		return has;
	}
	
	public void createBottomComposite(){
		bottomComposite = WidgetFactory.createComposite( shell, SWT.BORDER );
		bottomComposite.setLayout( new FormLayout() );		
		WidgetFactory.configureFormData( bottomComposite, new FormAttachment( 0, 5 ),
				new FormAttachment( topComposite, 5 ),
				new FormAttachment( 100, -5 ),
				new FormAttachment( 100, -5 ));
		
		okButton = WidgetFactory.createButton(bottomComposite, SWT.PUSH, GUILanguageResource.getProperty("OK"));
		exportButton = WidgetFactory.createButton(bottomComposite, SWT.PUSH, GUILanguageResource.getProperty("export"));
		
		WidgetFactory.configureFormData( okButton, new FormAttachment( 30, 100,0 ),
				new FormAttachment( 40, 100,0 ),
				null,
				null);
		WidgetFactory.configureFormData( exportButton, new FormAttachment( 60, 100,0 ),
				new FormAttachment( 40, 100,0 ),
				null,
				null);
	}
	
	public void createTableColumns(){
		TreeColumn treeColumnProject = new TreeColumn(tree, SWT.CENTER);
		treeColumnProject.setText(GUILanguageResource.getProperty("Project"));
		
		TreeColumn lineColumn = new TreeColumn(tree, SWT.CENTER);
		lineColumn.setText(GUILanguageResource.getProperty("lineColumn"));
		
		TreeColumn codeLineColumn = new TreeColumn(tree, SWT.CENTER);
		codeLineColumn.setText(GUILanguageResource.getProperty("codeLineColumn"));
		
		TreeColumn remarkLineColumn = new TreeColumn(tree, SWT.CENTER);
		remarkLineColumn.setText(GUILanguageResource.getProperty("remarkLineColumn"));
		
		TreeColumn blankLineColumn = new TreeColumn(tree, SWT.CENTER);
		blankLineColumn.setText(GUILanguageResource.getProperty("blankLineColumn"));
		
		TreeColumn arguNumColumn = new TreeColumn(tree, SWT.CENTER);
		arguNumColumn.setText(GUILanguageResource.getProperty("arguNumColumn"));
		
		TreeColumn localVarColumn = new TreeColumn(tree, SWT.CENTER);
		localVarColumn.setText(GUILanguageResource.getProperty("localVarColumn"));
		
		TreeColumn globVarColumn = new TreeColumn(tree, SWT.CENTER);
		globVarColumn.setText(GUILanguageResource.getProperty("globVarColumn"));
		
		TreeColumn branchNumColumn = new TreeColumn(tree, SWT.CENTER);
		branchNumColumn.setText(GUILanguageResource.getProperty("branchNumColumn"));
		
		TreeColumn loopNumColumn = new TreeColumn(tree, SWT.CENTER);
		loopNumColumn.setText(GUILanguageResource.getProperty("loopNumColumn"));
		
		TreeColumn maxLoopDepthColumn = new TreeColumn(tree, SWT.CENTER);
		maxLoopDepthColumn.setText(GUILanguageResource.getProperty("maxLoopDepthColumn"));
		
		TreeColumn calleeColumn = new TreeColumn(tree, SWT.CENTER);
		calleeColumn.setText(GUILanguageResource.getProperty("calleeColumn"));
		
		TreeColumn cyclomaticComplexityColumn = new TreeColumn(tree, SWT.CENTER);
		cyclomaticComplexityColumn.setText(GUILanguageResource.getProperty("cyclomaticComplexityColumn"));
		
		TreeColumn headFilesColumn = new TreeColumn(tree, SWT.CENTER);
		headFilesColumn.setText(GUILanguageResource.getProperty("headFilesColumn"));
		
		TreeColumn methodNumColumn = new TreeColumn(tree, SWT.CENTER);
		methodNumColumn.setText(GUILanguageResource.getProperty("methodNumColumn"));
		
		TreeColumn fileNumColumn = new TreeColumn(tree, SWT.CENTER);
		fileNumColumn.setText(GUILanguageResource.getProperty("fileNumColumn"));
		
	}
	
	public void packColumns(){
		TreeColumn[] columns = tree.getColumns();
		for (int i = 0, n = columns.length; i < n; i++) {
			columns[i].pack();
		}
	}
	
	public void dealEvent(){
		shell.addShellListener( new ShellCloseListener( this ) );
		
		okButton.addSelectionListener(new SelectionAdapter() 
		{
			public void widgetSelected( SelectionEvent e ) 
			{
				shell.dispose();	
			}
		});
		
		tree.addSelectionListener(new ViewTreeSelectionListener(tree));
		
		exportButton.addSelectionListener(new SelectionAdapter() 
		{
			public void widgetSelected( SelectionEvent e ) 
			{
				shell.setEnabled(false);
				new Thread()
				{
					public void run()
					{
						try{
							boolean result = doExport();
							if(!result)
								return;
							Display.getDefault().asyncExec(new Runnable()
							{
								public void run()
								{
									MessageBox box= WidgetFactory.createInfoMessageBox(shell, "导出结束", "导出结束，请到工程路径下查看SoftwareMetric.csv" );
									box.open();
									shell.setEnabled(true);
	
								}
							});
						}catch(Exception e){
							
						}
					}
				}.start();
			}
					
		});
	}
	
	void setTreeitemText(TreeItem ti,ProjectMetric metric){
		ti.setText(1, ""+metric.getLine());
		ti.setText(2, ""+metric.getCodeLine()+ "(" + metric.getCodeRate() + "%)");
		ti.setText(3, ""+metric.getAnnotationLine()+ "(" + metric.getAnnotationRate() + "%)");
		ti.setText(4, ""+metric.getBlankLine()+ "(" + metric.getBlankRate() + "%)");
		ti.setText(5, ""+metric.getArguNum());
		ti.setText(6, ""+metric.getLocalVar());
		ti.setText(7, ""+metric.getGlobVar());
		ti.setText(8, ""+metric.getBranchNum());
		ti.setText(9, ""+metric.getLoopNum());
		ti.setText(10, ""+metric.getMaxLoopDepth());
		ti.setText(11, ""+metric.getCallee());
		ti.setText(12, "");
		ti.setText(13, ""+metric.getHeadFiles());
		ti.setText(14, ""+metric.getMethodNum());
		ti.setText(15, ""+metric.getFileNum());
		
	}
	/*
	 * 设置文件夹度量属性 
	 * add by zhuqianchao
	 */
	void setTreeitemText(TreeItem ti,FolderMetric metric){
		ti.setText(1, ""+metric.getLine());
		ti.setText(2, ""+metric.getCodeLine()+ "(" + metric.getCodeRate() + "%)");
		ti.setText(3, ""+metric.getAnnotationLine()+ "(" + metric.getAnnotationRate() + "%)");
		ti.setText(4, ""+metric.getBlankLine()+ "(" + metric.getBlankRate() + "%)");
		ti.setText(5, ""+metric.getArguNum());
		ti.setText(6, ""+metric.getLocalVar());
		ti.setText(7, ""+metric.getGlobVar());
		ti.setText(8, ""+metric.getBranchNum());
		ti.setText(9, ""+metric.getLoopNum());
		ti.setText(10, ""+metric.getMaxLoopDepth());
		ti.setText(11, ""+metric.getCallee());
		ti.setText(12, "");
		ti.setText(13, ""+metric.getHeadFiles());
		ti.setText(14, ""+metric.getMethodNum());
		ti.setText(15, ""+metric.getFileNum());
		
	}
	void setTreeitemText(TreeItem ti,FileMetric metric){		
		ti.setText(1, ""+metric.getLine());
		ti.setText(2, ""+metric.getCodeLine()+ "(" + metric.getCodeRate() + "%)");
		ti.setText(3, ""+metric.getAnnotationLine()+ "(" + metric.getAnnotationRate() + "%)");
		ti.setText(4, ""+metric.getBlankLine()+ "(" + metric.getBlankRate() + "%)");
		ti.setText(5, ""+metric.getArguNum());
		ti.setText(6, ""+metric.getLocalVar());
		ti.setText(7, ""+metric.getGlobVar());
		ti.setText(8, ""+metric.getBranchNum());
		ti.setText(9, ""+metric.getLoopNum());
		ti.setText(10, ""+metric.getMaxLoopDepth());
		ti.setText(11, ""+metric.getCallee());
		ti.setText(12, "");
		ti.setText(13, ""+metric.getHeadFiles());
		ti.setText(14, ""+metric.getMethodNum());
		ti.setText(15, "1");
		
	}
	
	void setTreeitemText(TreeItem ti,MethodMetric metric){
		ti.setText(1, ""+metric.getLine());
		ti.setText(2, ""+metric.getCodeLine()+ "(" + metric.getCodeRate() + "%)");
		ti.setText(3, ""+metric.getAnnotationLine()+ "(" + metric.getAnnotationRate() + "%)");
		ti.setText(4, ""+metric.getBlankLine()+ "(" + metric.getBlankRate() + "%)");
		ti.setText(5, ""+metric.getArguNum());
		ti.setText(6, ""+metric.getLocalVar());
		ti.setText(7, "0");
		ti.setText(8, ""+metric.getBranchNum());
		ti.setText(9, ""+metric.getLoopNum());
		ti.setText(10, ""+metric.getMaxLoopDepth());
		ti.setText(11, ""+metric.getCallee());
		ti.setText(12, ""+metric.getCyclomaticComplexity());
		ti.setText(13, "0");
		ti.setText(14, "1");
		ti.setText(15, "0");
		
	}
	
	private boolean doExport(){
		final String path = this.currentProject.getPath()+"/SoftwareMetric.csv";
		File file = new File(path);
		if(file.exists()){
			file.delete();
			try {
				file.createNewFile();
			} catch (IOException e) {
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						MessageBox box= WidgetFactory.createInfoMessageBox(shell, "异常", "无法导出，请确认 "+ path + "未被其他程序占用" );
						box.open();
					}
				});
				return false;
			}
		}
		FileWriter fw = null;
		try {
			fw = new FileWriter(file);
		} catch (IOException e) {
			RecordToLogger.recordExceptionInfo(e, logger);
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					MessageBox box= WidgetFactory.createInfoMessageBox(shell, "异常", "无法导出，请确认 "+ path + "未被其他程序占用" );
					box.open();
				}
			});
			if(fw != null){
				try {
					fw.close();
				} catch (IOException e1) {
					RecordToLogger.recordExceptionInfo(e, logger);
				}
			}
			return false;
		}
		String header = GUILanguageResource.getProperty("Project");
		header = header + "," + GUILanguageResource.getProperty("lineColumn");
		header = header + "," + GUILanguageResource.getProperty("codeLineColumn");
		header = header + "," + GUILanguageResource.getProperty("remarkLineColumn");
		header = header + "," + GUILanguageResource.getProperty("blankLineColumn");
		header = header + "," + GUILanguageResource.getProperty("arguNumColumn");
		header = header + "," + GUILanguageResource.getProperty("localVarColumn");
		header = header + "," + GUILanguageResource.getProperty("globVarColumn");
		header = header + "," + GUILanguageResource.getProperty("branchNumColumn");
		header = header + "," + GUILanguageResource.getProperty("loopNumColumn");
		header = header + "," + GUILanguageResource.getProperty("maxLoopDepthColumn");
		header = header + "," + GUILanguageResource.getProperty("calleeColumn");
		header = header + "," + GUILanguageResource.getProperty("cyclomaticComplexityColumn");
		header = header + "," + GUILanguageResource.getProperty("headFilesColumn");
		header = header + "," + GUILanguageResource.getProperty("methodNumColumn");
		header = header + "," + GUILanguageResource.getProperty("fileNumColumn")+ "\r\n";

		try {
			fw.write(header);
			currentProject.toCSVFile(fw);
		} catch (IOException e) {
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					MessageBox box= WidgetFactory.createInfoMessageBox(shell, "异常", "无法导出，请确认 "+ path + "未被其他程序占用" );
					box.open();
				}
			});
			if(fw != null){
				try {
					fw.close();
				} catch (IOException e1) {
					RecordToLogger.recordExceptionInfo(e, logger);
				}
			}
			return false;
		}catch (Exception e) {
			RecordToLogger.recordExceptionInfo(e, logger);
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					MessageBox box= WidgetFactory.createInfoMessageBox(shell, "异常", "无法导出，请确认 "+ path + "未被其他程序占用" );
					box.open();
				}
			});
			if(fw != null){
				try {
					fw.close();
				} catch (IOException e1) {
					RecordToLogger.recordExceptionInfo(e, logger);
				}
			}
			return false;
		}
		try {
			fw.close();
		} catch (IOException e) {
			RecordToLogger.recordExceptionInfo(e, logger);
			return false;
		}
		return true;
	}
	public class ViewTreeSelectionListener  extends SelectionAdapter{
		private Tree tree = null;
		private TreeItem selected = null;
		
		public ViewTreeSelectionListener(Tree tree){
			this.tree = tree;
		}
		
		public void widgetSelected( SelectionEvent e ) 
		{
			TreeItem[] items1 = tree.getSelection();
			if(items1 !=null && items1.length > 0)
			{
				if(selected != null)
					selected.setBackground(null);
				selected = items1[0];
				selected.setBackground(Resource.backgroundColor2);
			}
				
		}
	}
	
	public class ShellCloseListener extends ShellAdapter {
		private UATSoftwareMetricGUI demo;
		public ShellCloseListener( UATSoftwareMetricGUI demo ) {
			this.demo = demo;
		} 
		
		public void shellClosed( ShellEvent e ) {
			demo.shell.dispose();
		}
		
	}

}
