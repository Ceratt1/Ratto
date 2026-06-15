import type { UploadResult } from "@/features/pdf-upload/models/upload.models";

interface UploadResultCardProps {
  result: UploadResult;
}

export function UploadResultCard({ result }: UploadResultCardProps) {
  return (
    <section className="result-card" aria-live="polite">
      <div className="result-heading">
        <span className="status-dot" />
        <div>
          <strong>Sua sessão de estudo está sendo preparada</strong>
          <p>{result.message}</p>
        </div>
      </div>

      <dl className="trace-grid">
        <div>
          <dt>Estudante</dt>
          <dd>{result.uuidUser}</dd>
        </div>
        <div>
          <dt>Sessão de estudo</dt>
          <dd>{result.uuidRequest}</dd>
        </div>
      </dl>

      <div className="result-files">
        {result.files.map((file) => (
          <div key={file.fileUuid}>
            <strong>{file.fileName}</strong>
            <code>{file.s3Path}</code>
          </div>
        ))}
      </div>
    </section>
  );
}
