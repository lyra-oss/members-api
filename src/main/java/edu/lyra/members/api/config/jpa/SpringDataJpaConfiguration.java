package edu.lyra.members.api.config.jpa;

import edu.lyra.members.api.MembersApiApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackageClasses = MembersApiApplication.class)
@EnableTransactionManagement
class SpringDataJpaConfiguration {}
