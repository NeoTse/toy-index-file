package me.johntse.toy.index.common;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * 可索引文件接口。
 *
 * @author John Tse
 */
public interface Indexable extends Closeable {
    File create(File text) throws IOException;

    void load(File index) throws IOException;

    byte[] readMeta() throws IOException;

    byte[] readIndex() throws IOException;

    byte[] readContent(long offset, int len) throws IOException;

    File asText(File index) throws IOException;
}
