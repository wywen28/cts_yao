package unittest.branchbound.c;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;

import softtest.cfg.c.VexNode;
import softtest.domain.c.analysis.SymbolDomainSet;
import softtest.domain.c.analysis.ValueSet;
import softtest.domain.c.interval.BitConstraintDomain;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.symbolic.Factor;
import softtest.domain.c.symbolic.NumberFactor;
import softtest.domain.c.symbolic.RelationExpression;
import softtest.domain.c.symbolic.SymbolExpression;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.domain.c.symbolic.SymbolFactor.IndexType;
import unittest.testcase.generate.util.TestCaseNew;

/**
 * ��֧�޽����ɲ��������Ļ��࣬���� 1.�����������ɵ������ 2.�ж�Լ���Ƿ������������ ���о���ķ���ʵ���ɼ̳���ȥʵ�֡�
 * 
 * @author radix
 * 
 */
public abstract class BB_onConstraint {
    private static Logger logger = Logger.getLogger(BB_onConstraint.class);
    /**
     * ��������ɲ��������׶ε��ã��򲻿���ִ���Ƿ�һ��
     */
    private boolean UATManualIntervn = false;
    /**
     * ����Լ��
     */
    private ConstraintExtractor constrains;
    /**
     * ���ź�ȡֵ��Ķ�Ӧ��ϵ����ֵ���ŵ�ֵ����Domain���������͵�ֵ����VD update 11.14 ȥ���������͵�VD
     * ֻ����ֵ���ʹ��ڷ���,ʹ��������SymbolDomainSet
     */
    private SymbolDomainSet factorAndDomain = new SymbolDomainSet();
    /**
     * ��¼�����Ƿ��Ѿ�ȡ��ȷ����ֵ�ˣ���Ҫ��factorAndDomain ͬʱ���и��£�ɾ�������ӵĲ���
     */
    private Hashtable<Factor, Boolean> concreteFactors = new Hashtable<Factor, Boolean>();
    /**
     * ���е��ⲿ���������Ӧһ��VD<br>
     * ��������û�з��ţ�����VD�ṹ������������ɲ�������<br>
     * �������ַ����飨�ַ�����,�����������ɹ����л�ı�VD�Ľṹ<br>
     * �������ͣ���������ȡ���������л��˹���
     */
    private ValueSet varAndVD = null;
    private TestCaseNew tc = null;
    private Hashtable<SymbolFactor, BitConstraintDomain> consDomainTable = null;

    private HashMap<SymbolFactor, Integer> SymbolTendencyMap; // ���������Լ��� for
                                                              // ���ֻ���
    private Boolean RollBackSuccess; // ���˳ɹ���־λ

    private HashMap<SymbolFactor, Integer> SymExprTendencyMap; // ���ű��ʽ�����Լ���
                                                               // for���˳�ֵѡȡ
    private RollBackState previousState;

    private int backtrackCount;// ��¼����ʱ���˵Ĵ���
    private int MAX_ROLLBACKCOUNT;// ��¼���˵�������add by baiyu
    private long MAX_BACKTRACKCOUNT;// ����ʱÿ����������������˴���
    private boolean success = true;
    private VexNode node;
    private Set<SymbolFactor> SymbolFactorSet;
    private ArrayList<SymbolFactor> sortedFbyD;
    private ArrayList<SymbolFactor> sortedFactor4String;

    public ArrayList<SymbolFactor> getSortedFactor4String() {
        return sortedFactor4String;
    }

    public void setSortedFactor4String(ArrayList<SymbolFactor> sortedFactor4String) {
        this.sortedFactor4String = sortedFactor4String;
    }

    public ArrayList<SymbolFactor> getSortedFbyD() {
        return sortedFbyD;
    }

    public void setSortedFbyD(ArrayList<SymbolFactor> sortedFbyD) {
        this.sortedFbyD = sortedFbyD;
    }

    public Set<SymbolFactor> getSymbolFactorSet() {
        return SymbolFactorSet;
    }

    public void setSymbolFactorSet(Set<SymbolFactor> symbolFactorSet) {
        SymbolFactorSet = symbolFactorSet;
    }

    public VexNode getNode() {
        return node;
    }

    public void setNode(VexNode node) {
        this.node = node;
    }

    /**
     * ��ȡValueSet
     * 
     * @return
     */
    public ValueSet getVarAndVD() {
        return varAndVD;
    }

    public void setVarAndVD(ValueSet varAndVD) {
        this.varAndVD = varAndVD;
    }


    public HashMap<SymbolFactor, Integer> getSymExprTendencyMap() {
        return SymExprTendencyMap;
    }

