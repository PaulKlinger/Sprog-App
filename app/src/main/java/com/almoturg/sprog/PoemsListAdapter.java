package com.almoturg.sprog;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.Calendar;
import java.util.Locale;

import static com.almoturg.sprog.SprogApplication.filtered_poems;


class PoemsListAdapter extends RecyclerView.Adapter<PoemsListAdapter.ViewHolder>
        implements FastScrollRecyclerView.SectionedAdapter {
    private Context context;
    private Calendar cal;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        View content_wrapper;
        View first_line;
        public CardView view;

        ViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            this.content_wrapper = v.findViewById(R.id.content_wrapper);
            this.view = (CardView) v;
            this.first_line = v.findViewById(R.id.first_line);
        }

        @Override
        public void onClick(View v) {
            // I'm really not sure about getLayoutPosition (instead of getAdapterPosition())
            // but at least it doesn't return -1 and cause an error.
            // I think as long as poems are only added at the end it should be ok.
            if (content_wrapper.getVisibility() == View.GONE) {
                first_line.setVisibility(View.GONE);
                content_wrapper.setVisibility(View.VISIBLE);
                ((TextView) v.findViewById(R.id.content)).setText(filtered_poems.get(getLayoutPosition()).content);
            } else {
                Intent intent = new Intent(context, PoemActivity.class);
                intent.putExtra("POEM_ID", getLayoutPosition());
                context.startActivity(intent);
            }
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    PoemsListAdapter(Context context) {
        this.context = context;
        this.cal = Calendar.getInstance(Locale.ENGLISH);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.poem_row, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Poem poem = (filtered_poems.get(position));

        Util.update_poem_row(poem, holder.view, false, true, context);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return filtered_poems.size();
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        if (((MainActivity) context).sort_order.equals("Date")) {
            cal.setTimeInMillis((long) filtered_poems.get(position).timestamp * 1000);
            return DateFormat.format("yyyy-MM", cal).toString();
        } else if (((MainActivity) context).sort_order.equals("Score")) {
            return Integer.toString(filtered_poems.get(position).score);
        } else if (((MainActivity) context).sort_order.equals("Gold")) {
            return Integer.toString(filtered_poems.get(position).gold);
        }
        return "";
    }
}
