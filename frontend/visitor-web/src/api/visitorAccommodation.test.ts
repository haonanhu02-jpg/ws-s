import { afterEach, expect, it, vi } from 'vitest'
import { visitorAccommodationApi } from './visitorAccommodation'
afterEach(() => vi.unstubAllGlobals())
it('loads real visitor records with authentication', async () => {
  vi.stubGlobal('sessionStorage', { getItem: () => 'Bearer test' })
  const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => [{ visitId: 'V1' }] })
  vi.stubGlobal('fetch', fetch)
  await expect(visitorAccommodationApi.list()).resolves.toEqual([{ visitId: 'V1' }])
  expect(fetch).toHaveBeenCalledWith('/api/visitor/dormitory/records', { method: 'GET', headers: { Authorization: 'Bearer test' } })
})
it('confirms through the backend and reports conflicts', async () => {
  vi.stubGlobal('sessionStorage', { getItem: () => 'Bearer test' })
  const fetch = vi.fn().mockResolvedValue({ ok: false, status: 409, json: async () => ({ detail: '申请已取消' }) })
  vi.stubGlobal('fetch', fetch)
  await expect(visitorAccommodationApi.confirm('V1')).rejects.toThrow('申请已取消')
  expect(fetch).toHaveBeenCalledWith('/api/visitor/dormitory/records/V1/confirm', expect.objectContaining({ method: 'POST' }))
})
