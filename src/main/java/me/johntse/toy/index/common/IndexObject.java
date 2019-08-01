package me.johntse.toy.index.common;

/**
 * 索引值对象。
 *
 * @author John Tse
 */
public class IndexObject {
    public final int key;
    public final int num;
    public final long offset;

    private int minKey = Integer.MIN_VALUE;
    private int midKey = Integer.MIN_VALUE;
    private int maxKey = Integer.MIN_VALUE;

    private int midDelta = Integer.MIN_VALUE;
    private int endDelta = Integer.MIN_VALUE;

    public IndexObject(int key, long offset) {
        this.key = key;
        this.offset = offset;
        this.num = -1;
    }

    public IndexObject(int key, int num, long offset) {
        this.key = key;
        this.num = num;
        this.offset = offset;
    }

    public int getMinKey() {
        return minKey;
    }

    public void setMinKey(int minKey) {
        this.minKey = minKey;
    }

    public int getMidKey() {
        return midKey;
    }

    public void setMidKey(int midKey) {
        this.midKey = midKey;
    }

    public int getMaxKey() {
        return maxKey;
    }

    public void setMaxKey(int maxKey) {
        this.maxKey = maxKey;
    }

    public int getMidDelta() {
        return midDelta;
    }

    public void setMidDelta(int midDelta) {
        this.midDelta = midDelta;
    }

    public int getEndDelta() {
        return endDelta;
    }

    public void setEndDelta(int endDelta) {
        this.endDelta = endDelta;
    }

    @Override
    public String toString() {
        return String.format("key=%d, offset=%d, num=%d, minKey=%d, midKey=%d, maxKey=%d, midDelta=%d, endDelta=%d",
                key, offset, num, minKey, midKey, maxKey, midDelta, endDelta);
    }
}
