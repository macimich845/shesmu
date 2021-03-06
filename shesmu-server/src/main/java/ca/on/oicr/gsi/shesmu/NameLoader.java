package ca.on.oicr.gsi.shesmu;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Packs named objects into a map for retrieval
 *
 * @param <T>
 *            the values in the map
 */
public final class NameLoader<T> {

	private final Map<String, T> items;

	/**
	 * Create a new map from the provided objects
	 *
	 * @param data
	 *            the stream of objects
	 * @param getName
	 *            a function to extract a name from each
	 */
	public NameLoader(Stream<T> data, Function<T, String> getName) {
		items = data.collect(Collectors.toMap(getName, Function.identity()));
	}

	/**
	 * Stream all the objects in the map
	 */
	public Stream<T> all() {
		return items.values().stream();
	}

	/**
	 * Get a particular object by name
	 */
	@RuntimeInterop
	public T get(String name) {
		return items.get(name);
	}
}
