package com.example.clickcounter

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // Счёт и рекорд
    private var counter = 0
    private var highScore = 0

    // UI элементы
    private lateinit var counterTextView: TextView
    private lateinit var highScoreTextView: TextView
    private lateinit var clickButton: Button
    private lateinit var resetButton: Button
    private lateinit var victoryTextView: TextView

    // Звук
    private lateinit var soundPool: SoundPool
    private var clickSoundId: Int = 0

    // Константы
    private val PREFS_NAME = "ClickCounterPrefs"
    private val COUNTER_KEY = "counter"
    private val HIGH_SCORE_KEY = "highScore"
    private val WINNING_SCORE = 77777

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация UI
        counterTextView = findViewById(R.id.counterTextView)
        highScoreTextView = findViewById(R.id.highScoreTextView)
        clickButton = findViewById(R.id.clickButton)
        resetButton = findViewById(R.id.resetButton)
        victoryTextView = findViewById(R.id.victoryTextView)

        // Загрузка данных
        loadData()

        // Настройка звука и анимации
        setupSound()
        val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce)

        // Проверка, не победили ли мы уже при загрузке
        if (counter >= WINNING_SCORE) {
            showVictoryState()
        }

        // --- ОБРАБОТЧИКИ КНОПОК ---

        // Основная кнопка клика
        clickButton.setOnClickListener {
            it.startAnimation(bounceAnimation)
            playSound()

            counter++
            updateCounterText()
            checkHighScore()

            if (counter >= WINNING_SCORE) {
                showVictoryState()
            }
        }

        // Кнопка сброса
        resetButton.setOnClickListener {
            counter = 0
            updateCounterText()
            hideVictoryState()
            saveData() // Сохраняем сброс
        }
    }

    private fun updateCounterText() {
        counterTextView.text = counter.toString()
    }

    private fun updateHighScoreText() {
        highScoreTextView.text = "Рекорд: $highScore"
    }

    private fun checkHighScore() {
        if (counter > highScore) {
            highScore = counter
            updateHighScoreText()
        }
    }

    // Показывает победное состояние
    private fun showVictoryState() {
        victoryTextView.visibility = View.VISIBLE
        clickButton.visibility = View.GONE
        resetButton.visibility = View.VISIBLE
    }

    // Скрывает победное состояние и возвращает к игре
    private fun hideVictoryState() {
        victoryTextView.visibility = View.GONE
        clickButton.visibility = View.VISIBLE
        resetButton.visibility = View.GONE
    }

    // --- ЗВУК ---
    private fun setupSound() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(audioAttributes).build()
        clickSoundId = soundPool.load(this, R.raw.click_sound, 1)
    }

    private fun playSound() {
        if (clickSoundId != 0) {
            soundPool.play(clickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
        }
    }

    // --- СОХРАНЕНИЕ И ЗАГРУЗКА ---
    private fun saveData() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        prefs.putInt(COUNTER_KEY, counter)
        prefs.putInt(HIGH_SCORE_KEY, highScore)
        prefs.apply()
    }

    private fun loadData() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        counter = prefs.getInt(COUNTER_KEY, 0)
        highScore = prefs.getInt(HIGH_SCORE_KEY, 0)

        updateCounterText()
        updateHighScoreText()
    }

    override fun onPause() {
        super.onPause()
        saveData() // Сохраняем данные, когда приложение сворачивается
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release() // Освобождаем ресурсы звука
    }
}