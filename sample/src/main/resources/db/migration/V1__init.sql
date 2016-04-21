-- -----------------------------------------------------
-- Table `company`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `company` (
  `id`          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(45)  NOT NULL DEFAULT '',
  `description` TEXT         NOT NULL DEFAULT '',
  `address`     VARCHAR(45)  NOT NULL DEFAULT '',
  `logo`        VARCHAR(255) NOT NULL DEFAULT '',
  `status`      TINYINT      NOT NULL DEFAULT 0,
  `created_at`  DATETIME     NOT NULL DEFAULT now(),
  `updated_at`  DATETIME     NOT NULL DEFAULT now(),
  PRIMARY KEY (`id`)
)
  ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `product`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `product` (
  `id`          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `company_id`  INT UNSIGNED NOT NULL DEFAULT 0,
  `name`        VARCHAR(45)  NOT NULL DEFAULT '',
  `description` TEXT         NOT NULL DEFAULT '',
  `status`      TINYINT      NOT NULL DEFAULT 0,
  `created_at`  DATETIME     NOT NULL DEFAULT now(),
  `updated_at`  DATETIME     NOT NULL DEFAULT now(),
  PRIMARY KEY (`id`)
)
  ENGINE = InnoDB;
