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
 * @author Cai Min 用户干预测试用例生成
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

    // add by chenruolin 点击运行时是否保存测试用例，避免全路径生成的重复保存问题
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
        // add by chenruolin 模态窗口显示
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
        charCheckButton.setText("以ASCII码形式输入字符");
        charCheckButton.setSelection(true);
        charCheckButton.setToolTipText("如果勾选则输入ascii码形式的字符，不选则直接输入所需的字符。\n如：输入65或A");
        WidgetFactory.configureFormData(charCheckButton, null, new FormAttachment(0, 5), new FormAttachment(100, -10), null);

        editDriverFileButton = WidgetFactory.createButton(charCheckComp, SWT.PUSH);
        editDriverFileButton.setText("从驱动文件编辑测试用例");
        WidgetFactory.configureFormData(editDriverFileButton, null, new FormAttachment(0, 1), new FormAttachment(charCheckButton, -10), null);

        generateTestCaseForAllpathButton = WidgetFactory.createButton(charCheckComp, SWT.PUSH);
        generateTestCaseForAllpathButton.setText("为所有路径生成测试用例");
        WidgetFactory.configureFormData(generateTestCaseForAllpathButton, null, new FormAttachment(0, 1), new FormAttachment(editDriverFileButton, -10), null);
        generateTestCaseForAllpathButton.setVisible(false);

        saveAndRunDriverFileButton = WidgetFactory.createButton(charCheckComp, SWT.PUSH);
        saveAndRunDriverFileButton.setText("保存并运行驱动文件");
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
                    String message = "使用方法:\n1.在驱动文件中声明和定义测试用例;\n2.点击\"保存驱动文件\"按钮保存;\n3.点击\"运行驱动文件\"运行\n\n" + "当下侧测试用例库中已填入值时,默认根据最后一列测试用例构造相应驱动文件;否则使用空测试用例生成驱动文件";
                    MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示信息", message);
                    box.open();
                    showDriverFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error("显示驱动文件错误，" + e.getMessage());
                }
                topTabFolder.setSelection(3);
                editDriverFileButton.setVisible(false);
                saveAndRunDriverFileButton.setVisible(true);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {}
        });


        // to be added 批量生成测试用例及统计信息写入文件
        generateTestCaseForAllpathButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                // add by chenruolin 设置生成用例后运行时不再重复保存用例
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
                                progressDisplayGUI.setInfo("正在生成所有未覆盖路径生成测试用例，请稍候...");

                            }

                        });
                        Display.getDefault().asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                TestModule tm = uatGui.getCurrentFunc();
                                int inFeasibleAmount = 0;

                                for (OnePath op : paths) {
                                    // added zhangxuzhou 2013-3-8 重新建立边的关系
                                    tm.getGraph().clearVisited();
                                    op.initpathedges();
                                    if (op.isInfeasible())
                                        inFeasibleAmount++;

                                }
                                // 创建文件 zhangxuzhou 2012-10-22
                                // 获取时间做文件名
                                String funcname = tm.getFuncName();// 2012-10-24 增加函数名
                                java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("MMDD_HHmmss");
                                java.util.Date time = new java.util.Date();
                                String timeString = new String(format.format(time));
                                String fileName = new String(tm.getBelongToFile().getTestResultDir() + File.separator + "result_Bat_" + timeString + ".txt");

                                File resFile = new File(fileName);

                                // 增加excel的导出 2012-11-4
                                String excelFileName = new String(tm.getBelongToFile().getTestResultDir() + File.separator + "info_Bat_" + timeString + ".xls");
                                File resExcelFile = new File(excelFileName);// 导出excel表格 2012-11-4
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
                                    book = Workbook.createWorkbook(resExcelFile);// 新建excel表格
                                    ws = book.createSheet("被测路径的统计信息", 0); // 新建一个sheet

                                    String[] title = {"路径编号", "路径长度", "分支数量", "条件节点表达式的数量为", "等值表达式的数量为", "非等值比较表达式的数量", "变量个数", "变量次数", "成功与否", "总回溯次数", "所用时间"};
                                    // 设置Excel表头
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
                                // 获得excel表的输入流，以便在bb中对excel进行写入

                                if (!resFile.exists())
                                    try {
                                        resFile.createNewFile();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                RandomAccessFile mm = null;
                                try {
                                    mm = new RandomAccessFile(fileName, "rw");
                                    mm.write((new String("当前被测函数是：" + funcname + "\r\n" + "为所有路径生成测试用例\r\n" + "\t其中可达路径数量为:" + (paths.size() - inFeasibleAmount) + "条" + "\t" + "不可达路径数量为:"
                                            + inFeasibleAmount)
                                            + "条" + "\r\n").getBytes());
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
                                        mm.seek(mm.length());// 到结尾处
                                        java.text.SimpleDateFormat format1 = new java.text.SimpleDateFormat("HH:mm:ss");
                                        time = new java.util.Date();
                                        timeString = new String(format1.format(time));
                                        mm.write(new String("\r\n" + seperator + "\r\n" + "现在为#" + opn + "号路径生成测试用例\r\n开始时间为：" + timeString + " \r\n;").getBytes());
                                        mm.seek(mm.length());// 到结尾处
                                        TestCaseNew tc = null;

                                        if (!op.getIsinfeasible()) {
                                            BranchBound bb = new BranchBound(op, mm, tm);
                                            bb.exec();
                                            // 对于生成的测试用例进行处理，生成成功就加入到表格中，否则记录生成失败
                                            tc = bb.getBranchBoudCase();// 分支限界生成的测试用例
                                            if (tc != null)
                                                tcTable.addValues(tc);// 所有的测试用例都添加上
                                            else
                                                count++;
                                            if (bb.getFailRun())
                                                failRunCount++;// 错误的测试用例个数


                                            // 写入excel的内容 2012-11-5
                                            try {
                                                int rowCount = ws.getRows();// 当前的行数 这个行数从1开始
                                                                            // 而坐标是从0开始
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
                                            mm.write(new String("~~~~~~~~~~~~~~~~~~" + "#" + opn + "号路径为不可达路径，跳过" + "~~~~~~~~~~~~~~~" + "\r\n\r\n").toString().getBytes());
                                            int rowCount = ws.getRows();// 当前的行数 这个行数从1开始 而坐标是从0开始
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
                                    errorMessage += "分支限界计算出现异常" + e.getMessage() + "\r\n";
                                    logger.error(errorMessage);
                                    e.printStackTrace();
                                    progressDisplayGUI.setTestProgressOver(0);
                                    progressDisplayGUI.terminateListener.setRunningThread(null);
                                    return;
                                }

                                try {
                                    long end = System.currentTimeMillis();
                                    mm.write(new String(seperator + "\r\n" + "全部生成完毕" + "，耗时" + (end - start) / 1000 + "." + (end - start) % 1000 + "秒\r\n" + seperator + "\r\n" + "处理完毕，可达路径中有"
                                            + count + "条路径没有生成测试用例,有" + failRunCount + "条路径生成错误的测试用例" + "\n" + "" + "Test Coverage for " + tm.getCoverRule().toString() + " is "
                                            + (int) (tm.getCoverSetList().get(tm.getCoverRule().getOutCon()).getCoverage() * 100) + "% for func " + tm.getFuncName()).getBytes());
                                    book.write();

                                    // chenruolin 2013-3-21 改变文件的格式。
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

                                Object[] options = {"是", "否"};
                                int response =
                                        JOptionPane.showOptionDialog(null, "对于所有路径的测试用例批量生成完毕。\r\n\t是否查看统计信息？", "完成", JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                                if (response == 0) {
                                    logger.debug("选择了查看文件");
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
                                    logger.debug("不查看");
                                }
                                // 关闭文件
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
                    logger.error("保存驱动文件失败" + e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error("保存驱动文件失败" + e.getMessage());
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
     * 在人工辅助界面上添加进度条
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
     * 创建显示显示测试用例的选项卡
     * add by xujiaoxian
     */
    public void createTestCaseLibWindow() {
        createTestCaseComposite();
        testCaseLibTabItem = WidgetFactory.createTabItem(testCaseProgressBarFolder, GUILanguageResource.getProperty("TestCaseTabItem"), null, testCaseLibComposite);
    }

    /**
     * 2013/05/30
     * 创建进度条选项卡
     * add by xujiaoxian
     */
    public void createProgressWindow() {
        createProgressBarComposite();
        progressBarTabItem = WidgetFactory.createTabItem(testCaseProgressBarFolder, GUILanguageResource.getProperty("TestProgress"), null, progressBarComposite);
    }

    /**
     * 2013/05/30
     * 创建显示进度条的组件
     * add by xujiaoxian
     */
    public void createProgressBarComposite() {
        progressBarComposite = WidgetFactory.createComposite(testCaseProgressBarFolder, SWT.BORDER);
        progressBarComposite.setLayout(new FormLayout());
        progressDisplayGUI = new UATProgressDisplayGUI(this.uatGui, progressBarComposite, testCaseProgressBarFolder, 1);
    }

    // 显示待测函数源代码
    private void showFunction() {
        TestModule tm = uatGui.getCurrentFunc();
        fileComp = WidgetFactory.createComposite(topTabFolder, SWT.NONE);
        sourceFileItem = WidgetFactory.createTabItem(topTabFolder, "手动输入", null, fileComp);
        try {
            new UATUncoveredElementGUI(tm, fileComp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createRandomComp() {
        randomComp = WidgetFactory.createComposite(topTabFolder, SWT.NONE);
        randomItem = WidgetFactory.createTabItem(topTabFolder, "区间随机", null, randomComp);
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
                        MessageBox box = WidgetFactory.createInfoMessageBox(uatGui.getShell(), "测试时异常", "函数 " + uatGui.getCurrentFunc().getFuncName() + "存在死循环，没有出口" + msg);
                        box.open();
                    }
                });
            }
        } else {
            uatGui.getCurrentFunc().setUnCoveredPathsFromUnCoveredElements();
        }
        paths = uatGui.getCurrentFunc().getManualUnCoveredPaths(); // paths是前台展示路径数据的集合 yumeng
        if (paths.size() == 0) {
            Text text = new Text(pathLeftComp, SWT.NONE);
            text.setText("未覆盖路径个数为0.");
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
            pathTable.getColumn(0).setText("序号");
            pathTable.getColumn(1).setText("路径区间");
            pathTable.getColumn(2).setText("编辑");
            pathTable.getColumn(3).setText("可达性");
            pathTable.getColumn(4).setText("路径轨迹");

            for (int i = 0; i < paths.size(); i++)
                new TableItem(pathTable, SWT.None);

            TableItem[] items = pathTable.getItems();
            for (int i = 0; i < items.length; i++) {
                items[i].setText(0, "#" + i);
                items[i].setText(1, "区间");
                TableEditor editor = new TableEditor(pathTable);
                Button button = new Button(pathTable, SWT.PUSH);
                button.setText("···");
                button.pack();
                editor.minimumWidth = button.getSize().x;
                editor.horizontalAlignment = SWT.CENTER;
                editor.setEditor(button, items[i], 2);
                if (!paths.get(i).isFeasiblePath())
                    items[i].setText(3, "不可达, 矛盾节点：" + paths.get(i).getCondictElement());

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
                        // 鼠标单击右键，3D鼠标
                        pathTable.setMenu(null);

                        // 检查是否点到了具体的节点
                        TableItem item = pathTable.getItem(new Point(e.x, e.y));

                        // 没有点到具体结点
                        if (item != null)
                            pathTable.setMenu(pathTableMenu);
                    }
                }
            });

            MenuItem item = new MenuItem(pathTableMenu, SWT.PUSH);
            item.setText("生成测试用例");
            item.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    int number = pathTable.getSelectionIndex();
                    OnePath path = paths.get(number);
                    // 分支限界
                    TestCaseNew tc = null;
                    String errorMessage = "";

                    // 分支限界改造后测试入口_140104_zmz_start
                    logger.info("***********&&&分支限界改造测试开始&&&**************");
                    VexNode tailNode = getFinalBranchNode(path);
                    ValueSet vs = tailNode.getValueSet();

                    ConstraintExtractor ce = path.getFlushedConstraintExtractor(); // 约束提取
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

                    HashSet<SymbolFactor> symbolSet = ce.getSymbolFactorsInConstr(vs); // 获取约束表达式中的所有符号
                    /**
                     * 开始分支限界生成测试用例
                     */
                    try {
                        NewBranchBound bbbb = new NewBranchBound(ce, vs, symbolSet, tailNode, false);
                        bbbb.generate();
                        tc = bbbb.getTc();
                    } catch (Exception e) {
                        errorMessage += "分支限界计算出现异常" + "\n" + e.getMessage() + "\r\n";
                        logger.error(errorMessage);
                    }

                    logger.info("生成的测试用例是" + tc);
                    tcTable.addValues(tc);// 向表中填入测试用例的值
                    MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示信息", "成功生成测试用例\r\n已加入测试用例列表中");
                    box.open();
                    testCaseProgressBarFolder.setSelection(0);
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {}
            });
        }

        sashForm2.setWeights(new int[] {12, 25});

    }

    // 显示待测函数源代码
    private void showDriverFile() throws IOException {
        TestModule tm = uatGui.getCurrentFunc();
        driverFileComp = WidgetFactory.createComposite(topTabFolder, SWT.NONE);
        driverFileItem = WidgetFactory.createTabItem(topTabFolder, uatGui.getCurrentFunc().getFuncName(), null, driverFileComp);
        sourceViewer = WidgetFactory.createSourceViewer(driverFileComp, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);

        try {
            java.util.List<TestCaseNew> list = tcTable.getTestCaseList();
            if (list.size() >= 1)
                et.buildFrame(list.get(list.size() - 1)); // 使用最后一个TestCase
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
        okButton.setText("运行");
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
                    messageBox.setText("提示");
                    messageBox.setMessage("是否需要查看未覆盖路径？");
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
                                        progressDisplayGUI.setInfo("正在生成未覆盖路径，请稍候...");

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
                MessageBox box = WidgetFactory.createInfoMessageBox(shell, "错误信息", "参数错误");
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
                // 修改界面的用这个线程去执行
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("正在执行测试用例，请稍候");
                        actionsGUI.clearOutputMessage();
                        uatGui.setStatusBarInfo("正在对函数 " + funcName + "运行指定测试用例...");
                        actionsGUI.addOutputMessage("正在对函数 " + funcName + "运行指定测试用例...");
                    }

                });
                try {
                    et.runTest();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            actionsGUI.doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            uatGui.setStatusBarInfo("对函数 " + funcName + "运行指定测试用例结束");
                            actionsGUI.addOutputMessage("对函数 " + funcName + "运行指定测试用例结束");
                            String info = "运行指定测试用例完成";
                            if (tm.getFuncVar().hasFileVar())
                                info += "\n参数或全局变量中有文件指针，请用户自行校验测试结果";
                            MessageBox box = WidgetFactory.createInfoMessageBox(uatGui.getShell(), "提示信息", info);
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
                            actionsGUI.addOutputMessage("函数 " + funcName + "驱动文件编辑错误" + msg);
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
                            actionsGUI.addOutputMessage("函数 " + funcName + "驱动文件编辑错误" + msg);
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
     * @param flag 0:驱动文件生成错误 1:驱动文件编辑错误
     */
    private void createErrorMessageBox(Throwable e, int flag) {
        String message;
        if (flag == 0)
            message = "驱动文件生成错误," + e.getMessage();
        else
            message = "驱动文件编辑错误," + e.getMessage();
        MessageBox box = WidgetFactory.createErrorMessageBox(uatGui.getShell(), "错误信息", message);
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
