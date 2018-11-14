package unittest.gui;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.util.Config;

public class UATTestCaseManagementSettingGUI {
    private static Logger logger = Logger.getLogger(UATTestCaseManagementSettingGUI.class);

    private Shell shell = null;
    private Display display = null;
    public UATGUI uatGui = null;

    private Button okButton = null;
    private Button cancelButton = null;


    private CLabel runTimeSingleTestCaseClabel;
    private Text runTimeSingleTestCaseText;
    // 测试用例生成时间参数 add by Yaoweichang
    private CLabel testCaseGenTimeClabel = null;
    private String testCaseGenTime = null;
    private Text testCaseGenTimeText = null;

    // 随机测试用例选择的区间选择的类型
    private CLabel randomTypeClabel;
    private Text randomTypeText;

    // 连读多少次生成不更新覆盖率的随机测试用例
    private CLabel notUpdateRandomTimeClabel;
    private Text notUpdateRandomTimeText;

    // 为一个目标覆盖元素生成多少条路径
    private CLabel pathForTargetCoverClabel;
    private Text pathForTargetCoverText;

    // 打桩的类型，使用原代码，还是使用自动打桩的代码
    private CLabel stubTypeClabel;
    private Text stubTypeText;

    // 覆盖率要求，90
    private CLabel coverageClabel;
    private Text coverageText;


    private Composite bottomComposite = null;
    private Composite controlComposite = null;
    private String runTimeSingleTestCase;
    private int coverage;
    private int randomType;

    public UATTestCaseManagementSettingGUI(UATGUI uatgui) {
        this.uatGui = uatgui;
    }

