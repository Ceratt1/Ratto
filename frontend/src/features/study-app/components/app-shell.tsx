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
        <div className="hero-copy">
          <span className="eyebrow">Minha área de estudos</span>
          <h1>Prepare sua próxima revisão.</h1>
          <p>
            Envie seus materiais para criar questões de prática e revisar com foco no que ainda
            precisa evoluir.
          </p>
        </div>
        <AuthActions />
      </header>
      <section className="app-shell-content" aria-label="Área de estudos">
        {children}
      </section>
    </main>
  );
}
