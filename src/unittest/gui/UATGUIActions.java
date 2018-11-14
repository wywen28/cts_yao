package unittest.gui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.TreeItem;

import unittest.UATGUITest;
import unittest.Exception.StubGenerateException;
import unittest.Exception.UnsupportedReturnTypeException;
import unittest.gui.helper.FileCTabItem;
import unittest.gui.helper.FileTabManager;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.gui.imageViewer.ImageViewer;
import unittest.localization.GUILanguageResource;
import unittest.managecoverelement.covereset.CoverSet;
import unittest.module.seperate.NoExitMethodException;
import unittest.module.seperate.TestModule;
import unittest.module.seperate.UnSupportedArgumentException;
import unittest.pathchoose.util.path.OnePath;
import unittest.pictureviewer.PictureViewer;
import unittest.pretreament.PretreamentException;
import unittest.regressiontest.RegressionTestReturnType;
import unittest.regressiontest.RegressionTester;
import unittest.staticAnalysis.StaticAnalysisException;
import unittest.util.AnalysisFile;
import unittest.util.CMDProcess;
import unittest.util.Config;
import unittest.util.CoverRule;
import unittest.util.FileOperationException;
import unittest.util.Project;
import unittest.util.RecordToLogger;
import unittest.util.TestType;

/**
 * @author Cai Min
 * 
 */
public class UATGUIActions {

    public static ArrayList<String> funcNameList;
    private static Logger logger = Logger.getLogger(UATGUIActions.class);
    private UATGUI demo;

    public UATGUIActions(UATGUI gui) {
        demo = gui;
    }

    public void addOutputMessage(String msg) {
        String timeMsg = new String();
        SimpleDateFormat tempDate = new SimpleDateFormat("yyyy-MM-dd" + " " + "HH:mm:ss");
        String datetime = tempDate.format(new java.util.Date());
        timeMsg += datetime;
        String outputMsg = timeMsg + "--->" + msg + "\n";
        demo.setOutputMsg(outputMsg);
        demo.getoutputMessageText().append(outputMsg);
    }

    public void clearOutputMessage() {
        SimpleDateFormat tempDate = new SimpleDateFormat("yyyy-MM-dd" + " " + "HH:mm:ss");
        String datetime = tempDate.format(new java.util.Date());
        String outputMsg = datetime;
        outputMsg = "CTS " + outputMsg + "\n";
        demo.setOutputMsg(outputMsg);
        demo.getoutputMessageText().setText(outputMsg);
    }

    /**
     * This function deal the about Event.
     * 
     * @see #dealHelpMenuEvent()
     */
    public void doAbout() {
        UATAboutGUI cag = new UATAboutGUI(demo);
        cag.uatGui.getShell().setEnabled(false);
        cag.go();
    }

    public void doAutoRandomTestBasedInputDomainAndPathForAllFiles() {
        doAutoTestForAllFiles(1);
    }

    public void doAutoRandomTestBasedInputDomainForAllFiles() {
        doAutoTestForAllFiles(0);
    }

    public void doAutoTestBasedRandomAndPathForAllFiles() {
        doAutoTestForAllFiles(2);
    }

    public void doPathBasedRandomTestForAllFiles() {
        doAutoTestForAllFiles(3);
    }

    public void doConstraintsTestForAllFiles() {
        doAutoTestForAllFiles(4);
    }

