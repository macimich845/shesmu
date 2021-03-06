package ca.on.oicr.gsi.shesmu.compiler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import ca.on.oicr.gsi.shesmu.ActionDefinition;
import ca.on.oicr.gsi.shesmu.Constant;
import ca.on.oicr.gsi.shesmu.FunctionDefinition;
import ca.on.oicr.gsi.shesmu.Imyhat;
import ca.on.oicr.gsi.shesmu.InputFormatDefinition;

public class ProgramNode {
	/**
	 * Parse a file of olive nodes
	 */
	public static boolean parseFile(CharSequence input, Consumer<ProgramNode> output, ErrorConsumer errorHandler) {
		final AtomicReference<String> inputFormat = new AtomicReference<>();
		final AtomicReference<List<OliveNode>> olives = new AtomicReference<>();
		final Parser result = Parser.start(input, errorHandler)//
				.whitespace()//
				.keyword("Input")//
				.whitespace()//
				.identifier(inputFormat::set)//
				.whitespace()//
				.symbol(";")//
				.whitespace()//
				.list(olives::set, OliveNode::parse)//
				.whitespace();
		if (result.isGood()) {
			if (result.isEmpty()) {
				output.accept(new ProgramNode(inputFormat.get(), olives.get()));
				return true;
			} else {
				errorHandler.raise(result.line(), result.column(), "Junk at end of file.");
			}
		}
		return false;
	}

	private final String input;

	private InputFormatDefinition inputFormatDefinition;

	private final List<OliveNode> olives;

	public ProgramNode(String input, List<OliveNode> olives) {
		super();
		this.input = input;
		this.olives = olives;
	}

	public InputFormatDefinition inputFormatDefinition() {
		return inputFormatDefinition;
	}

	/**
	 * Generate bytecode for this definition
	 */
	public void render(RootBuilder builder) {
		final Map<String, OliveDefineBuilder> definitions = new HashMap<>();
		olives.forEach(olive -> olive.build(builder, definitions));
		olives.forEach(olive -> olive.render(builder, definitions));
	}

	/**
	 * Check that a collection of olives, assumed to be a self-contained program, is
	 * well-formed.
	 *
	 * @param definedFunctions
	 *            the functions available; if a function is not found, null should
	 *            be returned
	 * @param definedActions
	 *            the actions available; if an action is not found, null should be
	 *            returned
	 * @param constants
	 */
	public boolean validate(Function<String, InputFormatDefinition> inputFormatDefinitions,
			Function<String, FunctionDefinition> definedFunctions, Function<String, ActionDefinition> definedActions,
			Consumer<String> errorHandler, Supplier<Stream<Constant>> constants) {

		inputFormatDefinition = inputFormatDefinitions.apply(input);
		if (inputFormatDefinition == null) {
			errorHandler.accept(String.format("No input format of data named “%s” is available.", input));
			return false;
		}
		// Find and resolve olive “Define” and “Matches”
		final Map<String, OliveNodeDefinition> definedOlives = new HashMap<>();
		final Set<String> metricNames = new HashSet<>();
		final Map<String, List<Imyhat>> dumpers = new HashMap<>();
		boolean ok = olives.stream().filter(olive -> olive.collectDefinitions(definedOlives, errorHandler))
				.count() == olives.size();
		ok &= olives.stream().filter(olive -> olive.resolveDefinitions(definedOlives, definedFunctions, definedActions,
				metricNames, dumpers, errorHandler)).count() == olives.size();

		// Resolve variables
		if (ok) {
			ok = olives.stream().filter(
					olive -> olive.resolve(inputFormatDefinition, inputFormatDefinitions, errorHandler, constants))
					.count() == olives.size();
		}
		if (ok) {
			ok = olives.stream().filter(olive -> olive.checkVariableStream(errorHandler)).count() == olives.size();
		}

		// Type check the resolved structure
		if (ok) {
			ok = olives.stream().filter(olive -> olive.typeCheck(errorHandler)).count() == olives.size();
		}
		return ok;
	}

}
