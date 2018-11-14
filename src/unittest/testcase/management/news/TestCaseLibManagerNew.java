package unittest.testcase.management.news;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import unittest.module.seperate.TestModule;
import unittest.testcase.generate.paramtype.AbstractParamValue;
import unittest.testcase.generate.util.JsonUtil;
import unittest.testcase.generate.util.TestCaseNew;
import unittest.util.Config;
import experiment.MutationTesting.StubUtilsForMutationTesting;

public class TestCaseLibManagerNew {
    private static String projectTestCaseDB;

    private static DBinterfaceNew dba = DBinterfaceNew.getInstance();

    public static void setProjectTestCaseDB(String name) {
        projectTestCaseDB = name;
    }

    public static void clearAllData() {
        dba.clearData(projectTestCaseDB);
    }

    public static List<TestCaseNew> showAllTestCase(TestModule tm) {
        dba.openDataBase(projectTestCaseDB);
        return dba.showAllTestCase(tm);
    }

    /**
     * 获取选定用例树节点的用例值
     * 
     * @param tm 被测函数
     * @param testCaseID 用例节点编号
     * @return 用例对象
     *         created by Yaoweichang on 2015-04-17 下午3:24:16
     */
    public static TestCaseNew showOneTestCase(TestModule tm, long testCaseID) {
        dba.openDataBase(projectTestCaseDB);
        return dba.showOneTestCase(tm, testCaseID);
    }

    // add by chenruolin using for buglink test
    public static List<TestCaseNew> showAllTestCaseForBugLink(TestModule tm) {
        dba.openDataBase(projectTestCaseDB);
        return dba.showAllTestCaseForBugLink(tm);
    }

    public static List<TestCaseNew> showTestCaseByFuncName(String fullFuncName, TestModule tm) {
        dba.openDataBase(projectTestCaseDB);
        return dba.showTestCaseByFuncName(fullFuncName, tm);
    }

    /**
     * 将实际返回值填充到预期返回值
     * 
     * created by Yaoweichang on 2015-04-17 下午3:25:39
     */
    public static void replaceTestCase() {
        dba.openDataBase(projectTestCaseDB);
        dba.replaceTestCase();
    }

    public static long saveTestCase(TestCaseNew testcase) {
        long id;
        dba.openDataBase(projectTestCaseDB);
        id = dba.saveTestCase(testcase);
        dba.closeDataBase();
        testcase.setId(id);
        return id;
    }

