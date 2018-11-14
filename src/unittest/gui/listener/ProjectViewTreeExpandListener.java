package unittest.gui.listener;

import java.io.File;

import org.eclipse.swt.events.TreeAdapter;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.widgets.TreeItem;

import unittest.gui.UATGUI;
import unittest.util.AnalysisFile;
import unittest.util.Config;

public class ProjectViewTreeExpandListener extends TreeAdapter
{
	private UATGUI demo;
	public ProjectViewTreeExpandListener( UATGUI uatGui ) 
	{
		this.demo = uatGui;
	}
	public void treeExpanded(TreeEvent e)
	{
		demo.setBugLinkMenuItemEnabled(false);
		demo.setBugLinkToolItemEnabled(false);
		demo.setSelectedTestCaseItemEnabled(false);
		TreeItem root = (TreeItem) e.item;
		if(root != null)
		{
			// The Selected TreeItem is a concrete file.
			//if( root.getParentItem()!= null)// added by cai min, 2011/5/23,防止下一个if报空指针错误
			//if( root.getParentItem().getParentItem()==null ) 
			//{			
				String name =  root.getText() ;		
				if(name.endsWith(".c") || name.endsWith(".C") || name.endsWith(".cc")) {
				//String allPathFileName = (String)root.getData();	
//					String allPathFileName = demo.getCurrentProject().getSourceCodePathString() + "\\" + getFullPathName(root);
					String allPathFileName = demo.getCurrentProject().getSourceCodePathString() + File.separator + getFullPathName(root);
					if(Config.os.equals("windows")){
						allPathFileName = allPathFileName.replaceAll("\\\\+","\\\\");
						
					}
					else{
						allPathFileName = allPathFileName.replaceAll("//+", File.separator);
					}
					
//				for( int i = 0; i <demo.getCurrentProject().getFileList().size(); i++ ) 
//				{
//					AnalysisFile f = demo.getCurrentProject().getFileList().get( i );					
//					if(f.getFile().equals(allPathFileName))
//					{
//						f.setExpand(true);
//						break;
//					}
//				}
				for(int i=0;i<demo.getCurrentProject().getFilenameList().size();i++){
					String filename = demo.getCurrentProject().getFilenameList().get(i);
					if(filename.equals(allPathFileName)){
						demo.getCurrentProject().getIsExpand().set(i, true);
						break;
					}
				}
			}
		}
	}
	
	public void treeCollapsed(TreeEvent e)
	{
		demo.setBugLinkMenuItemEnabled(false);
		demo.setSelectedTestCaseItemEnabled(false);
		demo.setBugLinkToolItemEnabled(false);
		TreeItem root = (TreeItem) e.item;
		if(root != null)
		{
			// The Selected TreeItem is a concrete file.
			//if(root.getParentItem() != null)// added by cai min, 2011/5/23,防止下一个if报空指针错误
			//if( root.getParentItem().getParentItem()==null ) 
			//{			
				String name =  root.getText() ;	
				if(name.endsWith(".c") || name.endsWith(".C") || name.endsWith(".cc")) {
					//String allPathFileName = (String)root.getData();	
					String allPathFileName = demo.getCurrentProject().getSourceCodePathString() + File.separator + getFullPathName(root);
					if(Config.os.equals("windows")){
						allPathFileName = allPathFileName.replaceAll("\\\\+","\\\\");
						
					}
					else{
						allPathFileName = allPathFileName.replaceAll("//+", File.separator);
					}
				//String allPathFileName = (String)root.getData();			
				
//				for( int i = 0; i <demo.getCurrentProject().getFileList().size(); i++ ) 
//				{
//					AnalysisFile f = demo.getCurrentProject().getFileList().get( i );					
//					if(f.getFile().equals(allPathFileName))
//					{
//						f.setExpand(false);
//						break;
//					}
//				}//by xujiaoxian 2012-11-26
				for(int i=0;i<demo.getCurrentProject().getFilenameList().size();i++){
					String filename = demo.getCurrentProject().getFilenameList().get(i);
					if(filename.equals(allPathFileName)){
						demo.getCurrentProject().getIsExpand().set(i, false);
						break;
					}
				}
			}
		}
	}
	
	private String getFullPathName(TreeItem item) {
		String res = "";//demo.getCurrentProject().getSourceCodePathString();
		//item = item.getParentItem();
		while(item.getParentItem() != null) {
//			res = item.getText() + "\\" + res;
			res = item.getText() + File.separator + res;
			item = item.getParentItem();
		}
//		if(res.endsWith("\\"))
		if(res.endsWith(File.separator))
			res = res.substring(0, res.length() - 1);
		return res;
	}

}
