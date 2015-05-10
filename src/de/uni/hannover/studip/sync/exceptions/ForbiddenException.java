package de.uni.hannover.studip.sync.exceptions;

public class ForbiddenException extends Exception {

	private static final long serialVersionUID = 1L;

	public ForbiddenException(final String message) {
		super(message);
	}
	
}
