package unittest.gui;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.SimpleNode;
import softtest.callgraph.c.CVexNode;
import softtest.symboltable.c.MethodNameDeclaration;
import unittest.Exception.StubGenerateException;
import unittest.Exception.UnsupportedReturnTypeException;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.gui.imageViewer.ImageViewer;
import unittest.module.Module;
import unittest.module.seperate.MultiFuncsTestModule;
import unittest.module.seperate.TestModule;
import unittest.unit.seperate.TestUnit;
import unittest.unit.seperate.TestUnitGenerator;
import unittest.util.AnalysisFile;
import unittest.util.Config;
import unittest.util.RecordToLogger;

/**
 * add by xujiaoxian
 * 人工模块划分使用，为统一接口，声明此界面类，废弃了原来的CATManualUnitSeparateGUI
 *
 */
public class CATManualModuleSeparateGUI
{
	private Display display;
	private Shell shell;
	
	private AnalysisFile file;
	
	private Composite infoComposite;
	private CLabel infoText;
	
	//private Composite unitResultComposite;
	private Group listGroup;
	private ScrolledComposite allModulesComp;
	private java.util.List<CVexNode> allVexNodeList;
	private ArrayList<Button> vexNodeButtons; 
	private Button ensureSelectionButton;//勾选模块的确认按钮
	private Button selectNoneButton;
	private List unitList;
	private List functionInUnitList;
//	private ArrayList<TestUnit> orig_units;
	private ArrayList<TestModule> orig_units;
//	private ArrayList<TestUnit> units;
	/**
	 * 由两个以上的函数单元组成的模块放在这个链表里
	 */
	private ArrayList<TestModule> units;
	private Button delUnitButton;
	//private Button delModuleButton;
	private Button restoreButton;
	private Button okButton;
	
	private Group callGraphGroup;
	
	public CATManualModuleSeparateGUI(AnalysisFile file) {
		this.file = file;
		orig_units = new ArrayList<TestModule>();
		for(TestModule u : file.getFunctionList())
			orig_units.add(u);
		for(TestModule tm : file.getFunctionList())
			file.addTestModuleToFuncListBak(tm);
		file.getFunctionList().clear();
		units = new ArrayList<TestModule>();
//		for(TestModule u : orig_units)
//			units.add(u);
//		for(TestModule u : orig_units)
//			units.add(u.clone());
		file.dumpCallGraph();
		go();
	}
	
	public void doRefresh()
	{
		if(unitList.getSelection().length == 0)
			delUnitButton.setEnabled(false);
		else
			delUnitButton.setEnabled(true);
		
	}
	
