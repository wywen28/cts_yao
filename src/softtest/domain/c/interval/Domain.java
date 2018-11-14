package softtest.domain.c.interval;

import java.util.ArrayList;
import java.util.TreeSet;

import softtest.config.c.Config;
import softtest.domain.c.symbolic.Expression;
import softtest.domain.c.symbolic.SymbolFactor;
import softtest.symboltable.c.VariableNameDeclaration;
import softtest.symboltable.c.Type.*;

public abstract class Domain implements Cloneable {
	public int compareTo(Domain o) {
		if (this instanceof IntegerDomain && o instanceof IntegerDomain) {
			IntegerDomain t = (IntegerDomain) this;
			IntegerDomain other = (IntegerDomain) o;
			return t.compareTo(other);
		} else if (this instanceof DoubleDomain && o instanceof DoubleDomain) {
			DoubleDomain t = (DoubleDomain) this;
			DoubleDomain other = (DoubleDomain) o;
			return t.compareTo(other);
		} else if (this instanceof IntegerDomain && o instanceof DoubleDomain) {
			IntegerDomain t = (IntegerDomain) this;
			DoubleDomain other = (DoubleDomain) o;
			if (t.getSize() > other.getSize())
				return 1;
			else if (t.getSize() < other.getSize())
				return -1;
		} else if (this instanceof DoubleDomain && o instanceof IntegerDomain) {
			DoubleDomain t = (DoubleDomain) this;
			IntegerDomain other = (IntegerDomain) o;
			if (t.getSize() > other.getSize())
				return 1;
			else if (t.getSize() < other.getSize())
				return -1;
		} else if ((this instanceof PointerDomain || this instanceof UnknownDomain)
				&& (o instanceof IntegerDomain || o instanceof DoubleDomain)) {
			return 1;
		} else if ((this instanceof IntegerDomain || this instanceof DoubleDomain)
				&& (o instanceof PointerDomain || this instanceof UnknownDomain)) {
			return -1;
		}
		return 0;
	}

	public DomainType getDomaintype() {
		return domaintype;
	}

	public abstract String toString();

	public DomainType domaintype;

	/**
	 * unknown��ǣ�����ǰ�����Ƿ�δ֪
	 */
	protected boolean unknown = false;

	public Domain() {
		domaintype = DomainType.UNKNOWN;
	}

	@Override
	public Domain clone() throws CloneNotSupportedException {
		Domain domain = (Domain) super.clone();
		domain.unknown = this.unknown;
		domain.domaintype = domaintype;
		return domain;
	}

	public boolean isNumberDomain() {
		return (domaintype == DomainType.DOUBLE || domaintype == DomainType.INTEGER);
	}

	public static DoubleDomain castToDoubleDomain(Domain domain) {
		return (DoubleDomain) castToType(domain,
				CType_BaseType.getBaseType("double"));
	}

	public static IntegerDomain castToIntegerDomain(Domain domain) {
		return (IntegerDomain) castToType(domain,
				CType_BaseType.getBaseType("int"));
	}

	/**
	 * �жϵ�ǰ��������Ƿ�Ϊδ֪����
	 * 
	 * @return ����unknown���
	 */
	public boolean isUnknown() {
		return unknown;
	}

	/**
	 * ����unknown���
	 * 
	 * @param unknown
	 */
	public void setUnknown(boolean unknown) {
		this.unknown = unknown;
	}

