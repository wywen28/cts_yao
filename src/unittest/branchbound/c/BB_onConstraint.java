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
 * 分支限界生成测试用例的基类，包含 1.测试用例生成的主框架 2.判定约束是否满足的主流程 其中具体的方法实现由继承类去实现。
 * 
 * @author radix
 * 
 */
public abstract class BB_onConstraint {
    private static Logger logger = Logger.getLogger(BB_onConstraint.class);
    /**
     * 如果是生成测试用例阶段调用，则不考虑执行是否一致
     */
    private boolean UATManualIntervn = false;
    /**
     * 符号约束
     */
    private ConstraintExtractor constrains;
    /**
     * 符号和取值域的对应关系：数值符号的值域是Domain；复合类型的值域是VD update 11.14 去掉复杂类型的VD
     * 只有数值类型存在符号,使用已有类SymbolDomainSet
     */
    private SymbolDomainSet factorAndDomain = new SymbolDomainSet();
    /**
     * 记录符号是否已经取了确定的值了，需要和factorAndDomain 同时进行更新，删除，增加的操作
     */
    private Hashtable<Factor, Boolean> concreteFactors = new Hashtable<Factor, Boolean>();
    /**
     * 所有的外部输入变量对应一个VD<br>
     * 复杂类型没有符号，保留VD结构，用于最终组成测试用例<br>
     * 特例：字符数组（字符串）,测试用例生成过程中会改变VD的结构<br>
     * 基本类型，将符号提取出来，进行回退过程
     */
    private ValueSet varAndVD = null;
    private TestCaseNew tc = null;
    private Hashtable<SymbolFactor, BitConstraintDomain> consDomainTable = null;

    private HashMap<SymbolFactor, Integer> SymbolTendencyMap; // 符号正负性集合 for
                                                              // 二分回退
    private Boolean RollBackSuccess; // 回退成功标志位

    private HashMap<SymbolFactor, Integer> SymExprTendencyMap; // 符号表达式正负性集合
                                                               // for回退初值选取
    private RollBackState previousState;

    private int backtrackCount;// 记录回溯时回退的次数
    private int MAX_ROLLBACKCOUNT;// 记录回退的最大次数add by baiyu
    private long MAX_BACKTRACKCOUNT;// 回溯时每个符号允许的最大回退次数
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
     * 获取ValueSet
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
     * 需要先执行generate 才可以得到tc
     * 
     * @return
     */
    public TestCaseNew getTc() {
        return tc;
    }

    /**
     * 是否需要更新VD标志位 zmz
     */

    private boolean need2UpdateVD = false;

    public boolean isNeed2UpdateVD() {
        return need2UpdateVD;
    }

    public void setNeed2UpdateVD(boolean need2UpdateVD) {
        this.need2UpdateVD = need2UpdateVD;
    }


