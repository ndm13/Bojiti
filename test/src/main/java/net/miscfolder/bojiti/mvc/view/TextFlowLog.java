package net.miscfolder.bojiti.mvc.view;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class TextFlowLog{
	private final TextFlow textFlow;
	private final ScrollPane scrollPane;

	public TextFlowLog(){
		this.textFlow = new TextFlow();
		this.scrollPane = new ScrollPane(textFlow);
		this.scrollPane.setVvalue(1);
		this.scrollPane.setVmax(1);
		this.scrollPane.vvalueProperty().bind(textFlow.heightProperty());
	}

	public TextFlow getTextFlow(){
		return textFlow;
	}

	public ScrollPane getScrollPane(){
		return scrollPane;
	}

	public PrintStream createPrintStream(Color color){
		return createPrintStream(color, true);
	}

	public PrintStream createPrintStream(Color color, boolean autoflush){
		return new PrintStream(new OutputStream(){
			private StringBuffer buffer = new StringBuffer();

			@Override
			public void write(int b){
				buffer.append((char) b);
			}

			@Override
			public void write(byte[] b){
				buffer.append(ByteBuffer.wrap(b).asCharBuffer());
			}

			@Override
			public void flush(){
				synchronized(scrollPane){
					if(buffer.length() == 0) return;
					Text text = new Text(buffer.toString());
					buffer.setLength(0);
					text.setFill(color);

					textFlow.getChildren().add(text);
				}
			}
		}, autoflush);
	}
}
