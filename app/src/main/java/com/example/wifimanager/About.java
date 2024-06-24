package com.example.wifimanager;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class About extends Fragment {

    public About() {
        // Required empty public constructor
    }

    public static About newInstance() {
        return new About();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        // About information
        TextView aboutTextView = view.findViewById(R.id.textViewAbout);
        aboutTextView.setText("About information\nVersion: 1.0\nDeveloped by: ENI\nWi-Fi Manager");

        return view;
    }
}
