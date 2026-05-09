package org.sxk.store.enums;

public enum MessageStatus {
    SENT(1, "已发送"),
    CONSUMED_SUCCESS(2, "消费成功"),
    CONSUMED_FAILED(3, "消费失败");

    private final int code;
    private final String desc;

    MessageStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static MessageStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MessageStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}