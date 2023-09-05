package com.metoo.nspm.core.config.shiro;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;

// 免密登录
public class EasyCredentialsMatch extends HashedCredentialsMatcher {

    /**
     * 重写方法
     * 区分 密码和非密码登录
     * 此次无需记录登录次数 详情看SysPasswordService
     */
    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {

        EasyTypeToken easyUsernameToken = (EasyTypeToken) token;

        //免密登录,不验证密码
        if (LoginType.NOPASSWD.equals(easyUsernameToken.getType())) {

            return true;
        }

        //密码登录
        Object tokenHashedCredentials = hashProvidedCredentials(token, info);
        Object accountCredentials = getCredentials(info);
        return equals(tokenHashedCredentials, accountCredentials);
    }
}
