package me.johntse.toy.index.common;

import java.io.*;
import java.util.*;

/**
 * 二级定长索引文件
 *
 * @author John Tse
 */
public class SecondaryFixedLengthIndexFile extends AbstractIndexFile {
    private static final String FIRST_INDEX = "FIRST_INDEX";
    private static final String SECOND_INDEX = "SECOND_INDEX";
    private static final String MAX_CONTENT_SIZE = "MAX_CONTENT_SIZE";
    private static final String MIN_CONTENT_SIZE = "MIN_CONTENT_SIZE";
    private static final String AVG_CONTENT_SIZE = "AVG_CONTENT_SIZE";

    private Range first;
    private Range second;
    private Range third;

    private int idx = 0;
    private int fixedLength = 0;

    private List<IndexObject> firstIndexes;
    private List<IndexObject> secondIndexes;

    private Map<Integer, Integer> contentFieldSize;

    private IndexBuildStrategy<Integer> buildStrategy;

    public SecondaryFixedLengthIndexFile(int idx, Range first, Range second, Range third,
                                         String charset, Map<Integer, Integer> contentFieldSize) {
        super(charset);
        this.idx = idx;
        this.first = first;
        this.second = second;
        this.third = third;

        this.firstIndexes = new ArrayList<>();
        this.secondIndexes = new ArrayList<>();
        this.contentFieldSize = Collections.unmodifiableMap(contentFieldSize);

        Collection<Integer> lens = this.contentFieldSize.values();
        for (int len : lens) {
            this.fixedLength += len;
        }

        buildStrategy = new SecondaryFixedLengthIndexBuild(first, second, third);
        counter.add(MAX_CONTENT_SIZE, Integer.MIN_VALUE);
        counter.add(MIN_CONTENT_SIZE, Integer.MAX_VALUE);
    }

    public SecondaryFixedLengthIndexFile() {
        super("utf8");
    }

    public static int getRecordNumForOffset(long offset) {
        return (int) ((offset >> 40) & 0x0FFFFFF);
    }

    public static long getOffset(long offset) {
        return offset & 0x0FFFFFFFFFFL;
    }

