package com.helpdesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.repository.UserRepository;
import com.helpdesk.service.BootstrapService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BootstrapTest extends SqliteTestSupport {
    private final BootstrapService bootstrap;
    private final UserRepository users;

    @Autowired
    BootstrapTest(BootstrapService bootstrap, UserRepository users) {
        this.bootstrap = bootstrap;
        this.users = users;
    }

    @Test
    void firstAdminNeedsCredentialsAndInitializationIsIdempotent() {
        assertThatThrownBy(() -> bootstrap.initialize("", "something"))
                .isInstanceOf(IllegalStateException.class);
        bootstrap.initialize("first_admin", "Bootstrap123!");
        assertThat(users.findByUsername("first_admin")).isPresent();
        bootstrap.initialize("ignored_admin", "Other123!");
        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findByUsername("ignored_admin")).isEmpty();
    }
}
