import { NextResponse } from "next/server";

import type {
  ApiError,
  DirectUploadRequest,
} from "@/features/pdf-upload/models/upload.models";
import { prepareUpload } from "@/features/pdf-upload/services/producer-client";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function POST(request: Request): Promise<NextResponse> {
  try {
    const payload = (await request.json()) as DirectUploadRequest;
    const uuidRequest = crypto.randomUUID();
    const prepared = await prepareUpload(uuidRequest, payload);
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
