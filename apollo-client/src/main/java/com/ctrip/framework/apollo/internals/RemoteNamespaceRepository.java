package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.dto.NamespaceDTO;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.util.http.HttpClient;
import com.ctrip.framework.apollo.util.http.HttpRequest;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: http调用获取appId-cluster下的 namespaces
 * @author: zhou shuai
 * @date: 2022/4/16 19:12
 * @version: v1
 */
public class RemoteNamespaceRepository {

    private static final Logger logger = LoggerFactory.getLogger(RemoteNamespaceRepository.class);
    private ConfigServiceLocator m_serviceLocator = ApolloInjector.getInstance(ConfigServiceLocator.class);
    private HttpClient m_httpClient = ApolloInjector.getInstance(HttpClient.class);
    private static final Escaper pathEscaper = UrlEscapers.urlPathSegmentEscaper();
    private static final Joiner.MapJoiner MAP_JOINER = Joiner.on("&").withKeyValueSeparator("=");


    public List<NamespaceDTO> getNamespacesByAppIdAndClusterName(final String appId, final String clusterName) {
        RemoteNamespaceRepository.logger.info("load namespaces from config service...");
        final String path = "namespaces/%s/%s";
        List<String> pathParams =
                Lists.newArrayList(pathEscaper.escape(appId), pathEscaper.escape(clusterName));
        String pathExpanded = String.format(path, pathParams.toArray());
        Map<String, String> queryParams = Maps.newHashMap();
        if (!queryParams.isEmpty()) {
            pathExpanded += "?" + MAP_JOINER.join(queryParams);
        }
        Map<Integer, String> namespacesUrls = getNamespacesUrl(pathExpanded);
        int maxRetries = namespacesUrls.size();
        for (int i = 0; i < maxRetries; i++) {
            String namespacesUrl = namespacesUrls.get(i);
            try {
                if (!StringUtils.isBlank(namespacesUrl)) {
                    return Arrays.asList(m_httpClient.doGet(new HttpRequest(namespacesUrl), NamespaceDTO[].class).getBody());
                }
            } catch (Exception e) {
                logger.warn("load namespaces from config service error,url:{}", namespacesUrl);
            }
        }
        return new ArrayList<>();
    }

    private Map<Integer, String> getNamespacesUrl(String pathExpanded) {
        List<String> namespacesUrls = this.getConfigServices().stream().map((serviceDTO) -> {
            String homepageUrl = serviceDTO.getHomepageUrl();
            if (!homepageUrl.endsWith("/")) {
                homepageUrl += "/";
            }
            return homepageUrl + pathExpanded;
        }).collect(Collectors.toList());
        Map<Integer, String> namespaceUrlMap = new HashMap();
        for(int i = 0; i < namespacesUrls.size(); ++i) {
            namespaceUrlMap.put(i, namespacesUrls.get(i));
        }
        return namespaceUrlMap;
    }

    private List<ServiceDTO> getConfigServices() {
        final List<ServiceDTO> services = this.m_serviceLocator.getConfigServices();
        if (services.size() == 0) {
            throw new ApolloConfigException("No available config service");
        }
        return services;
    }

}
