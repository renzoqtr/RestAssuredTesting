package org.timeapi.test;


import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.github.fge.jsonschema.SchemaVersion.DRAFTV4;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimeApiTest {

    private static final String URL = "https://timeapi.io/api/";
    private JsonSchemaFactory jsonSchemaFactory;

    @BeforeEach
    public void initTest() {
        jsonSchemaFactory = JsonSchemaFactory.newBuilder()
                .setValidationConfiguration(ValidationConfiguration.newBuilder().setDefaultVersion(DRAFTV4).freeze())
                .freeze();
    }

    /*
       Data driven and time verification
    */
    @ParameterizedTest
    @CsvFileSource(resources = "/TimeZones.csv", numLinesToSkip = 1)
    public void timeZonesTest(String timeZone) {
        //Arrange
        String timeZones = "TimeZone/AvailableTimeZones";
        Long givenTime = 2000L;
        //Act
        given()
                .accept("application/json")
                .when()
                .get(URL + timeZones)
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(containsString(timeZone))
                .and()
                .time(lessThan(givenTime));
    }

    /*
        Schema verification
     */
    @Test
    public void currentTimeTest() {
        //Arrange
        String currentTime = "Time/current/zone?timeZone=America/Bogota";
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String schema = "CurrentTimeSchema.json";
        //Act - Assert
        given()
                .accept("application/json")
                .when()
                .get(URL + currentTime)
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("timeZone", equalTo("America/Bogota"))
                .and()
                .body("year", equalTo(currentYear))
                .body(
                        matchesJsonSchemaInClasspath(schema).using(jsonSchemaFactory)
                );
    }

    /*
        Post, with request body as Map
     */
    @Test
    public void calculationIncrementTest() {
        //Arrange
        String calculationIncrement = "Calculation/current/increment";
        String schema = "CalculationIncrementSchema.json";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("timeZone", "Europe/Amsterdam");
        requestBody.put("timeSpan", "16:03:45:17");
        //Act - Assert
        given()
                .accept("application/json")
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post(URL + calculationIncrement)
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(
                        matchesJsonSchemaInClasspath(schema).using(jsonSchemaFactory)
                );
    }

    /*
        With Response interaction
     */
    @Test
    public void timeZonesTestWithResponse() {
        //Arrange
        String timeZones = "TimeZone/AvailableTimeZones";
        //Act
        Response response = given()
                .accept("application/json")
                .when()
                .get(URL + timeZones)
                .then()
                .extract()
                .response();
        //Assert
        assertEquals(200, response.statusCode());
        assertTrue(response.asString().contains("America/Bogota"));
    }
}
