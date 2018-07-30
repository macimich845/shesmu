package ca.on.oicr.gsi.shesmu;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ca.on.oicr.gsi.shesmu.ActionProcessor.Filter;

/**
 * Translate JSON-formatted queries into Java objects and perform the query
 */
public class Query {
	public static class FilterAfter extends FilterJson {
		private long epoch;

		@Override
		public Filter convert() {
			return ActionProcessor.updatedAfter(Instant.ofEpochSecond(getEpoch()));
		}

		public long getEpoch() {
			return epoch;
		}

		public void setEpoch(long epoch) {
			this.epoch = epoch;
		}

	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@JsonSubTypes({ //
			@Type(value = FilterStatus.class, name = "status"), //
			@Type(value = FilterAfter.class, name = "after"), //
			@Type(value = FilterType.class, name = "type") })
	public static abstract class FilterJson {
		public abstract Filter convert();
	}

	public static class FilterStatus extends FilterJson {
		private ActionState[] states;

		@Override
		public Filter convert() {
			return ActionProcessor.isState(states);
		}

		public ActionState[] getStates() {
			return states;
		}

		public void setState(ActionState[] states) {
			this.states = states;
		}
	}

	public static class FilterType extends FilterJson {
		private String[] types;

		@Override
		public Filter convert() {
			return ActionProcessor.type(types);
		}

		public String[] getTypes() {
			return types;
		}

		public void setTypes(String[] types) {
			this.types = types;
		}

	}

	private class Limiter<T> implements Predicate<T> {
		private long count;
		private final long limit;

		public Limiter(long hardLimit) {
			super();
			this.limit = Math.max(1, Math.min(getLimit(), hardLimit)) + Math.max(0, getSkip());
		}

		@Override
		public boolean test(T t) {
			return ++count < limit;
		}

	}

	FilterJson[] filters;

	long limit = 100;

	long skip = 0;

	public FilterJson[] getFilters() {
		return filters;
	}

	public long getLimit() {
		return limit;
	}

	public long getSkip() {
		return skip;
	}

	public Response perform(ObjectMapper mapper, ActionProcessor processor) throws IOException {
		final Filter[] filters = Arrays.stream(getFilters()).map(FilterJson::convert).toArray(Filter[]::new);
		final Response result = new Response();
		final Limiter<ObjectNode> limiter = new Limiter<>(500);
		result.setResults(processor.stream(mapper, filters)//
				.filter(limiter)//
				.skip(Math.max(0, getSkip()))//
				.toArray(ObjectNode[]::new));
		result.setTotal(limiter.count);
		return result;
	}

	public void setFilters(FilterJson[] filters) {
		this.filters = filters;
	}

	public void setLimit(long limit) {
		this.limit = limit;
	}

	public void setSkip(long skip) {
		this.skip = skip;
	}
}