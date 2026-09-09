import { queryClient } from '@/lib/query-client'
import { useMutation, useQuery } from '@tanstack/react-query'
import { importApi } from '../api'
import type { ColumnMapping } from '../types'

export function useImportBatches(page = 0, size = 25) {
  return useQuery({
    queryKey: ['imports', page, size],
    queryFn: () => importApi.list(page, size),
  })
}

export function useImportBatch(id: string | undefined) {
  return useQuery({
    queryKey: ['import', id],
    queryFn: () => importApi.getById(id!),
    enabled: !!id,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'PROCESSING' ? 2000 : false
    },
  })
}

export function useImportRows(id: string | undefined, page = 0, size = 25) {
  return useQuery({
    queryKey: ['import-rows', id, page, size],
    queryFn: () => importApi.getRows(id!, page, size),
    enabled: !!id,
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
