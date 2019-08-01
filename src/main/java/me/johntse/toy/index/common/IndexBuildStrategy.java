package me.johntse.toy.index.common;

import java.util.List;

/**
 * 索引构建策略。
 *
 * @author John Tse
 */
public abstract class IndexBuildStrategy<T> {
    protected Range range1;
    protected Range range2;
    protected Range range3;

    public IndexBuildStrategy(Range range1, Range range2, Range range3) {
        this.range1 = range1;
        this.range2 = range2;
        this.range3 = range3;
    }

    public abstract List<T> build(String str);

    public abstract String restore(List<T> indexes);
}
