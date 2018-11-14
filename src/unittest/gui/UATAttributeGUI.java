package unittest.gui;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.metric.FileMetric;
import unittest.metric.MethodMetric;
import unittest.metric.ProjectMetric;
import unittest.module.seperate.TestModule;
import unittest.util.AnalysisFile;
import unittest.util.CodeCounter;
import unittest.util.Project;

public class UATAttributeGUI {
    static Logger logger = Logger.getLogger(UATSoftwareMetricGUI.class);

    private Tree tree;
    private Project currentProject = null;
    public Shell shell = null;
    private Display display = null;
    private Composite topComposite = null;
    private Composite bottomComposite = null;
    private Button okButton = null;

    public UATAttributeGUI(Project currentProject) {
        this.currentProject = currentProject;
        shell = new Shell(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL);
        display = Display.getDefault();
    }

    public void showProjectAttribute() {
        shell.setText(GUILanguageResource.getProperty("ProjectAttribute"));
        this.createShell();
        createProjectComposite();
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

    public void showFolderAttribute(String folderName) {
        shell.setText(GUILanguageResource.getProperty("FolderAttribute"));
        this.createShell();
        createFolderComposite(folderName);
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

    public void showFileAttribute(String fileName) {
        shell.setText(GUILanguageResource.getProperty("FileAttribute"));
        this.createShell();
        createFileComposite(fileName);
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

    public void showFunctionAttribute(String funcName) {
        shell.setText(GUILanguageResource.getProperty("FuncAttribute"));
        this.createShell();
        createFunctionComposite(funcName);
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
        shell.setImage(Resource.UATImage);
        shell.setBounds(50, 50, 400, 400);
        shell.setLayout(new FormLayout());
        shell.setMaximized(false);
    }

    private void createProjectComposite() {
        topComposite = WidgetFactory.createComposite(shell, SWT.FLAT);
        topComposite.setBackground(Resource.backgroundColor);
        topComposite.setLayout(new FormLayout());
        WidgetFactory.configureFormData(topComposite, new FormAttachment(0, 5), new FormAttachment(0, 5), new FormAttachment(100, -5), new FormAttachment(85, 100, 0));

        // add by xujiaoxian
        tree = new Tree(topComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE);
        tree.setHeaderVisible(false);
        tree.setLinesVisible(false);
        WidgetFactory.configureFormData(tree, new FormAttachment(0, 0), new FormAttachment(0, 0), new FormAttachment(100, 0), new FormAttachment(100, 0));
        // 修正界面属性显示错位问题 modify by Yaoweichang
        TreeColumn treeColumnProject = new TreeColumn(tree, SWT.CENTER);
        treeColumnProject.setText("00000000000" + GUILanguageResource.getProperty("ProjectName"));
        createTableColumnsNew();
        setProjectTreeItemText(currentProject.getName(), currentProject.getSourceCodePathString());

        for (TreeItem t : tree.getItems()) {
            t.setExpanded(true);
        }

        packColumns();
        createBottomComposite();
    }

    private void createFolderComposite(String folderName) {
        topComposite = WidgetFactory.createComposite(shell, SWT.FLAT);
        topComposite.setBackground(Resource.backgroundColor);
        topComposite.setLayout(new FormLayout());
        WidgetFactory.configureFormData(topComposite, new FormAttachment(0, 5), new FormAttachment(0, 5), new FormAttachment(100, -5), new FormAttachment(85, 100, 0));

        // add by xujiaoxian
        tree = new Tree(topComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE);
        tree.setHeaderVisible(false);
        tree.setLinesVisible(false);
        WidgetFactory.configureFormData(tree, new FormAttachment(0, 0), new FormAttachment(0, 0), new FormAttachment(100, 0), new FormAttachment(100, 0));

        // 修正界面属性显示错位问题 modify by Yaoweichang
        TreeColumn treeColumnProject = new TreeColumn(tree, SWT.CENTER);
        treeColumnProject.setText("0000000000" + GUILanguageResource.getProperty("FolderName"));
        createTableColumnsNew();
        setFolderTreeItemText(currentProject.getSourceCodePathString() + File.separator + folderName);
        // end add by xujiaoxian

        for (TreeItem t : tree.getItems()) {
            t.setExpanded(true);
        }

        packColumns();
        createBottomComposite();
    }

    private void createFileComposite(String fName) {
        topComposite = WidgetFactory.createComposite(shell, SWT.FLAT);
        topComposite.setBackground(Resource.backgroundColor);
        topComposite.setLayout(new FormLayout());
        WidgetFactory.configureFormData(topComposite, new FormAttachment(0, 5), new FormAttachment(0, 5), new FormAttachment(100, -5), new FormAttachment(85, 100, 0));

        // add by xujiaoxian
        tree = new Tree(topComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE);
        tree.setHeaderVisible(false);
        tree.setLinesVisible(false);
        WidgetFactory.configureFormData(tree, new FormAttachment(0, 0), new FormAttachment(0, 0), new FormAttachment(100, 0), new FormAttachment(100, 0));
        // 修正界面属性显示错位问题 modify by Yaoweichang
        TreeColumn treeColumnProject = new TreeColumn(tree, SWT.CENTER);
        treeColumnProject.setText("00000000000" + GUILanguageResource.getProperty("FileName"));
        createTableColumnsNew();
        String fullpathfileName = currentProject.getSourceCodePathString() + File.separator + fName;
        setFileTreeItemText(fullpathfileName);

        for (TreeItem t : tree.getItems()) {
            t.setExpanded(true);
        }

        packColumns();
        createBottomComposite();
    }

    private void createFunctionComposite(String fName) {
        topComposite = WidgetFactory.createComposite(shell, SWT.FLAT);
        topComposite.setBackground(Resource.backgroundColor);
        topComposite.setLayout(new FormLayout());
        WidgetFactory.configureFormData(topComposite, new FormAttachment(0, 5), new FormAttachment(0, 5), new FormAttachment(100, -5), new FormAttachment(85, 100, 0));

        // add by xujiaoxian
        tree = new Tree(topComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE);
        tree.setHeaderVisible(false);
        tree.setLinesVisible(false);
        WidgetFactory.configureFormData(tree, new FormAttachment(0, 0), new FormAttachment(0, 0), new FormAttachment(100, 0), new FormAttachment(100, 0));
        TreeColumn treeColumnProject = new TreeColumn(tree, SWT.CENTER);
        treeColumnProject.setText("00000000000" + GUILanguageResource.getProperty("FunctionName"));
        createTableColumnsNew();
        java.util.List<AnalysisFile> fileList = currentProject.getFileList();
        boolean find = false;
        for (int i = 0; !find && i < fileList.size(); i++) {
            AnalysisFile f = fileList.get(i);
            for (int j = 0; !find && j < f.getFunctionList().size(); j++) {
                TestModule t = f.getFunctionList().get(j);
                if (t.getFuncName().equals(fName)) {
                    setFuncTreeItemText(fName, f.getFile(), t.getMethodMetric());
                    find = true;
                }
            }
        }

        for (TreeItem t : tree.getItems()) {
            t.setExpanded(true);
        }

        packColumns();
        createBottomComposite();
    }

    public void createBottomComposite() {
        bottomComposite = WidgetFactory.createComposite(shell, SWT.BORDER);
        bottomComposite.setLayout(new FormLayout());
        WidgetFactory.configureFormData(bottomComposite, new FormAttachment(0, 5), new FormAttachment(topComposite, 5), new FormAttachment(100, -5), new FormAttachment(100, -5));
        okButton = WidgetFactory.createButton(bottomComposite, SWT.PUSH, GUILanguageResource.getProperty("OK"));
        WidgetFactory.configureFormData(okButton, new FormAttachment(45, 100, 0), new FormAttachment(40, 100, 0), null, null);
    }

    public void createTableColumns() {
        TreeColumn lineColumn = new TreeColumn(tree, SWT.CENTER);
        lineColumn.setText(GUILanguageResource.getProperty("lineColumn"));

        TreeColumn codeLineColumn = new TreeColumn(tree, SWT.CENTER);
        codeLineColumn.setText(GUILanguageResource.getProperty("codeLineColumn"));

        TreeColumn remarkLineColumn = new TreeColumn(tree, SWT.CENTER);
        remarkLineColumn.setText(GUILanguageResource.getProperty("remarkLineColumn"));

        TreeColumn blankLineColumn = new TreeColumn(tree, SWT.CENTER);
        blankLineColumn.setText(GUILanguageResource.getProperty("blankLineColumn"));

        TreeColumn arguNumColumn = new TreeColumn(tree, SWT.CENTER);
        arguNumColumn.setText(GUILanguageResource.getProperty("arguNumColumn"));

        TreeColumn localVarColumn = new TreeColumn(tree, SWT.CENTER);
        localVarColumn.setText(GUILanguageResource.getProperty("localVarColumn"));

        TreeColumn globVarColumn = new TreeColumn(tree, SWT.CENTER);
        globVarColumn.setText(GUILanguageResource.getProperty("globVarColumn"));

        TreeColumn branchNumColumn = new TreeColumn(tree, SWT.CENTER);
        branchNumColumn.setText(GUILanguageResource.getProperty("branchNumColumn"));

        TreeColumn loopNumColumn = new TreeColumn(tree, SWT.CENTER);
        loopNumColumn.setText(GUILanguageResource.getProperty("loopNumColumn"));

        TreeColumn maxLoopDepthColumn = new TreeColumn(tree, SWT.CENTER);
        maxLoopDepthColumn.setText(GUILanguageResource.getProperty("maxLoopDepthColumn"));

        TreeColumn calleeColumn = new TreeColumn(tree, SWT.CENTER);
        calleeColumn.setText(GUILanguageResource.getProperty("calleeColumn"));

        TreeColumn cyclomaticComplexityColumn = new TreeColumn(tree, SWT.CENTER);
        cyclomaticComplexityColumn.setText(GUILanguageResource.getProperty("cyclomaticComplexityColumn"));

        TreeColumn headFilesColumn = new TreeColumn(tree, SWT.CENTER);
        headFilesColumn.setText(GUILanguageResource.getProperty("headFilesColumn"));

        TreeColumn methodNumColumn = new TreeColumn(tree, SWT.CENTER);
        methodNumColumn.setText(GUILanguageResource.getProperty("methodNumColumn"));

        TreeColumn fileNumColumn = new TreeColumn(tree, SWT.CENTER);
        fileNumColumn.setText(GUILanguageResource.getProperty("fileNumColumn"));

    }

    /**
     * 创建显示属性信息的表格
     * add by xujiaoxian
     */
    public void createTableColumnsNew() {
        TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
    }

    public void packColumns() {
        TreeColumn[] columns = tree.getColumns();
        for (int i = 0, n = columns.length; i < n; i++) {
            columns[i].pack();
        }
    }

    public void dealEvent() {
        shell.addShellListener(new ShellCloseListener(this));

        okButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                shell.dispose();
            }
        });
    }

