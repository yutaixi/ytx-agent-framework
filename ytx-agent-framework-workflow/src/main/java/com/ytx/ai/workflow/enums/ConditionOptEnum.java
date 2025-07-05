package com.ytx.ai.workflow.enums;

public enum ConditionOptEnum {

    AND("and"),
    OR("or"),
    EQUAL("="),
    NOT_EQUAL("!="),
    LEN_GT("len.>"),
    LEN_GT_OR_EQUAL("len.>="),
    LEN_LT("len.<"),
    LEN_LT_OR_EQUAL("len.<="),
    CONTAINS("contains"),
    NOT_CONTAIN("!contains"),
    EMPTY("empty"),
    NOT_EMPTY("!empty"),
    OTHERWISE("else");

    private final String opt;

    private ConditionOptEnum(String opt) {
        this.opt = opt;
    }

    public String getOpt() {
        return this.opt;
    }

    public static ConditionOptEnum of(String opt) {
        ConditionOptEnum target = null;
        for (ConditionOptEnum condition : ConditionOptEnum.values()) {
            if (condition.getOpt().equalsIgnoreCase(opt)) {
                target = condition;
                break;
            }
        }
        return target;
    }
}

