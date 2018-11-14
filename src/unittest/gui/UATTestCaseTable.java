package unittest.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_Array;
import softtest.symboltable.c.Type.CType_BaseType;
import softtest.symboltable.c.Type.CType_Pointer;
import softtest.symboltable.c.Type.CType_Qualified;
import softtest.symboltable.c.Type.CType_Struct;
import softtest.symboltable.c.Type.CType_Typedef;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.module.seperate.TestModule;
import unittest.testcase.generate.paramtype.AbstractParamValue;
import unittest.testcase.generate.paramtype.ArrayParamValue;
import unittest.testcase.generate.paramtype.EnumParamValue;
import unittest.testcase.generate.paramtype.FunctionParamValue;
import unittest.testcase.generate.paramtype.PointerParamValue;
import unittest.testcase.generate.paramtype.PrimitiveParamValue;
import unittest.testcase.generate.paramtype.StructParamValue;
import unittest.testcase.generate.paramtype.TypeDefParamValue;
import unittest.testcase.generate.util.TestCaseNew;
import unittest.testcase.management.news.TestCaseLibManagerNew;
import unittest.util.ASCIITranslator;
import unittest.util.Config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * @author Cai Min �ڽ�����ʾ����������
 */
public class UATTestCaseTable {
    private Logger logger = Logger.getLogger(this.getClass());
    private UATTestCaseTree tableTree;
    private Button passButton = null;
    private Composite composite;
    private UATGUI uatGui;
    private TestCaseNew testCase;
    public boolean showAllContents = true; // �Ƿ���ʾ����ֵ�����Ƿ�ʽ����Ϣ
    private int countMark = 1;
    private boolean structBegin = false;
    private TestModule tm;

    private ArrayList<VariableNameDeclaration> paramList;
    private ArrayList<VariableNameDeclaration> globalList;
    private CType returnType;
    private Map<String, UATTestCaseTreeItem> nodeAddrMap;
    private UATTestCaseTreeItem sameNodeItem = null;
    private UATTestCaseTreeItem paramItem;
    private UATTestCaseTreeItem globalItem;
    private UATTestCaseTreeItem consoleExpectOutputItem;
    private UATTestCaseTreeItem consoleOutputItem;
    private UATTestCaseTreeItem fileInputItem;
    private UATTestCaseTreeItem fileOutputItem;
    private UATTestCaseTreeItem expectRetItem;
    private UATTestCaseTreeItem socketSendItem;
    private UATTestCaseTreeItem socketRecItem;
    private UATTestCaseTreeItem retItem;
    private UATTestCaseTreeItem coverRuleItem;
    private UATTestCaseTreeItem passItem;

    public UATTestCaseTable(Composite composite, UATGUI uatGui) {
        tableTree = new UATTestCaseTree(composite, SWT.FULL_SELECTION | SWT.BORDER);
        tableTree.setHeaderVisible(true);
        tableTree.setLinesVisible(true);
        this.composite = composite;
        this.uatGui = uatGui;
        nodeAddrMap = new HashMap<String, UATTestCaseTreeItem>();
    }

    private void clear() {
        this.tableTree.dispose();
        this.tableTree = new UATTestCaseTree(composite, SWT.FULL_SELECTION | SWT.BORDER);
        tableTree.setHeaderVisible(true);
        tableTree.setLinesVisible(true);
    }

    // ����columnLength��varList
    private void setup(TestCaseNew testCase) {

        tm = uatGui.getCurrentFunc();
        if (tm == null)
            return;

        paramList = uatGui.getCurrentFunc().getFuncVar().getParamVar();
        globalList = uatGui.getCurrentFunc().getFuncVar().getGlobalVar();

        returnType = uatGui.getCurrentFunc().getFuncVar().getReturnType();
    }

    public int doAutoCompare(int col, TreeItem[] expectRetItems1, TreeItem[] retItems1) {
        // return 1 for correct; return -1 for false; return 0 for not having expectRet or actualRet
        if (expectRetItems1.length != retItems1.length)
            return -1;
        for (int i = 0; i <= expectRetItems1.length - 1 && i <= retItems1.length - 1; i++) {
            if (!expectRetItems1[i].getText(col).equals(retItems1[i].getText(col)))
                return -1;
            if (expectRetItems1[i].getItems().length > 0)
                doAutoCompare(col, expectRetItems1[i].getItems(), retItems1[i].getItems());
        }
        return 1;
    }