    void setTreeitemText(TreeItem ti, ProjectMetric metric) {
        ti.setText(1, "" + metric.getLine());
        ti.setText(2, "" + metric.getCodeLine() + "(" + metric.getCodeRate() + "%)");
        ti.setText(3, "" + metric.getAnnotationLine() + "(" + metric.getAnnotationRate() + "%)");
        ti.setText(4, "" + metric.getBlankLine() + "(" + metric.getBlankRate() + "%)");
        ti.setText(5, "" + metric.getArguNum());
        ti.setText(6, "" + metric.getLocalVar());
        ti.setText(7, "" + metric.getGlobVar());
        ti.setText(8, "" + metric.getBranchNum());
        ti.setText(9, "" + metric.getLoopNum());
        ti.setText(10, "" + metric.getMaxLoopDepth());
        ti.setText(11, "" + metric.getCallee());
        ti.setText(12, "");
        ti.setText(13, "" + metric.getHeadFiles());
        ti.setText(14, "" + metric.getMethodNum());
        ti.setText(15, "" + metric.getFileNum());

    }

    void setTreeitemText(TreeItem ti, FileMetric metric) {
        ti.setText(1, "" + metric.getLine());
        ti.setText(2, "" + metric.getCodeLine() + "(" + metric.getCodeRate() + "%)");
        ti.setText(3, "" + metric.getAnnotationLine() + "(" + metric.getAnnotationRate() + "%)");
        ti.setText(4, "" + metric.getBlankLine() + "(" + metric.getBlankRate() + "%)");
        ti.setText(5, "" + metric.getArguNum());
        ti.setText(6, "" + metric.getLocalVar());
        ti.setText(7, "" + metric.getGlobVar());
        ti.setText(8, "" + metric.getBranchNum());
        ti.setText(9, "" + metric.getLoopNum());
        ti.setText(10, "" + metric.getMaxLoopDepth());
        ti.setText(11, "" + metric.getCallee());
        ti.setText(12, "");
        ti.setText(13, "" + metric.getHeadFiles());
        ti.setText(14, "" + metric.getMethodNum());
        ti.setText(15, "1");

    }

