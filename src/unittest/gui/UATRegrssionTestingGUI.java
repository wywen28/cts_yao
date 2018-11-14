package unittest.gui;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import softtest.ast.c.ASTFunctionDefinition;
import softtest.cfg.c.Graph;
import softtest.cfg.c.VexNode;
import softtest.symboltable.c.MethodNameDeclaration;
import unittest.gui.imageViewer.ImageViewer;
import unittest.gui.imageViewer.SWTImageCanvas;
import org.eclipse.swt.widgets.Button;
import unittest.gui.helper.FileCTabItem;
import unittest.gui.helper.FileTabManager;
import unittest.gui.helper.WidgetFactory;
import unittest.module.seperate.TestModule;
import unittest.util.AnalysisFile;
import unittest.util.Config;
import unittest.util.RecordToLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.DoubleClickEvent;

public class UATRegrssionTestingGUI {

	public UATRegrssionTestingGUI(UATGUI demo) {
		super();
		this.shell = shell;
		this.demo = demo;
	}

	
	
	
	
	public UATRegrssionTestingGUI() {
		super();
		// TODO Auto-generated constructor stub
	}




	private static Logger logger = Logger.getLogger(UATRegrssionTestingGUI.class);
	protected Shell shell;
	private UATGUI demo;
	private FileCTabItem myFileCTabItem;
	
	
	
	/**
	 * @wbp.nonvisual location=122,262
	 */
	private final FileCTabItem fileCTabItem = new FileCTabItem((File) null);
	private Table table;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			UATRegrssionTestingGUI window = new UATRegrssionTestingGUI();
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
		shell.setSize(1225, 850);
		shell.setText("SWT Application");
		
		Group group = new Group(shell, SWT.NONE);
		group.setText("\u539F\u6587\u4EF6");
		group.setBounds(10, 10, 546, 445);
		
		final CTabFolder tabFolder = new CTabFolder(group, SWT.BORDER);

		tabFolder.setBounds(11, 24, 527, 412);
		tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		
		CTabItem tbtmcfg = new CTabItem(tabFolder, SWT.NONE);
		tbtmcfg.setText("\u539FCFG");
		
		final SWTImageCanvas imageCanvas = new SWTImageCanvas(tabFolder);
		tbtmcfg.setControl(imageCanvas);
		
		CTabItem tabItem = new CTabItem(tabFolder, SWT.NONE);
		tabItem.setText("\u539F\u4EE3\u7801");
		
		Group group_1 = new Group(shell, SWT.NONE);
		group_1.setBounds(649, 10, 531, 445);
		group_1.setText("\u4FEE\u6539\u540E\u6587\u4EF6");
		
		Button btnNewButton = new Button(shell, SWT.NONE);
		//add by qgn 给item项目添加事件，以显示源代码文件
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				File myfile = new File(demo.getCurrentFile().getFile());
				myFileCTabItem = new FileCTabItem(myfile, tabFolder, demo);
				//FileTabManager.ShowFile(myfile, demo, !demo.getCurrentFile().isConsoleAltered());
				demo.setCodeContents(myfile, myFileCTabItem.getSourceViewer(), !demo.getCurrentFile().isConsoleAltered());
				
			}
		});
		//end  by qgn
		btnNewButton.setBounds(562, 269, 72, 22);
		btnNewButton.setText("New Button");
		
		Button btnNewButton_1 = new Button(shell, SWT.NONE);
		//add by qgn 显示原CFG
		btnNewButton_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				final TestModule currentFunc = demo.getCurrentFunc();
				final AnalysisFile currentFile = demo.getCurrentFile();
				try
				{


					if(currentFunc == null)
						return;

					new Thread()
					{
						public void run()
						{
							try 
							{
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
				
			}
		});
		//end  by qgn
		btnNewButton_1.setBounds(562, 361, 72, 22);
		btnNewButton_1.setText("\u663E\u793A\u539FCFG");
		
		Group group_2 = new Group(shell, SWT.NONE);
		group_2.setText("\u5BF9\u5E94\u6E90\u4EE3\u7801");
		group_2.setBounds(10, 487, 546, 306);
		
		final TableViewer tableViewer = new TableViewer(group_2, SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent arg0) {
				  
				
				javax.swing.JOptionPane.showMessageDialog(null,table.getItem(table.getSelectionIndex()).getText());
				
				
				
			}
		});
		table = tableViewer.getTable();
		table.setBounds(10, 23, 526, 273);
		
		final TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnNewColumn = tableViewerColumn.getColumn();
		tblclmnNewColumn.setWidth(100);
		tblclmnNewColumn.setText("\u5E8F\u53F7");
		
		TableViewerColumn tableViewerColumn_1 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnNewColumn_1 = tableViewerColumn_1.getColumn();
		tblclmnNewColumn_1.setWidth(100);
		tblclmnNewColumn_1.setText("\u8282\u70B9\u540D");
		
		
		TableViewerColumn tableViewerColumn_2 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnNewColumn_2 = tableViewerColumn_2.getColumn();
		tblclmnNewColumn_2.setWidth(2000);
		tblclmnNewColumn_2.setText("\u6E90\u4EE3\u7801");
		table.setHeaderVisible(true);//要加这句才能显示
		
		Button button = new Button(shell, SWT.NONE);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				final TestModule currentFunc = demo.getCurrentFunc();
				final AnalysisFile currentFile = demo.getCurrentFile();
				Graph graph = currentFunc.getGraph();				
				
				ArrayList<VexNode> listnodes=new ArrayList<VexNode>();
				listnodes.addAll(graph.nodes.values());
				Collections.sort(listnodes);
				 
				for (VexNode tmpnode:listnodes) {
 		
					TableItem item = new TableItem(table, SWT.NONE);
					 
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
		button.setBounds(562, 587, 98, 30);
		button.setText("\u663E\u793A\u5BF9\u5E94\u5173\u7CFB");

	}
}
