package ui;

/** A screen shown in the main window. {@link #refresh()} reloads its data from the database. */
public interface Page {
    void refresh();
}
