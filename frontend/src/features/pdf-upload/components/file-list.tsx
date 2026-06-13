interface FileListProps {
  files: File[];
  disabled: boolean;
  onRemove: (index: number) => void;
}

export function FileList({ files, disabled, onRemove }: FileListProps) {
  if (files.length === 0) {
    return <p className="empty-state">Nenhum PDF selecionado.</p>;
  }

  return (
    <div className="file-list">
      {files.map((file, index) => (
        <article className="file-card" key={`${file.name}-${file.lastModified}`}>
          <div className="file-icon">PDF</div>
          <div className="file-details">
            <strong>{file.name}</strong>
            <span>{formatBytes(file.size)}</span>
          </div>
          <button
            aria-label={`Remover ${file.name}`}
            className="remove-button"
            disabled={disabled}
            onClick={() => onRemove(index)}
            type="button"
          >
            Remover
          </button>
        </article>
      ))}
    </div>
  );
}

function formatBytes(bytes: number): string {
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
