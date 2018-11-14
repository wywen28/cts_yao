package unittest.gui.listener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.TreeItem;

import unittest.gui.CATManualModuleSeparateGUI;
import unittest.gui.UATAttributeGUI;
import unittest.gui.UATGUI;
import unittest.gui.helper.FileTabManager;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.module.seperate.TestModule;
import unittest.pretreament.PretreamentException;
import unittest.staticAnalysis.StaticAnalysisException;
import unittest.testcase.management.news.TestCaseLibManagerNew;
import unittest.util.AnalysisFile;
import unittest.util.Config;

/**
 * The Mouse Listener for ProjectViewTree.
 * 
 * @author joaquin (�ﻪ��)
 * @see UATGUI
 *      edit by zhouao
 *      ȥ���ɰ������༭��
 */
public class ProjectViewTreeMouseListener extends MouseAdapter {
    private UATGUI demo;

    public ProjectViewTreeMouseListener(UATGUI uatGui) {
        this.demo = uatGui;
    }

    public void mouseUp(MouseEvent e) {
        TreeItem selected = demo.getProjectViewTree().getItem(new Point(e.x, e.y)); // ȡ�ڵ�ؼ�
        if (selected != null)// Project�ڵ�
        {
            // add by cai min, 2011/5/23, �л�������file��funcʱ��BugLinkMenuItem��SelectedTestCaseItem������
            demo.setBugLinkMenuItemEnabled(false);
            demo.setBugLinkToolItemEnabled(false);
            demo.setSelectedTestCaseItemEnabled(false);
            if (selected.getText().endsWith(".c") || selected.getText().endsWith(".C") || selected.getText().endsWith(".cc"))// ѡ�е��ļ��ڵ�
            {
                String fullPathFileName = demo.getCurrentProject().getSourceCodePathString() + File.separator + getFullPathName(selected);
                if (Config.os.equals("windows"))
                    fullPathFileName = fullPathFileName.replaceAll("\\\\+", "\\\\");
                else
                    fullPathFileName = fullPathFileName.replaceAll("//+", File.separator);
                AnalysisFile af = demo.getCurrentProject().getAnalysisFile(fullPathFileName);
                // ���ӱ�ѡ�к����Ĳ��������⽻������ add by Yaoweichang
                demo.setCurrentFile(af);
                demo.setCurrentFunc(null);

                if (demo.getOutputTabFolder().getSelectionIndex() == 2) {
                    demo.doShowTestCasesTree();
                    demo.doShowAvaiableTestCases();
                }
            } else if (selected.getItemCount() == 0// the selected treeitem is a function
                    && selected.getParentItem() != null
                    && (selected.getParentItem().getText().endsWith(".c") || selected.getParentItem().getText().endsWith(".C") || selected.getParentItem().getText().endsWith(".cc"))) {
                String fullPathFileName = demo.getCurrentProject().getSourceCodePathString() + File.separator + getFullPathName(selected.getParentItem());
                final String funcName = selected.getText();
                if (Config.os.equals("windows"))
                    fullPathFileName = fullPathFileName.replaceAll("\\\\+", "\\\\");
                else
                    fullPathFileName = fullPathFileName.replaceAll("//+", File.separator);
                AnalysisFile af = demo.getCurrentProject().getAnalysisFile(fullPathFileName);
                // ���ӱ�ѡ�к����Ĳ��������⽻������ add by Yaoweichang
                demo.setCurrentFile(af);
                for (int j = 0; j < af.getFunctionList().size(); j++) {
                    TestModule tm = af.getFunctionList().get(j);
                    if (tm.getFuncName().startsWith(funcName)) {
                        demo.setCurrentFunc(tm);
                        break;
                    }
                }
                if (demo.getOutputTabFolder().getSelectionIndex() == 2) {
                    demo.doShowTestCasesTree();
                    demo.doShowAvaiableTestCases();
                }
            } else {
                demo.setCurrentFile(null);
                demo.setCurrentFunc(null);
            }
        }
        demo.doMeauToolBarRefresh();
    }

    public void mouseDoubleClick(MouseEvent e) {

        TreeItem selected = demo.getProjectViewTree().getItem(new Point(e.x, e.y)); // ȡ�ڵ�ؼ�
        if (selected != null) {
            // The Selected TreeItem is the root TreeItem of Project View.
            if (selected.getParentItem() == null) {
                SashForm sashForm = demo.getSashForm();
                SashForm sashForm2 = demo.getSashForm2();
                SashForm sashForm3 = demo.getSashForm3();
                Composite leftComposite = demo.getLeftComposite();
                if (sashForm2.getMaximizedControl() == leftComposite) {
                    sashForm2.setMaximizedControl(null);
                    sashForm3.setWeights(demo.getweights3());
                    sashForm2.setWeights(demo.getweights2());
                } else {
                    sashForm2.setMaximizedControl(leftComposite);
                    sashForm.setWeights(new int[] {1, 0});
                    sashForm3.setWeights(new int[] {0, 1});
                }
            } else if (selected.getText().endsWith(".c") || selected.getText().endsWith(".C") || selected.getText().endsWith(".cc")) {
                // the selected tree item is a file
                String fullPathFileName = demo.getCurrentProject().getSourceCodePathString() + File.separator + getFullPathName(selected);
                if (Config.os.equals("windows"))
                    fullPathFileName = fullPathFileName.replaceAll("\\\\+", "\\\\");
                else
                    fullPathFileName = fullPathFileName.replaceAll("//+", File.separator);
                AnalysisFile af = demo.getCurrentProject().getAnalysisFile(fullPathFileName);
                // ���ӱ�ѡ�к����Ĳ��������⽻������ add by Yaoweichang
                demo.setCurrentFile(af);
                demo.setCurrentFunc(null);
                File file = new File(af.getFile());
                if (!file.exists()) {
                    MessageBox mb = WidgetFactory.createErrorMessageBox(demo.getShell(), "������Ϣ", "�ļ� " + file.getAbsolutePath() + " �����ڣ�");
                    mb.open();
                } else {
                    FileTabManager.ShowFile(file, demo, true);
                    if (af.isConsoleAltered()) {
                        demo.setshowSourceCodeFileToolItem(true);
                        demo.setshowSourceCodeMenuItem(true);
                    } else {
                        demo.setshowSourceCodeFileToolItem(false);
                        demo.setshowSourceCodeMenuItem(false);
                    }
                }
            } else if (selected.getItemCount() == 0// the selected treeitem is a function
                    && selected.getParentItem() != null
                    && (selected.getParentItem().getText().endsWith(".c") || selected.getParentItem().getText().endsWith(".C") || selected.getParentItem().getText().endsWith(".cc"))) {
                String fullPathFileName = demo.getCurrentProject().getSourceCodePathString() + File.separator + getFullPathName(selected.getParentItem());
                final String funcName = selected.getText();
                if (Config.os.equals("windows"))
                    fullPathFileName = fullPathFileName.replaceAll("\\\\+", "\\\\");
                else
                    fullPathFileName = fullPathFileName.replaceAll("//+", File.separator);
                AnalysisFile af = demo.getCurrentProject().getAnalysisFile(fullPathFileName);
                // ���ӱ�ѡ�к����Ĳ��������⽻������ add by Yaoweichang
                demo.setCurrentFile(af);
                for (int j = 0; j < af.getFunctionList().size(); j++) {
                    TestModule tm = af.getFunctionList().get(j);
                    if (tm.getFuncName().startsWith(funcName)) {
                        demo.setCurrentFunc(tm);
                        break;
                    }
                }

                FileTabManager.ShowFunction(demo.getCurrentFunc(), demo, true);
                if (af.isConsoleAltered()) {
                    demo.setshowSourceCodeFileToolItem(true);
                    demo.setshowSourceCodeMenuItem(true);
                }
                demo.getUATProgressDisplayGUI().setTestProgressOver(1);
            }
            // add by chenruolin
            // �Բ�֧�ֵĲ���������˫��.CC�ļ���������ʾ
            else {
                MessageBox mb = WidgetFactory.createErrorMessageBox(demo.getShell(), "Error", "������֧��");
                mb.open();
            }
        }
    }

