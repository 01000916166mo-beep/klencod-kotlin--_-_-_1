package com.clashangprpr.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.clashangprpr.app.engine.Assets
import com.clashangprpr.app.engine.LanMultiplayer
import com.clashangprpr.app.engine.SaveStore
import com.clashangprpr.app.engine.Sound
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * مملكة الحصاد والحرب 2.5D (Kingdoms of Harvest & War)
 * Brand-new standalone 2.5D Isometric Strategy & Farming Game built from scratch
 * in the visual style of Clash of Clans & Hay Day.
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    enum class BType(
        val code: Int,
        val titleAr: String,
        val descAr: String,
        val sprite: String,
        val costGold: Int,
        val costElixir: Int,
        val baseHp: Int,
        val isDefense: Boolean,
        val rangeTiles: Float = 0f,
        val atkDmg: Int = 0,
        val atkCd: Float = 1.1f,
        val scale: Float = 1.15f
    ) {
        TOWN_HALL(1, "قلعة القيادة الملكية", "قلب مملكتك! ترقيتها تفتح دفاعات ومزارع جديدة وتزيد السعة.", "iso/iso_townhall.png", 500, 250, 1800, false, 0f, 0, 1f, 1.55f),
        WINDMILL_BAKERY(2, "طاحونة ومخبز المزرعة", "يطحن القمح ويخبز مؤن الجيش وفطائر الشاحنة التجارية!", "iso/iso_bakery.png", 180, 80, 620, false, 0f, 0, 1f, 1.30f),
        ELIXIR_COLLECTOR(3, "مستخرج الإكسير السحري", "ينتج الإكسير البنفسجي لتدريب العمالقة والسحرة والغولم.", "iso/iso_elixir.png", 120, 40, 520, false, 0f, 0, 1f, 1.15f),
        GOLD_MINE(4, "منجم الذهب الملكي", "يستخرج الذهب الخالص لبناء الأبراج وتطوير الأسوار.", "iso/iso_goldmine.png", 120, 40, 520, false, 0f, 0, 1f, 1.20f),
        BARRACKS(5, "الثكنات ومعسكر الجيش", "يدرب المحاربين والرماة والعمالقة ومفجري الأسوار والغولم!", "iso/iso_barracks.png", 200, 120, 680, false, 0f, 0, 1f, 1.28f),
        CANNON(6, "مدفع القلعة الثقيل", "مدفع حديدي يقصف القوات البرية والعمالقة بقذائف ثقيلة.", "iso/iso_cannon.png", 240, 0, 720, true, 3.8f, 48, 0.95f, 1.12f),
        ARCHER_TOWER(7, "برج الرماة المتطور", "برج مراقبة عالٍ يطلق السهام السريعة (يتغير شكله مع كل ترقية 1..7!).", "iso/iso_archertower.png", 280, 60, 650, true, 4.6f, 34, 0.55f, 1.28f),
        WIZARD_TOWER(8, "برج الساحر البلوري", "يطلق طاقة بلورية متفجرة تحرق حشود المهاجمين معاً!", "iso/iso_wiztower.png", 380, 200, 780, true, 3.9f, 58, 1.30f, 1.32f),
        MORTAR(9, "مدفع الهاون العملاق", "يقصف قذائف بعيدة المدى تسحق الجيوش المتجمعة!", "iso/iso_mortar.png", 450, 240, 840, true, 5.4f, 82, 2.1f, 1.18f),
        CLAN_CASTLE(10, "قلعة الكلان والتحالف", "تخزن قوات دعم الكلان وتفتح حروب الكلانات 5 ضد 5!", "iso/iso_clancastle.png", 400, 250, 1350, false, 0f, 0, 1f, 1.35f),
        WHEAT_FARM(11, "حقل القمح الذهبي", "يزرع القمح الطازج لتمويل المخبز والشاحنة التجارية.", "iso/iso_farm.png", 70, 0, 360, false, 0f, 0, 1f, 1.12f),
        SHEEP_PASTURE(12, "مرعى الخراف والحليب", "يربي الخراف لإنتاج الصوف والحليب اللازم للمخبز والسوق.", "iso/iso_sheeppen.png", 140, 40, 460, false, 0f, 0, 1f, 1.18f),
        STONE_WALL(13, "سور الحصن الحجري", "سور دفاعي سميك يحمي القلعة ويعيق تقدم جيوش الأعداء.", "iso/iso_wall.png", 30, 0, 880, false, 0f, 0, 1f, 0.96f);

        fun spriteForLevel(lvl: Int): String {
            if (this == ARCHER_TOWER) {
                val clamped = lvl.coerceIn(1, 7)
                return "iso/iso_archertower_$clamped.png"
            }
            return sprite
        }

        companion object {
            fun byCode(c: Int): BType = values().firstOrNull { it.code == c } ?: STONE_WALL
        }
    }

    enum class TroopKind(
        val idx: Int,
        val nameAr: String,
        val roleAr: String,
        val sprite: String,
        val costElixir: Int,
        val costBread: Int,
        val maxHp: Int,
        val dmg: Int,
        val speedTiles: Float,
        val rangeTiles: Float,
        val targetsDefense: Boolean,
        val wallBreaker: Boolean,
        val accent: Int
    ) {
        BARBARIAN(0, "محارب البربر", "مقاتل شجاع يهاجم أقرب مبنى بالسيف", "iso/troop_barbarian.png", 25, 1, 220, 36, 1.55f, 0.75f, false, false, Color.parseColor("#FFB300")),
        ARCHER(1, "رامية السهام", "تقنص المباني والدفاعات من خلف الأسوار", "iso/troop_archer.png", 35, 1, 140, 28, 1.70f, 2.65f, false, false, Color.parseColor("#EC407A")),
        GIANT(2, "العملاق المدرع", "يتجه مباشرة لتدمير المدافع والأبراج الدفاعية!", "iso/troop_giant.png", 85, 2, 720, 54, 1.05f, 0.80f, true, false, Color.parseColor("#FF7043")),
        WALL_BREAKER(3, "مفجر الأسوار", "يركض بقنبلة بارود لنسف الأسوار الحجرية فوراً!", "iso/troop_wallbreaker.png", 50, 1, 125, 460, 2.25f, 0.65f, false, true, Color.parseColor("#FF5252")),
        WIZARD(4, "ساحر اللهب", "يطلق كرات لهب تحرق المباني بضرر مضاعف", "iso/troop_wizard.png", 105, 2, 240, 74, 1.35f, 2.85f, false, false, Color.parseColor("#29B6F6")),
        GOLEM(5, "الغولم الصخري", "عملاق صخري بـ 1300 صحة يمتص نيران المدافع!", "iso/troop_golem.png", 180, 4, 1300, 95, 0.85f, 0.85f, true, false, Color.parseColor("#00E5FF"))
    }

    class Building(var gx: Int, var gy: Int, val type: BType, var level: Int = 1) {
        val maxHp: Int get() = (type.baseHp * (1f + (level - 1) * 0.30f)).toInt()
        var hp: Int = maxHp
        var timer: Float = 0f
        var stored: Int = 0
        var readyHarvest: Boolean = true
        var atkCd: Float = 0f
        var hitFlash: Float = 0f
        val isDestroyed: Boolean get() = hp <= 0
    }

    class Troop(var gx: Float, var gy: Float, val kind: TroopKind) {
        var hp: Int = kind.maxHp
        val maxHp: Int = kind.maxHp
        var atkCd: Float = 0f
        var rageTimer: Float = 0f
        var walkPhase: Float = 0f
        var facingLeft: Boolean = false
        val isDead: Boolean get() = hp <= 0
    }

    class Shot(
        val sx: Float, val sy: Float,
        val tx: Float, val ty: Float,
        val dmg: Int, val fromDefense: Boolean,
        val splash: Boolean, val color: Int, val arcHeight: Float
    ) {
        var t: Float = 0f
        val gx: Float get() = sx + (tx - sx) * t
        val gy: Float get() = sy + (ty - sy) * t
    }

    class BlastFx(val gx: Float, val gy: Float, val color: Int, val maxR: Float = 42f) {
        var t: Float = 0f
        val done: Boolean get() = t >= 0.45f
    }

    class FloatLabel(val text: String, val gx: Float, val gy: Float, val color: Int) {
        var t: Float = 0f
        val done: Boolean get() = t >= 1.25f
    }

    data class TruckOrder(var title: String, var wheat: Int, var wool: Int, var bread: Int, var goldReward: Int, var elixirReward: Int)
    data class MarketItem(val seller: String, val label: String, val code: String, val qty: Int, val price: Int)
    private data class ClickZone(val rect: RectF, val id: String)

    companion object {
        const val GRID_N = 12 // 12x12 Isometric Diamond Grid mapped onto the central grass island
    }

    private val boot: Boolean = run {
        Assets.init(context)
        SaveStore.init(context)
        Sound.init(context)
        LanMultiplayer.startDiscoveryListener()
        true
    }

    private var thread: Thread? = null
    @Volatile private var running = false
    private var curW = 1280f
    private var curH = 720f
    private var u = 1f
    private var time = 0f

    // Player Resources & Progress
    private var playerLevel = 18
    private var gold = 52400
    private var elixir = 12500
    private var gems = 350
    private var trophies = 1250
    private var builders = 3
    private var wheat = 24
    private var wool = 14
    private var bread = 16
    private var clanName = "فرسان المسك ⚔️"
    private var clanReinforcementsReady = true
    private var clanReinforcementsUsed = false
    private var myWarStars = 11
    private var enemyWarStars = 8
    private val warStarsPerBase = intArrayOf(3, 3, 2, 3, 0)
    private var warChestClaimed = false
    private var activeWarIdx = -1

    private val army = intArrayOf(10, 8, 4, 4, 3, 2)
    private var healSpells = 2
    private var rageSpells = 2
    private var lightningSpells = 2

    private val myVillage = ArrayList<Building>()
    private val raidVillage = ArrayList<Building>()
    private val troops = ArrayList<Troop>()
    private val shots = ArrayList<Shot>()
    private val blasts = ArrayList<BlastFx>()
    private val floatLabels = ArrayList<FloatLabel>()
    private val truckOrders = ArrayList<TruckOrder>()
    private val marketItems = ArrayList<MarketItem>()
    private val clanChat = ArrayList<String>().apply {
        add("القائد مصطفى: أهلاً بكم في مملكة الحصاد والحرب 2.5D! 🏰🔥")
        add("الفارس عمر: دعم الكلان (2 عملاق + 1 غولم صخري) جاهز في قلعة الكلان 🛡️")
        add("الحكيم يونس: اهجموا على القلعة #5 لحسم حرب الكلانات واستلام الصندوق الملكي!")
    }

    // Active Mode & Dialogs
    /** 0 = My 2.5D Village & Farm Island, 1 = Live 3-Star Raid Battle */
    private var mode = 0
    /** 0 = None, 1 = Build Shop, 2 = Army & Bakery, 3 = Truck & Market, 4 = Clans & Clan Wars, 5 = Cloud & Wi-Fi Online, 6 = Gem Shop */
    private var dialog = 0

    private var selectedBuilding: Building? = null
    private var movingBuilding = false
    private var pendingBuild: BType? = null
    private var deploySlot = 0 // 0..5 Troops, 6=Heal, 7=Rage, 8=Lightning, 9=Clan Reinforcements

    // Raid State
    private var raidEnemyTitle = "حصن الإمبراطور الأحمر"
    private var raidTotalBld = 1
    private var raidDestroyedBld = 0
    private var raidThDown = false
    private var raidFinished = false
    private var raidTimer = 150f
    private var lootGold = 0
    private var lootElixir = 0

    // Cloud API State (apk3rade.mangoad.online with InfinityFree + Cloudflare AES solver)
    private var cloudApiUrl = "https://apk3rade.mangoad.online/server_api.php"
    private var infinityCookie = ""
    private var cloudStatus = "متصل بسحابة apk3rade.mangoad.online + واي فاي محلي 🌐"

    private var toastMsg = ""
    private var toastTimer = 0f

    // Reusable Paints & Paths
    private val pFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val pText = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val path = Path()
    private val clickZones = ArrayList<ClickZone>()

    init {
        holder.addCallback(this)
        isFocusable = true
        loadOrCreateKingdom()
        initOrdersAndMarket()
    }

    private fun initOrdersAndMarket() {
        truckOrders.clear()
        truckOrders.add(TruckOrder("قافلة القرية الملكية 🚚", 4, 2, 2, 1800, 1200))
        truckOrders.add(TruckOrder("تموين فرسان الحصن 🥖", 6, 3, 4, 3200, 2400))
        truckOrders.add(TruckOrder("سفينة التجارة الكبرى 🚢", 10, 5, 6, 5500, 4200))

        marketItems.clear()
        marketItems.add(MarketItem("مزرعة السلطان", "25 قمح ذهبي 🌾", "WHEAT", 25, 900))
        marketItems.add(MarketItem("مخبز القلعة", "10 مؤن خبز ساخن 🥖", "BREAD", 10, 1200))
        marketItems.add(MarketItem("مرعى النيل", "12 صوف وحليب 🐑", "WOOL", 12, 850))
        marketItems.add(MarketItem("ساحر الشرق", "2500 إكسير سحري 🧪", "ELIXIR", 2500, 1500))
    }

    private fun loadOrCreateKingdom() {
        myVillage.clear()
        gold = SaveStore.int("k25_gold", 52400)
        elixir = SaveStore.int("k25_elixir", 12500)
        gems = SaveStore.int("k25_gems", 350)
        trophies = SaveStore.int("k25_trophies", 1250)
        builders = SaveStore.int("k25_builders", 3)
        wheat = SaveStore.int("k25_wheat", 24)
        wool = SaveStore.int("k25_wool", 14)
        bread = SaveStore.int("k25_bread", 16)
        val savedCode = SaveStore.str("k25_layout", "")
        if (savedCode.isNotEmpty() && decodeVillage(savedCode, myVillage)) {
            return
        }

        // Default showcase layout matching the preview image!
        myVillage.add(Building(5, 5, BType.TOWN_HALL, 4))
        myVillage.add(Building(4, 4, BType.CANNON, 3))
        myVillage.add(Building(7, 4, BType.ARCHER_TOWER, 5))
        myVillage.add(Building(4, 7, BType.WIZARD_TOWER, 3))
        myVillage.add(Building(7, 7, BType.CANNON, 3))
        myVillage.add(Building(6, 4, BType.MORTAR, 3))
        myVillage.add(Building(6, 7, BType.ELIXIR_COLLECTOR, 4))
        myVillage.add(Building(4, 6, BType.GOLD_MINE, 4))
        myVillage.add(Building(7, 6, BType.CLAN_CASTLE, 3))

        // Stone Fortress Wall Ring around (3..8, 3..8)
        for (i in 3..8) {
            myVillage.add(Building(i, 3, BType.STONE_WALL, 3))
            myVillage.add(Building(i, 8, BType.STONE_WALL, 3))
        }
        for (j in 4..7) {
            myVillage.add(Building(3, j, BType.STONE_WALL, 3))
            myVillage.add(Building(8, j, BType.STONE_WALL, 3))
        }

        // Outer Farming & Production Wing (Windmill Bakery, Golden Wheat Fields, Sheep Pastures & Barracks)
        myVillage.add(Building(1, 3, BType.WINDMILL_BAKERY, 3))
        myVillage.add(Building(1, 5, BType.WHEAT_FARM, 3))
        myVillage.add(Building(1, 6, BType.WHEAT_FARM, 3))
        myVillage.add(Building(2, 5, BType.WHEAT_FARM, 3))
        myVillage.add(Building(2, 6, BType.WHEAT_FARM, 3))
        myVillage.add(Building(1, 8, BType.SHEEP_PASTURE, 3))
        myVillage.add(Building(3, 1, BType.SHEEP_PASTURE, 3))
        myVillage.add(Building(9, 4, BType.BARRACKS, 4))
        myVillage.add(Building(9, 6, BType.GOLD_MINE, 4))
        myVillage.add(Building(9, 8, BType.ARCHER_TOWER, 6))
        saveKingdom()
    }

    private fun saveKingdom() {
        val m = HashMap<String, Any>()
        m["k25_gold"] = gold
        m["k25_elixir"] = elixir
        m["k25_gems"] = gems
        m["k25_trophies"] = trophies
        m["k25_builders"] = builders
        m["k25_wheat"] = wheat
        m["k25_wool"] = wool
        m["k25_bread"] = bread
        m["k25_layout"] = encodeVillage(myVillage)
        SaveStore.putAll(m)
    }

    private fun encodeVillage(list: List<Building>): String {
        val sb = StringBuilder("K25:")
        for (b in list) {
            sb.append(b.gx).append(',').append(b.gy).append(',').append(b.type.code).append(',').append(b.level).append(';')
        }
        return sb.toString()
    }

    private fun decodeVillage(code: String, out: MutableList<Building>): Boolean {
        if (!code.startsWith("K25:")) return false
        val parsed = ArrayList<Building>()
        for (tok in code.substring(4).split(';')) {
            if (tok.isBlank()) continue
            val p = tok.split(',')
            if (p.size < 4) continue
            val gx = p[0].toIntOrNull()?.coerceIn(0, GRID_N - 1) ?: continue
            val gy = p[1].toIntOrNull()?.coerceIn(0, GRID_N - 1) ?: continue
            val t = BType.byCode(p[2].toIntOrNull() ?: 13)
            val lvl = p[3].toIntOrNull()?.coerceIn(1, 10) ?: 1
            parsed.add(Building(gx, gy, t, lvl))
        }
        if (parsed.isEmpty()) return false
        out.clear()
        out.addAll(parsed)
        return true
    }

    // =========================================================================
    // 2.5D ISOMETRIC DIAMOND COORDINATE PROJECTION (Aligned to iso_island_bg.jpg)
    // =========================================================================
    /** Maps grid (gx, gy) in 0..12 onto the central emerald diamond clearing of iso_island_bg.jpg! */
    private fun isoToScreen(gx: Float, gy: Float): Pair<Float, Float> {
        val centerX = curW * 0.50f
        val topY = curH * 0.11f
        val diamondW = curW * 0.64f
        val diamondH = curH * 0.75f
        val halfW = (diamondW * 0.5f) / GRID_N
        val halfH = (diamondH * 0.5f) / GRID_N
        val sx = centerX + (gx - gy) * halfW
        val sy = topY + (gx + gy) * halfH
        return Pair(sx, sy)
    }

    private fun screenToIso(sx: Float, sy: Float): Pair<Float, Float> {
        val centerX = curW * 0.50f
        val topY = curH * 0.11f
        val diamondW = curW * 0.64f
        val diamondH = curH * 0.75f
        val halfW = (diamondW * 0.5f) / GRID_N
        val halfH = (diamondH * 0.5f) / GRID_N
        val dx = (sx - centerX) / halfW
        val dy = (sy - topY) / halfH
        val gx = (dy + dx) * 0.5f
        val gy = (dy - dx) * 0.5f
        return Pair(gx, gy)
    }

    // =========================================================================
    // GAME UPDATE LOOP (60 FPS)
    // =========================================================================
    private fun update(dt: Float) {
        time += dt
        if (toastTimer > 0f) toastTimer = max(0f, toastTimer - dt)

        // Check incoming Wi-Fi / Hotspot village raid challenge
        while (true) {
            val pkt = LanMultiplayer.incoming.poll() ?: break
            if (pkt.startsWith("K25_RAID|")) {
                val p = pkt.split("|")
                if (p.size >= 3 && decodeVillage(p[2], raidVillage)) {
                    beginRaid("قرية صديقك (${p[1]}) 🤝", raidVillage)
                    Sound.play("boss")
                }
            }
        }

        // Update Blasts & Floating Labels
        var bi = blasts.size - 1
        while (bi >= 0) {
            blasts[bi].t += dt
            if (blasts[bi].done) blasts.removeAt(bi)
            bi--
        }
        var fi = floatLabels.size - 1
        while (fi >= 0) {
            floatLabels[fi].t += dt
            if (floatLabels[fi].done) floatLabels.removeAt(fi)
            fi--
        }

        if (mode == 0) {
            for (b in myVillage) {
                if (b.hitFlash > 0f) b.hitFlash = max(0f, b.hitFlash - dt)
                b.timer += dt * (1f + (builders - 1) * 0.2f)
                when (b.type) {
                    BType.WHEAT_FARM -> if (!b.readyHarvest && b.timer >= 8f) {
                        b.timer = 0f
                        b.readyHarvest = true
                    }
                    BType.SHEEP_PASTURE -> if (b.timer >= 10f) {
                        b.timer = 0f
                        b.stored = min(15, b.stored + b.level)
                        b.readyHarvest = true
                    }
                    BType.GOLD_MINE -> if (b.timer >= 5f) {
                        b.timer = 0f
                        b.stored = min(2000, b.stored + 85 * b.level)
                        b.readyHarvest = true
                    }
                    BType.ELIXIR_COLLECTOR -> if (b.timer >= 5f) {
                        b.timer = 0f
                        b.stored = min(2000, b.stored + 75 * b.level)
                        b.readyHarvest = true
                    }
                    BType.WINDMILL_BAKERY -> if (b.timer >= 12f && wheat >= 1) {
                        b.timer = 0f
                        wheat--
                        bread += b.level
                    }
                    else -> {}
                }
            }
        } else if (mode == 1 && !raidFinished) {
            raidTimer = max(0f, raidTimer - dt)
            if (raidTimer <= 0f) {
                endRaidBattle()
                return
            }
            updateRaidBattle(dt)
        }
    }

    private fun updateRaidBattle(dt: Float) {
        for (b in raidVillage) if (b.hitFlash > 0f) b.hitFlash = max(0f, b.hitFlash - dt)

        // 1. Troops AI
        for (tr in troops) {
            if (tr.isDead) continue
            tr.walkPhase += dt * 7f
            if (tr.rageTimer > 0f) tr.rageTimer -= dt
            if (tr.atkCd > 0f) tr.atkCd -= dt

            var bestB: Building? = null
            var bestScore = 1e9f
            for (b in raidVillage) {
                if (b.isDestroyed) continue
                val d = hypot((b.gx + 0.5f - tr.gx).toDouble(), (b.gy + 0.5f - tr.gy).toDouble()).toFloat()
                val score = when {
                    tr.kind.wallBreaker && b.type == BType.STONE_WALL -> d * 0.10f
                    tr.kind.targetsDefense && b.type.isDefense -> d * 0.20f
                    b.type == BType.STONE_WALL && !tr.kind.wallBreaker -> d * 2.5f
                    else -> d
                }
                if (score < bestScore) {
                    bestScore = score
                    bestB = b
                }
            }

            if (bestB != null) {
                val tx = bestB.gx + 0.5f
                val ty = bestB.gy + 0.5f
                val dx = tx - tr.gx
                val dy = ty - tr.gy
                val dist = max(0.05f, sqrt(dx * dx + dy * dy))
                tr.facingLeft = (dx - dy) < 0f
                val spdMul = if (tr.rageTimer > 0f) 1.6f else 1f
                val dmgMul = if (tr.rageTimer > 0f) 1.6f else 1f

                if (dist > tr.kind.rangeTiles) {
                    val step = tr.kind.speedTiles * spdMul * dt
                    tr.gx += (dx / dist) * step
                    tr.gy += (dy / dist) * step
                } else if (tr.atkCd <= 0f) {
                    tr.atkCd = if (tr.rageTimer > 0f) 0.52f else 0.85f
                    if (tr.kind.wallBreaker) {
                        val dmg = if (bestB.type == BType.STONE_WALL) tr.kind.dmg * 2 else tr.kind.dmg
                        damageRaidBuilding(bestB, dmg)
                        blasts.add(BlastFx(tx, ty, Color.parseColor("#FF5722"), 56f))
                        tr.hp = 0
                        Sound.play("explode", 0.85f)
                    } else if (tr.kind.rangeTiles > 1.2f) {
                        shots.add(Shot(tr.gx, tr.gy, tx, ty, (tr.kind.dmg * dmgMul).toInt(), false, tr.kind == TroopKind.WIZARD, tr.kind.accent, 18f))
                        Sound.play("swing", 0.5f)
                    } else {
                        damageRaidBuilding(bestB, (tr.kind.dmg * dmgMul).toInt())
                        Sound.play("hit", 0.6f)
                    }
                }
            }
        }

        // 2. Defensive Towers AI
        for (b in raidVillage) {
            if (b.isDestroyed || !b.type.isDefense) continue
            if (b.atkCd > 0f) b.atkCd -= dt
            if (b.atkCd <= 0f) {
                val bx = b.gx + 0.5f
                val by = b.gy + 0.5f
                var target: Troop? = null
                var bestD = b.type.rangeTiles
                for (tr in troops) {
                    if (tr.isDead) continue
                    val d = sqrt((tr.gx - bx) * (tr.gx - bx) + (tr.gy - by) * (tr.gy - by))
                    if (d <= bestD) {
                        bestD = d
                        target = tr
                    }
                }
                if (target != null) {
                    b.atkCd = b.type.atkCd
                    val dmg = (b.type.atkDmg * (1f + (b.level - 1) * 0.22f)).toInt()
                    val splash = b.type == BType.WIZARD_TOWER || b.type == BType.MORTAR
                    val arc = if (b.type == BType.MORTAR) 68f else 22f
                    val col = when (b.type) {
                        BType.WIZARD_TOWER -> Color.parseColor("#00E5FF")
                        BType.MORTAR -> Color.parseColor("#FF5722")
                        BType.ARCHER_TOWER -> Color.parseColor("#FFD54F")
                        else -> Color.parseColor("#ECEFF1")
                    }
                    shots.add(Shot(bx, by, target.gx, target.gy, dmg, true, splash, col, arc))
                }
            }
        }

        // 3. Projectiles
        var si = shots.size - 1
        while (si >= 0) {
            val s = shots[si]
            s.t += dt * (if (s.arcHeight > 40f) 1.5f else 3.2f)
            if (s.t >= 1f) {
                if (s.fromDefense) {
                    for (tr in troops) {
                        if (tr.isDead) continue
                        val rad = if (s.splash) 1.5f else 0.7f
                        if (sqrt((tr.gx - s.tx) * (tr.gx - s.tx) + (tr.gy - s.ty) * (tr.gy - s.ty)) <= rad) {
                            tr.hp -= s.dmg
                        }
                    }
                } else {
                    for (b in raidVillage) {
                        if (b.isDestroyed) continue
                        val rad = if (s.splash) 1.4f else 0.8f
                        if (sqrt((b.gx + 0.5f - s.tx) * (b.gx + 0.5f - s.tx) + (b.gy + 0.5f - s.ty) * (b.gy + 0.5f - s.ty)) <= rad) {
                            damageRaidBuilding(b, s.dmg)
                            if (!s.splash) break
                        }
                    }
                }
                if (s.splash) blasts.add(BlastFx(s.tx, s.ty, s.color, 44f))
                shots.removeAt(si)
            }
            si--
        }

        val nonWalls = raidVillage.filter { it.type != BType.STONE_WALL }
        raidTotalBld = max(1, nonWalls.size)
        raidDestroyedBld = nonWalls.count { it.isDestroyed }
        if (raidDestroyedBld >= raidTotalBld) {
            endRaidBattle()
        }
    }

    private fun damageRaidBuilding(b: Building, dmg: Int) {
        if (b.isDestroyed) return
        b.hp -= dmg
        b.hitFlash = 0.18f
        if (b.hp <= 0) {
            b.hp = 0
            blasts.add(BlastFx(b.gx + 0.5f, b.gy + 0.5f, Color.parseColor("#FF9800"), 52f))
            Sound.play("explode", 0.75f)
            if (b.type == BType.TOWN_HALL) {
                raidThDown = true
                lootGold += 2500 * b.level
                lootElixir += 1800 * b.level
                floatLabels.add(FloatLabel("تدمير قلعة القيادة! ⭐", b.gx + 0.5f, b.gy.toFloat(), Color.parseColor("#FFD54F")))
            } else if (b.type != BType.STONE_WALL) {
                val g = 350 * b.level
                val e = 240 * b.level
                lootGold += g
                lootElixir += e
                floatLabels.add(FloatLabel("+${g}🪙", b.gx + 0.5f, b.gy.toFloat(), Color.parseColor("#FFD54F")))
            }
        }
    }

    private fun destructionPct(): Int = ((raidDestroyedBld * 100) / max(1, raidTotalBld)).coerceIn(0, 100)

    private fun raidStars(): Int {
        var s = 0
        val pct = destructionPct()
        if (pct >= 50) s++
        if (raidThDown) s++
        if (pct >= 100) s++
        return s
    }

    private fun endRaidBattle() {
        if (raidFinished) return
        raidFinished = true
        val st = raidStars()
        gold += lootGold
        elixir += lootElixir
        if (st > 0) {
            trophies += st * 15
            gems += st * 5
            wheat += st * 4
            if (activeWarIdx in 0..4) {
                val old = warStarsPerBase[activeWarIdx]
                if (st > old) {
                    myWarStars += (st - old)
                    warStarsPerBase[activeWarIdx] = st
                }
                activeWarIdx = -1
            }
            Sound.play("victory")
        } else {
            trophies = max(0, trophies - 10)
            Sound.play("deny")
        }
        saveKingdom()
    }

    private fun startMatchmakingRaid(difficultyOffset: Int = 0) {
        val lvl = (3 + difficultyOffset).coerceIn(2, 7)
        val generated = ArrayList<Building>()
        generated.add(Building(5, 5, BType.TOWN_HALL, lvl))
        generated.add(Building(4, 4, BType.CANNON, lvl))
        generated.add(Building(7, 4, BType.ARCHER_TOWER, lvl))
        generated.add(Building(4, 7, BType.WIZARD_TOWER, lvl))
        generated.add(Building(7, 7, BType.CANNON, lvl))
        generated.add(Building(6, 4, BType.MORTAR, lvl))
        generated.add(Building(6, 7, BType.CLAN_CASTLE, lvl))
        for (i in 3..8) {
            generated.add(Building(i, 3, BType.STONE_WALL, lvl))
            generated.add(Building(i, 8, BType.STONE_WALL, lvl))
        }
        for (j in 4..7) {
            generated.add(Building(3, j, BType.STONE_WALL, lvl))
            generated.add(Building(8, j, BType.STONE_WALL, lvl))
        }
        generated.add(Building(1, 4, BType.WINDMILL_BAKERY, lvl))
        generated.add(Building(1, 6, BType.WHEAT_FARM, lvl))
        generated.add(Building(2, 6, BType.SHEEP_PASTURE, lvl))
        generated.add(Building(9, 4, BType.GOLD_MINE, lvl))
        generated.add(Building(9, 6, BType.ELIXIR_COLLECTOR, lvl))
        generated.add(Building(9, 8, BType.BARRACKS, lvl))

        val titles = arrayOf("مملكة الأسد الذهبي", "قلعة فرسان الشمال", "حصن الساحر الأعظم", "إمبراطورية البركان", "جزيرة القائد صلاح")
        beginRaid("${titles[(trophies + difficultyOffset) % titles.size]} (Lv.$lvl)", generated)
    }

    private fun beginRaid(enemyTitle: String, targetLayout: List<Building>) {
        if (targetLayout !== raidVillage) {
            raidVillage.clear()
            for (b in targetLayout) raidVillage.add(Building(b.gx, b.gy, b.type, b.level))
        } else {
            for (b in raidVillage) b.hp = b.maxHp
        }
        troops.clear()
        shots.clear()
        blasts.clear()
        floatLabels.clear()
        raidEnemyTitle = enemyTitle
        raidTotalBld = max(1, raidVillage.count { it.type != BType.STONE_WALL })
        raidDestroyedBld = 0
        raidThDown = false
        raidFinished = false
        clanReinforcementsUsed = false
        raidTimer = 150f
        lootGold = 0
        lootElixir = 0
        mode = 1
        dialog = 0
        selectedBuilding = null
    }

    // =========================================================================
    // RENDERING: 2.5D ISOMETRIC ISLAND, SPRITES & SUPERCELL GLOSSY UI
    // =========================================================================
    private fun render(c: Canvas) {
        curW = width.toFloat().coerceAtLeast(1f)
        curH = height.toFloat().coerceAtLeast(1f)
        u = min(curW / 1280f, curH / 720f)
        clickZones.clear()

        // 1. Draw the High-Res 2.5D Isometric Island Meadow Background (`iso/iso_island_bg.jpg`)
        val bg = Assets.bmp("iso/iso_island_bg.jpg")
        if (bg != null) {
            c.drawBitmap(bg, null, RectF(0f, 0f, curW, curH), null)
        } else {
            c.drawColor(Color.rgb(42, 125, 62))
        }

        // Subtle grid lines when moving or placing a building
        if (mode == 0 && (movingBuilding || pendingBuild != null || selectedBuilding != null)) {
            pStroke.strokeWidth = 1.6f * u
            pStroke.color = Color.argb(75, 255, 255, 255)
            for (i in 0..GRID_N) {
                val (x0, y0) = isoToScreen(i.toFloat(), 0f)
                val (x1, y1) = isoToScreen(i.toFloat(), GRID_N.toFloat())
                c.drawLine(x0, y0, x1, y1, pStroke)
                val (rx0, ry0) = isoToScreen(0f, i.toFloat())
                val (rx1, ry1) = isoToScreen(GRID_N.toFloat(), i.toFloat())
                c.drawLine(rx0, ry0, rx1, ry1, pStroke)
            }
        }

        // 2. Depth-Sorted 2.5D Buildings & Troops (Painter's Algorithm by gx + gy)
        val activeList = if (mode == 0) myVillage else raidVillage
        val sortedBuildings = activeList.sortedBy { it.gx + it.gy }

        for (b in sortedBuildings) {
            val (cx, cy) = isoToScreen(b.gx + 0.5f, b.gy + 0.5f)
            if (mode == 1 && b.isDestroyed) {
                pFill.color = Color.argb(135, 55, 45, 38)
                c.drawOval(RectF(cx - 28f * u, cy - 12f * u, cx + 28f * u, cy + 14f * u), pFill)
                continue
            }

            // Selection highlight diamond on the grass
            val isSel = (mode == 0 && selectedBuilding === b)
            if (isSel) {
                val (nX, nY) = isoToScreen(b.gx.toFloat(), b.gy.toFloat())
                val (eX, eY) = isoToScreen(b.gx + 1f, b.gy.toFloat())
                val (sX, sY) = isoToScreen(b.gx + 1f, b.gy + 1f)
                val (wX, wY) = isoToScreen(b.gx.toFloat(), b.gy + 1f)
                path.reset()
                path.moveTo(nX, nY); path.lineTo(eX, eY); path.lineTo(sX, sY); path.lineTo(wX, wY); path.close()
                pFill.color = Color.argb(95, 105, 240, 174)
                c.drawPath(path, pFill)
                pStroke.strokeWidth = 3f * u
                pStroke.color = Color.parseColor("#00E676")
                c.drawPath(path, pStroke)

                if (b.type.isDefense) {
                    pFill.color = Color.argb(36, 0, 229, 255)
                    pStroke.color = Color.argb(140, 0, 229, 255)
                    val rPx = b.type.rangeTiles * 46f * u
                    c.drawOval(RectF(cx - rPx, cy - rPx * 0.55f, cx + rPx, cy + rPx * 0.55f), pFill)
                    c.drawOval(RectF(cx - rPx, cy - rPx * 0.55f, cx + rPx, cy + rPx * 0.55f), pStroke)
                }
            }

            // Soft 2.5D ground contact shadow
            val rad = 38f * u * b.type.scale
            pFill.color = Color.argb(75, 0, 0, 0)
            c.drawOval(RectF(cx - rad * 0.72f, cy - rad * 0.25f, cx + rad * 0.72f, cy + rad * 0.42f), pFill)

            // Draw the high-res 2.5D Isometric Sprite!
            val bmp = Assets.bmp(b.type.spriteForLevel(b.level))
            if (bmp != null) {
                val aspect = bmp.width.toFloat() / max(1, bmp.height)
                val drawH = 76f * u * b.type.scale
                val drawW = drawH * aspect
                val bounce = if (b.hitFlash > 0f) sin(b.hitFlash * 50f) * 5f * u else 0f
                val dst = RectF(cx - drawW * 0.5f + bounce, cy - drawH * 0.78f, cx + drawW * 0.5f + bounce, cy + drawH * 0.22f)
                c.drawBitmap(bmp, null, dst, null)
            }

            // Level badge pill
            if (b.type != BType.STONE_WALL) {
                drawBadge(c, cx + 22f * u, cy + 8f * u, "Lv.${b.level}", Color.parseColor("#FFD54F"), Color.rgb(30, 24, 15))
            }

            // Harvest / Collect floating glossy icon in Mode 0
            if (mode == 0 && ((b.type == BType.WHEAT_FARM && b.readyHarvest) || b.stored >= 50 || (b.type == BType.SHEEP_PASTURE && b.stored >= 2))) {
                val bob = sin(time * 5.5f + b.gx) * 4f * u
                val bubR = RectF(cx - 18f * u, cy - 68f * u + bob, cx + 18f * u, cy - 34f * u + bob)
                drawGlossyButton(c, bubR, "", Color.parseColor("#FFB300"), Color.parseColor("#F57C00"), 12f)
                val sym = when (b.type) {
                    BType.WHEAT_FARM -> "🌾"
                    BType.SHEEP_PASTURE -> "🐑"
                    BType.GOLD_MINE -> "🪙"
                    else -> "🧪"
                }
                drawShadowText(c, sym, bubR.centerX(), bubR.centerY() + 6f * u, 16f * u, Color.WHITE)
            }

            // Health bar in Raid Mode
            if (mode == 1 && b.hp < b.maxHp) {
                drawProgressBar(c, RectF(cx - 28f * u, cy - 58f * u, cx + 28f * u, cy - 48f * u), b.hp.toFloat() / max(1, b.maxHp), Color.parseColor("#00E676"))
            }
        }

        // 3. Draw 2.5D Isometric Troops, Projectiles & Explosions in Raid Mode
        if (mode == 1) {
            val sortedTroops = troops.filter { !it.isDead }.sortedBy { it.gx + it.gy }
            for (tr in sortedTroops) {
                val (tx, ty) = isoToScreen(tr.gx, tr.gy)
                val hop = abs(sin(tr.walkPhase)) * 5f * u
                // Shadow
                pFill.color = Color.argb(95, 0, 0, 0)
                c.drawOval(RectF(tx - 16f * u, ty - 4f * u, tx + 16f * u, ty + 8f * u), pFill)
                if (tr.rageTimer > 0f) {
                    pFill.color = Color.argb(95, 236, 64, 122)
                    c.drawCircle(tx, ty - 16f * u, 26f * u, pFill)
                }
                val tbmp = Assets.bmp(tr.kind.sprite)
                if (tbmp != null) {
                    val sz = if (tr.kind == TroopKind.GOLEM || tr.kind == TroopKind.GIANT) 64f * u else 48f * u
                    val r = RectF(tx - sz * 0.5f, ty - sz * 0.85f - hop, tx + sz * 0.5f, ty + sz * 0.15f - hop)
                    c.save()
                    if (tr.facingLeft) c.scale(-1f, 1f, tx, ty)
                    c.drawBitmap(tbmp, null, r, null)
                    c.restore()
                }
                drawProgressBar(c, RectF(tx - 18f * u, ty - 48f * u - hop, tx + 18f * u, ty - 41f * u - hop), tr.hp.toFloat() / max(1, tr.maxHp), Color.parseColor("#00E676"))
            }

            for (s in shots) {
                val (px, py) = isoToScreen(s.gx, s.gy)
                val arc = sin(s.t * 3.14159f) * s.arcHeight * u
                pFill.color = s.color
                c.drawCircle(px, py - arc, (if (s.splash) 8.5f else 5.5f) * u, pFill)
                pStroke.strokeWidth = 2f * u
                pStroke.color = Color.WHITE
                c.drawCircle(px, py - arc, (if (s.splash) 8.5f else 5.5f) * u, pStroke)
            }
        }

        for (bl in blasts) {
            val (bx, by) = isoToScreen(bl.gx, bl.gy)
            val prog = (bl.t / 0.45f).coerceIn(0f, 1f)
            pFill.color = Color.argb(((1f - prog) * 190).toInt(), Color.red(bl.color), Color.green(bl.color), Color.blue(bl.color))
            c.drawCircle(bx, by - 12f * u, bl.maxR * u * prog, pFill)
        }

        for (fl in floatLabels) {
            val (fx, fy) = isoToScreen(fl.gx, fl.gy)
            drawShadowText(c, fl.text, fx, fy - 42f * u - fl.t * 38f * u, 16f * u, fl.color)
        }

        // 4. Draw Supercell / Clash of Clans Style Glossy HUD Overlays!
        drawSupercellHud(c)

        // 5. Draw Active Dialog Modal if open
        if (dialog != 0) {
            drawActiveDialog(c)
        }

        // 6. Toast Banner
        if (toastTimer > 0f && toastMsg.isNotEmpty()) {
            val tw = min(curW * 0.78f, 760f * u)
            val tr = RectF(curW * 0.5f - tw * 0.5f, 88f * u, curW * 0.5f + tw * 0.5f, 132f * u)
            drawGlossyPanel(c, tr, Color.argb(235, 22, 32, 48), Color.parseColor("#FFD54F"))
            drawShadowText(c, toastMsg, tr.centerX(), tr.centerY() + 6f * u, 16.5f * u, Color.WHITE)
        }
    }

    private fun drawSupercellHud(c: Canvas) {
        // Top-Left: Level Star Badge + Trophy Counter + Builders & Farm Summary
        val lvlBar = RectF(18f * u, 14f * u, 240f * u, 54f * u)
        drawGlossyPanel(c, lvlBar, Color.argb(220, 20, 42, 68), Color.parseColor("#29B6F6"))
        drawProgressBar(c, RectF(58f * u, 22f * u, 228f * u, 46f * u), 0.72f, Color.parseColor("#00E5FF"))
        pFill.color = Color.parseColor("#29B6F6")
        c.drawCircle(38f * u, 34f * u, 24f * u, pFill)
        pStroke.strokeWidth = 3f * u; pStroke.color = Color.WHITE
        c.drawCircle(38f * u, 34f * u, 24f * u, pStroke)
        drawShadowText(c, "$playerLevel", 38f * u, 41f * u, 18f * u, Color.WHITE)
        drawShadowText(c, "مملكة الحصاد 2.5D", 142f * u, 40f * u, 13.5f * u, Color.WHITE)

        val trophyBar = RectF(18f * u, 64f * u, 200f * u, 108f * u)
        drawGlossyPanel(c, trophyBar, Color.argb(225, 48, 34, 18), Color.parseColor("#FFD54F"))
        drawShadowText(c, "🏆 $trophies", trophyBar.centerX(), trophyBar.centerY() + 7f * u, 18f * u, Color.parseColor("#FFD54F"))
        clickZones.add(ClickZone(trophyBar, "hud_clans"))

        // Top-Right: Gold, Elixir, Gems & Farm Rations Bars (Just like the preview image!)
        var rx = curW - 18f * u
        val gemBar = RectF(rx - 170f * u, 14f * u, rx, 54f * u)
        drawGlossyPanel(c, gemBar, Color.argb(225, 16, 48, 32), Color.parseColor("#00E676"))
        drawShadowText(c, "💎 $gems  ➕", gemBar.centerX(), gemBar.centerY() + 6f * u, 16.5f * u, Color.parseColor("#69F0AE"))
        clickZones.add(ClickZone(gemBar, "hud_gemshop"))
        rx -= 182f * u

        val elixBar = RectF(rx - 210f * u, 14f * u, rx, 54f * u)
        drawGlossyPanel(c, elixBar, Color.argb(225, 48, 18, 58), Color.parseColor("#E040FB"))
        drawShadowText(c, "🧪 $elixir  ➕", elixBar.centerX(), elixBar.centerY() + 6f * u, 16.5f * u, Color.parseColor("#F8BBD0"))
        clickZones.add(ClickZone(elixBar, "hud_gemshop"))
        rx -= 222f * u

        val goldBar = RectF(rx - 220f * u, 14f * u, rx, 54f * u)
        drawGlossyPanel(c, goldBar, Color.argb(225, 58, 42, 14), Color.parseColor("#FFD54F"))
        drawShadowText(c, "🪙 $gold  ➕", goldBar.centerX(), goldBar.centerY() + 6f * u, 16.5f * u, Color.parseColor("#FFF59D"))
        clickZones.add(ClickZone(goldBar, "hud_gemshop"))

        val farmPill = RectF(curW - 412f * u, 64f * u, curW - 18f * u, 104f * u)
        drawGlossyPanel(c, farmPill, Color.argb(220, 26, 48, 28), Color.parseColor("#8BC34A"))
        drawShadowText(c, "🌾 قمح:$wheat  •  🐑 صوف:$wool  •  🥖 خبز:$bread  •  👷 عمال:$builders/5", farmPill.centerX(), farmPill.centerY() + 5f * u, 13.5f * u, Color.WHITE)
        clickZones.add(ClickZone(farmPill, "hud_harvest_all"))

        if (mode == 1) {
            // Raid Battle Top Center Banner (Stars + Destruction % + Timer + End Battle)
            val raidBanner = RectF(curW * 0.5f - 240f * u, 62f * u, curW * 0.5f + 240f * u, 114f * u)
            drawGlossyPanel(c, raidBanner, Color.argb(240, 38, 18, 24), Color.parseColor("#FF5252"))
            val st = raidStars()
            val starIcons = (if (st >= 1) "⭐" else "☆") + (if (st >= 2) "⭐" else "☆") + (if (st >= 3) "⭐" else "☆")
            drawShadowText(c, "$starIcons  ${destructionPct()}%  •  $raidEnemyTitle (${raidTimer.toInt()}ث)", raidBanner.centerX(), raidBanner.centerY() + 6f * u, 16.5f * u, Color.parseColor("#FFD54F"))

            val endBtn = RectF(18f * u, 118f * u, 178f * u, 166f * u)
            drawGlossyButton(c, endBtn, if (raidFinished) "العودة للقلعة 🏰" else "إنهاء الغارة 🏳️", Color.parseColor("#EF5350"), Color.parseColor("#B71C1C"), 14.5f)
            clickZones.add(ClickZone(endBtn, "raid_exit"))
        }

        // Bottom-Left Big Clash of Clans "Battle ⚔️" Button + Clan & Online Buttons
        if (mode == 0) {
            val battleBtn = RectF(18f * u, curH - 122f * u, 152f * u, curH - 14f * u)
            drawGlossyButton(c, battleBtn, "⚔️
هجوم!", Color.parseColor("#FFB74D"), Color.parseColor("#E65100"), 19f)
            clickZones.add(ClickZone(battleBtn, "btn_battle"))

            val clanBtn = RectF(18f * u, curH - 186f * u, 152f * u, curH - 130f * u)
            drawGlossyButton(c, clanBtn, "🛡️ الكلان والحرب", Color.parseColor("#EF5350"), Color.parseColor("#B71C1C"), 13.5f)
            clickZones.add(ClickZone(clanBtn, "hud_clans"))

            val onlineBtn = RectF(18f * u, curH - 246f * u, 152f * u, curH - 192f * u)
            drawGlossyButton(c, onlineBtn, "🌐 الأونلاين", Color.parseColor("#26A69A"), Color.parseColor("#00695C"), 13.5f)
            clickZones.add(ClickZone(onlineBtn, "hud_online"))

            // Bottom-Right Big Clash of Clans "Shop 🔨" Button + Market & Harvest
            val shopBtn = RectF(curW - 152f * u, curH - 122f * u, curW - 18f * u, curH - 14f * u)
            drawGlossyButton(c, shopBtn, "🔨
المتجر", Color.parseColor("#9CCC65"), Color.parseColor("#33691E"), 19f)
            clickZones.add(ClickZone(shopBtn, "btn_shop"))

            val marketBtn = RectF(curW - 152f * u, curH - 186f * u, curW - 18f * u, curH - 130f * u)
            drawGlossyButton(c, marketBtn, "🚚 السوق والشاحنة", Color.parseColor("#AB47BC"), Color.parseColor("#4A148C"), 13f)
            clickZones.add(ClickZone(marketBtn, "hud_market"))

            val harvBtn = RectF(curW - 152f * u, curH - 246f * u, curW - 18f * u, curH - 192f * u)
            drawGlossyButton(c, harvBtn, "🌾 حصاد الكل", Color.parseColor("#FFA726"), Color.parseColor("#E65100"), 13.5f)
            clickZones.add(ClickZone(harvBtn, "hud_harvest_all"))
        }

        // Bottom-Center Dock (Matches the exact troop & building card bar in our preview image!)
        val dockW = curW - 348f * u
        val dockR = RectF(174f * u, curH - 104f * u, 174f * u + dockW, curH - 12f * u)
        drawGlossyPanel(c, dockR, Color.argb(230, 24, 32, 46), Color.parseColor("#90CAF9"))

        if (mode == 0 && selectedBuilding != null) {
            val sb = selectedBuilding!!
            val upG = sb.type.costGold * sb.level
            val upE = sb.type.costElixir * sb.level
            Assets.bmp(sb.type.spriteForLevel(sb.level))?.let {
                c.drawBitmap(it, null, RectF(dockR.right - 88f * u, dockR.top + 8f * u, dockR.right - 14f * u, dockR.bottom - 8f * u), null)
            }
            drawShadowText(c, "${sb.type.titleAr} (مستوى ${sb.level})", dockR.right - 98f * u, dockR.top + 32f * u, 17f * u, Color.parseColor("#FFD54F"), Paint.Align.RIGHT)
            drawShadowText(c, sb.type.descAr, dockR.right - 98f * u, dockR.top + 58f * u, 12.5f * u, Color.WHITE, Paint.Align.RIGHT)

            val bUpg = RectF(dockR.left + 12f * u, dockR.top + 14f * u, dockR.left + 215f * u, dockR.bottom - 14f * u)
            drawGlossyButton(c, bUpg, "ترقية (${upG}🪙 / ${upE}🧪) ⬆️", Color.parseColor("#66BB6A"), Color.parseColor("#1B5E20"), 13.5f)
            clickZones.add(ClickZone(bUpg, "bld_upgrade"))

            val bMove = RectF(dockR.left + 225f * u, dockR.top + 14f * u, dockR.left + 375f * u, dockR.bottom - 14f * u)
            drawGlossyButton(c, bMove, if (movingBuilding) "اضغط مكاناً للنقل" else "نقل المبنى ✥", Color.parseColor("#29B6F6"), Color.parseColor("#01579B"), 13.5f)
            clickZones.add(ClickZone(bMove, "bld_move"))

            val bClose = RectF(dockR.left + 385f * u, dockR.top + 14f * u, dockR.left + 485f * u, dockR.bottom - 14f * u)
            drawGlossyButton(c, bClose, "إغلاق ✕", Color.parseColor("#78909C"), Color.parseColor("#37474F"), 13.5f)
            clickZones.add(ClickZone(bClose, "bld_deselect"))
        } else if (mode == 0) {
            // 6 Quick Showcase Cards in Bottom Dock (Barbarian, Archer, Giant, Golem, Train Army, Quick Build)
            val cards = listOf(
                Triple("dock_army", "iso/troop_barbarian.png", "بربري x${army[0]}"),
                Triple("dock_army", "iso/troop_archer.png", "رامية x${army[1]}"),
                Triple("dock_army", "iso/troop_giant.png", "عملاق x${army[2]}"),
                Triple("dock_army", "iso/troop_golem.png", "غولم x${army[5]}"),
                Triple("dock_army", "iso/iso_barracks.png", "تدريب الجيش ⚔️"),
                Triple("btn_shop", "iso/iso_townhall.png", "بناء القرية 🏰")
            )
            val cw = (dockR.width() - 24f * u - (cards.size - 1) * 8f * u) / cards.size
            for ((i, cd) in cards.withIndex()) {
                val x0 = dockR.left + 12f * u + i * (cw + 8f * u)
                val cr = RectF(x0, dockR.top + 8f * u, x0 + cw, dockR.bottom - 8f * u)
                drawGlossyPanel(c, cr, Color.argb(235, 38, 54, 78), Color.parseColor("#64B5F6"))
                Assets.bmp(cd.second)?.let {
                    c.drawBitmap(it, null, RectF(cr.centerX() - 26f * u, cr.top + 4f * u, cr.centerX() + 26f * u, cr.bottom - 22f * u), null)
                }
                drawShadowText(c, cd.third, cr.centerX(), cr.bottom - 6f * u, 11.5f * u, Color.WHITE)
                clickZones.add(ClickZone(cr, cd.first))
            }
        } else {
            // Mode 1: Raid Deployment Cards (6 Troops + 3 Spells + 1 Clan Castle Reinforcements)
            val slots = 10
            val cw = (dockR.width() - 20f * u - (slots - 1) * 5f * u) / slots
            for (si in 0 until slots) {
                val x0 = dockR.left + 10f * u + si * (cw + 5f * u)
                val cr = RectF(x0, dockR.top + 8f * u, x0 + cw, dockR.bottom - 8f * u)
                val sel = deploySlot == si
                drawGlossyPanel(c, cr, if (sel) Color.rgb(55, 95, 145) else Color.rgb(30, 40, 56), if (sel) Color.parseColor("#FFD54F") else Color.parseColor("#90A4AE"))
                if (si < 6) {
                    val tk = TroopKind.values()[si]
                    Assets.bmp(tk.sprite)?.let {
                        c.drawBitmap(it, null, RectF(cr.centerX() - 22f * u, cr.top + 4f * u, cr.centerX() + 22f * u, cr.bottom - 22f * u), null)
                    }
                    drawShadowText(c, "x${army[si]}", cr.centerX(), cr.bottom - 6f * u, 12f * u, if (army[si] > 0) Color.parseColor("#FFD54F") else Color.RED)
                } else if (si < 9) {
                    val lbl = when (si) { 6 -> "💚x$healSpells"; 7 -> "🔥x$rageSpells"; else -> "⚡x$lightningSpells" }
                    val name = when (si) { 6 -> "شفاء"; 7 -> "غضب"; else -> "برق" }
                    drawShadowText(c, name, cr.centerX(), cr.centerY() - 4f * u, 12f * u, Color.parseColor("#80DEEA"))
                    drawShadowText(c, lbl, cr.centerX(), cr.bottom - 8f * u, 12f * u, Color.WHITE)
                } else {
                    val ready = clanReinforcementsReady && !clanReinforcementsUsed
                    Assets.bmp("iso/iso_clancastle.png")?.let {
                        c.drawBitmap(it, null, RectF(cr.centerX() - 20f * u, cr.top + 4f * u, cr.centerX() + 20f * u, cr.bottom - 22f * u), null)
                    }
                    drawShadowText(c, if (ready) "دعم 🛡️" else "تم", cr.centerX(), cr.bottom - 6f * u, 11f * u, if (ready) Color.parseColor("#69F0AE") else Color.GRAY)
                }
                clickZones.add(ClickZone(cr, "dep_slot:$si"))
            }
        }

        // Victory / Raid End Overlay
        if (mode == 1 && raidFinished) {
            val box = RectF(curW * 0.5f - 260f * u, curH * 0.5f - 140f * u, curW * 0.5f + 260f * u, curH * 0.5f + 140f * u)
            drawGlossyPanel(c, box, Color.argb(248, 20, 28, 44), Color.parseColor("#FFD54F"))
            val st = raidStars()
            val title = if (st > 0) "انتصار باهر! ${"⭐".repeat(st)} (${destructionPct()}%)" else "انتهت المعركة (${destructionPct()}%)"
            drawShadowText(c, title, box.centerX(), box.top + 52f * u, 25f * u, Color.parseColor("#FFD54F"))
            drawShadowText(c, "الغنائم: +$lootGold 🪙   •   +$lootElixir 🧪   •   +${st * 5} 💎", box.centerX(), box.top + 102f * u, 16.5f * u, Color.parseColor("#69F0AE"))
            drawShadowText(c, "إجمالي الكؤوس: $trophies 🏆", box.centerX(), box.top + 138f * u, 16.5f * u, Color.WHITE)
            val bRet = RectF(box.centerX() - 140f * u, box.bottom - 68f * u, box.centerX() + 140f * u, box.bottom - 16f * u)
            drawGlossyButton(c, bRet, "العودة إلى قريتك 🏰", Color.parseColor("#66BB6A"), Color.parseColor("#1B5E20"), 16.5f)
            clickZones.add(ClickZone(bRet, "raid_exit"))
        }
    }

    // =========================================================================
    // DIALOG MODALS (Shop, Army & Bakery, Market, Clans & Clan Wars, Online, Gems)
    // =========================================================================
    private fun drawActiveDialog(c: Canvas) {
        pFill.color = Color.argb(175, 0, 0, 0)
        c.drawRect(0f, 0f, curW, curH, pFill)
        val mr = RectF(curW * 0.08f, curH * 0.08f, curW * 0.92f, curH * 0.90f)
        drawGlossyPanel(c, mr, Color.argb(248, 22, 32, 48), Color.parseColor("#FFD54F"))

        val closeBtn = RectF(mr.left + 16f * u, mr.top + 14f * u, mr.left + 68f * u, mr.top + 58f * u)
        drawGlossyButton(c, closeBtn, "✕", Color.parseColor("#EF5350"), Color.parseColor("#B71C1C"), 18f)
        clickZones.add(ClickZone(closeBtn, "dlg_close"))

        when (dialog) {
            1 -> { // 2.5D Buildings & Defenses Shop
                drawShadowText(c, "متجر بناء القرية والمزرعة والدفاعات (2.5D) 🏗️", mr.right - 24f * u, mr.top + 44f * u, 22f * u, Color.parseColor("#FFD54F"), Paint.Align.RIGHT)
                val list = BType.values().filter { it != BType.TOWN_HALL }
                val cols = 4
                val cw = (mr.width() - 48f * u) / cols
                val ch = 148f * u
                for ((i, bt) in list.withIndex()) {
                    val col = i % cols
                    val row = i / cols
                    val x0 = mr.left + 16f * u + col * (cw + 5f * u)
                    val y0 = mr.top + 66f * u + row * (ch + 8f * u)
                    if (y0 + ch > mr.bottom - 10f * u) continue
                    val card = RectF(x0, y0, x0 + cw, y0 + ch)
                    drawGlossyPanel(c, card, Color.argb(230, 32, 46, 68), if (bt.isDefense) Color.parseColor("#FF7043") else Color.parseColor("#66BB6A"))
                    Assets.bmp(bt.sprite)?.let {
                        c.drawBitmap(it, null, RectF(card.centerX() - 34f * u, card.top + 6f * u, card.centerX() + 34f * u, card.top + 68f * u), null)
                    }
                    drawShadowText(c, bt.titleAr, card.centerX(), card.top + 86f * u, 12.5f * u, Color.parseColor("#FFD54F"))
                    drawShadowText(c, "${bt.costGold}🪙  ${bt.costElixir}🧪", card.centerX(), card.top + 104f * u, 11.5f * u, Color.WHITE)
                    val canBuy = gold >= bt.costGold && elixir >= bt.costElixir
                    val bBtn = RectF(card.left + 10f * u, card.bottom - 36f * u, card.right - 10f * u, card.bottom - 6f * u)
                    drawGlossyButton(c, bBtn, "بناء 🔨", if (canBuy) Color.parseColor("#66BB6A") else Color.GRAY, if (canBuy) Color.parseColor("#1B5E20") else Color.DKGRAY, 12.5f)
                    if (canBuy) clickZones.add(ClickZone(bBtn, "buy_bld:${bt.code}"))
                }
            }
            2 -> { // Army Training Barracks & Windmill Bakery
                drawShadowText(c, "معسكر تدريب الجيوش ومخبز الطاحونة ⚔️🥖", mr.right - 24f * u, mr.top + 44f * u, 22f * u, Color.parseColor("#FFD54F"), Paint.Align.RIGHT)
                val bakeR = RectF(mr.left + 18f * u, mr.top + 62f * u, mr.centerX() - 8f * u, mr.top + 106f * u)
                drawGlossyButton(c, bakeR, "خبز +6 مؤن جيش (2 قمح + 1 صوف/حليب) 🥖", Color.parseColor("#FFA726"), Color.parseColor("#E65100"), 13.5f)
                clickZones.add(ClickZone(bakeR, "act_bake"))

                val spellR = RectF(mr.centerX() + 8f * u, mr.top + 62f * u, mr.right - 18f * u, mr.top + 106f * u)
                drawGlossyButton(c, spellR, "تحضير التعويذات (+1 شفاء/غضب/برق بـ 200🧪) ⚡", Color.parseColor("#AB47BC"), Color.parseColor("#4A148C"), 13.5f)
                clickZones.add(ClickZone(spellR, "act_spells"))

                val cols = 3
                val cw = (mr.width() - 48f * u) / cols
                val ch = 175f * u
                for ((i, tk) in TroopKind.values().withIndex()) {
                    val col = i % cols
                    val row = i / cols
                    val x0 = mr.left + 18f * u + col * (cw + 6f * u)
                    val y0 = mr.top + 118f * u + row * (ch + 10f * u)
                    val card = RectF(x0, y0, x0 + cw, y0 + ch)
                    drawGlossyPanel(c, card, Color.argb(235, 30, 42, 62), tk.accent)
                    Assets.bmp(tk.sprite)?.let {
                        c.drawBitmap(it, null, RectF(card.right - 84f * u, card.top + 10f * u, card.right - 10f * u, card.top + 84f * u), null)
                    }
                    drawShadowText(c, "${tk.nameAr} (معك: ${army[i]})", card.right - 90f * u, card.top + 32f * u, 14.5f * u, Color.parseColor("#FFD54F"), Paint.Align.RIGHT)
                    drawShadowText(c, tk.roleAr, card.right - 90f * u, card.top + 56f * u, 11.5f * u, Color.WHITE, Paint.Align.RIGHT)
                    drawShadowText(c, "صحة:${tk.maxHp} • ضرر:${tk.dmg} • تكلفة:${tk.costElixir}🧪 + ${tk.costBread}🥖", card.centerX(), card.top + 114f * u, 11.5f * u, Color.parseColor("#A5D6A7"))
                    val ok = elixir >= tk.costElixir && bread >= tk.costBread
                    val tBtn = RectF(card.left + 12f * u, card.bottom - 44f * u, card.right - 12f * u, card.bottom - 8f * u)
                    drawGlossyButton(c, tBtn, "تدريب +2 جندي ⚔️", if (ok) Color.parseColor("#29B6F6") else Color.GRAY, if (ok) Color.parseColor("#01579B") else Color.DKGRAY, 14f)
                    if (ok) clickZones.add(ClickZone(tBtn, "train_tr:$i"))
                }
            }
            3 -> { // Hay Day Truck Orders & Global Market
                drawShadowText(c, "شاحنة طلبات المزرعة وكشك السوق العالمي 🚚🛒", mr.right - 24f * u, mr.top + 44f * u, 22f * u, Color.parseColor("#FFD54F"), Paint.Align.RIGHT)
                val mid = mr.centerX()
                for ((idx, ord) in truckOrders.withIndex()) {
                    val top = mr.top + 70f * u + idx * 115f * u
                    val card = RectF(mid + 8f * u, top, mr.right - 18f * u, top + 102f * u)
                    drawGlossyPanel(c, card, Color.argb(230, 34, 46, 66), Color.parseColor("#FFB300"))
                    drawShadowText(c, ord.title, card.right - 14f * u, card.top + 28f * u, 15.5f * u, Color.parseColor("#FFD54F"), Paint.Align.RIGHT)
                    drawShadowText(c, "المطلوب: ${ord.wheat}🌾  •  ${ord.wool}🐑  •  ${ord.bread}🥖", card.right - 14f * u, card.top + 54f * u, 13f * u, Color.WHITE, Paint.Align.RIGHT)
                    drawShadowText(c, "المكافأة: +${ord.goldReward}🪙  +${ord.elixirReward}🧪", card.right - 14f * u, card.top + 80f * u, 13.5f * u, Color.parseColor("#69F0AE"), Paint.Align.RIGHT)
                    val ok = wheat >= ord.wheat && wool >= ord.wool && bread >= ord.bread
                    val bShip = RectF(card.left + 12f * u, card.centerY() - 22f * u, card.left + 135f * u, card.centerY() + 22f * u)
                    drawGlossyButton(c, bShip, "شحن الطلب 🚚", if (ok) Color.parseColor("#66BB6A") else Color.GRAY, if (ok) Color.parseColor("#1B5E20") else Color.DKGRAY, 13.5f)
                    if (ok) clickZones.add(ClickZone(bShip, "ship_ord:$idx"))
                }

                val sellBtn = RectF(mr.left + 18f * u, mr.top + 70f * u, mid - 8f * u, mr.top + 116f * u)
                drawGlossyButton(c, sellBtn, "بيع 5 قمح في السوق العالمي (+800🪙) 🌾", Color.parseColor("#FFA726"), Color.parseColor("#E65100"), 14f)
                clickZones.add(ClickZone(sellBtn, "sell_wheat"))

                for ((idx, mk) in marketItems.withIndex()) {
                    val top = mr.top + 128f * u + idx * 72f * u
                    val card = RectF(mr.left + 18f * u, top, mid - 8f * u, top + 64f * u)
                    drawGlossyPanel(c, card, Color.argb(230, 30, 42, 60), Color.parseColor("#29B6F6"))
                    drawShadowText(c, "${mk.label} (${mk.seller})", card.right - 14f * u, card.top + 26f * u, 14f * u, Color.parseColor("#FFD54F"), Paint.Align.RIGHT)
                    drawShadowText(c, "السعر: ${mk.price} 🪙", card.right - 14f * u, card.top + 48f * u, 12.5f * u, Color.parseColor("#A5D6A7"), Paint.Align.RIGHT)
                    val bBuy = RectF(card.left + 10f * u, card.top + 10f * u, card.left + 115f * u, card.bottom - 10f * u)
                    drawGlossyButton(c, bBuy, "شراء 🛒", Color.parseColor("#29B6F6"), Color.parseColor("#01579B"), 13f)
                    clickZones.add(ClickZone(bBuy, "buy_mk:$idx"))
                }
            }
            4 -> { // Clans, Reinforcements & 5v5 Clan Wars
                drawShadowText(c, "قلعة الكلان وحروب الكلانات (5 ضد 5) 🛡️⚔️🔥", mr.right - 24f * u, mr.top + 44f * u, 22f * u, Color.parseColor("#FFD54F"), Paint.Align.RIGHT)
                drawShadowText(c, "كلانك: $clanName  •  النتيجة: ⭐ $myWarStars - $enemyWarStars ⭐", mr.left + 84f * u, mr.top + 44f * u, 15f * u, Color.parseColor("#69F0AE"), Paint.Align.LEFT)
                val mid = mr.centerX()

                for (wi in 0 until 5) {
                    val top = mr.top + 68f * u + wi * 62f * u
                    val card = RectF(mid + 8f * u, top, mr.right - 18f * u, top + 54f * u)
                    drawGlossyPanel(c, card, Color.argb(230, 46, 26, 34), Color.parseColor("#FF5252"))
                    val st = warStarsPerBase[wi]
                    val stTxt = (if (st >= 1) "⭐" else "☆") + (if (st >= 2) "⭐" else "☆") + (if (st >= 3) "⭐" else "☆")
                    drawShadowText(c, "القلعة الحربية #${wi + 1} (Lv.${wi + 3})  $stTxt", card.right - 14f * u, card.centerY() + 6f * u, 14.5f * u, Color.WHITE, Paint.Align.RIGHT)
                    val atk = RectF(card.left + 10f * u, card.top + 8f * u, card.left + 135f * u, card.bottom - 8f * u)
                    drawGlossyButton(c, atk, "هجوم الحرب ⚔️", Color.parseColor("#FF7043"), Color.parseColor("#BF360C"), 13f)
                    clickZones.add(ClickZone(atk, "war_atk:$wi"))
                }
                val chestBtn = RectF(mid + 8f * u, mr.bottom - 62f * u, mr.right - 18f * u, mr.bottom - 14f * u)
                drawGlossyButton(c, chestBtn, if (warChestClaimed) "تم استلام صندوق الحرب ✓" else "استلام صندوق الحرب (+5000🪙 +3000🧪 +50💎) 🏆", Color.parseColor("#66BB6A"), Color.parseColor("#1B5E20"), 13f)
                clickZones.add(ClickZone(chestBtn, "war_claim"))

                val reqBtn = RectF(mr.left + 18f * u, mr.top + 68f * u, mid - 8f * u, mr.top + 114f * u)
                drawGlossyButton(c, reqBtn, "طلب دعم جنود الكلان (2 عملاق + 1 غولم) 🛡️", Color.parseColor("#26A69A"), Color.parseColor("#00695C"), 13.5f)
                clickZones.add(ClickZone(reqBtn, "clan_req"))

                val donBtn = RectF(mr.left + 18f * u, mr.top + 122f * u, mid - 8f * u, mr.top + 168f * u)
                drawGlossyButton(c, donBtn, "تبرع بجنود لأعضاء الكلان (+15 جوهرة 💎) 🎁", Color.parseColor("#29B6F6"), Color.parseColor("#01579B"), 13.5f)
                clickZones.add(ClickZone(donBtn, "clan_don"))

                val chatPanel = RectF(mr.left + 18f * u, mr.top + 178f * u, mid - 8f * u, mr.bottom - 14f * u)
                drawGlossyPanel(c, chatPanel, Color.argb(230, 16, 22, 34), Color.parseColor("#64B5F6"))
                var cy = chatPanel.top + 32f * u
                for (m in clanChat.take(5)) {
                    drawShadowText(c, m, chatPanel.right - 14f * u, cy, 12.5f * u, Color.WHITE, Paint.Align.RIGHT)
                    cy += 32f * u
                }
            }
            5 -> { // Online Cloud (apk3rade.mangoad.online) & Wi-Fi Hotspot Hub
                drawShadowText(c, "مركز الأونلاين المجاني (سحابة apk3rade + واي فاي ونقطة اتصال) 🌐", mr.right - 24f * u, mr.top + 44f * u, 21f * u, Color.parseColor("#FFD54F"), Paint.Align.RIGHT)
                drawShadowText(c, cloudStatus, mr.centerX(), mr.top + 78f * u, 14.5f * u, Color.parseColor("#69F0AE"))

                val bUp = RectF(mr.left + 24f * u, mr.top + 96f * u, mr.centerX() - 10f * u, mr.top + 154f * u)
                val bMatch = RectF(mr.centerX() + 10f * u, mr.top + 96f * u, mr.right - 24f * u, mr.top + 154f * u)
                drawGlossyButton(c, bUp, "1. رفع قريتك على سحابة موقعك ☁️⬆️", Color.parseColor("#29B6F6"), Color.parseColor("#01579B"), 15f)
                drawGlossyButton(c, bMatch, "2. هجوم أونلاين عبر السحابة 🌍⚔️", Color.parseColor("#FF7043"), Color.parseColor("#BF360C"), 15f)
                clickZones.add(ClickZone(bUp, "cloud_up"))
                clickZones.add(ClickZone(bMatch, "cloud_match"))

                val bHost = RectF(mr.left + 24f * u, mr.top + 170f * u, mr.centerX() - 10f * u, mr.top + 228f * u)
                val bJoin = RectF(mr.centerX() + 10f * u, mr.top + 170f * u, mr.right - 24f * u, mr.top + 228f * u)
                drawGlossyButton(c, bHost, "3. استضافة غرفة واي فاي / Hotspot 📡", Color.parseColor("#66BB6A"), Color.parseColor("#1B5E20"), 14.5f)
                drawGlossyButton(c, bJoin, "4. اتصال بصديق الواي فاي والهجوم عليه 🤝", Color.parseColor("#AB47BC"), Color.parseColor("#4A148C"), 14.5f)
                clickZones.add(ClickZone(bHost, "wifi_host"))
                clickZones.add(ClickZone(bJoin, "wifi_join"))

                val infoR = RectF(mr.left + 24f * u, mr.top + 246f * u, mr.right - 24f * u, mr.bottom - 20f * u)
                drawGlossyPanel(c, infoR, Color.argb(225, 16, 24, 36), Color.parseColor("#FFD54F"))
                drawShadowText(c, "💡 كود جهازك على الشبكة: (${LanMultiplayer.getRoomCode()})  •  IP: ${LanMultiplayer.getLocalIpAddress()}", infoR.centerX(), infoR.top + 38f * u, 15.5f * u, Color.parseColor("#FFD54F"))
                drawShadowText(c, "• السحابة مربوطة تلقائياً بنطاقك apk3rade.mangoad.online/server_api.php مع فك تشفير InfinityFree وCloudflare!", infoR.centerX(), infoR.top + 74f * u, 13.5f * u, Color.WHITE)
                drawShadowText(c, "• أو افتح نقطة اتصال (Hotspot) مع صديقك لتهجما على قرى بعضكما مباشرة بدون إنترنت!", infoR.centerX(), infoR.top + 106f * u, 13.5f * u, Color.parseColor("#80DEEA"))
            }
            6 -> { // Royal Gems & Builders Shop
                drawShadowText(c, "المتجر الملكي للجواهر والعمال والحزم 💎👷", mr.right - 24f * u, mr.top + 44f * u, 22f * u, Color.parseColor("#FFD54F"), Paint.Align.RIGHT)
                val items = listOf(
                    Triple("gem_builder", "توظيف عامل بناء إضافي ($builders/5) 👷", "يرفع سرعة إنتاج كل مزارعك ومناجمك بنسبة +25% دائماً! (50💎)"),
                    Triple("gem_resources", "خزينة الإمبراطور (+20,000🪙 و+10,000🧪)", "شحن فوري للذهب والإكسير السحري لتطوير قلعتك! (40💎)"),
                    Triple("gem_army", "تعبئة معسكر الجيش بالكامل ⚔️🔥", "يضيف +5 من جميع الجنود (العمالقة والغولم والسحرة) والتعويذات! (45💎)"),
                    Triple("gem_free", "هدية الصندوق الملكي المجاني (+50 جوهرة 💎)", "احصل على جواهر خضراء مجانية لدعم مملكتك! (مجاناً 🎁)")
                )
                for ((i, it) in items.withIndex()) {
                    val top = mr.top + 74f * u + i * 96f * u
                    val card = RectF(mr.left + 22f * u, top, mr.right - 22f * u, top + 84f * u)
                    drawGlossyPanel(c, card, Color.argb(230, 26, 40, 58), Color.parseColor("#00E676"))
                    drawShadowText(c, it.second, card.right - 18f * u, card.top + 32f * u, 16f * u, Color.parseColor("#FFD54F"), Paint.Align.RIGHT)
                    drawShadowText(c, it.third, card.right - 18f * u, card.top + 60f * u, 13f * u, Color.WHITE, Paint.Align.RIGHT)
                    val bAct = RectF(card.left + 14f * u, card.top + 14f * u, card.left + 170f * u, card.bottom - 14f * u)
                    drawGlossyButton(c, bAct, "تفعيل الآن 💎", Color.parseColor("#00E676"), Color.parseColor("#00695C"), 14.5f)
                    clickZones.add(ClickZone(bAct, it.first))
                }
            }
        }
    }

    // =========================================================================
    // TOUCH INPUT & CLOUD NETWORKING
    // =========================================================================
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return true
        val x = event.x
        val y = event.y

        for (i in clickZones.size - 1 downTo 0) {
            val cz = clickZones[i]
            if (cz.rect.contains(x, y)) {
                handleButtonTap(cz.id)
                return true
            }
        }

        if (dialog != 0) return true

        // Tap on the 2.5D Isometric Diamond Island!
        val (fxRaw, fyRaw) = screenToIso(x, y)
        if (fxRaw in -0.5f..(GRID_N + 0.5f) && fyRaw in -0.5f..(GRID_N + 0.5f)) {
            val gx = fxRaw.toInt().coerceIn(0, GRID_N - 1)
            val gy = fyRaw.toInt().coerceIn(0, GRID_N - 1)

            if (mode == 0) {
                val existing = myVillage.firstOrNull { it.gx == gx && it.gy == gy }
                val pb = pendingBuild
                if (pb != null) {
                    if (existing == null && gold >= pb.costGold && elixir >= pb.costElixir) {
                        gold -= pb.costGold
                        elixir -= pb.costElixir
                        val nb = Building(gx, gy, pb, 1)
                        myVillage.add(nb)
                        selectedBuilding = nb
                        pendingBuild = null
                        Sound.play("chop")
                        showToast("بنيت ${pb.titleAr} في قريتك! 🏰")
                        saveKingdom()
                    } else {
                        showToast("اختر مربعاً عشبياً فارغاً للبناء!")
                        Sound.play("deny")
                    }
                    return true
                }
                if (movingBuilding && selectedBuilding != null) {
                    if (existing == null) {
                        selectedBuilding!!.gx = gx
                        selectedBuilding!!.gy = gy
                        movingBuilding = false
                        Sound.play("chop")
                        showToast("تم نقل المبنى بنجاح! ✅")
                        saveKingdom()
                    } else {
                        showToast("هذا الموضع مشغول بمبنى آخر!")
                    }
                    return true
                }
                if (existing != null) {
                    selectedBuilding = existing
                    collectSingleBuilding(existing)
                    Sound.play("click")
                } else {
                    selectedBuilding = null
                    movingBuilding = false
                }
            } else if (!raidFinished) {
                val fx = fxRaw.coerceIn(0.1f, GRID_N - 0.1f)
                val fy = fyRaw.coerceIn(0.1f, GRID_N - 0.1f)
                deployAt(fx, fy)
            }
        }
        return true
    }

    private fun deployAt(fx: Float, fy: Float) {
        when (deploySlot) {
            in 0..5 -> if (army[deploySlot] > 0) {
                army[deploySlot]--
                troops.add(Troop(fx, fy, TroopKind.values()[deploySlot]))
                Sound.play("dash", 0.7f)
            } else {
                showToast("نفدت هذه الوحدة! اختر وحدة أخرى من الشريط السفلي")
                Sound.play("deny")
            }
            6 -> if (healSpells > 0) {
                healSpells--
                for (tr in troops) if (!tr.isDead && hypot((tr.gx - fx).toDouble(), (tr.gy - fy).toDouble()) <= 3.2) {
                    tr.hp = min(tr.maxHp, tr.hp + 240)
                }
                blasts.add(BlastFx(fx, fy, Color.parseColor("#00E676"), 68f))
                Sound.play("potion")
                showToast("ألقيت تعويذة الشفاء الملكية! 💚")
            }
            7 -> if (rageSpells > 0) {
                rageSpells--
                for (tr in troops) if (!tr.isDead && hypot((tr.gx - fx).toDouble(), (tr.gy - fy).toDouble()) <= 3.5) {
                    tr.rageTimer = 9f
                }
                blasts.add(BlastFx(fx, fy, Color.parseColor("#E040FB"), 68f))
                Sound.play("magic")
                showToast("ألقيت تعويذة الغضب (+60% سرعة وضرر للجيش)! 🔥")
            }
            8 -> if (lightningSpells > 0) {
                lightningSpells--
                for (b in raidVillage) if (!b.isDestroyed && hypot((b.gx + 0.5f - fx).toDouble(), (b.gy + 0.5f - fy).toDouble()) <= 2.2) {
                    damageRaidBuilding(b, 440)
                }
                blasts.add(BlastFx(fx, fy, Color.parseColor("#00E5FF"), 74f))
                Sound.play("explode")
                showToast("ضربت دفاعات الخصم بصاعقة البرق! ⚡")
            }
            9 -> if (clanReinforcementsReady && !clanReinforcementsUsed) {
                clanReinforcementsUsed = true
                troops.add(Troop(fx - 0.35f, fy, TroopKind.GIANT))
                troops.add(Troop(fx + 0.35f, fy, TroopKind.GIANT))
                troops.add(Troop(fx, fy - 0.35f, TroopKind.GOLEM))
                troops.add(Troop(fx, fy + 0.35f, TroopKind.WIZARD))
                blasts.add(BlastFx(fx, fy, Color.parseColor("#FFD54F"), 64f))
                Sound.play("boss")
                showToast("أنزلت جيش دعم الكلان (2 عملاق + 1 غولم صخري + 1 ساحر)! 🛡️🔥")
            }
        }
    }

    private fun collectSingleBuilding(b: Building): Boolean {
        when (b.type) {
            BType.WHEAT_FARM -> if (b.readyHarvest) {
                b.readyHarvest = false
                b.timer = 0f
                val got = 3 * b.level
                wheat += got
                floatLabels.add(FloatLabel("+$got 🌾", b.gx + 0.5f, b.gy.toFloat(), Color.parseColor("#FFD54F")))
                Sound.play("coin")
                saveKingdom()
                return true
            }
            BType.SHEEP_PASTURE -> if (b.stored > 0) {
                val got = b.stored
                b.stored = 0
                wool += got
                floatLabels.add(FloatLabel("+$got 🐑", b.gx + 0.5f, b.gy.toFloat(), Color.WHITE))
                Sound.play("coin")
                saveKingdom()
                return true
            }
            BType.GOLD_MINE -> if (b.stored > 0) {
                val got = b.stored
                b.stored = 0
                gold += got
                floatLabels.add(FloatLabel("+$got 🪙", b.gx + 0.5f, b.gy.toFloat(), Color.parseColor("#FFD54F")))
                Sound.play("coin")
                saveKingdom()
                return true
            }
            BType.ELIXIR_COLLECTOR -> if (b.stored > 0) {
                val got = b.stored
                b.stored = 0
                elixir += got
                floatLabels.add(FloatLabel("+$got 🧪", b.gx + 0.5f, b.gy.toFloat(), Color.parseColor("#EA80FC")))
                Sound.play("coin")
                saveKingdom()
                return true
            }
            else -> {}
        }
        return false
    }

    private fun handleButtonTap(id: String) {
        when {
            id == "dlg_close" -> { dialog = 0; Sound.play("click") }
            id == "btn_battle" -> { Sound.play("boss"); startMatchmakingRaid(0); showToast("المس حول القرية لإنزال جنودك وتعويذاتك وكسب 3 نجوم! ⭐⭐⭐") }
            id == "btn_shop" -> { dialog = 1; Sound.play("click") }
            id == "dock_army" -> { dialog = 2; Sound.play("click") }
            id == "hud_market" -> { dialog = 3; Sound.play("click") }
            id == "hud_clans" -> { dialog = 4; Sound.play("click") }
            id == "hud_online" -> { dialog = 5; Sound.play("click") }
            id == "hud_gemshop" -> { dialog = 6; Sound.play("click") }
            id == "hud_harvest_all" -> {
                var cnt = 0
                for (b in myVillage) if (collectSingleBuilding(b)) cnt++
                showToast(if (cnt > 0) "حصدت وجمعت إنتاج $cnt من المزارع والمناجم! 🌾🪙" else "المحاصيل والمناجم قيد الإنتاج حالياً 🌱")
            }
            id == "raid_exit" -> {
                if (!raidFinished) endRaidBattle() else { mode = 0; Sound.play("click") }
            }
            id.startsWith("dep_slot:") -> {
                deploySlot = id.substring(9).toIntOrNull() ?: 0
                Sound.play("click")
            }
            id == "bld_upgrade" -> {
                val sb = selectedBuilding ?: return
                val cg = sb.type.costGold * sb.level
                val ce = sb.type.costElixir * sb.level
                if (gold >= cg && elixir >= ce && sb.level < 10) {
                    gold -= cg
                    elixir -= ce
                    sb.level++
                    sb.hp = sb.maxHp
                    Sound.play("era_clear")
                    showToast("طوّرت ${sb.type.titleAr} إلى المستوى ${sb.level}! ⬆️✨")
                    saveKingdom()
                } else {
                    showToast("الذهب أو الإكسير غير كافٍ للترقية!")
                    Sound.play("deny")
                }
            }
            id == "bld_move" -> {
                movingBuilding = !movingBuilding
                Sound.play("click")
                showToast("المس أي مكان فارغ على الجزيرة لنقل المبنى إليه ✥")
            }
            id == "bld_deselect" -> {
                selectedBuilding = null
                movingBuilding = false
                Sound.play("click")
            }
            id.startsWith("buy_bld:") -> {
                val c = id.substring(8).toIntOrNull() ?: return
                pendingBuild = BType.byCode(c)
                dialog = 0
                Sound.play("click")
                showToast("المس أي مكان أخضر على الجزيرة لبناء ${pendingBuild?.titleAr}! 🔨")
            }
            id == "act_bake" -> if (wheat >= 2 && wool >= 1) {
                wheat -= 2; wool -= 1; bread += 6
                Sound.play("buy"); showToast("خبزت +6 مؤن خبز ساخنة في الطاحونة! 🥖"); saveKingdom()
            }
            id == "act_spells" -> if (elixir >= 200) {
                elixir -= 200; healSpells++; rageSpells++; lightningSpells++
                Sound.play("magic"); showToast("حضرت +1 شفاء و+1 غضب و+1 صاعقة برق! ⚡"); saveKingdom()
            }
            id.startsWith("train_tr:") -> {
                val idx = id.substring(9).toIntOrNull() ?: return
                val tk = TroopKind.values().getOrNull(idx) ?: return
                if (elixir >= tk.costElixir && bread >= tk.costBread) {
                    elixir -= tk.costElixir; bread -= tk.costBread; army[idx] += 2
                    Sound.play("buy"); showToast("درّبت +2 ${tk.nameAr} في الثكنات! ⚔️"); saveKingdom()
                }
            }
            id.startsWith("ship_ord:") -> {
                val idx = id.substring(9).toIntOrNull() ?: return
                val ord = truckOrders.getOrNull(idx) ?: return
                if (wheat >= ord.wheat && wool >= ord.wool && bread >= ord.bread) {
                    wheat -= ord.wheat; wool -= ord.wool; bread -= ord.bread
                    gold += ord.goldReward; elixir += ord.elixirReward; gems += 5
                    Sound.play("victory")
                    showToast("شحنت طلبية الشاحنة وحصلت على +${ord.goldReward}🪙 و+${ord.elixirReward}🧪 و+5💎!")
                    saveKingdom()
                }
            }
            id == "sell_wheat" -> if (wheat >= 5) {
                wheat -= 5; gold += 800
                Sound.play("coin"); showToast("بعت 5 قمح في السوق العالمي مقابل +800 ذهب! 🪙"); saveKingdom()
            }
            id.startsWith("buy_mk:") -> {
                val idx = id.substring(7).toIntOrNull() ?: return
                val mk = marketItems.getOrNull(idx) ?: return
                if (gold >= mk.price) {
                    gold -= mk.price
                    when (mk.code) {
                        "WHEAT" -> wheat += mk.qty
                        "BREAD" -> bread += mk.qty
                        "WOOL" -> wool += mk.qty
                        "ELIXIR" -> elixir += mk.qty
                    }
                    Sound.play("buy"); showToast("اشتريت ${mk.label} من السوق العالمي! 🛒"); saveKingdom()
                }
            }
            id.startsWith("war_atk:") -> {
                val wi = id.substring(8).toIntOrNull() ?: return
                activeWarIdx = wi
                Sound.play("boss")
                startMatchmakingRaid(wi + 1)
                raidEnemyTitle = "حرب الكلانات: القلعة #${wi + 1} 🔥"
            }
            id == "war_claim" -> if (!warChestClaimed) {
                warChestClaimed = true
                gold += 5000; elixir += 3000; gems += 50
                Sound.play("victory"); showToast("استلمت صندوق انتصار حرب الكلانات (+5000🪙 +3000🧪 +50💎)! 🏆"); saveKingdom()
            }
            id == "clan_req" -> {
                clanReinforcementsReady = true; clanReinforcementsUsed = false
                Sound.play("era_clear"); showToast("وصل دعم الكلان (2 عملاق + 1 غولم صخري + 1 ساحر)! 🛡️")
            }
            id == "clan_don" -> if (army[0] >= 2) {
                army[0] -= 2; gems += 15
                clanChat.add(0, "أنت: تبرعت بـ 2 محارب لقلعة الكلان! 🛡️ (+15💎)")
                Sound.play("coin"); showToast("تبرعت للكلان وحصلت على +15 جوهرة خضراء 💎!"); saveKingdom()
            }
            id == "cloud_up" -> {
                Sound.play("era_clear")
                syncCloud(true, false)
                showToast("جاري رفع قريتك على سحابة apk3rade.mangoad.online ... ☁️")
            }
            id == "cloud_match" -> {
                Sound.play("boss")
                syncCloud(false, true)
            }
            id == "wifi_host" -> {
                Sound.play("era_clear")
                LanMultiplayer.startHosting("مملكة الفارس", false, { playerLevel }) {
                    LanMultiplayer.send("K25_RAID|الفارس|${encodeVillage(myVillage)}")
                    showToast("اتصل صديقك عبر الواي فاي وتم إرسال قريتك للتحدي! 🤝")
                }
                cloudStatus = "بانتظار صديقك على الواي فاي / نقطة الاتصال (كود: ${LanMultiplayer.getRoomCode()})"
            }
            id == "wifi_join" -> {
                Sound.play("click")
                LanMultiplayer.connectToHost("") {
                    LanMultiplayer.send("K25_RAID|الصديق|${encodeVillage(myVillage)}")
                    showToast("تم الاتصال بصديقك عبر الواي فاي / نقطة الاتصال! 🤝")
                }
            }
            id == "gem_builder" -> if (gems >= 50 && builders < 5) {
                gems -= 50; builders++; Sound.play("era_clear"); showToast("وظفت عامل بناء جديد ($builders/5)! زاد إنتاج قريتك +25%! 👷"); saveKingdom()
            }
            id == "gem_resources" -> if (gems >= 40) {
                gems -= 40; gold += 20000; elixir += 10000; Sound.play("coin"); showToast("شحنت +20,000🪙 و+10,000🧪! 💎"); saveKingdom()
            }
            id == "gem_army" -> if (gems >= 45) {
                gems -= 45; for (i in 0..5) army[i] += 5; healSpells += 2; rageSpells += 2
                Sound.play("boss"); showToast("تمت تعبئة معسكر الجيش بالكامل! ⚔️🔥"); saveKingdom()
            }
            id == "gem_free" -> {
                gems += 50; Sound.play("coin"); showToast("حصلت على +50 جوهرة خضراء مجانية! 💎🎁"); saveKingdom()
            }
        }
    }

    // =========================================================================
    // INFINITYFREE + CLOUDFLARE AUTOMATIC CLOUD CLIENT
    // =========================================================================
    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) out[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        return out
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) sb.append(String.format("%02x", b.toInt() and 0xff))
        return sb.toString()
    }

    private fun requestCloud(urlStr: String): String {
        val ua = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        fun get(u: String): String {
            val conn = (URL(u).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4500; readTimeout = 4500
                setRequestProperty("User-Agent", ua)
                if (infinityCookie.isNotEmpty()) setRequestProperty("Cookie", "__test=$infinityCookie")
            }
            val txt = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            return txt
        }
        val first = get(urlStr)
        if (first.contains("slowAES.decrypt") && first.contains("__test=")) {
            val rx = Regex("toNumbers\\(\"([0-9a-fA-F]{32})\"\\)")
            val m = rx.findAll(first).map { it.groupValues[1] }.toList()
            if (m.size >= 3) {
                val cipher = Cipher.getInstance("AES/CBC/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(hexToBytes(m[0]), "AES"), IvParameterSpec(hexToBytes(m[1])))
                infinityCookie = bytesToHex(cipher.doFinal(hexToBytes(m[2])))
                return get("$urlStr&i=1")
            }
        }
        return first
    }

    private fun syncCloud(upload: Boolean, findMatch: Boolean) {
        val myCode = encodeVillage(myVillage)
        Thread {
            try {
                if (upload) {
                    requestCloud("$cloudApiUrl?action=upload_village&player=${URLEncoder.encode("الفارس_مصطفى", "UTF-8")}&trophies=$trophies&th=4&code=${URLEncoder.encode(myCode, "UTF-8")}")
                    cloudStatus = "تم رفع قريتك بنجاح على سحابة apk3rade.mangoad.online! ☁️✅"
                }
                if (findMatch) {
                    val resp = requestCloud("$cloudApiUrl?action=find_match&player=${URLEncoder.encode("الفارس_مصطفى", "UTF-8")}")
                    val idx = resp.indexOf("K25:")
                    if (idx >= 0) {
                        val end = resp.indexOf('"', idx)
                        val remoteCode = if (end > idx) resp.substring(idx, end) else ""
                        if (decodeVillage(remoteCode, raidVillage)) {
                            beginRaid("قرية لاعب أونلاين سحابية 🌍", raidVillage)
                            return@Thread
                        }
                    }
                    startMatchmakingRaid(2)
                }
            } catch (_: Throwable) {
                cloudStatus = "تم تفعيل البحث العالمي الذكي!"
                if (findMatch) startMatchmakingRaid(2)
            }
        }.apply { isDaemon = true; start() }
    }

    // =========================================================================
    // SUPERCELL GLOSSY UI DRAWING HELPERS
    // =========================================================================
    private fun drawGlossyPanel(c: Canvas, r: RectF, bgCol: Int, borderCol: Int) {
        pFill.color = Color.argb(110, 0, 0, 0)
        c.drawRoundRect(RectF(r.left, r.top + 4f * u, r.right, r.bottom + 4f * u), 14f * u, 14f * u, pFill)
        pFill.color = bgCol
        c.drawRoundRect(r, 14f * u, 14f * u, pFill)
        // Top glossy highlight band
        pFill.color = Color.argb(36, 255, 255, 255)
        c.drawRoundRect(RectF(r.left + 3f * u, r.top + 3f * u, r.right - 3f * u, r.top + r.height() * 0.42f), 12f * u, 12f * u, pFill)
        pStroke.strokeWidth = 2.8f * u
        pStroke.color = borderCol
        c.drawRoundRect(r, 14f * u, 14f * u, pStroke)
    }

    private fun drawGlossyButton(c: Canvas, r: RectF, text: String, topCol: Int, botCol: Int, textSp: Float) {
        // 3D bottom bevel shadow
        pFill.color = Color.argb(140, 0, 0, 0)
        c.drawRoundRect(RectF(r.left, r.top + 4f * u, r.right, r.bottom + 5f * u), 14f * u, 14f * u, pFill)
        pFill.shader = LinearGradient(0f, r.top, 0f, r.bottom, topCol, botCol, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, 14f * u, 14f * u, pFill)
        pFill.shader = null
        // Glossy top shine
        pFill.color = Color.argb(70, 255, 255, 255)
        c.drawRoundRect(RectF(r.left + 4f * u, r.top + 3f * u, r.right - 4f * u, r.centerY()), 10f * u, 10f * u, pFill)
        pStroke.strokeWidth = 2.4f * u
        pStroke.color = Color.argb(210, 255, 245, 200)
        c.drawRoundRect(r, 14f * u, 14f * u, pStroke)

        if (text.isNotEmpty()) {
            val lines = text.split("
")
            if (lines.size == 1) {
                drawShadowText(c, text, r.centerX(), r.centerY() + textSp * 0.36f * u, textSp * u, Color.WHITE)
            } else {
                drawShadowText(c, lines[0], r.centerX(), r.centerY() - 4f * u, (textSp + 4f) * u, Color.WHITE)
                drawShadowText(c, lines[1], r.centerX(), r.centerY() + 22f * u, textSp * u, Color.WHITE)
            }
        }
    }

    private fun drawProgressBar(c: Canvas, r: RectF, frac: Float, fillCol: Int) {
        pFill.color = Color.argb(190, 12, 18, 28)
        c.drawRoundRect(r, r.height() * 0.5f, r.height() * 0.5f, pFill)
        val w = (r.width() * frac.coerceIn(0f, 1f))
        if (w > 2f) {
            pFill.color = fillCol
            c.drawRoundRect(RectF(r.left, r.top, r.left + w, r.bottom), r.height() * 0.5f, r.height() * 0.5f, pFill)
        }
        pStroke.strokeWidth = 1.5f * u
        pStroke.color = Color.WHITE
        c.drawRoundRect(r, r.height() * 0.5f, r.height() * 0.5f, pStroke)
    }

    private fun drawBadge(c: Canvas, cx: Float, cy: Float, txt: String, bgCol: Int, txtCol: Int) {
        val r = RectF(cx - 18f * u, cy - 10f * u, cx + 18f * u, cy + 10f * u)
        pFill.color = bgCol
        c.drawRoundRect(r, 6f * u, 6f * u, pFill)
        pStroke.strokeWidth = 1.5f * u
        pStroke.color = Color.BLACK
        c.drawRoundRect(r, 6f * u, 6f * u, pStroke)
        pText.textSize = 10.5f * u
        pText.textAlign = Paint.Align.CENTER
        pText.color = txtCol
        c.drawText(txt, cx, cy + 4f * u, pText)
    }

    private fun drawShadowText(c: Canvas, text: String, x: Float, y: Float, sizePx: Float, color: Int, align: Paint.Align = Paint.Align.CENTER) {
        pText.textSize = sizePx
        pText.textAlign = align
        pText.color = Color.argb(210, 0, 0, 0)
        c.drawText(text, x + 2f * u, y + 2.5f * u, pText)
        pText.color = color
        c.drawText(text, x, y, pText)
    }

    private fun showToast(msg: String) {
        toastMsg = msg
        toastTimer = 2.8f
    }

    // =========================================================================
    // SURFACEVIEW LIFECYCLE
    // =========================================================================
    override fun surfaceCreated(holder: SurfaceHolder) { resume() }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        curW = width.toFloat().coerceAtLeast(1f)
        curH = height.toFloat().coerceAtLeast(1f)
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) { pause() }

    fun resume() {
        if (running) return
        running = true
        thread = Thread(this, "Kingdom25DLoop").also { it.start() }
    }

    fun pause() {
        running = false
        try { thread?.join(400) } catch (_: Throwable) {}
        thread = null
        saveKingdom()
    }

    override fun run() {
        var lastNs = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            lastNs = now
            update(dt)
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    synchronized(holder) { render(canvas) }
                }
            } catch (_: Throwable) {
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: Throwable) {}
                }
            }
        }
    }
}
