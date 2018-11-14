package unittest.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderAdapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.xml.sax.SAXException;

import softtest.interpro.c.InterContext;
import unittest.ConfigManage.SystemConfigManager;
import unittest.editor.color.SyntaxColorPainter;
import unittest.gui.helper.FileCTabItem;
import unittest.gui.helper.FileTabManager;
import unittest.gui.helper.FileTreeNode;
import unittest.gui.helper.FunctionTreeNode;
import unittest.gui.helper.Resource;
import unittest.gui.helper.TreeNode;
import unittest.gui.helper.WidgetFactory;
import unittest.gui.listener.CTabItemDisposeListener;
import unittest.gui.listener.ProjectViewTreeExpandListener;
import unittest.gui.listener.ProjectViewTreeMouseListener;
import unittest.gui.listener.ProjectViewTreeSelectionListener;
import unittest.gui.listener.TestCaseTreeMouseListener;
import unittest.instrumentation.measurement.message.CoverageMessage;
import unittest.localization.GUILanguageResource;
import unittest.module.seperate.NoExitMethodException;
import unittest.module.seperate.TestModule;
import unittest.testcase.generate.util.TestCaseNew;
import unittest.testcase.management.news.TestCaseLibManagerNew;
import unittest.util.AnalysisFile;
import unittest.util.Config;
import unittest.util.CoverRule;
import unittest.util.ExtractOptions;
import unittest.util.Project;
import unittest.util.RecordToLogger;
import unittest.util.SerializableAnalysisFileInfo;
import edu.emory.mathcs.backport.java.util.Arrays;


/**
 * This Class is the main GUI of our project --- UAT,
 * This is a project to delvelop a tools that automatic
 * test the C/C++ code
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>NO_BACKGROUND, NO_FOCUS, NO_MERGE_PAINTS, NO_REDRAW_RESIZE, NO_RADIO_GROUP, EMBEDDED,
 * DOUBLE_BUFFERED</dd>
 * <dt><b>Events:</b></dt>
 * <dd>(none)</dd>
 * </dl>
 * <p>
 * Note: The <code>NO_BACKGROUND</code>, <code>NO_FOCUS</code>, <code>NO_MERGE_PAINTS</code>, and
 * <code>NO_REDRAW_RESIZE</code> styles are intended for use with <code>Canvas</code>. They can be
 * used with <code>Composite</code> if you are drawing your own, but their behavior is undefined if
 * they are used with subclasses of <code>Composite</code> other than <code>Canvas</code>.
 * </p>
 * <p>
 * This class may be subclassed by custom control implementors who are building controls that are
 * constructed from aggregates of other controls.
 * </p>
 * 
 * @author joaquin(孙华衿)
 * @see WidgetFactory
 * @see CodemonNewProjectGui
 * @see CodemonNewTestCaseGui
 * @see CodemonProjectPropertiesGui
 * @see CodemonChangeTestCaseDescriptionGui
 * @see CodemonAboutGui
 * 
 * @see CTabItemDisposeListener
 * @see FileViewTreeMouseListener
 * @see PointViewMouseListener
 * @see ProjectViewTreeListener
 * @see ProjectPropertiesListener
 */
public class UATGUI {
    boolean isContinue = true;// 判断单步执行是否继续,added by hanchunxiao
    int currentTestCasecount = 0;// 当前第几个用例，如果是最后一个，则不再提示用户是否继续执行
    private static Logger logger = Logger.getLogger(UATGUI.class);

    private static SystemConfigManager systemConfigManager;
    private Shell shell = null;

    // The Main structure of the GUI.
    private Menu mainMenu = null;
    private ToolBar toolBar = null;

    private Composite composite = null;
    private Composite leftComposite = null;
    private Composite rightComposite = null;
    private Composite bottomComposite = null;
    private Composite middleComposite = null;

    // add by cm, 分割框 及其权重设置
    // 左、右、中、中下、下这五个composite用分割框进行布局，而不再用FormLayout进行布局
    SashForm sashForm;
    SashForm sashForm2;
    SashForm sashForm3;
    SashForm sashForm4;
    private int[] weights1 = new int[] {15, 1};
    private int[] weights2 = new int[] {3, 10};
    private int[] weights3 = new int[] {7, 3};
    private int[] weights4 = new int[] {1, 15};

    // The children of mainMenu
    private MenuItem fileMenuItem = null;
    private MenuItem testMenuItem = null;

    // private MenuItem testCaseMenuItem = null;
    private MenuItem showMenuItem = null;
    private MenuItem helpMenuItem = null;
    private MenuItem settingMenuItem = null;

    // The children of fileMenuItem
    private Menu fileMenu = null;
    private MenuItem openProjectMenuItem = null;
    private Menu openProjectMenu = null;
    private MenuItem openMenuItem = null;
    private MenuItem openCurrentProjectMenuItem = null;
    private Menu openCurrentProjectMenu = null;
    private MenuItem openCurrentProjectMenuItem1 = null;
    private MenuItem openCurrentProjectMenuItem2 = null;
    private MenuItem openCurrentProjectMenuItem3 = null;
    private MenuItem openCurrentProjectMenuItem4 = null;
    private MenuItem openCurrentProjectMenuItem5 = null;
    private MenuItem openExampleProjectMenuItem = null;
    private MenuItem newProjectMenuItem = null;
    private MenuItem closeProjectMenuItem = null;
    private MenuItem fileSeparatorMenuItem1 = null;
    private MenuItem saveProjectMenuItem = null;
    private MenuItem saveProjectAsMenuItem = null;
    private MenuItem fileSeparatorMenuItem2 = null;
    private MenuItem reloadProjectMenuItem = null;
    private MenuItem projectAttributeMenuItem = null;
    private MenuItem exportMenuItem = null;
    private MenuItem fileSeparatorMenuItem3 = null;
    private MenuItem printProjectMenuItem = null;
    private Menu printProjectMenu = null;
    private MenuItem printMenuItem = null;
    private MenuItem fastPrintMenuItem = null;
    private MenuItem printViewMenuItem = null;
    private MenuItem printSettingMenuItem = null;
    private MenuItem fileSeparatorMenuItem4 = null;
    private MenuItem exitMenuItem = null;

    // the children of the test menu item
    private Menu testMenu = null;
    private MenuItem buildTestEnvMenuItem = null;
    private MenuItem moduleSeparateMenuItem = null;
    // private MenuItem coverRuleSelectMenuItem = null;
    private MenuItem autoTestMenuItem = null;
    private Menu autoTestMenu = null;
    private MenuItem randomTestBaseInputDomainMenuItem = null;
    private MenuItem randomTestBaseInputDomainAndPathMenuItem = null;
    private MenuItem testBasedRandomAndPathMenuItem = null;
    private MenuItem bugLinkMenuItem = null;
    private MenuItem batchProcessMenuItem = null;
    private MenuItem testCaseMenuItem = null;

    // the children of the testCaseMenuItem
    private Menu testCaseMenu = null;
    private MenuItem showTestCaselibMenuItem = null;
    private MenuItem importTestCaseFromLibMenuItem = null;
    private MenuItem packTestCaseMenuItem = null;

    // the children of the show Menu item
    private Menu showMenu = null;
    private MenuItem showLogFileMenuItem = null;
    private MenuItem showTestCaseFileMenuItem = null;
    private MenuItem packFileMenuItem = null;
    private MenuItem testTableMenuItem = null;
    private MenuItem showStubFileMenuItem = null;
    private MenuItem showSourceCodeMenuItem = null;
    private MenuItem showDriverFileMenuItem = null;
    private MenuItem showRegressionTestFileMenuItem = null;
    private MenuItem showInstruFileMenuItem = null;
    public MenuItem showSoftwareMetricMenuItem = null;

    // The Children of helpMenuItem
    private Menu helpMenu = null;
    private MenuItem aboutMenuItem = null;
    private MenuItem helpContentsMenuItem = null;
    private MenuItem contactUsMenuItem = null;
    private MenuItem updateMenuItem = null;

    // The Children of the setting MenuItem;
    private Menu settingMenu = null;
    private MenuItem systemSettingMenuItem = null;
    private Menu systemSettingMenu = null;
    private MenuItem maxRangeSettingMenuItem2 = null;
    private MenuItem toolPathSettingMenuItem = null;
    private MenuItem filePathSettingMenuItem = null;
    private MenuItem shortCutsSettingMenuItem = null;
    private MenuItem AdvanceSettingMenuItem = null;
    private MenuItem projectSettingMenuItem = null;
    private Menu projectSettingMenu = null;
    private MenuItem coverRuleSelectMenuItem = null;
    private MenuItem testCaseManagementSettingMenuItem = null;
    private MenuItem manualVariableSettingMenuItem = null;
    private MenuItem preferenceSettingMenuItem = null;
    private Menu preferenceSettingMenu = null;
    private MenuItem backgroundSettingMenuItem = null;
    private MenuItem fontSettingMenuItem = null;
    private MenuItem languageSettingMenuItem = null;
    // private MenuItem compilerSettingMenuItem = null;
    private MenuItem compilerSettingMenuItem2 = null;
    private MenuItem versionSettingMenuItem = null;
    private Menu versionSettingMenu = null;
    private MenuItem formalVersionMenuItem = null;
    private MenuItem debugVersionMenuItem = null;

    // The Children of the toolBar
    private ToolItem newProjectToolItem = null;
    private ToolItem openProjectToolItem = null;
    private ToolItem closeProjectToolItem = null;

    private ToolItem saveProjectToolItem = null;
    private ToolItem saveFileToolItem = null;
    private ToolItem staticAnalysisToolItem = null;
    private ToolItem batchProcessToolItem = null;
    private ToolItem showTestReportToolItem = null;
    private ToolItem showTestResultToolItem = null;
    private ToolItem softwareMetricToolItem = null;

    public ToolItem showCoverageWindowToolItem = null;
    public ToolItem manualInterventionToolItem = null;

    public ToolItem InputEditorToolItem = null;
    private ToolItem runSelectedTestCaseItem = null;
    private ToolItem coverRuleSelectToolItem = null;
    private ToolItem bugLinkToolItem = null;


    public ToolItem showTestCaseFileToolItem = null;
    private ToolItem showTestStubFileToolItem = null;
    private ToolItem showTestDriverFileToolItem = null;
    private ToolItem showTestRegressionFileToolItem = null;
    private ToolItem showInstrumentFileToolItem = null;
    private ToolItem showSourceCodeFileToolItem = null;
    private ToolItem showLogFileToolItem = null;

    private Composite coverageComposite = null;
    private Composite testCaseLibComposite = null;
    private Composite testCaseLibComposite2 = null;
    private Composite testCaseTreeComposite = null;
    private Composite processComposite = null;
    private UATProgressDisplayGUI progressDisplayGUI = null;

    // function coverage composite
    private Composite funcCovComposite = null;
    public CLabel funcCLabel = null;
    private ProgressBar funcBlockCovProBar = null;
    private ProgressBar funcDecisionCovProBar = null;
    private ProgressBar funcMcDcCovProBar = null;
    private CLabel funcBlockCovCLabel = null;
    private CLabel funcDecisionCovCLabel = null;
    private CLabel funcMCDCCovCLabel = null;

    // 添加用例编号和运行通过状态之间的映射关系。状态值有：0：无；1：通过；2：错误；3，未知。
    public Map<Long, Integer> map = new HashMap<Long, Integer>();

    // the output message show in the window
    private static Text outputMessageText = null;
    private String outputMsg = "";

    // the output window of the GUI,include the output message window and the coverage info window
    private TabFolder outputTabFolder = null;

    // the output message tabItem
    private TabItem outputMessageTabItem = null;

    // the coverage information tabItem
    private TabItem coverageInfoTabItem = null;

    // the testCaseLib tabItem
    private TabItem testCaseLibTabItem = null;

    // the process tabItem
    private TabItem processTabItem = null;

    private Display display = null;

    // the project tree window of the GUI, shows the project tree.
    private TabFolder projectTabFolder = null;

    // 生成projectTabItem时使用
    private Composite treeComposite = null;

    // the project tree tabItem
    private TabItem projectTabItem = null;

    // The Children of the leftComposite
    public Tree projectViewTree = null;

    // The Children bottomComposite
    private CLabel statusBar = null;
    private String statusBarInfo = new String();

    // the current project in the GUI
    private Project currentProject = null;

    // the selected File in the GUI
    private AnalysisFile currentFile = null;

    // the selected Function in the GUI
    private TestModule currentFunc = null;

    // the selected testcase in the GUI
    private long currentTestCaseID;

    // the selected cover criteria
    private CoverRule currentCoverCriteria = new CoverRule();

    // show the testcase
    private Tree TestCaseTree = null;
    private TableViewer tableViewer = null;

    private UATTestCaseTable testCaseTable = null;
    // The Children of codeTabFolder
    public ArrayList<FileCTabItem> items = null;

    // The Children of middleComposite
    public CTabFolder codeCTabFolder = null;
    private Composite midBottomComposite = null;


    // the cover criteria combobox for selection
    private TreeNode projectViewTreeRoot = null;

    public UATGUIActions actionsGUI = new UATGUIActions(this);
    // 语法上色
    private SyntaxColorPainter syntaxColorPainter = null;

    /**
     * 判断工具栏上的故障定位按钮是否是可用状态
     * 
     * @return
     */
    public boolean isBugLinkToolItemEnabled() {
        return bugLinkToolItem.isEnabled();
    }

    /**
     * The UAT Class has a projectViewTree that show all
     * test cpp and its functions.
     * 
     * @return the projectViewTree
     */
    public Tree getProjectViewTree() {
        return this.projectViewTree;
    }

    public Tree getTestCaseTree() {
        return this.TestCaseTree;
    }

    public Logger getLogger() {
        return this.logger;
    }

    public SystemConfigManager getSystemConfigManager() {
        return this.systemConfigManager;
    }

    public AnalysisFile getCurrentFile() {
        return currentFile;
    }

    public TabFolder getOutputTabFolder() {
        return this.outputTabFolder;
    }

    public UATProgressDisplayGUI getUATProgressDisplayGUI() {
        return progressDisplayGUI;
    }

    public MenuItem getShowSoftwareMetricMenuItem() {
        return showSoftwareMetricMenuItem;
    }

    public void setCurrentFile(AnalysisFile currentFile) {
        this.currentFile = currentFile;
        if (currentFile != null && !currentFile.isHasAnalysised())
            staticAnalysisToolItem.setEnabled(true);
    }

    public TestModule getCurrentFunc() {
        return currentFunc;
    }

    public long getCurrentTestCaseID() {
        return currentTestCaseID;
    }

    public CoverRule getCurrentCoverCriteria() {
        return currentCoverCriteria;
    }

    public void setCurrentCoverCriteria(CoverRule currentCoverCriteria) {
        this.currentCoverCriteria.setCoverRule(currentCoverCriteria);
    }

    public void setCurrentFunc(TestModule currentFunc) {
        this.currentFunc = currentFunc;
    }

    public void setCurrentTestCaseID(long testCaseID) {
        this.currentTestCaseID = testCaseID;
    }

    // add by cai min, 2011/5/23, bugLinkMenuItem由TableTree控制
    public void setBugLinkMenuItemEnabled(boolean value) {
        bugLinkMenuItem.setEnabled(value);
    }

    public void setshowSourceCodeMenuItem(boolean value) {
        showSourceCodeMenuItem.setEnabled(value);
    }

    public void setshowSourceCodeFileToolItem(boolean value) {
        showSourceCodeFileToolItem.setEnabled(value);
    }

    public UATTestCaseTable getTestCaseTable() {
        return testCaseTable;
    }


    public void setBugLinkToolItemEnabled(boolean value) {
        bugLinkToolItem.setEnabled(value);
    }

    /**
     * 
     * @return The shell of UATGUI
     */
    public Shell getShell() {
        return this.shell;
    }

    public void setCurrentProject(Project project) {
        this.currentProject = project;
    }

    public Project getCurrentProject() {
        return this.currentProject;
    }

    /**
     * Set the status bar infomation
     * 
     */
    public void setStatusBarInfo(String info) {
        statusBar.setText(info);
    }

    public SashForm getSashForm() {
        return this.sashForm;
    }

    public SashForm getSashForm2() {
        return this.sashForm2;
    }

    public SashForm getSashForm3() {
        return this.sashForm3;
    }

    public Composite getLeftComposite() {
        return this.leftComposite;
    }

    public int[] getweights3() {
        return this.weights3;
    }

    public int[] getweights2() {
        return this.weights2;
    }

    public Text getoutputMessageText() {
        return outputMessageText;
    }

    public boolean getSaveFile() {
        return saveFileToolItem.isEnabled();
    }

    public String getOutputMsg() {
        return outputMsg;
    }

    public void setOutputMsg(String outputMsg) {
        this.outputMsg = outputMsg;
    }

