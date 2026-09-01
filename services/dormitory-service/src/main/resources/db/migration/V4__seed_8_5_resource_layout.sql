INSERT INTO dorm_building(name,region_name,display_order) VALUES
 ('盛心公寓','总部',1),('伏龙宿舍','总部',2),('花城宿舍','总部',3),('岙底罗','岙底罗',4);

INSERT INTO dorm_room(building_id,room_no,floor_no,facing,room_type,livable,display_order)
SELECT b.id,v.room_no,v.floor_no,v.facing,v.room_type,TRUE,v.ord
FROM dorm_building b JOIN (VALUES
 ('盛心公寓','201',2,'南','单间',1),('盛心公寓','203',2,'南','单间',2),('盛心公寓','205',2,'南','单间',3),('盛心公寓','207',2,'南','单间',4),('盛心公寓','209',2,'南','单间',5),('盛心公寓','211',2,'南','单间',6),('盛心公寓','213',2,'南','单间',7),('盛心公寓','215',2,'南','单间',8),('盛心公寓','217',2,'南','单间',9),('盛心公寓','218',2,'南','单间',10),
 ('盛心公寓','202',2,'北','标间',11),('盛心公寓','204',2,'北','标间',12),('盛心公寓','206',2,'北','标间',13),('盛心公寓','208',2,'北','标间',14),('盛心公寓','210',2,'北','标间',15),('盛心公寓','212',2,'北','标间',16),('盛心公寓','214',2,'北','标间',17),('盛心公寓','216',2,'北','标间',18),
 ('盛心公寓','301',3,'南','单间',19),('盛心公寓','303',3,'南','单间',20),('盛心公寓','305',3,'南','单间',21),('盛心公寓','307',3,'南','单间',22),('盛心公寓','309',3,'南','单间',23),('盛心公寓','311',3,'南','单间',24),('盛心公寓','313',3,'南','单间',25),('盛心公寓','315',3,'南','单间',26),('盛心公寓','317',3,'南','单间',27),('盛心公寓','318',3,'南','单间',28),
 ('盛心公寓','302',3,'北','标间',29),('盛心公寓','304',3,'北','标间',30),('盛心公寓','306',3,'北','标间',31),('盛心公寓','308',3,'北','标间',32),('盛心公寓','310',3,'北','标间',33),('盛心公寓','312',3,'北','标间',34),('盛心公寓','314',3,'北','标间',35),('盛心公寓','316',3,'北','标间',36),
 ('岙底罗','201',2,'北','单间',1),('岙底罗','204',2,'北','单间',2),('岙底罗','206',2,'北','单间',3),('岙底罗','202',2,'北','标间',4),('岙底罗','203',2,'北','标间',5),('岙底罗','205',2,'北','标间',6),('岙底罗','207',2,'东','单间',7),('岙底罗','208',2,'东','单间',8),
 ('伏龙宿舍','201',2,'南','单间',1),('伏龙宿舍','202',2,'南','标间',2),('伏龙宿舍','203',2,'南','单间',3),('伏龙宿舍','204',2,'南','标间',4),('伏龙宿舍','205',2,'南','单间',5),('伏龙宿舍','206',2,'南','单间',6),('伏龙宿舍','207',2,'北','单间',7),('伏龙宿舍','208',2,'北','标间',8),('伏龙宿舍','209',2,'北','标间',9),('伏龙宿舍','210',2,'北','单间',10),('伏龙宿舍','304',3,'南','单间',11),('伏龙宿舍','303',3,'南','标间',12),('伏龙宿舍','302',3,'南','标间',13),('伏龙宿舍','301',3,'北','标间',14)
) AS v(building_name,room_no,floor_no,facing,room_type,ord) ON b.name=v.building_name;

INSERT INTO dorm_room(building_id,room_no,floor_no,room_type,livable,grid_col,grid_row,grid_col_span,grid_row_span,display_order)
SELECT b.id,v.room_no,1,v.room_type,v.livable,v.gc,v.gr,v.gcs,v.grs,v.ord
FROM dorm_building b JOIN (VALUES
 ('小次卧','单间',TRUE,1,1,1,1,1),('书房','none',FALSE,2,1,1,1,2),('厨房','none',FALSE,3,1,1,1,3),
 ('储藏室','none',FALSE,1,2,1,1,4),('餐厅','none',FALSE,2,2,2,2,5),('卫生间','none',FALSE,1,3,1,1,6),
 ('入户门','none',FALSE,4,2,1,2,7),('主卧','单间',TRUE,1,4,1,2,8),('次卧','单间',TRUE,2,4,1,2,9),
 ('客厅','none',FALSE,3,4,1,1,10),('阳台','none',FALSE,3,5,1,1,11)
) AS v(room_no,room_type,livable,gc,gr,gcs,grs,ord) ON b.name='花城宿舍';

INSERT INTO dorm_bed(room_id,label,bed_code)
SELECT r.id,'单床',b.name||'-'||r.room_no||'-单床' FROM dorm_room r JOIN dorm_building b ON b.id=r.building_id
WHERE r.livable=TRUE AND r.room_type='单间';
INSERT INTO dorm_bed(room_id,label,bed_code)
SELECT r.id,'靠窗',b.name||'-'||r.room_no||'-靠窗' FROM dorm_room r JOIN dorm_building b ON b.id=r.building_id
WHERE r.livable=TRUE AND r.room_type='标间';
INSERT INTO dorm_bed(room_id,label,bed_code)
SELECT r.id,'靠门',b.name||'-'||r.room_no||'-靠门' FROM dorm_room r JOIN dorm_building b ON b.id=r.building_id
WHERE r.livable=TRUE AND r.room_type='标间';