    /**
     * �յĲ�����������ʾ
     * 
     * created by Yaoweichang on 2015-11-17 ����11:33:35
     */
    public void createPreContents() {
        clear();
        TreeColumn column1 = new TreeColumn(tableTree, SWT.CENTER);
        column1.setText("������");
        column1.setWidth(150);

        final TreeColumn column2 = new TreeColumn(tableTree, SWT.CENTER);
        column2.setText("����ֵ");
        column2.setWidth(610);

        boolean isParam = true;
        // ��ʾ������ֵ
        paramItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
        paramItem.setText(0, "����");

        globalItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
        globalItem.setText(0, "ȫ�ֱ���");


        consoleExpectOutputItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
        consoleExpectOutputItem.setText(0, "Ԥ�ڿ���̨���");

        consoleOutputItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
        consoleOutputItem.setText(0, "ʵ�ʿ���̨���");

        fileInputItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
        fileInputItem.setText(0, "д���ļ�������");

        fileOutputItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
        fileOutputItem.setText(0, "��ȡ�ļ�������");

        socketSendItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
        socketSendItem.setText(0, "socket��������");

        socketRecItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
        socketRecItem.setText(0, "socket��������");

        if (showAllContents) {
            boolean isActualRet = true;
            // ��ʾԤ�ڷ���ֵ
            expectRetItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
            expectRetItem.setText(0, "Ԥ�ڷ���ֵ");

            // ��ʾ����ֵ
            retItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
            retItem.setText(0, "����ֵ");

            // ��ʾ���Ƿ�ʽ
            coverRuleItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
            coverRuleItem.setText(0, "���Ƿ�ʽ");
            UATTestCaseTreeItem blockRuleItem = new UATTestCaseTreeItem(coverRuleItem, SWT.NONE, null, null);
            blockRuleItem.setText("��串��");
            UATTestCaseTreeItem branchRuleItem = new UATTestCaseTreeItem(coverRuleItem, SWT.NONE, null, null);
            branchRuleItem.setText("��֧����");
            UATTestCaseTreeItem mcdcRuleItem = new UATTestCaseTreeItem(coverRuleItem, SWT.NONE, null, null);
            mcdcRuleItem.setText("MC/DC����");
            coverRuleItem.setExpanded(true);

            passItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
            passItem.setText(0, "ͨ��");
        }
        composite.layout();
    }