    void setTreeitemText(TreeItem ti, MethodMetric metric) {
        ti.setText(1, "" + metric.getLine());
        ti.setText(2, "" + metric.getCodeLine() + "(" + metric.getCodeRate() + "%)");
        ti.setText(3, "" + metric.getAnnotationLine() + "(" + metric.getAnnotationRate() + "%)");
        ti.setText(4, "" + metric.getBlankLine() + "(" + metric.getBlankRate() + "%)");
        ti.setText(5, "" + metric.getArguNum());
        ti.setText(6, "" + metric.getLocalVar());
        ti.setText(7, "0");
        ti.setText(8, "" + metric.getBranchNum());
        ti.setText(9, "" + metric.getLoopNum());
        ti.setText(10, "" + metric.getMaxLoopDepth());
        ti.setText(11, "" + metric.getCallee());
        ti.setText(12, "" + metric.getCyclomaticComplexity());
        ti.setText(13, "0");
        ti.setText(14, "1");
        ti.setText(15, "0");

    }

    /**
     * add by xujiaoxian
     * 
     * @param metric
     */
    public void setFuncTreeItemText(String funcName, String location, MethodMetric metric) {
        TreeItem nameItem = new TreeItem(tree, SWT.NONE);
        nameItem.setText(0, GUILanguageResource.getProperty("FuncName") + ":");
        nameItem.setText(1, funcName);
        TreeItem typeItem = new TreeItem(tree, SWT.NONE);
        typeItem.setText(0, GUILanguageResource.getProperty("Type") + ":");
        typeItem.setText(1, GUILanguageResource.getProperty("FuncTypeName"));
        TreeItem lineItem = new TreeItem(tree, SWT.NONE);
        lineItem.setText(0, GUILanguageResource.getProperty("lineColumn") + ":");
        lineItem.setText(1, "" + metric.getLine());
        TreeItem codelineItem = new TreeItem(tree, SWT.NONE);
        codelineItem.setText(0, GUILanguageResource.getProperty("codeLineColumn") + ":");
        codelineItem.setText(1, "" + metric.getCodeLine() + "(" + metric.getCodeRate() + "%)");
        TreeItem remarkItem = new TreeItem(tree, SWT.NONE);
        remarkItem.setText(0, GUILanguageResource.getProperty("remarkLineColumn") + ":");
        remarkItem.setText(1, "" + metric.getAnnotationLine() + "(" + metric.getAnnotationRate() + "%)");
        TreeItem blankItem = new TreeItem(tree, SWT.NONE);
        blankItem.setText(0, GUILanguageResource.getProperty("blankLineColumn") + ":");
        blankItem.setText(1, "" + metric.getBlankLine() + "(" + metric.getBlankRate() + "%)");
        TreeItem locationItem = new TreeItem(tree, SWT.NONE);
        locationItem.setText(0, GUILanguageResource.getProperty("Location") + ":");
        locationItem.setText(1, location);
    }

