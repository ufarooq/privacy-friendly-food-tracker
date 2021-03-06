/*
This file is part of Privacy friendly food tracker.

Privacy friendly food tracker is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Privacy friendly food tracker is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Privacy friendly food tracker.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.secuso.privacyfriendlyfoodtracker.ui.adapter;

import android.content.Context;
import android.util.Log;

import org.secuso.privacyfriendlyfoodtracker.database.ApplicationDatabase;
import org.secuso.privacyfriendlyfoodtracker.database.ConsumedEntrieAndProductDao;
import org.secuso.privacyfriendlyfoodtracker.database.ConsumedEntries;
import org.secuso.privacyfriendlyfoodtracker.database.ConsumedEntriesDao;
import org.secuso.privacyfriendlyfoodtracker.database.Product;
import org.secuso.privacyfriendlyfoodtracker.database.ProductDao;

import java.util.ArrayList;
import java.util.List;

/**
 * Database access functions.
 *
 * @author Simon Reinkemeier, Andre Lutz
 */
public class DatabaseFacade {
    ProductDao productDao;
    ConsumedEntriesDao consumedEntriesDao;
    ConsumedEntrieAndProductDao consumedEntrieAndProductDao;

    public DatabaseFacade(Context context) throws Exception {
        this.productDao = ApplicationDatabase.getInstance(context).getProductDao();
        this.consumedEntriesDao = ApplicationDatabase.getInstance(context).getConsumedEntriesDao();
        this.consumedEntrieAndProductDao = ApplicationDatabase.getInstance(context).getConsumedEntriesAndProductDao();
    }

    /**
     * Insert a new consumed entry.
     * @param amount the amount
     * @param date the consumption date in UNIX format
     * @param name the name
     * @param productId the consumed product id
     * @return true if no error occurs
     */
    public boolean insertEntry(int amount, java.util.Date date, String name, float energy, int productId){
        int existingProductId = 0;
        //If the productId is 0 we need to create a new product in the database
        if (0 == productId) {
            insertProduct(name, energy, "");
            // retrieve ProductId of newly created Product from database
            List<Product> existingProducts = productDao.findExistingProducts(name, energy, "");
            // There is only one existing product so we take the first one from the List
            Product p = existingProducts.get(0);
            existingProductId = p.id;
        } else {
            existingProductId = productId;
        }
        try {
            consumedEntriesDao.insert(new ConsumedEntries(0, amount, new java.sql.Date(date.getTime()), name, existingProductId));
            return true;
        } catch (Exception e){
            return false;
        }
    }


    /**
     * Deletes a database entry by id.
     * @param id the id
     * @return successfully or not
     */
    public boolean deleteEntryById(int id ){
        try {
            List<ConsumedEntries> res = consumedEntriesDao.findConsumedEntriesById(id);
            if(res.size() != 1){return false;}
            consumedEntriesDao.delete(res.get(0));
            return true;
        } catch (Exception e){
            return false;
        }
    }

    /**
     * Edit a database entry.
     * @param id the id
     * @param amount the new amount
     * @return successfully or not
     */
    public boolean editEntryById(int id, int amount){
        try {
            List<ConsumedEntries> res = consumedEntriesDao.findConsumedEntriesById(id);
            if(res.size() != 1){return false;}
            ConsumedEntries consumedEntry = res.get(0);
            consumedEntry.amount = amount;
            consumedEntriesDao.update(res.get(0));
            return true;
        } catch (Exception e){
            return false;
        }
    }

    /**
     * Crate a new Product
     * @param name the name
     * @param energy the energy
     * @param barcode the barcode
     * @return successfully or not
     */
    public boolean insertProduct( String name, float energy,  String barcode){
        try{
            List<Product> res = productDao.findExistingProducts(name, energy, barcode);
            if(res.size() != 0){
                return false;
            }
            productDao.insert(new Product(0,name, energy, barcode));
            return true;
        }catch (Exception e){
            return false;
        }
    }

    /**
     * Find the most common products.
     * @return Returns a list with the most common products
     */
    public List<Product> findMostCommonProducts() {
        List<Product> products = new ArrayList<>();
        try {
            List<Integer> res = consumedEntriesDao.findMostCommonProducts();
            for (int i = 0; i < res.size(); i++) {
                products.add(productDao.findProductById(res.get(i)));
            }

        } catch (Exception e) {
            Log.e("DatabaseFacade", "Error o");
        }
        return products;
    }

    /**
     * Returns a database entry for a specified date.
     * @param date the date
     * @return DatabaseEntry
     */
    public DatabaseEntry[] getEntriesForDay(java.util.Date date) {
        List<DatabaseEntry> databaseEntries = new ArrayList<>();
        try {
            List<ConsumedEntries> res = consumedEntriesDao.findConsumedEntriesForDate(new java.sql.Date(date.getTime()));
            for (int i = 0; i < res.size(); i++) {
                ConsumedEntries consumedEntry = res.get(i);
                Product product = productDao.findProductById(consumedEntry.productId);
                databaseEntries.add(new DatabaseEntry(String.valueOf(consumedEntry.id),consumedEntry.name, consumedEntry.amount, product.energy));
            }

        } catch (Exception e) {
            Log.e("DatabaseFacade", "Error o");
        }
        return databaseEntries.toArray(new DatabaseEntry[databaseEntries.size()]);
    }

    /**
     * Returns the sum of calories per day for a time period.
     * @param startDate the start date
     * @param endDate the end date
     * @return the calories sum per day and the associated date
     */
    public List<ConsumedEntrieAndProductDao.DateCalories> getPeriodCalories(java.util.Date startDate, java.util.Date endDate){
        return    consumedEntrieAndProductDao.getCaloriesPeriod(new java.sql.Date(startDate.getTime()), new java.sql.Date(endDate.getTime()));
    }

    /**
     * Returns the sum of calories between two dates.
     * @param startDate the start date
     * @param endDate the end date
     * @return the calories sum (list position 0)
     */
    public List<ConsumedEntrieAndProductDao.DateCalories> getCaloriesPerDayinPeriod(java.util.Date startDate, java.util.Date endDate){
        return    consumedEntrieAndProductDao.getCaloriesPerDayinPeriod(new java.sql.Date(startDate.getTime()), new java.sql.Date(endDate.getTime()));
    }

    /**
     * Returns a list of products containing the input string
     * @param name the search term
     * @return a List with products containing the search term
     */
    public List<Product> getProductByName(String name){
        return productDao.findProductsByName("%" + name + "%");
    }


}
