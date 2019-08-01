package me.johntse.toy.index.common;

import java.io.*;

/**
 * 抽象索引文件
 *
 * @author John Tse
 */
public abstract class AbstractIndexFile implements Indexable {
    protected static final String READ_COUNT = "READ_COUNT";
    protected static final String WRITE_COUNT = "WRITE_COUNT";

    protected String charset;
    protected RandomAccessFile reader;

    protected File index;

    protected Counter counter;

    private int metaSize;
    private long fileSize;

    public AbstractIndexFile(String charset) {
        this.charset = charset;
        this.counter = new Counter();
    }

    protected File getIndexFileName(File input) {
        String oldFileName = input.getName();
        String fileName = oldFileName.contains(".") ?
                oldFileName.substring(0, oldFileName.lastIndexOf(".")) : oldFileName;
        return new File(input.getParent(), fileName.concat(".idx"));
    }

    protected File getTextFileName(File index) {
        String oldFileName = index.getName();
        String fileName = oldFileName.contains(".") ?
                oldFileName.substring(0, oldFileName.lastIndexOf(".")) : oldFileName;
        return new File(index.getParent(), fileName.concat("_index.txt"));
    }

    @Override
    public byte[] readMeta() throws IOException {
        fileSize = reader.length();
        reader.seek(fileSize - 4);
        metaSize = reader.readInt();
        reader.seek(fileSize - metaSize - 4);

        byte[] res = new byte[metaSize];
        reader.read(res);

        // 回退位置，为下面的取索引做准备
        reader.seek(fileSize - metaSize - 8);
        return res;
    }

    @Override
    public byte[] readIndex() throws IOException {
        int len = reader.readInt();

        reader.seek(fileSize - metaSize - 8 - len);
        byte[] res = new byte[len];
        reader.read(res);

        return res;
    }

    @Override
    public byte[] readContent(long offset, int len) throws IOException {
        reader.seek(offset);
        byte[] res = new byte[len];
        reader.read(res);

        return res;
    }

    @Override
    public File create(File text) throws IOException {
        this.index = createImpl(text);
        this.reader = new RandomAccessFile(index, "r");

        return this.index;
    }

    @Override
    public void load(File index) throws IOException {
        this.index = index;
        this.reader = new RandomAccessFile(index, "r");
        byte[] meta = readMeta();
        if (meta != null) {
            System.out.println(getMetaInfo(new DataInputStream(new ByteArrayInputStream(meta))));
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    protected abstract File createImpl(File text) throws IOException;

    protected abstract String getMetaInfo(DataInput input) throws IOException;
}
