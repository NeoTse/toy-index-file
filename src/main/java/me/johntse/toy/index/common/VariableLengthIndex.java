package me.johntse.toy.index.common;

import me.johntse.toy.index.impl.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 姓名索引。
 *
 * @author John Tse
 */
public class VariableLengthIndex implements Index<VariableLengthIndexFile, String> {
    /**
     * 每次I/O读取内容条数。
     */
    private int batchSize;

    /**
     * 内容缓存。
     */
    private String[] index;


    /**
     * 索引文件。
     */
    private VariableLengthIndexFile indexFile;

    public VariableLengthIndex() {
        this.batchSize = 1024;
    }

    public VariableLengthIndex(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public void load(VariableLengthIndexFile idx) throws IOException {
        if (ready()) {
            return;
        }

        List<IndexObject> indexObjects = idx.readVariableIndex();
        int size = indexObjects.size();
        index = new String[size];
        for (int i = 0; i < size; ) {
            int delta = Math.min(batchSize, size - i);
            String[] content = idx.getContent(indexObjects, i, i + delta);
            System.arraycopy(content, 0, index, i, content.length);

            i += delta;
        }
    }

    @Override
    public List<SearchResult> search(String text) throws IOException {
        return search(Integer.parseInt(text));
    }

    public List<SearchResult> search(int idx) throws IOException {
        List<SearchResult> res = new ArrayList<>(1);
        if (idx >= 0 && idx < index.length) {
            res.add(new SearchResult(index[idx]));
        }

        return res;
    }

    @Override
    public boolean ready() {
        return index != null;
    }
}
