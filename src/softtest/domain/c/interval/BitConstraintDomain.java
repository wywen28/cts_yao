package softtest.domain.c.interval;

import java.util.HashSet;

public class BitConstraintDomain {
	private String bitConsExp;
	public BitConstraintDomain(String cons){
		bitConsExp = new String(cons);
	}
	public void addConstraintDomain(String constraint){
		bitConsExp = constraint;
	}
	public String getConstraintDomain(){
		return bitConsExp;
	}
	//合并同一符号的两个约束值
	public static BitConstraintDomain intersect(BitConstraintDomain constraint1, BitConstraintDomain constraint2){
		String cons1 = constraint1.getConstraintDomain();
		String cons2 = constraint2.getConstraintDomain();
		StringBuffer result = new StringBuffer();
		for(int i=0; i<cons1.length(); i++){
			char c1 = cons1.charAt(i);
			char c2 = cons2.charAt(i);
			if(c1 == '0' || c2 == '0'){
				result.append('0');
			}else if(c1 == 'T' || c2 == 'T'){
				if(c1 != 'T')
					result.append(c1);
				else if(c2 != 'T')
					result.append(c2);
				else
					result.append('T');
			}else{
				result.append('1');
			}
		}
		return new BitConstraintDomain(result.toString());
	}
	
	@Override
	public String toString(){
		return bitConsExp;
	}
}
