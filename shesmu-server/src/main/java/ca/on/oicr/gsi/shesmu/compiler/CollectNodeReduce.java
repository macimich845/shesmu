package ca.on.oicr.gsi.shesmu.compiler;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import ca.on.oicr.gsi.shesmu.FunctionDefinition;
import ca.on.oicr.gsi.shesmu.Imyhat;
import ca.on.oicr.gsi.shesmu.compiler.Target.Flavour;

public class CollectNodeReduce extends CollectNode {

	private final Target accumulator = new Target() {

		@Override
		public Flavour flavour() {
			return Flavour.LAMBDA;
		}

		@Override
		public String name() {
			return accumulatorName;
		}

		@Override
		public Imyhat type() {
			return initial.type();
		}

	};
	private final String accumulatorName;
	private final ExpressionNode initial;
	private String name;

	private final Target parameter = new Target() {

		@Override
		public Flavour flavour() {
			return Flavour.LAMBDA;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public Imyhat type() {
			return type;
		}

	};

	private final ExpressionNode reducer;

	Imyhat type;

	public CollectNodeReduce(int line, int column, String accumulatorName, ExpressionNode reducer,
			ExpressionNode initial) {
		super(line, column);
		this.accumulatorName = accumulatorName;
		this.reducer = reducer;
		this.initial = initial;
	}

	@Override
	public void collectFreeVariables(Set<String> names, Predicate<Flavour> predicate) {
		initial.collectFreeVariables(names, predicate);
		final boolean remove = !names.contains(name);
		final boolean removeAccumulator = !names.contains(accumulatorName);
		reducer.collectFreeVariables(names, predicate);
		if (remove) {
			names.remove(name);
		}
		if (removeAccumulator) {
			names.remove(accumulatorName);
		}
	}

	@Override
	public void render(JavaStreamBuilder builder) {
		final Set<String> capturedNames = new HashSet<>();
		reducer.collectFreeVariables(capturedNames, Flavour::needsCapture);
		capturedNames.remove(name);
		capturedNames.remove(accumulatorName);
		final Renderer reducerRenderer = builder.reduce(name, initial.type(), accumulatorName, initial::render, builder
				.renderer().allValues().filter(v -> capturedNames.contains(v.name())).toArray(LoadableValue[]::new));
		reducerRenderer.methodGen().visitCode();
		reducer.render(reducerRenderer);
		reducerRenderer.methodGen().returnValue();
		reducerRenderer.methodGen().visitMaxs(0, 0);
		reducerRenderer.methodGen().visitEnd();
	}

	@Override
	public boolean resolve(String name, NameDefinitions defs, Consumer<String> errorHandler) {
		this.name = name;
		return initial.resolve(defs, errorHandler)
				& reducer.resolve(defs.bind(parameter).bind(accumulator), errorHandler);
	}

	@Override
	public boolean resolveFunctions(Function<String, FunctionDefinition> definedFunctions,
			Consumer<String> errorHandler) {
		return initial.resolveFunctions(definedFunctions, errorHandler)
				& reducer.resolveFunctions(definedFunctions, errorHandler);
	}

	@Override
	public Imyhat type() {
		return initial.type();
	}

	@Override
	public boolean typeCheck(Imyhat incoming, Consumer<String> errorHandler) {
		type = incoming;
		boolean ok = initial.typeCheck(errorHandler) & reducer.typeCheck(errorHandler);
		if (ok) {
			if (!initial.type().isSame(reducer.type())) {
				errorHandler.accept(String.format("%d:%d: Reducer produces type %s, but initial expression is %s.",
						line(), column(), reducer.type().name(), initial.type().name()));
				ok = false;
			}
		}
		return ok;
	}

}
