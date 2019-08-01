package me.johntse.toy.index.common;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 二级定长索引文件
 *
 * @author John Tse
 */
public abstract class SecondaryFixedLengthIndex implements Index<SecondaryFixedLengthIndexFile, String> {
    /**
     * 最大使用内存，主要用于缓存读取的内容。单位：MB
     */
    protected int maxCacheMemory;

    /**
     * 每次I/O读取内容条数。
     */
    protected int batchSize;

    /**
     * 索引数组。
     */
    // FIXME first index value or second index value or both may be greater than the max of integer
    protected Map<Integer, Map<Integer, IndexObject>> index;

    /**
     * 索引文件。
     */
    protected SecondaryFixedLengthIndexFile indexFile;


    public SecondaryFixedLengthIndex(int maxCacheMemory, int batchSize) {
        this.maxCacheMemory = maxCacheMemory;
        this.batchSize = batchSize;
    }

    @Override
    public void load(SecondaryFixedLengthIndexFile idx) throws IOException {
        if (ready()) {
            return;
        }

        byte[] firstIndexContent = idx.readIndex();
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(firstIndexContent));
        List<IndexObject> firstIndex = idx.readFirstIndex(input);
        index = new HashMap<>(firstIndex.size());
        for (IndexObject indexObject : firstIndex) {
            byte[] secondIndexLen = idx.readContent(indexObject.offset, 4);
            DataInputStream lenData = new DataInputStream(new ByteArrayInputStream(secondIndexLen));
            byte[] secondIndexContent = idx.readContent(indexObject.offset + 4, lenData.readInt());
            DataInputStream content = new DataInputStream(new ByteArrayInputStream(secondIndexContent));
            List<IndexObject> secondIndex = idx.readSecondIndex(content);
            int numOfSecondIndex = secondIndex.size();
            Map<Integer, IndexObject> secondIndexMap = new HashMap<>(numOfSecondIndex);
            index.put(indexObject.key, secondIndexMap);
            for (IndexObject secondIndexObject : secondIndex) {
                secondIndexMap.put(secondIndexObject.key, secondIndexObject);
            }
        }

        indexFile = idx;
    }

    @Override
    public boolean ready() {
        return index != null;
    }
}
