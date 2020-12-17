package com.romanbrunner.apps.mueslirandomizer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SeekBar;

import com.romanbrunner.apps.mueslirandomizer.databinding.MainScreenBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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

    private final static float FILLER_INGREDIENT_RATIO = 0.5F;
    private final static int MAX_RANDOMIZE_TRIES = 1024;
    private final static String ARTICLES_FILENAME = "AllArticles";
    private final static String PREFS_NAME = "GlobalPreferences";

    private static List<ArticleEntity> getDefaultArticles()
    {
        List<ArticleEntity> articleList = new LinkedList<>();

        // Very low sugar filler muesli:
        articleList.add(new ArticleEntity("Echte Kölln Kernige", "Kölln", Type.FILLER, 8F, 0.012F));
        articleList.add(new ArticleEntity("Blütenzarte Kölln Flocken", "Kölln", Type.FILLER, 9F, 0.012F));

        // Low sugar regular muesli:
        articleList.add(new ArticleEntity("Nuss & Krokant", "Kölln", Type.REGULAR, 9.5F, 0.077F));
        // Medium sugar regular muesli:
        articleList.add(new ArticleEntity("Superfood Crunchy Müsli Cacao & Nuts", "Kellogg", Type.REGULAR, 10.5F, 0.14F));
        articleList.add(new ArticleEntity("Crunchy Müsli Cacao & Hazelnut", "Kellogg", Type.REGULAR, 10F, 0.13F));
        articleList.add(new ArticleEntity("Knusper Müsli", "Manner", Type.REGULAR, 10.5F, 0.14F));
        articleList.add(new ArticleEntity("Schokomüsli Feinherb", "Vitalis", Type.REGULAR, 9.5F, 0.15F));
        articleList.add(new ArticleEntity("Joghurtmüsli mit Erdbeer-Stücken", "Vitalis", Type.REGULAR, 9F, 0.13F));
        articleList.add(new ArticleEntity("Knusper Himbeere 30% weniger Zucker", "Vitalis", Type.REGULAR, 9F, 0.15F));
        articleList.add(new ArticleEntity("Knusper Schoko 30% weniger Zucker", "Vitalis", Type.REGULAR, 10F, 0.14F));
        articleList.add(new ArticleEntity("Schoko 30% weniger Zucker", "Kölln", Type.REGULAR, 10F, 0.13F));
        articleList.add(new ArticleEntity("Knusper Honig-Nuss", "Kölln", Type.REGULAR, 11.5F, 0.19F));
        articleList.add(new ArticleEntity("Schoko-Kirsch", "Kölln", Type.REGULAR, 9.5F, 0.16F));
        // High sugar regular muesli:
        articleList.add(new ArticleEntity("Nesquik Knusper-Müsli", "Nestle", Type.REGULAR, 8F, 0.21F));
        articleList.add(new ArticleEntity("Clusters Chocolate", "Nestle", Type.REGULAR, 8F, 0.285F));
        articleList.add(new ArticleEntity("Crunchy Müsli Red Berries", "Kellogg", Type.REGULAR, 9.5F, 0.22F));
        articleList.add(new ArticleEntity("Crunchy Müsli Choco & Pistachio", "Kellogg", Type.REGULAR, 11.5F, 0.22F));
        articleList.add(new ArticleEntity("Knuspermüsli Nuss-Nougat", "Vitalis", Type.REGULAR, 11.5F, 0.25F));
        articleList.add(new ArticleEntity("Knuspermüsli Plus Nuss Mischung", "Vitalis", Type.REGULAR, 13.5F, 0.2F));
        articleList.add(new ArticleEntity("Knuspermüsli Honeys", "Vitalis", Type.REGULAR, 8.5F, 0.28F));
        articleList.add(new ArticleEntity("Knuspermüsli Flakes + Mandeln", "Vitalis", Type.REGULAR, 10.5F, 0.24F));
        articleList.add(new ArticleEntity("Knusper Beere & Schoko", "Kölln", Type.REGULAR, 11.5F, 0.24F));
        articleList.add(new ArticleEntity("Knusper Schoko-Krokant", "Kölln", Type.REGULAR, 11.5F, 0.22F));
        articleList.add(new ArticleEntity("Knusper Schoko-Minze", "Kölln", Type.REGULAR, 12F, 0.23F));
        articleList.add(new ArticleEntity("Knusper Schoko & Kaffee", "Kölln", Type.REGULAR, 12F, 0.22F));
        articleList.add(new ArticleEntity("Knusper Schoko & Keks", "Kölln", Type.REGULAR, 11.5F, 0.21F));
        articleList.add(new ArticleEntity("Knusper Schoko & Keks Kakao", "Kölln", Type.REGULAR, 12F, 0.22F));
        articleList.add(new ArticleEntity("Knusper Mango-Kurkuma", "Kölln", Type.REGULAR, 11.5F, 0.2F));
        articleList.add(new ArticleEntity("Knusper Joghurt-Honig", "Kölln", Type.REGULAR, 11F, 0.2F));
        articleList.add(new ArticleEntity("Knusprige Haferkissen Zimt", "Kölln", Type.REGULAR, 4.5F, 0.2F));
        articleList.add(new ArticleEntity("Knusper Schoko Feinherb 30% weniger Fett", "Kölln", Type.REGULAR, 12F, 0.2F));
        articleList.add(new ArticleEntity("Porridge Dreierlei Beere", "3 Bears", Type.REGULAR, 12.5F, 0.22F));
        articleList.add(new ArticleEntity("Porridge Zimtiger Apfel", "3 Bears", Type.REGULAR, 11F, 0.2F));

        return articleList;
    }

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


    // --------------------
    // Functional code
    // --------------------

    private final List<ArticleEntity> allArticles = new LinkedList<>();  // All catalogued articles, also not available ones
    private final List<ArticleEntity> fillerArticles = new LinkedList<>();  // All available filler type articles, separate from the other lists with only regular articles
    private final List<ArticleEntity> selectableArticles = new LinkedList<>();  // Selectable articles for the next muesli creation
    private final List<ArticleEntity> usedArticles = new LinkedList<>();  // Used articles, will be reshuffled into selectableArticles once that is depleted
    private final List<ArticleEntity> chosenArticles = new LinkedList<>();  // Chosen articles for the current muesli creation
    private final List<ArticleEntity> priorityChoosing = new LinkedList<>();  // Remaining articles that have to be chosen for the next muesli creation, articles are also in selectableArticles
    private IngredientsAdapter ingredientsAdapter;
    private ArticlesAdapter availableArticlesAdapter;
    private MainScreenBinding binding;
    private int sizeValue;
    private int sugarValue;
    private int articlesValue;

    public boolean isChosenMuesliUsed = true;

    private static <T> float getLowestValue(final List<T> list, final Function<T, Float> getter)
    {
        if (list.isEmpty())
        {
            Log.e("getLowestValue", "Cannot get value for an empty list");
        }

        float lowestValue = getter.apply(list.get(0));
        for (int i = 1; i < list.size(); i++)
        {
            lowestValue = Math.min(lowestValue, getter.apply(list.get(i)));
        }
        return lowestValue;
    }

    private static  <T> void removeNonIntersectingElements(final List<T> targetList, final List<T> checkList)
    {
        Iterator<T> iterator = targetList.iterator();
        while (iterator.hasNext())
        {
            if (!checkList.contains(iterator.next()))
            {
                iterator.remove();
            }
        }
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
        removeNonIntersectingElements(chosenArticles, availableArticles);
        removeNonIntersectingElements(priorityChoosing, availableArticles);
        // Add available articles to fitting state lists that aren't in one:
        availableArticles.removeAll(fillerArticles);
        availableArticles.removeAll(chosenArticles);
        addArticlesToFittingStateList(availableArticles);
    }

    private void refreshCountInfo()
    {
        binding.setFillerCount(fillerArticles.size());
        binding.setUsedCount(usedArticles.size());
        int count = 0;
        for (ArticleEntity article: selectableArticles) { count += article.getSelectionsLeft(); }
        for (ArticleEntity article: chosenArticles) { count += article.getSelectionsLeft(); }
        binding.setSelectableCount(count);
        count = 0;
        for (ArticleEntity article: priorityChoosing) { count += article.getSelectionsLeft(); }
        binding.setPriorityCount(count);
    }

    private void storeArticles(final List<ArticleEntity> articles, final String fileName)
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
            FileOutputStream fileOutputStream = getApplicationContext().openFileOutput(fileName, Context.MODE_PRIVATE);
            fileOutputStream.write(dataOutputStream.toByteArray());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean loadArticles(final List<ArticleEntity> articles, final String fileName)
    {
        try
        {
            final Context context = getApplicationContext();
            final List<String> fileNames = new ArrayList<>(Arrays.asList(context.fileList()));
            if (fileNames.contains(fileName))
            {
                byte[] bytes;
                FileInputStream fileInputStream = context.openFileInput(fileName);
                int length;
                while ((length = fileInputStream.read()) != -1)
                {
                    bytes = new byte[length];
                    fileInputStream.read(bytes);
                    articles.add(new ArticleEntity(bytes));
                }
                return true;
            }
        }
        catch (IOException e)
        {
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
        editor.apply();
    }

    private void loadPreferences()
    {
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        sizeValue = sharedPrefs.getInt("sizeValue", binding.sizeSlider.getProgress());
        sugarValue = sharedPrefs.getInt("sugarValue", binding.sugarSlider.getProgress());
        articlesValue = sharedPrefs.getInt("articlesValue", binding.articlesSlider.getProgress());
        binding.sizeSlider.setProgress(sizeValue);
        binding.sugarSlider.setProgress(sugarValue);
        binding.articlesSlider.setProgress(articlesValue);
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

    public void refreshData()
    {
        refreshStateLists();
        refreshCountInfo();
        availableArticlesAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        final Random random = new Random();
        binding = DataBindingUtil.setContentView(this, R.layout.main_screen);
        loadPreferences();

        // Setup recycle view adapters:
        ingredientsAdapter = new IngredientsAdapter(this);
        binding.ingredients.setAdapter(ingredientsAdapter);
        binding.ingredients.setLayoutManager(new LinearLayoutManager(this));
        availableArticlesAdapter = new ArticlesAdapter();
        binding.availableArticles.setAdapter(availableArticlesAdapter);
        binding.availableArticles.setLayoutManager(new LinearLayoutManager(this));

        // Load or create all articles and add them to the fitting state lists:
        final List<ArticleEntity> defaultArticles = getDefaultArticles();
        if (loadArticles(allArticles, ARTICLES_FILENAME))
        {
            // Add missing default articles:
            defaultLoop: for (ArticleEntity articleA : defaultArticles)
            {
                for (ArticleEntity articleB : allArticles)
                {
                    if (isNameTheSame(articleA, articleB))
                    {
                        continue defaultLoop;
                    }
                }
                allArticles.add(articleA);
            }
        }
        else
        {
            allArticles.addAll(defaultArticles);
        }
        allArticles.sort((Comparator<Article>) (articleA, articleB) -> (articleA.getBrand() + articleA.getName()).compareTo(articleB.getBrand() + articleB.getName()));
        availableArticlesAdapter.setArticles(allArticles);
        addArticlesToFittingStateList(allArticles);
        if (!selectableArticles.isEmpty() && !fillerArticles.isEmpty() && getLowestValue(selectableArticles, ArticleEntity::getSugarPercentage) <= getLowestValue(fillerArticles, ArticleEntity::getSugarPercentage)) throw new AssertionError("Sugar percentage of all filler articles has to be lower than that of regular articles");

        // Init layout variables:
        binding.setNewArticle(new ArticleEntity("", "", Type.REGULAR, 0F, 0F));
        refreshCountInfo();
        binding.setSizeWeight(String.format(Locale.getDefault(), "%.0f", sizeValue2SizeWeight(sizeValue)));
        binding.setSugarPercentage(String.format(Locale.getDefault(), "%.1f", sugarValue2SugarPercentage(sugarValue) * 100));
        binding.setArticlesCount(articlesValue2ArticlesCount(articlesValue));
        binding.setIsAvailabilityBoxMinimized(true);
        binding.setIsChosenMuesliUsed(isChosenMuesliUsed);
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
        binding.availabilityButton.setOnClickListener((View view) ->
        {
            // Flip availability box minimization:
            final boolean isMinimized = binding.getIsAvailabilityBoxMinimized();
            if (!isMinimized)
            {
                refreshData();
            }
            binding.setIsAvailabilityBoxMinimized(!isMinimized);
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
        });
        binding.addButton.setOnClickListener((View view) ->
        {
            // Check that there is no name duplicate:
            String newName = binding.getNewArticle().getName();
            for (ArticleEntity article: allArticles)
            {
                if (Objects.equals(article.getName(), newName))
                {
                    newName = null;
                    break;
                }
            }
            // Check for non-empty name and brand:
            if (newName != null && !Objects.equals(newName, ""))
            {
                final ArticleEntity newArticle = (ArticleEntity)binding.getNewArticle();
                allArticles.add(newArticle);
                addArticlesToFittingStateList(Collections.singletonList(newArticle));
                allArticles.sort((Comparator<Article>) (articleA, articleB) -> (articleA.getBrand() + articleA.getName()).compareTo(articleB.getBrand() + articleB.getName()));
                availableArticlesAdapter.setArticles(allArticles);
                binding.availableArticles.smoothScrollToPosition(allArticles.indexOf(newArticle));
                binding.setNewArticle(new ArticleEntity("", "", Type.REGULAR, 0F, 0F));
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
            }
            else
            {
                Log.i("onCreate", "Cannot add empty or duplicate muesli name");
            }
        });
        binding.importButton.setOnClickListener((View view) ->
        {
            // TODO: read jsonString from file in documents folder
            // TODO: merge jsonString data into allArticles, skip already existing articles
        });
        binding.exportButton.setOnClickListener((View view) ->
        {
            try
            {
                JSONArray jsonArray = new JSONArray();
                for (ArticleEntity article: allArticles)
                {
                    jsonArray.put(article.writeToJson());
                }
                String jsonString = jsonArray.toString(4);
                // TODO: write jsonString to file in documents folder
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        });
        binding.nameField.setOnFocusChangeListener(this::setEditTextFocus);
        binding.brandField.setOnFocusChangeListener(this::setEditTextFocus);
        binding.weightField.setOnFocusChangeListener(this::setEditTextFocus);
        binding.percentageField.setOnFocusChangeListener(this::setEditTextFocus);
        binding.randomizeButton.setOnClickListener((View view) ->
        {
            final int regularArticlesCount = articlesValue2ArticlesCount(articlesValue);
            final float targetWeight = sizeValue2SizeWeight(sizeValue);
            final float targetSugar = sugarValue2SugarPercentage(sugarValue) * targetWeight;

            // Return used articles back to the selectable pool if necessary:
            if (selectableArticles.size() + chosenArticles.size() < regularArticlesCount)
            {
                priorityChoosing.addAll(selectableArticles);
                priorityChoosing.addAll(chosenArticles);
                moveArticlesToStateList(usedArticles, selectableArticles);
            }

            if (fillerArticles.size() <= 0 || selectableArticles.size() + chosenArticles.size() < regularArticlesCount)
            {
                Log.i("onCreate", "Not enough available articles for a valid mix");
                binding.setIsInvalidSettings(true);
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                return;
            }

            // Retry with randomized ingredients until a valid mix is found:
            final List<IngredientEntity> ingredients = new ArrayList<>(regularArticlesCount + 1);
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

                    // Chose articles for muesli:
                    chosenArticles.addAll(priorityChoosing);
                    selectableArticles.removeAll(priorityChoosing);
                    for (int i = 0; i < regularArticlesCount - priorityChoosing.size(); i++)
                    {
                        chosenArticles.add(selectableArticles.remove(random.nextInt(selectableArticles.size())));
                    }
                    ArticleEntity fillerArticle = fillerArticles.get(random.nextInt(fillerArticles.size()));

                    // Determine ingredients:
                    float totalWeight = 0F;
                    float totalSugar = 0F;
                    ingredients.clear();
                    ArticleEntity article;
                    int spoonCount;
                    // Calculate and add spoons for first regular articles based on number of ingredients:
                    for (int i = 0; i < regularArticlesCount - 1; i++)
                    {
                        article = chosenArticles.get(i);
                        spoonCount = Math.round(targetWeight / (article.getSpoonWeight() * (regularArticlesCount + FILLER_INGREDIENT_RATIO)));
                        spoonCount = Math.max(spoonCount, 1);
                        totalWeight += spoonCount * article.getSpoonWeight();
                        totalSugar += spoonCount * article.getSpoonWeight() * (article.getSugarPercentage() - fillerArticle.getSugarPercentage());
                        ingredients.add(new IngredientEntity(article, spoonCount));
                    }
                    // Calculate and add spoons for last regular article based on sugar percentage:
                    article = chosenArticles.get(regularArticlesCount - 1);
                    spoonCount = Math.round((targetSugar - totalSugar) / (article.getSpoonWeight() * (article.getSugarPercentage() - fillerArticle.getSugarPercentage())));
                    if (spoonCount <= 0) continue;
                    totalWeight += article.getSpoonWeight() * spoonCount;
                    ingredients.add(new IngredientEntity(article, spoonCount));
                    // Calculate and add spoons for filler article based on total size:
                    spoonCount = Math.round((targetWeight - totalWeight) / fillerArticle.getSpoonWeight());
                    if (spoonCount < 0) continue;
                    if (spoonCount > 0)
                    {
                        ingredients.add(new IngredientEntity(fillerArticle, spoonCount));
                    }

                    // Display ingredients and adjust use button:
                    Log.i("onCreate", "totalWeight: " + (totalWeight + fillerArticle.getSpoonWeight() * spoonCount));
                    Log.i("onCreate", "tryCounter: " + tryCounter);
                    ingredientsAdapter.setIngredients(ingredients);
                    binding.setIsChosenMuesliUsed(isChosenMuesliUsed = false);
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

            // Adjust use button:
            binding.setIsChosenMuesliUsed(isChosenMuesliUsed = true);
            refreshCountInfo();
            ingredientsAdapter.notifyDataSetChanged();
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes

            storeArticles(allArticles, ARTICLES_FILENAME);
        });
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        storeArticles(allArticles, ARTICLES_FILENAME);
        storePreferences();
    }
}
