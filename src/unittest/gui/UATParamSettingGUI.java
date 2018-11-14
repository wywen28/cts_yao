package unittest.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TreeColumn;

import softtest.symboltable.c.Type.CType_Pointer;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;

/**
 * @author Cai Min 用户设置参数
 */
public class UATParamSettingGUI {
    private UATGUI uatGui;
    private Display display;
    private Shell shell;

    private UATTestCaseInputTableGUI tableGUI1;
    private UATTestCaseInputTableGUI tableGUI2;
    private UATTestCaseInputTableGUI tcTableGUI;

    private Composite parent;
    private Composite topComposite;
    private Composite bottomComposite;
    private Composite paramComposite;
    private Composite valueComposite;
    private Button okButton;
    private Button cancelButton;
    private Button generateButton;
    private Button editButton;

    private UATTestCaseTree paramTree1;
    private UATTestCaseTree paramTree2;

    private Composite charCheckComp;
    private Button charCheckButton;
    Spinner spinner;
    Combo combo;

    public UATParamSettingGUI(UATGUI uatGui) {
        this.uatGui = uatGui;
        uatGui.getShell().setEnabled(false);
        display = Display.getDefault();
        createShell();
        shell.open();

        while (!display.isDisposed())
            if (!display.readAndDispatch())
                display.sleep();
        display.dispose();
    }

    public UATParamSettingGUI(UATGUI uatGui, Composite parent) {
        this.uatGui = uatGui;
        uatGui.getShell().setEnabled(false);
        this.parent = parent;
        parent.setLayout(new FormLayout());
        createTopComposite();
        createBottomComposite();
    }

    public void setTcTable(UATTestCaseInputTableGUI tcTable) {
        tcTableGUI = tcTable;
    }

    private void createShell() {
        shell = new Shell(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL | SWT.MAX | SWT.MIN);
        shell.setText(GUILanguageResource.getProperty("setParamDomainAndValueItem"));
        shell.setImage(Resource.UATImage);
        shell.setBounds(50, 50, 850, 700);
        shell.setLayout(new FormLayout());
        shell.setMaximized(false);
        LayoutUtil.centerShell(display, shell);
        createCharCheckComposite();
        createTopComposite();
        createBottomComposite();
    }

