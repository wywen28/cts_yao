package unittest.testcase.management.news;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import unittest.module.seperate.TestModule;
import unittest.testcase.generate.util.JsonUtil;
import unittest.testcase.generate.util.TestCaseNew;
import unittest.util.Config;
import unittest.util.CoverRule;
import unittest.util.FileOperation;

import com.google.gson.Gson;

public class DBinterfaceNew {

    private static Logger logger = Logger.getLogger(DBinterfaceNew.class);

    public static String TEMPLATE_MDB_PATH = ".\\set\\TestCaseLibTemplate.mdb";
    public static String TEMPLATE_DB_PATH = "." + File.separator + "set" + File.separator + "TestCaseLibTemplate.db";// linux

    /** 数据库连接 */
    private Connection dbcon;
    private static DBinterfaceNew instance;

    DBinterfaceNew() {}

    public static DBinterfaceNew getInstance() {
        if (instance == null) {
            instance = new DBinterfaceNew();
        }
        return instance;
    }

    public void openDataBase(String dbName) {
        if (dbcon == null) {
            if (Config.os.equals("windows")) {
                dbcon = openDataBase(dbName, TEMPLATE_MDB_PATH);
            } else {
                dbcon = openDataBase(dbName, TEMPLATE_DB_PATH);
            }


        }
    }

