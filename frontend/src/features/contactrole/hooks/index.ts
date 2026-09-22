import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { contactRolesApi } from '../api'
import type { ContactRoleCreateRequest, ContactRoleUpdateRequest } from '../types'

const KEY = ['contact-roles'] as const

export function useContactRoles(includeInactive = true) {
  return useQuery({
    queryKey: [...KEY, includeInactive],
    queryFn: () => contactRolesApi.list(includeInactive),
  })
}

export function useCreateContactRole() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: ContactRoleCreateRequest) => contactRolesApi.create(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}

export function useUpdateContactRole(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: ContactRoleUpdateRequest) => contactRolesApi.update(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}
