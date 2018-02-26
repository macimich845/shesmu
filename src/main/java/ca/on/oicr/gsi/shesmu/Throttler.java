package ca.on.oicr.gsi.shesmu;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Throttler extends LoadedConfiguration {
	public static final String ENVIRONMENT = Optional.ofNullable(System.getenv("SHESMU_ENVIRONMENT"))
			.orElse("production");

	public static final ServiceLoader<Throttler> SERVICE_LOADER = ServiceLoader.load(Throttler.class);

	public static boolean anyOverloaded(Set<String> services) {
		return services().anyMatch(t -> t.isOverloaded(ENVIRONMENT, services));
	}

	public static boolean anyOverloaded(String... services) {
		return anyOverloaded(new HashSet<>(Arrays.asList(services)));

	}

	public static Stream<Throttler> services() {
		return StreamSupport.stream(SERVICE_LOADER.spliterator(), false);
	}

	public boolean isOverloaded(String environment, Set<String> services);

}