package com.metoo.nspm.core.config.shiro;

import org.apache.shiro.authc.UsernamePasswordToken;

// 免密登录
public class EasyTypeToken extends UsernamePasswordToken {

    private static final long serialVersionUID = -2564928913725078138L;

    private LoginType type;

    public EasyTypeToken() {
        super();
    }

    /**
     * 免密登录
     */
    public EasyTypeToken(String username) {
        super(username, "", false, null);
        this.type = LoginType.NOPASSWD;
    }

    /**
     * 账号密码登录
     */
    public EasyTypeToken(String username, String password, boolean rememberMe) {
        super(username, password, rememberMe, null);
        this.type = LoginType.PASSWORD;
    }

    public LoginType getType() {
        return type;
    }

    public void setType(LoginType type) {
        this.type = type;
    }
}
