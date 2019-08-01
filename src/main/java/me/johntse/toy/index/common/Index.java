package me.johntse.toy.index.common;

import me.johntse.toy.index.impl.SearchResult;

import java.io.IOException;
import java.util.List;

/**
 * 索引接口。
 *
 * @author John Tse
 */
public interface Index<T extends Indexable, V> {
    void load(T indexFile) throws IOException;

    List<SearchResult> search(V text) throws IOException;

    boolean ready();
}
