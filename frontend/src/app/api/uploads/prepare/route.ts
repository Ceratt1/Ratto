import { NextResponse } from "next/server";

import type {
  ApiError,
  DirectUploadRequest,
} from "@/features/pdf-upload/models/upload.models";
import { prepareUpload } from "@/features/pdf-upload/services/producer-client";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function POST(request: Request): Promise<NextResponse> {
  const authorization = request.headers.get("authorization");
  if (!authorization?.startsWith("Bearer ")) {
    return NextResponse.json({ message: "Token ausente." }, { status: 401 });
  }
  try {
    const payload = (await request.json()) as DirectUploadRequest;
    const uuidRequest = crypto.randomUUID();
    const prepared = await prepareUpload(uuidRequest, payload, authorization);
    return NextResponse.json(prepared);
  } catch (error) {
    return errorResponse(error);
  }
}

function errorResponse(error: unknown): NextResponse<ApiError> {
  console.error("Could not prepare PDF upload", error);
  return NextResponse.json(
    { message: error instanceof Error ? error.message : "Falha ao preparar uploads." },
    { status: 500 },
  );
}
