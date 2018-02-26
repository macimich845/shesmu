package ca.on.oicr.gsi.shesmu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RunTest {

	private class ActionChecker implements Consumer<Action> {

		private int bad;
		private int good;

		@Override
		public void accept(Action t) {
			if (t.perform() == ActionState.SUCCEEDED) {
				good++;
			} else {
				bad++;
			}
		}

		public boolean ok() {
			return bad == 0 && good > 0;
		}

	}

	public static class OkAction extends Action {

		public boolean ok;

		@Override
		public boolean equals(Object other) {
			return this == other;
		}

		@Override
		public int hashCode() {
			return 31;
		}

		@Override
		public ActionState perform() {
			return ok ? ActionState.SUCCEEDED : ActionState.FAILED;
		}

		@Override
		public int priority() {
			return 0;
		}

		@Override
		public long retryMinutes() {
			return 15;
		}

		@Override
		public ObjectNode toJson(ObjectMapper mapper) {
			return mapper.createObjectNode().put("ok", ok);
		}

	}

	private static final Type A_OK_ACTION_TYPE = Type.getType(OkAction.class);

	private static Variables[] DATA = new Variables[] {
			new Variables("1", "/foo1", "text/x-nothing", "94d1a7503ff45e5a205a51dd3841f36f", 3, "SlowA",
					new Tuple(1L, 2L, 3L), "the_foo_study", "unknown_sample", "that_guy",
					new Tuple("RUN", 1L, "AACCGGTT"), "EX", "Fresh", "An", "Frozen", "", "Inside", "", "", 307L,
					"pointy", "test"),
			new Variables("2", "/foo2", "text/x-nothing", "f031dcdb95c4ff2fbbc52a6be6c38117", 4, "SlowA",
					new Tuple(1L, 2L, 3L), "the_foo_study", "unknown_sample", "that_guy",
					new Tuple("RUN", 1L, "ACGTACGT"), "EX", "Fresh", "nn", "Frozen", "", "Inside", "", "", 300L,
					"pointy", "test") };

	private static final Lookup INT2DATE = new Lookup() {

		@Override
		public Object lookup(Object... parameters) {
			return Instant.ofEpochSecond(((Long) parameters[0]).longValue());
		}

		@Override
		public String name() {
			return "int_to_date";
		}

		@Override
		public Imyhat returnType() {
			return Imyhat.DATE;
		}

		@Override
		public Stream<Imyhat> types() {
			return Stream.of(Imyhat.INTEGER);
		}
	};

	private static final Lookup INT2STR = new Lookup() {

		@Override
		public Object lookup(Object... parameters) {
			return parameters[0].toString();
		}

		@Override
		public String name() {
			return "int_to_str";
		}

		@Override
		public Imyhat returnType() {
			return Imyhat.STRING;
		}

		@Override
		public Stream<Imyhat> types() {
			return Stream.of(Imyhat.INTEGER);
		}
	};
	private static final ActionDefinition OK_ACTION_DEFINITION = new ActionDefinition("ok", A_OK_ACTION_TYPE,
			Stream.of(ParameterDefinition.forField(A_OK_ACTION_TYPE, "ok", Imyhat.BOOLEAN))) {

		@Override
		public void initialize(GeneratorAdapter methodGen) {
			methodGen.newInstance(A_OK_ACTION_TYPE);
			methodGen.dup();
			methodGen.invokeConstructor(A_OK_ACTION_TYPE, new Method("<init>", Type.VOID_TYPE, new Type[] {}));
		}
	};

	private Stream<ActionDefinition> actions() {
		return Stream.of(OK_ACTION_DEFINITION);
	}

	private Stream<Variables> data() {
		return Arrays.stream(DATA);
	}

	private Stream<Lookup> lookups() {
		return Stream.of(INT2STR, INT2DATE);
	}

	@Test
	public void testData() throws IOException {
		System.err.println("Testing data-handling code");
		try (Stream<Path> files = Files.walk(Paths.get(this.getClass().getResource("/run").getPath()), 1)) {
			Assert.assertTrue("Sample program failed to run!", files//
					.filter(Files::isRegularFile)//
					.filter(f -> f.getFileName().toString().endsWith(".shesmu"))//
					.sorted((a, b) -> a.getFileName().compareTo(b.getFileName()))//
					.filter(this::testFile)//
					.count() == 0);
		}

	}

	private boolean testFile(Path file) {
		try {
			HotloadingCompiler compiler = new HotloadingCompiler(this::lookups, this::actions);
			ActionGenerator generator = compiler.compile(file).orElse(ActionGenerator.NULL);
			ActionChecker checker = new ActionChecker();
			generator.populateLookups(new NameLoader<>(lookups(), Lookup::name));
			generator.run(checker, this::data);
			if (checker.ok()) {
				System.err.printf("OK %s\n", file.getFileName());
				return false;
			} else {
				System.err.printf("FAIL %s\n", file.getFileName());
				return true;
			}
		} catch (Exception | VerifyError e) {
			System.err.printf("EXCP %s\n", file.getFileName());
			e.printStackTrace();
			return true;

		}
	}
}