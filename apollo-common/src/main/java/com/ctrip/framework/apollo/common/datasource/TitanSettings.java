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
package com.ctrip.framework.apollo.common.datasource;

import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.enums.EnvUtils;
import com.ctrip.framework.foundation.Foundation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TitanSettings {

    @Value("${dev.titan.url:}")
    private String devTitanUrl;

    @Value("${pre.titan.url:}")
    private String preTitanUrl;

    @Value("${yc.titan.url:}")
    private String ycTitanUrl;

    @Value("${gr.titan.url:}")
    private String grTitanUrl;

    @Value("${pro.titan.url:}")
    private String proTitanUrl;

    @Value("${dev.titan.dbname:}")
    private String devTitanDbname;

    @Value("${pre.titan.dbname:}")
    private String preTitanDbname;

    @Value("${yc.titan.dbname:}")
    private String ycTitanDbname;

    @Value("${gr.titan.dbname:}")
    private String grTitanDbname;

    @Value("${pro.titan.dbname:}")
    private String proTitanDbname;

    public String getTitanUrl() {
        Env env = EnvUtils.transformEnv(Foundation.server().getEnvType());
        switch (env) {
            case DEV:
                return devTitanUrl;
            case PRE:
                return preTitanUrl;
            case YC:
                return ycTitanUrl;
            case GR:
                return grTitanUrl;
            case PRO:
                return proTitanUrl;
            default:
                return "";
        }
    }

    public String getTitanDbname() {
        Env env = EnvUtils.transformEnv(Foundation.server().getEnvType());
        switch (env) {
            case DEV:
                return devTitanDbname;
            case PRE:
                return preTitanDbname;
            case YC:
                return ycTitanDbname;
            case GR:
                return grTitanDbname;
            case PRO:
                return proTitanDbname;
            default:
                return "";
        }
    }

}
