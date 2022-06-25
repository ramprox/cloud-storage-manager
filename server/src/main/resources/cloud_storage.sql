CREATE DATABASE IF NOT EXISTS `cloud_storage`;

USE `cloud_storage`;

CREATE TABLE IF NOT EXISTS `users` (
  `login` varchar(20) NOT NULL,
  `password` varchar(20) NOT NULL,
  PRIMARY KEY (`login`)
)