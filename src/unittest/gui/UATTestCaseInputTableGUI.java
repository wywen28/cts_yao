package unittest.gui;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeColumn;

import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_Array;
import softtest.symboltable.c.Type.CType_BaseType;
import softtest.symboltable.c.Type.CType_Enum;
import softtest.symboltable.c.Type.CType_Pointer;
import softtest.symboltable.c.Type.CType_Qualified;
import softtest.symboltable.c.Type.CType_Struct;
import softtest.symboltable.c.Type.CType_Typedef;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.testcase.generate.paramtype.AbstractParamValue;
import unittest.testcase.generate.paramtype.ArrayParamValue;
import unittest.testcase.generate.paramtype.EnumParamValue;
import unittest.testcase.generate.paramtype.FunctionParamValue;
import unittest.testcase.generate.paramtype.PointerParamValue;
import unittest.testcase.generate.paramtype.PrimitiveParamValue;
import unittest.testcase.generate.paramtype.StructParamValue;
import unittest.testcase.generate.paramtype.TypeDefParamValue;
import unittest.testcase.generate.util.TestCaseNew;
import unittest.util.ASCIITranslator;
import unittest.util.CLanguageMaxTypeRange;
import unittest.util.Config;
import unittest.util.UserInitiatedMaxRange;

/**
 * @author Cai Min 用于显示人工输入测试用例
 */
public class UATTestCaseInputTableGUI {
	private UATTestCaseTree tableTree;
	private Composite parentComp;
	private Long columnLength = 2L;
	private UATGUI uatGui;
	public int usage = 1; // 一表多用，取值 ---- 1:用于人工输入测试用例 2:用于参数设置的左表 3:用于参数设置的右表
	private boolean tcValidity; // 检查输入值的正确性
	public boolean charToAscii; // 以Ascii码形式输入字符
	//private TestCaseNew nullTC;
	Properties tempProp;
	private String filePath;
	private String fileName;
	//boolean hasSetDomain = false;  //用于判断是否可以从配置文件中读出取值范围
	
	private ArrayList<VariableNameDeclaration> paramList;
	private ArrayList<VariableNameDeclaration> globalList;
	
	private UATTestCaseTreeItem paramItem;
	private UATTestCaseTreeItem globalItem;
	private UATTestCaseTreeItem returnItem;
	
	final Menu treeMenu;
	
	//记录当前处理的vnd,用于判断不定长数组中
	private VariableNameDeclaration currentVnd;

	public UATTestCaseInputTableGUI(Composite composite, UATGUI uatGui) {
		tableTree = new UATTestCaseTree(composite, SWT.FULL_SELECTION | SWT.BORDER);
		tableTree.setHeaderVisible(true);
		tableTree.setLinesVisible(true);
		this.parentComp = composite;
		this.uatGui = uatGui;
		tcValidity = true;
		charToAscii = true;
		
		treeMenu = new Menu(parentComp.getShell(), SWT.POP_UP);
		
		paramList = uatGui.getCurrentFunc().getFuncVar().getParamVar();
		globalList = uatGui.getCurrentFunc().getFuncVar().getGlobalVar();
	}

	public void deleteValueFile(){
		uatGui.getCurrentFile().setHasSetDomain(false);
		File tempFile = new File(filePath+fileName+".properties");
		if (tempFile.exists()){
			tempFile.delete();
		}
	}
	
	public UATTestCaseTree getTableTree() {
		return tableTree;
	}
	
	public void addColumnLength(){
		columnLength++;
	}
	
	public void layout(){
		parentComp.layout();
	}

	public boolean getValidity() {
		return this.tcValidity;
	}

