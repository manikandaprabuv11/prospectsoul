import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { nicApi } from '../api'
import type {
  NicCodeCreateRequest,
  NicCodeUpdateRequest,
  NicListFilters,
} from '../types'

const KEYS = {
  root: ['nic-codes'] as const,
  list: (filters: NicListFilters) => ['nic-codes', 'list', filters] as const,
  detail: (id: string) => ['nic-codes', 'detail', id] as const,
  children: (id: string) => ['nic-codes', 'children', id] as const,
  tree: (rootId?: string, depth?: number) =>
    ['nic-codes', 'tree', rootId ?? null, depth ?? 3] as const,
  primary: () => ['nic-codes', 'primary'] as const,
}

export function useNicCodeList(filters: NicListFilters) {
  return useQuery({
    queryKey: KEYS.list(filters),
    queryFn: () => nicApi.list(filters),
  })
}

export function useNicTree(rootId?: string, depth?: number) {
  return useQuery({
    queryKey: KEYS.tree(rootId, depth),
    queryFn: () => nicApi.tree(rootId, depth),
  })
}

export function useNicPrimary() {
  return useQuery({ queryKey: KEYS.primary(), queryFn: () => nicApi.primary() })
}

export function useCreateNicCode() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: NicCodeCreateRequest) => nicApi.create(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.root }),
  })
}

export function useUpdateNicCode(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: NicCodeUpdateRequest) => nicApi.update(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.root }),
  })
}

export function useToggleNicPrimary() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => nicApi.togglePrimary(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.root }),
  })
}

export function useImportNicFile() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => nicApi.importFile(file),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.root }),
  })
}
