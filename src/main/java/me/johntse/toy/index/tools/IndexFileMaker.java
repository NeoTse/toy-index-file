package me.johntse.toy.index.tools;

import me.johntse.toy.index.Indexes;
import me.johntse.toy.index.common.Range;
import me.johntse.toy.index.impl.IndexType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * 索引文件构建器。
 *
 * @author John Tse
 */
public class IndexFileMaker {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            usage();
            return;
        }

        IndexType type = IndexType.valueOf(args[0]);
        System.out.println("begin create index " + type);

        File result;
        File input;
        int idx;
        String charset = "utf8";

        switch (type) {
            case SFZ:
            case MOBILE:
                if (args.length < 10) {
                    usage();
                    return;
                }

                input = new File(args[1]);
                if (!input.canRead()) {
                    System.err.println("Input file can't read. file: " + input);
                    return;
                }

                idx = Integer.parseInt(args[2]);
                int num = Integer.parseInt(args[6]);

                if (args.length == num + 8) {
                    charset = args[args.length - 1];
                    Charset.forName(charset);
                }

                String[] contentFieldSize = Arrays.asList(args).subList(7, 7 + num).toArray(new String[0]);

                result = Indexes.makeSecondaryFixedLengthIndexFile(input, type,
                        getRange(args[3], args[4], args[5]), getContentFieldSize(contentFieldSize), idx, charset);
                break;
            case NAME:
                input = new File(args[1]);
                if (!input.canRead()) {
                    System.err.println("Input file can't read. file: " + input);
                    return;
                }

                idx = Integer.parseInt(args[2]);
                if (args.length == 4) {
                    charset = args[3];
                    Charset.forName(charset);
                }

                result = Indexes.makeVariableLengthIndexFile(input, charset, idx, 1);
                break;
            default:
                throw new IllegalArgumentException("unknown index type: " + type);
        }
        System.out.println("create successfully. Index file: " + result);
    }

    private static void usage() {
        System.out.println("构建索引文件。\n[Usage] IndexFileMake [index_type] [text_input_file] [idx] " +
                "<range1,range2,range3> <field_num, field_size, field_size, field_size, ...> <charset> \n" +
                "\tindex_type: 填写值包括SFZ, MOBILE, NAME，分别表示身份证号码、手机号码和姓名\n" +
                "\ttext_input_file: 索引字段（idx）已排序，姓名已替换为编号的原始文本文件\n" +
                "\tidx: text_input_file中索引字段所在下标，该字段格式为'一级索引,二级索引,内容前缀'\n" +
                "\trange1: 一级索引表示，格式为M-N:size，即从索引字段如何生成一级索引。从下标M开始（包含）到下标N结束（包含）为一级索引,其大小为size个字节。" +
                "index_type=NAME时，可不填\n" +
                "\trange2: 二级索引表示，格式为M-N:size，即从索引字段如何生成二级索引。从下标M开始（包含）到下标N结束（包含）为二级索引,其大小为size个字节。" +
                "index_type=NAME时，可不填\n" +
                "\trange3: 内容前缀表示，格式为M-N:size，即从索引字段如何生成内容前缀。从下标M开始（包含）到下标N结束（包含）为内容前缀,其大小为size个字节。" +
                "index_type=NAME时，可不填\n" +
                "\tfield_num: 内容字段个数\n" +
                "\tfield_size: 内容字段大小表示，格式为Index:size，即内容第Index个字段，其大小为size个字节。" +
                "该配置项为多值，具体数量和内容中字段个数有关。index_type=NAME时，可不填\n" +
                "\tcharset: 原始文本文件的字符编码，可选值。默认为'utf8'");
    }

    private static List<Range> getRange(String... parameters) {
        List<Range> ranges = new ArrayList<>();
        for (String parameter : parameters) {
            String[] items = parameter.split("[:-]");
            int start = Integer.parseInt(items[0]);
            int end = Integer.parseInt(items[1]);
            int size = Integer.parseInt(items[2]);

            if (start < 0 || end <= start || size < 0 || size > 8 || size % 2 != 0) {
                throw new IllegalArgumentException(String.format("Bad Range. start=%d, end=%d, size=%d", start, end, size));
            }

            ranges.add(new Range(start, end, size));
        }


        return ranges;
    }

    private static Map<Integer, Integer> getContentFieldSize(String... parameters) {
        Map<Integer, Integer> contentFieldSize = new HashMap<>();
        for (String parameter : parameters) {
            String[] items = parameter.split(":");
            int index = Integer.parseInt(items[0]);
            int size = Integer.parseInt(items[1]);

            if (index < 0 || size < 0 || size > 8 || size % 2 != 0) {
                throw new IllegalArgumentException(String.format("Bad Field Size. index=%d, size=%d", index, size));
            }

            if (contentFieldSize.containsKey(index)) {
                throw new IllegalArgumentException("find duplicated field size config.");
            }

            contentFieldSize.put(index, size);
        }

        return contentFieldSize;
    }
}
