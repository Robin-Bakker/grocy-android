/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.RowRecipeEntryBinding;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataRecipesExtraField;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.UnitUtil;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class RecipeEntryAdapter extends
    RecyclerView.Adapter<RecipeEntryAdapter.ViewHolder> {

  private final static String TAG = RecipeEntryAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final LinearLayoutManager linearLayoutManager;
  private final ArrayList<Recipe> recipes;
  private final ArrayList<RecipeFulfillment> recipeFulfillments;
  private final RecipesItemAdapterListener listener;
  private final GrocyApi grocyApi;
  private final LazyHeaders grocyAuthHeaders;
  private String sortMode;
  private boolean sortAscending;
  private String extraField;

  public RecipeEntryAdapter(
      Context context,
      LinearLayoutManager linearLayoutManager,
      ArrayList<Recipe> recipes,
      ArrayList<RecipeFulfillment> recipeFulfillments,
      RecipesItemAdapterListener listener,
      String sortMode,
      boolean sortAscending,
      String extraField
  ) {
    this.context = context;
    this.linearLayoutManager = linearLayoutManager;
    this.recipes = new ArrayList<>(recipes);
    this.recipeFulfillments = new ArrayList<>(recipeFulfillments);
    this.listener = listener;
    this.grocyApi = new GrocyApi((Application) context.getApplicationContext());
    this.grocyAuthHeaders = RequestHeaders.getGlideGrocyAuthHeaders(context);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    this.extraField = extraField;
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onDetachedFromRecyclerView(recyclerView);
    this.context = null;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class RecipeViewHolder extends ViewHolder {

    private final RowRecipeEntryBinding binding;

    public RecipeViewHolder(RowRecipeEntryBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new RecipeViewHolder(RowRecipeEntryBinding.inflate(
        LayoutInflater.from(parent.getContext()),
        parent,
        false
    ));
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    int position = viewHolder.getAdapterPosition();

    Recipe recipe = recipes.get(position);
    RecipeFulfillment recipeFulfillment = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(recipeFulfillments, recipe.getId());
    RecipeViewHolder holder = (RecipeViewHolder) viewHolder;

    // NAME

    holder.binding.title.setText(recipe.getName());

    if (recipeFulfillment != null) {
      // DUE SCORE
      int due_score = recipeFulfillment.getDueScore();
      @ColorRes int color;

      if (due_score == 0) {
        color = R.color.retro_green_fg;
      }
      else if (due_score <= 10) {
        color = R.color.retro_yellow_fg;
      }
      else {
        color = R.color.retro_red_fg;
      }
      holder.binding.dueScore.setTextColor(ContextCompat.getColor(context, color));

      holder.binding.dueScore.setText(
              context.getString(
                      R.string.subtitle_recipe_due_score,
                      String.valueOf(recipeFulfillment.getDueScore())
              )
      );

      // REQUIREMENTS FULFILLED
      if (recipeFulfillment.isNeedFulfilled()) {
        holder.binding.fulfilled.setText(R.string.msg_recipes_enough_in_stock);
        holder.binding.imageFulfillment.setImageDrawable(ResourcesCompat.getDrawable(
            context.getResources(),
            R.drawable.ic_round_done,
            context.getTheme()
        ));
        holder.binding.imageFulfillment.setColorFilter(
            ContextCompat.getColor(context, R.color.retro_green_fg),
            android.graphics.PorterDuff.Mode.SRC_IN
        );
        holder.binding.missing.setVisibility(View.GONE);
      } else if (recipeFulfillment.isNeedFulfilledWithShoppingList()) {
        holder.binding.fulfilled.setText(R.string.msg_recipes_not_enough);
        holder.binding.imageFulfillment.setImageDrawable(ResourcesCompat.getDrawable(
            context.getResources(),
            R.drawable.ic_round_priority_high,
            context.getTheme()
        ));
        holder.binding.imageFulfillment.setColorFilter(
            ContextCompat.getColor(context, R.color.retro_yellow_fg),
            android.graphics.PorterDuff.Mode.SRC_IN
        );
        holder.binding.missing.setText(
            context.getResources()
                .getQuantityString(R.plurals.msg_recipes_ingredients_missing_but_on_shopping_list,
                    recipeFulfillment.getMissingProductsCount(),
                    recipeFulfillment.getMissingProductsCount())
        );
        holder.binding.missing.setVisibility(View.VISIBLE);
      } else {
        holder.binding.fulfilled.setText(R.string.msg_recipes_not_enough);
        holder.binding.imageFulfillment.setImageDrawable(ResourcesCompat.getDrawable(
            context.getResources(),
            R.drawable.ic_round_close,
            context.getTheme()
        ));
        holder.binding.imageFulfillment.setColorFilter(
            ContextCompat.getColor(context, R.color.retro_red_fg),
            android.graphics.PorterDuff.Mode.SRC_IN
        );
        holder.binding.missing.setText(
            context.getResources()
                .getQuantityString(R.plurals.msg_recipes_ingredients_missing,
                    recipeFulfillment.getMissingProductsCount(),
                    recipeFulfillment.getMissingProductsCount())
        );
        holder.binding.missing.setVisibility(View.VISIBLE);
      }
    }

    String extraFieldText = null;
    String extraFieldSubtitleText = null;
    switch (extraField) {
      case FilterChipLiveDataRecipesExtraField.EXTRA_FIELD_CALORIES:
        if (recipeFulfillment != null) {
          extraFieldText = NumUtil.trim(recipeFulfillment.getCalories());
          extraFieldSubtitleText = "kcal";
        }
        break;
    }
    if (extraFieldText != null) {
      holder.binding.extraField.setText(extraFieldText);
      holder.binding.extraFieldContainer.setVisibility(View.VISIBLE);
    } else {
      holder.binding.extraFieldContainer.setVisibility(View.GONE);
    }
    if (extraFieldSubtitleText != null) {
      holder.binding.extraFieldSubtitle.setText(extraFieldSubtitleText);
      holder.binding.extraFieldSubtitle.setVisibility(View.VISIBLE);
    } else {
      holder.binding.extraFieldSubtitle.setVisibility(View.GONE);
    }

    if (recipe.getPictureFileName() != null) {
      holder.binding.picture.layout(0, 0, 0, 0);

      Glide
          .with(context)
          .load(new GlideUrl(grocyApi.getRecipePicture(recipe.getPictureFileName()), grocyAuthHeaders))
          .transform(new CenterCrop(), new RoundedCorners(UnitUtil.dpToPx(context, 12)))
          .transition(DrawableTransitionOptions.withCrossFade())
          .listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(
                @Nullable @org.jetbrains.annotations.Nullable GlideException e, Object model,
                Target<Drawable> target, boolean isFirstResource) {
              holder.binding.picture.setVisibility(View.GONE);
              return false;
            }
            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                DataSource dataSource, boolean isFirstResource) {
              holder.binding.picture.setVisibility(View.VISIBLE);
              return false;
            }
          })
          .into(holder.binding.picture);
    } else {
      holder.binding.picture.setVisibility(View.GONE);
    }


    // CONTAINER

    holder.binding.linearRecipeEntryContainer.setOnClickListener(
        view -> listener.onItemRowClicked(recipe)
    );
  }

  @Override
  public int getItemCount() {
    return recipes.size();
  }

  public interface RecipesItemAdapterListener {

    void onItemRowClicked(Recipe recipe);
  }

  public void updateData(
      ArrayList<Recipe> newList,
      ArrayList<RecipeFulfillment> newRecipeFulfillments,
      String sortMode,
      boolean sortAscending,
      String extraField
  ) {

    RecipeEntryAdapter.DiffCallback diffCallback = new RecipeEntryAdapter.DiffCallback(
        this.recipes,
        newList,
        this.recipeFulfillments,
        newRecipeFulfillments,
        this.sortMode,
        sortMode,
        this.sortAscending,
        sortAscending,
        this.extraField,
        extraField
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.recipes.clear();
    this.recipes.addAll(newList);
    this.recipeFulfillments.clear();
    this.recipeFulfillments.addAll(newRecipeFulfillments);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    this.extraField = extraField;
    diffResult.dispatchUpdatesTo(new AdapterListUpdateCallback(this, linearLayoutManager));
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<Recipe> oldItems;
    ArrayList<Recipe> newItems;
    ArrayList<RecipeFulfillment> oldRecipeFulfillments;
    ArrayList<RecipeFulfillment> newRecipeFulfillments;
    String sortModeOld;
    String sortModeNew;
    boolean sortAscendingOld;
    boolean sortAscendingNew;
    String extraFieldOld;
    String extraFieldNew;

    public DiffCallback(
        ArrayList<Recipe> oldItems,
        ArrayList<Recipe> newItems,
        ArrayList<RecipeFulfillment> oldRecipeFulfillments,
        ArrayList<RecipeFulfillment> newRecipeFulfillments,
        String sortModeOld,
        String sortModeNew,
        boolean sortAscendingOld,
        boolean sortAscendingNew,
        String extraFieldOld,
        String extraFieldNew
    ) {
      this.oldItems = oldItems;
      this.newItems = newItems;
      this.oldRecipeFulfillments = oldRecipeFulfillments;
      this.newRecipeFulfillments = newRecipeFulfillments;
      this.sortModeOld = sortModeOld;
      this.sortModeNew = sortModeNew;
      this.sortAscendingOld = sortAscendingOld;
      this.sortAscendingNew = sortAscendingNew;
      this.extraFieldOld = extraFieldOld;
      this.extraFieldNew = extraFieldNew;
    }

    @Override
    public int getOldListSize() {
      return oldItems.size();
    }

    @Override
    public int getNewListSize() {
      return newItems.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, false);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, true);
    }

    private boolean compare(int oldItemPos, int newItemPos, boolean compareContent) {
      Recipe newItem = newItems.get(newItemPos);
      Recipe oldItem = oldItems.get(oldItemPos);

      if (!sortModeOld.equals(sortModeNew)) {
        return false;
      }
      if (sortAscendingOld != sortAscendingNew) {
        return false;
      }
      if (!extraFieldOld.equals(extraFieldNew)) {
        return false;
      }

      RecipeFulfillment recipeFulfillmentOld = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(oldRecipeFulfillments, oldItem.getId());
      RecipeFulfillment recipeFulfillmentNew = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(newRecipeFulfillments, newItem.getId());
      if (recipeFulfillmentOld == null && recipeFulfillmentNew != null
          || recipeFulfillmentOld != null && recipeFulfillmentNew == null
          || recipeFulfillmentOld != null && !recipeFulfillmentOld.equals(recipeFulfillmentNew)) {
        return false;
      }

      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }

      return newItem.equals(oldItem);
    }
  }

  /**
   * Custom ListUpdateCallback that prevents RecyclerView from scrolling down if top item is moved.
   */
  public static final class AdapterListUpdateCallback implements ListUpdateCallback {

    @NonNull
    private final RecipeEntryAdapter mAdapter;
    private final LinearLayoutManager linearLayoutManager;

    public AdapterListUpdateCallback(
        @NonNull RecipeEntryAdapter adapter,
        LinearLayoutManager linearLayoutManager
    ) {
      this.mAdapter = adapter;
      this.linearLayoutManager = linearLayoutManager;
    }

    @Override
    public void onInserted(int position, int count) {
      mAdapter.notifyItemRangeInserted(position, count);
    }

    @Override
    public void onRemoved(int position, int count) {
      mAdapter.notifyItemRangeRemoved(position, count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
      // workaround for https://github.com/patzly/grocy-android/issues/439
      // figure out the position of the first visible item
      int firstPos = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
      int offsetTop = 0;
      if(firstPos >= 0) {
        View firstView = linearLayoutManager.findViewByPosition(firstPos);
        if (firstView != null) {
          offsetTop = linearLayoutManager.getDecoratedTop(firstView)
              - linearLayoutManager.getTopDecorationHeight(firstView);
        }
      }

      mAdapter.notifyItemMoved(fromPosition, toPosition);

      // reapply the saved position
      if(firstPos >= 0) {
        linearLayoutManager.scrollToPositionWithOffset(firstPos, offsetTop);
      }
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
      mAdapter.notifyItemRangeChanged(position, count, payload);
    }
  }
}
