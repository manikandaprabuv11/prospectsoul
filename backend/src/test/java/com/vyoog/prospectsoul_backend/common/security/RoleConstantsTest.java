package com.vyoog.prospectsoul_backend.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoleConstantsTest {

    @Test
    void hasMutate_grantsAnalyst() {
        assertThat(evaluateSpel(RoleConstants.HAS_MUTATE, "ROLE_PS_ANALYST")).isTrue();
    }

    @Test
    void hasMutate_grantsSalesLead() {
        assertThat(evaluateSpel(RoleConstants.HAS_MUTATE, "ROLE_PS_SALES_LEAD")).isTrue();
    }

    @Test
    void hasMutate_grantsAdmin() {
        assertThat(evaluateSpel(RoleConstants.HAS_MUTATE, "ROLE_PS_ADMIN")).isTrue();
    }

    @Test
    void hasMutate_deniesViewer() {
        assertThat(evaluateSpel(RoleConstants.HAS_MUTATE, "ROLE_PS_VIEWER")).isFalse();
    }

    @Test
    void hasMutate_deniesCoo() {
        assertThat(evaluateSpel(RoleConstants.HAS_MUTATE, "ROLE_PS_COO")).isFalse();
    }

    @Test
    void hasExport_grantsSalesLead() {
        assertThat(evaluateSpel(RoleConstants.HAS_EXPORT, "ROLE_PS_SALES_LEAD")).isTrue();
    }

    @Test
    void hasExport_grantsAdmin() {
        assertThat(evaluateSpel(RoleConstants.HAS_EXPORT, "ROLE_PS_ADMIN")).isTrue();
    }

    @Test
    void hasExport_deniesAnalyst() {
        assertThat(evaluateSpel(RoleConstants.HAS_EXPORT, "ROLE_PS_ANALYST")).isFalse();
    }

    @Test
    void hasConfigure_grantsOnlyAdmin() {
        assertThat(evaluateSpel(RoleConstants.HAS_CONFIGURE, "ROLE_PS_ADMIN")).isTrue();
        assertThat(evaluateSpel(RoleConstants.HAS_CONFIGURE, "ROLE_PS_ANALYST")).isFalse();
        assertThat(evaluateSpel(RoleConstants.HAS_CONFIGURE, "ROLE_PS_SALES_LEAD")).isFalse();
        assertThat(evaluateSpel(RoleConstants.HAS_CONFIGURE, "ROLE_PS_VIEWER")).isFalse();
        assertThat(evaluateSpel(RoleConstants.HAS_CONFIGURE, "ROLE_PS_COO")).isFalse();
    }

    @Test
    void hasRead_grantsAllRoles() {
        assertThat(evaluateSpel(RoleConstants.HAS_READ, "ROLE_PS_ANALYST")).isTrue();
        assertThat(evaluateSpel(RoleConstants.HAS_READ, "ROLE_PS_SALES_LEAD")).isTrue();
        assertThat(evaluateSpel(RoleConstants.HAS_READ, "ROLE_PS_ADMIN")).isTrue();
        assertThat(evaluateSpel(RoleConstants.HAS_READ, "ROLE_PS_VIEWER")).isTrue();
        assertThat(evaluateSpel(RoleConstants.HAS_READ, "ROLE_PS_COO")).isTrue();
    }

    private boolean evaluateSpel(String expression, String authority) {
        Authentication auth = new TestingAuthenticationToken(
                "user", "pass", List.of(new SimpleGrantedAuthority(authority)));
        org.springframework.expression.spel.standard.SpelExpressionParser parser =
                new org.springframework.expression.spel.standard.SpelExpressionParser();
        org.springframework.expression.Expression expr = parser.parseExpression(expression);
        org.springframework.security.access.expression.SecurityExpressionRoot root =
                new org.springframework.security.access.expression.SecurityExpressionRoot(auth) {};
        org.springframework.expression.spel.support.StandardEvaluationContext ctx =
                new org.springframework.expression.spel.support.StandardEvaluationContext(root);
        return Boolean.TRUE.equals(expr.getValue(ctx, Boolean.class));
    }
}
