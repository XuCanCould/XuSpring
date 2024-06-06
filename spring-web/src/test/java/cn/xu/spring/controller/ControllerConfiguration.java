package cn.xu.spring.controller;

import cn.xu.spring.annotation.Configuration;
import cn.xu.spring.annotation.Import;
import cn.xu.spring.web.WebMvcConfiguration;

@Configuration
@Import(WebMvcConfiguration.class)
public class ControllerConfiguration {

}
