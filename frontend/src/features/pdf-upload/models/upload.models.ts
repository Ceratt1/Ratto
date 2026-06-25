export interface DirectUploadFileRequest {
  fileName: string;
  contentType: "application/pdf";
  sizeBytes: number;
}

export interface DirectUploadRequest {
  workspaceId?: string;
  description?: string;
  studyLanguage: StudyLanguage;
  files: DirectUploadFileRequest[];
}

export interface PreparedFileUpload {
  fileUuid: string;
  fileName: string;
  s3Path: string;
  uploadUrl: string;
  contentType: "application/pdf";
}

export interface PreparedUpload {
  uuidRequest: string;
  uuidUser: string;
  files: PreparedFileUpload[];
}

export interface ConfirmFileUploadRequest {
  fileUuid: string;
  fileName: string;
  s3Path: string;
}

export interface ConfirmDirectUploadRequest {
  workspaceId?: string;
  description?: string;
  studyLanguage: StudyLanguage;
  files: ConfirmFileUploadRequest[];
}

export interface ConfirmUploadCommand {
  uuidRequest: string;
  request: ConfirmDirectUploadRequest;
}

export interface UploadResult {
  uuidUser: string;
  uuidRequest: string;
  files: ConfirmFileUploadRequest[];
  message: string;
}

export interface ApiError {
  message: string;
}

export type StudyLanguage = "pt-BR" | "en" | "es";
