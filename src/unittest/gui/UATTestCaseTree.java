package unittest.gui;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * @author Cai Min
 *
 */
public class UATTestCaseTree extends Tree{

	private ArrayList<UATTestCaseTreeItem> testCaseTreeItems;
	
	public UATTestCaseTree(Composite parent, int style) {
		super(parent, style);
		testCaseTreeItems = new ArrayList<UATTestCaseTreeItem>();
	}
	
	@Override
    protected void checkSubclass() {
        // TODO Auto-generated method stub
    
    }
	
	public void addTestCaseTreeItems(UATTestCaseTreeItem item){
		testCaseTreeItems.add(item);
	}
	
	public UATTestCaseTreeItem[] getItems(){
		UATTestCaseTreeItem[] items = testCaseTreeItems.toArray(new UATTestCaseTreeItem[testCaseTreeItems.size()]);
		ArrayList<UATTestCaseTreeItem> newItems = new ArrayList<UATTestCaseTreeItem>();
		for(UATTestCaseTreeItem item : items)
			if(!item.isDisposed())
				newItems.add(item);
		return newItems.toArray(new UATTestCaseTreeItem[newItems.size()]);
	}
	
	public UATTestCaseTreeItem getItem(Point pt){
		UATTestCaseTreeItem res = (UATTestCaseTreeItem)super.getItem(pt);
		return res;
	}
	
	public UATTestCaseTreeItem[] getSelection(){
		TreeItem[] _res = super.getSelection();
		UATTestCaseTreeItem[] res = new UATTestCaseTreeItem[_res.length];
		for(int i = 0; i < _res.length; i ++)
			res[i] = (UATTestCaseTreeItem)_res[i];
		return res;
	}

}
