import { describe, expect, it } from "vitest";
import { chooseBedStay, classifyRoomBeds, editableGender, effectiveBeds, effectiveOccupancyStatus, effectiveRooms, hasVisibleOccupantName } from "./dormitoryOccupancy";

describe("dormitory occupancy presentation", () => {
  const businessDate = "2026-09-15";

  it("uses the manual arrival tick on the move-in day instead of assuming arrival", () => {
    const stay = { status: "BOOKED" as const, plannedMoveIn: businessDate, arrivalConfirmed: false };
    expect(effectiveOccupancyStatus(stay, businessDate)).toBe("BOOKED");
    expect(effectiveOccupancyStatus({ ...stay, arrivalConfirmed: true }, businessDate)).toBe("CHECKED_IN");
    expect(effectiveOccupancyStatus({ ...stay, status: "CHECKED_IN" }, businessDate)).toBe("BOOKED");
  });

  it("reconciles 51 beds with 36 occupants including two without gender", () => {
    const stay = (gender: string) => ({ status: "CHECKED_IN" as const, arrivalConfirmed: true, plannedMoveIn: "2026-09-01", person: { name: "入住人", gender } });
    const beds = [...Array(20).fill(stay("男")), ...Array(14).fill(stay("女")), stay(""), stay("未填写"), ...Array(13).fill(undefined), ...Array(2).fill({ ...stay("男"), arrivalConfirmed: false, status: "BOOKED", plannedMoveIn: "2026-10-19" })];
    const counts = classifyRoomBeds(beds, businessDate, "单间");
    expect(counts).toMatchObject({ occupiedMale: 20, occupiedFemale: 14, occupiedOther: 2, bookedMale: 2 });
    expect(Object.values(counts).reduce((sum, value) => sum + value, 0)).toBe(51);
  });

  it("retains unknown genders and cleaning beds in separate categories", () => {
    for (const gender of ["", "未填写", "未知", null, undefined]) {
      const stay = { status: "BOOKED" as const, plannedMoveIn: "2026-10-19", person: { name: "预订人", gender: gender as unknown as string } };
      const counts = classifyRoomBeds([stay, undefined], businessDate, "标间", [false, true]);
      expect(counts).toMatchObject({ bookedOther: 1, cleaning: 1, freePending: 0 });
      expect(Object.values(counts).reduce((sum, value) => sum + value, 0)).toBe(2);
    }
  });

  it("does not invent an arrival date for undated records", () => {
    expect(effectiveOccupancyStatus({ status: "BOOKED", plannedMoveIn: null }, businessDate)).toBe("BOOKED");
    expect(effectiveOccupancyStatus({ status: "CHECKED_IN", plannedMoveIn: null, arrivalConfirmed: true }, businessDate)).toBe("CHECKED_IN");
    expect(effectiveOccupancyStatus({ status: "CHECKED_IN", plannedMoveIn: null, arrivalConfirmed: false }, businessDate)).toBe("BOOKED");
    expect(chooseBedStay([{status: "BOOKED", plannedMoveIn: null}, {status: "BOOKED", plannedMoveIn: "2026-10-19"}], businessDate)).toBeDefined();
  });

  it("turns imported/default gender placeholders into an optional blank input", () => {
    for (const value of [null, undefined, "", "未填写"]) expect(editableGender(value)).toBe("");
    expect(editableGender("男")).toBe("男"); expect(editableGender(" 女 ")).toBe("女");
  });

  it("does not treat blank or placeholder names as visible occupants", () => {
    expect(hasVisibleOccupantName("")).toBe(false);
    expect(hasVisibleOccupantName(" 未填写 ")).toBe(false);
    expect(hasVisibleOccupantName("陈郭静")).toBe(true);
  });

  it("does not automatically confirm a booking after its arrival date", () => {
    expect(effectiveOccupancyStatus({ status: "BOOKED", plannedMoveIn: "2026-09-13", arrivalConfirmed: false }, businessDate)).toBe("BOOKED");
    expect(effectiveOccupancyStatus({ status: "BOOKED", plannedMoveIn: "2026-09-13", arrivalConfirmed: false }, "2026-09-20")).toBe("BOOKED");
    expect(effectiveOccupancyStatus({ status: "BOOKED", plannedMoveIn: "2026-09-13", arrivalConfirmed: true }, businessDate)).toBe("CHECKED_IN");
  });

  it("keeps a future stay booked", () => {
    expect(effectiveOccupancyStatus({ status: "BOOKED", plannedMoveIn: "2026-09-16" }, businessDate)).toBe("BOOKED");
  });

  it("uses the explicit tick rather than saved status or future dates", () => {
    expect(effectiveOccupancyStatus({ status: "CHECKED_IN", plannedMoveIn: "2026-10-19", arrivalConfirmed: false }, "2026-10-09")).toBe("BOOKED");
    expect(effectiveOccupancyStatus({ status: "BOOKED", plannedMoveIn: "2026-10-19", arrivalConfirmed: true }, "2026-10-09")).toBe("CHECKED_IN");
  });

  it("preserves terminal states even when an old tick remains saved", () => {
    for (const status of ["CANCELLED", "CHECKED_OUT"] as const)
      expect(effectiveOccupancyStatus({ status, plannedMoveIn: "2026-09-01", arrivalConfirmed: true }, businessDate)).toBeNull();
    expect(effectiveOccupancyStatus({ status: "CHECKED_IN", plannedMoveIn: businessDate, arrivalConfirmed: true }, businessDate)).toBe("CHECKED_IN");
  });

  it("uses the current dated stay instead of a later consecutive reservation", () => {
    const current = { id: 1, status: "BOOKED" as const, arrivalConfirmed: true, plannedMoveIn: "2026-09-13", plannedMoveOut: "2026-09-15" };
    const future = { id: 2, status: "BOOKED" as const, arrivalConfirmed: false, plannedMoveIn: "2026-09-16" };
    expect(chooseBedStay([future, current], businessDate)?.id).toBe(1);
  });

  it("counts an empty single room as gender-pending availability", () => {
    expect(classifyRoomBeds([undefined], businessDate)).toMatchObject({ freePending: 1, freeMale: 0, freeFemale: 0 });
  });

  it("assigns the empty bed in a twin room to the current occupant gender", () => {
    const man = { status: "BOOKED" as const, arrivalConfirmed: true, plannedMoveIn: "2026-09-13", person: { name: "入住人", gender: "男" as const } };
    expect(classifyRoomBeds([man, undefined], businessDate)).toMatchObject({ occupiedMale: 1, freeMale: 1, freePending: 0 });
  });

  it("does not use a missing occupant or a future booking to assign available-bed gender", () => {
    const blank = { status: "BOOKED" as const, plannedMoveIn: "2026-09-13", person: { name: "未填写", gender: "女" as const } };
    const future = { ...blank, plannedMoveIn: "2026-09-16", person: { name: "未来入住", gender: "男" as const } };
    expect(classifyRoomBeds([blank, undefined], businessDate)).toMatchObject({ freePending: 2, occupiedFemale: 0, freeFemale: 0 });
    expect(classifyRoomBeds([future, undefined], businessDate)).toMatchObject({ freePending: 1, freeMale: 0, bookedMale: 1 });
  });

  it("only assigns gender in an occupied twin room and excludes cleaning beds", () => {
    const woman = { status: "CHECKED_IN" as const, arrivalConfirmed: true, plannedMoveIn: "2026-09-13", person: { name: "入住人", gender: "女" } };
    expect(classifyRoomBeds([woman, undefined], businessDate, "单间")).toMatchObject({ freePending: 1, freeFemale: 0 });
    expect(classifyRoomBeds([woman, undefined, undefined], businessDate, "标间", [false, false, true])).toMatchObject({ occupiedFemale: 1, freeFemale: 1, freePending: 0 });
    expect(classifyRoomBeds([undefined, undefined], businessDate, "标间")).toMatchObject({ freePending: 2, freeMale: 0, freeFemale: 0 });
    expect(classifyRoomBeds([{ ...woman, person: { name: "未知性别", gender: "未填写" } }, undefined], businessDate)).toMatchObject({ occupiedFemale: 0, freePending: 1 });
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
