import { describe, expect, it } from 'vitest'
import { dormitoryDashboardStats } from './dashboardStats'

describe('dormitoryDashboardStats', () => {
  it('maps accommodation registrations to the three dormitory cards', () => {
    expect(dormitoryDashboardStats([
      { accommodationRequired: true, bedCode: null },
      { accommodationRequired: true, bedCode: '' },
      { accommodationRequired: true, bedCode: '盛心公寓-202-靠窗' },
      { accommodationRequired: false, bedCode: null },
    ])).toEqual({
      needsAccommodation: 3,
      pendingBed: 2,
      currentAccommodation: 1,
    })
  })
})
