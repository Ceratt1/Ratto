"use client";

export function LandingAuthActions() {
  return (
    <div className="landing-auth-actions">
      <button className="primary" onClick={() => { window.location.href = "/app"; }} type="button">
        Entrar
      </button>
    </div>
  );
}
