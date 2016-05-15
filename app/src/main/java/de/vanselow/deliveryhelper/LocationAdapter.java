package de.vanselow.deliveryhelper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hb.views.PinnedSectionListView;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Felix on 12.05.2016.
 */
public class LocationAdapter extends BaseAdapter implements PinnedSectionListView.PinnedSectionListAdapter {
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_SEPERATOR = 1;

    private ArrayList<ArrayList<DeliveryLocationModel>> allValues;
    private ArrayList<String> sections;

    private LayoutInflater layoutInflater;
    private Context context;

    public int selectedItemPosition = -1;

    public LocationAdapter(Context context, ArrayList<DeliveryLocationModel> values) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        allValues = new ArrayList<>();
        sections = new ArrayList<>();
        for (DeliveryLocationModel.State state : DeliveryLocationModel.State.values()) {
            allValues.add(new ArrayList<DeliveryLocationModel>());
            sections.add(state.sectionText);
        }
        for (DeliveryLocationModel dl : values) {
            allValues.get(dl.state.ordinal()).add(dl);
        }
    }

    @Override
    public int getCount() {
        int count = 0;
        for (ArrayList<DeliveryLocationModel> sectionValues : allValues) {
            count += sectionValues.size();
        }
        count += sections.size();
        return count;
    }

    private ItemInfo getItemInfo(int position) {
        int i = 0;
        int sectionPos = 1;
        for (ArrayList<DeliveryLocationModel> sectionValues : allValues) {
            int tempSectionPos = sectionPos + sectionValues.size();
            if (tempSectionPos > position) break;
            sectionPos = tempSectionPos + 1;
            i++;
        }
        int itemSectionPos = position-sectionPos;
        return new ItemInfo(i, itemSectionPos, itemSectionPos < 0);
    }

    public DeliveryLocationModel removeItem(int position) {
        ItemInfo itemInfo = getItemInfo(position);
        return itemInfo.isSectionHeader ? null : allValues.get(itemInfo.section).remove(itemInfo.itemSectionPos);
    }

    @Override
    public Object getItem(int position) {
        ItemInfo itemInfo = getItemInfo(position);
        return itemInfo.isSectionHeader ? sections.get(itemInfo.section) : allValues.get(itemInfo.section).get(itemInfo.itemSectionPos);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return getItemInfo(position).isSectionHeader ? TYPE_SEPERATOR : TYPE_ITEM;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        int type = getItemViewType(position);
        if (v == null) {
            switch (type) {
                case TYPE_ITEM:
                    v = layoutInflater.inflate(R.layout.location_list_row, null);

                    v.findViewById(R.id.location_list_item_delete_button).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            DeliveryLocationModel loc = removeItem(selectedItemPosition);
                            LocationsDatabaseHelper.getInstance(context).deleteLocation(loc);
                            selectedItemPosition = -1;
                            notifyDataSetChanged();
                        }
                    });

                    v.findViewById(R.id.location_list_item_nav_button).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            DeliveryLocationModel loc = (DeliveryLocationModel) getItem(selectedItemPosition);
                            Uri gmmIntentUri = Uri.parse(String.format(Locale.ENGLISH, "google.navigation:q=%f,%f", loc.latitude, loc.longitude));
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");
                            context.startActivity(mapIntent);
                        }
                    });

                    v.findViewById(R.id.location_list_item_done_button).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            DeliveryLocationModel loc = removeItem(selectedItemPosition);
                            loc.state = DeliveryLocationModel.State.DELIVERED;
                            LocationsDatabaseHelper.getInstance(context).addOrUpdateLocation(loc);
                            addItem(loc);
                        }
                    });
                    break;
                case TYPE_SEPERATOR:
                    v = layoutInflater.inflate(R.layout.location_list_section_header, null);
                    break;
            }

        }

        if (type == TYPE_ITEM) {
            View buttonBar = v.findViewById(R.id.location_list_item_button_bar);
            if (selectedItemPosition == position)
                buttonBar.setVisibility(View.VISIBLE);
            else
                buttonBar.setVisibility(View.GONE);
        }

        Object p = getItem(position);

        if (p != null) {
            switch (type) {
                case TYPE_ITEM:
                    DeliveryLocationModel loc = (DeliveryLocationModel) p;

                    TextView name = (TextView) v.findViewById(R.id.location_list_item_name_label);
                    TextView address = (TextView) v.findViewById(R.id.location_list_item_address_label);
                    TextView price = (TextView) v.findViewById(R.id.location_list_item_price_label);
                    TextView notes = (TextView) v.findViewById(R.id.location_list_item_notes_label);

                    if (name != null) {
                        name.setText(loc.name);
                    }

                    if (address != null) {
                        address.setText(loc.address);
                    }

                    if (price != null) {
                        price.setText(String.format(Locale.GERMANY, "%.2f€", loc.price));
                    }

                    if (notes != null) {
                        if (loc.notes.isEmpty() || selectedItemPosition != position) {
                            notes.setVisibility(View.GONE);
                            notes.setText(null);
                        } else {
                            notes.setVisibility(View.VISIBLE);
                            notes.setText(loc.notes);
                        }
                    }
                    break;
                case TYPE_SEPERATOR:
                    String sectionStr = (String) p;

                    TextView section = (TextView) v.findViewById(R.id.location_list_section_header_text);

                    if (section != null) {
                        section.setText(sectionStr);
                    }
                    break;
            }
        }

        return v;
    }

    public ArrayList<DeliveryLocationModel> getValuesForSection(DeliveryLocationModel.State state) {
        return allValues.get(state.ordinal());
    }

    public ArrayList<DeliveryLocationModel> getAllValues() {
        ArrayList<DeliveryLocationModel> result = new ArrayList<>();
        for (ArrayList<DeliveryLocationModel> sectionValues : allValues) {
            result.addAll(sectionValues);
        }
        return result;
    }

    public void addItem(DeliveryLocationModel dl) {
        allValues.get(dl.state.ordinal()).add(dl);
        notifyDataSetChanged();
    }

    @Override
    public boolean isItemViewTypePinned(int viewType) {
        return viewType == TYPE_SEPERATOR;
    }

    private class ItemInfo {
        public final int section;
        public final int itemSectionPos;
        public final boolean isSectionHeader;

        public ItemInfo(int section, int itemSectionPos, boolean isSectionHeader) {
            this.section = section;
            this.itemSectionPos = itemSectionPos;
            this.isSectionHeader = isSectionHeader;
        }
    }
}
