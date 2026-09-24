import { describe, expect, it } from "vitest";
import { chooseBedStay, classifyRoomBeds, effectiveBeds, effectiveOccupancyStatus, effectiveRooms, hasVisibleOccupantName } from "./dormitoryOccupancy";

describe("dormitory occupancy presentation", () => {
  const businessDate = "2026-09-15";

  it("does not treat blank or placeholder names as visible occupants", () => {
    expect(hasVisibleOccupantName("")).toBe(false);
    expect(hasVisibleOccupantName(" 未填写 ")).toBe(false);
    expect(hasVisibleOccupantName("陈郭静")).toBe(true);
  });

  it("shows a booked stay whose move-in date has arrived as checked in", () => {
    expect(effectiveOccupancyStatus({ status: "BOOKED", plannedMoveIn: "2026-09-13" }, businessDate)).toBe("CHECKED_IN");
  });

  it("keeps a future stay booked", () => {
    expect(effectiveOccupancyStatus({ status: "BOOKED", plannedMoveIn: "2026-09-16" }, businessDate)).toBe("BOOKED");
  });

  it("uses the current dated stay instead of a later consecutive reservation", () => {
    const current = { id: 1, status: "BOOKED" as const, plannedMoveIn: "2026-09-13", plannedMoveOut: "2026-09-15" };
    const future = { id: 2, status: "BOOKED" as const, plannedMoveIn: "2026-09-16" };
    expect(chooseBedStay([future, current], businessDate)?.id).toBe(1);
  });

  it("counts an empty single room as gender-pending availability", () => {
    expect(classifyRoomBeds([undefined], businessDate)).toMatchObject({ freePending: 1, freeMale: 0, freeFemale: 0 });
  });

  it("assigns the empty bed in a twin room to the current occupant gender", () => {
    const man = { status: "BOOKED" as const, plannedMoveIn: "2026-09-13", person: { gender: "男" as const } };
    expect(classifyRoomBeds([man, undefined], businessDate)).toMatchObject({ occupiedMale: 1, freeMale: 1, freePending: 0 });
  });

  it("excludes disabled buildings, rooms and beds from every capacity statistic", () => {
    const nodes = [
      { building: { enabled: false }, rooms: [{ enabled: true, livable: true, beds: [{ id: 1, enabled: true }] }] },
      { building: { enabled: true }, rooms: [
        { enabled: false, livable: true, beds: [{ id: 2, enabled: true }] },
        { enabled: true, livable: true, beds: [{ id: 3, enabled: true }, { id: 4, enabled: false }] },
      ] },
    ];
    const rooms = effectiveRooms(nodes);
    expect(rooms).toHaveLength(1);
    expect(effectiveBeds(rooms).map((bed) => bed.id)).toEqual([3]);
  });
});
