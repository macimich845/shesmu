package ca.on.oicr.gsi.shesmu.throttler.scheduled;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.kohsuke.MetaInfServices;

import ca.on.oicr.gsi.shesmu.AutoUpdatingDirectory;
import ca.on.oicr.gsi.shesmu.Pair;
import ca.on.oicr.gsi.shesmu.Throttler;
import ca.on.oicr.gsi.shesmu.WatchedFileListener;

/**
 * Reads <tt>maintenance.tsv</tt> which is a schedule or maintenance windows
 * during which all activity should be throttled.
 */
@MetaInfServices(Throttler.class)
public class MaintenanceSchedule implements Throttler {

	private static class ScheduleReader implements WatchedFileListener {

		private final Path fileName;
		private List<Instant[]> windows = Collections.emptyList();

		public ScheduleReader(Path fileName) {
			this.fileName = fileName;
		}

		public Pair<String, Map<String, String>> configuration() {
			final Map<String, String> properties = new HashMap<>();
			properties.put("state", inMaintenanceWindow() ? "throttled" : "permit");
			for (int i = 0; i < windows.size(); i++) {
				properties.put(String.format("Window %d", i),
						String.format("%s - %s", windows.get(i)[0], windows.get(i)[1]));
			}
			return new Pair<>("Maintenance Window Throttler: " + fileName.toString(), properties);
		}

		public boolean inMaintenanceWindow() {
			final Instant now = Instant.now();
			return windows.stream().anyMatch(window -> now.isAfter(window[0]) && now.isBefore(window[1]));
		}

		@Override
		public void start() {
			update();
		}

		@Override
		public void stop() {

		}

		@Override
		public Optional<Integer> update() {
			try {
				windows = Files.readAllLines(fileName).stream()//
						.map(line -> BLANK.splitAsStream(line)//
								.limit(2)//
								.map(str -> ZonedDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME).toInstant())//
								.toArray(Instant[]::new))//
						.filter(times -> times[0].isBefore(times[1]))//
						.sorted((a, b) -> a[0].compareTo(b[0]))//
						.collect(Collectors.toList());
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return Optional.empty();
		}
	}

	private static final Pattern BLANK = Pattern.compile("\\s+");

	private final AutoUpdatingDirectory<ScheduleReader> schedules = new AutoUpdatingDirectory<>(".schedule",
			ScheduleReader::new);

	@Override
	public boolean isOverloaded(Set<String> services) {
		return schedules.stream().anyMatch(ScheduleReader::inMaintenanceWindow);
	}

	@Override
	public Stream<Pair<String, Map<String, String>>> listConfiguration() {
		return schedules.stream().map(ScheduleReader::configuration);
	}

}
