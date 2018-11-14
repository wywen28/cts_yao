package unittest.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import softtest.cfg.c.Edge;
import softtest.cfg.c.VexNode;
import unittest.drawgraphtools.DrawGraph;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.gui.imageViewer.ImageViewer;
import unittest.localization.GUILanguageResource;
import unittest.module.seperate.NoExitMethodException;
import unittest.module.seperate.TestModule;
import unittest.module.seperate.UnSupportedArgumentException;
import unittest.testcase.generate.util.TestCaseNew;
import unittest.util.RecordToLogger;

/*
 * by za
 * modified by Cai Min
 * 选择目标覆盖元素的页面
 */
public class TargetCoverElementSelectGUI {
	static Logger logger = Logger.getLogger(TargetCoverElementSelectGUI.class);
	public UATGUI uatGui = null;
	public Shell shell = null;
	private Display display = null;
	private Composite topComposite = null;
	private Composite bottomComposite = null;

	private SashForm sashForm;

	// children of topComposite，显示控制流图和源代码
	private CTabFolder topTabFolder = null;
	private CTabItem sourceFileItem = null;
	private CTabItem CFGTabItem = null;
	private Composite imageViewerComp;
	private Composite codeViewerComp;
	private ImageViewer imageViewer = null;

	// children of bottomComposite，显示目标元素选择和测试用例
	private TabFolder bottomTabFolder = null;
	// bottomTabFolder有两个标签页：targetElementTabItem 和 testCaseTabItem
	private TabItem targetElementTabItem = null;
	private Group buttonGroup = null;
	private Button radioBlockButton = null;
	private Button radioBranchButton = null;
	private Button okButton = null;
	private Combo node = null;
	private Combo head = null;
	private Combo tail = null;
	private CLabel nodeNum = null;
	private CLabel headNum = null;
	private CLabel tailNum = null;

	private TabItem testCaseTabItem = null;
	private Composite testCaseLibComposite = null;

	private ArrayList<TestCaseNew> tArray;
	private UATTestCaseTable tableGui;

	private ProgressIndicator progressIndicator;
	private TabItem progressIndicatorTabItem = null;
	private Composite progressComposite = null;

	private CLabel status = null;

	public TargetCoverElementSelectGUI(UATGUI uatGui) {
		this.uatGui = uatGui;
		tArray = new ArrayList<TestCaseNew>();
	}

