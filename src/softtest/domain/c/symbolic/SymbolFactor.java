package softtest.domain.c.symbolic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import softtest.domain.c.analysis.SymbolDomainSet;
import softtest.domain.c.interval.Domain;
import softtest.symboltable.c.LocalScope;
import softtest.symboltable.c.MethodNameDeclaration;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.CType;
import softtest.symboltable.c.Type.CType_BaseType;

public class SymbolFactor extends Factor {
    /**
     * mark标记符号参与运算类型:
     * 0代表未初始化；
     * 1代表只参与数值型运算；
     * 2代表只参与位运算；
     * 3代表同时参与数值型运算和位运算。
     * 
     * add by Yaoweichang
     **/
    private int mark;
    private String symbol;
    private boolean isBitCompute;// 标记该符号是否参与位运算 add by Yaoweichang
    // zys:记录符号的来源，是由哪个变量生成的，主要用于描述形参生成的符号，为测试用例生成服务
    private VariableNameDeclaration relatedVar;

    // 记录符号生成时的先后顺序
    private long Serial;

    /**
     * quoted 2013-5-23 注意不要直接调用这个构造方法，必须要设置ctype类型，否则会导致tostring方法
     * 中出现null的错误，再利用toString进行操作就会发生问题，尽量使用静态方法 genSymbol
     * 通常是利用genSymbol
     * 
     * @param symbol
     */
    public SymbolFactor(String symbol) {
        super();
        this.symbol = symbol + name_count++;
        this.Serial = name_count;
        this.setMark(0);
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
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
        SymbolFactor other = (SymbolFactor) obj;
        if (symbol == null) {
            return false;
        } else if (symbol.equals(other.symbol)) {
            return true;
        }
        return false;

    }

    /**
     * 设置该符号是否参与位运算
     * 
     * @param isBit
     *        created by Yaoweichang on 2015-04-11 下午10:11:29
     */
    public void setIsBitCompute(boolean isBit) {
        isBitCompute = isBit;
    }

    /**
     * 得到该符号是否参与位运算
     * 
     * @return
     *         created by Yaoweichang on 2015-04-16 下午3:51:05
     */
    public boolean getIsBitCompute() {
        return isBitCompute;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol + "_" + type.toString();// tostring方法中的元素不能是null
    }

    // add jinkaifeng 2013.5.28
    @Override
    public String getVND() {
        StringBuilder s = new StringBuilder();
        String[] temp = symbol.split("_", -1);
        for (int i = 0; i <= temp.length - 2; i++) {
            if (i == temp.length - 2)
                s.append(temp[i]);
            else
                s.append(temp[i] + "_");
        }

        return s.toString();

    }

    @Override
    public int compareTo(Factor o) {
        if (o instanceof NumberFactor) {
            return 1;
        }
        if (o instanceof SymbolFactor) {
            SymbolFactor f = (SymbolFactor) o;
            return symbol.compareTo(f.symbol);
        }

        return -1;
    }

    @Override
    public Factor flatten(int depth) {
        super.flatten(depth);
        return this;
    }

    private static long name_count = 0;

    public static SymbolFactor genSymbol(CType type) {
        String name = "S_";// 2013-1-3 zxz 去掉一次序号的增加
        SymbolFactor ret = new SymbolFactor(name);// 这里每次调用都会增加两次name_count 目的？
        if (type == null) {
            type = CType_BaseType.getBaseType("int");
        }
        ret.setType(type);
        return ret;
    }

    public static SymbolFactor genSymbol(CType type, String str) {
        String name = str + "_";// 2013-1-3 zxz 去掉一次序号的增加
        SymbolFactor ret = new SymbolFactor(name);// 这里每次调用都会增加两次name_count 目的？？
        if (type == null) {
            type = CType_BaseType.getBaseType("int");
        }
        ret.setType(type);
        return ret;
    }

