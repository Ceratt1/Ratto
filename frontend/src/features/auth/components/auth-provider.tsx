"use client";

import Keycloak from "keycloak-js";
import Image from "next/image";
import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useState } from "react";

import type { AuthRuntimeConfig, UserProfile } from "@/features/auth/models/auth.models";

interface AuthContextValue {
  authenticated: boolean;
  initialized: boolean;
  profile: UserProfile | null;
  getToken: () => Promise<string>;
  login: () => void;
  register: () => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [keycloak, setKeycloak] = useState<Keycloak | null>(null);
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [profile, setProfile] = useState<UserProfile | null>(null);

  useEffect(() => {
    let active = true;
    async function initialize() {
      const configResponse = await fetch("/api/auth/config", { cache: "no-store" });
      if (!configResponse.ok) throw new Error("Não foi possível carregar a configuração de autenticação.");
      const client = new Keycloak((await configResponse.json()) as AuthRuntimeConfig);
      const loggedIn = await client.init({ onLoad: "login-required", pkceMethod: "S256", checkLoginIframe: false });
      if (!active) return;
      setKeycloak(client);
      setAuthenticated(loggedIn);
      setInitialized(true);
      if (loggedIn && client.token) {
        const response = await fetch("/api/users/me", {
          method: "PUT",
          headers: { Authorization: `Bearer ${client.token}` },
        });
        if (response.ok && active) setProfile((await response.json()) as UserProfile);
      }
    }
    initialize().catch((error) => {
      console.error("Could not initialize authentication", error);
      if (active) setInitialized(true);
    });
    return () => { active = false; };
  }, []);

  const getToken = useCallback(async () => {
    if (!keycloak?.authenticated) throw new Error("Entre na sua conta antes de enviar PDFs.");
    await keycloak.updateToken(30);
    if (!keycloak.token) throw new Error("A sessão expirou. Entre novamente.");
    return keycloak.token;
  }, [keycloak]);

  const value = useMemo<AuthContextValue>(() => ({
    authenticated,
    initialized,
    profile,
    getToken,
    login: () => void keycloak?.login(),
    register: () => void keycloak?.register(),
    logout: () => void keycloak?.logout({ redirectUri: window.location.origin }),
  }), [authenticated, getToken, initialized, keycloak, profile]);

  if (!initialized || !authenticated) {
    return (
      <div className="auth-loading" role="status">
        <div className="auth-loading-mark">
          <Image src="/logo-learn-ai.png" alt="Learn IA" width={72} height={72} priority />
        </div>
        <span>Preparando seu ambiente de estudos...</span>
      </div>
    );
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
