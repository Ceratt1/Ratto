import { NextResponse } from "next/server";

import type {
  ApiError,
  ConfirmUploadCommand,
  UploadResult,
} from "@/features/pdf-upload/models/upload.models";
import { confirmUpload } from "@/features/pdf-upload/services/producer-client";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function POST(request: Request): Promise<NextResponse> {
  const authorization = request.headers.get("authorization");
  if (!authorization?.startsWith("Bearer ")) {
    return NextResponse.json({ message: "Token ausente." }, { status: 401 });
  }
  try {
    const command = (await request.json()) as ConfirmUploadCommand;
    await confirmUpload(command.uuidRequest, command.request, authorization);
    const result: UploadResult = {
      uuidUser: subjectFromToken(authorization),
      uuidRequest: command.uuidRequest,
      files: command.request.files,
      message: "Materiais recebidos. Suas questões de estudo estão sendo preparadas.",
    };
    return NextResponse.json(result, { status: 202 });
  } catch (error) {
    return errorResponse(error);
  }
}

function subjectFromToken(authorization: string): string {
  const payload = authorization.slice("Bearer ".length).split(".")[1];
  const claims = JSON.parse(Buffer.from(payload, "base64url").toString("utf8")) as { sub?: string };
  if (!claims.sub) throw new Error("Token sem subject.");
  return claims.sub;
}

function errorResponse(error: unknown): NextResponse<ApiError> {
  console.error("Could not confirm PDF upload", error);
  return NextResponse.json(
    { message: error instanceof Error ? error.message : "Falha ao confirmar uploads." },
    { status: 500 },
  );
}
