package unittest.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.SWT;

 
import softtest.cfg.c.DumpGraphVisitor;
import softtest.cfg.c.Edge;
import softtest.cfg.c.Graph;
import softtest.cfg.c.GraphVisitor;
import unittest.regressiontest.DumpUncoveredElementsGraphVisitor;
import unittest.regressiontest.TestCaseMapToPathGraphVisitor;
import softtest.cfg.c.VexNode;
import unittest.drawgraphtools.DrawGraph;
import unittest.gui.helper.WidgetFactory;
import unittest.gui.imageViewer.SWTImageCanvas;
import unittest.module.seperate.TestModule;
import unittest.testcase.generate.util.TestCaseNew;
import unittest.testcase.management.news.DBinterfaceNew;
import unittest.testcase.management.news.TestCaseLibManagerNew;
import unittest.util.AnalysisFile;
import unittest.util.Config;
import unittest.util.RecordToLogger;

import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;

public class UATTestCasesToPathGUI {

	protected Shell shell;
	private Table table;
	
	private static Logger logger = Logger.getLogger(UATRegrssionTestingGUI.class);	
	private UATGUI demo;
	
	
	
	public Hashtable<Integer, HashSet<Integer>>  TCpathHashtable;
	public String FullPicFilename = "";
	public String UncoveredElementsFullPicFilename = "";
	public static boolean IsTableNull = true;
	private Table table_1;

	public Hashtable<Integer, HashSet<Integer>> getTCpathHashtable() {
		return TCpathHashtable;
	}

	public void setTCpathHashtable(
			Hashtable<Integer, HashSet<Integer>> tCpathHashtable) {
		TCpathHashtable = tCpathHashtable;
	}

	public UATTestCasesToPathGUI(UATGUI demo2) {
		super();		
		this.demo = demo2;
	}

