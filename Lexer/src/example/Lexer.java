package example;

import java.io.File;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class Lexer {
	private @Unique Buffer buf;
	
	@Unique("return" /* is CONSISTENT */)
	public Lexer(@Unique File f) {
		buf = new Buffer(f);
	}
	
	@Borrowed("this" /* is CONSISTENT */)
	public boolean isDone() {
		return buf.atEOF();
	}
	
	@Unique("return" /* is CONSISTENT */)
	@Borrowed("this" /* is CONSISTENT */)
	public File replace(@Unique File n) {
		buf.sync();
		File old = null;
		try {
			old = buf.getFile();
		} finally {
			buf = null;
			buf = new Buffer(n);
		} 
		return old;
	}
}

class Buffer {
	private @Unique File file;
	
	@Unique("return" /* is CONSISTENT */)
	@RegionEffects("none")
	public Buffer(@Unique File f) {
		file = f;
	}
	
	@Borrowed("this" /* is CONSISTENT */)
	@RegionEffects("reads Instance")
	public boolean atEOF() {
		// bogus, just return something
		return false;
	}
	
	@Borrowed("this" /* is CONSISTENT */)
	@RegionEffects("writes Instance")
	public void sync() {
		// do nothing
	}
	
	@Unique("return, this" /* is CONSISTENT */)
	@RegionEffects("writes Instance" /* is CONSISTENT */)
	public File getFile() {
		final File old = file;
		file = null;
		return old;
	}
}
