package unittest.branchbound.c;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import softtest.cfg.c.VexNode;
import softtest.config.c.Config;
import softtest.domain.c.analysis.SymbolDomainSet;
import softtest.domain.c.analysis.ValueSet;
import softtest.domain.c.interval.BitConstraintDomain;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.DomainType;
import softtest.domain.c.interval.DoubleDomain;
import softtest.domain.c.interval.DoubleInterval;
import softtest.domain.c.interval.IntegerDomain;
import softtest.domain.c.interval.IntegerInterval;
import softtest.domain.c.symbolic.RelationExpression;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.domain.c.symbolic.SymbolFactor.IndexType;
import softtest.symboltable.c.Scope;
import softtest.symboltable.c.SourceFileScope;
import softtest.symboltable.c.VariableNameDeclaration;
import unittest.path.analysis.variabledomain.VariableDomain;
import unittest.path.analysis.variabledomain.VariableSource;
import unittest.testcase.generate.paramtype.AbstractParamValue;
import unittest.testcase.generate.util.TestCaseNew;

/**
 * 分支限界实现类
 * 
 * @author Zmz
 * 
 */
public class NewBranchBound extends BB_onConstraint {

    private Set<SymbolFactor> symbolSet;
    private HashMap<SymbolFactor, Double> symbolNominalMap;
    private HashMap<SymbolFactor, Double> minSymbolRatio;
    private HashMap<SymbolFactor, Double> symbolRestraintMap;
    private HashMap<SymbolFactor, Integer> SymbolTendencyMap; // 符号正负性集合 for
                                                              // 二分回退
    private HashMap<SymbolFactor, Integer> SymbolExpressionTendencyMap; // 符号表达式正负性
                                                                        // for
                                                                        // 不回溯时初值选取依据
    private int SymbolExpressionTendency;
    private Boolean isRollBackSuccess;// 回退成功标志，for是否进行二分回退

    private SymbolDomainSet sfAndDomainAfterinit;// for 单元测试信息输出
    private SymbolDomainSet tmp;// for 单元测试信息输出

    public SymbolDomainSet getSfAndDomainAfterinit() {
        return sfAndDomainAfterinit;
    }

    public void setSfAndDomainAfterinit(SymbolDomainSet sfAndDomainAfterinit) {
        this.sfAndDomainAfterinit = sfAndDomainAfterinit;
    }

    public HashMap<SymbolFactor, Double> getSymbolNominalMap() {
        return symbolNominalMap;
    }

    public HashMap<SymbolFactor, Double> getMinSymbolRatio() {
        return minSymbolRatio;
    }

    public HashMap<SymbolFactor, Double> getSymbolRestraintMap() {
        return symbolRestraintMap;
    }

    public void setSymbolNominalMap(HashMap<SymbolFactor, Double> symbolNominalMap) {
        this.symbolNominalMap = symbolNominalMap;
    }

    public void setMinSymbolRatio(HashMap<SymbolFactor, Double> minSymbolRatio) {
        this.minSymbolRatio = minSymbolRatio;
    }

    public void setSymbolRestraintMap(HashMap<SymbolFactor, Double> symbolRestraintMap) {
        this.symbolRestraintMap = symbolRestraintMap;
    }

    public NewBranchBound(ConstraintExtractor constrains, ValueSet varAndVD, Set<SymbolFactor> sfset, VexNode node, boolean IsUATManual) {
        super(constrains, varAndVD, sfset, node, IsUATManual);
        // TODO Auto-generated constructor stub
    }

    public NewBranchBound(ConstraintExtractor ce, ValueSet vs, Hashtable<SymbolFactor, BitConstraintDomain> consTable) {
        super(ce, vs, consTable);
        // TODO Auto-generated constructor stub
    }