    public void mouseDown(MouseEvent e) {
        final TreeItem selected = demo.getProjectViewTree().getItem(new Point(e.x, e.y)); // ȡ�ڵ�ؼ�
        Menu menu = WidgetFactory.createMenu(demo.getShell(), SWT.POP_UP); // Ϊ�ڵ㽨POP UP�˵�
        if (demo.getProjectViewTree().getMenu() != null)
            if (!demo.getProjectViewTree().getMenu().isDisposed())
                demo.getProjectViewTree().getMenu().dispose();
        if (selected != null && e.button == 3) {
            if (selected.getParentItem() == null) { // ����
                demo.getCurrentProject().setFolderOperation(false);
                MenuItem mi4 = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.ModuleSeparateAllFilesImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                mi4.setText(GUILanguageResource.getProperty("ModuleSeparateAllFiles"));
                mi4.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                // songhao 2012-10-19
                // ��Ϊ�չ��̽��ô˲˵���
                if (demo.getCurrentProject().getFilenameList().size() == 0)
                    mi4.setEnabled(false);
                else
                    mi4.setEnabled(true);
                // end songhao 2012-10-19
                mi4.addListener(SWT.Selection, new Listener() {
                    public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                        MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                        String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                        if (selected != null) {
                            MessageBox box = new MessageBox(demo.getShell(), SWT.ICON_INFORMATION | SWT.OK | SWT.CANCEL);
                            box.setText("��ʾ��Ϣ");
                            box.setMessage("ģ�黮�ֻ�������в�������,������?");

                            int ans = box.open();
                            if (ans == SWT.OK) {
                                demo.doModuleSeparateForSelectedFiles(demo.getCurrentProject().getFilenameList());
                                Config.needSavePro = true;
                            }
                        }
                    }
                });

                if (!Config.IsDemoVersion) {
                    MenuItem mTest = WidgetFactory.createMenuItem(menu, SWT.CASCADE, "Test", Resource.testSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    mTest.setText(GUILanguageResource.getProperty("TestAllFuncInAllFiles"));
                    mTest.setData(selected.getText());

                    // ��Ϊ�չ��̽��ô˲˵��� songhao 2012-10-19
                    if (demo.getCurrentProject().getFilenameList().size() == 0)
                        mTest.setEnabled(false);
                    else
                        mTest.setEnabled(true);
                    // end songhao 2012-10-19

                    Menu testMenu = WidgetFactory.createMenu(demo.getShell(), SWT.DROP_DOWN, mTest, true);

                    MenuItem randomTestcaseGenerate = WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.randomTestImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    randomTestcaseGenerate.setText(GUILanguageResource.getProperty("RandomTestcaseGenerate"));
                    randomTestcaseGenerate.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��

                    randomTestcaseGenerate.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doAutoRandomTestBasedInputDomainAndPathForAllFiles();
                            }
                        }
                    });

                    MenuItem constraintTestcaseGenerate = WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.constraintTestImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    constraintTestcaseGenerate.setText(GUILanguageResource.getProperty("ConstraintTestcaseGenerate"));
                    constraintTestcaseGenerate.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��

                    constraintTestcaseGenerate.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doAutoTestBasedRandomAndPathForAllFiles();
                            }
                        }
                    });

                    if (Config.isDebugVersion) {// ������ڲ����԰汾������ʾ������Щ�˵���
                        if (unittest.util.Config.testWithSingleType) {
                            MenuItem autoRandomTestBasedInputDomainForAllFiles = WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.testSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                            autoRandomTestBasedInputDomainForAllFiles.setText(GUILanguageResource.getProperty("AutoRandomTestBasedInputDomainForAllFiles"));
                            autoRandomTestBasedInputDomainForAllFiles.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                            autoRandomTestBasedInputDomainForAllFiles.addListener(SWT.Selection, new Listener() {
                                public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                    MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                    String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                    if (selected != null) {
                                        demo.actionsGUI.doAutoRandomTestBasedInputDomainForAllFiles();
                                    }
                                }
                            });
                            MenuItem autoRandomTestBasedInputDomainAndPathForAllFiles =
                                    WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.RandomTestBaseInputDomainAndPathSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                            autoRandomTestBasedInputDomainAndPathForAllFiles.setText(GUILanguageResource.getProperty("AutoRandomTestBasedInputDomainAndPathForAllFiles"));
                            autoRandomTestBasedInputDomainAndPathForAllFiles.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                            autoRandomTestBasedInputDomainAndPathForAllFiles.addListener(SWT.Selection, new Listener() {
                                public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                    MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                    String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                    if (selected != null) {
                                        demo.actionsGUI.doAutoRandomTestBasedInputDomainAndPathForAllFiles();
                                    }
                                }
                            });
                            MenuItem autoTestBasedRandomAndPathForAllFiles =
                                    WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                            autoTestBasedRandomAndPathForAllFiles.setText(GUILanguageResource.getProperty("AutoTestBasedRandomAndPathForAllFiles"));
                            autoTestBasedRandomAndPathForAllFiles.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                            autoTestBasedRandomAndPathForAllFiles.addListener(SWT.Selection, new Listener() {
                                public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                    MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                    String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                    if (selected != null) {
                                        demo.actionsGUI.doAutoTestBasedRandomAndPathForAllFiles();
                                    }
                                }
                            });

                            MenuItem autoPathBasedTestForAllFiles = WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                            autoPathBasedTestForAllFiles.setText("�������ļ�������·�����������");
                            autoPathBasedTestForAllFiles.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                            autoPathBasedTestForAllFiles.addListener(SWT.Selection, new Listener() {
                                public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                    MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                    String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                    if (selected != null) {
                                        demo.actionsGUI.doPathBasedRandomTestForAllFiles();
                                    }
                                }
                            });

                            MenuItem autoConstraintsTestForAllFiles = WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                            autoConstraintsTestForAllFiles.setText("�������ļ�������Լ�����Ĳ���");
                            autoConstraintsTestForAllFiles.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                            autoConstraintsTestForAllFiles.addListener(SWT.Selection, new Listener() {
                                public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                    MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                    String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                    if (selected != null) {
                                        demo.actionsGUI.doConstraintsTestForAllFiles();
                                    }
                                }
                            });
                        }
                    }
                }

                MenuItem autoTestProject = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.TestReportSmall, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                autoTestProject.setText(GUILanguageResource.getProperty("AutoTestcaseGenerate"));
                autoTestProject.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                autoTestProject.addListener(SWT.Selection, new Listener() {
                    public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                        MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                        String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                        if (selected != null) {
                            demo.actionsGUI.doAutoTestForProject(selected);
                        }
                    }
                });

                MenuItem showResult = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.TestReportSmall, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                showResult.setText(GUILanguageResource.getProperty("ShowTestReport"));
                showResult.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                showResult.addListener(SWT.Selection, new Listener() {
                    public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                        MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                        String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                        if (selected != null) {
                            demo.actionsGUI.showTestResultReport();
                        }
                    }
                });

                MenuItem refreshProjectItem = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.RefreshProjectImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                refreshProjectItem.setText(GUILanguageResource.getProperty("RefreshProject"));
                refreshProjectItem.addListener(SWT.Selection, new Listener() {
                    public void handleEvent(Event arg0) {
                        demo.doReloadProject();
                    }
                });

                MenuItem projectAttributeItem = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.FileAttributeImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                projectAttributeItem.setText(GUILanguageResource.getProperty("ProjectAttribute"));
                projectAttributeItem.addListener(SWT.Selection, new Listener() {
                    public void handleEvent(Event arg0) {
                        UATAttributeGUI projectAttributeGUI = new UATAttributeGUI(demo.getCurrentProject());
                        projectAttributeGUI.showProjectAttribute();
                    }
                });
                demo.getProjectViewTree().setMenu(menu);
                demo.getProjectViewTree().getMenu().setVisible(true);
            } else {
                if (selected.getText().endsWith(".c") || selected.getText().endsWith(".C") || selected.getText().endsWith(".cc"))// ѡ�е��ļ��ڵ�
                {
                    final String fullPathFileName = demo.getCurrentProject().getSourceCodePathString() + File.separator + getFullPathName(selected);
                    final int filesCount = demo.getCurrentProject().getFilenameList().size();
                    AnalysisFile af = demo.getCurrentProject().getAnalysisFile(fullPathFileName);
                    // ���ӱ�ѡ�к����Ĳ��������⽻������ add by Yaoweichang
                    demo.setCurrentFile(af);
                    demo.setCurrentFunc(null);

                    MenuItem moduleSeparateItem = WidgetFactory.createMenuItem(menu, SWT.CASCADE, "Test", Resource.ModuleSeparateAllFilesImage, SWT.CTRL + 'R', true);
                    moduleSeparateItem.setText(GUILanguageResource.getProperty("ModuleSeparate"));
                    moduleSeparateItem.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    Menu moduleSeparate = WidgetFactory.createMenu(demo.getShell(), SWT.DROP_DOWN, moduleSeparateItem, true);

                    MenuItem autoModuleSeparateItem = WidgetFactory.createMenuItem(moduleSeparate, SWT.PUSH, "Test", Resource.ModuleSeparateAllFilesImage, SWT.CTRL + 'R', true);
                    autoModuleSeparateItem.setText("�Զ�ģ�黮��");
                    autoModuleSeparateItem.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    autoModuleSeparateItem.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {

                                MessageBox box = new MessageBox(demo.getShell(), SWT.ICON_INFORMATION | SWT.OK | SWT.CANCEL);
                                box.setText("��ʾ��Ϣ");
                                box.setMessage("ģ�黮�ֻ����" + demo.getCurrentFile().getFile() + "�����в�������,������?");

                                int ans = box.open();
                                if (ans == SWT.OK) {
                                    Config.IsManualModuleSeparate = false;
                                    demo.doModuleSeparate();
                                    Config.needSavePro = true;
                                    if (Config.DelTestcaseAfterReModuleSeperate)
                                        TestCaseLibManagerNew.deleteTCforFile(demo.getCurrentFile().getConsoleAlteredFile());// ģ�黮��ʱ��ɾ�����ڴ��ļ��Ĳ�������,to
                                                                                                                             // ...New
                                                                                                                             // By
                                                                                                                             // ����
                                                                                                                             // 20110920
                                }
                            }
                        }
                    });

                    MenuItem unitSeparate = WidgetFactory.createMenuItem(moduleSeparate, SWT.PUSH, "Test", Resource.ModuleSeparateAllFilesImage, SWT.CTRL + 'R', true);
                    unitSeparate.setText("�˹�ģ�黮��");
                    unitSeparate.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    unitSeparate.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                Config.IsManualModuleSeparate = true;
                                final AnalysisFile file = demo.getCurrentFile();
                                if (!file.isHasAnalysised()) {
                                    try {
                                        file.doPretreatment();
                                        file.doStaticAnalysis();
                                        file.doModuleSeparate();
                                    } catch (StaticAnalysisException ex) {
                                        if (Config.printExceptionInfoToConsole)
                                            ex.printStackTrace();
                                    } catch (PretreamentException ex) {
                                        if (Config.printExceptionInfoToConsole)
                                            ex.printStackTrace();
                                    }
                                }
                                // songhao 2012-10-16
                                if (file.isError()) {

                                    Display.getDefault().syncExec(new Runnable() {
                                        private String s = file.getFile();

                                        @Override
                                        public void run() {
                                            demo.actionsGUI.addOutputMessage(s + " ���벻��ͨ��");
                                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "�������", s + " ������� ");
                                            box.open();
                                        }

                                    });
                                    return;
                                }
                                new CATManualModuleSeparateGUI(demo.getCurrentFile());
                                int fileloc = demo.getCurrentProject().getfilesLoc(demo.getCurrentFile().getFile());
                                if (fileloc != -1)
                                    demo.getCurrentProject().setFuncsNumList(fileloc, demo.getCurrentFile().getFunctionList().size());
                                demo.actionsGUI.doProjectViewRefresh();
                            }
                        }
                    });


                    if (!Config.IsDemoVersion) {
                        MenuItem reLoadFile = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.ReLoadFileImage, SWT.CTRL + 'R', false);
                        reLoadFile.setText(GUILanguageResource.getProperty("ReLoadFile"));

                        MenuItem mTest = WidgetFactory.createMenuItem(menu, SWT.CASCADE, "Test", Resource.testSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                        mTest.setText(GUILanguageResource.getProperty("TestAllFuncInFiles"));
                        mTest.setData(selected.getText());
                        if (demo.getCurrentFile().isHasAnalysised()) // �Ƿ����ò˵���
                            mTest.setEnabled(true);
                        else
                            mTest.setEnabled(false);

                        Menu testMenu = WidgetFactory.createMenu(demo.getShell(), SWT.DROP_DOWN, mTest, true);

                        MenuItem randomTestcaseGenerate = WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.randomTestImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                        randomTestcaseGenerate.setText(GUILanguageResource.getProperty("RandomTestcaseGenerate"));
                        randomTestcaseGenerate.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��

                        randomTestcaseGenerate.addListener(SWT.Selection, new Listener() {
                            public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                if (selected != null) {
                                    demo.actionsGUI.doAutoRandomTestBasedInputDomainAndPathForAllFunctionInFile();
                                }
                            }
                        });

                        MenuItem constraintTestcaseGenerate = WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.constraintTestImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                        constraintTestcaseGenerate.setText(GUILanguageResource.getProperty("ConstraintTestcaseGenerate"));
                        constraintTestcaseGenerate.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��

                        constraintTestcaseGenerate.addListener(SWT.Selection, new Listener() {
                            public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                if (selected != null) {
                                    demo.actionsGUI.doAutoTestBasedRandomAndPathForAllFuncInFile();
                                }
                            }
                        });

                        if (Config.isDebugVersion) {// ������ڲ����԰汾������ʾ����Ĳ˵���
                            if (unittest.util.Config.testWithSingleType) {
                                MenuItem autoRandomTestBasedInputDomainForAllFuncInFile =
                                        WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.randomTestBaseInputDomainSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                                autoRandomTestBasedInputDomainForAllFuncInFile.setText(GUILanguageResource.getProperty("AutoRandomTestBasedInputDomainForAllFuncInFile"));
                                autoRandomTestBasedInputDomainForAllFuncInFile.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                                if (!demo.getCurrentFile().isHasAnalysised())
                                    autoRandomTestBasedInputDomainForAllFuncInFile.setEnabled(false);
                                if (demo.getCurrentFile().isError())
                                    autoRandomTestBasedInputDomainForAllFuncInFile.setEnabled(false);
                                autoRandomTestBasedInputDomainForAllFuncInFile.addListener(SWT.Selection, new Listener() {
                                    public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                        MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                        String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                        if (selected != null) {
                                            demo.actionsGUI.doAutoRandomTestBasedInputDomainForAllFunctionInFile();
                                        }
                                    }
                                });
                                MenuItem autoRandomTestBasedInputDomainAndPathForAllFuncInFile =
                                        WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.RandomTestBaseInputDomainAndPathSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                                autoRandomTestBasedInputDomainAndPathForAllFuncInFile.setText(GUILanguageResource.getProperty("AutoRandomTestBasedInputDomainAndPathForAllFuncInFile"));
                                autoRandomTestBasedInputDomainAndPathForAllFuncInFile.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                                if (!demo.getCurrentFile().isHasAnalysised())
                                    autoRandomTestBasedInputDomainAndPathForAllFuncInFile.setEnabled(false);
                                if (demo.getCurrentFile().isError())
                                    autoRandomTestBasedInputDomainAndPathForAllFuncInFile.setEnabled(false);
                                autoRandomTestBasedInputDomainAndPathForAllFuncInFile.addListener(SWT.Selection, new Listener() {
                                    public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                        MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                        String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                        if (selected != null) {
                                            demo.actionsGUI.doAutoRandomTestBasedInputDomainAndPathForAllFunctionInFile();
                                        }
                                    }
                                });
                                MenuItem autoTestBasedRandomAndPathForAllFuncInFile =
                                        WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                                autoTestBasedRandomAndPathForAllFuncInFile.setText(GUILanguageResource.getProperty("AutoTestBasedRandomAndPathForAllFuncInFile"));
                                autoTestBasedRandomAndPathForAllFuncInFile.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                                if (!demo.getCurrentFile().isHasAnalysised())
                                    autoTestBasedRandomAndPathForAllFuncInFile.setEnabled(false);
                                if (demo.getCurrentFile().isError())
                                    autoTestBasedRandomAndPathForAllFuncInFile.setEnabled(false);
                                autoTestBasedRandomAndPathForAllFuncInFile.addListener(SWT.Selection, new Listener() {
                                    public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                        MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                        String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                        if (selected != null) {
                                            demo.actionsGUI.doAutoTestBasedRandomAndPathForAllFuncInFile();
                                        }
                                    }
                                });


                                MenuItem autoPathBasedRandomTestForAllFuncInFile =
                                        WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                                autoPathBasedRandomTestForAllFuncInFile.setText("���ļ������к���������·�����������");
                                autoPathBasedRandomTestForAllFuncInFile.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                                if (!demo.getCurrentFile().isHasAnalysised())
                                    autoPathBasedRandomTestForAllFuncInFile.setEnabled(false);
                                if (demo.getCurrentFile().isError())
                                    autoPathBasedRandomTestForAllFuncInFile.setEnabled(false);
                                autoPathBasedRandomTestForAllFuncInFile.addListener(SWT.Selection, new Listener() {
                                    public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                        MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                        String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                        if (selected != null) {
                                            demo.actionsGUI.doPathBasedRandomTestForAllFuncInFile();
                                        }
                                    }
                                });

                                MenuItem autoConstraintTestForAllFuncInFile =
                                        WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                                autoConstraintTestForAllFuncInFile.setText("���ļ������к���������Լ�����Ĳ���");
                                autoConstraintTestForAllFuncInFile.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                                if (!demo.getCurrentFile().isHasAnalysised())
                                    autoConstraintTestForAllFuncInFile.setEnabled(false);
                                if (demo.getCurrentFile().isError())
                                    autoConstraintTestForAllFuncInFile.setEnabled(false);
                                autoConstraintTestForAllFuncInFile.addListener(SWT.Selection, new Listener() {
                                    public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                        MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                        String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                        if (selected != null) {
                                            demo.actionsGUI.doConstraintsTestForAllFuncInFile();
                                        }
                                    }
                                });
                            }
                        }
                    }

                    MenuItem autoTestProject = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.TestReportSmall, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    autoTestProject.setText(GUILanguageResource.getProperty("AutoTestcaseGenerate"));
                    autoTestProject.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    autoTestProject.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doAutoTestForFile(selected);
                            }
                        }
                    });

                    MenuItem GCG = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.GCGImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    GCG.setText(GUILanguageResource.getProperty("GenerateCallGraph"));
                    GCG.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    if (demo.getCurrentFile().isHasAnalysised())
                        GCG.setEnabled(true);
                    else
                        GCG.setEnabled(false);
                    GCG.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doGenerateCallGraph();
                            }
                        }
                    });

                    MenuItem showCoverageWindow = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.ShowCoverageWindowImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    showCoverageWindow.setText(GUILanguageResource.getProperty("ShowCoverageWindow"));
                    showCoverageWindow.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    if (demo.getCurrentFile().isHasAnalysised())
                        showCoverageWindow.setEnabled(true);
                    else
                        showCoverageWindow.setEnabled(false);
                    showCoverageWindow.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.showCoverageWindow();
                            }
                        }
                    });

                    // add by Cai Min
                    MenuItem showDefineFile = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.ShowDefineFileWindowImage, SWT.CTRL + 'R', true);
                    showDefineFile.setText(GUILanguageResource.getProperty("showDefineFile"));
                    showDefineFile.setData(selected.getText());
                    showDefineFile.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doShowDefineFile();
                            }
                        }
                    });

                    MenuItem fileAttribute = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.FileAttributeImage, SWT.CTRL + 'R', true);
                    fileAttribute.setText(GUILanguageResource.getProperty("FileAttribute"));
                    fileAttribute.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event arg0) {
                            // add by xujiaoxian
                            List<String> filenameList = demo.getCurrentProject().getFilenameList();
                            String currentFileName = filenameList.get(0);
                            int i = 1;
                            while (!currentFileName.equals(fullPathFileName) && i < filesCount) {
                                currentFileName = filenameList.get(i);
                                i++;
                            }
                            UATAttributeGUI fileAttributeGUI = new UATAttributeGUI(demo.getCurrentProject());
                            fileAttributeGUI.showFileAttribute(getFullPathName(selected));
                            // end add by xujiaoxian
                        }
                    });

                    demo.getProjectViewTree().setMenu(menu);
                    demo.getProjectViewTree().getMenu().setVisible(true);
                } else if ((selected.getParentItem().getText().endsWith(".c") || selected.getParentItem().getText().endsWith(".C") || selected.getParentItem().getText().endsWith(".cc")))// �����ڵ�
                {
                    final String funcName = selected.getText();
                    String allPathFileName = demo.getCurrentProject().getSourceCodePathString() + File.separator + getFullPathName(selected.getParentItem());
                    AnalysisFile af = demo.getCurrentProject().getAnalysisFile(allPathFileName);
                    demo.setCurrentFile(af);
                    TestModule tm = af.getFuncModule(funcName);
                    demo.setCurrentFunc(tm);

                    MenuItem mShow = WidgetFactory.createMenuItem(menu, SWT.CASCADE, "Test", Resource.ShowTestCaseFileSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    mShow.setText(GUILanguageResource.getProperty("Show"));
                    mShow.setData(selected.getText());

                    Menu showMenu = WidgetFactory.createMenu(demo.getShell(), SWT.DROP_DOWN, mShow, true);

                    MenuItem mShowTestCaseFile = WidgetFactory.createMenuItem(showMenu, SWT.PUSH, "Test", Resource.ShowTestCaseFileSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    mShowTestCaseFile.setText(GUILanguageResource.getProperty("ShowTestCaseFile"));
                    mShowTestCaseFile.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    if (!demo.getCurrentFile().isHasBuildedTest())
                        mShowTestCaseFile.setEnabled(false);
                    mShowTestCaseFile.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doShowTestCaseFile();
                            }
                        }
                    });

                    MenuItem mShowTestDriverFile = WidgetFactory.createMenuItem(showMenu, SWT.PUSH, "Test", Resource.DriverSmall, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    mShowTestDriverFile.setText(GUILanguageResource.getProperty("ShowDriverFile"));
                    mShowTestDriverFile.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    if (!demo.getCurrentFile().isHasBuildedTest()) {
                        mShowTestDriverFile.setEnabled(false);
                        mShow.setEnabled(false);
                    }
                    mShowTestDriverFile.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doShowDriverFile();
                            }
                        }
                    });

                    MenuItem mShowTestRegressionFile = WidgetFactory.createMenuItem(showMenu, SWT.PUSH, "Test", Resource.regressionTestImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    mShowTestRegressionFile.setText(GUILanguageResource.getProperty("ShowRegressionFile"));
                    mShowTestRegressionFile.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    if (!demo.getCurrentFile().isHasRegressionTest()) {
                        mShowTestRegressionFile.setEnabled(false);
                    }
                    mShowTestRegressionFile.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doShowRegressionTestFile();
                            }
                        }
                    });


                    MenuItem mShowTestStubFile = WidgetFactory.createMenuItem(showMenu, SWT.PUSH, "Test", Resource.StubSmall, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    mShowTestStubFile.setText(GUILanguageResource.getProperty("ShowStubFile"));
                    mShowTestStubFile.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    if (!demo.getCurrentFile().isHasBuildedTest())
                        mShowTestStubFile.setEnabled(false);
                    mShowTestStubFile.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doShowTestStubFile();
                            }
                        }
                    });


                    MenuItem mShowInstruFile = WidgetFactory.createMenuItem(showMenu, SWT.PUSH, "Test", Resource.InstrumentSmall, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    mShowInstruFile.setText(GUILanguageResource.getProperty("ShowInstruFile"));
                    mShowInstruFile.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    if (!demo.getCurrentFile().isHasBuildedTest())
                        mShowInstruFile.setEnabled(false);
                    mShowInstruFile.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doShowInstruFile();
                            }
                        }
                    });

                    MenuItem genCFG = WidgetFactory.createMenuItem(menu, SWT.CASCADE, "Test", Resource.genCFGImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    genCFG.setText(GUILanguageResource.getProperty("GenCFG"));

                    Menu showCFGMenu = WidgetFactory.createMenu(demo.getShell(), SWT.DROP_DOWN, genCFG, true);

                    MenuItem mShowCFG = WidgetFactory.createMenuItem(showCFGMenu, SWT.PUSH, "Test", Resource.mShowCFGImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    mShowCFG.setText(GUILanguageResource.getProperty("GenerateCFG"));
                    mShowCFG.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    if (!demo.getCurrentFile().isHasAnalysised())
                        mShowCFG.setEnabled(false);
                    mShowCFG.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doShowControlFlowGraph();
                            }
                        }
                    });


                    MenuItem genSimpleCFG = WidgetFactory.createMenuItem(showCFGMenu, SWT.PUSH, "Test", Resource.GenSimpleCFGImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    genSimpleCFG.setText(GUILanguageResource.getProperty("GenSimpleCFG"));
                    genSimpleCFG.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��

                    genSimpleCFG.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doShowSimpleCFG();
                            }
                        }
                    });



                    MenuItem autoTestMenuItem = WidgetFactory.createMenuItem(menu, SWT.CASCADE, "Test", Resource.testSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    autoTestMenuItem.setText(GUILanguageResource.getProperty("AutoTest"));
                    autoTestMenuItem.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��

                    autoTestMenuItem.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                // demo.doAutoTest();
                            }
                        }
                    });

                    Menu autoTestMenu = WidgetFactory.createMenu(demo.getShell(), SWT.DROP_DOWN, autoTestMenuItem, true);
                    MenuItem autoTestcaseGenerate = WidgetFactory.createMenuItem(autoTestMenu, SWT.PUSH, "Test", Resource.randomTestImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    autoTestcaseGenerate.setText(GUILanguageResource.getProperty("AutoTestcaseGenerate"));
                    autoTestcaseGenerate.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    autoTestcaseGenerate.addListener(SWT.Selection, new Listener() {

                        @Override
                        public void handleEvent(Event e) {
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData();
                            if (selected != null) {
                                Config.BoundaryTest = true;
                                Config.isTestCaseGenerate = true;
                                demo.actionsGUI.doAutoTestCaseGenerate();
                            }
                        }
                    });

                    if (Config.isDebugVersion) {// ����ڲ����԰汾�Ļ�������ʾ��Щ�˵���
                        MenuItem randomTestcaseGenerate = WidgetFactory.createMenuItem(autoTestMenu, SWT.PUSH, "Test", Resource.randomTestImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                        randomTestcaseGenerate.setText(GUILanguageResource.getProperty("RandomTestcaseGenerate"));
                        randomTestcaseGenerate.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��

                        randomTestcaseGenerate.addListener(SWT.Selection, new Listener() {
                            public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                if (selected != null) {
                                    Config.BoundaryTest = true;
                                    Config.isTestCaseGenerate = true;
                                    demo.actionsGUI.doRandomTestBaseInputDomainAndPathAndBoundary();
                                }
                            }
                        });

                        MenuItem constraintTestcaseGenerate = WidgetFactory.createMenuItem(autoTestMenu, SWT.PUSH, "Test", Resource.constraintTestImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                        constraintTestcaseGenerate.setText(GUILanguageResource.getProperty("ConstraintTestcaseGenerate"));
                        constraintTestcaseGenerate.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��

                        constraintTestcaseGenerate.addListener(SWT.Selection, new Listener() {
                            public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                if (selected != null) {
                                    Config.isTestCaseGenerate = true;
                                    demo.actionsGUI.doTestBaseRandomAndPath();
                                }
                            }
                        });
                    }


                    MenuItem userAssistTestcaseGenerate = WidgetFactory.createMenuItem(autoTestMenu, SWT.PUSH, "Test", Resource.manualInputTestImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    userAssistTestcaseGenerate.setText(GUILanguageResource.getProperty("UserAssistTestcaseGenerate"));
                    userAssistTestcaseGenerate.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��

                    userAssistTestcaseGenerate.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doManualIntervention();
                            }
                        }
                    });

                    MenuItem regressionTestItem = WidgetFactory.createMenuItem(autoTestMenu, SWT.PUSH, "Test", Resource.regressionTestImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    regressionTestItem.setText(GUILanguageResource.getProperty("regressionTest"));
                    regressionTestItem.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��

                    regressionTestItem.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                Config.isTestCaseGenerate = true;
                                demo.actionsGUI.doRegressionTest();
                            }
                        }
                    });

                    if (Config.isDebugVersion) {// ������ڲ����԰棬����ʾ��Щ�˵���
                        if (unittest.util.Config.testWithSingleType) {
                            MenuItem randomTestBaseInputDomain = WidgetFactory.createMenuItem(autoTestMenu, SWT.PUSH, "Test", Resource.randomTestBaseInputDomainSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                            randomTestBaseInputDomain.setText(GUILanguageResource.getProperty("RandomTestBaseInputDomain"));
                            randomTestBaseInputDomain.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��

                            randomTestBaseInputDomain.addListener(SWT.Selection, new Listener() {
                                public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                    MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                    String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                    if (selected != null) {
                                        Config.BoundaryTest = false;
                                        Config.isTestCaseGenerate = true;
                                        demo.actionsGUI.doRandomTestBaseInputDomain();
                                    }
                                }
                            });

                            MenuItem randomTestBasePath = WidgetFactory.createMenuItem(autoTestMenu, SWT.PUSH, "Test", Resource.RandomTestBaseInputDomainAndPathSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                            randomTestBasePath.setText(GUILanguageResource.getProperty("RandomTestBaseInputDomainAndPath"));
                            randomTestBasePath.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                            randomTestBasePath.addListener(SWT.Selection, new Listener() {
                                public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                    MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                    String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                    if (selected != null) {
                                        Config.BoundaryTest = false;
                                        Config.isTestCaseGenerate = true;
                                        demo.actionsGUI.doRandomTestBaseInputDomainAndPath();
                                    }
                                }
                            });

                            MenuItem testBasePathConstraintSolving =
                                    WidgetFactory.createMenuItem(autoTestMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                            testBasePathConstraintSolving.setText(GUILanguageResource.getProperty("RandomTestBasedInputDomainAndTestBasedPath"));
                            testBasePathConstraintSolving.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                            testBasePathConstraintSolving.addListener(SWT.Selection, new Listener() {
                                public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                    MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                    String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                    if (selected != null) {
                                        Config.BoundaryTest = false;
                                        Config.isTestCaseGenerate = true;
                                        demo.actionsGUI.doTestBaseRandomAndPath();
                                    }
                                }
                            });


                            MenuItem pathBasedRandomTest = WidgetFactory.createMenuItem(autoTestMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                            pathBasedRandomTest.setText("����·�����������");
                            pathBasedRandomTest.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                            pathBasedRandomTest.addListener(SWT.Selection, new Listener() {
                                public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                    MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                    String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                    if (selected != null) {
                                        Config.isTestCaseGenerate = true;
                                        demo.actionsGUI.doPathBasedRandomTest();
                                    }
                                }
                            });

                            MenuItem pathBasedBoundaryTest = WidgetFactory.createMenuItem(autoTestMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                            pathBasedBoundaryTest.setText("���ڱ߽�ֵ���������");
                            pathBasedBoundaryTest.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                            pathBasedBoundaryTest.addListener(SWT.Selection, new Listener() {
                                public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                    MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                    String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                    if (selected != null) {
                                        Config.BoundaryTest = true;
                                        Config.isTestCaseGenerate = true;
                                        demo.actionsGUI.doPathBasedBoundaryTest();
                                    }
                                }
                            });

                            MenuItem constraintTest = WidgetFactory.createMenuItem(autoTestMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                            constraintTest.setText("����Լ�����Ĳ���");
                            constraintTest.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                            constraintTest.addListener(SWT.Selection, new Listener() {
                                public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                    MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                    String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                    if (selected != null) {
                                        Config.isTestCaseGenerate = true;
                                        demo.actionsGUI.doConstraintsTest();
                                    }
                                }
                            });

                            MenuItem boundaryValueTest = WidgetFactory.createMenuItem(autoTestMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true);
                            boundaryValueTest.setText(GUILanguageResource.getProperty("BoundaryValueTest"));
                            boundaryValueTest.setData(selected.getText());
                            boundaryValueTest.addListener(SWT.Selection, new Listener() {

                                @Override
                                public void handleEvent(Event arg0) {

                                }
                            });

                            MenuItem mutationTest = WidgetFactory.createMenuItem(autoTestMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true);
                            mutationTest.setText(GUILanguageResource.getProperty("MutationTest"));
                            mutationTest.setData(selected.getText());
                            mutationTest.addListener(SWT.Selection, new Listener() {

                                @Override
                                public void handleEvent(Event arg0) {

                                }
                            });

                        }
                    }

                    MenuItem showResultItem = WidgetFactory.createMenuItem(menu, SWT.CASCADE, "Test", Resource.ShowResult, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    showResultItem.setText(GUILanguageResource.getProperty("ShowResult"));
                    Menu showResultMenu = WidgetFactory.createMenu(demo.getShell(), SWT.DROP_DOWN, showResultItem, true);

                    if (!Config.IsDemoVersion) {
                        MenuItem mShowAvaiableTestCases = WidgetFactory.createMenuItem(showResultMenu, SWT.PUSH, "Test", Resource.showAllTestCaseSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                        mShowAvaiableTestCases.setText(GUILanguageResource.getProperty("showAllTestCase"));
                        mShowAvaiableTestCases.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                        mShowAvaiableTestCases.addListener(SWT.Selection, new Listener() {
                            public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                if (selected != null) {
                                    demo.doShowTestCasesTree();
                                    demo.doShowAvaiableTestCases();
                                }
                            }
                        });
                    }

                    MenuItem showCoverageWindow = WidgetFactory.createMenuItem(showResultMenu, SWT.PUSH, "Test", Resource.ShowCoverageWindowImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    showCoverageWindow.setText(GUILanguageResource.getProperty("ShowCoverageWindow"));
                    showCoverageWindow.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    showCoverageWindow.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.showCoverageWindow();
                            }
                        }
                    });

                    MenuItem showTestResult = WidgetFactory.createMenuItem(showResultMenu, SWT.CASCADE, "Test", Resource.TestResultSmall, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    showTestResult.setText(GUILanguageResource.getProperty("ShowTestResult"));
                    showTestResult.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    showTestResult.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                // demo.doShowTestResult();
                            }
                        }
                    });

                    Menu showTestResultMenu = WidgetFactory.createMenu(demo.getShell(), SWT.DROP_DOWN, showTestResult, true);

                    MenuItem showTestResultItem = WidgetFactory.createMenuItem(showTestResultMenu, SWT.PUSH, "Test", Resource.TestResultSmall, SWT.CTRL + 'R', true);
                    showTestResultItem.setText(GUILanguageResource.getProperty("open"));
                    showTestResultItem.setData(selected.getText());
                    if (!demo.getCurrentFile().isHasBuildedTest())
                        showTestResultItem.setEnabled(false);
                    showTestResultItem.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doShowTestResult();
                            }
                        }
                    });

                    MenuItem showTestResultWithIEItem = WidgetFactory.createMenuItem(showTestResultMenu, SWT.PUSH, "Test", Resource.TestResultSmall, SWT.CTRL + 'R', true);
                    showTestResultWithIEItem.setText(GUILanguageResource.getProperty("openWithIE"));
                    showTestResultWithIEItem.setData(selected.getText());
                    if (!demo.getCurrentFile().isHasBuildedTest()) {
                        showTestResultWithIEItem.setEnabled(false);
                        showResultItem.setEnabled(false);
                    }
                    showTestResultWithIEItem.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doShowTestResultWithIE();
                            }
                        }
                    });

                    MenuItem funcAttributeItem = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", null, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    funcAttributeItem.setText(GUILanguageResource.getProperty("FuncAttribute"));
                    funcAttributeItem.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event arg0) {
                            UATAttributeGUI funcAttributeGUI = new UATAttributeGUI(demo.getCurrentProject());
                            funcAttributeGUI.showFunctionAttribute(funcName);
                        }
                    });


                    // //added by songhao 2013-9-23
                    // //����ʵ��ƽ̨
                    // /**
                    // * ������벻������update /tools��
                    // */
                    // MenuItem MutationItem = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test",
                    // null, 0, true); //Ϊ�˵������˵���
                    // MutationItem.setText("����ʵ��ƽ̨");
                    // MutationItem.addListener(SWT.Selection, new Listener() {
                    // public void handleEvent(Event arg0) {
                    // ExperimentPlatform platform = new ExperimentPlatform(demo.getCurrentFunc());
                    // }
                    // });
                    // //end songhao
                    demo.getProjectViewTree().setMenu(menu);
                    demo.getProjectViewTree().getMenu().setVisible(true);
                } else// warning:����ļ������ԡ�����.c�����Ļ����˵����ܾͻ�ҡ�
                {
                    demo.getCurrentProject().setFolderOperation(true);
                    if (selected != null) {
                        ArrayList<String> fileset = new ArrayList<String>();
                        String folderName = getFullPathName(selected);
                        folderName = demo.getCurrentProject().getSourceCodePathString() + File.separator + folderName;
                        for (int i = 0; i < demo.getCurrentProject().getFilenameList().size(); i++) {
                            String analysisfile = demo.getCurrentProject().getFilenameList().get(i);
                            if (analysisfile.startsWith(folderName)) {
                                fileset.add(analysisfile);
                            }
                        }
                        demo.getCurrentProject().setFolderFilenameList(fileset);
                    }
                    MenuItem mi4 = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.ModuleSeparateAllFilesImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    mi4.setText(GUILanguageResource.getProperty("ModuleSeparateAllFiles"));
                    mi4.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    mi4.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget

                            if (selected != null) {
                                MessageBox box = new MessageBox(demo.getShell(), SWT.ICON_INFORMATION | SWT.OK | SWT.CANCEL);
                                box.setText("��ʾ��Ϣ");
                                box.setMessage("ģ�黮�ֻ�������ڱ�Ŀ¼�ļ������в�������,������?");

                                int ans = box.open();
                                if (ans == SWT.OK) {
                                    demo.doModuleSeparateForSelectedFiles(demo.getCurrentProject().getFolderFilenameList());
                                    Config.needSavePro = true;
                                }
                            }
                        }
                    });

                    MenuItem autoTestFiles = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.TestReportSmall, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    autoTestFiles.setText(GUILanguageResource.getProperty("AutoTestcaseGenerate"));
                    autoTestFiles.setData(selected.getData()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    autoTestFiles.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                            String selected = mi.getData().toString(); // ȡ���ڽ����˵���ʱ���������Ķ���
                            if (selected != null) {
                                demo.actionsGUI.doAutoTestForProject(selected);
                            }
                        }
                    });

                    if (!Config.IsDemoVersion) {
                        MenuItem mTest = WidgetFactory.createMenuItem(menu, SWT.CASCADE, "Test", Resource.testSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                        mTest.setText(GUILanguageResource.getProperty("TestAllFuncInAllFiles"));
                        mTest.setData(selected.getText());

                        Menu testMenu = WidgetFactory.createMenu(demo.getShell(), SWT.DROP_DOWN, mTest, true);

                        MenuItem randomTestcaseGenerate = WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.randomTestImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                        randomTestcaseGenerate.setText(GUILanguageResource.getProperty("RandomTestcaseGenerate"));
                        randomTestcaseGenerate.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��

                        randomTestcaseGenerate.addListener(SWT.Selection, new Listener() {
                            public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                if (selected != null) {
                                    demo.actionsGUI.doAutoRandomTestBasedInputDomainAndPathForAllFiles();
                                }
                            }
                        });

                        MenuItem constraintTestcaseGenerate = WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.constraintTestImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                        constraintTestcaseGenerate.setText(GUILanguageResource.getProperty("ConstraintTestcaseGenerate"));
                        constraintTestcaseGenerate.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��

                        constraintTestcaseGenerate.addListener(SWT.Selection, new Listener() {
                            public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                if (selected != null) {
                                    demo.actionsGUI.doAutoTestBasedRandomAndPathForAllFiles();
                                }
                            }
                        });

                        if (Config.isDebugVersion) {// ������ڲ����԰汾������ʾ������Щ�˵���
                            if (unittest.util.Config.testWithSingleType) {
                                MenuItem autoRandomTestBasedInputDomainForAllFiles =
                                        WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.randomTestBaseInputDomainSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                                autoRandomTestBasedInputDomainForAllFiles.setText(GUILanguageResource.getProperty("AutoRandomTestBasedInputDomainForAllFiles"));
                                autoRandomTestBasedInputDomainForAllFiles.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                                autoRandomTestBasedInputDomainForAllFiles.addListener(SWT.Selection, new Listener() {
                                    public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                        MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                        String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                        if (selected != null) {
                                            demo.actionsGUI.doAutoRandomTestBasedInputDomainForAllFiles();
                                        }
                                    }
                                });
                                MenuItem autoRandomTestBasedInputDomainAndPathForAllFiles =
                                        WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.RandomTestBaseInputDomainAndPathSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                                autoRandomTestBasedInputDomainAndPathForAllFiles.setText(GUILanguageResource.getProperty("AutoRandomTestBasedInputDomainAndPathForAllFiles"));
                                autoRandomTestBasedInputDomainAndPathForAllFiles.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                                autoRandomTestBasedInputDomainAndPathForAllFiles.addListener(SWT.Selection, new Listener() {
                                    public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                        MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                        String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                        if (selected != null) {
                                            demo.actionsGUI.doAutoRandomTestBasedInputDomainAndPathForAllFiles();
                                        }
                                    }
                                });
                                MenuItem autoTestBasedRandomAndPathForAllFiles =
                                        WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                                autoTestBasedRandomAndPathForAllFiles.setText(GUILanguageResource.getProperty("AutoTestBasedRandomAndPathForAllFiles"));
                                autoTestBasedRandomAndPathForAllFiles.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                                autoTestBasedRandomAndPathForAllFiles.addListener(SWT.Selection, new Listener() {
                                    public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                        MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                        String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                        if (selected != null) {
                                            demo.actionsGUI.doAutoTestBasedRandomAndPathForAllFiles();
                                        }
                                    }
                                });

                                MenuItem autoPathBasedTestForAllFiles =
                                        WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                                autoPathBasedTestForAllFiles.setText("�������ļ�������·�����������");
                                autoPathBasedTestForAllFiles.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                                autoPathBasedTestForAllFiles.addListener(SWT.Selection, new Listener() {
                                    public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                        MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                        String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                        if (selected != null) {
                                            demo.actionsGUI.doPathBasedRandomTestForAllFiles();
                                        }
                                    }
                                });

                                MenuItem autoConstraintsTestForAllFiles =
                                        WidgetFactory.createMenuItem(testMenu, SWT.PUSH, "Test", Resource.testBasePathConstraintSolvingSmallImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                                autoConstraintsTestForAllFiles.setText("�������ļ�������Լ�����Ĳ���");
                                autoConstraintsTestForAllFiles.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                                autoConstraintsTestForAllFiles.addListener(SWT.Selection, new Listener() {
                                    public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                                        MenuItem mi = (MenuItem) e.widget; // ȡ�˵���Widget
                                        String selected = (String) mi.getData(); // ȡ���ڽ����˵���ʱ���������Ķ���
                                        if (selected != null) {
                                            demo.actionsGUI.doConstraintsTestForAllFiles();
                                        }
                                    }
                                });
                            }
                        }
                    }


                    MenuItem folderAttribute = WidgetFactory.createMenuItem(menu, SWT.PUSH, "Test", Resource.FileAttributeImage, SWT.CTRL + 'R', true); // Ϊ�˵������˵���
                    folderAttribute.setText("�ļ�������");
                    folderAttribute.setData(selected.getText()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
                    folderAttribute.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) { // ��ָ���û�������Ϣ!
                            UATAttributeGUI fileAttributeGUI = new UATAttributeGUI(demo.getCurrentProject());
                            fileAttributeGUI.showFolderAttribute(getFullPathName(selected));
                        }
                    });
                    demo.getProjectViewTree().setMenu(menu);
                    demo.getProjectViewTree().getMenu().setVisible(true);
                }
            }
        }
    }

    private String getFullPathName(TreeItem item) {
        String res = "";
        while (item.getParentItem() != null) {
            res = item.getText() + File.separator + res;
            item = item.getParentItem();
        }
        if (res.endsWith(File.separator))
            res = res.substring(0, res.length() - 1);
        return res;
    }
}