    /**
     * 设置文件属性的具体信息
     * add by xujiaoxian
     * 
     * @param filename
     */
    public void setFileTreeItemText(String filename) {
        CodeCounter counter = new CodeCounter();
        counter.CountComplexity(filename);
        int index = filename.lastIndexOf(File.separator);
        String location = filename.substring(0, index);
        String fileNameWithoutPath = filename.substring(index + 1);

        TreeItem nameItem = new TreeItem(tree, SWT.NONE);
        nameItem.setText(0, GUILanguageResource.getProperty("FileName") + ":");
        nameItem.setText(1, fileNameWithoutPath);
        TreeItem typeItem = new TreeItem(tree, SWT.NONE);
        typeItem.setText(0, GUILanguageResource.getProperty("Type") + ":");
        typeItem.setText(1, GUILanguageResource.getProperty("FileTypeName"));
        TreeItem lineItem = new TreeItem(tree, SWT.NONE);
        lineItem.setText(0, GUILanguageResource.getProperty("lineColumn") + ":");
        lineItem.setText(1, "" + counter.getPHYSIC());
        TreeItem codelineItem = new TreeItem(tree, SWT.NONE);
        codelineItem.setText(0, GUILanguageResource.getProperty("codeLineColumn") + ":");
        codelineItem.setText(1, "" + counter.getCODE() + "(" + (int) (((float) counter.getCODE()) / ((float) counter.getPHYSIC()) * 100) + "%)");
        TreeItem remarkItem = new TreeItem(tree, SWT.NONE);
        remarkItem.setText(0, GUILanguageResource.getProperty("remarkLineColumn") + ":");
        remarkItem.setText(1, "" + counter.getREMARK() + "(" + (int) (((float) counter.getREMARK()) / ((float) counter.getPHYSIC()) * 100) + "%)");
        TreeItem blankItem = new TreeItem(tree, SWT.NONE);
        blankItem.setText(0, GUILanguageResource.getProperty("blankLineColumn") + ":");
        blankItem.setText(1, "" + counter.getBLANK() + "(" + (int) (((float) counter.getBLANK()) / ((float) counter.getPHYSIC()) * 100) + "%)");
        TreeItem includeItem = new TreeItem(tree, SWT.NONE);
        includeItem.setText(0, GUILanguageResource.getProperty("IncludeLines") + ":");
        includeItem.setText(1, "" + counter.getINCLUDE());
        TreeItem methodItem = new TreeItem(tree, SWT.NONE);
        methodItem.setText(0, GUILanguageResource.getProperty("MethodCount") + ":");
        methodItem.setText(1, "" + counter.getFUNCS());
        TreeItem locationItem = new TreeItem(tree, SWT.NONE);
        locationItem.setText(0, GUILanguageResource.getProperty("Location") + ":");
        locationItem.setText(1, location);

        counter.clear();
        counter = null;
    }

