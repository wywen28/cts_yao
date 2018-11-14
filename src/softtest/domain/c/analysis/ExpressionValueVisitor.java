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
 * 在一些叠加表达式计算时（如LogicalORExpression，AdditiveExpression....),其原理是先计算最左表达式的值，
 * 然后依次计算后续表达式的值，根据左右值之间的操作符计算出中间结果，把中间结果向后续的计算进行传递，得到最终表达式的值；
 * 
 * 如果在计算过程中某一环节出现左右值为NULL的情况，则： 1、首先计算完后续的表达式，以防止其中包含变量的修改（如if(i*k>0 &&
 * i++>5),如果左值i*k>0计算不出来，则继续计算&&右侧表达式， 防止漏掉其中的i++运算； 2、在所有表达式都计算完毕后，为整个表达式赋一个全区间
 * 3、以上两步骤的代码通过捕获异常的方式实现
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
            // 从左到右进行符号计算
            for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                Node c = node.jjtGetChild(i);
                expdata.value = null;
                expdata.vd = null;
                c.jjtAccept(this, expdata);// 子终结点为const类型可以F6//这里是访问右边的节点
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
                // 指针和指针相减得到的是地址的差值，先粗略赋个int型
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
                 * 指针的处理分5步，以p+1为例 1. 获取指针p的指向域的抽象内存m0。 2.
                 * 在控制流图节点VexNode里查找ArrayVaribaleDomain里有没有哪个数组
                 * （连续抽象内存空间）中含有m0。如果存在返回此数组抽象内存的。 3.
                 * 如果没有，新建一匿名数组（连续抽象内存空间），m0为数组中的一成员
                 * ，它在数组中的地址（下标）为新建符号s，s的约束时大约等于0； 4.
                 * 返回p的指向域m0在连续抽象内存空间中的下标，查找连续抽象内存中是否存在
                 * （下标+1)的元素，存在则返回，不存在则新建一块内存单元m1，将<下标+1，m1>存入连续抽象内存中。 5.
                 * 执行p=p+1的操作。
                 */
                if (leftVD instanceof PointerVariableDomain && rightVD instanceof PrimitiveVariableDomain) {// 来到等式的左边
                    PointerVariableDomain left = (PointerVariableDomain) leftVD;
                    PrimitiveVariableDomain right = (PrimitiveVariableDomain) rightVD;
                    // 步骤1:(*p)不为NULL，获取(*p)所在的抽象内存
                    if (left.getPointerTo() == null) {
                        left.initMemberVD();
                    }
                    left.setStateNotNull();
                    VariableDomain pt = left.getPointerTo();

                    // 步骤2 查找是否有连续的内存地址中含有pt。
                    ValueSet vset = expdata.currentvex.getValueSet();
                    ArrayVariableDomain memoryBlock = vset.getMemoryBlockContainVD(pt);

                    // 步骤3 如果没有，新建一匿名数组（连续抽象内存空间）
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

                    // 步骤4 地址偏移1：addr+1,查找是否存在下标为addr+1对应的元素，存在则返回，不存在新建。
                    Expression addrAfter;
                    if (operator.equals("+")) {
                        addrAfter = addr.add(right.getExpression());
                    } else {// '-'号
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

                    // 步骤5 执行tmp=p+1的操作
                    PointerVariableDomain tmp = new PointerVariableDomain(left.getVariableNameDeclaration(), left.getVariableSource(), left.getNode()); // vnd有问题
                    tmp.setPointTo(ptAfter);
                    leftVD = tmp;
                }
                // add end
                /*
                 * add by jinkaifeng 2012.10.08处理数组地址的加减，如a[2]={0,1}; a+1
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
                     * right.getExpression(); } else {//理论上不会有减号 addrAfter =
                     * new.sub(right.getExpression()); }
                     */

                    addrAfter = right.getExpression();

                    // 抽象内存模型memAfter
                    // VariableDomain memAfter = left.getMember(addrAfter);
                    VariableDomain memAfter = left.getMember(addrAfter, expdata.currentvex.getSymDomainset());
                    if (memAfter != null) {
                        VariableNameDeclaration vnd4AnnoyPtr = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoyPtrTest", node, "annoyPtr");// 匿名指针的变量声明
                        vnd4AnnoyPtr.setType(new CType_Pointer(CType.getNextType(arrayType)));//

                        PointerVariableDomain annoyPtr = new PointerVariableDomain(vnd4AnnoyPtr, left.getVariableSource(), expdata.currentvex);// 指针指向地址

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

                        VariableNameDeclaration vnd4AnnoyPtr = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoyPtrTest", node, "annoyPtr");// 匿名指针的变量声明
                        vnd4AnnoyPtr.setType(new CType_Pointer(CType.getNextType(arrayType)));//
                        PointerVariableDomain annoyPtr = new PointerVariableDomain(vnd4AnnoyPtr, left.getVariableSource(), expdata.currentvex);// 指针指向地址

                        annoyPtr.setPointTo(memAfter);

                        leftVD = annoyPtr;

                    }// end 上面运行之后memAfter不为null
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

                        // 用指针指向可以解决*(num+2)问题？
                        VariableNameDeclaration vnd4AnnoyPtr = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoyPtrTest", node, "annoyPtr");// 匿名指针的变量声明
                        vnd4AnnoyPtr.setType(new CType_Pointer(CType.getNextType(arrayType)));//
                        PointerVariableDomain annoyPtr = new PointerVariableDomain(vnd4AnnoyPtr, left.getVariableSource(), expdata.currentvex);// 指针指向地址
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
        // 在ASTPostfixExpression中统一处理
        return super.visit(node, data);
    }

    // modified by zhouhb
    public Object visit(ASTInitDeclarator node, Object data) {
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        ASTDeclarator declarator = null;
        if (node.jjtGetChild(0) instanceof ASTDeclarator) {
            declarator = (ASTDeclarator) node.jjtGetChild(0);// 得到他的子节点，此节点为左值如
                                                             // int *c =
                                                             // a；中的*c
        } else if (node.jjtGetChild(1) instanceof ASTDeclarator) {
            declarator = (ASTDeclarator) node.jjtGetChild(1);
        }
        NameDeclaration decl = declarator.getDecl();// Declarator 声明符 variable
                                                    // 变量
        // 不处理结构体相关信息
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
         */// 取消将2重指针理解作为结构体类型不做处理 add by yaochi
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
        if (node.jjtGetNumChildren() == 1) {// 只有一个孩子节点 PS.这里使用老的数据结构
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
                // 修改了数组初始化时关联指针的属性为NULL
                p.setValue(PointerValue.NOTNULL);
                // modified by zhouhb 2010/6/22
                // 增加了利用全局变量初始化数组的功能 eg.char[a]
                ASTConstantExpression constant = (ASTConstantExpression) node.getFirstChildOfType(ASTConstantExpression.class);
                // 判断是否为char a[]这种未定义维度的数组声明
                if (constant != null) {
                    constant.jjtAccept(this, data);
                    IntegerDomain size = IntegerDomain.castToIntegerDomain(expdata.value.getDomain(expdata.currentvex.getSymDomainset()));
                    // 判断数组维数是否为函数参数
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
            if (node.jjtGetChild(1) instanceof ASTInitializer) {// 如果是一个赋值语句
                                                                // ASTInitializer
                                                                // 等价于 int a =
                                                                // c;中的c
                if (node.getType() instanceof CType_Array) {
                    // modified by zhouhb 2010/7/21//处理数组初始化长度eg.int
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
                 // 修改了空指针赋值使用
                else if (node.getType() instanceof CType_Pointer) {
                    if (node.containsChildOfType(ASTConstant.class) && !node.containsParentOfType(ASTEqualityExpression.class)
                            && ((ASTConstant) node.getFirstChildOfType(ASTConstant.class)).getImage().equals("0")) {
                        // 处理空指针NULL
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

                    // add by baiyu 2014.12.24 针对变量声明包含位运算时，添加的辅助变量的约束丢失的情况
                    if (expdata.Help != null) {
                        // expdata.currentvex.addHelpDomianToNode(v,
                        // expdata.Help);
                        rightvd.helpDomainForVD = expdata.Help;
                    }

                    if (leftvd instanceof PointerVariableDomain && rightvd instanceof ArrayVariableDomain) {
                        VariableNameDeclaration vnd;// 声明一个变量名声明，相当于声明了一个变量，
                                                    // ex：int a
                        Expression subIndex = new Expression(0);// Expression
                                                                // 保存别名
                        String image = rightvd.getVariableNameDeclaration().getName();// 得到变量的名称
                        Scope scope = node.getScope();// 得到节点作用域范围
                        NameDeclaration decla = Search.searchInVariableAndMethodUpward(image, scope);// 代表通过变量名称和作用范围搜索，标记符声明，
                        if (decla instanceof VariableNameDeclaration)// 如果decla是varibaleNameDeclaration的一个实例
                        {
                            vnd = (VariableNameDeclaration) decla;
                        } else {// 重新声明了一个变量声明，写入node中的信息
                            vnd = new VariableNameDeclaration(node.getFileName(), node.getScope(), image, node, subIndex.toString());
                            vnd.setType(rightvd.getType().getNormalType());
                            vnd.setParent(rightvd.getVariableNameDeclaration());
                        }

                        VariableDomain memvd = VariableDomain.newInstance(vnd, rightvd.getVariableSource().next(), expdata.currentvex);// 申明一个抽象内存的基类，类型与左值一致

                        VariableDomain currentVD = memvd;
                        VariableNameDeclaration vnd4AnnoyPtr = new VariableNameDeclaration(node.getFileName(), node.getScope(), "annoyPtr8", node, "annoyPtr8");// 申明了一个匿名变量
                        vnd4AnnoyPtr.setType(new CType_Pointer(currentVD.getType()));// 将匿名变量申明为指针类型
                        PointerVariableDomain varDomain = new PointerVariableDomain(vnd4AnnoyPtr, expdata.currentvex);// 申请一个PointVariableDomain类型对象，初始化使用之前填充好的指针类型匿名变量
                        varDomain.setPointTo(currentVD);// 指针类型抽象内存模型指向之前申请的memvd
                        expdata.vd = varDomain; // ExpressionVistorData
                                                // visit方法访问一个抽象语法树结点，得到的信息就存在这个里面

                    }
                    // add by baiyu 2014.12.25 针对变量声明包含位运算时，添加的辅助变量的约束丢失的情况

                    leftvd.assign(rightvd);
                    expdata.vd = leftvd;
                    expdata.currentvex.addVariableDomain(expdata.vd);
                }
            }
        }
        return data;
    }

    public Object visit(ASTAssignmentExpression node, Object data) {// 主要处理 赋值
                                                                    // 这样的符号
        ExpressionVistorData expdata = (ExpressionVistorData) data;

        // modified by zhouhb
        SimpleNode postfix = (SimpleNode) node.jjtGetParent().jjtGetParent();
        if (postfix.getImage().equals("calloc") && !(node.containsChildOfType(ASTConstant.class)))
            return data;
        if (node.jjtGetNumChildren() == 1) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(0);

            // 初始化SymbolExpression add by wangyi
            if (isConstrainNode((SimpleNode) node)) {// 是分支节点或循环节点
                if (expdata.currentSymbolExpression == null && ((child instanceof ASTLogicalORExpression) || (child instanceof ASTLogicalANDExpression))) {
                    SymbolExpression symbolExp = new SymbolExpression();
                    expdata.currentSymbolExpression = symbolExp;
                    expdata.currentvex.addSymbolExpression(symbolExp);
                }
                // 单独处理没有&&、没有||、子孩子为ASTUnaryExpression和ASTEqualityExpression的情况
                if (expdata.currentSymbolExpression == null
                        && !expdata.currentvex.getName().contains("for_inc") // 排除for_inc的情况
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
            // 提取类似(a+b)或者(a*b)这种表达式
            if (node.getParentsOfType(ASTAssignmentExpression.class).size() > 0 || node.getParentsOfType(ASTInitDeclarator.class).size() > 0) {
                // do nothing
            } else {
                // 处理只有一个变量作为表达式的情况 add by wangyi
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

                // 处理特殊的字符串函数 add by wangyi
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

            // 判断是否为MCDC选路，若是，则收集MCDC真值信息 add by wangyi
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
            // add by baiyu 2014.12.24 针对变量声明包含位运算时，添加的辅助变量的约束丢失的情况
            if (expdata.Help != null) {
                rightvd.helpDomainForVD = expdata.Help;
            }
            VariableDomain leftvd = null;// add by jinkaifeng 2012.10.12
            ASTPrimaryExpression p = (ASTPrimaryExpression) firstchild.getChildofType(ASTPrimaryExpression.class);// 得到当前节点单支子节点中为ASTPrimaryExpression类型的节点信息
            if (rightvalue == null && !(rightvd.getNd() instanceof MethodNameDeclaration))
                return data;
            if (secondchild.getOperatorType().get(0).equals("=")) {
                firstchild.jjtAccept(this, expdata);
                leftvd = expdata.vd;
                // add by zhouhb 2010/8/16
                // 修改了空指针赋值使用
                CType nodetype = node.getType();
                if (nodetype instanceof CType_Typedef) {
                    nodetype = nodetype.getSimpleType();
                }
                if (nodetype instanceof CType_Pointer) {// 被赋值的变量为指针
                    // add by zhouhb 2010/11/18
                    // 屏蔽指针数组
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
                        // 将符号运算表达式和整型表达式均转化为单符号（不知道为什么空指针会计算出上述两种情况）
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
                    if (leftvd instanceof PointerVariableDomain && rightvd.getNd() instanceof MethodNameDeclaration) {// 给指针变量赋函数地址
                        // 给指针变量赋函数地址时，鉴于抽象内存的表示形式，直接将righvd作为leftvd的指向就OK
                        ((PointerVariableDomain) leftvd).setPointTo(rightvd);
                        ((PointerVariableDomain) leftvd).setStateNotNull();
                        return data;
                    } else if (leftvd instanceof FunctionVariableDomain && rightvd instanceof FunctionVariableDomain) {
                        leftvd.assign(rightvd);
                    }
                    return data;
                }
                expdata.value = rightvalue;
                // add by jinkaifeng 2012.12.18 为了支持数组指针
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

                // add by jinkaifeng 2013.5.10解决函数返回值为指针的情况
                NameDeclaration nd;
                nd = rightvd.getNd();
                if ((rightvd instanceof PointerVariableDomain || rightvd instanceof StructVariableDomain) && nd instanceof MethodNameDeclaration) {

                    expdata.currentvex.popValue((MethodNameDeclaration) nd);
                    VariableSource vdSource = expdata.vd.getVariableSource();

                    // 将vd的variablesource设为返回值
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
                expdata.vd.assign(rightvd); // 这里出现S 了
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

                    // 步骤1:(*p)不为NULL，获取(*p)所在的抽象内存
                    PointerVariableDomain varDomain = (PointerVariableDomain) expdata.vd;
                    if (varDomain.getPointerTo() == null) {
                        varDomain.initMemberVD();
                    }
                    varDomain.setStateNotNull();
                    VariableDomain pt = varDomain.getPointerTo();

                    // 步骤2 查找是否有连续的内存地址中含有pt。
                    ValueSet vset = expdata.currentvex.getValueSet();
                    ArrayVariableDomain memoryBlock = vset.getMemoryBlockContainVD(pt);

                    // 步骤3 如果没有，新建一匿名数组（连续抽象内存空间）
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

                    // 步骤4
                    // 地址偏移1：addr+rightvalue,查找是否存在下标为addr+rightvalue对应的元素，存在则返回，不存在新建。
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

                    // 步骤5 执行p=p+1的操作
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

                    // 步骤1:(*p)不为NULL，获取(*p)所在的抽象内存
                    PointerVariableDomain varDomain = (PointerVariableDomain) expdata.vd;
                    if (varDomain.getPointerTo() == null) {
                        varDomain.initMemberVD();
                    }
                    varDomain.setStateNotNull();
                    VariableDomain pt = varDomain.getPointerTo();

                    // 步骤2 查找是否有连续的内存地址中含有pt。
                    ValueSet vset = expdata.currentvex.getValueSet();
                    ArrayVariableDomain memoryBlock = vset.getMemoryBlockContainVD(pt);

                    // 步骤3 如果没有，新建一匿名数组（连续抽象内存空间）
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

                    // 步骤4
                    // 地址偏移1：addr-rightvalue,查找是否存在下标为addr-rightvalue对应的元素，存在则返回，不存在新建。
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

                    // 步骤5 执行p=p+1的操作
                    ((PointerVariableDomain) expdata.vd).changePT(ptPlusRight);

                }
                // add end
            } else {
                node.jjtGetChild(0).jjtAccept(this, expdata);
                leftvalue = expdata.value;
                IntegerDomain i1 = Domain.castToIntegerDomain(leftvalue.getDomain(expdata.currentvex.getSymDomainset()));
                IntegerDomain i2 = Domain.castToIntegerDomain(rightvalue.getDomain(expdata.currentvex.getSymDomainset()));
                // 如果不能确定计算出值，直接产生一个抽象取值未定的符号
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
                        expdata.currentvex.addValue(v, expdata.value); // expdata.value陈旧数据类型，可不用
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
        // 由于新的编译环境将NULL编译为0，有时为(void *)0，故修改
        // add by zhouhb
        // 对于"=="表达式的处理时不产生空指针对应符号
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        // 强制类型转换可能会引起现有版本不能分析的类型的错误区间信息，在此屏蔽
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
                        if (name.contains("size_t")) {// size_t恒正
                            iDomain = (IntegerDomain) Domain.intersect(iDomain, new IntegerDomain(0, 1014), curtype);
                        }
                        expdata.currentvex.addSymbolDomain(p, iDomain);
                        expdata.value = new Expression(p);
                        if (expdata.vd.getVariableNameDeclaration() != null) {
                            pvd = (PrimitiveVariableDomain) PrimitiveVariableDomain.newInstance(expdata.vd.getVariableNameDeclaration(), VariableSource.LOCAL, expdata.currentvex);
                        } else {// 没有vnd 说明为const类型
                            pvd = PrimitiveVariableDomain.newIntConstant(iDomain.getMin());
                        }
                        pvd.setExpression(expdata.value);
                    } else if (expdata.vd instanceof PointerVariableDomain) {
                        // unsigned int 强制转化为0-1024
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
                    } else {// 没有vnd 说明为const类型
                        pvd = PrimitiveVariableDomain.newDoubleConstant(iDomain.getMin());
                    }

                    pvd.setExpression(expdata.value);
                    expdata.vd = pvd;

                } else if (name.contains("void")) {
                    if (CType.getOrignType(type.getType()).isClassType()) {
                        // 存在这种((NTP_Packet *)0)-> auth_keyid奇葩
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
            // modified by tangrong 2012-4-9 添加对==的支持
            // 处理空指针NULL(void*)0
            if (((ASTConstant) node.getFirstChildOfType(ASTConstant.class)).getImage().equals("0") && (((ASTTypeName) node.getFirstChildOfType(ASTTypeName.class)).getType() instanceof CType_Pointer)) {
                if (CType.getOrignType(type.getType()).isClassType()) {
                    // 存在这种((NTP_Packet *)0)-> auth_keyid奇葩
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
            // modified by tangrong 2012-4-9 添加对==的支持
            // 处理空指针NULL
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
        // 由于void ConditionalExpression() #ConditionalExpression(>1):
        // {}所以不会生成单支树，所以代码冗余了
        if (node.jjtGetNumChildren() == 1) {
            throw new RuntimeException("ASTConditionalExpression can't generate single child");
            // liuli:条件运算符的第二个表达式可以为空，像c = (++a ? : b);这种情况
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
                    expdata.value = new Expression(1);// 若第二个参数为空，缺省值为1
                    // by jinkaifeng 2013.5.13
                    VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                    tmpVND.setType(CType_BaseType.intType);
                    expdata.vd = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, expdata.value);

                }
                return data;
            }

            // 产生一个新的符号，域为两者的并
            CType type = node.getType();
            SymbolFactor sym = SymbolFactor.genSymbol(type);
            if (thirdvd instanceof PrimitiveVariableDomain) {// 暂时先只考虑数值
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

            // 产生一个新的符号，域为两者的并
            CType type = node.getType();
            SymbolFactor sym = SymbolFactor.genSymbol(type);
            if (secondvd instanceof PrimitiveVariableDomain && thirdvd instanceof PrimitiveVariableDomain) {// 先只处理数值域
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
        if (image.startsWith("\"") || image.startsWith("L\"") || image.equals("__FUNCTION__") || image.equals("__PRETTY_FUNCTION__") || image.equals("__func__")) {// 增加对L"abcd"形式的字符串赋值形式的处理
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
            // liuli:2010.7.23处理类似int i='DpV!';语句
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
                image = image.substring(0, image.length() - 1);// 以L结尾
                if (image.endsWith("l") || image.endsWith("L")) {
                    image = image.substring(0, image.length() - 1);// 以LL结尾
                    if (image.endsWith("u") || image.endsWith("U")) {
                        image = image.substring(0, image.length() - 1);// 以ULL结尾
                    }
                } else if (image.endsWith("u") || image.endsWith("U")) {
                    image = image.substring(0, image.length() - 1);// 以UL结尾
                }
            } else if (image.endsWith("u") || image.endsWith("U")) {
                image = image.substring(0, image.length() - 1);// 以U结尾
                if (image.endsWith("l") || image.endsWith("L")) {
                    image = image.substring(0, image.length() - 1);// 以LU结尾
                    if (image.endsWith("l") || image.endsWith("L")) {
                        image = image.substring(0, image.length() - 1);// 以LLU结尾
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
            // 依次从左到右进行符号计算
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

                // 处理strlen/strcmp等字符函数的约束 add by yaochi 20141020
                MethodSet ms = expdata.currentvex.getMethodSet();
                if (ms != null && ms.getFunctionList().size() != 0) {
                    MethodNameDeclaration mnd = null;
                    FunctionVariableDomain fvd = null;
                    if (ms.isMethodReturn(leftvd)) {
                        // 是函数返回值
                        mnd = ms.getMethodNameDeclarationByRetVD(leftvd);
                        if (mnd != null && mnd.isLib()) {
                            fvd = (FunctionVariableDomain) ms.getVd(mnd);
                            List<RelationExpression> explist = fvd.getLibFunctionConstraint(leftvd, operator, rightvd);
                            // 处理字符串函数约束集　add by wangyi
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
                        // 是函数返回值
                        mnd = ms.getMethodNameDeclarationByRetVD(rightvd);
                        if (mnd != null && mnd.isLib()) {
                            fvd = (FunctionVariableDomain) ms.getVd(mnd);
                            List<RelationExpression> explist = fvd.getLibFunctionConstraint(rightvd, operator, rightvd);
                            // 处理字符串函数约束集　add by wangyi
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
                                                   // 针对变量声明包含位运算时，添加的辅助变量的约束丢失的情况
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
                    // 添加等式的约束　add by wangyi
                    RelationExpression re =
                            new RelationExpression(((PrimitiveVariableDomain) leftvd).getExpression(), ((PrimitiveVariableDomain) rightvd).getExpression(), operator, expdata.currentvex);
                    expdata.currentvex.addExpaf(re);
                    if (expdata.currentLogicalExpression != null) {
                        expdata.currentLogicalExpression.addLRExpression(re);
                    }
                }
                // end add by xjx 2012-6-29

                // 将整个表达式的结果封装到vd中， by jinkaifeng 2013.5.13
                if (leftvalue.isValueEqual(rightvalue, expdata.currentvex.getSymDomainset())) {// 处理i=1if(i==3)这种类型
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
                    // 通过异常来处理此类情况 PathVexVisitor
                    throw new MyNullPointerException("EqualityExpression Value [0,1] in(.i) " + node.getBeginFileLine());
                }
            }
        } catch (MyNullPointerException e) { // 这是搞毛线
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
        // zys:2010.8.11 在条件判断节点，如果超过了语法树深度上限，则不再递归访问其子结点
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
        // 在ASTPostfixExpression中统一处理
        return super.visit(node, data);
    }

    public Object visit(ASTInclusiveORExpression node, Object data) {
        return dealBinaryBitOperation(node, data, "|");
    }

    public Object visit(ASTLogicalANDExpression node, Object data) {
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        // 初始化LogicalExpression
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

        // changed by jinkaifeng 增加了对抽象内存模型的支持 2012.11.7
        try {
            // 从左到右进行符号计算
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
                    // zys:2010.8.9 根据&&的短路特性，如果左表达式值为0,则不再计算右侧表达式的值
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
                        } else if (d1.isCanonical() && d1.getMin() == 0) {// d1.isCanonical的意义在于，如果是一个确定的数，表示表达式的逻辑取值为确定的真假
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
                    // 短路特性，如果有一个为空，则不计算后面
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
                    // 根据&&的短路特性，如果左表达式值为0,则不再计算右侧表达式的值
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
        expdata.currentvex.addOperator("&&");// 逻辑与 add by wangyi
        return data;
    }

    public Object visit(ASTLogicalORExpression node, Object data) {
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        // 初始化LogicalExpression add by wangyi
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
            // 根据表达式的左值，依次从左到右进行符号计算，并根据表达式的符号进行计算
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

                // changed by jinkaifeng 增加了对抽象内存模型的支持 2012.11.7
                if (leftVD instanceof PrimitiveVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                    if (leftvalue == null || rightvalue == null) {
                        throw new MyNullPointerException("LogicalORExpression Value NULL in(.i) " + node.getBeginFileLine());
                    }
                    // zys:2010.8.9 根据||的短路特性，如果左表达式值为1,则不再计算右侧表达式的值
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
        expdata.currentvex.addOperator("||");// 逻辑或 add by wangyi
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
            // 从左到右进行符号计算
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
                    // 2010.12.03 liuli:当expression得长度过长时，会导致计算陷入死循环
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
                            // zhangxuzhou 2012-9-14发现bug 注释上一句
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
                        // add by jinkaifeng 2012.10.17 屏蔽除数等于0的情况
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

    public Object visit(ASTPostfixExpression node, Object data) {// 在这个方法中，需要变量被所对应的符号表达式，而不是仅仅新生成一个符号
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        ASTPrimaryExpression primary = (ASTPrimaryExpression) node.jjtGetChild(0);
        expdata.vd = null;// add by yaochi 先清空
        primary.jjtAccept(this, data);// AST这里可以直接f6
        ArrayList<Boolean> flags = node.getFlags();
        ArrayList<String> operators = node.getOperatorType();
        Expression currentvalue = expdata.value;
        VariableDomain currentVD = expdata.vd; // add by tangrong 2012-02-17
        CType currenttype = primary.getType();
        int j = 1;
        for (int i = 0; i < flags.size(); i++) {// 当i=1的时候是第二维的数组/结构 以此类推
            if (currentVD != null && currentVD.getImage().contains("const")) {
                break;
            }
            boolean flag = flags.get(i);
            String operator = operators.get(i);
            if (operator.equals("[")) {// 数组
                Expression subindex = null;
                ASTExpression expression = null;
                if (flag) {
                    expression = (ASTExpression) node.jjtGetChild(j++);
                    expression.jjtAccept(this, data);// 得到下标
                    subindex = ((PrimitiveVariableDomain) ((ExpressionVistorData) data).vd).getExpression();
                } else {
                    throw new RuntimeException("ASTPostfixExpression error!");
                }
                // zys:2010.9.13 华为测试出错，暂时屏蔽下
                if (currenttype == null) {
                    logger.error(primary.getBeginFileLine() + "行的类型分析错误");
                    throw new RuntimeException(primary.getBeginFileLine() + "行的类型分析错误");
                }
                CType atype = currenttype.getSimpleType();
                if (atype instanceof CType_AbstPointer) {// 数组或指针的类型

                    CType_AbstPointer ptype = (CType_AbstPointer) atype;
                    currenttype = ptype.getOriginaltype();

                    // add by tangrong 2012-7-6
                    /*
                     * 指针的处理分5步，以p[i]为例 1. 获取指针p的指向域的抽象内存m0。 2.
                     * 在控制流图节点VexNode里查找ArrayVaribaleDomain里有没有哪个数组
                     * （连续抽象内存空间）中含有m0。如果存在返回此数组抽象内存的。 3.
                     * 如果没有，新建一匿名数组（连续抽象内存空间），
                     * m0为数组中的一成员，它在数组中的地址（下标）为新建符号s，s的约束时大约等于0； 4.
                     * 返回p的指向域m0在连续抽象内存空间中的下标
                     * ，查找连续抽象内存中是否存在（下标+1)的元素，存在则返回，不存在则新建一块内存单元m1
                     * ，将<下标+1，m1>存入连续抽象内存中。 5. 执行p=p+1的操作。
                     */// 声明一个 数组型抽象内存模型 memoryBlock
                    ArrayVariableDomain memoryBlock = null;
                    if (currentVD instanceof PointerVariableDomain) {// 如果当前抽象内存模型为指针类型抽象内存模型
                        PointerVariableDomain currVD = (PointerVariableDomain) currentVD;
                        // 步骤1:(*pt)不为NULL，获取(*pt)所在的抽象内存
                        if (currVD.getPointerTo() == null) { // currVD没有指向某一个抽象内存地址则初始化currVD
                            currVD.initMemberVD();
                        }
                        currVD.setStateNotNull();
                        VariableDomain pt = currVD.getPointerTo();

                        // 步骤2 查找是否有连续的内存地址中含有pt。
                        ValueSet vset = expdata.currentvex.getValueSet();
                        memoryBlock = vset.getMemoryBlockContainVD(pt);
                        /*
                         * if(pt instanceof ArrayVariableDomain){ memoryBlock =
                         * (ArrayVariableDomain)pt; }暂时屏蔽byyaochi 20130901
                         */

                        // 步骤3 如果没有，新建一匿名数组（连续抽象内存空间）SymbolDomainSet
                        if (memoryBlock == null) {// 声明一个变量
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
                            // CType_Array(pt.getType()));//填充变量的类型
                            // modify by yaochi 20130423
                            if (i + 1 < flags.size()) {// 确保有下一个符号
                                String nextoper = operators.get(i + 1);// 得到下一个符号
                                if (nextoper.equals("[")) {// 如果下一个符号为数组类型，则将值转换为数组类型
                                    vnd4MemoryBlock.setType(new CType_Array(new CType_Array(CType.getNextType(pt.getType()))));
                                } else {// 否则还是原来类型
                                    vnd4MemoryBlock.setType(new CType_Array(currenttype));
                                }
                            } else {// 没有下一个符号也是赋原来的类型
                                vnd4MemoryBlock.setType(new CType_Array(currenttype));
                            }
                            // modify by yaochi end
                            memoryBlock = new ArrayVariableDomain(vnd4MemoryBlock, currVD.getVariableSource(), expdata.currentvex);// 声明一个新的数组类型抽象内存空间并赋给memoryBlock
                            SymbolFactor factor = SymbolFactor.genSymbol(CType_BaseType.intType);
                            expdata.currentvex.addSymbolDomain(factor, new IntegerDomain(0, 0));
                            memoryBlock.addMember(new Expression(factor), pt);
                            if (vset.getValue((VariableNameDeclaration) currVD.getNd()) != null && (CType.getOrignType(primary.getType()) instanceof CType_Struct)
                                    || primary.getType() instanceof CType_Struct) {
                                // add by yaochi
                                // 如果vset中没有其指针才加入vest中，同时他为typedef类型否则直接改变p指向，结构体类型应该类似，
                                ((PointerVariableDomain) currentVD).setPointTo(memoryBlock);
                                currentVD = memoryBlock;
                            } else {
                                expdata.currentvex.addValue(vnd4MemoryBlock, memoryBlock);
                                memoryBlock = vset.getMemoryBlockContainVD(pt);
                                ((PointerVariableDomain) currentVD).setPointTo(memoryBlock);
                            }
                        } else {// 处理(a+i)[j]这种情况时，下标应该变成i+j add by jinkaifeng
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
                         * //currentVD为指针，后续有[]操作，说明请不应指向一个整型，应调整期指向 add by
                         * yaochi 20130608 ((PointerVariableDomain)
                         * currentVD).setPointTo(memoryBlock); } }
                         */

                    } else if (currentVD instanceof ArrayVariableDomain) {
                        memoryBlock = (ArrayVariableDomain) currentVD;

                    }
                    // 步骤4 处理p[i]
                    if (Config.Field) {
                        expdata.vd = memoryBlock.getMember(subindex, expdata.currentvex.getSymDomainset());

                        if (memoryBlock.getMembers().containsKey(subindex)) {
                            expdata.vd = memoryBlock.getMembers().get(subindex);
                        } else {
                            // tangrong 2012-9-20日修改，之前的方法只有else部分 为
                            // p[i]生成variableDomain
                            VariableNameDeclaration vnd;// 这里，生成数组指针的image有点小问题，不知道需不需要改
                                                        // yaochi
                            String subindexString = subindex.getVND();
                            String image = memoryBlock.getVariableNameDeclaration().getName().replaceAll("[\\[\\]*]", "_") + "[" + subindexString + "]";// 这一句正则表达式，对于多维数组，不正确
                                                                                                                                                        // 例如
                                                                                                                                                        // x[0][0]
                                                                                                                                                        // 得到了
                                                                                                                                                        // x_0_[0]
                            // String image = "(" +
                            // memoryBlock.getVariableNameDeclaration().getName()+")"+"["
                            // + subindex + "]";
                            Scope scope = node.getScope();
                            NameDeclaration decl = Search.searchInVariableAndMethodUpward(image, scope);
                            if (decl == null) {// 防止對於多維數組生成的image格式不對 這裡進行臨時的修改
                                image = memoryBlock.getVariableNameDeclaration().getName() + "[" + subindexString + "]";
                                decl = Search.searchInVariableAndMethodUpward(image, scope);
                            }

                            if (decl instanceof VariableNameDeclaration) {
                                vnd = (VariableNameDeclaration) decl;
                                vnd.setParent(memoryBlock.getVariableNameDeclaration());
                                vnd = (VariableNameDeclaration) decl;
                            } else {// decl 有可能是null 当下标是变量表达式时
                                String tmp = node.getImage();
                                node.setImage(image);
                                vnd = new VariableNameDeclaration(node, subindex);// 2013-5-22
                                                                                  // 解决simpleImage的问题
                                node.setImage(tmp);
                                // add by yaochi 20130423
                                if (i + 1 < flags.size()) {// 确保有下一个符号
                                    String nextoper = operators.get(i + 1);// 得到下一个符号
                                    if (nextoper.equals("[")) {// 如果下一个符号为数组类型，则将值转换为数组类型
                                        vnd.setType(new CType_Array(CType.getNextType(currenttype)));// add
                                                                                                     // by
                                                                                                     // yaochi
                                    } else {// 否则还是原来类型
                                        vnd.setType(currenttype);
                                    }
                                } else {// 没有下一个符号也是赋原来的类型
                                    vnd.setType(currenttype);
                                }
                                // add by yaochi end
                            }
                            vnd.setParent(memoryBlock.getVariableNameDeclaration());
                            // 申明了一个抽象内存模型 memvd，里面存放变量信息
                            vnd.setExpSimpleImage(subindex);// by zxz 2013-5-27
                            VariableDomain memvd = VariableDomain.newInstance(vnd, memoryBlock.getVariableSource().next(), expdata.currentvex);
                            memoryBlock.addMember(subindex, memvd);
                            // add by jinkaifeng 2013.5.21
                            // 将数组下标大于0的约束加入到symbolDomainSet中
                            Domain subdomain = subindex.getDomain(expdata.currentvex.getLastsymboldomainset());
                            if (subdomain != null && !subdomain.isConcrete()) {// 让非常数的下标进行计算
                                // Expression indexExp = new
                                ExpressionDomain.getExpressionDomain(subindex, new Expression(0), ">=", expdata.currentvex);
                                long arrayDimSize = ((ArrayVariableDomain) memoryBlock).getDimSize();
                                if (arrayDimSize != -1) {// 这里如果数组的这一维是定长的，应该给出一个定长的限制出来zxz
                                    ExpressionDomain.getExpressionDomain(subindex, new Expression(arrayDimSize), "<", expdata.currentvex);
                                }
                            }
                            // add end
                            expdata.vd = memvd;
                            currenttype = memvd.getType(); // add by tangrong
                                                           // 2012-9-20
                        }

                        // added 2012-12-17 如果i在之前出现过 例如 i =0 ; a[i] = A; 此时应该
                        // 对a[0]进行操作zhangxuzhou
                        // 目前只对一维数组有效
                        // 2013-5-24 保留新增的 a[i] 如果之前有a[0] 则把两者的区间保持一致
                        // 对于出现了a[i]的地方，都要对于区间进行对应下标的审查
                        Domain currentIndexDomain = subindex.getDomain(expdata.currentvex.getSymDomainset());
                        if (currentVD instanceof PrimitiveVariableDomain) {// 测试多层数组，临时屏蔽
                                                                           // yaochi
                            if (currentIndexDomain != null) {// 若是一个固定值，则用此值做下标
                                if (currentIndexDomain.isConcrete()) {
                                    Expression Consubindex = new Expression((currentIndexDomain.getConcreteDomain().longValue()));
                                    VariableDomain ConVd = memoryBlock.getMember(Consubindex, expdata.currentvex.getSymDomainset());
                                    VariableDomain VarVd = expdata.vd;
                                    if (ConVd != null) { // 将区间相交并且保持一致
                                        Domain ConMem = ((PrimitiveVariableDomain) ConVd).getExpression().getDomain(expdata.currentvex.getSymDomainset());
                                        Domain VarMem = ((PrimitiveVariableDomain) VarVd).getExpression().getDomain(expdata.currentvex.getSymDomainset());
                                        Domain mergeMem = Domain.intersect(ConMem, VarMem, VarVd.getType());
                                        SymbolFactor VarExp = (SymbolFactor) ((PrimitiveVariableDomain) VarVd).getExpression().getSingleFactor();
                                        SymbolFactor ConExp = (SymbolFactor) ((PrimitiveVariableDomain) ConVd).getExpression().getSingleFactor();

                                        // 2013-5-31 bug 整个表达式是 77+mynode.a[1]
                                        // 得到的mergeMem就是表达式的区间，
                                        // 表达式整体的区间在symbolSet中没法表示出来
                                        if (VarExp != null && ConExp != null) {
                                            expdata.currentvex.getSymDomainset().addDomain(VarExp, mergeMem);
                                            expdata.currentvex.getSymDomainset().addDomain(ConExp, mergeMem);
                                        }

                                        if (mergeMem == Domain.getEmptyDomainFromType(VarVd.getType())) {

                                        }
                                    }
                                } else if (false) { // 如果数组本身的定长的，未定
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
            } else if (operator.equals("(")) { // 函数指针的后括号处理在这里哦
                ASTArgumentExpressionList expressionlist = null;
                // add by zhouhb
                // 对指针相关函数的处理
                // 增减了模糊处理
                // add by zhouhb 2010/10/19
                // if(node.getImage().contains("malloc")||node.getImage().contains("Malloc")||node.getImage().contains("malloc"))
                if (node.getImage().equals("malloc") || node.getImage().equals("calloc")) {
                    // 增加了对malloc参数中包含函数参数引用的判断，对传入的未知参数不予处理
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
                    // 处理malloc(10)时无类型信息，故修改
                    // modified by zhouhb 2010/7/19
                    long mallocsize;
                    IntegerDomain size = IntegerDomain.castToIntegerDomain(expdata.value.getDomain(expdata.currentvex.getSymDomainset()));
                    // 如果malloc分配空间以参数传递进来，默认分配无穷大
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
                    // 为抽象内存模拟malloc行为，生成的指针只有notnull这么一个信息
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
                    // 结构体成员的free不予以处理
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
                    // 如果释放对象指针有运算，即ve不为SingleFactor(eg.1+S)，则释放不成功
                    if (temp == null) {
                        return data;
                    } else {
                        expdata.currentvex.addSymbolDomain(temp, p);
                        expdata.value = new Expression(temp);
                        return data;
                    }
                }
                if (flag || currentVD instanceof FunctionVariableDomain) {// 函数有没有实参的标志位
                    MethodSet ms = expdata.currentvex.getMethodSet();
                    if (!flag) {
                        FunctionVariableDomain currentTmp = (FunctionVariableDomain) currentVD;
                        currentTmp.addCalledIndex();
                        currentTmp.beCalled();
                        ms.addFuncVD(currentTmp);
                    }
                    if (flag && currentVD != null) {
                        expressionlist = (ASTArgumentExpressionList) node.jjtGetChild(j++); // 函数的参数在这里拿到
                        // expressionlist.jjtAccept(this, data);

                        while (currentVD != null && currentVD instanceof PointerVariableDomain) {// 指针的性质决定的
                            VariableDomain childvd = ((PointerVariableDomain) currentVD).getPointerTo();
                            if (childvd == null) {
                                MethodNameDeclaration mnd = new MethodNameDeclaration(currentVD.getVariableNameDeclaration());
                                childvd = VariableDomain.newInstance(mnd, expdata.currentvex);
                            }
                            currentVD = childvd;
                        }

                        FunctionVariableDomain currentTmp = (FunctionVariableDomain) currentVD;

                        if (currentTmp.getNd() instanceof MethodNameDeclaration) {
                            // 收集打桩需要的信息-实参出现的具体位置
                            // 函数类型后期调用次数+1
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
                                        expressionlist.jjtGetChild(childindex).jjtAccept(this, childdata);// 得到参数节点的vd
                                        MethodArgument ma = new MethodArgument(childdata.vd, childindex);
                                        malist.add(ma);
                                    }
                                }
                            } else
                                for (int childindex = 0; childindex < expressionlist.jjtGetNumChildren(); childindex++) {
                                    expressionlist.jjtGetChild(childindex).jjtAccept(this, expdata);// 得到参数节点的vd
                                    /*
                                     * if(!(childdata.vd instanceof
                                     * PrimitiveVariableDomain) &&
                                     * !(childdata.vd instanceof
                                     * FunctionVariableDomain) &&
                                     * childdata.vd.getNd() != null){
                                     */// 类似(char*)0这样的参数不应加入参数表中，加入判断
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
                                ms.addValue(mnd, childfvd);// 生成一个函数
                                ((PointerVariableDomain) retVD).setPointTo(childfvd);
                            }
                        }
                        if (retVD != null) {
                            retVD.setVariableSource(VariableSource.INPUT_ANNOMY);
                            expdata.currentvex.addValue(vnd, retVD);// 加入valueset中
                            SymbolFactor factor = SymbolFactor.genSymbol(retVD.getType());
                            expdata.currentvex.addSymbolDomain(factor, Domain.getFullDomainFromType(retVD.getType()));
                            ((FunctionVariableDomain) currentVD).addActualRetvd(retVD); // 这里设置函数的返回值
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
                        NameDeclaration decl = Search.searchInVariableAndMethodUpward(image1, scope);// node结点对应的是postfix，而postfix对应的image是总的语法树的image，这里需要前一次的
                        if (decl == null) {// add by yaochi容错处理
                            String image2 = "(" + currentVD.getNd().getImage() + ")" + "." + field.getImage();
                            decl = Search.searchInVariableAndMethodUpward(image2, scope);// node结点对应的是postfix，而postfix对应的image是总的语法树的image，这里需要前一次的

                        }
                        if (decl == null && (currenttype instanceof CType_Struct)) {// add
                                                                                    // by
                                                                                    // yaochi
                                                                                    // CType_Struct
                                                                                    // 找结构体的成员，没找到需要新建
                            decl = new VariableNameDeclaration(node.getFileName(), node.getScope(), image1, node, field.getImage());
                            decl.setType(((CType_Struct) currenttype).getCType(field.getImage()));
                            ((VariableNameDeclaration) decl).setParent((VariableNameDeclaration) currentVD.getNd());
                        }
                        if (decl == null && (currenttype instanceof CType_Typedef) && CType.getOrignType(currenttype) instanceof CType_Struct) {// add
                                                                                                                                                // by
                                                                                                                                                // yaochi
                                                                                                                                                // CType_Struct
                                                                                                                                                // 找结构体的成员，没找到需要新建
                            decl = new VariableNameDeclaration(node.getFileName(), node.getScope(), image1, node, field.getImage());
                            CType origtype = CType.getOrignType(currenttype);
                            decl.setType(((CType_Struct) origtype).getCType(field.getImage()));
                            ((VariableNameDeclaration) decl).setParent((VariableNameDeclaration) currentVD.getNd());
                        }// end yaochi 20130531

                        if (decl instanceof VariableNameDeclaration) {
                            // 变量处理
                            VariableNameDeclaration v = (VariableNameDeclaration) decl;
                            StructVariableDomain svd = (StructVariableDomain) expdata.currentvex.getVariableDomain((VariableNameDeclaration) currentVD.getNd());// add
                                                                                                                                                                // by
                                                                                                                                                                // yaochi
                                                                                                                                                                // 20130529
                            currentvalue = expdata.currentvex.getValue(v);
                            currentVD = expdata.currentvex.getVariableDomain(v);
                            if (currentVD == null) {
                                // add by tangrong 2012-5-7 为结构体添加成员。
                                // StructVariableDomain svd =
                                // (StructVariableDomain)
                                // expdata.currentvex.getVariableDomain(v.getParent());
                                String simpleImage = v.getSimpleImage();
                                if (svd != null) {
                                    if (svd.getMembers().containsKey(simpleImage)) {
                                        currentVD = svd.getMembers().get(simpleImage);// add by
                                                                                      // jinkaifeng
                                                                                      // 2013.5.15如果结构体里已经有这个成员，不用再生成新的vd
                                    } else {
                                        currentVD = VariableDomain.newInstance(v, svd.getVariableSource().next(), expdata.currentvex);
                                        svd.addMember(v.getSimpleImage(), currentVD);
                                    }
                                } else {

                                }
                            }

                        }
                        // 存在成员已存在于结构体中但找不到decl的情况 add by yaochi 2013-09-04
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
                            // 结构体中包含指向另一结构体的指针时(Struct.otherStruct.data)，可能存在field.image不完整问题需要补全
                            // add by yaochi 130617
                            decl = Search.searchInVariableAndMethodUpward(node.getImage(), scope);
                        }
                        if (decl instanceof VariableNameDeclaration) {
                            // 变量处理
                            VariableNameDeclaration v = (VariableNameDeclaration) decl;
                            PointerVariableDomain tmpVD = (PointerVariableDomain) currentVD;// 进入"->"中的都为指针类型VD
                            currentvalue = expdata.currentvex.getValue(v);
                            // add by tangrong 2012-02-17
                            currentVD = expdata.currentvex.getVariableDomain(v);
                            if (currentVD == null) { // 还没有为该指针分配抽象内存
                                VariableNameDeclaration pvnd = v;
                                VariableDomain parentVD = null;
                                /*
                                 * while( parentVD == null ){ pvnd =
                                 * pvnd.getParent(); if(pvnd == null) break;
                                 * parentVD =
                                 * expdata.currentvex.getVariableDomain(pvnd); }
                                 */
                                if (parentVD == null) {// modify by yaochi
                                                       // 使用pvnd的得到的在结构体类型时存在问题，故先屏蔽，改用ND查找父节点
                                    pvnd = (VariableNameDeclaration) tmpVD.getNd();// tmpVD为pvnd的父节点
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
                                                                                             // 解决simpleImage的问题
                                                node.setImage(tmp);
                                                vnd.setType(CType.getNextType(currenttype));
                                                vnd.setParent(avd.getVariableNameDeclaration());
                                            }
                                            // 申明了一个抽象内存模型 memvd，里面存放变量信息
                                            VariableDomain memvd = VariableDomain.newInstance(vnd, avd.getVariableSource().next(), expdata.currentvex);
                                            vnd.setExpSimpleImage(exp);// by zxz
                                                                       // 2013-5-27
                                            avd.addMember(exp, memvd);
                                            svd = (StructVariableDomain) memvd;
                                        }
                                    }
                                    VariableDomain vdmem = null;
                                    if (svd != null)
                                        vdmem = svd.getMember(v.getSimpleImage());// vdmem得到struct.element
                                    if (vdmem != null) {
                                        currentVD = vdmem;
                                    } else {
                                        // end add by xujiaoxian 2012-10-18
                                        vdmem = VariableDomain.newInstance(v, parentVD.getVariableSource().next(), expdata.currentvex);
                                        currentVD = VariableDomain.newInstance(v, parentVD.getVariableSource().next(), expdata.currentvex);
                                        ((PointerVariableDomain) parentVD).addArrowMember(v.getSimpleImage(), currentVD);// 增加->标示符指向的chengy
                                        VariableDomain tmpvd = currentVD;
                                        while (tmpvd instanceof PointerVariableDomain) {// add
                                                                                        // by
                                                                                        // yaochi
                                                                                        // 130625
                                                                                        // 解决node->left->left.element总是赋*void(0)问题
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
            } else if (operator.equals("++")) { // 后++ data++ p = null
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
                     * origVD.setExpression(currentvalue); //因为是后++，应该返回+1前的那个值
                     * modified by tangorng 2012-11-1
                     */// modify by jinkaifeng 这段应该移到sideeffect=false
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
                    origVD.setExpression(currentvalue); // 因为是后++，应该返回+1前的那个值
                                                        // modified by tangorng
                                                        // 2012-11-1
                    currentVD = origVD;
                }
                // add by tangrong 2012-7-3 基于抽象内存模型支持数值类型的++操作和指针的++操作。
                /*
                 * 指针的处理分5步，以p++为例 1. 获取指针p的指向域的抽象内存m0。 2.
                 * 在控制流图节点VexNode里查找ArrayVaribaleDomain里有没有哪个数组
                 * （连续抽象内存空间）中含有m0。如果存在返回此数组抽象内存的。 3.
                 * 如果没有，新建一匿名数组（连续抽象内存空间），m0为数组中的一成员
                 * ，它在数组中的地址（下标）为新建符号s，s的约束时大约等于0； 4.
                 * 返回p的指向域m0在连续抽象内存空间中的下标，查找连续抽象内存中是否存在
                 * （下标+1)的元素，存在则返回，不存在则新建一块内存单元m1，将<下标+1，m1>存入连续抽象内存中。 5.
                 * 执行p=p+1的操作。
                 */
                if (currentVD instanceof PointerVariableDomain) {

                    // 步骤1:(*p)不为NULL，获取(*p)所在的抽象内存
                    PointerVariableDomain varDomain = (PointerVariableDomain) currentVD;
                    if (varDomain.getPointerTo() == null) {
                        varDomain.initMemberVD();
                    }
                    varDomain.setStateNotNull();
                    VariableDomain pt = varDomain.getPointerTo();

                    // 步骤2 查找是否有连续的内存地址中含有pt。
                    ValueSet vset = expdata.currentvex.getValueSet();
                    ArrayVariableDomain memoryBlock = vset.getMemoryBlockContainVD(pt);

                    // 步骤3 如果没有，新建一匿名数组（连续抽象内存空间）
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

                    // 步骤4 地址偏移1：addr+1,查找是否存在下标为addr+1对应的元素，存在则返回，不存在新建。
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

                    // 步骤5 执行p=p+1的操作
                    if (ptPlusOne instanceof PointerVariableDomain) {// add by
                                                                     // yaochi
                                                                     // pt++后
                                                                     // 设置其parent为current的parent2013-09-03
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
                    origVD.setExpression(currentvalue); // 因为是后++，应该返回+1前的那个值
                                                        // modified by tangorng
                                                        // 2012-11-1
                    currentVD = origVD;
                }
                // end yaochi
                // add by tangrong 2012-7-5
                /*
                 * 指针的处理分5步，以p--为例 1. 获取指针p的指向域的抽象内存m0。 2.
                 * 在控制流图节点VexNode里查找ArrayVaribaleDomain里有没有哪个数组
                 * （连续抽象内存空间）中含有m0。如果存在返回此数组抽象内存的。 3.
                 * 如果没有，新建一匿名数组（连续抽象内存空间），m0为数组中的一成员
                 * ，它在数组中的地址（下标）为新建符号s，s的约束时大约等于0； 4.
                 * 返回p的指向域m0在连续抽象内存空间中的下标，查找连续抽象内存中是否存在
                 * （下标+1)的元素，存在则返回，不存在则新建一块内存单元m1，将<下标+1，m1>存入连续抽象内存中。 5.
                 * 执行p=p-1的操作。
                 */
                if (currentVD instanceof PointerVariableDomain) {
                    // 步骤1:(*p)不为NULL，获取(*p)所在的抽象内存
                    PointerVariableDomain varDomain = (PointerVariableDomain) currentVD;
                    if (varDomain.getPointerTo() == null) {
                        varDomain.initMemberVD();
                    }
                    varDomain.setStateNotNull();
                    VariableDomain pt = varDomain.getPointerTo();

                    // 步骤2 查找是否有连续的内存地址中含有pt。
                    ValueSet vset = expdata.currentvex.getValueSet();
                    ArrayVariableDomain memoryBlock = vset.getMemoryBlockContainVD(pt);

                    // 步骤3 如果没有，新建一匿名数组（连续抽象内存空间）
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

                    // 步骤4 地址偏移1：addr-1,查找是否存在下标为addr-1对应的元素，存在则返回，不存在新建。
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

                    // 步骤5 执行p=p-1的操作
                    if (ptSubOne instanceof PointerVariableDomain) {
                        // add by yaochi pt++后
                        // 设置其parent为current的parent2013-09-03
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
        // 处理p=f(a,b)时，会为f,a,b分别生成三个符号F,A,B，最终会生成一个新的符号P，但并未把F对应的区间与P关联
        // 忘了当时为什么这么改，暂且保留
        // if(primary.jjtGetNumChildren()==0&&primary.getType() instanceofo
        // CType_Function){
        // currentvalue=func;
        // }

        expdata.vd = currentVD;// 当前抽象内存模型
        if (expdata.vd instanceof PrimitiveVariableDomain) {
            expdata.value = ((PrimitiveVariableDomain) currentVD).getExpression();
        } else {
            expdata.value = currentvalue;// 当前变量符号表达式
        }
        // expdata.value=currentvalue;
        // expdata.vd = currentVD;//又多出来重复的代码。。。2013-5-13zxz

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

                // 变量处理
                VariableNameDeclaration v = (VariableNameDeclaration) decl;
                expdata.value = expdata.currentvex.getValue(v);
                // add by yaochi
                VariableDomain vd = expdata.currentvex.getVariableDomain(v);

                if (vd != null) {
                    VariableDomain expvd = expdata.vd;
                    expdata.vd = vd;
                    // add by baiyu 2014.12.24 针对变量声明包含位运算时，添加的辅助变量的约束丢失的情况
                    HashMap<Expression, IntegerDomain> temp = vd.helpDomainForVD;
                    if (temp != null) {
                        expdata.Help = temp;
                        // expdata.Help.put(expdata.value, temp);
                    }
                    if (expvd != null && expvd.getType().equals(vd.getType())) {
                        // 解决指针指向新地址后，vd得到的仍然是之前地址问题 add by yaochi 2013-09-03
                        if (((VariableNameDeclaration) expvd.getNd()).getAncestor().equals(((VariableNameDeclaration) vd.getNd()).getAncestor())) {
                            expdata.vd = expvd;
                        }
                    }
                }
                // end
                // expdata.vd = expdata.currentvex.getVariableDomain(v); // add
                // by tangrong 2011-12-16
                // 不处理结构体的相关信息
                // add by zhouhb 2010/8/19
                if (!Config.Field) {
                    if (v.getType() instanceof CType_Array && ((CType_Array) v.getType()).getOriginaltype() instanceof CType_Struct || v.getType() instanceof CType_Struct)
                        return data;
                }
                // add by tangrong 2011-12-15 临时添加在这里， 之后会根据输入域参数or局部变量分情况添加
                if (expdata.vd == null) {
                    VariableSource varSource = (v.getScope() instanceof SourceFileScope || v.getScope() instanceof MethodScope) ? VariableSource.INPUT : VariableSource.LOCAL;
                    expdata.vd = VariableDomain.newInstance(v, varSource, expdata.currentvex);
                    if (expdata.vd.getNd() != null && expdata.vd.getNd().getNode() instanceof ASTEnumerator) {
                        // Do nothing，变量是枚举的成员时不加入VS中
                    } else {
                        expdata.currentvex.addVariableDomain(expdata.vd);
                    }
                }
                // add end tangrong

                if (expdata.value == null) {
                    // 全局变量初值处理 如果有全局变量，且初始值在声明时给出，则加入到当前结点中 ssj
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
                    // //处理形如f(a,b)中参数的初始化问题
                    // //remodified by zhouhb 2010/8/6
                    // //参数无需初始化
                    // // if(v.getType() instanceof
                    // CType_Pointer&&node.jjtGetNumChildren()==0){
                    // // PointerDomain p=new PointerDomain();
                    // // expdata.currentvex.addSymbolDomain(sym, p);
                    // // }
                    // expdata.value=new Expression(sym);
                    // }

                    // 不用判断副作用
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
                                            // Do nothing，变量是枚举的成员时不加入VS中
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

                        // expdata.currentvex.addValue(v, expdata.value); 注释by
                        // tangrong 2012-02-21
                    }
                }
            } else if (decl instanceof MethodNameDeclaration) {
                MethodNameDeclaration mnd = (MethodNameDeclaration) decl;
                Method method = null;
                MethodSummary ms = null;

                // add by tangrong 2012-5-10 做最粗糙的处理，给函数返回值初始化最全的取值域。
                if (mnd != null && expdata.vd == null) {
                    MethodSet methodset = expdata.currentvex.getMethodSet();
                    VariableDomain vd = methodset.getMap().get(mnd);
                    expdata.vd = vd;
                    if (expdata.vd == null) {
                        expdata.vd = VariableDomain.newInstance(mnd, expdata.currentvex);
                    }
                    // if (/*!mnd.isLib( ) ||*/ mnd.isIOLib( )) {//加到methodSet中
                    // by jinkaifeng 2013.5.3
                    expdata.currentvex.addValue(mnd, expdata.vd);
                    // }
                }
                // 库函数返回值处理 ssj 貌似是生成摘要
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
                        // 是库函数，但是没有摘要，则直接生成一个符号，其返回值区间未知
                        expdata.value = new Expression(SymbolFactor.genSymbol(mnd.getType(), mnd.getImage()));
                    }
                } else {
                    // chh 计算表达式中遇到函数调用时，对可以计算出函数返回值的函数，获取其返回值计算
                    if (mnd != null)
                        method = mnd.getMethod();
                    if (method != null && method.getReturnDomain() != null) {
                        // 整型域
                        if ((method.getReturnDomain().getDomaintype() == DomainType.INTEGER)
                                && (((IntegerDomain) method.getReturnDomain()).getMax() == ((IntegerDomain) method.getReturnDomain()).getMin()))
                            expdata.value = new Expression(((IntegerDomain) method.getReturnDomain()).getMax());
                        // 浮点域
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
                    } else {// 其他暂不处理
                        // zys:如果是指针类型的赋值，则添加其默认赋值为NULLORNOTNULL
                        CType type = node.getType();
                        CType pointer = null;
                        if (type != null && type instanceof CType_Function) {
                            pointer = ((CType_Function) type).getReturntype();
                            // 如果函数返回值类型为版本暂时处理不了类型，则不予处理，减少漏报
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
                    // chh 计算表达式中遇到函数调用时，对可以计算出函数返回值的函数，获取其返回值计算 end

                    // 函数处理 针对全局变量后置条件处理 ssj
                    if (mnd != null)
                        ms = mnd.getMethodSummary();
                    if (ms != null && !ms.getPostConditions().isEmpty()) {
                        Set<MethodFeature> mtdpostcond = ms.getPostConditions();
                        for (MethodFeature mtdfea : mtdpostcond) {
                            if (mtdfea instanceof MethodPostCondition) {
                                MethodPostCondition mtdpost = (MethodPostCondition) mtdfea;
                                Map<Variable, Domain> msvariables = mtdpost.getPostConds();
                                for (Variable msvariable : msvariables.keySet()) {// zys:为后置条件中的变量生成新的符号表达式
                                    VariableNameDeclaration v = (VariableNameDeclaration) Search.searchInVariableUpward(msvariable.getName(), scope);
                                    /**
                                     * zys: 函数摘要生成的疑问？ 1、 假定有file1 . c与file2 .
                                     * c两个源文件都 # include "file3.c" ， 且file3 .
                                     * c中仅有一个带有条件编译选项的函数 2、 如果file1 .c先分析 ，
                                     * 则由于条件编译的不同 ， 生成了file1 . c中该函数的摘要信息 3、
                                     * 当file2 . c分析时遇到该函数调用 ， 则首先获取之前生成的摘要 ，
                                     * 再按照本文件中的条件编译选项添加新的摘要 4、
                                     * 以上分析的结果就导致摘要信息在单个文件中找不到
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
        // 还是处理大于小于这种关系的
        ExpressionVistorData expdata = (ExpressionVistorData) data;
        node.jjtGetChild(0).jjtAccept(this, expdata);
        Expression leftvalue = expdata.value;
        VariableDomain leftVD = expdata.vd;// add by jinkaifeng
        Expression rightvalue = null;
        VariableDomain rightVD = null;// add by jinkaifeng
        DoubleDomain d1 = null;
        DoubleDomain d2 = null;
        try {
            // 从左到右进行符号计算 ExpressionValueVisitor
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

                // 处理strlen等字符函数的约束 add by yaochi 20141020
                MethodSet ms = expdata.currentvex.getMethodSet();
                if (ms != null && ms.getFunctionList().size() != 0) {
                    MethodNameDeclaration mnd = null;
                    FunctionVariableDomain fvd = null;
                    if (ms.isMethodReturn(leftVD)) {
                        // 是函数返回值
                        mnd = ms.getMethodNameDeclarationByRetVD(leftVD);
                        if (mnd != null && mnd.isLib()) {
                            fvd = (FunctionVariableDomain) ms.getVd(mnd);
                            List<RelationExpression> explist = fvd.getLibFunctionConstraint(leftVD, operator, rightVD);
                            // 处理字符串函数约束集　add by wangyi
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
                        // 是函数返回值
                        mnd = ms.getMethodNameDeclarationByRetVD(rightVD);
                        if (mnd != null && mnd.isLib()) {
                            fvd = (FunctionVariableDomain) ms.getVd(mnd);
                            List<RelationExpression> explist = fvd.getLibFunctionConstraint(rightVD, operator, leftVD);
                            // 处理字符串函数约束集　add by wangyi
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

                // add by yaochi 对于*a[][]类型的处理
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
                for (int j = 0; j < expdata.origexplist.size(); j++) {// 右边的值有问题，当operator为<的时候，数组指针
                    expdata.currentvex.addMultiExp(expdata.origexplist.get(j));
                    // logger.debug("ASTRelationalExpression中复杂运算表达式："+expdata.origexplist.get(j).toString());
                }
                expdata.origexplist.clear();

                // 添加约束　add by wangyi
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

                if (operator.equals(">")) {// 此处为must的逻辑
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
            // super.visit(node,expdata); 注释by tangrong 2012-10-30
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

    // unary-expression一元表达式
    public Object visit(ASTUnaryExpression node, Object data) {

        ExpressionVistorData expdata = (ExpressionVistorData) data;

        if (node.jjtGetChild(0) instanceof ASTPostfixExpression) {
            node.jjtGetChild(0).jjtAccept(this, expdata);// F5。得到子节点的抽象内存模型，expdata.value已经不需要用了主要是vd

            // //以下是提取表达式 需要确保这个unaryExpression 不是一个数组的下标、
            // ArrayList<Boolean>
            // flags=((ASTPostfixExpression)node.jjtGetChild(0)).getFlags();
            // if(flags==null)return data;
            // boolean flag=flags.get(0);
            // add by xjx 2012-6-29
            if (node.jjtGetChild(0).jjtGetNumChildren() > 1)// 避免变量名和库函数重名
            {
                if (((node.jjtGetParent() instanceof ASTAssignmentExpression) // 处理if(isalnum(x))以及if(!isalnum(x))
                        || (node.jjtGetParent() instanceof ASTUnaryExpression) || (node.jjtGetParent() instanceof ASTLogicalANDExpression) || (node.jjtGetParent() instanceof ASTLogicalORExpression))
                        && (node.getParentsOfType(ASTExpressionStatement.class).size() == 0)) {
                    String oper = "";
                    if (node.getImage().equals("isalnum") || node.getImage().equals("isalpha") || node.getImage().equals("iscntrl") || node.getImage().equals("isdigit")
                            || node.getImage().equals("islower") || node.getImage().equals("isgraph") || node.getImage().equals("isprint") || node.getImage().equals("ispunct")
                            || node.getImage().equals("isupper") || node.getImage().equals("isspace")) {
                        if (node.getImage().equals("isalnum")) // 判断是否是数字或字母
                            oper = "isalnum";
                        else if (node.getImage().equals("isalpha")) // 判断是否是字母
                            oper = "isalpha";
                        else if (node.getImage().equals("iscntrl")) // 判断是否为控制字符，ASCII码在0-0x1F之间
                            oper = "iscntrl";
                        else if (node.getImage().equals("isdigit")) // 判断是否为数字
                            oper = "isdigit";
                        else if (node.getImage().equals("islower")) // 判断是否为小写字母
                            oper = "islower";
                        else if (node.getImage().equals("isgraph")) // 判断是否可以打印字符，不包括字符（ox21~ox7E）
                            oper = "isgraph";
                        else if (node.getImage().equals("isprint")) // 判断是否可打印字符包括空格ox21~ox7E
                            oper = "isprint";
                        else if (node.getImage().equals("ispunct")) // 判断是否为标点字符，不包括空格
                            oper = "ispunct";
                        else if (node.getImage().equals("isupper")) // 判断是否为大写字母
                            oper = "isupper";
                        else if (node.getImage().equals("isspace")) // 检查ch是否空格、跳格符（制表符）或换行符
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
                // added zhangxuzhou 2012-12-13 数组型 a[i] 会出错 将i的符号也提取出来 不是当做一个整体

                if ((node.getParentsOfType(ASTPostfixExpression.class).size() != 0)) {

                    List<ASTPostfixExpression> list = node.getParentsOfType(ASTPostfixExpression.class);
                    String operator = list.get(0).getOperators();
                    if (operator.startsWith("["))
                        return data; // 是 数组 a[i]中的i，在上层获取此约束整体
                    // boolean flag = list.get(0).getFlags().get(0);
                    // if(flag)return data; //有[]() . ->等
                }
                // added ended zhangxuzhou 2012-12-13

                // !=0是非条件语句 for while中的i++有问题，有待解决
                if (node.getParentsOfType(ASTExpressionStatement.class).size() == 0
                        && (node.getParentsOfType(ASTIterationStatement.class).size() == 0 || node.jjtGetParent().jjtGetNumChildren() == 1 || node.getParentsOfType(ASTSelectionStatement.class).size() > 0)
                        && node.getParentsOfType(ASTInitDeclarator.class).size() == 0 && node.getParentsOfType(ASTJumpStatement.class).size() == 0) {
                    if (expdata.value != null) {

                        Factor f = expdata.value.getSingleFactor();
                        if (f instanceof SymbolFactor) {
                            for (int i = 0; i < expdata.origexplist.size(); i++) {
                                expdata.currentvex.addMultiExp(expdata.origexplist.get(i));
                                // logger.debug("ASTUnaryExpression中对应的复杂表达式: "+expdata.origexplist.get(i).toString());
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
            } else if ((node.jjtGetParent() instanceof ASTEqualityExpression) || (node.jjtGetParent() instanceof ASTRelationalExpression)) {// 目前只对abs(x)支持ASTRelationalExpression的运算
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
                    IntegerDomain i1 = Domain.castToIntegerDomain(expression.getDomain(expdata.currentvex.getSymDomainset()));// 这是什么作用？求的是x_0的整数区间？
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
                    int flag = 0, flag1 = 0, flag2 = 0; // flag用来标识两个参数是否都为PrimitiveVariableDomain
                    // flag1用来标识两个参数的取值区间是精确值；flag1=0，都是精确值，=1只有一个，>1，一个没有
                    // flag2用来标识第二个参数是否是常数，flag2==1,表示是常数。
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

                            if (i2 instanceof IntegerDomain) // 此处判断取出的变量的域的类型，将其转化成double。是否需要根据变量类型判断？
                                i1 = ((IntegerDomain) i2).integerToDouble();
                            else if (i2 instanceof DoubleDomain)
                                i1 = (DoubleDomain) i2;
                            if (!i1.isConcrete()) {
                                value = expdata.value;
                                vd = expdata.vd;
                                flag1++;
                            }

                        } else if ((factor instanceof NumberFactor) && (i == 1))// 如果参数列表中第二个值是常量则把第一个参数的value和vd赋给expdata,作为要求的参数
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
                        if ((flag1 == 1) || (flag2 == 1)) // 如果都是精确值，应该怎么样处理？
                        // 如果都不是精确的区间，是否需要默认第一个参数为要转换的变量
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
            }// end if, add by xiongwei 2013-4-15在抽象语法树上寻找还有三角函数的表达式，并对表达式进行分析
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
            if (node.getOperatorType().get(0).equals("++")) {// 前++
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
                    // 步骤1:(*p)不为NULL，获取(*p)所在的抽象内存
                    PointerVariableDomain varDomain = (PointerVariableDomain) currentVD;
                    if (varDomain.getPointerTo() == null) {
                        varDomain.initMemberVD();
                    }
                    varDomain.setStateNotNull();
                    VariableDomain pt = varDomain.getPointerTo();

                    // 步骤2 查找是否有连续的内存地址中含有pt。
                    ValueSet vset = expdata.currentvex.getValueSet();
                    ArrayVariableDomain memoryBlock = vset.getMemoryBlockContainVD(pt);

                    // 步骤3 如果没有，新建一匿名数组（连续抽象内存空间）
                    if (p == null) {// add by yaochi 解决*(++a[][])不能得到抽象语法树节点
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

                    // 步骤4 地址偏移1：addr+1,查找是否存在下标为addr+1对应的元素，存在则返回，不存在新建。
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

                    // 步骤5 执行p=p+1的操作
                    if (ptPlusOne instanceof PointerVariableDomain) {// add by
                                                                     // yaochi
                                                                     // pt++后
                                                                     // 设置其parent为current的parent2013-09-03
                        PointerVariableDomain pvd = (PointerVariableDomain) ptPlusOne;
                        if (pvd.getPointerTo() == null)
                            pvd.initMemberVD();
                        ((VariableNameDeclaration) pvd.getPointerTo().getNd()).setParent(currentVD.getVariableNameDeclaration());
                    }
                    ((PointerVariableDomain) currentVD).changePT(ptPlusOne);
                }
            } else if (node.getOperatorType().get(0).equals("--")) {// 前--
                ASTPrimaryExpression p = (ASTPrimaryExpression) ((SimpleNode) node.jjtGetChild(0)).getSingleChildofType(ASTPrimaryExpression.class);

                if (currentVD instanceof PrimitiveVariableDomain) {
                    // expdata.value = expdata.value.sub(new
                    // Expression(1));//expdata.value已经废弃。
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
                    // 步骤1:(*p)不为NULL，获取(*p)所在的抽象内存
                    PointerVariableDomain varDomain = (PointerVariableDomain) currentVD;
                    if (varDomain.getPointerTo() == null) {
                        varDomain.initMemberVD();
                    }
                    varDomain.setStateNotNull();
                    VariableDomain pt = varDomain.getPointerTo();

                    // 步骤2 查找是否有连续的内存地址中含有pt。
                    ValueSet vset = expdata.currentvex.getValueSet();
                    ArrayVariableDomain memoryBlock = vset.getMemoryBlockContainVD(pt);

                    // 步骤3 如果没有，新建一匿名数组（连续抽象内存空间）
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

                    // 步骤4 地址偏移1：addr-1,查找是否存在下标为addr-1对应的元素，存在则返回，不存在新建。
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

                    // 步骤5 执行p=p-1的操作
                    if (ptSubOne instanceof PointerVariableDomain) {
                        // add by yaochi pt--后设置其parent为current的parent2013-09-03
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
        } else if (node.jjtGetChild(0) instanceof ASTUnaryOperator) {// 一元操作符
            // 指针的操作符*这里取 by tangrong
            ASTUnaryOperator operator = (ASTUnaryOperator) node.jjtGetChild(0);
            String o = operator.getOperatorType().get(0);
            // 为!构造LogicalNotExpression add by wangyi
            if (o.equals("!") && expdata.currentLogicalExpression != null) {
                LogicalNotExpression lne = new LogicalNotExpression();
                expdata.currentLogicalExpression.addLRExpression(lne);
                expdata.lastLogicalExpression = expdata.currentLogicalExpression;
                expdata.currentLogicalExpression = lne;
            }
            AbstractExpression castexpression = (AbstractExpression) node.jjtGetChild(1);
            node.jjtGetChild(1).jjtAccept(this, expdata);// F6 OK when o = "*"

            // 一个变量作为罗辑表达式时　add by wangyi
            if (expdata.vd instanceof PrimitiveVariableDomain && expdata.vd.getVariableNameDeclaration() != null && !expdata.vd.getVariableNameDeclaration().getName().startsWith("tmp_")
                    && !((SimpleNode) node.jjtGetParent() instanceof ASTAdditiveExpression)) {
                PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) expdata.vd;
                if (!pvd.isConst()) {
                    if (expdata.currentLogicalExpression != null) {
                        expdata.currentLogicalExpression.addLRExpression(new RelationExpression(expdata.value, null, null));
                    }
                }
            }

            // 指针的情况 add by wangyi
            if (expdata.vd instanceof PointerVariableDomain && isConstrainNode((SimpleNode) node.jjtGetParent().jjtGetParent()) && expdata.vd.getVariableNameDeclaration() != null
                    && !expdata.vd.getVariableNameDeclaration().getName().startsWith("tmp_") && !((SimpleNode) node.jjtGetParent() instanceof ASTEqualityExpression)
                    && !((SimpleNode) node.jjtGetParent() instanceof ASTRelationalExpression)) {
                if (expdata.currentLogicalExpression != null) {
                    expdata.currentLogicalExpression.addLRExpression(new RelationExpression(expdata.value, null, null));
                }
            }

            // 恢复现场 add by wangyi
            if (o.equals("!")) {
                expdata.currentLogicalExpression = expdata.lastLogicalExpression;
            }

            // add by xjx 2012-6-29
            if (((node.jjtGetParent() instanceof ASTAssignmentExpression) || (node.jjtGetParent() instanceof ASTLogicalANDExpression) || (node.jjtGetParent() instanceof ASTLogicalORExpression))
                    && (node.getSingleChildofType(ASTAssignmentExpression.class) == null)) {
                if (node.getParentsOfType(ASTExpressionStatement.class).size() == 0 && (node.getParentsOfType(ASTIterationStatement.class).size() == 0 || node.jjtGetParent().jjtGetNumChildren() == 1 // 中的i++有问题，有待解决
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
                                // logger.debug("ASTUnaryExpression中对应的复杂表达式: "+expdata.origexplist.get(i).toString());
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
                // 不知道怎么处理
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
            } else if (o.equals("*")) { // 指针取值操作符号*p
                /**
                 * 获取*p对应的vd，如果没有生成一个 add by tangrong 2011-12-15
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
                    if (memvd instanceof PointerVariableDomain && ((PointerVariableDomain) memvd).getPointerTo() != null) {// 如果VD为指针且指针指向的区域不为指针类型，则将其state改为NOTNULL
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
                } else if (!(expdata.vd instanceof FunctionVariableDomain)) {// 有*号但不是数组类型，则说明为指针类型,同时区别出不是函数指针类型
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
                        // 指针如果没有指向某一个具体内容
                        varDomain.initMemberVD();
                    }
                    varDomain.setStateNotNull();// 则对指针赋值
                    if (varDomain.initStateIsNull())
                        varDomain.setInitStateAndPtNotNull();
                    // if()
                    VariableDomain childvd = ((PointerVariableDomain) expdata.vd).getPointerTo();

                    if (childvd != null && childvd.getNd() instanceof VariableNameDeclaration) {
                        if (expdata.currentvex.getValueSet().getValue((VariableNameDeclaration) childvd.getNd()) != null) {
                            // 已经存在于抽象内存中，则直接取值
                            expdata.vd = varDomain.getPointerTo();
                        } else if (childvd instanceof ArrayVariableDomain && childvd.getNd().getImage().startsWith("*")) {
                            // 数组指针(*p)[2]单独处理
                            String image = childvd.getNd().getImage().replace("*", "").replace(".", "_") + "_point1arr";
                            VariableNameDeclaration vnd4MemoryBlock = new VariableNameDeclaration(node.getFileName(), node.getScope(), image, node, childvd.getNd().getImage());
                            // vnd4MemoryBlock.setType(new
                            // CType_Array(childvd.getType()));
                            // //为其指针建立内存，所以类型应该是他指向的类型
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
                                if (operators.contains("[")) {// 操作中存在[符号主要用于处理（*p）[1]类型
                                    String image = childvd.getNd().getImage().replace("*", "") + "_point2arr";
                                    VariableNameDeclaration vnd4MemoryBlock = new VariableNameDeclaration(node.getFileName(), node.getScope(), image, node, childvd.getNd().getImage());
                                    vnd4MemoryBlock.setType(childvd.getType()); // 为其指针建立内存，所以类型应该是他指向的类型
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
                // 不处理
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
                    // add by yaochi 2013-09-04 添加对抽象内存的支持
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
                // liuli 2010.8.13 添加对sizeof ("qwefff");这种情况的处理
                Domain ret = expdata.value.getDomain(con.getCurrentVexNode().getSymDomainset());
                if (ret != null && ret instanceof PointerDomain) {
                    PointerDomain p = (PointerDomain) ret;
                    long i = p.getLen(p);
                    expdata.value = new Expression(i);
                    // add by yaochi 2013-09-04 添加对抽象内存的支持
                    VariableNameDeclaration sizeofvnd = new VariableNameDeclaration(node.getFileName(), node.getScope(), "countofsize", node, "countofsize");
                    sizeofvnd.setType(CType_BaseType.intType);
                    PrimitiveVariableDomain sizeofpvd = new PrimitiveVariableDomain(sizeofvnd, expdata.currentvex, expdata.value);
                    sizeofpvd.setExpression(expdata.value);
                    expdata.vd = sizeofpvd;
                    // addend
                    return data;
                }
            } else {
                // liuli：sizeof的参数有可能为自定义的类型，在语法树中并不当做ASTTypeName识别
                Scope scope = node.getScope();
                String image = ((ASTUnaryExpression) node.jjtGetChild(0)).getImage();
                NameDeclaration decl = Search.searchInVariableUpward(image, scope);
                if (decl != null && decl.getType() != null) {
                    expdata.value = new Expression(decl.getType().getSize());
                    // add by yaochi 2013-09-04 添加对抽象内存的支持
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
                    // add by yaochi 2013-09-04 添加对抽象内存的支持
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
            // 不知道怎么处理
        } else if (node.getImage().contains("real")) {
            // 不知道怎么处理
        } else if (node.getImage().contains("imag")) {
            // 不知道怎么处理
        } else {

            // 不知道怎么处理
        }

        expdata.value = new Expression(SymbolFactor.genSymbol(node.getType()));
        if (expdata.vd instanceof PrimitiveVariableDomain) {
            PrimitiveVariableDomain pvd = (PrimitiveVariableDomain) expdata.vd;
            expdata.value = pvd.getExp();
        }
        return data;
    }

    /**
     * 处理位运算表达式
     * 
     * @param node
     * @param data
     * @param op
     * @return
     *         created by Yaoweichang on 2015-04-17 下午4:01:00
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
            // 从左到右进行符号计算
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
                            // 确定哪个值域是确定的
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
                            // 扫描已知量，确定二进制字符串类型
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
                                 * atBegin='0'证明二进制字符串是"000…111…000"的形式，
                                 * 这时要新生成两个中间辅助变量声明和符号binaryHelp和binaryZ, 如果是x
                                 * &y，y的值域是确定值，最终变形为leftvd=x-RatioForHelp
                                 * *binaryHelp-binaryZ;
                                 * 如果是x|y，y的值域是确定值，最终变形为leftvd
                                 * =x+RatioForHelp*binaryHelp
                                 */
                                if (atBegin == '0' && (op.equals("&") || (op.equals("|")))) {// baiyu
                                    int begin = binaryString.length() - count1 - count2 + 1;// 1开始的位置
                                    int end = binaryString.length() - count1;// 1结束的位置
                                    ArrayList<VariableNameDeclaration> tempList = new ArrayList<VariableNameDeclaration>();// 存放与辅助变量相关的变量

                                    // 生成中间变量binaryHelp,当符号为&时，要求中间变量的取值范围为i1/pow(2,end),这里可以直接设置吗？？？
                                    VariableNameDeclaration binaryHelpVND =
                                            new VariableNameDeclaration(node.getFileName(), node.getScope(), "bianryHelp_" + node.toString(), node, "binaryHelp" + node.toString());
                                    binaryHelpVND.setType(CType_BaseType.getBaseType("int"));
                                    SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"), "binaryHelp");
                                    sym.setRelatedVar(binaryHelpVND);
                                    Expression binaryHelpExp = new Expression(sym);
                                    VariableDomain binaryHelpVD = new PrimitiveVariableDomain(binaryHelpVND, VariableSource.LOCAL, expdata.currentvex, sym);
                                    expdata.currentvex.addValue(binaryHelpVND, binaryHelpVD);// 其实在这里binaryHelp的初始区间已经确定为[-inf.+inf]

                                    if (op.equals("&")) {
                                        int tmp1 = (int) Math.pow(2, end);
                                        Expression RatioForHelp = new Expression(tmp1);// 这是binaryHelp的系数，根据扫描字符串的结果得到
                                        IntegerDomain temp1 = new IntegerDomain((int) Math.pow(2, end), (int) Math.pow(2, end));
                                        IntegerDomain binaryHelpDomain = IntegerDomain.div(i1, temp1);
                                        expdata.currentvex.addSymbolDomain(sym, binaryHelpDomain);// 在这里设置中间符号变量binaryHelp的初始区间？？

                                        // 生成中间符号变量binaryZ,取值区间为[0,pow(2,begin-1)-1]
                                        VariableNameDeclaration binaryZVND =
                                                new VariableNameDeclaration(node.getFileName(), node.getScope(), "binaryZ_" + node.toString(), node, "binaryZ_" + node.toString());
                                        binaryZVND.setType(CType_BaseType.getBaseType("int"));
                                        SymbolFactor symZ = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"), "binaryZ");
                                        symZ.setRelatedVar(binaryZVND);
                                        Expression binaryZExp = new Expression(symZ);
                                        VariableDomain binaryZVD = new PrimitiveVariableDomain(binaryZVND, VariableSource.LOCAL, expdata.currentvex, symZ);
                                        IntegerDomain temp2 = new IntegerDomain(0, (int) Math.pow(2, begin - 1) - 1);// binaryZ的初始区间
                                        Expression lowbound = new Expression(0);
                                        Expression highbound = new Expression((int) (Math.pow(2, begin - 1) - 1));

                                        expdata.currentvex.addValue(binaryZVND, binaryZVD);
                                        // expdata.currentvex.addSymbolDomain(symZ,
                                        // temp2);//初始区间在这里设置？？

                                        // 为变形后的左侧生成一个变量声明、表达式和VD
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
                                    } else if (op.equals("|")) {// 当符号为|时，binaryHelp的取值范围是[0,pow(2,count2)-1]
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
                                     * 当atBegin=='1'时，说明二进制字符串是"111…000…111"的形式
                                     * ，这时要新生成一个中间辅助变量声明和符号binaryHelp
                                     * 此时binaryHelp的取值区间为[0,pow(2,count2)-1]
                                     */
                                    int begin = binaryString.length() - count1 - count2 + 1;// 0开始的位置
                                    int end = binaryString.length() - count1;
                                    VariableNameDeclaration binaryHelpVND =
                                            new VariableNameDeclaration(node.getFileName(), node.getScope(), "bianryHelp_" + node.toString(), node, "binaryHelp" + node.toString());
                                    binaryHelpVND.setType(CType_BaseType.getBaseType("int"));
                                    SymbolFactor sym = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"), "binaryHelp");
                                    sym.setRelatedVar(binaryHelpVND);
                                    Expression binaryHelpExp = new Expression(sym);
                                    VariableDomain binaryHelpVD = new PrimitiveVariableDomain(binaryHelpVND, VariableSource.LOCAL, expdata.currentvex, sym);
                                    int tmp1 = (int) Math.pow(2, begin - 1);
                                    IntegerDomain bianryHelpDomain = new IntegerDomain(0, (int) (Math.pow(2, count2) - 1));// 初始取值区间

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
                            } else {// 处理一个未知变量的情况 add by Yaoweichang
                                if (leftVD instanceof PrimitiveVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                                    PrimitiveVariableDomain left = (PrimitiveVariableDomain) leftVD;
                                    PrimitiveVariableDomain right = (PrimitiveVariableDomain) rightVD;
                                    Expression tmpvalue = null;
                                    VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                                    tmpVND.setType(left.getType());

                                    // 合并
                                    if (op.trim().equals("&")) {// 按位与
                                        leftvalue = leftvalue.and(rightvalue);
                                        tmpvalue = left.getExpression().and(right.getExpression());
                                    } else if (op.trim().equals("|")) {// 按位或
                                        leftvalue = leftvalue.inclusiveOR(rightvalue);
                                        tmpvalue = left.getExpression().inclusiveOR(right.getExpression());
                                    } else if (op.trim().equals("^")) {// 按位异或
                                        leftvalue = leftvalue.exclusiveOR(rightvalue);
                                        tmpvalue = left.getExpression().exclusiveOR(right.getExpression());
                                    }
                                    leftVD = new PrimitiveVariableDomain(tmpVND, expdata.currentvex, tmpvalue);
                                }
                            }

                        }
                    } else {// 两者全部都是位变量 add by Yaoweichang
                        if (leftVD instanceof PrimitiveVariableDomain && rightVD instanceof PrimitiveVariableDomain) {
                            PrimitiveVariableDomain left = (PrimitiveVariableDomain) leftVD;
                            PrimitiveVariableDomain right = (PrimitiveVariableDomain) rightVD;
                            Expression tmpvalue = null;
                            VariableNameDeclaration tmpVND = new VariableNameDeclaration(node.getFileName(), node.getScope(), "tmp_" + node.toString(), node, "tmp_" + node.toString());
                            tmpVND.setType(left.getType());

                            // 合并
                            if (op.trim().equals("&")) {// 按位与
                                leftvalue = leftvalue.and(rightvalue);
                                tmpvalue = left.getExpression().and(right.getExpression());
                            } else if (op.trim().equals("|")) {// 按位或
                                leftvalue = leftvalue.inclusiveOR(rightvalue);
                                tmpvalue = left.getExpression().inclusiveOR(right.getExpression());
                            } else if (op.trim().equals("^")) {// 按位异或
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
            // 变量处理
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
            // logger.info("捕获自定义异常："+msg);
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