    public HashMap<SymbolFactor, Integer> getSymTendencyMap() {
        return SymbolTendencyMap;
    }

    public Boolean getRollBackSuccess() {
        return RollBackSuccess;
    }

    public void setRollBackSuccess(Boolean rollBackSuccess) {
        RollBackSuccess = rollBackSuccess;
    }

    /**
     * ��Ҫ��ִ��generate �ſ��Եõ�tc
     * 
     * @return
     */
    public TestCaseNew getTc() {
        return tc;
    }

    /**
     * �Ƿ���Ҫ����VD��־λ zmz
     */

    private boolean need2UpdateVD = false;

    public boolean isNeed2UpdateVD() {
        return need2UpdateVD;
    }

    public void setNeed2UpdateVD(boolean need2UpdateVD) {
        this.need2UpdateVD = need2UpdateVD;
    }


    /**
     * �Ƿ�uni��س�Ա��ͳһֵ
     */
    private boolean updateFandD4members = false;

    public boolean isUpdateFandD4members() {
        return updateFandD4members;
    }

    public void setUpdateFandD4members(boolean updateFandD4members) {
        this.updateFandD4members = updateFandD4members;
    }

    private SymbolFactor sf4updateFandD4members = null;

    public SymbolFactor getSf4updateFandD4members() {
        return sf4updateFandD4members;
    }

    public void setSf4updateFandD4members(SymbolFactor sf4updateFandD4members) {
        this.sf4updateFandD4members = sf4updateFandD4members;
    }

    /**
     * 
     * @param constrains
     *        ���ű��ʽ
     * @param varAndVD
     *        ������Ӧ�ĳ����ڴ�ģ��
     * @param sfset
     *        ���ű��ʽ�г��ֵ����з��ţ���Ҫ����Լ����⣨�������ͣ�
     */
    public BB_onConstraint(ConstraintExtractor constrains, ValueSet varAndVD, Set<SymbolFactor> sfset, VexNode node, boolean IsUATManual) {
        this.UATManualIntervn = IsUATManual;
        this.constrains = constrains;
        this.varAndVD = varAndVD;
        this.node = node;
        this.SymbolFactorSet = sfset;
        if (!sfset.isEmpty()) {
            for (SymbolFactor sf : sfset) {
                factorAndDomain.addDomain(sf, Domain.getEmptyDomainFromType(sf.getType())); // hashtable
                concreteFactors.put(sf, new Boolean("false"));
            }
        }

        // Collection<VariableDomain> VDs = varAndVD.getAllVariableDomain();
        // for (VariableDomain VD : VDs) {
        // // ����ǻ������ͣ���˵�����ڷ��ţ������ż�������˵ķ��ż���.���ǵ�����ǻ������͵ķ���û������Լ����
        // if (VD.getType().isBasicType() && (VD.getVariableSource().isInput() ||
        // VD.getVariableNameDeclaration().isExtern())) {
        // SymbolFactor sf = ((PrimitiveVariableDomain) VD)
        // .getSymbolFactor();
        // if (!factorAndDomain.getTable().containsKey(sf)) {
        // factorAndDomain.addDomain(sf,
        // Domain.getEmptyDomainFromType(sf.getType()));
        // concreteFactors.put(sf, new Boolean("false"));
        // }
        // }
        // }
    }

    public BB_onConstraint(ConstraintExtractor constrains, ValueSet varAndVD, Hashtable<SymbolFactor, BitConstraintDomain> consTable) {
        this.constrains = constrains;
        this.varAndVD = varAndVD;
        this.consDomainTable = consTable;
    }

    /**
     * Ϊ����λ����ı������ɺ��ʵ�����ֵ
     * 
     * @param consValue ������Լ����ʽ
     * @param times��times��Ϊ��Լ����������ֵ
     * @return
     *         created by Yaoweichang on 2015-11-17 ����4:26:25
     */
    public int BitParamGenerateValue(String consValue, int times) {
        int i, pos;
        int p = 0, q = 0, count = 0;
        p = times / 2;
        q = times % 2;
        int[] numberArr = new int[32];
        for (i = 0, pos = 0; i < numberArr.length; i++, pos++) {
            if (consValue.charAt(pos) == 'X')// Լ��ì�ܣ����������㵱ǰԼ��������ֵ���򷵻�-1
                return -1;
            if (consValue.charAt(pos) == 'T') {
                if (count == p) {
                    numberArr[i] = q;
                } else if (count > p) {
                    numberArr[i] = 0;
                } else
                    numberArr[i] = 1;
                count++;
            } else {
                numberArr[i] = consValue.charAt(pos) - '0';
            }
        }

        // ���������ַ���ת��Ϊ����
        int max = numberArr.length;
        int result = 0;
        for (i = max - 1; i >= 0; i--)
            result += Math.pow(2, i) * numberArr[i];
        return result;
    }

