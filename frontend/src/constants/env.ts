/**
 * Typed access to the Vite environment. Only `VITE_*` variables reach the
 * browser bundle, so nothing secret may ever be read here.
 */
export const env = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1',
  keycloakUrl: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8081',
  keycloakRealm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'vyoog',
  keycloakClientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'prospectsoul-web',
} as const
