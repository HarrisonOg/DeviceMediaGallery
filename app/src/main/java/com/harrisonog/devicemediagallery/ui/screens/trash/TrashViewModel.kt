package com.harrisonog.devicemediagallery.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrisonog.devicemediagallery.data.repository.TrashRepository
import com.harrisonog.devicemediagallery.domain.model.TrashItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                trashRepository.getTrashItems().collect { items ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        trashItems = items
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load trash items"
                )
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
        val itemUri = item.originalUri.toString()
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
        val allUris = _uiState.value.trashItems.map { it.originalUri.toString() }.toSet()
        _uiState.value = _uiState.value.copy(
            selectedItems = allUris,
            isSelectionMode = true
        )
    }

    fun showEmptyTrashDialog() {
        _uiState.value = _uiState.value.copy(showEmptyTrashDialog = true)
    }

    fun hideEmptyTrashDialog() {
        _uiState.value = _uiState.value.copy(showEmptyTrashDialog = false)
    }

    fun showDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = true)
    }

    fun hideDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
    }

    fun restoreSelected() {
        viewModelScope.launch {
            val selectedUris = _uiState.value.selectedItems
            val itemsToRestore = _uiState.value.trashItems.filter {
                it.originalUri.toString() in selectedUris
            }

            trashRepository.restoreFromTrash(itemsToRestore)
                .onSuccess {
                    clearSelection()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to restore items"
                    )
                }
        }
    }

    fun deleteSelectedPermanently() {
        viewModelScope.launch {
            val selectedUris = _uiState.value.selectedItems
            val itemsToDelete = _uiState.value.trashItems.filter {
                it.originalUri.toString() in selectedUris
            }

            trashRepository.permanentlyDelete(itemsToDelete)
                .onSuccess {
                    clearSelection()
                    hideDeleteConfirmDialog()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to delete items"
                    )
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
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to empty trash"
                    )
                    hideEmptyTrashDialog()
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
