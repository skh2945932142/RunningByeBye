package com.runningbyebye.app

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.TransitionManager
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var rootContainer: MotionLayout
    private lateinit var bgImage: ImageView
    private lateinit var bgScrim: View
    private lateinit var bgSheen: View
    private lateinit var contentScroll: ScrollView
    private lateinit var rootContent: LinearLayout
    private lateinit var headerBar: LinearLayout
    private lateinit var tvAppTitle: TextView
    private lateinit var btnAppearance: ImageButton
    private lateinit var groupOpenIdInput: LinearLayout
    private lateinit var tilOpenID: TextInputLayout
    private lateinit var etOpenID: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnEditOpenId: MaterialButton
    private lateinit var groupFieldCards: LinearLayout
    private lateinit var spinnerField: Spinner
    private lateinit var tilPace: TextInputLayout
    private lateinit var etPace: TextInputEditText
    private lateinit var tilInterval: TextInputLayout
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
    private lateinit var progressBar: GlassLinearProgressView
    private lateinit var dashboardHalo: DashboardHaloView
    private lateinit var circularProgress: CircularProgressIndicator
    private lateinit var panelProgressMetric: LinearLayout
    private lateinit var panelFieldMetric: LinearLayout
    private lateinit var panelPointsMetric: LinearLayout
    private lateinit var panelPaceMetric: LinearLayout
    private lateinit var progressSheen: View
    private lateinit var tvProgress: TextView
    private lateinit var tvMileage: TextView
    private lateinit var tvPoints: TextView
    private lateinit var tvFieldName: TextView
    private lateinit var tvPaceMetric: TextView
    private lateinit var cardRunSummary: LinearLayout
    private lateinit var summaryBadge: RunSummaryBadgeView
    private lateinit var tvSummaryTitle: TextView
    private lateinit var tvSummaryMileage: TextView
    private lateinit var tvSummaryDetails: TextView
    private lateinit var groupActions: LinearLayout

    private var runner: mobile.Runner? = null
    private lateinit var runOptionsStore: RunOptionsStore
    private lateinit var appearanceStore: AppearanceStore
    private lateinit var appearanceApplier: AppearanceApplier
    private lateinit var motionController: GlassMotionController
    private lateinit var backgroundPickerLauncher: ActivityResultLauncher<Array<String>>
    private var appearanceConfig = AppearanceConfig()
    private var loggedInOpenID: String? = null
    private var selectedFieldIndex = 0
    private var lastRunningFieldName = ""
    private var lastSubmittedPoints = 0
    private var lastTotalPoints = 0
    private var displayedMileage = 0.0
    private var mileageAnimator: ValueAnimator? = null
    private val milestoneTracker = ProgressMilestoneTracker()
    private var fieldCardEntrancePlayed = false
    private val handler = Handler(Looper.getMainLooper())
    private var lastTerminalStatus: RunStatus? = null
    private val runStatusObserver = RunStatusObserver { status ->
        handler.post { renderRunStatus(status) }
    }

    // 场地映射
    private val fieldCodes = FieldCatalog.options.map { it.code }.toTypedArray()
    private val fieldNames = FieldCatalog.options.map { it.name }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        runOptionsStore = RunOptionsStore.from(this)
        appearanceStore = AppearanceStore.from(this)
        appearanceApplier = AppearanceApplier(this, window)
        motionController = GlassMotionController(this)
        registerBackgroundPicker()
        initViews()
        appearanceConfig = appearanceStore.load()
        setupSpinner()
        setupListeners()
        restoreSavedOptions()
        setupBackHandling()
        applyAppearance()
        setupMotion()

        // 初始化登录用 Go Runner
        initRunner()
        requestNotificationPermissionIfNeeded()
    }

    override fun onStart() {
        super.onStart()
        motionController.startAmbientSheen(bgSheen)
        RunServiceState.observe(runStatusObserver)
    }

    override fun onStop() {
        RunServiceState.removeObserver(runStatusObserver)
        motionController.setStatusPulse(tvStatusPill, false)
        motionController.stopAmbientSheen()
        super.onStop()
    }

    override fun onDestroy() {
        motionController.release()
        super.onDestroy()
    }

    private fun initViews() {
        rootContainer = findViewById(R.id.rootContainer)
        bgImage = findViewById(R.id.bgImage)
        bgScrim = findViewById(R.id.bgScrim)
        bgSheen = findViewById(R.id.bgSheen)
        contentScroll = findViewById(R.id.contentScroll)
        rootContent = findViewById(R.id.rootContent)
        headerBar = findViewById(R.id.headerBar)
        tvAppTitle = findViewById(R.id.tvAppTitle)
        btnAppearance = findViewById(R.id.btnAppearance)
        groupOpenIdInput = findViewById(R.id.groupOpenIdInput)
        tilOpenID = findViewById(R.id.tilOpenID)
        etOpenID = findViewById(R.id.etOpenID)
        btnLogin = findViewById(R.id.btnLogin)
        btnEditOpenId = findViewById(R.id.btnEditOpenId)
        groupFieldCards = findViewById(R.id.groupFieldCards)
        spinnerField = findViewById(R.id.spinnerField)
        tilPace = findViewById(R.id.tilPace)
        etPace = findViewById(R.id.etPace)
        tilInterval = findViewById(R.id.tilInterval)
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
        dashboardHalo = findViewById(R.id.dashboardHalo)
        circularProgress = findViewById(R.id.circularProgress)
        panelProgressMetric = findViewById(R.id.panelProgressMetric)
        panelFieldMetric = findViewById(R.id.panelFieldMetric)
        panelPointsMetric = findViewById(R.id.panelPointsMetric)
        panelPaceMetric = findViewById(R.id.panelPaceMetric)
        progressSheen = findViewById(R.id.progressSheen)
        tvProgress = findViewById(R.id.tvProgress)
        tvMileage = findViewById(R.id.tvMileage)
        tvPoints = findViewById(R.id.tvPoints)
        tvFieldName = findViewById(R.id.tvFieldName)
        tvPaceMetric = findViewById(R.id.tvPaceMetric)
        cardRunSummary = findViewById(R.id.cardRunSummary)
        summaryBadge = findViewById(R.id.summaryBadge)
        tvSummaryTitle = findViewById(R.id.tvSummaryTitle)
        tvSummaryMileage = findViewById(R.id.tvSummaryMileage)
        tvSummaryDetails = findViewById(R.id.tvSummaryDetails)
        groupActions = findViewById(R.id.groupActions)
        applyEdgeToEdgeInsets()
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
        selectedFieldIndex = fieldIndex
        spinnerField.setSelection(fieldIndex)
        updateSelectedFieldViews(animate = false)
    }

    private fun applyAppearance() {
        appearanceApplier.apply(
            AppearanceTargets(
                root = rootContainer,
                backgroundImage = bgImage,
                backgroundScrim = bgScrim,
                backgroundSheen = bgSheen,
                appTitle = tvAppTitle,
                appearanceButton = btnAppearance,
                cards = listOf(cardLogin, cardParams, cardProgress),
                glassPanels = listOf(
                    cardUserInfo,
                    panelProgressMetric,
                    panelFieldMetric,
                    panelPointsMetric,
                    panelPaceMetric,
                    cardRunSummary,
                ),
                inputLayouts = listOf(tilOpenID, tilPace, tilInterval),
                primaryButtons = listOf(btnLogin, btnStart),
                secondaryButtons = listOf(btnEditOpenId),
                dangerButtons = listOf(btnStop),
                statusPill = tvStatusPill,
                progressBar = progressBar,
                circularProgress = circularProgress,
                dashboardHalo = dashboardHalo,
                summaryBadge = summaryBadge,
            ),
            appearanceConfig,
        )
        setupFieldCards(animateEntrance = !fieldCardEntrancePlayed)
    }

    private fun applyEdgeToEdgeInsets() {
        val baseStart = rootContent.paddingStart
        val baseTop = rootContent.paddingTop
        val baseEnd = rootContent.paddingEnd
        val baseBottom = rootContent.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(rootContent) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                baseStart,
                baseTop + bars.top,
                baseEnd,
                baseBottom + bars.bottom,
            )
            insets
        }
        ViewCompat.requestApplyInsets(rootContent)
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val running = RunServiceState.current() is RunStatus.Running ||
                        RunServiceState.current() is RunStatus.Progress ||
                        btnStop.isEnabled
                    if (!running) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        return
                    }
                    showStopRunningConfirm()
                }
            },
        )
    }

    private fun showStopRunningConfirm() {
        MaterialAlertDialogBuilder(this)
            .setTitle("正在跑步")
            .setMessage("退出前是否停止当前跑步？")
            .setPositiveButton("停止跑步") { _, _ ->
                rootContainer.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                doStopRun()
            }
            .setNegativeButton("继续跑步", null)
            .show()
    }

    private fun setupFieldCards(animateEntrance: Boolean) {
        groupFieldCards.removeAllViews()
        val preset = AppearancePresetCatalog.find(appearanceConfig.presetId)
        FieldCatalog.options.forEachIndexed { index, option ->
            val summary = loadFieldSummary(option)
            val card = createFieldCard(summary, index, preset)
            groupFieldCards.addView(card)
        }
        updateSelectedFieldViews(animate = false)
        if (animateEntrance) {
            groupFieldCards.post {
                motionController.playEntrance(groupFieldCards.childrenList())
                groupFieldCards.childrenList()
                    .getOrNull(selectedFieldIndex)
                    ?.findFieldTrackView()
                    ?.playReveal()
            }
            fieldCardEntrancePlayed = true
        }
    }

    private fun createFieldCard(
        summary: FieldSummary,
        index: Int,
        preset: AppearancePreset,
    ): LinearLayout {
        val selected = index == selectedFieldIndex
        val title = TextView(this).apply {
            text = summary.option.name
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
        }
        val track = FieldTrackView(this).apply {
            setTrack(summary.points, preset.accent)
        }
        val meta = TextView(this).apply {
            text = "${summary.pointCount} 点 · ${MileageFormatter.formatKm(summary.distanceKm)}"
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            textSize = 12f
            maxLines = 1
        }

        return LinearLayout(this).apply {
            tag = FIELD_CARD_TAG_PREFIX + index
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(112)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = createFieldCardBackground(preset, selected)
            scaleX = if (selected) 1.02f else 1f
            scaleY = if (selected) 1.02f else 1f
            isClickable = true
            isFocusable = true
            addView(
                title,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            addView(
                track,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(52),
                ).apply {
                    topMargin = dp(6)
                },
            )
            addView(
                meta,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = dp(4)
                },
            )
            setOnClickListener {
                selectField(index, animate = true)
            }
            motionController.bindPressFeedback(GlassMotionController.PressFeedbackStyle.PANEL, this)
            layoutParams = LinearLayout.LayoutParams(dp(152), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(8)
            }
        }
    }

    private fun loadFieldSummary(option: FieldOption): FieldSummary {
        val dir = "${FieldCatalog.POINTS_ASSET_ROOT}/${option.code}"
        val firstJson = try {
            assets.list(dir)
                ?.filter { it.endsWith(".json", ignoreCase = true) }
                ?.sorted()
                ?.firstOrNull()
        } catch (_: Exception) {
            null
        }
        return FieldPointParser.summarize(option) {
            firstJson?.let { assets.open("$dir/$it") }
        }
    }

    private fun createFieldCardBackground(preset: AppearancePreset, selected: Boolean): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            setColor(withAlpha(preset.surfaceTint, if (selected) 112 else 58))
            setStroke(dp(if (selected) 2 else 1), withAlpha(preset.accent, if (selected) 232 else 104))
        }
    }

    private fun selectField(index: Int, animate: Boolean) {
        selectedFieldIndex = index.coerceIn(fieldCodes.indices)
        spinnerField.setSelection(selectedFieldIndex)
        updateSelectedFieldViews(animate)
    }

    private fun updateSelectedFieldViews(animate: Boolean) {
        if (!::groupFieldCards.isInitialized) {
            return
        }
        val preset = AppearancePresetCatalog.find(appearanceConfig.presetId)
        groupFieldCards.childrenList().forEachIndexed { index, child ->
            val selected = index == selectedFieldIndex
            child.background = createFieldCardBackground(preset, selected)
            child.animate()
                .scaleX(if (selected) 1.02f else 1f)
                .scaleY(if (selected) 1.02f else 1f)
                .setDuration(if (animate) 170L else 0L)
                .start()
        }
        val fieldName = fieldNames.getOrElse(selectedFieldIndex) { fieldNames.first() }
        tvFieldName.text = shortFieldName(fieldName)
        if (animate) {
            groupFieldCards.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            groupFieldCards.childrenList()
                .getOrNull(selectedFieldIndex)
                ?.findFieldTrackView()
                ?.playReveal()
        }
    }

    private fun saveAndApplyAppearance(config: AppearanceConfig) {
        appearanceStore.save(config)
        appearanceConfig = appearanceStore.load()
        applyAppearance()
    }

    private fun setupMotion() {
        motionController.bindHomeScrollMotion(
            scrollView = contentScroll,
            motionLayout = rootContainer,
            headerBar = headerBar,
            backgroundSheen = bgSheen,
            panels = listOf(cardLogin, cardParams, groupActions, cardProgress),
        )
        motionController.bindPressFeedback(
            GlassMotionController.PressFeedbackStyle.ICON,
            btnAppearance,
        )
        motionController.bindPressFeedback(
            GlassMotionController.PressFeedbackStyle.PRIMARY,
            btnLogin,
            btnStart,
        )
        motionController.bindPressFeedback(
            GlassMotionController.PressFeedbackStyle.SECONDARY,
            btnEditOpenId,
        )
        motionController.bindPressFeedback(
            GlassMotionController.PressFeedbackStyle.PRIMARY,
            btnStop,
        )
        rootContent.post {
            motionController.playEntrance(
                listOf(headerBar, cardLogin, cardParams, groupActions),
            )
        }
    }

    private fun showAppearanceSheet() {
        val dialog = BottomSheetDialog(this)
        val sheet = layoutInflater.inflate(R.layout.dialog_appearance, null)
        dialog.setContentView(sheet)
        bindAppearanceSheet(sheet, dialog)
        dialog.setOnShowListener {
            sheet.findViewById<View>(R.id.appearanceSheet)?.let { motionController.playSheetEntrance(it) }
        }
        dialog.show()
    }

    private fun bindAppearanceSheet(sheet: View, dialog: BottomSheetDialog) {
        val presetGroup = sheet.findViewById<GridLayout>(R.id.groupPresetButtons)
        val blurValue = sheet.findViewById<TextView>(R.id.tvBlurValue)
        val scrimValue = sheet.findViewById<TextView>(R.id.tvScrimValue)
        val glassValue = sheet.findViewById<TextView>(R.id.tvGlassValue)
        val blurSeek = sheet.findViewById<Slider>(R.id.seekBlurStrength)
        val scrimSeek = sheet.findViewById<Slider>(R.id.seekScrimStrength)
        val glassSeek = sheet.findViewById<Slider>(R.id.seekGlassStrength)
        val pickButton = sheet.findViewById<MaterialButton>(R.id.btnPickBackground)
        val clearButton = sheet.findViewById<MaterialButton>(R.id.btnClearBackground)
        val resetButton = sheet.findViewById<MaterialButton>(R.id.btnResetAppearance)
        val closeButton = sheet.findViewById<MaterialButton>(R.id.btnCloseAppearance)
        motionController.bindPressFeedback(
            GlassMotionController.PressFeedbackStyle.PRIMARY,
            pickButton,
            closeButton,
        )
        motionController.bindPressFeedback(
            GlassMotionController.PressFeedbackStyle.SECONDARY,
            clearButton,
            resetButton,
        )

        fun syncLabels() {
            blurValue.text = "${appearanceConfig.blurStrength}%"
            scrimValue.text = "${appearanceConfig.scrimStrength}%"
            glassValue.text = "${appearanceConfig.glassStrength}%"
        }

        fun syncControls() {
            blurSeek.value = appearanceConfig.blurStrength.toFloat()
            scrimSeek.value = appearanceConfig.scrimStrength.toFloat()
            glassSeek.value = appearanceConfig.glassStrength.toFloat()
            syncLabels()
            tintAppearanceSheetControls(
                sliders = listOf(blurSeek, scrimSeek, glassSeek),
                buttons = listOf(pickButton, clearButton, resetButton, closeButton),
            )
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

    private fun tintAppearanceSheetControls(
        sliders: List<Slider>,
        buttons: List<MaterialButton>,
    ) {
        val preset = AppearancePresetCatalog.find(appearanceConfig.presetId)
        val accent = preset.accent
        sliders.forEach { slider ->
            slider.trackActiveTintList = ColorStateList.valueOf(accent)
            slider.trackInactiveTintList = ColorStateList.valueOf(withAlpha(accent, 42))
            slider.thumbTintList = ColorStateList.valueOf(accent)
            slider.haloTintList = ColorStateList.valueOf(withAlpha(accent, 42))
        }
        buttons.forEach { button ->
            button.strokeColor = ColorStateList.valueOf(withAlpha(accent, 112))
            button.rippleColor = ColorStateList.valueOf(withAlpha(accent, 46))
        }
    }

    private fun bindPresetButtons(
        group: GridLayout,
        onPresetSelected: (AppearancePreset) -> Unit,
    ) {
        group.removeAllViews()
        AppearancePresetCatalog.all().forEach { preset ->
            val selected = appearanceConfig.customBackgroundUri.isBlank() && appearanceConfig.presetId == preset.id
            val button = AppearancePresetCardView(this).apply {
                bind(preset, selected)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onPresetSelected(preset)
                }
            }
            motionController.bindPressFeedback(GlassMotionController.PressFeedbackStyle.PANEL, button)
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(76)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(0, 0, dp(8), dp(8))
            }
            group.addView(button, params)
        }
    }

    private fun Slider.onUserProgressChanged(onChange: (Int) -> Unit) {
        addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                onChange(value.toInt())
            }
        }
        addOnSliderTouchListener(
            object : com.google.android.material.slider.Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    slider.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    slider.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
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
        val fieldIndex = selectedFieldIndex.coerceIn(fieldCodes.indices)
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
        lastRunningFieldName = fieldNames[fieldIndex]
        lastSubmittedPoints = 0
        lastTotalPoints = 0
        displayedMileage = 0.0
        milestoneTracker.reset()
        showProgressCard()
        hideRunSummary()
        resetDashboard(pace)
        setStatusText("跑步中...", R.color.success, "跑步中")
        rootContainer.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

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
            showRunSummary("启动失败", 0.0, e.message ?: "未知错误", danger = true)
            rootContainer.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            btnStart.isEnabled = true
            btnLogin.isEnabled = true
            btnEditOpenId.isEnabled = true
            btnStop.isEnabled = false
        }
    }

    private fun doStopRun() {
        RunForegroundService.stopRun(this)
        setStatusText("正在停止...", R.color.danger, "停止中")
        rootContainer.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        btnStop.isEnabled = false
    }

    private fun saveCurrentOptions(openID: String) {
        val rawFieldIndex = selectedFieldIndex
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
                motionController.setStatusPulse(tvStatusPill, false)
                btnLogin.isEnabled = true
                btnStart.isEnabled = loggedInOpenID != null
                btnEditOpenId.isEnabled = true
                btnStop.isEnabled = false
                dashboardHalo.setHaloState(progressBar.progress, active = false)
                updateStatusPill(if (loggedInOpenID == null) "未登录" else "待开始", getColor(R.color.primary))
            }

            is RunStatus.Running -> {
                showProgressCard()
                hideRunSummary()
                motionController.setStatusPulse(tvStatusPill, true)
                setStatusText("跑步中... ${status.fieldName}", R.color.success, "跑步中")
                btnLogin.isEnabled = false
                btnEditOpenId.isEnabled = false
                btnStart.isEnabled = false
                btnStop.isEnabled = true
                lastRunningFieldName = status.fieldName
                tvFieldName.text = shortFieldName(status.fieldName)
                tvPaceMetric.text = "${etPace.text} min"
                dashboardHalo.setHaloState(progressBar.progress, active = true)
                lastTerminalStatus = null
            }

            is RunStatus.Progress -> {
                val percent = if (status.total > 0) status.submitted * 100 / status.total else 0
                showProgressCard()
                hideRunSummary()
                motionController.setStatusPulse(tvStatusPill, true)
                setStatusText("跑步中...", R.color.success, "跑步中")
                progressBar.setProgressAnimated(percent)
                motionController.animateProgress(circularProgress, percent)
                dashboardHalo.setHaloState(percent, active = true)
                tvProgress.text = "$percent%"
                animateMileage(status.mileage)
                tvPoints.text = "${status.submitted}/${status.total}"
                lastSubmittedPoints = status.submitted
                lastTotalPoints = status.total
                tvPaceMetric.text = "${etPace.text} min"
                btnLogin.isEnabled = false
                btnEditOpenId.isEnabled = false
                btnStart.isEnabled = false
                btnStop.isEnabled = true
                milestoneTracker.hit(percent)?.let {
                    rootContainer.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    motionController.playProgressSheen(progressSheen)
                    pulseProgressMetrics()
                }
                pulseProgressMetrics()
            }

            is RunStatus.Completed -> {
                showProgressCard()
                motionController.setStatusPulse(tvStatusPill, false)
                setStatusText("跑步完成!", R.color.success, "完成")
                progressBar.setProgressAnimated(100)
                motionController.animateProgress(circularProgress, 100)
                dashboardHalo.setHaloState(100, active = false)
                motionController.playProgressSheen(progressSheen)
                tvProgress.text = "100%"
                animateMileage(status.mileage)
                showRunSummary(
                    title = "跑步完成",
                    mileage = status.mileage,
                    details = terminalSummaryDetails("完成"),
                    danger = false,
                )
                rootContainer.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                btnLogin.isEnabled = true
                btnEditOpenId.isEnabled = true
                btnStart.isEnabled = loggedInOpenID != null
                btnStop.isEnabled = false
                showTerminalToastOnce(status, "跑步完成! 里程: ${MileageFormatter.formatKm(status.mileage)}")
            }

            is RunStatus.Failed -> {
                showProgressCard()
                motionController.setStatusPulse(tvStatusPill, false)
                setStatusText("失败: ${status.message}", R.color.danger, "失败")
                dashboardHalo.setHaloState(progressBar.progress, active = false, danger = true)
                showRunSummary(
                    title = "跑步失败",
                    mileage = displayedMileage,
                    details = terminalSummaryDetails(status.message),
                    danger = true,
                )
                rootContainer.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                btnLogin.isEnabled = true
                btnEditOpenId.isEnabled = true
                btnStart.isEnabled = loggedInOpenID != null
                btnStop.isEnabled = false
                showTerminalToastOnce(status, "跑步失败: ${status.message}")
            }

            is RunStatus.Stopped -> {
                showProgressCard()
                motionController.setStatusPulse(tvStatusPill, false)
                setStatusText(status.message, R.color.danger, "已停止")
                dashboardHalo.setHaloState(progressBar.progress, active = false, danger = true)
                showRunSummary(
                    title = "已停止",
                    mileage = displayedMileage,
                    details = terminalSummaryDetails(status.message),
                    danger = true,
                )
                rootContainer.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
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
        updateStatusPill("待开始", getColor(R.color.primary))
        animateAppear(cardUserInfo)
    }

    private fun expandOpenIdInput() {
        TransitionManager.beginDelayedTransition(rootContent)
        groupOpenIdInput.visibility = View.VISIBLE
        etOpenID.requestFocus()
        updateStatusPill(if (loggedInOpenID == null) "未登录" else "已登录", getColor(R.color.primary))
        animateAppear(groupOpenIdInput)
    }

    private fun showProgressCard() {
        if (cardProgress.visibility == View.VISIBLE) {
            return
        }
        TransitionManager.beginDelayedTransition(rootContent)
        cardProgress.visibility = View.VISIBLE
        cardProgress.post {
            motionController.playEntrance(listOf(cardProgress))
            motionController.playProgressSheen(progressSheen)
        }
    }

    private fun setStatusText(status: String, colorRes: Int, pillText: String) {
        tvStatus.text = status
        val color = getColor(colorRes)
        tvStatus.setTextColor(color)
        updateStatusPill(pillText, color)
    }

    private fun updateStatusPill(text: String, color: Int) {
        val currentColor = tvStatusPill.currentTextColor
        ValueAnimator.ofObject(ArgbEvaluator(), currentColor, color).apply {
            duration = 220L
            addUpdateListener { animator ->
                tvStatusPill.setTextColor(animator.animatedValue as Int)
            }
            start()
        }
        if (tvStatusPill.text == text) {
            return
        }
        tvStatusPill.animate()
            .alpha(0.45f)
            .setDuration(90L)
            .withEndAction {
                tvStatusPill.text = text
                tvStatusPill.animate()
                    .alpha(1f)
                    .setDuration(140L)
                    .start()
            }
            .start()
    }

    private fun resetDashboard(pace: String) {
        mileageAnimator?.cancel()
        displayedMileage = 0.0
        progressBar.progress = 0
        circularProgress.progress = 0
        dashboardHalo.setHaloState(0, active = true)
        tvProgress.text = "0%"
        tvMileage.text = MileageFormatter.formatKmValue(0.0)
        tvPoints.text = "0/0"
        tvFieldName.text = shortFieldName(lastRunningFieldName)
        tvPaceMetric.text = "$pace min"
    }

    private fun animateMileage(target: Double) {
        mileageAnimator?.cancel()
        val start = displayedMileage
        if (start == target) {
            tvMileage.text = MileageFormatter.formatKmValue(target)
            return
        }
        mileageAnimator = ValueAnimator.ofFloat(start.toFloat(), target.toFloat()).apply {
            duration = 620L
            addUpdateListener { animator ->
                val value = (animator.animatedValue as Float).toDouble()
                tvMileage.text = MileageFormatter.formatKmValue(value)
            }
            doOnEnd {
                displayedMileage = target
                tvMileage.text = MileageFormatter.formatKmValue(target)
            }
            start()
        }
    }

    private fun showRunSummary(
        title: String,
        mileage: Double,
        details: String,
        danger: Boolean,
    ) {
        TransitionManager.beginDelayedTransition(rootContent)
        tvSummaryTitle.text = title
        tvSummaryMileage.text = MileageFormatter.formatKm(mileage)
        tvSummaryMileage.setTextColor(getColor(if (danger) R.color.danger else R.color.primary))
        tvSummaryDetails.text = details
        summaryBadge.setBadgeState(
            if (danger) RunSummaryBadgeView.State.DANGER else RunSummaryBadgeView.State.SUCCESS,
            animate = true,
        )
        cardRunSummary.visibility = View.VISIBLE
        cardRunSummary.post {
            animateSummaryAppear()
        }
    }

    private fun hideRunSummary() {
        cardRunSummary.visibility = View.GONE
    }

    private fun terminalSummaryDetails(status: String): String {
        val field = lastRunningFieldName.ifBlank { fieldNames.getOrElse(selectedFieldIndex) { fieldNames.first() } }
        val points = if (lastTotalPoints > 0) {
            "${lastSubmittedPoints}/${lastTotalPoints} 点"
        } else {
            "0/0 点"
        }
        return "$field · $points · $status · ${formatCompletionTime()}"
    }

    private fun formatCompletionTime(): String {
        return SimpleDateFormat("HH:mm", Locale.CHINA).format(Date())
    }

    private fun shortFieldName(name: String): String {
        return name
            .replace("运动场", "")
            .ifBlank { name }
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

    private fun animateSummaryAppear() {
        cardRunSummary.alpha = 0f
        cardRunSummary.translationY = dp(10).toFloat()
        cardRunSummary.scaleX = 0.985f
        cardRunSummary.scaleY = 0.985f
        cardRunSummary.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(260L)
            .start()
        motionController.pulseMetrics(tvSummaryMileage)
    }

    private fun pulseProgressMetrics() {
        motionController.pulseMetrics(tvProgress, tvMileage, tvPoints, tvFieldName, tvPaceMetric)
    }

    private fun LinearLayout.childrenList(): List<View> {
        return List(childCount) { index -> getChildAt(index) }
    }

    private fun View.findFieldTrackView(): FieldTrackView? {
        if (this is FieldTrackView) {
            return this
        }
        if (this !is ViewGroup) {
            return null
        }
        for (index in 0 until childCount) {
            val match = getChildAt(index).findFieldTrackView()
            if (match != null) {
                return match
            }
        }
        return null
    }

    private inline fun ValueAnimator.doOnEnd(crossinline action: () -> Unit) {
        addListener(
            object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) = Unit
                override fun onAnimationEnd(animation: android.animation.Animator) = action()
                override fun onAnimationCancel(animation: android.animation.Animator) = Unit
                override fun onAnimationRepeat(animation: android.animation.Animator) = Unit
            },
        )
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
        private const val FIELD_CARD_TAG_PREFIX = "field-card-"
    }
}
