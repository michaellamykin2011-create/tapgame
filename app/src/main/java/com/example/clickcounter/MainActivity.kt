package com.example.clickcounter

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout

class MainActivity : AppCompatActivity() {

    private var counter = 0L // Используем Long для больших чисел
    private var highScore = 0L

    // UI
    private lateinit var counterTextView: TextView
    private lateinit var highScoreTextView: TextView
    private lateinit var clickButton: Button
    private lateinit var resetButton: Button
    private lateinit var victoryTextView: TextView
    private lateinit var menuButton: ImageButton
    private lateinit var goldenBonus: ImageView
    private lateinit var shopButton: ImageButton
    private lateinit var comboLayout: LinearLayout
    private lateinit var comboTextView: TextView

    // Сервисы
    private lateinit var soundPool: SoundPool
    private var clickSoundId: Int = 0
    private val vibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    private val bonusHandler = Handler(Looper.getMainLooper())
    private val autoClickHandler = Handler(Looper.getMainLooper()) // Для авто-клика

    // Улучшения
    private var clickPower = 1
    private var isAutoClickerBought = false

    // Комбо
    private var comboMultiplier = 1
    private var lastClickTime: Long = 0
    private val comboTimestamps = mutableListOf<Long>()

    // Константы
    private val PREFS_NAME = "ClickCounterPrefs"
    private val WINNING_SCORE = 77777

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация UI
        // ... (все ваши findViewById)
        counterTextView = findViewById(R.id.counterTextView)
        highScoreTextView = findViewById(R.id.highScoreTextView)
        clickButton = findViewById(R.id.clickButton)
        resetButton = findViewById(R.id.resetButton)
        victoryTextView = findViewById(R.id.victoryTextView)
        menuButton = findViewById(R.id.menuButton)
        shopButton = findViewById(R.id.shopButton)
        goldenBonus = findViewById(R.id.goldenBonus)
        comboLayout = findViewById(R.id.comboLayout)
        comboTextView = findViewById(R.id.comboTextView)

