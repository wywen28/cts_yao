package unittest.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import org.apache.log4j.Logger;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import softtest.cfg.c.VexNode;
import softtest.domain.c.analysis.ValueSet;
import softtest.domain.c.symbolic.LogicalExpression;
import softtest.domain.c.symbolic.RelationExpression;
import softtest.domain.c.symbolic.SymbolExpression;
import softtest.domain.c.symbolic.SymbolFactor;
import unittest.Exception.StubGenerateException;
import unittest.Exception.UnsupportedReturnTypeException;
import unittest.branchbound.c.BranchBound;
import unittest.branchbound.c.ConstraintExtractor;
import unittest.branchbound.c.NewBranchBound;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.module.seperate.NoExitMethodException;
import unittest.module.seperate.TestModule;
import unittest.pathchoose.util.path.OnePath;
import unittest.pathchoose.util.path.Onemcdcpath;
import unittest.testcase.generate.manualinterventiontcgen.ExecuteTemplate;
import unittest.testcase.generate.util.TestCaseNew;
import unittest.util.CMDProcess;
import unittest.util.Config;
import unittest.util.CoverRule;
import unittest.util.RecordToLogger;

// import unittest.util.iconv;
/**
 * @author Cai Min �û���Ԥ������������
 */
public class UATManualInterventionGUI {
    static Logger logger = Logger.getLogger(UATManualInterventionGUI.class);

    private UATGUI uatGui;
    private Display display;
    private Shell shell;

    private Composite topComposite;
    private TabFolder topTabFolder;
    private TabItem sourceFileItem;
    private Composite bottomComposite;
    private Composite testCaseLibComposite;
    private Composite fileComp;
    private Composite randomComp;
    private Composite pathComp;
    private Composite pathLeftComp;
    private Composite pathRightComp;
    private Composite driverFileComp;
    private TabItem driverFileItem;
    // private List pathList;
    private Table pathTable;
    private Button okButton;
    private Button cancelButton;
    private SashForm sashForm;

    private UATTestCaseInputTableGUI tcTable;
    private UATParamSettingGUI pSettingGUI;
    private java.util.List<OnePath> paths;

    private Composite charCheckComp;
    private Button charCheckButton;
    private Button editDriverFileButton;
    private Button generateTestCaseForAllpathButton;
    private Button saveAndRunDriverFileButton;

    private SashForm sashForm2;
    private TabItem pathItem;
    private TabItem randomItem;

    boolean firstBuildPathComposite = true;
    CoverRule criteriaCoverRule;

    private ExecuteTemplate et;

    private SourceViewer sourceViewer;

    private Menu pathTableMenu;

    // add by xujiaoxian
    private Composite testCaseProgressBarComposite = null;
    private Composite progressBarComposite = null;
    private TabFolder testCaseProgressBarFolder = null;
    private TabItem testCaseLibTabItem = null;
    private TabItem progressBarTabItem = null;
    private UATProgressDisplayGUI progressDisplayGUI = null;
    // end add by xujiaoxian

    // add by chenruolin �������ʱ�Ƿ񱣴��������������ȫ·�����ɵ��ظ���������
    boolean needSave = true;

    /**
     * @wbp.parser.entryPoint
     */
    public UATManualInterventionGUI(UATGUI uatGui) {
        this.uatGui = uatGui;
        criteriaCoverRule = uatGui.getCurrentCoverCriteria();
        uatGui.getShell().setEnabled(false);
        et = new ExecuteTemplate(uatGui.getCurrentFunc(), uatGui.getCurrentCoverCriteria());
    }

    /**
     * @wbp.parser.entryPoint
     */
    public void go() {
        display = Display.getDefault();
        createShell();
        dealEvent();
        shell.open();

        while (!display.isDisposed())
            if (!display.readAndDispatch())
                display.sleep();
        display.dispose();
    }

    private void createShell() {
        shell = new Shell(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL | SWT.MAX | SWT.MIN);
        // add by chenruolin ģ̬������ʾ
        shell.setText(GUILanguageResource.getProperty("ManualIntervention"));
        shell.setImage(Resource.UATImage);
        shell.setBounds(50, 50, 800, 700);
        shell.setLayout(new FillLayout());
        shell.setMaximized(false);
        LayoutUtil.centerShell(display, shell);
        sashForm = new SashForm(shell, SWT.VERTICAL);

        createCharCheckComposite();
        createTopComposite();
        createTestCaseProcessBarComposite();
        createBottomComposite();

        sashForm.setWeights(new int[] {3, 32, 24, 3});
    }