	/**
	 * ���Ƶ�ǰ�������
	 * 
	 * @return ��ǰ��������һ������
	 */
	public Domain copy() {
		Domain ret = null;
		try {
			ret = (Domain) clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		return ret;
	}

	public static Domain castToType(Domain domain, CType type) {
		Domain ret = null;
		if (domain == null) {
			return null;
		}
		switch (Domain.getDomainTypeFromType(type)) {
		case POINTER: {
			CType_AbstPointer pointertype = null;
			// liuli:�Բ�ͬ���͵�type�ֿ�����
			if (type instanceof CType_Function) {
				CType_Function ftype = (CType_Function) type.getSimpleType();
				CType temp = ftype.getReturntype();
				if (temp instanceof CType_Typedef)
					pointertype = (CType_AbstPointer) temp.getNormalType();
				else
					pointertype = (CType_AbstPointer) ftype.getReturntype();
			} else if (type instanceof CType_AbstPointer) {
				pointertype = (CType_AbstPointer) type.getSimpleType();
			} else if (type instanceof CType_Qualified) {
				pointertype = (CType_AbstPointer) type.getSimpleType();
			} else if (type instanceof CType_Typedef) {
				pointertype = (CType_AbstPointer) type.getSimpleType();
			}

			CType eletype = pointertype.getOriginaltype();
			if (eletype == null) {
				eletype = CType_BaseType.getBaseType("int");
			}
			PointerDomain todomain = new PointerDomain();
			switch (domain.domaintype) {
			case POINTER: {
				PointerDomain fromdomain = (PointerDomain) domain;
				todomain.setElementtype(eletype);
				if (fromdomain.getLength() != null) {
					todomain.setLength(fromdomain.getLength().mul(
							new Expression((double) fromdomain.getElementtype()
									.getSize() / eletype.getSize())));
				}
				break;
			}
			case DOUBLE: {
				DoubleDomain fromdomain = (DoubleDomain) domain;
				todomain.setElementtype(eletype);
				DoubleInterval interval = fromdomain.jointoOneInterval();
				if (interval.contains(0)) {
					if (interval.isCanonical()) {
						todomain.value = PointerValue.NULL;
					} else {
						todomain.value = PointerValue.NULL_OR_NOTNULL;
					}
				}
				break;
			}
			case INTEGER: {
				IntegerDomain fromdomain = (IntegerDomain) domain;
				todomain.setElementtype(eletype);
				IntegerInterval interval = fromdomain.jointoOneInterval();
				if (interval.contains(0)) {
					if (interval.isCanonical()) {
						todomain.value = PointerValue.NULL;
						// �޸��˿�ָ�������жϵ�����ת��
						// modified by zhouhb 2010/8/17
						todomain.offsetRange.intervals.clear();
						// todomain.AllocType=CType_AllocType.Null;
						todomain.Type.add(CType_AllocType.Null);
					} else {
						todomain.value = PointerValue.NULL_OR_NOTNULL;
					}
				}
				break;
			}
			}
			if (domain instanceof PointerDomain)
				ret = domain;
			else
				ret = todomain;
			break;
		}
		case INTEGER: {
			IntegerDomain todomain = null;
			switch (domain.domaintype) {
			case POINTER: {
				PointerDomain fromdomain = (PointerDomain) domain;
				switch (fromdomain.getValue()) {
				case NULL:
					todomain = new IntegerDomain(0, 0);
					break;
				case NOTNULL:
					todomain = new IntegerDomain(1, Long.MAX_VALUE);
					break;
				default:
					todomain = new IntegerDomain(0, Long.MAX_VALUE);
					break;
				}
				break;
			}
			case DOUBLE: {
				DoubleDomain d = (DoubleDomain) domain;
				TreeSet<IntegerInterval> intervals = new TreeSet<IntegerInterval>();
				for (DoubleInterval interval : d.getIntervals()) {
					intervals.add(new IntegerInterval(Math.round(interval
							.getMin() - 0.5),
							Math.round(interval.getMax() - 0.5)));
				}// ��������
				todomain = new IntegerDomain();
				todomain.setIntervals(intervals);
				todomain.setUnknown(d.isUnknown());
				break;
			}
			case INTEGER: {
				todomain = (IntegerDomain) domain;
				break;
			}
			}
			ret = todomain;
			break;
		}
		case DOUBLE: {
			DoubleDomain todomain = null;
			switch (domain.domaintype) {
			case POINTER: {
				PointerDomain fromdomain = (PointerDomain) domain;
				switch (fromdomain.getValue()) {
				case NULL:
					todomain = new DoubleDomain(0, 0);
					break;
				case NOTNULL:
					todomain = new DoubleDomain(1, Double.POSITIVE_INFINITY);
					break;
				default:
					todomain = new DoubleDomain(0, Double.POSITIVE_INFINITY);
					break;
				}
				break;
			}
			case DOUBLE: {
				todomain = (DoubleDomain) domain;
				break;
			}
			case INTEGER: {
				IntegerDomain d = (IntegerDomain) domain;
				TreeSet<DoubleInterval> intervals = new TreeSet<DoubleInterval>();
				for (IntegerInterval interval : d.getIntervals()) {
					double min = (interval.getMin() == Long.MIN_VALUE) ? Double.NEGATIVE_INFINITY
							: interval.getMin();
					double max = (interval.getMax() == Long.MAX_VALUE) ? Double.POSITIVE_INFINITY
							: interval.getMax();
					intervals.add(new DoubleInterval(min, max));
				}
				todomain = new DoubleDomain();
				todomain.setIntervals(intervals);
				todomain.setUnknown(d.isUnknown());
				break;
			}
			}
			ret = todomain;
			break;
		}
		}
		return ret;
	}

	public static Domain inverse(Domain d) {
		Domain ret = null;
		if (d == null) {
			return null;
		}
		try {
			switch (d.getDomaintype()) {
			case POINTER: {
				PointerDomain p1 = null;
				p1 = (PointerDomain) d.clone();
				ret = PointerDomain.inverse(p1);
				break;
			}
			case INTEGER: {
				IntegerDomain p1 = null;
				p1 = (IntegerDomain) d.clone();
				ret = IntegerDomain.inverse(p1);
				break;
			}
			case DOUBLE: {
				DoubleDomain p1 = null;
				p1 = (DoubleDomain) d.clone();
				ret = DoubleDomain.inverse(p1);
				break;
			}
			}
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
		return ret;
	}

	public static Domain intersect(Domain d1, Domain d2, CType type) {
		Domain ret = null;
		try {
			if (d1 == null) {
				ret = (d2 == null) ? null : (Domain) d2.clone();
				return ret;
			}
			if (d2 == null) {
				ret = (d1 == null) ? null : (Domain) d1.clone();
				return ret;
			}
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}

		switch (Domain.getDomainTypeFromType(type)) {
		case POINTER: {
			PointerDomain p1 = null, p2 = null;
			p1 = (PointerDomain) castToType(d1, type);
			p2 = (PointerDomain) castToType(d2, type);
			ret = PointerDomain.intersect(p1, p2);
			break;
		}
		case INTEGER: {
			IntegerDomain i1 = null, i2 = null;
			i1 = (IntegerDomain) castToType(d1, type);
			i2 = (IntegerDomain) castToType(d2, type);
			ret = IntegerDomain.intersect(i1, i2);
			break;
		}
		case DOUBLE: {
			DoubleDomain i1 = null, i2 = null;
			i1 = (DoubleDomain) castToType(d1, type);
			i2 = (DoubleDomain) castToType(d2, type);
			ret = DoubleDomain.intersect(i1, i2);
			break;
		}
		}
		return ret;
	}

	public static Domain union(Domain d1, Domain d2, CType type) {
		Domain ret = null;
		try {
			if (d1 == null) {
				ret = (d2 == null) ? null : (Domain) d2.clone();
				return ret;
			}
			if (d2 == null) {
				ret = (d1 == null) ? null : (Domain) d1.clone();
				return ret;
			}
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}

		switch (Domain.getDomainTypeFromType(type)) {
		case POINTER: {
			PointerDomain p1 = null, p2 = null;
			p1 = (PointerDomain) castToType(d1, type);
			p2 = (PointerDomain) castToType(d2, type);
			ret = PointerDomain.union(p1, p2);
			break;
		}
		case INTEGER: {
			IntegerDomain i1 = null, i2 = null;
			i1 = (IntegerDomain) castToType(d1, type);
			i2 = (IntegerDomain) castToType(d2, type);
			ret = IntegerDomain.union(i1, i2);
			break;
		}
		case DOUBLE: {
			DoubleDomain i1 = null, i2 = null;
			i1 = (DoubleDomain) castToType(d1, type);
			i2 = (DoubleDomain) castToType(d2, type);
			ret = DoubleDomain.union(i1, i2);
			break;
		}
		}
		return ret;
	}

	public static Domain getEmptyDomainFromType(CType type) {
		Domain ret = null;
		switch (Domain.getDomainTypeFromType(type)) {
		case POINTER: {
			PointerDomain p = new PointerDomain();
			CType eletype = null;
			// liuli:����type�����ͷֿ�����
			if (type instanceof CType_AbstPointer) {
				CType_AbstPointer ptype = (CType_AbstPointer) type;
				eletype = ptype.getOriginaltype();
			} else if (type instanceof CType_Typedef) {
				CType_Typedef ttype = (CType_Typedef) type;
				eletype = ttype.getOriginaltype();
			}

			if (eletype == null) {
				eletype = CType_BaseType.getBaseType("int");
			}
			p.setElementtype(eletype);
			p.value = PointerValue.EMPTY;
			ret = p;
			break;
		}
		case INTEGER: {
			ret = IntegerDomain.getEmptyDomain();
			break;
		}
		case DOUBLE: {
			ret = DoubleDomain.getEmptyDomain();
			break;
		}
		}
		return ret;
	}

	public static Domain substract(Domain d1, Domain d2, CType type) {
		Domain ret = null;
		switch (Domain.getDomainTypeFromType(type)) {
		case POINTER: {
			PointerDomain p1 = null, p2 = null;
			p1 = (PointerDomain) castToType(d1, type);
			p2 = (PointerDomain) castToType(d2, type);
			ret = PointerDomain.intersect(p1, PointerDomain.inverse(p2));
			break;
		}
		case INTEGER: {
			IntegerDomain i1 = null, i2 = null;
			i1 = (IntegerDomain) castToType(d1, type);
			i2 = (IntegerDomain) castToType(d2, type);
			ret = IntegerDomain.intersect(i1, IntegerDomain.inverse(i2));
			break;
		}
		case DOUBLE: {
			DoubleDomain i1 = null, i2 = null;
			i1 = (DoubleDomain) castToType(d1, type);
			i2 = (DoubleDomain) castToType(d2, type);
			ret = DoubleDomain.intersect(i1, DoubleDomain.inverse(i2));
			break;
		}
		}
		return ret;
	}

	public static boolean isEmpty(Domain d) {
		if (d == null) {
			return false;
		}
		switch (d.getDomaintype()) {
		case POINTER: {
			PointerDomain p1 = null;
			p1 = (PointerDomain) d;
			if (p1.value == PointerValue.EMPTY) {
				return true;
			} else {
				return false;
			}
		}
		case INTEGER: {
			IntegerDomain p1 = null;
			p1 = (IntegerDomain) d;
			return p1.isEmpty();
		}
		case DOUBLE: {
			DoubleDomain p1 = null;
			p1 = (DoubleDomain) d;
			return p1.isEmpty();
		}
		}
		return false;
	}

	public static Domain getFullDomainFromType(CType type) {
		Domain ret = null;
		switch (Domain.getDomainTypeFromType(type)) {
		case POINTER: {
			PointerDomain p = new PointerDomain();
			CType_AbstPointer ptype = null;
			// liuli:type�Ƿ���ֵΪָ��ĺ����Ĵ���
			if (type instanceof CType_Function) {
				CType_Function ftype = (CType_Function) type.getSimpleType();
				CType temp = ftype.getReturntype();
				if (temp instanceof CType_Typedef)
					ptype = (CType_AbstPointer) temp.getNormalType();
				else
					ptype = (CType_AbstPointer) ftype.getReturntype();
			} else if (type instanceof CType_AbstPointer) {
				ptype = (CType_AbstPointer) type.getSimpleType();
			}

			CType eletype = null;
			if (ptype != null) {
				eletype = ptype.getOriginaltype();
			}
			if (eletype == null) {
				eletype = CType_BaseType.getBaseType("int");
			}
			p.setElementtype(eletype);
			// ȫ���޸�
			// modified by zhouhb 2010/7/19
			p.value = PointerValue.NULL_OR_NOTNULL;
			ret = p;
			break;
		}
		case INTEGER: {
			// add by jinkaifeng 2012.12.13 enum��CType_BitField�����ʼ��ʱ����Ӧ����ȫ��
			if (type instanceof CType_Enum || CType.getOrignType(type) instanceof CType_Enum) {
				if(!(type instanceof CType_Enum) ){
					type = CType.getOrignType(type);
				}
				IntegerDomain enumDomain = new IntegerDomain();
				ArrayList<Long> value = ((CType_Enum) type).getEnumValue();
				for (Long i : value) {
					enumDomain.mergeWith(new IntegerInterval(i));
				}
				ret = enumDomain;
			} else if (type instanceof CType_BitField) {
				int bitField = ((CType_BitField) type).getBitfield();
				long min = 0 - (long) Math.pow(2, bitField - 1);
				long max = (long) Math.pow(2, bitField - 1) - 1;
				ret = new IntegerDomain(min, max);

			}
			// add end
			else {
				ret = IntegerDomain.getFullDomain();
			}
			break;
		}
		case DOUBLE: {
			ret = DoubleDomain.getFullDomain();
			break;
		}
		}
		return ret;
	}

	/**
	 * ��һ����Χ�������һ��ֵ �������ɲ�������ʱ����������·��Լ���ı���������û��ʹ�� ���������Ա�Լ�������Ա ��һ������Ĳ���������ֵ int
	 * �͵� [-50,50] double [-10,10] zhangxuzhou
	 * 
	 * @param type
	 * @return
	 */
	public static Domain getOneRandomDomainFromType(CType type) {
		Domain ret = null;
		switch (Domain.getDomainTypeFromType(type)) {
		case POINTER: {
			PointerDomain p = new PointerDomain();
			CType_AbstPointer ptype = null;
			// liuli:type�Ƿ���ֵΪָ��ĺ����Ĵ���
			if (type instanceof CType_Function) {
				CType_Function ftype = (CType_Function) type.getSimpleType();
				CType temp = ftype.getReturntype();
				if (temp instanceof CType_Typedef)
					ptype = (CType_AbstPointer) temp.getNormalType();
				else
					ptype = (CType_AbstPointer) ftype.getReturntype();
			} else if (type instanceof CType_AbstPointer) {
				ptype = (CType_AbstPointer) type.getSimpleType();
			}

			CType eletype = null;
			if (ptype != null) {
				eletype = ptype.getOriginaltype();
			}
			if (eletype == null) {
				eletype = CType_BaseType.getBaseType("int");
			}
			p.setElementtype(eletype);
			// ȫ���޸�
			// modified by zhouhb 2010/7/19
			p.value = PointerValue.NULL_OR_NOTNULL;
			ret = p;
			break;
		}
		case INTEGER: {
			// add by jinkaifeng 2012.12.13 enum��CType_BitField�����ʼ��ʱ����Ӧ����ȫ��
			if (type instanceof CType_Enum) {
				IntegerDomain enumDomain = new IntegerDomain();
				ArrayList<Long> value = ((CType_Enum) type).getEnumValue();
				for (Long i : value) {
					enumDomain.mergeWith(new IntegerInterval(i));
				}
				ret = enumDomain;
			} else if (type instanceof CType_BitField) {
				int bitField = ((CType_BitField) type).getBitfield();
				long min = 0 - (long) Math.pow(2, bitField - 1);
				long max = (long) Math.pow(2, bitField - 1) - 1;
				ret = new IntegerDomain(min, max);

			}
			// add end
			else {
				int min = -50;
				int max = 50;
				ret = IntegerDomain.getOneRandomDomain(min, max);
			}
			break;
		}
		case DOUBLE: {
			float min = -10;
			float max = 10;
			ret = DoubleDomain.getOneRandomDomain(min, max);
			break;
		}
		}
		return ret;
	}

	public static DomainType getDomainTypeFromType(CType type) {
		if (type.isPointType()) {
			return DomainType.POINTER;
		} else if (type.isBasicType()) {
			CType_BaseType basetype = (CType_BaseType) type.getSimpleType();
			if (basetype.isIntegerType()) {
				return DomainType.INTEGER;
			} else {
				return DomainType.DOUBLE;
			}
		} else if (type instanceof CType_Function) {
			// zys:��Ҫ���ɺ���ժҪ��ȡ�ú����ķ���ֵ��Ϣ
			CType returnType = ((CType_Function) type).getReturntype();
			return getDomainTypeFromType(returnType);
		} else if (type instanceof CType_BitField) {
			return DomainType.INTEGER;
		} else if (type instanceof CType_Struct) {
			// ��ʱŪ������
			return DomainType.INTEGER;
		} else if (type instanceof CType_Union) {
			// ��ʱŪ������
			return DomainType.INTEGER;
			// liuli:����typedef��enum
		} else if (type instanceof CType_Typedef) {
			return DomainType.INTEGER;
		} else if (type instanceof CType_Enum) {
			// ��ʱŪ������
			return DomainType.INTEGER;
		} else if (type instanceof CType_Qualified) {
			return DomainType.INTEGER;
		}
		throw new RuntimeException("illeage type!");
	}

	public boolean isCanonical() {
		return false;
	}

	/*
	 * public Object getMin() { Object o=null; switch(domaintype) { case
	 * INTEGER: IntegerDomain inter=(IntegerDomain)this; if
	 * (inter.getIntervals().size() > 0) { return
	 * inter.getIntervals().first().getMin(); }else{ return Long.MAX_VALUE; }
	 * case DOUBLE: DoubleDomain dou=(DoubleDomain)this; if
	 * (dou.getIntervals().size() > 0) { return
	 * dou.getIntervals().first().getMin(); }else{ return
	 * Double.POSITIVE_INFINITY; } default:
	 * 
	 * } return o; }
	 * 
	 * public Object getMax() { Object o=null; switch(domaintype) { case
	 * INTEGER: IntegerDomain inter=(IntegerDomain)this; if
	 * (inter.getIntervals().size() > 0) { return
	 * inter.getIntervals().last().getMax(); }else{ return Long.MIN_VALUE; }
	 * case DOUBLE: DoubleDomain dou=(DoubleDomain)this; if
	 * (dou.getIntervals().size() > 0) { return
	 * dou.getIntervals().last().getMax(); }else{ return
	 * Double.NEGATIVE_INFINITY; } default:
	 * 
	 * } return o; }
	 */
	public static Domain wideningDomain(Domain before, Domain after) {

		if (before == null) {
			return after;
		} else if (after == null) {
			return before;
		}
		if (before.equals(after))
			return before;

		Domain retDomain = null;
		DomainType dt = before.getDomaintype();

		switch (dt) {
		case INTEGER:
			if (before.unknown || after.unknown)
				return IntegerDomain.getUnknownDomain();
			long d_min,
			d_max,
			a_min,
			a_max,
			b_min,
			b_max;
			a_min = ((IntegerDomain) before).getMin();
			a_max = ((IntegerDomain) before).getMax();
			after = Domain.castToType(after, CType_BaseType.intType);
			b_min = ((IntegerDomain) after).getMin();
			b_max = ((IntegerDomain) after).getMax();

			if (b_min < a_min) {
				d_min = Long.MIN_VALUE;
			} else {
				d_min = a_min;
			}

			if (b_max > a_max) {
				d_max = Long.MAX_VALUE;
			} else {
				d_max = a_max;
			}
			IntegerInterval t = new IntegerInterval(d_min, d_max);
			retDomain = new IntegerDomain(t);
			break;
		case DOUBLE:
			if (before.unknown || after.unknown)
				return DoubleDomain.getUnknownDomain();
			double r_min,
			r_max,
			x_min,
			x_max,
			y_min,
			y_max;
			x_min = ((DoubleDomain) before).getMin();
			x_max = ((DoubleDomain) before).getMax();
			after = Domain.castToType(after, CType_BaseType.doubleType);
			y_min = ((DoubleDomain) after).getMin();
			y_max = ((DoubleDomain) after).getMax();

			if (y_min < x_min) {
				r_min = Long.MIN_VALUE;
			} else {
				r_min = x_min;
			}

			if (y_max > x_max) {
				r_max = Long.MAX_VALUE;
			} else {
				r_max = x_max;
			}
			DoubleInterval d = new DoubleInterval(r_min, r_max);
			retDomain = new DoubleDomain(d);
			break;
		case POINTER:
			if (before.unknown || after.unknown)
				return IntegerDomain.getUnknownDomain();
			retDomain = new PointerDomain();

			// IntegerDomain allocRange=null;
			// CType_AllocType allocType=null;
			CType elementType = ((PointerDomain) before).getElementtype();
			((PointerDomain) retDomain).setElementtype(elementType);
			// Expression length=null;
			// String name=null;
			// IntegerDomain offsetRange=null;
			// Domain realDomain=null;
			// PointerValue value=null;
			break;
		case UNKNOWN:
			break;
		default:
			break;
		}

		return retDomain;
	}

	public static Domain narrowingDomain(Domain before, Domain after) {

		if (before == null) {
			return after;
		} else if (after == null) {
			return before;
		}
		if (before.equals(after))
			return before;

		Domain retDomain = null;
		DomainType dt = before.getDomaintype();

		switch (dt) {
		case INTEGER:
			if (before.unknown || after.unknown)
				return IntegerDomain.getUnknownDomain();
			long d_min,
			d_max,
			a_min,
			a_max,
			b_min,
			b_max;
			a_min = ((IntegerDomain) before).getMin();
			a_max = ((IntegerDomain) before).getMax();
			after = Domain.castToType(after, CType_BaseType.intType);
			b_min = ((IntegerDomain) after).getMin();
			b_max = ((IntegerDomain) after).getMax();

			if (a_min == Long.MIN_VALUE) {
				d_min = b_min;
			} else {
				d_min = Math.min(a_min, b_min);
			}

			if (a_max == Long.MAX_VALUE) {
				d_max = b_max;
			} else {
				d_max = Math.max(a_max, b_max);
			}
			IntegerInterval t = new IntegerInterval(d_min, d_max);
			retDomain = new IntegerDomain(t);
			break;
		case DOUBLE:
			if (before.unknown || after.unknown)
				return DoubleDomain.getUnknownDomain();
			double r_min,
			r_max,
			x_min,
			x_max,
			y_min,
			y_max;
			x_min = ((DoubleDomain) before).getMin();
			x_max = ((DoubleDomain) before).getMax();
			after = Domain.castToType(after, CType_BaseType.doubleType);
			y_min = ((DoubleDomain) after).getMin();
			y_max = ((DoubleDomain) after).getMax();

			if (x_min == Long.MIN_VALUE) {
				r_min = y_min;
			} else {
				r_min = Math.min(x_min, y_min);
			}

			if (x_max == Long.MAX_VALUE) {
				r_max = y_max;
			} else {
				r_max = Math.max(x_max, y_max);
			}
			DoubleInterval d = new DoubleInterval(r_min, r_max);
			retDomain = new DoubleDomain(d);
			break;
		case POINTER:
			if (before.unknown || after.unknown)
				return IntegerDomain.getUnknownDomain();
			retDomain = new PointerDomain();

			// IntegerDomain allocRange=null;
			// CType_AllocType allocType=null;
			CType elementType = ((PointerDomain) before).getElementtype();
			((PointerDomain) retDomain).setElementtype(elementType);
			// Expression length=null;
			// String name=null;
			// IntegerDomain offsetRange=null;
			// Domain realDomain=null;
			// PointerValue value=null;
			break;
		case UNKNOWN:
			break;
		default:
			break;
		}

		return retDomain;
	}

	/**
	 * ���ָ�����������unknown����
	 * 
	 * @param type
	 *            ָ����������
	 * @return ����ָ���������͵�unknown����
	 */

	private int domainStatus = 0;

	public void setDomainStatus(int domainstatus) {
		this.domainStatus = domainstatus;
	}

	public int getDomainStatus() {
		return domainStatus;
	}

	public static Domain getUnknownDomain(DomainType type) {
		Domain ret = null;
		switch (type) {

		case POINTER:
			ret = PointerDomain.getUnknownDomain();
			break;
		case INTEGER:
			ret = IntegerDomain.getUnknownDomain();
			break;
		case DOUBLE:
			ret = DoubleDomain.getUnknownDomain();
			break;

		}
		return ret;
	}

	/** �ӵ�ǰ������ѡȡ��������ֵ */
	/*
	 * public Domain selectConcreteDomain() { Domain ret=null;
	 * switch(domaintype){ case INTEGER: IntegerDomain id=(IntegerDomain)this;
	 * ret=id.nextConcreteDomain(); break; case DOUBLE: DoubleDomain
	 * dd=(DoubleDomain)this; ret=dd.nextConcreteDomain(); break; case POINTER:
	 * 
	 * default:
	 * 
	 * } return ret; }
	 */
	/**
	 * ��֧�޽��ѡֵ����������˼����ö��ֵķ�����������·��������ֵ ����Ĳ���Ϊ���������� ������������ڶ�Ӧ�ľ���������ʵ��
	 * 
	 * @param tendency
	 * @author jkf
	 */
	public Domain selectConcreteDomain(int tendency) {
		return null;
	}// add by jkf ֱ�Ӳ��ü̳еķ�ʽ���в�ͬ���͵�ȡֵ�������ж����ͺ��ٵ�����Ӧ�ĺ���
	public Domain selectConcreteDomainByPathTendency(int pathTendency) {
		return null;
	}
	
	/**
	 * ��֧�޽��ѡֵ�������Ա��ã������ѡֵ
	 * 
	 * @param tendency
	 */
	public Domain selectConcreteDomainByRandom(int tendency) {
		return null;
	}

	/**
	 * Ϊ��֧�޽���̺�����ѡ��ȷ�����䣬Ŀǰ���԰�������ѡȡ������У� 1����������������ֵ�޶�Ϊ-999-+999��
	 * 2��Ȼ��ѡȡ������ɢ����������Сֵ�� 3����ѡȡ����ɢ������м�ֵ�� 4����Щ��ѡ��ʧ�ܣ�����ôѡȡ�أ�ѡȡ��������������Ϊ10�Σ�
	 */
	protected int selectCount = 0;

	/** ��־�����Ƿ�����˳�ʼ��������1����INF���Ͻ���½��滻Ϊ�����Сֵ��2�������䰴�����С���м�ֵ�������� */
	protected boolean initialized = false;

	/** ��־�����Ƿ��ǵ�ֵ */
	public boolean isConcrete() {
		return false;
	}
	
	/**
	 * ��־$i�Ƿ���Ҫ����
	 * zmz
	 * @return
	 */
	public boolean DollarNeedRollback(){
		return false;
	}

	public Number getConcreteDomain() {
		return null;
	}

	/**
	 * �ڷ�֧�޽�����֮ǰ������������������ʼ��Ϊ�н�ֵ
	 * 
	 * @param d
	 * @author zys (changed by jkf 2012.4.11)
	 */
	public void initialize(double maxNominalValue, double ratio) {
		if (!initialized) {

			switch (domaintype) {
			case INTEGER:
				IntegerDomain id = (IntegerDomain) this;
				id.setDomainStatus(0);
				maxNominalValue = maxNominalValue / ratio;
				long minI = id.getMin();
				if (minI == Long.MIN_VALUE) {
					IntegerInterval firstInter = id.intervals.first();
					id.intervals.remove(firstInter);
					// minI=Config.MIN_INTEGER;
					long firstMax = firstInter.getMax();
					if (firstMax == Long.MAX_VALUE) {
						firstMax = Math.abs((long) maxNominalValue);
						minI = 0 - firstMax;
						// id.setDomainStatus(3);
					} else {
						minI = firstMax - Math.abs((long) maxNominalValue);
						// id.setDomainStatus(2);
					}
					firstInter = new IntegerInterval(minI, firstMax);
					id.intervals.add(firstInter);
				}
				long maxI = id.getMax();
				if (maxI == Long.MAX_VALUE) {
					IntegerInterval lastInter = id.intervals.last();
					id.intervals.remove(lastInter);
					// maxI=Config.MAX_INTEGER;
					long lastMin = lastInter.getMin();
					if (lastMin == Long.MIN_VALUE) {
						maxI = Math.abs((long) maxNominalValue);
						lastMin = 0 - maxI;
						// id.setDomainStatus(3);
					} else {
						maxI = lastMin + Math.abs((long) maxNominalValue);
						// id.setDomainStatus(1);
					}
					lastInter = new IntegerInterval(lastMin, maxI);
					id.intervals.add(lastInter);
				}
				break;
			case DOUBLE:
				DoubleDomain dd = (DoubleDomain) this;
				dd.setDomainStatus(0);
				double minD = dd.getMin();
				maxNominalValue = maxNominalValue / ratio;
				if (minD == Double.NEGATIVE_INFINITY) {
					DoubleInterval firstInter = dd.getIntervals().first();
					dd.getIntervals().remove(firstInter);
					// minD=Config.MIN_DOUBLE;
					double firstMax = firstInter.getMax();
					if (firstMax == Double.POSITIVE_INFINITY) {
						firstMax = Math.abs(maxNominalValue);
						minD = 0 - firstMax;
						// dd.setDomainStatus(3);
					} else {
						minD = firstMax - Math.abs(maxNominalValue);
						// dd.setDomainStatus(2);
					}
					firstInter = new DoubleInterval(minD, firstMax);
					dd.getIntervals().add(firstInter);
				}
				double maxD = dd.getMax();
				if (maxD == Double.POSITIVE_INFINITY) {
					DoubleInterval lastInter = dd.getIntervals().last();
					dd.getIntervals().remove(lastInter);
					// maxD=Config.MAX_DOUBLE;
					double lastMin = lastInter.getMin();
					if (lastMin == Double.NEGATIVE_INFINITY) {
						maxD = Math.abs(maxNominalValue);
						lastMin = 0 - maxD;
						// dd.setDomainStatus(3);
					} else {
						maxD = lastMin + Math.abs(maxNominalValue);
						// dd.setDomainStatus(1);
					}
					lastInter = new DoubleInterval(lastMin, maxD);
					dd.getIntervals().add(lastInter);
				}
				break;
			case POINTER:

			default:

			}
		}
	}

	/**
	 * �ڷ�֧�޽�����֮ǰ������������������ʼ��Ϊ�н�ֵ������ֵ�޶�Ϊ-999-+999 ����Ϊ���ݶԱ��ã�ʵ�ʲ���ʹ�����������ʼ������
	 * 
	 * @param
	 * @author zys
	 */
	public void initialize() {
		if (!initialized) {
			switch (domaintype) {
			case INTEGER:
				IntegerDomain id = (IntegerDomain) this;
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
				DoubleDomain dd = (DoubleDomain) this;
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
		}
	}
	
	// abstract Object getmax();
	// abstract Object getmin();
	/**
	 * ����maxRestrainValue�����ó�ʼֵ��maxRestrainValue��֮ǰ����ͬһ��Լ�������еĳ����Լ����������ֵ
	 * ����var���趨��ͬ�ķ�Χ<n> ����char �ķ�Χ�������ԾͲ����н�һ���ĳ�ʼ�� <li>char�������÷�Χ��<b>-128~127
	 * </b> <li>unsigned char �� <b>0~255</b> <li>unsigned
	 * int,long,short����-inf����0��ʼ��
	 * 
	 * @param
	 * @author zhangxuzhou
	 */
	public static Domain initialize(Domain domain, double maxRestrainValue,
			VariableNameDeclaration var) {
		// TODO Auto-generated method stub
		if (domain != null && !domain.initialized) {
			Domain ret = domain ; 
			CType_BaseType baseType = null;
			if(var.getType() instanceof CType_BaseType )
				baseType =   (CType_BaseType) var.getType();
			else if(var.getType() instanceof CType_Typedef && ((CType_Typedef) var.getType()).getOriginaltype() instanceof CType_BaseType)
				baseType = (CType_BaseType) ((CType_Typedef) var.getType()).getOriginaltype();
			else if(var.getType() instanceof CType_Enum || (var.getType() instanceof CType_Typedef &&  ((CType_Typedef) var.getType()).getOriginaltype() instanceof CType_Enum))
				return domain;
			else if(var.getType() instanceof CType_Qualified && ((CType_Qualified) var.getType()).getOriginaltype() instanceof CType_BaseType)
				return domain;
			else
				return null;
			
			// �����unsigned ������char���ͣ��Է�Χ���в���
			if (baseType.toString().contains("char")) { // �ַ���
				if (CType_BaseType.uCharType == baseType) {// ������(0,255)��this
					IntegerDomain CharDomain = new IntegerDomain(0, 255);
					ret = (IntegerDomain) Domain.intersect(domain, CharDomain,
							CType_BaseType.uCharType);
				} else { // signed char����char
					IntegerDomain CharDomain = new IntegerDomain(-128, 127);
					ret = (IntegerDomain) Domain.intersect(domain, CharDomain,
							CType_BaseType.uCharType);
				}

			} else if (baseType.toString().contains("unsigned")) {// ������unsigned�����
				IntegerDomain intDomain = new IntegerDomain(0, Long.MAX_VALUE);
				ret = (IntegerDomain) Domain.intersect(domain, intDomain,
						CType_BaseType.uCharType);
	
			} 
			
			
			
			switch (domain.domaintype) {
			case INTEGER:
				IntegerDomain id = (IntegerDomain) ret;
				id.setDomainStatus(0);
				long minI = id.getMin();
				if (minI == Long.MIN_VALUE) {
					IntegerInterval firstInter = id.intervals.first();
					id.intervals.remove(firstInter);
					// minI=Config.MIN_INTEGER;
					long firstMax = firstInter.getMax();
					if (firstMax == Long.MAX_VALUE) {
						firstMax = Math.abs((long) maxRestrainValue);
						minI = 0 - firstMax;

						id.setDomainStatus(3); // ���ע������������״̬����䣬���û�ж�̬�ı�����Ĳ���
					} else {
						// minI = firstMax-Math.abs((long)maxRestrainValue);
						minI = (firstMax < 0) ? firstMax
								- Math.abs((long) maxRestrainValue) : 0 - Math
								.abs((long) maxRestrainValue);

						id.setDomainStatus(2);
					}
					firstInter = new IntegerInterval(minI, firstMax);
					id.intervals.add(firstInter);
				}
				long maxI = id.getMax();
				if (maxI == Long.MAX_VALUE) {
					IntegerInterval lastInter = id.intervals.last();
					id.intervals.remove(lastInter);
					// maxI=Config.MAX_INTEGER;
					long lastMin = lastInter.getMin();
					if (lastMin == Long.MIN_VALUE) {
						maxI = Math.abs((long) maxRestrainValue);
						lastMin = 0 - maxI;
						id.setDomainStatus(3);
					} else {
						// maxI = lastMin+Math.abs((long)maxRestrainValue);
						maxI = (lastMin > 0) ? lastMin
								+ Math.abs((long) maxRestrainValue) : 0 + Math
								.abs((long) maxRestrainValue);

						id.setDomainStatus(1);
					}
					lastInter = new IntegerInterval(lastMin, maxI);
					id.intervals.add(lastInter);
				}
				break;
			case DOUBLE:
				DoubleDomain dd = (DoubleDomain) ret;
				dd.setDomainStatus(0);
				double minD = dd.getMin();
				if (minD == Double.NEGATIVE_INFINITY) {
					DoubleInterval firstInter = dd.getIntervals().first();
					dd.getIntervals().remove(firstInter);
					// minD=Config.MIN_DOUBLE;
					double firstMax = firstInter.getMax();
					if (firstMax == Double.POSITIVE_INFINITY) {
						firstMax = Math.abs(maxRestrainValue);
						minD = 0 - firstMax;
						dd.setDomainStatus(3);
					} else {
						minD = (firstMax < 0) ? firstMax
								- Math.abs(maxRestrainValue) : 0 - Math
								.abs(maxRestrainValue);
						// minD = firstMax-Math.abs(maxRestrainValue);
						dd.setDomainStatus(2);
					}
					firstInter = new DoubleInterval(minD, firstMax);
					dd.getIntervals().add(firstInter);
				}
				double maxD = dd.getMax();
				if (maxD == Double.POSITIVE_INFINITY) {
					DoubleInterval lastInter = dd.getIntervals().last();
					dd.getIntervals().remove(lastInter);
					// maxD=Config.MAX_DOUBLE;
					double lastMin = lastInter.getMin();
					if (lastMin == Double.NEGATIVE_INFINITY) {
						maxD = Math.abs(maxRestrainValue);
						lastMin = 0 - maxD;
						dd.setDomainStatus(3);
					} else {
						// maxD = lastMin+Math.abs(maxRestrainValue);
						maxD = (lastMin > 0) ? lastMin
								+ Math.abs(maxRestrainValue) : 0 + Math
								.abs(maxRestrainValue);

						dd.setDomainStatus(1);
					}
					lastInter = new DoubleInterval(lastMin, maxD);
					dd.getIntervals().add(lastInter);
				}
				break;
			case POINTER:

			default:

			}
			
			return ret;
		} else
			return null;

	}

	/**
	 * For��֧�޽���������ʼ��
	 * @param domain
	 * @param maxRestrainValue
	 * @param sf
	 * @return
	 */
	public static Domain initializeNew(Domain domain, double maxRestrainValue,
			SymbolFactor sf) {
		// TODO Auto-generated method stub
		if (domain != null && !domain.initialized) {
			Domain ret = domain ; 
			CType_BaseType baseType = null;
			if(sf.getType() instanceof CType_BaseType )
				baseType =   (CType_BaseType) sf.getType();
			else if(sf.getType() instanceof CType_Typedef && ((CType_Typedef) sf.getType()).getOriginaltype() instanceof CType_BaseType)
				baseType = (CType_BaseType) ((CType_Typedef) sf.getType()).getOriginaltype();
			else if(sf.getType() instanceof CType_Enum || (sf.getType() instanceof CType_Typedef &&  ((CType_Typedef) sf.getType()).getOriginaltype() instanceof CType_Enum))
				return domain;
			else if(sf.getType() instanceof CType_Qualified && ((CType_Qualified) sf.getType()).getOriginaltype() instanceof CType_BaseType)
				return domain;
			else
				return domain;
			
			// �����unsigned ������char���ͣ��Է�Χ���в���
			if (baseType.toString().contains("char")) { // �ַ���
				if (CType_BaseType.uCharType == baseType) {// ������(0,255)��this
					IntegerDomain CharDomain = new IntegerDomain(0, 255);
					ret = (IntegerDomain) Domain.intersect(domain, CharDomain,
							CType_BaseType.uCharType);
				} else { // signed char����char
					IntegerDomain CharDomain = new IntegerDomain(-128, 127);
					ret = (IntegerDomain) Domain.intersect(domain, CharDomain,
							CType_BaseType.uCharType);
				}

			} else if (baseType.toString().contains("unsigned")) {// ������unsigned�����
				IntegerDomain intDomain = new IntegerDomain(0, Long.MAX_VALUE);
				ret = (IntegerDomain) Domain.intersect(domain, intDomain,
						CType_BaseType.uCharType);
	
			} 
			
			
			
			switch (domain.domaintype) {
			case INTEGER:
				IntegerDomain id = (IntegerDomain) ret;
				id.setDomainStatus(0);
				long minI = id.getMin();
				if (minI == Long.MIN_VALUE) {
					IntegerInterval firstInter = id.intervals.first();
					id.intervals.remove(firstInter);
					// minI=Config.MIN_INTEGER;
					long firstMax = firstInter.getMax();
					if (firstMax == Long.MAX_VALUE) {
						firstMax = Math.abs((long) maxRestrainValue);
						minI = 0 - firstMax;

						id.setDomainStatus(3); // ���ע������������״̬����䣬���û�ж�̬�ı�����Ĳ���
					} else {
						// minI = firstMax-Math.abs((long)maxRestrainValue);
						minI = (firstMax < 0) ? firstMax
								- Math.abs((long) maxRestrainValue) : 0 - Math
								.abs((long) maxRestrainValue);

						id.setDomainStatus(2);
					}
					firstInter = new IntegerInterval(minI, firstMax);
					id.intervals.add(firstInter);
				}
				long maxI = id.getMax();
				if (maxI == Long.MAX_VALUE) {
					IntegerInterval lastInter = id.intervals.last();
					id.intervals.remove(lastInter);
					// maxI=Config.MAX_INTEGER;
					long lastMin = lastInter.getMin();
					if (lastMin == Long.MIN_VALUE) {
						maxI = Math.abs((long) maxRestrainValue);
						lastMin = 0 - maxI;
						id.setDomainStatus(3);
					} else {
						// maxI = lastMin+Math.abs((long)maxRestrainValue);
						maxI = (lastMin > 0) ? lastMin
								+ Math.abs((long) maxRestrainValue) : 0 + Math
								.abs((long) maxRestrainValue);

						id.setDomainStatus(1);
					}
					lastInter = new IntegerInterval(lastMin, maxI);
					id.intervals.add(lastInter);
				}
				break;
			case DOUBLE:
				DoubleDomain dd = (DoubleDomain) ret;
				dd.setDomainStatus(0);
				double minD = dd.getMin();
				if (minD == Double.NEGATIVE_INFINITY) {
					DoubleInterval firstInter = dd.getIntervals().first();
					dd.getIntervals().remove(firstInter);
					// minD=Config.MIN_DOUBLE;
					double firstMax = firstInter.getMax();
					if (firstMax == Double.POSITIVE_INFINITY) {
						firstMax = Math.abs(maxRestrainValue);
						minD = 0 - firstMax;
						dd.setDomainStatus(3);
					} else {
						minD = (firstMax < 0) ? firstMax
								- Math.abs(maxRestrainValue) : 0 - Math
								.abs(maxRestrainValue);
						// minD = firstMax-Math.abs(maxRestrainValue);
						dd.setDomainStatus(2);
					}
					firstInter = new DoubleInterval(minD, firstMax);
					dd.getIntervals().add(firstInter);
				}
				double maxD = dd.getMax();
				if (maxD == Double.POSITIVE_INFINITY) {
					DoubleInterval lastInter = dd.getIntervals().last();
					dd.getIntervals().remove(lastInter);
					// maxD=Config.MAX_DOUBLE;
					double lastMin = lastInter.getMin();
					if (lastMin == Double.NEGATIVE_INFINITY) {
						maxD = Math.abs(maxRestrainValue);
						lastMin = 0 - maxD;
						dd.setDomainStatus(3);
					} else {
						// maxD = lastMin+Math.abs(maxRestrainValue);
						maxD = (lastMin > 0) ? lastMin
								+ Math.abs(maxRestrainValue) : 0 + Math
								.abs(maxRestrainValue);

						dd.setDomainStatus(1);
					}
					lastInter = new DoubleInterval(lastMin, maxD);
					dd.getIntervals().add(lastInter);
				}
				break;
			case POINTER:

			default:

			}
			
			return ret;
		} else
			return null;

	}
	
	
	
	public void initializeOrder(double maxRestrainValue) {
		maxRestrainValue = getOrderByValue(maxRestrainValue);// ����
		if (!initialized) {
			
			
			
			
			switch (domaintype) {
			case INTEGER:
				IntegerDomain id = (IntegerDomain) this;
				id.setDomainStatus(0);
				long minI = id.getMin();
				if (minI == Long.MIN_VALUE) {
					IntegerInterval firstInter = id.intervals.first();
					id.intervals.remove(firstInter);
					// minI=Config.MIN_INTEGER;
					long firstMax = firstInter.getMax();
					if (firstMax == Long.MAX_VALUE) {
						firstMax = Math.abs((long) maxRestrainValue);
						minI = 0 - firstMax;
						id.setDomainStatus(3); // ���ע������������״̬����䣬���û�ж�̬�ı�����Ĳ���
					} else {
						minI = (firstMax < 0) ? firstMax
								- Math.abs((long) maxRestrainValue) : 0 - Math
								.abs((long) maxRestrainValue);

						id.setDomainStatus(2);
					}
					firstInter = new IntegerInterval(minI, firstMax);
					id.intervals.add(firstInter);
				}
				long maxI = id.getMax();
				if (maxI == Long.MAX_VALUE) {
					IntegerInterval lastInter = id.intervals.last();
					id.intervals.remove(lastInter);
					// maxI=Config.MAX_INTEGER;
					long lastMin = lastInter.getMin();
					if (lastMin == Long.MIN_VALUE) {
						maxI = Math.abs((long) maxRestrainValue);
						lastMin = 0 - maxI;
						id.setDomainStatus(3);
					} else {
						maxI = (lastMin > 0) ? lastMin
								+ Math.abs((long) maxRestrainValue) : 0 + Math
								.abs((long) maxRestrainValue);
						id.setDomainStatus(1);
					}
					lastInter = new IntegerInterval(lastMin, maxI);
					id.intervals.add(lastInter);
				}
				break;
			case DOUBLE:
				DoubleDomain dd = (DoubleDomain) this;
				dd.setDomainStatus(0);
				double minD = dd.getMin();
				if (minD == Double.NEGATIVE_INFINITY) {
					DoubleInterval firstInter = dd.getIntervals().first();
					dd.getIntervals().remove(firstInter);
					// minD=Config.MIN_DOUBLE;
					double firstMax = firstInter.getMax();
					if (firstMax == Double.POSITIVE_INFINITY) {
						firstMax = Math.abs(maxRestrainValue);
						minD = 0 - firstMax;
						dd.setDomainStatus(3);
					} else {
						minD = (firstMax < 0) ? firstMax
								- Math.abs(maxRestrainValue) : 0 - Math
								.abs(maxRestrainValue);
						// minD = firstMax-Math.abs(maxRestrainValue);
						dd.setDomainStatus(2);
					}
					firstInter = new DoubleInterval(minD, firstMax);
					dd.getIntervals().add(firstInter);
				}
				double maxD = dd.getMax();
				if (maxD == Double.POSITIVE_INFINITY) {
					DoubleInterval lastInter = dd.getIntervals().last();
					dd.getIntervals().remove(lastInter);
					// maxD=Config.MAX_DOUBLE;
					double lastMin = lastInter.getMin();
					if (lastMin == Double.NEGATIVE_INFINITY) {
						maxD = Math.abs(maxRestrainValue);
						lastMin = 0 - maxD;
						dd.setDomainStatus(3);
					} else {
						maxD = (lastMin > 0) ? lastMin
								+ Math.abs(maxRestrainValue) : 0 + Math
								.abs(maxRestrainValue);
						// maxD = lastMin+Math.abs(maxRestrainValue);
						dd.setDomainStatus(1);
					}
					lastInter = new DoubleInterval(lastMin, maxD);
					dd.getIntervals().add(lastInter);
				}
				break;
			case POINTER:

			default:

			}
		}
	}

	/**
	 * ����ֵ����Ӧ��ͬ������������� ����35 ����99 236 ����999
	 * 
	 * @param maxRestrainValue
	 * @return
	 */
	private double getOrderByValue(double maxRestrainValue) {
		// TODO Auto-generated method stub
		double resultOrder = 10;
		while ((maxRestrainValue /= 10) > 1) {

			resultOrder *= 10;
		}
		return resultOrder - 1;
	}
	public DoubleDomain integerToDouble()//added by baiyu 2014/5/30
	{
		DoubleDomain result=new DoubleDomain();
		return  result;
	}
}