package com.ctrip.framework.apollo.demo.controller;

import com.ctrip.framework.apollo.demo.properties.ApolloPropertiesTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description:
 * @author: zhou shuai
 * @date: 2022/11/2 10:35
 * @version: v1
 */
@RestController
@RequestMapping("demo")
public class DemoController {

    @Value("${name_1}")
    private String nameOne;

    @Value("${name_2}")
    private String nameTwo;

    @Autowired
    private ApolloPropertiesTest apolloPropertiesTest;

    /**
     * apolloPropertiesTest
     *
     * @return
     */
    @GetMapping("/apollo/properties/test")
    public ApolloPropertiesTest apolloPropertiesTest() {
        return apolloPropertiesTest;
    }

    /**
     * apollo refresh Test
     *
     * @return
     */
    @GetMapping("/apollo/refresh/test")
    public String apolloRefreshTest() {
        return nameOne + "****" + nameTwo;
    }

}
