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
 * ��֧�޽�ʵ����
 * 
 * @author Zmz
 * 
 */
public class NewBranchBound extends BB_onConstraint {

    private Set<SymbolFactor> symbolSet;
    private HashMap<SymbolFactor, Double> symbolNominalMap;
    private HashMap<SymbolFactor, Double> minSymbolRatio;
    private HashMap<SymbolFactor, Double> symbolRestraintMap;
    private HashMap<SymbolFactor, Integer> SymbolTendencyMap; // ���������Լ��� for
                                                              // ���ֻ���
    private HashMap<SymbolFactor, Integer> SymbolExpressionTendencyMap; // ���ű��ʽ������
                                                                        // for
                                                                        // ������ʱ��ֵѡȡ����
    private int SymbolExpressionTendency;
    private Boolean isRollBackSuccess;// ���˳ɹ���־��for�Ƿ���ж��ֻ���

    private SymbolDomainSet sfAndDomainAfterinit;// for ��Ԫ������Ϣ���
    private SymbolDomainSet tmp;// for ��Ԫ������Ϣ���

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
     * ��ɲ�������
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
                        tempvd = VariableDomain.newInstance(vnd, VariableSource.INPUT, node);// ��ʱ��lastNode�������ϣ�����һ��vexnode��û��Ӱ��
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
     * ����Լ����ȡ�� ÿ����������Ӧһ����ʽ���ϣ�����������乹�ɷ���Լ������ �����������㣬ǰ������ȥ����ÿһ���������ʽ�ļ��� ���Ϊ��
     * 1�����ʽ����������������� 2����������һ����ʽ���ϵ����������� 3����������������ٷ�֧���������������м���
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
            // ��Ҫ���շ�����������������ͽ�����ֵ��� add by Yaoweichang
            fanddsettmp.addDomain(sf, symbolDomainSet.getDomain(sf));
            if ((sf.getMark() & 2) == 2 && symbolDomainSet.getBitConsDomain(sf) != null)
                fanddsettmp.addBitConsDomain(sf, symbolDomainSet.getBitConsDomain(sf));
        }

        state.setFactorAndDomainSet(fanddsettmp);// ����factorAndDomainSet

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
     * ѡȡ���˷���
     * ȷ�����ŵĻ���ֵ
     * zmz
     */
    public FactorAndDomain rollback(RollBackState state, ArrayList<SymbolFactor> list) {
        // TODO Auto-generated method stub
        SymbolDomainSet factorAndDomain = state.getFactorAndDomainSet();
        isRollBackSuccess = super.getRollBackSuccess();
        SymbolExpressionTendencyMap = super.getSymExprTendencyMap();
        SymbolTendencyMap = super.getSymTendencyMap();
        for (SymbolFactor sf : list) {
            // �������if���жϣ�list�����ַ�����������������һЩ���ţ��ڻ���ʱ
            // �п��ܷ���list��ķ���factoranddomain������û�е�������ȽϺõĴ������Ƕ�factoranddomain��ķ���ֱ������Ȼ��洢ÿ�λ��˱仯
            if (factorAndDomain.getTable().keySet().contains(sf)) {
                // ��ȡ��ǰ���˷��ŵı��ʽ����������
                if (SymbolExpressionTendencyMap != null && SymbolExpressionTendencyMap.containsKey(sf)) {
                    SymbolExpressionTendency = SymbolExpressionTendencyMap.get(sf);
                } else {
                    SymbolExpressionTendency = 0;
                }
                Domain domain = factorAndDomain.getDomain(sf);
                // �����Ǵ�����ż����п��ܻᴫ���ǻ������͵ķ��ţ�Ӧ������Լ����ȡbug����֧�޽�ֻ�ܴ���������ͼ��������͵ĳ�Ա����ṹ�����ȣ��������ͣ�
                if (domain.getDomaintype() == DomainType.POINTER) {
                    continue;
                }
                // if(sf.toString().contains("[uni") && domain.isCanonical() ){
                // super.setUpdateFandD4members(true);
                // super.setSf4updateFandD4members(sf);
                // }
                if (!state.getConcreteFactors().get(sf)) { // �жϷ����Ƿ��Ѿ�����
                    // λ���㴦������ add by Yaoweichang
                    if ((sf.getMark() & 3) == 2) {// ֻ����λ���㣬ֱ�����ɾ�ȷ�����䣬��times=0��
                        String consValue = factorAndDomain.getBitConsDomain(sf).getConstraintDomain();
                        int value = super.BitParamGenerateValue(consValue, 0);
                        Domain concreteDomain = new IntegerDomain(value, value);
                        factorAndDomain.addDomain(sf, concreteDomain);
                        return new FactorAndDomain(sf, concreteDomain);
                    }
                    if ((sf.getMark() & 3) == 3) {// ͬʱ����λ�������ֵ������ add by Yaoweichang
                        String consValue = factorAndDomain.getBitConsDomain(sf).getConstraintDomain();
                        int i;
                        for (i = 0; i < 30; i++) {// ����ȡֵ30��
                            int value = super.BitParamGenerateValue(consValue, i);
                            if (Domain.castToIntegerDomain(domain).contains(value)) {// ���ɵ�����ֵ�ڵ�ǰԼ����Domain����
                                Domain concreteDomain = new IntegerDomain(value, value);
                                factorAndDomain.addDomain(sf, concreteDomain);
                                return new FactorAndDomain(sf, concreteDomain);
                            }
                        }
                        try {
                            if (i >= 30)
                                throw new Exception("λ������������ֵʧ�ܣ�");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // end λ���㴦������

                    // ���ַ������Ž������⴦�� begin
                    if (sf.getIndexType() == IndexType.uniSF) {
                        SymbolFactor down = sf.getLSF();
                        SymbolFactor up = sf.getUSF();
                        // unisf���½춼�Ƕ�ֵ��ʱ��ֹͣ����������ţ����������μ�ѡֵ
                        if (state.getFactorAndDomainSet().getTable().get(up).isCanonical() && state.getFactorAndDomainSet().getTable().get(down).isCanonical()) {
                            // ��uni���½춼ȷ���󣬹���uni������
                            IntegerDomain domainUp = (IntegerDomain) state.getFactorAndDomainSet().getTable().get(up);
                            IntegerDomain domainDown = (IntegerDomain) state.getFactorAndDomainSet().getTable().get(down);
                            domain = new IntegerDomain(domainDown.getMin(), domainUp.getMax());
                            factorAndDomain.addDomain(sf, domain);
                            super.setNeed2UpdateVD(true);// �ø���VD��־λ
                            return new FactorAndDomain(sf, domain);
                        } else {
                            continue;
                        }
                    }
                    if (sf.getIndexType() == IndexType.unsureSF) {
                        SymbolFactor n = sf.getRelySF();
                        // ��n�Ƕ�ֵ�ˣ�ֹͣ����unsuresf
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
                    // ���ַ������Ž������⴦�� end
                    if (domain.isCanonical()) {// domain�Ǿ�ȷֵ����ֱ�ӷ��أ����߽���ѡֵ����
                        factorAndDomain.addDomain(sf, domain);
                        return new FactorAndDomain(sf, domain);
                    } else {
                        Domain concreteDomain = domain.selectConcreteDomain(SymbolExpressionTendency);
                        // if (isRollBackSuccess == true) { //
                        // ���˳ɹ���־�����ڷ��ű��ʽ������Ϊnull��ѡֵΪselectConcreteDomain(0)���ѡֵ
                        // concreteDomain = domain
                        // .selectConcreteDomain(SymbolExpressionTendency);// domain�����䣬ѡֵ�����������ѡֵ
                        // } else { // ����ʧ�ܣ����ݸ��ݷ���������ѡֵ
                        // // ����ʱ���ֻ��˲����ã���Ϊ�����ò���·����������Ϣ��selectConcreteDomain(0)�������ѡֵ
                        // // Domain concreteDomain = domain
                        // // .selectConcreteDomainByPathTendency(SymbolTendencyMap
                        // // .get(sf));// ����ʧ�ܻ���ʱ���ݱ��������Խ��ж��ֻ���
                        // concreteDomain = domain
                        // .selectConcreteDomain(SymbolExpressionTendency);// ����ʧ�ܻ���ʱ���ݱ��������Խ��ж��ֻ���
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
     * �����ʼ��
     * ��ʼ����ֵ������Ϊ[-inf,+inf]
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
     * ����Լ����һ���Ż����� zmz
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
             * ��δ������ʼ����ָ��ȸ������ͷ��ŵ�inf���仮Ϊ-999~999
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
                setSymbolRestraintMap(di.getMaxRestrainValue());// for ��Ԫ������Ϣ���
            } catch (Exception e) {

            }
        }
        // factorAndDomain.sortByDomain();// ���������С����
        tmp = (SymbolDomainSet) factorAndDomain.clone();// for ��Ԫ������Ϣ���
        this.setSfAndDomainAfterinit(tmp);// for ��Ԫ������Ϣ���
    }

    // ���ַ�����ط��Ž�������
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
                // ����¼���ķ������±�
                IntegerDomain d1 = new IntegerDomain(0, 50);
                d1.setDomainStatus(0);
                state.getFactorAndDomainSet().addDomain(sf, d1);
                state.getConcreteFactors().put(sf, new Boolean("false"));
            } else {
                // ʣ�µ����Ӧ���������Ա
                IntegerDomain d2 = new IntegerDomain(-128, 127);
                d2.setDomainStatus(0);
                state.getFactorAndDomainSet().addDomain(sf, d2);
                state.getConcreteFactors().put(sf, new Boolean("false"));
            }
        }
        // �����Ƿ���Ҫ����һ���������㣬�¼���ķ��Ÿ�ʲô����ȡֵ����ȽϺ��ʣ�
        return state;
    }

    @Override
    public ArrayList<SymbolFactor> updateSortList(ArrayList<SymbolFactor> list1, ArrayList<SymbolFactor> list2) {
        // TODO Auto-generated method stub

        ArrayList<SymbolFactor> newList = new ArrayList<SymbolFactor>();
        for (SymbolFactor sf : list1) {
            if (sf.getIndexType() != IndexType.notIndex) {
                newList.add(sf); // �����Ҫ���µķ������±꽫�������µ��б�
            } else {
                list2.add(sf); // ���߽������뵽�Ѿ������б�ĩβ
            }
        }
        newList.addAll(list2); // ���Ѿ�������б���뵽�µ��б�
        return newList;
    }

}
