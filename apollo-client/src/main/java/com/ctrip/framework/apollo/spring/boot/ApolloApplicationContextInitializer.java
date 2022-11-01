/*
 * Copyright 2022 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.spring.boot;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.dto.NamespaceDTO;
import com.ctrip.framework.apollo.core.utils.DeferredLogger;
import com.ctrip.framework.apollo.internals.RemoteNamespaceRepository;
import com.ctrip.framework.apollo.spring.config.CachedCompositePropertySource;
import com.ctrip.framework.apollo.spring.config.ConfigPropertySourceFactory;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Initialize apollo system properties and inject the Apollo config in Spring Boot bootstrap phase
 *
 * <p>Configuration example:</p>
 * <pre class="code">
 *   # set app.id
 *   app.id = 100004458
 *   # enable apollo bootstrap config and inject 'application' namespace in bootstrap phase
 *   apollo.bootstrap.enabled = true
 * </pre>
 * <p>
 * or
 *
 * <pre class="code">
 *   # set app.id
 *   app.id = 100004458
 *   # enable apollo bootstrap config
 *   apollo.bootstrap.enabled = true
 *   # will inject 'application' and 'FX.apollo' namespaces in bootstrap phase
 *   apollo.bootstrap.namespaces = application,FX.apollo
 * </pre>
 * <p>
 * <p>
 * If you want to load Apollo configurations even before Logging System Initialization Phase,
 * add
 * <pre class="code">
 *   # set apollo.bootstrap.eagerLoad.enabled
 *   apollo.bootstrap.eagerLoad.enabled = true
 * </pre>
 * <p>
 * This would be very helpful when your logging configurations is set by Apollo.
 * <p>
 * for example, you have defined logback-spring.xml in your project, and you want to inject some attributes into logback-spring.xml.
 */
public class ApolloApplicationContextInitializer implements
        ApplicationContextInitializer<ConfigurableApplicationContext>, EnvironmentPostProcessor, Ordered {
    public static final int DEFAULT_ORDER = 0;

    private static final Logger logger = LoggerFactory.getLogger(ApolloApplicationContextInitializer.class);
    private final RemoteNamespaceRepository remoteNamespaceRepository = ApolloInjector.getInstance(RemoteNamespaceRepository.class);
    private ConfigUtil m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
    private static final Splitter NAMESPACE_SPLITTER = Splitter.on(",").omitEmptyStrings()
            .trimResults();
    public static final String[] APOLLO_SYSTEM_PROPERTIES = {ApolloClientSystemConsts.APP_ID,
            ApolloClientSystemConsts.APOLLO_LABEL,
            ApolloClientSystemConsts.APOLLO_CLUSTER,
            ApolloClientSystemConsts.APOLLO_CACHE_DIR,
            ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET,
            ApolloClientSystemConsts.APOLLO_META,
            ApolloClientSystemConsts.APOLLO_CONFIG_SERVICE,
            ApolloClientSystemConsts.APOLLO_PROPERTY_ORDER_ENABLE,
            ApolloClientSystemConsts.APOLLO_PROPERTY_NAMES_CACHE_ENABLE};

    private final ConfigPropertySourceFactory configPropertySourceFactory = SpringInjector
            .getInstance(ConfigPropertySourceFactory.class);

    private int order = DEFAULT_ORDER;

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();

        /**
         * 未设置默认修改为true
         */
        if (!environment.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, Boolean.class, true)) {
            logger.debug("Apollo bootstrap config is not enabled for context {}, see property: ${{}}", context, PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);
            return;
        }
        logger.debug("Apollo bootstrap config is enabled for context {}", context);

        initialize(environment);
    }


    /**
     * Initialize Apollo Configurations Just after environment is ready.
     *
     * @param environment
     */
    protected void initialize(ConfigurableEnvironment environment) {

        if (environment.getPropertySources().contains(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
            //already initialized, replay the logs that were printed before the logging system was initialized
            DeferredLogger.replayTo();
            return;
        }

        String namespaces = environment.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES, ConfigConsts.NAMESPACE_APPLICATION);
        logger.debug("Apollo bootstrap namespaces: {}", namespaces);
        //List<String> namespaceList = NAMESPACE_SPLITTER.splitToList(namespaces);
        final Set<String> namespaceList = NAMESPACE_SPLITTER.splitToList(namespaces).stream().collect(Collectors.toSet());

        try {
            List<NamespaceDTO> namespaceDTOs = this.remoteNamespaceRepository.getNamespacesByAppIdAndClusterName(this.m_configUtil.getAppId(), this.m_configUtil.getCluster());
            namespaceDTOs.forEach((namespaceDTO) -> {
                namespaceList.add(namespaceDTO.getNamespaceName());
            });
        } catch (Exception e) {
            logger.error("get namespaces by appId from config service error:", e);
        }

        CompositePropertySource composite;
        final ConfigUtil configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        if (configUtil.isPropertyNamesCacheEnabled()) {
            composite = new CachedCompositePropertySource(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME);
        } else {
            composite = new CompositePropertySource(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME);
        }
        for (String namespace : namespaceList) {
            Config config = ConfigService.getConfig(namespace);

            composite.addPropertySource(configPropertySourceFactory.getConfigPropertySource(namespace, config));
        }

        environment.getPropertySources().addFirst(composite);
    }

    /**
     * To fill system properties from environment config
     */
    void initializeSystemProperty(ConfigurableEnvironment environment) {
        for (String propertyName : APOLLO_SYSTEM_PROPERTIES) {
            fillSystemPropertyFromEnvironment(environment, propertyName);
        }
    }

    private void fillSystemPropertyFromEnvironment(ConfigurableEnvironment environment, String propertyName) {
        if (System.getProperty(propertyName) != null) {
            return;
        }

        String propertyValue = environment.getProperty(propertyName);

        if (Strings.isNullOrEmpty(propertyValue)) {
            return;
        }

        System.setProperty(propertyName, propertyValue);
    }

    /**
     * In order to load Apollo configurations as early as even before Spring loading logging system phase,
     * this EnvironmentPostProcessor can be called Just After ConfigFileApplicationListener has succeeded.
     * <p>
     * <br />
     * The processing sequence would be like this: <br />
     * Load Bootstrap properties and application properties -----> load Apollo configuration properties ----> Initialize Logging systems
     *
     * @param configurableEnvironment
     * @param springApplication
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment configurableEnvironment, SpringApplication springApplication) {

        // should always initialize system properties like app.id in the first place
        initializeSystemProperty(configurableEnvironment);

        /**
         * 默认设置将Apollo配置加载提到初始化日志系统之前
         */
        Boolean eagerLoadEnabled = configurableEnvironment.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED, Boolean.class, true);

        //EnvironmentPostProcessor should not be triggered if you don't want Apollo Loading before Logging System Initialization
        if (!eagerLoadEnabled) {
            return;
        }

        /**
         * 默认设置apollo.bootstrap.enabled为true
         */
        Boolean bootstrapEnabled = configurableEnvironment.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, Boolean.class, true);

        if (bootstrapEnabled) {
            DeferredLogger.enable();
            initialize(configurableEnvironment);
        }

    }

    /**
     * @since 1.3.0
     */
    @Override
    public int getOrder() {
        return order;
    }

    /**
     * @since 1.3.0
     */
    public void setOrder(int order) {
        this.order = order;
    }
}
