package de.vanselow.deliveryhelper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hb.views.PinnedSectionListView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import de.vanselow.deliveryhelper.utils.DatabaseHelper;

/**
 * Created by Felix on 12.05.2016.
 */
public class LocationListAdapter extends BaseAdapter implements PinnedSectionListView.PinnedSectionListAdapter {
    private static final String TAG = LocationListAdapter.class.getName();

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_SEPERATOR = 1;

    private ArrayList<ArrayList<LocationModel>> allValues;
    private ArrayList<String> sections;

    private LayoutInflater layoutInflater;
    private Context context;

    public int selectedItemPosition = -1;

    public LocationListAdapter(Context context, ArrayList<LocationModel> values) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        allValues = new ArrayList<>();
        sections = new ArrayList<>();
        for (LocationModel.State state : LocationModel.State.values()) {
            allValues.add(new ArrayList<LocationModel>());
            sections.add(state.sectionText);
        }
        for (LocationModel dl : values) {
            allValues.get(dl.state.ordinal()).add(dl);
        }
        sortDeliveredAlphabetically();
    }

    @Override
    public int getCount() {
        int count = 0;
        for (ArrayList<LocationModel> sectionValues : allValues) {
            count += sectionValues.size();
        }
        count += sections.size();
        return count;
    }

    private ItemInfo getItemInfo(int position) {
        int i = 0;
        int sectionPos = 1;
        for (ArrayList<LocationModel> sectionValues : allValues) {
            int tempSectionPos = sectionPos + sectionValues.size();
            if (tempSectionPos > position) break;
            sectionPos = tempSectionPos + 1;
            i++;
        }
        int itemSectionPos = position-sectionPos;
        return new ItemInfo(i, itemSectionPos, itemSectionPos < 0);
    }

    public LocationModel removeItem(int position) {
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

        ViewHolder viewHolder;
        if (v == null) {
            int type = getItemViewType(position);
            switch (type) {
                case TYPE_ITEM:
                    v = layoutInflater.inflate(R.layout.location_list_item, null);
                    viewHolder = new ItemViewHolder(v);
                    v.setTag(viewHolder);
                    break;
                case TYPE_SEPERATOR:
                    v = layoutInflater.inflate(R.layout.location_list_header, null);
                    viewHolder = new HeaderViewHolder(v);
                    v.setTag(viewHolder);
                    break;
                default:
                    Log.e(TAG, "Unknown Item type in location list");
                    return null;
            }
        } else {
            viewHolder = (ViewHolder) v.getTag();
        }

        viewHolder.setup(position);
        return v;
    }

    public ArrayList<LocationModel> getValuesForSection(LocationModel.State state) {
        return allValues.get(state.ordinal());
    }

    public ArrayList<LocationModel> getAllValues() {
        ArrayList<LocationModel> result = new ArrayList<>();
        for (ArrayList<LocationModel> sectionValues : allValues) {
            result.addAll(sectionValues);
        }
        return result;
    }

    public void addItem(LocationModel dl) {
        allValues.get(dl.state.ordinal()).add(dl);
        if (dl.state == LocationModel.State.DELIVERED) sortDeliveredAlphabetically();
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

    public void sortDeliveredAlphabetically() {
        ArrayList<LocationModel> deliveredLocations = allValues.get(LocationModel.State.DELIVERED.ordinal());
        Collections.sort(deliveredLocations, new Comparator<LocationModel>() {
            @Override
            public int compare(LocationModel lhs, LocationModel rhs) {
                return lhs.name.compareTo(rhs.name);
            }
        });
    }

    private interface ViewHolder {
        void setup(int position);
    }

    private class ItemViewHolder implements View.OnClickListener, ViewHolder {
        public View itemView;

        public TextView nameLabel;
        public TextView addressLabel;
        public TextView priceLabel;
        public TextView notesLabel;
        public LinearLayout buttonBar;
        public ImageButton deleteButton;
        public ImageButton navButton;
        public ImageButton doneButton;

        private int position;

        public ItemViewHolder(View itemView) {
            this.itemView = itemView;

            nameLabel = (TextView) itemView.findViewById(R.id.location_list_item_name_label);
            addressLabel = (TextView) itemView.findViewById(R.id.location_list_item_address_label);
            priceLabel = (TextView) itemView.findViewById(R.id.location_list_item_price_label);
            notesLabel = (TextView) itemView.findViewById(R.id.location_list_item_notes_label);
            buttonBar = (LinearLayout) itemView.findViewById(R.id.location_list_item_button_bar);
            deleteButton = (ImageButton) itemView.findViewById(R.id.location_list_item_delete_button);
            navButton = (ImageButton) itemView.findViewById(R.id.location_list_item_nav_button);
            doneButton = (ImageButton) itemView.findViewById(R.id.location_list_item_done_button);

            itemView.setOnClickListener(this);
            deleteButton.setOnClickListener(this);
            navButton.setOnClickListener(this);
            doneButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == itemView.getId()) {
                if (selectedItemPosition == position)
                    selectedItemPosition = -1;
                else
                    selectedItemPosition = position;
                notifyDataSetChanged();
            } else if (v.getId() == deleteButton.getId()) {
                LocationModel loc = removeItem(position);
                DatabaseHelper.getInstance(v.getContext()).deleteRouteLocation(loc);
                selectedItemPosition = -1;
                notifyDataSetChanged();
            } else if (v.getId() == navButton.getId()) {
                LocationModel loc = (LocationModel) getItem(position);
                Uri gmmIntentUri = Uri.parse(String.format(Locale.ENGLISH, "google.navigation:q=%f,%f", loc.latitude, loc.longitude));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                v.getContext().startActivity(mapIntent);
            } else if (v.getId() == doneButton.getId()) {
                LocationModel loc = removeItem(position);
                loc.state = LocationModel.State.DELIVERED;
                DatabaseHelper.getInstance(v.getContext()).updateRouteLocation(loc);
                addItem(loc);
                notifyDataSetChanged();
            }
        }

        public void setup(int position) {
            this.position = position;

            ItemInfo itemInfo = getItemInfo(position);
            LocationModel item = allValues.get(itemInfo.section).get(itemInfo.itemSectionPos);

            if (selectedItemPosition == position) {
                buttonBar.setVisibility(View.VISIBLE);
                notesLabel.setVisibility(item.notes.isEmpty() ? View.GONE : View.VISIBLE);
            } else {
                buttonBar.setVisibility(View.GONE);
                notesLabel.setVisibility(View.GONE);
            }
            nameLabel.setText(item.name);
            addressLabel.setText(item.address);
            notesLabel.setText(item.notes);

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
            priceLabel.setText(currencyFormat.format(item.price));
        }
    }

    private class HeaderViewHolder implements ViewHolder {
        private TextView sectionHeaderLabel;

        public HeaderViewHolder(View itemView) {
            sectionHeaderLabel = (TextView) itemView.findViewById(R.id.location_list_section_header_text);
        }

        public void setup(int position) {
            ItemInfo itemInfo = getItemInfo(position);
            sectionHeaderLabel.setText(sections.get(itemInfo.section));
        }
    }
}
