package ca.on.oicr.gsi.shesmu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Tree<T> {
	public enum Scope {
		ALL, LEAVES, ROOT
	}

	public static final LongPredicate SINGULAR = x -> x == 1;

	public static <T> Function<Stream<Tree<T>>, Stream<Tree<T>>> all(Predicate<T> predicate, Scope scope) {
		return s -> s.filter(t -> t.reduce(true, (a, v) -> a && predicate.test(v), (a, b) -> a && b, scope));
	}

	public static <T> Function<Stream<Tree<T>>, Stream<Tree<T>>> any(Predicate<T> predicate, Scope scope) {
		return s -> s.filter(t -> t.reduce(false, (a, v) -> a || predicate.test(v), (a, b) -> a || b, scope));
	}

	public static <T> Function<Stream<Tree<T>>, Stream<Tree<T>>> collect(
			Function<Stream<Tree<T>>, Stream<Tree<T>>> input, Scope scope) {
		return input.andThen(s -> s.flatMap(t -> t.stream(scope)));
	}

	public static <T, G, K> Stream<T> filter(Stream<T> input, Function<? super T, G> grouper,
			Function<? super T, ? extends K> makeKey, Function<? super T, ? extends K> getParent,
			LongPredicate groupCheck, Function<Stream<Tree<T>>, Stream<Tree<T>>> chooser) {
		Map<G, List<T>> groups = input.collect(Collectors.groupingBy(grouper));

		return groups.values().stream()//
				.flatMap(group -> {
					Map<K, Tree<T>> treeParts = group.stream()//
							.collect(Collectors.toMap(makeKey, Tree::new));
					group.stream().forEach(item -> {
						Tree<T> parent = treeParts.get(getParent.apply(item));
						if (parent != null) {
							parent.attach(treeParts.get(makeKey.apply(item)));
						}
					});
					if (groupCheck.test(treeParts.values().stream()//
							.filter(Tree::isRoot)//
							.count())) {
						return chooser.apply(//
								treeParts.values().stream()//
										.filter(Tree::isRoot))//
								.map(Tree::value);
					}
					return Stream.empty();
				});
	}

	public static <T, U extends Comparable<U>> Function<Stream<Tree<T>>, Stream<Tree<T>>> optima(boolean max,
			Function<T, U> extractor, Scope scope) {
		Comparator<Pair<Tree<T>, U>> comparator = Comparator.comparing(Pair::second);
		if (max) {
			comparator = comparator.reversed();
		}
		final Comparator<Pair<Tree<T>, U>> comp = comparator;
		return s -> s.map(t -> new Pair<>(t, t.<U>reduce(null, (a, v) -> {
			U b = extractor.apply(v);
			return a == null || (a.compareTo(b) < 0) ? b : a;
		}, (a, b) -> {
			return a == null || (a.compareTo(b) < 0) ? b : a;
		}, scope)))//
				.sorted(comp)//
				.findFirst()//
				.map(p -> Stream.of(p.first()))//
				.orElseGet(Stream::empty);
	}

	private final List<Tree<T>> children = new ArrayList<>();

	private boolean root = true;
	private final T value;
	private Tree(T value) {
		this.value = value;
	}

	private void attach(Tree<T> child) {
		children.add(child);
		child.root = false;
	}

	private boolean isRoot() {
		return root;
	}

	public <R> R reduce(R initial, BiFunction<? super R, ? super T, R> function, BinaryOperator<R> combiner,
			Scope scope) {
		if (scope == Scope.ROOT || scope == Scope.ALL || scope == Scope.LEAVES && children.isEmpty()) {
			initial = function.apply(initial, value);
		}
		if (scope == Scope.ALL || scope == Scope.LEAVES) {
			initial = children.stream().reduce(initial, (i, t) -> t.reduce(i, function, combiner, scope), combiner);
		}
		return initial;
	}

	private Stream<Tree<T>> stream(Scope scope) {
		switch (scope) {
		case ALL:
			return Stream.concat(Stream.of(this), children.stream().flatMap(c -> c.stream(scope)));
		case LEAVES:
			return children.isEmpty() ? Stream.of(this) : children.stream().flatMap(c -> c.stream(scope));
		case ROOT:
			return Stream.of(this);
		default:
			throw new UnsupportedOperationException();
		}
	}

	private T value() {
		return value;
	}
}