    public boolean isParamSymbol() {
        if (relatedVar != null)
            return relatedVar.isParam();
        return false;
    }

    /**
     * 2013-5-22 zxz
     * 是否是全局变量 通过
     * 
     * @return
     */
    public boolean isExternVar() {
        if (relatedVar != null) {
            return (relatedVar.isExtern() || (relatedVar.getScope() instanceof SourceFileScope));
        }
        return false;
    }

    /** 为了将符号与其来源变量相关联　 */
    public static SymbolFactor genSymbol(CType type, VariableNameDeclaration var) {
        // String name=var.getImage()+"_"+name_count++;
        String name = var.getImage() + "_";// 2013-1-3 zxz 去掉一次序号的增加
        SymbolFactor ret = new SymbolFactor(name);// 这里每次调用都会增加两次name_count 目的？
        if (type == null) {
            type = CType_BaseType.getBaseType("int");
        }
        ret.setType(type);

        if (var.isParam() || var.isExtern() || var.getScope() instanceof SourceFileScope || var.getScope() instanceof LocalScope) {
            ret.setRelatedVar(var);
        }// 2013-5-22 zxz
         // 把全局变量也考虑进去，同全局的参数关联起来，目的用于获得symbol的VariableNameDeclaration信息


        return ret;
    }

    @Override
    public Domain getDomain(SymbolDomainSet ds) {
        if (ds != null) {
            return ds.getDomain(this);
        } else {
            return null;
        }
    }

    public Domain getDomainWithoutNull(SymbolDomainSet ds) {
        Domain domain = getDomain(ds);
        if (domain == null) {
            return Domain.getFullDomainFromType(type);
        }
        return domain;
    }

    public static void resetNameCount() {
        name_count = 0;
    }

    public VariableNameDeclaration getRelatedVar() {
        return relatedVar;
    }

    public long getSymbolSerial() {
        return this.Serial;
    }

    public void setRelatedVar(VariableNameDeclaration param) {
        this.relatedVar = param;
    }

    // add by jinkaifeng 2013.5.2
    private MethodNameDeclaration relatedMethod;

    public void setRelatedMethod(MethodNameDeclaration param) {
        this.relatedMethod = param;

    }

    public MethodNameDeclaration getRelatedMethod() {
        return this.relatedMethod;
    }

    // 仿照变量的符号生成
    public static SymbolFactor genSymbol(CType type, MethodNameDeclaration var) {
        // String name=var.getImage()+"_"+name_count++;
        String name = var.getImage() + "_";// 2013-1-3 zxz 去掉一次序号的增加
        SymbolFactor ret = new SymbolFactor(name);// 这里每次调用都会增加两次name_count 目的？
        if (type == null) {
            type = CType_BaseType.getBaseType("int");
        }
        ret.setType(type);

        /*
         * if(var.isParam()||var.isExtern()){
         * ret.setRelatedVar(var);
         * }
         */
        // 把全局变量也考虑进去，同全局的参数关联起来，目的用于获得symbol的VariableNameDeclaration信息


        return ret;
    }

    // add end
    // added by xjx 2012-3-12
    /**
     * @author xjx
     */
    private int level = -1; // the level of the Variable，initial value is -1

    private int presentNum = 0; // 变量出现的次数，初始值为0

    /*
     * 这个属性要不要呢？正在考虑
     */
    // private int priority; //优先级

    /**
     * list来存储于本变量相关的其他变量，因为同一个表达式中可能出现多个变量.<br>
     * 因为变量和符号是相对应的，而且在提取的变量都是以符号的形式存储的，因此存储<br>
     * 相关变量的符号。
     * 
     */
    // private List<VariableNameDeclaration> relatedVarlist = new
    // ArrayList<VariableNameDeclaration>();
    private List<SymbolFactor> relatedVarlist = new ArrayList<SymbolFactor>();


    public int getLevel() {
        return level;
    }