	public  void dealEvent() 
	{
		shell.addShellListener( new ShellCloseListener( this ) );
		
		unitList.addSelectionListener(new UnitListSelection(this));
		
		ensureSelectionButton.addSelectionListener(new ensureSelection(this));
	}
	
	 
	 public void createShell()
	 {
		 	//units = new HashMap<String, ArrayList<String>>();
		 	display = Display.getDefault(); 
	        shell = new Shell(display, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL);
	        shell.setImage(Resource.UATImage);
	        shell.setText("模块划分");
	        LayoutUtil.centerShell(display, shell);
	        shell.setLayout( new FormLayout());  
	        
	        infoComposite = new Composite(shell,SWT.BORDER);
	        WidgetFactory.configureFormData(infoComposite, new FormAttachment(0, 0), new FormAttachment(0, 0), new FormAttachment(100, 0), new FormAttachment(0, 50));
	        infoText = new CLabel(infoComposite,SWT.FLAT);
	        infoText.setBounds(0, 2, 483, 45);
	        infoText.setText("为" + file.getFile() + "文件进行模块划分\n(系统已根据调用关系给出默认的模块划分)");
	        
	        listGroup = new Group(shell, SWT.NULL);
	        WidgetFactory.configureFormData(listGroup, new FormAttachment(0, 0), new FormAttachment(infoComposite, 5), new FormAttachment(50, 0), new FormAttachment(100, -50));
	        listGroup.setLayout(new FormLayout());
	        listGroup.setText("模块划分");
	        
	        //unitResultComposite = new Composite(shell, SWT.BORDER);
	        //WidgetFactory.configureFormData(unitResultComposite, new FormAttachment(0, 0), new FormAttachment(infoComposite, 5), new FormAttachment(50, 0), new FormAttachment(100, -50));
	        //unitResultComposite.setLayout(new FormLayout());
	        
	        allModulesComp = new ScrolledComposite(listGroup, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
	        WidgetFactory.configureFormData(allModulesComp, new FormAttachment(0, 0), new FormAttachment(0,0), new FormAttachment(100, 0), new FormAttachment(50, -35));
	        Composite allModulesComp_ = WidgetFactory.createComposite(allModulesComp, SWT.NONE);
	        allModulesComp_.setLayout(new FillLayout(SWT.VERTICAL));
	        allModulesComp.setContent(allModulesComp_);
	        allVexNodeList = file.getAllMethodVexNode();
	        vexNodeButtons = new ArrayList<Button>();
	        if (allVexNodeList != null){
	        	for(CVexNode cvnode : allVexNodeList) {
	        		SimpleNode sn = cvnode.getMethodNameDeclaration().getNode();
	        		if(sn instanceof ASTFunctionDefinition) {
	        			Button bt = WidgetFactory.createButton(allModulesComp_, SWT.CHECK);
	        			ASTFunctionDefinition fd = (ASTFunctionDefinition)sn;
	        			MethodNameDeclaration mnd = fd.getDecl();
	        			String name = mnd.toString();
	        			String fileName;
	        			if(Config.os.equals("windows"))
	        			{
	        				fileName = sn.getFileName().replaceAll("\\\\+", "\\\\");
	        			}
	        			else{
	        				fileName = sn.getFileName().replaceAll("//+", File.separator);
	        			}
	        			name = name + "_" + fileName;
	        			bt.setText(name);
	        			bt.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
	        			vexNodeButtons.add(bt);
	        		}
	        	}
	        }
	        allModulesComp_.setSize(allModulesComp_.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	        allModulesComp.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
	        ensureSelectionButton = WidgetFactory.createButton(listGroup, SWT.PUSH);
	        ensureSelectionButton.setText("生成模块");
	        selectNoneButton = WidgetFactory.createButton(listGroup, SWT.PUSH);
	        selectNoneButton.setText("取消选中");
	        WidgetFactory.configureFormData(ensureSelectionButton, 
	        		new FormAttachment(0, 30), new FormAttachment(50, -30), null, null);
	        WidgetFactory.configureFormData(selectNoneButton, 
	        		new FormAttachment(50, 35), new FormAttachment(50, -30), null, null);
	        
	        selectNoneButton.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					for(Button bt : vexNodeButtons) {
						if(bt.getSelection())
							bt.setSelection(false);
					}
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
				}
			});
	        
	        
	        unitList = new List(listGroup, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
	        WidgetFactory.configureFormData(unitList, 
	        		new FormAttachment(0, 0), new FormAttachment(50, 0), new FormAttachment(50, -65), new FormAttachment(100, -35));
	        unitList.setToolTipText("已划分的模块");
	        
	        for(int i = 0; i < units.size(); i ++) {
	        	unitList.add("默认模块" + i, i);
	        }
	        
	        functionInUnitList = new List(listGroup, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
	        WidgetFactory.configureFormData(functionInUnitList, new FormAttachment(50, -55), new FormAttachment(50, 0), new FormAttachment(100,0), new FormAttachment(100,-35));
	        unitList.setToolTipText("模块内的函数");
	        
	        delUnitButton = new Button(listGroup, SWT.NONE);
	        WidgetFactory.configureFormData(delUnitButton, new FormAttachment(0, 30), new FormAttachment(unitList,5), null, null);
	        delUnitButton.setText("删除模块");
	        delUnitButton.setToolTipText("删除模块");
	        delUnitButton.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					int i = unitList.getSelectionIndex();
					if(i != -1) {
						unitList.remove(i);
						units.remove(i);
					}
					doListRefresh();
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					
				}
			});
	        
	        /*delModuleButton = new Button(listGroup, SWT.NONE);
	        WidgetFactory.configureFormData(delModuleButton, new FormAttachment(50, 35), new FormAttachment(functionInUnitList,5), null, null);
	        delModuleButton.setText("删除单元");
	        delModuleButton.setToolTipText("删除模块里的单元");
	        delModuleButton.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					int i = functionInUnitList.getSelectionIndex();
					if(i != -1) {
						functionInUnitList.remove(i);
						int unitIndex = unitList.getSelectionIndex();
						if(i == 0)
							units.get(unitIndex).setUnitEntryFunction(null);
						else
							units.get(unitIndex).getUnitOtherFunctions().remove(i - 1);
					}
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
				}
			});*/
	        
	        callGraphGroup = new Group(shell, SWT.NULL);
	        WidgetFactory.configureFormData(callGraphGroup, new FormAttachment(50, 5), new FormAttachment(infoComposite, 5), new FormAttachment(100, 0), new FormAttachment(100, -50));
	        callGraphGroup.setLayout(new FormLayout());
	        callGraphGroup.setText("单元调用关系图");
	        new ImageViewer(callGraphGroup, 1).loadImage(file.getCallGraphPicName() + ".jpg");
	        
	        restoreButton = new Button(shell, SWT.NONE);
	        FormData fd_button = new FormData();
	        fd_button.bottom = new FormAttachment(100, -10);
	        fd_button.left = new FormAttachment(0, 50);
	        restoreButton.setLayoutData(fd_button);
	        restoreButton.setText("恢复默认");
	        restoreButton.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent arg0) {
//					units = new ArrayList<TestUnit>();
//					for(TestUnit u : orig_units)
//						units.add(u.clone());
					if(units!=null)
						units.clear();
					units = null;
					units = new ArrayList<TestModule>();
					for(TestModule u : orig_units)
						units.add(u);
					doListRefresh();
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
				}
			});
	        
	        okButton = new Button(shell, SWT.NONE);
	        FormData fd_button_2 = new FormData();
	        fd_button_2.bottom = new FormAttachment(100, -10);
	        fd_button_2.right = new FormAttachment(100, -78);
	        okButton.setLayoutData(fd_button_2);
	        okButton.setText(" 确  定 ");
	        okButton.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent arg0) {
//					ArrayList<TestUnit> tus = file.getAllTestUnit();
//					tus.clear();
//					for(TestUnit tu : units)
//						file.addTestUnit(tu);
					file.getFunctionList().clear();
					for(TestModule tm : units)
						file.addFunction(tm);
					
					//add by chenruolin
					//file.doModuleSeparate();
					//file.setHasAnalysised(true);
					
					shell.dispose();
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
				}
			});
	 }
	 
	 
	 
	 
	 private void go()
	 {
		 	createShell(); 
		 	dealEvent();
		   
	      //  shell.pack(); 
		 	shell.setSize(800, 600);
	        
	        shell.open();  
	        while (!shell.isDisposed()) 
	        {  
	            if (!display.readAndDispatch())  
	                display.sleep();  
	        }  
	      //  display.dispose(); 
	 }

	private class UnitListSelection implements SelectionListener
	{
		private CATManualModuleSeparateGUI demo;

		public UnitListSelection(CATManualModuleSeparateGUI testModuleSeparate) {
			demo = testModuleSeparate;

		}

		@Override
		public void widgetDefaultSelected(SelectionEvent arg0) 
		{
			demo.doRefresh();
			
		}

		@Override
		public void widgetSelected(SelectionEvent arg0) 
		{
			int i = unitList.getSelectionIndex();
			if(i != -1)
			{
				//String id = unitList.getItem(i);
				demo.functionInUnitList.removeAll();
				TestModule tmp = demo.units.get(i);
				for(SimpleNode sn : tmp.getAllFunctions()) {
					if(sn instanceof ASTFunctionDefinition) {
						ASTFunctionDefinition fd = (ASTFunctionDefinition)sn;
						MethodNameDeclaration mnd = fd.getDecl();
						String name = mnd.toString();
						String fileName = sn.getFileName().replaceAll("\\\\+", "\\\\");
						name = name + "_" + fileName;
						demo.functionInUnitList.add(name);
					}
				}
			}
			demo.doRefresh();
		}
	}
	
	/**
	 * This is the ShellListener of UATNewProjectGui.
	 * @author joaquin(孙华衿)
	 *
	 */
	public class ShellCloseListener extends ShellAdapter
	{
		private CATManualModuleSeparateGUI demo;
		public ShellCloseListener(CATManualModuleSeparateGUI de ) 
		{
			demo = de;
		} 
		
		public void shellClosed( ShellEvent e ) 
		{
			shell.dispose();
		}
		
	}
	
	private void doListRefresh(){
		unitList.removeAll();
		for(int i = 0; i < units.size(); i ++) {
			if(orig_units.contains(units.get(i)))
				unitList.add("默认模块" + i, i);
			else
				unitList.add("模块" + i, i);
        }
		functionInUnitList.removeAll();
	}
	
	
	private class ensureSelection implements SelectionListener
	{
		private CATManualModuleSeparateGUI demo;

		public ensureSelection(CATManualModuleSeparateGUI demo) {
			this.demo = demo;

		}

		@Override
		public void widgetDefaultSelected(SelectionEvent arg0) 
		{
			//demo.doRefresh();
		}

		@Override
		public void widgetSelected(SelectionEvent arg0) 
		{
			ArrayList<CVexNode> selectedNodes = new ArrayList<CVexNode>();
			for(int i = 0; i < vexNodeButtons.size(); i ++) {
				Button bt = vexNodeButtons.get(i);
				if(bt.getSelection())
					selectedNodes.add(allVexNodeList.get(i));
			}
			if(selectedNodes.size() == 0)
				return;
			TestUnitGenerator tuGen = new TestUnitGenerator(selectedNodes);
			TestUnit tu = tuGen.getTestUnit();
			if(tu == null) {
				MessageBox box = WidgetFactory.createInfoMessageBox(shell, "info", "所选单元不能构成模块");
				box.open();
				return;
			}
            tu.setAnalysisFile(file);
            MultiFuncsTestModule tm = new MultiFuncsTestModule(tuGen.getEntry(), tu);
			boolean error = false;
			try {
				for (SimpleNode n : tu.getUnitOtherFunctions())
					((ASTFunctionDefinition)n).getDecl().setIsMutil(true);//设置模块中被调用的函数不打桩
				file.doStubInformationGenerate();
				file.doStubCodeGenerate();
			} catch (StubGenerateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			}catch (UnsupportedReturnTypeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			if(units.contains(tm))
				return;
			tm.setBelongToFile(file);
			units.add(tm);
			int size = units.size() - 1;
			unitList.add("模块" + size+":"+tm.getFuncName(), size);
		}
	}
	
}
