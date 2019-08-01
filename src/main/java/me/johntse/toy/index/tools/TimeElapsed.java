package me.johntse.toy.index.tools;

/**
 * 计时工具。
 *
 * @author John Tse
 */
class TimeElapsed {
    private static final double NANO_TO_MS = 1000000.0;
    private long start = -1;
    private long end = -1;

    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;
    private long sum = 0;
    private int count = 0;

    public TimeElapsed() {
        start = System.nanoTime();

    }

    public void start() {
        start = System.nanoTime();
    }

    public long elapsed() {
        end();
        long elapsed = end - start;
        min = Math.min(min, elapsed);
        max = Math.max(max, elapsed);
        sum += elapsed;
        ++count;

        return elapsed;
    }

    public void end() {
        end = System.nanoTime();
    }

    public long getMax() {
        return max;
    }

    public long getMin() {
        return min;
    }

    public double getAvg() {
        return sum * 1.0 / count;
    }

    public long getSum() {
        return sum;
    }

    @Override
    public String toString() {
        return String.format("Total time: %f ms\nAvg time: %f ms\nMax time: %f ms\nMin time: %f ms\n",
                getSum() / NANO_TO_MS,
                getAvg() / NANO_TO_MS,
                getMax() / NANO_TO_MS,
                getMin() / NANO_TO_MS);
    }
}
