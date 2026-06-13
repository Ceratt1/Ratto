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
  try {
    const command = (await request.json()) as ConfirmUploadCommand;
    await confirmUpload(command.uuidRequest, command.request);
    const result: UploadResult = {
      uuidUser: command.request.uuidUser,
      uuidRequest: command.uuidRequest,
      files: command.request.files,
      message: "Uploads confirmados. O processamento assíncrono foi iniciado.",
    };
    return NextResponse.json(result, { status: 202 });
  } catch (error) {
    return errorResponse(error);
  }
}

function errorResponse(error: unknown): NextResponse<ApiError> {
  console.error("Could not confirm PDF upload", error);
  return NextResponse.json(
    { message: error instanceof Error ? error.message : "Falha ao confirmar uploads." },
    { status: 500 },
  );
}
