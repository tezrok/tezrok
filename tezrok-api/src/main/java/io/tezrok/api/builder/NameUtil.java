package io.tezrok.api.builder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.regex.Pattern;

public final class NameUtil {
    private final static Pattern GROUP_ALLOWED = Pattern.compile("^[\\w/]+$");
    private final static Pattern NAME_ALLOWED = Pattern.compile("^[a-zA-Z_]\\w*$");

    private NameUtil() {
    }


    /**
     * Validates group's name and throws an exception if name is invalid.
     *
     * @param group Group name.
     * @return The same value as in <code>group</code>.
     */
    public static String validateGroup(String group) {
        if (StringUtils.isBlank(group)) {
            return group;
        }

        if (!GROUP_ALLOWED.matcher(group).matches()) {
            throw new RuntimeException(String.format("Group name is invalid '%s'", group));
        }

        return group;
    }

    public static boolean isValidName(String name) {
        return NAME_ALLOWED.matcher(name).matches();
    }

    public static String validate(String name) {
        Validate.notBlank(name, "name");

        if (isValidName(name)) {
            return name;
        }

        throw new IllegalArgumentException(String.format("Name is invalid: '%s'", name));
    }
}
