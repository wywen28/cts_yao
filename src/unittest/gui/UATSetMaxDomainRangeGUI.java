package unittest.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import softtest.symboltable.c.Type.CType_Pointer;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.util.CLanguageMaxTypeRange;
import unittest.util.Config;
import unittest.util.RecordToLogger;
import unittest.util.UserInitiatedMaxRange;

/*
 * @author zhouao
 * 用户输入各类型最大边界
 */
public class UATSetMaxDomainRangeGUI {
	static Logger logger = Logger.getLogger(UATSetMaxDomainRangeGUI.class);
	
	public UATGUI uatGUI;
	private Table table;
	private boolean hasIllegal = false;
	
	public Shell shell = null;
	private Display  display = null;
	private Composite topComposite = null;
	private Composite bottomComposite = null;
	
	private Button okButton = null;
	private Button cancelButton = null;
	private TableItem selectAllItem = null;
	
	public UATSetMaxDomainRangeGUI (UATGUI uatGUI){
		this.uatGUI = uatGUI;
		display = Display.getDefault();
		shell = new Shell(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL);
	}
	
	public void go(){
		this.createShell();
		this.dealEvent();
		this.shell.open();
		LayoutUtil.centerShell(display, shell);
		
		while( !display.isDisposed() ) 
		{
			if( !display.readAndDispatch() ) 
			{
				display.sleep();
			}
		}
		display.dispose();
	}
	
	private void createShell(){
		shell.setText( GUILanguageResource.getProperty("MaxRangeSetting") );
		shell.setImage( Resource.UATImage );
		shell.setBounds( 50, 50, 550, 400);
		shell.setLayout( new FormLayout() );
		shell.setMaximized( false );
		createComposite();
	}
	
	private void createComposite(){
		createTopComposite();
		createBottomComposite();
	}
	
	private void createTopComposite(){
		topComposite = WidgetFactory.createComposite( shell, SWT.FLAT );
		topComposite.setBackground( Resource.backgroundColor );
		topComposite.setLayout( new FormLayout() );
		WidgetFactory.configureFormData( topComposite, new FormAttachment( 0, 5 ),
				new FormAttachment( 0, 5 ),
				new FormAttachment( 100, -5 ),
				new FormAttachment( 85, 100, 0));
		
		table = new Table( topComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER| SWT.SINGLE|SWT.FULL_SELECTION|SWT.CHECK);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		WidgetFactory.configureFormData( table, new FormAttachment( 0, 0 ),
				new FormAttachment( 0, 0 ),
				new FormAttachment( 100, 0 ),
				new FormAttachment(100,0));
		
		createTableColumns();
		packColumns();
		
	}
	
	public void createBottomComposite(){
		bottomComposite = WidgetFactory.createComposite( shell, SWT.BORDER );
		bottomComposite.setLayout( new FormLayout() );		
		WidgetFactory.configureFormData( bottomComposite, new FormAttachment( 0, 5 ),
				new FormAttachment( topComposite, 5 ),
				new FormAttachment( 100, -5 ),
				new FormAttachment( 100, -5 ));
		
		okButton = WidgetFactory.createButton(bottomComposite, SWT.PUSH, GUILanguageResource.getProperty("OK"));
		cancelButton = WidgetFactory.createButton(bottomComposite, SWT.PUSH, GUILanguageResource.getProperty("Cancel"));
		
		WidgetFactory.configureFormData( okButton, new FormAttachment( 30, 100,0 ),
				new FormAttachment( 40, 100,0 ),
				null,
				null);
		WidgetFactory.configureFormData( cancelButton, new FormAttachment( 60, 100,0 ),
				new FormAttachment( 40, 100,0 ),
				null,
				null);
	}
	
