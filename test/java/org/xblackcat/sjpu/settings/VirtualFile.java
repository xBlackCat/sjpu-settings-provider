package org.xblackcat.sjpu.settings;

import java.io.*;

/**
 * 27.06.2016 13:04
 *
 * @author xBlackCat
 */
public class VirtualFile extends PrintStream {
    public VirtualFile() {
        super(new ByteArrayOutputStream());
    }

    public InputStream getAsInputStream() {
        return new ByteArrayInputStream(((ByteArrayOutputStream) out).toByteArray());
    }

    public void print(PrintStream stream) throws IOException {
        stream.write(((ByteArrayOutputStream) out).toByteArray());
    }
}
