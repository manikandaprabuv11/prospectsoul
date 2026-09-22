import { queryClient } from '@/lib/query-client'
import { useMutation, useQuery } from '@tanstack/react-query'
import { importApi } from '../api'
import type { ColumnMapping } from '../types'

export function useImportBatches(page = 0, size = 25) {
  return useQuery({
    queryKey: ['imports', page, size],
    queryFn: () => importApi.list(page, size),
    refetchInterval: (query) => {
      const anyMoving = query.state.data?.content?.some(
        (b) => b.status === 'PROCESSING' || b.status === 'MAPPING' || b.status === 'PREVIEWING',
      )
      return anyMoving ? 2000 : false
    },
  })
}

/**
 * Auto-polls the batch every 1s while it's still moving so the detail
 * page shows live progress. The import runs @Async on the server, so
 * leaving the tab open is optional — every request is a cheap read.
 */
export function useImportBatch(id: string | undefined) {
  return useQuery({
    queryKey: ['import', id],
    queryFn: () => importApi.getById(id!),
    enabled: !!id,
    // Poll a live batch — the DB is being updated every row for the first
    // 20 rows and then at least every 500ms after that. 1s here is a good
    // compromise between "feels live" and "cheap".
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'PROCESSING' ? 1000 : false
    },
    refetchIntervalInBackground: true,
  })
}

/**
 * Auto-polls the row list every 1.5s so the table on the detail page
 * lights up in real time as rows land. Slightly slower than the batch
 * refresh so a very large list doesn't hammer the server.
 */
export function useImportRows(
  id: string | undefined,
  page = 0,
  size = 25,
  isLive = false,
) {
  return useQuery({
    queryKey: ['import-rows', id, page, size],
    queryFn: () => importApi.getRows(id!, page, size),
    enabled: !!id,
    refetchInterval: isLive ? 1500 : false,
    refetchIntervalInBackground: true,
  })
}

export function useMappingSuggestions(id: string | undefined) {
  return useQuery({
    queryKey: ['import-mappings', id],
    queryFn: () => importApi.suggestMappings(id!),
    enabled: !!id,
  })
}

export function useImportPreview(id: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: ['import-preview', id],
    queryFn: () => importApi.preview(id!),
    enabled: !!id && enabled,
  })
}

export function useUploadImport() {
  return useMutation({
    mutationFn: ({ file, source }: { file: File; source: string }) =>
      importApi.upload(file, source),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['imports'] })
    },
  })
}

export function useConfirmMappings(id: string) {
  return useMutation({
    mutationFn: (mappings: ColumnMapping[]) => importApi.confirmMappings(id, mappings),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['import', id] })
    },
  })
}

export function useStartProcessing(id: string) {
  return useMutation({
    mutationFn: () => importApi.startProcessing(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['import', id] })
      queryClient.invalidateQueries({ queryKey: ['imports'] })
    },
  })
}
