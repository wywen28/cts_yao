package unittest.gui;

import org.apache.tools.ant.taskdefs.Get;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.util.CLanguageMaxTypeRange;

/**
 * @author Cai Min
 *
 */
public class UATDomainEditGUI {
	private Shell shell;
	//private Shell parentShell;
	private Display display;
	private String domain;
	
	private Composite tableComp;
	private Composite buttonComp;
	
	private Table table;
	private Button addButton;
	private Button delButton;
	private Button resetButton;
	private Text domainText;
	private Button cancelButton;
	private Button okButton;
	private String domainType;

	public UATDomainEditGUI(String domainString, String type) {
		display = Display.getDefault();
		this.domain = domainString;
		this.domainType = type;
		//parentShell.setEnabled(false);
		createShell();
		dealEvent();
		
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
	public String getDomain(){
		return domain;
	}
	
	private void createShell() {
		shell = new Shell(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL | SWT.MAX | SWT.MIN);
		shell.setSize(600, 500);
		shell.setText("人工辅助-区间设置");
		shell.setLayout(new FormLayout());	
		shell.setImage(Resource.UATImage);
		LayoutUtil.centerShell(display, shell);
		
		Composite comp1 = new Composite(shell, SWT.BORDER);
		WidgetFactory.configureFormData(comp1, new FormAttachment(0,0), new FormAttachment(0), new FormAttachment(100), new FormAttachment(0, 45));
		
		Label label_1 = new Label(comp1, SWT.NONE);
		label_1.setBounds(5, 10, 210, 16);
		label_1.setText("双击单元格编辑区间");
		
		tableComp = new Composite(shell, SWT.BORDER);
		WidgetFactory.configureFormData(tableComp, new FormAttachment(0, 0), new FormAttachment(comp1), new FormAttachment(100),  new FormAttachment(0, 492));
		tableComp.setLayout(new FormLayout());
		
		createTable();
		createButtonComp();
		createDomainPreview();
	
	}

	private void createTable() {
		table = new Table(tableComp, SWT.BORDER | SWT.FULL_SELECTION);
		WidgetFactory.configureFormData(table, new FormAttachment(0, 0), new FormAttachment(0, 5), new FormAttachment(100, -117), new FormAttachment(0, 323));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn tableColumn = new TableColumn(table, SWT.NONE);
		tableColumn.setWidth(100);
		tableColumn.setText("左区间");
		
		TableColumn tableColumn_1 = new TableColumn(table, SWT.NONE);
		tableColumn_1.setWidth(100);
		tableColumn_1.setText("右区间");
		
		String temp = domain;
		TableItem tableItem;
		while (!temp.equals("")){
			int loc = temp.indexOf("]");
			String seperateDomain = temp.substring(0, loc+1);
			tableItem = new TableItem(table, SWT.NONE);
			if (seperateDomain.startsWith(","))
				seperateDomain = seperateDomain.substring(1);
			tableItem.setText(seperateDomain.substring(1, seperateDomain.indexOf(",")));
			tableItem.setText(1, seperateDomain.substring(seperateDomain.indexOf(",")+1, seperateDomain.length()-1));
			temp = temp.substring(loc+1);
		}
	}
	
	private void createButtonComp() {
		buttonComp = new Composite(tableComp, SWT.BORDER);
		WidgetFactory.configureFormData(buttonComp, new FormAttachment(table, 5), new FormAttachment(0, 5), new FormAttachment(100, -5), new FormAttachment(0, 323));
		
		addButton = new Button(buttonComp, SWT.NONE);
		addButton.setBounds(17, 27, 81, 26);
		addButton.setText("添加");
		
		delButton = new Button(buttonComp, SWT.NONE);
		delButton.setBounds(17, 82, 81, 26);
		delButton.setText("删除");
		//只有一个区间段时不支持删除区间操作
		if (table.getItemCount() <= 1)
			delButton.setEnabled(false);
		
		resetButton = new Button(buttonComp, SWT.NONE);
		resetButton.setBounds(17, 141, 81, 26);
		resetButton.setText("还原");

	}
	
	private void createDomainPreview() {
		Label label_2 = new Label(tableComp, SWT.NONE);
		WidgetFactory.configureFormData(label_2, new FormAttachment(0, 0), new FormAttachment(table), null,  null);
		label_2.setText("区间预览:");
		
		domainText = new Text(tableComp, SWT.BORDER);
		WidgetFactory.configureFormData(domainText, new FormAttachment(0, 0), new FormAttachment(label_2, 8), new FormAttachment(100),  null);
		domainText.setEditable(false);
		domainText.setText(domain);
		
		okButton = new Button(tableComp, SWT.NONE);
		WidgetFactory.configureFormData(okButton, new FormAttachment(100, -300), new FormAttachment(domainText, 10), null,null);
		okButton.setText("      应用      ");
		
		cancelButton = new Button(tableComp, SWT.NONE);
		WidgetFactory.configureFormData(cancelButton, new FormAttachment(okButton, 25), new FormAttachment(domainText, 10), null,null);
		cancelButton.setText("      取消      ");
	}
	
	private void dealEvent() {
		table.addMouseListener(new MouseListener() {		
			@Override
			public void mouseUp(MouseEvent arg0) {}
			
			@Override
			public void mouseDoubleClick(MouseEvent event) {
				int i = table.getSelectionIndex();
				if (i < 0)
					return;
				final TableItem item = table.getItem(i);
				final TableEditor editor = new TableEditor(table);
				
				int column = -1;
				Point pt = new Point(event.x, event.y);
				for (int j = 0; j <= 1; j++) {
					Rectangle rect = item.getBounds(j);
					if (rect.contains(pt)) {
						column = j;
						break;
					}
				}
				final int col = column;
				
				boolean showBorder = true;
				final Composite composite = new Composite(table,
						SWT.NONE);
				final Text text = new Text(composite, SWT.NONE | SWT.CENTER);
				final int inset = showBorder ? 1 : 0;
				composite.addListener(SWT.Resize, new Listener() {
					public void handleEvent(Event e) {
						Rectangle rect = composite.getClientArea();
						text.setBounds(rect.x + inset, rect.y + inset, rect.width - inset * 2, rect.height - inset * 2);
					}
				});
				
				Listener textListener = new Listener() {
					public void handleEvent(final Event e) {
						switch (e.type) {
						case SWT.FocusOut:
							if (!text.getText().equals("")){	//按照相应类型进行格式化
								String tempStr = text.getText();
								if (domainType.contains("float") || domainType.contains("double")){
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
								item.setText(col, tempStr);
							}
							if (!checkValidity() || !checkNotConflict(item)){
								String str = "请输入...";
								item.setText(col, str);
								composite.dispose();
								domainText.setText(domainPreview());
							}
							if (!composite.isDisposed())
								composite.dispose();
							domainText.setText(domainPreview());
							break;
						case SWT.Verify:
							String newText = text.getText();
							String leftText = newText.substring(0, e.start);
							String rightText = newText.substring(e.end, newText.length());
							GC gc = new GC(text);
							Point size = gc.textExtent(leftText + e.text + rightText);
							gc.dispose();
							size = text.computeSize(size.x, SWT.DEFAULT);
							Rectangle itemRect = item.getBounds(col),
							rect = table.getClientArea();
							editor.minimumWidth = Math.max(size.x, itemRect.width) + inset * 2;
							int left = itemRect.x,
							right = rect.x + rect.width;
							editor.minimumWidth = Math.min(editor.minimumWidth, right - left);
							editor.minimumHeight = size.y + inset * 2;
							editor.layout();
							break;
						case SWT.Traverse:
							switch (e.detail) {
								case SWT.TRAVERSE_RETURN:
									if (!text.getText().equals("")){	//按照相应类型进行格式化
										String tempStr = text.getText();
										if (domainType.contains("float") || domainType.contains("double")){
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
										item.setText(col, tempStr);
									}
									if (!checkValidity() || !checkNotConflict(item)){
										String str = "请输入...";
										item.setText(col, str);
										composite.dispose();
										domainText.setText(domainPreview());
									}
									// FALL THROUGH
								case SWT.TRAVERSE_ESCAPE:
									if (!composite.isDisposed())
										composite.dispose();
									domainText.setText(domainPreview());
									e.doit = false;
									break;
							}
						}
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
			
			@Override
			public void mouseDown(MouseEvent arg0) {}
		});
		
		addButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				int count = table.getItemCount();
				TableItem item = new TableItem (table, SWT.NONE, count);
				item.setText(0,"请输入...");
				item.setText(1,"请输入...");
				table.setSelection(count);
				domainText.setText("请将区间输入完整");
				okButton.setEnabled(false);
				delButton.setEnabled(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		delButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				int i = table.getSelectionIndex();
				if(i >= 0 && i < table.getItemCount() && table.getItemCount() >= 2)
					table.remove(i);
				if(i >= 0 && i < table.getItemCount())
					table.setSelection(i);
				domainText.setText(domainPreview());
				//当只有一个区间时，不能进行删除区间操作     add by chenruolin
				if (table.getItemCount() == 1)
					delButton.setEnabled(false);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		resetButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				int count = table.getItems().length;
				while (count > 0){
					count--;
					table.remove(count);
				}	
				String temp = domain;
				TableItem tableItem;
				while (!temp.equals("")){
					int loc = temp.indexOf("]");
					String seperateDomain = temp.substring(0, loc+1);
					tableItem = new TableItem(table, SWT.NONE);
					if (seperateDomain.startsWith(","))
						seperateDomain = seperateDomain.substring(1);
					tableItem.setText(seperateDomain.substring(1, seperateDomain.indexOf(",")));
					tableItem.setText(1, seperateDomain.substring(seperateDomain.indexOf(",")+1, seperateDomain.length()-1));
					temp = temp.substring(loc+1);
				}
				domainText.setText(domain);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		cancelButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				shell.dispose();
				//parentShell.setEnabled(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		okButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				domain = domainPreview();
				shell.dispose();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
	}

	private boolean checkValidity(){
		String leftDomainString = table.getSelection()[0].getText(0);
		String rightDomainString = table.getSelection()[0].getText(1);
		boolean leftNegtive = false;
		boolean rightNegtive = false;
		if (leftDomainString.startsWith("-")||leftDomainString.startsWith("+")){
			if (leftDomainString.startsWith("-"))
				leftNegtive = true;
			leftDomainString = leftDomainString.substring(1);
		}
		if (rightDomainString.startsWith("-")||rightDomainString.startsWith("+")){
			if (rightDomainString.startsWith("-"))
				rightNegtive = true;
			rightDomainString = rightDomainString.substring(1);
		}
		if (leftDomainString.equals("请输入...")){
			if (!isNumeric(rightDomainString)){
				return false;
			}
			return true;
		}
		else if (rightDomainString.equals("请输入...")){
			if (!isNumeric(leftDomainString)){
				return false;
			}
			return true;
		}
		else{
			if (!isNumeric(leftDomainString) || !isNumeric(rightDomainString))
				return false;
			else{
				try{
					if (!domainType.contains("float")&&!domainType.contains("double")){
						if (!CLanguageMaxTypeRange.isValid(domainType, Long.parseLong(rightDomainString)))
							return false;
						if (!CLanguageMaxTypeRange.isValid(domainType, Long.parseLong(leftDomainString)))
							return false;
					}
					else{
						if (!CLanguageMaxTypeRange.isValid(domainType, Double.parseDouble(rightDomainString)))
							return false;
						if (!CLanguageMaxTypeRange.isValid(domainType, Double.parseDouble(leftDomainString)))
							return false;
					}
				}
				catch (Exception e) {
					return false;
				}
				
				double left = Double.parseDouble(leftDomainString);
				double right = Double.parseDouble(rightDomainString);
				if (leftNegtive)
					left = 0 - left;
				if (rightNegtive)
					right = 0 - right;
				if (Double.compare(left, right)>=0)
					return false;
			}
		}
		return true;
	}
	
	private boolean checkNotConflict(TableItem item){
		if (item.getText(0).equals("请输入...") || item.getText(1).equals("请输入..."))
			return true;
		String leftD = item.getText(0);
		String rightD = item.getText(1);
		boolean leftNegtive = false;
		boolean rightNegtive = false;
		if (leftD.startsWith("-")||leftD.startsWith("+")){
			if (leftD.startsWith("-"))
				leftNegtive = true;
			leftD = leftD.substring(1);
		}
		if (rightD.startsWith("-")||rightD.startsWith("+")){
			if (rightD.startsWith("-"))
				rightNegtive = true;
			rightD = rightD.substring(1);
		}
		double leftValue = Double.parseDouble(leftD);
		double rightValue = Double.parseDouble(rightD);
		if (leftNegtive)
			leftValue = 0 - leftValue;
		if (rightNegtive)
			rightValue = 0 - rightValue;
		for (int i=0; i<table.getItemCount(); i++){
			if (table.getItem(i).equals(item))
				continue;
			String leftDomainString = table.getItem(i).getText(0);
			String rightDomainString = table.getItem(i).getText(1);
			if (leftDomainString.equals("请输入...") || rightDomainString.equals("请输入..."))
				continue;
			//处理需要比较的边界值
			leftNegtive = false;
			rightNegtive = false;
			if (leftDomainString.startsWith("-")||leftDomainString.startsWith("+")){
				if (leftDomainString.startsWith("-"))
					leftNegtive = true;
				leftDomainString = leftDomainString.substring(1);
			}
			if (rightDomainString.startsWith("-")||rightDomainString.startsWith("+")){
				if (rightDomainString.startsWith("-"))
					rightNegtive = true;
				rightDomainString = rightDomainString.substring(1);
			}
			double left = Double.parseDouble(leftDomainString);
			double right = Double.parseDouble(rightDomainString);
			if (leftNegtive)
				left = 0 - left;
			if (rightNegtive)
				right = 0 - right;
			//判断边界是否有重合的
			if (leftD.equals(table.getItem(i).getText(0)) || rightD.equals(table.getItem(i).getText(1)) 
					|| leftD.equals(table.getItem(i).getText(1)) || rightD.equals(table.getItem(i).getText(0)))
				return false;
			//判断边界是否在已有边界内
			if ((Double.compare(leftValue, left)>0 && Double.compare(leftValue, right)<0) || 
					(Double.compare(rightValue, left)>0 && Double.compare(rightValue, right)<0))
				return false;
			//判断是否包括已有区间
			if ((Double.compare(leftValue, left)>0 && Double.compare(rightValue, right)<0))
				return false;
			//判断是否在已有区间内
			if ((Double.compare(leftValue, left)<0 && Double.compare(rightValue, right)>0))
				return false;
		}
		return true;
	}
	
	private static boolean isNumeric(String str){
		if (str.isEmpty())
			return false;
		
		//add by chenruolin
		boolean hasDecimalPoint = false;	//mark if we already have a decimal point
		for (int i = 0; i < str.length(); i++){
		   if (!Character.isDigit(str.charAt(i)) && !(str.charAt(i) == '.'))
			   return false;
		   else if (str.charAt(i) == '.') {
			   if (hasDecimalPoint)		//the string contains more than one decimal point
				   return false;
			   else
				   hasDecimalPoint = true;
			   
			   if (str.startsWith("."))
				   return false;
			   else if ((str.startsWith("-") || str.startsWith("+")) && str.charAt(1)=='.')
				   return false;
		   }
		}
		return true;
	}
	
	private String domainPreview(){
		String tempDomain = "";
		for (int i=0; i<table.getItemCount(); i++){
			if (table.getItem(i).getText(0).equals("请输入...") || table.getItem(i).getText(1).equals("请输入...")){
				tempDomain = "请将区间输入完整";
				okButton.setEnabled(false);
				return tempDomain;
			}
			tempDomain = tempDomain+"["+table.getItem(i).getText(0)+","+table.getItem(i).getText(1)+"]"+",";
		}
		tempDomain = tempDomain.substring(0, tempDomain.length()-1);
		okButton.setEnabled(true);
		return tempDomain;
	}
	
	//public static void main(String[] args) {
	//	UATDomainEditGUI test = new UATDomainEditGUI();
	//}
}
