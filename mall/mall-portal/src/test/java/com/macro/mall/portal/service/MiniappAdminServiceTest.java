package com.macro.mall.portal.service;

import com.macro.mall.mapper.UmsAdminMapper;
import com.macro.mall.model.UmsAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MiniappAdminServiceTest {
    @Mock
    private UmsAdminMapper adminMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private MiniappAdminService service;

    private UmsAdmin admin;

    @BeforeEach
    void setUp() {
        admin = new UmsAdmin();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword("encoded");
        admin.setStatus(1);
    }

    @Test
    void acceptsEnabledAdministratorWithMatchingPassword() {
        when(adminMapper.selectByExample(any())).thenReturn(List.of(admin));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);

        assertEquals(admin, service.authenticate("admin", "secret"));
    }

    @Test
    void rejectsInvalidPassword() {
        when(adminMapper.selectByExample(any())).thenReturn(List.of(admin));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertNull(service.authenticate("admin", "wrong"));
    }

    @Test
    void rejectsDisabledAdministrator() {
        admin.setStatus(0);
        when(adminMapper.selectByExample(any())).thenReturn(List.of(admin));

        assertNull(service.authenticate("admin", "secret"));
    }

    @Test
    void rejectsUnknownOrAmbiguousAdministrator() {
        when(adminMapper.selectByExample(any())).thenReturn(List.of());
        assertNull(service.authenticate("missing", "secret"));

        when(adminMapper.selectByExample(any())).thenReturn(List.of(admin, admin));
        assertNull(service.authenticate("admin", "secret"));
    }
}
