package ca.on.oicr.gsi.shesmu.compiler;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import ca.on.oicr.gsi.shesmu.Lookup;
import ca.on.oicr.gsi.shesmu.ParameterDefinition;

/**
 * The arguments defined in the “With” section of a “Run” olive.
 */
public final class OliveArgumentNodeProvided extends OliveArgumentNode {

	private ParameterDefinition definition;
	private final ExpressionNode expression;

	public OliveArgumentNodeProvided(int line, int column, String name, ExpressionNode expression) {
		super(line, column, name);
		this.expression = expression;
	}

	@Override
	public void collectFreeVariables(Set<String> freeVariables) {
		expression.collectFreeVariables(freeVariables);
	}

	/**
	 * Produce an error if the type of the expression is not as required
	 *
	 * @param targetType
	 *            the required type
	 */
	@Override
	public boolean ensureType(ParameterDefinition definition, Consumer<String> errorHandler) {
		this.definition = definition;
		final boolean ok = definition.type().isSame(expression.type());
		if (!ok) {
			errorHandler.accept(String.format("%d:%d: Expected argument “%s” to have type %s, but got %s.", line,
					column, name, definition.type().name(), expression.type().name()));
		}
		return ok;
	}

	/**
	 * Generate bytecode for this argument's value
	 */
	@Override
	public void render(Renderer renderer, int action) {
		definition.store(renderer, action, expression::render);

	}

	/**
	 * Resolve variables in the expression of this argument
	 */
	@Override
	public boolean resolve(NameDefinitions defs, Consumer<String> errorHandler) {
		return expression.resolve(defs, errorHandler);
	}

	/**
	 * Resolve lookups in this argument
	 */
	@Override
	public boolean resolveLookups(Function<String, Lookup> definedLookups, Consumer<String> errorHandler) {
		return expression.resolveLookups(definedLookups, errorHandler);

	}

	/**
	 * Perform type check on this argument's expression
	 */
	@Override
	public boolean typeCheck(Consumer<String> errorHandler) {
		return expression.typeCheck(errorHandler);
	}
}