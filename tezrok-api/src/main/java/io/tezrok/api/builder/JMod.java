package io.tezrok.api.builder;

public class JMod {
    private int value;

    public final static int EMPTY = 0x0;

    public final static int PUBLIC = 0x0001;

    public final static int PRIVATE = 0x0002;

    public final static int PROTECTED = 0x0004;

    public final static int ABSTRACT = 0x0008;

    public final static int STATIC = 0x0010;

    public static final int FINAL = 0x0020;

    /**
     * Means create get method for field. And get method for a method.
     */
    public final static int GET = 0x0100;

    /**
     * Means create set method for field. And set method for a method.
     */
    public final static int SET = 0x0200;

    public final static int GETSET = GET | SET;

    public final static int USEEQUALS = 0x010000;

    public final static int INTERFACE = 0x020000;

    /**
     * If method is constructor.
     */
    public final static int CONSTRUCTOR = 0x040000;

    private JMod(int value) {
        this.value = value;
    }

    public boolean isPublic() {
        return hasFlag(PUBLIC);
    }

    public boolean isPrivate() {
        return hasFlag(PRIVATE);
    }

    public boolean isProtected() {
        return hasFlag(PROTECTED);
    }

    public boolean isGet() {
        return hasFlag(GET);
    }

    public boolean isSet() {
        return hasFlag(SET);
    }

    public boolean isGetSet() {
        return hasFlag(GETSET);
    }

    public boolean isInterface() {
        return hasFlag(INTERFACE);
    }

    public boolean isAbstract() {
        return hasFlag(ABSTRACT);
    }

    public boolean isUsedInEquals() {
        return hasFlag(USEEQUALS);
    }

    public boolean isConstructor() {
        return hasFlag(CONSTRUCTOR);
    }

    public boolean isStatic() {
        return hasFlag(STATIC);
    }

    public boolean isFinal() {
        return hasFlag(FINAL);
    }

    public boolean hasFlag(int flag) {
        return (value & flag) == flag;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value){
        this.value = value;
    }

    public static JMod valueOf(int mod) {
        return new JMod(mod);
    }

    public void makeAbstract() {
        this.value |= JMod.ABSTRACT;
    }
}
