package intextbooks.exceptions;

public class BookWithoutPageNumbersException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BookWithoutPageNumbersException(String message) {
		super(message);
	}
}
