package softtest.domain.c.symbolic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import softtest.config.c.Config;
import softtest.domain.c.analysis.SymbolDomainSet;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.DoubleDomain;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.PointerDomain;

public class Expression implements Comparable<Expression>, Cloneable {
    private static Logger logger = Logger.getRootLogger();

    private boolean flattened;
    private ArrayList<Term> terms;
    private boolean complicated = false;// ����expression�к�����һ�����ϵı������Term ��a*b

    // add by zhouhb 2011.5.5
    // ��ӿյĹ��캯�����ڶ�������δʹ�õĽṹ���Ա�����ı��ʽ
    public Expression() {
        super();
        terms = new ArrayList<Term>();
    }

    public Expression(ArrayList<Term> terms, boolean flattened) {
        super();
        this.terms = terms;
        this.flattened = flattened;
    }

    public Expression(Term t, boolean flattened) {
        super();
        terms = new ArrayList<Term>();
        terms.add(t);
        this.flattened = flattened;
    }

    public Expression(Factor f) {
        this(new Term(new Power(f, true), true), true);
    }

    public Expression(double d) {
        this(new DoubleFactor(d));
    }

    public Expression(long l) {
        this(new IntegerFactor(l));
    }

    public ArrayList<Term> getTerms() {
        return terms;
    }

    public Factor getSingleFactor() {
        if (terms.size() != 1) {
            return null;
        }
        return terms.get(0).getSingleFactor();
    }

    public void setTerms(ArrayList<Term> terms) {
        this.terms = terms;
        flattened = false;
    }

    public void appendTerm(Term t) {
        terms.add(t);
        flattened = false;
    }