    /**
     * 设置函数语句覆盖的覆盖率
     */
    public void setFuncBlockCoverage(int cover, int all) {

        funcBlockCovCLabel.setEnabled(true);
        funcBlockCovProBar.setEnabled(true);
        int temp = all;
        if (temp == 0)
            temp = 1;

        String criteria = GUILanguageResource.getProperty("CoverCritertia");
        String[] criteras = criteria.split(";");

        String data = criteras[0];

        double coverage = (cover * 100) / temp;

        data = data + " " + coverage + "% (" + cover + "/" + all + ")";
        funcBlockCovCLabel.setText(data);


        // Refresh the progressBar.
        this.funcBlockCovProBar.setMinimum(0);
        this.funcBlockCovProBar.setMaximum(all);
        this.funcBlockCovProBar.setSelection(cover);
        this.funcBlockCovProBar.setToolTipText(coverage + "%");

        if (all == 0) {
            funcBlockCovCLabel.setEnabled(false);
            funcBlockCovProBar.setEnabled(false);
            return;
        }
    }

    /**
     * 设置函数语句覆盖下未被覆盖的覆盖率，测试时使用
     * 
     * @param uncoverage4PathSelection
     * @param uncoverage4Testcase
     */
    public void setFuncBlockUnCoverage(float uncoverage4PathSelection, float uncoverage4Testcase) {
        String info = funcBlockCovCLabel.getText();
        NumberFormat numFormater = NumberFormat.getNumberInstance();
        numFormater.setMaximumFractionDigits(2);
        info += "[选路:" + numFormater.format(uncoverage4PathSelection * 100) + "%, 测试用例:" + numFormater.format(uncoverage4Testcase * 100) + "%]";
        funcBlockCovCLabel.setText(info);
    }

    /**
     * 设置分支覆盖率
     * 
     * @param cover
     * @param all
     */
    public void setFuncDecisionCoverage(int cover, int all) {

        funcDecisionCovCLabel.setEnabled(true);
        funcDecisionCovProBar.setEnabled(true);
        int temp = all;
        if (temp == 0)
            temp = 1;

        String criteria = GUILanguageResource.getProperty("CoverCritertia");
        String[] criteras = criteria.split(";");

        String data = criteras[1];

        double coverage = (cover * 100) / temp;
        data = data + " " + coverage + "% (" + cover + "/" + all + ")";
        funcDecisionCovCLabel.setText(data);

        // Refresh the progressBar.
        this.funcDecisionCovProBar.setMinimum(0);
        this.funcDecisionCovProBar.setMaximum(all);
        this.funcDecisionCovProBar.setSelection(cover);
        this.funcDecisionCovProBar.setToolTipText(coverage + "%");
        if (all == 0) {
            funcDecisionCovCLabel.setEnabled(false);
            funcDecisionCovProBar.setEnabled(false);
            return;
        }
    }

    /**
     * 设置分支覆盖下未被覆盖的覆盖率，测试时使用
     * 
     * @param uncoverage4PathSelection
     * @param uncoverage4Testcase
     */
    public void setFuncDecisionUnCoverage(float uncoverage4PathSelection, float uncoverage4Testcase) {
        String info = funcDecisionCovCLabel.getText();
        NumberFormat numFormater = NumberFormat.getNumberInstance();
        numFormater.setMaximumFractionDigits(2);
        info += "[选路:" + numFormater.format(uncoverage4PathSelection * 100) + "%, 测试用例:" + numFormater.format(uncoverage4Testcase * 100) + "%]";
        funcDecisionCovCLabel.setText(info);
    }

    /**
     * 设置McDc覆盖率
     * 
     * @param per
     */
    public void setFuncMcDcCoverage(int cover, int all) {

        funcMcDcCovProBar.setEnabled(true);
        funcMcDcCovProBar.setEnabled(true);
        int temp = all;
        if (temp == 0)
            temp = 1;

        String criteria = GUILanguageResource.getProperty("CoverCritertia");
        String[] criteras = criteria.split(";");

        String data = criteras[2];

        double coverage = (cover * 100) / temp;
        data = data + " " + coverage + "% (" + cover + "/" + all + ")";
        funcMCDCCovCLabel.setText(data);

        // Refresh the progressBar.
        this.funcMcDcCovProBar.setMinimum(0);
        this.funcMcDcCovProBar.setMaximum(all);
        this.funcMcDcCovProBar.setSelection(cover);
        this.funcMcDcCovProBar.setToolTipText(coverage + "%");
        if (all == 0) {
            funcMCDCCovCLabel.setEnabled(false);
            funcMcDcCovProBar.setEnabled(false);
            return;
        }
    }

    /**
     * 设置MC/DC覆盖准则下未被覆盖的覆盖率，测试时使用
     * 
     * @param uncoverage4PathSelection
     * @param uncoverage4Testcase
     */
    public void setFuncMcDcUnCoverage(float uncoverage4PathSelection, float uncoverage4Testcase) {
        String info = funcMCDCCovCLabel.getText();
        NumberFormat numFormater = NumberFormat.getNumberInstance();
        numFormater.setMaximumFractionDigits(2);
        info += "[选路:" + numFormater.format(uncoverage4PathSelection * 100) + "%, 测试用例:" + numFormater.format(uncoverage4Testcase * 100) + "%]";
        funcMCDCCovCLabel.setText(info);
    }


    /**
     * This function Show the contents of the file.
     * 
     * @param file
     * @param styledText
     * @return true if successed show the code contents, else return false.
     */
    public boolean setCodeContents(File file, SourceViewer sourceViewer, boolean isSourceCode) {
        boolean returnVal = true;
        if (file.isDirectory()) {
            returnVal = false;
        } else {
            syntaxColorPainter = new SyntaxColorPainter(sourceViewer.getTextWidget());
            try {
                FileInputStream fin;

                fin = new FileInputStream(file);

                int ch;
                StringBuffer data = new StringBuffer();
                while ((ch = fin.read()) != -1) {
                    data.append((char) ch);
                }
                if (data != null) {
                    String contents = new String(data.toString().getBytes("ISO-8859-1"), "GBK");
                    sourceViewer.setDocument(new Document(contents));
                    syntaxColorPainter.processPaint(0, contents.length());
                    if (isSourceCode) {
                        sourceViewer.setEditable(true);
                    } else
                        sourceViewer.setEditable(false);
                }
                fin.close();
                returnVal = true;
            } catch (Exception e) {
            }
        }
        return returnVal;
    }

    /**
     * @param the main entry point of the GUI
     * @author joaquin
     */
    public void go(String[] args) {
        final Display display = Display.getDefault();


        boolean exception = false;

        try {
            Config.loadConfigFile("./config/config.xml");
            GUILanguageResource.setLanguage(Config.Language);
            GUILanguageResource.loadProperties();
            systemConfigManager = new SystemConfigManager();

        } catch (ParserConfigurationException e) {
            exception = true;
            RecordToLogger.recordExceptionInfo(e, logger);
            e.printStackTrace();
        } catch (SAXException e) {
            exception = true;
            RecordToLogger.recordExceptionInfo(e, logger);
            e.printStackTrace();
        } catch (IOException e) {
            exception = true;
            RecordToLogger.recordExceptionInfo(e, logger);
            e.printStackTrace();
        } finally {
            if (exception) {
                Config.Language = "en_US";
                GUILanguageResource.setLanguage(Config.Language);
                try {
                    GUILanguageResource.loadProperties();
                } catch (IOException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    e.printStackTrace();
                }
            }
        }

        createShell(display);
        dealEvent();

        // 设置覆盖率信息
        CoverageMessage.init(display, this);

        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }

    /**
     * This function create the Shell and all its children.
     * 
     * @param display the Display of UAIGUI
     */
    private void createShell(Display display) {
        shell = new Shell();
        shell.setText(GUILanguageResource.getProperty("UAT"));
        shell.setImage(Resource.UATImageTask);
        shell.setBounds(50, 50, 800, 600);
        shell.setLayout(new FormLayout());
        shell.setMaximized(true);

        this.display = display;
        createToolBar();

        // LeftComposite、MiddleComposite、MidBottomComposite、RightComposite、BottomComposite使用分割框进行布局
        composite = WidgetFactory.createComposite(shell, SWT.BORDER);
        composite.setLayout(new FillLayout());// 注意这里不要用FormLayout，否则几个composite在窗口拉小时不能全部显示
        WidgetFactory.configureFormData(composite, new FormAttachment(0, 0), new FormAttachment(toolBar), new FormAttachment(100, 0), new FormAttachment(100, 0));

        sashForm = new SashForm(composite, SWT.VERTICAL);
        sashForm2 = new SashForm(sashForm, SWT.HORIZONTAL);

        createLeftComposite();

        sashForm3 = new SashForm(sashForm2, SWT.VERTICAL);

        createMiddleComposite();
        createMidBottomComposite();
        createRightComposite();
        createBottomComposite();

        sashForm.setWeights(weights1);
        sashForm2.setWeights(weights2);
        sashForm3.setWeights(weights3);

        createMenu();

    }

    /**
     * This function create the middle composite of CodemonGui.
     * The CodemonGui structure is like this:
     * shell
     * ToolBar
     * leftComposite
     * projectViewTree
     * rightComposite
     * pointViewTree
     * fileViewTree
     * bottomComposite
     * statusBar
     * middleComposite
     * code
     * code coverage information
     * 
     * @see #createShell
     * @see #createRightComposite
     * @see #createToolBar
     * @see #createBottomComposite
     * @see #createMiddleComposite
     * 
     * @see #createCoverageInfoComposite
     * @see #createCodeTabFolder
     */
    public void createMiddleComposite() {
        middleComposite = WidgetFactory.createComposite(sashForm3, SWT.BORDER);
        middleComposite.setLayout(new FormLayout());
        createCodeTabFolder();
    }

