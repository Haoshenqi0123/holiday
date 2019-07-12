
SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for holiday_date
-- ----------------------------
DROP TABLE IF EXISTS `holiday_date`;
CREATE TABLE `holiday_date` (
  `date` varchar(20) NOT NULL COMMENT '日期yyyy-MM-dd',
  `year` int(4) NOT NULL,
  `month` int(2) NOT NULL,
  `day` int(2) NOT NULL,
  `status` int(2) DEFAULT '0' COMMENT '0普通工作日1周末2需要补班的工作日3法定节假日',
  PRIMARY KEY (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT;