    /*
     * 要改
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * level的值自动加1
     */
    public void levelAddOne() {
        setLevel(level + 1);
    }

    public int getPresentNum() {
        return presentNum;
    }

    /*
     * 要改
     */
    public void setPresentNum(int presentNum) {
        this.presentNum = presentNum;
    }

    /**
     * presentNum的值自动加1
     */
    public void presentNumAddOne() {
        setPresentNum(presentNum + 1);
    }

    public List<SymbolFactor> getRelatedVarlist() {
        return relatedVarlist;
    }

    /*
     * 要改
     */
    public void setRelatedVarlist(List<SymbolFactor> list) {
        this.relatedVarlist = list;
    }

    /**
     * modified by xjx 2012-5-5
     * 添加一个与该变量相关的
     * 
     * @param symfat
     */
    public boolean addVarToRelatedVarlist(SymbolFactor symfat) {
        boolean hassymflag = false;
        for (int i = 0; i < this.getRelatedVarlist().size(); i++) {
            if (isEqual(this.getRelatedVarlist().get(i), symfat)) {
                hassymflag = true;
                break;
            }
        }
        if (!hassymflag) {
            this.getRelatedVarlist().add(symfat);
        }
        return !hassymflag;
    }

    /**
     * add by xjx 2012-5-5
     * 判断两个符号是否对应同一个变量，如果对应同一个变量返回true，否则返回false;
     * 
     * @param sym1
     * @param sym2
     * @return
     */
    public boolean isEqual(SymbolFactor sym1, SymbolFactor sym2) {
        String[] strarr = sym2.getSymbol().split("_");
        String headstr = strarr[0] + "_";
        if (headstr.equals("S_")) {// 不考虑S_开头的字符串
            if (sym1.getSymbol().equals(sym2.getSymbol()))
                return true;
            else
                return false;
        } else if (sym1.getSymbol().startsWith(headstr))
            return true;
        else
            return false;
    }

    /**
     * 标志是否是常量符号，默认不是
     * add by xujiaoxian 2012-9-21
     */
    public boolean isConstSym = false;

    /***
     * 增加字符串长度属性 add by zmz
     */

    private boolean strlentype = false;

    public void setStrlentype(boolean strlentype) {
        this.strlentype = strlentype;
    }


    public boolean getStrlentype() {
        return strlentype;
    }


    /**
     * 表明是符号的类型： zxz 为了进行字符串的实验 不在cts主分支 普通、unsureSF、UniSF
     * 
     * @author radix
     * 
     */
    private IndexType indexType = IndexType.notIndex;

    public enum IndexType {
        notIndex, Normal, uniSF, unsureSF
    }

    SymbolFactor uSF;
    SymbolFactor lSF;
    SymbolFactor relySF;

    public boolean setUSF(SymbolFactor lB, SymbolFactor uB) {
        if (this.indexType != IndexType.uniSF)
            return false;
        else {
            this.uSF = uB;
            this.lSF = lB;
            return true;
        }
    }

    public boolean setRelySF(SymbolFactor rely) {
        if (this.indexType != IndexType.unsureSF)
            return false;
        else {
            this.relySF = rely;
            return true;
        }
    }

    public SymbolFactor getRelySF() {
        if (this.indexType != IndexType.unsureSF)
            return null;
        else {
            return this.relySF;
        }
    }

    public SymbolFactor getUSF() {
        if (this.indexType != IndexType.uniSF)
            return null;
        else {
            return this.uSF;
        }
    }

    public SymbolFactor getLSF() {
        if (this.indexType != IndexType.uniSF)
            return null;
        else {
            return this.lSF;
        }
    }



    /**
     * 保存出现此下标的所有数组名 去重 若存在关联的数组，则说明这是一个下标变量。
     */
    protected Set<VariableNameDeclaration> relatedArrays = new HashSet<VariableNameDeclaration>();

