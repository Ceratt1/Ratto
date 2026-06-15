"use client";

import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/components/auth-provider";

export function AuthActions() {
  const { authenticated, initialized, login, logout, profile, register } = useAuth();
  if (!initialized) return <span className="auth-status">Carregando sessão...</span>;
  if (authenticated) {
    return (
      <div className="auth-actions">
        <span className="auth-status">{profile ? `${profile.firstName} ${profile.lastName}` : "Sessão autenticada"}</span>
        <Button onClick={logout} type="button">Sair</Button>
      </div>
    );
  }
  return (
    <div className="auth-actions">
      <Button onClick={login} type="button">Entrar</Button>
      <Button onClick={register} type="button">Cadastrar</Button>
    </div>
  );
}
