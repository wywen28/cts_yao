package unittest.gui;
//author by cai min, 2011/5/13, ���϶�λ
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTIterationStatement;
import softtest.ast.c.ASTSelectionStatement;
import softtest.ast.c.SimpleNode;
import unittest.gui.helper.FileCTabItem;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.gui.imageViewer.ImageViewer;
import unittest.localization.GUILanguageResource;
import unittest.module.seperate.TestModule;

public class UATBugLinkGUI {

	static Logger logger = Logger.getLogger(TargetCoverElementSelectGUI.class);
	public UATGUI uatGui = null;
	public Shell shell = null;
	private Display  display = null;
	private Composite upComp = null;
	private Composite downComp = null;
	private Composite topComposite = null;
	private Composite bottomComposite = null;

	// children of topComposite����ʾ������ͼ��Դ����
	private CTabFolder topTabFolder = null;
	private CTabItem sourceTabItem = null;
	private FileCTabItem sourceFileItem = null;
	private CTabItem CFGTabItem = null;
	private ImageViewer imageViewer = null;
	private Composite ImageViewerComposite = null;
	private Composite sourceViewerComposite = null;
	
	private SashForm sashform;
	private Composite leftComposite;
	private Composite rightComposite;
	//private org.eclipse.swt.widgets.List swtList;
	private Tree tree;
	
	private int currnetBugID = 0;
	private List<SimpleNode> currentBlocks;
	
	//private List<HashMap<List<SimpleNode>, Double>> allList;
	private List<HashMap<Double,List<List<SimpleNode>>>> allList;
	
	/*private  MenuItem item1 = null;
	private  MenuItem item2 = null;
	private  MenuItem item3 = null;
	private  MenuItem item4 = null;
	private  MenuItem item5 = null;
	private  MenuItem item6 = null;*/
	
	private Button lastBugButton;
	private Button nextBugButton;
	private Button lastBlockButton;
	private Button nextBlockButton;

	public boolean bugLinkFail = false;//���϶�λ�õ��Ľڵ㼯��Ϊ��ʱ��bugLinkFailΪ��
	
	public UATBugLinkGUI(UATGUI uatGui){
		this.uatGui = uatGui;
		TestModule tm = uatGui.getCurrentFunc();
		HashSet faultIDSet = (HashSet) uatGui.getTestCaseTable().getFaultTCID();
		HashSet correctIDSet = (HashSet) uatGui.getTestCaseTable().getCorrectTCID();
		List<List<SimpleNode>> faultedBlocksArrayList=null;
		List<SimpleNode> nodeList = null;
		if (tm!=null && faultIDSet!=null && correctIDSet!=null)
			allList=tm.setFaultNodes(faultIDSet, correctIDSet);
		if (allList == null){
			bugLinkFail = true;
			MessageBox mb = WidgetFactory.createInfoMessageBox(uatGui.getShell(), "������Ϣ", "�������в�������");
			mb.open();
			return;
		}
		if(allList.size()==0)
		{
			bugLinkFail = true;
			MessageBox mb = WidgetFactory.createInfoMessageBox(uatGui.getShell(), "������Ϣ", "ִ�еĲ�����������ȷ�����ܶ�λ�����ٴ�ȷ�������Ƿ��д�");
			mb.open();
			return;
		}
		if(faultIDSet.size() != 1 || correctIDSet.size() > 0)
		{
			//nodeList = tm.setFaultNodes(faultIDSet, correctIDSet);
			Iterator<List<List<SimpleNode>>> faultedBlocksIterator=allList.get(currnetBugID).values().iterator();
			while(faultedBlocksIterator.hasNext())
			{
				List<List<SimpleNode>> faultSingleBlock=new ArrayList<List<SimpleNode>>();
				faultSingleBlock=faultedBlocksIterator.next();
				for(List<SimpleNode> sn:faultSingleBlock)
				{
					nodeList=sn;
				}
				//���ɶ�
				Double susp = allList.get(currnetBugID).keySet().iterator().next();
			}
		}
		
	}

