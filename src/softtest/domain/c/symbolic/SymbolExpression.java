package softtest.domain.c.symbolic;

import java.util.List;

/**
 * Լ���Ķ����װ�������޼����ʽ������Ե�
 * @author wangyi
 *
 */
public class SymbolExpression implements Cloneable{
	
	private LogicalExpression logicalExpression;
	/**
	 * ��ע���ʽȡ�滹��ȡ��
	 */
	private boolean TF = true;
	
	/**
	 * ��ע�ַ���Լ���Ƿ�ȡ��
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
	 * @return ����һ��Expression��List����List����SymbolExpression�а���������ԭ��Expression
	 * @date 2015/1/4
	 */
	public List<Expression> getAllExpressions(){
		return this.logicalExpression.getAllExpressions();
	}
	
	/**
	 * ��ȡ����RelationExpresstion
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
