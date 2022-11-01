package com.ctrip.framework.apollo.configservice.controller;

import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.biz.service.NamespaceService;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.dto.NamespaceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @description: 从配置中心获取namespaces,如果该集群获取不到，取默认集群
 * @author: zhou shuai
 * @date: 2022/4/16 21:12
 * @version: v1
 */
@RestController
@RequestMapping("/namespaces")
public class NamespaceController {

    @Autowired
    private NamespaceService namespaceService;

    @GetMapping(value = "/{appId}/{clusterName}")
    public List<NamespaceDTO> getNameSpacesByAppIdAndCluster(@PathVariable("appId") String appId,
                                   @PathVariable("clusterName") String clusterName) {
        List<Namespace> groups = namespaceService.findNamespaces(appId, clusterName);
        if(CollectionUtils.isEmpty(groups)){
            groups = namespaceService.findNamespaces(appId, ConfigConsts.CLUSTER_NAME_DEFAULT);
        }
        return BeanUtils.batchTransform(NamespaceDTO.class, groups);
    }

}
