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
		
		//��ȡ��ǰѡ�������ĸ���·��
		TestCaseNew oneTestCaseFromDB = TestCaseLibManagerNew.showOneTestCase(demo.getCurrentFunc(),demo.getCurrentTestCaseID());
		ArrayList<Integer> listnodes = new ArrayList(oneTestCaseFromDB.getPath());
		Collections.sort(listnodes);
				
		ArrayList<VexNode> onePath = TransferIntToPathName(listnodes);
		DrawTestCaseMapToPathGraph(onePath);//���Ƶ�ǰ�������ǵĹ켣ͼ
		
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
		shell.setText("����"+id+"�Ŀ�����ͼ�켣");
		shell.setLayout(new FillLayout());
		
		Composite composite = WidgetFactory.createComposite(shell, SWT.BORDER);
		composite.setLayout(new FillLayout());
		
		final SWTImageCanvas imageCanvas = new SWTImageCanvas(composite);
		imageCanvas.setLayout(new FillLayout());
		
		//������ʾ
		imageCanvas.loadImage(FullPicFilename);
		imageCanvas.showOriginal();
	}
	
	
	/**
	 * ��·��intֵ���ҳ������Ľڵ�����������HashSet<String>��
	 */
	public ArrayList<VexNode> TransferIntToPathName(ArrayList<Integer> mytmplist) {

		TestModule currentFunc = demo.getCurrentFunc();
		Graph mygraph = currentFunc.getGraph();

		// ���нڵ�
		ArrayList<VexNode> listnodes = new ArrayList<VexNode>();
		listnodes.addAll(mygraph.nodes.values());

		// ·���Ͻڵ�
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
	 * ��ͼ
	 */
	public void DrawTestCaseMapToPathGraph(ArrayList<VexNode> nodesOnPath) {
		//Step	1   ·��������
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
			
		//Step	2 ����ͼ
		currentFunc.getGraph().accept(new TestCaseMapToPathGraphVisitor(nodesOnPath), FullFilename + ".dot");
		
		//Step	3 ����ͼƬ
		try {
			java.lang.Runtime.getRuntime().exec("dot -Tjpg -o " + FullFilename + ".jpg " + FullFilename + ".dot").waitFor();
		} catch (IOException e1) {
			System.out.println(e1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		System.out.println("������ͼ��ӡ�����ļ�" + FullFilename + ".jpg");
	}
}
