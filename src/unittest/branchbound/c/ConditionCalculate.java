package unittest.branchbound.c;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import softtest.config.c.Config;
import softtest.domain.c.analysis.ConditionData;
import softtest.domain.c.analysis.SymbolDomainSet;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.DoubleDomain;
import softtest.domain.c.interval.DoubleMath;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.symbolic.Expression;
import softtest.domain.c.symbolic.Factor;
import softtest.domain.c.symbolic.LRExpression;
import softtest.domain.c.symbolic.LogicalExpression;
import softtest.domain.c.symbolic.LogicalNotExpression;
import softtest.domain.c.symbolic.NumberFactor;
import softtest.domain.c.symbolic.Power;
import softtest.domain.c.symbolic.RelationExpression;
import softtest.domain.c.symbolic.SymbolExpression;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.domain.c.symbolic.Term;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_BaseType;

/**
 * 区间运算主要的类，运算的逻辑和ConditionDomainVisitor.java类似，不同的是没有用访问者模式
 * 注意：需要支持不同的覆盖准则，语句和分支覆盖的区间运算方法是一样的，MCDC略有不同，需要以后进行支持
 * 2014年12月4日 <br>
 * 处理的约束是符号表达式，符号表达式的 结构是
 * SymbolExpression = LogicExpression | ~logicExpression
 * LogicExpression = RelationExpression R LogicExpression R={&&,||}
 * RelationExpression = Expression R Expression R={>,<,==,>=,<=,!=}
 * Expression = Term....
 * Term ...
 * Power ...
 * Factor...
 * 
 * @author xiongwei
 *         若有必要 ，可以变成抽象类
 *         * 按照符号表达式的顺序依次进行区间的计算。<br>
 *         对符号表达式集合中的每一个符号进行顺次的计算，并根据分支“真 假”进行区间的分析<br>
 * 
 * @param 输入的{ &lt;符号：区间 &gt;}
 * 
 * @return 经过一次区间运算得到的区间值<br>
 *         经过区间运算之后的区间symboldomainAfterCalculate，若区间运算发生矛盾，
 *         那么运算之后的symboldomainAfterCalculate中会有区间是empty的符号，
 *         并且ConditionCalculate.constrainsIsValid标识会变为false，标识不满足约束。
 */
public class ConditionCalculate {

    private ConstraintExtractor constrains = new ConstraintExtractor();
    private SymbolDomainSet initSymbolDomainSet = null;
    // 本次的符号取值是否满足约束表示，true表示满足约束，false 表示不满足
    private boolean constrainsIsValid = true;
    private SymbolDomainSet symbolDomainSetAfterConditionCalculate = null;
    private Hashtable<SymbolFactor, Domain> tmptable;

    public ConditionCalculate(ConstraintExtractor constrains, SymbolDomainSet symbolDomainSet) {
        // TODO Auto-generated constructor stub
        this.constrains = constrains;
        this.initSymbolDomainSet = symbolDomainSet;
    }

    /**
     * 按照符号表达式的顺序依次进行区间的计算。
     * 
     * @return 经过一次区间运算得到的区间值
     */
    public SymbolDomainSet calculateDomain(SymbolDomainSet initSymbolDomainSet) {
        // TODO Auto-generated method stub
        List<SymbolExpression> symExprList = this.constrains.getSymbolExpressions();
        this.setInitSymbolDomainSet(initSymbolDomainSet);
        SymbolDomainSet symboldomainAfterCalculate = this.getInitSymbolDomainSet();
        ConditionData conDomainData = new ConditionData();

        for (SymbolExpression se : symExprList) {
            if (se.getLogicalExpression() == null || se.getLogicalExpression().getExpressions().size() == 0) {
                continue;
            }
            conDomainData.clearDomains();
            if (symboldomainAfterCalculate.isContradict())
                break;
            // 表达式的真假分支标志
            boolean predict = se.isTF();
            try {
                conDomainData = CalculateSymbolExpression(se, conDomainData, symboldomainAfterCalculate);
            } catch (Exception e) {
                // 区间运算异常
                e.printStackTrace();
                continue;
            }

            // 利用may must处理真假分支 的符号区间变化//switch如何表示？应当扩充symbolExpression的结构，约束提取暂时不支持
            // 此处需要考虑真假分支对位运算变量约束形式的影响，下一步需要做真假分支约束变形
            if (predict == true) {
                // 如果是真分支
                SymbolDomainSet ds = conDomainData.getTrueMayDomainSet();
                symboldomainAfterCalculate = SymbolDomainSet.intersect(symboldomainAfterCalculate, ds);
            } else if (predict == false) {
                SymbolDomainSet ds = conDomainData.getFalseMayDomainSet(symboldomainAfterCalculate);
                symboldomainAfterCalculate = SymbolDomainSet.intersect(symboldomainAfterCalculate, ds);
            } else {

                // 其他情况，例如switch_case 还未支持
            }

            if (symboldomainAfterCalculate.isContradict())
                break;
        }
        this.symbolDomainSetAfterConditionCalculate = symboldomainAfterCalculate;
        this.constrainsIsValid = !symboldomainAfterCalculate.isContradict();

        return symboldomainAfterCalculate;
    }

    /**
     * 迭代的进行区间运算，直至出现矛盾或者区间稳定
     * 此过程约束不变化，只有区间变化
     * 
     * @param initSymbolDomainSet2
     * @return
     */

    public SymbolDomainSet iterateCalculateDomain(SymbolDomainSet initSymbolDomainSet) {
        // TODO Auto-generated method stub
        SymbolDomainSet symboldomainBeforeCalculate = initSymbolDomainSet;
        SymbolDomainSet symboldomainAfterCalculate = this.symbolDomainSetAfterConditionCalculate;
        while (!symboldomainBeforeCalculate.equals(symboldomainAfterCalculate)) {
            symboldomainAfterCalculate = calculateDomain(symboldomainBeforeCalculate);
            if (symboldomainAfterCalculate.isContradict())
                break;
            symboldomainBeforeCalculate = symboldomainAfterCalculate;
        }

        return symboldomainAfterCalculate;
    }

    /**
     * 处理 symbolExpression 一个symbolEXpression由一个logicExpression和逻辑真假组成
     * 
     * @param se
     * @param symbolExprConData
     * @param symboldomain
     * @return conditiondata 暂时 应该是SymbolExpression的返回值，是逻辑真假
     * @throws Exception
     */
    private ConditionData CalculateSymbolExpression(SymbolExpression se, ConditionData symbolExprConData, SymbolDomainSet symboldomain) throws Exception {
        LogicalExpression logicExpression = se.getLogicalExpression();
        symbolExprConData = calculateLogicalExpression(logicExpression, symbolExprConData, symboldomain);
        return symbolExprConData;
    }

