package unittest.gui;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Text;

import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.localization.GUILanguageResource;
import unittest.testcase.management.news.DBinterfaceNew;

/**
 * @author Cai Min
 * 
 */
public class UATProgressDisplayGUI {
	private static Logger logger = Logger.getLogger(DBinterfaceNew.class);
	private UATGUI uatgui;
	private Composite parent;
	private Composite idleComposite;
	private Composite busyComposite;
	private TabFolder outputTabFolder;
	
	public CLabel infoLabel;
	public ProgressBar testProgressBar = null;
	public ProgressIndicator testProgressIndicator = null;
	public Button testTerminateButton = null;
	public TerminateButtonListener terminateListener = null;
	public CLabel progressCLabel = null;

	public UATProgressDisplayGUI(UATGUI demo, Composite parent,
			TabFolder outputTabFolder,int tabNum) {
		this.uatgui = demo;
		this.parent = parent;
		this.outputTabFolder = outputTabFolder;
		createContent(tabNum);
	}

	private void createContent(int tabNum) {
		createBusyComposite(tabNum);
		createIdleComposite();
		busyComposite.setVisible(false);
		idleComposite.setVisible(true);
	}
	
	public void setInfo(String info) {
		infoLabel.setText(info);
	}

	private void createBusyComposite(int tabNum) {
		busyComposite = WidgetFactory.createComposite(parent, SWT.BORDER);
		busyComposite.setLayout(new FormLayout());

		WidgetFactory.configureFormData(busyComposite,
				new FormAttachment(0, 0), new FormAttachment(0, 0),
				new FormAttachment(100, 0), new FormAttachment(100, 0));
		
		infoLabel = WidgetFactory.createCLabel(busyComposite, SWT.NONE, "                                         ");
		WidgetFactory.configureFormData(infoLabel, new FormAttachment(0,450), new FormAttachment(50, -30), null, null);

		progressCLabel = WidgetFactory.createCLabel(busyComposite, SWT.NONE,
				GUILanguageResource.getProperty("TestProgressLabel"));

		WidgetFactory.configureFormData(progressCLabel, new FormAttachment(20,
				100, 0), new FormAttachment(50, 0), null, null);


		testProgressIndicator = new ProgressIndicator(busyComposite);
		testProgressIndicator.setForeground(Resource.progressBarColor);

		WidgetFactory
				.configureFormData(testProgressIndicator, new FormAttachment(
						progressCLabel, 0), new FormAttachment(50, 0),
						new FormAttachment(80, 100, 0), null);

		testTerminateButton = WidgetFactory.createButton(busyComposite,
				SWT.PUSH);
		testTerminateButton.setImage(Resource.stopImage);
		WidgetFactory.configureFormData(testTerminateButton,
				new FormAttachment(testProgressIndicator, 5),
				new FormAttachment(50, -3), null, null);
		terminateListener = new TerminateButtonListener();
		testTerminateButton.addSelectionListener(terminateListener);

		testTerminateButton.setEnabled(false);

		setTestProgressInit(tabNum);
	}

	private void createIdleComposite() {
		idleComposite = WidgetFactory.createComposite(parent, SWT.BORDER);
		idleComposite.setLayout(new FillLayout());

		WidgetFactory.configureFormData(idleComposite,
				new FormAttachment(0, 0), new FormAttachment(0, 0),
				new FormAttachment(100, 0), new FormAttachment(100, 0));
		Text label = WidgetFactory.createText(idleComposite, SWT.READ_ONLY
				| SWT.H_SCROLL | SWT.V_SCROLL);
		label.setText("目前没有操作");
	}

	public class TerminateButtonListener extends SelectionAdapter {
		private Thread demo;

		public TerminateButtonListener(Thread t) {
			this.demo = t;
		}

		public TerminateButtonListener() {
			this.demo = null;
		}

		public void setRunningThread(Thread t) {
			if (t != null) {
				this.demo = t;
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {

						testTerminateButton.setEnabled(true);

					}

				});
			} else {
				this.demo = t;
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {

						testTerminateButton.setEnabled(false);

					}

				});
			}
		}

		public void widgetSelected(SelectionEvent e) {
			if (this.demo == null)
				return;
			this.demo.stop();
			testTerminateButton.setEnabled(false);
			setTestProgressOver(1);
			outputTabFolder.setSelection(1);
			this.demo = null;
			uatgui.setStatusBarInfo("测试线程结束,操作被人为终止");
			uatgui.getoutputMessageText().setText("测试线程结束,操作被人为终止");
			logger.info("测试线程结束,操作被人为终止");
			MessageBox box = WidgetFactory.createMessageBox(parent.getShell(),
					SWT.ICON_QUESTION | SWT.OK, "测试线程结束", "操作被人为终止");
			box.open();

		}

	}
	
	/**
	 * 初始化时默认选择的选项卡,主界面上和人工辅助界面上不一样
	 */
	public void setTestProgressInit(int tabNum) {
		outputTabFolder.setSelection(tabNum);
		/*
		 * this.testProgressBar.setMinimum( 0 );
		 * this.testProgressBar.setMaximum( 0 );
		 * this.testProgressBar.setSelection( 0 );
		 * this.testProgressBar.setToolTipText( ""+ 0 + "%" );
		 */
		testProgressIndicator.beginTask(0);
	}

	public void setTestProgressOver(int tabNum) {
		busyComposite.setVisible(false);
		idleComposite.setVisible(true);
		// 调转到覆盖率的界面
		outputTabFolder.setSelection(tabNum);
		testProgressIndicator.done();
		testProgressIndicator.beginTask(0);

	}
	
	/**
	 * 正在运行时默认选择的选项卡，主界面上和人工辅助界面上不一样
	 */
	public void setTestProgressRunning(int tabNum) {
		busyComposite.setVisible(true);
		idleComposite.setVisible(false);
		outputTabFolder.setSelection(tabNum);
		testProgressIndicator.beginAnimatedTask();
	}

}