    // add by chenruolin use for bug link test
    public static long saveTestCaseForBugLink(TestCaseNew testcase, String fileName, String dest_stub_path) {
        long id;
        dba.openDataBase(projectTestCaseDB);
        if (testcase.isHasRing()) {
            return -1;
        }
        id = dba.saveTestCaseForBugLink(testcase, fileName);
        dba.closeDataBase();

        // add by qgn 为变异实验，添加
        // 2013年10月10日
        // 此方法被 unittest.gui.UATTestCaseTable.createContents(List<TestCaseNew>)调用

        if (Config.IsMutationTesting) {
            // 从初始文件中取出每个用例对应的打桩文件
            try {
                // get the stub files
                String destdir = dest_stub_path + File.separator;// 是harness中temp文件夹：tm.getBelongToFile().getTestHarnessWorkDir()
                                                                 // + File.separator + "temp"
                String sourcedir = "D:\\temp_stub" + File.separator;
                if (new File(sourcedir).isDirectory()) {
                    // 创建目标文件夹
                    if (!(new File(destdir)).isDirectory()) {
                        (new File(destdir)).mkdirs();
                    }
                    // 获取源文件夹当前下的文件或目录
                    File[] file = (new File(sourcedir)).listFiles();
                    for (int i = 0; i < file.length; i++) {
                        if (file[i].isFile() && file[i].getName().contains(String.valueOf(testcase.getId()))) {
                            // 复制文件
                            String destfilename = destdir + file[i].getName().replaceAll(String.valueOf(testcase.getId()), String.valueOf(id));// 加一个id
                            copyFile(file[i], new File(destfilename));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                String message = "未成功取出桩文件\n" + e.getMessage();
                return id;
            }
        }

        testcase.setId(id);
        return id;
    }


    // add by chenruolin use for bug link test
    public static void saveTestCaseForBugLink(TestCaseNew testcase, String fileName) {
        dba.openDataBase(projectTestCaseDB);
        if (testcase.isHasRing()) {
            return;
        }
        dba.saveTestCaseForBugLink(testcase, fileName);
        dba.closeDataBase();
    }

    /**
     * 获得被测函数tm的编号为id的测试用例对象
     * 
     * @param id 用例编号
     * @param tm 被测函数
     * @return
     *         created by Yaoweichang on 2015-04-17 下午3:27:48
     */
    public static TestCaseNew getTestCaseById(long id, TestModule tm) {
        dba.openDataBase(projectTestCaseDB);
        TestCaseNew testcase = dba.getTestCaseById(id, tm);
        dba.closeDataBase();
        return testcase;
    }

    // 将控制台输出,文件操作的update也添加到此方法中
    public static void updateTestCase(TestModule tm, TestCaseNew tc) throws IOException {
        String ret = getReturnValue(tm);
        tc.setActualReturn(JsonUtil.json2AbstractParamValue(ret, tm.getReturnType()));// added by
                                                                                      // hanchunxiao,2012-6-13

        List<String> cOutputList = getStringListFromFile(tm.getBelongToFile().getConsoleOuputFile());
        tc.setConsoleOutput(cOutputList);
        List<String> fileInputList = getStringListFromFile(tm.getBelongToFile().getFileInputFile());
        tc.setFileInput(fileInputList);
        List<String> fileOutputList = getStringListFromFile(tm.getBelongToFile().getFileOutputFile());
        tc.setFileOutput(fileOutputList);
        List<String> socketSendList = getStringListFromFile(tm.getBelongToFile().getSocketSendDataFile());
        tc.setSocketSendData(socketSendList);
        List<String> socketRecList = getStringListFromFile(tm.getBelongToFile().getSocketRecDataFile());
        tc.setSocketRecData(socketRecList);

        dba.openDataBase(projectTestCaseDB);

        dba.updateTestCase(tc.getId(), "Tru_OutputValue", ret);
        dba.updateTestCase(tc.getId(), "Console_Output", JsonUtil.stringList2json(cOutputList));// add
                                                                                                // by
                                                                                                // yangyiwen
        dba.updateTestCase(tc.getId(), "File_Input", JsonUtil.stringList2json(fileInputList));
        dba.updateTestCase(tc.getId(), "File_Output", JsonUtil.stringList2json(fileOutputList));

        /**
         * + by Lin Huan to output stub_defs.h
         * at 2015.10.8
         */
        String stubdef = tm.getBelongToFile().getTestHarnessWorkDir() + File.separator + "_stub_defs_.h";
        String DBPath = TestCaseLibManagerNew.projectTestCaseDB;
        try {
            StubUtilsForMutationTesting.saveStubToDB(DBPath, stubdef, tc.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        /** End for Lin Huan's modification at 2015.10.8 */

        dba.updateTestCase(tc.getId(), "Socket_Send", JsonUtil.stringList2json(socketSendList));
        dba.updateTestCase(tc.getId(), "Socket_Rec", JsonUtil.stringList2json(socketRecList));
        dba.closeDataBase();
    }

    /**
     * 将控制台输出从文件中读取出来保存为字符串list
     * 
     * @author add by yangyiwen
     * @param 文件名
     * @return 字符串list
     */
    private static List<String> getStringListFromFile(String str) {
        FileReader fr = null;
        List<String> ret = new ArrayList<String>();
        String line;

        File file = new File(str);
        if (!file.exists())
            ret = null;
        else {
            try {
                fr = new FileReader(str);
                BufferedReader br = new BufferedReader(fr);
                while ((line = br.readLine()) != null)
                    if (!line.equals(""))
                        ret.add(line);
                br.close();
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    private static String getReturnValue(TestModule tm) {
        FileReader fr = null;
        String ret = "";


        File file = new File(tm.getBelongToFile().getTestHarnessWorkDir() + "Bin/ReturnValue.txt");
        if (!file.exists())
            ret = "null";
        else {
            try {
                fr = new FileReader(tm.getBelongToFile().getTestHarnessWorkDir() + "Bin/ReturnValue.txt");
                BufferedReader br = new BufferedReader(fr);
                ret = br.readLine();
                br.close();
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return ret;
    }

    public static void deleteTCforFile(String filename) {
        dba.openDataBase(projectTestCaseDB);
        dba.deleteTestCaseForOneFile(filename);
        dba.closeDataBase();
    }

    public static void addExpectedReturn(long id, AbstractParamValue param) {
        dba.openDataBase(projectTestCaseDB);
        dba.updateTestCase(id, "Exp_OutputValue", param.toJson());
        dba.closeDataBase();
    }

    public static long getMaxId() {
        dba.openDataBase(projectTestCaseDB);
        long id = dba.getMaxId();
        dba.closeDataBase();
        return id;
    }

    public static List<TestCaseNew> getNewTestCases(TestModule tm, long id) {
        dba.openDataBase(projectTestCaseDB);
        List<TestCaseNew> list = dba.getTestCasesWithID(tm, id);
        dba.closeDataBase();
        return list;
    }


    // add by qgn
    // 2013年10月10日 为变异测试实验，添加这个方法
    // 来源是若霖
    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        // 新建文件输入流并对它进行缓冲
        FileInputStream input = new FileInputStream(sourceFile);
        BufferedInputStream inBuff = new BufferedInputStream(input);

        // 新建文件输出流并对它进行缓冲
        FileOutputStream output = new FileOutputStream(targetFile);
        BufferedOutputStream outBuff = new BufferedOutputStream(output);

        // 缓冲数组
        byte[] b = new byte[1024 * 5];
        int len;
        while ((len = inBuff.read(b)) != -1) {
            outBuff.write(b, 0, len);
        }
        // 刷新此缓冲的输出流
        outBuff.flush();

        // 关闭流
        inBuff.close();
        outBuff.close();
        output.close();
        input.close();
    }

    /*
     * 拷贝文件，使用NIO
     * add by qgn
     * 2013年10月12日
     */

    public static void copyFileUsingFileChannels(File source, File dest) throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
    }
}
