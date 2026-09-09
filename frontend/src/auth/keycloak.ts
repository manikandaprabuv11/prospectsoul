import Keycloak from 'keycloak-js'
import { env } from '@/constants/env'

export const keycloak = new Keycloak({
  url: env.keycloakUrl,
  realm: env.keycloakRealm,
  clientId: env.keycloakClientId,
})

let initialization: Promise<boolean> | undefined

export function initializeKeycloak(): Promise<boolean> {
  initialization ??= keycloak.init({
    onLoad: 'check-sso',
    checkLoginIframe: false,
    silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
    pkceMethod: 'S256',
  })

  return initialization
}