    /**
     * The UATGui call this function to show the NewProject GUI.
     */
    public void go() {
        display = Display.getDefault();
        this.createShell();
        this.dealEvent();
        this.shell.open();

        while (!display.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }

    private void dealEvent() {
        shell.addShellListener(new ShellCloseListener(this));
        okButton.addSelectionListener(new OkButtonListener(this));
        cancelButton.addSelectionListener(new CancelButtonListener(this));
    }

    private void createShell() {
        shell = new Shell(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL);
        shell.setText(GUILanguageResource.getProperty("TestCaseManagement"));
        shell.setBounds(250, 250, 500, 500);
        shell.setLayout(null);

        bottomComposite = WidgetFactory.createComposite(shell, SWT.BORDER);
        bottomComposite.setBounds(0, 0, 490, 430);
        bottomComposite.setLayout(null);

        testCaseGenTimeClabel = WidgetFactory.createCLabel(bottomComposite, SWT.FLAT, GUILanguageResource.getProperty("testCaseGenTime"));
        testCaseGenTimeText = WidgetFactory.createText(bottomComposite, SWT.SINGLE | SWT.BORDER, testCaseGenTime, true);

        testCaseGenTimeText.setText(Config.TestCaseGenTime);
        testCaseGenTimeClabel.setBounds(5, 5, 490, 30);
        testCaseGenTimeText.setBounds(5, 35, 180, 25);

        runTimeSingleTestCaseClabel = WidgetFactory.createCLabel(bottomComposite, SWT.FLAT, GUILanguageResource.getProperty("TimeSingleTestCaseText"));
        runTimeSingleTestCaseText = WidgetFactory.createText(bottomComposite, SWT.SINGLE | SWT.BORDER, runTimeSingleTestCase, true);

        runTimeSingleTestCaseText.setText(Config.SecondsSingleTestCase);
        runTimeSingleTestCaseClabel.setBounds(5, 65, 490, 30);
        runTimeSingleTestCaseText.setBounds(5, 95, 180, 25);


        notUpdateRandomTimeClabel = WidgetFactory.createCLabel(bottomComposite, SWT.FLAT, GUILanguageResource.getProperty("TimesNotUpdateCoverageForRandom"));
        notUpdateRandomTimeText = WidgetFactory.createText(bottomComposite, SWT.SINGLE | SWT.BORDER, runTimeSingleTestCase, true);
        notUpdateRandomTimeText.setText("" + Config.NOTUPDATERANDTIMES);
        notUpdateRandomTimeClabel.setBounds(5, 125, 490, 30);
        notUpdateRandomTimeText.setBounds(5, 155, 180, 25);


        pathForTargetCoverClabel = WidgetFactory.createCLabel(bottomComposite, SWT.FLAT, GUILanguageResource.getProperty("PathForTargetCover"));

        pathForTargetCoverText = WidgetFactory.createText(bottomComposite, SWT.SINGLE | SWT.BORDER, runTimeSingleTestCase, true);
        pathForTargetCoverText.setText("" + Config.LimitNumPath);
        pathForTargetCoverClabel.setBounds(5, 185, 490, 30);
        pathForTargetCoverText.setBounds(5, 215, 180, 25);

        randomTypeClabel = WidgetFactory.createCLabel(bottomComposite, SWT.FLAT, GUILanguageResource.getProperty("RandomIntervalType"));

        randomTypeText = WidgetFactory.createText(bottomComposite, SWT.SINGLE | SWT.BORDER, runTimeSingleTestCase, true);
        randomTypeText.setText("" + Config.RandomMethod);
        randomTypeClabel.setBounds(5, 245, 490, 30);
        randomTypeText.setBounds(5, 275, 180, 25);


        coverageClabel = WidgetFactory.createCLabel(bottomComposite, SWT.FLAT, GUILanguageResource.getProperty("Coverage"));
        coverageText = WidgetFactory.createText(bottomComposite, SWT.SINGLE | SWT.BORDER, runTimeSingleTestCase, true);
        coverageText.setText("" + (int) (Config.COVERAGERATE * 100));
        coverageClabel.setBounds(5, 305, 490, 30);
        coverageText.setBounds(5, 335, 180, 25);

        stubTypeClabel = WidgetFactory.createCLabel(bottomComposite, SWT.FLAT, GUILanguageResource.getProperty("StubType"));
        stubTypeText = WidgetFactory.createText(bottomComposite, SWT.SINGLE | SWT.BORDER, runTimeSingleTestCase, true);
        stubTypeText.setText(Config.stubUseOrig ? "Y" : "N");
        stubTypeClabel.setBounds(5, 365, 490, 30);
        stubTypeText.setBounds(5, 395, 180, 25);

        controlComposite = WidgetFactory.createComposite(shell, SWT.NONE);
        controlComposite.setLayout(null);
        controlComposite.setBounds(5, 430, 400, 50);
        okButton = WidgetFactory.createButton(controlComposite, SWT.PUSH);
        okButton.setText(GUILanguageResource.getProperty("OK"));
        cancelButton = WidgetFactory.createButton(controlComposite, SWT.PUSH);
        okButton.setBounds(35, 5, 60, 30);
        cancelButton.setBounds(305, 5, 60, 30);
        cancelButton.setText(GUILanguageResource.getProperty("Cancel"));
    }

    /**
     * This is the SelectionListener of Ok Button.
     * 
     * @author joaquin(孙华衿)
     * 
     */
    private class OkButtonListener extends SelectionAdapter {
        private UATTestCaseManagementSettingGUI demo;

        public OkButtonListener(UATTestCaseManagementSettingGUI demo) {
            this.demo = demo;
        }

        public void widgetSelected(SelectionEvent e) {
            runTimeSingleTestCase = runTimeSingleTestCaseText.getText().trim();
            testCaseGenTime = testCaseGenTimeText.getText().trim();
            String errorMsg = checkValidity();
            if (errorMsg.equals("")) {
                logger.info("Single Test Case Run Time is changed to  " + runTimeSingleTestCase);

                // 修改配置文件参数 add by Yaoweichang
                Config.TestCaseGenTime = testCaseGenTime;
                Config.SecondsSingleTestCase = runTimeSingleTestCase;
                // add by xujiaoxian
                Config.NOTUPDATERANDTIMES = Integer.parseInt(notUpdateRandomTimeText.getText().trim());
                Config.LimitNumPath = Integer.parseInt(pathForTargetCoverText.getText().trim());
                Config.RandomMethod = Integer.parseInt(randomTypeText.getText().trim());
                Config.COVERAGERATE = Float.parseFloat(coverageText.getText().trim()) / 100;
                if (stubTypeText.getText().trim().equals("Y")) {
                    Config.stubUseOrig = true;
                } else {
                    Config.stubUseOrig = false;
                }

                try {
                    Config.updateConfigFile("./config/config.xml");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                demo.uatGui.getShell().setEnabled(true);
                demo.shell.dispose();
            } else {
                MessageBox mb = WidgetFactory.createMessageBox(shell, SWT.ICON_ERROR | SWT.OK, "错误信息", errorMsg);
                mb.open();
            }
        }

        private String checkValidity() {
            String errorMsg = "";
            try {
                Integer time = Integer.valueOf(testCaseGenTime);
                if (time <= 0)
                    errorMsg += "测试用例生成时间不能为负数\n";

                Integer a = Integer.valueOf(runTimeSingleTestCase);
                if (a <= 0)
                    errorMsg += "单个测试用例执行时间不能为负数\n";

                coverage = Integer.valueOf(coverageText.getText().trim());

                if (coverage > 100)
                    errorMsg += "覆盖率不能大于100\n";

                randomType = Integer.valueOf(randomTypeText.getText().trim());

                if (randomType < 0 || randomType > 3)
                    errorMsg += "随机数产生的类型，不能超过 3，小于0\n";

                String tmp = stubTypeText.getText().trim().toUpperCase();
                if (!(tmp.equals("Y") || tmp.equals("N")))
                    errorMsg += "使用或这不使用原函数定义作为桩函数只能用y/n表示\n";

            } catch (NumberFormatException e) {
                errorMsg = "Has invalid number";
            }
            return errorMsg;
        }
    }

    /**
     * This is the SelectionListener for Cancel Button.
     * 
     * @author joaquin
     * 
     */
    private class CancelButtonListener extends SelectionAdapter {
        private UATTestCaseManagementSettingGUI demo;

        public CancelButtonListener(UATTestCaseManagementSettingGUI demo) {
            this.demo = demo;
        }

        public void widgetSelected(SelectionEvent e) {
            demo.uatGui.getShell().setEnabled(true);
            demo.shell.dispose();
        }
    }

    /**
     * This is the ShellListener of CompilerSettingGUI.
     * 
     * @author joaquin(孙华衿)
     * 
     */
    public class ShellCloseListener extends ShellAdapter {
        private UATTestCaseManagementSettingGUI demo;

        public ShellCloseListener(UATTestCaseManagementSettingGUI demo) {
            this.demo = demo;
        }

        public void shellClosed(ShellEvent e) {
            demo.uatGui.getShell().setEnabled(true);
            demo.shell.dispose();
        }
    }
}
