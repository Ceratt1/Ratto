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
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

type BrowserCryptoWithOptionalUuid = Omit<Crypto, "randomUUID"> & {
  randomUUID?: () => string;
};

function ensureRandomUuid() {
  if (typeof window === "undefined") return;

  const browserCrypto = window.crypto as BrowserCryptoWithOptionalUuid;
  if (typeof browserCrypto.randomUUID === "function") return;

  Object.defineProperty(browserCrypto, "randomUUID", {
    configurable: true,
    value: () => {
      const bytes = new Uint8Array(16);
      browserCrypto.getRandomValues(bytes);
      bytes[6] = (bytes[6] & 0x0f) | 0x40;
      bytes[8] = (bytes[8] & 0x3f) | 0x80;
      const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0"));
      return `${hex.slice(0, 4).join("")}-${hex.slice(4, 6).join("")}-${hex.slice(6, 8).join("")}-${hex.slice(8, 10).join("")}-${hex.slice(10).join("")}`;
    },
  });
}

export function AuthProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [keycloak, setKeycloak] = useState<Keycloak | null>(null);
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [authError, setAuthError] = useState(false);

  useEffect(() => {
    let active = true;
    async function initialize() {
      ensureRandomUuid();
      const configResponse = await fetch("/api/auth/config", { cache: "no-store" });
      if (!configResponse.ok) throw new Error("Não foi possível carregar a configuração de autenticação.");
      const client = new Keycloak((await configResponse.json()) as AuthRuntimeConfig);
      setKeycloak(client);
      const loggedIn = await client.init({
        onLoad: "login-required",
        checkLoginIframe: false,
        pkceMethod: false,
        responseMode: "query",
      });
      if (!active) return;
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
      if (active) {
        setAuthError(true);
        setInitialized(true);
      }
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
    logout: () => void keycloak?.logout({ redirectUri: window.location.origin }),
  }), [authenticated, getToken, initialized, keycloak, profile]);

  if (!initialized) {
    return (
      <div className="auth-loading" role="status">
        <div className="auth-loading-mark">
          <Image src="/logo-ratto.png" alt="Ratto" width={72} height={72} priority />
        </div>
        <span>Preparando sua área Ratto...</span>
      </div>
    );
  }

  if (!authenticated) {
    return (
      <div className="auth-loading" role="status">
        <div className="auth-loading-mark">
          <Image src="/logo-ratto.png" alt="Ratto" width={72} height={72} priority />
        </div>
        <span>{authError ? "Não foi possível abrir o login automaticamente." : "Preparando sua área Ratto..."}</span>
        <button className="button" onClick={() => { void keycloak?.login({ redirectUri: `${window.location.origin}/app` }); }} type="button">
          Entrar
        </button>
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
