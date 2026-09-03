<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import {
  dormitoryApi,
  dormitoryExtensionApi,
  type Bed,
  type BuildingNode,
  type DormitoryStatistics,
  type FeeBill,
  type FeeRule,
  type MeterReading,
  type Person,
  type ResourceAudit,
  type Room,
  type Stay,
  type StayAudit,
  type StayAttachment,
} from "../api/dormitory";
const svgAttrs =
  'viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"';
const SETTING_ICONS: Record<string, string> = {
  download: '<path d="M12 3v12"/><path d="M7 11l5 5 5-5"/><path d="M5 21h14"/>',
  import:
    '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="M7 9l5-5 5 5"/><path d="M12 4v12"/>',
  add: '<path d="M12 5v14M5 12h14"/>',
};
function settingIcon(k: string): string {
  return `<svg ${svgAttrs}>` + (SETTING_ICONS[k] || "") + "</svg>";
}
const sections = [
  ["stats", "统计总览"],
  ["floorplan", "可视化平面图"],
  ["dashboard", "预警看板"],
  ["ledger", "入住台账"],
  ["reports", "水电报表"],
  ["archive", "历史档案"],
  ["settings", "设置"],
] as const;
const router = useRouter();
const currentUser = computed(() => JSON.parse(sessionStorage.getItem("visitor-user") || "{}"));
function logoutDormitory() {
  sessionStorage.removeItem("visitor-user");
  sessionStorage.removeItem("visitor-authorization");
  router.replace("/login");
}
const active = ref("stats"),
  buildings = ref<BuildingNode[]>([]),
  stays = ref<Stay[]>([]),
  people = ref<Person[]>([]),
  stayAudits = ref<StayAudit[]>([]),
  resourceAudits = ref<ResourceAudit[]>([]),
  selectedBuilding = ref<number | null>(null),
  loading = ref(true),
  error = ref(""),
  message = ref(""),
  modal = ref(false),
  selectedBed = ref<Bed | null>(null),
  selectedRoom = ref<Room | null>(null);
const form = reactive({
  name: "",
  centerName: "",
  department: "",
  gender: "男" as "男" | "女",
  category: "新员工(普通)",
  positionName: "",
  rankName: "",
  applicationCode: "",
  liaison: "",
  bedType: "过渡房",
  costCut: false,
  promiseSigned: false,
  plannedMoveIn: new Date().toISOString().slice(0, 10),
  plannedMoveOut: "",
  specialNote: "",
  remark: "",
});
const meterMonth = ref(new Date().toISOString().slice(0, 7)),
  meterReadings = ref<MeterReading[]>([]),
  previousReadings = ref<MeterReading[]>([]),
  meterValues = reactive<Record<number, { water: string; electric: string }>>(
    {},
  );
const resourceModal = ref(false),
  resourceKind = ref<"building" | "room" | "bed">("building"),
  resourceId = ref<number | null>(null),
  resourceForm = reactive<any>({});
const personModal = ref(false),
  ledgerSearch = ref(""),
  personSearch = ref(""),
  selectedPerson = ref<Person | null>(null),
  personHistory = ref<Stay[]>([]),
  personForm = reactive<Omit<Person, "id">>({
    name: "",
    centerName: "",
    department: "",
    gender: "男",
    category: "",
    positionName: "",
    rankName: "",
  });
const checkoutModal = ref(false),
  checkoutStay = ref<Stay | null>(null),
  checkoutForm = reactive({
    moveOutWater: "" as string | number,
    moveOutElectric: "" as string | number,
    reason: "",
  });
