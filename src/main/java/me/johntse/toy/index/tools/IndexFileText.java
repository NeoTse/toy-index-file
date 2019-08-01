package me.johntse.toy.index.tools;

import me.johntse.toy.index.common.SecondaryFixedLengthIndexFile;
import me.johntse.toy.index.common.VariableLengthIndexFile;
import me.johntse.toy.index.impl.IndexType;

import java.io.File;
import java.io.IOException;

/**
 * 索引文件查看，即将二进制文件转换为文本文件以供查看。
 *
 * @author John Tse
 */
public class IndexFileText {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            usage();
            return;
        }

        IndexType type = IndexType.valueOf(args[0]);
        File indexFile = new File(args[1]);
        if (!indexFile.canRead()) {
            throw new IllegalArgumentException("Can't read index file: " + indexFile);
        }

        System.out.println(String.format("start text %s index: %s", type, indexFile));
        File outputFile;
        switch (type) {
            case NAME:
                VariableLengthIndexFile variableLengthIndexFile = new VariableLengthIndexFile();
                outputFile = variableLengthIndexFile.asText(indexFile);
                break;
            case SFZ:
            case MOBILE:
                SecondaryFixedLengthIndexFile fixedLengthIndexFile = new SecondaryFixedLengthIndexFile();
                outputFile = fixedLengthIndexFile.asText(indexFile);
                break;
            default:
                throw new IllegalArgumentException("unknown test type");
        }

        System.out.println(String.format("finish text %s index. text file %s", type, outputFile));
    }

    private static void usage() {
        System.out.println("将二进制格式的索引文件文本化输出。\n[Usage] IndexFileText [index_type] [index_file]\n" +
                "\tindex_type: 填写值包括SFZ, MOBILE, NAME，分别表示身份证号码、手机号码和姓名索引。\n" +
                "\tindex_file: 需要文本化的索引文件。\n");
    }
}