	public void createTableColumns(){
		TableColumn column1 = new TableColumn(table,SWT.LEFT|SWT.CHECK);
		column1.setText(GUILanguageResource.getProperty("Type"));
		
		TableColumn column2 = new TableColumn(table,SWT.LEFT);
		column2.setText(GUILanguageResource.getProperty("leftInterval"));
		
		TableColumn column3 = new TableColumn(table,SWT.LEFT);
		column3.setText(GUILanguageResource.getProperty("rightInterval"));
		
		TableColumn column4 = new TableColumn(table,SWT.LEFT);
		column4.setText(GUILanguageResource.getProperty("ActualMaxRange"));
		
		TableItem intItem = new TableItem(table,SWT.NONE);
		intItem.setText(0, "int");
		intItem.setText(1, ""+UserInitiatedMaxRange.INT_MIN);
		intItem.setText(2, ""+UserInitiatedMaxRange.INT_MAX);
		intItem.setText(3, "[-2147483648,2147483647]");
		intItem.setData("DomainType", "long");
		
		TableItem uintItem = new TableItem(table,SWT.NONE);
		uintItem.setText(0, "unsigned int");
		uintItem.setText(1, ""+UserInitiatedMaxRange.UNSIGNED_INT_MIN);
		uintItem.setText(2, ""+UserInitiatedMaxRange.UNSIGNED_INT_MAX);
		uintItem.setText(3, "[0,4294967295L]");
		uintItem.setData("DomainType", "long");
		
		TableItem charItem = new TableItem(table,SWT.NONE);
		charItem.setText(0, "char");
		charItem.setText(1, ""+UserInitiatedMaxRange.CHAR_MIN);
		charItem.setText(2, ""+UserInitiatedMaxRange.CHAR_MAX);
		charItem.setText(3, "[-128,127]");
		charItem.setData("DomainType", "long");
		
		TableItem signedcharItem = new TableItem(table,SWT.NONE);
		signedcharItem.setText(0, "signed char");
		signedcharItem.setText(1, ""+UserInitiatedMaxRange.SIGNED_CHAR_MIN);
		signedcharItem.setText(2, ""+UserInitiatedMaxRange.SIGNED_CHAR_MAX);
		signedcharItem.setText(3, "[-128,127]");
		signedcharItem.setData("DomainType", "long");
		
		TableItem ucharItem = new TableItem(table,SWT.NONE);
		ucharItem.setText(0, "unsigned char");
		ucharItem.setText(1, ""+UserInitiatedMaxRange.UNSIGNED_CHAR_MIN);
		ucharItem.setText(2, ""+UserInitiatedMaxRange.UNSIGNED_CHAR_MAX);
		ucharItem.setText(3, "[0,255]");
		ucharItem.setData("DomainType", "long");
		
		TableItem doubleItem = new TableItem(table,SWT.NONE);
		doubleItem.setText(0, "double");
		doubleItem.setText(1, ""+UserInitiatedMaxRange.DOUBLE_MIN);
		doubleItem.setText(2, ""+UserInitiatedMaxRange.DOUBLE_MAX);
		doubleItem.setText(3, "[-1.79769e+308,1.79769e+308]");
		doubleItem.setData("DomainType", "double");
		
		TableItem longdoubleItem = new TableItem(table,SWT.NONE);
		longdoubleItem.setText(0, "long double");
		longdoubleItem.setText(1, ""+UserInitiatedMaxRange.LONG_DOUBLE_MIN);
		longdoubleItem.setText(2, ""+UserInitiatedMaxRange.LONG_DOUBLE_MAX);
		longdoubleItem.setText(3, "[-1.79769e+308,1.79769e+308]");
		longdoubleItem.setData("DomainType", "double");
		
		TableItem floatItem = new TableItem(table,SWT.NONE);
		floatItem.setText(0, "float");
		floatItem.setText(1, ""+UserInitiatedMaxRange.FLOAT_MIN);
		floatItem.setText(2, ""+UserInitiatedMaxRange.FLOAT_MAX);
		floatItem.setText(3, "[-3.40282e+038,3.40282e+038]");
		floatItem.setData("DomainType", "double");
		
		TableItem longItem = new TableItem(table,SWT.NONE);
		longItem.setText(0, "long");
		longItem.setText(1, ""+UserInitiatedMaxRange.LONG_MIN);
		longItem.setText(2, ""+UserInitiatedMaxRange.LONG_MAX);
		longItem.setText(3, "[-2147483648,2147483647]");
		longItem.setData("DomainType", "long");
		
		TableItem ulongItem = new TableItem(table,SWT.NONE);
		ulongItem.setText(0, "unsigned long");
		ulongItem.setText(1, ""+UserInitiatedMaxRange.UNSIGNED_LONG_MIN);
		ulongItem.setText(2, ""+UserInitiatedMaxRange.UNSIGNED_LONG_MAX);
		ulongItem.setText(3, "[0,4294967295L]");
		ulongItem.setData("DomainType", "long");
		
		TableItem longlongItem = new TableItem(table,SWT.NONE);
		longlongItem.setText(0, "long long");
		longlongItem.setText(1, ""+UserInitiatedMaxRange.LONGLONG_MIN);
		longlongItem.setText(2, ""+UserInitiatedMaxRange.LONGLONG_MAX);
		longlongItem.setText(3, "[-9223372036854774808L,9223372036854774807L]");
		longlongItem.setData("DomainType", "long");
		
		TableItem ulonglongItem = new TableItem(table,SWT.NONE);
		ulonglongItem.setText(0, "unsigned long long");
		ulonglongItem.setText(1, ""+UserInitiatedMaxRange.UNSIGNED_LONGLONG_MIN);
		ulonglongItem.setText(2, ""+UserInitiatedMaxRange.UNSIGNED_LONGLONG_MAX);
		ulonglongItem.setText(3, "[0,18446744073709549615.0]");
		ulonglongItem.setData("DomainType", "long");
		
		TableItem shortItem = new TableItem(table,SWT.NONE);
		shortItem.setText(0, "short");
		shortItem.setText(1, ""+UserInitiatedMaxRange.SHORT_MIN);
		shortItem.setText(2, ""+UserInitiatedMaxRange.SHORT_MAX);
		shortItem.setText(3, "[-32768,32767]");
		shortItem.setData("DomainType", "long");
		
		TableItem ushortItem = new TableItem(table,SWT.NONE);
		ushortItem.setText(0, "unsigned short");
		ushortItem.setText(1, ""+UserInitiatedMaxRange.UNSIGNED_SHORT_MIN);
		ushortItem.setText(2, ""+UserInitiatedMaxRange.UNSIGNED_SHORT_MAX);
		ushortItem.setText(3, "[0,65535]");
		ushortItem.setData("DomainType", "long");
		
		TableItem selectAllItem = new TableItem(table,SWT.NONE);
		selectAllItem.setText(0, "全选");
		selectAllItem.setText(1, "");
		selectAllItem.setText(2, "");
		selectAllItem.setText(3, "");
		selectAllItem.setData("DomainType", "null");
		this.selectAllItem = selectAllItem;

		
		//addCheckBox();
	}
	
