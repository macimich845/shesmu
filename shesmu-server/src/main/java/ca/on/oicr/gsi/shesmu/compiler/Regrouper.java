package ca.on.oicr.gsi.shesmu.compiler;

import java.util.function.Consumer;

import org.objectweb.asm.Type;

import ca.on.oicr.gsi.shesmu.Imyhat;

public interface Regrouper {

	/**
	 * Add a new collection of values slurped during iteration
	 *
	 * @param valueType
	 *            the type of the values in the collection
	 * @param fieldName
	 *            the name of the variable for consumption by downstream uses
	 */
	void addCollected(Imyhat valueType, String fieldName, Consumer<Renderer> loader);

	/**
	 * Count the number of matching rows.
	 *
	 * @param fieldName
	 *            the name of the variable for consumption by downstream uses
	 */
	void addCount(String fieldName);

	/**
	 * A single value to collected by a matching row
	 *
	 * @param fieldType
	 *            the type of the value being added
	 * @param fieldName
	 *            the name of the variable for consumption by downstream uses
	 */
	void addFirst(Type fieldType, String fieldName, Consumer<Renderer> loader);

	/**
	 * A single value which is the optima from all input values
	 *
	 * @param fieldType
	 *            the type of the value being added
	 * @param fieldName
	 *            the name of the variable for consumption by downstream uses
	 */
	void addOptima(Type fieldType, String fieldName, boolean max, Consumer<Renderer> loader);

	/**
	 * Count whether a variable matches a condition
	 *
	 * @param fieldName
	 *            the name of the variable for consumption by downstream uses
	 * @param condition
	 *            the condition that must be satisfied
	 */
	void addPartitionCount(String fieldName, Consumer<Renderer> condition);

	/**
	 * Conditionally add a variable
	 *
	 * @param condition
	 *            the condition that must be satisfied
	 */
	Regrouper addWhere(Consumer<Renderer> condition);
}