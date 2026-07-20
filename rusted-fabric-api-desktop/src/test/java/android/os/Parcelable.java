package android.os;

/** Test-classpath Android compatibility contract omitted from the desktop game Jar. */
public interface Parcelable {
    int describeContents();

    void writeToParcel(Parcel destination, int flags);

    interface Creator<T> {
        T createFromParcel(Parcel source);

        T[] newArray(int size);
    }
}