    @Override
    /**
     * 组成测试用例
     * zmz
     */
    public TestCaseNew generateTestcaseFromFactorDomain(RollBackState nextState) {
        // TODO Auto-generated method stub
        TestCaseNew tc = new TestCaseNew();
        boolean success = false;
        ValueSet vs = nextState.getVarAndVD();
        Map<String, AbstractParamValue> pts = new HashMap<String, AbstractParamValue>();
        Map<String, AbstractParamValue> annoyParams = new HashMap<String, AbstractParamValue>();
        VexNode node = super.getNode();
        Scope s = node.getTreenode().getScope();
        while (!(s instanceof SourceFileScope))
            s = s.getParent();
        VariableDomain tempvd;
        if (s != null && s instanceof SourceFileScope)
            for (VariableNameDeclaration vnd : s.getVariableDeclarations().keySet()) {
                if (!vnd.isLib() && vnd.isDeclOnly() && vnd.isExtern() == true) {
                    if (!vs.getTable().containsKey(vnd)) {
                        tempvd = VariableDomain.newInstance(vnd, VariableSource.INPUT, node);// 暂时用lastNode，理论上，用哪一个vexnode都没有影响
                        if (tempvd != null)
                            vs.addValue(vnd, tempvd);
                    }
                }
            }

        for (VariableNameDeclaration vnd : vs.getTable().keySet()) {
            VariableDomain vd = vs.getValue(vnd);
            if (vd.getVariableSource().isInput()) {
                boolean[] hasRing = {false};
                AbstractParamValue pv = vd.generateTestCaseNew(pts, vd.getVariableNameDeclaration().getName(), hasRing, annoyParams, nextState);
                pv.setVnd(vd.getVariableNameDeclaration());
                pv.setIsExtern(vd.getVariableNameDeclaration().isExtern());
                if (vnd.getScope() instanceof SourceFileScope) {
                    tc.addGlobalParam(pv);
                } else {
                    tc.addFunParam(pv);

                }
                if (hasRing[0]) {
                    tc.setHasRing(hasRing[0]);
                }
                success = true;
            }
        }
        return (success == true) ? tc : null;
    }

    @Override
    /**
     * @author tangyubin
     * 
     * 
     */
    public RollBackState updateConstrainsForComplicateFactor(RollBackState state) throws Exception {
        return new RollBackStateUpdater(state).updateForComplicatedFactor();
    }

    /**
     * 经过约束提取， 每个条件语句对应一组表达式集合，所有条件语句构成符号约束集合 进行区间运算，前向依次去分析每一个条件表达式的集合 层次为：
     * 1、表达式符号项的区间运算结果 2、条件语句的一组表达式集合的区间运算结果 3、根据条件语句的真假分支对区间运算结果进行计算
     */
    @Override
    public RollBackState calculateDomainForFactor(RollBackState state) {

        SymbolDomainSet symbolDomainSet = state.getFactorAndDomainSet();
        SymbolDomainSet fanddsettmp = state.getFactorAndDomainSet();
        ConstraintExtractor constrains = state.getConstrains();

        ConditionCalculate cc = new ConditionCalculate(constrains, symbolDomainSet);
        symbolDomainSet = cc.calculateDomain(cc.getInitSymbolDomainSet());
        state.setCurStateisValid(cc.isConstrainsIsValid());

        for (SymbolFactor sf : fanddsettmp.getTable().keySet()) {
            // 需要按照符号所参与的运算类型进行域值填充 add by Yaoweichang
            fanddsettmp.addDomain(sf, symbolDomainSet.getDomain(sf));
            if ((sf.getMark() & 2) == 2 && symbolDomainSet.getBitConsDomain(sf) != null)
                fanddsettmp.addBitConsDomain(sf, symbolDomainSet.getBitConsDomain(sf));
        }

        state.setFactorAndDomainSet(fanddsettmp);// 更新factorAndDomainSet

        return state;//
    }

    /**
     * @author tangyubin
     */
    @Override
    public void updateConstrainsForPrimitiveFactor(RollBackState stateToBeSimplyfied) throws Exception {
        stateToBeSimplyfied = new RollBackStateUpdater(stateToBeSimplyfied).updateForPrimitiveFactor();
    }

