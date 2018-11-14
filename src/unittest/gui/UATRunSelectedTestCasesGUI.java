package unittest.gui;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.testcase.generate.paramtype.AbstractParamValue;
import unittest.testcase.generate.util.TestCaseNew;
import unittest.testcase.management.news.TestCaseLibManagerNew;
import unittest.util.Config;

public class UATRunSelectedTestCasesGUI {

	private Shell shell = null;
	private Display  display = null;
	public UATGUI uatGui = null;
	
	private List<Long> testCaseIDSet;//选择的测试用例集合
	
	private Tree TestCaseTree = null;
	private CLabel infoCLabel = null;
	private Combo patternCombox = null; 
	private Button okButton = null;
	private Button cancelButton = null;
	
	private Composite topComposite = null;
	private Composite midComposite = null;
	private Composite bottomComposite = null;
	private Composite controlComposite = null;
	
	public UATRunSelectedTestCasesGUI(UATGUI uatgui) 
	{
		this.uatGui = uatgui;
		testCaseIDSet=new ArrayList<Long>();
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
		shell.setText( GUILanguageResource.getProperty("RunTestCasesFormat") );
		//居中显示
		Rectangle displayBounds = display.getPrimaryMonitor().getBounds();
        int shellWidth = 400; 
		int shellHeight = 500;   
        shell.setSize(shellWidth, shellHeight);
		int x = displayBounds.x + (displayBounds.width - shellWidth)>>1;   
        int y = displayBounds.y + (displayBounds.height - shellHeight)>>1;   
		shell.setLocation(x,y); 
		shell.setLayout( null );
		
		topComposite = WidgetFactory.createComposite( shell, SWT.BORDER );
		topComposite.setBackground( Resource.backgroundColor );
		topComposite.setBounds( 0, 0, 200, 500);
		topComposite.setLayout( new FillLayout() );
		
		TestCaseTree = new Tree( topComposite,SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL);
		TestCaseTree.setBackground( Resource.backgroundColor );
		TreeItem root = new TreeItem(TestCaseTree, 0);
		root.setText("测试用例树");
		root.setImage(Resource.projectImage);
		
		//获取当前函数的测试用例库界面中的测试用例树
		TreeItem testTree = uatGui.getTestCaseTree().getItem(0);
		TreeItem[] items = testTree.getItems();
		for(int i = items.length-1; i >= 0; i--)
		{
			TreeItem child = new TreeItem(root,SWT.NONE,0);
			child.setForeground(items[i].getForeground());
			child.setText(items[i].getText());
			child.setData(items[i].getData());
		}
		
		TestCaseTree.setBounds(0, 0, 200, 500);
		
		TestCaseTree.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				TreeItem ti = (TreeItem)e.item;
				if(ti.getText().equals("测试用例树")){
					testCaseIDSet.clear();
					TreeItem testTree = TestCaseTree.getItem(0);
					TreeItem[] items = testTree.getItems();
					if(ti.getChecked()){
						for(int i = items.length-1; i >= 0; i--){
							items[i].setChecked(true);
							testCaseIDSet.add((Long)items[i].getData());
						}
					}
					else{
						for(int i = items.length-1; i >= 0; i--)
							items[i].setChecked(false);;
					}
				}
				else{
					if(ti.getChecked())
						testCaseIDSet.add((Long)ti.getData());
					else
						testCaseIDSet.remove((Long)ti.getData());
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		root.setExpanded(true);
		midComposite = WidgetFactory.createComposite( shell, SWT.FLAT );
		midComposite.setLayout( new FillLayout() );
		midComposite.setBounds( 200, 0, 200, 100);
		
		infoCLabel = WidgetFactory.createCLabel( midComposite, SWT.NULL,
				GUILanguageResource.getProperty("RunTestCasesFormatInfo")+":");
		//infoCLabel.setBackground( new Color( null, 230, 250, 175) );
		//infoCLabel.setFont( Resource.courierNew_8_Font );
		infoCLabel.setBounds( 0, 30, 200, 40 );
		
		bottomComposite = WidgetFactory.createComposite( shell, SWT.NULL );
		bottomComposite.setBounds( 200, 130, 200, 50);
		bottomComposite.setLayout( null );
		
		patternCombox = WidgetFactory.createCombo(bottomComposite, new String[]{"连续执行", "单步执行"});
		patternCombox.setBounds( 50, 0, 100, 30 );
		patternCombox.select(0);
		
		controlComposite = WidgetFactory.createComposite( shell, SWT.NULL );
		controlComposite.setLayout(  null );
		controlComposite.setBounds( 200, 200, 200, 100 );
		
		okButton = WidgetFactory.createButton( controlComposite, SWT.PUSH );
		okButton.setText( GUILanguageResource.getProperty("OK") );
		okButton.setFont( Resource.courierNew_10_Font );
		okButton.setBounds( 30, 10, 60, 30 );
		
		cancelButton = WidgetFactory.createButton( controlComposite, SWT.PUSH );
		cancelButton.setText( GUILanguageResource.getProperty("Cancel") );
		cancelButton.setFont( Resource.courierNew_10_Font );
		cancelButton.setBounds( 100, 10, 60, 30 );
	}
	
	private class OkButtonListener extends SelectionAdapter 
	{
		private UATRunSelectedTestCasesGUI demo;
		public OkButtonListener( UATRunSelectedTestCasesGUI demo ) 
		{
			this.demo = demo;
		}

		public void widgetSelected( SelectionEvent e ) 
		{
			//to do  when the languge is changed 
			// the config file should be changed and after
			
			if(patternCombox.getSelectionIndex()==0){
				Config.ExecuteMode=0;
			}
			else if(patternCombox.getSelectionIndex()==1){
				Config.ExecuteMode=1;
			}
			final List<TestCaseNew> testCaseSet = new ArrayList<TestCaseNew>();
			for(int i =0; i < testCaseIDSet.size(); i++){
				System.out.println(testCaseIDSet.get(i));
				testCaseSet.add(TestCaseLibManagerNew.showOneTestCase(uatGui.getCurrentFunc(), testCaseIDSet.get(i)));
			}
			
			for(TestCaseNew tc: testCaseSet) {
				for(AbstractParamValue apv: tc.getFuncParamList()) {
					apv.clearAssginedFlag();
					apv.clearGeneFlag();
				}
				for(AbstractParamValue apv: tc.getGlobalParamList()) {
					apv.clearAssginedFlag();
					apv.clearGeneFlag();
				}
			}
			
//			final List<TestCaseNew> testCaseSet = demo.uatGui.getTestCaseTable().getSelectedTestCaseSet();
			
	    	if(Config.ExecuteMode==1)//单步执行
	    	{
	    		demo.uatGui.runSelectedSingleTC(testCaseSet);
	    	}
	    	else if(Config.ExecuteMode==0)//连续执行
	    	{
	    		demo.uatGui.runSelectedTCSet(testCaseSet, false);
	    		
//	    		if(!Config.IsMutationTesting)
//	    		{
//	    			byte crValue = 0;
//					byte blockCoverTrue = 1;
//					byte branchCoverTrue = 2;
//					byte mcdcCoverTrue = 4;
//					if(demo.uatGui.getCurrentCoverCriteria().BlockCover)
//						crValue ^= blockCoverTrue;
//					if(demo.uatGui.getCurrentCoverCriteria().BranchCover)
//						crValue ^= branchCoverTrue;
//					if(demo.uatGui.getCurrentCoverCriteria().MCDCCover)
//						crValue ^= mcdcCoverTrue;
//					float lastBlockCoverage = demo.uatGui.getCurrentFunc().getCoverSetList().get(0).getCoverage();//上一次的语句覆盖率
//					float lastBranchCoverage = demo.uatGui.getCurrentFunc().getCoverSetList().get(1).getCoverage();//上一次的分支覆盖率
//					float lastMcdcCoverage = demo.uatGui.getCurrentFunc().getCoverSetList().get(2).getCoverage();//上一次的MC/DC覆盖率
//					//接下来判断是否需要提示用户进行人工辅助，已达到100%的覆盖率要求
//					boolean needManualInterval = false;//是否需要人工辅助
//					switch(crValue){
//					case 0://没有选择语句覆盖、分支覆盖、MC/DC覆盖准则中的任何一个
//						break;
//					case 1://只选择语句覆盖
//						if(Math.abs(lastBlockCoverage-1.0)>0.01){
//							needManualInterval = true;
//						}
//						else{
//							needManualInterval = false;
//						}
//						break;
//					case 2://只选择了分支覆盖
//						if(Math.abs(lastBranchCoverage-1.0)>0.01){
//							needManualInterval = true;
//						}
//						else
//						{
//							needManualInterval = false;
//						}
//						break;
//					case 3://同时选择了语句覆盖和分支覆盖
//						if(Math.abs(lastBlockCoverage-1.0)>0.01 || Math.abs(lastBranchCoverage-1.0)>0.01)
//						{
//							needManualInterval = true;
//						}
//						else{
//							needManualInterval = false;
//						}
//						break;
//					case 4://只选择了MC/DC覆盖
//						if(Math.abs(lastMcdcCoverage-1.0)>0.01)
//						{
//							needManualInterval = true;
//						}
//						else{
//							needManualInterval = false;
//						}
//						break;
//					case 5://同时选择了MC/DC覆盖和语句覆盖
//						if(Math.abs(lastBlockCoverage-1.0)>0.01 || 
//								Math.abs(lastMcdcCoverage-1.0)>0.01)
//						{
//							needManualInterval = true;
//						}
//						else
//						{
//							needManualInterval = false;
//						}
//						break;
//					case 6://同时选择了MC/DC覆盖和分支覆盖
//						if(Math.abs(lastBranchCoverage-1.0)>0.01 || 
//								Math.abs(lastMcdcCoverage-1.0)>0.01)
//						{
//							needManualInterval = true;
//						}
//						else
//						{
//							needManualInterval = false;
//						}
//						break;
//					case 7://同时选择了MC/DC覆盖、分支覆盖和语句覆盖
//						if(Math.abs(lastBlockCoverage-1.0)>0.01 || 
//								Math.abs(lastBranchCoverage-1.0)>0.01 || 
//								Math.abs(lastMcdcCoverage-1.0)>0.01)
//						{
//							needManualInterval = true;
//						}
//						else
//						{
//							needManualInterval = false;
//						}
//						break;
//					default://其他的情况
//						break;
//					}
//					if(needManualInterval)
//					{
//						NumberFormat numFormater = NumberFormat.getNumberInstance();
//						numFormater.setMaximumFractionDigits(2);
//						String coverage = "";
//						if(demo.uatGui.getCurrentCoverCriteria().BlockCover)
//						{
//							coverage += "语句覆盖："+numFormater.format(lastBlockCoverage*100)
//							+"%, ";
//						}
//						if(demo.uatGui.getCurrentCoverCriteria().BranchCover)
//							coverage += "分支覆盖："+numFormater.format(lastBranchCoverage*100)
//							+"%, ";
//						if(demo.uatGui.getCurrentCoverCriteria().MCDCCover)
//							coverage += "MC/DC覆盖："+numFormater.format(lastMcdcCoverage*100)
//							+"%, ";
//						MessageBox box = WidgetFactory.createMessageBox(demo.uatGui.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO , 
//								GUILanguageResource.getProperty("NeedManualInterval"), coverage+GUILanguageResource.getProperty("NeedManualIntervalInfo")+"?");
//						int flag = box.open();
//						if(flag == SWT.YES) {
//							if (demo.uatGui.getSaveFile()){
//								MessageBox msgbox = WidgetFactory.createInfoMessageBox(demo.uatGui.getShell(), "提示信息", "请先保存文件");
//								msgbox.open();
//								return;
//							}
//							UATManualInterventionGUI target = new UATManualInterventionGUI(demo.uatGui);
//							target.go();
//						}
//					}
//	    		}
	    	}
	    	Config.isAutoCompare=false;
			demo.uatGui.getShell().setEnabled( true );
			demo.shell.dispose();
		}
	}
	
	private class CancelButtonListener extends SelectionAdapter 
	{
		private UATRunSelectedTestCasesGUI demo;
		public CancelButtonListener( UATRunSelectedTestCasesGUI demo ) 
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
		private UATRunSelectedTestCasesGUI demo;
		public ShellCloseListener( UATRunSelectedTestCasesGUI demo ) 
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
