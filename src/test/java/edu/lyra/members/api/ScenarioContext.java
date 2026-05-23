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
    private String               parentSub;

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

    String getParentSub() {
        return parentSub;
    }

    void setParentSub(final String parentSub) {
        this.parentSub = parentSub;
    }

}
