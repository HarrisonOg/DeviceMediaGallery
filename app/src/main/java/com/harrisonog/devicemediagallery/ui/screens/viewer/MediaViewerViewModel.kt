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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaViewerUiState(
    val mediaItems: List<MediaItem> = emptyList(),
    val initialIndex: Int = 0,
    val isLoading: Boolean = true,
    val showControls: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val folderPath: String = savedStateHandle.get<String>("folderPath") ?: ""
    private val initialIndexArg: Int = savedStateHandle.get<Int>("initialIndex") ?: 0

    private val _uiState = MutableStateFlow(MediaViewerUiState(initialIndex = initialIndexArg))
    val uiState: StateFlow<MediaViewerUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    private fun loadMedia() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                mediaRepository.getMediaItems(folderPath).collect { items ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        mediaItems = items
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load media"
                )
            }
        }
    }

    fun toggleControls() {
        _uiState.value = _uiState.value.copy(showControls = !_uiState.value.showControls)
    }
}
