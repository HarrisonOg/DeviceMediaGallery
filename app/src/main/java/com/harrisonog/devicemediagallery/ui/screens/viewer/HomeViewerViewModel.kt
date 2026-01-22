package com.harrisonog.devicemediagallery.ui.screens.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrisonog.devicemediagallery.data.repository.MediaRepository
import com.harrisonog.devicemediagallery.domain.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeViewerUiState(
    val isLoading: Boolean = true,
    val mediaItems: List<MediaItem> = emptyList(),
    val initialIndex: Int = 0,
    val error: String? = null
)

@HiltViewModel
class HomeViewerViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialIndexArg: Int = savedStateHandle.get<Int>("initialIndex") ?: 0

    private val _uiState = MutableStateFlow(HomeViewerUiState(initialIndex = initialIndexArg))
    val uiState: StateFlow<HomeViewerUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    private fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load all media (no folder filtering) with pagination
            mediaRepository.getMediaItems(
                limit = 10000,  // Large limit to get all recent media
                offset = 0
            )
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load media"
                        )
                    }
                }
                .collect { items ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            mediaItems = items
                        )
                    }
                }
        }
    }
}
