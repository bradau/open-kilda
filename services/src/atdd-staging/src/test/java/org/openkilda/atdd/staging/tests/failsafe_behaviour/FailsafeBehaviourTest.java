package org.openkilda.atdd.staging.tests.failsafe_behaviour;

import cucumber.api.CucumberOptions;
import org.junit.runner.RunWith;
import org.openkilda.atdd.staging.cucumber.CucumberWithSpringProfile;
import org.springframework.test.context.ActiveProfiles;

@RunWith(CucumberWithSpringProfile.class)
@CucumberOptions(features = {"classpath:features/failsafe_behaviour.feature"},
        glue = {"org.openkilda.atdd.staging.tests.failsafe_behaviour", "org.openkilda.atdd.staging.steps"},
        plugin = {"json:target/cucumber-reports/failsafe_behaviour_report.json"})
@ActiveProfiles("mock")
public class FailsafeBehaviourTest {
}
