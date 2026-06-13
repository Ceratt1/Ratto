import { UploadForm } from "@/features/pdf-upload/components/upload-form";

export default function HomePage() {
  return (
    <main>
      <header className="hero">
        <div className="brand-mark">LI</div>
        <div className="hero-copy">
          <span className="eyebrow">Learn IA</span>
          <h1>Transforme seus PDFs em questões para estudar.</h1>
          <p>
            Envie até dois documentos. O pipeline extrai o conteúdo, identifica o idioma e gera
            perguntas com alternativas explicadas.
          </p>
        </div>
      </header>
      <UploadForm />
    </main>
  );
}
