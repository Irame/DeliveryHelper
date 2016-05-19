package de.vanselow.deliveryhelper;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.BaseSwipeAdapter;
import com.hb.views.PinnedSectionListView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

import de.vanselow.deliveryhelper.utils.DatabaseHelper;

public class LocationListAdapter extends BaseSwipeAdapter implements PinnedSectionListView.PinnedSectionListAdapter {
    private static final String TAG = LocationListAdapter.class.getName();

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_SEPARATOR = 1;

    private ArrayList<ArrayList<LocationModel>> allValues;
    private ArrayList<String> sections;
    private long notesDisplayId;

    private LayoutInflater layoutInflater;
    private FragmentActivity activity;

    public LocationListAdapter(FragmentActivity activity, ArrayList<LocationModel> values) {
        this.activity = activity;
        layoutInflater = LayoutInflater.from(activity);
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

    public void addItem(LocationModel dl) {
        allValues.get(dl.state.ordinal()).add(dl);
        if (dl.state == LocationModel.State.DELIVERED) sortDeliveredAlphabetically();
        notifyDataSetChanged();
    }

    public LocationModel updateItem(LocationModel otherLocation) {
        for (ArrayList<LocationModel> sectionValues : allValues) {
            for (LocationModel location : sectionValues) {
                if (location.id == otherLocation.id && location.update(otherLocation)) {
                    notifyDataSetChanged();
                    return location;
                }
            }
        }
        return null;
    }

    public LocationModel removeItem(int position) {
        ItemInfo itemInfo = getItemInfo(position);
        LocationModel removedLoc = null;
        if (!itemInfo.isSectionHeader)
            removedLoc = allValues.get(itemInfo.section).remove(itemInfo.itemSectionPos);
        notifyDataSetChanged();
        return removedLoc;
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
        return getItemInfo(position).isSectionHeader ? TYPE_SEPARATOR : TYPE_ITEM;
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        switch (getItemViewType(position)) {
            case TYPE_SEPARATOR: return R.id.location_list_header_swipe;
            case TYPE_ITEM: return R.id.location_list_item_swipe;
            default: return -1;
        }
    }

    @Override
    public View generateView(int position, ViewGroup parent) {
        View v;
        ViewHolder viewHolder;
        int type = getItemViewType(position);
        switch (type) {
            case TYPE_ITEM:
                v = layoutInflater.inflate(R.layout.location_list_item, parent, false);
                viewHolder = new ItemViewHolder(v);
                v.setTag(viewHolder);
                break;
            case TYPE_SEPARATOR:
                v = layoutInflater.inflate(R.layout.location_list_header, parent, false);
                viewHolder = new HeaderViewHolder(v);
                v.setTag(viewHolder);
                break;
            default:
                Log.e(TAG, "Unknown Item type in location list");
                return null;
        }
        return v;
    }

    @Override
    public void fillValues(int position, View convertView) {
        ((ViewHolder) convertView.getTag()).setup(position);
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

    @Override
    public boolean isItemViewTypePinned(int viewType) {
        return viewType == TYPE_SEPARATOR;
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

    private class ItemViewHolder implements View.OnClickListener, ViewHolder, View.OnLongClickListener {
        public SwipeLayout swipeLayout;

        private View surfaceView;

        public ImageButton deleteButton;
        public ImageButton editButton;
        public ImageButton checkButton;

        public TextView nameLabel;
        public TextView addressLabel;
        public TextView priceLabel;
        public TextView notesLabel;

        private int position;
        private LocationModel loc;

        public ItemViewHolder(View itemView) {
            swipeLayout = (SwipeLayout) itemView;
            surfaceView = swipeLayout.getSurfaceView();

            nameLabel = (TextView) itemView.findViewById(R.id.location_list_item_name_label);
            addressLabel = (TextView) itemView.findViewById(R.id.location_list_item_address_label);
            priceLabel = (TextView) itemView.findViewById(R.id.location_list_item_price_label);
            notesLabel = (TextView) itemView.findViewById(R.id.location_list_item_notes_label);

            deleteButton = (ImageButton) itemView.findViewById(R.id.location_list_item_delete_button);
            editButton = (ImageButton) itemView.findViewById(R.id.location_list_item_edit_button);
            checkButton = (ImageButton) itemView.findViewById(R.id.location_list_item_check_button);

            surfaceView.setOnClickListener(this);
            surfaceView.setOnLongClickListener(this);
            deleteButton.setOnClickListener(this);
            editButton.setOnClickListener(this);
            checkButton.setOnClickListener(this);

            swipeLayout.setClickToClose(true);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == surfaceView.getId()) {
                notesDisplayId = loc.id == notesDisplayId ? -1 : loc.id;
                notifyDataSetChanged();
            } else if (v.getId() == deleteButton.getId()) {
                swipeLayout.close(false);
                LocationModel loc = removeItem(position);
                DatabaseHelper.getInstance(activity).deleteRouteLocation(loc);
            } else if (v.getId() == editButton.getId()) {
                Intent intent = new Intent(activity.getApplicationContext(), LocationAddActivity.class);
                intent.putExtra(LocationAddActivity.LOCATION_RESULT_KEY, loc);
                activity.startActivityForResult(intent, LocationListActivity.EDIT_LOCATION_REQUEST_CODE);
                swipeLayout.close();
            } else if (v.getId() == checkButton.getId()) {
                LocationModel loc = removeItem(position);
                loc.state = LocationModel.State.DELIVERED;
                DatabaseHelper.getInstance(activity).updateRouteLocation(loc);
                addItem(loc);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            LocationModel loc = (LocationModel) getItem(position);
            Uri gmmIntentUri = Uri.parse(String.format(Locale.ENGLISH, "google.navigation:q=%f,%f", loc.latitude, loc.longitude));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            v.getContext().startActivity(mapIntent);
            return true;
        }

        public void setup(int position) {
            this.position = position;

            ItemInfo itemInfo = getItemInfo(position);
            loc = allValues.get(itemInfo.section).get(itemInfo.itemSectionPos);

            nameLabel.setText(loc.name);
            addressLabel.setText(loc.address);

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
            priceLabel.setText(currencyFormat.format(loc.price));

            if (loc.notes.isEmpty()) {
                notesLabel.setText(R.string.no_note_available);
                notesLabel.setEnabled(false);
            } else {
                notesLabel.setText(loc.notes);
                notesLabel.setEnabled(true);
            }

            if (notesDisplayId == loc.id)
                notesLabel.setVisibility(View.VISIBLE);
            else
                notesLabel.setVisibility(View.GONE);

            if (loc.state == LocationModel.State.DELIVERED)
                checkButton.setVisibility(View.GONE);
            else
                checkButton.setVisibility(View.VISIBLE);
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
