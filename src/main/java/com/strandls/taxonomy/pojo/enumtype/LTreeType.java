package com.strandls.taxonomy.pojo.enumtype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

public class LTreeType implements UserType{

	@Override
	public int[] sqlTypes() {
		return  new int[] {Types.OTHER};
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class returnedClass() {
		return String.class;
	}

	@Override
	public boolean equals(Object x, Object y) {
		return x.equals(y);
	}

	@Override
	public int hashCode(Object x) {
		return x.hashCode();
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws SQLException {
		return rs.getString(names[0]);
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws SQLException {
		st.setObject(index, value, Types.OTHER);
	}

	@Override
	public Object deepCopy(Object value) {
		return new String((String) value);
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value) {
		return (Serializable) value;
	}

	@Override
	public Object assemble(Serializable cached, Object owner) {
		return cached;
	}

	@Override
	public Object replace(Object original, Object target, Object owner) {
		return deepCopy(original);
	}

}
