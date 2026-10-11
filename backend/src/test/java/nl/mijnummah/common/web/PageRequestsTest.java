package nl.mijnummah.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class PageRequestsTest {

    @Test
    void defaultsAndBoundsClientInput() {
        assertThat(PageRequests.of(null, null, Sort.unsorted()).getPageSize()).isEqualTo(PageRequests.DEFAULT_SIZE);
        assertThat(PageRequests.of(-3, 0, Sort.unsorted()).getPageNumber()).isZero();
        assertThat(PageRequests.of(2, 10_000, Sort.unsorted()).getPageSize()).isEqualTo(PageRequests.MAX_SIZE);
        assertThat(PageRequests.of(2, 15, Sort.unsorted()).getPageNumber()).isEqualTo(2);
    }
}
