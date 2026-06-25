import { NextResponse } from "next/server";

const coreServiceBaseUrl = process.env.CORE_SERVICE_BASE_URL ?? "http://localhost:8071";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

interface RouteContext {
  params: Promise<{ path: string[] }>;
}

export async function GET(request: Request, context: RouteContext): Promise<Response> {
  return proxy(request, context);
}

export async function POST(request: Request, context: RouteContext): Promise<Response> {
  return proxy(request, context);
}

export async function PATCH(request: Request, context: RouteContext): Promise<Response> {
  return proxy(request, context);
}

async function proxy(request: Request, context: RouteContext): Promise<Response> {
  const authorization = request.headers.get("authorization");
  if (!authorization?.startsWith("Bearer ")) {
    return NextResponse.json({ message: "Token ausente." }, { status: 401 });
  }

  const { path } = await context.params;
  const url = new URL(request.url);
  const target = `${coreServiceBaseUrl}/api/v1/study/${path.join("/")}${url.search}`;
  const body = request.method === "GET" ? undefined : await request.text();

  try {
    const response = await fetch(target, {
      method: request.method,
      headers: {
        Authorization: authorization,
        "Content-Type": request.headers.get("content-type") ?? "application/json",
      },
      body,
      cache: "no-store",
    });
    return new Response(await response.text(), {
      status: response.status,
      headers: { "Content-Type": response.headers.get("content-type") ?? "application/json" },
    });
  } catch (error) {
    console.error("Could not proxy study request", error);
    return NextResponse.json({ message: "Falha ao atualizar seus estudos." }, { status: 502 });
  }
}