    @Override
    public File createImpl(File text) throws IOException {
        File output = getIndexFileName(text);

        int sum = 0;
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(text), charset));
             DataOutputStream writer = new DataOutputStream(new FileOutputStream(output))) {
            String line;
            long offset = 0;
            int count = 0;
            int oldFirstIndex = Integer.MIN_VALUE;
            int oldSecondIndex = Integer.MIN_VALUE;

            int currentContentPrefix = Integer.MIN_VALUE;
            int oldContentPrefix = Integer.MIN_VALUE;
            List<CountItem> countItems = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    String[] items = line.split("\t");
                    if (items.length <= idx) {
                        throw new RuntimeException("Bad line! " + line);
                    }

                    // 增加输入个数
                    counter.increment(READ_COUNT, 1);
                    String[] indexes = items[idx].split(",");
                    int firstIndex = Integer.parseInt(indexes[0]);
                    int secondIndex = Integer.parseInt(indexes[1]);
                    items[idx] = indexes[2];

                    currentContentPrefix = Integer.parseInt(items[idx]);

                    if (oldFirstIndex == Integer.MIN_VALUE) {
                        oldFirstIndex = firstIndex;
                    } else if (oldFirstIndex != firstIndex) {
                        // 新增一级索引及其偏移量
                        firstIndexes.add(new IndexObject(oldFirstIndex, offset));

                        // 新增之前一级索引剩余的二级索引及其偏移量
                        secondIndexes.add(appendSearchInfo(new IndexObject(oldSecondIndex, count,
                                offset - count * getFixedLength()), countItems));

                        // 增加二级索引个数
                        counter.increment(SECOND_INDEX, 1);
                        counter.add(MAX_CONTENT_SIZE, Math.max(count, counter.get(MAX_CONTENT_SIZE)));
                        counter.add(MIN_CONTENT_SIZE, Math.min(count, counter.get(MIN_CONTENT_SIZE)));
                        sum += count;

                        // 输出二级索引
                        offset += writeSecondIndex(writer, secondIndexes);

                        count = 0;
                        oldFirstIndex = firstIndex;
                        oldSecondIndex = secondIndex;
                        countItems.clear();
                    }

                    if (oldSecondIndex == Integer.MIN_VALUE) {
                        oldSecondIndex = secondIndex;
                    } else if (oldSecondIndex != secondIndex) {
                        // 新增二级索引及其偏移量
                        secondIndexes.add(appendSearchInfo(new IndexObject(oldSecondIndex, count,
                                offset - count * getFixedLength()), countItems));
                        // 增加二级索引个数
                        counter.increment(SECOND_INDEX, 1);
                        counter.add(MAX_CONTENT_SIZE, Math.max(count, counter.get(MAX_CONTENT_SIZE)));
                        counter.add(MIN_CONTENT_SIZE, Math.min(count, counter.get(MIN_CONTENT_SIZE)));
                        sum += count;

                        count = 0;
                        oldSecondIndex = secondIndex;
                        countItems.clear();
                    }

                    if (oldContentPrefix != currentContentPrefix) {
                        countItems.add(new CountItem(currentContentPrefix, count));
                        oldContentPrefix = currentContentPrefix;
                    }

                    offset += writeContent(writer, items, idx);
                    ++count;

                    // 增加输出个数
                    counter.increment(WRITE_COUNT, 1);
                }
            }

            // 新增剩余的二级索引及其偏移量
            secondIndexes.add(appendSearchInfo(new IndexObject(oldSecondIndex, count,
                    offset - count * getFixedLength()), countItems));
            countItems.clear();

            // 增加二级索引个数
            counter.increment(SECOND_INDEX, 1);
            counter.add(MAX_CONTENT_SIZE, Math.max(count, counter.get(MAX_CONTENT_SIZE)));
            counter.add(MIN_CONTENT_SIZE, Math.min(count, counter.get(MIN_CONTENT_SIZE)));
            sum += count;

            // 输出最后一个二级索引
            writeSecondIndex(writer, secondIndexes);

            // 新增一级索引及其偏移量
            firstIndexes.add(new IndexObject(oldFirstIndex, offset));
            // 增加一级索引个数
            counter.increment(FIRST_INDEX, firstIndexes.size());

            // 输出一级索引
            writeFirstIndex(writer, firstIndexes);

            // 输出元数据信息
            appendMetaInfo(writer);

            writer.flush();
        }

        counter.add(AVG_CONTENT_SIZE, (int) (sum * 1.0 / counter.get(SECOND_INDEX)));

        System.out.println(counter.toString());

        return output;
    }

    private IndexObject appendSearchInfo(IndexObject secondIndex, List<CountItem> countItems) {
        if (countItems.isEmpty()) {
            return secondIndex;
        }

        int size = countItems.size();
        secondIndex.setMinKey(countItems.get(0).key);

        CountItem item = countItems.get(size / 2);
        secondIndex.setMidKey(item.key);
        secondIndex.setMidDelta(item.count);

        item = countItems.get(size - 1);
        secondIndex.setMaxKey(item.key);
        secondIndex.setEndDelta(item.count);

        return secondIndex;
    }

    @Override
    public void load(File index) throws IOException {
        super.load(index);

        Collection<Integer> lens = this.contentFieldSize.values();
        for (int len : lens) {
            this.fixedLength += len;
        }

        buildStrategy = new SecondaryFixedLengthIndexBuild(first, second, third);
    }

    @Override
    public File asText(File index) throws IOException {
        File output = getTextFileName(index);
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), charset))) {
            // 输出元数据信息
            load(index);
            // 获取一级索引
            byte[] firstIndexContent = readIndex();
            DataInputStream input1 = new DataInputStream(new ByteArrayInputStream(firstIndexContent));
            List<IndexObject> firstIndex = readFirstIndex(input1);
            for (IndexObject idx : firstIndex) {
                // 获取二级索引
                reader.seek(idx.offset);
                int secondIndexLen = reader.readInt();
                byte[] secondIndexContent = readContent(idx.offset + 4, secondIndexLen);
                DataInputStream input2 = new DataInputStream(new ByteArrayInputStream(secondIndexContent));

                List<IndexObject> secondIndex = readSecondIndex(input2);
                for (IndexObject current : secondIndex) {
                    reader.seek(current.offset);
                    Iterator<List<String>> iterator = new DefaultItr(reader, current.num, this);
                    while (iterator.hasNext()) {
                        List<String> res = iterator.next();

                        writer.append(buildStrategy.restore(Arrays.asList(idx.key, current.key,
                                Integer.parseInt(res.get(0))))).append("\t");
                        for (int i = 1; i < res.size(); i++) {
                            writer.append(res.get(i));

                            if (i != res.size() - 1) {
                                writer.append("\t");
                            }
                        }

                        writer.append("\n");
                    }
                }
            }

            writer.flush();
        }

        return output;
    }

    public List<IndexObject> readFirstIndex(DataInputStream input) throws IOException {
        int num = input.readShort();
        List<IndexObject> res = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            // FIXME first index value may be greater than the max of integer
            res.add(new IndexObject((int) readByLength(input, first.size), input.readLong()));
        }

        return res;
    }

    public List<IndexObject> readSecondIndex(DataInputStream input) throws IOException {
        int num = input.readShort();
        List<IndexObject> res = new ArrayList<>(num);
        // FIXME second index value or content prefix or both may be greater than the max of integer
        for (int i = 0; i < num; i++) {
            int key = (int) readByLength(input, second.size);
            long offset = input.readLong();
            IndexObject temp = new IndexObject(key, getRecordNumForOffset(offset), getOffset(offset));
            temp.setMinKey((int) readByLength(input, third.size));
            temp.setMidKey((int) readByLength(input, third.size));
            temp.setMaxKey((int) readByLength(input, third.size));
            temp.setMidDelta(input.readShort());
            temp.setEndDelta(input.readShort());

            res.add(temp);
        }

        return res;
    }

    public int getFixedLength() {
        return fixedLength;
    }

    public IndexBuildStrategy<Integer> getBuildStrategy() {
        return buildStrategy;
    }

    public long combineNumAndOffset(int num, long offset) {
        return (offset & 0x0FFFFFFFFFFL) | ((num & 0x0FFFFFFL) << 40);
    }

    protected long writeContent(DataOutput output, String[] items, int idx) throws IOException {
        long written = 0;
        written += writeContent(output, items[idx], idx);
        for (int i = 0; i < items.length; i++) {
            if (i != idx) {
                written += writeContent(output, items[i], i);
            }
        }

        return written;
    }

    protected long writeContent(DataOutput output, String item, int idx) throws IOException {
        long written = 0;
        Integer len = contentFieldSize.get(idx);
        if (len != null) {
            written += writeByLength(output, len, Long.parseLong(item));
        }

        return written;
    }

    protected long writeFirstIndex(DataOutput output, List<IndexObject> firstIndexes) throws IOException {
        int len = firstIndexes.size();
        if (len > Short.MAX_VALUE) {
            throw new IllegalArgumentException("the number of first index over " + Short.MAX_VALUE);
        }

        long written = 0;
        // 输出一级索引个数
        output.writeShort(len);
        written += 2;

        // 再输出一级索引
        for (IndexObject firstIndex : firstIndexes) {
            written += writeByLength(output, first.size, firstIndex.key);
            output.writeLong(firstIndex.offset);

            written += 8;
        }

        // 最后输出一级索引长度
        output.writeInt(len * getFirstIndexUnitLength() + 2);
        written += 4;

        firstIndexes.clear();

        return written;
    }

    protected int getFirstIndexUnitLength() {
        return first.size + 8;
    }

    protected long writeSecondIndex(DataOutput output, List<IndexObject> secondIndexes) throws IOException {
        int len = secondIndexes.size();
        if (len > Short.MAX_VALUE) {
            throw new IllegalArgumentException("the number of second index over " + Short.MAX_VALUE);
        }

        long written = 0;
        // 先输出二级索引长度和数量
        output.writeInt(len * getSecondIndexUnitLength() + 2);
        output.writeShort(len);

        written += 6;
        // 再输出二级索引
        for (IndexObject indexObject : secondIndexes) {
            written += writeByLength(output, second.size, indexObject.key);
            output.writeLong(combineNumAndOffset(indexObject.num, indexObject.offset));
            written += 8;

            // 输出搜索信息
            written += writeByLength(output, third.size, indexObject.getMinKey());
            written += writeByLength(output, third.size, indexObject.getMidKey());
            written += writeByLength(output, third.size, indexObject.getMaxKey());

            output.writeShort(indexObject.getMidDelta());
            output.writeShort(indexObject.getEndDelta());
            written += 4;
        }


        secondIndexes.clear();

        return written;
    }

    protected int getSecondIndexUnitLength() {
        return second.size + contentFieldSize.get(0) * 3 + 12;
    }

    protected int writeByLength(DataOutput output, int length, long value) throws IOException {
        switch (length) {
            case 1:
                output.writeByte((int) value);
                break;
            case 2:
                output.writeShort((int) value);
                break;
            case 4:
                output.writeInt((int) value);
                break;
            case 8:
                output.writeLong(value);
                break;
            default:
                throw new IllegalArgumentException("illegal size: " + value);
        }

        return length;
    }

    protected void writeByLength(DataOutput output, int length, byte[] value) throws IOException {
        output.writeShort(length);
        output.write(value, 0, length);
    }

    public long readByLength(DataInput input, int length) throws IOException {
        long res;
        switch (length) {
            case 1:
                res = input.readByte();
                break;
            case 2:
                res = input.readShort();
                break;
            case 4:
                res = input.readInt();
                break;
            case 8:
                res = input.readLong();
                break;
            default:
                throw new IllegalArgumentException("illegal size: " + length);
        }

        return res;
    }

    public String readByLength(DataInput input, String charset) throws IOException {
        int len = input.readShort();
        byte[] arr = new byte[len];
        input.readFully(arr);

        return new String(arr, charset);
    }

    public Map<Integer, Integer> getContentFieldSize() {
        return contentFieldSize;
    }

    public int getIdx() {
        return idx;
    }

    private void appendMetaInfo(DataOutput output) throws IOException {
        int len = 0;
        // 输出字符集
        byte[] ch = charset.getBytes();
        output.writeByte(ch.length);
        output.write(ch);
        len += (ch.length + 1);

        // 输出用于索引的字段下标
        output.writeByte(idx);
        len++;

        // 输出二级索引构建时三段切分范围
        output.writeByte(first.start);
        output.writeByte(first.end);
        output.writeByte(first.size);

        output.writeByte(second.start);
        output.writeByte(second.end);
        output.writeByte(second.size);

        output.writeByte(third.start);
        output.writeByte(third.end);
        output.writeByte(third.size);
        len += 9;

        // 输出内容各个字段的长度信息
        int num = contentFieldSize.size();
        output.writeByte(num);
        for (Map.Entry<Integer, Integer> entry : contentFieldSize.entrySet()) {
            output.writeByte(entry.getKey());
            output.writeByte(entry.getValue());
        }

        len += (2 * num + 1);

        // 输出整个元数据的长度信息
        output.writeInt(len);
    }

    public String getMetaInfo(DataInput input) throws IOException {
        // 获取字符集
        int size = input.readByte();
        byte[] ch = new byte[size];
        input.readFully(ch);

        charset = new String(ch);

        // 获取用于索引的字段下标
        idx = input.readByte();

        // 获取二级索引构建时三段切分范围
        first = new Range(input.readByte(), input.readByte(), input.readByte());
        second = new Range(input.readByte(), input.readByte(), input.readByte());
        third = new Range(input.readByte(), input.readByte(), input.readByte());

        // 获取内容各个字段的长度信息
        int num = input.readByte();
        contentFieldSize = new HashMap<>();
        for (int i = 0; i < num; i++) {
            contentFieldSize.put((int) input.readByte(), (int) input.readByte());
        }

        return String.format("Meta Info:\nCharset=%s\nidx=%d\nRange1: %s\nRange2: %s\nRange3: %s\nContentFieldSize: %s\n",
                charset, idx, first, second, third, contentFieldSize);
    }

    private static class CountItem {
        int key;
        int count;

        CountItem(int key, int count) {
            this.key = key;
            this.count = count;
        }
    }

    public static class SecondaryFixedLengthIndexBuild extends IndexBuildStrategy<Integer> {
        private Map<Integer, Integer> restore;
        private List<Range> ranges;

        public SecondaryFixedLengthIndexBuild(Range range1, Range range2, Range range3) {
            super(range1, range2, range3);

            ranges = Arrays.asList(range1, range2, range3);
            Collections.sort(ranges, new Comparator<Range>() {
                @Override
                public int compare(Range o1, Range o2) {
                    return o1.start - o2.start;
                }
            });

            restore = new HashMap<>();
            int n = 0;
            int index = ranges.indexOf(range1);
            restore.put(index, n++);

            index = ranges.indexOf(range2);
            restore.put(index, n++);

            index = ranges.indexOf(range3);
            restore.put(index, n);
        }

        @Override
        public List<Integer> build(String str) {
            List<Integer> res = new ArrayList<>(3);
            res.add(Integer.parseInt(str.substring(range1.start, range1.end + 1)));
            res.add(Integer.parseInt(str.substring(range2.start, range2.end + 1)));
            res.add(Integer.parseInt(str.substring(range3.start, range3.end + 1)));

            return res;
        }

        @Override
        public String restore(List<Integer> indexes) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < restore.size(); i++) {
                int n = restore.get(i);
                int current = indexes.get(n);
                Range range = ranges.get(i);
                builder.append(String.format(String.format("%%0%dd", range.end - range.start + 1), current));
            }

            return builder.toString();
        }
    }

    public static class DefaultItr implements Iterator<List<String>> {
        private SecondaryFixedLengthIndexFile indexFile;
        private DataInput input;
        private int num;

        private Map<Integer, Integer> fieldsLength;

        public DefaultItr(DataInput input, int num, SecondaryFixedLengthIndexFile indexFile) {
            this.indexFile = indexFile;
            this.input = input;
            this.num = num;

            this.fieldsLength = indexFile.getContentFieldSize();
        }

        @Override
        public boolean hasNext() {
            return num > 0;
        }

        @Override
        public List<String> next() {
            List<String> res = new ArrayList<>();
            try {
                int n = fieldsLength.size();
                for (int j = 0; j < n; j++) {
                    res.add(String.valueOf(indexFile.readByLength(input, fieldsLength.get(j))));
                }

                --num;

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return res;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
