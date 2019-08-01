package me.johntse.toy.index.common;

/**
 * 区间对象，用来表示一个索引构造区间。
 *
 * @author John Tse
 */
public class Range {
    public final int start;
    public final int end;
    public final int size;

    public Range(int start, int end, int size) {
        this.start = start;
        this.end = end;
        this.size = size;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj.getClass() == this.getClass()) {
            Range other = (Range) obj;
            return start == other.start && end == other.end && size == other.size;
        }

        return false;
    }

    @Override
    public String toString() {
        return String.format("start=%d, end=%d, size=%d", start, end, size);
    }

}
