import { ChevronLeft, ChevronRight, Move, ScanText, Sparkles } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { getDocument, GlobalWorkerOptions, type PDFDocumentProxy, type RenderTask } from "pdfjs-dist/legacy/build/pdf.mjs";
import pdfWorkerUrl from "pdfjs-dist/build/pdf.worker.min.mjs?url";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { SignatureAppearanceOverride } from "@/types/signing";

GlobalWorkerOptions.workerSrc = pdfWorkerUrl;

type PdfSignaturePlacementEditorProps = {
  disabled?: boolean;
  hasSignatureImage: boolean;
  pdfBlob: Blob;
  signaturePreviewUrl: string | null;
  value: SignatureAppearanceOverride;
  onChange: (value: SignatureAppearanceOverride) => void;
};

type DragMode = "move" | "resize";

type DragState = {
  mode: DragMode;
  startX: number;
  startY: number;
  startValue: Required<Pick<SignatureAppearanceOverride, "xPercent" | "yPercent" | "widthPercent" | "heightPercent">>;
};

const MAX_PREVIEW_WIDTH = 640;
const MIN_BOX_PERCENT = 8;

export function PdfSignaturePlacementEditor({
  disabled = false,
  hasSignatureImage,
  pdfBlob,
  signaturePreviewUrl,
  value,
  onChange,
}: PdfSignaturePlacementEditorProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const dragStateRef = useRef<DragState | null>(null);
  const appearanceRef = useRef<SignatureAppearanceOverride>(value);
  const [pdfDocument, setPdfDocument] = useState<PDFDocumentProxy | null>(null);
  const [pageCount, setPageCount] = useState(0);
  const [renderSize, setRenderSize] = useState({ width: 0, height: 0 });
  const [previewError, setPreviewError] = useState<string | null>(null);

  appearanceRef.current = value;

  const normalizedValue = useMemo(
    () => ({
      pageNumber: value.pageNumber ?? undefined,
      xPercent: clampPercent(value.xPercent ?? 62, 0, 92),
      yPercent: clampPercent(value.yPercent ?? 4, 0, 92),
      widthPercent: clampPercent(value.widthPercent ?? 34, MIN_BOX_PERCENT, 100),
      heightPercent: clampPercent(value.heightPercent ?? 16, MIN_BOX_PERCENT, 100),
      imageOnly: Boolean(value.imageOnly) && hasSignatureImage,
    }),
    [hasSignatureImage, value.heightPercent, value.imageOnly, value.pageNumber, value.widthPercent, value.xPercent, value.yPercent],
  );

  useEffect(() => {
    let cancelled = false;

    async function loadPdf() {
      setPreviewError(null);
      setPdfDocument(null);
      setPageCount(0);

      try {
        const bytes = new Uint8Array(await pdfBlob.arrayBuffer());
        const loadingTask = getDocument({ data: bytes });
        const document = await loadingTask.promise;
        if (cancelled) {
          void document.destroy();
          return;
        }
        setPdfDocument(document);
        setPageCount(document.numPages);
        if (!appearanceRef.current.pageNumber) {
          onChange({ ...appearanceRef.current, pageNumber: document.numPages });
        } else if ((appearanceRef.current.pageNumber || 1) > document.numPages) {
          onChange({ ...appearanceRef.current, pageNumber: document.numPages });
        }
      } catch (error) {
        if (!cancelled) {
          setPreviewError(error instanceof Error ? error.message : "Khong the tai preview PDF.");
        }
      }
    }

    void loadPdf();
    return () => {
      cancelled = true;
    };
  }, [onChange, pdfBlob]);

  useEffect(() => {
    if (!pdfDocument || !canvasRef.current) {
      return;
    }

    const currentPdfDocument = pdfDocument;
    let cancelled = false;
    let renderTask: RenderTask | null = null;

    async function renderPage() {
      try {
        const pageNumber = resolvePageNumber(normalizedValue.pageNumber, pageCount);
        const page = await currentPdfDocument.getPage(pageNumber);
        const baseViewport = page.getViewport({ scale: 1 });
        const scale = MAX_PREVIEW_WIDTH / baseViewport.width;
        const viewport = page.getViewport({ scale });
        const canvas = canvasRef.current;
        if (!canvas || cancelled) {
          return;
        }

        const context = canvas.getContext("2d");
        if (!context) {
          setPreviewError("Khong the khoi tao canvas preview.");
          return;
        }

        canvas.width = viewport.width;
        canvas.height = viewport.height;
        canvas.style.width = `${viewport.width}px`;
        canvas.style.height = `${viewport.height}px`;
        setRenderSize({ width: viewport.width, height: viewport.height });

        renderTask = page.render({ canvasContext: context, viewport });
        await renderTask.promise;
      } catch (error) {
        if (!cancelled) {
          setPreviewError(error instanceof Error ? error.message : "Khong the render preview PDF.");
        }
      }
    }

    void renderPage();
    return () => {
      cancelled = true;
      renderTask?.cancel();
    };
  }, [normalizedValue.pageNumber, pageCount, pdfDocument]);

  useEffect(() => {
    function handlePointerMove(event: PointerEvent) {
      const dragState = dragStateRef.current;
      if (!dragState || renderSize.width <= 0 || renderSize.height <= 0) {
        return;
      }

      const deltaXPercent = ((event.clientX - dragState.startX) / renderSize.width) * 100;
      const deltaYPercent = (-(event.clientY - dragState.startY) / renderSize.height) * 100;

      const nextXBase = dragState.startValue.xPercent;
      const nextYBase = dragState.startValue.yPercent;
      const nextWidthBase = dragState.startValue.widthPercent;
      const nextHeightBase = dragState.startValue.heightPercent;

      if (dragState.mode === "move") {
        const nextWidth = nextWidthBase;
        const nextHeight = nextHeightBase;
        onChange({
          ...appearanceRef.current,
          xPercent: clampPercent(nextXBase + deltaXPercent, 0, 100 - nextWidth),
          yPercent: clampPercent(nextYBase + deltaYPercent, 0, 100 - nextHeight),
        });
        return;
      }

      const nextWidth = clampPercent(nextWidthBase + deltaXPercent, MIN_BOX_PERCENT, 100 - nextXBase);
      const nextHeight = clampPercent(nextHeightBase + deltaYPercent, MIN_BOX_PERCENT, 100 - nextYBase);
      onChange({
        ...appearanceRef.current,
        widthPercent: nextWidth,
        heightPercent: nextHeight,
      });
    }

    function handlePointerUp() {
      dragStateRef.current = null;
    }

    window.addEventListener("pointermove", handlePointerMove);
    window.addEventListener("pointerup", handlePointerUp);
    return () => {
      window.removeEventListener("pointermove", handlePointerMove);
      window.removeEventListener("pointerup", handlePointerUp);
    };
  }, [onChange, renderSize.height, renderSize.width]);

  const pageNumber = resolvePageNumber(normalizedValue.pageNumber, pageCount);
  const canGoPrev = pageCount > 0 && pageNumber > 1;
  const canGoNext = pageCount > 0 && pageNumber < pageCount;

  return (
    <div className="space-y-5 rounded-[24px] border border-slate-200/80 bg-white/90 p-5 shadow-sm">
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <h3 className="text-lg font-semibold text-slate-950">Dat vi tri chu ky tren PDF ngay luc ky</h3>
          <p className="text-sm leading-6 text-slate-600">
            Kéo hộp chữ ký ngay trên preview tài liệu, chọn trang cần in và chỉnh kích thước trước khi bấm <span className="font-semibold text-slate-900">Execute signing</span>.
          </p>
        </div>
        <span className="inline-flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-100 text-slate-700">
          <Sparkles className="h-5 w-5" />
        </span>
      </div>

      <div className="grid gap-5 xl:grid-cols-[0.92fr_1.08fr]">
        <div className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <label className="space-y-2 text-sm font-medium text-slate-900">
              <span>Trang PDF</span>
              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  className="w-10 px-0"
                  size="sm"
                  variant="outline"
                  disabled={disabled || !canGoPrev}
                  onClick={() => onChange({ ...appearanceRef.current, pageNumber: pageNumber - 1 })}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <Input
                  disabled={disabled || pageCount === 0}
                  max={Math.max(1, pageCount)}
                  min={1}
                  type="number"
                  value={pageNumber}
                  onChange={(event) =>
                    onChange({
                      ...appearanceRef.current,
                      pageNumber: clampPercent(Number(event.target.value) || 1, 1, Math.max(1, pageCount)),
                    })
                  }
                />
                <Button
                  type="button"
                  className="w-10 px-0"
                  size="sm"
                  variant="outline"
                  disabled={disabled || !canGoNext}
                  onClick={() => onChange({ ...appearanceRef.current, pageNumber: pageNumber + 1 })}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
              <p className="text-xs font-normal text-slate-500">Tong cong {pageCount || "-"} trang.</p>
            </label>

            <label className="space-y-2 text-sm font-medium text-slate-900">
              <span>Hien thi</span>
              <label className="flex min-h-11 items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 text-sm font-medium text-slate-900">
                <input
                  checked={normalizedValue.imageOnly}
                  className="h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary/30"
                  disabled={disabled || !hasSignatureImage}
                  type="checkbox"
                  onChange={(event) => onChange({ ...appearanceRef.current, imageOnly: event.target.checked })}
                />
                Chi hien hinh anh
              </label>
              {!hasSignatureImage ? (
                <p className="text-xs font-normal text-amber-700">Ban chua tai anh chu ky, nen che do nay tam thoi bi khoa.</p>
              ) : null}
            </label>

            <NumericField
              disabled={disabled}
              label="X (%)"
              max={100 - normalizedValue.widthPercent}
              min={0}
              value={normalizedValue.xPercent}
              onChange={(next) => onChange({ ...appearanceRef.current, xPercent: next })}
            />
            <NumericField
              disabled={disabled}
              label="Y (%)"
              max={100 - normalizedValue.heightPercent}
              min={0}
              value={normalizedValue.yPercent}
              onChange={(next) => onChange({ ...appearanceRef.current, yPercent: next })}
            />
            <NumericField
              disabled={disabled}
              label="Width (%)"
              max={100 - normalizedValue.xPercent}
              min={MIN_BOX_PERCENT}
              value={normalizedValue.widthPercent}
              onChange={(next) => onChange({ ...appearanceRef.current, widthPercent: next })}
            />
            <NumericField
              disabled={disabled}
              label="Height (%)"
              max={100 - normalizedValue.yPercent}
              min={MIN_BOX_PERCENT}
              value={normalizedValue.heightPercent}
              onChange={(next) => onChange({ ...appearanceRef.current, heightPercent: next })}
            />
          </div>

          <div className="rounded-[20px] border border-slate-200/80 bg-slate-50/80 p-4 text-sm leading-6 text-slate-600">
            <p className="font-medium text-slate-900">Cach thao tac</p>
            <p className="mt-2">Keo hop chu ky de di chuyen. Keo nut o goc tren ben phai de doi kich thuoc. Toa do duoc tinh theo % so voi trang PDF goc.</p>
          </div>
        </div>

        <div className="rounded-[22px] border border-slate-200/80 bg-[linear-gradient(180deg,rgba(248,250,252,0.96),rgba(241,245,249,0.92))] p-4">
          <div className="mb-4 flex items-center justify-between gap-3">
            <div>
              <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Preview</p>
              <p className="mt-1 text-sm text-slate-600">Trang {pageNumber} / {pageCount || "-"}</p>
            </div>
            <span className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-600">
              <Move className="h-3.5 w-3.5" />
              Keo de dat vi tri
            </span>
          </div>

          {previewError ? (
            <div className="rounded-[20px] border border-red-200/80 bg-red-50/85 p-4 text-sm leading-6 text-red-700">
              {previewError}
            </div>
          ) : (
            <div className="overflow-auto rounded-[20px] border border-slate-200 bg-white p-4">
              <div className="mx-auto w-fit">
                <div className="relative overflow-hidden rounded-[18px] border border-slate-200 bg-white shadow-sm">
                  <canvas ref={canvasRef} className="block" />
                  {renderSize.width > 0 && renderSize.height > 0 ? (
                    <div
                      className={`absolute overflow-hidden rounded-xl ${disabled ? "cursor-default" : "cursor-grab active:cursor-grabbing"} ${normalizedValue.imageOnly ? "bg-transparent" : "border border-slate-300 bg-white/95 shadow-lg"}`}
                      style={{
                        left: `${normalizedValue.xPercent}%`,
                        bottom: `${normalizedValue.yPercent}%`,
                        width: `${normalizedValue.widthPercent}%`,
                        height: `${normalizedValue.heightPercent}%`,
                      }}
                      onPointerDown={(event) => {
                        if (disabled) return;
                        const target = event.target as HTMLElement;
                        const mode = target.dataset.resizeHandle === "true" ? "resize" : "move";
                        dragStateRef.current = {
                          mode,
                          startX: event.clientX,
                          startY: event.clientY,
                          startValue: {
                            xPercent: normalizedValue.xPercent,
                            yPercent: normalizedValue.yPercent,
                            widthPercent: normalizedValue.widthPercent,
                            heightPercent: normalizedValue.heightPercent,
                          },
                        };
                      }}
                    >
                      <div className="relative flex h-full w-full items-center justify-center p-2">
                        {signaturePreviewUrl ? (
                          <img alt="Signature preview" className="max-h-full w-auto object-contain select-none" draggable={false} src={signaturePreviewUrl} />
                        ) : (
                          <div className="flex h-full w-full items-center justify-center rounded-lg bg-slate-50 text-center text-[11px] font-medium leading-4 text-slate-500">
                            {normalizedValue.imageOnly ? "Anh chu ky" : "Vung chu ky"}
                          </div>
                        )}
                        <button
                          aria-label="Resize signature box"
                          className="absolute right-1.5 top-1.5 flex h-5 w-5 cursor-nwse-resize items-center justify-center rounded-full border border-slate-300 bg-white text-slate-500 shadow-sm"
                          data-resize-handle="true"
                          type="button"
                        >
                          <ScanText className="h-3 w-3" />
                        </button>
                      </div>
                      {!normalizedValue.imageOnly ? (
                        <div className="border-t border-slate-200/80 bg-slate-50/90 px-2 py-1 text-[9px] leading-3 text-slate-500">
                          signer / serial / time
                        </div>
                      ) : null}
                    </div>
                  ) : null}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function NumericField({
  disabled,
  label,
  min,
  max,
  value,
  onChange,
}: {
  disabled?: boolean;
  label: string;
  min: number;
  max: number;
  value: number;
  onChange: (value: number) => void;
}) {
  return (
    <label className="space-y-2 text-sm font-medium text-slate-900">
      <span>{label}</span>
      <Input
        disabled={disabled}
        max={Math.max(min, Math.floor(max))}
        min={Math.floor(min)}
        type="number"
        value={value}
        onChange={(event) => onChange(clampPercent(Number(event.target.value) || min, min, Math.max(min, max)))}
      />
    </label>
  );
}

function resolvePageNumber(pageNumber: number | undefined, pageCount: number) {
  if (pageCount <= 0) {
    return 1;
  }
  if (!pageNumber) {
    return pageCount;
  }
  return clampPercent(pageNumber, 1, pageCount);
}

function clampPercent(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}
