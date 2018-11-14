package softtest.domain.c.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import softtest.ast.c.ASTANDExpression;
import softtest.ast.c.ASTAdditiveExpression;
import softtest.ast.c.ASTArgumentExpressionList;
import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTCastExpression;
import softtest.ast.c.ASTConditionalExpression;
import softtest.ast.c.ASTConstant;
import softtest.ast.c.ASTConstantExpression;
import softtest.ast.c.ASTDeclarator;
import softtest.ast.c.ASTDirectDeclarator;
import softtest.ast.c.ASTEnumerator;
import softtest.ast.c.ASTEqualityExpression;
import softtest.ast.c.ASTExclusiveORExpression;
import softtest.ast.c.ASTExpression;
import softtest.ast.c.ASTExpressionStatement;
import softtest.ast.c.ASTFieldId;
import softtest.ast.c.ASTInclusiveORExpression;
import softtest.ast.c.ASTInitDeclarator;
import softtest.ast.c.ASTInitializer;
import softtest.ast.c.ASTInitializerList;
import softtest.ast.c.ASTIterationStatement;
import softtest.ast.c.ASTJumpStatement;
import softtest.ast.c.ASTLogicalANDExpression;
import softtest.ast.c.ASTLogicalORExpression;
import softtest.ast.c.ASTMultiplicativeExpression;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTRelationalExpression;
import softtest.ast.c.ASTSelectionStatement;
import softtest.ast.c.ASTShiftExpression;
import softtest.ast.c.ASTTypeName;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.ASTUnaryOperator;
import softtest.ast.c.AbstractExpression;
import softtest.ast.c.CParserVisitorAdapter;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;
import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.DomainType;
import softtest.domain.c.interval.DoubleDomain;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.PointerDomain;
import softtest.domain.c.interval.PointerValue;
import softtest.domain.c.symbolic.DoubleFactor;
import softtest.domain.c.symbolic.Expression;
import softtest.domain.c.symbolic.Factor;
import softtest.domain.c.symbolic.IntegerFactor;
import softtest.domain.c.symbolic.LogicalExpression;
import softtest.domain.c.symbolic.LogicalNotExpression;
import softtest.domain.c.symbolic.NumberFactor;
import softtest.domain.c.symbolic.RelationExpression;
import softtest.domain.c.symbolic.SymbolExpression;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.Method;
import softtest.interpro.c.Variable;
import softtest.summary.c.MethodFeature;
import softtest.summary.c.MethodPostCondition;
import softtest.summary.c.MethodSummary;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.MethodScope;
import softtest.symboltable.c.NameDeclaration;
import softtest.symboltable.c.Scope;
import softtest.symboltable.c.Search;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_AbstPointer;
import softtest.symboltable.c.Type.CType_AllocType;
import softtest.symboltable.c.Type.CType_Array;
import softtest.symboltable.c.Type.CType_BaseType;
import softtest.symboltable.c.Type.CType_Enum;
import softtest.symboltable.c.Type.CType_Function;
import softtest.symboltable.c.Type.CType_Pointer;
import softtest.symboltable.c.Type.CType_Qualified;
import softtest.symboltable.c.Type.CType_Struct;
import softtest.symboltable.c.Type.CType_Typedef;
import unittest.function.MultiplicativeExprWithOnePara;
import unittest.function.MultiplicativeExpression;
import unittest.function.MultiplicativeExpressionWithAbs;
import unittest.function.MultiplicativeExpressionWithCType;
import unittest.function.MultiplicativeExprlogarithm;
import unittest.function.MultiplicativeExrWithTwoPara;
import unittest.managecoverelement.coverelement.truthtable.TruthTableElement;
import unittest.path.analysis.variabledomain.ArrayVariableDomain;
import unittest.path.analysis.variabledomain.FunctionVariableDomain;
import unittest.path.analysis.variabledomain.IOFunctionVariableDomain;
import unittest.path.analysis.variabledomain.PointerVariableDomain;
import unittest.path.analysis.variabledomain.PointerVariableDomain.PointerState;
import unittest.path.analysis.variabledomain.PrimitiveVariableDomain;
import unittest.path.analysis.variabledomain.StructVariableDomain;
import unittest.path.analysis.variabledomain.VariableDomain;
import unittest.path.analysis.variabledomain.VariableSource;
import unittest.util.ExpressionDomain;

/**
 * modified by zys 2010.7.26
 * ��һЩ���ӱ��ʽ����ʱ����LogicalORExpression��AdditiveExpression....),��ԭ�����ȼ���������ʽ��ֵ��
 * Ȼ�����μ���������ʽ��ֵ����������ֵ֮��Ĳ�����������м��������м���������ļ�����д��ݣ��õ����ձ��ʽ��ֵ��
 * 
 * ����ڼ��������ĳһ���ڳ�������ֵΪNULL��������� 1�����ȼ���������ı��ʽ���Է�ֹ���а����������޸ģ���if(i*k>0 &&
 * i++>5),�����ֵi*k>0���㲻���������������&&�Ҳ���ʽ�� ��ֹ©�����е�i++���㣻 2�������б��ʽ��������Ϻ�Ϊ�������ʽ��һ��ȫ����
 * 3������������Ĵ���ͨ�������쳣�ķ�ʽʵ��
 * */
public class ExpressionValueVisitor extends CParserVisitorAdapter {
    /**
     * ASTAdditiveExpression ASTANDExpression ASTArgumentExpressionList
     * ASTAssignmentExpression ASTCastExpressionp ASTConditionalExpression
     * ASTConstant ASTConstantExpression ASTEqualityExpression ASTExpression
     * ASTFieldId ASTInclusiveORExpression ASTLogicalANDExpression
     * ASTLogicalORExpression ASTPostfixExpression ASTPrimaryExpression
     * ASTRelationalExpression ASTShiftExpression ASTUnaryExpression
     */
    public Object visit(ASTAdditiveExpression node, Object data) {
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        node.jjtGetChild(0).jjtAccept(this, expdata);
        Expression leftvalue = expdata.value;
        Expression rightvalue = null;
        VariableDomain leftVD = expdata.vd;
        VariableDomain rightVD = null;
        try {
            // �����ҽ��з��ż���
            for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                Node c = node.jjtGetChild(i);
                expdata.value = null;
                expdata.vd = null;
                c.jjtAccept(this, expdata);// ���ս��Ϊconst���Ϳ���F6//�����Ƿ����ұߵĽڵ�
                rightvalue = expdata.value;
                rightVD = expdata.vd;
                String operator = node.getOperatorType().get(i - 1);
                if (leftvalue == null || rightvalue == null)
                    throw new MyNullPointerException("AdditiveExpression Value NULL in(.i) " + node.getBeginFileLine());
                if (leftVD instanceof PrimitiveVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                    PrimitiveVariableDomain left = (PrimitiveVariableDomain) leftVD;
                    PrimitiveVariableDomain right = (PrimitiveVariableDomain) rightVD;
                    Expression tmpvalue;
                    VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                    tmpVND.setType(left.getType());
                    if (operator.equals("+")) {
                        leftvalue = leftvalue.add(rightvalue);
                        tmpvalue = left.getExpression().add(right.getExpression());
                    } else {
                        leftvalue = leftvalue.sub(rightvalue);
                        tmpvalue = left.getExpression().sub(right.getExpression());
                    }
                    leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, tmpvalue);
                }
                // ָ���ָ������õ����ǵ�ַ�Ĳ�ֵ���ȴ��Ը���int��
                if ((leftVD instanceof PointerVariableDomain && rightVD instanceof PointerVariableDomain) || (leftVD instanceof PointerVariableDomain && rightVD instanceof ArrayVariableDomain)
                        || (leftVD instanceof ArrayVariableDomain && rightVD instanceof PointerVariableDomain) || (leftVD instanceof ArrayVariableDomain && rightVD instanceof ArrayVariableDomain)) {
                    VariableNameDeclaration intvnd = new VariableNameDeclaration(node.getFileName(), node.getScope(), "pointDiff", node, "pointDiff");
                    intvnd.setType(CType_BaseType.intType);
                    PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) PrimitiveVariableDomain.newInstance(intvnd, expdata.currentvex);
                    expdata.currentvex.addSymbolDomain(pvd.getCurrentSymbolFactor(), new IntegerDomain(Long.MIN_VALUE, Long.MAX_VALUE));
                    leftVD = pvd;
                }

                // add by tangrong 2012-7-6
                /*
                 * ָ��Ĵ����5������p+1Ϊ�� 1. ��ȡָ��p��ָ����ĳ����ڴ�m0�� 2.
                 * �ڿ�����ͼ�ڵ�VexNode�����ArrayVaribaleDomain����û���ĸ�����
                 * �����������ڴ�ռ䣩�к���m0��������ڷ��ش���������ڴ�ġ� 3.
                 * ���û�У��½�һ�������飨���������ڴ�ռ䣩��m0Ϊ�����е�һ��Ա
                 * �����������еĵ�ַ���±꣩Ϊ�½�����s��s��Լ��ʱ��Լ����0�� 4.
                 * ����p��ָ����m0�����������ڴ�ռ��е��±꣬�������������ڴ����Ƿ����
                 * ���±�+1)��Ԫ�أ������򷵻أ����������½�һ���ڴ浥Ԫm1����<�±�+1��m1>�������������ڴ��С� 5.
                 * ִ��p=p+1�Ĳ�����
                 */
                if (leftVD instanceof PointerVariableDomain && rightVD instanceof PrimitiveVariableDomain) {// ������ʽ�����
                    PointerVariableDomain left = (PointerVariableDomain) leftVD;
                    PrimitiveVariableDomain right = (PrimitiveVariableDomain) rightVD;
                    // ����1:(*p)��ΪNULL����ȡ(*p)���ڵĳ����ڴ�
                    if (left.getPointerTo() == null) {
                        left.initMemberVD();
                    }
                    left.setStateNotNull();
                    VariableDomain pt = left.getPointerTo();

                    // ����2 �����Ƿ����������ڴ��ַ�к���pt��
                    ValueSet vset = expdata.currentvex.getValueSet();
                    ArrayVariableDomain memoryBlock = vset.getMemoryBlockContainVD(pt);

                    // ����3 ���û�У��½�һ�������飨���������ڴ�ռ䣩
                    Expression addr;
                    if (memoryBlock == null) {
                        VariableNameDeclaration vnd4MemoryBlock = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoy0", node, "annoy0");
                        vnd4MemoryBlock.setType(new CType_Array(pt.getType()));
                        memoryBlock = new ArrayVariableDomain(vnd4MemoryBlock, left.getVariableSource(), expdata.currentvex);
                        SymbolFactor factor = SymbolFactor.genSymbol(CType_BaseType.intType);

                        // add by yaochi 140421
                        String addrStr;
                        if (left != null && left.getNd() != null)
                            addrStr = left.getNd().getImage().replaceAll("[\\[|\\]|(|)|*]", "_") + "_" + "arrayAddrBase";
                        else
                            addrStr = "arrayAddrBase";
                        VariableNameDeclaration pvnd = new VariableNameDeclaration(node.getFileName(), node.getScope(), addrStr, node, addrStr);
                        pvnd.setType(CType_BaseType.intType);
                        // VariableSource.INPUT_ANNOMY
                        PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) VariableDomain.newInstance(pvnd, expdata.currentvex);
                        pvd.setVariableSource(VariableSource.INPUT_ANNOMY);
                        expdata.currentvex.addVariableDomain(pvd);
                        factor = pvd.getCurrentSymbolFactor();

                        expdata.currentvex.addSymbolDomain(factor, new IntegerDomain(0, Long.MAX_VALUE));
                        addr = new Expression(factor);
                        memoryBlock.addMember(addr, pt);
                        expdata.currentvex.addValue(vnd4MemoryBlock, memoryBlock);
                    } else {
                        addr = memoryBlock.getMember(pt);
                    }

                    // ����4 ��ַƫ��1��addr+1,�����Ƿ�����±�Ϊaddr+1��Ӧ��Ԫ�أ������򷵻أ��������½���
                    Expression addrAfter;
                    if (operator.equals("+")) {
                        addrAfter = addr.add(right.getExpression());
                    } else {// '-'��
                        addrAfter = addr.sub(right.getExpression());
                    }
                    String subIndex = addrAfter.getVND();
                    VariableDomain ptAfter = memoryBlock.getMember(addrAfter, expdata.currentvex.getSymDomainset());
                    if (ptAfter == null) {
                        String memName = memoryBlock.getVariableNameDeclaration().getName() + "[" + subIndex + "]";
                        VariableNameDeclaration vndPlusOne = new VariableNameDeclaration(node.getFileName(), node.getScope(), memName, node, addrAfter.toString());
                        vndPlusOne.setType(pt.getType());
                        vndPlusOne.setExpSimpleImage(addrAfter);
                        vndPlusOne.setParent(memoryBlock.getVariableNameDeclaration());
                        ptAfter = VariableDomain.newInstance(vndPlusOne, left.getVariableSource().next(), expdata.currentvex);
                        memoryBlock.addMember(addrAfter, ptAfter);
                    }

