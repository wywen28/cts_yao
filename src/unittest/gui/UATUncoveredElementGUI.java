package unittest.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.tools.ant.types.Path;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTIterationStatement;
import softtest.ast.c.ASTSelectionStatement;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.module.seperate.TestModule;
import unittest.pathchoose.util.path.OnePath;
import unittest.util.AnalysisFile;
import unittest.util.RecordToLogger;

/**
 * @author Cai Min
 * 显示未覆盖的元素
 */
public class UATUncoveredElementGUI {
	static Logger logger = Logger.getLogger(UATUncoveredElementGUI.class);

	private SourceViewer sourceViewer;
	private StyledText codeStyledText;
	private Display display;
	private Composite topComposite;

	private Vector<StyleRange> range;
	private StyleRange[] styleRanges;

	//将未覆盖的语句标红
	public UATUncoveredElementGUI(TestModule tm, Composite comp) throws IOException
	{
		this.topComposite = comp;
		try
		{
			init();
			styleRanges = initCoveredRange(tm);

			setDocumentFile(tm.getFileName());
			boolean error = true;
			while(error)
			{
				try
				{
					codeStyledText.setLineBackground(tm.getFuncRoot().getBeginLine() - 1, tm.getFuncRoot().getEndLine() - tm.getFuncRoot().getBeginLine() + 1, Resource.backgroundColor2);
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
			RecordToLogger.recordExceptionInfo(e, logger);
			String message = "覆盖的代码时出异常 ,文件中有宏展开，导致字符流对应不上" + e.getMessage();
			logger.error(message);
			codeStyledText.setStyleRanges(styleRanges);
			System.out.println("发生异常 " + e.getMessage());
		}
	}
	
	/**
	 * 将未覆盖的路径标红
	 * @param tm - 被测模块
	 * @param comp
	 * @param path - 未覆盖的路径
	 * @throws IOException - 抛出IO异常
	 */
	public UATUncoveredElementGUI(TestModule tm, Composite comp, OnePath path) throws IOException
	{
		this.topComposite = comp;
		try
		{
			init();
			styleRanges = initCoveredRange(tm, path);
			for (int i=0; i<styleRanges.length; i++){
				if (styleRanges[i].background.equals(Resource.InfeasibleColor)){
					while (i < styleRanges.length){
						styleRanges[i].background = Resource.InfeasibleColor;
						i++;
					}
				}
			}

			setDocumentFile(tm.getFileName());
			boolean error = true;
			while(error)
			{
				try
				{
					codeStyledText.setLineBackground(tm.getFuncRoot().getBeginLine() - 1, tm.getFuncRoot().getEndLine() - tm.getFuncRoot().getBeginLine() + 1, Resource.backgroundColor2);
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
			
			StyleRange sr = new StyleRange();
			sr.start = tm.getBelongToFile().getSreamByLine(tm.getFuncRoot().getBeginLine()-1);
			//sr.length = tm.getBelongToFile().getSreamByLine(tm.getFuncRoot().getEndLine()) - sr.start + 1;
			codeStyledText.setSelection(sr.start);
			codeStyledText.showSelection();
		}catch(Exception e)
		{
			RecordToLogger.recordExceptionInfo(e, logger);
			String message = "覆盖的代码时出异常 ,文件中有宏展开，导致字符流对应不上" + e.getMessage();
			logger.error(message);
			if(styleRanges!=null)
			codeStyledText.setStyleRanges(styleRanges);
			System.out.println("发生异常 " + e.getMessage());
		}
	}
	
	public void showInfeasibleLine(TestModule tm, Composite comp, OnePath path) throws IOException
	{
		try
		{
			boolean error = true;
			while(error)
			{
				try
				{
					SimpleNode simpleNode = path.getCondictSimpleNode();
					int begin = simpleNode.getBeginLine();
					int end = simpleNode.getEndLine();
					codeStyledText.setLineBackground(simpleNode.getBeginLine() - 1, simpleNode.getEndLine() - simpleNode.getBeginLine() + 1, Resource.InfeasibleColor);
					codeStyledText.replaceStyleRanges(simpleNode.getBeginLine() - 1, simpleNode.getEndLine() - simpleNode.getBeginLine() + 1, styleRanges);
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
			RecordToLogger.recordExceptionInfo(e, logger);
			String message = "覆盖的代码时出异常 ,文件中有宏展开，导致字符流对应不上" + e.getMessage();
			logger.error(message);
			codeStyledText.setStyleRanges(styleRanges);
			System.out.println("发生异常 " + e.getMessage());
		}
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
			String contents = new String( data.toString().getBytes( "ISO-8859-1" ), "GBK");
			sourceViewer.setDocument(new Document(contents));
		}
		fin.close();
	}

	private void init()
	{
		range = new Vector<StyleRange>();
		display = Display.getDefault();
		this.createShell();
	}

	//设置高亮部分，目前仅支持高亮显示未覆盖的语句
	private StyleRange[] initCoveredRange(TestModule tm) throws IOException 
	{
		Vector<StyleRange> range = new Vector<StyleRange>();

		Collection<VexNode> nodes = tm.getGraph().nodes.values();
		for(VexNode node : nodes)
		{
			if(node.getTreenode()!=null && !node.isCovered()  && !(node.getTreenode() instanceof ASTFunctionDefinition)
					&& !(node.getTreenode() instanceof ASTSelectionStatement) && 
					!(node.getTreenode() instanceof ASTIterationStatement) )
			{
				StyleRange sr = new StyleRange();
				sr.background = Resource.redColor2;
				int begin = tm.getBeginStreamByLine(node.getTreenode().getBeginLine() - 1) + node.getTreenode().getBeginColumn();
				int end = tm.getBeginStreamByLine(node.getTreenode().getEndLine() - 1) + node.getTreenode().getEndColumn();
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
	
	/**
	 * @param tm
	 * @param path2
	 * @return
	 */
	private StyleRange[] initCoveredRange(TestModule tm, OnePath path) {
		
		SimpleNode simpleNode = path.getCondictSimpleNode();
		Vector<StyleRange> range = new Vector<StyleRange>();

		Collection<VexNode> nodes = tm.getGraph().nodes.values();
		for(VexNode node : nodes)
		{
			if(path.getpathnodes().contains(node))
			{
				if(node.getTreenode() instanceof ASTSelectionStatement)
				{
					StyleRange sr = new StyleRange();
					//if (node.getTreenode().getBeginLine() == simpleNode.getBeginLine())
					if (node.getTreenode().equals(simpleNode))
						sr.background = Resource.InfeasibleColor;
					else
						sr.background = Resource.redColor2;
					int begin = tm.getBeginStreamByLine(node.getTreenode().getBeginLine() - 1) + node.getTreenode().getBeginColumn();
					int end = tm.getBeginStreamByLine(node.getTreenode().getBeginLine());
					sr.start = begin-1;
					sr.length = end - begin +1;
					range.add(sr);
				}
				else if(node.getTreenode() instanceof ASTFunctionDefinition)
					continue;
				else if(node.getTreenode() instanceof ASTIterationStatement)
					continue;
				else{
					if(node.getTreenode()!=null){
						StyleRange sr = new StyleRange();
						if (node.getTreenode().equals(simpleNode))
							sr.background = Resource.InfeasibleColor;
						else
							sr.background = Resource.redColor2;
						int begin = tm.getBeginStreamByLine(node.getTreenode().getBeginLine() - 1) + node.getTreenode().getBeginColumn();
						int end = tm.getBeginStreamByLine(node.getTreenode().getEndLine() - 1) + node.getTreenode().getEndColumn();
						sr.start = begin-1;
						sr.length = end - begin +1;
						range.add(sr);
					}
				}
			}
		}

		Comparator ct = new StyleRanleComparator();
		Collections.sort(range,ct);
		Vector<StyleRange> rangeTmp = new Vector<StyleRange>();

		if(range.size() >0)
			rangeTmp.add(range.elementAt(0));
		StyleRange preSR=null;
		for(int i =1;i< range.size();i++)
		{
			StyleRange curSR = range.get(i);
			if(preSR == null)
				preSR = range.get(i-1);
			else{
				if(preSR.start + preSR.length > curSR.start && rangeTmp.contains(preSR)){
					
				}
				else{
					preSR = range.get(i-1);
				}
			}
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

	private void createShell() 
	{
		topComposite.setLayout( new FillLayout());

		sourceViewer = WidgetFactory.createSourceViewer(topComposite, SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
		codeStyledText = sourceViewer.getTextWidget();

		sourceViewer.setEditable(false);
		codeStyledText.setFont( new Font( Display.getDefault(), "Courier New", 12, SWT.NONE  ) );

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

}
