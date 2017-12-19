package ca.on.oicr.gsi.shesmu.compiler;

import java.util.Map;
import java.util.function.Consumer;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * Helper class to hold state and context for bytecode generation.
 */
public class Renderer {
	private final Map<String, Consumer<GeneratorAdapter>> loadables;
	private final GeneratorAdapter methodGen;
	private final RootBuilder rootBuilder;

	private final int streamArg;

	private final Type streamType;

	public Renderer(RootBuilder rootBuilder, GeneratorAdapter methodGen, int streamArg, Type streamType,
			Map<String, Consumer<GeneratorAdapter>> loadables) {
		this.rootBuilder = rootBuilder;
		this.methodGen = methodGen;
		this.streamArg = streamArg;
		this.streamType = streamType;
		this.loadables = loadables;

	}

	/**
	 * Find a known variable by name and load it on the stack.
	 *
	 * @param name
	 */
	public void emitNamed(String name) {
		loadables.get(name).accept(methodGen);
	}

	public void loadImyhat(String signature) {
		rootBuilder.loadImyhat(signature, methodGen);
	}

	/**
	 * Find a known lookup and load it on the stack.
	 */
	public final void loadLookup(String name, GeneratorAdapter methodGen) {
		rootBuilder.loadLookup(name, methodGen);
	}

	/**
	 * Load the current stream value on the stack
	 *
	 * This cannot be used in the contexts where the stream hasn't started.
	 */
	public void loadStream() {
		if (streamType == null) {
			throw new UnsupportedOperationException();
		}
		methodGen.loadArg(streamArg);
	}

	/**
	 * Write the line number into the debugger for future reference.
	 */
	public void mark(int line) {
		methodGen.visitLineNumber(line, methodGen.mark());
	}

	/**
	 * Get the method currently being written.
	 */
	public GeneratorAdapter methodGen() {
		return methodGen;
	}

	/**
	 * The the owner of this method
	 */
	public RootBuilder root() {
		return rootBuilder;
	}

	/**
	 * Get the type of the current stream variable
	 *
	 * This will vary if the stream has been grouped.
	 */
	public Type streamType() {
		return streamType;
	}

}