package com.darchums.dalmiren.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.darchums.dalmiren.data.remote.NetworkClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PremiumPlanOption(
    val basePlanId: String,
    val billingPeriod: String,
    val formattedPrice: String,
    val trialPeriod: String?,
    val offerToken: String
) {
    val hasFreeTrial: Boolean
        get() = trialPeriod != null
}

data class PlayBillingState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val plans: List<PremiumPlanOption> = emptyList(),
    val tier: String = "FREE",
    val subscriptionStatus: String = "NONE",
    val message: String? = null
)

class PlayBillingManager(context: Context) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_ID = "dalmiren_plus"
        private val SUPPORTED_BASE_PLANS = listOf(
            "plus-weekly",
            "plus-monthly",
            "plus-annual-v1"
        )
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val mutableState = MutableStateFlow(PlayBillingState())
    val state: StateFlow<PlayBillingState> = mutableState.asStateFlow()

    private var productDetails: ProductDetails? = null
    private var accountBindingId: String? = null
    private var catalogPlanIds: Set<String> = emptySet()
    private var connectionInProgress = false

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun connect() {
        if (billingClient.isReady || connectionInProgress) return

        connectionInProgress = true

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connectionInProgress = false

                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    mutableState.value = mutableState.value.copy(
                        isConnected = true,
                        message = null
                    )
                    refresh()
                } else {
                    mutableState.value = mutableState.value.copy(
                        isConnected = false,
                        message = "Google Play Billing connection failed: ${result.debugMessage}"
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                connectionInProgress = false
                mutableState.value = mutableState.value.copy(
                    isConnected = false,
                    message = "Google Play disconnected. Reconnecting when needed."
                )
            }
        })
    }

    fun refresh() {
        scope.launch {
            mutableState.value = mutableState.value.copy(
                isLoading = true,
                message = null
            )

            try {
                refreshCatalog()

                try {
                    refreshEntitlements()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    accountBindingId = null
                    mutableState.value = mutableState.value.copy(
                        tier = "FREE",
                        subscriptionStatus = "UNAVAILABLE"
                    )
                }

                if (!billingClient.isReady) {
                    connect()
                    return@launch
                }

                loadProductDetails()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    message = "Unable to load subscription information. Please try again."
                )
            }
        }
    }

    private suspend fun refreshCatalog() {
        val response = NetworkClient.apiService.getSubscriptionCatalog()
        if (!response.isSuccessful) {
            throw IllegalStateException("Subscription catalog unavailable.")
        }

        val catalog = response.body()
            ?: throw IllegalStateException("Empty subscription catalog.")

        require(catalog.product_id == PRODUCT_ID) {
            "Unexpected subscription product."
        }

        catalogPlanIds = catalog.plans
            .map { it.base_plan_id }
            .filter { it in SUPPORTED_BASE_PLANS }
            .toSet()

        require(SUPPORTED_BASE_PLANS.all { it in catalogPlanIds }) {
            "One or more DALMIREN Plus subscription plans are missing from the catalogue."
        }
    }

    private suspend fun refreshEntitlements() {
        val response = NetworkClient.apiService.getSubscriptionEntitlements()
        if (!response.isSuccessful) {
            throw IllegalStateException("Subscription account unavailable.")
        }

        val entitlement = response.body()
            ?: throw IllegalStateException("Empty subscription account.")

        accountBindingId = entitlement.obfuscated_account_id.takeIf {
            it.isNotBlank()
        }

        mutableState.value = mutableState.value.copy(
            tier = entitlement.tier,
            subscriptionStatus = entitlement.status
        )
    }

    private suspend fun refreshBackendData() {
        refreshCatalog()
        refreshEntitlements()
    }

    private fun loadProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    message = "Unable to retrieve Google Play subscription offers: ${result.debugMessage}"
                )
                return@queryProductDetailsAsync
            }

            val details = queryResult.productDetailsList.firstOrNull {
                it.productId == PRODUCT_ID
            }

            if (details == null) {
                productDetails = null
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    plans = emptyList(),
                    message = "Premium offers are not currently available from Google Play."
                )
                return@queryProductDetailsAsync
            }

            productDetails = details

            val options = details.subscriptionOfferDetails
                .orEmpty()
                .filter { it.basePlanId in catalogPlanIds }
                .mapNotNull { offer ->
                    val phases = offer.pricingPhases.pricingPhaseList
                    val freePhase = phases.firstOrNull {
                        it.priceAmountMicros == 0L &&
                            it.billingPeriod.isNotBlank()
                    }
                    val paidPhase = phases.lastOrNull {
                        it.priceAmountMicros > 0L
                    } ?: phases.lastOrNull()

                    if (paidPhase == null) {
                        null
                    } else {
                        PremiumPlanOption(
                            basePlanId = offer.basePlanId,
                            billingPeriod = paidPhase.billingPeriod,
                            formattedPrice = paidPhase.formattedPrice,
                            trialPeriod = freePhase?.billingPeriod,
                            offerToken = offer.offerToken
                        )
                    }
                }
                .groupBy { it.basePlanId }
                .values
                .mapNotNull { offers ->
                    offers.maxByOrNull { if (it.hasFreeTrial) 1 else 0 }
                }
                .sortedBy { SUPPORTED_BASE_PLANS.indexOf(it.basePlanId) }

            mutableState.value = mutableState.value.copy(
                isLoading = false,
                isConnected = true,
                plans = options,
                message = if (options.isEmpty()) {
                    "No eligible subscription offers were returned by Google Play."
                } else {
                    null
                }
            )
        }
    }

    fun launchPurchase(activity: Activity, basePlanId: String) {
        if (!billingClient.isReady) {
            mutableState.value = mutableState.value.copy(
                message = "Google Play Billing is not connected. Please try again."
            )
            connect()
            return
        }

        val bindingId = accountBindingId
        if (bindingId.isNullOrBlank()) {
            mutableState.value = mutableState.value.copy(
                message = "Your account could not be securely linked to this purchase. Refresh and try again."
            )
            refresh()
            return
        }

        val details = productDetails
        val offer = mutableState.value.plans.firstOrNull {
            it.basePlanId == basePlanId
        }

        if (details == null || offer == null) {
            mutableState.value = mutableState.value.copy(
                message = "This subscription offer is no longer available. Refresh and try again."
            )
            refresh()
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offer.offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .setObfuscatedAccountId(bindingId)
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)

        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            mutableState.value = mutableState.value.copy(
                message = "Unable to start Google Play checkout: ${result.debugMessage}"
            )
        }
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            connect()
            mutableState.value = mutableState.value.copy(
                message = "Connecting to Google Play before restoring purchases."
            )
            return
        }

        mutableState.value = mutableState.value.copy(
            isLoading = true,
            message = null
        )

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    message = "Unable to retrieve Google Play purchases: ${result.debugMessage}"
                )
                return@queryPurchasesAsync
            }

            val matchingPurchases = purchases.filter {
                PRODUCT_ID in it.products
            }

            if (matchingPurchases.isEmpty()) {
                scope.launch {
                    try {
                        refreshBackendData()
                        mutableState.value = mutableState.value.copy(
                            isLoading = false,
                            message = "No active Google Play purchases were found."
                        )
                    } catch (_: Exception) {
                        mutableState.value = mutableState.value.copy(
                            isLoading = false,
                            message = "Unable to refresh your subscription account."
                        )
                    }
                }
            } else {
                matchingPurchases.forEach { purchase ->
                    processPurchase(purchase, isRestore = true)
                }
            }
        }
    }

    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty()
                    .filter { PRODUCT_ID in it.products }
                    .forEach { processPurchase(it, isRestore = false) }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                mutableState.value = mutableState.value.copy(
                    message = "Purchase cancelled. No payment was completed."
                )
            }

            else -> {
                mutableState.value = mutableState.value.copy(
                    message = "Google Play purchase error: ${result.debugMessage}"
                )
            }
        }
    }

    private fun processPurchase(
        purchase: Purchase,
        isRestore: Boolean
    ) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> {
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    message = "Your payment is pending. Premium will activate only after Google Play confirms payment."
                )
            }

            Purchase.PurchaseState.PURCHASED -> {
                scope.launch {
                    mutableState.value = mutableState.value.copy(
                        isLoading = true,
                        message = "Verifying your purchase securely..."
                    )

                    try {
                        val request = com.darchums.dalmiren.data.remote
                            .SubscriptionPurchaseApiRequest(purchase.purchaseToken)

                        val response = if (isRestore) {
                            NetworkClient.apiService.restoreSubscriptionPurchase(request)
                        } else {
                            NetworkClient.apiService.verifySubscriptionPurchase(request)
                        }

                        val verification = response.body()

                        if (response.isSuccessful && verification?.verified == true) {
                            refreshBackendData()

                            mutableState.value = mutableState.value.copy(
                                isLoading = false,
                                tier = verification.tier,
                                subscriptionStatus = verification.subscription_status,
                                message = if (verification.acknowledged) {
                                    "Your subscription has been verified successfully."
                                } else {
                                    "Purchase verified. Google Play acknowledgement is still being processed."
                                }
                            )
                        } else {
                            mutableState.value = mutableState.value.copy(
                                isLoading = false,
                                message = "Google Play reported a purchase, but the backend could not verify it. Premium access has not been granted."
                            )
                        }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        mutableState.value = mutableState.value.copy(
                            isLoading = false,
                            message = "Purchase verification could not be completed. Please retry or restore your purchase."
                        )
                    }
                }
            }
        }
    }

    fun close() {
        scope.cancel()
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}


