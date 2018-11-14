package softtest.domain.c.symbolic;

import softtest.cfg.c.VexNode;
import softtest.domain.c.analysis.SymbolDomainSet;
import softtest.domain.c.interval.Domain;
import softtest.symboltable.c.Type.CType;

public abstract class Factor implements Comparable<Factor>,Cloneable {
	public int compareTo(Factor o) {
		return 0;
	}
	
	public Factor flatten(int depth){
		depth++;
		if(depth>256){
			throw new RuntimeException("Recursion depth limit reached");
		}
		return this;
	}
	
	//add jinkaifeng 2013.5.28
	public abstract String getVND();

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	public abstract Domain getDomain(SymbolDomainSet ds);
	
	CType type=null;
	
	public CType getType(){
		return type;
	}
	
	public void setType(CType type){
		this.type=type;
	}
}
