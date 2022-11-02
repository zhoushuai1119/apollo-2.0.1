package com.ctrip.framework.apollo.demo.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * redisson配置类
 *
 * @author zhoushuai
 * @date 2021-05-29
 */
@Component
@ConfigurationProperties(prefix = "apollo.properties.test")
public class ApolloPropertiesTest {

    private String aa;
    private String bb;
    private String cc;

    public String getAa() {
        return aa;
    }

    public void setAa(String aa) {
        this.aa = aa;
    }

    public String getBb() {
        return bb;
    }

    public void setBb(String bb) {
        this.bb = bb;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

}
