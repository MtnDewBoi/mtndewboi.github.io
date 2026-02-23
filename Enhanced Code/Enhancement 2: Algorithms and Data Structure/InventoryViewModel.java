package com.example.cs360finalproject;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * ViewModel for managing inventory data and business logic.
 * Handles communication between the UI (OverviewActivity) and the Repository.
 */
public class InventoryViewModel extends AndroidViewModel {

    private final InventoryRepository inventoryRepository;
    private final MutableLiveData<List<InventoryItem>> items = new MutableLiveData<>();
    private List<InventoryItem> fullItemList = new ArrayList<>(); // Cache of all items for filtering

    private SortCriteria currentSortCriteria = SortCriteria.ID;
    private SortOrder currentSortOrder = SortOrder.NONE;
    private String currentFilter = "";
    private boolean showLowStockOnly = false;

    public enum SortCriteria {
        ID, NAME, QUANTITY, REQUIRED
    }

    public enum SortOrder {
        ASC, DESC, NONE
    }

    public InventoryViewModel(@NonNull Application application) {
        super(application);
        inventoryRepository = new InventoryRepository(application);
        loadItems();
    }

    /**
     * Returns the LiveData list of inventory items.
     */
    public LiveData<List<InventoryItem>> getItems() {
        return items;
    }

    /**
     * Loads items from the repository and processes them (filter/sort).
     */
    public void loadItems() {
        fullItemList = inventoryRepository.getAllItems();
        processList();
    }

    /**
     * Adds a new item to the inventory.
     * @return true if successful.
     */
    public boolean addItem(String name, long quantity, long requiredInventory) {
        boolean success = inventoryRepository.addItem(name, quantity, requiredInventory);
        if (success) loadItems();
        return success;
    }

    /**
     * Updates the quantity of an item.
     * Triggers SMS alert if quantity reaches zero.
     * @param itemId The ID of the item.
     * @param newQuantity The new quantity.
     * @return true if successful.
     */
    public boolean updateItemQuantity(int itemId, long newQuantity) {
        boolean success = inventoryRepository.updateItemQuantity(itemId, newQuantity);
        if (success) {
            loadItems();
            if (newQuantity == 0) {
                notifyZeroQuantity(itemId);
            }
        }
        return success;
    }

    /**
     * Deletes an item from the inventory.
     * @param itemId The ID of the item.
     * @return true if successful.
     */
    public boolean deleteItem(int itemId) {
        boolean success = inventoryRepository.deleteItem(itemId);
        if (success) loadItems();
        return success;
    }

    public InventoryItem getItemById(int id) { return inventoryRepository.getItemById(id); }
    public InventoryItem getItemByName(String name) { return inventoryRepository.getItemByName(name); }
    public boolean updateItemDetails(int id, String name, long quantity, long required) {
        return inventoryRepository.updateItemDetails(id, name, quantity, required);
    }

    /**
     * Updates the search filter query.
     * @param query The search text.
     */
    public void performSearch(String query) {
        currentFilter = query;
        processList();
    }

    /**
     * Sets whether to show only low stock items.
     * @param showLowStockOnly true to filter by low stock.
     */
    public void setShowLowStockOnly(boolean showLowStockOnly) {
        this.showLowStockOnly = showLowStockOnly;
        processList();
    }

    /**
     * Checks if the low stock filter is active.
     */
    public boolean isShowLowStockOnly() {
        return showLowStockOnly;
    }

    public SortOrder getCurrentSortOrder() { return currentSortOrder; }
    public SortCriteria getCurrentSortCriteria() { return currentSortCriteria; }

    /**
     * Sorts the items based on the given criteria.
     * Toggles sort order if the same criteria is selected again.
     * Cycle: ASC -> DESC -> NONE -> ASC.
     *
     * @param criteria The criteria to sort by (Name, Quantity, etc.).
     */
    public void sortItems(SortCriteria criteria) {
        if (currentSortCriteria == criteria) {
            // Cycle: ASC -> DESC -> NONE -> ASC
            switch (currentSortOrder) {
                case ASC: currentSortOrder = SortOrder.DESC; break;
                case DESC: currentSortOrder = SortOrder.NONE; break;
                case NONE: currentSortOrder = SortOrder.ASC; break;
            }
        } else {
            currentSortCriteria = criteria;
            currentSortOrder = SortOrder.ASC; // Default to ascending for new criteria
        }
        processList();
    }

    /**
     * Filters and sorts the full item list based on current settings and updates the LiveData.
     */
    private void processList() {
        List<InventoryItem> filteredList = new ArrayList<>();

        // 1. Filter (Search + Low Stock)
        String lowerQuery = (currentFilter != null) ? currentFilter.toLowerCase() : "";

        for (InventoryItem item : fullItemList) {
            boolean matchesSearch = lowerQuery.isEmpty() || item.getName().toLowerCase().contains(lowerQuery);
            boolean matchesLowStock = !showLowStockOnly || (item.getQuantity() < item.getRequiredInventory());

            if (matchesSearch && matchesLowStock) {
                filteredList.add(item);
            }
        }

        // 2. Sort
        if (currentSortOrder != SortOrder.NONE) {
            Collections.sort(filteredList, (o1, o2) -> {
                int result = 0;
                switch (currentSortCriteria) {
                    case NAME: result = o1.getName().compareToIgnoreCase(o2.getName()); break;
                    case QUANTITY: result = Long.compare(o1.getQuantity(), o2.getQuantity()); break;
                    case REQUIRED: result = Long.compare(o1.getRequiredInventory(), o2.getRequiredInventory()); break;
                    default: result = Integer.compare(o1.getId(), o2.getId());
                }
                return currentSortOrder == SortOrder.ASC ? result : -result;
            });
        } else {
            // Default order (ID) to restore original order
            Collections.sort(filteredList, (o1, o2) -> Integer.compare(o1.getId(), o2.getId()));
        }

        items.setValue(filteredList);
    }

    /**
     * Sends an SMS alert if an item's quantity reaches zero.
     * @param itemId The ID of the item.
     */
    private void notifyZeroQuantity(final int itemId) {
        Context context = getApplication();
        final SharedPreferences sharedPreferences = context.getSharedPreferences(SMSPermissionsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        final boolean smsEnabled = sharedPreferences.getBoolean(SMSPermissionsActivity.KEY_SMS_PERMISSION, false);
        final String phoneNumber = sharedPreferences.getString(SMSPermissionsActivity.KEY_PHONE_NUMBER, "");

        if (smsEnabled && !phoneNumber.isEmpty()) {
            InventoryItem item = getItemById(itemId);
            if (item != null) {
                String itemName = item.getName();
                if (itemName != null) {
                    SMSHelper.sendSMS(context, phoneNumber, "Inventory Alert: Item '" + itemName + "' has reached zero quantity.");
                }
            }
        }
    }

    /**
     * Retrieves predictive order recommendations.
     */
    public List<String> getOrderRecommendations() {
        return inventoryRepository.getOrderRecommendations();
    }

    /**
     * Retrieves usage history for the graph.
     */
    public Map<String, List<Integer>> getUsageHistory() {
        return inventoryRepository.getPerItemUsageHistory();
    }

    public Map<String, List<Integer>> getUsageHistory(long durationMillis, long stepMillis) {
        return inventoryRepository.getUsageHistory(durationMillis, stepMillis);
    }

    public Map<String, List<Integer>> getPredictedUsage(long durationMillis, long stepMillis) {
        return inventoryRepository.getPredictedUsage(durationMillis, stepMillis);
    }
}