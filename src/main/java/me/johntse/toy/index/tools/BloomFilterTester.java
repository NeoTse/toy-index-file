package me.johntse.toy.index.tools;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * Bloom Filter测试
 *
 * @author John Tse
 */
public class BloomFilterTester {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            usage();
            return;
        }

        File bloomFilterBin = new File(args[0]);
        if (!bloomFilterBin.exists()) {
            throw new IllegalArgumentException("bloom filter bin file don't exist. " + bloomFilterBin);
        }

        File testFile = new File(args[1]);
        if (!testFile.exists()) {
            throw new IllegalArgumentException("test file don't exist. " + testFile);
        }

        int index = Integer.parseInt(args[2]);
        if (index < 0) {
            throw new IllegalArgumentException("the index must be a positive number.");
        }

        int passed = 0;
        int denied = 0;
        TimeElapsed elapsed = new TimeElapsed();

        try (InputStream inputStream = new FileInputStream(bloomFilterBin);
             Scanner scanner = new Scanner(testFile)) {
            System.out.println("start init bloom filter.");
            BloomFilter<CharSequence> bloomFilter = BloomFilter.readFrom(inputStream,
                    Funnels.stringFunnel(Charset.forName("utf8")));
            System.out.println("bloom filter created.");

            System.out.println("begin bloom filter test, using file " + testFile);
            String line;
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                if (!line.isEmpty()) {
                    String[] items = line.split("\t");
                    if (items.length > index) {
                        elapsed.start();

                        if (bloomFilter.mightContain(items[index])) {
                            passed++;
                        } else {
                            denied++;
                        }

                        elapsed.elapsed();
                    }
                }
            }
        }

        System.out.println("end bloom filter test.");
        System.out.println(String.format("passed = %d\ndenied = %d", passed, denied));
        System.out.println(elapsed);
    }

    private static void usage() {
        System.out.println("Bloom Filter测试。\n[Usage] BloomFilterTest [bloom_filter_bin] [test_file] [index]\n" +
                "\tbloom_filter_bin: Bloom Filter序列化二进制文件\n" +
                "\ttest_file: 测试数据文本文件\n" +
                "\tindex: 测试数据文件哪一列作为测试\n");
    }
}
