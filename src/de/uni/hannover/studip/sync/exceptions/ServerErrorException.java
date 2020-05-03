package de.uni.hannover.studip.sync.exceptions;

public class ServerErrorException extends Exception {

	private static final long serialVersionUID = 1L;

	public ServerErrorException(final String message) {
		super(message);
	}
	
}
