"use client";

import { ChangeEvent, FormEvent, useRef, useState } from "react";

import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/components/auth-provider";
import { FileList } from "@/features/pdf-upload/components/file-list";
import { UploadResultCard } from "@/features/pdf-upload/components/upload-result-card";
import type {
  StudyLanguage,
  UploadResult,
} from "@/features/pdf-upload/models/upload.models";
import { uploadPdfs } from "@/features/pdf-upload/services/upload-client";

const MAX_FILES = 1;
const MAX_FILE_SIZE_BYTES = 30 * 1024 * 1024;
const LANGUAGE_OPTIONS: Array<{ value: StudyLanguage; flag: string; label: string }> = [
  { value: "pt-BR", flag: "🇧🇷", label: "Português" },
  { value: "en", flag: "🇺🇸", label: "Inglês" },
  { value: "es", flag: "🇪🇸", label: "Espanhol" },
];

interface UploadFormProps {
  workspaceId?: string;
  onUploaded?: (result: UploadResult) => void;
}

export function UploadForm({ workspaceId, onUploaded }: UploadFormProps) {
  const { authenticated, getToken, initialized } = useAuth();
  const inputRef = useRef<HTMLInputElement>(null);
  const [files, setFiles] = useState<File[]>([]);
  const [description, setDescription] = useState("");
  const [studyLanguage, setStudyLanguage] = useState<StudyLanguage>("pt-BR");
  const [status, setStatus] = useState<"idle" | "uploading" | "success" | "error">("idle");
  const [error, setError] = useState("");
  const [result, setResult] = useState<UploadResult | null>(null);

  function handleFiles(event: ChangeEvent<HTMLInputElement>) {
    const selected = Array.from(event.target.files ?? []);
    setError("");
    setResult(null);

    if (selected.length > MAX_FILES) {
      setFiles(selected.slice(0, MAX_FILES));
      setError("Envie apenas um PDF por vez.");
      return;
    }
    const oversizedFile = selected.find((file) => file.size > MAX_FILE_SIZE_BYTES);
    if (oversizedFile) {
      setFiles([]);
      setError(`${oversizedFile.name} excede o limite de 30 MB.`);
      return;
    }
    setFiles(selected);
  }

  function removeFile(index: number) {
    const remaining = files.filter((_, fileIndex) => fileIndex !== index);
    setFiles(remaining);
    if (inputRef.current) {
      inputRef.current.value = "";
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setResult(null);

    if (files.length !== MAX_FILES) {
      setError("Selecione exatamente um arquivo PDF.");
      return;
    }

    setStatus("uploading");
    try {
      const payload = await uploadPdfs(files, description, studyLanguage, await getToken(), workspaceId);
      setResult(payload);
      setStatus("success");
      setFiles([]);
      onUploaded?.(payload);
      if (inputRef.current) {
        inputRef.current.value = "";
      }
    } catch (requestError) {
      setStatus("error");
      setError(requestError instanceof Error ? requestError.message : "Falha inesperada.");
    }
  }

  const uploading = status === "uploading";

  return (
    <div className="upload-layout compact-upload">
      <form className="upload-panel" onSubmit={submit}>
        <div className="field">
          <label htmlFor="description">Objetivo do estudo</label>
          <textarea
            disabled={uploading || !authenticated}
            id="description"
            maxLength={200}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="Ex.: revisar os conceitos que mais caem na prova"
            rows={3}
            value={description}
          />
          <span className="field-hint">{description.length}/200</span>
        </div>

        <div className="field">
          <span className="field-label">Idioma da prova</span>
          <div className="language-segmented" role="radiogroup" aria-label="Idioma da prova">
            {LANGUAGE_OPTIONS.map((option) => (
              <button
                aria-checked={studyLanguage === option.value}
                className={studyLanguage === option.value ? "active" : ""}
                disabled={uploading || !authenticated}
                key={option.value}
                onClick={() => setStudyLanguage(option.value)}
                role="radio"
                type="button"
              >
                <span className="language-flag" aria-hidden="true">{option.flag}</span>
                {option.label}
              </button>
            ))}
          </div>
        </div>

        <div className="field">
          <div className="field-title">
            <label htmlFor="files">Arquivos PDF</label>
            <span>{files.length}/1 selecionado</span>
          </div>
          <label className={`drop-zone ${uploading || !authenticated ? "disabled" : ""}`} htmlFor="files">
            <strong>Escolha um PDF</strong>
            <span>Até 30 MB</span>
          </label>
          <input
            accept="application/pdf,.pdf"
            className="sr-only"
            disabled={uploading || !authenticated}
            id="files"
            onChange={handleFiles}
            ref={inputRef}
            type="file"
          />
        </div>

        <FileList disabled={uploading} files={files} onRemove={removeFile} />

        {!authenticated && initialized && <p className="error-message">Entre para enviar PDFs.</p>}
        {error && <p className="error-message">{error}</p>}

        <Button disabled={!authenticated || uploading || files.length === 0} type="submit">
          {uploading ? "Preparando..." : "Gerar prova"}
        </Button>
      </form>

      {result && <UploadResultCard result={result} />}
    </div>
  );
}
