#!/bin/sh
set -eu

KCADM=/opt/keycloak/bin/kcadm.sh
SERVER=http://keycloak:8080

until "$KCADM" config credentials \
  --server "$SERVER" \
  --realm master \
  --user "$KEYCLOAK_ADMIN" \
  --password "$KEYCLOAK_ADMIN_PASSWORD" >/dev/null 2>&1; do
  sleep 3
done

client_id=$("$KCADM" get clients -r ratto -q clientId=ratto-frontend --fields id --format csv --noquotes)
"$KCADM" update realms/ratto \
  -s loginTheme=ratto \
  -s internationalizationEnabled=true \
  -s defaultLocale=pt-BR \
  -s 'supportedLocales=["pt-BR","en"]'

"$KCADM" update "clients/$client_id" -r ratto \
  -s 'redirectUris=["http://localhost:3000/*"]' \
  -s 'webOrigins=["http://localhost:3000"]'

mapper_id=$("$KCADM" get "clients/$client_id/protocol-mappers/models" -r ratto \
  --fields id,name --format csv --noquotes |
  while IFS=, read -r id name; do
    if [ "$name" = "gateway-api audience" ]; then
      echo "$id"
      break
    fi
  done)

if [ -z "$mapper_id" ]; then
  "$KCADM" create "clients/$client_id/protocol-mappers/models" -r ratto \
    -s name='gateway-api audience' \
    -s protocol=openid-connect \
    -s protocolMapper=oidc-audience-mapper \
    -s 'config."included.custom.audience"=gateway-api' \
    -s 'config."access.token.claim"=true' \
    -s 'config."id.token.claim"=false'
fi

provider_exists() {
  alias=$1
  "$KCADM" get "identity-provider/instances/$alias" -r ratto >/dev/null 2>&1
}

configure_google() {
  if [ "${SSO_GOOGLE_ENABLED:-false}" != "true" ]; then
    if provider_exists google; then
      "$KCADM" update identity-provider/instances/google -r ratto -s enabled=false
    fi
    return
  fi

  if [ -z "${GOOGLE_CLIENT_ID:-}" ] || [ -z "${GOOGLE_CLIENT_SECRET:-}" ]; then
    echo "SSO_GOOGLE_ENABLED=true requires GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET" >&2
    exit 1
  fi

  if provider_exists google; then
    action=update
    resource=identity-provider/instances/google
  else
    action=create
    resource=identity-provider/instances
  fi

  "$KCADM" "$action" "$resource" -r ratto \
    -s alias=google \
    -s displayName='Continuar com Google' \
    -s providerId=google \
    -s enabled=true \
    -s trustEmail=true \
    -s storeToken=false \
    -s addReadTokenRoleOnCreate=false \
    -s linkOnly=false \
    -s firstBrokerLoginFlowAlias='first broker login' \
    -s "config.clientId=$GOOGLE_CLIENT_ID" \
    -s "config.clientSecret=$GOOGLE_CLIENT_SECRET" \
    -s config.defaultScope='openid profile email' \
    -s config.syncMode=IMPORT \
    -s config.useJwksUrl=true
}

configure_azure() {
  if [ "${SSO_AZURE_ENABLED:-false}" != "true" ]; then
    if provider_exists azure; then
      "$KCADM" update identity-provider/instances/azure -r ratto -s enabled=false
    fi
    return
  fi

  if [ -z "${AZURE_CLIENT_ID:-}" ] || [ -z "${AZURE_CLIENT_SECRET:-}" ] || [ -z "${AZURE_TENANT_ID:-}" ]; then
    echo "SSO_AZURE_ENABLED=true requires AZURE_CLIENT_ID, AZURE_CLIENT_SECRET and AZURE_TENANT_ID" >&2
    exit 1
  fi

  issuer="https://login.microsoftonline.com/$AZURE_TENANT_ID/v2.0"
  if provider_exists azure; then
    action=update
    resource=identity-provider/instances/azure
  else
    action=create
    resource=identity-provider/instances
  fi

  "$KCADM" "$action" "$resource" -r ratto \
    -s alias=azure \
    -s displayName='Continuar com Microsoft' \
    -s providerId=oidc \
    -s enabled=true \
    -s trustEmail=true \
    -s storeToken=false \
    -s addReadTokenRoleOnCreate=false \
    -s linkOnly=false \
    -s firstBrokerLoginFlowAlias='first broker login' \
    -s "config.clientId=$AZURE_CLIENT_ID" \
    -s "config.clientSecret=$AZURE_CLIENT_SECRET" \
    -s "config.issuer=$issuer" \
    -s "config.authorizationUrl=$issuer/oauth2/v2.0/authorize" \
    -s "config.tokenUrl=$issuer/oauth2/v2.0/token" \
    -s "config.jwksUrl=https://login.microsoftonline.com/$AZURE_TENANT_ID/discovery/v2.0/keys" \
    -s "config.userInfoUrl=https://graph.microsoft.com/oidc/userinfo" \
    -s config.clientAuthMethod=client_secret_post \
    -s config.defaultScope='openid profile email' \
    -s config.syncMode=IMPORT \
    -s config.useJwksUrl=true \
    -s config.validateSignature=true
}

configure_google
configure_azure
