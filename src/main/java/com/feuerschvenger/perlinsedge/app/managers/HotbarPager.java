package com.feuerschvenger.perlinsedge.app.managers;

import java.util.List;

/**
 * Splits a list of entries into pages of a fixed size, so a hotbar with a few number-key slots can reach every entry.
 * Slot numbers are 1-based and relative to the current page. Paging wraps around at both ends.
 *
 * @param <T> type of the entries
 */
public final class HotbarPager<T> {

    private final List<T> entries;
    private final int pageSize;
    private int page;

    /**
     * Creates a pager positioned on the first page.
     *
     * @param entries  all entries, in display order
     * @param pageSize number of slots per page, must be positive
     */
    public HotbarPager(List<T> entries, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        this.entries = List.copyOf(entries);
        this.pageSize = pageSize;
    }

    /** @return number of pages, at least 1 even when there are no entries */
    public int getPageCount() {
        return Math.max(1, (entries.size() + pageSize - 1) / pageSize);
    }

    /** @return zero-based index of the current page */
    public int getPage() {
        return page;
    }

    /** @return the entries on the current page, at most one page worth */
    public List<T> currentPage() {
        int from = Math.min(page * pageSize, entries.size());
        int to = Math.min(from + pageSize, entries.size());
        return entries.subList(from, to);
    }

    /**
     * Looks up the entry behind a slot number of the current page.
     *
     * @param slotNumber 1-based slot number
     * @return the entry, or null when the slot is empty or out of range
     */
    public T select(int slotNumber) {
        List<T> current = currentPage();
        return slotNumber >= 1 && slotNumber <= current.size() ? current.get(slotNumber - 1) : null;
    }

    /** Moves to the next page, wrapping from the last page to the first. */
    public void next() {
        page = (page + 1) % getPageCount();
    }

    /** Moves to the previous page, wrapping from the first page to the last. */
    public void previous() {
        page = (page - 1 + getPageCount()) % getPageCount();
    }

    /**
     * Moves to the page that holds an entry.
     *
     * @param entry the entry to show
     * @return true if the entry exists and its page is now current
     */
    public boolean showPageOf(T entry) {
        int index = entries.indexOf(entry);
        if (index < 0) {
            return false;
        }
        page = index / pageSize;
        return true;
    }

    /**
     * Finds the slot number an entry has on its page.
     *
     * @param entry the entry
     * @return the 1-based slot number, or -1 when the entry does not exist
     */
    public int slotOf(T entry) {
        int index = entries.indexOf(entry);
        return index < 0 ? -1 : index % pageSize + 1;
    }
}
