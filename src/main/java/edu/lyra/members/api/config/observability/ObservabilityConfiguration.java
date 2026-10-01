package edu.lyra.members.api.config.observability;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(ProtobufRuntimeHints.class)
class ObservabilityConfiguration {}