    /**
     * 处理LogicExpression logicExpression由多个relationExpression通过逻辑运算符&& ||连接起来，<br>
     * 依次处理每一个relationExpression，每处理一个relationExpression之后，就根据其之前的逻辑运算符进行<br>
     * 调用condata的合并算法
     * LogicExpression 可能由 RelationExpression LogicExpression LogicNotExpression组成
     * 
     * @param le
     * @param symbolExprConData
     * @param symboldomain
     * @return conditiondata 暂时 应该改成 LogicExpression 的返回值，是逻辑真假
     * @throws Exception
     */
    private ConditionData calculateLogicalExpression(LogicalExpression le, ConditionData symbolExprConData, SymbolDomainSet symboldomain) throws Exception {
        List<LRExpression> LRExprs = le.getExpressions();
        List<String> logicOperators = le.getOperators();

        LRExpression firstRE = LRExprs.get(0);
        ConditionData leftREconData = new ConditionData();
        // 计算首个LRExpression的condata 若之后有逻辑运运算符连接的LRExpression 再根据不同的逻辑运算符做不同的处理
        leftREconData = calculateLRExpression(firstRE, symbolExprConData, symboldomain);
        ConditionData nextREconData = new ConditionData();

        for (int i = 1; i <= logicOperators.size(); i++) {
            LRExpression nextRE = LRExprs.get(i);
            nextREconData = calculateLRExpression(nextRE, nextREconData, symboldomain);
            String nextLogicOperator = logicOperators.get(i - 1);
            // 根据left和right的逻辑操作符，来计算区间
            if (nextLogicOperator.equals("&&"))
                leftREconData = symbolExprConData.calLogicalAndExpression(leftREconData, nextREconData, symboldomain);
            else if (nextLogicOperator.equals("||")) {
                leftREconData = symbolExprConData.calLogicalOrExpression(leftREconData, nextREconData, symboldomain);
            } else {
                // 其他逻辑操作符，未处理
                try {
                    throw (new Exception("LogicOperator not supported yet"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            symbolExprConData.setDomainsTable(leftREconData.getDomainsTable());
        }
        return symbolExprConData;
    }

    /**
     * 处理LRExpression 根据是 logicExpression 还是
     * 
     * @param firstRE
     * @param symbolExprConData
     * @param symboldomain
     * @return
     * @throws Exception
     * @author tangyubin
     * @time 2015-3-30
     */
    private ConditionData calculateLRExpression(LRExpression firstLRE, ConditionData symbolExprConData, SymbolDomainSet symboldomain) throws Exception {
        // 由于抽象基类没有方法，只能用instanceof来判断了
        ConditionData nextLREconData = new ConditionData();

        if (firstLRE instanceof RelationExpression) {
            RelationExpression firstRE = (RelationExpression) firstLRE;
            nextLREconData = calculateRelationExpression(firstRE, symbolExprConData, symboldomain);
        } else if (firstLRE instanceof LogicalNotExpression) {
            LogicalNotExpression firstLNExpr = (LogicalNotExpression) firstLRE;
            nextLREconData = calculateLogicalNotExpression(firstLNExpr, symbolExprConData, symboldomain);
        } else if (firstLRE instanceof LogicalExpression) {
            LogicalExpression firstLExpr = (LogicalExpression) firstLRE;
            nextLREconData = calculateLogicalExpression(firstLExpr, symbolExprConData, symboldomain);
        }

        return nextLREconData;
    }

    /**
     * 处理 !(LogicalExpression )
     * 
     * @param firstLNExpr
     * @param symbolExprConData
     * @param symboldomain
     * @return
     * @throws Exception
     */
    private ConditionData calculateLogicalNotExpression(LogicalNotExpression firstLNExpr, ConditionData symbolExprConData, SymbolDomainSet symboldomain) throws Exception {
        LogicalExpression logicalExpression = firstLNExpr.getLogicalExpression();
        ConditionData nextLconData = new ConditionData();
        nextLconData = calculateLogicalExpression(logicalExpression, symbolExprConData, symboldomain);
        SymbolDomainSet may = nextLconData.getFalseMayDomainSet(symboldomain);
        SymbolDomainSet must = nextLconData.getFalseMustDomainSet(symboldomain);
        symbolExprConData.addMayDomain(may);
        symbolExprConData.addMustDomain(must);

        return symbolExprConData;
    }

    private ConditionData calculateRelationExpression(RelationExpression RE, ConditionData symbolExprConData, SymbolDomainSet symboldomain) throws Exception {
        // 得到左边
        Expression leftExpr = RE.getLeftvalue();
        Expression rightExpr = RE.getRightvalue();
        String operator = RE.getOperator();
        if (operator == null) {
            // 没有比较符的情况 例如if(a) if(a+b) if(-a)
            symbolExprConData = CalculateNoRelationExpression(leftExpr, symbolExprConData, symboldomain);
        } else if (operator.equals(">") || operator.equals("<") || operator.equals("<=") || operator.equals(">=")) {
            // 处理 >,<,>=,<=
            symbolExprConData = CalculateGreaterOrLessExpression(leftExpr, rightExpr, operator, symbolExprConData, symboldomain);
        } else if (operator.equals("==") || operator.equals("!=")) {
            // 处理 == !=
            symbolExprConData = CalculateEqualityExpression(leftExpr, rightExpr, operator, symbolExprConData, symboldomain);
        } else if (operator.equals("!")) {
            // 处理 !Expr 在logicNot中处理

        } else {
            throw new Exception("relationOperator not handled");
        }
        return symbolExprConData;
    }

    /**
     * 处理条件节点中没比较符的情况 例如 if(a+b) 此时只有leftExpr “a+b”<br>
     * 逻辑按照 if(a+b!=0)来处理 情况有： if(a) if(-a) if(3*a)
     * 
     * @param leftExpr
     * @param symbolExprConData
     * @param symboldomain
     * @return
     */
    private ConditionData CalculateNoRelationExpression(Expression leftExpr, ConditionData symbolExprConData, SymbolDomainSet symboldomain) {
        Expression exp = leftExpr;
        ConditionData condata = symbolExprConData;
        // 以下涉及到位运算符号R时，将约束表达式(a R b)等价为((a R b) != 0) add by Yaoweichang
        if (exp.getTerms().get(0).getOperator() == "&") {
            condata = CalculateAndExpression(exp, null, "!=", symbolExprConData, symboldomain);
            return condata;
        }
        if (exp.getTerms().get(0).getOperator() == "|") {
            condata = CalculateInclusiveORExpression(exp, null, "!=", symbolExprConData, symboldomain);
            return condata;
        }
        if (exp.getTerms().get(0).getOperator() == "^") {
            condata = CalculateExclusiveORExpression(exp, null, "!=", symbolExprConData, symboldomain);
            return condata;
        }

        // if中的式子如果是数值，if(R)并且不为零则真域为全域 若R为0那么则为全假
        if (exp != null && exp.getSingleFactor() instanceof NumberFactor) {
            NumberFactor f = (NumberFactor) exp.getSingleFactor();
            Domain may = null, must = null;
            SymbolFactor s = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
            if ((f.getDoubleValue() == 0)) {
                may = IntegerDomain.getEmptyDomain();
                must = IntegerDomain.getEmptyDomain();
                symbolExprConData.addMayDomain(s, may);
                symbolExprConData.addMustDomain(s, must);
            } else {
                may = IntegerDomain.getFullDomain();
                must = IntegerDomain.getFullDomain();
                symbolExprConData.addMayDomain(s, may);
                symbolExprConData.addMustDomain(s, must);
            }

            return symbolExprConData;
        }
        // 否则处理 if(a+b) if(3a) if(-a) if(a)等情况
        for (Term t : exp.getTerms()) {
            Factor f = t.getSingleFactor();
            if (f instanceof SymbolFactor) {
                SymbolFactor s = (SymbolFactor) f;
                Expression temp = new Expression(s).sub(exp);
                Domain rightdomain = temp.getDomain(symboldomain);
                Domain leftdomain = s.getDomainWithoutNull(symboldomain);
                CType type = s.getType();

                if (rightdomain != null) {
                    Domain may = null, must = null;
                    // 2012-10-16 如果系数为1 may=left-right; must = left-(~C.right)v!=[min,max] E
                    // min!=max

                    may = Domain.intersect(leftdomain, rightdomain, type);// 15-01-14

                    if (rightdomain.isCanonical()) {
                        must = Domain.intersect(leftdomain, rightdomain, type);
                    } else {
                        must = Domain.getEmptyDomainFromType(s.getType());
                    }

                    Domain temp1 = Domain.inverse(must);
                    Domain temp2 = Domain.inverse(may);
                    symbolExprConData.addMayDomain(s, temp1);
                    symbolExprConData.addMustDomain(s, temp2);

                }
            }
            // 处理系数为-1情况
            f = t.getMinusSingleFactor();
            if (f instanceof SymbolFactor) {
                SymbolFactor s = (SymbolFactor) f;
                Expression temp = new Expression(s).add(exp);//
                Domain rightdomain = temp.getDomain(symboldomain);
                Domain leftdomain = s.getDomainWithoutNull(symboldomain);
                CType type = s.getType();

                if (rightdomain != null) {
                    Domain may = null, must = null;
                    // may = Domain.intersect(leftdomain, rightdomain, type);//会导致真分支的时候走了假区间
                    may = Domain.intersect(leftdomain, rightdomain, type);// 根据文档修改 2012-10-17 by
                                                                          // zhangxuzhou

                    if (rightdomain.isCanonical()) {
                        must = Domain.intersect(leftdomain, rightdomain, type);
                        // must=Domain.substract(leftdomain, rightdomain, type);
                    } else {
                        must = Domain.getEmptyDomainFromType(s.getType());
                    }
                    Domain temp1 = Domain.inverse(must);
                    Domain temp2 = Domain.inverse(may);
                    symbolExprConData.addMayDomain(s, temp1);
                    symbolExprConData.addMustDomain(s, temp2);
                }
            }
            // added zxz
            f = t.getRatioSingleFactor();
            if (f instanceof SymbolFactor) {

                SymbolFactor s = (SymbolFactor) f;
                NumberFactor tratio = t.getRatio();// 获得不为±1的系数
                double ratio = tratio.getDoubleValue();
                Expression temp = new Expression(t, true).sub(exp);// 得到剩余的表达式
                Domain rightdomain = temp.getDomain(symboldomain);

                if (rightdomain != null && !rightdomain.isUnknown()) {
                    CType type = s.getType();
                    Domain may = null, must = null;
                    Domain leftdomain = s.getDomainWithoutNull(symboldomain);

                    IntegerDomain integerdomain = null;
                    DoubleDomain doubledomain = null;
                    switch (Domain.getDomainTypeFromType(type)) {
                        case INTEGER:
                            IntegerDomain rigthdomainTointeger;
                            rigthdomainTointeger = Domain.castToIntegerDomain(rightdomain);
                            // if(integerdomain.)
                            // 先判断能否整除
                            if ((rigthdomainTointeger.intervals.first().getMin() % ratio) != 0)// 这里是
                                                                                               // (3a+b
                                                                                               // )等效于
                                                                                               // 3a!=-b
                                integerdomain = IntegerDomain.getFullDomain();// 如果不能整除，则integerdomain是全集
                                                                              // 2012-10-17
                                                                              // 整除的判断有问题判断有问题。
                            else
                                integerdomain = IntegerDomain.reduce(rigthdomainTointeger, ratio);// 如果能整除的话，也需要考虑取整的问题
                                                                                                  // 2012-10-18
                            // 按比例缩小，注意这里，结果如果不能整除的话，对于if(3a+2)的情况，a的may就应该是全集2012-6-21
                            // 2012-11-5 有问题 需要改 需要取整 之后的区间
                            // long max = integerdomain.getMax(), min = integerdomain.getMin();

                            // may=Domain.substract(leftdomain, integerdomain, type);//根据文档修改
                            // 2012-10-17 by zhangxuzhou
                            may = Domain.intersect(leftdomain, integerdomain, type);
                            if (integerdomain.isCanonical()) {
                                // may = Domain.intersect(leftdomain, integerdomain, type);
                                // must = Domain.intersect(leftdomain, integerdomain, type);
                                must = Domain.intersect(leftdomain, integerdomain, type);// 根据文档修改
                                                                                         // 2012-10-17
                                                                                         // by
                                                                                         // zhangxuzhou
                            } else {
                                // may = Domain.intersect(leftdomain, integerdomain, type);
                                must = Domain.getEmptyDomainFromType(s.getType());
                            }

                            Domain temp1 = Domain.inverse(must);
                            Domain temp2 = Domain.inverse(may);
                            symbolExprConData.addMayDomain(s, temp1);
                            symbolExprConData.addMustDomain(s, temp2);
                            break;
                        case DOUBLE://
                            doubledomain = Domain.castToDoubleDomain(rightdomain);
                            doubledomain = DoubleDomain.reduce(doubledomain, ratio);
                            may = Domain.intersect(leftdomain, doubledomain, type);
                            // may=Domain.substract(leftdomain, rightdomain, type);//根据文档修改
                            // 2012-10-17 by zhangxuzhou
                            if (doubledomain.isCanonical()) {
                                must = Domain.intersect(leftdomain, integerdomain, type);
                                // must=Domain.substract(leftdomain, rightdomain, type);//根据文档修改
                                // 2012-10-17 by zhangxuzhou

                            } else {
                                must = Domain.getEmptyDomainFromType(s.getType());
                            }

                            temp1 = Domain.inverse(must);
                            temp2 = Domain.inverse(may);
                            symbolExprConData.addMayDomain(s, temp1);
                            symbolExprConData.addMustDomain(s, temp2);
                            break;

                    }

                }
            }// end added by zxz
        }
        return symbolExprConData;
    }

    /**
     * 处理比较符是 >,<,>=,<=的情况，具体实现参照conditionDomainVisitor中的relationalExpression
     * 已测
     * 
     * @param leftExpr
     * @param rightExpr
     * @param operator
     * @param symboldomain
     * @param symbolExprConData
     * @return
     * @throws Exception
     */
    private ConditionData CalculateGreaterOrLessExpression(Expression leftExpr, Expression rightExpr, String operator, ConditionData symbolExprConData, SymbolDomainSet symboldomain) throws Exception {
        Expression rightvalue = rightExpr, leftvalue = leftExpr;
        Expression exp = leftvalue.sub(rightvalue);
        ConditionData condata = symbolExprConData;

        // 以下涉及到位运算符号R时，即约束表达式的形式为((a R b)operator c) add by Yaoweichang
        if (leftvalue.getTerms().get(0).getOperator() == "&") {
            condata = CalculateAndExpression(leftExpr, rightExpr, operator, symbolExprConData, symboldomain);
            return condata;
        }
        if (leftvalue.getTerms().get(0).getOperator() == "|") {
            condata = CalculateInclusiveORExpression(leftExpr, rightExpr, operator, symbolExprConData, symboldomain);
            return condata;
        }
        if (leftvalue.getTerms().get(0).getOperator() == "^") {
            condata = CalculateExclusiveORExpression(leftExpr, rightExpr, operator, symbolExprConData, symboldomain);
            return condata;
        }

        // 表达式是常量的情况
        if (exp.getSingleFactor() instanceof NumberFactor) {
            NumberFactor f = (NumberFactor) exp.getSingleFactor();
            Domain may = null, must = null;
            SymbolFactor s = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
            if ((f.getDoubleValue() > 0 && operator.equals(">")) || // zys:此处的判断条件也有问题
                    (f.getDoubleValue() >= 0 && operator.equals(">=")) || (f.getDoubleValue() < 0 && operator.equals("<")) || (f.getDoubleValue() <= 0 && operator.equals("<="))) {
                may = Domain.getFullDomainFromType(f.getType());
                must = Domain.getFullDomainFromType(f.getType());

                condata.addMayDomain(s, may);
                condata.addMustDomain(s, must);
            } else {// zys: 不一定为空域
                may = IntegerDomain.getEmptyDomain();
                must = IntegerDomain.getEmptyDomain();
                condata.addMayDomain(s, may);
                condata.addMustDomain(s, must);
            }
            return condata;
        }
        // chh 2010.12.8 if(m>n)m,n都未知时无法判断。
        Domain LDomain = leftvalue.getDomain(symboldomain);
        Domain RDomain = rightvalue.getDomain(symboldomain);

        if (LDomain == null || RDomain == null)
            throw new Exception("expression can't get domain from symboldomainset");
        if (Config.USEUNKNOWN && (LDomain != null && RDomain != null && LDomain.isUnknown() && RDomain.isUnknown())) {
            return condata;
        }
        for (Term t : exp.getTerms()) {
            Factor f = t.getSingleFactor();
            if (f instanceof SymbolFactor) {
                SymbolFactor s = (SymbolFactor) f;
                Expression temp = new Expression(s).sub(exp);
                Domain rightdomain = temp.getDomain(symboldomain);

                if (rightdomain != null && !rightdomain.isUnknown()) {
                    CType type = s.getType();
                    Domain may = null, must = null;
                    Domain leftdomain = s.getDomainWithoutNull(symboldomain);

                    IntegerDomain integerdomain = null;
                    DoubleDomain doubledomain = null;
                    switch (Domain.getDomainTypeFromType(type)) {
                        case INTEGER:
                            integerdomain = Domain.castToIntegerDomain(rightdomain);
                            long max = integerdomain.getMax(),
                            min = integerdomain.getMin();
                            if (operator.equals(">")) {
                                may = Domain.intersect(leftdomain, new IntegerDomain(integerdomain.getMin() + 1, Long.MAX_VALUE), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain(max != Long.MAX_VALUE ? max + 1 : max, Long.MAX_VALUE), type);
                                condata.addMustDomain(s, must);
                            } else if (operator.equals(">=")) {
                                may = Domain.intersect(leftdomain, new IntegerDomain(integerdomain.getMin(), Long.MAX_VALUE), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain(integerdomain.getMax(), Long.MAX_VALUE), type);
                                condata.addMustDomain(s, must);
                            } else if (operator.equals("<")) {
                                may = Domain.intersect(leftdomain, new IntegerDomain(Long.MIN_VALUE, integerdomain.getMax() - 1), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain(Long.MIN_VALUE, min != Long.MIN_VALUE ? min - 1 : min), type);
                                condata.addMustDomain(s, must);
                            } else if (operator.equals("<=")) {
                                may = Domain.intersect(leftdomain, new IntegerDomain(Long.MIN_VALUE, integerdomain.getMax()), type);

                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain(Long.MIN_VALUE, integerdomain.getMin()), type);
                                condata.addMustDomain(s, must);
                            } else {
                                throw new RuntimeException("This is not a legal RelationalExpression");
                            }
                            break;
                        case DOUBLE:
                            doubledomain = Domain.castToDoubleDomain(rightdomain);
                            if (operator.equals(">")) {
                                may = Domain.intersect(leftdomain, new DoubleDomain(DoubleMath.nextfp(doubledomain.getMin()), Double.POSITIVE_INFINITY), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new DoubleDomain(DoubleMath.nextfp(doubledomain.getMax()), Double.POSITIVE_INFINITY), type);
                                condata.addMustDomain(s, must);
                            } else if (operator.equals(">=")) {
                                may = Domain.intersect(leftdomain, new DoubleDomain(doubledomain.getMin(), Double.POSITIVE_INFINITY), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new DoubleDomain(doubledomain.getMax(), Double.POSITIVE_INFINITY), type);
                                condata.addMustDomain(s, must);
                            } else if (operator.equals("<")) {
                                may = Domain.intersect(leftdomain, new DoubleDomain(Double.NEGATIVE_INFINITY, DoubleMath.prevfp(doubledomain.getMax())), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new DoubleDomain(Double.NEGATIVE_INFINITY, DoubleMath.prevfp(doubledomain.getMin())), type);
                                condata.addMustDomain(s, must);
                            } else if (operator.equals("<=")) {
                                may = Domain.intersect(leftdomain, new DoubleDomain(Double.NEGATIVE_INFINITY, doubledomain.getMax()), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new DoubleDomain(Double.NEGATIVE_INFINITY, doubledomain.getMin()), type);
                                condata.addMustDomain(s, must);
                            } else {
                                throw new RuntimeException("This is not a legal RelationalExpression");
                            }
                            break;
                    }
                }
            }
            // 处理系数为-1的情况
            f = t.getMinusSingleFactor();
            if (f instanceof SymbolFactor) {
                SymbolFactor s = (SymbolFactor) f;
                Expression temp = exp.add(new Expression(s));
                Domain rightdomain = temp.getDomain(symboldomain);

                if (rightdomain != null && !rightdomain.isUnknown()) {
                    CType type = s.getType();
                    Domain may = null, must = null;
                    Domain leftdomain = s.getDomainWithoutNull(symboldomain);

                    IntegerDomain integerdomain = null;
                    DoubleDomain doubledomain = null;

                    switch (Domain.getDomainTypeFromType(type)) {
                        case INTEGER:
                            integerdomain = Domain.castToIntegerDomain(rightdomain);
                            long max = integerdomain.getMax(),
                            min = integerdomain.getMin();
                            if (operator.equals("<")) {
                                may = Domain.intersect(leftdomain, new IntegerDomain(integerdomain.getMin() + 1, Long.MAX_VALUE), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain(max != Long.MAX_VALUE ? max + 1 : max, Long.MAX_VALUE), type);
                                condata.addMustDomain(s, must);
                            } else if (operator.equals("<=")) {
                                may = Domain.intersect(leftdomain, new IntegerDomain(integerdomain.getMin(), Long.MAX_VALUE), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain(integerdomain.getMax(), Long.MAX_VALUE), type);
                                condata.addMustDomain(s, must);
                            } else if (operator.equals(">")) {
                                may = Domain.intersect(leftdomain, new IntegerDomain(Long.MIN_VALUE, integerdomain.getMax() - 1), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain(Long.MIN_VALUE, min != Long.MIN_VALUE ? min - 1 : min), type);
                                condata.addMustDomain(s, must);
                            } else if (operator.equals(">=")) {
                                may = Domain.intersect(leftdomain, new IntegerDomain(Long.MIN_VALUE, integerdomain.getMax()), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain(Long.MIN_VALUE, integerdomain.getMin()), type);
                                condata.addMustDomain(s, must);
                            } else {
                                throw new RuntimeException("This is not a legal RelationalExpression");
                            }
                            break;
                        case DOUBLE:
                            doubledomain = Domain.castToDoubleDomain(rightdomain);
                            if (operator.equals("<")) {
                                may = Domain.intersect(leftdomain, new DoubleDomain(DoubleMath.nextfp(doubledomain.getMin()), Double.POSITIVE_INFINITY), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new DoubleDomain(DoubleMath.nextfp(doubledomain.getMax()), Double.POSITIVE_INFINITY), type);
                                condata.addMustDomain(s, must);
                            } else if (operator.equals("<=")) {
                                may = Domain.intersect(leftdomain, new DoubleDomain(doubledomain.getMin(), Double.POSITIVE_INFINITY), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new DoubleDomain(doubledomain.getMax(), Double.POSITIVE_INFINITY), type);
                                condata.addMustDomain(s, must);
                            } else if (operator.equals(">")) {
                                may = Domain.intersect(leftdomain, new DoubleDomain(Double.NEGATIVE_INFINITY, DoubleMath.prevfp(doubledomain.getMax())), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new DoubleDomain(Double.NEGATIVE_INFINITY, DoubleMath.prevfp(doubledomain.getMin())), type);
                                condata.addMustDomain(s, must);
                            } else if (operator.equals(">=")) {
                                may = Domain.intersect(leftdomain, new DoubleDomain(Double.NEGATIVE_INFINITY, doubledomain.getMax()), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new DoubleDomain(Double.NEGATIVE_INFINITY, doubledomain.getMin()), type);
                                condata.addMustDomain(s, must);
                            } else {
                                throw new RuntimeException("This is not a legal RelationalExpression");
                            }
                            break;
                    }
                }
            }
            // add by zxz 处理其他系数的情况。线性3*i+n>12

            f = t.getRatioSingleFactor();
            if (f instanceof SymbolFactor) {
                SymbolFactor s = (SymbolFactor) f;
                NumberFactor tratio = t.getRatio();// 获得系数
                double ratio = tratio.getDoubleValue();
                Expression temp = new Expression(t, true).sub(exp);// 得到剩余的表达式
                Domain rightdomain = temp.getDomain(symboldomain);

                if (rightdomain != null && !rightdomain.isUnknown()) {
                    CType type = s.getType();
                    Domain may = null, must = null;
                    Domain leftdomain = s.getDomainWithoutNull(symboldomain);

                    IntegerDomain integerdomain = null;
                    DoubleDomain doubledomain = null;

                    switch (Domain.getDomainTypeFromType(type)) {// 根据左边的符号的域类型设置
                        case INTEGER:
                            integerdomain = Domain.castToIntegerDomain(rightdomain);
                            if ((operator.equals(">") && ratio > 0)// 计算之后比较符是>
                                                                   // 2012-10-18
                                    || (operator.equals("<") && ratio < 0)) {
                                integerdomain = IntegerDomain.reduceByComprator(integerdomain, ratio, ">");// 将区间按系数i的比例缩小
                                                                                                           // 缩小后的取整方向取决于
                                                                                                           // 结合系数之后的比较符方向
                                                                                                           // 2012-10-17
                                long max = integerdomain.getMax(), min = integerdomain.getMin();

                                may = Domain.intersect(leftdomain, new IntegerDomain(min + 1, Long.MAX_VALUE), type);

                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain(max != Long.MAX_VALUE ? max + 1 : max, Long.MAX_VALUE), type);
                                condata.addMustDomain(s, must);
                            } else if ((operator.equals(">=") && ratio > 0)// 计算之后比较符是>=
                                    || (operator.equals("<=") && ratio < 0)) {
                                integerdomain = IntegerDomain.reduceByComprator(integerdomain, ratio, ">=");// 将区间按系数i的比例缩小
                                                                                                            // 缩小后的取整方向取决于
                                                                                                            // 结合系数之后的比较符方向
                                                                                                            // 2012-10-17
                                long max = integerdomain.getMax(), min = integerdomain.getMin();

                                may = Domain.intersect(leftdomain, new IntegerDomain((integerdomain.getMin()), Long.MAX_VALUE), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain((integerdomain.getMax()), Long.MAX_VALUE), type);
                                condata.addMustDomain(s, must);
                            } else if ((operator.equals("<") && ratio > 0)// 计算之后比较符是<
                                    || (operator.equals(">") && ratio < 0)) {
                                integerdomain = IntegerDomain.reduceByComprator(integerdomain, ratio, "<");// 将区间按系数i的比例缩小
                                                                                                           // 缩小后的取整方向取决于
                                                                                                           // 结合系数之后的比较符方向
                                                                                                           // 2012-10-17
                                long max = integerdomain.getMax(), min = integerdomain.getMin();

                                may = Domain.intersect(leftdomain, new IntegerDomain(Long.MIN_VALUE, integerdomain.getMax() - 1), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain(Long.MIN_VALUE, min != Long.MIN_VALUE ? min - 1 : min), type);
                                condata.addMustDomain(s, must);
                            } else if ((operator.equals("<=") && ratio > 0)// 计算之后比较符是<=
                                    || (operator.equals(">=") && ratio < 0)) {
                                integerdomain = IntegerDomain.reduceByComprator(integerdomain, ratio, "<=");// 将区间按系数i的比例缩小
                                                                                                            // 缩小后的取整方向取决于
                                                                                                            // 结合系数之后的比较符方向
                                                                                                            // 2012-10-17
                                long max = integerdomain.getMax(), min = integerdomain.getMin();

                                may = Domain.intersect(leftdomain, new IntegerDomain(Long.MIN_VALUE, integerdomain.getMax()), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain(Long.MIN_VALUE, integerdomain.getMin()), type);
                                condata.addMustDomain(s, must);
                            } else {
                                throw new RuntimeException("This is not a legal RelationalExpression");
                            }
                            break;
                        case DOUBLE:
                            doubledomain = Domain.castToDoubleDomain(rightdomain);
                            doubledomain = DoubleDomain.reduce(doubledomain, ratio);// 将区间按系数i的比例缩小
                            double max1 = doubledomain.getMax(),
                            min1 = doubledomain.getMin();

                            if ((operator.equals(">") && ratio > 0) || (operator.equals("<") && ratio < 0)) {
                                may = Domain.intersect(leftdomain, new DoubleDomain(DoubleMath.nextfp(min1), Double.POSITIVE_INFINITY), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new DoubleDomain(DoubleMath.nextfp(max1), Double.POSITIVE_INFINITY), type);
                                condata.addMustDomain(s, must);
                            } else if ((operator.equals(">=") && ratio > 0) || (operator.equals("<=") && ratio < 0)) {
                                may = Domain.intersect(leftdomain, new DoubleDomain(min1, Double.POSITIVE_INFINITY), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new DoubleDomain(max1, Double.POSITIVE_INFINITY), type);
                                condata.addMustDomain(s, must);
                            } else if ((operator.equals("<") && ratio > 0) || (operator.equals(">") && ratio < 0)) {
                                may = Domain.intersect(leftdomain, new DoubleDomain(Double.NEGATIVE_INFINITY, DoubleMath.prevfp(max1)), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new DoubleDomain(Double.NEGATIVE_INFINITY, DoubleMath.prevfp(min1)), type);
                                condata.addMustDomain(s, must);
                            } else if ((operator.equals("<=") && ratio > 0) || (operator.equals(">=") && ratio < 0)) {
                                may = Domain.intersect(leftdomain, new DoubleDomain(Double.NEGATIVE_INFINITY, max1), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new DoubleDomain(Double.NEGATIVE_INFINITY, min1), type);
                                condata.addMustDomain(s, must);
                            } else {
                                throw new RuntimeException("This is not a legal RelationalExpression");
                            }
                            break;
                    }
                }

            }
        }

        return condata;
    }

    /**
     * 处理if((a&b) == 2)的情况，位运算的用例生成需要借助运算法则来进行区间运算，
     * 不再依赖于原先的区间集的形式表示取值区间。
     * 
     * @param leftvalue
     * @param rightvalue
     * @param operator
     * @param symbolExprConData
     * @param symboldomain
     * @return
     *         created by Yaoweichang on 2015-04-17 下午4:32:45
     */
    private ConditionData CalculateAndExpression(Expression leftvalue, Expression rightvalue, String operator, ConditionData symbolExprConData, SymbolDomainSet symboldomain) {
        Expression exp = leftvalue;
        ConditionData condata = symbolExprConData;
        ArrayList<Power> allPowers = exp.getTerms().get(0).getPowers();
        HashSet<SymbolFactor> allSymbol = exp.getTerms().get(0).getAllSymbol();
        // 处理形式为if((a&number)==2)的特殊情况。ps:这里我们只考虑正整数参数位运算的情况
        if (allPowers.size() != allSymbol.size() && allSymbol.size() != 0) {
            Power numPower = null;
            // 获取number的Power类型表达式
            for (int i = 0; i < allPowers.size(); i++) {
                if (allPowers.get(i).isNumber()) {
                    numPower = allPowers.get(i);
                }
            }

            // 将符号a、number和等式右值转化为便于二进制运算的32位字符数组形式
            char[] symbolArr = new char[32];
            char[] numberArr = new char[32];
            char[] rightNumArr = new char[32];
            int number = Integer.parseInt(numPower.toString());
            String str = Integer.toBinaryString(number);
            int pos = 0, i = 0;
            for (i = str.length() - 1; i >= 0; i--, pos++) {
                numberArr[pos] = str.charAt(i);
            }
            for (; pos < 32; pos++)
                numberArr[pos] = '0';
            // 将rightvalue转化为32位字符数组标识
            int rightnumber;
            if (rightvalue != null)
                rightnumber = Integer.parseInt(rightvalue.toString());
            else
                rightnumber = 0;
            String rightstr = Integer.toBinaryString(rightnumber);
            for (i = rightstr.length() - 1, pos = 0; i >= 0; i--, pos++) {
                rightNumArr[pos] = rightstr.charAt(i);
            }
            for (; pos < 32; pos++)
                rightNumArr[pos] = '0';
            for (i = 0; i < symbolArr.length; i++)
                symbolArr[i] = 'T';

            // 进行符号a的抽象域计算
            for (i = 0; i < 32; i++) {
                if (numberArr[i] == '0' && rightNumArr[i] == '0')
                    symbolArr[i] = 'T';
                else if (numberArr[i] == '0' && rightNumArr[i] == '1')
                    symbolArr[i] = 'X';
                else if (numberArr[i] == '1' && rightNumArr[i] == '0')
                    symbolArr[i] = '0';
                else
                    symbolArr[i] = '1';
            }
            // 添加到对应参数符号的位约束属性
            SymbolFactor f = allSymbol.iterator().next();
            f.setIsBitCompute(true);// 设置符号属性为参与位运算
            String consExpr = new String(symbolArr);

            if (operator.equals("!=")) {// 如果符号为！=需要转换位运算约束形式
                String expr = null;
                boolean stop = false; // 假分支时只考虑取反最低一位的约束
                for (int i1 = 0; i1 < consExpr.length() && !stop; i1++) {
                    if (consExpr.charAt(i1) != 'T') {
                        char c = consExpr.charAt(i1);
                        if (c == '0')
                            expr = consExpr.replaceFirst("0", "1");
                        else
                            expr = consExpr.replaceFirst("1", "0");
                        stop = true;
                    }
                }
                consExpr = expr;
            }
            condata.addBitConstraint(f, consExpr);

            // 计算变量符号对应的may、must域值
            String consValue = new String(symbolArr);
            int value = BitParamGenerateValue(consValue);

            Domain may = null, must = null;
            may = new IntegerDomain(value, Long.MAX_VALUE);
            must = new IntegerDomain(value, value);
            if (operator.equals("==")) {
                condata.addMayDomain(f, may);
                condata.addMustDomain(f, must);
            } else if (operator.equals("!=")) {
                Domain temp1 = Domain.inverse(must);
                Domain temp2 = Domain.inverse(may);

                condata.addMayDomain(f, temp1);
                condata.addMustDomain(f, temp2);
            } else if (operator.equals(">")) {

            } else if (operator.equals(">=")) {

            } else if (operator.equals("<")) {

            } else if (operator.equals("<=")) {

            }
        } else {// 处理if((a&b) == 2)的情况
            // 将符号a、number和等式右值转化为便于二进制运算的32位字符数组形式
            char[] symbolArr = new char[32];
            char[] rightNumArr = new char[32];

            // 将rightvalue转化为32位字符数组标识
            int rightnumber, pos, i;
            if (rightvalue != null)
                rightnumber = Integer.parseInt(rightvalue.toString());
            else
                rightnumber = 0;
            String rightstr = Integer.toBinaryString(rightnumber);
            for (i = rightstr.length() - 1, pos = 0; i >= 0; i--, pos++) {
                rightNumArr[pos] = rightstr.charAt(i);
            }

            for (; pos < 32; pos++)
                rightNumArr[pos] = '0';

            // 进行符号a的抽象域计算
            for (i = 0; i < 32; i++) {
                if (rightNumArr[i] == '1') {
                    symbolArr[i] = '1';
                } else {
                    symbolArr[i] = 'T';
                }
            }
            // 添加到对应参数符号的位约束属性
            Iterator<SymbolFactor> it = allSymbol.iterator();
            while (it.hasNext()) {
                SymbolFactor sf = it.next();
                // 如果符号sf在前面存在约束，则合并其约束
                if (condata.getDomainsTable().containsKey(it)) {
                    String cons = condata.getBitConstraint(sf);
                    if (cons != null) {
                        for (i = 0; i < 32; i++) {
                            if (cons.charAt(i) == '0' || symbolArr[i] == '0') {
                                symbolArr[i] = '0';
                            } else if (cons.charAt(i) == '1' || symbolArr[i] == '1') {
                                if (cons.charAt(i) == 'T') {
                                    symbolArr[i] = 'T';
                                }
                            } else {
                                symbolArr[i] = cons.charAt(i);
                            }
                        }
                    }
                }
                condata.addBitConstraint(sf, String.valueOf(symbolArr));
            }
        }
        return condata;
    }

    /**
     * 处理if((a|b) == 2)的情况，位运算的用例生成需要借助运算法则来进行区间运算，
     * 不再依赖于原先的区间集的形式表示取值区间。
     * 
     * @param leftvalue
     * @param symbolExprConData
     * @param symboldomain
     * @return
     *         created by Yaoweichang on 2015-04-17 下午4:33:45
     */
    private ConditionData CalculateInclusiveORExpression(Expression leftvalue, Expression rightvalue, String operator, ConditionData symbolExprConData, SymbolDomainSet symboldomain) {
        Expression exp = leftvalue;
        ConditionData condata = symbolExprConData;
        ArrayList<Power> allPowers = exp.getTerms().get(0).getPowers();
        HashSet<SymbolFactor> allSymbol = exp.getTerms().get(0).getAllSymbol();
        // 处理形式为if((a&number)==2)的特殊情况,ps:这里我们只考虑正整数参数位运算的情况
        if (allPowers.size() != allSymbol.size() && allSymbol.size() != 0) {
            Power numPower = null;
            // 获取number的Power类型表达式
            for (int i = 0; i < allPowers.size(); i++) {
                if (allPowers.get(i).isNumber()) {
                    numPower = allPowers.get(i);
                }
            }

            // 将符号a、number和等式右值转化为便于二进制运算的32位字符数组形式
            char[] symbolArr = new char[32];
            char[] numberArr = new char[32];
            char[] rightNumArr = new char[32];
            int number = Integer.parseInt(numPower.toString());
            String str = Integer.toBinaryString(number);
            int pos = 0, i = 0;
            for (i = str.length() - 1; i >= 0; i--, pos++) {
                numberArr[pos] = str.charAt(i);
            }
            for (; pos < 32; pos++)
                numberArr[pos] = '0';
            // 将rightvalue转化为32位字符数组标识
            int rightnumber;
            if (rightvalue != null)
                rightnumber = Integer.parseInt(rightvalue.toString());
            else
                rightnumber = 0;
            String rightstr = Integer.toBinaryString(rightnumber);
            for (i = rightstr.length() - 1, pos = 0; i >= 0; i--, pos++) {
                rightNumArr[pos] = rightstr.charAt(i);
            }
            for (; pos < 32; pos++)
                rightNumArr[pos] = '0';
            for (i = 0; i < symbolArr.length; i++)
                symbolArr[i] = 'T';

            // 进行符号a的抽象域计算
            for (i = 0; i < 32; i++) {
                if (numberArr[i] == '0' && rightNumArr[i] == '0')
                    symbolArr[i] = '0';
                else if (numberArr[i] == '0' && rightNumArr[i] == '1')
                    symbolArr[i] = '1';
                else if (numberArr[i] == '1' && rightNumArr[i] == '0')
                    symbolArr[i] = 'X';
                else
                    symbolArr[i] = 'T';
            }
            // 添加到对应参数符号的位约束属性
            SymbolFactor f = allSymbol.iterator().next();
            f.setIsBitCompute(true);// 设置符号属性为参与位运算
            String consExpr = new String(symbolArr);
            if (operator.equals("!=")) {
                String expr = null;
                boolean stop = false; // 假分支时只考虑取反最低一位的约束
                for (int i1 = 0; i1 < consExpr.length() && !stop; i1++) {
                    if (consExpr.charAt(i1) != 'T') {
                        char c = consExpr.charAt(i1);
                        if (c == '0')
                            expr = consExpr.replaceFirst("0", "1");
                        else
                            expr = consExpr.replaceFirst("1", "0");
                        stop = true;
                    }
                }
                consExpr = expr;
            }
            condata.addBitConstraint(f, consExpr);

            // 计算变量符号对应的may、must域值
            String consValue = new String(symbolArr);
            int value = BitParamGenerateValue(consValue);

            Domain may = null, must = null;
            may = new IntegerDomain(value, Long.MAX_VALUE);
            must = new IntegerDomain(value, value);
            if (operator.equals("==")) {
                condata.addMayDomain(f, may);
                condata.addMustDomain(f, must);
            } else if (operator.equals("!=")) {
                Domain temp1 = Domain.inverse(must);
                Domain temp2 = Domain.inverse(may);

                condata.addMayDomain(f, temp1);
                condata.addMustDomain(f, temp2);
            } else if (operator.equals(">")) {

            } else if (operator.equals(">=")) {

            } else if (operator.equals("<")) {

            } else if (operator.equals("<=")) {

            }
        } else {// 处理if((a|b) == 2)的情况
            // 将符号a、number和等式右值转化为便于二进制运算的32位字符数组形式
            char[] symbolArr = new char[32];
            char[] rightNumArr = new char[32];

            // 将rightvalue转化为32位字符数组标识
            int rightnumber, pos, i;
            if (rightvalue != null)
                rightnumber = Integer.parseInt(rightvalue.toString());
            else
                rightnumber = 0;
            String rightstr = Integer.toBinaryString(rightnumber);
            for (i = rightstr.length() - 1, pos = 0; i >= 0; i--, pos++) {
                rightNumArr[pos] = rightstr.charAt(i);
            }

            for (; pos < 32; pos++)
                rightNumArr[pos] = '0';

            // 进行符号的抽象域计算
            for (i = 0; i < 32; i++) {
                if (rightNumArr[i] == '0') {
                    symbolArr[i] = '0';
                } else {
                    symbolArr[i] = 'T';
                }
            }
            // 添加到对应参数符号的位约束属性
            Iterator<SymbolFactor> it = allSymbol.iterator();
            while (it.hasNext()) {
                SymbolFactor sf = it.next();
                // 如果符号sf在前面存在约束，则合并其约束
                if (condata.getDomainsTable().containsKey(it)) {
                    String cons = condata.getBitConstraint(sf);
                    if (cons != null) {
                        for (i = 0; i < 32; i++) {
                            if (cons.charAt(i) != 'T' && symbolArr[i] == 'T') {
                                symbolArr[i] = cons.charAt(i);
                            } else if (cons.charAt(i) != symbolArr[i]) {
                                symbolArr[i] = 'X';
                            }
                        }
                    }
                }
                condata.addBitConstraint(sf, String.valueOf(symbolArr));
            }
        }
        return condata;
    }

    /**
     * 处理if((a^b) == 2)的情况，位运算的用例生成需要借助运算法则来进行区间运算，
     * 不再依赖于原先的区间集的形式表示取值区间。
     * 
     * @param leftvalue
     * @param symbolExprConData
     * @param symboldomain
     * @return
     *         created by Yaoweichang on 2015-04-17 下午4:36:45
     */
    private ConditionData CalculateExclusiveORExpression(Expression leftvalue, Expression rightvalue, String operator, ConditionData symbolExprConData, SymbolDomainSet symboldomain) {
        Expression exp = leftvalue;
        ConditionData condata = symbolExprConData;
        ArrayList<Power> allPowers = exp.getTerms().get(0).getPowers();
        HashSet<SymbolFactor> allSymbol = exp.getTerms().get(0).getAllSymbol();
        if (allPowers.size() != allSymbol.size() && allSymbol.size() != 0) {// 处理形式为if((a&number) ==
                                                                            // 2)的特殊情况,ps:这里我们只考虑正整数参数位运算的情况
            // 获取符号a和number的Power类型表达式
            Power numPower = allPowers.get(0);
            Power symbolPower = allPowers.get(1);
            System.out.println(symbolPower);
            // Factor sfactor = numPower.getSingleFactor();

            // 将符号a、number和等式右值转化为便于二进制运算的32位字符数组形式
            char[] symbolArr = new char[32];
            char[] numberArr = new char[32];
            char[] rightNumArr = new char[32];
            int number = Integer.parseInt(numPower.toString());
            String str = Integer.toBinaryString(number);
            int pos = 0, i = 0;
            for (i = str.length() - 1; i >= 0; i--, pos++) {
                numberArr[pos] = str.charAt(i);
            }
            for (; pos < 32; pos++)
                numberArr[pos] = '0';
            // 将rightvalue转化为32位字符数组标识
            int rightnumber;
            if (rightvalue != null)
                rightnumber = Integer.parseInt(rightvalue.toString());
            else
                rightnumber = 0;
            String rightstr = Integer.toBinaryString(rightnumber);
            for (i = rightstr.length() - 1, pos = 0; i >= 0; i--, pos++) {
                rightNumArr[pos] = rightstr.charAt(i);
            }
            for (; pos < 32; pos++)
                rightNumArr[pos] = '0';
            for (i = 0; i < symbolArr.length; i++)
                symbolArr[i] = 'T';

            // 进行符号a的抽象域计算
            for (i = 0; i < 32; i++) {
                if (numberArr[i] == '0' && rightNumArr[i] == '0')
                    symbolArr[i] = '0';
                else if (numberArr[i] == '0' && rightNumArr[i] == '1')
                    symbolArr[i] = '1';
                else if (numberArr[i] == '1' && rightNumArr[i] == '0')
                    symbolArr[i] = '1';
                else
                    symbolArr[i] = '0';
            }
            // 添加到对应参数符号的位约束属性
            SymbolFactor f = allSymbol.iterator().next();
            f.setIsBitCompute(true);// 设置符号属性为参与位运算
            String consExpr = new String(symbolArr);
            if (operator.equals("!=")) {
                String expr = null;
                boolean stop = false; // 假分支时只考虑取反最低一位的约束
                for (int i1 = 0; i1 < consExpr.length() && !stop; i1++) {
                    if (consExpr.charAt(i1) != 'T') {
                        char c = consExpr.charAt(i1);
                        if (c == '0')
                            expr = consExpr.replaceFirst("0", "1");
                        else
                            expr = consExpr.replaceFirst("1", "0");
                        stop = true;
                    }
                }
                consExpr = expr;
            }
            condata.addBitConstraint(f, consExpr);

            // 计算变量符号对应的may、must域值
            String consValue = new String(symbolArr);
            int value = BitParamGenerateValue(consValue);

            Domain may = null, must = null;
            may = new IntegerDomain(value, Long.MAX_VALUE);
            must = new IntegerDomain(value, value);
            if (operator.equals("==")) {
                condata.addMayDomain(f, may);
                condata.addMustDomain(f, must);
            } else if (operator.equals("!=")) {
                Domain temp1 = Domain.inverse(must);
                Domain temp2 = Domain.inverse(may);

                condata.addMayDomain(f, temp1);
                condata.addMustDomain(f, temp2);
            } else if (operator.equals(">")) {

            } else if (operator.equals(">=")) {

            } else if (operator.equals("<")) {

            } else if (operator.equals("<=")) {

            }
        } else {// 处理if((a^b) == 2)的情况
            int i;
            char[] symbolArr = new char[32];
            for (i = 0; i < 32; i++) {
                symbolArr[i] = 'T';
            }
            // 添加到对应参数符号的位约束属性
            Iterator<SymbolFactor> it = allSymbol.iterator();
            while (it.hasNext()) {
                SymbolFactor sf = it.next();
                // 如果符号sf在前面存在约束，则保留其约束；否则，替换掉。
                if (!condata.getDomainsTable().containsKey(it)) {
                    condata.addBitConstraint(sf, String.valueOf(symbolArr));
                }
            }
        }
        return condata;
    }

    /**
     * 处理比较符是 ==,!=，以及 if(a)的情况，具体实现参照conditionDomainVisitor中的equalityexpression
     * 没有把求模运算包含进来，因为求模不是符号表达式结构中的一个类型
     * 
     * @param leftExpr
     * @param leftExpr2
     * @param operator
     * @param symboldomain
     * @param symbolExprConData
     * @return testcase if(a==0) if(a==0.0) if(a=='\0') if(0==a) if(0.0==a)
     *         if('\0'==a) if(a!=0) if(a!=0.0) if(a!='\0')
     * @throws Exception
     */
    private ConditionData CalculateEqualityExpression(Expression leftExpr, Expression rightExpr, String operator, ConditionData symbolExprConData, SymbolDomainSet symboldomain) throws Exception {
        // TODO Auto-generated method stub
        ConditionData condata = symbolExprConData;
        Expression rightvalue = rightExpr, leftvalue = leftExpr;

        if (operator == null || operator.equals("")) {
            // if(a) if(-a)
            condata = CalculatePrimariyExpression(leftvalue, symbolExprConData, symboldomain);
            return condata;
        }
        // 处理位运算符号& add by Yaoweichang
        if (leftvalue.getTerms().get(0).getOperator() == "&") {
            condata = CalculateAndExpression(leftvalue, rightvalue, operator, symbolExprConData, symboldomain);
            return condata;
        }
        // 处理位运算符号| add by Yaoweichang
        if (leftvalue.getTerms().get(0).getOperator() == "|") {
            condata = CalculateInclusiveORExpression(leftvalue, rightvalue, operator, symbolExprConData, symboldomain);
            return condata;
        }
        // 处理位运算符号^ add by Yaoweichang
        if (leftvalue.getTerms().get(0).getOperator() == "^") {
            condata = CalculateExclusiveORExpression(leftvalue, rightvalue, operator, symbolExprConData, symboldomain);
            return condata;
        }
        Expression exp = leftvalue.sub(rightvalue);
        if (exp.getSingleFactor() instanceof NumberFactor) {
            NumberFactor f = (NumberFactor) exp.getSingleFactor();
            Domain may = null, must = null;
            SymbolFactor s = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
            if ((f.getDoubleValue() == 0 && operator.equals("==")) || (f.getDoubleValue() != 0 && operator.equals("!="))) {
                may = IntegerDomain.getFullDomain();
                must = IntegerDomain.getFullDomain();
                condata.addMayDomain(s, may);
                condata.addMustDomain(s, must);
            } else {
                may = IntegerDomain.getEmptyDomain();
                must = IntegerDomain.getEmptyDomain();
                condata.addMayDomain(s, may);
                condata.addMustDomain(s, must);
            }
            return condata;
        }
        for (Term t : exp.getTerms()) {
            Factor f = t.getSingleFactor();
            if (f instanceof SymbolFactor) {
                SymbolFactor s = (SymbolFactor) f;
                Expression temp = new Expression(s).sub(exp);
                Domain rightdomain = temp.getDomain(symboldomain);

                Domain leftdomain = s.getDomainWithoutNull(symboldomain);

                if (rightdomain == null || leftdomain == null)
                    throw new Exception("expression can't get domain from symboldomainset");

                CType type = s.getType();

                if (rightdomain != null) {
                    Domain may = null, must = null;
                    may = Domain.intersect(leftdomain, rightdomain, type);

                    if (rightdomain.isCanonical()) {
                        must = Domain.intersect(leftdomain, rightdomain, type);
                    } else {
                        must = Domain.getEmptyDomainFromType(s.getType());
                    }
                    if (operator.equals("==")) {
                        condata.addMayDomain(s, may);
                        condata.addMustDomain(s, must);
                    } else {
                        Domain temp1 = Domain.inverse(must);
                        Domain temp2 = Domain.inverse(may);

                        condata.addMayDomain(s, temp1);
                        condata.addMustDomain(s, temp2);
                    }
                }
                continue;
            }
            // 处理系数为-1情况
            f = t.getMinusSingleFactor();
            if (f instanceof SymbolFactor) {
                SymbolFactor s = (SymbolFactor) f;
                Expression temp = new Expression(s).add(exp);
                Domain rightdomain = temp.getDomain(symboldomain);
                Domain leftdomain = s.getDomainWithoutNull(symboldomain);
                if (rightdomain == null || leftdomain == null)
                    throw new Exception("expression can't get domain from symboldomainset");

                CType type = s.getType();

                if (rightdomain != null) {
                    Domain may = null, must = null;
                    may = Domain.intersect(leftdomain, rightdomain, type);
                    if (rightdomain.isCanonical()) {
                        must = Domain.intersect(leftdomain, rightdomain, type);
                    } else {
                        must = Domain.getEmptyDomainFromType(s.getType());
                    }
                    if (operator.equals("==")) {
                        condata.addMayDomain(s, may);
                        condata.addMustDomain(s, must);
                    } else {
                        condata.addMayDomain(s, Domain.inverse(must));
                        condata.addMustDomain(s, Domain.inverse(may));
                    }
                }
            }// added zxz
            f = t.getRatioSingleFactor();
            if (f instanceof SymbolFactor) {

                SymbolFactor s = (SymbolFactor) f;
                NumberFactor tratio = t.getRatio();// 获得系数
                double ratio = tratio.getDoubleValue();
                Expression temp = new Expression(t, true).sub(exp);// 得到剩余的表达式
                Domain rightdomain = temp.getDomain(symboldomain);

                if (rightdomain != null && !rightdomain.isUnknown()) {
                    CType type = s.getType();
                    Domain may = null, must = null;
                    Domain leftdomain = s.getDomainWithoutNull(symboldomain);

                    IntegerDomain integerdomain = null;
                    DoubleDomain doubledomain = null;

                    switch (Domain.getDomainTypeFromType(type)) {// 根据左边的符号的域类型设置
                        case INTEGER:

                            integerdomain = Domain.castToIntegerDomain(rightdomain);
                            if ((integerdomain.intervals.first().getMin() % ratio) != 0)// 例如
                                                                                        // 3*a
                                                                                        // ==
                                                                                        // [-5,4]
                                                                                        // 不能整除，但是应该有向上和向下取整的操作
                                                                                        // 得到a的区间是[-1,1]
                                                                                        // 2012-10-27
                                integerdomain = IntegerDomain.getFullDomain();// modified by
                                                                              // baiyu,如果不能整除，则integerdomian为全集
                            // integerdomain = IntegerDomain.getEmptyDomain();//
                            // 如果不能整除，则integerdomain是全集
                            // 2012-10-17
                            // 整除的判断有问题判断有问题。
                            else
                                integerdomain = IntegerDomain.reduce(integerdomain, ratio);

                            may = Domain.intersect(leftdomain, integerdomain, type);
                            if (integerdomain.isCanonical()) {
                                must = Domain.intersect(leftdomain, integerdomain, type);
                            } else {
                                must = Domain.getEmptyDomainFromType(s.getType());
                            }
                            if (operator.equals("==")) {
                                condata.addMayDomain(s, may);
                                condata.addMustDomain(s, must);
                            } else {
                                Domain temp1 = Domain.inverse(must);
                                Domain temp2 = Domain.inverse(may);
                                condata.addMayDomain(s, temp1);
                                condata.addMustDomain(s, temp2);
                            }
                            break;
                        case DOUBLE:
                            doubledomain = Domain.castToDoubleDomain(rightdomain);
                            doubledomain = DoubleDomain.reduce(doubledomain, ratio);// 将区间按系数i的比例缩小
                            // double max1
                            // =doubledomain.getMax(),min1=doubledomain.getMin();
                            may = Domain.intersect(leftdomain, doubledomain, type);
                            if (doubledomain.isCanonical()) {
                                must = Domain.intersect(leftdomain, doubledomain, type);
                            } else {
                                must = Domain.getEmptyDomainFromType(s.getType());
                            }
                            if (operator.equals("==")) {
                                condata.addMayDomain(s, may);
                                condata.addMustDomain(s, must);
                            } else {
                                Domain temp1 = Domain.inverse(must);
                                Domain temp2 = Domain.inverse(may);
                                condata.addMayDomain(s, temp1);
                                condata.addMustDomain(s, temp2);
                            }
                            break;
                    }
                }
            }
        }
        return condata;
    }

    /**
     * 处理 if(a) if(-a) if (1) if(0)的情况
     * 
     * @param leftvalue
     * @param symbolExprConData
     * @param symboldomain
     * @return
     */
    private ConditionData CalculatePrimariyExpression(Expression leftvalue, ConditionData symbolExprConData, SymbolDomainSet symboldomain) {
        // TODO Auto-generated method stub
        Expression exp = leftvalue;
        ConditionData condata = symbolExprConData;
        if (exp == null) {
            return null;
        }
        if (exp.getSingleFactor() instanceof NumberFactor) {
            NumberFactor f = (NumberFactor) exp.getSingleFactor();
            Domain may = null, must = null;
            SymbolFactor s = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
            if (f.getDoubleValue() == 0) {
                may = IntegerDomain.getEmptyDomain();
                must = IntegerDomain.getEmptyDomain();
                condata.addMayDomain(s, may);
                condata.addMustDomain(s, must);
            } else {
                may = IntegerDomain.getFullDomain();
                must = IntegerDomain.getFullDomain();
                condata.addMayDomain(s, may);
                condata.addMustDomain(s, must);
            }
            return condata;
        }

        for (Term t : exp.getTerms()) {
            Factor f = t.getSingleFactor();
            if (f instanceof SymbolFactor) {
                SymbolFactor s = (SymbolFactor) f;
                Expression temp = new Expression(s).sub(exp);
                Domain rightdomain = temp.getDomain(symboldomain);
                Domain leftdomain = s.getDomainWithoutNull(symboldomain);
                CType type = s.getType();

                if (rightdomain != null) {
                    Domain may = null, must = null;
                    may = Domain.intersect(leftdomain, rightdomain, type);
                    if (rightdomain.isCanonical()) {
                        must = Domain.intersect(leftdomain, rightdomain, type);
                    } else {
                        must = Domain.getEmptyDomainFromType(type);
                    }
                    // zys:2010.4.24 if(p)的意义为p!=0
                    if (temp.toString().equals("0")) {
                        may = Domain.inverse(may);
                        must = Domain.inverse(must);
                    }
                    condata.addMayDomain(s, may);
                    condata.addMustDomain(s, must);
                }
            }
            // 处理系数为-1情况
            f = t.getMinusSingleFactor();
            if (f instanceof SymbolFactor) {
                SymbolFactor s = (SymbolFactor) f;
                Expression temp = new Expression(s).add(exp);
                Domain rightdomain = temp.getDomain(symboldomain);
                Domain leftdomain = s.getDomainWithoutNull(symboldomain);
                CType type = s.getType();

                if (rightdomain != null) {
                    Domain may = null, must = null;
                    may = Domain.intersect(leftdomain, rightdomain, type);
                    if (rightdomain.isCanonical()) {
                        must = Domain.intersect(leftdomain, rightdomain, type);
                    } else {
                        must = Domain.getEmptyDomainFromType(type);
                    }
                    condata.addMayDomain(s, may);
                    condata.addMustDomain(s, must);
                }
            }
        }
        return condata;
    }

    public boolean isConstrainsIsValid() {
        return constrainsIsValid;
    }

    public void setConstrainsIsValid(boolean constrainsIsValid) {
        this.constrainsIsValid = constrainsIsValid;
    }

    public SymbolDomainSet getInitSymbolDomainSet() {
        return initSymbolDomainSet;
    }

    public void setInitSymbolDomainSet(SymbolDomainSet initSymbolDomainSet) {
        this.initSymbolDomainSet = initSymbolDomainSet;
    }

    /**
     * 根据位运算约束形式生成用例值
     * 
     * @param consValue 符号的约束表示
     * @return
     *         created by Yaoweichang on 2015-04-17 下午4:36:18
     */
    public int BitParamGenerateValue(String consValue) {
        int i, pos;
        char[] numberArr = new char[32];
        for (i = 0, pos = 0; i < numberArr.length; i++, pos++) {
            if (consValue.charAt(pos) == 'X')// 约束矛盾，不存在满足当前约束的用例值，则返回-1
                return -1;
            if (consValue.charAt(pos) == 'T')
                numberArr[i] = '0';
            else {
                numberArr[i] = consValue.charAt(pos);
            }
        }

        // 将二进制字符串转化为整数
        int max = numberArr.length;
        int result = 0;
        for (i = max - 1; i >= 0; i--) {
            char c = numberArr[i];
            int algorism = c - '0';
            result += Math.pow(2, i) * algorism;
        }
        return result;
    }
}
