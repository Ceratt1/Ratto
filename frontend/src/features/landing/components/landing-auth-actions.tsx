"use client";

import Keycloak from "keycloak-js";
import { useEffect, useState } from "react";

import type { AuthRuntimeConfig } from "@/features/auth/models/auth.models";

export function LandingAuthActions() {
  const [keycloak, setKeycloak] = useState<Keycloak | null>(null);

  useEffect(() => {
    let active = true;
    async function initialize() {
      const response = await fetch("/api/auth/config", { cache: "no-store" });
      if (!response.ok) return;
      const client = new Keycloak((await response.json()) as AuthRuntimeConfig);
      await client.init({ onLoad: "check-sso", checkLoginIframe: false });
      if (active) setKeycloak(client);
    }
    initialize().catch((error) => console.error("Could not initialize landing authentication", error));
    return () => { active = false; };
  }, []);

  const redirectUri = typeof window === "undefined" ? undefined : `${window.location.origin}/app`;

  return (
    <div className="landing-auth-actions">
      <button className="primary" disabled={!keycloak} onClick={() => void keycloak?.login({ redirectUri })} type="button">
        Entrar
      </button>
    </div>
  );
}
