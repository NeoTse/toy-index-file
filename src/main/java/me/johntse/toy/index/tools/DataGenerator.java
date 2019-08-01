package me.johntse.toy.index.tools;

import me.johntse.toy.index.util.CertNumUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * 数据生成器。
 *
 * @author John Tse
 */
public class DataGenerator {
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static List<String> cityCodes = new ArrayList<>();
    private static List<String> dates = new ArrayList<>();
    private static List<String> netCodes = new ArrayList<>();
    private static List<String> names = new ArrayList<>();

    private static boolean nameEncoded = true;
    private static File nameFile;

    public static void main(String[] args) throws IOException {
        if (args.length < 4 || args.length > 6) {
            usage();
            return;
        }

        int num = Integer.parseInt(args[0]);
        if (num < 0) {
            throw new IllegalArgumentException("the num of records must be a positive number.");
        }

        File outputFile = new File(args[1]);
        if (outputFile.exists()) {
            throw new IllegalArgumentException("output file existed. " + outputFile);
        }

        nameEncoded = Boolean.valueOf(args[3]);
        nameFile = new File(args[2]);
        if (!nameFile.exists()) {
            throw new IllegalArgumentException("name file isn't existed. " + outputFile);
        }

        String encoding = "utf8";
        if (args.length == 5) {
            encoding = args[4];
        }

        System.out.println("load dict data...");
        loadDict();
        System.out.println("finished load.");

        System.out.println("start generate data...");
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), encoding))) {
            for (int i = 0; i < num; i++) {
                writer.write(getOneRecord());
            }
        }

        System.out.println("finished generate data. " + outputFile);
    }

    private static void usage() {
        System.out.println("生成测试数据。\n[Usage] DataGenerate [records_num] [output_file] [name_file] [name_encoded] <encoding>\n" +
                "\trecords_num: 生成的记录条数。\n" +
                "\toutput_file: 生成的数据文件。\n" +
                "\tname_file: 姓名文件，用于生成记录中的姓名。\n" +
                "\tname_encoded: 是否对姓名进行编码，即生成的记录中使用姓名编码，而非姓名。\n" +
                "\tencoding: 文件编码方式，默认为'utf8'");
    }

    private static String getOneRecord() {
        return String.format("%s\t%s\t%s\n", getCertNum(), getMobile(), getName());
    }

    private static String getCertNum() {
        String pre = cityCodes.get(RANDOM.nextInt(cityCodes.size())) +
                (RANDOM.nextInt(100) + 1920) +
                dates.get(RANDOM.nextInt(dates.size())) +
                String.format("%03d", RANDOM.nextInt(1000));

        return CertNumUtils.addVerificationCode(pre);
    }

    private static String getMobile() {
        return netCodes.get(RANDOM.nextInt(netCodes.size())) +
                String.format("%04d", RANDOM.nextInt(10000)) +
                String.format("%04d", RANDOM.nextInt(10000));
    }

    private static String getName() {
        int index = RANDOM.nextInt(names.size());
        if (nameEncoded) {
            return "" + index;
        } else {
            return names.get(index);
        }

    }

    private static void loadDict() throws IOException {
        try (Scanner cityCode = new Scanner(Thread.currentThread().getContextClassLoader().getResourceAsStream("citycodes.txt"));
             Scanner date = new Scanner(Thread.currentThread().getContextClassLoader().getResourceAsStream("dates.txt"));
             Scanner netCode = new Scanner(Thread.currentThread().getContextClassLoader().getResourceAsStream("netcodes.txt"));
             Scanner name = new Scanner(nameFile)) {
            String line;
            while (cityCode.hasNextLine()) {
                line = cityCode.nextLine();
                if (!line.isEmpty()) {
                    cityCodes.add(line.trim());
                }
            }
            System.out.println(String.format("%d city code loaded.", cityCodes.size()));

            while (date.hasNextLine()) {
                line = date.nextLine();
                if (!line.isEmpty()) {
                    dates.add(line.trim());
                }
            }
            System.out.println(String.format("%d date loaded.", dates.size()));

            while (netCode.hasNextLine()) {
                line = netCode.nextLine();
                if (!line.isEmpty()) {
                    netCodes.add(line.trim());
                }
            }
            System.out.println(String.format("%d net code loaded.", netCodes.size()));

            while (name.hasNextLine()) {
                line = name.nextLine();
                if (!line.isEmpty()) {
                    names.add(line.trim());
                }
            }
            System.out.println(String.format("%d name loaded.", names.size()));
        }
    }

}