                    // ����5 ִ��tmp=p+1�Ĳ���
                    PointerVariableDomain tmp = new PointerVariableDomain(left.getVariableNameDeclaration(), left.getVariableSource(), left.getNode()); // vnd������
                    tmp.setPointTo(ptAfter);
                    leftVD = tmp;
                }
                // add end
                /*
                 * add by jinkaifeng 2012.10.08���������ַ�ļӼ�����a[2]={0,1}; a+1
                 */
                if (leftVD instanceof ArrayVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                    ArrayVariableDomain left = (ArrayVariableDomain) leftVD;
                    PrimitiveVariableDomain right = (PrimitiveVariableDomain) rightVD;
                    Expression addr;
                    // addr = new Expression(0);
                    Expression addrAfter;
                    CType arrayType = left.getType();
                    /*
                     * if (operator.equals("+")) { addrAfter =
                     * right.getExpression(); } else {//�����ϲ����м��� addrAfter =
                     * new.sub(right.getExpression()); }
                     */

                    addrAfter = right.getExpression();

                    // �����ڴ�ģ��memAfter
                    // VariableDomain memAfter = left.getMember(addrAfter);
                    VariableDomain memAfter = left.getMember(addrAfter, expdata.currentvex.getSymDomainset());
                    if (memAfter != null) {
                        VariableNameDeclaration vnd4AnnoyPtr = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoyPtrTest", node, "annoyPtr");// ����ָ��ı�������
                        vnd4AnnoyPtr.setType(new CType_Pointer(CType.getNextType(arrayType)));//

                        PointerVariableDomain annoyPtr = new PointerVariableDomain(vnd4AnnoyPtr, left.getVariableSource(), expdata.currentvex);// ָ��ָ���ַ

                        annoyPtr.setPointTo(memAfter);

                        leftVD = annoyPtr;
                    }
                    // add by yaochi 2013-0325
                    if (memAfter == null && left.getNd().getImage().contains("*") && !left.getNd().getImage().contains(".")) {
                        String imageStr = left.getNd().getImage();
                        imageStr = imageStr.replace("*", "");

                        VariableNameDeclaration vndPlusOne = new VariableNameDeclaration(node.getFileName(), node.getScope(), imageStr + "[0][" + addrAfter + "]", node, addrAfter.toString());
                        vndPlusOne.setType(CType.getNextType(arrayType));
                        vndPlusOne.setExpSimpleImage(addrAfter);
                        vndPlusOne.setParent(left.getVariableNameDeclaration());
                        memAfter = VariableDomain.newInstance(vndPlusOne, left.getVariableSource().next(), expdata.currentvex);
                        left.addMember(addrAfter, memAfter);

                        VariableNameDeclaration vnd4AnnoyPtr = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoyPtrTest", node, "annoyPtr");// ����ָ��ı�������
                        vnd4AnnoyPtr.setType(new CType_Pointer(CType.getNextType(arrayType)));//
                        PointerVariableDomain annoyPtr = new PointerVariableDomain(vnd4AnnoyPtr, left.getVariableSource(), expdata.currentvex);// ָ��ָ���ַ

                        annoyPtr.setPointTo(memAfter);

                        leftVD = annoyPtr;

                    }// end ��������֮��memAfter��Ϊnull
                    if (memAfter == null) {
                        String imageStr = left.getNd().getImage() + "[" + addrAfter.toString() + "]";
                        Scope scope = node.getScope();
                        NameDeclaration decl = Search.searchInVariableUpward(imageStr, scope);
                        if (decl != null && decl instanceof VariableNameDeclaration) {
                            decl = (VariableNameDeclaration) decl;
                        } else if (decl == null) {
                            decl = new VariableNameDeclaration(node.getFileName(), node.getScope(), imageStr, node, addrAfter.toString());
                            decl.setType(CType.getNextType(arrayType));
                            ((VariableNameDeclaration) decl).setParent(left.getVariableNameDeclaration());
                        }
                        memAfter = VariableDomain.newInstance(decl, left.getVariableSource().next(), expdata.currentvex);
                        left.addMember(addrAfter, memAfter);

                        // ��ָ��ָ����Խ��*(num+2)���⣿
                        VariableNameDeclaration vnd4AnnoyPtr = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoyPtrTest", node, "annoyPtr");// ����ָ��ı�������
                        vnd4AnnoyPtr.setType(new CType_Pointer(CType.getNextType(arrayType)));//
                        PointerVariableDomain annoyPtr = new PointerVariableDomain(vnd4AnnoyPtr, left.getVariableSource(), expdata.currentvex);// ָ��ָ���ַ
                        annoyPtr.setPointTo(memAfter);

                        leftVD = annoyPtr;
                    }

                }
            }
        } catch (MyNullPointerException e) {
            super.visit(node, expdata);
            SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("double"));
            expdata.currentvex.addSymbolDomain(sym, DoubleDomain.getFullDomain());
            leftvalue = new Expression(sym);
            expdata.value = leftvalue;
            return data;
        }

        expdata.value = leftvalue;
        expdata.vd = leftVD; // add by tangrong 2012-3-5
        // if (expdata.currentLogicalExpression != null) {
        // expdata.currentLogicalExpression
        // .addLRExpression(new RelationExpression(expdata.value,
        // null, null, expdata.currentvex));
        // }
        return data;
    }

    public Object visit(ASTANDExpression node, Object data) {
        return dealBinaryBitOperation(node, data, "&");
    }

    public Object visit(ASTArgumentExpressionList node, Object data) {
        // ��ASTPostfixExpression��ͳһ����
        return super.visit(node, data);
    }

    // modified by zhouhb
    public Object visit(ASTInitDeclarator node, Object data) {
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        ASTDeclarator declarator = null;
        if (node.jjtGetChild(0) instanceof ASTDeclarator) {
            declarator = (ASTDeclarator) node.jjtGetChild(0);// �õ������ӽڵ㣬�˽ڵ�Ϊ��ֵ��
                                                             // int *c =
                                                             // a���е�*c
        } else if (node.jjtGetChild(1) instanceof ASTDeclarator) {
            declarator = (ASTDeclarator) node.jjtGetChild(1);
        }
        NameDeclaration decl = declarator.getDecl();// Declarator ������ variable
                                                    // ����
        // ������ṹ�������Ϣ
        // add by zhouhb 2010/8/19
        // if(decl!=null){
        /*
         * if(decl.getType() instanceof CType_Array &&
         * (((CType_Array)decl.getType()).getOriginaltype() instanceof
         * CType_Struct||((CType_Array)decl.getType()).getOriginaltype()
         * instanceof CType_Pointer)|| decl.getType() instanceof
         * CType_Struct||decl.getType() instanceof CType_Pointer &&
         * (((CType_Pointer)decl.getType()).getOriginaltype() instanceof
         * CType_Struct||((CType_Pointer)decl.getType()).getOriginaltype()
         * instanceof
         * CType_Typedef||((CType_Pointer)decl.getType()).getOriginaltype()
         * instanceof CType_Pointer))
         */// ȡ����2��ָ�������Ϊ�ṹ�����Ͳ������� add by yaochi
        /*
         * if(decl.getType() instanceof CType_Array &&
         * (((CType_Array)decl.getType()).getOriginaltype() instanceof
         * CType_Struct||((CType_Array)decl.getType()).getOriginaltype()
         * instanceof CType_Pointer)|| decl.getType() instanceof
         * CType_Struct||decl.getType() instanceof CType_Pointer &&
         * (((CType_Pointer)decl.getType()).getOriginaltype() instanceof
         * CType_Struct||((CType_Pointer)decl.getType()).getOriginaltype()
         * instanceof CType_Typedef))
         * 
         * return data; }
         */
        if (node.jjtGetNumChildren() == 1) {// ֻ��һ�����ӽڵ� PS.����ʹ���ϵ����ݽṹ
            if (node.getType() instanceof CType_Pointer) {
                SymbolFactor sym = SymbolFactor.genSymbol(node.getType());
                PointerDomain p = new PointerDomain();
                expdata.currentvex.addSymbolDomain(sym, p);
                expdata.value = new Expression(sym);

                node.jjtGetChild(0).jjtAccept(this, data);

            } else if (node.getType() instanceof CType_Array) {
                SymbolFactor sym = SymbolFactor.genSymbol(node.getType());
                PointerDomain p = new PointerDomain();
                // modified by zhouhb 2010/7/23
                // �޸��������ʼ��ʱ����ָ�������ΪNULL
                p.setValue(PointerValue.NOTNULL);
                // modified by zhouhb 2010/6/22
                // ����������ȫ�ֱ�����ʼ������Ĺ��� eg.char[a]
                ASTConstantExpression constant = (ASTConstantExpression) node.getFirstChildOfType(ASTConstantExpression.class);
                // �ж��Ƿ�Ϊchar a[]����δ����ά�ȵ���������
                if (constant != null) {
                    constant.jjtAccept(this, data);
                    IntegerDomain size = IntegerDomain.castToIntegerDomain(expdata.value.getDomain(expdata.currentvex.getSymDomainset()));
                    // �ж�����ά���Ƿ�Ϊ��������
                    if (size != null) {
                        int arraySize = (int) size.getMin();
                        p.offsetRange = new IntegerDomain(0, arraySize - 1);
                        // p.AllocType=CType_AllocType.stackType;
                        if (p.Type.contains(CType_AllocType.NotNull)) {
                            p.Type.remove(CType_AllocType.NotNull);
                        }
                        p.Type.add(CType_AllocType.stackType);
                    }
                }
                expdata.currentvex.addSymbolDomain(sym, p);
                expdata.value = new Expression(sym);
                expdata.currentvex.addValue((VariableNameDeclaration) decl, expdata.value);
            }
            return data;
        }
        if (decl instanceof VariableNameDeclaration) {
            VariableNameDeclaration v = (VariableNameDeclaration) decl;
            node.jjtGetChild(1).jjtAccept(this, expdata);
            if (node.jjtGetChild(1) instanceof ASTInitializer) {// �����һ����ֵ���
                                                                // ASTInitializer
                                                                // �ȼ��� int a =
                                                                // c;�е�c
                if (node.getType() instanceof CType_Array) {
                    // modified by zhouhb 2010/7/21//���������ʼ������eg.int
                    // a[5]={3,3,3}
                    if (node.containsChildOfType(ASTInitializerList.class)) {
                        int num = ((ASTInitializerList) node.getFirstChildOfType(ASTInitializerList.class)).jjtGetNumChildren();
                        PointerDomain p = new PointerDomain();
                        p.offsetRange = new IntegerDomain(0, ((CType_Array) node.getType()).getDimSize() - 1);
                        p.allocRange = new IntegerDomain(num, num);
                        // p.AllocType=CType_AllocType.stackType;
                        if (p.Type.contains(CType_AllocType.NotNull)) {
                            p.Type.remove(CType_AllocType.NotNull);
                        }
                        p.Type.add(CType_AllocType.stackType);
                        SymbolFactor sym = SymbolFactor.genSymbol(node.getType());
                        expdata.value = new Expression(sym);
                        expdata.currentvex.addSymbolDomain(sym, p);
                        expdata.currentvex.addValue(v, expdata.value);
                    } else {
                        Expression e = expdata.value;
                        PointerDomain p = (PointerDomain) e.getDomain(expdata.currentvex.getSymDomainset());
                        if (p != null) {
                            p.offsetRange = new IntegerDomain(0, ((CType_Array) node.getType()).getDimSize() - 1);
                            // p.AllocType=CType_AllocType.stackType;
                            if (p.Type.contains(CType_AllocType.NotNull)) {
                                p.Type.remove(CType_AllocType.NotNull);
                            }
                            p.Type.add(CType_AllocType.stackType);
                            SymbolFactor temp = (SymbolFactor) e.getSingleFactor();
                            expdata.currentvex.addSymbolDomain(temp, p);
                        }
                    }
                }// add by zhouhb 2010/8/16
                 // �޸��˿�ָ�븳ֵʹ��
                else if (node.getType() instanceof CType_Pointer) {
                    if (node.containsChildOfType(ASTConstant.class) && !node.containsParentOfType(ASTEqualityExpression.class)
                            && ((ASTConstant) node.getFirstChildOfType(ASTConstant.class)).getImage().equals("0")) {
                        // �����ָ��NULL
                        SymbolFactor p = SymbolFactor.genSymbol(node.getType());
                        PointerDomain pDomain = new PointerDomain();
                        pDomain.offsetRange.intervals.clear();
                        // pDomain.AllocType=CType_AllocType.Null;
                        pDomain.Type.add(CType_AllocType.Null);
                        pDomain.setValue(PointerValue.NULL);
                        expdata.currentvex.addSymbolDomain(p, pDomain);
                        expdata.value = new Expression(p);
                    }
                }
                if (expdata.sideeffect && expdata.currentvex != null) {

                    VariableDomain leftvd = null;
                    VariableDomain rightvd = expdata.vd;
                    expdata.vd = null;
                    node.jjtGetChild(0).jjtAccept(this, expdata);
                    if (expdata.vd != null)
                        leftvd = expdata.vd;
                    else
                        leftvd = VariableDomain.newInstance(v, VariableSource.LOCAL, expdata.currentvex);
                    // add by yaochi 2013-03-12

                    // add by baiyu 2014.12.24 ��Ա�����������λ����ʱ����ӵĸ���������Լ����ʧ�����
                    if (expdata.Help != null) {
                        // expdata.currentvex.addHelpDomianToNode(v,
                        // expdata.Help);
                        rightvd.helpDomainForVD = expdata.Help;
                    }

                    if (leftvd instanceof PointerVariableDomain && rightvd instanceof ArrayVariableDomain) {
                        VariableNameDeclaration vnd;// ����һ���������������൱��������һ��������
                                                    // ex��int a
                        Expression subIndex = new Expression(0);// Expression
                                                                // �������
                        String image = rightvd.getVariableNameDeclaration().getName();// �õ�����������
                        Scope scope = node.getScope();// �õ��ڵ�������Χ
                        NameDeclaration decla = Search.searchInVariableAndMethodUpward(image, scope);// ����ͨ���������ƺ����÷�Χ��������Ƿ�������
                        if (decla instanceof VariableNameDeclaration)// ���decla��varibaleNameDeclaration��һ��ʵ��
                        {
                            vnd = (VariableNameDeclaration) decla;
                        } else {// ����������һ������������д��node�е���Ϣ
                            vnd = new VariableNameDeclaration(node.getFileName(), node.getScope(), image, node, subIndex.toString());
                            vnd.setType(rightvd.getType().getNormalType());
                            vnd.setParent(rightvd.getVariableNameDeclaration());
                        }

                        VariableDomain memvd = VariableDomain.newInstance(vnd, rightvd.getVariableSource().next(), expdata.currentvex);// ����һ�������ڴ�Ļ��࣬��������ֵһ��

                        VariableDomain currentVD = memvd;
                        VariableNameDeclaration vnd4AnnoyPtr = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoyPtr8", node, "annoyPtr8");// ������һ����������
                        vnd4AnnoyPtr.setType(new CType_Pointer(currentVD.getType()));// ��������������Ϊָ������
                        PointerVariableDomain varDomain = new PointerVariableDomain(vnd4AnnoyPtr, expdata.currentvex);// ����һ��PointVariableDomain���Ͷ��󣬳�ʼ��ʹ��֮ǰ���õ�ָ��������������
                        varDomain.setPointTo(currentVD);// ָ�����ͳ����ڴ�ģ��ָ��֮ǰ�����memvd
                        expdata.vd = varDomain; // ExpressionVistorData
                                                // visit��������һ�������﷨����㣬�õ�����Ϣ�ʹ����������

                    }
                    // add by baiyu 2014.12.25 ��Ա�����������λ����ʱ����ӵĸ���������Լ����ʧ�����

                    leftvd.assign(rightvd);
                    expdata.vd = leftvd;
                    expdata.currentvex.addVariableDomain(expdata.vd);
                }
            }
        }
        return data;
    }

    public Object visit(ASTAssignmentExpression node, Object data) {// ��Ҫ���� ��ֵ
                                                                    // �����ķ���
        ExpressionVistorData expdata = (ExpressionVistorData) data;

        // modified by zhouhb
        SimpleNode postfix = (SimpleNode) node.jjtGetParent().jjtGetParent();
        if (postfix.getImage().equals("calloc") && !(node.containsChildOfType(ASTConstant.class)))
            return data;
        if (node.jjtGetNumChildren() == 1) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(0);

            // ��ʼ��SymbolExpression add by wangyi
            if (isConstrainNode((SimpleNode) node)) {// �Ƿ�֧�ڵ��ѭ���ڵ�
                if (expdata.currentSymbolExpression == null && ((child instanceof ASTLogicalORExpression) || (child instanceof ASTLogicalANDExpression))) {
                    SymbolExpression symbolExp = new SymbolExpression();
                    expdata.currentSymbolExpression = symbolExp;
                    expdata.currentvex.addSymbolExpression(symbolExp);
                }
                // ��������û��&&��û��||���Ӻ���ΪASTUnaryExpression��ASTEqualityExpression�����
                if (expdata.currentSymbolExpression == null
                        && !expdata.currentvex.getName().contains("for_inc") // �ų�for_inc�����
                        && ((child instanceof ASTRelationalExpression) || (child instanceof ASTUnaryExpression && !child.containsChildOfType(ASTArgumentExpressionList.class))
                                || child instanceof ASTEqualityExpression || child instanceof ASTAdditiveExpression || child instanceof ASTANDExpression || child instanceof ASTInclusiveORExpression || child instanceof ASTExclusiveORExpression)) {
                    SymbolExpression symbolExp = new SymbolExpression();
                    expdata.currentSymbolExpression = symbolExp;
                    expdata.currentvex.addSymbolExpression(symbolExp);
                    LogicalExpression le = new LogicalExpression();
                    expdata.currentLogicalExpression = le;
                    symbolExp.setLogicalExpression(le);
                }
            }

            child.jjtAccept(this, expdata);

            // add by xjx 2012-6-29
            // ��ȡ����(a+b)����(a*b)���ֱ��ʽ
            if (node.getParentsOfType(ASTAssignmentExpression.class).size() > 0 || node.getParentsOfType(ASTInitDeclarator.class).size() > 0) {
                // do nothing
            } else {
                // ����ֻ��һ��������Ϊ���ʽ����� add by wangyi
                if (child != null && !(child instanceof ASTCastExpression) && !(child instanceof ASTLogicalORExpression) && !(child instanceof ASTLogicalANDExpression)
                        && !(child instanceof ASTEqualityExpression) && !(child instanceof ASTRelationalExpression) && !(child instanceof ASTUnaryExpression)) {
                    for (int i = 0; i < expdata.origexplist.size(); i++) {
                        expdata.currentvex.addMultiExp(expdata.origexplist.get(i));
                    }
                    expdata.origexplist.clear();
                    RelationExpression relationExp = new RelationExpression(expdata.value, null, null, expdata.currentvex);
                    expdata.currentvex.addExpaf(relationExp);
                    if (expdata.currentLogicalExpression != null) {
                        expdata.currentLogicalExpression.addLRExpression(relationExp);
                    }
                }
                if (child != null && child instanceof ASTUnaryExpression && expdata.vd instanceof PrimitiveVariableDomain && !(node.jjtGetParent().jjtGetParent() instanceof ASTJumpStatement)) {
                    PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) expdata.vd;
                    if (pvd.isConst() && expdata.currentLogicalExpression != null) {
                        RelationExpression relationExp = new RelationExpression(expdata.value, null, null, expdata.currentvex);
                        expdata.currentLogicalExpression.addLRExpression(relationExp);
                    }
                }

                // ����������ַ������� add by wangyi
                if (isSpecialStringFunction(node)) {
                    if (expdata.currentSymbolExpression == null) {
                        SymbolExpression symbolExp = new SymbolExpression();
                        expdata.currentSymbolExpression = symbolExp;
                        expdata.currentvex.addSymbolExpression(symbolExp);
                        LogicalExpression le = new LogicalExpression();
                        expdata.currentLogicalExpression = le;
                        symbolExp.setLogicalExpression(le);
                        MethodSet ms = expdata.currentvex.getMethodSet();
                        FunctionVariableDomain fvd = (FunctionVariableDomain) ms.getVd(ms.getMethodNameDeclarationByRetVD(expdata.vd));
                        List<RelationExpression> explist = fvd.getLibFunctionConstraint(expdata.vd, null, null);
                        if (explist != null && explist.size() > 0) {
                            expdata.currentvex.addExpafList(explist);
                            for (RelationExpression re : explist) {
                                expdata.currentLogicalExpression.addLRExpression(re);
                            }
                            for (int i1 = 0; i1 < explist.size() - 1; ++i1) {
                                expdata.currentLogicalExpression.addOperator("&&");
                            }
                        }
                    }
                }
            }

            // �ж��Ƿ�ΪMCDCѡ·�����ǣ����ռ�MCDC��ֵ��Ϣ add by wangyi
            if (isConstrainNode((SimpleNode) node) && expdata.isMcdc && expdata.currentSymbolExpression != null) {
                TruthTableElement table = expdata.mcdcTruthTable.get(expdata.mcdcIndex);
                if (table != null) {
                    boolean[] TFTable = table.getValue();
                    expdata.currentSymbolExpression.setTF(table.getResult());
                    List<RelationExpression> reList = expdata.currentSymbolExpression.getRelationExpressions();
                    int iter = 0;
                    for (RelationExpression re : reList) {
                        re.setMcdcTF(TFTable[iter++]);
                    }
                }
            }
        } else {
            Expression leftvalue, rightvalue;
            SimpleNode firstchild = (SimpleNode) node.jjtGetChild(0);
            SimpleNode secondchild = (SimpleNode) node.jjtGetChild(1);
            SimpleNode thirdchild = (SimpleNode) node.jjtGetChild(2);
            thirdchild.jjtAccept(this, expdata);
            rightvalue = expdata.value;
            VariableDomain rightvd = expdata.vd; // add by tangrong 2011-12-15
            if (rightvd == null) {
                return data; // add by yaochi
            }
            // add by baiyu 2014.12.24 ��Ա�����������λ����ʱ����ӵĸ���������Լ����ʧ�����
            if (expdata.Help != null) {
                rightvd.helpDomainForVD = expdata.Help;
            }
            VariableDomain leftvd = null;// add by jinkaifeng 2012.10.12
            ASTPrimaryExpression p = (ASTPrimaryExpression) firstchild.getChildofType(ASTPrimaryExpression.class);// �õ���ǰ�ڵ㵥֧�ӽڵ���ΪASTPrimaryExpression���͵Ľڵ���Ϣ
            if (rightvalue == null && !(rightvd.getNd() instanceof MethodNameDeclaration))
                return data;
            if (secondchild.getOperatorType().get(0).equals("=")) {
                firstchild.jjtAccept(this, expdata);
                leftvd = expdata.vd;
                // add by zhouhb 2010/8/16
                // �޸��˿�ָ�븳ֵʹ��
                CType nodetype = node.getType();
                if (nodetype instanceof CType_Typedef) {
                    nodetype = nodetype.getSimpleType();
                }
                if (nodetype instanceof CType_Pointer) {// ����ֵ�ı���Ϊָ��
                    // add by zhouhb 2010/11/18
                    // ����ָ������
                    ASTPrimaryExpression pri = (ASTPrimaryExpression) firstchild.getFirstChildOfType(ASTPrimaryExpression.class);
                    if (pri.getType() instanceof CType_Array) {
                        return data;
                    }
                    if (node.containsChildOfType(ASTConstant.class) && !node.containsParentOfType(ASTEqualityExpression.class)
                            && ((ASTConstant) node.getFirstChildOfType(ASTConstant.class)).getImage().equals("0")) {
                        PointerDomain pDomain = new PointerDomain();
                        pDomain.offsetRange.intervals.clear();
                        // pDomain.AllocType=CType_AllocType.Null;
                        pDomain.Type.add(CType_AllocType.Null);
                        pDomain.setValue(PointerValue.NULL);
                        // ������������ʽ�����ͱ��ʽ��ת��Ϊ�����ţ���֪��Ϊʲô��ָ��������������������
                        if (expdata.value.getSingleFactor() == null || expdata.value.getSingleFactor() instanceof IntegerFactor) {
                            expdata.value = new Expression(SymbolFactor.genSymbol(node.getType()));
                        }
                        expdata.currentvex.addSymbolDomain((SymbolFactor) expdata.value.getSingleFactor(), pDomain);
                        rightvalue = expdata.value;
                        if (leftvd instanceof PointerVariableDomain) {
                            PointerVariableDomain pvd = (PointerVariableDomain) leftvd;
                            pvd.setState(PointerState.NULL);
                        }
                    }
                    if (rightvd instanceof FunctionVariableDomain) {
                        ((PointerVariableDomain) leftvd).setPointTo(rightvd);
                    }
                    if (rightvd instanceof PointerVariableDomain) {
                        ((PointerVariableDomain) leftvd).assign(rightvd);
                        leftvd = rightvd;
                    }
                    return data;
                } else if (node.getType() instanceof CType_Typedef && CType.getOrignType(node.getType()) instanceof CType_Function) {
                    if (leftvd instanceof PointerVariableDomain && rightvd.getNd() instanceof MethodNameDeclaration) {// ��ָ�������������ַ
                        // ��ָ�������������ַʱ�����ڳ����ڴ�ı�ʾ��ʽ��ֱ�ӽ�righvd��Ϊleftvd��ָ���OK
                        ((PointerVariableDomain) leftvd).setPointTo(rightvd);
                        ((PointerVariableDomain) leftvd).setStateNotNull();
                        return data;
                    } else if (leftvd instanceof FunctionVariableDomain && rightvd instanceof FunctionVariableDomain) {
                        leftvd.assign(rightvd);
                    }
                    return data;
                }
                expdata.value = rightvalue;
                // add by jinkaifeng 2012.12.18 Ϊ��֧������ָ��
                if (rightvd instanceof ArrayVariableDomain) {
                    VariableNameDeclaration vnd;
                    Expression subIndex = new Expression(0);
                    String image = rightvd.getVariableNameDeclaration().getName() + "[" + subIndex + "]";
                    Scope scope = node.getScope();
                    NameDeclaration decl = Search.searchInVariableAndMethodUpward(image, scope);
                    if (decl instanceof VariableNameDeclaration) {
                        vnd = (VariableNameDeclaration) decl;
                        VariableDomain memvd = VariableDomain.newInstance(vnd, rightvd.getVariableSource().next(), expdata.currentvex);
                        ((ArrayVariableDomain) rightvd).addMember(subIndex, memvd);
                        VariableDomain currentVD = memvd;
                        VariableNameDeclaration vnd4AnnoyPtr = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoyPtr7", node, "annoyPtr7");
                        vnd4AnnoyPtr.setType(new CType_Pointer(currentVD.getType()));
                        PointerVariableDomain varDomain = new PointerVariableDomain(vnd4AnnoyPtr, expdata.currentvex);
                        varDomain.setPointTo(currentVD);
                        rightvd = varDomain;
                    }
                }

                // add by jinkaifeng 2013.5.10�����������ֵΪָ������
                NameDeclaration nd;
                nd = rightvd.getNd();
                if ((rightvd instanceof PointerVariableDomain || rightvd instanceof StructVariableDomain) && nd instanceof MethodNameDeclaration) {

                    expdata.currentvex.popValue((MethodNameDeclaration) nd);
                    VariableSource vdSource = expdata.vd.getVariableSource();

                    // ��vd��variablesource��Ϊ����ֵ
                    if (vdSource.isInput() && vdSource.isAnnomy()) {
                        expdata.vd.setVariableSource(VariableSource.INPUT_RET_ANNOMY);
                    } else if (vdSource.isInput() && !vdSource.isAnnomy()) {
                        expdata.vd.setVariableSource(VariableSource.INPUT_RET);
                    } else if (!vdSource.isInput() && vdSource.isAnnomy()) {
                        expdata.vd.setVariableSource(VariableSource.LOCAL_RET_ANNOMY);
                    } else {
                        expdata.vd.setVariableSource(VariableSource.LOCAL_RET);
                    }

                    expdata.currentvex.addValue((MethodNameDeclaration) nd, expdata.vd);
                }
                // add end
                expdata.vd.assign(rightvd); // �������S ��
                // add end tangrong
                // }

            } else if (secondchild.getOperatorType().get(0).equals("*=")) {
                firstchild.jjtAccept(this, expdata);
                leftvalue = ((PrimitiveVariableDomain) expdata.vd).getExpression();// add by yaochi
                // leftvalue=expdata.value;
                expdata.value = leftvalue.mul(rightvalue);
                // add by jinkaifeng 2012.10.12
                if (expdata.vd instanceof PrimitiveVariableDomain) {
                    PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) (expdata.vd);
                    pvd.setExpression(expdata.value);
                }
                // add end
            } else if (secondchild.getOperatorType().get(0).equals("/=")) {
                firstchild.jjtAccept(this, expdata);
                leftvalue = ((PrimitiveVariableDomain) expdata.vd).getExpression();// add by yaochi
                // leftvalue=expdata.value;
                expdata.value = leftvalue.div(rightvalue);
                // add by jinkaifeng 2012.10.12
                if (expdata.vd instanceof PrimitiveVariableDomain) {
                    PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) (expdata.vd);
                    pvd.setExpression(expdata.value);
                }
                // add end
            } else if (secondchild.getOperatorType().get(0).equals("+=")) {
                firstchild.jjtAccept(this, expdata);
                // leftvalue =
                // ((PrimitiveVariableDomain)expdata.vd).getExpression();//add
                // by yaochi
                // leftvalue=expdata.value;
                // expdata.value=leftvalue.add(rightvalue);
                // add by jinkaifeng 2012.10.18
                if (expdata.vd instanceof PrimitiveVariableDomain) {
                    PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) (expdata.vd);
                    leftvalue = ((PrimitiveVariableDomain) expdata.vd).getExpression();
                    expdata.value = leftvalue.add(rightvalue);
                    pvd.setExpression(expdata.value);
                }
                if (expdata.vd instanceof PointerVariableDomain) {

                    // ����1:(*p)��ΪNULL����ȡ(*p)���ڵĳ����ڴ�
                    PointerVariableDomain varDomain = (PointerVariableDomain) expdata.vd;
                    if (varDomain.getPointerTo() == null) {
                        varDomain.initMemberVD();
                    }
                    varDomain.setStateNotNull();
                    VariableDomain pt = varDomain.getPointerTo();

                    // ����2 �����Ƿ����������ڴ��ַ�к���pt��
                    ValueSet vset = expdata.currentvex.getValueSet();
                    ArrayVariableDomain memoryBlock = vset.getMemoryBlockContainVD(pt);

                    // ����3 ���û�У��½�һ�������飨���������ڴ�ռ䣩
                    Expression addr;
                    if (memoryBlock == null) {
                        VariableNameDeclaration vnd4MemoryBlock =
                                new VariableNameDeclaration(p.getFileName(), p.getScope(), varDomain.getVariableNameDeclaration().getName() + "_arr", node, varDomain.getVariableNameDeclaration()
                                        .getName() + "_arr");
                        vnd4MemoryBlock.setType(new CType_Array(pt.getType()));
                        memoryBlock = new ArrayVariableDomain(vnd4MemoryBlock, expdata.vd.getVariableSource(), expdata.currentvex);
                        SymbolFactor factor = SymbolFactor.genSymbol(CType_BaseType.intType);
                        expdata.currentvex.addSymbolDomain(factor, new IntegerDomain(0, Long.MAX_VALUE));
                        addr = new Expression(factor);
                        memoryBlock.addMember(addr, pt);
                        expdata.currentvex.addValue(vnd4MemoryBlock, memoryBlock);
                    } else {
                        addr = memoryBlock.getMember(pt);
                    }

                    // ����4
                    // ��ַƫ��1��addr+rightvalue,�����Ƿ�����±�Ϊaddr+rightvalue��Ӧ��Ԫ�أ������򷵻أ��������½���
                    Expression addrPlusRight = addr.add(rightvalue);
                    String subindex = addrPlusRight.getVND();
                    VariableDomain ptPlusRight = memoryBlock.getMember(addrPlusRight, expdata.currentvex.getSymDomainset());
                    if (ptPlusRight == null) {
                        String memName = memoryBlock.getVariableNameDeclaration().getName() + "[" + subindex + "]";
                        VariableNameDeclaration vndPlusRight = new VariableNameDeclaration(p.getFileName(), p.getScope(), memName, node, addrPlusRight.toString());
                        vndPlusRight.setType(pt.getType());
                        vndPlusRight.setExpSimpleImage(addrPlusRight);
                        vndPlusRight.setParent(memoryBlock.getVariableNameDeclaration());
                        ptPlusRight = VariableDomain.newInstance(vndPlusRight, pt.getVariableSource().next(), expdata.currentvex);
                        memoryBlock.addMember(addrPlusRight, ptPlusRight);
                    }

                    // ����5 ִ��p=p+1�Ĳ���
                    ((PointerVariableDomain) expdata.vd).changePT(ptPlusRight);

                }
                // add end
            } else if (secondchild.getOperatorType().get(0).equals("-=")) {
                firstchild.jjtAccept(this, expdata);
                if (expdata.vd instanceof PrimitiveVariableDomain) {
                    PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) (expdata.vd);
                    leftvalue = ((PrimitiveVariableDomain) expdata.vd).getExpression();// add by
                                                                                       // yaochi
                    expdata.value = leftvalue.sub(rightvalue);
                    pvd.setExpression(expdata.value);
                }

                if (expdata.vd instanceof PointerVariableDomain) {

                    // ����1:(*p)��ΪNULL����ȡ(*p)���ڵĳ����ڴ�
                    PointerVariableDomain varDomain = (PointerVariableDomain) expdata.vd;
                    if (varDomain.getPointerTo() == null) {
                        varDomain.initMemberVD();
                    }
                    varDomain.setStateNotNull();
                    VariableDomain pt = varDomain.getPointerTo();

                    // ����2 �����Ƿ����������ڴ��ַ�к���pt��
                    ValueSet vset = expdata.currentvex.getValueSet();
                    ArrayVariableDomain memoryBlock = vset.getMemoryBlockContainVD(pt);

                    // ����3 ���û�У��½�һ�������飨���������ڴ�ռ䣩
                    Expression addr;
                    if (memoryBlock == null) {
                        String stryc = varDomain.getVariableNameDeclaration().getName();
                        VariableNameDeclaration vnd4MemoryBlock =
                                new VariableNameDeclaration(p.getFileName(), p.getScope(), varDomain.getVariableNameDeclaration().getName() + "_arr", node, varDomain.getVariableNameDeclaration()
                                        .getName() + "_arr");
                        vnd4MemoryBlock.setType(new CType_Array(pt.getType()));
                        memoryBlock = new ArrayVariableDomain(vnd4MemoryBlock, expdata.vd.getVariableSource(), expdata.currentvex);
                        SymbolFactor factor = SymbolFactor.genSymbol(CType_BaseType.intType);
                        expdata.currentvex.addSymbolDomain(factor, new IntegerDomain(0, Long.MAX_VALUE));
                        addr = new Expression(factor);
                        memoryBlock.addMember(addr, pt);
                        expdata.currentvex.addValue(vnd4MemoryBlock, memoryBlock);
                    } else {
                        addr = memoryBlock.getMember(pt);
                    }

                    // ����4
                    // ��ַƫ��1��addr-rightvalue,�����Ƿ�����±�Ϊaddr-rightvalue��Ӧ��Ԫ�أ������򷵻أ��������½���
                    Expression addrPlusRight = addr.sub(rightvalue);
                    String subindex = addrPlusRight.getVND();
                    VariableDomain ptPlusRight = memoryBlock.getMember(addrPlusRight, expdata.currentvex.getSymDomainset());
                    if (ptPlusRight == null) {
                        String memName = memoryBlock.getVariableNameDeclaration().getName() + "[" + subindex + "]";
                        VariableNameDeclaration vndPlusRight = new VariableNameDeclaration(p.getFileName(), p.getScope(), memName, node, addrPlusRight.toString());
                        vndPlusRight.setType(pt.getType());
                        vndPlusRight.setExpSimpleImage(addrPlusRight);
                        vndPlusRight.setParent(memoryBlock.getVariableNameDeclaration());
                        ptPlusRight = VariableDomain.newInstance(vndPlusRight, pt.getVariableSource().next(), expdata.currentvex);
                        memoryBlock.addMember(addrPlusRight, ptPlusRight);
                    }

                    // ����5 ִ��p=p+1�Ĳ���
                    ((PointerVariableDomain) expdata.vd).changePT(ptPlusRight);

                }
                // add end
            } else {
                node.jjtGetChild(0).jjtAccept(this, expdata);
                leftvalue = expdata.value;
                IntegerDomain i1 = Domain.castToIntegerDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
                IntegerDomain i2 = Domain.castToIntegerDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
                // �������ȷ�������ֵ��ֱ�Ӳ���һ������ȡֵδ���ķ���
                if (i1 != null && i2 != null) {
                    if (i1.isCanonical() && i2.isCanonical()) {
                        long temp = 0;
                        if (secondchild.getOperatorType().get(0).equals("&=")) {
                            temp = i1.getMin() & i2.getMin();
                        } else if (secondchild.getOperatorType().get(0).equals("|=")) {
                            temp = i1.getMin() | i2.getMin();
                        } else if (secondchild.getOperatorType().get(0).equals("^=")) {
                            temp = i1.getMin() ^ i2.getMin();
                        } else if (secondchild.getOperatorType().get(0).equals(">>=")) {
                            temp = i1.getMin() >> i2.getMin();
                        } else if (secondchild.getOperatorType().get(0).equals("<<=")) {
                            temp = i1.getMin() << i2.getMin();
                        } else if (secondchild.getOperatorType().get(0).equals("%=")) {
                            temp = i1.getMin() % i2.getMin();
                        }
                        expdata.value = new Expression(new IntegerFactor(temp));
                    } else if (i1.isCanonical() || i2.isCanonical()) {// baiyu
                        if (secondchild.getOperatorType().get(0).equals("&=")) {
                            if (expdata.vd instanceof PrimitiveVariableDomain) {
                                PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) (expdata.vd);
                                leftvalue = ((PrimitiveVariableDomain) expdata.vd).getExpression();
                                expdata.value = leftvalue.and(rightvalue);
                                pvd.setExpression(expdata.value);
                            }
                        } else if (secondchild.getOperatorType().get(0).equals("|=")) {
                            if (expdata.vd instanceof PrimitiveVariableDomain) {
                                PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) (expdata.vd);
                                leftvalue = ((PrimitiveVariableDomain) expdata.vd).getExpression();
                                expdata.value = leftvalue.inclusiveOR(rightvalue);
                                pvd.setExpression(expdata.value);
                            }
                        } else if (secondchild.getOperatorType().get(0).equals("<<=")) {
                            if (expdata.vd instanceof PrimitiveVariableDomain) {
                                PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) (expdata.vd);
                                long expRatio = (long) Math.pow(2, i2.getMin());
                                Expression tempExp = ((PrimitiveVariableDomain) expdata.vd).getExpression().mul(new Expression(expRatio));
                                expdata.value = tempExp;
                                pvd.setExpression(expdata.value);
                            }
                        } else if (secondchild.getOperatorType().get(0).equals(">>=")) {
                            if (expdata.vd instanceof PrimitiveVariableDomain) {
                                PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) (expdata.vd);
                                long expRatio = (long) Math.pow(2, i2.getMin());
                                if (expRatio != 0) {
                                    Expression tempExp = ((PrimitiveVariableDomain) expdata.vd).getExpression().div(new Expression(expRatio));
                                    expdata.value = tempExp;
                                    pvd.setExpression(expdata.value);
                                }
                            }
                        }
                    }
                } else {
                    expdata.value = new Expression(SymbolFactor.genSymbol(CType_BaseType.getBaseType("int")));
                }
            }

            if (p != null) {
                VariableNameDeclaration v = p.getVariableDecl();
                if (v != null && expdata.sideeffect && expdata.currentvex != null && expdata.value != null) {
                    expdata.currentvex.addValue(v, expdata.value);
                    // expdata.currentvex.addValue(v, expdata.vd);
                }
            }
            if (Config.Field) {
                ASTPostfixExpression po = (ASTPostfixExpression) firstchild.getSingleChildofType(ASTPostfixExpression.class);
                if (po != null) {
                    VariableNameDeclaration v = po.getVariableDecl();
                    if (v != null && expdata.sideeffect && expdata.currentvex != null && expdata.value != null) {
                        expdata.currentvex.addValue(v, expdata.value); // expdata.value�¾��������ͣ��ɲ���
                        // expdata.currentvex.addValue(v, expdata.vd);//add by
                        // yaochi
                        // add by tangrong 2012-2-23
                        if (!expdata.vd.getVariableSource().isAnnomy()) {
                            expdata.currentvex.addValue(v, expdata.vd);
                        }

                    }
                }
            }
        }

        return data;
    }

    public Object visit(ASTCastExpression node, Object data) {
        super.visit(node, data);
        // �����µı��뻷����NULL����Ϊ0����ʱΪ(void *)0�����޸�
        // add by zhouhb
        // ����"=="���ʽ�Ĵ���ʱ��������ָ���Ӧ����
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        // ǿ������ת�����ܻ��������а汾���ܷ��������͵Ĵ���������Ϣ���ڴ�����
        // add by zhouhb 2010/8/30
        ASTTypeName type = (ASTTypeName) node.getFirstChildOfType(ASTTypeName.class);//
        CType curtype = type.getType();
        while (curtype instanceof CType_Typedef) {
            curtype = CType.getNextType(curtype);
        }

        if (expdata.vd == null) {
            return data;
        }

        if (node.jjtGetNumChildren() == 2 && node.containsChildOfType(ASTUnaryExpression.class)) {
            if ((curtype instanceof CType_BaseType)) {
                String name = (((ASTTypeName) node.getFirstChildOfType(ASTTypeName.class)).getType().getName());
                if (name.contains("long") || name.equals("int") || name.contains("size_t")) {
                    SymbolFactor p = SymbolFactor.genSymbol(node.getType());
                    Domain domain = null;
                    IntegerDomain iDomain = null;
                    PrimitiveVariableDomain pvd = null;
                    if (expdata.vd instanceof PrimitiveVariableDomain) {

                        Expression exp = ((PrimitiveVariableDomain) expdata.vd).getExpression();
                        domain = exp.getDomain(expdata.currentvex.getSymDomainset());

                        iDomain = Domain.castToIntegerDomain(domain);
                        if (name.contains("size_t")) {// size_t����
                            iDomain = (IntegerDomain) Domain.intersect(iDomain, new IntegerDomain(0, 1014), curtype);
                        }
                        expdata.currentvex.addSymbolDomain(p, iDomain);
                        expdata.value = new Expression(p);
                        if (expdata.vd.getVariableNameDeclaration() != null) {
                            pvd = (PrimitiveVariableDomain) PrimitiveVariableDomain.newInstance(expdata.vd.getVariableNameDeclaration(), VariableSource.LOCAL, expdata.currentvex);
                        } else {// û��vnd ˵��Ϊconst����
                            pvd = PrimitiveVariableDomain.newIntConstant(iDomain.getMin());
                        }
                        pvd.setExpression(expdata.value);
                    } else if (expdata.vd instanceof PointerVariableDomain) {
                        // unsigned int ǿ��ת��Ϊ0-1024
                        iDomain = (IntegerDomain) Domain.castToType(new IntegerDomain(0, 1014), curtype);
                        CType tmptype = expdata.vd.getType();
                        expdata.vd.getVariableNameDeclaration().setType(curtype);
                        expdata.currentvex.addSymbolDomain(p, iDomain);
                        expdata.value = new Expression(p);
                        pvd = (PrimitiveVariableDomain) PrimitiveVariableDomain.newInstance(expdata.vd.getVariableNameDeclaration(), VariableSource.LOCAL, expdata.currentvex);
                        expdata.vd.getVariableNameDeclaration().setType(tmptype);
                        pvd.setExpression(expdata.value);
                    }
                    expdata.vd = pvd;

                } else if (name.equals("double")) {
                    SymbolFactor p = SymbolFactor.genSymbol(node.getType());

                    Domain domain = null;
                    PrimitiveVariableDomain pvd = null;

                    Expression exp = ((PrimitiveVariableDomain) expdata.vd).getExpression();
                    domain = exp.getDomain(expdata.currentvex.getSymDomainset());
                    DoubleDomain iDomain = Domain.castToDoubleDomain(domain);

                    expdata.currentvex.addSymbolDomain(p, iDomain);
                    expdata.value = new Expression(p);
                    if (expdata.vd.getVariableNameDeclaration() != null) {
                        pvd = (PrimitiveVariableDomain) PrimitiveVariableDomain.newInstance(expdata.vd.getVariableNameDeclaration(), VariableSource.LOCAL, expdata.currentvex);
                    } else {// û��vnd ˵��Ϊconst����
                        pvd = PrimitiveVariableDomain.newDoubleConstant(iDomain.getMin());
                    }

                    pvd.setExpression(expdata.value);
                    expdata.vd = pvd;

                } else if (name.contains("void")) {
                    if (CType.getOrignType(type.getType()).isClassType()) {
                        // ��������((NTP_Packet *)0)-> auth_keyid����
                        SymbolFactor p = SymbolFactor.genSymbol(node.getType());
                        expdata.vd = PointerVariableDomain.NullPointer(expdata.currentvex);
                        PointerVariableDomain pvd = (PointerVariableDomain) expdata.vd;
                        expdata.value = new Expression(p);// setOriginaltype
                                                          // AbstractPoiner
                        if (pvd.getType() != null) {
                            ((CType_AbstPointer) pvd.getType()).setOriginaltype(CType.getNextType(type.getType()));
                        } else {
                            pvd.setType(CType.getNextType(type.getType()));
                        }
                        VariableNameDeclaration vnd = new VariableNameDeclaration((ASTTypeName) node.getFirstDirectChildOfType(ASTTypeName.class));
                        vnd.setParent(pvd.getVariableNameDeclaration());
                        VariableDomain mempd = VariableDomain.newInstance(vnd, expdata.currentvex);
                        pvd.setPointTo(mempd);
                    }
                }
            } else if (curtype instanceof CType_Pointer) {

            }
        } else if (node.jjtGetNumChildren() == 2 && node.containsChildOfType(ASTConstant.class)) {
            // modified by tangrong 2012-4-9 ��Ӷ�==��֧��
            // �����ָ��NULL(void*)0
            if (((ASTConstant) node.getFirstChildOfType(ASTConstant.class)).getImage().equals("0") && (((ASTTypeName) node.getFirstChildOfType(ASTTypeName.class)).getType() instanceof CType_Pointer)) {
                if (CType.getOrignType(type.getType()).isClassType()) {
                    // ��������((NTP_Packet *)0)-> auth_keyid����
                    SymbolFactor sf = SymbolFactor.genSymbol(node.getType());
                    expdata.vd = PointerVariableDomain.NullPointer(expdata.currentvex);
                    PointerVariableDomain pvd = (PointerVariableDomain) expdata.vd;
                    expdata.value = new Expression(sf);// setOriginaltype
                                                       // AbstractPoiner
                    if (pvd.getType() != null) {
                        ((CType_AbstPointer) pvd.getType()).setOriginaltype(CType.getNextType(type.getType()));
                    } else {
                        pvd.setType(CType.getNextType(type.getType()));
                    }
                    VariableNameDeclaration vnd = new VariableNameDeclaration((ASTTypeName) node.getFirstDirectChildOfType(ASTTypeName.class));
                    vnd.setParent(pvd.getVariableNameDeclaration());
                    VariableDomain mempd = VariableDomain.newInstance(vnd, expdata.currentvex);
                    pvd.setPointTo(mempd);
                } else {
                    SymbolFactor p = SymbolFactor.genSymbol(node.getType());
                    PointerDomain pDomain = new PointerDomain();
                    pDomain.offsetRange.intervals.clear();
                    pDomain.Type.add(CType_AllocType.Null);
                    pDomain.setValue(PointerValue.NULL);
                    expdata.currentvex.addSymbolDomain(p, pDomain);
                    expdata.value = new Expression(p);
                    expdata.vd = PointerVariableDomain.NullPointer(expdata.currentvex);
                }
            }
        } else if (node.jjtGetNumChildren() == 2 && expdata.vd instanceof PointerVariableDomain) {
            // modified by tangrong 2012-4-9 ��Ӷ�==��֧��
            // �����ָ��NULL
            SymbolFactor p = SymbolFactor.genSymbol(node.getType());
            PointerVariableDomain pvd = (PointerVariableDomain) expdata.vd.clone();
            PointerDomain pDomain = new PointerDomain();
            pDomain.offsetRange.intervals.clear();
            pDomain.Type.add(CType_AllocType.Null);
            pDomain.setValue(PointerValue.NULL);
            expdata.currentvex.addSymbolDomain(p, pDomain);
            expdata.value = new Expression(p);// setOriginaltype AbstractPoiner
            if (pvd.getType() != null) {
                CType ptype = pvd.getType();
                if (ptype instanceof CType_Qualified) {
                    ptype = CType.getNextType(ptype);
                }
                ((CType_AbstPointer) ptype).setOriginaltype(CType.getNextType(type.getType()));
            } else {
                pvd.setType(CType.getNextType(type.getType()));
            }
            VariableNameDeclaration vnd = new VariableNameDeclaration((ASTTypeName) node.getFirstDirectChildOfType(ASTTypeName.class));
            VariableDomain mempd = VariableDomain.newInstance(vnd, expdata.currentvex);
            pvd.setPointTo(mempd);
            expdata.vd = pvd;
        }
        return data;
    }

    public Object visit(ASTConditionalExpression node, Object data) {
        super.visit(node, data);
        // ����void ConditionalExpression() #ConditionalExpression(>1):
        // {}���Բ������ɵ�֧�������Դ���������
        if (node.jjtGetNumChildren() == 1) {
            throw new RuntimeException("ASTConditionalExpression can't generate single child");
            // liuli:����������ĵڶ������ʽ����Ϊ�գ���c = (++a ? : b);�������
        } else if (node.jjtGetNumChildren() == 2) {
            Expression firstvalue, thirdvalue;
            ExpressionVistorData expdata = (ExpressionVistorData) data;

            VariableDomain firstvd, thirdvd;// add by jinkaifeng 2013.5.13

            SimpleNode firstchild = (SimpleNode) node.jjtGetChild(0);
            firstchild.jjtAccept(this, expdata);
            firstvalue = expdata.value;
            firstvd = expdata.vd;// add by jinkaifeng 2013.5.13
            expdata.value = null;
            expdata.vd = null;
            SimpleNode thirdchild = (SimpleNode) node.jjtGetChild(1);
            thirdchild.jjtAccept(this, expdata);
            thirdvalue = expdata.value;
            thirdvd = expdata.vd;// add by jinkaifeng 2013.5.13

            IntegerDomain i = Domain.castToIntegerDomain(firstvalue.getDomain(expdata.currentvex.getSymDomainset()));
            if (i != null) {
                if (i.isCanonical() && i.getMin() == 0) {
                    expdata.value = thirdvalue;
                } else if (!i.contains(0)) {
                    expdata.value = new Expression(1);// ���ڶ�������Ϊ�գ�ȱʡֵΪ1
                    // by jinkaifeng 2013.5.13
                    VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                    tmpVND.setType(CType_BaseType.intType);
                    expdata.vd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, expdata.value);

                }
                return data;
            }

            // ����һ���µķ��ţ���Ϊ���ߵĲ�
            CType type = node.getType();
            SymbolFactor sym = SymbolFactor.genSymbol(type);
            if (thirdvd instanceof PrimitiveVariableDomain) {// ��ʱ��ֻ������ֵ
                if (type != null) {
                    Domain d1 = Domain.castToType(new IntegerDomain(1, 1), type);
                    Domain d2 = Domain.castToType(thirdvalue.getDomain(expdata.currentvex.getSymDomainset()), type);
                    expdata.currentvex.addSymbolDomain(sym, Domain.union(d1, d2, type));
                }
                expdata.value = new Expression(sym);
                VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                tmpVND.setType(CType_BaseType.intType);
                expdata.vd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, expdata.value);

            }
        } else {
            Expression firstvalue, secondvalue, thirdvalue;
            ExpressionVistorData expdata = (ExpressionVistorData) data;

            VariableDomain firstvd, secondvd, thirdvd;
            SimpleNode firstchild = (SimpleNode) node.jjtGetChild(0);
            firstchild.jjtAccept(this, expdata);
            firstvalue = expdata.value;
            firstvd = expdata.vd;
            expdata.value = null;
            expdata.vd = null;

            SimpleNode secondchild = (SimpleNode) node.jjtGetChild(1);
            secondchild.jjtAccept(this, expdata);
            secondvalue = expdata.value;
            secondvd = expdata.vd;
            expdata.value = null;
            expdata.vd = null;

            SimpleNode thirdchild = (SimpleNode) node.jjtGetChild(2);
            thirdchild.jjtAccept(this, expdata);
            thirdvalue = expdata.value;
            thirdvd = expdata.vd;

            IntegerDomain i = Domain.castToIntegerDomain(firstvalue.getDomain(expdata.currentvex.getSymDomainset()));
            if (i != null) {
                if (i.isCanonical() && i.getMin() == 0) {
                    expdata.value = thirdvalue;
                    VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                    tmpVND.setType(CType_BaseType.intType);
                    expdata.vd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, thirdvalue);
                } else if (!i.contains(0)) {
                    expdata.value = secondvalue;
                    VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                    tmpVND.setType(CType_BaseType.intType);
                    expdata.vd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, secondvalue);
                }

                return data;

            }

            // ����һ���µķ��ţ���Ϊ���ߵĲ�
            CType type = node.getType();
            SymbolFactor sym = SymbolFactor.genSymbol(type);
            if (secondvd instanceof PrimitiveVariableDomain && thirdvd instanceof PrimitiveVariableDomain) {// ��ֻ������ֵ��
                if (type != null && secondvalue != null && thirdvalue != null) {
                    Domain d1 = Domain.castToType(secondvalue.getDomain(expdata.currentvex.getSymDomainset()), type);
                    Domain d2 = Domain.castToType(thirdvalue.getDomain(expdata.currentvex.getSymDomainset()), type);
                    expdata.currentvex.addSymbolDomain(sym, Domain.union(d1, d2, type));
                }
                expdata.value = new Expression(sym);
                VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                tmpVND.setType(CType_BaseType.intType);
                expdata.vd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, expdata.value);

            }
        }
        return data;
    }

    public Object visit(ASTConstant node, Object data) {
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        String image = node.getImage();
        if (image.startsWith("\"") || image.startsWith("L\"") || image.equals("__FUNCTION__") || image.equals("__PRETTY_FUNCTION__") || image.equals("__func__")) {// ���Ӷ�L"abcd"��ʽ���ַ�����ֵ��ʽ�Ĵ���
            PointerDomain p = new PointerDomain();
            p.offsetRange = new IntegerDomain(0, image.length() - 2);
            p.allocRange = new IntegerDomain(image.length() - 1, image.length() - 1);
            // p.AllocType=CType_AllocType.staticType;
            if (p.Type.contains(CType_AllocType.NotNull)) {
                p.Type.remove(CType_AllocType.NotNull);
            }
            p.Type.add(CType_AllocType.staticType);
            p.setElementtype(CType_BaseType.getBaseType("char"));
            p.setLength(new Expression(image.length() + 1));
            p.setValue(PointerValue.NOTNULL);
            SymbolFactor sym = SymbolFactor.genSymbol(node.getType());
            expdata.currentvex.addSymbolDomain(sym, p);
            expdata.value = new Expression(sym);

            VariableNameDeclaration staticvnd = new VariableNameDeclaration(node.getFileName(), node.getScope(), "static_", node, node.getImage());
            staticvnd.setType(CType_AllocType.staticType);
            staticvnd.setStatic(true);
            PointerVariableDomain staticvd = new PointerVariableDomain(staticvnd, expdata.currentvex);
            expdata.vd = staticvd;

        } else if ((image.startsWith("L\'") || image.startsWith("\'")) && !(image.startsWith("\'\\x"))) {
            if (image.length() <= 2) {
                throw new RuntimeException("This is not a legal character");
            }
            if (image.startsWith("L\'")) {
                image = image.substring(1, image.length());
            }
            // liuli:2010.7.23��������int i='DpV!';���
            if (image.length() > 3) {
                if (image.startsWith("\'") && image.endsWith("\'")) {
                    if (!image.startsWith("\'\\")) {
                        SymbolFactor sym = SymbolFactor.genSymbol(node.getType());
                        IntegerDomain p = new IntegerDomain();
                        expdata.currentvex.addSymbolDomain(sym, p);
                        expdata.value = new Expression(sym);
                        return data;
                    }
                } else {
                    throw new RuntimeException("This is not a legal character");
                }
            }
            int count = 1;
            int secondChar = image.charAt(count++);
            int nextChar = image.charAt(count++);
            char value = (char) secondChar;
            if (secondChar == '\\') {
                switch (nextChar) {
                    case 'b':
                        value = '\b';
                        break;
                    case 't':
                        value = '\t';
                        break;
                    case 'n':
                        value = '\n';
                        break;
                    case 'f':
                        value = '\f';
                        break;
                    case 'r':
                        value = '\r';
                        break;
                    case '\"':
                        value = '\"';
                        break;
                    case '\'':
                        value = '\'';
                        break;
                    case '\\':
                        value = '\\';
                        break;
                    case 'a':
                        break;
                    case 'v':
                        break;
                    case '?':
                        break;
                    default: // octal (well-formed: ended by a ' )
                        if ('0' <= nextChar && nextChar <= '7') {
                            int number = nextChar - '0';
                            if (count >= image.length()) {
                                throw new RuntimeException("This is not a legal character");
                            }
                            nextChar = image.charAt(count);
                            if (nextChar != '\'') {
                                count++;
                                if (!('0' <= nextChar && nextChar <= '7')) {
                                    throw new RuntimeException("This is not a legal character");
                                }
                                number = (number * 8) + nextChar - '0';
                                if (count >= image.length()) {
                                    throw new RuntimeException("This is not a legal character");
                                }
                                nextChar = image.charAt(count);
                                if (nextChar != '\'') {
                                    count++;
                                    if (!('0' <= nextChar && nextChar <= '7')) {
                                        throw new RuntimeException("This is not a legal character");
                                    }
                                    number = (number * 8) + nextChar - '0';
                                }
                            }
                            value = (char) number;
                        } else {
                            throw new RuntimeException("This is not a legal character");
                        }
                }
                if (count >= image.length()) {
                    // logger.debug("cnt:" + count + "  " + image +
                    // image.length());
                    throw new RuntimeException("This is not a legal character");
                }
                nextChar = image.charAt(count++);
            }
            if (nextChar != '\'') {
                throw new RuntimeException("This is not a legal character");
            }
            if (secondChar == '\\') {
                int value_temp = image.charAt(count - 2);
                switch (value_temp) {
                    case 'a':
                        expdata.value = new Expression(new IntegerFactor(007));
                        expdata.vd = PrimitiveVariableDomain.newIntConstant(007);
                        break;
                    case 'v':
                        expdata.value = new Expression(new IntegerFactor(011));
                        expdata.vd = PrimitiveVariableDomain.newIntConstant(011);
                        break;
                    case '?':
                        expdata.value = new Expression(new IntegerFactor(063));
                        expdata.vd = PrimitiveVariableDomain.newIntConstant(063);
                        break;
                    default:
                        expdata.value = new Expression(new IntegerFactor(value));
                        expdata.vd = PrimitiveVariableDomain.newIntConstant(value);
                }
            } else {
                expdata.value = new Expression(new IntegerFactor(value));
                expdata.vd = PrimitiveVariableDomain.newIntConstant(value);
            }

        } else {
            if (image.startsWith("\'\\x")) {
                image = image.replace("\'", "");
                image = image.replace("\\x", "0x");
            }
            boolean isInteger = false;

            if (image.endsWith("l") || image.endsWith("L")) {
                image = image.substring(0, image.length() - 1);// ��L��β
                if (image.endsWith("l") || image.endsWith("L")) {
                    image = image.substring(0, image.length() - 1);// ��LL��β
                    if (image.endsWith("u") || image.endsWith("U")) {
                        image = image.substring(0, image.length() - 1);// ��ULL��β
                    }
                } else if (image.endsWith("u") || image.endsWith("U")) {
                    image = image.substring(0, image.length() - 1);// ��UL��β
                }
            } else if (image.endsWith("u") || image.endsWith("U")) {
                image = image.substring(0, image.length() - 1);// ��U��β
                if (image.endsWith("l") || image.endsWith("L")) {
                    image = image.substring(0, image.length() - 1);// ��LU��β
                    if (image.endsWith("l") || image.endsWith("L")) {
                        image = image.substring(0, image.length() - 1);// ��LLU��β
                    }
                }
            }

            char[] source = image.toCharArray();
            int length = source.length;
            long intValue = 0;
            double doubleValue = 0;
            long computeValue = 0L;
            try {
                if (source[0] == '0') {
                    if (length == 1) {
                        computeValue = 0;
                    } else {
                        final int shift, radix;
                        int j;
                        if ((source[1] == 'x') || (source[1] == 'X')) {
                            shift = 4;
                            j = 2;
                            radix = 16;
                        } else {
                            shift = 3;
                            j = 1;
                            radix = 8;
                        }
                        while (source[j] == '0') {
                            j++; // jump over redondant zero
                            if (j == length) { // watch for 000000000000000000
                                computeValue = 0;
                                break;
                            }
                        }
                        while (j < length) {
                            int digitValue = 0;
                            if (radix == 8) {
                                if ('0' <= source[j] && source[j] <= '7') {
                                    digitValue = source[j++] - '0';
                                } else {
                                    throw new RuntimeException("This is not a legal integer");
                                }
                            } else {
                                if ('0' <= source[j] && source[j] <= '9') {
                                    digitValue = source[j++] - '0';
                                } else if ('a' <= source[j] && source[j] <= 'f') {
                                    digitValue = source[j++] - 'a' + 10;
                                } else if ('A' <= source[j] && source[j] <= 'F') {
                                    digitValue = source[j++] - 'A' + 10;
                                } else if (source[j] == 'u' || source[j] == 'l' || source[j] == 'U' || source[j] == 'L') {
                                    j++;
                                    continue;
                                } else {
                                    throw new RuntimeException("This is not a legal integer");
                                }
                            }
                            computeValue = (computeValue << shift) | digitValue;

                        }
                    }
                } else { // -----------regular case : radix = 10-----------
                    for (int i = 0; i < length; i++) {
                        int digitValue;
                        if ('0' <= source[i] && source[i] <= '9') {
                            digitValue = source[i] - '0';
                        } else if (source[i] == 'u' || source[i] == 'l' || source[i] == 'U' || source[i] == 'L') {
                            continue;
                        } else {
                            throw new RuntimeException("This is not a legal integer");
                        }
                        computeValue = 10 * computeValue + digitValue;
                    }
                }
                intValue = computeValue;
                isInteger = true;
            } catch (RuntimeException e) {
            }

            if (isInteger) {
                expdata.value = new Expression(new IntegerFactor(intValue));
                expdata.vd = PrimitiveVariableDomain.newIntConstant(intValue);
            } else {
                doubleValue = Double.valueOf(image);
                expdata.value = new Expression(new DoubleFactor(doubleValue));
                expdata.vd = PrimitiveVariableDomain.newDoubleConstant(doubleValue);
            }

        }
        return data;
    }

    public Object visit(ASTConstantExpression node, Object data) {
        super.visit(node, data);
        return data;
    }

    public Object visit(ASTEqualityExpression node, Object data) {
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        node.jjtGetChild(0).jjtAccept(this, expdata);
        Expression leftvalue = expdata.value;
        VariableDomain leftvd = expdata.vd;
        Expression rightvalue = null;
        VariableDomain rightvd = null;
        try {
            // ���δ����ҽ��з��ż���
            for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                Node c = node.jjtGetChild(i);
                expdata.value = null;
                expdata.vd = null;
                c.jjtAccept(this, expdata);
                rightvalue = expdata.value;
                rightvd = expdata.vd;
                if (leftvalue == null || rightvalue == null) {
                    throw new MyNullPointerException("EqualityExpression Value NULL in(.i) " + node.getBeginFileLine());
                }
                String operator = node.getOperatorType().get(i - 1);

                // ����strlen/strcmp���ַ�������Լ�� add by yaochi 20141020
                MethodSet ms = expdata.currentvex.getMethodSet();
                if (ms != null && ms.getFunctionList().size() != 0) {
                    MethodNameDeclaration mnd = null;
                    FunctionVariableDomain fvd = null;
                    if (ms.isMethodReturn(leftvd)) {
                        // �Ǻ�������ֵ
                        mnd = ms.getMethodNameDeclarationByRetVD(leftvd);
                        if (mnd != null && mnd.isLib()) {
                            fvd = (FunctionVariableDomain) ms.getVd(mnd);
                            List<RelationExpression> explist = fvd.getLibFunctionConstraint(leftvd, operator, rightvd);
                            // �����ַ�������Լ������add by wangyi
                            if (explist != null && explist.size() > 0) {
                                expdata.currentvex.addExpafList(explist);
                                for (RelationExpression re : explist) {
                                    expdata.currentLogicalExpression.addLRExpression(re);
                                }
                                for (int i1 = 0; i1 < explist.size() - 1; ++i1) {
                                    expdata.currentLogicalExpression.addOperator("&&");
                                }
                            }
                        }

                    }
                    if (ms.isMethodReturn(rightvd)) {
                        // �Ǻ�������ֵ
                        mnd = ms.getMethodNameDeclarationByRetVD(rightvd);
                        if (mnd != null && mnd.isLib()) {
                            fvd = (FunctionVariableDomain) ms.getVd(mnd);
                            List<RelationExpression> explist = fvd.getLibFunctionConstraint(rightvd, operator, rightvd);
                            // �����ַ�������Լ������add by wangyi
                            if (explist != null && explist.size() > 0) {
                                expdata.currentvex.addExpafList(explist);
                                for (RelationExpression re : explist) {
                                    expdata.currentLogicalExpression.addLRExpression(re);
                                }
                                for (int i1 = 0; i1 < explist.size() - 1; ++i1) {
                                    expdata.currentLogicalExpression.addOperator("&&");
                                }
                            }
                        }

                    }
                }

                // add by xjx 2012-6-29
                for (int j = 0; j < expdata.origexplist.size(); j++) {
                    expdata.currentvex.addMultiExp(expdata.origexplist.get(j));
                }
                expdata.origexplist.clear();

                if (leftvd.helpDomainForVD != null)// add by baiyu 2014.12.25
                                                   // ��Ա�����������λ����ʱ����ӵĸ���������Լ����ʧ�����
                    expdata.Help = leftvd.helpDomainForVD;
                // add by tangrong 2012-8-28
                if (leftvd instanceof PrimitiveVariableDomain && rightvd instanceof PrimitiveVariableDomain) {
                    if (expdata.Help != null) {
                        for (Map.Entry<Expression, IntegerDomain> e : expdata.Help.entrySet()) {
                            RelationExpression re1 = new RelationExpression(e.getKey(), new Expression(e.getValue().getMin()), ">=", expdata.currentvex);
                            RelationExpression re2 = new RelationExpression(e.getKey(), new Expression(e.getValue().getMax()), "<=", expdata.currentvex);
                            expdata.currentvex.addExpaf(re1);
                            expdata.currentvex.addExpaf(re2);
                            if (expdata.currentLogicalExpression != null) {
                                expdata.currentLogicalExpression.addLRExpression(re1);
                                expdata.currentLogicalExpression.addOperator("&&");

                                expdata.currentLogicalExpression.addLRExpression(re2);
                                expdata.currentLogicalExpression.addOperator("&&");
                            }
                        }
                    }
                    // ��ӵ�ʽ��Լ����add by wangyi
                    RelationExpression re =
                            new RelationExpression(((PrimitiveVariableDomain) leftvd).getExpression(), ((PrimitiveVariableDomain) rightvd).getExpression(), operator, expdata.currentvex);
                    expdata.currentvex.addExpaf(re);
                    if (expdata.currentLogicalExpression != null) {
                        expdata.currentLogicalExpression.addLRExpression(re);
                    }
                }
                // end add by xjx 2012-6-29

                // ���������ʽ�Ľ����װ��vd�У� by jinkaifeng 2013.5.13
                if (leftvalue.isValueEqual(rightvalue, expdata.currentvex.getSymDomainset())) {// ����i=1if(i==3)��������
                    if (operator.equals("==")) {
                        leftvalue = new Expression(1);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.intType);
                        leftvd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        expdata.value = leftvalue;
                        expdata.vd = leftvd;
                    } else {
                        leftvalue = new Expression(0);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.intType);
                        leftvd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        expdata.value = leftvalue;
                        expdata.vd = leftvd;
                    }
                } else if (leftvalue.isValueMustNotEqual(rightvalue, expdata.currentvex.getSymDomainset())) {
                    if (operator.equals("!=")) {
                        leftvalue = new Expression(1);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.intType);
                        leftvd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        expdata.value = leftvalue;
                        expdata.vd = leftvd;
                    } else {
                        leftvalue = new Expression(0);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.intType);
                        leftvd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        expdata.value = leftvalue;
                        expdata.vd = leftvd;
                    }
                } else {
                    // ͨ���쳣������������ PathVexVisitor
                    throw new MyNullPointerException("EqualityExpression Value [0,1] in(.i) " + node.getBeginFileLine());
                }
            }
        } catch (MyNullPointerException e) { // ���Ǹ�ë��
            // super.visit(node, expdata);
            expdata.origexplist.clear();
            // end add by xjx 2012-6-29
            SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
            expdata.currentvex.addSymbolDomain(sym, new IntegerDomain(0, 1));
            leftvalue = new Expression(sym);
            expdata.value = leftvalue;

            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
            tmpVND.setType(CType_BaseType.intType);
            leftvd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
            expdata.vd = leftvd;
            return data;
        }
        expdata.value = leftvalue;
        return data;
    }

    public Object visit(ASTExclusiveORExpression node, Object data) {
        return dealBinaryBitOperation(node, data, "^");
    }

    public Object visit(ASTExpression node, Object data) {
        // zys:2010.8.11 �������жϽڵ㣬����������﷨��������ޣ����ٵݹ�������ӽ��
        int depth = node.getDescendantDepth();// F6
        if (depth <= Config.MAXASTTREEDEPTH) {
            super.visit(node, data);
        } else {
            // Logger.getRootLogger().info("ExpressionValueVisitor: ASTExpression in .i "+node.getBeginFileLine()+" depth="+depth+" overflow");
        }
        // super.visit(node, data);
        return data;
    }

    public Object visit(ASTFieldId node, Object data) {
        // ��ASTPostfixExpression��ͳһ����
        return super.visit(node, data);
    }

    public Object visit(ASTInclusiveORExpression node, Object data) {
        return dealBinaryBitOperation(node, data, "|");
    }

    public Object visit(ASTLogicalANDExpression node, Object data) {
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        // ��ʼ��LogicalExpression
        LogicalExpression logicalExp = null;
        if (expdata.currentSymbolExpression != null) {
            logicalExp = new LogicalExpression();
            if (expdata.currentSymbolExpression.getLogicalExpression() == null) {
                expdata.currentSymbolExpression.setLogicalExpression(logicalExp);
            } else {
                if (expdata.currentLogicalExpression instanceof LogicalNotExpression) {
                    LogicalNotExpression lne = (LogicalNotExpression) expdata.currentLogicalExpression;
                    lne.setLogicalExpression(logicalExp);
                } else {
                    expdata.currentLogicalExpression.addLRExpression(logicalExp);
                }
            }
            expdata.currentLogicalExpression = logicalExp;
        }
        node.jjtGetChild(0).jjtAccept(this, expdata);
        expdata.currentLogicalExpression = logicalExp;

        Expression leftvalue = expdata.value;
        Expression rightvalue = null;
        VariableDomain leftVD = expdata.vd;
        VariableDomain rightVD = null;
        DoubleDomain d1 = null;
        DoubleDomain d2 = null;

        // changed by jinkaifeng �����˶Գ����ڴ�ģ�͵�֧�� 2012.11.7
        try {
            // �����ҽ��з��ż���
            for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                Node c = node.jjtGetChild(i);
                expdata.value = null;
                expdata.vd = null;
                c.jjtAccept(this, expdata);
                expdata.currentLogicalExpression = logicalExp;
                if (logicalExp != null) {
                    logicalExp.addOperator("&&");
                }
                rightvalue = expdata.value;
                rightVD = expdata.vd;

                if (leftVD instanceof PrimitiveVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                    if (leftvalue == null || rightvalue == null) {
                        throw new MyNullPointerException("LogicalANDExpression Value NULL in(.i) " + node.getBeginFileLine());
                    }
                    // zys:2010.8.9 ����&&�Ķ�·���ԣ��������ʽֵΪ0,���ټ����Ҳ���ʽ��ֵ
                    d1 = Domain.castToDoubleDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
                    if (d1 != null && d1.isCanonical() && d1.getMin() == 0) {
                        leftvalue = new Expression(0);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(leftVD.getType());
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);

                        break;
                    }

                    d2 = Domain.castToDoubleDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
                    if (d1 != null && d2 != null) {
                        if (!d1.contains(0) && !d2.contains(0)) {
                            leftvalue = new Expression(1);
                            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                            tmpVND.setType(leftVD.getType());
                            leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                            continue;
                        } else if (d1.isCanonical() && d1.getMin() == 0) {// d1.isCanonical���������ڣ������һ��ȷ����������ʾ���ʽ���߼�ȡֵΪȷ�������
                            leftvalue = new Expression(0);
                            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                            tmpVND.setType(leftVD.getType());
                            leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                            break;
                        } else if (d2.isCanonical() && d2.getMin() == 0) {
                            leftvalue = new Expression(0);
                            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                            tmpVND.setType(leftVD.getType());
                            leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                            break;
                        }
                    } else {
                        throw new MyNullPointerException("LogicalANDExpression Domain NULL in(.i) " + node.getBeginFileLine());
                    }
                } else if (leftVD instanceof PointerVariableDomain && rightVD instanceof PointerVariableDomain) {
                    PointerVariableDomain left = (PointerVariableDomain) leftVD;
                    PointerVariableDomain right = (PointerVariableDomain) rightVD;
                    // ��·���ԣ������һ��Ϊ�գ��򲻼������
                    if (left.getState() == PointerState.NULL || right.getState() == PointerState.NULL) {
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.getBaseType("int"));
                        leftvalue = new Expression(0);
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        break;
                    } else if (left.getState() == PointerState.NOTNULL && right.getState() == PointerState.NOTNULL) {
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.getBaseType("int"));
                        leftvalue = new Expression(1);
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        continue;

                    } else {
                        throw new MyNullPointerException("LogicalANDExpression Domain NULL in(.i) " + node.getBeginFileLine());
                    }

                } else if (leftVD instanceof PrimitiveVariableDomain && rightVD instanceof PointerVariableDomain) {
                    // ����&&�Ķ�·���ԣ��������ʽֵΪ0,���ټ����Ҳ���ʽ��ֵ
                    d1 = Domain.castToDoubleDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
                    PointerVariableDomain right = (PointerVariableDomain) rightVD;
                    if (d1 != null && d1.isCanonical() && d1.getMin() == 0) {
                        leftvalue = new Expression(0);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(leftVD.getType());
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);

                        break;
                    }
                    if (d1 != null && !d1.contains(0) && right.getState() == PointerState.NOTNULL) {
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(leftVD.getType());
                        leftvalue = new Expression(1);
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        continue;
                    } else if (d1 != null && !d1.contains(0) && right.getState() == PointerState.NULL) {
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(leftVD.getType());
                        leftvalue = new Expression(0);
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        break;
                    } else {
                        throw new MyNullPointerException("LogicalANDExpression Domain NULL in(.i) " + node.getBeginFileLine());
                    }
                } else if (leftVD instanceof PointerVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                    d1 = Domain.castToDoubleDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
                    PointerVariableDomain left = (PointerVariableDomain) leftVD;
                    if (d1 != null && d1.isCanonical() && d1.getMin() == 0) {
                        leftvalue = new Expression(0);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(rightVD.getType());
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);

                        break;
                    }

                    if (d1 != null && !d1.contains(0) && left.getState() == PointerState.NOTNULL) {
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(rightVD.getType());
                        leftvalue = new Expression(1);
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        continue;
                    } else if (d1 != null && !d1.contains(0) && left.getState() == PointerState.NULL) {
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(rightVD.getType());
                        leftvalue = new Expression(0);
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        break;
                    } else {
                        throw new MyNullPointerException("LogicalANDExpression Domain NULL in(.i) " + node.getBeginFileLine());
                    }
                } else {
                    throw new MyNullPointerException("LogicalANDExpression Domain NULL in(.i) " + node.getBeginFileLine());
                }
            }
        } catch (MyNullPointerException e) {
            super.visit(node, expdata);
            SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
            expdata.currentvex.addSymbolDomain(sym, new IntegerDomain(0, 1));
            leftvalue = new Expression(sym);
            expdata.value = leftvalue;
            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
            tmpVND.setType(CType_BaseType.getBaseType("int"));
            expdata.vd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
            return data;
        }

        expdata.value = leftvalue;
        expdata.vd = leftVD;
        expdata.currentvex.addOperator("&&");// �߼��� add by wangyi
        return data;
    }

    public Object visit(ASTLogicalORExpression node, Object data) {
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        // ��ʼ��LogicalExpression add by wangyi
        LogicalExpression logicalExp = null;
        if (expdata.currentSymbolExpression != null) {
            logicalExp = new LogicalExpression();
            if (expdata.currentSymbolExpression.getLogicalExpression() == null) {
                expdata.currentSymbolExpression.setLogicalExpression(logicalExp);
            } else {
                if (expdata.currentLogicalExpression instanceof LogicalNotExpression) {
                    LogicalNotExpression lne = (LogicalNotExpression) expdata.currentLogicalExpression;
                    lne.setLogicalExpression(logicalExp);
                } else {
                    expdata.currentLogicalExpression.addLRExpression(logicalExp);
                }
            }
            expdata.currentLogicalExpression = logicalExp;
        }

        node.jjtGetChild(0).jjtAccept(this, expdata);
        expdata.currentLogicalExpression = logicalExp;
        Expression leftvalue = expdata.value;
        DoubleDomain d1 = null;
        Expression rightvalue = null;
        DoubleDomain d2 = null;
        VariableDomain leftVD = expdata.vd;
        VariableDomain rightVD = null;
        try {
            // ���ݱ��ʽ����ֵ�����δ����ҽ��з��ż��㣬�����ݱ��ʽ�ķ��Ž��м���
            for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                Node c = node.jjtGetChild(i);
                expdata.value = null;
                expdata.vd = null;
                c.jjtAccept(this, expdata);
                expdata.currentLogicalExpression = logicalExp;
                if (logicalExp != null) {
                    logicalExp.addOperator("||");
                }
                rightvalue = expdata.value;
                rightVD = expdata.vd;

                // changed by jinkaifeng �����˶Գ����ڴ�ģ�͵�֧�� 2012.11.7
                if (leftVD instanceof PrimitiveVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                    if (leftvalue == null || rightvalue == null) {
                        throw new MyNullPointerException("LogicalORExpression Value NULL in(.i) " + node.getBeginFileLine());
                    }
                    // zys:2010.8.9 ����||�Ķ�·���ԣ��������ʽֵΪ1,���ټ����Ҳ���ʽ��ֵ
                    d1 = Domain.castToDoubleDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
                    if (d1 != null && !d1.contains(0)) {
                        leftvalue = new Expression(1);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(leftVD.getType());
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        break;
                    }

                    d2 = Domain.castToDoubleDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
                    if (d1 != null && d2 != null) {
                        if (!d1.contains(0) || !d2.contains(0)) {
                            leftvalue = new Expression(1);
                            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                            tmpVND.setType(leftVD.getType());
                            leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                            break;
                        } else if (d1.isCanonical() && d1.getMin() == 0 && d2.isCanonical() && d2.getMin() == 0) {
                            leftvalue = new Expression(0);
                            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                            tmpVND.setType(leftVD.getType());
                            leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                            continue;
                        }
                    } else {
                        throw new MyNullPointerException("LogicalORExpression Domain NULL in(.i) " + node.getBeginFileLine());
                    }
                } else if (leftVD instanceof PointerVariableDomain && rightVD instanceof PointerVariableDomain) {
                    PointerVariableDomain left = (PointerVariableDomain) leftVD;
                    PointerVariableDomain right = (PointerVariableDomain) rightVD;

                    if (left.getState() == PointerState.NOTNULL || right.getState() == PointerState.NOTNULL) {
                        leftvalue = new Expression(1);

                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.getBaseType("int"));
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        break;

                    }

                    if (left.getState() == PointerState.NULL && right.getState() == PointerState.NULL) {
                        leftvalue = new Expression(0);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.getBaseType("int"));
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        continue;
                    } else {
                        throw new MyNullPointerException("LogicalORExpression Domain NULL in(.i) " + node.getBeginFileLine());
                    }
                } else if (leftVD instanceof PrimitiveVariableDomain && rightVD instanceof PointerVariableDomain) {
                    PointerVariableDomain right = (PointerVariableDomain) rightVD;

                    d1 = Domain.castToDoubleDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
                    if ((d1 != null && !d1.contains(0)) || right.getState() == PointerState.NOTNULL) {
                        leftvalue = new Expression(1);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(leftVD.getType());
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        break;
                    }

                    if ((d1 != null && d1.isCanonical() && d1.getMin() == 0) && right.getState() == PointerState.NULL) {
                        leftvalue = new Expression(0);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(leftVD.getType());
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        continue;
                    } else {
                        throw new MyNullPointerException("LogicalORExpression Domain NULL in(.i) " + node.getBeginFileLine());
                    }
                } else if (leftVD instanceof PointerVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                    PointerVariableDomain left = (PointerVariableDomain) leftVD;

                    d1 = Domain.castToDoubleDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
                    if ((d1 != null && !d1.contains(0)) || left.getState() == PointerState.NOTNULL) {
                        leftvalue = new Expression(1);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(leftVD.getType());
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        break;
                    }

                    if ((d1 != null && d1.isCanonical() && d1.getMin() == 0) && left.getState() == PointerState.NULL) {
                        leftvalue = new Expression(0);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(rightVD.getType());
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        continue;
                    } else {
                        throw new MyNullPointerException("LogicalORExpression Domain NULL in(.i) " + node.getBeginFileLine());
                    }
                } else {
                    throw new MyNullPointerException("LogicalORExpression Domain NULL in(.i) " + node.getBeginFileLine());
                }
            }
        } catch (MyNullPointerException e) {
            super.visit(node, expdata);
            SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
            expdata.currentvex.addSymbolDomain(sym, new IntegerDomain(0, 1));
            leftvalue = new Expression(sym);
            expdata.value = leftvalue;
            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
            tmpVND.setType(CType_BaseType.getBaseType("int"));
            expdata.vd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
            return data;
        }

        expdata.value = leftvalue;
        expdata.vd = leftVD;
        expdata.currentvex.addOperator("||");// �߼��� add by wangyi
        return data;
    }

    public Object visit(ASTMultiplicativeExpression node, Object data) {
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        node.jjtGetChild(0).jjtAccept(this, expdata);
        Expression leftvalue = expdata.value;
        VariableDomain leftvd = expdata.vd;
        Expression rightvalue = null;
        VariableDomain rightvd = null;
        try {
            // �����ҽ��з��ż���
            for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                Node c = node.jjtGetChild(i);
                expdata.value = null;
                c.jjtAccept(this, expdata);
                rightvalue = expdata.value;
                rightvd = expdata.vd;
                if (leftvd == null || rightvd == null)
                    throw new MyNullPointerException("MultiplicativeExpression Value NULL in(.i) " + node.getBeginFileLine());
                leftvalue = ((PrimitiveVariableDomain) leftvd).getExpression();
                rightvalue = ((PrimitiveVariableDomain) rightvd).getExpression();
                String operator = node.getOperatorType().get(i - 1);
                if (leftvalue == null || rightvalue == null)
                    throw new MyNullPointerException("MultiplicativeExpression Value NULL in(.i) " + node.getBeginFileLine());
                if (operator.equals("*")) {
                    // 2010.12.03 liuli:��expression�ó��ȹ���ʱ���ᵼ�¼���������ѭ��
                    if (rightvalue.getTerms().size() * leftvalue.getTerms().size() > 1000) {
                        IntegerDomain i1 = Domain.castToIntegerDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
                        IntegerDomain i2 = Domain.castToIntegerDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
                        if (i1 != null && i2 != null && i1.isCanonical() && i2.isCanonical()) {
                            leftvalue = new Expression(i1.getMin() * i2.getMin());
                        } else {
                            leftvalue = new Expression(SymbolFactor.genSymbol(node.getType()));
                        }
                    } else {
                        leftvalue = leftvalue.mul(rightvalue);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(leftvd.getType());
                        if (leftvd instanceof PrimitiveVariableDomain && rightvd instanceof PrimitiveVariableDomain) {
                            // Expression tmpExpr = ((PrimitiveVariableDomain)
                            // leftvd).getExpression().div(((PrimitiveVariableDomain)
                            // rightvd).getExpression());
                            // zhangxuzhou 2012-9-14����bug ע����һ��
                            Expression tmpExpr = ((PrimitiveVariableDomain) leftvd).getExpression().mul(((PrimitiveVariableDomain) rightvd).getExpression());
                            leftvd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, tmpExpr);
                        }
                    }
                } else if (operator.equals("/")) {
                    if (rightvalue.getTerms().size() * leftvalue.getTerms().size() > 1000) {
                        IntegerDomain i1 = Domain.castToIntegerDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
                        IntegerDomain i2 = Domain.castToIntegerDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
                        if (i1 != null && i2 != null && i1.isCanonical() && i2.isCanonical()) {
                            leftvalue = new Expression(i1.getMin() / i2.getMin());
                        } else {
                            leftvalue = new Expression(SymbolFactor.genSymbol(node.getType()));
                        }
                    } else {
                        // add by jinkaifeng 2012.10.17 ���γ�������0�����
                        if (rightvalue.toString().equals("0")) {
                            leftvalue = new Expression(SymbolFactor.genSymbol(node.getType()));
                            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                            tmpVND.setType(leftvd.getType());
                            leftvd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);

                        }
                        // add end
                        else {
                            leftvalue = leftvalue.div(rightvalue);
                            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                            tmpVND.setType(leftvd.getType());
                            if (leftvd instanceof PrimitiveVariableDomain && rightvd instanceof PrimitiveVariableDomain) {
                                Expression tmpExpr = ((PrimitiveVariableDomain) leftvd).getExpression().div(((PrimitiveVariableDomain) rightvd).getExpression());
                                leftvd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, tmpExpr);
                            }
                        }
                    }
                } else if (operator.equals("%")) {
                    if (rightvalue.toString().equals("0")) {
                        leftvalue = new Expression(SymbolFactor.genSymbol(node.getType()));
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(leftvd.getType());
                        leftvd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                    } else {
                        IntegerDomain i1 = Domain.castToIntegerDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
                        IntegerDomain i2 = Domain.castToIntegerDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
                        VariableNameDeclaration scaleVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "scale_" + node.toString(), node, "scale_" + node.toString());
                        // scaleVND.
                        scaleVND.setType(CType_BaseType.getBaseType("int"));
                        SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"), "scale");
                        Expression scaleExp = new Expression(sym);
                        VariableDomain scaleVD = new PrimitiveVariableDomain(scaleVND, VariableSource.LOCAL, expdata.currentvex, sym);
                        expdata.currentvex.addValue(scaleVND, scaleVD);
                        if (i2.isCanonical()) {
                            IntegerDomain temp = IntegerDomain.div(i1, i2);
                            expdata.currentvex.addSymbolDomain(sym, temp);
                        }
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.getBaseType("int"));
                        if (leftvd instanceof PrimitiveVariableDomain && rightvd instanceof PrimitiveVariableDomain && scaleVD instanceof PrimitiveVariableDomain) {
                            Expression tmpExpr =
                                    ((PrimitiveVariableDomain) leftvd).getExpression()
                                            .sub(((PrimitiveVariableDomain) scaleVD).getExpression().mul(((PrimitiveVariableDomain) rightvd).getExpression()));
                            leftvd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, tmpExpr);
                            leftvalue = tmpExpr;
                        }
                    }
                }
                continue;
            }
        } catch (MyNullPointerException e) {
            super.visit(node, expdata);
            SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("double"));
            if (Config.USEUNKNOWN)
                expdata.currentvex.addSymbolDomain(sym, DoubleDomain.getUnknownDomain());
            else
                expdata.currentvex.addSymbolDomain(sym, DoubleDomain.getFullDomain());
            leftvalue = new Expression(sym);
            expdata.value = leftvalue;

            return data;
        }
        expdata.value = leftvalue;
        expdata.vd = leftvd;
        return data;
    }

    public Object visit(ASTPostfixExpression node, Object data) {// ����������У���Ҫ����������Ӧ�ķ��ű��ʽ�������ǽ���������һ������
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        ASTPrimaryExpression primary = (ASTPrimaryExpression) node.jjtGetChild(0);
        expdata.vd = null;// add by yaochi �����
        primary.jjtAccept(this, data);// AST�������ֱ��f6
        ArrayList<Boolean> flags = node.getFlags();
        ArrayList<String> operators = node.getOperatorType();
        Expression currentvalue = expdata.value;
        VariableDomain currentVD = expdata.vd; // add by tangrong 2012-02-17
        CType currenttype = primary.getType();
        int j = 1;
        for (int i = 0; i < flags.size(); i++) {// ��i=1��ʱ���ǵڶ�ά������/�ṹ �Դ�����
            if (currentVD != null && currentVD.getImage().contains("const")) {
                break;
            }
            boolean flag = flags.get(i);
            String operator = operators.get(i);
            if (operator.equals("[")) {// ����
                Expression subindex = null;
                ASTExpression expression = null;
                if (flag) {
                    expression = (ASTExpression) node.jjtGetChild(j++);
                    expression.jjtAccept(this, data);// �õ��±�
                    subindex = ((PrimitiveVariableDomain) ((ExpressionVistorData) data).vd).getExpression();
                } else {
                    throw new RuntimeException("ASTPostfixExpression error!");
                }
                // zys:2010.9.13 ��Ϊ���Գ�����ʱ������
                if (currenttype == null) {
                    logger.error(primary.getBeginFileLine() + "�е����ͷ�������");
                    throw new RuntimeException(primary.getBeginFileLine() + "�е����ͷ�������");
                }
                CType atype = currenttype.getSimpleType();
                if (atype instanceof CType_AbstPointer) {// �����ָ�������

                    CType_AbstPointer ptype = (CType_AbstPointer) atype;
                    currenttype = ptype.getOriginaltype();

                    // add by tangrong 2012-7-6
                    /*
                     * ָ��Ĵ����5������p[i]Ϊ�� 1. ��ȡָ��p��ָ����ĳ����ڴ�m0�� 2.
                     * �ڿ�����ͼ�ڵ�VexNode�����ArrayVaribaleDomain����û���ĸ�����
                     * �����������ڴ�ռ䣩�к���m0��������ڷ��ش���������ڴ�ġ� 3.
                     * ���û�У��½�һ�������飨���������ڴ�ռ䣩��
                     * m0Ϊ�����е�һ��Ա�����������еĵ�ַ���±꣩Ϊ�½�����s��s��Լ��ʱ��Լ����0�� 4.
                     * ����p��ָ����m0�����������ڴ�ռ��е��±�
                     * ���������������ڴ����Ƿ���ڣ��±�+1)��Ԫ�أ������򷵻أ����������½�һ���ڴ浥Ԫm1
                     * ����<�±�+1��m1>�������������ڴ��С� 5. ִ��p=p+1�Ĳ�����
                     */// ����һ�� �����ͳ����ڴ�ģ�� memoryBlock
                    ArrayVariableDomain memoryBlock = null;
                    if (currentVD instanceof PointerVariableDomain) {// �����ǰ�����ڴ�ģ��Ϊָ�����ͳ����ڴ�ģ��
                        PointerVariableDomain currVD = (PointerVariableDomain) currentVD;
                        // ����1:(*pt)��ΪNULL����ȡ(*pt)���ڵĳ����ڴ�
                        if (currVD.getPointerTo() == null) { // currVDû��ָ��ĳһ�������ڴ��ַ���ʼ��currVD
                            currVD.initMemberVD();
                        }
                        currVD.setStateNotNull();
                        VariableDomain pt = currVD.getPointerTo();

                        // ����2 �����Ƿ����������ڴ��ַ�к���pt��
                        ValueSet vset = expdata.currentvex.getValueSet();
                        memoryBlock = vset.getMemoryBlockContainVD(pt);
                        /*
                         * if(pt instanceof ArrayVariableDomain){ memoryBlock =
                         * (ArrayVariableDomain)pt; }��ʱ����byyaochi 20130901
                         */

                        // ����3 ���û�У��½�һ�������飨���������ڴ�ռ䣩SymbolDomainSet
                        if (memoryBlock == null) {// ����һ������
                            VariableNameDeclaration vnd4MemoryBlock =
                                    new VariableNameDeclaration(node.getFileName(), node.getScope(), currentVD.getVariableNameDeclaration().getName().replace(".", "_") + "_arr", node, currentVD
                                            .getVariableNameDeclaration().getName() + "_arr");
                            // VariableNameDeclaration vnd4MemoryBlock = new
                            // VariableNameDeclaration(node.getFileName(),node.getScope(),
                            // currentVD.getVariableNameDeclaration().getName()+
                            // "_arr", node,
                            // currentVD.getVariableNameDeclaration().getName()+
                            // "_arr");
                            // vnd4MemoryBlock.setType(new
                            // CType_Array(pt.getType()));//������������
                            // modify by yaochi 20130423
                            if (i + 1 < flags.size()) {// ȷ������һ������
                                String nextoper = operators.get(i + 1);// �õ���һ������
                                if (nextoper.equals("[")) {// �����һ������Ϊ�������ͣ���ֵת��Ϊ��������
                                    vnd4MemoryBlock.setType(new CType_Array(new CType_Array(CType.getNextType(pt.getType()))));
                                } else {// ������ԭ������
                                    vnd4MemoryBlock.setType(new CType_Array(currenttype));
                                }
                            } else {// û����һ������Ҳ�Ǹ�ԭ��������
                                vnd4MemoryBlock.setType(new CType_Array(currenttype));
                            }
                            // modify by yaochi end
                            memoryBlock = new ArrayVariableDomain(vnd4MemoryBlock, currVD.getVariableSource(), expdata.currentvex);// ����һ���µ��������ͳ����ڴ�ռ䲢����memoryBlock
                            SymbolFactor factor = SymbolFactor.genSymbol(CType_BaseType.intType);
                            expdata.currentvex.addSymbolDomain(factor, new IntegerDomain(0, 0));
                            memoryBlock.addMember(new Expression(factor), pt);
                            if (vset.getValue((VariableNameDeclaration) currVD.getNd()) != null && (CType.getOrignType(primary.getType()) instanceof CType_Struct)
                                    || primary.getType() instanceof CType_Struct) {
                                // add by yaochi
                                // ���vset��û����ָ��ż���vest�У�ͬʱ��Ϊtypedef���ͷ���ֱ�Ӹı�pָ�򣬽ṹ������Ӧ�����ƣ�
                                ((PointerVariableDomain) currentVD).setPointTo(memoryBlock);
                                currentVD = memoryBlock;
                            } else {
                                expdata.currentvex.addValue(vnd4MemoryBlock, memoryBlock);
                                memoryBlock = vset.getMemoryBlockContainVD(pt);
                                ((PointerVariableDomain) currentVD).setPointTo(memoryBlock);
                            }
                        } else {// ����(a+i)[j]�������ʱ���±�Ӧ�ñ��i+j add by jinkaifeng
                                // 2012.11.1
                            Map<Expression, VariableDomain> members = memoryBlock.getMembers();
                            for (Expression key : members.keySet())
                                if (members.get(key).equals(pt)) {
                                    subindex = subindex.add(key);
                                    break;
                                }

                        }
                        /*
                         * if(currentVD instanceof PointerVariableDomain){
                         * if(((PointerVariableDomain) currentVD).getPointerTo()
                         * instanceof PrimitiveVariableDomain){
                         * //currentVDΪָ�룬������[]������˵���벻Ӧָ��һ�����ͣ�Ӧ������ָ�� add by
                         * yaochi 20130608 ((PointerVariableDomain)
                         * currentVD).setPointTo(memoryBlock); } }
                         */

                    } else if (currentVD instanceof ArrayVariableDomain) {
                        memoryBlock = (ArrayVariableDomain) currentVD;

                    }
                    // ����4 ����p[i]
                    if (Config.Field) {
                        expdata.vd = memoryBlock.getMember(subindex, expdata.currentvex.getSymDomainset());

                        if (memoryBlock.getMembers().containsKey(subindex)) {
                            expdata.vd = memoryBlock.getMembers().get(subindex);
                        } else {
                            // tangrong 2012-9-20���޸ģ�֮ǰ�ķ���ֻ��else���� Ϊ
                            // p[i]����variableDomain
                            VariableNameDeclaration vnd;// �����������ָ���image�е�С���⣬��֪���費��Ҫ��
                                                        // yaochi
                            String subindexString = subindex.getVND();
                            String image = memoryBlock.getVariableNameDeclaration().getName().replaceAll("[\\[\\]*]", "_") + "[" + subindexString + "]";// ��һ��������ʽ�����ڶ�ά���飬����ȷ
                                                                                                                                                        // ����
                                                                                                                                                        // x[0][0]
                                                                                                                                                        // �õ���
                                                                                                                                                        // x_0_[0]
                            // String image = "(" +
                            // memoryBlock.getVariableNameDeclaration().getName()+")"+"["
                            // + subindex + "]";
                            Scope scope = node.getScope();
                            NameDeclaration decl = Search.searchInVariableAndMethodUpward(image, scope);
                            if (decl == null) {// ��ֹ��춶�S���M���ɵ�image��ʽ���� �@�e�M���R�r���޸�
                                image = memoryBlock.getVariableNameDeclaration().getName() + "[" + subindexString + "]";
                                decl = Search.searchInVariableAndMethodUpward(image, scope);
                            }

                            if (decl instanceof VariableNameDeclaration) {
                                vnd = (VariableNameDeclaration) decl;
                                vnd.setParent(memoryBlock.getVariableNameDeclaration());
                                vnd = (VariableNameDeclaration) decl;
                            } else {// decl �п�����null ���±��Ǳ������ʽʱ
                                String tmp = node.getImage();
                                node.setImage(image);
                                vnd = new VariableNameDeclaration(node, subindex);// 2013-5-22
                                                                                  // ���simpleImage������
                                node.setImage(tmp);
                                // add by yaochi 20130423
                                if (i + 1 < flags.size()) {// ȷ������һ������
                                    String nextoper = operators.get(i + 1);// �õ���һ������
                                    if (nextoper.equals("[")) {// �����һ������Ϊ�������ͣ���ֵת��Ϊ��������
                                        vnd.setType(new CType_Array(CType.getNextType(currenttype)));// add
                                                                                                     // by
                                                                                                     // yaochi
                                    } else {// ������ԭ������
                                        vnd.setType(currenttype);
                                    }
                                } else {// û����һ������Ҳ�Ǹ�ԭ��������
                                    vnd.setType(currenttype);
                                }
                                // add by yaochi end
                            }
                            vnd.setParent(memoryBlock.getVariableNameDeclaration());
                            // ������һ�������ڴ�ģ�� memvd�������ű�����Ϣ
                            vnd.setExpSimpleImage(subindex);// by zxz 2013-5-27
                            VariableDomain memvd = VariableDomain.newInstance(vnd, memoryBlock.getVariableSource().next(), expdata.currentvex);
                            memoryBlock.addMember(subindex, memvd);
                            // add by jinkaifeng 2013.5.21
                            // �������±����0��Լ�����뵽symbolDomainSet��
                            Domain subdomain = subindex.getDomain(expdata.currentvex.getLastsymboldomainset());
                            if (subdomain != null && !subdomain.isConcrete()) {// �÷ǳ������±���м���
                                // Expression indexExp = new
                                ExpressionDomain.getExpressionDomain(subindex, new Expression(0), ">=", expdata.currentvex);
                                long arrayDimSize = ((ArrayVariableDomain) memoryBlock).getDimSize();
                                if (arrayDimSize != -1) {// ��������������һά�Ƕ����ģ�Ӧ�ø���һ�����������Ƴ���zxz
                                    ExpressionDomain.getExpressionDomain(subindex, new Expression(arrayDimSize), "<", expdata.currentvex);
                                }
                            }
                            // add end
                            expdata.vd = memvd;
                            currenttype = memvd.getType(); // add by tangrong
                                                           // 2012-9-20
                        }

                        // added 2012-12-17 ���i��֮ǰ���ֹ� ���� i =0 ; a[i] = A; ��ʱӦ��
                        // ��a[0]���в���zhangxuzhou
                        // Ŀǰֻ��һά������Ч
                        // 2013-5-24 ���������� a[i] ���֮ǰ��a[0] ������ߵ����䱣��һ��
                        // ���ڳ�����a[i]�ĵط�����Ҫ����������ж�Ӧ�±�����
                        Domain currentIndexDomain = subindex.getDomain(expdata.currentvex.getSymDomainset());
                        if (currentVD instanceof PrimitiveVariableDomain) {// ���Զ�����飬��ʱ����
                                                                           // yaochi
                            if (currentIndexDomain != null) {// ����һ���̶�ֵ�����ô�ֵ���±�
                                if (currentIndexDomain.isConcrete()) {
                                    Expression Consubindex = new Expression((currentIndexDomain.getConcreteDomain().longValue()));
                                    VariableDomain ConVd = memoryBlock.getMember(Consubindex, expdata.currentvex.getSymDomainset());
                                    VariableDomain VarVd = expdata.vd;
                                    if (ConVd != null) { // �������ཻ���ұ���һ��
                                        Domain ConMem = ((PrimitiveVariableDomain) ConVd).getExpression().getDomain(expdata.currentvex.getSymDomainset());
                                        Domain VarMem = ((PrimitiveVariableDomain) VarVd).getExpression().getDomain(expdata.currentvex.getSymDomainset());
                                        Domain mergeMem = Domain.intersect(ConMem, VarMem, VarVd.getType());
                                        SymbolFactor VarExp = (SymbolFactor) ((PrimitiveVariableDomain) VarVd).getExpression().getSingleFactor();
                                        SymbolFactor ConExp = (SymbolFactor) ((PrimitiveVariableDomain) ConVd).getExpression().getSingleFactor();

                                        // 2013-5-31 bug �������ʽ�� 77+mynode.a[1]
                                        // �õ���mergeMem���Ǳ��ʽ�����䣬
                                        // ���ʽ�����������symbolSet��û����ʾ����
                                        if (VarExp != null && ConExp != null) {
                                            expdata.currentvex.getSymDomainset().addDomain(VarExp, mergeMem);
                                            expdata.currentvex.getSymDomainset().addDomain(ConExp, mergeMem);
                                        }

                                        if (mergeMem == Domain.getEmptyDomainFromType(VarVd.getType())) {

                                        }
                                    }
                                } else if (false) { // ������鱾��Ķ����ģ�δ��
                                }
                            }
                        }
                        // added end 2012-12-17

                        currentVD = expdata.vd;
                        // data = expdata;
                        // return data;
                    }
                    // add end

                    if (currentvalue != null) {// domain = null ?
                        Domain domain = Domain.castToType(currentvalue.getDomain(expdata.currentvex.getSymDomainset()), atype);
                        if (domain instanceof PointerDomain) {
                            PointerDomain pdomain = (PointerDomain) domain;
                            SymbolFactor sym = SymbolFactor.genSymbol(atype);
                            if (currenttype.getSimpleType() instanceof CType_Array) {
                                CType_Array temp = (CType_Array) currenttype.getSimpleType();
                                if (temp.isFixed()) {
                                    pdomain = new PointerDomain();
                                    pdomain.setElementtype(currenttype);
                                    pdomain.setLength(new Expression(temp.getDimSize()));
                                    expdata.currentvex.addSymbolDomain(sym, pdomain);;

                                }
                            }
                            currentvalue = new Expression(sym);
                        }
                    }
                }
            } else if (operator.equals("(")) { // ����ָ��ĺ����Ŵ���������Ŷ
                ASTArgumentExpressionList expressionlist = null;
                // add by zhouhb
                // ��ָ����غ����Ĵ���
                // ������ģ������
                // add by zhouhb 2010/10/19
                // if(node.getImage().contains("malloc")||node.getImage().contains("Malloc")||node.getImage().contains("malloc"))
                if (node.getImage().equals("malloc") || node.getImage().equals("calloc")) {
                    // �����˶�malloc�����а��������������õ��жϣ��Դ����δ֪�������账��
                    // add by zhouhb 2010/8/18
                    if (((SimpleNode) node.jjtGetChild(1)).containsChildOfType(ASTPrimaryExpression.class)) {
                        List<Node> primarys = ((SimpleNode) node.jjtGetChild(1)).findChildrenOfType(ASTPrimaryExpression.class);
                        for (Node pri : primarys) {
                            if (pri.jjtGetNumChildren() == 0) {
                                VariableNameDeclaration v = ((SimpleNode) pri).getVariableNameDeclaration();
                                if (v != null && v.getScope() instanceof SourceFileScope)
                                    return data;
                            }
                        }
                    }
                    node.jjtGetChild(1).jjtAccept(this, expdata);
                    if (node.containsChildOfType(ASTTypeName.class)) {
                        ASTTypeName type = (ASTTypeName) node.getFirstChildOfType(ASTTypeName.class);
                        expdata.value = expdata.value.div(new Expression(type.getType().getSize()));
                    }
                    // ����malloc(10)ʱ��������Ϣ�����޸�
                    // modified by zhouhb 2010/7/19
                    long mallocsize;
                    IntegerDomain size = IntegerDomain.castToIntegerDomain(expdata.value.getDomain(expdata.currentvex.getSymDomainset()));
                    // ���malloc����ռ��Բ������ݽ�����Ĭ�Ϸ��������
                    if (size == null || size.isUnknown() || size.getIntervals().isEmpty()) {
                        mallocsize = IntegerDomain.DEFAULT_MAX;
                    } else {
                        mallocsize = (int) size.getIntervals().first().getMin();
                    }
                    SymbolFactor sym = SymbolFactor.genSymbol(node.getType());
                    PointerDomain p = new PointerDomain();
                    p.offsetRange = new IntegerDomain(0, mallocsize - 1);
                    // p.AllocType=CType_AllocType.heapType;
                    if (p.Type.contains(CType_AllocType.NotNull)) {
                        p.Type.remove(CType_AllocType.NotNull);
                    }
                    p.Type.add(CType_AllocType.heapType);
                    p.setValue(PointerValue.NULL_OR_NOTNULL);
                    expdata.currentvex.addSymbolDomain(sym, p);
                    expdata.value = new Expression(sym);

                    // add by jinkaifeng 2013.1.9
                    // Ϊ�����ڴ�ģ��malloc��Ϊ�����ɵ�ָ��ֻ��notnull��ôһ����Ϣ
                    VariableNameDeclaration vnd4AnnoyPtr = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoyPtr_malloc", node, "annoyPtr_malloc");
                    vnd4AnnoyPtr.setType(new CType_Pointer(node.getType()));
                    PointerVariableDomain annoyPtr = new PointerVariableDomain(vnd4AnnoyPtr, expdata.currentvex);
                    annoyPtr.setStateNotNullForMalloc();
                    expdata.vd = annoyPtr;
                    // add end
                    return data;
                } else if (node.getImage().equals("free")) {
                    node.jjtGetChild(1).jjtAccept(this, expdata);
                    PointerDomain p = new PointerDomain();
                    p.offsetRange = new IntegerDomain(0, 0);
                    // p.AllocType=CType_AllocType.Null;
                    p.Type.add(CType_AllocType.Null);
                    p.setValue(PointerValue.NULL);
                    ASTPrimaryExpression pri = (ASTPrimaryExpression) ((SimpleNode) node.jjtGetChild(1)).getFirstChildOfType(ASTPrimaryExpression.class);
                    VariableNameDeclaration v = pri.getVariableDecl();
                    // �ṹ���Ա��free�����Դ���
                    // add by zhouhb 2010/8/18
                    if (v != null && (v.getType() instanceof CType_Pointer && CType_Pointer.getOrignType(v.getType()) instanceof CType_Struct)) {
                        // PointerVariableDomain vd =
                        // (PointerVariableDomain)expdata.vd;
                        PointerVariableDomain vd = PointerVariableDomain.NullPointer(expdata.currentvex);
                        expdata.vd = vd;
                        return data;
                    }
                    Expression ve = null;
                    if (v != null)
                        ve = expdata.currentvex.getValue(v);
                    if (ve == null || ve != null && !(ve.getSingleFactor() instanceof SymbolFactor))
                        return data;
                    SymbolFactor temp = (SymbolFactor) ve.getSingleFactor();
                    // ����ͷŶ���ָ�������㣬��ve��ΪSingleFactor(eg.1+S)�����ͷŲ��ɹ�
                    if (temp == null) {
                        return data;
                    } else {
                        expdata.currentvex.addSymbolDomain(temp, p);
                        expdata.value = new Expression(temp);
                        return data;
                    }
                }
                if (flag || currentVD instanceof FunctionVariableDomain) {// ������û��ʵ�εı�־λ
                    MethodSet ms = expdata.currentvex.getMethodSet();
                    if (!flag) {
                        FunctionVariableDomain currentTmp = (FunctionVariableDomain) currentVD;
                        currentTmp.addCalledIndex();
                        currentTmp.beCalled();
                        ms.addFuncVD(currentTmp);
                    }
                    if (flag && currentVD != null) {
                        expressionlist = (ASTArgumentExpressionList) node.jjtGetChild(j++); // �����Ĳ����������õ�
                        // expressionlist.jjtAccept(this, data);

                        while (currentVD != null && currentVD instanceof PointerVariableDomain) {// ָ������ʾ�����
                            VariableDomain childvd = ((PointerVariableDomain) currentVD).getPointerTo();
                            if (childvd == null) {
                                MethodNameDeclaration mnd = new MethodNameDeclaration(currentVD.getVariableNameDeclaration());
                                childvd = VariableDomain.newInstance(mnd, expdata.currentvex);
                            }
                            currentVD = childvd;
                        }

                        FunctionVariableDomain currentTmp = (FunctionVariableDomain) currentVD;

                        if (currentTmp.getNd() instanceof MethodNameDeclaration) {
                            // �ռ���׮��Ҫ����Ϣ-ʵ�γ��ֵľ���λ��
                            // �������ͺ��ڵ��ô���+1
                            if (currentTmp instanceof FunctionVariableDomain) {
                                currentTmp.addCalledIndex();
                                currentTmp.beCalled();
                                ms.addFuncVD(currentTmp);
                            }
                            ArrayList<MethodArgument> malist = new ArrayList<MethodArgument>();
                            ArrayList<VariableDomain> globallist = new ArrayList<VariableDomain>();
                            if (currentTmp instanceof FunctionVariableDomain && currentTmp.isIOlib()) {
                                try {
                                    malist =
                                            ((IOFunctionVariableDomain) currentTmp).getIOfunctionArguments(expdata, expressionlist, this, node.getScope(), ms,
                                                    expdata.currentvex.getLastsymboldomainset());
                                } catch (Exception e) {
                                    malist.clear();
                                    for (int childindex = 0; childindex < expressionlist.jjtGetNumChildren(); childindex++) {
                                        ExpressionVistorData childdata = new ExpressionVistorData();
                                        childdata.currentvex = (VexNode) expdata.currentvex.clone();
                                        expressionlist.jjtGetChild(childindex).jjtAccept(this, childdata);// �õ������ڵ��vd
                                        MethodArgument ma = new MethodArgument(childdata.vd, childindex);
                                        malist.add(ma);
                                    }
                                }
                            } else
                                for (int childindex = 0; childindex < expressionlist.jjtGetNumChildren(); childindex++) {
                                    expressionlist.jjtGetChild(childindex).jjtAccept(this, expdata);// �õ������ڵ��vd
                                    /*
                                     * if(!(childdata.vd instanceof
                                     * PrimitiveVariableDomain) &&
                                     * !(childdata.vd instanceof
                                     * FunctionVariableDomain) &&
                                     * childdata.vd.getNd() != null){
                                     */// ����(char*)0�����Ĳ�����Ӧ����������У������ж�
                                       // if(childdata.vd.getNd() != null){

                                    if (null != expdata.vd) {
                                        MethodArgument ma = new MethodArgument(expdata.vd, childindex);

                                        malist.add(ma);
                                    } else {

                                    }
                                    // }
                                }
                            currentTmp.addActualParamList(malist);
                            currentTmp.addGlobalArgumentList(globallist);

                        }
                    }
                    if (currentVD instanceof FunctionVariableDomain) {
                        VariableDomain retVD = null;
                        int callIndex = ((FunctionVariableDomain) currentVD).getCalledIndex();
                        if (callIndex > 0 && ((FunctionVariableDomain) currentVD).getRetvdList().size() >= callIndex) {
                            // if vd
                            retVD = ((FunctionVariableDomain) currentVD).getRetvdList().get(callIndex - 1);
                            currentVD = retVD;
                            currenttype = currentVD.getType();
                            expdata.currentvex.addVariableDomain(retVD);
                            continue;
                        }
                        CType retType = ((FunctionVariableDomain) currentVD).getReturnType();
                        VariableNameDeclaration vnd =
                                new VariableNameDeclaration(node.getFileName(), node.getScope(), "ret_" + ((FunctionVariableDomain) currentVD).getNd().getImage() + callIndex, node, "ret_base");
                        vnd.setType(retType);
                        while (retType instanceof CType_Typedef) {
                            retType = CType.getNextType(retType);
                        }
                        if (retType instanceof CType_BaseType || retType instanceof CType_Enum) {
                            retVD = new PrimitiveVariableDomain(vnd, currentVD.getVariableSource(), expdata.currentvex);
                        } else if (retType instanceof CType_Array) {
                            retVD = new ArrayVariableDomain(vnd, currentVD.getVariableSource(), expdata.currentvex);
                        } else if (retType instanceof CType_Struct) {
                            retVD = new StructVariableDomain(vnd, currentVD.getVariableSource(), expdata.currentvex);
                        } else if (retType instanceof CType_Pointer) {
                            retVD = new PointerVariableDomain(vnd, currentVD.getVariableSource(), expdata.currentvex);
                            if (!retVD.getType().toString().contains("void *"))
                                ((PointerVariableDomain) retVD).initMemberVD();
                            if (CType.getOrignType(retType) instanceof CType_Function) {
                                MethodNameDeclaration mnd = new MethodNameDeclaration(vnd);
                                FunctionVariableDomain childfvd = new FunctionVariableDomain(vnd, currentVD.getVariableSource(), expdata.currentvex);
                                ms.addValue(mnd, childfvd);// ����һ������
                                ((PointerVariableDomain) retVD).setPointTo(childfvd);
                            }
                        }
                        if (retVD != null) {
                            retVD.setVariableSource(VariableSource.INPUT_ANNOMY);
                            expdata.currentvex.addValue(vnd, retVD);// ����valueset��
                            SymbolFactor factor = SymbolFactor.genSymbol(retVD.getType());
                            expdata.currentvex.addSymbolDomain(factor, Domain.getFullDomainFromType(retVD.getType()));
                            ((FunctionVariableDomain) currentVD).addActualRetvd(retVD); // �������ú����ķ���ֵ
                            currentVD = retVD;
                            currenttype = currentVD.getType();
                        }
                    }
                }
            } else if (operator.equals(".")) {
                ASTFieldId field = null;
                if (flag) {
                    field = (ASTFieldId) node.jjtGetChild(j++);
                    field.jjtAccept(this, data);
                    if (Config.Field) {
                        Scope scope = node.getScope();
                        // NameDeclaration
                        // decl=Search.searchInVariableAndMethodUpward(node.getImage(),
                        // scope);//(*s).v.i.data
                        String image1 = currentVD.getNd().getImage() + "." + field.getImage();
                        NameDeclaration decl = Search.searchInVariableAndMethodUpward(image1, scope);// node����Ӧ����postfix����postfix��Ӧ��image���ܵ��﷨����image��������Ҫǰһ�ε�
                        if (decl == null) {// add by yaochi�ݴ���
                            String image2 = "(" + currentVD.getNd().getImage() + ")" + "." + field.getImage();
                            decl = Search.searchInVariableAndMethodUpward(image2, scope);// node����Ӧ����postfix����postfix��Ӧ��image���ܵ��﷨����image��������Ҫǰһ�ε�

                        }
                        if (decl == null && (currenttype instanceof CType_Struct)) {// add
                                                                                    // by
                                                                                    // yaochi
                                                                                    // CType_Struct
                                                                                    // �ҽṹ��ĳ�Ա��û�ҵ���Ҫ�½�
                            decl = new VariableNameDeclaration(node.getFileName(), node.getScope(), image1, node, field.getImage());
                            decl.setType(((CType_Struct) currenttype).getCType(field.getImage()));
                            ((VariableNameDeclaration) decl).setParent((VariableNameDeclaration) currentVD.getNd());
                        }
                        if (decl == null && (currenttype instanceof CType_Typedef) && CType.getOrignType(currenttype) instanceof CType_Struct) {// add
                                                                                                                                                // by
                                                                                                                                                // yaochi
                                                                                                                                                // CType_Struct
                                                                                                                                                // �ҽṹ��ĳ�Ա��û�ҵ���Ҫ�½�
                            decl = new VariableNameDeclaration(node.getFileName(), node.getScope(), image1, node, field.getImage());
                            CType origtype = CType.getOrignType(currenttype);
                            decl.setType(((CType_Struct) origtype).getCType(field.getImage()));
                            ((VariableNameDeclaration) decl).setParent((VariableNameDeclaration) currentVD.getNd());
                        }// end yaochi 20130531

                        if (decl instanceof VariableNameDeclaration) {
                            // ��������
                            VariableNameDeclaration v = (VariableNameDeclaration) decl;
                            StructVariableDomain svd = (StructVariableDomain) expdata.currentvex.getVariableDomain((VariableNameDeclaration) currentVD.getNd());// add
                                                                                                                                                                // by
                                                                                                                                                                // yaochi
                                                                                                                                                                // 20130529
                            currentvalue = expdata.currentvex.getValue(v);
                            currentVD = expdata.currentvex.getVariableDomain(v);
                            if (currentVD == null) {
                                // add by tangrong 2012-5-7 Ϊ�ṹ����ӳ�Ա��
                                // StructVariableDomain svd =
                                // (StructVariableDomain)
                                // expdata.currentvex.getVariableDomain(v.getParent());
                                String simpleImage = v.getSimpleImage();
                                if (svd != null) {
                                    if (svd.getMembers().containsKey(simpleImage)) {
                                        currentVD = svd.getMembers().get(simpleImage);// add by
                                                                                      // jinkaifeng
                                                                                      // 2013.5.15����ṹ�����Ѿ��������Ա�������������µ�vd
                                    } else {
                                        currentVD = VariableDomain.newInstance(v, svd.getVariableSource().next(), expdata.currentvex);
                                        svd.addMember(v.getSimpleImage(), currentVD);
                                    }
                                } else {

                                }
                            }

                        }
                        // ���ڳ�Ա�Ѵ����ڽṹ���е��Ҳ���decl����� add by yaochi 2013-09-04
                        if (currentVD instanceof StructVariableDomain) {
                            VariableDomain mem = ((StructVariableDomain) currentVD).getMember(field.getImage());
                            if (mem != null) {
                                currentVD = mem;
                            }
                        }// end

                    }

                    if (currentVD != null)
                        currenttype = currentVD.getType(); // add by yaochi
                } else {
                    throw new RuntimeException("ASTPostfixExpression error!");
                }
                if (Config.Field) {
                    if (currentvalue == null) {
                        currentvalue = new Expression(SymbolFactor.genSymbol(field.getType()));
                    }
                } else
                    currentvalue = new Expression(SymbolFactor.genSymbol(field.getType()));
            } else if (operator.equals("->")) {
                ASTFieldId field = null;
                if (flag) {
                    field = (ASTFieldId) node.jjtGetChild(j++);
                    field.jjtAccept(this, data);
                    if (Config.Field) {
                        Scope scope = node.getScope();
                        NameDeclaration decl = Search.searchInVariableAndMethodUpward(field.getImage(), scope);
                        if (decl == null) {
                            // �ṹ���а���ָ����һ�ṹ���ָ��ʱ(Struct.otherStruct.data)�����ܴ���field.image������������Ҫ��ȫ
                            // add by yaochi 130617
                            decl = Search.searchInVariableAndMethodUpward(node.getImage(), scope);
                        }
                        if (decl instanceof VariableNameDeclaration) {
                            // ��������
                            VariableNameDeclaration v = (VariableNameDeclaration) decl;
                            PointerVariableDomain tmpVD = (PointerVariableDomain) currentVD;// ����"->"�еĶ�Ϊָ������VD
                            currentvalue = expdata.currentvex.getValue(v);
                            // add by tangrong 2012-02-17
                            currentVD = expdata.currentvex.getVariableDomain(v);
                            if (currentVD == null) { // ��û��Ϊ��ָ���������ڴ�
                                VariableNameDeclaration pvnd = v;
                                VariableDomain parentVD = null;
                                /*
                                 * while( parentVD == null ){ pvnd =
                                 * pvnd.getParent(); if(pvnd == null) break;
                                 * parentVD =
                                 * expdata.currentvex.getVariableDomain(pvnd); }
                                 */
                                if (parentVD == null) {// modify by yaochi
                                                       // ʹ��pvnd�ĵõ����ڽṹ������ʱ�������⣬�������Σ�����ND���Ҹ��ڵ�
                                    pvnd = (VariableNameDeclaration) tmpVD.getNd();// tmpVDΪpvnd�ĸ��ڵ�
                                    if (pvnd != null)
                                        parentVD = expdata.currentvex.getVariableDomain(pvnd);
                                }
                                if (parentVD == null)
                                    parentVD = tmpVD;
                                if (parentVD instanceof PointerVariableDomain) {
                                    // ((PointerVariableDomain)
                                    // parentVD).setPointToNull( );
                                    ((PointerVariableDomain) parentVD).initMemberVD();
                                    StructVariableDomain svd = null;
                                    // add by xujiaoxian 2012-10-18
                                    if (((PointerVariableDomain) parentVD).getPointerTo() instanceof StructVariableDomain) {
                                        svd = (StructVariableDomain) ((PointerVariableDomain) parentVD).getPointerTo();
                                    } else if (((PointerVariableDomain) parentVD).getPointerTo() instanceof ArrayVariableDomain) {
                                        ArrayVariableDomain avd = (ArrayVariableDomain) ((PointerVariableDomain) parentVD).getPointerTo();
                                        Expression exp = new Expression(0);
                                        for (Expression e : avd.getMembers().keySet()) {
                                            if (e.toString().equals("0")) {
                                                exp = e;
                                                break;
                                            }
                                        }
                                        if (avd.getMembers().containsKey(exp)) {
                                            svd = (StructVariableDomain) avd.getMember(exp);
                                        } else {
                                            String image = avd.getVariableNameDeclaration().getName().replaceAll("[\\[\\]*]", "_") + "[0]";
                                            decl = Search.searchInVariableAndMethodUpward(image, node.getScope());
                                            VariableNameDeclaration vnd;
                                            if (decl instanceof VariableNameDeclaration) {
                                                vnd = (VariableNameDeclaration) decl;
                                                vnd.setParent(avd.getVariableNameDeclaration());
                                                vnd = (VariableNameDeclaration) decl;
                                            } else {
                                                String tmp = node.getImage();
                                                node.setImage(image);
                                                vnd = new VariableNameDeclaration(node, exp);// 2013-5-22
                                                                                             // ���simpleImage������
                                                node.setImage(tmp);
                                                vnd.setType(CType.getNextType(currenttype));
                                                vnd.setParent(avd.getVariableNameDeclaration());
                                            }
                                            // ������һ�������ڴ�ģ�� memvd�������ű�����Ϣ
                                            VariableDomain memvd = VariableDomain.newInstance(vnd, avd.getVariableSource().next(), expdata.currentvex);
                                            vnd.setExpSimpleImage(exp);// by zxz
                                                                       // 2013-5-27
                                            avd.addMember(exp, memvd);
                                            svd = (StructVariableDomain) memvd;
                                        }
                                    }
                                    VariableDomain vdmem = null;
                                    if (svd != null)
                                        vdmem = svd.getMember(v.getSimpleImage());// vdmem�õ�struct.element
                                    if (vdmem != null) {
                                        currentVD = vdmem;
                                    } else {
                                        // end add by xujiaoxian 2012-10-18
                                        vdmem = VariableDomain.newInstance(v, parentVD.getVariableSource().next(), expdata.currentvex);
                                        currentVD = VariableDomain.newInstance(v, parentVD.getVariableSource().next(), expdata.currentvex);
                                        ((PointerVariableDomain) parentVD).addArrowMember(v.getSimpleImage(), currentVD);// ����->��ʾ��ָ���chengy
                                        VariableDomain tmpvd = currentVD;
                                        while (tmpvd instanceof PointerVariableDomain) {// add
                                                                                        // by
                                                                                        // yaochi
                                                                                        // 130625
                                                                                        // ���node->left->left.element���Ǹ�*void(0)����
                                            ((PointerVariableDomain) tmpvd).setStateNotNull();
                                            tmpvd = ((PointerVariableDomain) tmpvd).getPointerTo();
                                        }
                                    }
                                    ((PointerVariableDomain) parentVD).setStateNotNull();
                                } else {
                                    currentVD = VariableDomain.newInstance(v, parentVD.getVariableSource().next(), expdata.currentvex);
                                    ((StructVariableDomain) parentVD).addMember(v.getSimpleImage(), currentVD);
                                }
                                currenttype = currentVD.getType(); // add by
                                                                   // tangrong
                                                                   // 2012-9-20

                            }
                            currenttype = currentVD.getType(); // add by yaochi
                                                               // 2013-05-23
                        }
                    }
                } else {
                    throw new RuntimeException("ASTPostfixExpression error!");
                }
                if (Config.Field) {
                    if (currentvalue == null) {
                        currentvalue = new Expression(SymbolFactor.genSymbol(field.getType()));
                    }
                } else
                    currentvalue = new Expression(SymbolFactor.genSymbol(field.getType()));
            } else if (operator.equals("++")) { // ��++ data++ p = null
                ASTPrimaryExpression p = (ASTPrimaryExpression) ((SimpleNode) node.jjtGetChild(j - 1)).getSingleChildofType(ASTPrimaryExpression.class);
                if (p == null) {
                    p = (ASTPrimaryExpression) ((SimpleNode) node).getChildofType(ASTPrimaryExpression.class);
                }
                if (currentVD instanceof PrimitiveVariableDomain && expdata.sideeffect) {// add by
                                                                                         // jinkaifeng
                                                                                         // 2012.10.10
                    PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) (currentVD);// tong
                                                                                        // shang
                    currentvalue = pvd.getExpression();
                    // Expression temp = currentvalue.add(new Expression(1));
                    Expression temp = pvd.getExpression().add(new Expression(1));

                    pvd.setExpression(temp);// add by jinkaifeng
                    /*
                     * PrimitiveVariableDomain origVD = new
                     * PrimitiveVariableDomain(pvd);
                     * origVD.setExpression(currentvalue); //��Ϊ�Ǻ�++��Ӧ�÷���+1ǰ���Ǹ�ֵ
                     * modified by tangorng 2012-11-1
                     */// modify by jinkaifeng ���Ӧ���Ƶ�sideeffect=false
                    currentVD = pvd;
                    if (p != null) {
                        VariableNameDeclaration v = p.getVariableDecl();
                        if (v != null && expdata.sideeffect && expdata.currentvex != null) {
                            expdata.currentvex.addValue(v, temp);
                            // expdata.currentvex.addValue(v, pvd);//add by
                            // jinkaifeng

                        }
                    }
                    if (Config.Field) {
                        VariableNameDeclaration v = node.getVariableDecl();// ?yaochi
                        if (v != null && expdata.sideeffect && expdata.currentvex != null && expdata.value != null) {
                            expdata.currentvex.addValue(v, temp);
                            // expdata.currentvex.addValue(v, pvd);
                        }
                    }
                }
                if (currentVD instanceof PrimitiveVariableDomain && !expdata.sideeffect) {// add by
                                                                                          // jinkaifeng
                                                                                          // 2013.03.29
                    PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) (currentVD);// tong
                                                                                        // shang
                    currentvalue = pvd.getExpression();
                    Expression temp = currentvalue.add(new Expression(1));
                    // Expression temp = pvd.getExpression().add(new
                    // Expression(1));
                    pvd.setExpression(temp);// add by jinkaifeng
                    PrimitiveVariableDomain origVD = new PrimitiveVariableDomain(pvd);
                    origVD.setExpression(currentvalue); // ��Ϊ�Ǻ�++��Ӧ�÷���+1ǰ���Ǹ�ֵ
                                                        // modified by tangorng
                                                        // 2012-11-1
                    currentVD = origVD;
                }
                // add by tangrong 2012-7-3 ���ڳ����ڴ�ģ��֧����ֵ���͵�++������ָ���++������
                /*
                 * ָ��Ĵ����5������p++Ϊ�� 1. ��ȡָ��p��ָ����ĳ����ڴ�m0�� 2.
                 * �ڿ�����ͼ�ڵ�VexNode�����ArrayVaribaleDomain����û���ĸ�����
                 * �����������ڴ�ռ䣩�к���m0��������ڷ��ش���������ڴ�ġ� 3.
                 * ���û�У��½�һ�������飨���������ڴ�ռ䣩��m0Ϊ�����е�һ��Ա
                 * �����������еĵ�ַ���±꣩Ϊ�½�����s��s��Լ��ʱ��Լ����0�� 4.
                 * ����p��ָ����m0�����������ڴ�ռ��е��±꣬�������������ڴ����Ƿ����
                 * ���±�+1)��Ԫ�أ������򷵻أ����������½�һ���ڴ浥Ԫm1����<�±�+1��m1>�������������ڴ��С� 5.
                 * ִ��p=p+1�Ĳ�����
                 */
                if (currentVD instanceof PointerVariableDomain) {

                    // ����1:(*p)��ΪNULL����ȡ(*p)���ڵĳ����ڴ�
                    PointerVariableDomain varDomain = (PointerVariableDomain) currentVD;
                    if (varDomain.getPointerTo() == null) {
                        varDomain.initMemberVD();
                    }
                    varDomain.setStateNotNull();
                    VariableDomain pt = varDomain.getPointerTo();

                    // ����2 �����Ƿ����������ڴ��ַ�к���pt��
                    ValueSet vset = expdata.currentvex.getValueSet();
                    ArrayVariableDomain memoryBlock = vset.getMemoryBlockContainVD(pt);

                    // ����3 ���û�У��½�һ�������飨���������ڴ�ռ䣩
                    Expression addr;
                    if (memoryBlock == null) {
                        // VariableNameDeclaration vnd4MemoryBlock = new
                        // VariableNameDeclaration(p.getFileName(),
                        // p.getScope(),
                        // varDomain.getVariableNameDeclaration().getName()+
                        // "_arr", node,
                        // varDomain.getVariableNameDeclaration().getName()+
                        // "_arr");
                        VariableNameDeclaration vnd4MemoryBlock =
                                new VariableNameDeclaration(p.getFileName(), p.getScope(), varDomain.getVariableNameDeclaration().getName().replaceAll("[\\[\\]*]", "_") + "_arr", node, varDomain
                                        .getVariableNameDeclaration().getName() + "_arr"); // modify
                                                                                           // by
                                                                                           // yaochi
                        vnd4MemoryBlock.setType(new CType_Array(pt.getType()));
                        memoryBlock = new ArrayVariableDomain(vnd4MemoryBlock, currentVD.getVariableSource(), expdata.currentvex);
                        SymbolFactor factor = SymbolFactor.genSymbol(CType_BaseType.intType);
                        // add by yaochi 140421
                        String addrStr;
                        if (varDomain != null && varDomain.getNd() != null)
                            addrStr = varDomain.getNd().getImage().replaceAll("[\\[|\\]|(|)|*]", "_") + "_" + "arrayAddrBase";
                        else
                            addrStr = "arrayAddrBase";
                        VariableNameDeclaration pvnd = new VariableNameDeclaration(node.getFileName(), node.getScope(), addrStr, node, addrStr);
                        pvnd.setType(CType_BaseType.intType);
                        // VariableSource.INPUT_ANNOMY
                        PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) VariableDomain.newInstance(pvnd, expdata.currentvex);
                        pvd.setVariableSource(VariableSource.INPUT_ANNOMY);
                        expdata.currentvex.addVariableDomain(pvd);
                        factor = pvd.getCurrentSymbolFactor();

                        expdata.currentvex.addSymbolDomain(factor, new IntegerDomain(0, Long.MAX_VALUE));
                        addr = new Expression(factor);
                        memoryBlock.addMember(addr, pt);
                        expdata.currentvex.addValue(vnd4MemoryBlock, memoryBlock);// modify by
                                                                                  // yaochi 20130527
                    } else {
                        addr = memoryBlock.getMember(pt);
                    }

                    // ����4 ��ַƫ��1��addr+1,�����Ƿ�����±�Ϊaddr+1��Ӧ��Ԫ�أ������򷵻أ��������½���
                    Expression addrPlusOne = addr.add(new Expression(1));
                    String subIndex = addrPlusOne.getVND();
                    VariableDomain ptPlusOne = memoryBlock.getMember(addrPlusOne);

                    if (ptPlusOne == null) {
                        String memName = memoryBlock.getVariableNameDeclaration().getName() + "[" + subIndex + "]";
                        VariableNameDeclaration vndPlusOne = new VariableNameDeclaration(p.getFileName(), p.getScope(), memName, node, addrPlusOne.toString());
                        vndPlusOne.setType(pt.getType());
                        vndPlusOne.setExpSimpleImage(addrPlusOne);
                        vndPlusOne.setParent(memoryBlock.getVariableNameDeclaration());
                        ptPlusOne = VariableDomain.newInstance(vndPlusOne, pt.getVariableSource().next(), expdata.currentvex);
                        memoryBlock.addMember(addrPlusOne, ptPlusOne);
                    }

                    // ����5 ִ��p=p+1�Ĳ���
                    if (ptPlusOne instanceof PointerVariableDomain) {// add by
                                                                     // yaochi
                                                                     // pt++��
                                                                     // ������parentΪcurrent��parent2013-09-03
                        PointerVariableDomain pvd = (PointerVariableDomain) ptPlusOne;
                        if (pvd.getPointerTo() != null)
                            ((VariableNameDeclaration) pvd.getPointerTo().getNd()).setParent(currentVD.getVariableNameDeclaration());
                    }
                    ((PointerVariableDomain) currentVD).changePT(ptPlusOne);
                }
                // add end

            } else if (operator.equals("--")) {
                ASTPrimaryExpression p = (ASTPrimaryExpression) ((SimpleNode) node.jjtGetChild(j - 1)).getSingleChildofType(ASTPrimaryExpression.class);
                if (currentVD instanceof PrimitiveVariableDomain && expdata.sideeffect) {
                    PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) currentVD;
                    Expression temp = pvd.getExpression().sub(new Expression(1));// add by yaochi
                    // Expression temp = currentvalue.sub(new Expression(1));
                    pvd.setExpression(temp);// add by jinkaifeng 2012.10.10
                    currentVD = pvd;// modify by yaochi 2013.05.06
                    if (p != null) {
                        VariableNameDeclaration v = p.getVariableDecl();
                        if (v != null && expdata.sideeffect && expdata.currentvex != null) {
                            expdata.currentvex.addValue(v, temp);
                            // expdata.currentvex.addValue(v, currentVD);//add
                            // by jinkaifeng 2012.10.10 modify by yaochi
                            // 2013.05.06
                        }
                    }
                    if (Config.Field) {
                        VariableNameDeclaration v = node.getVariableDecl();
                        if (v != null && expdata.sideeffect && expdata.currentvex != null && expdata.value != null) {
                            expdata.currentvex.addValue(v, temp);
                        }
                    }
                }
                // modify by yaochi 2013.05.06
                if (currentVD instanceof PrimitiveVariableDomain && !expdata.sideeffect) {
                    PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) (currentVD);
                    currentvalue = pvd.getExpression();
                    Expression temp = currentvalue.sub(new Expression(1));
                    pvd.setExpression(temp);
                    PrimitiveVariableDomain origVD = new PrimitiveVariableDomain(pvd);
                    origVD.setExpression(currentvalue); // ��Ϊ�Ǻ�++��Ӧ�÷���+1ǰ���Ǹ�ֵ
                                                        // modified by tangorng
                                                        // 2012-11-1
                    currentVD = origVD;
                }
                // end yaochi
                // add by tangrong 2012-7-5
                /*
                 * ָ��Ĵ����5������p--Ϊ�� 1. ��ȡָ��p��ָ����ĳ����ڴ�m0�� 2.
                 * �ڿ�����ͼ�ڵ�VexNode�����ArrayVaribaleDomain����û���ĸ�����
                 * �����������ڴ�ռ䣩�к���m0��������ڷ��ش���������ڴ�ġ� 3.
                 * ���û�У��½�һ�������飨���������ڴ�ռ䣩��m0Ϊ�����е�һ��Ա
                 * �����������еĵ�ַ���±꣩Ϊ�½�����s��s��Լ��ʱ��Լ����0�� 4.
                 * ����p��ָ����m0�����������ڴ�ռ��е��±꣬�������������ڴ����Ƿ����
                 * ���±�+1)��Ԫ�أ������򷵻أ����������½�һ���ڴ浥Ԫm1����<�±�+1��m1>�������������ڴ��С� 5.
                 * ִ��p=p-1�Ĳ�����
                 */
                if (currentVD instanceof PointerVariableDomain) {
                    // ����1:(*p)��ΪNULL����ȡ(*p)���ڵĳ����ڴ�
                    PointerVariableDomain varDomain = (PointerVariableDomain) currentVD;
                    if (varDomain.getPointerTo() == null) {
                        varDomain.initMemberVD();
                    }
                    varDomain.setStateNotNull();
                    VariableDomain pt = varDomain.getPointerTo();

                    // ����2 �����Ƿ����������ڴ��ַ�к���pt��
                    ValueSet vset = expdata.currentvex.getValueSet();
                    ArrayVariableDomain memoryBlock = vset.getMemoryBlockContainVD(pt);

                    // ����3 ���û�У��½�һ�������飨���������ڴ�ռ䣩
                    Expression addr;
                    if (memoryBlock == null) {
                        VariableNameDeclaration vnd4MemoryBlock = new VariableNameDeclaration(p.getFileName(), p.getScope(), "annoy0", node, "annoy0");
                        vnd4MemoryBlock.setType(new CType_Array(pt.getType()));
                        memoryBlock = new ArrayVariableDomain(vnd4MemoryBlock, currentVD.getVariableSource(), expdata.currentvex);
                        SymbolFactor factor = SymbolFactor.genSymbol(CType_BaseType.intType);

                        String addrStr;
                        if (varDomain != null && varDomain.getNd() != null)
                            addrStr = varDomain.getNd().getImage().replaceAll("[\\[|\\]|(|)|*]", "_") + "_" + "arrayAddrBase";
                        else
                            addrStr = "arrayAddrBase";
                        VariableNameDeclaration pvnd = new VariableNameDeclaration(node.getFileName(), node.getScope(), addrStr, node, addrStr);
                        pvnd.setType(CType_BaseType.intType);
                        PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) VariableDomain.newInstance(pvnd, expdata.currentvex);
                        pvd.setVariableSource(VariableSource.INPUT_ANNOMY);
                        expdata.currentvex.addVariableDomain(pvd);
                        factor = pvd.getCurrentSymbolFactor();
                        expdata.currentvex.addSymbolDomain(factor, new IntegerDomain(0, Long.MAX_VALUE));
                        addr = new Expression(factor);
                        memoryBlock.addMember(addr, pt);
                    } else {
                        addr = memoryBlock.getMember(pt);
                    }

                    // ����4 ��ַƫ��1��addr-1,�����Ƿ�����±�Ϊaddr-1��Ӧ��Ԫ�أ������򷵻أ��������½���
                    Expression addrSubOne = addr.sub(new Expression(1));
                    String subindex = addrSubOne.getVND();
                    VariableDomain ptSubOne = memoryBlock.getMember(addrSubOne);
                    if (ptSubOne == null) {
                        String memName = memoryBlock.getVariableNameDeclaration().getName() + "[" + subindex + "]";
                        VariableNameDeclaration vndSubOne = new VariableNameDeclaration(p.getFileName(), p.getScope(), memName, node, addrSubOne.toString());
                        vndSubOne.setType(pt.getType());
                        ptSubOne = VariableDomain.newInstance(vndSubOne, pt.getVariableSource().next(), expdata.currentvex);
                        memoryBlock.addMember(addrSubOne, ptSubOne);
                    }

                    // ����5 ִ��p=p-1�Ĳ���
                    if (ptSubOne instanceof PointerVariableDomain) {
                        // add by yaochi pt++��
                        // ������parentΪcurrent��parent2013-09-03
                        PointerVariableDomain pvd = (PointerVariableDomain) ptSubOne;
                        ((VariableNameDeclaration) pvd.getPointerTo().getNd()).setParent(currentVD.getVariableNameDeclaration());
                    }
                    ((PointerVariableDomain) currentVD).changePT(ptSubOne);
                }
                // add end
            } else {
                currentvalue = new Expression(SymbolFactor.genSymbol(CType_BaseType.getBaseType("int")));
            }
        }
        // add by zhouhb
        // ����p=f(a,b)ʱ����Ϊf,a,b�ֱ�������������F,A,B�����ջ�����һ���µķ���P������δ��F��Ӧ��������P����
        // ���˵�ʱΪʲô��ô�ģ����ұ���
        // if(primary.jjtGetNumChildren()==0&&primary.getType() instanceofo
        // CType_Function){
        // currentvalue=func;
        // }

        expdata.vd = currentVD;// ��ǰ�����ڴ�ģ��
        if (expdata.vd instanceof PrimitiveVariableDomain) {
            expdata.value = ((PrimitiveVariableDomain) currentVD).getExpression();
        } else {
            expdata.value = currentvalue;// ��ǰ�������ű��ʽ
        }
        // expdata.value=currentvalue;
        // expdata.vd = currentVD;//�ֶ�����ظ��Ĵ��롣����2013-5-13zxz

        return data;
    }

    @Override
    public Object visit(ASTPrimaryExpression node, Object data) {
        super.visit(node, data);
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        String image = node.getImage();
        if (!image.equals("")) {
            Scope scope = node.getScope();
            NameDeclaration decl = Search.searchInVariableAndMethodUpward(image, scope);//
            CType orignType;
            if (decl instanceof VariableNameDeclaration) {

                // ��������
                VariableNameDeclaration v = (VariableNameDeclaration) decl;
                expdata.value = expdata.currentvex.getValue(v);
                // add by yaochi
                VariableDomain vd = expdata.currentvex.getVariableDomain(v);

                if (vd != null) {
                    VariableDomain expvd = expdata.vd;
                    expdata.vd = vd;
                    // add by baiyu 2014.12.24 ��Ա�����������λ����ʱ����ӵĸ���������Լ����ʧ�����
                    HashMap<Expression, IntegerDomain> temp = vd.helpDomainForVD;
                    if (temp != null) {
                        expdata.Help = temp;
                        // expdata.Help.put(expdata.value, temp);
                    }
                    if (expvd != null && expvd.getType().equals(vd.getType())) {
                        // ���ָ��ָ���µ�ַ��vd�õ�����Ȼ��֮ǰ��ַ���� add by yaochi 2013-09-03
                        if (((VariableNameDeclaration) expvd.getNd()).getAncestor().equals(((VariableNameDeclaration) vd.getNd()).getAncestor())) {
                            expdata.vd = expvd;
                        }
                    }
                }
                // end
                // expdata.vd = expdata.currentvex.getVariableDomain(v); // add
                // by tangrong 2011-12-16
                // ������ṹ��������Ϣ
                // add by zhouhb 2010/8/19
                if (!Config.Field) {
                    if (v.getType() instanceof CType_Array && ((CType_Array) v.getType()).getOriginaltype() instanceof CType_Struct || v.getType() instanceof CType_Struct)
                        return data;
                }
                // add by tangrong 2011-12-15 ��ʱ�������� ֮���������������or�ֲ�������������
                if (expdata.vd == null) {
                    VariableSource varSource = (v.getScope() instanceof SourceFileScope || v.getScope() instanceof MethodScope) ? VariableSource.INPUT : VariableSource.LOCAL;
                    expdata.vd = VariableDomain.newInstance(v, varSource, expdata.currentvex);
                    if (expdata.vd.getNd() != null && expdata.vd.getNd().getNode() instanceof ASTEnumerator) {
                        // Do nothing��������ö�ٵĳ�Աʱ������VS��
                    } else {
                        expdata.currentvex.addVariableDomain(expdata.vd);
                    }
                }
                // add end tangrong

                if (expdata.value == null) {
                    // ȫ�ֱ�����ֵ���� �����ȫ�ֱ������ҳ�ʼֵ������ʱ����������뵽��ǰ����� ssj
                    // if(v.getScope() instanceof SourceFileScope){
                    // Variable var = v.getVariable();
                    // if(var == null){
                    // if(var.getValue() instanceof Double){
                    // double vardouble = new Double(var.getValue().toString());
                    // expdata.value = new Expression(vardouble);
                    // }else if(var.getValue() instanceof Long){
                    // long varlong = Long.valueOf(var.getValue().toString());
                    // expdata.value = new Expression(varlong);
                    // }else if (var.getValue() instanceof PointerValue){
                    // PointerDomain pd=new PointerDomain((PointerValue)
                    // var.getValue());
                    // SymbolFactor s=SymbolFactor.genSymbol(v.getType(),
                    // v.getImage());
                    // expdata.currentvex.addSymbolDomain(s, pd);
                    // expdata.value=new Expression(s);
                    // }else{
                    // expdata.value=new
                    // Expression(SymbolFactor.genSymbol(v.getType(),v.getImage()));
                    //
                    // }
                    // }else{
                    // expdata.value=new
                    // Expression(SymbolFactor.genSymbol(v.getType(),v.getImage()));
                    // }
                    // }else {
                    // SymbolFactor
                    // sym=SymbolFactor.genSymbol(v.getType(),v.getImage());
                    // //modified by zhouhb
                    // //��������f(a,b)�в����ĳ�ʼ������
                    // //remodified by zhouhb 2010/8/6
                    // //���������ʼ��
                    // // if(v.getType() instanceof
                    // CType_Pointer&&node.jjtGetNumChildren()==0){
                    // // PointerDomain p=new PointerDomain();
                    // // expdata.currentvex.addSymbolDomain(sym, p);
                    // // }
                    // expdata.value=new Expression(sym);
                    // }

                    // �����жϸ�����
                    SymbolFactor sym = SymbolFactor.genSymbol(v.getType(), v);
                    if (vd instanceof PrimitiveVariableDomain) {
                        expdata.value = ((PrimitiveVariableDomain) vd).getExpression();
                    } else {
                        expdata.value = new Expression(sym);
                    }

                    // add by tangrong 2011-12-15
                    // expdata.vd = VariableDomain.newInstance(v);
                    if (expdata.currentvex != null) {// PointerVariableDomain
                        if (v.getScope() instanceof SourceFileScope) {
                            Variable var = Variable.getVariable(v);
                            if (var != null) {
                                if (!var.getType().isArrayType()) {
                                    if (Config.USEUNKNOWN) {
                                        Domain d = Domain.getUnknownDomain(Domain.getDomainTypeFromType(v.getType()));
                                        expdata.currentvex.addSymbolDomain(sym, d);
                                    } else {
                                        if (expdata.vd.getNd() != null && expdata.vd.getNd().getNode() instanceof ASTEnumerator) {
                                            // Do nothing��������ö�ٵĳ�Աʱ������VS��
                                        } else {
                                            expdata.currentvex.addSymbolDomain(sym, Domain.getFullDomainFromType(v.getType()));
                                        }

                                    }

                                }

                                else {
                                    PointerDomain pd = new PointerDomain((PointerValue) PointerValue.NOTNULL);
                                    expdata.currentvex.addSymbolDomain(sym, pd);
                                }
                            }
                        }

                        // expdata.currentvex.addValue(v, expdata.value); ע��by
                        // tangrong 2012-02-21
                    }
                }
            } else if (decl instanceof MethodNameDeclaration) {
                MethodNameDeclaration mnd = (MethodNameDeclaration) decl;
                Method method = null;
                MethodSummary ms = null;

                // add by tangrong 2012-5-10 ����ֲڵĴ�������������ֵ��ʼ����ȫ��ȡֵ��
                if (mnd != null && expdata.vd == null) {
                    MethodSet methodset = expdata.currentvex.getMethodSet();
                    VariableDomain vd = methodset.getMap().get(mnd);
                    expdata.vd = vd;
                    if (expdata.vd == null) {
                        expdata.vd = VariableDomain.newInstance(mnd, expdata.currentvex);
                    }
                    // if (/*!mnd.isLib( ) ||*/ mnd.isIOLib( )) {//�ӵ�methodSet��
                    // by jinkaifeng 2013.5.3
                    expdata.currentvex.addValue(mnd, expdata.vd);
                    // }
                }
                // �⺯������ֵ���� ssj ò��������ժҪ
                if (mnd != null && mnd.isLib()) {
                    InterContext interContext = InterContext.getInstance();
                    Map<String, MethodNameDeclaration> libDecls = interContext.getLibMethodDecls();
                    MethodNameDeclaration libmethod = libDecls.get(mnd.getImage());
                    if (libmethod != null) {
                        method = libmethod.getMethod();
                        if (method != null) {
                            Domain d = method.getReturnDomain();
                            SymbolFactor s = SymbolFactor.genSymbol(node.getType());
                            if (Config.USEUNKNOWN && d == null)
                                d = Domain.getUnknownDomain(Domain.getDomainTypeFromType(node.getType()));
                            node.getCurrentVexNode().addSymbolDomain(s, d);
                            expdata.value = new Expression(s);
                        }
                    }
                    if (expdata.value == null) {
                        // �ǿ⺯��������û��ժҪ����ֱ������һ�����ţ��䷵��ֵ����δ֪
                        expdata.value = new Expression(SymbolFactor.genSymbol(mnd.getType(), mnd.getImage()));
                    }
                } else {
                    // chh ������ʽ��������������ʱ���Կ��Լ������������ֵ�ĺ�������ȡ�䷵��ֵ����
                    if (mnd != null)
                        method = mnd.getMethod();
                    if (method != null && method.getReturnDomain() != null) {
                        // ������
                        if ((method.getReturnDomain().getDomaintype() == DomainType.INTEGER)
                                && (((IntegerDomain) method.getReturnDomain()).getMax() == ((IntegerDomain) method.getReturnDomain()).getMin()))
                            expdata.value = new Expression(((IntegerDomain) method.getReturnDomain()).getMax());
                        // ������
                        else if ((method.getReturnDomain().getDomaintype() == DomainType.DOUBLE)
                                && (((DoubleDomain) method.getReturnDomain()).getMax() == ((DoubleDomain) method.getReturnDomain()).getMin()))
                            expdata.value = new Expression(((DoubleDomain) method.getReturnDomain()).getMax());
                        else if (method.getReturnDomain().getDomaintype() == DomainType.POINTER) {
                            CType type = node.getType();
                            CType pointer = null;
                            if (type != null && type instanceof CType_Function) {
                                pointer = ((CType_Function) type).getReturntype();
                                if (pointer != null && pointer instanceof CType_Pointer) {
                                    SymbolFactor s = SymbolFactor.genSymbol(type);
                                    expdata.value = new Expression(s);
                                    expdata.currentvex.addSymbolDomain(s, method.getReturnDomain());

                                }
                            }
                        } else if (expdata.value == null) {
                            CType type = method.getReturnType();
                            expdata.value = new Expression(SymbolFactor.genSymbol(type));
                        }
                    } else {// �����ݲ�����
                        // zys:�����ָ�����͵ĸ�ֵ���������Ĭ�ϸ�ֵΪNULLORNOTNULL
                        CType type = node.getType();
                        CType pointer = null;
                        if (type != null && type instanceof CType_Function) {
                            pointer = ((CType_Function) type).getReturntype();
                            // �����������ֵ����Ϊ�汾��ʱ���������ͣ����账������©��
                            // add by zhouhb 2010/8/30
                            if (pointer != null && pointer instanceof CType_Pointer && !(((CType_Pointer) pointer).getOriginaltype() instanceof CType_BaseType)) {
                                return data;
                            }
                            // end by zhouhb
                            if (pointer != null && pointer instanceof CType_Pointer) {
                                SymbolFactor s = SymbolFactor.genSymbol(type);
                                expdata.value = new Expression(s);
                                PointerDomain domain = new PointerDomain();
                                expdata.currentvex.addSymbolDomain(s, domain);
                            }
                        }
                        if (expdata.value == null) {
                            expdata.value = new Expression(SymbolFactor.genSymbol(type));
                        }
                    }
                    // chh ������ʽ��������������ʱ���Կ��Լ������������ֵ�ĺ�������ȡ�䷵��ֵ���� end

                    // �������� ���ȫ�ֱ��������������� ssj
                    if (mnd != null)
                        ms = mnd.getMethodSummary();
                    if (ms != null && !ms.getPostConditions().isEmpty()) {
                        Set<MethodFeature> mtdpostcond = ms.getPostConditions();
                        for (MethodFeature mtdfea : mtdpostcond) {
                            if (mtdfea instanceof MethodPostCondition) {
                                MethodPostCondition mtdpost = (MethodPostCondition) mtdfea;
                                Map<Variable, Domain> msvariables = mtdpost.getPostConds();
                                for (Variable msvariable : msvariables.keySet()) {// zys:Ϊ���������еı��������µķ��ű��ʽ
                                    VariableNameDeclaration v = (VariableNameDeclaration) Search.searchInVariableUpward(msvariable.getName(), scope);
                                    /**
                                     * zys: ����ժҪ���ɵ����ʣ� 1�� �ٶ���file1 . c��file2 .
                                     * c����Դ�ļ��� # include "file3.c" �� ��file3 .
                                     * c�н���һ��������������ѡ��ĺ��� 2�� ���file1 .c�ȷ��� ��
                                     * ��������������Ĳ�ͬ �� ������file1 . c�иú�����ժҪ��Ϣ 3��
                                     * ��file2 . c����ʱ�����ú������� �� �����Ȼ�ȡ֮ǰ���ɵ�ժҪ ��
                                     * �ٰ��ձ��ļ��е���������ѡ������µ�ժҪ 4��
                                     * ���Ϸ����Ľ���͵���ժҪ��Ϣ�ڵ����ļ����Ҳ���
                                     */
                                    if (v == null)
                                        continue;
                                    SymbolFactor s = SymbolFactor.genSymbol(v.getType());
                                    Domain d = msvariables.get(msvariable);
                                    node.getCurrentVexNode().addSymbolDomain(s, d);
                                    Expression expr = new Expression(s);
                                    node.getCurrentVexNode().addValue(v, expr);
                                }
                            }
                        }
                    }
                }
                // if(Config.USEUNKNOWN &&
                // expdata.value.getDomain(expdata.currentvex.getSymDomainset())==null)
                // expdata.currentvex.addSymbolDomain(expdata.value.getAllSymbol().iterator().next(),
                // Domain.getUnknownDomain(Domain.getDomainTypeFromType(node.getType())));
            }
        }
        return data;
    }

    public Object visit(ASTRelationalExpression node, Object data) {
        // ���Ǵ������С�����ֹ�ϵ��
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        node.jjtGetChild(0).jjtAccept(this, expdata);
        Expression leftvalue = expdata.value;
        VariableDomain leftVD = expdata.vd;// add by jinkaifeng
        Expression rightvalue = null;
        VariableDomain rightVD = null;// add by jinkaifeng
        DoubleDomain d1 = null;
        DoubleDomain d2 = null;
        try {
            // �����ҽ��з��ż��� ExpressionValueVisitor
            for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                Node c = node.jjtGetChild(i);
                expdata.value = null;
                expdata.vd = null;
                c.jjtAccept(this, expdata);
                rightvalue = expdata.value;
                rightVD = expdata.vd;

                if (leftvalue == null || rightvalue == null) {
                    throw new MyNullPointerException("RelationalExpression Value NULL in(.i) " + node.getBeginFileLine());
                }
                String operator = node.getOperatorType().get(i - 1);

                // ����strlen���ַ�������Լ�� add by yaochi 20141020
                MethodSet ms = expdata.currentvex.getMethodSet();
                if (ms != null && ms.getFunctionList().size() != 0) {
                    MethodNameDeclaration mnd = null;
                    FunctionVariableDomain fvd = null;
                    if (ms.isMethodReturn(leftVD)) {
                        // �Ǻ�������ֵ
                        mnd = ms.getMethodNameDeclarationByRetVD(leftVD);
                        if (mnd != null && mnd.isLib()) {
                            fvd = (FunctionVariableDomain) ms.getVd(mnd);
                            List<RelationExpression> explist = fvd.getLibFunctionConstraint(leftVD, operator, rightVD);
                            // �����ַ�������Լ������add by wangyi
                            if (explist != null && explist.size() > 0) {
                                expdata.currentvex.addExpafList(explist);
                                for (RelationExpression re : explist) {
                                    expdata.currentLogicalExpression.addLRExpression(re);
                                }
                                for (int i1 = 0; i1 < explist.size() - 1; ++i1) {
                                    expdata.currentLogicalExpression.addOperator("&&");
                                }
                            }
                        }

                    }
                    if (ms.isMethodReturn(rightVD)) {
                        // �Ǻ�������ֵ
                        mnd = ms.getMethodNameDeclarationByRetVD(rightVD);
                        if (mnd != null && mnd.isLib()) {
                            fvd = (FunctionVariableDomain) ms.getVd(mnd);
                            List<RelationExpression> explist = fvd.getLibFunctionConstraint(rightVD, operator, leftVD);
                            // �����ַ�������Լ������add by wangyi
                            if (explist != null && explist.size() > 0) {
                                expdata.currentvex.addExpafList(explist);
                                for (RelationExpression re : explist) {
                                    expdata.currentLogicalExpression.addLRExpression(re);
                                }
                                for (int i1 = 0; i1 < explist.size() - 1; ++i1) {
                                    expdata.currentLogicalExpression.addOperator("&&");
                                }
                            }
                        }

                    }
                }
                // end

                // add by yaochi ����*a[][]���͵Ĵ���
                if (leftVD instanceof PointerVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                    if (((PointerVariableDomain) leftVD).getPointerTo() instanceof PrimitiveVariableDomain)
                        leftVD = ((PointerVariableDomain) leftVD).getPointerTo();
                }// end

                // leftvalue =
                // ((PrimitiveVariableDomain)leftVD).getExpression();//add by
                // zhangxuzhou 2012-12-14
                // rightvalue =
                // ((PrimitiveVariableDomain)rightVD).getExpression();//add by
                // zhangxuzhou
                // add by xjx 2012-6-29
                for (int j = 0; j < expdata.origexplist.size(); j++) {// �ұߵ�ֵ�����⣬��operatorΪ<��ʱ������ָ��
                    expdata.currentvex.addMultiExp(expdata.origexplist.get(j));
                    // logger.debug("ASTRelationalExpression�и���������ʽ��"+expdata.origexplist.get(j).toString());
                }
                expdata.origexplist.clear();

                // ���Լ����add by wangyi
                RelationExpression relationExp = new RelationExpression(leftvalue, rightvalue, operator, expdata.currentvex);
                expdata.currentvex.addExpaf(relationExp);
                if (expdata.currentLogicalExpression != null)
                    expdata.currentLogicalExpression.addLRExpression(relationExp);

                if (leftVD instanceof PrimitiveVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                    PrimitiveVariableDomain lvd = (PrimitiveVariableDomain) leftVD;
                    PrimitiveVariableDomain rvd = (PrimitiveVariableDomain) rightVD;
                    leftvalue = lvd.getExpression();
                    rightvalue = rvd.getExpression();
                    d1 = Domain.castToDoubleDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
                    d2 = Domain.castToDoubleDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
                }
                if (d1 == null || d2 == null) {
                    throw new MyNullPointerException("RelationalExpression Domain NULL in(.i) " + node.getBeginFileLine());
                }
                if (Config.USEUNKNOWN && (d1.isUnknown() && d2.isUnknown())) {
                    throw new MyNullPointerException("RelationalExpression Domain UNKNOWN in(.i) " + node.getBeginFileLine());
                }

                if (operator.equals(">")) {// �˴�Ϊmust���߼�
                    if (d1.getMin() > d2.getMax()) {
                        leftvalue = new Expression(1);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.intType);
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        continue;
                    } else if (d1.getMax() <= d2.getMin()) {
                        leftvalue = new Expression(0);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.intType);
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        continue;
                    }
                } else if (operator.equals(">=")) {
                    if (d1.getMin() >= d2.getMax()) {
                        leftvalue = new Expression(1);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.intType);
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        continue;
                    } else if (d1.getMax() < d2.getMin()) {
                        leftvalue = new Expression(0);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.intType);
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        continue;
                    }
                } else if (operator.equals("<")) {
                    if (d1.getMax() < d2.getMin()) {
                        leftvalue = new Expression(1);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.intType);
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        continue;
                    } else if (d1.getMin() >= d2.getMax()) {
                        leftvalue = new Expression(0);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.intType);
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        continue;
                    }
                } else if (operator.equals("<=")) {
                    if (d1.getMax() <= d2.getMin()) {
                        leftvalue = new Expression(1);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.intType);
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        continue;
                    } else if (d1.getMin() > d2.getMax()) {
                        leftvalue = new Expression(0);
                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString() + node.toString(), node, "tmp_" + node.toString());
                        tmpVND.setType(CType_BaseType.intType);
                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
                        continue;
                    }
                }
                throw new MyNullPointerException("RelationalExpression Domain Unknown in(.i) " + node.getBeginFileLine());
            }
        } catch (MyNullPointerException e) {
            // super.visit(node,expdata); ע��by tangrong 2012-10-30
            // add by xjx 2012-6-29
            expdata.origexplist.clear();
            // end add by xjx 2012-6-29
            SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
            expdata.currentvex.addSymbolDomain(sym, new IntegerDomain(0, 1));
            leftvalue = new Expression(sym);
            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
            tmpVND.setType(CType_BaseType.intType);
            leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
            expdata.value = leftvalue;
            expdata.vd = leftVD;
            return data;
        }

        expdata.value = leftvalue;
        expdata.vd = leftVD;
        return data;
    }

    public Object visit(ASTShiftExpression node, Object data) {
        return dealBinaryBitOperation(node, data, node.getOperators());
    }

    // unary-expressionһԪ���ʽ
    public Object visit(ASTUnaryExpression node, Object data) {

        ExpressionVistorData expdata = (ExpressionVistorData) data;

        if (node.jjtGetChild(0) instanceof ASTPostfixExpression) {
            node.jjtGetChild(0).jjtAccept(this, expdata);// F5���õ��ӽڵ�ĳ����ڴ�ģ�ͣ�expdata.value�Ѿ�����Ҫ������Ҫ��vd

            // //��������ȡ���ʽ ��Ҫȷ�����unaryExpression ����һ��������±ꡢ
            // ArrayList<Boolean>
            // flags=((ASTPostfixExpression)node.jjtGetChild(0)).getFlags();
            // if(flags==null)return data;
            // boolean flag=flags.get(0);
            // add by xjx 2012-6-29
            if (node.jjtGetChild(0).jjtGetNumChildren() > 1)// ����������Ϳ⺯������
            {
                if (((node.jjtGetParent() instanceof ASTAssignmentExpression) // ����if(isalnum(x))�Լ�if(!isalnum(x))
                        || (node.jjtGetParent() instanceof ASTUnaryExpression) || (node.jjtGetParent() instanceof ASTLogicalANDExpression) || (node.jjtGetParent() instanceof ASTLogicalORExpression))
                        && (node.getParentsOfType(ASTExpressionStatement.class).size() == 0)) {
                    String oper = "";
                    if (node.getImage().equals("isalnum") || node.getImage().equals("isalpha") || node.getImage().equals("iscntrl") || node.getImage().equals("isdigit")
                            || node.getImage().equals("islower") || node.getImage().equals("isgraph") || node.getImage().equals("isprint") || node.getImage().equals("ispunct")
                            || node.getImage().equals("isupper") || node.getImage().equals("isspace")) {
                        if (node.getImage().equals("isalnum")) // �ж��Ƿ������ֻ���ĸ
                            oper = "isalnum";
                        else if (node.getImage().equals("isalpha")) // �ж��Ƿ�����ĸ
                            oper = "isalpha";
                        else if (node.getImage().equals("iscntrl")) // �ж��Ƿ�Ϊ�����ַ���ASCII����0-0x1F֮��
                            oper = "iscntrl";
                        else if (node.getImage().equals("isdigit")) // �ж��Ƿ�Ϊ����
                            oper = "isdigit";
                        else if (node.getImage().equals("islower")) // �ж��Ƿ�ΪСд��ĸ
                            oper = "islower";
                        else if (node.getImage().equals("isgraph")) // �ж��Ƿ���Դ�ӡ�ַ����������ַ���ox21~ox7E��
                            oper = "isgraph";
                        else if (node.getImage().equals("isprint")) // �ж��Ƿ�ɴ�ӡ�ַ������ո�ox21~ox7E
                            oper = "isprint";
                        else if (node.getImage().equals("ispunct")) // �ж��Ƿ�Ϊ����ַ����������ո�
                            oper = "ispunct";
                        else if (node.getImage().equals("isupper")) // �ж��Ƿ�Ϊ��д��ĸ
                            oper = "isupper";
                        else if (node.getImage().equals("isspace")) // ���ch�Ƿ�ո���������Ʊ�������з�
                            oper = "isspace";
                        node.jjtGetChild(0).jjtGetChild(1).jjtGetChild(0).jjtGetChild(0).jjtAccept(this, expdata);

                        Expression expression = expdata.value;
                        VariableDomain expressionvd = expdata.vd;
                        if (expressionvd instanceof PrimitiveVariableDomain) {
                            SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
                            MultiplicativeExpressionWithCType expr = new MultiplicativeExpressionWithCType(((PrimitiveVariableDomain) expressionvd).getExpression(), oper);
                            // SymbolFactor sym =
                            // SymbolFactor.genSymbol(CType_BaseType.getBaseType("double"));
                            expr.setRelatedSymbolfactor(sym);
                            expdata.origexplistwithctype.add(expr);
                        }
                    }
                }
            }
            if (((node.jjtGetParent() instanceof ASTAssignmentExpression) || (node.jjtGetParent() instanceof ASTLogicalANDExpression) || (node.jjtGetParent() instanceof ASTLogicalORExpression))
                    && (node.getSingleChildofType(ASTAssignmentExpression.class) == null)) {
                // added zhangxuzhou 2012-12-13 ������ a[i] ����� ��i�ķ���Ҳ��ȡ���� ���ǵ���һ������

                if ((node.getParentsOfType(ASTPostfixExpression.class).size() != 0)) {

                    List<ASTPostfixExpression> list = node.getParentsOfType(ASTPostfixExpression.class);
                    String operator = list.get(0).getOperators();
                    if (operator.startsWith("["))
                        return data; // �� ���� a[i]�е�i�����ϲ��ȡ��Լ������
                    // boolean flag = list.get(0).getFlags().get(0);
                    // if(flag)return data; //��[]() . ->��
                }
                // added ended zhangxuzhou 2012-12-13

                // !=0�Ƿ�������� for while�е�i++�����⣬�д����
                if (node.getParentsOfType(ASTExpressionStatement.class).size() == 0
                        && (node.getParentsOfType(ASTIterationStatement.class).size() == 0 || node.jjtGetParent().jjtGetNumChildren() == 1 || node.getParentsOfType(ASTSelectionStatement.class).size() > 0)
                        && node.getParentsOfType(ASTInitDeclarator.class).size() == 0 && node.getParentsOfType(ASTJumpStatement.class).size() == 0) {
                    if (expdata.value != null) {

                        Factor f = expdata.value.getSingleFactor();
                        if (f instanceof SymbolFactor) {
                            for (int i = 0; i < expdata.origexplist.size(); i++) {
                                expdata.currentvex.addMultiExp(expdata.origexplist.get(i));
                                // logger.debug("ASTUnaryExpression�ж�Ӧ�ĸ��ӱ��ʽ: "+expdata.origexplist.get(i).toString());
                            }
                            expdata.origexplist.clear();

                            // add by wangyi
                            RelationExpression relationExp = new RelationExpression(expdata.value, null, null, expdata.currentvex);
                            expdata.currentvex.addExpaf(relationExp);
                            if (expdata.currentLogicalExpression != null && isConstrainNode((SimpleNode) node.jjtGetParent().jjtGetParent())) {
                                expdata.currentLogicalExpression.addLRExpression(relationExp);
                            }
                        }
                    }
                }
            } else if ((node.jjtGetParent() instanceof ASTEqualityExpression) || (node.jjtGetParent() instanceof ASTRelationalExpression)) {// Ŀǰֻ��abs(x)֧��ASTRelationalExpression������
                if ((node.jjtGetChild(0).jjtGetNumChildren() > 1) && (node.getImage().equals("sin") || node.getImage().equals("cos") || node.getImage().equals("tan"))) {
                    String operator = "";
                    if (node.getImage().equals("sin")) {
                        operator = "sin";
                    } else if (node.getImage().equals("cos")) {
                        operator = "cos";
                    } else if (node.getImage().equals("tan")) {
                        operator = "tan";
                    }
                    node.jjtGetChild(0).jjtGetChild(1).jjtGetChild(0).jjtGetChild(0).jjtAccept(this, expdata);
                    Expression expression = expdata.value;
                    VariableDomain expressionvd = expdata.vd;
                    IntegerDomain i1 = Domain.castToIntegerDomain(expression.getDomain(expdata.currentvex.getSymDomainset()));
                    VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                    tmpVND.setType(expressionvd.getType());
                    if (expressionvd instanceof PrimitiveVariableDomain) {
                        MultiplicativeExprWithOnePara expr = new MultiplicativeExprWithOnePara(((PrimitiveVariableDomain) expressionvd).getExpression(), operator);
                        SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("double"));
                        expr.setRelatedSymbolfactor(sym);
                        expdata.origexplistwithonepara.add(expr);
                        expressionvd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, new Expression(sym));
                        logger.debug("sss");
                    }

                }

                if ((node.jjtGetChild(0).jjtGetNumChildren() > 1)
                        && (node.getImage().equals("log") || node.getImage().equals("log10") || node.getImage().equals("exp") || node.getImage().equals("asin") || node.getImage().equals("acos")
                                || node.getImage().equals("atan") || node.getImage().equals("log2") || node.getImage().equals("log1p") || node.getImage().equals("exp2")
                                || node.getImage().equals("expm1") || node.getImage().equals("sqrt") || node.getImage().equals("cbrt") || node.getImage().equals("sinh")
                                || node.getImage().equals("cosh") || node.getImage().equals("tanh"))) {
                    String operator = "";
                    if (node.getImage().equals("log")) {
                        operator = "log";
                    } else if (node.getImage().equals("log10")) {
                        operator = "log10";
                    } else if (node.getImage().equals("exp")) {
                        operator = "exp";
                    } else if (node.getImage().equals("asin")) {
                        operator = "asin";
                    } else if (node.getImage().equals("acos")) {
                        operator = "acos";
                    } else if (node.getImage().equals("atan")) {
                        operator = "atan";
                    } else if (node.getImage().equals("exp2")) {
                        operator = "exp2";
                    } else if (node.getImage().equals("expm1")) {
                        operator = "expm1";
                    } else if (node.getImage().equals("log2")) {
                        operator = "log2";
                    } else if (node.getImage().equals("log1p")) {
                        operator = "log1p";
                    } else if (node.getImage().equals("sqrt")) {
                        operator = "sqrt";
                    } else if (node.getImage().equals("cbrt")) {
                        operator = "cbrt";
                    } else if (node.getImage().equals("sinh")) {
                        operator = "sinh";
                    } else if (node.getImage().equals("cosh")) {
                        operator = "cosh";
                    } else if (node.getImage().equals("tanh")) {
                        operator = "tanh";
                    }
                    node.jjtGetChild(0).jjtGetChild(1).jjtGetChild(0).jjtGetChild(0).jjtAccept(this, expdata);
                    Expression expression = expdata.value;
                    VariableDomain expressionvd = expdata.vd;
                    IntegerDomain i1 = Domain.castToIntegerDomain(expression.getDomain(expdata.currentvex.getSymDomainset()));// ����ʲô���ã������x_0���������䣿
                    VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                    tmpVND.setType(expressionvd.getType());
                    if (expressionvd instanceof PrimitiveVariableDomain) {
                        MultiplicativeExprlogarithm expr = new MultiplicativeExprlogarithm(((PrimitiveVariableDomain) expressionvd).getExpression(), operator);
                        SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("double"));
                        expr.setRelatedSymbolfactor(sym);
                        expdata.origexplistwithlogarithm.add(expr);
                        expressionvd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, new Expression(sym));
                    }
                }
                if ((node.jjtGetChild(0).jjtGetNumChildren() > 1) && (node.getImage().equals("pow") || node.getImage().equals("hypot"))) {
                    String operator = "";
                    if (node.getImage().equals("pow"))
                        operator = "pow";
                    else if (node.getImage().equals("hypot"))
                        operator = "hypot";
                    Expression value = null;
                    VariableDomain vd = null;
                    Map<Integer, Expression> a = new HashMap<Integer, Expression>();
                    int flag = 0, flag1 = 0, flag2 = 0; // flag������ʶ���������Ƿ�ΪPrimitiveVariableDomain
                    // flag1������ʶ����������ȡֵ�����Ǿ�ȷֵ��flag1=0�����Ǿ�ȷֵ��=1ֻ��һ����>1��һ��û��
                    // flag2������ʶ�ڶ��������Ƿ��ǳ�����flag2==1,��ʾ�ǳ�����
                    VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                    for (int i = 0; i < node.jjtGetChild(0).jjtGetChild(1).jjtGetNumChildren(); i++) {
                        node.jjtGetChild(0).jjtGetChild(1).jjtGetChild(i).jjtGetChild(0).jjtAccept(this, expdata);
                        expdata.expr.add(expdata.value);
                        expdata.exprvd.add(expdata.vd);
                        if (expdata.vd instanceof PrimitiveVariableDomain) {
                            flag++;
                            a.put(i, ((PrimitiveVariableDomain) expdata.vd).getExpression());
                        }
                        CType type = expdata.vd.getType();
                        DoubleDomain i1 = null;
                        Domain i2 = null;
                        Factor factor = expdata.value.getSingleFactor();

                        if (factor instanceof SymbolFactor) {
                            i2 = ((SymbolFactor) factor).getDomainWithoutNull(expdata.currentvex.getSymDomainset());

                            if (i2 instanceof IntegerDomain) // �˴��ж�ȡ���ı�����������ͣ�����ת����double���Ƿ���Ҫ���ݱ��������жϣ�
                                i1 = ((IntegerDomain) i2).integerToDouble();
                            else if (i2 instanceof DoubleDomain)
                                i1 = (DoubleDomain) i2;
                            if (!i1.isConcrete()) {
                                value = expdata.value;
                                vd = expdata.vd;
                                flag1++;
                            }

                        } else if ((factor instanceof NumberFactor) && (i == 1))// ��������б��еڶ���ֵ�ǳ�����ѵ�һ��������value��vd����expdata,��ΪҪ��Ĳ���
                        {
                            flag2++;
                            for (Expression e : expdata.expr) {
                                value = e;
                                break;
                            }
                            for (VariableDomain d : expdata.exprvd) {
                                vd = d;
                                break;
                            }
                        }
                    }
                    if (flag == 2) {
                        if ((flag1 == 1) || (flag2 == 1)) // ������Ǿ�ȷֵ��Ӧ����ô������
                        // ��������Ǿ�ȷ�����䣬�Ƿ���ҪĬ�ϵ�һ������ΪҪת���ı���
                        {
                            expdata.value = value;
                            expdata.vd = vd;
                        }
                        if (flag1 == 2) {
                            for (Expression e : expdata.expr) {
                                expdata.value = e;
                                break;
                            }
                            for (VariableDomain d : expdata.exprvd) {
                                expdata.vd = d;
                                break;
                            }
                        }
                        MultiplicativeExrWithTwoPara expr = new MultiplicativeExrWithTwoPara(a, operator);
                        SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("double"));
                        expr.setRelatedSymbol(sym);
                        expdata.origexplistwithTwoPara.add(expr);
                        logger.debug("ssss");
                    }
                }
                if ((node.jjtGetChild(0).jjtGetNumChildren() > 1) && (node.getImage().equals("abs") || node.getImage().equals("ceil") || node.getImage().equals("floor"))) {
                    String operator = "";
                    if (node.getImage().equals("abs")) {
                        operator = "abs";
                    } else if (node.getImage().equals("ceil")) {
                        operator = "ceil";
                    } else if (node.getImage().equals("floor")) {
                        operator = "floor";
                    }
                    Node tempNode = node.jjtGetChild(0).jjtGetChild(1).jjtGetChild(0);
                    if (tempNode instanceof ASTAssignmentExpression) {
                        tempNode.jjtGetChild(0).jjtAccept(this, expdata);
                        Expression expression = expdata.value;
                        VariableDomain expressionvd = expdata.vd;
                        if (expressionvd instanceof PrimitiveVariableDomain) {
                            SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("double"));
                            MultiplicativeExpressionWithAbs expr = new MultiplicativeExpressionWithAbs(((PrimitiveVariableDomain) expressionvd).getExpression(), operator);
                            // SymbolFactor sym =
                            // SymbolFactor.genSymbol(CType_BaseType.getBaseType("double"));
                            expr.setRelatedSymbolfactor(sym);
                            expdata.origexplistwithAbs.add(expr);
                        }

                    }
                }
            }// end if, add by xiongwei 2013-4-15�ڳ����﷨����Ѱ�һ������Ǻ����ı��ʽ�����Ա��ʽ���з���
             // else if (node.jjtGetParent() instanceof
             // ASTAssignmentExpression &&
             // expdata.vd.getImage().startsWith("const_")) {
             // expdata.currentLogicalExpression.addLRExpression(new
             // RelationExpression(expdata.value, null, null,
             // expdata.currentvex));
             // }
            return data;
        } else if (node.jjtGetChild(0) instanceof ASTInitializerList) {
            expdata = (ExpressionVistorData) data;
            node.jjtGetChild(0).jjtAccept(this, expdata);
        } else if (node.getOperatorType().size() == 1) {

            node.jjtGetChild(0).jjtAccept(this, expdata);
            VariableDomain currentVD = expdata.vd;
            Expression currentValue = expdata.value;
            if (node.getOperatorType().get(0).equals("++")) {// ǰ++
                ASTPrimaryExpression p = (ASTPrimaryExpression) ((SimpleNode) node.jjtGetChild(0)).getSingleChildofType(ASTPrimaryExpression.class);
                if (currentVD instanceof PrimitiveVariableDomain) {
                    PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) currentVD;
                    Expression temp = pvd.getExpression().add(new Expression(1));// add by yaochi
                    expdata.value = expdata.value.add(new Expression(1));
                    // pvd.setExpression(expdata.value);
                    pvd.setExpression(temp);// add by yaochi

                    if (p != null) {
                        VariableNameDeclaration v = p.getVariableDecl();
                        if (v != null && expdata.sideeffect && expdata.currentvex != null) {
                            expdata.currentvex.addValue(v, expdata.value);
                            expdata.currentvex.addValue(v, currentVD);
                        }
                    }/*
                      * modify by yaochi if (Config.Field) { ASTPostfixExpression
                      * po = (ASTPostfixExpression) ((SimpleNode) node
                      * .jjtGetChild(0))
                      * .getSingleChildofType(ASTPostfixExpression.class); if (po
                      * != null) { VariableNameDeclaration v =
                      * po.getVariableDecl(); if (v != null && expdata.sideeffect
                      * && expdata.currentvex != null && expdata.value != null) {
                      * expdata.currentvex.addValue(v, expdata.value);
                      * expdata.currentvex.addValue(v, currentVD); //modify by
                      * yaochi } } }
                      */
                }
                if (currentVD instanceof PointerVariableDomain) {
                    // TODO
                    // ����1:(*p)��ΪNULL����ȡ(*p)���ڵĳ����ڴ�
                    PointerVariableDomain varDomain = (PointerVariableDomain) currentVD;
                    if (varDomain.getPointerTo() == null) {
                        varDomain.initMemberVD();
                    }
                    varDomain.setStateNotNull();
                    VariableDomain pt = varDomain.getPointerTo();

                    // ����2 �����Ƿ����������ڴ��ַ�к���pt��
                    ValueSet vset = expdata.currentvex.getValueSet();
                    ArrayVariableDomain memoryBlock = vset.getMemoryBlockContainVD(pt);

                    // ����3 ���û�У��½�һ�������飨���������ڴ�ռ䣩
                    if (p == null) {// add by yaochi ���*(++a[][])���ܵõ������﷨���ڵ�
                        p = (ASTPrimaryExpression) ((SimpleNode) node.jjtGetChild(0)).getChildofType(ASTPrimaryExpression.class);
                    }// add end
                    Expression addr;
                    if (memoryBlock == null) {
                        // VariableNameDeclaration vnd4MemoryBlock = new
                        // VariableNameDeclaration(p.getFileName(),
                        // p.getScope(),
                        // varDomain.getVariableNameDeclaration().getName()+
                        // "_arr", node,
                        // varDomain.getVariableNameDeclaration().getName()+
                        // "_arr");//modify by yaochi
                        VariableNameDeclaration vnd4MemoryBlock =
                                new VariableNameDeclaration(p.getFileName(), p.getScope(), varDomain.getVariableNameDeclaration().getName().replaceAll("[\\[\\]*]", "_") + "_arr", node, varDomain
                                        .getVariableNameDeclaration().getName() + "_arr");
                        vnd4MemoryBlock.setType(new CType_Array(pt.getType()));
                        memoryBlock = new ArrayVariableDomain(vnd4MemoryBlock, currentVD.getVariableSource(), expdata.currentvex);
                        SymbolFactor factor = SymbolFactor.genSymbol(CType_BaseType.intType);
                        expdata.currentvex.addSymbolDomain(factor, new IntegerDomain(0, Long.MAX_VALUE));
                        addr = new Expression(factor);
                        memoryBlock.addMember(addr, pt);
                        expdata.currentvex.addValue(vnd4MemoryBlock, memoryBlock);
                    } else {
                        addr = memoryBlock.getMember(pt);
                    }

                    // ����4 ��ַƫ��1��addr+1,�����Ƿ�����±�Ϊaddr+1��Ӧ��Ԫ�أ������򷵻أ��������½���
                    Expression addrPlusOne = addr.add(new Expression(1));
                    String subindex = addrPlusOne.getVND();
                    VariableDomain ptPlusOne = memoryBlock.getMember(addrPlusOne, expdata.currentvex.getSymDomainset());
                    if (ptPlusOne == null) {
                        VariableNameDeclaration vndPlusOne = new VariableNameDeclaration(p.getFileName(), p.getScope(), "annoy0[" + subindex + "]", node, addrPlusOne.toString());
                        vndPlusOne.setType(pt.getType());
                        vndPlusOne.setExpSimpleImage(addrPlusOne);
                        ptPlusOne = VariableDomain.newInstance(vndPlusOne, pt.getVariableSource().next(), expdata.currentvex);
                        memoryBlock.addMember(addrPlusOne, ptPlusOne);
                    }

                    // ����5 ִ��p=p+1�Ĳ���
                    if (ptPlusOne instanceof PointerVariableDomain) {// add by
                                                                     // yaochi
                                                                     // pt++��
                                                                     // ������parentΪcurrent��parent2013-09-03
                        PointerVariableDomain pvd = (PointerVariableDomain) ptPlusOne;
                        if (pvd.getPointerTo() == null)
                            pvd.initMemberVD();
                        ((VariableNameDeclaration) pvd.getPointerTo().getNd()).setParent(currentVD.getVariableNameDeclaration());
                    }
                    ((PointerVariableDomain) currentVD).changePT(ptPlusOne);
                }
            } else if (node.getOperatorType().get(0).equals("--")) {// ǰ--
                ASTPrimaryExpression p = (ASTPrimaryExpression) ((SimpleNode) node.jjtGetChild(0)).getSingleChildofType(ASTPrimaryExpression.class);

                if (currentVD instanceof PrimitiveVariableDomain) {
                    // expdata.value = expdata.value.sub(new
                    // Expression(1));//expdata.value�Ѿ�������
                    // add by yaochi
                    PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) currentVD;
                    Expression temp = pvd.getExpression().sub(new Expression(1));
                    pvd.setExpression(temp);
                    if (p != null) {
                        VariableNameDeclaration v = p.getVariableDecl();
                        if (v != null && expdata.sideeffect && expdata.currentvex != null) {
                            expdata.currentvex.addValue(v, expdata.value);
                            expdata.currentvex.addValue(v, currentVD);
                        }
                    }
                }

                if (currentVD instanceof PointerVariableDomain) {
                    // ����1:(*p)��ΪNULL����ȡ(*p)���ڵĳ����ڴ�
                    PointerVariableDomain varDomain = (PointerVariableDomain) currentVD;
                    if (varDomain.getPointerTo() == null) {
                        varDomain.initMemberVD();
                    }
                    varDomain.setStateNotNull();
                    VariableDomain pt = varDomain.getPointerTo();

                    // ����2 �����Ƿ����������ڴ��ַ�к���pt��
                    ValueSet vset = expdata.currentvex.getValueSet();
                    ArrayVariableDomain memoryBlock = vset.getMemoryBlockContainVD(pt);

                    // ����3 ���û�У��½�һ�������飨���������ڴ�ռ䣩
                    Expression addr;
                    if (memoryBlock == null) {
                        VariableNameDeclaration vnd4MemoryBlock =
                                new VariableNameDeclaration(p.getFileName(), p.getScope(), varDomain.getVariableNameDeclaration().getName().replaceAll("[\\[\\]]", "_") + "_arr", node, varDomain
                                        .getVariableNameDeclaration().getName() + "_arr");
                        // VariableNameDeclaration vnd4MemoryBlock = new
                        // VariableNameDeclaration(p.getFileName(),
                        // p.getScope(), "annoy0", node, "annoy0");
                        vnd4MemoryBlock.setType(new CType_Array(pt.getType()));
                        memoryBlock = new ArrayVariableDomain(vnd4MemoryBlock, currentVD.getVariableSource(), expdata.currentvex);
                        SymbolFactor factor = SymbolFactor.genSymbol(CType_BaseType.intType);
                        expdata.currentvex.addSymbolDomain(factor, new IntegerDomain(1, Long.MAX_VALUE));
                        addr = new Expression(factor);
                        memoryBlock.addMember(addr, pt);
                    } else {
                        addr = memoryBlock.getMember(pt);
                    }

                    // ����4 ��ַƫ��1��addr-1,�����Ƿ�����±�Ϊaddr-1��Ӧ��Ԫ�أ������򷵻أ��������½���
                    Expression addrSubOne = addr.sub(new Expression(1));
                    String subindex = addrSubOne.getVND();
                    VariableDomain ptSubOne = memoryBlock.getMember(addrSubOne);
                    if (ptSubOne == null) {
                        String memName = memoryBlock.getVariableNameDeclaration().getName() + "[" + subindex + "]";
                        VariableNameDeclaration vndSubOne = new VariableNameDeclaration(p.getFileName(), p.getScope(), memName, node, addrSubOne.toString());
                        vndSubOne.setType(pt.getType());
                        ptSubOne = VariableDomain.newInstance(vndSubOne, pt.getVariableSource().next(), expdata.currentvex);
                        memoryBlock.addMember(addrSubOne, ptSubOne);
                    }

                    // ����5 ִ��p=p-1�Ĳ���
                    if (ptSubOne instanceof PointerVariableDomain) {
                        // add by yaochi pt--��������parentΪcurrent��parent2013-09-03
                        PointerVariableDomain pvd = (PointerVariableDomain) ptSubOne;
                        ((VariableNameDeclaration) pvd.getPointerTo().getNd()).setParent(currentVD.getVariableNameDeclaration());
                    }
                    ((PointerVariableDomain) currentVD).changePT(ptSubOne);
                }
            } else {
                currentValue = new Expression(SymbolFactor.genSymbol(CType_BaseType.getBaseType("int")));
            }

            expdata.value = currentValue;
            expdata.vd = currentVD;
            return data;
        } else if (node.jjtGetChild(0) instanceof ASTUnaryOperator) {// һԪ������
            // ָ��Ĳ�����*����ȡ by tangrong
            ASTUnaryOperator operator = (ASTUnaryOperator) node.jjtGetChild(0);
            String o = operator.getOperatorType().get(0);
            // Ϊ!����LogicalNotExpression add by wangyi
            if (o.equals("!") && expdata.currentLogicalExpression != null) {
                LogicalNotExpression lne = new LogicalNotExpression();
                expdata.currentLogicalExpression.addLRExpression(lne);
                expdata.lastLogicalExpression = expdata.currentLogicalExpression;
                expdata.currentLogicalExpression = lne;
            }
            AbstractExpression castexpression = (AbstractExpression) node.jjtGetChild(1);
            node.jjtGetChild(1).jjtAccept(this, expdata);// F6 OK when o = "*"

            // һ��������Ϊ�޼����ʽʱ��add by wangyi
            if (expdata.vd instanceof PrimitiveVariableDomain && expdata.vd.getVariableNameDeclaration() != null && !expdata.vd.getVariableNameDeclaration().getName().startsWith("tmp_")
                    && !((SimpleNode) node.jjtGetParent() instanceof ASTAdditiveExpression)) {
                PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) expdata.vd;
                if (!pvd.isConst()) {
                    if (expdata.currentLogicalExpression != null) {
                        expdata.currentLogicalExpression.addLRExpression(new RelationExpression(expdata.value, null, null));
                    }
                }
            }

            // ָ������ add by wangyi
            if (expdata.vd instanceof PointerVariableDomain && isConstrainNode((SimpleNode) node.jjtGetParent().jjtGetParent()) && expdata.vd.getVariableNameDeclaration() != null
                    && !expdata.vd.getVariableNameDeclaration().getName().startsWith("tmp_") && !((SimpleNode) node.jjtGetParent() instanceof ASTEqualityExpression)
                    && !((SimpleNode) node.jjtGetParent() instanceof ASTRelationalExpression)) {
                if (expdata.currentLogicalExpression != null) {
                    expdata.currentLogicalExpression.addLRExpression(new RelationExpression(expdata.value, null, null));
                }
            }

            // �ָ��ֳ� add by wangyi
            if (o.equals("!")) {
                expdata.currentLogicalExpression = expdata.lastLogicalExpression;
            }

            // add by xjx 2012-6-29
            if (((node.jjtGetParent() instanceof ASTAssignmentExpression) || (node.jjtGetParent() instanceof ASTLogicalANDExpression) || (node.jjtGetParent() instanceof ASTLogicalORExpression))
                    && (node.getSingleChildofType(ASTAssignmentExpression.class) == null)) {
                if (node.getParentsOfType(ASTExpressionStatement.class).size() == 0 && (node.getParentsOfType(ASTIterationStatement.class).size() == 0 || node.jjtGetParent().jjtGetNumChildren() == 1 // �е�i++�����⣬�д����
                || node.getParentsOfType(ASTSelectionStatement.class).size() > 0) && node.getParentsOfType(ASTInitDeclarator.class).size() == 0
                        && node.getParentsOfType(ASTJumpStatement.class).size() == 0) {
                    if (expdata.value != null) {
                        Factor f = expdata.value.getSingleFactor();
                        if (f instanceof SymbolFactor) {
                            // SymbolFactor sym = (SymbolFactor)f;
                            // if(sym!=null &&
                            // sym.getSymbol().startsWith("S_")){}
                            // else{
                            // }
                            for (int i = 0; i < expdata.origexplist.size(); i++) {
                                expdata.currentvex.addMultiExp(expdata.origexplist.get(i));
                                // logger.debug("ASTUnaryExpression�ж�Ӧ�ĸ��ӱ��ʽ: "+expdata.origexplist.get(i).toString());
                            }
                            expdata.origexplist.clear();
                            // expdata.currentvex.addExpaf(new
                            // ExpressionAfterExtract(expdata.value, null, o));
                            RelationExpression relationExp = new RelationExpression(expdata.value, null, o, expdata.currentvex);
                            expdata.currentvex.addExpaf(relationExp);
                            // expdata.currentLogicalExpression.addLRExpression(relationExp);
                            if (expdata.origexplistwithctype != null)
                                for (int i = 0; i < expdata.origexplistwithctype.size(); i++)
                                    logger.debug("ASTUnaryExpression:" + o + expdata.origexplistwithctype.get(i));
                            else
                                logger.debug("ASTUnaryExpression: " + o + expdata.value);
                        }
                        // else{
                        // expdata.currentvex.addExpaf(new
                        // ExpressionAfterExtract(expdata.value, null, o));
                        // logger.debug("ASTUnaryExpression: "+o+expdata.value);
                        // }
                    }
                }
            }
            // end add by xjx 2012-6-29
            if (o.equals("&&")) {
                // ��֪����ô����
            }// modified by zhouhb
            else if (o.equals("&")) {
                SymbolFactor sym = SymbolFactor.genSymbol(node.getType());
                PointerDomain p = new PointerDomain();
                p.offsetRange = new IntegerDomain(0, 0);
                // p.AllocType=CType_AllocType.stackType;
                if (p.Type.contains(CType_AllocType.NotNull)) {
                    p.Type.remove(CType_AllocType.NotNull);
                }
                p.Type.add(CType_AllocType.stackType);
                p.setValue(PointerValue.NOTNULL);
                p.setElementtype(castexpression.getType());
                expdata.currentvex.addSymbolDomain(sym, p);
                expdata.value = new Expression(sym);

                /*
                 * add by jinkaifeng 2012.10.09
                 */
                VariableDomain currentVD = expdata.vd;
                VariableDomain currVD = currentVD;
                if (currentVD instanceof ArrayVariableDomain) {// add by yaochi
                    CType curtype = currentVD.getType();
                    Scope scope = node.getScope();
                    while (curtype instanceof CType_Array) {
                        String childvndStr = currVD.getNd().getImage() + "[0]";
                        VariableNameDeclaration childvnd = (VariableNameDeclaration) Search.searchInVariableAndMethodUpward(childvndStr, scope);
                        if (childvnd != null) {
                            VariableDomain childvd = expdata.currentvex.getVariableDomain(childvnd);
                            if (childvd == null) {
                                childvd = VariableDomain.newInstance(childvnd, expdata.vd.getVariableSource().next(), expdata.currentvex);
                                ((ArrayVariableDomain) currVD).addMember(new Expression(0), childvd);
                            }
                            curtype = CType.getNextType(curtype);
                            currVD = childvd;
                        }
                    }
                    VariableNameDeclaration vnd4AnnoyPtr1 = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoyPtr9", node, "annoyPtr9");
                    vnd4AnnoyPtr1.setType(new CType_Pointer(curtype));
                    PointerVariableDomain varDomain = new PointerVariableDomain(vnd4AnnoyPtr1, expdata.currentvex);
                    // change first argument null to vnd4AnnoyPtr by jinkaifeng
                    // 2012.10.9
                    varDomain.setPointTo(currVD);
                    VariableNameDeclaration vnd4AnnoyPtr2 = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoyPtr10", node, "annoyPtr10");
                    vnd4AnnoyPtr2.setType(new CType_Pointer(varDomain.getType()));
                    PointerVariableDomain varDomain2 = new PointerVariableDomain(vnd4AnnoyPtr2, expdata.currentvex);
                    varDomain2.setPointTo(varDomain);
                    expdata.vd = varDomain2;
                } else {// end
                    VariableNameDeclaration vnd4AnnoyPtr = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoyPtr7", node, "annoyPtr7");
                    vnd4AnnoyPtr.setType(new CType_Pointer(currentVD.getType()));

                    // add by tangrong
                    PointerVariableDomain varDomain = new PointerVariableDomain(vnd4AnnoyPtr, expdata.currentvex);
                    varDomain.setPointTo(expdata.vd);
                    expdata.vd = varDomain;
                }
                return data;
            } else if (o.equals("*")) { // ָ��ȡֵ��������*p
                /**
                 * ��ȡ*p��Ӧ��vd�����û������һ�� add by tangrong 2011-12-15
                 */
                if (expdata.vd instanceof ArrayVariableDomain) {
                    VariableNameDeclaration vnd;
                    Expression subIndex = new Expression(0);
                    String image = expdata.vd.getVariableNameDeclaration().getName() + "[" + subIndex + "]";
                    Scope scope = node.getScope();
                    NameDeclaration decl = Search.searchInVariableAndMethodUpward(image, scope);

                    if (decl instanceof VariableNameDeclaration) {
                        vnd = (VariableNameDeclaration) decl;
                    } else {
                        String tmp = node.getImage();
                        vnd = new VariableNameDeclaration(node.getFileName(), node.getScope(), image, node, subIndex.toString());
                        // vnd.setType(expdata.vd.getType().getNormalType());
                        vnd.setType(CType.getNextType(expdata.vd.getType()));
                        vnd.setParent(expdata.vd.getVariableNameDeclaration());
                    }

                    VariableDomain memvd = VariableDomain.newInstance(vnd, expdata.vd.getVariableSource().next(), expdata.currentvex);
                    // add by yaochi
                    if (memvd instanceof PointerVariableDomain && ((PointerVariableDomain) memvd).getPointerTo() != null) {// ���VDΪָ����ָ��ָ�������Ϊָ�����ͣ�����state��ΪNOTNULL
                        if (!(((PointerVariableDomain) memvd).getPointerTo() instanceof PointerVariableDomain))
                            ((PointerVariableDomain) memvd).setStateNotNull();
                    }
                    // end
                    ((ArrayVariableDomain) expdata.vd).addMember(subIndex, memvd);
                    VariableDomain currentVD = memvd;
                    VariableNameDeclaration vnd4AnnoyPtr = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoyPtr7", node, "annoyPtr7");
                    vnd4AnnoyPtr.setType(new CType_Pointer(currentVD.getType()));
                    PointerVariableDomain varDomain = new PointerVariableDomain(vnd4AnnoyPtr, expdata.currentvex);
                    varDomain.setPointTo(currentVD);
                    expdata.vd = varDomain.getPointerTo();
                } else if (!(expdata.vd instanceof FunctionVariableDomain)) {// ��*�ŵ������������ͣ���˵��Ϊָ������,ͬʱ��������Ǻ���ָ������
                    PointerVariableDomain varDomain = (PointerVariableDomain) expdata.vd;
                    if (varDomain.getPointerTo() == null) {
                        if (varDomain.getVariableNameDeclaration() != null) {
                            String image = "*" + varDomain.getVariableNameDeclaration().getName();
                            if (!image.equals("")) {
                                Scope scope = node.getScope();
                                VariableNameDeclaration decl = (VariableNameDeclaration) Search.searchInVariableAndMethodUpward(image, scope);//
                                VariableDomain memvd = expdata.currentvex.getVariableDomain(decl);
                                if (memvd != null)
                                    varDomain.setPointTo(memvd);
                            }
                        }
                    }

                    if (varDomain.getPointerTo() == null) {
                        // ָ�����û��ָ��ĳһ����������
                        varDomain.initMemberVD();
                    }
                    varDomain.setStateNotNull();// ���ָ�븳ֵ
                    if (varDomain.initStateIsNull())
                        varDomain.setInitStateAndPtNotNull();
                    // if()
                    VariableDomain childvd = ((PointerVariableDomain) expdata.vd).getPointerTo();

                    if (childvd != null && childvd.getNd() instanceof VariableNameDeclaration) {
                        if (expdata.currentvex.getValueSet().getValue((VariableNameDeclaration) childvd.getNd()) != null) {
                            // �Ѿ������ڳ����ڴ��У���ֱ��ȡֵ
                            expdata.vd = varDomain.getPointerTo();
                        } else if (childvd instanceof ArrayVariableDomain && childvd.getNd().getImage().startsWith("*")) {
                            // ����ָ��(*p)[2]��������
                            String image = childvd.getNd().getImage().replace("*", "").replace(".", "_") + "_point1arr";
                            VariableNameDeclaration vnd4MemoryBlock = new VariableNameDeclaration(node.getFileName(), node.getScope(), image, node, childvd.getNd().getImage());
                            // vnd4MemoryBlock.setType(new
                            // CType_Array(childvd.getType()));
                            // //Ϊ��ָ�뽨���ڴ棬��������Ӧ������ָ�������
                            vnd4MemoryBlock.setType(childvd.getType());
                            ArrayVariableDomain memBlock = new ArrayVariableDomain(vnd4MemoryBlock, varDomain.getVariableSource(), expdata.currentvex);

                            SymbolFactor factor = SymbolFactor.genSymbol(CType_BaseType.intType);
                            expdata.currentvex.addSymbolDomain(factor, new IntegerDomain(0, 0));
                            Expression addr = new Expression(factor);

                            varDomain.setPointTo(memBlock);
                            // memBlock.addMember(addr, childvd);
                            memBlock.assign(childvd);
                            expdata.vd = memBlock;
                        } else {
                            List list = node.getParentsOfType(ASTPostfixExpression.class);
                            if (list.isEmpty() == false) {
                                ASTPostfixExpression ASTnode = (ASTPostfixExpression) list.get(0);
                                ArrayList<String> operators = ASTnode.getOperatorType();
                                if (operators.contains("[")) {// �����д���[������Ҫ���ڴ���*p��[1]����
                                    String image = childvd.getNd().getImage().replace("*", "") + "_point2arr";
                                    VariableNameDeclaration vnd4MemoryBlock = new VariableNameDeclaration(node.getFileName(), node.getScope(), image, node, childvd.getNd().getImage());
                                    vnd4MemoryBlock.setType(childvd.getType()); // Ϊ��ָ�뽨���ڴ棬��������Ӧ������ָ�������
                                    PointerVariableDomain memBlock = new PointerVariableDomain(vnd4MemoryBlock, varDomain.getVariableSource(), expdata.currentvex);
                                    if (((PointerVariableDomain) childvd).getPointerTo() != null) {
                                        ((PointerVariableDomain) childvd).setStateNotNull();
                                    }
                                    if (memBlock.getPointerTo() != null) {
                                        memBlock.setStateNotNull();
                                    }
                                    ((PointerVariableDomain) varDomain).setPointTo(memBlock);
                                    expdata.vd = varDomain.getPointerTo();
                                } else {
                                    expdata.vd = varDomain.getPointerTo();
                                }
                            }
                        }
                    } else if (childvd != null && childvd.getNd() instanceof MethodNameDeclaration) {
                        expdata.vd = varDomain.getPointerTo();
                    }

                }
            } else if (o.equals("+")) {
                // ������
            } else if (o.equals("-")) {
                expdata.value = new Expression(0).sub(expdata.value);
                // add by tangrong 2012-8-28
                VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                tmpVND.setType(expdata.vd.getType());
                if (expdata.vd instanceof PrimitiveVariableDomain) {
                    PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) expdata.vd;
                    boolean constFlag = pvd.isConst();
                    pvd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, new Expression(0).sub(((PrimitiveVariableDomain) expdata.vd).getExpression()));
                    if (constFlag) {
                        pvd.setConst(true);
                    }
                    expdata.vd = pvd;
                }
                // add by tangrong end;
                return data;
            } else if (o.equals("~")) {
                IntegerDomain i = Domain.castToIntegerDomain(expdata.value.getDomain(expdata.currentvex.getSymDomainset()));
                if (i != null && i.isCanonical()) {
                    expdata.value = new Expression(new IntegerFactor(~i.getMin()));
                    // add by jinkaifeng 2012.10.12
                    VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                    tmpVND.setType(expdata.vd.getType());
                    if (expdata.vd instanceof PrimitiveVariableDomain) {
                        expdata.vd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, expdata.value);
                    }
                    // add end
                    return data;
                }
            } else if (o.equals("!")) {
                if (expdata.value == null) {
                    SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
                    expdata.currentvex.addSymbolDomain(sym, new IntegerDomain(0, 1));
                    expdata.value = new Expression(sym);
                    return data;
                }
                if (expdata.vd instanceof PrimitiveVariableDomain) {
                    IntegerDomain i = Domain.castToIntegerDomain(expdata.value.getDomain(expdata.currentvex.getSymDomainset()));
                    // add by tangrong 2012-8-28
                    VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                    tmpVND.setType(expdata.vd.getType());
                    i = Domain.castToIntegerDomain(((PrimitiveVariableDomain) expdata.vd).getExpression().getDomain(expdata.currentvex.getSymDomainset()));
                    // add by tangrong end;
                    if (i != null) {
                        if (i.isCanonical() && i.getMin() == 0) {
                            expdata.value = new Expression(0);
                            // add by tagnrong 2012-8-28
                            if (expdata.vd instanceof PrimitiveVariableDomain) {
                                expdata.vd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, new Expression(0));
                            }
                            // add end
                            return data;
                        } else if (!i.contains(0)) {
                            expdata.value = new Expression(1);
                            // add by tagnrong 2012-8-28
                            if (expdata.vd instanceof PrimitiveVariableDomain) {
                                expdata.vd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, new Expression(1));
                            }
                            // add end
                            return data;
                        } else {
                            SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
                            expdata.currentvex.addSymbolDomain(sym, new IntegerDomain(0, 1));
                            expdata.value = new Expression(sym);
                            expdata.vd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, new Expression(sym));
                            return data;
                        }
                    }
                }
                if (expdata.vd instanceof PointerVariableDomain) {
                    return data;// add by yaochi 20130711
                }
            } else {
                throw new RuntimeException("ASTUnaryOperator error!");
            }
        } else if (node.getImage().equals("sizeof")) {

            node.jjtGetChild(0).jjtAccept(this, expdata);
            ASTConstant con = (ASTConstant) node.getSingleChildofType(ASTConstant.class);
            if (node.jjtGetChild(0) instanceof ASTTypeName) {
                ASTTypeName typeName = (ASTTypeName) node.jjtGetChild(0);
                if (typeName.getType() != null) {
                    expdata.value = new Expression(typeName.getType().getSize());
                    // add by yaochi 2013-09-04 ��ӶԳ����ڴ��֧��
                    VariableNameDeclaration sizeofvnd = new VariableNameDeclaration(node.getFileName(), node.getScope(), "countofsize", node, "countofsize");
                    sizeofvnd.setType(CType_BaseType.intType);
                    PrimitiveVariableDomain sizeofpvd = new PrimitiveVariableDomain(sizeofvnd, expdata.currentvex, expdata.value);
                    sizeofpvd.setExpression(expdata.value);
                    expdata.vd = sizeofpvd;
                    // addend
                    return data;
                }
            }
            if (con != null) {
                // liuli 2010.8.13 ��Ӷ�sizeof ("qwefff");��������Ĵ���
                Domain ret = expdata.value.getDomain(con.getCurrentVexNode().getSymDomainset());
                if (ret != null && ret instanceof PointerDomain) {
                    PointerDomain p = (PointerDomain) ret;
                    long i = p.getLen(p);
                    expdata.value = new Expression(i);
                    // add by yaochi 2013-09-04 ��ӶԳ����ڴ��֧��
                    VariableNameDeclaration sizeofvnd = new VariableNameDeclaration(node.getFileName(), node.getScope(), "countofsize", node, "countofsize");
                    sizeofvnd.setType(CType_BaseType.intType);
                    PrimitiveVariableDomain sizeofpvd = new PrimitiveVariableDomain(sizeofvnd, expdata.currentvex, expdata.value);
                    sizeofpvd.setExpression(expdata.value);
                    expdata.vd = sizeofpvd;
                    // addend
                    return data;
                }
            } else {
                // liuli��sizeof�Ĳ����п���Ϊ�Զ�������ͣ����﷨���в�������ASTTypeNameʶ��
                Scope scope = node.getScope();
                String image = ((ASTUnaryExpression) node.jjtGetChild(0)).getImage();
                NameDeclaration decl = Search.searchInVariableUpward(image, scope);
                if (decl != null && decl.getType() != null) {
                    expdata.value = new Expression(decl.getType().getSize());
                    // add by yaochi 2013-09-04 ��ӶԳ����ڴ��֧��
                    VariableNameDeclaration sizeofvnd = new VariableNameDeclaration(node.getFileName(), node.getScope(), "countofsize", node, "countofsize");
                    sizeofvnd.setType(CType_BaseType.intType);
                    PrimitiveVariableDomain sizeofpvd = new PrimitiveVariableDomain(sizeofvnd, expdata.currentvex, expdata.value);
                    sizeofpvd.setExpression(expdata.value);
                    expdata.vd = sizeofpvd;
                    // addend
                    return data;
                }
                // end
                AbstractExpression child = (AbstractExpression) node.jjtGetChild(0);
                if (child.getType() != null) {
                    if (child.getFirstChildOfType(ASTFieldId.class) != null && ((AbstractExpression) child.getFirstChildOfType(ASTFieldId.class)).getType() != null)
                        child = (AbstractExpression) child.getFirstChildOfType(ASTFieldId.class);
                    expdata.value = new Expression(child.getType().getSize());
                    // add by yaochi 2013-09-04 ��ӶԳ����ڴ��֧��
                    VariableNameDeclaration sizeofvnd = new VariableNameDeclaration(node.getFileName(), node.getScope(), "countofsize", node, "countofsize");
                    sizeofvnd.setType(CType_BaseType.intType);
                    PrimitiveVariableDomain sizeofpvd = new PrimitiveVariableDomain(sizeofvnd, expdata.currentvex, expdata.value);
                    sizeofpvd.setExpression(expdata.value);
                    expdata.vd = sizeofpvd;
                    // addend
                    return data;
                }
            }
        } else if (node.getImage().contains("alignof")) {
            // ��֪����ô����
        } else if (node.getImage().contains("real")) {
            // ��֪����ô����
        } else if (node.getImage().contains("imag")) {
            // ��֪����ô����
        } else {

            // ��֪����ô����
        }

        expdata.value = new Expression(SymbolFactor.genSymbol(node.getType()));
        if (expdata.vd instanceof PrimitiveVariableDomain) {
            PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) expdata.vd;
            expdata.value = pvd.getExp();
        }
        return data;
    }

    /**
     * ����λ������ʽ
     * 
     * @param node
     * @param data
     * @param op
     * @return
     *         created by Yaoweichang on 2015-04-17 ����4:01:00
     */
    private Object dealBinaryBitOperation(AbstractExpression node, Object data, String op) {
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        node.jjtGetChild(0).jjtAccept(this, expdata);
        Expression leftvalue = expdata.value;
        Expression rightvalue = null;
        VariableDomain leftVD = expdata.vd;
        VariableDomain rightVD = null;
        String binaryString = null;

        try {
            // �����ҽ��з��ż���
            for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                Node c = node.jjtGetChild(i);
                expdata.value = null;
                expdata.vd = null;
                c.jjtAccept(this, expdata);
                rightvalue = expdata.value;
                rightVD = expdata.vd;

                if (leftvalue == null || rightvalue == null) {
                    throw new MyNullPointerException("BinaryBitOperation Value NULL in(.i) " + node.getBeginFileLine());
                }

                IntegerDomain i1 = Domain.castToIntegerDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
                IntegerDomain i2 = Domain.castToIntegerDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
                if (i1 == null || i2 == null) {
                    throw new MyNullPointerException("BinaryBitOperation Domain NULL in(.i) " + node.getBeginFileLine());
                }

                if (i1 != null && i2 != null) {
                    if (i1.isCanonical() || i2.isCanonical()) {
                        if (i1.isCanonical() && i2.isCanonical()) {
                            long temp = 0;
                            PrimitiveVariableDomain left = (PrimitiveVariableDomain) leftVD;
                            PrimitiveVariableDomain right = (PrimitiveVariableDomain) rightVD;
                            Expression tmpvalue;
                            if (op.equals("&")) {
                                temp = i1.getMin() & i2.getMin();
                            } else if (op.equals("|")) {
                                temp = i1.getMin() | i2.getMin();
                            } else if (op.equals("^")) {
                                temp = i1.getMin() ^ i2.getMin();
                            } else if (op.equals(">>")) {
                                temp = i1.getMin() >> i2.getMin();
                            } else if (op.equals("<<")) {
                                temp = i1.getMin() << i2.getMin();
                            } else if (op.equals("%")) {
                                temp = i1.getMin() % i2.getMin();
                            }
                            leftvalue = new Expression(new IntegerFactor(temp));
                            tmpvalue = leftvalue;
                            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                            tmpVND.setType(left.getType());
                            leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, tmpvalue);
                        } else if (op.equals("<<")) {
                            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmpVND" + node.toString(), node, "tmpVND" + node.toString());
                            long exp = i2.getMin();
                            Expression Ratioexp = new Expression((int) Math.pow(2, exp));

                            tmpVND.setType(CType_BaseType.getBaseType("long"));
                            if (leftVD instanceof PrimitiveVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                                Expression tmpExpr = ((PrimitiveVariableDomain) leftVD).getExpression().mul(Ratioexp);
                                leftvalue = tmpExpr;
                                leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, tmpExpr);
                            }
                        } else if (op.equals(">>")) {
                            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmpVND" + node.toString(), node, "tmpVND" + node.toString());
                            long exp = i2.getMin();
                            Expression Ratioexp = new Expression((int) Math.pow(2, exp));

                            tmpVND.setType(CType_BaseType.getBaseType("long"));
                            if (leftVD instanceof PrimitiveVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                                Expression tmpExpr = ((PrimitiveVariableDomain) leftVD).getExpression().div(Ratioexp);
                                leftvalue = tmpExpr;
                                leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, tmpExpr);
                            }
                        } else {
                            Expression unSureExpr = null;
                            VariableDomain unSureVD = null;
                            // ȷ���ĸ�ֵ����ȷ����
                            if (i1.isCanonical()) {
                                binaryString = Long.toBinaryString(i1.getMin());
                                unSureExpr = rightvalue;
                                unSureVD = rightVD;
                            } else {
                                binaryString = Long.toBinaryString(i2.getMin());
                                unSureExpr = leftvalue;
                                unSureVD = leftVD;
                            }
                            if (binaryString.length() < Long.SIZE) {
                                StringBuffer tempSb = new StringBuffer();
                                for (int m = 0; m < (Long.SIZE - binaryString.length()); m++) {
                                    tempSb.append('0');
                                }
                                binaryString = tempSb.append(binaryString).toString();
                            }
                            // ɨ����֪����ȷ���������ַ�������
                            int count1 = 0, count2 = 0, count3 = 0;
                            int p = 0, q = 0;
                            char atBegin = binaryString.charAt(0);
                            while (q < binaryString.length() && binaryString.charAt(p) == binaryString.charAt(q)) {
                                q++;
                                count1++;
                            }
                            p = q;
                            while (q < binaryString.length() && binaryString.charAt(p) == binaryString.charAt(q)) {
                                q++;
                                count2++;
                            }
                            p = q;
                            while (q < binaryString.length() && binaryString.charAt(p) == binaryString.charAt(q)) {
                                q++;
                                count3++;
                            }
                            int temp = count1 + count2 + count3;
                            if (temp == binaryString.length() && node.containsParentOfType(ASTEqualityExpression.class)) {
                                /*
                                 * atBegin='0'֤���������ַ�����"000��111��000"����ʽ��
                                 * ��ʱҪ�����������м丨�����������ͷ���binaryHelp��binaryZ, �����x
                                 * &y��y��ֵ����ȷ��ֵ�����ձ���Ϊleftvd=x-RatioForHelp
                                 * *binaryHelp-binaryZ;
                                 * �����x|y��y��ֵ����ȷ��ֵ�����ձ���Ϊleftvd
                                 * =x+RatioForHelp*binaryHelp
                                 */
                                if (atBegin == '0' && (op.equals("&") || (op.equals("|")))) {// baiyu
                                    int begin = binaryString.length() - count1 - count2 + 1;// 1��ʼ��λ��
                                    int end = binaryString.length() - count1;// 1������λ��
                                    ArrayList<VariableNameDeclaration> tempList = new ArrayList<VariableNameDeclaration>();// ����븨��������صı���

                                    // �����м����binaryHelp,������Ϊ&ʱ��Ҫ���м������ȡֵ��ΧΪi1/pow(2,end),�������ֱ�������𣿣���
                                    VariableNameDeclaration binaryHelpVND =
                                            new VariableNameDeclaration(node.getFileName(), node.getScope(), "bianryHelp_" + node.toString(), node, "binaryHelp" + node.toString());
                                    binaryHelpVND.setType(CType_BaseType.getBaseType("int"));
                                    SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"), "binaryHelp");
                                    sym.setRelatedVar(binaryHelpVND);
                                    Expression binaryHelpExp = new Expression(sym);
                                    VariableDomain binaryHelpVD = new PrimitiveVariableDomain(binaryHelpVND, VariableSource.LOCAL, expdata.currentvex, sym);
                                    expdata.currentvex.addValue(binaryHelpVND, binaryHelpVD);// ��ʵ������binaryHelp�ĳ�ʼ�����Ѿ�ȷ��Ϊ[-inf.+inf]

                                    if (op.equals("&")) {
                                        int tmp1 = (int) Math.pow(2, end);
                                        Expression RatioForHelp = new Expression(tmp1);// ����binaryHelp��ϵ��������ɨ���ַ����Ľ���õ�
                                        IntegerDomain temp1 = new IntegerDomain((int) Math.pow(2, end), (int) Math.pow(2, end));
                                        IntegerDomain binaryHelpDomain = IntegerDomain.div(i1, temp1);
                                        expdata.currentvex.addSymbolDomain(sym, binaryHelpDomain);// �����������м���ű���binaryHelp�ĳ�ʼ���䣿��

                                        // �����м���ű���binaryZ,ȡֵ����Ϊ[0,pow(2,begin-1)-1]
                                        VariableNameDeclaration binaryZVND =
                                                new VariableNameDeclaration(node.getFileName(), node.getScope(), "binaryZ_" + node.toString(), node, "binaryZ_" + node.toString());
                                        binaryZVND.setType(CType_BaseType.getBaseType("int"));
                                        SymbolFactor symZ = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"), "binaryZ");
                                        symZ.setRelatedVar(binaryZVND);
                                        Expression binaryZExp = new Expression(symZ);
                                        VariableDomain binaryZVD = new PrimitiveVariableDomain(binaryZVND, VariableSource.LOCAL, expdata.currentvex, symZ);
                                        IntegerDomain temp2 = new IntegerDomain(0, (int) Math.pow(2, begin - 1) - 1);// binaryZ�ĳ�ʼ����
                                        Expression lowbound = new Expression(0);
                                        Expression highbound = new Expression((int) (Math.pow(2, begin - 1) - 1));

                                        expdata.currentvex.addValue(binaryZVND, binaryZVD);
                                        // expdata.currentvex.addSymbolDomain(symZ,
                                        // temp2);//��ʼ�������������ã���

                                        // Ϊ���κ���������һ���������������ʽ��VD
                                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                                        tmpVND.setType(CType_BaseType.getBaseType("int"));
                                        if (unSureVD instanceof PrimitiveVariableDomain && binaryHelpVD instanceof PrimitiveVariableDomain && binaryZVD instanceof PrimitiveVariableDomain) {
                                            Expression tmpExpr = ((PrimitiveVariableDomain) unSureVD).getExpression().sub(((PrimitiveVariableDomain) binaryHelpVD).getExpression().mul(RatioForHelp));
                                            tmpExpr = tmpExpr.sub(((PrimitiveVariableDomain) binaryZVD).getExpression());
                                            leftvalue = tmpExpr;
                                            leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, tmpExpr);
                                            expdata.Help = new HashMap<Expression, IntegerDomain>();
                                            expdata.Help.put(binaryZExp, temp2);
                                        }
                                    } else if (op.equals("|")) {// ������Ϊ|ʱ��binaryHelp��ȡֵ��Χ��[0,pow(2,count2)-1]
                                        int tmp1 = (int) Math.pow(2, begin - 1);
                                        IntegerDomain bianryHelpDomain = new IntegerDomain(0, (int) (Math.pow(2, count2) - 1));
                                        Expression RatioForHelp = new Expression(tmp1);
                                        // expdata.currentvex.addSymbolDomain(sym,
                                        // bianryHelpDomain);
                                        //
                                        VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                                        tmpVND.setType(CType_BaseType.getBaseType("int"));
                                        if (unSureVD instanceof PrimitiveVariableDomain && binaryHelpVD instanceof PrimitiveVariableDomain) {
                                            Expression tmpExpr = ((PrimitiveVariableDomain) unSureVD).getExpression().add(((PrimitiveVariableDomain) binaryHelpVD).getExpression().mul(RatioForHelp));
                                            leftvalue = tmpExpr;
                                            leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, tmpExpr);
                                            Expression lowbound = new Expression(0);
                                            Expression highbound = new Expression((int) (Math.pow(2, count2) - 1));
                                            expdata.Help = new HashMap<Expression, IntegerDomain>();
                                            expdata.Help.put(binaryHelpExp, bianryHelpDomain);
                                        }
                                    }
                                } else if (atBegin == '1' && op.equals("&")) {
                                    /*
                                     * ��atBegin=='1'ʱ��˵���������ַ�����"111��000��111"����ʽ
                                     * ����ʱҪ������һ���м丨�����������ͷ���binaryHelp
                                     * ��ʱbinaryHelp��ȡֵ����Ϊ[0,pow(2,count2)-1]
                                     */
                                    int begin = binaryString.length() - count1 - count2 + 1;// 0��ʼ��λ��
                                    int end = binaryString.length() - count1;
                                    VariableNameDeclaration binaryHelpVND =
                                            new VariableNameDeclaration(node.getFileName(), node.getScope(), "bianryHelp_" + node.toString(), node, "binaryHelp" + node.toString());
                                    binaryHelpVND.setType(CType_BaseType.getBaseType("int"));
                                    SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"), "binaryHelp");
                                    sym.setRelatedVar(binaryHelpVND);
                                    Expression binaryHelpExp = new Expression(sym);
                                    VariableDomain binaryHelpVD = new PrimitiveVariableDomain(binaryHelpVND, VariableSource.LOCAL, expdata.currentvex, sym);
                                    int tmp1 = (int) Math.pow(2, begin - 1);
                                    IntegerDomain bianryHelpDomain = new IntegerDomain(0, (int) (Math.pow(2, count2) - 1));// ��ʼȡֵ����

                                    Expression RatioForHelp = new Expression(tmp1);
                                    expdata.currentvex.addValue(binaryHelpVND, binaryHelpVD);
                                    VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                                    tmpVND.setType(CType_BaseType.getBaseType("int"));
                                    if (unSureVD instanceof PrimitiveVariableDomain && binaryHelpVD instanceof PrimitiveVariableDomain) {
                                        Expression tmpExpr = ((PrimitiveVariableDomain) unSureVD).getExpression().sub(((PrimitiveVariableDomain) binaryHelpVD).getExpression().mul(RatioForHelp));
                                        leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, tmpExpr);
                                        leftvalue = tmpExpr;
                                        Expression lowbound = new Expression(0);
                                        Expression highbound = new Expression((int) (Math.pow(2, count2) - 1));
                                        expdata.Help = new HashMap<Expression, IntegerDomain>();
                                        expdata.Help.put(binaryHelpExp, bianryHelpDomain);
                                    }
                                } else
                                    throw new MyNullPointerException("BinaryOperation Can't change the form " + node.getBeginFileLine());
                            } else {// ����һ��δ֪��������� add by Yaoweichang
                                if (leftVD instanceof PrimitiveVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                                    PrimitiveVariableDomain left = (PrimitiveVariableDomain) leftVD;
                                    PrimitiveVariableDomain right = (PrimitiveVariableDomain) rightVD;
                                    Expression tmpvalue = null;
                                    VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                                    tmpVND.setType(left.getType());

                                    // �ϲ�
                                    if (op.trim().equals("&")) {// ��λ��
                                        leftvalue = leftvalue.and(rightvalue);
                                        tmpvalue = left.getExpression().and(right.getExpression());
                                    } else if (op.trim().equals("|")) {// ��λ��
                                        leftvalue = leftvalue.inclusiveOR(rightvalue);
                                        tmpvalue = left.getExpression().inclusiveOR(right.getExpression());
                                    } else if (op.trim().equals("^")) {// ��λ���
                                        leftvalue = leftvalue.exclusiveOR(rightvalue);
                                        tmpvalue = left.getExpression().exclusiveOR(right.getExpression());
                                    }
                                    leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, tmpvalue);
                                }
                            }

                        }
                    } else {// ����ȫ������λ���� add by Yaoweichang
                        if (leftVD instanceof PrimitiveVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                            PrimitiveVariableDomain left = (PrimitiveVariableDomain) leftVD;
                            PrimitiveVariableDomain right = (PrimitiveVariableDomain) rightVD;
                            Expression tmpvalue = null;
                            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                            tmpVND.setType(left.getType());

                            // �ϲ�
                            if (op.trim().equals("&")) {// ��λ��
                                leftvalue = leftvalue.and(rightvalue);
                                tmpvalue = left.getExpression().and(right.getExpression());
                            } else if (op.trim().equals("|")) {// ��λ��
                                leftvalue = leftvalue.inclusiveOR(rightvalue);
                                tmpvalue = left.getExpression().inclusiveOR(right.getExpression());
                            } else if (op.trim().equals("^")) {// ��λ���
                                leftvalue = leftvalue.exclusiveOR(rightvalue);
                                tmpvalue = left.getExpression().exclusiveOR(right.getExpression());
                            }
                            leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, tmpvalue);
                        }
                    }
                }
            }
        } catch (MyNullPointerException e) {
            super.visit(node, expdata);
            SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
            // add by xjx 2012-6-29
            MultiplicativeExpression multiexp = new MultiplicativeExpression(leftvalue, rightvalue, op);
            multiexp.setRelatedSymbolfactor(sym);
            expdata.origexplist.add(multiexp);

            if (Config.USEUNKNOWN)
                expdata.currentvex.addSymbolDomain(sym, IntegerDomain.getUnknownDomain());
            else
                expdata.currentvex.addSymbolDomain(sym, IntegerDomain.getFullDomain());
            leftvalue = new Expression(sym);
            expdata.value = leftvalue;
            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
            tmpVND.setType(expdata.vd.getType());
            leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, leftvalue);
            expdata.vd = leftVD;
            return data;
        }
        expdata.value = leftvalue;
        expdata.vd = leftVD;
        return data;
    }

    public Object visit(ASTInitializerList node, Object data) {
        ArrayVariableDomain avd = null;
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        avd = new ArrayVariableDomain(expdata.currentvex);
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            Node c = node.jjtGetChild(i);
            expdata.value = null;
            expdata.vd = null;
            c.jjtAccept(this, expdata);
            Expression exp = new Expression(i);
            if (expdata.vd instanceof PrimitiveVariableDomain) {
                PrimitiveVariableDomain pvd = ((PrimitiveVariableDomain) expdata.vd);
                if (pvd.isConst()) {
                    pvd.setConst(false);
                }
            }
            avd.addMember(exp, expdata.vd);
        }
        expdata.vd = avd;
        return data;
    }

    public Object visit(ASTDirectDeclarator node, Object data) {
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        NameDeclaration nd = node.getDecl();
        if (nd instanceof VariableNameDeclaration) {
            // ��������
            VariableNameDeclaration v = (VariableNameDeclaration) nd;
            VariableDomain vd = expdata.currentvex.getVariableDomain(v);
            if (vd != null) {
                VariableDomain expvd = expdata.vd;
                expdata.vd = vd;
                if (expvd != null && expvd.getType().equals(vd.getType())) {
                    if (((VariableNameDeclaration) expvd.getNd()).getAncestor().equals(((VariableNameDeclaration) vd.getNd()).getAncestor())) {
                        expdata.vd = expvd;
                    }
                }
            }
            if (!Config.Field) {
                if (v.getType() instanceof CType_Array && ((CType_Array) v.getType()).getOriginaltype() instanceof CType_Struct || v.getType() instanceof CType_Struct)
                    return data;
            }
            if (expdata.vd == null) {
                VariableSource varSource = (v.getScope() instanceof SourceFileScope || v.getScope() instanceof MethodScope) ? VariableSource.INPUT : VariableSource.LOCAL;
                expdata.vd = VariableDomain.newInstance(v, varSource, expdata.currentvex);
                if (expdata.vd.getNd() != null && expdata.vd.getNd().getNode() instanceof ASTEnumerator) {
                } else {
                    expdata.currentvex.addVariableDomain(expdata.vd);
                }
            }
        }
        return data;
    }

    /**
     * @author wangyi
     * @param node
     * @return
     */
    private boolean isConstrainNode(SimpleNode node) {
        return (node.jjtGetParent().jjtGetParent() instanceof ASTSelectionStatement || node.jjtGetParent().jjtGetParent() instanceof ASTIterationStatement
                || node.jjtGetParent() instanceof ASTSelectionStatement || node.jjtGetParent() instanceof ASTIterationStatement);
    }

    static Logger logger = Logger.getRootLogger();

    class MyNullPointerException extends Exception {
        public MyNullPointerException(String msg) {
            // logger.info("�����Զ����쳣��"+msg);
        }
    }

    /**
     * @author wangyi
     * @param node
     * @return
     */
    private boolean isSpecialStringFunction(ASTAssignmentExpression node) {
        String[] specials = {"strcat", "strcpy", "strncmp", "strncpy"};
        for (String str : specials) {
            if (str.equals(node.getImage())) {
                return true;
            }
        }
        return false;
    }
}
