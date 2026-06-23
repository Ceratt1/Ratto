import Image from "next/image";
import Link from "next/link";

import { LandingAuthActions } from "@/features/landing/components/landing-auth-actions";

const studySteps = [
  {
    title: "Suba seu material",
    description: "Use PDFs, apostilas e provas para começar pelo conteúdo que já faz parte da sua rotina.",
  },
  {
    title: "Treine com perguntas",
    description: "Transforme leitura passiva em prática com questões criadas a partir dos seus próprios temas.",
  },
  {
    title: "Revise o que falhou",
    description: "Veja os pontos que pedem reforço e monte sessões curtas para fechar lacunas.",
  },
];

const studySignals = ["Prática ativa", "Revisão focada", "Clareza do progresso"];

export default function LandingPage() {
  return (
    <main className="landing landing-modern">
      <nav className="landing-nav modern-nav">
        <a className="landing-brand modern-brand" href="#inicio" aria-label="Ratto">
          <Image className="brand-logo" src="/logo-ratto.png" alt="" width={42} height={42} priority />
          <span>Ratto</span>
        </a>
        <div className="landing-nav-links modern-nav-links">
          <a href="#como-funciona">Como funciona</a>
          <a href="#rotina">Rotina de estudo</a>
        </div>
        <LandingAuthActions />
      </nav>

      <section className="modern-hero" id="inicio">
        <div className="modern-hero-copy">
          <span className="modern-pill">Estude melhor com o que você já tem</span>
          <h1>Seu material vira treino. Seu treino vira clareza.</h1>
          <p>
            Ratto transforma apostilas, PDFs e provas em uma rotina de prática ativa para você
            descobrir lacunas, revisar com foco e evoluir sem se perder no conteúdo.
          </p>
          <div className="modern-actions">
            <Link className="modern-button primary" href="/app">Começar agora</Link>
            <a className="modern-button secondary" href="#como-funciona">Ver como funciona</a>
          </div>
        </div>

        <div className="modern-study-card" aria-label="Prévia de uma rotina de estudos no Ratto">
          <div className="study-card-top">
            <div>
              <span>Sessão inteligente</span>
              <strong>Revisão de hoje</strong>
            </div>
            <span className="study-score">82%</span>
          </div>

          <div className="modern-progress"><span /></div>

          <div className="focus-card active">
            <span>Agora</span>
            <div>
              <strong>Responder 10 questões</strong>
              <p>Foco nos assuntos que ainda aparecem como dúvida.</p>
            </div>
          </div>

          <div className="focus-grid">
            <div>
              <span>3</span>
              <strong>lacunas</strong>
            </div>
            <div>
              <span>18 min</span>
              <strong>sessão curta</strong>
            </div>
          </div>
        </div>
      </section>

      <section className="modern-strip" aria-label="Benefícios do estudo com Ratto">
        {studySignals.map((signal) => (
          <span key={signal}>{signal}</span>
        ))}
      </section>

      <section className="modern-section" id="como-funciona">
        <div className="modern-section-heading">
          <span className="modern-eyebrow">Do material para a prática</span>
          <h2>Um fluxo simples para estudar com mais intenção.</h2>
        </div>
        <div className="modern-step-grid">
          {studySteps.map((step, index) => (
            <article className="modern-step-card" key={step.title}>
              <span>{String(index + 1).padStart(2, "0")}</span>
              <h3>{step.title}</h3>
              <p>{step.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="modern-routine" id="rotina">
        <div>
          <span className="modern-eyebrow">Rotina sem peso</span>
          <h2>Menos tempo decidindo o que estudar. Mais tempo praticando.</h2>
        </div>
        <Link className="modern-button primary" href="/app">Entrar na área de estudos</Link>
      </section>
    </main>
  );
}