    public void createContents(TestCaseNew testCaseNew) {
        testCase = testCaseNew;
        clear();
        setup(testCase);

        TreeColumn column1 = new TreeColumn(tableTree, SWT.CENTER);
        column1.setText("������");
        column1.setWidth(150);

        // �����Զ���������������ע�빦��
        if (Config.IsMutationTesting)// �ж��Ƿ����������ʵ��
        {
            column1.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event event) {
                    // �������ݿ�����������ʵ�ʷ���ֵ��ʵ�ʿ���̨����Ԥ����
                    TestCaseLibManagerNew.replaceTestCase();
                }
            });
        }
        final TreeColumn column2 = new TreeColumn(tableTree, SWT.CENTER);
        column2.setText("����ֵ");
        column2.setWidth(610);

        boolean isParam = true;
        // ��ʾ������ֵ
        paramItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
        paramItem.setText(0, "����");
        createItems(paramItem, isParam);
        addValues(isParam);

        globalItem = null;
        if (testCase.getGlobalParamList().size() != 0) {
            globalItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
            globalItem.setText(0, "ȫ�ֱ���");
            createItems(globalItem, !isParam);
            addValues(!isParam);
        }

        consoleExpectOutputItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
        consoleExpectOutputItem.setText(0, "Ԥ�ڿ���̨���");
        createItems(consoleExpectOutputItem, 0);
        addValues(consoleExpectOutputItem, 0);

        consoleOutputItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
        consoleOutputItem.setText(0, "ʵ�ʿ���̨���");
        createItems(consoleOutputItem, 1);
        addValues(consoleOutputItem, 1);

        fileInputItem = null;
        if (testCase.getFileInput().size() != 0) {
            fileInputItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
            fileInputItem.setText(0, "д���ļ�������");
            createItems(fileInputItem, 2);
            addValues(fileInputItem, 2);
        }
        fileOutputItem = null;
        if (testCase.getFileOutput().size() != 0) {
            fileOutputItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
            fileOutputItem.setText(0, "��ȡ�ļ�������");
            createItems(fileOutputItem, 3);
            addValues(fileOutputItem, 3);
        }
        socketSendItem = null;
        if (testCase.getSocketSend().size() != 0) {
            socketSendItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
            socketSendItem.setText(0, "socket��������");
            createItems(socketSendItem, 4);
            addValues(socketSendItem, 4);
        }
        socketRecItem = null;
        if (testCase.getSocketRec().size() != 0) {
            socketRecItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
            socketRecItem.setText(0, "socket��������");
            createItems(socketRecItem, 5);
            addValues(socketRecItem, 5);
        }

        // �ع����ʱʹ�� add by Yaoweichang
        if (showAllContents) {
            boolean isActualRet = true;
            // ��ʾԤ�ڷ���ֵ
            expectRetItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
            expectRetItem.setText(0, "Ԥ�ڷ���ֵ");
            createRetValItem(expectRetItem, !isActualRet);
            addRetVal(!isActualRet);

            // ��ʾ����ֵ
            retItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
            retItem.setText(0, "����ֵ");
            createRetValItem(retItem, isActualRet);
            addRetVal(isActualRet);

            // ��ʾ���Ƿ�ʽ
            coverRuleItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
            coverRuleItem.setText(0, "���Ƿ�ʽ");
            UATTestCaseTreeItem blockRuleItem = new UATTestCaseTreeItem(coverRuleItem, SWT.NONE, null, null);
            blockRuleItem.setText("��串��");
            UATTestCaseTreeItem branchRuleItem = new UATTestCaseTreeItem(coverRuleItem, SWT.NONE, null, null);
            branchRuleItem.setText("��֧����");
            UATTestCaseTreeItem mcdcRuleItem = new UATTestCaseTreeItem(coverRuleItem, SWT.NONE, null, null);
            mcdcRuleItem.setText("MC/DC����");
            coverRuleItem.setExpanded(true);
            if (testCase != null) {
                String coverRule = uatGui.getCurrentCoverCriteria().toString();

                if (coverRule.contains("��串��"))
                    blockRuleItem.setText(1, "��");
                else
                    blockRuleItem.setText(1, "-");
                if (coverRule.contains("��֧����"))
                    branchRuleItem.setText(1, "��");
                else
                    branchRuleItem.setText(1, "-");
                if (coverRule.contains("MC/DC����"))
                    mcdcRuleItem.setText(1, "��");
                else
                    mcdcRuleItem.setText(1, "-");

            }
            passItem = new UATTestCaseTreeItem(tableTree, SWT.NONE, null, null);
            passItem.setText(0, "ͨ��");
            addPassButton();
        }
        uatGui.setSelectedTestCaseItemEnabled(true);

        addTextEditor();// �ɱ༭����


        if (Config.IsMutationTesting) {
            // add by chenruolin �Զ���ʶ������ȷ��
            if (expectRetItem != null && retItem != null && consoleExpectOutputItem != null && consoleOutputItem != null) {
                if ((doAutoCompare(1, expectRetItem.getItems(), retItem.getItems()) == 1) && (doAutoCompare(1, consoleExpectOutputItem.getItems(), consoleOutputItem.getItems()) == 1)) {
                    passButton.setText("  ��  ");
                    uatGui.map.put(uatGui.getCurrentTestCaseID(), 1);
                } else {
                    if ((doAutoCompare(1, expectRetItem.getItems(), retItem.getItems()) == -1) || (doAutoCompare(1, consoleExpectOutputItem.getItems(), consoleOutputItem.getItems()) == -1)) {
                        passButton.setText("  ��  ");
                        uatGui.map.put(uatGui.getCurrentTestCaseID(), 2);
                    }
                }
                uatGui.refreshTestCasesTree();
            }
        }

        // enable buglink button
        if (getFaultTCID().isEmpty()) {
            uatGui.setBugLinkMenuItemEnabled(false);
            uatGui.setBugLinkToolItemEnabled(false);
        } else {
            uatGui.setBugLinkMenuItemEnabled(true);
            uatGui.setBugLinkToolItemEnabled(true);
        }

        UATTestCaseTreeItem[] items = tableTree.getItems();
        setEditableColor(items);

        composite.layout();
    }

    /**
     * ��ʶ���ɵĲ���������ȷ��
     * 
     * created by Yaoweichang on 2015-04-17 ����3:07:31
     */
    private void addPassButton() {
        UATTestCaseTreeItem item = null;
        for (UATTestCaseTreeItem temp : tableTree.getItems())
            if (temp.getText(0).equals("ͨ��"))
                item = temp;

        passButton = new Button(tableTree, SWT.NULL);
        TreeEditor editor = new TreeEditor(tableTree);
        switch (uatGui.map.get(uatGui.getCurrentTestCaseID())) {
            case 1: {
                passButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
                passButton.setText("  ��  ");
                break;
            }
            case 2: {
                passButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
                passButton.setText("  ��  ");
                break;
            }
            case 3: {
                passButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW));
                passButton.setText("  ?  ");
                break;
            }
            default:
                passButton.setText("      ");

        }

        passButton.setToolTipText(GUILanguageResource.getProperty("tcLibPassButtonToolTip"));
        passButton.pack();
        editor.minimumWidth = passButton.getSize().x;
        editor.horizontalAlignment = SWT.CENTER;
        editor.setEditor(passButton, item, 1);

        passButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (passButton.getText().equals("      ")) {
                    passButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
                    passButton.setText("  ��  ");
                    uatGui.map.put(uatGui.getCurrentTestCaseID(), 1);
                } else if (passButton.getText().equals("  ��  ")) {
                    passButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
                    passButton.setText("  ��  ");
                    uatGui.map.put(uatGui.getCurrentTestCaseID(), 2);
                } else if (passButton.getText().equals("  ��  ")) {
                    passButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW));
                    passButton.setText("  ?  ");
                    uatGui.map.put(uatGui.getCurrentTestCaseID(), 3);
                } else if (passButton.getText().equals("  ?  ")) {
                    passButton.setText("      ");
                    uatGui.map.put(uatGui.getCurrentTestCaseID(), 0);
                }

                if (getFaultTCID().isEmpty()) {
                    uatGui.setBugLinkMenuItemEnabled(false);
                    uatGui.setBugLinkToolItemEnabled(false);
                } else {
                    uatGui.setBugLinkMenuItemEnabled(true);
                    uatGui.setBugLinkToolItemEnabled(true);
                }

                uatGui.refreshTestCasesTree();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {}
        });
    }

    // �����пɱ༭������
    private void addTextEditor() {
        final TreeEditor editor = new TreeEditor(tableTree);
        tableTree.addMouseListener(new MouseAdapter() {
            public void mouseDown(MouseEvent event) {
                if (event.button == 1) {
                    Point pt = new Point(event.x, event.y);
                    final UATTestCaseTreeItem item = tableTree.getItem(pt); // �����ѡ�е���
                    if (item != null && (item.getText().contains("��") || item.getText().contains("��"))) {
                        System.out.println(item.getText(1));
                        Shell mShell = new Shell(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL);
                        mShell.setImage(Resource.UATImage);
                        mShell.setText(item.getText() + "����");
                        mShell.setLayout(new FillLayout());
                        mShell.setSize(new org.eclipse.swt.graphics.Point(400, 230));
                        int width = mShell.getMonitor().getClientArea().width;
                        int height = mShell.getMonitor().getClientArea().height;
                        int x = mShell.getSize().x;
                        int y = mShell.getSize().y;
                        if (x > width) {
                            mShell.getSize().x = width;
                        }
                        if (y > height) {
                            mShell.getSize().y = height;
                        }
                        mShell.setLocation((width - x) / 2, (height - y) / 2);


                        Text outPutText = new Text(mShell, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
                        outPutText.setLayoutData(new GridData(GridData.FILL_BOTH));
                        outPutText.setText(item.getText(1));
                        outPutText.pack();
                        mShell.open();
                    }
                    // �����item������,ȷ��ֻ��Ԥ�ڷ���ֵ���Ա༭
                    UATTestCaseTreeItem ancestor = item;
                    while (ancestor != null && ancestor.getParentItem() != null)
                        ancestor = ancestor.getParentItem();
                    if (ancestor == expectRetItem && ancestor != item) {

                        // ����������
                        int column = 1;

                        // ֻ�и����й���TestCase��Ԥ�ڷ���ֵ�ɱ༭
                        /*
                         * if (!(column > 0 && passButton[column - 1].isEnabled()))
                         * return;
                         */

                        boolean showBorder = true;
                        final Composite composite = new Composite(tableTree, SWT.NONE);
                        final Text text = new Text(composite, SWT.NONE | SWT.CENTER);
                        final int inset = showBorder ? 1 : 0;
                        composite.addListener(SWT.Resize, new Listener() {
                            public void handleEvent(Event e) {
                                Rectangle rect = composite.getClientArea();
                                text.setBounds(rect.x + inset, rect.y + inset, rect.width - inset * 2, rect.height - inset * 2);
                            }
                        });

                        final int col = column;
                        final UATTestCaseTreeItem ancestor_ = ancestor;
                        Listener textListener = new Listener() {
                            public void handleEvent(final Event e) {
                                switch (e.type) {
                                    case SWT.FocusOut:
                                        // add by chenruolin
                                        if (!isNumeric(text.getText())) {
                                            MessageBox box = WidgetFactory.createErrorMessageBox(composite.getShell(), "������Ϣ", "�������");
                                            box.open();
                                            composite.dispose();
                                            break;
                                        }
                                        item.setText(col, text.getText());
                                        composite.dispose();
                                        finishInput();
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
                                        rect = tableTree.getClientArea();
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
                                            case SWT.TRAVERSE_ESCAPE:
                                                item.setText(col, text.getText());
                                                // FALL THROUGH
                                                composite.dispose();
                                                e.doit = false;
                                                finishInput();
                                                break;
                                        }
                                        break;
                                }
                            }

                            // add by chenruolin
                            private boolean isNumeric(String str) {
                                if (str.isEmpty())
                                    return false;
                                if (str.startsWith("+") || str.startsWith("-"))
                                    str = str.substring(1);

                                boolean hasDecimalPoint = false; // mark if we already have a
                                                                 // decimal point
                                for (int i = 0; i < str.length(); i++) {
                                    if (!Character.isDigit(str.charAt(i)) && !(str.charAt(i) == '.'))
                                        return false;
                                    else if (str.charAt(i) == '.') {
                                        if (hasDecimalPoint) // the string contains more than one
                                                             // decimal point
                                            return false;
                                        else
                                            hasDecimalPoint = true;

                                        if (str.startsWith("."))
                                            return false;
                                        else if ((str.startsWith("-") || str.startsWith("+")) && str.charAt(1) == '.')
                                            return false;
                                    }
                                }
                                return true;
                            }

                            // �û���������,�Ա�Ԥ�ڷ���ֵ��ʵ�ʷ���ֵ
                            private void finishInput() {
                                try {
                                    if (ancestor_.getItems()[0].getText(col) == null)
                                        return;
                                    judgeRetEqual();
                                } catch (NumberFormatException e) {
                                    MessageBox box = WidgetFactory.createErrorMessageBox(composite.getShell(), "������Ϣ", "�������");
                                    box.open();
                                }
                            }

                            // ��ȡ�û������Ԥ�ڷ���ֵ
                            private void judgeRetEqual() {
                                TestCaseNew tc = testCase;

                                AbstractParamValue expectRet = null;
                                AbstractParamValue acturalRet = tc.getActualReturn();

                                try {
                                    expectRet = getExpectRet(expectRetItem.getItems()[0], acturalRet);
                                } catch (Exception e) {
                                    logger.error("Ԥ�ڷ���ֵ������� " + e.getMessage());
                                    e.printStackTrace();
                                    passButton.setText("��");
                                }

                                if (expectRet == null)
                                    return;

                                // �洢Ԥ��ֵ������������ add by tangrong 2011-09-230
                                TestCaseLibManagerNew.addExpectedReturn(tc.getId(), expectRet);

                                if (expectRet != null && acturalRet.equals(expectRet)) {
                                    passButton.setText("  ��  ");
                                    uatGui.map.put(uatGui.getCurrentTestCaseID(), 1);
                                } else {
                                    passButton.setText("  ��  ");
                                    uatGui.map.put(uatGui.getCurrentTestCaseID(), 2);
                                }

                                uatGui.refreshTestCasesTree();

                                if (getFaultTCID().isEmpty()) {
                                    uatGui.setBugLinkMenuItemEnabled(false);
                                    uatGui.setBugLinkToolItemEnabled(false);
                                } else {
                                    uatGui.setBugLinkMenuItemEnabled(true);
                                    uatGui.setBugLinkToolItemEnabled(true);
                                }

                            }

                            // �õ��û������Ԥ�ڷ���ֵ
                            private AbstractParamValue getExpectRet(UATTestCaseTreeItem item_, AbstractParamValue apv) {
                                UATTestCaseTreeItem[] childItems = item_.getItems();
                                if (childItems.length == 0) {
                                    PrimitiveParamValue acturalRet = (PrimitiveParamValue) apv;
                                    String value = item_.getText(col);
                                    if (!value.equals("-") && !value.equals("") && !value.equalsIgnoreCase("null")) {
                                        value = dealPlus((CType_BaseType) acturalRet.getType(), value);
                                        return new PrimitiveParamValue(acturalRet.getName(), acturalRet.getType(), value);
                                    } else
                                        return null;
                                } else {
                                    if (apv instanceof ArrayParamValue) {
                                        ArrayParamValue acturalRet = (ArrayParamValue) apv;
                                        Map<Integer, AbstractParamValue> memberValue = acturalRet.getMemberValue();
                                        Map<Integer, AbstractParamValue> tempMemValue = new HashMap<Integer, AbstractParamValue>();
                                        AbstractParamValue temp = null;
                                        for (Integer i : memberValue.keySet()) {
                                            temp = memberValue.get(i);
                                            for (UATTestCaseTreeItem childItem : childItems) {
                                                tempMemValue.put(Integer.valueOf(i), getExpectRet(childItem, temp));
                                            }
                                        }
                                        return new ArrayParamValue(acturalRet.getName(), acturalRet.getType(), tempMemValue);
                                    } else if (apv instanceof PointerParamValue) {
                                        PointerParamValue acturalRet = (PointerParamValue) apv;
                                        AbstractParamValue memValue = acturalRet.getMemberValue();
                                        AbstractParamValue tempMemValue = getExpectRet(childItems[0], memValue);
                                        return new PointerParamValue(acturalRet.getName(), acturalRet.getType(), tempMemValue);

                                    } else if (apv instanceof StructParamValue) {
                                        StructParamValue acturalRet = (StructParamValue) apv;
                                        Map<String, AbstractParamValue> memberValue = acturalRet.getMemberValue();
                                        LinkedHashMap<String, AbstractParamValue> tempMemberValue = new LinkedHashMap<String, AbstractParamValue>();
                                        AbstractParamValue temp = null;
                                        for (String i : memberValue.keySet()) {
                                            temp = memberValue.get(i);
                                            CType type = temp.getType();
                                            String typeAndName = temp.getName() + " < " + type.getName() + " > ";
                                            for (UATTestCaseTreeItem childItem : childItems) {
                                                if (childItem.getText(0).equalsIgnoreCase(typeAndName)) {
                                                    tempMemberValue.put(i, getExpectRet(childItem, temp));
                                                    break;
                                                }
                                            }
                                        }
                                        return new StructParamValue(acturalRet.getName(), acturalRet.getType(), tempMemberValue);
                                    }
                                }
                                return null;
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

                } else if (event.button == 3)// add by xujiaoxian
                {
                    Menu menu = WidgetFactory.createMenu(uatGui.getShell(), SWT.POP_UP); // Ϊ�ڵ㽨POP
                                                                                         // UP�˵�

                    MenuItem runSelectedTestcaseItem = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.startImage, SWT.CTRL + 'R', true);
                    runSelectedTestcaseItem.setText(GUILanguageResource.getProperty("RunSelectedTestcases"));
                    runSelectedTestcaseItem.addListener(SWT.Selection, new Listener() {

                        @Override
                        public void handleEvent(Event arg0) {
                            // TODO Auto-generated method stub
                            uatGui.runSelectedTestCases();
                        }
                    });

                    MenuItem bugLinkMenuItem = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.bugLinkImage, SWT.CTRL + 'R', true);
                    bugLinkMenuItem.setEnabled(uatGui.isBugLinkToolItemEnabled());

                    bugLinkMenuItem.setText(GUILanguageResource.getProperty("BugLink"));
                    bugLinkMenuItem.addListener(SWT.Selection, new Listener() {

                        @Override
                        public void handleEvent(Event arg0) {
                            // TODO Auto-generated method stub
                            // ��ӹ��϶�λ�ķ���
                            uatGui.actionsGUI.doBugLink();
                        }
                    });

                    tableTree.setMenu(menu);
                }
            }
        });

    }

    // ��������ֵ�Ĳ�����
    private void createItems(UATTestCaseTreeItem parent, boolean isParam) {
        List<VariableNameDeclaration> varList;
        if (isParam)
            varList = paramList;
        else
            varList = globalList;
        if (varList != null)
            for (VariableNameDeclaration vnd : varList) {
                structBegin = false;
                CType ctype = vnd.getType();
                createChildItem(ctype, parent, vnd);
            }
    }

    private void createItems(UATTestCaseTreeItem parent, int option) {
        int i = this.getLengthofDisplay(option);
        if (option == 1 || option == 0)
            for (int j = 1; j <= i; j++) {
                UATTestCaseTreeItem item = new UATTestCaseTreeItem(parent, SWT.NONE, null, null);
                item.setText("#��" + j + "��");
            }
        else
            for (int j = 1; j <= i; j++) {
                UATTestCaseTreeItem item = new UATTestCaseTreeItem(parent, SWT.NONE, null, null);
                item.setText("#��" + j + "��");
            }
        parent.setExpanded(true);
    }

    private int getLengthofDisplay(int option) {
        int i = 0;
        if (option == 1) {
            if (testCase != null) {
                testCase.setLengthofConsoleOutput(testCase.getConsoleOutput().size());
                if (i < testCase.getLengthofConsoleOutput())
                    i = testCase.getLengthofConsoleOutput();
            }
        }

        else if (option == 2) {
            if (testCase != null) {
                testCase.setLengthofFileInput(testCase.getFileInput().size());
                if (i < testCase.getLengthofFileInput())
                    i = testCase.getLengthofFileInput();
            }
        }

        else if (option == 3) {
            if (testCase != null) {
                testCase.setLengthofFileOutput(testCase.getFileOutput().size());
                if (i < testCase.getLengthofFileOutput())
                    i = testCase.getLengthofFileOutput();
            }
        } else if (option == 0) {
            if (testCase != null) {
                testCase.setLengthofExpectedConsoleOutput(testCase.getExpectedConsoleOutput().size());
                if (i < testCase.getLengofExpectedConsoleOutput())
                    i = testCase.getLengofExpectedConsoleOutput();
            }
        } else if (option == 4) {
            if (testCase != null) {
                testCase.setLengthofSocketSend(testCase.getSocketSend().size());
                if (i < testCase.getLengthofSocketSendData())
                    i = testCase.getLengthofSocketSendData();
            }
        } else if (option == 5) {
            if (testCase != null) {
                testCase.setLengthofSocketRec(testCase.getSocketRec().size());
                if (i < testCase.getLengthofSocketRecData())
                    i = testCase.getLengthofSocketRecData();
            }
        }
        return i;
    }

    // ����ֵisChild������ڵ��ظ��������ڱ�createItems()����ʱ����Ҫ����item���Ժ󱻵ݹ����ʱ��Ҫ������
    private UATTestCaseTreeItem createChildItem(CType ctype, UATTestCaseTreeItem parent, VariableNameDeclaration vnd) {
        UATTestCaseTreeItem item = new UATTestCaseTreeItem(parent, SWT.NONE, ctype, vnd);
        parent.setExpanded(true);
        if (vnd != null) {
            item.setText(0, vnd.getName() + " < " + vnd.getType().getName() + " >");
        } else if (ctype instanceof CType_Struct) {
            if (structBegin) {
                countMark++;
            } else {
                countMark = 1;
                structBegin = true;
            }
            item.setText(0, "<node>" + "(" + countMark + ")");
        } else {
            item.setText(0, ctype.getName());
        }

        if (ctype instanceof CType_BaseType) {
            item.setText(1, "-");// ��ǰ�����޹�ֵ�������������д�����ֵ����Ḳ���޹�ֵ
        } else if (ctype instanceof CType_Qualified) {
            CType originalType = ((CType_Qualified) ctype).getOriginaltype();
            item.setCType(originalType);
        } else if (ctype instanceof CType_Array) {
            CType_Array arrayType = (CType_Array) ctype;
            int len = (int) arrayType.getDimSize();
            if (len == -1) {
                len = Config.dimSize4varLenArr;// ��ֹlenû�б���ʼ��
            }
            CType originalType = arrayType.getOriginaltype();
            for (int i = 0; i < len; i++) {
                UATTestCaseTreeItem _item = createChildItem(originalType, item, null);
                _item.setText(0, _item.getText() + "[" + i + "]");
            }
        } else if (ctype instanceof CType_Pointer) {
            CType_Pointer pointerType = (CType_Pointer) ctype;
            CType originalType = pointerType.getOriginaltype();
            createChildItem(originalType, item, null);
        } else if (ctype instanceof CType_Struct) {
            CType_Struct structType = (CType_Struct) ctype;
            LinkedHashMap<String, CType> fieldType = structType.getfieldType();
            if (fieldType.size() == 0 || structType.getMems().size() == 0) {
                item.setUnlimited(true);
                return item;
            }
            for (String key : fieldType.keySet()) {
                CType type = fieldType.get(key);
                UATTestCaseTreeItem _item = createChildItem(type, item, null);
                _item.setText(0, key + " < " + type.getName() + " > ");
            }
        } else if (ctype instanceof CType_Typedef) {
            CType_Typedef typedefType = (CType_Typedef) ctype;
            CType originalType = typedefType.getOriginaltype();
            createChildItem(originalType, item, null);
        }
        return item;
    }


    private void addValues(UATTestCaseTreeItem parent, int option) {
        UATTestCaseTreeItem[] items = parent.getItems();
        Iterator<String> it = null;
        if (testCase != null) {
            if (option == 1)
                it = testCase.getConsoleOutput().iterator();
            else if (option == 2)
                it = testCase.getFileInput().iterator();
            else if (option == 3)
                it = testCase.getFileOutput().iterator();
            else if (option == 0)
                it = testCase.getExpectedConsoleOutput().iterator();
            else if (option == 4)
                it = testCase.getSocketSend().iterator();
            else if (option == 5)
                it = testCase.getSocketRec().iterator();
            for (UATTestCaseTreeItem _item : items) {
                while (it.hasNext()) {
                    String s = it.next();
                    _item.setText(1, s);
                    break;
                }
            }
        }
    }

    // �������ֵ����table��ÿһ�кͲ���������ÿ��������Ӧ����
    private void addValues(boolean isParam) {
        // TableTreeItem[] items = tableTree.getItems();
        UATTestCaseTreeItem parent;
        if (isParam)
            parent = paramItem;
        else
            parent = globalItem;
        UATTestCaseTreeItem[] items = parent.getItems();
        if (testCase != null) {
            List<AbstractParamValue> varList;
            if (isParam)
                varList = testCase.getFuncParamList();
            else
                varList = testCase.getGlobalParamList();
            for (AbstractParamValue apv : varList) {
                nodeAddrMap.clear();
                String typeAndName = apv.getName() + " < " + apv.getTypeName() + " >";
                for (UATTestCaseTreeItem _item : items) {
                    if (_item.getText().equals(typeAndName)) {
                        addItemValue(_item, apv, 1);
                        break;
                    }
                }
            }
        }
    }

    /**
     * private void addGlobalOutValues() {
     * UATTestCaseTreeItem parent = findTableTreeItem("ȫ�ֱ�������ֵ");
     * UATTestCaseTreeItem[] items = parent.getItems();
     * for (TestCaseNew tc : tcList) {
     * List<AbstractParamValue> varList = tc.getGlobalParamOutList();
     * if (varList == null)
     * return;
     * int i = 0;
     * for (AbstractParamValue apv : varList) {
     * addItemValue(items[i], apv, tc.getTableID());
     * i++;
     * }
     * }
     * }
     */

    // ����������ֵ��ʹ�õݹ���� modify by Yaoweichang
    private void addItemValue(UATTestCaseTreeItem item, AbstractParamValue apv, int columnIndex) {
        UATTestCaseTreeItem[] childItems = item.getItems();

        if (apv instanceof PrimitiveParamValue) {
            String value = ((PrimitiveParamValue) apv).getValue();
            if (value == null || value.equals("null"))
                value = "null";
            else if (((PrimitiveParamValue) apv).getType().getName().contains("char")) {
                value = ASCIITranslator.translate(Integer.parseInt(value));
            }
            if (value.equals("000"))
                value = "0";
            item.setText(columnIndex, value);
        } else if (apv instanceof ArrayParamValue) {
            ArrayParamValue arrayParamValue = (ArrayParamValue) apv;
            int len = (int) arrayParamValue.getLen();
            if (len == -1)
                len = Config.dimSize4varLenArr;// ��ֹlenû�б���ʼ��
            Map<Integer, AbstractParamValue> value = arrayParamValue.getMemberValue();
            AbstractParamValue[] temp = new AbstractParamValue[len];
            for (Integer i : value.keySet()) {
                temp[i] = value.get(i);
            }
            // add by chenruolin childItem���Ȳ���ʱ���ӳ���
            for (int i = 1; i <= temp.length - childItems.length; i++) {
                addNewTreeItem(childItems[0], item, childItems.length + i - 1, columnIndex);
            }
            childItems = item.getItems();
            for (int i = 0; i < temp.length; i++) {
                if (i > 150)
                    addItemValue(childItems[i], temp[i], columnIndex);
                else
                    addItemValue(childItems[i], temp[i], columnIndex);
            }
        } else if (apv instanceof PointerParamValue) {
            PointerParamValue pointerParamValue = (PointerParamValue) apv;
            AbstractParamValue temp = pointerParamValue.getMemberValue();
            // ָ��ӱ����ʱҪ������
            if (temp == null)// ������ Ҳ����null
                item.setText(columnIndex, "null");
            else
                addItemValue(childItems[0], temp, columnIndex);
        } else if (apv instanceof StructParamValue) {
            StructParamValue structParamValue = (StructParamValue) apv;

            // �ж��������� add by Yaoweichang
            String jsonStr = structParamValue.toJson();
            Map<String, String> paramMap = new Gson().fromJson(jsonStr, new TypeToken<Map<String, String>>() {}.getType());
            String primitiveAddr = paramMap.get("addr");// �õ���ǰ����ֵ�������ַ
            boolean mark = true;
            sameNodeItem = null;
            Iterator iter = nodeAddrMap.entrySet().iterator();
            while (iter.hasNext() && mark) {
                Map.Entry entry = (Map.Entry) iter.next();
                if (primitiveAddr == (String) entry.getKey()) {
                    mark = false;
                    sameNodeItem = (UATTestCaseTreeItem) entry.getValue();
                }
            }
            if (mark) {// ����������
                nodeAddrMap.put(primitiveAddr, item);
                AbstractParamValue temp = null;
                Map<String, AbstractParamValue> value = structParamValue.getMemberValue();
                if (value.size() == 0) {
                    item.setText(columnIndex, "null");
                } else {
                    if (childItems.length == 0) {
                        CType_Struct structType = (CType_Struct) item.getCType();
                        LinkedHashMap<String, CType> fieldType = structType.getCTypeWithMems().getfieldType();
                        for (String key : fieldType.keySet()) {
                            CType type = fieldType.get(key);
                            UATTestCaseTreeItem _item = createChildItem(type, item, null);
                            _item.setText(0, key + " < " + type.getName() + " > ");
                        }
                        /*
                         * for (String i : value.keySet()) {// for���ȳ���������ͣ�������Ȳ���ʱ�͹����µ�item
                         * temp = value.get(i);
                         * //createChildItem(temp.getType(), item, null);
                         * String name = i.substring(i.lastIndexOf('.') + 1, i.length());
                         * UATTestCaseTreeItem _item = createChildItem(temp.getType(), item, null);
                         * _item.setText(0, name + " < " + temp.getType().getName() + " > ");
                         * }
                         */
                        childItems = item.getItems();
                        item.setExpanded(false);
                    }
                    for (String i : value.keySet()) {
                        temp = value.get(i);
                        String name = i.substring(i.lastIndexOf('.') + 1, i.length());
                        CType type = temp.getType();
                        String typeAndName = name + " < " + type.getName() + " > ";
                        for (UATTestCaseTreeItem childItem : childItems) {
                            if (childItem.getText().equals(typeAndName)) {
                                addItemValue(childItem, temp, columnIndex);
                                break;
                            }
                        }
                    }
                }
            } else {// ��������
                item.setText(sameNodeItem.getText());
                item.addListener(SWT.Selection, new Listener() {
                    public void handleEvent(Event e) {
                        sameNodeItem.setExpanded(false);
                    }
                });
                nodeAddrMap.clear();
            }
        } else if (apv instanceof TypeDefParamValue) {
            if (apv == null)
                return;
            AbstractParamValue value = ((TypeDefParamValue) apv).getMemberValue();
            addItemValue(childItems[0], value, columnIndex);
        } else if (apv instanceof EnumParamValue) {
            if (apv == null)
                return;
            String value = ((EnumParamValue) apv).getValue();
            if (value == null)
                value = "null";
            item.setText(columnIndex, value);
        } else if (apv instanceof FunctionParamValue) {
            if (apv == null)
                return;
            String value = ((FunctionParamValue) apv).getFunctionName();
            if (value == null)
                value = "null";
            item.setText(columnIndex, value);
        }
    }

    // add by chenruolin
    private void addNewTreeItem(UATTestCaseTreeItem tempItem, UATTestCaseTreeItem root, int no, int col) {
        UATTestCaseTreeItem newItem = new UATTestCaseTreeItem(root, SWT.NONE, tempItem.getCType(), tempItem.getVND());
        root.setExpanded(tempItem.getParentItem().getExpanded());
        if (tempItem.getText().indexOf("[") < 0 || tempItem.getText().indexOf("]") < 0)
            newItem.setText(tempItem.getText());
        else {
            String tempString = tempItem.getText().substring(0, tempItem.getText().indexOf("[") + 1) + no;
            tempString += tempItem.getText().substring(tempItem.getText().indexOf("]"));
            newItem.setText(tempString);
        }

        newItem.setText(1, tempItem.getText(1));// ��ǰ�����޹�ֵ�������������д�����ֵ����Ḳ���޹�ֵ
        newItem.setExpanded(tempItem.getExpanded());
        for (int i = 0; i <= tempItem.getItems().length - 1; i++)
            addNewTreeItem(tempItem.getItems()[i], newItem, no, col);
    }

    // ��������ֵ��״��
    private void createRetValItem(UATTestCaseTreeItem parent, boolean isActualRet) {
        if (tm == null)
            return;
        createChildItem(returnType, parent, null);
    }

    // ���뷵��ֵ��ֵ
    private void addRetVal(boolean isActualRet) {
        UATTestCaseTreeItem parent;
        if (isActualRet)
            parent = retItem;
        else
            parent = expectRetItem;
        UATTestCaseTreeItem[] items = parent.getItems();
        if (testCase != null) {
            AbstractParamValue apv;
            if (isActualRet)
                apv = testCase.getActualReturn();
            else
                apv = testCase.getExpectReturn();

            addItemValue(items[0], apv, 1);
        }
    }

    public HashSet<Long> getFaultTCID() {
        HashSet<Long> idSet = new HashSet<Long>();
        if (testCase != null) {
            TreeItem testTree = uatGui.getTestCaseTree().getItem(0);
            TreeItem[] items = testTree.getItems();
            for (int i = 0; i < items.length; i++) {
                int state = uatGui.map.get(items[i].getData());// ��ȡ������״̬
                if (state == 2)
                    idSet.add((Long) items[i].getData());// �����������ID���뼯��
            }
        }
        return idSet;
    }

    public HashSet<Long> getCorrectTCID() {
        HashSet<Long> idSet = new HashSet<Long>();
        if (testCase != null) {
            TreeItem testTree = uatGui.getTestCaseTree().getItem(0);
            TreeItem[] items = testTree.getItems();
            for (int i = 0; i < items.length; i++) {
                int state = uatGui.map.get(items[i].getData());// ��ȡ������״̬
                if (state == 1)
                    idSet.add((Long) items[i].getData());// ��ͨ��״̬������ID���뼯��
            }
        }
        return idSet;
    }

    private String dealPlus(CType_BaseType type, String value) {
        if (!value.startsWith("+"))
            return value;
        if (type == CType_BaseType.bitType || type == CType_BaseType.uCharType || type == CType_BaseType.uIntType || type == CType_BaseType.uLongLongType || type == CType_BaseType.uLongType
                || type == CType_BaseType.uShortType)
            ;
        else
            value = value.substring(1); // ȥ��+��
        return value;
    }

    private void setEditableColor(UATTestCaseTreeItem[] items) {
        for (UATTestCaseTreeItem item : items) {
            String text = item.getText();
            if (text.equals("���Ƿ�ʽ") || text.equals("ͨ��") || text.equals(""))
                continue;
            if (item.getItems().length == 0) {
                if (!(text.equals("����") || text.equals("ȫ�ֱ���") || text.equals("Ԥ�ڷ���ֵ") || text.equals("����ֵ")))
                    item.setBackground(Resource.editableColor);
            } else
                setEditableColor(item.getItems());
        }
    }

    private boolean isEditableColor(UATTestCaseTreeItem item) {
        String text = item.getText();
        if (text.contains("����") || text.equals("ͨ��") || text.equals(""))
            return false;
        if (item.getItems().length == 0) {
            if (!(text.equals("����") || text.equals("ȫ�ֱ���") || text.equals("Ԥ�ڷ���ֵ") || text.equals("����ֵ")))
                return true;
        }
        return false;
    }

}