	public void createContents() {
		// 创建列
		TreeColumn column1 = new TreeColumn(tableTree, SWT.CENTER);
		column1.setWidth(160);

		if (usage == 1) {
			TreeColumn column2 = new TreeColumn(tableTree, SWT.CENTER);
			column2.setWidth(120);
			column2.setText("1");
			column1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					TreeColumn column = new TreeColumn(tableTree, SWT.CENTER);
					column.setWidth(120);
					column.setText(Integer.toString(tableTree.getColumns().length-1));
					columnLength++;
					parentComp.layout();
				}
			});
			//column1.setText("单击添加列");
			column1.setImage(Resource.addColumn);
		} else if (usage == 2) {
			TreeColumn column2 = new TreeColumn(tableTree, SWT.CENTER);
			column2.setText(GUILanguageResource.getProperty("valueDomain"));
			column2.setWidth(200);
		} 
		//else if (usage == 3) {
		//	TreeColumn column = new TreeColumn(tableTree, SWT.CENTER);
		//	column.setText("1");
		//	column.setWidth(100);
		//}
		
        boolean isParam = true;
    	UATTestCaseTreeItem parent;
    	paramItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
    	paramItem.setText("参数");
		createItems(paramItem, isParam);
		globalItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
		globalItem.setText("全局变量");
		createItems(globalItem, !isParam);

		if (usage == 1){
			returnItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
	    	returnItem.setText("预期返回值");
			createReturnItem(returnItem);
			addTextEditor(usage);
			setEditableColor(tableTree.getItems());
		}
		
		if (usage == 1) {
			createMenu();
			tableTree.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					if (e.button == 3) {
						// 鼠标单击右键
						tableTree.setMenu(null);

						// 检查是否点到了具体的节点
						UATTestCaseTreeItem treeItem = tableTree
								.getItem(new Point(e.x, e.y));

						// 没有点到具体结点
						if (treeItem != null)
							if (treeItem.isUnlimited())
								tableTree.setMenu(treeMenu);
					}
				}
			});
		}
	}

	/**
	 * @param parent
	 */
	public void createItems(UATTestCaseTreeItem parent, boolean isParam) {
		//List<AbstractParamValue> varList;
		//读入用户设置初始取值区间
		tempProp = new Properties();    	
		FileInputStream inputFile;
		fileName = uatGui.getCurrentFile().getFile();
		//changed by xujiaoxian
		//int startloc = fileName.lastIndexOf("\\");
		int startloc;
		if(Config.os=="windows"){
			startloc = fileName.lastIndexOf("\\");
		}
		else{
			startloc = fileName.lastIndexOf(File.separator);
		}
		//end changed by xujiaoxian
		int endloc = fileName.lastIndexOf(".");
		filePath = uatGui.getCurrentProject().getPath()+File.separator+"ValueConfig"+File.separator;
		fileName = fileName.substring(startloc+1, endloc);
		try {
			inputFile = new FileInputStream(filePath + fileName + ".properties");
            tempProp.load(inputFile);
            inputFile.close();
    		uatGui.getCurrentFile().setHasSetDomain(true);
        } catch (FileNotFoundException ex) {
        	//System.out.println("无取值区间配置文件，按系统默认生成区间");
        } catch (IOException ex) {
            System.out.println("装载文件--->失败");
            ex.printStackTrace();
        }
		/*if(isParam)
			varList = nullTC.getFuncParamList();
		else
			varList = nullTC.getGlobalParamList();
		 for (AbstractParamValue apv : varList){
			 UATTestCaseTreeItem item = new UATTestCaseTreeItem(parent, SWT.NONE, null);
			 parent.setExpanded(true);
			 item.setText(0, getTypeAndName(apv));
			 if (apv.getType() instanceof CType_Enum)
				 item.setText(0, "enum " + getTypeAndName(apv));
			 
			 if(!(apv.getType() instanceof CType_BaseType)) 
			    createChildItem(apv,item, true);
			 else {
				 if (!tempProp.containsKey(item.getText(0)) || !uatGui.getCurrentFile().getHasSetDomain())
					 setDomainValue(item, apv.getType());
				 else{
					 if (usage == 2){
						 String domain = tempProp.getProperty(item.getText(0));
						 item.setText(1, domain);
					 }
				 }
			 }
				 
		 }	*/ 
        ArrayList<VariableNameDeclaration> varList;
        if(isParam)
			varList = paramList;
		else
			varList = globalList;
        for(VariableNameDeclaration vnd : varList){
        	CType ctype = vnd.getType();
        	currentVnd = vnd;
        	UATTestCaseTreeItem item = createChildItem(ctype,parent,vnd);
        	//item.setText(0, vnd.toString());
        }
	}
	
	public void createReturnItem(UATTestCaseTreeItem parent) {
        CType ctype = uatGui.getCurrentFunc().getFuncVar().getReturnType();
        UATTestCaseTreeItem item = createChildItem(ctype,parent,null);
        //item.setText(1, "--");
	}
	
	// 布尔值isChild避免根节点重复建立（在被createItems()调用时不需要建立item，以后被递归调用时都要建立）
	private UATTestCaseTreeItem createChildItem(CType ctype, UATTestCaseTreeItem parent, VariableNameDeclaration vnd) {
		UATTestCaseTreeItem item = new UATTestCaseTreeItem(parent, SWT.NONE, ctype, vnd);
		parent.setExpanded(true);
		if(vnd != null)
			item.setText(0, vnd.toString());
		else
			item.setText(0, ctype.getName());
		
		if (ctype instanceof CType_BaseType)
			setDomainValue(item);
		else if (ctype instanceof CType_Qualified) {
			CType originalType = ((CType_Qualified) ctype).getOriginaltype();
			item.setCType(originalType);
			/*UATTestCaseTreeItem item = new UATTestCaseTreeItem(parent, SWT.NONE, ctype);
			parent.setExpanded(true);
			item.setText(0, ctype.getName());
			if (ctype instanceof CType_Enum)
				 item.setText(0, "enum " + ctype.getName());
			setDomainValue(item, ctype);
			if(apv != null) {
				item.setText(0, getTypeAndName(apv));
				if (apv instanceof EnumParamValue)
					item.setText(0, "enum "+getTypeAndName(apv));
				setDomainValue(item, apv.getType());
			}
			else { 
				item.setText("value");
				return;
			}
			parent = item;*/
			 if (usage == 2){
				 if (!tempProp.containsKey(item.getText(0)) || !uatGui.getCurrentFile().getHasSetDomain())
					 setDomainValue(item);
				 else{
				 String domain = tempProp.getProperty(item.getText(0));
				 item.setText(1, domain);
				 }
			 }
		}

		else if (ctype instanceof CType_Enum && usage==2) {
			CType_Enum enumType = (CType_Enum) ctype;
			ArrayList<Long> enumValue = enumType.getEnumValue();
			Long left = enumValue.get(0);
			Long right = enumValue.get(enumValue.size() - 1);
			String value = "[" + String.valueOf(left) + "," + String.valueOf(right) + "]";
			item.setText(1, value);
		}
		else if (ctype instanceof CType_Array) {
			CType_Array arrayType = (CType_Array) ctype;
			int len = (int) arrayType.getDimSize();
			if (len == -1) {
				len = Config.dimSize4varLenArr;// 防止len没有被初始化
				if(usage == 2) {
					String name = currentVnd.getName();
					UATTestCaseTreeItem tempItem = item.getParentItem();
					CType parentType = tempItem.getCType();
					while(parentType != null && parentType instanceof CType_Array) {
						name += "[]";
						tempItem = tempItem.getParentItem();
						if(tempItem != null)
							parentType = tempItem.getCType();
					}
					UATArrayLenDialog dialog = new UATArrayLenDialog(name);
					len = dialog.getLen();
					arrayType.setDimSize(len);
				}
			}
			CType originalType = arrayType.getOriginaltype();
			for (int i = 0; i < len; i++) {
				UATTestCaseTreeItem _item = createChildItem(originalType, item, null);
				_item.setText(0, _item.getText() + "[" + i + "]");
			}
		} else if (ctype instanceof CType_Pointer) {
			CType_Pointer pointerType = (CType_Pointer)ctype;
			CType originalType = pointerType.getOriginaltype();
			createChildItem(originalType, item, null);
		} else if (ctype instanceof CType_Struct) {
			CType_Struct structType = (CType_Struct)ctype;
			LinkedHashMap<String, CType> fieldType = structType.getfieldType(); 
			if(fieldType.size() == 0 || structType.getMems().size() == 0) {
				item.setUnlimited(true);
				return item;
			}
			for(String key : fieldType.keySet()) {
				CType type = fieldType.get(key);
				UATTestCaseTreeItem _item = createChildItem(type, item, null);
				_item.setText(0, key + " < " + type.getName() + " > ");
			}
		} else if (ctype instanceof CType_Typedef) {
			CType_Typedef typedefType = (CType_Typedef)ctype;
			CType originalType = typedefType.getOriginaltype();
			createChildItem(originalType, item, null);
		}
		return item;
	}
	
	private void addTextEditor(final int usage) {
		final TreeEditor editor = new TreeEditor(tableTree);
		tableTree.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent event) {
				Point pt = new Point(event.x, event.y);
				final UATTestCaseTreeItem item = tableTree.getItem(pt); // 计算出选中的行

				if (item != null && item.getItemCount() == 0 && item.getText() != "预期返回值"
						&& item.getText() != "参数" && item.getText() != "全局变量") {

					// 计算所在列
					int column = -1;
					for (int i = 0; i < tableTree.getColumnCount(); i++) {
						Rectangle rect = item.getBounds(i);
						if (rect.contains(pt)) {
							column = i;
							break;
						}
					}

					// 只有刚运行过的TestCase的预期返回值可编辑
					if (column < 1)
						return;

					boolean showBorder = true;
					final Composite composite = new Composite(tableTree,
							SWT.NONE);
					final Text text = new Text(composite, SWT.NONE | SWT.CENTER);
					final int inset = showBorder ? 1 : 0;
					composite.addListener(SWT.Resize, new Listener() {
						public void handleEvent(Event e) {
							Rectangle rect = composite.getClientArea();
							text.setBounds(rect.x + inset, rect.y + inset,
									rect.width - inset * 2, rect.height - inset
											* 2);
						}
					});

					final int col = column;
					Listener textListener = new Listener() {
						public void handleEvent(final Event e) {
							switch (e.type) {
							case SWT.FocusOut:
								String oldText = item.getText(col);
								//item.setText(col, text.getText());
								finishInput(oldText, SWT.FocusOut);
								if (usage == 2){
									if (!uatGui.getCurrentFile().getHasSetDomain()){
										//新建文件夹
										File file=new File(filePath);
										if (!file.exists())
											file.mkdir();
										//新建文件
										file = new File(filePath+fileName+".properties");
							        	try {
											file.createNewFile();
											FileInputStream inputFile = new FileInputStream(file);
								            tempProp.load(inputFile);
								            inputFile.close();
										} catch (IOException e1) {
											e1.printStackTrace();
										}
									}
									String value = item.getText(1) + "/" + item.getText(2);
									tempProp.setProperty(item.getText(0), value);
									FileOutputStream outputFile;
									try {
										outputFile = new FileOutputStream(filePath+fileName+".properties");
										try {
											tempProp.store(outputFile, null);
											outputFile.close();
										} catch (IOException e1) {
											e1.printStackTrace();
										}
									} catch (FileNotFoundException e1) {
										e1.printStackTrace();
									}
								}
								if (!composite.isDisposed())
									composite.dispose();
								break;
							case SWT.Verify:
								String newText = text.getText();
								String leftText = newText.substring(0, e.start);
								String rightText = newText.substring(e.end,
										newText.length());
								GC gc = new GC(text);
								Point size = gc.textExtent(leftText + e.text
										+ rightText);
								gc.dispose();
								size = text.computeSize(size.x, SWT.DEFAULT);
								Rectangle itemRect = item.getBounds(col),
								rect = tableTree.getClientArea();
								editor.minimumWidth = Math.max(size.x,
										itemRect.width) + inset * 2;
								int left = itemRect.x,
								right = rect.x + rect.width;
								editor.minimumWidth = Math.min(
										editor.minimumWidth, right - left);
								editor.minimumHeight = size.y + inset * 2;
								editor.layout();
								break;
							case SWT.Traverse:
								switch (e.detail) {
								case SWT.TRAVERSE_RETURN:
									oldText = item.getText(col);
									//item.setText(col, text.getText());
									finishInput(oldText, SWT.TRAVERSE_RETURN);
									// FALL THROUGH
								case SWT.TRAVERSE_ESCAPE:
									if (!composite.isDisposed())
										composite.dispose();
									e.doit = false;
								}
								break;
							}
						}

						boolean hasBeenOpened = false;		//若已在回车事件中生成过窗口，则FocusOut事件不再重复生成
						private void finishInput(String oldText, int event) {
							String tempStr = text.getText();
							if(tempStr.equalsIgnoreCase("NULL") && item.getParentItem()!= null
									&& item.getParentItem().getCType() instanceof CType_Pointer) {
								item.setText(col, tempStr);
								return;
							}
							//String itemText = item.getText();
							String cType = item.getCType().getTypeString();
							if (tempStr.equals("") && item.getParentItem().getText() == "预期返回值"){
								item.setText(col, tempStr);
								return;
							}
							if (!tempStr.equals("")){	//按照相应类型进行格式化
								if (cType.contains("float") || cType.contains("double")){
									if (!tempStr.contains("."))
										tempStr = tempStr + ".0";
									else if (tempStr.endsWith("."))
										tempStr = tempStr + "0";
									else
										tempStr = tempStr.substring(0, tempStr.indexOf(".")+2);
								}
								else {
									if (tempStr.contains("."))
										tempStr = tempStr.substring(0, tempStr.indexOf("."));
								}
								if (tempStr.equals("-0") || tempStr.equals("-0.0"))
									tempStr = tempStr.substring(1);
								if (tempStr.startsWith("+"))
									item.setText(col, tempStr.substring(1));
								else
									item.setText(col, tempStr);
							}
							if (!checkValidity(cType, tempStr)){
								
								if (!hasBeenOpened){
									hasBeenOpened = true;
									MessageBox box = WidgetFactory.createInfoMessageBox(
										parentComp.getShell(), "提示", "输入值无效，请重新输入！");
									box.open();
								}
								if (event == SWT.FocusOut){
									hasBeenOpened = false;
									text.selectAll();
									text.setFocus();
								}
								item.setText(col, oldText);
								tcValidity = false;
								return;
							}
							tcValidity = true;
						}
					};
					text.addListener(SWT.FocusOut, textListener);
					text.addListener(SWT.Traverse, textListener);
					text.addListener(SWT.Verify, textListener);
					editor.setEditor(composite, item, col);
					text.setText(item.getText(col));
					text.selectAll();
					text.setFocus();
				}
			}
		});

	}

	public List<TestCaseNew> getTestCaseList() {
		List<TestCaseNew> tcList = new ArrayList<TestCaseNew>();
		TreeColumn[] columns = tableTree.getColumns();
		UATTestCaseTreeItem[] items = tableTree.getItems();

		tcValidity = true;
		
		for (int i = 1; i < columns.length; i++) {
			if(isEmpty(items, i))
				continue;
			TestCaseNew tc = new TestCaseNew();
			try {
			/*
			for(UATTestCaseTreeItem item : items) {
				boolean isParam = true;//第一次是参数,第二次是全局变量
				castToTestCase(tc, item.getItems(), i, isParam);
				isParam = !isParam;
			}
			*/
				//第一次是参数
				boolean isParam = true;
				castToTestCase(tc, items[0].getItems(), i, isParam);
				//第二次是全局变量
				isParam = !isParam;
				castToTestCase(tc, items[1].getItems(), i, isParam);
				//第三次是预期返回值
				castToTestCase(tc, items[2].getItems(), i);
			}
			catch(TestCaseNewConstructionError e) {
				tcValidity = false;
			}
			tcList.add(tc);
		}
		return tcList;
	}
	
	/**
	 * 判断用户是否没有填入数据
	 */
	private boolean isEmpty(UATTestCaseTreeItem[] items, int col) {
		boolean empty = true;
		for(UATTestCaseTreeItem _item : items) {
			if(!_item.getText(col).equals("") || !isEmpty(_item.getItems(),col)) {
				empty = false;
				break;
			}
		}
		return empty;
	}
	
	private void castToTestCase(TestCaseNew tc, UATTestCaseTreeItem[] items, int col, boolean isParam) throws TestCaseNewConstructionError{
		for(UATTestCaseTreeItem item : items) {
			String name = item.getVND().getName();
			AbstractParamValue apv = castToAPV(item, col, name);
			apv.setName(name);
			apv.setVnd(item.getVND());
			apv.setIsExtern(item.getVND().isExtern());
			if(isParam)
				tc.addFunParam(apv);
			else
				tc.addGlobalParam(apv);
		}
	}
	//函数重载，用于保存该测试用例的预期返回值
	private void castToTestCase(TestCaseNew tc, UATTestCaseTreeItem[] items, int col) throws TestCaseNewConstructionError{
		for(UATTestCaseTreeItem item : items) {
			//String name = item.getVND().getName();
			String name = "";
			AbstractParamValue apv = castToAPV(item, col, name);
			apv.setName(name);
			apv.setVnd(item.getVND());
			tc.setExpectReturn(apv);
		}
	}
	
	private AbstractParamValue castToAPV(UATTestCaseTreeItem item, int col, String name) throws TestCaseNewConstructionError{
		try{
		AbstractParamValue apv = null;
		CType ctype = item.getCType();
		String text = item.getText(col);
		if(ctype instanceof CType_BaseType) {
			if(!text.equalsIgnoreCase("NULL")) {
				if(ctype.getName().equals(CType_BaseType.charType.toString())//字符要转化为ascii码存储
						|| ctype.getName().equals(CType_BaseType.uCharType.toString()))
					if(!text.equals(""))
						if(charToAscii)
							apv = new PrimitiveParamValue(null, ctype, String.valueOf((int)text.charAt(0)));
						else
							apv = new PrimitiveParamValue(null, ctype, text);
					else
						apv = new PrimitiveParamValue(null, ctype, text);
				else
					apv = new PrimitiveParamValue(null, ctype, text);
			}
			else
				apv = null;
		}
		else if(ctype instanceof CType_Enum) {
			apv = new EnumParamValue(null, ctype, item.getText(col));
		}
		else if (ctype instanceof CType_Array) {
			Map<Integer,AbstractParamValue> value = new LinkedHashMap<Integer, AbstractParamValue>();
			CType_Array arrayType = (CType_Array) ctype;
			int len = (int) arrayType.getDimSize();
			for(int i = 0; i < len; i ++) {
				String memsName = name + "[" + i + "]";
				AbstractParamValue mem = castToAPV(item.getItems()[i], col, memsName);
				if(mem != null)
					mem.setName(memsName);
				value.put(Integer.valueOf(i), mem);
			}
			apv = new ArrayParamValue(null, ctype, value);
		}
		else if (ctype instanceof CType_Pointer) {
			String memsName = "(*" + name + ")";
			AbstractParamValue value = castToAPV(item.getItem(0), col, memsName);
			if(value != null)
				value.setName(memsName);
			apv = new PointerParamValue(null, ctype, value);
		}
		else if (ctype instanceof CType_Struct) {
			Map<String, AbstractParamValue> value = new LinkedHashMap<String, AbstractParamValue>();
			CType_Struct structType = (CType_Struct)ctype;
			LinkedHashMap<String, CType> fieldType = structType.getfieldType(); 
			int i = 0;
			if(item.getItemCount() == 0) {
				apv = null;
			}
			else{
				if(fieldType.size() == 0)
					fieldType = structType.getCTypeWithMems().getfieldType();
				for(String key : fieldType.keySet()) {
					String memsName = name + "." + key;
					AbstractParamValue mem = castToAPV(item.getItem(i), col, memsName);
					if(mem!= null)
						mem.setName(memsName);
					value.put(memsName, mem);
					i ++;
				}
				apv = new StructParamValue(null, ctype, value);
			}
		}
		else if (ctype instanceof CType_Typedef) {
			AbstractParamValue value = castToAPV(item.getItem(0), col, name);
			if(value != null)
				value.setName(name);
			apv = new TypeDefParamValue(null, ctype, value);
		}
		return apv;
		}
		catch(Exception e) {
			throw new TestCaseNewConstructionError();
		}
	}

	private boolean checkValidity(String cType, String value) {
		if (value == "")
			return true;
		
		try {
			CType_BaseType.getBaseType(cType);
		} catch (RuntimeException e) {
			if (cType.contains("enum"))		//暂时不对enum进行操作
				return true;
			MessageBox box = WidgetFactory.createInfoMessageBox(
					parentComp.getShell(), "ERROR", e.getMessage());
			box.open();
			return false;
		}
		
		if (cType.contains("_Bool")){
			if (value.equals("0") || value.equals("1"))
				return true;
			return false;
		}
		//if (cType.contains("enum"))
		//	return false;
		if (cType.contains("double") || cType.contains("float")) {
			try {
				String tempValue = value;
				if (value.startsWith("+"))
					value = value.substring(1);
				if (tempValue.startsWith("+") || tempValue.startsWith("-"))
					tempValue = tempValue.substring(1);
				for (int i = 0; i < tempValue.length(); i++){
					if (!Character.isDigit(tempValue.charAt(i)) && !(tempValue.charAt(i) == '.'))
						return false;
					else if (tempValue.startsWith("."))
						return false;
				}
				if (!CLanguageMaxTypeRange.isValid(cType,
						Double.parseDouble(value))) {
					return false;
				}
			} catch (NumberFormatException e) {
				return false;
			}
		} else if (cType.contains("int") || cType.contains("long")) {
			try {
				String tempValue = value;
				if (value.startsWith("+"))
					value = value.substring(1);
				if (tempValue.startsWith("+") || tempValue.startsWith("-"))
					tempValue = tempValue.substring(1);
				for (int i = 0; i < tempValue.length(); i++){
					if (!Character.isDigit(tempValue.charAt(i)) && !(tempValue.charAt(i) == '.'))
						return false;
					else if (tempValue.startsWith("."))
						return false;
				}
				if (!CLanguageMaxTypeRange
						.isValid(cType, Long.parseLong(value))) {
					return false;
				}
			} catch (NumberFormatException e) {
				return false;
			}
		} else if (cType.contains("char")) {
			if (charToAscii) { // 以ASCII码形式输入
				String tempValue = value;
				if (value.startsWith("+"))
					value = value.substring(1);
				if (tempValue.startsWith("+") || tempValue.startsWith("-"))
					tempValue = tempValue.substring(1);
				for (int i = 0; i < tempValue.length(); i++){
					if (!Character.isDigit(tempValue.charAt(i)) && !(tempValue.charAt(i) == '.'))
						return false;
					else if (tempValue.startsWith("."))
						return false;
				}
				long longValue;
				try{
					longValue = Long.parseLong(value);
				}
				catch(NumberFormatException e) {
					return false;
				}
				if (!CLanguageMaxTypeRange
						.isValid(cType, longValue)) // 输入的value是数字形式
					return false;
			}
			else {
				if(value.length() > 1)
					return false;
			}
		}
		return true;
	}

	private void setDomainValue(UATTestCaseTreeItem item) {
		CType cType = item.getCType();
		if (usage != 2 || !cType.isBasicType())
			return;
		CType type = CType.getOrignType(cType);
		if (type.equals(CType_BaseType.boolType) || type.equals(CType_BaseType.BoolType)) {
			item.setText(1, "[0,1]");
		} else if (type.equals(CType_BaseType.intType)) {
			item.setText(1, "["+String.valueOf(UserInitiatedMaxRange.INT_MIN)+","+String.valueOf(UserInitiatedMaxRange.INT_MAX)+"]");
		} else if (type.equals(CType_BaseType.charType)) {
			item.setText(1, "["+String.valueOf(UserInitiatedMaxRange.CHAR_MIN)+","+String.valueOf(UserInitiatedMaxRange.CHAR_MAX)+"]");
		} else if (type.equals(CType_BaseType.signedCharType)) {
			item.setText(1, "["+String.valueOf(UserInitiatedMaxRange.SIGNED_CHAR_MIN)+","+String.valueOf(UserInitiatedMaxRange.SIGNED_CHAR_MAX)+"]");
		} else if (type.equals(CType_BaseType.shortType)) {
			item.setText(1, "["+String.valueOf(UserInitiatedMaxRange.SHORT_MIN)+","+String.valueOf(UserInitiatedMaxRange.SHORT_MAX)+"]");
		} else if (type.equals(CType_BaseType.longType)) {
			item.setText(1, "["+String.valueOf(UserInitiatedMaxRange.LONG_MIN)+","+String.valueOf(UserInitiatedMaxRange.LONG_MAX)+"]");
		} else if (type.equals(CType_BaseType.floatType)) {
			item.setText(1, "["+String.valueOf(UserInitiatedMaxRange.FLOAT_MIN)+","+String.valueOf(UserInitiatedMaxRange.FLOAT_MAX)+"]");
		} else if (type.equals(CType_BaseType.doubleType)) {
			item.setText(1, "["+String.valueOf(UserInitiatedMaxRange.DOUBLE_MIN)+","+String.valueOf(UserInitiatedMaxRange.DOUBLE_MAX)+"]");
		} else if (type.equals(CType_BaseType.uIntType)) {
			item.setText(1, "["+String.valueOf(UserInitiatedMaxRange.UNSIGNED_INT_MIN)+","+String.valueOf(UserInitiatedMaxRange.UNSIGNED_INT_MAX)+"]");
		} else if (type.equals(CType_BaseType.uCharType)) {
			item.setText(1, "["+String.valueOf(UserInitiatedMaxRange.UNSIGNED_CHAR_MIN)+","+String.valueOf(UserInitiatedMaxRange.UNSIGNED_CHAR_MAX)+"]");
		} else if (type.equals(CType_BaseType.uShortType)) {
			item.setText(1, "["+String.valueOf(UserInitiatedMaxRange.UNSIGNED_SHORT_MIN)+","+String.valueOf(UserInitiatedMaxRange.UNSIGNED_SHORT_MAX)+"]");
		} else if (type.equals(CType_BaseType.uLongType)) {
			item.setText(1, "["+String.valueOf(UserInitiatedMaxRange.UNSIGNED_LONG_MIN)+","+String.valueOf(UserInitiatedMaxRange.UNSIGNED_LONG_MAX)+"]");
		} else if (type.equals(CType_BaseType.longLongType)) {
			item.setText(1, "["+String.valueOf(UserInitiatedMaxRange.LONGLONG_MIN)+","+String.valueOf(UserInitiatedMaxRange.LONGLONG_MAX)+"]");
		} else if (type.equals(CType_BaseType.uLongLongType)) {
			item.setText(1, "["+String.valueOf(UserInitiatedMaxRange.UNSIGNED_LONGLONG_MIN)+","+String.valueOf(UserInitiatedMaxRange.UNSIGNED_LONGLONG_MAX)+"]");
		} else if (type.equals(CType_BaseType.longDoubleType)) {
			item.setText(1, "["+String.valueOf(UserInitiatedMaxRange.LONG_DOUBLE_MIN)+","+String.valueOf(UserInitiatedMaxRange.LONG_DOUBLE_MAX)+"]");
		}
	}
	
	private void setEditableColor(UATTestCaseTreeItem[] items) {
		for(UATTestCaseTreeItem item : items) {
			String text = item.getText();
			if(item.getItems().length == 0) {
				if(!(text.equals("参数") || text.equals("全局变量")))
					item.setBackground(Resource.editableColor);
			}
			else
				setEditableColor(item.getItems());
		}
	}
	
	private void createMenu(){
		MenuItem item = new MenuItem(treeMenu, SWT.PUSH);
		item.setText("添加指针值");
		item.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				//oint pt = Display.getCurrent().map(null, tableTree, new Point(event.x, event.y));
				UATTestCaseTreeItem parent = tableTree.getSelection()[0];
				if(parent == null)
					return;
				CType _type = parent.getCType();
				final UATTestCaseTreeItem realParent = parent;
				
				if(_type instanceof CType_Struct) {
					CType_Struct structType = (CType_Struct)_type;
					LinkedHashMap<String, CType> fieldType = structType.getCTypeWithMems().getfieldType();
					for(String key : fieldType.keySet()) {
						CType type = fieldType.get(key);
						UATTestCaseTreeItem _item = createChildItem(type, realParent, null);
						_item.setText(0, key + " < " + type.getName() + " > ");
					}
				}
				//createItems1(realParent);
				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
	}

	// 填入一个测试用例
	public void addValues(TestCaseNew tc) {
		// TableTreeItem[] items = tableTree.getItems();
		List<AbstractParamValue> paramList = tc.getFuncParamList();
		List<AbstractParamValue> globalList = tc.getGlobalParamList();
		int colNum = tableTree.getColumnCount();
		if(!isEmpty(tableTree.getItems(), colNum - 1)) {//判断已有列是否为空，不为空则新建一列
			TreeColumn col = new TreeColumn(tableTree, SWT.CENTER);
			col.setWidth(120);
			col.setText(Integer.toString(colNum));
		}
		addValues(paramItem, paramList);
		addValues(globalItem, globalList);		
	}

	private void addValues(UATTestCaseTreeItem parent, List<AbstractParamValue> varList) {
		UATTestCaseTreeItem[] items = parent.getItems();
		for (AbstractParamValue apv : varList) {
//			String typeAndName = apv.getName() + " < " + apv.getTypeName() + " >";
			String typeAndName = apv.getName() + " < " + apv.getType().toString() + " >";
			for(UATTestCaseTreeItem _item : items) {
//				if(_item.getText().equals(typeAndName)) {
				if(_item.getVND().toString().equals(typeAndName)){
					addItemValue(_item, apv);
					break;
				}
			}
	}
	}
	
	// 向表格中填入值，使用递归调用
	private void addItemValue(UATTestCaseTreeItem item, AbstractParamValue apv) {
		int columnIndex = tableTree.getColumnCount() - 1;
		UATTestCaseTreeItem[] childItems = item.getItems();
		if (apv == null) {
			//item.setText(columnIndex, "NULL");
			return;
		}
		if (apv instanceof PrimitiveParamValue) {
			String value = ((PrimitiveParamValue) apv).getValue();
			if (value == null || value.equals("NULL"))
				value = "NULL";
			else if (((PrimitiveParamValue) apv).getType().getName()
					.contains("char")) {
				value = ASCIITranslator.translate(Integer.parseInt(value));
			}
			if(value.equals("000"))
				value = "0";
			item.setText(columnIndex, value);
		} else if (apv instanceof ArrayParamValue) {
			ArrayParamValue arrayParamValue = (ArrayParamValue) apv;
			int len = (int) arrayParamValue.getLen();
			if (len == -1)
				len = Config.dimSize4varLenArr;// 防止len没有被初始化
			Map<Integer, AbstractParamValue> value = arrayParamValue
					.getMemberValue();
			AbstractParamValue[] temp = new AbstractParamValue[len];
			for (Integer i : value.keySet()) {
				temp[i] = value.get(i);
			}
			for (int i = 0; i < temp.length; i++)
				addItemValue(childItems[i], temp[i]);
		} else if (apv instanceof PointerParamValue) {
			PointerParamValue pointerParamValue = (PointerParamValue) apv;
			AbstractParamValue temp = pointerParamValue.getMemberValue();
			//TODO: 指针从表里读时要做处理。
			if (temp == null)
				item.setText(columnIndex, "NULL");
			addItemValue(childItems[0], temp);
		} else if (apv instanceof StructParamValue) {
			StructParamValue structParamValue = (StructParamValue) apv;
			Map<String, AbstractParamValue> value = structParamValue
					.getMemberValue();
			AbstractParamValue temp = null;
			if (childItems[0].getText().equals("") && childItems[0].getItems().length!=0)
				childItems = childItems[0].getItems();
			if(value.size() == 0) {
				item.setText(columnIndex, "NULL");
			}
			else {
				if (childItems.length == 0) {
					CType_Struct structType = (CType_Struct)item.getCType();
					LinkedHashMap<String, CType> fieldType = structType.getCTypeWithMems().getfieldType();
					for(String key : fieldType.keySet()) {
						CType type = fieldType.get(key);
						UATTestCaseTreeItem _item = createChildItem(type, item, null);
						_item.setText(0, key + " < " + type.getName() + " > ");
					}
					/*for (String i : value.keySet()) {// for不等长链表等类型，发现深度不够时就构造新的item
						temp = value.get(i);
							//createChildItem(temp.getType(), item, null);
						String name = i.substring(i.lastIndexOf('.') + 1, i.length());
							UATTestCaseTreeItem _item = createChildItem(temp.getType(), item, null);
							_item.setText(0, name + " < " + temp.getType().getName() + " > ");
					}*/
					childItems = item.getItems();
					item.setExpanded(false);
				}
				for (String i : value.keySet()) {
					temp = value.get(i);
					String name = i.substring(i.lastIndexOf('.') + 1, i.length());
					CType type = temp.getType();
					String typeAndName = name + " < " + type.getName() + " > ";
					for(UATTestCaseTreeItem childItem : childItems) {
						if(childItem.getText().equals(typeAndName)) {
							addItemValue(childItem, temp);
							break;
						}
					}
				}
			}
		}
		else if(apv instanceof TypeDefParamValue) {
			if (apv == null)
				return;
			AbstractParamValue value = ((TypeDefParamValue) apv).getMemberValue();
			addItemValue(childItems[0], value);
		}
		else if(apv instanceof EnumParamValue) {
			if (apv == null)
				return;
			String value = ((EnumParamValue) apv).getValue();
			if (value == null)
				value = "NULL";
			item.setText(columnIndex + 1, value);
		}
		else if(apv instanceof FunctionParamValue) {
			if (apv == null)
				return;
			String value = ((FunctionParamValue) apv).getFunctionName();
			if (value == null)
				value = "NULL";
			item.setText(columnIndex + 1, value);
		}
	}
}

class TestCaseNewConstructionError extends Exception{	
}
