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
	
	private List<Long> testCaseIDSet;//ѡ��Ĳ�����������
	
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
		//������ʾ
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
		root.setText("����������");
		root.setImage(Resource.projectImage);
		
		//��ȡ��ǰ�����Ĳ�������������еĲ���������
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
				if(ti.getText().equals("����������")){
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
		
		patternCombox = WidgetFactory.createCombo(bottomComposite, new String[]{"����ִ��", "����ִ��"});
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
			
	    	if(Config.ExecuteMode==1)//����ִ��
	    	{
	    		demo.uatGui.runSelectedSingleTC(testCaseSet);
	    	}
	    	else if(Config.ExecuteMode==0)//����ִ��
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
//					float lastBlockCoverage = demo.uatGui.getCurrentFunc().getCoverSetList().get(0).getCoverage();//��һ�ε���串����
//					float lastBranchCoverage = demo.uatGui.getCurrentFunc().getCoverSetList().get(1).getCoverage();//��һ�εķ�֧������
//					float lastMcdcCoverage = demo.uatGui.getCurrentFunc().getCoverSetList().get(2).getCoverage();//��һ�ε�MC/DC������
//					//�������ж��Ƿ���Ҫ��ʾ�û������˹��������Ѵﵽ100%�ĸ�����Ҫ��
//					boolean needManualInterval = false;//�Ƿ���Ҫ�˹�����
//					switch(crValue){
//					case 0://û��ѡ����串�ǡ���֧���ǡ�MC/DC����׼���е��κ�һ��
//						break;
//					case 1://ֻѡ����串��
//						if(Math.abs(lastBlockCoverage-1.0)>0.01){
//							needManualInterval = true;
//						}
//						else{
//							needManualInterval = false;
//						}
//						break;
//					case 2://ֻѡ���˷�֧����
//						if(Math.abs(lastBranchCoverage-1.0)>0.01){
//							needManualInterval = true;
//						}
//						else
//						{
//							needManualInterval = false;
//						}
//						break;
//					case 3://ͬʱѡ������串�Ǻͷ�֧����
//						if(Math.abs(lastBlockCoverage-1.0)>0.01 || Math.abs(lastBranchCoverage-1.0)>0.01)
//						{
//							needManualInterval = true;
//						}
//						else{
//							needManualInterval = false;
//						}
//						break;
//					case 4://ֻѡ����MC/DC����
//						if(Math.abs(lastMcdcCoverage-1.0)>0.01)
//						{
//							needManualInterval = true;
//						}
//						else{
//							needManualInterval = false;
//						}
//						break;
//					case 5://ͬʱѡ����MC/DC���Ǻ���串��
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
//					case 6://ͬʱѡ����MC/DC���Ǻͷ�֧����
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
//					case 7://ͬʱѡ����MC/DC���ǡ���֧���Ǻ���串��
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
//					default://���������
//						break;
//					}
//					if(needManualInterval)
//					{
//						NumberFormat numFormater = NumberFormat.getNumberInstance();
//						numFormater.setMaximumFractionDigits(2);
//						String coverage = "";
//						if(demo.uatGui.getCurrentCoverCriteria().BlockCover)
//						{
//							coverage += "��串�ǣ�"+numFormater.format(lastBlockCoverage*100)
//							+"%, ";
//						}
//						if(demo.uatGui.getCurrentCoverCriteria().BranchCover)
//							coverage += "��֧���ǣ�"+numFormater.format(lastBranchCoverage*100)
//							+"%, ";
//						if(demo.uatGui.getCurrentCoverCriteria().MCDCCover)
//							coverage += "MC/DC���ǣ�"+numFormater.format(lastMcdcCoverage*100)
//							+"%, ";
//						MessageBox box = WidgetFactory.createMessageBox(demo.uatGui.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO , 
//								GUILanguageResource.getProperty("NeedManualInterval"), coverage+GUILanguageResource.getProperty("NeedManualIntervalInfo")+"?");
//						int flag = box.open();
//						if(flag == SWT.YES) {
//							if (demo.uatGui.getSaveFile()){
//								MessageBox msgbox = WidgetFactory.createInfoMessageBox(demo.uatGui.getShell(), "��ʾ��Ϣ", "���ȱ����ļ�");
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
