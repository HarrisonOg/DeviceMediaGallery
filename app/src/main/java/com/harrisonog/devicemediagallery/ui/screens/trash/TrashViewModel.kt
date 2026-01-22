package com.harrisonog.devicemediagallery.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrisonog.devicemediagallery.data.repository.TrashRepository
import com.harrisonog.devicemediagallery.domain.model.TrashItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val isLoading: Boolean = true,
    val trashItems: List<TrashItem> = emptyList(),
    val selectedItems: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val error: String? = null,
    val showEmptyTrashDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val trashRepository: TrashRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        loadTrashItems()
        deleteExpiredItems()
    }

    private fun loadTrashItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            trashRepository.getTrashItems()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load trash items") }
                }
                .collect { items ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        trashItems = items
                    ) }
                }
        }
    }

    private fun deleteExpiredItems() {
        viewModelScope.launch {
            try {
                trashRepository.deleteExpiredItems()
            } catch (e: Exception) {
                // Non-critical error, don't show to user
            }
        }
    }

    fun toggleItemSelection(item: TrashItem) {
        _uiState.update { state ->
            val itemUri = item.originalUri.toString()
            val currentSelected = state.selectedItems
            val newSelected = if (itemUri in currentSelected) {
                currentSelected - itemUri
            } else {
                currentSelected + itemUri
            }

            state.copy(
                selectedItems = newSelected,
                isSelectionMode = newSelected.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(
            selectedItems = emptySet(),
            isSelectionMode = false
        ) }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allUris = state.trashItems.map { it.originalUri.toString() }.toSet()
            state.copy(
                selectedItems = allUris,
                isSelectionMode = true
            )
        }
    }

    fun showEmptyTrashDialog() {
        _uiState.update { it.copy(showEmptyTrashDialog = true) }
    }

    fun hideEmptyTrashDialog() {
        _uiState.update { it.copy(showEmptyTrashDialog = false) }
    }

    fun showDeleteConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    fun hideDeleteConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    fun restoreSelected() {
        viewModelScope.launch {
            val state = _uiState.value
            val selectedUris = state.selectedItems
            val itemsToRestore = state.trashItems.filter {
                it.originalUri.toString() in selectedUris
            }

            trashRepository.restoreFromTrash(itemsToRestore)
                .onSuccess {
                    clearSelection()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to restore items") }
                }
        }
    }

    fun deleteSelectedPermanently() {
        viewModelScope.launch {
            val state = _uiState.value
            val selectedUris = state.selectedItems
            val itemsToDelete = state.trashItems.filter {
                it.originalUri.toString() in selectedUris
            }

            trashRepository.permanentlyDelete(itemsToDelete)
                .onSuccess {
                    clearSelection()
                    hideDeleteConfirmDialog()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to delete items") }
                    hideDeleteConfirmDialog()
                }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            trashRepository.emptyTrash()
                .onSuccess {
                    hideEmptyTrashDialog()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to empty trash") }
                    hideEmptyTrashDialog()
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
