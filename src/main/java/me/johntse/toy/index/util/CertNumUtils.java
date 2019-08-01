package me.johntse.toy.index.util;

/**
 * 证件号码处理功能类.
 *
 * @author John Tse
 */
public final class CertNumUtils {
    public static String addVerificationCode(long certNum) {
        return addVerificationCode(String.valueOf(certNum));
    }

    public static String addVerificationCode(String certNum) {
        if (certNum.length() != 15 && certNum.length() != 17) {
            return certNum;
        }

        char[] ch = new char[18];
        char[] old = certNum.toCharArray();
        if (certNum.length() == 15) {
            System.arraycopy(old, 0, ch, 0, 6);
            ch[6] = '1';
            ch[7] = '9';
            System.arraycopy(old, 6, ch, 8, 9);
        } else {
            System.arraycopy(old, 0, ch, 0, 17);
        }

        int sum = 0;
        for (int i = 0; i < ch.length - 1; i++) {
            sum += ((1 << (17 - i)) % 11) * (ch[i] - '0');
        }

        int code = (12 - (sum % 11)) % 11;
        ch[17] = code < 10 ? (char) ('0' + code) : 'X';

        return new String(ch);
    }

    public static String removeVerificationCode(String certNum) {
        if (certNum.length() == 18) {
            return certNum.substring(0, 17);
        } else {
            return certNum;
        }
    }
}
