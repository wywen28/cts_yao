package unittest.gui;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.util.Config;
import unittest.util.CoverRule;

/*
 * add by za
 * 多覆盖准则选取界面
 */
public class UATCoverRuleSelectGUI {
    private UATGUI uatGui = null;
    private Shell shell = null;
    private Display display = null;

    private Composite composite = null;

    private Button blockButton = null;
    private Button branchButton = null;
    private Button MCDCButton = null;
    private Button blockBoundaryMutationButton = null;
    private Button branchBoundaryMutationButton = null;
    private Button MCDCBoundaryMutationButton = null;

    private Button okButton = null;
    private Button cancelButton = null;

    private CoverRule criteria;
    private CoverRule orig_criteria;

    public UATCoverRuleSelectGUI(UATGUI demo) {
        this.uatGui = demo;
        this.shell = new Shell(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL | SWT.MAX | SWT.MIN);
        this.criteria = new CoverRule();
        this.criteria.setCoverRule(uatGui.getCurrentCoverCriteria());
        this.orig_criteria = new CoverRule();
        this.orig_criteria.setCoverRule(uatGui.getCurrentCoverCriteria());
    }

    public void go() {
        display = Display.getDefault();
        this.createShell();
        this.dealEvent();
        this.shell.open();
        LayoutUtil.centerShell(display, shell);

        while (!display.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }

    private void createShell() {
        shell = new Shell(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.MAX | SWT.MIN);
        shell.setText(GUILanguageResource.getProperty("CoverRuleSelect"));
        shell.setImage(Resource.UATImage);
        shell.setBounds(50, 50, 300, 330);
        shell.setLayout(new FormLayout());
        shell.setMaximized(false);
        createComposite();
    }

    /**
     * 覆盖准则选取界面的事件处理方法
     * 
     * created by Yaoweichang on 2015-04-17 上午11:17:17
     */
    public void dealEvent() {
        shell.addShellListener(new ShellCloseListener(this));

        okButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (!(blockButton.getSelection() || branchButton.getSelection() || MCDCButton.getSelection() || blockBoundaryMutationButton.getSelection()
                        || branchBoundaryMutationButton.getSelection() || MCDCBoundaryMutationButton.getSelection())) {
                    Display.getDefault().syncExec(new Runnable() {
                        public void run() {
                            MessageBox mb = WidgetFactory.createInfoMessageBox(shell, "警告", "请至少选择一种覆盖准则");
                            mb.open();
                        }
                    });
                    return;
                }
                criteria.disableAll();
                if (blockButton.getSelection())
                    criteria.BlockCover = true;
                if (branchButton.getSelection())
                    criteria.BranchCover = true;
                if (MCDCButton.getSelection())
                    criteria.MCDCCover = true;
                if (blockBoundaryMutationButton.getSelection())
                    criteria.blockBoundaryMutationCover = true;
                if (branchBoundaryMutationButton.getSelection())
                    criteria.branchBoundaryMutationCover = true;
                if (MCDCBoundaryMutationButton.getSelection())
                    criteria.MCDCBoundaryMutationCover = true;

                if (!orig_criteria.toString().equals(criteria.toString())) {
                    uatGui.setCurrentCoverCriteria(criteria);
                    uatGui.getCurrentProject().setCriteria(criteria);
                    uatGui.setCurrentCoverCriteria(criteria);
                    Config.needSavePro = true;

                    boolean hadModuleSeparated = false;
                    for (int i = 0; i < uatGui.getCurrentProject().getFilenameList().size(); i++) {
                        if (uatGui.getCurrentProject().getIsModuleSeparated().get(i)) {
                            hadModuleSeparated = true;
                            break;
                        }
                    }
                    if (hadModuleSeparated) {// 已经对工程树中的某些文件进行过模块划分
                        MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK | SWT.CANCEL);
                        box.setText("提示信息");
                        box.setMessage("需要重新进行模块划分吗?");
                        int ans = box.open();
                        if (ans == SWT.OK) {
                            ArrayList<String> hasAnalysisedFiles = new ArrayList<String>();
                            for (int i = 0; i < uatGui.getCurrentProject().getFilenameList().size(); i++) {
                                if (uatGui.getCurrentProject().getIsModuleSeparated().get(i))
                                    hasAnalysisedFiles.add(uatGui.getCurrentProject().getFilenameList().get(i));
                            }
                            uatGui.doModuleSeparateForSelectedFiles(hasAnalysisedFiles);
                        }
                    } else {
                        MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
                        box.setText("提示信息");
                        box.setMessage("覆盖准则已修改成功！");
                        box.open();
                    }
                }

                uatGui.getShell().setEnabled(true);
                shell.dispose();

            }
        });

        cancelButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                uatGui.getShell().setEnabled(true);
                shell.dispose();
                return;

            }
        });
    }

    private void createComposite() {
        // 重新修改覆盖准则选取界面的布局 modify by Yaoweichang
        composite = WidgetFactory.createComposite(shell, SWT.BORDER);
        composite.setLayout(new FormLayout());
        WidgetFactory.configureFormData(composite, new FormAttachment(0, 5), new FormAttachment(0, 5), new FormAttachment(100, -5), new FormAttachment(100, -5));
        blockButton = WidgetFactory.createButton(composite, SWT.CHECK, GUILanguageResource.getProperty("BlockCover"));
        branchButton = WidgetFactory.createButton(composite, SWT.CHECK, GUILanguageResource.getProperty("BranchCover"));
        MCDCButton = WidgetFactory.createButton(composite, SWT.CHECK, GUILanguageResource.getProperty("MCDCCover"));
        blockBoundaryMutationButton = WidgetFactory.createButton(composite, SWT.CHECK, GUILanguageResource.getProperty("BlockCover") + GUILanguageResource.getProperty("BoundaryMutationCover"));
        branchBoundaryMutationButton = WidgetFactory.createButton(composite, SWT.CHECK, GUILanguageResource.getProperty("BranchCover") + GUILanguageResource.getProperty("BoundaryMutationCover"));
        MCDCBoundaryMutationButton = WidgetFactory.createButton(composite, SWT.CHECK, GUILanguageResource.getProperty("MCDCCover") + GUILanguageResource.getProperty("BoundaryMutationCover"));
        blockBoundaryMutationButton.setEnabled(false);
        branchBoundaryMutationButton.setEnabled(false);
        MCDCBoundaryMutationButton.setEnabled(false);
        okButton = WidgetFactory.createButton(composite, SWT.PUSH, GUILanguageResource.getProperty("OK"));
        cancelButton = WidgetFactory.createButton(composite, SWT.PUSH, GUILanguageResource.getProperty("Cancel"));

        if (criteria.BlockCover)
            blockButton.setSelection(true);
        if (criteria.BranchCover)
            branchButton.setSelection(true);
        if (criteria.MCDCCover)
            MCDCButton.setSelection(true);
        if (criteria.blockBoundaryMutationCover)
            blockBoundaryMutationButton.setSelection(true);
        if (criteria.branchBoundaryMutationCover)
            branchBoundaryMutationButton.setSelection(true);
        if (criteria.MCDCBoundaryMutationCover)
            MCDCBoundaryMutationButton.setSelection(true);

        WidgetFactory.configureFormData(blockButton, new FormAttachment(0, 5), new FormAttachment(0, 5), null, null);
        WidgetFactory.configureFormData(branchButton, new FormAttachment(0, 5), new FormAttachment(0, 40), null, null);
        WidgetFactory.configureFormData(MCDCButton, new FormAttachment(0, 5), new FormAttachment(0, 75), null, null);
        WidgetFactory.configureFormData(blockBoundaryMutationButton, new FormAttachment(0, 5), new FormAttachment(0, 110), null, null);
        WidgetFactory.configureFormData(branchBoundaryMutationButton, new FormAttachment(0, 5), new FormAttachment(0, 145), null, null);
        WidgetFactory.configureFormData(MCDCBoundaryMutationButton, new FormAttachment(0, 5), new FormAttachment(0, 180), null, null);
        WidgetFactory.configureFormData(okButton, new FormAttachment(20, 100, 0), new FormAttachment(85, 100, 2), null, new FormAttachment(100, -2));
        WidgetFactory.configureFormData(cancelButton, null, new FormAttachment(85, 100, 2), new FormAttachment(80, 100, 0), new FormAttachment(100, -2));
    }

    public class ShellCloseListener extends ShellAdapter {
        private UATCoverRuleSelectGUI demo;

        public ShellCloseListener(UATCoverRuleSelectGUI demo) {
            this.demo = demo;
        }

        public void shellClosed(ShellEvent e) {
            demo.uatGui.getShell().setEnabled(true);
            demo.shell.dispose();
        }

    }

}
