package softtest.domain.c.symbolic;

import java.util.List;

/**
 * 约束的顶层封装，包括罗辑表达式、真假性等
 * @author wangyi
 *
 */
public class SymbolExpression implements Cloneable{
	
	private LogicalExpression logicalExpression;
	/**
	 * 标注表达式取真还是取假
	 */
	private boolean TF = true;
	
	/**
	 * 标注字符串约束是否取反
	 */
	private boolean isFalseStringConstraint = false;

	public boolean isFalseStringConstraint() {
		return isFalseStringConstraint;
	}

	public void setFalseStringConstraint(boolean isFalseStringConstraint) {
		this.isFalseStringConstraint = isFalseStringConstraint;
	}

	public SymbolExpression() {
		this.logicalExpression = null;
	}
	
	public SymbolExpression(LogicalExpression expression, boolean tf) {
		this.logicalExpression = expression;
		this.TF = tf;
	}

	public LogicalExpression getLogicalExpression() {
		return logicalExpression;
	}

	public void setLogicalExpression(LogicalExpression expression) {
		this.logicalExpression = expression;
	}

	public boolean isTF() {
		return TF;
	}

	public void setTF(boolean tF) {
		TF = tF;
	}
	
	public String toString() {
		return logicalExpression.toString() + ", " + TF;
	}
	/**
	 * @author tangyubin
	 * @return 返回一个Expression的List，该List代表SymbolExpression中包含的所有原子Expression
	 * @date 2015/1/4
	 */
	public List<Expression> getAllExpressions(){
		return this.logicalExpression.getAllExpressions();
	}
	
	/**
	 * 获取所有RelationExpresstion
	 */
	public List<RelationExpression> getRelationExpressions() {
		return this.logicalExpression.getRelationExpressions();
	}
	
	/**
	 * @author tangyubin
	 * @throws CloneNotSupportedException 
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		SymbolExpression se = (SymbolExpression) super.clone();
			if(this.logicalExpression != null) {
				se.logicalExpression = (LogicalExpression) this.logicalExpression.clone();
			}
		
		return se;
	}
}