    /**
     * This function create the createMidBottomComposite
     * 
     * @see #createMiddleComposite()
     */
    public void createMidBottomComposite() {
        midBottomComposite = WidgetFactory.createComposite(sashForm3, SWT.NONE);
        midBottomComposite.setLayout(new FormLayout());

        outputTabFolder = WidgetFactory.createTabFolder(midBottomComposite, SWT.NONE);
        outputTabFolder.setToolTipText(GUILanguageResource.getProperty("OutputTab"));
        outputTabFolder.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {

            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (outputTabFolder.getSelectionIndex() == 2) {
                    doShowTestCasesTree();
                    doShowAvaiableTestCases();
                }
            }

        });

        outputTabFolder.addMouseListener(new MouseListener() {

            @Override
            public void mouseUp(MouseEvent arg0) {}

            @Override
            public void mouseDown(MouseEvent arg0) {}

            @Override
            public void mouseDoubleClick(MouseEvent arg0) {
                if (sashForm3.getMaximizedControl() == midBottomComposite) {
                    sashForm3.setMaximizedControl(null);
                    sashForm.setWeights(weights1);
                    sashForm2.setWeights(weights2);
                } else {
                    sashForm3.setMaximizedControl(midBottomComposite);
                    sashForm.setWeights(new int[] {1, 0});
                    sashForm2.setWeights(new int[] {0, 1});
                }
            }
        });

        WidgetFactory.configureFormData(outputTabFolder, new FormAttachment(0, 0), new FormAttachment(0, 0), new FormAttachment(100, 0), new FormAttachment(100, 0));

        createOutputMessageWindow();
        createCoverageInfoWindow();
        createTestCaseLibWindow();
        createProcessWindow();
    }

    private void createProcessWindow() {
        createProcessComposite();
        processTabItem = WidgetFactory.createTabItem(outputTabFolder, GUILanguageResource.getProperty("TestProgress"), null, processComposite);
    }

    private void createProcessComposite() {
        funcBlockCovProBar.setMinimum(0);
        funcBlockCovProBar.setMaximum(100);
        processComposite = WidgetFactory.createComposite(outputTabFolder, SWT.BORDER);
        processComposite.setLayout(new FormLayout());
        progressDisplayGUI = new UATProgressDisplayGUI(this, processComposite, outputTabFolder, 3);
    }


    /**
     * this function create the CoverageInfoWindow in the
     * outputtabFolder
     * 
     * @see #createOutputMessageWindow()
     * @see #createCoverageInfoWindow()
     * @see #createMidBottomComposite()
     */
    private void createTestCaseLibWindow() {
        createTestCaseLibComposite();
        testCaseLibTabItem = WidgetFactory.createTabItem(outputTabFolder, GUILanguageResource.getProperty("TestCaseLib"), null, testCaseLibComposite2);
    }

    /**
     * 测试用例显示模块
     * 
     * created by Yaoweichang on 2015-04-16 下午5:15:24
     */
    private void createTestCaseLibComposite() {
        testCaseLibComposite = WidgetFactory.createComposite(outputTabFolder, SWT.BORDER);
        testCaseLibComposite.setLayout(new FillLayout());
        testCaseLibComposite2 = WidgetFactory.createComposite(outputTabFolder, SWT.BORDER);
        testCaseLibComposite2.setLayout(new FillLayout());
        testCaseLibComposite2.setVisible(true);
        testCaseLibComposite.setVisible(false);

        sashForm4 = new SashForm(testCaseLibComposite2, SWT.HORIZONTAL);
        testCaseTreeComposite = WidgetFactory.createComposite(sashForm4, SWT.BORDER);
        testCaseTreeComposite.setLayout(new FillLayout());
        createTestCaseViewTree();

        testCaseTable = new UATTestCaseTable(sashForm4, this);
        tableViewer = new TableViewer(testCaseLibComposite, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);

        Table table = tableViewer.getTable();
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        table.setLayout(new FillLayout());

        final TableColumn newColumnTableColumn = new TableColumn(table, SWT.NONE | SWT.CHECK);
        newColumnTableColumn.setWidth(100);
        newColumnTableColumn.setText("测试用例编号");

        final TableColumn newColumnTableColumn_1 = new TableColumn(table, SWT.NONE);
        newColumnTableColumn_1.setWidth(220);
        newColumnTableColumn_1.setText("函数名称");

        final TableColumn newColumnTableColumn_2 = new TableColumn(table, SWT.NONE);
        newColumnTableColumn_2.setWidth(230);
        newColumnTableColumn_2.setText("参数");

        final TableColumn newColumnTableColumn_3 = new TableColumn(table, SWT.NONE);
        newColumnTableColumn_3.setWidth(220);
        newColumnTableColumn_3.setText("全局变量");

        final TableColumn newColumnTableColumn_4 = new TableColumn(table, SWT.NONE);
        newColumnTableColumn_4.setWidth(100);
        newColumnTableColumn_4.setText("返回值");

        final TableColumn newColumnTableColumn_5 = new TableColumn(table, SWT.NONE);
        newColumnTableColumn_5.setWidth(130);
        newColumnTableColumn_5.setText("覆盖准则");

        sashForm4.setWeights(weights4);
    }


    /**
     * this function create the CoverageInfoWindow in the
     * outputtabFolder
     * 
     * @see #createOutputMessageWindow()
     * @see #createMidBottomComposite()
     */
    private void createCoverageInfoWindow() {
        createCoverageComposite();
        coverageInfoTabItem = WidgetFactory.createTabItem(outputTabFolder, GUILanguageResource.getProperty("CoverageTab"), null, coverageComposite);
    }

    private void createCoverageComposite() {
        coverageComposite = WidgetFactory.createComposite(outputTabFolder, SWT.BORDER);
        coverageComposite.setLayout(new FormLayout());
        createFuncCovComposite();
    }

    /**
     * this function will create the file coverage information
     * 
     * @see #createCoverageInfoWindow()
     * @see
     */
    private void createFuncCovComposite() {
        funcCovComposite = WidgetFactory.createComposite(coverageComposite, SWT.BORDER);
        funcCovComposite.setLayout(new FormLayout());


        WidgetFactory.configureFormData(funcCovComposite, new FormAttachment(0, 0), new FormAttachment(0, 0), new FormAttachment(100, 0), new FormAttachment(100, 0));
        funcCLabel = WidgetFactory.createCLabel(funcCovComposite, SWT.NONE, GUILanguageResource.getProperty("FuncCoverage"));
        WidgetFactory.configureFormData(funcCLabel, new FormAttachment(0, 0), new FormAttachment(0, 0), new FormAttachment(100, 0), new FormAttachment(45, 0));
        funcBlockCovCLabel = WidgetFactory.createCLabel(funcCovComposite, SWT.NONE, "");
        WidgetFactory.configureFormData(funcBlockCovCLabel, new FormAttachment(0, 0), new FormAttachment(funcCLabel, 0), new FormAttachment(40, 0), new FormAttachment(60, 0));
        funcDecisionCovCLabel = WidgetFactory.createCLabel(funcCovComposite, SWT.NONE, "");

        WidgetFactory.configureFormData(funcDecisionCovCLabel, new FormAttachment(0, 0), new FormAttachment(funcBlockCovCLabel, 0), new FormAttachment(40, 0), new FormAttachment(75, 0));

        funcMCDCCovCLabel = WidgetFactory.createCLabel(funcCovComposite, SWT.NONE, "");

        WidgetFactory.configureFormData(funcMCDCCovCLabel, new FormAttachment(0, 0), new FormAttachment(funcDecisionCovCLabel, 0), new FormAttachment(40, 0), new FormAttachment(90, 0));


        funcBlockCovProBar = WidgetFactory.createHProgressBar(funcCovComposite, 0, 0, 100);
        funcBlockCovProBar.setForeground(Resource.progressBarColor);

        WidgetFactory.configureFormData(funcBlockCovProBar, new FormAttachment(funcBlockCovCLabel, 0), new FormAttachment(funcCLabel, 0), new FormAttachment(100, 0), new FormAttachment(60, 0));

        funcDecisionCovProBar = WidgetFactory.createHProgressBar(funcCovComposite, 0, 0, 100);
        funcDecisionCovProBar.setForeground(Resource.progressBarColor);


        WidgetFactory.configureFormData(funcDecisionCovProBar, new FormAttachment(funcDecisionCovCLabel, 0), new FormAttachment(funcBlockCovProBar, 0), new FormAttachment(100, 0), new FormAttachment(
                75, 0));

        funcMcDcCovProBar = WidgetFactory.createHProgressBar(funcCovComposite, 0, 0, 100);
        funcMcDcCovProBar.setForeground(Resource.progressBarColor);


        WidgetFactory.configureFormData(funcMcDcCovProBar, new FormAttachment(funcMCDCCovCLabel, 0), new FormAttachment(funcDecisionCovProBar, 0), new FormAttachment(100, 0),
                new FormAttachment(90, 0));

        // 初始化为0;
        this.setFuncBlockCoverage(0, 0);
        this.setFuncDecisionCoverage(0, 0);
        this.setFuncMcDcCoverage(0, 0);
    }


    /**
     * this function create the Output message Window in the
     * outputtabFolder
     * 
     * @see #createCoverageInfoWindow()
     * @see #createMidBottomComposite()
     */
    private void createOutputMessageWindow() {
        outputMessageText = WidgetFactory.createText(outputTabFolder, SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL, "");
        actionsGUI.addOutputMessage(GUILanguageResource.getProperty("UATDescription"));
        outputMessageTabItem = WidgetFactory.createTabItem(outputTabFolder, GUILanguageResource.getProperty("OutputTab"), null, outputMessageText);
    }

    /**
     * This function create CodeTabFolder
     * 
     * @see #createMiddleComposite()
     * @see #createCTabItems()
     */
    @SuppressWarnings("deprecation")
    public void createCodeTabFolder() {
        codeCTabFolder = WidgetFactory.createCTabFoler(middleComposite, SWT.TOP | SWT.CLOSE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        codeCTabFolder.setSimple(false);// 设置圆角
        WidgetFactory.configureFormData(codeCTabFolder, new FormAttachment(0, 0), new FormAttachment(0, 0), new FormAttachment(100, 0), new FormAttachment(100, 0));
        createCTabItems();
        codeCTabFolder.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                refreshSaveFileToolItem();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {

            }
        });

        // 双击codeCTabFolder，可以把代码框最大化
        codeCTabFolder.addMouseListener(new MouseListener() {

            @Override
            public void mouseUp(MouseEvent arg0) {}

            @Override
            public void mouseDown(MouseEvent arg0) {}

            @Override
            public void mouseDoubleClick(MouseEvent arg0) {
                if (sashForm3.getMaximizedControl() == middleComposite) {
                    sashForm3.setMaximizedControl(null);
                    sashForm.setWeights(weights1);
                    sashForm2.setWeights(weights2);
                } else {
                    sashForm3.setMaximizedControl(middleComposite);
                    sashForm.setWeights(new int[] {1, 0});
                    sashForm2.setWeights(new int[] {0, 1});
                }
            }
        });

        codeCTabFolder.addCTabFolderListener(new CTabFolderAdapter() {
            public void itemClosed(CTabFolderEvent e) {
                CTabItem closingItem = (CTabItem) e.item;
                if (!closeItem(closingItem))
                    e.doit = false;
            }
        });
        final Menu menu = new Menu(shell, SWT.POP_UP);
        final MenuItem closeCurrentMenuItem = new MenuItem(menu, SWT.PUSH);
        closeCurrentMenuItem.setText("关闭当前");
        closeCurrentMenuItem.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                CTabItem item = codeCTabFolder.getSelection();
                if (closeItem(item))
                    item.dispose();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {

            }
        });

        final MenuItem closeOthersMenuItem = new MenuItem(menu, SWT.PUSH);
        closeOthersMenuItem.setText("关闭其它");
        closeOthersMenuItem.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                CTabItem item = codeCTabFolder.getSelection();
                ArrayList<FileCTabItem> itemList = new ArrayList<FileCTabItem>();
                itemList.addAll(items);
                for (int i = 0; i < itemList.size(); i++) {
                    FileCTabItem otherFileItem = itemList.get(i);
                    CTabItem otherItem = otherFileItem.getCTabItem();
                    if (item != otherItem)
                        if (closeItem(otherItem))
                            otherItem.dispose();
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {

            }
        });

        final MenuItem closeAllMenuItem = new MenuItem(menu, SWT.PUSH);
        closeAllMenuItem.setText("关闭全部");
        closeAllMenuItem.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                ArrayList<FileCTabItem> itemList = new ArrayList<FileCTabItem>();
                itemList.addAll(items);
                for (int i = 0; i < itemList.size(); i++) {
                    FileCTabItem otherFileItem = itemList.get(i);
                    CTabItem otherItem = otherFileItem.getCTabItem();
                    if (closeItem(otherItem))
                        otherItem.dispose();
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {}
        });

        final MenuItem closeLeftMenuItem = new MenuItem(menu, SWT.PUSH);
        closeLeftMenuItem.setText("关闭左侧");
        closeLeftMenuItem.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                CTabItem item = codeCTabFolder.getSelection();
                ArrayList<FileCTabItem> itemList = new ArrayList<FileCTabItem>();
                itemList.addAll(items);
                for (int i = 0; i < itemList.size(); i++) {
                    FileCTabItem otherFileItem = itemList.get(i);
                    CTabItem otherItem = otherFileItem.getCTabItem();
                    if (item == otherItem)
                        break;
                    if (closeItem(otherItem))
                        otherItem.dispose();
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {

            }
        });

        final MenuItem closeRightMenuItem = new MenuItem(menu, SWT.PUSH);
        closeRightMenuItem.setText("关闭右侧");
        closeRightMenuItem.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                CTabItem item = codeCTabFolder.getSelection();
                ArrayList<FileCTabItem> itemList = new ArrayList<FileCTabItem>();
                itemList.addAll(items);
                int i;
                for (i = 0; i < itemList.size(); i++) {
                    FileCTabItem otherFileItem = itemList.get(i);
                    CTabItem otherItem = otherFileItem.getCTabItem();
                    if (item == otherItem)
                        break;
                }
                for (int j = i + 1; j < itemList.size(); j++) {
                    FileCTabItem otherFileItem = itemList.get(j);
                    CTabItem otherItem = otherFileItem.getCTabItem();
                    if (closeItem(otherItem))
                        otherItem.dispose();
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {

            }
        });

        codeCTabFolder.setMenu(menu);
        menu.addMenuListener(new MenuAdapter() {
            public void menuShown(MenuEvent e) {
                Point p = display.getCursorLocation();
                p = codeCTabFolder.toControl(p);
                CTabItem item = codeCTabFolder.getItem(p);
                if (item == null) {
                    menu.setVisible(false);
                    return;
                }
            }
        });

    }

    private boolean closeItem(CTabItem closingItem) {
        FileCTabItem fileCItem = null;
        for (FileCTabItem item : items)
            if (item.getCTabItem().equals(closingItem)) {
                fileCItem = item;
                break;
            }

        for (AnalysisFile af : currentProject.getFileList())
            if (af.getFile().equals(fileCItem.getFile().getAbsolutePath())) {
                if (af.isModified()) {
                    MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.YES | SWT.NO | SWT.CANCEL);
                    box.setText("提示信息");
                    box.setMessage("需要保存" + af.getFile() + "吗?");

                    int ans = box.open();
                    if (ans == SWT.YES)
                        doSaveFile(af);
                    else if (ans == SWT.CANCEL)
                        return false;
                }
                break;
            }
        saveFileToolItem.setEnabled(false);
        return true;
    }

    public void refreshSaveFileToolItem() {
        boolean flag = false;
        for (FileCTabItem item : items) {
            if (item.getCTabItem().equals(codeCTabFolder.getSelection())) {
                for (AnalysisFile af : currentProject.getFileList()) {
                    if (af.getFile().equals(item.getFile().getAbsolutePath())) {
                        if (af.isModified())
                            flag = true;
                        break;
                    }
                }
                break;
            }
        }
        if (flag)
            saveFileToolItem.setEnabled(true);
        else
            saveFileToolItem.setEnabled(false);
    }

    /**
     * This function create the cTabItems that show the codes contents
     * 
     * @see #createMiddleComposite()
     * @see #createCoverageInfoComposite()
     */
    public void createCTabItems() {
        items = new ArrayList<FileCTabItem>();
    }

    private void createRightComposite() {
        // 如果需要在添加

    }

    /**
     * This function create the left composite of CodemonGui.
     * The CodemonGui structure is like this:
     * shell
     * ToolBar
     * leftComposite
     * projectViewTree
     * rightComposite
     * ??ViewTree
     * ??ViewTree
     * bottomComposite
     * statusBar
     * middleComposite
     * code
     * output
     * 
     * @see #createShell
     * @see #createRightComposite
     * @see #createToolBar
     * @see #createBottomComposite
     * @see #createMiddleComposite
     * 
     * @see #createProjectViewTree
     */
    public void createLeftComposite() {
        leftComposite = WidgetFactory.createComposite(sashForm2, SWT.BORDER);
        leftComposite.setLayout(new FormLayout());
        projectTabFolder = WidgetFactory.createTabFolder(leftComposite, SWT.NONE);
        WidgetFactory.configureFormData(projectTabFolder, new FormAttachment(0, 0), new FormAttachment(0, 0), new FormAttachment(100, 0), new FormAttachment(100, 0));
        treeComposite = WidgetFactory.createComposite(projectTabFolder, SWT.BORDER);
        treeComposite.setLayout(new FormLayout());
        projectTabItem = WidgetFactory.createTabItem(projectTabFolder, GUILanguageResource.getProperty("ProjectTreeNavigator"), null, treeComposite);
        createProjectViewTree();

        projectTabFolder.addMouseListener(new MouseListener() {

            @Override
            public void mouseUp(MouseEvent arg0) {}

            @Override
            public void mouseDown(MouseEvent arg0) {}

            @Override
            public void mouseDoubleClick(MouseEvent arg0) {
                if (sashForm2.getMaximizedControl() == leftComposite) {
                    sashForm2.setMaximizedControl(null);
                    sashForm3.setWeights(weights3);
                    sashForm2.setWeights(weights2);
                } else {
                    sashForm2.setMaximizedControl(leftComposite);
                    sashForm.setWeights(new int[] {1, 0});
                    sashForm3.setWeights(new int[] {0, 1});
                }
            }
        });

    }

    /**
     * 显示空的测试用例树
     * 
     * created by Yaoweichang on 2015-04-16 下午5:17:25
     */
    public void createTestCaseViewTree() {
        TestCaseTree = new Tree(testCaseTreeComposite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        TestCaseTree.setBackground(Resource.backgroundColor);
        TreeItem root = new TreeItem(TestCaseTree, 0);
        root.setText("测试用例树");
        root.setImage(Resource.projectImage);
        TreeItem child = new TreeItem(root, SWT.NONE, 0);
        child.setText("暂无测试用例");
        root.setExpanded(true);
    }

    /**
     * This fucnction create the projectViewTree.
     * 
     * @see #createLeftComposite()
     * 
     * 
     */
    public void createProjectViewTree() {
        createProjectViewTreeRoot();
        projectViewTree = new Tree(treeComposite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        projectViewTree.setBackground(Resource.backgroundColor);

        WidgetFactory.configureFormData(projectViewTree, new FormAttachment(0, 0), new FormAttachment(0, 0), new FormAttachment(100, 0), new FormAttachment(100, 0));

        if (currentProject == null)
            projectViewTree.removeAll();
    }

    /**
     * This function add items into cTabItems
     * 
     * @param fileName the name of the file which will be shown in the CTabItem
     */
    public void addCTabItems(String fileName) {
        Text codeText = WidgetFactory.createText(codeCTabFolder, SWT.BORDER);
        CTabItem cti = WidgetFactory.createCTabItem(codeCTabFolder, fileName, null, codeText);
    }


    /**
     * This function deal the events of the CodemonGui
     * 
     * @see #dealMenuEvent()
     * @see #dealToolBarEvent()
     * @see #dealProjectViewTreeEvent()
     */
    public void dealEvent() {
        shell.addShellListener(new ShellAdapter() {
            public void shellClosed(ShellEvent e) {
                e.doit = actionsGUI.doExit();
            }
        });

        dealMenuEvent();
        dealToolBarEvent();
        dealProjectViewTreeEvent();
        dealTestCaseTreeEvent();
    }


    private void dealProjectViewTreeEvent() {
        projectViewTree.addMouseListener(new ProjectViewTreeMouseListener(this));
        projectViewTree.addSelectionListener(new ProjectViewTreeSelectionListener(this));
        projectViewTree.addTreeListener(new ProjectViewTreeExpandListener(this));
    }

    /**
     * 测试用例树的响应事件
     * 
     * created by Yaoweichang on 2015-11-16 下午5:19:15
     */
    private void dealTestCaseTreeEvent() {
        TestCaseTree.addMouseListener(new TestCaseTreeMouseListener(this));
    }

    /**
     * This function deal the Menu Event of UATGUI
     * 
     * @see #dealEvent()
     * @see #dealFileMenuEvent()
     * @see #dealProjectMenuEvent()
     * @see #dealTestCaseMenuEvent()
     * @see #dealPopupMenuEvent()
     */
    public void dealMenuEvent() {
        dealFileMenuEvent();
        dealTestMenuEvent();
        dealTestCaseMenuEvent();
        dealShowMenuEvent();

        dealHelpMenuEvent();

        dealSettingMenuEvent();

    }

    private void dealShowMenuEvent() {
        showLogFileMenuItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowLogFile();
            }
        });
        showDriverFileMenuItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowDriverFile();
            }
        });
        showRegressionTestFileMenuItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowRegressionTestFile();
            }
        });
        showInstruFileMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowInstruFile();
            }
        });
        showTestCaseFileMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowTestCaseFile();
            }
        });
        showSourceCodeMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowsourceFile();
            }
        });

        showStubFileMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowTestStubFile();
            }
        });
        showSoftwareMetricMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doSoftwareMetric();
            }
        });
    }

    /**
     * This function deal TestCase Menu Event
     * 
     * @see #doAutoTest()
     * @see #doRandomTestBaseInputDomain()
     * @see #doRandomTestBasePath()
     * @see #doTestBasePathConstraintSolving()
     * 
     */
    public void dealTestMenuEvent() {
        moduleSeparateMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK | SWT.CANCEL);
                box.setText("提示信息");
                box.setMessage("模块划分会清除所有测试用例,继续吗?");

                int ans = box.open();
                if (ans == SWT.OK) {
                    doModuleSeparateForSelectedFiles(currentProject.getFilenameList());
                    doRefresh();
                }
            }
        });

        coverRuleSelectMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                doSelectCoverRule();
            }
        });

        bugLinkMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doBugLink();
            }
        });

        batchProcessMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doBatchProcessing();
            }
        });
    }

    /**
     * This function deal TestCase Menu Event
     * 
     * @see #doShowTestCaseLib()
     * @see #doImportTestCase()
     */
    public void dealTestCaseMenuEvent() {
        showTestCaselibMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowTestCaseLib();
            }
        });

        importTestCaseFromLibMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doImportTestCase();
            }
        });
    }


    /**
     * This function deal File Menu Event
     * 
     * @see #doOpenProject()
     * @see #doNewProject()
     * @see #doCloseProject()
     * @see #doSaveProject()
     * @see #doSaveProjectAs()
     * @see #doExport()
     * @see #doExit(ShellEvent)
     */
    public void dealFileMenuEvent() {
        openProjectMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doOpenProject();
            }
        });

        newProjectMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                doNewProject();
            }
        });

        final UATGUI temp = this;
        closeProjectMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                UATExitDialog dialogTest = new UATExitDialog(temp);
                dialogTest.setUsage(2);
                dialogTest.go();
            }
        });

        saveProjectMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                doSaveProject();
            }
        });

        saveProjectAsMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                doSaveProjectAs();
            }
        });

        reloadProjectMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                doReloadProject();
            }
        });

        projectAttributeMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean result = currentProject.updateMetric();
                if (result) {
                    UATAttributeGUI projectAttributeGUI = new UATAttributeGUI(currentProject);
                    projectAttributeGUI.showProjectAttribute();
                } else {
                    MessageBox mb = WidgetFactory.createErrorMessageBox(shell, "错误信息", "请对所有文件进行模块划分后再进行软件度量");
                    mb.open();
                }
            }
        });

        exitMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doExit();
            }
        });

        createCurrentProjectMenu();
    }



    /**
     * This function deal toolbar event of UATGUI
     * 
     * @see #dealEvent()
     */
    public void dealToolBarEvent() {
        newProjectToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                doNewProject();
            }
        });
        openProjectToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doOpenProject();
            }
        });

        final UATGUI temp = this;
        closeProjectToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                UATExitDialog dialogTest = new UATExitDialog(temp);
                dialogTest.setUsage(2);
                dialogTest.go();
            }
        });

        saveProjectToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                doSaveProject();
            }
        });

        saveFileToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                CTabItem item = codeCTabFolder.getSelection();
                FileCTabItem fileCItem = null;
                for (FileCTabItem fileItem : items)
                    if (fileItem.getCTabItem() == item) {
                        fileCItem = fileItem;
                        break;
                    }

                for (AnalysisFile af : currentProject.getFileList())
                    if (af.getFile().equals(fileCItem.getFile().getAbsolutePath())) {
                        doSaveFile(af);

                        MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK | SWT.CANCEL);
                        box.setText("提示信息");
                        box.setMessage("需要为" + af.getFile() + "重新进行模块划分吗？\n(进行回归测试请选择“取消”)");

                        int ans = box.open();
                        if (ans == SWT.OK) {
                            Config.needSavePro = true;
                            if (Config.DelTestcaseAfterReModuleSeperate) {
                                // 模块划分时，删除属于此文件的测试用例 add By 唐容 20110920
                                TestCaseLibManagerNew.deleteTCforFile(af.getConsoleAlteredFile());
                            }
                            doModuleSeparate();
                        }
                        break;
                    }
                saveFileToolItem.setEnabled(false);
            }
        });

        showCoverageWindowToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.showCoverageWindow();
            }
        });

        manualInterventionToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doManualIntervention();
            }
        });

        showTestCaseFileToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowTestCaseFile();
            }
        });

        showLogFileToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowLogFile();
            }
        });


        showTestStubFileToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowTestStubFile();
            }
        });

        showTestDriverFileToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowDriverFile();
            }
        });

        showTestRegressionFileToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowRegressionTestFile();
            }
        });

        showInstrumentFileToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowInstruFile();
            }
        });

        showSourceCodeFileToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doShowsourceFile();
            }
        });

        staticAnalysisToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK | SWT.CANCEL);
                box.setText("提示信息");
                box.setMessage("模块划分会清除所有测试用例,继续吗?");

                int ans = box.open();
                if (ans == SWT.OK) {
                    doModuleSeparateForSelectedFiles(currentProject.getFilenameList());
                    Config.needSavePro = true;
                }
            }

        });

        batchProcessToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Config.needSavePro = true;
                actionsGUI.doBatchProcessing();
            }
        });

        showTestReportToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

                actionsGUI.showTestResultReport();
            }

        });
        showTestResultToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

                actionsGUI.doShowTestResult();
            }

        });
        softwareMetricToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

                actionsGUI.doSoftwareMetric();
            }

        });

        coverRuleSelectToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

                doSelectCoverRule();
            }
        });

        bugLinkToolItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

                actionsGUI.doBugLink();
            }
        });

        runSelectedTestCaseItem.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                runSelectedTestCases();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {}
        });
    }

    public void runSelectedTestCases() {
        Config.isAutoCompare = true; // added by hanchunxiao,执行模式选择
        Config.IsCompareFailed = false;
        currentTestCasecount = 0;
        isContinue = true;
        UATRunSelectedTestCasesGUI lsg = new UATRunSelectedTestCasesGUI(this);
        lsg.uatGui.getShell().setEnabled(false);
        lsg.go();
    }

    /**
     * 测试用例单步执行
     * added by hanchunxiao
     */
    public void runSelectedSingleTC(final List<TestCaseNew> testCaseSet) {
        if (currentFunc == null) {
            return;
        }
        currentFunc.clearFileContext();// 清空文件内容，只保存将要执行的用例
        // 逻辑线程，防止主界面假死
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                final String funcName = currentFunc.getFuncName();
                // 防止在CurrentFunc被改变,用一个临时的变量保存当前选中要测试的函数
                final TestModule testFunc = currentFunc;
                CoverRule cr = currentCoverCriteria;
                // 修改界面的用这个线程去执行
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        runSelectedTestCaseItem.setEnabled(false);
                        // 显示覆盖率板块
                        outputTabFolder.setSelection(3);

                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("正在生成测试用例，请稍候");
                        actionsGUI.clearOutputMessage();
                        setStatusBarInfo("正在对函数 " + funcName + "运行指定测试用例...");
                        actionsGUI.addOutputMessage("正在对函数 " + funcName + "运行指定测试用例...");
                    }
                });
                try {
                    TestCaseNew tc = null;
                    currentTestCasecount = 0;
                    final int length = testCaseSet.size();
                    for (TestCaseNew tc_ : testCaseSet) {
                        final int count1 = ++currentTestCasecount;
                        if (isContinue == false)
                            break;
                        tc = testFunc.runSingleTestCase(tc_, cr);
                        final long id = tc_.getId();
                        final boolean b = tc.isRightActualReturn();
                        Display.getDefault().syncExec(new Runnable() {
                            @Override
                            public void run() {
                                if (count1 < length) {
                                    MessageBox box = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                                    box.setText("提示信息");
                                    String messageString = "本次执行用例对应的用例库编号" + id + "\n对比" + (b ? "成功" : "失败") + "\n\n是否继续执行下一个用例？";
                                    box.setMessage(messageString);
                                    int response = box.open();
                                    switch (response) {
                                        case SWT.NO: // 选择终止
                                            isContinue = false;
                                            break;
                                        case SWT.YES: // 选择继续
                                            break;
                                    }
                                } else if (count1 == length) {
                                    String info = "本次执行用例对应的用例库编号" + id + "\n对比" + (b ? "成功" : "失败");
                                    MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示信息", info);
                                    box.open();
                                }
                            }
                        });
                    }
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            actionsGUI.doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            setStatusBarInfo("对函数 " + funcName + "运行指定测试用例结束");
                            actionsGUI.addOutputMessage("对函数 " + funcName + "运行指定测试用例结束");
                            String info = "运行指定测试用例完成";
                            if (testFunc.getFuncVar().hasFileVar())
                                info += "\n参数或全局变量中有文件指针，请用户自行校验测试结果";
                            if (Config.IsCompareFailed)
                                info += "\n对比失败，对比结果文件目录如下\n" + testFunc.getTcCompareRetFilePath();
                            else {
                                info += "\n对比成功，对比结果文件目录如下\n" + testFunc.getTcCompareRetFilePath();
                            }
                            MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示信息", info);
                            box.open();
                        }
                    });
                } catch (Exception e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    final String msg = e.getMessage();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(shell, "测试时异常", "基于输入域随机测试时发生异常\n" + msg);
                            box.open();
                            actionsGUI.addOutputMessage("基于输入域随机测试时出现异常 " + msg);
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
     * 第2个参数指定是否将这些TestCase存入数据库
     * needSave = true时， 数据库save操作
     * needSave = false时， 数据库update操作
     * by Cai Min
     */
    public void runSelectedTCSet(final List<TestCaseNew> testCaseSet, final boolean needSave) {
        if (currentFunc == null) {
            return;
        }
        currentFunc.clearFileContext();// 清空文件内容，只保存将要执行的用例
        // 逻辑线程，防止主界面假死
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                final String funcName = currentFunc.getFuncName();
                // 防止在CurrentFunc被改变,用一个临时的变量保存当前选中要测试的函数
                final TestModule testFunc = currentFunc;
                CoverRule cr = currentCoverCriteria;
                // 修改界面的用这个线程去执行
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        runSelectedTestCaseItem.setEnabled(false);
                        // 显示覆盖率板块
                        outputTabFolder.setSelection(3);

                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("正在执行测试用例，请稍候");
                        actionsGUI.clearOutputMessage();
                        setStatusBarInfo("正在对函数 " + funcName + "运行指定测试用例...");
                        actionsGUI.addOutputMessage("正在对函数 " + funcName + "运行指定测试用例...");
                    }
                });
                try {
                    testFunc.runSelectedTestCaseSet(testCaseSet, cr, needSave);
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            actionsGUI.doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            setStatusBarInfo("对函数 " + funcName + "运行指定测试用例结束");
                            actionsGUI.addOutputMessage("对函数 " + funcName + "运行指定测试用例结束");
                            String info = "运行指定测试用例完成";
                            if (testFunc.getFuncVar().hasFileVar())
                                info += "\n参数或全局变量中有文件指针，请用户自行校验测试结果";
                            if (Config.isAutoCompare) {
                                if (Config.IsCompareFailed)
                                    info += "\n对比失败，对比结果文件目录如下\n" + testFunc.getTcCompareRetFilePath();
                                else {
                                    info += "\n对比成功，对比结果文件目录如下\n" + testFunc.getTcCompareRetFilePath();
                                }
                                MessageBox box = WidgetFactory.createInfoMessageBox(shell, "提示信息", info);
                                box.open();
                            }
                        }
                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(shell, "测试时异常", "函数 " + funcName + "存在死循环，没有出口\n" + msg);
                            box.open();
                            actionsGUI.addOutputMessage("函数 " + funcName + "存在死循环，没有出口" + msg);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                } catch (Exception e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    final String msg = e.getMessage();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(shell, "测试时异常", "基于输入域随机测试时发生异常\n" + msg);
                            box.open();
                            actionsGUI.addOutputMessage("基于输入域随机测试时出现异常 " + msg);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                } finally {
                    progressDisplayGUI.terminateListener.setRunningThread(null);
                }
            }
        }.start();
    }

    protected void doExport() {

    }


    /**
     * This function deal the Help Menu Event
     * 
     * @see #dealMenuEvent()
     */
    public void dealHelpMenuEvent() {
        aboutMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doAbout();
            }
        });
    }


    /**
     * This function deal the Setting Menu Event
     * 
     * @see #dealMenuEvent()
     */
    public void dealSettingMenuEvent() {
        compilerSettingMenuItem2.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doCompilerSetting2();
            }
        });

        maxRangeSettingMenuItem2.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doMaxDomainRangeSetting();
            }
        });

        languageSettingMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doLanguageSetting();
            }
        });

        testCaseManagementSettingMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doTestCaseMangementSetting();
            }
        });

        formalVersionMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Config.isDebugVersion = false;
            }
        });

        debugVersionMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Config.isDebugVersion = true;
            }
        });

        systemSettingMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doSystemSetting();

            }
        });

        AdvanceSettingMenuItem.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                actionsGUI.doSystemSetting();
            }
        });

    }

    /**
     * 2013/03/26
     * 给出工程树的的根节点root，使用lazy方式生成工程树
     * 
     * @author xujiaoxian
     * @param root
     */
    public void buildTree(TreeItem root) {
        TestModule tm = getCurrentFunc();
        String funcName = null;
        if (currentFunc != null)
            funcName = currentFunc.getFuncName();
        boolean isfirstbuildtree = true;
        TreeItem[] items = root.getItems();
        if (((File) root.getData()).isFile()) {
            // int fileloc = currentProject.getfilesLoc(((File)root.getData()).getAbsolutePath());
            // if(currentProject.getFuncsNumList(fileloc) != items.length)
            // {
            // for(int i=0;i<items.length;i++)
            // items[i].dispose();
            // isfirstbuildtree = true;
            // }
            // else{
            // for (int i= 0; i<items.length; i++) {
            // if (items [i].getData () != null)
            // {
            // isfirstbuildtree = false;
            // break;
            // }
            // items [i].dispose ();
            // }
            // }
            if (Config.isTestCaseGenerate) {
                for (int i = 0; i < items.length; i++) {
                    if (items[i].getData() != null) {
                        isfirstbuildtree = false;
                        break;
                    }
                    items[i].dispose();
                }
            } else {
                for (int i = 0; i < items.length; i++) {
                    items[i].dispose();
                }
                isfirstbuildtree = true;
            }
        } else {
            for (int i = 0; i < items.length; i++) {
                if (items[i].getData() != null) {
                    isfirstbuildtree = false;
                    break;
                }
                items[i].dispose();
            }
        }
        if (isfirstbuildtree) {// 第一次新建树节点，说明之前没有，需要重新建立
            File file = (File) root.getData();
            if (file.isFile()) {// 对文件的展开要另外处理
                AnalysisFile analysisfile = currentProject.getAnalysisFile(file.getAbsolutePath());
                List<TestModule> functionlist = analysisfile.getFunctionList();
                for (int i = 0; i < functionlist.size(); i++) {
                    TreeItem item = new TreeItem(root, 0);
                    item.setText(functionlist.get(i).getFuncName());
                    item.setData(functionlist.get(i).getFuncName());
                    if (functionlist.get(i).isFirstTest())
                        item.setImage(Resource.testCaseImage);
                    else
                        item.setImage(Resource.testCase_hasTestedImage);
                }
                if (root.getExpanded()) {
                    // 先收回去，在展开
                    root.setExpanded(false);
                    root.setExpanded(true);
                }
            } else if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files == null || files.length == 0)
                    return;
                List dirlist = new ArrayList<File>();
                List cfilelist = new ArrayList<File>();
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory())
                        dirlist.add(files[i]);
                    else if (files[i].isFile())
                        cfilelist.add(files[i]);
                }
                File[] dirs = null, cfiles = null;
                if (dirlist.size() > 0) {
                    dirs = new File[dirlist.size()];
                    for (int i = 0; i < dirlist.size(); i++) {
                        dirs[i] = (File) dirlist.get(i);
                    }
                    dirlist.clear();
                }
                dirlist = null;

                if (cfilelist.size() > 0) {
                    cfiles = new File[cfilelist.size()];
                    for (int i = 0; i < cfilelist.size(); i++) {
                        cfiles[i] = (File) cfilelist.get(i);
                    }
                    cfilelist.clear();
                }
                cfilelist = null;

                if (dirs != null) {
                    Arrays.sort(dirs, new Comparator<File>() {
                        public int compare(File f1, File f2) {
                            String[] tmp = {f1.getName().toLowerCase(), f2.getName().toLowerCase()};
                            Arrays.sort(tmp);
                            if (f1.getName().equalsIgnoreCase(tmp[0]))
                                return -1;
                            else if (tmp[0].equalsIgnoreCase(tmp[1]))
                                return 0;
                            else
                                return 1;
                        }
                    });
                    for (int i = 0; i < dirs.length; i++) {
                        if (dirs[i].getName().matches("(^\\..*)")) {// 隐藏文件不显示
                            continue;
                        } else if (hasSrcFile(dirs[i])) {
                            TreeItem item = new TreeItem(root, 0);
                            item.setText(dirs[i].getName());
                            item.setData(dirs[i]);
                            item.setImage(Resource.folderImage);
                            new TreeItem(item, 0);
                        }
                    }
                }
                if (cfiles != null) {
                    Arrays.sort(cfiles, new Comparator<File>() {
                        public int compare(File f1, File f2) {
                            String[] tmp = {f1.getName().toLowerCase(), f2.getName().toLowerCase()};
                            Arrays.sort(tmp);
                            if (f1.getName().equalsIgnoreCase(tmp[0]))
                                return -1;
                            else if (tmp[0].equalsIgnoreCase(tmp[1]))
                                return 0;
                            else
                                return 1;
                        }
                    });
                    for (int i = 0; i < cfiles.length; i++) {
                        if (cfiles[i].getName().matches("(^\\..*)")) {// 隐藏文件不显示
                            continue;
                        } else if (cfiles[i].getName().matches(InterContext.SRCFILE_POSTFIX)) {
                            TreeItem item = new TreeItem(root, 0);
                            item.setText(cfiles[i].getName());
                            item.setData(cfiles[i]);
                            int fileloc = currentProject.getfilesLoc(cfiles[i].getAbsolutePath());
                            if (fileloc >= 0 && currentProject.getIsError().get(fileloc))
                                item.setImage(Resource.ErrorFile);
                            else
                                item.setImage(Resource.File);
                            if (fileloc >= 0 && !currentProject.getIsError().get(fileloc) && currentProject.getFuncsNumList(fileloc) != 0)
                                new TreeItem(item, 0);
                        }

                    }
                }
            }
        } else {// 之前已经创建树的节点了，但是要检查树是否有更新，如果有更新需要更新工程树
            for (int i = 0; i < items.length; i++) {
                if (items[i].getData() instanceof File) {
                    File file = (File) items[i].getData();
                    if (file.isFile()) {
                        int fileloc = currentProject.getfilesLoc(file.getAbsolutePath());
                        if (fileloc >= 0 && currentProject.getIsError().get(fileloc)) {
                            if (items[i].getExpanded())
                                items[i].setExpanded(false);
                            items[i].setImage(Resource.ErrorFile);
                            for (TreeItem it : items[i].getItems()) {
                                it.dispose();
                            }
                        } else
                            items[i].setImage(Resource.File);
                        if (fileloc >= 0 && !currentProject.getIsError().get(fileloc) && currentProject.getFuncsNumList(fileloc) != 0) {
                            if (items[i].getItemCount() <= 0)
                                new TreeItem(items[i], 0);
                            if (items[i].getExpanded()) {
                                items[i].setExpanded(false);
                                buildTree(items[i]);
                                items[i].setExpanded(true);
                            }
                        }
                    } else {// 说明是目录
                        buildTree(items[i]);
                    }
                } else {// 说明items[i]是函数节点
                    String funcname = (String) items[i].getData();
                    TreeItem parentitem = items[i].getParentItem();
                    AnalysisFile analysisfile = currentProject.getAnalysisFile(((File) parentitem.getData()).getAbsolutePath());
                    List<TestModule> functionlist = analysisfile.getFunctionList();
                    for (int j = 0; j < functionlist.size(); j++) {
                        if (functionlist.get(j).getFuncName().equals(funcname)) {
                            if (functionlist.get(j).isFirstTest())
                                items[i].setImage(Resource.testCaseImage);
                            else
                                items[i].setImage(Resource.testCase_hasTestedImage);
                            break;
                        }
                    }
                }
            }
        }

        // 在windows下需要启用这段代码 否则工程树显示会出现焦点问题，Linux下无此问题
        // 备注 by Yaoweichang
        // if(getCurrentFile() != null)
        // {
        // String fileName = getCurrentFile().getFile();
        // TreeItem[] allItems = getProjectViewTree().getItems();
        // for(int k=0;k<allItems.length;k++)
        // {
        // TreeItem[] fileItems = allItems[k].getItems();
        // for(int i=0;i<fileItems.length;i++)
        // {
        // if(fileName.contains(fileItems[i].getText()))
        // {
        // if(funcName != null)
        // {
        // setCurrentFunc(tm);
        // for(TreeItem it : fileItems[i].getItems()){
        // if(funcName.equals(it.getText()))
        // {
        // String os = System.getProperty("os.name");
        // if(! os.equals("Linux"))
        // getProjectViewTree().setSelection(it);//选中当前函数节点
        // actionsGUI.doCoverageInfoRefresh();
        // return;
        // }
        // }
        // }
        // else
        // {
        // getProjectViewTree().setSelection(fileItems[i]);//选中当前文件节点
        // return;
        // }
        // }
        // }
        // }
        // }
    }

    /**
     * 2013/03/27
     * 判断某一目录下是否有源文件
     * 
     * @author xujiaoxian
     * @param srcDir
     * @return 如果有返回true，如果没有返回false
     */
    public boolean hasSrcFile(File srcDir) {
        boolean has = false;
        if (srcDir.isFile()) {
            // 收集源文件
            if (srcDir.getName().matches(InterContext.SRCFILE_POSTFIX))
                has = true;
        } else if (srcDir.isDirectory()) {
            File[] fs = srcDir.listFiles();
            for (int i = 0; i < fs.length; i++) {
                has = has || hasSrcFile(fs[i]);
            }
        }
        return has;
    }

    /**
     * This function expand the projectViewTree
     */
    public void expandProjectViewTree() {
        TreeItem[] tis = projectViewTree.getItems();
        if (tis == null || tis.length == 0) {
            return;
        }
        // add by xujiaoxian
        tis[0].setExpanded(false);
        buildTree(tis[0]);
        tis[0].setExpanded(true);
        if (currentProject == null) {
            // JOptionPane.showMessageDialog( null, "ProjectNULL" );
            return;
        }
        TreeItem[] tis1 = projectViewTree.getItems()[0].getItems();
        for (TreeItem ti : tis1) {
            if (ti.getText().equals(currentProject.getName())) {
                // add by xujiaoxian
                ti.setExpanded(false);
                buildTree(ti);
                // end add by xujiaoxian
                ti.setExpanded(true);
            }
        }
        TreeItem root = projectViewTree.getItem(0);
        if (root != null)
            needExpand(root); // 保证节点expand的状态
    }

    private boolean needExpand(TreeItem item) {// 如果子节点中有expand状态的节点，那么父节点也需要expand
        boolean res = false;
        for (TreeItem child : item.getItems())
            if (needExpand(child)) {
                // 先收回去，再展开
                item.setExpanded(false);
                buildTree(item);
                res = true;
                item.setExpanded(true);
                // break;
            }
        if (item.getExpanded())
            res = true;
        return res;
    }

    /**
     * This function create Project Tree Root
     * 
     * @see #createProjectViewTree()
     */
    public void createProjectViewTreeRoot() {
        projectViewTreeRoot = new TreeNode(GUILanguageResource.getProperty("Project"));
    }

    /**
     * This function create the menu of the UATGUI
     * include: Main menu and popup menu.
     * 
     * @see #createFileMenu
     * @see #createProjectMenu
     * @see #createTestCaseMenu
     * @see #createHelpMenu
     * @see #createFileViewPopupMenu
     * @see #createProjectViewPopupMenu
     * @see #createPointViewPopupMenu
     */
    private void createMenu() {
        mainMenu = WidgetFactory.createMenu(shell, SWT.BAR | SWT.LEFT_TO_RIGHT);
        createFileMenu();
        createTestMenu();
        createShowMenu();
        createTestCaseMenu();
        createSettingMenu();
        createHelpMenu();
        shell.setMenuBar(mainMenu);
    }

    private void createShowMenu() {
        showMenuItem = WidgetFactory.createMenuItem(mainMenu, SWT.CASCADE, GUILanguageResource.getProperty("Show"), null, -1, true);

        // Children of the fileMenuItem
        showMenu = WidgetFactory.createMenu(shell, SWT.DROP_DOWN, showMenuItem, true);
        showLogFileMenuItem = WidgetFactory.createMenuItem(showMenu, SWT.PUSH, GUILanguageResource.getProperty("ShowLogFile"), Resource.Log, -1, false);

        showInstruFileMenuItem = WidgetFactory.createMenuItem(showMenu, SWT.PUSH, GUILanguageResource.getProperty("ShowInstruFile"), Resource.Instrument, -1, false);
        showStubFileMenuItem = WidgetFactory.createMenuItem(showMenu, SWT.PUSH, GUILanguageResource.getProperty("ShowStubFile"), Resource.Stub, -1, false);
        showDriverFileMenuItem = WidgetFactory.createMenuItem(showMenu, SWT.PUSH, GUILanguageResource.getProperty("ShowDriverFile"), Resource.Driver, -1, false);
        showRegressionTestFileMenuItem = WidgetFactory.createMenuItem(showMenu, SWT.PUSH, GUILanguageResource.getProperty("ShowRegressionFile"), Resource.Regression, -1, false);
        showTestCaseFileMenuItem = WidgetFactory.createMenuItem(showMenu, SWT.PUSH, GUILanguageResource.getProperty("ShowTestCaseFile"), Resource.showTestCaseFileImage, -1, false);
        showSourceCodeMenuItem = WidgetFactory.createMenuItem(showMenu, SWT.PUSH, GUILanguageResource.getProperty("ShowSourceFile"), Resource.showsourceCodeImage, -1, false);
        packFileMenuItem = WidgetFactory.createMenuItem(showMenu, SWT.PUSH, GUILanguageResource.getProperty("PackFile"), Resource.PackFileImage, -1, false);
        testTableMenuItem = WidgetFactory.createMenuItem(showMenu, SWT.PUSH, GUILanguageResource.getProperty("TestTable"), Resource.TestTableImage, -1, false);
        showSoftwareMetricMenuItem = WidgetFactory.createMenuItem(testMenu, SWT.PUSH, GUILanguageResource.getProperty("softwareMetric"), Resource.showSoftwareMetricImage, -1, false);
        showSoftwareMetricMenuItem.setEnabled(false);

    }

    /**
     * This function create the file menu:
     * The structure of the main menu is:
     * file menu, ..... ,help menu.
     * 
     * 
     * 
     * @see #createHelpMenu
     * @see #createMenu
     */
    public void createFileMenu() {
        fileMenuItem = WidgetFactory.createMenuItem(mainMenu, SWT.CASCADE, GUILanguageResource.getProperty("File"), null, -1, true);

        // Children of the fileMenuItem
        fileMenu = WidgetFactory.createMenu(shell, SWT.DROP_DOWN, fileMenuItem, true);

        newProjectMenuItem = WidgetFactory.createMenuItem(fileMenu, SWT.PUSH, GUILanguageResource.getProperty("NewProject"), Resource.newProjectImage, -1, true);
        openMenuItem = WidgetFactory.createMenuItem(fileMenu, SWT.CASCADE, GUILanguageResource.getProperty("OpenProject"), Resource.openProjectImage, -1, true);
        openProjectMenu = WidgetFactory.createMenu(shell, SWT.DROP_DOWN, openMenuItem, true);
        openProjectMenuItem = WidgetFactory.createMenuItem(openProjectMenu, SWT.PUSH, GUILanguageResource.getProperty("OpenProject"), Resource.openProjectImage, -1, true);
        // 最近打开的工程菜单栏
        openCurrentProjectMenuItem = WidgetFactory.createMenuItem(openProjectMenu, SWT.CASCADE, GUILanguageResource.getProperty("OpenCurrentProject"), Resource.OpenCurrentProjectImage, -1, false);
        openCurrentProjectMenu = WidgetFactory.createMenu(shell, SWT.DROP_DOWN, openCurrentProjectMenuItem, true);
        openCurrentProjectMenuItem1 = WidgetFactory.createMenuItem(openCurrentProjectMenu, SWT.PUSH, null, null, -1, false);
        openCurrentProjectMenuItem2 = WidgetFactory.createMenuItem(openCurrentProjectMenu, SWT.PUSH, null, null, -1, false);
        openCurrentProjectMenuItem3 = WidgetFactory.createMenuItem(openCurrentProjectMenu, SWT.PUSH, null, null, -1, false);
        openCurrentProjectMenuItem4 = WidgetFactory.createMenuItem(openCurrentProjectMenu, SWT.PUSH, null, null, -1, false);
        openCurrentProjectMenuItem5 = WidgetFactory.createMenuItem(openCurrentProjectMenu, SWT.PUSH, null, null, -1, false);
        openExampleProjectMenuItem = WidgetFactory.createMenuItem(openProjectMenu, SWT.PUSH, GUILanguageResource.getProperty("OpenExampleProject"), Resource.OpenExampleProjectImage, -1, false);
        closeProjectMenuItem = WidgetFactory.createMenuItem(fileMenu, SWT.PUSH, GUILanguageResource.getProperty("CloseProject"), Resource.closeProjectImage, -1, true);
        fileSeparatorMenuItem1 = WidgetFactory.createMenuItem(fileMenu, SWT.SEPARATOR, "", null, -1, true);
        saveProjectMenuItem = WidgetFactory.createMenuItem(fileMenu, SWT.PUSH, GUILanguageResource.getProperty("SaveProject"), Resource.saveProjectImage, -1, true);
        saveProjectAsMenuItem = WidgetFactory.createMenuItem(fileMenu, SWT.PUSH, GUILanguageResource.getProperty("SaveProjectAs"), Resource.saveProjectAsImage, -1, true);
        fileSeparatorMenuItem2 = WidgetFactory.createMenuItem(fileMenu, SWT.SEPARATOR, "", null, -1, true);
        reloadProjectMenuItem = WidgetFactory.createMenuItem(fileMenu, SWT.PUSH, GUILanguageResource.getProperty("ReloadProject"), Resource.ReloadProjectImage, -1, true);
        projectAttributeMenuItem = WidgetFactory.createMenuItem(fileMenu, SWT.PUSH, GUILanguageResource.getProperty("ProjectAttribute"), Resource.ProjectAttributeImage, -1, true);
        fileSeparatorMenuItem3 = WidgetFactory.createMenuItem(fileMenu, SWT.SEPARATOR, "", null, -1, true);
        printProjectMenuItem = WidgetFactory.createMenuItem(fileMenu, SWT.CASCADE, GUILanguageResource.getProperty("Print"), Resource.PrintImage, -1, true);
        printProjectMenu = WidgetFactory.createMenu(shell, SWT.DROP_DOWN, printProjectMenuItem, true);
        printMenuItem = WidgetFactory.createMenuItem(printProjectMenu, SWT.PUSH, GUILanguageResource.getProperty("Print"), Resource.PrintImage, -1, false);
        fastPrintMenuItem = WidgetFactory.createMenuItem(printProjectMenu, SWT.PUSH, GUILanguageResource.getProperty("FastPrint"), Resource.FastPrintImage, -1, false);
        printViewMenuItem = WidgetFactory.createMenuItem(printProjectMenu, SWT.PUSH, GUILanguageResource.getProperty("PrintView"), Resource.PrintViewImage, -1, false);
        printSettingMenuItem = WidgetFactory.createMenuItem(printProjectMenu, SWT.PUSH, GUILanguageResource.getProperty("PrintSetting"), Resource.PrintSettingImage, -1, false);
        fileSeparatorMenuItem4 = WidgetFactory.createMenuItem(fileMenu, SWT.SEPARATOR, "", null, -1, true);
        exitMenuItem = WidgetFactory.createMenuItem(fileMenu, SWT.PUSH, GUILanguageResource.getProperty("Exit"), Resource.exitImage, -1, true);

        printProjectMenuItem.setEnabled(false); // 打印菜单暂时不可用
        closeProjectMenuItem.setEnabled(false);
        saveProjectMenuItem.setEnabled(false);
        saveProjectAsMenuItem.setEnabled(false);
        reloadProjectMenuItem.setEnabled(false);
        projectAttributeMenuItem.setEnabled(false);
    }

    public void refreshCurrentProjectMenu() {
        // 根据配置文件修改菜单项内容
        final Properties tempProperties = new Properties();
        FileInputStream inputFile;
        try {
            inputFile = new FileInputStream(System.getProperty("user.dir") + File.separator + "config" + File.separator + "CurrentProjectConfig.properties");
            tempProperties.load(inputFile);
            inputFile.close();
        } catch (FileNotFoundException ex) {
            System.out.println("无目标配置文件，创建新文件");
            File file = new File(System.getProperty("user.dir") + File.separator + "config" + File.separator + "CurrentProjectConfig.properties");
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("装载文件--->失败");
            ex.printStackTrace();
        }
        if (tempProperties.containsKey("1")) {
            openCurrentProjectMenuItem.setEnabled(true);
            openCurrentProjectMenuItem1.setEnabled(true);
            openCurrentProjectMenuItem1.setText(tempProperties.getProperty("1"));
        }
        if (tempProperties.containsKey("2")) {
            openCurrentProjectMenuItem2.setEnabled(true);
            openCurrentProjectMenuItem2.setText(tempProperties.getProperty("2"));
        }
        if (tempProperties.containsKey("3")) {
            openCurrentProjectMenuItem3.setEnabled(true);
            openCurrentProjectMenuItem3.setText(tempProperties.getProperty("3"));
        }
        if (tempProperties.containsKey("4")) {
            openCurrentProjectMenuItem4.setEnabled(true);
            openCurrentProjectMenuItem4.setText(tempProperties.getProperty("4"));
        }
        if (tempProperties.containsKey("5")) {
            openCurrentProjectMenuItem5.setEnabled(true);
            openCurrentProjectMenuItem5.setText(tempProperties.getProperty("5"));
        }
    }

    private void createCurrentProjectMenu() {

        // 根据配置文件修改菜单项内容
        final Properties tempProperties = new Properties();
        FileInputStream inputFile;
        try {
            inputFile = new FileInputStream(System.getProperty("user.dir") + File.separator + "config" + File.separator + "CurrentProjectConfig.properties");
            tempProperties.load(inputFile);
            inputFile.close();
        } catch (FileNotFoundException ex) {
            logger.warn("无目标配置文件，创建新文件");
            File file = new File(System.getProperty("user.dir") + File.separator + "config" + File.separator + "CurrentProjectConfig.properties");
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException ex) {
            System.out.println("装载文件--->失败");
            ex.printStackTrace();
        }
        if (tempProperties.containsKey("1")) {
            openCurrentProjectMenuItem.setEnabled(true);
            openCurrentProjectMenuItem1.setEnabled(true);
            openCurrentProjectMenuItem1.setText(tempProperties.getProperty("1"));
            openCurrentProjectMenuItem1.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    System.out.println("1");
                    doOpenCurrentProject(tempProperties.getProperty("1"));
                }
            });
        }
        if (tempProperties.containsKey("2")) {
            openCurrentProjectMenuItem2.setEnabled(true);
            openCurrentProjectMenuItem2.setText(tempProperties.getProperty("2"));
            openCurrentProjectMenuItem2.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    System.out.println("2");
                    doOpenCurrentProject(tempProperties.getProperty("2"));
                }
            });
        }
        if (tempProperties.containsKey("3")) {
            openCurrentProjectMenuItem3.setEnabled(true);
            openCurrentProjectMenuItem3.setText(tempProperties.getProperty("3"));
            openCurrentProjectMenuItem3.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    System.out.println("3");
                    doOpenCurrentProject(tempProperties.getProperty("3"));
                }
            });
        }
        if (tempProperties.containsKey("4")) {
            openCurrentProjectMenuItem4.setEnabled(true);
            openCurrentProjectMenuItem4.setText(tempProperties.getProperty("4"));
            openCurrentProjectMenuItem4.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    System.out.println("4");
                    doOpenCurrentProject(tempProperties.getProperty("4"));
                }
            });
        }
        if (tempProperties.containsKey("5")) {
            openCurrentProjectMenuItem5.setEnabled(true);
            openCurrentProjectMenuItem5.setText(tempProperties.getProperty("5"));
            openCurrentProjectMenuItem5.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    System.out.println("5");
                    doOpenCurrentProject(tempProperties.getProperty("5"));
                }
            });
        }

    }

    public void doOpenCurrentProject(final String filepath) {
        File file = new File(filepath);
        if (!file.exists()) {// 判断文件的存在
            MessageBox mb = WidgetFactory.createErrorMessageBox(shell, "错误信息", "目标文件不存在！");
            mb.open();
            return;
        }
        try {
            new Thread() {
                public void run() {
                    progressDisplayGUI.terminateListener.setRunningThread(this);
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            progressDisplayGUI.setTestProgressRunning(3);
                            progressDisplayGUI.setInfo("正在打开工程，请稍候...");
                            actionsGUI.clearOutputMessage();
                        }

                    });
                    Project project = null;
                    try {
                        project = Project.open(filepath);
                    } catch (IOException e) {
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                        progressDisplayGUI.setTestProgressOver(0);
                        return;
                    } catch (ClassNotFoundException e) {
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                        progressDisplayGUI.setTestProgressOver(0);
                        return;
                    } catch (Exception e) {
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                        progressDisplayGUI.setTestProgressOver(0);
                        return;
                    }
                    setCurrentProject(project);
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {

                            actionsGUI.doProjectViewRefresh();
                            setSoftwareMetricMenuItemEnable();
                            actionsGUI.clearOutputMessage();
                            actionsGUI.addOutputMessage("打开工程  " + filepath);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                }
            }.start();
        } catch (Exception e) {
            String errMsg = "打开工程时出现错误！";
            MessageBox mb = WidgetFactory.createErrorMessageBox(shell, "错误信息", errMsg);
            mb.open();
            RecordToLogger.recordExceptionInfo(e, logger);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
        }
    }

    /**
     * 重新修改测试菜单的显示布局
     * 
     * modify by Yaoweichang
     */
    public void createTestMenu() {
        testMenuItem = WidgetFactory.createMenuItem(mainMenu, SWT.CASCADE, GUILanguageResource.getProperty("Test"), null, -1, true);
        testMenu = WidgetFactory.createMenu(shell, SWT.DROP_DOWN, testMenuItem, true);

        moduleSeparateMenuItem = WidgetFactory.createMenuItem(testMenu, SWT.PUSH, GUILanguageResource.getProperty("ModuleSeparate"), Resource.ModuleSeparateImage, -1, true);
        bugLinkMenuItem = WidgetFactory.createMenuItem(testMenu, SWT.PUSH, GUILanguageResource.getProperty("bugLink"), Resource.bugLinkImage, -1, true);
        batchProcessMenuItem = WidgetFactory.createMenuItem(testMenu, SWT.PUSH, GUILanguageResource.getProperty("BatchProcess"), Resource.BatchProcessImage, -1, true);
        moduleSeparateMenuItem.setEnabled(false);
        bugLinkMenuItem.setEnabled(false);
        batchProcessMenuItem.setEnabled(false);
    }

    /**
     * this function create the Test Case Menu
     * 
     * @see #createMenu()
     * @see #createFileMenu()
     */
    public void createTestCaseMenu() {
        testCaseMenuItem = WidgetFactory.createMenuItem(showMenu, SWT.CASCADE, GUILanguageResource.getProperty("TestCase"), Resource.TestCaseDatabaseImage, -1, false); // 测试用例库功能暂时未实现
        testCaseMenu = WidgetFactory.createMenu(shell, SWT.DROP_DOWN, testCaseMenuItem, true);

        showTestCaselibMenuItem = WidgetFactory.createMenuItem(testCaseMenu, SWT.PUSH, GUILanguageResource.getProperty("ShowTestCaseLib"), Resource.showTestCaselibImage, -1, true);
        importTestCaseFromLibMenuItem = WidgetFactory.createMenuItem(testCaseMenu, SWT.PUSH, GUILanguageResource.getProperty("ImportTestCase"), Resource.importTestCaseFromLibImage, -1, true);
        packTestCaseMenuItem = WidgetFactory.createMenuItem(testCaseMenu, SWT.PUSH, GUILanguageResource.getProperty("PackTestCase"), Resource.PackTestCaseImage, -1, true);

        showTestCaselibMenuItem.setEnabled(false);
        importTestCaseFromLibMenuItem.setEnabled(false);
        packTestCaseMenuItem.setEnabled(false);
    }

    /**
     * This function create the help menu.
     * 
     * @see #createMenu()
     * @see #createFileMenu()
     */
    public void createHelpMenu() {
        helpMenuItem = WidgetFactory.createMenuItem(mainMenu, SWT.CASCADE, GUILanguageResource.getProperty("Help"), null, -1, true);

        helpMenu = WidgetFactory.createMenu(shell, SWT.DROP_DOWN, helpMenuItem, true);
        aboutMenuItem = WidgetFactory.createMenuItem(helpMenu, SWT.PUSH, GUILanguageResource.getProperty("About"), Resource.aboutImage, -1, true);
        contactUsMenuItem = WidgetFactory.createMenuItem(helpMenu, SWT.PUSH, GUILanguageResource.getProperty("ContactUs"), Resource.contactUsImage, -1, true);
        updateMenuItem = WidgetFactory.createMenuItem(helpMenu, SWT.PUSH, GUILanguageResource.getProperty("Update"), Resource.updateImage, -1, true);
        contactUsMenuItem.setEnabled(false);
        updateMenuItem.setEnabled(false);
    }

    /**
     * This function create the setting menu.
     * 
     * @see #createMenu()
     * @see #createFileMenu()
     */
    public void createSettingMenu() {
        settingMenuItem = WidgetFactory.createMenuItem(mainMenu, SWT.CASCADE, GUILanguageResource.getProperty("Setting"), null, -1, true);

        settingMenu = WidgetFactory.createMenu(shell, SWT.DROP_DOWN, settingMenuItem, true);
        systemSettingMenuItem = WidgetFactory.createMenuItem(settingMenu, SWT.CASCADE, GUILanguageResource.getProperty("SystemSetting"), Resource.SystemSettingImage, -1, true);
        systemSettingMenu = WidgetFactory.createMenu(shell, SWT.DROP_DOWN, systemSettingMenuItem, true);
        compilerSettingMenuItem2 = WidgetFactory.createMenuItem(systemSettingMenu, SWT.PUSH, GUILanguageResource.getProperty("Compiler"), null, -1, true);
        maxRangeSettingMenuItem2 = WidgetFactory.createMenuItem(systemSettingMenu, SWT.PUSH, GUILanguageResource.getProperty("MaxRangeSetting"), null, -1, true);
        toolPathSettingMenuItem = WidgetFactory.createMenuItem(systemSettingMenu, SWT.PUSH, GUILanguageResource.getProperty("ToolPath"), null, -1, false);
        filePathSettingMenuItem = WidgetFactory.createMenuItem(systemSettingMenu, SWT.PUSH, GUILanguageResource.getProperty("FilePath"), null, -1, false);
        shortCutsSettingMenuItem = WidgetFactory.createMenuItem(systemSettingMenu, SWT.PUSH, GUILanguageResource.getProperty("ShortCuts"), null, -1, false);
        AdvanceSettingMenuItem = WidgetFactory.createMenuItem(systemSettingMenu, SWT.PUSH, GUILanguageResource.getProperty("Advance"), null, -1, true);

        projectSettingMenuItem = WidgetFactory.createMenuItem(settingMenu, SWT.CASCADE, GUILanguageResource.getProperty("ProjectSetting"), Resource.ProjectSettingImage, -1, true);
        projectSettingMenu = WidgetFactory.createMenu(shell, SWT.DROP_DOWN, projectSettingMenuItem, true);
        coverRuleSelectMenuItem = WidgetFactory.createMenuItem(projectSettingMenu, SWT.PUSH, GUILanguageResource.getProperty("CoverRuleSelect"), Resource.CoverRuleSelectImage, -1, true);
        testCaseManagementSettingMenuItem =
                WidgetFactory.createMenuItem(projectSettingMenu, SWT.PUSH, GUILanguageResource.getProperty("TestCaseManagement"), Resource.TestCaseManagementSettingImage, -1, true);
        manualVariableSettingMenuItem =
                WidgetFactory.createMenuItem(projectSettingMenu, SWT.PUSH, GUILanguageResource.getProperty("ManualVariableSetting"), Resource.ManualVariableSettingImage, -1, false);

        preferenceSettingMenuItem = WidgetFactory.createMenuItem(settingMenu, SWT.CASCADE, GUILanguageResource.getProperty("PreferenceSetting"), Resource.PreferenceSettingImage, -1, true);
        preferenceSettingMenu = WidgetFactory.createMenu(shell, SWT.DROP_DOWN, preferenceSettingMenuItem, true);
        backgroundSettingMenuItem = WidgetFactory.createMenuItem(preferenceSettingMenu, SWT.PUSH, GUILanguageResource.getProperty("Background"), Resource.BackgroundSettingImage, -1, false);
        fontSettingMenuItem = WidgetFactory.createMenuItem(preferenceSettingMenu, SWT.PUSH, GUILanguageResource.getProperty("Font"), Resource.FontSettingImage, -1, false);
        languageSettingMenuItem = WidgetFactory.createMenuItem(preferenceSettingMenu, SWT.PUSH, GUILanguageResource.getProperty("Language"), Resource.languageSettingImage, -1, true);
        // add by xujiaoxian
        versionSettingMenuItem = WidgetFactory.createMenuItem(settingMenu, SWT.CASCADE, GUILanguageResource.getProperty("VersionSetting"), Resource.VersionSettingImage, -1, true);
        versionSettingMenu = WidgetFactory.createMenu(shell, SWT.DROP_DOWN, versionSettingMenuItem, true);
        formalVersionMenuItem = WidgetFactory.createMenuItem(versionSettingMenu, SWT.RADIO, GUILanguageResource.getProperty("FormalVersion"), Resource.FormalVersionSettingImage, -1, true);
        formalVersionMenuItem.setSelection(true);
        debugVersionMenuItem = WidgetFactory.createMenuItem(versionSettingMenu, SWT.RADIO, GUILanguageResource.getProperty("DebugVersion"), Resource.DebugVersionSettingImage, -1, true);
    }



    /**
     * This function create the bottom composite of CodemonGui.
     * The CodemonGui structure is like this:
     * shell
     * ToolBar
     * leftComposite
     * projectViewTree
     * rightComposite
     * pointViewTree
     * fileViewTree
     * bottomComposite
     * statusBar
     * middleComposite
     * code
     * code coverage information
     * 
     * @see #createShell
     * @see #createRightComposite
     * @see #createToolBar
     * @see #createLeftComposite
     * @see #createMiddleComposite
     */
    public void createBottomComposite() {
        bottomComposite = WidgetFactory.createComposite(sashForm, SWT.BORDER);
        bottomComposite.setLayout(new FillLayout());
        statusBar = WidgetFactory.createCLabel(bottomComposite, SWT.NONE, GUILanguageResource.getProperty("StatusInfo"));
    }

    /**
     * This function create the toolbar of UATGUI
     * 
     * @author joaquin
     */
    private void createToolBar() {
        toolBar = WidgetFactory.createHToolBar(shell, SWT.BORDER);
        toolBar.setLayout(new FormLayout());
        WidgetFactory.configureFormData(toolBar, new FormAttachment(0, 5), new FormAttachment(0, 0), new FormAttachment(100, -5), new FormAttachment(0, 45));

        // toolbar
        newProjectToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.newProjectImage, GUILanguageResource.getProperty("NewProject"));
        openProjectToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.openProjectImage, GUILanguageResource.getProperty("OpenProject"));
        closeProjectToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.closeProjectImage, GUILanguageResource.getProperty("CloseProject"));
        saveProjectToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.saveProjectImage, GUILanguageResource.getProperty("SaveProject"));
        coverRuleSelectToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.CoverRuleSelectImage, GUILanguageResource.getProperty("CoverRuleSelect"));

        // toolbar2
        staticAnalysisToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.ModuleSeparateImage, GUILanguageResource.getProperty("ModuleSeparateAllFiles"));
        batchProcessToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.BatchProcessImage, GUILanguageResource.getProperty("BatchProcess"));
        softwareMetricToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.showSoftwareMetricImage, GUILanguageResource.getProperty("softwareMetric"));
        saveFileToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.saveFileImage, GUILanguageResource.getProperty("saveFile"));

        // toolbar3
        manualInterventionToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.ManualInterventionImageBig, "" + GUILanguageResource.getProperty("ManualIntervention"));
        runSelectedTestCaseItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.startImage, GUILanguageResource.getProperty("run"));
        bugLinkToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.bugLinkImage, GUILanguageResource.getProperty("bugLink"));

        // toolbar4
        showTestReportToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.TestReport, GUILanguageResource.getProperty("ShowTestReport"));
        showCoverageWindowToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.ShowCoverageWindowImageBig, "" + GUILanguageResource.getProperty("ShowCoverageWindow"));
        showTestResultToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.TestResult, GUILanguageResource.getProperty("ShowTestResult"));
        showTestCaseFileToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.showTestCaseFileImage, GUILanguageResource.getProperty("ShowTestCaseFile"));
        showLogFileToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.Log, GUILanguageResource.getProperty("ShowLogFile"));

        showTestStubFileToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.Stub, GUILanguageResource.getProperty("ShowStubFile"));
        showTestDriverFileToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.Driver, GUILanguageResource.getProperty("ShowDriverFile"));
        showTestRegressionFileToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.Regression, GUILanguageResource.getProperty("ShowRegressionFile"));
        showInstrumentFileToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.Instrument, GUILanguageResource.getProperty("ShowInstruFile"));
        showSourceCodeFileToolItem = WidgetFactory.createToolItem(toolBar, SWT.PUSH, null, Resource.showsourceCodeImage, GUILanguageResource.getProperty("ShowSourceFile"));

        staticAnalysisToolItem.setEnabled(false);
        coverRuleSelectToolItem.setEnabled(false);
        batchProcessToolItem.setEnabled(false);
        showTestCaseFileToolItem.setEnabled(false);
        showTestStubFileToolItem.setEnabled(false);
        showTestDriverFileToolItem.setEnabled(false);
        showTestRegressionFileToolItem.setEnabled(false);
        showInstrumentFileToolItem.setEnabled(false);
        showSourceCodeFileToolItem.setEnabled(false);
        showCoverageWindowToolItem.setEnabled(false);
        manualInterventionToolItem.setEnabled(false);
        closeProjectToolItem.setEnabled(false);
        saveProjectToolItem.setEnabled(false);
        saveFileToolItem.setEnabled(false);
        showTestReportToolItem.setEnabled(false);
        showTestResultToolItem.setEnabled(false);
        softwareMetricToolItem.setEnabled(false);
        bugLinkToolItem.setEnabled(false);
        runSelectedTestCaseItem.setEnabled(false);
        showLogFileToolItem.setEnabled(false);
    }

    // 所有的按钮对应的动作类，doXXXXX();
    /**
     * This function deal Refresh
     * Event that will refresh the entile gui.
     * 
     * @see #doProjectViewRefresh()
     * @see #doMeauToolBarRefresh()
     */
    public void doRefresh() {
        try {
            actionsGUI.doProjectViewReloadRefresh();
            doMeauToolBarRefresh();
            actionsGUI.doCoverageInfoRefresh();
        } catch (Exception e) {
            actionsGUI.addOutputMessage("更新时发生异常 " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }

    }

    public void doSelectCoverRule() {
        UATCoverRuleSelectGUI crs = new UATCoverRuleSelectGUI(this);
        crs.go();
    }

    /**
     * added by cai min
     * 重新单元划分之后，将CurrentFunc刷新
     */
    public void doCurrentFuncRefresh(AnalysisFile file) {
        this.currentFile = file;
        for (int j = 0; j < currentFile.getFunctionList().size(); j++) {
            TestModule tm = currentFile.getFunctionList().get(j);

            if (currentFunc != null && tm.getFuncName().startsWith(currentFunc.getFuncName())) {
                currentFunc = tm;
                break;
            }
        }
    }

    public void setSelectedTestCaseItemEnabled(boolean value) {
        runSelectedTestCaseItem.setEnabled(value);
    }

    public void setSoftwareMetricMenuItemEnable() {
        this.showSoftwareMetricMenuItem.setEnabled(true);
    }

    public int buildTree(List<String> filenameList, TreeNode root, int currentFileNo, int currentCharLoc) {
        if (currentFileNo < filenameList.size()) {
            String str = getWord(currentCharLoc, filenameList.get(currentFileNo));
            if (str.equals(""))
                return currentFileNo;
            if (str.endsWith(".")) { // 按文件处理
                // add by xujiaoxian 2012-11-21
                if (this.getCurrentProject().getIsModuleSeparated().get(currentFileNo)) {
                    AnalysisFile f = null;
                    if (filenameList.size() <= Config.AnalysisFileInMem) {
                        f = this.getCurrentProject().getFileList().get(currentFileNo);
                    } else {
                        for (int i = 0; i < this.getCurrentProject().getFileList().size(); i++) {
                            if (filenameList.get(currentFileNo).equals(this.getCurrentProject().getFileList().get(i).getFile())) {
                                f = this.getCurrentProject().getFileList().get(i);
                                break;
                            }
                        }
                        if (f == null) {
                            try {
                                FileInputStream fis = new FileInputStream(this.getCurrentProject().getAnalysisFileFullPath(filenameList.get(currentFileNo)));
                                ObjectInputStream ois = new ObjectInputStream(fis);
                                SerializableAnalysisFileInfo saf = (SerializableAnalysisFileInfo) ois.readObject();
                                ois.close();
                                f = new AnalysisFile(saf);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    String filename = filenameList.get(currentFileNo);
                    boolean isError = this.getCurrentProject().getIsError().get(currentFileNo);
                    boolean isExpand = this.getCurrentProject().getIsExpand().get(currentFileNo);
                    TreeNode tn = new FileTreeNode(filename, isError, isExpand);
                    root.addChild(tn);
                    if (f != null) {
                        for (int j = 0; j < f.getFunctionList().size(); j++) {
                            TestModule t = f.getFunctionList().get(j);
                            FunctionTreeNode ftn = new FunctionTreeNode(filename, t.isFirstTest());
                            if (this.currentFunc != null && this.currentFunc.equals(t)) {
                                ftn.setSelected(true);
                                this.currentProject.getIsExpand().set(currentFileNo, true);
                            }
                            tn.addChild(ftn);
                            ftn.setName(t.getFuncName());
                        }
                    }
                    f = null;
                    System.gc();
                } else {
                    String filename = filenameList.get(currentFileNo);
                    boolean isError = this.getCurrentProject().getIsError().get(currentFileNo);
                    boolean isExpand = this.getCurrentProject().getIsExpand().get(currentFileNo);
                    TreeNode tn = new FileTreeNode(filename, isError, isExpand);
                    root.addChild(tn);
                }
                currentFileNo++;
                return currentFileNo;
            } else {
                TreeNode tn;
                if (str.endsWith("\\") || str.endsWith("/")) // 判断是否为两种文件分隔符之一
                    tn = new TreeNode(str.substring(0, str.length() - 1));
                else {
                    tn = new TreeNode(str);
                }
                root.addChild(tn);
                currentFileNo = buildTree(filenameList, tn, currentFileNo, currentCharLoc + str.length());
                if (currentFileNo >= filenameList.size()) {
                    return currentFileNo;
                } else if (-1 == findLoc(tn.getName(), filenameList.get(currentFileNo), currentCharLoc + str.length())) {
                    return currentFileNo;
                } else {
                    String temp = getWord(currentCharLoc, filenameList.get(currentFileNo));
                    if (temp.equals(""))
                        return currentFileNo;
                    if (temp.endsWith("\\") || temp.endsWith("/"))
                        temp = temp.substring(0, temp.length() - 1);
                    if (temp.equals(tn.getName()) && currentFileNo < filenameList.size()) {
                        while (temp.equals(tn.getName()) && currentFileNo < filenameList.size()) {
                            currentFileNo = buildTree(filenameList, tn, currentFileNo, currentCharLoc + str.length());
                            if (currentFileNo < filenameList.size()) {
                                temp = getWord(currentCharLoc, filenameList.get(currentFileNo));
                                if (temp.equals(""))
                                    return currentFileNo;
                                if (temp.endsWith("\\") || temp.endsWith("/"))
                                    temp = temp.substring(0, temp.length() - 1);
                            }
                        }
                    } else if (currentFileNo < filenameList.size()) {
                        currentFileNo = buildTree(filenameList, root, currentFileNo, currentCharLoc);
                    }
                    return currentFileNo;
                }
            }
        }
        return currentFileNo;
    }

    public String getWord(int currentCharLoc, String fileName) {
        int tail = fileName.length() - 1;
        if (fileName.length() <= currentCharLoc + 1)
            return "";
        if ((fileName.charAt(currentCharLoc) == '\\') || (fileName.charAt(currentCharLoc) == '/')) {
            currentCharLoc++;
        }
        String copyOfFileName = fileName;
        fileName = fileName.substring(currentCharLoc, tail);
        int loc1 = fileName.indexOf(".");
        int loc2 = fileName.indexOf("\\");
        if (loc2 < 0)
            loc2 = fileName.indexOf("/");
        if (loc2 < 0 || loc1 <= loc2) {
            tail = loc1 + 1;
            if (loc2 > 0)
                tail = loc2 + 1;
            File tempFile = new File(copyOfFileName.substring(0, currentCharLoc + tail));
            if (loc2 >= 0 && tempFile.exists() && tempFile.isDirectory())
                tail = loc2 + 1;
            else
                tail = loc1 + 1;
        } else {
            tail = loc2 + 1;
        }
        fileName = fileName.substring(0, tail);
        return fileName;
    }

    public int findLoc(String rootName, String fileName, int loc) {
        while (rootName.endsWith("\\") || rootName.endsWith("/")) {
            rootName = rootName.substring(0, rootName.length() - 1);
        }
        while (fileName.endsWith("\\") || fileName.endsWith("/")) {
            fileName = fileName.substring(0, fileName.length() - 1);
        }
        if (fileName.length() <= loc)
            return -1;
        fileName = fileName.substring(0, loc);
        if (fileName.endsWith(rootName)) {
            return loc;
        }
        return -1;
    }

    /**
     * This function deal the New Project Event.
     * 
     * @see #doNewProject()
     * @see #doCloseProject()
     * @see #doSaveProject()
     * @see #doSaveProjectAs()
     * @see #doExport()
     * @see #doExit(ShellEvent)
     */
    public void doNewProject() {
        UATNewProjectGUI newProjectGui = new UATNewProjectGUI(this);
        newProjectGui.uatGui.getShell().setEnabled(false);
        newProjectGui.go();
    }

    /**
     * This function deal Refresh
     * Event that will refresh the meau and toolbar.
     * 
     * @see #doProjectViewRefresh()
     * @see #doMeauToolBarRefresh()
     */
    public void doMeauToolBarRefresh() {
        if (currentProject == null) {
            this.showLogFileMenuItem.setEnabled(false);
            this.showTestReportToolItem.setEnabled(false);
            this.showTestResultToolItem.setEnabled(false);
            this.softwareMetricToolItem.setEnabled(false);
            this.closeProjectMenuItem.setEnabled(false);
            this.closeProjectToolItem.setEnabled(false);
            this.projectAttributeMenuItem.setEnabled(false);
            this.coverRuleSelectMenuItem.setEnabled(false);
            this.coverRuleSelectToolItem.setEnabled(false);
            this.projectSettingMenuItem.setEnabled(false);
            this.saveProjectAsMenuItem.setEnabled(false);
            this.saveProjectMenuItem.setEnabled(false);
            this.reloadProjectMenuItem.setEnabled(false);
            this.moduleSeparateMenuItem.setEnabled(false);

            this.saveProjectToolItem.setEnabled(false);
            this.showTestCaselibMenuItem.setEnabled(false);
            this.importTestCaseFromLibMenuItem.setEnabled(false);

            this.testCaseManagementSettingMenuItem.setEnabled(false);

            this.showCoverageWindowToolItem.setEnabled(false);
            this.manualInterventionToolItem.setEnabled(false);
            this.staticAnalysisToolItem.setEnabled(false);
            this.batchProcessToolItem.setEnabled(false);
            this.batchProcessMenuItem.setEnabled(false);

            this.showTestCaseFileToolItem.setEnabled(false);
            this.showTestDriverFileToolItem.setEnabled(false);
            this.showTestRegressionFileToolItem.setEnabled(false);
            this.showTestStubFileToolItem.setEnabled(false);
            this.showInstrumentFileToolItem.setEnabled(false);
            this.showSourceCodeFileToolItem.setEnabled(false);

            this.showTestCaseFileMenuItem.setEnabled(false);
            this.showSourceCodeMenuItem.setEnabled(false);
            this.showDriverFileMenuItem.setEnabled(false);
            this.showRegressionTestFileMenuItem.setEnabled(false);
            this.showStubFileMenuItem.setEnabled(false);
            this.showInstruFileMenuItem.setEnabled(false);
            this.bugLinkMenuItem.setEnabled(false);
            this.bugLinkToolItem.setEnabled(false);

            // add by xujiaoxian
            this.showLogFileToolItem.setEnabled(false);

        } else {
            this.showLogFileMenuItem.setEnabled(true);
            this.showTestResultToolItem.setEnabled(false);
            this.showCoverageWindowToolItem.setEnabled(false);
            this.softwareMetricToolItem.setEnabled(true);
            this.showTestReportToolItem.setEnabled(true);

            this.languageSettingMenuItem.setEnabled(true);
            this.testCaseManagementSettingMenuItem.setEnabled(true);
            this.compilerSettingMenuItem2.setEnabled(true);
            this.maxRangeSettingMenuItem2.setEnabled(true);
            this.showLogFileToolItem.setEnabled(true);

            if (Config.needSavePro) {
                this.saveProjectMenuItem.setEnabled(true);
                this.saveProjectToolItem.setEnabled(true);
            } else {
                this.saveProjectMenuItem.setEnabled(false);
                this.saveProjectToolItem.setEnabled(false);
            }

            this.staticAnalysisToolItem.setEnabled(true);
            this.moduleSeparateMenuItem.setEnabled(true);
            this.batchProcessToolItem.setEnabled(true);
            this.batchProcessMenuItem.setEnabled(true);
            this.closeProjectMenuItem.setEnabled(true);
            this.closeProjectToolItem.setEnabled(true);
            this.projectAttributeMenuItem.setEnabled(true);
            this.coverRuleSelectMenuItem.setEnabled(true);
            this.coverRuleSelectToolItem.setEnabled(true);
            this.projectSettingMenuItem.setEnabled(true);
            this.saveProjectAsMenuItem.setEnabled(true);
            this.reloadProjectMenuItem.setEnabled(true);

            if (currentFile == null) {
                this.showCoverageWindowToolItem.setEnabled(false);
                this.manualInterventionToolItem.setEnabled(false);
                this.showTestCaseFileToolItem.setEnabled(false);
                this.showTestDriverFileToolItem.setEnabled(false);
                this.showTestRegressionFileToolItem.setEnabled(false);
                this.showTestStubFileToolItem.setEnabled(false);
                this.showInstrumentFileToolItem.setEnabled(false);
                this.showSourceCodeFileToolItem.setEnabled(false);
                this.showTestCaseFileMenuItem.setEnabled(false);
                this.showSourceCodeMenuItem.setEnabled(false);
                this.showDriverFileMenuItem.setEnabled(false);
                this.showRegressionTestFileMenuItem.setEnabled(false);
                this.showStubFileMenuItem.setEnabled(false);
                this.showInstruFileMenuItem.setEnabled(false);
                this.setshowSourceCodeFileToolItem(false);
                this.setshowSourceCodeMenuItem(false);
            } else {
                this.showCoverageWindowToolItem.setEnabled(true);
                if (currentFile.isHasAnalysised() == true) {
                    this.showCoverageWindowToolItem.setEnabled(true);
                }

                if (currentFile.isConsoleAltered()) {
                    this.setshowSourceCodeFileToolItem(true);
                    this.setshowSourceCodeMenuItem(true);
                } else {
                    this.setshowSourceCodeFileToolItem(false);
                    this.setshowSourceCodeMenuItem(false);
                }
                if (currentFunc == null) {
                    this.showTestCaseFileToolItem.setEnabled(false);
                    this.showTestDriverFileToolItem.setEnabled(false);
                    this.showTestRegressionFileToolItem.setEnabled(false);
                    this.showTestStubFileToolItem.setEnabled(false);
                    this.showInstrumentFileToolItem.setEnabled(false);


                    this.showTestCaseFileMenuItem.setEnabled(false);

                    this.showDriverFileMenuItem.setEnabled(false);
                    this.showRegressionTestFileMenuItem.setEnabled(false);
                    this.showStubFileMenuItem.setEnabled(false);
                    this.showInstruFileMenuItem.setEnabled(false);
                    this.showTestResultToolItem.setEnabled(false);
                    this.showCoverageWindowToolItem.setEnabled(false);
                    this.manualInterventionToolItem.setEnabled(false);
                } else {
                    this.manualInterventionToolItem.setEnabled(true);
                    if (currentFile.isHasBuildedTest()) {
                        this.showTestResultToolItem.setEnabled(true);
                        this.showCoverageWindowToolItem.setEnabled(true);
                        this.showTestCaseFileToolItem.setEnabled(true);
                        this.showTestDriverFileToolItem.setEnabled(true);
                        this.showTestStubFileToolItem.setEnabled(true);
                        this.showInstrumentFileToolItem.setEnabled(true);
                        this.showTestCaseFileMenuItem.setEnabled(true);
                        this.showDriverFileMenuItem.setEnabled(true);
                        this.showStubFileMenuItem.setEnabled(true);
                        this.showInstruFileMenuItem.setEnabled(true);
                    } else {
                        this.showTestResultToolItem.setEnabled(false);
                        this.showCoverageWindowToolItem.setEnabled(false);
                        this.showTestCaseFileToolItem.setEnabled(false);
                        this.showTestDriverFileToolItem.setEnabled(false);
                        this.showTestStubFileToolItem.setEnabled(false);
                        this.showInstrumentFileToolItem.setEnabled(false);
                        this.showTestCaseFileMenuItem.setEnabled(false);
                        this.showDriverFileMenuItem.setEnabled(false);
                        this.showStubFileMenuItem.setEnabled(false);
                        this.showInstruFileMenuItem.setEnabled(false);
                    }

                    if (currentFile.isHasRegressionTest()) {
                        this.showTestRegressionFileToolItem.setEnabled(true);
                        this.showRegressionTestFileMenuItem.setEnabled(true);
                    } else {
                        this.showTestRegressionFileToolItem.setEnabled(false);
                        this.showRegressionTestFileMenuItem.setEnabled(false);
                    }

                }
            }
        }

    }

    /**
     * 显示测试用例树
     * 
     * created by Yaoweichang on 2014-04-16 下午8:27:55
     */
    public void doShowTestCasesTree() {
        TreeItem root = TestCaseTree.getItem(0);
        root.removeAll();
        if (currentFunc == null) {
            TreeItem child = new TreeItem(root, SWT.NONE, 0);
            child.setText("暂无测试用例");
            return;
        }
        List<TestCaseNew> list = null;
        try {
            list = TestCaseLibManagerNew.showAllTestCase(currentFunc);

        } catch (Exception e) {
            String message = "从数据库取测试用例时出现异常\n" + e.getMessage();
            RecordToLogger.recordExceptionInfo(e, logger);
            actionsGUI.addOutputMessage(message);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
            MessageBox box = WidgetFactory.createErrorMessageBox(shell, "错误信息", message);
            box.open();
            return;
        }
        int serialNum = list.size();
        map.clear();
        TreeItem child = null;
        for (int i = 0; i < list.size(); i++) {
            TestCaseNew tc = list.get(i);
            child = new TreeItem(root, SWT.NONE, 0);
            child.setText("测试用例" + serialNum);
            child.setData(tc.getId());
            serialNum--;
            // 初始化用例状态
            map.put(tc.getId(), 0);
        }
        // 将焦点聚集于第一个用例节点，同时同步用例值显示。
        if (child != null) {
            root.setExpanded(true);
            TestCaseTree.setFocus();
            TestCaseTree.setSelection(child);
            setCurrentTestCaseID((Long) child.getData());
            doShowAvaiableTestCases();
        }
    }

    /**
     * 刷新测试用例树，添加上相应用例的状态色
     * 
     * created by Yaoweichang on 2015-11-16 下午8:29:04
     */
    public void refreshTestCasesTree() {
        TreeItem root = TestCaseTree.getItem(0);
        TreeItem[] items = root.getItems();
        for (int i = 0; i < items.length; i++) {
            switch (map.get(items[i].getData())) {
                case 1:
                    items[i].setForeground(new Color(null, 46, 139, 87));
                    break;
                case 2:
                    items[i].setForeground(new Color(null, 255, 0, 0));
                    break;
                case 3:
                    items[i].setForeground(new Color(null, 205, 155, 29));
                    break;
                default:
                    items[i].setForeground(new Color(null, 0, 0, 0));
                    break;
            }
        }
    }

    /**
     * 显示有效的测试用例
     * created by Yaoweichang on 2015-11-16 下午8:29:33
     */
    public void doShowAvaiableTestCases() {
        testCaseLibTabItem.setControl(testCaseLibComposite2);
        testCaseLibComposite2.setVisible(true);
        testCaseLibComposite.setVisible(false);
        if (currentFunc == null) {
            // 显示测试用例库这个东西
            testCaseTable.createPreContents();
            return;
        }
        TestCaseNew newTestCase = null;
        try {
            newTestCase = TestCaseLibManagerNew.showOneTestCase(currentFunc, this.getCurrentTestCaseID());

        } catch (Exception e) {
            String message = "从数据库取测试用例时出现异常\n" + e.getMessage();
            RecordToLogger.recordExceptionInfo(e, logger);
            actionsGUI.addOutputMessage(message);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
            MessageBox box = WidgetFactory.createErrorMessageBox(shell, "错误信息", message);
            box.open();
            return;
        }
        try {
            if (newTestCase == null)
                testCaseTable.createPreContents();
            else
                testCaseTable.createContents(newTestCase);
        } catch (Exception e) {
            String message = "显示测试用例时出现异常 " + e.getMessage();
            RecordToLogger.recordExceptionInfo(e, logger);
            actionsGUI.addOutputMessage(message);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
            MessageBox box = WidgetFactory.createErrorMessageBox(shell, "错误信息", message);
            box.open();
            return;
        }
    }

    /**
     * This function deal the Save Project As Event.
     * 
     * @see #doNewProject()
     * @see #doCloseProject()
     * @see #doSaveProject()
     * @see #doOpenProject()
     * @see #doExport()
     * @see #doExit(ShellEvent)
     */
    public void doSaveProjectAs() {
        if (currentProject != null) {
            shell.setEnabled(false);
            FileDialog fd = WidgetFactory.createFileDialog(shell);
            fd.setFilterExtensions(new String[] {"*.utp"});
            fd.setText("选择存储路径");
            String path = fd.open();
            if (path == null) {
                shell.setEnabled(true);
                return;
            }
            shell.setEnabled(true);
            try {
                if (path.endsWith(".utp") == false) {
                    path = path + ".utp";
                }
                final String pathName = path;
                new Thread() {
                    public void run() {
                        try {
                            currentProject.saveAs(pathName);
                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    actionsGUI.addOutputMessage("工程另存为" + pathName);
                                }
                            });
                        } catch (IOException e) {
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                        } catch (Exception e) {
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                        }
                    }
                }.start();
            } catch (Exception e) {
                String errMsg = "保存工程出现错误！";
                MessageBox mb = WidgetFactory.createErrorMessageBox(shell, "错误信息", errMsg);
                mb.open();
                RecordToLogger.recordExceptionInfo(e, logger);
                if (Config.printExceptionInfoToConsole)
                    e.printStackTrace();
            }
        }
    }

    /**
     * This function deal Save Project Event.
     * 
     * @see #doNewProject()
     * @see #doOpenProject()
     * @see #doCloseProject()
     * @see #doSaveProjectAs()
     * @see #doExport()
     * @see #doExit(ShellEvent)
     * 
     * @see #doRefresh()
     */
    public void doSaveProject() {
        if (currentProject != null) {
            saveProjectMenuItem.setEnabled(false);
            saveProjectToolItem.setEnabled(false);
            try {
                new Thread() {
                    public void run() {
                        try {
                            currentProject.save();
                            Config.needSavePro = false;
                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    actionsGUI.addOutputMessage("工程保存完毕");
                                }
                            });
                        } catch (IOException e) {
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                        } catch (Exception e) {
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                        }
                    }
                }.start();

            } catch (Exception e) {

                String errMsg = "保存工程出现错误！";
                MessageBox mb = WidgetFactory.createErrorMessageBox(shell, "错误信息", errMsg);
                mb.open();
                RecordToLogger.recordExceptionInfo(e, logger);
            }
        }
    }

    public void doSaveFile(AnalysisFile af) {
        if (currentFile == null)
            return;
        FileTabManager.saveFile(this, af);

        // 改变文件后，重新编译工程
        ExtractOptions extract = new ExtractOptions();
        extract.getOptions(new File(getCurrentProject().getSourceCodePathString()), new File(getCurrentProject().getPath()));
    }

    public void doReloadProject() {
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("正在重新载入工程，请稍候...");
                    }
                });
                String projectName = currentProject.getName();
                String projectPath = currentProject.getPath().substring(0, currentProject.getPath().length() - projectName.length());
                String sourceCodePath = currentProject.getSourceCodePathString();
                final Project project = new Project(projectName, projectPath, sourceCodePath);
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        setCurrentProject(project);
                        doRefresh();
                    }
                });
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        actionsGUI.doProjectViewRefresh();
                        actionsGUI.clearOutputMessage();
                        progressDisplayGUI.setTestProgressOver(0);
                    }
                });
            }
        }.start();
    }

    public void doModuleSeparate() {
        // add by xujiaoxian
        // 显示覆盖率板块
        this.getOutputTabFolder().setSelection(3);
        final UATProgressDisplayGUI progressDisplayGUI = this.getUATProgressDisplayGUI();
        if (currentFile == null)
            return;
        final AnalysisFile afile = currentFile;
        String cfilename;
        int loc = afile.getFile().lastIndexOf(File.separator);
        if (loc != -1)
            cfilename = afile.getFile().substring(loc + 1);
        else
            cfilename = afile.getFile();
        final String filename = cfilename;

        new Thread() {
            public void run() {
                // add by xujiaoxian
                progressDisplayGUI.terminateListener.setRunningThread(this);
                // 修改界面的用这个线程去执行
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("正在对文件" + filename + "进行模块划分，请稍候...");
                        setStatusBarInfo("正在文件 " + filename + "进行模块划分...");
                    }

                });
                try {
                    final long start = System.currentTimeMillis();
                    logger.info("[开始进行单元划分 " + afile.getFile() + "]");
                    actionsGUI.doModuleSeparateForOneFile();

                    int loc = currentProject.getfilesLoc(currentFile.getFile());
                    if (!currentProject.getHasAnalysised().get(loc))
                        currentProject.getProjectMetric().addFileMetric(currentFile);
                    currentProject.getHasAnalysised().set(loc, true);
                    final long end = System.currentTimeMillis();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            if (afile.isError()) {
                                int fileloc = currentProject.getfilesLoc(currentFile.getFile());
                                currentProject.setFuncsNumList(fileloc, 0);
                                actionsGUI.addOutputMessage(afile.getFile() + " 编译不能通过");
                                MessageBox box = WidgetFactory.createInfoMessageBox(shell, "编译错误", afile.getFile() + " 编译错误 ");
                                box.open();
                                logger.error(afile.getFile() + " 编译不能通过");
                                afile.clearFuncList();
                            } else {
                                int fileloc = currentProject.getfilesLoc(currentFile.getFile());
                                currentProject.setFuncsNumList(fileloc, currentFile.getFunctionList().size());
                                currentProject.getIsError().set(fileloc, false);
                                actionsGUI.addOutputMessage(afile.getFile() + " 单元划分结束");
                                logger.info(afile.getFile() + " 单元划分结束 " + (end - start) + "(ms)");
                            }
                            actionsGUI.doProjectViewRefresh();
                        }
                    });

                    // add by xujiaoxian
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            progressDisplayGUI.setTestProgressOver(0);
                            setStatusBarInfo("对文件 " + filename + "模块划分结束");
                        }
                    });
                } catch (Exception e) {
                    logger.error((afile.getFile() + " 单元划分出异常"));
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    final String msg = e.getMessage();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            actionsGUI.addOutputMessage(afile.getFile() + " 单元划分出异常" + msg);
                            MessageBox box = WidgetFactory.createInfoMessageBox(shell, "单元划分出异常", currentFile.getFile() + " 单元划分出异常 " + msg);
                            box.open();
                            afile.clearFuncList();
                            actionsGUI.doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                } finally {
                    progressDisplayGUI.terminateListener.setRunningThread(null);
                }
            }
        }.start();
    }

    public void doModuleSeparateForSelectedFiles(final ArrayList<String> files) {
        // add by xujiaoxian
        // 显示覆盖率板块
        this.getOutputTabFolder().setSelection(3);
        final UATProgressDisplayGUI progressDisplayGUI = this.getUATProgressDisplayGUI();
        if (currentProject == null)
            return;
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("正在对所有文件进行模块划分，请稍候...");
                        setStatusBarInfo("对所有文件进行单元划分");
                    }
                });
                long start = System.currentTimeMillis();

                int hasAnalysis = 0;
                for (String file : files) {
                    currentFile = null;
                    String analysisFilePath = "";// 存储AnalysisFile文件的全路径
                    try {
                        if (currentProject.getFilenameList().size() <= Config.AnalysisFileInMem) {
                            for (int i = 0; i < currentProject.getFileList().size(); i++) {
                                if (file.equals(currentProject.getFileList().get(i).getFile())) {
                                    currentFile = currentProject.getFileList().get(i);
                                    break;
                                }
                            }
                        } else {
                            for (int i = 0; i < currentProject.getFileList().size(); i++) {
                                if (file.equals(currentProject.getFileList().get(i).getFile())) {
                                    currentFile = currentProject.getFileList().get(i);
                                    break;
                                }
                            }
                            if (currentFile == null) {
                                analysisFilePath = currentProject.getAnalysisFileFullPath(file);
                                try {
                                    FileInputStream fis = new FileInputStream(analysisFilePath);
                                    ObjectInputStream ois = new ObjectInputStream(fis);
                                    SerializableAnalysisFileInfo saf = (SerializableAnalysisFileInfo) ois.readObject();
                                    ois.close();
                                    currentFile = new AnalysisFile(saf);
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        Display.getDefault().syncExec(new Runnable() {
                            public void run() {
                                if (currentFile != null) {
                                    String cfilename;
                                    int loc = currentFile.getFile().lastIndexOf(File.separator);
                                    if (loc != -1)
                                        cfilename = currentFile.getFile().substring(loc + 1);
                                    else
                                        cfilename = currentFile.getFile();
                                    progressDisplayGUI.setInfo("正在对" + cfilename + "文件进行模块划分，请稍候...");
                                    actionsGUI.addOutputMessage("开始对文件 " + cfilename + "进行模块划分");
                                    setStatusBarInfo("正在对文件 " + cfilename + "进行模块划分...");
                                }

                            }
                        });
                        actionsGUI.doModuleSeparateForOneFile();
                        int loc = currentProject.getfilesLoc(currentFile.getFile());
                        if (!currentProject.getHasAnalysised().get(loc))
                            currentProject.getProjectMetric().addFileMetric(currentFile);
                        currentProject.getHasAnalysised().set(loc, true);
                        // add by xujiaoxian
                        if (!analysisFilePath.equals("")) {// AnalysisFile是从文件中读取的时候，把AnalysisFile写入文件中
                            if (currentFile != null) {
                                try {
                                    FileOutputStream fos = new FileOutputStream(analysisFilePath);
                                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                                    oos.writeObject(new SerializableAnalysisFileInfo(currentFile));
                                    oos.close();
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        // 模块划分时，删除属于此文件的测试用例. create by 唐容 20110920
                        if (Config.DelTestcaseAfterReModuleSeperate)
                            TestCaseLibManagerNew.deleteTCforFile(currentFile.getConsoleAlteredFile());
                        Display.getDefault().syncExec(new Runnable() {
                            public void run() {
                                if (currentFile.isError()) {
                                    int fileloc = currentProject.getfilesLoc(currentFile.getFile());
                                    currentProject.setFuncsNumList(fileloc, 0);
                                    actionsGUI.addOutputMessage("文件 " + currentFile.getFile() + "编译不能通过");
                                    setStatusBarInfo("文件 " + currentFile.getFile() + "编译不能通过");
                                } else {
                                    int fileloc = currentProject.getfilesLoc(currentFile.getFile());
                                    currentProject.setFuncsNumList(fileloc, currentFile.getFunctionList().size());
                                    actionsGUI.addOutputMessage("对文件 " + currentFile.getFile() + "模块划分结束");
                                    setStatusBarInfo("对文件 " + currentFile.getFile() + "模块划分结束");
                                }
                            }
                        });
                    } catch (Exception e) {
                        final String fileName = file;
                        final String msg = e.getMessage();
                        Display.getDefault().syncExec(new Runnable() {
                            public void run() {
                                actionsGUI.addOutputMessage(fileName + " 单元划分时出现异常 " + msg);
                            }
                        });
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                        continue;
                    }
                    hasAnalysis++;
                }

                long end = System.currentTimeMillis();

                logger.info("对所有文件进行单元划分的时间是 " + (end - start) + " ms");
                final long time = end - start;
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        long startrefresh = System.currentTimeMillis();
                        actionsGUI.doProjectViewRefresh();
                        long endrefresh = System.currentTimeMillis();
                        long refreshtime = endrefresh - startrefresh;
                        actionsGUI.addOutputMessage("所有文件单元划分结束,耗时 " + time + " ms" + " , 刷新工程树所用时间：" + refreshtime + " ms");
                        currentProject.setModuleSeparated();
                        progressDisplayGUI.setTestProgressOver(0);
                        setStatusBarInfo("对所有文件模块划分结束");
                    }
                });
            }
        }.start();
    }

    public Display getDisplay() {
        return display;
    }

    public void setSaveFileToolItemEnabled(boolean enabled) {
        saveFileToolItem.setEnabled(enabled);
    }

    /**
     * 刷新当前的FileCTabItem,用在改造控制台输入模块
     * 
     * @param before 保存文件前，源文件是否经过控制台改造
     * @param now 保存文件后，源文件是否经过控制台改造
     * @param af 当前分析的AnalysisFile
     * @param s 保存改造前的经控制台改造的文件路径
     * @author Yangyiwen
     *         暂时弃用
     */

    private void refreshConsoleAlteredFile(boolean before, boolean now, AnalysisFile af, String s) {
        if (before == true && now == true) {// 做简单的刷新
            for (FileCTabItem fileItem : items)
                if (af.getConsoleAlteredFile().equals(fileItem.getFile().getAbsolutePath())) {
                    File file = new File(af.getConsoleAlteredFile());
                    setCodeContents(file, fileItem.getSourceViewer(), false);// 刷新改造后的文件以显示重新处理后的代码
                    break;
                }
        } else if (before == false && now == true) {// 显示改造后的文件
            for (FileCTabItem fileItem : items)
                if (af.getFile().equals(fileItem.getFile().getAbsolutePath())) {
                    File file = new File(af.getConsoleAlteredFile());
                    FileTabManager.ShowFile(file, this, false);
                    break;
                }
        } else if (before == true && now == false) {// 显示源文件，关闭改造后的文件
            for (FileCTabItem fileItem : items)
                if (s.equals(fileItem.getFile().getAbsolutePath())) {
                    File file = new File(af.getFile());
                    FileTabManager.ShowFile(file, this, true);
                    fileItem.getCTabItem().dispose();
                    showSourceCodeFileToolItem.setEnabled(false);
                    showSourceCodeMenuItem.setEnabled(false);
                }
        }
    }
}
