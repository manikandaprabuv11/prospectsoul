package com.vyoog.prospectsoul_backend.common.security;

public final class RoleConstants {

    private RoleConstants() {}

    public static final String ANALYST    = "PS_ANALYST";
    public static final String SALES_LEAD = "PS_SALES_LEAD";
    public static final String ADMIN      = "PS_ADMIN";
    public static final String VIEWER     = "PS_VIEWER";
    public static final String COO        = "PS_COO";

    public static final String HAS_MUTATE    = "hasAnyRole('PS_ANALYST','PS_SALES_LEAD','PS_ADMIN')";
    public static final String HAS_EXPORT    = "hasAnyRole('PS_SALES_LEAD','PS_ADMIN')";
    public static final String HAS_CONFIGURE = "hasRole('PS_ADMIN')";
    public static final String HAS_READ      = "hasAnyRole('PS_ANALYST','PS_SALES_LEAD','PS_ADMIN','PS_VIEWER','PS_COO')";
}
