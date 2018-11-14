package unittest.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import softtest.domain.c.interval.DoubleDomain;
import softtest.domain.c.interval.DoubleInterval;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.IntegerInterval;
import softtest.symboltable.c.VariableNameDeclaration;
import unittest.gui.helper.Resource;
import unittest.gui.helper.WidgetFactory;
import unittest.inputoutput.extractor.FuncVariable;
import unittest.inputoutput.extractor.LocalVariable;
import unittest.localization.GUILanguageResource;

public class UATInputOutputGUI 
{
	public UATGUI uatGui;
	public Shell shell;
	private Composite infoComposite = null;
	private Composite globalVarComposite = null;
	private Composite paramVarComposite = null;
	private Composite MemberVarComposite = null;
	private Composite localVarComposite = null;
	private Composite controlComposite = null;
	
	
	private CLabel globalInfoLabel = null;
	private CLabel paramInfoLabel = null;
	private CLabel memberInfoLabel = null;
	private CLabel localInfoLabel = null;
	
	private CLabel[] paramVarlabel =null;
	private Text[] paramVarText = null;
	private String[] paramVar = null; 
	
	private CLabel[] globalVarlabel =null;
	private Text[] globalVarText = null;
	private String[] globalVar = null; 
	               
	private CLabel[] localVarlabel =null;
	private Text[] localVarText = null;
	private String[] localVar = null;
	
	private CLabel[] MemberVarlabel =null;
	private Text[] MemberVarText = null;
	private String[] MemberVar = null;
	
	private CLabel infoCLabel = null;
	private Button okButton = null;
	private Button cancelButton = null;
	
	public UATInputOutputGUI(UATGUI demo)
	{
		this.uatGui = demo;
	}
	
