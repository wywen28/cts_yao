package unittest;

import java.io.File;
import java.io.IOException;

import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import unittest.gui.UATGUI;
import unittest.util.Config;
import unittest.util.ExcelOperation;
//import org.longmai.Smart2000.Smart2000Lib;

/**
 * <p>
 * The interface entry of UAT
 * </p>
 * 
 * @author Sun Huajin ,20100706
 * @version 3.0
 */

public class UATGUITest {

	
	public static ExcelOperation  excelOperation=null;
	/**
	 * 日志类，在每一个类的前面声明这么一个静态对象，然后在类的动作中记录相应的动作 或状态的改变
	 */
	private static Logger logger = Logger.getLogger(UATGUITest.class);

	public static void main(String[] args) {
		/* 配置log4j */
//		File logDir = new File(System.getProperty("user.dir") + File.separator + "自动化测试结果" + File.separator + "大小滚动log");
		//delete the former log files;    add by chenruolin
//		File[] fs = logDir.listFiles();
//		if (fs == null)
//			logDir.mkdir();
//		else {
//			fs = logDir.listFiles();
//			int len = fs.length;
//			for (int i = 0; i < len; i++) {
//				fs[i].delete();
//			}
//		}
		//create new log files
//		if(!logDir.exists())
//			logDir.mkdir();
		
//		File logDir1 = new File(System.getProperty("user.dir") + File.separator + "autotestResult" + File.separator + "RollingFileLog");
		//delete the former log files;    add by chenruolin
//		File[] fs1 = logDir1.listFiles();
//		if (fs1 == null)
//			logDir1.mkdir();
//		else {
//			fs1 = logDir1.listFiles();
//			int len = fs1.length;
//			for (int i = 0; i < len; i++) {
//				fs1[i].delete();
//			}
//		}
		//create new log files
//		if(!logDir1.exists())
//			logDir1.mkdir();
//		
		
		
		try {
			excelOperation=new ExcelOperation();
			excelOperation.fillExcelHeadForNewFile();
		} catch (RowsExceededException e) {
		 
			e.printStackTrace();
		} catch (WriteException e) {
		 
			e.printStackTrace();
		}
		String dirString1 = System.getProperty("user.dir") + File.separator + "自动化测试结果" + File.separator + "大小滚动log";
		File dirFile = new File(dirString1);
		if (!dirFile.exists() && !dirFile.isDirectory()) {
			dirFile.mkdirs();
		}
		String outString = dirString1 + File.separator + "c.log";
		File out = new File(outString);
		if(!out.exists())
			try {
				out.createNewFile();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		String dirString2 = System.getProperty("user.dir") + File.separator + "自动化测试结果" + File.separator + "日期滚动log";
		File dirFile1 = new File(dirString2);
		if (!dirFile1.exists() && !dirFile1.isDirectory()) {
			dirFile1.mkdirs();
		}
		String outString2 = dirString2 + File.separator + "c.log";
		File out1 = new File(outString2);
		if(!out1.exists())
			try {
				out1.createNewFile();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j.properties");
		
		String os = System.getProperty("os.name");
		if(os.equals("Linux"))
			Config.os = os;
		
//		logger.info("***********CTS 1.0 update to date: 2010/07/06***********");
//		Smart2000Lib mydog = Smart2000Lib.getInstance();
//		String appID = "MyApplication";;
//		int[] keyHandles = new int[8];
//		int[] keyNumber = new int[1];
//		int myflag = mydog.Find(appID, keyHandles, keyNumber);
//		byte[] UID = new byte[33];
//		mydog.GetUid(keyHandles[0], UID);
//		
//		if(myflag == 0) {
//			String string = null;
//			try {
//				string = new String(UID, "UTF-8");
//				if (!string.trim().equals("77e456a785528b750cf150aa669b1988")) {
//					logger.info("错误码: " + string + mydog.GetLastError() + " DogsId= " + appID + " myKeyHandles= " + keyHandles[0] + "    myKeyNumber= " + keyNumber[0]);
//					Display display = new Display();
//					Shell shell = new Shell(display);
//					MessageBox box = WidgetFactory.createErrorMessageBox(shell, "错误", "MD5不正确");
//					box.open();
//					System.exit(0);
//				}else {
//					final UATGUI uat = new UATGUI();
//					uat.go(args);
//				}
//			} catch (UnsupportedEncodingException e) {
//				// TODO: handle exception
//				e.printStackTrace();
//			}
//		}else {
//			logger.info("错误码: " + mydog.GetLastError() + " DogsId= " + appID + " myKeyHandles= " + keyHandles[0] + "    myKeyNumber= " + keyNumber[0]);
//			Display display = new Display();
//			Shell shell = new Shell(display);
//			MessageBox box = WidgetFactory.createErrorMessageBox(shell, "错误", "没插加密狗");
//			box.open();
//			System.exit(0);
//		}
		/* GUI */
		final UATGUI uat = new UATGUI();
		
		uat.go(args);

	}
}
