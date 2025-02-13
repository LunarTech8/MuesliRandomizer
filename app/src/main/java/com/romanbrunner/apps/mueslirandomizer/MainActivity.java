package com.romanbrunner.apps.mueslirandomizer;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import com.google.gson.Gson;
import com.romanbrunner.apps.mueslirandomizer.databinding.MainScreenBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.romanbrunner.apps.mueslirandomizer.ArticleEntity.*;


public class MainActivity extends AppCompatActivity
{
    // --------------------
    // Data code
    // --------------------

    private final static double FILLER_INGREDIENT_RATIO = 0.5;  // Ratio compared to first regular article, 1 being equal ratio
    private final static int TOPPINGS_INGREDIENT_COUNT = 1;  // Currently fixed, might be transformed back into a dynamic slider value
    private final static int MAX_FULL_RESET_RANDOMIZE_TRIES = 3;
    private final static int MAX_RANDOMIZE_TRIES = 1024;
    private final static String INTENT_TYPE_JSON = "*/*";  // No MIME type for json yet, thus allowing every file
    private final static String ARTICLES_FILENAME = "AllArticles";
    private final static String PREFS_NAME = "GlobalPreferences";
    private final static String EXPORT_ITEMS_FILENAME = "MuesliItemsData";
    private final static int DEFAULT_ARTICLES_RES_ID = R.raw.default_muesli_items_data;
    private final static int LATEST_ITEMS_VERSION = 1;

    private static double sizeValue2SizeWeight(int sizeValue)
    {
        return 35. + 15. * sizeValue;
    }

    private static double sugarValue2SugarPercentage(int sugarValue)
    {
        return 0.1 + 0.025 * sugarValue;
    }

    private static int articlesValue2ArticlesCount(int articlesValue)
    {
        return articlesValue + 1;
    }

    private static double toppingValue2ToppingPercentage(int toppingValue)
    {
        return 0.05 * toppingValue;
    }


    // --------------------
    // Functional code
    // --------------------

    public enum UserMode
    {
        MIX_MUESLI, AVAILABILITY, EDIT_ITEMS
    }

    public UserMode userMode = UserMode.MIX_MUESLI;
    public boolean isChosenMuesliUsed = true;

    private final List<ArticleEntity> allArticles = new LinkedList<>();  // All catalogued articles, also not available ones
    private final List<ArticleEntity> selectableFillerArticles = new LinkedList<>();  // Selectable filler type articles, separate from the other lists with only regular articles
    private final List<ArticleEntity> usedFillerArticles = new LinkedList<>();  // Used filler type articles, separate from the other lists with only regular articles
    private final List<ArticleEntity> selectableToppingArticles = new LinkedList<>();  // Selectable topping articles for the next muesli mix creation
    private final List<ArticleEntity> usedToppingArticles = new LinkedList<>();  // Used topping articles, will be reshuffled into selectableToppingArticles once that is depleted
    private final List<ArticleEntity> chosenToppingArticles = new LinkedList<>();  // Chosen topping articles for the current muesli mix creation
    private final List<ArticleEntity> selectableRegularArticles = new LinkedList<>();  // Selectable regular articles for the next muesli mix creation
    private final List<ArticleEntity> usedRegularArticles = new LinkedList<>();  // Used regular articles, will be reshuffled into selectableArticles once that is depleted
    private final List<ArticleEntity> chosenRegularArticles = new LinkedList<>();  // Chosen regular articles for the current muesli mix creation
    private final Set<ArticleEntity> priorityRegularArticles = new LinkedHashSet<>();  // Remaining regular articles that have to be chosen for the next muesli mix creation, articles are also in selectableArticles
    private IngredientsAdapter ingredientsAdapter;
    private ArticlesAdapter availableArticlesAdapter;
    private MuesliMix muesliMix;
    private MainScreenBinding binding;
    private int sizeValue;
    private int sugarValue;
    private int articlesValue;
    private int toppingsValue;
    private String itemsJsonString;
    private int loadedItemsVersion = 0;
    private ActivityResultLauncher<Intent> createFileActivityLauncher;
    private ActivityResultLauncher<Intent> openFileActivityLauncher;

