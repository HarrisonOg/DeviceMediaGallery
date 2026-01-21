package com.harrisonog.devicemediagallery.ui.screens.albumdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrisonog.devicemediagallery.data.repository.AlbumRepository
import com.harrisonog.devicemediagallery.data.repository.MediaRepository
import com.harrisonog.devicemediagallery.domain.model.MediaItem
import com.harrisonog.devicemediagallery.domain.model.VirtualAlbum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailUiState(
    val isLoading: Boolean = true,
    val album: VirtualAlbum? = null,
    val mediaItems: List<MediaItem> = emptyList(),
    val selectedItems: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isRefreshing: Boolean = false,
    val showRenameDialog: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val albumId: Long = savedStateHandle.get<Long>("albumId") ?: 0L

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    init {
        loadAlbumDetails()
    }

    private fun loadAlbumDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val album = albumRepository.getAlbumById(albumId)
                _uiState.value = _uiState.value.copy(album = album)

                albumRepository.getAlbumMediaUris(albumId).collect { uris ->
                    val allMedia = mediaRepository.getMediaItems().first()
                    val albumMedia = allMedia.filter { it.uri.toString() in uris }
                        .sortedByDescending { it.dateModified }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        mediaItems = albumMedia
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Failed to load album"
                )
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadAlbumDetails()
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

    fun removeSelectedFromAlbum() {
        viewModelScope.launch {
            try {
                albumRepository.removeMediaFromAlbum(albumId, _uiState.value.selectedItems.toList())
                clearSelection()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to remove items"
                )
            }
        }
    }

    fun showRenameDialog() {
        _uiState.value = _uiState.value.copy(showRenameDialog = true)
    }

    fun hideRenameDialog() {
        _uiState.value = _uiState.value.copy(showRenameDialog = false)
    }

    fun renameAlbum(newName: String) {
        viewModelScope.launch {
            try {
                _uiState.value.album?.let { album ->
                    val updatedAlbum = album.copy(name = newName)
                    albumRepository.updateAlbum(updatedAlbum)
                    _uiState.value = _uiState.value.copy(album = updatedAlbum)
                }
                hideRenameDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to rename album"
                )
            }
        }
    }
}
