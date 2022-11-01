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
package com.ctrip.framework.apollo.spring.annotation;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.dto.NamespaceDTO;
import com.ctrip.framework.apollo.internals.RemoteNamespaceRepository;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.property.PlaceholderHelper;
import com.ctrip.framework.apollo.spring.property.SpringValue;
import com.ctrip.framework.apollo.spring.property.SpringValueRegistry;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Apollo Annotation Processor for Spring Application
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class ApolloAnnotationProcessor extends ApolloProcessor implements BeanFactoryAware,
        EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(ApolloAnnotationProcessor.class);
    private static final Gson GSON = new Gson();

    private final ConfigUtil configUtil;
    private final PlaceholderHelper placeholderHelper;
    private final SpringValueRegistry springValueRegistry;
    private final RemoteNamespaceRepository remoteNamespaceRepository = ApolloInjector.getInstance(RemoteNamespaceRepository.class);
    private ConfigUtil m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);

    /**
     * resolve the expression.
     */
    private ConfigurableBeanFactory configurableBeanFactory;

    private Environment environment;

    public ApolloAnnotationProcessor() {
        configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        placeholderHelper = SpringInjector.getInstance(PlaceholderHelper.class);
        springValueRegistry = SpringInjector.getInstance(SpringValueRegistry.class);
    }

    @Override
    protected void processField(Object bean, String beanName, Field field) {
        this.processApolloConfig(bean, field);
        this.processApolloJsonValue(bean, beanName, field);
    }

    @Override
    protected void processMethod(final Object bean, String beanName, final Method method) {
        this.processApolloConfigChangeListener(bean, method);
        this.processApolloJsonValue(bean, beanName, method);
    }

    private void processApolloConfig(Object bean, Field field) {
        ApolloConfig annotation = AnnotationUtils.getAnnotation(field, ApolloConfig.class);
        if (annotation == null) {
            return;
        }

        Preconditions.checkArgument(Config.class.isAssignableFrom(field.getType()),
                "Invalid type: %s for field: %s, should be Config", field.getType(), field);

        final String namespace = annotation.value();
        final String resolvedNamespace = this.environment.resolveRequiredPlaceholders(namespace);
        Config config = ConfigService.getConfig(resolvedNamespace);

        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, bean, config);
    }

    private void processApolloConfigChangeListener(final Object bean, final Method method) {
        ApolloConfigChangeListener annotation = AnnotationUtils
                .findAnnotation(method, ApolloConfigChangeListener.class);
        if (annotation == null) {
            return;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        Preconditions.checkArgument(parameterTypes.length == 1,
                "Invalid number of parameters: %s for method: %s, should be 1", parameterTypes.length,
                method);
        Preconditions.checkArgument(ConfigChangeEvent.class.isAssignableFrom(parameterTypes[0]),
                "Invalid parameter type: %s for method: %s, should be ConfigChangeEvent", parameterTypes[0],
                method);

        ReflectionUtils.makeAccessible(method);

        //获取ApolloConfigChangeListener注解的namespace
        Set<String> namespaceList = Arrays.stream(annotation.value()).collect(Collectors.toSet());

        try {
            List<NamespaceDTO> namespaceDTOs = this.remoteNamespaceRepository.getNamespacesByAppIdAndClusterName(this.m_configUtil.getAppId(), this.m_configUtil.getCluster());
            namespaceDTOs.forEach((namespaceDTO) -> {
                namespaceList.add(namespaceDTO.getNamespaceName());
            });
        } catch (Exception e) {
            logger.error("get namespaces by appId from config service error:", e);
        }

        String[] annotatedInterestedKeys = annotation.interestedKeys();
        String[] annotatedInterestedKeyPrefixes = annotation.interestedKeyPrefixes();
        ConfigChangeListener configChangeListener = new ConfigChangeListener() {
            @Override
            public void onChange(ConfigChangeEvent changeEvent) {
                ReflectionUtils.invokeMethod(method, bean, changeEvent);
            }
        };

        Set<String> interestedKeys =
                annotatedInterestedKeys.length > 0 ? Sets.newHashSet(annotatedInterestedKeys) : null;
        Set<String> interestedKeyPrefixes =
                annotatedInterestedKeyPrefixes.length > 0 ? Sets.newHashSet(annotatedInterestedKeyPrefixes)
                        : null;

        namespaceList.forEach(namespace -> {
            final String resolvedNamespace = this.environment.resolveRequiredPlaceholders(namespace);
            Config config = ConfigService.getConfig(resolvedNamespace);

            if (interestedKeys == null && interestedKeyPrefixes == null) {
                config.addChangeListener(configChangeListener);
            } else {
                config.addChangeListener(configChangeListener, interestedKeys, interestedKeyPrefixes);
            }
        });
    }


    private void processApolloJsonValue(Object bean, String beanName, Field field) {
        ApolloJsonValue apolloJsonValue = AnnotationUtils.getAnnotation(field, ApolloJsonValue.class);
        if (apolloJsonValue == null) {
            return;
        }
        String placeholder = apolloJsonValue.value();
        Object propertyValue = placeholderHelper
                .resolvePropertyValue(this.configurableBeanFactory, beanName, placeholder);

        // propertyValue will never be null, as @ApolloJsonValue will not allow that
        if (!(propertyValue instanceof String)) {
            return;
        }

        boolean accessible = field.isAccessible();
        field.setAccessible(true);
        ReflectionUtils
                .setField(field, bean, parseJsonValue((String) propertyValue, field.getGenericType()));
        field.setAccessible(accessible);

        if (configUtil.isAutoUpdateInjectedSpringPropertiesEnabled()) {
            Set<String> keys = placeholderHelper.extractPlaceholderKeys(placeholder);
            for (String key : keys) {
                SpringValue springValue = new SpringValue(key, placeholder, bean, beanName, field, true);
                springValueRegistry.register(this.configurableBeanFactory, key, springValue);
                logger.debug("Monitoring {}", springValue);
            }
        }
    }

    private void processApolloJsonValue(Object bean, String beanName, Method method) {
        ApolloJsonValue apolloJsonValue = AnnotationUtils.getAnnotation(method, ApolloJsonValue.class);
        if (apolloJsonValue == null) {
            return;
        }
        String placeHolder = apolloJsonValue.value();

        Object propertyValue = placeholderHelper
                .resolvePropertyValue(this.configurableBeanFactory, beanName, placeHolder);

        // propertyValue will never be null, as @ApolloJsonValue will not allow that
        if (!(propertyValue instanceof String)) {
            return;
        }

        Type[] types = method.getGenericParameterTypes();
        Preconditions.checkArgument(types.length == 1,
                "Ignore @Value setter {}.{}, expecting 1 parameter, actual {} parameters",
                bean.getClass().getName(), method.getName(), method.getParameterTypes().length);

        boolean accessible = method.isAccessible();
        method.setAccessible(true);
        ReflectionUtils.invokeMethod(method, bean, parseJsonValue((String) propertyValue, types[0]));
        method.setAccessible(accessible);

        if (configUtil.isAutoUpdateInjectedSpringPropertiesEnabled()) {
            Set<String> keys = placeholderHelper.extractPlaceholderKeys(placeHolder);
            for (String key : keys) {
                SpringValue springValue = new SpringValue(key, apolloJsonValue.value(), bean, beanName,
                        method, true);
                springValueRegistry.register(this.configurableBeanFactory, key, springValue);
                logger.debug("Monitoring {}", springValue);
            }
        }
    }

    private Object parseJsonValue(String json, Type targetType) {
        try {
            return GSON.fromJson(json, targetType);
        } catch (Throwable ex) {
            logger.error("Parsing json '{}' to type {} failed!", json, targetType, ex);
            throw ex;
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.configurableBeanFactory = (ConfigurableBeanFactory) beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
