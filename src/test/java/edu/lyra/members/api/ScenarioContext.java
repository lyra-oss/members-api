package edu.lyra.members.api;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

@Component
@ScenarioScope
class ScenarioContext {

    private ResultActions resultActions;

    void setResultActions(final ResultActions resultActions) {
        this.resultActions = resultActions;
    }

    ResultActions getResultActions() {
        return resultActions;
    }

}
