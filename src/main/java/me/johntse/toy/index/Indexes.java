package me.johntse.toy.index;

import com.google.common.hash.BloomFilter;
import me.johntse.toy.index.common.*;
import me.johntse.toy.index.impl.IndexType;
import me.johntse.toy.index.impl.MobileIndexFile;
import me.johntse.toy.index.impl.SFZMobileIndex;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 索引工厂.
 *
 * @author John Tse
 */
public final class Indexes {
    public static File makeSecondaryFixedLengthIndexFile(File text,
                                                         IndexType type,
                                                         List<Range> ranges,
                                                         Map<Integer, Integer> contentFieldSize,
                                                         int idx,
                                                         String charset
    ) throws IOException {
        if (ranges.size() != 3) {
            throw new IllegalArgumentException("the number of ranges must be three");
        }

        if (type == IndexType.NAME) {
            throw new IllegalArgumentException("unsupported index type: " + IndexType.NAME);
        }

        SecondaryFixedLengthIndexFile file;
        switch (type) {
            case MOBILE:
                file = new MobileIndexFile(idx, ranges.get(0), ranges.get(1),
                        ranges.get(2), charset, contentFieldSize);
                break;
            default:
                file = new SecondaryFixedLengthIndexFile(idx, ranges.get(0), ranges.get(1),
                        ranges.get(2), charset, contentFieldSize);
                break;
        }

        return file.createImpl(text);

    }

    public static File makeVariableLengthIndexFile(File text, String charset, int idx, int indexUnitLength)
            throws IOException {
        if (indexUnitLength < 0 || indexUnitLength > 4) {
            throw new IllegalArgumentException(String.format("unsupported size: %d, it should be in (0, 4]", indexUnitLength));
        }

        VariableLengthIndexFile file = new VariableLengthIndexFile(charset, idx, indexUnitLength);
        return file.createImpl(text);
    }

    public static SecondaryFixedLengthIndex makeSFZMobileIndex(File indexBinFile,
                                                               VariableLengthIndex nameIndex,
                                                               IndexType type,
                                                               int maxCacheMemory, int batchSize)
            throws IOException {
        return makeSFZMobileIndex(indexBinFile, nameIndex, type, maxCacheMemory, batchSize, null);
    }

    public static SecondaryFixedLengthIndex makeSFZMobileIndex(File indexBinFile,
                                                               VariableLengthIndex nameIndex,
                                                               IndexType type,
                                                               int maxCacheMemory, int batchSize,
                                                               BloomFilter<CharSequence> bloomFilter)
            throws IOException {
        if (type == IndexType.NAME) {
            throw new IllegalArgumentException("unsupported index type: " + type);
        }

        if (maxCacheMemory > 1024) {
            throw new IllegalArgumentException(String.format("Illegal size for max cache memory: %d, it should not more than 1GB",
                    maxCacheMemory));
        }

        if (batchSize < 0 || batchSize > 1024 * 1024) {
            throw new IllegalArgumentException(String.format("Illegal size for batch: %d, it should be in (0, 1048576]",
                    batchSize));
        }

        if (nameIndex == null || !nameIndex.ready()) {
            throw new IllegalArgumentException("name index is not ready.");
        }

        SecondaryFixedLengthIndexFile indexFile = new SecondaryFixedLengthIndexFile();
        indexFile.load(indexBinFile);

        SFZMobileIndex index = new SFZMobileIndex(nameIndex, type, maxCacheMemory, batchSize);
        ;
        index.load(indexFile);
        index.setBloomFilter(bloomFilter);

        return index;
    }

    public static VariableLengthIndex makeNameIndex(File indexBinFile, int batchSize) throws IOException {
        VariableLengthIndexFile indexFile = new VariableLengthIndexFile();
        indexFile.load(indexBinFile);

        VariableLengthIndex index = new VariableLengthIndex(batchSize);
        index.load(indexFile);

        return index;
    }


}
