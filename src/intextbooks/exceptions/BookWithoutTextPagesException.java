package intextbooks.exceptions;

public class BookWithoutTextPagesException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BookWithoutTextPagesException(String message) {
		super(message);
	}
}
