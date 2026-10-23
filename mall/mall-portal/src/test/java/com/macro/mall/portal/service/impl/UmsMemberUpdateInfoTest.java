package com.macro.mall.portal.service.impl;

import com.macro.mall.common.exception.ApiException;
import com.macro.mall.mapper.UmsMemberMapper;
import com.macro.mall.model.UmsMember;
import com.macro.mall.portal.service.UmsMemberCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UmsMemberUpdateInfoTest {

    @Mock
    private UmsMemberMapper memberMapper;
    @Mock
    private UmsMemberCacheService memberCacheService;

    @InjectMocks
    private UmsMemberServiceImpl service;

    private UmsMember member;

    @BeforeEach
    void setUp() {
        member = new UmsMember();
        member.setId(1L);
        member.setNickname("旧昵称");
    }

    @Test
    void 正常修改昵称并返回更新后的会员() {
        UmsMember updated = new UmsMember();
        updated.setId(1L);
        updated.setNickname("新昵称");
        when(memberMapper.selectByPrimaryKey(1L)).thenReturn(updated);

        UmsMember result = service.updateInfo(1L, "  新昵称  ");

        assertEquals("新昵称", result.getNickname());
        ArgumentCaptor<UmsMember> captor = ArgumentCaptor.forClass(UmsMember.class);
        verify(memberMapper).updateByPrimaryKeySelective(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals("新昵称", captor.getValue().getNickname());
        verify(memberCacheService).delMember(1L);
    }

    @Test
    void 空昵称被拒绝且不落库() {
        assertThrows(ApiException.class, () -> service.updateInfo(1L, "   "));
        verify(memberMapper, never()).updateByPrimaryKeySelective(any(UmsMember.class));
    }

    @Test
    void 超长昵称被拒绝且不落库() {
        assertThrows(ApiException.class, () -> service.updateInfo(1L, "a".repeat(65)));
        verify(memberMapper, never()).updateByPrimaryKeySelective(any(UmsMember.class));
    }
}
