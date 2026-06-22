import Image from "next/image";
import Link from "next/link";

import { LandingAuthActions } from "@/features/landing/components/landing-auth-actions";

const studyBenefits = [
  {
    number: "01",
    title: "Prática a partir do seu material",
    description: "Transforme PDFs, apostilas e resumos em perguntas para treinar sem sair do seu ritmo.",
  },
  {
    number: "02",
    title: "Lacunas fáceis de perceber",
    description: "Enxergue os pontos que ainda não estão firmes e escolha melhor onde concentrar energia.",
  },
  {
    number: "03",
    title: "Revisão com direção",
    description: "Crie sessões curtas, práticas e objetivas para avançar com mais clareza.",
  },
];

export default function LandingPage() {
  return (
    <main className="landing">
      <nav className="landing-nav">
        <a className="landing-brand" href="#inicio" aria-label="Ratto">
          <Image className="brand-logo" src="/logo-ratto.png" alt="" width={42} height={42} priority />
          <span>Ratto</span>
        </a>
        <div className="landing-nav-links">
          <a href="#como-funciona">Como funciona</a>
          <a href="#metodo">Rotina de estudo</a>
        </div>
        <LandingAuthActions />
      </nav>

      <section className="landing-hero" id="inicio">
        <div className="landing-hero-copy">
          <span className="study-pill">Plataforma inteligente de apoio à aprendizagem com IA</span>
          <h1>Aprenda com mais foco, prática e clareza.</h1>
          <p>
            Ratto transforma seus materiais em questões, mostra onde estão suas lacunas e ajuda a
            montar revisões mais leves para a sua rotina.
          </p>
          <div className="landing-hero-actions">
            <Link className="button-link primary" href="/app">Começar a estudar</Link>
            <a className="button-link secondary" href="#como-funciona">Ver como funciona</a>
          </div>
          <div className="study-proof">
            <span>Questões para praticar</span>
            <span>Revisão sem enrolação</span>
            <span>Foco no que falta dominar</span>
          </div>
        </div>

        <div className="study-preview" aria-label="Exemplo de jornada de estudos">
          <div className="preview-header">
            <span>Sessão de hoje</span>
            <strong>Plano rápido de revisão</strong>
          </div>
          <div className="preview-progress">
            <div>
              <span>Confiança no tema</span>
              <strong>72%</strong>
            </div>
            <div className="progress-track"><span /></div>
          </div>
          <div className="preview-topic">
            <span className="topic-icon">01</span>
            <div>
              <strong>Arquitetura de aplicações</strong>
              <p>3 conceitos para reforçar antes de avançar</p>
            </div>
          </div>
          <div className="preview-topic">
            <span className="topic-icon">02</span>
            <div>
              <strong>Fundamentos de cloud</strong>
              <p>Bom domínio nas últimas questões</p>
            </div>
          </div>
          <div className="preview-note">
            <span>Próximo passo</span>
            <strong>Responder 8 questões sobre os pontos que ainda pedem revisão.</strong>
          </div>
        </div>
      </section>

      <section className="landing-section" id="como-funciona">
        <div className="section-heading">
          <span className="eyebrow">Do material à prática</span>
          <h2>Um jeito mais direto de transformar leitura em aprendizagem ativa.</h2>
          <p>Seu material vira ponto de partida para treinar, revisar e descobrir o que ainda precisa de atenção.</p>
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
          <span className="eyebrow">Sua rotina com Ratto</span>
          <h2>Menos tempo perdido escolhendo o que estudar. Mais prática no que importa.</h2>
        </div>
        <ol>
          <li><span>1</span><div><strong>Envie seus materiais</strong><p>Use apostilas, provas e conteúdos que fazem parte da sua preparação.</p></div></li>
          <li><span>2</span><div><strong>Pratique com questões</strong><p>Revise de forma ativa com perguntas geradas a partir do seu conteúdo.</p></div></li>
          <li><span>3</span><div><strong>Encontre lacunas</strong><p>Transforme dúvidas e erros em sinais claros sobre o que precisa ser reforçado.</p></div></li>
          <li><span>4</span><div><strong>Revise com foco</strong><p>Organize as próximas sessões com objetivos menores e mais fáceis de cumprir.</p></div></li>
        </ol>
      </section>

      <section className="landing-cta">
        <span className="eyebrow">Comece pelo que você já tem</span>
        <h2>Transforme seu material em uma sessão de estudo clara, prática e possível.</h2>
        <Link className="button-link primary" href="/app">Entrar na minha área de estudos</Link>
      </section>
    </main>
  );
}
