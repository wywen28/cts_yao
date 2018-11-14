package softtest.domain.c.analysis;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import softtest.ast.c.CParserVisitorAdapter;
import softtest.ast.c.Node;
import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.domain.c.interval.BitConstraintDomain;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.PointerDomain;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_AllocType;
import unittest.branchbound.c.BranchboundDomainVisitor;
import unittest.path.analysis.variabledomain.PointerVariableDomain;
import unittest.path.analysis.variabledomain.PointerVariableDomain.PointerState;
import unittest.teststub.generate.PathConstraintExtract.PathConditionVisitor;


/** �����޶��� */
public class ConditionData implements Cloneable {

    /** �����޶��� */
    class ConditionDomains {
        /** ������ */
        Domain may;
        /** �϶��� */
        Domain must;
        /** ����λ����ı���Լ�����ʽ add by Yaoweichang */
        String bitConstraint;
    }

    // add by tangrong tmp
    private PointerVariableDomain equalLeft = null;
    private PointerVariableDomain equalRight = null;
    private PointerVariableDomain equalLeftBak = null;
    private PointerVariableDomain equalRightBak = null;
    private boolean isEqual;
    private boolean isBitConstraint = false;


    /**
     * @author xujiaoxian
     *         2013-01-09
     */
    public Object clone() {
        ConditionData condata = null;
        try {
            condata = (ConditionData) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        condata.domainstable = (Hashtable<SymbolFactor, ConditionDomains>) domainstable.clone();
        if (equalLeft != null)
            condata.equalLeft = (PointerVariableDomain) equalLeft.clone();
        if (equalRight != null)
            condata.equalRight = (PointerVariableDomain) equalRight.clone();
        if (equalLeftBak != null)
            condata.equalLeftBak = (PointerVariableDomain) equalLeftBak.clone();
        if (equalRightBak != null)
            condata.equalRightBak = (PointerVariableDomain) equalRightBak.clone();
        return condata;
    }

    public ConditionData(VexNode currentvex) {
        this.currentvex = currentvex;
    }

    public ConditionData() {}

    // /**�������캯�� */
    // public ConditionData(ConditionData oldcondata) {
    // this.currentvex=oldcondata.currentvex;
    // Hashtable<SymbolFactor, ConditionDomains> oldtable=oldcondata.getDomainsTable();
    // for(SymbolFactor s : oldtable.keySet())
    // {
    // ConditionDomains condoms=oldtable.get(s);
    // domainstable.put(s, condoms);
    // }
    // }

    public void addPointerEqualConstraint(PointerVariableDomain left, PointerVariableDomain right, boolean isEqual) {
        this.equalLeft = left;
        this.equalRight = right;
        this.isEqual = isEqual;
    }

    public void calculatePointerConstraint() {
        if (this.equalLeft != null) {
            if (this.isEqual) {
                // p == NULL || p == 0
                if (this.equalRight == null) {
                    if (this.equalLeft.getState().equals(PointerState.NOTNULL)) {
                        this.equalLeft.setState(PointerState.EMPTY);
                    } else if (this.equalLeft.getState().equals(PointerState.UNSURE)) {
                        this.equalLeft.setState(PointerState.NULL);
                    }
                } else {
                    // add by xuajiaoxian 2012-10-19
                    this.equalLeftBak = new PointerVariableDomain(this.equalLeft);
                    this.equalRightBak = new PointerVariableDomain(this.equalRight);
                    // end add by xujiaoxian
                    this.equalLeft.clearDumped();
                    this.equalRight.clearDumped();
                    // this.equalLeft.combine(equalRight);
                    this.equalLeft.combineEqualExp(equalRight);
                }
            } else { // not-equal
                if (this.equalRight == null) {
                    if (this.equalLeft.getState().equals(PointerState.NULL)) {
                        this.equalLeft.setState(PointerState.EMPTY);
                    } else if (this.equalLeft.getState().equals(PointerState.UNSURE)) {
                        this.equalLeft.setState(PointerState.NOTNULL);
                    }
                } else {
                    this.equalLeft.combineNotEqual(this.equalRight);
                }
            }
        }

    }

    public void calculateFalsePointerConstraint() {
        this.isEqual = !this.isEqual;
        // if(!this.isEqual){
        // this.equalLeft = this.equalLeftBak;
        // this.equalRight = this.equalRightBak;
        // }
        this.calculatePointerConstraint();
    }

    VexNode currentvex = null;

    public VexNode getCurrentVex() {
        return currentvex;
    }

    public void setCurrentVex(VexNode currentvex) {
        this.currentvex = currentvex;
    }

    /** �ӱ����������޶���Ĺ�ϣ�� */
    private Hashtable<SymbolFactor, ConditionDomains> domainstable = new Hashtable<SymbolFactor, ConditionDomains>();

    /** ���ù�ϣ�� */
    public void setDomainsTable(Hashtable<SymbolFactor, ConditionDomains> domainstable) {
        this.domainstable = domainstable;
    }

    /** ��ù�ϣ�� */
    public Hashtable<SymbolFactor, ConditionDomains> getDomainsTable() {
        return domainstable;
    }

    /** �ж�ָ�������Ƿ��ڵ�ǰ�����޶����� */
    public boolean isSymbolContained(SymbolFactor s) {
        return domainstable.containsKey(s);
    }

    /**
     * ���ò���λ�������s��Լ�����ʽ
     * 
     * @param s
     * @param constraint
     *        created by Yaoweichang on 2015-04-17 ����4:12:53
     */
    public void addBitConstraint(SymbolFactor s, String constraint) {
        ConditionDomains domains = null;
        if (domainstable.containsKey(s)) {
            domains = domainstable.get(s);
        } else {
            domains = new ConditionDomains();
            domainstable.put(s, domains);
        }
        domains.bitConstraint = constraint;
        setBitConstraint(true);
    }

    /**
     * ��÷��ŵ�λ����Լ����ʽ
     * 
     * @param s Ҫ��ѯ�ķ���
     * @param constraint
     *        created by Yaoweichang on 2015-11-12 ����3:07:08
     */
    public String getBitConstraint(SymbolFactor s) {
        ConditionDomains domains = domainstable.get(s);
        if (domains != null) {
            return domains.bitConstraint;
        } else {
            return null;
        }
    }

    /** ���÷���s�Ŀ����� */
    public void addMayDomain(SymbolFactor s, Domain domain) {
        ConditionDomains domains = null;
        if (domainstable.containsKey(s)) {
            domains = domainstable.get(s);
        } else {
            domains = new ConditionDomains();
            domainstable.put(s, domains);
        }
        domains.may = domain;
    }

    /** ���÷���s�Ŀ϶��� */
    public void addMustDomain(SymbolFactor s, Domain domain) {
        ConditionDomains domains = null;
        if (domainstable.containsKey(s)) {
            domains = domainstable.get(s);
        } else {
            domains = new ConditionDomains();
            domainstable.put(s, domains);
        }
        domains.must = domain;
    }

    /** ��÷���s�Ŀ����� */
    public Domain getMayDomain(SymbolFactor s) {
        if (s == null)
            return null;
        ConditionDomains domains = domainstable.get(s);
        if (domains == null) {
            return null;
        }
        return domains.may;
    }

    /** ��÷���s�Ŀ϶��� */
    public Domain getMustDomain(SymbolFactor s) {
        ConditionDomains domains = domainstable.get(s);
        if (domains == null) {
            return null;
        }
        return domains.must;
    }

    /** �����ϣ�� */
    public void clearDomains() {
        domainstable.clear();
    }

    /** �������Ϊ��ǰ���µ������޶������� */
    public SymbolDomainSet getTrueMayDomainSet() {
        SymbolDomainSet ret = new SymbolDomainSet();
        Set<Map.Entry<SymbolFactor, ConditionDomains>> entry = domainstable.entrySet();
        Iterator<Map.Entry<SymbolFactor, ConditionDomains>> i = entry.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, ConditionDomains> e = i.next();
            SymbolFactor s = e.getKey();
            if ((s.getMark() & 2) == 2) {// ����λ���� add by Yaoweichang
                if (domainstable.get(s).bitConstraint != null) {
                    BitConstraintDomain consDomain = new BitConstraintDomain(domainstable.get(s).bitConstraint);
                    ret.addBitConsDomain(s, consDomain);
                }
            }
            if (e.getValue().may != null) {
                ret.addDomain(e.getKey(), e.getValue().may);
            }
        }
        return ret;
    }

