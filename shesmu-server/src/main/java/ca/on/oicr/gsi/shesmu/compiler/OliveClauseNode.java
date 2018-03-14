package ca.on.oicr.gsi.shesmu.compiler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import ca.on.oicr.gsi.shesmu.ActionDefinition;
import ca.on.oicr.gsi.shesmu.Lookup;
import ca.on.oicr.gsi.shesmu.compiler.OliveNode.ClauseStreamOrder;

/**
 * Base type for an olive clause
 */
public abstract class OliveClauseNode {
	private static final Pattern HELP = Pattern.compile("^\"([^\"]*)\"");

	public static Parser parse(Parser input, Consumer<OliveClauseNode> output) {

		final Parser whereParser = input.keyword("Where");
		if (whereParser.isGood()) {
			final AtomicReference<ExpressionNode> expression = new AtomicReference<>();
			final Parser result = ExpressionNode.parse(whereParser.whitespace(), expression::set).whitespace();
			if (result.isGood()) {
				output.accept(new OliveClauseNodeWhere(input.line(), input.column(), expression.get()));
			}
			return result;
		}
		final Parser matchesParser = input.keyword("Matches");
		if (matchesParser.isGood()) {
			final AtomicReference<String> name = new AtomicReference<>();
			final AtomicReference<List<ExpressionNode>> arguments = new AtomicReference<>();
			final Parser result = matchesParser//
					.whitespace()//
					.identifier(name::set)//
					.whitespace()//
					.symbol("(")//
					.whitespace()//
					.listEmpty(arguments::set, ExpressionNode::parse, ',')//
					.whitespace()//
					.symbol(")")//
					.whitespace();
			if (result.isGood()) {
				output.accept(new OliveClauseNodeMatches(input.line(), input.column(), name.get(), arguments.get()));
			}
			return result;
		}
		final Parser runParser = input.keyword("Group");
		if (runParser.isGood()) {
			final AtomicReference<List<GroupNode>> groups = new AtomicReference<>();
			final AtomicReference<List<String>> discriminators = new AtomicReference<>();
			final Parser result = runParser//
					.whitespace()//
					.list(groups::set, GroupNode::parse, ',')//
					.whitespace()//
					.keyword("By")//
					.whitespace()//
					.list(discriminators::set, (p, o) -> p.whitespace().identifier(o).whitespace(), ',')//
					.whitespace();
			if (result.isGood()) {
				output.accept(
						new OliveClauseNodeGroup(input.line(), input.column(), groups.get(), discriminators.get()));
			}
			return result;
		}
		final Parser monitorParser = input.keyword("Monitor");
		if (monitorParser.isGood()) {
			final AtomicReference<String> metricName = new AtomicReference<>();
			final AtomicReference<String> help = new AtomicReference<>();
			final AtomicReference<List<MonitorArgumentNode>> labels = new AtomicReference<>();

			final Parser result = monitorParser//
					.whitespace()//
					.identifier(metricName::set)//
					.whitespace()//
					.regex(HELP, m -> help.set(m.group(1)), "Failed to parse help text")//
					.whitespace()//
					.symbol("{")//
					.list(labels::set, MonitorArgumentNode::parse, ',')//
					.symbol("}")//
					.whitespace();

			if (result.isGood()) {
				output.accept(new OliveClauseNodeMonitor(input.line(), input.column(), metricName.get(), help.get(),
						labels.get()));
			}
			return result;
		}
		return input.raise("Expected olive clause.");

	}

	/**
	 * Check whether the variable stream is acceptable to the clause
	 *
	 * @param state
	 *            the current variable state
	 */
	public abstract ClauseStreamOrder ensureRoot(ClauseStreamOrder state, Consumer<String> errorHandler);

	/**
	 * Generate byte code for this clause.
	 *
	 * This will consume a stream off the stack, manipulate it as necessary, and
	 * leave a new stream on the stack. Any required other classes or methods must
	 * be generated by the clause.
	 */
	public abstract void render(RootBuilder builder, BaseOliveBuilder oliveBuilder,
			Map<String, OliveDefineBuilder> definitions);

	/**
	 * Resolve all variable definitions in this clause
	 *
	 * @param defs
	 *            the variable definitions available to this clause
	 * @return the variable definitions available to the next clause
	 */
	public abstract NameDefinitions resolve(NameDefinitions defs, Consumer<String> errorHandler);

	/**
	 * Resolve all non-variable definitions
	 */
	public abstract boolean resolveDefinitions(Map<String, OliveNodeDefinition> definedOlives,
			Function<String, Lookup> definedLookups, Function<String, ActionDefinition> definedActions,
			Set<String> metricNames, Consumer<String> errorHandler);

	/**
	 * Type any expression in the clause
	 */
	public abstract boolean typeCheck(Consumer<String> errorHandler);

}