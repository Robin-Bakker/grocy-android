package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class ShoppingListItemAdapter extends RecyclerView.Adapter<ShoppingListItemAdapter.ViewHolder> {

    private final static String TAG = ShoppingListItemAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private Context context;
    private ArrayList<GroupedListItem> groupedListItems;
    private ArrayList<QuantityUnit> quantityUnits;
    private ShoppingListItemAdapterListener listener;

    public ShoppingListItemAdapter(
            Context context,
            ArrayList<GroupedListItem> groupedListItems,
            ArrayList<QuantityUnit> quantityUnits,
            ShoppingListItemAdapterListener listener
    ) {
        this.context = context;
        this.groupedListItems = groupedListItems;
        this.quantityUnits = quantityUnits;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutContainer, linearLayoutNote;
        private TextView textViewName, textViewAmount, textViewGroupName, textViewNote;
        private View divider;

        public ViewHolder(View view) {
            super(view);

            linearLayoutContainer = view.findViewById(R.id.linear_shopping_list_item_container);
            linearLayoutNote = view.findViewById(R.id.linear_shopping_list_note);
            textViewName = view.findViewById(R.id.text_shopping_list_item_name);
            textViewAmount = view.findViewById(R.id.text_shopping_list_item_amount);
            textViewNote = view.findViewById(R.id.text_shopping_list_note);

            textViewGroupName = view.findViewById(R.id.text_shopping_list_group_name);
            divider = view.findViewById(R.id.view_shopping_list_group_divider);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return groupedListItems.get(position).getType();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == GroupedListItem.TYPE_HEADER) {
            return new ViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.row_shopping_list_group,
                            parent,
                            false
                    )
            );
        } else {
            return new ViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.row_shopping_list_item,
                            parent,
                            false
                    )
            );
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        GroupedListItem groupedListItem = groupedListItems.get(holder.getAdapterPosition());

        int type = getItemViewType(position);
        if (type == GroupedListItem.TYPE_HEADER) {
            if(holder.getAdapterPosition() == 0) {
                holder.divider.setVisibility(View.GONE);
            }
            holder.textViewGroupName.setText(((ProductGroup) groupedListItem).getName());
            return;
        }

        ShoppingListItem shoppingListItem = (ShoppingListItem) groupedListItem;

        // NAME

        Product product = shoppingListItem.getProduct();
        if(product != null) {
            holder.textViewName.setText(product.getName());
            holder.textViewName.setVisibility(View.VISIBLE);
        } else {
            holder.textViewName.setText(null);
            holder.textViewName.setVisibility(View.GONE);
        }
        if(shoppingListItem.isUndone()) {
            holder.textViewName.setPaintFlags(
                    holder.textViewName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        } else {
            holder.textViewName.setPaintFlags(
                    holder.textViewName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // AMOUNT

        if(shoppingListItem.getProduct() != null) {
            QuantityUnit quantityUnit = new QuantityUnit();
            for(int i = 0; i < quantityUnits.size(); i++) {
                if(quantityUnits.get(i).getId() == shoppingListItem.getProduct().getQuIdPurchase()) {
                    quantityUnit = quantityUnits.get(i);
                    break;
                }
            }

            if(DEBUG) Log.i(TAG, "onBindViewHolder: " + quantityUnit.getName());

            holder.textViewAmount.setText(
                    context.getString(
                            R.string.subtitle_amount,
                            NumUtil.trim(shoppingListItem.getAmount()),
                            shoppingListItem.getAmount() == 1
                                    ? quantityUnit.getName()
                                    : quantityUnit.getNamePlural()
                    )
            );
        } else {
            holder.textViewAmount.setText(NumUtil.trim(shoppingListItem.getAmount()));
        }

        if(shoppingListItem.isMissing()) {
            holder.textViewAmount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_medium)
            );
            holder.textViewAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.retro_blue_dark)
            );
        } else {
            holder.textViewAmount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_regular)
            );
            holder.textViewAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.on_background_secondary)
            );
        }
        if(shoppingListItem.isUndone()) {
            holder.textViewAmount.setPaintFlags(
                    holder.textViewAmount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        } else {
            holder.textViewAmount.setPaintFlags(
                    holder.textViewAmount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // DESCRIPTION

        if(shoppingListItem.getNote() != null && !shoppingListItem.getNote().equals("")) {
            holder.linearLayoutNote.setVisibility(View.VISIBLE);
            holder.textViewNote.setText(shoppingListItem.getNote().trim());
        } else {
            holder.linearLayoutNote.setVisibility(View.GONE);
            holder.textViewNote.setText(null);
        }
        if(shoppingListItem.isUndone()) {
            holder.textViewNote.setPaintFlags(
                    holder.textViewNote.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        } else {
            holder.textViewNote.setPaintFlags(
                    holder.textViewNote.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // CONTAINER

        holder.linearLayoutContainer.setOnClickListener(
                view -> listener.onItemRowClicked(holder.getAdapterPosition())
        );

    }

    @Override
    public int getItemCount() {
        return groupedListItems != null ? groupedListItems.size() : 0;
    }

    public interface ShoppingListItemAdapterListener {
        void onItemRowClicked(int position);
    }
}
