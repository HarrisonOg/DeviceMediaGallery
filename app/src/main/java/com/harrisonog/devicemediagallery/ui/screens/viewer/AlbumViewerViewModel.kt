package com.harrisonog.devicemediagallery.ui.screens.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrisonog.devicemediagallery.data.repository.AlbumRepository
import com.harrisonog.devicemediagallery.data.repository.MediaRepository
import com.harrisonog.devicemediagallery.domain.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumViewerUiState(
    val mediaItems: List<MediaItem> = emptyList(),
    val initialIndex: Int = 0,
    val isLoading: Boolean = true,
    val showControls: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AlbumViewerViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val albumId: Long = savedStateHandle.get<Long>("albumId") ?: 0L
    private val initialIndexArg: Int = savedStateHandle.get<Int>("initialIndex") ?: 0

    private val _uiState = MutableStateFlow(AlbumViewerUiState(initialIndex = initialIndexArg))
    val uiState: StateFlow<AlbumViewerUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    private fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            albumRepository.getAlbumMediaUris(albumId)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load media") }
                }
                .collect { uris ->
                    val albumMedia = mediaRepository.getMediaItemsByUris(uris.toList())
                        .sortedByDescending { it.dateModified }

                    _uiState.update { it.copy(
                        isLoading = false,
                        mediaItems = albumMedia
                    ) }
                }
        }
    }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }
}
