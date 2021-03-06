package ca.on.oicr.gsi.shesmu.compiler;

import java.util.function.Consumer;

public class ListNodeLimit extends ListNodeBaseRange {

	public ListNodeLimit(int line, int column, ExpressionNode expression) {
		super(line, column, expression);
	}

	@Override
	protected void render(JavaStreamBuilder builder, Consumer<Renderer> expression) {
		builder.limit(expression);
	}

}
