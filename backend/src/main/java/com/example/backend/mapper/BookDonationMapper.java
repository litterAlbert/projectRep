package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.pojo.BookDonation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图书捐赠 Mapper 接口
 * 时间: 2026-04-16
 */
@Mapper
public interface BookDonationMapper extends BaseMapper<BookDonation> {
}
