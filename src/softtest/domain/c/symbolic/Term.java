package softtest.domain.c.symbolic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

import softtest.config.c.Config;
import softtest.domain.c.analysis.SymbolDomainSet;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.DoubleDomain;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.PointerDomain;

public class Term implements Comparable<Term>, Cloneable {
    private boolean flattened;
    private String operator;
    private ArrayList<Power> powers;
    private static Logger logger = Logger.getRootLogger();
    private boolean complicated = false;// ����term��һ�����ϵı������ ��a*b

    public Term(String operator, ArrayList<Power> powers, boolean flattened) {
        super();
        this.operator = operator;
        this.powers = powers;
        this.flattened = flattened;
    }

    public Term(Power p, boolean flattened) {
        super();
        operator = "+";
        powers = new ArrayList<Power>();
        powers.add(p);
        this.flattened = flattened;
    }

    public void appendPower(Power power) {
        powers.add(power);
        flattened = false;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public ArrayList<Power> getPowers() {
        return powers;
    }

    public void setPowers(ArrayList<Power> powers) {
        this.powers = powers;
        flattened = false;
    }

    @Override
    public int hashCode() {
        if (!flattened) {
            flatten(0);
        }

        final int prime = 31;
        int result = 1;
        result = prime * result + ((operator == null) ? 0 : operator.hashCode());
        if (powers == null) {
            result = prime * result;
        } else {
            result = prime * result;
            Iterator<Power> i = powers.iterator();
            while (i.hasNext()) {
                Power p1 = i.next();
                result = result + p1.hashCode();
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

        if (!flattened) {
            flatten(0);
        }

        Term other = (Term) obj;
        if (!other.flattened) {
            other.flatten(0);
        }

        if (operator == null) {
            if (other.operator != null)
                return false;
        } else if (!operator.equals(other.operator))
            return false;

        if (powers == null) {
            if (other.powers != null)
                return false;
        } else {
            if (powers.size() != other.powers.size()) {
                return false;
            }
            Iterator<Power> i = powers.iterator();
            Iterator<Power> iother = other.powers.iterator();
            while (i.hasNext()) {
                Power p1 = i.next();
                Power p2 = iother.next();
                if (!p1.equals(p2)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        Iterator<Power> i = powers.iterator();
        boolean first = true;
        while (i.hasNext()) {
            Power p = i.next();
            if (first) {
                if (p.getOperator().equals("/"))
                    strBuilder.append("1/");
            } else {// ���Ӷ�λ����&�������Ĵ��� add by Yaoweichang
                if (this.operator.equals("&"))
                    strBuilder.append("&");
                else if (this.operator.equals("|"))
                    strBuilder.append("|");
                else if (this.operator.equals("^"))
                    strBuilder.append("^");
                else if (this.operator.equals("~"))
                    strBuilder.append("~");
                else
                    strBuilder.append(p.getOperator());
            }
            strBuilder.append(p.toString());
            first = false;
        }
        return strBuilder.toString();
    }

    // add by jinkaifeng 2013.5.28 Term��VND
    public String getVND() {
        StringBuilder strBuilder = new StringBuilder();
        Iterator<Power> i = powers.iterator();
        boolean first = true;
        while (i.hasNext()) {
            Power p = i.next();
            if (first) {
                if (p.getOperator().equals("/")) {
                    strBuilder.append("1/");
                }
            } else {
                strBuilder.append(p.getOperator());
            }
            strBuilder.append(p.getVND());
            first = false;
        }
        return strBuilder.toString();
    }

    public int compareTo(Term o) {
        int ret = 0;
        if (this.getPowers().size() != o.getPowers().size())
            return this.getPowers().size() - o.getPowers().size();

        Iterator<Power> i = powers.iterator();
        Iterator<Power> io = o.powers.iterator();
        while (i.hasNext() && io.hasNext()) {
            Power p1 = i.next();
            Power p2 = io.next();
            /*
             * while(p1.isNumber()&&i.hasNext()){//2012-10-10 �����仰��ϵ�����ԣ��ڱȽϵ�ʱ��Ӧ������
             * p1=i.next(); //compareTo�����������ںϲ�ͬ���������ʱ�������Ҫʵ�ֲ�ͬ��
             * } //��Ҫ
             * Power p2=io.next();
             * while(p2.isNumber()&&io.hasNext()){
             * p2=io.next();
             * }
             */
            ret = p1.compareTo(p2);
            if (ret != 0) {
                return ret;
            }
        }

        ret = operator.compareTo(o.operator);
        return ret;
    }

    public Factor getSingleFactor() {// ע�������power: b_362363^2 ��sizeҲ��1
        if (powers.size() != 1) {
            return null;
        }
        if (powers.get(0) != null)
            return powers.get(0).getSingleFactor();
        else
            return null;
    }

    public SymbolFactor getLastSymbolFactor() {
        SymbolFactor fa = null;
        for (Power p : powers) {
            for (Factor f : p.getFactors()) {
                if (f instanceof SymbolFactor) {
                    fa = (SymbolFactor) f;
                }
            }
        }
        return fa;
    }

    public Factor getMinusSingleFactor() {
        if (powers.size() == 2) {
            Factor co = powers.get(0).getSingleFactor();
            if (co instanceof NumberFactor) {
                if (((NumberFactor) co).getDoubleValue() == -1.0) {
                    return powers.get(1).getSingleFactor();
                }
            }
        }
        return null;
    }

    /**
     * ������ϵ���Ҳ�Ϊ��1��SingleFactor����
     * 
     * @author zxz
     * @return Factor
     */
    public Factor getRatioSingleFactor() {
        if (powers.size() == 2) {
            Factor co = powers.get(0).getSingleFactor();
            if (co instanceof NumberFactor) {
                if (((NumberFactor) co).getDoubleValue() != -1.0) {
                    return powers.get(1).getSingleFactor();
                }
            }
        }
        return null;
    }

    /**
     * ���� ϵ��
     * 
     * @author zxz
     * @return NumberFactor����ϵ�� Integer����Double
     */
    public NumberFactor getRatio() {
        if (powers.size() == 2) {
            Factor co = powers.get(0).getSingleFactor();
            if (co instanceof NumberFactor) {
                // if(((NumberFactor)co).getDoubleValue()!=-1.0){
                return (NumberFactor) co;
                // }
            }
        }
        return null;
    }


    public Term flatten(int depth) {
        if (flattened) {
            return this;
        }
        depth++;
        if (depth > 256) {
            throw new RuntimeException("Recursion depth limit reached");
        }

        // ������Ҫ��Ƕ�ױ��ʽ
        ArrayList<Power> sortedpowers = new ArrayList<Power>();
        Iterator<Power> ip = powers.iterator();
        while (ip.hasNext()) {
            Power power = ip.next().flatten(depth);
            Factor single = power.getSingleFactor();

            if (single instanceof NestedExprFactor) {
                String str = power.getOperator();
                NestedExprFactor nestedfactor = (NestedExprFactor) single;
                Expression expr = nestedfactor.getExpression();
                if (expr.getTerms().size() == 1) {
                    if (str.equals("*")) {
                        Term t = expr.getTerms().get(0);
                        sortedpowers.addAll(t.getPowers());
                    } else {
                        Term t = expr.getTerms().get(0);
                        for (Power p : t.getPowers()) {
                            if (p.getOperator().equals("*")) {
                                p.setOperator("*");
                            } else if (p.getOperator().equals("&")) {
                                p.setOperator("&");
                            } else {
                                p.setOperator("/");
                            }
                            sortedpowers.add(p);
                        }
                    }
                    continue;
                }
            }
            sortedpowers.add(power);
        }

        Collections.sort(sortedpowers);

        if (operator.equals("-")) {
            // ����Ϊ���ţ�����һ������ϵ��-1
            sortedpowers.add(0, new Power(new IntegerFactor(-1), true));
            operator = "+";
        }

        // �ϲ�ͬ�����ӵ�ָ�������ǵ����򣬽����ͷ��β�ϲ�һ�鼴��
        ArrayList<Power> listpower = new ArrayList<Power>();
        ip = sortedpowers.iterator();
        Power previousepower = null;
        while (ip.hasNext()) {
            Power currentpower = ip.next();
            ArrayList<Factor> pbase = null, cbase = null;
            NumberFactor pexp = null, cexp = null;
            if (previousepower == null) {
                previousepower = currentpower;
                continue;
            }
            Factor last = previousepower.getFactors().get(previousepower.getFactors().size() - 1);
            if (last instanceof NumberFactor) {
                pexp = (NumberFactor) last;
                pbase = new ArrayList<Factor>();
                pbase.addAll(previousepower.getFactors().subList(0, previousepower.getFactors().size() - 1));
            } else {
                pexp = new IntegerFactor(1);
                pbase = previousepower.getFactors();
            }

            last = currentpower.getFactors().get(currentpower.getFactors().size() - 1);
            if (last instanceof NumberFactor) {
                cexp = (NumberFactor) last;
                cbase = new ArrayList<Factor>();
                cbase.addAll(currentpower.getFactors().subList(0, currentpower.getFactors().size() - 1));
            } else {
                cexp = new IntegerFactor(1);
                cbase = currentpower.getFactors();
            }

            // pbase:��һ�����ӵĵ��� pexp����һ�����ӵ�ָ��
            // cbase:��ǰ���ӵĵ��� cexp����ǰ���ӵ�ָ��
            if (!pbase.isEmpty()) {
                // ��һ�����ӵײ�Ϊ��
                if (pbase.equals(cbase)) {
                    // ����ͬ����Ҫ�ϲ�
                    if (previousepower.getOperator().equals("/")) {
                        pexp = pexp.numMinus();
                    }
                    if (currentpower.getOperator().equals("/")) {
                        cexp = cexp.numMinus();
                    }
                    pexp = pexp.numAdd(cexp);

                    if (pexp.getDoubleValue() == 0) {
                        // �ϲ���ָ��Ϊ0������������ֵ��Ϊ1�����ǵ�1�ڳ˷�����Ҫ����
                        previousepower = null;
                    } else if (pexp.getDoubleValue() == 1) {
                        // �ϲ���ָ��Ϊ1������ָ��
                        previousepower = new Power("*", pbase, true);
                    } else {
                        // ��ͨ�ϲ�
                        pbase.add(pexp);
                        previousepower = new Power("*", pbase, true);
                    }
                } else {
                    // �ײ���ͬ����һ�����ӿ��Է���listpower����
                    if (previousepower.getOperator().equals("/")) {
                        pexp = pexp.numMinus();
                    }
                    if (pexp.getDoubleValue() == 0) {
                        // ָ��Ϊ0������������ֵ��Ϊ1�����ǵ�1�ڳ˷�����Ҫ����
                        previousepower = currentpower;
                    } else if (pexp.getDoubleValue() == 1) {
                        // ָ��Ϊ1������ָ��������listpower
                        listpower.add(new Power("*", pbase, true));
                        previousepower = currentpower;
                    } else {
                        // ��ͨ����listpower
                        pbase.add(pexp);
                        listpower.add(new Power("*", pbase, true));
                        previousepower = currentpower;
                    }
                }
            } else {
                // ��һ�����ӵ�Ϊ��
                if (cbase.isEmpty()) {
                    // �����ϲ�
                    if (previousepower.getOperator().equals("/")) {
                        pexp = new DoubleFactor(1).numDiv(pexp);
                    }
                    if (currentpower.getOperator().equals("/")) {
                        pexp = pexp.numDiv(cexp);
                    } else {
                        pexp = pexp.numMul(cexp);
                    }
                    if (pexp.getDoubleValue() == 0) {
                        // ����0
                        return new Term(new Power(new DoubleFactor(0), true), true);
                    } else if (pexp.getDoubleValue() == 1) {
                        // ����1�����ǵ�1�ڳ˷�����Ҫ����
                        previousepower = null;
                    } else {
                        // ��ͨ�����ϲ�
                        pbase.add(pexp);
                        previousepower = new Power("*", pbase, true);
                    }
                } else {
                    // �ײ���ͬ����һ���������ӿ��Է���listpower����
                    if (previousepower.getOperator().equals("/")) {
                        pexp = new DoubleFactor(1).numDiv(pexp);
                    }
                    if (pexp.getDoubleValue() == 0) {
                        // ����0
                        return new Term(new Power(new DoubleFactor(0), true), true);
                    } else if (pexp.getDoubleValue() == 1) {
                        // ����1�����ǵ�1�ڳ˷�����Ҫ����
                        previousepower = currentpower;
                    } else {
                        // ��ͨ�������ӣ�����listpower
                        pbase.add(pexp);
                        listpower.add(new Power("*", pbase, true));
                        previousepower = currentpower;
                    }
                }
            }
        }
        if (previousepower != null) {
            listpower.add(previousepower);
        }
        if (listpower.size() == 0) {
            listpower.add(new Power(new IntegerFactor(1), true));
        }
        Collections.sort(listpower);

        powers = listpower;
        flattened = true;
        return this;
    }

    public boolean isFlattened() {
        return flattened;
    }

    public void setFlattened(boolean flattened) {
        this.flattened = flattened;
    }

    public Term mul(Term other) {
        Term ret = null;
        try {
            ret = (Term) clone();
            other = (Term) other.clone();
        } catch (CloneNotSupportedException e) {
        }
        ret.flatten(0);
        other.flatten(0);
        ret.powers.addAll(other.getPowers());
        Collections.sort(ret.powers);
        ret.flattened = false;
        ret.flatten(0);
        return ret;
    }

    public Term div(Term other) {
        Term ret = null;
        try {
            ret = (Term) clone();
            other = (Term) other.clone();
        } catch (CloneNotSupportedException e) {
        }
        ret.flatten(0);
        other.flatten(0);
        for (Power p : other.getPowers()) {
            if (p.getOperator().equals("*")) {
                p.setOperator("/");
            } else {
                p.setOperator("*");
            }
            ret.powers.add(p);
        }
        Collections.sort(ret.powers);
        ret.flattened = false;
        ret.flatten(0);
        return ret;
    }

    /**
     * ����λ�������&�ı��ʽ
     * 
     * @param other
     * @return
     *         created by Yaoweichang on 2015-04-17 ����3:47:09
     */
    public Term and(Term other) {
        Term ret = null;
        try {
            ret = (Term) clone();
            other = (Term) other.clone();
        } catch (CloneNotSupportedException e) {
        }
        for (Power p : other.getPowers()) {
            p.setOperator("&");
            ret.powers.add(p);
        }
        Collections.sort(ret.powers);
        ret.setOperator("&");
        return ret;
    }

    /**
     * ����λ�������|�ı��ʽ
     * 
     * @param other
     * @return
     *         created by Yaoweichang on 2015-04-17 ����3:48:13
     */
    public Term inclusiveOR(Term other) {
        Term ret = null;
        try {
            ret = (Term) clone();
            other = (Term) other.clone();
        } catch (CloneNotSupportedException e) {
        }
        for (Power p : other.getPowers()) {
            p.setOperator("|");
            ret.powers.add(p);
        }
        Collections.sort(ret.powers);
        ret.setOperator("|");
        return ret;
    }

    /**
     * ����λ�������^�ı��ʽ
     * 
     * @param other
     * @return
     *         created by Yaoweichang on 2015-04-17 ����3:48:28
     */
    public Term exclusiveOR(Term other) {
        Term ret = null;
        try {
            ret = (Term) clone();
            other = (Term) other.clone();
        } catch (CloneNotSupportedException e) {
        }
        for (Power p : other.getPowers()) {
            p.setOperator("^");
            ret.powers.add(p);
        }
        Collections.sort(ret.powers);
        ret.setOperator("^");
        return ret;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Term term = (Term) super.clone();
        term.powers = new ArrayList<Power>();
        for (Power p : powers) {
            term.powers.add((Power) p.clone());
        }
        return term;
    }

    public Domain getDomain(SymbolDomainSet ds) {
        flatten(0);
        Domain ret = null;
        if (powers.size() == 0) {
            return ret;
        }
        ret = powers.get(0).getDomain(ds);
        for (int i = 1; i < powers.size(); i++) {
            DoubleDomain d1 = Domain.castToDoubleDomain(ret);
            if (d1 == null) {
                return null;
            }
            Power p = powers.get(i);
            Domain pd = p.getDomain(ds);
            DoubleDomain d2 = Domain.castToDoubleDomain(pd);
            if (d2 == null) {
                return null;
            }
            // zys:2010.8.11 ���������͵ı�����������
            if (ret instanceof IntegerDomain && pd instanceof PointerDomain) {
                try {
                    if (ret.isCanonical()) {
                        if (((IntegerDomain) ret).getMax() != -1) {
                            throw new RuntimeException("Pointer Domain can't in Multicative expression(such as 3*pinter)!" + pd);
                        }
                        ((PointerDomain) pd).setMinus(true);
                        ret = pd;// ָ��ļ�����������p1-p2,����ת��Ϊp1+(-1*p2)����ʱ��ֵ���㲻���������Է���ֵ����ν��ֻҪ������ָ�����;Ϳ�����
                    }
                } catch (RuntimeException e) {
                    ret = new DoubleDomain(Double.MIN_VALUE, Double.MAX_VALUE);
                    if (Config.TRACE)
                        logger.info(e.getMessage());
                    break;
                }
            } else if (p.getOperator().equals("*")) {
                ret = DoubleDomain.mul(d1, d2);
            } else {
                ret = DoubleDomain.div(d1, d2);
            }
        }
        return ret;
    }

    public boolean judgeIsComplicated() {
        int count = 0;
        if (this.powers.size() > 1)
            for (Power p : this.powers) {
                if (p.getSingleFactor() instanceof SymbolFactor)
                    count++;
            }
        if (count > 1)
            complicated = true;
        return complicated;
    }

    public HashSet<SymbolFactor> getAllSymbol() {
        HashSet<SymbolFactor> ret = new HashSet<SymbolFactor>();
        for (Power p : powers) {
            ret.addAll(p.getAllSymbol());
        }
        return ret;
    }
}
