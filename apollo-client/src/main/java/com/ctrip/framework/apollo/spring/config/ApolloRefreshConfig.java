package com.ctrip.framework.apollo.spring.config;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @description:ConfigurationProperties配置实现自动更新
 * ConfigurationProperties如果需要在Apollo配置变化时自动更新注入的值，需要配合使用EnvironmentChangeEvent或RefreshScope
 * @author: zhou shuai
 * @date: 2022/4/15 12:47
 * @version: v1
 */
public class ApolloRefreshConfig implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(ApolloRefreshConfig.class);

    private ApplicationContext applicationContext;
    private ConfigUtil m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);

    public ApolloRefreshConfig() {
    }

    @ApolloConfigChangeListener
    public void onChange(ConfigChangeEvent changeEvent) {
        if (this.m_configUtil.isAutoUpdateInjectedSpringPropertiesEnabled()) {
            changeEvent.changedKeys().stream().map(changeEvent::getChange).forEach(change -> {
                logger.info("found config change - namespace: {},  key: {}, oldValue: {}, newValue: {}, changeType: {}",
                        change.getNamespace(), change.getPropertyName(), change.getOldValue(),
                        change.getNewValue(), change.getChangeType());
            });
            //更新相应的bean的属性值，主要是存在@ConfigurationProperties注解的bean
            this.applicationContext.publishEvent(new EnvironmentChangeEvent(changeEvent.changedKeys()));
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
