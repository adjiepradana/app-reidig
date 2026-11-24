package com.reidig.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reidig.shared.GoldToken
import com.reidig.shared.InstallmentApplication
import com.reidig.shared.InstallmentContract
import com.reidig.shared.MockRepository
import com.reidig.shared.PaymentMethod
import com.reidig.shared.PrintMethod
import com.reidig.shared.PrintOrder
import com.reidig.shared.gram
import com.reidig.shared.rupiah

private data class Tab(val route: String, val label: String, val icon: () -> ImageVector)

private val TABS = listOf(
    Tab("home", "Beranda") { PrototypeIcons.Home },
    Tab("wallet", "Portofolio") { PrototypeIcons.PieChart },
    Tab("catalog", "Transaksi") { PrototypeIcons.ArrowsExchange },
    Tab("print-tab", "Cetak") { PrototypeIcons.GoldIngots },
    Tab("account", "Profil") { PrototypeIcons.User },
)

/** Tab yang sedang menyala; satu tab bisa berisi dua halaman. */
private fun activeTab(route: String?): String? = when (route) {
    "portfolio" -> "wallet"
    "sell-tab" -> "catalog"
    else -> route?.takeIf { current -> TABS.any { it.route == current } }
}

/**
 * Navigasi bawah 5 tab sesuai prototipe Goldin v1.2 (`Main.dc.html` L2778–2801):
 * - Beranda · Portofolio (pie-chart) · Transaksi (tombol melayang 56dp di tengah) · Cetak · Profil
 * - Permukaan putih `radius-2xl` atas, shadow overlay, animasi hover/tekan & ring halo aktif 200ms
 */