    @Override
    /**
     * 选取回退符号
     * 确定符号的回退值
     * zmz
     */
    public FactorAndDomain rollback(RollBackState state, ArrayList<SymbolFactor> list) {
        // TODO Auto-generated method stub
        SymbolDomainSet factorAndDomain = state.getFactorAndDomainSet();
        isRollBackSuccess = super.getRollBackSuccess();
        SymbolExpressionTendencyMap = super.getSymExprTendencyMap();
        SymbolTendencyMap = super.getSymTendencyMap();
        for (SymbolFactor sf : list) {
            // 下面这个if是判断：list由于字符串函数处理增加了一些符号，在回溯时
            // 有可能发生list里的符号factoranddomain集合里没有的情况。比较好的处理方案是对factoranddomain里的符号直接排序，然后存储每次回退变化
            if (factorAndDomain.getTable().keySet().contains(sf)) {
                // 获取当前回退符号的表达式符号正负性
                if (SymbolExpressionTendencyMap != null && SymbolExpressionTendencyMap.containsKey(sf)) {
                    SymbolExpressionTendency = SymbolExpressionTendencyMap.get(sf);
                } else {
                    SymbolExpressionTendency = 0;
                }
                Domain domain = factorAndDomain.getDomain(sf);
                // 这里是处理符号集里有可能会传来非基本类型的符号，应该属于约束提取bug，分支限界只能处理基本类型及复杂类型的成员对象结构变量等（基本类型）
                if (domain.getDomaintype() == DomainType.POINTER) {
                    continue;
                }
                // if(sf.toString().contains("[uni") && domain.isCanonical() ){
                // super.setUpdateFandD4members(true);
                // super.setSf4updateFandD4members(sf);
                // }
                if (!state.getConcreteFactors().get(sf)) { // 判断符号是否已经回退
                    // 位运算处理情形 add by Yaoweichang
                    if ((sf.getMark() & 3) == 2) {// 只参与位运算，直接生成精确的区间，即times=0。
                        String consValue = factorAndDomain.getBitConsDomain(sf).getConstraintDomain();
                        int value = super.BitParamGenerateValue(consValue, 0);
                        Domain concreteDomain = new IntegerDomain(value, value);
                        factorAndDomain.addDomain(sf, concreteDomain);
                        return new FactorAndDomain(sf, concreteDomain);
                    }
                    if ((sf.getMark() & 3) == 3) {// 同时参与位运算和数值型运算 add by Yaoweichang
                        String consValue = factorAndDomain.getBitConsDomain(sf).getConstraintDomain();
                        int i;
                        for (i = 0; i < 30; i++) {// 尝试取值30次
                            int value = super.BitParamGenerateValue(consValue, i);
                            if (Domain.castToIntegerDomain(domain).contains(value)) {// 生成的用例值在当前约束的Domain域中
                                Domain concreteDomain = new IntegerDomain(value, value);
                                factorAndDomain.addDomain(sf, concreteDomain);
                                return new FactorAndDomain(sf, concreteDomain);
                            }
                        }
                        try {
                            if (i >= 30)
                                throw new Exception("位运算生成用例值失败！");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // end 位运算处理情形

                    // 对字符串符号进行特殊处理 begin
                    if (sf.getIndexType() == IndexType.uniSF) {
                        SymbolFactor down = sf.getLSF();
                        SymbolFactor up = sf.getUSF();
                        // unisf上下届都是定值的时候停止回退这个符号，它本身并不参加选值
                        if (state.getFactorAndDomainSet().getTable().get(up).isCanonical() && state.getFactorAndDomainSet().getTable().get(down).isCanonical()) {
                            // 当uni上下届都确定后，构造uni的区间
                            IntegerDomain domainUp = (IntegerDomain) state.getFactorAndDomainSet().getTable().get(up);
                            IntegerDomain domainDown = (IntegerDomain) state.getFactorAndDomainSet().getTable().get(down);
                            domain = new IntegerDomain(domainDown.getMin(), domainUp.getMax());
                            factorAndDomain.addDomain(sf, domain);
                            super.setNeed2UpdateVD(true);// 置更新VD标志位
                            return new FactorAndDomain(sf, domain);
                        } else {
                            continue;
                        }
                    }
                    if (sf.getIndexType() == IndexType.unsureSF) {
                        SymbolFactor n = sf.getRelySF();
                        // 当n是定值了，停止回退unsuresf
                        if (state.getFactorAndDomainSet().getTable().get(n).isCanonical()) {
                            super.setNeed2UpdateVD(true);
                            return new FactorAndDomain(sf, domain);
                        } else {
                            continue;
                        }
                    }
                    if (sf.getIndexType() == IndexType.Normal) {
                        super.setNeed2UpdateVD(true);
                    }
                    // 对字符串符号进行特殊处理 end
                    if (domain.isCanonical()) {// domain是精确值区间直接返回，否者进行选值回退
                        factorAndDomain.addDomain(sf, domain);
                        return new FactorAndDomain(sf, domain);
                    } else {
                        Domain concreteDomain = domain.selectConcreteDomain(SymbolExpressionTendency);
                        // if (isRollBackSuccess == true) { //
                        // 回退成功标志，由于符号表达式正负性为null，选值为selectConcreteDomain(0)随机选值
                        // concreteDomain = domain
                        // .selectConcreteDomain(SymbolExpressionTendency);// domain是区间，选值。区间内随机选值
                        // } else { // 回退失败，回溯根据符号正负性选值
                        // // 回溯时二分回退不能用，因为现在拿不到路径正负性信息，selectConcreteDomain(0)，是随机选值
                        // // Domain concreteDomain = domain
                        // // .selectConcreteDomainByPathTendency(SymbolTendencyMap
                        // // .get(sf));// 回退失败回溯时依据变量正负性进行二分回退
                        // concreteDomain = domain
                        // .selectConcreteDomain(SymbolExpressionTendency);// 回退失败回溯时依据变量正负性进行二分回退
                        // }
                        if (concreteDomain == null) {
                            return null;
                        } else {
                            factorAndDomain.addDomain(sf, concreteDomain);
                            return new FactorAndDomain(sf, concreteDomain);
                        }
                    }
                }
            } else {
                continue;
            }
        }
        return null;
    }

    @Override
    /**
     * 区间初始化
     * 初始化数值型区间为[-inf,+inf]
     * zmz
     */
    public void initDomain(SymbolDomainSet factorAndDomain) {
        // TODO Auto-generated method stub
        Set<SymbolFactor> symbolSet = factorAndDomain.getTable().keySet();
        for (SymbolFactor sf : symbolSet) {
            Domain initDomain = Domain.getFullDomainFromType(sf.getType());
            factorAndDomain.addDomain(sf, initDomain);
        }
    }

    /**
     * 根据约束进一步优化区间 zmz
     */

    @Override
    public void optimizeDomain(SymbolDomainSet factorAndDomain, Set<List<RelationExpression>> set) {
        DomainInitialBaseConstraint di = new DomainInitialBaseConstraint(set);
        setSymbolNominalMap(di.getMaxNominalValue());
        setMinSymbolRatio(di.getMinSymbolRatio());
        Set<SymbolFactor> symbolSet = factorAndDomain.getTable().keySet();
        ArrayList<SymbolFactor> tmpList = new ArrayList<SymbolFactor>();
        for (SymbolFactor sf : symbolSet) {
            Double maxRestrainValue = di.getMaxRestrainValue().get(sf);
            if (maxRestrainValue == null) {
                maxRestrainValue = 9.0;

            }
            if (sf.getIndexType() != IndexType.notIndex) {
                Domain DomainAfterinit = new IntegerDomain(0, 10);
                DomainAfterinit.setDomainStatus(0);
                IndexType sfType = sf.getIndexType();
                switch (sfType) {
                    case unsureSF: {
                        factorAndDomain.addDomain(sf, DomainAfterinit);
                        factorAndDomain.addDomain(sf.getRelySF(), DomainAfterinit);
                        tmpList.add(sf.getRelySF());
                        continue;
                    }
                    case uniSF: {
                        factorAndDomain.addDomain(sf, DomainAfterinit);
                        factorAndDomain.addDomain(sf.getLSF(), DomainAfterinit);
                        factorAndDomain.addDomain(sf.getUSF(), DomainAfterinit);
                        tmpList.add(sf.getLSF());
                        tmpList.add(sf.getUSF());
                        continue;
                    }
                    case Normal: {
                        factorAndDomain.addDomain(sf, DomainAfterinit);
                        continue;
                    }
                }
            }
            if (sf.getStrlentype()) {
                IntegerDomain domaintmp = (IntegerDomain) factorAndDomain.getTable().get(sf);
                long minI = domaintmp.getMin();
                long maxI = domaintmp.getMax();
                if (minI == Long.MIN_VALUE) {
                    IntegerInterval firstInter = domaintmp.intervals.first();
                    domaintmp.intervals.remove(firstInter);
                    firstInter = new IntegerInterval(0, maxI);
                    domaintmp.intervals.add(firstInter);
                }
                // long maxI = domaintmp.getMax();
                if (maxI == Long.MAX_VALUE) {
                    IntegerInterval lastInter = domaintmp.intervals.last();
                    domaintmp.intervals.remove(lastInter);
                    long lastMin = lastInter.getMin();
                    lastInter = new IntegerInterval(lastMin, 11);
                    domaintmp.intervals.add(lastInter);
                }
                domaintmp.setDomainStatus(0);
                factorAndDomain.addDomain(sf, domaintmp);
                continue;
            }
            if (tmpList.contains(sf)) {
                continue;
            }
            Domain DomainAfterinit = Domain.initializeNew(factorAndDomain.getDomain(sf), maxRestrainValue, sf);
            /**
             * 将未正常初始化的指针等复杂类型符号的inf区间划为-999~999
             */
            switch (DomainAfterinit.getDomaintype()) {
                case INTEGER:
                    IntegerDomain id = (IntegerDomain) DomainAfterinit;
                    id.setDomainStatus(0);
                    long minI = id.getMin();
                    if (minI == Long.MIN_VALUE) {
                        IntegerInterval firstInter = id.intervals.first();
                        id.intervals.remove(firstInter);
                        minI = Config.MIN_INTEGER;
                        long firstMax = firstInter.getMax();
                        firstInter = new IntegerInterval(minI, firstMax);
                        id.intervals.add(firstInter);
                    }
                    long maxI = id.getMax();
                    if (maxI == Long.MAX_VALUE) {
                        IntegerInterval lastInter = id.intervals.last();
                        id.intervals.remove(lastInter);
                        maxI = Config.MAX_INTEGER;
                        long lastMin = lastInter.getMin();
                        lastInter = new IntegerInterval(lastMin, maxI);
                        id.intervals.add(lastInter);
                    }
                    break;
                case DOUBLE:
                    DoubleDomain dd = (DoubleDomain) DomainAfterinit;
                    double minD = dd.getMin();
                    dd.setDomainStatus(0);
                    if (minD == Double.NEGATIVE_INFINITY) {
                        DoubleInterval firstInter = dd.getIntervals().first();
                        dd.getIntervals().remove(firstInter);
                        minD = Config.MIN_DOUBLE;
                        double firstMax = firstInter.getMax();
                        firstInter = new DoubleInterval(minD, firstMax);
                        dd.getIntervals().add(firstInter);
                    }
                    double maxD = dd.getMax();
                    if (maxD == Double.POSITIVE_INFINITY) {
                        DoubleInterval lastInter = dd.getIntervals().last();
                        dd.getIntervals().remove(lastInter);
                        maxD = Config.MAX_DOUBLE;
                        double lastMin = lastInter.getMin();
                        lastInter = new DoubleInterval(lastMin, maxD);
                        dd.getIntervals().add(lastInter);
                    }
                    break;
                case POINTER:

                default:

            }
            try {
                factorAndDomain.addDomain(sf, DomainAfterinit);
                setSymbolRestraintMap(di.getMaxRestrainValue());// for 单元测试信息输出
            } catch (Exception e) {

            }
        }
        // factorAndDomain.sortByDomain();// 根据区间大小排序
        tmp = (SymbolDomainSet) factorAndDomain.clone();// for 单元测试信息输出
        this.setSfAndDomainAfterinit(tmp);// for 单元测试信息输出
    }

    // 对字符串相关符号进行排序
    @Override
    public ArrayList<SymbolFactor> sortFactor4String(Set<SymbolFactor> sfset) {
        // TODO Auto-generated method stub
        ArrayList<SymbolFactor> sflist = new ArrayList<SymbolFactor>();
        ArrayList<SymbolFactor> sortlist = new ArrayList<SymbolFactor>();
        sflist.addAll(sfset);
        for (SymbolFactor sf : sflist) {
            IndexType sfType = sf.getIndexType();
            switch (sfType) {
                case unsureSF: {
                    SymbolFactor relySF = sf.getRelySF();
                    if (relySF == null) {
                        continue;
                    } else {
                        sortlist.add(relySF);
                        sortlist.add(sf);
                        continue;
                    }
                }
                case uniSF: {
                    SymbolFactor usf = sf.getUSF();
                    SymbolFactor lsf = sf.getLSF();
                    if (usf == null && lsf == null) {
                        continue;
                    } else {
                        sortlist.add(lsf);
                        sortlist.add(usf);
                        sortlist.add(sf);
                        continue;
                    }
                }
                case Normal: {
                    sortlist.add(sf);
                    continue;
                }
                default:
                    break;
            }
            if (sf.toString().contains("[uni")) {
                sortlist.add(sf);
            }
        }
        if (sortlist == null) {
            return sflist;
        } else {
            sflist.removeAll(sortlist);
            if (sflist == null) {
                return sortlist;
            } else {
                sortlist.addAll(sflist);
                return sortlist;
            }
        }
    }

    @Override
    public RollBackState addNewSF4Rollback(RollBackState state, ArrayList<SymbolFactor> list) {
        // TODO Auto-generated method stub
        for (SymbolFactor sf : list) {
            if (sf.getIndexType() != IndexType.notIndex) {
                // 如果新加入的符号是下标
                IntegerDomain d1 = new IntegerDomain(0, 50);
                d1.setDomainStatus(0);
                state.getFactorAndDomainSet().addDomain(sf, d1);
                state.getConcreteFactors().put(sf, new Boolean("false"));
            } else {
                // 剩下的情况应该是数组成员
                IntegerDomain d2 = new IntegerDomain(-128, 127);
                d2.setDomainStatus(0);
                state.getFactorAndDomainSet().addDomain(sf, d2);
                state.getConcreteFactors().put(sf, new Boolean("false"));
            }
        }
        // 这里是否需要进行一遍区间运算，新加入的符号给什么样的取值区间比较合适？
        return state;
    }

    @Override
    public ArrayList<SymbolFactor> updateSortList(ArrayList<SymbolFactor> list1, ArrayList<SymbolFactor> list2) {
        // TODO Auto-generated method stub

        ArrayList<SymbolFactor> newList = new ArrayList<SymbolFactor>();
        for (SymbolFactor sf : list1) {
            if (sf.getIndexType() != IndexType.notIndex) {
                newList.add(sf); // 如果需要更新的符号是下标将它放入新的列表
            } else {
                list2.add(sf); // 否者将他加入到已经排序列表末尾
            }
        }
        newList.addAll(list2); // 将已经排序的列表加入到新的列表
        return newList;
    }

}
