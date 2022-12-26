package ca.wise.fgm.tools;

import com.google.common.base.Strings;

public class DoubleHelper {

    public static boolean isDouble(String value) {
        if (Strings.isNullOrEmpty(value))
            return false;
        value = value.trim();
        for (int i = 0; i < value.length(); i++) {
            if (i == 0 && value.charAt(i) == '-') {
                if (value.length() == 1)
                    return false;
                continue;
            }
            else if (i > 0 &&  value.charAt(i) == '.')
                continue;
            if (Character.digit(value.charAt(i), 10) < 0)
                return false;
        }
        return true;
    }
}
