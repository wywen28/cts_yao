package unittest.gui.listener;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.TreeItem;

import unittest.gui.UATGUI;
import unittest.module.seperate.*;
import unittest.util.AnalysisFile;
import unittest.util.Config;

public class ProjectViewTreeSelectionListener  extends SelectionAdapter
{
	private UATGUI demo;
	public ProjectViewTreeSelectionListener( UATGUI demo ) 
	{
		this.demo = demo;
	}
	public void widgetSelected( SelectionEvent e ) 
	{
		TreeItem[] items1 = demo.projectViewTree.getSelection();
		if(items1 !=null && items1.length > 0)
		{
			TreeItem selected = items1[0];
			//取节点控件
			if(selected!=null )
			{
				// The Selected TreeItem is the root TreeItem of Project View.
				if( selected.getParentItem() == null ) 
				{
					demo.setCurrentFile(null);
					demo.setCurrentFunc(null);
				}
				// The Selected TreeItem is a concrete file.
				else if(selected.getText().endsWith(".c") || selected.getText().endsWith(".cc") || selected.getText().endsWith(".C")) 
				{
					String fullPathFileName = demo.getCurrentProject().getSourceCodePathString()+File.separator+getFullPathName(selected);
					if(Config.os.equals("windows"))
						fullPathFileName = fullPathFileName.replaceAll("\\\\+", "\\\\");
					else
						fullPathFileName = fullPathFileName.replaceAll("//+", File.separator);
					AnalysisFile af = demo.getCurrentProject().getAnalysisFile(fullPathFileName);
					demo.setCurrentFile(af);
					demo.setCurrentFunc(null);
				}
				// The Selected TreeItem is a function.
				else if(selected.getItemCount() == 0 && (selected.getParentItem().getText().endsWith(".c") || selected.getParentItem().getText().endsWith(".C") || selected.getParentItem().getText().endsWith(".cc")))
				{
					String allPathFileName =  demo.getCurrentProject().getSourceCodePathString() + File.separator + getFullPathName(selected.getParentItem());
					String funcName = selected.getText();
					AnalysisFile file = demo.getCurrentProject().getAnalysisFile(allPathFileName);
					if(file!=null){
						demo.setCurrentFile(file);
						for( int j = 0; j < file.getFunctionList().size(); j++ ) 
						{
							TestModule tm = file.getFunctionList().get( j );
							
							if( tm.getFuncName().startsWith(funcName) ) 
							{
								demo.setCurrentFunc(tm);
								break;
							}
						}
					}
					else{
						demo.setCurrentFile(null);
						demo.setCurrentFunc(null);
					}
					
				}
			}
			else 
			{
				demo.setCurrentFile(null);
				demo.setCurrentFunc(null);

			}
		}
		demo.doMeauToolBarRefresh();
		demo.actionsGUI.doCoverageInfoRefresh();
	}
	
	private String getFullPathName(TreeItem item) {
		String res = "";
		while(item.getParentItem() != null) {
			res = item.getText() + File.separator + res;
			item = item.getParentItem();
		}
		if(res.endsWith(File.separator))
			res = res.substring(0, res.length() - 1);
		return res;
	}
	
}
