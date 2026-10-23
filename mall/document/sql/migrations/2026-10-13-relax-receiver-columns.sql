-- 摊位小程序改造：摊位订单（order_type=2）没有收货人信息，下单时不填 receiver_* 字段
-- 将 oms_order 的收货人字段放宽为可空，电商订单不受影响（仍会正常填写）
ALTER TABLE oms_order
    MODIFY COLUMN receiver_name varchar(100) NULL COMMENT '收货人姓名',
    MODIFY COLUMN receiver_phone varchar(32) NULL COMMENT '收货人电话';
