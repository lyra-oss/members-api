package edu.lyra.members.api.config.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@EnableConfigurationProperties(ApiBasePath.class)
@ImportRuntimeHints(HibernateValidatorRuntimeHints.class)
class ApiWebConfiguration {}