@Composable
private fun ReidigBottomBar(active: String, onSelect: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().background(Palette.Paper)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .shadow(16.dp, RoundedCornerShape(topStart = GoldinRadius.Xxl, topEnd = GoldinRadius.Xxl))
                .clip(RoundedCornerShape(topStart = GoldinRadius.Xxl, topEnd = GoldinRadius.Xxl))
                .background(Color.White)
                .navigationBarsPadding()
                .padding(top = GoldinSpace.Xs, bottom = 10.dp, start = GoldinSpace.Xs, end = GoldinSpace.Xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            TABS.forEach { tab ->
                val selected = tab.route == active
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val isPressed by interactionSource.collectIsPressedAsState()
                val tint by animateColorAsState(
                    targetValue = when {
                        selected -> Palette.Primary700
                        isHovered || isPressed -> Palette.Primary600
                        else -> Palette.InkSoft
                    },
                    animationSpec = tween(GoldinMotion.DurationColor),
                    label = "tabTint",
                )
                val tabScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1f,
                    animationSpec = tween(GoldinMotion.DurationPress),
                    label = "tabScale",
                )
                Column(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .graphicsLayer { scaleX = tabScale; scaleY = tabScale }
                        .clip(RoundedCornerShape(GoldinRadius.Lg))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                        ) { onSelect(tab.route) }
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    if (tab.route == "catalog") {
                        Spacer(Modifier.height(26.dp))
                    } else {
                        Icon(tab.icon(), null, tint = tint, modifier = Modifier.size(GoldinControl.IconLg))
                    }
                    Spacer(Modifier.height(GoldinSpace.Xxs))
                    Text(
                        tab.label,
                        fontFamily = BodyFontFamily,
                        color = tint,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
        val center = TABS.first { it.route == "catalog" }
        val centerActive = active == "catalog"
        val centerInteraction = remember { MutableInteractionSource() }
        val centerHovered by centerInteraction.collectIsHoveredAsState()
        val centerPressed by centerInteraction.collectIsPressedAsState()
        val centerScale by animateFloatAsState(
            targetValue = if (centerPressed) 0.94f else 1f,
            animationSpec = tween(GoldinMotion.DurationPress),
            label = "centerTabScale",
        )
        val centerBg by animateColorAsState(
            targetValue = when {
                centerPressed -> Palette.Primary800
                centerHovered -> Palette.Primary600
                else -> Palette.Primary700
            },
            animationSpec = tween(GoldinMotion.DurationColor),
            label = "centerTabBg",
        )
        val haloWidth by animateDpAsState(
            targetValue = if (centerActive) 4.dp else 0.dp,
            animationSpec = tween(GoldinMotion.DurationHover),
            label = "centerTabHalo",
        )
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(56.dp)
                .graphicsLayer { scaleX = centerScale; scaleY = centerScale }
                .then(
                    if (haloWidth > 0.dp) Modifier.border(haloWidth, Palette.Primary200, CircleShape)
                    else Modifier
                )
                .shadow(10.dp, CircleShape, ambientColor = Palette.Primary700, spotColor = Palette.Primary700)
                .clip(CircleShape)
                .background(centerBg)
                .clickable(
                    interactionSource = centerInteraction,
                    indication = null,
                    role = Role.Tab,
                    onClickLabel = center.label,
                ) { onSelect(center.route) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(center.icon(), center.label, tint = Palette.Primary50, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun ReidigApp(repo: MockRepository) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val env = remember { AppEnv(repo, snackbar, scope) }
    val nav = rememberNavController()
    val signup = remember { SignupDraft() }
    val kyc = remember { KycDraft() }
    val tradePrefill = remember { TradePrefillDraft() }
    val routinePlanPrefill = remember { RoutinePlanPrefillDraft() }
    val installmentFlow = remember { InstallmentFlowDraft() }
    val cetakFlow = remember { CetakFlowDraft() }
    // Hasil transaksi terakhir untuk layar sukses.
    val results = remember { TransactionResults() }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val state by repo.state.collectAsStateWithLifecycle()
    var pinToastMessage by remember { mutableStateOf<String?>(null) }
    var authToastMessage by remember { mutableStateOf<String?>(null) }
    var autoOpenBiometricOnPin by remember { mutableStateOf(false) }
    var kycGateSheet by remember { mutableStateOf<String?>(null) } // "need" | "wait"

    fun enterMain() = nav.navigate("home") { popUpTo(0) { inclusive = true } }
    fun startKycFlow() {
        val user = state.user
        kyc.reset(
            defaultName = user?.name?.takeIf { it.isNotBlank() } ?: signup.name,
            defaultDob = user?.birthDate?.takeIf { it.isNotBlank() } ?: signup.dob,
        )
        nav.navigate("kyc/consent")
    }
    fun exitKycFlow() {
        if (!nav.popBackStack("kyc/consent", inclusive = true)) {
            nav.popBackStack("home", inclusive = false)
        }
    }
    fun requireVerified(next: String) {
        val user = state.user
        when {
            user?.verified == true -> nav.navigate(next)
            user?.isKycPending == true -> kycGateSheet = "wait"
            else -> kycGateSheet = "need"
        }
    }
    fun requireVerifiedTab(tabRoute: String) {
        val user = state.user
        when {
            user?.verified == true -> nav.switchTab(tabRoute)
            user?.isKycPending == true -> kycGateSheet = "wait"
            else -> kycGateSheet = "need"
        }
    }
    fun openInstallmentFlow(prefillGrams: Double? = null) {
        val user = state.user
        when {
            user?.verified == true -> {
                if (state.installmentContracts.isNotEmpty() && prefillGrams == null) {
                    nav.navigate("cicil/list")
                } else {
                    installmentFlow.reset(prefillGrams ?: 1.0)
                    nav.navigate("cicil/sim")
                }
            }
            user?.isKycPending == true -> kycGateSheet = "wait"
            else -> kycGateSheet = "need"
        }
    }
    fun openCetakFlow(prefillBranchId: String? = null) {
        val user = state.user
        when {
            user?.verified == true -> {
                cetakFlow.reset(prefillBranchId)
                if (prefillBranchId != null) {
                    nav.navigate("cetak/brand?branch=$prefillBranchId")
                } else {
                    nav.navigate("cetak/brand")
                }
            }
            user?.isKycPending == true -> kycGateSheet = "wait"
            else -> kycGateSheet = "need"
        }
    }
    fun openRoutinePlansFlow() {
        val user = state.user
        when {
            user?.verified == true -> {
                if (state.routinePlans.isNotEmpty()) {
                    nav.navigate("plans")
                } else {
                    nav.navigate("plan-create")
                }
            }
            user?.isKycPending == true -> kycGateSheet = "wait"
            else -> kycGateSheet = "need"
        }
    }
    fun openCreateRoutinePlan(prefillAmt: Long? = null, prefillGrams: Double? = null) {
        val user = state.user
        when {
            user?.verified == true -> {
                if (prefillAmt != null || prefillGrams != null) {
                    routinePlanPrefill.set(prefillAmt ?: 0L, prefillGrams)
                }
                nav.navigate("plan-create")
            }
            user?.isKycPending == true -> kycGateSheet = "wait"
            else -> kycGateSheet = "need"
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Sesuai prototipe Goldin v1, aplikasi langsung dimulai pada layar 01 Landing ("welcome").
            NavHost(nav, startDestination = "welcome", modifier = Modifier.weight(1f)) {
                composable("splash") {
                    SplashScreen { nav.navigate("welcome") { popUpTo("splash") { inclusive = true } } }
                }
                composable("welcome") {
                    WelcomeScreen(env, onLogin = { nav.navigate("login") }, onRegister = { nav.navigate("register/email") })
                }
                composable("login") {
                    LoginScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onLoggedIn = { nav.navigate("login/pin") },
                        onRegister = { nav.navigate("register/email") },
                        onForgot = { nav.navigate("forgot/false") },
                        onGoogle = { nav.navigate("google/login") },
                        toastMessage = authToastMessage,
                        onToastDismissed = { authToastMessage = null },
                    )
                }
                composable("login/pin") {
                    val appState by repo.state.collectAsStateWithLifecycle()
                    LoginPinScreen(
                        env = env,
                        isFirstTime = appState.isFirstTimeLogin,
                        autoOpenBiometric = autoOpenBiometricOnPin,
                        toastMessage = pinToastMessage,
                        onToastDismissed = { pinToastMessage = null },
                        onBack = { nav.popBackStack() },
                        onPinSuccess = {
                            autoOpenBiometricOnPin = false
                            enterMain()
                        },
                        onForgotPin = { nav.navigate("forgot/pin/password") },
                    )
                }
                composable(
                    "forgot/{fromPin}",
                    arguments = listOf(navArgument("fromPin") { type = NavType.BoolType; defaultValue = false }),
                ) { entry ->
                    val fromPin = entry.arguments?.getBoolean("fromPin") ?: false
                    ForgotPasswordEmailScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onCodeSent = { email -> nav.navigate("forgot/otp/$email/$fromPin") },
                    )
                }
                composable(
                    "forgot/otp/{email}/{fromPin}",
                    arguments = listOf(
                        navArgument("email") { type = NavType.StringType },
                        navArgument("fromPin") { type = NavType.BoolType; defaultValue = false },
                    ),
                ) { entry ->
                    val email = entry.arguments?.getString("email") ?: MockRepository.GOLDIN_DEMO_EMAIL
                    val fromPin = entry.arguments?.getBoolean("fromPin") ?: false
                    OtpScreen(
                        title = "Verifikasi OTP",
                        step = null,
                        sentVia = "Masukkan kode verifikasi yang dikirimkan melalui email",
                        target = email,
                        demoCode = signup.devCode ?: DEMO_EMAIL_OTP,
                        onBack = { nav.popBackStack() },
                        onVerifyCode = { enteredCode ->
                            val ticket = env.repo.verifyEmailCode(email, enteredCode, "RESET_PASSWORD")
                            signup.emailTicket = ticket
                            true
                        },
                        onResend = {
                            val res = env.repo.requestEmailCode(email, "RESET_PASSWORD")
                            signup.devCode = res.devCode
                        },
                    ) {
                        nav.navigate("forgot/new/$email/$fromPin") {
                            popUpTo("forgot/otp/{email}/{fromPin}") { inclusive = true }
                        }
                    }
                }
                composable(
                    "forgot/new/{email}/{fromPin}",
                    arguments = listOf(
                        navArgument("email") { type = NavType.StringType },
                        navArgument("fromPin") { type = NavType.BoolType; defaultValue = false },
                    ),
                ) { entry ->
                    val email = entry.arguments?.getString("email") ?: MockRepository.GOLDIN_DEMO_EMAIL
                    val fromPin = entry.arguments?.getBoolean("fromPin") ?: false
                    ForgotPasswordNewScreen(
                        env = env,
                        email = email,
                        onBack = { nav.popBackStack() },
                        onSaved = {
                            authToastMessage = "Kata sandi berhasil diubah"
                            if (fromPin) {
                                nav.popBackStack("forgot/pin/password", inclusive = false)
                            } else {
                                nav.popBackStack("login", inclusive = false)
                            }
                        },
                    )
                }
                composable("forgot/pin/password") {
                    ForgotPinPasswordScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onForgotPassword = { nav.navigate("forgot/true") },
                        onVerified = { email -> nav.navigate("forgot/pin/otp/$email") },
                        toastMessage = authToastMessage,
                        onToastDismissed = { authToastMessage = null },
                    )
                }
                composable("forgot/pin/otp/{email}") { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email") ?: (repo.state.value.user?.email ?: "reiuser@gmail.com")
                    ForgotPinOtpScreen(
                        email = email,
                        onBack = { nav.popBackStack() },
                        onVerified = {
                            nav.navigate("forgot/pin/new") {
                                popUpTo("forgot/pin/password") { inclusive = true }
                            }
                        },
                    )
                }
                composable("forgot/pin/new") {
                    ForgotPinNewScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onPinSaved = {
                            autoOpenBiometricOnPin = false
                            pinToastMessage = "PIN berhasil diubah"
                            nav.popBackStack("login/pin", inclusive = false)
                        },
                    )
                }
                composable("register/email") {
                    RegisterEmailScreen(
                        signup = signup,
                        env = env,
                        onBack = { nav.popBackStack() },
                        onSent = { nav.navigate("register/otp") },
                        onGoogle = { nav.navigate("google/reg") },
                        onLogin = {
                            nav.navigate("login") {
                                popUpTo("welcome") { inclusive = false }
                            }
                        },
                    )
                }
                composable(
                    "google/{mode}",
                    arguments = listOf(navArgument("mode") { type = NavType.StringType; defaultValue = "login" }),
                ) { entry ->
                    val mode = entry.arguments?.getString("mode") ?: "login"
                    GoogleAccountScreen(
                        onBack = { nav.popBackStack() },
                        onSelected = { email, name ->
                            if (mode == "reg") {
                                signup.reset()
                                signup.email = email
                                signup.name = name
                                signup.viaGoogle = true
                            } else {
                                repo.loginWithGoogle()
                            }
                            nav.navigate("google/loading/$mode") {
                                popUpTo("google/{mode}") { inclusive = true }
                            }
                        },
                    )
                }
                composable(
                    "google/loading/{mode}",
                    arguments = listOf(navArgument("mode") { type = NavType.StringType; defaultValue = "login" }),
                ) { entry ->
                    val mode = entry.arguments?.getString("mode") ?: "login"
                    GoogleReturnLoadingScreen(
                        onFinished = {
                            if (mode == "reg") {
                                nav.navigate("register/profile") {
                                    popUpTo("google/loading/{mode}") { inclusive = true }
                                }
                            } else {
                                nav.navigate("login/pin") {
                                    popUpTo("google/loading/{mode}") { inclusive = true }
                                }
                            }
                        },
                    )
                }
                composable("register/otp") {
                    OtpScreen(
                        title = "Verifikasi OTP",
                        step = 2 to 3,
                        sentVia = "Masukkan kode verifikasi yang dikirimkan melalui email",
                        target = signup.email,
                        demoCode = signup.devCode ?: DEMO_EMAIL_OTP,
                        stepLabels = listOf("Buat akun", "Verifikasi", "Data diri"),
                        onBack = { nav.popBackStack() },
                        onVerifyCode = { enteredCode ->
                            val ticket = env.repo.verifyEmailCode(signup.email, enteredCode, "REGISTER")
                            signup.emailTicket = ticket
                            true
                        },
                        onResend = {
                            val res = env.repo.requestEmailCode(signup.email, "REGISTER")
                            signup.devCode = res.devCode
                        },
                    ) {
                        nav.navigate("register/profile") { popUpTo("register/email") }
                    }
                }
                composable("register/profile") {
                    RegisterProfileScreen(
                        env,
                        signup,
                        onBack = { nav.popBackStack() },
                        onDone = { nav.navigate("register/done") { popUpTo("welcome") } },
                    )
                }
                composable("register/done") {
                    RegisterDoneScreen(env, signup, onStart = ::enterMain)
                }

                composable("kyc/consent") {
                    KycConsentScreen(
                        onBack = { nav.popBackStack() },
                        onNext = { nav.navigate("kyc/phone") },
                    )
                }
                composable("kyc/phone") {
                    KycPhoneScreen(
                        kyc = kyc,
                        onBack = { nav.popBackStack() },
                        onExit = ::exitKycFlow,
                        onNext = { nav.navigate("kyc/otp") },
                    )
                }
                composable("kyc/otp") {
                    KycPhoneOtpScreen(
                        kyc = kyc,
                        onBack = { nav.popBackStack() },
                        onExit = ::exitKycFlow,
                        onVerified = {
                            nav.navigate("kyc/ktp") { popUpTo("kyc/phone") }
                        },
                    )
                }
                composable("kyc/ktp") {
                    KycKtpIntroScreen(
                        onBack = { nav.popBackStack() },
                        onExit = ::exitKycFlow,
                        onNext = { nav.navigate("kyc/cam") },
                    )
                }
                composable("kyc/cam") {
                    KycScanIdScreen(
                        kyc = kyc,
                        onBack = { nav.popBackStack() },
                        onCaptured = { nav.navigate("kyc/data") },
                    )
                }
                composable("kyc/data") {
                    KycDataScreen(
                        signup = signup,
                        kyc = kyc,
                        onBack = { nav.popBackStack("kyc/ktp", inclusive = false) },
                        onRetakeKtp = { nav.popBackStack() },
                        onExit = ::exitKycFlow,
                        onNext = { nav.navigate("kyc/addr") },
                    )
                }
                composable("kyc/addr") {
                    KycAddressScreen(
                        kyc = kyc,
                        onBack = { nav.popBackStack() },
                        onExit = ::exitKycFlow,
                        onNext = { nav.navigate("kyc/info") },
                    )
                }
                composable("kyc/info") {
                    KycInfoScreen(
                        kyc = kyc,
                        onBack = { nav.popBackStack() },
                        onExit = ::exitKycFlow,
                        onNext = { nav.navigate("kyc/face") },
                    )
                }
                composable("kyc/face") {
                    KycFaceIntroScreen(
                        onBack = { nav.popBackStack() },
                        onExit = ::exitKycFlow,
                        onStart = { nav.navigate("kyc/face-cam") },
                    )
                }
                composable("kyc/face-cam") {
                    KycFaceScanScreen(
                        onBack = { nav.popBackStack() },
                        onVerified = { nav.navigate("kyc/pin") { popUpTo("kyc/face") } },
                    )
                }
                composable("kyc/pin") {
                    KycPinScreen(
                        env = env,
                        kyc = kyc,
                        onBack = { nav.popBackStack() },
                        onExit = ::exitKycFlow,
                        onDone = { nav.navigate("kyc/done") { popUpTo("home") } },
                    )
                }
                composable("kyc/done") {
                    KycVerifiedScreen(onDone = ::enterMain)
                }

                composable("home") {
                    HomeScreen(
                        env = env,
                        onBuy = { nav.switchTab("catalog") },
                        onSell = { nav.switchTab("sell-tab") },
                        onPrint = { requireVerifiedTab("print-tab") },
                        onInstallment = { openInstallmentFlow() },
                        onWithdraw = { requireVerifiedTab("wallet") },
                        onVerify = {
                            if (state.user?.isKycPending == true) kycGateSheet = "wait" else startKycFlow()
                        },
                        onPrices = { nav.navigate("prices") },
                        onNotifications = { nav.navigate("notif") },
                        tradePrefill = tradePrefill,
                        routinePlanPrefill = routinePlanPrefill,
                        onOpenRoutinePlans = { openRoutinePlansFlow() },
                        onOpenRoutinePlanDetail = { planId -> nav.navigate("plan-detail/$planId") },
                        onCreateRoutinePlan = { openCreateRoutinePlan() },
                    )
                }
                composable("notif") {
                    NotificationScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onOpenTransaction = { txId ->
                            if (txId != null) {
                                val tx = state.transactions.find { it.id == txId }
                                when {
                                    tx?.type == com.reidig.shared.TxType.TOPUP && tx.status == "pending" && tx.method == "QRIS" -> {
                                        nav.navigate("topup/qris/pay/$txId")
                                    }
                                    tx?.status == "pending" -> {
                                        nav.navigate("trade/pay/$txId")
                                    }
                                    else -> {
                                        nav.navigate("trade/receipt/$txId")
                                    }
                                }
                            }
                        },
                    )
                }
                composable("prices") { PriceTableScreen(env) { nav.popBackStack() } }
                composable("catalog") {
                    TradeScreen(
                        env = env,
                        initialMode = "beli",
                        tradePrefill = tradePrefill,
                        onOpenLedger = { nav.navigate("ledger") },
                        onOpenPendingTx = { txId -> nav.navigate("trade/pay/$txId") },
                        onCheckout = { isBuy, quoteId -> nav.navigate("trade/checkout/$quoteId") },
                        onStartKyc = ::startKycFlow,
                        onPendingKycDrawer = { kycGateSheet = "wait" },
                    )
                }
                composable("buy/{sku}", listOf(navArgument("sku") { type = NavType.StringType })) { entry ->
                    BuyScreen(env, entry.arguments!!.getString("sku")!!, { nav.popBackStack() }) { minted ->
                        results.minted = minted
                        nav.navigate("buy-success") { popUpTo("catalog") }
                    }
                }
                composable("buy-success") {
                    SuccessScreen(
                        "Pembelian berhasil!",
                        "Emasmu sudah tersimpan di brankas dan tercatat dalam portofolio.",
                        "Lihat portofolio",
                        { nav.switchTab("wallet") },
                    ) {
                        results.minted.forEach { token ->
                            SurfaceCard(Modifier.padding(bottom = 10.dp)) {
                                SummaryRow("Keping emas", "${state.sku(token.skuCode).product} ${gram(state.sku(token.skuCode).weightMg)}", strong = true)
                                SummaryRow("Nomor seri", token.serial)
                                SummaryRow("Kode token", token.code)
                            }
                        }
                    }
                }
                composable("portfolio") {
                    PortfolioScreen(
                        env = env,
                        onBuyGold = { nav.switchTab("catalog") },
                        onRoutinePlans = { openRoutinePlansFlow() },
                        onInstallmentList = { openInstallmentFlow() },
                        onTopup = { requireVerified("topup") },
                        onWithdraw = { requireVerified("withdraw") },
                        onAddBank = { requireVerified("bank-add") },
                        onOpenLedger = { nav.navigate("ledger") },
                        onOpenReceipt = { txId -> nav.navigate("trade/receipt/$txId") },
                        onStartKyc = {
                            if (state.user?.isKycPending == true) kycGateSheet = "wait" else startKycFlow()
                        },
                    )
                }
                composable("token/{code}", listOf(navArgument("code") { type = NavType.StringType })) { entry ->
                    TokenDetailScreen(
                        env,
                        entry.arguments!!.getString("code")!!,
                        onBack = { nav.popBackStack() },
                        onSold = { nav.switchTab("wallet") },
                        onPrint = { nav.navigate("print?token=$it") },
                    )
                }
                composable("sell-tab") {
                    TradeScreen(
                        env = env,
                        initialMode = "jual",
                        tradePrefill = tradePrefill,
                        onOpenLedger = { nav.navigate("ledger") },
                        onOpenPendingTx = { txId -> nav.navigate("trade/pay/$txId") },
                        onCheckout = { isBuy, quoteId -> nav.navigate("trade/checkout/$quoteId") },
                        onStartKyc = ::startKycFlow,
                        onPendingKycDrawer = { kycGateSheet = "wait" },
                    )
                }
                composable(
                    "trade/checkout/{quoteId}",
                    arguments = listOf(navArgument("quoteId") { type = NavType.StringType }),
                ) { entry ->
                    val quoteId = entry.arguments?.getString("quoteId").orEmpty()
                    TradeCheckoutScreen(
                        env = env,
                        quoteId = quoteId,
                        onBack = { nav.popBackStack() },
                        onPayWait = { txId ->
                            nav.navigate("trade/pay/$txId") {
                                popUpTo("catalog")
                            }
                        },
                        onDone = { txId ->
                            nav.navigate("trade/done/$txId") {
                                popUpTo("catalog")
                            }
                        },
                        onInstallmentSim = { grams ->
                            openInstallmentFlow(grams)
                        },
                    )
                }
                composable(
                    "trade/pay/{txId}",
                    arguments = listOf(navArgument("txId") { type = NavType.StringType }),
                ) { entry ->
                    val txId = entry.arguments?.getString("txId").orEmpty()
                    TradePayScreen(
                        env = env,
                        txId = txId,
                        onBack = { nav.popBackStack() },
                        onDone = { completedTxId ->
                            val currentOrders = repo.state.value.physicalOrders
                            val matchingOrder = currentOrders.firstOrNull {
                                it.txId == completedTxId || it.id == completedTxId || it.orderNumber == completedTxId
                            }
                            if (matchingOrder != null) {
                                cetakFlow.orderId = matchingOrder.id
                                nav.navigate("cetak/ticket/${matchingOrder.id}") {
                                    popUpTo("trade/pay/{txId}") { inclusive = true }
                                }
                            } else {
                                nav.navigate("trade/done/$completedTxId") {
                                    popUpTo("trade/pay/{txId}") { inclusive = true }
                                }
                            }
                        },
                        onCancelOrder = { nav.popBackStack() },
                    )
                }
                composable(
                    "trade/done/{txId}",
                    arguments = listOf(navArgument("txId") { type = NavType.StringType }),
                ) { entry ->
                    val txId = entry.arguments?.getString("txId").orEmpty()
                    TradeDoneScreen(
                        env = env,
                        txId = txId,
                        onHome = { nav.switchTab("home") },
                        onPortfolio = { nav.switchTab("wallet") },
                        onReceipt = { nav.navigate("trade/receipt/$txId") },
                    )
                }
                composable("ledger") {
                    LedgerScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onOpenReceipt = { txId -> nav.navigate("trade/receipt/$txId") },
                        onOpenPendingPay = { txId -> nav.navigate("trade/pay/$txId") },
                    )
                }
                composable(
                    "trade/receipt/{txId}",
                    arguments = listOf(navArgument("txId") { type = NavType.StringType }),
                ) { entry ->
                    val txId = entry.arguments?.getString("txId").orEmpty()
                    ReceiptScreen(
                        env = env,
                        txId = txId,
                        onBack = { nav.popBackStack() },
                    )
                }
                composable(
                    "receipt/{txId}",
                    arguments = listOf(navArgument("txId") { type = NavType.StringType }),
                ) { entry ->
                    val txId = entry.arguments?.getString("txId").orEmpty()
                    ReceiptScreen(
                        env = env,
                        txId = txId,
                        onBack = { nav.popBackStack() },
                    )
                }
                composable("print-tab") {
                    CetakTabScreen(
                        env = env,
                        onStartCetak = { branchId -> openCetakFlow(branchId) },
                        onOpenBranchCatalog = { nav.navigate("cetak/branches") },
                        onOpenTicket = { orderId -> nav.navigate("cetak/ticket/$orderId") },
                        onStartKyc = {
                            if (state.user?.isKycPending == true) kycGateSheet = "wait" else startKycFlow()
                        },
                    )
                }
                composable("cetak") {
                    CetakTabScreen(
                        env = env,
                        onStartCetak = { branchId -> openCetakFlow(branchId) },
                        onOpenBranchCatalog = { nav.navigate("cetak/branches") },
                        onOpenTicket = { orderId -> nav.navigate("cetak/ticket/$orderId") },
                        onStartKyc = {
                            if (state.user?.isKycPending == true) kycGateSheet = "wait" else startKycFlow()
                        },
                    )
                }
                composable(
                    "cetak/brand?branch={branch}",
                    listOf(navArgument("branch") { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) { entry ->
                    val branchParam = entry.arguments?.getString("branch")
                    if (branchParam != null) {
                        cetakFlow.branchId = branchParam
                    }
                    CetakBrandSizeScreen(
                        env = env,
                        onBack = {
                            if (!nav.popBackStack()) {
                                nav.navigate("print-tab")
                            }
                        },
                        onNext = { brand, size ->
                            cetakFlow.brand = brand
                            cetakFlow.sizeGrams = size
                            nav.navigate("cetak/method")
                        },
                        initialBrand = cetakFlow.brand,
                        initialSize = cetakFlow.sizeGrams,
                    )
                }
                composable("cetak/brand") {
                    CetakBrandSizeScreen(
                        env = env,
                        onBack = {
                            if (!nav.popBackStack()) {
                                nav.navigate("print-tab")
                            }
                        },
                        onNext = { brand, size ->
                            cetakFlow.brand = brand
                            cetakFlow.sizeGrams = size
                            nav.navigate("cetak/method")
                        },
                        initialBrand = cetakFlow.brand,
                        initialSize = cetakFlow.sizeGrams,
                    )
                }
                composable("cetak/method") {
                    CetakMethodScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onNext = { method, branchId, addressId ->
                            cetakFlow.method = method
                            cetakFlow.branchId = branchId
                            val addr = state.savedAddresses.firstOrNull { it.id == addressId }
                            cetakFlow.deliveryAddress = addr?.let {
                                "${it.details}, ${it.subDistrict}, ${it.district}, ${it.city}, ${it.province} ${it.postalCode}"
                            }
                            cetakFlow.recipientName = addr?.recipientName
                            cetakFlow.recipientPhone = addr?.phoneNumber
                            nav.navigate("cetak/confirm")
                        },
                        onOpenAllBranches = {
                            nav.navigate("cetak/branches")
                        },
                        initialMethod = cetakFlow.method,
                        initialBranchId = cetakFlow.branchId,
                        initialAddressId = state.savedAddresses.firstOrNull {
                            it.recipientName == cetakFlow.recipientName
                        }?.id,
                    )
                }
                composable("cetak/address/add") {
                    CetakAddressFormScreen(
                        onBack = { nav.popBackStack() },
                        onSave = { newAddress ->
                            repo.addSavedAddress(newAddress)
                            nav.popBackStack()
                        },
                    )
                }
                composable("addrForm") {
                    CetakAddressFormScreen(
                        onBack = { nav.popBackStack() },
                        onSave = { newAddress ->
                            repo.addSavedAddress(newAddress)
                            nav.popBackStack()
                        },
                    )
                }
                composable("cetak/confirm") {
                    val currentSize = cetakFlow.sizeGrams ?: 1.0
                    val currentAddr = state.savedAddresses.firstOrNull {
                        it.recipientName == cetakFlow.recipientName
                    }
                    CetakReviewScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onSuccess = { orderId, txId, paymentMethod ->
                            cetakFlow.orderId = orderId
                            while (nav.currentBackStackEntry?.destination?.route?.startsWith("cetak/") == true &&
                                   nav.currentBackStackEntry?.destination?.route != "cetak") {
                                nav.popBackStack()
                            }
                            if (paymentMethod == PaymentMethod.WALLET) {
                                nav.navigate("cetak/ticket/$orderId")
                            } else {
                                nav.navigate("trade/pay/$txId")
                            }
                        },
                        brand = cetakFlow.brand,
                        sizeGrams = currentSize,
                        method = cetakFlow.method,
                        branchId = cetakFlow.branchId,
                        addressId = currentAddr?.id,
                        initialPaymentMethod = cetakFlow.paymentMethod,
                    )
                }
                composable("cetak/branches") {
                    CetakBranchScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onSelectBranch = { branchId ->
                            cetakFlow.branchId = branchId
                            nav.popBackStack()
                        },
                        selectedBranchId = cetakFlow.branchId,
                    )
                }
                composable(
                    "cetak/ticket/{orderId}",
                    arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
                ) { entry ->
                    val orderId = entry.arguments?.getString("orderId").orEmpty()
                    CetakTicketScreen(
                        env = env,
                        orderId = orderId,
                        onBack = {
                            if (!nav.popBackStack("print-tab", inclusive = false)) {
                                if (!nav.popBackStack("cetak", inclusive = false)) {
                                    nav.navigate("home")
                                }
                            }
                        },
                        onOpenReceipt = { txId ->
                            nav.navigate("trade/receipt/$txId")
                        },
                    )
                }
                composable("print?token={token}", listOf(navArgument("token") { type = NavType.StringType; defaultValue = "" })) { entry ->
                    PrintScreen(env, entry.arguments?.getString("token")?.ifEmpty { null }, { nav.popBackStack() }) { order ->
                        results.printOrder = order
                        nav.navigate("print-success") { popUpTo("home") }
                    }
                }
                composable("print-success") {
                    val order = results.printOrder
                    SuccessScreen(
                        "Pesanan cetak dibuat",
                        if (order?.method == PrintMethod.BRANCH) {
                            "Keping dikirim dulu dari gudang Jakarta. Kami kabari saat bisa diambil. Bawa KTP dan kode pengambilan."
                        } else {
                            "Keping sedang disiapkan untuk dikirim dengan JNE."
                        },
                        "Lihat portofolio",
                        { nav.switchTab("wallet") },
                    ) {
                        order?.let { PrintOrderCard(it) }
                    }
                }
                composable("wallet") {
                    PortfolioScreen(
                        env = env,
                        onBuyGold = { nav.switchTab("catalog") },
                        onRoutinePlans = { openRoutinePlansFlow() },
                        onInstallmentList = { openInstallmentFlow() },
                        onTopup = { requireVerified("topup") },
                        onWithdraw = { requireVerified("withdraw") },
                        onAddBank = { requireVerified("bank-add") },
                        onOpenLedger = { nav.navigate("ledger") },
                        onOpenReceipt = { txId -> nav.navigate("trade/receipt/$txId") },
                        onStartKyc = {
                            if (state.user?.isKycPending == true) kycGateSheet = "wait" else startKycFlow()
                        },
                    )
                }
                composable("bank-add") {
                    BankAddScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onAccountAdded = { nav.popBackStack() },
                    )
                }
                composable("bank/add") {
                    BankAddScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onAccountAdded = { nav.popBackStack() },
                    )
                }
                composable("topup") {
                    TopupMethodScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onSelectVa = { bankKey -> nav.navigate("topup/va/$bankKey") },
                        onSelectQris = { nav.navigate("topup/qris") },
                    )
                }
                composable(
                    "topup/va/{bankKey}",
                    arguments = listOf(navArgument("bankKey") { type = NavType.StringType }),
                ) { entry ->
                    val bankKey = entry.arguments?.getString("bankKey").orEmpty()
                    TopupVaScreen(
                        env = env,
                        bankKey = bankKey,
                        onBack = { nav.popBackStack() },
                        onTopupSuccess = { txId ->
                            nav.navigate("topup/done/$txId") {
                                popUpTo("wallet")
                            }
                        },
                    )
                }
                composable("topup/qris") {
                    TopupQrisScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onQrCreated = { txId -> nav.navigate("topup/qris/pay/$txId") },
                    )
                }
                composable(
                    "topup/qris/pay/{txId}",
                    arguments = listOf(navArgument("txId") { type = NavType.StringType }),
                ) { entry ->
                    val txId = entry.arguments?.getString("txId").orEmpty()
                    TopupQrPayScreen(
                        env = env,
                        txId = txId,
                        onBack = { nav.popBackStack("wallet", inclusive = false) },
                        onPaySuccess = { completedId ->
                            nav.navigate("topup/done/$completedId") {
                                popUpTo("wallet")
                            }
                        },
                    )
                }
                composable(
                    "topup/done/{txId}",
                    arguments = listOf(navArgument("txId") { type = NavType.StringType }),
                ) { entry ->
                    val txId = entry.arguments?.getString("txId").orEmpty()
                    TopupDoneScreen(
                        env = env,
                        txId = txId,
                        onReceipt = { id -> nav.navigate("trade/receipt/$id") },
                        onPortfolio = { nav.switchTab("wallet") },
                    )
                }
                composable("withdraw") {
                    WithdrawScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onAddBank = { requireVerified("bank-add") },
                        onReview = { accId, amount ->
                            nav.navigate("withdraw/review/$accId/$amount")
                        },
                    )
                }
                composable(
                    "withdraw/review/{accId}/{amount}",
                    arguments = listOf(
                        navArgument("accId") { type = NavType.StringType },
                        navArgument("amount") { type = NavType.LongType },
                    ),
                ) { entry ->
                    val accId = entry.arguments?.getString("accId").orEmpty()
                    val amount = entry.arguments?.getLong("amount") ?: 0L
                    WithdrawReviewScreen(
                        env = env,
                        bankAccountId = accId,
                        amount = amount,
                        onBack = { nav.popBackStack() },
                        onSuccess = { txId ->
                            nav.navigate("withdraw/done/$txId") {
                                popUpTo("wallet")
                            }
                        },
                    )
                }
                composable(
                    "withdraw/done/{txId}",
                    arguments = listOf(
                        navArgument("txId") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val txId = entry.arguments?.getString("txId").orEmpty()
                    WithdrawDoneScreen(
                        env = env,
                        txId = txId,
                        onReceipt = { id -> nav.navigate("trade/receipt/$id") },
                        onPortfolio = { nav.switchTab("wallet") },
                    )
                }
                // --- TABUNGAN RUTIN (TAHAP 7) ---
                composable("plans") {
                    RoutinePlanListScreen(
                        env = env,
                        onBack = {
                            if (!nav.popBackStack()) {
                                nav.popBackStack("home", inclusive = false)
                            }
                        },
                        onOpenPlanDetail = { planId ->
                            nav.navigate("plan-detail/$planId")
                        },
                        onCreatePlan = {
                            openCreateRoutinePlan()
                        },
                    )
                }
                composable("plan-create") {
                    RoutinePlanFormScreen(
                        env = env,
                        onBack = {
                            if (!nav.popBackStack()) {
                                nav.popBackStack("home", inclusive = false)
                            }
                        },
                        onPlanCreated = { planId ->
                            nav.navigate("plan-done/$planId") {
                                popUpTo("home") { inclusive = false }
                            }
                        },
                        onAddBank = { requireVerified("bank-add") },
                        prefillDraft = routinePlanPrefill,
                    )
                }
                composable(
                    "plan-done/{planId}",
                    arguments = listOf(navArgument("planId") { type = NavType.StringType }),
                ) { entry ->
                    val planId = entry.arguments?.getString("planId").orEmpty()
                    RoutinePlanDoneScreen(
                        env = env,
                        planId = planId,
                        onSeePlan = { id ->
                            nav.navigate("plan-detail/$id") {
                                popUpTo("plan-done/{planId}") { inclusive = true }
                            }
                        },
                    )
                }
                composable(
                    "plan-detail/{planId}",
                    arguments = listOf(navArgument("planId") { type = NavType.StringType }),
                ) { entry ->
                    val planId = entry.arguments?.getString("planId").orEmpty()
                    RoutinePlanDetailScreen(
                        env = env,
                        planId = planId,
                        onBack = {
                            if (!nav.popBackStack()) {
                                nav.popBackStack("home", inclusive = false)
                            }
                        },
                        onOpenReceipt = { txId ->
                            nav.navigate("trade/receipt/$txId")
                        },
                    )
                }

                // --- CICIL EMAS (TAHAP 7) ---
                composable("cicil") {
                    LaunchedEffect(Unit) {
                        if (state.installmentContracts.isNotEmpty()) {
                            nav.navigate("cicil/list") { popUpTo("cicil") { inclusive = true } }
                        } else {
                            installmentFlow.reset(1.0)
                            nav.navigate("cicil/sim") { popUpTo("cicil") { inclusive = true } }
                        }
                    }
                    Box(Modifier.fillMaxSize().background(Palette.Paper), contentAlignment = Alignment.Center) {
                        GoldinSpinner()
                    }
                }
                composable("installment") {
                    LaunchedEffect(Unit) {
                        if (state.installmentContracts.isNotEmpty()) {
                            nav.navigate("cicil/list") { popUpTo("installment") { inclusive = true } }
                        } else {
                            installmentFlow.reset(1.0)
                            nav.navigate("cicil/sim") { popUpTo("installment") { inclusive = true } }
                        }
                    }
                    Box(Modifier.fillMaxSize().background(Palette.Paper), contentAlignment = Alignment.Center) {
                        GoldinSpinner()
                    }
                }
                composable("cicil/list") {
                    InstallmentListScreen(
                        env = env,
                        onBack = {
                            if (!nav.popBackStack()) {
                                nav.popBackStack("home", inclusive = false)
                            }
                        },
                        onOpenDetail = { contractId ->
                            nav.navigate("cicil/detail/$contractId")
                        },
                        onCreateInstallment = {
                            installmentFlow.reset(1.0)
                            nav.navigate("cicil/sim")
                        },
                    )
                }
                composable("cicil/sim") {
                    InstallmentSimScreen(
                        env = env,
                        onBack = {
                            if (!nav.popBackStack()) {
                                nav.popBackStack("home", inclusive = false)
                            }
                        },
                        onNext = { grams, tenor, dp ->
                            installmentFlow.grams = grams
                            installmentFlow.tenorMonths = tenor
                            installmentFlow.dpPercent = dp
                            nav.navigate("cicil/method")
                        },
                        initialGrams = installmentFlow.grams,
                    )
                }
                composable("cicil/method") {
                    InstallmentPaymentMethodScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onNext = { mode, source ->
                            installmentFlow.paymentMode = mode
                            installmentFlow.fundingSource = source
                            nav.navigate("cicil/confirm")
                        },
                        onAddBank = { requireVerified("bank-add") },
                    )
                }
                composable("cicil/confirm") {
                    InstallmentConfirmScreen(
                        env = env,
                        grams = installmentFlow.grams,
                        tenorMonths = installmentFlow.tenorMonths,
                        dpPercent = installmentFlow.dpPercent,
                        paymentMode = installmentFlow.paymentMode,
                        fundingSource = installmentFlow.fundingSource,
                        onBack = { nav.popBackStack() },
                        onSubmit = { quoteId ->
                            installmentFlow.quoteId = quoteId
                            nav.navigate("cicil/check/$quoteId") {
                                popUpTo("cicil/sim") { inclusive = false }
                            }
                        },
                    )
                }
                composable(
                    "cicil/check/{quoteId}",
                    arguments = listOf(navArgument("quoteId") { type = NavType.StringType }),
                ) { entry ->
                    val quoteId = entry.arguments?.getString("quoteId") ?: installmentFlow.quoteId
                    InstallmentScoringScreen(
                        env = env,
                        quoteId = quoteId,
                        onApproved = { approvedQuoteId ->
                            nav.navigate("cicil/akad/$approvedQuoteId") {
                                popUpTo("cicil/check/{quoteId}") { inclusive = true }
                            }
                        },
                        onUbahSimulasi = {
                            nav.popBackStack("cicil/sim", inclusive = false)
                        },
                        onExit = {
                            nav.popBackStack("home", inclusive = false)
                        },
                    )
                }
                composable(
                    "cicil/akad/{quoteId}",
                    arguments = listOf(navArgument("quoteId") { type = NavType.StringType }),
                ) { entry ->
                    val quoteId = entry.arguments?.getString("quoteId") ?: installmentFlow.quoteId
                    InstallmentAkadScreen(
                        env = env,
                        quoteId = quoteId,
                        onBack = {
                            if (!nav.popBackStack()) {
                                nav.popBackStack("home", inclusive = false)
                            }
                        },
                        onSuccess = { contractId ->
                            installmentFlow.contractId = contractId
                            nav.navigate("cicil/success/$contractId") {
                                popUpTo("home") { inclusive = false }
                            }
                        },
                    )
                }
                composable(
                    "cicil/success/{contractId}",
                    arguments = listOf(navArgument("contractId") { type = NavType.StringType }),
                ) { entry ->
                    val contractId = entry.arguments?.getString("contractId") ?: installmentFlow.contractId
                    InstallmentSuccessScreen(
                        env = env,
                        contractId = contractId,
                        onSeeContract = { id ->
                            nav.navigate("cicil/detail/$id") {
                                popUpTo("cicil/success/{contractId}") { inclusive = true }
                            }
                        },
                    )
                }
                composable(
                    "cicil/detail/{contractId}",
                    arguments = listOf(navArgument("contractId") { type = NavType.StringType }),
                ) { entry ->
                    val contractId = entry.arguments?.getString("contractId") ?: ""
                    InstallmentDetailScreen(
                        env = env,
                        contractId = contractId,
                        onBack = {
                            if (!nav.popBackStack()) {
                                nav.popBackStack("home", inclusive = false)
                            }
                        },
                    )
                }
                composable("account") {
                    AccountScreen(
                        env,
                        onVerify = {
                            if (state.user?.isKycPending == true) kycGateSheet = "wait" else startKycFlow()
                        },
                        onInstallments = { openInstallmentFlow() },
                        onLogout = {
                            repo.logout()
                            nav.navigate("welcome") { popUpTo(0) { inclusive = true } }
                        },
                    )
                }
            }
            activeTab(route)?.let { active ->
                ReidigBottomBar(active) { nav.switchTab(it) }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 84.dp).fillMaxWidth(),
        ) { data ->
            Snackbar(data, containerColor = Palette.Ink, contentColor = Color.White, modifier = Modifier.padding(horizontal = 16.dp))
        }
        when (kycGateSheet) {
            "need" -> VerificationGateDrawer(
                onDismiss = { kycGateSheet = null },
                onVerifyNow = {
                    kycGateSheet = null
                    startKycFlow()
                },
            )
            "wait" -> PendingVerificationDrawer(
                onDismiss = { kycGateSheet = null },
            )
        }
    }
}

