package unittest.gui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;


public class SplitLog {
	static FileInputStream fis = null;
	static InputStreamReader isr;
	static BufferedReader br;
	
	public static void main(String[] args) {
		File file = new File("./自动化测试结果/日期滚动log/c.log");
		int i = 0;
		try {
			
			fis = new FileInputStream(file);
			isr = new InputStreamReader(fis,"UTF-8");
			br = new BufferedReader(isr);
			String line = null;
			StringBuilder sb = new StringBuilder();
			while((line=br.readLine()) != null) {
				if (line.trim().equals("************************************************************")) {
					i++;
					File file2 = new File("./自动化测试结果/日期滚动log/" + i + ".log");
					if(!file2.exists()) {
						file2.createNewFile();
					} else {
						file2.delete();
						try {
							file2.createNewFile();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file2), "UTF-8"));
					bw.write(sb.toString());
					bw.close();
					sb.delete(0, sb.toString().length());
					continue;
				} else {
					sb.append(line);
					sb.append("\r\n");
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		
	}
}
