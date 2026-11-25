package com.enmar.launcher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<AppInfo> allApps = new ArrayList<>();
    private final List<AppInfo> filteredApps = new ArrayList<>();

    public AppAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setApps(List<AppInfo> apps) {
        allApps.clear();
        allApps.addAll(apps);
        filteredApps.clear();
        filteredApps.addAll(apps);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredApps.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredApps.addAll(allApps);
        } else {
            String lower = query.toLowerCase(Locale.getDefault()).trim();
            for (AppInfo app : allApps) {
                if (app.label.toLowerCase(Locale.getDefault()).contains(lower)) {
                    filteredApps.add(app);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return filteredApps.size();
    }

    @Override
    public AppInfo getItem(int position) {
        return filteredApps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_app, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.app_icon);
            holder.label = convertView.findViewById(R.id.app_label);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppInfo info = getItem(position);
        holder.icon.setImageDrawable(info.icon);
        holder.label.setText(info.label);
        return convertView;
    }

    private static class ViewHolder {
        ImageView icon;
        TextView label;
    }
}
