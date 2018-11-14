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
     * 对所有文件进行所有的测试，包含：基于输入域的测试，基于路径的随机测试，基于路径的约束求解测试
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
                            progressDisplayGUI.setInfo("正在 对文件 " + fileName + "进行自动测试...");
                            addOutputMessage("正在 对文件 " + fileName + "进行自动测试...");
                            demo.setStatusBarInfo("正在对文件 " + fileName + "进行自动测试...");
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
                                demo.setStatusBarInfo("正在对函数 " + funcName + " 进行 " + cr + " 测试");
                                addOutputMessage("正在对函数 " + funcName + " 进行 " + cr + " 测试");
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
                                    addOutputMessage("函数 " + funcName + "含有测试用例生成不支持的数据类型");
                                }
                            });
                        } catch (NoExitMethodException e) {
                            final String msg = e.getMessage();
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    addOutputMessage("函数 " + funcName + "存在死循环，没有出口" + msg);
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
                        addOutputMessage("所有文件测试完毕");
                        demo.setStatusBarInfo("所有文件测试完毕");
                        MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试结束", " 所有函数自动测试结束");
                        box.open();
                    }
                });
                progressDisplayGUI.terminateListener.setRunningThread(null);
            }
        }.start();
    }

    /**
     * 对指定函数进行所有的测试，包含：基于输入域的测试，基于路径的随机测试，基于路径的约束求解测试
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
                            demo.setStatusBarInfo("正在对文件 " + fileName + " 的函数 " + funcName + " 进行 " + cr + " 测试");
                            addOutputMessage("正在对文件 " + fileName + " 的函数 " + funcName + " 进行 " + cr + " 测试");
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
                                addOutputMessage("函数 " + funcName + "含有测试用例生成不支持的数据类型");
                            }
                        });
                    } catch (NoExitMethodException e) {
                        final String msg = e.getMessage();
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                addOutputMessage("函数 " + funcName + "存在死循环，没有出口" + msg);
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
                        addOutputMessage("所有文件测试完毕");
                        demo.setStatusBarInfo("所有文件测试完毕");
                        MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试结束", " 所有函数自动测试结束");
                        box.open();
                    }
                });
                progressDisplayGUI.terminateListener.setRunningThread(null);
            }
        }.start();
    }

    /**
     * 对单个文件进行所有的测试
     * 
     * @throws IOException
     */
    public void doAutoTestForFile(final String selected) {


        System.out.println("对单个文件进行测试" + selected);
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
                        progressDisplayGUI.setInfo("正在 对文件 " + fileName + "进行自动测试...");
                        addOutputMessage("正在 对文件 " + fileName + "进行自动测试...");
                        demo.setStatusBarInfo("正在对文件 " + fileName + "进行自动测试...");
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
                            demo.setStatusBarInfo("正在对函数 " + funcName + " 进行 " + cr + " 测试");
                            addOutputMessage("正在对函数 " + funcName + " 进行 " + cr + " 测试");
                        }
                    });

                    demo.setCurrentFunc(tm);
                    doAutoTestCaseGenerateForAll();
                }

                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        doProjectViewRefresh();
                        progressDisplayGUI.setTestProgressOver(1);
                        addOutputMessage("所有文件测试完毕");
                        demo.setStatusBarInfo("所有文件测试完毕");
                        MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试结束", " 所有函数自动测试结束");
                        box.open();
                    }
                });
                progressDisplayGUI.terminateListener.setRunningThread(null);

                File out2 = new File("./自动化测试结果/异常.txt");
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
                        bos2.append("\r\n" + "文件名：" + demo.getCurrentFile().getFile() + "\r\n");
                    }

                    for (String testModuleName : funcNameList) {
                        try {
                            if (demo.getCurrentCoverCriteria().BlockCover) {
                                bos2.append("函数名:" + testModuleName + "\t" + "语句覆盖" + "\n");
                            }
                            if (demo.getCurrentCoverCriteria().BranchCover) {
                                bos2.append("函数名:" + testModuleName + "\t分支覆盖" + "\n");
                            }
                            if (demo.getCurrentCoverCriteria().MCDCCover) {
                                bos2.append("函数名:" + testModuleName + "\tMCDC覆盖" + "\n");
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
     * 工程级的文件测试
     * 
     * @param selected
     *        created by Yaoweichang on 2015-04-13 下午3:45:43
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
                            progressDisplayGUI.setInfo("正在 对文件 " + fileName + "进行自动测试...");
                            addOutputMessage("正在 对文件 " + fileName + "进行自动测试...");
                            demo.setStatusBarInfo("正在对文件 " + fileName + "进行自动测试...");
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
                                demo.setStatusBarInfo("正在对函数 " + funcName + " 进行 " + cr + " 测试");
                                addOutputMessage("正在对函数 " + funcName + " 进行 " + cr + " 测试");
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
                        addOutputMessage("所有文件测试完毕");
                        demo.setStatusBarInfo("所有文件测试完毕");
                        MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试结束", " 所有函数自动测试结束");
                        box.open();
                    }
                });
                progressDisplayGUI.terminateListener.setRunningThread(null);

            }
        }.start();
    }

    /**
     * 自动生成测试用例，用户不用关心生成测试 用例的策略，只要为用户生成覆盖率能达到100%的要求。
     * modified by xujiaoxian
     */
    public void doAutoTestCaseGenerateForAll() {
        // 显示覆盖率板块
        final TestModule currentFunc = demo.getCurrentFunc();
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        if (currentFunc == null) {
            return;
        }
        final String funcName = currentFunc.getFuncName();
        // 防止在CurrentFunc被改变,用一个临时的变量保存当前选中要测试的函数
        final TestModule testFunc = currentFunc;
        final CoverRule cr = demo.getCurrentCoverCriteria();
        // 修改界面的用这个线程去执行
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {

                progressDisplayGUI.setTestProgressRunning(1);
                progressDisplayGUI.setInfo("正在生成测试用例，请稍候...");
                clearOutputMessage();
                demo.setStatusBarInfo("正在对函数 " + funcName + "进行自动测试...");
                addOutputMessage("正在对函数 " + funcName + "进行自动测试...");
            }
        });

        Callable<String> call = new Callable<String>() {
            public String call() throws Exception {
                try {
                    logger.error(demo.getCurrentFile() + "\t" + demo.getCurrentFunc() + "\n*********\n");
                    testFunc.autoTestCaseGenerate(cr); // yumeng,testFunc就是线程交互的关键，这一步加上时间控制
                } catch (UnSupportedArgumentException e) {
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    final String msg = e.getMessage();
                    Display.getDefault().asyncExec(new Runnable() {

                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试用例生成不支持的数据类型", msg);
                            box.open();
                            addOutputMessage("函数 " + funcName + "含有测试用例生成不支持的数据类型 " + msg);
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
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试时异常", "自动测试时发生异常\n" + msg);
                            box.open();
                            addOutputMessage("对函数" + funcName + "测试时发生异常 " + msg);
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
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试时异常", "函数 " + funcName + "存在死循环，没有出口\n" + msg);
                            box.open();
                            addOutputMessage("函数 " + funcName + "存在死循环，没有出口" + msg);
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
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试时异常", "自动测试时发生异常\n" + msg);
                            box.open();
                            addOutputMessage("自动测试时出现异常 " + msg);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                } finally {
                    progressDisplayGUI.terminateListener.setRunningThread(null);
                }
                return "自动测试用例执行的时间控制机制完成";
            }
        };

        FutureTask<String> f = new FutureTask<String>(call);
        Thread t = new Thread(f);
        progressDisplayGUI.terminateListener.setRunningThread(t);
        t.start();
        try {
            f.get(Long.parseLong(Config.TestCaseGenTime), TimeUnit.MINUTES);// 限定自动测试用例生成最多执行的时间
        } catch (InterruptedException e1) {
        } catch (ExecutionException e1) {
        } catch (TimeoutException e1) {
            funcNameList.remove(demo.getCurrentFunc().getFuncName());

            File dirFile = new File("./自动化测试结果");
            if (!dirFile.exists() && !dirFile.isDirectory()) {
                dirFile.mkdir();
            }
            File out = new File("./自动化测试结果/超时.txt");
            if (!out.exists())
                try {
                    out.createNewFile();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }

            float lastBlockCoverage = testFunc.getCoverSetList().get(0).getCoverage();// 上一次的语句覆盖率
            float lastBranchCoverage = testFunc.getCoverSetList().get(1).getCoverage();// 上一次的分支覆盖率
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
                bos.append("\r\n文件名:" + demo.getCurrentFile().getFile() + "\t函数名:" + demo.getCurrentFunc().getFuncName() + "\t");
                if (demo.getCurrentCoverCriteria().BlockCover) {
                    msg1 =
                            msg1 + "覆盖元素数： " + (int) testFunc.getCoverSetList().get(0).calculateCoverednumber() + "\t总元素数: " + testFunc.getCoverSetList().get(0).getTotalElementNumber()
                                    + "\t语句覆盖覆盖率: " + numFormater.format(lastBlockCoverage * 100) + "%" + "\t" + demo.getCurrentFunc().getAllNewTestData().getTestCaseSize() + "\r\n";
                    bos.append(msg1);
                }
                if (demo.getCurrentCoverCriteria().BranchCover) {
                    msg2 =
                            msg2 + "覆盖元素数： " + (int) testFunc.getCoverSetList().get(1).calculateCoverednumber() + "\t总元素数： " + testFunc.getCoverSetList().get(1).getTotalElementNumber()
                                    + "\t分支覆盖覆盖率: " + numFormater.format(lastBranchCoverage * 100) + "%" + "\t" + demo.getCurrentFunc().getAllNewTestData().getTestCaseSize() + "\r\n";
                    bos.append(msg2);
                }
                if (demo.getCurrentCoverCriteria().MCDCCover) {
                    msg3 =
                            msg3 + "覆盖元素数： " + (int) testFunc.getCoverSetList().get(2).calculateCoverednumber() + "\t总元素数： " + testFunc.getCoverSetList().get(2).getTotalElementNumber()
                                    + "\tMCDC覆盖覆盖率: " + numFormater.format(lastMcdcCoverage * 100) + "%" + "\t" + demo.getCurrentFunc().getAllNewTestData().getTestCaseSize() + "\r\n";
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
     * 自动生成测试用例，用户不用关心生成测试 用例的策略，只要为用户生成覆盖率能达到100%的要求。
     * modified by xujiaoxian
     */
    public void doAutoTestCaseGenerate() {
        // 显示覆盖率板块
        demo.getOutputTabFolder().setSelection(3);
        final TestModule currentFunc = demo.getCurrentFunc();
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        if (currentFunc == null) {
            return;
        }
        // 逻辑线程，防止主界面假死
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                final String funcName = currentFunc.getFuncName();
                // 防止在CurrentFunc被改变,用一个临时的变量保存当前选中要测试的函数
                final TestModule testFunc = currentFunc;
                final CoverRule cr = demo.getCurrentCoverCriteria();
                // 修改界面的用这个线程去执行
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {

                        progressDisplayGUI.setTestProgressRunning(1);
                        progressDisplayGUI.setInfo("正在生成测试用例，请稍候...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("正在对函数 " + funcName + "进行自动测试...");
                        addOutputMessage("正在对函数 " + funcName + "进行自动测试...");
                    }
                });

                Callable<String> call = new Callable<String>() {
                    public String call() throws Exception {
                        try {
                            testFunc.autoTestCaseGenerate(cr); // yumeng,testFunc就是线程交互的关键，这一步加上时间控制

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
                                    float lastBlockCoverage = testFunc.getCoverSetList().get(0).getCoverage();// 上一次的语句覆盖率
                                    float lastBranchCoverage = testFunc.getCoverSetList().get(1).getCoverage();// 上一次的分支覆盖率
                                    float lastMcdcCoverage = testFunc.getCoverSetList().get(2).getCoverage();// 上一次的MC/DC覆盖率
                                    // 接下来判断是否需要提示用户进行人工辅助，已达到100%的覆盖率要求
                                    boolean needManualInterval = false;// 是否需要人工辅助
                                    switch (crValue) {
                                        case 0:// 没有选择语句覆盖、分支覆盖、MC/DC覆盖准则中的任何一个
                                            break;
                                        case 1:// 只选择语句覆盖
                                            if (Math.abs(lastBlockCoverage - 1.0) > 0.01) {
                                                needManualInterval = true;
                                            } else {
                                                needManualInterval = false;
                                            }
                                            break;
                                        case 2:// 只选择了分支覆盖
                                            if (Math.abs(lastBranchCoverage - 1.0) > 0.01) {
                                                needManualInterval = true;
                                            } else {
                                                needManualInterval = false;
                                            }
                                            break;
                                        case 3:// 同时选择了语句覆盖和分支覆盖
                                            if (Math.abs(lastBlockCoverage - 1.0) > 0.01 || Math.abs(lastBranchCoverage - 1.0) > 0.01) {
                                                needManualInterval = true;
                                            } else {
                                                needManualInterval = false;
                                            }
                                            break;
                                        case 4:// 只选择了MC/DC覆盖
                                            if (Math.abs(lastMcdcCoverage - 1.0) > 0.01) {
                                                needManualInterval = true;
                                            } else {
                                                needManualInterval = false;
                                            }
                                            break;
                                        case 5:// 同时选择了MC/DC覆盖和语句覆盖
                                            if (Math.abs(lastBlockCoverage - 1.0) > 0.01 || Math.abs(lastMcdcCoverage - 1.0) > 0.01) {
                                                needManualInterval = true;
                                            } else {
                                                needManualInterval = false;
                                            }
                                            break;
                                        case 6:// 同时选择了MC/DC覆盖和分支覆盖
                                            if (Math.abs(lastBranchCoverage - 1.0) > 0.01 || Math.abs(lastMcdcCoverage - 1.0) > 0.01) {
                                                needManualInterval = true;
                                            } else {
                                                needManualInterval = false;
                                            }
                                            break;
                                        case 7:// 同时选择了MC/DC覆盖、分支覆盖和语句覆盖
                                            if (Math.abs(lastBlockCoverage - 1.0) > 0.01 || Math.abs(lastBranchCoverage - 1.0) > 0.01 || Math.abs(lastMcdcCoverage - 1.0) > 0.01) {
                                                needManualInterval = true;
                                            } else {
                                                needManualInterval = false;
                                            }
                                            break;
                                        default:// 其他的情况
                                            break;
                                    }
                                    if (needManualInterval) {
                                        NumberFormat numFormater = NumberFormat.getNumberInstance();
                                        numFormater.setMaximumFractionDigits(2);
                                        String coverage = "";
                                        if (demo.getCurrentCoverCriteria().BlockCover) {
                                            coverage += "语句覆盖：" + numFormater.format(lastBlockCoverage * 100) + "%, ";
                                        }
                                        if (demo.getCurrentCoverCriteria().BranchCover)
                                            coverage += "分支覆盖：" + numFormater.format(lastBranchCoverage * 100) + "%, ";
                                        if (demo.getCurrentCoverCriteria().MCDCCover)
                                            coverage += "MC/DC覆盖：" + numFormater.format(lastMcdcCoverage * 100) + "%, ";
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
                                    demo.setStatusBarInfo("对函数 " + funcName + "进行自动测试结束");
                                    addOutputMessage("对函数 " + funcName + "进行自动测试结束");
                                    String info = "自动测试完成";
                                    if (currentFunc.getFuncVar().hasFileVar())
                                        info += "\n参数或全局变量中有文件指针，请用户自行校验测试结果";
                                    MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "提示信息", info);
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
                                    MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试用例生成不支持的数据类型", msg);
                                    box.open();
                                    addOutputMessage("函数 " + funcName + "含有测试用例生成不支持的数据类型 " + msg);
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
                                    MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试时异常", "自动测试时发生异常\n" + msg);
                                    box.open();
                                    addOutputMessage("对函数" + funcName + "测试时发生异常 " + msg);
                                    progressDisplayGUI.setTestProgressOver(0);
                                }
                            });
                        } catch (NoExitMethodException e) {
                            final String msg = e.getMessage();
                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "提示", "函数 " + funcName + "存在死循环，没有出口。请修改或跳过此测试函数。\n" + msg);
                                    box.open();
                                    addOutputMessage("函数 " + funcName + "存在死循环，没有出口。请修改或跳过此测试函数" + msg);
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
                                    MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试时异常", "自动测试时发生异常\n" + msg);
                                    box.open();
                                    addOutputMessage("自动测试时出现异常 " + msg);
                                    progressDisplayGUI.setTestProgressOver(0);
                                }
                            });
                        } finally {
                            progressDisplayGUI.terminateListener.setRunningThread(null);
                        }
                        return "自动测试用例执行的时间控制机制完成";
                    }
                };

                FutureTask<String> f = new FutureTask<String>(call);
                Thread t = new Thread(f);
                progressDisplayGUI.terminateListener.setRunningThread(t);
                t.start();
                try {
                    f.get(Long.parseLong(Config.TestCaseGenTime), TimeUnit.MINUTES);// 限定自动测试用例生成最多执行的时间
                } catch (InterruptedException e1) {
                    // e1.printStackTrace();
                } catch (ExecutionException e1) {
                    // e1.printStackTrace();
                } catch (TimeoutException e1) {
                    t.stop();
                    // 修改界面的用这个线程去执行
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "时间超时", "用例生成时间超过" + Config.TestCaseGenTime + "分钟，提前终止程序的运行。");
                            box.open();
                            progressDisplayGUI.setTestProgressOver(1);
                        }
                    });
                }
            }
        }.start();

    }

    /**
     * 基于输入与域的随机测试
     */
    public void doRandomTestBaseInputDomain() {
        // 显示覆盖率板块
        demo.getOutputTabFolder().setSelection(3);
        final TestModule currentFunc = demo.getCurrentFunc();
        final UATProgressDisplayGUI progressDisplayGUI = demo.getUATProgressDisplayGUI();
        if (currentFunc == null) {
            return;
        }
        // 逻辑线程，防止主界面假死
        new Thread() {
            public void run() {
                progressDisplayGUI.terminateListener.setRunningThread(this);
                final String funcName = currentFunc.getFuncName();
                // 防止在CurrentFunc被改变,用一个临时的变量保存当前选中要测试的函数
                TestModule testFunc = currentFunc;
                CoverRule cr = demo.getCurrentCoverCriteria();
                // 修改界面的用这个线程去执行
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {

                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("正在生成测试用例，请稍候...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("正在对函数 " + funcName + "进行基于输入域的随机测试...");
                        addOutputMessage("正在对函数 " + funcName + "进行基于输入域的随机测试...");
                    }
                });
                try {
                    testFunc.autoInputDomainRandomTest(cr);

                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            demo.setStatusBarInfo("对函数 " + funcName + "进行基于输入域的随机测试结束");
                            addOutputMessage("对函数 " + funcName + "进行基于输入域的随机测试结束");
                            String info = "基于输入域的随机测试完成";
                            if (currentFunc.getFuncVar().hasFileVar())
                                info += "\n参数或全局变量中有文件指针，请用户自行校验测试结果";
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "提示信息", info);
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
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试用例生成不支持的数据类型", msg);
                            box.open();
                            addOutputMessage("函数 " + funcName + "含有测试用例生成不支持的数据类型 " + msg);
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
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试时异常", "函数 " + funcName + "存在死循环，没有出口\n" + msg);
                            box.open();
                            addOutputMessage("函数 " + funcName + "存在死循环，没有出口" + msg);
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
     * 基于输入域、路径和边界值的随机测试
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
                        progressDisplayGUI.setInfo("正在生成测试用例，请稍候...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("正在对函数 " + funcName + "进行基于输入域、基于路径随机测试和基于边界值的测试...");
                        addOutputMessage("正在对函数 " + funcName + "进行基于输入域、基于路径随机测试和基于边界值的测试...");
                    }
                });
                try {
                    testFunc.autoInputDomainRandomAndPathRamdomTest(cr);
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            doCoverageInfoRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            demo.setStatusBarInfo("对函数 " + funcName + "进行基于输入域、基于路径随机测试和基于边界值的测试结束");
                            addOutputMessage("对函数 " + funcName + "进行基于输入域、基于路径随机测试和基于边界值的测试结束");
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "提示信息", "基于输入域、基于路径随机测试和基于边界值的测试完成");
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
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试用例生成不支持的数据类型", msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("函数 " + funcName + "含有测试用例生成不支持的数据类型 " + msg);
                        }
                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试时异常", "函数 " + funcName + "存在死循环，没有出口" + msg);
                            box.open();
                            addOutputMessage("函数 " + funcName + "存在死循环，没有出口" + msg);
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
     * 基于输入域和路径的随机测试
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
                        progressDisplayGUI.setInfo("正在生成测试用例，请稍候...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("正在对函数 " + funcName + "进行基于输入域的随机测试和基于路径的随机测试...");
                        addOutputMessage("正在对函数 " + funcName + "进行基于输入域的随机测试和基于路径的随机测试...");
                    }
                });
                try {
                    testFunc.autoInputDomainRandomAndPathRamdomTest(cr);
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            doCoverageInfoRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            demo.setStatusBarInfo("对函数 " + funcName + "进行基于输入域的随机测试和基于路径的随机测试结束");
                            addOutputMessage("对函数 " + funcName + "进行基于输入域的随机测试和基于路径的随机测试结束");
                            String info = "基于输入域的随机测试和基于路径的随机测试完成";
                            if (currentFunc.getFuncVar().hasFileVar())
                                info += "\n参数或全局变量中有文件指针，请用户自行校验测试结果";
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "提示信息", info);
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
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试用例生成不支持的数据类型", msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("函数 " + funcName + "含有测试用例生成不支持的数据类型 " + msg);
                        }
                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试时异常", "函数 " + funcName + "存在死循环，没有出口" + msg);
                            box.open();
                            addOutputMessage("函数 " + funcName + "存在死循环，没有出口" + msg);
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
     * 基于输入域和基于路径随机测试、路径约束求解生成测试用例的测试
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
                // 防止被瞬间改变
                CoverRule cr = demo.getCurrentCoverCriteria();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("正在生成测试用例，请稍候...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("正在对函数 " + funcName + "进行基于输入域的随机测试和基于路径的随机测试和基于路径的约束求解测试...");
                        addOutputMessage("正在对函数 " + funcName + "进行基于输入域的随机测试和基于路径的随机测试和基于路径的约束求解测试...");
                    }
                });
                try {
                    testFunc.autoRandomAndPathTest(cr);

                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            demo.setStatusBarInfo("对函数 " + funcName + "进行基于输入域的随机测试和基于路径的随机测试和基于路径的约束求解测试结束");
                            addOutputMessage("对函数 " + funcName + "进行基于输入域的随机测试和基于路径的随机测试和基于路径的约束求解测试结束");
                            String info = "基于输入域的随机测试和基于路径的随机测试和基于路径的约束求解完成";
                            if (currentFunc.getFuncVar().hasFileVar())
                                info += "\n参数或全局变量中有文件指针，请用户自行校验测试结果";
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "提示信息", info);
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
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试用例生成不支持的数据类型", msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("函数 " + funcName + "含有测试用例生成不支持的数据类型 " + msg);
                        }
                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试时异常", "函数 " + funcName + "存在死循环，没有出口" + msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("函数 " + funcName + "存在死循环，没有出口" + msg);
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
     * 基于输入域和基于路径随机测试、路径约束求解生成测试用例的测试
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
                // 防止被瞬间改变
                CoverRule cr = demo.getCurrentCoverCriteria();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("正在生成测试用例，请稍候...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("正在对函数 " + funcName + "进行基于路径的随机测试...");
                        addOutputMessage("正在对函数 " + funcName + "进行基于进行基于路径的随机测试...");
                    }
                });
                try {
                    testFunc.autoPathBasedRandomTest(cr);

                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            demo.setStatusBarInfo("对函数 " + funcName + "进行基于路径的随机测试结束");
                            addOutputMessage("对函数 " + funcName + "进行基于路径的随机测试结束");
                            String info = "基于路径的随机测试完成";
                            if (currentFunc.getFuncVar().hasFileVar())
                                info += "\n参数或全局变量中有文件指针，请用户自行校验测试结果";
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "提示信息", info);
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
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试用例生成不支持的数据类型", msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("函数 " + funcName + "含有测试用例生成不支持的数据类型 " + msg);
                        }
                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试时异常", "函数 " + funcName + "存在死循环，没有出口\n" + msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("函数 " + funcName + "存在死循环，没有出口" + msg);
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
                // 防止被瞬间改变
                CoverRule cr = demo.getCurrentCoverCriteria();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("正在生成测试用例，请稍候...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("正在对函数 " + funcName + "进行边界值测试...");
                        addOutputMessage("正在对函数 " + funcName + "进行基于进行边界值测试...");

                    }

                });
                try {
                    testFunc.autoBoundaryTest(cr);

                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            demo.setStatusBarInfo("对函数 " + funcName + "进行边界值测试结束");
                            addOutputMessage("对函数 " + funcName + "进行边界值测试结束");
                            String info = "基于边界值测试完成";
                            if (currentFunc.getFuncVar().hasFileVar())
                                info += "\n参数或全局变量中有文件指针，请用户自行校验测试结果";
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "提示信息", info);
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
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试用例生成不支持的数据类型", msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("函数 " + funcName + "含有测试用例生成不支持的数据类型 " + msg);
                        }
                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试时异常", "函数 " + funcName + "存在死循环，没有出口\n" + msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("函数 " + funcName + "存在死循环，没有出口" + msg);
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
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试时异常", "边界值发生异常 \n" + msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("边界值测试发生异常 " + msg);
                        }
                    });
                } finally {
                    progressDisplayGUI.terminateListener.setRunningThread(null);
                }
            }
        }.start();
    }

    /**
     * 基于约束求解测试
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
                // 防止被瞬间改变
                CoverRule cr = demo.getCurrentCoverCriteria();
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        progressDisplayGUI.setTestProgressRunning(3);
                        progressDisplayGUI.setInfo("正在生成测试用例，请稍候...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("正在对函数 " + funcName + "进行基于约束求解测试...");
                        addOutputMessage("正在对函数 " + funcName + "进行基于约束求解测试...");
                    }
                });
                try {
                    testFunc.autoConstraintTest(cr);
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            doProjectViewRefresh();
                            progressDisplayGUI.setTestProgressOver(1);
                            demo.setStatusBarInfo("对函数 " + funcName + "进行基于约束求解测试结束");
                            addOutputMessage("对函数 " + funcName + "进行基于约束求解测试结束");
                            String info = "基于约束求解测试完成";
                            if (currentFunc.getFuncVar().hasFileVar())
                                info += "\n参数或全局变量中有文件指针，请用户自行校验测试结果";
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "提示信息", info);
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
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试用例生成不支持的数据类型", msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("函数 " + funcName + "含有测试用例生成不支持的数据类型 " + msg);
                        }
                    });
                } catch (NoExitMethodException e) {
                    final String msg = e.getMessage();
                    RecordToLogger.recordExceptionInfo(e, logger);
                    if (Config.printExceptionInfoToConsole)
                        e.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), funcName + "存在死循环，没有出口", msg);
                            box.open();
                            progressDisplayGUI.setTestProgressOver(0);
                            addOutputMessage("函数 " + funcName + "存在死循环，没有出口" + msg);
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
     * 对一个文件内部的所有的函数进行自动测试
     */
    public void doAutoTestForAllFunctionInFile(final int kind) {
        // 显示输出信息
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

                    testName[0] = "基于输入域的随机测试";
                    testName[1] = "基于输入域和基于路径的随机测试";
                    testName[2] = "基于输入域和基于路径的随机测试和基于路径的约束求解测试";
                    testName[3] = "基于路径的随机测试";
                    testName[4] = "基于路径的约束求解测试";

                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            progressDisplayGUI.setTestProgressRunning(3);
                            progressDisplayGUI.setInfo("正在生成测试用例，请稍候...");
                            demo.setStatusBarInfo("对文件 " + fileName + "内的所有函数进行" + testName[kind]);
                            addOutputMessage("对文件 " + fileName + "内的所有函数进行" + testName[kind]);

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
                                    addOutputMessage("函数 " + funcName + "含有测试用例生成不支持的数据类型" + msg);
                                }
                            });
                        } catch (NoExitMethodException e) {
                            final String funcName = tm.getFuncName();
                            RecordToLogger.recordExceptionInfo(e, logger);
                            if (Config.printExceptionInfoToConsole)
                                e.printStackTrace();
                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    addOutputMessage("函数 " + funcName + "存在死循环，没有出口");
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
                            demo.setStatusBarInfo("对文件 " + fileName + " 内的所有函数的 " + testName[kind] + "测试结束");
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试结束", "对文件 " + fileName + " 内的所有函数的 " + testName[kind] + "测试结束");
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
                MessageBox mb = WidgetFactory.createInfoMessageBox(demo.getShell(), "内存溢出", "内存溢出");
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
                MessageBox mb = WidgetFactory.createInfoMessageBox(demo.getShell(), "内存溢出", "内存溢出");
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
                // 如果没有设置Inlude路径
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
                MessageBox mb = WidgetFactory.createInfoMessageBox(demo.getShell(), "内存溢出", "内存溢出");
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
                MessageBox mb = WidgetFactory.createInfoMessageBox(demo.getShell(), "内存溢出", "内存溢出");
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
                // 如果没有设置Inlude路径
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
                // 单元划分
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

        } else {// 为了解决软件度量和查看工程属性因为有文件未模块化分不能查看的问题 add by xujiaoxian
            demo.getCurrentProject().getIsError().set(demo.getCurrentProject().getfilesLoc(currentFile.getFile()), true);
        }
    }

    /**
     * 2013/3/26 xujiaoxian重写该方法.
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

        // 这一行很重要，关系工程树显示的风格,要保留原来的风格的话，必须要改好这一块
        // WidgetFactory.setTreeContents( root,demo.getProjectViewTree(), WidgetFactory.PROJECT );

        // demo.expandProjectViewTree();
        demo.doMeauToolBarRefresh();
        Config.isTestCaseGenerate = false;

        // demo.actionsGUI.doCoverageInfoRefresh();
    }

    /**
     * 2013/4/10 xujiaoxian
     * 重新载入和刷新工程时，调用的是该方法。
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

        // 这一行很重要，关系工程树显示的风格,要保留原来的风格的话，必须要改好这一块
        // WidgetFactory.setTreeContents( root,demo.getProjectViewTree(), WidgetFactory.PROJECT );

        demo.expandProjectViewTree();
    }

    // 更新覆盖率信息
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
            addOutputMessage("显示测试结果文件时发生异常  " + e.getMessage());
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
            addOutputMessage("显示测试结果文件时发生异常  " + e.getMessage());
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
            addOutputMessage("显示测试结果报告文件发生异常  " + e.getMessage());
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
            addOutputMessage("显示驱动文件时发生异常  " + e.getMessage());
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
            addOutputMessage("显示回归测试文件时发生异常  " + e.getMessage());
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
            addOutputMessage("显示桩文件时发生异常  " + e.getMessage());
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
            addOutputMessage("显示预处理文件时发生异常  " + e.getMessage());
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
            addOutputMessage("显示插装后时发生异常  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    /**
     * 查看控制台输出文件
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
            addOutputMessage("显示控制台输出文件时发生异常  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    /**
     * 查看选中函数的源代码
     */
    public void doShowsourceFile() {
        AnalysisFile currentFile = demo.getCurrentFile();
        try {
            File file = new File(currentFile.getFile());

            FileTabManager.ShowFile(file, demo, true);
        } catch (Exception e) {
            addOutputMessage("显示源代码时发生异常  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    /**
     * 查看选中函数的测试用例集合
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
            addOutputMessage("显示测试用例时发生异常  " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    public void doShowLogFile() {
        // 添加日志文件显示多系统支持功能 add by Yaoweichang
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
     * 编辑选中的函数的测试用例
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
            MessageBox mb = WidgetFactory.createInfoMessageBox(demo.getShell(), "文件不存在", currentFunc.getTestSuiteName() + "不存在");
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
         * //显示测试用例库这个东西
         * outputTabFolder.setSelection(2);
         * System.out.println("yes");
         * }catch(Exception e)
         * {
         * RecordToLogger.recordExceptionInfo(e, logger);
         * if(Config.printExceptionInfoToConsole)
         * e.printStackTrace();
         * addOutputMessage("显示测试用例库时出现异常 " +e.getMessage());
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
         * addOutputMessage("导入测试用例库时出现异常 " +e.getMessage());
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
            String errMsg = "没有帮助文件可用！";
            MessageBox mb = WidgetFactory.createErrorMessageBox(demo.getShell(), "提示信息", errMsg);
            mb.open();
            RecordToLogger.recordExceptionInfo(e, logger);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
        } catch (InterruptedException e) {
            String errMsg = "没有帮助文件可用！";
            MessageBox mb = WidgetFactory.createErrorMessageBox(demo.getShell(), "提示信息", errMsg);
            mb.open();
            RecordToLogger.recordExceptionInfo(e, logger);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
        } catch (Exception e) {
            String errMsg = "没有帮助文件可用！";
            MessageBox mb = WidgetFactory.createErrorMessageBox(demo.getShell(), "提示信息", errMsg);
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
            MessageBox mb = WidgetFactory.createErrorMessageBox(demo.getShell(), "错误信息", "请对所有文件进行模块划分后再进行软件度量");
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
     * 用户选择目标覆盖元素窗口
     */
    public void doSelectTargetCoverageElement() {
        /*********************************************
         * 面向路径测试用例生成不支持复杂数据类型，暂时折中处理
         * 等都支持了，将该段去掉
         * 
         * */
        if (!demo.getCurrentFunc().isTestCaseSupportArgument(TestType.InputDomainBasedRandomTestAndPathBasedRandomTest)) {
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    MessageBox mb = WidgetFactory.createInfoMessageBox(demo.getShell(), "存在不支持的数据类型", "存在不支持的数据类型");
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
     * 人工干预测试用例生成
     */
    public void doManualIntervention() {
        // add by chenruolin to check whether the file has been modified
        if (demo.getSaveFile()) {
            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "提示信息", "请先保存文件");
            box.open();
            return;
        }
        UATManualInterventionGUI target = new UATManualInterventionGUI(demo);
        target.go();
    }

    /**
     * @author Cai Min
     *         回归测试
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
                        demo.setStatusBarInfo("正在对函数 " + funcName + "进行回归测试...");
                        addOutputMessage("正在对函数 " + funcName + "进行回归测试...");

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
                                demo.setStatusBarInfo("对函数 " + funcName + "进行回归测试结束");
                                addOutputMessage("对函数 " + funcName + "进行回归测试结束");

                                String msg;
                                if (type == RegressionTestReturnType.ModificationError)
                                    msg = "源代码编译出错，请确认修改";
                                else if (type == RegressionTestReturnType.NoModification)
                                    msg = "原代码未作修改，回归测试终止";
                                else
                                    msg = "测试用例集为空，回归测试终止";

                                MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "提示信息", msg);
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
                        demo.setStatusBarInfo("对函数 " + funcName + "进行回归测试结束");
                        addOutputMessage("对函数 " + funcName + "进行回归测试结束");
                        String info = "回归测试完成,是否查看测试报告？";
                        if (currentFunc.getFuncVar().hasFileVar())
                            info += "\n参数或全局变量中有文件指针，请用户自行校验测试结果";
                        MessageBox box = WidgetFactory.createQuesMessageBox(demo.getShell(), "提示信息", info);
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
        fd.setText("选择工程文件");
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
            if (!dialogTest.getCancle()) // 选择了取消按钮
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
                            progressDisplayGUI.setInfo("正在打开工程，请稍候...");
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
                                String info = "打开工程出异常" + "\n工程文件不存在";
                                addOutputMessage(info);
                                demo.setStatusBarInfo(info);
                                MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "打开工程出异常", info);
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
                                String info = "打开工程出异常";
                                addOutputMessage(info);
                                demo.setStatusBarInfo(info);
                                MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "打开工程出异常", info);
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
                                String info = "打开工程出异常" + "\n内存不足,堆栈溢出,请调整jvm的虚拟机参数";
                                addOutputMessage(info);
                                demo.setStatusBarInfo(info);
                                MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "打开工程出异常", info);
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
                                addOutputMessage("打开工程出异常");
                                demo.setStatusBarInfo("打开工程出异常");
                                MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "打开工程出异常", "打开工程出异常");
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
                            addOutputMessage("打开工程 " + pathName);
                            progressDisplayGUI.setTestProgressOver(0);
                        }
                    });
                }
            }.start();
        } catch (Exception e) {
            String errMsg = "打开工程时发生错误！";
            MessageBox mb = WidgetFactory.createErrorMessageBox(demo.getShell(), "错误信息", errMsg);
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
            tempProperties.store(outputFile, "最近打开工程配置文件");
            outputFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        // 刷新最近打开工程菜单
        demo.refreshCurrentProjectMenu();
    }

    /**
     * 为一个函数生成一个控制流图
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
                                if (size >= 3 * 1024 * 1024)// 超过10M的图片
                                {
                                    MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "图片太大", "生成的控制流图的图片太大\n,请到" + currentFunc.getCfgName() + "查看！！\n（选中窗口Ctrl+C,到记事本粘贴路径）");
                                    box.open();
                                } else {
                                    new ImageViewer(currentFunc.getCfgName());
                                    addOutputMessage("生成控制流图" + currentFunc.getFuncName());
                                }
                            }
                        });
                    } catch (IOException e) {
                        System.gc();
                        final String msg = e.getMessage();
                        Display.getDefault().syncExec(new Runnable() {
                            public void run() {
                                addOutputMessage("生成控制流图时出现错误： " + msg);
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
            logger.error("生成控制流图时发生异常");
            RecordToLogger.recordExceptionInfo(e, logger);
            addOutputMessage("生成控制流图时发生异常 " + e.getMessage());
        }
    }

    /**
     * 显示单个测试用例的函数覆盖情况
     * 
     * @param testCaseID 测试用例编号
     *        created by Yaoweichang on 2015-04-16 下午4:19:48
     */
    public void showOneCaseCoverageWindow(long testCaseID) {
        if (demo.getSaveFile()) {
            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "提示信息", "请先保存文件");
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
            addOutputMessage("显示覆盖率信息时发生异常");
        }
    }

    public void showCoverageWindow() {
        // add by chenruolin to check whether the file has been modified
        if (demo.getSaveFile()) {
            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "提示信息", "请先保存文件");
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
            addOutputMessage("显示覆盖率信息时发生异常");
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
            addOutputMessage("显示路径到代码映射窗口时发生异常");
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
            addOutputMessage("显示路径到代码映射窗口时发生异常");
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
        // 内存管理
        System.gc();
        // end add by xujiaoxian
    }

    /*
     * Add by za
     * 清除当前工程的所有信息
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
                                if (size >= 2 * 1024 * 1024)// 超过2M的图片
                                {
                                    MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "图片太大", "生成的简化控制流图的图片太大\n,请到" + currentFunc.getCfgName() + "查看！！\n（选中窗口Ctrl+C,到记事本粘贴路径）");
                                    box.open();
                                } else {
                                    new ImageViewer(currentFunc.getCfgName());
                                    addOutputMessage("生成了简化的控制流图 " + currentFunc.getCfgName());
                                }
                            }
                        });
                    } catch (Exception e) {
                        final String msg = e.getMessage();
                        Display.getDefault().syncExec(new Runnable() {
                            public void run() {
                                addOutputMessage("生成控制流图时出现错误： " + msg);
                            }
                        });
                        logger.error("生成控制流图时出现错误");
                        RecordToLogger.recordExceptionInfo(e, logger);
                        if (Config.printExceptionInfoToConsole)
                            e.printStackTrace();
                        return;
                    }


                }
            }.start();

        } catch (Exception e) {
            addOutputMessage("生成简化的控制流图时发生异常 " + e.getMessage());
            RecordToLogger.recordExceptionInfo(e, logger);
        }
    }

    /* 为一个文件生成调用图 */
    public void doGenerateCallGraph() {
        final AnalysisFile currentFile = demo.getCurrentFile();
        if (currentFile == null)
            return;
        try {
            demo.setStatusBarInfo("对文件 " + currentFile.getFile() + " 生成函数调用图");
            currentFile.dumpCallGraph();
            PictureViewer.showPicture(currentFile.getCallGraphPicName() + ".jpg", "文件" + currentFile.getFile() + "函数调用图");
            addOutputMessage("生成调用图 " + currentFile.getCallGraphPicName() + ".jpg");

        } catch (Exception e) {
            addOutputMessage("生成函数调用图时发生异常");
            logger.error("生成函数调用图时发生异常\n");
            RecordToLogger.recordExceptionInfo(e, logger);
            if (Config.printExceptionInfoToConsole)
                e.printStackTrace();
        }
    }


    /**
     * add by Cai Min, 2011/6/29
     * 参数设置
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
                        progressDisplayGUI.setInfo("正在生成路径，请稍候...");
                        clearOutputMessage();
                        demo.setStatusBarInfo("正在对函数 " + funcName + "在" + demo.getCurrentCoverCriteria().toString() + "下进行生成路径...");
                        addOutputMessage("正在对函数 " + funcName + "在" + demo.getCurrentCoverCriteria().toString() + "下进行生成路径...");
                    }

                });
                try {
                    testFunc.generateAllPath(cr);
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            progressDisplayGUI.setTestProgressOver(0);
                            demo.setStatusBarInfo("对函数 " + funcName + "在" + demo.getCurrentCoverCriteria().toString() + "下生成路径结束");
                            addOutputMessage("对函数 " + funcName + "在" + demo.getCurrentCoverCriteria().toString() + "下生成路径结束");
                            if (demo.getCurrentFunc().getAllpath().size() > 0) {
                                addOutputMessage("函数 " + funcName + "在" + demo.getCurrentCoverCriteria().toString() + "下生成路径有：");
                                List<OnePath> onepathlist = demo.getCurrentFunc().getAllpath();
                                int listsize = onepathlist.size();
                                for (int i = 0; i < listsize; i++) {
                                    addOutputMessage(onepathlist.get(i).toString());
                                }
                            } else {
                                addOutputMessage("函数 " + funcName + "在" + demo.getCurrentCoverCriteria().toString() + "下没有生成路径");
                            }

                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "提示信息", "对函数 " + funcName + "在" + demo.getCurrentCoverCriteria().toString() + "下生成路径完成");
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
                            MessageBox box = WidgetFactory.createInfoMessageBox(demo.getShell(), "测试时异常", "函数 " + funcName + "存在死循环，没有出口" + msg);
                            box.open();
                            addOutputMessage("函数 " + funcName + "存在死循环，没有出口" + msg);
                        }
                    });
                }
            }
        }.start();
    }

}
