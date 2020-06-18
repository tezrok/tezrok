package io.tezrok.spring.relation;

import org.apache.commons.lang3.Validate;

/**
 * Created by ruslan on 02.04.2016.
 */
public class JoinTable {
    private final String name;
    private final JoinColumn joinColumn;
    private final JoinColumn inverseJoinColumn;

    public JoinTable(String name, JoinColumn joinColumn, JoinColumn inverseJoinColumn) {
        this.name = Validate.notNull(name, "name");
        this.joinColumn = Validate.notNull(joinColumn, "joinColumn");
        this.inverseJoinColumn = Validate.notNull(inverseJoinColumn, "inverseJoinColumn");
    }

    public String getName() {
        return name;
    }

    public JoinColumn getJoinColumn() {
        return joinColumn;
    }

    public JoinColumn getInverseJoinColumn() {
        return inverseJoinColumn;
    }

    @Override
    public String toString() {
        return "JoinTable{" +
                "name='" + name + '\'' +
                ", joinColumn=" + joinColumn +
                ", inverseJoinColumn=" + inverseJoinColumn +
                '}';
    }
}