        loadData()
        setupSound()
        startBonusCycle()
        val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce)

        if (isAutoClickerBought) {
            startAutoClicker()
        }
        if (counter >= WINNING_SCORE) {
            showVictoryState()
        }

        // --- ОБРАБОТЧИКИ НАЖАТИЙ ---

        clickButton.setOnClickListener {
            checkCombo() // Проверяем комбо
            val pointsToAdd = clickPower * comboMultiplier
            showFloatingText("+$pointsToAdd") // Показываем "+N"
            vibrator.vibrate(50)
            it.startAnimation(bounceAnimation)
            playSound()
            counter += pointsToAdd
            updateCounterText()
            checkHighScore()
            if (counter >= WINNING_SCORE) {
                showVictoryState()
            }
        }

        shopButton.setOnClickListener {
            showShopDialog()
        }

        // ... (остальные обработчики без изменений)
        resetButton.setOnClickListener { counter = 0; updateCounterText(); hideVictoryState(); saveData() }
        menuButton.setOnClickListener { showMenu(it) }
        goldenBonus.setOnClickListener { counter += 500; updateCounterText(); checkHighScore(); it.visibility = View.GONE; bonusHandler.removeCallbacksAndMessages(null); startBonusCycle() }
        var tapCount = 0
        var lastTapTime: Long = 0
        counterTextView.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime < 500) { tapCount++ } else { tapCount = 1 }
            lastTapTime = currentTime
            if (tapCount == 3) { tapCount = 0; counter += 1000; updateCounterText(); checkHighScore() }
        }
    }

    // ✨ ЛОГИКА КОМБО
    private fun checkCombo() {
        val currentTime = System.currentTimeMillis()
        comboTimestamps.add(currentTime)
        // Удаляем старые клики (старше 1 секунды)
        comboTimestamps.removeAll { currentTime - it > 1000 }

        if (comboTimestamps.size >= 5) { // 5 кликов в секунду для активации
            if (comboMultiplier == 1) { // Активируем комбо
                comboMultiplier = 2
                comboTextView.text = "Комбо x$comboMultiplier"
                comboLayout.visibility = View.VISIBLE
            }
        } else {
            if (comboMultiplier > 1) { // Деактивируем комбо
                comboMultiplier = 1
                comboLayout.visibility = View.GONE
            }
        }
    }

    // ✨ ЛОГИКА МАГАЗИНА
    private fun showShopDialog() {
        val shopItems = arrayOf(
            "Бабушкин свитер (Авто-клик +1/сек) - 500 очков",
            "Двойной эспрессо (Сила клика +1) - 1000 очков"
        )
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Магазин улучшений")
        builder.setItems(shopItems) { dialog, which ->
            when (which) {
                0 -> { // Авто-клик
                    if (!isAutoClickerBought && counter >= 500) {
                        counter -= 500
                        isAutoClickerBought = true
                        startAutoClicker()
                        Toast.makeText(this, "Авто-клик куплен!", Toast.LENGTH_SHORT).show()
                    } else if (isAutoClickerBought) {
                        Toast.makeText(this, "Уже куплено!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Недостаточно очков!", Toast.LENGTH_SHORT).show()
                    }
                }
                1 -> { // Сила клика
                    if (counter >= 1000) {
                        counter -= 1000
                        clickPower++
                        Toast.makeText(this, "Сила клика увеличена!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Недостаточно очков!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            updateCounterText() // Обновляем счет на экране
        }
        builder.setNegativeButton("Закрыть", null)
        builder.create().show()
    }

    private fun startAutoClicker() {
        autoClickHandler.post(object : Runnable {
            override fun run() {
                counter++
                updateCounterText()
                checkHighScore()
                autoClickHandler.postDelayed(this, 1000) // Запускаем снова через 1 секунду
            }
        })
    }

    // --- Остальные функции ---
    private fun showFloatingText(text: String) {
        val constraintLayout = findViewById<ConstraintLayout>(R.id.main)
        val floatingText = TextView(this).apply { this.text = text; setTextColor(android.graphics.Color.WHITE); textSize = 24f; x = clickButton.x + clickButton.width / 2; y = clickButton.y }
        constraintLayout.addView(floatingText)
        floatingText.animate().translationYBy(-200f).alpha(0f).setDuration(800).withEndAction { constraintLayout.removeView(floatingText) }.start()
    }
    private fun startBonusCycle() { bonusHandler.postDelayed({ val mainLayout = findViewById<View>(R.id.main); val randomX = (0..mainLayout.width - goldenBonus.width).random().toFloat(); val randomY = (0..mainLayout.height - goldenBonus.height).random().toFloat(); goldenBonus.x = randomX; goldenBonus.y = randomY; goldenBonus.visibility = View.VISIBLE; bonusHandler.postDelayed({ goldenBonus.visibility = View.GONE }, 2000) }, (5000..15000).random().toLong()) }
    private fun updateCounterText() { counterTextView.text = counter.toString() }
    private fun updateHighScoreText() { highScoreTextView.text = "Рекорд: $highScore" }
    private fun checkHighScore() { if (counter > highScore) { highScore = counter; updateHighScoreText() } }
    private fun showVictoryState() { victoryTextView.visibility = View.VISIBLE; clickButton.visibility = View.GONE; resetButton.visibility = View.VISIBLE; comboLayout.visibility = View.GONE }
    private fun hideVictoryState() { victoryTextView.visibility = View.GONE; clickButton.visibility = View.VISIBLE; resetButton.visibility = View.GONE }
    private fun setupSound() { val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(); soundPool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(audioAttributes).build(); clickSoundId = try { soundPool.load(this, R.raw.click_sound, 1) } catch (e: Exception) { 0 } }
    private fun playSound() { if (clickSoundId != 0) { soundPool.play(clickSoundId, 1.0f, 1.0f, 0, 0, 1.0f) } }
    private fun saveData() { val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit(); prefs.putLong("counter", counter); prefs.putLong("highScore", highScore); prefs.putInt("clickPower", clickPower); prefs.putBoolean("isAutoClickerBought", isAutoClickerBought); prefs.apply() }
    private fun loadData() { val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE); counter = prefs.getLong("counter", 0); highScore = prefs.getLong("highScore", 0); clickPower = prefs.getInt("clickPower", 1); isAutoClickerBought = prefs.getBoolean("isAutoClickerBought", false); updateCounterText(); updateHighScoreText() }
    override fun onPause() { super.onPause(); saveData() }
    override fun onDestroy() { super.onDestroy(); soundPool.release(); bonusHandler.removeCallbacksAndMessages(null); autoClickHandler.removeCallbacksAndMessages(null) }
    private fun showMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_reset -> { resetButton.performClick(); true }
                R.id.action_exit -> { finish(); true }
                R.id.action_back -> true
                else -> false
            }
        }
        popup.show()
    }
}