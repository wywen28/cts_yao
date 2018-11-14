package unittest.gui;

import java.awt.RadialGradientPaint;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import bsh.org.objectweb.asm.Label;
import unittest.gui.helper.BlockColorElement;
import unittest.gui.helper.BooleanExpColorElement;
import unittest.gui.helper.COVERTAG;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.module.seperate.*;
import unittest.util.AnalysisFile;

/*
 * edit by zhouao
 * 2011年9月21日
 * 修改shell创建方式，使其可以最大化
 */
public class UATCoverageWindow 
{
	private SourceViewer sourceViewer;
	private StyledText codeStyledText;
	private Shell shell;
	private Display display;
	private SashForm sashForm;
	private Composite topComposite;
	private Composite bottomComposite;

	
	private Vector<StyleRange> range;
	private StyleRange[] styleRanges;
	
	private int start = 0;
	
	public UATCoverageWindow()
	{
		init();
	}
	
	public UATCoverageWindow(AnalysisFile f) throws IOException
	{
		init();
		boolean first = true;
		for(TestModule tm: f.getFunctionList())
		{
			if(first)
			{
				styleRanges = initCoveredRange(tm);
			}else
			{
				StyleRange[] tmp = initCoveredRange(tm);
				int len1 = styleRanges.length;
				int len2 = tmp.length;
				int len =  len1 + len2;
				StyleRange temp[] = new StyleRange[len];
				System.arraycopy(tmp, 0, temp, 0, len2);
				System.arraycopy(styleRanges,0,temp,len2,len1);
				styleRanges = new  StyleRange[len];
				System.arraycopy(temp, 0, styleRanges, 0, len);
			}
			 
		}
		setDocumentFile(f.getConsoleAlteredFile());
		if(styleRanges != null)
		{
			boolean error = true;
			while(error)
			{
				try
				{
					codeStyledText.setStyleRanges(styleRanges);
					error = false;
				}catch(Exception e)
				{
					error = true;
					int size = styleRanges.length;
					StyleRange[] newStyleRanges = new StyleRange[size-1];
					for(int i = 0;i < size-1;i++)
						newStyleRanges[i] = styleRanges[i];
					
					styleRanges = new  StyleRange[size-1];
					for(int i = 0;i < size-1;i++)
						styleRanges[i] = newStyleRanges[i];
				}
			}
		}
		
		shell.setText("文件 " + f.getFile() +" 代码覆盖情况");
	}
	
	public UATCoverageWindow(TestModule tm, long testCaseID) throws IOException
	{
		try
		{
			init();
			start = tm.getBelongToFile().getSreamByLine(tm.getFuncRoot().getBeginLine()-1);
			styleRanges = initCoveredRange(tm, testCaseID);
			
			setDocumentFile(tm.getFileName());
			
			int end = tm.getBelongToFile().getSreamByLine(tm.getFuncRoot().getEndLine()-1);
			int length = end - start +1;
			sourceViewer.setVisibleRegion(start, length);
			boolean error = true;
			while(error)
			{
				try
				{
					codeStyledText.setStyleRanges(styleRanges);
					error = false;
				}catch(Exception e)
				{
					error = true;
					int size = styleRanges.length;
					StyleRange[] newStyleRanges = new StyleRange[size-1];
					for(int i = 0;i < size-1;i++)
						newStyleRanges[i] = styleRanges[i];
					
					styleRanges = new  StyleRange[size-1];
					for(int i = 0;i < size-1;i++)
						styleRanges[i] = newStyleRanges[i];
				}
			}
		}catch(Exception e)
		{
			MessageBox box= WidgetFactory.createErrorMessageBox(shell, "覆盖的代码时出异常","文件中有宏展开，导致字符流对应不上" + e.getMessage());
			box.open();
			//e.printStackTrace();
			
			codeStyledText.setStyleRanges(styleRanges);
			System.out.println("发生异常 " + e.getMessage());
		}
		shell.setText("函数 " + tm.getFuncName() +" 代码覆盖情况");
	}
	
