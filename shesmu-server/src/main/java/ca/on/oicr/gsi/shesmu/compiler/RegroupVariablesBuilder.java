package ca.on.oicr.gsi.shesmu.compiler;

import static org.objectweb.asm.Type.BOOLEAN_TYPE;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.LONG_TYPE;
import static org.objectweb.asm.Type.VOID_TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import ca.on.oicr.gsi.shesmu.Imyhat;
import ca.on.oicr.gsi.shesmu.PartitionCount;
import ca.on.oicr.gsi.shesmu.Tuple;

/**
 * Helps to build a “Group” clause and the corresponding variable class
 */
public final class RegroupVariablesBuilder implements Regrouper {
	private class Collected extends Element {
		private final String fieldName;
		private final Consumer<Renderer> loader;
		private final Imyhat valueType;

		public Collected(Imyhat valueType, String fieldName, Consumer<Renderer> loader) {
			super();
			this.valueType = valueType;
			this.fieldName = fieldName;
			this.loader = loader;
			buildGetter(A_SET_TYPE, fieldName);
		}

		@Override
		public void buildCollect() {
			collectRenderer.methodGen().loadArg(collectedSelfArgument);
			collectRenderer.methodGen().invokeVirtual(self, new Method(fieldName, A_SET_TYPE, new Type[] {}));
			loader.accept(collectRenderer);
			collectRenderer.methodGen().box(valueType.asmType());
			collectRenderer.methodGen().invokeInterface(A_SET_TYPE, METHOD_SET__ADD);
			collectRenderer.methodGen().pop();
		}

		@Override
		public int buildConstructor(GeneratorAdapter ctor, int index) {
			ctor.loadThis();
			Renderer.loadImyhatInMethod(ctor, valueType.signature());
			ctor.invokeVirtual(A_IMYHAT_TYPE, METHOD_IMYHAT__NEW_SET);
			ctor.putField(self, fieldName, A_SET_TYPE);
			return index;
		}

		@Override
		public void buildEquals(GeneratorAdapter methodGen, int otherLocal, Label end) {
			// Collections are not included in equality.
		}

		@Override
		public void buildHashCode(GeneratorAdapter hashMethod) {
			// Collections are not included in the hash.
		}

		@Override
		public Type constructorType() {
			return null;
		}

		@Override
		public void failIfBad(GeneratorAdapter okMethod) {
			// Do nothing
		}

		@Override
		public void loadConstructorArgument() {
			// No argument to constructor.
		}
	}

	private class Conditional extends Element implements Regrouper {

		private final Consumer<Renderer> condition;
		private final List<Element> elements = new ArrayList<>();

		public Conditional(Consumer<Renderer> condition) {
			this.condition = condition;
		}

		@Override
		public void addCollected(Imyhat valueType, String fieldName, Consumer<Renderer> loader) {
			elements.add(new Collected(valueType, fieldName, loader));
		}

		@Override
		public void addCount(String fieldName) {
			elements.add(new Count(fieldName));
		}

		@Override
		public void addFirst(Type fieldType, String fieldName, Consumer<Renderer> loader) {
			elements.add(new First(fieldType, fieldName, loader));
		}

		@Override
		public void addOptima(Type fieldType, String fieldName, boolean max, Consumer<Renderer> loader) {
			elements.add(new Optima(fieldType, fieldName, max, loader));

		}

		@Override
		public void addPartitionCount(String fieldName, Consumer<Renderer> condition) {
			elements.add(new PartitionCounter(fieldName, condition));
		}

		@Override
		public Regrouper addWhere(Consumer<Renderer> condition) {
			final Conditional c = new Conditional(condition);
			elements.add(c);
			return c;
		}

		@Override
		public void buildCollect() {
			final Label skip = collectRenderer.methodGen().newLabel();
			condition.accept(collectRenderer);
			collectRenderer.methodGen().ifZCmp(GeneratorAdapter.EQ, skip);
			elements.forEach(Element::buildCollect);
			collectRenderer.methodGen().mark(skip);
		}