    /*
     * λ��������������ɵ������� add by Yaoweichang
     */
    // public TestCaseNew generate(int i){
    // SymbolFactor key = null;
    // BitConstraintDomain value = null;
    // String keyvalue = null;
    // Iterator iter = consDomainTable.keySet().iterator();
    // while (iter.hasNext()) {
    // key = (SymbolFactor)iter.next();
    // value = (BitConstraintDomain)consDomainTable.get(key);
    // VariableNameDeclaration vnd = key.getRelatedVar();
    // VariableDomain vd = varAndVD.getValue(vnd);
    // System.out.println("key=" +key+"   value"+value);
    // //���ɲ�������
    // TestCaseNew tcase = new TestCaseNew();
    //
    // if (vd.getVariableSource().isInput()) {
    // boolean[] hasRing = { false };
    // //Ϊ����λ����Ĳ�����������ֵ
    // keyvalue = BitParamGenerateValue(value);
    // PrimitiveParamValue ppv = new PrimitiveParamValue(vd.getVariableNameDeclaration().getName(),
    // vd.getVariableNameDeclaration().getType(), keyvalue);
    // AbstractParamValue pv = ppv;
    // pv.setVnd(vd.getVariableNameDeclaration());
    // pv.setIsExtern(vd.getVariableNameDeclaration().isExtern());
    // if (vnd.getScope() instanceof SourceFileScope) {
    // tcase.addGlobalParam(pv);
    // } else {
    // tcase.addFunParam(pv);
    //
    // }
    // if (hasRing[0]) {
    // tcase.setHasRing(hasRing[0]);
    // }
    // }
    // this.tc = tcase;
    // }
    // return tc;
    // }


