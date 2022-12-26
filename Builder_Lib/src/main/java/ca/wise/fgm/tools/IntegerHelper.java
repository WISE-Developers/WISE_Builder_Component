package ca.wise.fgm.tools;

import com.google.common.base.Strings;

public final class IntegerHelper {
    
    public int value;
    
    public IntegerHelper(int value) {
        this.value = value;
    }

    public static boolean isInteger(String value) {
        return isInteger(value, 10);
    }
    
    public static boolean isInteger(String value, int radix) {
        if (Strings.isNullOrEmpty(value))
            return false;
        value = value.trim();
        for (int i = 0; i < value.length(); i++) {
            if (i == 0 && value.charAt(i) == '-') {
                if (value.length() == 1)
                    return false;
                continue;
            }
            if (Character.digit(value.charAt(i), radix) < 0)
                return false;
        }
        return true;
    }
}
