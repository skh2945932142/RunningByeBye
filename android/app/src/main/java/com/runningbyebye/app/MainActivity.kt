package com.runningbyebye.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var rootContainer: View
    private lateinit var bgImage: ImageView
    private lateinit var bgScrim: View
    private lateinit var rootContent: LinearLayout
    private lateinit var tvAppTitle: TextView
    private lateinit var tvAppSubtitle: TextView
    private lateinit var btnAppearance: ImageButton
    private lateinit var groupOpenIdInput: LinearLayout
    private lateinit var etOpenID: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnEditOpenId: MaterialButton
    private lateinit var spinnerField: Spinner
    private lateinit var etPace: TextInputEditText
    private lateinit var etInterval: TextInputEditText
    private lateinit var btnStart: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var cardUserInfo: LinearLayout
    private lateinit var tvStatusPill: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserInfo: TextView
    private lateinit var cardLogin: MaterialCardView
    private lateinit var cardParams: MaterialCardView
    private lateinit var cardProgress: MaterialCardView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var tvMileage: TextView
    private lateinit var tvPoints: TextView

    private var runner: mobile.Runner? = null
    private lateinit var runOptionsStore: RunOptionsStore
    private lateinit var appearanceStore: AppearanceStore
    private lateinit var appearanceApplier: AppearanceApplier
    private lateinit var backgroundPickerLauncher: ActivityResultLauncher<Array<String>>
    private var appearanceConfig = AppearanceConfig()
    private var loggedInOpenID: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastTerminalStatus: RunStatus? = null
    private val runStatusObserver = RunStatusObserver { status ->
        handler.post { renderRunStatus(status) }
    }

    // 场地映射
    private val fieldCodes = arrayOf("T1001", "T1005", "T1014")
    private val fieldNames = arrayOf("风华运动场", "太极运动场", "宁静苑")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        runOptionsStore = RunOptionsStore.from(this)
        appearanceStore = AppearanceStore.from(this)
        appearanceApplier = AppearanceApplier(this, window)
        registerBackgroundPicker()
        initViews()
        appearanceConfig = appearanceStore.load()
        setupSpinner()
        setupListeners()
        restoreSavedOptions()
        applyAppearance()

        // 初始化登录用 Go Runner
        initRunner()
        requestNotificationPermissionIfNeeded()
    }

    override fun onStart() {
        super.onStart()
        RunServiceState.observe(runStatusObserver)
    }

    override fun onStop() {
        RunServiceState.removeObserver(runStatusObserver)
        super.onStop()
    }

    private fun initViews() {
        rootContainer = findViewById(R.id.rootContainer)
        bgImage = findViewById(R.id.bgImage)
        bgScrim = findViewById(R.id.bgScrim)
        rootContent = findViewById(R.id.rootContent)
        tvAppTitle = findViewById(R.id.tvAppTitle)
        tvAppSubtitle = findViewById(R.id.tvAppSubtitle)
        btnAppearance = findViewById(R.id.btnAppearance)
        groupOpenIdInput = findViewById(R.id.groupOpenIdInput)
        etOpenID = findViewById(R.id.etOpenID)
        btnLogin = findViewById(R.id.btnLogin)
        btnEditOpenId = findViewById(R.id.btnEditOpenId)
        spinnerField = findViewById(R.id.spinnerField)
        etPace = findViewById(R.id.etPace)
        etInterval = findViewById(R.id.etInterval)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        cardUserInfo = findViewById(R.id.cardUserInfo)
        tvStatusPill = findViewById(R.id.tvStatusPill)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserInfo = findViewById(R.id.tvUserInfo)
        cardLogin = findViewById(R.id.cardLogin)
        cardParams = findViewById(R.id.cardParams)
        cardProgress = findViewById(R.id.cardProgress)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        tvMileage = findViewById(R.id.tvMileage)
        tvPoints = findViewById(R.id.tvPoints)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fieldNames)
        spinnerField.adapter = adapter
    }

    private fun setupListeners() {
        btnAppearance.setOnClickListener { showAppearanceSheet() }
        btnLogin.setOnClickListener { doLogin() }
        btnEditOpenId.setOnClickListener { expandOpenIdInput() }
        btnStart.setOnClickListener { doStartRun() }
        btnStop.setOnClickListener { doStopRun() }
    }

    private fun registerBackgroundPicker() {
        backgroundPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }
            persistBackgroundUri(uri)
            saveAndApplyAppearance(appearanceConfig.copy(customBackgroundUri = uri.toString()))
        }
    }

    private fun persistBackgroundUri(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Some document providers grant read access without supporting persistable grants.
        }
    }

    private fun restoreSavedOptions() {
        val options = runOptionsStore.load()
        etOpenID.setText(options.openID)
        etPace.setText(options.pace)
        etInterval.setText(options.intervalSeconds)

        val fieldIndex = fieldCodes.indexOf(options.fieldCode).takeIf { it >= 0 } ?: 0
        spinnerField.setSelection(fieldIndex)
    }

    private fun applyAppearance() {
        appearanceApplier.apply(
            AppearanceTargets(
                root = rootContainer,
                backgroundImage = bgImage,
                backgroundScrim = bgScrim,
                appTitle = tvAppTitle,
                appSubtitle = tvAppSubtitle,
                appearanceButton = btnAppearance,
                cards = listOf(cardLogin, cardParams, cardProgress),
                glassPanels = listOf(cardUserInfo, spinnerField),
                primaryButtons = listOf(btnLogin, btnStart),
                secondaryButtons = listOf(btnEditOpenId),
                dangerButtons = listOf(btnStop),
                statusPill = tvStatusPill,
                progressBar = progressBar,
            ),
            appearanceConfig,
        )
    }

    private fun saveAndApplyAppearance(config: AppearanceConfig) {
        appearanceStore.save(config)
        appearanceConfig = appearanceStore.load()
        applyAppearance()
    }

    private fun showAppearanceSheet() {
        val dialog = BottomSheetDialog(this)
        val sheet = layoutInflater.inflate(R.layout.dialog_appearance, null)
        dialog.setContentView(sheet)
        bindAppearanceSheet(sheet, dialog)
        dialog.show()
    }

    private fun bindAppearanceSheet(sheet: View, dialog: BottomSheetDialog) {
        val presetGroup = sheet.findViewById<GridLayout>(R.id.groupPresetButtons)
        val backgroundLabel = sheet.findViewById<TextView>(R.id.tvCustomBackgroundLabel)
        val blurValue = sheet.findViewById<TextView>(R.id.tvBlurValue)
        val scrimValue = sheet.findViewById<TextView>(R.id.tvScrimValue)
        val glassValue = sheet.findViewById<TextView>(R.id.tvGlassValue)
        val blurSeek = sheet.findViewById<SeekBar>(R.id.seekBlurStrength)
        val scrimSeek = sheet.findViewById<SeekBar>(R.id.seekScrimStrength)
        val glassSeek = sheet.findViewById<SeekBar>(R.id.seekGlassStrength)
        val pickButton = sheet.findViewById<MaterialButton>(R.id.btnPickBackground)
        val clearButton = sheet.findViewById<MaterialButton>(R.id.btnClearBackground)
        val resetButton = sheet.findViewById<MaterialButton>(R.id.btnResetAppearance)
        val closeButton = sheet.findViewById<MaterialButton>(R.id.btnCloseAppearance)

        fun syncLabels() {
            val preset = AppearancePresetCatalog.find(appearanceConfig.presetId)
            backgroundLabel.text = if (appearanceConfig.customBackgroundUri.isBlank()) {
                "当前使用预设背景: ${preset.title}"
            } else {
                "当前使用相册图片，${preset.title} 作为玻璃配色"
            }
            blurValue.text = "${appearanceConfig.blurStrength}%"
            scrimValue.text = "${appearanceConfig.scrimStrength}%"
            glassValue.text = "${appearanceConfig.glassStrength}%"
        }

        fun syncControls() {
            blurSeek.progress = appearanceConfig.blurStrength
            scrimSeek.progress = appearanceConfig.scrimStrength
            glassSeek.progress = appearanceConfig.glassStrength
            syncLabels()
            bindPresetButtons(presetGroup) { preset ->
                saveAndApplyAppearance(
                    appearanceConfig.copy(
                        presetId = preset.id,
                        customBackgroundUri = "",
                    ),
                )
                syncControls()
            }
        }

        syncControls()

        blurSeek.onUserProgressChanged {
            saveAndApplyAppearance(appearanceConfig.copy(blurStrength = it))
            syncLabels()
        }
        scrimSeek.onUserProgressChanged {
            saveAndApplyAppearance(appearanceConfig.copy(scrimStrength = it))
            syncLabels()
        }
        glassSeek.onUserProgressChanged {
            saveAndApplyAppearance(appearanceConfig.copy(glassStrength = it))
            syncLabels()
        }
        pickButton.setOnClickListener {
            dialog.dismiss()
            backgroundPickerLauncher.launch(arrayOf("image/*"))
        }
        clearButton.setOnClickListener {
            saveAndApplyAppearance(appearanceConfig.copy(customBackgroundUri = ""))
            syncControls()
        }
        resetButton.setOnClickListener {
            saveAndApplyAppearance(AppearanceConfig())
            syncControls()
        }
        closeButton.setOnClickListener { dialog.dismiss() }
    }

    private fun bindPresetButtons(
        group: GridLayout,
        onPresetSelected: (AppearancePreset) -> Unit,
    ) {
        group.removeAllViews()
        AppearancePresetCatalog.all().forEach { preset ->
            val selected = appearanceConfig.customBackgroundUri.isBlank() && appearanceConfig.presetId == preset.id
            val button = MaterialButton(this).apply {
                text = "${preset.title}\n${preset.subtitle}"
                isAllCaps = false
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                maxLines = 2
                minHeight = dp(64)
                setPadding(dp(10), 0, dp(10), 0)
                backgroundTintList = ColorStateList.valueOf(
                    withAlpha(preset.surfaceTint, if (selected) 116 else 58),
                )
                strokeColor = ColorStateList.valueOf(
                    withAlpha(preset.accent, if (selected) 230 else 104),
                )
                strokeWidth = dp(if (selected) 2 else 1)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                setOnClickListener { onPresetSelected(preset) }
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(0, 0, dp(8), dp(8))
            }
            group.addView(button, params)
        }
    }

    private fun SeekBar.onUserProgressChanged(onChange: (Int) -> Unit) {
        setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        onChange(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            },
        )
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATION_PERMISSION,
        )
    }

    private fun initRunner() {
        try {
            val dataDir = File(filesDir, "runc_data")
            dataDir.mkdirs()

            // 将 assets 中的点位数据复制到内部存储
            copyPointsData(dataDir)

            runner = mobile.Mobile.newRunner(dataDir.absolutePath)
        } catch (e: Exception) {
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun copyPointsData(dataDir: File) {
        val pointsDir = File(dataDir, "points")
        PointAssetCopier.copy(
            pointsDir = pointsDir,
            listAssets = { path -> assets.list(path) },
            openAsset = { path -> assets.open(path) },
        )
    }

    private fun doLogin() {
        val openID = etOpenID.text?.toString()?.trim() ?: ""
        if (openID.isEmpty()) {
            etOpenID.error = "请输入 OpenID"
            return
        }

        loggedInOpenID = null
        btnStart.isEnabled = false
        btnLogin.isEnabled = false
        btnLogin.text = "登录中..."

        Thread {
            try {
                val userInfoJson = runner?.login(openID)
                handler.post {
                    btnLogin.isEnabled = true
                    btnLogin.text = "登录"

                    if (userInfoJson != null) {
                        val info = JSONObject(userInfoJson)
                        loggedInOpenID = openID
                        saveCurrentOptions(openID)

                        showUserSummary(info)

                        btnStart.isEnabled = true
                        Toast.makeText(this, "登录成功!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    btnLogin.isEnabled = true
                    btnLogin.text = "登录"
                    Toast.makeText(this, "登录失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun doStartRun() {
        val openID = loggedInOpenID ?: return
        val fieldIndex = spinnerField.selectedItemPosition
        val fieldCode = fieldCodes[fieldIndex]
        val paceResult = RunInputValidator.resolvePace(etPace.text?.toString())
        if (!paceResult.isValid) {
            etPace.error = paceResult.errorMessage
            return
        }
        val intervalResult = RunInputValidator.resolveIntervalSeconds(etInterval.text?.toString())
        if (!intervalResult.isValid) {
            etInterval.error = intervalResult.errorMessage
            return
        }
        val pace = paceResult.value
        val interval = intervalResult.value
        etPace.setText(pace)
        etInterval.setText(interval)
        saveCurrentOptions(openID, fieldCode, pace, interval)

        val pointsDir = File(filesDir, "runc_data/points").absolutePath

        btnStart.isEnabled = false
        btnLogin.isEnabled = false
        btnEditOpenId.isEnabled = false
        btnStop.isEnabled = true
        showProgressCard()
        setStatusText("跑步中...", R.color.success, "跑步中")

        try {
            RunForegroundService.startRun(
                context = this,
                openID = openID,
                fieldCode = fieldCode,
                fieldName = fieldNames[fieldIndex],
                pace = pace,
                interval = interval,
                dataDir = File(filesDir, "runc_data").absolutePath,
                pointsDir = pointsDir,
            )
            setStatusText("跑步中... ${fieldNames[fieldIndex]}", R.color.success, "跑步中")
            lastTerminalStatus = null
        } catch (e: Exception) {
            setStatusText("启动失败: ${e.message}", R.color.danger, "失败")
            btnStart.isEnabled = true
            btnLogin.isEnabled = true
            btnEditOpenId.isEnabled = true
            btnStop.isEnabled = false
        }
    }

    private fun doStopRun() {
        RunForegroundService.stopRun(this)
        setStatusText("正在停止...", R.color.danger, "停止中")
        btnStop.isEnabled = false
    }

    private fun saveCurrentOptions(openID: String) {
        val rawFieldIndex = spinnerField.selectedItemPosition
        val fieldIndex = if (rawFieldIndex in fieldCodes.indices) rawFieldIndex else 0
        val pace = RunInputValidator.resolvePace(etPace.text?.toString()).value.ifEmpty { RunOptionsStore.DEFAULT_PACE }
        val interval = RunInputValidator.resolveIntervalSeconds(etInterval.text?.toString()).value.ifEmpty { RunOptionsStore.DEFAULT_INTERVAL_SECONDS }
        saveCurrentOptions(openID, fieldCodes[fieldIndex], pace, interval)
    }

    private fun saveCurrentOptions(openID: String, fieldCode: String, pace: String, interval: String) {
        runOptionsStore.save(
            SavedRunOptions(
                openID = openID,
                fieldCode = fieldCode,
                pace = pace,
                intervalSeconds = interval,
            ),
        )
    }

    private fun renderRunStatus(status: RunStatus) {
        when (status) {
            RunStatus.Idle -> {
                btnLogin.isEnabled = true
                btnStart.isEnabled = loggedInOpenID != null
                btnEditOpenId.isEnabled = true
                btnStop.isEnabled = false
                tvStatusPill.text = if (loggedInOpenID == null) "未登录" else "待开始"
            }

            is RunStatus.Running -> {
                showProgressCard()
                setStatusText("跑步中... ${status.fieldName}", R.color.success, "跑步中")
                btnLogin.isEnabled = false
                btnEditOpenId.isEnabled = false
                btnStart.isEnabled = false
                btnStop.isEnabled = true
                lastTerminalStatus = null
            }

            is RunStatus.Progress -> {
                val percent = if (status.total > 0) status.submitted * 100 / status.total else 0
                showProgressCard()
                setStatusText("跑步中...", R.color.success, "跑步中")
                progressBar.progress = percent
                tvProgress.text = "$percent%"
                tvMileage.text = MileageFormatter.formatKm(status.mileage)
                tvPoints.text = "${status.submitted}/${status.total}"
                btnLogin.isEnabled = false
                btnEditOpenId.isEnabled = false
                btnStart.isEnabled = false
                btnStop.isEnabled = true
                pulseProgressMetrics()
            }

            is RunStatus.Completed -> {
                showProgressCard()
                setStatusText("跑步完成!", R.color.success, "完成")
                progressBar.progress = 100
                tvProgress.text = "100%"
                tvMileage.text = MileageFormatter.formatKm(status.mileage)
                btnLogin.isEnabled = true
                btnEditOpenId.isEnabled = true
                btnStart.isEnabled = loggedInOpenID != null
                btnStop.isEnabled = false
                showTerminalToastOnce(status, "跑步完成! 里程: ${MileageFormatter.formatKm(status.mileage)}")
            }

            is RunStatus.Failed -> {
                showProgressCard()
                setStatusText("失败: ${status.message}", R.color.danger, "失败")
                btnLogin.isEnabled = true
                btnEditOpenId.isEnabled = true
                btnStart.isEnabled = loggedInOpenID != null
                btnStop.isEnabled = false
                showTerminalToastOnce(status, "跑步失败: ${status.message}")
            }

            is RunStatus.Stopped -> {
                showProgressCard()
                setStatusText(status.message, R.color.danger, "已停止")
                btnLogin.isEnabled = true
                btnEditOpenId.isEnabled = true
                btnStart.isEnabled = loggedInOpenID != null
                btnStop.isEnabled = false
                showTerminalToastOnce(status, status.message)
            }
        }
    }

    private fun showUserSummary(info: JSONObject) {
        TransitionManager.beginDelayedTransition(rootContent)
        tvUserName.text = info.optString("username").ifBlank { "已登录用户" }
        tvUserInfo.text = "${info.optString("student_no")} · ${info.optString("dept_name")}"
        cardUserInfo.visibility = View.VISIBLE
        groupOpenIdInput.visibility = View.GONE
        tvStatusPill.text = "待开始"
        animateAppear(cardUserInfo)
    }

    private fun expandOpenIdInput() {
        TransitionManager.beginDelayedTransition(rootContent)
        groupOpenIdInput.visibility = View.VISIBLE
        etOpenID.requestFocus()
        tvStatusPill.text = if (loggedInOpenID == null) "未登录" else "已登录"
        animateAppear(groupOpenIdInput)
    }

    private fun showProgressCard() {
        if (cardProgress.visibility == View.VISIBLE) {
            return
        }
        TransitionManager.beginDelayedTransition(rootContent)
        cardProgress.visibility = View.VISIBLE
        animateAppear(cardProgress)
    }

    private fun setStatusText(status: String, colorRes: Int, pillText: String) {
        tvStatus.text = status
        tvStatus.setTextColor(getColor(colorRes))
        tvStatusPill.text = pillText
    }

    private fun animateAppear(view: View) {
        view.alpha = 0f
        view.translationY = 8f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(180L)
            .start()
    }

    private fun pulseProgressMetrics() {
        tvMileage.animate()
            .scaleX(1.03f)
            .scaleY(1.03f)
            .setDuration(90L)
            .withEndAction {
                tvMileage.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(90L)
                    .start()
            }
            .start()
    }

    private fun showTerminalToastOnce(status: RunStatus, message: String) {
        if (lastTerminalStatus == status) {
            return
        }
        lastTerminalStatus = status
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 2001
    }
}
