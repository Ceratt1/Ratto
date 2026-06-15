import Image from "next/image";
import Link from "next/link";

import { AuthActions } from "@/features/auth/components/auth-actions";
import { UploadForm } from "@/features/pdf-upload/components/upload-form";

export default function StudyAppPage() {
  return (
    <main>
      <header className="hero">
        <Link className="brand-mark" href="/" aria-label="Voltar para a página inicial">
          <Image className="brand-logo" src="/logo-learn-ai.png" alt="" width={56} height={56} priority />
        </Link>
        <div className="hero-copy">
          <span className="eyebrow">Minha área de estudos</span>
          <h1>Prepare sua próxima sessão de estudo.</h1>
          <p>
            Envie seus materiais para criar questões de prática e começar uma revisão focada nos
            conteúdos que você quer dominar.
          </p>
        </div>
        <AuthActions />
      </header>
      <UploadForm />
    </main>
  );
}