    /*
     * �����������ɵ������̿��
     */
    public boolean generate() {
        this.backtrackCount = 0;
        this.MAX_ROLLBACKCOUNT = 20;// �����˴���
        MAX_BACKTRACKCOUNT = 500;// �����ݴ���
        /**
         * ��ȡ���ű��ʽ������
         */
        SymbolTendency st = new SymbolTendency(constrains, this.getSymbolFactorSet());
        SymExprTendencyMap = st.getSymExpressionTendency();
        SymbolTendencyMap = st.getSymbolTendencyMap();
        logger.info("���ű��ʽ������Ϊ��" + SymExprTendencyMap);
        /**
         * ��ʼ�����ŵ�ȡֵ�� [-inf,+inf]
         */
        initDomain(factorAndDomain);
        /**
         * ������ʼ״̬
         */
        FactorAndDomain currentFAndD = null;
        RollBackState initState = new RollBackState(constrains, factorAndDomain, concreteFactors, varAndVD, null);
        /**
         * ����Լ����һ���Ż�����
         */
        try {
            initState = calculateDomainForFactor(initState);
        } catch (Exception e) {
            logger.info(" �����ʼ������ʧ��");
            e.printStackTrace();
        }
        List<SymbolExpression> selist = constrains.getSymbolExpressions();
        for (SymbolExpression se : selist) {
            constrains.addConstraint(se.getRelationExpressions(), se.isTF());
        }
        optimizeDomain(initState.getFactorAndDomainSet(), constrains.getAllConstraintInPath().keySet());
        logger.info("�����ʼ������(ԭ·������)���Ϊ��" + initState.getFactorAndDomainSet());
        /**
         * ��������ʼ������ʧ�����³�ʼ������ȡֵ�� [-inf,+inf]�ڸ���Լ���Ż����� ����ֱ�Ӹ���Լ���Ż�����
         */
        if (initState.getFactorAndDomainSet().isContradict()) {
            initDomain(factorAndDomain);
            initState = new RollBackState(constrains, factorAndDomain, concreteFactors, varAndVD, null);
            optimizeDomain(initState.getFactorAndDomainSet(), constrains.getAllConstraintInPath().keySet());
        }
        /**
         * ��������ջ������ÿ�λ���״̬
         */
        Stack<RollBackState> stateStack = new Stack<RollBackState>();
        stateStack.add(initState);
        RollBackState currentState = stateStack.peek();
        RollBackState nextState = null;
        setRollBackSuccess(true);// ���˳ɹ���־λ

        /**
         * �Է��Ž�������
         */
        SymbolDomainSet FandDset = initState.getFactorAndDomainSet();
        setSortedFbyD(FandDset.sortByDomain()); // ���ݷ�����Ӧ�����С��������
        Set<SymbolFactor> sfset = new HashSet<SymbolFactor>();
        sfset.addAll(this.getSortedFbyD());
        setSortedFactor4String(this.sortFactor4String(sfset)); // ���ݷ������ȼ��������� for�ַ����±꼰��������
        /**
         * ���л���
         */
        while (currentState != null && !currentState.isFinalState() && success) {
            RollBackState currentStatetmp = (RollBackState) currentState.clone();
            currentFAndD = rollback(currentStatetmp, this.getSortedFactor4String());
            while (currentFAndD == null) {// ����ѡֵʧ�ܺ󣬽���������� add by baiyu 2015.6.16
                RollBackState tempState = null;
                logger.info("����ѡֵʧ��");
                try {
                    // nextState = backTrack(stateStack);// ���ݵ���ʼ״̬
                    tempState = newBackTrack(stateStack, currentStatetmp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (tempState == null) {
                    logger.info("����ʧ�ܣ���֧�޽����ʧ�ܣ�");
                    this.success = false;
                    return false;// add by baiyu 2015.6.16
                }
                currentFAndD = tempState.getCurrentFactorAndDomain();// ?
            }
            logger.info("��ǰ���˷���Ϊ��" + currentFAndD.getF() + "  ��ǰ����ֵΪ��" + currentFAndD.getD());
            logger.info("��һ��״̬Ϊ��" + currentState.getFactorAndDomainSet());
            currentStatetmp.setCurrentFactorAndDomain(currentFAndD);
            // �ж�ѡ��ֵ�Ƿ�����Լ��
            nextState = isConstraintValid(currentStatetmp, currentFAndD);
            // if(nextState != null && this.isUpdateFandD4members()){
            // for(SymbolFactor sf : this.getSortedFactor4String()){
            // if(this.isUpdateFandD4members() && nextState.getConcreteFactors().get(sf) &&
            // sf.getIndexType() == IndexType.uniSF){
            // //@���������
            // try {
            // nextState = new
            // RollBackStateUpdater((RollBackState)nextState.clone()).updateForUniValue(sf,
            // this.getSf4updateFandD4members());
            // this.setUpdateFandD4members(false);
            // } catch (Exception e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }
            // this.setUpdateFandD4members(false);
            // }
            // }
            // }
            /**
             * ���������Լ����Լ������ʧ�ܣ���������ì�ܣ�VD����ʧ�ܣ� ��Ҫ���л��� ���������ӻ��ݵĴ�������
             */
            if (nextState == null || !nextState.isCurStateisValid()) {
                try {
                    // nextState = backTrack(stateStack);// ���ݵ���ʼ״̬
                    nextState = newBackTrack(stateStack, (RollBackState) currentState.clone());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (nextState == null) {
                    logger.info("����ʧ�ܣ���֧�޽����ʧ�ܣ�");
                    this.success = false;
                    break;// add by baiyu 2015.6.16
                }
            }
            if (nextState.isFinalState()) {
                this.success = true;
                break;
            } else {
                stateStack.add(nextState);
                currentState = (RollBackState) nextState.clone();// ���˳ɹ��������״̬�������λ���״̬��Ϊ�´λ���״̬
            }
        }

        /**
         * ����ɹ�������ɲ������������򷵻�null
         */
        if (success == false)
            return false;
        else {
            // ��ԭ��strcatƴ�Ӻ���ַ�������add by tangyubin
            try {
                nextState = new RollBackStateUpdater(nextState).updateForStrCat();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this.tc = generateTestcaseFromFactorDomain(nextState);
            return true;
        }
    }

    /**
     * ��ɲ�������
     * 
     * @param
     * @return
     */
    public abstract TestCaseNew generateTestcaseFromFactorDomain(RollBackState nextState);

    /**
     * add by baiyu ѡȡ��һ��ì�ܱ������������
     */
    private SymbolFactor findFirstContradictSymbol(RollBackState currentState, ArrayList<SymbolFactor> list) {
        if (currentState == null)
            return null;
        SymbolFactor current = null;
        for (SymbolFactor sf : list) {
            if (!currentState.getConcreteFactors().get(sf)) {
                if (!sf.getIndexType().equals(IndexType.uniSF)) {
                    current = sf;// �����λ���ʧ�ܵķ��ű�����������ر����б���
                    break;
                }
            }
        }
        // current = currentState.getCurrentFactorAndDomain().getF();
        SymbolFactor returnSymbol = null;
        if (current == null)
            return null;
        Hashtable<Factor, Boolean> concreteFactorsAndValue = currentState.getConcreteFactors();
        List<Factor> concreteFactors = new ArrayList<Factor>();
        // �Ѿ���ֵ�ı�������
        for (Map.Entry<Factor, Boolean> entry : concreteFactorsAndValue.entrySet()) {
            if (entry.getValue()) {
                concreteFactors.add(entry.getKey());
            }
        }
        // ���û���Ѿ���ֵ�ı���������null
        if (concreteFactors.isEmpty())
            return null;
        // ������ǰ��������ر�������������Ѿ���ֵ��ֵ��Ϊ�յģ�˵���˷��ű���Ϊì�ܱ���
        for (SymbolFactor sf : current.getRelatedVarlist()) {
            if (concreteFactors.contains(sf) && currentState.getFactorAndDomainSet().getTable().get(sf).toString().equals("emptydomain")) {
                System.out.println("������" + sf + "������ì��");
                returnSymbol = sf;
                return returnSymbol;
            }
        }
        return returnSymbol;
    }


    public RollBackState newBackTrack(Stack<RollBackState> stateStack, RollBackState state) {
        setRollBackSuccess(false); // ���˴λ��˱�־Ϊʧ��
        // this.backtrackCount++;
        RollBackState RollBackAgainNextState = null;
        RollBackState nextState = null;
        // selectAll��ٱ�־�������ǰ��ȡֵ����٣���־��Ϊtrue,����Ϊ��Ծʽ�������ֻ��ݺͻ��˹��� add by baiyu 2015.6.11
        boolean selectAll = false;
        RollBackState RollBackAgainState = null;
        int rollbackCount4FailureSF = 1;// ��¼ÿ���������˵Ĵ���
        while (nextState == null && RollBackAgainNextState == null && rollbackCount4FailureSF < MAX_ROLLBACKCOUNT) {
            rollbackCount4FailureSF++;
            RollBackAgainState = (RollBackState) state.clone();
            try {
                FactorAndDomain RollBackAgaincurrentFAndD = rollback(RollBackAgainState, this.getSortedFactor4String());
                /**
                 * �������˸÷�������������ֵ�˴λ���ʧ��
                 */
                if (RollBackAgaincurrentFAndD == null) {
                    RollBackAgainNextState = null;
                    selectAll = true;
                    break;
                }
                logger.info("��ǰ���ݵĻ��˷���Ϊ��" + RollBackAgaincurrentFAndD.getF() + "  ��ǰ����ֵΪ��" + RollBackAgaincurrentFAndD.getD());
                logger.info("��һ��״̬Ϊ��" + state.getFactorAndDomainSet());
                RollBackAgainState.setCurrentFactorAndDomain(RollBackAgaincurrentFAndD);
                // �ж�ѡ��ֵ�Ƿ�����Լ��
                RollBackAgainNextState = isConstraintValid(RollBackAgainState, RollBackAgaincurrentFAndD);
                if (RollBackAgainNextState == null) {
                    continue;
                }
                nextState = (RollBackState) RollBackAgainNextState.clone();
            } catch (RuntimeException e) {
                RollBackAgainNextState = null;
                e.printStackTrace();
            }
        }
        // �������while�˳�֮���������������ٶ�֮ǰѡֵ����ȷ�ķ����Ѿ�����20�� nextState ==null
        // ��ʱҪ���ݣ���״̬ջ�ﵯ���µ�״̬������һ���˱�����������ѡֵ��nextState��=null���ص������̼���ѡֵ
        // bug?״̬ջ������������䣬�ⲿ���˿���list��������Լ���������� �µķ������߲�һ��
        if (this.backtrackCount < MAX_BACKTRACKCOUNT) {
            if (nextState != null) {
                setRollBackSuccess(true);
                return nextState;
            } else {
                RollBackState statetmp1 = null;
                if (selectAll || rollbackCount4FailureSF >= 20) {// ���������������ʱ���ǻ��ݵ�״̬ add by baiyu
                    backtrackCount++;// ��¼���ݴ���
                    SymbolFactor backTrackSymbol = findFirstContradictSymbol(RollBackAgainState, this.getSortedFactor4String());// ��Ծʽ���ݣ�������ݱ���
                    if (backTrackSymbol != null) {
                        while (!stateStack.empty() && !stateStack.peek().isInitState() && !stateStack.peek().getCurrentFactorAndDomain().getF().equals(backTrackSymbol)) {
                            statetmp1 = stateStack.pop();
                        }
                        if (!stateStack.empty() && !stateStack.peek().isInitState() && stateStack.peek().getCurrentFactorAndDomain().getF().equals(backTrackSymbol)) {
                            stateStack.pop();
                            statetmp1 = stateStack.pop();
                        }
                        setRollBackSuccess(true);
                        return statetmp1;
                    }
                }

                if (stateStack.isEmpty()) {
                    return null;
                } else {
                    // ��ʱ���˲��ɹ������ص�Ӧ��ѡ���µĻ��˱������״̬������ջ�� add by baiyu
                    if (stateStack.peek().getCurrentFactorAndDomain() == null) {
                        // ��ǰջ��ֻ�г�ʼ״̬��ȡ�����о��������������ѭ������ͣpeek��ʼ״̬�����ȷ������ʧ�ܴ������ƣ���
                        statetmp1 = newBackTrack(stateStack, stateStack.peek());
                        return statetmp1;
                    } else {
                        stateStack.pop(); // ջ������ʧ�ܷ���ǰһ������ѡֵ�ɹ���״̬������ ������ջ����ǰһ���ŵĳ�ʼ״̬
                        setRollBackSuccess(true);
                        statetmp1 = newBackTrack(stateStack, stateStack.peek());
                        return statetmp1;
                        // return stateStack.pop();

                    }
                }
            }
        } else {
            return null;
        }
    }

    public RollBackState backTrack(Stack<RollBackState> stateStack) throws RunTimeOverLimitException, BackTrackOverTimesException {

        if (stateStack.isEmpty()) {
            return null;
        } else {
            previousState = stateStack.pop();
        }
        setRollBackSuccess(false); // ���˴λ��˱�־Ϊʧ��
        RollBackState currentStatetmp = (RollBackState) previousState.clone();
        FactorAndDomain currentFAndD = rollback(currentStatetmp, this.getSortedFactor4String());
        RollBackState nextState = null;
        if (currentFAndD == null) {
            logger.info("����ѡֵʧ��");
        } else {
            logger.info("��ǰ���ݵĻ��˷���Ϊ��" + currentFAndD.getF() + "  ��ǰ����ֵΪ��" + currentFAndD.getD());
            logger.info("��һ��״̬Ϊ��" + previousState.getFactorAndDomainSet());
            currentStatetmp.setCurrentFactorAndDomain(currentFAndD);
            // �ж�ѡ��ֵ�Ƿ�����Լ��
            nextState = isConstraintValid(currentStatetmp, currentFAndD);
        }
        this.backtrackCount++;
        RollBackState RollBackAgainNextState = null;
        while (nextState == null && RollBackAgainNextState == null && backtrackCount < MAX_BACKTRACKCOUNT) {
            this.backtrackCount++;
            RollBackState RollBackAgainState = (RollBackState) previousState.clone();
            try {
                FactorAndDomain RollBackAgaincurrentFAndD = rollback(RollBackAgainState, this.getSortedFactor4String());
                /**
                 * �������˸÷�������������ֵ�˴λ���ʧ��
                 */
                if (RollBackAgaincurrentFAndD == null) {
                    RollBackAgainNextState = null;
                    break;
                }
                logger.info("��ǰ���ݵĻ��˷���Ϊ��" + RollBackAgaincurrentFAndD.getF() + "  ��ǰ����ֵΪ��" + RollBackAgaincurrentFAndD.getD());
                logger.info("��һ��״̬Ϊ��" + previousState.getFactorAndDomainSet());
                RollBackAgainState.setCurrentFactorAndDomain(RollBackAgaincurrentFAndD);
                // �ж�ѡ��ֵ�Ƿ�����Լ��
                RollBackAgainNextState = isConstraintValid(RollBackAgainState, RollBackAgaincurrentFAndD);
            } catch (RuntimeException e) {
                RollBackAgainNextState = null;
                e.printStackTrace();
            }
        }

        if (nextState != null) {
            stateStack.add(nextState);
            setRollBackSuccess(true); // ���˴λ��˱�־Ϊ�ɹ�
            return nextState;
        }

        if (RollBackAgainNextState == null) {
            // return null;
            return backTrack(stateStack); // ���λ���ʧ�� �ڻ��ݵ���һ״̬��
        } else {
            /**
             * ������λ��ݳɹ������ɹ���״̬��������������
             */
            stateStack.add(RollBackAgainNextState);
            setRollBackSuccess(true); // ���˴λ��˱�־Ϊ�ɹ�
            return RollBackAgainNextState;
        }
    }

    /**
     * �ж���ǰ�Ļ���ֵ�ڵ�ǰ��Լ��״̬�Ƿ��ܹ����� �ж���������Ҫ�����µ�״̬��״̬�е�ÿһ����������Ҫclone��һ��������Ȼ�����µ�״̬
     * �����ж��Ŀ�ܣ�����ʵ���ڼ̳��� �����������<br>
     * �������ͣ�<br>
     * 1.1���ж��ṹ�����ɷ��ţ��ַ�����Ա�� ���� 1.2���ݻ������͵�ȡֵ����Լ�� 2��������
     * 
     * @param currentState
     * @param currentFactorAndDomain
     * @return
     * @throws Exception
     */
    private RollBackState isConstraintValid(RollBackState retState, FactorAndDomain currentFAndD) {

        SymbolFactor curSymbol = currentFAndD.f;
        if (curSymbol.getIndexType() != IndexType.notIndex) {
            /**
             * ��ǰ���˷������±����str_i��unsure��unin
             * ͨ��need2UpdateVD��T����F�ж�unsure��unin�������ķ����Ƿ�������
             * �����ɺ���Ҫ���³����ڴ�VD
             */
            if (this.need2UpdateVD) {
                try {
                    IndexType sfType = curSymbol.getIndexType();
                    switch (sfType) {
                        case unsureSF:// �ǵ����ຯ�������ķ���
                        {
                            SymbolFactor relySF = curSymbol.getRelySF(); // �Ѿ���������n
                            // ����n����n���±���ţ����ǵ��������±�normal
                            List<SymbolFactor> ConcreteIndexList = curSymbol.getIndexWithConcreteRelyValue(relySF.getDomain(retState.getFactorAndDomainSet()));
                            // ���³����ڴ棬��Լ����Լ������������str[unsure]>0��Ϊstr[index1]>0...str[indexn]>0���˴��ݲ���str[index]
                            // ����needAddSFlist
                            // ��indexȷ�����ڼ�
                            retState = new RollBackStateUpdater(retState).updateForUnsureSF(ConcreteIndexList);
                            this.setNeed2UpdateVD(false);
                            break;
                        }
                        case uniSF: {
                            SymbolFactor LSF = curSymbol.getLSF();// �½����
                            SymbolFactor USF = curSymbol.getUSF();// �½����
                            // ���ɴ�LSF��USF��ȷ�����ֵ��±�
                            List<NumberFactor> SymbolIndexList = curSymbol.getIndexWithConcreteUpDown(retState.getFactorAndDomainSet().getDomain(USF), retState.getFactorAndDomainSet().getDomain(LSF));
                            // ���³����ڴ桢Լ����needAddSFlist��Լ����������up=1��down=0����str[unin] != c ��Ϊstr[0] !=
                            // c str[1] != c@tangyubin
                            RollBackState tmpState = new RollBackStateUpdater(retState).updateForUniSF(SymbolIndexList);
                            retState = addNewSF4Rollback(tmpState, tmpState.getNeedAddSFlist());
                            this.setNeed2UpdateVD(false);
                            // ���¼�����Ž�������
                            ArrayList<SymbolFactor> list1 = new ArrayList<SymbolFactor>();
                            list1 = this.updateSortList(retState.getNeedAddSFlist(), this.getSortedFactor4String());
                            setSortedFactor4String(list1);
                            break;
                        }
                        case Normal: {
                            // ������������str_i��indexn���³����ڴ桢Լ����needAddSFlist
                            RollBackState tmpState = new RollBackStateUpdater(retState).updateForNomal();
                            // RollBackState tmpState = new
                            // RollBackStateUpdater(retState).updateForComplicatedFactor();
                            retState = addNewSF4Rollback(tmpState, tmpState.getNeedAddSFlist());
                            this.setNeed2UpdateVD(false);
                            // ���¼�����Ž�������
                            ArrayList<SymbolFactor> list1 = new ArrayList<SymbolFactor>();
                            list1 = this.updateSortList(retState.getNeedAddSFlist(), this.getSortedFactor4String());
                            setSortedFactor4String(list1);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // ֻ������������
                retState = calculateDomainForFactor(retState);
                logger.info("��������Ľ���ǣ�" + retState.getFactorAndDomainSet());
                if (retState.getFactorAndDomainSet().isContradict()) {
                    return null;
                } else {
                    return retState;
                }
            }
        } else {
            /**
             * ��ǰ���˷����Ƿ��±���Ű�����ͨ���ź��ַ������ȷ��ţ���ͨ���Ű�����ͨSF��up��down��unsure������n��strlen
             * ��ͨ���Ÿ���Լ����strlen����VD
             */
            if (curSymbol.getStrlentype()) {
                // ����strlen���³����ڴ棬����Լ����,���ø���needAddSFlist@tangyubin
                try {
                    retState = new RollBackStateUpdater(retState).updateForStrlenType();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                // ����Լ��@tangyubin
                try {
                    // retState = new RollBackStateUpdater(retState).updateForPrimitiveFactor();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        // debug here
        retState = calculateDomainForFactor(retState);
        logger.info("��������Ľ���ǣ�" + retState.getFactorAndDomainSet());
        if (retState.getFactorAndDomainSet().isContradict()) {
            return null;
        } else {
            // ���˳ɹ� �����˷��ű�־Ϊtrue
            retState.getConcreteFactors().remove(curSymbol);
            retState.getConcreteFactors().put(curSymbol, new Boolean("true"));
            return retState;
        }
    }


    /**
     * ����������ڴ�ģ�ͺ�Լ�� ���ݸ������͵ķ���ȡֵ������ȡ��Ա�������*��.��[]����Ϊ�������͵ĳ�Ա���ɷ��ţ�����VD������Լ��
     * ���ͳ�Ա������Ĺ����У�����������µķ��ţ���Ҫ�����ż��뵽factorAndDomain������ ���� 2014-11-13 ����
     * arr[$i]�����ı��ʽ����Ҫ�ı������VD ����$i��ȫ�Ʒ���
     * �п����ںϲ�����ʱ�������󣬴�ʱ����retstate�е�valueset��null
     * 
     * @param state
     * @return
     * @throws Exception
     */
    public abstract RollBackState updateConstrainsForComplicateFactor(RollBackState state) throws Exception;

    /**
     * ��������<br>
     * �������ì�ܣ��ͽ�retState �е�factorAndDomain ����Ϊnull
     * 
     * @param state
     * @return
     */
    public abstract RollBackState calculateDomainForFactor(RollBackState state);

    /**
     * ������ֵ���͵�ȡֵ���ɷ��ţ���Լ������ -�����
     * 
     * @param stateToBeSimplyfied
     * @throws Exception
     */
    public abstract void updateConstrainsForPrimitiveFactor(RollBackState stateToBeSimplyfied) throws Exception;

    /**
     * ���� ѡ���ź�ѡֵ<br>
     * 1ѡ����<br>
     * 2ѡֵ<br>
     * 2.1�������ͣ�������<br>
     * 
     * @return
     */
    public abstract FactorAndDomain rollback(RollBackState state, ArrayList<SymbolFactor> list);

    /**
     * ���������С���� ˳��ѡֵ
     * ���� ѡ���ź�ѡֵ<br>
     * 1ѡ����<br>
     * 2ѡֵ<br>
     * 2.1�������ͣ�������<br>
     * 
     * @return
     */

    /**
     * ��ʼ��factorAndDomain<br>
     * 1���ýṹ<br>
     * 2���ýṹ�г�Ա�ĳ�ʼ��<br>
     * 2.1��ֵ���ͣ�ʹ�������ʾΪ[-inf,+inf]<br>
     * 2.2ָ�����ͣ����ڽṹ��ֵ��<br>
     * 3�Ż�ֵ��<br>
     */
    public abstract void initDomain(SymbolDomainSet factorAndDomain);

    /**
     * ����Լ����һ���Ż�����
     * 
     * @param factorAndDomain
     * @param set
     */
    public abstract void optimizeDomain(SymbolDomainSet factorAndDomain, Set<List<RelationExpression>> set);

    /**
     * ���ַ�����ط��Ű��ջ������ȼ���������
     */
    public abstract ArrayList<SymbolFactor> sortFactor4String(Set<SymbolFactor> sfset);

    /**
     * �����µķ��ŵ����������У��±�ĳ�ʼ�����ʼ��Ϊ[0,50]�������Ա��ʼ�����ʼ��Ϊ[-128, 127]
     */
    public abstract RollBackState addNewSF4Rollback(RollBackState state, ArrayList<SymbolFactor> list);

    /**
     * �����µķ��ŵ��Ѹ����±����ȼ���������� list1Ϊ���������б�list2Ϊ����Ļ��˷����б�
     */
    public abstract ArrayList<SymbolFactor> updateSortList(ArrayList<SymbolFactor> list1, ArrayList<SymbolFactor> list2);
}


class FactorAndDomain implements Cloneable {
    SymbolFactor f;
    Domain d;

    public SymbolFactor getF() {
        return f;
    }

    public void setF(SymbolFactor f) {
        this.f = f;
    }

    public Domain getD() {
        return d;
    }

    public void setD(Domain d) {
        this.d = d;
    }

    public FactorAndDomain(SymbolFactor f, Domain d) {
        this.f = f;
        this.d = d;
    }

    public Object clone() {
        FactorAndDomain FandD = null;
        try {
            FandD = (FactorAndDomain) super.clone();
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return FandD;

    }

}
