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
import androidx.compose.runtime.getValue
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
                        demoCode = DEMO_EMAIL_OTP,
                        onBack = { nav.popBackStack() },
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
                            autoOpenBiometricOnPin = true
                            pinToastMessage = "PIN berhasil diubah"
                            nav.popBackStack("login/pin", inclusive = false)
                        },
                    )
                }
                composable("register/email") {
                    RegisterEmailScreen(
                        signup = signup,
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
                        demoCode = DEMO_EMAIL_OTP,
                        stepLabels = listOf("Buat akun", "Verifikasi", "Data diri"),
                        onBack = { nav.popBackStack() },
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
                        onExit = { nav.popBackStack("home", inclusive = false) },
                        onNext = { nav.navigate("kyc/otp") },
                    )
                }
                composable("kyc/otp") {
                    KycPhoneOtpScreen(
                        kyc = kyc,
                        onBack = { nav.popBackStack() },
                        onExit = { nav.popBackStack("home", inclusive = false) },
                        onVerified = {
                            nav.navigate("kyc/ktp") { popUpTo("kyc/phone") }
                        },
                    )
                }
                composable("kyc/ktp") {
                    KycKtpIntroScreen(
                        onBack = { nav.popBackStack() },
                        onExit = { nav.popBackStack("home", inclusive = false) },
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
                        onExit = { nav.popBackStack("home", inclusive = false) },
                        onNext = { nav.navigate("kyc/addr") },
                    )
                }
                composable("kyc/addr") {
                    KycAddressScreen(
                        kyc = kyc,
                        onBack = { nav.popBackStack() },
                        onExit = { nav.popBackStack("home", inclusive = false) },
                        onNext = { nav.navigate("kyc/info") },
                    )
                }
                composable("kyc/info") {
                    KycInfoScreen(
                        kyc = kyc,
                        onBack = { nav.popBackStack() },
                        onExit = { nav.popBackStack("home", inclusive = false) },
                        onNext = { nav.navigate("kyc/face") },
                    )
                }
                composable("kyc/face") {
                    KycFaceIntroScreen(
                        onBack = { nav.popBackStack() },
                        onExit = { nav.popBackStack("home", inclusive = false) },
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
                        onExit = { nav.popBackStack("home", inclusive = false) },
                        onDone = { nav.navigate("kyc/done") { popUpTo("home") } },
                    )
                }
                composable("kyc/done") {
                    KycVerifiedScreen(onDone = ::enterMain)
                }

                composable("home") {
                    HomeScreen(
                        env = env,
                        onBuy = { requireVerifiedTab("catalog") },
                        onSell = { requireVerifiedTab("sell-tab") },
                        onPrint = { requireVerifiedTab("print-tab") },
                        onInstallment = { requireVerified("installment") },
                        onWithdraw = { requireVerifiedTab("wallet") },
                        onVerify = {
                            if (state.user?.isKycPending == true) kycGateSheet = "wait" else startKycFlow()
                        },
                        onPrices = { nav.navigate("prices") },
                        onNotifications = { nav.navigate("notif") },
                        tradePrefill = tradePrefill,
                        routinePlanPrefill = routinePlanPrefill,
                        onOpenRoutinePlans = { requireVerifiedTab("wallet") },
                        onOpenRoutinePlanDetail = { _ -> requireVerifiedTab("wallet") },
                        onCreateRoutinePlan = { requireVerifiedTab("wallet") },
                    )
                }
                composable("notif") {
                    NotificationScreen(
                        env = env,
                        onBack = { nav.popBackStack() },
                        onOpenTransaction = { txId ->
                            // Hook ke Struk transaksi pada Tahap 5; sementara arahkan ke Portofolio/Mutasi
                            if (txId != null) {
                                nav.switchTab("wallet")
                            }
                        },
                    )
                }
                composable("prices") { PriceTableScreen(env) { nav.popBackStack() } }
                composable("catalog") {
                    CatalogScreen(env, header = { TransactionSwitch(0) { nav.switchTab("sell-tab") } }) { code -> requireVerified("buy/$code") }
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
                        { nav.switchTab("portfolio") },
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
                        env,
                        onToken = { nav.navigate("token/$it") },
                        onPrint = { nav.switchTab("print-tab") },
                        onBuy = { nav.switchTab("catalog") },
                        header = { WalletSwitch(1) { nav.switchTab("wallet") } },
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
                    SellPickerScreen(
                        env,
                        null,
                        { nav.navigate("token/$it") },
                        title = "Transaksi",
                        header = {
                            TransactionSwitch(1, inset = 0.dp) { nav.switchTab("catalog") }
                            Spacer(Modifier.height(14.dp))
                        },
                    )
                }
                composable("print-tab") {
                    PrintScreen(env, null, null) { order ->
                        results.printOrder = order
                        nav.navigate("print-success") { popUpTo("home") }
                    }
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
                        { nav.switchTab("portfolio") },
                    ) {
                        order?.let { PrintOrderCard(it) }
                    }
                }
                composable("wallet") {
                    WalletScreen(
                        env,
                        header = { WalletSwitch(0) { nav.switchTab("portfolio") } },
                        onPortfolio = { nav.switchTab("portfolio") },
                        onWithdraw = { nav.navigate("withdraw") },
                    )
                }
                composable("withdraw") {
                    WithdrawScreen(env, { nav.popBackStack() }) { nav.switchTab("wallet") }
                }
                composable("installment") {
                    InstallmentScreen(env, { nav.popBackStack() }) { draft ->
                        results.installmentDraft = draft
                        nav.navigate("installment-agreement")
                    }
                }
                composable("installment-agreement") {
                    results.installmentDraft?.let { draft ->
                        InstallmentAgreementScreen(env, draft, { nav.popBackStack() }) { application ->
                            results.application = application
                            nav.navigate("installment-pay") { popUpTo("home") }
                        }
                    }
                }
                composable("installment-pay") {
                    results.application?.let { application ->
                        DownPaymentScreen(env, application, onCancelled = { nav.popBackStack("home", false) }) { contract ->
                            results.contract = contract
                            nav.navigate("installment-success") { popUpTo("home") }
                        }
                    }
                }
                composable("installment-success") {
                    SuccessScreen(
                        "Uang muka diterima",
                        "Cicilan Anda aktif. Token terkunci sampai lunas.",
                        "Lihat kontrak",
                        { nav.switchTab("account") },
                    ) {
                        results.contract?.let { contract ->
                            SurfaceCard {
                                SummaryRow("Kontrak", contract.id)
                                SummaryRow("Uang muka", rupiah(contract.downPayment))
                                SummaryRow("Angsuran per bulan", rupiah(contract.monthlyBeforeFees), strong = true)
                                SummaryRow("Tenor", "${contract.tenorMonths} bulan")
                            }
                        }
                    }
                }
                composable("account") {
                    AccountScreen(
                        env,
                        onVerify = {
                            if (state.user?.isKycPending == true) kycGateSheet = "wait" else startKycFlow()
                        },
                        onInstallments = { requireVerified("installment") },
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
