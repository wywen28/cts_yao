package unittest.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;


/**
 * @author Cai Min
 *
 */
public class UATPathRangeSettingGUI {
	private Shell shell;
	private Display display;
	
	private Composite bottomComp;
	private Composite tableComp;
	private Composite buttonComp;
	
	private Table table;
	private Button okButton;
	private Button cancelButton;
	private Button editButton;
	private Button resetButton;
	private Button genTCButton;
	private Button lockButton;
	private Text domainText;
	private Button resetButton2;
	private Button applyButton;

	public UATPathRangeSettingGUI() {
		display = Display.getDefault();
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
	
	private void createShell() {
		shell = new Shell(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL | SWT.MAX | SWT.MIN);
		shell.setSize(800, 600);
		shell.setText("路径区间设置");
		shell.setLayout(new FormLayout());	
		shell.setImage(Resource.UATImage);
		LayoutUtil.centerShell(display, shell);
		
		Composite comp1 = new Composite(shell, SWT.BORDER);
		WidgetFactory.configureFormData(comp1, new FormAttachment(0,0), new FormAttachment(0), new FormAttachment(100), new FormAttachment(0, 45));
		
		Label label_1 = new Label(comp1, SWT.NONE);
		label_1.setBounds(5, 10, 210, 16);
		label_1.setText("根据提示区间为路径生成测试用例:");
		
		tableComp = new Composite(shell, SWT.BORDER);
		WidgetFactory.configureFormData(tableComp, new FormAttachment(0, 0), new FormAttachment(comp1), new FormAttachment(100),  new FormAttachment(0, 492));
		tableComp.setLayout(new FormLayout());
		
		createTable();
		createButtonComp();
		createDomainPreview();
		createButtomComp();
		
		table.setSelection(0);
		domainText.setText(table.getItem(0).getText(1));
	
	}

	private void createTable() {
		table = new Table(tableComp, SWT.BORDER | SWT.FULL_SELECTION);
		WidgetFactory.configureFormData(table, new FormAttachment(0, 0), new FormAttachment(0, 5), new FormAttachment(100, -117), new FormAttachment(0, 323));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn tableColumn = new TableColumn(table, SWT.NONE);
		tableColumn.setWidth(100);
		tableColumn.setText("参数/全局变量");
		
		TableColumn tableColumn_1 = new TableColumn(table, SWT.NONE);
		tableColumn_1.setWidth(355);
		tableColumn_1.setText("区间");
		
		TableColumn tableColumn_2 = new TableColumn(table, SWT.NONE);
		tableColumn_2.setWidth(100);
		tableColumn_2.setText("测试用例");
		
		TableColumn tableColumn_3 = new TableColumn(table, SWT.NONE);
		tableColumn_3.setWidth(100);
		tableColumn_3.setText("状态");
		
		TableItem tableItem = new TableItem(table, SWT.NONE);
		tableItem.setText("int a");
		tableItem.setText(1, "[0,100],[200,300],[400,500]");
		tableItem.setText(2, "50");
		tableItem.setText(3, "锁定");
		
		TableItem tableItem_1 = new TableItem(table, SWT.NONE);
		tableItem_1.setText("int b");
		tableItem_1.setText(1, "[0,100]");
		
		TableItem tableItem_2 = new TableItem(table, SWT.NONE);
		tableItem_2.setText("int c");
		tableItem_2.setText(1, "[0,500]");
	}
	
	private void createButtonComp() {
		buttonComp = new Composite(tableComp, SWT.BORDER);
		WidgetFactory.configureFormData(buttonComp, new FormAttachment(table, 5), new FormAttachment(0, 5), new FormAttachment(100, -5), new FormAttachment(0, 323));
		
		editButton = new Button(buttonComp, SWT.NONE);
		editButton.setBounds(17, 27, 81, 26);
		editButton.setText("编辑区间");
		
		resetButton = new Button(buttonComp, SWT.NONE);
		resetButton.setBounds(17, 82, 81, 26);
		resetButton.setText("复位区间");
		
		genTCButton = new Button(buttonComp, SWT.NONE);
		genTCButton.setBounds(17, 141, 81, 26);
		genTCButton.setText("生成测试用例");
		
		lockButton = new Button(buttonComp, SWT.NONE);
		lockButton.setBounds(17, 195, 81, 26);
		lockButton.setText("锁定");
	}
	
	private void createButtomComp() {
		bottomComp = new Composite(shell, SWT.BORDER);
		WidgetFactory.configureFormData(bottomComp, new FormAttachment(0), new FormAttachment(tableComp, 10), new FormAttachment(100), new FormAttachment(100));
		bottomComp.setLayout(new FormLayout());
		
		okButton = new Button(bottomComp, SWT.NONE);
		WidgetFactory.configureFormData(okButton, new FormAttachment(100, -210), new FormAttachment(0, 15), null,null);
		okButton.setText("      确定      ");
		
		cancelButton = new Button(bottomComp, SWT.NONE);
		WidgetFactory.configureFormData(cancelButton, new FormAttachment(okButton, 25), new FormAttachment(0, 15), null,null);
		cancelButton.setText("      取消      ");
	}
	
	private void createDomainPreview() {
		Label label_2 = new Label(tableComp, SWT.NONE);
		WidgetFactory.configureFormData(label_2, new FormAttachment(0, 0), new FormAttachment(table,10), null,  null);
		label_2.setText("区间预览:");
		
		domainText = new Text(tableComp, SWT.BORDER);
		WidgetFactory.configureFormData(domainText, new FormAttachment(0, 0), new FormAttachment(label_2, 8), new FormAttachment(100),  null);
		domainText.setEditable(false);
		
		resetButton2 = new Button(tableComp, SWT.NONE);
		WidgetFactory.configureFormData(resetButton2, new FormAttachment(100, -210), new FormAttachment(domainText, 20), null,null);
		resetButton2.setText("      复位      ");
		
		applyButton = new Button(tableComp, SWT.NONE);
		WidgetFactory.configureFormData(applyButton, new FormAttachment(resetButton2, 25), new FormAttachment(domainText, 20), null,null);
		applyButton.setText("      应用      ");
	}
	
	private void dealEvent() {
		table.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				int i = table.getSelectionIndex();
				TableItem item = table.getItem(i);
				domainText.setText(item.getText(1));
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		editButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				String temp1 = table.getSelection()[0].getText();
				temp1 = temp1.substring(0, temp1.lastIndexOf(' '));
				String temp2 = table.getSelection()[0].getText(1);
				UATDomainEditGUI domainEditGUI = new UATDomainEditGUI(temp2, temp1);
				table.getSelection()[0].setText(1, domainEditGUI.getDomain());
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		cancelButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				shell.dispose();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
	}
	
	public static void main(String[] args) {
		UATPathRangeSettingGUI test = new UATPathRangeSettingGUI();
	}
}