		@Override
		public int buildConstructor(GeneratorAdapter ctor, int index) {
			return elements.stream().reduce(index, (i, element) -> element.buildConstructor(ctor, i), (a, b) -> {
				throw new UnsupportedOperationException();
			});
		}

		@Override
		public void buildEquals(GeneratorAdapter methodGen, int otherLocal, Label end) {
			elements.forEach(element -> element.buildEquals(methodGen, otherLocal, end));
		}

		@Override
		public void buildHashCode(GeneratorAdapter method) {
			elements.forEach(element -> element.buildHashCode(method));
		}

		@Override
		public Type constructorType() {
			return null;
		}

		@Override
		public void failIfBad(GeneratorAdapter okMethod) {
			elements.forEach(element -> element.failIfBad(okMethod));
		}

		@Override
		public void loadConstructorArgument() {
			// No argument to constructor.
		}

	}

	private class Count extends Element {

		private final String fieldName;

		public Count(String fieldName) {
			this.fieldName = fieldName;
			buildGetter(LONG_TYPE, fieldName);
		}

		@Override
		public void buildCollect() {
			collectRenderer.methodGen().loadArg(collectedSelfArgument);
			collectRenderer.methodGen().dup();
			collectRenderer.methodGen().getField(self, fieldName, LONG_TYPE);
			collectRenderer.methodGen().push(1L);
			collectRenderer.methodGen().math(GeneratorAdapter.ADD, LONG_TYPE);
			collectRenderer.methodGen().putField(self, fieldName, LONG_TYPE);
		}

		@Override
		public int buildConstructor(GeneratorAdapter ctor, int index) {
			return index;
		}

		@Override
		public void buildEquals(GeneratorAdapter methodGen, int otherLocal, Label end) {
			// Counts are not included in equality.
		}

		@Override
		public void buildHashCode(GeneratorAdapter method) {
			// Counts are not included in hash code.
		}

		@Override
		public Type constructorType() {
			return null;
		}

		@Override
		public void failIfBad(GeneratorAdapter okMethod) {
			// Counts are always okay.
		}

		@Override
		public void loadConstructorArgument() {
			// No argument to constructor.
		}

	}

	private class Discriminator extends Element {
		private final String fieldName;
		private final Type fieldType;
		private final Consumer<Renderer> loader;

		public Discriminator(Type fieldType, String fieldName, Consumer<Renderer> loader) {
			super();
			this.fieldType = fieldType;
			this.fieldName = fieldName;
			this.loader = loader;
			buildGetter(fieldType, fieldName);
		}

		@Override
		public void buildCollect() {
			// No collection required
		}

		@Override
		public int buildConstructor(GeneratorAdapter ctor, int index) {
			ctor.loadThis();
			ctor.loadArg(index);
			ctor.putField(self, fieldName, fieldType);
			return index + 1;
		}

		@Override
		public void buildEquals(GeneratorAdapter method, int otherLocal, Label end) {
			method.loadThis();
			method.getField(self, fieldName, fieldType);
			method.loadLocal(otherLocal);
			method.getField(self, fieldName, fieldType);
			switch (fieldType.getSort()) {
			case Type.ARRAY:
			case Type.OBJECT:
				method.invokeVirtual(A_OBJECT_TYPE, METHOD_EQUALS);
				method.ifZCmp(GeneratorAdapter.EQ, end);
				break;
			default:
				method.ifCmp(fieldType, GeneratorAdapter.NE, end);
			}
		}

		@Override
		public void buildHashCode(GeneratorAdapter method) {
			method.push(31);
			method.math(GeneratorAdapter.MUL, INT_TYPE);
			method.loadThis();
			method.getField(self, fieldName, fieldType);
			switch (fieldType.getSort()) {
			case Type.ARRAY:
			case Type.OBJECT:
				method.invokeVirtual(A_OBJECT_TYPE, METHOD_HASH_CODE);
				break;
			default:
				method.cast(fieldType, INT_TYPE);
				break;
			}
			method.math(GeneratorAdapter.ADD, INT_TYPE);

		}

		@Override
		public Type constructorType() {
			return fieldType;
		}

		@Override
		public void failIfBad(GeneratorAdapter okMethod) {
			// Do nothing
		}

