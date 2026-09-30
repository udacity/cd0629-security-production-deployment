package simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class OrdersListSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http.baseUrl("http://localhost:8081");

    ScenarioBuilder scn = scenario("List Orders")
            .exec(http("list orders").get("/orders").check(status().is(200)));

    {
        setUp(
                scn.injectOpen(constantUsersPerSec(200).during(30))
        ).protocols(httpProtocol);
    }
}
