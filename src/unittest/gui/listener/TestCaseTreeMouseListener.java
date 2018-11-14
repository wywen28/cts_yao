package unittest.gui.listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TreeItem;

import unittest.gui.UATGUI;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.testcase.generate.util.TestCaseNew;
import unittest.testcase.management.news.TestCaseLibManagerNew;
import unittest.util.Config;

/**
 * 测试用例树的鼠标事件响应函数
 * 
 * created by Yaoweichang on 2015-04-17 上午11:28:59
 */
public class TestCaseTreeMouseListener extends MouseAdapter {
    private UATGUI demo;

    public TestCaseTreeMouseListener(UATGUI uatGui) {
        this.demo = uatGui;
    }

    public void mouseUp(MouseEvent e) {
        TreeItem selected = demo.getTestCaseTree().getItem(new Point(e.x, e.y)); // 取节点控件
        if (selected != null) {
            if (selected.getData() != null)
                demo.setCurrentTestCaseID((Long) selected.getData());
            else
                demo.setCurrentTestCaseID(-1);
            demo.doShowAvaiableTestCases();

            demo.getTestCaseTree().setMenu(null);
            if (e.button == 3 && !selected.getText().equals("测试用例树") && !selected.getText().equals("暂无测试用例")) {
                Menu testCaseMenu = WidgetFactory.createMenu(demo.getShell(), SWT.POP_UP); // 为节点建POP
                                                                                           // UP菜单

                MenuItem runTestCase = WidgetFactory.createMenuItem(testCaseMenu, SWT.PUSH, "Test", null, 0, true);
                runTestCase.setText("运行");
                runTestCase.setData(selected.getData()); // 向响应菜单项事件的代码，传递值。
                runTestCase.addListener(SWT.Selection, new Listener() {
                    public void handleEvent(Event e) {
                        MenuItem mi = (MenuItem) e.widget;
                        final List<TestCaseNew> testCaseSet = new ArrayList<TestCaseNew>();
                        testCaseSet.add(TestCaseLibManagerNew.showOneTestCase(demo.getCurrentFunc(), (Long) mi.getData()));
                        demo.runSelectedSingleTC(testCaseSet);
                    }
                });

                if (!Config.IsDemoVersion) {
                    MenuItem runTrackItem = WidgetFactory.createMenuItem(testCaseMenu, SWT.CASCADE, "Test", Resource.genCFGImage, 0, true);
                    runTrackItem.setText("查看执行轨迹");

                    Menu runTrack = WidgetFactory.createMenu(demo.getShell(), SWT.DROP_DOWN, runTrackItem, true);
                    MenuItem sourceCodeItem = WidgetFactory.createMenuItem(runTrack, SWT.PUSH, "Test", null, 0, true);
                    sourceCodeItem.setText("源代码覆盖");
                    sourceCodeItem.setData(selected.getData()); // 向响应菜单项事件的代码，传递值
                    sourceCodeItem.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) {
                            MenuItem mi = (MenuItem) e.widget;
                            demo.actionsGUI.showOneCaseCoverageWindow((Long) mi.getData());
                        }
                    });

                    MenuItem controlFlowDiagramItem = WidgetFactory.createMenuItem(runTrack, SWT.PUSH, "Test", null, 0, true);
                    controlFlowDiagramItem.setText("控制流图");
                    controlFlowDiagramItem.setData(selected.getData()); // 向响应菜单项事件的代码，传递值
                    controlFlowDiagramItem.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) {
                            try {
                                demo.actionsGUI.showUATOneTestcasesToPathWindow();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }
                demo.getTestCaseTree().setMenu(testCaseMenu);
            }
        }
    }
}
