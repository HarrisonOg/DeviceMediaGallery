package com.harrisonog.devicemediagallery.ui.screens.folderdetail

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

data class FolderDetailUiState(
    val isLoading: Boolean = true,
    val mediaItems: List<MediaItem> = emptyList(),
    val folderName: String = "",
    val error: String? = null,
    val selectedItems: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val folderPath: String = savedStateHandle.get<String>("folderPath") ?: ""

    private val _uiState = MutableStateFlow(FolderDetailUiState())
    val uiState: StateFlow<FolderDetailUiState> = _uiState.asStateFlow()

    init {
        val folderName = folderPath.substringAfterLast("/")
        _uiState.value = _uiState.value.copy(folderName = folderName)
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                mediaRepository.getMediaItems(folderPath).collect { items ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        mediaItems = items
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Failed to load media"
                )
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadMedia()
    }

    fun toggleSelectionMode() {
        val newSelectionMode = !_uiState.value.isSelectionMode
        _uiState.value = _uiState.value.copy(
            isSelectionMode = newSelectionMode,
            selectedItems = if (newSelectionMode) _uiState.value.selectedItems else emptySet()
        )
    }

    fun toggleItemSelection(item: MediaItem) {
        val itemUri = item.uri.toString()
        val currentSelected = _uiState.value.selectedItems
        val newSelected = if (itemUri in currentSelected) {
            currentSelected - itemUri
        } else {
            currentSelected + itemUri
        }

        _uiState.value = _uiState.value.copy(
            selectedItems = newSelected,
            isSelectionMode = newSelected.isNotEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedItems = emptySet(),
            isSelectionMode = false
        )
    }

    fun selectAll() {
        val allUris = _uiState.value.mediaItems.map { it.uri.toString() }.toSet()
        _uiState.value = _uiState.value.copy(
            selectedItems = allUris,
            isSelectionMode = true
        )
    }
}
