import type { ReactNode } from "react";

import { AuthProvider } from "@/features/auth/components/auth-provider";
import { AppShell } from "@/features/study-app/components/app-shell";

export default function StudyAppLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <AuthProvider>
      <AppShell>{children}</AppShell>
    </AuthProvider>
  );
}
