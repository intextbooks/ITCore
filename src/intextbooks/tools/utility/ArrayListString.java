package intextbooks.tools.utility;

import java.util.ArrayList;

public class ArrayListString extends ArrayList<String> {
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Uses normalized strings for comparison (
	 * {@link StringUtils#normalizeWhitespace(String)})
	 */

	@Override
	public boolean contains(Object obj) {

		for (String s : this) {

			if (StringUtils.normalizeWhitespace(s).equals(obj)) {
				return true;
			}

		}

		return false;

	}

}
