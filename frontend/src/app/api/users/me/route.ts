import { NextResponse } from "next/server";

const coreServiceBaseUrl = process.env.CORE_SERVICE_BASE_URL ?? "http://localhost:8071";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function PUT(request: Request): Promise<Response> {
  const authorization = request.headers.get("authorization");
  if (!authorization?.startsWith("Bearer ")) {
    return NextResponse.json({ message: "Token ausente." }, { status: 401 });
  }
  try {
    const response = await fetch(`${coreServiceBaseUrl}/api/v1/users/me`, {
      method: "PUT",
      headers: { Authorization: authorization },
      cache: "no-store",
    });
    return new Response(await response.text(), {
      status: response.status,
      headers: { "Content-Type": "application/json" },
    });
  } catch (error) {
    console.error("Could not synchronize user profile", error);
    return NextResponse.json({ message: "Falha ao sincronizar perfil." }, { status: 502 });
  }
}