    private void createCharCheckComposite() {
        charCheckComp = WidgetFactory.createComposite(shell, SWT.BORDER);
        WidgetFactory.configureFormData(charCheckComp, new FormAttachment(0, 5), new FormAttachment(0, 0), new FormAttachment(100, -5), new FormAttachment(0, 35));
        charCheckComp.setLayout(new FormLayout());
        charCheckButton = WidgetFactory.createButton(charCheckComp, SWT.CHECK);
        charCheckButton.setText("以ASCII码形式输入字符");
        charCheckButton.setSelection(true);
        WidgetFactory.configureFormData(charCheckButton, null, new FormAttachment(0, 5), new FormAttachment(100, -5), null);
        charCheckButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (charCheckButton.getSelection()) {
                    tableGUI1.charToAscii = true;
                    tableGUI2.charToAscii = true;
                } else {
                    tableGUI1.charToAscii = false;
                    tableGUI2.charToAscii = false;
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {}
        });
    }

    private void createTopComposite() {
        if (shell != null)
            topComposite = WidgetFactory.createComposite(shell, SWT.NONE);
        else
            topComposite = WidgetFactory.createComposite(parent, SWT.NONE);
        topComposite.setLayout(new FormLayout());
        WidgetFactory.configureFormData(topComposite, new FormAttachment(0, 5), new FormAttachment(charCheckComp), new FormAttachment(100, -3), new FormAttachment(100, -40));

        generateButton = WidgetFactory.createButton(topComposite, SWT.PUSH);
        WidgetFactory.configureFormData(generateButton, new FormAttachment(50, -5), new FormAttachment(0, 150), new FormAttachment(50, 70), null);
        generateButton.setText(GUILanguageResource.getProperty("generate"));

        editButton = WidgetFactory.createButton(topComposite, SWT.PUSH);
        WidgetFactory.configureFormData(editButton, new FormAttachment(50, -5), new FormAttachment(generateButton, -100), new FormAttachment(50, 70), null);
        editButton.setText(GUILanguageResource.getProperty("edit"));

        createParamComposite();
        createValueComposite();
    }

    private void createParamComposite() {
        paramComposite = WidgetFactory.createComposite(topComposite, SWT.NONE);
        paramComposite.setLayout(new FillLayout());
        WidgetFactory.configureFormData(paramComposite, new FormAttachment(0, 0), new FormAttachment(0, 3), new FormAttachment(generateButton, -15), new FormAttachment(100, -3));

        tableGUI1 = new UATTestCaseInputTableGUI(paramComposite, uatGui);
        tableGUI1.usage = 2;
        paramTree1 = tableGUI1.getTableTree();
        tableGUI1.createContents();
    }

    private void createValueComposite() {
        valueComposite = WidgetFactory.createComposite(topComposite, SWT.NONE);
        valueComposite.setLayout(new FillLayout());
        WidgetFactory.configureFormData(valueComposite, new FormAttachment(generateButton, 15), new FormAttachment(0, 3), new FormAttachment(100, -3), new FormAttachment(100, -3));

        tableGUI2 = new UATTestCaseInputTableGUI(valueComposite, uatGui);
        tableGUI2.usage = 3;
        paramTree2 = tableGUI2.getTableTree();
        tableGUI2.createContents();
    }

    private void createBottomComposite() {
        bottomComposite = WidgetFactory.createComposite(parent, SWT.BORDER);
        WidgetFactory.configureFormData(bottomComposite, new FormAttachment(0), new FormAttachment(topComposite), new FormAttachment(100), new FormAttachment(100));
        bottomComposite.setLayout(new FormLayout());

        Label label = new Label(bottomComposite, SWT.NONE);
        WidgetFactory.configureFormData(label, new FormAttachment(0, 5), new FormAttachment(0, 8), null, null);
        label.setText("每次生成用例个数(<100):");

        spinner = new Spinner(bottomComposite, SWT.BORDER);
        WidgetFactory.configureFormData(spinner, new FormAttachment(label, 10), new FormAttachment(0, 8), null, null);
        spinner.setMinimum(1);
        spinner.setMaximum(100);
        spinner.setIncrement(1);

        Label listLabel = new Label(bottomComposite, SWT.NONE);
        WidgetFactory.configureFormData(listLabel, new FormAttachment(spinner, 25), new FormAttachment(0, 8), null, null);
        listLabel.setText("用例生成策略:");

        combo = new Combo(bottomComposite, SWT.DROP_DOWN | SWT.V_SCROLL);
        WidgetFactory.configureFormData(combo, new FormAttachment(listLabel, 10), new FormAttachment(0, 8), null, null);
        combo.add("全区间取值");
        combo.add("区间内取值");
        combo.add("区间边界取值");
        combo.select(0);

        okButton = new Button(bottomComposite, SWT.NONE);
        WidgetFactory.configureFormData(okButton, new FormAttachment(100, -250), new FormAttachment(0, 8), null, null);
        okButton.setText("      复位      ");

        cancelButton = new Button(bottomComposite, SWT.NONE);
        WidgetFactory.configureFormData(cancelButton, new FormAttachment(okButton, 25), new FormAttachment(0, 8), null, null);
        cancelButton.setText("      应用      ");
        cancelButton.setEnabled(false);
    }

    public void dealEvent() {
        okButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                // 重置ParamComposite
                tableGUI1.getTableTree().removeAll();
                // 删除取值区间配置文件
                tableGUI1.deleteValueFile();
                boolean isParam = true;
                UATTestCaseTreeItem parent;
                parent = new UATTestCaseTreeItem(tableGUI1.getTableTree(), SWT.NONE, null, null);
                parent.setText("参数");
                tableGUI1.createItems(parent, isParam);
                parent = new UATTestCaseTreeItem(tableGUI1.getTableTree(), SWT.NONE, null, null);
                parent.setText("全局变量");
                tableGUI1.createItems(parent, !isParam);
                // 重置ValueComposite
                tableGUI2.getTableTree().removeAll();
                tableGUI2.getTableTree().redraw();
                isParam = true;
                parent = new UATTestCaseTreeItem(tableGUI2.getTableTree(), SWT.NONE, null, null);
                parent.setText("参数");
                tableGUI2.createItems(parent, isParam);
                parent = new UATTestCaseTreeItem(tableGUI2.getTableTree(), SWT.NONE, null, null);
                parent.setText("全局变量");
                tableGUI2.createItems(parent, !isParam);
                // 重置生成测试用例个数按钮
                spinner.setSelection(0);
                // 置应用按钮为无效
                cancelButton.setEnabled(false);
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {}
        });

        cancelButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                // 添加新列
                int columnNo = tcTableGUI.getTableTree().getColumns().length;
                for (int i = 1; i <= spinner.getSelection(); i++) {
                    TreeColumn column = new TreeColumn(tcTableGUI.getTableTree(), SWT.CENTER);
                    column.setWidth(120);
                    column.setText(Integer.toString(columnNo++));
                    tcTableGUI.addColumnLength();
                    tcTableGUI.layout();
                }
                copyValue(paramTree2.getItems(), tcTableGUI.getTableTree().getItems(), spinner.getSelection());
                // expand the value tree add by chenruolin
                expand_paramTree(tcTableGUI.getTableTree().getItems());
                cancelButton.setEnabled(false);
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {}
        });

        generateButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                int TestCaseNum = Integer.parseInt(spinner.getText());
                // 判断spinner设置的值是否大于该生成策略下可以生成的最大用例个数
                long maxNum = countMaxNum(paramTree1.getItems(), 1, TestCaseNum);
                if (maxNum < TestCaseNum) {
                    MessageBox msgBox = new MessageBox(paramComposite.getShell(), SWT.OK | SWT.ICON_WARNING | SWT.ON_TOP);
                    msgBox.setText("提示");
                    msgBox.setMessage("超出生成策略能生成的测试用例数" + maxNum + "，请修改生成测试用例个数！");
                    msgBox.open();
                } else {
                    // 重建生成值表格
                    boolean isParam = true;
                    UATTestCaseTreeItem parent;
                    int columnLength = tableGUI2.getTableTree().getColumns().length;
                    while (columnLength > 1) {
                        tableGUI2.getTableTree().getColumn(columnLength - 1).dispose();
                        columnLength--;
                    }
                    paramTree2.removeAll();
                    parent = new UATTestCaseTreeItem(tableGUI2.getTableTree(), SWT.NONE, null, null);
                    parent.setText("参数");
                    tableGUI2.createItems(parent, isParam);
                    parent.setExpanded(true);
                    parent = new UATTestCaseTreeItem(tableGUI2.getTableTree(), SWT.NONE, null, null);
                    parent.setText("全局变量");
                    tableGUI2.createItems(parent, !isParam);
                    parent.setExpanded(true);
                    for (int i = 1; i <= spinner.getSelection(); i++) {
                        TreeColumn column = new TreeColumn(paramTree2, SWT.CENTER);
                        column.setText(String.valueOf(i));
                        column.setWidth(100);
                        setValue(paramTree1.getItems(), paramTree2.getItems(), i);
                        while (checkExist(paramTree2.getItems(), i))
                            setValue(paramTree1.getItems(), paramTree2.getItems(), i);
                    }
                    // expand the value tree add by chenruolin
                    expand_paramTree(paramTree2.getItems());
                    // 置应用按钮为有效
                    cancelButton.setEnabled(true);
                }
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {}

            private long countMaxNum(UATTestCaseTreeItem[] items, long maxNum, int TestCaseNum) {
                if (combo.getItem(combo.getSelectionIndex()).equals("区间边界取值")) {
                    for (int i = 0, count = 0; i < items.length; i++, count = 0) {
                        String domain = items[i].getText();
                        if (domain.equals("参数") || domain.equals("全局变量") || items[i].getText(1).equals(""))
                            maxNum = countMaxNum(items[i].getItems(), maxNum, TestCaseNum);
                        else {
                            domain = items[i].getText(1);
                            while (!domain.equals("")) {
                                int loc = domain.indexOf("]");
                                count = count + 2;
                                domain = domain.substring(loc + 1);
                            }
                            if (maxNum < TestCaseNum || count == 0)
                                maxNum = maxNum * count;
                        }
                    }
                } else {
                    long count = 0;
                    for (int i = 0; i < items.length; i++, count = 0) {
                        if (items[i].getText().equals("参数") || items[i].getText().equals("全局变量") || items[i].getText(1).equals(""))
                            maxNum = countMaxNum(items[i].getItems(), maxNum, TestCaseNum);
                        else {
                            String domain = items[i].getText(1);
                            while (!domain.equals("")) {
                                int loc = domain.indexOf("]");
                                String tempDomain = domain.substring(0, loc);
                                if (tempDomain.startsWith(","))
                                    tempDomain = tempDomain.substring(1);
                                long left, right;
                                if (items[i].getText().contains("float") || items[i].getText().contains("double")) {
                                    try {
                                        String str = tempDomain.substring(1, tempDomain.indexOf(","));
                                        str = str.substring(0, str.indexOf('.')) + str.substring(str.indexOf('.') + 1);
                                        left = Long.parseLong(str);
                                        str = tempDomain.substring(tempDomain.indexOf(",") + 1);
                                        str = str.substring(0, str.indexOf('.')) + str.substring(str.indexOf('.') + 1);
                                        right = Long.parseLong(str);
                                    } catch (Exception e) {
                                        MessageBox msgBox = new MessageBox(paramComposite.getShell(), SWT.OK | SWT.ICON_WARNING | SWT.ON_TOP);
                                        msgBox.setText("错误");
                                        msgBox.setMessage("取值范围超过能处理最大范围，请减小边界值！");
                                        msgBox.open();
                                        return -1;
                                    }
                                } else {
                                    left = Long.parseLong(tempDomain.substring(1, tempDomain.indexOf(",")));
                                    right = Long.parseLong(tempDomain.substring(tempDomain.indexOf(",") + 1));
                                }
                                if (combo.getItem(combo.getSelectionIndex()).equals("区间内取值"))
                                    count = count + right - left - 1;
                                else
                                    count = count + right - left + 1;
                                domain = domain.substring(loc + 1);
                            }
                            if (maxNum < TestCaseNum || count == 0)
                                maxNum = maxNum * count;
                        }
                    }
                }
                return maxNum;
            }
        });

        editButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                UATTestCaseTreeItem[] itemSelected = tableGUI1.getTableTree().getSelection();
                String type = null;
                if (itemSelected.length > 0) {
                    type = itemSelected[0].getText();
                    if (type.lastIndexOf(' ') >= 0)
                        type = type.substring(0, type.lastIndexOf(' '));
                }
                if (itemSelected.length == 0 || itemSelected[0].getText(1).equals("") || type == null) {
                    MessageBox msgBox = new MessageBox(paramComposite.getShell(), SWT.OK | SWT.ICON_WARNING);
                    msgBox.setText("提示");
                    msgBox.setMessage("请先选择需要编辑的区间");
                    msgBox.open();
                } else if (type.contains("bool") || type.contains("Bool") || type.contains("enum")) {
                    MessageBox msgBox = new MessageBox(paramComposite.getShell(), SWT.OK | SWT.ICON_WARNING);
                    msgBox.setText("提示");
                    msgBox.setMessage("此类型不提供编辑");
                    msgBox.open();
                } else {
                    // String domainType = itemSelected[0].getText();
                    String domainType = itemSelected[0].getCType().getName();
                    int to = domainType.lastIndexOf(" ");
                    if (to > 0) {
                        domainType = domainType.substring(0, domainType.lastIndexOf(" "));
                    }
                    UATDomainEditGUI domainEditGUI = new UATDomainEditGUI(itemSelected[0].getText(1), domainType);
                    itemSelected[0].setText(1, domainEditGUI.getDomain());
                    saveDomain(itemSelected[0].getText(), domainEditGUI.getDomain());
                }
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {}
        });

        spinner.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent arg0) {
                if (!spinner.getText().equals("")) {
                    int value = Integer.parseInt(spinner.getText());
                    if (value > 100) {
                        spinner.setSelection(0);
                        MessageBox msgBox = new MessageBox(paramComposite.getShell(), SWT.OK | SWT.ICON_WARNING);
                        msgBox.setText("提示");
                        msgBox.setMessage("超出最大值100，重置为1");
                        msgBox.open();
                    }
                }
            }
        });
    }

    private void expand_paramTree(UATTestCaseTreeItem[] currentTreeItems) {
        for (int i = 0; i < currentTreeItems.length; i++) {
            currentTreeItems[i].setExpanded(true);
            if (currentTreeItems[i].getItems().length > 1)
                expand_paramTree(currentTreeItems[i].getItems());
        }
    }

    private void setValue(UATTestCaseTreeItem[] items1, UATTestCaseTreeItem[] items2, int loc) {
        for (int i = 0; i < items1.length; i++) {
            if (items1[i].getItems().length != 0 || items1[i].getText().equals("参数") || items1[i].getText().equals("全局变量") || items1[i].getText(1).equals("")) {
                setValue(items1[i].getItems(), items2[i].getItems(), loc);
                if (items2[i].getParentItem() != null && items2[i].getParentItem().getCType() instanceof CType_Pointer && !(items2[0].getCType() instanceof CType_Pointer))
                    items2[i].setText(loc, "NULL");
            } else {
                String type = items1[i].getCType().toString();
                if (items1[i].getText(1).isEmpty())
                    continue;
                // 新建参数区间的arraylist,读入区间
                ArrayList<String> domainArray = new ArrayList<String>();
                String temp = items1[i].getText(1);
                while (!temp.equals("")) {
                    int count = temp.indexOf("]");
                    String seperateDomain = temp.substring(0, count + 1);
                    if (seperateDomain.startsWith(","))
                        seperateDomain = seperateDomain.substring(1);
                    domainArray.add(seperateDomain.substring(1, seperateDomain.indexOf(",")));
                    domainArray.add(seperateDomain.substring(seperateDomain.indexOf(",") + 1, seperateDomain.length() - 1));
                    temp = temp.substring(count + 1);
                }
                // 根据生成策略随机生成用例
                if (combo.getItem(combo.getSelectionIndex()).equals("区间边界取值")) {
                    int position = (int) (Math.random() * domainArray.size());
                    items2[i].setText(loc, domainArray.get(position));
                } else if (combo.getItem(combo.getSelectionIndex()).equals("区间内取值")) {
                    int domainNo = (int) (Math.random() * (domainArray.size() / 2)); // 随机选择一段区间
                    boolean leftNegtive = false;
                    boolean rightNegtive = false;
                    String leftDomainString = domainArray.get(domainNo * 2);
                    String rightDomainString = domainArray.get(domainNo * 2 + 1);
                    if (leftDomainString.startsWith("-") || leftDomainString.startsWith("+")) {
                        if (leftDomainString.startsWith("-"))
                            leftNegtive = true;
                        leftDomainString = leftDomainString.substring(1);
                    }
                    if (rightDomainString.startsWith("-") || rightDomainString.startsWith("+")) {
                        if (rightDomainString.startsWith("-"))
                            rightNegtive = true;
                        rightDomainString = rightDomainString.substring(1);
                    }
                    if (type.contains("float") || type.contains("double")) {
                        double left = Double.parseDouble(leftDomainString);
                        double right = Double.parseDouble(rightDomainString);
                        if (leftNegtive)
                            left = 0 - left;
                        if (rightNegtive)
                            right = 0 - right;
                        double value = Math.random() * (right - left) + left;
                        String valueString = String.valueOf(value);
                        valueString = valueString.substring(0, valueString.indexOf(".") + 2);
                        if (valueString.equals("-0.0"))
                            valueString = "0.0";
                        while (valueString.equals(leftDomainString) || valueString.equals(rightDomainString)) {
                            value = Math.random() * (right - left) + left;
                            valueString = String.valueOf(value);
                            valueString = valueString.substring(0, valueString.indexOf(".") + 2);
                            if (valueString.equals("-0.0"))
                                valueString = "0.0";
                        }
                        items2[i].setText(loc, valueString);
                    } else if (type.contains("int") || type.contains("long") || type.contains("short") || type.contains("Qualified") || type.contains("char") || type.contains("enum")) {
                        long left = Long.parseLong(leftDomainString);
                        long right = Long.parseLong(rightDomainString);
                        if (leftNegtive)
                            left = 0 - left;
                        if (rightNegtive)
                            right = 0 - right;
                        long value = (long) (Math.random() * (right - left) + left);
                        while (value == left || value == right)
                            value = (long) (Math.random() * (right - left) + left);
                        items2[i].setText(loc, String.valueOf(value));
                    }
                } else {
                    int domainNo = (int) (Math.random() * (domainArray.size() / 2)); // 随机选择一段区间
                    boolean leftNegtive = false;
                    boolean rightNegtive = false;
                    String leftDomainString = domainArray.get(domainNo * 2);
                    String rightDomainString = domainArray.get(domainNo * 2 + 1);
                    if (leftDomainString.startsWith("-") || leftDomainString.startsWith("+")) {
                        if (leftDomainString.startsWith("-"))
                            leftNegtive = true;
                        leftDomainString = leftDomainString.substring(1);
                    }
                    if (rightDomainString.startsWith("-") || rightDomainString.startsWith("+")) {
                        if (rightDomainString.startsWith("-"))
                            rightNegtive = true;
                        rightDomainString = rightDomainString.substring(1);
                    }
                    if (type.contains("float") || type.contains("double")) {
                        double left = Double.parseDouble(leftDomainString);
                        double right = Double.parseDouble(rightDomainString);
                        if (leftNegtive)
                            left = 0 - left;
                        if (rightNegtive)
                            right = 0 - right;
                        double value = Math.random() * (right - left + 0.1) + left;
                        String valueString = String.valueOf(value);
                        valueString = valueString.substring(0, valueString.indexOf(".") + 2);
                        if (valueString.equals("-0.0"))
                            valueString = "0.0";
                        items2[i].setText(loc, valueString);
                    } else if (type.equals("_Bool")) {
                        int value = (int) (Math.random() * 2);
                        items2[i].setText(loc, String.valueOf(value));
                    } else if (type.contains("int") || type.contains("long") || type.contains("short") || type.contains("Qualified") || type.contains("char") || type.contains("enum")) {
                        long left = Long.parseLong(leftDomainString);
                        long right = Long.parseLong(rightDomainString);
                        if (leftNegtive)
                            left = 0 - left;
                        if (rightNegtive)
                            right = 0 - right;
                        long value = (long) (Math.random() * (right - left + 1) + left);
                        items2[i].setText(loc, String.valueOf(value));
                    }
                }
            }
        }
    }

    private boolean checkExist(UATTestCaseTreeItem[] items, int loc) {
        boolean exist = false; // 判断当前这组用例是否已经生成过
        for (int i = 1; !exist && i < loc; i++) { // 逐个测试用例进行检查（列）
            boolean notFindArgumentDifference = true;
            boolean notFindGlobalVariableDifference = true;
            if (items[0].getText().equals("参数"))
                notFindArgumentDifference = checkSame(items[0].getItems(), i, loc);
            if (items[1].getText().equals("全局变量"))
                notFindGlobalVariableDifference = checkSame(items[1].getItems(), i, loc);
            if (notFindArgumentDifference && notFindGlobalVariableDifference)
                exist = true;
        }
        return exist;
    }

    private boolean checkSame(UATTestCaseTreeItem[] items, int compareLoc, int currentLoc) {
        boolean notFindDifference = true; // 判断当前这组用例与正在比较的用例有没有不同之处
        if (items.length == 0)
            notFindDifference = true;
        else {
            for (int j = 0; notFindDifference && j < items.length; j++) { // 对每组用例逐行进行测试
                if (items[j].getText(1).equals(""))
                    notFindDifference = checkSame(items[j].getItems(), compareLoc, currentLoc);
                else {
                    if (!items[j].getText(compareLoc).equals(items[j].getText(currentLoc)))
                        notFindDifference = false;
                }
            }
        }
        return notFindDifference;
    }

    private void saveDomain(String key, String domain) {
        Properties domainProp = new Properties();
        String fileName = uatGui.getCurrentFile().getFile();
        int startloc = fileName.lastIndexOf("\\");
        int endloc = fileName.lastIndexOf(".");
        String filePath = uatGui.getCurrentProject().getPath() + File.separator + "ValueConfig" + File.separator;
        fileName = fileName.substring(startloc + 1, endloc);

        if (!uatGui.getCurrentFile().getHasSetDomain()) {
            // 新建文件夹
            File file = new File(filePath);
            if (!file.exists())
                file.mkdir();
            // 新建文件
            file = new File(filePath + fileName + ".properties");
            try {
                file.createNewFile();
                FileInputStream inputFile = new FileInputStream(file);
                domainProp.load(inputFile);
                inputFile.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else {
            try {
                FileInputStream inputFile = new FileInputStream(filePath + fileName + ".properties");
                domainProp.load(inputFile);
                inputFile.close();
                uatGui.getCurrentFile().setHasSetDomain(true);
            } catch (IOException ex) {
                System.out.println("装载文件--->失败");
                ex.printStackTrace();
            }
        }
        domainProp.setProperty(key, domain);
        FileOutputStream outputFile;
        try {
            outputFile = new FileOutputStream(filePath + fileName + ".properties");
            try {
                domainProp.store(outputFile, null);
                outputFile.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
    }

    private void copyValue(UATTestCaseTreeItem[] items1, UATTestCaseTreeItem[] items2, int columns) {
        for (int i = 0; i < items1.length; i++) {
            if (items1[i].getItems().length != 0 || items1[i].getText().equals("参数") || items1[i].getText().equals("全局变量"))
                copyValue(items1[i].getItems(), items2[i].getItems(), columns);
            else {
                if (items1[i].getText(1).equals(""))
                    return;
                // 复制数据
                int loc = tcTableGUI.getTableTree().getColumns().length - columns - 1;
                while (items2[i].getText(loc) != "") {
                    loc++;
                }
                for (int j = 1; j <= columns; j++) {
                    items2[i].setText(loc, items1[i].getText(j));
                    loc++;
                }
            }
        }
    }
}