		@Override
		public void loadConstructorArgument() {
			loader.accept(newRenderer);
		}

	}

	private abstract class Element {

		public abstract void buildCollect();

		public abstract int buildConstructor(GeneratorAdapter ctor, int index);

		public abstract void buildEquals(GeneratorAdapter methodGen, int otherLocal, Label end);

		public abstract void buildHashCode(GeneratorAdapter method);

		public abstract Type constructorType();

		public abstract void failIfBad(GeneratorAdapter okMethod);

		public abstract void loadConstructorArgument();
	}

	private class First extends Element {

		private final String fieldName;
		private final Type fieldType;
		private final Consumer<Renderer> loader;

		public First(Type fieldType, String fieldName, Consumer<Renderer> loader) {
			this.fieldType = fieldType;
			this.fieldName = fieldName;
			this.loader = loader;
			classVisitor.visitField(Opcodes.ACC_PUBLIC, fieldName + "$ok", BOOLEAN_TYPE.getDescriptor(), null, null)
					.visitEnd();
			buildGetter(fieldType, fieldName);
		}

		@Override
		public void buildCollect() {
			collectRenderer.methodGen().loadArg(collectedSelfArgument);
			collectRenderer.methodGen().getField(self, fieldName + "$ok", BOOLEAN_TYPE);
			final Label skip = collectRenderer.methodGen().newLabel();
			collectRenderer.methodGen().ifZCmp(GeneratorAdapter.NE, skip);

			collectRenderer.methodGen().loadArg(collectedSelfArgument);
			collectRenderer.methodGen().push(true);
			collectRenderer.methodGen().putField(self, fieldName + "$ok", BOOLEAN_TYPE);
			collectRenderer.methodGen().loadArg(collectedSelfArgument);
			loader.accept(collectRenderer);
			collectRenderer.methodGen().putField(self, fieldName, fieldType);
			collectRenderer.methodGen().mark(skip);
		}

		@Override
		public int buildConstructor(GeneratorAdapter ctor, int index) {
			return index;
		}

		@Override
		public void buildEquals(GeneratorAdapter methodGen, int otherLocal, Label end) {
			// Firsts are not included in equality.
		}

		@Override
		public void buildHashCode(GeneratorAdapter method) {
			// Firsts are not included in hash code.
		}

		@Override
		public Type constructorType() {
			return null;
		}

		@Override
		public void failIfBad(GeneratorAdapter okMethod) {
			okMethod.loadThis();
			okMethod.getField(self, fieldName + "$ok", BOOLEAN_TYPE);
			final Label next = okMethod.newLabel();
			okMethod.ifZCmp(GeneratorAdapter.NE, next);
			okMethod.push(false);
			okMethod.returnValue();
			okMethod.mark(next);
		}

		@Override
		public void loadConstructorArgument() {
			// No argument to constructor.
		}

	}

	private class Optima extends Element {

		private final Comparison comparison;
		private final String fieldName;
		private final Type fieldType;
		private final Consumer<Renderer> loader;

		public Optima(Type fieldType, String fieldName, boolean max, Consumer<Renderer> loader) {
			this.fieldType = fieldType;
			this.fieldName = fieldName;
			comparison = max ? Comparison.GT : Comparison.LT;
			this.loader = loader;
			buildGetter(fieldType, fieldName);
			classVisitor.visitField(Opcodes.ACC_PUBLIC, fieldName + "$first", BOOLEAN_TYPE.getDescriptor(), null, null)
					.visitEnd();
			classVisitor.visitField(Opcodes.ACC_PUBLIC, fieldName + "$ok", BOOLEAN_TYPE.getDescriptor(), null, null)
					.visitEnd();

		}

