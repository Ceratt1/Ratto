"use client";

import { useEffect, useRef, useState } from "react";

import { useAuth } from "@/features/auth/components/auth-provider";

export function AuthActions() {
  const { authenticated, initialized, login, logout, profile, register } = useAuth();
  const [open, setOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handlePointerDown(event: PointerEvent) {
      if (!menuRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, []);

  if (!initialized) return <span className="auth-status">Carregando sessão...</span>;
  if (authenticated) {
    const fullName = profile ? `${profile.firstName} ${profile.lastName}`.trim() : "Estudante";
    const initials = fullName
      .split(" ")
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0])
      .join("")
      .toUpperCase();

    return (
      <div className="user-menu" ref={menuRef}>
        <button
          aria-expanded={open}
          aria-haspopup="menu"
          className="user-menu-trigger"
          onClick={() => setOpen((current) => !current)}
          type="button"
        >
          <span className="user-avatar">{initials || "R"}</span>
          <span className="user-menu-name">{fullName}</span>
          <span aria-hidden="true" className="user-menu-chevron">⌄</span>
        </button>
        {open && (
          <div className="user-menu-popover" role="menu">
            <div className="user-menu-profile">
              <strong>{fullName}</strong>
              {profile?.email && <span>{profile.email}</span>}
            </div>
            <button onClick={logout} role="menuitem" type="button">Sair da conta</button>
          </div>
        )}
      </div>
    );
  }
  return (
    <div className="auth-actions">
      <button className="button" onClick={login} type="button">Entrar</button>
      <button className="button" onClick={register} type="button">Cadastrar</button>
    </div>
  );
}
