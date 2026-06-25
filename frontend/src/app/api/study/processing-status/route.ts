import { NextResponse } from "next/server";

const eventLedgerBaseUrl = process.env.EVENT_LEDGER_BASE_URL ?? "http://localhost:9090";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(request: Request): Promise<Response> {
  const authorization = request.headers.get("authorization");
  if (!authorization?.startsWith("Bearer ")) {
    return NextResponse.json({ message: "Token ausente." }, { status: 401 });
  }

  const url = new URL(request.url);
  const fileUuid = url.searchParams.get("fileUuid");
  if (!fileUuid) {
    return NextResponse.json({ message: "Prova não encontrada." }, { status: 400 });
  }

  try {
    const uuidUser = subjectFromToken(authorization);
    const response = await fetch(
      `${eventLedgerBaseUrl}/internal/v1/ingestion-status/${encodeURIComponent(fileUuid)}?uuidUser=${encodeURIComponent(uuidUser)}`,
      { cache: "no-store" },
    );
    return new Response(await response.text(), {
      status: response.status,
      headers: { "Content-Type": response.headers.get("content-type") ?? "application/json" },
    });
  } catch (error) {
    console.error("Could not fetch processing status", error);
    return NextResponse.json({ message: "Falha ao consultar o status da prova." }, { status: 502 });
  }
}

function subjectFromToken(authorization: string): string {
  const payload = authorization.slice("Bearer ".length).split(".")[1];
  const claims = JSON.parse(Buffer.from(payload, "base64url").toString("utf8")) as { sub?: string };
  if (!claims.sub) throw new Error("Token sem subject.");
  return claims.sub;
}
