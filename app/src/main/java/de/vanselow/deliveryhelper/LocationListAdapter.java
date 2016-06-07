package de.vanselow.deliveryhelper;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.BaseSwipeAdapter;
import com.google.android.gms.maps.model.LatLng;

import java.text.Collator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import de.vanselow.deliveryhelper.utils.DatabaseAsync;
import de.vanselow.deliveryhelper.utils.DatabaseHelper;
import de.vanselow.deliveryhelper.utils.ParcelableArrayList;
import de.vanselow.deliveryhelper.utils.Utils;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class LocationListAdapter extends BaseSwipeAdapter implements StickyListHeadersAdapter, Parcelable {
    private static final String TAG = LocationListAdapter.class.getName();

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_SEPARATOR = 1;
    private static final int TYPE_EPTY_NOTE = 2;

    private ParcelableArrayList<LocationModel>[] allValues;
    private String[] sections;
    private String[] emptySectionTexts;
    private boolean[] externallySorted;
    private long notesDisplayId;

    private LayoutInflater layoutInflater;
    private FragmentActivity activity;
    private long routeId;

    ItemCollectionChangedListener itemCollectionChangedListener;

    @SuppressWarnings("unchecked")
    public LocationListAdapter(FragmentActivity activity, long routeId) {
        this.activity = activity;
        this.routeId = routeId;
        layoutInflater = LayoutInflater.from(activity);
        int stateCount = LocationModel.State.values().length;
        allValues = new ParcelableArrayList[stateCount];
        sections = new String[stateCount];
        emptySectionTexts = new String[stateCount];
        externallySorted = new boolean[stateCount];
        for (LocationModel.State state : LocationModel.State.values()) {
            allValues[state.ordinal()] = new ParcelableArrayList<>();
            sections[state.ordinal()] = activity.getString(state.sectionStringId);
            emptySectionTexts[state.ordinal()] = activity.getString(state.emptyListStringId);
            externallySorted[state.ordinal()] = false;
        }
    }

    @Override
    public int getCount() {
        int count = 0;
        for (List<LocationModel> sectionValues : allValues) {
            count += Math.max(sectionValues.size(), 1);
        }
        return count;
    }

    private ItemInfo getItemInfo(int position) {
        int type = -1;
        int section = -1;
        while (position >= 0) {
            section++;
            int sectionSize = allValues[section].size();
            if (position == 0 && sectionSize == 0) {
                type = TYPE_EPTY_NOTE;
                break;
            }
            if (position < sectionSize) {
                type = TYPE_ITEM;
                break;
            }
            position -= Math.max(sectionSize, 1);
        }
        return new ItemInfo(section, position, type);
    }

    public void updateLocationFromDatabase(final long locationId) {
        if (locationId < 0) return;
        DatabaseAsync.getInstance(activity).getRouteLocationById(locationId, new DatabaseAsync.Callback<LocationModel>() {
            @Override
            public void onPostExecute(LocationModel locationModel) {
                boolean found = false;
                for (int section = 0; section < allValues.length; section++) {
                    List<LocationModel> sectionValues = allValues[section];
                    for (Iterator<LocationModel> iterator = sectionValues.iterator(); iterator.hasNext(); ) {
                        LocationModel location = iterator.next();
                        if (location.id == locationId) {
                            if (locationModel == null || locationModel.state.ordinal() != section) {
                                if (locationModel == null) found = true;
                                iterator.remove();
                            } else {
                                found = true;
                                location.update(locationModel);
                            }
                            onItemCollectionChanged();
                            break;
                        }
                    }
                }
                if (!found) {
                    allValues[locationModel.state.ordinal()].add(locationModel);
                    externallySorted[locationModel.state.ordinal()] = false;
                    onItemCollectionChanged();
                }
            }
        });
    }

    public void updateAllLocationsFromDatabase() {
        activity.findViewById(R.id.location_list_loading_panel).setVisibility(View.VISIBLE);
        DatabaseAsync.getInstance(activity).getAllRouteLocations(routeId, new DatabaseAsync.Callback<ArrayList<LocationModel>>() {
            @Override
            public void onPostExecute(ArrayList<LocationModel> locationModels) {
                for (int i = 0; i < allValues.length; i++) {
                    allValues[i].clear();
                    externallySorted[i] = false;
                }
                for (LocationModel location : locationModels) {
                    allValues[location.state.ordinal()].add(location);
                }
                onItemCollectionChanged();
                activity.findViewById(R.id.location_list_loading_panel).setVisibility(View.GONE);
            }
        });
    }

    @Override
    public Object getItem(int position) {
        ItemInfo itemInfo = getItemInfo(position);
        switch (itemInfo.type) {
            case TYPE_SEPARATOR:
                return sections[itemInfo.section];
            case TYPE_ITEM:
                return allValues[itemInfo.section].get(itemInfo.relativeItemPos);
            case TYPE_EPTY_NOTE:
                return emptySectionTexts[itemInfo.section];
            default:
                return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        return getItemInfo(position).type;
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        switch (getItemViewType(position)) {
            case TYPE_SEPARATOR: return R.id.location_list_header_swipe;
            case TYPE_ITEM: return R.id.location_list_item_swipe;
            case TYPE_EPTY_NOTE: return R.id.list_emptynote_swipe;
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
                break;
            case TYPE_EPTY_NOTE:
                v = layoutInflater.inflate(R.layout.list_emptynote, parent, false);
                viewHolder = new EmptyNoteViewHolder(v);
                break;
            default:
                Log.e(TAG, "Unknown Item type in location list");
                return null;
        }
        v.setTag(viewHolder);
        return v;
    }

    @Override
    public void fillValues(int position, View convertView) {
        ((ViewHolder) convertView.getTag()).setup(position);
    }

    public List<LocationModel> getValuesForSection(LocationModel.State state) {
        return allValues[state.ordinal()];
    }

    public List<LocationModel> getAllValues() {
        ArrayList<LocationModel> result = new ArrayList<>();
        for (List<LocationModel> sectionValues : allValues) {
            result.addAll(sectionValues);
        }
        return result;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder;
        if (convertView == null) {
            v = layoutInflater.inflate(R.layout.location_list_header, parent, false);
            viewHolder = new HeaderViewHolder(v);
            v.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) v.getTag();
        }
        viewHolder.setup(position);
        return v;
    }

    @Override
    public long getHeaderId(int position) {
        return getItemInfo(position).section;
    }

    private class ItemInfo {
        public final int section;
        public final int relativeItemPos;
        public final int type;

        public ItemInfo(int section, int relativeItemPos, int type) {
            this.section = section;
            this.relativeItemPos = relativeItemPos;
            this.type = type;
        }

        public boolean isHeader() {
            return type == TYPE_SEPARATOR;
        }

        public boolean isItem() {
            return type == TYPE_ITEM;
        }

        public boolean isEptyNote() {
            return type == TYPE_EPTY_NOTE;
        }
    }

    public interface ItemCollectionChangedListener {
        void onChanged();
    }

    public void setItemCollectionChangedListener(ItemCollectionChangedListener listener) {
        itemCollectionChangedListener = listener;
    }

    private void onItemCollectionChanged() {
        itemCollectionChangedListener.onChanged();
        sortAlphabetically();
        notifyDataSetChanged();
    }

    public void sortAlphabetically() {
        final Collator collator = Collator.getInstance();
        collator.setStrength(Collator.PRIMARY);
        for (int i = 0; i < allValues.length; i++) {
            if (externallySorted[i]) continue;
            List<LocationModel> sectionValues = allValues[i];
            Collections.sort(sectionValues, new Comparator<LocationModel>() {
                @Override
                public int compare(LocationModel lhs, LocationModel rhs) {
                    return collator.compare(lhs.name, rhs.name);
                }
            });
        }
    }

    public void customSort(LocationModel.State state, Comparator<LocationModel> comparator) {
        externallySorted[state.ordinal()] = true;
        Collections.sort(allValues[state.ordinal()], comparator);
        notifyDataSetChanged();
    }

    public void resetCustomSort(LocationModel.State state) {
        externallySorted[state.ordinal()] = false;
        sortAlphabetically();
        notifyDataSetChanged();
    }

    public void setActivity(FragmentActivity activity) {
        this.activity = activity;
    }

    protected LocationListAdapter(Parcel in) {
        this(null, 0);
        in.readTypedArray(allValues, ParcelableArrayList.CREATOR);
        in.readBooleanArray(externallySorted);
        notesDisplayId = in.readLong();
        routeId = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelableArray(allValues, flags);
        dest.writeBooleanArray(externallySorted);
        dest.writeLong(notesDisplayId);
        dest.writeLong(routeId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LocationListAdapter> CREATOR = new Creator<LocationListAdapter>() {
        @Override
        public LocationListAdapter createFromParcel(Parcel in) {
            return new LocationListAdapter(in);
        }

        @Override
        public LocationListAdapter[] newArray(int size) {
            return new LocationListAdapter[size];
        }
    };

    private interface ViewHolder {
        void setup(int position);
    }

    private class ItemViewHolder implements View.OnClickListener, ViewHolder, View.OnLongClickListener {
        public SwipeLayout swipeLayout;

        private View surfaceView;

        public ImageButton deleteButton;
        public ImageButton editButton;
        public ImageButton checkButton;
        public ImageButton uncheckButton;

        public ImageView noteIcon;
        public TextView nameLabel;
        public TextView addressLabel;
        public TextView priceLabel;
        public TextView notesLabel;

        private LocationModel loc;

        public ItemViewHolder(View itemView) {
            swipeLayout = (SwipeLayout) itemView;
            surfaceView = swipeLayout.getSurfaceView();

            swipeLayout.addDrag(SwipeLayout.DragEdge.Right, swipeLayout.findViewById(R.id.location_list_item_right_bottom));
            swipeLayout.addDrag(SwipeLayout.DragEdge.Left, swipeLayout.findViewById(R.id.location_list_item_left_bottom));

            noteIcon = (ImageView) itemView.findViewById(R.id.location_list_item_note_icon);
            nameLabel = (TextView) itemView.findViewById(R.id.location_list_item_name_label);
            addressLabel = (TextView) itemView.findViewById(R.id.location_list_item_address_label);
            priceLabel = (TextView) itemView.findViewById(R.id.location_list_item_price_label);
            notesLabel = (TextView) itemView.findViewById(R.id.location_list_item_notes_label);

            deleteButton = (ImageButton) itemView.findViewById(R.id.location_list_item_delete_button);
            editButton = (ImageButton) itemView.findViewById(R.id.location_list_item_edit_button);
            checkButton = (ImageButton) itemView.findViewById(R.id.location_list_item_check_button);
            uncheckButton = (ImageButton) itemView.findViewById(R.id.location_list_item_uncheck_button);

            surfaceView.setOnClickListener(this);
            surfaceView.setOnLongClickListener(this);
            deleteButton.setOnClickListener(this);
            editButton.setOnClickListener(this);
            checkButton.setOnClickListener(this);
            uncheckButton.setOnClickListener(this);

            swipeLayout.setClickToClose(true);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == surfaceView.getId()) {
                notesDisplayId = notesLabel.getVisibility() == View.VISIBLE ? -1 : loc.id;
                if (notesDisplayId == loc.id)
                    notesLabel.startAnimation(new NoteExpandAnimation(notesLabel, 100));
                notifyDataSetChanged();
            } else if (v.getId() == deleteButton.getId()) {
                swipeLayout.close();
                Utils.deleteAlert(activity, loc.name, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DatabaseHelper.getInstance(activity).deleteRouteLocation(loc);
                        updateLocationFromDatabase(loc.id);
                        dialog.dismiss();
                    }
                }).show();
            } else if (v.getId() == editButton.getId()) {
                Intent intent = new Intent(activity.getApplicationContext(), LocationAddActivity.class);
                intent.putExtra(LocationAddActivity.LOCATION_ID_KEY, loc.id);
                intent.putExtra(LocationAddActivity.ROUTE_ID_KEY, routeId);
                activity.startActivityForResult(intent, LocationListActivity.EDIT_LOCATION_REQUEST_CODE);
                swipeLayout.close();
            } else if (v.getId() == checkButton.getId()) {
                swipeLayout.close(false);
                loc.state = LocationModel.State.DELIVERED;
                DatabaseAsync.getInstance(activity).updateRouteLocation(loc, new DatabaseAsync.Callback<Long>() {
                    @Override
                    public void onPostExecute(Long locationId) {
                        updateLocationFromDatabase(locationId);
                    }
                });
            } else if (v.getId() == uncheckButton.getId()) {
                swipeLayout.close(false);
                loc.state = LocationModel.State.OPEN;
                DatabaseAsync.getInstance(activity).updateRouteLocation(loc, new DatabaseAsync.Callback<Long>() {
                    @Override
                    public void onPostExecute(Long locationId) {
                        updateLocationFromDatabase(locationId);
                    }
                });
            }
        }

        @Override
        public boolean onLongClick(View v) {
            Utils.startNavigation(activity, new LatLng(loc.place.latitude, loc.place.longitude));
            return true;
        }

        public void setup(int position) {
            ItemInfo itemInfo = getItemInfo(position);
            loc = allValues[itemInfo.section].get(itemInfo.relativeItemPos);

            nameLabel.setText(loc.name);
            addressLabel.setText(loc.place.address);

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
            priceLabel.setText(currencyFormat.format(loc.price));

            if (loc.notes.isEmpty()) {
                noteIcon.setVisibility(View.GONE);
                notesLabel.setText(R.string.no_note_available);
                notesLabel.setEnabled(false);
            } else {
                noteIcon.setVisibility(View.VISIBLE);
                notesLabel.setText(loc.notes);
                notesLabel.setEnabled(true);
            }

            if (notesLabel.getVisibility() == View.VISIBLE && notesDisplayId != loc.id) {
                notesLabel.startAnimation(new NoteExpandAnimation(notesLabel, 100));
            }

            if (loc.state == LocationModel.State.DELIVERED) {
                checkButton.setVisibility(View.GONE);
                uncheckButton.setVisibility(View.VISIBLE);
            } else {
                checkButton.setVisibility(View.VISIBLE);
                uncheckButton.setVisibility(View.GONE);
            }
        }
    }

    private class NoteExpandAnimation extends Animation {
        private View animatedView;
        private LinearLayout.LayoutParams viewLayoutParams;
        private int bottomMarginInitial;
        private int marginStart, marginEnd;
        private boolean initialVisible = false;

        NoteExpandAnimation(View view, int duration) {
            setDuration(duration);
            animatedView = view;

            initialVisible = (view.getVisibility() == View.VISIBLE);

            viewLayoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
            bottomMarginInitial = 0;

            view.measure(View.MeasureSpec.makeMeasureSpec(((LinearLayout)view.getParent()).getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0xffffff, View.MeasureSpec.AT_MOST));
            int heightToAnimate = view.getMeasuredHeight() + viewLayoutParams.topMargin;

            marginStart = initialVisible ? bottomMarginInitial : -heightToAnimate;
            marginEnd = initialVisible ? -heightToAnimate : bottomMarginInitial;

            setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    animatedView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    viewLayoutParams.bottomMargin = bottomMarginInitial;
                    if (initialVisible) {
                        animatedView.setVisibility(View.GONE);
                    }
                    animatedView.requestLayout();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            if (interpolatedTime < 1.0f) {
                viewLayoutParams.bottomMargin = marginStart + (int) ((marginEnd - marginStart) * interpolatedTime);

                animatedView.requestLayout();
            }
        }
    }

    private class HeaderViewHolder implements ViewHolder {
        private TextView sectionHeaderLabel;

        public HeaderViewHolder(View itemView) {
            sectionHeaderLabel = (TextView) itemView.findViewById(R.id.location_list_section_header_text);
        }

        public void setup(int position) {
            ItemInfo itemInfo = getItemInfo(position);
            sectionHeaderLabel.setText(sections[itemInfo.section]);
        }
    }

    private class EmptyNoteViewHolder implements ViewHolder {
        private TextView emptynoteLabel;

        public EmptyNoteViewHolder(View view) {
            emptynoteLabel = (TextView) view.findViewById(R.id.list_emptynote_text);
        }

        @Override
        public void setup(int position) {
            ItemInfo itemInfo = getItemInfo(position);
            emptynoteLabel.setText(emptySectionTexts[itemInfo.section]);
        }
    }
}
