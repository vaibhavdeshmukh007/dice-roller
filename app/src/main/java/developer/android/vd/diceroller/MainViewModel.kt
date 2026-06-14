package developer.android.vd.diceroller

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel(
    private val proStatusProvider: ProStatusProvider,
    private val repository: RollHistoryRepository
) : ViewModel() {

    companion object {
        const val MAX_DICE = 9
        const val MIN_DICE = 1
    }

    private val _uiState = MutableLiveData(
        DiceUiState()
    )
    val uiState: LiveData<DiceUiState> = _uiState


    private val _rollHistory = MutableLiveData<List<RollEntry>>(emptyList())
    val rollHistory: LiveData<List<RollEntry>> = _rollHistory

    fun increaseDice() {
        val state = _uiState.value ?: return
        if (state.diceCount < MAX_DICE) {
            _uiState.value = state.copy(
                diceCount = state.diceCount + 1,
                results = List(state.diceCount + 1) { 0 },
                total = 0,
                lockedIndices = emptySet()
            )
        }
    }

    fun decreaseDice() {
        val state = _uiState.value ?: return
        if (state.diceCount > MIN_DICE) {
            _uiState.value = state.copy(
                diceCount = state.diceCount - 1,
                results = List(state.diceCount - 1) { 0 },
                total = 0,
                lockedIndices = emptySet()
            )
        }
    }

    fun toggleDiceLock(index: Int) {
        val state = _uiState.value ?: return
        if (state.isRolling) return
        val currentLocked = state.lockedIndices.toMutableSet()
        if (currentLocked.contains(index)) {
            currentLocked.remove(index)
        } else {
            currentLocked.add(index)
        }
        _uiState.value = state.copy(lockedIndices = currentLocked)
    }

    fun rollDice(diceType: DiceType) {
        if (diceType.proOnly && !proStatusProvider.isProActive()) {
            return
        }

        if (diceType.proOnly) {
            proStatusProvider.markProUsed()
        }

        proStatusProvider.incrementRollCount()


        val state = _uiState.value ?: return
        val results = List(state.diceCount) { index ->
            if (state.lockedIndices.contains(index) && state.results.size > index && state.results[index] > 0) {
                state.results[index]
            } else {
                kotlin.random.Random.nextInt(1, diceType.sides + 1)
            }
        }

        val total = results.sum()

        _uiState.value = state.copy(
            diceType = diceType,
            results = results,
            total = total,
            isRolling = true
        )

        val entry = RollEntry(
            timestamp = System.currentTimeMillis(),
            diceType = diceType,
            diceCount = state.diceCount,
            results = results,
            total = total
        )

        viewModelScope.launch {
            repository.add(entry)
            _rollHistory.value =
                repository.getHistory(proStatusProvider.isProActive())
        }
    }

    fun restoreHistory() {
        viewModelScope.launch {
            _rollHistory.value =
                repository.getHistory(proStatusProvider.isProActive())
        }
    }

    fun setDiceType(type: DiceType) {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(
            diceType = type,
            results = List(state.diceCount) { 0 },
            total = 0,
            lockedIndices = emptySet()
        )
    }

    fun onRollAnimationFinished() {
        val state = _uiState.value ?: return
        if (state.isRolling) {
            _uiState.value = state.copy(isRolling = false)
        }
    }


}
