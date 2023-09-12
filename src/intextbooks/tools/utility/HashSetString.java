package intextbooks.tools.utility;

import java.util.HashSet;

/**
 * Wrapper class for HashSet(String) with overridden contains method
 *
 * @author Kyrill
 *
 */

public class HashSetString extends HashSet<String> {

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