    private static void createErrorAlertDialog(final Context context, final String title, final String message)
    {
        Log.e(title, message);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder
                .setTitle(title + " Error")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Continue", (dialog, id) -> dialog.cancel());
        final AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private static <T> double getLowestValue(final Context context, final List<T> list, final Function<T, Double> getter)
    {
        if (list.isEmpty()) createErrorAlertDialog(context, "getLowestValue", "Cannot get value for an empty list");

        double lowestValue = getter.apply(list.get(0));
        for (var i = 1; i < list.size(); i++)
        {
            lowestValue = Math.min(lowestValue, getter.apply(list.get(i)));
        }
        return lowestValue;
    }

    private static <T> void removeNonIntersectingElements(final Collection<T> targetList, final Collection<T> checkList)
    {
        targetList.removeIf(t -> !checkList.contains(t));
    }

    public boolean isDarkModeEnabled()
    {
        return (this.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refreshData(boolean reloadArticleAdapter)
    {
        refreshStateLists();
        refreshCountInfo();
        refreshInventoryInfo();
        if (reloadArticleAdapter)
        {
            availableArticlesAdapter.notifyDataSetChanged();
        }
    }

    public void removeArticle(final ArticleEntity article)
    {
        allArticles.remove(article);
    }

    private List<ArticleEntity> getAvailableArticles()
    {
        return allArticles.stream().filter(ArticleEntity::isAvailable).collect(Collectors.toCollection(LinkedList::new));
    }

    private void moveArticlesToStateList(final List<ArticleEntity> sourceStateList, final List<ArticleEntity> targetStateList)
    {
        if (sourceStateList == usedRegularArticles || sourceStateList == usedToppingArticles || sourceStateList == usedFillerArticles)
        {
            sourceStateList.forEach((var article) -> article.setSelectionsLeft(article.getMultiplier()));
        }
        if (targetStateList == usedRegularArticles || targetStateList == usedToppingArticles || targetStateList == usedFillerArticles)
        {
            sourceStateList.forEach((var article) -> article.setSelectionsLeft(0));
        }
        targetStateList.addAll(sourceStateList);
        sourceStateList.clear();
    }

    private void moveSimilarArticleToStateList(final ArticleEntity similarArticle ,final List<ArticleEntity> sourceStateList, final List<ArticleEntity> targetStateList)
    {
        for (var article : sourceStateList)
        {
            if (isNameTheSame(article, similarArticle))
            {
                if (sourceStateList == usedRegularArticles || sourceStateList == usedToppingArticles || sourceStateList == usedFillerArticles)
                {
                    article.setSelectionsLeft(article.getMultiplier());
                }
                if (targetStateList == usedRegularArticles || targetStateList == usedToppingArticles || targetStateList == usedFillerArticles)
                {
                    article.setSelectionsLeft(0);
                }
                targetStateList.add(article);
                sourceStateList.remove(article);
                return;
            }
        }
    }

    private void priorityAddAll(Set<ArticleEntity> prioritySet, final List<ArticleEntity> articles)
    {
        articles.forEach((var article) -> article.setHasPriority(true));
        prioritySet.addAll(articles);
    }

    private void priorityRemoveIf(Set<ArticleEntity> prioritySet, Predicate<? super ArticleEntity> filter)
    {
        prioritySet.stream().filter(filter).forEach((var article) -> article.setHasPriority(false));
        prioritySet.removeIf(filter);
    }

    private void addArticlesToFittingStateList(final List<ArticleEntity> articles)
    {
        for (var article: articles)
        {
            if (!article.isAvailable())
            {
                continue;
            }
            // Get article type dependant lists:
            List<ArticleEntity> usedList;
            List<ArticleEntity> selectableList;
            Set<ArticleEntity> prioritySet;
            final var type = article.getType();
            if (type.isRegular())
            {
                usedList = usedRegularArticles;
                selectableList = selectableRegularArticles;
                prioritySet = priorityRegularArticles;
            }
            else if (type == Type.FILLER)
            {
                usedList = usedFillerArticles;
                selectableList = selectableFillerArticles;
                prioritySet = null;
            }
            else if (type == Type.TOPPING)
            {
                usedList = usedToppingArticles;
                selectableList = selectableToppingArticles;
                prioritySet = null;
            }
            else
            {
                continue;
            }
            // Add article to fitting lists:
            if (article.getSelectionsLeft() == 0)
            {
                usedList.add(article);
            }
            else
            {
                selectableList.add(article);
            }
            if (article.getHasPriority())
            {
                assert prioritySet != null;
                prioritySet.add(article);
            }
        }
    }

    private void refreshStateLists()
    {
        final var availableArticles = getAvailableArticles();
        // Clear state lists that could be outdated:
        usedFillerArticles.clear();
        selectableFillerArticles.clear();
        usedToppingArticles.clear();
        selectableToppingArticles.clear();
        usedRegularArticles.clear();
        selectableRegularArticles.clear();
        // Remove unavailable articles from not-cleared state lists:
        removeNonIntersectingElements(chosenToppingArticles, availableArticles);
        removeNonIntersectingElements(chosenRegularArticles, availableArticles);
        removeNonIntersectingElements(priorityRegularArticles, availableArticles);
        // Add available articles to fitting state lists that aren't in one:
        availableArticles.removeAll(chosenToppingArticles);
        availableArticles.removeAll(chosenRegularArticles);
        addArticlesToFittingStateList(availableArticles);
    }

    private void refreshCountInfo()
    {
        // Toppings:
        var count = 0;
        for (var article: selectableToppingArticles) { count += article.getSelectionsLeft(); }
        for (var article: chosenToppingArticles) { count += article.getSelectionsLeft(); }
        binding.setSelectableToppingAmount(count);
        count = 0;
        for (var article: usedToppingArticles) { count += (article.isAvailable() ? 1 : 0); }
        binding.setUsedToppingAmount(count);
        // Regulars:
        count = 0;
        for (var article: selectableRegularArticles) { count += article.getSelectionsLeft(); }
        for (var article: chosenRegularArticles) { count += article.getSelectionsLeft(); }
        binding.setSelectableRegularAmount(count);
        count = 0;
        for (var article: usedRegularArticles) { count += (article.isAvailable() ? 1 : 0); }
        binding.setUsedRegularAmount(count);
        count = 0;
        for (var article: priorityRegularArticles) { count += article.getSelectionsLeft(); }
        binding.setPriorityRegularAmount(count);
        // Fillers:
        count = 0;
        for (var article: selectableFillerArticles) { count += article.getSelectionsLeft(); }
        binding.setSelectableFillerAmount(count);
        count = 0;
        for (var article: usedFillerArticles) { count += (article.isAvailable() ? 1 : 0); }
        binding.setUsedFillerAmount(count);
    }

    private void refreshInventoryInfo()
    {
        final var availableArticles = getAvailableArticles();
        // Toppings:
        binding.setAvailableToppingAmount(Math.toIntExact(availableArticles.stream().filter(article -> article.getType() == Type.TOPPING).count()));
        binding.setRegisteredToppingAmount(Math.toIntExact(allArticles.stream().filter(article -> article.getType() == Type.TOPPING).count()));
        // Crunchy:
        binding.setAvailableCrunchyAmount(Math.toIntExact(availableArticles.stream().filter(article -> article.getType() == Type.CRUNCHY).count()));
        binding.setRegisteredCrunchyAmount(Math.toIntExact(allArticles.stream().filter(article -> article.getType() == Type.CRUNCHY).count()));
        // Tender:
        binding.setAvailableTenderAmount(Math.toIntExact(availableArticles.stream().filter(article -> article.getType() == Type.TENDER).count()));
        binding.setRegisteredTenderAmount(Math.toIntExact(allArticles.stream().filter(article -> article.getType() == Type.TENDER).count()));
        // Puffy:
        binding.setAvailablePuffyAmount(Math.toIntExact(availableArticles.stream().filter(article -> article.getType() == Type.PUFFY).count()));
        binding.setRegisteredPuffyAmount(Math.toIntExact(allArticles.stream().filter(article -> article.getType() == Type.PUFFY).count()));
        // Flaky:
        binding.setAvailableFlakyAmount(Math.toIntExact(availableArticles.stream().filter(article -> article.getType() == Type.FLAKY).count()));
        binding.setRegisteredFlakyAmount(Math.toIntExact(allArticles.stream().filter(article -> article.getType() == Type.FLAKY).count()));
        // Fillers:
        binding.setAvailableFillerAmount(Math.toIntExact(availableArticles.stream().filter(article -> article.getType() == Type.FILLER).count()));
        binding.setRegisteredFillerAmount(Math.toIntExact(allArticles.stream().filter(article -> article.getType() == Type.FILLER).count()));
    }

    private void storeArticles(final List<ArticleEntity> articles)
    {
        final var ITEMS_VERSION_LENGTH = 1;
        try
        {
            byte[] bytes;
            var dataOutputStream = new ByteArrayOutputStream();
            // Write items version into first two bytes (length and number) of output stream:
            dataOutputStream.write(ITEMS_VERSION_LENGTH);
            dataOutputStream.write(LATEST_ITEMS_VERSION);
            // Write data packages (first byte defines the length, rest is the data) from given articles into output stream:
            for (var article: articles)
            {
                bytes = article.toByteArray();
                dataOutputStream.write(bytes.length);
                dataOutputStream.write(bytes);

                if (bytes.length > 255) createErrorAlertDialog(this, "storeArticles", "Data size of an Article is too big, consider limiting allowed string sizes or use two bytes for data size");
            }
            dataOutputStream.close();
            // Transform output stream into array of bytes and store them in the file:
            FileOutputStream fileOutputStream = getApplicationContext().openFileOutput(ARTICLES_FILENAME, Context.MODE_PRIVATE);
            fileOutputStream.write(dataOutputStream.toByteArray());
            fileOutputStream.close();
        }
        catch (IOException e)
        {
            createErrorAlertDialog(this, "storeArticles", "Storing articles to " + ARTICLES_FILENAME + " failed");
            e.printStackTrace();
        }
    }

    private boolean loadArticles(final List<ArticleEntity> articles)
    {
        final var ITEMS_VERSION_LENGTH = 1;
        try
        {
            final var context = getApplicationContext();
            final List<String> fileNames = new ArrayList<>(Arrays.asList(context.fileList()));
            if (fileNames.contains(ARTICLES_FILENAME))
            {
                byte[] bytes;
                var fileInputStream = context.openFileInput(ARTICLES_FILENAME);
                int length;
                // Read items version from first two bytes if available and check against loaded version:
                var newItemsVersion = 0;
                if ((length = fileInputStream.read()) != -1 && length == ITEMS_VERSION_LENGTH)
                {
                    newItemsVersion = fileInputStream.read();
                }
                else
                {
                    //noinspection ResultOfMethodCallIgnored
                    fileInputStream.skip(-1);
                }
                if (newItemsVersion < loadedItemsVersion)
                {
                    fileInputStream.close();
                    return false;
                }
                loadedItemsVersion = newItemsVersion;
                // Read data packages (first byte defines the length, rest is the data) into an array of bytes and transform them into an article entity:
                while ((length = fileInputStream.read()) != -1)
                {
                    bytes = new byte[length];
                    //noinspection ResultOfMethodCallIgnored
                    fileInputStream.read(bytes);
                    articles.add(new ArticleEntity(bytes));
                }
                fileInputStream.close();
                return true;
            }
        }
        catch (IOException e)
        {
            createErrorAlertDialog(this, "loadArticles", "Loading articles from " + ARTICLES_FILENAME + " failed");
            e.printStackTrace();
        }
        return false;
    }

    private void storePreferences()
    {
        final var sharedPrefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        final var editor = sharedPrefs.edit();
        editor.putInt("sizeValue", sizeValue);
        editor.putInt("sugarValue", sugarValue);
        editor.putInt("articlesValue", articlesValue);
        editor.putInt("toppingsValue", toppingsValue);
        editor.putString("muesliMix", muesliMix != null ? new Gson().toJson(muesliMix) : null);
        editor.apply();
    }

    private void loadPreferences()
    {
        final var sharedPrefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sizeValue = sharedPrefs.getInt("sizeValue", binding.sizeSlider.getProgress());
        binding.sizeSlider.setProgress(sizeValue);
        sugarValue = sharedPrefs.getInt("sugarValue", binding.sugarSlider.getProgress());
        binding.sugarSlider.setProgress(sugarValue);
        articlesValue = sharedPrefs.getInt("articlesValue", binding.articlesSlider.getProgress());
        binding.articlesSlider.setProgress(articlesValue);
        toppingsValue = sharedPrefs.getInt("toppingsValue", binding.articlesSlider.getProgress());
        binding.toppingSlider.setProgress(toppingsValue);
        muesliMix = new Gson().fromJson(sharedPrefs.getString("muesliMix", null), MuesliMix.class);
    }

    private void hideKeyboard(final View view)
    {
        final var inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        Objects.requireNonNull(inputMethodManager).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setEditTextFocus(final View view, final boolean hasFocus)
    {
        if (!hasFocus)
        {
            // Hide keyboard when tapping out of edit text:
            hideKeyboard(view);
        }
    }

    private void updateItemsJsonString()
    {
        try
        {
            final var jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("version", LATEST_ITEMS_VERSION);
            jsonArray.put(jsonObject);
            for (var article: allArticles)
            {
                jsonArray.put(article.writeToJson());
            }
            itemsJsonString = jsonArray.toString(4);
        }
        catch (JSONException e)
        {
            createErrorAlertDialog(this, "updateItemsJsonString", "Update of itemsJsonString to values of allArticles failed, setting it to an empty string");
            itemsJsonString = "";
            e.printStackTrace();
        }
    }

    private int extractItemsJsonVersion(JSONArray jsonArray) throws JSONException
    {
        var jsonVersion = 0;
        if (jsonArray.length() > 0)
        {
            var firstJsonObject = jsonArray.getJSONObject(0);
            if (firstJsonObject.has("version"))
            {
                jsonVersion = firstJsonObject.getInt("version");
                jsonArray.remove(0);
            }
        }
        return jsonVersion;
    }

    private void mergeItemsJsonString()
    {
        try
        {
            var jsonArray = new JSONArray(itemsJsonString);
            final var newItemsJsonVersion = extractItemsJsonVersion(jsonArray);
            final var overwriteCurrent = newItemsJsonVersion > loadedItemsVersion;
            loadedItemsVersion = Math.max(newItemsJsonVersion, loadedItemsVersion);
            var hasNewItems = false;
            for (var i = 0; i < jsonArray.length(); i++)
            {
                var jsonObject = jsonArray.getJSONObject(i);
                var newArticle = new ArticleEntity(jsonObject);
                // Add json entry as new article if an entry with the same name doesn't exist already:
                if (allArticles.stream().noneMatch(article -> isNameTheSame(article, newArticle)))
                {
                    allArticles.add(newArticle);
                    addArticlesToFittingStateList(Collections.singletonList(newArticle));
                    hasNewItems = true;
                }
                // Else if overwrite is allowed replace the values of current article with the new one:
                else if (overwriteCurrent)
                {
                    Objects.requireNonNull(allArticles.stream().filter(article -> isNameTheSame(article, newArticle)).findFirst().orElse(null)).readFromJson(jsonObject);
                }
            }
            if (hasNewItems)
            {
                allArticles.sort(Comparator.comparing(article -> (article.getBrand() + article.getName())));
                availableArticlesAdapter.setArticles(allArticles);
            }
        }
        catch (JSONException e)
        {
            createErrorAlertDialog(this, "mergeItemsJsonString", "itemsJsonString is corrupted, resetting it to the values of allArticles");
            updateItemsJsonString();
            e.printStackTrace();
        }
    }

    private String readTextFile(final Uri targetUri) throws IOException
    {
        final var inputStream = getContentResolver().openInputStream(targetUri);
        final var bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)));
        final var stringBuilder = new StringBuilder();
        var line = bufferedReader.readLine();
        while (line != null)
        {
            stringBuilder.append(line);
            line = bufferedReader.readLine();
        }
        bufferedReader.close();
        inputStream.close();
        return stringBuilder.toString();
    }

    private void writeTextFile(final Uri targetUri, final String text) throws IOException
    {
        final var outputStream = getContentResolver().openOutputStream(targetUri, "w");
        final var bufferedWriter = new BufferedWriter(new OutputStreamWriter(Objects.requireNonNull(outputStream)));
        bufferedWriter.write(text);
        bufferedWriter.close();
        outputStream.close();
    }

    private class MuesliMix
    {
        private double targetWeight;
        private double targetSugar;
        private int regularArticlesCount;
        private double toppingPercentage;
        private final int toppingsCount;
        private int totalSpoons;
        private double totalWeight;
        private double totalSugar;
        private final List<IngredientEntity> ingredients;
        private ArticleEntity fillerArticle;

        public MuesliMix(final double targetWeight, final double targetSugar, final int regularArticlesCount, final double toppingPercentage, final int toppingsCount)
        {
            this.targetWeight = targetWeight;
            this.targetSugar = targetSugar;
            this.regularArticlesCount = regularArticlesCount;
            this.toppingPercentage = toppingPercentage;
            this.toppingsCount = toppingsCount;
            fillerArticle = null;
            ingredients = new ArrayList<>(regularArticlesCount + 1);
        }
        public MuesliMix(final MuesliMix muesliMix)
        {
            targetWeight = muesliMix.targetWeight;
            targetSugar = muesliMix.targetSugar;
            regularArticlesCount = muesliMix.regularArticlesCount;
            toppingPercentage = muesliMix.toppingPercentage;
            toppingsCount = muesliMix.toppingsCount;
            totalSpoons = muesliMix.totalSpoons;
            totalWeight = muesliMix.totalWeight;
            totalSugar = muesliMix.totalSugar;
            ingredients = new ArrayList<>(muesliMix.ingredients.size());
            ingredients.addAll(muesliMix.ingredients);
            fillerArticle = muesliMix.fillerArticle;
        }

        public boolean isFillerChosen()
        {
            if (this.fillerArticle != null)
            {
                for (var ingredient : this.ingredients)
                {
                    if (Objects.equals(ingredient.getName(), this.fillerArticle.getName()))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        public void changeTargetWeight(double targetWeight)
        {
            this.targetWeight = targetWeight;
            if (this.determineIngredients())
            {
                this.updateDisplayValid();
            }
            else
            {
                this.updateDisplayInvalid();
            }
        }

        public void changeTargetSugar(double targetSugar)
        {
            this.targetSugar = targetSugar;
            if (this.determineIngredients())
            {
                this.updateDisplayValid();
            }
            else
            {
                this.updateDisplayInvalid();
            }
        }

        public void changeRegularArticlesCount(int regularArticlesCount, Random random)
        {
            final var countChange = regularArticlesCount - this.regularArticlesCount;
            if (this.regularArticlesCount + countChange <= 0 || -countChange >= chosenRegularArticles.size() || countChange > selectableRegularArticles.size())
            {
                this.updateDisplayInvalid();
                return;
            }
            if (countChange > 0)
            {
                for (var i = 0; i < countChange; i++)
                {
                    chosenRegularArticles.add(selectableRegularArticles.remove(random.nextInt(selectableRegularArticles.size())));
                }
            }
            else if (countChange < 0)
            {
                for (var i = 0; i < -countChange; i++)
                {
                    selectableRegularArticles.add(chosenRegularArticles.remove(chosenRegularArticles.size() - 1));
                }
            }
            this.regularArticlesCount = regularArticlesCount;
            if (this.determineIngredients())
            {
                this.updateDisplayValid();
            }
            else
            {
                this.updateDisplayInvalid();
            }
        }

        public void changeToppingPercentage(double toppingPercentage)
        {
            this.toppingPercentage = toppingPercentage;
            if (this.determineIngredients())
            {
                this.updateDisplayValid();
            }
            else
            {
                this.updateDisplayInvalid();
            }
        }

        /** Return chosen articles back to the selectable pool. */
        public void resetArticlesPool()
        {
            if (!chosenToppingArticles.isEmpty())
            {
                moveArticlesToStateList(chosenToppingArticles, selectableToppingArticles);
            }
            if (!chosenRegularArticles.isEmpty())
            {
                moveArticlesToStateList(chosenRegularArticles, selectableRegularArticles);
            }
        }

        /** Chose articles for muesli from global lists. */
        public void choseArticles(final Random random)
        {
            fillerArticle = selectableFillerArticles.get(random.nextInt(selectableFillerArticles.size()));
            while (chosenToppingArticles.size() < toppingsCount)
            {
                chosenToppingArticles.add(selectableToppingArticles.remove(random.nextInt(selectableToppingArticles.size())));
            }
            if (!priorityRegularArticles.isEmpty())
            {
                // A stream with skip has to be used because priorityRegularArticles is a set and not a list
                chosenRegularArticles.add(selectableRegularArticles.remove(selectableRegularArticles.indexOf(priorityRegularArticles.stream().skip(random.nextInt(priorityRegularArticles.size())).findFirst().orElse(null))));
            }
            while (chosenRegularArticles.size() < regularArticlesCount)
            {
                final var randomArticle = selectableRegularArticles
                    .stream()
                    .filter(article -> chosenRegularArticles.stream().noneMatch(chosen -> chosen.getType() == article.getType()))
                    .collect(Collectors.collectingAndThen(Collectors.toList(),
                        list -> list.isEmpty() ? selectableRegularArticles.get(random.nextInt(selectableRegularArticles.size())) : list.get(random.nextInt(list.size()))));
                selectableRegularArticles.remove(randomArticle);
                chosenRegularArticles.add(randomArticle);
            }
        }

        /** Determine ingredients based on chosen articles and target values. */
        public boolean determineIngredients()
        {
            totalSpoons = 0;
            totalWeight = 0.;
            totalSugar = 0.;
            double weight;
            double sugarPartialSum = 0.;
            double toppingsWeight;
            ingredients.clear();
            ArticleEntity article;
            int spoonCount;
            // Calculate and add spoons for topping articles based on topping percentage:
            for (var i = 0; i < toppingsCount && toppingPercentage > 0; i++)
            {
                article = chosenToppingArticles.get(i);
                spoonCount = (int)Math.round(targetWeight * toppingPercentage / (article.getSpoonWeight() * toppingsCount));
                spoonCount = Math.max(spoonCount, 1);
                weight = spoonCount * article.getSpoonWeight();
                totalSpoons += spoonCount;
                totalWeight += weight;
                totalSugar += weight * article.getSugarPercentage();
                sugarPartialSum += weight * (article.getSugarPercentage() - fillerArticle.getSugarPercentage());
                ingredients.add(new IngredientEntity(article, spoonCount));
            }
            toppingsWeight = totalWeight;
            // Calculate and add spoons for non-last regular articles based on number of ingredients:
            for (var i = 0; i < regularArticlesCount - 1; i++)
            {
                article = chosenRegularArticles.get(i);
                spoonCount = (int)Math.round((targetWeight - toppingsWeight) / (article.getSpoonWeight() * (regularArticlesCount + FILLER_INGREDIENT_RATIO)));
                spoonCount = Math.max(spoonCount, 1);
                weight = spoonCount * article.getSpoonWeight();
                totalSpoons += spoonCount;
                totalWeight += weight;
                totalSugar += weight * article.getSugarPercentage();
                sugarPartialSum += weight * (article.getSugarPercentage() - fillerArticle.getSugarPercentage());
                ingredients.add(new IngredientEntity(article, spoonCount));
            }
            // Calculate and add spoons for last regular article based on sugar percentage:
            article = chosenRegularArticles.get(regularArticlesCount - 1);
            spoonCount = (int)Math.round((targetSugar - targetWeight * fillerArticle.getSugarPercentage() - sugarPartialSum) / (article.getSpoonWeight() * (article.getSugarPercentage() - fillerArticle.getSugarPercentage())));
            if (spoonCount <= 0) return false;
            weight = spoonCount * article.getSpoonWeight();
            totalSpoons += spoonCount;
            totalWeight += weight;
            totalSugar += weight * article.getSugarPercentage();
            ingredients.add(new IngredientEntity(article, spoonCount));
            // Calculate and add spoons for filler article based on total size:
            spoonCount = (int)Math.round((targetWeight - totalWeight) / fillerArticle.getSpoonWeight());
            if (spoonCount < 0) return false;
            if (spoonCount > 0)
            {
                weight = spoonCount * fillerArticle.getSpoonWeight();
                totalSpoons += spoonCount;
                totalWeight += weight;
                totalSugar += weight * fillerArticle.getSugarPercentage();
                ingredients.add(new IngredientEntity(fillerArticle, spoonCount));
            }
            return true;
        }

        /** Adjust mix buttons and ingredients list for valid settings. */
        @SuppressLint("NotifyDataSetChanged")
        public void updateDisplayValid()
        {
            binding.setTotalSpoonCount(String.format(Locale.getDefault(), "%d spoons", this.totalSpoons));
            binding.setTotalWeight(String.format(Locale.getDefault(), "%.1f", this.totalWeight));
            binding.setTotalSugarPercentage(String.format(Locale.getDefault(), "%.1f", 100 * this.totalSugar / this.totalWeight));
            ingredientsAdapter.setIngredients(this.ingredients);
            binding.setIsChosenMuesliUsed(isChosenMuesliUsed = false);
            binding.setIsIngredientsListEmpty(false);
            binding.setIsInvalidSettings(false);
            ingredientsAdapter.notifyDataSetChanged();
        }

        /** Adjust mix buttons and ingredients list for invalid settings. */
        public void updateDisplayInvalid()
        {
            binding.setIsChosenMuesliUsed(isChosenMuesliUsed = true);
            binding.setIsIngredientsListEmpty(true);
            binding.setIsInvalidSettings(true);
        }
    }

    public void onRadioButtonClicked(@NonNull View view)
    {
        final var id = view.getId();
        if (id == R.id.mixMuesliButton)
        {
            userMode = UserMode.MIX_MUESLI;
        }
        else if (id == R.id.availabilityButton)
        {
            userMode = UserMode.AVAILABILITY;
            refreshData(true);
        }
        else if (id == R.id.editItemsButton)
        {
            userMode = UserMode.EDIT_ITEMS;
            refreshData(true);
        }
        binding.setUserMode(userMode);
        binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        final var random = new Random();
        binding = DataBindingUtil.setContentView(this, R.layout.main_screen);
        loadPreferences();

        // Setup activity result launcher for document handling:
        ActivityResultCallback<ActivityResult> createFileActivityCallback = result ->
        {
            try
            {
                final var resultData = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && resultData != null)
                {
                    final var targetUri = resultData.getData();
                    assert targetUri != null;
                    updateItemsJsonString();
                    writeTextFile(targetUri, itemsJsonString);
                }
            }
            catch (IOException e)
            {
                createErrorAlertDialog(this, "createFileActivityCallback", "Export request failed");
                e.printStackTrace();
            }
        };
        createFileActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), createFileActivityCallback);
        ActivityResultCallback<ActivityResult> openFileActivityCallback = result ->
        {
            try
            {
                final var resultData = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && resultData != null)
                {
                    final var targetUri = resultData.getData();
                    assert targetUri != null;
                    itemsJsonString = readTextFile(targetUri);
                    mergeItemsJsonString();
                }
            }
            catch (IOException e)
            {
                createErrorAlertDialog(this, "createFileActivityCallback", "Import request failed");
                e.printStackTrace();
            }
        };
        openFileActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), openFileActivityCallback);

        // Setup adapters and layout managers:
        ingredientsAdapter = new IngredientsAdapter(this);
        binding.ingredients.setAdapter(ingredientsAdapter);
        binding.ingredients.setLayoutManager(new LinearLayoutManager(this));
        availableArticlesAdapter = new ArticlesAdapter(this);
        binding.availableArticles.setAdapter(availableArticlesAdapter);
        binding.availableArticles.setLayoutManager(new LinearLayoutManager(this));
        final var typeSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Type.values());
        binding.typeSpinner.setAdapter(typeSpinnerAdapter);

        // Load all articles and add them to the fitting state lists:
        if (loadArticles(allArticles))
        {
            availableArticlesAdapter.setArticles(allArticles);
        }
        try
        {
            final var resources = this.getResources();
            itemsJsonString = readTextFile(new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE).authority(resources.getResourcePackageName(DEFAULT_ARTICLES_RES_ID)).appendPath(resources.getResourceTypeName(DEFAULT_ARTICLES_RES_ID)).appendPath(resources.getResourceEntryName(DEFAULT_ARTICLES_RES_ID)).build());
            mergeItemsJsonString();
        }
        catch (IOException e)
        {
            createErrorAlertDialog(this, "onCreate", "Default muesli item data import failed");
            e.printStackTrace();
        }
        addArticlesToFittingStateList(allArticles);
        if (!selectableRegularArticles.isEmpty() && !selectableFillerArticles.isEmpty() && getLowestValue(this, selectableRegularArticles, ArticleEntity::getSugarPercentage) <= getLowestValue(this, selectableFillerArticles, ArticleEntity::getSugarPercentage)) createErrorAlertDialog(this, "onCreate", "Sugar percentage of all filler articles has to be lower than that of regular articles to get valid mixes.");

        // Init layout variables:
        binding.setUserMode(userMode);
        binding.typeSpinner.setSelection(typeSpinnerAdapter.getPosition(Type.CRUNCHY));
        binding.setNewArticle(new ArticleEntity("", "", (Type)binding.typeSpinner.getSelectedItem(), 0F, 0F));
        refreshCountInfo();
        binding.setSizeWeight(String.format(Locale.getDefault(), "%.0f", sizeValue2SizeWeight(sizeValue)));
        binding.setSugarPercentage(String.format(Locale.getDefault(), "%.1f", sugarValue2SugarPercentage(sugarValue) * 100));
        binding.setArticlesCount(articlesValue2ArticlesCount(articlesValue));
        binding.setToppingPercentage(String.format(Locale.getDefault(), "%.0f", toppingValue2ToppingPercentage(toppingsValue) * 100));
        binding.setIsChosenMuesliUsed(isChosenMuesliUsed);
        binding.setIsIngredientsListEmpty(true);
        binding.setIsInvalidSettings(false);
        binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
        // Load in stored mix if available:
        if (muesliMix != null)
        {
            muesliMix = new MuesliMix(muesliMix);  // Has to be cloned to have access to the MainActivity variables
            isChosenMuesliUsed = false;
            muesliMix.updateDisplayValid();
            // Move stored ingredients from selectable to chosen article lists:
            for (var ingredient : muesliMix.ingredients)
            {
                // Move fitting topping article:
                if (selectableToppingArticles.stream()
                        .filter(ingredient::isSimilarArticle)
                        .findFirst()
                        .map(article ->
                        {
                            selectableToppingArticles.remove(article);
                            chosenToppingArticles.add(article);
                            return true;
                        }).orElse(false))
                {
                    continue;
                }
                // Move fitting regular article:
                if (selectableRegularArticles.stream()
                        .filter(ingredient::isSimilarArticle)
                        .findFirst()
                        .map(article ->
                        {
                            selectableRegularArticles.remove(article);
                            chosenRegularArticles.add(article);
                            return true;
                        }).orElse(false))
                {
                    continue;
                }
                // Ignore filler article:
                if (!ingredient.isSimilarArticle(muesliMix.fillerArticle))
                {
                    createErrorAlertDialog(this, "onCreate", "Could not find stored ingredient '" + ingredient.getName() + "' in selectable articles");
                }
            }
        }

        // Create slider and button listeners:
        binding.sizeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                sizeValue = progress;
                if (muesliMix != null)
                {
                    muesliMix.changeTargetWeight(sizeValue2SizeWeight(sizeValue));
                }
                else
                {
                    ingredientsAdapter.setIngredients(new ArrayList<>(0));
                    ingredientsAdapter.notifyDataSetChanged();
                    binding.setIsIngredientsListEmpty(true);
                }
                binding.setSizeWeight(String.format(Locale.getDefault(), "%.0f", sizeValue2SizeWeight(sizeValue)));
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                storePreferences();
            }
        });
        binding.sugarSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                sugarValue = progress;
                if (muesliMix != null)
                {
                    muesliMix.changeTargetSugar(sugarValue2SugarPercentage(sugarValue) * muesliMix.targetWeight);
                }
                else
                {
                    ingredientsAdapter.setIngredients(new ArrayList<>(0));
                    ingredientsAdapter.notifyDataSetChanged();
                    binding.setIsIngredientsListEmpty(true);
                }
                binding.setSugarPercentage(String.format(Locale.getDefault(), "%.1f", sugarValue2SugarPercentage(sugarValue) * 100));
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                storePreferences();
            }
        });
        binding.articlesSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                articlesValue = progress;
                if (muesliMix != null)
                {
                    muesliMix.changeRegularArticlesCount(articlesValue2ArticlesCount(articlesValue), random);
                }
                else
                {
                    ingredientsAdapter.setIngredients(new ArrayList<>(0));
                    ingredientsAdapter.notifyDataSetChanged();
                    binding.setIsIngredientsListEmpty(true);
                }
                binding.setArticlesCount(articlesValue2ArticlesCount(articlesValue));
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                storePreferences();
            }
        });
        binding.toppingSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                toppingsValue = progress;
                if (muesliMix != null)
                {
                    muesliMix.changeToppingPercentage(toppingValue2ToppingPercentage(toppingsValue));
                }
                else
                {
                    ingredientsAdapter.setIngredients(new ArrayList<>(0));
                    ingredientsAdapter.notifyDataSetChanged();
                    binding.setIsIngredientsListEmpty(true);
                }
                binding.setToppingPercentage(String.format(Locale.getDefault(), "%.0f", toppingValue2ToppingPercentage(toppingsValue) * 100));
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                storePreferences();
            }
        });
        binding.typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id)
            {
                final var selectedType = (Type)(adapterView.getItemAtPosition(position));
                final var newArticle = (ArticleEntity)binding.getNewArticle();
                newArticle.setType(selectedType);
                binding.setNewArticle(newArticle);  // Required to update weight field hint
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        binding.addButton.setOnClickListener((View view) ->
        {
            final var newArticle = (ArticleEntity)binding.getNewArticle();
            // Check for name duplicate:
            final var isDuplicate = allArticles.stream().anyMatch(article -> isNameTheSame(article, newArticle));
            // Check for empty name or brand:
            final var newName = newArticle.getName();
            final var newBrand = newArticle.getBrand();
            final var hasEmptyField = (newName == null || Objects.equals(newName, "") || newBrand == null || Objects.equals(newBrand, ""));
            // Add and sort in new article, update adapter and scroll to its position:
            if (!isDuplicate && !hasEmptyField)
            {
                allArticles.add(newArticle);
                addArticlesToFittingStateList(Collections.singletonList(newArticle));
                allArticles.sort(Comparator.comparing(article -> (article.getBrand() + article.getName())));
                availableArticlesAdapter.setArticles(allArticles);
                binding.availableArticles.smoothScrollToPosition(allArticles.indexOf(newArticle));
                binding.setNewArticle(new ArticleEntity("", "", newArticle.getType(), 0F, 0F));
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
            }
            else
            {
                Log.i("onCreate", "Cannot add empty or duplicate muesli name");
            }
        });
        binding.importButton.setOnClickListener((View view) -> openFileActivityLauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(INTENT_TYPE_JSON)));
        binding.exportButton.setOnClickListener((View view) -> createFileActivityLauncher.launch(new Intent(Intent.ACTION_CREATE_DOCUMENT).putExtra(Intent.EXTRA_TITLE, EXPORT_ITEMS_FILENAME + ".json").addCategory(Intent.CATEGORY_OPENABLE).setType(INTENT_TYPE_JSON)));
        binding.nameField.setOnFocusChangeListener(this::setEditTextFocus);
        binding.brandField.setOnFocusChangeListener(this::setEditTextFocus);
        binding.weightField.setOnFocusChangeListener(this::setEditTextFocus);
        binding.percentageField.setOnFocusChangeListener(this::setEditTextFocus);
        binding.newButton.setOnClickListener((View view) ->
        {
            final var targetWeight = sizeValue2SizeWeight(sizeValue);
            final var targetSugar = sugarValue2SugarPercentage(sugarValue) * targetWeight;
            final var regularArticlesCount = articlesValue2ArticlesCount(articlesValue);
            final var toppingPercentage = toppingValue2ToppingPercentage(toppingsValue);
            final var toppingArticlesCount = TOPPINGS_INGREDIENT_COUNT;
            muesliMix = new MuesliMix(targetWeight, targetSugar, regularArticlesCount, toppingPercentage, toppingArticlesCount);
            muesliMix.resetArticlesPool();
            ingredientsAdapter.setIngredients(new ArrayList<>(0));
            refreshData(false);

            // Return used articles back to the selectable pool if necessary:
            if (selectableFillerArticles.isEmpty())
            {
                moveArticlesToStateList(usedFillerArticles, selectableFillerArticles);
            }
            if (selectableToppingArticles.size() < toppingArticlesCount)
            {
                moveArticlesToStateList(usedToppingArticles, selectableToppingArticles);
            }
            if (selectableRegularArticles.size() < regularArticlesCount)
            {
                priorityAddAll(priorityRegularArticles, selectableRegularArticles);
                moveArticlesToStateList(usedRegularArticles, selectableRegularArticles);
            }

            // Check general conditions for valid mix:
            if (selectableFillerArticles.isEmpty() || selectableRegularArticles.size() < regularArticlesCount || selectableToppingArticles.size() < toppingArticlesCount)
            {
                muesliMix.updateDisplayInvalid();
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                muesliMix = null;
                Log.i("onCreate", "Not enough available articles for a valid mix");
                return;
            }

            // Retry with randomized ingredients until a valid mix is found:
            var fullResetTryCounter = 0;
            while (fullResetTryCounter <= MAX_FULL_RESET_RANDOMIZE_TRIES)
            {
                for (var tryCounter = 0; tryCounter < MAX_RANDOMIZE_TRIES; tryCounter++)
                {
                    // Randomly chose new mix of articles of current pool:
                    muesliMix.resetArticlesPool();
                    muesliMix.choseArticles(random);
                    // Determine mix with chosen articles and retry loop if it's invalid:
                    if (!muesliMix.determineIngredients()) continue;
                    // Valid mix could be determined, display result and exit search:
                    muesliMix.updateDisplayValid();
                    refreshCountInfo();
                    binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                    Log.i("onCreate", "tryCounter: " + tryCounter);
                    return;
                }
                // Return used and chosen articles back to the selectable pool and update priority choosing:
                moveArticlesToStateList(usedFillerArticles, selectableFillerArticles);
                moveArticlesToStateList(usedToppingArticles, selectableToppingArticles);
                moveArticlesToStateList(chosenToppingArticles, selectableToppingArticles);
                priorityAddAll(priorityRegularArticles, selectableRegularArticles);
                priorityAddAll(priorityRegularArticles, chosenRegularArticles);
                moveArticlesToStateList(usedRegularArticles, selectableRegularArticles);
                moveArticlesToStateList(chosenRegularArticles, selectableRegularArticles);
                Log.i("onCreate", "Cannot find valid mix with selectable articles, retrying with full reset");
                fullResetTryCounter += 1;
            }
            // No valid mix could be found:
            muesliMix.updateDisplayInvalid();
            refreshCountInfo();
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
            muesliMix = null;
        });
        binding.useButton.setOnClickListener((View view) ->
        {
            if (chosenToppingArticles.isEmpty()) createErrorAlertDialog(this, "onCreate", "chosenToppingArticles is empty");
            if (chosenRegularArticles.isEmpty()) createErrorAlertDialog(this, "onCreate", "chosenRegularArticles is empty");

            // Decrement selections left, move articles to fitting pools and reset priority choosing:
            if (muesliMix.isFillerChosen())
            {
                selectableFillerArticles.remove(muesliMix.fillerArticle);
                muesliMix.fillerArticle.decrementSelectionsLeft();
                addArticlesToFittingStateList(Collections.singletonList(muesliMix.fillerArticle));
                muesliMix.fillerArticle = null;
            }
            if (muesliMix.toppingPercentage > 0)
            {
                chosenToppingArticles.forEach(ArticleEntity::decrementSelectionsLeft);
            }
            addArticlesToFittingStateList(chosenToppingArticles);
            chosenToppingArticles.clear();
            chosenRegularArticles.forEach(ArticleEntity::decrementSelectionsLeft);
            priorityRemoveIf(priorityRegularArticles, t -> (chosenRegularArticles.contains(t) && t.getSelectionsLeft() == 0));
            addArticlesToFittingStateList(chosenRegularArticles);
            chosenRegularArticles.clear();

            // Adjust mix buttons and ingredients list:
            ingredientsAdapter.actOnMarkedIngredients();
            binding.setIsChosenMuesliUsed(isChosenMuesliUsed = true);
            refreshCountInfo();
            ingredientsAdapter.notifyDataSetChanged();
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes
            muesliMix = null;

            // Store updated articles in memory:
            storeArticles(allArticles);
        });
        binding.clearButton.setOnClickListener((View view) ->
        {
            // Return chosen articles back to the selectable pool if necessary:
            if (!chosenToppingArticles.isEmpty())
            {
                moveArticlesToStateList(chosenToppingArticles, selectableToppingArticles);
            }
            if (!chosenRegularArticles.isEmpty())
            {
                moveArticlesToStateList(chosenRegularArticles, selectableRegularArticles);
            }

            // Adjust mix buttons and ingredients list:
            ingredientsAdapter.setIngredients(new ArrayList<>(0));
            binding.setIsChosenMuesliUsed(isChosenMuesliUsed = true);
            binding.setIsIngredientsListEmpty(true);
            refreshData(false);
            ingredientsAdapter.notifyDataSetChanged();
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes
            muesliMix = null;

            // Store updated articles in memory:
            storeArticles(allArticles);
        });
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // Store current mix, updated articles and preferences in memory:
        storeArticles(allArticles);
        storePreferences();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        // Update and store articles in memory (required for emptied articles):
        if (muesliMix == null)
        {
            ingredientsAdapter.setIngredients(new ArrayList<>(0));
            refreshStateLists();
            storeArticles(allArticles);
        }
    }
}
