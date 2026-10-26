package com.example.data.models

data class UpgradeItem(
  val id: String,
  val nameRu: String,
  val descRu: String,
  val iconName: String,
  val maxRank: Int,
  val baseCost: Int,
  val costPerRank: Int,
  val currentRank: Int
) {
  val isMaxed: Boolean get() = currentRank >= maxRank
  val nextCost: Int get() = baseCost + (currentRank * costPerRank)
}

object AbilityRepository {
  fun getUpgrades(
    doubleJumpRank: Int,
    dashRank: Int,
    speedRank: Int,
    jumpRank: Int,
    magnetRank: Int,
    shieldRank: Int,
    chronoRank: Int,
    glideRank: Int,
    dustMultiplierRank: Int,
    wallJumpRank: Int,
    radarRank: Int,
    shieldRechargeRank: Int,
    phoenixRank: Int,
    rayTeleportRank: Int = 0
  ): List<UpgradeItem> {
    return listOf(
      UpgradeItem(
        id = "ray_teleport",
        nameRu = "Лучевой Телепорт",
        descRu = "Мгновенно телепортирует героя на 1 блок (ур. 1) или 2 блока (ур. 2) по направлению взгляда. Перезарядка 5 сек.",
        iconName = "ray_teleport",
        maxRank = 2,
        baseCost = 85,
        costPerRank = 75,
        currentRank = rayTeleportRank
      ),
      UpgradeItem(
        id = "double_jump",
        nameRu = "Двойной Прыжок",
        descRu = "Позволяет оттолкнуться от воздуха во второй раз с сияющими крыльями.",
        iconName = "double_jump",
        maxRank = 1,
        baseCost = 100,
        costPerRank = 0,
        currentRank = doubleJumpRank
      ),
      UpgradeItem(
        id = "air_dash",
        nameRu = "Рывок в Воздухе",
        descRu = "Стремительный горизонтальный рывок сквозь опасные пропасти и ловушки.",
        iconName = "air_dash",
        maxRank = 2,
        baseCost = 80,
        costPerRank = 70,
        currentRank = dashRank
      ),
      UpgradeItem(
        id = "speed",
        nameRu = "Скорость и Ловкость",
        descRu = "Увеличивает скорость перемещения и ускорение на +15% за каждый уровень.",
        iconName = "speed",
        maxRank = 3,
        baseCost = 40,
        costPerRank = 30,
        currentRank = speedRank
      ),
      UpgradeItem(
        id = "high_jump",
        nameRu = "Сила Прыжка",
        descRu = "Повышает начальный импульс прыжка для преодоления высоких уступов.",
        iconName = "high_jump",
        maxRank = 3,
        baseCost = 45,
        costPerRank = 35,
        currentRank = jumpRank
      ),
      UpgradeItem(
        id = "lunar_glide",
        nameRu = "Лунное Парение",
        descRu = "Удерживайте прыжок в воздухе, чтобы плавно планировать над пропастями.",
        iconName = "glide",
        maxRank = 2,
        baseCost = 75,
        costPerRank = 60,
        currentRank = glideRank
      ),
      UpgradeItem(
        id = "wall_spring",
        nameRu = "Кинетический Отскок",
        descRu = "Увеличивает силу и высоту прыжка от стен (Wall Jump) на +35% за уровень.",
        iconName = "wall_spring",
        maxRank = 2,
        baseCost = 65,
        costPerRank = 45,
        currentRank = wallJumpRank
      ),
      UpgradeItem(
        id = "magnet",
        nameRu = "Магнит Реликвий",
        descRu = "Притягивает звёздную пыль и парящие артефакты с увеличенного расстояния.",
        iconName = "magnet",
        maxRank = 3,
        baseCost = 50,
        costPerRank = 40,
        currentRank = magnetRank
      ),
      UpgradeItem(
        id = "dust_alchemist",
        nameRu = "Алхимия Звёзд",
        descRu = "Множитель пыли: повышает награду за кристаллы и артефакты на +50% / +100%.",
        iconName = "dust_mult",
        maxRank = 2,
        baseCost = 60,
        costPerRank = 50,
        currentRank = dustMultiplierRank
      ),
      UpgradeItem(
        id = "artifact_radar",
        nameRu = "Компас Артефактов",
        descRu = "Проецирует квантовый луч вокруг героя, указывающий путь к несобранным артефактам.",
        iconName = "radar",
        maxRank = 1,
        baseCost = 90,
        costPerRank = 0,
        currentRank = radarRank
      ),
      UpgradeItem(
        id = "vitality_shield",
        nameRu = "Живучесть и Щит",
        descRu = "Добавляет дополнительные сердца (до 5) и создаёт защитный оберег от урона.",
        iconName = "shield",
        maxRank = 2,
        baseCost = 90,
        costPerRank = 80,
        currentRank = shieldRank
      ),
      UpgradeItem(
        id = "shield_recharge",
        nameRu = "Регенерация Щита",
        descRu = "Защитный барьер автоматически восстанавливается после урона каждые 18 / 10 сек.",
        iconName = "shield_recharge",
        maxRank = 2,
        baseCost = 110,
        costPerRank = 80,
        currentRank = shieldRechargeRank
      ),
      UpgradeItem(
        id = "chrono_shift",
        nameRu = "Хроно-Сдвиг",
        descRu = "Особая способность: замедляет ход времени на 50% для совершения точнейших прыжков.",
        iconName = "chrono",
        maxRank = 2,
        baseCost = 120,
        costPerRank = 90,
        currentRank = chronoRank
      ),
      UpgradeItem(
        id = "phoenix_blessing",
        nameRu = "Благословение Феникса",
        descRu = "Один раз за уровень спасает от гибели в шипах или бездне, возрождая героя в портале.",
        iconName = "phoenix",
        maxRank = 1,
        baseCost = 150,
        costPerRank = 0,
        currentRank = phoenixRank
      )
    )
  }
}
