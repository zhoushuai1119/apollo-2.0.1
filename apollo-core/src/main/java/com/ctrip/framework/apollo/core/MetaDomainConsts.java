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
package com.ctrip.framework.apollo.core;

import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.utils.ResourceUtils;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Properties;

/**
 * The meta domain will try to load the meta server address from MetaServerProviders, the default
 * ones are:
 *
 * <ul>
 * <li>com.ctrip.framework.apollo.core.internals.LegacyMetaServerProvider</li>
 * </ul>
 * <p>
 * If no provider could provide the meta server url, the default meta url will be used(http://apollo.meta).
 * <br />
 * <p>
 * 3rd party MetaServerProvider could be injected by typical Java Service Loader pattern.
 *
 * @see com.ctrip.framework.apollo.core.internals.LegacyMetaServerProvider
 */
public class MetaDomainConsts {

    private static final Map<Env, String> domains = Maps.newConcurrentMap();

    public static final String DEFAULT_META_URL = "http://apollo.meta";

    /**
     * Return one meta server address. If multiple meta server addresses are configured, will select
     * one.
     */
    public static String getDomain(Env env) {
        return String.valueOf(domains.get(env));
    }


    static {
        Properties prop = new Properties();
        prop = ResourceUtils.readConfigFile("apollo-env.properties", prop);
        final Properties env = System.getProperties();
        domains.put(Env.LOCAL, env.getProperty("local_meta", prop.getProperty("local.meta", DEFAULT_META_URL)));
        domains.put(Env.DEV, env.getProperty("dev_meta", prop.getProperty("dev.meta", DEFAULT_META_URL)));
        domains.put(Env.FAT, env.getProperty("fat_meta", prop.getProperty("fat.meta", DEFAULT_META_URL)));
        domains.put(Env.UAT, env.getProperty("uat_meta", prop.getProperty("uat.meta", DEFAULT_META_URL)));
        domains.put(Env.LPT, env.getProperty("lpt_meta", prop.getProperty("lpt.meta", DEFAULT_META_URL)));
        domains.put(Env.PRO, env.getProperty("pro_meta", prop.getProperty("pro.meta", DEFAULT_META_URL)));
    }

}
