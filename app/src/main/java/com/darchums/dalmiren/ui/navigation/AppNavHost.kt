package com.darchums.dalmiren.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.darchums.dalmiren.data.repository.NetworkParentingAssistantRepository
import com.darchums.dalmiren.data.repository.PreferencesRepository
import com.darchums.dalmiren.data.repository.ReflectionsRepository
import com.darchums.dalmiren.ui.screens.auth.AuthScreen
import com.darchums.dalmiren.ui.screens.behaviour.BehaviourScreen
import com.darchums.dalmiren.ui.screens.behaviour.BehaviourViewModel
import com.darchums.dalmiren.ui.screens.communication.CommunicationScreen
import com.darchums.dalmiren.ui.screens.communication.CommunicationViewModel
import com.darchums.dalmiren.ui.screens.companion.CompanionScreen
import com.darchums.dalmiren.ui.screens.companion.CompanionViewModel
import com.darchums.dalmiren.ui.screens.dashboard.DashboardScreen
import com.darchums.dalmiren.ui.screens.dashboard.DashboardViewModel
import com.darchums.dalmiren.ui.screens.discipline.DisciplineScreen
import com.darchums.dalmiren.ui.screens.discipline.DisciplineViewModel
import com.darchums.dalmiren.ui.screens.emotional.EmotionalScreen
import com.darchums.dalmiren.ui.screens.emotional.EmotionalViewModel
import com.darchums.dalmiren.ui.screens.insights.InsightsScreen
import com.darchums.dalmiren.ui.screens.insights.InsightsViewModel
import com.darchums.dalmiren.ui.screens.learning.LearningScreen
import com.darchums.dalmiren.ui.screens.learning.LearningViewModel
import com.darchums.dalmiren.ui.screens.onboarding.OnboardingScreen
import com.darchums.dalmiren.ui.screens.onboarding.OnboardingViewModel
import com.darchums.dalmiren.ui.screens.settings.SettingsScreen
import com.darchums.dalmiren.ui.screens.settings.SettingsViewModel
import com.darchums.dalmiren.ui.theme.DALMIRENAiTheme

@Composable
fun DalMirenApp(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    val preferencesRepository = remember {
        PreferencesRepository(context)
    }
    val assistantRepository = remember {
        NetworkParentingAssistantRepository()
    }
    val reflectionsRepository = remember {
        ReflectionsRepository()
    }

    val authSessionViewModel: AuthSessionViewModel = viewModel(
        factory = AuthSessionViewModelFactory(context)
    )
    val authState by authSessionViewModel.state.collectAsState()

    val prefs by preferencesRepository.userPreferencesFlow.collectAsState(
        initial = null
    )

    DALMIRENAiTheme(
        darkTheme = prefs?.darkTheme
            ?: androidx.compose.foundation.isSystemInDarkTheme()
    ) {
        if (prefs == null) {
            return@DALMIRENAiTheme
        }

        // Authentication is required before accessing parenting features.
        if (!authState.isAuthenticated) {
            AuthScreen(
                state = authState,
                onLogin = authSessionViewModel::login,
                onRegister = authSessionViewModel::register,
                onRetrySession = authSessionViewModel::restoreSession,
                onClearError = authSessionViewModel::clearError,
                modifier = Modifier.fillMaxSize()
            )
            return@DALMIRENAiTheme
        }

        val startDestination = if (prefs!!.onboardingCompleted) {
            ScreenRoute.Dashboard.route
        } else {
            ScreenRoute.Onboarding.route
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                DalMirenBottomBar(navController = navController)
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(ScreenRoute.Onboarding.route) {
                    val viewModel = remember {
                        OnboardingViewModel(preferencesRepository)
                    }
                    OnboardingScreen(
                        onGetStartedClick = {
                            viewModel.completeOnboarding {
                                navController.navigate(ScreenRoute.Dashboard.route) {
                                    popUpTo(ScreenRoute.Onboarding.route) {
                                        inclusive = true
                                    }
                                }
                            }
                        }
                    )
                }

                composable(ScreenRoute.Dashboard.route) {
                    val viewModel = remember {
                        DashboardViewModel(
                            assistantRepository,
                            reflectionsRepository
                        )
                    }
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToCompanion = {
                            navController.navigate(ScreenRoute.Companion.route)
                        },
                        onNavigateToBehaviour = {
                            navController.navigate(ScreenRoute.Behaviour.route)
                        },
                        onNavigateToEmotional = {
                            navController.navigate(ScreenRoute.Emotional.route)
                        },
                        onNavigateToCommunication = {
                            navController.navigate(ScreenRoute.Communication.route)
                        },
                        onNavigateToDiscipline = {
                            navController.navigate(ScreenRoute.Discipline.route)
                        },
                        onNavigateToLearning = {
                            navController.navigate(ScreenRoute.Learning.route)
                        }
                    )
                }

                composable(ScreenRoute.Companion.route) {
                    val viewModel = remember {
                        CompanionViewModel(assistantRepository)
                    }
                    CompanionScreen(viewModel = viewModel)
                }

                composable(ScreenRoute.Behaviour.route) {
                    val viewModel = remember {
                        BehaviourViewModel(
                            assistantRepository,
                            reflectionsRepository
                        )
                    }
                    BehaviourScreen(viewModel = viewModel)
                }

                composable(ScreenRoute.Emotional.route) {
                    val viewModel = remember {
                        EmotionalViewModel(assistantRepository)
                    }
                    EmotionalScreen(viewModel = viewModel)
                }

                composable(ScreenRoute.Communication.route) {
                    val viewModel = remember {
                        CommunicationViewModel(assistantRepository)
                    }
                    CommunicationScreen(viewModel = viewModel)
                }

                composable(ScreenRoute.Discipline.route) {
                    val viewModel = remember {
                        DisciplineViewModel(assistantRepository)
                    }
                    DisciplineScreen(viewModel = viewModel)
                }

                composable(ScreenRoute.Learning.route) {
                    val viewModel = remember {
                        LearningViewModel(assistantRepository)
                    }
                    LearningScreen(viewModel = viewModel)
                }

                composable(ScreenRoute.Insights.route) {
                    val viewModel = remember {
                        InsightsViewModel(reflectionsRepository)
                    }
                    InsightsScreen(viewModel = viewModel)
                }

                composable(ScreenRoute.Settings.route) {
                    val consentViewModel: ConsentViewModel = viewModel(
                        factory = ConsentViewModelFactory()
                    )

                    val viewModel = remember {
                        SettingsViewModel(
                            preferencesRepository,
                            reflectionsRepository
                        )
                    }

                    SettingsScreen(
                        viewModel = viewModel,
                        accountEmail = authState.user?.email,
                        isLoggingOut = authState.isLoading,
                        onLogout = {
                            authSessionViewModel.logout()
                        },
                        consentViewModel = consentViewModel,
                        onResetOnboarding = {
                            navController.navigate(ScreenRoute.Onboarding.route) {
                                popUpTo(0) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
