import Image from "next/image";
import Link from "next/link";

import { LandingAuthActions } from "@/features/landing/components/landing-auth-actions";

const studyBenefits = [
  {
    number: "01",
    title: "Questões a partir do seu material",
    description: "Envie os PDFs que você já usa e transforme conteúdo denso em perguntas para praticar ativamente.",
  },
  {
    number: "02",
    title: "Gaps mais fáceis de enxergar",
    description: "Use seus erros e dúvidas para descobrir quais assuntos precisam de mais atenção na próxima revisão.",
  },
  {
    number: "03",
    title: "Estudo focado no que importa",
    description: "Direcione seu tempo para os pontos que ainda não estão firmes e avance com mais clareza.",
  },
];

export default function LandingPage() {
  return (
    <main className="landing">
      <nav className="landing-nav">
        <a className="landing-brand" href="#inicio" aria-label="Learn IA">
          <span className="brand-mark">
            <Image className="brand-logo" src="/logo-learn-ai.png" alt="" width={42} height={42} priority />
          </span>
          <span>Learn IA</span>
        </a>
        <div className="landing-nav-links">
          <a href="#como-funciona">Como funciona</a>
          <a href="#metodo">Método de estudo</a>
        </div>
        <LandingAuthActions />
      </nav>

      <section className="landing-hero" id="inicio">
        <div className="landing-hero-copy">
          <span className="study-pill">Aprendizado ativo guiado por IA</span>
          <h1>Estude com foco nos pontos que ainda precisam evoluir.</h1>
          <p>
            Transforme seus materiais em questões, entenda onde estão seus gaps e construa uma
            rotina de revisão direcionada ao que realmente faz diferença.
          </p>
          <div className="landing-hero-actions">
            <Link className="button-link primary" href="/app">Começar a estudar</Link>
            <a className="button-link secondary" href="#como-funciona">Conhecer o método</a>
          </div>
          <div className="study-proof">
            <span>Questões explicadas</span>
            <span>Revisões direcionadas</span>
            <span>Evolução contínua</span>
          </div>
        </div>

        <div className="study-preview" aria-label="Exemplo de jornada de estudos">
          <div className="preview-header">
            <span>Revisão de hoje</span>
            <strong>Foco nos seus gaps</strong>
          </div>
          <div className="preview-progress">
            <div>
              <span>Progresso da revisão</span>
              <strong>72%</strong>
            </div>
            <div className="progress-track"><span /></div>
          </div>
          <div className="preview-topic">
            <span className="topic-icon">?</span>
            <div>
              <strong>Arquitetura de aplicações</strong>
              <p>3 conceitos para reforçar antes de avançar</p>
            </div>
          </div>
          <div className="preview-topic">
            <span className="topic-icon">✓</span>
            <div>
              <strong>Fundamentos de cloud</strong>
              <p>Bom domínio nas últimas questões</p>
            </div>
          </div>
          <div className="preview-note">
            <span>Próximo passo</span>
            <strong>Praticar 8 questões sobre seus pontos de atenção.</strong>
          </div>
        </div>
      </section>

      <section className="landing-section" id="como-funciona">
        <div className="section-heading">
          <span className="eyebrow">Do material à evolução</span>
          <h2>Um ciclo de estudos pensado para aprender, praticar e melhorar.</h2>
          <p>Seu PDF é apenas o começo. O objetivo é transformar leitura passiva em prática com direção.</p>
        </div>
        <div className="benefit-grid">
          {studyBenefits.map((benefit) => (
            <article className="benefit-card" key={benefit.number}>
              <span>{benefit.number}</span>
              <h3>{benefit.title}</h3>
              <p>{benefit.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="study-method" id="metodo">
        <div>
          <span className="eyebrow">Seu fluxo de estudo</span>
          <h2>Menos tempo tentando decidir o que revisar. Mais tempo aprendendo.</h2>
        </div>
        <ol>
          <li><span>1</span><div><strong>Envie seus materiais</strong><p>Use apostilas, provas e conteúdos que fazem parte da sua preparação.</p></div></li>
          <li><span>2</span><div><strong>Pratique com questões</strong><p>Revise o conteúdo de forma ativa com perguntas geradas a partir dos PDFs.</p></div></li>
          <li><span>3</span><div><strong>Mapeie seus gaps</strong><p>Transforme erros e dúvidas em sinais claros sobre o que precisa ser reforçado.</p></div></li>
          <li><span>4</span><div><strong>Estude com direção</strong><p>Organize as próximas revisões com foco nos pontos de maior impacto.</p></div></li>
        </ol>
      </section>

      <section className="landing-cta">
        <span className="eyebrow">Comece pelo seu próprio material</span>
        <h2>Transforme o que você precisa aprender em um caminho claro de estudo.</h2>
        <Link className="button-link primary" href="/app">Entrar na minha área de estudos</Link>
      </section>
    </main>
  );
}
