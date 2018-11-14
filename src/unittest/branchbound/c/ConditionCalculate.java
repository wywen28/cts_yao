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
 * ����������Ҫ���࣬������߼���ConditionDomainVisitor.java���ƣ���ͬ����û���÷�����ģʽ
 * ע�⣺��Ҫ֧�ֲ�ͬ�ĸ���׼�����ͷ�֧���ǵ��������㷽����һ���ģ�MCDC���в�ͬ����Ҫ�Ժ����֧��
 * 2014��12��4�� <br>
 * �����Լ���Ƿ��ű��ʽ�����ű��ʽ�� �ṹ��
 * SymbolExpression = LogicExpression | ~logicExpression
 * LogicExpression = RelationExpression R LogicExpression R={&&,||}
 * RelationExpression = Expression R Expression R={>,<,==,>=,<=,!=}
 * Expression = Term....
 * Term ...
 * Power ...
 * Factor...
 * 
 * @author xiongwei
 *         ���б�Ҫ �����Ա�ɳ�����
 *         * ���շ��ű��ʽ��˳�����ν�������ļ��㡣<br>
 *         �Է��ű��ʽ�����е�ÿһ�����Ž���˳�εļ��㣬�����ݷ�֧���� �١���������ķ���<br>
 * 
 * @param �����{ &lt;���ţ����� &gt;}
 * 
 * @return ����һ����������õ�������ֵ<br>
 *         ������������֮�������symboldomainAfterCalculate�����������㷢��ì�ܣ�
 *         ��ô����֮���symboldomainAfterCalculate�л���������empty�ķ��ţ�
 *         ����ConditionCalculate.constrainsIsValid��ʶ���Ϊfalse����ʶ������Լ����
 */
public class ConditionCalculate {

    private ConstraintExtractor constrains = new ConstraintExtractor();
    private SymbolDomainSet initSymbolDomainSet = null;
    // ���εķ���ȡֵ�Ƿ�����Լ����ʾ��true��ʾ����Լ����false ��ʾ������
    private boolean constrainsIsValid = true;
    private SymbolDomainSet symbolDomainSetAfterConditionCalculate = null;
    private Hashtable<SymbolFactor, Domain> tmptable;

    public ConditionCalculate(ConstraintExtractor constrains, SymbolDomainSet symbolDomainSet) {
        // TODO Auto-generated constructor stub
        this.constrains = constrains;
        this.initSymbolDomainSet = symbolDomainSet;
    }

    /**
     * ���շ��ű��ʽ��˳�����ν�������ļ��㡣
     * 
     * @return ����һ����������õ�������ֵ
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
            // ���ʽ����ٷ�֧��־
            boolean predict = se.isTF();
            try {
                conDomainData = CalculateSymbolExpression(se, conDomainData, symboldomainAfterCalculate);
            } catch (Exception e) {
                // ���������쳣
                e.printStackTrace();
                continue;
            }

            // ����may must������ٷ�֧ �ķ�������仯//switch��α�ʾ��Ӧ������symbolExpression�Ľṹ��Լ����ȡ��ʱ��֧��
            // �˴���Ҫ������ٷ�֧��λ�������Լ����ʽ��Ӱ�죬��һ����Ҫ����ٷ�֧Լ������
            if (predict == true) {
                // ��������֧
                SymbolDomainSet ds = conDomainData.getTrueMayDomainSet();
                symboldomainAfterCalculate = SymbolDomainSet.intersect(symboldomainAfterCalculate, ds);
            } else if (predict == false) {
                SymbolDomainSet ds = conDomainData.getFalseMayDomainSet(symboldomainAfterCalculate);
                symboldomainAfterCalculate = SymbolDomainSet.intersect(symboldomainAfterCalculate, ds);
            } else {

                // �������������switch_case ��δ֧��
            }

            if (symboldomainAfterCalculate.isContradict())
                break;
        }
        this.symbolDomainSetAfterConditionCalculate = symboldomainAfterCalculate;
        this.constrainsIsValid = !symboldomainAfterCalculate.isContradict();

        return symboldomainAfterCalculate;
    }

    /**
     * �����Ľ����������㣬ֱ������ì�ܻ��������ȶ�
     * �˹���Լ�����仯��ֻ������仯
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
     * ���� symbolExpression һ��symbolEXpression��һ��logicExpression���߼�������
     * 
     * @param se
     * @param symbolExprConData
     * @param symboldomain
     * @return conditiondata ��ʱ Ӧ����SymbolExpression�ķ���ֵ�����߼����
     * @throws Exception
     */
    private ConditionData CalculateSymbolExpression(SymbolExpression se, ConditionData symbolExprConData, SymbolDomainSet symboldomain) throws Exception {
        LogicalExpression logicExpression = se.getLogicalExpression();
        symbolExprConData = calculateLogicalExpression(logicExpression, symbolExprConData, symboldomain);
        return symbolExprConData;
    }

