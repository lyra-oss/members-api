package edu.lyra.members.api;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@Component
@ScenarioScope
class ScenarioContext {

    private ResultActions resultActions;
    private RequestPostProcessor jwtProcessor;

    private String parentEmail;

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

    String getParentEmail() {
        return parentEmail;
    }

    void setParentEmail(final String parentEmail) {
        this.parentEmail = parentEmail;
    }

}
