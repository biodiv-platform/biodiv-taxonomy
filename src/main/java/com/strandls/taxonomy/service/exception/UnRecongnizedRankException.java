package com.strandls.taxonomy.service.exception;

public class UnRecongnizedRankException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String message;

	public UnRecongnizedRankException(String string) {
		this.message = string;
	}
	
	@Override
	public String toString() {
		return message;
	}
}
