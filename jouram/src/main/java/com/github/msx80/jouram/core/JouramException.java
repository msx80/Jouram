package com.github.msx80.jouram.core;

public class JouramException extends RuntimeException {


	private static final long serialVersionUID = 3644649371651256610L;

	public JouramException() {
	}

	public JouramException(String message) {
		super(message);
	}

	public JouramException(Throwable cause) {
		super(cause);
	}

	public JouramException(String message, Throwable cause) {
		super(message, cause);
	}

	public JouramException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