	public void go() 
	{
		Display display = Display.getDefault();
		this.createShell();
		this.dealEvent();
		this.shell.open();

		while (!display.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
	
	private void createShell() 
	{
		shell = new Shell( SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL );
		shell.setText(GUILanguageResource.getProperty("InputEditor") );
		shell.setBounds( 500, 500, 410, 550 );
		shell.setLayout( null );
		
		infoComposite = WidgetFactory.createComposite( shell, SWT.FLAT );
		infoComposite.setBackground( Resource.backgroundColor );
		infoComposite.setLayout( new FillLayout() );
		infoComposite.setBounds( 5, 5, 410, 50);
		
		infoCLabel = WidgetFactory.createCLabel( infoComposite, SWT.BORDER,
				GUILanguageResource.getProperty("InputEditorInfo") );
		infoCLabel.setBackground( new Color( null, 230, 250, 175));
		infoCLabel.setFont( Resource.courierNew_10_Font );
		infoCLabel.setBounds( 0, 0, 410, 40 );
		
		globalVarComposite = WidgetFactory.createComposite( shell, SWT.BORDER|SWT.V_SCROLL|SWT.H_SCROLL );
		globalVarComposite.setBounds( 5, 56, 410, 100);
		globalVarComposite.setLayout(null );
		
		paramVarComposite = WidgetFactory.createComposite( shell, SWT.BORDER|SWT.V_SCROLL|SWT.H_SCROLL );
		paramVarComposite.setBounds( 5, 160, 410, 100);
		paramVarComposite.setLayout( null );
		
		MemberVarComposite = WidgetFactory.createComposite( shell, SWT.BORDER|SWT.V_SCROLL|SWT.H_SCROLL );
		MemberVarComposite.setBounds( 5, 260, 410, 100);
		MemberVarComposite.setLayout( null );
		
		localVarComposite = WidgetFactory.createComposite( shell, SWT.BORDER|SWT.V_SCROLL|SWT.H_SCROLL );
		localVarComposite.setBounds( 5,360, 410, 100);
		localVarComposite.setLayout( null );
		
		createTextAndLabel();
		
		
		controlComposite = WidgetFactory.createComposite( shell, SWT.NONE );
		controlComposite.setLayout(new FillLayout() );
		controlComposite.setBounds( 5, 460, 370, 50 );
		okButton = WidgetFactory.createButton( controlComposite, SWT.PUSH );
		okButton.setText( GUILanguageResource.getProperty("OK") );
		okButton.setFont( Resource.courierNew_10_Font );
		okButton.setBounds( 15, 5, 60, 30 );
		
		cancelButton = WidgetFactory.createButton( controlComposite, SWT.PUSH );
		cancelButton.setBounds( 305, 5, 60, 30 );
		cancelButton.setText(  GUILanguageResource.getProperty("Cancel") );
		cancelButton.setFont( Resource.courierNew_10_Font );
		
	}
	private void createTextAndLabel() 
	{
		// TODO Auto-generated method stub
		FuncVariable funcVar = uatGui.getCurrentFunc().getFuncVar();
		if(funcVar == null)
		{
			
		}
		else
		{
			//建立全局变量
			int globalSize = funcVar.getGlobalVar().size();
			globalInfoLabel = WidgetFactory.createCLabel( globalVarComposite, SWT.BORDER, GUILanguageResource.getProperty("GolbalVarNum") + globalSize );
			globalInfoLabel.setBackground( new Color( null, 0, 250, 175));
			globalInfoLabel.setFont( Resource.courierNew_10_Font );
			globalInfoLabel.setBounds( 0, 0, 410, 20 );
			
			globalVarlabel =new CLabel[globalSize];
			globalVarText = new Text[globalSize];
			globalVar = new String[globalSize];
			Iterator iter = funcVar.getGlobalVar().iterator();
			for(int i= 0;i< globalSize;i++)
			{
				VariableNameDeclaration arg =(VariableNameDeclaration) iter.next();
				
				globalVarlabel[i] =  WidgetFactory.createCLabel( globalVarComposite, SWT.FLAT,arg.getName()+ "(" +arg.getTypeImage()+")" );
				globalVarlabel[i].setFont( Resource.courierNew_10_Font );
				globalVarlabel[i].setBounds(5,30 +i*30 ,150 ,30 );
				
				globalVarText[i]= WidgetFactory.createText( globalVarComposite, SWT.SINGLE | SWT.BORDER, globalVar[i], true );
				globalVarText[i].setFont( Resource.courierNew_10_Font );
				globalVarText[i].setBounds(160,30 +i*30 ,180 ,20 );
			}
			
			//建立参数
			int paramNum = funcVar.getParamNum();
			paramInfoLabel = WidgetFactory.createCLabel( paramVarComposite, SWT.BORDER, GUILanguageResource.getProperty("ParamVarNum") + paramNum );
			paramInfoLabel.setBackground( new Color( null, 0, 250, 175));
			paramInfoLabel.setFont( Resource.courierNew_10_Font );
			paramInfoLabel.setBounds( 0, 0, 410, 20 );
			
			paramVarlabel =new CLabel[paramNum];
			paramVarText = new Text[paramNum];
			paramVar = new String[paramNum];
			ArrayList<VariableNameDeclaration> it = funcVar.getParamVar();
			for(int i= 0;i< paramNum;i++)
			{
				VariableNameDeclaration arg = it.get(i);//.elementAt(i);
				
				paramVarlabel[i] =  WidgetFactory.createCLabel( paramVarComposite, SWT.FLAT,arg.getName() + "(" +arg.getTypeImage()+")" );
				paramVarlabel[i].setFont( Resource.courierNew_10_Font );
				paramVarlabel[i].setBounds(5,30 +i*30 ,150 ,30 );
				
//				if(i != 0)
//				WidgetFactory.configureFormData(paramVarlabel[i], 
//						new FormAttachment(paramVarlabel[i-1],0), 
//						new FormAttachment(5,0), 
//						new FormAttachment(30,0), 
//						new FormAttachment(40,0));
//				else
//					WidgetFactory.configureFormData(paramVarlabel[i], 
//							new FormAttachment(0,0), 
//							new FormAttachment(5,0), 
//							new FormAttachment(30,0), 
//							new FormAttachment(40,0));
				
				
				//paramVarlabel[i].setLayoutData(new FormData(5,30));
				
				paramVarText[i]= WidgetFactory.createText( paramVarComposite, SWT.SINGLE | SWT.BORDER, paramVar[i], true );
				paramVarText[i].setFont( Resource.courierNew_10_Font );
				paramVarText[i].setBounds(160,30 +i*30 ,180 ,20 );
				/*if(i != 0)
					WidgetFactory.configureFormData(paramVarText[i], 
							new FormAttachment(paramVarText[i-1],0), 
							new FormAttachment(paramVarlabel[i],0), 
							new FormAttachment(30,0), 
							new FormAttachment(40,0));
				else
					WidgetFactory.configureFormData(paramVarText[i], 
							new FormAttachment(0,0), 
							new FormAttachment(paramVarlabel[i],0), 
							new FormAttachment(30,0), 
							new FormAttachment(40,0));*/
				//paramVarlabel[i].setBounds(5,30 +i*30 ,150 ,30 );
			//	paramVarlabel[i].setLayoutData();
			}
			
			
			//建立成员变量
			memberInfoLabel =  WidgetFactory.createCLabel( MemberVarComposite, SWT.BORDER,GUILanguageResource.getProperty("MemberVarNum") );
			memberInfoLabel.setBackground( new Color( null, 0, 250, 175));
			memberInfoLabel.setFont( Resource.courierNew_10_Font );
			memberInfoLabel.setBounds( 0, 0, 410, 20 );
			
			//建立局部变量，做溢出测试，现在只是提取出来，
			int localSize = funcVar.getLocalVar().size();
			localInfoLabel  =  WidgetFactory.createCLabel( localVarComposite, SWT.BORDER, GUILanguageResource.getProperty("LocalVarNum") + localSize );
			localInfoLabel.setBackground( new Color( null, 0, 250, 175));
			localInfoLabel.setFont( Resource.courierNew_10_Font );
			localInfoLabel.setBounds( 0, 0, 410, 20 );
			
			
			localVarlabel =new CLabel[localSize];
			localVarText = new Text[localSize];
			localVar = new String[localSize];
			iter = funcVar.getLocalVar().iterator();
			for(int i= 0;i< localSize;i++)
			{
				LocalVariable arg =(LocalVariable) iter.next();
				
				localVarlabel[i] =  WidgetFactory.createCLabel( localVarComposite, SWT.FLAT,arg.getVarName() +" at line :" + arg.getDecLine() + "(" +arg.getVarType()+")" );
				localVarlabel[i].setFont( Resource.courierNew_10_Font );
				localVarlabel[i].setBounds(5,30 +i*30 ,150 ,30 );
				
				localVarText[i]= WidgetFactory.createText( localVarComposite, SWT.SINGLE | SWT.BORDER, localVar[i], true );
				localVarText[i].setFont( Resource.courierNew_10_Font );
				localVarText[i].setBounds(160,30 +i*30 ,180 ,20 );
			}
			
		}
		
	}

	private void dealEvent() 
	{
		// TODO Auto-generated method stub
		shell.addShellListener( new ShellCloseListener( this ) );
		okButton.addSelectionListener( new OkButtonListener( this )) ;
		
		cancelButton.addSelectionListener( new CancelButtonListener( this ) );
		
	}
	
	/**
	 * This is the SelectionListener of Ok Button.
	 * @author joaquin(孙华衿)
	 *
	 */
	private class OkButtonListener extends SelectionAdapter {
		private UATInputOutputGUI demo;
		public OkButtonListener( UATInputOutputGUI demo ) {
			this.demo = demo;
		}

		public void widgetSelected( SelectionEvent e ) {
			
			String errorMsg = checkValidity();
			if( errorMsg.equals( "" ) ) {
				doProcess();
				demo.uatGui.getShell().setEnabled( true );
				//uatGui.doProjectViewRefresh();
				//demo.uatGui.doFileViewRefresh();
				demo.uatGui.doRefresh();

				demo.shell.dispose();
			} 
			else 
			{
				MessageBox mb = WidgetFactory.createMessageBox( shell, SWT.ICON_ERROR | SWT.OK, "错误信息", errorMsg );
				mb.open();
			}
			//demo.codemonGui.doTestCaseMenu_ToolBarRefresh();
		}
		
		/**
		 * 对输入情况进行处理
		 */
		private void doProcess() 
		{
			
			FuncVariable funcVar = uatGui.getCurrentFunc().getFuncVar();
			if(funcVar ==null)
				return;
			
			//首先处理全局变量
			int globalSize = funcVar.getGlobalVar().size();
			Iterator iter = funcVar.getGlobalVar().iterator();
			for(int i= 0;i< globalSize;i++)
			{
				VariableNameDeclaration arg =(VariableNameDeclaration) iter.next();
				String text = globalVarText[i].getText();
				getDomain(arg,text);
			}
			
			//处理参数
			int paramNum = funcVar.getParamNum();
			ArrayList<VariableNameDeclaration> it = funcVar.getParamVar();
			for(int i= 0;i< paramNum;i++)
			{
				VariableNameDeclaration arg = it.get(i);//.elementAt(i);
				String text = paramVarText[i].getText();
				getDomain(arg,text);
				System.out.println(arg.getDomain().toString());
			}
		}
		
		/**
		 * 根据输入的参数，得到参数的约束区间，当输入有错误的时候，采用默认区间
		 * @param arg，参数
		 * @param text，输入的区间信息
		 */
		private void getDomain(VariableNameDeclaration arg, String text) 
		{
			String type = arg.getTypeImage();
			if(type.startsWith("int") || type.startsWith("short") || type.startsWith("long"))
			{
				IntegerDomain domain = new IntegerDomain();
				if(text == null || text.trim().isEmpty())
				{
					//arg.addConstraints(new IntegerDomain("int"));
				}
				else
				{
					boolean error = false;
					
					text = text.trim();
					
					String[] domains = text.split(";");
					for(int i=0; !error && i<domains.length;i++)
					{
						String temp = domains[i];
						temp = temp.trim();
						int index = temp.lastIndexOf(',');
						if( index == -1)
						{
							String num = temp.substring(1,temp.length()-1);
							num = num.trim();
							try
							{
								int n = Integer.parseInt(num);
								//arg.addConstraints(new IntegerDomain("int",n));
							}catch(NumberFormatException e)
							{
								error = true;
							}
							
						}
						else
						{
							String minNum = temp.substring(1,index);
							
							String maxNum =temp.substring(index+1,temp.length()-1);
							minNum = minNum.trim();
							maxNum = maxNum.trim();
							try
							{
								int min = Integer.parseInt(minNum);
								int max = Integer.parseInt(maxNum);
								if(min > max)
									error = true;
								domain.mergeWith(new IntegerInterval(min,max));
								//arg.addConstraints(new IntegerDomain("int",min,max));
							}catch(NumberFormatException e)
							{
								error = true;
							}
						}
					}
					
					if(domain.isEmpty())
						error = true;
					if(!error)
					{
						System.out.println(domain.toString());
						arg.setDomain(domain);
					}
				}
			}
			else if(type.startsWith("float") || type.startsWith("double"))
			{
				DoubleDomain domain = new DoubleDomain();
				
				if(text == null || text.trim().isEmpty())
				{
					//arg.addConstraints(new IntegerDomain("int"));
				}
				else
				{
					boolean error = false;
					
					text = text.trim();
					
					String[] domains = text.split(";");
					for(int i=0; !error && i<domains.length;i++)
					{
						String temp = domains[i];
						temp = temp.trim();
						int index = temp.lastIndexOf(',');
						if( index == -1)
						{
							String num = temp.substring(1,temp.length()-1);
							num = num.trim();
							try
							{
								int n = Integer.parseInt(num);
								//arg.addConstraints(new IntegerDomain("int",n));
							}catch(NumberFormatException e)
							{
								error = true;
							}
							
						}
						else
						{
							String minNum = temp.substring(1,index);
							
							String maxNum =temp.substring(index+1,temp.length()-1);
							minNum = minNum.trim();
							maxNum = maxNum.trim();
							try
							{
								double min = Double.parseDouble(minNum);
								double max =  Double.parseDouble(maxNum);
								if(min > max)
									error = true;
								domain.mergeWith(new DoubleInterval(min,max));
								//arg.addConstraints(new IntegerDomain("int",min,max));
							}catch(NumberFormatException e)
							{
								error = true;
							}
						}
					}
					
					if(domain.isEmpty())
						error = true;
					if(!error)
					{
						System.out.println(domain.toString());
						arg.setDomain(domain);
					}
				}
			}
				
		}

		private String checkValidity() 
		{
			// TODO Auto-generated method stub
			return "";
		}
	}

	/**
	 * This is the SelectionListener for Cancel Button.
	 * @author joaquin(孙华衿)
	 *
	 */
	private class CancelButtonListener extends SelectionAdapter 
	{
		private UATInputOutputGUI demo;
		public CancelButtonListener( UATInputOutputGUI demo ) 
		{
			this.demo = demo;
		}
		public void widgetSelected( SelectionEvent e ) {
			demo.uatGui.getShell().setEnabled( true );
			demo.shell.dispose();
		}
	}
	
	/**
	 * This is the ShellListener of UATNewProjectGUI.
	 * @author Xu Mingming(徐明明)
	 *
	 */
	public class ShellCloseListener extends ShellAdapter 
	{
		private UATInputOutputGUI demo;
		public ShellCloseListener( UATInputOutputGUI demo ) 
		{
			this.demo = demo;
		} 
		
		public void shellClosed( ShellEvent e ) 
		{
			demo.uatGui.getShell().setEnabled( true );
			demo.shell.dispose();
		}
		
	}
}
