"use client";

import { useState } from "react";

import { UploadForm } from "@/features/pdf-upload/components/upload-form";
import { useAuth } from "@/features/auth/components/auth-provider";

type WorkspaceView = "overview" | "create";

const studyAreas = [
  {
    title: "Provas e concursos",
    description: "Organize materiais de preparação e pratique por tema.",
    meta: "Pronto para começar",
  },
  {
    title: "Faculdade",
    description: "Transforme aulas e apostilas em sessões de revisão.",
    meta: "Em breve",
  },
  {
    title: "Certificações",
    description: "Crie trilhas para reforçar conceitos antes da prova.",
    meta: "Em breve",
  },
];

export function StudyWorkspace() {
  const [view, setView] = useState<WorkspaceView>("overview");
  const { profile } = useAuth();
  const firstName = profile?.firstName || "estudante";

  if (view === "create") {
    return (
      <section className="workspace-create" id="criar">
        <div className="workspace-create-header">
          <button className="back-button" onClick={() => setView("overview")} type="button">
            Voltar
          </button>
          <div>
            <span className="eyebrow">Nova prática</span>
            <h1>Gerar questões</h1>
            <p>Envie um PDF de até 30 MB para criar uma sessão de estudo focada.</p>
          </div>
        </div>
        <UploadForm />
      </section>
    );
  }

  return (
    <div className="student-workspace">
      <section className="workspace-hero">
        <div>
          <span className="eyebrow">Área de estudante</span>
          <h1>Bom estudo, {firstName}.</h1>
          <p>
            Organize áreas de estudo, acompanhe sua prática e crie questões quando quiser revisar
            com mais foco.
          </p>
        </div>
        <button className="workspace-primary-action" onClick={() => setView("create")} type="button">
          Gerar questões
        </button>
      </section>

      <section className="workspace-grid">
        <div className="workspace-main">
          <div className="workspace-section-heading" id="areas">
            <div>
              <span className="eyebrow">Suas áreas</span>
              <h2>Escolha onde quer evoluir.</h2>
            </div>
            <span className="subtle-button pending-action">Novas áreas em breve</span>
          </div>

          <div className="study-area-grid">
            {studyAreas.map((area) => (
              <article className="study-area-card" key={area.title}>
                <span>{area.meta}</span>
                <h3>{area.title}</h3>
                <p>{area.description}</p>
              </article>
            ))}
          </div>
        </div>

        <aside className="workspace-side" id="atividade">
          <div className="quick-action-card">
            <span className="eyebrow">Atalho</span>
            <h2>Crie uma sessão a partir de um PDF.</h2>
            <p>Ideal para transformar material recente em prática objetiva.</p>
            <button className="workspace-primary-action" onClick={() => setView("create")} type="button">
              Começar
            </button>
          </div>

          <div className="study-status-card">
            <span className="eyebrow">Hoje</span>
            <strong>Nenhuma sessão ativa</strong>
            <p>Quando suas questões forem criadas, elas aparecerão aqui para continuar a revisão.</p>
          </div>
        </aside>
      </section>
    </div>
  );
}
