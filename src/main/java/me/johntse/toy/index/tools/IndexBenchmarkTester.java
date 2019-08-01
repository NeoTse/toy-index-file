package me.johntse.toy.index.tools;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import me.johntse.toy.index.Indexes;
import me.johntse.toy.index.common.Index;
import me.johntse.toy.index.common.Indexable;
import me.johntse.toy.index.impl.IndexType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 索引性能测试器
 *
 * @author John Tse
 */
public class IndexBenchmarkTester {
    public static void main(String[] args) throws IOException {
        if (args.length < 4 || args.length > 6) {
            usage();
            return;
        }

        IndexType type = IndexType.valueOf(args[0]);
        File indexFile = new File(args[1]);
        if (!indexFile.canRead()) {
            throw new IllegalArgumentException("index file can't read. " + indexFile);
        }

        String[] files = args[2].split(",");
        List<File> testFiles = new ArrayList<>();
        for (String file : files) {
            File temp = new File(file);
            if (!temp.canRead()) {
                throw new IllegalArgumentException("test file can't read. " + temp);
            }

            testFiles.add(temp);
        }


        int batchSize = Integer.parseInt(args[3]);
        if (batchSize < 0) {
            throw new IllegalArgumentException("times or batch_size must be a positive number.");
        }

        System.out.println(String.format("start init %s index.", type));

        Index<?, String> index;
        switch (type) {
            case NAME:
                index = Indexes.makeNameIndex(indexFile, batchSize);
                break;
            case MOBILE:
            case SFZ:
                if (args.length < 5) {
                    usage();
                    return;
                }

                File nameIndexFile = new File(args[4]);

                BloomFilter<CharSequence> bloomFilter = null;
                if (args.length == 6) {
                    File bloomFilterBin = new File(args[5]);

                    System.out.println("found a bloom filter bin file, now init a bloom filter");
                    bloomFilter = BloomFilter.readFrom(new FileInputStream(bloomFilterBin),
                            Funnels.stringFunnel(Charset.forName("utf8")));
                    System.out.println("bloom filter created.");
                }

                index = Indexes.makeSFZMobileIndex(indexFile,
                        Indexes.makeNameIndex(nameIndexFile, batchSize),
                        type, 64, batchSize, bloomFilter);
                break;
            default:
                throw new IllegalArgumentException("unknown test type");
        }

        System.out.println(String.format("%s index created.", type));

        // 避免查询过程中出现Full GC，造成过长的响应时间
        System.gc();

        for (File testFile : testFiles) {
            indexTest(type, index, testFile);
        }
    }

    private static void usage() {
        System.out.println("索引文件性能测试。\n[Usage] IndexBenchmark [index_type] [index_file] [test_file] [batch_size] " +
                "<name_index_file> <bloom_filter_bin>\n" +
                "\tindex_type: 填写值包括SFZ, MOBILE, NAME，分别表示身份证号码、手机号码和姓名索引测试\n" +
                "\tindex_file: 和测试类型对应的索引文件\n" +
                "\ttest_file: 测试文本文件，模拟查询，多个文件时使用','分隔\n" +
                "\tbatch_size: 每次从磁盘读取的记录条数\n" +
                "\tname_index_file: 姓名索引文件，当测试类型为SFZ和MOBILE时，需要设置\n" +
                "\tbloom_filter_bin: bloom filter二进制文件，用于构造bloom filter，不设置时表示不使用bloom filter。" +
                "当测试类型为SFZ和MOBILE时，设置有效\n");
    }

    private static <T extends Indexable> void indexTest(IndexType type, Index<T, String> index, File testFile) throws IOException {
        System.out.println(String.format("begin %s index test, using file %s", type.name(), testFile));

        TimeElapsed elapsed = new TimeElapsed();
        try (Scanner scanner = new Scanner(testFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (!line.isEmpty()) {
                    String[] items = line.split("\t");
                    elapsed.start();
                    index.search(items[0]);
                    elapsed.elapsed();
                }
            }
        }

        System.out.println(String.format("end %s index test", type.name()));
        System.out.println(elapsed.toString());
    }

}
