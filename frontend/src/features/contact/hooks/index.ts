import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { contactsApi } from '../api'
import type { ContactCreateRequest, ContactUpdateRequest } from '../types'

const KEY = (companyId: string) => ['contacts', companyId] as const

export function useCompanyContacts(companyId: string) {
  return useQuery({
    queryKey: KEY(companyId),
    queryFn: () => contactsApi.listForCompany(companyId),
    enabled: !!companyId,
  })
}

export function useCreateContact(companyId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: ContactCreateRequest) => contactsApi.create(companyId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY(companyId) }),
  })
}

export function useUpdateContact(companyId: string, contactId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: ContactUpdateRequest) => contactsApi.update(contactId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY(companyId) }),
  })
}

export function useMakeContactPrimary(companyId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (contactId: string) => contactsApi.makePrimary(contactId),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY(companyId) }),
  })
}
