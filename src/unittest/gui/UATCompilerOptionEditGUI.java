package unittest.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

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
import org.eclipse.swt.widgets.Item;
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
 * @author chenruolin
 *
 */
public class UATCompilerOptionEditGUI {
	private Shell shell;
	//private Shell parentShell;
	private Display display;
	private String compilerOptionString;
	
	Properties compilerProperties;
	
	private Composite tableComp;
	private Composite buttonComp;
	
	private Table table;
	private Button addButton;
	private Button delButton;
	private Text compilerOptionText;
	private Text domainText;
	private Button cancelButton;
	private Button okButton;

	public UATCompilerOptionEditGUI(String complierOptionString) {
		display = Display.getDefault();
		this.compilerOptionString = complierOptionString;
		//parentShell.setEnabled(false);
		
		//读入编译选项配置文件信息
		compilerProperties = new Properties();
		FileInputStream inputFile;
		try {
            inputFile = new FileInputStream(System.getProperty("user.dir") 
    				+ File.separator + "config" + File.separator + "compilerOptionsConfig.properties");
            compilerProperties.load(inputFile);
            inputFile.close();
        } catch (FileNotFoundException ex) {
        	System.out.println("无目标配置文件，创建新文件");
        	File file=new File(System.getProperty("user.dir") 
    				+ File.separator + "config" + File.separator + "compilerOptionsConfig.properties");
        	try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("装载文件--->失败");
            ex.printStackTrace();
        }
		
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
	
	public String getOptions(){
		return compilerOptionString;
	}
	
	private void createShell() {
		shell = new Shell(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL | SWT.MAX | SWT.MIN);
		shell.setSize(600, 500);
		shell.setText("编译选项设置");
		shell.setLayout(new FormLayout());	
		shell.setImage(Resource.UATImage);
		LayoutUtil.centerShell(display, shell);
		
		Composite comp1 = new Composite(shell, SWT.BORDER);
		WidgetFactory.configureFormData(comp1, new FormAttachment(0,0), new FormAttachment(0), new FormAttachment(100), new FormAttachment(0, 45));
		
		Label label_1 = new Label(comp1, SWT.NONE);
		label_1.setBounds(5, 10, 210, 16);
		label_1.setText("双击单元格编辑选项");
		
		tableComp = new Composite(shell, SWT.BORDER);
		WidgetFactory.configureFormData(tableComp, new FormAttachment(0, 0), new FormAttachment(comp1), new FormAttachment(100),  new FormAttachment(0, 492));
		tableComp.setLayout(new FormLayout());
		
		createTable();
		createButtonComp();
		
		Label label_2 = new Label(tableComp, SWT.NONE);
		WidgetFactory.configureFormData(label_2, new FormAttachment(0, 0), new FormAttachment(table), null,  null);
		label_2.setText("预览：");
		
		domainText = new Text(tableComp, SWT.BORDER);
		WidgetFactory.configureFormData(domainText, new FormAttachment(0, 0), new FormAttachment(label_2, 8), new FormAttachment(100),  null);
		domainText.setEditable(false);
		domainText.setText("暂不支持");
		
		okButton = new Button(tableComp, SWT.NONE);
		WidgetFactory.configureFormData(okButton, new FormAttachment(100, -300), new FormAttachment(domainText, 10), null,null);
		okButton.setText("      应用      ");
		
		cancelButton = new Button(tableComp, SWT.NONE);
		WidgetFactory.configureFormData(cancelButton, new FormAttachment(okButton, 25), new FormAttachment(domainText, 10), null,null);
		cancelButton.setText("      取消      ");
	}

	private void createTable() {
		table = new Table( tableComp, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER| SWT.SINGLE|SWT.FULL_SELECTION|SWT.CHECK);
		WidgetFactory.configureFormData(table, new FormAttachment(0, 0), new FormAttachment(0, 5), new FormAttachment(100, -117), new FormAttachment(0, 323));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn tableColumn = new TableColumn(table, SWT.CHECK);
		tableColumn.setWidth(100);
		tableColumn.setText("参数");
		
		TableColumn tableColumn_1 = new TableColumn(table, SWT.NONE);
		tableColumn_1.setWidth(300);
		tableColumn_1.setText("说明");
		
		String tempString = compilerOptionString;
		TableItem tableItem;
		String propertyString = compilerProperties.toString().substring(1);
		while (!compilerProperties.isEmpty() && !propertyString.equals("")){
			while (propertyString.startsWith(" "))
				propertyString = propertyString.substring(1);
			int loc = propertyString.indexOf(",")-1;
			if (loc < 0)
				loc = propertyString.indexOf("}")-1;
			String singleOption = propertyString.substring(0, loc+1);
			tableItem = new TableItem(table, SWT.NONE);
			tableItem.setText(0,singleOption.substring(0, singleOption.lastIndexOf("=")) );
			if (singleOption.substring(singleOption.lastIndexOf("=")+1).equals(""))
				tableItem.setText(1," ");
			else
				tableItem.setText(1,singleOption.substring(singleOption.lastIndexOf("=")+1) );
			if (loc != propertyString.length() - 1)
				propertyString = propertyString.substring(loc+2);
			else
				propertyString = "";
		}
		
		while (!tempString.equals("")){
			while (tempString.startsWith(" "))
				tempString = tempString.substring(1);
			int loc = tempString.indexOf(",");
			if (loc < 0)
				loc = tempString.length() - 1;
			else
				loc--;
			String singleOption = tempString.substring(0, loc+1);
			if (compilerProperties.containsKey(singleOption)){
				boolean find = false;
				for (int i=0; !find && i<table.getItems().length; i++){
					if (table.getItem(i).getText().equals(singleOption)){
						table.getItem(i).setChecked(true);
						find = true;
					}
				}
			}
			else{
				tableItem = new TableItem(table, SWT.NONE);
				tableItem.setText(singleOption);
				tableItem.setText(1, " ");
				tableItem.setChecked(true);
			}
			if (loc != tempString.length() - 1)
				tempString = tempString.substring(loc+2);
			else
				tempString = "";
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
							if (item.getText(1).equals(""))
								item.setText(col, " ");
							else
								item.setText(col, text.getText());
							if (compilerProperties.contains(item.getText(0)))
								compilerProperties.setProperty(item.getText(0), item.getText(1));
							else
								compilerProperties.put(item.getText(0), item.getText(1));
							if (!composite.isDisposed())
								composite.dispose();
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
									item.setText(col, text.getText());
									composite.dispose();
									// FALL THROUGH
								case SWT.TRAVERSE_ESCAPE:
									if (!composite.isDisposed())
										composite.dispose();
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
				item.setText(1," ");
				table.setSelection(count);
				//compilerOptionText.setText("请输入编译选项");
				delButton.setEnabled(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		delButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				int i = table.getSelectionIndex();
				if(i >= 0 && i < table.getItemCount() && table.getItemCount() >= 2){
					String keyString = table.getItem(i).getText();
					if (compilerProperties.containsKey(keyString))
						compilerProperties.remove(keyString);
					table.remove(i);
				}
				if(i >= 0 && i < table.getItemCount())
					table.setSelection(i);
				//当只有一个区间时，不能进行删除区间操作     add by chenruolin
				if (table.getItemCount() == 1)
					delButton.setEnabled(false);
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
				//按checklist生成最终结果
				String tempDomain = "";
				for (int i=0; i<table.getItemCount(); i++){
					if (table.getItem(i).getText(0).equals("请输入...")){
						MessageBox mb = WidgetFactory.createMessageBox( shell, SWT.ICON_ERROR | SWT.OK, "错误信息", "参数信息不完整！" );
						mb.open();
						return;
					}
					if (table.getItem(i).getChecked())
						tempDomain = tempDomain+table.getItem(i).getText(0)+",";
				}
				if (!tempDomain.equals(""))
					tempDomain = tempDomain.substring(0, tempDomain.length()-1);
				compilerOptionString = tempDomain;
				
				//写配置文件
				try{
		            FileOutputStream outputFile = new FileOutputStream(System.getProperty("user.dir") 
							+ File.separator + "config" + File.separator + "compilerOptionsConfig.properties");
		            compilerProperties.store(outputFile, "编译选项");
		            outputFile.close();
		        } catch (FileNotFoundException e){
		            e.printStackTrace();
		        } catch (IOException ioe){
		            ioe.printStackTrace();
		        }
				shell.dispose();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
	}
}
