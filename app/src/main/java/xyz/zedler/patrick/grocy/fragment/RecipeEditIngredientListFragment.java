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

package xyz.zedler.patrick.grocy.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.RecipeEditIngredientListEntryAdapter;
import xyz.zedler.patrick.grocy.adapter.RecipeEditIngredientListEntryAdapter.RecipeEditIngredientListEntryAdapterListener;
import xyz.zedler.patrick.grocy.adapter.RecipeEntryAdapter;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentRecipeEditIngredientListBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ACTION;
import xyz.zedler.patrick.grocy.viewmodel.RecipeEditIngredientListViewModel;

public class RecipeEditIngredientListFragment extends BaseFragment
        implements RecipeEditIngredientListEntryAdapterListener {

  private final static String TAG = RecipeEditIngredientListFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentRecipeEditIngredientListBinding binding;
  private RecipeEditIngredientListViewModel viewModel;
  private SwipeBehavior swipeBehavior;
  private ClickUtil clickUtil;
  private InfoFullscreenHelper infoFullscreenHelper;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentRecipeEditIngredientListBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (binding != null) {
      binding.recycler.animate().cancel();
      binding.recycler.setAdapter(null);
      binding = null;
    }
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    RecipeEditIngredientListFragmentArgs args = RecipeEditIngredientListFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new RecipeEditIngredientListViewModel
        .RecipeEditIngredientListViewModelFactory(activity.getApplication(), args)
    ).get(RecipeEditIngredientListViewModel.class);
    binding.setActivity(activity);
    binding.setFormData(viewModel.getFormData());
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);
    clickUtil = new ClickUtil();

    binding.recycler.setLayoutManager(
            new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recycler.setAdapter(new MasterPlaceholderAdapter());

    if (savedInstanceState == null) {
      binding.recycler.scrollToPosition(0);
    }

    viewModel.getInfoFullscreenLive().observe(
            getViewLifecycleOwner(),
            infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getRecipePositionsLive().observe(getViewLifecycleOwner(), items -> {
      if (items == null) {
        return;
      }
      if (items.isEmpty()) {
        InfoFullscreen info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_INGREDIENTS);
        viewModel.getInfoFullscreenLive().setValue(info);
      } else {
        viewModel.getInfoFullscreenLive().setValue(null);
      }
      if (binding.recycler.getAdapter() instanceof RecipeEntryAdapter) {
        ((RecipeEditIngredientListEntryAdapter) binding.recycler.getAdapter()).updateData(
                items,
                viewModel.getProducts(),
                viewModel.getQuantityUnits()
        );
      } else {
        binding.recycler.setAdapter(
                new RecipeEditIngredientListEntryAdapter(
                        requireContext(),
                        (LinearLayoutManager) binding.recycler.getLayoutManager(),
                        items,
                        viewModel.getProducts(),
                        viewModel.getQuantityUnits(),
                        this
                )
        );
      }
    });

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        SnackbarMessage message = (SnackbarMessage) event;
        Snackbar snack = message.getSnackbar(activity, activity.binding.frameMainContainer);
        activity.showSnackbar(snack);
      } else if (event.getType() == Event.NAVIGATE_UP) {
        activity.navigateUp();
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    if (swipeBehavior == null) {
      swipeBehavior = new SwipeBehavior(
              activity,
              swipeStarted -> binding.swipe.setEnabled(!swipeStarted)
      ) {
        @Override
        public void instantiateUnderlayButton(
                RecyclerView.ViewHolder viewHolder,
                List<UnderlayButton> underlayButtons
        ) {
          int position = viewHolder.getAdapterPosition();
          List<RecipePosition> displayedItems = viewModel.getRecipePositions();
          if (displayedItems == null || position < 0
                  || position >= displayedItems.size()) {
            return;
          }

          underlayButtons.add(new UnderlayButton(
                  R.drawable.ic_round_delete_anim,
                  pos -> {
                    if (pos >= displayedItems.size()) {
                      return;
                    }
                    swipeBehavior.recoverLatestSwipedItem();
                    new Handler().postDelayed(() -> {
                      RecipePosition recipePosition = displayedItems.get(pos);
                      deleteRecipePosition(recipePosition.getId());
                    }, 100);
                  }
          ));
        }
      };
    }
    swipeBehavior.attachToRecyclerView(binding.recycler);

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    updateUI(savedInstanceState == null);
  }

  private void updateUI(boolean animated) {
    activity.getScrollBehavior().setUpScroll(R.id.scroll);
    activity.getScrollBehavior().setHideOnScroll(true);
    activity.updateBottomAppBar(
        Constants.FAB.POSITION.CENTER,
        R.menu.menu_empty,
        menuItem -> false
    );
    activity.updateFab(
        R.drawable.ic_round_add_anim,
        R.string.action_add,
        Constants.FAB.TAG.ADD,
        animated,
        () -> navigate(RecipeEditIngredientListFragmentDirections
          .actionRecipeEditIngredientListFragmentToRecipeEditIngredientEditFragment(
                  ACTION.CREATE,
                  viewModel.getAction()
          )
          .setRecipe(viewModel.getRecipe())
        )
    );
  }

  @Override
  public void onItemRowClicked(RecipePosition recipePosition, int position) {
    navigate(
        RecipeEditIngredientListFragmentDirections
            .actionRecipeEditIngredientListFragmentToRecipeEditIngredientEditFragment(
                    ACTION.EDIT,
                    viewModel.getAction()
            )
            .setRecipePosition(recipePosition)
            .setRecipe(viewModel.getRecipe())
    );
  }

  @Override
  public void deleteRecipePosition(int recipePositionId) {
    viewModel.deleteRecipePosition(recipePositionId);
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!isOnline);
    if (isOnline) {
      viewModel.downloadData();
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