		@Override
		public void buildCollect() {
			final int local = collectRenderer.methodGen().newLocal(fieldType);
			loader.accept(collectRenderer);
			collectRenderer.methodGen().storeLocal(local);

			collectRenderer.methodGen().loadArg(collectedSelfArgument);
			collectRenderer.methodGen().getField(self, fieldName + "$first", BOOLEAN_TYPE);
			final Label store = collectRenderer.methodGen().newLabel();
			final Label end = collectRenderer.methodGen().newLabel();
			collectRenderer.methodGen().ifZCmp(GeneratorAdapter.NE, store);

			collectRenderer.methodGen().loadArg(collectedSelfArgument);
			collectRenderer.methodGen().push(false);
			collectRenderer.methodGen().putField(self, fieldName + "$first", BOOLEAN_TYPE);

			collectRenderer.methodGen().loadArg(collectedSelfArgument);
			collectRenderer.methodGen().getField(self, fieldName, fieldType);
			collectRenderer.methodGen().loadLocal(local);

			if (fieldType.equals(Type.LONG_TYPE)) {
				comparison.branchInt(end, collectRenderer.methodGen());
			} else {
				comparison.branchDate(end, collectRenderer.methodGen());
			}

			collectRenderer.methodGen().mark(store);
			collectRenderer.methodGen().loadArg(collectedSelfArgument);
			collectRenderer.methodGen().loadLocal(local);
			collectRenderer.methodGen().putField(self, fieldName, fieldType);
			collectRenderer.methodGen().loadArg(collectedSelfArgument);
			collectRenderer.methodGen().push(true);
			collectRenderer.methodGen().putField(self, fieldName + "$ok", BOOLEAN_TYPE);
			collectRenderer.methodGen().mark(end);
		}

		@Override
		public int buildConstructor(GeneratorAdapter ctor, int index) {
			return index;
		}

		@Override
		public void buildEquals(GeneratorAdapter methodGen, int otherLocal, Label end) {
			// Optima are not included in equality.
		}

		@Override
		public void buildHashCode(GeneratorAdapter method) {
			// Optima are not included in hash code.
		}

		@Override
		public Type constructorType() {
			return null;
		}

		@Override
		public void failIfBad(GeneratorAdapter okMethod) {
			okMethod.loadThis();
			okMethod.getField(self, fieldName + "$ok", BOOLEAN_TYPE);
			final Label next = okMethod.newLabel();
			okMethod.ifZCmp(GeneratorAdapter.NE, next);
			okMethod.push(false);
			okMethod.returnValue();
			okMethod.mark(next);
		}

		@Override
		public void loadConstructorArgument() {
			// No argument to constructor.
		}

	}

	private class PartitionCounter extends Element {
		private final Consumer<Renderer> condition;
		private final String fieldName;

		public PartitionCounter(String fieldName, Consumer<Renderer> condition) {
			this.fieldName = fieldName;
			this.condition = condition;
			classVisitor.visitField(Opcodes.ACC_PUBLIC, fieldName, A_PARTITION_COUNT_TYPE.getDescriptor(), null, null)
					.visitEnd();
			final GeneratorAdapter getMethod = new GeneratorAdapter(Opcodes.ACC_PUBLIC,
					new Method(fieldName, A_TUPLE_TYPE, new Type[] {}), null, null, classVisitor);
			getMethod.visitCode();
			getMethod.loadThis();
			getMethod.getField(self, fieldName, A_PARTITION_COUNT_TYPE);
			getMethod.invokeVirtual(A_PARTITION_COUNT_TYPE, METHOD_PARTITION_COUNT__TO_TUPLE);
			getMethod.returnValue();
			getMethod.visitMaxs(0, 0);
			getMethod.visitEnd();
		}

		@Override
		public void buildCollect() {
			collectRenderer.methodGen().loadArg(collectedSelfArgument);
			collectRenderer.methodGen().getField(self, fieldName, A_PARTITION_COUNT_TYPE);
			condition.accept(collectRenderer);
			collectRenderer.methodGen().invokeVirtual(A_PARTITION_COUNT_TYPE, METHOD_PARTITION_COUNT__ACCUMULATE);
		}

		@Override
		public int buildConstructor(GeneratorAdapter ctor, int index) {
			ctor.loadThis();
			ctor.newInstance(A_PARTITION_COUNT_TYPE);
			ctor.dup();
			ctor.invokeConstructor(A_PARTITION_COUNT_TYPE, METHOD_DEFAULT_CTOR);
			ctor.putField(self, fieldName, A_PARTITION_COUNT_TYPE);
			return index;
		}

