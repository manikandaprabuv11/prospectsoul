import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { PageHeader } from '@/components/layout/PageHeader'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { ArrowLeft, CheckCircle, Loader2, Upload } from 'lucide-react'
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

  // Initialize mappings from suggestions
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

  // Poll for processing completion
  if (step === 'processing' && batch?.status === 'COMPLETED') {
    setStep('complete')
  }

  const stepList = ['upload', 'mapping', 'preview', 'processing', 'complete'] as WizardStep[]
  const currentIdx = stepList.indexOf(step)

  return (
    <div className="space-y-6">
      <PageHeader
        breadcrumb={<Link to="/imports" className="inline-flex items-center gap-1 hover:text-foreground transition-colors"><ArrowLeft className="size-3.5" /> Back to imports</Link>}
        title="Import data"
        description="Upload a file, map its columns, preview the outcome, then process. Skip-on-error is on by default."
      />

      {/* Step indicator — polished progress rail */}
      <ol className="flex items-center gap-2 overflow-x-auto pb-1">
        {stepList.map((s, i) => {
          const state = i === currentIdx ? 'current' : i < currentIdx ? 'done' : 'todo'
          return (
            <li key={s} className="flex items-center gap-2 shrink-0">
              <div className={
                'flex size-6 items-center justify-center rounded-full text-[11px] font-semibold ' +
                (state === 'done' ? 'bg-emerald-500 text-white' :
                 state === 'current' ? 'bg-primary text-primary-foreground shadow' :
                 'bg-muted text-muted-foreground border border-border')
              }>{i + 1}</div>
              <span className={
                'text-xs font-medium capitalize ' +
                (state === 'current' ? 'text-foreground' : 'text-muted-foreground')
              }>{s}</span>
              {i < stepList.length - 1 && (
                <div className={'h-px w-8 ' + (i < currentIdx ? 'bg-emerald-500' : 'bg-border')} />
              )}
            </li>
          )
        })}
      </ol>

      {/* Upload step */}
      {step === 'upload' && (
        <Card>
          <CardHeader><CardTitle>Upload File</CardTitle></CardHeader>
          <CardContent>
            <div className="flex flex-col items-center gap-4 rounded-lg border-2 border-dashed p-12">
              <Upload className="size-12 text-muted-foreground" />
              <p className="text-muted-foreground">Drop an Excel (.xlsx) or CSV (.csv) file here</p>
              <input
                ref={fileRef}
                type="file"
                accept=".xlsx,.xls,.csv"
                className="hidden"
                onChange={handleUpload}
              />
              <Button onClick={() => fileRef.current?.click()} disabled={uploadMutation.isPending}>
                {uploadMutation.isPending && <Loader2 className="animate-spin" />}
                Choose File
              </Button>
              {uploadMutation.isError && (
                <p className="text-sm text-destructive">
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
            <p className="text-sm text-muted-foreground">
              Review and adjust the column mappings. Low-confidence suggestions are marked.
            </p>
          </CardHeader>
          <CardContent>
            {suggestionsLoading ? (
              <div className="space-y-3">
                {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
              </div>
            ) : (
              <div className="space-y-4">
                <div className="overflow-x-auto rounded-lg border">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b bg-muted/50">
                        <th className="px-3 py-2.5 text-left font-medium text-muted-foreground">Source Column</th>
                        <th className="px-3 py-2.5 text-left font-medium text-muted-foreground">Map To</th>
                        <th className="px-3 py-2.5 text-left font-medium text-muted-foreground">Confidence</th>
                      </tr>
                    </thead>
                    <tbody>
                      {mappings.map((mapping) => {
                        const suggestion = suggestions?.suggestions.find(
                          (s) => s.source_column === mapping.source_column,
                        )
                        return (
                          <tr key={mapping.source_column} className="border-b last:border-0">
                            <td className="px-3 py-2.5 font-medium">{mapping.source_column}</td>
                            <td className="px-3 py-2.5">
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
                            <td className="px-3 py-2.5">
                              {suggestion?.confidence != null && suggestion.confidence > 0 ? (
                                <Badge variant={suggestion.confidence >= 0.8 ? 'success' : 'warning'}>
                                  {Math.round(suggestion.confidence * 100)}% {suggestion.match_type}
                                </Badge>
                              ) : (
                                <Badge variant="outline">No match</Badge>
                              )}
                              {suggestion?.ambiguous && (
                                <span className="ml-2 text-xs text-amber-600">Needs review</span>
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
            <p className="text-sm text-muted-foreground">
              Review mapped data before importing. {preview?.total_rows} total rows.
            </p>
          </CardHeader>
          <CardContent>
            {previewLoading ? (
              <Skeleton className="h-48 w-full" />
            ) : preview ? (
              <div className="space-y-4">
                {preview.validation_errors.length > 0 && (
                  <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm dark:border-amber-900 dark:bg-amber-950">
                    <p className="font-medium text-amber-800 dark:text-amber-200">
                      {preview.validation_errors.length} validation issue(s):
                    </p>
                    <ul className="mt-2 list-disc pl-5">
                      {preview.validation_errors.map((err, i) => (
                        <li key={i}>Row {err.row_number}: {err.field} — {err.message}</li>
                      ))}
                    </ul>
                  </div>
                )}

                <div className="overflow-x-auto rounded-lg border">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b bg-muted/50">
                        {preview.preview_rows[0] &&
                          Object.keys(preview.preview_rows[0]).map((key) => (
                            <th key={key} className="px-3 py-2.5 text-left font-medium text-muted-foreground">
                              {key}
                            </th>
                          ))}
                      </tr>
                    </thead>
                    <tbody>
                      {preview.preview_rows.map((row, i) => (
                        <tr key={i} className="border-b last:border-0">
                          {Object.values(row).map((val, j) => (
                            <td key={j} className="px-3 py-2.5">{val || '—'}</td>
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
            <div className="flex flex-col items-center gap-4 py-8">
              <Loader2 className="size-12 animate-spin text-primary" />
              <p className="text-muted-foreground">
                Processing {batch.processed_rows} of {batch.total_rows} rows...
              </p>
              <div className="h-2 w-64 overflow-hidden rounded-full bg-muted">
                <div
                  className="h-full bg-primary transition-all"
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
            <div className="flex flex-col items-center gap-4 py-8">
              <CheckCircle className="size-12 text-emerald-500" />
              <p className="text-lg font-medium">Import completed successfully</p>
              <div className="grid grid-cols-3 gap-8 text-center">
                <div>
                  <p className="text-2xl font-bold text-emerald-600">{batch.created_rows}</p>
                  <p className="text-sm text-muted-foreground">Created</p>
                </div>
                <div>
                  <p className="text-2xl font-bold text-amber-600">{batch.duplicate_rows}</p>
                  <p className="text-sm text-muted-foreground">Duplicates</p>
                </div>
                <div>
                  <p className="text-2xl font-bold text-red-600">{batch.rejected_rows}</p>
                  <p className="text-sm text-muted-foreground">Rejected</p>
                </div>
              </div>
              <div className="mt-4 flex gap-3">
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
