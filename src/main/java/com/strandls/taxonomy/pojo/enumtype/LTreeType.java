package com.strandls.taxonomy.pojo.enumtype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

public class LTreeType implements UserType<String> {

	@Override
	public int getSqlType() {
		return Types.OTHER;
	}

	@Override
	public Class<String> returnedClass() {
		return String.class;
	}

	@Override
	public boolean equals(String x, String y) {
		return Objects.equals(x, y);
	}

	@Override
	public int hashCode(String x) {
		return Objects.hashCode(x);
	}

	@Override
	public String nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
			throws SQLException {
		return rs.getString(position);
	}

	@Override
	public void nullSafeSet(PreparedStatement st, String value, int index, SharedSessionContractImplementor session)
			throws SQLException {
		if (value == null) {
			st.setNull(index, Types.OTHER);
		} else {
			st.setObject(index, value, Types.OTHER);
		}
	}

	@Override
	public String deepCopy(String value) {
		return value == null ? null : new String(value);
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(String value) {
		return value;
	}

	@Override
	public String assemble(Serializable cached, Object owner) {
		return (String) cached;
	}

	@Override
	public String replace(String original, String target, Object owner) {
		return original;
	}
}
