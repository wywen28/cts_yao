package softtest.domain.c.symbolic;
import java.util.*;

import softtest.cfg.c.VexNode;
import softtest.domain.c.analysis.SymbolDomainSet;
import softtest.domain.c.interval.Domain;
import softtest.domain.c.interval.DoubleDomain;

public class Power implements Comparable<Power> ,Cloneable{
	private boolean flattened;;
	
	private String operator;
	
	private ArrayList<Factor> factors;

	public Power(String operator,ArrayList<Factor> factorlist,boolean flattened) {
		super();
		this.operator=operator;
		this.factors = factorlist;
		this.flattened=flattened;
	}
	
	public void appendFactor(Factor factor){
		factors.add(factor);
		flattened=false;
	}
	
	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public Power(Factor f,boolean flattened){
		factors = new ArrayList<Factor>();
		factors.add(f);
		operator="*";
		this.flattened=flattened;
	}

	public ArrayList<Factor> getFactors() {
		return factors;
	}

	public void setFactors(ArrayList<Factor> factors) {
		this.factors = factors;
		flattened=false;
	}
	
    @Override
	public int hashCode() {
    	if(!flattened){
    		flatten(0);
    	}
    	
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((factors == null) ? 0 : factors.hashCode());
		result = prime * result
				+ ((operator == null) ? 0 : operator.hashCode());
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
		Power other = (Power) obj;
		
		if(!flattened){
			flatten(0);
		}
		if(!other.flattened){
			other.flatten(0);
		}
		
		if (operator == null) {
			if (other.operator != null)
				return false;
		} else if (!operator.equals(other.operator))
			return false;
		if (factors == null) {
			if (other.factors != null)
				return false;
		} else if (!factors.equals(other.factors))
			return false;
		return true;
	}

	@Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        int factorListSize = factors.size();
        for (int i = 0; i < factorListSize; i++) {
            Factor factor = factors.get(i);
            if (i != 0) {
                strBuilder.append("^");
            }
            strBuilder.append(factor.toString());
        }
        return strBuilder.toString();
    }
	
	//add by jinkaifeng 2013.5.24 获得一个power的vnd
	public String getVND() {
		StringBuilder strBuilder = new StringBuilder();
		int factorListSize = factors.size();
		for (int i = 0; i < factorListSize; i++) {
			Factor factor = factors.get(i);
			if (i != 0) {
				strBuilder.append("^");
			}
			strBuilder.append(factor.getVND());
		}
		return strBuilder.toString();
	}

	public int compareTo(Power o) {
		int ret=0;
		for(int i=0;i<factors.size()&&i<o.factors.size();i++){
			ret=factors.get(i).compareTo(o.factors.get(i));
			if(ret!=0){
				return ret;
			}
		}
		if(factors.size()<o.factors.size()){
			return -1;
		}
		if(factors.size()>o.factors.size()){
			return 1;
		}
		ret=operator.compareTo(o.operator);
		return ret;
	}
	
	public Factor getSingleFactor() {
		if (factors.size() != 1) {
			return null;
		}
		return factors.get(0);
	}
	
	public boolean isNumber(){
		if(!flattened){
			flatten(0);
		}
		return getSingleFactor() instanceof NumberFactor;
	}
	
	public Power flatten(int depth){
		if(flattened){
			return this;
		}
		depth++;
		if(depth>256){
			throw new RuntimeException("Recursion depth limit reached");
		}		

		//从最外层开始化简指数
		LinkedList<Factor> list=new LinkedList<Factor>();
		ListIterator<Factor> i=factors.listIterator(factors.size());
		Factor previousfactor=i.previous().flatten(depth);
		list.addFirst(previousfactor);
		while(i.hasPrevious()){
			Factor f=i.previous().flatten(depth);
			if(previousfactor instanceof NumberFactor){
				//外层指数为常量
				NumberFactor nfp=(NumberFactor)previousfactor;
				if(f instanceof NumberFactor){
					//里层指数为常量，合并
					NumberFactor nfc=(NumberFactor)f;
					list.removeFirst();
					list.addFirst(new DoubleFactor(Math.pow(nfc.getDoubleValue(), nfp.getDoubleValue())));
				}else{
					//保持不变
					list.addFirst(f);
				}
			}else{
				//保持不变
				list.addFirst(f);
			}
			previousfactor=list.getFirst();
		}
		
		ArrayList<Factor> temp=new ArrayList<Factor>();
		temp.addAll(list);
		
		factors=temp;
		flattened=true;
        return this;
	}

	public boolean isFlattened() {
		return flattened;
	}

	public void setFlattened(boolean flattened) {
		this.flattened = flattened;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		Power power=(Power)super.clone();
		power.factors=new ArrayList<Factor>();
		for(Factor f:factors){
			power.factors.add((Factor)f.clone());
		}
		return power;
	}
	
	public Domain getDomain(SymbolDomainSet ds){
		flatten(0);
		Domain ret=null;
		if(factors.size()==0){
			return ret;
		}
		if(this.toString( ).equals("NaN"))
			return null;
		ret=factors.get(0).getDomain(ds);
		/*
		 * 2011年9月20日
		 * add by zhaoys
		 * edit by zhouao
		 * 增加对ret是否为null的判断
		 */
		if(ret == null)
			return null;
		for(int i=1;i<factors.size();i++){
			DoubleDomain d1=new DoubleDomain(Domain.castToDoubleDomain(ret));
			if(d1==null){
				return null;
			}
			Factor f=factors.get(i);
			Domain fd=f.getDomain(ds);
			DoubleDomain d2=Domain.castToDoubleDomain(fd);
			if(d2==null){
				return null;
			}
			//zys:2011.6.20	如果是平方操作，则调用更加精确的sqr，而不是power
			if(d2.isCanonical() && Double.doubleToLongBits(d2.getMax())==Double.doubleToLongBits(2.0)){
				ret=DoubleDomain.sqr(d1);
			}else{
				ret=DoubleDomain.power(d1, d2);
			}
		}
		return ret;
	}
	
	public HashSet<SymbolFactor> getAllSymbol(){
		HashSet<SymbolFactor> ret=new HashSet<SymbolFactor>();
		for(Factor f:factors){
			if(f instanceof SymbolFactor){
				ret.add((SymbolFactor)f);
			}else if(f instanceof NestedExprFactor){
				ret.addAll(((NestedExprFactor)f).getExpression().getAllSymbol());
			}
		}
		return ret;
	}
}
