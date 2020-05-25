package io.tezrok.api.builder.type;

import io.tezrok.api.builder.JavaUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic type.
 */
public abstract class Type {
    private final String name;
    private final String packagePath;

    protected Type(String name, String packagePath) {
        this.name = Validate.notBlank(name, "name");
        this.packagePath = Validate.notNull(packagePath, "packagePath");
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        if (StringUtils.isBlank(packagePath)) {
            return name;
        }

        return packagePath + "." + name;
    }

    public String getPackage() {
        return packagePath;
    }

    public boolean isGeneric() {
        return false;
    }

    public boolean isEnum() {
        return false;
    }

    public boolean isPrimitive() {
        return false;
    }

    public boolean isEntity() {
        return false;
    }

    public boolean isDto() {
        return false;
    }

    /**
     * Returns imports for this type.
     */
    public List<String> getImports() {
        List<String> result = new ArrayList<>();

        result.add(getFullName());

        return result;
    }

    public String toVarName() {
        return JavaUtil.toVarName(name);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Type type1 = (Type) o;

        if (!getFullName().equals(type1.getFullName())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getFullName().hashCode();
    }

    public <T extends Type> T as() {
        return (T) this;
    }
}
