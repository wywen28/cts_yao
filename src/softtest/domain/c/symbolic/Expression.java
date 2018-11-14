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
    private boolean complicated = false;// 标明expression中含有由一个以上的变量组成Term 如a*b

    // add by zhouhb 2011.5.5
    // 添加空的构造函数用于对声明但未使用的结构体成员变量的表达式
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

    // add by jinkaifeng 2013.5.28 目的是获得Expression中对应的VND的string, 比如i_0xx+j_0xx，得到i+j
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
            ret = t1.compareTo(t2);// 2012-10-10 zhangxuzhou 存在bug-2与2相比返回0
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
     * 此方法用于处理表达式，合并参数项以及常数项
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

        // 处理不必要的嵌套表达式，和不必要的0项
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
        // modified by zhangxuzhou 2012-10-10 规定排序方法
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
        });// 合并同类项是否依赖于这个排序？2012-10-10 zhangxuzhou 这里需要改变排序的依据

        // 当前还没有处理使用分配率展开多项式乘法

        // 合并同类项
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

            // pbase:上一个项的因数 pcoe：上一个项的系数
            // cbase:当前项的因数 ccoe：当前项的系数
            if (!pbase.isEmpty()) {
                // 上一个项因数不为空
                if (pbase.equals(cbase)) {
                    // 因数相同，需要合并
                    if (previouseterm.getOperator().equals("-")) {
                        pcoe = pcoe.numMinus();
                    }
                    if (currentterm.getOperator().equals("-")) {
                        ccoe = ccoe.numMinus();
                    }
                    pcoe = pcoe.numAdd(ccoe);

                    if (pcoe.getDoubleValue() == 0) {
                        // 合并后系数为0，即整个项值变为0，考虑到0在加法中需要忽略
                        previouseterm = null;
                    } else if (pcoe.getDoubleValue() == 1) {
                        // 合并后系数为1，丢弃系数
                        previouseterm = new Term("+", pbase, true);
                    } else {
                        // 普通合并
                        pbase.add(0, new Power(pcoe, true));
                        previouseterm = new Term("+", pbase, true);

                    }
                } else {
                    // 因数不相同，上一个项可以放入listterm中了
                    if (previouseterm.getOperator().equals("-")) {
                        pcoe = pcoe.numMinus();
                    }
                    if (pcoe.getDoubleValue() == 0) {
                        // 系数为0，即整个项值变为0，考虑到0在加法中需要忽略
                        previouseterm = currentterm;
                    } else if (pcoe.getDoubleValue() == 1) {
                        // 系数为1，丢弃系数
                        listterm.add(new Term("+", pbase, true));
                        previouseterm = currentterm;
                    } else {
                        // 普通加入listterm
                        pbase.add(0, new Power(pcoe, true));
                        listterm.add(new Term("+", pbase, true));
                        previouseterm = currentterm;
                    }
                }
            } else {
                // 上一项因数为空
                if (cbase.isEmpty()) {
                    // 常量合并
                    if (previouseterm.getOperator().equals("-")) {
                        pcoe = pcoe.numMinus();
                    }
                    if (currentterm.getOperator().equals("-")) {
                        ccoe = ccoe.numMinus();
                    }
                    pcoe = pcoe.numAdd(ccoe);

                    if (pcoe.getDoubleValue() == 0) {
                        // 常量0
                        previouseterm = null;
                    } else {
                        // 普通常量
                        pbase.add(0, new Power(pcoe, true));
                        previouseterm = new Term("+", pbase, true);
                    }
                } else {
                    // 因数不相同，上一个常量项可以放入listterm中了
                    if (previouseterm.getOperator().equals("-")) {
                        pcoe = pcoe.numMinus();
                    }
                    if (pcoe.getDoubleValue() == 0) {
                        // 常量0
                        previouseterm = currentterm;
                    } else {
                        // 普通常量
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
        // 由于表达式other有可能计算不出来，故返回值也无法计算
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
        // Collections.sort(ret.terms);//这个sort方法也要改变 2012-10-10 zhangxuzhou
        // 使用规定的比较器，忽略系数，便于合并同类项
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
            // terms.add(new Term(new Power(f1,true),true));//2012-8-23注释掉
            // terms.add(new Term(new Power(f2,true),true));
            // return new Expression(terms,true);
            // add by xjx 2012-8-23
            /*
             * 作此修改是因为 原来的处理将(a+b)/(c+d)类型表达式处理成(c+d)+(a+b)类型。
             * 出现这个问题的原因是，除数的项大于1。
             * 经过修改后，(a+b)/(c+d)类型表达式会被处理成a/(c+d)+b/(c+d)形式，虽然修改后
             * 得到的表达式形式与原表达式并不完全一样，但却是等价的。
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
     * 合并位运算&的表达式
     * 
     * @param other
     * @return
     *         created by Yaoweichang on 2015-04-17 下午4:03:45
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
     * 合并位运算|的表达式
     * 
     * @param other
     * @return
     *         created by Yaoweichang on 2015-04-17 下午4:07:00
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
     * 合并位运算^的表达式
     * 
     * @param other
     * @return
     *         created by Yaoweichang on 2015-04-17 下午4:07:16
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
            // 当为指针类型时不做类型转换
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
                // changed 2012-12-21 同是integerDOmain类型的返回整形区间 之前都转换成double了
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
     * 判断当前符号表达式是否比较复杂。如果表达式包含两个和两个以上符号，或者符号表达式
     * 中项的系数不为+1 -1，或者符号表达式中项的指数不为1
     * modified by zxz,对于系数没有了限制，可以是任意系数
     * 2012-11-19 有问题
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
     * 检测是否包含由 a*b这种类型组成的term
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
     * 添加该方法的初衷是方便 约束和内存模型更新模块 的实现
     * 替换Factor
     * 
     * @param oldFactor 被替换的Factor
     * @param newFactor 要替换成的Factor
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
        this.flatten(0);// 化简
    }

}
