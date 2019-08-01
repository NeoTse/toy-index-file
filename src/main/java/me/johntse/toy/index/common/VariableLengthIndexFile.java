package me.johntse.toy.index.common;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 姓名可索引文件。
 *
 * @author John Tse
 */
public class VariableLengthIndexFile extends AbstractIndexFile {
    private static final String INDEX_COUNT = "INDEX_COUNT";

    private List<Integer> lens = new ArrayList<>();

    /**
     * 需要构建索引的字段.
     */
    private int idx;

    /**
     * 单个索引长度，即限制了能够表示的内容的长度. 单位：字节
     */
    private int indexUnitLength;

    public VariableLengthIndexFile() {
        super("utf8");
    }

    public VariableLengthIndexFile(String charset, int idx, int indexUnitLength) {
        super(charset);

        this.idx = idx;

        if (indexUnitLength < 0 || indexUnitLength > 4) {
            throw new IllegalArgumentException("unsupported unit length: " + indexUnitLength);
        }

        this.indexUnitLength = indexUnitLength;
    }

    @Override
    public File createImpl(File text) throws IOException {
        File output = getIndexFileName(text);
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(text), charset));
             DataOutputStream writer = new DataOutputStream(new FileOutputStream(output))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    String[] items = line.split("\t");
                    if (items.length > idx) {
                        counter.add(READ_COUNT, 1);

                        // 输出索引内容
                        byte[] bytes = items[idx].getBytes();
                        writer.write(bytes);
                        lens.add(bytes.length);

                        counter.add(WRITE_COUNT, 1);
                    }
                }
            }

            // 输出索引
            for (int len : lens) {
                switch (indexUnitLength) {
                    case 1:
                        writer.writeByte(len);
                        break;
                    case 2:
                        writer.writeShort(len);
                        break;
                    case 3:
                    case 4:
                        writer.writeInt(len);
                        break;
                    default:
                        throw new IllegalArgumentException("unsupported unit length: " + indexUnitLength);
                }
            }

            counter.add(INDEX_COUNT, lens.size());

            // 输出索引长度
            writer.writeInt(lens.size() * indexUnitLength);

            writer.flush();
        }

        System.out.println(counter.toString());

        return output;
    }

    @Override
    protected String getMetaInfo(DataInput input) throws IOException {
        return "";
    }

    @Override
    public byte[] readMeta() throws IOException {
        return null;
    }

    @Override
    public byte[] readIndex() throws IOException {
        long fileSize = reader.length();
        reader.seek(fileSize - 4);
        int len = reader.readInt();

        reader.seek(fileSize - len - 4);
        byte[] res = new byte[len];
        reader.read(res);

        return res;
    }

    @Override
    public File asText(File index) throws IOException {
        File output = getTextFileName(index);
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), charset))) {
            load(index);
            List<IndexObject> indexObjects = readVariableIndex();
            int size = indexObjects.size();
            int n = 0;
            for (int i = 0; i < size; ) {
                int delta = Math.min(1024, size - i);
                String[] content = getContent(indexObjects, i, i + delta);
                for (String s : content) {
                    writer.append(s).append('\t').append(String.valueOf(n)).append('\n');
                    ++n;
                }

                i += delta;
            }

            writer.flush();
        }

        return output;
    }

    public List<IndexObject> readVariableIndex() throws IOException {
        byte[] indexContent = readIndex();
        long offset = 0;
        List<IndexObject> results = new ArrayList<>(1024);
        for (int i = 0; i < indexContent.length; i++) {
            results.add(new IndexObject(i, indexContent[i], offset));
            offset += indexContent[i];
        }

        return results;
    }

    public String[] getContent(List<IndexObject> index, int start, int end) throws IOException {
        int size = index.size();
        if (start > size || end < 0 || start >= end) {
            throw new IllegalArgumentException("Illegal start or end index.");
        }

        int length;
        if (end >= index.size()) {
            end = index.size();
            length = index.get(end - 1).num + (int) (index.get(end - 1).offset - index.get(start).offset);
        } else {
            length = (int) (index.get(end).offset - index.get(start).offset);
        }

        String[] content = new String[end - start];
        byte[] bs = readContent(index.get(start).offset, length);
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bs));
        for (int i = start; i < end; i++) {
            int len = index.get(i).num;
            byte[] s = new byte[len];
            if (input.read(s) != len) {
                throw new RuntimeException("read error length, it should be " + len);
            }

            content[i - start] = new String(s, charset);
        }

        return content;
    }
}