	public void go() {
		display = Display.getDefault();
		this.createShell();
		this.dealEvent();
		this.shell.open();

		while (!display.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();
		display.dispose();
	}

	private void createShell() {
		shell = new Shell(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL);
		shell.setText(GUILanguageResource.getProperty("TargetElementSelect"));
		shell.setImage(Resource.UATImage);
		shell.setBounds(50, 50, 800, 700);
		shell.setLayout(new FillLayout());
		LayoutUtil.centerShell(display, shell);
		sashForm = new SashForm(shell, SWT.VERTICAL);
		createTopComposite();
		createBottomComposite();
		sashForm.setWeights(new int[] { 5, 2 });

	}

	private void createTopComposite() {
		topComposite = WidgetFactory.createComposite(sashForm, SWT.FLAT);
		topComposite.setBackground(Resource.backgroundColor);
		topComposite.setLayout(new FillLayout());

		topTabFolder = WidgetFactory.createCTabFoler(topComposite, 0);
		WidgetFactory.configureFormData(topTabFolder, new FormAttachment(0, 1),
				new FormAttachment(0, 1), new FormAttachment(100, -1),
				new FormAttachment(100, -1));
		if (uatGui != null) {
			ShowFunction();
		}

		try {
			uatGui.getCurrentFunc().generateCFG(
					uatGui.getCurrentFile().getCFGPicDir());
			File pic = new File (uatGui.getCurrentFunc().getCfgName());
			long size = pic.length();
			if(size >= 1024 *1024)//超过1M的图片
			{
				MessageBox box= WidgetFactory.createInfoMessageBox(uatGui.getShell(), "图片太大", "生成的控制流图的图片太大\n,请到" +uatGui.getCurrentFunc().getCfgName() +"查看！！\n（选中窗口Ctrl+C,到记事本粘贴路径）");
				box.open();
			}
			else
			{
				imageViewerComp = WidgetFactory.createComposite(topTabFolder,
						SWT.BORDER);
				WidgetFactory.configureFormData(imageViewerComp,
						new FormAttachment(0, 0), new FormAttachment(0, 0),
						new FormAttachment(100, 0), new FormAttachment(100, 0));
				imageViewerComp.setLayout(new FormLayout());
				imageViewer = new ImageViewer(imageViewerComp, 1);
				imageViewer.loadImage(uatGui.getCurrentFunc().getCfgName());
				CFGTabItem = WidgetFactory.createCTabItem(topTabFolder,
						GUILanguageResource.getProperty("ControlFlowPic"), null,
						imageViewerComp);
			}
			
		} catch (IOException e) {

		}

	}

	public void createBottomComposite() {
		bottomComposite = WidgetFactory.createComposite(sashForm, SWT.BORDER);
		bottomComposite.setLayout(new FormLayout());

		bottomTabFolder = WidgetFactory.createTabFoler(bottomComposite, 0);
		WidgetFactory.configureFormData(bottomTabFolder, new FormAttachment(0,
				5), new FormAttachment(0, 5), new FormAttachment(100, -5),
				new FormAttachment(100, -5));

		buttonGroup = WidgetFactory.createGroup(bottomTabFolder, 0);
		buttonGroup.setLayout(new FormLayout());
		radioBlockButton = WidgetFactory.createButton(buttonGroup, SWT.RADIO,
				GUILanguageResource.getProperty("Block"));
		radioBranchButton = WidgetFactory.createButton(buttonGroup, SWT.RADIO,
				GUILanguageResource.getProperty("Edge"));
		okButton = WidgetFactory.createButton(buttonGroup, SWT.PUSH,
				GUILanguageResource.getProperty("OK"));
		int number = this.uatGui.getCurrentFunc().getGraph().getVexNum();
		String sNumber = "";
		for (int i = 0; i < number; i++)
			sNumber = sNumber + i + " ";
		node = WidgetFactory.createCombo(buttonGroup, sNumber.split(" "));
		head = WidgetFactory.createCombo(buttonGroup, sNumber.split(" "));
		tail = WidgetFactory.createCombo(buttonGroup, sNumber.split(" "));
		nodeNum = WidgetFactory.createCLabel(buttonGroup, 0,
				GUILanguageResource.getProperty("NodeNum"));
		headNum = WidgetFactory.createCLabel(buttonGroup, 0,
				GUILanguageResource.getProperty("HeadNum"));
		tailNum = WidgetFactory.createCLabel(buttonGroup, 0,
				GUILanguageResource.getProperty("TailNum"));
		radioBlockButton.setSelection(true);
		head.setEnabled(false);
		tail.setEnabled(false);

		WidgetFactory.configureFormData(buttonGroup, new FormAttachment(0, 0),
				new FormAttachment(0, 0), new FormAttachment(100, 0),
				new FormAttachment(100, 0));
		WidgetFactory.configureFormData(radioBlockButton, new FormAttachment(0,
				2), new FormAttachment(0, 2), new FormAttachment(
				radioBranchButton, -5), null);
		WidgetFactory.configureFormData(radioBranchButton, new FormAttachment(
				radioBlockButton, 5), new FormAttachment(0, 2), null, null);
		WidgetFactory.configureFormData(nodeNum, new FormAttachment(0, 2),
				new FormAttachment(radioBlockButton, 5), null, null);
		WidgetFactory.configureFormData(node, new FormAttachment(nodeNum, 17),
				new FormAttachment(radioBlockButton, 5), null, null);
		WidgetFactory.configureFormData(headNum, new FormAttachment(0, 2),
				new FormAttachment(node, 5), null, null);
		WidgetFactory.configureFormData(head, new FormAttachment(headNum, 5),
				new FormAttachment(node, 5), null, null);
		WidgetFactory.configureFormData(tailNum, new FormAttachment(head, 5),
				new FormAttachment(node, 5), null, null);
		WidgetFactory.configureFormData(tail, new FormAttachment(tailNum, 5),
				new FormAttachment(node, 5), null, null);
		WidgetFactory.configureFormData(okButton,
				new FormAttachment(40, 100, 0), null, null, new FormAttachment(
						100, -1));
		targetElementTabItem = WidgetFactory.createTabItem(bottomTabFolder,
				"覆盖元素选择", null, buttonGroup);

		createTestCaseLibComposite();
		testCaseTabItem = WidgetFactory.createTabItem(bottomTabFolder, "测试用例",
				null, testCaseLibComposite);

		createProgressComposite();
		progressIndicatorTabItem = WidgetFactory.createTabItem(bottomTabFolder,
				"进度", null, progressComposite);
	}

	public void dealEvent() {
		shell.addShellListener(new ShellCloseListener(this));
		radioBlockButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				node.setEnabled(true);
				head.setEnabled(false);
				tail.setEnabled(false);
			}
		});

		radioBranchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				node.setEnabled(false);
				head.setEnabled(true);
				tail.setEnabled(true);
			}
		});
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (radioBlockButton.getSelection()) {
					String number;
					final String select = node.getText();
					if (select.equals("")) {
						Display.getDefault().syncExec(new Runnable() {
							public void run() {
								MessageBox mb = WidgetFactory
										.createInfoMessageBox(shell,
												"请输入有效的结点号", "请输入有效的结点号");
								mb.open();
							}
						});
						return;
					}
					//add by xujiaoxian 2012-10-24
					status.setText("正在运行,请稍候...");
					//end add by xujiaoxian 2012-10-24
					bottomTabFolder.setSelection(2);
					progressIndicator.beginAnimatedTask();
					new Thread() {
						public void run() {
							generateTestCase(select);
						}

					}.start();
				} else if (radioBranchButton.getSelection()) {
					final String headNum = head.getText();
					final String tailNum = tail.getText();
					if (headNum.equals("") || tailNum.equals("")) {
						Display.getDefault().syncExec(new Runnable() {
							public void run() {
								MessageBox mb = WidgetFactory
										.createInfoMessageBox(shell,
												"请输入有效的结点号", "请输入有效的结点号");
								mb.open();
							}
						});
						return;
					}
					//add by xujiaoxian 2012-10-24
					status.setText("正在运行,请稍候...");
					//end add by xujiaoxian 2012-10-24
					bottomTabFolder.setSelection(2);
					progressIndicator.beginAnimatedTask();
					new Thread() {
						public void run() {
							generateTestCase(headNum, tailNum);
						}

					}.start();
				}
			}
		});
	}

	private void generateTestCase(String select) {
		//uatGui.getCurrentFunc().gererateControlFlowGraph();
		String number;
		for (VexNode n : uatGui.getCurrentFunc().getGraph().nodes.values()) {
			number = "" + n.getSnumber();
			if (select.equals(number))
				try {
					boolean generate = uatGui.getCurrentFunc()
							.autoTestforTarget(n);
					if (generate) {
						Display.getDefault().syncExec(new Runnable() {
							public void run() {
								List list = new ArrayList<TestCaseNew>();
								tArray = uatGui.getCurrentFunc()
										.getRunningTestDataNew()
										.getTestCaseNewArray();

								topTabFolder.setSelection(1);

								list.add(tArray.get(tArray.size() - 1));
								DrawGraph dg = uatGui.getCurrentFunc()
										.getOutputTool();
								String name = dg.getFilepath()
										+ dg.getfilename()
										+ "_Graph_for_Target" + ".jpg";
								File f = new File(name);
								if (f.exists())
									imageViewer.loadImage(name);
								MessageBox mb = WidgetFactory
										.createInfoMessageBox(shell, "信息",
												"目标测试用例生成完毕");
								mb.open();

								//add by xujiaoxian 2012-10-24
								status.setText("运行结束......");
								//end add by xujiaoxian 2012-10-24
								progressIndicator.done();
								progressIndicator.beginTask(0);
								for (Control control : testCaseLibComposite
										.getChildren())
									control.dispose();
								topTabFolder.setSelection(1);
								bottomTabFolder.setSelection(1);
								tableGui = new UATTestCaseTable(
										testCaseLibComposite, uatGui);
								tableGui.showAllContents = false;
								//tableGui.createContents(tArray);
							}
						});
						return;
					} else {
						Display.getDefault().syncExec(new Runnable() {
							public void run() {
								MessageBox mb = WidgetFactory
										.createInfoMessageBox(shell, "警告",
												"暂时无法生成目标测试用例");
								mb.open();

								//add by xujiaoxian 2012-10-24
								status.setText("运行结束......");
								progressIndicator.done();
								progressIndicator.beginTask(0);
								bottomTabFolder.setSelection(0);
								//end add by xujiaoxian
							}
						});
						return;
					}
				} catch (InterruptedException e1) {
					RecordToLogger.recordExceptionInfo(e1, logger);
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							MessageBox mb = WidgetFactory.createInfoMessageBox(
									shell, "异常",
									"出现异常(InterruptedException)，暂时无法生成目标测试用例");
							mb.open();

							//add by xujiaoxian 2012-10-24
							status.setText("运行结束......");
							progressIndicator.done();
							progressIndicator.beginTask(0);
							bottomTabFolder.setSelection(0);
							//end add by xujiaoxian
						}
					});
					return;
				} catch (UnSupportedArgumentException e1) {
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							MessageBox mb = WidgetFactory.createInfoMessageBox(
									shell, "异常", "有不支持的参数类型，暂时无法生成目标测试用例");
							mb.open();
							
							//add by xujiaoxian 2012-10-24
							status.setText("运行结束......");
							progressIndicator.done();
							progressIndicator.beginTask(0);
							bottomTabFolder.setSelection(0);
							//end add by xujiaoxian
						}
					});
					return;
				} catch (NoExitMethodException e1) {
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							MessageBox mb = WidgetFactory.createInfoMessageBox(
									shell, "异常", "函数没有出口，暂时无法生成目标测试用例");
							mb.open();

							//add by xujiaoxian 2012-10-24
							status.setText("运行结束......");
							progressIndicator.done();
							progressIndicator.beginTask(0);
							bottomTabFolder.setSelection(0);
							//end add by xujiaoxian
						}
					});
					return;
				} catch (Exception e1) {
//					Display.getDefault().syncExec(new Runnable() {
//						public void run() {
//							MessageBox mb = WidgetFactory.createInfoMessageBox(
//									shell, "异常", "出现异常，暂时无法生成目标测试用例");
//							mb.open();
//						}
//					});

					progressIndicator.done();
					progressIndicator.beginTask(0);
					bottomTabFolder.setSelection(0);
					return;
				}
		}
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				MessageBox mb = WidgetFactory.createInfoMessageBox(shell,
						"请输入有效的结点号", "请输入有效的结点号");
				mb.open();

				progressIndicator.done();
				progressIndicator.beginTask(0);
				bottomTabFolder.setSelection(0);
			}
		});
	}

	private void generateTestCase(String headNum, String tailNum) {
		//uatGui.getCurrentFunc().gererateControlFlowGraph();
		for (Edge edge : uatGui.getCurrentFunc().getGraph().edges.values()) {
			if (headNum.endsWith(edge.getTailNode().getSnumber() + "")
					&& tailNum.endsWith(edge.getHeadNode().getSnumber() + "")) {
				try {
					boolean generate = uatGui.getCurrentFunc()
							.autoTestforTarget(edge);
					if (generate) {
						Display.getDefault().syncExec(new Runnable() {
							public void run() {
								List list = new ArrayList<TestCaseNew>();
								tArray = uatGui.getCurrentFunc()
										.getRunningTestDataNew()
										.getTestCaseNewArray();
								list.add(tArray.get(tArray.size() - 1));
								DrawGraph dg = uatGui.getCurrentFunc()
										.getOutputTool();
								String name = dg.getFilepath()
										+ dg.getfilename()
										+ "_Graph_for_Target" + ".jpg";
								File f = new File(name);
								if (f.exists())
									imageViewer.loadImage(name);
								MessageBox mb = WidgetFactory
										.createInfoMessageBox(shell, "信息",
												"目标测试用例生成完毕");
								mb.open();

								//add by xujiaoxian 2012-10-24
								status.setText("运行结束......");
								//end add by xujiaoxian 2012-10-24
								progressIndicator.done();
								progressIndicator.beginTask(0);
								for (Control control : testCaseLibComposite
										.getChildren())
									control.dispose();
								topTabFolder.setSelection(1);
								bottomTabFolder.setSelection(1);
								tableGui = new UATTestCaseTable(
										testCaseLibComposite, uatGui);
								tableGui.showAllContents = false;
								//tableGui.createContents(tArray);
							}
						});
						return;
					} else {
						Display.getDefault().syncExec(new Runnable() {
							public void run() {
								MessageBox mb = WidgetFactory
										.createInfoMessageBox(shell, "警告",
												"暂时无法生成目标测试用例");
								mb.open();

								//add by xujiaoxian 2012-10-24
								status.setText("运行结束......");
								progressIndicator.done();
								progressIndicator.beginTask(0);
								bottomTabFolder.setSelection(0);
								//end add by xujiaoxian
							}
						});
						return;
					}
				} catch (InterruptedException e1) {
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							MessageBox mb = WidgetFactory.createInfoMessageBox(
									shell, "异常",
									"出现异常(InterruptedException)，暂时无法生成目标测试用例");
							mb.open();

							//add by xujiaoxian 2012-10-24
							status.setText("运行结束......");
							progressIndicator.done();
							progressIndicator.beginTask(0);
							bottomTabFolder.setSelection(0);
							//end add by xujiaoxian
						}
					});
					return;
				} catch (UnSupportedArgumentException e1) {
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							MessageBox mb = WidgetFactory.createInfoMessageBox(
									shell, "异常", "存在不支持的参数类型，暂时无法生成目标测试用例");
							mb.open();
							
							//add by xujiaoxian 2012-10-24
							status.setText("运行结束......");
							progressIndicator.done();
							progressIndicator.beginTask(0);
							bottomTabFolder.setSelection(0);
							//end add by xujiaoxian
						}
					});
					return;
				} catch (NoExitMethodException e1) {
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							MessageBox mb = WidgetFactory.createInfoMessageBox(
									shell, "异常", "函数没有出口，暂时无法生成目标测试用例");
							mb.open();

							//add by xujiaoxian 2012-10-24
							status.setText("运行结束......");
							progressIndicator.done();
							progressIndicator.beginTask(0);
							bottomTabFolder.setSelection(0);
							//end add by xujiaoxian
						}
					});
					return;
				} catch (Exception e1) {
//					Display.getDefault().syncExec(new Runnable() {
//						public void run() {
//							MessageBox mb = WidgetFactory.createInfoMessageBox(
//									shell, "异常", "出现异常，暂时无法生成目标测试用例");
//							mb.open();
//						}
//					});
					return;
				}
			}
		}
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				MessageBox mb = WidgetFactory.createInfoMessageBox(shell,
						"请输入有效的结点号", "请输入有效的结点号");
				mb.open();

				progressIndicator.done();
				progressIndicator.beginTask(0);
				bottomTabFolder.setSelection(0);
			}
		});
	}

	// 显示待测函数源代码
	private void ShowFunction() {
		TestModule tm = uatGui.getCurrentFunc();
		try {
			codeViewerComp = WidgetFactory.createComposite(topTabFolder,
					SWT.BORDER);
			WidgetFactory.configureFormData(codeViewerComp, new FormAttachment(
					0, 0), new FormAttachment(0, 0),
					new FormAttachment(100, 0), new FormAttachment(100, 0));
			codeViewerComp.setLayout(new FormLayout());
			new UATUncoveredElementGUI(tm, codeViewerComp);
			sourceFileItem = WidgetFactory.createCTabItem(topTabFolder, uatGui
					.getCurrentFunc().getFuncName(), null, codeViewerComp);
		} catch (Exception e) {
			MessageBox mb = WidgetFactory.createErrorMessageBox(shell,
					"错误信息", "显示 " + tm.getFuncName() + " 出现异常!");
			mb.open();
			e.printStackTrace();
		}
	}

	// 以表格的方式显示测试用例
	private void createTestCaseLibComposite() {
		testCaseLibComposite = WidgetFactory.createComposite(bottomTabFolder,
				SWT.BORDER);
		testCaseLibComposite.setLayout(new FillLayout());

		tableGui = new UATTestCaseTable(testCaseLibComposite, uatGui);
		tableGui.showAllContents = false;
		//tableGui.createContents(tArray);
	}

	private void createProgressComposite() {
		progressComposite = WidgetFactory.createComposite(bottomTabFolder,
				SWT.BORDER);
		progressComposite.setLayout(new FormLayout());

		progressIndicator = new ProgressIndicator(progressComposite);
		progressIndicator.setForeground(Resource.progressBarColor);
		progressIndicator.beginTask(0);

		WidgetFactory.configureFormData(progressIndicator, new FormAttachment(
				progressComposite, 10), new FormAttachment(50, 0),
				new FormAttachment(80, 100, 0), null);

		status = new CLabel(progressComposite, SWT.NONE);
		WidgetFactory
				.configureFormData(status, new FormAttachment(
						progressComposite, 10), new FormAttachment(50, -25),
						null, null);
		status.setText("尚未运行,请稍候...");
	}

	public class ShellCloseListener extends ShellAdapter {
		private TargetCoverElementSelectGUI demo;

		public ShellCloseListener(TargetCoverElementSelectGUI demo) {
			this.demo = demo;
		}

		public void shellClosed(ShellEvent e) {
			demo.uatGui.getShell().setEnabled(true);
			if(imageViewer != null)
				demo.imageViewer.dispose();
			demo.shell.dispose();
		}

	}

}
