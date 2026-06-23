import Image from "next/image";
import Link from "next/link";
import type { ReactNode } from "react";

import { AuthActions } from "@/features/auth/components/auth-actions";

interface AppShellProps {
  children: ReactNode;
}

export function AppShell({ children }: Readonly<AppShellProps>) {
  return (
    <main className="app-shell">
      <header className="hero app-shell-header">
        <Link className="brand-mark" href="/" aria-label="Voltar para a página inicial">
          <Image className="brand-logo" src="/logo-ratto.png" alt="" width={56} height={56} priority />
          <span>Ratto</span>
        </Link>
        <nav className="app-shell-nav" aria-label="Navegação da área de estudos">
          <a href="#areas">Áreas de estudo</a>
          <a href="#atividade">Atividade</a>
        </nav>
        <AuthActions />
      </header>
      <section className="app-shell-content" aria-label="Área de estudos">
        {children}
      </section>
    </main>
  );
}
