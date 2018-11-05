package net.miscfolder.bojiti.support;

import java.io.IOException;
import java.io.Reader;
import java.nio.BufferUnderflowException;
import java.nio.CharBuffer;

public class CharBufferReader extends Reader{
	private final CharBuffer buffer;
	private volatile boolean closed;

	public CharBufferReader(CharBuffer buffer){
		this.buffer = buffer;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException{
		if(closed) return -1;
		CharBuffer temp = CharBuffer.allocate(len);
		int read = this.buffer.read(temp);
		if(read != -1) System.arraycopy(temp.array(), 0, cbuf, off, read);
		return read;
	}

	@Override
	public int read(CharBuffer target) throws IOException{
		return buffer.read(target);
	}

	@Override
	public int read(){
		try{
			return buffer.get();
		}catch(BufferUnderflowException e){
			return -1;
		}
	}

	@Override
	public void mark(int readAheadLimit){
		buffer.mark();
	}

	@Override
	public void reset(){
		buffer.reset();
	}

	@Override
	public boolean markSupported(){
		return true;
	}

	@Override
	public void close(){
		closed = true;
	}
}
