package com.vyoog.prospectsoul_backend.imports.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ActivitiesJsonParserTest {

    private final ActivitiesJsonParser parser = new ActivitiesJsonParser(new ObjectMapper());

    @Test
    void parsesValidArray() {
        var r = parser.parse("[{\"NIC5DigitId\":\"47522\",\"Description\":\"Retail sale of hardware\"}," +
                "{\"NIC5DigitId\":\"47594\",\"Description\":\"Retail sale of glassware\"}]");
        assertThat(r).isInstanceOf(ActivitiesJsonParser.Result.Ok.class);
        List<ActivitiesJsonParser.Activity> acts = ((ActivitiesJsonParser.Result.Ok) r).activities();
        assertThat(acts).hasSize(2);
        assertThat(acts.get(0).nicCode()).isEqualTo("47522");
        assertThat(acts.get(1).description()).isEqualTo("Retail sale of glassware");
    }

    @Test
    void parsesSingleElement() {
        var r = parser.parse("[{\"NIC5DigitId\":\"01111\",\"Description\":\"Cereals\"}]");
        assertThat(r).isInstanceOf(ActivitiesJsonParser.Result.Ok.class);
        assertThat(((ActivitiesJsonParser.Result.Ok) r).activities()).hasSize(1);
    }

    @Test
    void literalNaTreatedAsEmpty() {
        assertThat(parser.parse("NA")).isInstanceOf(ActivitiesJsonParser.Result.Empty.class);
        assertThat(parser.parse("na")).isInstanceOf(ActivitiesJsonParser.Result.Empty.class);
    }

    @Test
    void nullOrBlankTreatedAsEmpty() {
        assertThat(parser.parse(null)).isInstanceOf(ActivitiesJsonParser.Result.Empty.class);
        assertThat(parser.parse("")).isInstanceOf(ActivitiesJsonParser.Result.Empty.class);
        assertThat(parser.parse("   ")).isInstanceOf(ActivitiesJsonParser.Result.Empty.class);
        assertThat(parser.parse("null")).isInstanceOf(ActivitiesJsonParser.Result.Empty.class);
    }

    @Test
    void emptyArrayIsEmpty() {
        assertThat(parser.parse("[]")).isInstanceOf(ActivitiesJsonParser.Result.Empty.class);
    }

    @Test
    void malformedJsonReturnsInvalid_notThrows() {
        var r = parser.parse("[{broken");
        assertThat(r).isInstanceOf(ActivitiesJsonParser.Result.Invalid.class);
    }

    @Test
    void objectAtRootIsInvalid() {
        var r = parser.parse("{\"NIC5DigitId\":\"01111\"}");
        assertThat(r).isInstanceOf(ActivitiesJsonParser.Result.Invalid.class);
    }
}
