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
     * mark��Ƿ��Ų�����������:
     * 0����δ��ʼ����
     * 1����ֻ������ֵ�����㣻
     * 2����ֻ����λ���㣻
     * 3����ͬʱ������ֵ�������λ���㡣
     * 
     * add by Yaoweichang
     **/
    private int mark;
    private String symbol;
    private boolean isBitCompute;// ��Ǹ÷����Ƿ����λ���� add by Yaoweichang
    // zys:��¼���ŵ���Դ�������ĸ��������ɵģ���Ҫ���������β����ɵķ��ţ�Ϊ�����������ɷ���
    private VariableNameDeclaration relatedVar;

    // ��¼��������ʱ���Ⱥ�˳��
    private long Serial;

    /**
     * quoted 2013-5-23 ע�ⲻҪֱ�ӵ���������췽��������Ҫ����ctype���ͣ�����ᵼ��tostring����
     * �г���null�Ĵ���������toString���в����ͻᷢ�����⣬����ʹ�þ�̬���� genSymbol
     * ͨ��������genSymbol
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
     * ���ø÷����Ƿ����λ����
     * 
     * @param isBit
     *        created by Yaoweichang on 2015-04-11 ����10:11:29
     */
    public void setIsBitCompute(boolean isBit) {
        isBitCompute = isBit;
    }

    /**
     * �õ��÷����Ƿ����λ����
     * 
     * @return
     *         created by Yaoweichang on 2015-04-16 ����3:51:05
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
        return symbol + "_" + type.toString();// tostring�����е�Ԫ�ز�����null
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
        String name = "S_";// 2013-1-3 zxz ȥ��һ����ŵ�����
        SymbolFactor ret = new SymbolFactor(name);// ����ÿ�ε��ö�����������name_count Ŀ�ģ�
        if (type == null) {
            type = CType_BaseType.getBaseType("int");
        }
        ret.setType(type);
        return ret;
    }

    public static SymbolFactor genSymbol(CType type, String str) {
        String name = str + "_";// 2013-1-3 zxz ȥ��һ����ŵ�����
        SymbolFactor ret = new SymbolFactor(name);// ����ÿ�ε��ö�����������name_count Ŀ�ģ���
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
     * �Ƿ���ȫ�ֱ��� ͨ��
     * 
     * @return
     */
    public boolean isExternVar() {
        if (relatedVar != null) {
            return (relatedVar.isExtern() || (relatedVar.getScope() instanceof SourceFileScope));
        }
        return false;
    }

    /** Ϊ�˽�����������Դ����������� */
    public static SymbolFactor genSymbol(CType type, VariableNameDeclaration var) {
        // String name=var.getImage()+"_"+name_count++;
        String name = var.getImage() + "_";// 2013-1-3 zxz ȥ��һ����ŵ�����
        SymbolFactor ret = new SymbolFactor(name);// ����ÿ�ε��ö�����������name_count Ŀ�ģ�
        if (type == null) {
            type = CType_BaseType.getBaseType("int");
        }
        ret.setType(type);

        if (var.isParam() || var.isExtern() || var.getScope() instanceof SourceFileScope || var.getScope() instanceof LocalScope) {
            ret.setRelatedVar(var);
        }// 2013-5-22 zxz
         // ��ȫ�ֱ���Ҳ���ǽ�ȥ��ͬȫ�ֵĲ�������������Ŀ�����ڻ��symbol��VariableNameDeclaration��Ϣ


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

    // ���ձ����ķ�������
    public static SymbolFactor genSymbol(CType type, MethodNameDeclaration var) {
        // String name=var.getImage()+"_"+name_count++;
        String name = var.getImage() + "_";// 2013-1-3 zxz ȥ��һ����ŵ�����
        SymbolFactor ret = new SymbolFactor(name);// ����ÿ�ε��ö�����������name_count Ŀ�ģ�
        if (type == null) {
            type = CType_BaseType.getBaseType("int");
        }
        ret.setType(type);

        /*
         * if(var.isParam()||var.isExtern()){
         * ret.setRelatedVar(var);
         * }
         */
        // ��ȫ�ֱ���Ҳ���ǽ�ȥ��ͬȫ�ֵĲ�������������Ŀ�����ڻ��symbol��VariableNameDeclaration��Ϣ


        return ret;
    }

    // add end
    // added by xjx 2012-3-12
    /**
     * @author xjx
     */
    private int level = -1; // the level of the Variable��initial value is -1

    private int presentNum = 0; // �������ֵĴ�������ʼֵΪ0

    /*
     * �������Ҫ��Ҫ�أ����ڿ���
     */
    // private int priority; //���ȼ�

    /**
     * list���洢�ڱ�������ص�������������Ϊͬһ�����ʽ�п��ܳ��ֶ������.<br>
     * ��Ϊ�����ͷ��������Ӧ�ģ���������ȡ�ı��������Է��ŵ���ʽ�洢�ģ���˴洢<br>
     * ��ر����ķ��š�
     * 
     */
    // private List<VariableNameDeclaration> relatedVarlist = new
    // ArrayList<VariableNameDeclaration>();
    private List<SymbolFactor> relatedVarlist = new ArrayList<SymbolFactor>();


    public int getLevel() {
        return level;
    }

    /*
     * Ҫ��
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * level��ֵ�Զ���1
     */
    public void levelAddOne() {
        setLevel(level + 1);
    }

    public int getPresentNum() {
        return presentNum;
    }

    /*
     * Ҫ��
     */
    public void setPresentNum(int presentNum) {
        this.presentNum = presentNum;
    }

    /**
     * presentNum��ֵ�Զ���1
     */
    public void presentNumAddOne() {
        setPresentNum(presentNum + 1);
    }

    public List<SymbolFactor> getRelatedVarlist() {
        return relatedVarlist;
    }

    /*
     * Ҫ��
     */
    public void setRelatedVarlist(List<SymbolFactor> list) {
        this.relatedVarlist = list;
    }

    /**
     * modified by xjx 2012-5-5
     * ���һ����ñ�����ص�
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
     * �ж����������Ƿ��Ӧͬһ�������������Ӧͬһ����������true�����򷵻�false;
     * 
     * @param sym1
     * @param sym2
     * @return
     */
    public boolean isEqual(SymbolFactor sym1, SymbolFactor sym2) {
        String[] strarr = sym2.getSymbol().split("_");
        String headstr = strarr[0] + "_";
        if (headstr.equals("S_")) {// ������S_��ͷ���ַ���
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
     * ��־�Ƿ��ǳ������ţ�Ĭ�ϲ���
     * add by xujiaoxian 2012-9-21
     */
    public boolean isConstSym = false;

    /***
     * �����ַ����������� add by zmz
     */

    private boolean strlentype = false;

    public void setStrlentype(boolean strlentype) {
        this.strlentype = strlentype;
    }


    public boolean getStrlentype() {
        return strlentype;
    }


    /**
     * �����Ƿ��ŵ����ͣ� zxz Ϊ�˽����ַ�����ʵ�� ����cts����֧ ��ͨ��unsureSF��UniSF
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
     * ������ִ��±������������ ȥ�� �����ڹ��������飬��˵������һ���±������
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
     * ����uniSF���� �õ����½�֮���һ��ȷ���������±꣬�������ķ�����ʽ �Ѽ�� 1���½�С���Ͻ� 2���½��Ͻ綼����ֵ 3�����½���� δ���
     * �Ͻ�С�ڳ���
     * 
     * @param concreteUpBound
     *        ȷ�����Ͻ�
     * @param concreteDownDound
     *        ȷ�����½�
     * @return ���Ͻ���½�֮���һ������
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
     * ����unsureSF���� ����relyFactor��ֵn,����n���±����
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
                Sf.setIndexType(indexType.Normal);// �����±�����zmz
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
