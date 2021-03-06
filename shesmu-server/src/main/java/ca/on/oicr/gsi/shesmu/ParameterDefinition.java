package ca.on.oicr.gsi.shesmu;

import java.util.function.Consumer;

import org.objectweb.asm.Type;

import ca.on.oicr.gsi.shesmu.Imyhat.BaseImyhat;
import ca.on.oicr.gsi.shesmu.compiler.Renderer;

/**
 * A definition for a parameter that should be user-definable for an action
 */
public interface ParameterDefinition {
	/**
	 * Create a parameter definition that will be written to a public field
	 *
	 * @param owner
	 *            The type of the action object
	 * @param fieldName
	 *            the name of the field
	 * @param fieldType
	 *            type of the field
	 * @param required
	 *            whether this field must be set in the script
	 */
	public static ParameterDefinition forField(Type owner, String fieldName, BaseImyhat fieldType, boolean required) {
		return new ParameterDefinition() {

			@Override
			public String name() {
				return fieldName;
			}

			@Override
			public boolean required() {
				return required;
			}

			@Override
			public void store(Renderer renderer, int actionLocal, Consumer<Renderer> loadParameter) {
				renderer.methodGen().loadLocal(actionLocal);
				loadParameter.accept(renderer);
				renderer.methodGen().putField(owner, fieldName, fieldType.asmType());
			}

			@Override
			public Imyhat type() {
				return fieldType;
			}

		};
	}

	/**
	 * The name of the parameter as the user will set it.
	 */
	String name();

	/**
	 * Whether this parameter is required or not.
	 *
	 * If not required, the user may omit setting the value.s
	 */
	boolean required();

	/**
	 * A procedure to write the bytecode to set the parameter in the action instance
	 *
	 * @param renderer
	 *            The method where the code is being generated
	 * @param actionLocal
	 *            The local variable holding the action being populated
	 * @param loadParameter
	 *            a callback to load the desired value for the parameter; it should
	 *            be called exactly once.
	 */
	void store(Renderer renderer, int actionLocal, Consumer<Renderer> loadParameter);

	/**
	 * The type of the parameter
	 */
	Imyhat type();

}
