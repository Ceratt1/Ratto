import type {
  ApiError,
  ConfirmDirectUploadRequest,
  ConfirmUploadCommand,
  DirectUploadRequest,
  PreparedUpload,
  UploadResult,
} from "@/features/pdf-upload/models/upload.models";

export async function uploadPdfs(
  files: File[],
  description: string,
): Promise<UploadResult> {
  validateFiles(files);

  const uuidUser = getOrCreateUserUuid();
  const prepared = await prepare({
    uuidUser,
    description: description.trim() || undefined,
    files: files.map((file) => ({
      fileName: file.name,
      contentType: "application/pdf",
    })),
  });

  await Promise.all(
    prepared.files.map((preparedFile, index) =>
      uploadFileToS3(files[index], preparedFile.uploadUrl),
    ),
  );

  const confirmRequest: ConfirmDirectUploadRequest = {
    uuidUser,
    description: description.trim() || undefined,
    files: prepared.files.map(({ fileUuid, fileName, s3Path }) => ({
      fileUuid,
      fileName,
      s3Path,
    })),
  };

  return confirm({ uuidRequest: prepared.uuidRequest, request: confirmRequest });
}

async function prepare(request: DirectUploadRequest): Promise<PreparedUpload> {
  return apiRequest<PreparedUpload>("/api/uploads/prepare", request);
}

async function confirm(command: ConfirmUploadCommand): Promise<UploadResult> {
  return apiRequest<UploadResult>("/api/uploads/confirm", command);
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

async function apiRequest<T extends object>(path: string, body: object): Promise<T> {
  const response = await fetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const payload = (await response.json()) as T | ApiError;
  if (!response.ok) {
    throw new Error("message" in payload ? payload.message : "Falha na integração.");
  }
  return payload as T;
}

function getOrCreateUserUuid(): string {
  const storageKey = "learn-ia-user-uuid";
  const existing = window.localStorage.getItem(storageKey);
  if (existing) {
    return existing;
  }
  const created = crypto.randomUUID();
  window.localStorage.setItem(storageKey, created);
  return created;
}

function validateFiles(files: File[]): void {
  if (files.length < 1 || files.length > 2) {
    throw new Error("Selecione um ou dois arquivos PDF.");
  }
  for (const file of files) {
    if (file.type !== "application/pdf" || !file.name.toLowerCase().endsWith(".pdf")) {
      throw new Error(`${file.name} não é um PDF válido.`);
    }
    if (file.size > 100 * 1024 * 1024) {
      throw new Error(`${file.name} excede o limite de 100 MB.`);
    }
  }
}
