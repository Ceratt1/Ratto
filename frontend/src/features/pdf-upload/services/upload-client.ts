import type {
  ApiError,
  ConfirmDirectUploadRequest,
  ConfirmUploadCommand,
  DirectUploadRequest,
  PreparedUpload,
  UploadResult,
} from "@/features/pdf-upload/models/upload.models";

const MAX_PDF_FILES = 1;
const MAX_PDF_SIZE_BYTES = 30 * 1024 * 1024;

export async function uploadPdfs(
  files: File[],
  description: string,
  token: string,
): Promise<UploadResult> {
  validateFiles(files);

  const prepared = await prepare({
    description: description.trim() || undefined,
    files: files.map((file) => ({
      fileName: file.name,
      contentType: "application/pdf",
      sizeBytes: file.size,
    })),
  }, token);

  await Promise.all(
    prepared.files.map((preparedFile, index) =>
      uploadFileToS3(files[index], preparedFile.uploadUrl),
    ),
  );

  const confirmRequest: ConfirmDirectUploadRequest = {
    description: description.trim() || undefined,
    files: prepared.files.map(({ fileUuid, fileName, s3Path }) => ({
      fileUuid,
      fileName,
      s3Path,
    })),
  };

  return confirm({ uuidRequest: prepared.uuidRequest, request: confirmRequest }, token);
}

async function prepare(request: DirectUploadRequest, token: string): Promise<PreparedUpload> {
  return apiRequest<PreparedUpload>("/api/uploads/prepare", request, token);
}

async function confirm(command: ConfirmUploadCommand, token: string): Promise<UploadResult> {
  return apiRequest<UploadResult>("/api/uploads/confirm", command, token);
}

async function uploadFileToS3(file: File, uploadUrl: string): Promise<void> {
  const response = await fetch(uploadUrl, {
    method: "PUT",
    headers: { "Content-Type": "application/pdf" },
    body: file,
  });
  if (!response.ok) {
    throw new Error(`S3 recusou o upload de ${file.name} (${response.status}).`);
  }
}

async function apiRequest<T extends object>(path: string, body: object, token: string): Promise<T> {
  const response = await fetch(path, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const payload = (await response.json()) as T | ApiError;
  if (!response.ok) {
    throw new Error("message" in payload ? payload.message : "Falha na integração.");
  }
  return payload as T;
}

function validateFiles(files: File[]): void {
  if (files.length !== MAX_PDF_FILES) {
    throw new Error("Selecione exatamente um arquivo PDF.");
  }
  for (const file of files) {
    if (file.type !== "application/pdf" || !file.name.toLowerCase().endsWith(".pdf")) {
      throw new Error(`${file.name} não é um PDF válido.`);
    }
    if (file.size > MAX_PDF_SIZE_BYTES) {
      throw new Error(`${file.name} excede o limite de 30 MB.`);
    }
  }
}