	public void packColumns(){
		TableColumn[] columns = table.getColumns();
		for (int i = 0, n = columns.length; i < n; i++) {
			columns[i].pack();
		}
	}
	
	public void dealEvent(){
		shell.addShellListener( new ShellCloseListener( this ) );
		
		cancelButton.addSelectionListener(new SelectionAdapter() 
		{
			public void widgetSelected( SelectionEvent e ) 
			{
				uatGUI.getShell().setEnabled(true);
				shell.dispose();	
			}
		});
		
		addTextEditor(table);
		
		table.addListener(SWT.Selection, new Listener(){
	        public void handleEvent(Event event) {
	            if (event.detail == SWT.CHECK) {
	                TableItem item = (TableItem) event.item;
	                	if( item == selectAllItem){
	                		if(item.getChecked()){
	                			TableItem[] items = table.getItems();
	                			for (int i = 0, n = items.length; i < n-1; i++) {
	                				items[i].setChecked(true);
	                			}
	                		}
	                		else{
	                			TableItem[] items = table.getItems();
	                			for (int i = 0, n = items.length; i < n-1; i++) {
	                				items[i].setChecked(false);
	                			}
	                		}
	                	}
	                	//add by xujiaoxian 2012-10-10
	                	//解决变量边界值设置界面复选框全选的问题
	                	else{
	                		TableItem[] items = table.getItems();
	                		boolean isAllSelected = true;
	                		for(int i=0,n=items.length;i<n-1;i++){
	                			isAllSelected = isAllSelected && items[i].getChecked();
	                		}
	                		if(isAllSelected){
	                			items[items.length-1].setChecked(true);
	                		}
	                		else{
	                			items[items.length-1].setChecked(false);
	                		}
	                	}
	                	//add by xujiaoxian
	            }

	        }

	    });
		

		
		okButton.addSelectionListener(new SelectionAdapter() 
		{
			public void widgetSelected( SelectionEvent e ) 
			{
				new Thread()
				{
					public void run()
					{
						try{
							Display.getDefault().asyncExec(new Runnable()
							{
								public void run()
								{
									doStore();
									Config.needSavePro = true;
									if(hasIllegal){
										MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "存在违法输入，请确保输入是有效最大范围内的有效数字");
										box.open();
									}
									else {
										doExit();
									}
										
								}
							});
							
						}catch(Exception e){
							RecordToLogger.recordExceptionInfo(e, logger);
						}
					}
				}.start();
			}
					
		});
	}
	
	//设置行可编辑的属性
	private void addTextEditor(final Table table)
	{
		// Create an editor object to use for text editing
		final TableEditor editor = new TableEditor(table);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		// Use a mouse listener, not a selection listener, since we're interested
		// in the selected column as well as row
		table.addMouseListener(new MouseAdapter() {		
			public void mouseDown(MouseEvent event){
				Control old = editor.getEditor();
				if (old != null) old.dispose();
			}
			public void mouseDoubleClick(MouseEvent event) {
				// Dispose any existing editor
				Control old = editor.getEditor();
				if (old != null) old.dispose();

				// Determine where the mouse was clicked
				Point pt = new Point(event.x, event.y);

				// Determine which row was selected
				final TableItem item = table.getItem(pt);
				//final TableItem item =table.getSelection()[0];
				
				if(item == null)
					return;

				//add by xujiaoxian 2012-10-11
				//解决能对全选的from和to中的值做修改的这个问题
				int itemCount = table.getItemCount();
				if(table.getItem(itemCount-1)==item){
					//do nothing
				}
				else{
					// Determine which column was selected
					int column = -1;
					for (int i = 0, n = table.getColumnCount(); i < n; i++) {
						Rectangle rect = item.getBounds(i);
						if (rect.contains(pt)) {
							// This is the selected column
							column = i;
							break;
						}
					}
					
					if (column > 0 && column <3 ) {
						//save the former text of the item
						String formerString = item.getText(column);
						
						// Create the Text object for our editor
						final Text text = new Text(table, SWT.CENTER);
						text.setForeground(item.getForeground());
						
						// Transfer any text from the cell to the Text control,
						// set the color to match this row, select the text,
						// and set focus to the control
						text.setText(item.getText(column));
						text.setForeground(item.getForeground());
						//text.selectAll();
						//text.setFocus();
						
						// Recalculate the minimum width for the editor
						editor.minimumWidth = text.getBounds().width;
						
						// Set the control into the editor
						editor.setEditor(text, item, column);
						
						// Add a handler to transfer the text back to the cell
						// any time it's modified
						final int col = column;
						final String fString = formerString;
						
						//add by chenruolin
						Listener textListener = new Listener() {
							public void handleEvent(final Event e) {
								switch (e.type) {
								case SWT.FocusOut:
									//item.setText(col, text.getText());
									finishInput(fString, SWT.FocusOut);
									break;
								case SWT.Traverse:
									switch (e.detail) {
									case SWT.TRAVERSE_RETURN:
										//item.setText(col, text.getText());
										finishInput(fString, SWT.TRAVERSE_RETURN);
										// FALL THROUGH
									case SWT.TRAVERSE_ESCAPE:
										e.doit = false;
									}
									break;
								}
							}

							boolean hasBeenOpened = false;		//若已在回车事件中生成过窗口，则FocusOut事件不再重复生成
							private void finishInput(final String oldText, final int event) {
								if (hasBeenOpened)
									return;
								//check input validity
								try{
									String domainType = (String)item.getData("DomainType");
									//parse函数不能转换包含"+"的整数，因此要将加号去掉
									if (text.getText().startsWith("+"))
										text.setText(text.getText().substring(1));
									
									if (domainType.equals("float") || domainType.equals("double") || domainType.equals("long double")){
										double doubleValue = Double.parseDouble(text.getText());
										//如果输入的是整数则添加小数点，如果输入的小数没有整数位则添加"0."
										//parse函数能够转换".12"和"-.12"这种数字，因此界面可以支持这种输入，但在显示时应将其改为正确的格式
										if (text.getText().indexOf(".") == -1)
											text.setText(text.getText()+".0");
										if (text.getText().startsWith("."))
											text.setText("0"+text.getText());
										else if (text.getText().startsWith("-")){
											if (text.getText().charAt(1) == '.'){
												String temp = text.getText();
												text.setText("-0"+temp.substring(1));
											}
										}
										
										
										String valueRange = item.getText(3);
										String minValueString = valueRange.substring(1, valueRange.indexOf(","));
										String maxValueString = valueRange.substring(valueRange.indexOf(",")+1, valueRange.length()-2);
										double minValue = Double.parseDouble(minValueString);
										double maxValue = Double.parseDouble(maxValueString);
										if (doubleValue>maxValue || doubleValue<minValue){
											if (event == SWT.TRAVERSE_RETURN)
												hasBeenOpened = true;
											MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "输入值超出取值范围！");
											box.open();
											item.setText(col, oldText);
											return;
										}
										if(col == 1){
											String teString = item.getText(2);
											double rightValue= Double.parseDouble(item.getText(2));
											if( (doubleValue>rightValue) || (!CLanguageMaxTypeRange.isValid(domainType, doubleValue))){
												if (event == SWT.TRAVERSE_RETURN)
													hasBeenOpened = true;
												MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "请输入有效值！");
												box.open();
												item.setText(col, oldText);
												return;
											}
										}
										if(col == 2){
											double leftValue= Double.parseDouble(item.getText(1));
											if( (doubleValue<leftValue) || (!CLanguageMaxTypeRange.isValid(domainType, doubleValue))){
												if (event == SWT.TRAVERSE_RETURN)
													hasBeenOpened = true;
												MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "请输入有效值！");
												box.open();
												item.setText(col, oldText);
												return;
											}
										}
									}
									else {
										long longValue = Long.parseLong(text.getText());
										String valueRange = item.getText(3);
										String minValueString = valueRange.substring(1, valueRange.indexOf(","));
										String maxValueString = valueRange.substring(valueRange.indexOf(",")+1, valueRange.length()-2);
										long minValue = Long.parseLong(minValueString);
										long maxValue = Long.parseLong(maxValueString);
										if (longValue>maxValue || longValue<minValue){
											if (event == SWT.TRAVERSE_RETURN)
												hasBeenOpened = true;
											MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "输入值超出取值范围！");
											box.open();
											item.setText(col, oldText);
											return;
										}
										if(col == 1){
											long rightValue= Long.parseLong(item.getText(2));
											if( (longValue>rightValue) || (!CLanguageMaxTypeRange.isValid(domainType, longValue))){
												if (event == SWT.TRAVERSE_RETURN)
													hasBeenOpened = true;
												MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "请输入有效值！");
												box.open();
												item.setText(col, oldText);
												return;
											}
										}
										if(col == 2){
											long leftValue= Long.parseLong(item.getText(1));
											if( (longValue<leftValue) || (!CLanguageMaxTypeRange.isValid(domainType, longValue))){
												if (event == SWT.TRAVERSE_RETURN)
													hasBeenOpened = true;
												MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "请输入有效值！");
												box.open();
												item.setText(col, oldText);
												return;
											}
										}
									}
									// Set the text of the editor's control back into the cell
									item.setText(col, text.getText());
								}
								catch(NumberFormatException e){
									Display.getDefault().asyncExec(new Runnable()
									{
										public void run()
										{
											if (event == SWT.TRAVERSE_RETURN)
												hasBeenOpened = true;
											MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "请确保输入的为数字");
											box.open();
											item.setText(col, oldText);
										}
									});
								}
							}
						};
						text.addListener(SWT.FocusOut, textListener);
						text.addListener(SWT.Traverse, textListener);
						text.selectAll();
						text.setFocus();
						
			/*			text.addModifyListener(new ModifyListener() {
							public void modifyText(ModifyEvent event) {
								//check input validity
								try{
									String domainType = (String)item.getData("DomainType");
									if (domainType.equals("float") || domainType.equals("double") || domainType.equals("long double")){
										double doubleValue = Double.parseDouble(text.getText());
										String valueRange = item.getText(3);
										String minValueString = valueRange.substring(1, valueRange.indexOf(","));
										String maxValueString = valueRange.substring(valueRange.indexOf(",")+1, valueRange.length()-2);
										double minValue = Double.parseDouble(minValueString);
										double maxValue = Double.parseDouble(maxValueString);
										if (doubleValue>maxValue || doubleValue<minValue){
											MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "输入值超出取值范围！");
											box.open();
											//item.setText(col, fString);
											return;
										}
										if(col == 1){
											double rightValue= Double.parseDouble(item.getText(2));
											if( (doubleValue>rightValue) || (!CLanguageMaxTypeRange.isValid(domainType, doubleValue))){
												MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "请输入有效值！");
												box.open();
												//item.setText(col, fString);
												return;
											}
										}
										if(col == 2){
											double leftValue= Double.parseDouble(item.getText(1));
											if( (doubleValue<leftValue) || (!CLanguageMaxTypeRange.isValid(domainType, doubleValue))){
												MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "请输入有效值！");
												box.open();
												//item.setText(col, fString);
												return;
											}
										}
									}
									else {
										long longValue = Long.parseLong(text.getText());
										String valueRange = item.getText(3);
										String minValueString = valueRange.substring(1, valueRange.indexOf(","));
										String maxValueString = valueRange.substring(valueRange.indexOf(",")+1, valueRange.length()-2);
										long minValue = Long.parseLong(minValueString);
										long maxValue = Long.parseLong(maxValueString);
										if (longValue>maxValue || longValue<minValue){
											MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "输入值超出取值范围！");
											box.open();
											//item.setText(col, fString);
											return;
										}
										if(col == 1){
											long rightValue= Long.parseLong(item.getText(2));
											if( (longValue>rightValue) || (!CLanguageMaxTypeRange.isValid(domainType, longValue))){
												MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "请输入有效值！");
												box.open();
												//item.setText(col, fString);
												return;
											}
										}
										if(col == 2){
											long leftValue= Long.parseLong(item.getText(1));
											if( (longValue<leftValue) || (!CLanguageMaxTypeRange.isValid(domainType, longValue))){
												MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "请输入有效值！");
												box.open();
												//item.setText(col, fString);
												return;
											}
										}
									}
									// Set the text of the editor's control back into the cell
									item.setText(col, text.getText());
								}
								catch(NumberFormatException e){
									Display.getDefault().asyncExec(new Runnable()
									{
										public void run()
										{
											MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示", "请确保输入的为数字");
											box.open();
											//item.setText(col, fString);
										}
									});
								}
							}
						});*/
					}
					
				}
				//end add by xujiaoxian 2012-10-11
			}
		});
	}
	
	
	private void doStore(){
		UserInitiatedMaxRange.clearHasInitiated();
		hasIllegal = false;
		
		TableItem[] items = table.getItems();
		TableItem item = null;
		int size = items.length - 1;
		for (int i = 0; i < size; i++) {
			item = items[i];
			item.setBackground(null);
			if(!item.getChecked())
				continue;
			
			String domainType = (String)item.getData("DomainType");
			String type = item.getText(0);
			try{
				if(domainType.equals("long")){
					long min= Long.parseLong(item.getText(1));
					long max= Long.parseLong(item.getText(2));
					if(CLanguageMaxTypeRange.isValid(type, min) && CLanguageMaxTypeRange.isValid(type, max)
							&& min<=max)
					{
						UserInitiatedMaxRange.setValue(type, min, max);
					}
					else{
						hasIllegal = true;
						item.setBackground(Resource.backgroundColor );
					}
					
				}
				if(domainType.equals("double")){
					double min = Double.parseDouble(item.getText(1));
					double max= Double.parseDouble(item.getText(2));
					if(CLanguageMaxTypeRange.isValid(type, min) && CLanguageMaxTypeRange.isValid(type, max)
							&& min<=max)
					{
						UserInitiatedMaxRange.setValue(type, min, max);
					}
					else{
						hasIllegal = true;
						item.setBackground(Resource.backgroundColor );
					}
				}
			}catch(NumberFormatException e){
				hasIllegal = true;
				item.setBackground(Resource.backgroundColor );
			}catch(Exception e){
				hasIllegal = true;
				item.setBackground(Resource.backgroundColor );
			}
		}
	}
	private void doExit(){
		uatGUI.getShell().setEnabled(true);
		shell.dispose();
	}
		
	
	public class ShellCloseListener extends ShellAdapter {
		private UATSetMaxDomainRangeGUI demo;
		public ShellCloseListener( UATSetMaxDomainRangeGUI demo ) {
			this.demo = demo;
		} 
		
		public void shellClosed( ShellEvent e ) {
			demo.uatGUI.getShell().setEnabled(true);
			demo.shell.dispose();
		}
		
	}

}