	public UATTestCasesToPathGUI() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			UATTestCasesToPathGUI window = new UATTestCasesToPathGUI();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				 IsTableNull = true;
			}
		});
		shell.setSize(1411, 793);
		shell.setText("\u5143\u7D20\u8986\u76D6\u60C5\u51B5");
		shell.setLayout(new FormLayout());
		
		Group grpCfg = new Group(shell, SWT.NONE);
		FormData fd_grpCfg = new FormData();
		fd_grpCfg.bottom = new FormAttachment(0, 736);
		fd_grpCfg.right = new FormAttachment(0, 834);
		fd_grpCfg.top = new FormAttachment(0, 10);
		fd_grpCfg.left = new FormAttachment(0, 10);
		grpCfg.setLayoutData(fd_grpCfg);
		grpCfg.setText("CFG");
		grpCfg.setLayout(null);
		
		final SWTImageCanvas imageCanvas = new SWTImageCanvas(grpCfg);
		imageCanvas.setBounds(3, 20, 818, 703);
		imageCanvas.setLayout(null);
		
		Group group_1 = new Group(shell, SWT.NONE);
		FormData fd_group_1 = new FormData();
		fd_group_1.bottom = new FormAttachment(0, 566);
		fd_group_1.top = new FormAttachment(0, 327);
		fd_group_1.left = new FormAttachment(0, 852);
		group_1.setLayoutData(fd_group_1);
		group_1.setText("\u6D4B\u8BD5\u7528\u4F8B");
		
		
		
		
		TableViewer tableViewer = new TableViewer(group_1, SWT.BORDER | SWT.FULL_SELECTION);		
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent arg0) {
				//双击表中一行事件				
				 //javax.swing.JOptionPane.showMessageDialog(null,table.getItem(table.getSelectionIndex()).getText());
 
				
				 
				
				 try{ 
					 
					 
						new Thread()
						{
							public void run()
							{
								try 
								{ 
									Display.getDefault().syncExec(new Runnable()
									{
										public void run()
											
										{
										int tmp = Integer.parseInt(table.getItem(table.getSelectionIndex()).getText());
										HashSet<Integer> myxxset = TCpathHashtable.get(tmp);
										   
										ArrayList  mytmplist = new ArrayList( );  				
										mytmplist.addAll(myxxset);				 
										ArrayList  mytmplist2 = (ArrayList) mytmplist.get(0);	
											ArrayList<VexNode>  nodesOnPath = TransferIntToPathName(mytmplist2);
											DrawTestCaseMapToPathGraph(nodesOnPath); 
										//界面显示
										imageCanvas.loadImage(FullPicFilename);
										 
										imageCanvas.fitCanvas();}
									});
								} catch(Exception e1)
								{
									RecordToLogger.recordExceptionInfo(e1, logger);
									System.gc();
								}


							}
						}.start();
 
			}catch(Exception e1){}}
		});
		table = tableViewer.getTable();
		table.setBounds(10, 23, 499, 206);
		
		TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tableColumn = tableViewerColumn.getColumn();
		tableColumn.setWidth(100);
		tableColumn.setText("\u5E8F\u53F7");
		
		TableViewerColumn tableViewerColumn_1 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tableColumn_1 = tableViewerColumn_1.getColumn();
		tableColumn_1.setWidth(1000);
		tableColumn_1.setText("\u8DEF\u5F84");
		//加这句才显示列头
		table.setHeaderVisible(true);
		
		
		
		Button btnNewButton = new Button(shell, SWT.NONE);
		FormData fd_btnNewButton = new FormData();
		fd_btnNewButton.right = new FormAttachment(0, 980);
		fd_btnNewButton.top = new FormAttachment(0, 599);
		fd_btnNewButton.left = new FormAttachment(0, 882);
		btnNewButton.setLayoutData(fd_btnNewButton);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if( IsTableNull ==  false )
					return;
				/*
				 * Step 1  显示CFG
				 *
				*/			
				final TestModule currentFunc = demo.getCurrentFunc();
				final AnalysisFile currentFile = demo.getCurrentFile();
				try
				{
					if(currentFunc == null )
						return;
					 

					new Thread()
					{
						public void run()
						{
							try 
							{
								IsTableNull =  false ;
								currentFunc.generateCFG(currentFile.getCFGPicDir());
								Display.getDefault().syncExec(new Runnable()
								{
									public void run()
									{
										File pic = new File (currentFunc.getCfgName());
										long size = pic.length();
										if(size >= 3*1024 *1024)//超过10M的图片
										{
											MessageBox box= WidgetFactory.createInfoMessageBox(demo.getShell(), "图片太大", "生成的控制流图的图片太大\n,请到" +currentFunc.getCfgName() +"查看！！\n（选中窗口Ctrl+C,到记事本粘贴路径）");
											box.open();
										}
										else {
											//ImageViewer CFGViewer = new ImageViewer(currentFunc.getCfgName());
											
											imageCanvas.loadImage(currentFunc.getCfgName());
											
											//PictureViewer.showPicture(currentFunc.getCfgName(),currentFunc.getFuncName()+"控制流图");
											demo.actionsGUI.addOutputMessage("生成控制流图" + currentFunc.getFuncName());
										}
									}
								});
							} catch (IOException e) 
							{
								System.gc();
								final String msg = e.getMessage();
								Display.getDefault().syncExec(new Runnable()
								{
									public void run()
									{
										demo.actionsGUI.addOutputMessage("Error when generage the cfg :details " +msg );
									}
								});
								RecordToLogger.recordExceptionInfo(e, logger);
								if(Config.printExceptionInfoToConsole)
									e.printStackTrace();
								return ;
							}
							catch(Exception e1)
							{
								RecordToLogger.recordExceptionInfo(e1, logger);
								System.gc();
							}


						}
					}.start();

				}catch(Exception e1)
				{
					logger.error("生成控制流图时发生异常");
					RecordToLogger.recordExceptionInfo(e1, logger);
					demo.actionsGUI.addOutputMessage("生成控制流图时发生异常 " + e1.getMessage());
				}				
				
				/*
				 * Step 2  读取DB，取出用例对应的路径
				 *
				*/
				DBinterfaceNew myDBDBinterface = DBinterfaceNew.getInstance();
				 
				//暂时为固定值
				myDBDBinterface.openDataBase("D:\\uatProject\\TestProject\\TestProject.mdb");
				
				List<TestCaseNew> ALLTestCaseFromDB = myDBDBinterface.showTestCaseByFuncName(currentFunc.getUniqueFuncName(), currentFunc);
				
				Hashtable<Integer, HashSet<Integer>> myTCpathHashtable = new Hashtable<Integer, HashSet<Integer>>();

				for (int i = 0; i < ALLTestCaseFromDB.size(); i++) {

					// 先对结点排序，再显示。因为这个序列的顺序，只是为用户大致理解
					ArrayList<Integer> listnodes = new ArrayList(
							ALLTestCaseFromDB.get(i).getPath());
					Collections.sort(listnodes);
					// 再保存
					HashSet<Integer> mytempset = new HashSet(Arrays
							.asList(listnodes));
					myTCpathHashtable.put(i, mytempset);

				}
				TCpathHashtable = myTCpathHashtable;
				
				/*
				 * Step 3  在表格中显示路径
				 *
				*/
				

				for (int i = 0; i < myTCpathHashtable.size(); i++) {
					TableItem item = new TableItem(table, SWT.NONE);
					item.setText(0, Integer.toString(i));

					HashSet<Integer> mytmphashset =  myTCpathHashtable.get(i);					
					
					ArrayList mytmplist = new ArrayList(myTCpathHashtable.get(i));  
					String mytmpstring = ""; 
					for(int j = 0;j<mytmplist.size();j++){
						
						mytmpstring += mytmplist.get(j).toString();
					}
					 item.setText(1,mytmpstring);

				}
				
				
				/*
				 * Step 4  显示源代码对应关系
				 *
				*/
				

				Graph graph = currentFunc.getGraph();				
				
				ArrayList<VexNode> listnodes=new ArrayList<VexNode>();
				listnodes.addAll(graph.nodes.values());
				Collections.sort(listnodes);
				 
				for (VexNode tmpnode:listnodes) {
 		
					TableItem item = new TableItem(table_1, SWT.NONE);
					 
					item.setText(0, String.valueOf(listnodes.indexOf(tmpnode)));
					
					item.setText(1, tmpnode.getName());

					int beginline = tmpnode.getTreenode().getBeginLine();
					int begincolumn = tmpnode.getTreenode().getBeginColumn();
					int endline = tmpnode.getTreenode().getEndLine();
					int endcolumn = tmpnode.getTreenode().getEndColumn();
					String filepath = currentFile.getConsoleAlteredFile();

					String tmpString = softtest.database.c.DBAccess
							.getSouceCodeAtom(filepath, beginline, begincolumn,
									endline, endcolumn);
					item.setText(2, tmpString);
					 
				}

			
				
				
				
				
				
				
				
				
				
				 
			}
		});
		btnNewButton.setText("\u83B7\u53D6\u8DEF\u5F84");
		
		Button btnNewButton_1 = new Button(shell, SWT.NONE);
		FormData fd_btnNewButton_1 = new FormData();
		fd_btnNewButton_1.right = new FormAttachment(0, 980);
		fd_btnNewButton_1.top = new FormAttachment(0, 649);
		fd_btnNewButton_1.left = new FormAttachment(0, 882);
		btnNewButton_1.setLayoutData(fd_btnNewButton_1);
		btnNewButton_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				imageCanvas.zoomIn();
			}
		});
		btnNewButton_1.setText("\u653E\u5927");
		
		Button btnNewButton_2 = new Button(shell, SWT.NONE);
		FormData fd_btnNewButton_2 = new FormData();
		fd_btnNewButton_2.right = new FormAttachment(0, 1112);
		fd_btnNewButton_2.top = new FormAttachment(0, 649);
		fd_btnNewButton_2.left = new FormAttachment(0, 1014);
		btnNewButton_2.setLayoutData(fd_btnNewButton_2);
		btnNewButton_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				imageCanvas.zoomOut();
			}
		});
		btnNewButton_2.setText("\u7F29\u5C0F");
		
		Button btnNewButton_3 = new Button(shell, SWT.NONE);
		FormData fd_btnNewButton_3 = new FormData();
		fd_btnNewButton_3.right = new FormAttachment(0, 980);
		fd_btnNewButton_3.top = new FormAttachment(0, 695);
		fd_btnNewButton_3.left = new FormAttachment(0, 882);
		btnNewButton_3.setLayoutData(fd_btnNewButton_3);
		btnNewButton_3.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				imageCanvas.fitCanvas();
			}
		});
		btnNewButton_3.setText("\u9002\u5E94\u7A97\u53E3");
		
		Button btnNewButton_4 = new Button(shell, SWT.NONE);
		FormData fd_btnNewButton_4 = new FormData();
		fd_btnNewButton_4.right = new FormAttachment(0, 1112);
		fd_btnNewButton_4.top = new FormAttachment(0, 695);
		fd_btnNewButton_4.left = new FormAttachment(0, 1014);
		btnNewButton_4.setLayoutData(fd_btnNewButton_4);
		btnNewButton_4.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				imageCanvas.showOriginal();
				 
			}
		});
		btnNewButton_4.setText("\u5B9E\u9645\u5927\u5C0F");
		
		Button btnNewButton_5 = new Button(shell, SWT.NONE);
		btnNewButton_5.addSelectionListener(new SelectionAdapter() {
			@Override//查看未覆盖元素
			public void widgetSelected(SelectionEvent e) {

				 			
				 //javax.swing.JOptionPane.showMessageDialog(null,table.getItem(table.getSelectionIndex()).getText());
 
				 try{ 
						new Thread()
						{
							public void run()
							{
								try 
								{ 
									Display.getDefault().syncExec(new Runnable()
									{
										public void run()
											
										{	 
											
											DrawUncoveredElements();
											 
											 
										//界面显示
										imageCanvas.loadImage(UncoveredElementsFullPicFilename);
										 
										imageCanvas.fitCanvas();}
									});
								} catch(Exception e1)
								{
									RecordToLogger.recordExceptionInfo(e1, logger);
									System.gc();
								}


							}
						}.start();
 
			}catch(Exception e1){}
			}
		});
		FormData fd_btnNewButton_5 = new FormData();
		fd_btnNewButton_5.top = new FormAttachment(btnNewButton, 0, SWT.TOP);
		fd_btnNewButton_5.left = new FormAttachment(btnNewButton_2, 0, SWT.LEFT);
		btnNewButton_5.setLayoutData(fd_btnNewButton_5);
		btnNewButton_5.setText("\u67E5\u770B\u672A\u8986\u76D6\u5143\u7D20");
		
		Group group = new Group(shell, SWT.NONE);
		fd_group_1.right = new FormAttachment(group, 0, SWT.RIGHT);
		group.setText("\u5BF9\u5E94\u6E90\u4EE3\u7801");
		FormData fd_group = new FormData();
		fd_group.top = new FormAttachment(0, 10);
		fd_group.bottom = new FormAttachment(group_1, -6);
		fd_group.right = new FormAttachment(grpCfg, 537, SWT.RIGHT);
		fd_group.left = new FormAttachment(grpCfg, 18);
		group.setLayoutData(fd_group);
		
		TableViewer tableViewer_1 = new TableViewer(group, SWT.BORDER | SWT.FULL_SELECTION);
		table_1 = tableViewer_1.getTable();
		table_1.setHeaderVisible(true);
		table_1.setBounds(10, 26, 499, 275);
		
		TableViewerColumn tableViewerColumn_2 = new TableViewerColumn(tableViewer_1, SWT.NONE);
		TableColumn tableColumn_2 = tableViewerColumn_2.getColumn();
		tableColumn_2.setWidth(100);
		tableColumn_2.setText("\u5E8F\u53F7");
		
		TableViewerColumn tableViewerColumn_3 = new TableViewerColumn(tableViewer_1, SWT.NONE);
		TableColumn tableColumn_3 = tableViewerColumn_3.getColumn();
		tableColumn_3.setWidth(100);
		tableColumn_3.setText("\u8282\u70B9\u540D");
		
		TableViewerColumn tableViewerColumn_4 = new TableViewerColumn(tableViewer_1, SWT.NONE);
		TableColumn tableColumn_4 = tableViewerColumn_4.getColumn();
		tableColumn_4.setWidth(2000);
		tableColumn_4.setText("\u6E90\u4EE3\u7801");

	}
	
	
	/*
	 * 自定义方法
	 */
	
	 
	
	
	/**
	 * 将路径int值，找出其代表的节点名，保存在HashSet<String>中
	 */
	public ArrayList<VexNode> TransferIntToPathName(ArrayList<Integer> mytmplist) {

		TestModule currentFunc = demo.getCurrentFunc();
		Graph mygraph = currentFunc.getGraph();

		// 所有节点
		ArrayList<VexNode> listnodes = new ArrayList<VexNode>();
		listnodes.addAll(mygraph.nodes.values());

		// 路径上节点
		ArrayList<VexNode> nodesOnPath = new ArrayList<VexNode>();

		for (VexNode v : listnodes) {
			for (Integer i : mytmplist) {
				if (v.getSnumber() == i) {
					nodesOnPath.add(v);
					break;
				}
			}
		}

		return nodesOnPath;

	}
	
	/**
	 * 画图
	 */
	public void DrawTestCaseMapToPathGraph(ArrayList<VexNode> nodesOnPath) {
		//Step	1   路径名生成
		String workdir = "";
		String filename = "";
		String FullFilename = "";
		TestModule currentFunc = demo.getCurrentFunc();
		
		workdir = currentFunc.getBelongToFile().getWorkDir() + File.separator
				+ "record" + File.separator;

//		filename = currentFunc.getFileFuncName() + "_"
//				+ currentFunc.getUniqueFuncName();

		filename =  currentFunc.getUniqueFuncName();
		
		File file = new File(workdir);
		file.mkdir();
		FullFilename = workdir + filename;
		FullPicFilename = FullFilename + ".jpg ";
		
			
		//Step	2 访问图
		
		currentFunc.getGraph().accept(new TestCaseMapToPathGraphVisitor(nodesOnPath), FullFilename + ".dot");
		
			
		
		
		//Step	3 生成图片
		
		try {
			java.lang.Runtime.getRuntime().exec("dot -Tjpg -o " + FullFilename + ".jpg " + FullFilename + ".dot").waitFor();
		} catch (IOException e1) {
			System.out.println(e1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		
		System.out.println("控制流图打印到了文件" + FullFilename + ".jpg");
		
		
		

	}
	/**
	 * 显示未覆盖元素
	 */
	public void DrawUncoveredElements() {
		TestModule currentFunc = demo.getCurrentFunc();
		 //处理一下路径，加上未覆盖前缀
		
		//Step	1   路径名生成
		String workdir = "";
		String filename = "";
		String FullFilename = "";
		 
		workdir = currentFunc.getBelongToFile().getWorkDir() + File.separator
				+ "record" + File.separator;

//		filename = currentFunc.getFileFuncName() + "_"
//				+ currentFunc.getUniqueFuncName();

		filename =  currentFunc.getUniqueFuncName();
		
		File file = new File(workdir);
		file.mkdir();
		FullFilename = workdir + filename+ "_Uncovered_";
		UncoveredElementsFullPicFilename = FullFilename + ".jpg ";
		 
		//图的访问者方法
		
		currentFunc.getGraph().accept(new DumpUncoveredElementsGraphVisitor(), FullFilename + ".dot");
		
		//显示未覆盖元素
		
		try {
			java.lang.Runtime.getRuntime().exec("dot -Tjpg -o " + FullFilename + ".jpg " + FullFilename + ".dot").waitFor();
		} catch (IOException e1) {
			System.out.println(e1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
}