		@Override
		public void buildEquals(GeneratorAdapter methodGen, int otherLocal, Label end) {
			// Partition counters are not included in equality.
		}

		@Override
		public void buildHashCode(GeneratorAdapter hashMethod) {
			// Partition counters are not included in the hash.
		}

		@Override
		public Type constructorType() {
			return null;
		}

		@Override
		public void failIfBad(GeneratorAdapter okMethod) {
			// Do nothing
		}

		@Override
		public void loadConstructorArgument() {
			// No argument to constructor.
		}
	}

	private static final Type A_IMYHAT_TYPE = Type.getType(Imyhat.class);

	private static final Type A_OBJECT_TYPE = Type.getType(Object.class);

	private static final Type A_PARTITION_COUNT_TYPE = Type.getType(PartitionCount.class);

	private static final Type A_SET_TYPE = Type.getType(Set.class);

	private static final Type A_TUPLE_TYPE = Type.getType(Tuple.class);

	private static final Method CTOR_DEFAULT = new Method("<init>", VOID_TYPE, new Type[] {});
	private static final Method METHOD_DEFAULT_CTOR = new Method("<init>", VOID_TYPE, new Type[] {});
	private static final Method METHOD_EQUALS = new Method("equals", BOOLEAN_TYPE, new Type[] { A_OBJECT_TYPE });

	private static final Method METHOD_HASH_CODE = new Method("hashCode", INT_TYPE, new Type[] {});

	private static final Method METHOD_IMYHAT__NEW_SET = new Method("newSet", A_SET_TYPE, new Type[] {});
	private static final Method METHOD_IS_OK = new Method("$isOk", BOOLEAN_TYPE, new Type[] {});
	private static final Method METHOD_PARTITION_COUNT__ACCUMULATE = new Method("accumulate", VOID_TYPE,
			new Type[] { BOOLEAN_TYPE });
	private static final Method METHOD_PARTITION_COUNT__TO_TUPLE = new Method("toTuple", A_TUPLE_TYPE, new Type[] {});
	private static final Method METHOD_SET__ADD = new Method("add", BOOLEAN_TYPE, new Type[] { A_OBJECT_TYPE });
	private final ClassVisitor classVisitor;
	public final int collectedSelfArgument;

	private final Renderer collectRenderer;

	private final List<Element> elements = new ArrayList<>();

	private final Renderer newRenderer;

	private final Type self;

