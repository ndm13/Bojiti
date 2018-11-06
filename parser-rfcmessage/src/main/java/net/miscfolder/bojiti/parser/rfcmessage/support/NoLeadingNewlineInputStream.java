package net.miscfolder.bojiti.parser.rfcmessage.support;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class NoLeadingNewlineInputStream extends FilterInputStream{
	private volatile boolean startedContent = false;

	public NoLeadingNewlineInputStream(InputStream in){
		super(in);
	}

	@Override
	public int read() throws IOException{
		if(startedContent) return super.read();
		int read = super.read();
		while(read == '\n' || read == '\r')
			read = super.read();
		startedContent = true;
		return read;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException{
		if(startedContent) return super.read(b, off, len);
		// We only need to read the first byte before we can delegate it.
		int read = read();
		if(read == -1) return -1;
		b[off] = (byte)read;
		return super.read(b, off + 1, len - 1) + 1;
	}

	@Override
	public boolean markSupported(){
		return !startedContent;
	}

	@Override
	public int read(byte[] b) throws IOException{
		return startedContent ? super.read(b) : read(b, 0, b.length);
	}
}
