package unittest.gui;

import java.util.ArrayList;

import org.eclipse.swt.widgets.TreeItem;

import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType;

/**
 * @author Cai Min
 *
 */
public class UATTestCaseTreeItem extends TreeItem{

	private CType ctype;
	private UATTestCaseTreeItem parentItem;
	private ArrayList<UATTestCaseTreeItem> testCaseTreeItems;
	private boolean unlimited;
	private VariableNameDeclaration vnd;
	
	public UATTestCaseTreeItem(UATTestCaseTree parent, int style, CType _ctype, VariableNameDeclaration _vnd) {
		super(parent, style);
		this.ctype = _ctype;
		this.vnd = _vnd;
		testCaseTreeItems = new ArrayList<UATTestCaseTreeItem>();
		parent.addTestCaseTreeItems(this);
		unlimited = false;
	}
	
	public UATTestCaseTreeItem(UATTestCaseTreeItem parent, int style, CType _ctype, VariableNameDeclaration _vnd) {
		super(parent, style);
		this.ctype = _ctype;
		this.vnd = _vnd;
		parentItem = parent;
		testCaseTreeItems = new ArrayList<UATTestCaseTreeItem>();
		parent.addTestCaseTreeItems(this);
		unlimited = false;
	}
	
	public UATTestCaseTreeItem getParentItem(){
		return parentItem;
	}
	
	@Override
    protected void checkSubclass() {
        // TODO Auto-generated method stub
    
    }
	
	private void addTestCaseTreeItems(UATTestCaseTreeItem item){
		testCaseTreeItems.add(item);
	}
	
	public UATTestCaseTreeItem[] getItems(){
		return testCaseTreeItems.toArray(new UATTestCaseTreeItem[testCaseTreeItems.size()]);
	}
	
	public UATTestCaseTreeItem getItem(int index) {
		return testCaseTreeItems.toArray(new UATTestCaseTreeItem[testCaseTreeItems.size()])[index];
	}
	
	public void setCType(CType _ctype) {
		this.ctype = _ctype;
	}
	
	public CType getCType(){
		return ctype;
	}
	
	public VariableNameDeclaration getVND(){
		return this.vnd;
	}
	
	public void setUnlimited(boolean flag) {
		unlimited = flag;
	}
	
	public boolean isUnlimited() {
		return unlimited;
	}
	
}


