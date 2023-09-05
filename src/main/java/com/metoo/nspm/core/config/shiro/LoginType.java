package com.metoo.nspm.core.config.shiro;

// 免密登录
public enum LoginType {

    /** 密码登录 */
    PASSWORD("PASSWORD"),
    /** 密码登录 */
    NOPASSWD("NOPASSWORD");
    /** 状态值 */
    private String code;
    private LoginType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