	public UATCoverageWindow(TestModule tm) throws IOException
	{
		try
		{
			init();
			start = tm.getBelongToFile().getSreamByLine(tm.getFuncRoot().getBeginLine()-1);
			styleRanges = initCoveredRange(tm);
			
			setDocumentFile(tm.getFileName());
			
			int end = tm.getBelongToFile().getSreamByLine(tm.getFuncRoot().getEndLine()-1);
			int length = end - start +1;
			sourceViewer.setVisibleRegion(start, length);
			boolean error = true;
			while(error)
			{
				try
				{
					codeStyledText.setStyleRanges(styleRanges);
					error = false;
				}catch(Exception e)
				{
					error = true;
					int size = styleRanges.length;
					StyleRange[] newStyleRanges = new StyleRange[size-1];
					for(int i = 0;i < size-1;i++)
						newStyleRanges[i] = styleRanges[i];
					
					styleRanges = new  StyleRange[size-1];
					for(int i = 0;i < size-1;i++)
						styleRanges[i] = newStyleRanges[i];
				}
			}
		}catch(Exception e)
		{
			MessageBox box= WidgetFactory.createErrorMessageBox(shell, "覆盖的代码时出异常","文件中有宏展开，导致字符流对应不上" + e.getMessage());
			box.open();
			//e.printStackTrace();
			
			codeStyledText.setStyleRanges(styleRanges);
			System.out.println("发生异常 " + e.getMessage());
		}
		String coverRule ="";
//		CoverSet cset = tm.getCoverSet();
//		if(cset != null)
//		{
//			if(cset instanceof BlockCoverSet )
//				coverRule ="语句覆盖";
//			else if(cset instanceof MCDCCSET)
//				coverRule ="MC/DC覆盖";
//			else
//				coverRule = "分支覆盖";
//		}
		shell.setText("函数 " + tm.getFuncName() + coverRule +" 代码覆盖情况");
	}
	private void setDocumentFile(String fileName) throws IOException 
	{
		File file = new File(fileName);
		FileInputStream  fin;
		
		fin = new FileInputStream( file );
		
		int ch;
		StringBuffer data = new StringBuffer();
		while( ( ch = fin.read() ) != -1 ) {
			data.append( ( char )ch );
		} 
		if( data != null ) 
		{
			String contents = new String( data.toString().getBytes( "ISO-8859-1" ), "GBK"   );
			sourceViewer.setDocument(new Document(contents));
		}
		fin.close();
		//codeStyledText.setStyleRanges(styleRanges);
		
	}

	private void init()
	{
		range = new Vector<StyleRange>();
		display = Display.getDefault();
		this.createShell();
	}
	
	private StyleRange[] initCoveredRange(TestModule tm, long testCaseID) throws IOException 
	{
		System.out.println(new Throwable().getStackTrace()[0].getMethodName()+"::此处应获取单个测试用例的覆盖区域！");
		return null;
	}
	
	private StyleRange[] initCoveredRange(TestModule tm) throws IOException 
	{
		int all =0;
		Set<BlockColorElement> blocks = tm.getBlock();
		if(blocks != null)
		{
			for(BlockColorElement block :blocks)
			{ 
				StyleRange sr = new StyleRange();
				sr.background = Resource.LineCoverColor;
				//tm.getBelongToFile().getSreamByLine(tm.getFuncRoot().getBeginLine()-1), tm.getBelongToFile().getSreamByLine(tm.getFuncRoot().getEndLine()-1));
				int begin = tm.getBelongToFile().getSreamByLine(block.getBeginLine()-1); //+ block.getBeginCol();
				int end = tm.getBelongToFile().getSreamByLine(block.getEndLine());// + block.getEndCol();
				sr.start = begin -1 - this.start;
				sr.length = end - begin +1;
				
				all++;
				range.add(sr);
			}
		}
		
		Map<BooleanExpColorElement, COVERTAG> booleanExp = tm.getBooleanExp();
		if(booleanExp != null)
		{
			Set<BooleanExpColorElement> keySet = booleanExp.keySet();
			for(BooleanExpColorElement key:keySet)
			{
				StyleRange sr = new StyleRange();
				COVERTAG tag = booleanExp.get(key) ;
				if( tag == COVERTAG.ALL)
					sr.background = Resource.AllCoverColor;
				else if(tag == COVERTAG.TRUE)
					sr.background = Resource.TrueCoverColor;
				else 
					sr.background = Resource.FalseCoverColor;

				int begin = tm.getBelongToFile().getSreamByLine(key.getBeginLine()-1) + key.getBeginCol();
				int end = tm.getBelongToFile().getSreamByLine(key.getEndLine()-1) + key.getEndCol();
				sr.start = begin - 1 - this.start;
				sr.length = end - begin +1;
				all++;
				
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
			
			}//宏替换导致endCol和beginColoum不对
			else if(preSR.start < curSR.start && preSR.start + preSR.length >= curSR.length + curSR.start)
			{
				//删除前面那个
				rangeTmp.remove(preSR);
				rangeTmp.add(curSR);
			}
			else if(preSR.start < curSR.start && preSR.start + preSR.length > curSR.start)
			{
				rangeTmp.remove(preSR);
				preSR.length = curSR.start - preSR.start;//+ curSR.length - preSR.start;
				rangeTmp.add(preSR);
				rangeTmp.add(curSR);
			}
			else
				rangeTmp.add(curSR);
				
		}
		
		StyleRange[] srs = new StyleRange[rangeTmp.size()];
		int i=0;
		for(StyleRange sr : rangeTmp)
		{
			srs[i++] = sr;
//			System.out.println("start -->" + sr.start +" length -->" + sr.length );
		}
		
		return srs;

		
		
	}

