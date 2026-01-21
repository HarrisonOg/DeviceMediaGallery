package com.harrisonog.devicemediagallery.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrisonog.devicemediagallery.data.repository.AlbumRepository
import com.harrisonog.devicemediagallery.domain.model.VirtualAlbum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumsUiState(
    val isLoading: Boolean = true,
    val albums: List<VirtualAlbum> = emptyList(),
    val selectedAlbums: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val showCreateDialog: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumsUiState())
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    init {
        loadAlbums()
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                albumRepository.getVirtualAlbums().collect { albums ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        albums = albums
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load albums"
                )
            }
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun createAlbum(name: String) {
        viewModelScope.launch {
            try {
                albumRepository.createAlbum(name)
                hideCreateDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to create album"
                )
            }
        }
    }

    fun toggleAlbumSelection(album: VirtualAlbum) {
        val currentSelected = _uiState.value.selectedAlbums
        val newSelected = if (album.id in currentSelected) {
            currentSelected - album.id
        } else {
            currentSelected + album.id
        }

        _uiState.value = _uiState.value.copy(
            selectedAlbums = newSelected,
            isSelectionMode = newSelected.isNotEmpty()
        )
    }

    fun enterSelectionMode(album: VirtualAlbum) {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedAlbums = setOf(album.id)
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedAlbums = emptySet()
        )
    }

    fun deleteSelectedAlbums() {
        viewModelScope.launch {
            try {
                _uiState.value.selectedAlbums.forEach { albumId ->
                    albumRepository.deleteAlbum(albumId)
                }
                clearSelection()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete albums"
                )
            }
        }
    }
}
