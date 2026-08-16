package edu.lyra.members.api.config.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ApiBasePath.class)
class ApiWebConfiguration {}
