-- 摊位小程序改造：订单表新增取货码
-- 格式：N + 按天三位流水（N001），每天重置；仅摊位订单（order_type=2）使用
ALTER TABLE oms_order ADD COLUMN pickup_code varchar(16) NULL COMMENT '取货码：N+按天三位流水（N001），每天重置；摊位订单使用';
