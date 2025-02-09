package com.minar.randomix.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.minar.randomix.R;
import com.minar.randomix.adapter.RecentAdapter;
import com.minar.randomix.utilities.OnItemClickListener;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouletteBottomSheet extends BottomSheetDialogFragment {
    private List<List<String>> recentList;
    private final RouletteFragment roulette;
    private SharedPreferences sp;
    private RecentAdapter adapter;

    RouletteBottomSheet(RouletteFragment roulette) {
        this.roulette = roulette;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the bottom sheet, initialize the shared preferences and the recent options list
        View v = inflater.inflate(R.layout.roulette_bottom_sheet, container, false);
        sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        String recent = sp.getString("recent", "");
        Gson gson = new Gson();
        Type type = new TypeToken<List<List<String>>>() {}.getType();
        recentList = gson.fromJson(recent, type);
        if (recentList == null) recentList = new ArrayList<>();

        // Take the recycler view and the main layout, and populate the recycler
        RecyclerView recentListLayout = v.findViewById(R.id.recentList);
        ConstraintLayout rouletteBottomSheet = v.findViewById(R.id.rouletteBottomSheet);
        populateRecentLayout(recentListLayout, rouletteBottomSheet);

        // Animate the drawable in loop
        ImageView noRecentImage = v.findViewById(R.id.recentImage);
        Drawable animatedNoRecent = noRecentImage.getDrawable();
        if (animatedNoRecent instanceof Animatable2) {
            ((Animatable2) animatedNoRecent).registerAnimationCallback(new Animatable2.AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable drawable) {
                    ((Animatable2) animatedNoRecent).start();
                }
            });
            ((Animatable2) animatedNoRecent).start();
        }
        return v;
    }

    // Insert the recent options in the recycler view
    private void populateRecentLayout(RecyclerView recentListLayout, ConstraintLayout rouletteBottomSheet) {
        // Case 1, placeholder
        if (recentList == null || recentList.isEmpty())
            rouletteBottomSheet.findViewById(R.id.recentNoResult).setVisibility(View.VISIBLE);

        // Case 2, populate the recycler
        else {
            adapter = new RecentAdapter(getContext(), recentList);
            recentListLayout.setLayoutManager(new LinearLayoutManager(requireContext()));
            recentListLayout.setAdapter(adapter);
            adapter.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(int position, List<String> optionList,View view) {
                    roulette.restoreOption(optionList);
                }

                @Override
                public void onItemLongClick(int position, View view) {
                    deleteRecent(position, requireContext());
                }
            });
        }
    }

    // Update the stored value of the recent options
    void deleteRecent(int index, Context context) {
        Gson gson = new Gson();
        fetchRecentList(context);
        recentList.remove(index);
        adapter.notifyItemRemoved(index);
        String json = gson.toJson(recentList);
        sp.edit().putString("recent", json).apply();
    }

    // Update the stored value of the recent options
    void updateRecent(List<String> options, Context context) {
        Gson gson = new Gson();
        fetchRecentList(context);
        insertInRecent(options);
        String json = gson.toJson(recentList);
        sp.edit().putString("recent", json).apply();
    }

    // Restore the latest option list
    void restoreLatest(Context context) {
        fetchRecentList(context);
        if (recentList.size() > 0) {
            List<String> lastOption = recentList.get(recentList.size() - 1);
            roulette.restoreOption(lastOption);
        }
    }

    // Insert a new list in the recent options list
    private void insertInRecent(List<String> newRecent) {
        // Check if there's a duplicate, create a copy to avoid overwriting
        List<String> values = new ArrayList<>(newRecent);
        for (List<String> elem : recentList) {
            if (newRecent.size() == elem.size()) {
                newRecent = new ArrayList<>(newRecent);
                elem = new ArrayList<>(elem);
                Collections.sort(newRecent);
                Collections.sort(elem);
                if (newRecent.equals(elem)) return;
            }
        }
        // Keep 10 recent only
        recentList.add(values);
        if (recentList.size() > 10) recentList.remove(0);
    }

    // Fetch the recent list from share preferences or initialize it
    private void fetchRecentList(Context context) {
        Gson gson = new Gson();
        if (recentList == null) {
            // Shared preferences may be null if the method is called without opening the dialog
            if (sp == null) sp = PreferenceManager.getDefaultSharedPreferences(context);
            String recent = sp.getString("recent", "");
            Type type = new TypeToken<List<List<String>>>() {
            }.getType();
            recentList = gson.fromJson(recent, type);
            if (recentList == null) recentList = new ArrayList<>();
        }
    }
}
