package com.romanbrunner.apps.mueslirandomizer;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import com.romanbrunner.apps.mueslirandomizer.databinding.MainScreenBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

import static com.romanbrunner.apps.mueslirandomizer.ArticleEntity.*;


public class MainActivity extends AppCompatActivity
{
    // --------------------
    // Data code
    // --------------------

    private final static float FILLER_INGREDIENT_RATIO = 0.5F;  // Ratio compared to first regular article, 1 being equal ratio
    private final static float TOPPINGS_INGREDIENT_RATIO = 0.1F;  // Ratio of whole mix
    private final static int MAX_RANDOMIZE_TRIES = 1024;
    private final static String INTENT_TYPE_JSON = "*/*";  // No MIME type for json yet, thus allowing every file
    private final static String ARTICLES_FILENAME = "AllArticles";
    private final static String PREFS_NAME = "GlobalPreferences";
    private final static String EXPORT_ITEMS_FILENAME = "MuesliItemsData";
    private final static int DEFAULT_ARTICLES_RES_ID = R.raw.default_muesli_items_data;

    private static float sizeValue2SizeWeight(int sizeValue)
    {
        return 35F + 15F * sizeValue;
    }

    private static float sugarValue2SugarPercentage(int sugarValue)
    {
        return 0.1F + 0.025F * sugarValue;
    }

    private static int articlesValue2ArticlesCount(int articlesValue)
    {
        return articlesValue + 1;
    }

