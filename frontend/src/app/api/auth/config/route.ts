import { NextResponse } from "next/server";

import type { AuthRuntimeConfig } from "@/features/auth/models/auth.models";

export const dynamic = "force-dynamic";

export function GET(): NextResponse<AuthRuntimeConfig> {
  return NextResponse.json({
    url: process.env.KEYCLOAK_PUBLIC_URL ?? "http://localhost:3000",
    realm: process.env.KEYCLOAK_REALM ?? "ratto",
    clientId: process.env.KEYCLOAK_CLIENT_ID ?? "ratto-frontend",
  });
}
