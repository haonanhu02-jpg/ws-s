export type OccupancyStatus = "BOOKED" | "CHECKED_IN";

export interface DatedStay {
  status: "BOOKED" | "CHECKED_IN" | "CHECKED_OUT" | "CANCELLED";
  plannedMoveIn: string;
  plannedMoveOut?: string;
}

export function hasVisibleOccupantName(name: string | null | undefined): boolean {
  const normalized = name?.trim();
  return Boolean(normalized && normalized !== "未填写");
}

export function effectiveOccupancyStatus(stay: DatedStay, businessDate: string): OccupancyStatus | null {
  if (stay.status === "CHECKED_IN") return "CHECKED_IN";
  if (stay.status !== "BOOKED") return null;
  return stay.plannedMoveIn <= businessDate ? "CHECKED_IN" : "BOOKED";
}

export function chooseBedStay<T extends DatedStay>(stays: T[], businessDate: string): T | undefined {
  return [...stays].sort((a, b) => {
    const aStatus = effectiveOccupancyStatus(a, businessDate);
    const bStatus = effectiveOccupancyStatus(b, businessDate);
    if (aStatus !== bStatus) return aStatus === "CHECKED_IN" ? -1 : 1;
    return a.plannedMoveIn.localeCompare(b.plannedMoveIn);
  })[0];
}

export interface GenderedStay extends DatedStay {
  person: { gender: "男" | "女" };
}

export function classifyRoomBeds(stays: Array<GenderedStay | undefined>, businessDate: string) {
  const result = { freePending: 0, freeMale: 0, freeFemale: 0, occupiedMale: 0, occupiedFemale: 0, bookedMale: 0, bookedFemale: 0 };
  const currentGender = stays.filter((stay) => stay && effectiveOccupancyStatus(stay, businessDate) === "CHECKED_IN")
    .map((stay) => stay?.person.gender).find(Boolean);
  for (const stay of stays) {
    if (!stay) {
      if (currentGender === "男") result.freeMale += 1;
      else if (currentGender === "女") result.freeFemale += 1;
      else result.freePending += 1;
    } else if (effectiveOccupancyStatus(stay, businessDate) === "CHECKED_IN") {
      if (stay.person.gender === "男") result.occupiedMale += 1; else result.occupiedFemale += 1;
    } else if (stay.person.gender === "男") result.bookedMale += 1; else result.bookedFemale += 1;
  }
  return result;
}