@Composable
private fun WalletSwitch(selected: Int, onSwitch: () -> Unit) {
    SegmentSwitch(listOf("Dompet Kas & Emas", "Keping Token"), selected) { onSwitch() }
}

@Composable
private fun TransactionSwitch(selected: Int, inset: Dp = 20.dp, onSwitch: () -> Unit) {
    SegmentSwitch(listOf("Beli emas", "Jual emas"), selected, inset) { onSwitch() }
}

private class InstallmentFlowDraft {
    var grams by mutableStateOf(1.0)
    var tenorMonths by mutableIntStateOf(12)
    var dpPercent by mutableIntStateOf(20)
    var paymentMode by mutableStateOf("auto_debit")
    var fundingSource by mutableStateOf("bank_bsi")
    var quoteId by mutableStateOf("")
    var contractId by mutableStateOf("")

    fun reset(initialGrams: Double = 1.0) {
        grams = initialGrams.coerceAtLeast(0.5)
        tenorMonths = 12
        dpPercent = 20
        paymentMode = "auto_debit"
        fundingSource = "bank_bsi"
        quoteId = ""
        contractId = ""
    }
}

private class TransactionResults {
    var minted: List<GoldToken> = emptyList()
    var printOrder: PrintOrder? = null
    var installmentDraft: InstallmentDraft? = null
    var application: InstallmentApplication? = null
    var contract: InstallmentContract? = null
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo("home")
        launchSingleTop = true
    }
}