    @Override
    public int hashCode() {
        if (!flattened) {
            flatten(0);
        }

        final int prime = 31;
        int result = 1;

        if (terms == null) {
            result = prime * result;
        } else {
            result = prime * result;
            Iterator<Term> i = terms.iterator();
            while (i.hasNext()) {
                Term t = i.next();
                result = result + t.hashCode();
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Expression other = (Expression) obj;

        if (!flattened) {
            flatten(0);
        }

        if (!other.flattened) {
            other.flatten(0);
        }

        if (terms == null) {
            if (other.terms != null)
                return false;
        } else {
            if (terms.size() != other.terms.size()) {
                return false;
            }
            Iterator<Term> i = terms.iterator();
            Iterator<Term> iother = other.terms.iterator();
            while (i.hasNext()) {
                Term t1 = i.next();
                Term t2 = iother.next();
                if (!t1.equals(t2)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isValueEqual(Expression obj, SymbolDomainSet ds) {
        if (equals(obj)) {
            return true;
        }
        Domain d1 = getDomain(ds);
        Domain d2 = obj.getDomain(ds);
        if (d1 != null && d2 != null) {
            DoubleDomain doubledomain1 = Domain.castToDoubleDomain(d1);
            DoubleDomain doubledomain2 = Domain.castToDoubleDomain(d2);
            if (doubledomain1.isCanonical() && doubledomain2.isCanonical()) {
                if (doubledomain1.jointoOneInterval().getMin() == doubledomain2.jointoOneInterval().getMin()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isValueMustNotEqual(Expression obj, SymbolDomainSet ds) {
        Domain d1 = getDomain(ds);
        Domain d2 = obj.getDomain(ds);
        if (d1 != null && d2 != null) {
            DoubleDomain doubledomain1 = Domain.castToDoubleDomain(d1);
            DoubleDomain doubledomain2 = Domain.castToDoubleDomain(d2);
            if (doubledomain1.isCanonical() && doubledomain2.isCanonical()) {
                if (doubledomain1.jointoOneInterval().getMin() != doubledomain2.jointoOneInterval().getMin()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        Iterator<Term> i = terms.iterator();
        boolean first = true;
        while (i.hasNext()) {
            Term t = i.next();
            if (first) {
                if (t.getOperator().equals("-")) {
                    strBuilder.append("-");
                }
            } else {
                strBuilder.append(t.getOperator());
            }
            strBuilder.append(t.toString());
            first = false;
        }
        return strBuilder.toString();
    }

    // add by jinkaifeng 2013.5.28 Ŀ���ǻ��Expression�ж�Ӧ��VND��string, ����i_0xx+j_0xx���õ�i+j
    public String getVND() {
        StringBuilder strBuilder = new StringBuilder();
        Iterator<Term> i = terms.iterator();
        boolean first = true;
        while (i.hasNext()) {
            Term t = i.next();
            if (first) {
                if (t.getOperator().equals("-")) {
                    strBuilder.append("-");
                }
            } else {
                strBuilder.append(t.getOperator());
            }
            strBuilder.append(t.getVND());
            first = false;
        }
        return strBuilder.toString();
    }

    public int compareTo(Expression o) {
        int ret = 0;
        if (this.getTerms().size() != o.getTerms().size())
            return this.getTerms().size() - o.getTerms().size();

        Iterator<Term> i = terms.iterator();
        Iterator<Term> io = o.terms.iterator();
        while (i.hasNext() && io.hasNext()) {
            Term t1 = i.next();
            Term t2 = io.next();
            ret = t1.compareTo(t2);// 2012-10-10 zhangxuzhou ����bug-2��2��ȷ���0
            if (ret != 0) {
                return ret;
            }
        }
        if (terms.size() < o.terms.size()) {
            return -1;
        }
        if (terms.size() > o.terms.size()) {
            return 1;
        }
        return ret;
    }

    /**
     * �˷������ڴ�����ʽ���ϲ��������Լ�������
     * 
     * @param depth
     * @return
     */

    @SuppressWarnings("unchecked")
    public Expression flatten(int depth) {
        if (flattened) {
            return this;
        }
        depth++;
        if (depth > 256) {
            throw new RuntimeException("Recursion depth limit reached");
        }

        // ������Ҫ��Ƕ�ױ��ʽ���Ͳ���Ҫ��0��
        Iterator<Term> iterms = terms.iterator();
        LinkedList<Term> sortedterms = new LinkedList<Term>();
        while (iterms.hasNext()) {
            Term term = iterms.next().flatten(depth);
            Factor single = term.getSingleFactor();

            if (single instanceof NestedExprFactor) {
                NestedExprFactor nestedfactor = (NestedExprFactor) single;
                Expression expr = nestedfactor.getExpression();
                for (Term t : expr.terms) {
                    if (term.getOperator().equals("-")) {
                        if (t.getOperator().equals("+")) {
                            t.setOperator("-");
                        } else {
                            t.setOperator("+");
                        }
                    }
                    if (t.getSingleFactor() instanceof NumberFactor) {
                        NumberFactor nf = (NumberFactor) t.getSingleFactor();
                        if (nf.getDoubleValue() == 0) {
                            continue;
                        }
                    }
                    sortedterms.add(t);
                }
                continue;
            }

            if (single instanceof NumberFactor) {
                NumberFactor nf = (NumberFactor) single;
                if (nf.getDoubleValue() == 0) {
                    continue;
                }
            }
            sortedterms.add(term);
        }

        if (sortedterms.size() == 0) {
            sortedterms.add(new Term(new Power(new IntegerFactor(0), true), true));
        }
        // modified by zhangxuzhou 2012-10-10 �涨���򷽷�
        Collections.sort(sortedterms, new Comparator() {
            public int compare(Object arg0, Object arg1) {
                int ret = 0;
                Term terma = (Term) arg0;
                Term termb = (Term) arg1;
                Iterator<Power> i = terma.getPowers().iterator();
                Iterator<Power> io = termb.getPowers().iterator();
                while (i.hasNext() && io.hasNext()) {
                    Power p1 = i.next();

                    while (p1.isNumber() && i.hasNext()) {
                        p1 = i.next();
                    }
                    Power p2 = io.next();
                    while (p2.isNumber() && io.hasNext()) {
                        p2 = io.next();
                    }
                    ret = p1.compareTo(p2);
                    if (ret != 0) {
                        return ret;
                    }
                }
                ret = terma.getOperator().compareTo(termb.getOperator());
                return ret;
            }
        });// �ϲ�ͬ�����Ƿ��������������2012-10-10 zhangxuzhou ������Ҫ�ı����������

        // ��ǰ��û�д���ʹ�÷�����չ������ʽ�˷�

        // �ϲ�ͬ����
        ArrayList<Term> listterm = new ArrayList<Term>();
        iterms = sortedterms.iterator();
        Term previouseterm = null;
        while (iterms.hasNext()) {
            Term currentterm = iterms.next();
            ArrayList<Power> pbase = null, cbase = null;
            NumberFactor pcoe = null, ccoe = null;
            if (previouseterm == null) {
                previouseterm = currentterm;
                continue;
            }
            Power first = previouseterm.getPowers().get(0);
            if (first.getSingleFactor() instanceof NumberFactor) {
                pcoe = (NumberFactor) first.getSingleFactor();
                pbase = new ArrayList<Power>();
                pbase.addAll(previouseterm.getPowers().subList(1, previouseterm.getPowers().size()));
            } else {
                pcoe = new IntegerFactor(1);
                pbase = previouseterm.getPowers();
            }

            first = currentterm.getPowers().get(0);
            if (first.getSingleFactor() instanceof NumberFactor) {
                ccoe = (NumberFactor) first.getSingleFactor();
                cbase = new ArrayList<Power>();
                cbase.addAll(currentterm.getPowers().subList(1, currentterm.getPowers().size()));
            } else {
                ccoe = new IntegerFactor(1);
                cbase = currentterm.getPowers();
            }

            // pbase:��һ��������� pcoe����һ�����ϵ��
            // cbase:��ǰ������� ccoe����ǰ���ϵ��
            if (!pbase.isEmpty()) {
                // ��һ����������Ϊ��
                if (pbase.equals(cbase)) {
                    // ������ͬ����Ҫ�ϲ�
                    if (previouseterm.getOperator().equals("-")) {
                        pcoe = pcoe.numMinus();
                    }
                    if (currentterm.getOperator().equals("-")) {
                        ccoe = ccoe.numMinus();
                    }
                    pcoe = pcoe.numAdd(ccoe);

                    if (pcoe.getDoubleValue() == 0) {
                        // �ϲ���ϵ��Ϊ0����������ֵ��Ϊ0�����ǵ�0�ڼӷ�����Ҫ����
                        previouseterm = null;
                    } else if (pcoe.getDoubleValue() == 1) {
                        // �ϲ���ϵ��Ϊ1������ϵ��
                        previouseterm = new Term("+", pbase, true);
                    } else {
                        // ��ͨ�ϲ�
                        pbase.add(0, new Power(pcoe, true));
                        previouseterm = new Term("+", pbase, true);

                    }
                } else {
                    // ��������ͬ����һ������Է���listterm����
                    if (previouseterm.getOperator().equals("-")) {
                        pcoe = pcoe.numMinus();
                    }
                    if (pcoe.getDoubleValue() == 0) {
                        // ϵ��Ϊ0����������ֵ��Ϊ0�����ǵ�0�ڼӷ�����Ҫ����
                        previouseterm = currentterm;
                    } else if (pcoe.getDoubleValue() == 1) {
                        // ϵ��Ϊ1������ϵ��
                        listterm.add(new Term("+", pbase, true));
                        previouseterm = currentterm;
                    } else {
                        // ��ͨ����listterm
                        pbase.add(0, new Power(pcoe, true));
                        listterm.add(new Term("+", pbase, true));
                        previouseterm = currentterm;
                    }
                }
            } else {
                // ��һ������Ϊ��
                if (cbase.isEmpty()) {
                    // �����ϲ�
                    if (previouseterm.getOperator().equals("-")) {
                        pcoe = pcoe.numMinus();
                    }
                    if (currentterm.getOperator().equals("-")) {
                        ccoe = ccoe.numMinus();
                    }
                    pcoe = pcoe.numAdd(ccoe);

                    if (pcoe.getDoubleValue() == 0) {
                        // ����0
                        previouseterm = null;
                    } else {
                        // ��ͨ����
                        pbase.add(0, new Power(pcoe, true));
                        previouseterm = new Term("+", pbase, true);
                    }
                } else {
                    // ��������ͬ����һ����������Է���listterm����
                    if (previouseterm.getOperator().equals("-")) {
                        pcoe = pcoe.numMinus();
                    }
                    if (pcoe.getDoubleValue() == 0) {
                        // ����0
                        previouseterm = currentterm;
                    } else {
                        // ��ͨ����
                        pbase.add(0, new Power(pcoe, true));
                        listterm.add(new Term("+", pbase, true));
                        previouseterm = currentterm;
                    }
                }
            }
        }

        if (previouseterm != null) {
            listterm.add(previouseterm);
        }
        if (listterm.size() == 0) {
            listterm.add(new Term(new Power(new IntegerFactor(0), true), true));
        }
        Collections.sort(listterm);

        terms = listterm;
        flattened = true;
        return this;
    }

    public boolean isFlattened() {
        return flattened;
    }

    public void setFlattened(boolean flattened) {
        this.flattened = flattened;
    }

    public Expression add(Expression other) {
        Expression ret = null;
        // ���ڱ��ʽother�п��ܼ��㲻�������ʷ���ֵҲ�޷�����
        // modified by zhouhb 2010/8/12
        if (other == null)
            return null;
        try {
            ret = (Expression) clone();
            other = (Expression) other.clone();
        } catch (CloneNotSupportedException e) {
        }
        ret.flatten(0);
        other.flatten(0);
        ret.terms.addAll(other.getTerms());
        Collections.sort(ret.terms);
        ret.flattened = false;
        ret.flatten(0);
        return ret;
    }

    @SuppressWarnings("unchecked")
    public Expression sub(Expression other) {
        Expression ret = null;
        try {
            ret = (Expression) clone();
            other = (Expression) other.clone();
        } catch (CloneNotSupportedException e) {
        }
        ret.flatten(0);
        other.flatten(0);
        for (Term t : other.getTerms()) {
            if (t.getOperator().endsWith("+")) {
                t.setOperator("-");
            } else {
                t.setOperator("+");
            }
            t.setFlattened(false);
            ret.terms.add(t);
        }
        // Collections.sort(ret.terms);//���sort����ҲҪ�ı� 2012-10-10 zhangxuzhou
        // ʹ�ù涨�ıȽ���������ϵ�������ںϲ�ͬ����
        Collections.sort(ret.terms, new Comparator() {
            public int compare(Object arg0, Object arg1) {
                int ret = 0;
                Term terma = (Term) arg0;
                Term termb = (Term) arg1;
                Iterator<Power> i = terma.getPowers().iterator();
                Iterator<Power> io = termb.getPowers().iterator();
                while (i.hasNext() && io.hasNext()) {
                    Power p1 = i.next();

                    while (p1.isNumber() && i.hasNext()) {
                        p1 = i.next();
                    }
                    Power p2 = io.next();
                    while (p2.isNumber() && io.hasNext()) {
                        p2 = io.next();
                    }
                    ret = p1.compareTo(p2);
                    if (ret != 0) {
                        return ret;
                    }
                }
                ret = terma.getOperator().compareTo(termb.getOperator());
                return ret;
            }
        });
        ret.flattened = false;
        ret.flatten(0);
        return ret;
    }

    public Expression mul(Expression other) {
        Expression ret = null;
        try {
            ret = (Expression) clone();
            other = (Expression) other.clone();
        } catch (CloneNotSupportedException e) {
        }

        ret.flatten(0);
        other.flatten(0);

        ArrayList<Term> termlist = new ArrayList<Term>();
        for (Term t : ret.terms) {
            for (Term t2 : other.getTerms()) {
                termlist.add(t.mul(t2));
            }
        }
        Collections.sort(termlist);
        ret.flattened = false;
        ret.setTerms(termlist);
        ret.flatten(0);
        return ret;
    }

    public Expression div(Expression other) {
        Expression ret = null;
        try {
            ret = (Expression) clone();
            other = (Expression) other.clone();
        } catch (CloneNotSupportedException e) {
        }
        ret.flatten(0);
        other.flatten(0);
        if (other.getTerms().size() > 1) {
            NestedExprFactor f1 = new NestedExprFactor(other);
            NestedExprFactor f2 = new NestedExprFactor(ret);
            ArrayList<Term> terms = new ArrayList<Term>();
            // terms.add(new Term(new Power(f1,true),true));//2012-8-23ע�͵�
            // terms.add(new Term(new Power(f2,true),true));
            // return new Expression(terms,true);
            // add by xjx 2012-8-23
            /*
             * �����޸�����Ϊ ԭ���Ĵ���(a+b)/(c+d)���ͱ��ʽ�����(c+d)+(a+b)���͡�
             * ������������ԭ���ǣ������������1��
             * �����޸ĺ�(a+b)/(c+d)���ͱ��ʽ�ᱻ�����a/(c+d)+b/(c+d)��ʽ����Ȼ�޸ĺ�
             * �õ��ı��ʽ��ʽ��ԭ���ʽ������ȫһ������ȴ�ǵȼ۵ġ�
             */
            Term t1 = new Term(new Power(f1, true), true);
            for (Term t2 : ret.terms) {
                terms.add(t2.div(t1));
            }
            Collections.sort(terms);
            ret.setTerms(terms);
            ret.flattened = false;
            ret.flatten(0);
            return ret;
            // end add by xjx 2012-8-23
        } else {
            ArrayList<Term> termlist = new ArrayList<Term>();
            Term t2 = other.getTerms().get(0);
            for (Term t : ret.terms) {
                termlist.add(t.div(t2));
            }
            Collections.sort(termlist);
            ret.setTerms(termlist);
            ret.flattened = false;
            ret.flatten(0);
            return ret;
        }
    }

    /**
     * �ϲ�λ����&�ı��ʽ
     * 
     * @param other
     * @return
     *         created by Yaoweichang on 2015-04-17 ����4:03:45
     */
    public Expression and(Expression other) {
        Expression ret = null;
        if (other == null)
            return null;
        try {
            ret = (Expression) clone();
            other = (Expression) other.clone();
        } catch (CloneNotSupportedException e) {
        }
        ret.flatten(0);
        other.flatten(0);

        ArrayList<Term> termlist = new ArrayList<Term>();
        for (Term t : ret.terms) {
            t.setOperator("&");
            for (Term t2 : other.getTerms()) {
                t2.setOperator("&");
                termlist.add(t.and(t2));
            }
        }
        Collections.sort(termlist);
        ret.flattened = false;
        ret.setTerms(termlist);
        // ret.flatten(0);
        return ret;
    }

    /**
     * �ϲ�λ����|�ı��ʽ
     * 
     * @param other
     * @return
     *         created by Yaoweichang on 2015-04-17 ����4:07:00
     */
    public Expression inclusiveOR(Expression other) {
        Expression ret = null;
        if (other == null)
            return null;
        try {
            ret = (Expression) clone();
            other = (Expression) other.clone();
        } catch (CloneNotSupportedException e) {
        }
        ret.flatten(0);
        other.flatten(0);

        ArrayList<Term> termlist = new ArrayList<Term>();
        for (Term t : ret.terms) {
            t.setOperator("|");
            for (Term t2 : other.getTerms()) {
                t2.setOperator("|");
                termlist.add(t.inclusiveOR(t2));
            }
        }
        Collections.sort(termlist);
        ret.flattened = false;
        ret.setTerms(termlist);
        ret.flatten(0);
        return ret;
    }

    /**
     * �ϲ�λ����^�ı��ʽ
     * 
     * @param other
     * @return
     *         created by Yaoweichang on 2015-04-17 ����4:07:16
     */
    public Expression exclusiveOR(Expression other) {
        Expression ret = null;
        if (other == null)
            return null;
        try {
            ret = (Expression) clone();
            other = (Expression) other.clone();
        } catch (CloneNotSupportedException e) {
        }
        ret.flatten(0);
        other.flatten(0);

        ArrayList<Term> termlist = new ArrayList<Term>();
        for (Term t : ret.terms) {
            t.setOperator("^");
            for (Term t2 : other.getTerms()) {
                t2.setOperator("^");
                termlist.add(t.exclusiveOR(t2));
            }
        }
        Collections.sort(termlist);
        ret.flattened = false;
        ret.setTerms(termlist);
        ret.flatten(0);
        return ret;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Expression e = (Expression) super.clone();
        e.terms = new ArrayList<Term>();
        for (Term t : terms) {
            e.terms.add((Term) t.clone());
        }
        return e;
    }

    public Domain getDomain(SymbolDomainSet ds) {
        flatten(0);
        Domain ret = null;
        if (terms.size() == 0) {
            return ret;
        }
        ret = terms.get(0).getDomain(ds);
        for (int i = 1; i < terms.size(); i++) {
            // DoubleDomain d1=Domain.castToDoubleDomain(ret);
            if (ret == null) {
                return null;
            }


            Term t = terms.get(i);
            Domain td = t.getDomain(ds);

            // DoubleDomain d2=Domain.castToDoubleDomain(td);
            if (td == null) {
                return null;
            }
            // modified by zhouhb
            // ��Ϊָ������ʱ��������ת��
            if (ret instanceof IntegerDomain && td instanceof PointerDomain) {
                if (t.getOperator().equals("+")) {
                    td = PointerDomain.add((PointerDomain) td, (IntegerDomain) ret);
                } else {
                    td = PointerDomain.sub((PointerDomain) td, (IntegerDomain) ret);
                }
                ret = td;
            }// add by zhouhb 2010/8/18
            else if (ret instanceof PointerDomain && td instanceof IntegerDomain) {
                if (t.getOperator().equals("+")) {
                    ret = PointerDomain.add((PointerDomain) ret, (IntegerDomain) td);
                } else {
                    ret = PointerDomain.sub((PointerDomain) ret, (IntegerDomain) td);
                }
            } else if (ret instanceof PointerDomain && td instanceof PointerDomain) {
                try {// zys:2010.8.11
                    if (t.getOperator().equals("+")) {
                        if (((PointerDomain) td).isMinus())
                            td = new IntegerDomain();
                        else
                            throw new RuntimeException("Pointer + Pointer Error!");
                    } else {
                        td = new IntegerDomain();
                    }
                    ret = td;
                } catch (RuntimeException e) {
                    ret = new DoubleDomain(Double.MIN_VALUE, Double.MAX_VALUE);
                    if (Config.TRACE)
                        logger.info(e.getMessage());
                    break;

                }
            } else if (ret instanceof IntegerDomain && (td instanceof IntegerDomain || td instanceof DoubleDomain)) {
                // changed 2012-12-21 ͬ��integerDOmain���͵ķ����������� ֮ǰ��ת����double��
                if (t.getOperator().equals("+")) {
                    ret = IntegerDomain.add(Domain.castToIntegerDomain(ret), Domain.castToIntegerDomain(td));
                } else {
                    ret = IntegerDomain.sub(Domain.castToIntegerDomain(ret), Domain.castToIntegerDomain(td));
                }
            } else {
                if (t.getOperator().equals("+")) {
                    ret = DoubleDomain.add(Domain.castToDoubleDomain(ret), Domain.castToDoubleDomain(td));
                } else {
                    ret = DoubleDomain.sub(Domain.castToDoubleDomain(ret), Domain.castToDoubleDomain(td));
                }
            }
        }
        return ret;
    }

    //
    public HashSet<SymbolFactor> getAllSymbol() {
        HashSet<SymbolFactor> ret = new HashSet<SymbolFactor>();
        for (Term t : terms) {
            ret.addAll(t.getAllSymbol());
        }
        return ret;
    }

    /**
     * �жϵ�ǰ���ű��ʽ�Ƿ�Ƚϸ��ӡ�������ʽ�����������������Ϸ��ţ����߷��ű��ʽ
     * �����ϵ����Ϊ+1 -1�����߷��ű��ʽ�����ָ����Ϊ1
     * modified by zxz,����ϵ��û�������ƣ�����������ϵ��
     * 2012-11-19 ������
     * 
     * @return
     */
    public boolean isComplicated() {
        int num = getAllSymbol().size();
        if (num == 0) {
            return false;
        }
        if (num >= 2) {
            return true;
        }
        for (Term t : terms) {
            if (t.getSingleFactor() == null) {
                if (t.getMinusSingleFactor() == null) {
                    if (t.getRatioSingleFactor() == null)
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * ����Ƿ������ a*b����������ɵ�term
     * 
     * @return
     */
    public boolean judgeTermIsComplicated() {
        if (!complicated)
            for (Term p : this.terms) {
                if (p.judgeIsComplicated()) {
                    complicated = true;
                    break;
                }
            }
        return complicated;
    }

    public SymbolFactor getLastSymbolFactor() {
        SymbolFactor f = null;
        for (Term t : terms) {
            f = t.getLastSymbolFactor();
        }
        return f;
    }

    /**
     * ��Ӹ÷����ĳ����Ƿ��� Լ�����ڴ�ģ�͸���ģ�� ��ʵ��
     * �滻Factor
     * 
     * @param oldFactor ���滻��Factor
     * @param newFactor Ҫ�滻�ɵ�Factor
     */
    public void replaceFactor(Factor oldFactor, Factor newFactor) {
        for (Term term : this.getTerms()) {
            for (Power power : term.getPowers()) {
                ArrayList<Factor> factors = power.getFactors();
                for (int i = 0; i < factors.size(); i++) {
                    Factor factor = factors.get(i);
                    if (factor.equals(oldFactor)) {
                        factors.set(i, newFactor);
                    }
                }
            }
        }
        this.flatten(0);// ����
    }

}
