package edu.lyra.members.api.cucumber;

import java.util.HashMap;
import java.util.Map;

import io.cucumber.spring.ScenarioScope;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@Component
@ScenarioScope
public class ScenarioContext {

    private final Map<String, String> locations = new HashMap<>();

    @Getter
    @Setter
    private ResultActions resultActions;

    @Getter
    @Setter
    private RequestPostProcessor jwtProcessor;

    public void putLocation(final String key, final String location) {
        this.locations.put(key, location);
    }

    public String getLocation(final String key) {
        return this.locations.get(key);
    }

}