	public RegroupVariablesBuilder(RootBuilder builder, String name, Renderer newMethodGen, Renderer collectedMethodGen,
			int collectedSelfArgument) {
		newRenderer = newMethodGen;
		collectRenderer = collectedMethodGen;
		this.collectedSelfArgument = collectedSelfArgument;
		self = Type.getObjectType(name);
		classVisitor = builder.createClassVisitor();
		classVisitor.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, A_OBJECT_TYPE.getInternalName(), null);
		classVisitor.visitSource(builder.sourcePath(), null);
	}

	@Override
	public void addCollected(Imyhat valueType, String fieldName, Consumer<Renderer> loader) {
		elements.add(new Collected(valueType, fieldName, loader));
	}

	@Override
	public void addCount(String fieldName) {
		elements.add(new Count(fieldName));
	}

	@Override
	public void addFirst(Type fieldType, String fieldName, Consumer<Renderer> loader) {
		elements.add(new First(fieldType, fieldName, loader));
	}

	public void addKey(Type fieldType, String fieldName, Consumer<Renderer> loader) {
		elements.add(new Discriminator(fieldType, fieldName, loader));
	}

	@Override
	public void addOptima(Type fieldType, String fieldName, boolean max, Consumer<Renderer> loader) {
		elements.add(new Optima(fieldType, fieldName, max, loader));

	}

	@Override
	public void addPartitionCount(String fieldName, Consumer<Renderer> condition) {
		elements.add(new PartitionCounter(fieldName, condition));
	}

	@Override
	public Regrouper addWhere(Consumer<Renderer> condition) {
		final Conditional c = new Conditional(condition);
		elements.add(c);
		return c;
	}

	private void buildGetter(Type fieldType, String fieldName) {
		classVisitor.visitField(Opcodes.ACC_PUBLIC, fieldName, fieldType.getDescriptor(), null, null).visitEnd();
		final GeneratorAdapter getMethod = new GeneratorAdapter(Opcodes.ACC_PUBLIC,
				new Method(fieldName, fieldType, new Type[] {}), null, null, classVisitor);
		getMethod.visitCode();
		getMethod.loadThis();
		getMethod.getField(self, fieldName, fieldType);
		getMethod.returnValue();
		getMethod.visitMaxs(0, 0);
		getMethod.visitEnd();
	}

	/**
	 * Generate the class completely
	 */
	public void finish() {
		final Method ctorType = new Method("<init>", VOID_TYPE,
				elements.stream().map(Element::constructorType).filter(Objects::nonNull).toArray(Type[]::new));
		final GeneratorAdapter ctor = new GeneratorAdapter(Opcodes.ACC_PUBLIC, ctorType, null, null, classVisitor);
		ctor.visitCode();
		ctor.loadThis();
		ctor.invokeConstructor(A_OBJECT_TYPE, CTOR_DEFAULT);
		elements.stream().reduce(0, (index, element) -> element.buildConstructor(ctor, index), (a, b) -> {
			throw new UnsupportedOperationException();
		});
		ctor.visitInsn(Opcodes.RETURN);
		ctor.visitMaxs(0, 0);
		ctor.visitEnd();

		final GeneratorAdapter hashMethod = new GeneratorAdapter(Opcodes.ACC_PUBLIC, METHOD_HASH_CODE, null, null,
				classVisitor);
		hashMethod.visitCode();
		hashMethod.push(0);
		elements.forEach(element -> element.buildHashCode(hashMethod));

		hashMethod.returnValue();
		hashMethod.visitMaxs(0, 0);
		hashMethod.visitEnd();

		final GeneratorAdapter equalsMethod = new GeneratorAdapter(Opcodes.ACC_PUBLIC, METHOD_EQUALS, null, null,
				classVisitor);
		equalsMethod.visitCode();
		equalsMethod.loadArg(0);
		equalsMethod.instanceOf(self);
		final Label equalsFalse = equalsMethod.newLabel();
		equalsMethod.ifZCmp(GeneratorAdapter.EQ, equalsFalse);
		final int equalsOtherLocal = equalsMethod.newLocal(self);
		equalsMethod.loadArg(0);
		equalsMethod.checkCast(self);
		equalsMethod.storeLocal(equalsOtherLocal);
		elements.forEach(element -> element.buildEquals(equalsMethod, equalsOtherLocal, equalsFalse));
		equalsMethod.push(true);
		equalsMethod.returnValue();
		equalsMethod.mark(equalsFalse);
		equalsMethod.push(false);
		equalsMethod.returnValue();
		equalsMethod.visitMaxs(0, 0);
		equalsMethod.visitEnd();

		newRenderer.methodGen().visitCode();
		newRenderer.methodGen().newInstance(self);
		newRenderer.methodGen().dup();
		elements.forEach(Element::loadConstructorArgument);
		newRenderer.methodGen().invokeConstructor(self, ctorType);
		newRenderer.methodGen().returnValue();
		newRenderer.methodGen().visitMaxs(0, 0);
		newRenderer.methodGen().visitEnd();

		collectRenderer.methodGen().visitCode();
		elements.forEach(Element::buildCollect);
		collectRenderer.methodGen().visitInsn(Opcodes.RETURN);
		collectRenderer.methodGen().visitMaxs(0, 0);
		collectRenderer.methodGen().visitEnd();

		final GeneratorAdapter okMethod = new GeneratorAdapter(Opcodes.ACC_PUBLIC, METHOD_IS_OK, null, null,
				classVisitor);
		okMethod.visitCode();
		elements.forEach(element -> element.failIfBad(okMethod));
		okMethod.push(true);
		okMethod.returnValue();
		okMethod.visitMaxs(0, 0);
		okMethod.visitEnd();

		classVisitor.visitEnd();
	}

}
