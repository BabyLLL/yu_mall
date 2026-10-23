-- Apply after mall.sql. The table keeps WeChat identity separate from generated UmsMember fields.
CREATE TABLE IF NOT EXISTS `ums_member_wechat_identity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_id` varchar(64) NOT NULL,
  `open_id` varchar(128) NOT NULL,
  `union_id` varchar(128) DEFAULT NULL,
  `member_id` bigint(20) NOT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wechat_app_open` (`app_id`, `open_id`),
  UNIQUE KEY `uk_wechat_member` (`app_id`, `member_id`),
  CONSTRAINT `fk_wechat_member` FOREIGN KEY (`member_id`) REFERENCES `ums_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信小程序会员身份映射';

-- Rollback: DROP TABLE IF EXISTS `ums_member_wechat_identity`;
