package io.tezrok.api.builder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public class JavaUtil {
    public static String toVarName(String type){
        return StringUtils.uncapitalize(Validate.notBlank(type));
    }
}
