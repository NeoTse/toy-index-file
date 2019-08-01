package me.johntse.toy.index.tools;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * Bloom Filter过滤器序列化文件构建
 *
 * @author John Tse
 */
public class BloomFilterCreator {
    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            usage();
            return;
        }

        File data = new File(args[0]);
        if (!data.exists()) {
            throw new IllegalArgumentException("data file doesn't exist. " + data);
        }

        int index = Integer.parseInt(args[1]);
        if (index < 0) {
            throw new IllegalArgumentException("the index must be a positive number.");
        }

        double expectedFpp = Double.parseDouble(args[2]);
        if (expectedFpp <= 0 || expectedFpp >= 1.0) {
            throw new IllegalArgumentException("expectedFpp should be in (0.0, 1.0)");
        }

        int recordNum = Integer.parseInt(args[3]);
        if (recordNum <= 0) {
            throw new IllegalArgumentException("the number of records must be a positive number.");
        }

        File output = new File(args[4]);
        BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("utf8")),
                recordNum, expectedFpp);
        try (Scanner scanner = new Scanner(data);
             FileOutputStream outputStream = new FileOutputStream(output)) {
            String line;
            int n = 0;
            while (scanner.hasNextLine() && n < recordNum) {
                line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    String[] items = line.split("\t");
                    if (items.length > index && bloomFilter.put(items[index])) {
                        ++n;
                    }
                }
            }

            System.out.println(String.format("%d unique record put into Bloom Filter.\n", n));
            System.out.println("start writing bloom filter...");
            bloomFilter.writeTo(outputStream);
            System.out.println("finished write. " + output);
        }
    }

    private static void usage() {
        System.out.println("构建Bloom Filter。\n[Usage] BloomFilterMake [data_file] [index] [expected_fpp] [record_num] [output_file]\n" +
                "\tdata_file: 构建时使用的数据文本文件\n" +
                "\tindex: 构建时使用的数据文本文件使用数据文本文件中哪一列\n" +
                "\texpected_fpp: 期望的最大误判率\n" +
                "\trecord_num: 构建时使用的记录数\n" +
                "\toutput_file: 输出的Bloom Filter过滤器序列化二进制文件\n");
    }
}
