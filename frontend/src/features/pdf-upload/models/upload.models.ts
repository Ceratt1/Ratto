export interface DirectUploadFileRequest {
  fileName: string;
  contentType: "application/pdf";
}

export interface DirectUploadRequest {
  uuidUser: string;
  description?: string;
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
  uuidUser: string;
  description?: string;
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
