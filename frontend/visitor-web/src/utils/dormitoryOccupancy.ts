export type OccupancyStatus = "BOOKED" | "CHECKED_IN";

export interface DatedStay {
  status: "BOOKED" | "CHECKED_IN" | "CHECKED_OUT" | "CANCELLED";
  plannedMoveIn: string | null;
  plannedMoveOut?: string;
}

export function hasVisibleOccupantName(name: string | null | undefined): boolean {
  const normalized = name?.trim();
  return Boolean(normalized && normalized !== "未填写");
}

export function editableGender(value: string | null | undefined): "" | "男" | "女" {
  const gender = value?.trim();
  return gender === "男" || gender === "女" ? gender : "";
}

export function effectiveOccupancyStatus(stay: DatedStay, businessDate: string): OccupancyStatus | null {
  if (stay.status !== "BOOKED" && stay.status !== "CHECKED_IN") return null;
  if (!stay.plannedMoveIn) return stay.status;
  return stay.plannedMoveIn <= businessDate ? "CHECKED_IN" : "BOOKED";
}

export function chooseBedStay<T extends DatedStay>(stays: T[], businessDate: string): T | undefined {
  return [...stays].sort((a, b) => {
    const aStatus = effectiveOccupancyStatus(a, businessDate);
    const bStatus = effectiveOccupancyStatus(b, businessDate);
    if (aStatus !== bStatus) return aStatus === "CHECKED_IN" ? -1 : 1;
    return (a.plannedMoveIn || "").localeCompare(b.plannedMoveIn || "");
  })[0];
}

export interface GenderedStay extends DatedStay {
  person: { name?: string; gender: string };
}

export function floorplanOccupancyStatus(stay: GenderedStay | undefined, businessDate: string): OccupancyStatus | null {
  return stay && hasVisibleOccupantName(stay.person.name) ? effectiveOccupancyStatus(stay, businessDate) : null;
}

export function classifyRoomBeds(stays: Array<GenderedStay | undefined>, businessDate: string, roomType = "标间", unavailable: boolean[] = []) {
  const result = { freePending: 0, freeMale: 0, freeFemale: 0, occupiedMale: 0, occupiedFemale: 0, bookedMale: 0, bookedFemale: 0 };
  const genders = new Set(stays.filter((stay, index) => !unavailable[index] && floorplanOccupancyStatus(stay, businessDate) === "CHECKED_IN")
    .map((stay) => stay!.person.gender));
  const currentGender = roomType.includes("标间") && genders.size === 1 ? [...genders][0] : undefined;
  for (const [index, stay] of stays.entries()) {
    if (unavailable[index]) continue;
    const status = floorplanOccupancyStatus(stay, businessDate);
    if (!status) {
      if (currentGender === "男") result.freeMale += 1;
      else if (currentGender === "女") result.freeFemale += 1;
      else result.freePending += 1;
    } else if (status === "CHECKED_IN") {
      if (stay!.person.gender === "男") result.occupiedMale += 1;
      else if (stay!.person.gender === "女") result.occupiedFemale += 1;
    } else if (stay!.person.gender === "男") result.bookedMale += 1;
    else if (stay!.person.gender === "女") result.bookedFemale += 1;
  }
  return result;
}

export interface EnabledBed { id: number; enabled: boolean }
export interface EnabledRoom { enabled: boolean; livable: boolean; beds: EnabledBed[] }
export interface EnabledBuildingNode { building: { enabled: boolean }; rooms: EnabledRoom[] }

export function effectiveRooms<T extends EnabledRoom>(nodes: Array<{ building: { enabled: boolean }; rooms: T[] }>): T[] {
  return nodes.filter((node) => node.building.enabled).flatMap((node) => node.rooms).filter((room) => room.enabled && room.livable);
}

export function effectiveBeds<T extends EnabledBed>(rooms: Array<{ beds: T[] }>): T[] {
  return rooms.flatMap((room) => room.beds).filter((bed) => bed.enabled);
}
