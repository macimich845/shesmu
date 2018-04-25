package ca.on.oicr.gsi.shesmu;

import java.util.stream.Stream;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

/**
 * A multi-keyed map that lookups a value based on rules/tables
 */
public interface LookupDefinition {

	public static LookupDefinition staticMethod(Class<?> owner, String methodName, Imyhat returnType,
			Imyhat... argumentTypes) {
		return new LookupDefinition() {

			@Override
			public String name() {
				return methodName;
			}

			@Override
			public void render(GeneratorAdapter methodGen) {
				methodGen.invokeStatic(Type.getType(owner), new Method(methodName, returnType.asmType(),
						Stream.of(argumentTypes).map(Imyhat::asmType).toArray(Type[]::new)));
			}

			@Override
			public Imyhat returnType() {
				return returnType;
			}

			@Override
			public Stream<Imyhat> types() {
				return Stream.of(argumentTypes);
			}
		};
	}

	/**
	 * The name of the lookup.
	 */
	String name();

	/**
	 * Create bytecode for this lookup.
	 */
	void render(GeneratorAdapter methodGen);

	/**
	 * The return type of the map
	 */
	Imyhat returnType();

	/**
	 * The types of the parameters
	 */
	Stream<Imyhat> types();
}