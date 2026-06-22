"use client";

import { Button } from "@/components/ui/button";

interface StudyAppErrorProps {
  reset: () => void;
}

export default function StudyAppError({ reset }: Readonly<StudyAppErrorProps>) {
  return (
    <div className="route-state route-state-error" role="alert">
      <div>
        <strong>Não foi possível carregar esta etapa de estudo.</strong>
        <p>Tente novamente para continuar de onde parou.</p>
      </div>
      <Button onClick={reset} type="button">Tentar novamente</Button>
    </div>
  );
}
