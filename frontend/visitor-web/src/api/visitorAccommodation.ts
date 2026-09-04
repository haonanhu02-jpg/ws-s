export type VisitorAccommodation = {
  visitId: string; visitorName: string; mobile: string; hostName: string;
  hostDepartment: string; visitReason: string; accommodationRequired: boolean;
  accommodationConfirmed: boolean; bedCode: string | null;
}
async function request<T>(path: string, method = 'GET'): Promise<T> {
  const response = await fetch(`/api/visitor/dormitory${path}`, {
    method, headers: { Authorization: sessionStorage.getItem('visitor-authorization') || '' },
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.detail || body.message || `请求失败（${response.status}），请刷新后重试`)
  }
  return response.json()
}
export const visitorAccommodationApi = {
  list: () => request<VisitorAccommodation[]>('/records'),
  confirm: (id: string) => request<VisitorAccommodation>(`/records/${encodeURIComponent(id)}/confirm`, 'POST'),
}
