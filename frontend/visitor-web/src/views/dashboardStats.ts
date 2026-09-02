type DormitoryRegistration = {
  accommodationRequired?: boolean
  bedCode?: string | null
}

export function dormitoryDashboardStats(records: DormitoryRegistration[]) {
  const required = records.filter((record) => record.accommodationRequired !== false)
  return {
    needsAccommodation: required.length,
    pendingBed: required.filter((record) => !record.bedCode).length,
    currentAccommodation: required.filter((record) => Boolean(record.bedCode)).length,
  }
}
