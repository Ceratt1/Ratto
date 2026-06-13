"use client";

import { ChangeEvent, FormEvent, useRef, useState } from "react";

import { Button } from "@/components/ui/button";
import { FileList } from "@/features/pdf-upload/components/file-list";
import { UploadResultCard } from "@/features/pdf-upload/components/upload-result-card";
import type {
  UploadResult,
} from "@/features/pdf-upload/models/upload.models";
import { uploadPdfs } from "@/features/pdf-upload/services/upload-client";

const MAX_FILES = 2;

export function UploadForm() {
  const inputRef = useRef<HTMLInputElement>(null);
  const [files, setFiles] = useState<File[]>([]);
  const [description, setDescription] = useState("");
  const [status, setStatus] = useState<"idle" | "uploading" | "success" | "error">("idle");
  const [error, setError] = useState("");
  const [result, setResult] = useState<UploadResult | null>(null);

  function handleFiles(event: ChangeEvent<HTMLInputElement>) {
    const selected = Array.from(event.target.files ?? []);
    setError("");
    setResult(null);

    if (selected.length > MAX_FILES) {
      setFiles(selected.slice(0, MAX_FILES));
      setError("Somente os dois primeiros PDFs foram selecionados.");
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

    if (files.length < 1 || files.length > MAX_FILES) {
      setError("Selecione um ou dois arquivos PDF.");
      return;
    }

    setStatus("uploading");
    try {
      const payload = await uploadPdfs(files, description);
      setResult(payload);
      setStatus("success");
      setFiles([]);
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
    <div className="upload-layout">
      <form className="upload-panel" onSubmit={submit}>
        <div className="field">
          <label htmlFor="description">Objetivo do estudo</label>
          <textarea
            disabled={uploading}
            id="description"
            maxLength={200}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="Ex.: gerar questões para revisar os principais conceitos"
            rows={3}
            value={description}
          />
          <span className="field-hint">{description.length}/200</span>
        </div>

        <div className="field">
          <div className="field-title">
            <label htmlFor="files">Arquivos PDF</label>
            <span>{files.length}/2 selecionados</span>
          </div>
          <label className={`drop-zone ${uploading ? "disabled" : ""}`} htmlFor="files">
            <strong>Escolha um ou dois PDFs</strong>
            <span>Até 100 MB por arquivo</span>
          </label>
          <input
            accept="application/pdf,.pdf"
            className="sr-only"
            disabled={uploading}
            id="files"
            multiple
            onChange={handleFiles}
            ref={inputRef}
            type="file"
          />
        </div>

        <FileList disabled={uploading} files={files} onRemove={removeFile} />

        {error && <p className="error-message">{error}</p>}

        <Button disabled={uploading || files.length === 0} type="submit">
          {uploading ? "Enviando e iniciando pipeline..." : "Enviar PDFs"}
        </Button>
      </form>

      <aside className="pipeline-panel">
        <span className="eyebrow">Pipeline assíncrono</span>
        <h2>Do PDF até as questões</h2>
        <ol className="pipeline-list">
          <li>
            <span>1</span>
            <div>
              <strong>Upload seguro</strong>
              <p>O producer prepara os paths e o arquivo original vai para o S3.</p>
            </div>
          </li>
          <li>
            <span>2</span>
            <div>
              <strong>Extração</strong>
              <p>O worker lê o PDF e salva o texto extraído na mesma pasta.</p>
            </div>
          </li>
          <li>
            <span>3</span>
            <div>
              <strong>Geração com Gemini</strong>
              <p>As questões e explicações são geradas no idioma do documento.</p>
            </div>
          </li>
        </ol>
        <div className="user-id">
          <span>Identificador do usuário</span>
          <code>{result?.uuidUser ?? "será gerado no envio"}</code>
        </div>
      </aside>

      {result && <UploadResultCard result={result} />}
    </div>
  );
}