	public void go()
	{
		
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
		
	}


	private void createShell() 
	{
		//shell = new Shell( SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.CLOSE );
		shell = new Shell(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL | SWT.MAX | SWT.MIN);
		shell.setLayout(new FillLayout());
		
		shell.setText( "牛" );
		
		//add by chenruolin
		sashForm = new SashForm(shell, SWT.VERTICAL);
		sashForm.setLayout(new FillLayout());
		
		createTopComposite();
		createBottomComposite();
		sashForm.setWeights(new int[]{90, 10});
	}
	private void createTopComposite(){
		topComposite = new Composite(sashForm, SWT.None);
		topComposite.setLayout( new FillLayout());
		sourceViewer = WidgetFactory.createSourceViewer(topComposite, SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
		codeStyledText = sourceViewer.getTextWidget();

		sourceViewer.setEditable(false);
	
		//sourceViewer.setDocument( new Document("#include <stdio.h>\nint main()\n{\n     return 0;\n}\n"));
		
		codeStyledText.setFont( new Font( Display.getDefault(), "Courier New", 12, SWT.NONE  ) );
	}
	
	private void createBottomComposite() {
		bottomComposite = new Composite(sashForm, SWT.NONE);
		//bottomComposite.setBackground();
		RowLayout rowLayout = new RowLayout();
		//rowLayout.marginWidth = 20;
		//rowLayout.marginHeight = 20;
		rowLayout.spacing = 20;
		rowLayout.justify = true;
		bottomComposite.setLayout(rowLayout);
		
		Composite composite1 = new Composite(bottomComposite, SWT.NONE);
		//rowLayout.marginWidth = 10;
		//rowLayout.marginHeight = 10;
		rowLayout.spacing = 10;
		rowLayout.justify = true;
		rowLayout.center = true;
		composite1.setLayout(rowLayout);
		CLabel CoverCLabel = new CLabel(composite1, SWT.NONE );
		CoverCLabel.setText("    ");
		CoverCLabel.setBackground(new Color( null, 201, 181, 232 ));
		CoverCLabel = new CLabel(composite1, SWT.NONE );
		CoverCLabel.setText("语句覆盖");
		composite1.pack();
		
		composite1 = new Composite(bottomComposite, SWT.NONE);
		composite1.setLayout(rowLayout);
		CoverCLabel = new CLabel(composite1, SWT.NONE );
		CoverCLabel.setText("    ");
		CoverCLabel.setBackground(new Color(null,218,253,167));
		CoverCLabel = new CLabel(composite1, SWT.NONE );
		CoverCLabel.setText("真分支覆盖");
		composite1.pack();
		
		composite1 = new Composite(bottomComposite, SWT.None);
		composite1.setLayout(rowLayout);
		CoverCLabel = new CLabel(composite1, SWT.NONE );
		CoverCLabel.setText("    ");
		CoverCLabel.setBackground(new Color(null,255,162,161));
		CoverCLabel = new CLabel(composite1, SWT.NONE );
		CoverCLabel.setText("假分支覆盖");
		composite1.pack();
		
		composite1 = new Composite(bottomComposite, SWT.None);
		composite1.setLayout(rowLayout);
		CoverCLabel = new CLabel(composite1, SWT.NONE );
		CoverCLabel.setText("    ");
		CoverCLabel.setBackground(new Color(null,158,234,255));
		CoverCLabel = new CLabel(composite1, SWT.NONE );
		CoverCLabel.setText("真假分支均覆盖");
		composite1.pack();
		
		bottomComposite.pack();
	}
	
	public static void main(String args[])
	{
		UATCoverageWindow coverWin = new UATCoverageWindow();
		coverWin.go();
	}
	/**
	 * This is the ShellListener of UATCoverageWindow.
	 * @author joaquin(孙华衿)
	 *
	 */
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
	class ShellCloseListener extends ShellAdapter 
	{
		private UATCoverageWindow demo;
		public ShellCloseListener( UATCoverageWindow demo ) 
		{
			this.demo = demo;
		} 
		
		public void shellClosed( ShellEvent e ) 
		{
			demo.shell.dispose();
			//demo.display.dispose();
		}
		
	}

}
