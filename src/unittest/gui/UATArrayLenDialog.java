package unittest.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.util.Config;

/**
 * @author Cai Min
 *
 */
public class UATArrayLenDialog {
	private static Label lb;
	private final Text text;
	private Button okButton;
	private int len;
	public UATArrayLenDialog(final String arrName) {
		Display display = Display.getDefault();
		final Shell shell = new Shell(display);
		shell.setText("����");
		shell.setImage(Resource.UATImage);
		shell.setSize(240, 140);
		LayoutUtil.centerShell(display, shell);
		text = new Text(shell, SWT.None | SWT.BORDER);
		text.setBounds(69,39,100,25);
		text.setText(String.valueOf(Config.dimSize4varLenArr));
		text.selectAll();
		lb = new Label(shell, SWT.NONE);
		lb.setBounds(10,7,235,30);
		lb.setText("��Ϊ����" + arrName + "�趨���ȣ���Χ1~1000��");
		okButton = new Button(shell, SWT.NONE);
		okButton.setBounds(69,75,100,25);
		okButton.setText("ȷ��");
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e){
				try {
					len = Integer.parseInt(text.getText());
					if(len<1 || len>1000){
						MessageBox box = WidgetFactory.createMessageBox(shell.getShell(), SWT.ERROR, "����������", "���鳤�ȷ�ΧӦΪ[1,1000]!");
						box.open();
						return;
					}
				}
				catch(NumberFormatException ex) {
					MessageBox box = WidgetFactory.createMessageBox(shell.getShell(), SWT.ERROR, "����������", "�Ƿ�����");
					box.open();
					return;
				}
				shell.dispose();
			}
		});
		
		//shell.pack();
		shell.open();
		while(!shell.isDisposed()){
			if(!display.readAndDispatch())
				display.sleep();
		}
		//display.dispose();
	}

	public int getLen(){
		return len;
	}
	
	/*public static void main(String[] args) {
		new UATArrayLenDialog("la");
		
	}*/
}
