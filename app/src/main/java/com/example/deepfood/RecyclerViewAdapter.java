package com.example.deepfood;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Food> foodList;
    private int itemResource;

    public RecyclerViewAdapter(Context context, ArrayList<Food> foodList) {
        this.context = context;
        this.foodList = foodList;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.food_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Food food = (Food) foodList.get(position);

        holder.name.setText(food.getName());
        holder.calorie.setText(food.getCalories());

        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FoodDB foodDB = new FoodDB(context);
                foodDB.deleteFood(foodList.get(position));
                deleteItem(position);

            }
        });


    }


    @Override
    public int getItemCount() {
        return this.foodList.size();
    }
    public void deleteItem(int pos){

        foodList.remove(pos);
        notifyItemRemoved(pos);
        notifyItemRangeChanged(pos, foodList.size());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView calorie;
        private FloatingActionButton delete;
        private View parentView;

        public ViewHolder(View itemView){
            super(itemView);
            this.parentView = itemView;
            this.name = (TextView)itemView.findViewById(R.id.heading);
            this.calorie = (TextView)itemView.findViewById(R.id.text);
            this.delete = (FloatingActionButton)itemView.findViewById(R.id.delete);
        }

    }

}
