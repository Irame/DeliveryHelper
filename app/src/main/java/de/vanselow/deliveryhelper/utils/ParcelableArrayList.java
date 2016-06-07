package de.vanselow.deliveryhelper.utils;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ParcelableArrayList<T extends Parcelable> implements List<T>, Parcelable {
    private final List<T> list;

    public ParcelableArrayList() {
        list = new ArrayList<>();
    }

    public ParcelableArrayList(int capacity) {
        list = new ArrayList<>(capacity);
    }

    public ParcelableArrayList(Collection<? extends T> collection) {
        list = new ArrayList<>(collection);
    }

    protected ParcelableArrayList(Parcel in) {
        int size = in.readInt();
        if (size == 0) {
            list = new ArrayList<>();
        } else {
            Class<?> type = (Class<?>) in.readSerializable();
            list = new ArrayList<>(size);
            in.readList(list, type.getClassLoader());
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(list.size());
        if (list.size() > 0) {
            final Class<?> objectsType = list.get(0).getClass();
            dest.writeSerializable(objectsType);
            dest.writeList(list);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ParcelableArrayList> CREATOR = new Creator<ParcelableArrayList>() {
        @Override
        public ParcelableArrayList createFromParcel(Parcel in) {
            return new ParcelableArrayList(in);
        }

        @Override
        public ParcelableArrayList[] newArray(int size) {
            return new ParcelableArrayList[size];
        }
    };

    @Override
    public void add(int location, T object) {
        list.add(location, object);
    }

    @Override
    public boolean add(T object) {
        return list.add(object);
    }

    @Override
    public boolean addAll(int location, Collection<? extends T> collection) {
        return list.addAll(location, collection);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        return list.addAll(collection);
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public boolean contains(Object object) {
        return list.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return list.containsAll(collection);
    }

    @Override
    public boolean equals(Object object) {
        return list.equals(object);
    }

    @Override
    public T get(int location) {
        return list.get(location);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    @Override
    public int indexOf(Object object) {
        return list.indexOf(object);
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public int lastIndexOf(Object object) {
        return list.lastIndexOf(object);
    }

    @Override
    public ListIterator<T> listIterator() {
        return list.listIterator();
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator(int location) {
        return list.listIterator(location);
    }

    @Override
    public T remove(int location) {
        return list.remove(location);
    }

    @Override
    public boolean remove(Object object) {
        return list.remove(object);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return list.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return list.retainAll(collection);
    }

    @Override
    public T set(int location, T object) {
        return list.set(location, object);
    }

    @Override
    public int size() {
        return list.size();
    }

    @NonNull
    @Override
    public List<T> subList(int start, int end) {
        return list.subList(start, end);
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @NonNull
    @Override
    public <T1> T1[] toArray(T1[] array) {
        return list.toArray(array);
    }
}
