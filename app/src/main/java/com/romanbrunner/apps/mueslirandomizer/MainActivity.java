package com.romanbrunner.apps.mueslirandomizer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import com.romanbrunner.apps.mueslirandomizer.databinding.MainScreenBinding;

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
        articleList.add(new ArticleEntity("Schokomüsli Feinherb", "Vitalis", Type.REGULAR, 9.5F, 0.15F));
        articleList.add(new ArticleEntity("Joghurtmüsli mit Erdbeer-Stücken", "Vitalis", Type.REGULAR, 9F, 0.13F));
        articleList.add(new ArticleEntity("Schoko 30% weniger Zucker", "Kölln", Type.REGULAR, 10F, 0.13F));
        articleList.add(new ArticleEntity("Knusper Honig-Nuss", "Kölln", Type.REGULAR, 11.5F, 0.19F));
        // High sugar regular muesli:
        articleList.add(new ArticleEntity("Nesquik Knusper-Müsli", "Nestle", Type.REGULAR, 8F, 0.21F));
        articleList.add(new ArticleEntity("Crunchy Müsli Red Berries", "Kellogg", Type.REGULAR, 9.5F, 0.22F));
        articleList.add(new ArticleEntity("Crunchy Müsli Choco & Pistachio", "Kellogg", Type.REGULAR, 11.5F, 0.22F));
        articleList.add(new ArticleEntity("Knuspermüsli Nuss-Nougat", "Vitalis", Type.REGULAR, 11.5F, 0.25F));
        articleList.add(new ArticleEntity("Knuspermüsli Plus Nuss Mischung", "Vitalis", Type.REGULAR, 13.5F, 0.2F));
        articleList.add(new ArticleEntity("Knuspermüsli Honeys", "Vitalis", Type.REGULAR, 8.5F, 0.28F));
        articleList.add(new ArticleEntity("Knusper Beere & Schoko", "Kölln", Type.REGULAR, 11.5F, 0.24F));
        articleList.add(new ArticleEntity("Knusper Schoko-Krokant", "Kölln", Type.REGULAR, 11.5F, 0.22F));
        articleList.add(new ArticleEntity("Knusper Schoko & Kaffee", "Kölln", Type.REGULAR, 12F, 0.22F));
        articleList.add(new ArticleEntity("Knusper Schoko & Keks", "Kölln", Type.REGULAR, 11.5F, 0.21F));
        articleList.add(new ArticleEntity("Knusper Schoko & Keks Kakao", "Kölln", Type.REGULAR, 12F, 0.22F));
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

    private IngredientsAdapter ingredientsAdapter;
    private ArticlesAdapter availableArticlesAdapter;
    private MainScreenBinding binding;
    private List<ArticleEntity> allArticles = new LinkedList<>();
    private List<ArticleEntity> fillerArticles = new LinkedList<>();
    private List<ArticleEntity> selectableArticles = new LinkedList<>();
    private List<ArticleEntity> usedArticles = new LinkedList<>();
    private List<ArticleEntity> chosenArticles = new LinkedList<>();
    private List<ArticleEntity> priorityChoosing = new LinkedList<>();
    private int sizeValue;
    private int sugarValue;
    private int articlesValue;

    private static <T> float getLowestValue(List<T> list, Function<T, Float> getter)
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

    private static  <T> void removeNonIntersectingElements(List<T> targetList, List<T> checkList)
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
            if (article.getState() != State.UNAVAILABLE)
            {
                availableArticles.add(article);
            }
        }
        return availableArticles;
    }

    private void moveArticlesToStateList(List<ArticleEntity> sourceStateList, List<ArticleEntity> targetStateList)
    {
        if (targetStateList == selectableArticles)
        {
            sourceStateList.forEach((ArticleEntity article) -> article.setState(State.SELECTABLE));
        }
        else if (targetStateList == usedArticles)
        {
            sourceStateList.forEach((ArticleEntity article) -> article.setState(State.USED));
        }
        targetStateList.addAll(sourceStateList);
        sourceStateList.clear();
    }

    private void addArticlesToFittingStateList(List<ArticleEntity> articles)
    {
        for (ArticleEntity article: articles)
        {
            switch (article.getType())
            {
                case FILLER:
                    if (article.getState() != State.UNAVAILABLE)
                    {
                        fillerArticles.add(article);
                    }
                    break;
                case REGULAR:
                    switch (article.getState())
                    {
                        case UNAVAILABLE:
                            break;
                        case SELECTABLE:
                            selectableArticles.add(article);
                            break;
                        case USED:
                            usedArticles.add(article);
                            break;
                        default:
                            Log.e("addArticlesToFittingStateList", "Unrecognized article type");
                    }
                    break;
            }
        }
    }

    private void adjustStateListsBasedOnAvailability()
    {
        // Remove unavailable articles from state lists:
        List<ArticleEntity> availableArticles = getAvailableArticles();
        removeNonIntersectingElements(fillerArticles, availableArticles);
        removeNonIntersectingElements(selectableArticles, availableArticles);
        removeNonIntersectingElements(chosenArticles, availableArticles);
        removeNonIntersectingElements(usedArticles, availableArticles);
        removeNonIntersectingElements(priorityChoosing, availableArticles);
        // Add available articles to fitting state lists that aren't in one:
        availableArticles.removeAll(fillerArticles);
        availableArticles.removeAll(selectableArticles);
        availableArticles.removeAll(chosenArticles);
        availableArticles.removeAll(usedArticles);
        addArticlesToFittingStateList(availableArticles);
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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        final Random random = new Random();
        binding = DataBindingUtil.setContentView(this, R.layout.main_screen);
        loadPreferences();

        // Setup recycle view adapters:
        ingredientsAdapter = new IngredientsAdapter();
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
            allArticles = defaultArticles;
        }
        Collections.sort(allArticles, (Comparator<Article>) (articleA, articleB) -> (articleA.getBrand() + articleA.getName()).compareTo(articleB.getBrand() + articleB.getName()));
        availableArticlesAdapter.setArticles(allArticles);
        addArticlesToFittingStateList(allArticles);
        if (!selectableArticles.isEmpty() && !fillerArticles.isEmpty() && getLowestValue(selectableArticles, ArticleEntity::getSugarPercentage) <= getLowestValue(fillerArticles, ArticleEntity::getSugarPercentage)) throw new AssertionError("Sugar percentage of all filler articles has to be lower than that of regular articles");

        // Init layout variables:
        binding.setSizeWeight(String.format(Locale.getDefault(), "%.0f", sizeValue2SizeWeight(sizeValue)));
        binding.setSugarPercentage(String.format(Locale.getDefault(), "%.1f", sugarValue2SugarPercentage(sugarValue) * 100));
        binding.setArticlesCount(articlesValue2ArticlesCount(articlesValue));
        binding.setIsAvailabilityBoxMinimized(true);
        binding.setIsChosenMuesliUsed(true);
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
                Log.d("onCreate", "------------------------");  // DEBUG:
                Log.d("onCreate", "selectableArticles count: " + selectableArticles.size());  // DEBUG:
                Log.d("onCreate", "chosenArticles count: " + chosenArticles.size());  // DEBUG:
                Log.d("onCreate", "usedArticles count: " + usedArticles.size());  // DEBUG:
                Log.d("onCreate", "priorityChoosing count: " + priorityChoosing.size());  // DEBUG:
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
                    Log.d("onCreate", "totalWeight: " + (totalWeight + fillerArticle.getSpoonWeight() * spoonCount));  // DEBUG:
                    Log.d("onCreate", "tryCounter: " + tryCounter);  // DEBUG:
                    ingredientsAdapter.setIngredients(ingredients);
                    binding.setIsChosenMuesliUsed(false);
                    binding.setIsInvalidSettings(false);
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
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
        });
        binding.useButton.setOnClickListener((View view) ->
        {
            if (chosenArticles.isEmpty())
            {
                Log.e("onCreate", "ChosenArticles is empty");
            }

            // Move chosen articles to used pool and reset priority choosing:
            moveArticlesToStateList(chosenArticles, usedArticles);
            priorityChoosing.clear();

            // Adjust use button:
            binding.setIsChosenMuesliUsed(true);
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes

            storeArticles(allArticles, ARTICLES_FILENAME);
        });
        binding.availabilityButton.setOnClickListener((View view) ->
        {
            // Flip availability box minimization:
            final boolean isMinimized = binding.getIsAvailabilityBoxMinimized();
            if (!isMinimized)
            {
                adjustStateListsBasedOnAvailability();
            }
            binding.setIsAvailabilityBoxMinimized(!isMinimized);
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
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