    /**
     * �������ļ��������еĲ��ԣ�����������������Ĳ��ԣ�����·����������ԣ�����·����Լ��������
     */
    public void doAutoTestForAllFiles(final int kind) {
        demo.getOutputTabFolder().setSelection(0);
        final Project currentProject = demo.getCurrentProject();
        if (currentProject == null)
            return;
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                ArrayList<AnalysisFile> fileList;
                if (currentProject.getFolderOperation())
                    fileList = currentProject.getFolderFileList();
                else
                    fileList = currentProject.getFileList();
                final CoverRule cr = demo.getCurrentCoverCriteria();
                for (AnalysisFile file : fileList) {
                    final String fileName = file.getFile();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            progressDisplayGUI.setTestProgressRunning(3);
                            progressDisplayGUI.setInfo("���� ���ļ� " + fileName + "�����Զ�����...");
                            addOutputMessage("���� ���ļ� " + fileName + "�����Զ�����...");
                            demo.setStatusBarInfo("���ڶ��ļ� " + fileName + "�����Զ�����...");
                        }
                    });
                    final AnalysisFile currentFile = file;
                    if (!currentFile.isHasAnalysised()) {
                        doModuleSeparateForFile(currentFile);
                    }
                    ArrayList<TestModule> functionList = file.getFunctionList();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                        }
                    });

                    for (TestModule tm : functionList) {
                        final String funcName = tm.getFuncName();
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                demo.setStatusBarInfo("���ڶԺ��� " + funcName + " ���� " + cr + " ����");
                                addOutputMessage("���ڶԺ��� " + funcName + " ���� " + cr + " ����");
                            }
                        });
                        try {
                            if (kind == 0) {
                                tm.autoInputDomainRandomTest(cr);
                            } else if (kind == 1)
                                tm.autoInputDomainRandomAndPathRamdomTest(cr);
                            else if (kind == 2)
                                tm.autoRandomAndPathTest(cr);
                            else if (kind == 3)
                                tm.autoPathBasedRandomTest(cr);
                            else if (kind == 4)
                                tm.autoConstraintTest(cr);
                        } catch (UnSupportedArgumentException e) {
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    progressDisplayGUI.setTestProgressOver(0);
                                    addOutputMessage("���� " + funcName + "���в����������ɲ�֧�ֵ���������");
                                }
                            });
                        } catch (NoExitMethodException e) {
                            final String msg = e.getMessage();
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    addOutputMessage("���� " + funcName + "������ѭ����û�г���" + msg);
                                    progressDisplayGUI.setTestProgressOver(0);
                                }
                            });
                        } catch (InterruptedException e) {
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                        } catch (Exception e) {
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                        }
                    }
                }
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        doProjectViewRefresh();
                        progressDisplayGUI.setTestProgressOver(1);
                        addOutputMessage("�����ļ��������");
                        demo.setStatusBarInfo("�����ļ��������");
                        MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "���Խ���", " ���к����Զ����Խ���");
                        box.open();
                    }
                });
                progressDisplayGUI.terminateListener.setRunningThread(null);
            }
        }.start();
    }

    /**
     * ��ָ�������������еĲ��ԣ�����������������Ĳ��ԣ�����·����������ԣ�����·����Լ��������
     */
    public void doAutoTestForSelectedFiles(final int kind, final Set<TestModule> tmSet) {
        demo.getOutputTabFolder().setSelection(0);
        Project currentProject = demo.getCurrentProject();
        if (currentProject == null)
            return;
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                final CoverRule cr = demo.getCurrentCoverCriteria();
                for (TestModule tm : tmSet) {
                    final String fileName = tm.getFileName();
                    final String funcName = tm.getFuncName();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            demo.setStatusBarInfo("���ڶ��ļ� " + fileName + " �ĺ��� " + funcName + " ���� " + cr + " ����");
                            addOutputMessage("���ڶ��ļ� " + fileName + " �ĺ��� " + funcName + " ���� " + cr + " ����");
                        }
                    });
                    try {
                        if (kind == 0) {
                            tm.autoInputDomainRandomTest(cr);
                        } else if (kind == 1)
                            tm.autoInputDomainRandomAndPathRamdomTest(cr);
                        else if (kind == 2)
                            tm.autoRandomAndPathTest(cr);
                        else if (kind == 3)
                            tm.autoPathBasedRandomTest(cr);
                        else if (kind == 4)
                            tm.autoConstraintTest(cr);
                    } catch (UnSupportedArgumentException e) {
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                progressDisplayGUI.setTestProgressOver(0);
                                addOutputMessage("���� " + funcName + "���в����������ɲ�֧�ֵ���������");
                            }
                        });
                    } catch (NoExitMethodException e) {
                        final String msg = e.getMessage();
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                addOutputMessage("���� " + funcName + "������ѭ����û�г���" + msg);
                                progressDisplayGUI.setTestProgressOver(0);
                            }
                        });
                    } catch (InterruptedException e) {
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                    } catch (Exception e) {
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                    }
                }
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        doProjectViewRefresh();
                        progressDisplayGUI.setTestProgressOver(1);
                        addOutputMessage("�����ļ��������");
                        demo.setStatusBarInfo("�����ļ��������");
                        MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "���Խ���", " ���к����Զ����Խ���");
                        box.open();
                    }
                });
                progressDisplayGUI.terminateListener.setRunningThread(null);
            }
        }.start();
    }

    /**
     * �Ե����ļ��������еĲ���
     * 
     * @throws IOException
     */
    public void doAutoTestForFile(final String selected) {


        System.out.println("�Ե����ļ����в���" + selected);
        demo.getOutputTabFolder().setSelection(0);
        final AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFile == null)
            return;
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                AnalysisFile testFile = demo.getCurrentFile();
                final CoverRule cr = demo.getCurrentCoverCriteria();
                final String fileName = testFile.getFile();

                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("���� ���ļ� " + fileName + "�����Զ�����...");
                        addOutputMessage("���� ���ļ� " + fileName + "�����Զ�����...");
                        demo.setStatusBarInfo("���ڶ��ļ� " + fileName + "�����Զ�����...");
                    }
                });

                ArrayList<TestModule> functionList = testFile.getFunctionList();
                funcNameList = new ArrayList<String>();
                for (TestModule testModule : functionList) {
                    String funcName = testModule.getFuncName();
                    funcNameList.add(funcName);
                }
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        doProjectViewRefresh();
                    }
                });

                final AnalysisFile currentFile = testFile;
                if (!currentFile.isHasAnalysised()) {
                    doModuleSeparateForFile(currentFile);
                }

                for (TestModule tm : functionList) {
                    final String funcName = tm.getFuncName();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            demo.setStatusBarInfo("���ڶԺ��� " + funcName + " ���� " + cr + " ����");
                            addOutputMessage("���ڶԺ��� " + funcName + " ���� " + cr + " ����");
                        }
                    });

                    demo.setCurrentFunc(tm);
                    doAutoTestCaseGenerateForAll();
                }

                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        doProjectViewRefresh();
                        progressDisplayGUI.setTestProgressOver(1);
                        addOutputMessage("�����ļ��������");
                        demo.setStatusBarInfo("�����ļ��������");
                        MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "���Խ���", " ���к����Զ����Խ���");
                        box.open();
                    }
                });
                progressDisplayGUI.terminateListener.setRunningThread(null);

                File out2 = new File("./�Զ������Խ��/�쳣.txt");
                if (!out2.exists())
                    try {
                        out2.createNewFile();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                FileOutputStream fos2 = null;
                OutputStreamWriter osw2 = null;
                BufferedWriter bos2 = null;
                try {
                    fos2 = new FileOutputStream(out2, true);
                    osw2 = new OutputStreamWriter(fos2, "utf-8");
                    bos2 = new BufferedWriter(osw2);
                    if (funcNameList.size() != 0) {
                        bos2.append("\r\n" + "�ļ�����" + demo.getCurrentFile().getFile() + "\r\n");
                    }

                    for (String testModuleName : funcNameList) {
                        try {
                            if (demo.getCurrentCoverCriteria().BlockCover) {
                                bos2.append("������:" + testModuleName + "\t" + "��串��" + "\n");
                            }
                            if (demo.getCurrentCoverCriteria().BranchCover) {
                                bos2.append("������:" + testModuleName + "\t��֧����" + "\n");
                            }
                            if (demo.getCurrentCoverCriteria().MCDCCover) {
                                bos2.append("������:" + testModuleName + "\tMCDC����" + "\n");
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {

                } finally {
                    if (bos2 != null) {
                        try {
                            bos2.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (osw2 != null) {
                        try {
                            osw2.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (fos2 != null) {
                        try {
                            fos2.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.start();
    }

    /**
     * ���̼����ļ�����
     * 
     * @param selected
     *        created by Yaoweichang on 2015-04-13 ����3:45:43
     */
    public void doAutoTestForProject(final String selected) {
        demo.getOutputTabFolder().setSelection(0);
        final Project currentProject = demo.getCurrentProject();
        if (currentProject == null)
            return;
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                ArrayList<AnalysisFile> fileList;
                ArrayList<AnalysisFile> fileset = new ArrayList<AnalysisFile>();
                if (currentProject.getFolderOperation()) {
                    for (AnalysisFile file : currentProject.getFolderFileList()) {
                        if (file.getFile().startsWith(selected)) {
                            fileset.add(file);
                        }
                    }
                    fileList = fileset;
                } else
                    fileList = currentProject.getFileList();
                final CoverRule cr = demo.getCurrentCoverCriteria();
                for (AnalysisFile file : fileList) {
                    final String fileName = file.getFile();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            progressDisplayGUI.setTestProgressRunning(3);
                            progressDisplayGUI.setInfo("���� ���ļ� " + fileName + "�����Զ�����...");
                            addOutputMessage("���� ���ļ� " + fileName + "�����Զ�����...");
                            demo.setStatusBarInfo("���ڶ��ļ� " + fileName + "�����Զ�����...");
                        }
                    });
                    final AnalysisFile currentFile = file;
                    if (!currentFile.isHasAnalysised()) {
                        doModuleSeparateForFile(currentFile);
                    }
                    ArrayList<TestModule> functionList = file.getFunctionList();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                        }
                    });

                    for (TestModule tm : functionList) {
                        final String funcName = tm.getFuncName();
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                demo.setStatusBarInfo("���ڶԺ��� " + funcName + " ���� " + cr + " ����");
                                addOutputMessage("���ڶԺ��� " + funcName + " ���� " + cr + " ����");
                            }
                        });

                        demo.setCurrentFunc(tm);
                        doAutoTestCaseGenerateForAll();
                    }
                }
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        doProjectViewRefresh();
                        progressDisplayGUI.setTestProgressOver(1);
                        addOutputMessage("�����ļ��������");
                        demo.setStatusBarInfo("�����ļ��������");
                        MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "���Խ���", " ���к����Զ����Խ���");
                        box.open();
                    }
                });
                progressDisplayGUI.terminateListener.setRunningThread(null);

            }
        }.start();
    }

    /**
     * �Զ����ɲ����������û����ù������ɲ��� �����Ĳ��ԣ�ֻҪΪ�û����ɸ������ܴﵽ100%��Ҫ��
     * modified by xujiaoxian
     */
    public void doAutoTestCaseGenerateForAll() {
        // ��ʾ�����ʰ��
        final TestModule currentFunc = demo.getCurrentFunc();
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        if (currentFunc == null) {
            return;
        }
        final String funcName = currentFunc.getFuncName();
        // ��ֹ��CurrentFunc���ı�,��һ����ʱ�ı������浱ǰѡ��Ҫ���Եĺ���
        final TestModule testFunc = currentFunc;
        final CoverRule cr = demo.getCurrentCoverCriteria();
        // �޸Ľ����������߳�ȥִ��
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {

                progressDisplayGUI.setTestProgressRunning(1);
                progressDisplayGUI.setInfo("�������ɲ������������Ժ�...");
                clearOutputMessage();
                demo.setStatusBarInfo("���ڶԺ��� " + funcName + "�����Զ�����...");
                addOutputMessage("���ڶԺ��� " + funcName + "�����Զ�����...");
            }
        });

        Callable<String> call = new Callable<String>() {
            public String call() throws Exception {
                try {
                    logger.error(demo.getCurrentFile() + "\t" + demo.getCurrentFunc() + "\n*********\n");
                    testFunc.autoTestCaseGenerate(cr); // yumeng,testFunc�����߳̽����Ĺؼ�����һ������ʱ�����
                } catch (UnSupportedArgumentException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    final String msg = e.getMessage();
                    Display.getDefault().asyncExec(new Runnable() {

                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "�����������ɲ�֧�ֵ���������", msg);
                            box.open();
                            addOutputMessage("���� " + funcName + "���в����������ɲ�֧�ֵ��������� " + msg);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                        }
                    });
                } catch (InterruptedException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "����ʱ�쳣", "�Զ�����ʱ�����쳣\n" + msg);
                            box.open();
                            addOutputMessage("�Ժ���" + funcName + "����ʱ�����쳣 " + msg);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "����ʱ�쳣", "���� " + funcName + "������ѭ����û�г���\n" + msg);
                            box.open();
                            addOutputMessage("���� " + funcName + "������ѭ����û�г���" + msg);
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
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "����ʱ�쳣", "�Զ�����ʱ�����쳣\n" + msg);
                            box.open();
                            addOutputMessage("�Զ�����ʱ�����쳣 " + msg);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                } finally {
                    progressDisplayGUI.terminateListener.setRunningThread(null);
                }
                return "�Զ���������ִ�е�ʱ����ƻ������";
            }
        };

        FutureTask<String> f = new FutureTask<String>(call);
        Thread t = new Thread(f);
        progressDisplayGUI.terminateListener.setRunningThread(t);
        t.start();
        try {
            f.get(Long.parseLong(Config.TestCaseGenTime), TimeUnit.MINUTES);// �޶��Զ����������������ִ�е�ʱ��
        } catch (InterruptedException e1) {
        } catch (ExecutionException e1) {
        } catch (TimeoutException e1) {
            funcNameList.remove(demo.getCurrentFunc().getFuncName());

            File dirFile = new File("./�Զ������Խ��");
            if (!dirFile.exists() && !dirFile.isDirectory()) {
                dirFile.mkdir();
            }
            File out = new File("./�Զ������Խ��/��ʱ.txt");
            if (!out.exists())
                try {
                    out.createNewFile();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }

            float lastBlockCoverage = testFunc.getCoverSetList().get(0).getCoverage();// ��һ�ε���串����
            float lastBranchCoverage = testFunc.getCoverSetList().get(1).getCoverage();// ��һ�εķ�֧������
            float lastMcdcCoverage = testFunc.getCoverSetList().get(2).getCoverage();
            NumberFormat numFormater = NumberFormat.getNumberInstance();
            numFormater.setMaximumFractionDigits(2);
            FileOutputStream fos = null;
            OutputStreamWriter osw = null;
            BufferedWriter bos = null;
            String msg1 = "";
            String msg2 = "";
            String msg3 = "";
            try {
                fos = new FileOutputStream(out, true);
                osw = new OutputStreamWriter(fos, "utf-8");
                bos = new BufferedWriter(osw);
                bos.append("\r\n�ļ���:" + demo.getCurrentFile().getFile() + "\t������:" + demo.getCurrentFunc().getFuncName() + "\t");
                if (demo.getCurrentCoverCriteria().BlockCover) {
                    msg1 =
                            msg1 + "����Ԫ������ " + (int) testFunc.getCoverSetList().get(0).calculateCoverednumber() + "\t��Ԫ����: " + testFunc.getCoverSetList().get(0).getTotalElementNumber()
                                    + "\t��串�Ǹ�����: " + numFormater.format(lastBlockCoverage * 100) + "%" + "\t" + demo.getCurrentFunc().getAllNewTestData().getTestCaseSize() + "\r\n";
                    bos.append(msg1);
                }
                if (demo.getCurrentCoverCriteria().BranchCover) {
                    msg2 =
                            msg2 + "����Ԫ������ " + (int) testFunc.getCoverSetList().get(1).calculateCoverednumber() + "\t��Ԫ������ " + testFunc.getCoverSetList().get(1).getTotalElementNumber()
                                    + "\t��֧���Ǹ�����: " + numFormater.format(lastBranchCoverage * 100) + "%" + "\t" + demo.getCurrentFunc().getAllNewTestData().getTestCaseSize() + "\r\n";
                    bos.append(msg2);
                }
                if (demo.getCurrentCoverCriteria().MCDCCover) {
                    msg3 =
                            msg3 + "����Ԫ������ " + (int) testFunc.getCoverSetList().get(2).calculateCoverednumber() + "\t��Ԫ������ " + testFunc.getCoverSetList().get(2).getTotalElementNumber()
                                    + "\tMCDC���Ǹ�����: " + numFormater.format(lastMcdcCoverage * 100) + "%" + "\t" + demo.getCurrentFunc().getAllNewTestData().getTestCaseSize() + "\r\n";
                    bos.append(msg3);
                }

            } catch (FileNotFoundException e11) {
                e11.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bos.flush();
                    osw.flush();
                    fos.flush();
                    bos.close();
                    osw.close();
                    fos.close();
                } catch (IOException e11) {
                    e11.printStackTrace();
                }
            }
            t.stop();
        }
    }

    /**
     * �Զ����ɲ����������û����ù������ɲ��� �����Ĳ��ԣ�ֻҪΪ�û����ɸ������ܴﵽ100%��Ҫ��
     * modified by xujiaoxian
     */
    public void doAutoTestCaseGenerate() {
        // ��ʾ�����ʰ��
        demo.getOutputTabFolder().setSelection(3);
        final TestModule currentFunc = demo.getCurrentFunc();
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        if (currentFunc == null) {
            return;
        }
        // �߼��̣߳���ֹ���������
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                final String funcName = currentFunc.getFuncName();
                // ��ֹ��CurrentFunc���ı�,��һ����ʱ�ı������浱ǰѡ��Ҫ���Եĺ���
                final TestModule testFunc = currentFunc;
                final CoverRule cr = demo.getCurrentCoverCriteria();
                // �޸Ľ����������߳�ȥִ��
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {

                        progressDisplayGUI.setTestProgressRunning(1);
                        progressDisplayGUI.setInfo("�������ɲ������������Ժ�...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("���ڶԺ��� " + funcName + "�����Զ�����...");
                        addOutputMessage("���ڶԺ��� " + funcName + "�����Զ�����...");
                    }
                });

                Callable<String> call = new Callable<String>() {
                    public String call() throws Exception {
                        try {
                            testFunc.autoTestCaseGenerate(cr); // yumeng,testFunc�����߳̽����Ĺؼ�����һ������ʱ�����

                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    byte crValue = 0;
                                    byte blockCoverTrue = 1;
                                    byte branchCoverTrue = 2;
                                    byte mcdcCoverTrue = 4;
                                    if (demo.getCurrentCoverCriteria().BlockCover)
                                        crValue ^= blockCoverTrue;
                                    if (demo.getCurrentCoverCriteria().BranchCover)
                                        crValue ^= branchCoverTrue;
                                    if (demo.getCurrentCoverCriteria().MCDCCover)
                                        crValue ^= mcdcCoverTrue;
                                    float lastBlockCoverage = testFunc.getCoverSetList().get(0).getCoverage();// ��һ�ε���串����
                                    float lastBranchCoverage = testFunc.getCoverSetList().get(1).getCoverage();// ��һ�εķ�֧������
                                    float lastMcdcCoverage = testFunc.getCoverSetList().get(2).getCoverage();// ��һ�ε�MC/DC������
                                    // �������ж��Ƿ���Ҫ��ʾ�û������˹��������Ѵﵽ100%�ĸ�����Ҫ��
                                    boolean needManualInterval = false;// �Ƿ���Ҫ�˹�����
                                    switch (crValue) {
                                        case 0:// û��ѡ����串�ǡ���֧���ǡ�MC/DC����׼���е��κ�һ��
                                            break;
                                        case 1:// ֻѡ����串��
                                            if (Math.abs(lastBlockCoverage - 1.0) > 0.01) {
                                                needManualInterval = true;
                                            } else {
                                                needManualInterval = false;
                                            }
                                            break;
                                        case 2:// ֻѡ���˷�֧����
                                            if (Math.abs(lastBranchCoverage - 1.0) > 0.01) {
                                                needManualInterval = true;
                                            } else {
                                                needManualInterval = false;
                                            }
                                            break;
                                        case 3:// ͬʱѡ������串�Ǻͷ�֧����
                                            if (Math.abs(lastBlockCoverage - 1.0) > 0.01 || Math.abs(lastBranchCoverage - 1.0) > 0.01) {
                                                needManualInterval = true;
                                            } else {
                                                needManualInterval = false;
                                            }
                                            break;
                                        case 4:// ֻѡ����MC/DC����
                                            if (Math.abs(lastMcdcCoverage - 1.0) > 0.01) {
                                                needManualInterval = true;
                                            } else {
                                                needManualInterval = false;
                                            }
                                            break;
                                        case 5:// ͬʱѡ����MC/DC���Ǻ���串��
                                            if (Math.abs(lastBlockCoverage - 1.0) > 0.01 || Math.abs(lastMcdcCoverage - 1.0) > 0.01) {
                                                needManualInterval = true;
                                            } else {
                                                needManualInterval = false;
                                            }
                                            break;
                                        case 6:// ͬʱѡ����MC/DC���Ǻͷ�֧����
                                            if (Math.abs(lastBranchCoverage - 1.0) > 0.01 || Math.abs(lastMcdcCoverage - 1.0) > 0.01) {
                                                needManualInterval = true;
                                            } else {
                                                needManualInterval = false;
                                            }
                                            break;
                                        case 7:// ͬʱѡ����MC/DC���ǡ���֧���Ǻ���串��
                                            if (Math.abs(lastBlockCoverage - 1.0) > 0.01 || Math.abs(lastBranchCoverage - 1.0) > 0.01 || Math.abs(lastMcdcCoverage - 1.0) > 0.01) {
                                                needManualInterval = true;
                                            } else {
                                                needManualInterval = false;
                                            }
                                            break;
                                        default:// ���������
                                            break;
                                    }
                                    if (needManualInterval) {
                                        NumberFormat numFormater = NumberFormat.getNumberInstance();
                                        numFormater.setMaximumFractionDigits(2);
                                        String coverage = "";
                                        if (demo.getCurrentCoverCriteria().BlockCover) {
                                            coverage += "��串�ǣ�" + numFormater.format(lastBlockCoverage * 100) + "%, ";
                                        }
                                        if (demo.getCurrentCoverCriteria().BranchCover)
                                            coverage += "��֧���ǣ�" + numFormater.format(lastBranchCoverage * 100) + "%, ";
                                        if (demo.getCurrentCoverCriteria().MCDCCover)
                                            coverage += "MC/DC���ǣ�" + numFormater.format(lastMcdcCoverage * 100) + "%, ";
                                        MessageBox box =
                                                WidgetFactory.createMessageBox(demo.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO, GUILanguageResource.getProperty("NeedManualInterval"), coverage
                                                        + GUILanguageResource.getProperty("NeedManualIntervalInfo") + "?");
                                        int flag = box.open();
                                        if (flag == SWT.YES) {
                                            doManualIntervention();
                                        }
                                    }
                                    doProjectViewRefresh();

                                    progressDisplayGUI.setTestProgressOver(1);
                                    demo.setStatusBarInfo("�Ժ��� " + funcName + "�����Զ����Խ���");
                                    addOutputMessage("�Ժ��� " + funcName + "�����Զ����Խ���");
                                    String info = "�Զ��������";
                                    if (currentFunc.getFuncVar().hasFileVar())
                                        info += "\n������ȫ�ֱ��������ļ�ָ�룬���û�����У����Խ��";
                                    MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "��ʾ��Ϣ", info);
                                    box.open();

                                }

                            });
                        } catch (UnSupportedArgumentException e) {
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                            final String msg = e.getMessage();
                            Display.getDefault().asyncExec(new Runnable() {

                                public void run() {
                                    MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "�����������ɲ�֧�ֵ���������", msg);
                                    box.open();
                                    addOutputMessage("���� " + funcName + "���в����������ɲ�֧�ֵ��������� " + msg);
                                    progressDisplayGUI.setTestProgressOver(0);
                                }
                            });
                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    doProjectViewRefresh();
                                }
                            });
                        } catch (InterruptedException e) {
                            final String msg = e.getMessage();
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "����ʱ�쳣", "�Զ�����ʱ�����쳣\n" + msg);
                                    box.open();
                                    addOutputMessage("�Ժ���" + funcName + "����ʱ�����쳣 " + msg);
                                    progressDisplayGUI.setTestProgressOver(0);
                                }
                            });
                        } catch (NoExitMethodException e) {
                            final String msg = e.getMessage();
                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "��ʾ", "���� " + funcName + "������ѭ����û�г��ڡ����޸Ļ������˲��Ժ�����\n" + msg);
                                    box.open();
                                    addOutputMessage("���� " + funcName + "������ѭ����û�г��ڡ����޸Ļ������˲��Ժ���" + msg);
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
                                    MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "����ʱ�쳣", "�Զ�����ʱ�����쳣\n" + msg);
                                    box.open();
                                    addOutputMessage("�Զ�����ʱ�����쳣 " + msg);
                                    progressDisplayGUI.setTestProgressOver(0);
                                }
                            });
                        } finally {
                            progressDisplayGUI.terminateListener.setRunningThread(null);
                        }
                        return "�Զ���������ִ�е�ʱ����ƻ������";
                    }
                };

                FutureTask<String> f = new FutureTask<String>(call);
                Thread t = new Thread(f);
                progressDisplayGUI.terminateListener.setRunningThread(t);
                t.start();
                try {
                    f.get(Long.parseLong(Config.TestCaseGenTime), TimeUnit.MINUTES);// �޶��Զ����������������ִ�е�ʱ��
                } catch (InterruptedException e1) {
                    // e1.printStackTrace();
                } catch (ExecutionException e1) {
                    // e1.printStackTrace();
                } catch (TimeoutException e1) {
                    t.stop();
                    // �޸Ľ����������߳�ȥִ��
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "ʱ�䳬ʱ", "��������ʱ�䳬��" + Config.TestCaseGenTime + "���ӣ���ǰ��ֹ��������С�");
                            box.open();
                            progressDisplayGUI.setTestProgressOver(1);
                        }
                    });
                }
            }
        }.start();

    }

    /**
     * ��������������������
     */
    public void doRandomTestBaseInputDomain() {
        // ��ʾ�����ʰ��
        demo.getOutputTabFolder().setSelection(3);
        final TestModule currentFunc = demo.getCurrentFunc();
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        if (currentFunc == null) {
            return;
        }
        // �߼��̣߳���ֹ���������
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                final String funcName = currentFunc.getFuncName();
                // ��ֹ��CurrentFunc���ı�,��һ����ʱ�ı������浱ǰѡ��Ҫ���Եĺ���
                TestModule testFunc = currentFunc;
                CoverRule cr = demo.getCurrentCoverCriteria();
                // �޸Ľ����������߳�ȥִ��
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {

                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("�������ɲ������������Ժ�...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("���ڶԺ��� " + funcName + "���л�����������������...");
                        addOutputMessage("���ڶԺ��� " + funcName + "���л�����������������...");
                    }
                });
                try {
                    testFunc.autoInputDomainRandomTest(cr);

                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            demo.setStatusBarInfo("�Ժ��� " + funcName + "���л����������������Խ���");
                            addOutputMessage("�Ժ��� " + funcName + "���л����������������Խ���");
                            String info = "���������������������";
                            if (currentFunc.getFuncVar().hasFileVar())
                                info += "\n������ȫ�ֱ��������ļ�ָ�룬���û�����У����Խ��";
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "��ʾ��Ϣ", info);
                            box.open();
                        }
                    });
                } catch (UnSupportedArgumentException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    final String msg = e.getMessage();
                    Display.getDefault().asyncExec(new Runnable() {

                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "�����������ɲ�֧�ֵ���������", msg);
                            box.open();
                            addOutputMessage("���� " + funcName + "���в����������ɲ�֧�ֵ��������� " + msg);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                        }
                    });
                } catch (InterruptedException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "����ʱ�쳣", "���� " + funcName + "������ѭ����û�г���\n" + msg);
                            box.open();
                            addOutputMessage("���� " + funcName + "������ѭ����û�г���" + msg);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                } catch (Exception e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                } finally {
                    progressDisplayGUI.terminateListener.setRunningThread(null);
                }
            }
        }.start();

    }

    /**
     * ����������·���ͱ߽�ֵ���������
     */
    public void doRandomTestBaseInputDomainAndPathAndBoundary() {
        demo.getOutputTabFolder().setSelection(3);
        final TestModule currentFunc = demo.getCurrentFunc();
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        if (currentFunc == null) {
            return;
        }
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                final String funcName = currentFunc.getFuncName();
                TestModule testFunc = currentFunc;
                CoverRule cr = demo.getCurrentCoverCriteria();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("�������ɲ������������Ժ�...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("���ڶԺ��� " + funcName + "���л��������򡢻���·��������Ժͻ��ڱ߽�ֵ�Ĳ���...");
                        addOutputMessage("���ڶԺ��� " + funcName + "���л��������򡢻���·��������Ժͻ��ڱ߽�ֵ�Ĳ���...");
                    }
                });
                try {
                    testFunc.autoInputDomainRandomAndPathRamdomTest(cr);
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            doCoverageInfoRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            demo.setStatusBarInfo("�Ժ��� " + funcName + "���л��������򡢻���·��������Ժͻ��ڱ߽�ֵ�Ĳ��Խ���");
                            addOutputMessage("�Ժ��� " + funcName + "���л��������򡢻���·��������Ժͻ��ڱ߽�ֵ�Ĳ��Խ���");
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "��ʾ��Ϣ", "���������򡢻���·��������Ժͻ��ڱ߽�ֵ�Ĳ������");
                            box.open();
                        }
                    });
                } catch (UnSupportedArgumentException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    final String msg = e.getMessage();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "�����������ɲ�֧�ֵ���������", msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("���� " + funcName + "���в����������ɲ�֧�ֵ��������� " + msg);
                        }
                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "����ʱ�쳣", "���� " + funcName + "������ѭ����û�г���" + msg);
                            box.open();
                            addOutputMessage("���� " + funcName + "������ѭ����û�г���" + msg);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                } catch (InterruptedException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                } catch (Exception e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                } finally {
                    progressDisplayGUI.terminateListener.setRunningThread(null);
                }
            }
        }.start();
    }

    /**
     * �����������·�����������
     */
    public void doRandomTestBaseInputDomainAndPath() {
        demo.getOutputTabFolder().setSelection(3);
        final TestModule currentFunc = demo.getCurrentFunc();
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        if (currentFunc == null) {
            return;
        }
        new Thread() {
            public void run() {
                // testTerminateButton.setEnabled(true);
                progressDisplayGUI.terminateListener.setRunningThread(this);
                final String funcName = currentFunc.getFuncName();
                TestModule testFunc = currentFunc;
                CoverRule cr = demo.getCurrentCoverCriteria();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {

                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("�������ɲ������������Ժ�...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("���ڶԺ��� " + funcName + "���л����������������Ժͻ���·�����������...");
                        addOutputMessage("���ڶԺ��� " + funcName + "���л����������������Ժͻ���·�����������...");
                    }
                });
                try {
                    testFunc.autoInputDomainRandomAndPathRamdomTest(cr);
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            doCoverageInfoRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            demo.setStatusBarInfo("�Ժ��� " + funcName + "���л����������������Ժͻ���·����������Խ���");
                            addOutputMessage("�Ժ��� " + funcName + "���л����������������Ժͻ���·����������Խ���");
                            String info = "�����������������Ժͻ���·��������������";
                            if (currentFunc.getFuncVar().hasFileVar())
                                info += "\n������ȫ�ֱ��������ļ�ָ�룬���û�����У����Խ��";
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "��ʾ��Ϣ", info);
                            box.open();
                        }
                    });
                } catch (UnSupportedArgumentException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    final String msg = e.getMessage();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "�����������ɲ�֧�ֵ���������", msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("���� " + funcName + "���в����������ɲ�֧�ֵ��������� " + msg);
                        }
                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "����ʱ�쳣", "���� " + funcName + "������ѭ����û�г���" + msg);
                            box.open();
                            addOutputMessage("���� " + funcName + "������ѭ����û�г���" + msg);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                } catch (InterruptedException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                } catch (Exception e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                } finally {
                    progressDisplayGUI.terminateListener.setRunningThread(null);
                }
            }
        }.start();

    }


    /**
     * ����������ͻ���·��������ԡ�·��Լ��������ɲ��������Ĳ���
     */
    public void doTestBaseRandomAndPath() {
        demo.getOutputTabFolder().setSelection(3);
        final TestModule currentFunc = demo.getCurrentFunc();
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        if (currentFunc == null) {
            return;
        }
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);

                final String funcName = currentFunc.getFuncName();
                TestModule testFunc = currentFunc;
                // ��ֹ��˲��ı�
                CoverRule cr = demo.getCurrentCoverCriteria();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("�������ɲ������������Ժ�...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("���ڶԺ��� " + funcName + "���л����������������Ժͻ���·����������Ժͻ���·����Լ��������...");
                        addOutputMessage("���ڶԺ��� " + funcName + "���л����������������Ժͻ���·����������Ժͻ���·����Լ��������...");
                    }
                });
                try {
                    testFunc.autoRandomAndPathTest(cr);

                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            demo.setStatusBarInfo("�Ժ��� " + funcName + "���л����������������Ժͻ���·����������Ժͻ���·����Լ�������Խ���");
                            addOutputMessage("�Ժ��� " + funcName + "���л����������������Ժͻ���·����������Ժͻ���·����Լ�������Խ���");
                            String info = "�����������������Ժͻ���·����������Ժͻ���·����Լ��������";
                            if (currentFunc.getFuncVar().hasFileVar())
                                info += "\n������ȫ�ֱ��������ļ�ָ�룬���û�����У����Խ��";
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "��ʾ��Ϣ", info);
                            box.open();
                        }

                    });
                } catch (UnSupportedArgumentException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    final String msg = e.getMessage();
                    Display.getDefault().asyncExec(new Runnable() {

                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "�����������ɲ�֧�ֵ���������", msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("���� " + funcName + "���в����������ɲ�֧�ֵ��������� " + msg);
                        }
                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "����ʱ�쳣", "���� " + funcName + "������ѭ����û�г���" + msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("���� " + funcName + "������ѭ����û�г���" + msg);
                        }
                    });
                } catch (InterruptedException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                } catch (Exception e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                } finally {
                    progressDisplayGUI.terminateListener.setRunningThread(null);
                }
            }
        }.start();


    }

    /**
     * ����������ͻ���·��������ԡ�·��Լ��������ɲ��������Ĳ���
     */
    public void doPathBasedRandomTest() {
        demo.getOutputTabFolder().setSelection(3);
        final TestModule currentFunc = demo.getCurrentFunc();
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        if (currentFunc == null) {
            return;
        }
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);

                final String funcName = currentFunc.getFuncName();
                TestModule testFunc = currentFunc;
                // ��ֹ��˲��ı�
                CoverRule cr = demo.getCurrentCoverCriteria();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("�������ɲ������������Ժ�...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("���ڶԺ��� " + funcName + "���л���·�����������...");
                        addOutputMessage("���ڶԺ��� " + funcName + "���л��ڽ��л���·�����������...");
                    }
                });
                try {
                    testFunc.autoPathBasedRandomTest(cr);

                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            demo.setStatusBarInfo("�Ժ��� " + funcName + "���л���·����������Խ���");
                            addOutputMessage("�Ժ��� " + funcName + "���л���·����������Խ���");
                            String info = "����·��������������";
                            if (currentFunc.getFuncVar().hasFileVar())
                                info += "\n������ȫ�ֱ��������ļ�ָ�룬���û�����У����Խ��";
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "��ʾ��Ϣ", info);
                            box.open();
                        }
                    });
                } catch (UnSupportedArgumentException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    final String msg = e.getMessage();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "�����������ɲ�֧�ֵ���������", msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("���� " + funcName + "���в����������ɲ�֧�ֵ��������� " + msg);
                        }
                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "����ʱ�쳣", "���� " + funcName + "������ѭ����û�г���\n" + msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("���� " + funcName + "������ѭ����û�г���" + msg);
                        }
                    });
                } catch (InterruptedException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                } catch (Exception e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                } finally {
                    progressDisplayGUI.terminateListener.setRunningThread(null);
                }
            }
        }.start();
    }

    public void doPathBasedBoundaryTest() {
        demo.getOutputTabFolder().setSelection(3);
        final TestModule currentFunc = demo.getCurrentFunc();
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        if (currentFunc == null) {
            return;
        }
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);

                final String funcName = currentFunc.getFuncName();
                TestModule testFunc = currentFunc;
                // ��ֹ��˲��ı�
                CoverRule cr = demo.getCurrentCoverCriteria();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("�������ɲ������������Ժ�...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("���ڶԺ��� " + funcName + "���б߽�ֵ����...");
                        addOutputMessage("���ڶԺ��� " + funcName + "���л��ڽ��б߽�ֵ����...");

                    }

                });
                try {
                    testFunc.autoBoundaryTest(cr);

                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            demo.setStatusBarInfo("�Ժ��� " + funcName + "���б߽�ֵ���Խ���");
                            addOutputMessage("�Ժ��� " + funcName + "���б߽�ֵ���Խ���");
                            String info = "���ڱ߽�ֵ�������";
                            if (currentFunc.getFuncVar().hasFileVar())
                                info += "\n������ȫ�ֱ��������ļ�ָ�룬���û�����У����Խ��";
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "��ʾ��Ϣ", info);
                            box.open();
                        }

                    });
                } catch (UnSupportedArgumentException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    final String msg = e.getMessage();
                    Display.getDefault().asyncExec(new Runnable() {

                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "�����������ɲ�֧�ֵ���������", msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("���� " + funcName + "���в����������ɲ�֧�ֵ��������� " + msg);
                        }
                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "����ʱ�쳣", "���� " + funcName + "������ѭ����û�г���\n" + msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("���� " + funcName + "������ѭ����û�г���" + msg);
                        }
                    });
                } catch (InterruptedException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                } catch (Exception e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    final String msg = e.getMessage();
                    Display.getDefault().asyncExec(new Runnable() {

                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "����ʱ�쳣", "�߽�ֵ�����쳣 \n" + msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("�߽�ֵ���Է����쳣 " + msg);
                        }
                    });
                } finally {
                    progressDisplayGUI.terminateListener.setRunningThread(null);
                }
            }
        }.start();
    }

    /**
     * ����Լ��������
     */
    public void doConstraintsTest() {
        demo.getOutputTabFolder().setSelection(3);
        final TestModule currentFunc = demo.getCurrentFunc();
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        if (currentFunc == null) {
            return;
        }
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);

                final String funcName = currentFunc.getFuncName();
                TestModule testFunc = currentFunc;
                // ��ֹ��˲��ı�
                CoverRule cr = demo.getCurrentCoverCriteria();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("�������ɲ������������Ժ�...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("���ڶԺ��� " + funcName + "���л���Լ��������...");
                        addOutputMessage("���ڶԺ��� " + funcName + "���л���Լ��������...");
                    }
                });
                try {
                    testFunc.autoConstraintTest(cr);
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            demo.setStatusBarInfo("�Ժ��� " + funcName + "���л���Լ�������Խ���");
                            addOutputMessage("�Ժ��� " + funcName + "���л���Լ�������Խ���");
                            String info = "����Լ�����������";
                            if (currentFunc.getFuncVar().hasFileVar())
                                info += "\n������ȫ�ֱ��������ļ�ָ�룬���û�����У����Խ��";
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "��ʾ��Ϣ", info);
                            box.open();
                        }
                    });
                } catch (UnSupportedArgumentException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    final String msg = e.getMessage();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "�����������ɲ�֧�ֵ���������", msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("���� " + funcName + "���в����������ɲ�֧�ֵ��������� " + msg);
                        }
                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), funcName + "������ѭ����û�г���", msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("���� " + funcName + "������ѭ����û�г���" + msg);
                        }
                    });
                } catch (InterruptedException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                } catch (Exception e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                } finally {
                    progressDisplayGUI.terminateListener.setRunningThread(null);
                }
            }
        }.start();
    }

    public void doAutoRandomTestBasedInputDomainForAllFunctionInFile() {
        doAutoTestForAllFunctionInFile(0);
    }

    public void doAutoRandomTestBasedInputDomainAndPathForAllFunctionInFile() {
        doAutoTestForAllFunctionInFile(1);
    }

    public void doAutoTestBasedRandomAndPathForAllFuncInFile() {
        doAutoTestForAllFunctionInFile(2);
    }

    public void doPathBasedRandomTestForAllFuncInFile() {
        doAutoTestForAllFunctionInFile(3);
    }

    public void doConstraintsTestForAllFuncInFile() {
        doAutoTestForAllFunctionInFile(4);
    }

    /**
     * ��һ���ļ��ڲ������еĺ��������Զ�����
     */
    public void doAutoTestForAllFunctionInFile(final int kind) {
        // ��ʾ�����Ϣ
        demo.getOutputTabFolder().setSelection(0);
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFile == null)
            return;

        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                try {
                    AnalysisFile testFile = demo.getCurrentFile();
                    final String[] testName = new String[5];
                    final String fileName = testFile.getFile();
                    CoverRule cr = demo.getCurrentCoverCriteria();

                    testName[0] = "������������������";
                    testName[1] = "����������ͻ���·�����������";
                    testName[2] = "����������ͻ���·����������Ժͻ���·����Լ��������";
                    testName[3] = "����·�����������";
                    testName[4] = "����·����Լ��������";

                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            progressDisplayGUI.setTestProgressRunning(3);
                            progressDisplayGUI.setInfo("�������ɲ������������Ժ�...");
                            demo.setStatusBarInfo("���ļ� " + fileName + "�ڵ����к�������" + testName[kind]);
                            addOutputMessage("���ļ� " + fileName + "�ڵ����к�������" + testName[kind]);

                        }
                    });
                    ArrayList<TestModule> functions = testFile.getFunctionList();
                    for (TestModule tm : functions) {
                        try {
                            if (kind == 0) {
                                tm.autoInputDomainRandomTest(cr);
                            } else if (kind == 1)
                                tm.autoInputDomainRandomAndPathRamdomTest(cr);
                            else if (kind == 2)
                                tm.autoRandomAndPathTest(cr);
                            else if (kind == 3)
                                tm.autoPathBasedRandomTest(cr);
                            else if (kind == 4)
                                tm.autoConstraintTest(cr);
                        } catch (UnSupportedArgumentException e) {
                            final String funcName = tm.getFuncName();
                            final String msg = e.getMessage();
                            RecordToLogger.recordExceptionInfo(e, logger);
                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    addOutputMessage("���� " + funcName + "���в����������ɲ�֧�ֵ���������" + msg);
                                }
                            });
                        } catch (NoExitMethodException e) {
                            final String funcName = tm.getFuncName();
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    addOutputMessage("���� " + funcName + "������ѭ����û�г���");
                                }
                            });
                        } catch (InterruptedException e) {
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                        } catch (Exception e) {
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                        }
                    }

                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(0);
                            demo.setStatusBarInfo("���ļ� " + fileName + " �ڵ����к����� " + testName[kind] + "���Խ���");
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "���Խ���", "���ļ� " + fileName + " �ڵ����к����� " + testName[kind] + "���Խ���");
                            box.open();

                        }
                    });
                } catch (Exception e) {

                } finally {
                    progressDisplayGUI.terminateListener.setRunningThread(null);
                }
            }
        }.start();
    }

    private void doModuleSeparateForFile(AnalysisFile currentFile) {
        if (currentFile == null)
            return;
        boolean error = false;

        currentFile.clear();
        currentFile.setError(false);
        if (!currentFile.isError()) {
            try {
                currentFile.doPretreatment();
            } catch (OutOfMemoryError e) {
                MessageBox mb = WidgetFactory.createInfoMessageBox(demo.getShell(), "�ڴ����", "�ڴ����");
                mb.open();
                error = true;
                currentFile.setError(error);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                RecordToLogger.recordExceptionInfo(e, logger);
                return;
            } catch (PretreamentException e) {
                error = true;
                currentFile.setError(error);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                RecordToLogger.recordExceptionInfo(e, logger);
                return;
            } catch (Exception e) {
                error = true;
                currentFile.setError(error);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                RecordToLogger.recordExceptionInfo(e, logger);
                return;
            }

            try {
                currentFile.doStaticAnalysis();
            } catch (OutOfMemoryError e) {
                MessageBox mb = WidgetFactory.createInfoMessageBox(demo.getShell(), "�ڴ����", "�ڴ����");
                mb.open();
                error = true;
                currentFile.setError(error);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                return;
            } catch (StaticAnalysisException e) {

                error = true;
                currentFile.setError(true);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                RecordToLogger.recordExceptionInfo(e, logger);
                if (Config.printExceptionInfoToConsole)
                    e.printStackTrace();
                return;
            } catch (Exception e) {
                error = true;
                currentFile.setError(error);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                RecordToLogger.recordExceptionInfo(e, logger);
                return;
            }

            if (softtest.pretreatment.Pretreatment.systemInc.size() == 0) {
                // by jinkaifeng
                String INCLUDE;
                if (Config.os.equals("windows")) {
                    INCLUDE = System.getenv("GCCINC");
                } else {
                    INCLUDE = Config.LinuxInclude;
                }
                // ���û������Inlude·��
                if (INCLUDE == null) {
                    logger.error("System environment variable \"GCCINC\" error!");
                } else {
                    String headers[] = INCLUDE.split(";");
                    for (int i = 0; i < headers.length; i++) {
                        softtest.pretreatment.Pretreatment.addInc(headers[i]);
                    }
                }
            }

            currentFile.doModuleSeparate();
            currentFile.setHasAnalysised(true);

            int loc = demo.getCurrentProject().getfilesLoc(currentFile.getFile());
            if (loc != -1)
                demo.getCurrentProject().getIsModuleSeparated().set(loc, true);
        }
    }

    /**
     * this function static analysis a cpp file
     * ,find all the funtion,and get the input and output for a function,
     * may the data property for each class, struct
     */
    public void doModuleSeparateForOneFile() {
        AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFile == null)
            return;
        boolean error = false;

        currentFile.clear();
        currentFile.setError(false);
        if (!currentFile.isError()) {
            try {
                currentFile.doPretreatment();
            } catch (OutOfMemoryError e) {
                MessageBox mb = WidgetFactory.createInfoMessageBox(demo.getShell(), "�ڴ����", "�ڴ����");
                mb.open();
                error = true;
                currentFile.setError(error);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                RecordToLogger.recordExceptionInfo(e, logger);
                return;
            } catch (PretreamentException e) {
                error = true;
                currentFile.setError(error);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                RecordToLogger.recordExceptionInfo(e, logger);
                return;
            } catch (Exception e) {
                error = true;
                currentFile.setError(error);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                RecordToLogger.recordExceptionInfo(e, logger);
                return;
            }

            try {
                currentFile.doStaticAnalysis();
            } catch (OutOfMemoryError e) {
                MessageBox mb = WidgetFactory.createInfoMessageBox(demo.getShell(), "�ڴ����", "�ڴ����");
                mb.open();
                error = true;
                currentFile.setError(error);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                return;
            } catch (StaticAnalysisException e) {
                error = true;
                currentFile.setError(true);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                RecordToLogger.recordExceptionInfo(e, logger);
                if (Config.printExceptionInfoToConsole)
                    e.printStackTrace();
                return;
            } catch (Exception e) {
                error = true;
                currentFile.setError(error);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                RecordToLogger.recordExceptionInfo(e, logger);
                return;
            }

            if (softtest.pretreatment.Pretreatment.systemInc.size() == 0) {
                // by jinkaifeng
                String INCLUDE;
                if (Config.os.equals("windows")) {
                    INCLUDE = System.getenv("GCCINC");
                } else {
                    INCLUDE = Config.LinuxInclude;
                }
                // ���û������Inlude·��
                if (INCLUDE == null) {
                    logger.error("System environment variable \"GCCINC\" error!");
                } else {
                    String headers[] = INCLUDE.split(";");
                    for (int i = 0; i < headers.length; i++) {
                        softtest.pretreatment.Pretreatment.addInc(headers[i]);
                    }
                }
            }

            try {
                // ��Ԫ����
                currentFile.doModuleSeparate();
                currentFile.doStubInformationGenerate();
                currentFile.doStubCodeGenerate();
                currentFile.setHasAnalysised(true);
                // add by xujiaoixan
                int loc = demo.getCurrentProject().getfilesLoc(currentFile.getFile());
                if (loc != -1)
                    demo.getCurrentProject().getIsModuleSeparated().set(loc, true);
                demo.doCurrentFuncRefresh(currentFile);
            } catch (StubGenerateException e) {
                final String msg = e.getMessage();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        addOutputMessage(msg);
                    }
                });
                error = true;
                currentFile.setError(error);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                RecordToLogger.recordExceptionInfo(e, logger);
                return;
            } catch (UnsupportedReturnTypeException e) {
                final String msg = e.getMessage();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        addOutputMessage(msg);
                    }
                });
                error = true;
                currentFile.setError(error);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                RecordToLogger.recordExceptionInfo(e, logger);
                return;
            } catch (Exception e) {
                final String msg = e.getMessage();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        addOutputMessage(msg);
                    }
                });
                RecordToLogger.recordExceptionInfo(e, logger);
                if (Config.printExceptionInfoToConsole)
                    e.printStackTrace();
                error = true;
                currentFile.setError(error);
                // add by xujiaoxian
                demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), error);
                return;
            }

        } else {// Ϊ�˽����������Ͳ鿴����������Ϊ���ļ�δģ�黯�ֲ��ܲ鿴������ add by xujiaoxian
            demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), true);
        }
    }

    /**
     * 2013/3/26 xujiaoxian��д�÷���.
     * This function deal ProjectView Refresh Event.
     * 
     * @see #doFileViewRefresh()
     * @see #doPointViewRefresh()
     */
    public void doProjectViewRefresh() {
        Project currentProject = demo.getCurrentProject();

        if (currentProject == null) {
            demo.getProjectViewTree().removeAll();
            return;
        }

        if (demo.getProjectViewTree().getItemCount() <= 0) {
            String sourceCodePathString = demo.getCurrentProject().getSourceCodePathString();
            File sourceCodePathFile = new File(sourceCodePathString);
            TreeItem root = new TreeItem(demo.getProjectViewTree(), 0);
            root.setText(demo.getCurrentProject().getName());
            root.setData(sourceCodePathFile);
            root.setImage(Resource.projectImage);
            new TreeItem(root, 0);
        } else {
            TreeItem[] items = demo.getProjectViewTree().getItems();
            demo.buildTree(items[0]);
        }
        demo.getProjectViewTree().addListener(SWT.Expand, new Listener() {
            public void handleEvent(Event event) {
                TreeItem root = (TreeItem) event.item;
                demo.buildTree(root);
            }
        });

        // ��һ�к���Ҫ����ϵ��������ʾ�ķ��,Ҫ����ԭ���ķ��Ļ�������Ҫ�ĺ���һ��
        // WidgetFactory.setTreeContents( root,demo.getProjectViewTree(), WidgetFactory.PROJECT );

        // demo.expandProjectViewTree();
        demo.doMeauToolBarRefresh();
        Config.isTestCaseGenerate = false;

        // demo.actionsGUI.doCoverageInfoRefresh();
    }

    /**
     * 2013/4/10 xujiaoxian
     * ���������ˢ�¹���ʱ�����õ��Ǹ÷�����
     * 
     * @see #doFileViewRefresh()
     * @see #doPointViewRefresh()
     */
    public void doProjectViewReloadRefresh() {
        Project currentProject = demo.getCurrentProject();
        if (currentProject == null) {
            demo.getProjectViewTree().removeAll();
            return;
        }
        demo.getProjectViewTree().removeAll();
        String sourceCodePathString = demo.getCurrentProject().getSourceCodePathString();
        File sourceCodePathFile = new File(sourceCodePathString);
        TreeItem root = new TreeItem(demo.getProjectViewTree(), 0);
        root.setText(demo.getCurrentProject().getName());
        root.setData(sourceCodePathFile);
        root.setImage(Resource.projectImage);
        new TreeItem(root, 0);
        demo.getProjectViewTree().addListener(SWT.Expand, new Listener() {
            public void handleEvent(Event event) {
                TreeItem root = (TreeItem) event.item;
                demo.buildTree(root);
            }
        });

        // ��һ�к���Ҫ����ϵ��������ʾ�ķ��,Ҫ����ԭ���ķ��Ļ�������Ҫ�ĺ���һ��
        // WidgetFactory.setTreeContents( root,demo.getProjectViewTree(), WidgetFactory.PROJECT );

        demo.expandProjectViewTree();
    }

    // ���¸�������Ϣ
    public void doCoverageInfoRefresh() {
        TestModule tm = demo.getCurrentFunc();
        if (null != tm /* && null != tm.getCoverSet() */) {
            demo.funcCLabel.setText(tm.getFuncName() + " " + GUILanguageResource.getProperty("Selected") + " ");


            demo.setFuncBlockCoverage(0, 0);
            demo.setFuncDecisionCoverage(0, 0);
            demo.setFuncMcDcCoverage(0, 0);

            List<CoverSet> allCoverSet = tm.getCoverSetList();
            if (allCoverSet == null)
                return;
            if (demo.getCurrentCoverCriteria().BlockCover && allCoverSet.size() >= 1) {
                CoverSet coverset = allCoverSet.get(0);
                if (coverset != null) {
                    int cover = 0;
                    int all = 1;
                    all = coverset.getTotalElementNumber();
                    cover = (int) coverset.calculateCoverednumber();
                    demo.setFuncBlockCoverage(cover, all);
                    if (Config.isDebugVersion) {
                        demo.setFuncBlockUnCoverage(coverset.getUncoveredRate4PathSelection(), coverset.getUncoveredRate4TestCase());
                    }
                }
            }
            if (demo.getCurrentCoverCriteria().BranchCover && allCoverSet.size() >= 2) {
                CoverSet coverset = allCoverSet.get(1);
                if (coverset != null) {
                    int cover = 0;
                    int all = 1;
                    all = coverset.getTotalElementNumber();
                    cover = (int) coverset.calculateCoverednumber();
                    demo.setFuncDecisionCoverage(cover, all);
                    if (Config.isDebugVersion) {
                        demo.setFuncDecisionUnCoverage(coverset.getUncoveredRate4PathSelection(), coverset.getUncoveredRate4TestCase());
                    }
                }

            }
            if (demo.getCurrentCoverCriteria().MCDCCover && allCoverSet.size() >= 3) {
                CoverSet coverset = allCoverSet.get(2);
                if (coverset != null) {
                    int cover = 0;
                    int all = 1;
                    all = coverset.getTotalElementNumber();
                    cover = (int) coverset.calculateCoverednumber();
                    demo.setFuncMcDcCoverage(cover, all);
                    if (Config.isDebugVersion) {
                        demo.setFuncMcDcUnCoverage(coverset.getUncoveredRate4PathSelection(), coverset.getUncoveredRate4TestCase());
                    }
                }

            }

        } else {
            int all = 0;
            int cover = 0;
            demo.funcCLabel.setText(GUILanguageResource.getProperty("NoFunctionSelected"));
            demo.setFuncBlockCoverage(cover, all);
            demo.setFuncDecisionCoverage(cover, all);
            demo.setFuncMcDcCoverage(cover, all);
        }
    }

    public void doBatchProcessing() {
        UATBatchProcess target = new UATBatchProcess(demo);
        target.go();
    }

    public void doBugLink() {
        UATBugLinkGUI target = new UATBugLinkGUI(demo);
        if (!target.bugLinkFail) {
            target.uatGui.getShell().setEnabled(false);
            target.go();
        }
    }

    public void doShowTestResult() {
        TestModule currentFunc = demo.getCurrentFunc();
        AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFunc == null || !currentFile.isHasBuildedTest())
            return;
        try {
            File file = new File(currentFunc.getTestResultFile());
            FileTabManager.ShowFile(file, demo, false);
        } catch (Exception e) {
            addOutputMessage("��ʾ���Խ���ļ�ʱ�����쳣  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    public void doShowTestResultWithIE() {
        TestModule currentFunc = demo.getCurrentFunc();
        AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFunc == null || !currentFile.isHasBuildedTest())
            return;
        try {
            Runtime run = Runtime.getRuntime();
            String filename;
            String cmd;
            if (Config.os.equals("windows")) {
                filename = currentFunc.getTestResultFile().replace("/", "\\");
                filename = filename.replace("\\\\\\", "\\");
                filename = filename.replace("\\\\", "\\");
                cmd = "cmd.exe /c start iexplore " + filename;
            } else {
                filename = currentFunc.getTestResultFile();
                cmd = "firefox file://" + filename;
            }
            run.exec(cmd);
        } catch (Exception e) {
            addOutputMessage("��ʾ���Խ���ļ�ʱ�����쳣  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    public void showTestResultReport() {
        Project currentProject = demo.getCurrentProject();
        if (currentProject == null)
            return;
        try {
            File file = new File(currentProject.getTestResultName());
            if (!file.exists())
                file.createNewFile();

            FileTabManager.ShowFile(file, demo, false);
        } catch (Exception e) {
            addOutputMessage("��ʾ���Խ�������ļ������쳣  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    public void doShowDriverFile() {
        TestModule currentFunc = demo.getCurrentFunc();
        AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFunc == null || !currentFile.isHasBuildedTest())
            return;
        try {
            File file = new File(currentFile.getTestDriverFile());
            FileTabManager.ShowFile(file, demo, false);
        } catch (Exception e) {
            addOutputMessage("��ʾ�����ļ�ʱ�����쳣  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    public void doShowRegressionTestFile() {
        TestModule currentFunc = demo.getCurrentFunc();
        AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFunc == null || !currentFile.isHasRegressionTest())
            return;
        try {
            File dir = new File(currentFile.getRegressionOutputDir());
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; ++i) {
                if (files[i].getName().endsWith(currentFunc.getUniqueFuncName() + "_RegressionTestReport.xml")) {
                    FileTabManager.ShowFile(files[i], demo, false);
                }
            }
        } catch (Exception e) {
            addOutputMessage("��ʾ�ع�����ļ�ʱ�����쳣  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    public void doShowTestStubFile() {
        TestModule currentFunc = demo.getCurrentFunc();
        AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFunc == null || !currentFile.isHasBuildedTest())
            return;
        try {
            File file = new File(currentFile.getTestStubFile());
            FileTabManager.ShowFile(file, demo, false);
        } catch (Exception e) {
            addOutputMessage("��ʾ׮�ļ�ʱ�����쳣  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    public void doShowDefineFile() {
        AnalysisFile currentFile = demo.getCurrentFile();
        try {
            Runtime run = Runtime.getRuntime();
            String cmd;
            if (Config.os.equals("windows")) {
                cmd = "cmd.exe /c start notepad " + currentFile.getDefineFile();
            } else {
                cmd = "xdg-open " + currentFile.getDefineFile();
            }
            try {
                run.exec(cmd);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            addOutputMessage("��ʾԤ�����ļ�ʱ�����쳣  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    public void doShowInstruFile() {
        TestModule currentFunc = demo.getCurrentFunc();
        AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFunc == null || !currentFile.isHasBuildedTest())
            return;
        try {
            File file = new File(currentFile.getInstruFile());
            FileTabManager.ShowFile(file, demo, false);
        } catch (Exception e) {
            addOutputMessage("��ʾ��װ��ʱ�����쳣  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    /**
     * �鿴����̨����ļ�
     */
    public void doShowConsoleOutputFile() {
        AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFile == null)
            return;
        try {
            File file = new File(currentFile.getConsoleOuputFile());
            if (file.exists())
                FileTabManager.ShowFile(file, demo, false);
            else
                return;
        } catch (Exception e) {
            addOutputMessage("��ʾ����̨����ļ�ʱ�����쳣  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    /**
     * �鿴ѡ�к�����Դ����
     */
    public void doShowsourceFile() {
        AnalysisFile currentFile = demo.getCurrentFile();
        try {
            File file = new File(currentFile.getFile());

            FileTabManager.ShowFile(file, demo, true);
        } catch (Exception e) {
            addOutputMessage("��ʾԴ����ʱ�����쳣  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    /**
     * �鿴ѡ�к����Ĳ�����������
     */
    public void doShowTestCaseFile() {
        TestModule currentFunc = demo.getCurrentFunc();
        AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFunc == null || !currentFile.isHasBuildedTest())
            return;
        try {
            File file = new File(currentFunc.getTestSuiteName());
            FileTabManager.ShowFile(file, demo, false);
        } catch (Exception e) {
            addOutputMessage("��ʾ��������ʱ�����쳣  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    public void doShowLogFile() {
        // �����־�ļ���ʾ��ϵͳ֧�ֹ��� add by Yaoweichang
        String os = "";
        if (Config.isLinux())
            os = "gedit ";
        else
            os = "notepad ";
        final String cmd = os + Config.LOG_FILE;

        new Thread() {
            public void run() {
                try {
                    File file = new File(Config.LOG_FILE);
                    if (!file.exists())
                        file.createNewFile();
                    CMDProcess.process(cmd, false);
                } catch (IOException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                } catch (InterruptedException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * �༭ѡ�еĺ����Ĳ�������
     */
    public void doEditTestCases() {
        TestModule currentFunc = demo.getCurrentFunc();
        AnalysisFile currentFile = demo.getCurrentFile();

        boolean error = false;
        if (!currentFile.isHasBuildedTest() || currentFunc == null)
            return;
        File f = new File(currentFunc.getTestSuiteName());
        if (!f.exists()) {
            error = true;
        }
        String cmd = "\"" + Config.VCEXEPath + "\" " + "\"" + currentFunc.getTestSuiteName() + "\"";
        try {
            CMDProcess.process(cmd, false);
        } catch (IOException e) {
            RecordToLogger.recordExceptionInfo(e, logger);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
            error = true;
        } catch (InterruptedException e) {
            RecordToLogger.recordExceptionInfo(e, logger);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
            error = true;
        } catch (Exception e) {
            RecordToLogger.recordExceptionInfo(e, logger);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
            error = true;
        }
        if (error) {
            MessageBox mb = WidgetFactory.createInfoMessageBox(demo.getShell(), "�ļ�������", currentFunc.getTestSuiteName() + "������");
            mb.open();
            return;
        }
    }

    /**
     * This function deal the show test case Lib Event.
     * 
     * @see #dealHelpMenuEvent()
     */

    public void doShowTestCaseLib() {
        /*
         * testCaseLibTabItem.setControl(testCaseLibComposite);
         * testCaseLibComposite.setVisible(true);
         * testCaseLibComposite2.setVisible(false);
         * try
         * {
         * List list = TestCaseLibManager.showAllTestCase();
         * MyLabelProvider.resetId();
         * tableViewer.setInput(list);
         * //��ʾ�����������������
         * outputTabFolder.setSelection(2);
         * System.out.println("yes");
         * }catch(Exception e)
         * {
         * RecordToLogger.recordExceptionInfo(e, logger);
         * if(Config.printExceptionInfoToConsole)
         * e.printStackTrace();
         * addOutputMessage("��ʾ����������ʱ�����쳣 " +e.getMessage());
         * }
         */
    }

    /**
     * This function deal the import test case Lib Event.
     * 
     * @see #dealHelpMenuEvent()
     */
    public void doImportTestCase() {
        /*
         * if(currentProject == null)
         * {
         * return ;
         * }
         * try
         * {
         * ArrayList<AnalysisFile> fileList = currentProject.getFileList();
         * for(AnalysisFile f:fileList)
         * {
         * ArrayList<TestModule> funcionList = f.getFunctionList();
         * for(TestModule tm : funcionList)
         * {
         * TestCaseLibManager.loadTestCase(tm);
         * }
         * }
         * }catch(Exception e)
         * {
         * RecordToLogger.recordExceptionInfo(e, logger);
         * if(Config.printExceptionInfoToConsole)
         * e.printStackTrace();
         * addOutputMessage("�������������ʱ�����쳣 " +e.getMessage());
         * }
         */
    }

    /**
     * This function deal the help Event.
     * 
     * @see #dealHelpMenuEvent()
     */
    public void doHelp() {
        try {
            String cmd = "hh.exe help/helpDoc.chm";
            CMDProcess.process(cmd, false);
        } catch (IOException e) {
            String errMsg = "û�а����ļ����ã�";
            MessageBox mb = WidgetFactory.createErrorMessageBox(demo.getShell(), "��ʾ��Ϣ", errMsg);
            mb.open();
            RecordToLogger.recordExceptionInfo(e, logger);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
        } catch (InterruptedException e) {
            String errMsg = "û�а����ļ����ã�";
            MessageBox mb = WidgetFactory.createErrorMessageBox(demo.getShell(), "��ʾ��Ϣ", errMsg);
            mb.open();
            RecordToLogger.recordExceptionInfo(e, logger);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
        } catch (Exception e) {
            String errMsg = "û�а����ļ����ã�";
            MessageBox mb = WidgetFactory.createErrorMessageBox(demo.getShell(), "��ʾ��Ϣ", errMsg);
            mb.open();
            RecordToLogger.recordExceptionInfo(e, logger);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
        }
    }

    /**
     * This function deal the complier setting Event.
     * 
     * @see #dealSettingMenuEvent()
     */
    public void doCompilerSetting2() {
        UATCompilerSettingGUI csg = new UATCompilerSettingGUI(demo);
        csg.uatGui.getShell().setEnabled(false);
        csg.go();
    }

    public void doMaxDomainRangeSetting() {
        UATSetMaxDomainRangeGUI maxDomainRangeSet = new UATSetMaxDomainRangeGUI(demo);
        maxDomainRangeSet.uatGUI.getShell().setEnabled(false);
        maxDomainRangeSet.go();
    }

    /**
     * This function deal the complier setting Event.
     * 
     * @see #dealSettingMenuEvent()
     */
    public void doCompilerSetting() {
        UATCompilerSettingGUI_New csg = new UATCompilerSettingGUI_New(demo);
        csg.uatGui.getShell().setEnabled(false);
        csg.go();
    }

    /**
     * This function deal the Language setting Event.
     * 
     * @see #dealSettingMenuEvent()
     */
    public void doLanguageSetting() {
        UATLanguageSettingGUI lsg = new UATLanguageSettingGUI(demo);
        lsg.uatGui.getShell().setEnabled(false);
        lsg.go();
    }

    /**
     * This function deal the test case management setting Event.
     * 
     * @see #dealSettingMenuEvent()
     */
    public void doTestCaseMangementSetting() {
        UATTestCaseManagementSettingGUI lsg = new UATTestCaseManagementSettingGUI(demo);
        lsg.uatGui.getShell().setEnabled(false);
        lsg.go();
    }

    /**
     * This function deal the system setting Event.
     * 
     * @see #dealSettingMenuEvent()
     */
    public void doSystemSetting() {
        UATSystemSettingGUI lsg = new UATSystemSettingGUI(demo);
        lsg.uatGui.getShell().setEnabled(false);
        lsg.go();
    }

    public void doSoftwareMetric() {
        Project currentProject = demo.getCurrentProject();
        boolean result = currentProject.updateMetric();
        if (result) {
            UATSoftwareMetricGUI metricGUI = new UATSoftwareMetricGUI(currentProject);
            metricGUI.go();
        } else {
            MessageBox mb = WidgetFactory.createErrorMessageBox(demo.getShell(), "������Ϣ", "��������ļ�����ģ�黮�ֺ��ٽ����������");
            mb.open();
        }
    }

    /**
     * This function do something when exit the software.
     * 
     * @param e
     */
    public boolean doExit() {
        Project currentProject = demo.getCurrentProject();

        if (null != UATGUITest.excelOperation) {
            UATGUITest.excelOperation.writeAndClose();
        }
        if (null != currentProject) {
            final UATGUI temp = demo;
            UATExitDialog dialogTest = new UATExitDialog(temp);
            dialogTest.setUsage(1);
            dialogTest.go();
            return dialogTest.getCancle();
        } else {
            System.exit(0);
            return true;
        }
    }

    /**
     * this function will generate a GUI for a function which
     * will show all the variable in the function , the author
     * will input some domain for each of the variable
     */
    public void doInputEdit() {
        UATInputOutputGUI inout = new UATInputOutputGUI(demo);
        inout.uatGui.getShell().setEnabled(false);
        inout.go();
    }

    /*
     * add by za
     * �û�ѡ��Ŀ�긲��Ԫ�ش���
     */
    public void doSelectTargetCoverageElement() {
        /*********************************************
         * ����·�������������ɲ�֧�ָ����������ͣ���ʱ���д���
         * �ȶ�֧���ˣ����ö�ȥ��
         * 
         * */
        if (!demo.getCurrentFunc().isTestCaseSupportArgument(TestType.InputDomainBasedRandomTestAndPathBasedRandomTest)) {
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    MessageBox mb = WidgetFactory.createInfoMessageBox(demo.getShell(), "���ڲ�֧�ֵ���������", "���ڲ�֧�ֵ���������");
                    mb.open();
                }
            });
            return;
        }

        /**********************************************/
        TargetCoverElementSelectGUI target = new TargetCoverElementSelectGUI(demo);
        target.uatGui.getShell().setEnabled(false);
        target.go();
    }

    /*
     * add by Cai Min
     * �˹���Ԥ������������
     */
    public void doManualIntervention() {
        // add by chenruolin to check whether the file has been modified
        if (demo.getSaveFile()) {
            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "��ʾ��Ϣ", "���ȱ����ļ�");
            box.open();
            return;
        }
        UATManualInterventionGUI target = new UATManualInterventionGUI(demo);
        target.go();
    }

    /**
     * @author Cai Min
     *         �ع����
     */
    public void doRegressionTest() {
        final TestModule currentFunc = demo.getCurrentFunc();
        AnalysisFile currentFile = demo.getCurrentFile();
        currentFile.isHasRegressionTest();
        final RegressionTester tester = new RegressionTester(demo);
        demo.getOutputTabFolder().setSelection(3);
        if (currentFunc == null) {
            return;
        }
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        new Thread() {
            public void run() {

                progressDisplayGUI.terminateListener.setRunningThread(this);
                final String funcName = currentFunc.getFuncName();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {

                        progressDisplayGUI.setTestProgressRunning(3);
                        clearOutputMessage();
                        demo.setStatusBarInfo("���ڶԺ��� " + funcName + "���лع����...");
                        addOutputMessage("���ڶԺ��� " + funcName + "���лع����...");

                    }

                });

                try {
                    final RegressionTestReturnType type = tester.test();
                    if (type != RegressionTestReturnType.ExecutionDone) {
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                doProjectViewRefresh();
                                doCoverageInfoRefresh();
                                progressDisplayGUI.setTestProgressOver(1);
                                demo.setStatusBarInfo("�Ժ��� " + funcName + "���лع���Խ���");
                                addOutputMessage("�Ժ��� " + funcName + "���лع���Խ���");

                                String msg;
                                if (type == RegressionTestReturnType.ModificationError)
                                    msg = "Դ������������ȷ���޸�";
                                else if (type == RegressionTestReturnType.NoModification)
                                    msg = "ԭ����δ���޸ģ��ع������ֹ";
                                else
                                    msg = "����������Ϊ�գ��ع������ֹ";

                                MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "��ʾ��Ϣ", msg);
                                box.open();
                            }
                        });
                        return;
                    }
                } catch (InterruptedException e1) {
                    logger.error("error in regression test." + e1.toString());
                } catch (UnSupportedArgumentException e1) {
                    logger.error("error in regression test." + e1.toString());
                } catch (NoExitMethodException e1) {
                    logger.error("error in regression test." + e1.toString());
                } catch (IOException e1) {
                    logger.error("error in regression test." + e1.toString());
                } catch (FileOperationException e1) {
                    logger.error("error in regression test." + e1.toString());
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        doProjectViewRefresh();
                        doCoverageInfoRefresh();
                        progressDisplayGUI.setTestProgressOver(1);
                        demo.setStatusBarInfo("�Ժ��� " + funcName + "���лع���Խ���");
                        addOutputMessage("�Ժ��� " + funcName + "���лع���Խ���");
                        String info = "�ع�������,�Ƿ�鿴���Ա��棿";
                        if (currentFunc.getFuncVar().hasFileVar())
                            info += "\n������ȫ�ֱ��������ļ�ָ�룬���û�����У����Խ��";
                        MessageBox box = WidgetFactory.createQuesMessageBox(demo.getShell(), "��ʾ��Ϣ", info);
                        if (box.open() == SWT.YES) {
                            Runtime run = Runtime.getRuntime();
                            String cmd;
                            if (Config.isLinux())
                                cmd = "xdg-open " + tester.getReport();
                            else
                                cmd = "cmd.exe /c start iexplore \"" + tester.getReport() + "\"";
                            try {
                                run.exec(cmd);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        }.start();
    }


    /**
     * This function deal the open project event
     * 
     * @see #dealFileMenuEvent()
     * 
     */
    public void doOpenProject() {
        FileDialog fd = WidgetFactory.createFileDialog(demo.getShell());
        fd.setText("ѡ�񹤳��ļ�");
        fd.setFilterExtensions(new String[] {"*.utp"});
        String path = "";
        path = fd.open();
        if (path == null) {
            return;
        }
        try {
            addPathToConfigFile(path);
        } catch (IOException e2) {
            e2.printStackTrace();
        }

        Project currentProject = demo.getCurrentProject();
        if (currentProject != null) {
            UATExitDialog dialogTest = new UATExitDialog(demo);
            dialogTest.setUsage(2);
            dialogTest.go();
            if (!dialogTest.getCancle()) // ѡ����ȡ����ť
                return;
        }

        final String pathName = path;
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        try {
            new Thread() {
                public void run() {
                    progressDisplayGUI.terminateListener.setRunningThread(this);
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            progressDisplayGUI.setTestProgressRunning(3);
                            progressDisplayGUI.setInfo("���ڴ򿪹��̣����Ժ�...");
                            clearOutputMessage();
                        }
                    });
                    Project project = null;
                    try {
                        project = Project.open(pathName);
                    } catch (IOException e) {
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                progressDisplayGUI.setTestProgressOver(0);
                                String info = "�򿪹��̳��쳣" + "\n�����ļ�������";
                                addOutputMessage(info);
                                demo.setStatusBarInfo(info);
                                MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "�򿪹��̳��쳣", info);
                                box.open();
                            }
                        });
                        return;
                    } catch (ClassNotFoundException e) {
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                progressDisplayGUI.setTestProgressOver(0);
                                String info = "�򿪹��̳��쳣";
                                addOutputMessage(info);
                                demo.setStatusBarInfo(info);
                                MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "�򿪹��̳��쳣", info);
                                box.open();
                            }
                        });
                        return;
                    } catch (OutOfMemoryError e) {
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                progressDisplayGUI.setTestProgressOver(0);
                                String info = "�򿪹��̳��쳣" + "\n�ڴ治��,��ջ���,�����jvm�����������";
                                addOutputMessage(info);
                                demo.setStatusBarInfo(info);
                                MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "�򿪹��̳��쳣", info);
                                box.open();
                            }
                        });
                        return;
                    } catch (Exception e) {
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                progressDisplayGUI.setTestProgressOver(0);
                                addOutputMessage("�򿪹��̳��쳣");
                                demo.setStatusBarInfo("�򿪹��̳��쳣");
                                MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "�򿪹��̳��쳣", "�򿪹��̳��쳣");
                                box.open();
                            }
                        });
                        return;
                    }
                    demo.setCurrentProject(project);
                    demo.setCurrentCoverCriteria(project.getCriteria());
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {

                            doProjectViewRefresh();
                            demo.setSoftwareMetricMenuItemEnable();
                            clearOutputMessage();
                            addOutputMessage("�򿪹��� " + pathName);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                }
            }.start();
        } catch (Exception e) {
            String errMsg = "�򿪹���ʱ��������";
            MessageBox mb = WidgetFactory.createErrorMessageBox(demo.getShell(), "������Ϣ", errMsg);
            mb.open();
            RecordToLogger.recordExceptionInfo(e, logger);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
        }
    }

    public void addPathToConfigFile(String utpFilePath) throws IOException {
        Properties tempProperties = new Properties();
        FileInputStream inputFile = null;
        try {
            inputFile = new FileInputStream(System.getProperty("user.dir") + File.separator + "config" + File.separator + "CurrentProjectConfig.properties");
            tempProperties.load(inputFile);
            inputFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (tempProperties.containsKey("oldest") && !tempProperties.containsValue(utpFilePath)) {
            String oldProjectNo = tempProperties.getProperty("oldest");
            tempProperties.setProperty(oldProjectNo, utpFilePath);
            if (oldProjectNo.equals("1"))
                tempProperties.setProperty("oldest", "2");
            else if (oldProjectNo.equals("2"))
                tempProperties.setProperty("oldest", "3");
            else if (oldProjectNo.equals("3"))
                tempProperties.setProperty("oldest", "4");
            else if (oldProjectNo.equals("4"))
                tempProperties.setProperty("oldest", "5");
            else
                tempProperties.setProperty("oldest", "1");
        } else if (!tempProperties.containsKey("oldest")) {
            tempProperties.setProperty("oldest", "2");
            tempProperties.setProperty("1", utpFilePath);
        }
        try {
            FileOutputStream outputFile = new FileOutputStream(System.getProperty("user.dir") + File.separator + "config" + File.separator + "CurrentProjectConfig.properties");
            tempProperties.store(outputFile, "����򿪹��������ļ�");
            outputFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        // ˢ������򿪹��̲˵�
        demo.refreshCurrentProjectMenu();
    }

    /**
     * Ϊһ����������һ��������ͼ
     */
    public void doShowControlFlowGraph() {
        final TestModule currentFunc = demo.getCurrentFunc();
        final AnalysisFile currentFile = demo.getCurrentFile();
        try {


            if (currentFunc == null)
                return;

            new Thread() {
                public void run() {
                    try {
                        currentFunc.generateCFG(currentFile.getCFGPicDir());
                        Display.getDefault().syncExec(new Runnable() {
                            public void run() {
                                File pic = new File(currentFunc.getCfgName());
                                long size = pic.length();
                                if (size >= 3 * 1024 * 1024)// ����10M��ͼƬ
                                {
                                    MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "ͼƬ̫��", "���ɵĿ�����ͼ��ͼƬ̫��\n,�뵽" + currentFunc.getCfgName() + "�鿴����\n��ѡ�д���Ctrl+C,�����±�ճ��·����");
                                    box.open();
                                } else {
                                    new ImageViewer(currentFunc.getCfgName());
                                    addOutputMessage("���ɿ�����ͼ" + currentFunc.getFuncName());
                                }
                            }
                        });
                    } catch (IOException e) {
                        System.gc();
                        final String msg = e.getMessage();
                        Display.getDefault().syncExec(new Runnable() {
                            public void run() {
                                addOutputMessage("���ɿ�����ͼʱ���ִ��� " + msg);
                            }
                        });
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                        return;
                    } catch (Exception e1) {
                        RecordToLogger.recordExceptionInfo(e1, logger);
                        System.gc();
                    }
                }
            }.start();

        } catch (Exception e) {
            logger.error("���ɿ�����ͼʱ�����쳣");
            RecordToLogger.recordExceptionInfo(e, logger);
            addOutputMessage("���ɿ�����ͼʱ�����쳣 " + e.getMessage());
        }
    }

    /**
     * ��ʾ�������������ĺ����������
     * 
     * @param testCaseID �����������
     *        created by Yaoweichang on 2015-04-16 ����4:19:48
     */
    public void showOneCaseCoverageWindow(long testCaseID) {
        if (demo.getSaveFile()) {
            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "��ʾ��Ϣ", "���ȱ����ļ�");
            box.open();
            return;
        }

        final TestModule currentFunc = demo.getCurrentFunc();
        if (currentFunc == null)
            return;
        UATCoverageWindow coverageWin;
        try {
            coverageWin = new UATCoverageWindow(currentFunc, testCaseID);
            coverageWin.go();
        } catch (IOException e) {
            RecordToLogger.recordExceptionInfo(e, logger);
        } catch (Exception e) {
            RecordToLogger.recordExceptionInfo(e, logger);
            addOutputMessage("��ʾ��������Ϣʱ�����쳣");
        }
    }

    public void showCoverageWindow() {
        // add by chenruolin to check whether the file has been modified
        if (demo.getSaveFile()) {
            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "��ʾ��Ϣ", "���ȱ����ļ�");
            box.open();
            return;
        }

        final TestModule currentFunc = demo.getCurrentFunc();
        AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFunc == null && currentFile == null)
            return;
        UATCoverageWindow coverageWin;
        try {
            if (currentFunc != null)
                coverageWin = new UATCoverageWindow(currentFunc);
            else
                coverageWin = new UATCoverageWindow(currentFile);
            coverageWin.go();
        } catch (IOException e) {
            RecordToLogger.recordExceptionInfo(e, logger);
        } catch (Exception e) {
            RecordToLogger.recordExceptionInfo(e, logger);
            addOutputMessage("��ʾ��������Ϣʱ�����쳣");
        }


    }

    // add by qgn
    public void showUATTestcasesToPathWindow() throws IOException {
        final TestModule currentFunc = demo.getCurrentFunc();
        AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFunc == null && currentFile == null)
            return;

        UATTestCasesToPathGUI UATTestcasesToPathWindow = new UATTestCasesToPathGUI(this.demo);

        try {
            UATTestcasesToPathWindow.open();
        } catch (Exception e) {
            RecordToLogger.recordExceptionInfo(e, logger);
            addOutputMessage("��ʾ·��������ӳ�䴰��ʱ�����쳣");
        }
    }


    public void showUATOneTestcasesToPathWindow() throws IOException {
        final TestModule currentFunc = demo.getCurrentFunc();
        AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFunc == null && currentFile == null)
            return;

        UATOneTestCasesToPathGUI UATTestcasesToPathWindow = new UATOneTestCasesToPathGUI(this.demo, demo.getCurrentTestCaseID());

        try {
            UATTestcasesToPathWindow.open();
        } catch (Exception e) {
            RecordToLogger.recordExceptionInfo(e, logger);
            addOutputMessage("��ʾ·��������ӳ�䴰��ʱ�����쳣");
        }
    }

    /**
     * This function deal Close Project Event.
     * 
     * @see #doNewProject()
     * @see #doOpenProject()
     * @see #doSaveProject()
     * @see #doSaveProjectAs()
     * @see #doExport()
     * @see #doExit(ShellEvent)
     * 
     * @see #doRefresh()
     */
    public void doCloseProject() {

        if (demo.getCurrentProject() == null) {
            return;
        }

        synchronized (demo.items) {
            for (int i = 0; i < demo.items.size(); i++) {
                FileCTabItem fci = demo.items.get(i);
                demo.items.remove(i);
                fci.getCTabItem().dispose();
                i--;
            }
        }
        demo.actionsGUI.clearOutputMessage();

        Project currentProject = demo.getCurrentProject();
        Config.lastProjectPath = currentProject.getPath().substring(0, currentProject.getPath().lastIndexOf(File.separator));
        doClearProject();
        // add by xujixiao
        // memory management
        demo.getCurrentProject().getIncludeDirs().clear();
        demo.getCurrentProject().getFileList().clear();
        demo.getCurrentProject().getFilenameList().clear();
        if (demo.getCurrentProject().getFolderFileList() != null)
            demo.getCurrentProject().getFolderFileList().clear();
        if (demo.getCurrentProject().getFolderFilenameList() != null)
            demo.getCurrentProject().getFolderFilenameList().clear();
        demo.getCurrentProject().getFuncsNumList().clear();
        demo.getCurrentProject().setIsError(null);
        demo.getCurrentProject().setIsExpand(null);
        demo.getCurrentProject().setIsModuleSeparated(null);
        demo.getCurrentProject().setHasAnalysised(null);
        // end add by xujiaoxian
        demo.setCurrentProject(null);
        demo.showSoftwareMetricMenuItem.setEnabled(false);
        demo.doRefresh();
        if (demo.getOutputTabFolder().getSelectionIndex() == 2) {
            demo.doShowTestCasesTree();
            demo.doShowAvaiableTestCases();
            demo.getOutputTabFolder().setSelection(0);
        }
        // add by xujiaoxian
        // �ڴ����
        System.gc();
        // end add by xujiaoxian
    }

    /*
     * Add by za
     * �����ǰ���̵�������Ϣ
     */
    private void doClearProject() {
        demo.setCurrentFile(null);
        demo.setCurrentFunc(null);
    }


    public void doShowSimpleCFG() {
        final TestModule currentFunc = demo.getCurrentFunc();
        final AnalysisFile currentFile = demo.getCurrentFile();
        try {
            if (currentFunc == null)
                return;
            new Thread() {
                public void run() {
                    try {
                        currentFunc.printCFG(currentFile.getCFGPicDir());
                        Display.getDefault().syncExec(new Runnable() {
                            public void run() {
                                File pic = new File(currentFunc.getCfgName());
                                long size = pic.length();
                                if (size >= 2 * 1024 * 1024)// ����2M��ͼƬ
                                {
                                    MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "ͼƬ̫��", "���ɵļ򻯿�����ͼ��ͼƬ̫��\n,�뵽" + currentFunc.getCfgName() + "�鿴����\n��ѡ�д���Ctrl+C,�����±�ճ��·����");
                                    box.open();
                                } else {
                                    new ImageViewer(currentFunc.getCfgName());
                                    addOutputMessage("�����˼򻯵Ŀ�����ͼ " + currentFunc.getCfgName());
                                }
                            }
                        });
                    } catch (Exception e) {
                        final String msg = e.getMessage();
                        Display.getDefault().syncExec(new Runnable() {
                            public void run() {
                                addOutputMessage("���ɿ�����ͼʱ���ִ��� " + msg);
                            }
                        });
                        logger.error("���ɿ�����ͼʱ���ִ���");
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                        return;
                    }


                }
            }.start();

        } catch (Exception e) {
            addOutputMessage("���ɼ򻯵Ŀ�����ͼʱ�����쳣 " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    /* Ϊһ���ļ����ɵ���ͼ */
    public void doGenerateCallGraph() {
        final AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFile == null)
            return;
        try {
            demo.setStatusBarInfo("���ļ� " + currentFile.getFile() + " ���ɺ�������ͼ");
            currentFile.dumpCallGraph();
            PictureViewer.showPicture(currentFile.getCallGraphPicName() + ".jpg", "�ļ�" + currentFile.getFile() + "��������ͼ");
            addOutputMessage("���ɵ���ͼ " + currentFile.getCallGraphPicName() + ".jpg");

        } catch (Exception e) {
            addOutputMessage("���ɺ�������ͼʱ�����쳣");
            logger.error("���ɺ�������ͼʱ�����쳣\n");
            RecordToLogger.recordExceptionInfo(e, logger);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
        }
    }


    /**
     * add by Cai Min, 2011/6/29
     * ��������
     */
    public void doParamSetting() {
        new UATParamSettingGUI(demo);
    }

    /**
     * 2012-11-1
     * generate all of the paths in the test module
     * 
     * @author xujiaoxian
     */
    public void doGenerateAllPath() {
        demo.getOutputTabFolder().setSelection(3);
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        final TestModule currentFunc = demo.getCurrentFunc();
        if (currentFunc == null) {
            return;
        }
        new Thread() {

            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                final String funcName = currentFunc.getFuncName();
                TestModule testFunc = currentFunc;
                CoverRule cr = demo.getCurrentCoverCriteria();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("��������·�������Ժ�...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("���ڶԺ��� " + funcName + "��" + demo.getCurrentCoverCriteria().toString() + "�½�������·��...");
                        addOutputMessage("���ڶԺ��� " + funcName + "��" + demo.getCurrentCoverCriteria().toString() + "�½�������·��...");
                    }

                });
                try {
                    testFunc.generateAllPath(cr);
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            progressDisplayGUI.setTestProgressOver(0);
                            demo.setStatusBarInfo("�Ժ��� " + funcName + "��" + demo.getCurrentCoverCriteria().toString() + "������·������");
                            addOutputMessage("�Ժ��� " + funcName + "��" + demo.getCurrentCoverCriteria().toString() + "������·������");
                            if (demo.getCurrentFunc().getAllpath().size() > 0) {
                                addOutputMessage("���� " + funcName + "��" + demo.getCurrentCoverCriteria().toString() + "������·���У�");
                                List<OnePath> onepathlist = demo.getCurrentFunc().getAllpath();
                                int listsize = onepathlist.size();
                                for (int i = 0; i < listsize; i++) {
                                    addOutputMessage(onepathlist.get(i).toString());
                                }
                            } else {
                                addOutputMessage("���� " + funcName + "��" + demo.getCurrentCoverCriteria().toString() + "��û������·��");
                            }

                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "��ʾ��Ϣ", "�Ժ��� " + funcName + "��" + demo.getCurrentCoverCriteria().toString() + "������·�����");
                            box.open();
                        }

                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "����ʱ�쳣", "���� " + funcName + "������ѭ����û�г���" + msg);
                            box.open();
                            addOutputMessage("���� " + funcName + "������ѭ����û�г���" + msg);
                        }
                    });
                }
            }
        }.start();
    }

}
