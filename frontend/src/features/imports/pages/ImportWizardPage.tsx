import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { PageHeader } from '@/components/layout/PageHeader'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { ArrowLeft, Check, CheckCircle, Loader2, Upload } from 'lucide-react'
import { useCallback, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import {
  useConfirmMappings,
  useImportBatch,
  useImportPreview,
  useMappingSuggestions,
  useStartProcessing,
  useUploadImport,
} from '../hooks'
import type { ColumnMapping, MappingSuggestion } from '../types'

type WizardStep = 'upload' | 'mapping' | 'preview' | 'processing' | 'complete'

export function ImportWizardPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState<WizardStep>('upload')
  const [batchId, setBatchId] = useState<string>()
  const [mappings, setMappings] = useState<ColumnMapping[]>([])

  const uploadMutation = useUploadImport()
  const fileRef = useRef<HTMLInputElement>(null)

  const { data: batch } = useImportBatch(batchId)
  const { data: suggestions, isLoading: suggestionsLoading } = useMappingSuggestions(
    step === 'mapping' ? batchId : undefined,
  )
  const confirmMutation = useConfirmMappings(batchId ?? '')
  const { data: preview, isLoading: previewLoading } = useImportPreview(
    batchId,
    step === 'preview',
  )
  const startMutation = useStartProcessing(batchId ?? '')

  const handleUpload = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    uploadMutation.mutate(
      { file, source: 'EXCEL_CSV' },
      {
        onSuccess: (result) => {
          setBatchId(result.id)
          setStep('mapping')
        },
      },
    )
  }, [uploadMutation])

  const initMappings = useCallback((suggestions: MappingSuggestion[]) => {
    setMappings(
      suggestions.map((s) => ({
        source_column: s.source_column,
        target_field: s.target_field,
      })),
    )
  }, [])

  if (suggestions && mappings.length === 0) {
    initMappings(suggestions.suggestions)
  }

  const handleConfirmMappings = () => {
    confirmMutation.mutate(mappings, {
      onSuccess: () => setStep('preview'),
    })
  }

  const handleStartProcessing = () => {
    startMutation.mutate(undefined, {
      onSuccess: () => setStep('processing'),
    })
  }

  const updateMapping = (sourceColumn: string, targetField: string | null) => {
    setMappings((prev) =>
      prev.map((m) =>
        m.source_column === sourceColumn ? { ...m, target_field: targetField } : m,
      ),
    )
  }

  if (step === 'processing' && batch?.status === 'COMPLETED') {
    setStep('complete')
  }

  const stepList = ['upload', 'mapping', 'preview', 'processing', 'complete'] as WizardStep[]
  const currentIdx = stepList.indexOf(step)

  return (
    <div className="space-y-6">
      <PageHeader
        breadcrumb={<Link to="/imports" className="inline-flex items-center gap-1 hover:text-foreground transition-colors duration-200"><ArrowLeft className="size-3.5" /> Back to imports</Link>}
        title="Import data"
        description="Upload a file, map its columns, preview the outcome, then process. Skip-on-error is on by default."
      />

      {/* Step indicator */}
      <div className="rounded-xl border border-border bg-card p-4 shadow-card">
        <ol className="flex items-center gap-1">
          {stepList.map((s, i) => {
            const state = i === currentIdx ? 'current' : i < currentIdx ? 'done' : 'todo'
            return (
              <li key={s} className="flex items-center gap-1 shrink-0">
                <div className={
                  'flex size-7 items-center justify-center rounded-full text-[11px] font-bold transition-all duration-300 ' +
                  (state === 'done' ? 'bg-accent-emerald text-white shadow-sm' :
                   state === 'current' ? 'bg-primary text-primary-foreground shadow-md ring-4 ring-primary/15' :
                   'bg-surface-1 text-muted-foreground border border-border')
                }>
                  {state === 'done' ? <Check className="size-3.5" /> : i + 1}
                </div>
                <span className={
                  'text-xs font-medium capitalize ' +
                  (state === 'current' ? 'text-foreground' : state === 'done' ? 'text-accent-emerald' : 'text-muted-foreground')
                }>{s}</span>
                {i < stepList.length - 1 && (
                  <div className={'h-px w-8 mx-1 transition-colors duration-300 ' + (i < currentIdx ? 'bg-accent-emerald' : 'bg-border')} />
                )}
              </li>
            )
          })}
        </ol>
      </div>

      {/* Upload step */}
      {step === 'upload' && (
        <Card>
          <CardHeader><CardTitle>Upload File</CardTitle></CardHeader>
          <CardContent>
            <div className="flex flex-col items-center gap-5 rounded-xl border-2 border-dashed border-border hover:border-primary/40 transition-colors duration-200 p-14 bg-surface-1/30">
              <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/8 text-primary">
                <Upload className="size-7" />
              </div>
              <div className="text-center space-y-1">
                <p className="font-medium">Drop an Excel (.xlsx) or CSV (.csv) file here</p>
                <p className="text-sm text-muted-foreground">Or click the button below to browse</p>
              </div>
              <input
                ref={fileRef}
                type="file"
                accept=".xlsx,.xls,.csv"
                className="hidden"
                onChange={handleUpload}
              />
              <Button size="lg" onClick={() => fileRef.current?.click()} disabled={uploadMutation.isPending}>
                {uploadMutation.isPending && <Loader2 className="animate-spin" />}
                Choose File
              </Button>
              {uploadMutation.isError && (
                <p className="text-sm text-destructive font-medium">
                  {uploadMutation.error instanceof Error ? uploadMutation.error.message : 'Upload failed'}
                </p>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Mapping step */}
      {step === 'mapping' && (
        <Card>
          <CardHeader>
            <CardTitle>Column Mapping</CardTitle>
            <p className="text-sm text-muted-foreground leading-relaxed">
              Review and adjust the column mappings. Low-confidence suggestions are marked.
            </p>
          </CardHeader>
          <CardContent>
            {suggestionsLoading ? (
              <div className="space-y-3">
                {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-10 w-full rounded-lg" />)}
              </div>
            ) : (
              <div className="space-y-4">
                <div className="overflow-x-auto rounded-xl border border-border">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-border bg-surface-1">
                        <th className="px-4 py-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Source Column</th>
                        <th className="px-4 py-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Map To</th>
                        <th className="px-4 py-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Confidence</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                      {mappings.map((mapping) => {
                        const suggestion = suggestions?.suggestions.find(
                          (s) => s.source_column === mapping.source_column,
                        )
                        return (
                          <tr key={mapping.source_column} className="hover:bg-accent/30 transition-colors duration-150">
                            <td className="px-4 py-3 font-medium">{mapping.source_column}</td>
                            <td className="px-4 py-3">
                              <Select
                                value={mapping.target_field ?? ''}
                                onChange={(e) =>
                                  updateMapping(mapping.source_column, e.target.value || null)
                                }
                                className="w-56"
                              >
                                <option value="">— Ignore Column —</option>
                                {suggestions?.available_target_fields.map((f) => (
                                  <option key={f} value={f}>{f}</option>
                                ))}
                              </Select>
                            </td>
                            <td className="px-4 py-3">
                              {suggestion?.confidence != null && suggestion.confidence > 0 ? (
                                <Badge variant={suggestion.confidence >= 0.8 ? 'success' : 'warning'}>
                                  {Math.round(suggestion.confidence * 100)}% {suggestion.match_type}
                                </Badge>
                              ) : (
                                <Badge variant="outline">No match</Badge>
                              )}
                              {suggestion?.ambiguous && (
                                <span className="ml-2 text-xs font-medium text-accent-amber">Needs review</span>
                              )}
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
                <div className="flex gap-3">
                  <Button onClick={handleConfirmMappings} disabled={confirmMutation.isPending}>
                    {confirmMutation.isPending && <Loader2 className="animate-spin" />}
                    Confirm Mappings
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Preview step */}
      {step === 'preview' && (
        <Card>
          <CardHeader>
            <CardTitle>Preview & Validate</CardTitle>
            <p className="text-sm text-muted-foreground leading-relaxed">
              Review mapped data before importing. {preview?.total_rows} total rows.
            </p>
          </CardHeader>
          <CardContent>
            {previewLoading ? (
              <Skeleton className="h-48 w-full rounded-xl" />
            ) : preview ? (
              <div className="space-y-4">
                {preview.validation_errors.length > 0 && (
                  <div className="rounded-xl border border-accent-amber/30 bg-accent-amber/5 p-4 text-sm">
                    <p className="font-semibold text-accent-amber">
                      {preview.validation_errors.length} validation issue(s):
                    </p>
                    <ul className="mt-2 list-disc pl-5 text-foreground/80 space-y-0.5">
                      {preview.validation_errors.map((err, i) => (
                        <li key={i}>Row {err.row_number}: {err.field} — {err.message}</li>
                      ))}
                    </ul>
                  </div>
                )}

                <div className="overflow-x-auto rounded-xl border border-border">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-border bg-surface-1">
                        {preview.preview_rows[0] &&
                          Object.keys(preview.preview_rows[0]).map((key) => (
                            <th key={key} className="px-3 py-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                              {key}
                            </th>
                          ))}
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                      {preview.preview_rows.map((row, i) => (
                        <tr key={i} className="hover:bg-accent/30 transition-colors duration-150">
                          {Object.values(row).map((val, j) => (
                            <td key={j} className="px-3 py-3">{val || <span className="text-muted-foreground/60">—</span>}</td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                <div className="flex gap-3">
                  <Button onClick={handleStartProcessing} disabled={startMutation.isPending}>
                    {startMutation.isPending && <Loader2 className="animate-spin" />}
                    Start Import ({preview.total_rows} rows)
                  </Button>
                  <Button variant="outline" onClick={() => setStep('mapping')}>
                    Back to Mapping
                  </Button>
                </div>
              </div>
            ) : null}
          </CardContent>
        </Card>
      )}

      {/* Processing step */}
      {step === 'processing' && batch && (
        <Card>
          <CardHeader><CardTitle>Processing...</CardTitle></CardHeader>
          <CardContent>
            <div className="flex flex-col items-center gap-5 py-12">
              <div className="relative">
                <div className="absolute inset-0 rounded-full bg-primary/10 animate-pulse-ring" />
                <Loader2 className="size-14 animate-spin text-primary relative" />
              </div>
              <p className="text-muted-foreground font-medium">
                Processing <span className="tabular-nums text-foreground">{batch.processed_rows}</span> of <span className="tabular-nums text-foreground">{batch.total_rows}</span> rows...
              </p>
              <div className="h-2.5 w-72 overflow-hidden rounded-full bg-surface-1">
                <div
                  className="h-full rounded-full bg-brand-gradient transition-all duration-500"
                  style={{
                    width: `${batch.total_rows > 0 ? (batch.processed_rows / batch.total_rows) * 100 : 0}%`,
                  }}
                />
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Complete step */}
      {step === 'complete' && batch && (
        <Card>
          <CardHeader><CardTitle>Import Complete</CardTitle></CardHeader>
          <CardContent>
            <div className="flex flex-col items-center gap-6 py-12">
              <div className="flex size-16 items-center justify-center rounded-2xl bg-accent-emerald/10 animate-scale-in">
                <CheckCircle className="size-10 text-accent-emerald" />
              </div>
              <p className="text-xl font-bold tracking-tight">Import completed successfully</p>
              <div className="grid grid-cols-3 gap-10 text-center">
                <div>
                  <p className="text-3xl font-bold tabular-nums text-accent-emerald">{batch.created_rows}</p>
                  <p className="text-sm text-muted-foreground mt-1">Created</p>
                </div>
                <div>
                  <p className="text-3xl font-bold tabular-nums text-accent-amber">{batch.duplicate_rows}</p>
                  <p className="text-sm text-muted-foreground mt-1">Duplicates</p>
                </div>
                <div>
                  <p className="text-3xl font-bold tabular-nums text-accent-rose">{batch.rejected_rows}</p>
                  <p className="text-sm text-muted-foreground mt-1">Rejected</p>
                </div>
              </div>
              <div className="mt-2 flex gap-3">
                <Button onClick={() => navigate(`/imports/${batch.id}`)}>
                  View Details
                </Button>
                <Button variant="outline" onClick={() => navigate('/companies')}>
                  View Companies
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
