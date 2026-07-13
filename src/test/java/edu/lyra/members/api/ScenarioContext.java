package edu.lyra.members.api;

import java.util.HashMap;
import java.util.Map;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@Component
@ScenarioScope
class ScenarioContext {

    private final Map<String, String> locations = new HashMap<>();

    private ResultActions resultActions;
    private RequestPostProcessor jwtProcessor;

    void setResultActions(final ResultActions resultActions) {
        this.resultActions = resultActions;
    }

    ResultActions getResultActions() {
        return resultActions;
    }

    RequestPostProcessor getJwtProcessor() {
        return jwtProcessor;
    }

    void setJwtProcessor(final RequestPostProcessor jwtProcessor) {
        this.jwtProcessor = jwtProcessor;
    }

    void putLocation(final String key, final String location) {
        this.locations.put(key, location);
    }

    String getLocation(final String key) {
        return this.locations.get(key);
    }

}
