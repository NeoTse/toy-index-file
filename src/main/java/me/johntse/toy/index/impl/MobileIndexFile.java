package me.johntse.toy.index.impl;

import me.johntse.toy.index.common.Range;
import me.johntse.toy.index.common.SecondaryFixedLengthIndexFile;
import me.johntse.toy.index.util.CertNumUtils;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

/**
 * 手机号码索引。
 *
 * @author John Tse
 */
public class MobileIndexFile extends SecondaryFixedLengthIndexFile {
    public MobileIndexFile(int idx, Range first, Range second, Range left, String charset,
                           Map<Integer, Integer> fieldsLength) {
        super(idx, first, second, left, charset, fieldsLength);
    }

    @Override
    protected long writeContent(DataOutput output, String item, int idx) throws IOException {
        return super.writeContent(output, CertNumUtils.removeVerificationCode(item), idx);
    }
}
