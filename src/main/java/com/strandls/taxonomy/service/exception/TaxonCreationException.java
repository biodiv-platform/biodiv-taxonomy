package com.strandls.taxonomy.service.exception;

public class TaxonCreationException extends Exception {

	/** */
	private static final long serialVersionUID = 1L;

	private final String message;

	public TaxonCreationException(String string) {
		this.message = string;
	}

	@Override
	public String toString() {
		return message;
	}
}
