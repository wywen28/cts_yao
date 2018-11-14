package unittest.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import softtest.cfg.c.Graph;
import softtest.cfg.c.VexNode;
import unittest.gui.helper.WidgetFactory;
import unittest.gui.imageViewer.SWTImageCanvas;
import unittest.module.seperate.TestModule;
import unittest.regressiontest.TestCaseMapToPathGraphVisitor;
import unittest.testcase.generate.util.TestCaseNew;
import unittest.testcase.management.news.TestCaseLibManagerNew;

public class UATOneTestCasesToPathGUI {
	private UATGUI demo;
	protected Shell shell;
	public String FullPicFilename = "";
	Long id = 0L;
	public UATOneTestCasesToPathGUI(UATGUI demo2, Long testCaseID) {
		super();		
		this.demo = demo2;
		id = testCaseID;
	}
	
	public void open() {
		Display display = Display.getDefault();
		
		//获取当前选中用例的覆盖路径
		TestCaseNew oneTestCaseFromDB = TestCaseLibManagerNew.showOneTestCase(demo.getCurrentFunc(),demo.getCurrentTestCaseID());
		ArrayList<Integer> listnodes = new ArrayList(oneTestCaseFromDB.getPath());
		Collections.sort(listnodes);
				
		ArrayList<VexNode> onePath = TransferIntToPathName(listnodes);
		DrawTestCaseMapToPathGraph(onePath);//绘制当前用例覆盖的轨迹图
		
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
	protected void createContents() {
		shell = new Shell();
		shell.setBounds(200, 30, 800, 600);
		shell.setText("用例"+id+"的控制流图轨迹");
		shell.setLayout(new FillLayout());
		
		Composite composite = WidgetFactory.createComposite(shell, SWT.BORDER);
		composite.setLayout(new FillLayout());
		
		final SWTImageCanvas imageCanvas = new SWTImageCanvas(composite);
		imageCanvas.setLayout(new FillLayout());
		
		//界面显示
		imageCanvas.loadImage(FullPicFilename);
		imageCanvas.showOriginal();
	}
	
	
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
}
