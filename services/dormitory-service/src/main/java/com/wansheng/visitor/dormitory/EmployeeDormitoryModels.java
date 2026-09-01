package com.wansheng.visitor.dormitory;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

final class EmployeeDormitoryModels {
 private EmployeeDormitoryModels() {}
 enum StayStatus { BOOKED, CHECKED_IN, CHECKED_OUT, CANCELLED }
 record Building(Long id,String name,String regionName,boolean enabled,int displayOrder) {}
 record Room(Long id,Long buildingId,String roomNo,int floorNo,String facing,String roomType,boolean livable,boolean cleaningRequired,Integer gridCol,Integer gridRow,Integer gridColSpan,Integer gridRowSpan,int displayOrder,String specialNote,boolean enabled,List<Bed> beds) {}
 record Bed(Long id,Long roomId,String label,String bedCode,String threePiece,boolean enabled) {}
 record ResourceTree(List<BuildingNode> buildings) {}
 record ResourceAudit(Long id,String resourceType,Long resourceId,String action,String beforeValue,String afterValue,String operatorName,Instant operatedAt) {}
 record StayAudit(Long id,Long stayId,String personName,String action,String oldBedCode,String newBedCode,String beforeStatus,String afterStatus,String operatorName,String reason,Instant operatedAt) {}
 record MeterReading(Long id,Long buildingId,String buildingName,Long roomId,String roomNo,String readingMonth,BigDecimal waterEnd,BigDecimal electricEnd,String operatorName,Instant updatedAt) {}
 record ImportSummary(int received,int peopleCreated,int buildingsCreated,int roomsCreated,int bedsCreated,List<String> skipped) {}
 record StatisticsSummary(int people,int buildings,int rooms,int beds,int booked,int checkedIn,int checkedOut,int cancelled,int freeBeds) {}
 record StatisticsItem(String name,int total,int active) {}
 record DormitoryStatistics(StatisticsSummary summary,List<StatisticsItem> buildings,List<StatisticsItem> categories,List<StatisticsItem> statuses) {}
 record StayAttachment(Long id,Long stayId,String attachmentType,String originalName,String contentType,long fileSize,String operatorName,Instant createdAt) {}
 record FeeRule(BigDecimal waterPrice,BigDecimal electricPrice,BigDecimal freeWater,BigDecimal freeElectric,boolean enabled,String operatorName,Instant updatedAt) {}
 record FeeBill(Long id,Long roomId,String buildingName,String roomNo,String billingMonth,BigDecimal waterUsage,BigDecimal electricUsage,BigDecimal waterPrice,BigDecimal electricPrice,BigDecimal freeWater,BigDecimal freeElectric,BigDecimal waterAmount,BigDecimal electricAmount,BigDecimal adjustment,BigDecimal totalAmount,String status,String remark,String operatorName,Instant updatedAt) {}
 record BuildingNode(Building building,List<Room> rooms) {}
 record Person(Long id,String name,String centerName,String department,String gender,String category,String positionName,String rankName) {}
 record Stay(Long id,Person person,Bed bed,StayStatus status,String applicationCode,String liaison,String bedType,boolean costCut,Boolean promiseSigned,BigDecimal moveInWater,BigDecimal moveInElectric,BigDecimal moveOutWater,BigDecimal moveOutElectric,LocalDate plannedMoveIn,LocalDate plannedMoveOut,Instant checkedInAt,Instant checkedOutAt,String specialNote,String remark,long version) {}

 record BuildingCommand(@NotBlank String name,@NotBlank String regionName,Boolean enabled,Integer displayOrder) {}
 record RoomCommand(@NotNull Long buildingId,@NotBlank String roomNo,@NotNull Integer floorNo,String facing,@NotBlank String roomType,Boolean livable,Boolean cleaningRequired,Integer gridCol,Integer gridRow,Integer gridColSpan,Integer gridRowSpan,Integer displayOrder,String specialNote,Boolean enabled) {}
 record BedCommand(@NotNull Long roomId,@NotBlank String label,@NotBlank String bedCode,String threePiece,Boolean enabled) {}
 record PersonCommand(@NotBlank String name,String centerName,@NotBlank String department,@Pattern(regexp="男|女") String gender,@NotBlank String category,String positionName,String rankName) {}
 record ResourceImportCommand(@NotBlank String buildingName,@NotBlank String regionName,@NotBlank String roomNo,@NotNull Integer floorNo,String facing,@NotBlank String roomType,@NotBlank String bedLabel,@NotBlank String bedCode,String threePiece) {}
 record BookCommand(@NotNull Long personId,@NotNull Long bedId,String applicationCode,String liaison,@NotBlank String bedType,@NotNull Boolean costCut,Boolean promiseSigned,BigDecimal moveInWater,BigDecimal moveInElectric,@NotNull LocalDate plannedMoveIn,LocalDate plannedMoveOut,String specialNote,String remark) {}
 record TransferCommand(@NotNull Long bedId,String reason) {}
 record ExtendCommand(@NotNull LocalDate plannedMoveOut,String reason) {}
 record CheckoutCommand(@PositiveOrZero BigDecimal moveOutWater,@PositiveOrZero BigDecimal moveOutElectric,String reason) {}
 record MeterReadingCommand(@NotNull Long roomId,@NotBlank @Pattern(regexp="\\d{4}-(0[1-9]|1[0-2])") String readingMonth,@PositiveOrZero BigDecimal waterEnd,@PositiveOrZero BigDecimal electricEnd) {}
 record FeeRuleCommand(@NotNull @PositiveOrZero BigDecimal waterPrice,@NotNull @PositiveOrZero BigDecimal electricPrice,@NotNull @PositiveOrZero BigDecimal freeWater,@NotNull @PositiveOrZero BigDecimal freeElectric,@NotNull Boolean enabled) {}
 record FeeBillCommand(@NotNull BigDecimal adjustment,String remark,@NotBlank @Pattern(regexp="DRAFT|CONFIRMED") String status) {}
}
