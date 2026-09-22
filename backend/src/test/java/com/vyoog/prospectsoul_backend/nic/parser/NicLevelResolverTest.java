package com.vyoog.prospectsoul_backend.nic.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NicLevelResolverTest {

    @Test
    void detectsLevelForEveryDigitLength() {
        assertThat(NicLevelResolver.forCode("1")).isEqualTo((short) 1);
        assertThat(NicLevelResolver.forCode("22")).isEqualTo((short) 2);
        assertThat(NicLevelResolver.forCode("221")).isEqualTo((short) 3);
        assertThat(NicLevelResolver.forCode("2211")).isEqualTo((short) 4);
        assertThat(NicLevelResolver.forCode("22111")).isEqualTo((short) 5);
    }

    @Test
    void rejectsNonNumeric() {
        assertThatThrownBy(() -> NicLevelResolver.forCode("22a"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmpty() {
        assertThatThrownBy(() -> NicLevelResolver.forCode(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTooLong() {
        assertThatThrownBy(() -> NicLevelResolver.forCode("123456"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> NicLevelResolver.forCode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