    private void createCharCheckComposite() {
        charCheckComp = WidgetFactory.createComposite(sashForm, SWT.BORDER);
        charCheckComp.setLayout(new FormLayout());
        charCheckButton = WidgetFactory.createButton(charCheckComp, SWT.CHECK);
        charCheckButton.setText("��ASCII����ʽ�����ַ�");
        charCheckButton.setSelection(true);
        charCheckButton.setToolTipText("�����ѡ������ascii����ʽ���ַ�����ѡ��ֱ������������ַ���\n�磺����65��A");
        WidgetFactory.configureFormData(charCheckButton, null, new FormAttachment(0, 5), new FormAttachment(100, -10), null);

        editDriverFileButton = WidgetFactory.createButton(charCheckComp, SWT.PUSH);
        editDriverFileButton.setText("�������ļ��༭��������");
        WidgetFactory.configureFormData(editDriverFileButton, null, new FormAttachment(0, 1), new FormAttachment(charCheckButton, -10), null);

        generateTestCaseForAllpathButton = WidgetFactory.createButton(charCheckComp, SWT.PUSH);
        generateTestCaseForAllpathButton.setText("Ϊ����·�����ɲ�������");
        WidgetFactory.configureFormData(generateTestCaseForAllpathButton, null, new FormAttachment(0, 1), new FormAttachment(editDriverFileButton, -10), null);
        generateTestCaseForAllpathButton.setVisible(false);

        saveAndRunDriverFileButton = WidgetFactory.createButton(charCheckComp, SWT.PUSH);
        saveAndRunDriverFileButton.setText("���沢���������ļ�");
        WidgetFactory.configureFormData(saveAndRunDriverFileButton, null, new FormAttachment(0, 1), new FormAttachment(charCheckButton, -10), null);
        saveAndRunDriverFileButton.setVisible(false);
        charCheckButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (charCheckButton.getSelection())
                    tcTable.charToAscii = true;
                else
                    tcTable.charToAscii = false;
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {}
        });

        editDriverFileButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                try {
                    String message = "ʹ�÷���:\n1.�������ļ��������Ͷ����������;\n2.���\"���������ļ�\"��ť����;\n3.���\"���������ļ�\"����\n\n" + "���²������������������ֵʱ,Ĭ�ϸ������һ�в�������������Ӧ�����ļ�;����ʹ�ÿղ����������������ļ�";
                    MessageBox box = WidgetFactory.createInfoMessageBox(shell, "��ʾ��Ϣ", message);
                    box.open();
                    showDriverFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error("��ʾ�����ļ�����" + e.getMessage());
                }
                topTabFolder.setSelection(3);
                editDriverFileButton.setVisible(false);
                saveAndRunDriverFileButton.setVisible(true);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {}
        });


        // to be added �������ɲ���������ͳ����Ϣд���ļ�
        generateTestCaseForAllpathButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                // add by chenruolin ������������������ʱ�����ظ���������
                if (paths.size() == 0) {
                    testCaseProgressBarFolder.setSelection(0);
                    return;
                }

                // add by xujiaoxian
                testCaseProgressBarFolder.setSelection(1);
                new Thread() {
                    public void run() {
                        progressDisplayGUI.terminateListener.setRunningThread(this);
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {

                                progressDisplayGUI.setTestProgressRunning(1);
                                progressDisplayGUI.setInfo("������������δ����·�����ɲ������������Ժ�...");

                            }

                        });
                        Display.getDefault().asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                TestModule tm = uatGui.getCurrentFunc();
                                int inFeasibleAmount = 0;

                                for (OnePath op : paths) {
                                    // added zhangxuzhou 2013-3-8 ���½����ߵĹ�ϵ
                                    tm.getGraph().clearVisited();
                                    op.initpathedges();
                                    if (op.isInfeasible())
                                        inFeasibleAmount++;

                                }
                                // �����ļ� zhangxuzhou 2012-10-22
                                // ��ȡʱ�����ļ���
                                String funcname = tm.getFuncName();// 2012-10-24 ���Ӻ�����
                                java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("MMDD_HHmmss");
                                java.util.Date time = new java.util.Date();
                                String timeString = new String(format.format(time));
                                String fileName = new String(tm.getBelongToFile().getTestResultDir() + File.separator + "result_Bat_" + timeString + ".txt");

                                File resFile = new File(fileName);

                                // ����excel�ĵ��� 2012-11-4
                                String excelFileName = new String(tm.getBelongToFile().getTestResultDir() + File.separator + "info_Bat_" + timeString + ".xls");
                                File resExcelFile = new File(excelFileName);// ����excel��� 2012-11-4
                                WritableSheet ws = null;
                                WritableFont wfont = new WritableFont(WritableFont.ARIAL, 12, WritableFont.NO_BOLD, false, jxl.format.UnderlineStyle.NO_UNDERLINE, jxl.format.Colour.BLACK);
                                WritableCellFormat titleFormat = new WritableCellFormat(wfont);
                                if (!resExcelFile.exists())
                                    try {
                                        resExcelFile.createNewFile();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                WritableWorkbook book = null;
                                try {
                                    book = Workbook.createWorkbook(resExcelFile);// �½�excel���
                                    ws = book.createSheet("����·����ͳ����Ϣ", 0); // �½�һ��sheet

                                    String[] title = {"·�����", "·������", "��֧����", "�����ڵ���ʽ������Ϊ", "��ֵ���ʽ������Ϊ", "�ǵ�ֵ�Ƚϱ��ʽ������", "��������", "��������", "�ɹ����", "�ܻ��ݴ���", "����ʱ��"};
                                    // ����Excel��ͷ
                                    for (int i = 0; i < title.length; i++) {
                                        Label excelTitle = new Label(i, 0, title[i], titleFormat);
                                        ws.addCell(excelTitle);
                                    }
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                } catch (RowsExceededException e) {
                                    e.printStackTrace();
                                } catch (WriteException e) {
                                    e.printStackTrace();
                                }
                                // ���excel������������Ա���bb�ж�excel����д��

                                if (!resFile.exists())
                                    try {
                                        resFile.createNewFile();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                RandomAccessFile mm = null;
                                try {
                                    mm = new RandomAccessFile(fileName, "rw");
                                    mm.write((new String("��ǰ���⺯���ǣ�" + funcname + "\r\n" + "Ϊ����·�����ɲ�������\r\n" + "\t���пɴ�·������Ϊ:" + (paths.size() - inFeasibleAmount) + "��" + "\t" + "���ɴ�·������Ϊ:"
                                            + inFeasibleAmount)
                                            + "��" + "\r\n").getBytes());
                                } catch (FileNotFoundException e1) {
                                    e1.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                int opn = 0;
                                int count = 0;
                                int failRunCount = 0;
                                String seperator = new String("---------------------------------------------------------------------------------------------");
                                long start = System.currentTimeMillis();


                                try {
                                    for (OnePath op : paths) {
                                        mm.seek(mm.length());// ����β��
                                        java.text.SimpleDateFormat format1 = new java.text.SimpleDateFormat("HH:mm:ss");
                                        time = new java.util.Date();
                                        timeString = new String(format1.format(time));
                                        mm.write(new String("\r\n" + seperator + "\r\n" + "����Ϊ#" + opn + "��·�����ɲ�������\r\n��ʼʱ��Ϊ��" + timeString + " \r\n;").getBytes());
                                        mm.seek(mm.length());// ����β��
                                        TestCaseNew tc = null;

                                        if (!op.getIsinfeasible()) {
                                            BranchBound bb = new BranchBound(op, mm, tm);
                                            bb.exec();
                                            // �������ɵĲ����������д������ɳɹ��ͼ��뵽����У������¼����ʧ��
                                            tc = bb.getBranchBoudCase();// ��֧�޽����ɵĲ�������
                                            if (tc != null)
                                                tcTable.addValues(tc);// ���еĲ��������������
                                            else
                                                count++;
                                            if (bb.getFailRun())
                                                failRunCount++;// ����Ĳ�����������


                                            // д��excel������ 2012-11-5
                                            try {
                                                int rowCount = ws.getRows();// ��ǰ������ ���������1��ʼ
                                                                            // �������Ǵ�0��ʼ
                                                Label excelTitle = new Label(0, rowCount, "#" + opn++, titleFormat);
                                                ws.addCell(excelTitle);
                                                ArrayList<String> cells = bb.getWriteExcelString();
                                                for (int i = 0; i < cells.size(); i++) {
                                                    excelTitle = new Label(i + 1, rowCount, cells.get(i), titleFormat);
                                                    ws.addCell(excelTitle);
                                                }

                                            } catch (RowsExceededException e) {
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                            } catch (WriteException e) {
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                            }
                                            bb = null;
                                            continue;
                                        } else {
                                            mm.write(new String("~~~~~~~~~~~~~~~~~~" + "#" + opn + "��·��Ϊ���ɴ�·��������" + "~~~~~~~~~~~~~~~" + "\r\n\r\n").toString().getBytes());
                                            int rowCount = ws.getRows();// ��ǰ������ ���������1��ʼ �������Ǵ�0��ʼ
                                            Label excelTitle = new Label(0, rowCount, "#" + opn++, titleFormat);
                                            try {
                                                ws.addCell(excelTitle);
                                            } catch (RowsExceededException e1) {
                                                // TODO Auto-generated catch block
                                                e1.printStackTrace();
                                            } catch (WriteException e1) {
                                                // TODO Auto-generated catch block
                                                e1.printStackTrace();
                                            }

                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    progressDisplayGUI.setTestProgressOver(0);
                                    progressDisplayGUI.terminateListener.setRunningThread(null);
                                } catch (Exception e) {
                                    String errorMessage = "";
                                    errorMessage += "��֧�޽��������쳣" + e.getMessage() + "\r\n";
                                    logger.error(errorMessage);
                                    e.printStackTrace();
                                    progressDisplayGUI.setTestProgressOver(0);
                                    progressDisplayGUI.terminateListener.setRunningThread(null);
                                    return;
                                }

                                try {
                                    long end = System.currentTimeMillis();
                                    mm.write(new String(seperator + "\r\n" + "ȫ���������" + "����ʱ" + (end - start) / 1000 + "." + (end - start) % 1000 + "��\r\n" + seperator + "\r\n" + "������ϣ��ɴ�·������"
                                            + count + "��·��û�����ɲ�������,��" + failRunCount + "��·�����ɴ���Ĳ�������" + "\n" + "" + "Test Coverage for " + tm.getCoverRule().toString() + " is "
                                            + (int) (tm.getCoverSetList().get(tm.getCoverRule().getOutCon()).getCoverage() * 100) + "% for func " + tm.getFuncName()).getBytes());
                                    book.write();

                                    // chenruolin 2013-3-21 �ı��ļ��ĸ�ʽ��
                                    if (Config.os != "windows") {
                                        String reCodefile = fileName.substring(0, fileName.indexOf(".txt")) + "_linux.txt";
                                        String cmdping = new String("iconv -f gb2312 -t utf-8 " + fileName + " -o " + reCodefile);
                                        System.out.println(cmdping);
                                        CMDProcess.process(cmdping, true);
                                    }
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                    progressDisplayGUI.setTestProgressOver(0);
                                    progressDisplayGUI.terminateListener.setRunningThread(null);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    progressDisplayGUI.setTestProgressOver(0);
                                    progressDisplayGUI.terminateListener.setRunningThread(null);
                                };

                                Object[] options = {"��", "��"};
                                int response =
                                        JOptionPane.showOptionDialog(null, "��������·���Ĳ�����������������ϡ�\r\n\t�Ƿ�鿴ͳ����Ϣ��", "���", JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                                if (response == 0) {
                                    logger.debug("ѡ���˲鿴�ļ�");
                                    try {
                                        Process p1;
                                        if (Config.os.equals("windows"))
                                            p1 = Runtime.getRuntime().exec("notepad " + fileName);
                                        else {
                                            String reCodefile = fileName.substring(0, fileName.indexOf(".txt")) + "_linux.txt";
                                            p1 = Runtime.getRuntime().exec("gedit " + reCodefile);
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        progressDisplayGUI.setTestProgressOver(0);
                                        progressDisplayGUI.terminateListener.setRunningThread(null);
                                    }
                                } else if (response == 1) {
                                    logger.debug("���鿴");
                                }
                                // �ر��ļ�
                                try {
                                    mm.close();
                                    book.close();

                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                    progressDisplayGUI.setTestProgressOver(0);
                                    progressDisplayGUI.terminateListener.setRunningThread(null);
                                } catch (WriteException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                    progressDisplayGUI.setTestProgressOver(0);
                                    progressDisplayGUI.terminateListener.setRunningThread(null);
                                }
                                progressDisplayGUI.setTestProgressOver(0);
                                progressDisplayGUI.terminateListener.setRunningThread(null);

                            }
                        });

                    }
                }.start();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {

            }
        });
        saveAndRunDriverFileButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (sourceViewer == null)
                    return;
                IDocument doc = sourceViewer.getDocument();
                String content = doc.get();
                TestModule tm = uatGui.getCurrentFunc();
                FileOutputStream out;
                try {
                    out = new FileOutputStream(tm.getTestSuiteName());
                    out.write(content.getBytes());
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    logger.error("���������ļ�ʧ��" + e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error("���������ļ�ʧ��" + e.getMessage());
                }

                runDriverFile();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {}
        });
    }

    private void createTopComposite() {
        topComposite = WidgetFactory.createComposite(sashForm, SWT.FLAT);
        topComposite.setBackground(Resource.backgroundColor);
        topComposite.setLayout(new FormLayout());
        topTabFolder = WidgetFactory.createTabFolder(topComposite, 0);
        WidgetFactory.configureFormData(topTabFolder, new FormAttachment(0, 1), new FormAttachment(0, 1), new FormAttachment(100, -1), new FormAttachment(100, -1));
        showFunction();
        createRandomComp();
        pathComp = WidgetFactory.createComposite(topTabFolder, SWT.NONE);
        pathComp.setLayout(new FillLayout());
        pathItem = WidgetFactory.createTabItem(topTabFolder, GUILanguageResource.getProperty("BranchBound"), null, pathComp);
    }

    private void createTestCaseComposite() {
        testCaseLibComposite = WidgetFactory.createComposite(testCaseProgressBarFolder, SWT.BORDER);
        testCaseLibComposite.setLayout(new FillLayout());
        tcTable = new UATTestCaseInputTableGUI(testCaseLibComposite, uatGui);
        pSettingGUI.setTcTable(tcTable);
        tcTable.usage = 1;
        tcTable.createContents();
        pSettingGUI.dealEvent();
    }

    /**
     * 2013/05/30
     * ���˹�������������ӽ�����
     * add by xujiaoxian
     */
    public void createTestCaseProcessBarComposite() {
        testCaseProgressBarComposite = WidgetFactory.createComposite(sashForm, SWT.BORDER);
        testCaseProgressBarComposite.setLayout(new FormLayout());
        testCaseProgressBarFolder = WidgetFactory.createTabFolder(testCaseProgressBarComposite, SWT.NONE);
        testCaseProgressBarFolder.setToolTipText(GUILanguageResource.getProperty("TestCaseTabItem"));
        WidgetFactory.configureFormData(testCaseProgressBarFolder, new FormAttachment(0, 0), new FormAttachment(0, 0), new FormAttachment(100, 0), new FormAttachment(100, 0));
        createTestCaseLibWindow();
    }

    /**
     * 2013/05/30
     * ������ʾ��ʾ����������ѡ�
     * add by xujiaoxian
     */
    public void createTestCaseLibWindow() {
        createTestCaseComposite();
        testCaseLibTabItem = WidgetFactory.createTabItem(testCaseProgressBarFolder, GUILanguageResource.getProperty("TestCaseTabItem"), null, testCaseLibComposite);
    }

    /**
     * 2013/05/30
     * ����������ѡ�
     * add by xujiaoxian
     */
    public void createProgressWindow() {
        createProgressBarComposite();
        progressBarTabItem = WidgetFactory.createTabItem(testCaseProgressBarFolder, GUILanguageResource.getProperty("TestProgress"), null, progressBarComposite);
    }

    /**
     * 2013/05/30
     * ������ʾ�����������
     * add by xujiaoxian
     */
    public void createProgressBarComposite() {
        progressBarComposite = WidgetFactory.createComposite(testCaseProgressBarFolder, SWT.BORDER);
        progressBarComposite.setLayout(new FormLayout());
        progressDisplayGUI = new UATProgressDisplayGUI(this.uatGui, progressBarComposite, testCaseProgressBarFolder, 1);
    }

    // ��ʾ���⺯��Դ����
    private void showFunction() {
        TestModule tm = uatGui.getCurrentFunc();
        fileComp = WidgetFactory.createComposite(topTabFolder, SWT.NONE);
        sourceFileItem = WidgetFactory.createTabItem(topTabFolder, "�ֶ�����", null, fileComp);
        try {
            new UATUncoveredElementGUI(tm, fileComp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createRandomComp() {
        randomComp = WidgetFactory.createComposite(topTabFolder, SWT.NONE);
        randomItem = WidgetFactory.createTabItem(topTabFolder, "�������", null, randomComp);
        pSettingGUI = new UATParamSettingGUI(uatGui, randomComp);
    }

    private void createPathComposite() throws InterruptedException {
        // add by chenruolin
        generateTestCaseForAllpathButton.setVisible(true);

        sashForm2 = new SashForm(pathComp, SWT.HORIZONTAL);

        pathLeftComp = WidgetFactory.createComposite(sashForm2, SWT.BORDER);
        WidgetFactory.configureFormData(pathLeftComp, new FormAttachment(0, 0), new FormAttachment(0, 0), new FormAttachment(0, 150), new FormAttachment(100, 0));
        pathLeftComp.setLayout(new FillLayout());

        pathRightComp = WidgetFactory.createComposite(sashForm2, SWT.BORDER);
        WidgetFactory.configureFormData(pathRightComp, new FormAttachment(pathLeftComp, 2), new FormAttachment(0, 0), new FormAttachment(100, 0), new FormAttachment(100, 0));
        pathRightComp.setLayout(new FillLayout());

        if (uatGui.getCurrentFunc().isFirstTest()) {
            try {
                uatGui.getCurrentFunc().setUnCoveredPathsBeforeAnyTest(criteriaCoverRule);
            } catch (NoExitMethodException e) {
                final String msg = e.getMessage();
                RecordToLogger.recordExceptionInfo(e, logger);
                if (Config.printExceptionInfoToConsole)
                    e.printStackTrace();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        MessageBox box = WidgetFactory.createInfoMessageBox(uatGui.getShell(), "����ʱ�쳣", "���� " + uatGui.getCurrentFunc().getFuncName() + "������ѭ����û�г���" + msg);
                        box.open();
                    }
                });
            }
        } else {
            uatGui.getCurrentFunc().setUnCoveredPathsFromUnCoveredElements();
        }
        paths = uatGui.getCurrentFunc().getManualUnCoveredPaths(); // paths��ǰ̨չʾ·�����ݵļ��� yumeng
        if (paths.size() == 0) {
            Text text = new Text(pathLeftComp, SWT.NONE);
            text.setText("δ����·������Ϊ0.");
        } else {
            pathTable = new Table(pathLeftComp, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
            pathTableMenu = new Menu(shell, SWT.POP_UP);
            pathTable.setLinesVisible(true);
            pathTable.setHeaderVisible(true);

            new TableColumn(pathTable, SWT.CENTER).setWidth(50);
            new TableColumn(pathTable, SWT.CENTER).setWidth(80);
            new TableColumn(pathTable, SWT.CENTER).setWidth(50);
            new TableColumn(pathTable, SWT.CENTER).setWidth(70);
            new TableColumn(pathTable, SWT.LEFT).setWidth(600);
            pathTable.getColumn(0).setText("���");
            pathTable.getColumn(1).setText("·������");
            pathTable.getColumn(2).setText("�༭");
            pathTable.getColumn(3).setText("�ɴ���");
            pathTable.getColumn(4).setText("·���켣");

            for (int i = 0; i < paths.size(); i++)
                new TableItem(pathTable, SWT.None);

            TableItem[] items = pathTable.getItems();
            for (int i = 0; i < items.length; i++) {
                items[i].setText(0, "#" + i);
                items[i].setText(1, "����");
                TableEditor editor = new TableEditor(pathTable);
                Button button = new Button(pathTable, SWT.PUSH);
                button.setText("������");
                button.pack();
                editor.minimumWidth = button.getSize().x;
                editor.horizontalAlignment = SWT.CENTER;
                editor.setEditor(button, items[i], 2);
                if (!paths.get(i).isFeasiblePath())
                    items[i].setText(3, "���ɴ�, ì�ܽڵ㣺" + paths.get(i).getCondictElement());

                items[i].setText(4, paths.get(i).manualToString());
                button.addSelectionListener(new SelectionListener() {
                    @Override
                    public void widgetSelected(SelectionEvent arg0) {
                        new UATPathRangeSettingGUI();
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent arg0) {}
                });
            }

            pathTable.setSelection(0);
            OnePath path = paths.get(0);
            try {
                new UATUncoveredElementGUI(uatGui.getCurrentFunc(), pathRightComp, path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            pathRightComp.layout();

            pathTable.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(final SelectionEvent arg0) {
                    int number = pathTable.getSelectionIndex();
                    OnePath path = paths.get(number);
                    try {
                        for (int i = 0; i < pathRightComp.getChildren().length; i++) {
                            pathRightComp.getChildren()[i].dispose();
                        }
                        new UATUncoveredElementGUI(uatGui.getCurrentFunc(), pathRightComp, path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    pathRightComp.layout();
                }
            });

            pathTable.addMouseListener(new MouseAdapter() {
                public void mouseDown(MouseEvent e) {
                    if (e.button == 3) {
                        // ��굥���Ҽ���3D���
                        pathTable.setMenu(null);

                        // ����Ƿ�㵽�˾���Ľڵ�
                        TableItem item = pathTable.getItem(new Point(e.x, e.y));

                        // û�е㵽������
                        if (item != null)
                            pathTable.setMenu(pathTableMenu);
                    }
                }
            });

            MenuItem item = new MenuItem(pathTableMenu, SWT.PUSH);
            item.setText("���ɲ�������");
            item.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    int number = pathTable.getSelectionIndex();
                    OnePath path = paths.get(number);
                    // ��֧�޽�
                    TestCaseNew tc = null;
                    String errorMessage = "";

                    // ��֧�޽�����������_140104_zmz_start
                    logger.info("***********&&&��֧�޽������Կ�ʼ&&&**************");
                    VexNode tailNode = getFinalBranchNode(path);
                    ValueSet vs = tailNode.getValueSet();

                    ConstraintExtractor ce = path.getFlushedConstraintExtractor(); // Լ����ȡ
                    // mcdc
                    if (path instanceof Onemcdcpath) {
                        List<SymbolExpression> mcdcList = new ArrayList<SymbolExpression>();
                        List<SymbolExpression> selist = ce.getSymbolExpressions();
                        for (SymbolExpression se : selist) {
                            List<RelationExpression> relist = se.getRelationExpressions();
                            for (RelationExpression reExp : relist) {
                                SymbolExpression symExp = new SymbolExpression();
                                LogicalExpression lgExp = new LogicalExpression();
                                symExp.setLogicalExpression(lgExp);
                                symExp.setTF(reExp.isMcdcTF());
                                symExp.getLogicalExpression().addLRExpression(reExp);
                                mcdcList.add(symExp);
                            }
                        }
                        ce.setSymbolExpressions(mcdcList);
                    }

                    HashSet<SymbolFactor> symbolSet = ce.getSymbolFactorsInConstr(vs); // ��ȡԼ�����ʽ�е����з���
                    /**
                     * ��ʼ��֧�޽����ɲ�������
                     */
                    try {
                        NewBranchBound bbbb = new NewBranchBound(ce, vs, symbolSet, tailNode, false);
                        bbbb.generate();
                        tc = bbbb.getTc();
                    } catch (Exception e) {
                        errorMessage += "��֧�޽��������쳣" + "\n" + e.getMessage() + "\r\n";
                        logger.error(errorMessage);
                    }

                    logger.info("���ɵĲ���������" + tc);
                    tcTable.addValues(tc);// ������������������ֵ
                    MessageBox box = WidgetFactory.createInfoMessageBox(shell, "��ʾ��Ϣ", "�ɹ����ɲ�������\r\n�Ѽ�����������б���");
                    box.open();
                    testCaseProgressBarFolder.setSelection(0);
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {}
            });
        }

        sashForm2.setWeights(new int[] {12, 25});

    }

    // ��ʾ���⺯��Դ����
    private void showDriverFile() throws IOException {
        TestModule tm = uatGui.getCurrentFunc();
        driverFileComp = WidgetFactory.createComposite(topTabFolder, SWT.NONE);
        driverFileItem = WidgetFactory.createTabItem(topTabFolder, uatGui.getCurrentFunc().getFuncName(), null, driverFileComp);
        sourceViewer = WidgetFactory.createSourceViewer(driverFileComp, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);

        try {
            java.util.List<TestCaseNew> list = tcTable.getTestCaseList();
            if (list.size() >= 1)
                et.buildFrame(list.get(list.size() - 1)); // ʹ�����һ��TestCase
            else
                et.buildFrame(null);
        } catch (NoExitMethodException e) {
            createErrorMessageBox(e, 0);
        } catch (UnsupportedReturnTypeException e) {
            createErrorMessageBox(e, 0);
        } catch (StubGenerateException e) {
            createErrorMessageBox(e, 0);
        }

        File file = new File(tm.getTestSuiteName());
        FileInputStream fin = new FileInputStream(file);
        int ch;
        StringBuffer data = new StringBuffer();
        while ((ch = fin.read()) != -1) {
            data.append((char) ch);
        }
        if (data != null) {
            String contents = new String(data.toString().getBytes("ISO-8859-1"), "GBK");
            sourceViewer.setDocument(new Document(contents));
        }
        fin.close();
        sourceViewer.setEditable(true);
    }

    private void createBottomComposite() {
        bottomComposite = WidgetFactory.createComposite(sashForm, SWT.BORDER);
        bottomComposite.setLayout(new FormLayout());

        okButton = WidgetFactory.createButton(bottomComposite, SWT.PUSH);
        okButton.setText("����");
        WidgetFactory.configureFormData(okButton, new FormAttachment(0, 150), new FormAttachment(0, 2), new FormAttachment(0, 200), new FormAttachment(100, -2));
        cancelButton = WidgetFactory.createButton(bottomComposite, SWT.PUSH);
        cancelButton.setText(GUILanguageResource.getProperty("Cancel"));
        WidgetFactory.configureFormData(cancelButton, new FormAttachment(100, -200), new FormAttachment(0, 1), new FormAttachment(100, -150), new FormAttachment(100, -1));
    }

    private void dealEvent() {
        shell.addShellListener(new ShellListener() {
            public void shellIconified(ShellEvent arg0) {}

            public void shellDeiconified(ShellEvent arg0) {}

            public void shellDeactivated(ShellEvent arg0) {}

            public void shellClosed(ShellEvent arg0) {
                uatGui.getShell().setEnabled(true);
                shell.dispose();
                uatGui.getUATProgressDisplayGUI().setTestProgressOver(1);
            }

            public void shellActivated(ShellEvent arg0) {}
        });

        topTabFolder.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(final SelectionEvent e) {
                if (firstBuildPathComposite && topTabFolder.getSelectionIndex() == 2) {
                    MessageBox messageBox = new MessageBox(shell, SWT.YES | SWT.NO);
                    messageBox.setText("��ʾ");
                    messageBox.setMessage("�Ƿ���Ҫ�鿴δ����·����");
                    int rc = messageBox.open();
                    if (rc == SWT.YES) {
                        // add by xujiaoxian
                        createProgressWindow();
                        testCaseProgressBarFolder.setSelection(1);
                        new Thread() {
                            public void run() {
                                progressDisplayGUI.terminateListener.setRunningThread(this);
                                Display.getDefault().asyncExec(new Runnable() {
                                    public void run() {

                                        progressDisplayGUI.setTestProgressRunning(1);
                                        progressDisplayGUI.setInfo("��������δ����·�������Ժ�...");

                                    }

                                });
                                Display.getDefault().asyncExec(new Runnable() {

                                    @Override
                                    public void run() {
                                        try {
                                            createPathComposite();
                                            progressDisplayGUI.setTestProgressOver(1);
                                        } catch (InterruptedException e) {
                                            progressDisplayGUI.setTestProgressOver(0);
                                            e.printStackTrace();
                                        } finally {
                                            progressDisplayGUI.terminateListener.setRunningThread(null);
                                            pathComp.layout(true);
                                            firstBuildPathComposite = false;
                                        }

                                    }
                                });

                            }
                        }.start();
                    } else if (rc == SWT.NO) {
                        topTabFolder.setSelection(0);
                    }
                } else {
                    testCaseProgressBarFolder.setSelection(0);
                }
            }
        });

        okButton.addSelectionListener(new OKButtonListener(this));
        cancelButton.addSelectionListener(new CancelButtonListener(this));
    }

    private class OKButtonListener extends SelectionAdapter {
        private UATManualInterventionGUI demo2;

        public OKButtonListener(UATManualInterventionGUI demo2) {
            this.demo2 = demo2;
        }

        public void widgetSelected(SelectionEvent e) {
            demo2.uatGui.getShell().setEnabled(true);

            java.util.List<TestCaseNew> list = tcTable.getTestCaseList();

            if (!tcTable.getValidity()) {
                MessageBox box = WidgetFactory.createInfoMessageBox(shell, "������Ϣ", "��������");
                box.open();
                return;
            }

            // add by chenruolin use for buglink test
            if (list.size() != 0) {
                Config.isAutoCompare = false;
                uatGui.runSelectedTCSet(list, needSave);
            }

            shell.dispose();
            uatGui.getUATProgressDisplayGUI().setTestProgressOver(1);
        }
    }

    private class CancelButtonListener extends SelectionAdapter {
        private UATManualInterventionGUI demo2;

        public CancelButtonListener(UATManualInterventionGUI demo2) {
            this.demo2 = demo2;
        }

        public void widgetSelected(SelectionEvent e) {
            demo2.uatGui.getShell().setEnabled(true);
            shell.dispose();
            uatGui.getUATProgressDisplayGUI().setTestProgressOver(1);
        }
    }

    private void runDriverFile() {
        uatGui.getShell().setEnabled(true);
        final TestModule tm = uatGui.getCurrentFunc();
        final UATProgressDisplayGUI progressDisplayGUI = uatGui.getUATProgressDisplayGUI();
        final UATGUIActions actionsGUI = uatGui.actionsGUI;
        final String funcName = tm.getFuncName();

        new Thread() {
            public void run() {
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        shell.dispose();
                    }
                });
                progressDisplayGUI.terminateListener.setRunningThread(this);
                // �޸Ľ����������߳�ȥִ��
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("����ִ�в������������Ժ�");
                        actionsGUI.clearOutputMessage();
                        uatGui.setStatusBarInfo("���ڶԺ��� " + funcName + "����ָ����������...");
                        actionsGUI.addOutputMessage("���ڶԺ��� " + funcName + "����ָ����������...");
                    }

                });
                try {
                    et.runTest();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            actionsGUI.doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            uatGui.setStatusBarInfo("�Ժ��� " + funcName + "����ָ��������������");
                            actionsGUI.addOutputMessage("�Ժ��� " + funcName + "����ָ��������������");
                            String info = "����ָ�������������";
                            if (tm.getFuncVar().hasFileVar())
                                info += "\n������ȫ�ֱ��������ļ�ָ�룬���û�����У����Խ��";
                            MessageBox box = WidgetFactory.createInfoMessageBox(uatGui.getShell(), "��ʾ��Ϣ", info);
                            box.open();
                        }
                    });
                } catch (final IOException e) {
                    final String msg = e.getMessage();
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            createErrorMessageBox(e, 1);
                            actionsGUI.addOutputMessage("���� " + funcName + "�����ļ��༭����" + msg);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                } catch (final Exception e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    final String msg = e.getMessage();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            createErrorMessageBox(e, 1);
                            actionsGUI.addOutputMessage("���� " + funcName + "�����ļ��༭����" + msg);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                } finally {
                    progressDisplayGUI.terminateListener.setRunningThread(null);
                }
            }
        }.start();
    }

    /**
     * @param flag 0:�����ļ����ɴ��� 1:�����ļ��༭����
     */
    private void createErrorMessageBox(Throwable e, int flag) {
        String message;
        if (flag == 0)
            message = "�����ļ����ɴ���," + e.getMessage();
        else
            message = "�����ļ��༭����," + e.getMessage();
        MessageBox box = WidgetFactory.createErrorMessageBox(uatGui.getShell(), "������Ϣ", message);
        box.open();
        logger.error(message);
    }

    private VexNode getFinalBranchNode(OnePath path) {
        List<VexNode> list = new ArrayList<VexNode>(path.getpathnodes());
        Collections.reverse(list);
        Iterator<VexNode> it = list.iterator();
        VexNode tailNode = null;
        VexNode temp = null;
        while (it.hasNext()) {
            tailNode = it.next();
            if (tailNode.getOutedges().size() > 1 || tailNode.getName().startsWith("label_head_case")) {
                tailNode = temp;
                break;
            }
            temp = tailNode;
        }
        if (tailNode == null || tailNode.getInedges().size() == 0) {
            it = path.getpathnodes().iterator();
            tailNode = list.get(0);
        }

        return tailNode;
    }
}