    /**
     * 处理文件夹属性的显示
     * add by xujiaoxian
     * 
     * @param folderPathName
     */
    public void setFolderTreeItemText(String folderPathName) {
        ArrayList<String> srcFiles = new ArrayList<String>();
        this.currentProject.collect(new File(folderPathName), srcFiles);
        CodeCounter counter = new CodeCounter();
        for (String srcfile : srcFiles)
            counter.CountComplexity(srcfile);
        int index = folderPathName.lastIndexOf(File.separator);
        String location = folderPathName.substring(0, index);
        String folderNameWithoutPath = folderPathName.substring(index + 1);

        TreeItem nameItem = new TreeItem(tree, SWT.NONE);
        nameItem.setText(0, GUILanguageResource.getProperty("FileName") + ":");
        nameItem.setText(1, folderNameWithoutPath);
        TreeItem typeItem = new TreeItem(tree, SWT.NONE);
        typeItem.setText(0, GUILanguageResource.getProperty("Type") + ":");
        typeItem.setText(1, GUILanguageResource.getProperty("FolderTypeName"));

        TreeItem lineItem = new TreeItem(tree, SWT.NONE);
        lineItem.setText(0, GUILanguageResource.getProperty("lineColumn") + ":");
        lineItem.setText(1, "" + counter.getPHYSIC());
        TreeItem codelineItem = new TreeItem(tree, SWT.NONE);
        codelineItem.setText(0, GUILanguageResource.getProperty("codeLineColumn") + ":");
        codelineItem.setText(1, "" + counter.getCODE() + "(" + (int) (((float) counter.getCODE()) / ((float) counter.getPHYSIC()) * 100) + "%)");

        TreeItem remarkItem = new TreeItem(tree, SWT.NONE);
        remarkItem.setText(0, GUILanguageResource.getProperty("remarkLineColumn") + ":");
        remarkItem.setText(1, "" + counter.getREMARK() + "(" + (int) (((float) counter.getREMARK()) / ((float) counter.getPHYSIC()) * 100) + "%)");
        TreeItem blankItem = new TreeItem(tree, SWT.NONE);
        blankItem.setText(0, GUILanguageResource.getProperty("blankLineColumn") + ":");
        blankItem.setText(1, "" + counter.getBLANK() + "(" + (int) (((float) counter.getBLANK()) / ((float) counter.getPHYSIC()) * 100) + "%)");

        TreeItem includeItem = new TreeItem(tree, SWT.NONE);
        includeItem.setText(0, GUILanguageResource.getProperty("IncludeLines") + ":");
        includeItem.setText(1, "" + counter.getINCLUDE());
        TreeItem methodItem = new TreeItem(tree, SWT.NONE);
        methodItem.setText(0, GUILanguageResource.getProperty("MethodCount") + ":");
        methodItem.setText(1, "" + counter.getFUNCS());

        TreeItem filesItem = new TreeItem(tree, SWT.NONE);
        filesItem.setText(0, GUILanguageResource.getProperty("FilesCount") + ":");
        filesItem.setText(1, "" + srcFiles.size());
        TreeItem locationItem = new TreeItem(tree, SWT.NONE);
        locationItem.setText(0, GUILanguageResource.getProperty("Location") + ":");
        locationItem.setText(1, location);

        srcFiles = null;
        counter.clear();
        counter = null;
    }