    /**
     * 是否将uni相关成员置统一值
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
     *        符号表达式
     * @param varAndVD
     *        变量对应的抽象内存模型
     * @param sfset
     *        符号表达式中出现的所有符号，需要进行约束求解（基本类型）
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
        // // 如果是基本类型，则说明存在符号，将符号加入待回退的符号集合.考虑的情况是基本类型的符号没出现在约束中
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
     * 为参与位运算的变量生成合适的用例值
     * 
     * @param consValue 变量的约束形式
     * @param times第times次为该约束生成用例值
     * @return
     *         created by Yaoweichang on 2015-11-17 下午4:26:25
     */
    public int BitParamGenerateValue(String consValue, int times) {
        int i, pos;
        int p = 0, q = 0, count = 0;
        p = times / 2;
        q = times % 2;
        int[] numberArr = new int[32];
        for (i = 0, pos = 0; i < numberArr.length; i++, pos++) {
            if (consValue.charAt(pos) == 'X')// 约束矛盾，不存在满足当前约束的用例值，则返回-1
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

        // 将二进制字符串转化为整数
        int max = numberArr.length;
        int result = 0;
        for (i = max - 1; i >= 0; i--)
            result += Math.pow(2, i) * numberArr[i];
        return result;
    }

    /*
     * 位运算测试用例生成的主流程 add by Yaoweichang
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
    // //生成测试用例
    // TestCaseNew tcase = new TestCaseNew();
    //
    // if (vd.getVariableSource().isInput()) {
    // boolean[] hasRing = { false };
    // //为参与位运算的参数生成用例值
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
     * 测试用例生成的主流程框架
     */
    public boolean generate() {
        this.backtrackCount = 0;
        this.MAX_ROLLBACKCOUNT = 20;// 最大回退次数
        MAX_BACKTRACKCOUNT = 500;// 最大回溯次数
        /**
         * 获取符号表达式正负性
         */
        SymbolTendency st = new SymbolTendency(constrains, this.getSymbolFactorSet());
        SymExprTendencyMap = st.getSymExpressionTendency();
        SymbolTendencyMap = st.getSymbolTendencyMap();
        logger.info("符号表达式正负性为：" + SymExprTendencyMap);
        /**
         * 初始化符号的取值域 [-inf,+inf]
         */
        initDomain(factorAndDomain);
        /**
         * 构建初始状态
         */
        FactorAndDomain currentFAndD = null;
        RollBackState initState = new RollBackState(constrains, factorAndDomain, concreteFactors, varAndVD, null);
        /**
         * 根据约束进一步优化区间
         */
        try {
            initState = calculateDomainForFactor(initState);
        } catch (Exception e) {
            logger.info(" 区间初始化分析失败");
            e.printStackTrace();
        }
        List<SymbolExpression> selist = constrains.getSymbolExpressions();
        for (SymbolExpression se : selist) {
            constrains.addConstraint(se.getRelationExpressions(), se.isTF());
        }
        optimizeDomain(initState.getFactorAndDomainSet(), constrains.getAllConstraintInPath().keySet());
        logger.info("区间初始化分析(原路径分析)结果为：" + initState.getFactorAndDomainSet());
        /**
         * 如果区间初始化分析失败重新初始化符号取值域 [-inf,+inf]在根据约束优化区间 否者直接根据约束优化区间
         */
        if (initState.getFactorAndDomainSet().isContradict()) {
            initDomain(factorAndDomain);
            initState = new RollBackState(constrains, factorAndDomain, concreteFactors, varAndVD, null);
            optimizeDomain(initState.getFactorAndDomainSet(), constrains.getAllConstraintInPath().keySet());
        }
        /**
         * 构建回退栈，保存每次回退状态
         */
        Stack<RollBackState> stateStack = new Stack<RollBackState>();
        stateStack.add(initState);
        RollBackState currentState = stateStack.peek();
        RollBackState nextState = null;
        setRollBackSuccess(true);// 回退成功标志位

        /**
         * 对符号进行排序
         */
        SymbolDomainSet FandDset = initState.getFactorAndDomainSet();
        setSortedFbyD(FandDset.sortByDomain()); // 根据符号相应区间大小进行排序
        Set<SymbolFactor> sfset = new HashSet<SymbolFactor>();
        sfset.addAll(this.getSortedFbyD());
        setSortedFactor4String(this.sortFactor4String(sfset)); // 根据符号优先级进行排序 for字符串下标及依赖符号
        /**
         * 进行回退
         */
        while (currentState != null && !currentState.isFinalState() && success) {
            RollBackState currentStatetmp = (RollBackState) currentState.clone();
            currentFAndD = rollback(currentStatetmp, this.getSortedFactor4String());
            while (currentFAndD == null) {// 回退选值失败后，进入回溯流程 add by baiyu 2015.6.16
                RollBackState tempState = null;
                logger.info("回退选值失败");
                try {
                    // nextState = backTrack(stateStack);// 回溯到初始状态
                    tempState = newBackTrack(stateStack, currentStatetmp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (tempState == null) {
                    logger.info("回溯失败，分支限界求解失败！");
                    this.success = false;
                    return false;// add by baiyu 2015.6.16
                }
                currentFAndD = tempState.getCurrentFactorAndDomain();// ?
            }
            logger.info("当前回退符号为：" + currentFAndD.getF() + "  当前回退值为：" + currentFAndD.getD());
            logger.info("上一次状态为：" + currentState.getFactorAndDomainSet());
            currentStatetmp.setCurrentFactorAndDomain(currentFAndD);
            // 判定选的值是否满足约束
            nextState = isConstraintValid(currentStatetmp, currentFAndD);
            // if(nextState != null && this.isUpdateFandD4members()){
            // for(SymbolFactor sf : this.getSortedFactor4String()){
            // if(this.isUpdateFandD4members() && nextState.getConcreteFactors().get(sf) &&
            // sf.getIndexType() == IndexType.uniSF){
            // //@唐玉宾更新
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
             * 如果不满足约束（约束更新失败，区间运算矛盾，VD更新失败） 需要进行回溯 还可以增加回溯的次数限制
             */
            if (nextState == null || !nextState.isCurStateisValid()) {
                try {
                    // nextState = backTrack(stateStack);// 回溯到初始状态
                    nextState = newBackTrack(stateStack, (RollBackState) currentState.clone());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (nextState == null) {
                    logger.info("回溯失败，分支限界求解失败！");
                    this.success = false;
                    break;// add by baiyu 2015.6.16
                }
            }
            if (nextState.isFinalState()) {
                this.success = true;
                break;
            } else {
                stateStack.add(nextState);
                currentState = (RollBackState) nextState.clone();// 回退成功保存回退状态并将本次回退状态作为下次回退状态
            }
        }

        /**
         * 如果成功，就组成测试用例，否则返回null
         */
        if (success == false)
            return false;
        else {
            // 还原经strcat拼接后的字符串——add by tangyubin
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
     * 组成测试用例
     * 
     * @param
     * @return
     */
    public abstract TestCaseNew generateTestcaseFromFactorDomain(RollBackState nextState);

    /**
     * add by baiyu 选取第一个矛盾变量并对其回退
     */
    private SymbolFactor findFirstContradictSymbol(RollBackState currentState, ArrayList<SymbolFactor> list) {
        if (currentState == null)
            return null;
        SymbolFactor current = null;
        for (SymbolFactor sf : list) {
            if (!currentState.getConcreteFactors().get(sf)) {
                if (!sf.getIndexType().equals(IndexType.uniSF)) {
                    current = sf;// 求出多次回退失败的符号变量，在其相关变量列表中
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
        // 已经赋值的变量集合
        for (Map.Entry<Factor, Boolean> entry : concreteFactorsAndValue.entrySet()) {
            if (entry.getValue()) {
                concreteFactors.add(entry.getKey());
            }
        }
        // 如果没有已经赋值的变量，返回null
        if (concreteFactors.isEmpty())
            return null;
        // 遍历当前变量的相关变量，如果存在已经赋值且值域为空的，说明此符号变量为矛盾变量
        for (SymbolFactor sf : current.getRelatedVarlist()) {
            if (concreteFactors.contains(sf) && currentState.getFactorAndDomainSet().getTable().get(sf).toString().equals("emptydomain")) {
                System.out.println("最早在" + sf + "处发生矛盾");
                returnSymbol = sf;
                return returnSymbol;
            }
        }
        return returnSymbol;
    }


    public RollBackState newBackTrack(Stack<RollBackState> stateStack, RollBackState state) {
        setRollBackSuccess(false); // 至此次回退标志为失败
        // this.backtrackCount++;
        RollBackState RollBackAgainNextState = null;
        RollBackState nextState = null;
        // selectAll穷举标志，如果当前变取值被穷举，标志置为true,用来为跳跃式回溯区分回溯和回退过程 add by baiyu 2015.6.11
        boolean selectAll = false;
        RollBackState RollBackAgainState = null;
        int rollbackCount4FailureSF = 1;// 记录每个变量回退的次数
        while (nextState == null && RollBackAgainNextState == null && rollbackCount4FailureSF < MAX_ROLLBACKCOUNT) {
            rollbackCount4FailureSF++;
            RollBackAgainState = (RollBackState) state.clone();
            try {
                FactorAndDomain RollBackAgaincurrentFAndD = rollback(RollBackAgainState, this.getSortedFactor4String());
                /**
                 * 如果穷举了该符号区间内所有值此次回溯失败
                 */
                if (RollBackAgaincurrentFAndD == null) {
                    RollBackAgainNextState = null;
                    selectAll = true;
                    break;
                }
                logger.info("当前回溯的回退符号为：" + RollBackAgaincurrentFAndD.getF() + "  当前回退值为：" + RollBackAgaincurrentFAndD.getD());
                logger.info("上一次状态为：" + state.getFactorAndDomainSet());
                RollBackAgainState.setCurrentFactorAndDomain(RollBackAgaincurrentFAndD);
                // 判定选的值是否满足约束
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
        // 上面这个while退出之后可能有两种情况①对之前选值不正确的符号已经回退20次 nextState ==null
        // 这时要回溯，从状态栈里弹出新的状态，对上一回退变量进行重新选值②nextState！=null返回到主流程继续选值
        // bug?状态栈里符号数量不变，外部回退控制list可能由于约束更新增加 新的符号两边不一致
        if (this.backtrackCount < MAX_BACKTRACKCOUNT) {
            if (nextState != null) {
                setRollBackSuccess(true);
                return nextState;
            } else {
                RollBackState statetmp1 = null;
                if (selectAll || rollbackCount4FailureSF >= 20) {// 当满足这两种情况时，是回溯的状态 add by baiyu
                    backtrackCount++;// 记录回溯次数
                    SymbolFactor backTrackSymbol = findFirstContradictSymbol(RollBackAgainState, this.getSortedFactor4String());// 跳跃式回溯，求出回溯变量
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
                    // 此时回退不成功，返回的应是选出新的回退变量后的状态，加入栈里 add by baiyu
                    if (stateStack.peek().getCurrentFactorAndDomain() == null) {
                        // 当前栈里只有初始状态，取出。感觉这样处理会有死循环，不停peek初始状态，如何确定回溯失败次数控制？。
                        statetmp1 = newBackTrack(stateStack, stateStack.peek());
                        return statetmp1;
                    } else {
                        stateStack.pop(); // 栈顶的是失败符号前一个符号选值成功的状态，弹出 ，返回栈顶，前一符号的初始状态
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
        setRollBackSuccess(false); // 至此次回退标志为失败
        RollBackState currentStatetmp = (RollBackState) previousState.clone();
        FactorAndDomain currentFAndD = rollback(currentStatetmp, this.getSortedFactor4String());
        RollBackState nextState = null;
        if (currentFAndD == null) {
            logger.info("回溯选值失败");
        } else {
            logger.info("当前回溯的回退符号为：" + currentFAndD.getF() + "  当前回退值为：" + currentFAndD.getD());
            logger.info("上一次状态为：" + previousState.getFactorAndDomainSet());
            currentStatetmp.setCurrentFactorAndDomain(currentFAndD);
            // 判定选的值是否满足约束
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
                 * 如果穷举了该符号区间内所有值此次回溯失败
                 */
                if (RollBackAgaincurrentFAndD == null) {
                    RollBackAgainNextState = null;
                    break;
                }
                logger.info("当前回溯的回退符号为：" + RollBackAgaincurrentFAndD.getF() + "  当前回退值为：" + RollBackAgaincurrentFAndD.getD());
                logger.info("上一次状态为：" + previousState.getFactorAndDomainSet());
                RollBackAgainState.setCurrentFactorAndDomain(RollBackAgaincurrentFAndD);
                // 判定选的值是否满足约束
                RollBackAgainNextState = isConstraintValid(RollBackAgainState, RollBackAgaincurrentFAndD);
            } catch (RuntimeException e) {
                RollBackAgainNextState = null;
                e.printStackTrace();
            }
        }

        if (nextState != null) {
            stateStack.add(nextState);
            setRollBackSuccess(true); // 至此次回退标志为成功
            return nextState;
        }

        if (RollBackAgainNextState == null) {
            // return null;
            return backTrack(stateStack); // 本次回退失败 在回溯到上一状态？
        } else {
            /**
             * 如果本次回溯成功，将成功的状态保存起来并返回
             */
            stateStack.add(RollBackAgainNextState);
            setRollBackSuccess(true); // 至此次回退标志为成功
            return RollBackAgainNextState;
        }
    }

    /**
     * 判定当前的回退值在当前的约束状态是否能够满足 判定过程中需要构建新的状态，状态中的每一个参数都需要clone出一个副本，然后构造新的状态
     * 保留判定的框架，具体实现在继承类 唐玉宾、熊威<br>
     * 复合类型：<br>
     * 1.1先判定结构，生成符号（字符串成员） 或者 1.2根据基本类型的取值化简约束 2区间运算
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
             * 当前回退符号是下标包括str_i、unsure、unin
             * 通过need2UpdateVD是T还是F判断unsure或unin所依赖的符号是否求解完成
             * 求解完成后需要更新抽象内存VD
             */
            if (this.need2UpdateVD) {
                try {
                    IndexType sfType = curSymbol.getIndexType();
                    switch (sfType) {
                        case unsureSF:// 是第三类函数产生的符号
                        {
                            SymbolFactor relySF = curSymbol.getRelySF(); // 已经求解出来的n
                            // 根据n生成n个下标符号，他们的属性是下标normal
                            List<SymbolFactor> ConcreteIndexList = curSymbol.getIndexWithConcreteRelyValue(relySF.getDomain(retState.getFactorAndDomainSet()));
                            // 更新抽象内存，及约束。约束更新例：由str[unsure]>0变为str[index1]>0...str[indexn]>0，此处暂不将str[index]
                            // 加入needAddSFlist
                            // 待index确定后在加
                            retState = new RollBackStateUpdater(retState).updateForUnsureSF(ConcreteIndexList);
                            this.setNeed2UpdateVD(false);
                            break;
                        }
                        case uniSF: {
                            SymbolFactor LSF = curSymbol.getLSF();// 下界符号
                            SymbolFactor USF = curSymbol.getUSF();// 下界符号
                            // 生成从LSF到USF个确定数字的下标
                            List<NumberFactor> SymbolIndexList = curSymbol.getIndexWithConcreteUpDown(retState.getFactorAndDomainSet().getDomain(USF), retState.getFactorAndDomainSet().getDomain(LSF));
                            // 更新抽象内存、约束及needAddSFlist，约束更新例：up=1，down=0，由str[unin] != c 变为str[0] !=
                            // c str[1] != c@tangyubin
                            RollBackState tmpState = new RollBackStateUpdater(retState).updateForUniSF(SymbolIndexList);
                            retState = addNewSF4Rollback(tmpState, tmpState.getNeedAddSFlist());
                            this.setNeed2UpdateVD(false);
                            // 对新加入符号进行排序
                            ArrayList<SymbolFactor> list1 = new ArrayList<SymbolFactor>();
                            list1 = this.updateSortList(retState.getNeedAddSFlist(), this.getSortedFactor4String());
                            setSortedFactor4String(list1);
                            break;
                        }
                        case Normal: {
                            // 根据求解出来的str_i，indexn更新抽象内存、约束及needAddSFlist
                            RollBackState tmpState = new RollBackStateUpdater(retState).updateForNomal();
                            // RollBackState tmpState = new
                            // RollBackStateUpdater(retState).updateForComplicatedFactor();
                            retState = addNewSF4Rollback(tmpState, tmpState.getNeedAddSFlist());
                            this.setNeed2UpdateVD(false);
                            // 对新加入符号进行排序
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
                // 只进行区间运算
                retState = calculateDomainForFactor(retState);
                logger.info("区间运算的结果是：" + retState.getFactorAndDomainSet());
                if (retState.getFactorAndDomainSet().isContradict()) {
                    return null;
                } else {
                    return retState;
                }
            }
        } else {
            /**
             * 当前回退符号是非下标符号包括普通符号和字符串长度符号，普通符号包括普通SF、up、down、unsure依赖的n，strlen
             * 普通符号更新约束，strlen更新VD
             */
            if (curSymbol.getStrlentype()) {
                // 根据strlen更新抽象内存，更新约束，,不用更新needAddSFlist@tangyubin
                try {
                    retState = new RollBackStateUpdater(retState).updateForStrlenType();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                // 更新约束@tangyubin
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
        logger.info("区间运算的结果是：" + retState.getFactorAndDomainSet());
        if (retState.getFactorAndDomainSet().isContradict()) {
            return null;
        } else {
            // 回退成功 至回退符号标志为true
            retState.getConcreteFactors().remove(curSymbol);
            retState.getConcreteFactors().put(curSymbol, new Boolean("true"));
            return retState;
        }
    }


    /**
     * 更新数组的内存模型和约束 根据复合类型的符号取值，解释取成员运算符（*，.，[]），为复合类型的成员生成符号，更新VD，更新约束
     * 解释成员运算符的过程中，如果生成了新的符号，需要将符号加入到factorAndDomain集合中 更新 2014-11-13 解析
     * arr[$i]这样的表达式，需要改变数组的VD 其中$i是全称符号
     * 有可能在合并符号时发生错误，此时返回retstate中的valueset是null
     * 
     * @param state
     * @return
     * @throws Exception
     */
    public abstract RollBackState updateConstrainsForComplicateFactor(RollBackState state) throws Exception;

    /**
     * 区间运算<br>
     * 如果发生矛盾，就将retState 中的factorAndDomain 设置为null
     * 
     * @param state
     * @return
     */
    public abstract RollBackState calculateDomainForFactor(RollBackState state);

    /**
     * 根据数值类型的取值生成符号，对约束化简 -唐玉宾
     * 
     * @param stateToBeSimplyfied
     * @throws Exception
     */
    public abstract void updateConstrainsForPrimitiveFactor(RollBackState stateToBeSimplyfied) throws Exception;

    /**
     * 回退 选符号和选值<br>
     * 1选符号<br>
     * 2选值<br>
     * 2.1基本类型：张明哲<br>
     * 
     * @return
     */
    public abstract FactorAndDomain rollback(RollBackState state, ArrayList<SymbolFactor> list);

    /**
     * 根据区间大小排序 顺序选值
     * 回退 选符号和选值<br>
     * 1选符号<br>
     * 2选值<br>
     * 2.1基本类型：张明哲<br>
     * 
     * @return
     */

    /**
     * 初始化factorAndDomain<br>
     * 1设置结构<br>
     * 2设置结构中成员的初始域<br>
     * 2.1数值类型：使用区间表示为[-inf,+inf]<br>
     * 2.2指针类型：基于结构的值域<br>
     * 3优化值域<br>
     */
    public abstract void initDomain(SymbolDomainSet factorAndDomain);

    /**
     * 根据约束进一步优化区间
     * 
     * @param factorAndDomain
     * @param set
     */
    public abstract void optimizeDomain(SymbolDomainSet factorAndDomain, Set<List<RelationExpression>> set);

    /**
     * 对字符串相关符号按照回退优先级进行排序
     */
    public abstract ArrayList<SymbolFactor> sortFactor4String(Set<SymbolFactor> sfset);

    /**
     * 加入新的符号到待回退序列，下标的初始区间初始化为[0,50]，数组成员初始区间初始化为[-128, 127]
     */
    public abstract RollBackState addNewSF4Rollback(RollBackState state, ArrayList<SymbolFactor> list);

    /**
     * 加入新的符号到已根据下标优先级排序的序列 list1为新增符号列表list2为有序的回退符号列表
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
