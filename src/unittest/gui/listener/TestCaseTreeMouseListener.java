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
 * ����������������¼���Ӧ����
 * 
 * created by Yaoweichang on 2015-04-17 ����11:28:59
 */
public class TestCaseTreeMouseListener extends MouseAdapter {
    private UATGUI demo;

    public TestCaseTreeMouseListener(UATGUI uatGui) {
        this.demo = uatGui;
    }

    public void mouseUp(MouseEvent e) {
        TreeItem selected = demo.getTestCaseTree().getItem(new Point(e.x, e.y)); // ȡ�ڵ�ؼ�
        if (selected != null) {
            if (selected.getData() != null)
                demo.setCurrentTestCaseID((Long) selected.getData());
            else
                demo.setCurrentTestCaseID(-1);
            demo.doShowAvaiableTestCases();

            demo.getTestCaseTree().setMenu(null);
            if (e.button == 3 && !selected.getText().equals("����������") && !selected.getText().equals("���޲�������")) {
                Menu testCaseMenu = WidgetFactory.createMenu(demo.getShell(), SWT.POP_UP); // Ϊ�ڵ㽨POP
                                                                                           // UP�˵�

                MenuItem runTestCase = WidgetFactory.createMenuItem(testCaseMenu, SWT.PUSH, "Test", null, 0, true);
                runTestCase.setText("����");
                runTestCase.setData(selected.getData()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ��
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
                    runTrackItem.setText("�鿴ִ�й켣");

                    Menu runTrack = WidgetFactory.createMenu(demo.getShell(), SWT.DROP_DOWN, runTrackItem, true);
                    MenuItem sourceCodeItem = WidgetFactory.createMenuItem(runTrack, SWT.PUSH, "Test", null, 0, true);
                    sourceCodeItem.setText("Դ���븲��");
                    sourceCodeItem.setData(selected.getData()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ
                    sourceCodeItem.addListener(SWT.Selection, new Listener() {
                        public void handleEvent(Event e) {
                            MenuItem mi = (MenuItem) e.widget;
                            demo.actionsGUI.showOneCaseCoverageWindow((Long) mi.getData());
                        }
                    });

                    MenuItem controlFlowDiagramItem = WidgetFactory.createMenuItem(runTrack, SWT.PUSH, "Test", null, 0, true);
                    controlFlowDiagramItem.setText("������ͼ");
                    controlFlowDiagramItem.setData(selected.getData()); // ����Ӧ�˵����¼��Ĵ��룬����ֵ
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
