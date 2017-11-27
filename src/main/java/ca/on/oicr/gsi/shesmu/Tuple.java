package ca.on.oicr.gsi.shesmu;

import java.util.Arrays;

/**
 * A fixed-length list of heterogenous values
 *
 * This class is mostly a thin wrapper on an array of objects with sensible
 * equals/hashcode methods
 */
public final class Tuple {
	private final Object[] elements;

	/**
	 * Create a new tuple from the specified array
	 *
	 * @param elements
	 *            the elements in the tuple; the array must not have any references
	 *            after passing to the constructors
	 */
	public Tuple(Object... elements) {
		super();
		this.elements = elements;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Tuple other = (Tuple) obj;
		if (!Arrays.equals(elements, other.elements)) {
			return false;
		}
		return true;
	}

	public Object get(int i) {
		return elements[i];
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(elements);
		return result;
	}

}