    public boolean addRelatedArrays(VariableNameDeclaration arrayVND) {
        if (arrayVND == null)
            return false;
        return relatedArrays.add(arrayVND);
    }

    public Set<VariableNameDeclaration> setRelatedArrays(Set<VariableNameDeclaration> relatedArray) {
        return this.relatedArrays = relatedArray;
    }

    public Set<VariableNameDeclaration> getRelatedArrays() {
        return this.relatedArrays;
    }

    public boolean isIndexSF() {
        return relatedArrays.size() > 0 ? true : false;
    }

    public void setIndexType(IndexType indexType) {
        this.indexType = indexType;
    }

    public IndexType getIndexType() {
        return indexType;
    }

    /**
     * 对于uniSF而言 得到上下界之间的一组确定的整数下标，以整数的符号形式 已检查 1、下界小于上界 2、下界上界都是正值 3、上下界存在 未检查
     * 上界小于长度
     * 
     * @param concreteUpBound
     *        确定的上界
     * @param concreteDownDound
     *        确定的下界
     * @return 在上界和下界之间的一组整数
     */
    public List<NumberFactor> getIndexWithConcreteUpDown(Domain concreteUpBound, Domain concreteDownDound) {
        if (concreteUpBound == null || concreteDownDound == null || this.getIndexType() != IndexType.uniSF) {
            throw new NullPointerException("Up or Down bound is null");
        }
        int upBound = concreteUpBound.getConcreteDomain().intValue();
        int downBound = concreteDownDound.getConcreteDomain().intValue();
        if (downBound < upBound || upBound > 0 || downBound > 0) {
            ArrayList<NumberFactor> retIntIndexSymList = new ArrayList<NumberFactor>();
            for (int i = downBound; i <= upBound; i++) {
                NumberFactor Sf = new IntegerFactor(i);
                retIntIndexSymList.add(Sf);
            }
            return retIntIndexSymList;
        } else {
            throw new ArithmeticException("bound illegal");
        }
    }

    /**
     * 对于unsureSF而言 根据relyFactor的值n,生成n个下标符号
     * 
     * @param relyFactorValue
     * @return
     */
    public List<SymbolFactor> getIndexWithConcreteRelyValue(Domain relyFactorValue) {
        if (relyFactorValue == null || this.getIndexType() != IndexType.unsureSF)
            throw new NullPointerException("relyFactorValue is null");
        int genIndexSFTotal = relyFactorValue.getConcreteDomain().intValue();
        if (genIndexSFTotal > 0) {
            ArrayList<SymbolFactor> retIntIndexSymList = new ArrayList<SymbolFactor>();
            String rootSF = this.symbol;
            for (int i = 0; i < genIndexSFTotal; i++) {
                SymbolFactor Sf = SymbolFactor.genSymbol(CType_BaseType.getBaseType("int"), rootSF + "_" + String.valueOf(i));
                Sf.setRelatedArrays(this.getRelatedArrays());
                Sf.setIndexType(indexType.Normal);// 设置下标属性zmz
                retIntIndexSymList.add(Sf);
            }
            return retIntIndexSymList;
        } else {
            throw new ArithmeticException("relyFactorValue illegal");
        }
    }

    public boolean convertToUniSF(SymbolFactor lowerBound, SymbolFactor upperBound, VariableNameDeclaration arrayVar) {
        this.indexType = IndexType.uniSF;
        boolean flag = this.relatedArrays.add(arrayVar);
        return flag && this.setUSF(lowerBound, upperBound);
    }

    public boolean convertToUnsureSF(SymbolFactor relySF, VariableNameDeclaration arrayVar) {
        this.indexType = IndexType.unsureSF;
        boolean flag = this.relatedArrays.add(arrayVar);
        return flag && this.setRelySF(relySF);
    }

    public int getMark() {
        return mark;
    }

    public void setMark(int mark) {
        this.mark = mark;
    }
}
