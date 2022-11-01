package com.ctrip.framework.apollo.core.dto;

/**
 * @description:
 * @author: zhou shuai
 * @date: 2022/4/16 19:13
 * @version: v1
 */
public class NamespaceDTO {

    private long id;
    private String appId;
    private String clusterName;
    private String namespaceName;

    public NamespaceDTO() {
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAppId() {
        return this.appId;
    }

    public String getClusterName() {
        return this.clusterName;
    }

    public String getNamespaceName() {
        return this.namespaceName;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setNamespaceName(String namespaceName) {
        this.namespaceName = namespaceName;
    }

}
