package nl.mijnummah.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Bounds client-supplied paging parameters, so a request can never ask for an unbounded result. */
public final class PageRequests {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageRequests() {
    }

    public static PageRequest of(Integer page, Integer size, Sort sort) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }
}