    private static int toppingsValue2ToppingsCount(int toppingsValue)
    {
        return toppingsValue;
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
    private final List<ArticleEntity> fillerArticles = new LinkedList<>();  // All available filler type articles, separate from the other lists with only regular articles
    private final List<ArticleEntity> toppingArticles = new LinkedList<>();  // All available topping type articles, separate from the other lists with only regular articles
    private final List<ArticleEntity> selectableArticles = new LinkedList<>();  // Selectable articles for the next muesli mix creation
    private final List<ArticleEntity> usedArticles = new LinkedList<>();  // Used articles, will be reshuffled into selectableArticles once that is depleted
    private final List<ArticleEntity> chosenArticles = new LinkedList<>();  // Chosen articles for the current muesli mix creation
    private final List<ArticleEntity> priorityChoosing = new LinkedList<>();  // Remaining articles that have to be chosen for the next muesli mix creation, articles are also in selectableArticles
    private IngredientsAdapter ingredientsAdapter;
    private ArticlesAdapter availableArticlesAdapter;
    private MainScreenBinding binding;
    private int sizeValue;
    private int sugarValue;
    private int articlesValue;
    private int toppingsValue;
    private String itemsJsonString;
    private ActivityResultLauncher<Intent> createFileActivityLauncher;
    private ActivityResultLauncher<Intent> openFileActivityLauncher;

    private static <T> double getLowestValue(final List<T> list, final Function<T, Double> getter)
    {
        if (list.isEmpty())
        {
            Log.e("getLowestValue", "Cannot get value for an empty list");
        }

        double lowestValue = getter.apply(list.get(0));
        for (int i = 1; i < list.size(); i++)
        {
            lowestValue = Math.min(lowestValue, getter.apply(list.get(i)));
        }
        return lowestValue;
    }

    private static <T> void removeNonIntersectingElements(final List<T> targetList, final List<T> checkList)
    {
        targetList.removeIf(t -> !checkList.contains(t));
    }

    public void refreshData(boolean reloadArticleAdapter)
    {
        refreshStateLists();
        refreshCountInfo();
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
        List<ArticleEntity> availableArticles = new LinkedList<>();
        for (ArticleEntity article: allArticles)
        {
            if (article.isAvailable())
            {
                availableArticles.add(article);
            }
        }
        return availableArticles;
    }

    private void moveArticlesToStateList(final List<ArticleEntity> sourceStateList, final List<ArticleEntity> targetStateList)
    {
        if (sourceStateList == usedArticles)
        {
            sourceStateList.forEach((ArticleEntity article) -> article.setSelectionsLeft(article.getMultiplier()));
        }
        if (targetStateList == usedArticles)
        {
            sourceStateList.forEach((ArticleEntity article) -> article.setSelectionsLeft(0));
        }
        targetStateList.addAll(sourceStateList);
        sourceStateList.clear();
    }

    private void addArticlesToFittingStateList(final List<ArticleEntity> articles)
    {
        for (ArticleEntity article: articles)
        {
            switch (article.getType())
            {
                case FILLER:
                    if (article.isAvailable())
                    {
                        fillerArticles.add(article);
                    }
                    break;
                case TOPPING:
                    if (article.isAvailable())
                    {
                        toppingArticles.add(article);
                    }
                    break;
                case REGULAR:
                    if (article.isAvailable())
                    {
                        if (article.getSelectionsLeft() == 0)
                        {
                            usedArticles.add(article);
                        }
                        else
                        {
                            selectableArticles.add(article);
                        }
                    }
                    break;
            }
        }
    }

    private void refreshStateLists()
    {
        final List<ArticleEntity> availableArticles = getAvailableArticles();
        // Clear state lists that could be outdated:
        usedArticles.clear();
        selectableArticles.clear();
        // Remove unavailable articles from uncleared state lists:
        removeNonIntersectingElements(fillerArticles, availableArticles);
        removeNonIntersectingElements(toppingArticles, availableArticles);
        removeNonIntersectingElements(chosenArticles, availableArticles);
        removeNonIntersectingElements(priorityChoosing, availableArticles);
        // Add available articles to fitting state lists that aren't in one:
        availableArticles.removeAll(fillerArticles);
        availableArticles.removeAll(toppingArticles);
        availableArticles.removeAll(chosenArticles);
        addArticlesToFittingStateList(availableArticles);
    }

    private void refreshCountInfo()
    {
        binding.setUsedAmount(usedArticles.size());
        int count = 0;
        for (ArticleEntity article: selectableArticles) { count += article.getSelectionsLeft(); }
        for (ArticleEntity article: chosenArticles) { count += article.getSelectionsLeft(); }
        binding.setSelectableAmount(count);
        count = 0;
        for (ArticleEntity article: priorityChoosing) { count += article.getSelectionsLeft(); }
        binding.setPriorityAmount(count);
        binding.setFillerAmount(fillerArticles.size());
        binding.setToppingAmount(toppingArticles.size());
    }

    private void storeArticles(final List<ArticleEntity> articles)
    {
        try
        {
            byte[] bytes;
            ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();
            for (ArticleEntity article: articles)
            {
                bytes = article.toByteArray();
                dataOutputStream.write(bytes.length);
                dataOutputStream.write(bytes);

                if (bytes.length > 255)
                {
                    Log.e("onPause", "Data size of an Article is too big, consider limiting allowed string sizes or use two bytes for data size");
                }
            }
            dataOutputStream.close();
            FileOutputStream fileOutputStream = getApplicationContext().openFileOutput(ARTICLES_FILENAME, Context.MODE_PRIVATE);
            fileOutputStream.write(dataOutputStream.toByteArray());
            fileOutputStream.close();
        }
        catch (IOException e)
        {
            Log.e("storeArticles", "Storing articles to " + ARTICLES_FILENAME + " failed");
            e.printStackTrace();
        }
    }

    private boolean loadArticles(final List<ArticleEntity> articles)
    {
        try
        {
            final Context context = getApplicationContext();
            final List<String> fileNames = new ArrayList<>(Arrays.asList(context.fileList()));
            if (fileNames.contains(ARTICLES_FILENAME))
            {
                byte[] bytes;
                FileInputStream fileInputStream = context.openFileInput(ARTICLES_FILENAME);
                int length;
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
            Log.e("loadArticles", "Loading articles from " + ARTICLES_FILENAME + " failed");
            e.printStackTrace();
        }
        return false;
    }

    private void storePreferences()
    {
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt("sizeValue", sizeValue);
        editor.putInt("sugarValue", sugarValue);
        editor.putInt("articlesValue", articlesValue);
        editor.putInt("toppingsValue", toppingsValue);
        editor.apply();
    }

    private void loadPreferences()
    {
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        sizeValue = sharedPrefs.getInt("sizeValue", binding.sizeSlider.getProgress());
        sugarValue = sharedPrefs.getInt("sugarValue", binding.sugarSlider.getProgress());
        articlesValue = sharedPrefs.getInt("articlesValue", binding.articlesSlider.getProgress());
        toppingsValue = sharedPrefs.getInt("toppingsValue", binding.articlesSlider.getProgress());
        binding.sizeSlider.setProgress(sizeValue);
        binding.sugarSlider.setProgress(sugarValue);
        binding.articlesSlider.setProgress(articlesValue);
        binding.toppingsSlider.setProgress(toppingsValue);
    }

    private void hideKeyboard(final View view)
    {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
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
            final JSONArray jsonArray = new JSONArray();
            for (ArticleEntity article: allArticles)
            {
                jsonArray.put(article.writeToJson());
            }
            itemsJsonString = jsonArray.toString(4);
        }
        catch (JSONException e)
        {
            Log.e("updateItemsJsonString", "Update of itemsJsonString to values of allArticles failed, setting it to an empty string");
            itemsJsonString = "";
            e.printStackTrace();
        }
    }

    private void mergeItemsJsonString()
    {
        try
        {
            final JSONArray jsonArray = new JSONArray(itemsJsonString);
            boolean hasNewItems = false;
            for (int i = 0; i < jsonArray.length(); i++)
            {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ArticleEntity newArticle = new ArticleEntity(jsonObject);
                // Add json entry as new article if an entry with the same name doesn't exist already:
                if (allArticles.stream().noneMatch(article -> isNameTheSame(article, newArticle)))
                {
                    allArticles.add(newArticle);
                    addArticlesToFittingStateList(Collections.singletonList(newArticle));
                    hasNewItems = true;
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
            Log.e("mergeItemsJsonString", "itemsJsonString is corrupted, resetting it to the values of allArticles");
            updateItemsJsonString();
            e.printStackTrace();
        }
    }

    private String readTextFile(final Uri targetUri) throws IOException
    {
        final InputStream inputStream = getContentResolver().openInputStream(targetUri);
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)));
        final StringBuilder stringBuilder = new StringBuilder();
        String line = bufferedReader.readLine();
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
        final OutputStream outputStream = getContentResolver().openOutputStream(targetUri, "w");
        final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(Objects.requireNonNull(outputStream)));
        bufferedWriter.write(text);
        bufferedWriter.close();
        outputStream.close();
    }

    public void onRadioButtonClicked(@NonNull View view)
    {
        final int id = view.getId();
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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        final Random random = new Random();
        binding = DataBindingUtil.setContentView(this, R.layout.main_screen);
        loadPreferences();

        // Setup activity result launcher for document handling:
        ActivityResultCallback<ActivityResult> createFileActivityCallback = result ->
        {
            try
            {
                final Intent resultData = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && resultData != null)
                {
                    final Uri targetUri = resultData.getData();
                    assert targetUri != null;
                    updateItemsJsonString();
                    writeTextFile(targetUri, itemsJsonString);
                }
            }
            catch (IOException e)
            {
                Log.e("createFileActivityCallback", "Export request failed");
                e.printStackTrace();
            }
        };
        createFileActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), createFileActivityCallback);
        ActivityResultCallback<ActivityResult> openFileActivityCallback = result ->
        {
            try
            {
                final Intent resultData = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && resultData != null)
                {
                    final Uri targetUri = resultData.getData();
                    assert targetUri != null;
                    itemsJsonString = readTextFile(targetUri);
                    mergeItemsJsonString();
                }
            }
            catch (IOException e)
            {
                Log.e("createFileActivityCallback", "Import request failed");
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
        final ArrayAdapter<Type> typeSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Type.values());
        binding.typeSpinner.setAdapter(typeSpinnerAdapter);

        // Load all articles and add them to the fitting state lists:
        if (loadArticles(allArticles))
        {
            availableArticlesAdapter.setArticles(allArticles);
        }
        try
        {
            final Resources resources = this.getResources();
            itemsJsonString = readTextFile(new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE).authority(resources.getResourcePackageName(DEFAULT_ARTICLES_RES_ID)).appendPath(resources.getResourceTypeName(DEFAULT_ARTICLES_RES_ID)).appendPath(resources.getResourceEntryName(DEFAULT_ARTICLES_RES_ID)).build());
            mergeItemsJsonString();
        }
        catch (IOException e)
        {
            Log.e("onCreate", "Default muesli item data import failed");
            e.printStackTrace();
        }
        addArticlesToFittingStateList(allArticles);
        if (!selectableArticles.isEmpty() && !fillerArticles.isEmpty() && getLowestValue(selectableArticles, ArticleEntity::getSugarPercentage) <= getLowestValue(fillerArticles, ArticleEntity::getSugarPercentage)) throw new AssertionError("Sugar percentage of all filler articles has to be lower than that of regular articles");

        // Init layout variables:
        binding.setUserMode(userMode);
        binding.typeSpinner.setSelection(typeSpinnerAdapter.getPosition(Type.REGULAR));
        binding.setNewArticle(new ArticleEntity("", "", (Type)binding.typeSpinner.getSelectedItem(), 0F, 0F));
        refreshCountInfo();
        binding.setSizeWeight(String.format(Locale.getDefault(), "%.0f", sizeValue2SizeWeight(sizeValue)));
        binding.setSugarPercentage(String.format(Locale.getDefault(), "%.1f", sugarValue2SugarPercentage(sugarValue) * 100));
        binding.setArticlesCount(articlesValue2ArticlesCount(articlesValue));
        binding.setToppingsCount(toppingsValue2ToppingsCount(toppingsValue));
        binding.setIsChosenMuesliUsed(isChosenMuesliUsed);
        binding.setIsIngredientsListEmpty(true);
        binding.setIsInvalidSettings(false);
        binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync

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
                binding.setArticlesCount(articlesValue2ArticlesCount(articlesValue));
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                storePreferences();
            }
        });
        binding.toppingsSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                toppingsValue = progress;
                binding.setToppingsCount(toppingsValue2ToppingsCount(toppingsValue));
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                storePreferences();
            }
        });
        binding.typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id)
            {
                final Type selectedType = (Type)(adapterView.getItemAtPosition(position));
                final ArticleEntity newArticle = (ArticleEntity)binding.getNewArticle();
                newArticle.setType(selectedType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        binding.addButton.setOnClickListener((View view) ->
        {
            final ArticleEntity newArticle = (ArticleEntity)binding.getNewArticle();
            // Check for name duplicate:
            final boolean isDuplicate = allArticles.stream().anyMatch(article -> isNameTheSame(article, newArticle));
            // Check for empty name or brand:
            final String newName = newArticle.getName();
            final String newBrand = newArticle.getBrand();
            final boolean hasEmptyField = (newName == null || Objects.equals(newName, "") || newBrand == null || Objects.equals(newBrand, ""));
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
            final double targetWeight = sizeValue2SizeWeight(sizeValue);
            final double targetSugar = sugarValue2SugarPercentage(sugarValue) * targetWeight;
            final int regularArticlesCount = articlesValue2ArticlesCount(articlesValue);
            final int toppingsCount = toppingsValue2ToppingsCount(toppingsValue);

            // Return used articles back to the selectable pool if necessary:
            if (selectableArticles.size() + chosenArticles.size() < regularArticlesCount)
            {
                priorityChoosing.addAll(selectableArticles);
                priorityChoosing.addAll(chosenArticles);
                moveArticlesToStateList(usedArticles, selectableArticles);
            }

            // Check general conditions for valid mix:
            if (fillerArticles.size() <= 0 || selectableArticles.size() + chosenArticles.size() < regularArticlesCount || toppingArticles.size() < toppingsCount)
            {
                // Adjust mix buttons and ingredients list:
                Log.i("onCreate", "Not enough available articles for a valid mix");
                binding.setIsChosenMuesliUsed(isChosenMuesliUsed = true);
                binding.setIsIngredientsListEmpty(true);
                binding.setIsInvalidSettings(true);
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                return;
            }

            // Retry with randomized ingredients until a valid mix is found:
            final List<IngredientEntity> ingredients = new ArrayList<>(regularArticlesCount + 1);
            final List<ArticleEntity> selectableToppingArticles = new ArrayList<>(toppingArticles);
            final List<ArticleEntity> chosenToppingArticles = new ArrayList<>(toppingsCount);  // TODO: implement usage
            int fullResetTryCounter = 0;
            while (fullResetTryCounter <= 1)
            {
                for (int tryCounter = 0; tryCounter < MAX_RANDOMIZE_TRIES; tryCounter++)
                {
                    // Return chosen articles back to the selectable pool if necessary:
                    if (!chosenArticles.isEmpty())
                    {
                        moveArticlesToStateList(chosenArticles, selectableArticles);
                    }
                    if (!chosenToppingArticles.isEmpty())
                    {
                        selectableToppingArticles.addAll(chosenToppingArticles);
                        chosenToppingArticles.clear();
                    }

                    // Chose articles for muesli:
                    chosenArticles.addAll(priorityChoosing);
                    selectableArticles.removeAll(priorityChoosing);
                    for (int i = 0; i < regularArticlesCount - priorityChoosing.size(); i++)
                    {
                        chosenArticles.add(selectableArticles.remove(random.nextInt(selectableArticles.size())));
                    }
                    ArticleEntity fillerArticle = fillerArticles.get(random.nextInt(fillerArticles.size()));
                    for (int i = 0; i < regularArticlesCount - priorityChoosing.size(); i++)
                    {
                        chosenToppingArticles.add(selectableToppingArticles.remove(random.nextInt(selectableToppingArticles.size())));
                    }

                    // Determine ingredients:
                    int totalSpoons = 0;
                    float totalWeight = 0F;
                    float totalSugar = 0F;
                    float weight;
                    float sugarPartialSum = 0F;
                    ingredients.clear();
                    ArticleEntity article;
                    int spoonCount;
                    // Calculate and add spoons for first regular articles based on number of ingredients:
                    for (int i = 0; i < regularArticlesCount - 1; i++)
                    {
                        article = chosenArticles.get(i);
                        spoonCount = (int)Math.round(targetWeight / (article.getSpoonWeight() * (regularArticlesCount + FILLER_INGREDIENT_RATIO)));
                        spoonCount = Math.max(spoonCount, 1);
                        weight = spoonCount * (float)article.getSpoonWeight();
                        totalSpoons += spoonCount;
                        totalWeight += weight;
                        totalSugar += weight * article.getSugarPercentage();
                        sugarPartialSum += weight * (article.getSugarPercentage() - fillerArticle.getSugarPercentage());
                        ingredients.add(new IngredientEntity(article, spoonCount));
                    }
                    // Calculate and add spoons for last regular article based on sugar percentage:
                    article = chosenArticles.get(regularArticlesCount - 1);
                    spoonCount = (int)Math.round((targetSugar - targetWeight * fillerArticle.getSugarPercentage() - sugarPartialSum) / (article.getSpoonWeight() * (article.getSugarPercentage() - fillerArticle.getSugarPercentage())));
                    if (spoonCount <= 0) continue;
                    weight = spoonCount * (float)article.getSpoonWeight();
                    totalSpoons += spoonCount;
                    totalWeight += weight;
                    totalSugar += weight * article.getSugarPercentage();
                    ingredients.add(new IngredientEntity(article, spoonCount));
                    // Calculate and add spoons for filler article based on total size:
                    spoonCount = (int)Math.round((targetWeight - totalWeight) / fillerArticle.getSpoonWeight());
                    if (spoonCount < 0) continue;
                    if (spoonCount > 0)
                    {
                        weight = spoonCount * (float)fillerArticle.getSpoonWeight();
                        totalSpoons += spoonCount;
                        totalWeight += weight;
                        totalSugar += weight * fillerArticle.getSugarPercentage();
                        ingredients.add(new IngredientEntity(fillerArticle, spoonCount));
                    }

                    // Adjust mix buttons and ingredients list:
                    Log.i("onCreate", "tryCounter: " + tryCounter);
                    binding.setTotalSpoonCount(String.format(Locale.getDefault(), "%d spoons", totalSpoons));
                    binding.setTotalWeight(String.format(Locale.getDefault(), "%.1f", totalWeight));
                    binding.setTotalSugarPercentage(String.format(Locale.getDefault(), "%.1f", 100 * totalSugar / totalWeight));
                    ingredientsAdapter.setIngredients(ingredients);
                    binding.setIsChosenMuesliUsed(isChosenMuesliUsed = false);
                    binding.setIsIngredientsListEmpty(false);
                    binding.setIsInvalidSettings(false);
                    refreshCountInfo();
                    ingredientsAdapter.notifyDataSetChanged();
                    binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                    return;
                }
                // Return used and chosen articles back to the selectable pool and reset priority choosing:
                moveArticlesToStateList(usedArticles, selectableArticles);
                moveArticlesToStateList(chosenArticles, selectableArticles);
                priorityChoosing.clear();
                Log.i("onCreate", "Cannot find valid mix with selectable articles, retrying with full reset");
                fullResetTryCounter += 1;
            }
            // Adjust mix buttons and ingredients list:
            binding.setIsChosenMuesliUsed(isChosenMuesliUsed = true);
            binding.setIsIngredientsListEmpty(true);
            binding.setIsInvalidSettings(true);
            refreshCountInfo();
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
        });
        binding.useButton.setOnClickListener((View view) ->
        {
            if (chosenArticles.isEmpty())
            {
                Log.e("onCreate", "ChosenArticles is empty");
            }

            // Decrement selections left, move articles to fitting pools and reset priority choosing:
            chosenArticles.forEach(ArticleEntity::decrementSelectionsLeft);
            addArticlesToFittingStateList(chosenArticles);
            chosenArticles.clear();
            priorityChoosing.clear();

            // Adjust mix buttons and ingredients list:
            binding.setIsChosenMuesliUsed(isChosenMuesliUsed = true);
            refreshCountInfo();
            ingredientsAdapter.notifyDataSetChanged();
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes

            // Store updated articles in memory:
            storeArticles(allArticles);
        });
        binding.clearButton.setOnClickListener((View view) ->
        {
            // Return chosen articles back to the selectable pool if necessary:
            if (!chosenArticles.isEmpty())
            {
                moveArticlesToStateList(chosenArticles, selectableArticles);
            }

            // Adjust mix buttons and ingredients list:
            ingredientsAdapter.setIngredients(new ArrayList<>(0));
            binding.setIsChosenMuesliUsed(isChosenMuesliUsed = true);
            binding.setIsIngredientsListEmpty(true);
            refreshCountInfo();
            ingredientsAdapter.notifyDataSetChanged();
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes

            // Store updated articles in memory:
            storeArticles(allArticles);
        });
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // Store updated articles and preferences in memory:
        storeArticles(allArticles);
        storePreferences();
    }
}