    /** �������Ϊ��ǰ���µ������޶������� */
    public SymbolDomainSet getFalseMayDomainSet() {
        SymbolDomainSet ret = new SymbolDomainSet();
        Set<Map.Entry<SymbolFactor, ConditionDomains>> entry = domainstable.entrySet();
        Iterator<Map.Entry<SymbolFactor, ConditionDomains>> i = entry.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, ConditionDomains> e = i.next();
            SymbolFactor s = e.getKey();
            if (e.getValue().must != null) {
                Domain a = s.getDomainWithoutNull(currentvex.getSymDomainset());
                CType type = s.getType();
                Domain b = e.getValue().must;
                Domain result = Domain.substract(a, b, type);
                if (result instanceof PointerDomain) {
                    if (((PointerDomain) result).getValue().toString().equals("NOTNULL") && ((PointerDomain) result).Type.isEmpty())
                        ((PointerDomain) result).Type.add(CType_AllocType.NotNull);
                    else if (((PointerDomain) result).getValue().toString().equals("NULL") && ((PointerDomain) result).Type.isEmpty())
                        ((PointerDomain) result).Type.add(CType_AllocType.Null);
                }
                // ��ָ����������ж�
                // add by zhouhb 2010/7/19
                if (a instanceof PointerDomain && ((PointerDomain) result).allocRange.isEmpty()) {
                    ((PointerDomain) result).allocRange = new IntegerDomain(0, 0);
                }
                ret.addDomain(s, result);
            }
        }
        return ret;
    }

    /**
     * �������Ϊ��ǰ���µ������޶�������
     * ָ��sd�����Ǵ�vexnode����
     * */
    public SymbolDomainSet getFalseMayDomainSet(SymbolDomainSet ds) {
        SymbolDomainSet ret = new SymbolDomainSet();
        Set<Map.Entry<SymbolFactor, ConditionDomains>> entry = domainstable.entrySet();
        Iterator<Map.Entry<SymbolFactor, ConditionDomains>> iter = entry.iterator();
        while (iter.hasNext()) {
            Map.Entry<SymbolFactor, ConditionDomains> e = iter.next();
            SymbolFactor s = e.getKey();

            if (s.getIsBitCompute()) {// ����λ���㣬����Ϊ�٣�Ҫ�任��Լ����ʽ add by Yaoweichang
                String consExpr = domainstable.get(s).bitConstraint;
                if (consExpr != null) {
                    String expr = null;
                    boolean stop = false; // �ٷ�֧ʱֻ����ȡ�����һλ��Լ��
                    for (int i = 0; i < consExpr.length() && !stop; i++) {
                        if (consExpr.charAt(i) != 'T') {
                            char c = consExpr.charAt(i);
                            if (c == '0')
                                expr = consExpr.replaceFirst("0", "1");
                            else
                                expr = consExpr.replaceFirst("1", "0");
                            stop = true;
                        }
                    }
                    BitConstraintDomain consDomain = new BitConstraintDomain(expr);
                    ret.addBitConsDomain(s, consDomain);
                }
            } else {
                if (e.getValue().must != null) {
                    Domain a = s.getDomainWithoutNull(ds);
                    CType type = s.getType();
                    Domain b = e.getValue().must;
                    Domain result = Domain.substract(a, b, type);
                    if (result instanceof PointerDomain) {
                        if (((PointerDomain) result).getValue().toString().equals("NOTNULL") && ((PointerDomain) result).Type.isEmpty())
                            ((PointerDomain) result).Type.add(CType_AllocType.NotNull);
                        else if (((PointerDomain) result).getValue().toString().equals("NULL") && ((PointerDomain) result).Type.isEmpty())
                            ((PointerDomain) result).Type.add(CType_AllocType.Null);
                    }
                    // ��ָ����������ж�
                    // add by zhouhb 2010/7/19
                    if (a instanceof PointerDomain && ((PointerDomain) result).allocRange.isEmpty()) {
                        ((PointerDomain) result).allocRange = new IntegerDomain(0, 0);
                    }
                    ret.addDomain(s, result);
                }
            }
        }

        return ret;
    }

    /** �������Ϊ��ǰ���µ������޶��϶��� */
    public SymbolDomainSet getTrueMustDomainSet() {
        SymbolDomainSet ret = new SymbolDomainSet();
        Set<Map.Entry<SymbolFactor, ConditionDomains>> entry = domainstable.entrySet();
        Iterator<Map.Entry<SymbolFactor, ConditionDomains>> i = entry.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, ConditionDomains> e = i.next();
            if (e.getValue().must != null) {
                ret.addDomain(e.getKey(), e.getValue().must);
            }
        }
        return ret;
    }

    /** �������Ϊ��ǰ���µ������޶��϶��� */
    public SymbolDomainSet getFalseMustDomainSet() {
        SymbolDomainSet ret = new SymbolDomainSet();
        Set<Map.Entry<SymbolFactor, ConditionDomains>> entry = domainstable.entrySet();
        Iterator<Map.Entry<SymbolFactor, ConditionDomains>> i = entry.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, ConditionDomains> e = i.next();
            SymbolFactor s = e.getKey();
            if (e.getValue().may != null) {
                Domain a = s.getDomainWithoutNull(currentvex.getSymDomainset());
                CType type = s.getType();
                Domain b = e.getValue().may;
                Domain result = Domain.substract(a, b, type);
                if (result instanceof PointerDomain) {
                    if (((PointerDomain) result).getValue().toString().equals("NOTNULL") && ((PointerDomain) result).Type.isEmpty())
                        ((PointerDomain) result).Type.add(CType_AllocType.NotNull);
                    else if (((PointerDomain) result).getValue().toString().equals("NULL") && ((PointerDomain) result).Type.isEmpty())
                        ((PointerDomain) result).Type.add(CType_AllocType.Null);
                }
                // ��ָ����������ж�
                // add by zhouhb 2010/7/19
                if (a instanceof PointerDomain && ((PointerDomain) result).allocRange.isEmpty()) {
                    ((PointerDomain) result).allocRange = new IntegerDomain(0, 0);
                }
                ret.addDomain(s, result);
            }
        }
        return ret;
    }

    /**
     * �������Ϊ��ǰ���µ������޶��϶���
     * ָ��sd�����Ǵ�vexnode����
     */
    public SymbolDomainSet getFalseMustDomainSet(SymbolDomainSet sd) {
        SymbolDomainSet ret = new SymbolDomainSet();
        Set<Map.Entry<SymbolFactor, ConditionDomains>> entry = domainstable.entrySet();
        Iterator<Map.Entry<SymbolFactor, ConditionDomains>> i = entry.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, ConditionDomains> e = i.next();
            SymbolFactor s = e.getKey();
            if (e.getValue().may != null) {
                Domain a = s.getDomainWithoutNull(sd);
                CType type = s.getType();
                Domain b = e.getValue().may;
                Domain result = Domain.substract(a, b, type);
                if (result instanceof PointerDomain) {
                    if (((PointerDomain) result).getValue().toString().equals("NOTNULL") && ((PointerDomain) result).Type.isEmpty())
                        ((PointerDomain) result).Type.add(CType_AllocType.NotNull);
                    else if (((PointerDomain) result).getValue().toString().equals("NULL") && ((PointerDomain) result).Type.isEmpty())
                        ((PointerDomain) result).Type.add(CType_AllocType.Null);
                }
                // ��ָ����������ж�
                // add by zhouhb 2010/7/19
                if (a instanceof PointerDomain && ((PointerDomain) result).allocRange.isEmpty()) {
                    ((PointerDomain) result).allocRange = new IntegerDomain(0, 0);
                }
                ret.addDomain(s, result);
            }
        }
        return ret;
    }

    /** �ж������޶��������Ƿ�ì�� */
    public boolean isMayContradict() {
        SymbolDomainSet ds = getTrueMayDomainSet();
        return ds.isContradict();
    }

    /** �ж������޶��϶����Ƿ�ì�� */
    public boolean isMustContradict() {
        SymbolDomainSet ds = getTrueMustDomainSet();
        return ds.isContradict();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        Set<Map.Entry<SymbolFactor, ConditionDomains>> entryset = domainstable.entrySet();
        Iterator<Map.Entry<SymbolFactor, ConditionDomains>> i = entryset.iterator();
        SymbolFactor v = null;
        ConditionDomains d = null;
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, ConditionDomains> e = i.next();
            v = e.getKey();
            d = e.getValue();
            b.append("" + v.getSymbol() + ":{may:" + d.may + "; must:" + d.must + "; bitConstraint:" + d.bitConstraint + "}");
        }
        return b.toString();
    }

    private boolean isLogicallyTrueBut(SymbolDomainSet ds, SymbolFactor v) {
        SymbolFactor v1 = null;
        Domain d1 = null, d2 = null;
        Set<Map.Entry<SymbolFactor, Domain>> entryset = ds.getTable().entrySet();
        Iterator<Map.Entry<SymbolFactor, Domain>> i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v1 = e.getKey();
            d1 = e.getValue();
            if (v1 == v) {
                continue;
            }
            d2 = v1.getDomainWithoutNull(currentvex.getSymDomainset());
            if (d2.equals(d1)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ֱ��ָ��ds
     * 
     * @param must
     * @param v
     * @param ds
     * @return
     */
    private boolean isLogicallyTrueBut(SymbolDomainSet must, SymbolFactor v, SymbolDomainSet ds) {
        SymbolFactor v1 = null;
        Domain d1 = null, d2 = null;
        Set<Map.Entry<SymbolFactor, Domain>> entryset = must.getTable().entrySet();
        Iterator<Map.Entry<SymbolFactor, Domain>> i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v1 = e.getKey();
            d1 = e.getValue();
            if (v1 == v) {
                continue;
            }
            d2 = v1.getDomainWithoutNull(ds);
            if (d2.equals(d1)) {
                return true;
            }
        }
        return false;
    }

    private boolean isContradicBut(SymbolDomainSet ds, SymbolFactor v) {
        SymbolFactor v1 = null;
        Domain d1 = null;
        Set<Map.Entry<SymbolFactor, Domain>> entryset = ds.getTable().entrySet();
        Iterator<Map.Entry<SymbolFactor, Domain>> i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v1 = e.getKey();
            d1 = e.getValue();
            if (v1 == v) {
                continue;
            }
            if (Domain.isEmpty(d1)) {
                return true;
            }
        }
        return false;
    }

    /** �����е�����������Ϊ��Ӧ�������޶��϶��� */
    public void addMustDomain(SymbolDomainSet ds) {
        Set<Map.Entry<SymbolFactor, Domain>> entryset = ds.getTable().entrySet();
        Iterator<Map.Entry<SymbolFactor, Domain>> i = entryset.iterator();
        SymbolFactor v = null;
        Domain d = null;
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v = (SymbolFactor) e.getKey();
            d = e.getValue();
            addMustDomain(v, d);
        }
    }

    /** �����е�����������Ϊ��Ӧ�������޶������� */
    public void addMayDomain(SymbolDomainSet ds) {
        Set<Map.Entry<SymbolFactor, Domain>> entryset = ds.getTable().entrySet();
        Iterator<Map.Entry<SymbolFactor, Domain>> i = entryset.iterator();
        SymbolFactor v = null;
        Domain d = null;
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v = (SymbolFactor) e.getKey();
            d = e.getValue();
            addMayDomain(v, d);
        }
    }

    /**
     * �����߼�&&�������ұ��ʽ���漰��Լ���������䣬���ϲ����ֱ������ֵ�ͺ�λ�����ͷ��ţ�
     * 
     * @param leftdata ����ʽ
     * @param rightdata �ұ��ʽ
     * @return
     */
    public ConditionData calLogicalAndExpression(ConditionData leftdata, ConditionData rightdata) {
        ConditionData r = new ConditionData(currentvex);
        SymbolDomainSet may1 = leftdata.getTrueMayDomainSet();
        SymbolDomainSet may2 = rightdata.getTrueMayDomainSet();

        SymbolDomainSet must1 = leftdata.getTrueMustDomainSet();
        SymbolDomainSet must2 = rightdata.getTrueMustDomainSet();

        SymbolDomainSet may = new SymbolDomainSet();
        SymbolDomainSet must = new SymbolDomainSet();

        SymbolFactor v1 = null, v2 = null;
        Domain d1 = null, d2 = null;
        Set<Map.Entry<SymbolFactor, Domain>> entryset = may1.getTable().entrySet();
        Iterator<Map.Entry<SymbolFactor, Domain>> i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v1 = (SymbolFactor) e.getKey();
            d1 = e.getValue();
            // ��domainset�в���
            d2 = may2.getDomain(v1);
            CType type = v1.getType();
            if (d2 == null) {
                if (!isContradicBut(may2, v1)) {
                    Domain d = v1.getDomainWithoutNull(currentvex.getSymDomainset());
                    may.addDomain(v1, Domain.intersect(d, d1, type));
                } else {
                    // always FALSE, add EMPTY
                    may.addDomain(v1, Domain.getEmptyDomainFromType(type));
                }
            } else {
                // ȡintersect
                may.addDomain(v1, Domain.intersect(d1, d2, v1.getType()));
            }
        }

        // ��b����Щaû�г��ֵĲ��뵽table��
        entryset = may2.getTable().entrySet();
        i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v2 = e.getKey();
            d2 = e.getValue();
            // ��domainset�в���
            d1 = may1.getDomain(v2);
            CType type = v2.getType();
            if (d1 == null) {
                if (!isContradicBut(may1, v2)) {
                    Domain d = v2.getDomainWithoutNull(currentvex.getSymDomainset());
                    may.addDomain(v2, Domain.intersect(d, d2, type));
                } else {
                    // always FALSE, add EMPTY
                    may.addDomain(v2, Domain.getEmptyDomainFromType(type));
                }
            }
        }

        r.addMayDomain(may);
        // ����must
        entryset = must1.getTable().entrySet();
        i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v1 = (SymbolFactor) e.getKey();
            d1 = e.getValue();
            // ��domainset�в���
            d2 = must2.getDomain(v1);
            CType type = v1.getType();
            if (d2 == null) {
                if (isLogicallyTrueBut(must2, v1)) {
                    // ���棬ȡ��ǰȡ���������must
                    must.addDomain(v1, d1);
                } else {
                    must.addDomain(v1, Domain.getEmptyDomainFromType(type));
                }
            } else {
                // ȡintersect
                must.addDomain(v1, Domain.intersect(d1, d2, v1.getType()));
            }
        }

        // ��b����Щaû�г��ֵĲ��뵽table��
        entryset = must2.getTable().entrySet();
        i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v2 = e.getKey();
            d2 = e.getValue();
            // ��domainset�в���
            d1 = must1.getDomain(v2);
            if (d1 == null) {
                if (isLogicallyTrueBut(must1, v2)) {
                    // ���棬ȡ���������must
                    must.addDomain(v2, d2);
                } else {
                    must.addDomain(v2, Domain.getEmptyDomainFromType(v2.getType()));
                }
            } else {
                must.addDomain(v2, Domain.intersect(d1, d2, v2.getType()));
            }
        }

        r.addMustDomain(must);

        // ��λ����Լ���ϲ� add by Yaoweichang
        BitConstraintDomain b1 = null, b2 = null;
        Set<Map.Entry<SymbolFactor, BitConstraintDomain>> bitEntryset = may1.getConsTable().entrySet();
        Iterator<Entry<SymbolFactor, BitConstraintDomain>> i2 = bitEntryset.iterator();
        while (i2.hasNext()) {// ��a�г��ֵ����з���Լ������table��ͬʱ�ϲ�a��b�����ֵ�Լ��
            Map.Entry<SymbolFactor, BitConstraintDomain> e = i2.next();
            v1 = (SymbolFactor) e.getKey();
            b1 = e.getValue();
            b2 = may1.getBitConsDomain(v1);
            if (b2 == null)
                r.addBitConstraint(v1, b1.getConstraintDomain());
            else {
                r.addBitConstraint(v1, BitConstraintDomain.intersect(b1, b2).getConstraintDomain());
            }
        }

        bitEntryset = may2.getConsTable().entrySet();
        i2 = bitEntryset.iterator();
        while (i2.hasNext()) {// ��b����Щaû�г��ֵĲ��뵽table��
            Map.Entry<SymbolFactor, BitConstraintDomain> e = i2.next();
            v1 = (SymbolFactor) e.getKey();
            b1 = e.getValue();
            b2 = may1.getBitConsDomain(v1);
            if (b2 == null)
                r.addBitConstraint(v1, b1.getConstraintDomain());
        }
        return r;
    }

    /**
     * �����߼�||�������ұ��ʽ���漰��Լ���������䣬���ϲ����ֱ������ֵ�ͺ�λ�����ͷ��ţ�
     * 
     * @param leftdata ����ʽ
     * @param rightdata �ұ��ʽ
     * @return
     */
    public ConditionData calLogicalOrExpression(ConditionData leftdata, ConditionData rightdata) {
        ConditionData r = new ConditionData(currentvex);
        SymbolDomainSet may1 = leftdata.getTrueMayDomainSet();
        SymbolDomainSet may2 = rightdata.getTrueMayDomainSet();

        SymbolDomainSet must1 = leftdata.getTrueMustDomainSet();
        SymbolDomainSet must2 = rightdata.getTrueMustDomainSet();

        SymbolDomainSet may = new SymbolDomainSet();
        SymbolDomainSet must = new SymbolDomainSet();

        SymbolFactor v1 = null, v2 = null;
        Domain d1 = null, d2 = null;
        Set<Map.Entry<SymbolFactor, Domain>> entryset = may1.getTable().entrySet();
        Iterator<Map.Entry<SymbolFactor, Domain>> i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v1 = e.getKey();
            d1 = e.getValue();
            // ��domainset�в���
            d2 = may2.getDomain(v1);
            CType type = v1.getType();
            if (d2 == null) {
                if (!isContradicBut(may2, v1)) {
                    // ��ì�ܣ�ȡȫ��
                    Domain d = v1.getDomainWithoutNull(currentvex.getSymDomainset());
                    may.addDomain(v1, Domain.intersect(d, Domain.getFullDomainFromType(type), type));
                } else {
                    // ì�ܣ�ȡ���������may
                    may.addDomain(v1, d1);
                }
            } else {
                // ȡunion��
                may.addDomain(v1, Domain.union(d1, d2, v1.getType()));
            }
        }

        // ��b����Щaû�г��ֵĲ��뵽table��
        entryset = may2.getTable().entrySet();
        i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v2 = e.getKey();
            d2 = e.getValue();
            // ��domainset�в���
            d1 = may1.getDomain(v2);
            CType type = v2.getType();
            if (d1 == null) {
                if (!isContradicBut(may1, v2)) {
                    // ��ì�ܣ�ȡȫ��
                    Domain d = v2.getDomainWithoutNull(currentvex.getSymDomainset());
                    may.addDomain(v2, Domain.intersect(d, Domain.getFullDomainFromType(type), type));
                } else {
                    // ì�ܣ�ȡ���������may
                    may.addDomain(v2, d2);
                }
            }
        }

        r.addMayDomain(may);
        // ����must
        entryset = must1.getTable().entrySet();
        i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v1 = e.getKey();
            d1 = e.getValue();
            // ��domainset�в���
            d2 = must2.getDomain(v1);
            CType type = v1.getType();
            if (d2 == null) {
                if (isLogicallyTrueBut(must2, v1)) {
                    // ���棬ȡȫ��
                    Domain d = v1.getDomainWithoutNull(currentvex.getSymDomainset());
                    may.addDomain(v1, Domain.intersect(d, Domain.getFullDomainFromType(type), type));
                } else {
                    // �����棬ȡ���������must
                    must.addDomain(v1, d1);
                }
            } else {
                // ȡunion���������⴦��
                must.addDomain(v1, Domain.union(d1, d2, v1.getType()));
            }
        }

        // ��b����Щaû�г��ֵĲ��뵽table��
        entryset = must2.getTable().entrySet();
        i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v2 = e.getKey();
            d2 = e.getValue();
            // ��domainset�в���
            d1 = must1.getDomain(v2);
            CType type = v2.getType();
            if (d1 == null) {
                if (isLogicallyTrueBut(must1, v2)) {
                    // ���棬ȡȫ��
                    Domain d = v2.getDomainWithoutNull(currentvex.getSymDomainset());
                    may.addDomain(v2, Domain.intersect(d, Domain.getFullDomainFromType(type), type));
                } else {
                    // �����棬ȡ���������must
                    must.addDomain(v2, d2);
                }
            }
        }

        r.addMustDomain(must);

        // ��λ����Լ���ϲ� add by Yaoweichang
        BitConstraintDomain b1 = null, b2 = null;
        Set<Map.Entry<SymbolFactor, BitConstraintDomain>> bitEntryset = may1.getConsTable().entrySet();
        Iterator<Entry<SymbolFactor, BitConstraintDomain>> i2 = bitEntryset.iterator();
        while (i2.hasNext()) {// ��a�г��ֵ����з���Լ������table��ͬʱ�ϲ�a��b�����ֵ�Լ��
            Map.Entry<SymbolFactor, BitConstraintDomain> e = i2.next();
            v1 = (SymbolFactor) e.getKey();
            b1 = e.getValue();
            b2 = may1.getBitConsDomain(v1);
            if (b2 == null)
                r.addBitConstraint(v1, b1.getConstraintDomain());
            else {
                r.addBitConstraint(v1, BitConstraintDomain.intersect(b1, b2).getConstraintDomain());
            }
        }

        bitEntryset = may2.getConsTable().entrySet();
        i2 = bitEntryset.iterator();
        while (i2.hasNext()) {// ��b����Щaû�г��ֵĲ��뵽table��
            Map.Entry<SymbolFactor, BitConstraintDomain> e = i2.next();
            v1 = (SymbolFactor) e.getKey();
            b1 = e.getValue();
            b2 = may1.getBitConsDomain(v1);
            if (b2 == null)
                r.addBitConstraint(v1, b1.getConstraintDomain());
        }
        return r;
    }

    /**
     * �����߼�&&�������ұ��ʽ���漰��Լ���������䣬���ϲ����ֱ������ֵ�ͺ�λ�����ͷ��ţ�
     * 
     * @param leftdata
     * @param rightdata
     * @param curSymbolDomainSet
     * @return
     */
    public ConditionData calLogicalAndExpression(ConditionData leftdata, ConditionData rightdata, SymbolDomainSet curSymbolDomainSet) {
        ConditionData r = new ConditionData();
        SymbolDomainSet may1 = leftdata.getTrueMayDomainSet();
        SymbolDomainSet may2 = rightdata.getTrueMayDomainSet();

        SymbolDomainSet must1 = leftdata.getTrueMustDomainSet();
        SymbolDomainSet must2 = rightdata.getTrueMustDomainSet();

        SymbolDomainSet may = new SymbolDomainSet();
        SymbolDomainSet must = new SymbolDomainSet();

        SymbolFactor v1 = null, v2 = null;
        Domain d1 = null, d2 = null;
        Set<Map.Entry<SymbolFactor, Domain>> entryset = may1.getTable().entrySet();
        Iterator<Map.Entry<SymbolFactor, Domain>> i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v1 = (SymbolFactor) e.getKey();
            d1 = e.getValue();
            // ��domainset�в���
            d2 = may2.getDomain(v1);
            CType type = v1.getType();
            if (d2 == null) {
                if (!isContradicBut(may2, v1)) {
                    Domain d = v1.getDomainWithoutNull(curSymbolDomainSet);
                    may.addDomain(v1, Domain.intersect(d, d1, type));
                } else {
                    // always FALSE, add EMPTY
                    may.addDomain(v1, Domain.getEmptyDomainFromType(type));
                }
            } else {
                // ȡintersect
                may.addDomain(v1, Domain.intersect(d1, d2, v1.getType()));
            }
        }

        // ��b����Щaû�г��ֵĲ��뵽table��
        entryset = may2.getTable().entrySet();
        i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v2 = e.getKey();
            d2 = e.getValue();
            // ��domainset�в���
            d1 = may1.getDomain(v2);
            CType type = v2.getType();
            if (d1 == null) {
                if (!isContradicBut(may1, v2)) {
                    Domain d = v2.getDomainWithoutNull(curSymbolDomainSet);
                    may.addDomain(v2, Domain.intersect(d, d2, type));
                } else {
                    // always FALSE, add EMPTY
                    may.addDomain(v2, Domain.getEmptyDomainFromType(type));
                }
            }
        }

        r.addMayDomain(may);
        // ����must
        entryset = must1.getTable().entrySet();
        i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v1 = (SymbolFactor) e.getKey();
            d1 = e.getValue();
            // ��domainset�в���
            d2 = must2.getDomain(v1);
            CType type = v1.getType();
            if (d2 == null) {
                if (isLogicallyTrueBut(must2, v1, curSymbolDomainSet)) {
                    // ���棬ȡ��ǰȡ���������must
                    must.addDomain(v1, d1);
                } else {
                    must.addDomain(v1, Domain.getEmptyDomainFromType(type));
                }
            } else {
                // ȡintersect
                must.addDomain(v1, Domain.intersect(d1, d2, v1.getType()));
            }
        }

        // ��b����Щaû�г��ֵĲ��뵽table��
        entryset = must2.getTable().entrySet();
        i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v2 = e.getKey();
            d2 = e.getValue();
            // ��domainset�в���
            d1 = must1.getDomain(v2);
            if (d1 == null) {
                if (isLogicallyTrueBut(must1, v2, curSymbolDomainSet)) {
                    // ���棬ȡ���������must
                    must.addDomain(v2, d2);
                } else {
                    must.addDomain(v2, Domain.getEmptyDomainFromType(v2.getType()));
                }
            } else {
                must.addDomain(v2, Domain.intersect(d1, d2, v2.getType()));
            }
        }

        r.addMustDomain(must);

        // ��λ����Լ���ϲ� add by Yaoweichang
        BitConstraintDomain b1 = null, b2 = null;
        Set<Map.Entry<SymbolFactor, BitConstraintDomain>> bitEntryset = may1.getConsTable().entrySet();
        Iterator<Entry<SymbolFactor, BitConstraintDomain>> i2 = bitEntryset.iterator();
        while (i2.hasNext()) {// ��a�г��ֵ����з���Լ������table��ͬʱ�ϲ�a��b�����ֵ�Լ��
            Map.Entry<SymbolFactor, BitConstraintDomain> e = i2.next();
            v1 = (SymbolFactor) e.getKey();
            b1 = e.getValue();
            b2 = may1.getBitConsDomain(v1);
            if (b2 == null)
                r.addBitConstraint(v1, b1.getConstraintDomain());
            else {
                r.addBitConstraint(v1, BitConstraintDomain.intersect(b1, b2).getConstraintDomain());
            }
        }

        bitEntryset = may2.getConsTable().entrySet();
        i2 = bitEntryset.iterator();
        while (i2.hasNext()) {// ��b����Щaû�г��ֵĲ��뵽table��
            Map.Entry<SymbolFactor, BitConstraintDomain> e = i2.next();
            v1 = (SymbolFactor) e.getKey();
            b1 = e.getValue();
            b2 = may1.getBitConsDomain(v1);
            if (b2 == null)
                r.addBitConstraint(v1, b1.getConstraintDomain());
        }

        return r;
    }

    /**
     * �����߼�||�������ұ��ʽ���漰��Լ���������䣬���ϲ����ֱ������ֵ�ͺ�λ�����ͷ��ţ�
     * 
     * @param leftdata
     * @param rightdata
     * @param curSymbolDomainSet
     * @return
     */
    public ConditionData calLogicalOrExpression(ConditionData leftdata, ConditionData rightdata, SymbolDomainSet curSymbolDomainSet) {
        ConditionData r = new ConditionData();
        SymbolDomainSet may1 = leftdata.getTrueMayDomainSet();
        SymbolDomainSet may2 = rightdata.getTrueMayDomainSet();

        SymbolDomainSet must1 = leftdata.getTrueMustDomainSet();
        SymbolDomainSet must2 = rightdata.getTrueMustDomainSet();

        SymbolDomainSet may = new SymbolDomainSet();
        SymbolDomainSet must = new SymbolDomainSet();

        SymbolFactor v1 = null, v2 = null;
        Domain d1 = null, d2 = null;
        Set<Map.Entry<SymbolFactor, Domain>> entryset = may1.getTable().entrySet();
        Iterator<Map.Entry<SymbolFactor, Domain>> i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v1 = e.getKey();
            d1 = e.getValue();
            // ��domainset�в���
            d2 = may2.getDomain(v1);
            CType type = v1.getType();
            if (d2 == null) {
                if (!isContradicBut(may2, v1)) {
                    // ��ì�ܣ�ȡȫ��
                    Domain d = v1.getDomainWithoutNull(curSymbolDomainSet);
                    may.addDomain(v1, Domain.intersect(d, Domain.getFullDomainFromType(type), type));
                } else {
                    // ì�ܣ�ȡ���������may
                    may.addDomain(v1, d1);
                }
            } else {
                // ȡunion��
                may.addDomain(v1, Domain.union(d1, d2, v1.getType()));
            }
        }

        // ��b����Щaû�г��ֵĲ��뵽table��
        entryset = may2.getTable().entrySet();
        i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v2 = e.getKey();
            d2 = e.getValue();
            // ��domainset�в���
            d1 = may1.getDomain(v2);
            CType type = v2.getType();
            if (d1 == null) {
                if (!isContradicBut(may1, v2)) {
                    // ��ì�ܣ�ȡȫ��
                    Domain d = v2.getDomainWithoutNull(curSymbolDomainSet);
                    may.addDomain(v2, Domain.intersect(d, Domain.getFullDomainFromType(type), type));
                } else {
                    // ì�ܣ�ȡ���������may
                    may.addDomain(v2, d2);
                }
            }
        }

        r.addMayDomain(may);
        // ����must
        entryset = must1.getTable().entrySet();
        i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v1 = e.getKey();
            d1 = e.getValue();
            // ��domainset�в���
            d2 = must2.getDomain(v1);
            CType type = v1.getType();
            if (d2 == null) {
                if (isLogicallyTrueBut(must2, v1, curSymbolDomainSet)) {
                    // ���棬ȡȫ��
                    Domain d = v1.getDomainWithoutNull(curSymbolDomainSet);
                    may.addDomain(v1, Domain.intersect(d, Domain.getFullDomainFromType(type), type));
                } else {
                    // �����棬ȡ���������must
                    must.addDomain(v1, d1);
                }
            } else {
                // ȡunion���������⴦��
                must.addDomain(v1, Domain.union(d1, d2, v1.getType()));
            }
        }

        // ��b����Щaû�г��ֵĲ��뵽table��
        entryset = must2.getTable().entrySet();
        i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry<SymbolFactor, Domain> e = i.next();
            v2 = e.getKey();
            d2 = e.getValue();
            // ��domainset�в���
            d1 = must1.getDomain(v2);
            CType type = v2.getType();
            if (d1 == null) {
                if (isLogicallyTrueBut(must1, v2, curSymbolDomainSet)) {
                    // ���棬ȡȫ��
                    Domain d = v2.getDomainWithoutNull(curSymbolDomainSet);
                    may.addDomain(v2, Domain.intersect(d, Domain.getFullDomainFromType(type), type));
                } else {
                    // �����棬ȡ���������must
                    must.addDomain(v2, d2);
                }
            }
        }

        r.addMustDomain(must);

        // ��λ����Լ���ϲ� add by Yaoweichang
        BitConstraintDomain b1 = null, b2 = null;
        Set<Map.Entry<SymbolFactor, BitConstraintDomain>> bitEntryset = may1.getConsTable().entrySet();
        Iterator<Entry<SymbolFactor, BitConstraintDomain>> i2 = bitEntryset.iterator();
        while (i2.hasNext()) {// ��a�г��ֵ����з���Լ������table��ͬʱ�ϲ�a��b�����ֵ�Լ��
            Map.Entry<SymbolFactor, BitConstraintDomain> e = i2.next();
            v1 = (SymbolFactor) e.getKey();
            b1 = e.getValue();
            b2 = may1.getBitConsDomain(v1);
            if (b2 == null)
                r.addBitConstraint(v1, b1.getConstraintDomain());
            else {
                r.addBitConstraint(v1, BitConstraintDomain.intersect(b1, b2).getConstraintDomain());
            }
        }

        bitEntryset = may2.getConsTable().entrySet();
        i2 = bitEntryset.iterator();
        while (i2.hasNext()) {// ��b����Щaû�г��ֵĲ��뵽table��
            Map.Entry<SymbolFactor, BitConstraintDomain> e = i2.next();
            v1 = (SymbolFactor) e.getKey();
            b1 = e.getValue();
            b2 = may1.getBitConsDomain(v1);
            if (b2 == null)
                r.addBitConstraint(v1, b1.getConstraintDomain());
        }
        return r;
    }

    public static ConditionData calLoopCondtion(ConditionData data, VexNode vex, CParserVisitorAdapter convisitor, Node treenode) {
        treenode.jjtAccept(convisitor, data);
        if (!Config.LOOPCAL) {
            // zys:2010.11.11 ���û�ж�ѭ��������ȷ����,����Ҫ������ȷ��MUST���䣻
            // ���ִ�����CPP��ֻ��ѭ������һ�ε���������Ȼ�������䱣�أ���ѭ���ڵ������Ǵ����
            SymbolDomainSet old = vex.getSymDomainset();
            ConditionData data1 = new ConditionData(vex);
            vex.setSymDomainset(null);
            treenode.jjtAccept(convisitor, data1);
            vex.setSymDomainset(old);

            Set entryset = data1.domainstable.entrySet();
            Iterator i = entryset.iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                SymbolFactor v = (SymbolFactor) e.getKey();
                ConditionDomains d = (ConditionDomains) e.getValue();
                data.addMustDomain(v, d.must);
            }
        }
        return data;
    }

    /*
     * add by za
     * ��public static ConditionData calLoopCondtion(ConditionData data,VexNode
     * vex,ConditionDomainVisitor convisitor,Node treenode)
     * ���߼���ͬ����ConditionDomainVisitor���������˻���·����ConditionDomainVisitor
     */
    public static ConditionData calLoopCondtion(ConditionData data, VexNode vex, PathConditionVisitor convisitor, Node treenode) {
        SymbolDomainSet old = vex.getSymDomainset();
        treenode.jjtAccept(convisitor, data);

        ConditionData data1 = new ConditionData(vex);
        vex.setSymDomainset(null);
        treenode.jjtAccept(convisitor, data1);
        vex.setSymDomainset(old);
        // ������ȷ��ѭ������

        Set entryset = data1.domainstable.entrySet();
        Iterator i = entryset.iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            SymbolFactor v = (SymbolFactor) e.getKey();
            ConditionDomains d = (ConditionDomains) e.getValue();
            data.addMustDomain(v, d.must);
            data.addMayDomain(v, d.may);
        }
        return data;
    }

    /**
     * add by zxz
     * ��public static ConditionData calLoopCondtion(ConditionData data,VexNode
     * vex,ConditionDomainVisitor convisitor,Node treenode)
     * ���߼���ͬ����ConditionDomainVisitor���������˻��ڷ�֧�޽��ImplicateDomainVisitor
     */
    public static ConditionData calLoopCondtion(ConditionData data, VexNode vex, BranchboundDomainVisitor convisitor, Node treenode) {
        treenode.jjtAccept(convisitor, data);
        if (!Config.LOOPCAL) {
            // zys:2010.11.11 ���û�ж�ѭ��������ȷ����,����Ҫ������ȷ��MUST���䣻
            // ���ִ�����CPP��ֻ��ѭ������һ�ε���������Ȼ�������䱣�أ���ѭ���ڵ������Ǵ����
            SymbolDomainSet old = vex.getSymDomainset();
            ConditionData data1 = new ConditionData(vex);
            vex.setSymDomainset(null);
            treenode.jjtAccept(convisitor, data1);
            vex.setSymDomainset(old);

            Set entryset = data1.domainstable.entrySet();
            Iterator i = entryset.iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                SymbolFactor v = (SymbolFactor) e.getKey();
                ConditionDomains d = (ConditionDomains) e.getValue();
                data.addMustDomain(v, d.must);
            }
        }
        return data;
    }

    /**
     * ���÷����Ƿ����λ����
     * 
     * @param isBit
     * @return
     *         created by Yaoweichang on 2015-11-16 ����3:37:46
     */
    public boolean setBitConstraint(boolean isBit) {
        return isBitConstraint = isBit;
    }

    /**
     * �õ������Ƿ����λ����
     * 
     * @return
     *         created by Yaoweichang on 2015-11-16 ����3:38:25
     */
    public boolean getBitConstraint() {
        return isBitConstraint;
    }
}