    /**
     * ����LogicExpression logicExpression�ɶ��relationExpressionͨ���߼������&& ||����������<br>
     * ���δ���ÿһ��relationExpression��ÿ����һ��relationExpression֮�󣬾͸�����֮ǰ���߼����������<br>
     * ����condata�ĺϲ��㷨
     * LogicExpression ������ RelationExpression LogicExpression LogicNotExpression���
     * 
     * @param le
     * @param symbolExprConData
     * @param symboldomain
     * @return conditiondata ��ʱ Ӧ�øĳ� LogicExpression �ķ���ֵ�����߼����
     * @throws Exception
     */
    private ConditionData calculateLogicalExpression(LogicalExpression le, ConditionData symbolExprConData, SymbolDomainSet symboldomain) throws Exception {
        List<LRExpression> LRExprs = le.getExpressions();
        List<String> logicOperators = le.getOperators();

        LRExpression firstRE = LRExprs.get(0);
        ConditionData leftREconData = new ConditionData();
        // �����׸�LRExpression��condata ��֮�����߼�����������ӵ�LRExpression �ٸ��ݲ�ͬ���߼����������ͬ�Ĵ���
        leftREconData = calculateLRExpression(firstRE, symbolExprConData, symboldomain);
        ConditionData nextREconData = new ConditionData();

        for (int i = 1; i <= logicOperators.size(); i++) {
            LRExpression nextRE = LRExprs.get(i);
            nextREconData = calculateLRExpression(nextRE, nextREconData, symboldomain);
            String nextLogicOperator = logicOperators.get(i - 1);
            // ����left��right���߼�������������������
            if (nextLogicOperator.equals("&&"))
                leftREconData = symbolExprConData.calLogicalAndExpression(leftREconData, nextREconData, symboldomain);
            else if (nextLogicOperator.equals("||")) {
                leftREconData = symbolExprConData.calLogicalOrExpression(leftREconData, nextREconData, symboldomain);
            } else {
                // �����߼���������δ����
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
     * ����LRExpression ������ logicExpression ����
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
        // ���ڳ������û�з�����ֻ����instanceof���ж���
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
     * ���� !(LogicalExpression )
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
        // �õ����
        Expression leftExpr = RE.getLeftvalue();
        Expression rightExpr = RE.getRightvalue();
        String operator = RE.getOperator();
        if (operator == null) {
            // û�бȽϷ������ ����if(a) if(a+b) if(-a)
            symbolExprConData = CalculateNoRelationExpression(leftExpr, symbolExprConData, symboldomain);
        } else if (operator.equals(">") || operator.equals("<") || operator.equals("<=") || operator.equals(">=")) {
            // ���� >,<,>=,<=
            symbolExprConData = CalculateGreaterOrLessExpression(leftExpr, rightExpr, operator, symbolExprConData, symboldomain);
        } else if (operator.equals("==") || operator.equals("!=")) {
            // ���� == !=
            symbolExprConData = CalculateEqualityExpression(leftExpr, rightExpr, operator, symbolExprConData, symboldomain);
        } else if (operator.equals("!")) {
            // ���� !Expr ��logicNot�д���

        } else {
            throw new Exception("relationOperator not handled");
        }
        return symbolExprConData;
    }

    /**
     * ���������ڵ���û�ȽϷ������ ���� if(a+b) ��ʱֻ��leftExpr ��a+b��<br>
     * �߼����� if(a+b!=0)������ ����У� if(a) if(-a) if(3*a)
     * 
     * @param leftExpr
     * @param symbolExprConData
     * @param symboldomain
     * @return
     */
    private ConditionData CalculateNoRelationExpression(Expression leftExpr, ConditionData symbolExprConData, SymbolDomainSet symboldomain) {
        Expression exp = leftExpr;
        ConditionData condata = symbolExprConData;
        // �����漰��λ�������Rʱ����Լ�����ʽ(a R b)�ȼ�Ϊ((a R b) != 0) add by Yaoweichang
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

        // if�е�ʽ���������ֵ��if(R)���Ҳ�Ϊ��������Ϊȫ�� ��RΪ0��ô��Ϊȫ��
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
        // ������ if(a+b) if(3a) if(-a) if(a)�����
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
                    // 2012-10-16 ���ϵ��Ϊ1 may=left-right; must = left-(~C.right)v!=[min,max] E
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
            // ����ϵ��Ϊ-1���
            f = t.getMinusSingleFactor();
            if (f instanceof SymbolFactor) {
                SymbolFactor s = (SymbolFactor) f;
                Expression temp = new Expression(s).add(exp);//
                Domain rightdomain = temp.getDomain(symboldomain);
                Domain leftdomain = s.getDomainWithoutNull(symboldomain);
                CType type = s.getType();

                if (rightdomain != null) {
                    Domain may = null, must = null;
                    // may = Domain.intersect(leftdomain, rightdomain, type);//�ᵼ�����֧��ʱ�����˼�����
                    may = Domain.intersect(leftdomain, rightdomain, type);// �����ĵ��޸� 2012-10-17 by
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
                NumberFactor tratio = t.getRatio();// ��ò�Ϊ��1��ϵ��
                double ratio = tratio.getDoubleValue();
                Expression temp = new Expression(t, true).sub(exp);// �õ�ʣ��ı��ʽ
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
                            // ���ж��ܷ�����
                            if ((rigthdomainTointeger.intervals.first().getMin() % ratio) != 0)// ������
                                                                                               // (3a+b
                                                                                               // )��Ч��
                                                                                               // 3a!=-b
                                integerdomain = IntegerDomain.getFullDomain();// ���������������integerdomain��ȫ��
                                                                              // 2012-10-17
                                                                              // �������ж��������ж������⡣
                            else
                                integerdomain = IntegerDomain.reduce(rigthdomainTointeger, ratio);// ����������Ļ���Ҳ��Ҫ����ȡ��������
                                                                                                  // 2012-10-18
                            // ��������С��ע��������������������Ļ�������if(3a+2)�������a��may��Ӧ����ȫ��2012-6-21
                            // 2012-11-5 ������ ��Ҫ�� ��Ҫȡ�� ֮�������
                            // long max = integerdomain.getMax(), min = integerdomain.getMin();

                            // may=Domain.substract(leftdomain, integerdomain, type);//�����ĵ��޸�
                            // 2012-10-17 by zhangxuzhou
                            may = Domain.intersect(leftdomain, integerdomain, type);
                            if (integerdomain.isCanonical()) {
                                // may = Domain.intersect(leftdomain, integerdomain, type);
                                // must = Domain.intersect(leftdomain, integerdomain, type);
                                must = Domain.intersect(leftdomain, integerdomain, type);// �����ĵ��޸�
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
                            // may=Domain.substract(leftdomain, rightdomain, type);//�����ĵ��޸�
                            // 2012-10-17 by zhangxuzhou
                            if (doubledomain.isCanonical()) {
                                must = Domain.intersect(leftdomain, integerdomain, type);
                                // must=Domain.substract(leftdomain, rightdomain, type);//�����ĵ��޸�
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
     * ����ȽϷ��� >,<,>=,<=�����������ʵ�ֲ���conditionDomainVisitor�е�relationalExpression
     * �Ѳ�
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

        // �����漰��λ�������Rʱ����Լ�����ʽ����ʽΪ((a R b)operator c) add by Yaoweichang
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

        // ���ʽ�ǳ��������
        if (exp.getSingleFactor() instanceof NumberFactor) {
            NumberFactor f = (NumberFactor) exp.getSingleFactor();
            Domain may = null, must = null;
            SymbolFactor s = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"));
            if ((f.getDoubleValue() > 0 && operator.equals(">")) || // zys:�˴����ж�����Ҳ������
                    (f.getDoubleValue() >= 0 && operator.equals(">=")) || (f.getDoubleValue() < 0 && operator.equals("<")) || (f.getDoubleValue() <= 0 && operator.equals("<="))) {
                may = Domain.getFullDomainFromType(f.getType());
                must = Domain.getFullDomainFromType(f.getType());

                condata.addMayDomain(s, may);
                condata.addMustDomain(s, must);
            } else {// zys: ��һ��Ϊ����
                may = IntegerDomain.getEmptyDomain();
                must = IntegerDomain.getEmptyDomain();
                condata.addMayDomain(s, may);
                condata.addMustDomain(s, must);
            }
            return condata;
        }
        // chh 2010.12.8 if(m>n)m,n��δ֪ʱ�޷��жϡ�
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
            // ����ϵ��Ϊ-1�����
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
            // add by zxz ��������ϵ�������������3*i+n>12

            f = t.getRatioSingleFactor();
            if (f instanceof SymbolFactor) {
                SymbolFactor s = (SymbolFactor) f;
                NumberFactor tratio = t.getRatio();// ���ϵ��
                double ratio = tratio.getDoubleValue();
                Expression temp = new Expression(t, true).sub(exp);// �õ�ʣ��ı��ʽ
                Domain rightdomain = temp.getDomain(symboldomain);

                if (rightdomain != null && !rightdomain.isUnknown()) {
                    CType type = s.getType();
                    Domain may = null, must = null;
                    Domain leftdomain = s.getDomainWithoutNull(symboldomain);

                    IntegerDomain integerdomain = null;
                    DoubleDomain doubledomain = null;

                    switch (Domain.getDomainTypeFromType(type)) {// ������ߵķ��ŵ�����������
                        case INTEGER:
                            integerdomain = Domain.castToIntegerDomain(rightdomain);
                            if ((operator.equals(">") && ratio > 0)// ����֮��ȽϷ���>
                                                                   // 2012-10-18
                                    || (operator.equals("<") && ratio < 0)) {
                                integerdomain = IntegerDomain.reduceByComprator(integerdomain, ratio, ">");// �����䰴ϵ��i�ı�����С
                                                                                                           // ��С���ȡ������ȡ����
                                                                                                           // ���ϵ��֮��ıȽϷ�����
                                                                                                           // 2012-10-17
                                long max = integerdomain.getMax(), min = integerdomain.getMin();

                                may = Domain.intersect(leftdomain, new IntegerDomain(min + 1, Long.MAX_VALUE), type);

                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain(max != Long.MAX_VALUE ? max + 1 : max, Long.MAX_VALUE), type);
                                condata.addMustDomain(s, must);
                            } else if ((operator.equals(">=") && ratio > 0)// ����֮��ȽϷ���>=
                                    || (operator.equals("<=") && ratio < 0)) {
                                integerdomain = IntegerDomain.reduceByComprator(integerdomain, ratio, ">=");// �����䰴ϵ��i�ı�����С
                                                                                                            // ��С���ȡ������ȡ����
                                                                                                            // ���ϵ��֮��ıȽϷ�����
                                                                                                            // 2012-10-17
                                long max = integerdomain.getMax(), min = integerdomain.getMin();

                                may = Domain.intersect(leftdomain, new IntegerDomain((integerdomain.getMin()), Long.MAX_VALUE), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain((integerdomain.getMax()), Long.MAX_VALUE), type);
                                condata.addMustDomain(s, must);
                            } else if ((operator.equals("<") && ratio > 0)// ����֮��ȽϷ���<
                                    || (operator.equals(">") && ratio < 0)) {
                                integerdomain = IntegerDomain.reduceByComprator(integerdomain, ratio, "<");// �����䰴ϵ��i�ı�����С
                                                                                                           // ��С���ȡ������ȡ����
                                                                                                           // ���ϵ��֮��ıȽϷ�����
                                                                                                           // 2012-10-17
                                long max = integerdomain.getMax(), min = integerdomain.getMin();

                                may = Domain.intersect(leftdomain, new IntegerDomain(Long.MIN_VALUE, integerdomain.getMax() - 1), type);
                                condata.addMayDomain(s, may);
                                must = Domain.intersect(leftdomain, new IntegerDomain(Long.MIN_VALUE, min != Long.MIN_VALUE ? min - 1 : min), type);
                                condata.addMustDomain(s, must);
                            } else if ((operator.equals("<=") && ratio > 0)// ����֮��ȽϷ���<=
                                    || (operator.equals(">=") && ratio < 0)) {
                                integerdomain = IntegerDomain.reduceByComprator(integerdomain, ratio, "<=");// �����䰴ϵ��i�ı�����С
                                                                                                            // ��С���ȡ������ȡ����
                                                                                                            // ���ϵ��֮��ıȽϷ�����
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
                            doubledomain = DoubleDomain.reduce(doubledomain, ratio);// �����䰴ϵ��i�ı�����С
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
     * ����if((a&b) == 2)�������λ���������������Ҫ�������㷨���������������㣬
     * ����������ԭ�ȵ����伯����ʽ��ʾȡֵ���䡣
     * 
     * @param leftvalue
     * @param rightvalue
     * @param operator
     * @param symbolExprConData
     * @param symboldomain
     * @return
     *         created by Yaoweichang on 2015-04-17 ����4:32:45
     */
    private ConditionData CalculateAndExpression(Expression leftvalue, Expression rightvalue, String operator, ConditionData symbolExprConData, SymbolDomainSet symboldomain) {
        Expression exp = leftvalue;
        ConditionData condata = symbolExprConData;
        ArrayList<Power> allPowers = exp.getTerms().get(0).getPowers();
        HashSet<SymbolFactor> allSymbol = exp.getTerms().get(0).getAllSymbol();
        // ������ʽΪif((a&number)==2)�����������ps:��������ֻ��������������λ��������
        if (allPowers.size() != allSymbol.size() && allSymbol.size() != 0) {
            Power numPower = null;
            // ��ȡnumber��Power���ͱ��ʽ
            for (int i = 0; i < allPowers.size(); i++) {
                if (allPowers.get(i).isNumber()) {
                    numPower = allPowers.get(i);
                }
            }

            // ������a��number�͵�ʽ��ֵת��Ϊ���ڶ����������32λ�ַ�������ʽ
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
            // ��rightvalueת��Ϊ32λ�ַ������ʶ
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

            // ���з���a�ĳ��������
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
            // ��ӵ���Ӧ�������ŵ�λԼ������
            SymbolFactor f = allSymbol.iterator().next();
            f.setIsBitCompute(true);// ���÷�������Ϊ����λ����
            String consExpr = new String(symbolArr);

            if (operator.equals("!=")) {// �������Ϊ��=��Ҫת��λ����Լ����ʽ
                String expr = null;
                boolean stop = false; // �ٷ�֧ʱֻ����ȡ�����һλ��Լ��
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

            // ����������Ŷ�Ӧ��may��must��ֵ
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
        } else {// ����if((a&b) == 2)�����
            // ������a��number�͵�ʽ��ֵת��Ϊ���ڶ����������32λ�ַ�������ʽ
            char[] symbolArr = new char[32];
            char[] rightNumArr = new char[32];

            // ��rightvalueת��Ϊ32λ�ַ������ʶ
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

            // ���з���a�ĳ��������
            for (i = 0; i < 32; i++) {
                if (rightNumArr[i] == '1') {
                    symbolArr[i] = '1';
                } else {
                    symbolArr[i] = 'T';
                }
            }
            // ��ӵ���Ӧ�������ŵ�λԼ������
            Iterator<SymbolFactor> it = allSymbol.iterator();
            while (it.hasNext()) {
                SymbolFactor sf = it.next();
                // �������sf��ǰ�����Լ������ϲ���Լ��
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
     * ����if((a|b) == 2)�������λ���������������Ҫ�������㷨���������������㣬
     * ����������ԭ�ȵ����伯����ʽ��ʾȡֵ���䡣
     * 
     * @param leftvalue
     * @param symbolExprConData
     * @param symboldomain
     * @return
     *         created by Yaoweichang on 2015-04-17 ����4:33:45
     */
    private ConditionData CalculateInclusiveORExpression(Expression leftvalue, Expression rightvalue, String operator, ConditionData symbolExprConData, SymbolDomainSet symboldomain) {
        Expression exp = leftvalue;
        ConditionData condata = symbolExprConData;
        ArrayList<Power> allPowers = exp.getTerms().get(0).getPowers();
        HashSet<SymbolFactor> allSymbol = exp.getTerms().get(0).getAllSymbol();
        // ������ʽΪif((a&number)==2)���������,ps:��������ֻ��������������λ��������
        if (allPowers.size() != allSymbol.size() && allSymbol.size() != 0) {
            Power numPower = null;
            // ��ȡnumber��Power���ͱ��ʽ
            for (int i = 0; i < allPowers.size(); i++) {
                if (allPowers.get(i).isNumber()) {
                    numPower = allPowers.get(i);
                }
            }

            // ������a��number�͵�ʽ��ֵת��Ϊ���ڶ����������32λ�ַ�������ʽ
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
            // ��rightvalueת��Ϊ32λ�ַ������ʶ
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

            // ���з���a�ĳ��������
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
            // ��ӵ���Ӧ�������ŵ�λԼ������
            SymbolFactor f = allSymbol.iterator().next();
            f.setIsBitCompute(true);// ���÷�������Ϊ����λ����
            String consExpr = new String(symbolArr);
            if (operator.equals("!=")) {
                String expr = null;
                boolean stop = false; // �ٷ�֧ʱֻ����ȡ�����һλ��Լ��
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

            // ����������Ŷ�Ӧ��may��must��ֵ
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
        } else {// ����if((a|b) == 2)�����
            // ������a��number�͵�ʽ��ֵת��Ϊ���ڶ����������32λ�ַ�������ʽ
            char[] symbolArr = new char[32];
            char[] rightNumArr = new char[32];

            // ��rightvalueת��Ϊ32λ�ַ������ʶ
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

            // ���з��ŵĳ��������
            for (i = 0; i < 32; i++) {
                if (rightNumArr[i] == '0') {
                    symbolArr[i] = '0';
                } else {
                    symbolArr[i] = 'T';
                }
            }
            // ��ӵ���Ӧ�������ŵ�λԼ������
            Iterator<SymbolFactor> it = allSymbol.iterator();
            while (it.hasNext()) {
                SymbolFactor sf = it.next();
                // �������sf��ǰ�����Լ������ϲ���Լ��
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
     * ����if((a^b) == 2)�������λ���������������Ҫ�������㷨���������������㣬
     * ����������ԭ�ȵ����伯����ʽ��ʾȡֵ���䡣
     * 
     * @param leftvalue
     * @param symbolExprConData
     * @param symboldomain
     * @return
     *         created by Yaoweichang on 2015-04-17 ����4:36:45
     */
    private ConditionData CalculateExclusiveORExpression(Expression leftvalue, Expression rightvalue, String operator, ConditionData symbolExprConData, SymbolDomainSet symboldomain) {
        Expression exp = leftvalue;
        ConditionData condata = symbolExprConData;
        ArrayList<Power> allPowers = exp.getTerms().get(0).getPowers();
        HashSet<SymbolFactor> allSymbol = exp.getTerms().get(0).getAllSymbol();
        if (allPowers.size() != allSymbol.size() && allSymbol.size() != 0) {// ������ʽΪif((a&number) ==
                                                                            // 2)���������,ps:��������ֻ��������������λ��������
            // ��ȡ����a��number��Power���ͱ��ʽ
            Power numPower = allPowers.get(0);
            Power symbolPower = allPowers.get(1);
            System.out.println(symbolPower);
            // Factor sfactor = numPower.getSingleFactor();

            // ������a��number�͵�ʽ��ֵת��Ϊ���ڶ����������32λ�ַ�������ʽ
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
            // ��rightvalueת��Ϊ32λ�ַ������ʶ
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

            // ���з���a�ĳ��������
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
            // ��ӵ���Ӧ�������ŵ�λԼ������
            SymbolFactor f = allSymbol.iterator().next();
            f.setIsBitCompute(true);// ���÷�������Ϊ����λ����
            String consExpr = new String(symbolArr);
            if (operator.equals("!=")) {
                String expr = null;
                boolean stop = false; // �ٷ�֧ʱֻ����ȡ�����һλ��Լ��
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

            // ����������Ŷ�Ӧ��may��must��ֵ
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
        } else {// ����if((a^b) == 2)�����
            int i;
            char[] symbolArr = new char[32];
            for (i = 0; i < 32; i++) {
                symbolArr[i] = 'T';
            }
            // ��ӵ���Ӧ�������ŵ�λԼ������
            Iterator<SymbolFactor> it = allSymbol.iterator();
            while (it.hasNext()) {
                SymbolFactor sf = it.next();
                // �������sf��ǰ�����Լ����������Լ���������滻����
                if (!condata.getDomainsTable().containsKey(it)) {
                    condata.addBitConstraint(sf, String.valueOf(symbolArr));
                }
            }
        }
        return condata;
    }

    /**
     * ����ȽϷ��� ==,!=���Լ� if(a)�����������ʵ�ֲ���conditionDomainVisitor�е�equalityexpression
     * û�а���ģ���������������Ϊ��ģ���Ƿ��ű��ʽ�ṹ�е�һ������
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
        // ����λ�������& add by Yaoweichang
        if (leftvalue.getTerms().get(0).getOperator() == "&") {
            condata = CalculateAndExpression(leftvalue, rightvalue, operator, symbolExprConData, symboldomain);
            return condata;
        }
        // ����λ�������| add by Yaoweichang
        if (leftvalue.getTerms().get(0).getOperator() == "|") {
            condata = CalculateInclusiveORExpression(leftvalue, rightvalue, operator, symbolExprConData, symboldomain);
            return condata;
        }
        // ����λ�������^ add by Yaoweichang
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
            // ����ϵ��Ϊ-1���
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
                NumberFactor tratio = t.getRatio();// ���ϵ��
                double ratio = tratio.getDoubleValue();
                Expression temp = new Expression(t, true).sub(exp);// �õ�ʣ��ı��ʽ
                Domain rightdomain = temp.getDomain(symboldomain);

                if (rightdomain != null && !rightdomain.isUnknown()) {
                    CType type = s.getType();
                    Domain may = null, must = null;
                    Domain leftdomain = s.getDomainWithoutNull(symboldomain);

                    IntegerDomain integerdomain = null;
                    DoubleDomain doubledomain = null;

                    switch (Domain.getDomainTypeFromType(type)) {// ������ߵķ��ŵ�����������
                        case INTEGER:

                            integerdomain = Domain.castToIntegerDomain(rightdomain);
                            if ((integerdomain.intervals.first().getMin() % ratio) != 0)// ����
                                                                                        // 3*a
                                                                                        // ==
                                                                                        // [-5,4]
                                                                                        // ��������������Ӧ�������Ϻ�����ȡ���Ĳ���
                                                                                        // �õ�a��������[-1,1]
                                                                                        // 2012-10-27
                                integerdomain = IntegerDomain.getFullDomain();// modified by
                                                                              // baiyu,���������������integerdomianΪȫ��
                            // integerdomain = IntegerDomain.getEmptyDomain();//
                            // ���������������integerdomain��ȫ��
                            // 2012-10-17
                            // �������ж��������ж������⡣
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
                            doubledomain = DoubleDomain.reduce(doubledomain, ratio);// �����䰴ϵ��i�ı�����С
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
     * ���� if(a) if(-a) if (1) if(0)�����
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
                    // zys:2010.4.24 if(p)������Ϊp!=0
                    if (temp.toString().equals("0")) {
                        may = Domain.inverse(may);
                        must = Domain.inverse(must);
                    }
                    condata.addMayDomain(s, may);
                    condata.addMustDomain(s, must);
                }
            }
            // ����ϵ��Ϊ-1���
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
     * ����λ����Լ����ʽ��������ֵ
     * 
     * @param consValue ���ŵ�Լ����ʾ
     * @return
     *         created by Yaoweichang on 2015-04-17 ����4:36:18
     */
    public int BitParamGenerateValue(String consValue) {
        int i, pos;
        char[] numberArr = new char[32];
        for (i = 0, pos = 0; i < numberArr.length; i++, pos++) {
            if (consValue.charAt(pos) == 'X')// Լ��ì�ܣ����������㵱ǰԼ��������ֵ���򷵻�-1
                return -1;
            if (consValue.charAt(pos) == 'T')
                numberArr[i] = '0';
            else {
                numberArr[i] = consValue.charAt(pos);
            }
        }

        // ���������ַ���ת��Ϊ����
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