	public void go(){
		display = Display.getDefault();
		this.createShell();
		this.dealEvent();
		this.shell.open();

		if(bugLinkFail)
		{
			MessageBox mb = WidgetFactory.createInfoMessageBox(shell, "��ʾ��Ϣ", "���϶�λ����Ϊ��");
			mb.open();
		}
		
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
		shell = new Shell(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL);
		shell.setText( GUILanguageResource.getProperty("bugLink") );
		shell.setImage( Resource.bugLinkImage );
		shell.setBounds( 50, 50, 800, 700);
		shell.setLayout( new FormLayout() );
		shell.setMaximized( false );
		
		createUpComposite();
		
		createDownComposite();
		
	}
	
	private void createUpComposite(){
		upComp = WidgetFactory.createComposite(shell, SWT.BORDER);
		WidgetFactory.configureFormData(upComp, new FormAttachment(0, 0), new FormAttachment(0, 0), new FormAttachment(100, 0), new FormAttachment(0, 35));
		upComp.setLayout(new RowLayout());
		
		lastBugButton = new Button(upComp, SWT.PUSH);
		lastBugButton.setImage(Resource.last_bug_Image);
		lastBugButton.setToolTipText("��һ��");
		lastBugButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if(currnetBugID == 0)
					return;
				currnetBugID --;
				currentBlocks =allList.get(currnetBugID).values().iterator().next().get(0);
				setSelectionText();
				tree.setSelection(tree.getItem(currnetBugID));	
				nextBugButton.setEnabled(true);
				if(currnetBugID == 0)
				{
					lastBugButton.setEnabled(false);
				}
					
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
		nextBugButton = new Button(upComp, SWT.PUSH);
		nextBugButton.setImage(Resource.next_bug_Image);
		nextBugButton.setToolTipText("��һ��");
		nextBugButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if(currnetBugID == allList.size() - 1)
				{
					currnetBugID = -1;
				}
				currnetBugID ++;
/*				System.out.println("��ǰ�ţ�"+currnetBugID);
				System.out.println("����������鼯�ϣ�"+allList);
				System.out.println("��ǰ���������������"+currentBlocks.size());*/
				currentBlocks =allList.get(currnetBugID).values().iterator().next().get(0);
				setSelectionText();
				tree.setSelection(tree.getItem(currnetBugID));		
				lastBugButton.setEnabled(true);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
		lastBlockButton = new Button(upComp, SWT.PUSH);
		lastBlockButton.setImage(Resource.last_block_Image);
		lastBlockButton.setToolTipText("��һ������");
		lastBlockButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				List<List<SimpleNode>> tmp = allList.get(currnetBugID).values().iterator().next();
				int index = tmp.indexOf(currentBlocks);
				nextBlockButton.setEnabled(true);
				if((index-1)==0)
				{
					  lastBlockButton.setEnabled(false);
				}
				if((index - 1) >= 0 && (index - 1) < tmp.size()){
					currentBlocks = tmp.get(index - 1);
					setSelectionText();
					TreeItem currentSelectedItem = tree.getSelection()[0];
					if(currentSelectedItem.getItems().length == 0){//ѡ�ڹ�����
						tree.setSelection(currentSelectedItem.getParentItem().getItem(index - 1));
					}
					else {
						tree.setSelection(currentSelectedItem.getItem(index-1));
					}
						
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		nextBlockButton = new Button(upComp, SWT.PUSH);
		nextBlockButton.setImage(Resource.next_block_Image);
		nextBlockButton.setToolTipText("��һ������");
		nextBlockButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				List<List<SimpleNode>> tmp = allList.get(currnetBugID).values().iterator().next();
				int index = tmp.indexOf(currentBlocks);
/*				System.out.println("��ǰ�ţ�"+currnetBugID); //added
				System.out.println("����������鼯�ϣ�"+allList);
				System.out.println("��ǰ���������������"+currentBlocks.size());*/
				lastBlockButton.setEnabled(true);
				if((index + 1) >= 0 && (index + 1) <= tmp.size()) {
					if((index+1) == tmp.size())
					{
						currentBlocks=tmp.get(0);
						setSelectionText();
						TreeItem currentSelectedItem = tree.getSelection()[0];
						if(currentSelectedItem.getItems().length != 0){//ѡ�ڹ�����
							tree.setSelection(currentSelectedItem.getItem(0));
						}
						else {
							tree.setSelection(currentSelectedItem.getParentItem().getItem(0));
						}
					}
					else
					{
						currentBlocks = tmp.get(index + 1);
						setSelectionText();
						TreeItem currentSelectedItem = tree.getSelection()[0];
						if(currentSelectedItem.getItems().length != 0){//ѡ�ڹ�����
							tree.setSelection(currentSelectedItem.getItem(index+1));
						}
						else {
							tree.setSelection(currentSelectedItem.getParentItem().getItem(index + 1));
						}
					}
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		if(allList.size() == 0){
			lastBlockButton.setEnabled(false);
			nextBlockButton.setEnabled(false);
			lastBugButton.setEnabled(false);
			nextBugButton.setEnabled(false);
		}
	}
	
	private void createDownComposite(){
		downComp = WidgetFactory.createComposite(shell, SWT.BORDER);
		WidgetFactory.configureFormData(downComp, new FormAttachment(0, 0), new FormAttachment(upComp), new FormAttachment(100, 0), new FormAttachment(100,0));
		downComp.setLayout(new FillLayout());
		sashform = new SashForm(downComp,SWT.HORIZONTAL);
		sashform.setLayout(new FillLayout());
		leftComposite = WidgetFactory.createComposite(sashform, SWT.NONE);
		leftComposite.setLayout(new FillLayout());
		rightComposite = WidgetFactory.createComposite(sashform, SWT.NONE);
		rightComposite.setLayout(new FormLayout());
		sashform.setWeights(new int[]{1,3});
		createLeftComposite();
		createRightComposite();
	}
	
	private void createLeftComposite(){
		tree = new Tree(leftComposite, SWT.BORDER);
		if(allList.size() == 0)
			return;
		for(int i = 0; i < allList.size(); i ++) {
			Double susp=allList.get(i).keySet().iterator().next();
			String suspStr = String.format("%.2f", susp); //ֻ��2��С����
			TreeItem item = new TreeItem(tree,SWT.NONE);
			item.setText("#��" + i + "��(���ɶȣ�" + suspStr + ")");
			List<List<SimpleNode>> tmp=allList.get(i).values().iterator().next();
			int j = 0;
			for(List<SimpleNode> list : tmp) {
				TreeItem child = new TreeItem(item, SWT.NONE);
				child.setText("��" + (j + 1)  + "������");
				j ++;
			}
			item.setExpanded(true);
		}
		tree.setSelection(tree.getItem(0));
		tree.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				TreeItem[] selected = tree.getSelection();
				if(selected.length <= 0)
					return;
				if(selected[0].getItemCount() != 0){//����ǹ�����
					int index = tree.indexOf(selected[0]);
					currnetBugID = index;
					currentBlocks =allList.get(currnetBugID).values().iterator().next().get(0);
					setSelectionText();
					//createPopupMenu();
				}
				else{//�����������
					int index = selected[0].getParentItem().indexOf(selected[0]);
					currnetBugID = tree.indexOf(selected[0].getParentItem());
					List<List<SimpleNode>> tmp=allList.get(currnetBugID).values().iterator().next();
					currentBlocks = tmp.get(index);
					setSelectionText();
					//createPopupMenu();
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
	}

	private void createRightComposite(){
		topComposite = WidgetFactory.createComposite(rightComposite, SWT.FLAT );
		topComposite.setBackground( Resource.backgroundColor );
		topComposite.setLayout( new FormLayout() );
		WidgetFactory.configureFormData( topComposite, new FormAttachment( 0, 5 ),
				new FormAttachment( 0, 5 ),
				new FormAttachment( 100, -5 ),
				new FormAttachment( 100, -5));

		topTabFolder = WidgetFactory.createCTabFoler(topComposite, 0);
		WidgetFactory.configureFormData( topTabFolder, new FormAttachment( 0, 1 ),
				new FormAttachment( 0, 1 ),
				new FormAttachment( 100, -1 ),
				new FormAttachment( 100, -1));
		if(uatGui != null){
			ShowFunction();
			//createPopupMenu();
		}

//		add by chenruolin ������ʾ������ͼ
//		ImageViewerComposite = WidgetFactory.createComposite(topTabFolder, SWT.BORDER);
//		ImageViewerComposite.setLayout(new FormLayout());
//		WidgetFactory.configureFormData( ImageViewerComposite, new FormAttachment( 0, 0 ),
//				new FormAttachment( 0, 0 ),
//				new FormAttachment( 100, 0 ),
//				new FormAttachment( 100, 0));
//		uatGui.getCurrentFunc().getOutputTool().drawFaultNodes();
//		imageViewer = new ImageViewer(ImageViewerComposite, SWT.NONE);// new ImageViewer(topTabFolder, 1 ,uatGui.getCurrentFunc().getFaultNodesCfgName());
//		imageViewer.loadImage(uatGui.getCurrentFunc().getFaultNodesCfgName());
		
//		CFGTabItem = WidgetFactory.createCTabItem(topTabFolder, GUILanguageResource.getProperty("ControlFlowPic"), null, ImageViewerComposite);
		
		topTabFolder.setSelection(sourceFileItem.getCTabItem());
	}

	public void dealEvent(){
		shell.addShellListener( new ShellCloseListener( this ) );
		/*okButton.addSelectionListener(new SelectionAdapter() 
		{
			public void widgetSelected( SelectionEvent e ) 
			{
				uatGui.getShell().setEnabled( true );
				shell.dispose();
			}
		});*/
	}

	//��ʾ���⺯��Դ����
	private void ShowFunction(){
		TestModule tm = uatGui.getCurrentFunc();
		
		File file = new File(uatGui.getCurrentFunc().getFileName());
		sourceFileItem =null;
		sourceFileItem = WidgetFactory.createFileCTabItem( file, topTabFolder, uatGui );
		setCodeContents( file, sourceFileItem.getSourceViewer() );
		
		if(allList.size() != 0)
		try {
			currentBlocks =allList.get(currnetBugID).values().iterator().next().get(0);
			setSelectionText();
		}
		catch(Exception e)
		{
			MessageBox mb = WidgetFactory.createErrorMessageBox( shell, "������Ϣ", "��ʾ " + tm.getFuncName()+ " �����쳣!"  );
			mb.open();
			e.printStackTrace();
		}
	}
	
	private  void setSelectionText() {
		//update by cai min, 2011/7/6, avoid IllegalArgumentException
			//StyleRange[] styleRange = initCoveredRange(allList.get(currnetBugID).keySet().iterator().next());
		//List<List<SimpleNode>> tmp=allList.get(currnetBugID).values().iterator().next();
		//for(List<SimpleNode> ele:tmp)
		//{
			//currentBlocks = ele;
			StyleRange[] styleRange = initCoveredRange(currentBlocks);
			//add by chenruolin
			if (styleRange.length == 0)
				return;
			StyledText codeStyledText = sourceFileItem.getSourceViewer().getTextWidget();
			codeStyledText.setStyleRanges(styleRange);
			sourceFileItem.setCodeStyledText(codeStyledText);
			codeStyledText.setSelection(styleRange[0].start);
		//}
	
	}
	
	private StyleRange[] initCoveredRange(List<SimpleNode> nodeList) {

		Vector<StyleRange> range = new Vector<StyleRange>();

		for(SimpleNode node : nodeList)
		{
				if(node instanceof ASTSelectionStatement)
				{
					StyleRange sr = new StyleRange();
					sr.background = Display.getDefault().getSystemColor(SWT.COLOR_RED);
					int begin = uatGui.getCurrentFunc().getBeginStreamByLine(node.getBeginLine() - 1) + node.getBeginColumn();
					int end = uatGui.getCurrentFunc().getBeginStreamByLine(node.getBeginLine());
					sr.start = begin-1;
					sr.length = end - begin +1;
					range.add(sr);
				}
				else if(node instanceof ASTFunctionDefinition)
					continue;
				else if(node instanceof ASTIterationStatement)
					continue;
				else{
				StyleRange sr = new StyleRange();
				sr.background = Display.getDefault().getSystemColor(SWT.COLOR_RED);
				int begin = uatGui.getCurrentFunc().getBeginStreamByLine(node.getBeginLine() - 1) + node.getBeginColumn();
				int end = uatGui.getCurrentFunc().getBeginStreamByLine(node.getEndLine() - 1) + node.getEndColumn();
				sr.start = begin-1;
				sr.length = end - begin +1;
				range.add(sr);
			}
		}

		Comparator ct = new StyleRanleComparator();
		Collections.sort(range,ct);
		Vector<StyleRange> rangeTmp = new Vector<StyleRange>();

		if(range.size() >0)
			rangeTmp.add(range.elementAt(0));
		for(int i =1;i< range.size();i++)
		{
			StyleRange preSR = range.get(i-1);
			StyleRange curSR = range.get(i);
			if(preSR.start == curSR.start)
			{

			}//���滻����endCol��beginColoum����
			else if(preSR.start < curSR.start && preSR.start + preSR.length >= curSR.length + curSR.start)
			{
				//ɾ��ǰ���Ǹ�
				rangeTmp.remove(preSR);
				rangeTmp.add(curSR);
			}
			else if(preSR.start < curSR.start && preSR.start + preSR.length > curSR.start)
			{
				rangeTmp.remove(preSR);
				preSR.length = curSR.start + curSR.length - preSR.start;
				rangeTmp.add(preSR);
			}
			else
				rangeTmp.add(curSR);

		}

		StyleRange[] srs = new StyleRange[rangeTmp.size()];
		int i=0;
		for(StyleRange sr : rangeTmp)
			srs[i++] = sr;

		return srs;
	
	}
	
	class StyleRanleComparator implements Comparator{

		@Override
		public int compare(Object o1, Object o2) {
			StyleRange sr1 = (StyleRange)o1;
			StyleRange sr2 = (StyleRange)o2;
			if(sr1.start != sr2.start)
				return sr1.start - sr2.start;
			return sr1.length - sr2.length;
		}

	}

	//������ʾ��ѡ������ʹ�����ⲿ���
	public boolean setCodeContents( File file, SourceViewer  sourceViewer) 
	{
		boolean returnVal = false;
		if( file.isDirectory() ) 
		{
			returnVal = false;
		}
		else 
		{
			try {
				FileInputStream  fin;

				fin = new FileInputStream( file );

				int ch;
				StringBuffer data = new StringBuffer();
				while( ( ch = fin.read() ) != -1 ) {
					data.append( ( char )ch );
				} 
				if( data != null ) {
					String contents = new String( data.toString().getBytes( "ISO-8859-1" ), "GBK"   );
					sourceViewer.setDocument(new Document(contents));
				}
				fin.close();
				returnVal = true;
			} catch( Exception e ) {
				returnVal = false;
				MessageBox mb = WidgetFactory.createErrorMessageBox( shell, "������Ϣ", "�ļ� " + file.getAbsolutePath() + " ������!"  );
				mb.open();
				e.printStackTrace();
			}
		}
		return returnVal;
	}

	public class ShellCloseListener extends ShellAdapter {
		private UATBugLinkGUI demo;
		public ShellCloseListener( UATBugLinkGUI uatBugLinkGUI ) {
			this.demo = uatBugLinkGUI;
		} 

		public void shellClosed( ShellEvent e ) {
			demo.uatGui.getShell().setEnabled( true );
			if(demo.imageViewer != null)
			demo.imageViewer.dispose();
			demo.shell.dispose();
		}

	}

	/*private void createPopupMenu(){
		Menu menu = new Menu (sourceFileItem.getSourceViewer().getTextWidget());
		item1 = WidgetFactory.createMenuItem(menu, SWT.PUSH, "��һ�����ϵ�", null, -1, false);
		item2 = WidgetFactory.createMenuItem(menu, SWT.PUSH, "��һ�����ϵ�", null, -1, true);
		item3 = WidgetFactory.createMenuItem(menu, SWT.CASCADE, "���й��ϵ�", null, -1, true);
		WidgetFactory.createMenuItem(menu, SWT.SEPARATOR,"", null, -1, true );
		item4 = WidgetFactory.createMenuItem(menu, SWT.PUSH, "��һ�����ϵ�", null, -1, true);
		item5 = WidgetFactory.createMenuItem(menu, SWT.PUSH, "���һ�����ϵ�", null, -1, true);
		WidgetFactory.createMenuItem(menu, SWT.SEPARATOR,"", null, -1, true );
		item6 = WidgetFactory.createMenuItem(menu, SWT.CASCADE, "����", null, -1, true);
		
		item1.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if(currnetBugID == 0)
					return;
				currnetBugID --;
				currentBlocks =allList.get(currnetBugID).values().iterator().next().get(0);
				setSelectionText();
				if(currnetBugID == 0)
					item1.setEnabled(false);
				if(currnetBugID != allList.size() - 1)
					item2.setEnabled(true);
				tree.setSelection(tree.getItem(currnetBugID));
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		item2.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if(currnetBugID == allList.size() - 1)
					return;
				currnetBugID ++;
				currentBlocks =allList.get(currnetBugID).values().iterator().next().get(0);
				setSelectionText();
				if(currnetBugID != 0)
					item1.setEnabled(true);
				if(currnetBugID == allList.size() - 1)
					item2.setEnabled(false);
				tree.setSelection(tree.getItem(currnetBugID));
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		item4.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				currnetBugID = 0;
				currentBlocks =allList.get(currnetBugID).values().iterator().next().get(0);
				setSelectionText();
				item1.setEnabled(false);
				if(currnetBugID == allList.size() - 1)
					item2.setEnabled(false);
				else
					item2.setEnabled(true);
				tree.setSelection(tree.getItem(currnetBugID));
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		item5.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				currnetBugID = allList.size() - 1;
				currentBlocks =allList.get(currnetBugID).values().iterator().next().get(0);
				setSelectionText();
				item2.setEnabled(false);
				if(currnetBugID == 0)
					item1.setEnabled(false);
				else
					item1.setEnabled(true);
				tree.setSelection(tree.getItem(currnetBugID));
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		if(currnetBugID == 0)
			item1.setEnabled(false);
		if(currnetBugID == allList.size() - 1)
			item2.setEnabled(false);
		
		Menu subMenu = new Menu (menu);
		item3.setMenu (subMenu);
		for(int i = 0; i < allList.size(); i ++) {
			MenuItem subItem = WidgetFactory.createMenuItem(subMenu, SWT.RADIO, "��" + (i + 1)  + "�����ϵ�", null, -1, true);
			final int index = i;
			subItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					currnetBugID = index;
					currentBlocks =allList.get(currnetBugID).values().iterator().next().get(0);
					setSelectionText();
					if(currnetBugID != 0)
						item1.setEnabled(true);
					else
						item1.setEnabled(false);
					if(currnetBugID == allList.size() - 1)
						item2.setEnabled(false);
					else
						item2.setEnabled(true);
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {}
			});
		}
		
		Menu subMenu2 = new Menu (menu);
		item6.setMenu (subMenu2);
		List<List<SimpleNode>> tmp=allList.get(currnetBugID).values().iterator().next();
		int i = 0;
		for(List<SimpleNode> ele:tmp)
		{
			MenuItem subItem = WidgetFactory.createMenuItem(subMenu2, SWT.RADIO, "��" + (i + 1)  + "������", null, -1, true);
			final int index = i;
			final List<SimpleNode> currNodes = ele;
			subItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					//currnetBugID = index;
					currentBlocks = currNodes;
					setSelectionText();
					if(currnetBugID != 0)
						item1.setEnabled(true);
					else
						item1.setEnabled(false);
					if(currnetBugID == allList.size() - 1)
						item2.setEnabled(false);
					else
						item2.setEnabled(true);
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {}
			});
			i++;
		}
		
		sourceFileItem.getSourceViewer().getTextWidget().setMenu(menu);
	}*/

}
