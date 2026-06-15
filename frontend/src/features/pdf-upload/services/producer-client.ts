import {
  ConfirmDirectUploadRequest,
  DirectUploadRequest,
  PreparedUpload,
} from "@/features/pdf-upload/models/upload.models";

const producerBaseUrl = process.env.PRODUCER_BASE_URL ?? "http://localhost:8070/api";

export async function prepareUpload(
  uuidRequest: string,
  request: DirectUploadRequest,
  authorization: string,
): Promise<PreparedUpload> {
  return producerRequest<PreparedUpload>(`/v1/receiver/${uuidRequest}/uploads`, {
    method: "POST",
    headers: { Authorization: authorization, "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export async function confirmUpload(
  uuidRequest: string,
  request: ConfirmDirectUploadRequest,
  authorization: string,
): Promise<void> {
  await producerRequest<void>(`/v1/receiver/${uuidRequest}/confirm`, {
    method: "POST",
    headers: { Authorization: authorization, "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

async function producerRequest<T>(path: string, init: RequestInit): Promise<T> {
  const response = await fetch(`${producerBaseUrl}${path}`, {
    ...init,
    cache: "no-store",
  });

  if (!response.ok) {
    const details = await response.text();
    throw new Error(`Producer retornou ${response.status}: ${details || response.statusText}`);
  }

  if (response.status === 204 || response.status === 202) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