    /** 打开数据库连接 */
    public Connection openDataBase(String dbName, String templateName) {
        Connection conn;
        File file = new File(dbName);
        String driver;
        String url;
        if (!file.exists()) {
            FileOperation.createMdbFile(templateName, dbName);
        }
        if (Config.os.equals("windows")) {
            driver = "sun.jdbc.odbc.JdbcOdbcDriver";
            url = "jdbc:odbc:DRIVER=Microsoft Access Driver (*.mdb);DBQ=" + dbName;

        } else {
            driver = "org.sqlite.JDBC";
            url = "jdbc:sqlite:" + dbName;
        }

        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url, "", "");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Access database driver error", ex);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Access database connect error");
        }
        return conn;
    }

    /** 关闭数据库连接 */
    public void closeDataBase() {
        if (dbcon != null) {
            try {
                dbcon.close();
            } catch (SQLException ex) {
                // ex.printStackTrace();
                throw new RuntimeException("Access database connect error", ex);
            }
        }
        dbcon = null;
    }

    /** 清空mdb文件中所有记录 */
    public void clearData(String pathname) {
        String driver;
        String url;
        if (Config.os.equals("windows")) {
            driver = "sun.jdbc.odbc.JdbcOdbcDriver";
            url = "jdbc:odbc:DRIVER=Microsoft Access Driver (*.mdb);DBQ=" + pathname;
        } else {
            driver = "org.sqlite.JDBC";
            url = "jdbc:sqlite:" + pathname;
        }

        Connection conn = null;
        Statement stmt = null;

        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url, "", "");
            stmt = conn.createStatement();
            String sql = "delete * from TestCaseLib";
            stmt.executeUpdate(sql);

        } catch (ClassNotFoundException ex) {
            closeDataBase();
            throw new RuntimeException("Access database driver error", ex);
        } catch (SQLException ex) {
            throw new RuntimeException("Access database connect error", ex);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    throw new RuntimeException("Access database connect error", ex);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    throw new RuntimeException("Access database connect error", ex);
                }
            }
        }
    }


    /**
     * 向数据库输出一条记录 参数含义：函数名，输入参数，全局变量，输出类型，期待输出，实际输出
     */

    public void saveTestCase(String fileName, String fName, String pInput, String gVariable, String expOutput, String truOutput, String gloResult, String vparamResult, String cr, String path,
            String expectedcOutput, String cOutput, String fileInput, String fileOutput, String socketSend, String socketRec) {
        Statement select = null;
        PreparedStatement pstmt = null;
        if (dbcon == null) {
            throw new RuntimeException("database connection closed.");
        }
        try {
            String strSQL = "";
            // 预编译指令

            strSQL =
                    "insert into TestCaseLib (Func_Name,Param_Input,Glo_Variable,Exp_OutputValue,Tru_OutputValue,Glo_Result,Vparam_Result,CoverRule,FILE_NAME,Path,ExpConsole_Output,Console_Output,File_Input,File_Output,Socket_Send,Socket_Rec) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

            pstmt = dbcon.prepareStatement(strSQL);
            // 设置插入的值，包括函数名，输入参数，全局变量，输出类型，期望输出，实际输出
            pstmt.setString(1, fName);
            pstmt.setString(2, pInput);
            pstmt.setString(3, gVariable);
            pstmt.setString(4, expOutput);
            pstmt.setString(5, truOutput);
            pstmt.setString(6, gloResult);
            pstmt.setString(7, vparamResult);
            pstmt.setString(8, cr);
            pstmt.setString(9, fileName);
            pstmt.setString(10, path);
            pstmt.setString(11, expectedcOutput);
            pstmt.setString(12, cOutput);
            pstmt.setString(13, fileInput);
            pstmt.setString(14, fileOutput);
            pstmt.setString(15, socketSend);
            pstmt.setString(16, socketRec);
            pstmt.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
            closeDataBase();
            throw new RuntimeException("Access database connect error", ex);
        } finally {
            if (select != null) {
                try {
                    select.close();
                } catch (SQLException ex) {
                    closeDataBase();
                    throw new RuntimeException("Access database connect error", ex);
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException ex) {
                    closeDataBase();
                    throw new RuntimeException("Access database connect error", ex);
                }
            }
        }
    }

    public long saveTestCase(TestCaseNew testcase) {
        String t1 = JsonUtil.abstractParamValue2Json(testcase.getExpectReturn());
        String t2 = JsonUtil.abstractParamValue2Json(testcase.getActualReturn());
        String t3 = JsonUtil.list2Json(testcase.getGlobalParamOutList());
        String t4 = JsonUtil.list2Json(testcase.getFuncParamOutList());
        String t5 = new Gson().toJson(testcase.getCoverRule());
        String t6 = testcase.getPathInString();
        this.saveTestCase(testcase.getFileName(), testcase.getFuncName(), JsonUtil.list2Json(testcase.getFuncParamList()), JsonUtil.list2Json(testcase.getGlobalParamList()), t1, t2, t3, t4, t5, t6,
                JsonUtil.stringList2json(testcase.getExpectedConsoleOutput()), JsonUtil.stringList2json(testcase.getConsoleOutput()), JsonUtil.stringList2json(testcase.getFileInput()),
                JsonUtil.stringList2json(testcase.getFileOutput()), JsonUtil.stringList2json(testcase.getSocketSend()), JsonUtil.stringList2json(testcase.getSocketRec()));
        PreparedStatement pstmt = null;
        long id = -1L;
        try {
            if (Config.os.equals("windows")) {
                pstmt = dbcon.prepareStatement("select @@IDENTITY as id");
            } else {// changed by xujiaoxian 2012-10-22
                pstmt = dbcon.prepareStatement("select last_insert_rowid() as id");
            }
            ResultSet result = pstmt.executeQuery();
            while (result.next()) {
                id = result.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }

    // add by chenruolin use for bug link test
    public long saveTestCaseForBugLink(TestCaseNew testcase, String fileName) {
        // exchange the expected return value with the actual return value
        String t2 = JsonUtil.abstractParamValue2Json(testcase.getExpectReturn());
        String t1 = JsonUtil.abstractParamValue2Json(testcase.getActualReturn());
        String t3 = JsonUtil.list2Json(testcase.getGlobalParamOutList());
        String t4 = JsonUtil.list2Json(testcase.getFuncParamOutList());
        String t5 = new Gson().toJson(testcase.getCoverRule());
        String t6 = testcase.getPathInString();
        testcase.setFileName(fileName);
        this.saveTestCase(testcase.getFileName(), testcase.getFuncName(), JsonUtil.list2Json(testcase.getFuncParamList()), JsonUtil.list2Json(testcase.getGlobalParamList()), t1, t2, t3, t4, t5,
                t6,
                JsonUtil.stringList2json(testcase.getConsoleOutput()),// add by chenruolin
                JsonUtil.stringList2json(testcase.getExpectedConsoleOutput()),// add by yangyiwen
                JsonUtil.stringList2json(testcase.getFileInput()), JsonUtil.stringList2json(testcase.getFileOutput()), JsonUtil.stringList2json(testcase.getSocketSend()),
                JsonUtil.stringList2json(testcase.getSocketRec()));
        PreparedStatement pstmt = null;
        long id = -1L;
        try {
            if (Config.os.equals("windows")) {
                pstmt = dbcon.prepareStatement("select @@IDENTITY as id");
            } else {// changed by xujiaoxian 2012-10-22
                pstmt = dbcon.prepareStatement("select last_insert_rowid() as id");
            }
            ResultSet result = pstmt.executeQuery();
            while (result.next()) {
                id = result.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }


    /**
     * 回归测试时使用，将程序修改前的用例实际返回值写到预期返回值
     * 
     * created by Yaoweichang on 2015-04-17 下午3:13:56
     */
    public void replaceTestCase() {
        String strSQL = "Select * from TestCaseLib";
        PreparedStatement pstmt = null;
        try {
            pstmt = dbcon.prepareStatement(strSQL);
            ResultSet result = pstmt.executeQuery();

            while (result.next()) {
                // 更新预期返回值
                updateTestCase(result.getLong("ID"), "Exp_OutputValue", result.getString("Tru_OutputValue"));
                // 更新预期控制台输出
                updateTestCase(result.getLong("ID"), "ExpConsole_Output", result.getString("Console_Output"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("连接数据库文件出错", e);
        } finally {
            closeDataBase();
        }
    }

    public List<TestCaseNew> showTestCaseByFuncName(String name, TestModule tm) {
        String strSQL = "SELECT * FROM TestCaseLib WHERE Func_Name='" + name + "' and file_name='" + tm.getBelongToFile().getConsoleAlteredFile() + "'";
        return showTestCaseBySQL(strSQL, tm);
    }


    public List<TestCaseNew> showAllTestCase(TestModule tm) {
        String strSQL = "SELECT * FROM TestCaseLib where func_name='" + tm.getUniqueFuncName() + "' and file_name ='" + tm.getBelongToFile().getConsoleAlteredFile() + "'";
        return showTestCaseBySQL(strSQL, tm);
    }

    /**
     * 获得被测函数tm的编号为testcaseid的测试用例对象
     * 
     * @param tm 被测函数
     * @param testCaseID 用例编号
     * @return
     *         created by Yaoweichang on 2015-04-17 下午3:16:26
     */
    public TestCaseNew showOneTestCase(TestModule tm, long testCaseID) {
        String strSQL = "SELECT * FROM TestCaseLib where func_name='" + tm.getUniqueFuncName() + "' and file_name ='" + tm.getBelongToFile().getConsoleAlteredFile() + "' and id = " + testCaseID;
        return showOneTestCaseBySQL(strSQL, tm);
    }

    // add by chenruolin using for buglink test
    public List<TestCaseNew> showAllTestCaseForBugLink(TestModule tm) {
        String sourceFileName = tm.getBelongToFile().getFile();
        int loc = sourceFileName.indexOf("_fault");
        sourceFileName = sourceFileName.substring(0, loc) + ".c";
        String strSQL = "SELECT * FROM TestCaseLib where func_name='" + tm.getUniqueFuncName() + "' and file_name ='" + sourceFileName + "'";
        return showTestCaseBySQL(strSQL, tm);
    }

    /**
     * 执行sql语句获得一条用例记录
     * 
     * @param strSQL
     * @param tm
     * @return
     *         created by Yaoweichang on 2015-04-17 下午3:17:50
     */
    public TestCaseNew showOneTestCaseBySQL(String strSQL, TestModule tm) {
        Statement select = null;
        PreparedStatement pstmt = null;
        try {
            pstmt = dbcon.prepareStatement(strSQL);
            ResultSet result = pstmt.executeQuery();

            while (result.next()) {
                return this.buildTestCase(result, tm);
            }
            closeDataBase();
        } catch (SQLException e) {
            e.printStackTrace();
            closeDataBase();
            throw new RuntimeException("连接数据库文件出错", e);
        } finally {
            closeDataBase();
            if (select != null) {
                try {
                    select.close();
                } catch (SQLException ex) {
                    closeDataBase();
                    throw new RuntimeException("Access database connect error", ex);
                }
            }
        }
        return null;
    }

    public List<TestCaseNew> showTestCaseBySQL(String strSQL, TestModule tm) {
        List<TestCaseNew> list = new ArrayList<TestCaseNew>();
        Statement select = null;
        PreparedStatement pstmt = null;
        try {
            pstmt = dbcon.prepareStatement(strSQL);
            ResultSet result = pstmt.executeQuery();

            while (result.next()) {
                list.add(this.buildTestCase(result, tm));
            }
            closeDataBase();
        } catch (SQLException e) {
            e.printStackTrace();
            closeDataBase();
            throw new RuntimeException("连接数据库文件出错", e);
        } finally {
            if (select != null) {
                try {
                    select.close();
                } catch (SQLException ex) {
                    closeDataBase();
                    throw new RuntimeException("Access database connect error", ex);
                }
            }
        }

        return list;
    }

    private TestCaseNew buildTestCase(ResultSet rs, TestModule tm) throws SQLException {
        TestCaseNew testcase = new TestCaseNew();
        testcase.setId(rs.getLong("ID"));
        testcase.setFileName(rs.getString("FILE_NAME"));
        testcase.setFuncName(rs.getString("FUNC_NAME"));
        testcase.setFuncParamList(JsonUtil.paramJson2List(rs.getString("PARAM_INPUT"), tm));
        testcase.setFuncParamOutList(JsonUtil.paramJson2List(rs.getString("Vparam_Result"), tm));
        testcase.setGlobalParamList(JsonUtil.json2List(rs.getString("GLO_VARIABLE"), tm));
        // testcase.setGlobalParamOutList(JsonUtil.json2List(rs.getString("Glo_Result")));
        testcase.setActualReturn(JsonUtil.json2AbstractParamValue(rs.getString("Tru_OutputValue"), tm.getReturnType()));
        testcase.setExpectReturn(JsonUtil.json2AbstractParamValue(rs.getString("Exp_OutputValue"), tm.getReturnType()));
        testcase.setCoverRule(new Gson().fromJson(rs.getString("CoverRule"), CoverRule.class));
        testcase.setPathInString(rs.getString("Path"));
        // testcase.setReturnType((CType) SerializeUtil.readObject(rs.getString("output_type")));
        testcase.setExpectedConsoleOutput(JsonUtil.json2StringList(rs.getString("ExpConsole_Output")));
        testcase.setConsoleOutput(JsonUtil.json2StringList(rs.getString("Console_Output")));
        testcase.setFileInput(JsonUtil.json2StringList(rs.getString("File_Input")));
        testcase.setFileOutput(JsonUtil.json2StringList(rs.getString("File_Output")));
        testcase.setSocketSendData(JsonUtil.json2StringList(rs.getString("Socket_Send")));
        testcase.setSocketRecData(JsonUtil.json2StringList(rs.getString("Socket_Rec")));

        return testcase;
    }

    public TestCaseNew getTestCaseById(long id, TestModule tm) {
        String sql = "SELECT * FROM TestCaseLib WHERE id=" + id;
        List<TestCaseNew> testcaseList = this.showTestCaseBySQL(sql, tm);
        if (testcaseList.size() > 0) {
            return testcaseList.get(0);
        }
        return null;
    }

    // 根据id更新数据库中的测试用例,id:TestCase的id， field指定字段，value是字段的值
    public void updateTestCase(long id, String field, String value) {
        Statement select = null;
        PreparedStatement pstmt = null;
        if (dbcon == null) {
            throw new RuntimeException("database connection closed.");
        }
        try {
            String strSQL = "update TestCaseLib set " + field + " = (?) where id = (?)";
            pstmt = dbcon.prepareStatement(strSQL);
            pstmt.setString(1, value);
            pstmt.setString(2, String.valueOf(id));
            pstmt.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
            closeDataBase();
            throw new RuntimeException("Access database connect error", ex);
        } finally {
            if (select != null) {
                try {
                    select.close();
                } catch (SQLException ex) {
                    closeDataBase();
                    throw new RuntimeException("Access database connect error", ex);
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException ex) {
                    closeDataBase();
                    throw new RuntimeException("Access database connect error", ex);
                }
            }
        }
    }

    // 从dbInterface里移植过来的方法，修改2011-9-20By唐容
    public void deleteTestCaseForOneFile(String filename) {
        // String strSQL="DELETE * FROM TestCaseLib WHERE FILE_NAME='"+filename+"'";
        String strSQL = "DELETE  FROM TestCaseLib WHERE FILE_NAME='" + filename + "'";
        PreparedStatement pstmt = null;
        if (dbcon == null) {
            throw new RuntimeException("database connection closed.");
        }

        try {
            pstmt = dbcon.prepareStatement(strSQL);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            closeDataBase();
            throw new RuntimeException("Access database connect error", e);
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException ex) {
                    closeDataBase();
                    throw new RuntimeException("Access database connect error", ex);
                }
            }
        }
    }

    /**
     * @author Cai Min
     *         测试用例库中最大id
     * @return
     */
    public long getMaxId() {
        PreparedStatement pstmt;
        long id = -1;
        try {
            pstmt = dbcon.prepareStatement("select MAX(id) FROM TestCaseLib");
            ResultSet result = pstmt.executeQuery();
            while (result.next()) {
                id = result.getLong(1);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return id;
    }

    /**
     * @author Cai Min
     *         找出id>给定id的测试用例
     */
    public List<TestCaseNew> getTestCasesWithID(TestModule tm, long id) {
        String strSQL = "SELECT * FROM TestCaseLib where id > " + id;
        return showTestCaseBySQL(strSQL, tm);
    }
}