    /**
     * 处理工程属性的显示
     * add by xujiaoxian
     * 
     * @param folderPathName
     */
    public void setProjectTreeItemText(String projectName, String path) {
        ArrayList<String> srcFiles = new ArrayList<String>();
        this.currentProject.collect(new File(path), srcFiles);
        CodeCounter counter = new CodeCounter();
        for (String srcfile : srcFiles)
            counter.CountComplexity(srcfile);

        TreeItem nameItem = new TreeItem(tree, SWT.NONE);
        nameItem.setText(0, GUILanguageResource.getProperty("ProjectName") + ":");
        nameItem.setText(1, projectName);
        TreeItem typeItem = new TreeItem(tree, SWT.NONE);
        typeItem.setText(0, GUILanguageResource.getProperty("Type") + ":");
        typeItem.setText(1, GUILanguageResource.getProperty("ProjectTypeName"));

        TreeItem lineItem = new TreeItem(tree, SWT.NONE);
        lineItem.setText(0, GUILanguageResource.getProperty("lineColumn") + ":");
        lineItem.setText(1, "" + counter.getPHYSIC());
        TreeItem codelineItem = new TreeItem(tree, SWT.NONE);
        codelineItem.setText(0, GUILanguageResource.getProperty("codeLineColumn") + ":");
        codelineItem.setText(1, "" + counter.getCODE() + "(" + (int) (((float) counter.getCODE()) / ((float) counter.getPHYSIC()) * 100) + "%)");

        TreeItem remarkItem = new TreeItem(tree, SWT.NONE);
        remarkItem.setText(0, GUILanguageResource.getProperty("remarkLineColumn") + ":");
        remarkItem.setText(1, "" + counter.getREMARK() + "(" + (int) (((float) counter.getREMARK()) / ((float) counter.getPHYSIC()) * 100) + "%)");
        TreeItem blankItem = new TreeItem(tree, SWT.NONE);
        blankItem.setText(0, GUILanguageResource.getProperty("blankLineColumn") + ":");
        blankItem.setText(1, "" + counter.getBLANK() + "(" + (int) (((float) counter.getBLANK()) / ((float) counter.getPHYSIC()) * 100) + "%)");

        TreeItem includeItem = new TreeItem(tree, SWT.NONE);
        includeItem.setText(0, GUILanguageResource.getProperty("IncludeLines") + ":");
        includeItem.setText(1, "" + counter.getINCLUDE());
        TreeItem methodItem = new TreeItem(tree, SWT.NONE);
        methodItem.setText(0, GUILanguageResource.getProperty("MethodCount") + ":");
        methodItem.setText(1, "" + counter.getFUNCS());

        TreeItem filesItem = new TreeItem(tree, SWT.NONE);
        filesItem.setText(0, GUILanguageResource.getProperty("FilesCount") + ":");
        filesItem.setText(1, "" + srcFiles.size());

        TreeItem locationItem = new TreeItem(tree, SWT.NONE);
        locationItem.setText(0, GUILanguageResource.getProperty("TestedSrcPath") + ":");
        locationItem.setText(1, path);

        TreeItem projectLocationItem = new TreeItem(tree, SWT.NONE);
        projectLocationItem.setText(0, GUILanguageResource.getProperty("Location") + ":");
        projectLocationItem.setText(1, "" + currentProject.getPath());

        srcFiles = null;
        counter.clear();
        counter = null;
    }

    public class ShellCloseListener extends ShellAdapter {
        private UATAttributeGUI demo;

        public ShellCloseListener(UATAttributeGUI uatAttributeGUI) {
            this.demo = uatAttributeGUI;
        }

        public void shellClosed(ShellEvent e) {
            demo.shell.dispose();
        }

    }

}