const statistics = ref<DormitoryStatistics | null>(null);
const attachmentModal=ref(false),attachmentStay=ref<Stay|null>(null),attachments=ref<StayAttachment[]>([]),attachmentType=ref('APPLICATION'),attachmentFile=ref<File|null>(null)
const feeRule=reactive<FeeRule>({waterPrice:0,electricPrice:0,freeWater:0,freeElectric:0,enabled:false,operatorName:'',updatedAt:''}),feeBills=ref<FeeBill[]>([])
const shownBuildings = computed(() =>
  selectedBuilding.value
    ? buildings.value.filter((n) => n.building.id === selectedBuilding.value)
    : buildings.value,
);
const selectedBedIds = computed(() =>
  new Set(shownBuildings.value.flatMap((n) => n.rooms.flatMap((r) => r.beds.map((b) => b.id)))),
);
const shownStays = computed(() =>
  selectedBuilding.value === null
    ? stays.value
    : stays.value.filter((s) => selectedBedIds.value.has(s.bed.id)),
);
const shownTotals = computed(() => {
  const rooms = shownBuildings.value.flatMap((n) => n.rooms).filter((r) => r.livable);
  const beds = rooms.flatMap((r) => r.beds).filter((b) => b.enabled);
  const booked = shownStays.value.filter((s) => s.status === "BOOKED").length;
  const occupied = shownStays.value.filter((s) => s.status === "CHECKED_IN").length;
  return {
    buildings: shownBuildings.value.length,
    rooms: rooms.length,
    beds: beds.length,
    booked,
    occupied,
    free: Math.max(0, beds.length - booked - occupied),
  };
});
const headquartersBuildings = computed(() =>
  buildings.value.filter((node) => !/[岙吞底罗空间]/.test(node.building.name)),
);
const remoteBuilding = computed(() =>
  buildings.value.find((node) => /[岙吞底罗空间]/.test(node.building.name)) ?? buildings.value.at(-1),
);
const overview = computed(() => {
  const checkedIn = shownStays.value.filter((stay) => stay.status === "CHECKED_IN");
  const totalBeds = shownTotals.value.beds;
  return {
    rooms: shownTotals.value.rooms,
    beds: totalBeds,
    rate: totalBeds ? `${((shownTotals.value.occupied / totalBeds) * 100).toFixed(1)}%` : "0.0%",
    male: checkedIn.filter((stay) => stay.person.gender === "男").length,
    female: checkedIn.filter((stay) => stay.person.gender === "女").length,
    booked: shownTotals.value.booked,
  };
});
const capacityRows = computed(() => {
  const labels = ["单间", "标间"];
  return labels.map((label) => {
    const rooms = shownBuildings.value.flatMap((node) => node.rooms).filter((room) => room.livable && room.roomType.includes(label));
    const bedIds = new Set(rooms.flatMap((room) => room.beds.filter((bed) => bed.enabled).map((bed) => bed.id)));
    const scoped = shownStays.value.filter((stay) => bedIds.has(stay.bed.id));
    const occupied = scoped.filter((stay) => stay.status === "CHECKED_IN");
    const booked = scoped.filter((stay) => stay.status === "BOOKED");
    const totalBeds = bedIds.size;
    const free = Math.max(0, totalBeds - occupied.length - booked.length);
    return {
      label,
      freeMale: Math.ceil(free / 2),
      freeFemale: Math.floor(free / 2),
      occupiedMale: occupied.filter((stay) => stay.person.gender === "男").length,
      occupiedFemale: occupied.filter((stay) => stay.person.gender === "女").length,
      bookedMale: booked.filter((stay) => stay.person.gender === "男").length,
      bookedFemale: booked.filter((stay) => stay.person.gender === "女").length,
    };
  });
});
const capacityTotal = computed(() => capacityRows.value.reduce((total, row) => ({
  label: "合计",
  freeMale: total.freeMale + row.freeMale,
  freeFemale: total.freeFemale + row.freeFemale,
  occupiedMale: total.occupiedMale + row.occupiedMale,
  occupiedFemale: total.occupiedFemale + row.occupiedFemale,
  bookedMale: total.bookedMale + row.bookedMale,
  bookedFemale: total.bookedFemale + row.bookedFemale,
}), { label: "合计", freeMale: 0, freeFemale: 0, occupiedMale: 0, occupiedFemale: 0, bookedMale: 0, bookedFemale: 0 }));
const shownBuildingStatistics = computed(() =>
  selectedBuilding.value === null
    ? statistics.value?.buildings ?? []
    : (statistics.value?.buildings ?? []).filter(
        (x) => x.name === shownBuildings.value[0]?.building.name,
      ),
);
const shownCategories = computed(() => {
  if (selectedBuilding.value === null) return statistics.value?.categories ?? [];
  const names = new Map<string, { name: string; total: number; active: number }>();
  for (const stay of shownStays.value) {
    const name = stay.person.category || "未分类";
    const item = names.get(name) ?? { name, total: 0, active: 0 };
    item.total += 1;
    if (stay.status === "BOOKED" || stay.status === "CHECKED_IN") item.active += 1;
    names.set(name, item);
  }
  return [...names.values()];
});
const shownStatuses = computed(() => {
  if (selectedBuilding.value === null) return statistics.value?.statuses ?? [];
  const labels: Record<Stay["status"], string> = {
    BOOKED: "已预定",
    CHECKED_IN: "已入住",
    CHECKED_OUT: "已退宿",
    CANCELLED: "已取消",
  };
  return Object.entries(labels).map(([status, name]) => ({
    name,
    total: shownStays.value.filter((s) => s.status === status).length,
    active: 0,
  }));
});
function selectBuilding(id: number | null) {
  selectedBuilding.value = id;
  message.value = id === null ? "已切换至全集团" : `已切换至${shownBuildings.value[0]?.building.name ?? "所选楼栋"}`;
}
const activeStays = computed(() =>
  stays.value.filter((s) => s.status === "BOOKED" || s.status === "CHECKED_IN"),
);
const stayByBed = computed(() =>
  Object.fromEntries(activeStays.value.map((s) => [s.bed.id, s])),
);
const totals = computed(() => {
  const rooms = buildings.value
      .flatMap((b) => b.rooms)
      .filter((r) => r.livable),
    beds = rooms.flatMap((r) => r.beds).filter((b) => b.enabled),
    booked = stays.value.filter((s) => s.status === "BOOKED").length,
    occupied = stays.value.filter((s) => s.status === "CHECKED_IN").length;
  return {
    buildings: buildings.value.length,
    rooms: rooms.length,
    beds: beds.length,
    booked,
    occupied,
    free: Math.max(0, beds.length - booked - occupied),
  };
});
const alerts = computed(() => ({
  clean: buildings.value
    .flatMap((b) => b.rooms)
    .filter((r) => r.cleaningRequired),
  overdue: activeStays.value.filter(
    (s) =>
      s.plannedMoveOut &&
      s.plannedMoveOut <= new Date().toISOString().slice(0, 10),
  ),
}));
type RoomState = "public" | "clean" | "live" | "book" | "ok";
function roomState(room: Room): RoomState {
  if (!room.livable) return "public";
  if (room.cleaningRequired) return "clean";
  const bedStays = room.beds.map((b) => stayByBed.value[b.id]).filter(Boolean);
  if (bedStays.some((s) => s.status === "CHECKED_IN")) return "live";
  if (bedStays.some((s) => s.status === "BOOKED")) return "book";
  return "ok";
}
const ROOM_STATE_LABEL: Record<RoomState, string> = {
  public: "公共区域",
  clean: "待打扫",
  live: "已入住",
  book: "已预订",
  ok: "可入住",
};
const statCards = computed(() => [
  { label: "房间总数", value: overview.value.rooms, icon: '<path d="M3 21h18"/><path d="M5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16"/>', tone: "blue" },
  { label: "床位总数", value: overview.value.beds, icon: '<path d="M3 18v-6a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v6"/><path d="M3 14h18"/><path d="M6 10V7M18 10V7"/>', tone: "cyan" },
  { label: "总入住率", value: overview.value.rate, icon: '<path d="M3 3v18h18"/><path d="M7 15l4-4 3 3 5-6"/>', tone: "green" },
  { label: "男性人数", value: overview.value.male, icon: '<circle cx="10" cy="14" r="5"/><path d="M13 11l8-8"/><path d="M18 3h3v3"/>', tone: "indigo" },
  { label: "女性人数", value: overview.value.female, icon: '<circle cx="12" cy="8" r="5"/><path d="M12 13v8M8 17h8"/>', tone: "pink" },
  { label: "已预订人数", value: overview.value.booked, icon: '<rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/>', tone: "amber" },
]);
function statIcon(icon: string): string {
  return `<svg ${svgAttrs}>${icon}</svg>`;
}
const historyStays = computed(() =>
  stays.value.filter(
    (s) => s.status === "CHECKED_OUT" || s.status === "CANCELLED",
  ),
);
const filteredPeople = computed(() => {
  const q = personSearch.value.trim().toLowerCase();
  return people.value.filter(
    (p) =>
      !q ||
      [
        p.name,
        p.centerName,
        p.department,
        p.category,
        p.positionName,
        p.rankName,
      ].some((v) => v?.toLowerCase().includes(q)),
  );
});
const filteredStays = computed(() => {
  const q = ledgerSearch.value.trim().toLowerCase();
  return stays.value.filter((s) => !q || [s.person.name, s.person.department, s.person.centerName, s.bed.bedCode, statusLabel(s.status)].some((v) => v?.toLowerCase().includes(q)));
});
function duplicatePerson(p: Person) {
  return people.value.some(
    (x) => x.id !== p.id && x.name === p.name && x.department === p.department,
  );
}
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const [t, s, ps, sa, ra, stats] = await Promise.all([
      dormitoryApi.tree(),
      dormitoryApi.stays(),
      dormitoryApi.people(),
      dormitoryApi.stayAudits(),
      dormitoryApi.resourceAudits(),
      dormitoryApi.statistics(),
    ]);
    buildings.value = t.buildings;
    stays.value = s;
    people.value = ps;
    stayAudits.value = sa;
    resourceAudits.value = ra;
    statistics.value = stats;
    await Promise.all([loadMeters(),loadFees()]);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
  } finally {
    loading.value = false;
  }
}
function previousMonth(month: string) {
  const [y, m] = month.split("-").map(Number);
  const d = new Date(y, m - 2, 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}
async function loadMeters() {
  const [current, previous] = await Promise.all([
    dormitoryApi.meterReadings(meterMonth.value),
    dormitoryApi.meterReadings(previousMonth(meterMonth.value)),
  ]);
  meterReadings.value = current;
  previousReadings.value = previous;
  for (const n of buildings.value)
    for (const r of n.rooms.filter((x) => x.livable)) {
      const v = current.find((x) => x.roomId === r.id);
      meterValues[r.id] = {
        water: v?.waterEnd?.toString() || "",
        electric: v?.electricEnd?.toString() || "",
      };
    }
}
async function loadFees(){const[rule,bills]=await Promise.all([dormitoryExtensionApi.feeRule(),dormitoryExtensionApi.feeBills(meterMonth.value)]);Object.assign(feeRule,rule);feeBills.value=bills}
async function changeMeterMonth() {
  try {
    await Promise.all([loadMeters(),loadFees()]);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "抄表数据加载失败";
  }
}
async function saveFeeRule(){await resourceAction(()=>dormitoryExtensionApi.saveFeeRule({waterPrice:Number(feeRule.waterPrice),electricPrice:Number(feeRule.electricPrice),freeWater:Number(feeRule.freeWater),freeElectric:Number(feeRule.freeElectric),enabled:feeRule.enabled}),'费用规则已保存')}
async function generateFees(){try{feeBills.value=await dormitoryExtensionApi.generateFeeBills(meterMonth.value);message.value=`${meterMonth.value} 账单已生成`}catch(e){error.value=e instanceof Error?e.message:'账单生成失败'}}
async function updateFee(bill:FeeBill,confirmBill=false){const adjustment=confirmBill?bill.adjustment:Number(prompt('请输入调整金额（可为负数）',String(bill.adjustment))??bill.adjustment);try{await dormitoryExtensionApi.updateFeeBill(bill.id,{adjustment,remark:bill.remark||'',status:confirmBill?'CONFIRMED':'DRAFT'});message.value=confirmBill?'账单已确认':'账单已调整';await loadFees()}catch(e){error.value=e instanceof Error?e.message:'账单更新失败'}}
async function openAttachments(stay:Stay){attachmentStay.value=stay;attachmentModal.value=true;attachmentFile.value=null;attachments.value=await dormitoryExtensionApi.attachments(stay.id)}
async function uploadAttachment(){if(!attachmentStay.value||!attachmentFile.value){error.value='请选择附件';return}try{await dormitoryExtensionApi.uploadAttachment(attachmentStay.value.id,attachmentType.value,attachmentFile.value);attachments.value=await dormitoryExtensionApi.attachments(attachmentStay.value.id);attachmentFile.value=null;message.value='附件已上传'}catch(e){error.value=e instanceof Error?e.message:'附件上传失败'}}
async function deleteAttachment(a:StayAttachment){if(!confirm(`确认删除 ${a.originalName}？`))return;await dormitoryExtensionApi.deleteAttachment(a.id);if(attachmentStay.value)attachments.value=await dormitoryExtensionApi.attachments(attachmentStay.value.id)}
function usage(roomId: number, key: "waterEnd" | "electricEnd") {
  const current =
    meterValues[roomId]?.[key === "waterEnd" ? "water" : "electric"];
  const previous = previousReadings.value.find((r) => r.roomId === roomId)?.[
    key
  ];
  if (
    current === "" ||
    current === undefined ||
    previous === undefined ||
    previous === null
  )
    return "-";
  return Math.max(0, Number(current) - Number(previous)).toFixed(2);
}
async function saveMeters() {
  const rows = buildings.value
    .flatMap((n) => n.rooms)
    .filter(
      (r) =>
        r.livable &&
        meterValues[r.id] &&
        (meterValues[r.id].water !== "" || meterValues[r.id].electric !== ""),
    )
    .map((r) => ({
      roomId: r.id,
      readingMonth: meterMonth.value,
      waterEnd:
        meterValues[r.id].water === "" ? null : Number(meterValues[r.id].water),
      electricEnd:
        meterValues[r.id].electric === ""
          ? null
          : Number(meterValues[r.id].electric),
    }));
  if (!rows.length) {
    error.value = "请至少录入一间房的抄表数据";
    return;
  }
  await resourceAction(
    () => dormitoryApi.saveMeterReadings(rows),
    "月度抄表已保存",
  );
}
function openBed(room: Room, bed: Bed) {
  const existing = stayByBed.value[bed.id];
  if (existing) {
    active.value = "ledger";
    return;
  }
  selectedRoom.value = room;
  selectedBed.value = bed;
  modal.value = true;
  message.value = "";
}
async function saveBooking() {
  if (!selectedBed.value) return;
  error.value = "";
  try {
    const person: Person = await dormitoryApi.addPerson({
      name: form.name,
      centerName: form.centerName,
      department: form.department,
      gender: form.gender,
      category: form.category,
      positionName: form.positionName,
      rankName: form.rankName,
    });
    await dormitoryApi.book({
      personId: person.id,
      bedId: selectedBed.value.id,
      applicationCode: form.applicationCode,
      liaison: form.liaison,
      bedType: form.bedType,
      costCut: form.costCut,
      promiseSigned: form.promiseSigned,
      plannedMoveIn: form.plannedMoveIn,
      plannedMoveOut: form.plannedMoveOut || null,
      specialNote: form.specialNote,
      remark: form.remark,
    });
    modal.value = false;
    message.value = "预订保存成功";
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "保存失败";
  }
}
async function act(stay: Stay, action: "check-in" | "check-out" | "cancel") {
  if (action === "check-out") {
    openCheckout(stay);
    return;
  }
  if (!confirm(action === "check-in" ? "确认办理入住？" : "确认取消预订？"))
    return;
  try {
    await dormitoryApi.action(stay.id, action);
    message.value = "操作成功";
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "操作失败";
  }
}
function openCheckout(stay: Stay) {
  checkoutStay.value = stay;
  Object.assign(checkoutForm, {
    moveOutWater: stay.moveOutWater ?? "",
    moveOutElectric: stay.moveOutElectric ?? "",
    reason: "",
  });
  checkoutModal.value = true;
}
async function saveCheckout() {
  if (!checkoutStay.value) return;
  await resourceAction(
    () =>
      dormitoryApi.action(checkoutStay.value!.id, "check-out", {
        moveOutWater:
          checkoutForm.moveOutWater === ""
            ? null
            : Number(checkoutForm.moveOutWater),
        moveOutElectric:
          checkoutForm.moveOutElectric === ""
            ? null
            : Number(checkoutForm.moveOutElectric),
        reason: checkoutForm.reason,
      }),
    "退宿已办理",
  );
  if (!error.value) checkoutModal.value = false;
}
async function transfer(stay: Stay) {
  const code = prompt("请输入目标床位编码");
  if (!code) return;
  const bed = buildings.value
    .flatMap((n) => n.rooms)
    .flatMap((r) => r.beds)
    .find((b) => b.bedCode === code.trim());
  if (!bed) {
    error.value = "目标床位编码不存在";
    return;
  }
  try {
    await dormitoryApi.action(stay.id, "transfer", {
      bedId: bed.id,
      reason: "页面调宿",
    });
    message.value = "调宿成功";
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "调宿失败";
  }
}
async function extend(stay: Stay) {
  const date = prompt(
    "请输入新的计划退宿日期（YYYY-MM-DD）",
    stay.plannedMoveOut || "",
  );
  if (!date) return;
  try {
    await dormitoryApi.action(stay.id, "extend", {
      plannedMoveOut: date,
      reason: "页面续住",
    });
    message.value = "续住成功";
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "续住失败";
  }
}
async function resourceAction(task: () => Promise<unknown>, success: string) {
  error.value = "";
  message.value = "";
  try {
    await task();
    message.value = success;
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "保存失败";
  }
}
function optionalNumber(value: string | null) {
  if (value === null || value.trim() === "") return null;
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}
function buildingBody(b: BuildingNode["building"], enabled = b.enabled) {
  return {
    name: b.name,
    regionName: b.regionName,
    enabled,
    displayOrder: b.displayOrder,
  };
}
function roomBody(r: Room, patch: Partial<Room> = {}) {
  const v = { ...r, ...patch };
  return {
    buildingId: v.buildingId,
    roomNo: v.roomNo,
    floorNo: v.floorNo,
    facing: v.facing || "",
    roomType: v.roomType,
    livable: v.livable,
    cleaningRequired: v.cleaningRequired,
    gridCol: v.gridCol ?? null,
    gridRow: v.gridRow ?? null,
    gridColSpan: v.gridColSpan ?? null,
    gridRowSpan: v.gridRowSpan ?? null,
    displayOrder: v.displayOrder ?? 0,
    specialNote: v.specialNote || "",
    enabled: v.enabled,
  };
}
function bedBody(b: Bed, patch: Partial<Bed> = {}) {
  const v = { ...b, ...patch };
  return {
    roomId: v.roomId,
    label: v.label,
    bedCode: v.bedCode,
    threePiece: v.threePiece || "",
    enabled: v.enabled,
  };
}
function openResource(kind: "building" | "room" | "bed", data: any) {
  resourceKind.value = kind;
  resourceId.value = data.id ?? null;
  Object.keys(resourceForm).forEach((k) => delete resourceForm[k]);
  Object.assign(resourceForm, data);
  resourceModal.value = true;
}
function addBuilding() {
  openResource("building", {
    name: "",
    regionName: "总部",
    enabled: true,
    displayOrder: buildings.value.length + 1,
  });
}
function editBuilding(n: BuildingNode) {
  openResource("building", { ...buildingBody(n.building), id: n.building.id });
}
function toggleBuilding(n: BuildingNode) {
  resourceAction(
    () =>
      dormitoryApi.updateBuilding(
        n.building.id,
        buildingBody(n.building, !n.building.enabled),
      ),
    n.building.enabled ? "楼栋已停用" : "楼栋已启用",
  );
}
function addRoom(n: BuildingNode) {
  openResource("room", {
    buildingId: n.building.id,
    roomNo: "",
    floorNo: 1,
    facing: "",
    roomType: "单间",
    livable: true,
    cleaningRequired: false,
    gridCol: null,
    gridRow: null,
    gridColSpan: null,
    gridRowSpan: null,
    displayOrder: n.rooms.length + 1,
    specialNote: "",
    enabled: true,
  });
}
function editRoom(r: Room) {
  openResource("room", { ...roomBody(r), id: r.id });
}
function toggleRoom(r: Room) {
  resourceAction(
    () => dormitoryApi.updateRoom(r.id, roomBody(r, { enabled: !r.enabled })),
    r.enabled ? "房间已停用" : "房间已启用",
  );
}
function toggleClean(r: Room) {
  resourceAction(
    () =>
      dormitoryApi.updateRoom(
        r.id,
        roomBody(r, { cleaningRequired: !r.cleaningRequired }),
      ),
    r.cleaningRequired ? "已完成打扫" : "已标记待打扫",
  );
}
function addBed(r: Room) {
  openResource("bed", {
    roomId: r.id,
    label: r.roomType === "标间" ? "靠窗" : "单床",
    bedCode: "",
    threePiece: "",
    enabled: true,
  });
}
function editBed(b: Bed) {
  openResource("bed", { ...bedBody(b), id: b.id });
}
function toggleBed(b: Bed) {
  resourceAction(
    () => dormitoryApi.updateBed(b.id, bedBody(b, { enabled: !b.enabled })),
    b.enabled ? "床位已停用" : "床位已启用",
  );
}
async function saveResource() {
  const id = resourceId.value;
  const kind = resourceKind.value;
  const body = { ...resourceForm };
  delete body.id;
  await resourceAction(
    () =>
      kind === "building"
        ? id
          ? dormitoryApi.updateBuilding(id, body)
          : dormitoryApi.createBuilding(body)
        : kind === "room"
          ? id
            ? dormitoryApi.updateRoom(id, body)
            : dormitoryApi.createRoom(body)
          : id
            ? dormitoryApi.updateBed(id, body)
            : dormitoryApi.createBed(body),
    `${kind === "building" ? "楼栋" : kind === "room" ? "房间" : "床位"}已保存`,
  );
  if (!error.value) resourceModal.value = false;
}
function editPerson(p: Person) {
  selectedPerson.value = p;
  Object.assign(personForm, {
    name: p.name,
    centerName: p.centerName || "",
    department: p.department,
    gender: p.gender,
    category: p.category,
    positionName: p.positionName || "",
    rankName: p.rankName || "",
  });
  personModal.value = true;
}
async function savePerson() {
  if (!selectedPerson.value) return;
  await resourceAction(
    () =>
      dormitoryApi.updatePerson(selectedPerson.value!.id, { ...personForm }),
    "人员档案已保存",
  );
  if (!error.value) personModal.value = false;
}
async function showPersonHistory(p: Person) {
  selectedPerson.value = p;
  try {
    personHistory.value = await dormitoryApi.personStays(p.id);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "住宿历史加载失败";
  }
}
function statusLabel(s: Stay["status"]) {
  return {
    BOOKED: "已预定",
    CHECKED_IN: "已入住",
    CHECKED_OUT: "已退房",
    CANCELLED: "已取消",
  }[s];
}
function actionLabel(action: string) {
  return (
    {
      BOOK: "预订",
      CHECK_IN: "入住",
      TRANSFER: "调宿",
      EXTEND: "续住",
      CHECK_OUT: "退宿",
      CANCEL: "取消",
      CREATE: "新增",
      UPDATE: "修改",
    }[action] || action
  );
}
function resourceLabel(type: string) {
  return { BUILDING: "楼栋", ROOM: "房间", BED: "床位" }[type] || type;
}
function localTime(value: string) {
  return value
    ? new Date(value).toLocaleString("zh-CN", { hour12: false })
    : "-";
}
async function exportWorkbook(
  filename: string,
  sheetName: string,
  headers: string[],
  rows: (string | number | boolean | null | undefined)[][],
) {
  const ExcelJS = (await import("exceljs")).default;
  const workbook = new ExcelJS.Workbook();
  const sheet = workbook.addWorksheet(sheetName);
  sheet.addRow(headers);
  rows.forEach((row) => sheet.addRow(row.map((v) => v ?? "")));
  sheet.getRow(1).font = { bold: true };
  sheet.views = [{ state: "frozen", ySplit: 1 }];
  sheet.columns.forEach((c, index) => {
    c.width = Math.min(
      28,
      Math.max(12, ...rows.map((r) => String(r[index] ?? "").length + 2)),
    );
  });
  const buffer = await workbook.xlsx.writeBuffer();
  const link = document.createElement("a");
  link.href = URL.createObjectURL(
    new Blob([buffer], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    }),
  );
  link.download = `${filename}_${new Date().toISOString().slice(0, 10)}.xlsx`;
  link.click();
  setTimeout(() => URL.revokeObjectURL(link.href), 1000);
}
function exportLedger() {
  return exportWorkbook(
    "入住台账",
    "入住台账",
    [
      "姓名",
      "中心",
      "部门",
      "性别",
      "类别",
      "床位",
      "状态",
      "计划入住",
      "计划退宿",
    ],
    stays.value.map((s) => [
      s.person.name,
      s.person.centerName,
      s.person.department,
      s.person.gender,
      s.person.category,
      s.bed.bedCode,
      statusLabel(s.status),
      s.plannedMoveIn,
      s.plannedMoveOut,
    ]),
  );
}
function exportPeople() {
  return exportWorkbook(
    "人员档案",
    "人员档案",
    ["姓名", "中心", "部门", "性别", "类别", "岗位", "职级", "疑似重复"],
    filteredPeople.value.map((p) => [
      p.name,
      p.centerName,
      p.department,
      p.gender,
      p.category,
      p.positionName,
      p.rankName,
      duplicatePerson(p) ? "是" : "否",
    ]),
  );
}
function exportHistory() {
  return exportWorkbook(
    "住宿历史",
    "住宿历史",
    [
      "姓名",
      "部门",
      "床位",
      "状态",
      "入住水表",
      "入住电表",
      "退宿水表",
      "退宿电表",
      "实际入住",
      "实际退宿",
    ],
    historyStays.value.map((s) => [
      s.person.name,
      s.person.department,
      s.bed.bedCode,
      statusLabel(s.status),
      s.moveInWater,
      s.moveInElectric,
      s.moveOutWater,
      s.moveOutElectric,
      localTime(s.checkedInAt || ""),
      localTime(s.checkedOutAt || ""),
    ]),
  );
}
function exportMeters() {
  return exportWorkbook(
    `水电抄表_${meterMonth.value}`,
    "水电抄表",
    [
      "月份",
      "楼栋",
      "房号",
      "水表读数",
      "本月用水",
      "电表读数",
      "本月用电",
      "操作人",
    ],
    meterReadings.value.map((m) => [
      m.readingMonth,
      m.buildingName,
      m.roomNo,
      m.waterEnd,
      usage(m.roomId, "waterEnd"),
      m.electricEnd,
      usage(m.roomId, "electricEnd"),
      m.operatorName,
    ]),
  );
}
async function downloadImportTemplate(kind: "people" | "resources") {
  if (kind === "people")
    return exportWorkbook(
      "人员导入模板",
      "人员",
      ["姓名", "中心", "部门", "性别", "人员类别", "岗位", "职级"],
      [["张三", "制造中心", "生产部", "男", "新员工(普通)", "操作工", "P1"]],
    );
  return exportWorkbook(
    "房间床位导入模板",
    "房间床位",
    [
      "楼栋名称",
      "所属区域",
      "房号",
      "楼层",
      "朝向",
      "房型",
      "床位名称",
      "床位编码",
      "三件套",
    ],
    [["1号楼", "总部", "101", 1, "南", "标间", "靠窗", "1-101-A", "公司提供"]],
  );
}
async function chooseImport(kind: "people" | "resources") {
  const input = document.createElement("input");
  input.type = "file";
  input.accept = ".xlsx";
  input.onchange = async () => {
    const file = input.files?.[0];
    if (!file) return;
    error.value = "";
    try {
      const ExcelJS = (await import("exceljs")).default;
      const workbook = new ExcelJS.Workbook();
      await workbook.xlsx.load(await file.arrayBuffer());
      const sheet = workbook.worksheets[0];
      if (!sheet) throw new Error("Excel 中没有工作表");
      const rows: string[][] = [];
      sheet.eachRow((row, index) => {
        if (index > 1)
          rows.push(
            (row.values as unknown[])
              .slice(1)
              .map((v) => String(v ?? "").trim()),
          );
      });
      const clean = rows.filter((r) => r.some(Boolean));
      if (!clean.length) throw new Error("Excel 中没有可导入的数据");
      const summary =
        kind === "people"
          ? await dormitoryApi.importPeople(
              clean.map((r) => ({
                name: r[0],
                centerName: r[1],
                department: r[2],
                gender: r[3],
                category: r[4],
                positionName: r[5],
                rankName: r[6],
              })),
            )
          : await dormitoryApi.importResources(
              clean.map((r) => ({
                buildingName: r[0],
                regionName: r[1],
                roomNo: r[2],
                floorNo: Number(r[3]),
                facing: r[4],
                roomType: r[5],
                bedLabel: r[6],
                bedCode: r[7],
                threePiece: r[8],
              })),
            );
      message.value =
        kind === "people"
          ? `人员导入完成：新增 ${summary.peopleCreated}，跳过 ${summary.skipped.length}`
          : `资源导入完成：楼栋 ${summary.buildingsCreated}、房间 ${summary.roomsCreated}、床位 ${summary.bedsCreated}，跳过 ${summary.skipped.length}`;
      await load();
    } catch (e) {
      error.value = e instanceof Error ? e.message : "导入失败";
    }
  };
  input.click();
}
function printStay(stay: Stay, kind: "checkin" | "checkout") {
  const html = (value: unknown) =>
    String(value ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#39;");
  const isOut = kind === "checkout";
  const title = isOut ? "员工退宿确认单" : "员工住宿确认单";
  const rows = [
    ["姓名", stay.person.name],
    [
      "中心/部门",
      `${stay.person.centerName || "-"} / ${stay.person.department}`,
    ],
    ["人员类别", stay.person.category],
    ["床位编码", stay.bed.bedCode],
    ["申请单编码", stay.applicationCode || "-"],
    ["对接人", stay.liaison || "-"],
    ["床位类型", stay.bedType],
    ["计划入住", stay.plannedMoveIn],
    ["计划退宿", stay.plannedMoveOut || "-"],
    ["实际入住", localTime(stay.checkedInAt || "")],
    ["实际退宿", localTime(stay.checkedOutAt || "")],
    ["入住水/电", `${stay.moveInWater ?? "-"} / ${stay.moveInElectric ?? "-"}`],
    [
      "退宿水/电",
      `${stay.moveOutWater ?? "-"} / ${stay.moveOutElectric ?? "-"}`,
    ],
    ["备注", stay.remark || "-"],
  ];
  const popup = window.open("", "_blank", "width=820,height=900");
  if (!popup) {
    error.value = "浏览器阻止了打印窗口，请允许弹窗";
    return;
  }
  popup.document.write(
    `<!doctype html><html><head><title>${title}</title><style>body{font-family:Arial,"Microsoft YaHei";padding:36px;color:#222}h1{text-align:center;font-size:24px;margin-bottom:30px}table{width:100%;border-collapse:collapse}td{border:1px solid #777;padding:12px}td:first-child{width:150px;background:#f5f5f5;font-weight:bold}.sign{display:flex;justify-content:space-between;margin-top:70px}@media print{button{display:none}}</style></head><body><h1>${title}</h1><table>${rows.map((r) => `<tr><td>${html(r[0])}</td><td>${html(r[1])}</td></tr>`).join("")}</table><div class="sign"><span>住宿人员签字：____________</span><span>管理员签字：____________</span></div><p>日期：____年__月__日</p><button onclick="window.print()">打印</button></body></html>`,
  );
  popup.document.close();
}
function exportStatistics() {
  if (!statistics.value) return;
  return exportWorkbook(
    "宿舍统计报表",
    "楼栋统计",
    ["楼栋", "总床位", "使用中", "空床", "入住率"],
    statistics.value.buildings.map((x) => [
      x.name,
      x.total,
      x.active,
      Math.max(0, x.total - x.active),
      x.total ? `${((x.active / x.total) * 100).toFixed(1)}%` : "0%",
    ]),
  );
}
onMounted(load);
</script>
<template>
  <div class="dorm-system">
    <header class="dorm-head">
      <div class="dorm-title"><strong>宿舍管理系统</strong><small>v8.5</small></div>
      <div class="region-tabs">
        <div class="region-primary">
          <button :class="{ active: selectedBuilding === null }" @click="selectBuilding(null)">全集团</button>
          <button :class="{ active: !!selectedBuilding && headquartersBuildings.some((n) => n.building.id === selectedBuilding) }" @click="selectBuilding(headquartersBuildings[0]?.building.id ?? null)">总部</button>
          <button v-if="remoteBuilding" :class="{ active: selectedBuilding === remoteBuilding.building.id }" @click="selectBuilding(remoteBuilding.building.id)">岙底罗(万盛空间)</button>
        </div>
        <div class="region-sub">
          <button v-for="n in headquartersBuildings" :key="n.building.id" :class="{ active: selectedBuilding === n.building.id }" @click="selectBuilding(n.building.id)">{{ n.building.name }}</button>
        </div>
      </div>
      <div class="dorm-account"><span>当前：{{ currentUser.username || '管理员' }}</span><button type="button" @click="logoutDormitory">退出</button></div>
    </header>
    <div class="dorm-shell">
      <nav class="dorm-nav">
        <button
          v-for="s in sections"
          :key="s[0]"
          :class="{ active: active === s[0] }"
          @click="active = s[0]"
        >
          {{ s[1] }}
        </button>
      </nav>
      <section class="dorm-content">
        <p v-if="error" class="notice-error">{{ error }}</p>
        <p v-if="message" class="notice-success">{{ message }}</p>
        <p v-if="loading" class="muted">正在加载宿舍数据…</p>
        <template v-if="!loading && active === 'stats'"
          ><h3 class="stat-section-title">统计总览 · 卡片</h3>
          <div class="dorm-stats">
            <article v-for="c in statCards" :key="c.label" :class="['stat-tone', c.tone]">
              <span class="stat-ico" v-html="statIcon(c.icon)"></span>
              <div class="stat-body"><b>{{ c.value }}</b><span>{{ c.label }}</span></div>
            </article>
          </div>
          <h3 class="stat-section-title">统计总览 · 表格（含淡蓝合计行）</h3>
          <div class="statistics-grid">
            <article class="stat-summary-block">
              <h3>{{ selectedBuilding === null ? "全集团" : shownBuildings[0]?.building.name }}总览</h3>
              <div class="table-wrap">
                <table class="capacity-table">
                  <thead><tr><th>房型</th><th>可入住男</th><th>可入住女</th><th>已入住男</th><th>已入住女</th><th>已预定男</th><th>已预定女</th></tr></thead>
                  <tbody>
                    <tr v-for="row in capacityRows" :key="row.label"><td>{{ row.label }}</td><td>{{ row.freeMale }}</td><td>{{ row.freeFemale }}</td><td>{{ row.occupiedMale }}</td><td>{{ row.occupiedFemale }}</td><td>{{ row.bookedMale }}</td><td>{{ row.bookedFemale }}</td></tr>
                    <tr class="summary-total"><td>{{ capacityTotal.label }}</td><td>{{ capacityTotal.freeMale }}</td><td>{{ capacityTotal.freeFemale }}</td><td>{{ capacityTotal.occupiedMale }}</td><td>{{ capacityTotal.occupiedFemale }}</td><td>{{ capacityTotal.bookedMale }}</td><td>{{ capacityTotal.bookedFemale }}</td></tr>
                  </tbody>
                </table>
              </div>
              <p class="rate-line">入住率 <b>{{ overview.rate }}</b></p>
            </article>
          </div></template
        >
        <template v-else-if="!loading && active === 'floorplan'"
          ><div class="fp-head">
            <div class="fp-compass">
              <svg class="fp-rose" viewBox="0 0 60 60" aria-hidden="true">
                <circle cx="30" cy="30" r="27" fill="#fafbfd" stroke="#16202f" stroke-width="2" />
                <circle cx="30" cy="30" r="22" fill="none" stroke="#e3e8f0" />
                <text x="30" y="13" text-anchor="middle" font-size="10" font-weight="700" fill="#c0392b">N</text>
                <text x="52" y="33" text-anchor="middle" font-size="9" font-weight="700" fill="#16202f">E</text>
                <text x="30" y="55" text-anchor="middle" font-size="9" font-weight="700" fill="#16202f">S</text>
                <text x="8" y="33" text-anchor="middle" font-size="9" font-weight="700" fill="#16202f">W</text>
                <polygon points="30,30 50,26 50,34" fill="#e53935" />
                <polygon points="30,30 10,28 10,32" fill="#16202f" opacity=".55" />
                <circle cx="30" cy="30" r="3" fill="#16202f" />
                <circle cx="30" cy="30" r="1.2" fill="#fff" />
              </svg>
              <div class="fp-compass-text"
                ><b>上北下南 · 左西右东</b
                ><small>颜色块代表房间状态 · 点击床位可办理</small></div
              >
            </div>
            <div class="fp-legend">
              <span>图例</span><i class="fp-sw ok"></i>可入住<i class="fp-sw book"></i>已预订<i class="fp-sw live"></i>已入住<i class="fp-sw clean"></i>待打扫<i class="fp-sw public"></i>公共区域
            </div>
          </div>
          <div v-for="node in shownBuildings" :key="node.building.id" class="fp-building">
            <header class="fp-building-head"><h3>{{ node.building.name }}<small>{{ node.building.regionName }}</small></h3></header>
            <div v-for="floor in [...new Set(node.rooms.map((r) => r.floorNo))]" :key="floor" class="fp-floor">
              <b class="fp-floor-label">{{ floor }}F</b>
              <div class="fp-board">
                <span class="fp-axis north">↑ 北 · 标间</span>
                <div class="fp-row">
                  <div class="fp-stair"><div class="fp-stair-arrows">↑↓</div><div>楼梯</div></div>
                  <article v-for="room in node.rooms.filter((r) => r.floorNo === floor && r.roomType.includes('标间'))" :key="room.id" :class="['fp-room', roomState(room)]">
                    <header><strong>{{ room.roomNo }}</strong><small>{{ room.roomType }} · {{ ROOM_STATE_LABEL[roomState(room)] }}</small></header>
                    <div v-if="room.livable" class="fp-beds">
                      <button v-for="bed in room.beds" :key="bed.id" :class="{ occupied: stayByBed[bed.id], booked: stayByBed[bed.id]?.status === 'BOOKED' }" @click="openBed(room, bed)"><span>{{ bed.label }}</span><b>{{ stayByBed[bed.id]?.person.name || "空" }}</b></button>
                    </div>
                    <p v-else>公共区域</p>
                  </article>
                </div>
                <div class="fp-corridor">过 道</div>
                <div class="fp-row">
                  <article v-for="room in node.rooms.filter((r) => r.floorNo === floor && !r.roomType.includes('标间'))" :key="room.id" :class="['fp-room', roomState(room)]">
                    <header><strong>{{ room.roomNo }}</strong><small>{{ room.roomType }} · {{ ROOM_STATE_LABEL[roomState(room)] }}</small></header>
                    <div v-if="room.livable" class="fp-beds">
                      <button v-for="bed in room.beds" :key="bed.id" :class="{ occupied: stayByBed[bed.id], booked: stayByBed[bed.id]?.status === 'BOOKED' }" @click="openBed(room, bed)"><span>{{ bed.label }}</span><b>{{ stayByBed[bed.id]?.person.name || "空" }}</b></button>
                    </div>
                    <p v-else>公共区域</p>
                  </article>
                </div>
                <span class="fp-axis south">↓ 南 · 单间</span>
              </div>
            </div>
          </div></template
        >
        <template v-else-if="!loading && active === 'dashboard'"
          ><div class="section-hero"><div><span class="section-kicker">实时预警</span><h2>预警看板</h2><p>待打扫房间与退房提醒集中展示，请及时处理。</p></div></div>
          <div class="warning-columns">
            <article>
              <h3>待打扫（{{ alerts.clean.length }}）</h3>
              <p v-for="r in alerts.clean" :key="r.id">房号 {{ r.roomNo }}</p>
              <p v-if="!alerts.clean.length" class="muted">暂无</p>
            </article>
            <article>
              <h3>退房提醒（{{ alerts.overdue.length }}）</h3>
              <p v-for="s in alerts.overdue" :key="s.id">
                {{ s.person.name }} · {{ s.plannedMoveOut }}
              </p>
              <p v-if="!alerts.overdue.length" class="muted">暂无</p>
            </article>
          </div></template
        >
        <template v-else-if="!loading && active === 'ledger'"
          ><div class="ledger-heading"><div><span class="section-kicker">住宿业务</span><h2>入住人员台账</h2><p>集中查看预订、在住与退宿人员，支持快速办理住宿业务。</p></div><div class="ledger-heading-right"><div class="ledger-heading-actions"><button class="secondary-button" @click="exportLedger">导出入住台账</button><button class="secondary-button" @click="exportPeople">导出人员档案</button></div><div class="ledger-count"><b>{{ stays.length }}</b><span>全部记录</span></div></div></div>
          <div class="ledger-toolbar"><label><span>搜索台账</span><input v-model.trim="ledgerSearch" placeholder="输入姓名、部门、床位或状态" /></label><div class="ledger-legend"><span><i class="dot booked"></i>已预订</span><span><i class="dot living"></i>已入住</span><span><i class="dot done"></i>已退宿</span></div></div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>姓名</th>
                  <th>部门</th>
                  <th>性别</th>
                  <th>床位</th>
                  <th>状态</th>
                  <th>入住时间</th>
                  <th>计划退宿</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in filteredStays" :key="s.id">
                  <td>{{ s.person.name }}</td>
                  <td>{{ s.person.department }}</td>
                  <td>{{ s.person.gender }}</td>
                  <td>{{ s.bed.bedCode }}</td>
                  <td><span :class="['ledger-status', s.status.toLowerCase()]">{{ statusLabel(s.status) }}</span></td>
                  <td>{{ s.plannedMoveIn }}</td>
                  <td>{{ s.plannedMoveOut || "-" }}</td>
                  <td class="stay-actions">
                    <button
                      v-if="s.status === 'BOOKED'"
                      @click="act(s, 'check-in')"
                    >
                      入住</button
                    ><button
                      v-if="s.status === 'BOOKED'"
                      class="secondary"
                      @click="act(s, 'cancel')"
                    >
                      取消</button
                    ><button
                      v-if="s.status === 'BOOKED' || s.status === 'CHECKED_IN'"
                      class="secondary"
                      @click="transfer(s)"
                    >
                      调宿</button
                    ><button
                      v-if="s.status === 'BOOKED' || s.status === 'CHECKED_IN'"
                      class="secondary"
                      @click="extend(s)"
                    >
                      续住</button
                    ><button
                      v-if="s.status === 'CHECKED_IN'"
                      @click="act(s, 'check-out')"
                    >
                      退宿
                    </button>
                    <button
                      class="secondary"
                      @click="printStay(s, s.status === 'CHECKED_OUT' ? 'checkout' : 'checkin')"
                    >
                      打印
                    </button>
                    <button class="secondary" @click="openAttachments(s)">附件</button>
                  </td>
                </tr>
                <tr v-if="!filteredStays.length">
                  <td colspan="8" class="empty-cell">暂无住宿记录</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="section-title subsection-head people-head">
            <div><span class="section-kicker">基础资料</span><h3>人员档案</h3><p>维护入住人员信息并查看完整住宿历史。</p></div>
            <div class="row-actions">
              <label class="pretty-search"><span>搜索人员</span><input v-model.trim="personSearch" placeholder="姓名、部门、中心或类别" /></label>
              <button class="secondary-button" @click="downloadImportTemplate('people')">
                下载模板
              </button>
              <button @click="chooseImport('people')">批量导入</button>
            </div>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>姓名</th>
                  <th>中心/部门</th>
                  <th>性别</th>
                  <th>人员类别</th>
                  <th>岗位/职级</th>
                  <th>提示</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in filteredPeople" :key="p.id">
                  <td>{{ p.name }}</td>
                  <td>{{ p.centerName || "-" }} / {{ p.department }}</td>
                  <td>{{ p.gender }}</td>
                  <td>{{ p.category }}</td>
                  <td>{{ p.positionName || "-" }} / {{ p.rankName || "-" }}</td>
                  <td>
                    <span
                      v-if="duplicatePerson(p)"
                      class="status-pill processing"
                      >疑似重复</span
                    ><span v-else>-</span>
                  </td>
                  <td class="stay-actions">
                    <button @click="editPerson(p)">编辑</button
                    ><button class="secondary" @click="showPersonHistory(p)">
                      住宿历史
                    </button>
                  </td>
                </tr>
                <tr v-if="!filteredPeople.length">
                  <td colspan="7" class="empty-cell">暂无人员档案</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div
            v-if="selectedPerson && personHistory.length"
            class="person-history"
          >
            <h3>{{ selectedPerson.name }} · 住宿历史</h3>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>床位</th>
                    <th>状态</th>
                    <th>计划入住</th>
                    <th>计划退宿</th>
                    <th>实际入住</th>
                    <th>实际退宿</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="s in personHistory" :key="s.id">
                    <td>{{ s.bed.bedCode }}</td>
                    <td>{{ statusLabel(s.status) }}</td>
                    <td>{{ s.plannedMoveIn }}</td>
                    <td>{{ s.plannedMoveOut || "-" }}</td>
                    <td>{{ localTime(s.checkedInAt || "") }}</td>
                    <td>{{ localTime(s.checkedOutAt || "") }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <p v-else-if="selectedPerson" class="muted person-history">
            该人员暂无住宿记录。
          </p></template
        >
        <template v-else-if="!loading && active === 'reports'"
          ><div class="rp-head">
            <div class="rp-title">
              <span class="rp-info">i</span>
              <div><h2>月度抄表录入（{{ meterMonth }}）</h2><small>房间单表模式 · 用量 = 本月末读数 − 上月末读数</small></div>
            </div>
            <div class="rp-actions">
              <label class="rp-field">月份<input v-model="meterMonth" type="month" @change="changeMeterMonth" /></label>
              <button class="secondary-button" @click="changeMeterMonth">确定</button>
              <button class="secondary-button" @click="exportMeters">导出水电抄表</button>
            </div>
          </div>
          <div class="rp-save-bar"><button class="rp-save" @click="saveMeters"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><path d="M17 21v-8H7v8"/><path d="M7 3v5h8"/></svg>保存抄表</button></div>
          <div class="rp-strip"><span class="dot"></span>提示：录入读数后请保存抄表，系统将自动生成水电费用台账。</div>
          <div
            v-for="node in shownBuildings"
            :key="node.building.id"
            class="resource-setting"
          >
            <header>
              <div>
                <strong>{{ node.building.name }}</strong
                ><small>{{ node.building.regionName }}</small>
              </div>
            </header>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>房号</th>
                    <th>水表月末读数(吨)</th>
                    <th>本月用水</th>
                    <th>电表月末读数(度)</th>
                    <th>本月用电</th>
                    <th>最后保存</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="room in node.rooms.filter((r) => r.livable)"
                    :key="room.id"
                  >
                    <td>{{ room.roomNo }}</td>
                    <td>
                      <input
                        v-model="meterValues[room.id].water"
                        class="meter-input"
                        type="number"
                        min="0"
                        step="0.01"
                      />
                    </td>
                    <td>{{ usage(room.id, "waterEnd") }}</td>
                    <td>
                      <input
                        v-model="meterValues[room.id].electric"
                        class="meter-input"
                        type="number"
                        min="0"
                        step="0.01"
                      />
                    </td>
                    <td>{{ usage(room.id, "electricEnd") }}</td>
                    <td>
                      {{
                        localTime(
                          meterReadings.find((x) => x.roomId === room.id)
                            ?.updatedAt || "",
                        )
                      }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <section class="fee-section">
            <div class="section-title"><div><h3>费用结算</h3><small>按房间月度用量生成账单；已确认账单不可修改</small></div><div class="row-actions"><button class="secondary-button" @click="saveFeeRule">保存规则</button><button @click="generateFees">生成本月账单</button></div></div>
            <div class="fee-rule-grid"><label>水价（元/吨）<input v-model.number="feeRule.waterPrice" type="number" min="0" step="0.0001"/></label><label>电价（元/度）<input v-model.number="feeRule.electricPrice" type="number" min="0" step="0.0001"/></label><label>每房免费水量<input v-model.number="feeRule.freeWater" type="number" min="0" step="0.01"/></label><label>每房免费电量<input v-model.number="feeRule.freeElectric" type="number" min="0" step="0.01"/></label><label class="choice"><input v-model="feeRule.enabled" type="checkbox"/> 启用结算规则</label></div>
            <div class="table-wrap"><table><thead><tr><th>楼栋/房间</th><th>水量/水费</th><th>电量/电费</th><th>调整</th><th>合计</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="b in feeBills" :key="b.id"><td>{{b.buildingName}} / {{b.roomNo}}</td><td>{{b.waterUsage}} / ¥{{b.waterAmount}}</td><td>{{b.electricUsage}} / ¥{{b.electricAmount}}</td><td>¥{{b.adjustment}}</td><td><b>¥{{b.totalAmount}}</b></td><td>{{b.status==='CONFIRMED'?'已确认':'草稿'}}</td><td class="stay-actions"><button v-if="b.status==='DRAFT'" class="secondary" @click="updateFee(b)">调整</button><button v-if="b.status==='DRAFT'" @click="updateFee(b,true)">确认</button></td></tr><tr v-if="!feeBills.length"><td colspan="7" class="empty-cell">尚未生成本月账单；需要本月和上月抄表数据。</td></tr></tbody></table></div>
          </section></template
        >
        <template v-else-if="!loading && active === 'archive'"
          ><div class="section-hero"><div><span class="section-kicker">留档记录</span><h2>历史档案</h2><p>已结束住宿与住宿操作轨迹留档，便于追溯。</p></div><button class="secondary-button" @click="exportHistory">导出历史档案</button></div>
          <h3 class="subsection-title">已结束住宿</h3>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>姓名</th>
                  <th>部门</th>
                  <th>床位</th>
                  <th>结果</th>
                  <th>计划入住</th>
                  <th>实际退宿</th>
                  <th>备注</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in historyStays" :key="s.id">
                  <td>{{ s.person.name }}</td>
                  <td>{{ s.person.department }}</td>
                  <td>{{ s.bed.bedCode }}</td>
                  <td>{{ statusLabel(s.status) }}</td>
                  <td>{{ s.plannedMoveIn }}</td>
                  <td>{{ localTime(s.checkedOutAt || "") }}</td>
                  <td>{{ s.remark || "-" }}</td>
                  <td><button class="secondary" @click="printStay(s, 'checkout')">打印退宿单</button></td>
                </tr>
                <tr v-if="!historyStays.length">
                  <td colspan="8" class="empty-cell">暂无历史档案</td>
                </tr>
              </tbody>
            </table>
          </div>
          <h3 class="subsection-title">住宿操作轨迹</h3>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>时间</th>
                  <th>人员</th>
                  <th>操作</th>
                  <th>床位变化</th>
                  <th>状态变化</th>
                  <th>操作人</th>
                  <th>原因</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="a in stayAudits" :key="a.id">
                  <td>{{ localTime(a.operatedAt) }}</td>
                  <td>{{ a.personName }}</td>
                  <td>{{ actionLabel(a.action) }}</td>
                  <td>{{ a.oldBedCode || "-" }} → {{ a.newBedCode || "-" }}</td>
                  <td>
                    {{ a.beforeStatus || "-" }} → {{ a.afterStatus || "-" }}
                  </td>
                  <td>{{ a.operatorName }}</td>
                  <td>{{ a.reason || "-" }}</td>
                </tr>
                <tr v-if="!stayAudits.length">
                  <td colspan="7" class="empty-cell">暂无操作轨迹</td>
                </tr>
              </tbody>
            </table>
          </div></template
        >
        <template v-else-if="!loading && active === 'settings'"
          ><header class="settings-hero">
            <div class="settings-hero-text">
              <p class="eyebrow">RESOURCE SETTINGS</p>
              <h2>设置</h2>
              <p class="muted">维护楼栋、房间与床位资源，支持批量导入与下载模板。</p>
            </div>
            <div class="settings-hero-actions">
              <button class="secondary-button" @click="downloadImportTemplate('resources')"><span class="btn-ico" v-html="settingIcon('download')"></span>下载导入模板</button>
              <button class="secondary-button" @click="chooseImport('resources')"><span class="btn-ico" v-html="settingIcon('import')"></span>批量导入房间床位</button>
              <button @click="addBuilding"><span class="btn-ico" v-html="settingIcon('add')"></span>新增楼栋</button>
            </div>
          </header>
          <div
            v-for="node in buildings"
            :key="node.building.id"
            class="resource-setting"
          >
            <header>
              <div>
                <strong>{{ node.building.name }}</strong
                ><small
                  >{{ node.building.regionName }} ·
                  {{ node.building.enabled ? "已启用" : "已停用" }}</small
                >
              </div>
              <div class="row-actions">
                <button @click="editBuilding(node)">编辑楼栋</button
                ><button class="secondary" @click="toggleBuilding(node)">
                  {{ node.building.enabled ? "停用" : "启用" }}</button
                ><button @click="addRoom(node)">新增房间</button>
              </div>
            </header>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>房间</th>
                    <th>楼层/朝向</th>
                    <th>房型</th>
                    <th>状态</th>
                    <th>床位</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="room in node.rooms" :key="room.id">
                    <td>{{ room.roomNo }}</td>
                    <td>{{ room.floorNo }}F / {{ room.facing || "-" }}</td>
                    <td>{{ room.roomType }}</td>
                    <td>
                      {{ room.enabled ? "启用" : "停用" }} ·
                      {{ room.cleaningRequired ? "待打扫" : "已打扫" }}
                    </td>
                    <td>
                      <div class="setting-beds">
                        <span v-for="bed in room.beds" :key="bed.id"
                          ><b>{{ bed.label }}</b> {{ bed.bedCode }}（{{
                            bed.enabled ? "启用" : "停用"
                          }}）<button @click="editBed(bed)">编辑</button
                          ><button @click="toggleBed(bed)">
                            {{ bed.enabled ? "停用" : "启用" }}
                          </button></span
                        >
                      </div>
                    </td>
                    <td class="stay-actions">
                      <button @click="editRoom(room)">编辑</button
                      ><button class="secondary" @click="toggleRoom(room)">
                        {{ room.enabled ? "停用" : "启用" }}</button
                      ><button class="secondary" @click="toggleClean(room)">
                        {{
                          room.cleaningRequired ? "完成打扫" : "标记待打扫"
                        }}</button
                      ><button @click="addBed(room)">新增床位</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <h3 class="subsection-title">资源操作审计</h3>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>时间</th>
                  <th>资源</th>
                  <th>资源ID</th>
                  <th>操作</th>
                  <th>操作人</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="a in resourceAudits" :key="a.id">
                  <td>{{ localTime(a.operatedAt) }}</td>
                  <td>{{ resourceLabel(a.resourceType) }}</td>
                  <td>{{ a.resourceId }}</td>
                  <td>{{ actionLabel(a.action) }}</td>
                  <td>{{ a.operatorName }}</td>
                </tr>
                <tr v-if="!resourceAudits.length">
                  <td colspan="5" class="empty-cell">暂无资源操作记录</td>
                </tr>
              </tbody>
            </table>
          </div></template
        >
        <template v-else
          ><div class="pending-module">
            <h2>{{ sections.find((s) => s[0] === active)?.[1] }}</h2>
            <p>栏目位置保持不变，对应后端将在下一批次接入。</p>
          </div></template
        >
      </section>
    </div>
    <div v-if="modal" class="modal-backdrop">
      <form class="panel modal-card dorm-booking" @submit.prevent="saveBooking">
        <button type="button" class="drawer-close" @click="modal = false">
          ×
        </button>
        <h3>{{ selectedRoom?.roomNo }} · {{ selectedBed?.label }}</h3>
        <div class="booking-grid">
          <label>姓名<input v-model.trim="form.name" required /></label
          ><label>中心<input v-model.trim="form.centerName" /></label
          ><label>部门<input v-model.trim="form.department" required /></label
          ><label
            >性别<select v-model="form.gender">
              <option>男</option>
              <option>女</option>
            </select></label
          ><label>人员类别<input v-model.trim="form.category" required /></label
          ><label
            >床位类型<select v-model="form.bedType">
              <option>长住房</option>
              <option>过渡房</option>
              <option>客房</option>
            </select></label
          ><label>申请单编码<input v-model.trim="form.applicationCode" /></label
          ><label>对接人<input v-model.trim="form.liaison" /></label
          ><label
            >入住时间<input
              v-model="form.plannedMoveIn"
              type="date"
              required /></label
          ><label
            >计划退宿<input v-model="form.plannedMoveOut" type="date" /></label
          ><label class="choice"
            ><input v-model="form.costCut" type="checkbox" /> 纳入降本</label
          ><label class="choice"
            ><input v-model="form.promiseSigned" type="checkbox" />
            已签承诺书</label
          ><label class="wide"
            >备注<textarea v-model.trim="form.remark" rows="2" />
          </label>
        </div>
        <button>保存预订</button>
      </form>
    </div>
    <div v-if="resourceModal" class="modal-backdrop">
      <form
        class="panel modal-card resource-form"
        @submit.prevent="saveResource"
      >
        <button
          type="button"
          class="drawer-close"
          @click="resourceModal = false"
        >
          ×
        </button>
        <h3>
          {{ resourceId ? "编辑" : "新增"
          }}{{
            resourceKind === "building"
              ? "楼栋"
              : resourceKind === "room"
                ? "房间"
                : "床位"
          }}
        </h3>
        <div v-if="resourceKind === 'building'" class="booking-grid">
          <label
            >楼栋名称<input v-model.trim="resourceForm.name" required /></label
          ><label
            >所属区域<input
              v-model.trim="resourceForm.regionName"
              required /></label
          ><label
            >显示顺序<input
              v-model.number="resourceForm.displayOrder"
              type="number"
              required /></label
          ><label class="choice"
            ><input v-model="resourceForm.enabled" type="checkbox" />
            启用</label
          >
        </div>
        <div v-else-if="resourceKind === 'room'" class="booking-grid">
          <label
            >所属楼栋<select v-model.number="resourceForm.buildingId">
              <option
                v-for="n in buildings"
                :key="n.building.id"
                :value="n.building.id"
              >
                {{ n.building.name }}
              </option>
            </select></label
          ><label
            >房号<input v-model.trim="resourceForm.roomNo" required /></label
          ><label
            >楼层<input
              v-model.number="resourceForm.floorNo"
              type="number"
              required /></label
          ><label>朝向<input v-model.trim="resourceForm.facing" /></label
          ><label
            >房型<select v-model="resourceForm.roomType">
              <option>单间</option>
              <option>标间</option>
              <option>公共区域</option>
            </select></label
          ><label
            >显示顺序<input
              v-model.number="resourceForm.displayOrder"
              type="number" /></label
          ><label
            >平面图列<input
              v-model.number="resourceForm.gridCol"
              type="number" /></label
          ><label
            >平面图行<input
              v-model.number="resourceForm.gridRow"
              type="number" /></label
          ><label
            >列宽<input
              v-model.number="resourceForm.gridColSpan"
              type="number" /></label
          ><label
            >行高<input
              v-model.number="resourceForm.gridRowSpan"
              type="number" /></label
          ><label class="choice"
            ><input v-model="resourceForm.livable" type="checkbox" />
            可住</label
          ><label class="choice"
            ><input v-model="resourceForm.cleaningRequired" type="checkbox" />
            待打扫</label
          ><label class="choice"
            ><input v-model="resourceForm.enabled" type="checkbox" />
            启用</label
          ><label class="wide"
            >特殊备注<textarea
              v-model.trim="resourceForm.specialNote"
              rows="2"
            />
          </label>
        </div>
        <div v-else class="booking-grid">
          <label
            >所属房间<select v-model.number="resourceForm.roomId">
              <optgroup
                v-for="n in buildings"
                :key="n.building.id"
                :label="n.building.name"
              >
                <option v-for="r in n.rooms" :key="r.id" :value="r.id">
                  {{ r.roomNo }}
                </option>
              </optgroup>
            </select></label
          ><label
            >床位名称<input v-model.trim="resourceForm.label" required /></label
          ><label
            >床位编码<input
              v-model.trim="resourceForm.bedCode"
              required /></label
          ><label
            >三件套状态<input v-model.trim="resourceForm.threePiece" /></label
          ><label class="choice"
            ><input v-model="resourceForm.enabled" type="checkbox" />
            启用</label
          >
        </div>
        <div class="modal-actions">
          <button
            type="button"
            class="secondary-button"
            @click="resourceModal = false"
          >
            取消</button
          ><button>保存</button>
        </div>
      </form>
    </div>
    <div v-if="personModal" class="modal-backdrop">
      <form class="panel modal-card resource-form" @submit.prevent="savePerson">
        <button type="button" class="drawer-close" @click="personModal = false">
          ×
        </button>
        <h3>编辑人员档案</h3>
        <div class="booking-grid">
          <label>姓名<input v-model.trim="personForm.name" required /></label
          ><label>中心<input v-model.trim="personForm.centerName" /></label
          ><label
            >部门<input v-model.trim="personForm.department" required /></label
          ><label
            >性别<select v-model="personForm.gender">
              <option>男</option>
              <option>女</option>
            </select></label
          ><label
            >人员类别<input
              v-model.trim="personForm.category"
              required /></label
          ><label>岗位<input v-model.trim="personForm.positionName" /></label
          ><label>职级<input v-model.trim="personForm.rankName" /></label>
        </div>
        <div class="modal-actions">
          <button
            type="button"
            class="secondary-button"
            @click="personModal = false"
          >
            取消</button
          ><button>保存</button>
        </div>
      </form>
    </div>
    <div v-if="checkoutModal" class="modal-backdrop">
      <form
        class="panel modal-card resource-form"
        @submit.prevent="saveCheckout"
      >
        <button
          type="button"
          class="drawer-close"
          @click="checkoutModal = false"
        >
          ×
        </button>
        <h3>办理退宿 · {{ checkoutStay?.person.name }}</h3>
        <p class="muted">
          床位：{{ checkoutStay?.bed.bedCode }}，退宿时间由服务器生成。
        </p>
        <div class="booking-grid">
          <label
            >退宿水表读数<input
              v-model="checkoutForm.moveOutWater"
              type="number"
              min="0"
              step="0.01" /></label
          ><label
            >退宿电表读数<input
              v-model="checkoutForm.moveOutElectric"
              type="number"
              min="0"
              step="0.01" /></label
          ><label class="wide"
            >退宿原因/备注<textarea
              v-model.trim="checkoutForm.reason"
              rows="3"
            />
          </label>
        </div>
        <div class="modal-actions">
          <button
            type="button"
            class="secondary-button"
            @click="checkoutModal = false"
          >
            取消</button
          ><button>确认退宿</button>
        </div>
      </form>
    </div>
    <div v-if="attachmentModal" class="modal-backdrop"><section class="panel modal-card resource-form"><button type="button" class="drawer-close" @click="attachmentModal=false">×</button><h3>住宿附件 · {{attachmentStay?.person.name}}</h3><div class="attachment-upload"><select v-model="attachmentType"><option value="APPLICATION">申请单</option><option value="PROMISE">承诺书</option><option value="IDENTITY">其他证明</option><option value="OTHER">其他</option></select><input type="file" accept=".pdf,.jpg,.jpeg,.png,.doc,.docx,.xls,.xlsx" @change="attachmentFile=($event.target as HTMLInputElement).files?.[0]||null"/><button @click="uploadAttachment">上传</button></div><div class="table-wrap"><table><thead><tr><th>类型</th><th>文件名</th><th>大小</th><th>操作</th></tr></thead><tbody><tr v-for="a in attachments" :key="a.id"><td>{{a.attachmentType}}</td><td>{{a.originalName}}</td><td>{{(a.fileSize/1024).toFixed(1)}} KB</td><td class="stay-actions"><button @click="dormitoryExtensionApi.downloadAttachment(a)">下载</button><button class="secondary" @click="deleteAttachment(a)">删除</button></td></tr><tr v-if="!attachments.length"><td colspan="4" class="empty-cell">暂无附件</td></tr></tbody></table></div></section></div>
  </div>
</template>
