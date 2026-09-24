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

/**
 * Resolves a manually-typed NIC code (e.g. "22199") to its id, for screens
 * that want a text-entry alternative to picking from a dropdown (Companies
 * Map page). Reuses the existing paginated list search (`q` matches on code
 * prefix) rather than adding a dedicated lookup-by-code endpoint, then
 * requires an exact code match — a prefix hit alone isn't a resolution.
 * Throws when nothing matches, so callers show that as an inline error.
 */
export function useResolveNicByCode() {
  return useMutation({
    mutationFn: async (code: string) => {
      const trimmed = code.trim()
      const result = await nicApi.list({ q: trimmed, size: 50, active: true })
      const match = result.content.find((n) => n.code === trimmed)
      if (!match) throw new Error(`No NIC found for '${trimmed}'`)
      return match
    },
  })
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
