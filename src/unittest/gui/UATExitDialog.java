package unittest.gui;

import java.io.File;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.sun.istack.internal.FinalArrayList;

import unittest.gui.helper.Resource;
import unittest.util.Config;

/**
 * 
 * @author Chen Ruolin
 * �˳�ϵͳ��ȷ�϶Ի���
 */
public class UATExitDialog {
	private UATGUI demo;
	private Shell shell;
	private Display display;
	boolean exit = true;
	boolean shellHasClosed = false;		//�жϴ����Ƿ��Ѿ���close����ֹ���ڼ��������ظ����������쳣
	int usage = 1;	//������;��1Ϊ�˳�ϵͳ��ʾ���ڣ�2Ϊ�رչ�����ʾ����
	
	/**
	 * @wbp.parser.entryPoint
	 */
	public UATExitDialog( UATGUI uatGui ) 
	{
		this.demo = uatGui;
	}
	
	/**
	 * @wbp.parser.entryPoint
	 */
	
	public boolean getCancle() {
		return exit;
	}
	
	public void setUsage(int value) {
		usage = value;
	}
	
	public void go() {
		if (!Config.needSavePro && usage == 1){
			System.exit( 0 );
		}
		if (Config.rememberChoice.equals("forget")){//rememberChoice == forget ����¼�Ƿ񱣴湤��
			if (!Config.needSavePro)
				demo.actionsGUI.doCloseProject();
			else
				CreateShell();
		}
		else if (Config.rememberChoice.equals("no")){//rememberChoice == no ��ס��ѡ��Ϊ���˳�ǰ�����湤�̡�
			demo.getCurrentProject().clean();
			if (usage == 1){
				exit = true;
				System.exit( 0 );
			}
			else {
				demo.actionsGUI.doCloseProject();
				//shell.close();
			}
		}
		else {			//rememberChoice == yes ��ס��ѡ��Ϊ���˳�ǰ���湤�̡�
			saveProject();
			if (usage == 1)
				exit = true;
			else {
				demo.actionsGUI.doCloseProject();
				//shell.close();
			}
		}
		
	}
	
	public void CreateShell(){
		display = Display.getDefault();
		shell = new Shell(display, SWT.CLOSE|SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL | SWT.MAX | SWT.MIN); //edited by Cai Min. ��С���ɱ༭
		shell.setSize(320, 160);
		if (usage == 1)
			shell.setText("�˳�ϵͳ");
		else
			shell.setText("�رչ���");
		shell.setImage(Resource.UATImage);
		
		LayoutUtil.centerShell(display, shell);
		
		Label lblNewLabel = new Label(shell, SWT.NONE);
		lblNewLabel.setImage(display.getSystemImage(SWT.ICON_QUESTION));
		lblNewLabel.setBounds(29, 15, 33, 32);
		
		Label label = new Label(shell, SWT.NONE);
		//label.setFont(SWTResourceManager.getFont("����", 10, SWT.NORMAL));
		label.setBounds(86, 25, 196, 22);
		if (usage == 1)
			label.setText("�˳�ǰ�Ƿ񱣴湤�̣�");
		else 
			label.setText("�ر�ǰ�Ƿ񱣴湤�̣�");
		
		final Button button_2 = new Button(shell, SWT.CHECK);
		button_2.setBounds(10, 100, 154, 16);
		button_2.setText("��סѡ�񣬲�����ʾ");
		
		Button yesButton = new Button(shell, SWT.NONE);
		yesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (button_2.getSelection()){
					Config.rememberChoice = "yes";
					demo.getSystemConfigManager().storeCofig();
				}
				saveProject();
				if (usage == 1)
					exit = true;
				else{
					demo.actionsGUI.doCloseProject();
					shellHasClosed = true;
					shell.close();
				}
			}
		});
		yesButton.setBounds(10, 65, 72, 22);
		yesButton.setText("��");
		
		Button noButton = new Button(shell, SWT.NONE);
		noButton.setBounds(113, 65, 72, 22);
		noButton.setText("��");
		noButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (button_2.getSelection()){
					Config.rememberChoice = "no";
					demo.getSystemConfigManager().storeCofig();
				}
				demo.getCurrentProject().clean();
				if (usage == 1){
					exit = true;
					System.exit( 0 );
				}
				else {
					demo.actionsGUI.doCloseProject();
					shellHasClosed = true;
					shell.close();
				}
			}
		});
		
		Button cancelButton = new Button(shell, SWT.NONE);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exit = false;
				shellHasClosed = true;
				shell.close();
			}
		});
		cancelButton.setBounds(216, 65, 72, 22);
		cancelButton.setText("ȡ��");
		
		shell.open();
		shell.layout();
		dealEvent();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
	public void dealEvent() 
	{
		shell.addShellListener( new ShellAdapter() 
		{
			public void shellClosed( ShellEvent e ) 
			{
				//exit = false;
				if (!shellHasClosed){
					shellHasClosed = true;
					shell.close();
				}
			}
		});
	}
	
	public void saveProject(){
		try 
		{
			Config.lastProjectPath = demo.getCurrentProject().getPath().substring(0, demo.getCurrentProject().getPath().lastIndexOf(File.separator));
			
			demo.getSystemConfigManager().storeCofig();
			demo.getCurrentProject().save();
		} catch (IOException e1) 
		{
			demo.getLogger().error(e1.getMessage() + e1.getStackTrace());
			if(Config.printExceptionInfoToConsole)
				e1.printStackTrace();
		}
		if (usage == 1)
			System.exit( 0 );
	}
}
