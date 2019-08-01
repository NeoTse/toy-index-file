package me.johntse.toy.index.impl;

import com.google.common.hash.BloomFilter;
import me.johntse.toy.index.common.IndexObject;
import me.johntse.toy.index.common.SecondaryFixedLengthIndex;
import me.johntse.toy.index.common.SecondaryFixedLengthIndexFile;
import me.johntse.toy.index.common.VariableLengthIndex;
import me.johntse.toy.index.util.CertNumUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 身份证、手机号和姓名索引.
 *
 * @author John Tse
 */
public class SFZMobileIndex extends SecondaryFixedLengthIndex {
    private final static List<SearchResult> EMPTY_RESULT = new ArrayList<>(0);
    private VariableLengthIndex nameIndex;
    private IndexType type;

    private BloomFilter<CharSequence> bloomFilter;

    public SFZMobileIndex(VariableLengthIndex nameIndex, IndexType type) {
        this(nameIndex, type, 64, 1024);
    }

    public SFZMobileIndex(VariableLengthIndex nameIndex, IndexType type, int maxCacheMemory, int batchSize) {
        super(maxCacheMemory, batchSize);

        this.nameIndex = nameIndex;

        if (type != IndexType.SFZ && type != IndexType.MOBILE) {
            throw new IllegalArgumentException("unsupported index type");
        }

        this.type = type;
    }

    private static SearchResult toSearchResult(DataInput input, SFZMobileIndex index)
            throws IOException {
        SearchResult result = new SearchResult();
        IndexType type = index.type;
        SecondaryFixedLengthIndexFile indexFile = index.indexFile;
        Map<Integer, Integer> contentFieldSize = indexFile.getContentFieldSize();

        long v = indexFile.readByLength(input, contentFieldSize.get(1));
        if (type == IndexType.SFZ) {
            result.setMob(String.valueOf(v));
        } else {
            result.setCertNum(CertNumUtils.addVerificationCode(v));
        }

        int idx = (int) indexFile.readByLength(input, contentFieldSize.get(2));
        result.setName(index.nameIndex.search(idx).get(0).getName());

        return result;
    }

    public void setBloomFilter(BloomFilter<CharSequence> bloomFilter) {
        this.bloomFilter = bloomFilter;
    }

    @Override
    public void load(SecondaryFixedLengthIndexFile idx) throws IOException {
        super.load(idx);
    }

    @Override
    public List<SearchResult> search(String text) throws IOException {
        List<Integer> indexes = indexFile.getBuildStrategy().build(text);
        int firstIndex = indexes.get(0);
        int secondIndex = indexes.get(1);
        int thirdIndex = indexes.get(2);

        Map<Integer, IndexObject> next = index.get(firstIndex);
        if (next == null) {
            return EMPTY_RESULT;
        }


        IndexObject searchIndex = next.get(secondIndex);
        if (searchIndex == null) {
            return EMPTY_RESULT;
        }

        // 通过BloomFilter过滤
        if (bloomFilter != null && !bloomFilter.mightContain(text)) {
            return EMPTY_RESULT;
        }

        IndexObject indexObject = searchIndex;
        // 使用搜索信息进一步确认
        if (thirdIndex < searchIndex.getMinKey() || thirdIndex > searchIndex.getMaxKey()) {
            return EMPTY_RESULT;
        } else if (thirdIndex == searchIndex.getMaxKey()) {
            indexObject = new IndexObject(secondIndex,
                    searchIndex.num - searchIndex.getEndDelta(),
                    searchIndex.offset + indexFile.getFixedLength() * searchIndex.getEndDelta());
        } else if (thirdIndex >= searchIndex.getMidKey()) {
            indexObject = new IndexObject(secondIndex,
                    searchIndex.num - searchIndex.getMidDelta(),
                    searchIndex.offset + indexFile.getFixedLength() * searchIndex.getMidDelta());
        }

        List<SearchResult> results = new ArrayList<>();

        // 缓存中不存在，或者只缓存了索引位置，读磁盘
        Iterator<SearchResult> iterator = new Itr(indexObject, this, thirdIndex);
        while (iterator.hasNext()) {
            SearchResult result = iterator.next();
            if (result != null) {
                if (type == IndexType.SFZ) {
                    result.setCertNum(text);
                } else {
                    result.setMob(text);
                }

                results.add(result);
            }
        }

        return results;
    }

    private static class Itr implements Iterator<SearchResult> {
        private IndexObject index;
        private SecondaryFixedLengthIndexFile indexFile;
        private SFZMobileIndex sfzMobileIndex;
        private Map<Integer, Integer> contentFieldSize;

        private int batch;
        private int currentReadCount;
        private int cursor;

        private long offset;

        private byte[] content;
        private int searchKey;

        private boolean terminated;
        private boolean matched;

        public Itr(IndexObject index, SFZMobileIndex sfzMobileIndex, int searchKey) {
            this.index = index;
            this.sfzMobileIndex = sfzMobileIndex;
            this.indexFile = sfzMobileIndex.indexFile;
            this.batch = sfzMobileIndex.batchSize;
            this.searchKey = searchKey;
            this.contentFieldSize = indexFile.getContentFieldSize();

            currentReadCount = 0;
            cursor = 0;

            offset = index.offset;
            terminated = false;
            matched = false;
        }

        @Override
        public boolean hasNext() {
            return currentReadCount < index.num && !terminated;
        }

        @Override
        public SearchResult next() {
            try {
                if (content == null) {
                    int num = Math.min(index.num - currentReadCount, batch);
                    content = indexFile.readContent(offset, num * indexFile.getFixedLength());

                    cursor = 0;
                }

                DataInputStream input = new DataInputStream(new ByteArrayInputStream(content,
                        cursor, content.length - cursor));

                int len = contentFieldSize.get(indexFile.getIdx());
                SearchResult result = null;
                while (cursor < content.length) {
                    int key = (int) indexFile.readByLength(input, len);

                    if (searchKey == key) {
                        result = toSearchResult(input, sfzMobileIndex);

                        matched = true;
                    } else {
                        if (matched) {
                            terminated = true;
                            matched = false;
                            break;
                        }

                        input.skipBytes(indexFile.getFixedLength() - len);
                    }

                    cursor += indexFile.getFixedLength();
                    ++currentReadCount;

                    if (result != null) {
                        return result;
                    }
                }

                offset += content.length;
                content = null;
            } catch (IOException e) {
                e.printStackTrace();
                terminated = true;
            }

            return null;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
