package com.harrisonog.devicemediagallery.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrisonog.devicemediagallery.data.repository.MediaRepository
import com.harrisonog.devicemediagallery.domain.model.MediaFolder
import com.harrisonog.devicemediagallery.domain.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val mediaItems: List<MediaItem> = emptyList(),
    val folders: List<MediaFolder> = emptyList(),
    val selectedItems: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                mediaRepository.getMediaItems().collect { items ->
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

        viewModelScope.launch {
            try {
                mediaRepository.getFolders().collect { folders ->
                    _uiState.value = _uiState.value.copy(folders = folders)
                }
            } catch (e: Exception) {
                // Folders loading error - non-critical
            }
        }
    }

    fun toggleItemSelection(item: MediaItem) {
        val currentSelected = _uiState.value.selectedItems.toMutableSet()
        val itemUri = item.uri.toString()

        if (itemUri in currentSelected) {
            currentSelected.remove(itemUri)
        } else {
            currentSelected.add(itemUri)
        }

        _uiState.value = _uiState.value.copy(
            selectedItems = currentSelected,
            isSelectionMode = currentSelected.isNotEmpty()
        )
    }

    fun enterSelectionMode(item: MediaItem) {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedItems = setOf(item.uri.toString())
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedItems = emptySet()
        )
    }

    fun selectAll() {
        val allUris = _uiState.value.mediaItems.map { it.uri.toString() }.toSet()
        _uiState.value = _uiState.value.copy(selectedItems = allUris)
    }
}
